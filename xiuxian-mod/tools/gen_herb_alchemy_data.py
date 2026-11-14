#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
鸿蒙修仙 —— 灵植 / 炼丹的数据文件生成器
=======================================

一次写出（全部 JSON）：

    data/hongmeng/loot_tables/blocks/<herb>.json            掉落表（种子 + 成熟花瓣）
    data/hongmeng/loot_tables/blocks/lingtian_soil.json     灵田土掉落自己
    data/hongmeng/loot_tables/blocks/alchemy_furnace.json   丹炉掉落自己
    data/hongmeng/worldgen/configured_feature/<herb>.json   野生成簇（random_patch）
    data/hongmeng/worldgen/placed_feature/<herb>.json       地表放置（WORLD_SURFACE_WG）
    data/hongmeng/forge/biome_modifier/add_<herb>.json      挂到生物群系
    data/hongmeng/recipes/lingtian_soil.json                9 泥土 + 1 灵叶 → 3 灵田土
    data/hongmeng/recipes/alchemy_furnace.json              灵铁锭 + 炼药锅 + 烈焰粉 + 灵叶
    data/hongmeng/recipes/<herb>_seed.json                  小麦种子 + 灵叶 → 2 种子（保底）
    data/hongmeng/recipes/<herb>_seed_from_<herb>_petal.json 花瓣 → 种子（自续种）

