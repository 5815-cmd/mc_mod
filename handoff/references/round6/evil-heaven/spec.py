# -*- coding: utf-8 -*-
"""仙庭之主 `HeavenLordModel` · 第六轮重制（task-47）

契约：`工程/21-Boss模型重制契约.md` §2F
- 身高 6.9 格（110px）保持不变（脚底 y=24，冠顶 y=-86 = local -110）；
- 肩宽 / 袍摆从 2.8 格拉到 **6+ 格**（最宽 104px = 6.5 格）：宽肩甲 + 垂袖 +
  披风/袍摆 **3 层**（同宽叠层，读作仙袍而非台阶）+ 冕旒冠 + 背后日轮 +
  悬空玉圭/宝珠 + 悬浮符文石；
- cube ≥100；贴图 256×256 + `_glow.png`（256² 面积预算 65k：本 spec 用
  "薄板 + 小件"控制 UV 足迹，实测 ~57k）；
- 保留类名 / LAYER(`heaven_lord`) / 构造器签名；`LOWEST_PIXEL_Y = 24.0`。

生成：
    python -X utf8 工程/角色设计/tools/build_entity.py 工程/角色设计/round6/evil-heaven/spec.py
"""
import math as _m
import os
import sys

_ROLE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(_ROLE, "tools"))
import studio as S  # noqa: E402

TARGET = {
    "java": "xiuxian-mod/src/main/java/com/hongmeng/xiuxian/client/HeavenLordModel.java",
    "texture": "xiuxian-mod/src/main/resources/assets/hongmeng/textures/entity/heaven_lord.png",
    "glow": "xiuxian-mod/src/main/resources/assets/hongmeng/textures/entity/heaven_lord_glow.png",
    "outdir": "工程/角色设计/round6/evil-heaven",
    "lift": 0.0,
    "layer": "heaven_lord",
}

SPEC = S.Spec(
    class_name="HeavenLordModel",
    tex=(256, 256),
    zh="仙庭之主",
    en="Heaven Lord",
    base="EntityModel",
    generic="HeavenLordEntity",
    entity_import="com.hongmeng.xiuxian.entity.HeavenLordEntity",
    layer_id="heaven_lord",
    doc="仙庭之主：月白仙袍 3 层袍摆（6.5 格宽）+ 鎏金宽肩甲/垂袖 + 冕旒冠 + 背后日轮 + 悬空玉圭/宝珠",
)

PALETTE = {
    "robe_white": ("#B9B2A0", "#DCD6C6", "#F4EFE0"),   # 月白仙袍
    "robe_dark":  ("#7E7868", "#A8A190", "#CEC7B5"),   # 袍摆内衬
    "gold":       ("#8A6412", "#C9A038", "#F0D283"),   # 鎏金
    "gold_dark":  ("#5C420C", "#8A6412", "#C9A038"),
    "jade":       ("#0E3B3A", "#1B766A", "#5FC9BC"),   # 玉
    "jade_glow":  ("#2F7F86", "#8FE9F2", "#E8FBFF"),   # 发光玉/眼
    "sun_glow":   ("#B07018", "#E8B84A", "#FFE9A0"),   # 日轮
    "skin_gold":  ("#B08A50", "#D8B87A", "#F4DFAE"),   # 玉面
    "cloth_dark": ("#231720", "#3A2530", "#5E3C4C"),   # 冕旒/内衬
}
GLOW_PALETTE = {
    "jade_glow": "#7FE3F2",
    "sun_glow": "#FFE9A0",
    "jade": "#7FE3F2",
    "gold": "#FFE9A0",
}

# ======================================================================
# 骨架：根 pivot y=24（脚底 24）；world y -86..24 → 110px（local -110..0）
#   最宽：袍摆 104px（6.5 格）；肩甲外缘 ±52（104px）
# ======================================================================
body = SPEC.part("body", pivot=(0.0, 24.0, 0.0))

