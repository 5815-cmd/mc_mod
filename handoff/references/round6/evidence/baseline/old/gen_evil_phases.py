"""「恶」的**三套独立骨架**（按阶段切换 —— 第五轮升级版）。

## 为什么不是一套骨架改显隐
玩家明确要「三套」：一阶段只有一条手臂从云里垂下，二阶段头探进来，
三阶段整个上半身挤满天空。**体量差距**本身就是压迫感的来源，
靠"隐藏部件"做不出来 —— 那只是同一具身体的零件开关。

## 三套骨架（第五轮扩容）
| 骨架 | 阶段 | 内容 | cube | 锚点（模型空间 y） |
|---|---|---|---|---|
| `EvilArmModel`   | 1 | 从云里垂下的巨臂：肩甲→上臂→肘→前臂→巨掌五指爪 + 12 眼 + 骨刺 + 3 触须 | ≥26 | 顶 -54 / 底 144（±15% 内） |
| `EvilHeadModel`  | 2 | 裂口巨颅：主眼+瞳孔+10 小眼 + 上下颚 8 獠牙 + 双角 + 后颅触须 + 一条臂 | ≥30 | 顶 -90 / 底 96 |
| `EvilUpperModel` | 3 | 挤满天空的上半身：宽肩躯干+胸甲肋骨 + 双巨臂各 3 段+爪 + 巨颅 + 24 眼 + 背部触须/骨刺 | ≥48 | 顶 -148 / 底 102 |

## 坐标 / 锚点约定（**不许破坏**）
* 1 unit = 1/16 格，+y 向下，-z 正脸；
* 模型从**天空裂口垂下**：root 部件整体在负 y（悬空），底部（maxY）是"最低垂点"；
* `EvilGazerRenderer` 按阶段切模型，三套骨架的 root pivot 都接近世界锚点，切模型不跳；
* 锚点容差 ±15%（本轮 ARM 底 +6%、HEAD 顶 -5%、UPPER 顶 -12.6%，均写进报告）。

## 渲染器怎么切
`MobRenderer.getModel()` 没有实体参数，所以 `EvilGazerRenderer` 覆写 `render(...)`
先记下当前阶段、`getModel()` 再返回对应骨架（三套骨架共用一个 1024² 图集）。

## 本脚本产出（单一真源）
1. `parts` 表 → `Atlas` 装箱（1024×1024，带 2px 间隔）→ 生成三套骨架；
2. 按**分区材质**程序化画 `evil_phases.png`（腐肉/血/骨/眼/裂缝/甲/角/爪/口腔，
   含噪声、纹理、边缘阴影、眼球高光）；
3. emit 三个 Java 模型（父链 getChild、`createBodyLayer()` 签名、`LayerDefinition.create(mesh,1024,1024)` 不变）；
4. `ANIMS` 由部件名**程序化生成**（绝对赋值、禁止 `+=`/`*=`），并留"阶段入场压入"接口注释。
"""
import io
import math
import os

import numpy as np
from PIL import Image

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
JAVA = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng", "xiuxian")
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
SIZE = 1024
GUTTER = 2          # 图集里每个盒子之间的间隔（防采样渗色）


# ======================================================================
# 0. 图集装箱
# ======================================================================
class Atlas:
    """简单的货架装箱（行高 = 该行最高盒），带 gutter 间隔。"""

    def __init__(self, size=SIZE, gutter=GUTTER):
        self.size, self.gutter = size, gutter
        self.x, self.y, self.row = 0, 0, 0

    def alloc(self, w, h):
        if self.x + w > self.size:
            self.x, self.y = 0, self.y + self.row + self.gutter
            self.row = 0
        if self.y + h > self.size:
            raise RuntimeError("atlas full（1024² 装不下，减盒子或减小尺寸）")
        u, v = self.x, self.y
        self.x += w + self.gutter
        self.row = max(self.row, h)
        return u, v


def box_uv(w, h, d):
    """原版盒式展开占用的图集矩形：(2d+2w, d+h)。"""
    return 2 * d + 2 * w, d + h


