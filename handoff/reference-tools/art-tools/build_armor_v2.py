# -*- coding: utf-8 -*-
"""
职业甲外观 v2：10 套各自独立形制 + 配色（不再同骨架换色）
=========================================================
设计表：`工程/art/装备外观重做-设计表v2.md` §二。

每套一个 spec：躯干/裙摆/肩甲/头盔/靴/附加件 + 独立调色板；
严格遵守 GeckoLib 护甲 8 骨契约（armorHead / armorBody / armorRightArm / armorLeftArm /
armorRightLeg / armorLeftLeg / armorRightBoot / armorLeftBoot），所有几何都在这 8 根骨里。

剑修进阶套（剑心道袍）沿用 W-01R 已交付版本，不重做。
"""
from __future__ import annotations

import copy
import json
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import g2_tex
from artlib import (Geo, build_mesh, contact_sheet, render_views, ring_cubes)
from build_w01r import build_armor as build_w01r_armor

HERE = os.path.dirname(os.path.abspath(__file__))
W01R = os.path.abspath(os.path.join(HERE, "..", "w01r"))
PREV = os.path.join(W01R, "armor_v2")
WS = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
os.makedirs(PREV, exist_ok=True)


def add(bone, x0, x1, y0, y1, z0, z1, mat="metal", **kw):
    # 允许传入任意顺序的区间（左右镜像时端点会反过来），统一归一化
    bone.add((min(x0, x1), min(y0, y1), min(z0, z1)),
             (abs(x1 - x0), abs(y1 - y0), abs(z1 - z0)), mat=mat, **kw)


# ---------------------------------------------------------------------------
# 调色板
# ---------------------------------------------------------------------------

def pal(metal, steel, cloth, jade, rune, glow, hem, gold, grip=None,
        tassel=None):
    return dict(pal={"metal": metal, "steel": steel, "cloth": cloth,
                     "jade": jade, "rune": rune},
                glow=glow, hem=hem, gold=gold, grip=grip, tassel=tassel)


SPECS = [
    dict(id="jian_1", cn="青锋剑袍", head="daocrown", torso="robe",
         skirt="cloth", shoulder="slim", arm="sleeve", boot="cloud",
         extras=[],
         color=pal((188, 214, 228), (240, 248, 252), (226, 240, 248),
                   (140, 220, 240), (150, 235, 245), (150, 235, 245),
                   (70, 150, 190), (190, 200, 120))),
    dict(id="xue_1", cn="血纹战衣", head="headband", torso="warrobe",
         skirt="cloth", shoulder="slim", arm="sleeve", boot="cloth",
         extras=["banner"],
         color=pal((150, 138, 140), (208, 196, 194), (112, 22, 30),
                   (230, 74, 74), (255, 74, 74), (255, 74, 74),
                   (120, 14, 22), (206, 168, 84))),
    dict(id="xue_2", cn="血海战衣", head="horncirclet", torso="warrobe",
         skirt="cloth", shoulder="spike", arm="plate", boot="plate",
         extras=["cape"],
         color=pal((120, 70, 74), (190, 150, 150), (70, 12, 16),
                   (255, 60, 60), (255, 60, 60), (255, 60, 60),
                   (100, 10, 14), (190, 150, 90))),
    dict(id="mo_1", cn="骸骨魔铠", head="skullcrown", torso="ribs",
         skirt="bone", shoulder="skull", arm="sleeve", boot="plate",
         extras=[],
         color=pal((190, 182, 164), (232, 226, 208), (24, 24, 28),
                   (176, 96, 255), (176, 96, 255), (176, 96, 255),
                   (60, 40, 80), (150, 140, 110))),
    dict(id="mo_2", cn="魔渊魔铠", head="horned", torso="plate",
         skirt="plate", shoulder="huge", arm="plate", boot="plate",
         extras=["eye"],
         color=pal((52, 44, 62), (110, 96, 128), (30, 20, 40),
                   (176, 96, 255), (176, 96, 255), (176, 96, 255),
                   (60, 30, 90), (150, 120, 170))),
    dict(id="gui_1", cn="幽魂道袍", head="hood", torso="robe",
         skirt="tatter", shoulder="slim", arm="sleeve", boot="cloth",
         extras=["veil", "wisps"],
         color=pal((150, 176, 166), (200, 222, 212), (30, 48, 42),
                   (118, 255, 184), (102, 242, 200), (102, 242, 200),
                   (38, 108, 88), (170, 176, 120))),
    dict(id="gui_2", cn="黄泉鬼袍", head="lanterncrown", torso="robe",
         skirt="longrobe", shoulder="slim", arm="wide", boot="cloth",
         extras=["lantern", "chain"],
         color=pal((140, 138, 96), (200, 198, 150), (44, 44, 26),
                   (216, 255, 112), (216, 255, 112), (216, 255, 112),
                   (90, 84, 40), (190, 170, 100))),
    dict(id="ti_1", cn="玄铁重甲", head="visor", torso="heavy",
         skirt="plate", shoulder="huge", arm="plate", boot="heavy",
         extras=[],
         color=pal((58, 58, 64), (122, 126, 134), (44, 42, 40),
                   (150, 150, 150), (170, 180, 185), (120, 130, 140),
                   (70, 66, 60), (140, 120, 90))),
    dict(id="ti_2", cn="不灭金身", head="halo", torso="gold",
         skirt="plate", shoulder="huge", arm="plate", boot="heavy",
         extras=["halo_ring"],
         color=pal((140, 106, 32), (224, 184, 80), (120, 22, 22),
                   (255, 215, 92), (255, 215, 92), (255, 215, 92),
                   (140, 90, 20), (250, 220, 130))),
    # ================= C 扩充：每职业第三套「名器」=================
    dict(id="jian_3", cn="剑仙羽衣", head="daocrown", torso="robe",
         skirt="cloth", shoulder="slim", arm="sleeve", boot="cloud",
         extras=["cape", "halo_ring"],
         color=pal((236, 242, 250), (255, 255, 255), (222, 238, 250),
                   (150, 230, 250), (170, 240, 255), (170, 240, 255),
                   (120, 190, 230), (236, 206, 120))),
    dict(id="xue_3", cn="血神袍", head="horncirclet", torso="warrobe",
         skirt="longrobe", shoulder="spike", arm="plate", boot="plate",
         extras=["cape", "banner"],
         color=pal((170, 92, 96), (226, 168, 164), (86, 12, 16),
                   (255, 70, 70), (255, 76, 76), (255, 76, 76),
                   (130, 10, 16), (236, 196, 116))),
    dict(id="mo_3", cn="天魔甲", head="horned", torso="heavy",
         skirt="plate", shoulder="huge", arm="plate", boot="heavy",
         extras=["cape", "eye"],
         color=pal((64, 48, 78), (132, 104, 156), (30, 18, 40),
                   (210, 120, 255), (200, 96, 255), (200, 96, 255),
                   (70, 26, 100), (176, 150, 210))),
    dict(id="gui_3", cn="幽冥纱", head="hood", torso="robe",
         skirt="tatter", shoulder="slim", arm="wide", boot="cloth",
         extras=["veil", "wisps", "chain"],
         color=pal((196, 208, 202), (236, 242, 238), (36, 46, 44),
                   (110, 255, 200), (110, 255, 200), (110, 255, 200),
                   (44, 100, 88), (200, 200, 170))),
    dict(id="ti_3", cn="不灭圣铠", head="halo", torso="gold",
         skirt="plate", shoulder="huge", arm="plate", boot="heavy",
         extras=["halo_ring", "cape"],
         color=pal((168, 128, 36), (248, 216, 120), (128, 20, 20),
                   (255, 236, 140), (255, 240, 150), (255, 240, 150),
                   (160, 100, 24), (255, 232, 150))),
]


