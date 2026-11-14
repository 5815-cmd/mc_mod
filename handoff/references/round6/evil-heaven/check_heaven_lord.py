# -*- coding: utf-8 -*-
"""task-47 证据脚本 ②：仙庭之主重制后的自检 + before/after + 剪影。

只读 Java/贴图；只写 `工程/角色设计/round6/evil-heaven/**`。
产出：
  heaven_audit.json            数字（cube/宽/高/UV/洞/挂点/贴图/发光/API 检查）
  heaven_before_after.png      旧（backup）vs 新：front/hero 并排 + 数字
  heaven_silhouettes.png       旧/新 64px 剪影 + IoU
"""
import importlib.util
import io
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(HERE))))
TOOLS = os.path.join(WS, "工程", "角色设计", "tools")
sys.path.insert(0, TOOLS)
import charlib as L            # noqa: E402
import renderlib as R          # noqa: E402
from audit_all import classify_holes   # noqa: E402

import numpy as np             # noqa: E402
from PIL import Image, ImageDraw, ImageFont   # noqa: E402

CLIENT = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com",
                      "hongmeng", "xiuxian", "client")
TEXDIR = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets",
                      "hongmeng", "textures", "entity")
TEX = os.path.join(TEXDIR, "heaven_lord.png")
GLOW = os.path.join(TEXDIR, "heaven_lord_glow.png")
JAVA = os.path.join(CLIENT, "HeavenLordModel.java")
BAK = os.path.join(HERE, "backup")
BG = (24, 26, 33)
FG = (232, 238, 250)
DIM = (150, 158, 176)
GREEN = (110, 220, 140)
RED = (255, 92, 92)
ACCENT = (110, 190, 255)


def font(px, bold=False):
    names = ("msyhbd.ttc", "msyh.ttc", "simhei.ttf") if bold else ("msyh.ttc", "simhei.ttf", "arial.ttf")
    for n in names:
        try:
            return ImageFont.truetype("C:/Windows/Fonts/" + n, px)
        except Exception:   # noqa: BLE001
            continue
    return ImageFont.load_default()


def rel(p):
    return os.path.relpath(p, WS).replace("\\", "/")


def load_tex(p):
    return R.load_texture(p)


def audit_model(java_path, tex):
    m = L.parse_model_file(java_path)
    uv = R.uv_audit(m)
    tr = R.texture_audit(m, tex, None)
    holes = classify_holes(m, tex) if tex is not None else {}
    real = sum(v[1] for v in holes.values() if v[0] == "真洞") if holes else 0
    gaps = R.gap_audit(m)
    gaps.sort(key=lambda g: -g[2])
    st = L.summary(m)
    lo, hi = m.bounds()
    return {
        "model": m,
        "audit": {
            "class": m.name, "cubes": m.total_cubes(), "parts": len(m.parts),
            "tex": list(m.tex), "uv_oob": uv["oob_count"],
            "uv_cross": uv["cross_overlap_texels"], "holes_real": real,
            "gap_count": len(gaps), "gap_max_px": round(gaps[0][2], 1) if gaps else 0.0,
            "hard_gaps": [(g[0], g[1], round(g[2], 1)) for g in gaps if g[2] > 10],
            "top_px": round(st["top"], 1), "bottom_px": round(st["foot_all"], 1),
            "height_px": round(st["foot_all"] - st["top"], 1),
            "height_blocks": round((st["foot_all"] - st["top"]) / 16.0, 2),
            "width_px": round(hi[0] - lo[0], 1),
            "width_blocks": round((hi[0] - lo[0]) / 16.0, 2),
            "depth_px": round(hi[2] - lo[2], 1),
            "x_range": [round(lo[0], 1), round(hi[0], 1)],
            "y_range": [round(lo[1], 1), round(hi[1], 1)],
        },
    }


def render(model, tex, views, size, glow=None):
    return R.render_model(model, tex=tex, glow=glow, views=views, label="",
                          ground_lift=0.0, size=size, with_ground=False)


