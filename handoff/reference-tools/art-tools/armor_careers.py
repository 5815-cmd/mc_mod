# -*- coding: utf-8 -*-
"""
五职业 · 盔甲专属形制（重甲/轻甲/道袍/骨甲/纱袍）
=================================================
玩家反馈「每个职业穿上像同一个模子印出来的」——所以这次不再共用骨架，
每个职业从零搭一套完全不同的剪影：

- 剑修：瘦高道袍 + 高领 + 肩部剑翼 + 背后剑匣 + 长飘带 + 分片长袍
- 血修：宽胸战甲 + 尖肩 + 长披风 + 身前血幡 + 双角冠
- 魔修：巨型双角 + 肋骨胸甲 + 骷髅肩 + 骨刺/利爪 + 厚重靴
- 鬼修：无甲长袍 + 大兜帽 + 宽袖 + 曳地破纱 + 悬浮魂灯
- 体修：极宽巨肩 + 厚胸板 + 巨拳 + 重靴 + 面甲

保持 GeckoLib 8 骨契约，所有几何都在 armorHead/armorBody/armorLeft|RightArm/
ArmorLeft|RightLeg/Left|RightBoot 里。
"""
from __future__ import annotations

import math

from artlib import Geo

SLOTS = ["helmet", "chestplate", "leggings", "boots"]

PALETTES = {
    "jian": dict(metal=(205, 222, 238), steel=(245, 250, 255), cloth=(226, 240, 250),
                 jade=(140, 225, 240), rune=(120, 220, 250), glow=(150, 235, 255),
                 hem=(70, 160, 200), gold=(228, 200, 120)),
    "xue": dict(metal=(150, 80, 84), steel=(205, 150, 148), cloth=(88, 12, 18),
                jade=(240, 70, 70), rune=(255, 70, 70), glow=(255, 70, 70),
                hem=(120, 12, 20), gold=(220, 170, 80)),
    "mo": dict(metal=(62, 50, 74), steel=(130, 110, 150), cloth=(26, 18, 34),
               jade=(190, 110, 255), rune=(200, 90, 255), glow=(190, 90, 255),
               hem=(70, 30, 100), gold=(160, 140, 190)),
    "gui": dict(metal=(140, 160, 152), steel=(200, 220, 210), cloth=(30, 48, 44),
                jade=(110, 255, 200), rune=(120, 255, 205), glow=(110, 255, 200),
                hem=(40, 110, 95), gold=(160, 170, 130)),
    "ti": dict(metal=(96, 96, 104), steel=(170, 174, 182), cloth=(48, 42, 40),
               jade=(230, 180, 90), rune=(240, 200, 110), glow=(240, 200, 110),
               hem=(90, 70, 40), gold=(226, 186, 96)),
}

# 境界段：给材质/镶边加色（1-2 铁灰、3-4 铜、5-6 玉、7-8 血/紫、9-10 霜/金）
ACCENT = {
    0: (150, 185, 200), 1: (225, 170, 95), 2: (130, 235, 180),
    3: (240, 85, 110), 4: (190, 130, 255),
}


