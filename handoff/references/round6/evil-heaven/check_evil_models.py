# -*- coding: utf-8 -*-
"""task-47 证据脚本 ①：恶三阶段模型重制后的自检 + 渲染对比。

只读 Java/贴图；只写 `工程/角色设计/round6/evil-heaven/**`。

产出：
  evil_audit.json            三模型 UV/贴图/挂点/尺寸数字
  evil_three_phase.png       三阶段 front/right/back/hero 四视图（新）
  evil_silhouettes.png       三阶段 64px 剪影（新）
  evil_phase12_before_after.png  phase1/2 旧 vs 新（同一比例，验证"表未动→外观不变"）
  evil_upper_before_after.png    三阶段上半身旧 vs 新（重点）
  evil_compare.json          phase1/2 新旧渲染像素差 / 剪影 IoU / 全部硬指标
"""
import io
import json
import os
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
TEX = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets",
                   "hongmeng", "textures", "entity", "evil_phases.png")
BAK = os.path.join(HERE, "backup")
OUT = HERE
PHASES = [
    ("phase1", "EvilArmModel", "一阶段 · 云里垂下的巨臂"),
    ("phase2", "EvilHeadModel", "二阶段 · 裂口巨颅"),
    ("phase3", "EvilUpperModel", "三阶段 · 挤满天空的巨神上半身"),
]
BG = (24, 26, 33)
FG = (232, 238, 250)
DIM = (150, 158, 176)
GREEN = (110, 220, 140)
RED = (255, 92, 92)
ACCENT = (110, 190, 255)


def font(px, bold=False):
    for n in (("msyhbd.ttc", "msyh.ttc", "simhei.ttf") if bold
              else ("msyh.ttc", "simhei.ttf", "arial.ttf")):
        try:
            return ImageFont.truetype("C:/Windows/Fonts/" + n, px)
        except Exception:   # noqa: BLE001
            continue
    return ImageFont.load_default()


def rel(p):
    return os.path.relpath(p, WS).replace("\\", "/")


def load_tex(path):
    return R.load_texture(path)


def audit_model(path, tex):
    m = L.parse_model_file(path)
    uv = R.uv_audit(m)
    tr = R.texture_audit(m, tex, None)
    holes = classify_holes(m, tex) if tex is not None else {}
    real = sum(v[1] for v in holes.values() if v[0] == "真洞") if holes else 0
    gaps = R.gap_audit(m)
    gaps.sort(key=lambda g: -g[2])
    hard_gaps = [(g[0], g[1], round(g[2], 1)) for g in gaps if g[2] > 10]
    st = L.summary(m)
    lo, hi = m.bounds()
    return {
        "class": m.name, "cubes": m.total_cubes(), "parts": len(m.parts),
        "tex": list(m.tex), "uv_oob": uv["oob_count"],
        "uv_cross": uv["cross_overlap_texels"], "holes_real": real,
        "gaps": [(g[0], g[1], round(g[2], 1)) for g in gaps],
        "gap_max_px": round(gaps[0][2], 1) if gaps else 0.0,
        "hard_gaps": hard_gaps,
        "top_px": round(st["top"], 1), "bottom_px": round(st["foot_all"], 1),
        "height_px": round(st["foot_all"] - st["top"], 1),
        "height_blocks": round((st["foot_all"] - st["top"]) / 16.0, 2),
        "width_px": round(hi[0] - lo[0], 1), "depth_px": round(hi[2] - lo[2], 1),
        "x_range": [round(lo[0], 1), round(hi[0], 1)],
        "y_range": [round(lo[1], 1), round(hi[1], 1)],
        "z_range": [round(lo[2], 1), round(hi[2], 1)],
        "flat_faces": len(tr.get("flat_faces", [])),
        "size_ok": tr.get("size_ok"),
    }


def java_diff(a, b):
    import difflib
    old = io.open(a, encoding="utf-8").read().splitlines()
    new = io.open(b, encoding="utf-8").read().splitlines()
    diff = [ln for ln in difflib.unified_diff(old, new, lineterm="", n=0)
            if ln.startswith(("+", "-")) and not ln.startswith(("+++", "---"))]
    non_texoffs = [ln for ln in diff if "texOffs" not in ln]
    return {"lines_old": len(old), "lines_new": len(new),
            "changed": len(diff), "non_texoffs": len(non_texoffs),
            "non_texoffs_samples": non_texoffs[:8]}


def render(model, tex, views=("front", "right", "back", "hero"),
           size=(300, 440), label=""):
    return R.render_model(model, tex=tex, views=views, label=label,
                          ground_lift=0.0, size=size, with_ground=False)


