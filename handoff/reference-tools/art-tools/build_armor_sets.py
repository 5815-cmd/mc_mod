# -*- coding: utf-8 -*-
"""
R-2 · 职业甲 10 套资源生成（W-06 第一版：同骨架 + 职业形制 + 职业色板）
=====================================================================
- 剑修进阶套（career_armor_jian_2）已由 W-01R 交付，本脚本**不覆盖**它；
- 其余 9 套：复制 W-01R 的护甲骨架，按职业加"形制件"（血幡 / 魔骨 / 鬼纱 / 重甲），
  再按职业色板重绘贴图（256×256 + glowmask），出 4×32×32 图标；
- 输出直接进 mod 资源目录；预览图进 工程/art/armor_sets/。

美术口径：06 §1 / 08 §1 的名称与职业主色（Career.rgb()）——
  剑修 #9ED9FF / 血修 #E6212E / 魔修 #9E33D9 / 鬼修 #73FFB8 / 体修 #F2C759
"""
from __future__ import annotations

import copy
import json
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import g2_tex
from artlib import build_mesh, contact_sheet, render_views
from build_w01r import build_armor

HERE = os.path.dirname(os.path.abspath(__file__))
W01R = os.path.abspath(os.path.join(HERE, "..", "w01r"))
PREV = os.path.join(W01R, "armor_sets")
WS = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
os.makedirs(PREV, exist_ok=True)

# ---------------------------------------------------------------------------
# 职业色板（材质四色 + 发光色 + 衣缘 + 金线）
# ---------------------------------------------------------------------------

PALETTES = {
    "jian": dict(
        metal=(198, 214, 230), steel=(238, 244, 250), cloth=(250, 251, 253),
        jade=(165, 232, 248), rune=(110, 216, 246),
        glow=(127, 227, 242), hem=(59, 180, 224), gold=(201, 162, 74)),
    "xue": dict(
        metal=(150, 138, 140), steel=(208, 196, 194), cloth=(118, 24, 32),
        jade=(226, 72, 72), rune=(255, 74, 74),
        glow=(255, 74, 74), hem=(120, 14, 22), gold=(206, 168, 84)),
    "mo": dict(
        metal=(118, 108, 128), steel=(178, 168, 188), cloth=(58, 36, 74),
        jade=(176, 96, 255), rune=(176, 96, 255),
        glow=(176, 96, 255), hem=(86, 48, 124), gold=(188, 158, 80)),
    "gui": dict(
        metal=(148, 174, 164), steel=(198, 220, 210), cloth=(36, 62, 54),
        jade=(118, 255, 184), rune=(102, 242, 200),
        glow=(102, 242, 200), hem=(38, 108, 88), gold=(176, 176, 118)),
    "ti": dict(
        metal=(122, 118, 112), steel=(192, 186, 174), cloth=(108, 78, 30),
        jade=(255, 215, 92), rune=(255, 215, 92),
        glow=(255, 215, 92), hem=(116, 88, 40), gold=(228, 188, 92)),
}


def pal_for(career_id):
    c = PALETTES[career_id]
    return {k: c[k] for k in ("metal", "steel", "cloth", "jade", "rune")}


def accent_for(career_id):
    c = PALETTES[career_id]
    return c["glow"], c["hem"], c["gold"]


def add(bone, x0, x1, y0, y1, z0, z1, mat):
    bone.add((x0, y0, z0), (x1 - x0, y1 - y0, z1 - z0), mat=mat)


# ---------------------------------------------------------------------------
# 职业"形制件"：同骨架 + 每个职业一眼能分的附加结构
# ---------------------------------------------------------------------------

def decorate(geo, career_id, advanced):
    head = geo.get("armorHead")
    body = geo.get("armorBody")
    ra = geo.get("armorRightArm")
    la = geo.get("armorLeftArm")
    rl = geo.get("armorRightLeg")
    ll = geo.get("armorLeftLeg")
    rb = geo.get("armorRightBoot")
    lb = geo.get("armorLeftBoot")

    if career_id == "xue":
        # 血修：腰带垂血幡 ×2 + 肩头血晶
        add(body, -2.6, -1.7, 4.0, 10.6, -3.1, -2.7, "cloth")
        add(body, 1.7, 2.6, 4.0, 10.6, -3.1, -2.7, "cloth")
        add(la, 4.6, 5.6, 22.2, 23.2, -2.2, -1.2, "jade")
        add(ra, -5.6, -4.6, 22.2, 23.2, -2.2, -1.2, "jade")
    elif career_id == "mo":
        # 魔修：肩骨刺 ×2 + 胸骨珠 + 两侧肋片
        add(la, 6.2, 7.4, 24.4, 26.4, -0.7, 0.7, "metal")
        add(ra, -7.4, -6.2, 24.4, 26.4, -0.7, 0.7, "metal")
        add(body, -1.5, 1.5, 19.4, 21.4, -3.55, -3.15, "jade")
        add(body, -4.0, -3.2, 15.0, 20.5, -3.2, -2.8, "metal")
        add(body, 3.2, 4.0, 15.0, 20.5, -3.2, -2.8, "metal")
    elif career_id == "gui":
        # 鬼修：背后鬼纱 ×2 + 双肩悬浮魂灯
        add(body, -3.7, -2.8, 3.8, 10.4, 3.0, 3.35, "cloth")
        add(body, 2.8, 3.7, 3.8, 10.4, 3.0, 3.35, "cloth")
        add(la, 3.4, 4.4, 25.4, 26.6, 1.0, 2.0, "jade")
        add(ra, -4.4, -3.4, 25.4, 26.6, 1.0, 2.0, "jade")
        add(head, -1.2, 1.2, 34.6, 35.4, -4.2, -3.6, "rune")
    elif career_id == "ti":
        # 体修：加厚肩顶 + 腰带铁链 + 拳甲
        add(la, 3.6, 8.9, 25.0, 26.3, -3.2, 3.2, "metal")
        add(ra, -8.9, -3.6, 25.0, 26.3, -3.2, 3.2, "metal")
        add(body, -4.6, 4.6, 10.2, 10.7, -3.1, -2.7, "steel")
        add(la, 3.8, 8.2, 11.9, 12.6, -2.7, 2.7, "steel")
        add(ra, -8.2, -3.8, 11.9, 12.6, -2.7, 2.7, "steel")
    elif career_id == "jian":
        # 剑修：胸前剑形流苏（基础套比进阶套少两枚符纹）
        add(body, -0.5, 0.5, 9.4, 11.4, -3.3, -2.9, "jade")

    if advanced:
        # 进阶套统一加"能量件"：胸口两侧符文片 + 冠侧光片
        add(body, -3.4, -2.4, 17.0, 18.2, -3.4, -3.0, "rune")
        add(body, 2.4, 3.4, 17.0, 18.2, -3.4, -3.0, "rune")
        add(head, -5.6, -4.9, 30.2, 31.0, -1.2, 1.2, "rune")
        add(head, 4.9, 5.6, 30.2, 31.0, -1.2, 1.2, "rune")
    else:
        # 基础套：腰带扣换成布结（更朴素）
        add(body, -1.2, 1.2, 11.2, 12.3, -3.35, -3.0, "cloth")


