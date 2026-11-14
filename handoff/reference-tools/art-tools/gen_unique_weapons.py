# -*- coding: utf-8 -*-
"""
60 把独立武器：每个「类型 × 境界」都是一件独立武器、独立外观
================================================================
玩家要求：外观不分阶段——每个外观都是一把独立武器，从最基础境界到鸿蒙境界都有。

实现：
- 6 型 × 10 境界 = 60 个 family：`career_<form>_t<tier>`
- 造型以现有 27 个名器为"原型"，按境界追加独立的饰件（金属箍/玉饰/符文/光环）
  并调色（铁灰 → 铜金 → 青玉 → 血紫 → 霜银），保证 60 把外观各不相同；
- 名称、语言、物品模型、映射表一并生成。
"""
from __future__ import annotations

import json
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import g2_tex
import build_weapons_v2 as bw
import build_war_scythes as bws
from artlib import build_mesh, contact_sheet, render_views

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(HERE, "..", "w01r", "weapons_unique"))
WS = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
os.makedirs(OUT, exist_ok=True)

FORMS = ["sword", "greatsword", "scythe", "spear", "staff", "war_scythe"]

# 每个境界段使用的"原型"（对应 v2 名器 family 的 key）
ARCH = {
    "sword": ["qingfeng", "qingfeng", "chixiao", "chixiao", "xuantie",
              "xuantie", "xuecao", "xuecao", "shuangwen", "shuangwen"],
    "greatsword": ["zhenyue", "zhenyue", "zhenyue", "duanyue", "duanyue",
                   "longji", "longji", "longji", "mogu", "mogu"],
    "scythe": ["gouhun", "gouhun", "sishen", "sishen", "xueyue",
               "xueyue", "yougu", "yougu", "xukong", "xukong"],
    "spear": ["pojun", "pojun", "pojun", "youlong", "youlong",
              "youlong", "leiji", "leiji", "xueying", "xueying"],
    "staff": ["jiangmo", "jiangmo", "jiangmo", "panlong", "panlong",
              "panlong", "zhenhun", "zhenhun", "shougu", "shougu"],
    "war_scythe": ["lie", "lie", "po", "po", "long", "long", "xue", "xue",
                   "kong", "kong"],
}

REALM_CN = ["练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"]
REALM_EN = ["Qi Refining", "Foundation", "Golden Core", "Nascent Soul",
            "Spirit Severing", "Dao Seal", "Ascension", "Immortal", "Emperor", "Hongmeng"]

