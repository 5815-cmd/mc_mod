# -*- coding: utf-8 -*-
"""
武器外观 v2：21 个基础名各一版独特几何 + 配色
================================================
设计表见 `工程/art/装备外观重做-设计表v2.md`。

- 每把名器 = 一个 family：`career_<form>_<key>`（例 career_sword_chixiao）
- 50 件武器按 06 §2 的阶数分段映射到这 21 个 family（映射写进 mapping.json，Java 侧同步）
- 全部沿用 GeoWeaponItem 契约：握把底 [0,0,0]、刃身朝 +Y
"""
from __future__ import annotations

import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import g2_tex
from artlib import (Geo, build_mesh, contact_sheet, render_views, ring_cubes,
                    step_point, taper_blade)

HERE = os.path.dirname(os.path.abspath(__file__))
OUTROOT = os.path.abspath(os.path.join(HERE, "..", "w01r", "weapons_v2"))
WS = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
os.makedirs(OUTROOT, exist_ok=True)


def add(bone, x0, x1, y0, y1, z0, z1, mat="metal"):
    bone.add((x0, y0, z0), (x1 - x0, y1 - y0, z1 - z0), mat=mat)


def rot_box(bone, cx, cy, cz, sx, sy, sz, rot_z, mat="metal"):
    bone.add((cx - sx / 2, cy - sy / 2, cz - sz / 2), (sx, sy, sz),
             mat=mat, rotation=(0, 0, rot_z), pivot=(cx, cy, cz))


# ---------------------------------------------------------------------------
# 材质：每把名器独立调色
# ---------------------------------------------------------------------------

def P(metal, steel, cloth, jade, rune, glow, hem=None, gold=(201, 162, 74),
      grip=None, tassel=None):
    return dict(pal={"metal": metal, "steel": steel, "cloth": cloth,
                     "jade": jade, "rune": rune},
                glow=glow, hem=hem or glow, gold=gold,
                grip=grip, tassel=tassel)