# ---------------------------------------------------------------------------
# 部件构造
# ---------------------------------------------------------------------------

def torso_robe(b, spec, long_hem=False):
    add(b, -4.5, 4.5, 21.9, 23.4, -3.0, -2.55, "cloth")   # 交领前
    add(b, -4.3, 4.3, 14.8, 21.9, -2.9, -2.2, "cloth")    # 前袍身
    add(b, -4.3, 4.3, 14.8, 23.2, 2.2, 2.9, "cloth")      # 后袍身
    for s in (-1, 1):
        x0 = -4.6 if s < 0 else 4.0
        add(b, x0, x0 + 0.6, 13.0, 23.0, -2.1, 2.1, "cloth")
    add(b, -4.2, 4.2, 12.6, 13.6, -2.7, -2.3, "cloth")
    add(b, -4.6, 4.6, 11.4, 12.6, -3.0, 2.4, "steel")      # 腰带
    add(b, -1.2, 1.2, 11.1, 12.7, -3.3, -2.9, "metal")
    add(b, -1.9, 1.9, 21.5, 23.2, -3.3, -2.9, "metal")     # 胸甲小片


def torso_warrobe(b, spec):
    torso_robe(b, spec)
    add(b, -2.6, 2.6, 17.0, 21.2, -3.15, -2.85, "metal")
    add(b, -0.5, 0.5, 17.2, 20.8, -3.3, -3.05, "rune")        # 血纹
    for s in (-1, 1):
        add(b, 3.0 * s - 0.4, 3.0 * s + 0.4, 18.4, 19.6, -3.25, -3.05, "rune")


def torso_ribs(b, spec):
    add(b, -4.3, 4.3, 13.0, 23.0, -2.8, 2.8, "cloth")
    for i in range(3):
        y = 15.0 + i * 2.6
        add(b, -3.9 + i * 0.3, 3.9 - i * 0.3, y, y + 1.1, -3.0, -2.5, "steel")
        add(b, -3.9 + i * 0.3, 3.9 - i * 0.3, y, y + 1.1, 2.5, 3.0, "steel")
    add(b, -1.3, 1.3, 18.6, 21.4, -3.35, -3.0, "rune")        # 魔眼
    add(b, -4.6, 4.6, 11.4, 12.8, -3.0, 2.8, "steel")
    add(b, -1.0, 1.0, 10.8, 11.6, -3.2, -2.7, "metal")


def torso_plate(b, spec):
    for y0, y1, x in ((20.0, 23.2, 4.6), (16.4, 20.2, 4.4), (13.2, 16.6, 4.2)):
        add(b, -x, x, y0, y1, -3.0, -2.3, "metal")
        add(b, -x, x, y0, y1, 2.3, 3.0, "metal")
        add(b, -x - 0.35, -x + 0.35, y0, y1, -2.3, 2.3, "metal")
        add(b, x - 0.35, x + 0.35, y0, y1, -2.3, 2.3, "metal")
    add(b, -1.0, 1.0, 14.0, 23.0, -3.2, -2.9, "steel")
    add(b, -4.9, 4.9, 11.4, 13.2, -3.1, 2.9, "steel")
    add(b, -1.1, 1.1, 11.0, 13.4, -3.4, -3.0, "metal")


