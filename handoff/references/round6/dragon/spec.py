# -*- coding: utf-8 -*-
"""虚空龙帝（VoidDragonEmperor / void_dragon_emperor）· 第六轮旗舰重制（v5 · 视觉验收修正版）

《冰与火之歌》龙类参考。**视觉验收要点**（Lead 反馈后重做）：
* 正视图必须一眼读出「龙」：**两侧各一大块竖直翼膜扇形** + 粗壮躯干/后腿 + 前倾的巨颅；
* 翼骨整体**抬升 35~55° 斜向上展开**（不是水平横杆），翼膜是 **x-y 竖直面、厚 2px**，
  从腕部扇形铺开（4 片指间膜），正视图看到的是「面」而不是「线」；
* 躯干胸腔 22px 高、大腿 ≥18px 直径、头骨 ≥40px 长（颅 24 + 吻 13 + 颚）× ≥26px 宽、
  颈 3 节各 ≥12px 粗；
* 头颈朝前略低（颈前倾 0.18/0.14/0.08 + 头 0.10），正视图能看到吻部/双角/双眼；
* 尾巴向后下方拖出（5 节 pivot 逐节下移）。

坐标：1 unit = 1/16 格；+y 向下；-z 正脸；**脚底 y = 24**；贴图 256×256 + `_glow.png`。
目标：站高 **≈112px = 7.0 格**、翼展 **224~256px = 14~16 格**、鼻到尾 **≈185px ≈ 11.6 格**、cube ≥ 110。

## 贴图省空间：镜像件共用 UV
对称件（双翼/双腿/双角/颊刺/肩甲）用 `mirror=True` + **共用源盒 UV**（原版 mirror 语义，
`uv_audit` 记为「同足迹共用」而非重叠）。`_MIRROR_QUEUE` 收集镜像盒，文件末尾把 `S.pack_uv`
包一层：装箱前临时摘下镜像盒（只给"主盒"排 UV），装完再把镜像盒挂回并复制源盒 u/v。
主盒 UV ≈ 48.5k / 65.5k（74%），与傀儡祖师同级，256² 装得下。

## 动画（setupAnim，全部绝对赋值）
躯干呼吸（上下 ±0.8px + xScale/zScale）／3 节颈 S 摆（相位错开）／头随 `netHeadYaw*0.35`／
下颚低吼开合／双翼展呼吸（肱/前臂基础角 ± 小幅摆动，左右异相）／指骨扇形摆动／
5 节尾左右摆／数字行走（前后腿交替）／死亡时颈垂翼垂。
"""
import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_TOOLS = os.path.normpath(os.path.join(_HERE, "..", "..", "tools"))
sys.path.insert(0, _TOOLS)
import studio as S  # noqa: E402

# ----------------------------------------------------------------------
# 1. 输出目标（整文件重写现有模型/贴图；类名不变）
# ----------------------------------------------------------------------
TARGET = dict(
    java=r"xiuxian-mod\src\main\java\com\hongmeng\xiuxian\client\VoidDragonEmperorModel.java",
    texture=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\void_dragon_emperor.png",
    glow=r"xiuxian-mod\src\main\resources\assets\hongmeng\textures\entity\void_dragon_emperor_glow.png",
    outdir=r"工程\角色设计\round6\dragon",
    lift=0.0,
)

# ----------------------------------------------------------------------
# 2. 色板（虚空紫黑鳞三阶 + 骨白 + 暗红翼膜 + 青色发光）
# ----------------------------------------------------------------------
PALETTE = {
    "void_scale":    ("#1C1030", "#3E2A5E", "#7A5AB0"),   # 主鳞（虚空紫黑→亮紫罗兰）
    "void_scale2":   ("#231640", "#4E3676", "#9470C8"),   # 腹甲 / 翼骨 / 甲片
    "void_bone":     ("#4E4A5A", "#A8A2B8", "#EDE8F8"),   # 骨白角 / 牙
    "void_membrane": ("#2A0C16", "#6E1E32", "#B84A62"),   # 翼膜（暗红半透感，提亮可读）
    "void_claw":     ("#3A3444", "#7A7286", "#C8C0D8"),   # 爪 / 刺
    "void_glow":     ("#3FB4E0", "#7FE3F2", "#E8FBFF"),   # 发光裂纹 / 眼 / 晶刃
}
GLOW_PALETTE = {
    "void_glow": "#7FE3F2",   # 青色发光（眼 / 脊椎・翼骨裂纹 / 喉 / 角尖 / 尾刃）
}