SPECS = [
    # ---------------- 长剑 ----------------
    dict(key="qingfeng", form="sword", cn="青锋长剑", tiers=(1, 2),
         color=P((150, 195, 212), (218, 238, 246), (34, 58, 72), (130, 225, 238),
                 (150, 235, 245), glow=(150, 235, 245), gold=(190, 200, 120),
                 grip=(30, 52, 66), tassel=(190, 235, 245)),
         shape=dict(blade_len=24, w0=3.6, w1=2.6, t0=1.4, t1=1.0, segs=4,
                    guard="cloud", tassel=True, rune_bars=0)),
    dict(key="chixiao", form="sword", cn="赤霄灵纹剑", tiers=(3, 4),
         color=P((196, 126, 92), (242, 206, 178), (86, 18, 14), (255, 126, 62),
                 (255, 120, 40), glow=(255, 120, 40), hem=(160, 50, 24),
                 gold=(232, 186, 96), grip=(74, 16, 12), tassel=(255, 150, 90)),
         shape=dict(blade_len=27, w0=4.8, w1=3.2, t0=1.6, t1=1.1, segs=6,
                    guard="flame", tassel=True, rune_bars=5, ribs=3)),
    dict(key="xuantie", form="sword", cn="玄铁重锋剑", tiers=(5, 6),
         color=P((74, 76, 82), (138, 142, 150), (40, 40, 44), (150, 150, 150),
                 (170, 180, 185), glow=(120, 130, 140), gold=(150, 130, 90),
                 grip=(48, 34, 26), tassel=(120, 125, 130)),
         shape=dict(blade_len=21, w0=7.2, w1=5.2, t0=2.6, t1=1.9, segs=5,
                    guard="square", tassel=False, rune_bars=0, rivets=True)),
    dict(key="xuecao", form="sword", cn="血槽魔纹剑", tiers=(7, 8),
         color=P((128, 66, 66), (198, 148, 148), (48, 8, 10), (255, 74, 74),
                 (226, 32, 44), glow=(230, 40, 50), hem=(120, 12, 18),
                 gold=(180, 150, 90), grip=(58, 10, 12), tassel=(230, 70, 70)),
         shape=dict(blade_len=28, w0=5.6, w1=3.6, t0=1.9, t1=1.2, segs=6,
                    guard="small", tassel=True, rune_bars=3, groove=True,
                    serrated=True, skull=True)),
    dict(key="shuangwen", form="sword", cn="霜纹古篆剑", tiers=(9, 10),
         color=P((198, 214, 230), (240, 246, 252), (53, 36, 97), (165, 232, 248),
                 (110, 216, 246), glow=(127, 227, 242), hem=(59, 180, 224),
                 gold=(201, 162, 74), grip=(53, 36, 97), tassel=(206, 232, 247)),
         shape=dict(w01r=True)),

    # ---------------- 巨剑 ----------------
    dict(key="zhenyue", form="greatsword", cn="镇岳巨剑", tiers=(1, 3),
         color=P((140, 123, 92), (212, 196, 160), (78, 58, 30), (150, 190, 120),
                 (200, 210, 160), glow=(200, 210, 160), gold=(190, 160, 90),
                 grip=(72, 50, 26), tassel=(200, 190, 150)),
         shape=dict(blade_len=32, w0=15.0, w1=10.5, t0=4.2, t1=3.0, segs=6,
                    guard="stone", rocks=True, rune_bars=0)),
    dict(key="duanyue", form="greatsword", cn="断岳玄重剑", tiers=(4, 5),
         color=P((80, 84, 92), (150, 158, 170), (38, 40, 48), (130, 140, 150),
                 (160, 175, 185), glow=(120, 140, 150), gold=(120, 120, 120),
                 grip=(40, 32, 28), tassel=(140, 150, 160)),
         shape=dict(blade_len=35, w0=14.0, w1=10.0, t0=3.8, t1=2.8, segs=7,
                    guard="bar", groove=True, rune_bars=0)),
    dict(key="longji", form="greatsword", cn="龙脊裂地剑", tiers=(6, 8),
         color=P((108, 120, 88), (192, 204, 150), (48, 58, 38), (110, 255, 170),
                 (120, 255, 180), glow=(120, 255, 180), hem=(60, 130, 90),
                 gold=(226, 190, 96), grip=(44, 56, 32), tassel=(160, 240, 190)),
         shape=dict(blade_len=35, w0=15.0, w1=10.5, t0=4.0, t1=3.0, segs=7,
                    guard="dragon", spine=6, rune_bars=2)),
    dict(key="mogu", form="greatsword", cn="魔骨血刃巨剑", tiers=(9, 10),
         color=P((200, 190, 170), (240, 235, 220), (58, 10, 14), (255, 70, 70),
                 (240, 50, 60), glow=(255, 60, 60), hem=(110, 10, 16),
                 gold=(150, 140, 110), grip=(56, 18, 16), tassel=(230, 90, 80)),
         shape=dict(blade_len=36, w0=15.5, w1=11.0, t0=4.2, t1=3.2, segs=7,
                    guard="bone", bone_spikes=True, rune_bars=2)),

    # ---------------- 镰刀 ----------------
    dict(key="gouhun", form="scythe", cn="勾魂镰", tiers=(1, 2),
         color=P((120, 100, 150), (192, 174, 222), (40, 28, 60), (190, 120, 255),
                 (190, 120, 255), glow=(190, 120, 255), hem=(70, 44, 110),
                 gold=(190, 170, 120), grip=(44, 30, 60)),
         shape=dict(shaft=27, arc_r=20.0, sweep=108, blade_h=9.0, skull=True,
                    moon=False, floats=0, chains=True)),
    # C 线：玩家要的「死神之镰」——黑曜巨镰 + 骷髅刃座 + 锯齿背 + 魂火
    dict(key="sishen", form="scythe", cn="死神之镰", tiers=(3, 4),
         color=P((46, 46, 56), (124, 128, 146), (20, 20, 28), (120, 255, 200),
                 (150, 255, 215), glow=(120, 255, 200), hem=(60, 90, 80),
                 gold=(150, 150, 170), grip=(16, 16, 22)),
         shape=dict(shaft=28, arc_r=21.5, sweep=116, blade_h=10.5, skull=True,
                    moon=False, floats=0, chains=False, death=True)),
    dict(key="xueyue", form="scythe", cn="血月弯镰", tiers=(5, 6),
         color=P((150, 62, 62), (222, 142, 132), (50, 8, 10), (255, 90, 90),
                 (255, 84, 84), glow=(255, 80, 80), hem=(120, 12, 18),
                 gold=(210, 170, 100), grip=(58, 10, 12)),
         shape=dict(shaft=27, arc_r=21.0, sweep=116, blade_h=9.6, skull=False,
                    moon=True, floats=0, chains=False)),
    dict(key="yougu", form="scythe", cn="幽骨冥纹镰", tiers=(7, 8),
         color=P((190, 184, 166), (232, 228, 212), (30, 50, 50), (102, 242, 200),
                 (102, 242, 200), glow=(102, 242, 200), hem=(36, 92, 84),
                 gold=(150, 160, 120), grip=(40, 60, 56)),
         shape=dict(shaft=28, arc_r=20.5, sweep=112, blade_h=9.2, bone_shaft=True,
                    skull=False, moon=False, floats=0, chains=False)),
    dict(key="xukong", form="scythe", cn="虚空割裂镰", tiers=(9, 10),
         color=P((52, 44, 66), (120, 104, 160), (18, 14, 26), (176, 96, 255),
                 (176, 96, 255), glow=(176, 96, 255), hem=(60, 30, 90),
                 gold=(140, 120, 180), grip=(24, 18, 34)),
         shape=dict(shaft=29, arc_r=22.0, sweep=120, blade_h=10.0, skull=False,
                    moon=False, floats=3, chains=False, rift=True)),

    # ---------------- 长枪 ----------------
    dict(key="pojun", form="spear", cn="破军枪", tiers=(1, 3),
         color=P((126, 124, 120), (206, 202, 194), (56, 42, 24), (214, 172, 88),
                 (240, 210, 120), glow=(240, 210, 120), hem=(96, 72, 34),
                 gold=(232, 196, 110), grip=(58, 42, 24), tassel=(210, 46, 40)),
         shape=dict(shaft=44, head_len=20, head_w=7.6, tassel="red",
                    cloud_rings=0, lightning=False, lugs=True, butt=True)),
    dict(key="youlong", form="spear", cn="游龙穿云枪", tiers=(4, 6),
         color=P((168, 210, 200), (236, 250, 246), (28, 66, 58), (120, 255, 220),
                 (140, 255, 230), glow=(140, 255, 230), hem=(40, 110, 96),
                 gold=(200, 220, 160), grip=(30, 60, 54), tassel=(160, 250, 225)),
         shape=dict(shaft=46, head_len=22, head_w=5.6, tassel="white",
                    cloud_rings=2, lightning=False, snake_head=True, lugs=True,
                    butt=True)),
    dict(key="leiji", form="spear", cn="雷脊玄铁枪", tiers=(7, 8),
         color=P((88, 88, 128), (168, 168, 220), (20, 20, 50), (150, 132, 255),
                 (170, 150, 255), glow=(170, 150, 255), hem=(50, 40, 100),
                 gold=(180, 170, 220), grip=(28, 24, 60), tassel=(170, 160, 255)),
         shape=dict(shaft=46, head_len=18, head_w=6.4, tassel="blue",
                    lightning=True, serrated_head=True, lugs=True, butt=True)),
    dict(key="xueying", form="spear", cn="血缨魔纹枪", tiers=(9, 10),
         color=P((132, 72, 72), (202, 152, 152), (58, 10, 12), (255, 62, 62),
                 (255, 70, 70), glow=(255, 66, 66), hem=(110, 10, 16),
                 gold=(170, 150, 100), grip=(52, 10, 14), tassel=(240, 40, 50)),
         shape=dict(shaft=48, head_len=21, head_w=7.0, tassel="big",
                    groove=True, rune_bars=5, lugs=True, butt=True)),

    # ---------------- 棍 ----------------
    dict(key="jiangmo", form="staff", cn="降魔棍", tiers=(1, 3),
         color=P((124, 94, 52), (194, 162, 112), (70, 50, 26), (220, 180, 90),
                 (240, 210, 120), glow=(240, 210, 120), hem=(100, 70, 30),
                 gold=(226, 190, 96), grip=(64, 46, 24)),
         shape=dict(length=42, radius=2.9)),
    dict(key="panlong", form="staff", cn="盘龙棍", tiers=(4, 6),
         color=P((152, 122, 42), (232, 202, 116), (44, 58, 38), (130, 245, 170),
                 (150, 255, 190), glow=(150, 255, 190), hem=(70, 100, 50),
                 gold=(240, 206, 110), grip=(54, 40, 26)),
         shape=dict(length=44, radius=3.0)),
    dict(key="zhenhun", form="staff", cn="镇魂玄木棍", tiers=(7, 8),
         color=P((62, 52, 42), (112, 96, 76), (34, 28, 20), (140, 255, 200),
                 (140, 255, 200), glow=(140, 255, 200), hem=(50, 70, 56),
                 gold=(130, 120, 90), grip=(40, 32, 22)),
         shape=dict(length=44, radius=2.9)),
    dict(key="shougu", form="staff", cn="兽骨铃纹棍", tiers=(9, 10),
         color=P((204, 192, 164), (242, 236, 216), (66, 52, 34), (210, 180, 120),
                 (230, 200, 130), glow=(230, 200, 130), hem=(90, 72, 44),
                 gold=(200, 170, 110), grip=(60, 46, 30)),
         shape=dict(length=44, radius=3.0)),
]


