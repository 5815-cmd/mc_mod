# -*- coding: utf-8 -*-
"""
R-2 资源自检（离线、不需要客户端）
==================================
服务端不校验 geo/动画/模型 JSON 的语法与结构 —— 这些错误只在**客户端资源加载**时爆，
最坏会导致资源重载失败、整包卸载甚至崩客户端（HANDOFF 里记过真事故）。
本脚本把能在离线阶段抓的全抓一遍：

1. 10 套职业甲 + 15 个武器外观的 geo：JSON 合法、identifier、骨骼名/契约、
   cube 的 origin/size/faceUV 是否越界；
2. 贴图/glowmask/图标的尺寸与存在性；
3. 90 个物品模型 JSON、引用的贴图是否真实存在；
4. 90 条中英 lang 是否齐全；
5. 26 个动画 JSON 是否合法且含 idle；
6. 物品 ID 集合（40 甲 + 50 武器）是否与资源一一对应、无重复。
"""
from __future__ import annotations

import json
import os
import re
import sys

from PIL import Image

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
REPORT = os.path.join(WS, "工程", "art", "w01r", "reports", "asset_validation.txt")

ARMOR_BONES = ["armorHead", "armorBody", "armorRightArm", "armorLeftArm",
               "armorRightLeg", "armorLeftLeg", "armorRightBoot", "armorLeftBoot"]
FACES = {"west", "east", "down", "up", "north", "south"}
FORMS = ["sword", "greatsword", "scythe", "spear", "staff"]
SLOTS = ["helmet", "chestplate", "leggings", "boots"]

errors = []
warnings = []
lines = []


def ok(msg):
    lines.append("[OK]   " + msg)


def bad(msg):
    errors.append(msg)
    lines.append("[FAIL] " + msg)


def warn(msg):
    warnings.append(msg)
    lines.append("[WARN] " + msg)


def load_json(path, label):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:  # noqa: BLE001
        bad("%s JSON 解析失败：%s（%s）" % (label, os.path.basename(path), e))
        return None


def read_armor_sets():
    src = open(os.path.join(
        WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng", "xiuxian",
        "item", "armor", "CareerArmorSet.java"), encoding="utf-8").read()
    pat = re.compile(r'(\w+)\(Career\.(\w+), Tier\.(\w+), "([^"]+)"')
    cid = {"JIAN_XIU": "jian", "XUE_XIU": "xue", "MO_XIU": "mo",
           "GUI_XIU": "gui", "TI_XIU": "ti"}
    out = []
    for m in pat.finditer(src):
        _, career, tier, cn = m.groups()
        iv = {"BASIC": 1, "ADVANCED": 2, "MASTER": 3}[tier]
        out.append((cid[career], iv, cn))
    return out