def torso_heavy(b, spec):
    torso_plate(b, spec)
    add(b, -4.8, 4.8, 20.0, 23.4, -3.3, 3.3, "metal")     # 厚肩基座
    add(b, -4.6, 4.6, 13.0, 19.8, -3.3, 3.3, "metal")
    add(b, -0.8, 0.8, 13.2, 19.6, -3.5, -3.1, "steel")


def torso_gold(b, spec):
    torso_plate(b, spec)
    for y in (22.6, 18.0, 13.8):
        add(b, -4.7, 4.7, y, y + 0.5, -3.25, -2.95, "jade")
    add(b, -1.5, 1.5, 17.8, 21.0, -3.5, -3.05, "rune")     # 不灭印
    add(b, -1.0, 1.0, 12.0, 13.0, -3.3, -2.9, "jade")


def leg_skirt(leg, sign, kind):
    x = 1 if sign > 0 else -1
    if kind == "cloth":
        add(leg, 0.6 * x, 3.6 * x, 5.6, 11.2, -3.0, -2.45, "cloth")
        add(leg, 0.6 * x, 3.6 * x, 4.8, 5.6, -3.0, -2.45, "cloth")
        add(leg, 0.6 * x, 3.6 * x, 5.0, 11.2, 2.45, 3.0, "cloth")
        add(leg, 3.5 * x, 4.3 * x, 5.4, 11.2, -2.3, 2.3, "cloth")
    elif kind == "tatter":
        for i, (x0, x1, y0, y1) in enumerate(((0.7, 3.6, 3.2, 11.2),
                                               (0.9, 2.6, 2.2, 3.4),
                                               (2.4, 3.6, 1.4, 2.4))):
            add(leg, x0 * x, x1 * x, y0, y1, -2.9, -2.45, "cloth")
        add(leg, 3.4 * x, 4.2 * x, 3.0, 9.0, -2.1, 2.1, "cloth")
    elif kind == "longrobe":
        add(leg, 0.5 * x, 3.7 * x, 1.6, 11.2, -3.0, -2.45, "cloth")
        add(leg, 0.5 * x, 3.7 * x, 1.6, 10.4, 2.45, 3.0, "cloth")
        add(leg, 3.4 * x, 4.2 * x, 2.0, 10.6, -2.2, 2.2, "cloth")
    elif kind == "plate":
        for i in range(2):
            y0 = 7.2 + i * 2.2
            add(leg, 0.5 * x, 3.8 * x, y0, y0 + 1.9, -2.8, -2.2, "metal")
            add(leg, 3.5 * x, 4.3 * x, y0, y0 + 1.9, -2.0, 2.0, "metal")
        add(leg, 0.6 * x, 3.9 * x, 4.2, 5.8, -2.9, -2.3, "steel")
    elif kind == "bone":
        for i in range(3):
            y0 = 6.0 + i * 1.8
            add(leg, 0.6 * x, 3.6 * x, y0, y0 + 1.2, -2.85, -2.3, "steel")
        add(leg, 0.8 * x, 3.6 * x, 3.4, 4.6, -2.9, -2.4, "jade")


def leg_guard(leg, sign, spec):
    x = 1 if sign > 0 else -1
    kind = spec["skirt"]
    if kind in ("cloth", "tatter", "longrobe"):
        add(leg, 0.5 * x, 3.7 * x, 1.5, 4.2, -2.5, -1.95, "metal")
        add(leg, 3.65 * x, 4.15 * x, 1.8, 4.2, -2.2, 2.2, "metal")
    else:
        add(leg, 0.4 * x, 3.8 * x, 1.4, 4.4, -2.6, -2.0, "metal")
        add(leg, 3.6 * x, 4.2 * x, 1.6, 4.4, -2.3, 2.3, "metal")


def boot_part(boot, sign, kind):
    x = 1 if sign > 0 else -1
    if kind == "cloud":
        add(boot, 0.45 * x, 4.15 * x, 0.0, 0.7, -3.1, 2.35, "steel")
        add(boot, 0.4 * x, 4.05 * x, 0.7, 2.0, -2.45, 2.25, "metal")
        add(boot, 0.8 * x, 3.5 * x, 1.6, 2.8, -3.15, -2.55, "jade")
        add(boot, 0.45 * x, 3.9 * x, 2.0, 4.6, -2.45, -1.95, "metal")
        add(boot, 0.45 * x, 3.9 * x, 4.3, 4.9, -2.5, 2.5, "rune")
    elif kind == "cloth":
        add(boot, 0.5 * x, 4.1 * x, 0.0, 0.6, -3.0, 2.3, "steel")
        add(boot, 0.45 * x, 4.0 * x, 0.6, 1.9, -2.4, 2.2, "cloth")
        add(boot, 0.45 * x, 4.0 * x, 1.9, 4.2, -2.4, 2.2, "cloth")
    elif kind == "plate":
        add(boot, 0.4 * x, 4.15 * x, 0.0, 1.0, -3.2, 2.4, "metal")
        add(boot, 0.35 * x, 4.05 * x, 1.0, 2.2, -2.5, 2.3, "metal")
        add(boot, 0.4 * x, 3.95 * x, 2.2, 4.6, -2.5, -2.0, "metal")
        add(boot, 0.4 * x, 3.95 * x, 2.2, 4.6, 2.0, 2.5, "metal")
        add(boot, 3.7 * x, 4.2 * x, 2.2, 4.6, -2.2, 2.2, "metal")
    elif kind == "heavy":
        add(boot, 0.3 * x, 4.2 * x, 0.0, 1.2, -3.3, 2.5, "metal")
        add(boot, 0.25 * x, 4.1 * x, 1.2, 2.6, -2.6, 2.4, "metal")
        add(boot, 0.3 * x, 4.0 * x, 2.6, 5.2, -2.6, -2.0, "metal")
        add(boot, 0.3 * x, 4.0 * x, 2.6, 5.2, 2.0, 2.6, "metal")
        add(boot, 3.9 * x, 4.3 * x, 2.6, 5.2, -2.4, 2.4, "metal")
        add(boot, 0.5 * x, 3.9 * x, 5.0, 5.6, -2.7, 2.7, "steel")


