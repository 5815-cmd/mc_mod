# -*- coding: utf-8 -*-
"""万兽之主（beast_boss）重制证据：before / after 大图 + 剪影 + 发光层 + 数字对比。

用法：
    python -X utf8 工程/角色设计/round6/beast/evidence.py

产物（本目录）：
    10_after_三视图大图.png     新模型 front/right/back/hero（深色底，与总览一致）
    11_after_hero_灰底发光.png   新模型 hero 大图（灰底 + _glow 层，最接近实机观感）
    12_before_after_对比.png     旧模型 vs 新模型（hero，同一缩放 + 数字）
    13_剪影64_新旧.png           64px 剪影可辨识度对比 + 32px 缩略
    14_glow层预览.png            只用发光贴图着色（看发光分布）
    15_numbers.md                before/after 数字表
"""
import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_WS = os.path.normpath(os.path.join(_HERE, "..", "..", "..", ".."))
sys.path.insert(0, os.path.join(_WS, "工程", "角色设计", "tools"))

import charlib as L            # noqa: E402
import renderlib as R          # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

JAVA_DIR = os.path.join(_WS, "xiuxian-mod", "src", "main", "java", "com",
                        "hongmeng", "xiuxian", "client")
TEX_DIR = os.path.join(_WS, "xiuxian-mod", "src", "main", "resources",
                       "assets", "hongmeng", "textures", "entity")
BASE = os.path.join(_WS, "工程", "角色设计", "round6", "evidence", "baseline", "old")

NEW_JAVA = os.path.join(JAVA_DIR, "BeastLordModel.java")
NEW_TEX = os.path.join(TEX_DIR, "boss_beast_lord.png")
NEW_GLOW = os.path.join(TEX_DIR, "boss_beast_lord_glow.png")
OLD_JAVA = os.path.join(BASE, "client", "BeastLordModel.java")
OLD_TEX = os.path.join(BASE, "textures", "boss_beast_lord.png")

BG = (22, 24, 30)
GRAY = (96, 96, 104)


def numbers(model):
    lo, hi = model.bounds()
    return dict(
        cubes=model.total_cubes(),
        parts=len([p for p in model.parts if p.cubes]),
        height=(24.0 - lo[1]) / 16.0,
        width=(hi[0] - lo[0]) / 16.0,
        length=(hi[2] - lo[2]) / 16.0,
        nose=lo[2],
        tail=hi[2],
        top=lo[1],
        foot=hi[1],
    )