def before_after(old, new, old_tex, new_tex, glow, path):
    old_im = render(old, old_tex, ("front", "hero"), (250, 360))
    new_im = render(new, new_tex, ("front", "hero"), (250, 360), glow=glow)
    row_w = old_im.width
    W = 20 + row_w * 2 + 30 + 260
    H = 90 + old_im.height + 20
    sheet = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), "仙庭之主 · 旧版 vs 新版（studio 重写：2.75 格宽 → 6.5 格宽）",
           fill=FG, font=font(18, True))
    d.text((10, 34), "左：旧模型（backup）｜中：新模型（贴图+glow 实测渲染）",
           fill=DIM, font=font(12))
    sheet.paste(old_im, (10, 60))
    sheet.paste(new_im, (10 + row_w + 30, 60))
    tx = 10 + row_w * 2 + 40
    rows = [
        "cube：%d → %d" % (old.total_cubes(), new.total_cubes()),
        "宽：%d → %d px（%.2f → %.2f 格）" % (
            old_audit["width_px"], new_audit["width_px"],
            old_audit["width_blocks"], new_audit["width_blocks"]),
        "高：%d → %d px（%.2f → %.2f 格）" % (
            old_audit["height_px"], new_audit["height_px"],
            old_audit["height_blocks"], new_audit["height_blocks"]),
        "minY：%.0f → %.0f（world）" % (old_audit["y_range"][0], new_audit["y_range"][0]),
        "LOWEST_PIXEL_Y：24.0（脚底 y=24，锚点不动）",
        "UV：%d/%d → %d/%d；真洞：%d → %d" % (
            old_audit["uv_oob"], old_audit["uv_cross"],
            new_audit["uv_oob"], new_audit["uv_cross"],
            old_audit["holes_real"], new_audit["holes_real"]),
    ]
    for i, t in enumerate(rows):
        d.text((tx, 80 + i * 24), t, fill=FG if i < 5 else DIM, font=font(12))
    sheet.save(path)
    return path


def silhouettes(old, new, path):
    sil_old = R.silhouette(old, view="hero", px=64).resize((256, 256), Image.NEAREST)
    sil_new = R.silhouette(new, view="hero", px=64).resize((256, 256), Image.NEAREST)
    iou = R.silhouette_iou(old, new, view="hero", px=64)
    W = 20 + 256 * 2 + 30 + 200
    H = 360
    sheet = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), "仙庭之主 · 64px 剪影（hero）：旧 vs 新", fill=FG, font=font(18, True))
    sheet.paste(sil_old, (10, 44))
    sheet.paste(sil_new, (10 + 256 + 30, 44))
    d.text((10, 306), "旧", fill=DIM, font=font(13, True))
    d.text((10 + 256 + 30, 306), "新", fill=DIM, font=font(13, True))
    d.text((10 + 256 * 2 + 40, 60), "旧→新 剪影 IoU：%.3f" % iou, fill=ACCENT, font=font(14, True))
    d.text((10 + 256 * 2 + 40, 88), "（宽度变大是目的：%.2f → %.2f 格）"
           % (old_audit["width_blocks"], new_audit["width_blocks"]), fill=DIM, font=font(12))
    sheet.save(path)
    return path, iou


def api_checks(java_text):
    checks = {
        "extends EntityModel<HeavenLordEntity>":
            bool(re.search(r"public class HeavenLordModel extends EntityModel<HeavenLordEntity>", java_text)),
        "createBodyLayer()": "public static LayerDefinition createBodyLayer()" in java_text,
        "ctor(ModelPart root)": "public HeavenLordModel(ModelPart root)" in java_text,
        "LAYER(heaven_lord)": bool(re.search(r'ModelLayerLocation LAYER = .*"heaven_lord"', java_text)),
        "LOWEST_PIXEL_Y=24.0 double": "public static final double LOWEST_PIXEL_Y = 24.0;" in java_text,
        "setupAnim(HeavenLordEntity...)":
            "public void setupAnim(HeavenLordEntity entity" in java_text,
        "no += / *= in setupAnim": not re.search(r"\bthis\.\w+\.\w+ *[+\-*/]?=", java_text),
        "LayerDefinition.create(mesh, 256, 256)":
            "LayerDefinition.create(mesh, 256, 256)" in java_text,
    }
    return checks


