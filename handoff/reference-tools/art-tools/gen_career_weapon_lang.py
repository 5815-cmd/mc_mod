# -*- coding: utf-8 -*-
"""为 50 件通用双手武器生成物品模型 + 中英 lang（幂等）。"""
import json
import os

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")

REALM_CN = ["练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"]
REALM_EN = ["Qi Refining", "Foundation", "Golden Core", "Nascent Soul", "Spirit Severing",
            "Dao Seal", "Ascension", "Immortal", "Emperor", "Hongmeng"]

FORMS = ["sword", "greatsword", "scythe", "spear", "staff"]
FORM_EN = {"sword": "Longsword", "greatsword": "Greatsword", "scythe": "Scythe",
           "spear": "Spear", "staff": "Staff"}

# (起始阶, 结束阶, 中文基础名, 英文基础名)
BASE = {
    "sword": [(1, 2, "青锋长剑", "Azure Edge Longsword"),
              (3, 4, "赤霄灵纹剑", "Crimson Sky Runesword"),
              (5, 6, "玄铁重锋剑", "Darkiron Heavy Edge"),
              (7, 8, "血槽魔纹剑", "Bloodgroove Runeblade"),
              (9, 10, "霜纹古篆剑", "Frostscript Ancient Sword")],
    "greatsword": [(1, 3, "镇岳巨剑", "Mountain-Sealing Greatsword"),
                   (4, 5, "断岳玄重剑", "Mountain-Breaking Greatsword"),
                   (6, 8, "龙脊裂地剑", "Dragonbone Earthsplitter"),
                   (9, 10, "魔骨血刃巨剑", "Fiendbone Bloodblade")],
    "scythe": [(1, 2, "勾魂镰", "Soulreaper Scythe"),
               (3, 4, "死神之镰", "Death's Scythe"),
               (5, 6, "血月弯镰", "Bloodmoon Crescent"),
               (7, 8, "幽骨冥纹镰", "Netherbone Rune Scythe"),
               (9, 10, "虚空割裂镰", "Voidrender Scythe")],
    "spear": [(1, 3, "破军枪", "Armybreaker Spear"),
              (4, 6, "游龙穿云枪", "Cloudpiercer Dragon Spear"),
              (7, 8, "雷脊玄铁枪", "Thunderidge Iron Spear"),
              (9, 10, "血缨魔纹枪", "Bloodtassel Rune Spear")],
    "staff": [(1, 3, "降魔棍", "Demonqueller Staff"),
              (4, 6, "盘龙棍", "Coiled Dragon Staff"),
              (7, 8, "镇魂玄木棍", "Soulcalm Woodstaff"),
              (9, 10, "兽骨铃纹棍", "Beastbone Bellstaff")],
}


def style_of(tier):
    if tier <= 3:
        return "low"
    if tier <= 6:
        return "mid"
    return "high"


def base_name(form, tier):
    for lo, hi, cn, en in BASE[form]:
        if lo <= tier <= hi:
            return cn, en
    raise ValueError((form, tier))


def main():
    zh = json.load(open(os.path.join(RES, "lang", "zh_cn.json"), encoding="utf-8"))
    en = json.load(open(os.path.join(RES, "lang", "en_us.json"), encoding="utf-8"))
    models = 0
    for form in FORMS:
        for tier in range(1, 11):
            item = "career_weapon_%s_%d" % (form, tier)
            family = "career_%s_%s" % (form, style_of(tier))
            mp = os.path.join(RES, "models", "item", item + ".json")
            if not os.path.exists(mp):
                os.makedirs(os.path.dirname(mp), exist_ok=True)
                json.dump({"parent": "builtin/entity",
                           "textures": {"particle": "hongmeng:item/" + family}},
                          open(mp, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
                models += 1
            cn, enb = base_name(form, tier)
            key = "item.hongmeng." + item
            zh[key] = "%s·%s" % (REALM_CN[tier - 1], cn)
            en[key] = "%s - %s" % (REALM_EN[tier - 1], enb)
    json.dump(zh, open(os.path.join(RES, "lang", "zh_cn.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)
    json.dump(en, open(os.path.join(RES, "lang", "en_us.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)
    total = sum(1 for k in zh if "career_weapon_" in k)
    print("new models:", models, "| career weapon lang keys:", total)


if __name__ == "__main__":
    main()
