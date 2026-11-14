#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
鸿蒙修仙 —— 灵兽系统物品贴图生成器
===================================

生成 3 张 16×16 RGBA 物品图标 + 对应的 ``item/generated`` 模型：

    assets/hongmeng/textures/item/contract_talisman.png   契灵符
    assets/hongmeng/textures/item/beast_pouch.png         灵兽袋
    assets/hongmeng/textures/item/beast_armor.png         灵兽护甲

    assets/hongmeng/models/item/<name>.json

画法与既有的 ``tools/gen_item_textures.py`` 完全一致（这是"复用既有口径"，
不是另起一套）：

  1. 用 16×16 的 ASCII 网格定义**剪影**，只关心"哪一格是什么角色"；
  2. 角色映射到调色板（主色 / 主色亮 / 主色暗 / 点缀 / 宝石 / 留白 / 描边）；
  3. 按"左上受光、右下压暗"补三级明暗，无渐变、无抗锯齿；
  4. 描边由 **silhouette 的八邻域**自动补——用真正的剪影求边，
     而不是"看哪个像素旁边是透明的"（后者在贴图被 UV 拆分后会乱套，
     ``gen_beast_mount_textures.py`` 里踩过这个坑）。

用法::

    python tools/gen_beast_item_textures.py            # 写入模组资源目录
    python tools/gen_beast_item_textures.py --check    # 只校验现有文件
    python tools/gen_beast_item_textures.py --out DIR
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
TEX_DIR = os.path.join(RES, "textures", "item")
MODEL_DIR = os.path.join(RES, "models", "item")

SIZE = 16
MOD_ID = "hongmeng"


# ---------------------------------------------------------------------------
# 剪影：每个字符是一个角色
#   .  透明        B 主色        H 主色亮      S 主色暗
#   A  点缀        G 宝石        W 高光        o 描边（自动补，网格里不用写）
# ---------------------------------------------------------------------------

TALISMAN = (
    "................",
    "................",
    ".....AAAAAA.....",
    ".....ABBBA......",
    ".....ABGBA......",
    ".....ABGBA......",
    ".....ABGBA......",
    ".....ABGBA......",
    ".....ABGBA......",
    ".....ABGBA......",
    ".....ABBBA......",
    ".....AAAAAA.....",
    "................",
    "................",
    "................",
    "................",
)

POUCH = (
    "................",
    "................",
    "......AAAA......",
    ".....AABBAA.....",
    "....ABBBBBBA....",
    "...ABBBBBBBBA...",
    "..ABBBBBBBBBBA..",
    "..ABBBGGBBBBBA..",
    "..ABBBGGBBBBBA..",
    "..ABBBBBBBBBBA..",
    "..ABBBBBBBBBBA..",
    "...ABBBBBBBBA...",
    "....ABBBBBBA....",
    ".....AAAAAA.....",
    "................",
    "................",
)

ARMOR = (
    "................",
    "................",
    "..BB......BB....",
    ".BGGB....BGGB...",
    ".BGGB....BGGB...",
    ".BBBBBBBBBBBB...",
    "..BBBBBBBBBB....",
    "..BBBGGBBBBB....",
    "..BBBGGBBBBB....",
    "..BBBBBBBBBB....",
    "..BBBBBBBBBB....",
    "..ABBBBBBBBA....",
    "..AABBBBBBAA....",
    "...AAAAAAAA.....",
    "................",
    "................",
)

#: 三个物品的调色板（主色, 主色亮, 主色暗, 点缀, 宝石, 高光）
PALETTES = {
    # 契灵符：明黄符纸 + 朱砂符纹 + 靛蓝镶边
    "contract_talisman": dict(
        body="#e8d9a0", body_hi="#f7eecb", body_sh="#a8955e",
        accent="#3f4f8f", gem="#c0392b", white="#fffbe8",
    ),
    # 灵兽袋：青碧锦囊 + 金色束口 + 灵石扣
    "beast_pouch": dict(
        body="#2f7f74", body_hi="#4fb8a2", body_sh="#18463f",
        accent="#d8b45c", gem="#7ff0e0", white="#e8fff8",
    ),
    # 灵兽护甲：灵铁甲片 + 青玉镶钉
    "armor": dict(
        body="#8f98a8", body_hi="#c8d2e0", body_sh="#4e5665",
        accent="#d8b45c", gem="#4fd8e8", white="#eaf6ff",
    ),
}
PALETTES["beast_armor"] = PALETTES["armor"]
PALETTES["contract_talisman"]["gem"] = "#c0392b"
PALETTES["beast_pouch"]["gem"] = "#7ff0e0"
PALETTES["beast_armor"]["gem"] = "#4fd8e8"

GRIDS = {
    "contract_talisman": TALISMAN,
    "beast_pouch": POUCH,
    "beast_armor": ARMOR,
}


# ---------------------------------------------------------------------------
# 颜色
# ---------------------------------------------------------------------------