# ----------------------------------------------------------------------
# 3. Spec
# ----------------------------------------------------------------------
SPEC = S.Spec(
    class_name="VoidDragonEmperorModel",
    tex=(512, 512),
    zh="虚空龙帝",
    en="VoidDragonEmperor",
    base="EntityModel",
    generic="CultivationMonsterEntity",
    layer_id=None,           # LAYER 常量在 VoidDragonEmperorRenderer 里（不改）
    doc="GoT 风虚空巨龙：前倾巨颅 + 长 S 颈 + 抬升 35~55° 的巨翼（4 指骨+4 片竖直指间膜）+ 粗后腿/小前爪 + 后下拖长尾 + 发光裂纹",
)

# ======================================================================
# 3.1 镜像工具：对称件共用 UV（见文件头说明）
# ======================================================================
_MIRROR_QUEUE = []          # [(part, mirror_box, src_box)]


def sym(part, x, y, z, sx, sy, sz, mat, **kw):
    """在 part 里同时加「左」盒与镜像「右」盒（共用 UV + mirror()）。"""
    b = part.box(x, y, z, sx, sy, sz, mat, **kw)
    mb = part.box(-(x + sx), y, z, sx, sy, sz, mat,
                  detail=kw.get("detail"), glow=kw.get("glow", False), mirror=True)
    _MIRROR_QUEUE.append((part, mb, b))
    return b


def mirror_part(src, name, parent, pivot=None, rot=None):
    """镜像一个完整部件：pivot.x 取反、rot 的 y/z 取反、盒 x 取反，UV 共用源盒。"""
    px, py, pz = pivot if pivot is not None else src.pivot
    if rot is None:
        rot = (src.rot[0], -src.rot[1], -src.rot[2]) if src.rot else None
    m = SPEC.part(name, parent=parent, pivot=(-px, py, pz), rot=rot)
    for b in src.boxes:
        nb = m.box(-(b.x + b.sx), b.y, b.z, b.sx, b.sy, b.sz, b.mat,
                   detail=b.detail, glow=b.glow, mirror=True)
        _MIRROR_QUEUE.append((m, nb, b))
    return m


# ======================================================================
# 3.2 躯干（根部件，pivot 在髋 y=-8）：**胸腔 22px 高** + 腹 + 盆 + 胸甲/发光裂纹
# ======================================================================
torso = SPEC.part("torso", pivot=(0.0, -8.0, 0.0))
torso.box(-13.0, -22.0, -20.0, 26.0, 22.0, 20.0, "void_scale", detail="obsidian")   # 胸腔 y -30..-8
torso.box(-9.0, -7.0, -9.0, 18.0, 7.0, 18.0, "void_scale", detail="obsidian")       # 腹 y -15..-8
torso.box(-8.0, -4.0, -4.0, 16.0, 4.0, 16.0, "void_scale", detail="obsidian")       # 盆 y -12..-8
torso.box(-8.0, -20.0, -21.0, 16.0, 14.0, 2.0, "void_glow", glow=True)              # 胸口发光裂纹
torso.box(-1.0, -32.0, -12.0, 2.0, 2.0, 10.0, "void_glow", glow=True)               # 脊椎发光裂纹
# 背鳍刺（2 根，中线）
for _z, _y in ((-10.0, -31.0), (-2.0, -29.0)):
    torso.box(-1.5, _y, _z, 3.0, 4.0, 6.0, "void_claw", detail="bone")
