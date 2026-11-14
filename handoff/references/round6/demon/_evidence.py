# -*- coding: utf-8 -*-
"""血魔老祖新旧对比证据：before/after 三视图 + 64px 剪影 + hero 大图 + 对比总览。

跑：python -X utf8 工程/角色设计/round6/demon/_evidence.py
"""
import os
import sys

WS = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                  "..", "..", "..", ".."))
sys.path.insert(0, os.path.join(WS, "工程", "角色设计", "tools"))
import charlib as L  # noqa: E402
import renderlib as R  # noqa: E402

OUT = os.path.dirname(os.path.abspath(__file__))
CLIENT = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                      "xiuxian", "client")
TEXDIR = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets",
                      "hongmeng", "textures", "entity")

new = L.parse_model_file(os.path.join(CLIENT, "BloodDemonModel.java"))
tex = R.load_texture(os.path.join(TEXDIR, "boss_demon_elder.png"))
glow = R.load_texture(os.path.join(TEXDIR, "boss_demon_elder_glow.png"))

# AFTER：hero 大图（带发光层）+ 64px 剪影
views = R.render_model(new, tex=tex, glow=glow, views=("front", "right", "back", "hero"),
                       label="AFTER 血魔老祖（113 cube / 6.07 格 / 三头六臂）",
                       size=(340, 520))
views.save(os.path.join(OUT, "g1_after_三视图.png"))
R.render_model(new, tex=tex, glow=glow, views=("hero",),
               label="AFTER 血魔老祖 · hero", size=(520, 760)).save(
    os.path.join(OUT, "g1_after_hero.png"))
R.silhouette(new, view="hero", px=64).save(os.path.join(OUT, "g1_after_剪影64.png"))

# 新旧对比总览（旧三视图/旧剪影/旧贴图 由 _selfcheck 前置保存）
files = [
    os.path.join(OUT, "g1_before_三视图_旧模型.png"),
    os.path.join(OUT, "g1_after_三视图.png"),
    os.path.join(OUT, "g1_before_剪影64_旧模型.png"),
    os.path.join(OUT, "g1_after_剪影64.png"),
    os.path.join(OUT, "g1_after_hero.png"),
    os.path.join(OUT, "boss_demon_elder_before_128.png"),
]
try:
    R.contact_sheet([R.Image.open(p) if os.path.exists(p) else None for p in files],
                    os.path.join(OUT, "00_血魔老祖_新旧对比.png"), cols=3,
                    title="血魔老祖 · 第六轮重制对比（旧 32 cube/3.3 格/128² → 新 113 cube/6.07 格/256²）")
    print("对比总览：00_血魔老祖_新旧对比.png")
except Exception as e:
    print("[warn] 对比总览失败：%s" % e)

# 64px 剪影与其它 Boss 的 IoU（契约 §3.3：尽量 <0.70）
others = ["PuppetMasterModel", "BeastLordModel", "VoidLordModel",
          "BlazeEmperorModel", "VoidDragonEmperorModel"]
for name in others:
    p = os.path.join(CLIENT, name + ".java")
    if not os.path.exists(p):
        continue
    try:
        other = L.parse_model_file(p)
        print("IoU vs %-24s = %.3f" % (name, R.silhouette_iou(new, other)))
    except Exception as e:
        print("IoU vs %-24s 计算失败：%s" % (name, e))
print("evidence OK")
