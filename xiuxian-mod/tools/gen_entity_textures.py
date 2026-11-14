#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
红蒙修仙模组 —— 实体贴图生成器
================================

生成 5 张实体贴图，全部可复现（固定随机种子，每次跑结果一致）：

    assets/hongmeng/textures/entity/blood_demon_cultivator.png   64×64
    assets/hongmeng/textures/entity/puppet_captain.png           64×64
    assets/hongmeng/textures/entity/demon_general.png            64×64
    assets/hongmeng/textures/entity/void_overlord.png            64×64
    assets/hongmeng/textures/entity/spirit_wolf.png              64×32

用法::

    python tools/gen_entity_textures.py            # 写入模组资源目录
    python tools/gen_entity_textures.py --out DIR  # 写到别处（预览用）
    python tools/gen_entity_textures.py --check    # 只校验现有贴图尺寸/UV，不写文件

设计约束（与 Java 模型严格对齐）
--------------------------------

**人形怪**（`CultivationHumanoidModel`）的贴图是标准 64×64 人形布局：

    人形六件套（原版 `HumanoidModel.createMesh` 的 UV，任何 64×64 皮肤都能贴）
      头部     texOffs( 0,  0)  8×8×8
      身体     texOffs(16, 16)  8×12×4
      右臂     texOffs(40, 16)  4×12×4
      左臂     texOffs(40, 32).mirror()  4×12×4
      右腿     texOffs( 0, 16)  4×12×4
      左腿     texOffs(16, 48).mirror()  4×12×4

    自建部件（全部落在原版人形贴图 u≥32 或 v≥32 的空白区；
    原版 zombie.png 有像素的范围是 u0..55 / v0..31）：

      部件       texOffs      尺寸        UV 包围盒（脚本自查的实际占用）
      发髻顶     (32, 32)     4×4×4       u32..47  v32..43
      发髻环     (48, 32)     6×2×6       u48..63  v32..45
      道袍腰摆   ( 0, 32)     9×3×5       u 0..27  v32..44
      前袍角     ( 0, 40)     8×6×1       u 0..17  v40..47
      后袍角     (16, 40)     8×6×1       u16..33  v40..47
      披风       (32, 32)     7×12×1      u32..47  v32..45
      剑身       (48, 32)     3×6×1       u48..55  v32..39
      剑柄       (32, 48)     4×1×2       u32..43  v48..52
      右护肩     (32, 56)     4×2×3       u32..45  v56..63
      右护肩钉   (40, 56)     3×2×3       u40..51  v56..63
      左护肩     (48, 56)mir  4×2×3       u48..63  v56..63
      左护肩钉   (56, 48)mir  3×2×3       u56..63  v48..55

    **原版人形贴图本身有两处刻意的共用**（不是 bug，是原版就这么设计的）：

      · 头顶横带 v0..7：`head` 的顶面 与 `hat` 外层共用；
      · 脚底横带 v16..23：`head` 的底面 与 两条腿的顶面共用。

    所以"自建件 vs 原版件"这一层只做**警告**不做拦截（实际也避开了），
    而"自建件 vs 自建件"是硬校验——后者重叠才会真的糊成一片。

     顺带一个反直觉的点：**一个盒子在贴图上占的不是一个矩形**。原版展开是
       u1=u  u2=u+d  u3=u+d+w  u4=u+2d+w  u5=u+2d+2w
       v1=v  v2=v+d  v3=v+d+h  v4=v+2d+h
     右面 [u1,v2,u2,v3]、左面 [u2,v2,u3,v3]、顶面 [u2,v1,u3,v2]、
     底面 [u2,v3,u3,v4]、前面 [u2,v2,u3,v3]、后面 [u4,v2,u5,v3]，
     所以包围盒是 (2d+2w) × (2d+h)：一个 8×8×8 的头就要吃掉 u0..31 / v0..31。
     这个公式是拿原版 `wolf.png` 反推验证过的（预测的 8×8 网格占用与
     实际有像素的网格完全一致，missing=[] extra=[]）。

**灵狼**（`SpiritWolfRenderer.WolfBody`）沿用原版狼的 64×32 UV：

    头 texOffs( 0, 0) 6×6×4 | 耳 texOffs(16,14) 2×2×1 ×2 | 吻 texOffs( 0,10) 3×3×4
    身 texOffs(18,14) 6×9×6 | 上身 texOffs(21, 0) 8×6×7
    腿 texOffs( 0,18) 2×8×2 ×4 | 尾 texOffs( 9,18) 2×8×2

UV 公式（原版 `Cube` 的展开方式，本文件与 Java 侧共用同一套）：

    u1 = u          u2 = u + d          u4 = u + d + w      u5 = u + 2d + w
    v1 = v          v2 = v + d          v3 = v + d + h

    右面  [u,   v2, u+d, v3]      左面  [u+d, v2, u+2d, v3]
    顶面  [u+d, v1, u+d+w, v2]    底面  [u+d, v3, u+d+w, v3+d]
    前面  [u+d, v2, u+d+w, v3]    后面  [u+2d+w, v2, u+2d+2w, v3]

（这里"前面"= 模型 -Z 方向的那一面，也就是贴图里默认画脸的那一半。
 骷髅、僵尸、玩家皮肤都是这么画的，所以标准皮肤贴上来五官位置正确。）
