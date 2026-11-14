# -*- coding: utf-8 -*-
"""焚天炎帝 → **熔岩泰坦**（Blaze Emperor / Lava Titan）· 第六轮 Boss 重制 B

设计真源：`工程/21-Boss模型重制契约.md` §2B（灾变风：大体量 / 层叠甲板 / 发光内芯 / 夸张肩甲巨臂）。
一句话：黑曜石外壳的熔岩巨神——**胸口 28×28 发光熔炉**、**层叠巨型肩甲+骨刺**、**双臂 3 节+巨拳**、
**牛角冠+火焰鬃毛+发光眼**、**背部排气管+熔岩喷口**；远看=肩宽 4.5 格的熔炉巨人。

尺寸（1 格=16px，+y 向下，-z 正前；脚底 y=24）：
  站高 **96px = 6.0 格**（蹄底 24 → 角尖 -72）；
  肩宽 **72px = 4.5 格**（肩甲外缘 x=±36）；躯干深 12px、含背包/鬃毛约 24px；
  cube **约 95（≥80）**，贴图 **256×256** + `_glow.png`。

UV 预算说明：256²=65536 纹素，box 足迹 = (2d+2w)(d+h)（≈表面积+2d²），深度和超大盒子最贵。
本稿按「剪影优先、细节靠贴图」把总足迹压到约 5 万，保证 pack_uv 装得下
（build_entity 的装箱是逐格贪心 + 1px 间隙，256² 装不下 118 个盒子，故减深/减件）。

坐标系：部件 pivot 用「模型空间绝对坐标」，子件 pivot 相对父件 pivot；box 坐标相对所属 pivot。
"""

import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_TOOLS = os.path.normpath(os.path.join(_HERE, "..", "..", "tools"))
sys.path.insert(0, _TOOLS)
import studio as S  # noqa: E402

# ======================================================================
# 1. 输出目标（整文件重写现有模型/贴图；renderer/实体/LAYER 不改）
# ======================================================================
TARGET = dict(
    java=r"xiuxian-mod\src\main\java\com\hongmeng\xiuxian\client\BlazeEmperorModel.java",
    texture=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\blaze_emperor.png",
    glow=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\blaze_emperor_glow.png",
    outdir=r"工程\角色设计\round6\blaze",
    lift=0.0,
)

# ======================================================================
# 2. 色板（深 / 中 / 亮 三阶）
# ======================================================================
PALETTE = {
    "obsidian":      ("#07070C", "#12121B", "#22202F"),   # 黑曜石外壳（主料）
    "obsidian_dark": ("#040406", "#0B0B12", "#17151F"),   # 阴影面/裙甲
    "lava_dark":     ("#2A0A04", "#4E1407", "#7A2410"),   # 暗红熔岩肉
    "lava":          ("#5A1A05", "#9E3208", "#D2550C"),   # 熔岩橙
    "gold":          ("#5A3E0E", "#9C741C", "#E0B84A"),   # 金饰边
    "bone":          ("#8E8676", "#C4BCA8", "#EDE6D4"),   # 骨刺/獠牙/角
    "iron":          ("#101015", "#22222B", "#3C3C48"),   # 排气管/格栅/面甲
    # 发光底材（本体暗色，真正发光在 glow 层）
    "core_glow":     ("#3A1A04", "#7A3604", "#C4600A"),
    "vent_glow":     ("#301404", "#6A2A04", "#A84C08"),
    "eye_glow":      ("#4A2404", "#8A4404", "#D07A10"),
    "mane_glow":     ("#4A1804", "#8A2E08", "#C84A10"),
    "crack_glow":    ("#3A1004", "#6E1E06", "#A8360C"),
    "pipe_glow":     ("#2E1604", "#623004", "#A85408"),
}
GLOW_PALETTE = {
    "core_glow":  "#FFD24A",   # 胸核最亮
    "vent_glow":  "#FF9A22",   # 散热口
    "eye_glow":   "#FFE066",   # 眼睛
    "mane_glow":  "#FF7A18",   # 火焰鬃毛
    "crack_glow": "#FF5A14",   # 关节裂缝
    "pipe_glow":  "#FFB040",   # 排气管口
}

