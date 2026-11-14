# -*- coding: utf-8 -*-
"""虚空道主（VoidLord / void_boss）· 第六轮重制 Spec

设计：**浮空独眼虚无法相**（契约 21 §2E）
    无腿：下摆/布幔/虚空触须收束到 y=24（浮空）；
    法袍躯干：层叠布幔 3 层 + 腰封 + 宽肩甲；
    独眼头：巨眼（发光）+ 眼睑 + 移动瞳孔 + 额冠金饰 + 裂纹 + 侧角；
    6 片悬浮翼片（每侧 3，各分 3 节、节间 2px 离体）铺开成扇；
    4 条手臂（2 长 2 短）持虚空晶刃；
    4 块悬浮符文石环绕 + 背后虚空环（金环 + 发光符面）。

坐标：1 unit = 1/16 格；+y 向下；-z 为正面；脚底/最低点 y = 24；浮空高 104px = 6.5 格。
贴图：256×256（主 + `_glow.png`）。
"""
import math
import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_TOOLS = os.path.normpath(os.path.join(_HERE, "..", "..", "tools"))
sys.path.insert(0, _TOOLS)
import studio as S  # noqa: E402

# ----------------------------------------------------------------------
# 1. 输出目标（整文件重写现有模型；类名/路径不变）
# ----------------------------------------------------------------------
TARGET = dict(
    java=r"xiuxian-mod\src\main\java\com\hongmeng\xiuxian\client\VoidLordModel.java",
    texture=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\boss_void_lord.png",
    glow=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\boss_void_lord_glow.png",
    outdir=r"工程\角色设计\round6\void",
    lift=0.0,
)

# ----------------------------------------------------------------------
# 2. 色板（深紫/暗蓝黑 + 冷白骨 + 金边 + 青发光）
# ----------------------------------------------------------------------
PALETTE = {
    "void_black": ("#07050D", "#100B1A", "#1C142C"),
    "void_purple": ("#160B2A", "#2A1450", "#4B2A8C"),
    "robe_blue": ("#080D20", "#141F44", "#2A3D78"),
    "robe_purple": ("#1A0E30", "#33205C", "#5B3E96"),
    "cloth": ("#261340", "#412769", "#6F51A6"),
    "bone": ("#6A6E7E", "#A9AFBF", "#E2E8F4"),
    "gold": ("#6B5420", "#C9A038", "#F0D283"),
    "stone": ("#12101A", "#221D2E", "#3A334C"),
    "crystal": ("#16333F", "#2A6478", "#8FE0EC"),
    "eye": ("#07121A", "#0F3540", "#1E6A78"),
    "pupil": ("#0A1A20", "#12424C", "#2A7C8A"),
    "crack": ("#2A0A1E", "#5A1030", "#A02050"),
}
GLOW_PALETTE = {
    "eye": "#8FF8FF",
    "pupil": "#D8FDFF",
    "crystal": "#9FF6FF",
    "gold": "#FFE28A",
    "crack": "#7FE8FF",
}

# ----------------------------------------------------------------------
# 3. 骨架
# ----------------------------------------------------------------------
SPEC = S.Spec(
    class_name="VoidLordModel",
    tex=(256, 256),
    zh="虚空道主",
    en="VoidLord",
    base="EntityModel",
    generic="CultivationMonsterEntity",
    layer_id=None,
    identifier="geometry.void_lord",
    doc=("浮空独眼虚无法相：无腿下摆收束 y=24、层叠法袍 3 层、独眼头（巨眼/眼睑/瞳孔/额冠）、"
         "6 片分节悬浮翼片、4 条手臂持虚空晶刃、4 块悬浮符文石 + 背后虚空环"),
)

