# -*- coding: utf-8 -*-
"""
灵石（spirit stone）内容 · 2D 资源生成器
=========================================

生成"灵石矿脉 + 灵石 + 两颗灵石系修为丹"的全部 2D 资源，可重复执行（幂等）：

* `textures/block/spirit_stone_ore.png`  16×16 矿石方块贴图（石底 + 青色灵晶矿点）
* `textures/item/spirit_stone.png`       16×16 灵石图标（三簇青玉晶体）
* `textures/item/pill_juling.png`        16×16 聚灵丹图标（青玉色丹丸 + 聚气旋纹）
* `textures/item/pill_ningyuan.png`      16×16 凝元丹图标（蓝紫色丹丸 + 晶体刻印）
* `models/block|item/spirit_stone_ore.json`、`blockstates/spirit_stone_ore.json`
* `models/item/{spirit_stone,pill_juling,pill_ningyuan}.json`（item/generated）
* **补 13 个刷怪蛋物品模型**：`models/item/<round2 刷怪蛋>.json` →
  `minecraft:item/template_spawn_egg`（这些蛋此前没有模型，游戏里是紫黑 missing model）
* 预览图：`工程/art/w01r/spirit_stone_content.png`（8× 放大）

画风对齐既有素材：
- 矿石 = `tools/gen_ore_textures.py` 同款"灰石噪点 + 成簇矿点"，矿点色换成青玉；
- 物品/丹药 = 既有 16×16 图标风格：深色描边 + 左上一档亮面 + 高光点 + 极少量噪点；
- 丹丸按 `pill_huiling/pill_exp_common` 的圆丹 + 环形描边样式。

用法：`python 工程/art/tools/gen_spirit_stone_assets.py`
"""

from __future__ import annotations

import json
import os
import random

from PIL import Image, ImageDraw

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
TEX_BLOCK = os.path.join(RES, "textures", "block")
TEX_ITEM = os.path.join(RES, "textures", "item")
MODEL_ITEM = os.path.join(RES, "models", "item")
MODEL_BLOCK = os.path.join(RES, "models", "block")
BLOCKSTATES = os.path.join(RES, "blockstates")
PREVIEW_DIR = os.path.join(WS, "工程", "art", "w01r")

# ---------------------------------------------------------------------------
# 调色板（青玉 / 灵晶）
# ---------------------------------------------------------------------------
JADE_DARK = (10, 48, 46, 255)      # 描边
JADE_BASE = (47, 182, 176, 255)    # 主色
JADE_LIGHT = (127, 232, 223, 255)  # 亮面
JADE_PALE = (214, 255, 250, 255)   # 高光
JADE_DEEP = (27, 124, 121, 255)    # 暗面

PILL_JULING_BASE = (74, 214, 195, 255)
PILL_JULING_LIGHT = (150, 255, 236, 255)
PILL_JULING_DARK = (22, 96, 86, 255)
PILL_JULING_MARK = (232, 255, 250, 255)

PILL_NING_BASE = (111, 125, 226, 255)
PILL_NING_LIGHT = (190, 202, 255, 255)
PILL_NING_DARK = (40, 38, 104, 255)
PILL_NING_MARK = (238, 240, 255, 255)

STONE = (128, 128, 128)
STONE_DARK = (105, 105, 105)
STONE_LIGHT = (150, 150, 150)

#: 第二轮新怪的 13 颗刷怪蛋（ModItemsExtra）——此前没有 item 模型
ROUND2_SPAWN_EGGS = [
    "marsh_wraith_spawn_egg",
    "crystal_spider_spawn_egg",
    "terracotta_soldier_spawn_egg",
    "scarecrow_puppet_spawn_egg",
    "lava_hound_spawn_egg",
    "void_jelly_spawn_egg",
    "end_apostle_spawn_egg",
    "fallen_immortal_spawn_egg",
    "blood_leech_spawn_egg",
    "bone_eagle_spawn_egg",
    "demon_spider_spawn_egg",
    "charred_skeleton_spawn_egg",
    "star_dust_wraith_spawn_egg",
]