# ======================================================================
# 3. Spec
# ======================================================================
SPEC = S.Spec(
    class_name="BlazeEmperorModel",
    tex=(256, 256),
    zh="焚天炎帝",
    en="Blaze Emperor · Lava Titan",
    base="EntityModel",
    generic="CultivationMonsterEntity",
    entity_import="com.hongmeng.xiuxian.entity.CultivationMonsterEntity",
    layer_id=None,   # LAYER 定义在 BlazeEmperorRenderer，这里不重复
    doc="灾变风熔岩泰坦：黑曜石重甲 + 胸口 28x28 发光熔炉 + 层叠巨肩甲/骨刺 + "
        "双臂三节巨拳 + 牛角冠/火焰鬃毛/发光眼 + 背部排气管与熔岩喷口。",
)

# ======================================================================
# 4. 部件（模型空间 y：蹄底 24 → 角尖 -72；x：肩甲外缘 ±36）
# ======================================================================

# ---------- 4.1 下半身：髋 / 腰甲 / 粗腿蹄足 ----------
hips = SPEC.part("hips", pivot=(0.0, 4.0, 0.0))              # 模型 y -2..4
hips.box(-14.0, -6.0, -7.0, 28, 6, 12, "obsidian")
hips.box(-13.0, -5.0, -7.0, 26, 2, 12, "lava_dark")          # 骨盆熔岩缝

waist = SPEC.part("waist", parent=hips, pivot=(0.0, -6.0, 0.0))   # 模型 y=-2
waist.box(-16.0, -4.0, -8.0, 32, 4, 10, "obsidian")          # 腰甲裙板 y -6..-2
waist.box(-12.0, -8.0, -9.0, 24, 4, 4, "lava_dark")          # 前裙板（垂到 y -10）
waist.box(-17.0, -3.0, -2.0, 2, 3, 1, "gold")                # 左金边
waist.box(15.0, -3.0, -2.0, 2, 3, 1, "gold")                 # 右金边


def build_leg(side):
    """粗腿 + 蹄足（side=1 左 / -1 右）。脚底严格 y=24。"""
    tag = "L" if side > 0 else "R"
    thigh = SPEC.part("thigh" + tag, parent=hips, pivot=(12.0 * side, 0.0, 0.0))
    thigh.box(-8.0, 0.0, -5.0, 16, 8, 10, "obsidian")         # 大腿 y 4..13
    thigh.box(-8.0, 4.0, -6.0, 16, 3, 8, "lava")        # 膝甲 y 8..11

    shin = SPEC.part("shin" + tag, parent=thigh, pivot=(0.0, 9.0, 0.0))   # 模型 y=13
    shin.box(-7.0, 0.0, -5.0, 14, 7, 10, "obsidian")          # 小腿 y 13..20
    shin.box(-7.0, 2.0, -5.5, 14, 2, 1, "gold")              # 胫甲金边
    shin.box(-2.0, 3.0, -7.5, 4, 3, 1, "crack_glow", glow=True)   # 小腿裂缝

    hoof = SPEC.part("hoof" + tag, parent=shin, pivot=(0.0, 7.0, 0.0))    # 模型 y=20
    hoof.box(-9.0, 0.0, -7.0, 18, 4, 14, "obsidian")
    hoof.box(-9.0, 1.0, -8.0, 18, 2, 1, "gold")             # 蹄前金边          # 蹄 y 20..24
    hoof.box(-9.0, 0.0, -9.5, 4, 3, 2, "lava_dark")           # 蹄尖裂口（内）
    hoof.box(5.0, 0.0, -9.5, 4, 3, 2, "lava_dark")            # 蹄尖裂口（外）
    return thigh, shin, hoof


build_leg(1)
build_leg(-1)