# ======================================================================
# 3.1 下摆 / 法袍裙甲（根部件；最低点 y=24）
# ======================================================================
robe = SPEC.part("robe", pivot=(0.0, 24.0, 0.0))
robe.box(-20.0, -4.0, -9.0, 40.0, 4.0, 18.0, "robe_purple")   # 下摆最外层 y 20..24
robe.box(-17.0, -10.0, -7.0, 34.0, 6.0, 14.0, "robe_blue")    # 第二层 y 14..20
robe.box(-14.0, -16.0, -6.0, 28.0, 6.0, 12.0, "robe_purple")  # 第三层 y 8..14
robe.box(-9.0, -20.0, -4.0, 18.0, 4.0, 8.0, "void_black")     # 内层暗影 y 4..8
robe.box(-12.0, -22.0, 8.0, 24.0, 18.0, 2.0, "cloth")         # 背后垂布 y 2..20

# 四片垂摆布幔（挂在裙甲四边；pivot 在 y=24 边上，向下摆动）
hemF = SPEC.part("hem_f", parent=robe, pivot=(0.0, 0.0, -9.0))
hemF.box(-20.0, -8.0, -2.0, 40.0, 8.0, 3.0, "cloth")           # y 16..24
hemB = SPEC.part("hem_b", parent=robe, pivot=(0.0, 0.0, 9.0))
hemB.box(-18.0, -8.0, -1.0, 36.0, 8.0, 2.0, "robe_blue")       # y 16..24
hemL = SPEC.part("hem_l", parent=robe, pivot=(-20.0, 0.0, 0.0))
hemL.box(-2.0, -8.0, -9.0, 3.0, 8.0, 18.0, "cloth")           # x -22..-19, y 16..24
hemR = SPEC.part("hem_r", parent=robe, pivot=(20.0, 0.0, 0.0))
hemR.box(-1.0, -8.0, -9.0, 3.0, 8.0, 18.0, "cloth")           # x 19..22

# 6 条虚空触须（尖端精确落在 y=24）
for _i, (_tx, _tz) in enumerate((
        (-14.0, -10.0), (13.0, -11.0), (-11.0, 12.0),
        (10.0, 9.0), (0.0, 10.0), (-2.0, -9.0))):
    _t = SPEC.part("tendril%d" % _i, parent=robe, pivot=(_tx, 0.0, _tz))
    _t.box(-1.0, -8.0, -1.0, 2.0, 8.0, 2.0, "void_black")

# 2 条两节虚空长触须（从裙摆侧面垂落再卷起）
tentAUp = SPEC.part("tent_a_up", parent=robe, pivot=(16.0, 0.0, 5.0))
tentAUp.box(-1.0, -14.0, -1.0, 2.0, 14.0, 2.0, "void_purple")
tentATip = SPEC.part("tent_a_tip", parent=tentAUp, pivot=(0.0, -14.0, 0.0))
tentATip.box(-1.0, -9.0, -1.0, 1.0, 9.0, 1.0, "void_black")
tentBUp = SPEC.part("tent_b_up", parent=robe, pivot=(-15.0, 0.0, 6.0))
tentBUp.box(-1.0, -13.0, -1.0, 2.0, 13.0, 2.0, "void_purple")
tentBTip = SPEC.part("tent_b_tip", parent=tentBUp, pivot=(0.0, -13.0, 0.0))
tentBTip.box(-1.0, -8.0, -1.0, 1.0, 8.0, 1.0, "void_black")

# ======================================================================
# 3.2 腰封 / 法袍躯干（层叠 3 层）
# ======================================================================
waist = SPEC.part("waist", parent=robe, pivot=(0.0, -20.0, 0.0))
waist.box(-9.0, -6.0, -5.0, 18.0, 10.0, 10.0, "robe_blue")     # y -2..8
waist.box(-10.0, -6.0, -6.0, 20.0, 3.0, 12.0, "gold")          # 金腰封 y -2..1
waist.box(-9.0, -3.0, -6.0, 18.0, 2.0, 12.0, "cloth")          # 紫绸束带 y 1..3
waist.box(-2.0, -6.0, -9.0, 4.0, 4.0, 1.0, "crystal", glow=True)  # 腰前晶核 y -2..2

