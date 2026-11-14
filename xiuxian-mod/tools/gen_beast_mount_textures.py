#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
鸿蒙修仙 —— 灵兽（坐骑）贴图生成器
==================================

生成 3 张实体贴图（固定算法、无随机，每次跑结果逐像素一致）：

    assets/hongmeng/textures/entity/spirit_steed.png    64×64
    assets/hongmeng/textures/entity/contract_eagle.png  64×64
    assets/hongmeng/textures/entity/jiaolong.png        256×256

用法::

    python tools/gen_beast_mount_textures.py              # 写入模组资源目录
    python tools/gen_beast_mount_textures.py --check      # 只做 UV 排版自查
    python tools/gen_beast_mount_textures.py --uv         # 只打印 UV 表（贴 Java 用）
    python tools/gen_beast_mount_textures.py --out DIR    # 写到别处

★★★ 与 Java 侧的对应关系（改一处必须改另一处）★★★

UV 布局逐条对应下面三个渲染器的 ``createBodyLayer()``：

    STEED_LAYOUT    -> client/SpiritSteedRenderer.SteedModel
    EAGLE_LAYOUT    -> client/ContractEagleRenderer.EagleModel
    JIAOLONG_LAYOUT -> client/JiaolongRenderer.JiaolongModel

**坐标不是手写的，是排版器算出来的。** 手排 25 个盒子极易出错，而 MC 对
UV 越界/重叠**不报错**——只会把像素夹到边缘、把两块颜色糊在一起，
在游戏里表现为"某块颜色不对"，几乎不可能定位。所以这里：

  1. 每个部件只声明**尺寸** (w, h, d)（这个必须与 Java 侧一致）；
  2. 排版器按"高度降序 + 货架装箱"算出 (u, v)，保证任意两个盒子的
     **UV 包围盒**互不重叠（有意共图的同名/别名件除外）；
  3. ``check_layouts()`` 再独立验算一遍（越界 + 重叠），跑生成前先过这一关；
  4. ``--uv`` 把结果打成 Java 侧可直接粘贴的 ``texOffs(u, v)`` 表。

UV 展开公式（原版 ``ModelPart$Cube`` 的展开方式，与 ``gen_beast_textures.py``
共用同一套；那份文件里有拿原版 ``wolf.png`` 反推验证过的记录）::

    u1 = u          u2 = u + d          u4 = u + d + w      u5 = u + 2d + w
    v1 = v          v2 = v + d          v3 = v + d + h

    右面 [u,   v2, u+d,   v3]     左面 [u+d, v2, u+2d,  v3]
    顶面 [u+d, v1, u+d+w, v2]     底面 [u+d, v3, u+d+w, v3+d]
    前面 [u+d, v2, u+d+w, v3]     后面 [u+2d+w, v2, u+2d+2w, v3]