NAMES = {
    "sword": [("青锋剑", "Azure Edge Sword"), ("青锋·流光剑", "Azure Edge Gleam Sword"),
              ("赤霄剑", "Crimson Sky Sword"), ("赤霄·灵纹剑", "Crimson Sky Runesword"),
              ("玄铁剑", "Darkiron Sword"), ("玄铁·重锋剑", "Darkiron Heavy Edge"),
              ("血槽剑", "Bloodgroove Sword"), ("血槽·魔纹剑", "Bloodgroove Runeblade"),
              ("霜纹剑", "Frostscript Sword"), ("霜纹·古篆剑", "Frostscript Ancient Sword")],
    "greatsword": [("铁岳巨剑", "Ironpeak Greatsword"), ("镇岳巨剑", "Mountain-Sealing Greatsword"),
                   ("断岳剑", "Mountain-Breaking Sword"), ("断岳·玄重剑", "Mountain-Breaking Heavy Sword"),
                   ("龙脊巨剑", "Dragonbone Greatsword"), ("龙脊·裂地剑", "Dragonbone Earthsplitter"),
                   ("骨刃巨剑", "Boneblade Greatsword"), ("魔骨巨剑", "Fiendbone Greatsword"),
                   ("魔骨血刃巨剑", "Fiendbone Bloodblade"), ("破天·血刃巨剑", "Skyrend Bloodblade")],
    "scythe": [("勾魂镰", "Soulreaper Scythe"), ("夺魄镰", "Soulcatcher Scythe"),
               ("死神镰", "Death's Scythe"), ("死神·断魂镰", "Death's Soulsever Scythe"),
               ("血月镰", "Bloodmoon Scythe"), ("血月·赤潮镰", "Bloodmoon Tide Scythe"),
               ("幽骨镰", "Netherbone Scythe"), ("幽骨·冥纹镰", "Netherbone Runeblade"),
               ("死神·帝镰", "Death Emperor Scythe"), ("死神·终焉镰", "Death Finality Scythe")],
    "spear": [("破军枪", "Armybreaker Spear"), ("破军·铁叶枪", "Armybreaker Ironleaf Spear"),
              ("破军·裂甲枪", "Armybreaker Armorrender"), ("游龙枪", "Dragon Spear"),
              ("游龙·穿云枪", "Cloudpiercer Dragon Spear"), ("游龙·盘蛇枪", "Coiled Serpent Spear"),
              ("雷脊枪", "Thunderidge Spear"), ("雷脊·玄铁枪", "Thunderidge Iron Spear"),
              ("血缨枪", "Bloodtassel Spear"), ("血缨·魔纹枪", "Bloodtassel Rune Spear")],
    "staff": [("降魔棍", "Demonqueller Staff"), ("降魔·铜环棍", "Demonqueller Brassring Staff"),
              ("降魔·禅杖", "Demonqueller Khakkhara"), ("盘龙棍", "Coiled Dragon Staff"),
              ("盘龙·金鳞棍", "Golden Scale Dragon Staff"), ("盘龙·龙首杖", "Dragonhead Staff"),
              ("镇魂棍", "Soulcalm Staff"), ("镇魂·玄木杖", "Soulcalm Woodstaff"),
              ("兽骨棍", "Beastbone Staff"), ("兽骨·铃纹杖", "Beastbone Bellstaff")],
    "war_scythe": [("裂地战镰", "Earthsplitter War Scythe"), ("裂地·重刃镰", "Earthsplitter Heavy War Scythe"),
                   ("破军战镰", "Armybreaker War Scythe"), ("破军·金翼镰", "Armybreaker Goldwing War Scythe"),
                   ("龙牙战镰", "Dragontooth War Scythe"), ("龙牙·锯齿战镰", "Dragontooth Serrated War Scythe"),
                   ("血河战镰", "Bloodriver War Scythe"), ("血河·噬魂战镰", "Bloodriver Soulrend War Scythe"),
                   ("虚空战镰", "Void War Scythe"), ("虚空·终焉战镰", "Void Finality War Scythe")],
}

# 境界段调色（附到武器的高亮件上）
ACCENT = {
    0: (150, 190, 205),   # 1-2 铁灰青
    1: (220, 165, 95),    # 3-4 铜金
    2: (130, 230, 175),   # 5-6 青玉
    3: (235, 86, 110),    # 7-8 血紫
    4: (185, 125, 255),   # 9-10 霜紫
}