def pauldron(arm, sign, kind, spec):
    x = 1 if sign > 0 else -1
    if kind == "slim":
        add(arm, 3.9 * x, 7.5 * x, 23.2, 24.6, -2.9, 2.9, "metal")
        add(arm, 3.7 * x, 8.2 * x, 21.8, 23.4, -3.0, 3.0, "metal")
        add(arm, 7.8 * x, 8.3 * x, 21.9, 24.2, -2.3, 2.3, "steel")
        add(arm, 4.6 * x, 5.4 * x, 24.4, 25.2, -1.8, 1.8, "jade")
    elif kind == "spike":
        add(arm, 3.7 * x, 8.4 * x, 21.6, 24.8, -3.1, 3.1, "metal")
        for i in range(2):
            add(arm, (7.2 + i) * x, (8.2 + i) * x, 24.6, 25.6 + i * 1.2,
                -1.2, 1.2, "steel")
        add(arm, 3.9 * x, 7.9 * x, 20.2, 21.8, -3.0, 3.0, "metal")
    elif kind == "skull":
        add(arm, 3.9 * x, 8.2 * x, 21.6, 24.8, -3.0, 3.0, "steel")
        add(arm, 5.0 * x, 7.2 * x, 24.8, 26.6, -1.5, 1.5, "steel")
        add(arm, 5.4 * x, 6.8 * x, 25.0, 26.2, -1.9, -1.5, "rune")
        add(arm, 5.4 * x, 6.8 * x, 25.0, 26.2, 1.5, 1.9, "rune")
    elif kind == "huge":
        add(arm, 3.8 * x, 8.8 * x, 22.8, 25.4, -3.2, 3.2, "metal")
        add(arm, 3.5 * x, 9.6 * x, 20.6, 23.4, -3.2, 3.2, "metal")
        add(arm, 3.2 * x, 10.2 * x, 18.8, 21.2, -3.0, 3.0, "metal")
        add(arm, 9.8 * x, 10.4 * x, 19.0, 24.6, -2.4, 2.4, "steel")
        add(arm, 8.6 * x, 10.3 * x, 24.8, 26.0, -2.2, 2.2, "steel")
    add(arm, 3.9 * x, 8.0 * x, 12.4, 16.2, -2.5, 2.5, "cloth" if spec["arm"] == "sleeve" else "metal")
    if spec["arm"] == "plate":
        add(arm, 3.8 * x, 8.1 * x, 12.2, 16.4, -2.6, -2.0, "metal")
        add(arm, 3.8 * x, 8.1 * x, 12.2, 16.4, 2.0, 2.6, "metal")
        add(arm, 7.7 * x, 8.2 * x, 12.2, 16.4, -2.0, 2.0, "steel")
    elif spec["arm"] == "wide":
        add(arm, 3.2 * x, 8.4 * x, 12.6, 16.4, -3.0, 3.0, "cloth")


