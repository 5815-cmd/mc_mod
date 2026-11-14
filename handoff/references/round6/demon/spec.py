# -*- coding: utf-8 -*-
"""血魔老祖 Blood Demon Elder —— 三头六臂血魔（第六轮 Boss 重制 D）

跑：
    python -X utf8 工程/角色设计/tools/build_entity.py 工程/角色设计/round6/demon/spec.py

契约：`工程/21-Boss模型重制契约.md` §1 通用约束 + §2D 血魔老祖
目标：站高 6.0 格（96px）、肩宽 4 格（64px）、cube ≥85、贴图 256×256 + glow，
      最低点 y=24（袍摆底）、UV 0/0/0、真洞 0、挂点 0。

剪影三识别点（契约 §2D）：
  1. **三颗头**：中央戴冠主头最大（16×16×16 + 冕冠/骨刺/双角），左右两颗小头（12×14×14，各有角/眼/獠牙）；
  2. **六条手臂**：左右各 3，姿态错落（上抬持血刃 / 平伸张爪 / 下垂握爪），爪与血刃交替；
  3. **袍摆层叠 + 血雾**：血袍/裙甲 4 层下摆到 y=24；背部骨刺脊 + 蝠翼残片 + 血雾薄板。

配色：暗血红 / 黑 / 骨白 + 发光血眼 / 胸口血核 / 刃口 / 冠冕宝石。
"""
import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_TOOLS = os.path.normpath(os.path.join(_HERE, "..", "..", "tools"))
sys.path.insert(0, _TOOLS)
import studio as S  # noqa: E402

# ----------------------------------------------------------------------
# 1. 输出目标（写死契约路径：类名/贴图名不变，尺寸 128² → 256²）
# ----------------------------------------------------------------------
TARGET = dict(
    java=r"xiuxian-mod\src\main\java\com\hongmeng\xiuxian\client\BloodDemonModel.java",
    texture=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\boss_demon_elder.png",
    glow=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\boss_demon_elder_glow.png",
    outdir=r"工程\角色设计\round6\demon",
    lift=0.0,
)

# ----------------------------------------------------------------------
# 2. 色板（契约 §2D：暗血红/黑/骨白 + 发光血眼/血核/刃口）
# ----------------------------------------------------------------------
PALETTE = {
    "red":      ("#3A0A10", "#6A1420", "#9A2434"),   # 血肉主料
    "red_dark": ("#26060A", "#4A0E16", "#7A1E28"),   # 血肉暗部
    "robe":     ("#2E0810", "#521018", "#7A1A24"),   # 血袍
    "armor":    ("#12080A", "#241014", "#3E1A20"),   # 黑甲/肩甲/冠
    "crown":    ("#1A0A0E", "#3A1218", "#6A2028"),   # 冠冕底座
    "bone":     ("#6E5E4A", "#A89880", "#D8CCB4"),   # 骨白（骨刺/肋骨/牙）
    "horn":     ("#3A2E20", "#6A563C", "#9A8464"),   # 角
    "blade":    ("#2A060C", "#6A0E18", "#B01E2E"),   # 血刃刀身
    "mist":     ("#1A0408", "#3A0810", "#5E1018"),   # 血雾薄板 / 蝠翼膜
    "core":     ("#4A0A12", "#8A1020", "#C02033"),   # 胸口血核（发光）
    "eye":      ("#200404", "#5A0A10", "#FF3A2A"),   # 血眼（发光）
    "gem":      ("#3A2A0A", "#7A5410", "#C9A227"),   # 冠冕宝石（发光）
    "edge":     ("#4A0A10", "#B01E2E", "#FF7A5A"),   # 刃口（发光）
}
GLOW_PALETTE = {
    "core": "#FF2030",
    "eye": "#FF3A2A",
    "edge": "#FF7A5A",
    "gem": "#FFB030",
}

# ----------------------------------------------------------------------
# 3. Spec
# ----------------------------------------------------------------------
SPEC = S.Spec(
    class_name="BloodDemonModel",
    tex=(256, 256),
    zh="血魔老祖", en="Blood Demon Elder",
    base="EntityModel",
    generic="CultivationMonsterEntity",
    doc="三头六臂血魔：中央戴冠主头+左右小头、宽肩胸甲+外露肋骨+血核、六臂三姿态、"
        "血袍裙甲四层下摆到 y=24、背部骨刺脊/蝠翼残片/血雾薄板；站高 96px、肩宽 64px。",
)
# 契约 §1：生成后要有的最低点常量（放进 extra，重跑也不会丢）
SPEC.extra = (
    "\n    /** 最低点像素 y（血袍下摆底 = 24）。 */\n"
    "    public static final double LOWEST_PIXEL_Y = 24.0;\n"
)

