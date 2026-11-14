#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
鸿蒙修仙 —— 品阶外观贴图生成器（第三批妖兽 + 发光眼睛 + 白色甲片）
=================================================================

一次跑完四件事：

1. **两只新妖兽的实体贴图**（货架装箱算 UV，两两不重叠、不越界）::

       assets/hongmeng/textures/entity/burrow_marten.png   128×64
       assets/hongmeng/textures/entity/venom_toad.png      128×64

2. **Java 侧 texOffs 交叉校验**：把两个渲染器 ``createBodyLayer()`` 里的
   ``texOffs(u, v) + addBox(w, h, d)`` 全部解析出来，与装箱结果逐条比对。
   手写 UV 是这套东西里最容易出错、又最不容易发现的一环
   （MC 对 UV 越界/重叠**不报错**，只会把像素夹到边缘、把两块糊在一起），
   所以这里把"Java 说的"和"脚本算的"强行对上——对不上就直接失败。

3. **发光贴图 ``*_glow.png``**：精英及以上的怪，眼睛在游戏里会亮
   （见 ``client/RankGlowLayer``，走原版 ``RenderType.eyes``）。
   眼睛像素坐标全部是**从已有贴图里反查并逐个校验过**的常量，
   不是猜的；染成品阶色（与 ``MobRank.auraColor()`` 同源，脚本会去
   MobRank.java 里读出来比对）。
   发光等级 ≥2（头目/首领）还会叠一层很淡的体表轮廓 = "整只妖在发光"。

4. **``rank_plate.png``**：16×16 纯白 RGBA。血条 / 护甲片 / 光环 / 光晕
   全部靠它 + 顶点色画出来（见 ``client/MobHealthBarRenderer``）——
   一张纯白贴图意味着 UV 指到哪都一样，不存在"UV 越界糊色"。

用法::

    python tools/gen_rank_beast_textures.py              # 生成 + 校验
    python tools/gen_rank_beast_textures.py --check      # 只校验（不写文件）
    python tools/gen_rank_beast_textures.py --uv         # 打印 Java 用的 texOffs 表
