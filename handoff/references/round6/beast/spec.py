# -*- coding: utf-8 -*-
"""万兽之主（beast_boss）· 灾变风「骨甲巨兽」Spec（第六轮 Boss 重制 C · v5 视觉强化）。

契约：`工程/21-Boss模型重制契约.md` §1 / §2C。
v5 按 Lead 实机渲染反馈再修一轮（正文"米色盒子+四条细腿"→骨甲巨兽）：
  1. 兽首放大并压低前伸：头骨 32×20×16（含吻部/鼻端全长 34px），下颌加宽 22px，
     双角改成**向外后掠**（左右各 3 段、最远 x=±26），发光眼加大 8×4；头整体在肩峰之下。
  2. 前高后低：前肩/驼峰顶顶到高度线，臀线/后段躯干下沉 6~8px；背骨板加高，
     最前面一块做成**外翻尖刺**（左右外翻 + 正前一根前突棘）。
  3. 腿加粗：前肢大腿直径 14→（相对旧版 +4px），爪加大到 4×2×6；后肢同步加粗；尾巴上翘。
  4. 保持：脚底严格 y=24、总高 4.2 格、鼻到尾 9 格、贴图 256×256、UV 0 problems、类 API 不变。
  5. 贴图预算：256×256（旧 192×128）体积有限，所以**只加深/加宽轮廓必需尺寸**，
     深度方向（z）普遍收紧 —— 正视剪影不掉，UV 装得下。

坐标：+y 向下、-z 朝前、1 格 = 16px。盒子按**模型世界坐标**写，`add()` 自动换算局部坐标；
部件 pivot 也按世界坐标写，`part()` 自动换算成相对父件 offset（studio 的 PartPose 是相对的）。
"""
import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_TOOLS = os.path.normpath(os.path.join(_HERE, "..", "..", "tools"))
sys.path.insert(0, _TOOLS)
import studio as S  # noqa: E402

# ----------------------------------------------------------------------
# 1. 输出目标（写死到现有文件，类名不变）
# ----------------------------------------------------------------------
TARGET = dict(
    java=r"xiuxian-mod\src\main\java\com\hongmeng\xiuxian\client\BeastLordModel.java",
    texture=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\boss_beast_lord.png",
    glow=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\boss_beast_lord_glow.png",
    outdir=r"工程\角色设计\round6\beast",
    lift=0.0,
)

# ----------------------------------------------------------------------
# 2. 色板：骨白 / 土褐 / 暗红 三阶 + 绿金发光
# ----------------------------------------------------------------------
PALETTE = {
    "bone":       ("#5E5544", "#948870", "#D6CBB2"),
    "bone_white": ("#6E6552", "#A89C82", "#E4DCC8"),
    "bone_dark":  ("#3E3729", "#625846", "#8A7C62"),
    "hide":       ("#2B1C12", "#46301E", "#6B4A2E"),
    "hide_dark":  ("#1B120C", "#2E2014", "#46311F"),
    "leather":    ("#241A10", "#3A2A18", "#54402A"),
    "horn":       ("#4A3E2C", "#7E6A4C", "#B49A72"),
    "claw":       ("#1C1610", "#33291C", "#4E3E2A"),
    "blood":      ("#2E080C", "#5E141A", "#8E2A32"),
    "eye_glow":   ("#2A3A10", "#7FA82A", "#E8FF7A"),
    "core_glow":  ("#3A3A10", "#8A8A2A", "#E8FF8A"),
    "rib_glow":   ("#2A3A10", "#6FA22A", "#C8F06A"),
    "mouth_glow": ("#3A0A0E", "#7E1420", "#B82A38"),
}

GLOW_PALETTE = {
    "eye_glow": "#C8FF5A",
    "core_glow": "#C8FF6A",
    "rib_glow": "#9AFF4A",
    "mouth_glow": "#FF5A4A",
}