# ----------------------------------------------------------------------
# 4. 骨架（+y 向下；脚底/最低点 y=24；头顶 -72 → 96px 高）
# ----------------------------------------------------------------------
body = SPEC.part("body", pivot=(0.0, 0.0, 0.0))

# ---- 下半身：髋 / 腰 / 血袍裙甲 4 层（下摆到 y=24）----
hip = SPEC.part("hip", parent=body, pivot=(0.0, 0.0, 0.0))
hip.box(-12, -8, -7, 24, 7, 15, "armor")
waist = SPEC.part("waist", parent=body, pivot=(0.0, -8.0, 0.0))
waist.box(-10, -8, -6, 20, 7, 13, "armor")

skirt1 = SPEC.part("skirt1", parent=hip, pivot=(0.0, 0.0, 0.0))
skirt1.box(-12, 0, -8, 24, 8, 16, "robe")
# 裙甲逐层链式父件：每层挂在上一层上（挂点连续，摆动能逐层放大）
skirt2 = SPEC.part("skirt2", parent=skirt1, pivot=(0.0, 6.0, 0.0))
skirt2.box(-15, 0, -9, 30, 7, 18, "red_dark")
skirt3 = SPEC.part("skirt3", parent=skirt2, pivot=(0.0, 6.0, 0.0))
skirt3.box(-17, 0, -9, 34, 6, 19, "robe")
skirt4 = SPEC.part("skirt4", parent=skirt3, pivot=(0.0, 5.0, 0.0))
skirt4.box(-20, 0, -11, 40, 7, 22, "red_dark")
# 双脚：契约「脚 y=24 藏于袍下」；也让 ground_audit 能识别脚底部件
foot_l = SPEC.part("foot_l", parent=skirt4, pivot=(0.0, 4.0, 0.0))
foot_l.box(-9, 0, -6, 6, 3, 8, "bone")
foot_r = SPEC.part("foot_r", parent=skirt4, pivot=(0.0, 4.0, 0.0))
foot_r.box(3, 0, -6, 6, 3, 8, "bone")

# ---- 躯干：宽胸 + 领甲 + 前胸甲 2 层 + 发光血核 + 外露肋骨 ----
chest = SPEC.part("chest", parent=body, pivot=(0.0, -16.0, 0.0))
chest.box(-13, -24, -8, 26, 24, 16, "red")
collar = SPEC.part("collar", parent=chest, pivot=(0.0, -24.0, 0.0))
collar.box(-15, -3, -9, 30, 3, 16, "armor")
plate1 = SPEC.part("chest_plate1", parent=chest, pivot=(0.0, -18.0, -9.0))
plate1.box(-13, -8, -2, 26, 8, 2, "armor")
plate2 = SPEC.part("chest_plate2", parent=chest, pivot=(0.0, -8.0, -9.0))
plate2.box(-11, -10, -2, 22, 9, 2, "armor")
core = SPEC.part("core", parent=chest, pivot=(0.0, -14.0, -9.0))
core.box(-5, -5, -3, 10, 10, 3, "core", glow=True)

# 外露肋骨：左右各 3 根（贴在胸侧，向前后伸出）
for _i, _y in enumerate((-18.0, -10.0, -2.0)):
    rl = SPEC.part("rib_l%d" % _i, parent=chest, pivot=(14.0, _y, 0.0))
    rl.box(0, 0, -8, 2, 2, 16, "bone")
    rr = SPEC.part("rib_r%d" % _i, parent=chest, pivot=(-14.0, _y, 0.0))
    rr.box(-2, 0, -8, 2, 2, 16, "bone")

