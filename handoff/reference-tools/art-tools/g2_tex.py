# -*- coding: utf-8 -*-
"""
G2 贴图工具（武器盔甲师自用）
==============================
给 G2 阶段做三件事，全部离线、可重复：

1. **逐面 UV 打包**：把每个 cube 的 6 个面按"每单位多少像素"排进一张图集
   （shelf packing），写回 `cube.uv_faces`，geo 导出时变成 Bedrock faceUV。
2. **程序化绘制贴图 + 自发光层**：按材质/部位画三阶明暗、边线、布料褶、霜纹、
   玉简渐变、符文发光；自发光层按"符文/刃口/核心"规则单独画。
3. **统计自发光面积**（美术圣经要求 ≤15%，硬上限 20%）。

用法：
    from g2_tex import pack_geo, paint_geo, save_png
"""
from __future__ import annotations

import math
import os

import numpy as np
from PIL import Image

# ---------------------------------------------------------------------------
# 调色板（G2 定稿，来源：01 美术风格圣经 §1 + Career.JIAN_XIU.rgb() #9ED9FF）
# ---------------------------------------------------------------------------

PAL = {
    "metal": (198, 214, 230),      # 霜银（冷）
    "steel": (238, 244, 250),      # 亮银（高光面）
    "cloth": (250, 251, 253),      # 白绸（暖白）
    "jade": (165, 232, 248),       # 青玉
    "rune": (110, 216, 246),       # 符文（会发光）
}
GLOW_CYAN = (127, 227, 242)        # #7FE3F2
GLOW_DEEP = (63, 180, 224)         # #3FB4E0
GOLD = (201, 162, 74)              # #C9A24A，≤5%
PURPLE = (53, 36, 97)              # #352461，鸿蒙缠绳
HEM_CYAN = (59, 180, 224)          # 裙摆/衣缘

FACES = ("west", "east", "down", "up", "north", "south")


def face_dims(size):
    """每个面的 (宽, 高) —— 模型单位。"""
    w, h, d = size
    return {
        "west": (d, h), "east": (d, h),
        "down": (w, d), "up": (w, d),
        "north": (w, h), "south": (w, h),
    }


# ---------------------------------------------------------------------------
# 1) UV 打包
# ---------------------------------------------------------------------------

def pack_geo(geo, tex_size=(128, 128), pad=1, kmax=8):
    """给 geo 里所有 cube 分配逐面 UV。返回 (k, 用量统计)。"""
    best = None
    for k in range(kmax, 1, -1):
        rects = []
        for bone in geo.bones:
            for ci, cube in enumerate(bone.cubes):
                for face, (du, dv) in face_dims(cube.size).items():
                    pw = max(2, int(round(du * k)))
                    ph = max(2, int(round(dv * k)))
                    rects.append([bone.name, ci, face, pw, ph, cube])
        order = sorted(range(len(rects)), key=lambda i: (-rects[i][4], -rects[i][3]))
        x = y = rowh = 0
        assign = {}
        ok = True
        for i in order:
            pw, ph = rects[i][3], rects[i][4]
            if pw + 2 * pad > tex_size[0]:
                ok = False
                break
            if x + pw + pad > tex_size[0]:
                x = 0
                y += rowh + pad
                rowh = 0
            if y + ph + pad > tex_size[1]:
                ok = False
                break
            assign[i] = (x, y, pw, ph)
            x += pw + pad
            rowh = max(rowh, ph)
        if ok:
            used_h = y + rowh
            best = (k, rects, assign, used_h)
            break
    if best is None:
        raise RuntimeError("UV 打包失败：试到 k=2 也放不下")
    k, rects, assign, used_h = best
    # 写回 cube.uv_faces
    for i, (bname, ci, face, pw, ph, cube) in enumerate(rects):
        x, y, w, h = assign[i]
        if cube.uv_faces is None:
            cube.uv_faces = {}
        cube.uv_faces[face] = (x, y, w, h)
    return k, {"faces": len(rects), "used_height": used_h,
               "fill": (sum(r[3] * r[4] for r in rects) /
                        float(tex_size[0] * tex_size[1]))}


