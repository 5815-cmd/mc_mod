"""离线校验：模型贴地 + 形态不散架（不编译、不进游戏，纯源码静态校验）。

四条断言（对应历史上出现过的每一类"模型出问题"）
============================================================================
A. **贴地**：把渲染器里真实的 `GROUND_LIFT` 算进去之后，模型的最低点必须落在
   世界 y = 0（地面）上，误差 ≤ 0.03 格（≈0.5 像素）；并且**有腿的模型**
   脚底必须正好在模型空间 y = 24、`GROUND_LIFT` 必须为 0
   （24 是本项目 1.20.1 的基准：原版 `LivingEntityRenderer` 会 translate
   `(0, -1.501, 0)`，1.501 格 = 24.016 像素）。

B. **抬升不许写在 scale() 里**：`scale()` 那一层会与动画、骑乘、品阶体型叠加，
   表现是"走动时模型越来越高"。断言 `scale()` 方法体里没有 `translate(...)`、
   没有 `groundAlign(...)`；有 `GROUND_LIFT` 的文件必须在 `setupRotations()`
   里调用它。

C. **不许写躯干的 zRot**：`body`/`torso`/`chest`/`upper_body`/`haunch`/`pelvis`
   一旦自己滚转，头/手/腿（它们的兄弟节点）不会跟着转 —— 接缝处就是"散架"。
   人形更糟：原版 `HumanoidModel.setupAnim` 会把躯干旋转复制给头与双臂。

D. **呼吸/起伏必须是绝对赋值**：`setupAnim` 每帧都跑，能不能累加取决于"有没有人
   替我们重置部件"。这两件事都用字节码核实过（`tools/probe_vanilla_pose_reset.py`）：
     * 自建模型继承 `ColorableAgeableListModel`，它的 `setupAnim` 是**抽象方法**，
       原版不会碰我们的部件 —— `p.y = p.y + v`、`p.xRot += v`、`xScale *= k`
       都是**真的会跨帧累加**（模型越涨越大 / 部件越离越远）。
     * 人形模型继承 `HumanoidModel`，它的 `setupAnim` 每帧会把 `body.y`、`head.y`、
       `rightArm.y`… 写成绝对值（0.0 / 蹲下时 3.2 / 4.2 / 5.2），所以
       `this.body.y += breath` 实际上不会漂；但 `pouch` 这类**自建部件**
       不在原版的管辖范围内，`pouch.xRot += ...` 会真的漂。
   规则统一按"绝对赋值"要求 —— 因为这条规则要能在任何部件上成立。

用法：  python tools/check_model_ground.py          （全部通过时退出码 0）
"""
import io
import os
import re
import sys

WS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(WS, "tools"))
import model_parse_lib as L  # noqa: E402

CLIENT = os.path.join(WS, "xiuxian-mod", "src", "main", "java", "com", "hongmeng",
                      "xiuxian", "client")

# --client DIR 可以指到别的目录（例如 tools/_backup_client_before），
# 用来验证"这套断言在修之前确实是 FAIL 的"——断言有牙才有意义。
if "--client" in sys.argv:
    CLIENT = os.path.abspath(sys.argv[sys.argv.index("--client") + 1])
BASELINE = 24.0            # 脚底该在的模型空间像素 y
FOOT_TOL = 0.5             # 允许的像素误差
LAND_TOL = 0.03            # 允许的落地误差（格）= 0.5 像素
LIFT_TOL = 0.02            # GROUND_LIFT 与几何算出的值允许的差（格）

# 每个渲染器 -> 模型骨架所在文件（契约灵狼复用灵狼的骨架）
# 灵驹（SpiritSteedRenderer）已按玩家要求整体删除，故不在此表内。
MODELS = [
    ("BurrowMartenRenderer.java", "BurrowMartenRenderer.java", "土行貂"),
    ("ContractEagleRenderer.java", "ContractEagleRenderer.java", "契约灵鹰"),
    ("ContractWolfRenderer.java", "SpiritWolfRenderer.java", "契约灵狼"),
    ("CultivationHumanoidModel.java", "CultivationHumanoidModel.java", "人形怪"),
    ("CultivationNpcModel.java", "CultivationNpcModel.java", "NPC"),
    ("FlameApeRenderer.java", "FlameApeRenderer.java", "烈焰猿"),
    ("JiaolongRenderer.java", "JiaolongRenderer.java", "蛟龙"),
    ("SpiritWolfRenderer.java", "SpiritWolfRenderer.java", "灵狼"),
    ("StoneGolemRenderer.java", "StoneGolemRenderer.java", "石傀"),
    ("ThunderEagleRenderer.java", "ThunderEagleRenderer.java", "雷鹰"),
    ("VenomToadRenderer.java", "VenomToadRenderer.java", "毒涎蟾"),
    ("VoidSerpentRenderer.java", "VoidSerpentRenderer.java", "虚空蛟"),
]

# 躯干部件：写它们的 zRot 就会让躯干与头/四肢各转各的
TORSO = r'(?:body|torso|chest|upper_?[Bb]ody|haunch|pelvis)'

fails = []
checks = []


def read(name):
    return io.open(os.path.join(CLIENT, name), encoding="utf-8").read()


def code_lines(src):
    """去掉注释后的源码（**行号不变**）：不然新写的注释里提到 `+=` 会被自己判为违规。"""
    return L.strip_comments(src).splitlines()


def check(tag, ok, detail):
    checks.append((tag, ok, detail))
    if not ok:
        fails.append("%s —— %s" % (tag, detail))
    return ok


def lift_of(src):
    m = re.search(r'GROUND_LIFT\s*=\s*(-?[\d.]+)D', src)
    return float(m.group(1)) if m else None