# ----------------------------------------------------------------------
# 3. 骨架（四足：脚底 y=24，顶 -43.4 ≈ 4.2 格；鼻 -76 .. 尾 +68 = 9 格；宽 ±26 = 3.25 格）
# ----------------------------------------------------------------------
SPEC = S.Spec(
    class_name="BeastLordModel",
    tex=(256, 256),
    zh="万兽之主",
    en="BeastLord",
    base="EntityModel",
    generic="CultivationMonsterEntity",
    layer_id=None,                      # LAYER 在 BeastLordRenderer 里，保持不动
    doc="骨甲巨兽：低伏长躯干（前高后低）+ 驼峰 3 排骨板/外翻尖刺 + 巨角兽首（8 獠牙/可开合下颚）"
        " + 肩甲前肢 + 外露肋骨 + 胸口发光核心 + 上翘长尾 4 节/尾锤",
)

PARTS = {}
#: 部件**模型世界坐标** pivot（PartPose 相对父件，这里统一换算）
ABS = {}


def part(name, parent=None, pivot=(0.0, 0.0, 0.0), rot=None):
    abs_p = (float(pivot[0]), float(pivot[1]), float(pivot[2]))
    parent_abs = ABS[parent.name] if parent is not None else (0.0, 0.0, 0.0)
    rel = tuple(round(abs_p[i] - parent_abs[i], 1) for i in range(3))
    p = SPEC.part(name, parent=parent, pivot=rel, rot=rot)
    PARTS[name] = p
    ABS[name] = abs_p
    return p


def add(p, x, y, z, w, h, d, mat, **kw):
    """按**模型世界坐标**加盒子（自动减去部件世界 pivot）。"""
    px, py, pz = ABS[p.name]
    return p.box(round(x - px, 1), round(y - py, 1), round(z - pz, 1),
                 w, h, d, mat, **kw)


# ======================================================================
# 3.1 低伏长躯干（前高后低）：胸前顶 -16 → 腰 -12 → 臀 -8，z -32..+20
# ======================================================================
body = part("body", pivot=(0.0, 0.0, 0.0))
add(body, -16, -16, -32, 32, 20, 12, "hide", detail="pelt")        # 胸块（最高）
add(body, -12, -12, -20, 24, 18, 18, "hide_dark", detail="pelt")   # 腹段（中）
add(body, -14, -8, -2, 28, 14, 18, "hide", detail="pelt")          # 臀部（最低 → 前高后低）

# ======================================================================
# 3.2 颈 → 巨角兽首（头压在肩峰之下：低伏捕食者）
# ======================================================================
neck = part("neck", parent=body, pivot=(0.0, -12.0, -30.0))
add(neck, -7, -20, -44, 14, 12, 12, "hide", detail="pelt")         # 颈

head = part("head", parent=neck, pivot=(0.0, -22.0, -44.0))
add(head, -16, -38, -58, 32, 20, 16, "bone_white", detail="bone")  # 头骨（放大：宽 32 / 长 16）
add(head, -17, -40, -66, 34, 4, 7, "bone", detail="bone")          # 眉骨/冠脊（前伸压眼窝）
add(head, -10, -27, -68, 20, 8, 9, "bone", detail="bone")          # 吻部（加宽 20）
add(head, -8, -30, -76, 16, 6, 7, "bone_dark", detail="bone")      # 鼻端（前伸到 -76）
# 发光眼（加大到 8×4，藏在眉骨下面）
add(head, 7, -34, -62, 8, 4, 2, "eye_glow", glow=True)
add(head, -15, -34, -62, 8, 4, 2, "eye_glow", glow=True)
# 上獠牙 4 颗（从吻部下缘垂下）
for fx in (-8.0, -4.0, 2.0, 6.0):
    add(head, fx, -19, -66, 2, 6, 2, "bone_white", detail="bone")

# 巨角：左右各 3 段，**向外后掠**（最远 x=±26、向后收 z，不朝上戳）
horn_l = part("horn_l", parent=head, pivot=(13.0, -34.0, -52.0))
add(horn_l, 13, -36, -58, 6, 8, 8, "horn")
add(horn_l, 18, -38, -50, 5, 7, 7, "horn")
add(horn_l, 22, -40, -42, 4, 7, 7, "bone_white", detail="bone")

horn_r = part("horn_r", parent=head, pivot=(-13.0, -34.0, -52.0))
add(horn_r, -19, -36, -58, 6, 8, 8, "horn")
add(horn_r, -23, -38, -50, 5, 7, 7, "horn")
add(horn_r, -26, -40, -42, 4, 7, 7, "bone_white", detail="bone")