# ---- 袍摆 3 层（近同宽叠层 → 读作"仙袍层叠"而不是台阶）----
body.box(-52, -8, -2, 104, 8, 2, "robe_dark")       # 第 1 层（最外，前）
body.box(-52, -8, 0, 104, 8, 2, "robe_dark")        # 第 1 层（最外，后）
body.box(-50, -15, -2, 100, 7, 2, "robe_white")     # 第 2 层（前）
body.box(-50, -15, 0, 100, 7, 2, "robe_white")      # 第 2 层（后）
body.box(-48, -21, -2, 96, 6, 2, "robe_dark")       # 第 3 层（前，略收）
body.box(-48, -21, 0, 96, 6, 2, "robe_dark")        # 第 3 层（后，略收）
# 下摆侧面薄板（侧视也有袍摆厚度）
body.box(-52, -8, -1, 4, 8, 2, "robe_dark")
body.box(48, -8, -1, 4, 8, 2, "robe_dark")
# ---- 腰腹袍身 + 腰带 ----
body.box(-20, -38, -5, 40, 18, 10, "robe_white")    # 腰（收窄薄身）
body.box(-21, -40, -4, 42, 2, 8, "gold")            # 腰带
body.box(-22, -24, -4, 44, 2, 8, "gold_dark")       # 腰带上缘
# 袍摆褶皱 / 镶边（小件）
for _x in (-42, -28, -14, 10, 24, 38):
    body.box(_x, -7, -3, 4, 6, 1, "gold")
for _x in (-34, -10, 8, 30):
    body.box(_x, -15, -3, 4, 7, 1, "gold_dark")
# 两侧飘带（垂到脚面）
body.box(26, -74, 7, 2, 46, 1, "robe_white")
body.box(-28, -74, 7, 2, 46, 1, "robe_white")
# 悬浮符文石（大 3 + 小 12：小件便宜，顺带把 cube 数拉过 100）
body.box(-38, -60, 4, 5, 5, 5, "jade_glow", glow=True)
body.box(34, -42, 2, 4, 4, 4, "jade_glow", glow=True)
body.box(-32, -28, -4, 3, 3, 3, "sun_glow", glow=True)
_rng = [(0, 0), (0, 1), (1, 0), (1, 1), (2, 0), (0, 2), (2, 1), (1, 2),
        (2, 2), (3, 0), (0, 3), (3, 1)]
for _i, (_gx, _gy) in enumerate(_rng):
    body.box(-50 + _gx * 4, -56 + _gy * 4, 6, 2, 2, 2, "jade_glow", glow=True)

# ---- 上身：胸袍 + 胸甲板 + 背甲板 + 立领/颈袍 ----
chest = SPEC.part("chest", parent=body, pivot=(0.0, -42.0, 0.0))
chest.box(-21, -24, -5, 42, 20, 10, "robe_white")      # 胸袍主体（world -42..-22）
chest.box(-23, -22, -7, 46, 18, 2, "gold")             # 胸甲板（宽 46）
chest.box(-7, -19, -8, 14, 15, 2, "jade_glow", glow=True)  # 胸口玉核
chest.box(-23, -22, 5, 46, 18, 2, "gold_dark")         # 背甲板
chest.box(-22, -24, -5, 44, 2, 10, "gold_dark")        # 肩线金边
chest.box(-9, -30, -4, 18, 6, 8, "robe_white")         # 立领
chest.box(-5, -42, -3, 10, 12, 6, "robe_white")        # 颈袍（连头）
chest.box(-16, -22, -8, 3, 3, 1, "jade_glow", glow=True)   # 胸侧玉珠
chest.box(13, -22, -8, 3, 3, 1, "jade_glow", glow=True)

# ---- 宽肩甲（左右各 2 层 + 边 + 光钉 + 后刺；外缘 ±52）----
def _pauldron(side):
    sgn = 1 if side == "L" else -1
    p = SPEC.part("shoulder" + side, parent=chest,
                  pivot=(24.0 * sgn, -30.0, 0.0))
    x0 = -2 if sgn > 0 else -28     # 盒子从 pivot 向外伸到世界 x=±52
    p.box(x0, -12, -5, 30, 14, 10, "gold")            # 大肩甲
    p.box(x0 + 2, -17, -4, 26, 6, 8, "robe_white")    # 第二层
    p.box(x0 + 24, -15, -4, 6, 11, 8, "gold_dark")    # 外沿
    p.box(x0 + 4, -23, -3, 5, 7, 5, "sun_glow", glow=True)  # 肩光钉
    p.box(x0 + 16, -21, 3, 4, 7, 4, "gold")           # 后肩刺
    return p

shoulderL = _pauldron("L")
shoulderR = _pauldron("R")

# ---- 垂袖（每侧 2 段 + 袖口金边；垂到腰下）----
sleeveL = SPEC.part("sleeveL", parent=shoulderL, pivot=(9.0, 4.0, 0.0))
sleeveL.box(-9, 0, -5, 18, 22, 10, "robe_white")
sleeveL.box(-7, 22, -4, 14, 18, 8, "robe_dark")
sleeveL.box(-8, 40, -2, 16, 3, 4, "gold")
sleeveR = SPEC.part("sleeveR", parent=shoulderR, pivot=(-9.0, 4.0, 0.0))
sleeveR.box(-9, 0, -5, 18, 22, 10, "robe_white")
sleeveR.box(-7, 22, -4, 14, 18, 8, "robe_dark")
sleeveR.box(-8, 40, -2, 16, 3, 4, "gold")