# 肩甲（镜像共用 UV）
sym(torso, 12.0, -27.0, -9.0, 11.0, 8.0, 12.0, "void_scale2", detail="stone")

# ======================================================================
# 3.3 长 S 颈（3 节，各 ≥12px 粗；前倾 0.18 / 0.14 / 0.08）
# ======================================================================
neck1 = SPEC.part("neck1", parent=torso, pivot=(0.0, -22.0, -12.0), rot=(0.18, 0.0, 0.0))
neck1.box(-6.0, -13.0, -6.5, 12.0, 13.0, 13.0, "void_scale", detail="obsidian")
neck1.box(-3.0, -13.0, -7.5, 6.0, 3.0, 2.0, "void_glow", glow=True)                 # 颈部发光裂纹
neck2 = SPEC.part("neck2", parent=neck1, pivot=(0.0, -13.0, 0.0), rot=(0.14, 0.0, 0.0))
neck2.box(-6.0, -13.0, -6.0, 12.0, 13.0, 12.0, "void_scale", detail="obsidian")
neck3 = SPEC.part("neck3", parent=neck2, pivot=(0.0, -13.0, 0.0), rot=(0.08, 0.0, 0.0))
neck3.box(-6.0, -12.0, -6.0, 12.0, 12.0, 12.0, "void_scale", detail="obsidian")
neck3.box(-1.5, -13.0, -3.0, 3.0, 4.0, 6.0, "void_claw", detail="bone")             # 颈鳍刺

# ======================================================================
# 3.4 头骨（**颅 24 长 + 吻 13 + 颚 → 总长 >40px**，宽 26px；前倾略低）
# ======================================================================
head = SPEC.part("head", parent=neck3, pivot=(0.0, -12.0, 0.0), rot=(0.0, 0.0, 0.0))
head.box(-13.0, -16.0, -12.0, 26.0, 13.0, 24.0, "void_scale", detail="obsidian")    # 颅骨
head.box(-8.0, -11.0, -25.0, 16.0, 7.0, 13.0, "void_bone", detail="bone")           # 吻部
head.box(-6.5, -7.0, -24.0, 13.0, 4.0, 13.0, "void_bone", detail="bone")            # 上颚
# 眉骨 / 眼 / 颊刺（镜像共用 UV）
sym(head, 8.0, -15.0, -13.0, 9.0, 3.0, 6.0, "void_claw", detail="bone")             # 眉骨
sym(head, 9.0, -12.0, -12.0, 7.0, 5.0, 3.0, "void_scale2", detail="stone")          # 眼窝
sym(head, 10.0, -11.0, -13.0, 5.0, 3.0, 1.0, "void_glow", glow=True)                # 发光眼
sym(head, 11.0, -10.0, -4.0, 4.0, 3.0, 6.0, "void_claw", detail="bone")             # 颊刺 1
sym(head, 12.0, -8.0, 3.0, 3.0, 3.0, 5.0, "void_claw", detail="bone")               # 颊刺 2
# 双角各 3 节（后掠；角尖发光）
sym(head, 8.0, -13.0, -4.0, 8.0, 5.0, 8.0, "void_bone", detail="bone")
sym(head, 9.0, -12.0, 4.0, 8.0, 4.0, 10.0, "void_bone", detail="bone")
sym(head, 10.0, -11.0, 14.0, 7.0, 3.0, 10.0, "void_bone", detail="bone")
sym(head, 11.0, -11.0, 24.0, 4.0, 2.0, 3.0, "void_glow", glow=True)                 # 角尖发光
# 12 牙：上颚 6（前缘交错）+ 下颚 6
for _x in (-8.0, -4.8, -1.6, 1.6, 4.8, 8.0):
    head.box(_x, -6.0, -37.0, 2.0, 5.0, 2.0, "void_bone", detail="bone")