def helm(head, kind, spec):
    if kind == "daocrown":
        add(head, -4.5, 4.5, 30.4, 31.8, -4.7, -4.0, "metal")
        add(head, -4.7, -4.0, 29.4, 31.8, -4.0, 4.0, "metal")
        add(head, 4.0, 4.7, 29.4, 31.8, -4.0, 4.0, "metal")
        add(head, -4.5, 4.5, 29.4, 31.8, 4.0, 4.7, "metal")
        add(head, -2.6, 2.6, 31.8, 33.4, -2.6, 2.6, "steel")
        add(head, -0.7, 0.7, 33.4, 35.6, -4.6, -4.1, "jade")
        add(head, -0.3, 0.3, 35.6, 36.6, -4.5, -4.1, "rune")
    elif kind == "headband":
        add(head, -4.6, 4.6, 29.4, 30.8, -4.8, -4.1, "cloth")
        add(head, -4.7, -4.1, 28.6, 30.8, -4.1, 4.1, "cloth")
        add(head, 4.1, 4.7, 28.6, 30.8, -4.1, 4.1, "cloth")
        add(head, -4.6, 4.6, 28.6, 30.8, 4.1, 4.8, "cloth")
        add(head, -0.8, 0.8, 30.8, 32.6, -4.5, -4.0, "jade")
    elif kind == "horncirclet":
        add(head, -4.5, 4.5, 30.2, 31.6, -4.7, -4.1, "metal")
        add(head, -4.6, -4.0, 29.2, 31.6, -4.0, 4.0, "metal")
        add(head, 4.0, 4.6, 29.2, 31.6, -4.0, 4.0, "metal")
        for s in (-1, 1):
            add(head, 4.4 * s, 5.4 * s, 31.6, 34.4, -1.0, 1.0, "steel")
            add(head, 4.8 * s, 5.6 * s, 34.4, 36.0, -0.6, 0.6, "jade")
    elif kind == "skullcrown":
        add(head, -4.4, 4.4, 29.6, 31.4, -4.6, -4.0, "steel")
        add(head, -4.4, 4.4, 31.4, 33.2, -4.0, 4.0, "steel")
        for s in (-1, 1):
            add(head, 3.4 * s, 4.6 * s, 33.2, 35.2, -1.2, 1.2, "steel")
        add(head, -1.4, 1.4, 31.6, 33.0, -4.5, -3.9, "rune")
        add(head, -0.8, 0.8, 30.4, 31.6, -4.6, -4.1, "rune")
    elif kind == "horned":
        add(head, -4.6, 4.6, 28.8, 31.6, -4.7, 4.7, "metal")
        add(head, -3.2, 4.0, 26.4, 29.0, -4.6, -4.0, "metal")   # 面甲
        for s in (-1, 1):
            add(head, (3.6 + 0) * s, 4.7 * s, 30.6, 34.6, -1.2, 1.2, "steel")
            add(head, 4.4 * s, 5.6 * s, 34.6, 37.4, -0.7, 0.7, "jade")
        add(head, -3.6, -2.4, 27.0, 28.6, -4.65, -4.1, "rune")
        add(head, 2.4, 3.6, 27.0, 28.6, -4.65, -4.1, "rune")
    elif kind == "hood":
        add(head, -4.6, 4.6, 30.8, 33.6, -4.6, 4.6, "cloth")
        add(head, -4.8, -4.2, 27.6, 31.0, -4.2, 4.2, "cloth")
        add(head, 4.2, 4.8, 27.6, 31.0, -4.2, 4.2, "cloth")
        add(head, -4.6, 4.6, 27.6, 30.8, 4.2, 4.9, "cloth")
        add(head, -1.2, 1.2, 30.8, 32.3, -4.7, -4.2, "rune")
    elif kind == "lanterncrown":
        add(head, -4.4, 4.4, 30.2, 31.4, -4.6, -4.0, "metal")
        for s in (-1, 1):
            add(head, 4.0 * s, 4.7 * s, 29.2, 31.4, -4.0, 4.0, "metal")
        add(head, -1.2, 1.2, 31.4, 32.4, -4.4, -3.9, "steel")
        add(head, -0.9, 0.9, 32.4, 33.6, -4.3, -3.9, "rune")   # 冥灯
    elif kind == "visor":
        add(head, -4.8, 4.8, 28.6, 32.2, -4.7, -4.0, "metal")
        add(head, -4.9, -4.0, 28.6, 32.2, -4.2, 4.2, "metal")
        add(head, 4.0, 4.9, 28.6, 32.2, -4.2, 4.2, "metal")
        add(head, -4.8, 4.8, 28.6, 32.2, 4.0, 4.9, "metal")
        add(head, -4.8, 4.8, 26.8, 28.8, -4.7, -4.1, "metal")
        add(head, -0.9, 0.9, 27.0, 28.4, -4.8, -4.2, "steel")
    elif kind == "halo":
        add(head, -4.2, 4.2, 30.0, 31.6, -4.5, -4.0, "metal")
        for s in (-1, 1):
            add(head, 3.8 * s, 4.6 * s, 29.0, 31.6, -4.0, 4.0, "metal")
        add(head, -1.1, 1.1, 31.6, 33.4, -4.4, -3.9, "jade")
        add(head, -0.7, 0.7, 33.4, 34.4, -4.3, -3.9, "rune")


def extras(spec, body, head, legs):
    ex = spec["extras"]
    if "banner" in ex:
        add(body, -2.6, -1.7, 4.0, 10.6, -3.1, -2.7, "cloth")
        add(body, 1.7, 2.6, 4.0, 10.6, -3.1, -2.7, "cloth")
    if "cape" in ex:
        add(body, -4.4, 4.4, 3.4, 13.0, 3.0, 3.4, "cloth", rotation=(-6, 0, 0),
            pivot=(0, 13.0, 3.2))
    if "ribs" in ex:
        for i in range(3):
            y = 15.4 + i * 2.4
            add(body, -3.6, 3.6, y, y + 0.9, -3.15, -2.85, "rune")
    if "veil" in ex:
        add(body, -3.6, -2.6, 2.6, 10.4, 3.0, 3.3, "cloth")
        add(body, 2.6, 3.6, 2.6, 10.4, 3.0, 3.3, "cloth")
    if "wisps" in ex:
        for s in (-1, 1):
            body.add((4.6 * s - 0.6, 25.8, 1.2), (1.2, 1.2, 1.2), mat="jade")
    if "lantern" in ex:
        add(body, -0.9, 0.9, 7.4, 9.6, -3.3, -2.9, "rune")
        add(body, -1.3, 1.3, 9.6, 10.4, -3.2, -3.0, "steel")
    if "chain" in ex:
        for i in range(4):
            add(body, -4.0 + i * 2.4, -3.2 + i * 2.4, 16.0, 16.6, -3.3,
                -3.05, "steel")
    if "eye" in ex:
        add(body, -1.1, 1.1, 18.4, 20.6, -3.5, -3.05, "rune")
        add(body, -0.6, 0.6, 19.0, 20.0, -3.7, -3.4, "jade")
    if "halo_ring" in ex:
        ring = ring_cubes
        # 后脑金环（挂在头骨上，保持 8 骨契约）
        for i in range(16):
            import math
            a = 2 * math.pi * i / 16
            cx, cy = math.cos(a) * 4.6, 26.0 + math.sin(a) * 4.6
            head.add((cx - 0.35, cy - 0.35, 5.2 - 0.35), (0.7, 0.7, 0.7),
                     mat="jade", rotation=(0, 0, math.degrees(a)))