torso = SPEC.part("torso", parent=waist, pivot=(0.0, -6.0, 0.0))
torso.box(-15.0, -24.0, -6.0, 30.0, 22.0, 12.0, "robe_purple")  # 胸腹主袍 y -26..-2
torso.box(-16.0, -22.0, -7.0, 32.0, 18.0, 14.0, "robe_blue")   # 法袍第二层 y -24..-4
torso.box(-14.0, -20.0, -8.0, 28.0, 16.0, 1.0, "cloth")        # 前襟布幔 y -22..-6
torso.box(-2.0, -24.0, 7.0, 4.0, 20.0, 1.0, "eye", glow=True)   # 背部发光脊 y -26..-6
torso.box(-4.0, -18.0, -9.0, 8.0, 8.0, 1.0, "crystal", glow=True)  # 胸口虚空核 y -20..-12
torso.box(-15.0, -20.0, -8.0, 2.0, 14.0, 2.0, "bone")          # 左胸肋骨
torso.box(13.0, -20.0, -8.0, 2.0, 14.0, 2.0, "bone")           # 右胸肋骨

# 层叠肩甲（2 层）
pauldronL = SPEC.part("pauldron_l", parent=torso, pivot=(-14.0, -24.0, 0.0))
pauldronL.box(-11.0, -3.0, -7.0, 12.0, 6.0, 14.0, "robe_blue")
pauldronL.box(-10.0, -6.0, -6.0, 10.0, 4.0, 11.0, "gold")
pauldronL.box(-11.0, -9.0, -3.0, 3.0, 3.0, 4.0, "void_purple")
pauldronR = SPEC.part("pauldron_r", parent=torso, pivot=(14.0, -24.0, 0.0))
pauldronR.box(0.0, -3.0, -7.0, 12.0, 6.0, 14.0, "robe_blue")
pauldronR.box(0.0, -6.0, -6.0, 10.0, 4.0, 11.0, "gold")
pauldronR.box(8.0, -9.0, -3.0, 3.0, 3.0, 4.0, "void_purple")

# ======================================================================
# 3.3 颈 / 独眼头（巨眼 + 眼睑 + 瞳孔 + 额冠 + 侧角 + 裂纹）
# ======================================================================
neck = SPEC.part("neck", parent=torso, pivot=(0.0, -24.0, 0.0))
neck.box(-6.0, -30.0, -5.0, 12.0, 30.0, 10.0, "robe_blue")      # y -56..-26
neck.box(-8.0, -4.0, -8.0, 16.0, 4.0, 16.0, "gold")             # 领口金环 y -30..-26
neck.box(8.0, -28.0, 6.0, 2.0, 22.0, 2.0, "void_black")         # 颈部触须
neck.box(-9.0, -28.0, 6.0, 2.0, 18.0, 2.0, "void_black")
neck.box(-2.0, -8.0, -6.0, 4.0, 4.0, 1.0, "crystal", glow=True)  # 领口晶核

head = SPEC.part("head", parent=neck, pivot=(0.0, -30.0, 0.0))
head.box(-14.0, -22.0, -8.0, 28.0, 22.0, 16.0, "bone")          # 颅骨 y -78..-56
head.box(-15.0, -19.0, -9.0, 30.0, 4.0, 2.0, "void_purple")     # 眉脊 y -75..-71
head.box(-11.0, -6.0, -9.0, 22.0, 4.0, 2.0, "bone")             # 下颚 y -62..-58
head.box(-4.0, -24.0, -9.0, 8.0, 3.0, 2.0, "gold")              # 额冠中央 y -80..-77
head.box(-12.0, -24.0, -7.0, 7.0, 3.0, 2.0, "gold")              # 额冠左
head.box(5.0, -24.0, -7.0, 7.0, 3.0, 2.0, "gold")                # 额冠右
head.box(-19.0, -20.0, -5.0, 4.0, 16.0, 4.0, "bone")             # 左侧角
head.box(15.0, -20.0, -5.0, 4.0, 16.0, 4.0, "bone")              # 右侧角
head.box(11.0, -20.0, 8.0, 2.0, 18.0, 2.0, "void_purple")       # 颅后触须 ×3
head.box(0.0, -22.0, 9.0, 2.0, 20.0, 2.0, "void_purple")
head.box(-11.0, -18.0, 8.0, 2.0, 16.0, 2.0, "void_purple")
head.box(8.0, -23.0, -3.0, 2.0, 2.0, 2.0, "bone")                # 颅顶骨刺
head.box(-10.0, -23.0, 1.0, 2.0, 2.0, 2.0, "bone")
# 眼眶金环（上下左右）
head.box(-11.0, -19.0, -9.0, 22.0, 2.0, 1.0, "gold")
head.box(-11.0, -7.0, -9.0, 22.0, 2.0, 1.0, "gold")
head.box(-12.0, -19.0, -9.0, 2.0, 12.0, 1.0, "gold")
head.box(10.0, -19.0, -9.0, 2.0, 12.0, 1.0, "gold")

