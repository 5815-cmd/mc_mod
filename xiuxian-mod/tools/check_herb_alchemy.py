#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
鸿蒙修仙 —— 灵植 / 炼丹 离线自检
=================================

**这就是"丹方表要响亮地失败"的那道闸。** 它不碰注册表（不加载任何 Minecraft 类），
所以能在没有游戏的情况下跑；同时又**真的把编译产物里的丹方表跑一遍**
（`java tools/AlchemySelfCheck.java` 调 `AlchemyRecipes.validate()`），
不是"照着源码再猜一遍"。

检查范围：

  A. 丹方表（Java 侧真跑）：表合法 / 九颗丹药全覆盖 / 无重复组合 / 每颗丹药可达
  B. ModItems 交叉验证：item/ 下**所有**丹药物品（按药品类识别，不是按名字）
     都必须在表里有方子；表里的材料必须都是已注册物品；原版材料在白名单内
  C. 灵植资源：6 株 × (blockstate 8 个 age / 4 张方块模型 / 4 张阶段贴图 /
     种子+花瓣贴图 / 掉落表 / 3 个世界生成文件)
  D. 丹炉与灵田土：blockstate / 模型 / 贴图 / 掉落表 / 配方 / 界面接线
  E. 数据完整性：本功能涉及的每个 data/**.json 都能 json.loads（坏 JSON 会让服务端起不来）
  F. 语言文件：zh_cn 与 en_us **双语都有**（方块名 / 种子 / 花瓣 / 容器与界面提示）
  G. 约束自查：SpiritHerbBlock 的构造函数里没有 .get()（构造期解引用注册对象会崩加载）

用法::

    python xiuxian-mod/tools/check_herb_alchemy.py
    python xiuxian-mod/tools/check_herb_alchemy.py --java-home <jdk/bin>

退出码：0 = 全部通过；1 = 有失败项（失败项会逐条打印）。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("需要 Pillow：pip install pillow")

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
WS = os.path.dirname(MOD_ROOT)

JAVA = os.path.join(MOD_ROOT, "src", "main", "java")
RES = os.path.join(MOD_ROOT, "src", "main", "resources", "assets", "hongmeng")
DATA = os.path.join(MOD_ROOT, "src", "main", "resources", "data", "hongmeng")
MODOUT = os.path.join(WS, "_cache", "modout")

MOD_ITEMS = os.path.join(JAVA, "com", "hongmeng", "xiuxian", "item", "ModItems.java")
MOD_BLOCKS = os.path.join(JAVA, "com", "hongmeng", "xiuxian", "ModBlocks.java")
MOD_TABS = os.path.join(JAVA, "com", "hongmeng", "xiuxian", "item", "ModCreativeTabs.java")
MOD_MAIN = os.path.join(JAVA, "com", "hongmeng", "xiuxian", "HongmengMod.java")
RECIPES_JAVA = os.path.join(JAVA, "com", "hongmeng", "xiuxian", "alchemy", "AlchemyRecipes.java")
HERB_BLOCK_JAVA = os.path.join(JAVA, "com", "hongmeng", "xiuxian", "block", "SpiritHerbBlock.java")
SELF_CHECK_JAVA = os.path.join(HERE, "AlchemySelfCheck.java")

HERBS = ["xuelian_grass", "huoyan_hua", "hanbing_teng", "leiyin_cao",
         "kongming_lan", "xueluo_zhi"]
HERB_TIER = {"xuelian_grass": 1, "huoyan_hua": 2, "hanbing_teng": 3,
             "leiyin_cao": 4, "kongming_lan": 5, "xueluo_zhi": 6}

#: 丹药物品的实现类：**按类识别**而不是按名字，这样以后新增丹药也能被这道闸拦住
PILL_CLASSES = {"CultivationPillItem", "BreakthroughPillItem", "RerollRootItem",
                "ManaPillItem", "ZhaohuaPillItem", "DualDomainPillItem",
                "ExperiencePillItem"}

#: 本功能配方里用到的原版物品（离线没有原版注册表，用白名单兜住拼写错误）
VANILLA_ITEMS = {"minecraft:dirt", "minecraft:wheat_seeds", "minecraft:cauldron",
                 "minecraft:blaze_powder", "minecraft:wheat",
                 "minecraft:glowstone_dust", "minecraft:amethyst_shard",
                 "minecraft:air"}

RESULTS = []


def check(what, ok, detail=""):
    RESULTS.append((what, bool(ok), detail))
    return bool(ok)


def strip_comments(text):
    """去掉 // 行注释与 /* */ 块注释。

    必须去：自查里"构造函数不许出现 .get()""配方表不许出现 ItemStack"
    这类断言会被**注释里解释这条规矩的话**误伤（写文档的注释里正好就写着 .get()）。
    """
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def read(path):
    with open(path, "r", encoding="utf-8") as fh:
        return fh.read()


def load_json(path):
    with open(path, "r", encoding="utf-8-sig") as fh:
        return json.load(fh)


def registered_items():
    """ModItems.java 里注册的全部物品名（ITEMS.register("x") 与 simple("x")）。"""
    text = read(MOD_ITEMS)
    names = set(re.findall(r'ITEMS\.register\(\s*"([^"]+)"', text))
    names |= set(re.findall(r'\bsimple\(\s*"([^"]+)"', text))
    return names


def registered_blocks():
    text = read(MOD_BLOCKS)
    return set(re.findall(r'BLOCKS\.register\(\s*"([^"]+)"', text))


def pill_items():
    """按"实现类是丹药类"识别丹药物品。"""
    text = read(MOD_ITEMS)
    found = {}
    for m in re.finditer(r'ITEMS\.register\(\s*"([^"]+)"\s*,\s*\(\)\s*->\s*new\s+'
                         r'(?:[\w.]*\.)?(\w+)\(', text):
        if m.group(2) in PILL_CLASSES:
            found[m.group(1)] = m.group(2)
    return found


def run_java_self_check(java_home):
    """真的把编译好的丹方表跑一遍。"""
    java = os.path.join(java_home, "bin", "java.exe")
    if not os.path.exists(java):
        java = os.path.join(java_home, "bin", "java")
    if not os.path.exists(java):
        return None, f"找不到 java：{java}"
    cmd = [java, "-Dfile.encoding=UTF-8", "-cp", MODOUT, SELF_CHECK_JAVA]
    try:
        proc = subprocess.run(cmd, capture_output=True, timeout=180)
    except Exception as exc:  # pragma: no cover
        return None, f"运行失败：{exc}"
    out = proc.stdout.decode("utf-8", "replace")
    err = proc.stderr.decode("utf-8", "replace")
    if proc.returncode != 0:
        return None, f"退出码 {proc.returncode}\n{err.strip()[:800]}"
    return out, ""


# ---------------------------------------------------------------------------
# A. 丹方表
# ---------------------------------------------------------------------------

def section_recipes(java_home):
    out, err = run_java_self_check(java_home)
    if not check("丹方表：能离线跑起来（AlchemyRecipes.validate()）", out is not None,
                 err or "java -cp _cache/modout tools/AlchemySelfCheck.java"):
        return None

    header = next((l for l in out.splitlines() if l.startswith("ALCHEMY-TABLE-OK|")), "")
    stats = dict(kv.split("=", 1) for kv in header.split("|")[1:] if "=" in kv)
    brews = []
    for line in out.splitlines():
        if line.startswith("BREW|"):
            p = line.split("|")
            brews.append(dict(main=p[1], aux=p[2], result=p[3], count=int(p[4]),
                              tier=int(p[5]), ticks=int(p[6]), note=p[7] if len(p) > 7 else ""))
    pills = [l.split("|")[1] for l in out.splitlines() if l.startswith("PILL|")]
    unreachable = [l.split("|")[1] for l in out.splitlines()
                   if l.startswith("PILL|") and l.endswith("UNREACHABLE")]
    fuels = {}
    for line in out.splitlines():
        if line.startswith("FUEL|"):
            p = line.split("|")
            fuels = {"leaf": p[1], "leaf_ticks": int(p[2]),
                     "blaze": p[3], "blaze_ticks": int(p[4])}

    check("丹方表：非空且每条都合法（主料/辅料/产物/数量/品阶）", bool(brews) and bool(stats),
          f"{len(brews)} 条丹方")
    check("丹方表：没有不可达的丹药（PILL_IDS 全部有方子）", not unreachable,
          "缺方子的丹药：" + ", ".join(unreachable) if unreachable else "9/9 可达")
    check("丹方表：炼制时间统一 200 tick", stats.get("brewTicks") == "200",
          f"brewTicks={stats.get('brewTicks')}")

    # ---- 灵植表（HerbCatalog，同样是纯数据，离线可跑）----
    herbs = []
    for line in out.splitlines():
        if line.startswith("HERB|"):
            p = line.split("|")
            herbs.append(dict(id=p[1], cn=p[2], tier=int(p[3]), use=p[4],
                              growth=float(p[5]), petal_max=int(p[6]),
                              seed=p[7], petal=p[8]))
    tiers = sorted(h["tier"] for h in herbs)
    check("灵植表：6 株、品阶正好 1..6 各一个", tiers == [1, 2, 3, 4, 5, 6], str(tiers))
    check("灵植表：注册名与调色板表一致",
          sorted(h["id"] for h in herbs) == sorted(HERBS),
          str(sorted(h["id"] for h in herbs)))
    check("灵植表：生长概率都在 (0,1]（灵田土再 ×3，封顶 1.0）",
          all(0.0 < h["growth"] <= 1.0 for h in herbs),
          str({h["id"]: h["growth"] for h in herbs}))
    check("灵植表：高阶不会比低阶长得快（品阶越高越慢）",
          all(herbs[i]["growth"] >= herbs[i + 1]["growth"] for i in range(len(herbs) - 1)),
          str([h["growth"] for h in herbs]))

    # 组合唯一（含主辅互换）
    seen, dup = set(), []
    for b in brews:
        key = tuple(sorted([b["main"], b["aux"]]))
        if key in seen:
            dup.append(f"{b['main']}+{b['aux']}")
        seen.add(key)
    check("丹方表：没有重复组合（主辅互换也算重复）", not dup, str(dup))

    # 每一阶灵植都当过主料
    used_mains = {b["main"] for b in brews}
    missing_main = [h for h in HERBS if f"{h}_petal" not in used_mains]
    check("丹方表：六阶灵植各自都当过主料", not missing_main,
          "没当过主料的：" + ", ".join(missing_main) if missing_main else "6/6")

    # 燃料
    check("燃料：灵叶 1 片 = 200 tick（正好一炉）",
          fuels.get("leaf") == "lingye" and fuels.get("leaf_ticks") == 200, str(fuels))
    check("燃料：烈焰粉 1 份 = 2400 tick（12 炉）",
          fuels.get("blaze") == "minecraft:blaze_powder" and fuels.get("blaze_ticks") == 2400,
          str(fuels))

    return dict(brews=brews, pills=pills, fuels=fuels, herbs=herbs)


# ---------------------------------------------------------------------------
# B. 与 ModItems 交叉验证
# ---------------------------------------------------------------------------

def section_items(table):
    items = registered_items()
    pills = pill_items()
    check("ModItems：能解析出丹药物品（按实现类识别）", len(pills) >= 9,
          f"识别到 {len(pills)} 颗：" + ", ".join(sorted(pills)))

    if table:
        covered = {b["result"] for b in table["brews"]}
        missing = sorted(set(pills) - covered)
        check("覆盖：item/ 下每一颗丹药都有丹方（以后新增丹药也会被拦）", not missing,
              "没有丹方的丹药：" + ", ".join(missing) if missing else
              f"{len(pills)}/{len(pills)} 颗可炼")

        declared = set(table["pills"])
        check("AlchemyRecipes.PILL_IDS 与 ModItems 实际丹药集合一致",
              declared == set(pills),
              f"只在表里：{sorted(declared - set(pills))}；只在 ModItems：{sorted(set(pills) - declared)}")

        bad_ing = []
        unknown_ns = []
        for b in table["brews"]:
            for ing in (b["main"], b["aux"]):
                if ing.startswith("minecraft:"):
                    if ing not in VANILLA_ITEMS:
                        unknown_ns.append(ing)
                elif ing not in items:
                    bad_ing.append(ing)
            if b["result"] not in items:
                bad_ing.append(b["result"])
        check("材料：丹方里每个 hongmeng 材料/产物都是已注册物品", not bad_ing, str(sorted(set(bad_ing))))
        check("材料：丹方里用到的原版物品都在白名单内（防拼写错误）", not unknown_ns,
              str(sorted(set(unknown_ns))))

        # 灵植表里的种子/花瓣 id 也必须是已注册物品
        herb_bad = []
        for h in table["herbs"]:
            for item_name in (h["seed"], h["petal"]):
                if item_name not in items:
                    herb_bad.append(item_name)
            if h["id"] not in registered_blocks():
                herb_bad.append(h["id"] + "（方块）")
        check("灵植表：每株的种子/花瓣/方块都已注册", not herb_bad, str(herb_bad))


# ---------------------------------------------------------------------------
# C/D/E/F/G. 资源与数据
# ---------------------------------------------------------------------------

def section_herb_assets():
    problems = []
    for herb in HERBS:
        bs = os.path.join(RES, "blockstates", f"{herb}.json")
        if not os.path.exists(bs):
            problems.append(f"缺 blockstate {herb}.json")
        else:
            data = load_json(bs)
            variants = data.get("variants", {})
            for age in range(8):
                key = f"age={age}"
                if key not in variants:
                    problems.append(f"{herb}.json 缺 {key}")
                    continue
                model = variants[key]["model"].split(":", 1)[1]
                mpath = os.path.join(RES, "models", *model.split("/")) + ".json"
                if not os.path.exists(mpath):
                    problems.append(f"{herb} {key} -> 模型不存在 {model}")
        for stage in range(4):
            tex = os.path.join(RES, "textures", "block", f"{herb}_stage{stage}.png")
            if not os.path.exists(tex):
                problems.append(f"缺贴图 {herb}_stage{stage}.png")
        for kind in ("seed", "petal"):
            tex = os.path.join(RES, "textures", "item", f"{herb}_{kind}.png")
            if not os.path.exists(tex):
                problems.append(f"缺贴图 {herb}_{kind}.png")
            model = os.path.join(RES, "models", "item", f"{herb}_{kind}.json")
            if not os.path.exists(model):
                problems.append(f"缺物品模型 {herb}_{kind}.json")
        if not os.path.exists(os.path.join(RES, "models", "item", f"{herb}.json")):
            problems.append(f"缺物品模型 {herb}.json")
        if not os.path.exists(os.path.join(DATA, "loot_tables", "blocks", f"{herb}.json")):
            problems.append(f"缺掉落表 {herb}.json")
        for rel in (f"worldgen/configured_feature/{herb}.json",
                    f"worldgen/placed_feature/{herb}.json",
                    f"forge/biome_modifier/add_{herb}.json"):
            if not os.path.exists(os.path.join(DATA, *rel.split("/"))):
                problems.append(f"缺世界生成文件 {rel}")
    check("灵植资源：6 株 × (blockstate/模型/贴图/掉落表/世界生成) 全部落盘", not problems,
          "; ".join(problems[:8]) if problems else "6 株齐备")

    # 贴图规格（缺一张客户端就会崩）
    bad_tex = []
    for herb in HERBS:
        for stage in range(4):
            p = os.path.join(RES, "textures", "block", f"{herb}_stage{stage}.png")
            if not os.path.exists(p):
                continue
            im = Image.open(p)
            alpha = im.convert("RGBA").getchannel("A")
            opaque = sum(alpha.histogram()[1:])
            if im.size != (16, 16) or im.mode != "RGBA" or opaque <= 24:
                bad_tex.append(f"{herb}_stage{stage}={im.size}/{im.mode}/{opaque}px")
    for name in [f"{h}_{k}" for h in HERBS for k in ("seed", "petal")] + \
                ["lingtian_soil", "alchemy_furnace_side", "alchemy_furnace_top",
                 "alchemy_furnace_front", "alchemy_furnace_front_lit"]:
        folder = "item" if name.endswith(("_seed", "_petal")) else "block"
        p = os.path.join(RES, "textures", folder, name + ".png")
        if not os.path.exists(p):
            bad_tex.append(f"缺 {name}.png")
            continue
        im = Image.open(p)
        alpha = im.convert("RGBA").getchannel("A")
        opaque = sum(alpha.histogram()[1:])
        if im.size != (16, 16) or im.mode != "RGBA" or opaque <= 24:
            bad_tex.append(f"{name}={im.size}/{im.mode}/{opaque}px")
        if folder == "block" and opaque != 256:
            bad_tex.append(f"{name} 方块贴图有透明像素（{opaque}/256）")
    check("贴图：全部 16×16 RGBA、非空白；方块贴图完全不透明", not bad_tex,
          "; ".join(bad_tex[:8]) if bad_tex else "41 张全部合格")


def section_furnace():
    problems = []
    for rel in ("blockstates/lingtian_soil.json", "blockstates/alchemy_furnace.json",
                "models/block/lingtian_soil.json", "models/block/alchemy_furnace.json",
                "models/block/alchemy_furnace_lit.json",
                "models/item/lingtian_soil.json", "models/item/alchemy_furnace.json"):
        if not os.path.exists(os.path.join(RES, *rel.split("/"))):
            problems.append(f"缺 {rel}")
    bs_path = os.path.join(RES, "blockstates", "alchemy_furnace.json")
    if os.path.exists(bs_path):
        variants = load_json(bs_path).get("variants", {})
        for facing in ("north", "east", "south", "west"):
            for lit in ("true", "false"):
                key = f"facing={facing},lit={lit}"
                if key not in variants:
                    problems.append(f"alchemy_furnace.json 缺 {key}")
    for rel in ("loot_tables/blocks/lingtian_soil.json", "loot_tables/blocks/alchemy_furnace.json",
                "recipes/lingtian_soil.json", "recipes/alchemy_furnace.json"):
        if not os.path.exists(os.path.join(DATA, *rel.split("/"))):
            problems.append(f"缺 {rel}")
    check("丹炉/灵田土：blockstate（含 facing×lit）、模型、贴图、掉落表、配方齐全",
          not problems, "; ".join(problems) if problems else "齐备")

    # Java 接线
    blocks = registered_blocks()
    items = registered_items()
    wiring = []
    if "alchemy_furnace" not in blocks:
        wiring.append("ModBlocks 没注册 alchemy_furnace")
    if "lingtian_soil" not in blocks:
        wiring.append("ModBlocks 没注册 lingtian_soil")
    for herb in HERBS:
        if herb not in blocks:
            wiring.append(f"ModBlocks 没注册 {herb}")
    for name in ("alchemy_furnace", "lingtian_soil") + tuple(HERBS) + \
                tuple(f"{h}_{k}" for h in HERBS for k in ("seed", "petal")):
        if name not in items:
            wiring.append(f"ModItems 没注册 {name}")

    tabs = read(MOD_TABS)
    for name in ("ALCHEMY_FURNACE_ITEM", "LINGTIAN_SOIL_ITEM", "XUELIAN_GRASS_PETAL",
                 "XUELUO_ZHI_SEED", "LEIYIN_CAO_ITEM"):
        if name not in tabs:
            wiring.append(f"ModCreativeTabs 没放 {name}")

    main = read(MOD_MAIN)
    if "ModAlchemy.register(modBus)" not in main:
        wiring.append("HongmengMod 没注册 ModAlchemy")
    if "AlchemyClientSetup::onClientSetup" not in main:
        wiring.append("HongmengMod 没挂 AlchemyClientSetup")
    if "FMLEnvironment.dist == Dist.CLIENT" not in main:
        wiring.append("HongmengMod 里没有 dist 判断（客户端类会污染服务端）")
    else:
        # AlchemyClientSetup 必须在 dist 判断之内
        idx_dist = main.index("FMLEnvironment.dist == Dist.CLIENT")
        idx_client = main.index("AlchemyClientSetup::onClientSetup")
        close = main.index("}", idx_dist)
        if not (idx_dist < idx_client < close):
            wiring.append("AlchemyClientSetup 不在 dist==CLIENT 分支内")
    check("接线：ModBlocks/ModItems/ModCreativeTabs/HongmengMod 全部就位", not wiring,
          "; ".join(wiring) if wiring else "6 处接线正常")


def section_data_json(table=None):
    """坏一个 JSON 就能让服务端起不来，所以逐个 json.loads。"""
    files = [
        *(f"loot_tables/blocks/{h}.json" for h in HERBS),
        "loot_tables/blocks/lingtian_soil.json",
        "loot_tables/blocks/alchemy_furnace.json",
        *(f"worldgen/configured_feature/{h}.json" for h in HERBS),
        *(f"worldgen/placed_feature/{h}.json" for h in HERBS),
        *(f"forge/biome_modifier/add_{h}.json" for h in HERBS),
        "recipes/lingtian_soil.json", "recipes/alchemy_furnace.json",
        *(f"recipes/{h}_seed.json" for h in HERBS),
        *(f"recipes/{h}_seed_from_{h}_petal.json" for h in HERBS),
    ]
    bad, parsed = [], {}
    for rel in files:
        path = os.path.join(DATA, *rel.split("/"))
        try:
            parsed[rel] = load_json(path)
        except Exception as exc:
            bad.append(f"{rel}: {exc}")
    check(f"data：本功能 {len(files)} 个 JSON 全部存在且语法合法", not bad,
          "; ".join(bad[:6]) if bad else f"{len(files)}/{len(files)} 合法")

    # 引用链：biome_modifier -> placed_feature -> configured_feature -> 方块
    blocks = registered_blocks()
    items = registered_items()
    chain_bad = []
    for herb in HERBS:
        bm = parsed.get(f"forge/biome_modifier/add_{herb}.json")
        pf = parsed.get(f"worldgen/placed_feature/{herb}.json")
        cf = parsed.get(f"worldgen/configured_feature/{herb}.json")
        if not (bm and pf and cf):
            continue
        if bm.get("features") != f"hongmeng:{herb}":
            chain_bad.append(f"{herb}: biome_modifier.features 指着 {bm.get('features')}")
        if bm.get("step") != "vegetal_decoration":
            chain_bad.append(f"{herb}: step={bm.get('step')}（应为 vegetal_decoration）")
        if not bm.get("biomes", "").startswith("#minecraft:"):
            chain_bad.append(f"{herb}: biomes 不是原版标签 {bm.get('biomes')}")
        if pf.get("feature") != f"hongmeng:{herb}":
            chain_bad.append(f"{herb}: placed_feature 指着 {pf.get('feature')}")
        state = cf["config"]["feature"]["feature"]["config"]["to_place"]["state"]
        name = state["Name"].split(":", 1)[1]
        if name not in blocks:
            chain_bad.append(f"{herb}: configured_feature 里的方块 {name} 未注册")
        if state.get("Properties", {}).get("age") != "0":
            chain_bad.append(f"{herb}: 野生生成没有把 age 定成 0")
    check("worldgen：生物群系 -> 放置 -> 成簇 -> 方块 的引用链完整", not chain_bad,
          "; ".join(chain_bad[:6]) if chain_bad else "6 条链全部闭合")

    # 掉落表引用的物品 / 方块都存在
    herb_by_id = {h["id"]: h for h in (table["herbs"] if table else [])}
    loot_bad = []
    for herb in HERBS:
        loot = parsed.get(f"loot_tables/blocks/{herb}.json")
        if not loot:
            continue
        names = []
        for pool in loot["pools"]:
            for entry in pool["entries"]:
                names.append(entry["name"])
            for cond in pool.get("conditions", []):
                if cond.get("condition") == "minecraft:block_state_property":
                    if cond["block"].split(":", 1)[1] not in blocks:
                        loot_bad.append(f"{herb}: 掉落表条件里的方块 {cond['block']} 未注册")
        for n in names:
            if n.split(":", 1)[1] not in items:
                loot_bad.append(f"{herb}: 掉落 {n} 不是已注册物品")
        if not any(n.endswith("_seed") for n in names):
            loot_bad.append(f"{herb}: 掉落表没有种子（种田会断供）")
        if not any(n.endswith("_petal") for n in names):
            loot_bad.append(f"{herb}: 掉落表没有花瓣（炼丹会断料）")
        # 花瓣上限必须与 HerbCatalog.petalMax() 一致（Java 表 vs 生成出来的 JSON）
        info = herb_by_id.get(herb)
        if info:
            maxes = []
            for pool in loot["pools"]:
                for entry in pool["entries"]:
                    for fn in entry.get("functions", []):
                        if fn.get("function") == "minecraft:set_count":
                            maxes.append(int(fn["count"]["max"]))
            if maxes and maxes[0] != info["petal_max"]:
                loot_bad.append(f"{herb}: 掉落花瓣上限 {maxes[0]} != HerbCatalog.petalMax "
                                f"{info['petal_max']}")
    for block in ("lingtian_soil", "alchemy_furnace"):
        loot = parsed.get(f"loot_tables/blocks/{block}.json")
        if loot and any(e["name"] != f"hongmeng:{block}"
                        for pool in loot["pools"] for e in pool["entries"]):
            loot_bad.append(f"{block}: 掉落表没有掉自己")
    check("掉落表：引用物都存在，且灵植必掉种子+花瓣", not loot_bad,
          "; ".join(loot_bad[:6]) if loot_bad else "8 张掉落表正常")

    # 配方引用的物品
    recipe_bad = []
    for rel, data in parsed.items():
        if not rel.startswith("recipes/"):
            continue
        refs = []
        for key, val in (data.get("key") or {}).items():
            refs.append(val.get("item"))
        for ing in data.get("ingredients", []) or []:
            refs.append(ing.get("item"))
        refs.append((data.get("result") or {}).get("item"))
        for ref in refs:
            if not ref:
                continue
            if ref.startswith("minecraft:"):
                if ref not in VANILLA_ITEMS:
                    recipe_bad.append(f"{rel}: 原版物品不在白名单 {ref}")
            elif ref.split(":", 1)[1] not in items:
                recipe_bad.append(f"{rel}: 物品未注册 {ref}")
    check("配方：引用的物品全部存在（mod 物品查注册表，原版查白名单）", not recipe_bad,
          "; ".join(recipe_bad[:6]) if recipe_bad else "14 个配方正常")


def section_lang():
    zh = load_json(os.path.join(RES, "lang", "zh_cn.json"))
    en = load_json(os.path.join(RES, "lang", "en_us.json"))
    need = []
    for herb in HERBS:
        need.append(f"block.hongmeng.{herb}")
        need.append(f"item.hongmeng.{herb}_seed")
        need.append(f"item.hongmeng.{herb}_petal")
    need += ["block.hongmeng.lingtian_soil", "block.hongmeng.alchemy_furnace",
             "container.hongmeng.alchemy_furnace"]
    need += [f"gui.hongmeng.alchemy.slot.{i}" for i in range(4)]
    need += ["gui.hongmeng.alchemy.hint"]

    missing_zh = [k for k in need if k not in zh]
    missing_en = [k for k in need if k not in en]
    check("lang：zh_cn 有全部灵植/炼丹词条", not missing_zh, str(missing_zh))
    check("lang：en_us 有全部灵植/炼丹词条（两套都要，缺一套就是英文显示 key）",
          not missing_en, str(missing_en))
    diff = set(zh) ^ set(en)
    mine = {k for k in diff if any(t in k for t in
                                   ("xuelian", "huoyan", "hanbing", "leiyin", "kongming",
                                    "xueluo_zhi", "lingtian", "alchemy"))}
    check("lang：本功能的键在 zh_cn / en_us 之间完全对称", not mine, str(sorted(mine)))


def section_constraints():
    text = strip_comments(read(HERB_BLOCK_JAVA))
    ctor = re.search(r"public SpiritHerbBlock\((.*?)\)\s*\{(.*?)\n    \}", text, re.S)
    check("约束：SpiritHerbBlock 能找到构造函数（自查脚本本身没瞎）", ctor is not None)
    if ctor:
        body = ctor.group(2)
        check("约束：SpiritHerbBlock 构造函数里没有调用 .get()（构造期解引用会崩加载）",
              ".get()" not in body, body.strip()[:160])
        check("约束：种子/花瓣以 Supplier 传入", "Supplier<Item>" in ctor.group(1), ctor.group(1))
    recipe_text = strip_comments(read(RECIPES_JAVA))
    mc_types = [t for t in ("net.minecraft.", "ItemStack", "BlockState", "ResourceLocation",
                            "Blocks.", "ModItems")
                if t in recipe_text]
    check("约束：AlchemyRecipes 是纯数据（不引用任何 Minecraft 类型，离线可断言）",
          not mc_types, str(mc_types))
    check("约束：SpiritHerbBlock 没有在 getShape 里解引用 level",
          "getShape" not in text, "未覆盖 getShape（用 CropBlock 的常量形状）")


def main():
    ap = argparse.ArgumentParser(description="灵植/炼丹 离线自检")
    ap.add_argument("--java-home", default=os.path.join(
        os.environ.get("APPDATA", ""), ".minecraft", "runtime", "java-runtime-gamma-snapshot"),
        help="JDK/JRE 目录（需含 bin/java）")
    args = ap.parse_args()

    print("=== A. 丹方表（真的跑编译产物里的 AlchemyRecipes）===")
    table = section_recipes(args.java_home)
    print("=== B. 与 ModItems 交叉验证 ===")
    section_items(table)
    print("=== C. 灵植资源 ===")
    section_herb_assets()
    print("=== D. 丹炉 / 灵田土 ===")
    section_furnace()
    print("=== E. 数据 JSON 与引用链 ===")
    section_data_json(table)
    print("=== F. 语言文件 ===")
    section_lang()
    print("=== G. 约束自查 ===")
    section_constraints()

    failed = [(w, d) for w, ok, d in RESULTS if not ok]
    for what, ok, detail in RESULTS:
        mark = "OK  " if ok else "FAIL"
        print(f"  [{mark}] {what}" + (f"  <- {detail}" if detail and not ok else ""))
    print()
    if table:
        print("灵植表（HerbCatalog）：")
        for h in table["herbs"]:
            print(f"  {h['id']:16s} {h['cn']:6s} {h['tier']}阶  {h['use']:12s}"
                  f" 基础生长={h['growth']:.2f} 灵田={min(1.0, h['growth'] * 3):.2f}"
                  f"  成熟掉花瓣≤{h['petal_max']}")
        print()
        print("丹方表（主料 + 辅料 -> 产物 ×数量，品阶，tick）：")
        for b in table["brews"]:
            print(f"  {b['main']:28s} + {b['aux']:24s} -> {b['result']:20s}"
                  f" ×{b['count']}  {b['tier']}阶 {b['ticks']}t")
    print()
    print(f"通过 {len(RESULTS) - len(failed)} 项，失败 {len(failed)} 项")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
