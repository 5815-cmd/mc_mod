#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
灵兽模型的等轴测预览（开发辅助，不进模组资源）。

它做的事：用 ``gen_beast_mount_textures.py`` 里那份 **UV 排版表**，
把模型盒子的三个可见面（top / right(+X) / front(+Z)）按等轴测投影画出来，
每个像素都从真实贴图里采样。

意义：贴图与 UV 是"数值对不上就只在游戏里表现为颜色糊掉"的那类问题，
肉眼看平铺贴图**看不出**对错——只有按 UV 把面拼回立体形状，
才能一眼判断脸是不是画在脸上、鬃毛有没有接到脖子。

用法::

    python tools/preview_mount_models.py            # 写 tools/preview/*.png
"""

from __future__ import annotations

import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import gen_beast_mount_textures as G  # noqa: E402  （复用同一份 UV 与配色）

OUT_DIR = os.path.join(HERE, "preview")
SCALE = 6


# ---------------------------------------------------------------------------
# 盒子定义：(名字, x, y, z, w, h, d)
#   x/y/z = 盒子的**最小角**（模型坐标，+y 向下，脚底在 y=24）
#   UV 从 G.*_LAYOUT 查；(u,v) 是 texOffs，(w,h,d) 必须与排版表一致
# ---------------------------------------------------------------------------

STEED_BOXES = [
    ("head", -2.5, 0, -5, 5, 6, 6),
    ("snout", -1.5, 5, -11, 3, 3, 6),
    ("ear", -3.5, -2, -5, 2, 2, 2),
    ("ear", 1.5, -2, -5, 2, 2, 2),
    ("antler", -2.5, -11, -4.5, 2, 5, 2),
    ("antler", 0.5, -11, -4.5, 2, 5, 2),
    ("antler_tip", -2.5, -13, -5.5, 2, 2, 3),
    ("antler_tip", 0.5, -13, -5.5, 2, 2, 3),
    ("neck", -2, 8, -7, 4, 8, 4),
    ("mane", -1, 6, -3, 2, 12, 3),
    ("body", -4, 5, -11, 8, 10, 10),
    ("haunch", -4.5, 5, 9, 9, 11, 8),
    ("tail", -1, 14, 14, 2, 10, 2),
    ("leg", -4, 15, -8, 2, 9, 2),
    ("leg", 2, 15, -8, 2, 9, 2),
    ("leg", -4, 15, 10, 2, 9, 2),
    ("leg", 2, 15, 10, 2, 9, 2),
]

EAGLE_BOXES = [
    ("neck", -2, 3, -7, 4, 6, 4),
    ("head", -3, -3, -8, 6, 6, 6),
    ("beak", -1.5, -1, -14, 3, 3, 6),
    ("crest", -2, -5, -7, 4, 2, 5),
    ("body", -4, 5, -6, 8, 10, 12),
    ("tail", -3, 12, 5, 6, 2, 12),
    ("wing", -7, 5, -5, 3, 2, 8),
    ("wing", 4, 5, -5, 3, 2, 8),
    ("wing_tip", -10, 5, -5, 3, 2, 8),
    ("wing_tip", 7, 5, -5, 3, 2, 8),
    ("leg", -3.5, 15, -1.5, 3, 6, 3),
    ("leg", 0.5, 15, -1.5, 3, 6, 3),
    ("talon", -4, 21, -3.5, 4, 3, 5),
    ("talon", 0, 21, -3.5, 4, 3, 5),
]


def jiaolong_boxes():
    boxes = [
        ("head", -4.5, -2, -12, 9, 8, 12),
        ("snout", -3, -1, -16, 6, 5, 4),
        ("jaw", -3.5, 6, -12, 7, 3, 10),
        ("horn", -5, -7, -3, 2, 6, 2),
        ("horn", 3, -7, -3, 2, 6, 2),
        ("whisker", -3.5, 0, -13, 1, 5, 1),
        ("whisker", 2.5, 0, -13, 1, 5, 1),
    ]
    z = 1.0
    for i in range(G.JIAOLONG_SEGMENTS):
        boxes.append(("segment", -3.5, 2, z, 7, 7, 7))
        if i in G.JIAOLONG_FIN_AT:
            boxes.append(("fin", -0.5, -4, z + 2, 1, 6, 7))
        z += 7
    boxes.append(("tail_fin", -4, 3, z, 8, 6, 14))
    boxes.append(("tail_fin_ridge", -0.5, -4, z + 4, 1, 7, 8))
    return boxes


# ---------------------------------------------------------------------------
# 等轴测投影
# ---------------------------------------------------------------------------

def project(x: float, y: float, z: float):
    """(x,y,z) -> 屏幕坐标（浮点）。+y 向下（模型坐标），所以 y 直接加。"""
    return (x - z, (x + z) * 0.5 + y)


FACE_SRC = {
    "top": lambda x, y, z, w, h, d: [
        (x, y, z + d), (x + w, y, z + d), (x + w, y, z), (x, y, z)],
    "front": lambda x, y, z, w, h, d: [
        (x, y, z + d), (x + w, y, z + d), (x + w, y + h, z + d), (x, y + h, z + d)],
    "right": lambda x, y, z, w, h, d: [
        (x + w, y, z + d), (x + w, y, z), (x + w, y + h, z), (x + w, y + h, z + d)],
}


def draw_box(img, px, tex, name, x, y, z, w, h, d, layout, ox, oy):
    u, v, bw, bh, bd = layout[name]
    assert (bw, bh, bd) == (w, h, d), f"{name} 尺寸与排版表不符：{(bw, bh, bd)} vs {(w, h, d)}"
    rects = G.face_rects(u, v, w, h, d)

    for face in ("top", "right", "front"):
        corners = FACE_SRC[face](x, y, z, w, h, d)
        pts = [project(*c) for c in corners]
        x0, y0, x1, y1 = rects[face]
        fw, fh = x1 - x0, y1 - y0
        if fw <= 0 or fh <= 0:
            continue
        # 双线性填充：平行四边形的两个方向向量 + 一个角
        a = pts[0]
        e1 = (pts[1][0] - a[0], pts[1][1] - a[1])
        e2 = (pts[3][0] - a[0], pts[3][1] - a[1])
        steps = max(2, int(max(abs(e1[0]), abs(e1[1]), abs(e2[0]), abs(e2[1])) * SCALE * 2))
        for i in range(steps + 1):
            for j in range(steps + 1):
                s, t = i / steps, j / steps
                sx = a[0] + e1[0] * s + e2[0] * t
                sy = a[1] + e1[1] * s + e2[1] * t
                tx = x0 + min(fw - 1, int(s * fw))
                ty = y0 + min(fh - 1, int(t * fh))
                c = tex.getpixel((tx, ty))
                if c[3] == 0:
                    continue
                sx_i = int(round(ox + sx * SCALE))
                sy_i = int(round(oy + sy * SCALE))
                if 0 <= sx_i < img.width and 0 <= sy_i < img.height:
                    img.putpixel((sx_i, sy_i), c)


def render(boxes, layout, texture_path, title):
    tex = Image.open(texture_path).convert("RGBA")
    pts = []
    for name, x, y, z, w, h, d in boxes:
        for face in ("top", "right", "front"):
            for c in FACE_SRC[face](x, y, z, w, h, d):
                pts.append(project(*c))
    minx = min(p[0] for p in pts)
    maxx = max(p[0] for p in pts)
    miny = min(p[1] for p in pts)
    maxy = max(p[1] for p in pts)
    pad = 8
    W = int((maxx - minx) * SCALE) + pad * 2
    H = int((maxy - miny) * SCALE) + pad * 2 + 14
    img = Image.new("RGBA", (W, H), (30, 32, 42, 255))

    ox = pad - minx * SCALE
    oy = pad - miny * SCALE + 14

    # 画家算法：z 大（远）的先画 —— 本投影里 (x+z) 越大越远，y 越大越靠下
    order = sorted(boxes, key=lambda b: (-(b[1] + b[3]), -(b[2] + b[5]), b[1] + b[2]))
    for name, x, y, z, w, h, d in order:
        draw_box(img, None, tex, name, x, y, z, w, h, d, layout, ox, oy)

    from PIL import ImageDraw
    dr = ImageDraw.Draw(img)
    dr.text((6, 3), title, fill=(230, 230, 240, 255))
    return img


def main() -> int:
    os.makedirs(OUT_DIR, exist_ok=True)
    jobs = [
        ("spirit_steed", STEED_BOXES, G.STEED_LAYOUT, "spirit_steed 灵驹"),
        ("contract_eagle", EAGLE_BOXES, G.EAGLE_LAYOUT, "contract_eagle 契约雷鹰"),
        ("jiaolong", jiaolong_boxes(), G.JIAOLONG_LAYOUT, "jiaolong 蛟龙"),
    ]
    outs = []
    for name, boxes, layout, title in jobs:
        tex = os.path.join(G.TEX_DIR, name + ".png")
        if not os.path.exists(tex):
            print(f"  ! 缺少贴图 {tex}（先跑 gen_beast_mount_textures.py）")
            return 1
        img = render(boxes, layout, tex, title)
        path = os.path.join(OUT_DIR, name + "_iso.png")
        img.save(path)
        outs.append(img)
        print(f"  -> {os.path.relpath(path, G.MOD_ROOT)}  {img.size[0]}×{img.size[1]}")

    # 拼成一张对比图
    W = sum(i.width for i in outs) + 20
    H = max(i.height for i in outs) + 20
    sheet = Image.new("RGBA", (W, H), (18, 18, 24, 255))
    x = 10
    for i in outs:
        sheet.alpha_composite(i, (x, 10))
        x += i.width + 10
    sheet_path = os.path.join(OUT_DIR, "mounts_sheet.png")
    sheet.save(sheet_path)
    print(f"  -> {os.path.relpath(sheet_path, G.MOD_ROOT)}  {sheet.size[0]}×{sheet.size[1]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