# ---- 巨型层叠肩甲（左右各 3 层 + 3 根骨刺）----
shoulder_left = SPEC.part("shoulder_left", parent=chest, pivot=(14.0, -24.0, 0.0))
shoulder_left.box(0, -6, -8, 18, 8, 16, "armor")
shoulder_left2 = SPEC.part("shoulder_left2", parent=shoulder_left, pivot=(1.0, -6.0, 0.0))
shoulder_left2.box(0, -6, -6, 14, 5, 13, "red_dark")
for _i, (_x, _z) in enumerate(((4.0, -7.0), (9.0, 0.0), (14.0, 7.0))):
    sp = SPEC.part("shoulder_spike_left_%d" % _i, parent=shoulder_left, pivot=(_x, -6.0, _z))
    sp.box(0, -6, 0, 2, 6, 2, "bone")

shoulder_right = SPEC.part("shoulder_right", parent=chest, pivot=(-14.0, -24.0, 0.0))
shoulder_right.box(-18, -6, -8, 18, 8, 16, "armor")
shoulder_right2 = SPEC.part("shoulder_right2", parent=shoulder_right, pivot=(-1.0, -6.0, 0.0))
shoulder_right2.box(-14, -6, -6, 14, 5, 13, "red_dark")
for _i, (_x, _z) in enumerate(((-4.0, -7.0), (-9.0, 0.0), (-14.0, 7.0))):
    sp = SPEC.part("shoulder_spike_right_%d" % _i, parent=shoulder_right, pivot=(_x, -6.0, _z))
    sp.box(-2, -6, 0, 2, 6, 2, "bone")

# ---- 中央主头（最大：16×16×16）+ 冕冠 + 骨刺 + 双角 + 三眼 + 獠牙 ----
neck_mid = SPEC.part("neck_mid", parent=chest, pivot=(0.0, -24.0, 0.0))
neck_mid.box(-4, -8, -4, 8, 8, 8, "red_dark")
head_mid = SPEC.part("head_mid", parent=neck_mid, pivot=(0.0, -8.0, 0.0))
head_mid.box(-8, -14, -8, 16, 16, 16, "red")

crown_base = SPEC.part("crown_base", parent=head_mid, pivot=(0.0, -14.0, -1.0))
crown_base.box(-9, -5, -9, 18, 5, 18, "crown")
crown_gem = SPEC.part("crown_gem", parent=crown_base, pivot=(0.0, -4.0, -9.0))
crown_gem.box(-2, -2, 0, 4, 4, 1, "gem", glow=True)
_CRSPIKE = ((0.0, 0.0, 3.0, 5.0, 3.0), (-6.0, -6.0, 2.0, 4.0, 2.0),
            (6.0, 6.0, 2.0, 4.0, 2.0), (-6.0, 6.0, 2.0, 4.0, 2.0),
            (6.0, -6.0, 2.0, 4.0, 2.0))
for _i, (_x, _z, _w, _h, _d) in enumerate(_CRSPIKE):
    cs = SPEC.part("crown_spike_%d" % _i, parent=crown_base, pivot=(_x, -5.0, _z))
    cs.box(-_w / 2.0, -_h, -_d / 2.0, _w, _h, _d, "bone")

horn_l1 = SPEC.part("horn_l1", parent=head_mid, pivot=(7.0, -12.0, 0.0),
                    rot=(0.0, 0.0, -0.35))
horn_l1.box(0, -8, 0, 3, 8, 3, "horn")
horn_l2 = SPEC.part("horn_l2", parent=horn_l1, pivot=(1.0, -7.0, 0.0),
                    rot=(0.0, 0.0, -0.30))
horn_l2.box(0, -6, 0, 2, 6, 2, "horn")
horn_r1 = SPEC.part("horn_r1", parent=head_mid, pivot=(-7.0, -12.0, 0.0),
                    rot=(0.0, 0.0, 0.35))
horn_r1.box(-3, -8, 0, 3, 8, 3, "horn")
horn_r2 = SPEC.part("horn_r2", parent=horn_r1, pivot=(-1.0, -7.0, 0.0),
                    rot=(0.0, 0.0, 0.30))
horn_r2.box(-2, -6, 0, 2, 6, 2, "horn")

eye_mid = SPEC.part("eye_mid", parent=head_mid, pivot=(0.0, 0.0, 0.0))
eye_mid.box(-2, -13, -9, 4, 3, 1, "eye", glow=True)
eye_l = SPEC.part("eye_l", parent=head_mid, pivot=(0.0, 0.0, 0.0))
eye_l.box(-6, -9, -9, 3, 3, 1, "eye", glow=True)
eye_r = SPEC.part("eye_r", parent=head_mid, pivot=(0.0, 0.0, 0.0))
eye_r.box(3, -9, -9, 3, 3, 1, "eye", glow=True)

