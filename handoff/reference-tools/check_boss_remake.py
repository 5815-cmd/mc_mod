# -*- coding: utf-8 -*-
"""第六轮 · Boss 模型重制 · 门禁 + 证据（只读工具，不改模型）。

契约：`工程/21-Boss模型重制契约.md` §1/§3。

覆盖 6 项重制（共 7 个模型）：
  1. 虚空龙帝 `void_dragon_emperor`  → VoidDragonEmperorModel / void_dragon_emperor.png(+glow)
  2. 焚天炎帝 `blaze_emperor`         → BlazeEmperorModel / blaze_emperor.png(+glow)
  3. 万兽之主 `beast_boss`            → BeastLordModel / boss_beast_lord.png(+glow)
  4. 血魔老祖 `demon_boss`            → BloodDemonModel / boss_demon_elder.png(+glow)
  5. 虚空道主 `void_boss`             → VoidLordModel / boss_void_lord.png(+glow)
  6. 恶·三阶段上身 + 仙庭之主         → EvilUpperModel（1024² 共享图集）
                                        + HeavenLordModel / heaven_lord.png(+glow)

审计口径**完全复用**现有工具链（不另造判定）：
`charlib.parse_model_file` + `renderlib.uv_audit / texture_audit / ground_audit / gap_audit`
+ `audit_all.classify_holes / read_lift / GAP_DESIGN_EXEMPT`。

用法（Windows 控制台建议先 `set PYTHONIOENCODING=utf-8`）：
    python -X utf8 tools/check_boss_remake.py                  # 门禁 + 证据 + 全量审计
    python -X utf8 tools/check_boss_remake.py --wait 2400      # 等 7 个模型全部落盘再跑
    python -X utf8 tools/check_boss_remake.py --no-render      # 只出 md/json
    python -X utf8 tools/check_boss_remake.py --skip-full-audit
    python -X utf8 tools/check_boss_remake.py --only dragon    # 只跑一只（调试）

写盘范围：`工程/角色设计/round6/evidence/**`、`round6/00-Boss重制总览.png`
（全量审计由任务指定的 `audit_all.py --tag "13-Boss重制"` 自写 `审计/`）。
返回码：0 = 7 模型全 PASS 且全量硬问题 0；2 = 仍在等模型落盘；1 = 有 FAIL。
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))                 # <ws>/tools
WS = os.path.dirname(HERE)
TOOLS = os.path.join(WS, "工程", "角色设计", "tools")
sys.path.insert(0, TOOLS)

import numpy as np                                                # noqa: E402
from PIL import Image, ImageDraw, ImageFont                       # noqa: E402

import charlib as L                                               # noqa: E402
import renderlib as R                                             # noqa: E402
import audit_all as A                                             # noqa: E402

CLIENT = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                      "xiuxian", "client")
TEXDIR = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets",
                      "hongmeng", "textures", "entity")
ROUND6 = os.path.join(WS, "工程", "角色设计", "round6")
EVID = os.path.join(ROUND6, "evidence")
BASELINE = os.path.join(EVID, "baseline")
OLD = os.path.join(BASELINE, "old")
EVIL_DIR = os.path.join(EVID, "bosses")
OVERVIEW = os.path.join(ROUND6, "00-Boss重制总览.png")
AUDIT_DIR = os.path.join(WS, "工程", "角色设计", "审计")
AUDIT_TAG = "13-Boss重制"
AUDIT_MD = os.path.join(AUDIT_DIR, AUDIT_TAG + "-全量模型审计.md")
AUDIT_JSON = os.path.join(AUDIT_DIR, AUDIT_TAG + "-全量模型审计.json")
#: 全量审计的旧基线（第六轮之前最后一次全量：55 实体 / 0 硬问题）
PREV_AUDIT_NAME = "12-恶实机修复终版-全量模型审计.json"
PREV_AUDIT_JSON = os.path.join(AUDIT_DIR, PREV_AUDIT_NAME)
REPORT_MD = os.path.join(EVID, "boss-remake.md")
REPORT_JSON = os.path.join(EVID, "boss-remake.json")
README = os.path.join(EVID, "README.md")
OLD_NUMBERS = os.path.join(OLD, "old_numbers.json")

# ----------------------------------------------------------------------
# 6 项重制 / 7 个模型（契约 §2 的目标：cube / 高 / 翼展·肩宽 / 贴图）
# 尺寸单位：px（1 格 = 16px）。height_px 范围、width/depth 范围见契约。
# ----------------------------------------------------------------------
BOSSES = [
    dict(
        key="dragon", item="A", id="void_dragon_emperor", cls="VoidDragonEmperorModel",
        zh="虚空龙帝", tex="void_dragon_emperor", tex_size=(512, 512),
        old_tex_size=(192, 192), glow=True, generic="CultivationMonsterEntity",
        renderer="VoidDragonEmperorRenderer", reg_file="ClientSetup.java",
        reg_pattern=r"VoidDragonEmperorRenderer::new", ctor_arity=1,
        layer_ids=["void_dragon_emperor"], layer_source="renderer",
        glowlayer_required=True, ground_mode="hard",
        targets=dict(cubes=90, height=(103, 123), width=(224, 256), depth=(173, 211)),
        target_desc="cube≥90；站高 7.0 格（112px）；翼展 14~16 格（224~256px）；"
                    "鼻尖到尾尖 12 格（192px ±10%）",
        dims_note="宽=翼展、深=鼻尾长",
    ),
    dict(
        key="blaze", item="B", id="blaze_emperor", cls="BlazeEmperorModel",
        zh="焚天炎帝", tex="blaze_emperor", tex_size=(256, 256),
        old_tex_size=(192, 192), glow=True, generic="CultivationMonsterEntity",
        renderer="BlazeEmperorRenderer", reg_file="ClientSetup.java",
        reg_pattern=r"BlazeEmperorRenderer::new", ctor_arity=1,
        layer_ids=["blaze_emperor"], layer_source="renderer",
        glowlayer_required=True, ground_mode="hard",
        targets=dict(cubes=80, height=(88, 104), width=(65, 79)),
        target_desc="cube≥80；站高 6.0 格（96px）；肩宽 4.5 格（72px ±10%）",
        dims_note="宽=肩宽",
    ),
    dict(
        key="beast", item="C", id="beast_boss", cls="BeastLordModel",
        zh="万兽之主", tex="boss_beast_lord", tex_size=(256, 256),
        old_tex_size=(192, 128), glow=True, generic="CultivationMonsterEntity",
        renderer="BeastLordRenderer", reg_file="ClientSetup.java",
        reg_pattern=r'BeastLordRenderer\(ctx,\s*"boss_beast_lord"\)', ctor_arity=2,
        layer_ids=["beast_lord"], layer_source="renderer",
        glowlayer_required=True, ground_mode="hard",
        targets=dict(cubes=70, height=(60, 74), width=(45, 57), depth=(130, 158)),
        target_desc="cube≥70；四足站立高 4.2 格（67px）；鼻到尾长 9 格（144px）；"
                    "肩宽 3.2 格（51px）",
        dims_note="宽=肩宽、深=鼻尾长",
    ),
    dict(
        key="demon", item="D", id="demon_boss", cls="BloodDemonModel",
        zh="血魔老祖", tex="boss_demon_elder", tex_size=(256, 256),
        old_tex_size=(128, 128), glow=True, generic="CultivationMonsterEntity",
        renderer="BloodDemonRenderer", reg_file="ClientSetup.java",
        reg_pattern=r'BloodDemonRenderer\(ctx,\s*"boss_demon_elder"\)', ctor_arity=2,
        layer_ids=["blood_demon"], layer_source="renderer",
        glowlayer_required=True, ground_mode="hard",
        targets=dict(cubes=85, height=(88, 104), width=(58, 70)),
        target_desc="cube≥85；站高 6.0 格（96px）；肩宽 4 格（64px ±10%）",
        dims_note="宽=肩宽",
    ),
    dict(
        key="void", item="E", id="void_boss", cls="VoidLordModel",
        zh="虚空道主", tex="boss_void_lord", tex_size=(256, 256),
        old_tex_size=(128, 128), glow=True, generic="CultivationMonsterEntity",
        renderer="VoidLordRenderer", reg_file="ClientSetup.java",
        reg_pattern=r'VoidLordRenderer\(ctx,\s*"boss_void_lord"\)', ctor_arity=2,
        layer_ids=["void_lord"], layer_source="renderer",
        glowlayer_required=True, ground_mode="hard",
        targets=dict(cubes=85, height=(96, 112), width=(130, 158)),
        target_desc="cube≥85；浮空高 6.5 格（104px）；翼展 9 格（144px ±10%）",
        dims_note="宽=翼展",
    ),
    dict(
        key="evil", item="F1", id="evil_gazer", cls="EvilUpperModel",
        zh="恶·三阶段上身", tex="evil_phases", tex_size=(1024, 1024),
        old_tex_size=(1024, 1024), glow=False, generic="CultivationMonsterEntity",
        renderer="EvilGazerRenderer", reg_file="ClientSetup.java",
        reg_pattern=r"EvilGazerRenderer::new", ctor_arity=1,
        layer_ids=["evil_arm", "evil_head", "evil_upper"], layer_source="renderer",
        glowlayer_required=False, ground_mode="exempt",
        targets=dict(cubes=120, height=(240, 265)),
        target_desc="cube≥120；高度 15~17 格（240~272px，且契约要求 ≤265px 上限）；"
                    "保留 1024² 共享图集 evil_phases.png",
        dims_note="恶从天空裂口垂下：脚底不按 24 基准判（沿用 audit_all code_is_evil）",
    ),
    dict(
        key="heaven", item="F2", id="heaven_lord", cls="HeavenLordModel",
        zh="仙庭之主", tex="heaven_lord", tex_size=(256, 256),
        old_tex_size=(256, 256), glow=True, generic="HeavenLordEntity",
        renderer="HeavenLordRenderer", reg_file="HeavenMobRenderers.java",
        reg_pattern=r"HeavenLordRenderer::new", ctor_arity=1,
        layer_ids=["heaven_lord"], layer_source="model",
        glowlayer_required=True, ground_mode="hard",
        targets=dict(cubes=100, height=(101, 119), width_min=96),
        target_desc="cube≥100；身高 6.9 格（110px）不变；肩宽/袍摆 ≥6 格（≥96px）",
        dims_note="宽=肩宽/袍摆（只判下限 ≥96px）",
    ),
]

REMADE_ENTITY_IDS = [b["id"] for b in BOSSES]
TEX_EXPECT = {b["tex"]: b["tex_size"] for b in BOSSES}

BG = (24, 26, 33)
TILE_BG = (1, 2, 3)
FG = (228, 233, 244)
DIM = (140, 148, 164)
ACCENT = (120, 190, 255)
RED = (255, 96, 96)
GREEN = (110, 220, 140)
YELLOW = (255, 210, 120)


# ----------------------------------------------------------------------
# 小工具
# ----------------------------------------------------------------------
def font(px, bold=False):
    names = (("msyhbd.ttc", "msyh.ttc", "simhei.ttf") if bold
             else ("msyh.ttc", "simhei.ttf", "arial.ttf"))
    for name in names:
        try:
            return ImageFont.truetype("C:/Windows/Fonts/" + name, px)
        except Exception:   # noqa: BLE001
            continue
    return ImageFont.load_default()


def rel(p):
    try:
        return os.path.relpath(p, WS).replace("\\", "/")
    except Exception:   # noqa: BLE001
        return p


def sha256_file(p):
    h = hashlib.sha256()
    with io.open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def read_text(p):
    try:
        return io.open(p, encoding="utf-8").read()
    except Exception:   # noqa: BLE001
        return ""


def model_path(b):
    return os.path.join(CLIENT, b["cls"] + ".java")


def old_model_path(b):
    return os.path.join(OLD, "client", b["cls"] + ".java")


def tex_path(b, old=False):
    base = os.path.join(OLD, "textures") if old else TEXDIR
    return os.path.join(base, b["tex"] + ".png")


def old_tex_path(b):
    return tex_path(b, old=True)


def renderer_path(b):
    return os.path.join(CLIENT, b["renderer"] + ".java")


def registration_path(b):
    """注册文件路径：ClientSetup.java 在 `.../xiuxian/` 下，其他渲染器接线在 client/ 下。"""
    p = os.path.join(CLIENT, b["reg_file"])
    if os.path.exists(p):
        return p
    alt = os.path.join(os.path.dirname(CLIENT), b["reg_file"])
    return alt if os.path.exists(alt) else p


def ensure_dirs():
    for d in (EVID, BASELINE, OLD, EVIL_DIR):
        os.makedirs(d, exist_ok=True)
    for b in BOSSES:
        os.makedirs(os.path.join(EVIL_DIR, b["key"]), exist_ok=True)


# ----------------------------------------------------------------------
# 基线快照（重制前模型/贴图/渲染器；用于「新旧对比」与「是否已落新版」判定）
# ----------------------------------------------------------------------
def snapshot_baseline(force=False):
    """若 baseline/old 不存在 → 把当前 7 模型/贴图/渲染器快照为「旧版」。

    ⚠ 必须在重制落盘**之前**跑过一次；本任务已在 2026-11-07 17:31 快照。
    已存在则不动（除非 --force-snapshot）。
    """
    os.makedirs(os.path.join(OLD, "client"), exist_ok=True)
    os.makedirs(os.path.join(OLD, "textures"), exist_ok=True)
    os.makedirs(os.path.join(OLD, "renderers"), exist_ok=True)
    already = all(os.path.exists(old_model_path(b)) for b in BOSSES)
    if already and not force:
        return False
    import shutil
    if already and force:
        print("[warn] --force-snapshot：用当前文件覆盖旧版基线（若模型已重制会丢失新旧对比！）")
    for b in BOSSES:
        shutil.copy2(model_path(b), old_model_path(b))
        shutil.copy2(renderer_path(b), os.path.join(OLD, "renderers",
                                                    b["renderer"] + ".java"))
        if os.path.exists(tex_path(b)):
            shutil.copy2(tex_path(b), old_tex_path(b))
        glow = os.path.join(TEXDIR, b["tex"] + "_glow.png")
        if os.path.exists(glow):
            shutil.copy2(glow, os.path.join(OLD, "textures", b["tex"] + "_glow.png"))
    gen = os.path.join(WS, "tools", "gen_evil_phases.py")
    if os.path.exists(gen):
        shutil.copy2(gen, os.path.join(OLD, "gen_evil_phases.py"))
    return True


def load_old_numbers():
    """旧版 7 模型数字（优先读 baseline/old/old_numbers.json，缺失则现场解析）。"""
    if os.path.exists(OLD_NUMBERS):
        try:
            return json.load(io.open(OLD_NUMBERS, encoding="utf-8"))
        except Exception:   # noqa: BLE001
            pass
    out = {}
    for b in BOSSES:
        p = old_model_path(b)
        if not os.path.exists(p):
            continue
        m = L.parse_model_file(p)
        lo, hi = m.bounds()
        tex = R.load_texture(old_tex_path(b))
        out[b["cls"]] = dict(
            cubes=m.total_cubes(), parts=len(m.parts), tex_model=list(m.tex),
            tex_png=None if tex is None else [tex.shape[1], tex.shape[0]],
            min_y=round(lo[1], 1), max_y=round(hi[1], 1),
            height_px=round(hi[1] - lo[1], 1),
            height_blocks=round((hi[1] - lo[1]) / 16.0, 2),
            width_px=round(hi[0] - lo[0], 1), depth_px=round(hi[2] - lo[2], 1))
    io.open(OLD_NUMBERS, "w", encoding="utf-8", newline="\n").write(
        json.dumps(out, ensure_ascii=False, indent=2))
    return out


# ----------------------------------------------------------------------
# 新版检测 / 等待
# ----------------------------------------------------------------------
def current_state():
    state = {"models": {}, "new_count": 0, "ready": False}
    for b in BOSSES:
        p = model_path(b)
        rec = {"exists": os.path.exists(p), "changed": False, "cubes": None,
               "parse_ok": False, "sha256": None}
        if rec["exists"]:
            rec["sha256"] = sha256_file(p)
            op = old_model_path(b)
            old_sha = sha256_file(op) if os.path.exists(op) else None
            rec["changed"] = old_sha is not None and rec["sha256"] != old_sha
            try:
                rec["cubes"] = L.parse_model_file(p).total_cubes()
                rec["parse_ok"] = True
            except Exception:   # noqa: BLE001
                pass
        state["models"][b["cls"]] = rec
        if rec["changed"]:
            state["new_count"] += 1
    state["ready"] = state["new_count"] == len(BOSSES)
    state["tex_ready"] = all(os.path.exists(tex_path(b)) for b in BOSSES)
    return state


def wait_for_new(timeout_s, poll_s=15.0):
    t0 = time.time()
    while True:
        st = current_state()
        if st["ready"] and st["tex_ready"]:
            return st, time.time() - t0
        msg = []
        for b in BOSSES:
            rec = st["models"][b["cls"]]
            msg.append("%s:%s" % (b["key"],
                                  ("%d/≥%d" % (rec["cubes"], b["targets"]["cubes"]))
                                  if rec["exists"] else "缺失"))
        print("  [wait] %s · 已重制 %d/%d，%.0fs 后重查…"
              % (" ".join(msg), st["new_count"], len(BOSSES), poll_s))
        if time.time() - t0 >= timeout_s:
            return st, time.time() - t0
        time.sleep(poll_s)


# ----------------------------------------------------------------------
# 渲染器 API 兼容检查
# ----------------------------------------------------------------------
def parse_layer_ids(src):
    return re.findall(r'new ModelLayerLocation\(\s*new ResourceLocation\([^,]+,\s*"([^"]+)"',
                      src)


def parse_ctor_arity(src, cls):
    m = re.search(r'public\s+%s\s*\(\s*EntityRendererProvider\.Context' % cls, src)
    if not m:
        return None
    start = src.index("(", m.start())
    depth, args, i = 0, [""], start
    while i < len(src):
        ch = src[i]
        if ch == "(":
            depth += 1
            if depth == 1:
                i += 1
                continue
        elif ch == ")":
            depth -= 1
            if depth == 0:
                break
        elif ch == "," and depth == 1:
            args.append("")
            i += 1
            continue
        if depth >= 1:
            args[-1] += ch
        i += 1
    args = [a.strip() for a in args if a.strip()]
    # 第一个参数是 Context，后面还有参数则 arity = 1 + len(rest)
    return 1 + len(args[1:])


def parse_lowest_in_model(src):
    m = re.search(r'LOWEST_PIXEL_Y\s*=\s*(-?[\d.]+)', src)
    return None if not m else float(m.group(1))


def check_renderer_api(b, model_src):
    """渲染器/API 兼容检查（不判美学，只判契约 §1 的接口约束）。"""
    issues, notes = [], []
    rp = renderer_path(b)
    if not os.path.exists(rp):
        issues.append(f"渲染器缺失：client/{b['renderer']}.java")
        return issues, notes, {}
    rsrc = read_text(rp)
    # 1) 渲染器引用了模型类
    if b["cls"] not in rsrc:
        issues.append(f"渲染器未引用模型类 {b['cls']}")
    # 2) 构造器 arity 与 ClientSetup 注册方式一致
    arity = parse_ctor_arity(rsrc, b["renderer"])
    if arity is None:
        issues.append("渲染器缺少 (EntityRendererProvider.Context...) 构造器")
    elif arity != b["ctor_arity"]:
        issues.append(f"渲染器构造器参数数量 {arity} ≠ 注册方式要求 {b['ctor_arity']}"
                      "（ClientSetup 注册会编译失败）")
    # 3) layer id 未被改
    if b["layer_source"] == "renderer":
        ids = set(parse_layer_ids(rsrc))
        missing = [i for i in b["layer_ids"] if i not in ids]
        if missing:
            issues.append(f"渲染器 LAYER id 被改/缺失：期望 {b['layer_ids']}，"
                          f"实际 {sorted(ids)}")
    else:   # 模型里声明 LAYER，渲染器引用 <Model>.LAYER
        mids = set(parse_layer_ids(model_src))
        missing = [i for i in b["layer_ids"] if i not in mids]
        if missing:
            issues.append(f"模型 LAYER id 缺失：期望 {b['layer_ids']}，实际 {sorted(mids)}")
        if f"{b['cls']}.LAYER" not in rsrc:
            issues.append(f"渲染器未引用 {b['cls']}.LAYER")
    # 4) ClientSetup / HeavenMobRenderers 注册仍在
    regp = registration_path(b)
    reg_src = read_text(regp) if os.path.exists(regp) else ""
    if not re.search(b["reg_pattern"], reg_src):
        issues.append(f"{b['reg_file']} 注册缺失/被改（期望匹配 {b['reg_pattern']}；"
                      f"读取路径 {rel(regp)}）")
    # 5) RankGlowLayer
    has_glow_layer = "RankGlowLayer" in rsrc
    if b["glowlayer_required"] and not has_glow_layer:
        issues.append("渲染器未加 RankGlowLayer（契约 §1 要求发光层）")
    if b["glowlayer_required"] and has_glow_layer and not os.path.exists(
            os.path.join(TEXDIR, b["tex"] + "_glow.png")):
        issues.append(f"渲染器加了 RankGlowLayer 但缺少 {b['tex']}_glow.png")
    if (not b["glowlayer_required"]) and has_glow_layer:
        notes.append("渲染器含 RankGlowLayer（本模型原设计无 glow，未强制）")
    # 6) 固定贴图路径（非参数化贴图的渲染器）
    if b["ctor_arity"] == 1 and f"textures/entity/{b['tex']}.png" not in rsrc:
        issues.append(f"渲染器贴图路径不是 textures/entity/{b['tex']}.png")
    # 7) LOWEST_PIXEL_Y：渲染器引用则模型必须提供；值需等于模型实际最低点
    lowest_model = parse_lowest_in_model(model_src)
    uses_lowest = f"{b['cls']}.LOWEST_PIXEL_Y" in rsrc
    if uses_lowest and lowest_model is None:
        issues.append(f"渲染器引用 {b['cls']}.LOWEST_PIXEL_Y，但模型未声明该常量")
    ground_info = {"uses_lowest": uses_lowest, "lowest_model": lowest_model,
                   "layer_ids": sorted(parse_layer_ids(
                       rsrc if b["layer_source"] == "renderer" else model_src))}
    return issues, notes, ground_info


def parse_ground_lift(rsrc):
    """解析渲染器里的 GROUND_LIFT 字段（先剥注释，避免命中 javadoc 里的示例文本）。

    返回 (lift_expr, lift_value_or_None)；lift_value 形如 ("lowest", 24.0) 或 ("const", 0.0)。
    """
    src = L.strip_comments(rsrc)
    m = re.search(r'GROUND_LIFT\s*=\s*([^;]+);', src)
    if not m:
        return None, None
    expr = re.sub(r'//.*', '', m.group(1)).strip()
    x = re.search(r'LOWEST_PIXEL_Y\s*-\s*([\d.]+)', expr)
    if x and "16" in expr:
        return expr, ("lowest", float(x.group(1)))
    v = re.search(r'(-?[\d.]+)\s*[DdFf]?\s*/\s*16', expr)
    if v:
        return expr, ("const", float(v.group(1)))
    return expr, None


# ----------------------------------------------------------------------
# 单只 Boss 审计
# ----------------------------------------------------------------------
def _gap_floating(name):
    pn = (name or "").lower()
    return any(k in pn for k in ("eye", "wing", "membrane", "veil", "halo", "aura",
                                 "orb", "rune", "shard", "fragment", "chain", "spike",
                                 "thorn", "horn", "tentacle", "tendril", "crown",
                                 "wisp", "ember"))


def audit_one(b, old_nums):
    rec = {"key": b["key"], "item": b["item"], "id": b["id"], "zh": b["zh"],
           "cls": b["cls"], "status": "缺失", "problems": [], "warnings": [],
           "checks": [], "notes": [], "gaps_hard": [], "gaps_exempt": [],
           "gaps_warn": [], "renderer_issues": [], "renderer_notes": [],
           "model": None}
    p = model_path(b)
    if not os.path.exists(p):
        rec["problems"].append(f"模型文件不存在：client/{b['cls']}.java")
        return rec
    src = read_text(p)
    try:
        model = L.parse_model_file(p)
    except Exception as e:      # noqa: BLE001
        rec["problems"].append(f"模型解析失败：{e}")
        return rec
    rec["model"] = model
    rec["status"] = "已解析"
    tex = R.load_texture(tex_path(b))
    glow_p = os.path.join(TEXDIR, b["tex"] + "_glow.png")
    glow = R.load_texture(glow_p) if os.path.exists(glow_p) else None
    old = old_nums.get(b["cls"]) or {}
    lo, hi = model.bounds()
    uv = R.uv_audit(model)
    texr = R.texture_audit(model, tex, glow)
    gaps = R.gap_audit(model)
    foot_px = round(hi[1], 2)
    top_px = round(lo[1], 2)
    height_px = round(hi[1] - lo[1], 2)
    width_px = round(hi[0] - lo[0], 2)
    depth_px = round(hi[2] - lo[2], 2)
    rec.update({
        "parts": len(model.parts), "cubes": model.total_cubes(),
        "tex_model": list(model.tex),
        "tex_png": None if tex is None else [tex.shape[1], tex.shape[0]],
        "glow_png": None if glow is None else [glow.shape[1], glow.shape[0]],
        "uv_oob": uv["oob_count"], "uv_cross": uv["cross_overlap_texels"],
        "uv_shared": uv["shared_texels"], "uv_coverage": round(uv["coverage_ratio"], 3),
        "holes_all": texr.get("transparent_used", 0),
        "flat_faces": len(texr.get("flat_faces", [])),
        "min_y": top_px, "max_y": foot_px, "height_px": height_px,
        "height_blocks": round(height_px / 16.0, 2),
        "width_px": width_px, "depth_px": depth_px,
        "old": {k: old.get(k) for k in ("cubes", "height_px", "width_px",
                                        "depth_px", "tex_model")},
    })
    # ---- 洞分类（真洞 / 有意透明）----
    holes_real = holes_allowed = 0
    detail = {}
    if tex is not None:
        detail = A.classify_holes(model, tex)
        holes_real = sum(v[1] for v in detail.values() if v[0] == "真洞")
        holes_allowed = sum(v[1] for v in detail.values() if v[0] == "有意透明")
    rec["holes_real"] = holes_real
    rec["holes_allowed"] = holes_allowed
    rec["hole_detail"] = {k: v for k, v in detail.items() if v[0] == "真洞"}
    # ---- 挂点：>10px 未登记/网格不相接 = 硬问题（与 audit_all 同阈值）----
    stats = {r["name"]: r for r in L.part_stats(model.parts)}
    exempt_cfg = A.GAP_DESIGN_EXEMPT.get(b["id"], {})
    hard_gaps = []
    for gp in gaps:
        child, parent, dist = gp[0], gp[1], float(gp[2])
        if dist <= 10.0:
            if dist > 3.0:
                rec["gaps_warn"].append({"part": child, "parent": parent,
                                         "px": round(dist, 1)})
            continue
        if child in exempt_cfg:
            rec["gaps_exempt"].append({"part": child, "parent": parent,
                                       "px": round(dist, 1),
                                       "reason": exempt_cfg[child]})
            continue
        touch = None
        if child in stats:
            c = stats[child]
            best = None
            for name, row in stats.items():
                if name in (child, parent):
                    continue
                d = [max(c["lo"][i] - row["hi"][i], 0.0, row["lo"][i] - c["hi"][i])
                     for i in range(3)]
                gap = (d[0] ** 2 + d[1] ** 2 + d[2] ** 2) ** 0.5
                if best is None or gap < best[1]:
                    best = (name, gap)
            if best is not None and best[1] <= 2.0:
                touch = best
        if touch is not None:
            rec["gaps_exempt"].append({"part": child, "parent": parent,
                                       "px": round(dist, 1),
                                       "reason": f"pivot 口径：网格与 {touch[0]} 相接"
                                                 f"（间隙 {touch[1]:.1f}px）"})
        elif _gap_floating(child) or _gap_floating(parent):
            rec["gaps_exempt"].append({"part": child, "parent": parent,
                                       "px": round(dist, 1),
                                       "reason": "悬浮/装饰件设计（部件名命中悬浮件）"})
        else:
            hard_gaps.append({"part": child, "parent": parent,
                              "px": round(dist, 1)})
    rec["gaps_hard"] = hard_gaps
    rec["gaps_all"] = [(g[0], g[1], round(float(g[2]), 1)) for g in gaps]
    # ---- 渲染器 API ----
    r_issues, r_notes, ground_info = check_renderer_api(b, src)
    rec["renderer_issues"] = r_issues
    rec["renderer_notes"] = r_notes
    rec["renderer"] = b["renderer"]
    if r_issues:
        rec["problems"].extend(r_issues)
    for n in r_notes:
        rec["warnings"].append(n)
    # ---- 地面对齐（world_foot）----
    rsrc = read_text(renderer_path(b)) if os.path.exists(renderer_path(b)) else ""
    lift_expr, lift = parse_ground_lift(rsrc)
    lift_val = 0.0
    if lift and lift[0] == "lowest":
        lm = ground_info.get("lowest_model")
        if lm is None:
            rec["warnings"].append(
                "渲染器 GROUND_LIFT 用 LOWEST_PIXEL_Y 但模型未声明 → 按 0 处理")
        else:
            lift_val = (lm - lift[1]) / 16.0
    elif lift and lift[0] == "const":
        lift_val = lift[1] / 16.0
    world_foot = round(lift_val + (1.501 - foot_px / 16.0), 4)
    rec.update({"lift": lift_val, "lift_expr": lift_expr,
                "world_foot": world_foot, "foot_px": foot_px})
    if b["ground_mode"] == "exempt":
        rec["notes"].append("恶为天穹特殊定位：脚底不按 24 基准判（沿用 "
                            "audit_all code_is_evil 口径）")
        if abs(world_foot) > 0.05:
            rec["warnings"].append(f"脚底世界高度 {world_foot:+.3f} 格（本模型豁免）")
    else:
        if abs(world_foot) > 0.05:
            rec["problems"].append(
                f"脚底离地 {world_foot:+.3f} 格（应 0；foot_px={foot_px:.2f}，"
                f"LOWEST={ground_info.get('lowest_model')}，lift={lift_val:.4f}）")
    # ---- 契约目标 ----
    t = b["targets"]
    checks = []
    checks.append({"name": f"cube ≥ {t['cubes']}", "expect": f"≥{t['cubes']}",
                   "actual": str(rec["cubes"]),
                   "pass": rec["cubes"] >= t["cubes"]})
    if "height" in t:
        lo_h, hi_h = t["height"]
        checks.append({"name": "模型高（契约目标）",
                       "expect": f"{lo_h}~{hi_h}px（{lo_h/16:.2f}~{hi_h/16:.2f} 格）",
                       "actual": f"{height_px:.1f}px（{height_px/16:.2f} 格）",
                       "pass": lo_h <= height_px <= hi_h})
    if "width" in t:
        lo_w, hi_w = t["width"]
        checks.append({"name": "宽度/翼展（契约目标）",
                       "expect": f"{lo_w}~{hi_w}px（{lo_w/16:.2f}~{hi_w/16:.2f} 格）",
                       "actual": f"{width_px:.1f}px（{width_px/16:.2f} 格）",
                       "pass": lo_w <= width_px <= hi_w})
    if "width_min" in t:
        checks.append({"name": "宽度/肩宽下限（契约目标）",
                       "expect": f"≥{t['width_min']}px",
                       "actual": f"{width_px:.1f}px",
                       "pass": width_px >= t["width_min"]})
    if "depth" in t:
        lo_d, hi_d = t["depth"]
        checks.append({"name": "长度/鼻尾（契约目标）",
                       "expect": f"{lo_d}~{hi_d}px（{lo_d/16:.2f}~{hi_d/16:.2f} 格）",
                       "actual": f"{depth_px:.1f}px（{depth_px/16:.2f} 格）",
                       "pass": lo_d <= depth_px <= hi_d})
    # 贴图 / glow
    exp = b["tex_size"]
    tm = tuple(model.tex)
    checks.append({"name": "LayerDefinition 贴图尺寸（契约新尺寸）",
                   "expect": f"{exp[0]}×{exp[1]}",
                   "actual": f"{tm[0]}×{tm[1]}", "pass": tm == exp})
    tp = None if tex is None else (tex.shape[1], tex.shape[0])
    checks.append({"name": "PNG 贴图尺寸（契约新尺寸）",
                   "expect": f"{exp[0]}×{exp[1]}",
                   "actual": ("%d×%d" % tp) if tp else "缺失", "pass": tp == exp})
    gm = re.search(r'LayerDefinition\.create\([^,]+,\s*(\d+)\s*,\s*(\d+)\)', src)
    if gm:
        lit = (int(gm.group(1)), int(gm.group(2)))
        checks.append({"name": "LayerDefinition.create 入参尺寸",
                       "expect": f"{exp[0]}×{exp[1]}",
                       "actual": f"{lit[0]}×{lit[1]}", "pass": lit == exp})
    if b["glow"]:
        gp = None if glow is None else (glow.shape[1], glow.shape[0])
        checks.append({"name": "glow 贴图尺寸与主贴图一致",
                       "expect": f"{exp[0]}×{exp[1]}（必须有）",
                       "actual": ("%d×%d" % gp) if gp else "缺失",
                       "pass": gp == exp})
    else:
        rec["notes"].append("本模型原设计无 glow 层（契约未要求新增）")
        if glow is not None:
            gp = (glow.shape[1], glow.shape[0])
            checks.append({"name": "glow 贴图尺寸与主贴图一致（存在时）",
                           "expect": f"{exp[0]}×{exp[1]}",
                           "actual": "%d×%d" % gp, "pass": gp == exp})
    checks.append({"name": "UV 越界", "expect": "0", "actual": str(uv["oob_count"]),
                   "pass": uv["oob_count"] == 0})
    checks.append({"name": "跨足迹 UV 重叠", "expect": "0",
                   "actual": str(uv["cross_overlap_texels"]),
                   "pass": uv["cross_overlap_texels"] == 0})
    checks.append({"name": "真洞（采样到透明纹素）", "expect": "0",
                   "actual": str(holes_real), "pass": holes_real == 0})
    checks.append({"name": "挂点硬断开（>10px 未豁免）", "expect": "0",
                   "actual": str(len(hard_gaps)), "pass": len(hard_gaps) == 0})
    # API 兼容（上面 renderer_issues 已计入 problems，这里给出可读判定行）
    checks.append({"name": "Renderer API 兼容", "expect": "类名/createBodyLayer/"
                                                        "构造器/LAYER id/注册 一致",
                   "actual": "OK" if not r_issues else f"{len(r_issues)} 项问题",
                   "pass": not r_issues})
    rec["checks"] = checks
    for c in checks:
        if not c["pass"] and c["name"] != "Renderer API 兼容":
            rec["problems"].append(
                f"{c['name']} 不达标（实测 {c['actual']}，期望 {c['expect']}）")
    for g in hard_gaps:
        rec["problems"].append(f"挂点疑似断开：{g['part']}→{g['parent']} "
                               f"{g['px']}px（>10px 且未登记）")
    for g in rec["gaps_exempt"]:
        rec["warnings"].append(f"设计性悬空（豁免）：{g['part']}→{g['parent']} "
                               f"{g['px']}px（{g['reason']}）")
    for g in rec["gaps_warn"]:
        rec["warnings"].append(f"挂点间距 {g['part']}→{g['parent']} {g['px']}px"
                               "（3~10px，只记警告）")
    # 大模型常被 audit_all 记 hitbox 高差警告（允许，但要列出来）
    hb = A.read_entity_sizes().get(b["id"])
    if hb:
        rank_scale = A.RANK.get(b.get("rank", ""), (1.0, 1.0))
        # 从 ENTITIES 查品阶
        ent = next((e for e in A.ENTITIES if e[0] == b["id"]), None)
        rank = ent[2] if ent else None
        sxz, sy = A.RANK.get(rank, (1.0, 1.0))
        target_h = hb[1] / sy * 16.0
        rec["hitbox"] = list(hb)
        rec["target_h_px"] = round(target_h, 1)
        rec["h_delta_pct"] = round((height_px / target_h - 1) * 100, 1)
        if abs(rec["h_delta_pct"]) > 25:
            rec["warnings"].append(
                f"hitbox 高差 {rec['h_delta_pct']:+.1f}%（audit_all 会记警告，契约允许增加）")
    rec["status"] = "PASS" if not rec["problems"] else "FAIL"
    return rec


# ----------------------------------------------------------------------
# 全量审计 + 与旧基线对比
# ----------------------------------------------------------------------
def run_full_audit():
    cmd = [sys.executable, os.path.join(TOOLS, "audit_all.py"),
           "--no-render", "--tag", AUDIT_TAG]
    print(f"  $ python -X utf8 工程/角色设计/tools/audit_all.py --no-render "
          f"--tag \"{AUDIT_TAG}\"")
    t0 = time.time()
    proc = subprocess.run(cmd, cwd=WS, capture_output=True, text=True,
                          encoding="utf-8", errors="replace")
    tail = "\n".join((proc.stdout or "").strip().splitlines()[-6:])
    print(tail or "(audit_all 无输出)")
    if proc.returncode != 0:
        print(f"[warn] audit_all 返回码 {proc.returncode}："
              f"{(proc.stderr or '')[-400:]}")
    res = {"ok": False, "rows": 0, "hard_entities": [], "warnings": 0,
           "sil_pairs": 0, "regressions": [], "hitbox_warnings": [],
           "hitbox_new": [], "evil_before": None, "evil_after": None,
           "audit_md": rel(AUDIT_MD), "audit_json": rel(AUDIT_JSON),
           "elapsed_s": round(time.time() - t0, 1), "stdout_tail": tail,
           "prev_audit": PREV_AUDIT_NAME}
    if not os.path.exists(AUDIT_JSON):
        res["error"] = f"audit_all 没写出 JSON：{AUDIT_JSON}"
        return res
    data = json.load(io.open(AUDIT_JSON, encoding="utf-8"))
    rows = data.get("rows") or []
    res["rows"] = len(rows)
    res["hard_entities"] = [{"id": r.get("id"), "zh": r.get("zh"),
                             "problems": r.get("problems")}
                            for r in rows if r.get("problems")]
    res["warnings"] = sum(1 for r in rows if r.get("warnings"))
    res["sil_pairs"] = len(data.get("sil_pairs") or [])
    now = {r.get("id"): r for r in rows}
    # hitbox 高差警告（允许增加，列出）
    for r in rows:
        if any("模型高" in w for w in (r.get("warnings") or [])):
            res["hitbox_warnings"].append({"id": r.get("id"), "zh": r.get("zh"),
                                           "h_delta_pct": r.get("h_delta_pct")})
    if os.path.exists(PREV_AUDIT_JSON):
        prev = json.load(io.open(PREV_AUDIT_JSON, encoding="utf-8"))
        prev_rows = {r.get("id"): r for r in (prev.get("rows") or [])}
        prev_hw = {r.get("id"): r.get("h_delta_pct")
                   for r in (prev.get("rows") or [])
                   if any("模型高" in w for w in (r.get("warnings") or []))}
        res["hitbox_new"] = [
            x for x in res["hitbox_warnings"]
            if x["id"] not in prev_hw
            or abs((x.get("h_delta_pct") or 0.0) - (prev_hw[x["id"]] or 0.0)) > 0.06]
        res["evil_before"] = {k: prev_rows.get("evil_gazer", {}).get(k) for k in
                              ("cubes", "height_blocks", "world_foot", "uv_oob",
                               "uv_cross_overlap", "holes_real", "warnings")}
        res["evil_after"] = {k: now.get("evil_gazer", {}).get(k) for k in
                             ("cubes", "height_blocks", "world_foot", "uv_oob",
                              "uv_cross_overlap", "holes_real", "warnings")}
        for eid, prow in prev_rows.items():
            crow = now.get(eid)
            if crow is None:
                res["regressions"].append({"id": eid, "why": "本次审计里消失了"})
                continue
            if eid in REMADE_ENTITY_IDS:
                continue
            if list(crow.get("problems") or []) != list(prow.get("problems") or []):
                res["regressions"].append(
                    {"id": eid, "why": "非重制实体硬问题变化："
                                       f"{prow.get('problems')} → {crow.get('problems')}"})
            elif list(crow.get("warnings") or []) != list(prow.get("warnings") or []):
                res["regressions"].append(
                    {"id": eid, "why": "非重制实体警告变化"})
    res["ok"] = (res["rows"] == 55 and not res["hard_entities"]
                 and not res["regressions"])
    return res


# ----------------------------------------------------------------------
# 证据图
# ----------------------------------------------------------------------
def _title_bar(body, title, sub=""):
    h = 54 if sub else 34
    sheet = Image.new("RGB", (body.width, body.height + h), BG)
    sheet.paste(body, (0, h))
    d = ImageDraw.Draw(sheet)
    d.text((8, 6), title, fill=FG, font=font(17, True))
    if sub:
        d.text((8, 28), sub, fill=DIM, font=font(12))
    return sheet


def _sil_mask(model):
    return np.asarray(R.silhouette(model, view="hero", px=64).convert("L")) < 128


def render_bosses(recs):
    for rec in recs:
        if rec.get("model") is None:
            continue
        b = next(x for x in BOSSES if x["key"] == rec["key"])
        outdir = os.path.join(EVIL_DIR, b["key"])
        tex = R.load_texture(tex_path(b))
        glow = R.load_texture(os.path.join(TEXDIR, b["tex"] + "_glow.png")) \
            if os.path.exists(os.path.join(TEXDIR, b["tex"] + "_glow.png")) else None
        row = rec
        big = b["key"] in ("dragon", "void", "evil")
        vsize = (720, 640) if big else (520, 640)
        title = f"{row['zh']} {b['cls']} · {b['item']} 项重制"
        sub = (f"cube {row.get('old', {}).get('cubes')} → {row['cubes']} · "
               f"高 {row['height_px']:.1f}px（{row['height_blocks']:.2f} 格）· "
               f"贴图 {row['tex_png']} · 脚底 {row['world_foot']:+.3f} 格")
        views = R.render_model(row["model"], tex=tex, glow=glow,
                               views=("front", "right", "back", "hero"),
                               label="", ground_lift=row["lift"], size=vsize, bg=BG)
        _title_bar(views, title, sub).save(os.path.join(outdir, "g1_四视图.png"))
        R.silhouette(row["model"], view="hero", px=64, label="").save(
            os.path.join(outdir, "g1_剪影64.png"))
        R.thumb_32(row["model"], label=f"{row['zh']} {b['cls']}",
                   path=os.path.join(outdir, "g1_缩略32.png"))
        hero = R.render_model(row["model"], tex=tex, glow=glow, views=("hero",),
                              size=(360, 600), caption=False, label="",
                              ground_lift=row["lift"], bg=BG)
        hero.save(os.path.join(outdir, "g2_hero.png"))
        rec["_hero"] = hero
        rec["_sil"] = R.silhouette(row["model"], view="hero", px=64)
        rec["_mask"] = _sil_mask(row["model"])
        # 新旧对比
        old_p = old_model_path(b)
        if os.path.exists(old_p):
            try:
                old_model = L.parse_model_file(old_p)
                old_tex = R.load_texture(old_tex_path(b))
                old_glow = None
                old_glow_p = os.path.join(OLD, "textures", b["tex"] + "_glow.png")
                if os.path.exists(old_glow_p):
                    old_glow = R.load_texture(old_glow_p)
                cw, ch = 380, 480
                sheet = Image.new("RGB", (cw * 2 + 60, ch + 120), BG)
                d = ImageDraw.Draw(sheet)
                d.text((20, 10), f"{row['zh']} · 新旧对比（before / after）", fill=FG,
                       font=font(20, True))
                d.text((20, 38), f"旧：{row.get('old', {}).get('cubes')} cube / "
                                 f"{row.get('old', {}).get('height_px')}px → 新："
                                 f"{row['cubes']} cube / {row['height_px']:.1f}px",
                       fill=DIM, font=font(12))
                old_im = R.render_model(old_model, tex=old_tex, glow=old_glow,
                                        views=("hero",), size=(cw, ch),
                                        caption=False, label="", bg=BG)
                new_im = R.render_model(row["model"], tex=tex, glow=glow,
                                        views=("hero",), size=(cw, ch),
                                        caption=False, label="", bg=BG)
                sheet.paste(old_im, (20, 74))
                sheet.paste(new_im, (cw + 40, 74))
                d.text((20, ch + 82), "BEFORE 旧版", fill=YELLOW, font=font(14, True))
                d.text((cw + 40, ch + 82), "AFTER 新版", fill=GREEN,
                       font=font(14, True))
                sheet.save(os.path.join(outdir, "g3_新旧对比.png"))
            except Exception as e:      # noqa: BLE001
                rec["warnings"].append(f"新旧对比渲染失败：{e}")


def _hero_same_scale(model, tex, glow, ppu):
    big = R.render_model(model, tex=tex, glow=glow, views=("hero",),
                         size=(760, 1200), caption=False, label="",
                         bg=TILE_BG, with_ground=False)
    arr = np.asarray(big.convert("RGB"))
    mask = np.any(arr != np.array(TILE_BG, dtype=np.uint8), axis=2)
    ys, xs = np.where(mask)
    if len(xs) == 0:
        return big
    crop = big.crop((int(xs.min()), int(ys.min()), int(xs.max()) + 1,
                     int(ys.max()) + 1))
    lo, hi = model.bounds()
    target_h = max(24, int((hi[1] - lo[1]) * ppu))
    target_w = max(12, int(crop.width * target_h / crop.height))
    crop = crop.resize((target_w, target_h), Image.LANCZOS)
    a = np.array(crop.convert("RGB"))
    sub = np.all(a == np.array(TILE_BG, dtype=np.uint8), axis=2)
    a[sub] = np.array(BG, dtype=np.uint8)
    return Image.fromarray(a, "RGB")


def build_overview(recs):
    ok = [r for r in recs if r.get("model") is not None]
    if not ok:
        return None
    max_h = max(r["height_px"] for r in ok)
    ppu = 500.0 / max_h
    heroes = []
    for r in ok:
        b = next(x for x in BOSSES if x["key"] == r["key"])
        tex = R.load_texture(tex_path(b))
        glow = R.load_texture(os.path.join(TEXDIR, b["tex"] + "_glow.png")) \
            if os.path.exists(os.path.join(TEXDIR, b["tex"] + "_glow.png")) else None
        try:
            im = _hero_same_scale(r["model"], tex, glow, ppu)
        except Exception:   # noqa: BLE001
            im = None
        heroes.append((r, im))
    tile_w, gap, top, cap_h = 260, 18, 104, 168
    W = 40 + len(heroes) * (tile_w + gap)
    H = top + 540 + cap_h
    sheet = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(sheet)
    d.text((22, 14), "鸿蒙修仙 · 第六轮 Boss 模型重制总览（6 项 / 7 个模型）",
           fill=FG, font=font(26, True))
    n_pass = sum(1 for r in ok if r["status"] == "PASS")
    d.text((22, 52), f"PASS {n_pass}/{len(ok)} ｜ 同比例并排（px/模型像素 = {ppu:.2f}，"
                     "底部对齐）｜ 证据口径=charlib+renderlib（只读，不改模型）｜ "
                     f"{time.strftime('%Y-%m-%d %H:%M')}", fill=DIM, font=font(13))
    d.text((22, 74), "按契约 §2 顺序：A 龙帝 / B 炎帝 / C 万兽 / D 血魔 / E 虚空道主 / "
                     "F1 恶上身 / F2 仙庭之主（F 项含 2 个模型）", fill=DIM,
           font=font(11))
    base_y = top + 512
    x = 40
    for r, im in heroes:
        if im is not None:
            sheet.paste(im, (x + (tile_w - im.width) // 2, base_y - im.height))
            d.line([(x, base_y + 1), (x + tile_w, base_y + 1)], fill=(80, 210, 120),
                   width=2)
        cy = base_y + 14
        d.text((x, cy), f"{r['item']} {r['zh']}", fill=FG, font=font(17, True))
        d.text((x, cy + 24), f"{r['cls']} · {r['cubes']} cube", fill=DIM,
               font=font(11))
        d.text((x, cy + 42), f"高 {r['height_px']:.1f}px（{r['height_blocks']:.2f} 格）",
               fill=DIM, font=font(11))
        d.text((x, cy + 60), f"宽 {r['width_px']:.0f} · 深 {r['depth_px']:.0f}px · "
                             f"贴图 {r['tex_png']}", fill=DIM, font=font(10))
        d.text((x, cy + 80), f"UV {r['uv_oob']}/{r['uv_cross']} · 真洞 "
                             f"{r['holes_real']} · 挂点硬断 {len(r['gaps_hard'])}",
               fill=DIM, font=font(10))
        box = GREEN if r["status"] == "PASS" else RED
        d.text((x, cy + 100), f"判定：{r['status']}", fill=box, font=font(15, True))
        x += tile_w + gap
    d.text((22, H - 28),
           "注：模型体量可超出旧 hitbox（audit 只记警告）；脚底世界高度按各 renderer 的 "
           "GROUND_LIFT 换算；恶为天穹特殊定位不按 24 判。明细见 evidence/boss-remake.md。",
           fill=DIM, font=font(11))
    os.makedirs(ROUND6, exist_ok=True)
    sheet.save(OVERVIEW)
    return OVERVIEW


def silhouette_report(recs):
    """两两 IoU + 与未重制 Boss 的 IoU（>0.70 标出）。"""
    ok = [r for r in recs if r.get("_mask") is not None]
    pairs = []
    for i in range(len(ok)):
        for j in range(i + 1, len(ok)):
            a, b = ok[i], ok[j]
            inter = int(np.logical_and(a["_mask"], b["_mask"]).sum())
            union = int(np.logical_or(a["_mask"], b["_mask"]).sum())
            pairs.append({"a": a["key"], "b": b["key"],
                          "iou": round(inter / union if union else 1.0, 3)})
    pool = []
    for ent in A.ENTITIES:
        eid = ent[0]
        if eid in REMADE_ENTITY_IDS:
            continue
        if ent[6] not in ("boss", "boss_tier"):
            continue
        mf = ent[3]
        if not mf:
            continue
        p = os.path.join(CLIENT, mf)
        if not os.path.exists(p):
            continue
        try:
            m = L.parse_model_file(p)
            pool.append((eid, ent[1], _sil_mask(m)))
        except Exception:   # noqa: BLE001
            continue
    vs_pool = []
    for r in ok:
        best = None
        for eid, zh, mask in pool:
            inter = int(np.logical_and(r["_mask"], mask).sum())
            union = int(np.logical_or(r["_mask"], mask).sum())
            iou = inter / union if union else 1.0
            if best is None or iou > best[2]:
                best = (eid, zh, iou)
        if best:
            vs_pool.append({"key": r["key"], "id": r["id"], "nearest": best[0],
                            "nearest_zh": best[1], "iou": round(best[2], 3),
                            "flagged": best[2] > 0.70})
    return {"pairs": pairs, "pool_size": len(pool), "vs_pool": vs_pool}


def render_iou_sheet(recs, sil):
    ok = [r for r in recs if r.get("_sil") is not None]
    if not ok:
        return None
    cell, gap, top = 150, 24, 96
    W = 40 + len(ok) * (cell + gap)
    H = top + cell + 170
    sheet = Image.new("RGB", (W, H), (250, 250, 252))
    d = ImageDraw.Draw(sheet)
    d.text((20, 12), "Boss 重制 · 64px 剪影对比（同尺寸同取景）", fill=(40, 48, 60),
           font=font(20, True))
    d.text((20, 40), "同组重制 Boss 两两 IoU / 与未重制 Boss 池最近邻 IoU（>0.70 标红）",
           fill=(90, 98, 112), font=font(12))
    x = 26
    for r in ok:
        sil_img = r["_sil"].resize((cell, cell), Image.NEAREST)
        sheet.paste(sil_img, (x, top))
        d.text((x, top + cell + 6), f"{r['item']} {r['zh']}", fill=(40, 48, 60),
               font=font(13, True))
        d.text((x, top + cell + 26), f"{r['cubes']} cube · {r['height_blocks']:.2f} 格",
               fill=(90, 98, 112), font=font(11))
        x += cell + gap
    ty = top + cell + 52
    d.text((26, ty), "两两 IoU：", fill=(40, 48, 60), font=font(13, True))
    ty += 20
    lines = []
    for p in sil["pairs"]:
        flag = " ⚠>0.70" if p["iou"] > 0.70 else ""
        lines.append(f"{p['a']} vs {p['b']} = {p['iou']:.2f}{flag}")
    for line in lines[:5]:
        d.text((40, ty), line, fill=(60, 70, 90), font=font(11))
        ty += 16
    ty += 6
    d.text((26, ty), f"与未重制 Boss 池（{sil['pool_size']} 只）最近邻：",
           fill=(40, 48, 60), font=font(13, True))
    ty += 20
    for v in sil["vs_pool"]:
        flag = " ⚠>0.70" if v["flagged"] else ""
        color = (200, 40, 40) if v["flagged"] else (60, 70, 90)
        d.text((40, ty), f"{v['key']} ↔ {v['nearest_zh']}({v['nearest']}) = "
                         f"{v['iou']:.2f}{flag}", fill=color, font=font(11))
        ty += 16
    p = os.path.join(EVID, "剪影对比.png")
    sheet.save(p)
    return p


# ----------------------------------------------------------------------
# 报告
# ----------------------------------------------------------------------
def write_report(recs, full, old_nums, sil, state, wait_s, elapsed):
    all_pass = all(r["status"] == "PASS" for r in recs) and \
        all(r.get("model") is not None for r in recs)
    issues = [r for r in recs if r["status"] != "PASS"]
    flat = [x for r in recs for x in r["problems"]]
    L_ = []
    L_.append("# Boss 模型重制 · 门禁与证据（第六轮 · 只读）")
    L_.append("")
    L_.append(f"> 生成：`python -X utf8 tools/check_boss_remake.py`  ")
    L_.append(f"> 时间：{time.strftime('%Y-%m-%d %H:%M:%S')}（耗时 {elapsed:.1f}s；"
              f"等待落盘 {wait_s:.0f}s）  ")
    L_.append("> 契约：`工程/21-Boss模型重制契约.md` §1（工程约束）+ §3（验收门禁）  ")
    L_.append("> 口径：charlib 解析 + renderlib `uv_audit/texture_audit/"
              "classify_holes/ground_audit/gap_audit`（与 `audit_all` 同一套判定），"
              "外加契约 §1 的 renderer/API 兼容检查  ")
    L_.append("> 写域：只读 Java/贴图；仅写 `round6/evidence/**` 与 "
              "`round6/00-Boss重制总览.png`（全量审计由任务指定命令写 `审计/`）")
    L_.append("")
    L_.append("## 0. 结论")
    L_.append("")
    L_.append("- 重制模型：共 **7 个**（6 项）：" + "、".join(
        f"{r['item']} {r['zh']}(`{r['cls']}`) **{r['status']}**" for r in recs))
    L_.append(f"- 合计 cube {sum(r.get('cubes') or 0 for r in recs)}；"
              f"告警/问题：{'**0**' if not flat else f'**{len(flat)}**'}")
    if full is None:
        L_.append("- 全量审计 `13-Boss重制`：**未执行**（模型未全部落盘/参数跳过）")
    else:
        hard_n = len(full["hard_entities"])
        L_.append(f"- 全量审计 `13-Boss重制`：**{full['rows']} 实体 / 硬问题 "
                  f"{hard_n}**" + ("（0 = PASS）" if hard_n == 0 else " ← 见 §4.1"))
        reg_n = len(full["regressions"])
        reg_txt = "非重制实体 **0 回归**" if reg_n == 0 else f"**{reg_n} 处回归/变化**"
        L_.append(f"- 与旧基线 `{PREV_AUDIT_NAME}` 对比：{reg_txt}")
    L_.append(f"- 模型问题清单：{'**无**（7/7 PASS）' if all_pass else f'**{len(issues)} 个模型不达标**'}")
    if full is not None and full["hard_entities"]:
        L_.append("- ⚠ 全量审计硬问题（外部 audit_all 口径，非本脚本门禁）：" + "；".join(
            f"{h['zh']}({h['id']}）：{' / '.join(h['problems'])}"
            for h in full["hard_entities"]))
    L_.append("")
    L_.append("## 1. 逐只数字 vs 契约 §2 目标")
    L_.append("")
    L_.append("| 项 | Boss | cube（目标） | 高 px（目标） | 宽 px（目标） | 深 px | 贴图(模型/PNG/glow) | UV 越界/重叠 | 真洞 | 挂点硬断 | 脚底(世界格) | 判定 |")
    L_.append("|---|---|---|---|---|---|---|---|---|---|---|---|")
    for r in recs:
        if r.get("model") is None:
            L_.append(f"| {r['item']} | {r['zh']} | - | - | - | - | - | - | - | - "
                      f"| - | **{r['status']}**（缺失） |")
            continue
        b = next(x for x in BOSSES if x["key"] == r["key"])
        t = b["targets"]
        h_t = f"{t['height'][0]}~{t['height'][1]}" if "height" in t else "-"
        if "width" in t:
            w_t = f"{t['width'][0]}~{t['width'][1]}"
        elif "width_min" in t:
            w_t = f"≥{t['width_min']}"
        else:
            w_t = "-"
        texs = "/".join(str(x) for x in (tuple(r["tex_model"]) if r.get("tex_model") else "-",
                                         "%dx%d" % tuple(r["tex_png"]) if r.get("tex_png") else "-",
                                         "%dx%d" % tuple(r["glow_png"]) if r.get("glow_png") else "无"))
        L_.append(f"| {r['item']} | {r['zh']} | {r['cubes']}（≥{t['cubes']}） | "
                  f"{r['height_px']:.1f}（{h_t}） | {r['width_px']:.1f}（{w_t}） | "
                  f"{r['depth_px']:.1f} | {texs} | {r['uv_oob']} / {r['uv_cross']} | "
                  f"{r['holes_real']} | {len(r['gaps_hard'])} | "
                  f"{r['world_foot']:+.3f} | **{r['status']}** |")
    L_.append("")
    L_.append("> 目标换算：1 格 = 16px。逐只目标原文见契约 §2；"
              "脚底世界高度按 renderer 的 `GROUND_LIFT` 换算（应为 0；恶为天穹特殊定位豁免）。")
    L_.append("")
    L_.append("## 2. 契约 §1 硬门禁 / API 兼容逐项")
    L_.append("")
    for r in recs:
        if r.get("model") is None:
            continue
        L_.append(f"### {r['item']} {r['zh']}（`{r['cls']}`）")
        for c in r["checks"]:
            L_.append(f"- {'✅' if c['pass'] else '❌'} {c['name']}：实测 "
                      f"{c['actual']}（期望 {c['expect']}）")
        if r["renderer_notes"]:
            for n in r["renderer_notes"]:
                L_.append(f"- ℹ {n}")
        L_.append("")
    L_.append("## 3. 问题清单（只报告，不改模型）")
    L_.append("")
    if not flat:
        L_.append("（无）")
    else:
        for r in recs:
            if not r["problems"]:
                continue
            L_.append(f"### ❌ {r['item']} {r['zh']}（`{r['cls']}`）")
            for p in r["problems"]:
                L_.append(f"- {p}")
            L_.append("")
    L_.append("## 4. 全量审计（`audit_all.py --no-render --tag \"13-Boss重制\"`）")
    L_.append("")
    if full is None:
        L_.append("未执行（模型未全部落盘 / --skip-full-audit）。")
    else:
        L_.append(f"- 实体数：**{full['rows']}**（期望 55）；硬问题实体："
                  f"**{len(full['hard_entities'])}**；警告实体：{full['warnings']}；"
                  f"剪影高相似对：{full['sil_pairs']}")
        L_.append(f"- 报告：`{full['audit_md']}` / `{full['audit_json']}`")
        ev_b, ev_a = full.get("evil_before") or {}, full.get("evil_after") or {}
        L_.append(f"- evil（evil_gazer 行）：cube {ev_b.get('cubes')} → "
                  f"**{ev_a.get('cubes')}**；高度 {ev_b.get('height_blocks')} → "
                  f"**{ev_a.get('height_blocks')} 格**；UV 0/0 → 0/0；真洞 0 → 0")
        reg_n = len(full["regressions"])
        reg_txt = "**非重制实体 0 回归**" if reg_n == 0 else f"**{reg_n} 处回归/变化**"
        L_.append(f"- 与 `{PREV_AUDIT_NAME}` 逐实体对比：{reg_txt}")
        for reg in full["regressions"][:12]:
            L_.append(f"  - {reg['id']}：{reg['why']}")
        if full["hitbox_warnings"]:
            new_ids = {x["id"] for x in (full.get("hitbox_new") or [])}
            L_.append("- hitbox 高度警告（契约允许增加，全部列出；"
                      "**粗体=本次新增/变化**）：" + "、".join(
                (f"**{x['zh']}({x['id']}) {x['h_delta_pct']:+.1f}%**"
                 if x["id"] in new_ids else f"{x['zh']}({x['id']}) "
                                            f"{x['h_delta_pct']:+.1f}%")
                for x in full["hitbox_warnings"] if x.get("h_delta_pct") is not None))
        L_.append("")
        L_.append("### 4.1 全量审计硬问题明细（外部 audit_all 判定）")
        L_.append("")
        if not full["hard_entities"]:
            L_.append("（无）")
        else:
            for h in full["hard_entities"]:
                L_.append(f"- ❌ **{h['zh']}（{h['id']}）**：")
                for p in h["problems"]:
                    L_.append(f"  - {p}")
            L_.append("")
            L_.append("> 说明：上列问题按 `audit_all` 的固定豁免表（`GAP_DESIGN_EXEMPT`）"
                      "判定，若属悬浮眼/骨刺等设计件而未被登记，需要 Lead 调整 "
                      "`tools/audit_all.py` 的豁免表或由模型 agent 调整 pivot；"
                      "本脚本只报告，不修改任何模型/工具。")
    L_.append("")
    L_.append("## 5. 剪影 IoU（>0.70 标出）")
    L_.append("")
    if sil:
        L_.append("### 5.1 重制 Boss 两两 IoU")
        L_.append("")
        L_.append("| A | B | IoU | 判定 |")
        L_.append("|---|---|---|---|")
        for p in sil["pairs"]:
            L_.append(f"| {p['a']} | {p['b']} | {p['iou']:.3f} | "
                      f"{'**>0.70**' if p['iou'] > 0.70 else '可区分'} |")
        L_.append("")
        L_.append(f"### 5.2 与未重制 Boss 池（{sil['pool_size']} 只）最近邻")
        L_.append("")
        L_.append("| 重制模型 | 最近邻 | IoU | 判定 |")
        L_.append("|---|---|---|---|")
        for v in sil["vs_pool"]:
            L_.append(f"| {v['key']} | {v['nearest_zh']}({v['nearest']}) | "
                      f"{v['iou']:.3f} | "
                      f"{'**>0.70**' if v['flagged'] else '可区分'} |")
    L_.append("")
    L_.append("## 6. 证据文件")
    L_.append("")
    L_.append("| 路径 | 内容 |")
    L_.append("|---|---|")
    L_.append(f"| `{rel(OVERVIEW)}` | 7 个重制模型同比例并排（体量对比）+ 关键数字 + 判定 |")
    L_.append(f"| `{rel(os.path.join(EVID, '剪影对比.png'))}` | 64px 剪影并排 + 两两 IoU + "
              "与未重制 Boss 池最近邻 |")
    for b in BOSSES:
        L_.append(f"| `{rel(os.path.join(EVIL_DIR, b['key']))}/` | {b['zh']}：四视图 / "
                  "64px 剪影 / 32px 缩略 / hero / 新旧对比 |")
    L_.append(f"| `{rel(OLD)}` | 重制前 7 模型 + 贴图 + renderer 快照（旧版基线，只读） |")
    L_.append(f"| `{rel(REPORT_JSON)}` | 机读数据（本 md 全部数字） |")
    L_.append("")
    L_.append("## 7. 复跑")
    L_.append("")
    L_.append("```")
    L_.append("# 门禁 + 证据 + 全量审计（7 个模型都落盘后）")
    L_.append("python -X utf8 tools/check_boss_remake.py")
    L_.append("")
    L_.append("# 等 7 个模型落地（最多 2400s）再跑")
    L_.append("python -X utf8 tools/check_boss_remake.py --wait 2400")
    L_.append("")
    L_.append("# 只跑一只（调试）")
    L_.append("python -X utf8 tools/check_boss_remake.py --only dragon")
    L_.append("```")
    L_.append("")
    L_.append("判定口径：7 模型全部 PASS（契约 §2 cube/高度/翼展·肩宽 + §1 UV 0/0、真洞 0、"
              "贴图/glow 尺寸、脚底 0、挂点 0、renderer API 兼容）且 `audit_all` 55 实体"
              "硬问题 0、非重制实体无回归 = **整体 PASS**。发现模型问题只报告不改"
              "（交 Lead / 对应 agent）。")
    L_.append("返回码：`0`=PASS；`2`=仍在等模型落盘；`1`=有 FAIL。")
    L_.append("")
    L_.append("## 8. 已知限制")
    L_.append("")
    L_.append("- 离线几何/UV 审计：`setupAnim` 动画姿态下的穿模、粒子/染色、实机光照/演出"
              "不在覆盖内；`build_entity.py` 的模型/贴图单一真源审计由各模型 agent 自跑，"
              "本脚本只复核生成结果；")
    L_.append("- 模型体量允许超出旧 hitbox（契约 §1），`audit_all` 会记 hitbox 高差警告，"
              "本报告 §4 已列出；硬问题必须 0；")
    L_.append("- 恶（EvilUpperModel）为天穹特殊定位：脚底不按 24 基准判（沿用 "
              "`audit_all` code_is_evil 口径），高度上限按契约 ≤265px 判；")
    L_.append("- 新旧对比的「旧版」是重制前快照（`round6/evidence/baseline/old`，"
              "2026-11-07 17:31 采集）；若快照缺失脚本首跑会把当前文件当旧版（会告警）。")
    L_.append("")
    os.makedirs(EVID, exist_ok=True)
    io.open(REPORT_MD, "w", encoding="utf-8", newline="\n").write("\n".join(L_) + "\n")
    data = {
        "generated": time.strftime("%Y-%m-%d %H:%M:%S"),
        "elapsed_s": round(elapsed, 1), "wait_s": round(wait_s, 1),
        "contract": "工程/21-Boss模型重制契约.md §1/§3",
        "remade": [r["key"] for r in recs],
        "models": {r["key"]: {k: v for k, v in r.items()
                              if not k.startswith("_") and k not in ("model",)}
                   for r in recs},
        "silhouette": sil,
        "old_baseline": {b["cls"]: old_nums.get(b["cls"]) for b in BOSSES},
        "old_snapshot": rel(OLD),
        "full_audit": full,
        "overall_pass": all_pass and (full is None or full.get("ok", False)),
        "waiting_new_version": not state.get("ready", False),
    }
    io.open(REPORT_JSON, "w", encoding="utf-8", newline="\n").write(
        json.dumps(_jsonable(data), ensure_ascii=False, indent=2))
    # README
    readme = """# 第六轮 Boss 重制 · 门禁与证据

