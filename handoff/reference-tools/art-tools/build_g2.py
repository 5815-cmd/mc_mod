# -*- coding: utf-8 -*-
"""
W-01R · G2：贴图 / 自发光 / 图标 + 贴图三视图
=============================================
输入 = G1 的 geo（几何不动），输出 = G2 的最终资产：

  assets/geo/career_sword_10.geo.json            （带逐面 UV）
  assets/geo/career_armor_jian_2.geo.json
  assets/textures/item/career_sword_10.png       （128×128 主贴图）
  assets/textures/item/career_sword_10_glowmask.png
  assets/textures/item/career_armor_jian_2.png   （128/256 主贴图）
  assets/textures/item/career_armor_jian_2_glowmask.png
  assets/textures/item/career_armor_jian_2_{helmet,chestplate,leggings,boots}.png  （32×32 图标）
  assets/animations/career_sword_10.animation.json
  assets/animations/career_armor_jian_2.animation.json

以及工程内预览图（g2/ 目录）：贴图三视图、全套贴图试穿、图标总览、贴图展开图。
"""
from __future__ import annotations

import json
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import g2_tex
from artlib import (build_edges, build_mesh, contact_sheet, player_body_geo,
                    render_views)
from build_w01r import build_armor, build_sword

HERE = os.path.dirname(os.path.abspath(__file__))
W01R = os.path.abspath(os.path.join(HERE, "..", "w01r"))
G2 = os.path.join(W01R, "g2")
ASSETS = os.path.join(W01R, "assets")
TEX = os.path.join(ASSETS, "textures", "item")
GEO = os.path.join(ASSETS, "geo")
ANIM = os.path.join(ASSETS, "animations")
for d in (G2, TEX, GEO, ANIM):
    os.makedirs(d, exist_ok=True)


def to_np(path):
    return np.array(Image.open(path).convert("RGB"))


def save_geo(geo, ident, path):
    geo.identifier = ident
    geo.save(path)
    return path


def write_anim_sword(path):
    """剑 idle：双符环反向旋转 + 前刃浮沉 + 剑穗摆。"""
    anim = {
        "format_version": "1.8.0",
        "animations": {
            "idle": {
                "loop": True,
                "animation_length": 4.0,
                "bones": {
                    "ring_a": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, -360, 0]}},
                    "ring_b": {"rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]}},
                    "blade_float": {
                        "position": {"0.0": [0, 0, 0], "1.0": [0, 0.16, 0],
                                     "2.0": [0, 0, 0], "3.0": [0, 0.16, 0],
                                     "4.0": [0, 0, 0]}
                    },
                    "tassel": {"rotation": {"0.0": [0, 0, -7], "1.0": [0, 0, 7],
                                            "2.0": [0, 0, -7], "3.0": [0, 0, 7],
                                            "4.0": [0, 0, -7]}},
                },
            }
        },
    }
    with open(path, "w", encoding="utf-8") as f:
        json.dump(anim, f, ensure_ascii=False, indent=2)
    return path


def write_anim_armor(path):
    """护甲 idle：GeoArmorRenderer 会接管骨骼跟随，这里只放一个合法的空 idle。"""
    anim = {
        "format_version": "1.8.0",
        "animations": {"idle": {"loop": True, "animation_length": 2.0, "bones": {}}},
    }
    with open(path, "w", encoding="utf-8") as f:
        json.dump(anim, f, ensure_ascii=False, indent=2)
    return path


SWORD_TEX = (256, 256)
ARMOR_TEX = (128, 128)


