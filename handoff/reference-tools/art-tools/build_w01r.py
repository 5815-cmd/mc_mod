# -*- coding: utf-8 -*-
"""
W-01R · G1 白模：鸿蒙·霜纹古篆剑 + 进阶·剑心道袍 4 件
====================================================
只产出：
  1) geo json（G1 阶段只用于渲染，不进 assets）
  2) 白模三视图 / 穿模检查图 / 32px 缩略图
不写 Java、不注册物品、不碰任何现有资产。

设计口径（对照 06 总表 / 08 交接表 / 01 美术风格圣经）：
  - 霜纹古篆剑 = 长剑 10 阶（鸿蒙），F-1 仙侠骨架 + F-2 终局幻想：
    中段断口悬浮前刃、双符环（一竖一横）、霜晶剑格、篆文浮片、冰蓝剑穗。
  - 剑心道袍 = 剑修进阶甲（职业主色 Career.JIAN_XIU.rgb = #9ED9FF 银蓝）：
    白绸交领 + 剑脊胸甲 + 剑心晶核 + 剑形冠饰 + 后浮玉简。
"""
from __future__ import annotations

import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from artlib import (Geo, build_edges, build_mesh, contact_sheet,
                    player_body_geo, render_views, ring_cubes, step_point,
                    taper_blade, thumb_32)

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(HERE, "..", "w01r", "g1"))
GEO_OUT = os.path.abspath(os.path.join(HERE, "..", "w01r", "assets", "geo"))
os.makedirs(OUT, exist_ok=True)
os.makedirs(GEO_OUT, exist_ok=True)


def limb_box(bone, sign, x0, x1, y0, y1, z0, z1, mat="metal", **kw):
    """按"正半轴坐标"描述一侧肢体，sign=-1 时镜像到 -X 侧。"""
    if sign < 0:
        x0, x1 = -x1, -x0
    bone.add((x0, y0, z0), (x1 - x0, y1 - y0, z1 - z0), mat=mat, **kw)


# ===========================================================================
# 一、鸿蒙·霜纹古篆剑（总长 40u = 2.5 格）
# ===========================================================================

