# -*- coding: utf-8 -*-
"""
职业甲 200 件 = 50 套独立外观（5 职业 × 10 境界，每套 4 件）
=============================================================
玩家要求：盔甲外观也要每件独立、不同职业一眼能区分。

做法：
- 以 15 套（5 职业 × 初/进/名器）为"原型"，按境界追加独立饰件并调色；
- 每个职业再加独立的"形制签名"（剑修高领云肩/血修披风血幡/魔修双角肋骨/鬼修兜帽破纱/体修巨肩重甲）；
- 输出 50 套 geo/贴图/glowmask/4 图标，200 个物品模型与 50 个动画。
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
import build_armor_v2 as ba
import build_w01r as w01r
import armor_careers as ac
from artlib import build_mesh, contact_sheet, render_views

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(HERE, "..", "w01r", "armor_unique"))
WS = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
os.makedirs(OUT, exist_ok=True)

CAREERS = ["jian", "xue", "mo", "gui", "ti"]
SLOTS = ["helmet", "chestplate", "leggings", "boots"]
REALM_CN = ["练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"]
ACCENT = {
    0: (150, 190, 205), 1: (220, 165, 95), 2: (130, 230, 175),
    3: (235, 86, 110), 4: (185, 125, 255),
}



ARCHETYPES = {
    "light": dict(head="headband", torso="robe", skirt="cloth",
                  shoulder="slim", arm="sleeve", boot="cloth", extras=[]),
    "medium": dict(head="horncirclet", torso="warrobe", skirt="cloth",
                    shoulder="spike", arm="plate", boot="plate", extras=[]),
    "heavy": dict(head="visor", torso="heavy", skirt="plate",
                   shoulder="huge", arm="plate", boot="heavy", extras=[]),
    "robe": dict(head="daocrown", torso="robe", skirt="longrobe",
                  shoulder="slim", arm="wide", boot="cloth", extras=[]),
    "gauze": dict(head="hood", torso="robe", skirt="tatter",
                   shoulder="slim", arm="wide", boot="cloth",
                   extras=["veil", "wisps", "chain"]),
    "bone": dict(head="skullcrown", torso="ribs", skirt="bone",
                  shoulder="skull", arm="plate", boot="plate", extras=[]),
    "gold": dict(head="halo", torso="gold", skirt="plate",
                  shoulder="huge", arm="plate", boot="heavy", extras=["halo_ring"]),
    "plate": dict(head="horned", torso="plate", skirt="plate",
                   shoulder="spike", arm="plate", boot="heavy", extras=[]),
}

TYPE_CN = {"light": "轻甲", "medium": "中甲", "heavy": "重甲", "robe": "道袍",
           "gauze": "纱袍", "bone": "骨甲", "gold": "金甲", "plate": "板甲"}

# 每个职业 10 个境界的甲型（同境界尽量五种不同，且低→高有成长感）
TYPE_MAP = {
    "jian": ["light", "light", "light", "light", "robe", "robe",
             "gold", "gold", "gold", "gold"],
    "xue": ["medium", "medium", "medium", "medium", "heavy", "heavy",
            "heavy", "heavy", "heavy", "heavy"],
    "mo": ["bone"] * 10,
    "gui": ["robe", "robe", "gauze", "gauze", "gauze", "gauze",
            "gauze", "gauze", "gauze", "gauze"],
    "ti": ["heavy", "heavy", "heavy", "heavy", "medium", "medium",
           "plate", "plate", "plate", "plate"],
}

def band_of(realm):
    return 1 if realm <= 3 else (2 if realm <= 6 else 3)


def blend(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def realm_color(color, realm):
    band = 0 if realm <= 2 else 1 if realm <= 4 else 2 if realm <= 6 else 3 if realm <= 8 else 4
    accent = ACCENT[band]
    b = 0.86 + 0.02 * realm
    pal = dict(color["pal"])
    for k in ("metal", "steel"):
        pal[k] = blend(tuple(min(255, int(v * b)) for v in pal[k]), accent, 0.18)
    pal["cloth"] = tuple(min(255, int(v * b)) for v in pal["cloth"])
    pal["jade"] = blend(pal["jade"], accent, 0.55)
    pal["rune"] = blend(pal["rune"], accent, 0.75)
    return dict(pal=pal, glow=blend(color["glow"], accent, 0.7),
                hem=blend(color["hem"], accent, 0.5),
                gold=blend(color["gold"], accent, 0.35),
                grip=color.get("grip"), tassel=color.get("tassel"))


def bone(geo, name):
    for b in geo.bones:
        if b.name == name:
            return b
    return None


def add(b, x0, x1, y0, y1, z0, z1, mat="metal"):
    b.add((min(x0, x1), min(y0, y1), min(z0, z1)),
          (abs(x1 - x0), abs(y1 - y0), abs(z1 - z0)), mat=mat)


def embellish(geo, realm):
    """按境界加独立饰件（每境件数与位置不同）。"""
    r = realm
    body = bone(geo, "armorBody")
    head = bone(geo, "armorHead")
    legs = [bone(geo, "armorRightLeg"), bone(geo, "armorLeftLeg")]
    arms = [bone(geo, "armorRightArm"), bone(geo, "armorLeftArm")]
    boots = [bone(geo, "armorRightBoot"), bone(geo, "armorLeftBoot")]

    if body is not None:
        add(body, -0.9, 0.9, 17.6, 20.6, -3.55, -3.2, "rune")
        if r >= 3:
            add(body, -3.4, 3.4, 21.8, 22.6, -3.5, -3.2, "jade")
        if r >= 5:
            for s in (-1, 1):
                add(body, 4.4 * s - 0.7, 4.4 * s + 0.7, 23.0, 25.4, -0.9, 0.9, "steel")
        if r >= 7:
            add(body, -1.4, 1.4, 12.2, 13.2, -3.45, 3.0, "steel")
            for s in (-1, 1):
                add(body, 5.2 * s - 0.7, 5.2 * s + 0.7, 25.4, 27.0, -0.7, 0.7, "jade")
        if r >= 9:
            for k in range(4):
                add(body, -4.2 + k * 2.8, -3.6 + k * 2.8, 13.6, 14.4, 3.0, 3.4, "rune")
            add(body, -5.6, 5.6, 3.2, 3.8, 3.0, 3.6, "steel")
    if head is not None:
        if r >= 2:
            add(head, -0.8, 0.8, 30.2, 31.6, -4.9, -4.4, "jade")
        if r >= 4:
            for s in (-1, 1):
                add(head, 4.2 * s, 5.6 * s, 29.6, 31.6, -0.9, 0.9, "steel")
        if r >= 6:
            add(head, -0.7, 0.7, 32.4, 34.6, -4.6, -4.0, "rune")
            for s in (-1, 1):
                add(head, 4.6 * s, 5.6 * s, 31.6, 33.4, -0.6, 0.6, "jade")
        if r >= 8:
            for k in range(8):
                a = math.pi * (k + 1) / 9.0
                add(head, math.cos(a) * 5.2 - 0.35, math.cos(a) * 5.2 + 0.35,
                    31.0 + math.sin(a) * 2.4, 31.7 + math.sin(a) * 2.4,
                    4.6, 5.2, "jade")
        if r >= 10:
            for s in (-1, 1):
                add(head, 4.9 * s, 5.5 * s, 33.0, 36.2, -0.4, 0.4, "steel")
    for arm in arms:
        if arm is None:
            continue
        sign = 1 if arm.name.endswith("LeftArm") else -1
        if r >= 3:
            add(arm, 3.2 * sign, 9.0 * sign, 24.4, 25.6, -1.4, 1.4, "steel")
        if r >= 6:
            add(arm, 7.4 * sign, 8.6 * sign, 25.6, 26.8, -0.9, 0.9, "jade")
            add(arm, 3.6 * sign, 8.6 * sign, 13.0, 14.2, -2.9, 2.9, "jade")
        if r >= 9:
            add(arm, 3.4 * sign, 8.8 * sign, 20.6, 21.4, -3.1, 3.1, "rune")
    for leg in legs:
        if leg is None:
            continue
        sign = 1 if leg.name.endswith("LeftLeg") else -1
        if r >= 4:
            add(leg, 0.6 * sign, 3.8 * sign, 6.2, 7.4, -3.1, -2.5, "metal")
        if r >= 7:
            for k in range(2):
                add(leg, 0.7 * sign, 3.7 * sign, 3.2 + k * 1.6, 3.9 + k * 1.6,
                    -3.0, -2.55, "rune")
        if r >= 9:
            add(leg, 3.6 * sign, 4.3 * sign, 2.0, 9.8, -2.2, 2.2, "steel")
    for b in boots:
        if b is None:
            continue
        sign = 1 if b.name.endswith("LeftBoot") else -1
        if r >= 5:
            add(b, 0.4 * sign, 4.1 * sign, 0.0, 1.2, -3.3, -2.8, "steel")
        if r >= 8:
            add(b, 0.5 * sign, 3.9 * sign, 4.4, 5.0, -2.6, 2.6, "jade")


def career_style(geo, career, realm):
    """职业专属形制：让五个职业一眼能区分（不只是换色）。"""
    body = bone(geo, "armorBody")
    head = bone(geo, "armorHead")
    arms = [bone(geo, "armorRightArm"), bone(geo, "armorLeftArm")]
    legs = [bone(geo, "armorRightLeg"), bone(geo, "armorLeftLeg")]
    boots = [bone(geo, "armorRightBoot"), bone(geo, "armorLeftBoot")]

    if career == "jian":
        if body is not None:
            add(body, -4.4, 4.4, 22.6, 23.6, -3.4, 3.4, "cloth")
            add(body, -4.8, -3.7, 21.6, 23.6, -1.0, 1.0, "metal")
            add(body, 3.7, 4.8, 21.6, 23.6, -1.0, 1.0, "metal")
            add(body, -1.2, 1.2, 12.6, 13.6, -3.4, -2.9, "jade")
            add(body, -0.6, 0.6, 9.4, 12.6, -3.3, -2.9, "steel")
            for k in range(3):
                add(body, -2.2 + k * 1.6, -1.6 + k * 1.6, 3.4, 10.4, 3.0, 3.3, "cloth")
        for arm in arms:
            if arm is None:
                continue
            sign = 1 if arm.name.endswith("LeftArm") else -1
            add(arm, 3.2 * sign, 8.8 * sign, 24.2, 25.4, -2.6, 2.6, "cloth")
            add(arm, 7.8 * sign, 9.0 * sign, 21.2, 25.2, -1.4, 1.4, "jade")
    elif career == "xue":
        if body is not None:
            for k in range(3):
                add(body, -5.4 + k * 3.6, -4.2 + k * 3.6, 2.2, 13.2, 3.0, 3.4, "cloth")
            add(body, -1.8, 1.8, 6.0, 12.2, -3.4, -3.0, "cloth")
            add(body, -1.4, 1.4, 18.6, 20.6, -3.5, -3.0, "rune")
            add(body, -4.8, 4.8, 12.6, 13.4, -3.3, 3.0, "metal")
        for arm in arms:
            if arm is None:
                continue
            sign = 1 if arm.name.endswith("LeftArm") else -1
            add(arm, 3.6 * sign, 9.4 * sign, 23.8, 26.2, -2.6, 2.6, "metal")
            add(arm, 8.6 * sign, 9.8 * sign, 25.4, 27.2, -1.2, 1.2, "steel")
        if head is not None:
            for s in (-1, 1):
                add(head, 3.8 * s, 4.7 * s, 32.0, 34.4, -1.0, 1.0, "steel")
    elif career == "mo":
        if head is not None:
            for s in (-1, 1):
                add(head, 3.6 * s, 4.8 * s, 32.4, 36.4, -1.4, 1.4, "steel")
                add(head, 4.3 * s, 5.7 * s, 36.4, 39.0, -0.8, 0.8, "steel")
            add(head, -1.2, 1.2, 29.6, 31.2, -4.9, -4.3, "rune")
        if body is not None:
            for i in range(3):
                add(body, -3.6 + i * 0.5, 3.6 - i * 0.5, 15.2 + i * 2.6,
                    15.2 + i * 2.6 + 1.1, -3.5, -3.0, "steel")
            add(body, -1.1, 1.1, 18.4, 20.8, -3.6, -3.0, "rune")
            for k in range(5):
                add(body, -0.9, 0.9, 6.0 + k * 2.2, 7.4 + k * 2.2, 3.0, 3.5, "steel")
        for arm in arms:
            if arm is None:
                continue
            sign = 1 if arm.name.endswith("LeftArm") else -1
            add(arm, 4.0 * sign, 9.6 * sign, 23.4, 26.0, -2.8, 2.8, "steel")
            add(arm, 6.0 * sign - 0.9, 6.0 * sign + 0.9, 25.0, 27.2, -1.9, 1.9, "steel")
    elif career == "gui":
        if head is not None:
            add(head, -5.0, 5.0, 31.2, 33.8, -4.9, 4.9, "cloth")
            add(head, -2.6, 2.6, 25.8, 27.4, -4.8, -4.2, "cloth")
        if body is not None:
            for k in range(4):
                add(body, -4.6 + k * 2.6, -3.8 + k * 2.6, 2.0, 11.8, 3.0, 3.4, "cloth")
            add(body, -3.0, -2.4, 2.0, 10.8, -3.3, -2.9, "cloth")
            add(body, 2.4, 3.0, 2.0, 10.8, -3.3, -2.9, "cloth")
        for arm in arms:
            if arm is None:
                continue
            sign = 1 if arm.name.endswith("LeftArm") else -1
            add(arm, 5.2 * sign - 0.9, 5.2 * sign + 0.9, 25.2, 27.4, -1.4, 1.4, "jade")
            add(arm, 3.4 * sign, 8.8 * sign, 23.4, 24.6, -2.8, 2.8, "cloth")
    else:  # ti
        if body is not None:
            add(body, -5.2, 5.2, 20.6, 23.2, -3.4, 3.4, "metal")
            add(body, -1.2, 1.2, 13.6, 20.4, -3.5, -3.1, "steel")
            add(body, -4.4, 4.4, 12.6, 13.6, -3.4, 3.2, "steel")
        if head is not None:
            add(head, -4.9, 4.9, 28.2, 30.2, -4.9, -4.2, "metal")
            add(head, -1.1, 1.1, 30.6, 32.6, -4.9, -4.3, "steel")
        for arm in arms:
            if arm is None:
                continue
            sign = 1 if arm.name.endswith("LeftArm") else -1
            add(arm, 3.4 * sign, 10.4 * sign, 24.0, 26.4, -3.0, 3.0, "metal")
            add(arm, 9.4 * sign, 10.8 * sign, 22.4, 26.0, -2.2, 2.2, "steel")
            add(arm, 3.8 * sign, 8.8 * sign, 13.2, 15.2, -3.0, 3.0, "metal")
        for leg in legs:
            if leg is None:
                continue
            sign = 1 if leg.name.endswith("LeftLeg") else -1
            add(leg, 0.5 * sign, 4.0 * sign, 5.2, 7.0, -3.2, -2.6, "metal")
        for b in boots:
            if b is None:
                continue
            sign = 1 if b.name.endswith("LeftBoot") else -1
            add(b, 0.3 * sign, 4.2 * sign, 0.0, 1.4, -3.5, 2.6, "metal")


def build_set(career, realm):
    geo, color = ac.build(career, realm)
    return geo, color, career


def icon_for(geo, tex_arr, glow_arr, bones, out_png):
    hidden = [b.name for b in geo.bones if b.name not in bones]
    tris = build_mesh(geo, hidden=hidden)
    tmp = os.path.join(OUT, "_icon_tmp.png")
    render_views(tris, tmp, views=("front",), size=(512, 512),
                 transparent=True, unlit=True, texture=tex_arr, glow=glow_arr)
    g2_tex.make_icon(tmp, out_png, size=32)




def write_anim(family, geo):
    names = {b.name for b in geo.bones}
    bones = {}
    if "fx" in names:
        bones["fx"] = {
            "rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]},
            "scale": {"0.0": [1, 1, 1], "1.0": [1.15, 1.15, 1.15],
                      "2.0": [1, 1, 1], "3.0": [1.15, 1.15, 1.15],
                      "4.0": [1, 1, 1]},
        }
    if "cape" in names:
        bones["cape"] = {"rotation": {"0.0": [-2, 0, 0], "1.0": [2, 0, 1.5],
                                      "2.0": [-2, 0, 0], "3.0": [2, 0, -1.5],
                                      "4.0": [-2, 0, 0]}}
    if "veil" in names:
        bones["veil"] = {"rotation": {"0.0": [2, 0, -1], "1.0": [-2, 0, 1],
                                      "2.0": [2, 0, -1], "3.0": [-2, 0, 1],
                                      "4.0": [2, 0, -1]}}
    for name in ("lantern_l", "lantern_r"):
        if name in names:
            bones[name] = {
                "position": {"0.0": [0, 0, 0], "1.0": [0, 0.25, 0],
                              "2.0": [0, 0, 0], "3.0": [0, 0.25, 0],
                              "4.0": [0, 0, 0]},
                "rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]},
            }
    anim = {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 4.0, "bones": bones}}}
    import json as _json
    import os as _os
    with open(_os.path.join(RES, "animations", family + ".animation.json"),
              "w", encoding="utf-8") as f:
        _json.dump(anim, f, ensure_ascii=False, indent=2)


def main():
    previews = []
    stats = []
    for career in CAREERS:
        for realm in range(1, 11):
            geo, color, arch = build_set(career, realm)
            family = "career_armor_%s_%d" % (career, realm)
            geo.identifier = "geometry." + family
            g2_tex.pack_geo(geo, (256, 256))
            geo.tex = (256, 256)
            geo.save(os.path.join(RES, "geo", family + ".geo.json"))
            tex_p = os.path.join(RES, "textures", "item", family + ".png")
            glow_p = os.path.join(RES, "textures", "item", family + "_glowmask.png")
            ratio, _ = g2_tex.paint_geo(geo, (256, 256), tex_p, glow_p,
                                        pal=color["pal"], glow_color=color["glow"],
                                        hem=color["hem"], gold=color["gold"],
                                        grip_color=color["grip"],
                                        tassel_color=color["tassel"])
            tex_arr = np.array(Image.open(tex_p).convert("RGB"))
            glow_arr = np.array(Image.open(glow_p).convert("RGB"))
            write_anim(family, geo)
            slot_bones = [["armorHead"],
                          ["armorBody", "armorRightArm", "armorLeftArm"],
                          ["armorRightLeg", "armorLeftLeg"],
                          ["armorRightBoot", "armorLeftBoot"]]
            for slot_i, slot in enumerate(SLOTS):
                item = "career_armor_%s_%d_%s" % (career, realm, slot)
                icon_for(geo, tex_arr, glow_arr, slot_bones[slot_i],
                         os.path.join(RES, "textures", "item", item + ".png"))
            prev = os.path.join(OUT, "prev_%s.png" % family)
            render_views(build_mesh(geo), prev, views=("front", "hero"),
                         size=(200, 340),
                         label="%s·%s" % (REALM_CN[realm - 1], career),
                         texture=tex_arr, glow=glow_arr)
            previews.append(prev)
            stats.append({"family": family, "cubes": geo.cube_count(),
                          "glow": round(ratio, 4)})
            print("[OK] %-26s %s cubes=%d glow=%.1f%%"
                  % (family, REALM_CN[realm - 1], geo.cube_count(), ratio * 100))
    for career in CAREERS:
        for realm in range(1, 11):
            for slot in SLOTS:
                item = "career_armor_%s_%d_%s" % (career, realm, slot)
                with open(os.path.join(RES, "models", "item", item + ".json"),
                          "w", encoding="utf-8") as f:
                    json.dump({"parent": "minecraft:item/generated",
                               "textures": {"layer0": "hongmeng:item/" + item}},
                              f, ensure_ascii=False, indent=2)
    contact_sheet(previews, os.path.join(OUT, "0_ARMOR_50.png"), cols=10,
                  title="50 套独立职业甲（5 职业 × 10 境界）")
    with open(os.path.join(OUT, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)
    print("[DONE] 50 套甲 →", RES)


if __name__ == "__main__":
    main()
