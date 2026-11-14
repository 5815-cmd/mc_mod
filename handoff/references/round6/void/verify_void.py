# -*- coding: utf-8 -*-
"""虚空道主（第六轮重制 E）交付自检：模型/动画/size/贴图/renderer 接线。

用法：
    python -X utf8 工程/角色设计/round6/void/verify_void.py
"""
import io
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(HERE))))
TOOLS = os.path.join(WS, "工程", "角色设计", "tools")

sys.path.insert(0, TOOLS)
import charlib as L  # noqa: E402

JAVA = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com",
                    "hongmeng", "xiuxian", "client", "VoidLordModel.java")
RENDERER = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com",
                        "hongmeng", "xiuxian", "client", "VoidLordRenderer.java")
SETUP = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com",
                     "hongmeng", "xiuxian", "ClientSetup.java")
TEX = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets",
                   "hongmeng", "textures", "entity", "boss_void_lord.png")
GLOW = TEX.replace(".png", "_glow.png")

CHECKS = []


def check(name, cond, detail=""):
    CHECKS.append((name, bool(cond), detail))


def main():
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, ValueError):
        pass
    src = io.open(JAVA, encoding="utf-8").read()
    rs = io.open(RENDERER, encoding="utf-8").read()
    st = io.open(SETUP, encoding="utf-8").read()
    m = L.parse_model_file(JAVA)
    lo, hi = m.bounds()
    h_blocks = (hi[1] - lo[1]) / 16.0
    span_blocks = (hi[0] - lo[0]) / 16.0

    check("类签名不变（EntityModel<CultivationMonsterEntity>）",
          "public class VoidLordModel extends EntityModel<CultivationMonsterEntity>" in src)
    check("createBodyLayer()/构造器签名不变",
          "public static LayerDefinition createBodyLayer()" in src
          and "public VoidLordModel(ModelPart root)" in src)
    check("cube ≥ 85（契约 §2E）", m.total_cubes() >= 85, "%d cube" % m.total_cubes())
    check("浮空高 6.5 格 ±0.1", abs(h_blocks - 6.5) <= 0.1, "%.2f 格" % h_blocks)
    check("翼展约 9 格（8.5~9.5）", 8.5 <= span_blocks <= 9.5, "%.2f 格" % span_blocks)
    check("最低点 = y24（LOWEST_PIXEL_Y 24.0）",
          re.search(r"LOWEST_PIXEL_Y\s*=\s*24\.0\s*;", src) is not None)
    check("无未声明部件字段的动画引用",
          not [r for r in set(re.findall(r"this\.(\w+)", src[src.find("setupAnim"):]))
               if r not in set(re.findall(r"private final ModelPart (\w+);", src))])
    check("有悬浮/翼片/瞳孔/眨眼动画",
          all(k in src for k in ("floatY", "wingL0A", "pupil.x", "blink")))
    check("贴图 256×256", os.path.getsize(TEX) > 0 and _size(TEX) == (256, 256))
    check("发光贴图 256×256 且存在", os.path.exists(GLOW) and _size(GLOW) == (256, 256))
    check("renderer 加了 RankGlowLayer",
          "this.addLayer(new RankGlowLayer<>(this));" in rs)
    check("renderer 保留 LOWEST_PIXEL_Y 离地修正",
          "VoidLordModel.LOWEST_PIXEL_Y" in rs)
    check("LAYER id 未变（void_lord）", '"void_lord"' in rs)
    check("ClientSetup 注册未变",
          "VoidLordRenderer.LAYER" in st and "VoidLordRenderer(ctx" in st)

    print("=" * 88)
    for name, ok, d in CHECKS:
        print("%s  %s%s" % ("PASS" if ok else "**FAIL**", name,
                            ("（%s）" % d) if d else ""))
    bad = [c for c in CHECKS if not c[1]]
    print("=" * 88)
    print("虚空道主第六轮自检：%d/%d PASS%s"
          % (len(CHECKS) - len(bad), len(CHECKS),
             "" if not bad else "；失败：%s" % [c[0] for c in bad]))
    return 0 if not bad else 1


def _size(path):
    from PIL import Image
    return Image.open(path).size


if __name__ == "__main__":
    sys.exit(main())