# ---------- 4.2 躯干 + 胸口熔炉 + 散热口 ----------
torso = SPEC.part("torso", parent=hips, pivot=(0.0, -6.0, 0.0))   # 模型 y=-2
torso.box(-20.0, -28.0, -6.0, 40, 28, 10, "obsidian")        # 胸腔 y -30..-2
torso.box(-18.0, -32.0, -5.0, 36, 4, 8, "lava_dark")        # 斜方肌/颈基 y -34..-30
torso.box(-20.0, -28.0, -7.0, 40, 2, 1, "gold")              # 上胸金边
torso.box(-20.0, -6.0, -7.0, 40, 2, 1, "gold")               # 下胸金边
torso.box(-22.0, -26.0, -5.0, 2, 18, 2, "obsidian_dark")    # 左侧肋甲
torso.box(20.0, -26.0, -5.0, 2, 18, 2, "obsidian_dark")     # 右侧肋甲
torso.box(-22.0, -22.0, -6.0, 1, 8, 1, "crack_glow", glow=True)   # 左肋裂缝
torso.box(21.0, -22.0, -6.0, 1, 8, 1, "crack_glow", glow=True)    # 右肋裂缝

# 胸口熔炉：28×28 发光格栅 + 铁框 + 两道栅条
furnace = SPEC.part("furnace", parent=torso, pivot=(0.0, -14.0, -6.0))  # 模型 (0,-16,-6)
furnace.box(-14.0, -13.0, -2.0, 28, 28, 2, "core_glow", glow=True)      # 28×28 发光格栅
furnace.box(-15.0, -14.0, -2.5, 30, 2, 2, "iron")             # 上框
furnace.box(-15.0, 12.0, -2.5, 30, 2, 2, "iron")              # 下框
furnace.box(-15.0, -14.0, -2.5, 2, 28, 2, "iron")             # 左框
furnace.box(13.0, -14.0, -2.5, 2, 28, 2, "iron")              # 右框
furnace.box(-14.0, -5.0, -3.0, 28, 2, 1, "iron")              # 栅条 1
furnace.box(-14.0, 3.0, -3.0, 28, 2, 1, "iron")               # 栅条 2

for side in (1, -1):
    tag = "L" if side > 0 else "R"
    v = SPEC.part("vent" + tag, parent=torso, pivot=(16.0 * side, -14.0, -6.0))
    v.box(-3.0, -10.0, -2.0, 6, 20, 2, "vent_glow", glow=True)   # 散热口 y -26..-6
    v.box(-3.0, -8.0, -3.0, 6, 1, 1, "iron")
    v.box(-3.0, 4.0, -3.0, 6, 1, 1, "iron")

# ---------- 4.3 层叠巨型肩甲 + 骨刺（外缘 x=±36 → 肩宽 72）----------
for side in (1, -1):
    tag = "L" if side > 0 else "R"
    pa = SPEC.part("pauldron" + tag, parent=torso,
                   pivot=(22.0 * side, -26.0, 0.0))          # 模型 (±22,-28,0)
    if side > 0:
        pa.box(-4.0, 0.0, -6.0, 18, 7, 10, "obsidian")       # 第一层甲 x 18..36
        pa.box(-4.0, -1.0, -5.0, 18, 2, 4, "gold")          # 金脊（外缘金边）
    else:
        pa.box(-14.0, 0.0, -6.0, 18, 7, 10, "obsidian")
        pa.box(-14.0, -1.0, -5.0, 18, 2, 4, "gold")

    pa2 = SPEC.part("pauldron" + tag + "2", parent=pa,
                    pivot=(0.0, 7.0, 0.0))                   # 模型 (±22,-21,0)
    if side > 0:
        pa2.box(-2.0, 0.0, -5.0, 10, 4, 10, "lava_dark")     # 第二层甲 x 20..30
        pa2.box(2.0, -10.0, -3.0, 3, 10, 3, "bone")          # 骨刺 1（向上）
        pa2.box(-3.0, -8.0, 4.0, 3, 8, 3, "bone")            # 骨刺 2（向后）
    else:
        pa2.box(-8.0, 0.0, -5.0, 10, 4, 10, "lava_dark")
        pa2.box(-5.0, -10.0, -3.0, 3, 10, 3, "bone")
        pa2.box(0.0, -8.0, 4.0, 3, 8, 3, "bone")

