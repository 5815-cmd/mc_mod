# -*- coding: utf-8 -*-
"""
R-2 · 通用双手武器 15 个外观模型（5 型 × 3 档 = 覆盖 50 件）
=============================================================
- 长剑 low/mid 由本脚本生成；长剑 high 直接复用 W-01R 已验收的 `build_sword()`
  （鸿蒙·霜纹古篆剑），不重做；
- 巨剑 / 镰刀 / 长枪 / 棍 各 3 档（low/mid/high），由 parametric builder 生成；
- 每档一套贴图（256×256 + glowmask），命名 `career_<form>_<style>`；
- 输出到 mod 资源目录；预览进 工程/art/weapons/。

坐标契约（GeoWeaponItem）：握把底 [0,0,0]、刃身朝 +Y；1 unit = 1/16 格。
"""
from __future__ import annotations

import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import g2_tex
from artlib import (Geo, build_mesh, contact_sheet, render_views, ring_cubes,
                    step_point, taper_blade)
from build_w01r import build_sword

HERE = os.path.dirname(os.path.abspath(__file__))
W01R = os.path.abspath(os.path.join(HERE, "..", "w01r"))
PREV = os.path.join(W01R, "weapons")
WS = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
os.makedirs(PREV, exist_ok=True)

# 三档材质（低/中/高）：玄铁 → 银钢+金 → 霜银+青蓝
STYLE_PAL = {
    "low": dict(metal=(120, 116, 108), steel=(178, 172, 160), cloth=(74, 58, 36),
                jade=(150, 200, 205), rune=(150, 210, 218),
                glow=(150, 210, 218), hem=(90, 74, 42), gold=(172, 138, 70)),
    "mid": dict(metal=(190, 200, 210), steel=(236, 240, 246), cloth=(62, 48, 86),
                jade=(214, 190, 110), rune=(255, 215, 120),
                glow=(255, 215, 120), hem=(80, 64, 110), gold=(220, 182, 92)),
    "high": dict(metal=(198, 214, 230), steel=(240, 246, 252), cloth=(53, 36, 97),
                 jade=(165, 232, 248), rune=(110, 216, 246),
                 glow=(127, 227, 242), hem=(59, 180, 224), gold=(201, 162, 74)),
}


def pals(style):
    p = STYLE_PAL[style]
    return ({k: p[k] for k in ("metal", "steel", "cloth", "jade", "rune")},
            p["glow"], p["hem"], p["gold"])


def add(bone, x0, x1, y0, y1, z0, z1, mat="metal"):
    bone.add((x0, y0, z0), (x1 - x0, y1 - y0, z1 - z0), mat=mat)


# ---------------------------------------------------------------------------
# 长剑 low / mid（high 复用 W-01R）
# ---------------------------------------------------------------------------

def build_sword_low():
    g = Geo("geometry.career_sword_low", tex=(256, 256))
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 9.0, 0), parent="grip")
    blade = g.bone("blade", (0, 11.0, 0), parent="guard")
    add(grip, -1.4, 1.4, 0.0, 1.6, -1.4, 1.4)
    add(grip, -1.5, 1.5, 1.6, 2.0, -1.5, 1.5, "steel")
    add(grip, -1.0, 1.0, 2.0, 9.0, -1.0, 1.0, "cloth")
    for y in (3.4, 5.4, 7.4):
        add(grip, -1.15, 1.15, y, y + 0.4, -1.15, 1.15, "steel")
    add(guard, -1.7, 1.7, 9.0, 10.4, -1.4, 1.4)
    add(guard, -0.6, 0.6, 9.4, 10.2, -1.75, -1.45, "rune")
    for s in (-1, 1):
        x0, x1 = (1.7, 3.4) if s > 0 else (-3.4, -1.7)
        add(guard, x0, x1, 9.2, 10.2, -1.0, 1.0)
    taper_blade(blade, 11.0, 35.0, 3.4, 2.6, 1.3, 1.0, 4)
    step_point(blade, 35.0, 37.4, 2.6, 1.0, 2)
    return g


