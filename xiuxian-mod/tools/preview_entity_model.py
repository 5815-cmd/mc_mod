#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
红蒙修仙模组 —— 实体模型离线预览 / UV 交叉校验
==============================================

这个脚本做两件事，都不需要启动游戏：

1. **交叉校验**：从 `CultivationHumanoidModel.java` / `SpiritWolfRenderer.java`
   里把 `texOffs(...)` 与 `addBox(...)` 解析出来，和 `gen_entity_textures.py`
   的 UV 表逐个比对。两边不一致就直接报错——这是"模型指着 A 处像素、
   贴图画在 B 处"这类错误唯一能在离线阶段抓住的地方（游戏里只会糊成一片）。

2. **软件渲染预览**：用一个极小的正交光栅器把模型画成等轴测图，
   采样真实贴图。看到的就是游戏里的样子，只是没有光影和动画。

用法::

    python tools/preview_entity_model.py                 # 校验 + 出图
    python tools/preview_entity_model.py --check-only    # 只校验
"""

from __future__ import annotations

import argparse
import os
import re
import sys

try:
    from PIL import Image, ImageDraw
except ImportError:  # pragma: no cover
    sys.exit("需要 Pillow：pip install pillow")

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
TEX_DIR = os.path.join(MOD_ROOT, "src", "main", "resources",
                       "assets", "hongmeng", "textures", "entity")
MODEL_JAVA = os.path.join(MOD_ROOT, "src", "main", "java", "com", "hongmeng",
                          "xiuxian", "client", "CultivationHumanoidModel.java")
WOLF_JAVA = os.path.join(MOD_ROOT, "src", "main", "java", "com", "hongmeng",
                         "xiuxian", "client", "SpiritWolfRenderer.java")

sys.path.insert(0, HERE)
import gen_entity_textures as gen  # noqa: E402


# ---------------------------------------------------------------------------
# 1. 从 Java 源码里读模型
# ---------------------------------------------------------------------------

#: `.addOrReplaceChild("name", CubeListBuilder.create()`
_PART_OPEN = re.compile(r'addOrReplaceChild\(\s*"(?P<name>\w+)"\s*,\s*CubeListBuilder\.create\(\)')
_TEXOFFS = re.compile(r'\.texOffs\(\s*(-?\d+)\s*,\s*(-?\d+)\s*\)')
_ADDBox = re.compile(
    r'\.addBox\(\s*(-?[\d.]+)F\s*,\s*(-?[\d.]+)F\s*,\s*(-?[\d.]+)F\s*,'
    r'\s*(-?[\d.]+)F\s*,\s*(-?[\d.]+)F\s*,\s*(-?[\d.]+)F')
_MIRROR = re.compile(r'\.mirror\(\)')
_POSE = re.compile(r'PartPose\.\w+\(')


def _strip_comments(line: str) -> str:
    """砍掉 `//` 之后的行注释（本文件只需要处理行注释，不处理块注释里的字符串）。"""
    i = line.find("//")
    if i >= 0:
        line = line[:i]
    s = line.strip()
    # 块注释的中间行/结束行（以 * 开头或 */ 结尾）也一并丢掉
    if s.startswith("*") or s.startswith("/*"):
        return ""
    return line


def parse_java_model(path: str, root_names=None) -> dict:
    """把 Java 里的部件定义解析成 {name: (texOffs_u, texOffs_v, w, h, d, mirror)}。

    **单趟状态机，按行扫。** 有两个坑必须避开，都踩过：

    1. 不能用一条跨行的 `addOrReplaceChild(.*?)PartPose` 大正则：部件是嵌套声明的
       （`root.addOrReplaceChild("head", ...)` 的后面紧跟着
       `head.addOrReplaceChild("hair_bun", ...)`），`.*?` 会从外层一路吃到内层的
       `PartPose`，凭空造出一个错的 head 定义、同时把内层部件整段跳过。
    2. 注释必须先剥掉：文件头 javadoc 里就写着 `PartPose.offset`、`texOffs(0,0)`
       这类示例，不剥的话解析器会被注释带跑。

    状态机规则：遇到 `addOrReplaceChild("name", CubeListBuilder.create()`
    就开一个新部件；随后行里出现的 texOffs / mirror / addBox 都归它；
    遇到 `PartPose.` 就收尾。因为每个部件声明恰好是
    "开→texOffs+addBox→PartPose" 三行，这个规则不会串台。
    """
    raw = open(path, "r", encoding="utf-8").read().splitlines()
    lines = [_strip_comments(ln) for ln in raw]

    parts = {}
    cur = None       # 当前部件的 (name, u, v, w, h, d, mirror)

    def flush():
        nonlocal cur
        if cur is None:
            return
        name, u, v, w, h, d, mirror = cur
        if None not in (u, v, w, h, d):
            parts[name] = (u, v, w, h, d, mirror)
        cur = None

    for ln in lines:
        m = _PART_OPEN.search(ln)
        if m:
            flush()
            cur = [m.group("name"), None, None, None, None, None, False]
        if cur is None:
            continue
        to = _TEXOFFS.search(ln)
        if to and cur[1] is None:
            cur[1], cur[2] = int(to.group(1)), int(to.group(2))
        if _MIRROR.search(ln):
            cur[6] = True
        box = _ADDBox.search(ln)
        if box and cur[3] is None:
            vals = [float(x) for x in box.groups()]
            cur[3], cur[4], cur[5] = int(vals[3]), int(vals[4]), int(vals[5])
        if _POSE.search(ln) and cur[3] is not None:
            flush()
    flush()
    return parts


def check_against_generator(java_parts: dict, py_table: dict, label: str,
                            alias: dict | None = None) -> int:
    """Java 里的 texOffs / 尺寸必须和贴图生成器的 UV 表逐字一致。

    `alias` 用来把 Java 里多个部件名映射到生成器里的同一条 UV 记录
    （比如狼的四条腿都是同一个 `leg`）。
    """
    alias = alias or {}
    bad = 0
    seen_py = set()
    for name, (u, v, w, h, d, mirror) in java_parts.items():
        key = alias.get(name, name)
        if key not in py_table:
            print(f"  ! {label}: Java 有部件 {name}，但贴图生成器里没有（别名为 {key}）")
            bad += 1
            continue
        seen_py.add(key)
        pu, pv, pw, ph, pd, pm = py_table[key]
        if (u, v, w, h, d) != (pu, pv, pw, ph, pd):
            print(f"  ! {label}: 部件 {name} 不一致 "
                  f"Java=({u},{v},{w},{h},{d}) 生成器=({pu},{pv},{pw},{ph},{pd})")
            bad += 1
        elif mirror != pm:
            print(f"  ! {label}: 部件 {name} 的 mirror 标志不一致 "
                  f"Java={mirror} 生成器={pm}")
            bad += 1
    for name in py_table:
        if name not in seen_py:
            print(f"  ! {label}: 贴图生成器有部件 {name}，但 Java 模型里没有")
            bad += 1
    return bad


# ---------------------------------------------------------------------------
# 2. 最小正交光栅器
# ---------------------------------------------------------------------------

# 每个面的法线（模型坐标系：+x 右、+y 下、+z 前）
FACE_NORMALS = {
    "front": (0.0, 0.0, -1.0),
    "back": (0.0, 0.0, 1.0),
    "right": (-1.0, 0.0, 0.0),   # 模型 -x 那一侧（贴图里的"右面"）
    "left": (1.0, 0.0, 0.0),
    "top": (0.0, -1.0, 0.0),
    "bottom": (0.0, 1.0, 0.0),
}


def box_faces(x, y, z, w, h, d):
    """六个面的四个角（按逆时针，从外面看）。"""
    x0, y0, z0 = x, y, z
    x1, y1, z1 = x + w, y + h, z + d
    return {
        "front":  [(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)],
        "back":   [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
        "right":  [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
        "left":   [(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)],
        "top":    [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
        "bottom": [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
    }


def face_uv_quad(rects, face):
    """面的四个角对应的 UV（和 box_faces 的顺序一一对应）。"""
    x0, y0, x1, y1 = rects[face]
    if face == "front":
        return [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
    if face == "back":
        return [(x1, y0), (x0, y0), (x0, y1), (x1, y1)]
    if face == "right":
        return [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
    if face == "left":
        return [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
    if face == "top":
        return [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
    return [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]


def project(p, yaw, pitch, scale, ox, oy):
    """等轴测投影：先绕 y 转 yaw，再绕 x 转 pitch。"""
    import math
    x, y, z = p
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy + z * sy, -x * sy + z * cy
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    return (ox + x * scale, oy + y * scale, z)


def render(model_parts, texture, yaw, pitch, scale, pad=6, label=""):
    """把一堆 (顶点四角, uv 四角, 颜色) 面片按画家算法画出来。"""
    quads = []
    for verts, uvs, base in model_parts:
        pts = [project(v, yaw, pitch, scale, 0, 0) for v in verts]
        depth = sum(p[2] for p in pts) / len(pts)
        quads.append((depth, pts, uvs, base))
    quads.sort(key=lambda q: q[0])          # 远的先画

    xs = [p[0] for _, pts, _, _ in quads for p in pts]
    ys = [p[1] for _, pts, _, _ in quads for p in pts]
    w = int(max(xs) - min(xs)) + pad * 2
    h = int(max(ys) - min(ys)) + pad * 2 + (14 if label else 0)
    img = Image.new("RGBA", (max(w, 1), max(h, 1)), (26, 26, 32, 255))
    px = img.load()
    ox = pad - min(xs)
    oy = pad - min(ys)

    tex = texture.load()
    tw, th = texture.size

    for _, pts, uvs, base in quads:
        pts = [(p[0] + ox, p[1] + oy) for p in pts]
        # 背面剔除用投影后的有向面积
        area = sum((pts[i][0] - pts[(i + 1) % 4][0]) * (pts[i][1] + pts[(i + 1) % 4][1])
                   for i in range(4))
        if area > 0:
            continue
        # 扫描线
        minx = max(0, int(min(p[0] for p in pts)))
        maxx = min(img.width - 1, int(max(p[0] for p in pts)) + 1)
        miny = max(0, int(min(p[1] for p in pts)))
        maxy = min(img.height - 1, int(max(p[1] for p in pts)) + 1)
        for yy in range(miny, maxy + 1):
            for xx in range(minx, maxx + 1):
                hit = _inside(pts, xx + 0.5, yy + 0.5)
                if hit is None:
                    continue
                a, b = hit
                # 双线性插值 UV（用四角重心近似：按两条边的参数插值）
                u = ((1 - a) * (1 - b) * uvs[0][0] + a * (1 - b) * uvs[1][0]
                     + a * b * uvs[2][0] + (1 - a) * b * uvs[3][0])
                v = ((1 - a) * (1 - b) * uvs[0][1] + a * (1 - b) * uvs[1][1]
                     + a * b * uvs[2][1] + (1 - a) * b * uvs[3][1])
                sx = min(tw - 1, max(0, int(u)))
                sy = min(th - 1, max(0, int(v)))
                col = tex[sx, sy]
                if col[3] == 0:
                    continue
                px[xx, yy] = (int(col[0] * base), int(col[1] * base),
                              int(col[2] * base), 255)
    if label:
        ImageDraw.Draw(img).text((4, img.height - 12), label, fill=(225, 225, 230, 255))
    return img


def _inside(pts, x, y):
    """四边形命中测试；返回 (a, b) 双线性参数，未命中返回 None。"""
    p0, p1, p2, p3 = pts
    # 拆成两个三角形
    for tri, mapping in (((p0, p1, p2), ((0, 0), (1, 0), (1, 1))),
                         ((p0, p2, p3), ((0, 0), (1, 1), (0, 1)))):
        r = _bary(tri, x, y)
        if r is None:
            continue
        w0, w1, w2 = r
        a = w1 * mapping[1][0] + w2 * mapping[2][0]
        b = w1 * mapping[1][1] + w2 * mapping[2][1]
        return a, b
    return None


def _bary(tri, x, y):
    (x0, y0), (x1, y1), (x2, y2) = tri
    den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(den) < 1e-9:
        return None
    w0 = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) / den
    w1 = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) / den
    w2 = 1 - w0 - w1
    if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
        return None
    return w0, w1, w2


# ---------------------------------------------------------------------------
# 3. 组装
# ---------------------------------------------------------------------------

#: 各怪用到哪些部件（和 CultivationHumanoidRenderer.Variant 对应）
VARIANTS = {
    "blood_demon_cultivator": dict(pauldrons=False, cape=False, blade=False),
    "puppet_captain":         dict(pauldrons=True,  cape=False, blade=True),
    "demon_general":          dict(pauldrons=False, cape=True,  blade=True),
    "void_overlord":          dict(pauldrons=True,  cape=True,  blade=True),
}


def build_quads(java_parts, texture, keep):
    out = []
    for name, (u, v, w, h, d, mirror) in java_parts.items():
        if name in ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg"):
            continue
        if not keep(name):
            continue
        uu = u + (d + w if mirror else 0)
        rects = gen.Canvas.face_rects(uu, v, w, h, d)
        # 位置：这里只做近似摆放，够看出贴图对不对即可
        x, y, z = PART_POS.get(name, (0, 0, 0))
        for face, verts in box_faces(x, y, z, w, h, d).items():
            out.append((verts, face_uv_quad(rects, face), SHADE[face]))
    return out


#: 自建部件在预览坐标系里的位置。
#: 预览坐标 = 原版 HumanoidModel 的模型坐标 + y 偏移 24（脚底落在 y=24）。
#: 头的盒子是 y 0..8，`PartPose` 挂在 y=0，所以发髻相对头的 -7.5..-2.5
#: 换算过来就是 -7.5..-2.5。
PART_POS = {
    "hair_bun":       (-2, -7.5, -2),      # head 子部件，y=-7.5 起，高 5
    "cape":           (-3, 8.5, 2.3),      # body 子部件，下垂 8
    "blade":          (-1, 9.5, 3.2),
    "blade_hilt":     (-4.0, 9.5, 3.2),
    "right_pauldron": (-8.0, 9.5, -1.0),
    "left_pauldron":  (4.0, 9.5, -1.0),
}

#: 人形骨架在世界坐标里的位置（和原版 HumanoidModel 一致，单位=像素，脚底 y=24）
BODY_POS = {
    "head":      (-4, 0, -4),
    "body":      (-4, 8, -2),
    "right_arm": (-8, 10, -2),
    "left_arm":  (4, 10, -2),
    "right_leg": (-3.9, 20, -2),
    "left_leg":  (1.9, 20, -2),
}

#: 腿在预览里稍微分开一点，不然两条腿完全重叠看不出是两条
LEG_SPLIT = 0.0

#: 哪些面在预览里直接丢掉（贴着别的部件，画出来反而糊）
SKIP_FACES = {
    "head": {"bottom"},
    "body": {"top", "bottom"},
    "right_leg": {"top"},
    "left_leg": {"top"},
    # 杖/剑背面贴着躯干，且原版左臂底面的 UV 跟它共用像素
    "blade": {"back"},
    "blade_hilt": {"back"},
    # 护肩背面朝里、披风背面朝身体
    "cape": {"back"},
    "right_pauldron": {"back"},
    "left_pauldron": {"back"},
}

SHADE = {"front": 1.0, "back": 0.72, "right": 0.82, "left": 0.78,
         "top": 1.12, "bottom": 0.55}


def build_body_quads(java_parts, keep):
    """人形六个主体部件的面片（骨架上色，用真实贴图）。"""
    out = []
    for name, pos in BODY_POS.items():
        if name not in java_parts:
            continue
        u, v, w, h, d, mirror = java_parts[name]
        uu = u + (d + w if mirror else 0)
        rects = gen.Canvas.face_rects(uu, v, w, h, d)
        x, y, z = pos
        for face, verts in box_faces(x, y, z, w, h, d).items():
            if face in SKIP_FACES.get(name, ()):
                continue
            out.append((verts, face_uv_quad(rects, face), SHADE[face] * 0.94))
    return out


def build_part_quads(java_parts, keep):
    """自建部件的面片。"""
    out = []
    for name, (u, v, w, h, d, mirror) in java_parts.items():
        if name in BODY_POS:
            continue
        if not keep(name):
            continue
        uu = u + (d + w if mirror else 0)
        rects = gen.Canvas.face_rects(uu, v, w, h, d)
        x, y, z = PART_POS.get(name, (0, 0, 0))
        for face, verts in box_faces(x, y, z, w, h, d).items():
            if face in SKIP_FACES.get(name, ()):
                continue
            out.append((verts, face_uv_quad(rects, face), SHADE[face]))
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description="实体模型离线预览 + UV 交叉校验")
    ap.add_argument("--tex", default=TEX_DIR, help="贴图目录")
    ap.add_argument("--out", default=os.path.join(MOD_ROOT, "tools", "preview"),
                    help="预览图输出目录")
    ap.add_argument("--check-only", action="store_true")
    args = ap.parse_args()

    print("== Java 模型解析 ==")
    java = parse_java_model(MODEL_JAVA, None)
    for k in sorted(java):
        print(f"  {k:16s} texOffs=({java[k][0]:>2},{java[k][1]:>2}) "
              f"{java[k][2]}x{java[k][3]}x{java[k][4]} mirror={java[k][5]}")

    print("\n== 与贴图生成器交叉校验 ==")
    bad = check_against_generator(java, gen.HUMANOID_LAYOUT, "人形")
    # 灵狼：四条腿共用一个 texOffs（生成器里只有一条 `leg` 记录）；
    # 耳朵和吻是挂在 head 上的额外盒子，不是独立部件，所以生成器里单独列出来画。
    wolf_java = parse_java_model(WOLF_JAVA, None)
    wolf_alias = {
        "right_hind_leg": "leg", "left_hind_leg": "leg",
        "right_front_leg": "leg", "left_front_leg": "leg",
    }
    wolf_py = {k: (u, v, w, h, d, False)
               for k, (u, v, w, h, d) in gen.WOLF_LAYOUT.items()
               if k not in ("ear", "muzzle")}
    bad += check_against_generator(wolf_java, wolf_py, "灵狼", alias=wolf_alias)
    if bad:
        print(f"  => {bad} 处不一致，模型和贴图会对不上！")
        return 1
    print("  => 一致：Java 模型的 texOffs/尺寸 与贴图 UV 表逐项吻合")

    if args.check_only:
        return 0

    print("\n== 渲染预览 ==")
    os.makedirs(args.out, exist_ok=True)
    tiles = []
    for mob, spec in VARIANTS.items():
        tex = Image.open(os.path.join(args.tex, mob + ".png")).convert("RGBA")

        def keep(n, s=spec):
            if n == "cape":
                return s["cape"]
            if n in ("blade", "blade_hilt"):
                return s["blade"]
            if n in ("right_pauldron", "left_pauldron"):
                return s["pauldrons"]
            return True

        quads = build_body_quads(java, keep) + build_part_quads(java, keep)
        for view, yaw in (("front", 0.55), ("back", 2.55)):
            tiles.append(render(quads, tex, yaw=yaw, pitch=0.30, scale=4.0,
                                label=f"{mob} [{view}]"))
        print(f"  {mob}: {len(quads)} 个面")

    cols = 2
    rows = (len(tiles) + cols - 1) // cols
    cw = max(t.width for t in tiles) + 8
    ch = max(t.height for t in tiles) + 8
    sheet = Image.new("RGBA", (cw * cols, ch * rows), (18, 18, 22, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, (8 + (i % cols) * cw, 8 + (i // cols) * ch))
    p = os.path.join(args.out, "humanoid_preview.png")
    sheet.convert("RGB").save(p)
    print(f"  -> {p}  ({sheet.width}x{sheet.height})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
