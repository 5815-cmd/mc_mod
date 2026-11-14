# -*- coding: utf-8 -*-
"""为 40 件职业甲生成物品模型 + 中英 lang（幂等，可重复跑）。"""
import json
import os
import re

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")

CAREER_ID = {"JIAN_XIU": "jian", "XUE_XIU": "xue", "MO_XIU": "mo",
             "GUI_XIU": "gui", "TI_XIU": "ti"}
SLOTS = ["helmet", "chestplate", "leggings", "boots"]
SLOT_CN = ["冠", "胸甲", "护腿", "靴"]
TIER_CN = {1: "初阶", 2: "进阶", 3: "名器"}
TIER_EN = {1: "Basic", 2: "Advanced", 3: "Master"}
SUIT_EN = {
    "青锋剑袍": "Azure Blade Robe", "剑心道袍": "Swordheart Robe",
    "血纹战衣": "Bloodrun Vestment", "血海战衣": "Bloodsea Vestment",
    "骸骨魔铠": "Bonefiend Armor", "魔渊魔铠": "Abyssal Fiend Armor",
    "幽魂道袍": "Wraith Daoist Robe", "黄泉鬼袍": "Yellow Springs Robe",
    "玄铁重甲": "Darkiron Heavy Armor", "不灭金身": "Undying Golden Body",
    "剑仙羽衣": "Sword Immortal Feather Robe", "血神袍": "Blood God Robe",
    "天魔甲": "Heavenly Demon Armor", "幽冥纱": "Netherworld Shroud",
    "不灭圣铠": "Undying Sacred Armor",
}


def main():
    src = open(os.path.join(
        WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
        "xiuxian", "item", "armor", "CareerArmorSet.java"), encoding="utf-8").read()
    pattern = re.compile(r'(\w+)\(Career\.(\w+), Tier\.(\w+), "([^"]+)"')
    sets = []
    for m in pattern.finditer(src):
        _, career, tier, cn = m.groups()
        iv = {"BASIC": 1, "ADVANCED": 2, "MASTER": 3}[tier]
        sets.append((career, iv, cn))
    print("sets:", sets)
    assert len(sets) == 15, "应读出 15 套"

    models = 0
    zh = json.load(open(os.path.join(RES, "lang", "zh_cn.json"), encoding="utf-8"))
    en = json.load(open(os.path.join(RES, "lang", "en_us.json"), encoding="utf-8"))
    for career, iv, suit in sets:
        cid = CAREER_ID[career]
        en_suit = SUIT_EN.get(suit, suit)
        for slot, scn in zip(SLOTS, SLOT_CN):
            item = "career_armor_%s_%d_%s" % (cid, iv, slot)
            mp = os.path.join(RES, "models", "item", item + ".json")
            if not os.path.exists(mp):
                os.makedirs(os.path.dirname(mp), exist_ok=True)
                json.dump({"parent": "minecraft:item/generated",
                           "textures": {"layer0": "hongmeng:item/" + item}},
                          open(mp, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
                models += 1
            key = "item.hongmeng." + item
            if key not in zh:
                zh[key] = "%s·%s·%s" % (TIER_CN[iv], suit, scn)
            if key not in en:
                en[key] = "%s - %s" % (TIER_EN[iv], en_suit)
    json.dump(zh, open(os.path.join(RES, "lang", "zh_cn.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)
    json.dump(en, open(os.path.join(RES, "lang", "en_us.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)
    total_zh = sum(1 for k in zh if "career_armor_" in k)
    print("new item models:", models, "| career armor lang keys:", total_zh)


if __name__ == "__main__":
    main()