一个盒子的 UV 包围盒因此是 ``(2d + 2w) × (2d + h)``——
排版时的头号误解就是把它当成"一个面一个方块"。
"""

from __future__ import annotations

import argparse
import os
import sys

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("需要 Pillow：pip install pillow")

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
TEX_DIR = os.path.join(MOD_ROOT, "src", "main", "resources",
                       "assets", "hongmeng", "textures", "entity")

FACE_SHADE = {
    "right": 0.82,
    "left": 0.72,
    "top": 1.14,
    "bottom": 0.52,
    "front": 1.00,
    "back": 0.86,
}


# ---------------------------------------------------------------------------
# UV 数学（与 Java 侧共用）
# ---------------------------------------------------------------------------

def face_rects(u: int, v: int, w: int, h: int, d: int) -> dict:
    u1, u2, u3 = u, u + d, u + d + w
    u4, u5 = u + 2 * d + w, u + 2 * d + 2 * w
    v1, v2, v3, v4 = v, v + d, v + d + h, v + 2 * d + h
    return {
        "right": (u1, v2, u2, v3),
        "left": (u2, v2, u3, v3),
        "top": (u2, v1, u3, v2),
        "bottom": (u2, v3, u3, v4),
        "front": (u2, v2, u3, v3),
        "back": (u4, v2, u5, v3),
    }


def uv_extent(u: int, v: int, w: int, h: int, d: int):
    rects = face_rects(u, v, w, h, d).values()
    return (min(r[0] for r in rects), min(r[1] for r in rects),
            max(r[2] for r in rects), max(r[3] for r in rects))


def overlap(a, b) -> bool:
    return not (a[2] <= b[0] or b[2] <= a[0] or a[3] <= b[1] or b[3] <= a[1])


# ---------------------------------------------------------------------------
# 货架装箱器：把盒子的 UV 包围盒排进贴图，保证两两不重叠
# ---------------------------------------------------------------------------

def pack(entries, texture):
    """entries: [(name, w, h, d)]；返回 {name: (u, v, w, h, d)}。

    策略：高度降序 + 沿 x 依次摆放，放不下就换一行（行高 = 该行首个盒子的高度）。
    对 25 个盒子这种规模，这个贪心已经能得到紧凑且百分之百无重叠的结果，
    而且**完全确定**——同样的输入永远给同样的坐标。
    """
    tex_w, tex_h = texture
    boxes = []
    for name, w, h, d in entries:
        bw, bh = 2 * d + 2 * w, 2 * d + h
        boxes.append([name, w, h, d, bw, bh])
    boxes.sort(key=lambda b: (-b[5], -b[4], b[0]))

    placed = {}
    shelves = []          # [[y, height, used_x], ...]
    for name, w, h, d, bw, bh in boxes:
        target = None
        for s in shelves:
            if bh <= s[1] and s[2] + bw <= tex_w:
                target = s
                break
        if target is None:
            y = 0 if not shelves else shelves[-1][0] + shelves[-1][1]
            if y + bh > tex_h:
                raise SystemExit(
                    f"贴图排不下：{name} 需要 {bw}×{bh}，剩余高度不足"
                    f"（当前 {y + bh} > {tex_h}）")
            target = [y, bh, 0]
            shelves.append(target)
        placed[name] = (target[2], target[0], w, h, d)
        target[2] += bw
    return placed


# ---------------------------------------------------------------------------
# 三个模型的部件尺寸表
#
# 尺寸 (w, h, d) 必须与 Java 侧 createBodyLayer() 里的 addBox 逐字一致；
# (u, v) 由 pack() 算出来，不要手写。
# ---------------------------------------------------------------------------

# ---- 灵驹：马的身架 + 鹿角 + 长鬃 ----
STEED_TEXTURE = (128, 64)
STEED_PARTS = [
    ("neck", 4, 8, 4),
    ("head", 5, 6, 6),
    ("snout", 3, 3, 6),
    ("ear", 2, 2, 2),
    ("antler", 2, 5, 2),
    ("antler_tip", 2, 2, 3),
    ("mane", 2, 12, 3),
    ("body", 8, 10, 10),
    ("haunch", 9, 11, 8),
    ("tail", 2, 10, 2),
    ("leg", 2, 9, 2),
]
#: 四条腿、两只耳、两只角都是**同一个盒子**，共用 UV 是刻意的
STEED_ALIASES = {
    "left_ear": "ear",
    "right_antler": "antler",
    "left_antler": "antler",
    "right_antler_tip": "antler_tip",
    "left_antler_tip": "antler_tip",
    "front_leg": "leg",
    "hind_leg": "leg",
}

# ---- 契约雷鹰：可骑的鸟 ----
EAGLE_TEXTURE = (128, 64)
EAGLE_PARTS = [
    ("body", 8, 10, 12),
    ("neck", 4, 6, 4),
    ("head", 6, 6, 6),
    ("beak", 3, 3, 6),
    ("crest", 4, 2, 5),
    ("tail", 6, 2, 12),
    ("wing", 3, 2, 8),
    ("wing_tip", 3, 2, 8),
    ("leg", 3, 6, 3),
    ("talon", 4, 3, 5),
]
EAGLE_ALIASES = {
    "left_wing": "wing",
    "right_wing": "wing",
    "left_wing_tip": "wing_tip",
    "right_wing_tip": "wing_tip",
    "left_leg": "leg",
    "right_leg": "leg",
    "left_talon": "talon",
    "right_talon": "talon",
}

# ---- 蛟龙：九节蛇身 ----
JIAOLONG_TEXTURE = (256, 256)
JIAOLONG_SEGMENTS = 9
#: 带背鳍的节序号（0 基）—— 必须与 JiaolongRenderer.FIN_AT 一致
JIAOLONG_FIN_AT = (1, 3, 5, 7)
JIAOLONG_PARTS = [
    ("head", 9, 8, 12),
    ("snout", 6, 5, 4),
    ("jaw", 7, 3, 10),
    ("horn", 2, 6, 2),
    ("whisker", 1, 5, 1),
    ("segment", 7, 7, 7),
    ("tail_fin", 8, 6, 14),
    ("tail_fin_ridge", 1, 7, 8),
    ("fin", 1, 6, 7),
]
JIAOLONG_ALIASES = {
    "left_horn": "horn",
    "right_horn": "horn",
    "left_whisker": "whisker",
    "right_whisker": "whisker",
}

#: 排版结果（模块加载时算一次）
STEED_PLACED = pack(STEED_PARTS, STEED_TEXTURE)
EAGLE_PLACED = pack(EAGLE_PARTS, EAGLE_TEXTURE)
JIAOLONG_PLACED = pack(JIAOLONG_PARTS, JIAOLONG_TEXTURE)


def layout_of(placed: dict, aliases: dict) -> dict:
    """把排版结果展开成 name -> (u, v, w, h, d)，别名件直接指到本体坐标。"""
    out = dict(placed)
    for alias, base in aliases.items():
        out[alias] = placed[base]
    return out


STEED_LAYOUT = layout_of(STEED_PLACED, STEED_ALIASES)
EAGLE_LAYOUT = layout_of(EAGLE_PLACED, EAGLE_ALIASES)
JIAOLONG_LAYOUT = layout_of(JIAOLONG_PLACED, JIAOLONG_ALIASES)


# ---------------------------------------------------------------------------
# 画布
# ---------------------------------------------------------------------------

class Canvas:
    def __init__(self, width: int, height: int):
        self.width = width
        self.height = height
        self.img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.px = self.img.load()

    def put(self, x: int, y: int, c) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            # 颜色既可能是 '#rrggbb' 字符串、也可能是 (r,g,b) / (r,g,b,a)
            alpha = 255 if isinstance(c, str) else (int(c[3]) if len(c) > 3 else 255)
            self.px[x, y] = rgb(c) + (alpha,)

    def get(self, x: int, y: int, default=(0, 0, 0, 0)):
        if 0 <= x < self.width and 0 <= y < self.height:
            return self.px[x, y]
        return default

    def box(self, u: int, v: int, w: int, h: int, d: int, shader,
            shade_faces: dict | None = None) -> None:
        shade_faces = shade_faces or FACE_SHADE
        for face, (x0, y0, x1, y1) in face_rects(u, v, w, h, d).items():
            sh = shade_faces.get(face, 1.0)
            for y in range(y0, y1):
                for x in range(x0, x1):
                    c = shader(x, y, x - x0, y - y0, x1 - x0, y1 - y0, face, sh)
                    if c is not None:
                        self.put(x, y, c)

    def save(self, path: str) -> None:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path, "PNG")
        print(f"  -> {os.path.relpath(path, MOD_ROOT)}  ({self.width}×{self.height})")


# ---------------------------------------------------------------------------
# 颜色工具
# ---------------------------------------------------------------------------

def rgb(c):
    if isinstance(c, str):
        s = c.lstrip("#")
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))
    return tuple(c[:3])


def shade(c, f: float, a: int = 255):
    r, g, b = rgb(c)
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))),
            max(0, min(255, int(b * f))), a)


def mix(a, b, t: float):
    ar, ag, ab = rgb(a)
    br, bg, bb = rgb(b)
    return (int(ar + (br - ar) * t), int(ag + (bg - ag) * t),
            int(ab + (bb - ab) * t), 255)


def hsh(x: int, y: int, salt: int) -> int:
    h = (x * 374761393 + y * 668265263 + salt * 1442695040888963407) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h ^ (h >> 16)) & 0xFF


def noise(c, x: int, y: int, salt: int, amount: float = 0.06):
    n = (hsh(x, y, salt) / 255.0 - 0.5) * 2.0 * amount
    return shade(c, 1.0 + n)


def stroke_edges(c: Canvas) -> None:
    """给已有像素压一圈暗边（只在已有像素上操作，不扩张轮廓）。"""
    for y in range(c.height):
        for x in range(c.width):
            if c.get(x, y)[3] == 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if c.get(x + dx, y + dy)[3] == 0:
                    c.put(x, y, shade(c.get(x, y), 0.70))
                    break


# ---------------------------------------------------------------------------
# 1) 灵驹
# ---------------------------------------------------------------------------

STEED_PAL = dict(
    coat="#e6eef7", coat_dark="#a9bccf", belly="#f7fbff",
    hoof="#3e4750", horn="#e8d9a8", horn_dark="#a89463",
    mane="#7fd8e8", mane_dark="#3f93a8",
    eye="#46d6ff", muzzle="#c3ccd6",
)


def build_steed() -> Canvas:
    pal = STEED_PAL
    c = Canvas(*STEED_TEXTURE)
    L = STEED_LAYOUT

    def coat(x, y, lx, ly, lw, lh, face, sh, *, dark_top=False, belly=False):
        col = noise(pal["coat"], x, y, 11, 0.055)
        if (lx * 3 + ly * 2) % 7 == 0:
            col = shade(col, 0.90)
        if (lx + ly * 2) % 5 == 0:
            col = shade(col, 1.05)
        if dark_top and face == "top":
            col = noise(pal["coat_dark"], x, y, 12, 0.05)
        if belly and face == "bottom":
            col = noise(pal["belly"], x, y, 13, 0.04)
        return shade(col, sh)

    def head_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["coat"], x, y, 14, 0.055)
        if face == "front":
            if ly == 2 and lx in (0, lw - 1):
                col = pal["eye"]
            elif ly == 3 and lx in (0, lw - 1):
                col = shade(pal["eye"], 0.45)
            elif ly == 0 and 1 <= lx <= lw - 2:
                col = noise(pal["belly"], x, y, 15, 0.04)
            elif ly >= lh - 2 and lx == lw // 2:
                col = noise(pal["muzzle"], x, y, 16, 0.04)
        elif face == "top":
            col = noise(pal["coat_dark"], x, y, 17, 0.05)
        return shade(col, sh)

    def snout_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["muzzle"], x, y, 18, 0.05)
        if face == "front" and ly == 0 and lx in (0, lw - 1):
            col = shade(pal["hoof"], 1.15)
        return shade(col, sh)

    def horn_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["horn"], x, y, 19, 0.06)
        if ly % 2 == 1:
            col = noise(pal["horn_dark"], x, y, 20, 0.05)
        if face == "top":
            col = shade(pal["horn"], 1.15)
        return shade(col, sh)

    def mane_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["mane"], x, y, 21, 0.07)
        if ly % 3 == 0:
            col = mix(col, pal["belly"], 0.45)
        if lx == 0:
            col = noise(pal["mane_dark"], x, y, 22, 0.05)
        return shade(col, sh)

    def leg_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["coat_dark"], x, y, 23, 0.06)
        if ly >= lh - 2:
            col = noise(pal["hoof"], x, y, 24, 0.05)
        return shade(col, sh)

    def tail_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["mane"], x, y, 25, 0.07)
        if ly >= lh - 4:
            col = mix(col, pal["belly"], 0.5)
        if ly < 3:
            col = noise(pal["coat"], x, y, 26, 0.05)
        return shade(col, sh)

    def ear_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["coat"], x, y, 27, 0.06)
        if face == "front":
            col = noise(pal["coat_dark"], x, y, 28, 0.05)
        return shade(col, sh)

    c.box(*L["neck"], shader=coat)
    c.box(*L["head"], shader=head_shader)
    c.box(*L["snout"], shader=snout_shader)
    c.box(*L["ear"], shader=ear_shader)
    c.box(*L["antler"], shader=horn_shader)
    c.box(*L["antler_tip"], shader=horn_shader)
    c.box(*L["mane"], shader=mane_shader)
    c.box(*L["body"], shader=lambda x, y, lx, ly, lw, lh, f, s:
          coat(x, y, lx, ly, lw, lh, f, s, dark_top=True, belly=True))
    c.box(*L["haunch"], shader=lambda x, y, lx, ly, lw, lh, f, s:
          coat(x, y, lx, ly, lw, lh, f, s, dark_top=True))
    c.box(*L["tail"], shader=tail_shader)
    c.box(*L["leg"], shader=leg_shader)

    return c


# ---------------------------------------------------------------------------
# 2) 契约雷鹰
# ---------------------------------------------------------------------------

EAGLE_PAL = dict(
    feather="#4b6a92", feather_dark="#2b3f5c", belly="#cfe4f2",
    wing="#39527a", wing_edge="#8fe3ff", beak="#e8c25a",
    talon="#d8b45c", eye="#8fe3ff", spark="#bff4ff", crest="#9fd8ff",
)


def build_eagle() -> Canvas:
    pal = EAGLE_PAL
    c = Canvas(*EAGLE_TEXTURE)
    L = EAGLE_LAYOUT

    def body_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["feather"], x, y, 33, 0.06)
        if face == "bottom":
            col = noise(pal["belly"], x, y, 34, 0.05)
        if face == "top":
            # 背脊：鞍位那一块刻意压平一档，让"能骑"看得出来
            col = noise(pal["feather_dark"], x, y, 35, 0.05)
            if 2 <= ly <= 5:
                col = noise(pal["wing"], x, y, 36, 0.03)
        if face in ("right", "left") and ly % 4 == 0:
            col = shade(col, 0.92)
        return shade(col, sh)

    def neck_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["feather"], x, y, 31, 0.06)
        if (lx + ly * 2) % 5 == 0:
            col = shade(col, 0.88)
        if face == "top":
            col = noise(pal["feather_dark"], x, y, 32, 0.05)
        return shade(col, sh)

    def head_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["feather"], x, y, 37, 0.06)
        if face == "front":
            if ly == 2 and lx in (1, lw - 2):
                col = pal["eye"]
            elif ly == 3 and lx in (1, lw - 2):
                col = shade(pal["eye"], 0.4)
            elif ly == 1 and lx in (1, lw - 2):
                col = noise(pal["feather_dark"], x, y, 38, 0.04)
            elif ly >= lh - 2 and lw // 2 - 1 <= lx <= lw // 2:
                col = noise(pal["beak"], x, y, 39, 0.05)
        elif face == "top":
            col = noise(pal["feather_dark"], x, y, 40, 0.05)
        return shade(col, sh)

    def wing_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["wing"], x, y, 41, 0.06)
        # 外侧一道电光边：契约雷鹰的标志
        if face in ("top", "bottom") and lx >= lw - 1:
            col = pal["wing_edge"]
        elif face in ("front", "back") and ly >= lh - 1:
            col = pal["wing_edge"]
        if (lx * 2 + ly) % 4 == 0:
            col = shade(col, 0.9)
        return shade(col, sh)

    def wing_tip_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["wing"], x, y, 42, 0.06)
        if face in ("top", "bottom"):
            col = mix(col, pal["spark"], 0.25 + 0.35 * (lx / max(1, lw - 1)))
        if (lx + ly) % 3 == 0:
            col = shade(col, 0.9)
        return shade(col, sh)

    def beak_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["beak"], x, y, 43, 0.05)
        if face == "bottom":
            col = shade(col, 0.7)
        return shade(col, sh)

    def crest_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["crest"], x, y, 44, 0.07)
        if lx >= lw - 1:
            col = mix(col, pal["spark"], 0.5)
        return shade(col, sh)

    def tail_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["wing"], x, y, 45, 0.06)
        if face in ("top", "bottom") and lx >= lw - 1:
            col = pal["wing_edge"]
        return shade(col, sh)

    def leg_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["talon"], x, y, 46, 0.06)
        if ly % 3 == 2:
            col = shade(col, 0.82)
        return shade(col, sh)

    def talon_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["talon"], x, y, 47, 0.06)
        if face == "bottom":
            col = shade(pal["talon"], 1.2)
        return shade(col, sh)

    c.box(*L["body"], shader=body_shader)
    c.box(*L["neck"], shader=neck_shader)
    c.box(*L["head"], shader=head_shader)
    c.box(*L["beak"], shader=beak_shader)
    c.box(*L["crest"], shader=crest_shader)
    c.box(*L["tail"], shader=tail_shader)
    c.box(*L["wing"], shader=wing_shader)
    c.box(*L["wing_tip"], shader=wing_tip_shader)
    c.box(*L["leg"], shader=leg_shader)
    c.box(*L["talon"], shader=talon_shader)

    return c


# ---------------------------------------------------------------------------
# 3) 蛟龙
# ---------------------------------------------------------------------------

JIAOLONG_PAL = dict(
    scale="#2f6f6a", scale_dark="#17403f", scale_light="#5fb8a6",
    belly="#cfe8d8", fin="#3fa0a8", fin_edge="#7ff0e0",
    horn="#d8c078", horn_dark="#8f7a3c",
    whisker="#e8d9a8", eye="#ffd24a", glow="#64e8d8",
    belly_plate="#a8c8b0",
)


def build_jiaolong() -> Canvas:
    pal = JIAOLONG_PAL
    c = Canvas(*JIAOLONG_TEXTURE)
    L = JIAOLONG_LAYOUT

    def scale(x, y, lx, ly, lw, lh, face, sh, *, salt=51):
        col = noise(pal["scale"], x, y, salt, 0.06)
        if (lx + ly) % 4 == 0:
            col = noise(pal["scale_dark"], x, y, salt + 1, 0.05)
        if (lx - ly) % 4 == 0:
            col = shade(col, 1.06)
        return shade(col, sh)

    def body_shader(x, y, lx, ly, lw, lh, face, sh):
        if face == "bottom":
            # 腹甲：一片片横向的浅色板
            col = noise(pal["belly_plate"], x, y, 52, 0.05)
            if lx % 2 == 0:
                col = shade(col, 0.9)
            return shade(col, sh)
        return scale(x, y, lx, ly, lw, lh, face, sh)

    def head_shader(x, y, lx, ly, lw, lh, face, sh):
        col = scale(x, y, lx, ly, lw, lh, face, sh, salt=53)
        if face == "top":
            col = noise(pal["scale_dark"], x, y, 54, 0.05)
            if lx == lw // 2:
                col = mix(col, pal["fin_edge"], 0.35)
        if face == "bottom":
            col = noise(pal["belly"], x, y, 55, 0.05)
        if face in ("right", "left"):
            # 侧面一只竖瞳的龙眼
            if ly == 3 and lx in (0, 1):
                col = pal["eye"]
            elif ly in (2, 4):
                col = noise(pal["scale_dark"], x, y, 56, 0.04)
        return col

    def snout_shader(x, y, lx, ly, lw, lh, face, sh):
        col = scale(x, y, lx, ly, lw, lh, face, sh, salt=57)
        if face == "bottom":
            col = noise(pal["belly"], x, y, 58, 0.05)
        return col

    def jaw_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["scale_dark"], x, y, 59, 0.06)
        if face == "top":
            col = mix(pal["scale_dark"], "#7a2a2a", 0.6)
        return shade(col, sh)

    def horn_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["horn"], x, y, 60, 0.06)
        if ly % 2 == 1:
            col = noise(pal["horn_dark"], x, y, 61, 0.05)
        return shade(col, sh)

    def whisker_shader(x, y, lx, ly, lw, lh, face, sh):
        return shade(noise(pal["whisker"], x, y, 62, 0.08), sh)

    def fin_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fin"], x, y, 63, 0.06)
        if face in ("front", "back"):
            # 尾缘发亮：蛟龙飞起来的时候背鳍是荧光的
            col = mix(col, pal["fin_edge"], 0.35 + 0.4 * (ly / max(1, lh - 1)))
        if (lx + ly) % 3 == 0:
            col = shade(col, 0.9)
        return shade(col, sh)

    def tail_shader(x, y, lx, ly, lw, lh, face, sh):
        col = scale(x, y, lx, ly, lw, lh, face, sh, salt=64)
        if face == "bottom":
            col = noise(pal["belly"], x, y, 65, 0.05)
        if lx >= lw - 3:
            col = mix(col, pal["glow"], 0.45)
        return col

    def ridge_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fin"], x, y, 66, 0.07)
        if face in ("front", "back"):
            col = mix(col, pal["fin_edge"], 0.55)
        return shade(col, sh)

    c.box(*L["head"], shader=head_shader)
    c.box(*L["snout"], shader=snout_shader)
    c.box(*L["jaw"], shader=jaw_shader)
    c.box(*L["horn"], shader=horn_shader)
    c.box(*L["whisker"], shader=whisker_shader)
    c.box(*L["tail_fin"], shader=tail_shader)
    c.box(*L["tail_fin_ridge"], shader=ridge_shader)
    c.box(*L["fin"], shader=fin_shader)

    # 九节身体共用同一块 UV（Java 侧也是同一组 texOffs）；
    # 但每节叠一层按节号错开的确定性色偏，免得九节像素完全一样。
    seg = L["segment"]
    for i in range(JIAOLONG_SEGMENTS):
        tint = 0.94 + (i % 3) * 0.05
        c.box(*seg, shader=lambda x, y, lx, ly, lw, lh, f, s, _t=tint:
              shade(body_shader(x, y, lx, ly, lw, lh, f, s), _t))

    return c


# ---------------------------------------------------------------------------
# UV 排版自查
# ---------------------------------------------------------------------------

def unique_boxes(placed: dict):
    """只保留"本体"（别名件不重复参与重叠检查）。"""
    seen = {}
    for name, rect in placed.items():
        seen.setdefault(rect, name)
    return {name: rect for rect, name in seen.items()}


def check_layout(placed: dict, texture, aliases: dict, label: str = "",
                 expanded: dict | None = None) -> list:
    problems = []
    bodies = unique_boxes(placed)
    extents = {}
    for name, (u, v, w, h, d) in bodies.items():
        x0, y0, x1, y1 = uv_extent(u, v, w, h, d)
        if x0 < 0 or y0 < 0 or x1 > texture[0] or y1 > texture[1]:
            problems.append(f"{label}{name} UV 越界 {(x0, y0, x1, y1)} > {texture}")
        extents[name] = (x0, y0, x1, y1)

    # 别名件必须精确指到本体坐标（否则左右不对称，一边的耳/角会错位）
    table = expanded if expanded is not None else placed
    for alias, base in aliases.items():
        if table.get(alias) != placed.get(base):
            problems.append(f"{label}{alias} 的 UV 与 {base} 不一致")

    names = sorted(extents)
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            if overlap(extents[a], extents[b]):
                problems.append(f"{label}{a} {extents[a]} 与 {b} {extents[b]} 重叠")
    return problems


def check_layouts() -> None:
    print("== UV 排版自查 ==")
    problems = []
    problems += check_layout(STEED_PLACED, STEED_TEXTURE, STEED_ALIASES, "灵驹 ",
                             STEED_LAYOUT)
    problems += check_layout(EAGLE_PLACED, EAGLE_TEXTURE, EAGLE_ALIASES, "雷鹰 ",
                             EAGLE_LAYOUT)
    problems += check_layout(JIAOLONG_PLACED, JIAOLONG_TEXTURE, JIAOLONG_ALIASES,
                             "蛟龙 ", JIAOLONG_LAYOUT)
    if problems:
        print("发现问题：")
        for p in problems:
            print("  !", p)
        raise SystemExit(1)
    print(f"  · 灵驹：{len(STEED_PLACED)} 个部件填在 {STEED_TEXTURE[0]}×"
          f"{STEED_TEXTURE[1]} 内，两两不重叠")
    print(f"  · 雷鹰：{len(EAGLE_PLACED)} 个部件填在 {EAGLE_TEXTURE[0]}×"
          f"{EAGLE_TEXTURE[1]} 内，两两不重叠")
    print(f"  · 蛟龙：{len(JIAOLONG_PLACED)} 个部件填在 {JIAOLONG_TEXTURE[0]}×"
          f"{JIAOLONG_TEXTURE[1]} 内，其中九节身体有意共用同一块 UV")


def print_uv_tables() -> None:
    print("\n== Java 侧 texOffs 对照表（createBodyLayer 用）==")
    for label, placed in (("灵驹 SpiritSteedRenderer.SteedModel", STEED_PLACED),
                          ("雷鹰 ContractEagleRenderer.EagleModel", EAGLE_PLACED),
                          ("蛟龙 JiaolongRenderer.JiaolongModel", JIAOLONG_PLACED)):
        print(f"\n-- {label} --")
        for name, (u, v, w, h, d) in placed.items():
            print(f"   {name:18s} texOffs({u:3d}, {v:3d})"
                  f"   addBox w={w} h={h} d={d}")


# ---------------------------------------------------------------------------
# 入口
# ---------------------------------------------------------------------------

EXPECT = {
    "spirit_steed": (build_steed, STEED_TEXTURE),
    "contract_eagle": (build_eagle, EAGLE_TEXTURE),
    "jiaolong": (build_jiaolong, JIAOLONG_TEXTURE),
}


def verify_files(tex_dir: str) -> int:
    bad = 0
    for name, (_fn, size) in EXPECT.items():
        path = os.path.join(tex_dir, name + ".png")
        if not os.path.exists(path):
            print(f"  ! 缺少 {name}.png")
            bad += 1
            continue
        im = Image.open(path)
        opaque = sum(1 for p in im.convert("RGBA").get_flattened_data() if p[3] > 0)
        ok = im.size == size and im.mode == "RGBA" and opaque > 100
        print(f"  [{'OK ' if ok else 'BAD'}] {name + '.png':22s} "
              f"{im.size[0]}×{im.size[1]} {im.mode} 不透明像素={opaque}")
        if not ok:
            bad += 1
    return bad


def main() -> int:
    ap = argparse.ArgumentParser(description="生成灵兽（坐骑）实体贴图")
    ap.add_argument("--out", default=TEX_DIR)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--uv", action="store_true")
    args = ap.parse_args()

    check_layouts()
    if args.uv:
        print_uv_tables()
        return 0
    if args.check:
        print("\n== 现有贴图校验 ==")
        return 1 if verify_files(args.out) else 0

    print("\n== 生成贴图 ==")
    for name, (fn, _size) in EXPECT.items():
        fn().save(os.path.join(args.out, name + ".png"))

    print("\n== 结果校验 ==")
    bad = verify_files(args.out)
    print("\n完成。" if not bad else f"\n有 {bad} 个文件不合格。")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