def build_sword_mid():
    g = Geo("geometry.career_sword_mid", tex=(256, 256))
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 9.6, 0), parent="grip")
    blade = g.bone("blade", (0, 11.8, 0), parent="guard")
    ring = g.bone("ring", (0, 22.0, 0), parent="blade")
    add(grip, -1.5, 1.5, 0.0, 1.7, -1.5, 1.5)
    add(grip, -1.25, 1.25, 1.7, 2.1, -1.25, 1.25, "steel")
    add(grip, -1.05, 1.05, 2.1, 9.6, -1.05, 1.05, "cloth")
    for y in (3.4, 5.4, 7.4):
        add(grip, -1.2, 1.2, y, y + 0.4, -1.2, 1.2, "steel")
    add(guard, -1.9, 1.9, 9.6, 11.2, -1.5, 1.5)
    for s in (-1, 1):
        x0, x1 = (1.9, 4.2) if s > 0 else (-4.2, -1.9)
        add(guard, x0, x1, 10.0, 11.0, -1.0, 1.0)
        x0, x1 = (3.6, 4.6) if s > 0 else (-4.6, -3.6)
        add(guard, x0, x1, 11.0, 11.8, -0.6, 0.6, "jade")
    add(guard, -0.8, 0.8, 9.8, 11.2, -1.85, -1.5, "rune")
    taper_blade(blade, 11.8, 37.0, 3.8, 2.8, 1.5, 1.1, 5)
    for s in (-1, 1):
        x0 = 1.6 if s > 0 else -1.72
        add(blade, x0, x0 + 0.12, 12.0, 36.4, -0.42, 0.42, "rune")
    step_point(blade, 37.0, 39.6, 2.8, 1.1, 2)
    ring_cubes(ring, 10, 22.0, 3.6, 0.6, mat="rune")
    return g


# ---------------------------------------------------------------------------
# 巨剑
# ---------------------------------------------------------------------------

def build_greatsword(style):
    high = style == "high"
    g = Geo("geometry.career_greatsword_" + style, tex=(256, 256))
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 13.0, 0), parent="grip")
    blade = g.bone("blade", (0, 16.5, 0), parent="guard")
    add(grip, -2.2, 2.2, 0.0, 2.2, -2.2, 2.2)
    add(grip, -1.8, 1.8, 2.2, 2.8, -1.8, 1.8, "steel")
    add(grip, -1.4, 1.4, 2.8, 13.0, -1.4, 1.4, "cloth")
    for y in (4.6, 7.0, 9.4, 11.8):
        add(grip, -1.55, 1.55, y, y + 0.5, -1.55, 1.55, "steel")
    add(guard, -2.6, 2.6, 13.0, 16.5, -2.0, 2.0)
    add(guard, -0.9, 0.9, 14.2, 15.4, -2.25, -1.85, "rune")
    for s in (-1, 1):
        x0, x1 = (2.6, 6.4) if s > 0 else (-6.4, -2.6)
        add(guard, x0, x1, 13.6, 15.6, -1.4, 1.4)
        x0, x1 = (5.4, 6.8) if s > 0 else (-6.8, -5.4)
        add(guard, x0, x1, 15.0, 16.6, -0.9, 0.9, "jade" if not high else "rune")
    taper_blade(blade, 16.5, 39.0, 8.4, 5.8, 2.6, 1.8, 6)
    add(blade, -1.0, 1.0, 16.8, 38.6, -1.4, -1.2, "steel")
    if high:
        for s in (-1, 1):
            x0 = 3.9 if s > 0 else -4.02
            add(blade, x0, x0 + 0.12, 16.8, 38.4, -0.6, 0.6, "rune")
        for xx, yy in ((-5.4, 33.0), (5.4, 29.0), (-4.6, 23.0)):
            add(blade, xx - 0.35, xx + 0.35, yy - 0.35, yy + 0.35, -0.35, 0.35, "jade")
    step_point(blade, 39.0, 41.6, 5.8, 1.8, 2)
    return g


# ---------------------------------------------------------------------------
# 镰刀
# ---------------------------------------------------------------------------