jaw_mid = SPEC.part("jaw_mid", parent=head_mid, pivot=(0.0, -2.0, -8.0))
jaw_mid.box(-7, 0, -7, 14, 4, 9, "red_dark")
for _i in range(4):
    tm = SPEC.part("tooth_mid_%d" % _i, parent=jaw_mid,
                   pivot=(-5.0 + _i * 3.3, -2.0, -7.0))
    tm.box(-0.5, 0, 0, 1, 2, 1, "bone")
fang_l = SPEC.part("fang_mid_l", parent=jaw_mid, pivot=(-6.0, -2.0, -6.0))
fang_l.box(-1, 0, -1, 2, 4, 2, "bone")
fang_r = SPEC.part("fang_mid_r", parent=jaw_mid, pivot=(4.0, -2.0, -6.0))
fang_r.box(-1, 0, -1, 2, 4, 2, "bone")

# ---- 左右两颗小头（12×14×14，各有角/眼/獠牙）----
# 先加一段颈根（挂点连续：胸顶 → 小头底），避免挂点断开
side_neck_l = SPEC.part("side_neck_l", parent=chest, pivot=(15.0, -26.0, -3.0))
side_neck_l.box(-3, -10, -3, 6, 14, 6, "armor")
head_l = SPEC.part("head_l", parent=side_neck_l, pivot=(0.0, -8.0, 0.0))
head_l.box(-6, -7, -7, 12, 14, 14, "red_dark")
horn_side_left1 = SPEC.part("horn_side_left1", parent=head_l, pivot=(5.0, -6.0, 0.0),
                         rot=(0.0, 0.0, -0.40))
horn_side_left1.box(0, -6, 0, 2, 6, 2, "horn")
horn_side_left2 = SPEC.part("horn_side_left2", parent=horn_side_left1, pivot=(0.0, -5.0, 0.0),
                         rot=(0.0, 0.0, -0.30))
horn_side_left2.box(0, -5, 0, 2, 5, 2, "horn")
eye_side_l1 = SPEC.part("eye_side_l1", parent=head_l, pivot=(0.0, 0.0, 0.0))
eye_side_l1.box(-4, -4, -8, 2, 2, 1, "eye", glow=True)
eye_side_l2 = SPEC.part("eye_side_l2", parent=head_l, pivot=(0.0, 0.0, 0.0))
eye_side_l2.box(2, -4, -8, 2, 2, 1, "eye", glow=True)
jaw_l = SPEC.part("jaw_l", parent=head_l, pivot=(0.0, 0.0, -7.0))
jaw_l.box(-5, 0, -5, 10, 3, 7, "red_dark")
tooth_l0 = SPEC.part("tooth_l0", parent=jaw_l, pivot=(-3.0, -1.0, -5.0))
tooth_l0.box(0, 0, 0, 1, 2, 1, "bone")
tooth_l1 = SPEC.part("tooth_l1", parent=jaw_l, pivot=(2.0, -1.0, -5.0))
tooth_l1.box(0, 0, 0, 1, 2, 1, "bone")

side_neck_r = SPEC.part("side_neck_r", parent=chest, pivot=(-15.0, -26.0, -3.0))
side_neck_r.box(-3, -10, -3, 6, 14, 6, "armor")
head_r = SPEC.part("head_r", parent=side_neck_r, pivot=(0.0, -8.0, 0.0))
head_r.box(-6, -7, -7, 12, 14, 14, "red_dark")
horn_side_right1 = SPEC.part("horn_side_right1", parent=head_r, pivot=(-5.0, -6.0, 0.0),
                         rot=(0.0, 0.0, 0.40))
horn_side_right1.box(-2, -6, 0, 2, 6, 2, "horn")
horn_side_right2 = SPEC.part("horn_side_right2", parent=horn_side_right1, pivot=(0.0, -5.0, 0.0),
                         rot=(0.0, 0.0, 0.30))