eye = SPEC.part("eye", parent=head, pivot=(0.0, -13.0, -10.0))
eye.box(-9.0, -5.0, 0.0, 18.0, 10.0, 1.0, "eye", glow=True)      # 巨眼 y -74..-64
pupil = SPEC.part("pupil", parent=head, pivot=(0.0, -13.0, -11.0))
pupil.box(-3.0, -3.0, 0.0, 6.0, 6.0, 1.0, "pupil", glow=True)    # 瞳孔 y -72..-66
lidUp = SPEC.part("lid_up", parent=head, pivot=(0.0, -18.0, -10.0))
lidUp.box(-10.0, -2.0, 0.0, 20.0, 3.0, 1.0, "bone")               # 上眼睑 y -76..-73
lidDn = SPEC.part("lid_dn", parent=head, pivot=(0.0, -8.0, -10.0))
lidDn.box(-10.0, 0.0, 0.0, 20.0, 2.0, 1.0, "bone")                # 下眼睑 y -64..-62
crackA = SPEC.part("crack_a", parent=head, pivot=(-10.0, -16.0, -10.0))
crackA.box(0.0, -2.0, 0.0, 1.0, 5.0, 1.0, "crack", glow=True)    # 眼下裂纹
crackA.box(-1.0, 2.0, 0.0, 3.0, 1.0, 1.0, "crack", glow=True)
crackB = SPEC.part("crack_b", parent=head, pivot=(10.0, -14.0, -10.0))
crackB.box(0.0, -1.0, 0.0, 1.0, 4.0, 1.0, "crack", glow=True)    # 另侧裂纹（三阶段显形）

# ======================================================================
# 3.4 六片悬浮翼片（每侧 3，各 3 节；节与节之间 2px 离体悬浮）
# ======================================================================
# 每片翼：pivot 在肩后（离躯干 1~2px 的悬浮感），PartPose 先给基础展开角；
# 分节用 2px 间隙，动画里再错相浮动。


def _blade_box(side, length):
    """沿 ±x 伸出的翼片（厚 4px、深 2px），从 2px 间隙后开始。"""
    if side > 0:
        return (2.0, -3.0, -1.0, float(length), 6.0, 2.0)
    return (-float(length) - 2.0, -3.0, -1.0, float(length), 6.0, 2.0)


def _membrane_box(side, length):
    """翼片下缘薄膜（1px 厚），拉开负空间。"""
    if side > 0:
        return (0.0, 2.0, -1.0, float(length), 1.0, 5.0)
    return (-float(length), 2.0, -1.0, float(length), 1.0, 5.0)


