# -*- coding: utf-8 -*-
"""
深板岩矿变体 · 2D 资源生成器
=============================

玩家反馈：「那些掉落物矿啊锭的这种要有对应的矿可以采集」+「完善一下方块」。
审计发现两个真问题：

1. **空冥矿 / 血罗矿 只替换“石头”**，但它们的生成高度（-60~-8 / -60~16）几乎全在
   深板岩层 → 实际几乎不生成，玩家自然觉得“没有矿可采”；
2. 灵石矿（上一轮新增）**没进 `mineable/pickaxe` 标签** → 任何镐都“不是正确工具”，
   挖了什么都不掉。

本脚本补 3 个深板岩变体的 2D 资源（空冥 / 血罗 / 灵石），画风对齐
`deepslate_spirit_iron_ore` / `deepslate_spirit_crystal_ore`：深板岩噪点底 + 本矿脉同色矿点。

生成：
* `textures/block/deepslate_{kongming,xueluo,spirit_stone}_ore.png`
* `models/block/*.json`、`models/item/*.json`、`blockstates/*.json`
* 预览 `工程/art/w01r/deepslate_ores.png`

用法：`python 工程/art/tools/gen_deepslate_ore_assets.py`（幂等）
"""

from __future__ import annotations

import json
import os
import random

from PIL import Image, ImageDraw

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
TEX_BLOCK = os.path.join(RES, "textures", "block")
MODEL_BLOCK = os.path.join(RES, "models", "block")
MODEL_ITEM = os.path.join(RES, "models", "item")
BLOCKSTATES = os.path.join(RES, "blockstates")
PREVIEW_DIR = os.path.join(WS, "工程", "art", "w01r")

# 深板岩底色（与 deepslate_spirit_*_ore 同口径）
DEEPSLATE = (80, 80, 84)
DEEPSLATE_DARK = (62, 62, 66)
DEEPSLATE_LIGHT = (100, 100, 105)

#: 三个深板岩矿：id → (主色, 亮色, 暗色, 种子)
VARIANTS = {
    "deepslate_kongming_ore": ((150, 92, 224, 255), (214, 184, 255, 255),
                               (94, 48, 152, 255), 8401),
    "deepslate_xueluo_ore": ((208, 52, 56, 255), (255, 158, 150, 255),
                             (128, 24, 32, 255), 8402),
    "deepslate_spirit_stone_ore": ((47, 182, 176, 255), (214, 255, 250, 255),
                                   (27, 124, 121, 255), 8403),
}


def ensure_dirs():
    for d in (TEX_BLOCK, MODEL_BLOCK, MODEL_ITEM, BLOCKSTATES, PREVIEW_DIR):
        os.makedirs(d, exist_ok=True)


def make_deepslate_ore(seed, main, hi, lo):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r = rng.random()
            c = DEEPSLATE_DARK if r < 0.2 else (DEEPSLATE_LIGHT if r < 0.36 else DEEPSLATE)
            j = rng.randint(-7, 7)
            img.putpixel((x, y), (max(0, min(255, c[0] + j)),
                                  max(0, min(255, c[1] + j)),
                                  max(0, min(255, c[2] + j)), 255))

    # 矿点：每簇 = 亮心 + 十字 + 若干主色点（与既有矿石同款观感）
    centers = [(3, 4), (11, 3), (6, 10), (12, 11)]
    for i, (cx, cy) in enumerate(centers):
        if i == 3 and rng.random() < 0.35:
            continue  # 深板岩矿点略稀，看起来更"深"
        img.putpixel((cx, cy), hi)
        for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
            px, py = cx + dx, cy + dy
            if 0 <= px < 16 and 0 <= py < 16:
                img.putpixel((px, py), main)
        for dx, dy in ((2, 0), (1, 1), (0, 2), (-1, 1), (1, -1)):
            px, py = cx + dx, cy + dy
            if 0 <= px < 16 and 0 <= py < 16 and rng.random() < 0.8:
                img.putpixel((px, py), lo)
    return img


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def gen_models(ids):
    for name in ids:
        write_json(os.path.join(MODEL_BLOCK, name + ".json"), {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": "hongmeng:block/" + name},
        })
        write_json(os.path.join(BLOCKSTATES, name + ".json"), {
            "variants": {"": {"model": "hongmeng:block/" + name}},
        })
        write_json(os.path.join(MODEL_ITEM, name + ".json"),
                   {"parent": "hongmeng:block/" + name})


def preview(textures):
    scale, pad = 8, 8
    cell = 16 * scale
    width = (cell + pad) * len(textures) + pad
    canvas = Image.new("RGBA", (width, cell + pad * 2 + 22), (26, 28, 36, 255))
    d = ImageDraw.Draw(canvas)
    for i, (name, img) in enumerate(textures.items()):
        x = pad + i * (cell + pad)
        big = img.resize((cell, cell), Image.NEAREST)
        canvas.paste(big, (x, pad), big)
        d.text((x, pad + cell + 4), name, fill=(220, 225, 240, 255))
    out = os.path.join(PREVIEW_DIR, "deepslate_ores.png")
    canvas.save(out)
    return out


def main() -> int:
    ensure_dirs()
    textures = {}
    for name, (main, hi, lo, seed) in VARIANTS.items():
        img = make_deepslate_ore(seed, main, hi, lo)
        path = os.path.join(TEX_BLOCK, name + ".png")
        img.save(path)
        check = Image.open(path).convert("RGBA")
        assert check.size == (16, 16), path
        opaque = sum(1 for px in check.get_flattened_data() if px[3] > 0)
        assert opaque >= 200, (path, opaque)
        textures[name] = img
    gen_models(VARIANTS)
    out = preview(textures)
    print("生成贴图：")
    for name in VARIANTS:
        print("  ", os.path.relpath(os.path.join(TEX_BLOCK, name + ".png"), WS))
    print("生成模型/blockstate：%d 组" % len(VARIANTS))
    print("预览：", os.path.relpath(out, WS))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
