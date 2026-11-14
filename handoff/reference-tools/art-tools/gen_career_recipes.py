# -*- coding: utf-8 -*-
"""
为 C 扩充后的 120 件装备生成独立配方（60 武器 + 60 甲）
=========================================================
- 每件武器一个独立配方；同型 1→10 阶可逐级合成（最终到鸿蒙阶）。
- 每件甲一个独立配方；同职业同部位 初阶→进阶→名器 可逐级合成。
- 全部 shapeless，材料只用本模组已有物品（spirit_iron_ingot / lingye /
  spirit_crystal / zhenyuan_crystal / kongming_essence / xueluo_essence /
  puppet_core / beast_heart / blood_core / void_eye / 原版 stick、leather）。
"""
import json
import os

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
RECIPES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "data",
                       "hongmeng", "recipes")
os.makedirs(RECIPES, exist_ok=True)

FORMS = ["sword", "greatsword", "scythe", "spear", "staff", "war_scythe"]
CAREER_CORE = {"jian": "puppet_core", "xue": "blood_core", "mo": "beast_heart",
               "gui": "xueluo_essence", "ti": "beast_heart"}
SLOTS = ["helmet", "chestplate", "leggings", "boots"]

# 武器 1→10 阶升级材料（除上一层武器外的附加材料）
UPGRADE = {
    2: [("spirit_iron_ingot", 4), ("lingye", 2)],
    3: [("spirit_iron_ingot", 4), ("spirit_crystal", 1)],
    4: [("spirit_crystal", 3), ("zhenyuan_crystal", 1)],
    5: [("spirit_crystal", 4), ("zhenyuan_crystal", 2)],
    6: [("zhenyuan_crystal", 4), ("kongming_essence", 1)],
    7: [("kongming_essence", 3), ("zhenyuan_crystal", 2)],
    8: [("kongming_essence", 3), ("xueluo_essence", 1), ("__CORE__", 1)],
    9: [("__CORE__", 2), ("kongming_essence", 4)],
    10: [("__CORE__", 3), ("kongming_essence", 4), ("xueluo_essence", 2)],
}


def ing(item, count=1):
    name = ("minecraft:" + item) if item in ("stick", "leather") \
        else ("hongmeng:" + item)
    entry = {"item": name}
    if count > 1:
        entry["count"] = count
    return entry


def write_recipe(name, ingredients, result, count=1):
    data = {
        "type": "minecraft:crafting_shapeless",
        "category": "equipment",
        "ingredients": ingredients,
        "result": {"item": "hongmeng:" + result, "count": count},
    }
    path = os.path.join(RECIPES, name + ".json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def main():
    n = 0
    # ---------------- 武器：每型一条 1→10 阶升级链 ----------------
    for form in FORMS:
        core = {"sword": "puppet_core", "greatsword": "beast_heart",
                "scythe": "blood_core", "spear": "beast_heart",
                "staff": "void_eye", "war_scythe": "void_eye"}[form]
        first_id = "career_weapon_%s_1" % form
        write_recipe("career_weapon_%s_1" % form,
                     [ing("spirit_iron_ingot", 3), ing("lingye", 1),
                      ing("stick", 1)], first_id)
        n += 1
        for tier in range(2, 11):
            ings = [ing("career_weapon_%s_%d" % (form, tier - 1))]
            for mat, cnt in UPGRADE[tier]:
                m = core if mat == "__CORE__" else mat
                ings.append(ing(m, cnt))
            rid = "career_weapon_%s_%d" % (form, tier)
            write_recipe(rid, ings, rid)
            n += 1

    # ---------------- 甲：每职业 × 每部位 × 10 境界升级链 ----------------
    ARMOR_UP = {
        2: [("spirit_iron_ingot", 4), ("lingye", 3)],
        3: [("spirit_iron_ingot", 4), ("lingye", 3), ("spirit_crystal", 1)],
        4: [("spirit_crystal", 2), ("zhenyuan_crystal", 1)],
        5: [("spirit_crystal", 3), ("zhenyuan_crystal", 2)],
        6: [("spirit_crystal", 4), ("zhenyuan_crystal", 2), ("kongming_essence", 1)],
        7: [("kongming_essence", 2), ("zhenyuan_crystal", 2)],
        8: [("kongming_essence", 3), ("xueluo_essence", 1), ("__CORE__", 1)],
        9: [("__CORE__", 2), ("kongming_essence", 3)],
        10: [("__CORE__", 3), ("kongming_essence", 4), ("xueluo_essence", 2)],
    }
    for cid in ("jian", "xue", "mo", "gui", "ti"):
        core = CAREER_CORE[cid]
        for slot in SLOTS:
            first = "career_armor_%s_1_%s" % (cid, slot)
            write_recipe(first, [ing("spirit_iron_ingot", 4), ing("lingye", 2),
                                ing("leather", 2)], first)
            n += 1
            for realm in range(2, 11):
                ings = [ing("career_armor_%s_%d_%s" % (cid, realm - 1, slot))]
                for mat, cnt in ARMOR_UP[realm]:
                    m = core if mat == "__CORE__" else mat
                    ings.append(ing(m, cnt))
                rid = "career_armor_%s_%d_%s" % (cid, realm, slot)
                write_recipe(rid, ings, rid)
                n += 1
    print("recipes written:", n, "->", RECIPES)


if __name__ == "__main__":
    main()