# ---------- 4.4 双臂三节 + 巨拳（24×24×20）+ 4 指（合并两两成块）----------
for side in (1, -1):
    tag = "L" if side > 0 else "R"
    a = SPEC.part("arm" + tag, parent=torso, pivot=(23.0 * side, -22.0, 0.0))  # 模型 (±22,-24,0)
    a.box(-6.0, 0.0, -4.0, 12, 15, 8, "obsidian")           # 上臂 y -24..-8
    a.box(-7.0, 2.0, -4.0, 14, 3, 8, "lava_dark")           # 肘部熔岩环
    a.box(-1.0, 5.0, -6.0, 2, 6, 1, "crack_glow", glow=True)  # 臂缝

    f = SPEC.part("fore" + tag, parent=a, pivot=(0.0, 16.0, 0.0))           # 模型 (±22,-8,0)
    f.box(-6.0, 0.0, -4.0, 12, 8, 8, "lava_dark")           # 前臂 y -8..0
    f.box(-7.0, 2.0, -4.5, 14, 2, 1, "gold")                # 前臂金边
    f.box(-2.0, 3.0, -6.5, 4, 3, 1, "crack_glow", glow=True)

    fist = SPEC.part("fist" + tag, parent=f, pivot=(0.0, 8.0, 0.0))         # 模型 (±22,0,0)
    fist.box(-12.0, 0.0, -9.0, 24, 24, 18, "obsidian")      # 巨拳 24×24×20（拳底 y=24）
    fist.box(-12.0, 17.0, -9.5, 24, 2, 1, "gold")           # 拳箍金边
    fist.box(-12.0, 1.0, -9.5, 24, 5, 1, "iron")            # 指节铁板
    fist.box(-10.0, 3.0, -10.0, 5, 8, 1, "crack_glow", glow=True)   # 拳面熔岩裂纹
    fist.box(5.0, 3.0, -10.0, 5, 8, 1, "crack_glow", glow=True)

    fi = SPEC.part("fingers" + tag, parent=fist, pivot=(0.0, 13.0, -6.0))   # 模型 (±22,13,-6)
    fi.box(-12.0, 0.0, -5.0, 11, 5, 6, "lava_dark")          # 外侧两指（合并）
    fi.box(1.0, 0.0, -5.0, 11, 5, 6, "lava_dark")            # 内侧两指（合并）
    fi.box(-12.0 if side > 0 else 10.0, 1.0, -3.0, 2, 4, 5, "obsidian")  # 拇指

# ---------- 4.5 头：牛角冠 / 火焰鬃毛 / 发光眼 / 獠牙面甲 ----------
head = SPEC.part("head", parent=torso, pivot=(0.0, -32.0, 0.0))   # 模型 y=-34
head.box(-10.0, -18.0, -6.0, 20, 18, 12, "obsidian")         # 颅 y -52..-34
head.box(-11.0, -16.0, -8.0, 22, 3, 2, "lava_dark")          # 眉骨
head.box(-9.0, -14.0, -8.5, 18, 10, 2, "iron")               # 面甲
head.box(-8.0, -12.0, -9.0, 5, 3, 1, "eye_glow", glow=True)  # 左眼
head.box(3.0, -12.0, -9.0, 5, 3, 1, "eye_glow", glow=True)   # 右眼
head.box(-9.0, -5.0, -8.5, 3, 5, 2, "bone")                  # 獠牙（左）
head.box(6.0, -5.0, -8.5, 3, 5, 2, "bone")                   # 獠牙（右）

# 下颌（可开合）
jaw = SPEC.part("jaw", parent=head, pivot=(0.0, -4.0, -6.0))  # 模型 (0,-38,-6)
jaw.box(-8.0, 0.0, -3.0, 16, 3, 8, "lava_dark")
jaw.box(-7.0, 1.0, -4.0, 14, 1, 1, "bone")                   # 下排牙