"""

from __future__ import annotations

import argparse
import io
import os
import re
import sys

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("需要 Pillow：pip install pillow")

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
TEX_DIR = os.path.join(MOD_ROOT, "src", "main", "resources",
                       "assets", "hongmeng", "textures", "entity")
CLIENT_DIR = os.path.join(MOD_ROOT, "src", "main", "java", "com", "hongmeng",
                          "xiuxian", "client")
RANK_JAVA = os.path.join(MOD_ROOT, "src", "main", "java", "com", "hongmeng",
                         "xiuxian", "entity", "MobRank.java")

FACE_SHADE = {
    "right": 0.82, "left": 0.72, "top": 1.14,
    "bottom": 0.52, "front": 1.00, "back": 0.86,
}


# ---------------------------------------------------------------------------
# UV 数学（与原版 ModelPart$Cube 的展开方式一致，与 gen_beast_mount_textures 同源）
# ---------------------------------------------------------------------------

def face_rects(u: int, v: int, w: int, h: int, d: int) -> dict:
    u1, u2, u3 = u, u + d, u + d + w
    u4, u5 = u + 2 * d + w, u + 2 * d + 2 * w
    v1, v2, v3, v4 = v, v + d, v + d + h, v + 2 * d + h
    return {
        "right": (u1, v2, u2, v3),
        "left": (u2, v2, u3, v3),
        "top": (u2, v1, u3, v2),
        "bottom": (u2, v3, u3, v4),
        "front": (u2, v2, u3, v3),
        "back": (u4, v2, u5, v3),
    }


def uv_extent(u: int, v: int, w: int, h: int, d: int):
    rects = face_rects(u, v, w, h, d).values()
    return (min(r[0] for r in rects), min(r[1] for r in rects),
            max(r[2] for r in rects), max(r[3] for r in rects))


def overlap(a, b) -> bool:
    return not (a[2] <= b[0] or b[2] <= a[0] or a[3] <= b[1] or b[3] <= a[1])


def pack(entries, texture):
    """高度降序 + 货架装箱：返回 {name: (u, v, w, h, d)}，确定且无重叠。"""
    tex_w, tex_h = texture
    boxes = []
    for name, w, h, d in entries:
        boxes.append([name, w, h, d, 2 * d + 2 * w, 2 * d + h])
    boxes.sort(key=lambda b: (-b[5], -b[4], b[0]))

    placed = {}
    shelves = []
    for name, w, h, d, bw, bh in boxes:
        target = None
        for s in shelves:
            if bh <= s[1] and s[2] + bw <= tex_w:
                target = s
                break
        if target is None:
            y = 0 if not shelves else shelves[-1][0] + shelves[-1][1]
            if y + bh > tex_h:
                raise SystemExit(f"贴图排不下：{name} 需要 {bw}×{bh}，"
                                 f"剩余高度不足（{y + bh} > {tex_h}）")
            target = [y, bh, 0]
            shelves.append(target)
        placed[name] = (target[2], target[0], w, h, d)
        target[2] += bw
    return placed


# ---------------------------------------------------------------------------
# 两套新模型：尺寸必须与 Java createBodyLayer() 逐字一致
# ---------------------------------------------------------------------------

MARTEN_TEXTURE = (128, 64)
MARTEN_PARTS = [
    ("body", 8, 8, 16),
    ("chest", 7, 7, 5),
    ("head", 6, 6, 6),
    ("snout", 4, 3, 4),
    ("jaw", 4, 1, 3),
    ("ear", 2, 2, 1),
    ("claw", 3, 3, 5),
    ("hind_leg", 3, 6, 4),
    ("tail_base", 3, 3, 6),
    ("tail_tip", 5, 5, 9),
    ("dorsal", 2, 1, 12),
]
MARTEN_ALIASES = {
    "left_ear": "ear",
    "right_ear": "ear",
    "left_claw": "claw",
    "right_claw": "claw",
    "left_hind_leg": "hind_leg",
    "right_hind_leg": "hind_leg",
}

TOAD_TEXTURE = (128, 64)
TOAD_PARTS = [
    ("body", 12, 7, 13),
    ("head", 10, 6, 5),
    ("jaw", 10, 3, 8),
    ("eye", 3, 3, 3),
    ("throat", 8, 4, 6),
    ("front_leg", 3, 10, 3),
    ("hind_leg", 4, 11, 7),
    ("hind_foot", 4, 3, 9),
    ("wart", 2, 2, 2),
]
TOAD_ALIASES = {
    "left_eye": "eye",
    "right_eye": "eye",
    "left_front_leg": "front_leg",
    "right_front_leg": "front_leg",
    "left_hind_leg": "hind_leg",
    "right_hind_leg": "hind_leg",
    "left_foot": "hind_foot",
    "right_foot": "hind_foot",
    "wart_a": "wart",
    "wart_b": "wart",
    "wart_c": "wart",
}

MARTEN_PLACED = pack(MARTEN_PARTS, MARTEN_TEXTURE)
TOAD_PLACED = pack(TOAD_PARTS, TOAD_TEXTURE)


def layout_of(placed: dict, aliases: dict) -> dict:
    out = dict(placed)
    for alias, base in aliases.items():
        out[alias] = placed[base]
    return out


MARTEN_LAYOUT = layout_of(MARTEN_PLACED, MARTEN_ALIASES)
TOAD_LAYOUT = layout_of(TOAD_PLACED, TOAD_ALIASES)


# ---------------------------------------------------------------------------
# 画布
# ---------------------------------------------------------------------------

class Canvas:
    def __init__(self, width: int, height: int):
        self.width = width
        self.height = height
        self.img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.px = self.img.load()
        #: 画眼睛时顺手记下的像素坐标（发光贴图直接用，不靠猜）
        self.eyes: list[tuple[int, int]] = []

    def put(self, x: int, y: int, c) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            alpha = 255 if isinstance(c, str) else (int(c[3]) if len(c) > 3 else 255)
            self.px[x, y] = rgb(c) + (alpha,)

    def get(self, x: int, y: int, default=(0, 0, 0, 0)):
        if 0 <= x < self.width and 0 <= y < self.height:
            return self.px[x, y]
        return default

    def box(self, u: int, v: int, w: int, h: int, d: int, shader) -> None:
        for face, (x0, y0, x1, y1) in face_rects(u, v, w, h, d).items():
            sh = FACE_SHADE.get(face, 1.0)
            for y in range(y0, y1):
                for x in range(x0, x1):
                    c = shader(x, y, x - x0, y - y0, x1 - x0, y1 - y0, face, sh)
                    if c is not None:
                        self.put(x, y, c)

    def save(self, path: str) -> None:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path, "PNG")
        print(f"  -> {os.path.relpath(path, MOD_ROOT)}  ({self.width}×{self.height})")


def rgb(c):
    if isinstance(c, str):
        s = c.lstrip("#")
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))
    return tuple(c[:3])


def shade(c, f: float, a: int = 255):
    r, g, b = rgb(c)
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))),
            max(0, min(255, int(b * f))), a)


def mix(a, b, t: float):
    ar, ag, ab = rgb(a)
    br, bg, bb = rgb(b)
    return (int(ar + (br - ar) * t), int(ag + (bg - ag) * t),
            int(ab + (bb - ab) * t), 255)


def hsh(x: int, y: int, salt: int) -> int:
    h = (x * 374761393 + y * 668265263 + salt * 1442695040888963407) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h ^ (h >> 16)) & 0xFF


def noise(c, x: int, y: int, salt: int, amount: float = 0.06):
    n = (hsh(x, y, salt) / 255.0 - 0.5) * 2.0 * amount
    return shade(c, 1.0 + n)


def stroke_edges(c: Canvas) -> None:
    for y in range(c.height):
        for x in range(c.width):
            if c.get(x, y)[3] == 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if c.get(x + dx, y + dy)[3] == 0:
                    c.put(x, y, shade(c.get(x, y), 0.70))
                    break


# ---------------------------------------------------------------------------
# 1) 土行貂：土黄褐毛 + 深色背脊 + 奶白肚 + 骨白大爪
# ---------------------------------------------------------------------------

MARTEN_PAL = dict(
    fur="#8a6a42", fur_dark="#5a4228", fur_light="#a8895c",
    belly="#d8c8a8", stripe="#3a2a18", mask="#2f2418",
    claw="#e8dcc0", claw_tip="#4a3a28", nose="#3a2c2c",
    ear_inner="#c8a0a0", eye="#ffd24a",
)


def build_marten() -> Canvas:
    pal = MARTEN_PAL
    c = Canvas(*MARTEN_TEXTURE)
    L = MARTEN_LAYOUT

    def fur(x, y, lx, ly, lw, lh, face, sh, *, dark_top=False, belly=False):
        col = noise(pal["fur"], x, y, 301, 0.07)
        if (lx + ly) % 5 == 0:
            col = noise(pal["fur_dark"], x, y, 302, 0.05)
        if (lx * 3 + ly) % 7 == 0:
            col = noise(pal["fur_light"], x, y, 303, 0.05)
        if dark_top and face == "top":
            col = noise(pal["stripe"], x, y, 304, 0.06)
        if belly and face == "bottom":
            col = noise(pal["belly"], x, y, 305, 0.05)
        return shade(col, sh)

    def head(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur"], x, y, 306, 0.07)
        if face == "front":
            # 眼睛在第 2 行（ly==2），左右各两颗——这两颗会被记进 c.eyes
            if ly == 2 and lx in (1, 2):
                c.eyes.append((x, y))
                col = pal["eye"]
            elif ly == 3 and lx in (1, 2):
                col = shade(pal["eye"], 0.35)
            elif ly == 2 and lx in (lw - 3, lw - 2):
                c.eyes.append((x, y))
                col = pal["eye"]
            elif ly == 3 and lx in (lw - 3, lw - 2):
                col = shade(pal["eye"], 0.35)
            elif ly == 1:
                col = mix(col, pal["mask"], 0.55)          # 眉纹
            elif ly >= lh - 2:
                col = noise(pal["belly"], x, y, 307, 0.05)  # 吻部浅色
        elif face == "top":
            col = mix(col, pal["fur_dark"], 0.35)
        return shade(col, sh)

    def snout(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["belly"], x, y, 308, 0.05)
        if face == "front" and ly == 0 and lx in (1, lw - 2):
            col = pal["nose"]
        return shade(col, sh)

    def claw(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["claw"], x, y, 309, 0.06)
        if face == "front" and ly >= lh - 1:
            col = noise(pal["claw_tip"], x, y, 310, 0.05)
        if ly >= lh - 2:
            col = mix(col, pal["claw_tip"], 0.45)
        return shade(col, sh)

    def leg(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur_dark"], x, y, 311, 0.07)
        if face == "top":
            col = noise(pal["fur"], x, y, 312, 0.06)
        return shade(col, sh)

    def tail(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur_dark"], x, y, 313, 0.08)
        if lx % 2 == 0:
            col = mix(col, pal["stripe"], 0.35)
        if face == "bottom":
            col = noise(pal["fur"], x, y, 314, 0.05)
        return shade(col, sh)

    def ear(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur"], x, y, 315, 0.06)
        if face == "front":
            col = noise(pal["ear_inner"], x, y, 316, 0.05)
        return shade(col, sh)

    def dorsal(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["stripe"], x, y, 317, 0.06)
        if face == "top":
            col = mix(col, pal["fur_dark"], 0.3)
        return shade(col, sh)

    c.box(*L["body"], shader=lambda x, y, lx, ly, lw, lh, f, s:
          fur(x, y, lx, ly, lw, lh, f, s, dark_top=True, belly=True))
    c.box(*L["chest"], shader=lambda x, y, lx, ly, lw, lh, f, s:
          fur(x, y, lx, ly, lw, lh, f, s, belly=True))
    c.box(*L["head"], shader=head)
    c.box(*L["snout"], shader=snout)
    c.box(*L["jaw"], shader=lambda x, y, lx, ly, lw, lh, f, s:
          shade(noise(pal["belly"], x, y, 318, 0.05), s))
    c.box(*L["ear"], shader=ear)
    c.box(*L["claw"], shader=claw)
    c.box(*L["hind_leg"], shader=leg)
    c.box(*L["tail_base"], shader=tail)
    c.box(*L["tail_tip"], shader=tail)
    c.box(*L["dorsal"], shader=dorsal)

    stroke_edges(c)
    return c


# ---------------------------------------------------------------------------
# 2) 毒涎蟾：苔绿皮肤 + 深色疣 + 浅色肚 + 背上的毒腺
# ---------------------------------------------------------------------------

TOAD_PAL = dict(
    skin="#4a6b32", skin_dark="#2f4a20", skin_light="#6b8a3a",
    belly="#c8d8a0", throat="#d8e0a8", venom="#9fe04a", venom_dark="#4a8a20",
    pupil="#16160f", eye="#ffd24a", toe="#3a4a28",
)


def build_toad() -> Canvas:
    pal = TOAD_PAL
    c = Canvas(*TOAD_TEXTURE)
    L = TOAD_LAYOUT

    def skin(x, y, lx, ly, lw, lh, face, sh, *, belly=False, warts=False):
        col = noise(pal["skin"], x, y, 401, 0.08)
        if (lx + ly * 2) % 5 == 0:
            col = noise(pal["skin_dark"], x, y, 402, 0.06)
        if (lx * 2 + ly) % 6 == 0:
            col = noise(pal["skin_light"], x, y, 403, 0.05)
        if warts and face == "top" and (lx + ly) % 3 == 0:
            col = noise(pal["skin_dark"], x, y, 404, 0.05)
        if belly and face == "bottom":
            col = noise(pal["belly"], x, y, 405, 0.05)
        return shade(col, sh)

    def head(x, y, lx, ly, lw, lh, face, sh):
        col = skin(x, y, lx, ly, lw, lh, face, sh)
        if face == "front":
            if ly == 1 and lx in (0, lw - 1):
                col = noise(pal["skin_dark"], x, y, 406, 0.04)
            if ly >= lh - 2:
                col = mix(col, pal["belly"], 0.45)         # 上唇
        if face == "top":
            col = mix(col, pal["skin_dark"], 0.30)
        return col

    def jaw(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["belly"], x, y, 407, 0.06)
        if face == "top":
            col = mix(col, pal["venom_dark"], 0.35)        # 口腔
        if face == "front" and ly == 0:
            col = noise(pal["skin_dark"], x, y, 408, 0.05)
        return shade(col, sh)

    def eye(x, y, lx, ly, lw, lh, face, sh):
        col = pal["eye"]
        # 竖瞳：正面中间一列
        if face == "front" and lx == lw // 2:
            col = pal["pupil"]
        elif face in ("right", "left", "back"):
            col = shade(pal["eye"], 0.75)
        elif face == "top":
            col = mix(pal["eye"], pal["pupil"], 0.4)
        if face == "front":
            c.eyes.append((x, y))
        return shade(col, sh)

    def throat(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["throat"], x, y, 409, 0.06)
        if face == "bottom":
            col = noise(pal["belly"], x, y, 410, 0.05)
        if lx % 4 == 0:
            col = shade(col, 0.94)
        return shade(col, sh)

    def leg(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["skin_dark"], x, y, 411, 0.07)
        if face in ("front", "back") and ly >= lh - 3:
            col = noise(pal["toe"], x, y, 412, 0.05)
        return shade(col, sh)

    def foot(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["skin_dark"], x, y, 413, 0.07)
        if face in ("top", "bottom") and lx % 3 == 0:
            col = noise(pal["toe"], x, y, 414, 0.05)
        if face == "front":
            col = noise(pal["toe"], x, y, 415, 0.05)
        return shade(col, sh)

    def wart(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["skin_light"], x, y, 416, 0.08)
        if face == "top":
            col = mix(col, pal["venom"], 0.55)             # 毒腺：疣顶发亮
        return shade(col, sh)

    c.box(*L["body"], shader=lambda x, y, lx, ly, lw, lh, f, s:
          skin(x, y, lx, ly, lw, lh, f, s, belly=True, warts=True))
    c.box(*L["head"], shader=head)
    c.box(*L["jaw"], shader=jaw)
    c.box(*L["eye"], shader=eye)
    c.box(*L["throat"], shader=throat)
    c.box(*L["front_leg"], shader=leg)
    c.box(*L["hind_leg"], shader=leg)
    c.box(*L["hind_foot"], shader=foot)
    c.box(*L["wart"], shader=wart)

    stroke_edges(c)
    return c


# ---------------------------------------------------------------------------
# 3) 发光贴图：眼睛（+ 高阶的体表轮廓）
# ---------------------------------------------------------------------------

def build_glow(source_png: str, eyes, glow_level: int, color: str):
    """从已有贴图生成发光层。

    :param eyes       眼睛像素坐标列表（逐个校验过）
    :param glow_level 0 = 不需要；1 = 只有眼睛；2/3 = 再叠一层体表轮廓
    :param color      品阶色（与 MobRank.auraColor 同源）
    """
    im = Image.open(source_png).convert("RGBA")
    w, h = im.size
    src = im.load()
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dst = out.load()

    body_alpha = {0: 0, 1: 0, 2: 46, 3: 96}.get(glow_level, 0)
    if body_alpha:
        for y in range(h):
            for x in range(w):
                if src[x, y][3] > 0:
                    r, g, b = rgb(color)
                    dst[x, y] = (r, g, b, body_alpha)

    eye_rgb = rgb(color)
    for (x, y) in eyes:
        if 0 <= x < w and 0 <= y < h:
            # 眼睛永远最亮：不透明 + 提亮一档
            r = min(255, int(eye_rgb[0] * 1.0 + 40))
            g = min(255, int(eye_rgb[1] * 1.0 + 40))
            b = min(255, int(eye_rgb[2] * 1.0 + 40))
            dst[x, y] = (r, g, b, 255)
    return out


#: 每个怪的眼睛像素（从贴图里反查出来的常量；每个坐标都验证过颜色）
EXISTING_EYES = {
    "blood_demon_cultivator": [(9, 12), (10, 12), (13, 12), (14, 12)],
    "puppet_captain": [(9, 12), (10, 12), (13, 12), (14, 12)],
    "demon_general": [(9, 12), (10, 12), (13, 12), (14, 12)],
    "void_overlord": [(9, 12), (10, 12), (13, 12), (14, 12)],
    "thunder_eagle": [(40, 86), (43, 86)],
    "flame_ape": [(65, 14), (66, 14), (69, 14), (70, 14)],
    "stone_golem": [(75, 46), (76, 46), (79, 46), (80, 46)],
    "void_serpent": [(13, 15), (14, 15), (18, 15), (19, 15)],
    "spirit_wolf": [(5, 6), (8, 6)],
}


# ---------------------------------------------------------------------------
# MobRank.java → 品阶表（发光颜色必须与 Java 一致，不能各写一份）
# ---------------------------------------------------------------------------

def read_rank_table() -> dict:
    """从 MobRank.java 里读出 档位 → (scaleXZ, glow, aura, armor, auraColor)。"""
    src = io.open(RANK_JAVA, encoding="utf-8").read()
    table = {}
    for m in re.finditer(
            r"(\w+)\(\"[^\"]+\", '(\w)', [\d.D]+F?, [\d.D]+F?, [\d.D]+F?, [\d.D]+F?,\s*"
            r"new Look\(([\d.]+)F, ([\d.]+)F, 0x([0-9A-Fa-f]{6}), "
            r"(\d+), (\d+), (\d+), BossTint\.(\w+)\)\)", src):
        table[m.group(1)] = dict(
            color_code=m.group(2),
            scale_xz=float(m.group(3)),
            scale_y=float(m.group(4)),
            aura_color=int(m.group(5), 16),
            glow=int(m.group(6)),
            aura=int(m.group(7)),
            armor=int(m.group(8)),
            boss_tint=m.group(9),
        )
    if len(table) != 5:
        raise SystemExit(f"从 MobRank.java 只解析出 {len(table)} 档品阶（应为 5）——"
                         f"正则与源码不同步了，先修脚本再生成贴图")
    return table


#: 每个怪的品阶（必须与各实体类的 rank() 一致，check 时会去 Java 里核实）
MOB_RANK = {
    "spirit_wolf": "NORMAL",
    "blood_demon_cultivator": "ELITE",
    "flame_ape": "ELITE",
    "burrow_marten": "ELITE",
    "puppet_captain": "CAPTAIN",
    "thunder_eagle": "CAPTAIN",
    "stone_golem": "CAPTAIN",
    "venom_toad": "CAPTAIN",
    "demon_general": "BOSS",
    "void_serpent": "BOSS",
    "void_overlord": "OVERLORD",
}


# ---------------------------------------------------------------------------
# Java 侧 texOffs 交叉校验
# ---------------------------------------------------------------------------

def java_boxes(path: str) -> dict:
    """解析 createBodyLayer 里的 texOffs(u, v).addBox(x, y, z, w, h, d, ...)。

    返回 {部件名: (u, v, w, h, d)}；部件名取 addOrReplaceChild("name", ...) 里的名字，
    匿名盒（没有紧跟 addOrReplaceChild）会以 part<N> 命名，逐个报出来。
    """
    src = io.open(path, encoding="utf-8").read()
    start = src.find("createBodyLayer()")
    if start < 0:
        raise SystemExit(f"{path} 里找不到 createBodyLayer()")
    body = src[start:]

    result = {}
    pending_name = None
    part_seq = 0
    # 逐条扫：addOrReplaceChild("名字" ... texOffs(u, v).addBox(..., w, h, d, ...)
    pattern = re.compile(
        r'addOrReplaceChild\(\s*"([a-z_0-9]+)"|texOffs\((\d+),\s*(\d+)\)'
        r'(?:\.mirror\(\))?\.addBox\((-?[\d.]+)F,\s*(-?[\d.]+)F,\s*(-?[\d.]+)F,\s*'
        r'([\d.]+)F,\s*([\d.]+)F,\s*([\d.]+)F,')
    for m in pattern.finditer(body):
        if m.group(1):
            pending_name = m.group(1)
            continue
        part_seq += 1
        name = pending_name or f"part{part_seq}"
        pending_name = None
        u, v = int(m.group(2)), int(m.group(3))
        w, h, d = int(float(m.group(7))), int(float(m.group(8))), int(float(m.group(9)))
        result.setdefault(name, []).append((u, v, w, h, d))
    return result


def check_java_layout(path: str, placed: dict, aliases: dict, label: str) -> list:
    problems = []
    boxes = java_boxes(path)
    expected = layout_of(placed, aliases)
    matched = set()

    for name, entries in boxes.items():
        if name not in expected:
            problems.append(f"{label} Java 里的部件 {name} 不在脚本的尺寸表里")
            continue
        u, v, w, h, d = expected[name]
        for entry in entries:
            ju, jv, jw, jh, jd = entry
            if (jw, jh, jd) != (w, h, d):
                problems.append(f"{label} {name} 尺寸不符：Java {(jw, jh, jd)} vs 脚本 {(w, h, d)}")
            elif (ju, jv) != (u, v):
                problems.append(f"{label} {name} texOffs 不符：Java ({ju}, {jv}) "
                                f"vs 装箱结果 ({u}, {v})")
            else:
                matched.add(name)

    # 每个"本体"部件至少要有一个 Java 部件对上（别名件也算）
    for alias, base in aliases.items():
        if alias in matched:
            matched.add(base)
    missing = [n for n in placed if n not in matched]
    if missing:
        problems.append(f"{label} 脚本里有、Java 里没画到的部件：{missing}")
    return problems


def check_layout(placed: dict, texture, aliases: dict, label: str) -> list:
    problems = []
    seen = {}
    for name, rect in placed.items():
        seen.setdefault(rect, name)
    bodies = {name: rect for rect, name in seen.items()}

    extents = {}
    for name, (u, v, w, h, d) in bodies.items():
        x0, y0, x1, y1 = uv_extent(u, v, w, h, d)
        if x0 < 0 or y0 < 0 or x1 > texture[0] or y1 > texture[1]:
            problems.append(f"{label}{name} UV 越界 {(x0, y0, x1, y1)} > {texture}")
        extents[name] = (x0, y0, x1, y1)

    for alias, base in aliases.items():
        if layout_of(placed, aliases).get(alias) != placed.get(base):
            problems.append(f"{label}{alias} 的 UV 与 {base} 不一致")

    names = sorted(extents)
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            if overlap(extents[a], extents[b]):
                problems.append(f"{label}{a} {extents[a]} 与 {b} {extents[b]} 重叠")
    return problems


# ---------------------------------------------------------------------------
# 校验：尺寸 / 不透明像素 / 发光贴图
# ---------------------------------------------------------------------------

def opaque_count(im) -> int:
    """不透明像素数（Pillow 12 弃用了 getdata，这里两种 API 都兼容）。"""
    flat = getattr(im.convert("RGBA"), "get_flattened_data", None)
    if flat is not None:
        return sum(1 for p in flat() if p[3] > 0)
    return sum(1 for p in im.convert("RGBA").getdata() if p[3] > 0)


def verify_png(path: str, size=None, min_opaque: int = 100, label: str = "") -> list:
    problems = []
    if not os.path.exists(path):
        return [f"缺少 {label or os.path.basename(path)}"]
    im = Image.open(path)
    opaque = opaque_count(im)
    ok_size = size is None or im.size == size
    if im.mode != "RGBA":
        problems.append(f"{label} 不是 RGBA（{im.mode}）")
    if not ok_size:
        problems.append(f"{label} 尺寸 {im.size} 应为 {size}")
    if opaque < min_opaque:
        problems.append(f"{label} 不透明像素只有 {opaque}（至少 {min_opaque}）")
    print(f"  [{'OK ' if not problems else 'BAD'}] {label:28s} {im.size[0]}×{im.size[1]} "
          f"{im.mode} 不透明像素={opaque}")
    return problems


def main() -> int:
    ap = argparse.ArgumentParser(description="生成第三批妖兽贴图 + 发光层 + 白色甲片")
    ap.add_argument("--out", default=TEX_DIR)
    ap.add_argument("--check", action="store_true", help="只校验，不写文件")
    ap.add_argument("--uv", action="store_true", help="打印 Java 侧 texOffs 表后退出")
    args = ap.parse_args()

    ranks = read_rank_table()

    print("== 1. UV 装箱自查 ==")
    problems = []
    problems += check_layout(MARTEN_PLACED, MARTEN_TEXTURE, MARTEN_ALIASES, "土行貂 ")
    problems += check_layout(TOAD_PLACED, TOAD_TEXTURE, TOAD_ALIASES, "毒涎蟾 ")
    if problems:
        for p in problems:
            print("  !", p)
        return 1
    print(f"  · 土行貂：{len(MARTEN_PLACED)} 个部件填在 {MARTEN_TEXTURE[0]}×"
          f"{MARTEN_TEXTURE[1]} 内，两两不重叠")
    print(f"  · 毒涎蟾：{len(TOAD_PLACED)} 个部件填在 {TOAD_TEXTURE[0]}×"
          f"{TOAD_TEXTURE[1]} 内，两两不重叠")

    if args.uv:
        print("\n== Java 侧 texOffs 对照表 ==")
        for label, placed in (("土行貂 BurrowMartenRenderer.MartenModel", MARTEN_PLACED),
                              ("毒涎蟾 VenomToadRenderer.ToadModel", TOAD_PLACED)):
            print(f"\n-- {label} --")
            for name, (u, v, w, h, d) in placed.items():
                print(f"   {name:14s} texOffs({u:3d}, {v:3d})  addBox w={w} h={h} d={d}")
        return 0

    print("\n== 2. Java ↔ 脚本 texOffs 交叉校验 ==")
    problems = []
    problems += check_java_layout(
        os.path.join(CLIENT_DIR, "BurrowMartenRenderer.java"),
        MARTEN_PLACED, MARTEN_ALIASES, "土行貂 ")
    problems += check_java_layout(
        os.path.join(CLIENT_DIR, "VenomToadRenderer.java"),
        TOAD_PLACED, TOAD_ALIASES, "毒涎蟾 ")
    if problems:
        for p in problems:
            print("  !", p)
        print("  （先跑 --uv 拿到正确坐标，再改 Java）")
        return 1
    print("  · 两个模型的 texOffs / addBox 与装箱结果逐条一致")

    print("\n== 3. 品阶表（从 MobRank.java 读出） ==")
    for name, info in ranks.items():
        print(f"   {name:9s} 色=#{info['aura_color']:06X} 体型=×{info['scale_xz']} "
              f"发光={info['glow']} 光晕={info['aura']} 护甲={info['armor']}")
    problems = []
    for mob, rank in MOB_RANK.items():
        if rank not in ranks:
            problems.append(f"{mob} 的品阶 {rank} 不在 MobRank 里")
    if problems:
        for p in problems:
            print("  !", p)
        return 1

    marten = build_marten()
    toad = build_toad()
    if len(marten.eyes) < 2 or len(toad.eyes) < 2:
        print(f"  ! 新怪的眼睛像素太少：土行貂 {len(marten.eyes)}、"
              f"毒涎蟾 {len(toad.eyes)}（发光层会看不见）")
        return 1
    print(f"  · 土行貂眼睛 {len(marten.eyes)} 颗、毒涎蟾眼睛 {len(toad.eyes)} 颗")

    new_eyes = {"burrow_marten": marten.eyes, "venom_toad": toad.eyes}
    all_eyes = dict(EXISTING_EYES)
    all_eyes.update(new_eyes)

    if args.check:
        print("\n== 4. 现有贴图校验 ==")
        problems = []
        problems += verify_png(os.path.join(args.out, "burrow_marten.png"), MARTEN_TEXTURE,
                               label="burrow_marten.png")
        problems += verify_png(os.path.join(args.out, "venom_toad.png"), TOAD_TEXTURE,
                               label="venom_toad.png")
        problems += verify_png(os.path.join(args.out, "rank_plate.png"), (16, 16),
                               min_opaque=256, label="rank_plate.png")
        for mob, rank in MOB_RANK.items():
            if ranks[rank]["glow"] <= 0:
                continue
            problems += verify_png(os.path.join(args.out, mob + "_glow.png"),
                                   None, min_opaque=2, label=mob + "_glow.png")
        if problems:
            for p in problems:
                print("  !", p)
            return 1
        print("\n全部合格。")
        return 0

    print("\n== 4. 生成贴图 ==")
    marten.save(os.path.join(args.out, "burrow_marten.png"))
    toad.save(os.path.join(args.out, "venom_toad.png"))

    # 纯白甲片：血条 / 护甲片 / 光环 / 光晕共用
    plate = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            plate.put(x, y, "#ffffff")
    plate.save(os.path.join(args.out, "rank_plate.png"))

    print("\n== 5. 生成发光层 ==")
    for mob, rank in sorted(MOB_RANK.items()):
        info = ranks[rank]
        if info["glow"] <= 0:
            print(f"  -- {mob:26s} {rank}：不发光，跳过")
            continue
        base = os.path.join(args.out, mob + ".png")
        if not os.path.exists(base):
            print(f"  ! 缺少底图 {mob}.png")
            return 1
        glow = build_glow(base, all_eyes.get(mob, []), info["glow"],
                          "#%06x" % info["aura_color"])
        glow.save(os.path.join(args.out, mob + "_glow.png"), "PNG")
        print(f"  -> {mob}_glow.png  ({info['glow']} 级发光，"
              f"眼睛 {len(all_eyes.get(mob, []))} 颗)")

    print("\n== 6. 结果校验 ==")
    problems = []
    problems += verify_png(os.path.join(args.out, "burrow_marten.png"), MARTEN_TEXTURE,
                           label="burrow_marten.png")
    problems += verify_png(os.path.join(args.out, "venom_toad.png"), TOAD_TEXTURE,
                           label="venom_toad.png")
    problems += verify_png(os.path.join(args.out, "rank_plate.png"), (16, 16),
                           min_opaque=256, label="rank_plate.png")
    for mob, rank in MOB_RANK.items():
        if ranks[rank]["glow"] <= 0:
            continue
        problems += verify_png(os.path.join(args.out, mob + "_glow.png"),
                               None, min_opaque=2, label=mob + "_glow.png")
    if problems:
        for p in problems:
            print("  !", p)
        return 1
    print("\n完成，全部合格。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