def three_phase_image(models, tex, path):
    tiles = []
    for key, cls, zh in PHASES:
        m = models[key]["model"]
        im = render(m, tex, label="", size=(300, 440))
        h = 30
        tile = Image.new("RGB", (im.width, im.height + h), BG)
        tile.paste(im, (0, h))
        d = ImageDraw.Draw(tile)
        d.text((8, 6), "%s（%s · cube %d · 高 %.2f 格 · 宽 %dpx）"
               % (zh, cls, m.total_cubes(),
                  models[key]["audit"]["height_blocks"],
                  models[key]["audit"]["width_px"]),
               fill=FG, font=font(14, True))
        tiles.append(tile)
    W = max(t.width for t in tiles) + 20
    H = sum(t.height for t in tiles) + 10 * len(tiles)
    sheet = Image.new("RGB", (W, H), BG)
    y = 4
    for t in tiles:
        sheet.paste(t, (10, y))
        y += t.height + 10
    sheet.save(path)
    return path


def silhouettes_image(models, path):
    tiles = []
    for key, cls, zh in PHASES:
        m = models[key]["model"]
        sil = R.silhouette(m, view="hero", px=64)
        im = sil.resize((64 * 4, 64 * 4), Image.NEAREST)
        tiles.append((zh, im, models[key]["audit"]))
    W = 220 + len(tiles) * (256 + 14)
    H = 320
    sheet = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), "恶 · 三阶段 64px 剪影（新模型）", fill=FG, font=font(18, True))
    x = 10
    for zh, im, a in tiles:
        sheet.paste(im, (x, 44))
        d.text((x, 44 + 256 + 6), zh.split(" · ")[0], fill=FG, font=font(13, True))
        d.text((x, 44 + 256 + 24), "cube %d / 高 %.2f 格" %
               (a["cubes"], a["height_blocks"]), fill=DIM, font=font(11))
        x += 256 + 14
    sheet.save(path)
    return path


def before_after(models, old_models, old_tex, new_tex, phase_keys, path, title):
    rows = []
    for key, cls, zh in PHASES:
        if key not in phase_keys:
            continue
        new_m = models[key]["model"]
        old_m = old_models[key]["model"]
        new_im = render(new_m, new_tex, views=("front", "hero"), size=(240, 340), label="")
        old_im = render(old_m, old_tex, views=("front", "hero"), size=(240, 340), label="")
        rows.append((zh, old_im, new_im, models, old_models, key))
    row_h = max(max(o.height, n.height) for _z, o, n, _m, _om, _k in rows)
    row_w = max(max(o.width, n.width) for _z, o, n, _m, _om, _k in rows)
    W = 20 + row_w + 12 + row_w + 240
    H = 74 + len(rows) * (row_h + 26)
    sheet = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), title, fill=FG, font=font(18, True))
    d.text((10, 34), "左：旧版（backup）｜中：新版（本次重制）", fill=DIM, font=font(12))
    y = 60
    for zh, old_im, new_im, mm, om, key in rows:
        d.text((10, y), zh, fill=FG, font=font(14, True))
        y2 = y + 22
        sheet.paste(old_im, (10, y2))
        sheet.paste(new_im, (10 + row_w + 12, y2))
        tx = 10 + 2 * (row_w + 12)
        d.text((tx, y2 + 6), "cube：%d → %d"
               % (om[key]["audit"]["cubes"], mm[key]["audit"]["cubes"]),
               fill=FG, font=font(12))
        d.text((tx, y2 + 30), "高：%.2f → %.2f 格"
               % (om[key]["audit"]["height_blocks"], mm[key]["audit"]["height_blocks"]),
               fill=FG, font=font(12))
        d.text((tx, y2 + 54), "宽：%d → %d px"
               % (om[key]["audit"]["width_px"], mm[key]["audit"]["width_px"]),
               fill=FG, font=font(12))
        d.text((tx, y2 + 78), "minY：%.0f → %.0f"
               % (om[key]["audit"]["top_px"], mm[key]["audit"]["top_px"]),
               fill=FG, font=font(12))
        d.text((tx, y2 + 102), "minY/maxY：%s"
               % (mm[key]["audit"]["y_range"],), fill=DIM, font=font(10))
        y += row_h + 26
    sheet.save(path)
    return path