# 火焰鬃毛（发光，头后/两侧）
mane = SPEC.part("mane", parent=head, pivot=(0.0, -8.0, 6.0))  # 模型 (0,-42,6)
mane.box(-13.0, -10.0, 1.0, 6, 14, 4, "mane_glow", glow=True)   # 左鬃
mane.box(7.0, -10.0, 1.0, 6, 14, 4, "mane_glow", glow=True)     # 右鬃
mane.box(-6.0, -16.0, 2.0, 12, 14, 4, "mane_glow", glow=True)   # 中鬃（更高）

# 牛角冠（左右各 3 节；角尖 y = -72 → 总高 96px = 6.0 格）
for side in (1, -1):
    tag = "L" if side > 0 else "R"
    hn = SPEC.part("horn" + tag, parent=head, pivot=(7.0 * side, -16.0, 0.0))  # 模型 (±7,-50,0)
    if side > 0:
        hn.box(-2.0, -8.0, -2.0, 5, 8, 5, "bone")            # y -58..-50
        hn.box(-2.0, -15.0, -1.0, 4, 7, 4, "bone")           # y -65..-58
        hn.box(-1.5, -22.0, -0.5, 3, 7, 3, "bone")           # 角尖 y -72..-65
        hn.box(2.0, -13.0, -1.0, 2, 5, 2, "gold")            # 角环
    else:
        hn.box(-3.0, -8.0, -2.0, 5, 8, 5, "bone")
        hn.box(-2.0, -15.0, -1.0, 4, 7, 4, "bone")
        hn.box(-1.5, -22.0, -0.5, 3, 7, 3, "bone")
        hn.box(-4.0, -13.0, -1.0, 2, 5, 2, "gold")

# 中央冠冕（牛角冠中轴）
crown = SPEC.part("crown", parent=head, pivot=(0.0, -16.0, 2.0))   # 模型 (0,-50,2)
crown.box(-2.5, -16.0, -1.5, 5, 16, 4, "gold")
crown.box(-1.5, -19.0, -0.5, 3, 3, 3, "bone")

# ---------- 4.6 背部：排气管 / 熔岩喷口 / 尖刺 ----------
back = SPEC.part("back", parent=torso, pivot=(0.0, -2.0, 6.0))     # 模型 (0,-4,6)
back.box(-10.0, -14.0, 0.0, 20, 14, 6, "iron")               # 背包 y -18..-2
back.box(-7.0, -32.0, 2.0, 4, 16, 4, "iron")                 # 左排气管 y -34..-16
back.box(3.0, -32.0, 2.0, 4, 16, 4, "iron")                  # 右排气管
back.box(-7.0, -34.0, 2.0, 4, 2, 4, "pipe_glow", glow=True)  # 左管口
back.box(3.0, -34.0, 2.0, 4, 2, 4, "pipe_glow", glow=True)   # 右管口
back.box(-5.0, -8.0, 6.0, 10, 7, 3, "pipe_glow", glow=True)  # 熔岩喷口
back.box(-11.0, -10.0, 5.0, 3, 10, 3, "bone")                # 背刺（左）
back.box(8.0, -10.0, 5.0, 3, 10, 3, "bone")                  # 背刺（右）

# ---------- 4.7 腰链 + 挂炉 ----------
chain = SPEC.part("chainL", parent=hips, pivot=(14.0, -6.0, -6.0))  # 模型 (14,-2,-6)
chain.box(-1.0, 0.0, -1.0, 2, 4, 2, "iron")
chain.box(-1.0, 3.0, -1.0, 2, 4, 2, "iron")
chain.box(-1.0, 6.0, -1.0, 2, 4, 2, "iron")
chain.box(-4.0, 9.0, -4.0, 8, 8, 6, "lava_dark")             # 挂炉
chain.box(-2.5, 11.0, -5.0, 5, 4, 1, "core_glow", glow=True)  # 炉窗口