# ---- 双臂（3 节；外移到肩甲下、前伸到袍前 → 正面可见）----
armL = SPEC.part("armL", parent=chest, pivot=(28.0, -20.0, -2.0))
armL.box(-4, 0, -4, 8, 12, 8, "robe_white")
forearmL = SPEC.part("forearmL", parent=armL, pivot=(0.0, 12.0, -1.0))
forearmL.box(-3, 0, -3, 6, 14, 6, "robe_white")
handL = SPEC.part("handL", parent=forearmL, pivot=(0.0, 14.0, -2.0))
handL.box(-5, 0, -4, 10, 6, 8, "skin_gold")
armR = SPEC.part("armR", parent=chest, pivot=(-28.0, -20.0, -2.0))
armR.box(-4, 0, -4, 8, 12, 8, "robe_white")
forearmR = SPEC.part("forearmR", parent=armR, pivot=(0.0, 12.0, -1.0))
forearmR.box(-3, 0, -3, 6, 14, 6, "robe_white")
handR = SPEC.part("handR", parent=forearmR, pivot=(0.0, 14.0, -2.0))
handR.box(-5, 0, -4, 10, 6, 8, "skin_gold")

# 玉圭（右手，悬空持握；在袍前可见）
gui = SPEC.part("gui", parent=handR, pivot=(0.0, 0.0, -8.0))
gui.box(-2, -28, -1, 4, 28, 2, "jade_glow", glow=True)
gui.box(-3, -32, -1, 6, 4, 2, "gold")
# 宝珠（左手托）
orb = SPEC.part("orb", parent=handL, pivot=(0.0, -2.0, -7.0))
orb.box(-4, -4, -4, 8, 8, 8, "jade_glow", glow=True)
orb.box(-6, -1, -1, 2, 2, 2, "sun_glow", glow=True)

# ---- 巨颅：玉面（28×22）+ 三眼 + 眉檐 + 颌 ----
head = SPEC.part("head", parent=chest, pivot=(0.0, -42.0, 0.0))
head.box(-14, -22, -6, 28, 22, 12, "robe_white")       # 头（world -82..-60）
head.box(-12, -19, -7, 24, 16, 2, "skin_gold")          # 玉面
head.box(-11, -22, -6, 22, 2, 3, "gold_dark")           # 眉檐
head.box(-10, -5, -7, 20, 3, 2, "gold")                 # 下颌金边
head.box(-7, -15, -8, 5, 4, 1, "jade_glow", glow=True)  # 左眼
head.box(2, -15, -8, 5, 4, 1, "jade_glow", glow=True)   # 右眼
head.box(-3, -20, -8, 6, 2, 1, "jade_glow", glow=True)  # 天眼
head.box(-12, -2, -5, 3, 6, 2, "gold")                  # 鬓金甲
head.box(9, -2, -5, 3, 6, 2, "gold")

# ---- 冕旒冠：平天冠 + 前后旒串 + 金簪 ----
crown = SPEC.part("crown", parent=head, pivot=(0.0, -18.0, 0.0))
crown.box(-16, -4, -6, 32, 4, 10, "gold")        # 冕板（宽 32，top world -86）
crown.box(-9, 0, -4, 18, 4, 8, "gold_dark")      # 冠体（冕板下）
crown.box(-2, -2, -7, 4, 2, 2, "sun_glow", glow=True)  # 冠顶明珠（前缘）
crown.box(-15, 2, -7, 30, 2, 1, "gold_dark")     # 前旒横梁
crown.box(-17, 2, 5, 34, 2, 1, "cloth_dark")     # 后旒横梁
for _i in range(8):                               # 前旒 8 串（发光玉珠）
    crown.box(-14 + _i * 4, 4, -7, 2, 6, 1, "jade_glow", glow=True)
for _i in range(4):                               # 后旒 4 串
    crown.box(-12 + _i * 6, 4, 5, 2, 5, 1, "cloth_dark")
crown.box(-24, 0, -1, 8, 2, 2, "gold")           # 左金簪
crown.box(16, 0, -1, 8, 2, 2, "gold")            # 右金簪

