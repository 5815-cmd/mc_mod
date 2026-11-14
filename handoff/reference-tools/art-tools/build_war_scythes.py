# -*- coding: utf-8 -*-
"""
C 扩充 · 第 6 型「战镰」共 10 件（5 个基础名 × 2 阶）
=====================================================
结构按返修包《战镰结构参考》：刀身沿杆延长线（同向）、外侧薄刃、内侧厚脊、
根部护手/套筒、两个握把/握钉、尾端配重；三档剪影/材料/附件递进。
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
from artlib import Geo, build_mesh, contact_sheet, render_views

HERE = os.path.dirname(os.path.abspath(__file__))
PREV = os.path.join(os.path.abspath(os.path.join(HERE, "..", "w01r")), "war_scythes")
WS = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
os.makedirs(PREV, exist_ok=True)


def add(bone, x0, x1, y0, y1, z0, z1, mat="metal"):
    bone.add((x0, y0, z0), (x1 - x0, y1 - y0, z1 - z0), mat=mat)


def P(metal, steel, cloth, jade, rune, glow, hem=None, gold=(201, 162, 74),
      grip=None):
    return dict(pal={"metal": metal, "steel": steel, "cloth": cloth,
                     "jade": jade, "rune": rune},
                glow=glow, hem=hem or glow, gold=gold, grip=grip)


SPECS = [
    dict(key="lie", cn="裂地战镰", tiers=(1, 2), blade=12.0, width=4.2,
         bend=1.2, teeth=0, fragments=0, guard="bar", rune=0,
         color=P((120, 112, 96), (196, 188, 168), (52, 42, 28), (150, 190, 120),
                 (200, 210, 160), glow=(200, 210, 160), hem=(90, 76, 48),
                 grip=(56, 42, 26))),
    dict(key="po", cn="破军战镰", tiers=(3, 4), blade=13.6, width=4.6,
         bend=1.4, teeth=2, fragments=0, guard="wing", rune=1,
         color=P((150, 130, 96), (222, 200, 156), (60, 40, 20), (226, 190, 96),
                 (240, 210, 120), glow=(240, 210, 120), hem=(110, 82, 34),
                 grip=(66, 46, 24))),
    dict(key="long", cn="龙牙战镰", tiers=(5, 6), blade=14.8, width=5.0,
         bend=1.6, teeth=4, fragments=0, guard="dragon", rune=2,
         color=P((106, 120, 88), (192, 206, 152), (44, 56, 36), (120, 255, 180),
                 (130, 255, 190), glow=(130, 255, 190), hem=(60, 130, 90),
                 grip=(44, 56, 32))),
    dict(key="xue", cn="血河战镰", tiers=(7, 8), blade=15.6, width=5.2,
         bend=1.8, teeth=3, fragments=0, guard="wing", rune=4,
         color=P((130, 70, 70), (202, 152, 152), (54, 8, 12), (255, 70, 70),
                 (255, 66, 66), glow=(255, 66, 66), hem=(110, 10, 16),
                 grip=(52, 10, 14))),
    dict(key="kong", cn="虚空战镰", tiers=(9, 10), blade=16.4, width=5.6,
         bend=2.0, teeth=2, fragments=3, guard="void", rune=2,
         color=P((54, 44, 70), (120, 104, 168), (18, 14, 28), (176, 96, 255),
                 (176, 96, 255), glow=(176, 96, 255), hem=(60, 30, 90),
                 grip=(24, 18, 34))),
]


def build(spec):
    g = Geo("geometry.career_war_scythe_" + spec["key"], tex=(256, 256))
    S = 18.0
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    blade = g.bone("blade", (0, S, 0), parent="head")
    # 细杆（灵动）+ 两个握把/握钉
    nseg = 8
    for i in range(nseg):
        y0 = i * S / nseg
        y1 = (i + 1) * S / nseg
        d0 = (i / nseg) ** 1.5 * 2.0
        d1 = ((i + 1) / nseg) ** 1.5 * 2.0
        r = 1.05 - 0.12 * (i / nseg)
        add(shaft, d0 - r, d1 + r, y0, y1, -r, r, "cloth")
    for y in (6.0, 11.5):
        add(shaft, -1.35, 1.65, y, y + 2.6, -1.3, 1.3, "cloth")
        add(shaft, -0.35, 0.35, y + 1.1, y + 1.8, -2.0, 2.0, "steel")
    add(shaft, -1.8, 1.8, -2.0, 0.4, -1.8, 1.8, "metal")
    add(shaft, -1.2, 1.2, -3.4, -2.0, -1.2, 1.2, "metal")
    # 套筒 + 护手
    add(head, -1.9, 1.9, S - 1.4, S + 2.8, -1.9, 1.9, "metal")
    add(head, -1.4, 1.4, S + 2.8, S + 4.0, -1.4, 1.4, "steel")
    if spec["guard"] == "bar":
        add(head, -3.4, 3.4, S + 0.2, S + 1.6, -0.9, 0.9, "steel")
    elif spec["guard"] == "wing":
        add(head, -3.8, 3.8, S + 0.2, S + 1.8, -0.9, 0.9, "steel")
        for s in (-1, 1):
            add(head, 3.2 * s, 4.4 * s, S + 0.8, S + 3.0, -0.6, 0.6, "metal")
    elif spec["guard"] == "dragon":
        add(head, -4.0, 4.0, S + 0.2, S + 2.0, -1.0, 1.0, "metal")
        for s in (-1, 1):
            add(head, 3.4 * s, 4.8 * s, S + 1.0, S + 3.6, -0.6, 0.6, "jade")
    else:  # void
        add(head, -4.2, 4.2, S + 0.2, S + 2.2, -1.1, 1.1, "metal")
        for i in range(3):
            add(head, -1.5 + i * 1.5, -0.8 + i * 1.5, S + 2.2, S + 3.4,
                -2.0, -1.4, "rune")
    # 刀身：沿 +Y 延长线，向外弯；外侧薄刃、内侧厚脊
    total, width = spec["blade"], spec["width"]
    n = 11
    for i in range(n):
        f0, f1 = i / n, (i + 1) / n
        y0 = S + 3.0 + total * f0
        y1 = S + 3.0 + total * f1
        b0 = spec["bend"] * f0 * f0
        b1 = spec["bend"] * f1 * f1
        w = width * (1.0 - 0.60 * f0)
        t = 2.8 - 1.1 * f0
        blade.add((b0 - w * 0.35, y0, -t / 2),
                  (b1 - b0 + w * 0.7, y1 - y0, t), mat="metal")
        blade.add((b0 - w * 0.35, y0, -t * 0.60),
                  (w * 0.42, y1 - y0, t * 1.2), mat="steel")   # 内侧厚脊
        blade.add((b1 + w * 0.02, y0, -0.36), (0.9, y1 - y0, 0.72),
                  mat="steel")                                  # 外侧薄刃
        if spec["teeth"] and i % 2 == 0 and i < spec["teeth"] * 2:
            blade.add((b0 - w * 0.1, y1 - 0.2, -0.35),
                      (w * 0.7, 1.1, 0.7), mat="steel")
        if spec["rune"] and i in (2, 4, 6, 8)[:spec["rune"]]:
            blade.add((b0 + w * 0.1, y0 + 0.2, -t / 2 - 0.08),
                      (0.8, y1 - y0 - 0.4, t + 0.16), mat="rune")
    # 刀尖
    tx = spec["bend"] + 0.8
    ty = S + 3.0 + total
    blade.add((tx - 0.7, ty - 0.3, -0.5), (2.2, 3.4, 1.0), mat="steel",
              rotation=(0, 0, -24), pivot=(tx, ty, 0))
    for i in range(spec["fragments"]):
        a = math.radians(34 + i * 20)
        px = math.cos(a) * 5.2 + 2.0
        py = S + 8.0 + math.sin(a) * 5.2
        add(blade, px - 0.9, px + 0.9, py - 0.9, py + 0.9, -0.5, 0.5, "jade")
    return g


REALM_CN = ["练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"]
REALM_EN = ["Qi Refining", "Foundation", "Golden Core", "Nascent Soul", "Spirit Severing",
            "Dao Seal", "Ascension", "Immortal", "Emperor", "Hongmeng"]
NAME_EN = {"lie": "Earthsplitter War Scythe", "po": "Armybreaker War Scythe",
           "long": "Dragontooth War Scythe", "xue": "Bloodriver War Scythe",
           "kong": "Void War Scythe"}


def main():
    previews = []
    stats = []
    tier_to_family = {}
    for spec in SPECS:
        geo = build(spec)
        family = "career_war_scythe_" + spec["key"]
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
                                    grip_color=c["grip"])
        anim = {"format_version": "1.8.0", "animations": {
            "idle": {"loop": True, "animation_length": 2.0, "bones": {}}}}
        with open(os.path.join(RES, "animations", family + ".animation.json"),
                  "w", encoding="utf-8") as f:
            json.dump(anim, f, ensure_ascii=False, indent=2)
        tex_arr = np.array(Image.open(tex_p).convert("RGB"))
        glow_arr = np.array(Image.open(glow_p).convert("RGB"))
        prev = os.path.join(PREV, "prev_%s.png" % family)
        render_views(build_mesh(geo), prev, views=("front", "side", "hero"),
                     size=(260, 560),
                     label="%s（%d-%d 阶）" % (spec["cn"], spec["tiers"][0],
                                              spec["tiers"][1]),
                     texture=tex_arr, glow=glow_arr)
        previews.append(prev)
        stats.append({"family": family, "cn": spec["cn"],
                      "tiers": list(spec["tiers"]), "cubes": geo.cube_count(),
                      "glow": round(ratio, 4)})
        for tier in range(spec["tiers"][0], spec["tiers"][1] + 1):
            tier_to_family["career_weapon_war_scythe_%d" % tier] = family
        print("[OK] %-28s %d-%d cubes=%d glow=%.1f%%"
              % (family, spec["tiers"][0], spec["tiers"][1],
                 geo.cube_count(), ratio * 100))

    # 物品模型 + 语言
    zh = json.load(open(os.path.join(RES, "lang", "zh_cn.json"), encoding="utf-8"))
    en = json.load(open(os.path.join(RES, "lang", "en_us.json"), encoding="utf-8"))
    for tier in range(1, 11):
        item = "career_weapon_war_scythe_%d" % tier
        fam = tier_to_family[item]
        key = fam.split("_")[-1]
        cn2 = {"lie": "裂地战镰", "po": "破军战镰", "long": "龙牙战镰",
               "xue": "血河战镰", "kong": "虚空战镰"}[key]
        mp = os.path.join(RES, "models", "item", item + ".json")
        with open(mp, "w", encoding="utf-8") as f:
            json.dump({"parent": "builtin/entity",
                       "textures": {"particle": "hongmeng:item/" + fam}},
                      f, ensure_ascii=False, indent=2)
        zh["item.hongmeng." + item] = "%s·%s" % (REALM_CN[tier - 1], cn2)
        en["item.hongmeng." + item] = "%s - %s" % (REALM_EN[tier - 1], NAME_EN[key])
    json.dump(zh, open(os.path.join(RES, "lang", "zh_cn.json"), "w",
                       encoding="utf-8"), ensure_ascii=False, indent=2)
    json.dump(en, open(os.path.join(RES, "lang", "en_us.json"), "w",
                       encoding="utf-8"), ensure_ascii=False, indent=2)
    contact_sheet(previews, os.path.join(PREV, "0_WAR_SCYTHES.png"), cols=3,
                  title="C 扩充：战镰 5 个基础名（沿杆同向刀身）")
    with open(os.path.join(PREV, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)
    print("[DONE] 战镰 10 件 →", RES)


if __name__ == "__main__":
    main()