def build_sword() -> Geo:
    g = Geo("geometry.w01r_sword_shuangwen", tex=(128, 128), visible=(0, 20, 0))
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 10.0, 0), parent="grip")
    blade = g.bone("blade_main", (0, 12.0, 0), parent="guard")
    floating = g.bone("blade_float", (0, 29.0, 0), parent="guard")
    ring_a = g.bone("ring_a", (0, 18.0, 0), parent="blade_main")
    ring_b = g.bone("ring_b", (0, 24.5, 0), parent="blade_main")
    tassel = g.bone("tassel", (0, 0, 0), parent="grip")

    # ---- 剑首（0~1.8）+ 柄（2.2~10.0）----
    grip.add((-1.6, 0.0, -1.6), (3.2, 1.8, 3.2), mat="metal")      # 剑首
    grip.add((-1.2, 1.8, -1.2), (2.4, 0.4, 2.4), mat="steel")      # 剑首颈圈
    grip.add((-1.1, 2.2, -1.1), (2.2, 7.8, 2.2), mat="cloth")      # 缠绳柄
    for y in (3.6, 5.6, 7.6):                                      # 缠绳箍 ×3
        grip.add((-1.25, y, -1.25), (2.5, 0.5, 2.5), mat="steel")

    # ---- 剑格（10.0~12.0）：中块 + 两侧上翘翼 + 剑心宝石 + 悬浮碎晶 ----
    guard.add((-2.0, 10.0, -1.6), (4.0, 1.8, 3.2), mat="metal")
    for s in (-1, 1):
        def gx(x0, x1):
            return (x0, x1) if s > 0 else (-x1, -x0)
        x0, x1 = gx(2.0, 3.6)
        guard.add((x0, 10.4, -1.2), (x1 - x0, 1.3, 2.4), mat="metal")     # 内翼
        x0, x1 = gx(3.6, 5.0)
        guard.add((x0, 10.9, -1.0), (x1 - x0, 1.2, 2.0), mat="metal")     # 外翼
        x0, x1 = gx(4.7, 5.6)
        guard.add((x0, 11.9, -0.6), (x1 - x0, 1.0, 1.2), mat="jade")      # 外翼霜晶
        x0, x1 = gx(3.9, 4.7)
        guard.add((x0, 13.0, -0.55), (x1 - x0, 0.8, 0.8), mat="jade")     # 悬浮碎晶
    guard.add((-0.9, 10.2, -1.95), (1.8, 1.7, 0.35), mat="rune")          # 剑心宝石

    # ---- 主刃 12.0~29.0（锥形 + 两侧刃口条 + 剑脊线 + 篆文浮片）----
    taper_blade(blade, 12.0, 29.0, 4.0, 3.2, 1.6, 1.2, 5, mat="metal")
    blade.add((-0.55, 12.2, -0.88), (1.1, 16.4, 0.22), mat="steel")      # 剑脊凸线
    for s in (-1, 1):
        x0 = 1.68 if s > 0 else -1.80
        blade.add((x0, 12.2, -0.45), (0.12, 16.6, 0.9), mat="rune")      # 刃口条
    for yy, xx, rr in ((15.0, -2.45, 18), (19.2, 2.45, -18),
                       (23.4, -2.45, 18), (27.2, 2.45, -18)):
        blade.add((xx - 0.45, yy - 0.65, -0.10), (0.9, 1.3, 0.18),
                  mat="jade", rotation=(0, rr, 0), pivot=(xx, yy, 0))

    # ---- 悬浮前刃 31.2~40.2（断口 2.2u，强调"断刃悬浮"）----
    taper_blade(floating, 31.2, 38.8, 3.0, 1.6, 1.2, 0.75, 3, mat="metal")
    floating.add((-0.4, 31.4, -0.62), (0.8, 7.4, 0.2), mat="steel")
    for s in (-1, 1):
        x0 = 0.72 if s > 0 else -0.84
        floating.add((x0, 31.4, -0.3), (0.12, 7.5, 0.6), mat="rune")
    step_point(floating, 38.8, 40.2, 1.6, 0.75, 2, mat="metal")
    for xx, yy in ((-2.6, 33.0), (2.6, 34.4), (-2.2, 37.4)):           # 霜晶碎块
        floating.add((xx - 0.3, yy - 0.3, -0.3), (0.6, 0.6, 0.6), mat="jade")

    # ---- 符环 A（竖环：从正面读）与符环 B（横环：从侧/3-4 读）----
    # 节流：每 3 块里 1 块是真发光符文（rune），其余是青玉（jade），
    # 把剑的自发光面积压到 15% 以内（G2 实测会写进报告）。
    for i in range(20):
        a = 2 * math.pi * i / 20
        cx, cy = math.cos(a) * 4.4, 18.0 + math.sin(a) * 4.4
        ring_a.add((cx - 0.43, cy - 0.43, -0.43), (0.86, 0.86, 0.86),
                   mat=("rune" if i % 6 == 0 else "jade"),
                   rotation=(0, 0, math.degrees(a)))
    for i in range(20):
        a = 2 * math.pi * i / 20
        cx = math.cos(a) * 4.4
        cz = math.sin(a) * 4.4
        ring_b.add((cx - 0.43, 24.5 - 0.43, cz - 0.43), (0.86, 0.86, 0.86),
                   mat=("rune" if i % 6 == 1 else "jade"),
                   rotation=(0, -math.degrees(a), 0))

    # ---- 剑穗（3 节，垂在剑首下）----
    tassel.add((-0.6, -0.9, 0.5), (1.2, 1.0, 0.7), mat="cloth")
    tassel.add((-0.5, -2.0, 0.45), (1.0, 1.1, 0.65), mat="cloth")
    tassel.add((-0.4, -3.1, 0.4), (0.8, 1.1, 0.6), mat="cloth")
    return g


# ===========================================================================
# 二、进阶·剑心道袍（4 件，8 根骨骼，玩家空间脚底 y=0）
# ===========================================================================