def guard_sword(guard, style):
    if style == "cloud":
        add(guard, -2.2, 2.2, 0.0, 1.8, -1.6, 1.6)
        for s in (-1, 1):
            x0, x1 = (2.2, 4.6) if s > 0 else (-4.6, -2.2)
            add(guard, x0, x1, 0.4, 1.3, -1.1, 1.1)
            x0, x1 = (3.8, 5.2) if s > 0 else (-5.2, -3.8)
            add(guard, x0, x1, 0.8, 1.6, -0.7, 0.7, "jade")
    elif style == "flame":
        add(guard, -2.6, 2.6, 0.0, 2.0, -1.7, 1.7)
        for s in (-1, 1):
            for dx, dy, w, h in ((1.2, 0.4, 2.4, 1.6), (2.6, 1.3, 2.2, 2.4),
                                 (3.7, 3.0, 1.6, 3.0)):
                x0 = s * dx
                cx = x0 + s * w / 2
                add(guard, cx - w / 2, cx + w / 2, dy, dy + h, -1.0, 1.0)
            add(guard, 3.4 * s, 4.6 * s, 4.4, 5.4, -0.7, 0.7, "jade")
    elif style == "square":
        add(guard, -4.4, 4.4, 0.0, 2.0, -2.0, 2.0)
        for s in (-1, 1):
            x0 = 3.0 * s
            add(guard, min(x0, x0 + s * 0.6), max(x0, x0 + s * 0.6), 0.4, 1.4,
                -2.4, 2.4, "steel")
        for i in range(3):
            add(guard, -3.2 + i * 2.4, -2.7 + i * 2.4, 2.0, 2.7, -1.2, 1.2,
                "steel")
    elif style == "small":
        add(guard, -2.0, 2.0, 0.0, 1.6, -1.5, 1.5)
        add(guard, -0.9, 0.9, 0.2, 1.5, -1.9, -1.5, "jade")
    elif style == "bar":
        add(guard, -9.0, 9.0, 0.0, 2.2, -2.2, 2.2)
        for s in (-1, 1):
            x0 = 8.2 * s
            add(guard, min(x0, x0 + s * 1.4), max(x0, x0 + s * 1.4), 0.6, 2.8,
                -1.8, 1.8, "steel")
        add(guard, -1.0, 1.0, 0.4, 2.4, -2.4, -1.9, "steel")
    elif style == "stone":
        add(guard, -8.5, 8.5, 0.0, 3.0, -2.8, 2.8)
        for s in (-1, 1):
            x0 = 2.4 * s
            add(guard, min(x0, x0 + s * 3.6), max(x0, x0 + s * 3.6), 0.4, 3.0,
                -2.4, 2.4)
            add(guard, min(x0, x0 + s * 2.4), max(x0, x0 + s * 2.4), 3.0, 4.4,
                -1.6, 1.6, "steel")
            add(guard, min(6.6 * s, 7.6 * s), max(6.6 * s, 7.6 * s), 0.6, 2.4,
                -1.9, 1.9, "steel")
    elif style == "dragon":
        add(guard, -7.6, 7.6, 0.0, 2.6, -2.4, 2.4)
        for s in (-1, 1):
            x0 = 2.8 * s
            add(guard, min(x0, x0 + s * 3.0), max(x0, x0 + s * 3.0), 0.6, 2.8,
                -1.9, 1.9)
            add(guard, min(x0, x0 + s * 1.8), max(x0, x0 + s * 1.8), 2.8, 4.4,
                -1.2, 1.2, "jade")
            add(guard, min(5.8 * s, 6.9 * s), max(5.8 * s, 6.9 * s), 2.0, 3.8,
                -1.6, 1.6, "steel")
    elif style == "bone":
        add(guard, -7.0, 7.0, 0.0, 2.2, -2.2, 2.2, "steel")
        for s in (-1, 1):
            for i in range(4):
                x0 = s * (2.0 + i * 1.2)
                add(guard, min(x0, x0 + s * 0.9), max(x0, x0 + s * 0.9),
                    1.4 + i * 0.7, 2.4 + i * 1.0, -0.7, 0.7)


def build_sword(spec):
    if spec["shape"].get("w01r"):
        return build_frost_sword()
    g = Geo("geometry.career_sword_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 9.2, 0), parent="grip")
    top = {"cloud": 10.8, "flame": 11.2, "square": 10.8, "small": 10.6,
           "bar": 11.4, "stone": 11.8, "dragon": 11.6, "bone": 11.4}
    y0 = top.get(sh.get("guard", "cloud"), 10.8)
    blade = g.bone("blade", (0, y0, 0), parent="guard")
    add(grip, -1.35, 1.35, 0.0, 2.0, -1.35, 1.35)
    add(grip, -1.05, 1.05, 2.0, 2.4, -1.05, 1.05, "steel")
    add(grip, -0.95, 0.95, 2.4, 9.2, -0.95, 0.95, "cloth")
    for y in (3.4, 5.4, 7.4):
        add(grip, -1.3, 1.3, y, y + 0.4, -1.3, 1.3, "steel")
    guard_sword(guard, sh.get("guard", "cloud"))
    y1 = y0 + sh["blade_len"]
    taper_blade(blade, y0, y1, sh["w0"], sh["w1"], sh["t0"] * 1.3,
                sh["t1"] * 1.3, sh.get("segs", 4))
    # 刃口高光条（贴刃两侧，让剑"开刃"）
    for s in (-1, 1):
        x0 = sh["w0"] * 0.50 * s
        add(blade, min(x0, x0 - 0.16 * s), max(x0, x0 - 0.16 * s), y0 + 0.4,
            y1 - 0.6, -sh["t0"] * 0.30, sh["t0"] * 0.30, "steel")
    if sh.get("groove"):
        cw = sh["w0"] * 0.24
        add(blade, -cw, cw, y0 + 0.5, y1 - 0.5, -sh["t0"] * 0.40,
            sh["t0"] * 0.40, "steel")
        add(blade, -cw * 0.4, cw * 0.4, y0 + 0.8, y1 - 0.8,
            -sh["t0"] * 0.46, -sh["t0"] * 0.38, "metal")
    if sh.get("serrated"):
        for i in range(5):
            yy = y0 + 2.2 + i * (y1 - y0 - 3) / 5.0
            add(blade, sh["w0"] * 0.40, sh["w0"] * 0.68, yy, yy + 1.4,
                -0.7, 0.7, "steel")
    if sh.get("rune_bars"):
        for i in range(sh["rune_bars"]):
            yy = y0 + (i + 0.6) * (y1 - y0) / (sh["rune_bars"] + 0.2)
            add(blade, sh["w0"] * 0.16, sh["w0"] * 0.34, yy, yy + 1.2,
                -0.08, 0.08, "rune")
    step_point(blade, y1, y1 + 2.6, sh["w1"], sh["t1"], 3)
    if sh.get("skull"):
        add(grip, -2.2, 2.2, -0.7, 1.0, -2.2, 2.2, "steel")
        add(grip, -1.2, 1.2, 1.0, 2.2, -2.5, -1.7, "jade")
        add(grip, -1.2, 1.2, 1.0, 2.2, 1.7, 2.5, "jade")
    if sh.get("tassel"):
        t = g.bone("tassel", (0, 0, 0), parent="grip")
        for i in range(4):
            add(t, -0.7, 0.7, -1.1 - i * 1.0, -0.1 - i * 1.0,
                0.4 + i * 0.15, 1.2 + i * 0.15, "cloth")
    return g


def build_frost_sword():
    """霜纹古篆剑：连续剑身（无断口），符环贴身，玉简刻痕贴刃。"""
    g = Geo("geometry.career_sword_shuangwen", tex=(256, 256))
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 9.4, 0), parent="grip")
    blade = g.bone("blade", (0, 11.4, 0), parent="guard")
    ring_a = g.bone("ring_a", (0, 21.0, 0), parent="blade")
    ring_b = g.bone("ring_b", (0, 27.0, 0), parent="blade")
    add(grip, -1.6, 1.6, 0.0, 1.9, -1.6, 1.6)
    add(grip, -1.25, 1.25, 1.9, 2.3, -1.25, 1.25, "steel")
    add(grip, -1.05, 1.05, 2.3, 9.4, -1.05, 1.05, "cloth")
    for y in (3.5, 5.6, 7.7):
        add(grip, -1.25, 1.25, y, y + 0.4, -1.25, 1.25, "steel")
    add(guard, -2.3, 2.3, 9.4, 11.4, -1.8, 1.8)
    for s in (-1, 1):
        add(guard, 2.3 * s, 4.9 * s, 10.0, 11.2, -1.1, 1.1)
        add(guard, 3.2 * s, 4.6 * s, 11.2, 12.6, -0.8, 0.8, "jade")
        add(guard, 4.2 * s, 5.6 * s, 10.2, 11.6, -0.6, 0.6, "jade")
    add(guard, -0.9, 0.9, 9.6, 11.6, -2.1, -1.8, "rune")
    add(guard, -0.9, 0.9, 9.6, 11.6, 1.8, 2.1, "rune")
    y0, y1 = 11.4, 39.0
    taper_blade(blade, y0, y1, 4.8, 2.9, 1.9, 1.2, 7)
    for s in (-1, 1):
        x0 = 2.4 * s
        add(blade, min(x0, x0 - 0.2 * s), max(x0, x0 - 0.2 * s), y0 + 0.4,
            y1 - 0.6, -0.42, 0.42, "rune")
    # 篆文玉简：贴在刃面上（不悬空）
    for i, yy in enumerate((17.0, 23.5, 30.5)):
        add(blade, -0.8, 0.8, yy, yy + 1.6, -1.02, -0.9, "jade")
        add(blade, -0.5, 0.5, yy + 0.2, yy + 0.6, -1.06, -0.98, "rune")
        add(blade, -0.6, 0.6, yy + 0.9, yy + 1.2, -1.06, -0.98, "rune")
    step_point(blade, y1, 41.8, 2.9, 1.2, 3)
    ring_cubes(ring_a, 8, 21.0, 3.1, 0.5, mat="jade")
    ring_cubes(ring_b, 8, 27.0, 2.9, 0.5, mat="jade")
    t = g.bone("tassel", (0, 0, 0), parent="grip")
    for i in range(4):
        add(t, -0.65, 0.65, -1.1 - i * 1.0, -0.1 - i * 1.0,
            0.4 + i * 0.12, 1.1 + i * 0.12, "cloth")
    return g