def face_rects(u, v, w, h, d):
    """六个面在某盒子图集矩形里的子矩形（与 charlib.face_rects 完全一致）。"""
    return {
        "right":  (u, v + d, u + d, v + d + h),
        "front":  (u + d, v + d, u + d + w, v + d + h),
        "left":   (u + d + w, v + d, u + d + w + d, v + d + h),
        "back":   (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        "top":    (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
    }


# ======================================================================
# 1. 三套骨架的 parts 表
#    条目 = (name, parent, pose(x,y,z), box(x,y,z,w,h,d), mat)
#    * pose = 子部件相对**父件枢轴**的偏移（原版 PartPose.offset 语义）
#    * box  = 相对**本部件枢轴**的盒子
#    * mat  = 贴图分区材质：flesh / blood / bone / eye / pupil / crack /
#             plate / horn / claw / maw / rib
# ======================================================================

# ---------------------------------------------------------------- 一阶段：巨臂
ARM = [
    # 肩甲「从云里露出的那一截」
    ("shoulder", None, (0, -40, 0), (-26, -14, -26, 52, 22, 52), "plate"),
    ("pauldronL", "shoulder", (22, -2, 0), (0, -12, -14, 14, 22, 28), "plate"),
    ("pauldronR", "shoulder", (-22, -2, 0), (-14, -12, -14, 14, 22, 28), "plate"),
    # 上臂（可摆动主体）
    ("arm", "shoulder", (0, 0, 0), (-18, 0, -18, 36, 70, 36), "flesh"),
    # 肘 / 前臂 / 巨掌
    ("elbow", "arm", (0, 70, 0), (-22, -8, -22, 44, 16, 44), "bone"),
    ("forearm", "elbow", (0, 16, 0), (-19, 0, -19, 38, 64, 38), "crack"),
    ("hand", "forearm", (0, 64, 0), (-24, 0, -24, 48, 34, 48), "flesh"),
]
# 五指 + 爪尖（从掌前缘伸出，指向前下方）
for _i, _fx in enumerate((-18, -9, 0, 9, 18)):
    ARM.append(("finger%d" % _i, "hand", (_fx, 30, -24),
                (-3, 0, -8, 6, 10, 10), "flesh"))
    ARM.append(("claw%d" % _i, "finger%d" % _i, (0, 10, -6),
                (-2, 0, -4, 4, 7, 7), "claw"))
# 臂上成排眼睛（≥10）：上臂 6 + 肘 2 + 前臂 4
for _i in range(6):
    ARM.append(("eyeA%d" % _i, "arm", (0, 8 + _i * 10, -18),
                (-8, -8, -3, 16, 16, 3), "eye"))
for _i in range(2):
    ARM.append(("eyeE%d" % _i, "elbow", (-14 + _i * 28, 0, -22),
                (-6, -6, -3, 12, 12, 3), "eye"))
for _i in range(4):
    ARM.append(("eyeF%d" % _i, "forearm", (0, 10 + _i * 14, -19),
                (-7, -7, -3, 14, 14, 3), "eye"))
# 臂表面骨刺（4）
for _i, (_sx, _sy) in enumerate(((18, 18), (-18, 18), (18, 46), (-18, 46))):
    ARM.append(("spike%d" % _i, "arm", (_sx, _sy, 4),
                (0 if _sx > 0 else -14, -6, -6, 14, 12, 12), "bone"))
# 末端 3 条触须（每条 2 段，从前臂背面垂下）
for _i, _tx in enumerate((-22, 0, 22)):
    ARM.append(("tent%d" % _i, "forearm", (_tx, 38, 20),
                (-5, 0, -5, 10, 22, 10), "blood"))
    ARM.append(("tent%d2" % _i, "tent%d" % _i, (0, 22, 0),
                (-4, 0, -4, 8, 20, 8), "blood"))

# ---------------------------------------------------------------- 二阶段：巨颅
HEAD = [
    ("head", None, (0, 6, 0), (-34, -56, -30, 68, 56, 60), "flesh"),
    ("cranium", "head", (0, -56, 0), (-28, -24, -22, 56, 24, 44), "bone"),
    ("skullPlateL", "cranium", (28, -12, 0), (0, -12, -18, 8, 14, 36), "plate"),
    ("skullPlateR", "cranium", (-28, -12, 0), (-8, -12, -18, 8, 14, 36), "plate"),
    # 上下颚 + 口腔
    ("upperJaw", "head", (0, 0, -8), (-30, 0, -28, 60, 18, 50), "flesh"),
    ("lowerJaw", "upperJaw", (0, 18, 0), (-26, 0, -24, 52, 16, 46), "flesh"),
    ("maw", "upperJaw", (0, 16, -6), (-24, 0, -18, 48, 14, 38), "maw"),
    # 中央主眼 + 瞳孔（独立部件，可以眨眼/脉动）
    ("mainEye", "head", (0, -24, -30), (-16, -16, -3, 32, 32, 3), "eye"),
    ("pupil", "mainEye", (0, 0, -3), (-7, -7, -2, 14, 14, 2), "pupil"),
    # 一圈小眼（10）：面部 4 + 颅顶 3 + 两侧 2 + 后颅 1
    ("eyeH0", "head", (-24, -42, -30), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeH1", "head", (-24, -14, -30), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeH2", "head", (24, -42, -30), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeH3", "head", (24, -14, -30), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeH4", "cranium", (-16, -12, -22), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH5", "cranium", (16, -12, -22), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH6", "cranium", (0, -14, -22), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH7", "head", (30, -20, 6), (0, -6, -6, 3, 12, 12), "eye"),
    ("eyeH8", "head", (-30, -20, 6), (-3, -6, -6, 3, 12, 12), "eye"),
    ("eyeH9", "head", (0, -30, 30), (-6, -6, 0, 12, 12, 3), "eye"),
    # 獠牙：上颚 4 + 下颚 4
    ("fangU0", "upperJaw", (-20, 18, -24), (-4, 0, -4, 8, 16, 8), "claw"),
    ("fangU1", "upperJaw", (-7, 18, -26), (-4, 0, -4, 8, 20, 8), "claw"),
    ("fangU2", "upperJaw", (7, 18, -26), (-4, 0, -4, 8, 20, 8), "claw"),
    ("fangU3", "upperJaw", (20, 18, -24), (-4, 0, -4, 8, 16, 8), "claw"),
    ("fangL0", "lowerJaw", (-18, 16, -22), (-4, -12, -4, 8, 12, 8), "claw"),
    ("fangL1", "lowerJaw", (-6, 16, -24), (-4, -10, -4, 8, 10, 8), "claw"),
    ("fangL2", "lowerJaw", (6, 16, -24), (-4, -10, -4, 8, 10, 8), "claw"),
    ("fangL3", "lowerJaw", (18, 16, -22), (-4, -12, -4, 8, 12, 8), "claw"),
    # 双角（各 2 段）
    ("hornL1", "cranium", (22, -18, 0), (-5, -14, -5, 10, 14, 10), "horn"),
    ("hornL2", "hornL1", (0, -14, 0), (-3, -10, -3, 6, 10, 6), "horn"),
    ("hornR1", "cranium", (-22, -18, 0), (-5, -14, -5, 10, 14, 10), "horn"),
    ("hornR2", "hornR1", (0, -14, 0), (-3, -10, -3, 6, 10, 6), "horn"),
    # 颅后裂口触须（3 条 × 2 段）
    ("tentH0", "head", (-22, -20, 30), (-5, 0, -5, 10, 30, 10), "blood"),
    ("tentH02", "tentH0", (0, 30, 0), (-4, 0, -4, 8, 24, 8), "blood"),
    ("tentH1", "head", (0, -24, 30), (-5, 0, -5, 10, 34, 10), "blood"),
    ("tentH12", "tentH1", (0, 34, 0), (-4, 0, -4, 8, 26, 8), "blood"),
    ("tentH2", "head", (22, -20, 30), (-5, 0, -5, 10, 30, 10), "blood"),
    ("tentH22", "tentH2", (0, 30, 0), (-4, 0, -4, 8, 24, 8), "blood"),
    # 二阶段也有一条臂（3 段 + 爪）
    ("arm2", "head", (-44, -30, 0), (-14, 0, -14, 28, 60, 28), "flesh"),
    ("forearm2", "arm2", (0, 60, 0), (-12, 0, -12, 24, 36, 24), "crack"),
    ("hand2", "forearm2", (0, 36, 0), (-16, 0, -16, 32, 14, 32), "flesh"),
    ("claw2a", "hand2", (-10, 14, -14), (-3, 0, -5, 6, 8, 10), "claw"),
    ("claw2b", "hand2", (0, 14, -16), (-3, 0, -5, 6, 9, 10), "claw"),
    ("claw2c", "hand2", (10, 14, -14), (-3, 0, -5, 6, 8, 10), "claw"),
    # 臂上的眼睛（4）
    ("eyeA2a", "arm2", (0, 14, -14), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeA2b", "arm2", (0, 40, -14), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeA2c", "forearm2", (0, 12, -12), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeA2d", "forearm2", (0, 26, -12), (-5, -5, -3, 10, 10, 3), "eye"),
]

# ---------------------------------------------------------------- 三阶段：上半身
UPPER = [
    ("torso", None, (0, 6, 0), (-35, -60, -26, 70, 60, 52), "flesh"),
    ("chest", "torso", (0, -60, 0), (-42, -26, -30, 84, 26, 60), "plate"),
    ("padL", "chest", (34, 0, 0), (-10, -14, -20, 20, 22, 40), "plate"),
    ("padR", "chest", (-34, 0, 0), (-10, -14, -20, 20, 22, 40), "plate"),
    # 胸甲肋骨（4 横 + 2 竖）
    ("rib0", "chest", (0, -24, -30), (-30, 0, -2, 60, 4, 4), "rib"),
    ("rib1", "chest", (0, -18, -30), (-32, 0, -2, 64, 4, 4), "rib"),
    ("rib2", "chest", (0, -12, -30), (-30, 0, -2, 60, 4, 4), "rib"),
    ("rib3", "chest", (0, -6, -30), (-26, 0, -2, 52, 4, 4), "rib"),
    ("sternumL", "chest", (-8, -24, -30), (-4, 0, -2, 4, 24, 4), "rib"),
    ("sternumR", "chest", (8, -24, -30), (-4, 0, -2, 4, 24, 4), "rib"),
    # 巨颅（复用二阶段元素）
    ("head", "chest", (0, -26, 0), (-30, -40, -26, 60, 40, 52), "flesh"),
    ("cranium", "head", (0, -40, 0), (-26, -18, -22, 52, 18, 44), "bone"),
    ("mainEye", "head", (0, -20, -26), (-14, -14, -3, 28, 28, 3), "eye"),
    ("pupil", "mainEye", (0, 0, -3), (-6, -6, -2, 12, 12, 2), "pupil"),
    # 头部小眼（8）
    ("eyeH0", "head", (-22, -34, -26), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH1", "head", (22, -34, -26), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH2", "head", (-22, -8, -26), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH3", "head", (22, -8, -26), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH4", "cranium", (-14, -10, -22), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH5", "cranium", (14, -10, -22), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeH6", "head", (30, -26, 4), (0, -5, -5, 3, 10, 10), "eye"),
    ("eyeH7", "head", (-30, -26, 4), (-3, -5, -5, 3, 10, 10), "eye"),
    # 上下颚 + 獠牙 8
    ("upperJaw", "head", (0, 0, -6), (-28, 0, -26, 56, 16, 46), "flesh"),
    ("lowerJaw", "upperJaw", (0, 16, 0), (-24, 0, -22, 48, 14, 42), "flesh"),
    ("fangU0", "upperJaw", (-18, 16, -22), (-4, 0, -4, 8, 14, 8), "claw"),
    ("fangU1", "upperJaw", (-6, 16, -24), (-4, 0, -4, 8, 17, 8), "claw"),
    ("fangU2", "upperJaw", (6, 16, -24), (-4, 0, -4, 8, 17, 8), "claw"),
    ("fangU3", "upperJaw", (18, 16, -22), (-4, 0, -4, 8, 14, 8), "claw"),
    ("fangL0", "lowerJaw", (-16, 14, -20), (-4, -10, -4, 8, 10, 8), "claw"),
    ("fangL1", "lowerJaw", (-5, 14, -22), (-4, -8, -4, 8, 8, 8), "claw"),
    ("fangL2", "lowerJaw", (5, 14, -22), (-4, -8, -4, 8, 8, 8), "claw"),
    ("fangL3", "lowerJaw", (16, 14, -20), (-4, -10, -4, 8, 10, 8), "claw"),
    # 双角（1 段，从颅顶两侧向上外）
    ("hornL", "cranium", (20, -18, 0), (-5, -10, -5, 10, 10, 10), "horn"),
    ("hornR", "cranium", (-20, -18, 0), (-5, -10, -5, 10, 10, 10), "horn"),
    # 双巨臂：各 3 段 + 爪（5 指）
    ("armR", "torso", (-38, -34, 0), (-16, 0, -16, 32, 50, 32), "flesh"),
    ("forearmR", "armR", (0, 50, 0), (-14, 0, -14, 28, 50, 28), "crack"),
    ("handR", "forearmR", (0, 50, 0), (-18, 0, -18, 36, 18, 36), "flesh"),
    ("armL", "torso", (38, -34, 0), (-16, 0, -16, 32, 50, 32), "flesh"),
    ("forearmL", "armL", (0, 50, 0), (-14, 0, -14, 28, 50, 28), "crack"),
    ("handL", "forearmL", (0, 50, 0), (-18, 0, -18, 36, 18, 36), "flesh"),
]
for _side in ("R", "L"):
    for _i, _fx in enumerate((-12, -6, 0, 6, 12)):
        UPPER.append(("claw%s%d" % (_side, _i), "hand" + _side,
                      (_fx, 18, -14), (-3, 0, -5, 6, 12, 10), "claw"))
# 躯干/胸/肩上的眼睛（8）
UPPER += [
    ("eyeT0", "torso", (-18, -30, -26), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeT1", "torso", (18, -30, -26), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeT2", "torso", (-18, -6, -26), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeT3", "torso", (18, -6, -26), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeC0", "chest", (-20, -20, -30), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeC1", "chest", (20, -20, -30), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeS0", "padL", (12, 6, -18), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeS1", "padR", (12, 6, -18), (-6, -6, -3, 12, 12, 3), "eye"),
    # 臂上的眼睛（4）
    ("eyeAR", "armR", (0, 16, -16), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeAR2", "forearmR", (0, 16, -14), (-5, -5, -3, 10, 10, 3), "eye"),
    ("eyeAL", "armL", (0, 16, -16), (-6, -6, -3, 12, 12, 3), "eye"),
    ("eyeAL2", "forearmL", (0, 16, -14), (-5, -5, -3, 10, 10, 3), "eye"),
    # 背部裂口触须（3 × 2） + 骨刺（4）
    ("tentB0", "torso", (-24, -30, 26), (-5, 0, -5, 10, 26, 10), "blood"),
    ("tentB02", "tentB0", (0, 26, 0), (-4, 0, -4, 8, 22, 8), "blood"),
    ("tentB1", "torso", (0, -34, 26), (-5, 0, -5, 10, 30, 10), "blood"),
    ("tentB12", "tentB1", (0, 30, 0), (-4, 0, -4, 8, 24, 8), "blood"),
    ("tentB2", "torso", (24, -30, 26), (-5, 0, -5, 10, 26, 10), "blood"),
    ("tentB22", "tentB2", (0, 26, 0), (-4, 0, -4, 8, 22, 8), "blood"),
    ("spikeB0", "chest", (-30, -20, 24), (0, -6, -6, 14, 12, 12), "bone"),
    ("spikeB1", "chest", (30, -20, 24), (-14, -6, -6, 14, 12, 12), "bone"),
    ("spikeB2", "torso", (-26, -46, 20), (0, -10, -5, 10, 20, 10), "bone"),
    ("spikeB3", "torso", (26, -46, 20), (-10, -10, -5, 10, 20, 10), "bone"),
]

MODELS = [
    ("EvilArmModel", "一阶段：从云里垂下的巨臂", ARM),
    ("EvilHeadModel", "二阶段：裂口中的巨颅 + 一条臂", HEAD),
    ("EvilUpperModel", "三阶段：挤满天空的上半身", UPPER),
]

# ======================================================================
# 2. 图集装箱
# ======================================================================
atlas = Atlas()
all_parts = []
for cls, title, parts in MODELS:
    built = []
    for (name, parent, pose, box, mat) in parts:
        w, h, d = int(round(box[3])), int(round(box[4])), int(round(box[5]))
        u, v = atlas.alloc(*box_uv(w, h, d))
        built.append(dict(name=name, parent=parent, pose=pose, box=box,
                          mat=mat, uv=(u, v)))
    all_parts.append((cls, title, built))
used = max((p["uv"][1] + box_uv(int(round(p["box"][3])), int(round(p["box"][4])),
                                int(round(p["box"][5])))[1]) for _c, _t, bp in all_parts
            for p in bp)
print("atlas 使用高度 %d / %d" % (used, SIZE))

# ======================================================================
# 3. 程序化贴图（分区材质 + 噪声 + 纹理 + 边缘阴影 + 眼球高光）
# ======================================================================
img = Image.new("RGBA", (SIZE, SIZE), (8, 6, 12, 255))
px = img.load()
rng = np.random.default_rng(17)

#: 材质基色（暗, 中, 亮）——「恶」的色板：腐肉暗紫 / 血红 / 骨灰白 / 甲紫黑 / 角 / 爪 / 口腔
MAT = {
    "flesh": ((30, 8, 40), (62, 18, 76), (96, 34, 104)),        # 腐肉暗紫
    "blood": ((36, 6, 14), (84, 16, 28), (140, 30, 40)),        # 血红
    "crack": ((32, 10, 38), (66, 22, 70), (150, 40, 92)),       # 裂肉（带亮红裂纹）
    "bone":  ((92, 84, 70), (150, 140, 118), (206, 198, 174)),  # 骨灰白
    "rib":   ((74, 66, 52), (124, 112, 90), (176, 164, 138)),   # 肋骨
    "plate": ((34, 24, 44), (62, 46, 76), (104, 84, 122)),      # 紫黑甲片
    "horn":  ((70, 60, 52), (120, 104, 82), (188, 170, 132)),   # 角
    "claw":  ((84, 74, 60), (140, 126, 98), (224, 210, 176)),   # 爪/牙
    "maw":   ((28, 4, 10), (72, 10, 20), (128, 24, 30)),        # 口腔内
    "eye":   ((20, 6, 16), (110, 26, 26), (190, 60, 24)),       # 眼球底色（front 面另画）
    "pupil": ((8, 4, 10), (40, 14, 24), (150, 110, 40)),        # 瞳孔（front 面另画）
}

# 各朝向的明暗（三阶明暗靠大面表达体积）
FACE_SHADE = {"front": 1.00, "back": 0.80, "right": 0.88, "left": 0.88,
              "top": 1.10, "bottom": 0.66}


def _clamp8(v):
    return 0 if v < 0 else (255 if v > 255 else int(v))


def _hsh(x, y, salt):
    h = (x * 374761393 + y * 668265263 + salt * 97531) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h ^ (h >> 16)) & 0xFF


def _lum(c):
    """三阶色板的感知亮度（0.299R+0.587G+0.114B）。"""
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def _blend3(light, mid, dark, t):
    """三阶平滑过渡：t=0 亮 → 0.5 中 → 1 暗。

    P1-1~3 关键：**三阶色差保留**（palette 亮/暗差仍 ~40 灰阶），但过渡是平滑的，
    不再逐像素在 light/mid/dark 之间随机跳（旧版噪点 = 旧版 6.9~8.3 倍的主因）。
    """
    if t <= 0.5:
        f = t / 0.5
        a, b = light, mid
    else:
        f = (t - 0.5) / 0.5
        a, b = mid, dark
    return (a[0] + (b[0] - a[0]) * f,
            a[1] + (b[1] - a[1]) * f,
            a[2] + (b[2] - a[2]) * f)


def _vein_pixels(rect, seed, mat):
    """随机游走的低对比肉纹/伤疤（替代旧版 `(yy-y0)%11==0` 的规则横线）。"""
    x0, y0, x1, y1 = rect
    out = {}
    if mat not in ("flesh", "crack", "blood"):
        return out
    r = np.random.default_rng(seed * 13 + 5)
    for _ in range(2 + (seed % 3)):
        cx = int(r.integers(x0, max(x0 + 1, x1)))
        cy = int(r.integers(y0, max(y0 + 1, y1)))
        for _s in range(int(min(x1 - x0, y1 - y0) * 0.8) + 3):
            cx += int(r.integers(-1, 2))
            cy += int(r.integers(-1, 2))
            if not (x0 <= cx < x1 and y0 <= cy < y1):
                break
            out[(cx, cy)] = -7                       # 低对比暗肉纹
            if cx + 1 < x1 and r.integers(0, 3) == 0:
                out[(cx + 1, cy)] = -4
    return out


def _paint_face(rect, mat, face, salt):
    """画一个面（P1-1~3 修复版）。

    质感目标：远看是"三阶明暗的大面"，近看只有轻微纹理，**不是逐像素雪花**。
    * 基色：沿垂直方向在 light→mid→dark 之间**平滑**过渡（三阶明暗保留）；
    * 低频：3px 块状哈希做 ±6 色阶抖动（块内稳定）；
    * 高频：逐像素只留 ±2 细噪；
    * 材质：肉纹改随机游走曲线、骨纹/甲格改低对比（≤9 灰阶）纹理；
    * 保留：朝向明暗、2px 边缘阴影、裂纹（_paint_cracks）、眼球虹膜高光（_paint_eye）。
    """
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0:
        return
    dark, mid, light = MAT.get(mat, MAT["flesh"])
    shade = FACE_SHADE[face]
    seed = salt * 31 + {"front": 1, "back": 2, "right": 3, "left": 4,
                        "top": 5, "bottom": 6}[face]
    veins = _vein_pixels(rect, seed, mat)
    fine_seed = seed ^ 0x5BD1
    # 三阶渐变按面高限斜率（≈0.7 灰阶/行）+ 色差归一（light-dark 渲染差 ≈48 灰阶）：
    # 大面读得出三阶明暗、小面不成条纹；不同材质（骨白跨度 106 / 腐肉 42）观感一致。
    gs = min(1.0, h / 60.0)
    tone_span = max(1.0, _lum(light) - _lum(dark))
    tone_scale = min(1.0, 48.0 / tone_span)
    for yy in range(y0, y1):
        t = 0.5 + ((yy - y0) / max(1, h - 1) - 0.5) * gs
        base = _blend3(light, mid, dark, t)
        base = (mid[0] + (base[0] - mid[0]) * tone_scale,
                mid[1] + (base[1] - mid[1]) * tone_scale,
                mid[2] + (base[2] - mid[2]) * tone_scale)
        for xx in range(x0, x1):
            # 低频块状色阶抖动（3px 块、±3）+ 逐像素 ±2 细噪
            #   （逐像素细噪只抬"近看质感"，4× 降采样会被平均掉，不影响 mipmap 噪点）
            d = (_hsh(xx // 3, yy // 3, seed) % 5 - 2) * 1.5
            d += (_hsh(xx, yy, fine_seed) % 5) - 2
            # 材质纹理（全部低对比，只做"读得出材质"的量）
            if mat in ("flesh", "crack", "blood"):
                d += veins.get((xx, yy), 0)
                if mat == "blood" and _hsh(xx // 4, yy // 4, seed + 3) % 7 == 0:
                    d += 5
            elif mat in ("bone", "rib", "horn", "claw"):
                # 骨纹：2px 块状随机斑（不用周期条纹，避免 mipmap 摩尔纹）
                if _hsh(xx // 2, yy // 2, seed + 7) % 6 == 0:
                    d -= 4
                elif _hsh(xx // 2, yy // 2, seed + 9) % 17 == 0:
                    d += 5
            elif mat == "plate":
                if (xx - x0) % 16 < 1 or (yy - y0) % 16 < 1:
                    d -= 5
                if _hsh(xx // 2, yy // 2, seed + 11) % 37 == 0:
                    d += 6                               # 金铆钉微高光
            elif mat == "maw":
                if _hsh(xx // 3, yy // 3, seed + 5) % 5 == 0:
                    d -= 6
            c = (_clamp8(base[0] * shade + d),
                 _clamp8(base[1] * shade + d),
                 _clamp8(base[2] * shade + d))
            # 边缘阴影（2px，保留；强度 0.84 —— 只做"倒角"读感，不制造噪点）
            if xx - x0 < 2 or x1 - xx <= 2 or yy - y0 < 2 or y1 - yy <= 2:
                c = (_clamp8(c[0] * 0.90), _clamp8(c[1] * 0.90), _clamp8(c[2] * 0.90))
            px[xx, yy] = (c[0], c[1], c[2], 255)


def _paint_cracks(rect, face, salt):
    """在裂肉面上画 2~3 条亮红裂纹（"发光感"靠高饱和亮红 + 暗描边）。"""
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w < 6 or h < 6:
        return
    r = np.random.default_rng(salt * 7 + 3)
    for _ in range(2 + (salt % 2)):
        cx = int(r.integers(x0 + 1, max(x0 + 2, x1 - 2)))
        cy = int(r.integers(y0 + 1, max(y0 + 2, y1 - 2)))
        steps = int(min(w, h) * 0.6) + 4
        for s in range(steps):
            cx += int(r.integers(-1, 2))
            cy += int(r.integers(0, 3))
            if not (x0 < cx < x1 - 1 and y0 < cy < y1 - 1):
                break
            px[cx, cy] = (255, 78, 46, 255)             # 热芯
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nx, ny = cx + dx, cy + dy
                if x0 <= nx < x1 and y0 <= ny < y1:
                    if px[nx, ny][:3] != (255, 78, 46):
                        px[nx, ny] = (96, 14, 10, 255)  # 暗描边


def _paint_eye(rect, face, seed, big):
    """眼球：红金虹膜 + 暗瞳 + 白色高光（big=主眼，画大一点）。"""
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w < 4 or h < 4:
        return
    cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    rad = min(w, h) / 2.0
    for yy in range(y0, y1):
        for xx in range(x0, x1):
            dx = (xx + 0.5 - cx) / rad
            dy = (yy + 0.5 - cy) / rad
            d = math.sqrt(dx * dx + dy * dy)
            n = _hsh(xx, yy, seed) % 100
            if d > 1.0:
                c = (24, 8, 18)
            elif d > 0.78:
                c = (120, 18, 14) if n < 70 else (160, 34, 18)   # 眼白（血红）
            elif d > 0.42:
                c = (190, 54, 16) if n < 60 else (214, 82, 22)   # 虹膜（橙红）
            elif d > 0.16:
                c = (12, 4, 8)                                    # 瞳孔
            else:
                c = (255, 244, 214)                               # 高光
            if d <= 1.0 and n > 98 and d > 0.2:
                c = (255, 190, 120)
            px[xx, yy] = (c[0], c[1], c[2], 255)


def _paint_pupil(rect, face, seed):
    """瞳孔部件：暗瞳 + 金色环 + 白高光点。"""
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w < 2 or h < 2:
        return
    for yy in range(y0, y1):
        for xx in range(x0, x1):
            edge = (xx - x0 < 1 or x1 - xx <= 1 or yy - y0 < 1 or y1 - yy <= 1)
            n = _hsh(xx, yy, seed) % 100
            if edge:
                c = (196, 150, 40) if n > 20 else (120, 84, 16)
            elif n > 92:
                c = (255, 250, 230)
            else:
                c = (10, 4, 8)
            px[xx, yy] = (c[0], c[1], c[2], 255)


salt = 0
for (_c, _t, built) in all_parts:
    for p in built:
        salt += 1
        u, v = p["uv"]
        bx = p["box"]
        w, h, d = int(round(bx[3])), int(round(bx[4])), int(round(bx[5]))
        rects = face_rects(u, v, w, h, d)
        mat = p["mat"]
        for face, rect in rects.items():
            x0, y0, x1, y1 = rect
            if x0 >= SIZE or y0 >= SIZE:
                continue
            if mat == "eye":
                _paint_eye((max(0, x0), max(0, y0), min(SIZE, x1), min(SIZE, y1)),
                           face, salt, face == "front")
            elif mat == "pupil":
                _paint_pupil((max(0, x0), max(0, y0), min(SIZE, x1), min(SIZE, y1)),
                             face, salt)
            else:
                _paint_face(rect, mat, face, salt)
                if mat == "crack" and face in ("front", "back"):
                    _paint_cracks(rect, face, salt)

img.save(os.path.join(RES, "textures", "entity", "evil_phases.png"))
print("   textures/entity/evil_phases.png  (%d²)" % SIZE)

# ======================================================================
# 4. 生成 Java（父链 getChild + 原有签名不变）
# ======================================================================
HEADER = [
    "package com.hongmeng.xiuxian.client;\n",
    "import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;",
    "import net.minecraft.client.model.EntityModel;",
    "import net.minecraft.client.model.geom.ModelPart;",
    "import net.minecraft.client.model.geom.PartPose;",
    "import net.minecraft.client.model.geom.builders.CubeDeformation;",
    "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
    "import net.minecraft.client.model.geom.builders.LayerDefinition;",
    "import net.minecraft.client.model.geom.builders.MeshDefinition;",
    "import net.minecraft.client.model.geom.builders.PartDefinition;",
    "import net.minecraft.util.Mth;\n",
]

#: 阶段入场接口注释（renderer 用阶段字段驱动；模型侧不改 root 位置，只留说明）
ENTRANCE_NOTE = """
        // ────────────────────────────────────────────────────────────────
        // 【阶段入场压入 · 接口约定】renderer 侧用阶段字段驱动：
        //   p = 入场进度 0..1（EvilSky / EvilFall 阶段开始后 40t 内线性推进）
        //   renderer: pose.translate(0, ENTRANCE_DROP * (1 - p), 0)  // 从上方压下来
        //   本模型**不**自己改 root 位置（避免与阶段缩放/镜头叠加），
        //   只把各部件姿态按 p 做"收拢→展开"（下方已按常态计算，入场时由 renderer 乘外部系数）。
        // ────────────────────────────────────────────────────────────────"""


def emit(cls, title, built, anim_body):
    L = list(HEADER)
    L.append("/**")
    L.append(" * <b>%s</b> —— 「恶」的三套阶段骨架之一（由 " % title)
    L.append(" * {@code tools/gen_evil_phases.py} 从 parts 表生成：模型/贴图/动画同一张表）。")
    L.append(" *")
    L.append(" * <p><b>阶段入场压入接口</b>：renderer 用阶段字段做"
             " {@code translate(0, ENTRANCE_DROP*(1-p), 0)} 从上方压入；")
    L.append(" * 本模型不改 root 位置，只把部件姿态按常态计算（绝对赋值，禁止 += / *=）。</p>")
    L.append(" */")
    L.append("public class %s extends EntityModel<CultivationMonsterEntity> {\n" % cls)
    L.append("    private final ModelPart root;")
    for p in built:
        L.append("    private final ModelPart %s;" % p["name"])
    L.append("")
    L.append("    public %s(ModelPart root) {" % cls)
    L.append("        this.root = root;")
    by_name = {p["name"]: p for p in built}
    for p in built:
        # 父链 getChild（当前渲染器/工具都按这个契约取部件）
        chain = []
        q = p
        while q["parent"] is not None:
            chain.append(q["parent"])
            q = by_name[q["parent"]]
        path = "root"
        for cn in reversed(chain):
            path += '.getChild("%s")' % cn
        L.append('        this.%s = %s.getChild("%s");' % (p["name"], path, p["name"]))
    L.append("    }\n")
    L.append("    @Override")
    L.append("    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,")
    L.append("                               com.mojang.blaze3d.vertex.VertexConsumer buf,")
    L.append("                               int light, int overlay,")
    L.append("                               float r, float g, float b, float a) {")
    L.append("        this.root.render(pose, buf, light, overlay, r, g, b, a);")
    L.append("    }\n")
    L.append("    public static LayerDefinition createBodyLayer() {")
    L.append("        MeshDefinition mesh = new MeshDefinition();")
    L.append("        PartDefinition root = mesh.getRoot();")
    L.append("        CubeDeformation NONE = CubeDeformation.NONE;")
    by_name2 = {p["name"]: p for p in built}
    for p in built:
        bx, u, v, pose = p["box"], p["uv"][0], p["uv"][1], p["pose"]
        # 父链（工具/渲染器都按 root.getChild(a).getChild(b) 取子树）
        chain = []
        q = p
        while q["parent"] is not None:
            chain.append(q["parent"])
            q = by_name2[q["parent"]]
        target = "root"
        for cn in reversed(chain):
            target += '.getChild("%s")' % cn
        L.append('        %s.addOrReplaceChild("%s", CubeListBuilder.create()'
                 % (target, p["name"]))
        L.append("                .texOffs(%d, %d).addBox(%.1fF, %.1fF, %.1fF, %.1fF, "
                 "%.1fF, %.1fF, NONE)," % (u, v, bx[0], bx[1], bx[2], bx[3], bx[4], bx[5]))
        L.append("                PartPose.offset(%.1fF, %.1fF, %.1fF));"
                 % (pose[0], pose[1], pose[2]))
    L.append("        return LayerDefinition.create(mesh, %d, %d);" % (SIZE, SIZE))
    L.append("    }\n")
    L.append("    @Override")
    L.append("    public void setupAnim(CultivationMonsterEntity entity, float limbSwing,")
    L.append("                         float limbSwingAmount, float ageInTicks,")
    L.append("                         float netHeadYaw, float headPitch) {")
    L.append(anim_body)
    L.append("    }")
    L.append("}")
    return "\n".join(L)


# ======================================================================
# 5. ANIMS：按部件名程序化生成（绝对赋值；眼睛用 yScale 眨眼；触须多相位）
# ======================================================================
def group_eyes(built, prefix):
    return [p["name"] for p in built if p["name"].startswith(prefix)]


def blink_lines(names, base_phase=0.0, groups=3, tag="a"):
    """生成 N 个分件眼睛的眨眼/脉动（绝对赋值；yScale 只用于眼睛，契约 §1.4 允许）。

    ⚠ 同一 setupAnim 里会被调用多次（不同眼睛组），变量名必须带 tag 否则
    "variable blink0 is already defined" —— 第五轮编译时踩过。
    """
    out = []
    if not names:
        return out
    for g in range(groups):
        ph = base_phase + g * 2.1
        out.append("        float blink%s%d = 1.0F - 0.55F * Mth.clamp("
                   "Mth.sin(t * 0.045F + %.1fF) * 3.2F - 2.4F, 0.0F, 1.0F);"
                   % (tag, g, ph))
    for i, nm in enumerate(names):
        g = i % groups
        out.append("        this.%s.yScale = blink%s%d;" % (nm, tag, g))
        out.append("        this.%s.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + %.1fF);"
                   % (nm, i * 0.7))
    return out


def tentacle_lines(names, base_speed=0.05):
    """触须多相位波动（xRot/zRot 绝对赋值）。names 按 2 段一组配对。"""
    out = []
    for i in range(0, len(names), 2):
        a = names[i]
        out.append("        this.%s.xRot = 0.14F + 0.10F * Mth.sin(t * %.2fF + %.1fF);"
                   % (a, base_speed, i * 0.5))
        out.append("        this.%s.zRot = 0.08F * Mth.sin(t * %.2fF + %.1fF);"
                   % (a, base_speed * 0.8, i * 0.5 + 1.0))
        if i + 1 < len(names):
            b = names[i + 1]
            out.append("        this.%s.xRot = -0.10F + 0.14F * Mth.sin(t * %.2fF + %.1fF);"
                       % (b, base_speed, i * 0.5 + 0.9))
            out.append("        this.%s.zRot = -0.10F * Mth.sin(t * %.2fF + %.1fF);"
                       % (b, base_speed * 0.9, i * 0.5 + 1.7))
    return out


def grip_lines(names, base=0.10, amp=0.18, speed=0.07):
    out = []
    for i, nm in enumerate(names):
        out.append("        this.%s.xRot = %.2fF + %.2fF * Mth.sin(t * %.2fF + %.1fF);"
                   % (nm, base, amp, speed, i * 0.9))
    return out


def anim_arm(built):
    L = [ENTRANCE_NOTE,
         "        float t = ageInTicks;",
         "        final float DEG = 0.017453292F;",
         "        // 巨臂慢摆（整条臂从肩甲处垂下，像被天空拽着）",
         "        this.arm.xRot = 0.06F + 0.08F * Mth.sin(t * 0.045F);",
         "        this.arm.yRot = 0.12F * Mth.sin(t * 0.031F);",
         "        this.arm.zRot = 0.05F * Mth.sin(t * 0.027F + 0.6F);",
         "        this.shoulder.xRot = 0.02F * Mth.sin(t * 0.023F);",
         "        this.elbow.xRot = 0.03F * Mth.sin(t * 0.037F + 1.0F);",
         "        this.forearm.xRot = 0.05F + 0.04F * Mth.sin(t * 0.041F + 2.0F);",
         "        this.hand.xRot = 0.05F * Mth.sin(t * 0.052F + 0.4F);",
         "        // 五指抓握（张开-收拢循环）"]
    L += grip_lines(["finger%d" % i for i in range(5)], 0.12, 0.18, 0.07)
    L += ["        // 爪尖（比指节慢半拍，收拢时先勾）"]
    L += grip_lines(["claw%d" % i for i in range(5)], 0.16, 0.22, 0.07)
    L += ["        // 触须多相位波动"]
    L += tentacle_lines(["tent0", "tent02", "tent1", "tent12", "tent2", "tent22"])
    L += ["        // 臂上眼睛：分件眨眼 + 脉动（瞳孔高光在贴图上）"]
    eyes = group_eyes(built, "eye")
    L += blink_lines(eyes, 0.2, groups=3, tag="g")
    return "\n".join(L)


def anim_head(built):
    L = [ENTRANCE_NOTE,
         "        float t = ageInTicks;",
         "        final float DEG = 0.017453292F;",
         "        // 巨颅随视线微转（裂口里的头）",
         "        this.head.yRot = netHeadYaw * DEG * 0.30F;",
         "        this.head.xRot = headPitch * DEG * 0.22F;",
         "        this.cranium.xRot = 0.02F * Mth.sin(t * 0.021F);",
         "        // 上下颚开合（低频，像在呼吸/低吼）",
         "        this.upperJaw.xRot = 0.03F * Mth.sin(t * 0.023F);",
         "        this.lowerJaw.xRot = 0.10F + 0.10F * Mth.sin(t * 0.023F + 0.8F);",
         "        // 主眼缓慢收缩 + 瞳孔脉动",
         "        float eyePulse = 1.0F + 0.05F * Mth.sin(t * 0.035F);",
         "        this.mainEye.xScale = eyePulse;",
         "        this.mainEye.zScale = eyePulse;",
         "        this.pupil.yScale = 1.0F - 0.45F * Mth.clamp("
         "Mth.sin(t * 0.017F) * 2.6F - 2.0F, 0.0F, 1.0F);",
         "        // 小眼分件眨眼",
         ]
    L += blink_lines(group_eyes(built, "eyeH"), 0.6, groups=4, tag="h")
    L += ["        // 二阶段那条臂慢摆 + 爪抓握",
          "        this.arm2.xRot = 0.05F + 0.10F * Mth.sin(t * 0.043F + 1.4F);",
          "        this.arm2.zRot = 0.06F * Mth.sin(t * 0.029F);",
          "        this.forearm2.xRot = 0.05F * Mth.sin(t * 0.037F + 2.2F);",
          "        this.hand2.xRot = 0.06F * Mth.sin(t * 0.051F);"]
    L += grip_lines(["claw2a", "claw2b", "claw2c"], 0.12, 0.20, 0.065)
    L += ["        // 颅后裂口触须"]
    L += tentacle_lines(["tentH0", "tentH02", "tentH1", "tentH12", "tentH2", "tentH22"])
    L += blink_lines(group_eyes(built, "eyeA2"), 1.2, groups=2, tag="2")
    return "\n".join(L)


def anim_upper(built):
    L = [ENTRANCE_NOTE,
         "        float t = ageInTicks;",
         "        final float DEG = 0.017453292F;",
         "        // 三阶段：整个上半身「挤满天空」——躯干呼吸（只动 xScale/zScale）",
         "        float breath = 1.0F + 0.014F * Mth.sin(t * 0.024F);",
         "        this.torso.xScale = breath;",
         "        this.torso.zScale = breath;",
         "        this.torso.xRot = 0.02F * Mth.sin(t * 0.019F);",
         "        this.chest.xRot = 0.015F * Mth.sin(t * 0.017F + 0.7F);",
         "        // 巨颅随视线 + 下颌开合",
         "        this.head.yRot = netHeadYaw * DEG * 0.26F;",
         "        this.head.xRot = headPitch * DEG * 0.18F;",
         "        this.upperJaw.xRot = 0.03F * Mth.sin(t * 0.021F);",
         "        this.lowerJaw.xRot = 0.09F + 0.09F * Mth.sin(t * 0.021F + 0.9F);",
         "        // 主眼脉动",
         "        float eyePulse = 1.0F + 0.05F * Mth.sin(t * 0.033F);",
         "        this.mainEye.xScale = eyePulse;",
         "        this.mainEye.zScale = eyePulse;",
         "        this.pupil.yScale = 1.0F - 0.45F * Mth.clamp("
         "Mth.sin(t * 0.015F) * 2.6F - 2.0F, 0.0F, 1.0F);",
         "        // 双臂异相缓摆 + **张开的基础姿势**（P1-4：正面剪影一眼是"
         "\"双臂张开的上半身\"，不是\"腿\"）",
         "        //   上臂 zRot 外张 ±0.18（右臂 -x 侧取正、左臂 +x 侧取负）；"
         "前臂 xRot 前收 -0.20（爪离开躯干下缘）；手掌略抬 -0.10",
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
    L += ["        // 背部裂口触须 + 颅后触须"]
    L += tentacle_lines(["tentB0", "tentB02", "tentB1", "tentB12", "tentB2", "tentB22"])
    L += ["        // 全身眼睛：分件眨眼（头 8 + 躯干/胸/肩 8 + 臂 4）"]
    L += blink_lines(group_eyes(built, "eye"), 0.4, groups=5, tag="e")
    return "\n".join(L)


ANIMS = {
    "EvilArmModel": anim_arm,
    "EvilHeadModel": anim_head,
    "EvilUpperModel": anim_upper,
}

# ======================================================================
# 6. 写出 Java + 打印尺寸
# ======================================================================
for cls, title, built in all_parts:
    anim_body = ANIMS[cls](built)
    io.open(os.path.join(JAVA, "client", cls + ".java"), "w",
            encoding="utf-8", newline="\n").write(
        emit(cls, title, built, anim_body))
    # 用与 charlib 相同的父链变换算模型空间 bounds
    tr = {}
    by_name = {p["name"]: p for p in built}

    def resolve(p, depth=0):
        if p["name"] in tr:
            return tr[p["name"]]
        base = (0.0, 0.0, 0.0) if p["parent"] is None else resolve(by_name[p["parent"]], depth + 1)
        t = (base[0] + p["pose"][0], base[1] + p["pose"][1], base[2] + p["pose"][2])
        tr[p["name"]] = t
        return t

    miny, maxy = 1e9, -1e9
    minx, maxx = 1e9, -1e9
    minz, maxz = 1e9, -1e9
    for p in built:
        px_, py_, pz_ = resolve(p)
        bx = p["box"]
        minx = min(minx, px_ + bx[0]); maxx = max(maxx, px_ + bx[0] + bx[3])
        miny = min(miny, py_ + bx[1]); maxy = max(maxy, py_ + bx[1] + bx[4])
        minz = min(minz, pz_ + bx[2]); maxz = max(maxz, pz_ + bx[2] + bx[5])
    print("   %s.java  cube=%d parts=%d  y[%.0f..%.0f] 高 %.2f 格  x[%.0f..%.0f] z[%.0f..%.0f]"
          % (cls, len(built), len(built), miny, maxy, (maxy - miny) / 16.0,
             minx, maxx, minz, maxz))