def arm_full(arm, sign, spec):
    """补满整条手臂：上臂/护肘/手套（v2 只做了肘部一段，游戏里看起来"手臂断了"）。"""
    x = 1 if sign > 0 else -1
    if spec["arm"] in ("sleeve", "wide"):
        add(arm, 3.5 * x, 8.2 * x, 16.2, 23.6, -2.6, 2.6, "cloth")
        add(arm, 3.3 * x, 8.4 * x, 22.6, 23.8, -2.7, 2.7, "metal")
        add(arm, 3.6 * x, 8.1 * x, 15.8, 16.8, -2.5, 2.5, "steel")
        if spec["arm"] == "wide":
            add(arm, 2.8 * x, 8.9 * x, 17.2, 20.6, -3.3, 3.3, "cloth")
            add(arm, 3.1 * x, 8.6 * x, 19.4, 20.4, -3.0, 3.0, "jade")
    else:
        add(arm, 3.5 * x, 8.2 * x, 16.2, 23.6, -2.7, 2.7, "metal")
        add(arm, 3.2 * x, 8.5 * x, 20.4, 22.2, -2.9, 2.9, "metal")
        add(arm, 7.9 * x, 8.6 * x, 16.2, 23.2, -2.2, 2.2, "steel")
        add(arm, 3.4 * x, 8.3 * x, 16.4, 17.2, -2.8, 2.8, "steel")
    # 手套 + 护腕
    add(arm, 3.6 * x, 8.1 * x, 11.6, 13.0, -2.6, 2.6, "metal")
    add(arm, 3.4 * x, 8.3 * x, 13.0, 14.2, -2.8, 2.8, "steel")
    if spec["id"] in ("ti_1", "ti_2"):
        add(arm, 3.2 * x, 8.6 * x, 13.6, 15.0, -3.0, 3.0, "metal")


def helm_crest(head, spec):
    """头盔冠饰：按套加剑冠/血翎/骨角/兜帽尖/冥火/面甲脊/金环。"""
    i = spec["id"]
    if i == "jian_1":
        add(head, -0.6, 0.6, 33.4, 36.4, -4.6, -4.0, "jade")
        add(head, -1.2, 1.2, 33.0, 34.2, -4.6, -4.1, "metal")
        for s in (-1, 1):
            add(head, 4.2 * s, 5.4 * s, 32.2, 33.6, -1.2, 1.2, "metal")
    elif i == "xue_1":
        for i2 in range(3):
            add(head, -1.0, 1.0, 31.8 + i2 * 1.1, 32.6 + i2 * 1.1,
                -1.8 + i2 * 0.5, -0.9 + i2 * 0.5, "cloth")
        add(head, -0.7, 0.7, 31.4, 32.0, -4.5, -4.0, "rune")
    elif i == "xue_2":
        for s in (-1, 1):
            add(head, 4.0 * s, 5.6 * s, 32.4, 36.0, -1.4, 1.4, "steel")
            add(head, 4.6 * s, 5.4 * s, 36.0, 38.4, -0.8, 0.8, "jade")
        for i2 in range(4):
            add(head, -0.9, 0.9, 32.4 + i2 * 1.4, 33.6 + i2 * 1.4,
                -2.2 + i2 * 0.6, -1.2 + i2 * 0.6, "cloth")
    elif i == "mo_1":
        for s in (-1, 1):
            add(head, 3.4 * s, 4.6 * s, 33.2, 36.4, -1.2, 1.2, "steel")
            add(head, 3.9 * s, 4.4 * s, 36.4, 38.6, -0.7, 0.7, "steel")
        add(head, -2.2, 2.2, 30.6, 31.8, -4.4, -4.0, "jade")
    elif i == "mo_2":
        add(head, -1.1, 1.1, 30.0, 31.4, -4.8, -4.2, "rune")
        for s in (-1, 1):
            add(head, 4.6 * s, 5.4 * s, 30.6, 33.6, -1.0, 1.0, "steel")
            add(head, 3.2 * s, 4.0 * s, 34.4, 36.8, -0.8, 0.8, "jade")
    elif i == "gui_1":
        add(head, -1.0, 1.0, 33.6, 35.8, -2.4, -1.6, "cloth")
        add(head, -2.2, 2.2, 27.4, 28.6, 4.2, 4.8, "cloth")
    elif i == "gui_2":
        for i2, x0 in enumerate((-1.6, -0.5, 0.5)):
            add(head, x0, x0 + 1.1, 31.6, 34.6 - i2 * 0.4, -4.4, -3.9,
                "rune")
        add(head, -0.5, 0.5, 34.2, 35.6, -4.2, -3.9, "jade")
    elif i == "ti_1":
        add(head, -0.8, 0.8, 32.2, 34.4, -4.9, -4.3, "steel")
        add(head, -2.6, 2.6, 31.6, 32.4, -4.8, -4.2, "metal")
    elif i == "ti_2":
        add(head, -0.9, 0.9, 32.0, 34.0, -4.5, -4.0, "jade")
        for s in (-1, 1):
            add(head, 4.2 * s, 6.0 * s, 31.0, 33.2, -0.9, 0.9, "metal")