_WING_DEF = (
    # (翼序号, pivot 世界 y, 基础展开角(rad), 三节长度)
    (0, -24.0, 0.62, (20, 18, 16)),
    (1, -17.0, 0.20, (20, 18, 16)),
    (2, -10.0, -0.26, (18, 16, 14)),
)
for _side, _s in (("l", 1), ("r", -1)):
    for _k, _py, _ang, _lens in _WING_DEF:
        _name = "wing_%s_%d" % (_side, _k)
        _a = SPEC.part(_name + "_a", parent=torso,
                       pivot=(12.0 * _s, _py + 2.0, 3.0),
                       rot=(0.0, 0.0, -_s * _ang))
        _a.box(*_blade_box(_s, _lens[0]), "robe_purple")
        _b = SPEC.part(_name + "_b", parent=_a,
                       pivot=(_s * (_lens[0] + 2), 0.0, 0.0))
        _b.box(*_blade_box(_s, _lens[1]), "robe_blue")
        _c = SPEC.part(_name + "_c", parent=_b,
                       pivot=(_s * (_lens[1] + 2), 0.0, 0.0))
        _c.box(*_blade_box(_s, _lens[2]), "void_purple")

# ======================================================================
# 3.5 四条手臂（2 长 2 短，持虚空晶刃）
# ======================================================================


def _arm(prefix, parent, pivot, upper, fore, s, mat):
    up = SPEC.part(prefix + "_up", parent=parent, pivot=pivot)
    up.box(-3.0, 0.0, -3.0, 6.0, float(upper), 6.0, mat)
    el = SPEC.part(prefix + "_el", parent=up, pivot=(0.0, float(upper), 0.0))
    el.box(-2.0, 0.0, -2.0, 5.0, float(fore), 5.0, "robe_blue")
    hand = SPEC.part(prefix + "_hand", parent=el, pivot=(0.0, float(fore), 0.0))
    hand.box(-3.0, 0.0, -3.0, 6.0, 4.0, 6.0, "bone")
    # 虚空晶刃（发光）：从手心斜向前下（长 14，不垂到 y=24 以下）
    if s > 0:
        hand.box(1.0, 2.0, -1.0, 2.0, 14.0, 2.0, "crystal", glow=True)
    else:
        hand.box(-3.0, 2.0, -1.0, 2.0, 14.0, 2.0, "crystal", glow=True)
    return up, el, hand


armLlUp, armLlEl, armLlHand = _arm("arm_ll", torso, (14.0, -22.0, 0.0), 14, 12, 1, "void_purple")
armRlUp, armRlEl, armRlHand = _arm("arm_rl", torso, (-14.0, -22.0, 0.0), 14, 12, -1, "void_purple")
armLsUp, armLsEl, armLsHand = _arm("arm_ls", torso, (15.0, -14.0, 4.0), 10, 8, 1, "robe_blue")
armRsUp, armRsEl, armRsHand = _arm("arm_rs", torso, (-15.0, -14.0, 4.0), 10, 8, -1, "robe_blue")

# ======================================================================
# 3.6 悬浮符文石 4 块（root 级独立悬浮，动画绕体公转）
# ======================================================================
_RUNE_POS = (
    (30.0, -50.0, 6.0),
    (-32.0, -40.0, -5.0),
    (24.0, -32.0, 10.0),
    (-26.0, -68.0, 2.0),
)
for _i, (_rx, _ry, _rz) in enumerate(_RUNE_POS):
    _r = SPEC.part("rune%d" % _i, pivot=(_rx, _ry, _rz))
    _r.box(-3.0, -3.0, -2.0, 7.0, 7.0, 5.0, "stone")
    _r.box(-2.0, -2.0, -3.0, 5.0, 5.0, 1.0, "crystal", glow=True)

# ======================================================================
# 3.7 背后虚空环（金环 8 段 + 4 块发光符面）
# ======================================================================
halo = SPEC.part("halo", pivot=(0.0, -56.0, 14.0))
for _i in range(8):
    _ang = _i * (3.141592653589793 / 4.0)
    _cx = 20.0 * math.cos(_ang)
    _cy = 20.0 * math.sin(_ang)
    halo.box(round(_cx - 3.0, 1), round(_cy - 1.0, 1), -1.0, 6.0, 2.0, 2.0, "gold")