def build_armor() -> Geo:
    g = Geo("geometry.w01r_armor_jian2", tex=(128, 128), visible=(0, 16, 0))

    # ---------------- 冠（armorHead）----------------
    head = g.bone("armorHead", (0, 24, 0))
    # 额带 / 侧带 / 后带（贴着头，前片在眉上，不遮眼）
    head.add((-4.5, 30.2, -4.85), (9.0, 1.4, 0.55), mat="metal")   # 额带
    head.add((-4.85, 29.4, -4.1), (0.7, 2.2, 8.2), mat="metal")    # 左侧带
    head.add((4.15, 29.4, -4.1), (0.7, 2.2, 8.2), mat="metal")     # 右侧带
    head.add((-4.5, 29.4, 4.3), (9.0, 2.2, 0.55), mat="metal")     # 后带
    # 束发冠：第一层
    head.add((-3.1, 31.6, -3.4), (6.2, 1.4, 0.6), mat="steel")
    head.add((-3.1, 31.6, 2.8), (6.2, 1.4, 0.6), mat="steel")
    head.add((-3.4, 31.6, -2.8), (0.6, 1.4, 5.6), mat="steel")
    head.add((2.8, 31.6, -2.8), (0.6, 1.4, 5.6), mat="steel")
    # 束发冠：第二层 + 顶盖
    head.add((-2.2, 33.0, -2.35), (4.4, 1.2, 0.55), mat="metal")
    head.add((-2.2, 33.0, 1.8), (4.4, 1.2, 0.55), mat="metal")
    head.add((-1.3, 34.2, -1.3), (2.6, 0.6, 2.6), mat="steel")
    # 前额剑形冠饰（竖着的三节小剑，整个冠的记忆点）
    head.add((-1.4, 30.0, -5.15), (2.8, 0.5, 0.4), mat="steel")    # 小剑格
    head.add((-0.8, 30.5, -5.15), (1.6, 1.6, 0.4), mat="jade")
    head.add((-0.6, 32.1, -5.12), (1.2, 2.0, 0.38), mat="jade")
    head.add((-0.35, 34.1, -5.1), (0.7, 1.8, 0.36), mat="rune")
    # 发簪 + 簪头
    head.add((-5.6, 32.3, 3.4), (11.2, 0.5, 0.6), mat="steel")
    head.add((-6.1, 32.15, 3.25), (0.6, 0.8, 0.9), mat="jade")
    head.add((5.5, 32.15, 3.25), (0.6, 0.8, 0.9), mat="jade")
    # 耳侧护片
    for s in (-1, 1):
        x0 = -5.1 if s < 0 else 4.4
        head.add((x0, 27.6, -1.5), (0.7, 2.2, 4.0), mat="metal")

    # ---------------- 胸（armorBody）----------------
    body = g.bone("armorBody", (0, 24, 0))
    # 后浮剑形玉简改挂"肩后"：挂在胸甲骨背侧、双肩后方（随胸甲出现，不再挤在冠两侧）
    for s in (-1, 1):
        x0 = -6.5 if s < 0 else 4.6
        body.add((x0, 20.4, 3.15), (1.9, 5.0, 0.5), mat="jade")
        body.add((x0 + 0.35, 25.4, 3.18), (1.2, 1.0, 0.45), mat="jade")
        body.add((x0 + 0.65, 26.4, 3.2), (0.6, 0.8, 0.4), mat="rune")
    # 交领（两片斜领，压在胸甲之上）
    body.add((-3.8, 21.9, -3.3), (3.7, 1.5, 0.45), mat="cloth",
             rotation=(0, 0, -20), pivot=(-0.1, 23.4, -3.1))
    body.add((0.1, 21.9, -3.3), (3.7, 1.5, 0.45), mat="cloth",
             rotation=(0, 0, 20), pivot=(0.1, 23.4, -3.1))
    # 胸甲（上宽下窄两片，收出 V 形腰）
    body.add((-4.6, 17.0, -2.95), (9.2, 6.4, 0.8), mat="metal")
    body.add((-4.1, 14.6, -2.85), (8.2, 2.6, 0.7), mat="metal")
    # 剑脊竖线
    body.add((-0.7, 14.8, -3.22), (1.4, 8.6, 0.45), mat="steel")
    # 剑心晶核
    body.add((-1.3, 18.4, -3.36), (2.6, 2.6, 0.45), mat="rune")
    body.add((-0.75, 18.95, -3.56), (1.5, 1.5, 0.25), mat="jade")
    # 背甲 + 背脊
    body.add((-4.6, 14.2, 2.15), (9.2, 9.4, 0.65), mat="metal")
    body.add((-0.8, 13.6, 2.8), (1.6, 9.0, 0.4), mat="steel")
    # 侧甲
    body.add((-4.85, 13.0, -2.1), (0.7, 10.6, 4.2), mat="metal")
    body.add((4.15, 13.0, -2.1), (0.7, 10.6, 4.2), mat="metal")
    # 腹部两层
    body.add((-4.15, 13.7, -2.7), (8.3, 0.9, 0.6), mat="cloth")
    body.add((-3.95, 12.6, -2.6), (7.9, 0.9, 0.6), mat="cloth")
    # 腰带（四围）+ 带扣
    body.add((-4.45, 11.5, -2.9), (8.9, 1.2, 0.7), mat="steel")
    body.add((-4.45, 11.5, 2.2), (8.9, 1.2, 0.7), mat="steel")
    body.add((-4.45, 11.5, -2.2), (0.7, 1.2, 4.4), mat="steel")
    body.add((3.75, 11.5, -2.2), (0.7, 1.2, 4.4), mat="steel")
    body.add((-1.1, 11.3, -3.2), (2.2, 1.6, 0.6), mat="metal")
    # 腰坠（短款，挂在腰带上）
    body.add((-0.9, 9.6, -3.15), (1.8, 2.0, 0.45), mat="jade")
    body.add((-0.5, 11.0, -3.2), (1.0, 1.1, 0.4), mat="rune")
    # 髋部短裙摆（刚性，只护髋，不影响摆腿）——长袍片改挂到腿骨上
    body.add((-4.3, 10.8, -2.95), (8.6, 1.3, 0.55), mat="cloth")
    body.add((-4.3, 10.8, 2.4), (8.6, 1.3, 0.55), mat="cloth")
    for s in (-1, 1):
        x0 = -4.45 if s < 0 else 3.9
        body.add((x0, 10.8, -2.4), (0.55, 1.3, 4.8), mat="cloth")
    # 中缝前后垂布（填住两腿之间的空隙，让下摆读作"一整条道袍"）
    body.add((-0.5, 5.0, -2.98), (1.0, 6.2, 0.5), mat="cloth")
    body.add((-0.5, 4.6, -2.98), (1.0, 0.5, 0.5), mat="cloth")
    body.add((-0.5, 4.9, 2.5), (1.0, 6.3, 0.5), mat="cloth")

    # ---------------- 臂（肩甲 + 护臂）----------------
    for name, sign in (("armorRightArm", -1), ("armorLeftArm", 1)):
        arm = g.bone(name, (5.0 * sign, 22, 0))
        # 三层肩甲：顶盖高过肩线、逐层外扩下压
        limb_box(arm, sign, 4.2, 8.4, 24.4, 26.0, -2.6, 2.6, "metal")   # 顶盖
        limb_box(arm, sign, 3.9, 9.4, 22.4, 24.6, -3.1, 3.1, "metal")   # 中层
        limb_box(arm, sign, 3.6, 9.9, 20.3, 22.6, -3.1, 3.1, "metal")   # 底层
        # 外缘银白条 + 外侧上翘翼尖
        limb_box(arm, sign, 9.6, 10.0, 20.8, 24.4, -2.4, 2.4, "steel")
        limb_box(arm, sign, 8.8, 10.1, 23.2, 24.9, -2.1, 2.1, "steel")
        # 上臂外护
        limb_box(arm, sign, 7.55, 8.15, 16.0, 20.4, -2.3, 2.3, "cloth")
        # 前臂护甲（前/后/外三片 + 腕环）
        limb_box(arm, sign, 3.9, 8.05, 12.4, 16.2, -2.55, -2.0, "metal")
        limb_box(arm, sign, 3.9, 8.05, 12.4, 16.2, 2.0, 2.55, "metal")
        limb_box(arm, sign, 7.5, 8.15, 12.4, 16.2, -2.0, 2.0, "metal")
        limb_box(arm, sign, 3.9, 7.9, 12.2, 12.8, -2.5, 2.5, "steel")

    # ---------------- 腿（四片裙甲挂腿骨 + 膝 + 胫甲）----------------
    for name, sign in (("armorRightLeg", -1), ("armorLeftLeg", 1)):
        leg = g.bone(name, (1.9 * sign, 12, 0))
        # 裙甲四片挂腿骨：摆腿时跟着走，不穿模
        limb_box(leg, sign, 0.55, 3.55, 6.0, 11.2, -2.95, -2.4, "cloth")   # 前摆上段
        limb_box(leg, sign, 0.45, 3.85, 5.2, 6.0, -2.95, -2.4, "cloth")    # 前摆外撇
        limb_box(leg, sign, 0.55, 3.55, 5.2, 11.2, 2.4, 2.95, "cloth")     # 后摆
        limb_box(leg, sign, 3.55, 4.35, 5.4, 11.2, -2.3, 2.3, "cloth")     # 外侧摆
        # 膝甲 + 剑纹
        limb_box(leg, sign, 0.6, 3.9, 4.2, 5.8, -2.75, -2.15, "steel")
        limb_box(leg, sign, 1.75, 2.25, 4.0, 6.2, -2.9, -2.6, "rune")
        # 胫甲
        limb_box(leg, sign, 0.55, 3.75, 1.5, 4.2, -2.5, -1.95, "metal")
        limb_box(leg, sign, 3.65, 4.15, 1.8, 4.2, -2.2, 2.2, "metal")

    # ---------------- 靴 ----------------
    for name, sign in (("armorRightBoot", -1), ("armorLeftBoot", 1)):
        boot = g.bone(name, (1.9 * sign, 12, 0))
        limb_box(boot, sign, 0.45, 4.15, 0.0, 0.7, -3.1, 2.35, "steel")   # 云纹鞋底
        limb_box(boot, sign, 0.40, 4.05, 0.7, 2.0, -2.45, 2.25, "metal")  # 靴面
        limb_box(boot, sign, 0.55, 3.80, 0.7, 1.7, -3.3, -2.45, "metal")  # 靴尖下段
        limb_box(boot, sign, 0.80, 3.50, 1.7, 3.0, -3.25, -2.65, "steel") # 靴尖上翘
        limb_box(boot, sign, 0.45, 3.90, 2.0, 4.8, -2.45, -1.95, "metal") # 踝前
        limb_box(boot, sign, 0.45, 3.90, 2.0, 4.8, 1.95, 2.45, "metal")   # 踝后
        limb_box(boot, sign, 3.65, 4.15, 2.0, 4.8, -2.2, 2.2, "metal")    # 踝外
        limb_box(boot, sign, 0.45, 3.90, 4.4, 4.9, -2.5, 2.5, "rune")     # 青纹踝环

    return g