def main():
    global ARMOR_TEX
    sword = build_sword()
    armor = build_armor()

    # ---------- UV 打包 ----------
    sk, sstat = g2_tex.pack_geo(sword, SWORD_TEX)
    try:
        ak, astat = g2_tex.pack_geo(armor, ARMOR_TEX)
    except RuntimeError:
        ARMOR_TEX = (256, 256)
        ak, astat = g2_tex.pack_geo(armor, ARMOR_TEX)
    print("[UV] sword k=%d %s" % (sk, sstat))
    print("[UV] armor k=%d tex=%s %s" % (ak, ARMOR_TEX, astat))
    sword.tex = SWORD_TEX
    armor.tex = ARMOR_TEX

    # ---------- 贴图 ----------
    s_tex = os.path.join(TEX, "career_sword_10.png")
    s_glow = os.path.join(TEX, "career_sword_10_glowmask.png")
    a_tex = os.path.join(TEX, "career_armor_jian_2.png")
    a_glow = os.path.join(TEX, "career_armor_jian_2_glowmask.png")
    s_ratio, s_stat = g2_tex.paint_geo(sword, SWORD_TEX, s_tex, s_glow)
    a_ratio, a_stat = g2_tex.paint_geo(armor, ARMOR_TEX, a_tex, a_glow)
    print("[GLOW] sword %.1f%%  armor %.1f%%" % (s_ratio * 100, a_ratio * 100))
    print("[TEX ] sword %s  armor %s" % (SWORD_TEX, ARMOR_TEX))

    # ---------- geo（带 UV）----------
    save_geo(sword, "geometry.career_sword_10",
             os.path.join(GEO, "career_sword_10.geo.json"))
    save_geo(armor, "geometry.career_armor_jian_2",
             os.path.join(GEO, "career_armor_jian_2.geo.json"))

    # ---------- 动画 ----------
    write_anim_sword(os.path.join(ANIM, "career_sword_10.animation.json"))
    write_anim_armor(os.path.join(ANIM, "career_armor_jian_2.animation.json"))

    # ---------- 预览 ----------
    s_tex_arr, s_glow_arr = to_np(s_tex), to_np(s_glow)
    a_tex_arr, a_glow_arr = to_np(a_tex), to_np(a_glow)
    body = player_body_geo()
    body_tris = build_mesh(body)

    render_views(build_mesh(sword), os.path.join(G2, "1_sword_3view_tex.png"),
                 views=("front", "side", "back", "hero"), size=(420, 700),
                 label="鸿蒙·霜纹古篆剑 · G2 贴图（正/侧/背/3-4）",
                 texture=s_tex_arr, glow=s_glow_arr)

    pieces = [
        ("armorHead", "冠", "2_armor_helmet_tex.png", "helmet"),
        (["armorBody", "armorRightArm", "armorLeftArm"], "胸甲",
         "3_armor_chestplate_tex.png", "chestplate"),
        (["armorRightLeg", "armorLeftLeg"], "护腿", "4_armor_leggings_tex.png", "leggings"),
        (["armorRightBoot", "armorLeftBoot"], "靴", "5_armor_boots_tex.png", "boots"),
    ]
    icon_tmp = os.path.join(G2, "_icon_tmp.png")
    icon_sheets = []
    for key, cn, fn, slot in pieces:
        if isinstance(key, list):
            keep = set(key)
            hidden = [b.name for b in armor.bones if b.name not in keep]
        else:
            hidden = [b.name for b in armor.bones if b.name != key]
        tris = build_mesh(armor, hidden=hidden)
        render_views(tris, os.path.join(G2, fn), views=("front", "side", "back"),
                     size=(380, 520), label=cn + " · G2 贴图（正/侧/背）",
                     texture=a_tex_arr, glow=a_glow_arr)
        # 32×32 图标（透明底 + 外描边；护甲用正面 unlit，避免 3D 明暗把 32px 糊掉）
        render_views(tris, icon_tmp, views=("front",), size=(512, 512),
                     transparent=True, unlit=True,
                     texture=a_tex_arr, glow=a_glow_arr)
        icon_png = os.path.join(TEX, "career_armor_jian_2_" + slot + ".png")
        g2_tex.make_icon(icon_tmp, icon_png, size=32)
        icon_sheets.append(icon_png)

    # 剑图标（备档，不用于 item model）
    render_views(build_mesh(sword), icon_tmp, views=("icon",), size=(512, 512),
                 transparent=True, unlit=True, texture=s_tex_arr, glow=s_glow_arr)
    g2_tex.make_icon(icon_tmp, os.path.join(TEX, "career_sword_10_icon.png"), size=32)
    icon_sheets.insert(0, os.path.join(TEX, "career_sword_10_icon.png"))
    if os.path.exists(icon_tmp):
        os.remove(icon_tmp)

    # 全套贴图试穿
    render_views(body_tris + build_mesh(armor), os.path.join(G2, "6_armor_full_tex.png"),
                 views=("front", "side", "back", "hero"), size=(430, 700),
                 label="进阶·剑心道袍 4 件 · G2 贴图 + 灰模玩家",
                 texture=a_tex_arr, glow=a_glow_arr)

    # 图标总览（放大显示）
    thumbs = []
    for p in icon_sheets:
        im = Image.open(p).convert("RGBA").resize((128, 128), Image.NEAREST)
        thumbs.append(im)
    sheet = Image.new("RGBA", (len(thumbs) * 136 + 8, 150), (48, 54, 66, 255))
    for i, im in enumerate(thumbs):
        sheet.alpha_composite(im, (8 + i * 136, 16))
    sheet.convert("RGB").save(os.path.join(G2, "7_icons_32px.png"))

    # 贴图展开图（主贴图 + glowmask 放大 2×）
    tex_sheet_imgs = []
    for p, name in ((s_tex, "剑主贴图"), (s_glow, "剑发光层"),
                    (a_tex, "甲主贴图"), (a_glow, "甲发光层")):
        im = Image.open(p).convert("RGB")
        k = 2 if max(im.size) <= 128 else 1
        im = im.resize((im.width * k, im.height * k), Image.NEAREST)
        tex_sheet_imgs.append(im)
    contact_sheet(tex_sheet_imgs, os.path.join(G2, "8_textures.png"), cols=2,
                  title="G2 贴图展开（主贴图 + 自发光层）")

    # 一览
    contact_sheet([
        os.path.join(G2, "1_sword_3view_tex.png"),
        os.path.join(G2, "2_armor_helmet_tex.png"),
        os.path.join(G2, "3_armor_chestplate_tex.png"),
        os.path.join(G2, "4_armor_leggings_tex.png"),
        os.path.join(G2, "5_armor_boots_tex.png"),
        os.path.join(G2, "6_armor_full_tex.png"),
    ], os.path.join(G2, "0_W01R_G2_全览.png"), cols=2,
        title="W-01R · G2 贴图总览")

    print("[OK] G2 资产输出：", ASSETS)


if __name__ == "__main__":
    main()