def check_geo(path, label, is_armor):
    data = load_json(path, label)
    if data is None:
        return
    try:
        top = data["minecraft:geometry"][0]
        desc = top["description"]
        tex_w = int(desc["texture_width"])
        tex_h = int(desc["texture_height"])
        bones = top["bones"]
    except Exception as e:  # noqa: BLE001
        bad("%s geo 结构缺字段：%s" % (label, e))
        return
    names = [b.get("name") for b in bones]
    if is_armor:
        # 契约 8 骨必须齐全；允许追加动态件骨（fx/cape/veil/lantern）
        if not set(ARMOR_BONES).issubset(set(names)):
            bad("%s 护甲缺契约骨：%s" % (label, names))
    else:
        # 武器契约是"握把底在 [0,0,0]、刃身朝 +Y"，不要求骨骼一定叫 grip：
        # 只要求 ① 有 pivot=[0,0,0] 的根骨 ② 包围盒从 y≈0 起、且纵向远长于横向。
        roots = [b for b in bones if not b.get("parent")]
        zero_pivot = [b for b in roots if list(b.get("pivot", [])) == [0, 0, 0]]
        if not zero_pivot:
            bad("%s 武器没有 pivot=[0,0,0] 的根骨：%s"
                % (label, [(b.get("name"), b.get("pivot")) for b in roots]))
        ys = []
        xs = []
        for b in bones:
            for c in b.get("cubes", []):
                ys.append(c["origin"][1])
                ys.append(c["origin"][1] + c["size"][1])
                xs.append(c["origin"][0])
                xs.append(c["origin"][0] + c["size"][0])
        if ys:
            if min(ys) > 0.6:
                bad("%s 握把底不在 y≈0（最低 y=%s）" % (label, min(ys)))
            # 长武器：纵向不短于横向（死神镰刀盘很大，允许横向更宽）
            if (max(ys) - min(ys)) < 0.9 * (max(xs) - min(xs)):
                bad("%s 纵向长度不足（不是朝 +Y 的长武器）" % label)
    cube_n = 0
    uv_bad = 0
    for b in bones:
        for c in b.get("cubes", []):
            cube_n += 1
            for k in ("origin", "size"):
                if not (isinstance(c.get(k), list) and len(c[k]) == 3):
                    bad("%s cube 缺 %s" % (label, k))
            uv = c.get("uv")
            if isinstance(uv, dict):
                for face, rect in uv.items():
                    if face not in FACES:
                        bad("%s 非法面名 %s" % (label, face))
                        continue
                    u, v = int(rect["uv"][0]), int(rect["uv"][1])
                    w, h = int(rect["uv_size"][0]), int(rect["uv_size"][1])
                    if w <= 0 or h <= 0:
                        bad("%s faceUV 尺寸非正：%s %s" % (label, face, rect))
                    if u < 0 or v < 0 or u + w > tex_w or v + h > tex_h:
                        uv_bad += 1
            elif not (isinstance(uv, list) and len(uv) == 2):
                bad("%s cube 的 uv 既不是盒 UV 也不是 faceUV" % label)
    if uv_bad:
        bad("%s 有 %d 个面的 UV 越过 %dx%d 图集" % (label, uv_bad, tex_w, tex_h))
    if cube_n < 10:
        warn("%s 只有 %d 个 cube（偏少）" % (label, cube_n))
    if not is_armor and is_armor is False:
        pass
    ok("%s geo 合法（%d cube / %dx%d）" % (label, cube_n, tex_w, tex_h))


def check_tex(path, size, label):
    if not os.path.exists(path):
        bad("%s 缺贴图：%s" % (label, os.path.basename(path)))
        return
    try:
        im = Image.open(path)
        if tuple(im.size) != tuple(size):
            bad("%s 贴图尺寸 %s ≠ 期望 %s" % (label, im.size, size))
        else:
            ok("%s 贴图 %s" % (label, size))
    except Exception as e:  # noqa: BLE001
        bad("%s 贴图打不开（%s）" % (label, e))