# ======================================================================
# 3.3 可开合下颚（加宽；独立部件，动画开合）
# ======================================================================
jaw = part("jaw", parent=head, pivot=(0.0, -22.0, -44.0))
add(jaw, -11, -27, -68, 22, 7, 12, "bone", detail="bone")          # 下颌主体（宽 22）
add(jaw, -9, -24, -74, 18, 4, 5, "bone_dark", detail="bone")       # 下颏
# 下獠牙 4 颗（朝上）
for fx in (-9.0, -4.0, 2.0, 7.0):
    add(jaw, fx, -28, -64, 2, 7, 2, "bone_white", detail="bone")
add(jaw, -6, -25, -66, 12, 2, 10, "mouth_glow", glow=True)         # 喉部发光

# ======================================================================
# 3.4 驼峰骨板 3 排（前高后低，最前一块外翻尖刺）—— 剪影主特征
# ======================================================================
hump_front = part("hump_front", parent=body, pivot=(0.0, -14.0, -24.0),
                  rot=(-0.12, 0.0, 0.0))
add(hump_front, -6, -34, -32, 12, 22, 9, "bone", detail="bone")    # 中骨板（最高）
add(hump_front, 7, -30, -32, 8, 18, 9, "bone_dark", detail="bone")     # 右骨板
add(hump_front, -15, -30, -32, 8, 18, 9, "bone_dark", detail="bone")   # 左骨板
# 外翻尖刺：左右两片向外翻 + 正前一根向前突
add(hump_front, 17, -28, -36, 5, 7, 8, "bone_white", detail="bone")
add(hump_front, -22, -28, -36, 5, 7, 8, "bone_white", detail="bone")
add(hump_front, -4, -38, -40, 8, 7, 12, "bone_white", detail="bone")
add(hump_front, -3, -43, -30, 5, 10, 5, "bone_white", detail="bone")   # 中棘刺（顶到高度线）

hump_mid = part("hump_mid", parent=body, pivot=(0.0, -12.0, -10.0),
                rot=(-0.05, 0.0, 0.0))
add(hump_mid, -13, -32, -16, 10, 22, 8, "bone", detail="bone")
add(hump_mid, 2, -32, -16, 10, 22, 8, "bone_dark", detail="bone")
add(hump_mid, -9, -38, -14, 4, 7, 4, "bone_white", detail="bone")
add(hump_mid, 5, -37, -14, 4, 6, 4, "bone_white", detail="bone")

hump_rear = part("hump_rear", parent=body, pivot=(0.0, -8.0, 2.0),
                 rot=(0.06, 0.0, 0.0))
add(hump_rear, -6, -26, -2, 9, 20, 8, "bone", detail="bone")
add(hump_rear, 7, -22, -2, 7, 16, 8, "bone_dark", detail="bone")
add(hump_rear, -14, -22, -2, 7, 16, 8, "bone_dark", detail="bone")
add(hump_rear, -2, -32, 0, 4, 6, 4, "bone_white", detail="bone")

# ======================================================================
# 3.5 外露肋骨（左右各 3 根，发光缝隙）+ 胸口发光核心
# ======================================================================
rib_l = part("rib_l", parent=body, pivot=(-15.0, -6.0, -20.0))
rib_r = part("rib_r", parent=body, pivot=(15.0, -6.0, -20.0))
for i in range(3):
    add(rib_l, -19, -17 + i * 6, -24 + i * 5, 4, 6, 8, "rib_glow",
        glow=True, detail="bone")
    add(rib_r, 15, -17 + i * 6, -24 + i * 5, 4, 6, 8, "rib_glow",
        glow=True, detail="bone")

core = part("core", parent=body, pivot=(0.0, -6.0, -33.0))
add(core, -6, -8, -35, 12, 11, 3, "core_glow", glow=True)          # 发光核心
add(core, -8, -11, -36, 16, 3, 3, "bone_white", detail="bone")     # 上骨框
add(core, -8, 2, -36, 16, 2, 3, "bone_white", detail="bone")       # 下骨框
add(core, -9, -8, -36, 3, 11, 3, "bone", detail="bone")            # 左骨框
add(core, 6, -8, -36, 3, 11, 3, "bone", detail="bone")             # 右骨框