# ---------------------------------------------------------------------------
# 生成
# ---------------------------------------------------------------------------

SETS = [
    ("jian", 1, "青锋剑袍"), ("xue", 1, "血纹战衣"),
    ("xue", 2, "血海战衣"), ("mo", 1, "骸骨魔铠"), ("mo", 2, "魔渊魔铠"),
    ("gui", 1, "幽魂道袍"), ("gui", 2, "黄泉鬼袍"),
    ("ti", 1, "玄铁重甲"), ("ti", 2, "不灭金身"),
]
# 剑修进阶（jian,2）已由 W-01R 交付，不重做


def main():
    base = build_armor()
    previews = []
    stats = []
    for cid, iv, cn in SETS:
        geo = copy.deepcopy(base)
        decorate(geo, cid, iv == 2)
        geo.identifier = "geometry.career_armor_%s_%d" % (cid, iv)
        used = 0
        try:
            k, st = g2_tex.pack_geo(geo, (256, 256))
        except RuntimeError:
            k, st = g2_tex.pack_geo(geo, (512, 512))
            used = 512
        tex_size = (256, 256) if used == 0 else (512, 512)
        geo.tex = tex_size
        name = "career_armor_%s_%d" % (cid, iv)
        geo_p = os.path.join(RES, "geo", name + ".geo.json")
        geo.save(geo_p)
        tex_p = os.path.join(RES, "textures", "item", name + ".png")
        glow_p = os.path.join(RES, "textures", "item", name + "_glowmask.png")
        pal = pal_for(cid)
        glow_color, hem, gold = accent_for(cid)
        ratio, tstat = g2_tex.paint_geo(geo, tex_size, tex_p, glow_p,
                                        pal=pal, glow_color=glow_color,
                                        hem=hem, gold=gold)
        tex_arr = np.array(Image.open(tex_p).convert("RGB"))
        glow_arr = np.array(Image.open(glow_p).convert("RGB"))
        with open(os.path.join(RES, "animations", name + ".animation.json"),
                  "w", encoding="utf-8") as f:
            json.dump({"format_version": "1.8.0",
                       "animations": {"idle": {"loop": True,
                                               "animation_length": 2.0,
                                               "bones": {}}}}, f, ensure_ascii=False, indent=2)
        # 4 件图标
        tris_all = build_mesh(geo)
        for slot, bones in (
                ("helmet", ["armorHead"]),
                ("chestplate", ["armorBody", "armorRightArm", "armorLeftArm"]),
                ("leggings", ["armorRightLeg", "armorLeftLeg"]),
                ("boots", ["armorRightBoot", "armorLeftBoot"])):
            hidden = [b.name for b in geo.bones if b.name not in bones]
            tris = build_mesh(geo, hidden=hidden)
            tmp = os.path.join(PREV, "_tmp.png")
            render_views(tris, tmp, views=("front",), size=(512, 512),
                         transparent=True, unlit=True,
                         texture=tex_arr, glow=glow_arr)
            g2_tex.make_icon(tmp, os.path.join(RES, "textures", "item",
                                               name + "_" + slot + ".png"), size=32)
        if os.path.exists(os.path.join(PREV, "_tmp.png")):
            os.remove(os.path.join(PREV, "_tmp.png"))
        # 预览（贴图三视图）
        prev = os.path.join(PREV, "prev_%s.png" % name)
        render_views(tris_all, prev, views=("front", "side", "hero"),
                     size=(380, 560),
                     label="%s·%s · R-2 外观 v1" % (cn, "进阶" if iv == 2 else "初阶"),
                     texture=tex_arr, glow=glow_arr)
        previews.append(prev)
        stats.append({"set": name, "cn": cn, "cubes": geo.cube_count(),
                      "k": k, "tex": tex_size, "glow": round(ratio, 4)})
        print("[OK]", name, cn, "cubes", geo.cube_count(), "k", k,
              "tex", tex_size, "glow %.1f%%" % (ratio * 100))

    contact_sheet(previews, os.path.join(PREV, "0_ARMOR_9SETS.png"), cols=3,
                  title="R-2 职业甲 9 套外观 v1（剑修进阶已由 W-01R 交付）")
    with open(os.path.join(PREV, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)
    print("[DONE] 9 套资源已写入", RES)


if __name__ == "__main__":
    main()
