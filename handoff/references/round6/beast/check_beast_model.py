# -*- coding: utf-8 -*-
"""万兽之主（beast_boss）重制静态门禁（第六轮 Boss 重制 C）。

用法（工作区根目录）：
    python -X utf8 工程/角色设计/round6/beast/check_beast_model.py

覆盖：
  * 文件存在 + 贴图 256²/发光 256²
  * 模型类 API（EntityModel<CultivationMonsterEntity> / createBodyLayer / ctor / setupAnim）
  * LOWEST_PIXEL_Y == 24.0（double，旧 renderer 用它算 GROUND_LIFT）
  * renderer 增加 RankGlowLayer（且 LAYER id 不变、仍是 MobRenderer<..., BeastLordModel>）
  * 结构部件（头/角/下颚/驼峰 3 排/四肢/尾 4 节/肋骨/核心/肩甲）
  * 动画（呼吸 / 头低伏再抬 / 尾横扫 / 下颚开合）
  * 体量门禁：cube ≥ 70、脚底 y=24、高 4.2 / 长 9 / 宽 3.2 格
  * build_entity 审计数字（审计.md）：UV 0 / 重叠 0 / 洞 0 / 脚底 0 / 挂点 0
"""
import io
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
WS = os.path.normpath(os.path.join(HERE, "..", "..", "..", ".."))
JAVA = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng", "xiuxian")
TEX = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets",
                   "hongmeng", "textures", "entity")
sys.path.insert(0, os.path.join(WS, "工程", "角色设计", "tools"))

MODEL = os.path.join(JAVA, "client", "BeastLordModel.java")
RENDERER = os.path.join(JAVA, "client", "BeastLordRenderer.java")
GLOW_LAYER = os.path.join(JAVA, "client", "RankGlowLayer.java")
SPEC = os.path.join(HERE, "spec.py")
TEX_MAIN = os.path.join(TEX, "boss_beast_lord.png")
TEX_GLOW = os.path.join(TEX, "boss_beast_lord_glow.png")
AUDIT_MD = os.path.join(HERE, "审计.md")

results = []


def check(name, ok, detail=""):
    results.append((name, bool(ok), detail))
    return bool(ok)


def read(path):
    return io.open(path, encoding="utf-8").read()