def blend(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def tier_color(c, tier):
    band = 0 if tier <= 2 else 1 if tier <= 4 else 2 if tier <= 6 else 3 if tier <= 8 else 4
    accent = ACCENT[band]
    brightness = 0.86 + 0.025 * tier
    def mul(v):
        return tuple(min(255, int(v * brightness)) for v in v)
    pal = dict(c["pal"])
    pal["metal"] = blend(mul(pal["metal"]), accent, 0.16)
    pal["steel"] = blend(mul(pal["steel"]), accent, 0.10)
    pal["cloth"] = mul(pal["cloth"])
    pal["jade"] = blend(pal["jade"], accent, 0.55)
    pal["rune"] = blend(pal["rune"], accent, 0.75)
    glow = blend(c["glow"], accent, 0.7)
    return dict(pal=pal, glow=glow, hem=blend(c["hem"], accent, 0.5),
                gold=blend(c["gold"], accent, 0.35),
                grip=c.get("grip"), tassel=c.get("tassel"))


def find_bone(geo, names):
    for n in names:
        for b in geo.bones:
            if b.name == n:
                return b
    return geo.bones[0] if geo.bones else None


def embellish(geo, form, tier):
    """按境界追加独立饰件：金属箍/玉饰/符文/光环，逐级递增。"""
    grip = find_bone(geo, ["grip", "shaft"])
    head = find_bone(geo, ["head", "blade", "blade_main"])
    # 握把/杆：金属箍（3 阶起）+ 玉饰（2 阶起）
    if grip is not None:
        px, py, pz = grip.pivot
        for k in range(1 + tier // 2):
            y = py + 2.2 + k * 2.6
            grip.add((px - 1.9, y, pz - 1.9), (3.8, 0.65, 3.8), mat="steel")
        if tier >= 2:
            y = py + 1.4 + (tier % 3) * 0.8
            grip.add((px - 0.85, y, pz - 2.3), (1.7, 1.4, 0.7), mat="jade")
    # 刃/头：符文宝石（数量随境界增加）
    if head is not None:
        px, py, pz = head.pivot
        for k in range(tier // 2 + 1):
            y = py + 3.0 + k * 2.4
            x = -1.0 + (k % 2) * 1.2
            head.add((px + x - 0.7, y, pz - 1.1), (1.4, 1.4, 0.8), mat="rune")
        # 高阶：套筒光环/悬浮晶片
        if tier >= 7:
            for k in range(3):
                ang = k * 2.1 + tier
                head.add((px + math.cos(ang) * 2.4 - 0.4,
                          py + 1.0 + k * 0.9,
                          pz + math.sin(ang) * 2.4 - 0.4),
                         (1.3, 1.3, 1.3), mat="jade")




def add_fx(geo, form, tier):
    """3D 特效：绕武器旋转的发光颗粒 + 呼吸灯（动画在 write_anim 里）。"""
    fx = geo.bone("fx", (0, 12.0, 0))
    count = 4 if tier < 7 else 6
    for k in range(count):
        a = 2 * math.pi * k / count + tier * 0.3
        r = 3.6 + 0.5 * (tier % 3)
        y = 11.0 + tier * 1.0 + k * 0.8
        fx.add((math.cos(a) * r - 0.45, y - 0.45, math.sin(a) * r - 0.45),
               (0.9, 0.9, 0.9), mat="rune")
    fx.add((-0.6, 12.0 + tier, -4.2), (1.2, 1.2, 1.2), mat="jade")


def write_anim(family, geo):
    bones = {}
    bone_names = {b.name for b in geo.bones}
    if "fx" in bone_names:
        bones["fx"] = {
            "rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]},
            "scale": {"0.0": [1, 1, 1], "1.0": [1.12, 1.12, 1.12],
                      "2.0": [1, 1, 1], "3.0": [1.12, 1.12, 1.12],
                      "4.0": [1, 1, 1]},
        }
    for name in ("ring_a", "ring_b"):
        if name in bone_names:
            direction = -360 if name == "ring_a" else 360
            bones[name] = {"rotation": {"0.0": [0, 0, 0], "4.0": [0, direction, 0]}}
    if "tassel" in bone_names:
        bones["tassel"] = {"rotation": {"0.0": [0, 0, -6], "1.0": [0, 0, 6],
                                        "2.0": [0, 0, -6], "3.0": [0, 0, 6],
                                        "4.0": [0, 0, -6]}}
    anim = {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 4.0, "bones": bones}}}
    import json as _json
    import os as _os
    p = _os.path.join(RES, "animations", family + ".animation.json")
    with open(p, "w", encoding="utf-8") as f:
        _json.dump(anim, f, ensure_ascii=False, indent=2)


def main():
    stats = []
    previews = []
    mapping = {}
    for form in FORMS:
        for tier in range(1, 11):
            key = ARCH[form][tier - 1]
            if form == "war_scythe":
                spec = next(s for s in bws.SPECS if s["key"] == key)
                geo = bws.build(spec)
            else:
                spec = next(s for s in bw.SPECS
                            if s["form"] == form and s["key"] == key)
                geo = bw.BUILDERS[form](spec)
            family = "career_%s_t%d" % (form, tier)
            embellish(geo, form, tier)
            add_fx(geo, form, tier)
            geo.identifier = "geometry." + family
            g2_tex.pack_geo(geo, (256, 256))
            geo.tex = (256, 256)
            geo.save(os.path.join(RES, "geo", family + ".geo.json"))
            color = tier_color(spec["color"], tier)
            tex_p = os.path.join(RES, "textures", "item", family + ".png")
            glow_p = os.path.join(RES, "textures", "item", family + "_glowmask.png")
            ratio, _ = g2_tex.paint_geo(geo, (256, 256), tex_p, glow_p,
                                        pal=color["pal"], glow_color=color["glow"],
                                        hem=color["hem"], gold=color["gold"],
                                        grip_color=color["grip"],
                                        tassel_color=color["tassel"])
            write_anim(family, geo)
            tex_arr = np.array(Image.open(tex_p).convert("RGB"))
            glow_arr = np.array(Image.open(glow_p).convert("RGB"))
            prev = os.path.join(OUT, "prev_%s.png" % family)
            render_views(build_mesh(geo), prev, views=("front", "hero"),
                         size=(180, 380),
                         label="%s·%s" % (REALM_CN[tier - 1], NAMES[form][tier - 1][0]),
                         texture=tex_arr, glow=glow_arr)
            previews.append(prev)
            mapping["career_weapon_%s_%d" % (form, tier)] = family
            stats.append({"family": family, "form": form, "tier": tier,
                          "name": NAMES[form][tier - 1][0],
                          "cubes": geo.cube_count(), "glow": round(ratio, 4)})
            print("[OK] %-24s %s·%s cubes=%d" % (family, REALM_CN[tier - 1],
                                                 NAMES[form][tier - 1][0],
                                                 geo.cube_count()))
    # 60 个物品模型 + lang
    zh = json.load(open(os.path.join(RES, "lang", "zh_cn.json"), encoding="utf-8"))
    en = json.load(open(os.path.join(RES, "lang", "en_us.json"), encoding="utf-8"))
    for form in FORMS:
        for tier in range(1, 11):
            item = "career_weapon_%s_%d" % (form, tier)
            fam = mapping[item]
            with open(os.path.join(RES, "models", "item", item + ".json"),
                      "w", encoding="utf-8") as f:
                json.dump({"parent": "builtin/entity",
                           "textures": {"particle": "hongmeng:item/" + fam}},
                          f, ensure_ascii=False, indent=2)
            cn = NAMES[form][tier - 1][0]
            en_name = NAMES[form][tier - 1][1]
            zh["item.hongmeng." + item] = "%s·%s" % (REALM_CN[tier - 1], cn)
            en["item.hongmeng." + item] = "%s - %s" % (REALM_EN[tier - 1], en_name)
    json.dump(zh, open(os.path.join(RES, "lang", "zh_cn.json"), "w",
                       encoding="utf-8"), ensure_ascii=False, indent=2)
    json.dump(en, open(os.path.join(RES, "lang", "en_us.json"), "w",
                       encoding="utf-8"), ensure_ascii=False, indent=2)
    with open(os.path.join(OUT, "mapping.json"), "w", encoding="utf-8") as f:
        json.dump({"tier_to_family": mapping, "stats": stats},
                  f, ensure_ascii=False, indent=2)
    contact_sheet(previews, os.path.join(OUT, "0_WEAPONS_60.png"), cols=10,
                  title="60 把独立武器：6 型 × 10 境界，各自独立外观")
    print("[DONE] 60 把独立武器 →", RES)


if __name__ == "__main__":
    import math  # noqa: E402  （embellish 用到，放这里避免顶部顺序问题）
    main()