def rgb(c):
    if isinstance(c, str):
        s = c.lstrip("#")
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)
    return tuple(c)


def scale(c, f: float):
    r, g, b, a = rgb(c)
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))),
            max(0, min(255, int(b * f))), a)


def mix(a, b, t: float):
    ar, ag, ab, _ = rgb(a)
    br, bg, bb, _ = rgb(b)
    return (int(ar + (br - ar) * t), int(ag + (bg - ag) * t),
            int(ab + (bb - ab) * t), 255)


def hsh(x: int, y: int, salt: int) -> int:
    h = (x * 374761393 + y * 668265263 + salt * 1442695040888963407) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h ^ (h >> 16)) & 0xFF


def noise(c, x: int, y: int, salt: int, amount: float = 0.05):
    return scale(c, 1.0 + (hsh(x, y, salt) / 255.0 - 0.5) * 2.0 * amount)


# ---------------------------------------------------------------------------
# 画
# ---------------------------------------------------------------------------

def build(name: str) -> Image.Image:
    grid = GRIDS[name]
    # 网格写错一个字符（多一个空格、少一个点）在贴图上只表现为"缺一角"，
    # 肉眼极难发现——所以这里直接硬校验。
    for i, row in enumerate(grid):
        if len(row) != SIZE:
            raise SystemExit(f"{name} 第 {i} 行长度为 {len(row)}，应为 {SIZE}：{row!r}")
    if len(grid) != SIZE:
        raise SystemExit(f"{name} 只有 {len(grid)} 行，应为 {SIZE}")

    pal = PALETTES[name]
    salt = sum(ord(ch) for ch in name)

    roles = {
        "B": pal["body"], "H": pal["body_hi"], "S": pal["body_sh"],
        "A": pal["accent"], "G": pal["gem"], "W": pal["white"],
    }

    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    # 1) 剪影 + 三级明暗
    for y, row in enumerate(grid[:SIZE]):
        for x, ch in enumerate(row[:SIZE]):
            if ch == ".":
                continue
            col = roles.get(ch)
            if col is None:
                continue
            col = rgb(col)
            # 左上受光：越靠左上越亮
            t = (x + y) / (2.0 * (SIZE - 1))
            col = mix(scale(col, 1.14), scale(col, 0.80), t)
            col = noise(col, x, y, salt, 0.05)
            px[x, y] = col

    # 2) 高光：主体最上面两行再提一档（金属/纸面的反光）
    for y, row in enumerate(grid[:SIZE]):
        for x, ch in enumerate(row[:SIZE]):
            if ch in ("B", "H") and (y == 0 or grid[y - 1][x] == "."):
                px[x, y] = scale(px[x, y], 1.18)

    # 3) 描边：用**剪影的八邻域**求边，不是"旁边透明就描边"
    #    （后者在贴图被 UV 拆分后会乱套）
    solid = {(x, y) for y, row in enumerate(grid[:SIZE])
             for x, ch in enumerate(row[:SIZE]) if ch != "."}
    outline = rgb("#241f1a")
    for (x, y) in solid:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < SIZE and 0 <= ny < SIZE and (nx, ny) not in solid:
                px[nx, ny] = outline

    return img


def write_model(name: str, model_dir: str) -> None:
    path = os.path.join(model_dir, name + ".json")
    os.makedirs(model_dir, exist_ok=True)
    data = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{MOD_ID}:item/{name}"},
    }
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def verify(tex_dir: str) -> int:
    bad = 0
    for name in GRIDS:
        path = os.path.join(tex_dir, name + ".png")
        if not os.path.exists(path):
            print(f"  ! 缺少 {name}.png")
            bad += 1
            continue
        im = Image.open(path)
        opaque = sum(1 for p in im.convert("RGBA").get_flattened_data() if p[3] > 0)
        ok = im.size == (SIZE, SIZE) and im.mode == "RGBA" and opaque >= 24
        print(f"  [{'OK ' if ok else 'BAD'}] {name + '.png':24s} "
              f"{im.size[0]}×{im.size[1]} {im.mode} 不透明像素={opaque}")
        if not ok:
            bad += 1
    return bad


def main() -> int:
    ap = argparse.ArgumentParser(description="生成灵兽系统物品贴图与模型")
    ap.add_argument("--out", default=TEX_DIR)
    ap.add_argument("--model-dir", default=MODEL_DIR)
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()

    if args.check:
        print("== 现有文件校验 ==")
        return 1 if verify(args.out) else 0

    print("== 生成物品贴图 ==")
    for name in GRIDS:
        img = build(name)
        os.makedirs(args.out, exist_ok=True)
        path = os.path.join(args.out, name + ".png")
        img.save(path, "PNG")
        write_model(name, args.model_dir)
        print(f"  -> {os.path.relpath(path, MOD_ROOT)}  +  models/item/{name}.json")

    print("\n== 结果校验 ==")
    bad = verify(args.out)
    print("\n完成。" if not bad else f"\n有 {bad} 个文件不合格。")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