horn_side_right2.box(-2, -5, 0, 2, 5, 2, "horn")
eye_side_r1 = SPEC.part("eye_side_r1", parent=head_r, pivot=(0.0, 0.0, 0.0))
eye_side_r1.box(-4, -4, -8, 2, 2, 1, "eye", glow=True)
eye_side_r2 = SPEC.part("eye_side_r2", parent=head_r, pivot=(0.0, 0.0, 0.0))
eye_side_r2.box(2, -4, -8, 2, 2, 1, "eye", glow=True)
jaw_r = SPEC.part("jaw_r", parent=head_r, pivot=(0.0, 0.0, -7.0))
jaw_r.box(-5, 0, -5, 10, 3, 7, "red_dark")
tooth_r0 = SPEC.part("tooth_r0", parent=jaw_r, pivot=(-3.0, -1.0, -5.0))
tooth_r0.box(0, 0, 0, 1, 2, 1, "bone")
tooth_r1 = SPEC.part("tooth_r1", parent=jaw_r, pivot=(2.0, -1.0, -5.0))
tooth_r1.box(0, 0, 0, 1, 2, 1, "bone")

# ---- 六条手臂（左右各 3；上抬持血刃 / 平伸张爪 / 下垂握爪）----
# 左侧：+x 侧；右侧镜像。姿态错落用 PartPose 静态 rot。
arm_u1_left = SPEC.part("arm_u1_left", parent=chest, pivot=(13.0, -24.0, 0.0),
                    rot=(0.0, 0.0, -0.55))
arm_u1_left.box(0, 0, -4, 8, 13, 7, "red")
arm_f1_left = SPEC.part("arm_f1_left", parent=arm_u1_left, pivot=(0.0, 13.0, 0.0),
                    rot=(0.35, 0.0, 0.35))
arm_f1_left.box(-4, 0, -4, 7, 11, 7, "red_dark")
blade1_left = SPEC.part("blade1_left", parent=arm_f1_left, pivot=(0.0, 11.0, 0.0))
blade1_left.box(-1, 0, -2, 2, 18, 4, "blade")
edge1_left = SPEC.part("edge1_left", parent=blade1_left, pivot=(0.0, 0.0, 0.0))
edge1_left.box(-1, 0, -3, 1, 18, 1, "edge", glow=True)

arm_u2_left = SPEC.part("arm_u2_left", parent=chest, pivot=(12.0, -14.0, 2.0),
                    rot=(0.25, 0.0, -0.15))
arm_u2_left.box(0, 0, -3, 7, 13, 7, "armor")
arm_f2_left = SPEC.part("arm_f2_left", parent=arm_u2_left, pivot=(0.0, 12.0, 0.0),
                    rot=(0.45, 0.0, 0.15))
arm_f2_left.box(-3, 0, -3, 7, 10, 7, "red_dark")
hand2_left = SPEC.part("hand2_left", parent=arm_f2_left, pivot=(0.0, 10.0, 0.0))
hand2_left.box(-4, 0, -3, 8, 5, 7, "red_dark")
finger2_left_0 = SPEC.part("finger2_left_0", parent=hand2_left, pivot=(-2.0, 5.0, -2.0))
finger2_left_0.box(-1, 0, -1, 2, 4, 2, "bone")
finger2_left_1 = SPEC.part("finger2_left_1", parent=hand2_left, pivot=(1.0, 5.0, -2.0))
finger2_left_1.box(-1, 0, -1, 2, 4, 2, "bone")

arm_u3_left = SPEC.part("arm_u3_left", parent=chest, pivot=(13.0, -4.0, -1.0),
                    rot=(0.0, 0.0, 0.35))
arm_u3_left.box(0, 0, -3, 7, 13, 7, "red")
arm_f3_left = SPEC.part("arm_f3_left", parent=arm_u3_left, pivot=(0.0, 13.0, 0.0),
                    rot=(0.20, 0.0, -0.25))
arm_f3_left.box(-3, 0, -3, 7, 10, 7, "red_dark")
hand3_left = SPEC.part("hand3_left", parent=arm_f3_left, pivot=(0.0, 10.0, 0.0))
hand3_left.box(-4, 0, -3, 8, 5, 7, "red_dark")
finger3_left_0 = SPEC.part("finger3_left_0", parent=hand3_left, pivot=(-2.0, 5.0, -2.0))
finger3_left_0.box(-1, 0, -1, 2, 4, 2, "bone")
finger3_left_1 = SPEC.part("finger3_left_1", parent=hand3_left, pivot=(1.0, 5.0, -2.0))
finger3_left_1.box(-1, 0, -1, 2, 4, 2, "bone")