# ======================================================================
# 3.6 前肢：肩甲 + 上/下腿 + 爪 3 指（腿加粗 diam +4、爪加大）
# ======================================================================
shoulder_l = part("shoulder_l", parent=body, pivot=(-14.0, -10.0, -28.0))
add(shoulder_l, -26, -24, -36, 12, 18, 10, "bone", detail="bone")   # 肩甲大板（外挑到 3.2 格肩宽）
add(shoulder_l, -24, -30, -33, 4, 7, 4, "bone_white", detail="bone")  # 肩甲棘刺

shoulder_r = part("shoulder_r", parent=body, pivot=(14.0, -10.0, -28.0))
add(shoulder_r, 14, -24, -36, 12, 18, 10, "bone", detail="bone")
add(shoulder_r, 20, -30, -33, 4, 7, 4, "bone_white", detail="bone")

leg_fl = part("leg_fl", parent=body, pivot=(-15.0, -10.0, -26.0))
add(leg_fl, -23, -10, -30, 14, 16, 9, "hide", detail="pelt")        # 上腿（加粗）
add(leg_fl, -21, 6, -28, 11, 13, 7, "hide_dark", detail="pelt")     # 下腿
add(leg_fl, -23, 19, -32, 14, 5, 10, "bone_dark", detail="bone")    # 足
for cx in (-22.0, -17.0, -12.0):
    add(leg_fl, cx, 22, -38, 4, 2, 6, "claw")                       # 爪（加大）

leg_fr = part("leg_fr", parent=body, pivot=(15.0, -10.0, -26.0))
add(leg_fr, 7, -10, -30, 14, 16, 9, "hide", detail="pelt")
add(leg_fr, 9, 6, -28, 11, 13, 7, "hide_dark", detail="pelt")
add(leg_fr, 7, 19, -32, 14, 5, 10, "bone_dark", detail="bone")
for cx in (8.0, 13.0, 18.0):
    add(leg_fr, cx, 22, -38, 4, 2, 6, "claw")

# ======================================================================
# 3.7 后肢：豹式大腿 / 小腿 / 足 / 爪 2 趾（加粗）
# ======================================================================
leg_bl = part("leg_bl", parent=body, pivot=(-15.0, -8.0, 16.0))
add(leg_bl, -22, -8, 16, 15, 18, 12, "hide", detail="pelt")         # 大腿
add(leg_bl, -21, 10, 18, 11, 10, 8, "hide_dark", detail="pelt")     # 小腿
add(leg_bl, -22, 20, 16, 15, 4, 9, "bone_dark", detail="bone")      # 足
for cx in (-21.0, -16.0):
    add(leg_bl, cx, 20, 10, 4, 4, 6, "claw")

leg_br = part("leg_br", parent=body, pivot=(15.0, -8.0, 16.0))
add(leg_br, 7, -8, 16, 15, 18, 12, "hide", detail="pelt")
add(leg_br, 10, 10, 18, 11, 10, 8, "hide_dark", detail="pelt")
add(leg_br, 7, 20, 16, 15, 4, 9, "bone_dark", detail="bone")
for cx in (10.0, 15.0):
    add(leg_br, cx, 20, 10, 4, 4, 6, "claw")

# ======================================================================
# 3.8 上翘长尾 4 节 + 尾刺 / 尾锤（鼻 -76 .. 尾 +68 = 144px = 9 格）
# ======================================================================
tail_1 = part("tail_1", parent=body, pivot=(0.0, -6.0, 16.0))
add(tail_1, -6, -10, 16, 12, 12, 14, "hide", detail="pelt")
tail_2 = part("tail_2", parent=tail_1, pivot=(0.0, -8.0, 30.0))
add(tail_2, -5, -14, 30, 10, 10, 10, "hide_dark", detail="pelt")
add(tail_2, -1, -20, 33, 2, 6, 2, "bone_white", detail="bone")     # 尾刺 1
tail_3 = part("tail_3", parent=tail_2, pivot=(0.0, -10.0, 40.0))
add(tail_3, -4, -18, 40, 8, 8, 10, "hide", detail="pelt")
add(tail_3, 0, -23, 43, 2, 5, 2, "bone_white", detail="bone")      # 尾刺 2
tail_4 = part("tail_4", parent=tail_3, pivot=(0.0, -12.0, 50.0))
add(tail_4, -3, -22, 50, 6, 6, 7, "hide_dark", detail="pelt")
tail_blade = part("tail_blade", parent=tail_4, pivot=(0.0, -14.0, 57.0))
add(tail_blade, -5, -26, 57, 10, 10, 11, "bone", detail="bone")    # 尾锤骨甲（到 +68）
add(tail_blade, -1, -32, 59, 2, 6, 2, "bone_white", detail="bone") # 尾刺 3