# ---------------------------------------------------------------------------
# 2) 绘制
# ---------------------------------------------------------------------------

def _shade_face(arr, x, y, w, h, base, mat, bone, cube, sword_ymin=12.0,
                sword_ymax=40.0, hem=HEM_CYAN, gold=GOLD,
                grip_color=None, tassel_color=None):
    """在 arr 的 (x,y,w,h) 区域里画一张面的基础色 + 三阶明暗。"""
    rgb = np.zeros((h, w, 3), dtype=np.float64)
    rgb[:] = base
    is_blade = bone in ("blade_main", "blade_float")
    if is_blade and mat in ("metal", "steel"):
        # 剑身：按"绝对高度"做连续渐变（跨段不出现横向接缝）+ 霜纹
        cy = cube.origin[1] + cube.size[1] * 0.5
        t = (cy - sword_ymin) / max(1e-6, sword_ymax - sword_ymin)
        f = 0.90 + 0.18 * max(0.0, min(1.0, t))
        rgb *= f
    else:
        # 纵向三阶：上亮 8%、中平、下暗 10%
        for j in range(h):
            t = (j / max(1, h - 1)) if h > 1 else 0.0
            if t < 0.33:
                f = 1.08
            elif t > 0.72:
                f = 0.90
            else:
                f = 1.0
            rgb[j, :, :] *= f
        # 1px 深色边线（让每个面/块在游戏里读得清）
        if h >= 3 and w >= 3:
            e = 0.86
            rgb[0, :, :] *= e
            rgb[-1, :, :] *= e
            rgb[:, 0, :] *= e
            rgb[:, -1, :] *= e
    # 材质细节
    if mat == "cloth":
        for p in (0.32, 0.68):
            cx = int(round((w - 1) * p))
            rgb[:, cx, :] *= 0.94
    elif mat in ("metal", "steel"):
        if h > w * 1.4:      # 长条金属（刃/板）：中间竖直高光
            cx = w // 2
            rgb[:, cx, :] = np.clip(rgb[:, cx, :] * 1.08, 0, 255)
        if w >= 6 and h >= 6:
            gy = max(1, h // 3)
            rgb[gy:gy + 1, 1:-1, :] = np.clip(rgb[gy:gy + 1, 1:-1, :] * 1.06, 0, 255)
    elif mat == "jade":
        cy, cx = h / 2.0, w / 2.0
        yy, xx = np.mgrid[0:h, 0:w]
        dist = np.sqrt(((yy - cy) / max(1.0, h)) ** 2 + ((xx - cx) / max(1.0, w)) ** 2)
        glow = np.clip(1.25 - dist * 1.4, 0.8, 1.22)
        rgb *= glow[:, :, None]
    elif mat == "rune":
        rgb[:] = np.array(GLOW_CYAN, dtype=np.float64)
        if w >= 3:
            cx = w // 2
            rgb[:, cx, :] = np.clip(rgb[:, cx, :] * 1.18 + 30, 0, 255)

    # 部位特例：缠绳柄 / 剑穗 / 裙摆衣缘 / 剑身霜纹 / 金线
    if bone == "grip" and mat == "cloth":
        rgb[:] = np.array(grip_color if grip_color else PURPLE, dtype=np.float64)
        for j in range(h):
            if (j // 3) % 2 == 0:
                rgb[j, :, :] *= 0.86
        if w >= 3:
            rgb[:, 0, :] = np.array(gold, dtype=np.float64) * 0.9
    if bone == "tassel":
        rgb[:] = np.array(tassel_color if tassel_color else (206, 232, 247),
                          dtype=np.float64)
        if h > 2:
            rgb[1:h - 1, :, :] *= 1.04
    if bone in ("armorRightLeg", "armorLeftLeg") and mat == "cloth":
        if h >= 6:
            rgb[-2:, :, :] = np.array(hem, dtype=np.float64)
    if mat == "steel" and bone == "armorBody" and 11.1 <= cube.origin[1] <= 11.9:
        rgb[-1, :, :] = np.array(gold, dtype=np.float64) * 0.95   # 腰带金线
    if mat == "cloth" and bone == "armorBody" and cube.origin[1] > 21.5:
        rgb[-1, :, :] = np.array(gold, dtype=np.float64) * 0.94   # 交领金边
    if bone in ("blade_main", "blade_float") and mat == "metal":
        # 霜纹：两条极淡的斜裂纹（不破坏大面）
        for p in (0.35, 0.62):
            yy = int(round((h - 1) * p))
            if 1 <= yy < h - 1:
                for xx in range(1, w - 1):
                    if (xx + yy) % 7 < 2:
                        rgb[yy, xx, :] = np.clip(
                            rgb[yy, xx, :] * 0.78 + np.array((176, 228, 244)) * 0.22,
                            0, 255)

    arr[y:y + h, x:x + w, :] = np.clip(rgb, 0, 255)


def paint_geo(geo, tex_size, out_png, out_glow_png, pal=None,
              glow_color=None, hem=None, gold=None, grip_color=None,
              tassel_color=None):
    """程序化绘制主贴图 + 自发光层，保存 PNG，返回 (glow_ratio, stats)。

    pal / glow_color / hem / gold / grip_color / tassel_color 可整套替换
    （10 套职业甲、21 把名器按各自配色生成时用）。
    """
    pal = PAL if pal is None else pal
    glow_color = GLOW_CYAN if glow_color is None else glow_color
    hem = HEM_CYAN if hem is None else hem
    gold = GOLD if gold is None else gold
    W, H = tex_size
    arr = np.full((H, W, 3), 40, dtype=np.float64)      # 未使用区：深灰
    glow = np.zeros((H, W, 3), dtype=np.float64)
    glow_px = 0
    total_px = 0
    for bone in geo.bones:
        for cube in bone.cubes:
            base = np.array(pal.get(cube.mat, pal["metal"]), dtype=np.float64)
            for face in FACES:
                if not cube.uv_faces or face not in cube.uv_faces:
                    continue
                x, y, w, h = cube.uv_faces[face]
                _shade_face(arr, x, y, w, h, base, cube.mat, bone.name, cube,
                            sword_ymin=12.0, sword_ymax=40.2, hem=hem, gold=gold,
                            grip_color=grip_color, tassel_color=tassel_color)
                total_px += w * h
                if cube.mat == "rune":
                    sub = glow[y:y + h, x:x + w, :]
                    sub[:] = glow_color
                    if w >= 3:
                        c = w // 2
                        sub[:, c, :] = (250, 255, 255)
                    glow_px += w * h
    os.makedirs(os.path.dirname(os.path.abspath(out_png)), exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB").save(out_png)
    if out_glow_png:
        Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGB").save(out_glow_png)
    ratio = glow_px / float(max(1, total_px))
    return ratio, {"total_face_px": total_px, "glow_px": glow_px}


# ---------------------------------------------------------------------------
# 3) 图标：把渲染出的 RGBA 剪影收成 32×32
# ---------------------------------------------------------------------------

def make_icon(rgba_path, out_path, size=32, pad=2, outline=(11, 11, 16)):
    im = Image.open(rgba_path).convert("RGBA")
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)
    inner = size - pad * 2 - 2
    scale = min(inner / float(im.width), inner / float(im.height))
    nw = max(1, int(round(im.width * scale)))
    nh = max(1, int(round(im.height * scale)))
    im = im.resize((nw, nh), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox = (size - nw) // 2
    oy = (size - nh) // 2
    canvas.alpha_composite(im, (ox, oy))
    # 1px 外描边：膨胀 alpha 后补深色
    alpha = np.array(canvas)[:, :, 3]
    mask = alpha > 24
    dil = mask.copy()
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        dil |= np.roll(np.roll(mask, dy, axis=0), dx, axis=1)
    arr = np.array(canvas)
    edge = dil & ~mask
    arr[edge, :3] = outline
    arr[edge, 3] = 255
    out = Image.fromarray(arr, "RGBA")
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    out.save(out_path)
    return out_path