# 下颚（独立部件：低吼开合）
lowerJaw = SPEC.part("lowerJaw", parent=head, pivot=(0.0, -2.0, -25.0))
lowerJaw.box(-6.5, 0.0, -2.0, 13.0, 4.0, 11.0, "void_bone", detail="bone")
for _x in (-6.0, -3.0, 0.0, 3.0, 6.0):
    lowerJaw.box(_x, -3.5, -10.0, 2.0, 4.0, 2.0, "void_bone", detail="bone")

# ======================================================================
# 3.5 双巨翼（左翼 +x）：肱抬 35.5°、前臂再抬 → 腕部高；指骨扇形 + **竖直翼膜**
# ======================================================================
wingL = SPEC.part("wingL", parent=torso, pivot=(14.0, -26.0, -2.0))
wingL.box(-3.0, -7.0, -8.0, 14.0, 9.0, 16.0, "void_scale2", detail="stone")         # 翼根肩甲
wingArmL = SPEC.part("wingArmL", parent=wingL, pivot=(2.0, -2.0, 0.0), rot=(0.0, 0.0, -0.55))
wingArmL.box(0.0, -4.0, -4.0, 32.0, 8.0, 8.0, "void_scale", detail="obsidian")      # 肱骨抬 35.5°
wingArmL.box(0.0, -5.0, -5.0, 32.0, 1.0, 1.0, "void_glow", glow=True)               # 肱骨发光裂纹
membraneArmL = SPEC.part("membraneArmL", parent=wingArmL, pivot=(0.0, 0.0, 0.0))
membraneArmL.box(0.0, -14.0, -2.0, 34.0, 28.0, 2.0, "void_membrane", detail="cloth")
wingForeL = SPEC.part("wingForeL", parent=wingArmL, pivot=(32.0, 0.0, 0.0), rot=(0.0, 0.0, 0.03))
wingForeL.box(0.0, -3.5, -3.5, 42.0, 7.0, 7.0, "void_scale", detail="obsidian")     # 前臂（腕部到 x≈75, y≈-68）
membraneForeL = SPEC.part("membraneForeL", parent=wingForeL, pivot=(0.0, 0.0, 0.0))
membraneForeL.box(0.0, -15.0, -2.0, 44.0, 30.0, 2.0, "void_membrane", detail="cloth")
wingWristL = SPEC.part("wingWristL", parent=wingForeL, pivot=(42.0, 0.0, 0.0), rot=(0.0, 0.0, 0.0))
wingWristL.box(-1.5, -4.5, -4.5, 9.0, 9.0, 9.0, "void_scale2", detail="stone")       # 腕
wingClawL = SPEC.part("wingClawL", parent=wingArmL, pivot=(6.0, 0.0, 0.0),
                      rot=(0.0, 0.0, 0.9))
wingClawL.box(0.0, -2.0, -2.0, 10.0, 4.0, 4.0, "void_claw", detail="bone")           # 翼爪
# 4 根分节指骨：全局角 -5° / 20° / 50° / 85°（腕部累计 -0.59rad，故 rel = 全局 +0.59）
_FINGER = [("fingerL0", 0.50, 24.0, 12.0), ("fingerL1", 0.94, 24.0, 12.0),
           ("fingerL2", 1.46, 24.0, 12.0), ("fingerL3", 2.07, 24.0, 12.0)]
for _name, _rz, _s1, _s2 in _FINGER:
    _f = SPEC.part(_name, parent=wingWristL, pivot=(2.0, 0.0, 0.0), rot=(0.0, 0.0, _rz))
    _f.box(0.0, -2.5, -2.5, _s1, 5.0, 5.0, "void_scale", detail="obsidian")
    _f.box(_s1, -2.0, -2.0, _s2, 4.0, 4.0, "void_claw", detail="bone")
# 6 片**竖直指间膜**（x-y 平面、厚 2px、径向 40、弦 15）—— 正视图是"整片翼面"
for _i, _rz in enumerate((0.30, 0.72, 1.20, 1.76, 2.20, 2.55)):
    _m = SPEC.part("membraneL%d" % _i, parent=wingWristL, pivot=(2.0, 0.0, 0.0),
                   rot=(0.0, 0.0, _rz))
    _m.box(0.0, -7.5, -1.0, 40.0, 15.0, 2.0, "void_membrane", detail="cloth")