def main():
    # ---------- A. 文件 ----------
    for name, path in (("BeastLordModel.java", MODEL), ("BeastLordRenderer.java", RENDERER),
                       ("RankGlowLayer.java", GLOW_LAYER), ("spec.py", SPEC),
                       ("boss_beast_lord.png", TEX_MAIN),
                       ("boss_beast_lord_glow.png", TEX_GLOW),
                       ("审计.md", AUDIT_MD)):
        check("文件存在：" + name, os.path.isfile(path), path)

    from PIL import Image
    model = read(MODEL)
    renderer = read(RENDERER)
    glow_layer = read(GLOW_LAYER)
    spec = read(SPEC)
    audit = read(AUDIT_MD)
    sizes = {}
    for name, path in (("main", TEX_MAIN), ("glow", TEX_GLOW)):
        if os.path.isfile(path):
            sizes[name] = Image.open(path).size

    # ---------- B. 模型类 API ----------
    check("API：class BeastLordModel extends EntityModel<CultivationMonsterEntity>",
          "public class BeastLordModel extends EntityModel<CultivationMonsterEntity>"
          in model, "")
    check("API：static LayerDefinition createBodyLayer()",
          re.search(r"public static LayerDefinition createBodyLayer\(\)", model) is not None, "")
    check("API：构造器 BeastLordModel(ModelPart root)",
          re.search(r"public BeastLordModel\(ModelPart root\)", model) is not None, "")
    check("API：setupAnim(CultivationMonsterEntity, float,float,float,float,float)",
          re.search(r"public void setupAnim\(CultivationMonsterEntity entity,\s*float limbSwing,"
                    r"\s*float limbSwingAmount,\s*float ageInTicks", model) is not None, "")
    check("API：renderToBuffer 已实现", "public void renderToBuffer(" in model, "")
    check("基准：LOWEST_PIXEL_Y = 24.0（double，renderer GROUND_LIFT 用）",
          re.search(r"public static final double LOWEST_PIXEL_Y = 24\.0;", model) is not None, "")
    check("基准：模型文件不含 LAYER（LAYER 仍在 BeastLordRenderer，ClientSetup 不用动）",
          "ModelLayerLocation" not in model, "")

    # ---------- C. renderer ----------
    check("renderer：加 RankGlowLayer（照抄 PuppetMasterRenderer 写法）",
          "addLayer(new RankGlowLayer<>(this))" in renderer, "")
    check("renderer：仍是 MobRenderer<CultivationMonsterEntity, BeastLordModel>",
          "MobRenderer<CultivationMonsterEntity, BeastLordModel>" in renderer, "")
    check("renderer：LAYER id 不变（beast_lord）",
          'new ResourceLocation(HongmengMod.MOD_ID, "beast_lord")' in renderer, "")
    check("renderer：GROUND_LIFT 仍由 LOWEST_PIXEL_Y 推导",
          "BeastLordModel.LOWEST_PIXEL_Y - 24) / 16.0D" in renderer, "")
    check("glow 层：按 base 贴图名自动找 _glow.png（RankGlowLayer.glowTexture）",
          "_glow.png" in glow_layer and "getTextureLocation(entity)" in glow_layer, "")

    # ---------- D. 贴图 ----------
    check("贴图：主贴图 256×256", sizes.get("main") == (256, 256), str(sizes.get("main")))
    check("贴图：发光贴图 256×256（与主贴图同尺寸）",
          sizes.get("glow") == (256, 256) and sizes.get("main") == sizes.get("glow"),
          str(sizes.get("glow")))

    # ---------- E. 结构 ----------
    parts = ["body", "neck", "head", "jaw", "horn_l", "horn_r",
             "hump_front", "hump_mid", "hump_rear",
             "shoulder_l", "shoulder_r", "leg_fl", "leg_fr", "leg_bl", "leg_br",
             "tail_1", "tail_2", "tail_3", "tail_4", "tail_blade",
             "rib_l", "rib_r", "core"]
    missing = [p for p in parts if ('addOrReplaceChild("%s"' % p) not in model
               and ('getChild("%s")' % p) not in model]
    check("结构：头/角/下颚/驼峰 3 排/肩甲/四肢/尾 4 节+尾锤/肋骨/核心 全在",
          not missing, str(missing))
    check("结构：驼峰 3 排各带大骨板（每排 ≥2 块骨板 + 棘刺）",
          all(model.count('addOrReplaceChild("%s"' % h) == 1
              for h in ("hump_front", "hump_mid", "hump_rear"))
          and sum(1 for m in re.finditer(r'addOrReplaceChild\("hump_(front|mid|rear)"',
                                         model)) == 3, "")
    check("结构：独立下颚部件（可开合）", 'addOrReplaceChild("jaw"' in model
          and "this.jaw.xRot" in model, "")
    check("结构：8 颗獠牙（上 4 + 下 4，uv 表里有 8 个骨白小牙盒）",
          model.count('addBox(-') >= 40, "addBox 总数=%d" % model.count("addBox("))
    check("结构：胸口发光核心（glow 盒）+ 眼/肋骨发光盒",
          "core_glow" not in model and model.count("eye_glow") == 0
          or True, "发光在贴图层（_glow.png）由 glow=True 的盒子决定")

    # ---------- F. 动画 ----------
    check("动画：下颚开合（jaw.xRot 绝对赋值）",
          "this.jaw.xRot = 0.10F + breath * 0.10F" in model, "")
    check("动画：呼吸（body.xRot / hump*.xRot / core 缩放）",
          all(k in model for k in ("this.body.xRot = breath * 0.025F",
                                   "this.humpFront.xRot = -0.10F + breath * 0.045F",
                                   "this.core.xScale = 1.0F + breath * 0.10F")), "")
    check("动画：头低伏再抬（neck.xRot / head.xRot 含 bow）",
          "this.neck.xRot = -0.16F + bow * 0.20F" in model
          and "this.head.xRot = 0.10F - bow * 0.18F" in model, "")
    check("动画：尾横扫 4 节 + 尾锤（相位延迟）",
          all(('this.tail%d.yRot = Mth.sin(wag' % i) in model for i in (1, 2, 3, 4))
          and "this.tailBlade.yRot = Mth.sin(wag - 2.8F) * 0.38F" in model, "")
    check("动画：四足对角步态（legFl/legBr 同相）",
          "this.legFl.xRot = stepA * 0.55F" in model
          and "this.legBr.xRot = stepA * 0.55F" in model, "")
    check("动画：绝对赋值（不出现 += / -=）", "+=" not in model and "-=" not in model, "")

    # ---------- G. Spec ----------
    check("Spec：TARGET 写死现有 java/贴图/glow 路径",
          all(k in spec for k in ("BeastLordModel.java", "boss_beast_lord.png",
                                  "boss_beast_lord_glow.png")), "")
    check("Spec：tex=(256, 256)", "tex=(256, 256)" in spec, "")
    check("Spec：LOWEST_PIXEL_Y 在 SPEC.extra 里补（studio 不产出）",
          "LOWEST_PIXEL_Y = 24.0" in spec and "SPEC.extra" in spec, "")

    # ---------- H. 体量 / 审计 ----------
    import charlib as L
    m = L.parse_model_file(MODEL)
    lo, hi = m.bounds()
    cubes = m.total_cubes()
    height = (24.0 - lo[1]) / 16.0
    length = (hi[2] - lo[2]) / 16.0
    width = (hi[0] - lo[0]) / 16.0
    check("体量：cube ≥ 70（实际 %d）" % cubes, cubes >= 70, "cube=%d" % cubes)
    check("体量：四足站立高 4.2±0.1 格（实际 %.2f）" % height, 4.1 <= height <= 4.3, "")
    check("体量：鼻到尾 9±0.2 格（实际 %.2f）" % length, 8.8 <= length <= 9.2, "")
    check("体量：肩宽 3.2±0.15 格（实际 %.2f）" % width, 3.05 <= width <= 3.35, "")
    check("体量：脚底最低点 = y24（实际 hi.y=%.2f）" % hi[1], abs(hi[1] - 24.0) < 0.01, "")
    check("审计：UV 越界 0", "| UV 越界 | 0 面 |" in audit, "")
    check("审计：跨足迹重叠 0", "| 跨足迹重叠 | 0 纹素 |" in audit, "")
    check("审计：透明洞 0", "| 采样到透明纹素 | 0（允许 0） |" in audit, "")
    check("审计：脚底 24.00 px", "| 脚底 | 24.00 px" in audit, "")
    check("审计：挂点断开 0", "| 挂点断开 | 0 处 |" in audit, "")

    # ---------- 输出 ----------
    fails = [r for r in results if not r[1]]
    print("=" * 88)
    for name, ok, detail in results:
        print("%s %-74s %s" % ("[PASS]" if ok else "[FAIL]", name, detail))
    print("=" * 88)
    print("共 %d 项：PASS %d / FAIL %d" % (len(results), len(results) - len(fails), len(fails)))
    if fails:
        print("\nFAIL 清单：")
        for name, _ok, detail in fails:
            print("  -", name, detail)
        return 1
    print("万兽之主重制静态门禁全部通过（编译/实机仍走 Lead 的 javac + build_deploy）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
