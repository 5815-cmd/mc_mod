#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
鸿蒙修仙 —— 灵植 / 灵田土 / 丹炉 贴图与模型生成器
=================================================

生成（全部 16×16 RGBA，无渐变、无抗锯齿）：

    textures/block/<herb>_stage0..3.png      六阶灵植 × 4 个生长阶段   = 24
    textures/block/lingtian_soil.png         灵田土                     =  1
    textures/block/alchemy_furnace_side.png  丹炉：侧面                 =  1
    textures/block/alchemy_furnace_top.png   丹炉：顶面                 =  1
    textures/block/alchemy_furnace_front.png 丹炉：正面（未点火）       =  1
    textures/block/alchemy_furnace_front_lit.png 丹炉：正面（炼丹中）   =  1
    textures/item/<herb>_seed.png            种子                       =  6
    textures/item/<herb>_petal.png           花瓣（炼丹材料）           =  6
                                                              合计  = 41

以及对应的 models/block、models/item、blockstates JSON。

画法沿用 ``tools/gen_item_textures.py`` / ``xiuxian-mod/tools/gen_beast_item_textures.py``
的既有口径：

  1. 先用 **ASCII 网格**定义剪影（这里是几何循环生成网格，house style 明确允许
     "网格或几何循环"），每个字符是一个角色；
  2. 角色映射到调色板：主体 B / 主体亮 H / 主体暗 S / 点缀 A / 宝石 G / 高光 W；
  3. 主体按"左上受光、右下压暗"分三级明暗 + 一点点确定性噪点，**没有渐变**；
  4. 1px 描边由**剪影的八邻域膨胀**自动补（不是"旁边是透明就描边"）；
  5. 整方块贴图（灵田土 / 丹炉）走另一条渲染路径：**铺满 16×16、不描边**
     ——方块贴图不能有透明像素，否则模型看着像漏了洞。

用法::

    python xiuxian-mod/tools/gen_herb_alchemy_textures.py            # 写入资源目录
    python xiuxian-mod/tools/gen_herb_alchemy_textures.py --check    # 只校验
    python xiuxian-mod/tools/gen_herb_alchemy_textures.py --preview  # 出一张预览拼图