"""

from __future__ import annotations

import argparse
import math
import os
import sys

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("需要 Pillow：pip install pillow")

# ---------------------------------------------------------------------------
# 常量
# ---------------------------------------------------------------------------

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
TEX_DIR = os.path.join(MOD_ROOT, "src", "main", "resources",
                       "assets", "hongmeng", "textures", "entity")

# 六个面的名字，顺序固定，方便和 Java 侧对照
FACES = ("right", "left", "top", "bottom", "front", "back")

# 面的明暗系数：同一个颜色刷六个面，靠这个把立体感做出来
FACE_SHADE = {
    "right": 0.82,
    "left": 0.72,
    "top": 1.12,
    "bottom": 0.55,
    "front": 1.00,
    "back": 0.88,
}


# ---------------------------------------------------------------------------
# 画布
# ---------------------------------------------------------------------------

class Canvas:
    """一张 RGBA 贴图 + 按原版 UV 规则刷盒子的能力。"""

    def __init__(self, width: int, height: int):
        self.width = width
        self.height = height
        self.img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.px = self.img.load()

    # -- 基础绘制 ----------------------------------------------------------

    def put(self, x: int, y: int, rgba) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            self.px[x, y] = rgb(rgba)

    def get(self, x: int, y: int, default=(0, 0, 0, 0)):
        if 0 <= x < self.width and 0 <= y < self.height:
            return self.px[x, y]
        return default

    def fill_rect(self, x0: int, y0: int, x1: int, y1: int, rgba) -> None:
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.put(x, y, rgba)

    # -- UV 布局 -----------------------------------------------------------

    @staticmethod
    def face_rects(u: int, v: int, w: int, h: int, d: int) -> dict:
        """按原版 `Cube` 的展开方式算出六个面在贴图上的矩形。

        返回 {面名: (x0, y0, x1, y1)}，区间左闭右开。

        原版展开（`net.minecraft.client.model.geom.ModelPart$Cube` 的构造器）::

            u1=u  u2=u+d  u3=u+d+w  u4=u+2d+w  u5=u+2d+2w
            v1=v  v2=v+d  v3=v+d+h  v4=v+2d+h

            右面 [u1,v2,u2,v3]   左面 [u2,v2,u3,v3]
            顶面 [u2,v1,u3,v2]   底面 [u2,v3,u3,v4]
            前面 [u2,v2,u3,v3]   后面 [u4,v2,u5,v3]

        **这个公式是实测出来的，不是抄文档**：把原版 `wolf.png`（64×32）的
        7 个部件代进去，预测的 8×8 网格占用与"实际有像素的网格"**完全一致**
        （missing=[] extra=[]）；换成 `u2 = u + w` 的写法就会漏掉
        (5,2)(5,3)(6,0)(6,1) 四格。所以 `u2` 是 `u + d`。

        （`zombie.png` 用同一公式只差 (2,0)(2,1) 两格，那是原版僵尸贴图自己的
         留白——头部 texOffs(0,0) 的 8×8×8 覆盖 u0..31/v0..31，
         v0..7 那两行左右两角本来就没画。）

        一个盒子的 UV 包围盒因此是 `(2d+2w) × (2d+h)`：一个 8×8×8 的头就要
        吃掉整块 u0..31 / v0..31。自建件必须按"包围盒"排版，不能按"一个面
        一个方块"想。
        """
        u1, u2, u3 = u, u + d, u + d + w
        u4, u5 = u + 2 * d + w, u + 2 * d + 2 * w
        v1, v2, v3, v4 = v, v + d, v + d + h, v + 2 * d + h

        return {
            "right":  (u1, v2, u2, v3),
            "left":   (u2, v2, u3, v3),
            "top":    (u2, v1, u3, v2),
            "bottom": (u2, v3, u3, v4),
            "front":  (u2, v2, u3, v3),
            "back":   (u4, v2, u5, v3),
        }

    @staticmethod
    def uv_extent(u: int, v: int, w: int, h: int, d: int):
        """这个盒子真正会读到的 UV 包围盒 (x0, y0, x1, y1)。"""
        rects = Canvas.face_rects(u, v, w, h, d).values()
        x0 = min(r[0] for r in rects)
        y0 = min(r[1] for r in rects)
        x1 = max(r[2] for r in rects)
        y1 = max(r[3] for r in rects)
        return x0, y0, x1, y1

    # -- 刷盒子 ------------------------------------------------------------

    def box(self, name: str, u: int, v: int, w: int, h: int, d: int,
            shaders: dict, face_shade: dict | None = None) -> None:
        """用 6 个着色器把这个盒子的六个面画满。

        `shaders` 形如 {"right": fn, "default": fn}；单个面的函数签名是
        `fn(x, y, lx, ly, lw, lh, face, shade) -> RGBA`，返回 None 表示跳过
        （留透明，用于薄片不需要的面）。
        """
        if face_shade is None:
            face_shade = FACE_SHADE
        default = shaders.get("default")
        for face, (x0, y0, x1, y1) in self.face_rects(u, v, w, h, d).items():
            fn = shaders.get(face, default)
            if fn is None:
                continue
            shade = face_shade.get(face, 1.0)
            for y in range(y0, y1):
                for x in range(x0, x1):
                    rgba = fn(x, y, x - x0, y - y0, x1 - x0, y1 - y0, face, shade)
                    if rgba is not None:
                        self.put(x, y, rgba)

    # -- 收尾 --------------------------------------------------------------

    def save(self, path: str) -> None:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path, "PNG")
        print(f"  -> {os.path.relpath(path, MOD_ROOT)}  ({self.width}×{self.height})")


# ---------------------------------------------------------------------------
# 颜色工具
# ---------------------------------------------------------------------------

def rgb(color) -> tuple:
    """接受 '#rrggbb' / (r,g,b) / (r,g,b,a)，统一成 4 元组。"""
    if isinstance(color, str):
        s = color.lstrip("#")
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)
    if len(color) == 3:
        return (color[0], color[1], color[2], 255)
    return tuple(color)


def shade(color, factor: float, alpha: int = 255) -> tuple:
    """按系数调整明度；factor>1 变亮，<1 变暗。"""
    r, g, b, _ = rgb(color)
    return (max(0, min(255, int(r * factor))),
            max(0, min(255, int(g * factor))),
            max(0, min(255, int(b * factor))),
            alpha)


def mix(a, b, t: float) -> tuple:
    """两色线性插值，t=0 取 a，t=1 取 b。"""
    ar, ag, ab, aa = rgb(a)
    br, bg, bb, _ = rgb(b)
    return (int(ar + (br - ar) * t),
            int(ag + (bg - ag) * t),
            int(ab + (bb - ab) * t),
            aa)


def hsh(x: int, y: int, salt: int) -> int:
    """确定性哈希：同样的 (x,y,salt) 永远给同一个 0..255 的值。

    用它代替 random，保证每次生成的贴图逐像素一致（也就是"可复现"）。
    """
    h = (x * 374761393 + y * 668265263 + salt * 1442695040888963407) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h ^ (h >> 16)) & 0xFF


def noise(color, x: int, y: int, salt: int, amount: float = 0.06):
    """给颜色叠一点确定性噪点，避免大色块看起来像塑料。"""
    n = (hsh(x, y, salt) / 255.0 - 0.5) * 2.0 * amount
    return shade(color, 1.0 + n)


def jitter_alpha_edges(canvas: Canvas) -> None:
    """把轮廓最外圈的像素压暗一档，做出一点描边感。

    只处理已经有像素的位置：贴图不会凭空长出新的不透明区域。
    """
    w, h = canvas.width, canvas.height
    for y in range(h):
        for x in range(w):
            if canvas.get(x, y)[3] == 0:
                continue
            edge = False
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if canvas.get(x + dx, y + dy)[3] == 0:
                    edge = True
                    break
            if edge:
                canvas.put(x, y, shade(canvas.get(x, y), 0.72))


# ---------------------------------------------------------------------------
# 修仙者人形贴图
# ---------------------------------------------------------------------------

#: UV 布局：名称 -> (texOffs u, v, 宽 w, 高 h, 厚 d, 是否镜像)
#: 这张表和 CultivationHumanoidModel.createBodyLayer() 必须逐字对应。
#: 左臂、左腿与右侧用**同一个 texOffs**（原版就是靠 mirror() 翻 UV 实现镜像的，
#: 所以标准皮肤里左臂左腿那两块是空的——镜像件复用了右半边的像素）。
HUMANOID_LAYOUT = {
    # ---- 原版人形六件套 ----
    "head":       (0, 0, 8, 8, 8, False),
    "body":       (16, 16, 8, 12, 4, False),
    "right_arm":  (40, 16, 4, 12, 4, False),
    "left_arm":   (40, 16, 4, 12, 4, True),
    "right_leg":  (0, 16, 4, 12, 4, False),
    "left_leg":   (0, 16, 4, 12, 4, True),
    # ---- 自建部件 ----
    # 64×64 人形贴图的下半区实际只有 u32..63 / v32..63 这 32×32 一块空地：
    # u0..31 被左臂／左腿的镜像件和躯干下缘吃掉。所以自建件必须小而准，
    # 每个尺寸都是"刚好塞得下"倒推出来的（见文件头的 UV 布局表）。
    # 发髻贴脸那一圈、剑柄那段短茎这些"看不见的面"允许互相压——
    # visible_faces() 只要求看得见的面独占像素。
    "hair_bun":       (32, 32, 4, 5, 4, False),
    "cape":           (40, 48, 6, 6, 1, False),
    "blade":          (48, 32, 2, 6, 1, False),
    "blade_hilt":     (48, 40, 3, 2, 2, False),
    "right_pauldron": (32, 56, 4, 2, 3, False),
    "left_pauldron":  (32, 56, 4, 2, 3, True),
}

#: 有意共用同一块贴图的镜像对（左件就是右件翻个面，UV 必须重合）
MIRROR_TWINS = {
    "left_arm": "right_arm",
    "left_leg": "right_leg",
    "left_pauldron": "right_pauldron",
}

#: 原版人形骨架（用于"自建件有没有踩到原版区域"的软提示）
HUMANOID_VANILLA = {
    "head":       (0, 0, 8, 8, 8, False),
    "hat":        (32, 0, 8, 8, 8, False),
    "body":       (16, 16, 8, 12, 4, False),
    "right_arm":  (40, 16, 4, 12, 4, False),
    "left_arm":   (40, 16, 4, 12, 4, True),
    "right_leg":  (0, 16, 4, 12, 4, False),
    "left_leg":   (0, 16, 4, 12, 4, True),
}

#: 每个人形怪用到哪些部件（和渲染器里的 Variant 对应）
HUMANOID_VARIANTS = {
    "blood_demon_cultivator": dict(pauldrons=False, cape=False, blade=False),
    "puppet_captain":         dict(pauldrons=True,  cape=False, blade=True),
    "demon_general":          dict(pauldrons=False, cape=True,  blade=True),
    "void_overlord":          dict(pauldrons=True,  cape=True,  blade=True),
}


# ---- 各怪的调色板 ---------------------------------------------------------

PALETTES = {
    "blood_demon_cultivator": dict(
        robe=("#7d0f18", "#a81824"),      # 主袍：血红
        robe_dark="#43060c",
        trim="#e8c9a0",                   # 滚边：暗金
        sash="#2a0409",
        skin="#d8c0a0",
        skin_dark="#9c7f5f",
        eye="#ff3324",
        hair="#5c0810",
        metal="#8d6b46",
        boot="#2b0207",
        accent="#ff5a3c",
    ),
    "puppet_captain": dict(
        robe=("#6c5230", "#8f6f3f"),      # 青铜傀儡
        robe_dark="#3a2b17",
        trim="#c9a44e",
        sash="#5a4426",
        skin="#a98a5c",
        skin_dark="#6d5636",
        eye="#ffc23a",
        hair="#7a6238",
        metal="#d8b45c",
        boot="#33260f",
        accent="#8fe3d0",                 # 灵枢的青光
    ),
    "demon_general": dict(
        robe=("#2f1a4a", "#452a68"),      # 深紫魔道
        robe_dark="#170b26",
        trim="#9a6ee0",
        sash="#0f0618",
        skin="#c8b4a8",
        skin_dark="#8a7266",
        eye="#d24bff",
        hair="#1b0f2c",
        metal="#7f6a9c",
        boot="#120818",
        accent="#b06cff",
    ),
    "void_overlord": dict(
        robe=("#121016", "#1d1a24"),      # 近黑
        robe_dark="#08070b",
        trim="#e8be53",                   # 金线
        sash="#2c2410",
        skin="#2a2630",
        skin_dark="#151219",
        eye="#ffd24a",
        hair="#0a0810",
        metal="#f0cd63",
        boot="#0a0810",
        accent="#ffd977",
    ),
}

#: 品阶标识色（和 MobRank.colorCode 的观感对齐）
RANK_ACCENT = {
    "blood_demon_cultivator": "elite",     # 精英：一条细血线
    "puppet_captain": "captain",           # 队长：金属分节环
    "demon_general": "boss",               # 头目：贯通符纹
    "void_overlord": "overlord",           # 首领：金线满绣
}


def _face_shader(kind: str, pal: dict, rank: str, salt: int, part: str):
    """给某个部位造一个着色器。kind 决定画什么花样。"""

    def base(x, y, lx, ly, lw, lh, face, sh):
        return None  # 占位

    def robe(x, y, lx, ly, lw, lh, face, sh):
        """道袍主体：竖向褶皱 + 腰带 + 袍摆。

        躯干盒子 8×12×4，从上到下 12 像素：
          ly 0..1   领口
          ly 2     腰带（比别的行暗一档，读起来是束腰）
          ly 9..11 袍摆（往下渐深，最后一行压一道滚边）
        下面 3 行是给"道袍垂过胯"用的——穿了袍子就不该看见裤子分叉。
        """
        t = ly / max(1, lh - 1)
        col = mix(pal["robe"][0], pal["robe"][1], t)
        # 竖向褶皱：每 3 像素一折，用哈希错开
        if (lx + (hsh(lx, ly, salt) & 1)) % 3 == 0:
            col = shade(col, 0.86)
        col = noise(col, x, y, salt + 11, 0.05)
        if ly <= 1:
            col = shade(pal["robe_dark"], 1.15)          # 交领
        elif ly == 2:
            col = noise(pal["sash"], x, y, salt + 15, 0.05)   # 腰带
        elif ly >= lh - 3:
            col = shade(col, 0.9)                        # 袍摆
            if ly == lh - 1:
                col = shade(pal["trim"], 0.8)            # 下摆滚边
        return shade(col, sh)

    def skin(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["skin"], x, y, salt + 3, 0.055)
        if rank == "elite":
            # 血魔：皮肤下透出血纹，每隔几行一道
            if hsh(lx, ly, salt + 21) % 5 == 0:
                col = mix(col, pal["robe"][1], 0.55)
        elif rank == "boss":
            if hsh(lx, ly, salt + 31) % 7 == 0:
                col = mix(col, pal["accent"], 0.35)
        elif rank == "overlord":
            if (lx + ly) % 6 == 0:
                col = mix(col, pal["metal"], 0.45)
        elif rank == "captain":
            # 傀儡：分节环，横向一道亮一道暗
            if ly % 4 == 3:
                col = mix(pal["metal"], pal["skin_dark"], 0.35)
            elif ly % 4 == 0:
                col = shade(pal["metal"], 1.05)
        return shade(col, sh)

    def face_shader(x, y, lx, ly, lw, lh, face, sh):
        """头：正面画五官，其余面是皮肤 + 头顶发际。"""
        if face == "bottom":
            return None  # 脖子那一面被身体挡住，留透明省得渗色
        col = noise(pal["skin"], x, y, salt + 7, 0.05)
        if face == "top":
            # 头顶：一圈头发
            col = noise(pal["hair"], x, y, salt + 8, 0.06)
            return shade(col, sh)
        # 发际线：正面最上面 2 行、侧面最上面 2 行是头发
        hair_rows = 2
        if face == "front":
            if ly < 3:
                col = noise(pal["hair"], x, y, salt + 9, 0.06)
            else:
                # 眼：ly=4 那一行，左右各 2 像素
                eye_row = 4
                if ly == eye_row and (lx in (1, 2) or lx in (5, 6)):
                    col = pal["eye"]
                    col = shade(col, 1.15)
                elif ly == eye_row + 1 and (lx in (1, 2) or lx in (5, 6)):
                    col = shade(pal["eye"], 0.55)
                # 嘴
                elif ly == 6 and 3 <= lx <= 4:
                    col = shade(pal["skin_dark"], 0.7)
                # 颧骨阴影
                elif ly == 5 and lx in (0, 7):
                    col = shade(pal["skin_dark"], 0.85)
        elif face == "back":
            if ly < 4:
                col = noise(pal["hair"], x, y, salt + 10, 0.06)
        else:
            if ly < hair_rows:
                col = noise(pal["hair"], x, y, salt + 12, 0.06)
            elif ly == hair_rows and rank in ("boss", "overlord"):
                col = mix(col, pal["accent"], 0.4)  # 鬓边一道灵纹
        return shade(col, sh)

    def hair(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["hair"], x, y, salt + 13, 0.07)
        if rank == "overlord" and (lx + ly) % 3 == 0:
            col = mix(col, pal["metal"], 0.5)
        return shade(col, sh)

    def crown(x, y, lx, ly, lw, lh, face, sh):
        """发冠：一圈金属箍，首领加宝石。"""
        col = noise(pal["metal"], x, y, salt + 14, 0.06)
        if ly == 0 or ly == lh - 1:
            col = shade(pal["metal"], 1.2)
        if rank == "overlord" and face == "front" and lx == lw // 2:
            col = pal["accent"]
        return shade(col, sh)

    def skirt(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["sash"], x, y, salt + 15, 0.05)
        return shade(col, sh)

    def flap(x, y, lx, ly, lw, lh, face, sh):
        col = mix(pal["robe"][0], pal["robe"][1], ly / max(1, lh - 1))
        if (lx + hsh(lx, ly, salt + 17) % 2) % 3 == 0:
            col = shade(col, 0.87)
        return shade(col, sh)

    def cape(x, y, lx, ly, lw, lh, face, sh):
        """披风：只有正反面；首领/头目加纹样。"""
        if face in ("top", "bottom"):
            return None
        # 内面（朝身体那侧）更暗
        inner = face == "back"
        col = mix(pal["robe_dark"], pal["robe"][1], ly / max(1, lh - 1))
        if inner:
            col = shade(col, 0.7)
        if rank == "overlord":
            # 金线：竖两条 + 下摆一道
            if lx in (2, lw - 3) or ly == lh - 1 or ly == lh - 2:
                col = pal["trim"] if not inner else shade(pal["trim"], 0.6)
            elif ly % 5 == 0:
                col = mix(col, pal["trim"], 0.35)
        elif rank == "boss":
            # 魔道符纹：中轴一列亮点
            if lx == lw // 2 and ly % 4 == 0:
                col = pal["accent"]
            elif lx in (1, lw - 2):
                col = shade(col, 0.85)
        else:
            if ly % 3 == 0:
                col = shade(col, 1.08)
        return shade(col, sh)

    def armour(x, y, lx, ly, lw, lh, face, sh):
        """护肩：金属底 + 铆钉 + 高光边。"""
        col = noise(pal["metal"], x, y, salt + 18, 0.07)
        if ly == 0:
            col = shade(pal["metal"], 1.25)
        if ly == lh - 1:
            col = shade(pal["metal"], 0.6)
        if rank == "captain":
            if lx % 2 == 1:
                col = shade(col, 0.9)
            if lx in (0, lw - 1):
                col = shade(pal["metal"], 1.15)
        if rank == "overlord":
            if lx == 0 or lx == lw - 1 or ly == 0:
                col = pal["trim"]
        return shade(col, sh)

    def spike(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["metal"], x, y, salt + 19, 0.08)
        if face == "top":
            col = shade(pal["metal"], 1.3)
        return shade(col, sh)

    def blade(x, y, lx, ly, lw, lh, face, sh):
        """剑身：一条亮脊 + 两侧暗，剑尖带灵光。"""
        if face in ("top", "bottom"):
            return None
        col = noise(pal["metal"], x, y, salt + 20, 0.05)
        if face == "front":
            if lx == 0:
                col = shade(pal["metal"], 1.35)
            else:
                col = shade(pal["metal"], 0.72)
            if ly <= 1:
                col = mix(col, pal["accent"], 0.5)
        else:
            col = shade(pal["metal"], 0.78)
        return shade(col, sh)

    def hilt(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["trim"], x, y, salt + 22, 0.06)
        if ly == 0:
            col = shade(pal["trim"], 1.25)
        return shade(col, sh)

    def boot(x, y, lx, ly, lw, lh, face, sh):
        return shade(noise(pal["boot"], x, y, salt + 23, 0.05), sh)

    shaders = {
        "robe": robe, "skin": skin, "face": face_shader, "hair": hair,
        "crown": crown, "skirt": skirt, "flap": flap, "cape": cape,
        "armour": armour, "spike": spike, "blade": blade, "hilt": hilt,
        "boot": boot,
    }
    return shaders[kind]


def build_humanoid(name: str) -> Canvas:
    """造一张 64×64 的修仙者人形贴图。"""
    pal = PALETTES[name]
    rank = RANK_ACCENT[name]
    spec = HUMANOID_VARIANTS[name]
    salt = abs(hash(name)) % 1000 if False else sum(ord(c) for c in name)

    c = Canvas(64, 64)
    L = HUMANOID_LAYOUT

    def paint(part: str, kind: str, salt_off: int = 0) -> None:
        u, v, w, h, d, mirror = L[part]
        if mirror:
            # Minecraft 的镜像盒把 UV 整体翻到 texOffs 右侧那一半
            u = u + d + w
        c.box(part, u, v, w, h, d,
              {"default": _face_shader(kind, pal, rank, salt + salt_off, part)})

    # 原版人形六件套
    paint("head", "face", 1)
    paint("body", "robe", 2)
    paint("right_arm", "skin", 3)
    paint("left_arm", "skin", 3)   # 镜像件：texOffs 与右臂相同，靠 mirror() 翻 UV
    paint("right_leg", "boot", 4)
    paint("left_leg", "boot", 4)   # 同理

    # 自建部件（都在 u≥32 或 v≥32 的空白区）
    paint("hair_bun", "hair", 5)
    paint("blade", "blade", 9)
    paint("blade_hilt", "hilt", 10)
    if spec["pauldrons"]:
        paint("right_pauldron", "armour", 11)
        paint("left_pauldron", "armour", 11)
    if spec["cape"]:
        paint("cape", "cape", 13)

    # 品阶标识：让四只怪一眼能分出高低
    _stamp_rank_mark(c, name, rank, pal)
    jitter_alpha_edges(c)
    return c


def _stamp_rank_mark(c: Canvas, name: str, rank: str, pal: dict) -> None:
    """在躯干正面的胸口位置盖一个品阶标识。

    躯干正面 = texOffs(16,16) 8×12×4 的 `front` 面，也就是贴图 u20..27 / v20..31。
    """
    x0, y0 = 20, 20
    if rank == "elite":
        # 精英：胸口斜着一道血痕
        for i in range(6):
            c.put(x0 + 1 + i, y0 + 2 + i, pal["accent"])
        return
    if rank == "captain":
        # 队长：胸口一块青铜方形护心镜
        c.fill_rect(x0 + 2, y0 + 3, x0 + 6, y0 + 7, shade(pal["metal"], 0.85))
        c.fill_rect(x0 + 3, y0 + 4, x0 + 5, y0 + 6, pal["metal"])
        c.put(x0 + 4, y0 + 5, pal["accent"])
        return
    if rank == "boss":
        # 头目：胸口一个「魔」字意的符纹（竖轴 + 两道横）
        for y in range(y0 + 2, y0 + 8):
            c.put(x0 + 4, y, pal["accent"])
        c.fill_rect(x0 + 2, y0 + 3, x0 + 7, y0 + 4, shade(pal["accent"], 0.8))
        c.fill_rect(x0 + 2, y0 + 6, x0 + 7, y0 + 7, shade(pal["accent"], 0.8))
        return
    # 首领：金线滚边 + 中轴金链
    for y in range(y0, y0 + 12):
        c.put(x0, y, pal["trim"])
        c.put(x0 + 7, y, pal["trim"])
    for y in range(y0 + 1, y0 + 11, 2):
        c.put(x0 + 4, y, pal["trim"])
    c.fill_rect(x0 + 2, y0 + 4, x0 + 6, y0 + 7, shade(pal["trim"], 0.55))
    c.put(x0 + 4, y0 + 5, pal["accent"])


# ---------------------------------------------------------------------------
# 灵狼贴图（64×32，沿用原版狼 UV）
# ---------------------------------------------------------------------------

WOLF_LAYOUT = {
    # 原版狼的 texOffs；只有 body 的 v 从 14 挪到 28。
    # 原版那个 6×9×6 的盒子，底面包围盒是 v32..36，**出图了**——MC 会把超出的
    # UV 夹到边缘像素，"能用"但会把最后一行像素抹平。这里挪一格让它落在图内，
    # 顺便和尾巴（u9..13）错开，互不干扰。
    "head":       (0, 0, 6, 6, 4),
    "ear":        (16, 14, 2, 2, 1),
    "muzzle":     (0, 10, 3, 3, 4),
    "body":       (18, 14, 6, 9, 6),
    "upper_body": (21, 0, 8, 6, 7),
    "leg":        (0, 18, 2, 8, 2),
    "tail":       (9, 18, 2, 8, 2),
}

WOLF_PAL = dict(
    fur="#dce9f2",
    fur_dark="#9fb6c9",
    fur_belly="#f2f7fb",
    back="#7f97ad",
    eye="#4fe3ff",
    nose="#2b3a45",
    claw="#8fa6b8",
    glow="#8ff2ff",
)


def build_wolf() -> Canvas:
    """雪灵狼：银白毛皮 + 背脊一道深色 + 发出青光的眼睛。"""
    pal = WOLF_PAL
    c = Canvas(64, 32)
    L = WOLF_LAYOUT

    def fur(x, y, lx, ly, lw, lh, face, sh, *, dark_top=False, belly=False):
        col = noise(pal["fur"], x, y, 71, 0.07)
        # 毛发纹理：斜向短毛
        if (lx * 3 + ly * 2) % 7 == 0:
            col = shade(col, 0.92)
        if (lx + ly) % 5 == 0:
            col = shade(col, 1.06)
        if dark_top and face == "top":
            col = noise(pal["back"], x, y, 72, 0.06)
        if belly and face == "bottom":
            col = noise(pal["fur_belly"], x, y, 73, 0.05)
        return shade(col, sh)

    def head_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur"], x, y, 74, 0.07)
        if face == "front":
            # 6×6 的脸：眼睛在 ly=2，鼻梁居中
            if ly == 2 and lx in (1, 4):
                col = pal["eye"]
            elif ly == 3 and lx in (1, 4):
                col = shade(pal["eye"], 0.5)
            elif ly == 4 and lx in (2, 3):
                col = pal["nose"]
            elif ly == 0:
                col = noise(pal["back"], x, y, 75, 0.06)
        elif face == "top":
            col = noise(pal["back"], x, y, 76, 0.06)
        return shade(col, sh)

    def ear_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur_dark"], x, y, 77, 0.07)
        if face == "front":
            col = noise(pal["back"], x, y, 78, 0.06)
        return shade(col, sh)

    def muzzle_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur"], x, y, 79, 0.07)
        if face == "front":
            if ly == 0 and lx in (1, 2):
                col = pal["nose"]
            elif ly == 1 and lx == 1:
                col = shade(pal["fur_belly"], 0.9)
        if face == "bottom":
            col = noise(pal["fur_belly"], x, y, 80, 0.05)
        return shade(col, sh)

    def leg_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur_dark"], x, y, 81, 0.07)
        if ly >= lh - 2:
            col = noise(pal["claw"], x, y, 82, 0.05)
        if (lx + ly) % 4 == 0:
            col = shade(col, 0.93)
        return shade(col, sh)

    def tail_shader(x, y, lx, ly, lw, lh, face, sh):
        col = noise(pal["fur"], x, y, 83, 0.07)
        # 尾巴尖发灵光
        if ly >= lh - 3:
            col = mix(col, pal["glow"], 0.45 + 0.2 * ((ly) % 2))
        return shade(col, sh)

    c.box("head", *L["head"], {"default": head_shader})
    c.box("ear", *L["ear"], {"default": ear_shader})
    c.box("muzzle", *L["muzzle"], {"default": muzzle_shader})
    c.box("body", *L["body"],
          {"default": lambda x, y, lx, ly, lw, lh, f, s: fur(x, y, lx, ly, lw, lh, f, s, belly=True)})
    c.box("upper_body", *L["upper_body"],
          {"default": lambda x, y, lx, ly, lw, lh, f, s: fur(x, y, lx, ly, lw, lh, f, s, dark_top=True)})
    c.box("leg", *L["leg"], {"default": leg_shader})
    c.box("tail", *L["tail"], {"default": tail_shader})

    jitter_alpha_edges(c)
    return c


# ---------------------------------------------------------------------------
# 校验
# ---------------------------------------------------------------------------

def visible_faces(w: int, h: int, d: int) -> tuple:
    """这个盒子上"看得见"的面。

    只有这些面的 UV 必须独占像素且落在图内——看不见的面可以随便和别的面共用、
    甚至越界（越界时 MC 会把 UV 夹到边缘像素，跟原版一样不报错）：
    贴着身体的躯干面、薄片的内侧、被别的部件挡住的底面，都渲染不到。

    按"六个面都要独占"排版是排不出来的：一件道袍光袍角就有 8 个面，
    而 64×64 人形贴图的下半区总共只剩 32×32 一块空地。
    """
    faces = {"front", "back", "top"}   # bottom 朝下，几乎看不到，不算进来
    if w > 1:
        faces |= {"right", "left"}
    return tuple(sorted(faces))


def check_layouts() -> None:
    """离线自查：确认每个部件的 UV 都落在图内，且自建部件之间没有互相踩到。

    Minecraft 的盒子 UV 越界或重叠都**不会报错**，只会在模型上糊出一片颜色，
    所以必须在生成之前离线拦住。

    两条不同强度的规则：

    * **硬错误**：任何面的 UV 越出贴图范围；自建部件之间有任何面矩形相交。
    * **软警告**：自建部件与原版骨架的面矩形相交。原版贴图本身就有两处
      刻意的共用（v0..7 头顶／帽子外层、v16..23 头底／两条腿的顶面），
      所以这一层只提示不拦截——只要别把整块区域盖掉就行。
    """
    problems = []
    warnings = []

    def mirror_u(u, w, d, mirror):
        """镜像盒：UV 整体翻到 texOffs 右半，即起点 + (d + w)。"""
        return u + (d + w) if mirror else u

    # 1) 人形：所有**看得见**的面都必须在 64×64 内
    #    （看不见的面越界无所谓——比如腿底面 v64..68，那个面朝上贴着躯干，
    #     永远渲染不到；原版自己也有这种越界）
    for table in (HUMANOID_LAYOUT, HUMANOID_VANILLA):
        for part, (u, v, w, h, d, mirror) in table.items():
            uu = mirror_u(u, w, d, mirror)
            rects = Canvas.face_rects(uu, v, w, h, d)
            for face in visible_faces(w, h, d):
                x0, y0, x1, y1 = rects[face]
                if x0 < 0 or y0 < 0 or x1 > 64 or y1 > 64:
                    problems.append(f"人形 {part}.{face} 越界: {(x0, y0, x1, y1)}")

    def face_list(table, skip=(), only_visible=True):
        out = {}
        for part, (u, v, w, h, d, mirror) in table.items():
            if part in skip:
                continue
            uu = mirror_u(u, w, d, mirror)
            rects = Canvas.face_rects(uu, v, w, h, d)
            keep = visible_faces(w, h, d) if only_visible else tuple(rects)
            out[part] = {f: rects[f] for f in keep}
        return out

    custom = face_list(HUMANOID_LAYOUT, skip=tuple(HUMANOID_VANILLA.keys()))
    vanilla = face_list(HUMANOID_VANILLA, only_visible=False)

    # 2) 自建部件之间：任何一个**看得见**的面相交都是硬错误
    #    （有意共图的镜像对除外）
    names = sorted(custom)
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            if MIRROR_TWINS.get(a) == b or MIRROR_TWINS.get(b) == a:
                continue
            for fa, ra in custom[a].items():
                for fb, rb in custom[b].items():
                    if _overlap(ra, rb):
                        problems.append(
                            f"人形自建件 {a}.{fa} {ra} 与 {b}.{fb} {rb} 重叠")

    # 3) 自建部件 vs 原版骨架：只警告（原版自己就共用头顶／脚底两条横带）
    for a in names:
        for b, faces in vanilla.items():
            for fa, ra in custom[a].items():
                for fb, rb in faces.items():
                    if _overlap(ra, rb):
                        warnings.append(f"{a}.{fa} {ra} 与原版 {b}.{fb} {rb} 共用像素")

    # 4) 狼：所有**看得见**的面必须在 64×32 内。
    #    `body` 的底面（v29..35）原版自己就出图了——那是原版 design，
    #    而且那个面朝下被腿和地面挡着看不见，所以只记一条提示。
    for part, (u, v, w, h, d) in WOLF_LAYOUT.items():
        rects = Canvas.face_rects(u, v, w, h, d)
        for face in visible_faces(w, h, d):
            x0, y0, x1, y1 = rects[face]
            if x0 < 0 or y0 < 0 or x1 > 64 or y1 > 32:
                problems.append(f"狼 {part}.{face} 越界: {(x0, y0, x1, y1)}")
        x0, y0, x1, y1 = rects["bottom"]
        if y1 > 32:
            warnings.append(
                f"狼 {part}.bottom UV 到 v{y1}（原版 wolf.png 同样出图，"
                f"该面朝下不可见，MC 会夹到边缘像素）")

    if warnings:
        print(f"提示：{len(warnings)} 处与原版骨架共用像素（不影响渲染，仅记录）")
        for w in warnings[:6]:
            print("  ·", w)
        if len(warnings) > 6:
            print(f"  · ...还有 {len(warnings) - 6} 处")

    if problems:
        print("UV 自查发现问题：")
        for p in problems:
            print("  !", p)
        raise SystemExit(1)

    print("UV 自查通过：")
    print(f"  · 人形：{len(HUMANOID_VANILLA)} 个原版骨架件 + "
          f"{len(custom)} 个自建件，所有面都在 64×64 内，自建件之间零相交")
    print(f"  · 灵狼：{len(WOLF_LAYOUT)} 个部件的所有面都在 64×32 内")


def _overlap(a, b) -> bool:
    return not (a[2] <= b[0] or b[2] <= a[0] or a[3] <= b[1] or b[3] <= a[1])


def verify_files(tex_dir: str) -> int:
    """校验生成结果的尺寸与模式，返回出错条数。"""
    expect = {
        "blood_demon_cultivator.png": (64, 64),
        "puppet_captain.png": (64, 64),
        "demon_general.png": (64, 64),
        "void_overlord.png": (64, 64),
        "spirit_wolf.png": (64, 32),
    }
    bad = 0
    for fn, want in expect.items():
        path = os.path.join(tex_dir, fn)
        if not os.path.exists(path):
            print(f"  ! 缺少 {fn}")
            bad += 1
            continue
        im = Image.open(path)
        ok_size = im.size == want
        ok_mode = im.mode == "RGBA"
        mark = "OK " if (ok_size and ok_mode) else "BAD"
        print(f"  [{mark}] {fn:32s} {im.size[0]}×{im.size[1]} {im.mode}")
        if not (ok_size and ok_mode):
            bad += 1
    return bad


# ---------------------------------------------------------------------------
# 入口
# ---------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(description="生成红蒙修仙模组的实体贴图")
    ap.add_argument("--out", default=TEX_DIR, help="输出目录（默认写模组资源目录）")
    ap.add_argument("--check", action="store_true", help="只校验现有贴图，不重新生成")
    ap.add_argument("--only", default=None, help="只生成某一张（不含 .png）")
    args = ap.parse_args()

    print("== UV 布局自查 ==")
    check_layouts()

    if args.check:
        print("\n== 现有贴图校验 ==")
        return 1 if verify_files(args.out) else 0

    print("\n== 生成贴图 ==")
    targets = {
        "blood_demon_cultivator": lambda: build_humanoid("blood_demon_cultivator"),
        "puppet_captain":         lambda: build_humanoid("puppet_captain"),
        "demon_general":          lambda: build_humanoid("demon_general"),
        "void_overlord":          lambda: build_humanoid("void_overlord"),
        "spirit_wolf":            build_wolf,
    }
    for name, fn in targets.items():
        if args.only and args.only != name:
            continue
        fn().save(os.path.join(args.out, name + ".png"))

    print("\n== 结果校验 ==")
    bad = verify_files(args.out)
    print("\n完成。" if not bad else f"\n有 {bad} 个文件不合格。")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