arm_u1_right = SPEC.part("arm_u1_right", parent=chest, pivot=(-13.0, -24.0, 0.0),
                    rot=(0.0, 0.0, 0.55))
arm_u1_right.box(-8, 0, -4, 8, 13, 7, "red")
arm_f1_right = SPEC.part("arm_f1_right", parent=arm_u1_right, pivot=(0.0, 13.0, 0.0),
                    rot=(0.35, 0.0, -0.35))
arm_f1_right.box(-4, 0, -4, 7, 11, 7, "red_dark")
blade1_right = SPEC.part("blade1_right", parent=arm_f1_right, pivot=(0.0, 11.0, 0.0))
blade1_right.box(-1, 0, -2, 2, 18, 4, "blade")
edge1_right = SPEC.part("edge1_right", parent=blade1_right, pivot=(0.0, 0.0, 0.0))
edge1_right.box(0, 0, -3, 1, 18, 1, "edge", glow=True)

arm_u2_right = SPEC.part("arm_u2_right", parent=chest, pivot=(-12.0, -14.0, 2.0),
                    rot=(0.25, 0.0, 0.15))
arm_u2_right.box(-7, 0, -3, 7, 13, 7, "armor")
arm_f2_right = SPEC.part("arm_f2_right", parent=arm_u2_right, pivot=(0.0, 12.0, 0.0),
                    rot=(0.45, 0.0, -0.15))
arm_f2_right.box(-3, 0, -3, 7, 10, 7, "red_dark")
hand2_right = SPEC.part("hand2_right", parent=arm_f2_right, pivot=(0.0, 10.0, 0.0))
hand2_right.box(-4, 0, -3, 8, 5, 7, "red_dark")
finger2_right_0 = SPEC.part("finger2_right_0", parent=hand2_right, pivot=(-2.0, 5.0, -2.0))
finger2_right_0.box(-1, 0, -1, 2, 4, 2, "bone")
finger2_right_1 = SPEC.part("finger2_right_1", parent=hand2_right, pivot=(1.0, 5.0, -2.0))
finger2_right_1.box(-1, 0, -1, 2, 4, 2, "bone")

arm_u3_right = SPEC.part("arm_u3_right", parent=chest, pivot=(-13.0, -4.0, -1.0),
                    rot=(0.0, 0.0, -0.35))
arm_u3_right.box(-7, 0, -3, 7, 13, 7, "red")
arm_f3_right = SPEC.part("arm_f3_right", parent=arm_u3_right, pivot=(0.0, 13.0, 0.0),
                    rot=(0.20, 0.0, 0.25))
arm_f3_right.box(-3, 0, -3, 7, 10, 7, "red_dark")
hand3_right = SPEC.part("hand3_right", parent=arm_f3_right, pivot=(0.0, 10.0, 0.0))
hand3_right.box(-4, 0, -3, 8, 5, 7, "red_dark")
finger3_right_0 = SPEC.part("finger3_right_0", parent=hand3_right, pivot=(-2.0, 5.0, -2.0))
finger3_right_0.box(-1, 0, -1, 2, 4, 2, "bone")
finger3_right_1 = SPEC.part("finger3_right_1", parent=hand3_right, pivot=(1.0, 5.0, -2.0))
finger3_right_1.box(-1, 0, -1, 2, 4, 2, "bone")

# ---- 背部：骨刺脊 + 蝠翼残片 + 血雾薄板 ----
spine = SPEC.part("spine", parent=chest, pivot=(0.0, -24.0, 9.0),
                  rot=(-0.12, 0.0, 0.0))
spine.box(-4, 0, -3, 8, 26, 6, "bone")
for _i in range(6):
    ssp = SPEC.part("spine_spike_%d" % _i, parent=spine, pivot=(0.0, 2.0 + _i * 4.0, 0.0))
    ssp.box(-1, -5, 0, 2, 5, 2, "bone")

wing_bone1_left = SPEC.part("wing_bone1_left", parent=chest, pivot=(10.0, -10.0, 9.0),
                        rot=(0.0, 0.0, -0.50))