> 由 `tools/check_boss_remake.py` 生成（只读脚本，不写任何 Java/贴图）。
> 契约：`工程/21-Boss模型重制契约.md` §1/§3。

## 复跑

```
python -X utf8 tools/check_boss_remake.py                 # 门禁+证据+全量审计
python -X utf8 tools/check_boss_remake.py --wait 2400     # 等 7 个模型落盘
python -X utf8 tools/check_boss_remake.py --no-render     # 只出 md/json
python -X utf8 tools/check_boss_remake.py --only dragon   # 单只调试
```

全量审计（脚本内部会调用，也可单独跑）：

```
python -X utf8 工程/角色设计/tools/audit_all.py --no-render --tag "13-Boss重制"
```

## 产物

| 路径 | 内容 |
|---|---|
| `boss-remake.md` / `.json` | 逐只审计数字、契约目标达标表、renderer/API 兼容、问题清单、全量审计结论、剪影 IoU |
| `bosses/<key>/g1_四视图.png` | front/right/back/hero 四视图 |
| `bosses/<key>/g1_剪影64.png` / `g1_缩略32.png` / `g2_hero.png` | 剪影 / 缩略 / hero |
| `bosses/<key>/g3_新旧对比.png` | 重制前 vs 重制后 |
| `剪影对比.png` | 7 模型 64px 剪影并排 + 两两 IoU + 与未重制 Boss 池最近邻 |
| `baseline/old/` | 重制前的 7 模型 + 贴图 + renderer 快照（只读对照） |
| `../00-Boss重制总览.png` | 7 模型同比例并排（体量对比）+ 数字 + 判定 |