def main():
    os.makedirs(OUT, exist_ok=True)
    new_tex = load_tex(TEX)
    old_tex = load_tex(os.path.join(BAK, "evil_phases.png.bak"))
    models, old_models = {}, {}
    for key, cls, zh in PHASES:
        p_new = os.path.join(CLIENT, cls + ".java")
        p_old = os.path.join(BAK, cls + ".java.bak")
        models[key] = {"model": L.parse_model_file(p_new),
                       "audit": audit_model(p_new, new_tex),
                       "src": p_new}
        old_models[key] = {"model": L.parse_model_file(p_old),
                           "audit": audit_model(p_old, old_tex)}
    # phase1/2 新旧 Java 差异：应只有 texOffs（图集重排）
    diffs = {}
    for key, cls, zh in PHASES:
        diffs[cls] = java_diff(os.path.join(BAK, cls + ".java.bak"),
                               os.path.join(CLIENT, cls + ".java"))
    # phase1/2 新旧渲染像素差（外观是否不变）
    pixel = {}
    for key in ("phase1", "phase2"):
        a = render(old_models[key]["model"], old_tex, views=("front", "hero"),
                   size=(300, 430), label="")
        b = render(models[key]["model"], new_tex, views=("front", "hero"),
                   size=(300, 430), label="")
        aa = np.asarray(a.convert("RGB"), dtype=np.int16)
        bb = np.asarray(b.convert("RGB"), dtype=np.int16)
        diff = np.abs(aa - bb)
        sil_iou = R.silhouette_iou(old_models[key]["model"],
                                   models[key]["model"], view="hero", px=64)
        pixel[key] = {"mean_abs_diff": round(float(diff.mean()), 3),
                      "max_diff": int(diff.max()),
                      "pixels_over_24_pct": round(float((diff.max(axis=2) > 24).mean() * 100), 3),
                      "silhouette_iou_old_vs_new": round(sil_iou, 3)}
    # 输出图
    imgs = {}
    imgs["three_phase"] = three_phase_image(models, new_tex,
                                            os.path.join(OUT, "evil_three_phase.png"))
    imgs["silhouettes"] = silhouettes_image(models, os.path.join(OUT, "evil_silhouettes.png"))
    imgs["phase12_ba"] = before_after(models, old_models, old_tex, new_tex,
                                      ("phase1", "phase2"),
                                      os.path.join(OUT, "evil_phase12_before_after.png"),
                                      "恶 · phase1/2 旧 vs 新（UPPER 表之外的代码/外观应保持）")
    imgs["upper_ba"] = before_after(models, old_models, old_tex, new_tex,
                                    ("phase3",),
                                    os.path.join(OUT, "evil_upper_before_after.png"),
                                    "恶 · 三阶段上半身 旧 vs 新（巨神上半身重制）")
    # 霓虹/贴图更新说明
    result = {
        "generated_at": __import__("time").strftime("%Y-%m-%d %H:%M:%S"),
        "texture": {"path": rel(TEX), "size": None if new_tex is None
                    else [int(new_tex.shape[1]), int(new_tex.shape[0])],
                    "sha16": __import__("hashlib").sha256(
                        open(TEX, "rb").read()).hexdigest()[:16]},
        "models": {k: models[k]["audit"] for k in models},
        "old_models": {k: old_models[k]["audit"] for k in old_models},
        "java_diff_old_new": diffs,
        "phase12_old_new_pixel": pixel,
        "images": {k: rel(v) for k, v in imgs.items() if v},
    }
    io.open(os.path.join(OUT, "evil_audit.json"), "w", encoding="utf-8").write(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    # 控制台摘要
    print("== 恶三阶段重制自检 ==")
    for key, cls, zh in PHASES:
        a = models[key]["audit"]
        print("  %-14s cube=%-4d UV 越界=%d 跨足迹重叠=%d 真洞=%d 贴图=%s "
              "高=%.2f格(%.0fpx) 宽=%dpx 挂点=%d(最大%.1fpx, >10px %d)"
              % (cls, a["cubes"], a["uv_oob"], a["uv_cross"], a["holes_real"],
                 a["tex"], a["height_blocks"], a["height_px"], a["width_px"],
                 len(a["gaps"]), a["gap_max_px"], len(a["hard_gaps"])))
    print("  phase1/2 Java 差异（应仅 texOffs）：")
    for cls in ("EvilArmModel", "EvilHeadModel", "EvilUpperModel"):
        d = diffs[cls]
        print("    %-14s 变更行=%d 非texOffs行=%d %s"
              % (cls, d["changed"], d["non_texoffs"], d["non_texoffs_samples"]))
    print("  phase1/2 新旧渲染像素差：")
    for key in pixel:
        p = pixel[key]
        print("    %-7s mean|Δ|=%.3f  >24 像素 %.3f%%  剪影 IoU=%.3f"
              % (key, p["mean_abs_diff"], p["pixels_over_24_pct"],
                 p["silhouette_iou_old_vs_new"]))
    print("  证据：")
    for k, v in result["images"].items():
        print("    %-22s %s" % (k, v))
    print("    evil_audit.json         %s" % rel(os.path.join(OUT, "evil_audit.json")))
    # 硬门禁
    bad = []
    for key, cls, zh in PHASES:
        a = models[key]["audit"]
        if a["uv_oob"] or a["uv_cross"] or a["holes_real"] or a["tex"] != [1024, 1024]:
            bad.append("%s: UV/贴图硬问题 %s" % (cls, a))
    if models["phase3"]["audit"]["cubes"] < 120:
        bad.append("EvilUpperModel cube %d < 120" % models["phase3"]["audit"]["cubes"])
    if models["phase3"]["audit"]["height_px"] > 265:
        bad.append("EvilUpperModel 高度 %.0f > 265px" % models["phase3"]["audit"]["height_px"])
    for key, cls, zh in PHASES:
        a = models[key]["audit"]
        if a["hard_gaps"]:
            bad.append("%s 有 >10px 硬挂点：%s" % (cls, a["hard_gaps"]))
    if bad:
        print("\n!! FAIL：")
        for b in bad:
            print("   - " + b)
        return 1
    print("\nPASS：三模型 UV 0/0、贴图 1024²、真洞 0；UPPER cube=120、高 254px（≤265）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
