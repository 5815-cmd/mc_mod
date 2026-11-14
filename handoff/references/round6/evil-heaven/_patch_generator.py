# -*- coding: utf-8 -*-
"""一次性补丁：把 tools/gen_evil_phases.py 的
  1) 图集装箱（shelf → NFDH，让 3 套骨架 + 新上半身塞进 1024²）
  2) UPPER 部件表（→ 巨神上半身，≥120 cube）
  3) anim_upper 增加肩甲/垂袖摆动
换掉。只改这三段，ARM/HEAD 表与其余逻辑不动。

用法：python 工程/角色设计/round6/evil-heaven/_patch_generator.py
"""
import io
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(HERE))))
GEN = os.path.join(WS, "tools", "gen_evil_phases.py")

NEW_PACK = '''# 2. 图集装箱（1024×1024 单图集）
# ======================================================================
#: 装箱策略：**NFDH（按高度降序的货架装箱）+ 2px 间隔**。
#: 第五轮是"按 parts 表顺序"装，第三阶段扩到 120+ 个盒子后高度会超 1024；
#: 按高度降序能显著提高利用率（三套骨架 + 新上身实测 < 900 行）。
#: 注意：装箱顺序只决定 UV 坐标，**不改变 parts 表顺序**（贴图 seed/动画仍按表序）。
def pack_atlas(all_parts, size=SIZE, gutter=GUTTER):
    items = []
    for (_cls, _title, built) in all_parts:
        for p in built:
            bx = p["box"]
            w, h, d = int(round(bx[3])), int(round(bx[4])), int(round(bx[5]))
            uw, vh = box_uv(w, h, d)
            items.append((vh, uw, p))
    items.sort(key=lambda it: (-it[0], -it[1]))
    x = y = row_h = 0
    for (vh, uw, p) in items:
        if x + uw > size:
            x = 0
            y += row_h + gutter
            row_h = 0
        if y + vh > size:
            raise RuntimeError(
                "atlas full（1024² 装不下：第 %s 个盒子需要 y=%d+%d；"
                "请减小盒子或检查 NFDH 装箱）" % (p["name"], y, vh))
        p["uv"] = (x, y)
        x += uw + gutter
        row_h = max(row_h, vh)
    used = y + row_h
    print("atlas 使用高度 %d / %d（NFDH 装箱，%d 个盒子）" % (used, size, len(items)))
    return used


pack_atlas(all_parts)
'''