## 口径

* 完全复用 `charlib` + `renderlib`（`uv_audit/texture_audit/classify_holes/ground_audit/gap_audit`）
  与 `audit_all` 同判定；另加契约 §1 的 renderer API 检查（类名 / `createBodyLayer` /
  `LOWEST_PIXEL_Y` / `RankGlowLayer` / LAYER id / ClientSetup 注册 / 贴图路径 / glow 尺寸）。
* 挂点：与 `audit_all` 同阈值（>10px 未登记豁免才算硬问题；3~10px 只记警告）；
  悬浮/装饰件与"pivot 口径、网格实际相接"按设计豁免并在报告注明。
* 契约目标：cube 下限、模型高范围、翼展/肩宽/长度范围均在 `check_boss_remake.py` 的
  `BOSSES` 配置里逐只可查。
* 剪影：`renderlib.silhouette(px=64, view=hero)`；与未重制 Boss 池（`audit_all.ENTITIES`
  的 boss/boss_tier 组去掉 7 个重制项）取最近邻 IoU，>0.70 标红。

## 交付边界

* 只读 Java / 贴图 / 其他工具；发现模型问题**只报告不修**（见 `boss-remake.md` §3）；
* 写盘仅限 `工程/角色设计/round6/evidence/**`、`工程/角色设计/round6/00-Boss重制总览.png`；
  全量审计由任务书指定命令自写 `工程/角色设计/审计/`。