def blend(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def palette_for(career, realm):
    p = dict(PALETTES[career])
    band = 0 if realm <= 2 else 1 if realm <= 4 else 2 if realm <= 6 else 3 if realm <= 8 else 4
    accent = ACCENT[band]
    b = 0.9 + 0.015 * realm
    for k in ("metal", "steel", "cloth"):
        p[k] = tuple(min(255, int(v * b)) for v in p[k])
    p["jade"] = blend(p["jade"], accent, 0.45)
    p["rune"] = blend(p["rune"], accent, 0.55)
    p["glow"] = blend(p["glow"], accent, 0.5)
    p["hem"] = blend(p["hem"], accent, 0.4)
    p["gold"] = blend(p["gold"], accent, 0.25)
    return p


def add(b, x0, x1, y0, y1, z0, z1, mat="metal"):
    b.add((min(x0, x1), min(y0, y1), min(z0, z1)),
          (abs(x1 - x0), abs(y1 - y0), abs(z1 - z0)), mat=mat)


def rot(b, cx, cy, cz, sx, sy, sz, rz, mat="metal", rx=0.0, ry=0.0):
    b.add((cx - sx / 2, cy - sy / 2, cz - sz / 2), (sx, sy, sz), mat=mat,
          rotation=(rx, ry, rz), pivot=(cx, cy, cz))


def new_geo(name):
    g = Geo("geometry." + name, tex=(256, 256))
    head = g.bone("armorHead", (0, 24, 0))
    body = g.bone("armorBody", (0, 24, 0))
    ra = g.bone("armorRightArm", (-5, 22, 0))
    la = g.bone("armorLeftArm", (5, 22, 0))
    rl = g.bone("armorRightLeg", (-1.9, 12, 0))
    ll = g.bone("armorLeftLeg", (1.9, 12, 0))
    rb = g.bone("armorRightBoot", (-1.9, 12, 0))
    lb = g.bone("armorLeftBoot", (1.9, 12, 0))
    return g, head, body, ra, la, rl, ll, rb, lb


def arms(each):
    """对左右手调用 each(bone, sign)。"""
    sign = -1
    for _ in range(2):
        each(sign)
        sign = 1


# ---------------------------------------------------------------------------
# 剑修：瘦高道袍 + 剑翼肩 + 剑匣 + 飘带
# ---------------------------------------------------------------------------
def build_jian(realm, p):
    g, head, body, ra, la, rl, ll, rb, lb = new_geo("career_armor_jian_%d" % realm)
    # 躯干：窄而高的道袍
    add(body, -4.2, 4.2, 13.6, 23.0, -2.9, -2.35, "cloth")
    add(body, -4.2, 4.2, 13.6, 23.0, 2.35, 2.9, "cloth")
    add(body, -4.4, -3.9, 13.8, 22.6, -2.3, 2.3, "cloth")
    add(body, 3.9, 4.4, 13.8, 22.6, -2.3, 2.3, "cloth")
    # 高领
    for z in (-2.6, 2.6):
        add(body, -3.6, 3.6, 22.6, 25.0, z - 0.5, z + 0.5, "cloth")
    add(body, -3.9, 3.9, 24.6, 25.4, -2.7, 2.7, "metal")
    add(body, -0.9, 0.9, 22.8, 25.2, -3.0, -2.6, "jade")
    # 胸前玉牌 + 腰带
    add(body, -1.7, 1.7, 17.8, 20.8, -3.15, -2.85, "jade")
    add(body, -0.5, 0.5, 18.4, 20.2, -3.3, -3.1, "rune")
    add(body, -4.5, 4.5, 12.8, 14.0, -3.0, 3.0, "cloth")
    add(body, -4.6, 4.6, 13.2, 13.8, -3.05, 3.05, "gold")
    # 背后剑匣 + 剑柄
    add(body, -1.2, 1.2, 11.0, 22.0, 3.0, 4.4, "metal")
    add(body, -0.6, 0.6, 22.0, 24.2, 3.4, 4.0, "steel")
    add(body, -1.6, 1.6, 23.8, 24.6, 3.1, 4.3, "jade")
    # 背后长飘带
    for i, x in enumerate((-3.4, -1.4, 1.4, 3.4)):
        add(body, x - 0.6, x + 0.6, 2.6 + i * 0.4, 13.2, 3.1, 3.5, "cloth")
    # 剑翼肩（不是普通肩甲：向外展开的薄翼 + 羽尖）
    def wing(arm, sign):
        add(arm, 3.0 * sign, 9.6 * sign, 22.6, 24.0, -2.2, 2.2, "metal")
        add(arm, 3.4 * sign, 10.4 * sign, 21.4, 22.6, -1.8, 1.8, "cloth")
        for k in range(3):
            x = (9.0 + k * 0.9) * sign
            add(arm, min(x, x + 1.4 * sign), max(x, x + 1.4 * sign),
                24.0 - k * 0.9, 25.0 - k * 0.6, -0.7, 0.7, "jade")
        add(arm, 8.4 * sign, 9.4 * sign, 22.0, 24.0, -1.0, 1.0, "rune")
    arms(lambda sign: wing(ra if sign < 0 else la, sign))
    # 袖子：贴身
    arms(lambda sign: (add(ra if sign < 0 else la, 3.6 * sign, 8.2 * sign,
                           14.0, 21.6, -2.2, 2.2, "cloth"),
                       add(ra if sign < 0 else la, 3.4 * sign, 8.4 * sign,
                           12.4, 14.2, -2.3, 2.3, "steel")))
    # 腿部：分片长袍（前/后/外侧），到靴
    def robe(leg, sign):
        add(leg, 0.4 * sign, 3.5 * sign, 1.6, 13.0, -2.9, -2.35, "cloth")
        add(leg, 0.4 * sign, 3.5 * sign, 1.6, 13.0, 2.35, 2.9, "cloth")
        add(leg, 3.3 * sign, 4.1 * sign, 1.6, 12.6, -2.2, 2.2, "cloth")
        add(leg, 0.6 * sign, 3.3 * sign, 4.6, 5.6, -2.9, -2.4, "gold")
    arms(lambda sign: robe(rl if sign < 0 else ll, sign))
    def boot(b, sign):
        add(b, 0.4 * sign, 4.0 * sign, 0.0, 1.2, -3.0, 2.2, "steel")
        add(b, 0.5 * sign, 3.8 * sign, 1.2, 3.2, -2.4, 2.1, "cloth")
        add(b, 0.7 * sign, 3.4 * sign, 2.6, 3.8, -3.1, -2.5, "jade")
    arms(lambda sign: boot(rb if sign < 0 else lb, sign))
    # 头部：高冠 + 两侧翼 + 后飘带
    add(head, -4.4, 4.4, 30.4, 31.6, -4.4, 4.4, "cloth")
    add(head, -4.6, -4.0, 27.0, 31.6, -4.0, 4.0, "cloth")
    add(head, 4.0, 4.6, 27.0, 31.6, -4.0, 4.0, "cloth")
    add(head, -4.4, 4.4, 27.0, 30.6, 4.0, 4.6, "cloth")
    add(head, -3.6, 3.6, 31.4, 32.2, -3.6, 3.6, "metal")
    add(head, -0.7, 0.7, 32.2, 35.6, -4.3, -3.6, "jade")
    for s in (-1, 1):
        add(head, 4.0 * s, 5.6 * s, 31.4, 33.2, -0.8, 0.8, "metal")
        add(head, 5.2 * s, 5.9 * s, 32.4, 34.0, -0.5, 0.5, "rune")
    add(head, -0.9, 0.9, 32.0, 33.0, -4.7, -4.0, "rune")
    for i in range(3):
        add(head, -1.4 + i * 1.2, -0.8 + i * 1.2, 20.0, 27.0, 4.4, 4.8, "cloth")
    return g


# ---------------------------------------------------------------------------
# 血修：宽胸战甲 + 尖肩 + 披风 + 血幡
# ---------------------------------------------------------------------------
def build_xue(realm, p):
    g, head, body, ra, la, rl, ll, rb, lb = new_geo("career_armor_xue_%d" % realm)
    add(body, -4.5, 4.5, 13.2, 23.0, -2.9, -2.3, "cloth")
    add(body, -4.5, 4.5, 13.2, 23.0, 2.3, 2.9, "cloth")
    add(body, -3.9, 3.9, 16.6, 22.4, -3.35, -2.85, "metal")
    add(body, -1.5, 1.5, 17.4, 20.4, -3.5, -3.1, "rune")
    add(body, -4.8, 4.8, 12.6, 14.0, -3.2, 3.2, "metal")
    add(body, -1.2, 1.2, 12.8, 14.0, -3.4, -3.0, "jade")
    # 长披风（背后两片 + 下摆）
    for x0, x1 in ((-5.2, -0.6), (0.6, 5.2)):
        add(body, x0, x1, 2.4, 22.6, 2.9, 3.5, "cloth")
        add(body, x0 + 0.3, x1 - 0.3, 0.6, 2.6, 3.0, 3.6, "cloth")
    # 身前血幡（三条）
    for i, x in enumerate((-2.4, 0.0, 2.4)):
        add(body, x - 0.8, x + 0.8, 3.4 + i * 0.6, 13.0, -3.4, -3.0, "cloth")
        add(body, x - 0.5, x + 0.5, 2.4 + i * 0.6, 3.8 + i * 0.6, -3.5, -3.0, "rune")
    def pauldron(arm, sign):
        add(arm, 3.0 * sign, 9.2 * sign, 22.2, 24.6, -2.8, 2.8, "metal")
        add(arm, 2.8 * sign, 10.0 * sign, 20.8, 22.6, -2.6, 2.6, "metal")
        for k in range(2):
            base = (8.4 + k * 0.8) * sign
            add(arm, min(base, base + 2.6 * sign), max(base, base + 2.6 * sign),
                24.4 + k, 25.6 + k, -0.8, 0.8, "steel")
        add(arm, 7.8 * sign, 8.8 * sign, 22.2, 24.4, -1.0, 1.0, "rune")
    arms(lambda sign: pauldron(ra if sign < 0 else la, sign))
    def armplate(arm, sign):
        add(arm, 3.5 * sign, 8.3 * sign, 14.0, 20.6, -2.5, 2.5, "metal")
        add(arm, 3.3 * sign, 8.5 * sign, 12.2, 14.2, -2.6, 2.6, "steel")
        add(arm, 7.9 * sign, 8.6 * sign, 14.2, 20.2, -0.8, 0.8, "rune")
    arms(lambda sign: armplate(ra if sign < 0 else la, sign))
    def legplate(leg, sign):
        add(leg, 0.5 * sign, 3.8 * sign, 5.0, 12.8, -3.0, -2.4, "metal")
        add(leg, 3.5 * sign, 4.3 * sign, 4.0, 12.4, -2.2, 2.2, "metal")
        add(leg, 0.7 * sign, 3.6 * sign, 3.6, 4.6, -3.1, -2.5, "gold")
    arms(lambda sign: legplate(rl if sign < 0 else ll, sign))
    def boot(b, sign):
        add(b, 0.3 * sign, 4.3 * sign, 0.0, 1.3, -3.4, 2.5, "metal")
        add(b, 0.4 * sign, 4.1 * sign, 1.3, 3.6, -2.6, 2.4, "metal")
        add(b, 0.6 * sign, 3.8 * sign, 3.2, 4.2, -3.2, -2.6, "rune")
    arms(lambda sign: boot(rb if sign < 0 else lb, sign))
    # 头：双角冠 + 面罩 + 后红翎
    add(head, -4.5, 4.5, 29.4, 31.4, -4.5, 4.5, "metal")
    add(head, -4.7, -4.0, 27.0, 31.4, -4.2, 4.2, "metal")
    add(head, 4.0, 4.7, 27.0, 31.4, -4.2, 4.2, "metal")
    add(head, -4.5, 4.5, 27.0, 30.4, 4.2, 4.9, "metal")
    for s in (-1, 1):
        add(head, 3.6 * s, 4.6 * s, 31.6, 34.8, -1.1, 1.1, "steel")
        add(head, 4.0 * s, 4.9 * s, 34.8, 37.0, -0.7, 0.7, "rune")
    add(head, -1.3, 1.3, 30.2, 31.8, -4.9, -4.2, "rune")
    for i in range(4):
        add(head, -1.6 + i * 0.9, -0.9 + i * 0.9, 24.0, 30.0, 4.7, 5.2, "cloth")
    return g


# ---------------------------------------------------------------------------
# 魔修：巨型双角 + 肋骨胸甲 + 骷髅肩 + 骨刺利爪
# ---------------------------------------------------------------------------
def build_mo(realm, p):
    g, head, body, ra, la, rl, ll, rb, lb = new_geo("career_armor_mo_%d" % realm)
    add(body, -4.6, 4.6, 13.0, 23.0, -2.9, -2.3, "cloth")
    add(body, -4.6, 4.6, 13.0, 23.0, 2.3, 2.9, "cloth")
    add(body, -5.0, 5.0, 21.0, 23.2, -3.4, 3.4, "metal")
    # 肋骨（每侧 4 根弧形）
    for side in (-1, 1):
        for i in range(4):
            y = 15.0 + i * 1.9
            x0 = 0.9 * side
            x1 = 3.9 * side
            add(body, x0, x1, y, y + 1.0, -3.55, -3.05, "steel")
            add(body, x1 - 0.4 * side, x1, y - 0.5, y + 1.2, -3.4, -2.9, "steel")
    add(body, -0.9, 0.9, 15.0, 22.2, -3.6, -3.1, "steel")
    add(body, -1.5, 1.5, 18.6, 21.0, -3.75, -3.15, "rune")
    add(body, -5.2, 5.2, 12.6, 14.2, -3.3, 3.3, "metal")
    # 背脊骨刺
    for i in range(5):
        add(body, -1.1, 1.1, 8.0 + i * 2.4, 9.6 + i * 2.4, 2.9, 3.6, "steel")
        add(body, -0.5, 0.5, 8.4 + i * 2.4, 9.4 + i * 2.4, 3.5, 4.1, "metal")
    # 肩：巨型骨甲 + 上翘骨角
    def pauldron(arm, sign):
        add(arm, 2.8 * sign, 10.0 * sign, 21.4, 25.2, -3.2, 3.2, "metal")
        add(arm, 2.6 * sign, 10.8 * sign, 19.4, 21.8, -3.0, 3.0, "steel")
        add(arm, 2.4 * sign, 11.4 * sign, 17.4, 19.6, -2.8, 2.8, "metal")
        for k in range(2):
            rot(arm, (9.2 + k * 1.2) * sign, 25.6 + k * 1.4, 0.0,
                1.3, 3.6, 1.3, 0, "steel")
        add(arm, 4.4 * sign - 1.0, 4.4 * sign + 1.0, 25.0, 26.6, -1.6, 1.6, "steel")
        add(arm, 6.6 * sign - 0.7, 6.6 * sign + 0.7, 25.2, 26.2, -2.4, -1.6, "rune")
    arms(lambda sign: pauldron(ra if sign < 0 else la, sign))
    def armplate(arm, sign):
        add(arm, 3.4 * sign, 8.6 * sign, 14.0, 20.4, -2.8, 2.8, "metal")
        add(arm, 3.2 * sign, 8.8 * sign, 12.0, 14.2, -3.0, 3.0, "steel")
        for k in range(3):
            add(arm, (3.6 + k * 1.4) * sign, (4.2 + k * 1.4) * sign, 10.6, 12.2,
                -2.4 + k * 1.6, -1.8 + k * 1.6, "steel")
    arms(lambda sign: armplate(ra if sign < 0 else la, sign))
    def legplate(leg, sign):
        add(leg, 0.4 * sign, 3.9 * sign, 4.6, 12.6, -3.1, -2.4, "metal")
        add(leg, 3.5 * sign, 4.4 * sign, 4.0, 12.2, -2.2, 2.2, "steel")
        for i in range(2):
            add(leg, 0.7 * sign, 3.6 * sign, 3.4 + i * 1.6, 4.2 + i * 1.6,
                -3.2, -2.6, "steel")
    arms(lambda sign: legplate(rl if sign < 0 else ll, sign))
    def boot(b, sign):
        add(b, 0.2 * sign, 4.5 * sign, 0.0, 1.4, -3.6, 2.6, "metal")
        add(b, 0.3 * sign, 4.3 * sign, 1.4, 3.8, -2.8, 2.5, "metal")
        for k in range(3):
            add(b, (0.8 + k * 1.1) * sign, (1.4 + k * 1.1) * sign, -0.6, 0.6,
                -3.9, -3.2, "steel")
    arms(lambda sign: boot(rb if sign < 0 else lb, sign))
    # 头：双巨角 + 骷髅面甲
    add(head, -4.6, 4.6, 28.4, 31.6, -4.5, 4.5, "metal")
    add(head, -3.8, 3.8, 26.6, 28.6, -4.6, -4.0, "steel")
    add(head, -1.4, 1.4, 27.2, 28.6, -4.9, -4.4, "rune")
    for s in (-1, 1):
        # 三段弯曲巨角
        add(head, 3.6 * s, 5.2 * s, 31.4, 34.4, -1.6, 1.6, "steel")
        add(head, 4.6 * s, 6.2 * s, 34.2, 36.8, -1.2, 1.2, "steel")
        add(head, 5.4 * s, 6.8 * s, 36.6, 39.0, -0.8, 0.8, "rune")
    add(head, -2.2, 2.2, 30.4, 31.2, -4.9, -4.2, "rune")
    return g


# ---------------------------------------------------------------------------
# 鬼修：无甲长袍 + 大兜帽 + 宽袖 + 曳地破纱 + 魂灯
# ---------------------------------------------------------------------------
def build_gui(realm, p):
    g, head, body, ra, la, rl, ll, rb, lb = new_geo("career_armor_gui_%d" % realm)
    add(body, -4.6, 4.6, 13.0, 23.0, -2.9, -2.3, "cloth")
    add(body, -4.6, 4.6, 13.0, 23.0, 2.3, 2.9, "cloth")
    add(body, -4.8, -4.3, 12.4, 22.8, -2.2, 2.2, "cloth")
    add(body, 4.3, 4.8, 12.4, 22.8, -2.2, 2.2, "cloth")
    add(body, -4.6, 4.6, 22.6, 24.0, -2.9, 2.9, "cloth")
    add(body, -1.1, 1.1, 6.4, 13.4, -3.1, -2.7, "cloth")  # 前襟
    # 符纸
    for i, x in enumerate((-2.6, -0.8, 1.0)):
        add(body, x, x + 0.9, 14.2, 17.8, -3.2, -3.0, "rune")
    add(body, -2.2, -1.2, 13.6, 17.4, 3.0, 3.2, "cloth")
    add(body, 1.2, 2.2, 13.6, 17.4, 3.0, 3.2, "cloth")
    # 曳地破纱：前/后/侧多片、长短不一
    for k in range(4):
        x0 = -4.4 + k * 2.4
        add(body, x0, x0 + 1.8, 1.0 + (k % 2) * 1.2, 13.0, 3.0, 3.4, "cloth")
        add(body, x0 + 0.3, x0 + 1.5, 0.2 + (k % 2) * 1.0, 12.6, -3.4, -3.0, "cloth")
    # 肩：无甲，只有布披 + 悬浮魂灯（放在手臂骨上，位置在肩外）
    def ghost(arm, sign):
        add(arm, 3.4 * sign, 8.8 * sign, 21.6, 23.4, -2.6, 2.6, "cloth")
        # 悬浮魂灯
        add(arm, 7.4 * sign - 1.0, 7.4 * sign + 1.0, 25.6, 27.8, -1.0, 1.0, "metal")
        add(arm, 7.4 * sign - 0.7, 7.4 * sign + 0.7, 26.0, 27.4, -1.3, 1.3, "rune")
        add(arm, 7.4 * sign - 0.4, 7.4 * sign + 0.4, 27.6, 28.4, -0.4, 0.4, "steel")
        add(arm, 7.4 * sign - 0.3, 7.4 * sign + 0.3, 23.4, 25.8, -0.3, 0.3, "steel")
    arms(lambda sign: ghost(ra if sign < 0 else la, sign))
    # 宽大长袖（垂到膝盖）
    def sleeve(arm, sign):
        add(arm, 3.3 * sign, 9.6 * sign, 13.2, 21.4, -3.0, 3.0, "cloth")
        add(arm, 3.1 * sign, 10.0 * sign, 9.4, 13.4, -3.2, 3.2, "cloth")
        for k in range(3):
            add(arm, (3.6 + k * 2.0) * sign, (4.4 + k * 2.0) * sign, 8.0, 10.2,
                -2.6, 2.6, "cloth")
    arms(lambda sign: sleeve(ra if sign < 0 else la, sign))
    def legrobe(leg, sign):
        for k in range(4):
            x0 = (0.3 + k * 0.9) * sign
            add(leg, min(x0, x0 + 0.8 * sign), max(x0, x0 + 0.8 * sign),
                0.4 + (k % 2) * 0.8, 12.6, -2.9, -2.4, "cloth")
            add(leg, min(x0, x0 + 0.8 * sign), max(x0, x0 + 0.8 * sign),
                0.4 + (k % 2) * 0.8, 12.6, 2.4, 2.9, "cloth")
        add(leg, 3.4 * sign, 4.2 * sign, 0.6, 12.4, -2.1, 2.1, "cloth")
    arms(lambda sign: legrobe(rl if sign < 0 else ll, sign))
    def boot(b, sign):
        add(b, 0.4 * sign, 4.0 * sign, 0.0, 0.9, -3.0, 2.2, "cloth")
        add(b, 0.5 * sign, 3.9 * sign, 0.9, 2.6, -2.5, 2.1, "cloth")
    arms(lambda sign: boot(rb if sign < 0 else lb, sign))
    # 头：大兜帽 + 面纱 + 后长纱
    add(head, -5.0, 5.0, 29.0, 33.6, -5.0, 5.0, "cloth")
    add(head, -5.2, -4.4, 25.4, 33.6, -4.8, 4.8, "cloth")
    add(head, 4.4, 5.2, 25.4, 33.6, -4.8, 4.8, "cloth")
    add(head, -5.0, 5.0, 25.4, 29.4, 4.4, 5.2, "cloth")
    add(head, -2.8, 2.8, 24.6, 26.2, -5.1, -4.5, "cloth")
    add(head, -0.9, 0.9, 28.6, 30.4, -5.2, -4.7, "rune")
    for i in range(4):
        add(head, -2.4 + i * 1.6, -1.6 + i * 1.6, 14.0, 26.0, 5.0, 5.5, "cloth")
    return g


# ---------------------------------------------------------------------------
# 体修：极宽巨肩 + 厚胸板 + 巨拳 + 重靴
# ---------------------------------------------------------------------------
def build_ti(realm, p):
    g, head, body, ra, la, rl, ll, rb, lb = new_geo("career_armor_ti_%d" % realm)
    # 厚胸板
    add(body, -6.2, 6.2, 14.0, 23.2, -3.6, -2.9, "metal")
    add(body, -6.2, 6.2, 14.0, 23.2, 2.9, 3.6, "metal")
    add(body, -6.6, -5.9, 13.6, 23.0, -3.0, 3.0, "metal")
    add(body, 5.9, 6.6, 13.6, 23.0, -3.0, 3.0, "metal")
    add(body, -1.3, 1.3, 14.4, 22.8, -3.9, -3.2, "steel")
    add(body, -5.8, 5.8, 12.0, 14.2, -3.7, 3.7, "steel")
    add(body, -1.5, 1.5, 17.4, 19.8, -4.0, -3.4, "rune")
    add(body, -4.4, 4.4, 22.6, 24.0, -3.4, 3.4, "metal")
    # 腰甲片
    for k in range(3):
        add(body, -4.8 + k * 3.2, -2.4 + k * 3.2, 10.8, 12.2, -3.8, -3.2, "metal")
    # 巨肩（三层、外扩）
    def pauldron(arm, sign):
        add(arm, 2.6 * sign, 11.0 * sign, 22.0, 25.4, -3.4, 3.4, "metal")
        add(arm, 2.4 * sign, 12.2 * sign, 19.6, 22.4, -3.2, 3.2, "steel")
        add(arm, 2.2 * sign, 12.8 * sign, 17.2, 19.8, -3.0, 3.0, "metal")
        add(arm, 11.6 * sign, 13.0 * sign, 17.6, 24.6, -2.6, 2.6, "steel")
        for k in range(3):
            rot(arm, (10.6 + k * 0.8) * sign, 25.6 + k * 0.9, 0.0,
                1.2, 2.8, 1.2, 0, "steel")
        add(arm, 4.6 * sign - 1.2, 4.6 * sign + 1.2, 25.2, 26.8, -2.6, 2.6, "rune")
    arms(lambda sign: pauldron(ra if sign < 0 else la, sign))
    # 巨拳 + 粗臂
    def armplate(arm, sign):
        add(arm, 3.0 * sign, 10.0 * sign, 12.6, 20.4, -3.2, 3.2, "metal")
        add(arm, 2.8 * sign, 10.4 * sign, 9.8, 12.8, -3.4, 3.4, "steel")
        for k in range(3):
            add(arm, (3.4 + k * 2.2) * sign, (5.0 + k * 2.2) * sign, 9.2, 10.6,
                -2.6, 2.6, "metal")
    arms(lambda sign: armplate(ra if sign < 0 else la, sign))
    # 粗腿 + 膝甲
    def legplate(leg, sign):
        add(leg, 0.2 * sign, 4.4 * sign, 4.6, 12.8, -3.3, -2.6, "metal")
        add(leg, 3.8 * sign, 4.8 * sign, 4.0, 12.6, -2.4, 2.4, "steel")
        add(leg, 0.4 * sign, 4.2 * sign, 2.0, 4.8, -3.4, -2.7, "metal")
    arms(lambda sign: legplate(rl if sign < 0 else ll, sign))
    # 巨型重靴
    def boot(b, sign):
        add(b, 0.0 * sign, 4.8 * sign, 0.0, 1.6, -3.8, 2.8, "metal")
        add(b, 0.1 * sign, 4.6 * sign, 1.6, 4.2, -3.0, 2.6, "metal")
        add(b, 0.3 * sign, 4.4 * sign, 4.0, 5.0, -3.2, 2.4, "steel")
    arms(lambda sign: boot(rb if sign < 0 else lb, sign))
    # 头：小面甲 + 顶冠
    add(head, -4.8, 4.8, 28.0, 31.6, -4.8, 4.8, "metal")
    add(head, -4.2, 4.2, 26.4, 28.4, -4.8, -4.0, "metal")
    add(head, -1.4, 1.4, 26.8, 28.2, -5.1, -4.6, "rune")
    add(head, -0.9, 0.9, 31.4, 34.6, -4.6, -4.0, "steel")
    add(head, -2.6, 2.6, 31.6, 32.4, -4.7, -4.1, "gold")
    return g




def realm_trim(geo, career, realm):
    """同一职业的不同境界也加境界专属件，避免同职业 10 套只是换色。"""
    body = next((b for b in geo.bones if b.name == "armorBody"), None)
    head = next((b for b in geo.bones if b.name == "armorHead"), None)
    arms = [b for b in geo.bones if b.name in ("armorRightArm", "armorLeftArm")]
    legs = [b for b in geo.bones if b.name in ("armorRightLeg", "armorLeftLeg")]
    if body is not None:
        for k in range(1 + realm // 4):
            y = 15.4 + k * 2.0
            add(body, -0.8, 0.8, y, y + 1.2, -3.75, -3.35, "jade")
        if realm >= 3:
            for s in (-1, 1):
                add(body, 5.4 * s - 0.6, 5.4 * s + 0.6, 18.0, 22.0,
                    -1.0, 1.0, "steel")
        if realm >= 6:
            add(body, -1.2, 1.2, 12.6, 13.4, 3.2, 3.8, "jade")
        if realm >= 8:
            for s in (-1, 1):
                add(body, 5.8 * s - 0.7, 5.8 * s + 0.7, 24.0, 25.4,
                    -0.7, 0.7, "rune")
    if head is not None:
        add(head, -0.7, 0.7, 28.8, 29.8, -4.9, -4.4, "jade")
        if realm >= 4:
            for s in (-1, 1):
                add(head, 4.4 * s, 5.4 * s, 30.2, 31.4, -0.7, 0.7, "steel")
        if realm >= 7:
            add(head, -1.0, 1.0, 31.6, 33.0, -4.6, -4.1, "rune")
        if realm >= 9:
            for k in range(6):
                a = math.pi * (k + 1) / 7.0
                add(head, math.cos(a) * 4.8 - 0.3, math.cos(a) * 4.8 + 0.3,
                    31.0 + math.sin(a) * 2.0, 31.6 + math.sin(a) * 2.0,
                    4.6, 5.1, "jade")
    for arm in arms:
        sign = 1 if arm.name.endswith("LeftArm") else -1
        if realm >= 5:
            add(arm, 3.6 * sign, 8.4 * sign, 12.6, 13.4, -2.6, 2.6, "steel")
        if realm >= 8:
            add(arm, 7.8 * sign - 0.5, 7.8 * sign + 0.5, 24.0, 25.2,
                -1.0, 1.0, "rune")
    for leg in legs:
        sign = 1 if leg.name.endswith("LeftLeg") else -1
        if realm >= 2:
            add(leg, 0.6 * sign, 3.8 * sign, 8.0, 9.0, -3.3, -2.7, "steel")
        if realm >= 6:
            add(leg, 0.6 * sign, 3.6 * sign, 4.0, 4.8, -3.3, -2.8, "jade")


BUILDERS = {"jian": build_jian, "xue": build_xue, "mo": build_mo,
            "gui": build_gui, "ti": build_ti}


def rear_leg_cover(geo, career):
    """腿甲后侧包裹：之前只做了前面/侧面，第三人称从背后看是空的。"""
    mat = "cloth" if career in ("jian", "gui") else "metal"
    boot_mat = "steel" if career not in ("jian", "gui") else "cloth"
    for b in geo.bones:
        if b.name in ("armorRightLeg", "armorLeftLeg"):
            sign = 1 if b.name.endswith("LeftLeg") else -1
            add(b, 0.4 * sign, 3.7 * sign, 3.8, 12.4, 2.15, 2.8, mat)
            add(b, 3.4 * sign, 4.2 * sign, 4.2, 12.2, 1.8, 2.7, mat)
            if career not in ("jian", "gui"):
                add(b, 0.6 * sign, 3.5 * sign, 6.6, 7.4, 2.6, 2.95, "steel")
        if b.name in ("armorRightBoot", "armorLeftBoot"):
            sign = 1 if b.name.endswith("LeftBoot") else -1
            add(b, 0.5 * sign, 4.0 * sign, 0.2, 3.6, 2.05, 2.7, boot_mat)


def add_dynamic_bones(geo, career, realm):
    """动态件：呼吸灯 fx、披风 cape、纱幔 veil、魂灯 lantern_l/r（单独骨，动画用）。"""
    body = next((b for b in geo.bones if b.name == "armorBody"), None)
    head = next((b for b in geo.bones if b.name == "armorHead"), None)

    fx = geo.bone("fx", (0, 18.0, 0), parent="armorBody")
    for k in range(3 + realm // 4):
        import math as _m
        a = 2 * _m.pi * k / max(1, 3 + realm // 4) + realm * 0.4
        r = 4.6 + 0.4 * (realm % 3)
        y = 16.0 + realm * 0.5 + k * 1.1
        fx.add((_m.cos(a) * r - 0.45, y, _m.sin(a) * r - 0.45),
               (0.9, 0.9, 0.9), mat="rune")

    if career in ("xue", "mo", "ti"):
        cape = geo.bone("cape", (0, 23.0, 3.0), parent="armorBody")
        w = 4.6 if career != "ti" else 5.4
        # 只保留披肩上段（不过腰），避免下摆被误看成"不会动的腿甲"
        for i in range(4):
            x0 = -w + i * (2 * w / 4)
            cape.add((x0, 18.6, 3.1), (2 * w / 4 - 0.2, 4.4, 0.55), mat="cloth")

    if career in ("gui",):
        veil = geo.bone("veil", (0, 25.0, 3.4), parent="armorHead")
        for i in range(4):
            x0 = -2.8 + i * 1.6
            length = 9.0 + (i % 2) * 3.0
            veil.add((x0, 25.0 - length, 4.2), (1.3, length, 0.4), mat="cloth")
        for side, name in ((-1, "lantern_l"), (1, "lantern_r")):
            lan = geo.bone(name, (side * 6.2, 26.0, 0.0), parent="armorBody")
            lan.add((-0.9, -1.4, -0.9), (1.8, 2.8, 1.8), mat="metal")
            lan.add((-0.6, -0.9, -1.15), (1.2, 1.8, 2.3), mat="rune")
            lan.add((-0.4, 1.4, -0.4), (0.8, 0.8, 0.8), mat="steel")


def detail_3d(geo, career):
    """3D 层次强化：给每套甲加前后凸起甲片、侧甲、铆钉、盔沿、膝甲、靴头，
    避免看起来像一层平面贴图。"""
    body = next((b for b in geo.bones if b.name == "armorBody"), None)
    head = next((b for b in geo.bones if b.name == "armorHead"), None)
    arms = [b for b in geo.bones if b.name in ("armorRightArm", "armorLeftArm")]
    legs = [b for b in geo.bones if b.name in ("armorRightLeg", "armorLeftLeg")]
    boots = [b for b in geo.bones if b.name in ("armorRightBoot", "armorLeftBoot")]

    soft = career in ("jian", "gui")
    plate = "cloth" if soft else "metal"
    rim = "gold" if career in ("jian", "xue", "ti") else "jade"

    if body is not None:
        add(body, -3.9, 3.9, 15.2, 21.8, -4.0, -3.5, plate)
        add(body, -4.4, 4.4, 14.2, 15.0, -4.1, 3.2, "steel" if not soft else "cloth")
        add(body, -3.6, 3.6, 18.6, 19.4, -4.2, -3.9, rim)
        add(body, -5.6, -4.8, 15.0, 22.4, -2.4, 2.4, plate)
        add(body, 4.8, 5.6, 15.0, 22.4, -2.4, 2.4, plate)
        add(body, -3.2, 3.2, 15.0, 22.0, 3.0, 3.6, plate)
        for i in range(4):
            x = -3.4 + i * 2.2
            add(body, x - 0.25, x + 0.25, 20.6, 21.2, -4.25, -4.0, rim)
            add(body, x - 0.25, x + 0.25, 14.0, 14.6, -4.25, -4.0, rim)
        add(body, -1.0, 1.0, 11.8, 13.2, -4.2, -3.6, "gold" if career in ("jian", "xue") else "jade")
    if head is not None:
        add(head, -4.6, 4.6, 29.0, 29.8, -5.2, 5.2, plate)
        add(head, -4.8, 4.8, 27.6, 29.0, -5.4, 5.4, "steel" if not soft else "cloth")
        add(head, -5.2, 5.2, 30.4, 31.2, -4.4, 4.4, rim)
        for side in (-1, 1):
            add(head, side * 4.3, side * 5.4, 26.0, 30.2, -2.2, 2.2, plate)
    for arm in arms:
        sign = 1 if arm.name.endswith("LeftArm") else -1
        add(arm, 6.8 * sign, 9.6 * sign, 21.4, 24.8, -2.8, 2.8, "steel" if not soft else "cloth")
        add(arm, 3.4 * sign, 8.8 * sign, 14.6, 15.4, -3.0, 3.0, rim)
        add(arm, 7.0 * sign, 7.8 * sign, 12.8, 14.8, -1.8, 1.8, "jade")
    for leg in legs:
        sign = 1 if leg.name.endswith("LeftLeg") else -1
        add(leg, 0.4 * sign, 3.8 * sign, 4.4, 11.8, -3.9, -3.35, plate)
        add(leg, 3.6 * sign, 4.6 * sign, 4.0, 11.6, -2.4, 2.4, plate)
        add(leg, 0.6 * sign, 3.6 * sign, 7.8, 8.8, -4.1, -3.7, "steel" if not soft else "cloth")
        add(leg, 0.5 * sign, 3.7 * sign, 5.0, 11.6, 3.0, 3.5, plate)
    for b in boots:
        sign = 1 if b.name.endswith("LeftBoot") else -1
        add(b, 0.3 * sign, 4.2 * sign, 0.0, 1.6, -3.8, 2.6, "steel" if not soft else "cloth")
        add(b, 0.6 * sign, 3.9 * sign, 2.0, 3.2, -3.8, -3.2, rim)


def build(career, realm):
    p = palette_for(career, realm)
    geo = BUILDERS[career](realm, p)
    rear_leg_cover(geo, career)
    detail_3d(geo, career)
    add_dynamic_bones(geo, career, realm)
    realm_trim(geo, career, realm)
    return geo, {
        "pal": {"metal": p["metal"], "steel": p["steel"], "cloth": p["cloth"],
                "jade": p["jade"], "rune": p["rune"]},
        "glow": p["glow"], "hem": p["hem"], "gold": p["gold"],
        "grip": None, "tassel": None,
    }