"""

from __future__ import annotations

import argparse
import json
import os
import sys

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("需要 Pillow：pip install pillow")

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
RES = os.path.join(MOD_ROOT, "src", "main", "resources", "assets", "hongmeng")
TEX_BLOCK = os.path.join(RES, "textures", "block")
TEX_ITEM = os.path.join(RES, "textures", "item")
MODEL_BLOCK = os.path.join(RES, "models", "block")
MODEL_ITEM = os.path.join(RES, "models", "item")
BLOCKSTATES = os.path.join(RES, "blockstates")

SIZE = 16
MOD_ID = "hongmeng"

# ---------------------------------------------------------------------------
# 六阶灵植
#   shape: 剪影风格          tier: 品阶（进 docs/herb-alchemy-spec.md 的表）
#   调色板：body/body_hi/body_sh 是茎叶，accent 是花，gem 是丹心/斑点
# ---------------------------------------------------------------------------

HERBS = [
    dict(id="xuelian_grass", cn="雪莲草", tier=1, shape="lotus",
         body="#6fae5c", body_hi="#9ad37f", body_sh="#3c6631",
         accent="#eef6ff", gem="#f3c9d9", white="#ffffff"),
    dict(id="huoyan_hua", cn="火焰花", tier=2, shape="flame",
         body="#7a5a2a", body_hi="#b08a44", body_sh="#463018",
         accent="#ff7a22", gem="#ffd24a", white="#ffe9c0"),
    dict(id="hanbing_teng", cn="寒冰藤", tier=3, shape="vine",
         body="#3f6f8a", body_hi="#6fb6d4", body_sh="#22414f",
         accent="#8fd8f0", gem="#eaffff", white="#ffffff"),
    dict(id="leiyin_cao", cn="雷音草", tier=4, shape="spike",
         body="#4a3f8a", body_hi="#7a68c8", body_sh="#2a2352",
         accent="#a86fe8", gem="#fff07a", white="#f0e8ff"),
    dict(id="kongming_lan", cn="空冥兰", tier=5, shape="orchid",
         body="#2f2a52", body_hi="#544a8a", body_sh="#1a1730",
         accent="#8a6fe8", gem="#e0d0ff", white="#ffffff"),
    dict(id="xueluo_zhi", cn="血珞芝", tier=6, shape="fungus",
         body="#6b1220", body_hi="#a8323f", body_sh="#3d0a12",
         accent="#c0392b", gem="#f0c04a", white="#ffd9d0"),
]

#: 丹炉用色：青铜 + 金纹 + 炉火
FURNACE_PAL = dict(body="#8a6a3a", body_hi="#c39a52", body_sh="#4e3a1c",
                   accent="#d8b45c", gem="#3a2a14", white="#ffe9c0",
                   fire="#ff8a22", fire_hi="#ffd24a")

#: 灵田土用色：沃土 + 灵光
SOIL_PAL = dict(body="#7a5a34", body_hi="#9c7645", body_sh="#523a20",
                accent="#6fd8a0", gem="#a8f0c8", white="#d8f0d0")

SEED_PAL = dict(body="#c8a45c", body_hi="#e8cf8f", body_sh="#8a6a34",
                accent="#6fae5c", gem="#f0e0a0", white="#fffbe8")


# ---------------------------------------------------------------------------
# 画布辅助
# ---------------------------------------------------------------------------

def canvas():
    return [["." for _ in range(SIZE)] for _ in range(SIZE)]


def put(grid, x, y, ch):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        grid[y][x] = ch


def stem(grid, x, height, ch="B", lean=0):
    """从底部 y=15 往上画一根茎，lean>0 时向右偏。"""
    for i in range(height):
        put(grid, x + (lean * i) // max(1, height), 15 - i, ch)


def leaf(grid, x, y, dx, ch="B"):
    """一片三像素的小叶子，dx=±1 决定朝向。"""
    put(grid, x, y, ch)
    put(grid, x + dx, y - 1, ch)
    put(grid, x + 2 * dx, y - 1, ch)
    put(grid, x + 3 * dx, y, ch)


def bloom(grid, cx, cy, ch="A", core="G", wide=2):
    """花冠：一个上宽下窄的小花团，中间一颗丹心。"""
    for dx in range(-wide, wide + 1):
        put(grid, cx + dx, cy, ch)
    for dx in range(-(wide - 1), wide):
        put(grid, cx + dx, cy + 1, ch)
    put(grid, cx, cy + 1, core)


def cap(grid, cx, cy, half, ch="A", fleck="G"):
    """菌盖：一段圆弧。"""
    for dx in range(-half, half + 1):
        drop = abs(dx) // 2
        put(grid, cx + dx, cy + drop, ch)
    for dx in (-half + 2, 0, half - 2):
        put(grid, cx + dx, cy + abs(dx) // 2, fleck)


def sprout(grid, cx=8):
    """幼苗（stage 0）：短茎 + 两对子叶。

    六种植物的"刚种下"看起来都该是差不多的幼苗；也保证这一格贴图
    有足够的可辨像素（太小的一撮绿点在实际游戏里等于看不见）。
    """
    stem(grid, cx, 3)
    leaf(grid, cx, 14, -1)
    leaf(grid, cx, 14, 1)
    leaf(grid, cx, 12, -1)
    leaf(grid, cx, 12, 1)
    put(grid, cx, 12, "H")


# --- 六种剪影（按 stage 0..3） --------------------------------------------

def shape_lotus(g, stage):
    if stage == 0:
        sprout(g)
        return
    """雪莲草：直立草叶 + 顶端白莲。"""
    height = (3, 5, 8, 10)[stage]
    stem(g, 7, height)
    if stage >= 1:
        leaf(g, 7, 12, -1)
        leaf(g, 7, 14, 1)
    if stage >= 2:
        leaf(g, 7, 9, -1)
        leaf(g, 7, 11, 1)
    if stage >= 3:
        leaf(g, 7, 7, -1)
        bloom(g, 7, 15 - height, "A", "G", 2)


def shape_flame(g, stage):
    if stage == 0:
        sprout(g)
        return
    """火焰花：暗茎 + 向上的火舌。"""
    height = (2, 5, 7, 9)[stage]
    stem(g, 8, height)
    if stage >= 1:
        leaf(g, 8, 13, -1)
        leaf(g, 8, 14, 1)
    top = 15 - height
    if stage == 2:
        bloom(g, 8, top, "A", "G", 1)
    if stage >= 3:
        # 三簇火舌：中间最高
        for dy, dxs in ((0, (0,)), (-1, (-1, 0, 1)), (-2, (-1, 1)), (-3, (0,))):
            for dx in dxs:
                put(g, 8 + dx, top + dy, "A")
        put(g, 8, top, "G")
        put(g, 7, top - 1, "G")
        put(g, 9, top - 1, "G")


def shape_vine(g, stage):
    if stage == 0:
        sprout(g)
        return
    """寒冰藤：左右缠绕的藤蔓 + 冰晶叶。"""
    height = (3, 6, 9, 12)[stage]
    sway = (0, 0, 1, 1, 0, 0, -1, -1)
    for i in range(height):
        put(g, 8 + sway[(i // 2) % 8], 15 - i, "B")
    if stage >= 1:
        leaf(g, 7, 12, -1, "A")
        leaf(g, 9, 14, 1)
    if stage >= 2:
        leaf(g, 6, 8, -1)
        leaf(g, 10, 10, 1, "A")
    if stage >= 3:
        for dx, dy in ((0, 0), (-1, -1), (1, -1), (0, -2)):
            put(g, 8 + dx, 15 - height + dy, "G")


def shape_spike(g, stage):
    if stage == 0:
        sprout(g)
        return
    """雷音草：锯齿状草茎，顶端带电火花。"""
    height = (3, 6, 9, 11)[stage]
    for i in range(height):
        put(g, 8 + (1 if i % 2 else -1 if i % 4 == 3 else 0), 15 - i, "B")
    if stage >= 1:
        leaf(g, 8, 13, -1)
    if stage >= 2:
        leaf(g, 8, 9, 1)
        put(g, 8, 15 - height, "A")
    if stage >= 3:
        top = 15 - height
        for dx, dy in ((-2, 0), (2, 0), (-1, -2), (1, -2), (0, -1)):
            put(g, 8 + dx, top + dy, "G")
        put(g, 8, top, "A")


def shape_orchid(g, stage):
    if stage == 0:
        sprout(g)
        return
    """空冥兰：细长叶 + 幽兰。"""
    height = (3, 5, 8, 10)[stage]
    stem(g, 8, height)
    if stage >= 1:
        for k in range(3):
            put(g, 8 - 1 - k // 2, 13 - k, "B")
            put(g, 8 + 1 + k // 2, 13 - k, "B")
    if stage >= 2:
        for k in range(3):
            put(g, 8 - 2 - k // 2, 10 - k, "B")
            put(g, 8 + 2 + k // 2, 10 - k, "B")
    if stage >= 3:
        top = 15 - height
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (-2, 0), (2, 0), (0, 1)):
            put(g, 8 + dx, top + dy, "A")
        put(g, 8, top, "G")
        put(g, 7, top - 1, "G")


def shape_fungus(g, stage):
    if stage == 0:
        sprout(g)
        return
    """血珞芝：灵芝。stage>=1 起盖。"""
    height = (2, 4, 6, 8)[stage]
    stem(g, 8, height, "S")
    if stage == 0:
        put(g, 7, 15, "B")
        put(g, 9, 15, "B")
    if stage >= 1:
        cap(g, 8, 15 - height, min(4, stage + 2), "A", "G")
    if stage >= 3:
        cap(g, 8, 15 - height - 1, 5, "A", "G")
        put(g, 6, 15 - height + 2, "B")
        put(g, 10, 15 - height + 2, "B")


SHAPES = {
    "lotus": shape_lotus,
    "flame": shape_flame,
    "vine": shape_vine,
    "spike": shape_spike,
    "orchid": shape_orchid,
    "fungus": shape_fungus,
}


# ---------------------------------------------------------------------------
# 渲染（与既有生成器同一套：三级明暗 + 剪影八邻域描边）
# ---------------------------------------------------------------------------

def rgb(c):
    if isinstance(c, str):
        s = c.lstrip("#")
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)
    return tuple(c)


def scale(c, f):
    r, g, b, a = rgb(c)
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))),
            max(0, min(255, int(b * f))), a)


def mix(a, b, t):
    ar, ag, ab, _ = rgb(a)
    br, bg, bb, _ = rgb(b)
    return (int(ar + (br - ar) * t), int(ag + (bg - ag) * t),
            int(ab + (bb - ab) * t), 255)


def hsh(x, y, salt):
    h = (x * 374761393 + y * 668265263 + salt * 1442695040888963407) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h ^ (h >> 16)) & 0xFF


def roles_of(pal):
    return {"B": pal["body"], "H": pal["body_hi"], "S": pal["body_sh"],
            "A": pal["accent"], "G": pal["gem"], "W": pal["white"]}


def render_cutout(grid, pal, salt=0, outline="#241f1a"):
    """透明底 + 剪影描边（作物/物品用）。"""
    for i, row in enumerate(grid):
        if len(row) != SIZE:
            raise SystemExit(f"第 {i} 行长度为 {len(row)}，应为 {SIZE}：{row!r}")
    if len(grid) != SIZE:
        raise SystemExit(f"只有 {len(grid)} 行，应为 {SIZE}")

    roles = roles_of(pal)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    for y in range(SIZE):
        for x in range(SIZE):
            ch = grid[y][x]
            if ch == ".":
                continue
            col = roles.get(ch)
            if col is None:
                continue
            c = rgb(col)
            t = (x + y) / (2.0 * (SIZE - 1))
            c = mix(scale(c, 1.12), scale(c, 0.82), t)
            c = scale(c, 1.0 + (hsh(x, y, salt) / 255.0 - 0.5) * 0.10)
            px[x, y] = c

    # 高光：主体最上面一行提亮
    for y in range(SIZE):
        for x in range(SIZE):
            if grid[y][x] in ("B", "H") and (y == 0 or grid[y - 1][x] == "."):
                px[x, y] = scale(px[x, y], 1.16)

    solid = {(x, y) for y in range(SIZE) for x in range(SIZE) if grid[y][x] != "."}
    oc = rgb(outline)
    for (x, y) in solid:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < SIZE and 0 <= ny < SIZE and (nx, ny) not in solid:
                px[nx, ny] = oc
    return img


def render_full(grid, pal, salt=0):
    """不透明整方块（灵田土 / 丹炉用）：铺满 16×16，不描边。"""
    for i, row in enumerate(grid):
        if len(row) != SIZE:
            raise SystemExit(f"第 {i} 行长度为 {len(row)}，应为 {SIZE}：{row!r}")
    roles = roles_of(pal)
    img = Image.new("RGBA", (SIZE, SIZE), rgb(pal["body"]))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            ch = grid[y][x]
            col = roles.get(ch, pal["body"])
            c = rgb(col)
            t = (x + y) / (2.0 * (SIZE - 1))
            c = mix(scale(c, 1.08), scale(c, 0.88), t)
            c = scale(c, 1.0 + (hsh(x, y, salt) / 255.0 - 0.5) * 0.08)
            px[x, y] = c
    return img


# ---------------------------------------------------------------------------
# 灵田土 / 丹炉：整方块网格
# ---------------------------------------------------------------------------

def grid_lingtian_soil():
    g = [["B" for _ in range(SIZE)] for _ in range(SIZE)]
    # 三道犁沟（比底色暗一档），沟里点灵光
    for row in (3, 4, 9, 10, 14):
        for x in range(SIZE):
            g[row][x] = "S"
    for x in range(SIZE):
        g[0][x] = "H"
        g[15][x] = "S"
    for (x, y) in ((2, 4), (7, 3), (12, 4), (4, 10), (10, 10), (14, 9), (6, 14)):
        g[y][x] = "A"
    for (x, y) in ((3, 5), (11, 3), (13, 10), (5, 13)):
        g[y][x] = "G"
    return g


def grid_furnace_side():
    g = [["B" for _ in range(SIZE)] for _ in range(SIZE)]
    for x in range(SIZE):
        g[0][x] = "H"
        g[1][x] = "H"
        g[15][x] = "S"
    # 上下两道金箍
    for row in (4, 11):
        for x in range(SIZE):
            g[row][x] = "A"
    # 回纹：金箍之间的方角纹
    for (x, y) in ((3, 6), (4, 6), (5, 6), (3, 7), (3, 8), (11, 6), (12, 6), (12, 7),
                   (12, 8), (7, 7), (8, 7), (7, 8), (8, 8)):
        g[y][x] = "A"
    for (x, y) in ((2, 6), (13, 6), (2, 9), (13, 9)):
        g[y][x] = "G"
    return g


def grid_furnace_top(lit=False):
    g = [["B" for _ in range(SIZE)] for _ in range(SIZE)]
    for x in range(SIZE):
        g[0][x] = "H"
        g[15][x] = "S"
        g[x][0] = "H"
        g[x][15] = "S"
    # 炉口：7×7 的方孔 + 金边
    for y in range(4, 12):
        for x in range(4, 12):
            on_edge = (x in (4, 11) or y in (4, 11))
            g[y][x] = "A" if on_edge else ("A" if lit else "G")
    if lit:
        for (x, y) in ((6, 6), (9, 6), (6, 9), (9, 9), (7, 7), (8, 8)):
            g[y][x] = "W"
    return g


def grid_furnace_front(lit=False):
    g = grid_furnace_side()
    # 正面：挖一个拱形炉门（照旧铺满，只是换了角色，不留透明像素）
    for y in range(6, 13):
        width = 4 if y < 9 else max(1, 4 - (y - 9))
        for x in range(8 - width, 8 + width):
            edge = x in (8 - width, 8 + width - 1) or y == 12
            if edge:
                g[y][x] = "A"
            else:
                g[y][x] = "A" if lit else "G"
    if lit:
        for (x, y) in ((6, 10), (9, 10), (7, 11), (8, 11), (8, 9)):
            g[y][x] = "W"
    else:
        g[11][7] = "S"
        g[11][8] = "S"
    return g


# ---------------------------------------------------------------------------
# 种子 / 花瓣
# ---------------------------------------------------------------------------

def grid_seed(herb):
    g = canvas()
    # 三粒种子：斜躺的椭圆
    spots = ((4, 11), (8, 9), (11, 12))
    for i, (cx, cy) in enumerate(spots):
        for dy in range(3):
            for dx in range(4):
                if dx + dy in (0, 4):
                    continue
                put(g, cx + dx, cy + dy, "B")
        put(g, cx + 1, cy + 1, "H")
        put(g, cx + 2, cy + 1, "G" if i == 1 else "A")
    return g


def grid_petal(herb):
    g = canvas()
    acc = "A"
    # 三片花瓣绕一个丹心
    petals = (
        [(6, 5), (5, 6), (7, 6), (6, 7), (6, 6)],
        [(10, 6), (9, 7), (10, 7), (11, 7), (10, 8), (10, 7)],
        [(8, 10), (7, 10), (9, 10), (7, 11), (8, 11), (9, 11), (8, 12)],
    )
    for p in petals:
        for (x, y) in p:
            put(g, x, y, acc)
    put(g, 8, 7, "G")
    put(g, 7, 8, "G")
    put(g, 9, 8, "G")
    put(g, 8, 9, "W")
    return g


# ---------------------------------------------------------------------------
# 落盘
# ---------------------------------------------------------------------------

def opaque_pixels(img):
    alpha = img.convert("RGBA").getchannel("A")
    return sum(alpha.histogram()[1:])


def write_png(img, folder, name):
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, name + ".png")
    img.save(path, "PNG")
    return path


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    return path


def herb_textures():
    """返回 [(文件名, 图像, 目标目录, 是否必须完全不透明)]。"""
    out = []
    for herb in HERBS:
        pal = {k: herb[k] for k in ("body", "body_hi", "body_sh", "accent", "gem", "white")}
        for stage in range(4):
            g = canvas()
            SHAPES[herb["shape"]](g, stage)
            out.append((f"{herb['id']}_stage{stage}", render_cutout(g, pal, stage * 7),
                        TEX_BLOCK, False))
        out.append((f"{herb['id']}_seed", render_cutout(grid_seed(herb), SEED_PAL, 3),
                    TEX_ITEM, False))
        out.append((f"{herb['id']}_petal", render_cutout(grid_petal(herb), pal, 5),
                    TEX_ITEM, False))
    out.append(("lingtian_soil", render_full(grid_lingtian_soil(), SOIL_PAL, 1), TEX_BLOCK, True))
    out.append(("alchemy_furnace_side", render_full(grid_furnace_side(), FURNACE_PAL, 2),
                TEX_BLOCK, True))
    out.append(("alchemy_furnace_top", render_full(grid_furnace_top(False), FURNACE_PAL, 3),
                TEX_BLOCK, True))
    out.append(("alchemy_furnace_front", render_full(grid_furnace_front(False), FURNACE_PAL, 4),
                TEX_BLOCK, True))
    out.append(("alchemy_furnace_front_lit", render_full(grid_furnace_front(True), FURNACE_PAL, 5),
                TEX_BLOCK, True))
    return out


def build_assets():
    """写全部贴图 + 模型 + blockstate。"""
    written = []
    for name, img, folder, _must_opaque in herb_textures():
        written.append(write_png(img, folder, name))

    # ---- 方块模型：灵植四阶 ----
    for herb in HERBS:
        for stage in range(4):
            write_json(os.path.join(MODEL_BLOCK, f"{herb['id']}_stage{stage}.json"), {
                "parent": "minecraft:block/crop",
                "textures": {"crop": f"{MOD_ID}:block/{herb['id']}_stage{stage}"},
            })
    # ---- 方块模型：灵田土 / 丹炉 ----
    write_json(os.path.join(MODEL_BLOCK, "lingtian_soil.json"), {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"{MOD_ID}:block/lingtian_soil"},
    })
    for lit in (False, True):
        suffix = "_lit" if lit else ""
        write_json(os.path.join(MODEL_BLOCK, f"alchemy_furnace{suffix}.json"), {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": f"{MOD_ID}:block/alchemy_furnace_top",
                "side": f"{MOD_ID}:block/alchemy_furnace_side",
                "front": f"{MOD_ID}:block/alchemy_furnace_front{suffix}",
            },
        })

    # ---- blockstates：灵植 8 个 age 映射到 4 张模型 ----
    for herb in HERBS:
        variants = {}
        for age in range(8):
            variants[f"age={age}"] = {"model": f"{MOD_ID}:block/{herb['id']}_stage{age // 2}"}
        write_json(os.path.join(BLOCKSTATES, f"{herb['id']}.json"), {"variants": variants})

    write_json(os.path.join(BLOCKSTATES, "lingtian_soil.json"), {
        "variants": {"": {"model": f"{MOD_ID}:block/lingtian_soil"}},
    })
    furnace_variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in ("false", "true"):
            suffix = "_lit" if lit == "true" else ""
            entry = {"model": f"{MOD_ID}:block/alchemy_furnace{suffix}"}
            if y:
                entry["y"] = y
            furnace_variants[f"facing={facing},lit={lit}"] = entry
    write_json(os.path.join(BLOCKSTATES, "alchemy_furnace.json"), {"variants": furnace_variants})

    # ---- 物品模型 ----
    for herb in HERBS:
        write_json(os.path.join(MODEL_ITEM, f"{herb['id']}.json"),
                   {"parent": f"{MOD_ID}:block/{herb['id']}_stage3"})
        for kind in ("seed", "petal"):
            write_json(os.path.join(MODEL_ITEM, f"{herb['id']}_{kind}.json"), {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"{MOD_ID}:item/{herb['id']}_{kind}"},
            })
    write_json(os.path.join(MODEL_ITEM, "lingtian_soil.json"),
               {"parent": f"{MOD_ID}:block/lingtian_soil"})
    write_json(os.path.join(MODEL_ITEM, "alchemy_furnace.json"),
               {"parent": f"{MOD_ID}:block/alchemy_furnace"})
    return written


def verify():
    """自查：16×16 RGBA、非空白；整方块贴图必须完全不透明。"""
    bad = 0
    tiles = herb_textures()
    for name, _img, folder, must_opaque in tiles:
        path = os.path.join(folder, name + ".png")
        if not os.path.exists(path):
            print(f"  ! 缺少 {name}.png")
            bad += 1
            continue
        im = Image.open(path)
        opaque = opaque_pixels(im)
        total = SIZE * SIZE
        ok = im.size == (SIZE, SIZE) and im.mode == "RGBA" and opaque >= 24
        if must_opaque and opaque != total:
            ok = False
        print(f"  [{'OK ' if ok else 'BAD'}] {name + '.png':34s} {im.size[0]}×{im.size[1]} "
              f"{im.mode} 不透明={opaque}/{total}")
        if not ok:
            bad += 1
    return bad


def preview(path):
    """把所有贴图拼成一张预览图（放大 6 倍），方便人眼扫一遍。"""
    tiles = herb_textures()
    scale = 6
    cols = 8
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * SIZE * scale, rows * SIZE * scale), (24, 20, 32, 255))
    for i, (_name, img, _folder, _opaque) in enumerate(tiles):
        big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        sheet.alpha_composite(big, ((i % cols) * SIZE * scale, (i // cols) * SIZE * scale))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.save(path, "PNG")
    return path


def main():
    ap = argparse.ArgumentParser(description="生成灵植 / 灵田土 / 丹炉的贴图与模型")
    ap.add_argument("--check", action="store_true", help="只校验现有文件")
    ap.add_argument("--preview", action="store_true", help="额外输出预览拼图")
    args = ap.parse_args()

    if args.check:
        print("== 现有文件校验 ==")
        return 1 if verify() else 0

    print("== 生成贴图与模型 ==")
    written = build_assets()
    print(f"  贴图 {len(written)} 张 + 模型/blockstate 若干")
    for p in written[:3]:
        print("   ->", os.path.relpath(p, MOD_ROOT))
    print("   ...")

    if args.preview:
        out = preview(os.path.join(HERE, "herb_alchemy_preview.png"))
        print("  预览图：", os.path.relpath(out, MOD_ROOT))

    print("\n== 结果校验 ==")
    bad = verify()
    print("\n完成。" if not bad else f"\n有 {bad} 个文件不合格。")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
