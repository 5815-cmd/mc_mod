# -*- coding: utf-8 -*-
import io

P = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace\工程\art\tools\gen_unique_weapons.py"
s = io.open(P, encoding="utf-8").read()

# 1) 帝境/鸿蒙镰刀改名并沿用死神镰刀外形（ARCH 已经是 sishen 同款 xukong 也要死神化）
s = s.replace('("虚空镰", "Void Scythe"), ("虚空·终焉战镰", "Void Finality War Scythe")',
              '("死神·帝镰", "Death Emperor Scythe"), ("死神·终焉镰", "Death Finality Scythe")')

# 2) 加 fx 骨（悬浮发光颗粒）与 呼吸/旋绕动画
add_fx = '''

def add_fx(geo, form, tier):
    """3D 特效：绕武器旋转的发光颗粒 + 呼吸灯（动画在 write_anim 里）。"""
    fx = geo.bone("fx", (0, 12.0, 0))
    count = 4 if tier < 7 else 6
    for k in range(count):
        a = 2 * math.pi * k / count + tier * 0.3
        r = 3.6 + 0.5 * (tier % 3)
        y = 11.0 + tier * 1.0 + k * 0.8
        fx.add((math.cos(a) * r - 0.45, y - 0.45, math.sin(a) * r - 0.45),
               (0.9, 0.9, 0.9), mat="rune")
    fx.add((-0.6, 12.0 + tier, -4.2), (1.2, 1.2, 1.2), mat="jade")


def write_anim(family, geo):
    bones = {}
    bone_names = {b.name for b in geo.bones}
    if "fx" in bone_names:
        bones["fx"] = {
            "rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]},
            "scale": {"0.0": [1, 1, 1], "1.0": [1.12, 1.12, 1.12],
                      "2.0": [1, 1, 1], "3.0": [1.12, 1.12, 1.12],
                      "4.0": [1, 1, 1]},
        }
    for name in ("ring_a", "ring_b"):
        if name in bone_names:
            direction = -360 if name == "ring_a" else 360
            bones[name] = {"rotation": {"0.0": [0, 0, 0], "4.0": [0, direction, 0]}}
    if "tassel" in bone_names:
        bones["tassel"] = {"rotation": {"0.0": [0, 0, -6], "1.0": [0, 0, 6],
                                        "2.0": [0, 0, -6], "3.0": [0, 0, 6],
                                        "4.0": [0, 0, -6]}}
    anim = {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 4.0, "bones": bones}}}
    import json as _json
    import os as _os
    p = _os.path.join(RES, "animations", family + ".animation.json")
    with open(p, "w", encoding="utf-8") as f:
        _json.dump(anim, f, ensure_ascii=False, indent=2)


'''
s = s.replace("def main():", add_fx + "def main():")

# 3) build 循环里调用 add_fx，并用 write_anim 替代空 idle
s = s.replace("            geo = bw.BUILDERS[form](spec)\n", "            geo = bw.BUILDERS[form](spec)\n")
s = s.replace("            embellish(geo, form, tier)\n",
              "            embellish(geo, form, tier)\n            add_fx(geo, form, tier)\n", 1)
s = s.replace('''            anim = {"format_version": "1.8.0", "animations": {
                "idle": {"loop": True, "animation_length": 2.0, "bones": {}}}}
            with open(os.path.join(RES, "animations", family + ".animation.json"),
                      "w", encoding="utf-8") as f:
                json.dump(anim, f, ensure_ascii=False, indent=2)''',
              "            write_anim(family, geo)")

io.open(P, "w", encoding="utf-8").write(s)
print("weapon fx/anim patched")