# ---- 右翼镜像（共用 UV）----
wingR = mirror_part(wingL, "wingR", torso)
wingArmR = mirror_part(wingArmL, "wingArmR", wingR)
wingForeR = mirror_part(wingForeL, "wingForeR", wingArmR)
wingWristR = mirror_part(wingWristL, "wingWristR", wingForeR)
wingClawR = mirror_part(wingClawL, "wingClawR", wingArmR)
for _name, _rz, _s1, _s2 in _FINGER:
    _src = next(p for p in SPEC.parts if p.name == _name)
    mirror_part(_src, _name.replace("L", "R"), wingWristR)
for _i in range(6):
    _src = next(p for p in SPEC.parts if p.name == "membraneL%d" % _i)
    mirror_part(_src, "membraneR%d" % _i, wingWristR)

# 上臂/前臂大膜面镜像
membraneArmR = mirror_part(membraneArmL, "membraneArmR", wingArmR)
membraneForeR = mirror_part(membraneForeL, "membraneForeR", wingForeR)

# ======================================================================
# 3.6 后腿 ×2（**大腿 18px 直径**；脚底 y=24；镜像共用 UV）
# ======================================================================
legL = SPEC.part("legL", parent=torso, pivot=(11.0, 0.0, 0.0))
legL.box(-9.0, 0.0, -7.0, 18.0, 16.0, 15.0, "void_scale", detail="obsidian")        # 大腿 y -8..8
shinL = SPEC.part("shinL", parent=legL, pivot=(0.0, 16.0, 2.0))
shinL.box(-6.0, 0.0, -5.0, 12.0, 12.0, 12.0, "void_scale", detail="obsidian")       # 小腿 y 8..20
footL = SPEC.part("footL", parent=shinL, pivot=(0.0, 12.0, -2.0))
footL.box(-6.5, 0.0, -10.0, 13.0, 4.0, 16.0, "void_scale2", detail="stone")         # 足 y 20..24
for _cx, _w, _d in ((-5.0, 3.0, 6.0), (-1.5, 3.0, 7.0), (2.0, 3.0, 6.0)):
    footL.box(_cx, 1.0, -10.0 - _d, _w, 3.0, _d, "void_claw", detail="bone")         # 3 爪（y 21..24）

legR = mirror_part(legL, "legR", torso)
shinR = mirror_part(shinL, "shinR", legR)
footR = mirror_part(footL, "footR", shinR)

# ======================================================================
# 3.7 小前爪 ×2（镜像共用 UV）
# ======================================================================
armL = SPEC.part("armL", parent=torso, pivot=(8.0, -24.0, -14.0))
armL.box(-3.5, 0.0, -3.5, 7.0, 9.0, 7.0, "void_scale", detail="obsidian")           # 上臂
foreL = SPEC.part("foreL", parent=armL, pivot=(0.0, 9.0, 0.0))
foreL.box(-2.5, 0.0, -2.5, 5.0, 8.0, 5.0, "void_scale", detail="obsidian")          # 前臂
for _cx in (-2.5, 0.0, 2.5):
    foreL.box(_cx, 8.0, -10.0, 2.0, 3.0, 4.0, "void_claw", detail="bone")           # 3 爪

armR = mirror_part(armL, "armR", torso)
foreR = mirror_part(foreL, "foreR", armR)