# ===========================================================================
# 三、渲染
# ===========================================================================

def main():
    sword = build_sword()
    armor = build_armor()
    body = player_body_geo()

    p_sword = sword.save(os.path.join(GEO_OUT, "w01r_sword.geo.json"))
    p_armor = armor.save(os.path.join(GEO_OUT, "w01r_armor_jian2.geo.json"))

    print("== 剑 ==", "cubes:", sword.cube_count(), "bones:", len(sword.bones))
    print("    bounds:", [round(v, 1) for v in sword.bounds()[0]],
          [round(v, 1) for v in sword.bounds()[1]])
    print("== 甲 ==", "cubes:", armor.cube_count(),
          "bones:", [b.name for b in armor.bones])
    print("    bounds:", [round(v, 1) for v in armor.bounds()[0]],
          [round(v, 1) for v in armor.bounds()[1]])

    sword_tris = build_mesh(sword)
    sword_edges = build_edges(sword)
    render_views(sword_tris, os.path.join(OUT, "1_sword_views.png"),
                 views=("front", "side", "back", "hero"), size=(420, 700),
                 label="鸿蒙·霜纹古篆剑 · G1 白模（正/侧/背/3-4）",
                 edges=sword_edges)
    thumb_32(sword_tris, os.path.join(OUT, "1_sword_thumb32.png"), "剑 32px")

    pieces = [
        ("armorHead", "冠", "2_armor_helmet.png"),
        (["armorBody", "armorRightArm", "armorLeftArm"], "胸甲", "3_armor_chestplate.png"),
        (["armorRightLeg", "armorLeftLeg"], "护腿", "4_armor_leggings.png"),
        (["armorRightBoot", "armorLeftBoot"], "靴", "5_armor_boots.png"),
    ]
    for key, cn, fn in pieces:
        if isinstance(key, list):
            keep = set(key)
            hidden = [b.name for b in armor.bones if b.name not in keep]
        else:
            hidden = [b.name for b in armor.bones if b.name != key]
        tris = build_mesh(armor, hidden=hidden)
        edges = build_edges(armor, hidden=hidden)
        render_views(tris, os.path.join(OUT, fn), views=("front", "side", "back"),
                     size=(380, 520), label=cn + " · G1 白模（正/侧/背）",
                     edges=edges)

    # 全套 + 灰模玩家（穿模检查）
    full_tris = build_mesh(body) + build_mesh(armor)
    render_views(full_tris, os.path.join(OUT, "6_armor_full.png"),
                 views=("front", "side", "back", "hero"), size=(430, 700),
                 label="进阶·剑心道袍 4 件 + 灰模玩家（正/侧/背/3-4）",
                 edges=build_edges(armor))

    # 32px 缩略图
    sheets = [os.path.join(OUT, "1_sword_thumb32.png")]
    for key, cn, fn in pieces:
        if isinstance(key, list):
            keep = set(key)
            hidden = [b.name for b in armor.bones if b.name not in keep]
        else:
            hidden = [b.name for b in armor.bones if b.name != key]
        tris = build_mesh(armor, hidden=hidden)
        tp = os.path.join(OUT, "thumb_" + cn + ".png")
        thumb_32(tris, tp, cn)
        sheets.append(tp)
    contact_sheet(sheets, os.path.join(OUT, "7_thumb32_all.png"), cols=5,
                  title="32×32 识别自检（缩到 32px 还能认出是什么）")

    # 走路姿态（腿/臂各摆 ±22°，只用于 G1 穿模检查）
    import copy as _copy
    walk = _copy.deepcopy(armor)
    walk.get("armorRightLeg").rotation = (22, 0, 0)
    walk.get("armorLeftLeg").rotation = (-22, 0, 0)
    walk.get("armorRightBoot").rotation = (22, 0, 0)
    walk.get("armorLeftBoot").rotation = (-22, 0, 0)
    walk.get("armorRightArm").rotation = (-20, 0, 0)
    walk.get("armorLeftArm").rotation = (20, 0, 0)
    render_views(build_mesh(body) + build_mesh(walk),
                 os.path.join(OUT, "8_armor_walk.png"),
                 views=("side", "front", "hero"), size=(430, 700),
                 label="走路姿态穿模检查（腿 ±22° / 臂 ∓20°）",
                 edges=build_edges(walk))

    contact_sheet([
        os.path.join(OUT, "1_sword_views.png"),
        os.path.join(OUT, "2_armor_helmet.png"),
        os.path.join(OUT, "3_armor_chestplate.png"),
        os.path.join(OUT, "4_armor_leggings.png"),
        os.path.join(OUT, "5_armor_boots.png"),
        os.path.join(OUT, "6_armor_full.png"),
        os.path.join(OUT, "8_armor_walk.png"),
    ], os.path.join(OUT, "0_W01R_G1_全览.png"), cols=2,
        title="W-01R · G1 白模总览")

    print("geo:", p_sword, p_armor)
    print("png:", OUT)


if __name__ == "__main__":
    main()