def ensure_dirs():
    for d in (TEX_BLOCK, TEX_ITEM, MODEL_ITEM, MODEL_BLOCK, BLOCKSTATES, PREVIEW_DIR):
        os.makedirs(d, exist_ok=True)


# ---------------------------------------------------------------------------
# 画：灵石矿方块（16×16，灰石噪点 + 青玉矿点）
# ---------------------------------------------------------------------------
def make_ore_texture(seed=202611071):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r = rng.random()
            c = STONE_DARK if r < 0.18 else (STONE_LIGHT if r < 0.34 else STONE)
            j = rng.randint(-8, 8)
            img.putpixel((x, y), (max(0, min(255, c[0] + j)),
                                  max(0, min(255, c[1] + j)),
                                  max(0, min(255, c[2] + j)), 255))

    # 4 簇矿点，形状是"亮点 + L 形拖尾"（和既有矿石同款观感）
    centers = [(3, 4), (10, 3), (6, 10), (12, 11)]
    for cx, cy in centers:
        img.putpixel((cx, cy), JADE_PALE)
        for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
            if 0 <= cx + dx < 16 and 0 <= cy + dy < 16:
                img.putpixel((cx + dx, cy + dy), JADE_LIGHT)
        for dx, dy in ((2, 0), (1, 1), (0, 2), (-1, 1), (1, -1)):
            px, py = cx + dx, cy + dy
            if 0 <= px < 16 and 0 <= py < 16:
                img.putpixel((px, py), JADE_BASE)
        # 一点点暗边，避免矿点糊在石头上
        for dx, dy in ((-1, -1), (2, 2)):
            px, py = cx + dx, cy + dy
            if 0 <= px < 16 and 0 <= py < 16 and rng.random() < 0.5:
                img.putpixel((px, py), JADE_DEEP)
    return img


# ---------------------------------------------------------------------------
# 画：物品图标（16×16，深色描边 + 亮面 + 高光）
# ---------------------------------------------------------------------------
def make_spirit_stone_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def shard(points, base, light):
        d.polygon(points, fill=base, outline=JADE_DARK)
        # 左上亮面：取多边形靠左上的边带
        d.line(points[:2], fill=light, width=1)

    # 左小簇
    shard([(2, 8), (5, 6), (6, 11), (4, 13), (1, 12)], JADE_DEEP, JADE_BASE)
    # 右小簇
    shard([(11, 7), (14, 8), (14, 13), (11, 13), (10, 10)], JADE_DEEP, JADE_BASE)
    # 中央主晶体
    shard([(8, 2), (11, 6), (10, 12), (6, 13), (5, 7)], JADE_BASE, JADE_LIGHT)

    # 主晶体的左侧亮面与中央高光
    d.line([(8, 2), (6, 6), (6, 12)], fill=JADE_LIGHT, width=1)
    d.line([(8, 3), (7, 6)], fill=JADE_PALE, width=1)
    img.putpixel((8, 5), JADE_PALE)
    img.putpixel((7, 9), JADE_PALE)
    # 底部一点暗色，坐得更稳
    img.putpixel((7, 12), JADE_DARK)
    img.putpixel((9, 12), JADE_DEEP)
    return img


def _pill_base(draw, base, light, dark):
    """圆丹底：外圈深色描边 + 左上亮面 + 右下暗面。"""
    draw.ellipse([2, 2, 13, 13], fill=base, outline=dark)
    draw.ellipse([2, 2, 13, 13], outline=dark)
    # 左上亮弧
    draw.arc([3, 3, 10, 10], start=180, end=270, fill=light)
    # 右下暗弧
    draw.arc([5, 5, 12, 12], start=0, end=90, fill=dark)
    draw.ellipse([2, 2, 13, 13], outline=dark)


def make_pill_juling_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    _pill_base(d, PILL_JULING_BASE, PILL_JULING_LIGHT, PILL_JULING_DARK)
    # 聚气旋纹：三圈螺旋点
    for x, y in ((8, 7), (9, 8), (8, 9), (7, 8), (8, 8), (9, 6), (6, 9)):
        img.putpixel((x, y), PILL_JULING_MARK)
    img.putpixel((5, 5), PILL_JULING_MARK)   # 右上高光
    img.putpixel((4, 6), PILL_JULING_LIGHT)
    return img