# ======================================================================
# 3.8 长尾（5 节；pivot 逐节下移 → 向后下方拖出 + 虚空晶刃）
# ======================================================================
tail1 = SPEC.part("tail1", parent=torso, pivot=(0.0, -2.0, 15.0))
tail1.box(-6.0, -5.0, 0.0, 12.0, 9.0, 26.0, "void_scale", detail="obsidian")
tail2 = SPEC.part("tail2", parent=tail1, pivot=(0.0, 4.0, 26.0))
tail2.box(-5.0, -4.0, -0.5, 10.0, 7.0, 24.0, "void_scale", detail="obsidian")
tail3 = SPEC.part("tail3", parent=tail2, pivot=(0.0, 4.0, 24.0))
tail3.box(-4.0, -3.5, -0.5, 8.0, 6.0, 22.0, "void_scale", detail="obsidian")
tail4 = SPEC.part("tail4", parent=tail3, pivot=(0.0, 3.0, 22.0))
tail4.box(-3.5, -2.5, -0.5, 7.0, 5.0, 20.0, "void_scale", detail="obsidian")
tail5 = SPEC.part("tail5", parent=tail4, pivot=(0.0, 3.0, 20.0))
tail5.box(-2.5, -2.0, -0.5, 5.0, 4.0, 16.0, "void_scale", detail="obsidian")
tailBlade = SPEC.part("tailBlade", parent=tail5, pivot=(0.0, 2.0, 16.0))
tailBlade.box(-1.0, -4.0, 0.0, 2.0, 8.0, 14.0, "void_glow", glow=True)            # 虚空晶刃
# 尾刺 2 根
tail2.box(-1.5, -5.0, 5.0, 3.0, 4.0, 6.0, "void_claw", detail="bone")
tail3.box(-1.5, -5.0, 5.0, 3.0, 4.0, 6.0, "void_claw", detail="bone")

