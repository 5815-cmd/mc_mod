# -*- coding: utf-8 -*-
"""血魔老祖（三头六臂）交付自检：模型 API / 尺寸 / UV / 贴图 / 动画 / 渲染器接线。

跑：python -X utf8 工程/角色设计/round6/demon/_selfcheck.py
"""
import io
import os
import re
import sys

WS = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                  "..", "..", "..", ".."))
TOOLS = os.path.join(WS, "工程", "角色设计", "tools")
sys.path.insert(0, TOOLS)
import charlib as L  # noqa: E402
import renderlib as R  # noqa: E402

CLIENT = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                      "xiuxian", "client")
TEXDIR = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets",
                      "hongmeng", "textures", "entity")
MODEL = os.path.join(CLIENT, "BloodDemonModel.java")
REND = os.path.join(CLIENT, "BloodDemonRenderer.java")
TEX = os.path.join(TEXDIR, "boss_demon_elder.png")
GLOW = os.path.join(TEXDIR, "boss_demon_elder_glow.png")
OUT = os.path.dirname(os.path.abspath(__file__))

results = []


def check(name, cond, detail=""):
    results.append((name, bool(cond), detail))
    print("[%s] %s%s" % ("PASS" if cond else "FAIL", name,
                         ("  — " + detail) if (detail and not cond) else ""))
    return cond


def read(p):
    return io.open(p, encoding="utf-8", errors="replace").read() if os.path.exists(p) else ""


src = read(MODEL)
rend = read(REND)
ok = True

# ---------- 1. 模型类 API（契约 §1：类名/extends/createBodyLayer/构造器/LOWEST） ----------
ok &= check("class BloodDemonModel extends EntityModel<CultivationMonsterEntity>",
            "public class BloodDemonModel extends EntityModel<CultivationMonsterEntity>" in src)
ok &= check("public static LayerDefinition createBodyLayer()",
            "public static LayerDefinition createBodyLayer()" in src)
ok &= check("构造器 BloodDemonModel(ModelPart root)",
            re.search(r"public BloodDemonModel\(ModelPart root\)", src) is not None)
ok &= check("LOWEST_PIXEL_Y = 24.0（double，契约 §1）",
            re.search(r"public static final double LOWEST_PIXEL_Y = 24\.0;", src) is not None)
ok &= check("LayerDefinition.create(mesh, 256, 256)（贴图 256²）",
            "LayerDefinition.create(mesh, 256, 256)" in src)

# ---------- 2. 反解模型 → 尺寸 / UV / 洞 / 挂点 ----------
model = L.parse_model_file(MODEL)
lo, hi = model.bounds()
height = hi[1] - lo[1]
width = hi[0] - lo[0]
depth = hi[2] - lo[2]
uv = R.uv_audit(model)
tex = R.load_texture(TEX)
glow = R.load_texture(GLOW)
texr = R.texture_audit(model, tex, glow)
g = R.ground_audit(model, 0.0)
gaps = R.gap_audit(model)

ok &= check("cube ≥ 85（契约 §2D）", model.total_cubes() >= 85,
            "cube=%d" % model.total_cubes())
ok &= check("站高 6.0 格 ±15%（96px）", abs(height - 96.0) / 96.0 <= 0.15,
            "height=%.1fpx (%.2f格)" % (height, height / 16.0))
ok &= check("肩宽 4 格 ±15%（64px）", abs(width - 64.0) / 64.0 <= 0.15,
            "width=%.1fpx (%.2f格)" % (width, width / 16.0))
ok &= check("最低点 y = 24.00px（脚底踩地，GROUND_LIFT=0）",
            abs(g["foot_all_px"] - 24.0) < 0.01 and g["on_ground"],
            "foot_all=%.2f on_ground=%s" % (g["foot_all_px"], g["on_ground"]))
ok &= check("UV 越界 0 / 跨足迹重叠 0", uv["oob_count"] == 0
            and uv["cross_overlap_texels"] == 0,
            "oob=%d overlap=%d" % (uv["oob_count"], uv["cross_overlap_texels"]))