# ======================================================================
# 5. 动画（全部绝对赋值，禁止 += / *=）
# ======================================================================
SPEC.anim = r"""
        final float DEG = 0.017453292F;
        float t = ageInTicks;
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float atk = entity.getAttackAnim(0.0F);
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);
        int phase = Math.max(1, entity.bossPhase());          // 1..3
        float rage = phase >= 3 ? 1.0F : (phase == 2 ? 0.6F : 0.3F);

        // ================================================================
        // 1. 呼吸 / 站立基准：整身缓慢起伏，行走时另加步频
        // ================================================================
        float breathe = Mth.sin(t * 0.045F);
        float stepBob = walk * 1.2F * Mth.sin(limbSwing * 0.9F);
        this.hips.y = 4.0F + 0.8F * breathe + stepBob;
        this.hips.zRot = 0.02F * Mth.sin(t * 0.031F) - 0.04F * hurt;
        this.hips.xRot = 0.03F * walk * Mth.sin(limbSwing * 0.9F + 1.5708F);

        // ================================================================
        // 2. 躯干：熔炉呼吸（xScale/zScale 脉动）+ 受击后仰 + 死亡塌陷
        // ================================================================
        float breath = 1.0F + (0.020F + 0.012F * rage) * Mth.sin(t * 0.06F);
        this.torso.xScale = breath * (1.0F - 0.05F * hurt) * (1.0F + 0.06F * dead);
        this.torso.zScale = this.torso.xScale;
        this.torso.xRot = 0.02F * Mth.sin(t * 0.033F) + 0.10F * hurt + 0.35F * dead;
        this.torso.yRot = 0.02F * Mth.sin(t * 0.027F);
        this.torso.y = -6.0F + 0.5F * breathe;

        // ================================================================
        // 3. 胸口熔炉：核心呼吸脉动（阶段越高越亮/越大）
        // ================================================================
        float corePulse = 1.0F + (0.045F + 0.030F * rage) * Mth.sin(t * 0.09F)
                + 0.10F * hurt;
        float coreScale = corePulse * (1.0F - 0.55F * dead);
        this.furnace.xScale = coreScale;
        this.furnace.yScale = coreScale;
        this.furnace.zScale = coreScale;
        this.ventL.xScale = 1.0F + 0.05F * Mth.sin(t * 0.11F);
        this.ventR.xScale = 1.0F + 0.05F * Mth.sin(t * 0.11F + 1.5708F);

        // ================================================================
        // 4. 头部环视 + 下颌 + 鬃毛/冠：慢速环视，攻击时低头
        // ================================================================
        this.head.xRot = headPitch * 0.30F * DEG + 0.04F * Mth.sin(t * 0.021F)
                + 0.08F * hurt + 0.20F * dead + 0.10F * atk;
        this.head.yRot = netHeadYaw * 0.40F * DEG + 0.22F * Mth.sin(t * 0.017F);
        this.head.zRot = 0.03F * Mth.sin(t * 0.023F);
        this.jaw.xRot = 0.06F + 0.18F * Mth.sin(t * 0.05F) + 0.25F * atk + 0.10F * hurt;
        this.mane.xRot = 0.05F * Mth.sin(t * 0.04F) + 0.10F * dead;
        this.mane.zRot = 0.04F * Mth.sin(t * 0.033F);
        this.crown.y = -16.0F + 0.3F * Mth.sin(t * 0.05F);

        // ================================================================
        // 5. 双臂：待机微摆 → 攻击展开（zRot）+ 握拳（拳缩放 + 手指内扣）
        // ================================================================
        float swing = Mth.sin(atk * (float) Math.PI);          // 0→1→0
        float clench = Mth.sin(atk * (float) Math.PI * 1.6F);
        this.armL.zRot = 0.05F * Mth.sin(t * 0.04F) - (0.70F + 0.15F * rage) * swing;
        this.armR.zRot = 0.05F * Mth.sin(t * 0.04F + 1.5708F) + (0.70F + 0.15F * rage) * swing;
        this.armL.xRot = 0.04F * Mth.sin(t * 0.037F) + 0.18F * atk + 0.10F * hurt;
        this.armR.xRot = 0.04F * Mth.sin(t * 0.037F + 1.5708F) + 0.18F * atk + 0.10F * hurt;
        this.foreL.xRot = -0.05F * walk * Mth.sin(limbSwing * 0.9F) - 0.25F * atk;
        this.foreR.xRot = 0.05F * walk * Mth.sin(limbSwing * 0.9F) - 0.25F * atk;
        float clenchScale = 1.0F - 0.10F * clench - 0.20F * dead;
        this.fistL.xScale = clenchScale;
        this.fistL.zScale = clenchScale;
        this.fistR.xScale = clenchScale;
        this.fistR.zScale = clenchScale;
        this.fingersL.xRot = 0.90F * clench + 0.25F * dead;
        this.fingersR.xRot = 0.90F * clench + 0.25F * dead;

        // ================================================================
        // 6. 肩甲：轻微上下 + 攻击时后张（层叠甲板错相）
        // ================================================================
        this.pauldronL.y = -26.0F + 0.6F * Mth.sin(t * 0.05F) - 0.8F * swing;
        this.pauldronR.y = -26.0F + 0.6F * Mth.sin(t * 0.05F + 1.5708F) - 0.8F * swing;
        this.pauldronL.zRot = 0.03F * Mth.sin(t * 0.045F) - 0.10F * swing;
        this.pauldronR.zRot = 0.03F * Mth.sin(t * 0.045F + 1.5708F) + 0.10F * swing;
        this.pauldronL2.y = 7.0F + 0.4F * Mth.sin(t * 0.05F + 0.6F);
        this.pauldronR2.y = 7.0F + 0.4F * Mth.sin(t * 0.05F + 2.2F);
        this.pauldronL2.zRot = 0.04F * Mth.sin(t * 0.043F + 0.8F);
        this.pauldronR2.zRot = 0.04F * Mth.sin(t * 0.043F + 2.5F);

        // ================================================================
        // 7. 腿：粗腿慢步（幅度小，重量感）；死亡下蹲
        // ================================================================
        this.thighL.xRot = 0.10F * walk * Mth.sin(limbSwing * 0.9F) + 0.12F * dead;
        this.thighR.xRot = 0.10F * walk * Mth.sin(limbSwing * 0.9F + (float) Math.PI)
                + 0.12F * dead;
        this.shinL.xRot = -0.08F * walk * Mth.sin(limbSwing * 0.9F) - 0.10F * dead;
        this.shinR.xRot = -0.08F * walk * Mth.sin(limbSwing * 0.9F + (float) Math.PI)
                - 0.10F * dead;
        this.hoofL.xRot = 0.05F * walk * Mth.sin(limbSwing * 0.9F + 0.5F);
        this.hoofR.xRot = 0.05F * walk * Mth.sin(limbSwing * 0.9F + (float) Math.PI + 0.5F);

        // ================================================================
        // 8. 背部排气管/喷口 + 腰链：炉气脉动、链条摆动
        // ================================================================
        this.back.xRot = 0.02F * Mth.sin(t * 0.04F) + 0.05F * hurt;
        this.back.yScale = 1.0F + 0.02F * Mth.sin(t * 0.07F);
        this.chainL.zRot = 0.10F * Mth.sin(t * 0.05F)
                + 0.20F * walk * Mth.sin(limbSwing * 0.9F);
        this.chainL.xRot = 0.03F * Mth.sin(t * 0.043F);
        this.waist.xScale = 1.0F + 0.015F * Mth.sin(t * 0.06F);
        this.waist.zScale = this.waist.xScale;
"""

# ======================================================================
# 6. 追加成员（renderer/审计需要的基准常量）
# ======================================================================
SPEC.extra = """
    /** 模型最低点像素 y（蹄足底面，正好踩地：24 → 世界 0；renderer GROUND_LIFT=0）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

    /** 最高点像素 y（牛角冠角尖）。模型总高 96px = 6.0 格。 */
    public static final double HIGHEST_PIXEL_Y = -72.0;

    /** 肩宽（像素）：肩甲外缘 ±36 → 72px = 4.5 格。 */
    public static final double SHOULDER_WIDTH_PX = 72.0;
"""