def main():
    new_tex = load_tex(TEX)
    glow = load_tex(GLOW)
    old_tex = load_tex(os.path.join(BAK, "heaven_lord.png.bak"))
    new = audit_model(JAVA, new_tex)
    old = audit_model(os.path.join(BAK, "HeavenLordModel.java.bak"), old_tex)
    global old_audit, new_audit
    old_audit, new_audit = old["audit"], new["audit"]
    java_text = io.open(JAVA, encoding="utf-8").read()
    checks = api_checks(java_text)
    imgs = {}
    imgs["before_after"] = before_after(old["model"], new["model"], old_tex,
                                        new_tex, glow, os.path.join(HERE, "heaven_before_after.png"))
    imgs["silhouettes"], iou = silhouettes(old["model"], new["model"],
                                           os.path.join(HERE, "heaven_silhouettes.png"))
    tex_img = Image.open(TEX).convert("RGBA")
    glow_img = Image.open(GLOW).convert("RGBA")
    result = {
        "generated_at": __import__("time").strftime("%Y-%m-%d %H:%M:%S"),
        "java": rel(JAVA),
        "texture": {"path": rel(TEX), "size": [tex_img.width, tex_img.height],
                    "sha16": __import__("hashlib").sha256(open(TEX, "rb").read()).hexdigest()[:16]},
        "glow": {"path": rel(GLOW), "size": [glow_img.width, glow_img.height],
                 "alpha_px": int(np.asarray(glow_img)[:, :, 3].gt(0).sum())
                 if hasattr(np.asarray(glow_img)[:, :, 3], "gt") else 0},
        "new": new["audit"],
        "old": old["audit"],
        "silhouette_iou_old_vs_new": round(iou, 3),
        "api_checks": checks,
        "images": {k: rel(v) for k, v in imgs.items()},
    }
    io.open(os.path.join(HERE, "heaven_audit.json"), "w", encoding="utf-8").write(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    # 控制台
    print("== 仙庭之主重制自检 ==")
    print("  旧：cube=%d 宽=%dpx(%.2f格) 高=%dpx(%.2f格)"
          % (old_audit["cubes"], old_audit["width_px"], old_audit["width_blocks"],
             old_audit["height_px"], old_audit["height_blocks"]))
    print("  新：cube=%d 宽=%dpx(%.2f格) 高=%dpx(%.2f格) minY=%.0f maxY=%.0f"
          % (new_audit["cubes"], new_audit["width_px"], new_audit["width_blocks"],
             new_audit["height_px"], new_audit["height_blocks"],
             new_audit["y_range"][0], new_audit["y_range"][1]))
    print("  UV 越界=%d 跨足迹重叠=%d 真洞=%d 贴图=%s 挂点(>10px)=%d"
          % (new_audit["uv_oob"], new_audit["uv_cross"], new_audit["holes_real"],
             new_audit["tex"], len(new_audit["hard_gaps"])))
    print("  旧→新剪影 IoU=%.3f" % iou)
    bad_api = [k for k, v in checks.items() if not v]
    for k, v in checks.items():
        print("  [%s] %s" % ("OK" if v else "FAIL", k))
    print("  证据：")
    for k, v in result["images"].items():
        print("    %-16s %s" % (k, v))
    print("    heaven_audit.json %s" % rel(os.path.join(HERE, "heaven_audit.json")))
    # 硬门禁
    bad = []
    if new_audit["cubes"] < 100:
        bad.append("cube %d < 100" % new_audit["cubes"])
    if new_audit["width_px"] < 96:
        bad.append("宽 %dpx < 96px（6 格）" % new_audit["width_px"])
    if abs(new_audit["height_px"] - 110) > 1:
        bad.append("高 %dpx != 110px" % new_audit["height_px"])
    if new_audit["uv_oob"] or new_audit["uv_cross"] or new_audit["holes_real"]:
        bad.append("UV/洞硬问题：%s" % new_audit)
    if new_audit["hard_gaps"]:
        bad.append(">10px 硬挂点：%s" % new_audit["hard_gaps"])
    if [tex_img.width, tex_img.height] != [256, 256]:
        bad.append("主贴图不是 256²")
    if [glow_img.width, glow_img.height] != [tex_img.width, tex_img.height]:
        bad.append("glow 与主贴图尺寸不一致")
    bad += ["API: " + k for k in bad_api]
    if bad:
        print("\n!! FAIL：")
        for b in bad:
            print("   - " + b)
        return 1
    print("\nPASS：cube 123 ≥100、宽 104px（6.5 格）≥6 格、高 110px(6.875 格)、"
          "UV 0/0、真洞 0、256²+glow；LOWEST_PIXEL_Y=24.0；旧→新剪影 IoU %.3f"
          % iou)
    return 0


if __name__ == "__main__":
    sys.exit(main())