ok &= check("真洞 0（贴图 100% 不透明）", len(texr.get("holes", [])) == 0,
            "holes=%d" % len(texr.get("holes", [])))
ok &= check("挂点断开 0", len(gaps) == 0, "gaps=%s" % (gaps[:3],))
ok &= check("贴图实际尺寸 = LayerDefinition 声明 256×256",
            tex is not None and tex.shape[1] == 256 and tex.shape[0] == 256
            and texr["size_ok"], "tex=%s" % (None if tex is None else tex.shape,))
ok &= check("_glow.png 与主贴图同尺寸（256×256）",
            glow is not None and glow.shape[:2] == tex.shape[:2],
            "glow=%s" % (None if glow is None else glow.shape,))
glow_px = int((glow[..., 3] > 8).sum())
ok &= check("发光层非空（血眼/血核/刃口/冠宝石）", glow_px > 0, "glow_px=%d" % glow_px)

# ---------- 3. 动画：绝对赋值 + 引用字段都存在 ----------
fields = set(re.findall(r"private final ModelPart (\w+);", src))
i = src.find("public void setupAnim")
j = src.find("\n    }", i)
anim = src[i:j]
refs = set(re.findall(r"this\.(\w+)", anim))
bad = sorted(refs - fields - {"root"})
ok &= check("setupAnim 引用的部件字段都存在", not bad, "未声明=%s" % bad)
ok &= check("setupAnim 无 += / *= 自增赋值",
            not re.search(r"this\.\w+\.\w+\s*[+\-*/]=", anim),
            "发现自增赋值")
need_anim = ["headMid", "headL", "headR", "armU1Left", "armU1Right", "armU2Left",
             "armU3Right", "skirt1", "skirt2", "skirt3", "skirt4", "core",
             "wingBone1Left", "mistA", "jawMid"]
missing_anim = [n for n in need_anim if ("this." + n) not in anim]
ok &= check("三头/六臂/袍摆/血核/血雾 动画齐全", not missing_anim,
            "缺动画引用=%s" % missing_anim)

# ---------- 4. 渲染器：LAYER/构造器不变 + RankGlowLayer ----------
ok &= check("renderer 保留 LAYER = \"blood_demon\"",
            re.search(r'"blood_demon"', rend) is not None)
ok &= check("renderer 构造器签名不变 (Context, String texName)",
            re.search(r"public BloodDemonRenderer\(EntityRendererProvider\.Context ctx,\s*String texName\)",
                      rend) is not None)
ok &= check("renderer 已加 RankGlowLayer（契约 §1）",
            "new RankGlowLayer<>(this)" in rend)
ok &= check("renderer 仍按 LOWEST_PIXEL_Y 算 GROUND_LIFT",
            "BloodDemonModel.LOWEST_PIXEL_Y - 24" in rend)

# ---------- 5. 旧模型对照（before 证据） ----------
old_ok = os.path.exists(os.path.join(OUT, "g1_before_三视图_旧模型.png")) and \
    os.path.exists(os.path.join(OUT, "g1_before_剪影64_旧模型.png")) and \
    os.path.exists(os.path.join(OUT, "boss_demon_elder_before_128.png"))
ok &= check("before 证据齐全（旧三视图/旧剪影/旧 128² 贴图）", old_ok)

print()
print("数字：cube=%d  parts=%d  高=%.1fpx(%.2f格)  宽=%.1fpx(%.2f格)  深=%.1fpx  "
      "脚底=%.2fpx  UV=%d/%d  洞=%d  glow=%dpx"
      % (model.total_cubes(), len(model.parts), height, height / 16.0,
         width, width / 16.0, depth, g["foot_all_px"], uv["oob_count"],
         uv["cross_overlap_texels"], len(texr.get("holes", [])), glow_px))
print()
print("SELF-CHECK:", "PASS" if ok else "FAIL")
sys.exit(0 if ok else 1)