wing_bone1_left.box(0, 0, 0, 2, 12, 2, "bone")
wing_bone2_left = SPEC.part("wing_bone2_left", parent=wing_bone1_left, pivot=(0.0, 12.0, 0.0),
                        rot=(0.0, 0.0, -0.40))
wing_bone2_left.box(0, 0, 0, 2, 10, 2, "bone")
wing_mem_left = SPEC.part("wing_mem_left", parent=wing_bone1_left, pivot=(1.0, 0.0, 0.0))
wing_mem_left.box(0, 0, -7, 1, 12, 14, "mist")

wing_bone1_right = SPEC.part("wing_bone1_right", parent=chest, pivot=(-10.0, -10.0, 9.0),
                        rot=(0.0, 0.0, 0.50))
wing_bone1_right.box(0, 0, 0, 2, 12, 2, "bone")
wing_bone2_right = SPEC.part("wing_bone2_right", parent=wing_bone1_right, pivot=(0.0, 12.0, 0.0),
                        rot=(0.0, 0.0, 0.40))
wing_bone2_right.box(0, 0, 0, 2, 10, 2, "bone")
wing_mem_right = SPEC.part("wing_mem_right", parent=wing_bone1_right, pivot=(-1.0, 0.0, 0.0))
wing_mem_right.box(-1, 0, -7, 1, 12, 14, "mist")

mist_a = SPEC.part("mist_a", parent=body, pivot=(0.0, -18.0, 0.0),
                   rot=(0.0, 0.25, 0.0))
mist_a.box(-18, -2, -16, 36, 3, 1, "mist")
mist_b = SPEC.part("mist_b", parent=body, pivot=(0.0, -12.0, 0.0),
                   rot=(0.0, -0.30, 0.0))
mist_b.box(-16, -2, 12, 32, 3, 1, "mist")