NEW_UPPER = '''# ---------------------------------------------------------------- 三阶段：巨神上半身
# 第六轮重制（task-47）：挤满天空的巨神上半身，不再是"紫色方块墙"
#   * 胸宽 100px（chest 盒 -50..50）；巨肩甲 3 层 + 骨刺，外缘 x≈±80；
#   * 双巨臂各 3 节（上臂 60 / 前臂 58 / 巨掌 26）+ 5 爪（垂到 y=102）+ 臂上骨刺/眼；
#   * 巨颅：主眼+瞳孔、上下颚 8 獠牙、8 小眼、双角（2 段）；
#   * 外露肋骨 8 根（左右各 4）+ 胸骨 2；
#   * 全身眼 ≥20（头 8+主眼 + 躯 4 + 胸 4 + 肩 2 + 臂 6）；
#   * 背部裂口触须 3×2 + 背骨刺 6 + 脊板 4；
#   * 总高：minY=-152（角尖）/ maxY=102（爪尖）= 254px = 15.9 格（≤265 上限）。
UPPER = [
    # ---- 腰腹（上半身最下端，y 世界 -50..6）----
    ("torso", None, (0, 6, 0), (-30, -56, -22, 60, 56, 44), "flesh"),
    # ---- 巨胸：宽 100px ----
    ("chest", "torso", (0, -56, 0), (-50, -34, -30, 100, 34, 60), "flesh"),
    ("chestPlate", "chest", (0, -4, -32), (-48, -26, -4, 96, 26, 4), "plate"),
    ("chestPlateTop", "chest", (0, -34, 0), (-44, -6, -26, 88, 6, 52), "plate"),
    ("backPlate", "chest", (0, 0, 30), (-42, -30, -4, 84, 30, 4), "plate"),
    ("collar", "chest", (0, -36, 0), (-30, -6, -24, 60, 6, 48), "plate"),
    ("neck", "chest", (0, -30, 0), (-18, -12, -18, 36, 12, 36), "flesh"),
    # ---- 外露肋骨 8 根（左右各 4，横过胸甲前）----
    ("sternumL", "chest", (-4, -30, -34), (-4, 0, -2, 8, 30, 4), "rib"),
    ("sternumR", "chest", (4, -30, -34), (-4, 0, -2, 8, 30, 4), "rib"),
    # ---- 肩甲：每侧 3 层 + 2 骨刺 + 1 眼，外缘 x≈±80 ----
    ("padL0", "chest", (46, -30, 0), (-2, -8, -18, 22, 20, 36), "plate"),
    ("padL1", "padL0", (10, -10, 0), (-4, -10, -16, 20, 22, 32), "plate"),
    ("padL2", "padL1", (8, -8, 0), (-5, -8, -14, 16, 18, 28), "plate"),
    ("spikePL0", "padL1", (18, 2, -8), (-4, -14, -4, 8, 16, 8), "bone"),
    ("spikePL1", "padL2", (16, 0, -6), (-3, -10, -3, 6, 12, 6), "bone"),
    ("eyePL", "padL0", (8, -22, -18), (-5, -5, -3, 10, 10, 3), "eye"),
    ("padR0", "chest", (-46, -30, 0), (-20, -8, -18, 22, 20, 36), "plate"),
    ("padR1", "padR0", (-10, -10, 0), (-16, -10, -16, 20, 22, 32), "plate"),
    ("padR2", "padR1", (-8, -8, 0), (-11, -8, -14, 16, 18, 28), "plate"),
    ("spikePR0", "padR1", (-18, 2, -8), (-4, -14, -4, 8, 16, 8), "bone"),
    ("spikePR1", "padR2", (-16, 0, -6), (-3, -10, -3, 6, 12, 6), "bone"),
    ("eyePR", "padR0", (2, -22, -18), (-5, -5, -3, 10, 10, 3), "eye"),
    # ---- 垂袖（从肩甲下沿垂下，2 段）----
    ("sleeveL", "padL0", (2, 20, 0), (-16, 0, -16, 32, 16, 32), "plate"),
    ("sleeveL2", "sleeveL", (0, 16, 0), (-14, 0, -14, 28, 20, 28), "plate"),
    ("sleeveR", "padR0", (-2, 20, 0), (-16, 0, -16, 32, 16, 32), "plate"),
    ("sleeveR2", "sleeveR", (0, 16, 0), (-14, 0, -14, 28, 20, 28), "plate"),
    # ---- 双巨臂：3 节 + 5 爪（爪尖 y=102）----
    ("armL", "chest", (56, -16, 0), (-17, 0, -17, 34, 60, 34), "flesh"),
    ("forearmL", "armL", (0, 60, 0), (-15, 0, -15, 30, 58, 30), "crack"),
    ("handL", "forearmL", (0, 58, 0), (-20, 0, -20, 40, 26, 40), "flesh"),
    ("armR", "chest", (-56, -16, 0), (-17, 0, -17, 34, 60, 34), "flesh"),
    ("forearmR", "armR", (0, 60, 0), (-15, 0, -15, 30, 58, 30), "crack"),
    ("handR", "forearmR", (0, 58, 0), (-20, 0, -20, 40, 26, 40), "flesh"),
    # 臂上骨刺（每侧 3）+ 臂上眼（每侧 3）
    ("spikeAL0", "armL", (17, 12, 0), (0, -5, -5, 10, 10, 10), "bone"),
    ("spikeAL1", "armL", (17, 30, 0), (0, -4, -4, 8, 8, 8), "bone"),
    ("spikeAL2", "forearmL", (15, 24, 0), (0, -4, -4, 8, 8, 8), "bone"),
    ("spikeAR0", "armR", (-17, 12, 0), (-10, -5, -5, 10, 10, 10), "bone"),
    ("spikeAR1", "armR", (-17, 30, 0), (-8, -4, -4, 8, 8, 8), "bone"),
    ("spikeAR2", "forearmR", (-15, 24, 0), (-8, -4, -4, 8, 8, 8), "bone"),
    ("eyeAL", "armL", (0, 22, -18), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeAL2", "forearmL", (0, 20, -16), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeAL3", "handL", (0, 10, -21), (-4, -4, -2, 8, 8, 2), "eye"),
    ("eyeAR", "armR", (0, 22, -18), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeAR2", "forearmR", (0, 20, -16), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeAR3", "handR", (0, 10, -21), (-4, -4, -2, 8, 8, 2), "eye"),
    # ---- 巨颅（裂口中的头）----
    ("head", "neck", (0, -12, 0), (-32, -34, -26, 64, 34, 52), "flesh"),
    ("cranium", "head", (0, -34, 0), (-28, -16, -24, 56, 16, 48), "bone"),
    ("browL", "cranium", (-24, -2, -22), (-8, -6, -4, 8, 6, 8), "bone"),
    ("browR", "cranium", (24, -2, -22), (-8, -6, -4, 8, 6, 8), "bone"),
    # 上下颚 + 口腔 + 8 獠牙
    ("upperJaw", "head", (0, -2, -20), (-28, 0, -24, 56, 14, 44), "flesh"),
    ("lowerJaw", "upperJaw", (0, 14, 0), (-24, 0, -22, 48, 12, 40), "flesh"),
    ("maw", "upperJaw", (0, 12, -4), (-22, 0, -16, 44, 10, 32), "maw"),
    ("fangU0", "upperJaw", (-18, 14, -20), (-4, 0, -4, 8, 16, 8), "claw"),
    ("fangU1", "upperJaw", (-6, 14, -22), (-4, 0, -4, 8, 19, 8), "claw"),
    ("fangU2", "upperJaw", (6, 14, -22), (-4, 0, -4, 8, 19, 8), "claw"),
    ("fangU3", "upperJaw", (18, 14, -20), (-4, 0, -4, 8, 16, 8), "claw"),
    ("fangL0", "lowerJaw", (-16, 12, -18), (-4, -10, -4, 8, 10, 8), "claw"),
    ("fangL1", "lowerJaw", (-5, 12, -20), (-4, -8, -4, 8, 8, 8), "claw"),
    ("fangL2", "lowerJaw", (5, 12, -20), (-4, -8, -4, 8, 8, 8), "claw"),
    ("fangL3", "lowerJaw", (16, 12, -18), (-4, -10, -4, 8, 10, 8), "claw"),
    # 主眼 + 瞳孔
    ("mainEye", "head", (0, -10, -26), (-15, -15, -3, 30, 30, 3), "eye"),
    ("pupil", "mainEye", (0, 0, -3), (-6, -6, -2, 12, 12, 2), "pupil"),
    # 8 小眼：面部 4 + 颅顶 2 + 两侧 2
    ("eyeH0", "head", (-24, -28, -24), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH1", "head", (24, -28, -24), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH2", "head", (-24, -8, -24), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH3", "head", (24, -8, -24), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH4", "cranium", (-14, -10, -22), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH5", "cranium", (14, -10, -22), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH6", "head", (-30, -18, 2), (0, -5, -5, 3, 10, 10), "eye"),
    ("eyeH7", "head", (30, -18, 2), (-3, -5, -5, 3, 10, 10), "eye"),
    # 双角（各 2 段，尖 y≈-152）
    ("hornL1", "cranium", (22, -10, 0), (0, -10, -5, 8, 10, 10), "horn"),
    ("hornL2", "hornL1", (6, -10, 0), (0, -6, -4, 6, 6, 8), "horn"),
    ("hornR1", "cranium", (-22, -10, 0), (-8, -10, -5, 8, 10, 10), "horn"),
    ("hornR2", "hornR1", (-6, -10, 0), (-6, -6, -4, 6, 6, 8), "horn"),
    # 脸颊骨刺（2）
    ("cheekL", "head", (30, -4, -6), (0, -4, -4, 10, 8, 8), "bone"),
    ("cheekR", "head", (-30, -4, -6), (-10, -4, -4, 10, 8, 8), "bone"),
    # ---- 躯干/胸上的眼（8）----
    ("eyeT0", "torso", (-18, -30, -24), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeT1", "torso", (18, -30, -24), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeT2", "torso", (-18, -8, -24), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeT3", "torso", (18, -8, -24), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeC0", "chest", (-30, -24, -33), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeC1", "chest", (30, -24, -33), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeC2", "chest", (-14, -14, -34), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeC3", "chest", (14, -14, -34), (-5, -5, -3, 10, 10, 3), "eye"),
    # ---- 背部：裂口触须 3×2 + 背骨刺 6 + 脊板 4 ----
    ("tentB0", "chest", (-24, -30, 30), (-5, 0, -5, 10, 26, 10), "blood"),
    ("tentB02", "tentB0", (0, 26, 0), (-4, 0, -4, 8, 22, 8), "blood"),
    ("tentB1", "chest", (0, -34, 30), (-5, 0, -5, 10, 30, 10), "blood"),
    ("tentB12", "tentB1", (0, 30, 0), (-4, 0, -4, 8, 24, 8), "blood"),
    ("tentB2", "chest", (24, -30, 30), (-5, 0, -5, 10, 26, 10), "blood"),
    ("tentB22", "tentB2", (0, 26, 0), (-4, 0, -4, 8, 22, 8), "blood"),
    ("spikeB0", "chest", (-40, -34, 26), (0, -8, -6, 12, 16, 12), "bone"),
    ("spikeB1", "chest", (40, -34, 26), (-12, -8, -6, 12, 16, 12), "bone"),
    ("spikeB2", "chest", (-30, -40, 28), (0, -8, -5, 10, 16, 10), "bone"),
    ("spikeB3", "chest", (30, -40, 28), (-10, -8, -5, 10, 16, 10), "bone"),
    ("spikeB4", "chest", (-18, -44, 30), (0, -6, -4, 8, 12, 8), "bone"),
    ("spikeB5", "chest", (18, -44, 30), (-8, -6, -4, 8, 12, 8), "bone"),
    ("spine0", "chest", (0, -34, 34), (-16, -5, -4, 32, 10, 6), "bone"),
    ("spine1", "chest", (0, -26, 34), (-14, -5, -4, 28, 10, 6), "bone"),
    ("spine2", "chest", (0, -18, 34), (-12, -4, -4, 24, 8, 6), "bone"),
    ("spine3", "chest", (0, -10, 34), (-10, -4, -4, 20, 8, 6), "bone"),
]
# 8 根外露肋骨（左右各 4；挂在 chest 前侧，横过胸甲）
for _i in range(4):
    UPPER.append(("ribL%d" % _i, "chest", (-26, -26 + _i * 7, -35),
                  (-18, 0, -2, 36, 4, 4), "rib"))
    UPPER.append(("ribR%d" % _i, "chest", (26, -26 + _i * 7, -35),
                  (-18, 0, -2, 36, 4, 4), "rib"))
# 每侧 5 爪（垂到世界 y=102）
for _side in ("L", "R"):
    for _i in range(5):
        UPPER.append(("claw%s%d" % (_side, _i), "hand" + _side,
                      (-16 + _i * 8, 24, -16), (-3, 0, -6, 6, 26, 12), "claw"))

'''