# ======================================================================
# 4. 动画（绝对赋值；呼吸 / 头低伏再抬 / 尾横扫 / 下颚开合）
# ======================================================================
SPEC.anim = """
        float yaw = netHeadYaw * ((float) Math.PI / 180.0F);
        float pitch = headPitch * ((float) Math.PI / 180.0F);
        float breath = Mth.sin(ageInTicks * 0.08F);
        float walk = limbSwingAmount;

        // ---- 四足步态：前后对角腿同相（兽类慢跑）----
        float stepA = Mth.cos(limbSwing * 0.6F) * walk;
        float stepB = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * walk;
        this.legFl.xRot = stepA * 0.55F;
        this.legBr.xRot = stepA * 0.55F;
        this.legFr.xRot = stepB * 0.55F;
        this.legBl.xRot = stepB * 0.55F;

        // ---- 呼吸：胸腔起伏 + 驼峰骨板随呼吸轻抬（绝对赋值）----
        this.body.xRot = breath * 0.025F;
        this.body.y = breath * 0.6F;
        this.humpFront.xRot = -0.12F + breath * 0.045F;
        this.humpMid.xRot = -0.05F + breath * 0.04F;
        this.humpRear.xRot = 0.06F + breath * 0.035F;
        this.core.xScale = 1.0F + breath * 0.10F;
        this.core.yScale = 1.0F + breath * 0.10F;
        this.core.zScale = 1.0F + breath * 0.10F;

        // ---- 头：低头嗅地 → 再抬起（低频），叠加玩家视角 ----
        float bow = Mth.sin(ageInTicks * 0.045F);
        this.neck.xRot = -0.16F + bow * 0.22F + pitch * 0.30F;
        this.neck.yRot = yaw * 0.45F;
        this.head.xRot = 0.12F - bow * 0.20F + pitch * 0.65F;
        this.head.yRot = yaw * 0.85F;

        // ---- 下颚开合：呼吸 + 偶尔的咆哮（双频叠加）----
        float roar = Mth.sin(ageInTicks * 0.13F) * Mth.sin(ageInTicks * 0.037F);
        this.jaw.xRot = 0.10F + breath * 0.10F + Math.max(0.0F, roar) * 0.55F;
        this.hornL.xRot = this.head.xRot * 0.35F;
        this.hornR.xRot = this.head.xRot * 0.35F;

        // ---- 尾：上翘 + 横扫（4 节相位延迟，末端甩得更狠）----
        float wag = ageInTicks * 0.075F;
        this.tail1.yRot = Mth.sin(wag) * 0.20F;
        this.tail2.yRot = Mth.sin(wag - 0.7F) * 0.26F;
        this.tail3.yRot = Mth.sin(wag - 1.4F) * 0.30F;
        this.tail4.yRot = Mth.sin(wag - 2.1F) * 0.34F;
        this.tailBlade.yRot = Mth.sin(wag - 2.8F) * 0.38F;
        this.tail1.xRot = -0.10F + breath * 0.03F;
        this.tail2.xRot = -0.14F + breath * 0.03F;
        this.tailBlade.xRot = -0.18F + breath * 0.10F;
"""

# 追加成员：renderer 的 GROUND_LIFT 依赖它（studio 不产出，手动补）
SPEC.extra = """    /** 最低点像素 y（四只爪子/尾锤都不低于 24）；BeastLordRenderer 用它算 GROUND_LIFT。 */
    public static final double LOWEST_PIXEL_Y = 24.0;
"""