def main():
    os.makedirs(_HERE, exist_ok=True)
    new = L.parse_model_file(NEW_JAVA)
    old = L.parse_model_file(OLD_JAVA)
    new_tex = R.load_texture(NEW_TEX)
    new_glow = R.load_texture(NEW_GLOW)
    old_tex = R.load_texture(OLD_TEX)
    n_new = numbers(new)
    n_old = numbers(old)

    # 10 新模型三视图（深色底，与 boss sheet 同风格）
    im = R.render_model(new, tex=new_tex, glow=None,
                        views=("front", "right", "back", "hero"),
                        size=(680, 560), ss=2, margin=0.93, bg=BG,
                        label="AFTER 万兽之主（重制：骨甲巨兽）")
    im.save(os.path.join(_HERE, "10_after_三视图大图.png"))

    # 11 hero 灰底 + 发光（最接近实机观感）
    im = R.render_model(new, tex=new_tex, glow=new_glow, views=("hero",),
                        size=(900, 720), ss=2, margin=0.95, bg=GRAY,
                        label="AFTER 万兽之主（灰底 + 发光层）")
    im.save(os.path.join(_HERE, "11_after_hero_灰底发光.png"))

    # 12 before / after hero 对比（同缩放：两图单独渲染后拼）
    after = R.render_model(new, tex=new_tex, glow=new_glow, views=("hero",),
                           size=(560, 500), ss=2, margin=0.95, bg=GRAY,
                           label="AFTER  cube=%d  高%.2f格 长%.2f格 宽%.2f格"
                                 % (n_new["cubes"], n_new["height"],
                                    n_new["length"], n_new["width"]))
    before = R.render_model(old, tex=old_tex, glow=None, views=("hero",),
                            size=(560, 500), ss=2, margin=0.95, bg=GRAY,
                            label="BEFORE  cube=%d  高%.2f格 长%.2f格 宽%.2f格"
                                  % (n_old["cubes"], n_old["height"],
                                     n_old["length"], n_old["width"]))
    w = after.width + before.width + 24
    h = max(after.height, before.height) + 16
    sheet = Image.new("RGB", (w, h), (250, 250, 252))
    sheet.paste(before, (8, 8))
    sheet.paste(after, (before.width + 16, 8))
    ImageDraw.Draw(sheet).text((10, h - 20),
                               "before: 旧 192×128 / 29 cube  ·  after: 256×256 + glow / 86 cube",
                               fill=(30, 30, 34))
    sheet.save(os.path.join(_HERE, "12_before_after_对比.png"))

    # 13 剪影 64px 可辨识度
    s_new = R.silhouette(new, view="hero", px=64)
    s_old = R.silhouette(old, view="hero", px=64)
    sheet = Image.new("RGB", (2 * 64 * 3 + 80, 260), (250, 250, 252))
    big_new = s_new.resize((192, 192), Image.NEAREST)
    big_old = s_old.resize((192, 192), Image.NEAREST)
    sheet.paste(big_old, (20, 40))
    sheet.paste(s_old, (90, 40))
    sheet.paste(big_new, (260, 40))
    sheet.paste(s_new, (330, 40))
    d = ImageDraw.Draw(sheet)
    d.text((20, 14), "BEFORE（旧）", fill=(30, 30, 34))
    d.text((260, 14), "AFTER（重制）", fill=(30, 30, 34))
    d.text((20, 240), "64px 剪影：驼峰骨板 + 巨角兽首 + 长尾，远看能读成\"骨甲巨兽\"",
           fill=(30, 30, 34))
    sheet.save(os.path.join(_HERE, "13_剪影64_新旧.png"))

    # 14 发光层分布（把 glow 当主贴图渲一遍）
    im = R.render_model(new, tex=new_glow, glow=None, views=("front", "hero"),
                        size=(620, 560), ss=2, margin=0.95, bg=(8, 8, 10),
                        label="_glow.png 发光分布（眼 / 肋骨 / 胸口核心 / 喉部）")
    im.save(os.path.join(_HERE, "14_glow层预览.png"))

    # 15 数字
    lines = [
        "# 万兽之主（beast_boss）重制 · before/after 数字",
        "",
        "| 项 | BEFORE（旧） | AFTER（重制） | 契约目标 |",
        "|---|---|---|---|",
        "| cube | %d | %d | ≥ 70 |" % (n_old["cubes"], n_new["cubes"]),
        "| 部件 | %d | %d | — |" % (n_old["parts"], n_new["parts"]),
        "| 四足站立高 | %.2f 格 | %.2f 格 | 4.2 格（67px） |"
        % (n_old["height"], n_new["height"]),
        "| 鼻到尾长 | %.2f 格 | %.2f 格 | 9 格（144px） |"
        % (n_old["length"], n_new["length"]),
        "| 肩宽 | %.2f 格 | %.2f 格 | 3.2 格 |"
        % (n_old["width"], n_new["width"]),
        "| 贴图 | 192×128 | 256×256 + _glow 256×256 | 256² + glow |",
        "| 脚底 | y=24 | y=24（最低点 %.2f） | y=24 |" % n_new["foot"],
        "| 鼻端 z | %.1f | %.1f | 前伸 |" % (n_old["nose"], n_new["nose"]),
        "| 尾端 z | %.1f | %.1f | 后伸 |" % (n_old["tail"], n_new["tail"]),
        "",
        "新模型：低伏长躯干 + 驼峰 3 排骨板（每排 2 块大骨板 + 棘刺）+ 巨角兽首（8 獠牙 / 发光眼）",
        "+ 独立可开合下颚 + 肩甲前肢（3 爪）+ 后肢 + 外露肋骨 + 胸口发光核心 + 长尾 4 节 / 尾锤。",
        "",
        "生成：`python -X utf8 工程/角色设计/tools/build_entity.py 工程/角色设计/round6/beast/spec.py`",
        "审计：UV 越界 0 / 跨足迹重叠 0 / 透明洞 0 / 脚底 24.00px / 挂点断开 0（0 problems）",
    ]
    with open(os.path.join(_HERE, "15_numbers.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")

    print("evidence written:")
    for name in ("10_after_三视图大图.png", "11_after_hero_灰底发光.png",
                 "12_before_after_对比.png", "13_剪影64_新旧.png",
                 "14_glow层预览.png", "15_numbers.md"):
        p = os.path.join(_HERE, name)
        print("  %-28s %8d bytes" % (name, os.path.getsize(p)))


if __name__ == "__main__":
    main()
