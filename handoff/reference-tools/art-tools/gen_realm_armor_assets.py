# -*- coding: utf-8 -*-
"""
按境界分件的职业甲资源：200 件（5 职业 × 10 境界 × 4 部位）
=============================================================
- 外观复用 15 套（5 职业 × 初/进/名器）：1~3 境初阶、4~6 进阶、7~10 名器；
- 物品模型 layer0 直接指向对应档的 32×32 图标（图标不重复生成）；
- 中英 lang：境界名 + 套名 + 部位。
"""
import json
import os
import re

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
SLOTS = ["helmet", "chestplate", "leggings", "boots"]
SLOT_CN = ["冠", "胸甲", "护腿", "靴"]
SLOT_EN = ["Helmet", "Chestguard", "Legguards", "Boots"]
REALM_CN = ["练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"]
REALM_EN = ["Qi Refining", "Foundation", "Golden Core", "Nascent Soul", "Spirit Severing",
            "Dao Seal", "Ascension", "Immortal", "Emperor", "Hongmeng"]
CAREERS = ["jian", "xue", "mo", "gui", "ti"]
CAREER_ID = {"JIAN_XIU": "jian", "XUE_XIU": "xue", "MO_XIU": "mo",
             "GUI_XIU": "gui", "TI_XIU": "ti"}
SUIT_EN = {
    "青锋剑袍": "Azure Blade Robe", "剑心道袍": "Swordheart Robe",
    "剑仙羽衣": "Sword Immortal Feather Robe",
    "血纹战衣": "Bloodrun Vestment", "血海战衣": "Bloodsea Vestment",
    "血神袍": "Blood God Robe",
    "骸骨魔铠": "Bonefiend Armor", "魔渊魔铠": "Abyssal Fiend Armor",
    "天魔甲": "Heavenly Demon Armor",
    "幽魂道袍": "Wraith Daoist Robe", "黄泉鬼袍": "Yellow Springs Robe",
    "幽冥纱": "Netherworld Shroud",
    "玄铁重甲": "Darkiron Heavy Armor", "不灭金身": "Undying Golden Body",
    "不灭圣铠": "Undying Sacred Armor",
}


def read_bands():
    src = open(os.path.join(
        WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng", "xiuxian",
        "item", "armor", "CareerArmorSet.java"), encoding="utf-8").read()
    pat = re.compile(r'(\w+)\(Career\.(\w+), Tier\.(\w+), "([^"]+)", "([^"]+)"')
    out = {}
    for m in pat.finditer(src):
        _, career, tier, cn, en = m.groups()
        band = {"BASIC": 1, "ADVANCED": 2, "MASTER": 3}[tier]
        out[(CAREER_ID[career], band)] = (cn, en or SUIT_EN.get(cn, cn))
    return out


def band_of(realm):
    return 1 if realm <= 3 else (2 if realm <= 6 else 3)


def main():
    bands = read_bands()
    zh = json.load(open(os.path.join(RES, "lang", "zh_cn.json"), encoding="utf-8"))
    en = json.load(open(os.path.join(RES, "lang", "en_us.json"), encoding="utf-8"))
    models = 0
    # 清掉旧编号（<career>_1|2|3）的 lang
    for career in CAREERS:
        for band in (1, 2, 3):
            for slot in SLOTS:
                for table in (zh, en):
                    table.pop("item.hongmeng.career_armor_%s_%d_%s" % (career, band, slot),
                              None)
    for career in CAREERS:
        for realm in range(1, 11):
            band = band_of(realm)
            suit_cn, suit_en = bands[(career, band)]
            for slot_i, slot in enumerate(SLOTS):
                item = "career_armor_%s_%d_%s" % (career, realm, slot)
                mp = os.path.join(RES, "models", "item", item + ".json")
                with open(mp, "w", encoding="utf-8") as f:
                    json.dump({"parent": "minecraft:item/generated",
                               "textures": {"layer0": "hongmeng:item/career_armor_%s_%d_%s"
                                            % (career, band, slot)}},
                              f, ensure_ascii=False, indent=2)
                models += 1
                key = "item.hongmeng." + item
                zh[key] = "%s·%s·%s" % (REALM_CN[realm - 1], suit_cn, SLOT_CN[slot_i])
                en[key] = "%s - %s %s" % (REALM_EN[realm - 1], suit_en, SLOT_EN[slot_i])
    json.dump(zh, open(os.path.join(RES, "lang", "zh_cn.json"), "w",
                       encoding="utf-8"), ensure_ascii=False, indent=2)
    json.dump(en, open(os.path.join(RES, "lang", "en_us.json"), "w",
                       encoding="utf-8"), ensure_ascii=False, indent=2)
    print("realm armor item models:", models,
          "| lang keys:", sum(1 for k in zh if "career_armor_" in k))


if __name__ == "__main__":
    main()