NEW_ANIM = '''def anim_upper(built):
    L = [ENTRANCE_NOTE,
         "        float t = ageInTicks;",
         "        final float DEG = 0.017453292F;",
         "        // 三阶段：巨神上半身「挤满天空」——躯干呼吸（只动 xScale/zScale）",
         "        float breath = 1.0F + 0.016F * Mth.sin(t * 0.024F);",
         "        this.torso.xScale = breath;",
         "        this.torso.zScale = breath;",
         "        this.torso.xRot = 0.02F * Mth.sin(t * 0.019F);",
         "        this.chest.xRot = 0.015F * Mth.sin(t * 0.017F + 0.7F);",
         "        // 巨颅随视线 + 下颌开合",
         "        this.head.yRot = netHeadYaw * DEG * 0.26F;",
         "        this.head.xRot = headPitch * DEG * 0.18F;",
         "        this.upperJaw.xRot = 0.03F + 0.03F * Mth.sin(t * 0.021F);",
         "        this.lowerJaw.xRot = 0.10F + 0.10F * Mth.sin(t * 0.021F + 0.9F);",
         "        // 主眼脉动",
         "        float eyePulse = 1.0F + 0.05F * Mth.sin(t * 0.033F);",
         "        this.mainEye.xScale = eyePulse;",
         "        this.mainEye.zScale = eyePulse;",
         "        this.pupil.yScale = 1.0F - 0.45F * Mth.clamp("
         "Mth.sin(t * 0.015F) * 2.6F - 2.0F, 0.0F, 1.0F);",
         "        // 巨肩甲/垂袖：极慢沉浮（左右异相，P1-4 后仍保留'双臂张开'的基础姿势）",
         "        this.padL0.zRot = -0.020F + 0.012F * Mth.sin(t * 0.018F);",
         "        this.padR0.zRot = 0.020F - 0.012F * Mth.sin(t * 0.018F + 0.8F);",
         "        this.sleeveL.xRot = 0.02F + 0.02F * Mth.sin(t * 0.021F + 0.5F);",
         "        this.sleeveR.xRot = 0.02F - 0.02F * Mth.sin(t * 0.021F + 1.3F);",
         "        // 双臂异相缓摆 + 张开的基础姿势（正面剪影=巨神张开双臂）",
         "        this.armR.xRot = 0.02F + 0.09F * Mth.sin(t * 0.043F);",
         "        this.armL.xRot = 0.02F - 0.09F * Mth.sin(t * 0.043F + 1.2F);",
         "        this.armR.zRot = 0.18F + 0.03F * Mth.sin(t * 0.031F);",
         "        this.armL.zRot = -0.18F - 0.03F * Mth.sin(t * 0.031F + 0.8F);",
         "        this.forearmR.xRot = -0.20F + 0.04F * Mth.sin(t * 0.047F + 0.5F);",
         "        this.forearmL.xRot = -0.20F + 0.04F * Mth.sin(t * 0.047F + 2.0F);",
         "        this.handR.xRot = -0.10F + 0.03F * Mth.sin(t * 0.055F);",
         "        this.handL.xRot = -0.10F + 0.03F * Mth.sin(t * 0.055F + 1.6F);",
         "        // 双爪抓握（左右异相）"]
    L += grip_lines(["clawR%d" % i for i in range(5)], 0.10, 0.20, 0.068)
    L += grip_lines(["clawL%d" % i for i in range(5)], 0.10, 0.20, 0.068)
    L += ["        // 背部裂口触须（3 根错相）"]
    L += tentacle_lines(["tentB0", "tentB02", "tentB1", "tentB12", "tentB2", "tentB22"])
    L += ["        // 全身眼睛：分件眨眼（巨颅 8 + 主眼 + 躯 4 + 胸 4 + 肩 2 + 臂 6）"]
    L += blink_lines(group_eyes(built, "eye"), 0.4, groups=5, tag="e")
    return "\\n".join(L)

'''


def replace_block(src, start_marker, end_marker, new_text, name):
    i = src.index(start_marker)
    j = src.index(end_marker, i)
    print("替换 %s：%d..%d 字符" % (name, i, j))
    return src[:i] + new_text + src[j:]


def main():
    src = io.open(GEN, encoding="utf-8").read()
    # 1) 装箱段
    src = replace_block(src,
                        "# 2. 图集装箱\n# ======================================================================\natlas = Atlas()",
                        "# ======================================================================\n# 3. 程序化贴图",
                        NEW_PACK + "\n", "图集装箱")
    # 2) UPPER 段
    src = replace_block(src,
                        "# ---------------------------------------------------------------- 三阶段：上半身",
                        "MODELS = [",
                        NEW_UPPER + "\n", "UPPER 部件表")
    # 3) anim_upper
    src = replace_block(src, "def anim_upper(built):", "\nANIMS = {",
                        NEW_ANIM, "anim_upper")
    io.open(GEN, "w", encoding="utf-8", newline="\n").write(src)
    print("已写入", GEN)


if __name__ == "__main__":
    main()