# ======================================================================
# 3.9 动画（绝对赋值；带基础旋转的部件必须把基础角写回）
# ======================================================================
SPEC.anim = """
        final float DEG = 0.017453292F;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        float walk = limbSwingAmount;
        float t = ageInTicks;
        float breathe = Mth.sin(t * 0.026F);

        // ---- 呼吸：整条龙上下起伏（0.8px）+ 胸腹微鼓 ----
        this.torso.y = breathe * 0.8F;
        this.torso.xScale = 1.0F + 0.008F * breathe;
        this.torso.zScale = 1.0F + 0.008F * breathe;
        this.torso.xRot = 0.008F * Mth.sin(t * 0.019F);

        // ---- 长 S 颈：3 节相位错开的低频摆动（基础前倾角写回）----
        this.neck1.yRot = yaw * 0.12F;
        this.neck1.xRot = 0.18F + 0.020F * Mth.sin(t * 0.023F);
        this.neck2.yRot = yaw * 0.10F;
        this.neck2.xRot = 0.14F + 0.025F * Mth.sin(t * 0.021F + 0.6F);
        this.neck3.yRot = yaw * 0.09F;
        this.neck3.xRot = 0.08F + 0.020F * Mth.sin(t * 0.019F + 1.2F);

        // ---- 头随目标（netHeadYaw * 0.35）+ 略低头 + 低吼下颚开合 ----
        this.head.yRot = yaw * 0.35F;
        this.head.xRot = pitch * 0.30F;
        this.head.zRot = 0.02F * Mth.sin(t * 0.015F);
        this.lowerJaw.xRot = 0.05F + 0.05F * Mth.sin(t * 0.045F);

        // ---- 双巨翼：翼展呼吸（左右异相，基础张角写回）----
        this.wingL.zRot = 0.030F * Mth.sin(t * 0.027F);
        this.wingR.zRot = -0.030F * Mth.sin(t * 0.027F + 0.5F);
        this.wingArmL.zRot = -0.55F + 0.080F * Mth.sin(t * 0.030F);
        this.wingArmR.zRot = 0.55F - 0.080F * Mth.sin(t * 0.030F + 0.5F);
        this.wingForeL.zRot = 0.03F + 0.055F * Mth.sin(t * 0.034F + 0.8F);
        this.wingForeR.zRot = -0.03F - 0.055F * Mth.sin(t * 0.034F + 1.3F);
        this.wingWristL.zRot = 0.040F * Mth.sin(t * 0.038F + 0.4F);
        this.wingWristR.zRot = -0.040F * Mth.sin(t * 0.038F + 0.9F);
        // 指骨扇形微摆（基础角写回）
        this.fingerL0.zRot = 0.50F + 0.035F * Mth.sin(t * 0.041F);
        this.fingerL1.zRot = 0.94F + 0.035F * Mth.sin(t * 0.041F + 0.5F);
        this.fingerL2.zRot = 1.46F + 0.035F * Mth.sin(t * 0.041F + 1.0F);
        this.fingerL3.zRot = 2.07F + 0.035F * Mth.sin(t * 0.041F + 1.5F);
        this.fingerR0.zRot = -0.50F - 0.035F * Mth.sin(t * 0.041F + 2.0F);
        this.fingerR1.zRot = -0.94F - 0.035F * Mth.sin(t * 0.041F + 2.5F);
        this.fingerR2.zRot = -1.46F - 0.035F * Mth.sin(t * 0.041F + 3.0F);
        this.fingerR3.zRot = -2.07F - 0.035F * Mth.sin(t * 0.041F + 3.5F);

        // ---- 5 节尾左右摆（相位逐节滞后）----
        this.tail1.yRot = 0.050F * Mth.sin(t * 0.020F);
        this.tail2.yRot = 0.060F * Mth.sin(t * 0.020F - 0.5F);
        this.tail3.yRot = 0.070F * Mth.sin(t * 0.020F - 1.0F);
        this.tail4.yRot = 0.080F * Mth.sin(t * 0.020F - 1.5F);
        this.tail5.yRot = 0.090F * Mth.sin(t * 0.020F - 2.0F);
        this.tailBlade.zRot = 0.06F * Mth.sin(t * 0.016F);

        // ---- 数字行走：后腿 + 小前爪交替 ----
        float step = Mth.cos(limbSwing * 0.5F) * walk;
        this.legL.xRot = 0.14F * step;
        this.legR.xRot = -0.14F * step;
        this.shinL.xRot = -0.10F * step;
        this.shinR.xRot = 0.10F * step;
        this.footL.xRot = 0.06F * step;
        this.footR.xRot = -0.06F * step;
        this.armL.xRot = -0.10F * step;
        this.armR.xRot = 0.10F * step;

        // ---- 死亡：颈垂、翼垂、头低 ----
        if (entity.isDeadOrDying()) {
            this.torso.xRot = 0.10F;
            this.neck1.xRot = 0.45F;
            this.neck2.xRot = 0.35F;
            this.neck3.xRot = 0.25F;
            this.head.xRot = 0.40F;
            this.lowerJaw.xRot = 0.35F;
            this.wingArmL.zRot = -1.30F;
            this.wingArmR.zRot = 1.30F;
            this.wingForeL.zRot = -0.60F;
            this.wingForeR.zRot = 0.60F;
        }
"""

SPEC.extra = """
    /** 模型最低点像素 y（脚底基准；renderer 若加 GROUND_LIFT 用 (LOWEST_PIXEL_Y - 24) / 16）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

    /** 最高点（头骨顶 / 后掠角），模型总高 ≈112px = 7.0 格。 */
    public static final double HIGHEST_PIXEL_Y = -88.0;
"""

# ======================================================================
# 3.10 让镜像盒共用源盒 UV（在 build_entity 调 pack_uv 之前生效）
# ======================================================================
_orig_pack_uv = S.pack_uv


def _pack_uv_mirror_aware(spec, tex=None, gutter=1):
    """装箱时先摘掉镜像盒（它们不占 UV），装完再挂回并复制源盒的 u/v。"""
    removed = []
    for (part, mb, src) in _MIRROR_QUEUE:
        if mb in part.boxes:
            part.boxes.remove(mb)
            removed.append((part, mb))
    _orig_pack_uv(spec, tex, gutter)
    for (part, mb, src) in _MIRROR_QUEUE:
        part.boxes.append(mb)
        mb.u, mb.v = src.u, src.v
    return spec


S.pack_uv = _pack_uv_mirror_aware