# ----------------------------------------------------------------------
# 5. 动画（绝对赋值，禁止 +=/*=）
#    三头错相微转 / 六臂缓慢张合 / 袍摆血雾浮动 / 血核脉动 / 受击与死亡
# ----------------------------------------------------------------------
SPEC.anim = """
        float t = ageInTicks;
        float breathe = Mth.sin(t * 0.05F);
        float yaw = netHeadYaw * ((float) Math.PI / 180.0F);
        float pitch = headPitch * ((float) Math.PI / 180.0F);
        float atk = entity.getAttackAnim(0.0F);
        float swing = Mth.sin(atk * (float) Math.PI);
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);

        // ---- 躯干呼吸（缩放，不动 y）+ 受击后仰 / 死亡前扑 ----
        this.chest.xScale = 1.0F + breathe * 0.012F;
        this.chest.zScale = 1.0F + breathe * 0.012F;
        this.chest.xRot = -0.02F + breathe * 0.01F - hurt * 0.10F + dead * 0.25F;
        this.chest.zRot = 0.0F;
        this.body.y = -dead * 2.0F;

        // ---- 三头：中央跟视线最紧，左右错相微转（相位差 2.1 rad）----
        float phaseL = Mth.sin(t * 0.07F);
        float phaseR = Mth.sin(t * 0.07F + 2.1F);
        this.headMid.yRot = yaw * 0.80F;
        this.headMid.xRot = pitch * 0.60F + breathe * 0.010F - swing * 0.08F;
        this.headMid.zRot = 0.0F;
        this.headL.yRot = yaw * 0.45F - 0.30F + phaseL * 0.06F;
        this.headL.xRot = pitch * 0.35F + phaseL * 0.04F - hurt * 0.10F;
        this.headL.zRot = phaseL * 0.02F;
        this.headR.yRot = yaw * 0.45F + 0.30F + phaseR * 0.06F;
        this.headR.xRot = pitch * 0.35F + phaseR * 0.04F - hurt * 0.10F;
        this.headR.zRot = phaseR * 0.02F;
        this.neckMid.yRot = yaw * 0.25F;
        this.neckMid.xRot = breathe * 0.015F;
        this.crownBase.yRot = yaw * 0.10F;
        this.crownGem.yRot = yaw * 0.10F;
        this.hornL1.zRot = -0.35F + phaseL * 0.02F;
        this.hornR1.zRot = 0.35F - phaseR * 0.02F;
        this.jawMid.xRot = 0.06F + swing * 0.35F;
        this.jawMid.yRot = 0.0F;
        this.jawL.xRot = 0.05F + swing * 0.20F;
        this.jawR.xRot = 0.05F + swing * 0.20F;
        this.eyeMid.zScale = 1.0F + Mth.sin(t * 0.13F) * 0.05F;

        // ---- 六臂：缓慢张合 + 出手时上抬/前压（三条臂相位错开）----
        float open1 = Mth.sin(t * 0.06F);
        float open2 = Mth.sin(t * 0.06F + 1.4F);
        float open3 = Mth.sin(t * 0.06F + 2.8F);
        this.armU1Left.zRot = -0.55F + open1 * 0.05F - swing * 0.20F;
        this.armU1Right.zRot = 0.55F - open1 * 0.05F + swing * 0.20F;
        this.armF1Left.xRot = 0.35F + breathe * 0.03F + swing * 0.30F;
        this.armF1Right.xRot = 0.35F + breathe * 0.03F + swing * 0.30F;
        this.blade1Left.xRot = 0.05F - swing * 0.45F;
        this.blade1Right.xRot = 0.05F - swing * 0.45F;
        this.armU2Left.zRot = -0.15F + open2 * 0.05F;
        this.armU2Right.zRot = 0.15F - open2 * 0.05F;
        this.armF2Left.xRot = 0.45F + open2 * 0.04F + swing * 0.20F;
        this.armF2Right.xRot = 0.45F + open2 * 0.04F + swing * 0.20F;
        this.hand2Left.xRot = swing * 0.15F;
        this.hand2Right.xRot = swing * 0.15F;
        this.armU3Left.zRot = 0.35F + open3 * 0.05F;
        this.armU3Right.zRot = -0.35F - open3 * 0.05F;
        this.armF3Left.xRot = 0.20F + open3 * 0.04F;
        this.armF3Right.xRot = 0.20F + open3 * 0.04F;
        this.hand3Left.xRot = open3 * 0.06F;
        this.hand3Right.xRot = -open3 * 0.06F;
        this.shoulderLeft.xRot = breathe * 0.02F;
        this.shoulderRight.xRot = breathe * 0.02F;

        // ---- 血袍裙甲 4 层：逐层错相摆动（下摆最飘）----
        this.skirt1.zRot = Mth.sin(t * 0.05F) * 0.02F;
        this.skirt2.zRot = Mth.sin(t * 0.05F + 0.6F) * 0.03F;
        this.skirt3.zRot = Mth.sin(t * 0.05F + 1.2F) * 0.035F;
        this.skirt4.zRot = Mth.sin(t * 0.05F + 1.8F) * 0.045F;
        this.skirt2.xRot = Mth.sin(t * 0.04F + 0.3F) * 0.015F;
        this.skirt3.xRot = Mth.sin(t * 0.04F + 0.9F) * 0.02F;
        this.skirt4.xRot = Mth.sin(t * 0.04F + 1.5F) * 0.025F;

        // ---- 胸口血核：脉动（比其它 Boss 更快、更像心跳）----
        float beat = 1.0F + Mth.sin(t * 0.25F) * 0.10F + swing * 0.12F;
        this.core.xScale = beat;
        this.core.yScale = beat;
        this.core.zScale = beat;
        this.core.yRot = t * 0.05F;

        // ---- 背部骨刺脊 / 蝠翼残片 / 血雾薄板：缓慢浮动 ----
        this.spine.xRot = -0.12F + Mth.sin(t * 0.04F) * 0.02F;
        this.wingBone1Left.zRot = -0.50F + Mth.sin(t * 0.08F) * 0.06F;
        this.wingBone1Right.zRot = 0.50F - Mth.sin(t * 0.08F + 1.3F) * 0.06F;
        this.wingBone2Left.zRot = -0.40F + Mth.sin(t * 0.08F + 0.7F) * 0.05F;
        this.wingBone2Right.zRot = 0.40F - Mth.sin(t * 0.08F + 2.0F) * 0.05F;
        this.wingMemLeft.zRot = Mth.sin(t * 0.06F) * 0.04F;
        this.wingMemRight.zRot = -Mth.sin(t * 0.06F + 0.9F) * 0.04F;
        this.mistA.yRot = 0.25F + Mth.sin(t * 0.04F) * 0.10F;
        this.mistB.yRot = -0.30F - Mth.sin(t * 0.04F + 1.0F) * 0.10F;
"""