haloRunes = SPEC.part("halo_runes", pivot=(0.0, -56.0, 14.0))
for _i in range(4):
    _ang = _i * (3.141592653589793 / 2.0)
    _cx = 20.0 * math.cos(_ang)
    _cy = 20.0 * math.sin(_ang)
    haloRunes.box(round(_cx - 2.0, 1), round(_cy - 2.0, 1), -2.0, 4.0, 4.0, 1.0,
                  "crystal", glow=True)

# ======================================================================
# 4. 动画（全部绝对赋值，禁止 += / *=）
# ======================================================================
SPEC.anim = """
        final float DEG = 0.017453292F;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        int phase = Math.max(1, Math.min(3, entity.bossPhase()));
        float t = ageInTicks;
        float hurt = entity.hurtTime > 0 ? (float) entity.hurtTime / 10.0F : 0.0F;
        float floatY = 2.0F * Mth.sin(t * 0.05F);              // 悬浮上下 ±2px
        float spread = 1.0F + 0.16F * (float) (phase - 1);     // 阶段越高翼/臂张得越开
        float t2 = t * 0.04F;

        // ---- 下摆/布幔/触须：慢摆（错相）----
        this.robe.zRot = 0.010F * Mth.sin(t * 0.025F);
        this.robe.yRot = 0.012F * Mth.sin(t * 0.019F);
        this.hemF.xRot = 0.05F + 0.035F * Mth.sin(t * 0.045F);
        this.hemB.xRot = -0.04F + 0.030F * Mth.sin(t * 0.041F + 1.7F);
        this.hemL.zRot = 0.06F + 0.040F * Mth.sin(t * 0.037F + 0.8F);
        this.hemR.zRot = -0.06F - 0.040F * Mth.sin(t * 0.039F + 2.1F);
        this.tendril0.xRot = 0.12F * Mth.sin(t * 0.060F);
        this.tendril1.xRot = 0.10F * Mth.sin(t * 0.055F + 1.1F);
        this.tendril2.xRot = 0.11F * Mth.sin(t * 0.062F + 2.3F);
        this.tendril3.xRot = 0.09F * Mth.sin(t * 0.051F + 3.4F);
        this.tendril4.xRot = 0.13F * Mth.sin(t * 0.066F + 4.2F);
        this.tendril5.xRot = 0.10F * Mth.sin(t * 0.058F + 5.1F);
        this.tendril0.zRot = 0.06F * Mth.sin(t * 0.047F + 0.3F);
        this.tendril2.zRot = 0.05F * Mth.sin(t * 0.049F + 1.5F);
        this.tentAUp.xRot = 0.10F + 0.08F * Mth.sin(t * 0.042F);
        this.tentATip.xRot = -0.18F + 0.10F * Mth.sin(t * 0.042F + 1.4F);
        this.tentBUp.xRot = 0.08F + 0.07F * Mth.sin(t * 0.038F + 2.0F);
        this.tentBTip.xRot = -0.16F + 0.09F * Mth.sin(t * 0.038F + 3.1F);

        // ---- 上身悬浮（腰以上整体上下 ±2px）+ 呼吸 ----
        this.waist.y = 4.0F + floatY;
        this.waist.xRot = 0.02F * Mth.sin(t * 0.033F) - 0.05F * hurt;
        this.torso.xRot = 0.018F * Mth.sin(t * 0.028F) + 0.030F * Mth.sin(t * 0.021F);
        this.torso.yRot = yaw * 0.18F;
        this.torso.zRot = 0.010F * Mth.sin(t * 0.024F);
        this.pauldronL.xRot = 0.03F * Mth.sin(t * 0.026F);
        this.pauldronR.xRot = 0.03F * Mth.sin(t * 0.026F + 1.6F);

        // ---- 颈/头：跟随视线；独眼眨眼 + 瞳孔移动 ----
        this.neck.xRot = -pitch * 0.15F;
        this.head.yRot = yaw * 0.75F + 0.035F * Mth.sin(t * 0.045F);
        this.head.xRot = -pitch * 0.55F + 0.025F * Mth.sin(t * 0.041F);
        this.head.zRot = 0.020F * Mth.sin(t * 0.033F);
        float blinkCyc = t % 96.0F;
        float blink = blinkCyc < 7.0F
                ? Mth.sin(blinkCyc / 7.0F * (float) Math.PI) : 0.0F;
        this.lidUp.y = -18.0F + 4.5F * blink;
        this.lidDn.y = -8.0F - 3.5F * blink;
        this.lidUp.xScale = 1.0F + 0.06F * blink;
        this.lidDn.xScale = 1.0F + 0.06F * blink;
        this.eye.xScale = 1.0F + 0.05F * Mth.sin(t * 0.055F)
                + 0.09F * (float) (phase - 1);
        this.eye.yScale = 1.0F + 0.04F * Mth.sin(t * 0.048F + 0.7F)
                + 0.06F * (float) (phase - 1);
        this.pupil.x = 2.8F * Mth.sin(t * 0.023F);
        this.pupil.y = -13.0F + 2.0F * Mth.cos(t * 0.019F);
        this.pupil.xScale = 1.0F - 0.06F * Mth.sin(t * 0.031F);
        this.crackA.visible = phase >= 2;
        this.crackA.xScale = 1.0F + 0.15F * (float) (phase - 2);
        this.crackB.visible = phase >= 3;
        this.crackB.xScale = 1.0F + 0.25F * Mth.sin(t * 0.09F);

        // ---- 六片悬浮翼：错相浮动（每片 3 节各自相位）----
        this.wingL0A.zRot = -0.62F * spread + 0.06F * Mth.sin(t2);
        this.wingL0A.xRot = 0.05F * Mth.sin(t * 0.030F + 0.4F);
        this.wingL0B.zRot = 0.05F * Mth.sin(t * 0.045F + 0.9F);
        this.wingL0C.zRot = 0.04F * Mth.sin(t * 0.052F + 1.4F);
        this.wingR0A.zRot = 0.62F * spread - 0.06F * Mth.sin(t2 + 0.8F);
        this.wingR0A.xRot = 0.05F * Mth.sin(t * 0.030F + 1.2F);
        this.wingR0B.zRot = -0.05F * Mth.sin(t * 0.045F + 1.7F);
        this.wingR0C.zRot = -0.04F * Mth.sin(t * 0.052F + 2.2F);
        this.wingL1A.zRot = -0.20F * spread + 0.05F * Mth.sin(t2 + 1.1F);
        this.wingL1A.xRot = 0.04F * Mth.sin(t * 0.034F + 2.0F);
        this.wingL1B.zRot = 0.05F * Mth.sin(t * 0.048F + 2.8F);
        this.wingL1C.zRot = 0.04F * Mth.sin(t * 0.055F + 3.3F);
        this.wingR1A.zRot = 0.20F * spread - 0.05F * Mth.sin(t2 + 1.9F);
        this.wingR1A.xRot = 0.04F * Mth.sin(t * 0.034F + 2.8F);
        this.wingR1B.zRot = -0.05F * Mth.sin(t * 0.048F + 3.6F);
        this.wingR1C.zRot = -0.04F * Mth.sin(t * 0.055F + 4.1F);
        this.wingL2A.zRot = 0.26F * spread + 0.05F * Mth.sin(t2 + 2.2F);
        this.wingL2A.xRot = 0.04F * Mth.sin(t * 0.038F + 3.6F);
        this.wingL2B.zRot = 0.04F * Mth.sin(t * 0.051F + 4.4F);
        this.wingL2C.zRot = 0.03F * Mth.sin(t * 0.058F + 4.9F);
        this.wingR2A.zRot = -0.26F * spread - 0.05F * Mth.sin(t2 + 3.0F);
        this.wingR2A.xRot = 0.04F * Mth.sin(t * 0.038F + 4.4F);
        this.wingR2B.zRot = -0.04F * Mth.sin(t * 0.051F + 5.2F);
        this.wingR2C.zRot = -0.03F * Mth.sin(t * 0.058F + 5.7F);

        // ---- 四条手臂：缓慢张合（长臂主攻、短臂护体）----
        float armOut = 0.16F * spread;
        this.armLlUp.zRot = -armOut + 0.06F * Mth.sin(t * 0.033F);
        this.armLlUp.xRot = 0.06F * Mth.sin(t * 0.028F + 0.5F);
        this.armLlEl.xRot = 0.18F + 0.10F * Mth.sin(t * 0.037F + 1.1F);
        this.armLlHand.xRot = 0.10F * Mth.sin(t * 0.043F + 2.0F);
        this.armRlUp.zRot = armOut + 0.06F * Mth.sin(t * 0.033F + 1.7F);
        this.armRlUp.xRot = 0.06F * Mth.sin(t * 0.028F + 2.2F);
        this.armRlEl.xRot = 0.18F + 0.10F * Mth.sin(t * 0.037F + 2.8F);
        this.armRlHand.xRot = 0.10F * Mth.sin(t * 0.043F + 3.7F);
        this.armLsUp.zRot = -0.10F * spread + 0.05F * Mth.sin(t * 0.036F + 3.0F);
        this.armLsUp.xRot = 0.04F * Mth.sin(t * 0.031F + 4.0F);
        this.armLsEl.xRot = 0.12F + 0.08F * Mth.sin(t * 0.041F + 4.6F);
        this.armLsHand.xRot = 0.06F * Mth.sin(t * 0.047F + 5.2F);
        this.armRsUp.zRot = 0.10F * spread - 0.05F * Mth.sin(t * 0.036F + 4.4F);
        this.armRsUp.xRot = 0.04F * Mth.sin(t * 0.031F + 5.4F);
        this.armRsEl.xRot = 0.12F + 0.08F * Mth.sin(t * 0.041F + 5.8F);
        this.armRsHand.xRot = 0.06F * Mth.sin(t * 0.047F + 6.4F);

        // ---- 悬浮符文石：绕体公转 + 自旋 ----
        float o0 = t * 0.016F;
        this.rune0.x = 30.0F * Mth.cos(o0);
        this.rune0.y = -50.0F + 4.0F * Mth.sin(t * 0.031F);
        this.rune0.z = 6.0F + 30.0F * Mth.sin(o0);
        this.rune0.yRot = t * 0.5F;
        float o1 = t * 0.019F + 2.1F;
        this.rune1.x = 32.0F * Mth.cos(o1);
        this.rune1.y = -40.0F + 5.0F * Mth.sin(t * 0.027F + 1.2F);
        this.rune1.z = -5.0F + 32.0F * Mth.sin(o1);
        this.rune1.yRot = -t * 0.4F;
        float o2 = t * 0.013F + 4.2F;
        this.rune2.x = 24.0F * Mth.cos(o2);
        this.rune2.y = -32.0F + 3.0F * Mth.sin(t * 0.035F + 2.4F);
        this.rune2.z = 10.0F + 24.0F * Mth.sin(o2);
        this.rune2.yRot = t * 0.7F;
        float o3 = t * 0.017F + 5.6F;
        this.rune3.x = 26.0F * Mth.cos(o3);
        this.rune3.y = -68.0F + 3.0F * Mth.sin(t * 0.029F + 3.6F);
        this.rune3.z = 2.0F + 26.0F * Mth.sin(o3);
        this.rune3.yRot = -t * 0.6F;

        // ---- 背后虚空环：慢转 ----
        this.halo.yRot = t * 0.012F;
        this.halo.xRot = 0.08F * Mth.sin(t * 0.020F);
        this.haloRunes.yRot = -t * 0.020F;
        this.haloRunes.xRot = 0.08F * Mth.sin(t * 0.020F);
"""

# 追加成员：renderer 引用 LOWEST_PIXEL_Y（最低点像素 y = 24 → 离地修正 0）。
SPEC.extra = """
    /** 模型最低点像素 y（袍摆触须收束处，浮空基准）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;
"""