def build_scythe(style):
    high = style == "high"
    g = Geo("geometry.career_scythe_" + style, tex=(256, 256))
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, 34.0, 0), parent="shaft")
    blade = g.bone("blade", (0, 34.0, 0), parent="head")
    add(shaft, -1.6, 1.6, 0.0, 34.0, -1.6, 1.6, "cloth")
    for y in (4.0, 9.0, 14.0, 19.0, 24.0, 29.0):
        add(shaft, -1.85, 1.85, y, y + 0.7, -1.85, 1.85, "steel")
    add(shaft, -2.2, 2.2, 0.0, 2.0, -2.2, 2.2)
    add(head, -2.4, 2.4, 33.0, 36.0, -2.4, 2.4)
    add(head, -0.8, 0.8, 34.2, 35.4, -2.7, -2.3, "rune")
    # 月牙刃：以 (0,22) 为圆心、半径 12 的圆弧，从 90° 扫到 15°
    # （起点正好接在杆顶 (0,34)，终点向外下方收，形成"弯月"剪影）
    cx, cy, r = 0.0, 22.0, 12.0
    segs = 9
    for i in range(segs):
        a0 = math.radians(90 - i * (75.0 / segs))
        a1 = math.radians(90 - (i + 1) * (75.0 / segs))
        p0 = (cx + r * math.cos(a0), cy + r * math.sin(a0))
        p1 = (cx + r * math.cos(a1), cy + r * math.sin(a1))
        mx, my = (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2
        length = math.dist(p0, p1) + 0.4
        deg = math.degrees(math.atan2(p1[1] - p0[1], p1[0] - p0[0]))
        h = 4.0 - i * 0.26
        blade.add((mx - length / 2, my - h / 2, -0.8),
                  (length, h, 1.6), mat="metal",
                  rotation=(0, 0, deg), pivot=(mx, my, 0))
        if high and i % 2 == 0:
            blade.add((mx - 0.3, my - 2.6, -0.5), (0.6, 0.6, 1.0), mat="rune")
    if style != "low":
        ring_cubes(head, 16, 30.0, 3.4, 0.6, mat="rune" if high else "jade")
    return g


# ---------------------------------------------------------------------------
# 长枪
# ---------------------------------------------------------------------------

def build_spear(style):
    high = style == "high"
    g = Geo("geometry.career_spear_" + style, tex=(256, 256))
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, 44.0, 0), parent="shaft")
    add(shaft, -1.5, 1.5, 0.0, 44.0, -1.5, 1.5, "cloth")
    for y in (3.5, 9.0, 14.5, 20.0, 25.5, 31.0, 36.5, 41.0):
        add(shaft, -1.75, 1.75, y, y + 0.6, -1.75, 1.75, "steel")
    add(shaft, -1.9, 1.9, 0.0, 2.0, -1.9, 1.9)
    add(head, -2.2, 2.2, 43.0, 46.0, -2.2, 2.2)
    add(head, -0.7, 0.7, 44.2, 45.2, -2.45, -2.1, "rune")
    add(head, -1.8, 1.8, 46.0, 48.0, -1.8, 1.8, "steel")
    taper_blade(head, 48.0, 57.0, 4.2, 1.4, 1.8, 0.7, 4)
    step_point(head, 57.0, 59.6, 1.4, 0.7, 2)
    if high:
        for s in (-1, 1):
            x0 = 0.6 if s > 0 else -0.72
            add(head, x0, x0 + 0.12, 48.4, 56.6, -0.3, 0.3, "rune")
    # 枪缨
    tassel = g.bone("tassel", (0, 44.0, 0), parent="shaft")
    for i, dz in enumerate((-1.6, 1.6, 0.0)):
        tassel.add((-1.0, 38.0 - i * 0.0, dz - 0.6), (2.0, 5.0, 1.2),
                   mat="cloth", rotation=(15 if i == 0 else -15, 0, 0),
                   pivot=(0, 43.0, dz))
    if style != "low":
        ring_cubes(head, 12, 46.5, 2.8, 0.55, mat="rune" if high else "jade")
    return g


# ---------------------------------------------------------------------------
# 棍
# ---------------------------------------------------------------------------

def build_staff(style):
    high = style == "high"
    g = Geo("geometry.career_staff_" + style, tex=(256, 256))
    shaft = g.bone("shaft", (0, 0, 0))
    add(shaft, -1.8, 1.8, 0.0, 40.0, -1.8, 1.8, "cloth")
    add(shaft, -0.6, 0.6, 19.4, 20.6, -2.1, -1.7, "rune")
    for y in (3, 8, 13, 18, 23, 28, 33, 37.5):
        add(shaft, -2.05, 2.05, y, y + 0.8, -2.05, 2.05, "steel")
    for y in (0.0, 38.0):
        add(shaft, -2.6, 2.6, y, y + 2.0, -2.6, 2.6)
        add(shaft, -2.0, 2.0, y + 2.0, y + 2.6, -2.0, 2.0, "steel")
    if style != "low":
        for y in (10.0, 30.0):
            add(shaft, -2.4, 2.4, y, y + 1.0, -2.4, 2.4, "jade")
    if high:
        ring_a = g.bone("ring_a", (0, 20.0, 0), parent="shaft")
        ring_b = g.bone("ring_b", (0, 20.0, 0), parent="shaft")
        ring_cubes(ring_a, 14, 20.0, 3.6, 0.6, mat="rune")
        for i in range(6):
            a = 2 * math.pi * i / 6
            ring_b.add((math.cos(a) * 5.0 - 0.35, 30.0 - 0.35,
                        math.sin(a) * 5.0 - 0.35), (0.7, 0.7, 0.7), mat="jade")
    return g