**为什么用脚本生成而不是手写 40 个 JSON**：手写一个逗号错位就是
"服务端直接起不来"（这个仓库踩过：data/** 里的坏 JSON 会让 SERVER FAIL TO START）。
脚本里 json.dump 保证语法一定合法，而且世界生成的数值（成簇次数、稀有度）
集中在一张表里，改起来不用翻 18 个文件。生成后由
``tools/check_herb_alchemy.py`` 再逐个 json.loads 校验一遍。

用法::

    python xiuxian-mod/tools/gen_herb_alchemy_data.py
    python xiuxian-mod/tools/gen_herb_alchemy_data.py --check   # 只校验现有文件
"""

from __future__ import annotations

import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
DATA = os.path.join(MOD_ROOT, "src", "main", "resources", "data", "hongmeng")

MOD_ID = "hongmeng"

# ---------------------------------------------------------------------------
# 六阶灵植（与 gen_herb_alchemy_textures.py、ModBlocks/ModItems 三处保持一致）
#   world: 野生生成参数
#     biome  生物群系标签（只用原版确实存在的标签：#minecraft:is_overworld /
#            is_taiga / is_mountain —— 写错标签名不会崩，但会一株都不长）
#     count  每区块的成簇次数      tries 每簇尝试次数
#     chance rarity_filter 的 1/N（None = 不额外稀释）
# ---------------------------------------------------------------------------

HERBS = [
    dict(id="xuelian_grass", cn="雪莲草", tier=1,
         biome="#minecraft:is_overworld", count=8, tries=24, chance=None),
    dict(id="huoyan_hua", cn="火焰花", tier=2,
         biome="#minecraft:is_overworld", count=5, tries=20, chance=None),
    dict(id="hanbing_teng", cn="寒冰藤", tier=3,
         biome="#minecraft:is_taiga", count=6, tries=20, chance=None),
    dict(id="leiyin_cao", cn="雷音草", tier=4,
         biome="#minecraft:is_mountain", count=5, tries=18, chance=None),
    dict(id="kongming_lan", cn="空冥兰", tier=5,
         biome="#minecraft:is_overworld", count=3, tries=12, chance=4),
    dict(id="xueluo_zhi", cn="血珞芝", tier=6,
         biome="#minecraft:is_overworld", count=2, tries=8, chance=12),
]


def petal_max(tier: int) -> int:
    """成熟时一次掉多少花瓣（上限）。一阶 2 片，六阶 4 片。"""
    return 1 + (tier + 1) // 2


# ---------------------------------------------------------------------------
# 掉落表
# ---------------------------------------------------------------------------

def herb_loot(herb) -> dict:
    herb_id = herb["id"]
    return {
        "type": "minecraft:block",
        "pools": [
            {
                # 种子：无论长没长熟都掉 1 颗，保证"种下去不会亏"
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"{MOD_ID}:{herb_id}_seed"}],
                "functions": [{"function": "minecraft:explosion_decay"}],
            },
            {
                # 花瓣：只有 age=7（长熟）才掉，品阶越高掉得越多
                "rolls": 1,
                "entries": [{
                    "type": "minecraft:item",
                    "name": f"{MOD_ID}:{herb_id}_petal",
                    "functions": [
                        {"function": "minecraft:set_count",
                         "count": {"type": "minecraft:uniform", "min": 1.0,
                                   "max": float(petal_max(herb["tier"]))}},
                        {"function": "minecraft:apply_bonus",
                         "enchantment": "minecraft:fortune",
                         "formula": "minecraft:uniform_bonus_count",
                         "parameters": {"bonusMultiplier": 1}},
                    ],
                }],
                "conditions": [{
                    "condition": "minecraft:block_state_property",
                    "block": f"{MOD_ID}:{herb_id}",
                    "properties": {"age": "7"},
                }],
                "functions": [{"function": "minecraft:explosion_decay"}],
            },
        ],
    }


def self_loot(block_id: str) -> dict:
    """普通方块：炸不掉、掉自己。"""
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{MOD_ID}:{block_id}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


# ---------------------------------------------------------------------------
# 世界生成
# ---------------------------------------------------------------------------

def configured_feature(herb) -> dict:
    """random_patch 里塞一株 simple_block —— 原版花草用的就是这套。

    ``matching_blocks: minecraft:air`` 保证只长在空气里；
    ``SimpleBlockFeature`` 自己会再问一次 ``canSurvive``，
    所以长在石头/水里都不会硬塞（灵植的 mayPlaceOn 只认泥土/草/沙/耕地/灵田土）。
    """
    herb_id = herb["id"]
    return {
        "type": "minecraft:random_patch",
        "config": {
            "feature": {
                "feature": {
                    "type": "minecraft:simple_block",
                    "config": {
                        "to_place": {
                            "type": "minecraft:simple_state_provider",
                            "state": {"Name": f"{MOD_ID}:{herb_id}",
                                      "Properties": {"age": "0"}},
                        }
                    },
                },
                "placement": [{
                    "type": "minecraft:block_predicate_filter",
                    "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                }],
            },
            "tries": herb["tries"],
            "xz_spread": 6,
            "y_spread": 3,
        },
    }


def placed_feature(herb) -> dict:
    placement = []
    if herb["chance"]:
        placement.append({"type": "minecraft:rarity_filter", "chance": herb["chance"]})
    placement += [
        {"type": "minecraft:count", "count": herb["count"]},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:biome"},
    ]
    return {"feature": f"{MOD_ID}:{herb['id']}", "placement": placement}


def biome_modifier(herb) -> dict:
    return {
        "type": "forge:add_features",
        "biomes": herb["biome"],
        "features": f"{MOD_ID}:{herb['id']}",
        "step": "vegetal_decoration",
    }


# ---------------------------------------------------------------------------
# 合成表（炼丹的"开店本钱"：种子能自己做，不必先野外找到）
# ---------------------------------------------------------------------------

def recipe_lingtian_soil() -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": ["DDD", "DLD", "DDD"],
        "key": {
            "D": {"item": "minecraft:dirt"},
            "L": {"item": f"{MOD_ID}:lingye"},
        },
        "result": {"item": f"{MOD_ID}:lingtian_soil", "count": 3},
    }


def recipe_alchemy_furnace() -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": ["ISI", "ICI", "IBI"],
        "key": {
            "I": {"item": f"{MOD_ID}:spirit_iron_ingot"},
            "S": {"item": f"{MOD_ID}:lingye"},
            "C": {"item": "minecraft:cauldron"},
            "B": {"item": "minecraft:blaze_powder"},
        },
        "result": {"item": f"{MOD_ID}:alchemy_furnace", "count": 1},
    }


def recipe_seed(herb) -> dict:
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "minecraft:wheat_seeds"},
            {"item": f"{MOD_ID}:lingye"},
        ],
        "result": {"item": f"{MOD_ID}:{herb['id']}_seed", "count": 2},
    }


def recipe_seed_from_petal(herb) -> dict:
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": f"{MOD_ID}:{herb['id']}_petal"}],
        "result": {"item": f"{MOD_ID}:{herb['id']}_seed", "count": 1},
    }


# ---------------------------------------------------------------------------
# 落盘
# ---------------------------------------------------------------------------

def all_files():
    """返回 [(相对 data/hongmeng 的路径, JSON 对象)]。"""
    out = []
    for herb in HERBS:
        out.append((f"loot_tables/blocks/{herb['id']}.json", herb_loot(herb)))
        out.append((f"worldgen/configured_feature/{herb['id']}.json", configured_feature(herb)))
        out.append((f"worldgen/placed_feature/{herb['id']}.json", placed_feature(herb)))
        out.append((f"forge/biome_modifier/add_{herb['id']}.json", biome_modifier(herb)))
        out.append((f"recipes/{herb['id']}_seed.json", recipe_seed(herb)))
        out.append((f"recipes/{herb['id']}_seed_from_{herb['id']}_petal.json",
                    recipe_seed_from_petal(herb)))
    out.append(("loot_tables/blocks/lingtian_soil.json", self_loot("lingtian_soil")))
    out.append(("loot_tables/blocks/alchemy_furnace.json", self_loot("alchemy_furnace")))
    out.append(("recipes/lingtian_soil.json", recipe_lingtian_soil()))
    out.append(("recipes/alchemy_furnace.json", recipe_alchemy_furnace()))
    return out


def write_all():
    written = []
    for rel, data in all_files():
        path = os.path.join(DATA, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(data, fh, ensure_ascii=False, indent=2)
            fh.write("\n")
        written.append(path)
    return written


def check_all() -> int:
    bad = 0
    for rel, data in all_files():
        path = os.path.join(DATA, *rel.split("/"))
        if not os.path.exists(path):
            print(f"  ! 缺少 {rel}")
            bad += 1
            continue
        try:
            with open(path, "r", encoding="utf-8") as fh:
                on_disk = json.load(fh)
        except Exception as exc:
            print(f"  ! 坏 JSON {rel}: {exc}")
            bad += 1
            continue
        if on_disk != data:
            print(f"  ! 内容与生成器不一致（跑一次生成器即可修正）：{rel}")
            bad += 1
    print(f"  共 {len(all_files())} 个文件，问题 {bad} 个")
    return bad


def main() -> int:
    ap = argparse.ArgumentParser(description="生成灵植/炼丹的数据文件")
    ap.add_argument("--check", action="store_true", help="只校验现有文件（不写入）")
    args = ap.parse_args()

    if args.check:
        print("== 校验 data/ 下的灵植炼丹文件 ==")
        bad = check_all()
        return 1 if bad else 0

    written = write_all()
    print(f"== 写入 {len(written)} 个文件 ==")
    for p in written:
        print("  ->", os.path.relpath(p, MOD_ROOT))
    print()
    print("== 校验 ==")
    bad = check_all()
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