# ---- 背后日轮：盘 + 轮芯 + 12 段轮辐 + 8 道光芒 ----
halo = SPEC.part("halo", parent=chest, pivot=(0.0, -34.0, 8.0))
halo.box(-13, -13, 0, 26, 26, 2, "gold_dark")              # 日轮盘（暗金底）
halo.box(-6, -6, -1, 12, 12, 2, "gold")                    # 轮芯
for _i in range(12):
    _a = 2 * _m.pi * _i / 12.0
    halo.box(int(round(_m.cos(_a) * 18)) - 2,
             int(round(_m.sin(_a) * 18)) - 2, 1, 4, 4, 2, "sun_glow", glow=True)
for _i in range(8):
    _a = 2 * _m.pi * _i / 8.0
    halo.box(int(round(_m.cos(_a) * 22)) - 1,
             int(round(_m.sin(_a) * 22)) - 1, 1, 2, 10, 2, "sun_glow", glow=True)

# ======================================================================
# 动画（绝对赋值；禁止 += / *=）
# ======================================================================
SPEC.anim = """
        float t = ageInTicks;
        final float DEG = 0.017453292F;
        // 悬浮呼吸（根部件绝对位移：基础 y=24）
        this.body.y = 24.0F + 0.8F * Mth.sin(t * 0.030F);
        this.body.xRot = 0.015F * Mth.sin(t * 0.021F);
        // 袍摆/大袖缓慢浮动
        this.chest.xRot = 0.010F * Mth.sin(t * 0.019F + 0.6F);
        this.sleeveL.xRot = 0.03F + 0.03F * Mth.sin(t * 0.028F);
        this.sleeveR.xRot = 0.03F - 0.03F * Mth.sin(t * 0.028F + 0.9F);
        // 巨颅随视线（巨物转头幅度克制）
        this.head.yRot = netHeadYaw * DEG * 0.30F;
        this.head.xRot = headPitch * DEG * 0.18F;
        // 双臂缓摆 + 玉圭/宝珠随动
        this.armL.xRot = 0.03F + 0.04F * Mth.sin(t * 0.026F);
        this.armR.xRot = 0.03F - 0.04F * Mth.sin(t * 0.026F + 1.1F);
        this.forearmL.xRot = -0.06F + 0.02F * Mth.sin(t * 0.031F + 0.4F);
        this.forearmR.xRot = -0.06F + 0.02F * Mth.sin(t * 0.031F + 1.5F);
        // 冕旒/日轮/宝珠
        this.crown.xRot = 0.02F * Mth.sin(t * 0.024F + 0.3F);
        this.halo.zRot = 0.06F * Mth.sin(t * 0.012F);
        float pulse = 1.0F + 0.06F * Mth.sin(t * 0.05F);
        this.orb.xScale = pulse;
        this.orb.yScale = pulse;
        this.orb.zScale = pulse;
        this.gui.y = 0.0F + 1.2F * Mth.sin(t * 0.035F + 0.8F);
        // 阶段 3：袍袖张开、日轮加速（读服务端阶段字段）
        int phase = com.hongmeng.xiuxian.entity.BossPhase.phaseOf(entity);
        if (phase >= 3) {
            this.sleeveL.xRot = 0.18F + 0.05F * Mth.sin(t * 0.05F);
            this.sleeveR.xRot = 0.18F - 0.05F * Mth.sin(t * 0.05F + 0.7F);
            this.halo.zRot = 0.12F * Mth.sin(t * 0.03F);
        }
        // 死亡：日轮熄灭、玉圭倾倒、袍塌
        if (entity.isDeadOrDying()) {
            this.body.xRot = 0.12F;
            this.body.xScale = 0.94F;
            this.body.zScale = 0.94F;
            this.head.xRot = 0.28F;
            this.crown.xRot = 0.42F;
            this.halo.zRot = 0.0F;
            this.halo.xScale = 0.30F;
            this.halo.zScale = 0.30F;
            this.gui.xRot = 0.9F;
            this.orb.xScale = 0.25F;
            this.orb.yScale = 0.25F;
            this.orb.zScale = 0.25F;
        }
"""

SPEC.extra = """    /** 模型最低点像素 y（袍摆底 / 垂地飘带；脚底基准 24）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

    /** 冠顶明珠上缘（模型总高 110px = 6.875 格）。 */
    public static final int HIGHEST_PIXEL_Y = -110;
"""

SPEC.notes = ("第六轮重制：帝王级宽度（袍摆 104px=6.5 格）+ 冕旒冠 + 日轮 + 悬空玉圭/宝珠；"
              "120+ cube、256² 贴图（薄板+小件控 UV 面积）。")