def make_pill_ningyuan_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    _pill_base(d, PILL_NING_BASE, PILL_NING_LIGHT, PILL_NING_DARK)
    # 晶体刻印：菱形
    for x, y in ((8, 5), (8, 6), (7, 7), (8, 7), (9, 7), (8, 8),
                 (6, 8), (10, 8), (7, 9), (8, 9), (9, 9), (8, 10)):
        img.putpixel((x, y), PILL_NING_MARK)
    img.putpixel((5, 5), PILL_NING_MARK)
    img.putpixel((4, 6), PILL_NING_LIGHT)
    return img


# ---------------------------------------------------------------------------
# 写 JSON
# ---------------------------------------------------------------------------
def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def gen_models():
    write_json(os.path.join(MODEL_BLOCK, "spirit_stone_ore.json"), {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "hongmeng:block/spirit_stone_ore"},
    })
    write_json(os.path.join(BLOCKSTATES, "spirit_stone_ore.json"), {
        "variants": {"": {"model": "hongmeng:block/spirit_stone_ore"}},
    })
    write_json(os.path.join(MODEL_ITEM, "spirit_stone_ore.json"),
               {"parent": "hongmeng:block/spirit_stone_ore"})
    for name in ("spirit_stone", "pill_juling", "pill_ningyuan"):
        write_json(os.path.join(MODEL_ITEM, name + ".json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "hongmeng:item/" + name},
        })
    for egg in ROUND2_SPAWN_EGGS:
        write_json(os.path.join(MODEL_ITEM, egg + ".json"),
                   {"parent": "minecraft:item/template_spawn_egg"})


def preview():
    """拼一张 8× 放大预览：矿石、灵石、两颗丹、刷怪蛋模型清单。"""
    icons = [
        ("spirit_stone_ore", make_ore_texture()),
        ("spirit_stone", make_spirit_stone_icon()),
        ("pill_juling", make_pill_juling_icon()),
        ("pill_ningyuan", make_pill_ningyuan_icon()),
    ]
    scale, pad = 8, 8
    cell = 16 * scale
    width = (cell + pad) * len(icons) + pad
    canvas = Image.new("RGBA", (width, cell + pad * 2 + 22), (26, 28, 36, 255))
    d = ImageDraw.Draw(canvas)
    for i, (name, img) in enumerate(icons):
        x = pad + i * (cell + pad)
        canvas.paste(img.resize((cell, cell), Image.NEAREST), (x, pad), img.resize((cell, cell), Image.NEAREST))
        d.text((x, pad + cell + 4), name, fill=(220, 225, 240, 255))
    canvas.save(os.path.join(PREVIEW_DIR, "spirit_stone_content.png"))
    return os.path.join(PREVIEW_DIR, "spirit_stone_content.png")


# ---------------------------------------------------------------------------
def main() -> int:
    ensure_dirs()
    ore = make_ore_texture()
    stone = make_spirit_stone_icon()
    juling = make_pill_juling_icon()
    ningyuan = make_pill_ningyuan_icon()

    items = {
        os.path.join(TEX_BLOCK, "spirit_stone_ore.png"): ore,
        os.path.join(TEX_ITEM, "spirit_stone.png"): stone,
        os.path.join(TEX_ITEM, "pill_juling.png"): juling,
        os.path.join(TEX_ITEM, "pill_ningyuan.png"): ningyuan,
    }
    for path, img in items.items():
        img.save(path)
        # 自检：尺寸/有透明像素/主体不透明像素足够
        check = Image.open(path).convert("RGBA")
        assert check.size == (16, 16), path
        opaque = sum(1 for px in check.getdata() if px[3] > 0)
        assert opaque >= 40, (path, opaque)

    gen_models()
    out = preview()

    print("贴图：")
    for path in items:
        print("  ", os.path.relpath(path, WS))
    print("模型 / blockstate：spirit_stone_ore + spirit_stone + 2 丹药 + %d 个刷怪蛋"
          % len(ROUND2_SPAWN_EGGS))
    print("预览：", os.path.relpath(out, WS))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