# ----------------------------------------------------------------------
def check_A_ground():
    print("── A. 贴地：最低点落在世界 y=0 ──────────────────────────────────")
    print("%-14s %-9s %-9s %-11s %-11s %-9s %s" %
          ("模型", "脚底y", "整体最低", "GROUND_LIFT", "落地误差(格)", "有腿", "判定"))
    for renderer, modelfile, cn in MODELS:
        src = read(renderer)
        parts, _tex = L.parse_model_file(os.path.join(CLIENT, modelfile))
        st = L.summary(parts)
        lift = lift_of(src) or 0.0
        low = st["foot_all"]
        # 世界高度：提升量 + (基准 24.016 - 模型像素 y) / 16
        land = lift + (24.016 - low) / 16.0
        has_legs = st["foot_leg"] is not None
        foot = st["foot_leg"] if has_legs else low
        ok = abs(land) <= LAND_TOL
        if has_legs:
            ok = ok and abs(foot - BASELINE) <= FOOT_TOL
            ok = ok and abs(lift - (low - BASELINE) / 16.0) <= LIFT_TOL
        tag = "%s(%s)" % (cn, renderer[:-5])
        check("A " + tag, ok,
              "落地误差 %+.3f 格 / 脚底 %.1f / lift %+.2f" % (land, foot, lift))
        print("%-14s %-9.1f %-9.1f %-11s %-11s %-9s %s" %
              (cn, foot, low, "%+.2f" % lift, "%+.3f" % land,
               "是" if has_legs else "无(蛇身)", "PASS" if ok else "FAIL"))
    print()


def check_B_scale_hook():
    print("── B. 抬升只许写在 setupRotations，不许写进 scale() ─────────────")
    for renderer, _m, cn in MODELS:
        src = read(renderer)
        body = L.extract_body(src, r'protected void scale\s*\(')
        problems = []
        if body:
            if re.search(r'\btranslate\s*\(', body):
                problems.append("scale() 里有 translate()")
            if "groundAlign" in body:
                problems.append("scale() 里调了 groundAlign()")
        if "GROUND_LIFT" in src:
            hook = L.extract_body(src, r'protected void setupRotations\s*\(')
            if not hook or "groundAlign" not in hook:
                problems.append("有 GROUND_LIFT 但 setupRotations() 里没调 groundAlign()")
        ok = not problems
        check("B %s" % renderer[:-5], ok, "; ".join(problems) or "scale() 干净")
        print("   %-26s %s" % (renderer[:-5], "PASS" if ok else "FAIL " + "; ".join(problems)))
    print()


def check_C_torso_zrot():
    print("── C. 不许给躯干写 zRot（会让头/手/腿各转各的 = 散架）──────────")
    pat = re.compile(r'(?:this\.)?' + TORSO + r'\.zRot\s*(?:[-+*/]?=)')
    for renderer, modelfile, _cn in MODELS:
        problems = []
        for f in {renderer, modelfile}:
            src = read(f)
            for i, line in enumerate(code_lines(src), 1):
                if pat.search(line):
                    problems.append("%s:%d %s" % (f, i, line.strip()[:60]))
        ok = not problems
        check("C %s" % renderer[:-5], ok, "; ".join(problems) or "没有躯干 zRot 写入")
        print("   %-26s %s" % (renderer[:-5], "PASS" if ok else "FAIL " + "; ".join(problems)))
    print()


def check_D_absolute_breath():
    print("── D. 呼吸/起伏必须绝对赋值（不能跨帧累加）────────────────────")
    # ① 缩放不能 *= ② 部件不能 += ③ 不能写 p.y = p.y + ...
    p_scale = re.compile(r'\.(?:x|y|z)Scale\s*\*=')
    p_plus = re.compile(r'this\.\w+\.(?:xRot|yRot|zRot|x|y|z)\s*\+=')
    p_self = re.compile(r'this\.(\w+)\.(xRot|yRot|zRot|x|y|z)\s*=\s*this\.\1\.\2\s*\+')
    for renderer, modelfile, _cn in MODELS:
        problems = []
        for f in {renderer, modelfile}:
            src = read(f)
            for i, line in enumerate(code_lines(src), 1):
                for p, why in ((p_scale, "*= 缩放会逐帧累乘"),
                               (p_plus, "+= 会逐帧累加"),
                               (p_self, "自引用赋值会逐帧累加")):
                    if p.search(line):
                        problems.append("%s:%d [%s] %s" % (f, i, why, line.strip()[:52]))
        ok = not problems
        check("D %s" % renderer[:-5], ok, "; ".join(problems) or "全部绝对赋值")
        print("   %-26s %s" % (renderer[:-5], "PASS" if ok else "FAIL " + "; ".join(problems)))
    print()


def main():
    print("=" * 96)
    print("模型贴地 / 形态 离线校验  tools/check_model_ground.py")
    print("基准：1.20.1 原版把模型空间 y=24 画到地面（render 前 translate(0, -1.501, 0)）")
    print("检查目录：" + CLIENT)
    print("=" * 96)
    print()
    check_A_ground()
    check_B_scale_hook()
    check_C_torso_zrot()
    check_D_absolute_breath()

    passed = sum(1 for _t, ok, _d in checks if ok)
    print("=" * 96)
    print("合计 %d 项断言：通过 %d，失败 %d" % (len(checks), passed, len(checks) - passed))
    if fails:
        print()
        print("失败明细：")
        for f in fails:
            print("   × " + f)
        print()
        print("结果：FAIL")
        return 1
    print("结果：PASS —— 每个模型的脚底都踩在 y=24，抬升在 setupRotations，")
    print("      没有躯干 zRot 写入，呼吸全部是绝对赋值。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