def main():
    sets = read_armor_sets()
    lines.append("职业甲套数：%d" % len(sets))
    if len(sets) != 15:
        bad("应读出 15 套职业甲，实际 %d" % len(sets))

    armor_ids = []
    # 职业甲：5 职业 × 10 境界 = 50 套独立外观，每套 4 个独立图标
    for cid in ("jian", "xue", "mo", "gui", "ti"):
        for realm in range(1, 11):
            base = "career_armor_%s_%d" % (cid, realm)
            geo_p = os.path.join(RES, "geo", base + ".geo.json")
            if not os.path.exists(geo_p):
                bad("%s 缺 geo" % base)
            else:
                check_geo(geo_p, base, True)
            check_tex(os.path.join(RES, "textures", "item", base + ".png"),
                      (256, 256), base)
            check_tex(os.path.join(RES, "textures", "item", base + "_glowmask.png"),
                      (256, 256), base + " glow")
            for slot in SLOTS:
                item = base + "_" + slot
                armor_ids.append(item)
                check_tex(os.path.join(RES, "textures", "item", item + ".png"),
                          (32, 32), item + " 图标")

    weapon_ids = []
    weapon_variants = [
        ("sword", ["t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"]),
        ("greatsword", ["t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"]),
        ("scythe", ["t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"]),
        ("spear", ["t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"]),
        ("staff", ["t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"]),
        ("war_scythe", ["t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"]),
    ]
    fams = []
    for form, keys in weapon_variants:
        for key in keys:
            fams.append("career_%s_%s" % (form, key))
    for fam in fams:
        geo_p = os.path.join(RES, "geo", fam + ".geo.json")
        if not os.path.exists(geo_p):
            bad("%s 缺 geo" % fam)
        else:
            check_geo(geo_p, fam, False)
        check_tex(os.path.join(RES, "textures", "item", fam + ".png"),
                  (256, 256), fam)
        check_tex(os.path.join(RES, "textures", "item", fam + "_glowmask.png"),
                  (256, 256), fam + " glow")
    for form, _ in weapon_variants:
        for tier in range(1, 11):
            weapon_ids.append("career_weapon_%s_%d" % (form, tier))

    all_ids = armor_ids + weapon_ids
    if len(all_ids) != len(set(all_ids)):
        bad("物品 ID 有重复")
    else:
        ok("物品 ID 共 %d 个，无重复（甲 %d + 武器 %d）"
           % (len(all_ids), len(armor_ids), len(weapon_ids)))

    # 物品模型
    model_bad = 0
    for item in all_ids:
        p = os.path.join(RES, "models", "item", item + ".json")
        if not os.path.exists(p):
            bad("缺物品模型 " + item)
            model_bad += 1
            continue
        data = load_json(p, item)
        if data is None:
            continue
        texs = data.get("textures", {})
        for key in ("layer0", "particle"):
            if key in texs:
                rel = texs[key].replace("hongmeng:", "")
                tp = os.path.join(RES, "textures", rel + ".png")
                if not os.path.exists(tp):
                    bad("%s 引用的贴图不存在：%s" % (item, texs[key]))
    if model_bad == 0:
        ok("%d 个物品模型齐全且贴图引用有效" % len(all_ids))

    # lang
    zh = load_json(os.path.join(RES, "lang", "zh_cn.json"), "zh_cn")
    en = load_json(os.path.join(RES, "lang", "en_us.json"), "en_us")
    if zh and en:
        lack_zh = [i for i in all_ids if ("item.hongmeng." + i) not in zh]
        lack_en = [i for i in all_ids if ("item.hongmeng." + i) not in en]
        if lack_zh or lack_en:
            bad("lang 缺条目：zh %d / en %d（例：%s）"
                % (len(lack_zh), len(lack_en), (lack_zh or lack_en)[:3]))
        else:
            ok("%d 件装备中英 lang 齐全" % len(all_ids))

    # 动画（10 套甲 + 21 把名器）
    anim_bad = 0
    anim_total = 0
    anim_fams = ["career_armor_%s_%d" % (cid, realm) for cid in ("jian", "xue", "mo", "gui", "ti") for realm in range(1, 11)] + fams
    for fam in anim_fams:
        anim_total += 1
        p = os.path.join(RES, "animations", fam + ".animation.json")
        if not os.path.exists(p):
            bad("缺动画 " + fam)
            anim_bad += 1
        else:
            data = load_json(p, fam)
            if data and "idle" not in data.get("animations", {}):
                bad("%s 动画缺 idle" % fam)
            # 动画里引用的骨骼必须真的存在于同族 geo 中（否则动画静默失效/报错）
            elif data:
                geo_p = os.path.join(RES, "geo", fam + ".geo.json")
                if os.path.exists(geo_p):
                    geo = load_json(geo_p, fam + " geo")
                    if geo:
                        geo_bones = {b.get("name") for b in
                                     geo["minecraft:geometry"][0]["bones"]}
                        anim_bones = set(data["animations"]["idle"].get("bones", {}))
                        missing = anim_bones - geo_bones
                        if missing:
                            bad("%s 动画引用了不存在的骨骼 %s" % (fam, missing))
    if anim_bad == 0:
        ok("%d 个动画文件存在（含 idle）" % anim_total)

    # 汇总
    lines.append("")
    lines.append("===== 资源自检：%d 项失败 / %d 警告 / 共 %d 条检查 ====="
                 % (len(errors), len(warnings), len(lines)))
    text = "\n".join(lines)
    os.makedirs(os.path.dirname(REPORT), exist_ok=True)
    with open(REPORT, "w", encoding="utf-8") as f:
        f.write(text + "\n")
    print(text)
    if errors:
        sys.exit(1)


if __name__ == "__main__":
    main()
