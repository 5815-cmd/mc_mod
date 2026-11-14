#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
鸿蒙修仙 —— 六种灵植方块外观生成器（多层立体模型 + 高清像素贴图）
==================================================================

产出（24 个模型 + 24 张贴图 + 2 张预览，全部可重复生成、结果确定、不联网）::

    assets/hongmeng/models/block/<id>_stage<0..3>.json     24 个
    assets/hongmeng/textures/block/<id>_stage<0..3>.png    24 张（32x32 RGBA）

    tools/preview/herb_models_sheet.png   6 行(物种) x 4 列(阶段) 接触表，4 倍放大
    tools/preview/herb_models_iso.png     伪 3D 正交投影（线框 + 面填充）

六种灵植::

    xuelian_grass  雪莲草   冰蓝 + 白，莲花瓣层层包裹
    huoyan_hua     火焰花   橙红 + 金，火焰状花瓣向上收
    hanbing_teng   寒冰藤   青白，藤蔓缠绕 + 冰晶
    leiyin_cao     雷音草   紫 + 亮黄，细长草叶带电弧
    kongming_lan   空冥兰   深紫 + 银白，兰花形，悬空感
    xueluo_zhi     血珞芝   暗红 + 朱砂，灵芝伞盖 + 纹路

技术要点（都是踩过坑的地方）
---------------------------
1. 每个模型顶层写 ``"render_type": "minecraft:cutout"`` —— 不写透明像素会变纯黑。
2. **不**继承 ``minecraft:block/crop``：自己写 ``elements`` + ``display``。
3. 每个阶段 4~6 个 element：
     * 2 个对角交叉的竖直面（绕 Y 轴 45° / 135°）；
     * 2 个倾斜面（绕 X 轴 ±27~30°，``rescale: true``）做花瓣张开；
     * stage2 起加水平花冠面，stage3 再加一个 45° 顶盖面。
4. 竖直平面**一律不写 cullface**（0 厚度平面写 cullface 会被剔除消失），
   只有水平顶面写 ``cullface: up``。
5. 贴图切两条横带：``body``(y16..31) 给竖直面，``head``(y0..15) 给倾斜面/花冠，
   同一张 32x32 贴图被 4~6 个面引用的是**不同 UV 区域**，不是叠两遍同一张图。

用法::

    python tools/gen_herb_block_models.py            # 生成 + 校验 + 预览
    python tools/gen_herb_block_models.py --check     # 只校验（不写任何文件）