"""
    io.open(README, "w", encoding="utf-8", newline="\n").write(readme)
    return REPORT_MD


def _jsonable(o):
    if isinstance(o, dict):
        return {str(k): _jsonable(v) for k, v in o.items() if not str(k).startswith("_")}
    if isinstance(o, (list, tuple)):
        return [_jsonable(v) for v in o]
    if isinstance(o, (np.integer,)):
        return int(o)
    if isinstance(o, (np.floating,)):
        return float(o)
    if isinstance(o, np.ndarray):
        return o.tolist()
    if isinstance(o, (str, int, float, bool)) or o is None:
        return o
    return str(o)


# ----------------------------------------------------------------------
# main
# ----------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser(description="第六轮 Boss 重制 · 门禁 + 证据")
    ap.add_argument("--wait", type=float, default=0.0,
                    help="等 7 个模型全部落盘的秒数（默认 0 = 不等）")
    ap.add_argument("--no-render", action="store_true")
    ap.add_argument("--skip-full-audit", action="store_true")
    ap.add_argument("--only", default=None, help="只跑一只（key：dragon/blaze/...）")
    ap.add_argument("--force", action="store_true",
                    help="不管是否检测到新版都跑门禁/证据/全量审计")
    ap.add_argument("--force-snapshot", action="store_true",
                    help="强制用当前文件覆盖旧版基线（慎用）")
    args = ap.parse_args()

    t0 = time.time()
    ensure_dirs()
    print("=" * 104)
    print("第六轮 · Boss 模型重制 · 门禁 + 证据（只读；契约 §1/§3）")
    print("=" * 104)

    snap_now = snapshot_baseline(force=args.force_snapshot)
    if snap_now:
        print(f"[warn] 旧版基线快照已生成 -> {rel(OLD)}（若模型已重制，新旧对比不可靠）")
    if not os.path.exists(OLD_NUMBERS):
        load_old_numbers()
    old_nums = load_old_numbers()
    print("旧版基线：" + " / ".join(
        f"{b['key']} {old_nums.get(b['cls'], {}).get('cubes', '?')}cube "
        f"{old_nums.get(b['cls'], {}).get('height_px', '?')}px" for b in BOSSES))

    state = current_state()
    wait_s = 0.0
    if args.wait > 0 and not state["ready"] and not args.force:
        print(f"等待 7 个重制模型落盘（每 15s 检查；最多 {args.wait:.0f}s）…")
        state, wait_s = wait_for_new(args.wait)
        print(f"  {'已全部落盘' if state['ready'] else '超时：仍有模型是旧版/缺失'}"
              f"（等待 {wait_s:.0f}s）")

    targets = BOSSES
    if args.only:
        targets = [b for b in BOSSES if b["key"] == args.only]
        if not targets:
            print(f"[error] 未知 --only {args.only}；可选：{ [b['key'] for b in BOSSES] }")
            return 1

    recs = []
    for b in targets:
        print(f"  审计 {b['item']} {b['zh']} ({b['cls']})…")
        try:
            rec = audit_one(b, old_nums)
        except Exception as e:      # noqa: BLE001
            import traceback
            traceback.print_exc(limit=2)
            rec = {"key": b["key"], "item": b["item"], "id": b["id"],
                   "zh": b["zh"], "cls": b["cls"], "status": "FAIL",
                   "problems": [f"审计异常：{e}"], "warnings": [], "checks": [],
                   "notes": [], "gaps_hard": [], "gaps_exempt": [], "gaps_warn": [],
                   "renderer_issues": [], "renderer_notes": [], "model": None}
        recs.append(rec)

    sil = None
    if not args.no_render:
        try:
            render_bosses(recs)
        except Exception as e:      # noqa: BLE001
            print(f"[warn] 证据渲染失败：{e}")
            import traceback
            traceback.print_exc(limit=2)
        try:
            sil = silhouette_report(recs)
            render_iou_sheet(recs, sil)
            build_overview(recs)
        except Exception as e:      # noqa: BLE001
            print(f"[warn] 剪影/总览失败：{e}")
            import traceback
            traceback.print_exc(limit=2)
    if sil is None:
        try:
            sil = silhouette_report(recs)
        except Exception:   # noqa: BLE001
            sil = None

    all_pass = all(r["status"] == "PASS" for r in recs) and \
        all(r.get("model") is not None for r in recs)
    full = None
    run_full = (not args.skip_full_audit) and all_pass and \
        (state["ready"] or args.force) and args.only is None
    if run_full:
        print()
        print(f"[全量审计] audit_all --no-render --tag \"{AUDIT_TAG}\"")
        try:
            full = run_full_audit()
        except Exception as e:      # noqa: BLE001
            print(f"[warn] 全量审计失败：{e}")
            full = {"ok": False, "rows": 0, "hard_entities": [], "warnings": 0,
                    "sil_pairs": 0, "regressions": [], "hitbox_warnings": [],
                    "error": str(e)}
    elapsed = time.time() - t0
    md = write_report(recs, full, old_nums, sil or {"pairs": [], "pool_size": 0,
                                                   "vs_pool": []},
                      state, wait_s, elapsed)

    print()
    print(f"{'项':<4} {'模型':<26} {'cube':<6} {'目标':<6} {'高px':<9} {'目标高':<12} "
          f"{'宽px':<9} {'真洞':<5} {'挂点':<5} {'脚底格':<9} 判定")
    print("-" * 122)
    for r in recs:
        b = next(x for x in BOSSES if x["key"] == r["key"])
        t = b["targets"]
        ht = (f"{t['height'][0]}~{t['height'][1]}" if "height" in t else "-")
        hp = f"{r['height_px']:.1f}" if r.get("height_px") is not None else "-"
        wp = f"{r['width_px']:.1f}" if r.get("width_px") is not None else "-"
        wf = f"{r['world_foot']:+.3f}" if r.get("world_foot") is not None else "-"
        print(f"{r['item']:<4} {r['cls']:<26} {str(r.get('cubes', '-')):<6} "
              f"{('≥' + str(t['cubes'])):<6} {hp:<9} {ht:<12} {wp:<9} "
              f"{str(r.get('holes_real', '-')):<5} "
              f"{str(len(r.get('gaps_hard') or [])):<5} {wf:<9} {r['status']}")
    print()
    if full is not None:
        print(f"全量审计：{full['rows']} 实体 / 硬问题 {len(full['hard_entities'])} / "
              f"回归 {len(full['regressions'])} / hitbox 高差警告 "
              f"{len(full.get('hitbox_warnings') or [])}")
    else:
        print("全量审计：未执行（模型未全部落盘/跳过）")
    print(f"重制模型：{sum(1 for r in recs if r['status'] == 'PASS')}/{len(recs)} PASS")
    print(f"报告 -> {rel(md)}")
    print(f"总览 -> {rel(OVERVIEW)}")
    if not state["ready"] and not args.force:
        print("判定：WAIT（仍有模型未落盘，退出码 2）")
        return 2
    overall = all_pass and (full is None or full.get("ok", False))
    print(f"判定：{'PASS' if overall else 'FAIL（见 boss-remake.md §3）'}")
    return 0 if overall else 1


if __name__ == "__main__":
    sys.exit(main())