BUILDERS = {
    ("sword", "low"): build_sword_low,
    ("sword", "mid"): build_sword_mid,
    ("sword", "high"): build_sword,
    ("greatsword", "low"): lambda: build_greatsword("low"),
    ("greatsword", "mid"): lambda: build_greatsword("mid"),
    ("greatsword", "high"): lambda: build_greatsword("high"),
    ("scythe", "low"): lambda: build_scythe("low"),
    ("scythe", "mid"): lambda: build_scythe("mid"),
    ("scythe", "high"): lambda: build_scythe("high"),
    ("spear", "low"): lambda: build_spear("low"),
    ("spear", "mid"): lambda: build_spear("mid"),
    ("spear", "high"): lambda: build_spear("high"),
    ("staff", "low"): lambda: build_staff("low"),
    ("staff", "mid"): lambda: build_staff("mid"),
    ("staff", "high"): lambda: build_staff("high"),
}


def main():
    previews = []
    stats = []
    for (form, style), builder in BUILDERS.items():
        geo = builder()
        name = "career_%s_%s" % (form, style)
        geo.identifier = "geometry." + name
        k, st = g2_tex.pack_geo(geo, (256, 256))
        geo.tex = (256, 256)
        geo.save(os.path.join(RES, "geo", name + ".geo.json"))
        pal, glow, hem, gold = pals(style)
        tex_p = os.path.join(RES, "textures", "item", name + ".png")
        glow_p = os.path.join(RES, "textures", "item", name + "_glowmask.png")
        ratio, _ = g2_tex.paint_geo(geo, (256, 256), tex_p, glow_p,
                                    pal=pal, glow_color=glow, hem=hem, gold=gold)
        # 动画：长剑 high 用 W-01R 的符环旋转/前刃浮沉/剑穗摆动；
        # 其余档位先给合法空 idle（不引用不存在的骨骼）。
        if form == "sword" and style == "high":
            anim = {"format_version": "1.8.0", "animations": {"idle": {
                "loop": True, "animation_length": 4.0, "bones": {
                    "ring_a": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, -360, 0]}},
                    "ring_b": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]}},
                    "blade_float": {"position": {
                        "0.0": [0, 0, 0], "1.0": [0, 0.16, 0], "2.0": [0, 0, 0],
                        "3.0": [0, 0.16, 0], "4.0": [0, 0, 0]}},
                    "tassel": {"rotation": {
                        "0.0": [0, 0, -7], "1.0": [0, 0, 7], "2.0": [0, 0, -7],
                        "3.0": [0, 0, 7], "4.0": [0, 0, -7]}},
                }}}}
        else:
            anim = {"format_version": "1.8.0", "animations": {
                "idle": {"loop": True, "animation_length": 2.0, "bones": {}}}}
        with open(os.path.join(RES, "animations", name + ".animation.json"),
                  "w", encoding="utf-8") as f:
            json.dump(anim, f, ensure_ascii=False, indent=2)
        tex_arr = np.array(Image.open(tex_p).convert("RGB"))
        glow_arr = np.array(Image.open(glow_p).convert("RGB"))
        prev = os.path.join(PREV, "prev_%s.png" % name)
        render_views(build_mesh(geo), prev, views=("front", "side", "hero"),
                     size=(300, 620), label="%s · %s" % (form, style),
                     texture=tex_arr, glow=glow_arr)
        previews.append(prev)
        stats.append({"name": name, "form": form, "style": style,
                      "cubes": geo.cube_count(), "k": k,
                      "glow": round(ratio, 4)})
        print("[OK]", name, "cubes", geo.cube_count(), "k", k,
              "glow %.1f%%" % (ratio * 100))
    contact_sheet(previews, os.path.join(PREV, "0_WEAPONS_15.png"), cols=5,
                  title="R-2 通用武器 15 外观（5 型 × 低/中/高）")
    with open(os.path.join(PREV, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)
    print("[DONE] 15 个武器外观已写入", RES)


if __name__ == "__main__":
    main()