def deco_v3(body, head, arms, legs, boots, spec):
    """"加料"：披风/肩刺/胸口纹章/腰饰/膝甲/悬浮晶片，按套区分。"""
    i = spec["id"]
    if i == "jian_1":
        add(body, -4.6, -3.6, 12.9, 13.5, -3.1, -2.6, "jade")
        add(body, 3.6, 4.6, 12.9, 13.5, -3.1, -2.6, "jade")
        add(body, -0.7, 0.7, 10.6, 12.4, -3.2, -2.8, "jade")
        add(body, -2.4, 2.4, 19.6, 20.2, -3.2, -2.9, "jade")
    elif i == "xue_1":
        add(body, -2.2, 2.2, 18.4, 20.8, -3.3, -3.0, "rune")
        add(body, -4.2, 4.2, 12.8, 13.6, -3.2, 2.9, "metal")
        add(body, -1.4, 1.4, 6.2, 12.6, -3.1, -2.7, "cloth")
    elif i == "xue_2":
        add(body, -5.0, 5.0, 3.6, 13.6, 3.1, 3.7, "cloth")
        add(body, -4.4, 4.4, 2.4, 4.2, 3.0, 3.5, "cloth")
        for s in (-1, 1):
            add(arms[0 if s < 0 else 1], 3.4 * s, 9.6 * s, 24.8, 26.6,
                -1.6, 1.6, "steel")
        add(body, -1.6, 1.6, 17.4, 20.4, -3.4, -3.0, "rune")
    elif i == "mo_1":
        for y in (13.4, 16.2, 19.0, 21.8):
            add(body, -0.9, 0.9, y, y + 1.4, 2.9, 3.3, "steel")
        add(body, -1.5, 1.5, 17.6, 20.6, -3.5, -3.0, "rune")
        for s in (-1, 1):
            add(body, 5.2 * s - 0.8, 5.2 * s + 0.8, 24.0, 25.6, 1.4, 3.0,
                "jade")
    elif i == "mo_2":
        add(body, -1.4, 1.4, 17.8, 20.8, -3.6, -3.0, "rune")
        add(body, -0.7, 0.7, 18.4, 20.0, -3.8, -3.4, "jade")
        add(body, -5.2, 5.2, 3.2, 13.4, 3.1, 3.8, "cloth")
        for s in (-1, 1):
            add(body, 5.0 * s - 0.9, 5.0 * s + 0.9, 25.2, 27.0, -0.9, 0.9,
                "jade")
            add(body, 4.8 * s - 0.7, 4.8 * s + 0.7, 21.0, 22.4, 1.6, 2.6,
                "rune")
    elif i == "gui_1":
        for s in (-1, 1):
            add(body, 3.8 * s - 0.9, 3.8 * s + 0.9, 24.4, 26.4, 1.0, 2.2,
                "jade")
            add(body, 3.2 * s - 0.5, 3.2 * s + 0.5, 20.4, 21.4, 2.6, 3.1,
                "rune")
        add(body, -3.4, -2.4, 2.0, 10.6, 3.0, 3.4, "cloth")
        add(body, 2.4, 3.4, 2.0, 10.6, 3.0, 3.4, "cloth")
    elif i == "gui_2":
        add(body, -1.4, 1.4, 6.8, 10.2, -3.5, -3.0, "rune")
        for s in (-1, 1):
            add(body, 2.4 * s, 4.6 * s, 13.6, 14.6, -3.2, -2.9, "steel")
            add(body, 4.4 * s - 0.8, 4.4 * s + 0.8, 21.4, 23.4, 2.2, 3.2,
                "jade")
    elif i == "ti_1":
        add(body, -5.2, 5.2, 14.6, 16.0, -3.4, 3.4, "metal")
        add(body, -1.0, 1.0, 13.4, 15.0, 3.2, 3.7, "steel")
        for s in (-1, 1):
            add(legs[0 if s < 0 else 1], 0.6 * s, 3.7 * s, 5.8, 7.4,
                -3.0, -2.4, "metal")
            add(boots[0 if s < 0 else 1], 0.4 * s, 4.0 * s, 0.0, 1.4,
                -3.4, -2.8, "steel")
    elif i == "ti_2":
        add(body, -1.8, 1.8, 17.6, 21.2, -3.6, -3.0, "rune")
        add(body, -0.9, 0.9, 18.2, 20.4, -3.8, -3.4, "jade")
        add(body, -4.8, 4.8, 12.9, 13.7, -3.3, 3.0, "jade")
        for s in (-1, 1):
            add(body, 4.6 * s - 0.9, 4.6 * s + 0.9, 25.0, 27.2, -0.8, 0.8,
                "jade")
            add(arms[0 if s < 0 else 1], 3.2 * s, 9.2 * s, 24.6, 26.2,
                -1.5, 1.5, "metal")


