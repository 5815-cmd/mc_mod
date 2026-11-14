# -*- coding: utf-8 -*-
"""全量角色模型审计（39 个实体注册项）—— 找"模型错位"类硬伤

审计口径（全部离线、逐项给数字）：
1. **UV 越界**（texOffs + 盒尺寸超出贴图）→ 游戏里采样不到 / 糊边；
2. **跨足迹重叠**（两个不同尺寸的盒子抢同一块纹素）→ 后画的覆盖先画的，
   模型上表现为"贴图错位"，而且 MC **不报错**；
3. **透明洞**（模型采样到 alpha=0 的纹素）→ 游戏里被 alpha test 丢弃 = 穿透/黑洞。
   分两类：`hat`/披风/薄片等**有意透明**（原版 hat 层就是靠透明实现），
   其余部件上透明 = 真洞（贴图没画到该面）；
4. **贴图尺寸与 LayerDefinition 不符** → UV 整体缩放错位；
5. **脚底**：模型最低点应落在世界 y=0（脚底像素 24 + 16×GROUND_LIFT）；
6. **挂点断开**：子部件 pivot 离父部件包围盒太远 → "部件散架"；
7. **hitbox 匹配**：模型高/宽 vs 注册 hitbox ÷ 品阶体型倍率，偏差 >25% 报出；
8. **剪影相似度**：64px 剪影两两 IoU，同组内 >0.75 视为"剪影重复度高"。

输出：
* `工程/角色设计/审计/00-基线-全量模型审计.md`（人读表 + 缺陷清单 + 剪影相似表）
* `工程/角色设计/审计/00-基线-全量模型审计.json`（机读，后面改完可 diff）
* `工程/角色设计/previews/baseline/*.png`（每个实体的 before 三视图）
* `工程/角色设计/previews/baseline/_剪影总览.png`（64px 剪影拼图）

用法（Windows 控制台先设 PYTHONIOENCODING=utf-8）：
    python 工程/角色设计/tools/audit_all.py            # 全跑（含渲染）
    python 工程/角色设计/tools/audit_all.py --no-render
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import traceback

TOOLS = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(os.path.dirname(TOOLS)))
sys.path.insert(0, TOOLS)

import charlib as L          # noqa: E402
import renderlib as R        # noqa: E402

CLIENT = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                      "xiuxian", "client")
ENT_JSON = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                        "xiuxian", "entity", "ModEntities.java")
#: 第二轮新增怪物在独立的注册表文件里（避免多人改共享文件），尺寸一起解析
ENT_EXTRA_JSON = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                              "xiuxian", "entity", "ModEntitiesExtra.java")
ENT_EXTRA2_JSON = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                               "xiuxian", "entity", "ModEntitiesExtra2.java")
ENT_EXTRA3_JSON = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                               "xiuxian", "entity", "ModEntitiesExtra3.java")
TEXDIR = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets",
                      "hongmeng", "textures", "entity")
OUTDIR = os.path.join(WS, "工程", "角色设计", "审计")
PREVDIR = os.path.join(WS, "工程", "角色设计", "previews", "baseline")

#: 品阶体型倍率（XZ, Y）—— 与 MobRank 一致
RANK = {"普通": (1.00, 1.00), "精英": (1.05, 1.03), "队长": (1.12, 1.07),
        "头目": (1.20, 1.12), "首领": (1.32, 1.20), "NPC": (1.00, 1.00),
        "灵兽": (1.00, 1.00)}

#: 有意透明的部件关键词（原版 hat 层 / 披风薄片 / 鳍 / 须 ……）
ALLOW_PARTS = ("hat", "cape", "wing", "whisker", "fin", "veil", "banner",
               "flag", "halo", "aura", "glow")

#: (实体 id, 中文, 品阶, 模型 java, GROUND_LIFT 来源 java, 贴图名, 分组, 备注)
#: 设计性"挂点悬空"豁免：实体 id -> {子部件: 说明}。
#: 只有"网格真的连着、只是 pivot 距离大"或"本来就是悬浮体"的才登记；
#: 真断模型（子件完全飞在半空）不许进这张表。
GAP_DESIGN_EXEMPT = {
    # 虚空龙帝：四足 leg pivot 落在脚底线 y=24（世界几何正确，腿从身体下方 12px
    # 处起画），网格实际间隙 3px，只是 pivot 口径看起来大。
    "void_dragon_emperor": {
        "legFL": "四足 leg pivot 落在地面线，网格实际间隙 3px",
        "legFR": "四足 leg pivot 落在地面线，网格实际间隙 3px",
        "legBL": "四足 leg pivot 落在地面线，网格实际间隙 3px",
        "legBR": "四足 leg pivot 落在地面线，网格实际间隙 3px",
    },
    # 恶：天穹隐藏首领，11 格巨颅**刻意悬空**不连躯干（设计稿明确要求）。
    "evil_gazer": {
        "head": "巨颅悬空设计（26px 间隙），不连躯干",
    },
}

ENTITIES = [
    # ---------------- 七个首领 ----------------
    ("epoch_boss", "傀儡祖师", "头目", "PuppetMasterModel.java", "PuppetMasterRenderer.java",
     "boss_puppet_master", "boss", ""),
    ("beast_boss", "万兽之主", "头目", "BeastLordModel.java", "BeastLordRenderer.java",
     "boss_beast_lord", "boss", ""),
    ("demon_boss", "血魔老祖", "头目", "BloodDemonModel.java", "BloodDemonRenderer.java",
     "boss_demon_elder", "boss", ""),
    ("void_boss", "虚空道主", "首领", "VoidLordModel.java", "VoidLordRenderer.java",
     "boss_void_lord", "boss", ""),
    ("blaze_emperor", "焚天炎帝", "头目", "BlazeEmperorModel.java", "BlazeEmperorRenderer.java",
     "blaze_emperor", "boss", ""),
    ("void_dragon_emperor", "虚空龙帝", "首领", "VoidDragonEmperorModel.java",
     "VoidDragonEmperorRenderer.java", "void_dragon_emperor", "boss", ""),
    ("evil_gazer", "恶", "首领", "EvilUpperModel.java", None, "evil_phases", "boss",
     "天穹特殊定位（三套骨架按阶段切换：Arm/Head/Upper）"),
    # ---------------- 首领级散怪 ----------------
    ("demon_general", "魔道统领", "头目", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "demon_general", "boss_tier", "人形变体"),
    ("void_overlord", "虚空霸主", "首领", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "void_overlord", "boss_tier", "人形变体"),
    ("void_walker", "虚空行者", "头目", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "void_walker", "boss_tier", "人形变体"),
    ("void_serpent", "虚空蛟", "头目", "VoidSerpentRenderer.java", "VoidSerpentRenderer.java",
     "void_serpent", "boss_tier", ""),
    # ---------------- 精英 / 队长 / 普通 ----------------
    ("spirit_wolf", "灵狼", "普通", "SpiritWolfRenderer.java", "SpiritWolfRenderer.java",
     "spirit_wolf", "mob", ""),
    ("crimson_flame_wolf", "赤炎狼", "精英", "SpiritWolfRenderer.java",
     "SpiritWolfRenderer.java", "crimson_flame_wolf", "mob", "复用灵狼骨架"),
    ("flame_ape", "烈焰猿", "精英", "FlameApeModel.java", "FlameApeRenderer.java",
     "flame_ape", "mob", ""),
    ("thunder_eagle", "雷鹰", "队长", "ThunderEagleRenderer.java", "ThunderEagleRenderer.java",
     "thunder_eagle", "mob", ""),
    ("storm_eagle", "雷纹鹰", "精英", "ThunderEagleRenderer.java", "ThunderEagleRenderer.java",
     "storm_eagle", "mob", "复用雷鹰骨架"),
    ("stone_golem", "石傀", "队长", "StoneGolemRenderer.java", "StoneGolemRenderer.java",
     "stone_golem", "mob", ""),
    ("frost_python", "玄冰蟒", "队长", "VoidSerpentRenderer.java", "VoidSerpentRenderer.java",
     "frost_python", "mob", "复用虚空蛟骨架"),
    ("venom_toad", "毒涎蟾", "队长", "VenomToadRenderer.java", "VenomToadRenderer.java",
     "venom_toad", "mob", ""),
    ("burrow_marten", "土行貂", "精英", "BurrowMartenRenderer.java",
     "BurrowMartenRenderer.java", "burrow_marten", "mob", ""),
    ("sword_tomb_guardian", "剑冢守灵", "队长", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "sword_tomb_guardian", "mob", "人形变体"),
    ("tribulation_ash_cultivator", "劫灰修士", "精英", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "tribulation_ash_cultivator", "mob", "人形变体"),
    ("blood_demon_cultivator", "血魔修士", "精英", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "blood_demon_cultivator", "mob", "人形变体"),
    ("puppet_captain", "傀儡队长", "队长", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "puppet_captain", "mob", "人形变体"),
    ("blood_demon_elder", "血魔长老", "队长", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "blood_demon_elder", "mob", "人形变体"),
    ("demon_steward", "魔道执事", "精英", "CultivationHumanoidModel.java",
     "CultivationHumanoidRenderer.java", "demon_steward", "mob", "人形变体"),
    # ---------------- NPC ----------------
    ("sect_elder", "门派长老", "NPC", "SectElderModel.java",
     "SectElderRenderer.java", "sect_elder", "npc", "二轮独立模型：白须+拂尘"),
    ("alchemy_master", "炼丹师", "NPC", "AlchemyMasterModel.java",
     "AlchemyMasterRenderer.java", "alchemy_master", "npc", "二轮独立模型：丹葫芦+药匣"),
    ("wandering_merchant", "云游商人", "NPC", "WanderingMerchantModel.java",
     "CultivationNpcRenderer.java", "wandering_merchant", "npc", "试稿③ 独立模型"),
    ("pill_merchant", "丹药商人", "NPC", "PillMerchantModel.java",
     "PillMerchantRenderer.java", "pill_merchant", "npc", "二轮独立模型：双葫芦串+算盘+小幡"),
    ("artifact_refiner", "炼器师", "NPC", "ArtifactRefinerModel.java",
     "ArtifactRefinerRenderer.java", "artifact_refiner", "npc", "二轮独立模型：剑胚+铁锤"),
    ("realm_guide", "秘境向导", "NPC", "RealmGuideModel.java",
     "RealmGuideRenderer.java", "realm_guide", "npc", "二轮独立模型：路引竿+卷轴筒"),
    ("bounty_board", "悬赏牌看板", "NPC", "BountyBoardModel.java",
     "BountyBoardRenderer.java", "bounty_board", "npc", "二轮独立模型：非人形木牌"),
    # ---------------- 灵兽 ----------------
    ("contract_wolf", "契约灵狼", "灵兽", "SpiritWolfRenderer.java",
     "ContractWolfRenderer.java", "contract_wolf", "beast", ""),
    ("contract_eagle", "契约雷鹰", "灵兽", "ContractEagleRenderer.java",
     "ContractEagleRenderer.java", "contract_eagle", "beast", ""),
    ("jiaolong", "蛟龙", "灵兽", "JiaolongRenderer.java", "JiaolongRenderer.java",
     "jiaolong", "beast", ""),
    # ---------------- 杂项 ----------------
    ("guilty_sect_disciple", "叛徒执事", "精英", "TraitorDiscipleModel.java",
     "TraitorDiscipleRenderer.java", "sect_disciple_qianji", "misc",
     "二轮自建人形骨架（鬼面罩/教徽/锁链腕环，三派换色）"),
    ("venom_spit", "毒涎", "-", "VenomSpitModel.java", "VenomSpitRenderer.java",
     "venom_spit", "misc", "二轮自建 6 盒小模型"),
    ("jiaolong_spit", "蛟龙之息", "-", "JiaolongSpitModel.java", "JiaolongSpitRenderer.java",
     "jiaolong_spit", "misc", "二轮自建 6 盒小模型"),
    # ---------------- 第二轮新增：地形怪（第一梯队 6 只） ----------------
    ("marsh_wraith", "泽魅", "普通", "MarshWraithModel.java", "MarshWraithRenderer.java",
     "marsh_wraith", "mob", "云梦泽"),
    ("crystal_spider", "晶蛛", "普通", "CrystalSpiderModel.java",
     "CrystalSpiderRenderer.java", "crystal_spider", "mob", "灵脉山谷"),
    ("terracotta_soldier", "兵俑", "精英", "TerracottaSoldierModel.java",
     "TerracottaSoldierRenderer.java", "terracotta_soldier", "mob", "荒古战场"),
    ("scarecrow_puppet", "稻草傀儡", "普通", "ScarecrowPuppetModel.java",
     "ScarecrowPuppetRenderer.java", "scarecrow_puppet", "mob", "纪元秘境波次"),
    ("lava_hound", "熔岩犬", "精英", "SpiritWolfRenderer.java", "LavaHoundRenderer.java",
     "lava_hound", "mob", "下界；复用灵狼骨架"),
    ("void_jelly", "虚空水母", "普通", "VoidJellyModel.java", "VoidJellyRenderer.java",
     "void_jelly", "mob", "虚空秘境波次"),
    # ---------------- 第二轮新增：地形怪（第二梯队 7 只） ----------------
    ("end_apostle", "终界使徒", "精英", "EndApostleModel.java", "EndApostleRenderer.java",
     "end_apostle", "mob", "末地自然刷"),
    ("fallen_immortal", "堕仙", "队长", "FallenImmortalModel.java",
     "FallenImmortalRenderer.java", "fallen_immortal", "mob", "真世界精英替换"),
    ("blood_leech", "血蛭", "普通", "BloodLeechModel.java", "BloodLeechRenderer.java",
     "blood_leech", "mob", "血食泥沼"),
    ("bone_eagle", "骨鹰", "精英", "BoneEagleModel.java", "BoneEagleRenderer.java",
     "bone_eagle", "mob", "兽骨荒原"),
    ("demon_spider", "魔蛛", "普通", "DemonSpiderModel.java", "DemonSpiderRenderer.java",
     "demon_spider", "mob", "黑石魔窟"),
    ("charred_skeleton", "焦骨骷髅", "普通", "CharredSkeletonModel.java",
     "CharredSkeletonRenderer.java", "charred_skeleton", "mob", "焦土魔原"),
    ("star_dust_wraith", "星尘游魂", "精英", "StarDustWraithModel.java",
     "StarDustWraithRenderer.java", "star_dust_wraith", "mob", "星尘荒原"),
    # ---------------- 第四轮：天外天（3 只） ----------------
    ("heaven_lord", "仙庭之主", "首领", "HeavenLordModel.java", "HeavenLordRenderer.java",
     "heaven_lord", "boss", "天外天巨型首领"),
    ("heaven_soldier", "天兵", "精英", "HeavenSoldierModel.java",
     "HeavenSoldierRenderer.java", "heaven_soldier", "mob", "天外天环境怪"),
    ("cloud_crane", "云鹤", "队长", "CloudCraneModel.java", "CloudCraneRenderer.java",
     "cloud_crane", "mob", "天外天飞行怪"),
]


def read_entity_sizes():
    out = {}
    for path in (ENT_JSON, ENT_EXTRA_JSON, ENT_EXTRA2_JSON, ENT_EXTRA3_JSON):
        if not path or not os.path.exists(path):
            continue
        src = open(path, encoding="utf-8").read()
        for m in re.finditer(r'register\(\s*"([^"]+)"\s*,(.{0,800}?)\)\s*;', src, re.S):
            sz = re.search(r'\.sized\(\s*([\d.]+)F?\s*,\s*([\d.]+)F?\s*\)', m.group(2))
            if sz:
                out[m.group(1)] = (float(sz.group(1)), float(sz.group(2)))
    return out


def read_lift(fname):
    if not fname:
        return 0.0
    p = os.path.join(CLIENT, fname)
    if not os.path.exists(p):
        return 0.0
    src = open(p, encoding="utf-8").read()
    # 只认字段声明（javadoc 里也出现过 `GROUND_LIFT = (24-24)/16 = 0`，正则此前误抓过注释）
    m = re.search(r'static\s+final\s+double\s+GROUND_LIFT\s*=\s*([^;]+);', src, re.S)
    if not m:
        return 0.0
    expr = re.sub(r'//.*', '', m.group(1)).strip()
    if "LOWEST" in expr:      # 形如 (Model.LOWEST_PIXEL_Y - 24) / 16.0D
        return 0.0
    mm = re.search(r'(-?[\d.]+)\s*[DdFf]?', expr)
    if mm:
        try:
            return float(mm.group(1))
        except ValueError:
            pass
    return 0.0


def classify_holes(model, tex):
    """透明洞分类：{部件: [类别, 洞纹素, 该部件采样纹素]}。"""
    claims, cubes = R._cube_texel_claims(model)
    part_tot, part_holes = {}, {}
    for texel, users in claims.items():
        for u in users:
            pn = cubes[u[0]][0]
            part_tot[pn] = part_tot.get(pn, 0) + 1
            if tex[texel[1], texel[0], 3] < 8:
                part_holes[pn] = part_holes.get(pn, 0) + 1
    detail = {}
    for pn, hc in part_holes.items():
        whole = hc >= part_tot[pn] * 0.9
        if any(k in pn.lower() for k in ALLOW_PARTS) or whole:
            detail[pn] = ["有意透明", hc, part_tot[pn]]
        else:
            detail[pn] = ["真洞", hc, part_tot[pn]]
    return detail


def code_is_evil(eid):
    return eid == "evil_gazer"


def audit_one(ent, sizes, render):
    eid, zh, rank, modelfile, liftfile, texname, group, note = ent
    row = {"id": eid, "zh": zh, "rank": rank, "group": group, "note": note,
           "model": modelfile, "texture": texname, "status": "ok",
           "problems": [], "warnings": []}
    if modelfile is None:
        row["status"] = "skip"
        return row, None
    mp = os.path.join(CLIENT, modelfile)
    if not os.path.exists(mp):
        row["status"] = "missing_model"
        row["problems"].append("模型文件不存在：" + modelfile)
        return row, None
    try:
        model = L.parse_model_file(mp)
    except Exception as e:
        row["status"] = "parse_fail"
        row["problems"].append("模型解析失败：%s" % e)
        return row, None
    lift = read_lift(liftfile)
    row["lift"] = lift
    row["cubes"] = model.total_cubes()
    row["parts"] = len(model.parts)
    row["tex_model"] = list(model.tex)
    tex = glow = None
    if texname:
        tex = R.load_texture(os.path.join(TEXDIR, texname + ".png"))
        glow = R.load_texture(os.path.join(TEXDIR, texname + "_glow.png"))
        row["tex_png"] = None if tex is None else [tex.shape[1], tex.shape[0]]
        row["has_glow"] = glow is not None
        if tex is None:
            row["problems"].append("贴图缺失：" + texname + ".png")
    uv = R.uv_audit(model)
    row["uv_oob"] = uv["oob_count"]
    row["uv_cross_overlap"] = uv["cross_overlap_texels"]
    row["uv_shared"] = uv["shared_texels"]
    row["uv_coverage"] = round(uv["coverage_ratio"], 3)
    if uv["oob_count"]:
        row["problems"].append("UV 越界 %d 面（例 %s）" % (uv["oob_count"], uv["oob"][:2]))
    if uv["cross_overlap_texels"]:
        row["problems"].append("跨足迹 UV 重叠 %d 纹素（足迹对 %s）" % (
            uv["cross_overlap_texels"], uv["cross_pairs"][:2]))
    texr = R.texture_audit(model, tex, glow)
    row["holes"] = texr.get("transparent_used", 0)
    row["flat_faces"] = len(texr.get("flat_faces", []))
    row["holes_real"] = row["holes_allowed"] = 0
    if tex is not None:
        detail = classify_holes(model, tex)
        row["hole_detail"] = detail
        row["holes_real"] = sum(v[1] for v in detail.values() if v[0] == "真洞")
        row["holes_allowed"] = sum(v[1] for v in detail.values() if v[0] == "有意透明")
        if not texr["size_ok"]:
            row["problems"].append("贴图 PNG %s 与模型声明 %s 不符"
                                   % (texr.get("size"), model.tex))
        if row["holes_real"]:
            row["problems"].append("透明洞(真) %d 纹素：%s" % (
                row["holes_real"],
                ", ".join("%s %d/%d" % (k, v[1], v[2]) for k, v in
                          list(detail.items())[:5] if v[0] == "真洞")))
        if row["holes_allowed"]:
            row["warnings"].append("有意透明 %d 纹素（hat/薄片），不计缺陷"
                                   % row["holes_allowed"])
        if texr.get("glow_size_ok") is False:
            row["problems"].append("发光贴图尺寸与主贴图不符")
    g = R.ground_audit(model, lift)
    if g:
        row.update({"foot_px": round(g["foot_px"], 2),
                    "world_foot": round(g["world_foot"], 3),
                    "height_blocks": round(g["height_blocks"], 2)})
        if code_is_evil(eid):
            row["warnings"].append("天穹特殊定位，脚底不按 24 基准判")
        elif abs(g["world_foot"]) > 0.05:
            row["problems"].append("脚底离地 %+.3f 格（应 0）" % g["world_foot"])
    if eid in sizes:
        bw, bh = sizes[eid]
        # 弹体（毒涎/蛟龙之息）在表里品阶写 "-"：没有品阶倍率，按 1.0 算，
        # 只用于"模型高 vs hitbox"的提示性对比，不参与 FAIL 判定。
        sxz, sy = RANK.get(rank, (1.0, 1.0))
        target_h = bh / sy * 16.0
        row["hitbox"] = [bw, bh]
        row["target_h_px"] = round(target_h, 1)
        row["target_w_px"] = round(bw / sxz * 16.0, 1)
        if g:
            row["h_delta_pct"] = round((g["height_blocks"] * 16.0 / target_h - 1) * 100, 1)
        lo, hi = model.bounds()
        row["model_w_px"] = round(hi[0] - lo[0], 1)
        row["model_d_px"] = round(hi[2] - lo[2], 1)
        if abs(row.get("h_delta_pct", 0)) > 25 and not code_is_evil(eid):
            row["warnings"].append("模型高 %+.0f%% vs hitbox 目标（%s 倍率 %.2f）"
                                   % (row["h_delta_pct"], rank, sy))
    gaps = R.gap_audit(model)
    row["gaps"] = [(gp[0], gp[1], round(gp[2], 1)) for gp in gaps]
    # 阈值分层：>10px 算硬问题；4~10px 只记警告（分节蛇身/巨颅等刻意留悬空的部件很多）
    #
    # 设计性悬空豁免：有些"大间距"是**刻意的造型**，不是断模型——
    # 网格本身是连着的（或本来就是悬浮体），审计按 pivot 量出来的距离会误报。
    exempt = GAP_DESIGN_EXEMPT.get(eid, {})
    hard_gaps = []
    exempt_gaps = []
    for gp in gaps:
        if gp[0] in exempt:
            exempt_gaps.append(gp)
        elif gp[2] > 10.0:
            hard_gaps.append(gp)
    if hard_gaps:
        row["problems"].append("挂点疑似断开 %d 处：%s" % (
            len(hard_gaps), ", ".join("%s->%s %.0fpx" % (gp[0], gp[1], gp[2])
                                      for gp in hard_gaps[:3])))
    if exempt_gaps:
        row["warnings"].append("设计性悬空 %d 处（已豁免）：%s" % (
            len(exempt_gaps), "; ".join("%s->%s %.0fpx（%s）" % (
                gp[0], gp[1], gp[2], exempt[gp[0]]) for gp in exempt_gaps[:3])))
    if not hard_gaps and not exempt_gaps and gaps:
        row["warnings"].append("挂点间距偏大 %d 处（4~10px，多为分节/悬空设计）：%s" % (
            len(gaps), ", ".join("%s->%s %.0fpx" % (gp[0], gp[1], gp[2])
                                 for gp in gaps[:3])))
    if render:
        try:
            os.makedirs(PREVDIR, exist_ok=True)
            im = R.render_model(model, tex=tex, glow=glow,
                                views=("front", "right", "back", "hero"),
                                label="BEFORE %s（%s）" % (zh, model.name),
                                ground_lift=lift, size=(300, 450))
            im.save(os.path.join(PREVDIR, eid + ".png"))
            R.silhouette(model, view="hero", px=64).save(
                os.path.join(PREVDIR, "_sil_" + eid + ".png"))
        except Exception as e:
            row["warnings"].append("渲染失败：%s" % e)
    return row, model


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--no-render", action="store_true")
    ap.add_argument("--only", default=None)
    ap.add_argument("--tag", default="00-基线",
                    help="输出文件名前缀（例：00-基线 / 02-修复后）")
    args = ap.parse_args()
    tag = args.tag
    os.makedirs(OUTDIR, exist_ok=True)
    sizes = read_entity_sizes()
    rows, models = [], {}
    print("=" * 104)
    print("%-22s %-6s %-5s %-6s %-5s %-7s %-9s %-7s %s" % (
        "实体", "品阶", "cube", "UV越界", "重叠", "真洞", "脚底(格)", "高差%", "问题"))
    print("=" * 104)
    for ent in ENTITIES:
        if args.only and ent[0] != args.only:
            continue
        try:
            row, model = audit_one(ent, sizes, not args.no_render)
        except Exception as e:
            row = {"id": ent[0], "zh": ent[1], "status": "exception", "group": ent[6],
                   "problems": ["审计异常：%s" % e], "warnings": []}
            model = None
            traceback.print_exc(limit=2)
        if model is not None:
            models[ent[0]] = model
        rows.append(row)
        print("%-22s %-6s %-5s %-6s %-5s %-7s %-9s %-7s %s" % (
            row["id"], row.get("rank", "-"), row.get("cubes", "-"),
            row.get("uv_oob", "-"), row.get("uv_cross_overlap", "-"),
            row.get("holes_real", "-"), row.get("world_foot", "-"),
            row.get("h_delta_pct", "-"),
            "OK" if row.get("status") == "ok" and not row["problems"]
            else "; ".join(row["problems"])[:52]))

    print()
    print("剪影相似度（64px hero，同组内两两 IoU > 0.75 视为重复度高）")
    sil_pairs = []
    groups = {}
    for row in rows:
        if row["id"] in models:
            groups.setdefault(row["group"], []).append(row["id"])
    for grp, ids in groups.items():
        for i in range(len(ids)):
            for j in range(i + 1, len(ids)):
                a, b = ids[i], ids[j]
                try:
                    iou = R.silhouette_iou(models[a], models[b])
                except Exception:
                    continue
                if iou >= 0.75:
                    sil_pairs.append((grp, a, b, round(iou, 3)))
                    print("  ! %-10s %-22s <-> %-22s IoU=%.2f" % (grp, a, b, iou))
    sil_pairs.sort(key=lambda t: -t[3])

    md = ["# 全量角色模型审计（%s · 只读）" % tag, "",
          "> 生成：`python 工程/角色设计/tools/audit_all.py`  ",
          "> 覆盖：%d 个实体注册项（2 个原版投掷物无自建模型、1 个僵尸骨架占位）" % len(rows),
          "> 口径：UV 越界 / 跨足迹重叠 / 透明洞 / 贴图尺寸 / 脚底世界高度 / 挂点断开 / hitbox 偏差 / 剪影相似度",
          "",
          "## 0. 总览", "",
          "| 项 | 数量 |", "|---|---|",
          "| 审计实体 | %d |" % len([r for r in rows if r.get("status") == "ok"]),
          "| 有硬问题（UV/真洞/脚底/挂点） | %d |" % len([r for r in rows if r["problems"]]),
          "| 有警告（hitbox 偏差 / 有意透明） | %d |" % len([r for r in rows if r["warnings"]]),
          "| 剪影 IoU>=0.75 的对 | %d |" % len(sil_pairs),
          "",
          "## 1. 逐实体", "",
          "| 实体 | 品阶 | 模型 | cube | 贴图 | UV越界 | 跨足迹重叠 | 真洞 | 脚底(世界格) | 高差vs hitbox | 判定 |",
          "|---|---|---|---|---|---|---|---|---|---|---|"]
    for row in rows:
        if row.get("status") == "skip":
            md.append("| %s %s | - | - | - | - | - | - | - | - | - | 跳过：%s |"
                      % (row["zh"], row["id"], row["note"]))
            continue
        if row.get("status") in ("missing_model", "parse_fail", "exception"):
            md.append("| %s %s | %s | - | - | - | - | - | - | - | - | **%s** |"
                      % (row["zh"], row["id"], row.get("rank", "-"),
                         "；".join(row["problems"])[:80]))
            continue
        ok = not row["problems"]
        md.append("| %s %s | %s | %s | %d | %s | %s | %s | %s | %s | %s | %s |" % (
            row["zh"], row["id"], row["rank"], row["model"], row["cubes"],
            "%dx%d" % tuple(row["tex_model"]),
            row["uv_oob"], row["uv_cross_overlap"], row["holes_real"],
            row.get("world_foot"),
            ("%+.0f%%" % row["h_delta_pct"]) if "h_delta_pct" in row else "-",
            "**PASS**" if ok else "**FAIL**"))
    md += ["", "## 2. 硬问题清单（必须修）", ""]
    n = 0
    for row in rows:
        if not row["problems"]:
            continue
        n += 1
        md.append("### %d) %s（%s）" % (n, row["zh"], row["id"]))
        for pr in row["problems"]:
            md.append("- " + pr)
        if row.get("hole_detail"):
            md.append("")
            md.append("  透明洞明细：")
            for pn, v in row["hole_detail"].items():
                if v[0] == "真洞":
                    md.append("  - `%s`：%d / %d 纹素" % (pn, v[1], v[2]))
        md.append("")
    if not n:
        md.append("（无）")
    md += ["", "## 3. 警告清单（设计层面待处理）", ""]
    n = 0
    for row in rows:
        if not row["warnings"]:
            continue
        n += 1
        md.append("- **%s（%s）**：%s" % (row["zh"], row["id"], "；".join(row["warnings"])))
    if not n:
        md.append("（无）")
    md += ["", "## 4. 剪影相似度（同组两两 IoU >= 0.75）", "",
           "| 组 | 实体 A | 实体 B | IoU |", "|---|---|---|---|"]
    for (grp, a, b, iou) in sil_pairs:
        md.append("| %s | %s | %s | %.2f |" % (grp, a, b, iou))
    if not sil_pairs:
        md.append("| - | - | - | - |")
    md += ["", "## 5. 证据", "",
           "- 每个实体的 before 三视图：`工程/角色设计/previews/baseline/<id>.png`",
           "- 64px 剪影：`previews/baseline/_sil_<id>.png`",
           "- 机读数据：`工程/角色设计/审计/00-基线-全量模型审计.json`",
           "", "## 6. 已知限制", "",
           "- 离线几何/UV 审计：`setupAnim` 动画姿态下的穿模、粒子/染色、实机光照不在覆盖内；",
           "- 人形怪/NPC 共用一套骨架，UV 审计只查共享骨架本身（贴图按单张 64×64 判）；",
           "- 恶（天穹）为特殊定位，脚底不按 24 基准判；三套骨架只审计第 3 阶段 Upper 模型。",
           ""]
    with open(os.path.join(OUTDIR, tag + "-全量模型审计.md"), "w",
              encoding="utf-8", newline="\n") as f:
        f.write("\n".join(md) + "\n")
    with open(os.path.join(OUTDIR, tag + "-全量模型审计.json"), "w",
              encoding="utf-8", newline="\n") as f:
        json.dump({"rows": rows, "sil_pairs": sil_pairs}, f, ensure_ascii=False, indent=2)
    sils = [os.path.join(PREVDIR, "_sil_%s.png" % r["id"]) for r in rows
            if os.path.exists(os.path.join(PREVDIR, "_sil_%s.png" % r["id"]))]
    if sils:
        try:
            R.contact_sheet(sils, os.path.join(PREVDIR, "_剪影总览.png"), cols=8,
                            title="基线 64px 剪影总览（%d 个实体）" % len(sils))
        except Exception as e:
            print("[warn] 剪影总览失败：%s" % e)
    fail = len([r for r in rows if r["problems"]])
    print()
    print("报告：工程/角色设计/审计/" + tag + "-全量模型审计.md")
    print("硬问题实体：%d / %d；剪影高相似对：%d"
          % (fail, len([r for r in rows if r.get("status") != "skip"]), len(sil_pairs)))


if __name__ == "__main__":
    main()