"""

from __future__ import annotations

import argparse
import json
import math
import os
import sys

try:
    from PIL import Image, ImageDraw
except ImportError:  # pragma: no cover
    sys.exit("需要 Pillow（PIL）。")

# Windows 控制台默认 GBK，中文 + 勾叉会直接抛 UnicodeEncodeError
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except Exception:  # noqa: BLE001
        pass

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
ASSETS = os.path.join(MOD_ROOT, "src", "main", "resources", "assets", "hongmeng")
MODEL_DIR = os.path.join(ASSETS, "models", "block")
TEX_DIR = os.path.join(ASSETS, "textures", "block")
PREVIEW_DIR = os.path.join(HERE, "preview")

TEX_W = TEX_H = 32     # 贴图 32x32（必须正方形：UV 空间 0..16 是方的，否则像素被拉长）
TEX_SIZE = (TEX_W, TEX_H)
QUAD = 16              # 2x2 象限，每个 16x16 = 一个精灵
BAND_H = 16            # 每条横带高 16px（下半 = 植株，上半 = 花头）
MAX_ELEM = 16.0        # MC 硬上限
# MC 方块模型 UV 永远按 0..16 归一化（与贴图分辨率无关！）——
# 32x32 铺满 0..16，所以 1 贴图像素 = 0.5 个 UV 单位。
UV_UNITS_PER_PX = MAX_ELEM / TEX_W         # = 0.5
# 四个象限中心（画布绝对 x）
COL_CX = (8.0, 24.0)

# ---------------------------------------------------------------------------
# 1. 调色板：每种灵植 base(主) / dark(暗) / light(亮) / accent(点睛) / glow(发光)
# ---------------------------------------------------------------------------

PALETTES = {
    # 雪莲草：冰蓝 + 白
    "xuelian_grass": dict(
        base=(168, 220, 245), dark=(74, 128, 176), light=(150, 205, 235),
        accent=(255, 255, 255), glow=(186, 236, 255),
        stem=(104, 152, 118), stem_dark=(52, 88, 66), stem_light=(150, 194, 154),
    ),
    # 火焰花：橙红 + 金
    "huoyan_hua": dict(
        base=(228, 96, 32), dark=(146, 38, 14), light=(255, 132, 44),
        accent=(255, 200, 76), glow=(255, 236, 150),
        stem=(96, 132, 64), stem_dark=(48, 74, 38), stem_light=(146, 178, 96),
    ),
    # 寒冰藤：青白
    "hanbing_teng": dict(
        base=(126, 198, 196), dark=(48, 104, 122), light=(196, 238, 238),
        accent=(226, 252, 255), glow=(132, 226, 255),
        stem=(92, 162, 150), stem_dark=(40, 92, 94), stem_light=(168, 220, 208),
    ),
    # 雷音草：紫 + 亮黄
    "leiyin_cao": dict(
        base=(124, 78, 190), dark=(62, 32, 108), light=(174, 118, 232),
        accent=(255, 224, 70), glow=(255, 246, 158),
        stem=(104, 74, 158), stem_dark=(52, 34, 88), stem_light=(164, 130, 214),
    ),
    # 空冥兰：深紫 + 银白
    "kongming_lan": dict(
        base=(112, 74, 172), dark=(50, 28, 90), light=(168, 118, 214),
        accent=(240, 242, 255), glow=(186, 152, 255),
        stem=(78, 66, 128), stem_dark=(38, 30, 70), stem_light=(140, 130, 186),
    ),
    # 血珞芝：暗红 + 朱砂
    "xueluo_zhi": dict(
        base=(156, 38, 42), dark=(84, 16, 22), light=(198, 66, 52),
        accent=(248, 214, 148), glow=(255, 126, 78),
        stem=(146, 114, 80), stem_dark=(72, 54, 38), stem_light=(196, 166, 124),
    ),
}

SPECIES = [
    ("xuelian_grass", "雪莲草"),
    ("huoyan_hua",    "火焰花"),
    ("hanbing_teng",  "寒冰藤"),
    ("leiyin_cao",    "雷音草"),
    ("kongming_lan",  "空冥兰"),
    ("xueluo_zhi",    "血珞芝"),
]

# ---------------------------------------------------------------------------
# 2. 像素画工具（小像素画布 + 笔刷 + 光照/描边后处理）
# ---------------------------------------------------------------------------

_DIRS8 = ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1))


def clamp8(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def shade_rgb(c, f):
    return clamp8(tuple(v * f for v in c))


class SpriteOverflow(Exception):
    """精灵画出了所属象限（会在贴图边缘被切掉，必须整体缩小重画）。"""

    def __init__(self, excess):
        super().__init__(f"sprite overflows quadrant by {excess:.2f}px")
        self.excess = excess


class Canvas:
    """小尺寸 RGBA 画布。px[x][y]，本地 y 向上生长（(0,0) 在左下角）。

    clip 非空时记录该象限的安全绘制区（含 y 向缩放），越界抛 SpriteOverflow。
    """

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[None] * h for _ in range(w)]
        self.special = set()
        self.clip = None          # dict(y0, y1, x0, x1)
        self.ys = 1.0             # y 向整体缩放（自适应缩小时用）

    def inside(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def _clip_y(self, y):
        if self.clip is None:
            return 0.0
        c = self.clip
        if y > c["y1"] + 1e-6:
            return y - c["y1"]
        if y < c["y0"] - 1e-6:
            return -(c["y0"] - y)
        return 0.0

    def put(self, x, y, c, special=False):
        if not self.inside(x, y):
            return
        # 只对 y 溢出报错（那是"植株高出象限"，要整体缩小重画）；
        # x 方向上瓣叶可能压线，轻微压线直接丢弃即可，不必整体缩小。
        if self._clip_y(y) > 0.5:
            raise SpriteOverflow(self._clip_y(y))
        if self.clip is not None:
            cx = self.clip
            if x < cx["x0"] or x > cx["x1"]:
                return
        self.px[x][y] = clamp8(c)
        if special:
            self.special.add((x, y))

    def disc(self, cx, cy, rx, ry, c, special=False):
        """椭圆（半径 <= 0.6 时也至少画 1px，不会画丢）。"""
        ry = ry * self.ys
        if rx <= 0 or ry <= 0:
            return
        rx = max(rx, 0.6)
        ry = max(ry, 0.6)
        x0, x1 = int(math.floor(cx - rx)), int(math.ceil(cx + rx))
        y0, y1 = int(math.floor(cy - ry)), int(math.ceil(cy + ry))
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                dx = (x + 0.5 - cx) / rx
                dy = (y + 0.5 - cy) / ry
                if dx * dx + dy * dy <= 1.0:
                    self.put(x, y, c, special)

    def stroke(self, pts, c, t0=1.6, t1=None, special=False):
        """折线笔刷：相邻控制点之间用竖直扫描线填充，t0/t1 为首尾半宽（支持锥形）。"""
        if t1 is None:
            t1 = t0
        pts = [(px, py * self.ys) for px, py in pts]
        if len(pts) == 1:
            self.disc(pts[0][0], pts[0][1], t0, t0, c, special)
            return
        seg_n = len(pts) - 1
        for i in range(seg_n):
            x0, y0 = pts[i]
            x1, y1 = pts[i + 1]
            t_a = t0 + (t1 - t0) * (i / seg_n)
            t_b = t0 + (t1 - t0) * ((i + 1) / seg_n)
            ya, yb = (y0, y1) if y0 <= y1 else (y1, y0)
            ta, tb = (t_a, t_b) if y0 <= y1 else (t_b, t_a)
            span = yb - ya
            for y in range(int(math.floor(ya)), int(math.ceil(yb)) + 1):
                f = 0.0 if span < 1e-6 else max(0.0, min(1.0, (y - ya) / span))
                t = ta + (tb - ta) * f
                cx = x0 + (x1 - x0) * f
                for x in range(int(math.floor(cx - t)), int(math.ceil(cx + t)) + 1):
                    if abs(x + 0.5 - cx) <= t:
                        self.put(x, y, c, special)

    def arc(self, cx, cy, rx, ry, a0, a1, c, t=1.0, steps=64, special=False):
        pts = []
        for i in range(steps + 1):
            a = math.radians(a0 + (a1 - a0) * i / steps)
            pts.append((cx + rx * math.cos(a), cy + ry * math.sin(a)))
        self.stroke(pts, c, t, t, special)

    def marks(self):
        return [(x, y) for x in range(self.w) for y in range(self.h)
                if self.px[x][y] is not None]

    def shade_outline(self, pal):
        """① 外描边（最暗档）② 上/左边缘高光 ③ 下/右边缘暗部 —— 只要 3 档，不做渐变。"""
        outline = shade_rgb(pal["dark"], 0.55)
        snap = [[self.px[x][y] for y in range(self.h)] for x in range(self.w)]

        def filled(x, y):
            return self.inside(x, y) and snap[x][y] is not None

        for x in range(self.w):
            for y in range(self.h):
                if snap[x][y] is None and any(filled(x + dx, y + dy) for dx, dy in _DIRS8):
                    self.put(x, y, outline)
        for x in range(self.w):
            for y in range(self.h):
                c = snap[x][y]
                if c is None or (x, y) in self.special:
                    continue
                if not filled(x, y + 1) or not filled(x - 1, y):
                    self.put(x, y, pal["light"])
                elif not filled(x, y - 1) or not filled(x + 1, y):
                    self.put(x, y, pal["dark"])

    def to_image(self, w, h):
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        for x in range(self.w):
            for y in range(self.h):
                c = self.px[x][y]
                if c is None:
                    continue
                iy = self.h - 1 - y          # 本地 y 向上 -> 图像 y 向下
                if 0 <= x < w and 0 <= iy < h:
                    img.putpixel((x, iy), (c[0], c[1], c[2], 255))
        return img


class Pen:
    """把横带的本地坐标（左下原点、向上生长）映射到 32x32 画布的绝对像素。"""

    def __init__(self, canvas, tile):
        self.c = canvas
        self.ox, self.oy = tile["ox"], tile["oy"]

    def disc(self, cx, cy, rx, ry, col, special=False):
        self.c.disc(self.ox + cx, self.oy + cy, rx, ry, col, special)

    def stroke(self, pts, col, t0=1.6, t1=None, special=False):
        self.c.stroke([(self.ox + p[0], self.oy + p[1]) for p in pts], col, t0, t1, special)

    def arc(self, cx, cy, rx, ry, a0, a1, col, t=1.0, special=False):
        self.c.arc(self.ox + cx, self.oy + cy, rx, ry, a0, a1, col, t, 64, special)

# ---------------------------------------------------------------------------
# 3. 六种灵植的画法
#    「阶段 = 形态变化」而不是"整体放大"：
#      stage0 破土幼苗（1~2 片子叶 + 土堆，很矮）
#      stage1 抽茎展叶（茎变高、叶片张开、出现物种特征色）
#      stage2 花苞成形（合拢的花苞 / 缠绕藤+冰晶 / 草叶+电弧 / 悬垂花序 / 伞盖初现）
#      stage3 完全绽放（层层花瓣 / 金红花冠 / 透明冰壳 / 亮黄电弧 / 银白花蕊 / 朱砂纹伞盖）
#    一张贴图里有 4 个精灵位（2x2 象限），每个位**画的内容不同**，绝无重复图案：
#      body  左下 = 完整植株（竖直交叉面用）
#      body  右下 = 完整植株的镜像叶序变体（倾斜面用）
#      flower 左上 = 单独的花头特写（花冠用）
#      flower 右上 = 花头 + 上部叶片 + 花茎（倾斜面 B / 顶盖用）
# ---------------------------------------------------------------------------

BASE_CX = 8.0          # 当前精灵象限的中轴（画布 x），渲染时按象限切换
BASE_K = 1.0           # 横向铺开系数（象限只有 16px 宽，收边用）
MODE = "body"          # body / flower / sprout：同一个物种的不同"取景"
VARIANT = 0            # 0/1：叶序镜像，保证同象限内不出现两块一样的花纹

# 精灵位 -> (象限左上角, 取景, 变体)：见下方 SLOT_* 常量


def sx(dx):
    """相对中轴 dx 的横向位置 -> 象限内局部坐标（Pen 会再加上象限原点）。

    象限原点在左上角，所以中轴是 QUAD/2 = 8，配合 BASE_K 做整体缩放。
    """
    return QUAD / 2.0 + dx * _kvar()


def _kvar():
    return BASE_K


# ---- 造型基元 -------------------------------------------------------------

def _h_stem(pen, h, pal, base_y=0.0, curve=0.0, t0=1.8, t1=1.0):
    pts = [(sx(curve * (i / 4.0)), base_y + (h - base_y) * i / 4.0) for i in range(5)]
    pen.stroke(pts, pal["stem"], t0, t1)
    pc = pal.get("stem_light", pal["stem"])
    pen.stroke([(x - 0.4, y) for x, y in pts], pc, t0 * 0.5, t1 * 0.4)
    return pts


def _h_leaf(pen, x0, y0, dx, dy, col, t0=1.6):
    """一片叶子：从中轴斜向外上方（dx, dy），宽度锥形收尖。"""
    x1, y1 = sx(x0 + dx * 0.55), y0 + dy * 0.55
    x2, y2 = sx(x0 + dx), y0 + dy
    pen.stroke([(sx(x0), y0), (x1, y1), (x2, y2)], col, t0, 0.6)


def _h_leafpair(pen, y, spread, pal, droop=-1.2, col=None, t0=1.6):
    col = col or pal["stem"]
    s = -1.0 if VARIANT else 1.0
    for k in (1.0, -1.0):
        _h_leaf(pen, 0.0, y + (0.0 if k * s > 0 else 0.5),
                k * s * spread, spread * 0.45 + (droop if k * s > 0 else droop * 0.5), col, t0)


def _h_soil(pen, pal):
    """破土的小土堆：让幼苗看起来是"从土里冒出来"的。"""
    for dx, w in ((-3, 2), (-2, 3), (-1, 3), (0, 4), (1, 3), (2, 3), (3, 2)):
        pen.disc(sx(dx), 0.3, w * 0.5, 1.1, pal["stem_dark"])
    for dx in (-2, 0, 2):
        pen.disc(sx(dx), 0.9, 1.0, 0.6, pal["stem"])


def _h_sprout(pen, pal, h=3.0):
    """破土幼苗：一小截嫩茎 + 两片子叶 + 顶端芽尖。"""
    _h_soil(pen, pal)
    _h_stem(pen, h, pal, base_y=0.6, t0=1.4, t1=1.0)
    _h_leafpair(pen, h * 0.70, 2.6, pal, droop=1.0, col=pal.get("stem_light", pal["stem"]), t0=1.5)
    tip = pal.get("accent", pal["light"])
    pen.disc(sx(0), h + 0.7, 1.3, 1.1, tip)


def _h_ring(pen, cy, rad, pal, col, pw=1.6, ry=0.66, angles=None):
    for a in (angles or (30, 70, 110, 150)):
        rad_a = math.radians(a)
        pen.stroke([(sx(0), cy),
                    (sx(rad * math.cos(rad_a)), cy + rad * ry * math.sin(rad_a))], col, pw, 0.7)


def _h_bloom_xuelian(pen, cy, scale, pal, stage):
    _h_ring(pen, cy, 3.1 * scale, pal, pal["base"], 1.8)
    _h_ring(pen, cy, (2.3 if stage >= 3 else 1.7) * scale, pal, pal["accent"], 1.7)
    _h_ring(pen, cy + 0.4, 1.1 * scale, pal, pal["light"], 1.4)
    pen.disc(sx(0), cy + 0.3, 1.7 * scale, 1.5 * scale, pal["accent"])
    if stage >= 3:
        pen.disc(sx(0), cy + 0.3, 0.9, 0.8, pal["glow"], special=True)
        _h_ring(pen, cy - 0.6, 3.9 * scale, pal, pal["base"], 1.4, 0.52, (45, 90, 135))


def _h_bud_xuelian(pen, cy, scale, pal):
    """合拢的莲瓣花苞：竖着的椭圆 + 竖向分瓣线。"""
    pen.disc(sx(0), cy, 1.9 * scale, 3.0 * scale, pal["accent"])
    for dx in (-1.1, 0.0, 1.1):
        pen.stroke([(sx(dx * scale), cy - 2.0 * scale), (sx(dx * scale), cy + 2.2 * scale)],
                   pal["base"], 0.7, 0.7)
    pen.disc(sx(0), cy + 2.6 * scale, 0.8, 0.7, pal["glow"], special=True)


def _h_bloom_huoyan(pen, cy, scale, pal, stage):
    n = 5 if stage >= 3 else 4
    ln = 4.6 * scale
    for i in range(n):
        f = i / (n - 1) * 2.0 - 1.0
        bx, by = sx(f * 2.1 * scale), cy - 1.4 * scale
        pen.stroke([(bx, by), (sx(f * 2.9 * scale), by + ln * 0.55),
                    (sx(f * 3.6 * scale), by + ln * (1.0 - 0.22 * abs(f)))],
                   pal["base"], 2.0, 0.6)
        pen.stroke([(bx, by + 0.6), (sx(f * 2.6 * scale), by + ln * 0.55),
                    (sx(f * 3.2 * scale), by + ln * (0.92 - 0.22 * abs(f)))],
                   pal["light"], 1.0, 0.5)
    pen.disc(sx(0), cy - 0.8 * scale, 1.9 * scale, 1.6 * scale, pal["accent"])
    if stage >= 3:
        pen.disc(sx(0), cy - 0.8 * scale, 1.0, 0.9, pal["glow"], special=True)


def _h_bud_huoyan(pen, cy, scale, pal):
    """向上收拢的火舌花苞。"""
    for f in (-0.7, 0.0, 0.7):
        pen.stroke([(sx(f * 1.7 * scale), cy - 1.4 * scale),
                    (sx(f * 2.3 * scale), cy + 1.6 * scale),
                    (sx(f * 1.8 * scale), cy + 3.6 * scale)],
                   pal["base"], 1.9, 0.6)
    pen.stroke([(sx(0), cy - 1.2 * scale), (sx(0), cy + 3.0 * scale)], pal["accent"], 1.0, 0.8)


def _h_bloom_hanbing(pen, cy, scale, pal, stage):
    r = 2.4 * scale
    _h_ring(pen, cy, r, pal, pal["base"], 1.6, 0.8, (0, 72, 144, 216, 288))
    _h_ring(pen, cy + 0.3, r * 0.62, pal, pal["accent"], 1.3, 0.8, (36, 108, 180, 252, 324))
    pen.disc(sx(0), cy + 0.3, 1.0, 0.9, pal["light"])
    if stage >= 3:
        # 成熟冰壳：一圈更亮更大的冰晶
        for a in (0, 60, 120, 180, 240, 300):
            ra = math.radians(a)
            px, py = sx((r + 1.5) * math.cos(ra)), cy + (r + 1.5) * 0.75 * math.sin(ra)
            pen.stroke([(px - 1.0, py), (px, py + 1.3), (px + 1.0, py),
                        (px, py - 1.3), (px - 1.0, py)], pal["accent"], 0.8, 0.8)


def _h_bud_hanbing(pen, cy, scale, pal):
    pen.disc(sx(0), cy, 1.5 * scale, 2.2 * scale, pal["base"])
    pen.disc(sx(0), cy + 0.6 * scale, 0.7, 0.7, pal["accent"])
    for f in (-1, 1):
        pen.stroke([(sx(f * 1.4 * scale), cy - 0.6 * scale), (sx(f * 2.4 * scale), cy + 1.6 * scale)],
                   pal["light"], 0.9, 0.9)


def _h_bloom_leiyin(pen, cy, scale, pal, stage):
    """草穗 + 电弧：成熟期电弧更亮更长。"""
    for f in (-1.0, -0.35, 0.35, 1.0):
        pen.stroke([(sx(f * 1.3 * scale), cy - 2.6 * scale),
                    (sx(f * 2.1 * scale), cy + 0.6 * scale),
                    (sx(f * 2.6 * scale), cy + 2.0 * scale)], pal["base"], 1.2, 0.5)
    top = cy + 2.4 * scale
    pen.stroke([(sx(-5.0 * scale), top), (sx(-1.2 * scale), top - 1.8 * scale),
                (sx(-3.2 * scale), top - 2.2 * scale), (sx(1.2 * scale), top - 4.2 * scale),
                (sx(-0.9 * scale), top - 4.6 * scale), (sx(4.2 * scale), top - 6.2 * scale)],
               pal["accent"], 1.1, 0.6, special=True)
    if stage >= 3:
        for dx, dy in ((-4.6, -1.6), (4.0, 0.6), (0.4, -2.6)):
            pen.disc(sx(dx * scale), top + dy * scale, 1.0, 1.0, pal["glow"], special=True)


def _h_bud_leiyin(pen, cy, scale, pal, stage):
    """抽穗期：紧贴的草叶 + 一道短电弧。"""
    for f in (-1.0, -0.3, 0.3, 1.0):
        pen.stroke([(sx(f * 1.1 * scale), cy - 3.0 * scale),
                    (sx(f * 1.7 * scale), cy + 1.8 * scale)], pal["base"], 1.3, 0.6)
    pen.stroke([(sx(-3.0 * scale), cy + 1.6 * scale), (sx(-0.6 * scale), cy + 0.4 * scale),
                (sx(-2.0 * scale), cy + 0.0), (sx(1.6 * scale), cy - 1.6 * scale)],
               pal["accent"], 1.0, 0.6, special=True)


def _h_bloom_kongming(pen, cy, scale, pal, stage):
    """悬垂花序：两列下垂的小花。"""
    hw = 3.4 * scale
    pen.stroke([(sx(-hw), cy + 2.4 * scale), (sx(-hw), cy - 1.2 * scale)], pal["base"], 1.3, 0.9)
    pen.stroke([(sx(hw), cy + 2.0 * scale), (sx(hw), cy - 1.6 * scale)], pal["base"], 1.3, 0.9)
    n = 3 if stage >= 3 else 2
    for i in range(n):
        dy = cy + 1.8 * scale - i * 1.7 * scale
        for f in (-1, 1):
            px, py = sx(f * hw), dy
            pen.disc(px, py, 1.2, 1.3, pal["accent"])
            pen.disc(px, py - 0.7, 0.7, 0.7, pal["light"])
    pen.disc(sx(0), cy + 2.9 * scale, 1.3 * scale, 1.1 * scale, pal["accent"])
    if stage >= 3:
        pen.disc(sx(0), cy + 2.9 * scale, 0.8, 0.7, pal["glow"], special=True)
        for f in (-1, 1):
            pen.disc(sx(f * (hw + 1.2)), cy - 2.4 * scale, 0.8, 0.8, pal["glow"], special=True)


def _h_bud_kongming(pen, cy, scale, pal):
    """初生花序：一小串合拢的花苞，向下垂。"""
    pen.stroke([(sx(-1.6 * scale), cy + 2.2 * scale), (sx(-2.4 * scale), cy - 1.4 * scale)],
               pal["base"], 1.5, 1.1)
    pen.stroke([(sx(1.6 * scale), cy + 1.8 * scale), (sx(2.4 * scale), cy - 1.0 * scale)],
               pal["base"], 1.5, 1.1)
    for i, f in enumerate((-1, 1)):
        pen.disc(sx(f * 2.1 * scale), cy + (0.6 - i * 1.2) * scale, 1.3, 1.5, pal["accent"], )
    pen.disc(sx(0), cy + 2.6 * scale, 1.4, 1.2, pal["light"])


def _h_bloom_xueluo(pen, cy, scale, pal, stage):
    """灵芝伞盖：宽扁的盖 + 同心纹路 + 朱砂斑。"""
    r = 4.2 * scale
    pen.disc(sx(0), cy, r, r * 0.50, pal["base"])
    pen.disc(sx(0), cy + r * 0.26, r * 0.94, r * 0.30, pal["dark"])
    pen.arc(sx(0), cy, r * 0.62, r * 0.32, 10, 170, pal["dark"], 0.9)
    if stage >= 3:
        pen.arc(sx(0), cy, r * 0.34, r * 0.18, 10, 170, pal["light"], 0.9)
    for f in (-0.62, 0.0, 0.62):
        pen.disc(sx(f * r), cy + r * 0.30, 0.9, 0.7, pal["accent"])
    for f in (-1, 1):
        pen.stroke([(sx(f * r * 0.86), cy + r * 0.16), (sx(f * (r + 1.2)), cy + r * 0.34)],
                   pal["light"], 1.0, 0.6)
    if stage >= 3:
        pen.disc(sx(0), cy + r * 0.62, 1.0, 0.9, pal["glow"], special=True)


def _h_bud_xueluo(pen, cy, scale, pal):
    """伞盖初现：小圆盖半张开。"""
    pen.disc(sx(0), cy, 2.3 * scale, 1.5 * scale, pal["base"])
    pen.disc(sx(0), cy + 0.8 * scale, 1.7 * scale, 0.9 * scale, pal["light"])
    for f in (-1, 1):
        pen.stroke([(sx(f * 1.8 * scale), cy + 0.4 * scale), (sx(f * 2.8 * scale), cy + 1.4 * scale)],
                   pal["accent"], 0.9, 0.9)


def _h_bud_xueluo_small(pen, cy, scale, pal):
    """刚冒头的菌盖：比 stage0 的芽大一圈，还没张开。"""
    pen.disc(sx(0), cy, 2.0 * scale, 1.8 * scale, pal["base"])
    pen.disc(sx(0), cy + 0.9 * scale, 1.2 * scale, 0.9 * scale, pal["light"])
    for f in (-1, 1):
        pen.disc(sx(f * 2.4 * scale), cy - 0.4 * scale, 0.9, 0.7, pal["dark"])


# ---- 六种灵植的参数表 -----------------------------------------------------
# stages[i] = dict(叶数, 叶展, 叶宽, 茎粗, 花苞类型/花型, 花相对高度, 花大小)

SPECIES_MORPH = {
    "xuelian_grass": dict(
        stem_h=[2.2, 7.0, 9.0, 11.0], color_from=1,
        stages=[
            dict(leaves=0, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=2, spread=4.6, lw=1.4, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=3, spread=5.6, lw=1.5, bud="xuelian", flower=2.4, fscale=1.0),
            dict(leaves=4, spread=6.4, lw=1.6, bud="bloom_xuelian", flower=2.6, fscale=1.15),
        ]),
    "huoyan_hua": dict(
        stem_h=[2.4, 6.0, 8.0, 10.0], color_from=1,
        stages=[
            dict(leaves=0, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=2, spread=4.6, lw=1.5, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=2, spread=5.4, lw=1.5, bud="huoyan", flower=2.0, fscale=1.0),
            dict(leaves=3, spread=6.0, lw=1.6, bud="bloom_huoyan", flower=2.2, fscale=1.1),
        ]),
    "hanbing_teng": dict(
        stem_h=[3.0, 7.0, 10.0, 12.0], color_from=1, climber=True,
        stages=[
            dict(leaves=0, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=2, spread=4.4, lw=1.3, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=3, spread=5.4, lw=1.4, bud="hanbing", flower=2.2, fscale=1.0),
            dict(leaves=4, spread=6.2, lw=1.5, bud="bloom_hanbing", flower=2.4, fscale=1.1),
        ]),
    "leiyin_cao": dict(
        stem_h=[2.6, 6.0, 8.6, 10.2], color_from=1, blades=True,
        stages=[
            dict(leaves=0, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=2, spread=4.6, lw=1.6, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=3, spread=5.6, lw=1.7, bud="leiyin", flower=1.1, fscale=1.0),
            dict(leaves=4, spread=6.6, lw=1.9, bud="bloom_leiyin", flower=1.2, fscale=1.05),
        ]),
    "kongming_lan": dict(
        stem_h=[2.2, 5.8, 8.5, 10.5], color_from=1,
        stages=[
            dict(leaves=0, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=2, spread=4.4, lw=1.3, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=3, spread=5.4, lw=1.4, bud="kongming", flower=1.6, fscale=1.0),
            dict(leaves=4, spread=6.2, lw=1.5, bud="bloom_kongming", flower=2.0, fscale=1.1),
        ]),
    "xueluo_zhi": dict(
        stem_h=[2.2, 5.2, 7.0, 8.5], color_from=1, thick=True, no_leaf=True,
        stages=[
            dict(leaves=0, bud=None, flower=0.0, fscale=1.0),
            dict(leaves=0, bud="xueluo_small", flower=1.2, fscale=1.0),
            dict(leaves=0, bud="xueluo", flower=1.2, fscale=1.0),
            dict(leaves=0, bud="bloom_xueluo", flower=1.4, fscale=1.15),
        ]),
}


def _h_foliage(pen, st, pal, mode):
    """按物种形态画叶片；蘑菇类不长叶。"""
    n = 0 if mode == "sprout" else st.get("leaves", 0)
    if not n:
        return
    spread = st.get("spread", 5.0) * BASE_K
    lw = st.get("lw", 1.5)
    base = pal["stem"] if MODE_COLORED() else pal.get("stem_light", pal["stem"])
    for i in range(n):
        y = 1.0 + i * 1.6
        w = spread * (1.0 if i < n - 1 else 0.78)
        _h_leafpair(pen, y, w, pal, droop=-1.0 - 0.3 * i, col=base, t0=lw)


def MODE_COLORED():
    """stage>=1 起才用物种特征色（幼苗统一是嫩茎色）。"""
    return _STAGE >= SPECIES_MORPH[_SID]["color_from"]


_SID = "xuelian_grass"
_STAGE = 0


def draw_plant(pen, sid, stage, mode="body", variant=0):
    """核心画法：按物种 + 阶段 + 取景画一株。"""
    global _SID, _STAGE, MODE, VARIANT
    _SID, _STAGE, MODE, VARIANT = sid, stage, mode, variant
    morph = SPECIES_MORPH[sid]
    pal = PALETTES[sid]
    st = morph["stages"][stage]
    h = morph["stem_h"][stage]

    if mode == "sprout" or stage == 0:
        _h_sprout(pen, pal, h)
        if stage == 0 and morph.get("thick"):
            # 灵芝类的幼苗：一小截菌柄 + 圆头
            pen.disc(sx(0), h + 1.0, 1.7, 1.3, pal["base"])
        return

    if mode == "flower":
        # 花头特写：花茎从下往上 + 上部叶 + 花
        base_y = 0.6
        _h_stem(pen, h * 0.86, pal, base_y=base_y,
                t0=2.1 if morph.get("thick") else 1.6, t1=1.1)
        if not morph.get("no_leaf"):
            for i in range(2):
                _h_leafpair(pen, base_y + 1.4 + i * 1.7,
                            (st.get("spread", 5.0) * 0.92 + i * 0.8) * BASE_K,
                            pal, droop=-1.0, col=pal["stem"], t0=st.get("lw", 1.5))
        fy = min(h * 0.86 + st.get("flower", 1.8), 8.8)
        fs = st.get("fscale", 1.0) * 1.45
    else:
        if morph.get("climber"):
            _draw_climber(pen, h, pal, st)
        else:
            _h_stem(pen, h, pal, t0=2.2 if morph.get("thick") else 1.8, t1=1.1)
            _h_foliage(pen, st, pal, mode)
            if morph.get("blades"):
                _draw_blades(pen, h, pal, st)
        # 花/花苞高度封顶：保证任何阶段的植株都完整落在 16px 象限内（不会被切也不会被缩）
        fy = min(h + st.get("flower", 1.8), 10.5)
        fs = st.get("fscale", 1.0)

    bud = st.get("bud")
    if not bud:
        return
    bud_scale = fs * (1.25 if mode == "flower" else 1.0)
    if bud.startswith("bloom_"):
        kind = bud.split("_", 1)[1]
        fn = globals()[f"_h_bloom_{kind}"]
        fn(pen, fy, fs, pal, stage)
    else:
        fn = globals()[f"_h_bud_{bud}"]
        if bud == "leiyin":
            fn(pen, fy, bud_scale, pal, stage)
        else:
            fn(pen, fy, bud_scale, pal)


def _draw_blades(pen, h, pal, st):
    """雷音草：细长草叶从基部向外张开。"""
    n = max(2, st.get("leaves", 2) + 2)
    for i in range(n):
        f = (i / (n - 1) * 2.0 - 1.0) if n > 1 else 0.0
        bh = h * (0.62 + 0.38 * (1.0 - abs(f) * 0.5))
        pen.stroke([(sx(f * 1.4), 0.7), (sx(f * 1.4 + f * 3.0), bh * 0.55),
                    (sx(f * 1.4 + f * 4.6), bh)], pal["base"], 1.5, 0.5)


def _draw_climber(pen, h, pal, st):
    """寒冰藤：主茎 + 缠绕的之字藤 + 冰晶。"""
    _h_stem(pen, h * 0.88, pal, t0=1.3, t1=0.9)
    amp = 4.2 * BASE_K
    steps = 6
    pts = []
    for i in range(steps + 1):
        t = i / steps
        pts.append((sx(amp * math.sin(t * math.pi * 1.7 + (0.0 if not VARIANT else math.pi))),
                    h * 0.10 + t * h * 0.86))
    pen.stroke(pts, pal["base"], 1.4, 1.0)
    for i in range(1, len(pts) - 1):
        x, y = pts[i]
        for s in (-1, 1):
            pen.stroke([(x, y), (x + s * 3.0 * BASE_K, y + 1.3),
                        (x + s * 4.2 * BASE_K, y + 0.3)], pal["stem"], 1.1, 0.5)
    # 冰晶
    for i in (1, 3, 5):
        x, y = pts[i]
        s = -1 if i % 4 == 1 else 1
        cxp, cyp = x + s * 2.6 * BASE_K, y + 1.0
        pen.stroke([(cxp - 1.8, cyp), (cxp, cyp + 2.4), (cxp + 1.8, cyp),
                    (cxp, cyp - 2.4), (cxp - 1.8, cyp)], pal["accent"], 0.9, 0.9)


# 每个精灵位 -> (取景模式, 变体, 是否幼苗特写)
def draw_slot(pen, sid, stage, slot):
    """按精灵位取景画一株；四个位内容各不相同（绝无两块重复花纹）。"""
    if stage == 0:
        if slot == SLOT_FLOWER:
            _draw_sprout_big(pen, sid)
        else:
            draw_plant(pen, sid, stage, "sprout", 1 if slot in (SLOT_BODY_M, SLOT_TOP) else 0)
        return
    if slot == SLOT_BODY_V:
        draw_plant(pen, sid, stage, "body", 0)
    elif slot == SLOT_BODY_M:
        draw_plant(pen, sid, stage, "body", 1)
    elif slot == SLOT_FLOWER:
        draw_plant(pen, sid, stage, "flower", 0)
    else:
        draw_plant(pen, sid, stage, "flower", 1)


def _draw_sprout_big(pen, sid):
    """幼苗位 2：把嫩芽画大一点（花冠用），保留土堆 + 子叶。"""
    pal = PALETTES[sid]
    h = SPECIES_MORPH[sid]["stem_h"][0] * 1.5
    _h_sprout(pen, pal, h)
    if SPECIES_MORPH[sid].get("thick"):
        pen.disc(sx(0), h + 1.0, 2.1, 1.6, pal["base"])
    else:
        pen.disc(sx(0), h + 0.9, 1.5, 1.3, pal.get("accent", pal["light"]))


# 精灵位编号：0=左下(竖直面) 1=右下(倾斜面) 2=左上(花冠) 3=右上(倾斜面B/顶盖)
SLOT_BODY_V, SLOT_BODY_M, SLOT_FLOWER, SLOT_TOP = 0, 1, 2, 3
SLOT_LIST = (SLOT_BODY_V, SLOT_BODY_M, SLOT_FLOWER, SLOT_TOP)

# 视觉密度：留白太少会糊成一团。每个取景的目标占位（相对象限高度，1.0 = 顶满）
SLOT_FILL = {SLOT_BODY_V: 0.78, SLOT_BODY_M: 0.78, SLOT_FLOWER: 0.68, SLOT_TOP: 0.68}

# ---------------------------------------------------------------------------
# 4. 贴图布局与 UV 映射
#    贴图分两条横带：body = 图 y16..31（画在地上的主株：茎+叶+花），
#                    head = 图 y0..15 （花头特写，给倾斜面/花冠/顶盖用）。
#    每个 element 只取**其中一条带的一个横向区间**，并且只映射到模型的一段高度：
#      竖直面 -> body 带，映射模型 y 0..(株高)
#      倾斜面 -> head 带，映射模型 y (株高*0.62)..16
#    这样 4 个面各自显示不同的贴图内容，不会出现"同一张图叠好几遍"。
# ---------------------------------------------------------------------------

BANDS = {
    "body": dict(ox=0, oy=QUAD, h=QUAD),   # 画布下半（绝对 y 16..31），植株本体
    "head": dict(ox=0, oy=0, h=QUAD),      # 画布上半（绝对 y 0..15），花头
}
# 每条带的上边缘在 UV 空间的 v
BAND_V_TOP = {"body": QUAD * UV_UNITS_PER_PX, "head": 0.0}


def band_v0(band):
    """该带"画布本地 y=0"（画布内部底边）处的 v。"""
    return (BANDS[band]["oy"] + QUAD) * UV_UNITS_PER_PX


# 画布像素窗口（2x2 象限，带 1px 边距；左上角=0）
WIN_BODY_L = (1.0, 15.0)
WIN_BODY_R = (17.0, 31.0)
WIN_HEAD_L = (1.0, 15.0)
WIN_HEAD_R = (17.0, 31.0)
WIN_MID = (17.0, 29.0)


def uv_window(band, x0, x1, y0_model, y1_model, cy0=0.0, cy1=16.0):
    """画布窗口 [x0,x1] × [cy0,cy1] -> 模型高度 [y0_model, y1_model] 的 UV 矩形。

    贴图坐标 -> UV 是"参数化"的：画布像素 (x, y)（y 向上）落在 UV
        u = x * UV_UNITS_PER_PX
        v = band_v0(band) - y * UV_UNITS_PER_PX      （v 轴向下，与图像 y 同向）
    也就是说画多少像素高的植株，在模型里就是多少格高（不拉伸）。
    """
    s = UV_UNITS_PER_PX
    v0 = band_v0(band)
    v_hi = v0 - cy0 * s
    v_lo = v0 - cy1 * s
    return [round(x0 * s, 4), round(v_lo, 4), round(x1 * s, 4), round(v_hi, 4)]


# element -> (band, texture key, 横向窗口)；纵向窗口见下面每个元素的 UV_*
TILES = {
    0: ("body", "t_body", WIN_BODY_L),   # 左下象限：完整植株（竖直交叉面）
    1: ("body", "t_cell", WIN_BODY_R),   # 右下象限：镜像叶序植株（倾斜面 A）
    2: ("head", "t_side", WIN_HEAD_L),   # 左上象限：花头（倾斜面 B）
    3: ("head", "t_head", WIN_HEAD_R),   # 右上象限：花头变体（花冠）
    4: ("head", "t_cap",  WIN_MID),      # 右上象限中间一块：花心（顶盖）
}

# 各 element 的 UV 纵向窗口（画布本地 y 向上 -> 模型高度）
#   body 带 y0..15 = 植株全高（贴地）
#   head 带 y4..16 = 花头那一段（悬在上部）
UV_BODY = (0.0, 15.0)
UV_HEAD = (4.0, 16.0)
UV_HEAD_MID = (9.0, 16.0)

# 各阶段每个 element 用哪块 UV（None = 该阶段没有这个 element）
STAGE_TILE = {
    0: dict(p0=0, p1=1, p2=2, p3=2, crow=None, cap=None),
    1: dict(p0=0, p1=1, p2=2, p3=2, crow=None, cap=None),
    2: dict(p0=0, p1=1, p2=2, p3=3, crow=3,  cap=None),
    3: dict(p0=0, p1=1, p2=2, p3=3, crow=3,  cap=4),
}


def _uv_for(tile_idx, y0_model, y1_model, win):
    band, key, (x0, x1) = TILES[tile_idx]
    return key, uv_window(band, x0, x1, y0_model, y1_model, win[0], win[1])


# ---------------------------------------------------------------------------
# 5. 几何：交叉竖直面 + 倾斜面 + 水平花冠 / 顶盖
# ---------------------------------------------------------------------------

# 植株在 16px 象限里大约占满 2~14（中轴 ±6），所以模型平面也按这个比例定宽：
# 贴图像素 -> 模型格 是 1:1，平面 12 格宽 + 竖直面 16 格高，比例才对得上。
PLANE_W = 6.5   # 竖直面半宽
TILT_W = 5.0    # 倾斜面半宽
CROW_R = 4.6    # 花冠半宽

# 每个阶段的几何；(h=竖直面高, t=倾斜面高, tilt=外倾角, cy=花冠高度, by=顶盖高度)
# 植株"长大"由贴图里长高体现，几何只在成熟期多出花冠/顶盖两个面。
STAGE_GEOM = {
    0: dict(h=16.0, t=16.0, tilt=26.0, cy=12.0, by=13.0, lift=0.0),
    1: dict(h=16.0, t=16.0, tilt=27.0, cy=12.4, by=13.4, lift=0.2),
    2: dict(h=16.0, t=16.0, tilt=28.0, cy=12.8, by=13.8, lift=0.4),
    3: dict(h=16.0, t=16.0, tilt=29.0, cy=13.0, by=14.2, lift=0.6),
}


def _flip_u(uv):
    """背面用的 UV：u 反向，避免从另一侧看到镜像的植株。"""
    return [uv[2], uv[1], uv[0], uv[3]]


def _vertical_plane(name, angle, h, tex_key, uv, lift=0.0):
    """沿 X 轴的竖直平面（法线指向 Z），绕 Y 轴转 angle。不写 cullface。"""
    return {
        "name": name,
        "from": [round(8 - PLANE_W, 3), round(lift, 3), 8],
        "to":   [round(8 + PLANE_W, 3), round(min(MAX_ELEM, lift + h), 3), 8],
        "faces": {
            "north": {"uv": _flip_u(uv), "texture": tex_key},
            "south": {"uv": uv, "texture": tex_key},
        },
        "rotation": {"origin": [8, 0, 8], "axis": "y", "angle": angle, "rescale": True},
    }


def _cross_plane(name, angle, h, tex_key, uv, lift=0.0):
    """沿 Z 轴的竖直平面（法线指向 X），绕 Y 轴转 angle。不写 cullface。"""
    return {
        "name": name,
        "from": [8, round(lift, 3), round(8 - PLANE_W, 3)],
        "to":   [8, round(min(MAX_ELEM, lift + h), 3), round(8 + PLANE_W, 3)],
        "faces": {
            "east": {"uv": uv, "texture": tex_key},
            "west": {"uv": _flip_u(uv), "texture": tex_key},
        },
        "rotation": {"origin": [8, 0, 8], "axis": "y", "angle": angle, "rescale": True},
    }


def _tilt_plane(name, angle, h, tex_key, uv):
    """倾斜面：竖直平面绕 X 轴向外倒 angle（底边贴地、向外张开）。"""
    return {
        "name": name,
        "from": [round(8 - TILT_W, 3), 0, 8],
        "to":   [round(8 + TILT_W, 3), round(min(MAX_ELEM, h), 3), 8],
        "faces": {
            "north": {"uv": _flip_u(uv), "texture": tex_key},
            "south": {"uv": uv, "texture": tex_key},
        },
        "rotation": {"origin": [8, 0, 8], "axis": "x", "angle": angle, "rescale": True},
    }


def _crown(name, cy, tex_key, uv):
    """水平花冠面：俯视看是一片花瓣（只有 up 面）。"""
    return {
        "name": name,
        "from": [round(8 - CROW_R, 3), round(cy, 3), round(8 - CROW_R, 3)],
        "to":   [round(8 + CROW_R, 3), round(cy + 0.05, 3), round(8 + CROW_R, 3)],
        "faces": {"up": {"uv": uv, "texture": tex_key, "cullface": "up"}},
    }


def _cap_plane(name, by, tex_key, uv):
    """顶盖：绕 Y 轴 45° 的细长条，做花心的收口（比花冠更高、更小）。"""
    return {
        "name": name,
        "from": [round(8 - CROW_R, 3), round(by, 3), round(8 - 0.5, 3)],
        "to":   [round(8 + CROW_R, 3), round(by + 0.05, 3), round(8 + 0.5, 3)],
        "faces": {"up": {"uv": uv, "texture": tex_key}},
        "rotation": {"origin": [8, by, 8], "axis": "y", "angle": 45, "rescale": True},
    }


def build_model(sid, stage):
    g = STAGE_GEOM[stage]
    spec = STAGE_TILE[stage]
    tex = {}
    els = []

    def uv(tile_idx, win, y0m, y1m):
        key, rect = _uv_for(tile_idx, y0m, y1m, win)
        tex[key] = f"hongmeng:block/{sid}_stage{stage}"
        return key, rect

    # 竖直交叉面 / 倾斜面：body 带全高 -> 模型 y 0..15（各自不同横向窗口）
    k0, uv0 = uv(spec["p0"], UV_BODY, 0.0, 15.0)
    k1, uv1 = uv(spec["p1"], UV_BODY, 0.0, 15.0)
    # 倾斜面：head 带的花头那一段 -> 模型上部（悬空的花朵，不在土里）
    k2, uv2 = uv(spec["p2"], UV_HEAD, 6.0, 16.0)
    k3, uv3 = uv(spec["p3"], UV_HEAD, 6.0, 16.0)

    els.append(_vertical_plane("plane_ns", 45.0, g["h"], k0, uv0, g["lift"]))
    els.append(_cross_plane("plane_we", 135.0, g["h"], k1, uv1, g["lift"]))
    els.append(_tilt_plane("tilt_a", -g["tilt"], g["t"], k2, uv2))
    els.append(_tilt_plane("tilt_b", g["tilt"], g["t"], k3, uv3))

    if spec["crow"] is not None:
        kc, uvc = uv(spec["crow"], UV_HEAD, 6.0, 16.0)
        els.append(_crown("crown", g["cy"], kc, uvc))
    if spec["cap"] is not None:
        kk, uvk = uv(spec["cap"], UV_HEAD_MID, 8.0, 16.0)
        els.append(_cap_plane("cap", g["by"], kk, uvk))

    return {
        "render_type": "minecraft:cutout",
        "ambientocclusion": False,
        "textures": tex,
        "elements": els,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0],
                    "scale": [0.625, 0.625, 0.625]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0],
                      "scale": [0.5, 0.5, 0.5]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0],
                       "scale": [0.25, 0.25, 0.25]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2.5, 0],
                                      "scale": [0.4, 0.4, 0.4]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                      "scale": [0.375, 0.375, 0.375]},
        },
    }


# ---------------------------------------------------------------------------
# 6. 生成贴图
# ---------------------------------------------------------------------------

def _fit_slot(sid, stage, slot, tile):
    """二分找"能完整画进象限的最大统一缩放"（x/y 同比缩，形状不变形）。

    用测量的方式而不是估算：画一遍就知道会不会溢出，避免成熟的植株反而变小。
    """
    def works(s):
        global BASE_K
        staging = Canvas(TEX_W, TEX_H)
        staging.ys = s
        BASE_K = s
        staging.clip = dict(y0=float(tile["oy"]) + 0.5,
                            y1=float(tile["oy"]) + QUAD - 0.5,
                            x0=float(tile["ox"]),
                            x1=float(tile["ox"] + QUAD - 1))
        try:
            draw_slot(Pen(staging, tile), sid, stage, slot)
        except SpriteOverflow:
            return None
        return staging

    best = None
    hi = SLOT_FILL[slot]
    lo = 0.5
    if works(hi) is not None:
        return hi, works(hi)
    for _ in range(14):
        mid = (lo + hi) / 2.0
        got = works(mid)
        if got is not None:
            best, lo = (mid, got), mid
        else:
            hi = mid
    if best is None:
        best = (0.5, works(0.5))
    return best


def generate_texture(sid, stage):
    """一张 32x32 贴图 = 2x2 精灵位；四位内容各不相同（见 draw_slot）。"""
    global BASE_CX, BASE_K
    canvas = Canvas(TEX_W, TEX_H)
    for slot in SLOT_LIST:
        col, row = slot % 2, slot // 2
        # row 0 = 画布下半（body 带，贴地上的植株），row 1 = 画布上半（head 带，花头）
        band = "body" if row == 0 else "head"
        tile = dict(BANDS[band])
        tile["ox"] = int(round(COL_CX[col] - QUAD / 2.0))
        BASE_CX = float(COL_CX[col])
        _scale, staging = _fit_slot(sid, stage, slot, tile)
        if staging is None:
            continue
        for x in range(TEX_W):
            for y in range(TEX_H):
                if staging.px[x][y] is not None:
                    canvas.px[x][y] = staging.px[x][y]
        canvas.special |= staging.special
    canvas.ys = 1.0
    canvas.clip = None
    BASE_CX, BASE_K = 8.0, 1.0
    canvas.shade_outline(PALETTES[sid])
    return canvas.to_image(TEX_W, TEX_H)


# ---------------------------------------------------------------------------
# 7. 校验（脚本内置，--check 只读不写）
# ---------------------------------------------------------------------------

def _rot_corner(p, rot):
    if not rot:
        return p
    o = rot.get("origin", [0, 0, 0])
    ax = rot.get("axis", "y")
    a = math.radians(rot.get("angle", 0))
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    if ax == "y":
        x, y, z = x * math.cos(a) + z * math.sin(a), y, -x * math.sin(a) + z * math.cos(a)
    elif ax == "x":
        x, y, z = x, y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    else:
        x, y, z = x, y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    return (x + o[0], y + o[1], z + o[2])


def _el_corners(el):
    x0, y0, z0 = el["from"]
    x1, y1, z1 = el["to"]
    lo = (min(x0, x1), min(y0, y1), min(z0, z1))
    hi = (max(x0, x1), max(y0, y1), max(z0, z1))
    pts = [(lo[0], lo[1], lo[2]), (hi[0], lo[1], lo[2]), (hi[0], lo[1], hi[2]),
           (lo[0], lo[1], hi[2]), (lo[0], hi[1], lo[2]), (hi[0], hi[1], lo[2]),
           (hi[0], hi[1], hi[2]), (lo[0], hi[1], hi[2])]
    return [_rot_corner(p, el.get("rotation")) for p in pts]


def validate_all(verbose=True):
    problems, counts_by_species = [], {}
    n_models = n_tex = n_uv = 0
    elem_min, elem_max = 99, 0

    for sid, zh in SPECIES:
        counts_by_species[sid] = []
        for stage in range(4):
            name = f"{sid}_stage{stage}"
            mp = os.path.join(MODEL_DIR, name + ".json")
            tp = os.path.join(TEX_DIR, name + ".png")

            # ---------- 模型 ----------
            data = None
            if not os.path.isfile(mp):
                problems.append(f"[模型缺失] {name}.json")
            else:
                try:
                    with open(mp, "r", encoding="utf-8") as fh:
                        data = json.load(fh)
                    n_models += 1
                except Exception as exc:  # noqa: BLE001
                    problems.append(f"[JSON 非法] {name}.json: {exc}")

            if data is not None:
                if data.get("render_type") != "minecraft:cutout":
                    problems.append(f"[render_type] {name}: 缺少 render_type=cutout")
                if "parent" in data:
                    problems.append(f"[parent] {name}: 仍继承 {data['parent']}（要求自带几何）")
                if data.get("ambientocclusion") is not False:
                    problems.append(f"[ambientocclusion] {name}: 应为 false")
                els = data.get("elements") or []
                elem_min, elem_max = min(elem_min, len(els)), max(elem_max, len(els))
                if len(els) < 3:
                    problems.append(f"[elements] {name}: 只有 {len(els)} 个 element（要求 >=3）")
                texkeys = data.get("textures") or {}
                if not texkeys:
                    problems.append(f"[textures] {name}: 没有贴图声明")
                for key, ref in texkeys.items():
                    if not str(ref).startswith("hongmeng:block/"):
                        problems.append(f"[贴图路径] {name}.{key} -> {ref} 前缀不对")
                        continue
                    f = str(ref).split("/", 1)[1] + ".png"
                    if not os.path.isfile(os.path.join(TEX_DIR, f)):
                        problems.append(f"[贴图缺失] {name}.{key} -> textures/block/{f}")
                for el in els:
                    ename = el.get("name", "?")
                    faces = el.get("faces") or {}
                    if not faces:
                        problems.append(f"[faces] {name}.{ename}: 没有面")
                    for fk, face in faces.items():
                        tk = face.get("texture")
                        if tk not in texkeys:
                            problems.append(f"[face.texture] {name}.{ename}.{fk} -> {tk} 未声明")
                        uv = face.get("uv")
                        if uv is None or len(uv) != 4:
                            problems.append(f"[face.uv] {name}.{ename}.{fk}: uv 缺失/长度不对")
                            continue
                        for j in range(0, 4, 2):
                            u, v = uv[j], uv[j + 1]
                            n_uv += 1
                            if not (0.0 <= u <= 16.0) or not (0.0 <= v <= 16.0):
                                problems.append(
                                    f"[UV 越界] {name}.{ename}.{fk}: u={u} v={v}（必须 0..16）")
                        if uv[1] > uv[3]:
                            problems.append(f"[UV 上下反] {name}.{ename}.{fk}: {uv}")
                    if fk == "up" and "cullface" in face and face["cullface"] != "up":
                        problems.append(f"[cullface] {name}.{ename}.up -> {face['cullface']}")
                    # 交叉平面 / 倾斜平面不允许写 cullface
                    rot = el.get("rotation")
                    if rot is not None and any("cullface" in f for f in faces.values()):
                        problems.append(f"[cullface] {name}.{ename}: 旋转面写了 cullface（会被剔除消失）")
                    # 几何范围
                    for c in _el_corners(el):
                        if not (-MAX_ELEM - 1 <= c[0] <= MAX_ELEM + 1
                                and -MAX_ELEM - 1 <= c[1] <= 33.0
                                and -MAX_ELEM - 1 <= c[2] <= MAX_ELEM + 1):
                            problems.append(f"[几何越界] {name}.{ename}: 角点 {tuple(round(v,2) for v in c)}")
                    # 倾斜面：两个面在中心轴相交是**故意的**（保证底部不裂缝），
                    # 只要不整体翻到对面去就不算错。
                    if rot and rot.get("axis") == "x":
                        for c in _el_corners(el):
                            if abs(c[2] - 8.0) > 8.0:
                                problems.append(
                                    f"[倾斜面异常] {name}.{ename}: 角点 z={c[2]:.2f} 离中心太远")
                            if c[1] < -0.01:
                                problems.append(
                                    f"[倾斜面下陷] {name}.{ename}: 角点 y={c[1]:.2f} 低于地面")

            # ---------- 贴图 ----------
            if not os.path.isfile(tp):
                problems.append(f"[贴图缺失] {name}.png")
                continue
            with Image.open(tp) as im:
                if im.size != TEX_SIZE:
                    problems.append(f"[尺寸] {name}.png = {im.size}，应为 {TEX_W}x{TEX_H}")
                if im.mode != "RGBA":
                    problems.append(f"[格式] {name}.png mode={im.mode}，应为 RGBA")
                rgba = im.convert("RGBA")
            # UV 密度：0..16 的 UV 铺满整张贴图 -> 每 UV 单位覆盖的像素数在两个方向必须一致，
            # 否则模型上的像素会被拉成非正方形（贴图宽高比必须 1:1）
            if TEX_W != TEX_H:
                problems.append(f"[贴图宽高比] {name}.png = {TEX_W}x{TEX_H}，"
                                f"UV 空间 0..16 是正方形，贴图必须 1:1 否则像素被拉伸")
            pixels = [rgba.getpixel((x, y)) for y in range(TEX_H) for x in range(TEX_W)]
            opaque = [p for p in pixels if p[3] > 0]
            semi = [p for p in pixels if 0 < p[3] < 255]
            n_tex += 1
            if not opaque:
                problems.append(f"[全透明] {name}.png")
                continue
            if semi:
                problems.append(f"[半透明] {name}.png: {len(semi)} 个 0<a<255 像素（cutout 下会丢边）")
            n_all = TEX_W * TEX_H
            # 视觉密度：只看"占了几个象限"，在占用的象限里必须留白 >=35%，
            # 否则植株会糊成一团看不出轮廓。
            quads_used = set()
            for yy in range(TEX_H):
                for xx in range(TEX_W):
                    if rgba.getpixel((xx, yy))[3] > 0:
                        quads_used.add((xx // QUAD, yy // QUAD))
            area = max(1, len(quads_used)) * QUAD * QUAD
            if len(opaque) > area * 0.65:
                problems.append(f"[糊成一团] {name}.png: 占用 {len(quads_used)} 个象限，"
                                f"不透明像素 {len(opaque)}/{area}（留白不足 35%）")
            if n_all - len(opaque) < 150:
                problems.append(f"[遮罩不足] {name}.png: 只有 {n_all - len(opaque)} 个透明像素")
            dark = [p for p in opaque if p[0] + p[1] + p[2] < 30]
            if len(dark) > len(opaque) * 0.25:
                problems.append(f"[过黑] {name}.png: 暗像素占 {len(dark)/len(opaque):.0%}")
            uniq = len({(p[0] // 16, p[1] // 16, p[2] // 16) for p in opaque})
            if uniq < 3:
                problems.append(f"[层次不足] {name}.png: 只有 {uniq} 档颜色")
            counts_by_species[sid].append(len(opaque))

        c = counts_by_species[sid]
        for i in range(1, 4):
            if len(c) == 4 and c[i] <= c[i - 1]:
                problems.append(f"[无递进] {sid}: stage{i}({c[i]}px) 不大于 stage{i-1}({c[i-1]}px)")

    if verbose:
        print("=" * 76)
        print("自检报告（render_type / 贴图引用 / elements / UV / 贴图像素）")
        print("=" * 76)
        print(f"  [1] 模型文件      : {n_models}/24 存在且是合法 JSON，全部带 render_type=cutout")
        print(f"  [2] 贴图引用      : 每个 face.texture 都能在 textures/block/ 找到对应 PNG")
        print(f"  [3] elements 数量 : 每模型 {elem_min}~{elem_max} 个（要求 >=3）")
        print(f"  [4] UV 顶点       : 共 {n_uv} 个，全部落在 0..16 内")
        print(f"  [5] 贴图文件      : {n_tex}/24 存在、32x32、RGBA、非全透明、无半透明像素")
        print("  [6] 阶段递进（不透明像素数，必须 stage0<1<2<3）：")
        for sid, zh in SPECIES:
            row = counts_by_species[sid]
            print(f"        {sid:<14}{zh}   " + "  ".join(f"s{i}={v:>4}px" for i, v in enumerate(row)))
        print("-" * 76)
        if problems:
            print(f"  ✗ 校验失败 {len(problems)} 项：")
            for p in problems:
                print("      " + p)
        else:
            print("  ✓ 全部校验项通过")
        print("=" * 76)
    return problems


# ---------------------------------------------------------------------------
# 8. 预览一：接触表（6 行 x 4 列，4 倍放大，1px 灰网格 + 棋盘底）
# ---------------------------------------------------------------------------

def build_sheet():
    """6 行(物种) x 4 列(阶段)：整张贴图 4 倍放大 + 1px 灰网格。"""
    scale = 4
    cell_w, cell_h = TEX_W * scale, TEX_H * scale
    pad = 1
    W = 4 * (cell_w + pad) + pad
    H = len(SPECIES) * (cell_h + pad) + pad
    sheet = Image.new("RGBA", (W, H), (58, 58, 62, 255))
    dr = ImageDraw.Draw(sheet)
    for r, (sid, zh) in enumerate(SPECIES):
        for c in range(4):
            with Image.open(os.path.join(TEX_DIR, f"{sid}_stage{c}.png")) as im:
                big = im.convert("RGBA").resize((cell_w, cell_h), Image.NEAREST)
            x = pad + c * (cell_w + pad)
            y = pad + r * (cell_h + pad)
            for by in range(0, cell_h, 8):
                for bx in range(0, cell_w, 8):
                    if ((bx // 8) + (by // 8)) % 2 == 0:
                        dr.rectangle([x + bx, y + by, x + bx + 7, y + by + 7], fill=(46, 46, 50, 255))
            sheet.alpha_composite(big, (x, y))
            dr.rectangle([x - 1, y - 1, x + cell_w, y + cell_h], outline=(122, 122, 128, 255))
            dr.line([x, y + cell_h // 2, x + cell_w, y + cell_h // 2], fill=(150, 108, 64, 255))
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    out = os.path.join(PREVIEW_DIR, "herb_models_sheet.png")
    sheet.save(out)
    return out


# ---------------------------------------------------------------------------
# 9. 预览二：伪 3D 正交投影（线框 + 面填充，画家算法排序）
# ---------------------------------------------------------------------------

FACES_IDX = {
    "down":  (0, 1, 2, 3), "up": (4, 5, 6, 7),
    "north": (0, 1, 5, 4), "south": (3, 2, 6, 7),
    "east":  (1, 2, 6, 5), "west": (0, 3, 7, 4),
}
FACE_FILL = {
    "up": (176, 206, 156), "down": (74, 88, 66),
    "north": (140, 166, 124), "south": (150, 176, 132),
    "east": (116, 140, 106), "west": (108, 132, 100),
}


def project(p, yaw, pitch, scale, cx, cy):
    a = math.radians(yaw)
    x = p[0] * math.cos(a) + p[2] * math.sin(a)
    z = -p[0] * math.sin(a) + p[2] * math.cos(a)
    y = p[1]
    b = math.radians(pitch)
    y2 = y * math.cos(b) - z * math.sin(b)
    z2 = y * math.sin(b) + z * math.cos(b)
    return (float(cx + x * scale), float(cy - y2 * scale), float(z2))


def render_iso(models, cols=3, yaw=35.0, pitch=20.0):
    """models: [(label, model_dict)]；每个模型用正交投影画出来（不贴图，只线框+面填充）。"""
    rows = math.ceil(len(models) / cols)
    cw, ch = 230, 240
    img = Image.new("RGBA", (cols * cw + 1, rows * ch + 1), (28, 28, 32, 255))
    dr = ImageDraw.Draw(img)
    for idx, (label, model) in enumerate(models):
        gx = (idx % cols) * cw
        gy = (idx // cols) * ch
        dr.rectangle([gx + 1, gy + 1, gx + cw - 2, gy + ch - 2],
                     fill=(40, 40, 46, 255), outline=(76, 76, 84, 255))
        scale = 10.5
        cx = gx + cw / 2
        cy = gy + ch * 0.78
        quads = []
        for el in model["elements"]:
            corners = _el_corners(el)
            proj = [project(p, yaw, pitch, scale, cx, cy) for p in corners]
            faces = el.get("faces") or {}
            for fname, idxs in FACES_IDX.items():
                if fname not in faces:
                    continue
                poly = [proj[i] for i in idxs]
                depth = sum(p[2] for p in poly) / len(poly)
                quads.append((depth, poly, FACE_FILL[fname]))
        quads.sort(key=lambda q: q[0])
        for _, poly, fill in quads:
            dr.polygon([(round(px, 2), round(py, 2)) for px, py, _ in poly],
                       fill=fill, outline=(238, 242, 228))
        dr.text((gx + 8, gy + 7), label, fill=(230, 230, 240))
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    return img


def build_iso(models_for_preview):
    groups = [models_for_preview[i:i + 3] for i in range(0, len(models_for_preview), 3)]
    ims = [render_iso(g) for g in groups]
    w = max(i.width for i in ims)
    h = sum(i.height for i in ims) + 4 * (len(ims) - 1)
    combo = Image.new("RGBA", (w, h), (28, 28, 32, 255))
    y = 0
    for i in ims:
        combo.alpha_composite(i, (0, y))
        y += i.height + 4
    out = os.path.join(PREVIEW_DIR, "herb_models_iso.png")
    combo.save(out)
    return out


# ---------------------------------------------------------------------------
# 9b. 预览三：真·贴图正交投影渲染（软件光栅化 + Z-buffer + cutout 抠图）
#     这一张才是"游戏里大概长什么样"的判据。
# ---------------------------------------------------------------------------

# 每个 face 的 (u 轴, v 轴) 在世界坐标里的方向（与 MC 的 UV 生成一致）
FACE_AXES = {
    "north": ((1, 0, 0), (0, 1, 0)), "south": ((-1, 0, 0), (0, 1, 0)),
    "east":  ((0, 0, 1), (0, 1, 0)), "west":  ((0, 0, -1), (0, 1, 0)),
    "up":    ((1, 0, 0), (0, 0, 1)), "down":  ((1, 0, 0), (0, 0, -1)),
}
FACE_SHADE = {"up": 1.0, "down": 0.42, "north": 0.82, "south": 0.82,
              "east": 0.64, "west": 0.64}


def _dot3(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _face_uvs(el, corners, fname, uv):
    """把 [u1,v1,u2,v2] 按**真实 3D 仿射**分配给该面的四个角点。

    注意：uv 区间在贴图上取样多大范围，是由角点沿面内 u/v 轴的**实际长度**决定的
    （不是归一化到 0..1）。0 厚度平面（交叉面）在一个轴上长度为 0 —— 那说明
    贴图在该轴被压成一条线是设计如此。这里按仿射映射还原，薄面不会被压扁。
    """
    au, av = FACE_AXES[fname]
    u0, u1 = uv[0], uv[2]
    v0, v1 = uv[1], uv[3]
    us = [_dot3(c, au) for c in corners]
    vs = [_dot3(c, av) for c in corners]
    umin, umax = min(us), max(us)
    vmin, vmax = min(vs), max(vs)
    du = (u1 - u0) / (umax - umin) if umax - umin > 1e-6 else 0.0
    dv = (v1 - v0) / (vmax - vmin) if vmax - vmin > 1e-6 else 0.0
    out = []
    for i in range(4):
        out.append((u0 + (us[i] - umin) * du, v0 + (vs[i] - vmin) * dv))
    return out


def _tri(raster, zbuf, p0, p1, p2, uv0, uv1, uv2, tex, shade):
    """扫描线填充一个三角形，逐像素采样贴图；alpha<128 直接丢弃（simulate cutout）。"""
    xs = (p0[0], p1[0], p2[0])
    ys = (p0[1], p1[1], p2[1])
    x0, x1 = int(math.floor(min(xs))), int(math.ceil(max(xs)))
    y0, y1 = int(math.floor(min(ys))), int(math.ceil(max(ys)))
    area = (p1[0] - p0[0]) * (p2[1] - p0[1]) - (p1[1] - p0[1]) * (p2[0] - p0[0])
    if abs(area) < 1e-9:
        return
    W, H = tex.size
    for py in range(max(0, y0), min(raster.height, y1 + 1)):
        for px in range(max(0, x0), min(raster.width, x1 + 1)):
            cx, cy = px + 0.5, py + 0.5
            w0 = ((p1[0] - p0[0]) * (cy - p0[1]) - (p1[1] - p0[1]) * (cx - p0[0])) / area
            w1 = ((cx - p0[0]) * (p2[1] - p0[1]) - (cy - p0[1]) * (p2[0] - p0[0])) / area
            w2 = 1.0 - w0 - w1
            if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                continue
            z = p0[2] * w1 + p1[2] * w2 + p2[2] * w0
            if z <= zbuf[px][py]:
                continue
            u = uv0[0] * w1 + uv1[0] * w2 + uv2[0] * w0
            v = uv0[1] * w1 + uv1[1] * w2 + uv2[1] * w0
            tx = min(W - 1, max(0, int(u / 16.0 * W)))
            ty = min(H - 1, max(0, int(v / 16.0 * H)))
            r, g, b, a = tex.getpixel((tx, ty))
            if a < 128:
                continue
            zbuf[px][py] = z
            raster.putpixel((px, py), (min(255, int(r * shade)), min(255, int(g * shade)),
                                       min(255, int(b * shade)), 255))


def render_textured(model, tex_path, size=(190, 220), yaw=35.0, pitch=22.0, zoom=5.6):
    """把一个模型按真实 UV 采样贴图渲染出来（正交投影 + Z-buffer + cutout 抠图）。"""
    tex = Image.open(tex_path).convert("RGBA")
    img = Image.new("RGBA", size, (32, 34, 40, 255))
    zbuf = [[-1e18] * size[1] for _ in range(size[0])]
    cx, cy = size[0] / 2.0, size[1] * 0.88
    for el in model["elements"]:
        corners = _el_corners(el)
        proj = [project(p, yaw, pitch, zoom, cx, cy) for p in corners]
        for fname, idxs in FACES_IDX.items():
            face = (el.get("faces") or {}).get(fname)
            if face is None:
                continue
            pts = [proj[i] for i in idxs]
            uvs = _face_uvs(el, [corners[i] for i in idxs], fname, face["uv"])
            sh = FACE_SHADE[fname]
            _tri(img, zbuf, pts[0], pts[1], pts[2], uvs[0], uvs[1], uvs[2], tex, sh)
            _tri(img, zbuf, pts[0], pts[2], pts[3], uvs[0], uvs[2], uvs[3], tex, sh)
    return img


def build_textured_sheet(dirpath):
    """6 行(物种) x 4 列(阶段) 的真·贴图渲染表。"""
    tw, th = 168, 200
    img = Image.new("RGBA", (4 * tw + 8, 6 * th + 8), (24, 26, 30, 255))
    dr = ImageDraw.Draw(img)
    for r, (sid, zh) in enumerate(SPECIES):
        for c in range(4):
            model = build_model(sid, c)
            tile = render_textured(model, os.path.join(TEX_DIR, f"{sid}_stage{c}.png"),
                                   size=(tw - 6, th - 6))
            x, y = 4 + c * tw, 4 + r * th
            img.alpha_composite(tile, (x, y))
            dr.rectangle([x - 1, y - 1, x + tw - 7, y + th - 7], outline=(88, 90, 98, 255))
    out = os.path.join(dirpath, "herb_models_3d.png")
    img.save(out)
    return out


# ---------------------------------------------------------------------------
# 10. main
# ---------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser(description="鸿蒙修仙 灵植方块外观生成器")
    ap.add_argument("--check", action="store_true", help="只校验，不写任何文件")
    ap.add_argument("--no-preview", action="store_true", help="不生成预览图")
    args = ap.parse_args()

    for d in (MODEL_DIR, TEX_DIR, PREVIEW_DIR):
        os.makedirs(d, exist_ok=True)

    written_m = written_t = 0
    models_for_preview = []
    for sid, zh in SPECIES:
        for stage in range(4):
            name = f"{sid}_stage{stage}"
            model = build_model(sid, stage)
            models_for_preview.append((f"{sid}  stage{stage}", model))
            if not args.check:
                with open(os.path.join(MODEL_DIR, name + ".json"), "w",
                          encoding="utf-8", newline="\n") as fh:
                    json.dump(model, fh, ensure_ascii=False, indent=2)
                    fh.write("\n")
                written_m += 1
                generate_texture(sid, stage).save(os.path.join(TEX_DIR, name + ".png"))
                written_t += 1

    if not args.check:
        print(f"写入 {written_m} 个模型 JSON + {written_t} 张贴图 PNG")

    problems = validate_all(verbose=True)

    if not args.check and not args.no_preview:
        print(f"接触表预览：{build_sheet()}")
        print(f"伪 3D 预览：{build_iso(models_for_preview)}")
        print(f"贴图渲染预览：{build_textured_sheet(PREVIEW_DIR)}")

    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())