def helm_shell(head, spec):
    """完整的头盔外壳：盖住头顶/两侧/后脑（否则游戏里只剩一圈飘着的装饰带）。"""
    kind = spec["head"]
    metal = "cloth" if kind in ("hood", "lanterncrown", "headband", "daocrown") \
        else "metal"
    # 头顶
    add(head, -4.7, 4.7, 31.6, 32.8, -4.7, 4.7, metal)
    # 后脑 + 两侧
    add(head, -4.7, 4.7, 26.6, 31.6, 3.9, 4.7, metal)
    add(head, -4.8, -4.0, 26.6, 31.8, -4.3, 4.3, metal)
    add(head, 4.0, 4.8, 26.6, 31.8, -4.3, 4.3, metal)
    if kind in ("visor", "horned"):
        # 面甲：上额 + 下颚，中间留眼缝
        add(head, -4.6, 4.6, 29.8, 31.6, -4.9, -4.2, "metal")
        add(head, -4.6, 4.6, 26.4, 28.4, -4.9, -4.2, "metal")
        add(head, -1.0, 1.0, 27.6, 29.0, -5.0, -4.4, "steel")
    elif kind in ("daocrown", "skullcrown", "horncirclet", "lanterncrown"):
        # 前额帽檐
        add(head, -4.6, 4.6, 30.6, 31.8, -4.9, -4.2, metal)
    elif kind == "hood":
        add(head, -4.7, 4.7, 28.4, 31.8, -4.6, -4.0, "cloth")
        add(head, -2.0, 2.0, 26.2, 28.6, -4.6, -4.1, "cloth")
    elif kind == "halo":
        add(head, -4.6, 4.6, 30.4, 31.8, -4.9, -4.2, "metal")
    # 护颈
    add(head, -3.6, 3.6, 25.2, 26.6, -3.0, 3.4, "steel")
    add(head, -3.2, 3.2, 24.4, 25.4, -2.4, 3.0, "cloth")


def build_armor(spec):
    g = Geo("geometry.career_armor_" + spec["id"], tex=(256, 256))
    head = g.bone("armorHead", (0, 24, 0))
    body = g.bone("armorBody", (0, 24, 0))
    ra = g.bone("armorRightArm", (-5, 22, 0))
    la = g.bone("armorLeftArm", (5, 22, 0))
    rl = g.bone("armorRightLeg", (-1.9, 12, 0))
    ll = g.bone("armorLeftLeg", (1.9, 12, 0))
    rb = g.bone("armorRightBoot", (-1.9, 12, 0))
    lb = g.bone("armorLeftBoot", (1.9, 12, 0))

    torso = {"robe": torso_robe, "warrobe": torso_warrobe, "ribs": torso_ribs,
             "plate": torso_plate, "heavy": torso_heavy,
             "gold": torso_gold}[spec["torso"]]
    torso(body, spec)
    helm(head, spec["head"], spec)
    helm_shell(head, spec)
    helm_crest(head, spec)
    for leg, sign in ((rl, -1), (ll, 1)):
        leg_skirt(leg, sign, spec["skirt"])
        leg_guard(leg, sign, spec)
    for arm, sign in ((ra, -1), (la, 1)):
        pauldron(arm, sign, spec["shoulder"], spec)
        arm_full(arm, sign, spec)
    for boot, sign in ((rb, -1), (lb, 1)):
        boot_part(boot, sign, spec["boot"])
    extras(spec, body, head, [rl, ll])
    deco_v3(body, head, [ra, la], [rl, ll], [rb, lb], spec)
    return g


def main():
    previews = []
    stats = []
    for spec in SPECS:
        if spec["id"] == "jian_2":
            continue
        geo = build_armor(spec)
        k, _ = g2_tex.pack_geo(geo, (256, 256))
        geo.tex = (256, 256)
        name = "career_armor_" + spec["id"]
        geo.identifier = "geometry." + name
        geo.save(os.path.join(RES, "geo", name + ".geo.json"))
        c = spec["color"]
        tex_p = os.path.join(RES, "textures", "item", name + ".png")
        glow_p = os.path.join(RES, "textures", "item", name + "_glowmask.png")
        ratio, _ = g2_tex.paint_geo(geo, (256, 256), tex_p, glow_p,
                                    pal=c["pal"], glow_color=c["glow"],
                                    hem=c["hem"], gold=c["gold"],
                                    grip_color=c.get("grip"),
                                    tassel_color=c.get("tassel"))
        tex_arr = np.array(Image.open(tex_p).convert("RGB"))
        glow_arr = np.array(Image.open(glow_p).convert("RGB"))
        for slot, bones in (
                ("helmet", ["armorHead"]),
                ("chestplate", ["armorBody", "armorRightArm", "armorLeftArm"]),
                ("leggings", ["armorRightLeg", "armorLeftLeg"]),
                ("boots", ["armorRightBoot", "armorLeftBoot"])):
            hidden = [b.name for b in geo.bones if b.name not in bones]
            tmp = os.path.join(PREV, "_tmp.png")
            render_views(build_mesh(geo, hidden=hidden), tmp, views=("front",),
                         size=(512, 512), transparent=True, unlit=True,
                         texture=tex_arr, glow=glow_arr)
            g2_tex.make_icon(tmp, os.path.join(RES, "textures", "item",
                                               name + "_" + slot + ".png"), 32)
        if os.path.exists(os.path.join(PREV, "_tmp.png")):
            os.remove(os.path.join(PREV, "_tmp.png"))
        prev = os.path.join(PREV, "prev_%s.png" % name)
        render_views(build_mesh(geo), prev, views=("front", "side", "hero"),
                     size=(380, 560), label="%s · 外观 v2" % spec["cn"],
                     texture=tex_arr, glow=glow_arr)
        previews.append(prev)
        stats.append({"set": name, "cn": spec["cn"], "cubes": geo.cube_count(),
                      "k": k, "glow": round(ratio, 4)})
        print("[OK] %-26s %-6s cubes=%3d k=%d glow=%.1f%%"
              % (name, spec["cn"], geo.cube_count(), k, ratio * 100))

    contact_sheet(previews, os.path.join(PREV, "0_ARMOR_V2.png"), cols=3,
                  title="职业甲 9 套外观 v2（按名字定形制与配色；剑修进阶沿用 W-01R）")
    with open(os.path.join(PREV, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)
    print("[DONE] 盔甲 v2 →", RES)


if __name__ == "__main__":
    main()