def build_greatsword(spec):
    g = Geo("geometry.career_greatsword_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 13.5, 0), parent="grip")
    blade = g.bone("blade", (0, 17.0, 0), parent="guard")
    add(grip, -2.1, 2.1, 0.0, 2.6, -2.1, 2.1)
    add(grip, -1.6, 1.6, 2.6, 3.2, -1.6, 1.6, "steel")
    add(grip, -1.45, 1.45, 3.2, 13.5, -1.45, 1.45, "cloth")
    for y in (5.0, 7.6, 10.2):
        add(grip, -1.95, 1.95, y, y + 0.55, -1.95, 1.95, "steel")
    guard_sword(guard, sh.get("guard", "bar"))
    # 护手与剑身之间的连接段（消除"握把和剑身分开"）
    add(guard, -2.6, 2.6, 12.0, 15.4, -2.6, 2.6, "steel")
    add(blade, -2.0, 2.0, 13.2, 18.8, -2.0, 2.0, "metal")
    add(blade, -2.8, 2.8, 14.6, 16.4, -2.8, 2.8, "steel")
    add(blade, -1.4, 1.4, 16.2, 18.2, -2.4, 2.4, "metal")
    y0, y1 = 17.0, 17.0 + sh["blade_len"]
    w0, w1 = sh["w0"], sh["w1"]
    t0, t1 = sh["t0"], sh["t1"]
    taper_blade(blade, y0, y1, w0, w1, t0 * 1.1, t1 * 1.1, sh.get("segs", 6))
    # 剑根加宽（让巨剑在手里"压手"）
    add(blade, -w0 * 0.62, w0 * 0.62, y0, y0 + 4.2, -t0 * 0.62, t0 * 0.62,
        "metal")
    for s in (-1, 1):
        x0 = w0 * 0.5 * s
        add(blade, min(x0, x0 - 0.22 * s), max(x0, x0 - 0.22 * s), y0 + 0.4,
            y1 - 0.8, -t0 * 0.32, t0 * 0.32, "steel")
    add(blade, -1.6, 1.6, y0 + 0.6, y1 - 0.8, -t0 * 0.52, t0 * 0.52, "steel")
    if sh.get("groove"):
        add(blade, -2.6, 2.6, y0 + 0.8, y1 - 1.0, -t0 * 0.58, -t0 * 0.50,
            "metal")
        add(blade, -2.6, 2.6, y0 + 0.8, y1 - 1.0, t0 * 0.50, t0 * 0.58,
            "metal")
    if sh.get("spine"):
        for i in range(sh["spine"]):
            yy = y0 + 1.6 + i * (y1 - y0 - 3.2) / sh["spine"]
            add(blade, -w0 * 0.5, w0 * 0.5, yy, yy + 1.4,
                -t0 * 0.72, -t0 * 0.44, "jade")
    if sh.get("rocks"):
        add(blade, -w0 * 0.6, w0 * 0.6, y0 + 1.0, y0 + 5.0,
            -t0 * 0.9, t0 * 0.9, "steel")
        for s in (-1, 1):
            add(blade, w0 * 0.35 * s, w0 * 0.62 * s, y0 + 5.0, y0 + 7.4,
                -t0 * 0.7, t0 * 0.7, "metal")
    if sh.get("bone_spikes"):
        for i in range(5):
            yy = y0 + 2.0 + i * 6.4
            add(blade, -w0 * 0.66, -w0 * 0.50, yy, yy + 2.6, -0.7, 0.7,
                "steel")
            add(blade, w0 * 0.50, w0 * 0.66, yy + 3.0, yy + 5.6, -0.7, 0.7,
                "steel")
    if sh.get("rune_bars"):
        for i in range(sh["rune_bars"]):
            yy = y0 + (i + 0.6) * (y1 - y0) / (sh["rune_bars"] + 0.2)
            add(blade, w0 * 0.22, w0 * 0.36, yy, yy + 1.6, -0.08, 0.08,
                "rune")
    step_point(blade, y1, y1 + 3.2, w1, t1, 3)
    return g


def build_scythe(spec):
    """死神镰刀（1-8 阶）：杆略弯，刀身【横装】在杆顶 75~90°，套筒连接、楔形厚刃、平滑弧线。"""
    key = spec["key"]
    g = Geo("geometry.career_scythe_" + key, tex=(256, 256))
    S = 21.0
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    blade = g.bone("blade", (0, S, 0), parent="head")

    # ---- 杆：略弯（多段小漂移），上细下粗 ----
    segs = 10
    for i in range(segs):
        y0 = i * S / segs
        y1 = (i + 1) * S / segs
        drift0 = (i / segs) ** 1.7 * 1.5
        drift1 = ((i + 1) / segs) ** 1.7 * 1.5
        r = 1.05 - 0.15 * (i / segs)
        add(shaft, drift0 - r, drift1 + r, y0, y1, -r, r, "cloth")
        if i % 3 == 2:
            add(shaft, drift0 - r - 0.15, drift1 + r + 0.15, y0, y0 + 0.35,
                -r - 0.15, r + 0.15, "steel")
    # 上下握把 + 垂直握钉
    add(shaft, -1.35, 1.55, 7.0, 10.5, -1.35, 1.35, "cloth")
    add(shaft, -1.35, 1.45, 12.6, 15.8, -1.35, 1.35, "cloth")
    for y in (8.2, 14.0):
        add(shaft, -0.35, 0.35, y, y + 0.9, -2.15, 2.15, "steel")
        add(shaft, -0.55, 0.55, y + 0.3, y + 0.6, -2.35, 2.35, "metal")
    # 尾锤（配重锥）
    add(shaft, -2.0, 2.0, -1.4, 0.4, -2.0, 2.0, "metal")
    add(shaft, -1.5, 1.5, -2.6, -1.4, -1.5, 1.5, "metal")
    add(shaft, -0.8, 0.8, -4.4, -2.6, -0.8, 0.8, "steel")
    # 套筒（刀身插进去，0 间隙）
    add(head, -1.9, 1.9, S - 1.6, S + 3.0, -1.9, 1.9, "metal")
    add(head, -2.2, 2.2, S + 0.2, S + 1.2, -2.2, 2.2, "steel")
    add(head, -1.5, 1.5, S + 3.0, S + 4.6, -1.5, 1.5, "steel")

    # ---- 刀身：横向弧线（根部切线≈水平，向刀尖下弯），刀背厚/刃口薄 ----
    total = {"gouhun": 9.6, "sishen": 18.6, "xueyue": 14.8,
             "yougu": 15.6, "xukong": 21.5}[key]
    root_w = {"gouhun": 2.9, "sishen": 6.2, "xueyue": 4.8,
              "yougu": 5.0, "xukong": 7.2}[key]
    drop_k = {"gouhun": 2.2, "sishen": 7.2, "xueyue": 4.8,
              "yougu": 5.4, "xukong": 8.6}[key]
    rx, ry = 1.2, S + 2.6
    n = 12
    for i in range(n):
        f0, f1 = i / n, (i + 1) / n
        x0, x1 = rx + total * f0, rx + total * f1
        y0 = ry - drop_k * (f0 ** 1.7)
        y1 = ry - drop_k * (f1 ** 1.7)
        ang = math.degrees(math.atan2(y1 - y0, x1 - x0))
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2
        seg = math.dist((x0, y0), (x1, y1)) + 0.35
        h = root_w * (1.0 - 0.66 * f0)
        t = 3.0 - 1.2 * f0
        # 主体（楔形：靠上厚、靠下薄）
        blade.add((mx - seg / 2, my - h * 0.32, -t / 2),
                  (seg, h * 0.64, t), mat="metal",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        # 刀背加厚（上缘）
        blade.add((mx - seg / 2, my + h * 0.30, -t * 0.58),
                  (seg, h * 0.34, t * 1.16), mat="steel",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        # 刃口（下缘，薄）
        blade.add((mx - seg / 2, my - h * 0.46, -0.36),
                  (seg, 1.0, 0.72), mat="steel",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        if key in ("sishen", "xukong") and i % 2 == 0:
            # 锯齿背（黑曜尖齿，沿刀背外侧）
            nx, ny = math.cos(math.radians(ang + 90)), math.sin(math.radians(ang + 90))
            sx, sy = mx + nx * (h * 0.54), my + ny * (h * 0.54)
            blade.add((sx - 0.7, sy - 0.7, -t * 0.45), (1.4, 1.4, t * 0.9),
                      mat="steel", rotation=(0, 0, ang), pivot=(sx, sy, 0))
        if key in ("sishen", "xukong") and i in (2, 5, 8, 11):
            # 刀脊魂火（幽绿发光）
            nx, ny = math.cos(math.radians(ang + 90)), math.sin(math.radians(ang + 90))
            sx, sy = mx + nx * (h * 0.20), my + ny * (h * 0.20)
            blade.add((sx - 0.35, sy - 0.35, -t / 2 - 0.10),
                      (0.7, 0.7, t + 0.20), mat="rune",
                      rotation=(0, 0, ang), pivot=(sx, sy, 0))
        if key == "xueyue" and i in (3, 6, 9):
            blade.add((mx - seg / 2, my - h * 0.08, -t / 2 - 0.07),
                      (seg, h * 0.26, t + 0.14), mat="rune",
                      rotation=(0, 0, ang), pivot=(mx, my, 0))
        if key == "yougu" and i % 2 == 0:
            blade.add((mx - seg / 2, my + h * 0.50, -t * 0.40),
                      (seg * 0.8, 1.2, t * 0.8), mat="steel",
                      rotation=(0, 0, ang), pivot=(mx, my, 0))
            add_blade_gem(blade, mx + seg * 0.25, my + h * 0.55)
    # 刀尖收锋
    tipx = rx + total + 0.4
    tipy = ry - drop_k
    blade.add((tipx - 1.2, tipy - 0.6, -0.45), (2.6, 1.1, 0.9), mat="steel",
              rotation=(0, 0, -24), pivot=(tipx, tipy, 0))
    blade.add((tipx + 1.4, tipy - 1.5, -0.28), (1.8, 0.8, 0.56), mat="steel",
              rotation=(0, 0, -24), pivot=(tipx, tipy, 0))
    # 刀根 tang：从套筒伸进刀身，保证 0 间隙连接
    blade.add((0.2, ry - 2.2, -1.2), (1.8, 4.2, 2.4), mat="steel")

    if key in ("sishen", "xukong"):
        # 骷髅刃座（前后各一个）
        for z in (-1.0, 1.0):
            add(head, -1.9, 1.9, S - 0.6, S + 2.8, z * 2.4 - 0.8, z * 2.4 + 0.8,
                "steel")
            add(head, -1.2, 1.2, S + 0.2, S + 1.6, z * 2.7 - 0.6, z * 2.7 + 0.6,
                "jade")
            add(head, -0.9, 0.9, S - 1.2, S + 0.2, z * 2.5 - 0.9, z * 2.5 + 0.9,
                "steel")
        # 反向小刃（根部下方，死神镰的副刃）
        for i in range(3):
            blade.add((2.0 + i * 1.3, S + 1.0 - i * 0.9, -0.5),
                      (1.3, 1.0, 1.0), mat="steel",
                      rotation=(0, 0, -30 - i * 12), pivot=(2.0 + i * 1.3, S + 1.0, 0))
        # 刃座锁链
        for i in range(4):
            add(head, -3.2 - i * 0.9, -2.6 - i * 0.9, S - 2.2 - i * 1.1,
                S - 1.4 - i * 1.1, -0.3, 0.3, "steel")
        # 破布条（挂在刃座下）
        for i, dz in enumerate((-2.2, -0.8, 0.8, 2.2)):
            add(head, -0.7, 0.7, S - 7.0 - (i % 2) * 1.2, S - 1.0,
                dz - 0.5, dz + 0.5, "cloth")
        # 魂火
        add(head, -1.2, 1.2, S - 2.0, S - 0.8, -3.4, -2.8, "rune")
        if key == "xukong":
            for i in range(3):
                px = math.cos(0.6 + i * 1.5) * 4.0
                py = S - 4.0 + math.sin(0.6 + i * 1.5) * 4.0
                add(head, px - 0.9, px + 0.9, py - 0.9, py + 0.9, -0.6, 0.6, "jade")
        add(head, -0.8, 0.8, S - 3.2, S - 2.0, 2.8, 3.4, "jade")
    elif key == "gouhun":
        add(head, -1.6, 1.6, S - 0.8, S + 2.8, -3.1, -2.3, "steel")
        add(head, -0.9, 0.9, S, S + 1.4, -3.5, -3.0, "jade")
        for i in range(3):
            add(head, 1.5 - 0.35 * i, 2.7 - 0.15 * i,
                S - 3.0 - i * 1.5, S - 2.0 - i * 1.5, -0.35, 0.35, "steel")
    elif key == "xueyue":
        for i in range(18):
            a = 2 * math.pi * i / 18
            cx, cy = math.cos(a) * 3.2, S + 3.4 + math.sin(a) * 3.2
            head.add((cx - 0.55, cy - 0.55, -0.7), (1.1, 1.1, 1.4), mat="jade")
        add(head, -1.2, 1.2, S - 1.2, S + 2.2, -3.3, -2.7, "rune")
    else:
        for i in range(3):
            add(head, -2.4 + i * 1.9, -1.3 + i * 1.9, S + 2.4, S + 3.6,
                -2.7, -2.0, "rune")
        add(head, -1.1, 1.1, S - 1.4, S + 3.0, 2.2, 2.9, "rune")
        add(head, -2.5, 2.5, S + 3.1, S + 4.3, -2.5, 2.5, "steel")
    return g


def add_blade_gem(blade, x, y):
    blade.add((x - 0.35, y - 0.35, -0.5), (0.7, 0.7, 1.0), mat="jade")


def build_war_scythe(spec):
    """战镰（9-10 阶）：刀身沿杆延长线（同向），外侧开刃、内侧刀脊，根部护手。"""
    g = Geo("geometry.career_scythe_xukong", tex=(256, 256))
    S = 18.0
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    blade = g.bone("blade", (0, S, 0), parent="head")
    segs = 8
    for i in range(segs):
        y0 = i * S / segs
        y1 = (i + 1) * S / segs
        drift0 = (i / segs) ** 1.5 * 2.2
        drift1 = ((i + 1) / segs) ** 1.5 * 2.2
        r = 1.05 - 0.12 * (i / segs)
        add(shaft, drift0 - r, drift1 + r, y0, y1, -r, r, "cloth")
    for y in (6.0, 12.0):
        add(shaft, -2.0, 2.4, y, y + 3.0, -2.0, 2.0, "cloth")
        add(shaft, -0.45, 0.45, y + 1.2, y + 2.0, -2.7, 2.7, "steel")
    add(shaft, -2.2, 2.2, -2.2, 0.4, -2.2, 2.2, "metal")
    add(shaft, -1.5, 1.5, -3.9, -2.2, -1.5, 1.5, "metal")
    add(head, -2.0, 2.0, S - 1.6, S + 3.0, -2.0, 2.0, "metal")
    add(head, -3.4, 3.4, S + 0.4, S + 1.6, -0.9, 0.9, "steel")
    add(head, -1.7, 1.7, S + 3.0, S + 4.6, -1.7, 1.7, "steel")
    n = 11
    total = 14.0
    rx, ry = 1.4, S + 3.0
    for i in range(n):
        f0, f1 = i / n, (i + 1) / n
        y0, y1 = ry + total * f0, ry + total * f1
        x0, x1 = rx + 1.4 * f0 * f0, rx + 1.4 * f1 * f1
        ang = math.degrees(math.atan2(y1 - y0, x1 - x0))
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2
        seg = math.dist((x0, y0), (x1, y1)) + 0.3
        w = 3.8 * (1.0 - 0.62 * f0)
        t = 3.0 - 1.2 * f0
        blade.add((mx - w * 0.35, my - seg / 2, -t / 2),
                  (w * 0.7, seg, t), mat="metal",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        blade.add((mx - w * 0.35, my - seg / 2, -t * 0.62),
                  (w * 0.42, seg, t * 1.24), mat="steel",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        blade.add((mx + w * 0.30, my - seg / 2, -0.3), (0.9, seg, 0.6),
                  mat="steel", rotation=(0, 0, ang), pivot=(mx, my, 0))
        if i in (4, 7):
            blade.add((mx - w * 0.1, my - seg / 2, -t / 2 - 0.07),
                      (0.7, seg, t + 0.14), mat="rune",
                      rotation=(0, 0, ang), pivot=(mx, my, 0))
    tx, ty = rx + 1.4, ry + total
    blade.add((tx - 0.7, ty - 0.3, -0.45), (2.0, 3.2, 0.9), mat="steel",
              rotation=(0, 0, -26), pivot=(tx, ty, 0))
    for i in range(3):
        a = math.radians(30 + i * 22)
        px = math.cos(a) * 5.0 + 2.4
        py = S + 8.0 + math.sin(a) * 5.0
        add(blade, px - 0.9, px + 0.9, py - 0.9, py + 0.9, -0.5, 0.5, "jade")
    return g


def build_spear(spec):
    """长枪：总长 ~2.2 格；枪头在顶部占 20~25%（叶形+中脊+厚度），枪箍/红缨/尾锥/握把。"""
    g = Geo("geometry.career_spear_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    key = spec["key"]
    S = 31.0
    hw = 6.9 if key != "youlong" else 6.2
    hl = 10.6 if key != "youlong" else 11.2
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    tassel = g.bone("tassel", (0, S, 0), parent="shaft")
    nseg = 7
    for i in range(nseg):
        y0 = i * S / nseg
        y1 = (i + 1) * S / nseg
        r0 = 1.28 - 0.18 * (i / nseg)
        r1 = 1.28 - 0.18 * ((i + 1) / nseg)
        add(shaft, -r0, r0, y0, y1, -r0, r0, "cloth")
    add(shaft, -1.55, 1.55, 8.5, 16.8, -1.55, 1.55, "cloth")
    for y in (7.0, 10.5, 14.0):
        add(shaft, -1.65, 1.65, y, y + 0.7, -1.65, 1.65, "steel")
    # 尾锥
    add(shaft, -1.9, 1.9, -1.8, 0.8, -1.9, 1.9, "metal")
    add(shaft, -1.2, 1.2, -4.6, -1.6, -1.2, 1.2, "steel")
    add(shaft, -0.4, 0.4, -6.0, -4.4, -0.4, 0.4, "steel")
    # 枪箍/套筒 + 护片
    add(head, -1.9, 1.9, S - 2.4, S + 1.2, -1.9, 1.9, "metal")
    add(head, -2.2, 2.2, S - 1.4, S - 0.2, -2.2, 2.2, "steel")
    for s in (-1, 1):
        add(head, 0.8 * s, 1.9 * s, S - 6.4, S - 2.2, -0.55, 0.55, "steel")
        add(head, 2.1 * s, 2.6 * s, S - 3.2, S - 2.4, -0.9, 0.9, "jade")
    # 枪头：叶形（宽叶），中脊，尖端 3 段收锋
    y0 = S + 1.2
    prof = [0.42, 0.72, 0.96, 1.0, 0.88, 0.65, 0.34]
    for i, fw in enumerate(prof):
        ya = y0 + hl * i / len(prof)
        yb = y0 + hl * (i + 1) / len(prof)
        w = hw * fw
        th = 2.5 - 0.14 * i
        add(head, -w / 2, w / 2, ya, yb + 0.15, -th / 2, th / 2, "metal")
    # 中脊
    add(head, -0.85, 0.85, y0 + 0.3, y0 + hl - 0.7, -1.2, 1.2, "steel")
    # 刃口高光
    for s in (-1, 1):
        add(head, hw * 0.36 * s, hw * 0.50 * s, y0 + 0.5, y0 + hl - 1.2,
            -0.45, 0.45, "steel")
        add(head, hw * 0.20 * s, hw * 0.50 * s, y0 + 0.4, y0 + hl * 0.45,
            -1.3, -1.15, "jade")
    # 尖锋
    step_point(head, y0 + hl, y0 + hl + 2.4, hw * 0.34, 1.0, 2)
    if key == "leiji":
        for i in range(4):
            add(head, hw * 0.44, hw * 0.72, y0 + 1.8 + i * 1.6,
                y0 + 2.6 + i * 1.6, -0.5, 0.5, "rune")
            add(head, -hw * 0.72, -hw * 0.44, y0 + 2.6 + i * 1.6,
                y0 + 3.4 + i * 1.6, -0.5, 0.5, "rune")
    if key == "xueying":
        add(head, -hw * 0.07, hw * 0.07, y0 + 0.4, y0 + hl - 0.8, -1.25, 1.25,
            "rune")
        for i in range(4):
            add(head, hw * 0.46, hw * 0.66, y0 + 1.4 + i * 1.5,
                y0 + 2.2 + i * 1.5, -0.5, 0.5, "steel")
    if key == "youlong":
        add(head, -hw * 0.40, hw * 0.40, y0 + 0.3, y0 + 2.2, -1.25, 1.25,
            "jade")
        for s in (-1, 1):
            add(head, hw * 0.30 * s, hw * 0.50 * s, y0 + 1.0, y0 + 1.8,
                -1.30, -0.95, "rune")
    if key == "pojun":
        for i in range(3):
            add(head, -hw * 0.60 + i * 1.5, -hw * 0.36 + i * 1.5, y0 + 0.4,
                y0 + 3.2, -1.25, 1.25, "steel")
    # 红缨：枪头下方一圈，多段下垂
    radius = 1.8
    for i in range(8):
        a = math.radians(i * 45)
        px, pz = math.cos(a) * radius, math.sin(a) * radius
        tassel.add((px - 0.55, S - 5.4 - (i % 2) * 0.8, pz - 0.55),
                   (1.1, 5.0, 1.1), mat="cloth",
                   rotation=(14 if i % 2 == 0 else -14, 0, 0),
                   pivot=(px, S + 1.0, pz))
    return g


def build_staff(spec):
    """棍：总长 ~1.7 格、杆细（半径 1.4）、两端端帽、中段握把、身份件（环/龙/符/铃）。"""
    g = Geo("geometry.career_staff_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    key = spec["key"]
    L = 24.0
    R = 1.15
    shaft = g.bone("shaft", (0, 0, 0))
    nseg = 6
    for i in range(nseg):
        y0 = i * L / nseg
        y1 = (i + 1) * L / nseg
        r0 = R + 0.10 * (1 - i / nseg)
        r1 = R + 0.10 * (1 - (i + 1) / nseg)
        add(shaft, -r0, r0, y0, y1, -r0, r0, "cloth")
    # 中段握把（缠绳）
    add(shaft, -R - 0.35, R + 0.35, 9.5, 14.5, -R - 0.35, R + 0.35, "cloth")
    for y in range(10, 15):
        add(shaft, -R - 0.45, R + 0.45, y, y + 0.35, -R - 0.45, R + 0.45,
            "steel")
    # 两端端帽
    for y0 in (0.0, L - 2.4):
        add(shaft, -1.8, 1.8, y0, y0 + 2.4, -1.8, 1.8, "metal")
        add(shaft, -1.2, 1.2, y0 + 2.4, y0 + 3.1, -1.2, 1.2, "steel")
        add(shaft, -2.0, 2.0, y0 + 0.7, y0 + 1.3, -2.0, 2.0, "jade")
    if key == "jiangmo":
        # 降魔锡杖：顶部三道大环 + 顶针 + 环坠
        add(shaft, -0.9, 0.9, L + 0.4, L + 2.4, -0.9, 0.9, "metal")
        for i, rr in enumerate((3.6, 3.0, 2.4)):
            ring_cubes(shaft, 12, L + 3.2 + i * 2.2, rr, 0.7,
                       mat="jade", tilt=90.0)
        add(shaft, -0.6, 0.6, L + 9.4, L + 12.0, -0.6, 0.6, "steel")
        add(shaft, -0.3, 0.3, L + 12.0, L + 13.2, -0.3, 0.3, "rune")
        for s in (-1, 1):
            for i in range(3):
                add(shaft, s * 2.6 - 0.3, s * 2.6 + 0.3,
                    L + 2.8 + i * 2.2 - 2.0, L + 2.8 + i * 2.2 - 1.1,
                    -0.3, 0.3, "steel")
    elif key == "panlong":
        for i in range(16):
            yy = 3.5 + i * 1.15
            a = i * 2.0
            cx, cz = math.cos(a) * (R + 0.55), math.sin(a) * (R + 0.55)
            add(shaft, cx - 0.45, cx + 0.45, yy, yy + 0.75, cz - 0.45,
                cz + 0.45, "metal")
        add(shaft, -2.0, 2.0, L + 0.2, L + 2.8, -1.6, 1.6, "metal")
        add(shaft, -1.3, 1.3, L + 2.8, L + 4.6, -1.2, 1.2, "metal")
        for s in (-1, 1):
            add(shaft, 1.4 * s, 2.6 * s, L + 1.6, L + 3.6, -0.65, 0.65,
                "steel")
            add(shaft, 0.6 * s, 1.2 * s, L + 3.0, L + 3.7, -1.5, -0.9,
                "rune")
        add(shaft, -0.8, 0.8, L + 4.6, L + 5.8, -0.8, 0.8, "rune")
    elif key == "zhenhun":
        # 镇魂：两端幽火灯（笼 + 核心 + 垂饰）
        for y0 in (-6.2, L - 1.0):
            add(shaft, -2.6, 2.6, y0, y0 + 6.4, -2.6, 2.6, "metal")
            add(shaft, -2.9, 2.9, y0 + 0.6, y0 + 1.6, -2.9, 2.9, "metal")
            add(shaft, -2.9, 2.9, y0 + 4.8, y0 + 5.8, -2.9, 2.9, "metal")
            add(shaft, -1.5, 1.5, y0 + 1.8, y0 + 4.6, -1.7, 1.7, "rune")
            add(shaft, -2.0, 2.0, y0 + 2.4, y0 + 3.0, -2.0, 2.0, "jade")
            for s in (-1, 1):
                add(shaft, 2.1 * s - 0.4, 2.1 * s + 0.4, y0 + 1.4,
                    y0 + 5.0, -0.4, 0.4, "steel")
        add(shaft, -0.9, 0.9, L + 5.4, L + 7.4, -0.9, 0.9, "rune")
        add(shaft, -0.9, 0.9, -8.6, -6.4, -0.9, 0.9, "rune")
        for sgn in (-1, 1):
            add(shaft, 2.0 * sgn - 0.8, 2.0 * sgn + 0.8, L - 2.0, L + 4.0,
                -0.25, 0.25, "cloth")
            add(shaft, 2.0 * sgn - 0.6, 2.0 * sgn + 0.6, L - 4.2, L - 2.0,
                -0.2, 0.2, "rune")
        for i in range(4):
            add(shaft, -3.0, -2.2, L * 0.55 - i * 1.4, L * 0.55 - i * 1.4 + 1.0,
                -0.25, 0.25, "steel")
        add(shaft, -0.9, 0.9, L + 4.8, L + 6.2, -0.9, 0.9, "rune")
        add(shaft, -0.9, 0.9, -8.4, -7.0, -0.9, 0.9, "rune")
        for i in range(3):
            add(shaft, -2.0 + i * 1.0, -1.4 + i * 1.0, L * 0.30 + i * 1.5,
                L * 0.30 + i * 1.5 + 1.0, 1.4, 2.0, "steel")
    else:  # shougu
        for i in range(6):
            yy = 3.2 + i * (L - 6.4) / 6
            add(shaft, -1.9, 1.9, yy, yy + 1.2, -1.9, 1.9, "steel")
            add(shaft, -2.2, 2.2, yy + 1.2, yy + 1.9, -2.2, 2.2, "cloth")
        add(shaft, -2.1, 2.1, L - 1.0, L + 2.2, -2.1, 2.1, "steel")
        add(shaft, -1.4, 1.4, L + 2.2, L + 4.0, -1.4, 1.4, "steel")
        for s in (-1, 1):
            add(shaft, 0.8 * s, 1.6 * s, L + 2.8, L + 3.6, -2.0, -1.4, "rune")
            add(shaft, 1.5 * s, 2.4 * s, L + 3.6, L + 6.2, -0.6, 0.6, "steel")
            add(shaft, 1.2 * s - 0.3, 1.2 * s + 0.3, -3.4, -1.0, -0.3, 0.3,
                "steel")
        add(shaft, -1.5, 1.5, -6.0, -3.4, -1.5, 1.5, "steel")
        add(shaft, -0.7, 0.7, -7.6, -6.0, -0.7, 0.7, "jade")
    return g


BUILDERS = {
    "sword": build_sword,
    "greatsword": build_greatsword,
    "scythe": build_scythe,
    "spear": build_spear,
    "staff": build_staff,
}


def write_anim(family, key):
    if key == "shuangwen":
        anim = {"format_version": "1.8.0", "animations": {"idle": {
            "loop": True, "animation_length": 4.0, "bones": {
                "ring_a": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, -360, 0]}},
                "ring_b": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]}},
                "tassel": {"rotation": {
                    "0.0": [0, 0, -7], "1.0": [0, 0, 7], "2.0": [0, 0, -7],
                    "3.0": [0, 0, 7], "4.0": [0, 0, -7]}},
            }}}}
    else:
        anim = {"format_version": "1.8.0", "animations": {
            "idle": {"loop": True, "animation_length": 2.0, "bones": {}}}}
    p = os.path.join(RES, "animations", family + ".animation.json")
    with open(p, "w", encoding="utf-8") as f:
        json.dump(anim, f, ensure_ascii=False, indent=2)


def main():
    previews = []
    stats = []
    mapping = {}
    for spec in SPECS:
        form, key = spec["form"], spec["key"]
        geo = BUILDERS[form](spec)
        family = "career_%s_%s" % (form, key)
        geo.identifier = "geometry." + family
        k, _ = g2_tex.pack_geo(geo, (256, 256))
        geo.tex = (256, 256)
        geo.save(os.path.join(RES, "geo", family + ".geo.json"))
        c = spec["color"]
        tex_p = os.path.join(RES, "textures", "item", family + ".png")
        glow_p = os.path.join(RES, "textures", "item", family + "_glowmask.png")
        ratio, _ = g2_tex.paint_geo(geo, (256, 256), tex_p, glow_p,
                                    pal=c["pal"], glow_color=c["glow"],
                                    hem=c["hem"], gold=c["gold"],
                                    grip_color=c["grip"],
                                    tassel_color=c["tassel"])
        write_anim(family, key)
        tex_arr = np.array(Image.open(tex_p).convert("RGB"))
        glow_arr = np.array(Image.open(glow_p).convert("RGB"))
        prev = os.path.join(OUTROOT, "prev_%s.png" % family)
        render_views(build_mesh(geo), prev, views=("front", "side", "hero"),
                     size=(260, 560),
                     label="%s（%d-%d 阶）" % (spec["cn"], spec["tiers"][0],
                                              spec["tiers"][1]),
                     texture=tex_arr, glow=glow_arr)
        # 32px / 64px 纯黑剪影（返修单要求的"剪影测试"）
        sil = os.path.join(OUTROOT, "sil_%s.png" % family)
        render_views(build_mesh(geo), sil, views=("front",), size=(256, 512),
                     transparent=True, unlit=True,
                     texture=np.zeros_like(tex_arr), glow=None)
        base_sil = Image.open(sil).convert("RGBA")
        alpha = base_sil.split()[-1]
        for px in (32, 64):
            black = Image.new("RGBA", base_sil.size, (0, 0, 0, 0))
            black.putalpha(alpha)
            black = black.resize((px, px * 2), Image.NEAREST)
            black.save(os.path.join(OUTROOT,
                                    "sil%d_%s.png" % (px, family)))
        previews.append(prev)
        stats.append({"family": family, "cn": spec["cn"], "form": form,
                      "tiers": list(spec["tiers"]), "cubes": geo.cube_count(),
                      "k": k, "glow": round(ratio, 4)})
        mapping[family] = list(range(spec["tiers"][0], spec["tiers"][1] + 1))
        print("[OK] %-28s %-10s %d-%d cubes=%3d k=%d glow=%.1f%%"
              % (family, spec["cn"], spec["tiers"][0], spec["tiers"][1],
                 geo.cube_count(), k, ratio * 100))

    # 档位 -> family（Java 侧同步用）
    tier_family = {}
    for form in ("sword", "greatsword", "scythe", "spear", "staff"):
        for tier in range(1, 11):
            found = None
            for spec in SPECS:
                if spec["form"] == form and spec["tiers"][0] <= tier <= spec["tiers"][1]:
                    found = "career_%s_%s" % (form, spec["key"])
                    break
            tier_family["career_weapon_%s_%d" % (form, tier)] = found
    with open(os.path.join(OUTROOT, "mapping.json"), "w", encoding="utf-8") as f:
        json.dump({"families": mapping, "tier_to_family": tier_family,
                   "stats": stats}, f, ensure_ascii=False, indent=2)
    contact_sheet(previews, os.path.join(OUTROOT, "0_WEAPONS_21.png"), cols=4,
                  title="R-2 名器 21 把（按名字定形状与配色）")
    # 剪影测试图：21 把 × 32px 并排
    sheet = Image.new("RGBA", (21 * 40 + 16, 120), (32, 35, 46, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((8, 4), "32px 剪影测试（返修单 §1.6）", fill=(230, 235, 245, 255))
    for idx, spec in enumerate(SPECS):
        fam = "career_%s_%s" % (spec["form"], spec["key"])
        p32 = os.path.join(OUTROOT, "sil32_%s.png" % fam)
        if os.path.exists(p32):
            im = Image.open(p32).convert("RGBA")
            im = im.resize((32, 64), Image.NEAREST)
            sheet.alpha_composite(im, (8 + idx * 40, 26))
    sheet.save(os.path.join(OUTROOT, "0_SILHOUETTES_32.png"))
    print("[DONE] 21 把名器 →", RES)


if __name__ == "__main__":
    main()
