# -*- coding: utf-8 -*-
import io

BASE = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace\工程\art\tools"

# ---------- 1) armor_careers.py：加 fx/cape/veil/lantern 动态骨 ----------
P = BASE + r"\armor_careers.py"
s = io.open(P, encoding="utf-8").read()

FUNC = '''def add_dynamic_bones(geo, career, realm):
    """动态件：呼吸灯 fx、披风 cape、纱幔 veil、魂灯 lantern_l/r（单独骨，动画用）。"""
    body = next((b for b in geo.bones if b.name == "armorBody"), None)
    head = next((b for b in geo.bones if b.name == "armorHead"), None)

    fx = geo.bone("fx", (0, 18.0, 0), parent="armorBody")
    for k in range(3 + realm // 4):
        import math as _m
        a = 2 * _m.pi * k / max(1, 3 + realm // 4) + realm * 0.4
        r = 4.6 + 0.4 * (realm % 3)
        y = 16.0 + realm * 0.5 + k * 1.1
        fx.add((_m.cos(a) * r - 0.45, y, _m.sin(a) * r - 0.45),
               (0.9, 0.9, 0.9), mat="rune")

    if career in ("xue", "mo", "ti"):
        cape = geo.bone("cape", (0, 23.0, 3.0), parent="armorBody")
        w = 4.6 if career != "ti" else 5.4
        for i in range(4):
            x0 = -w + i * (2 * w / 4)
            length = 9.0 + (i % 2) * 1.6
            cape.add((x0, 22.0 - length, 3.1), (2 * w / 4 - 0.2, length, 0.55),
                     mat="cloth")
        # 披风底部破边
        for i in range(3):
            cape.add((-w + i * 3.0, 22.0 - 10.6 - (i % 2) * 1.2, 3.15),
                     (1.6, 1.6, 0.5), mat="cloth")

    if career in ("gui",):
        veil = geo.bone("veil", (0, 25.0, 3.4), parent="armorHead")
        for i in range(4):
            x0 = -2.8 + i * 1.6
            length = 9.0 + (i % 2) * 3.0
            veil.add((x0, 25.0 - length, 4.2), (1.3, length, 0.4), mat="cloth")
        for side, name in ((-1, "lantern_l"), (1, "lantern_r")):
            lan = geo.bone(name, (side * 6.2, 26.0, 0.0), parent="armorBody")
            lan.add((-0.9, -1.4, -0.9), (1.8, 2.8, 1.8), mat="metal")
            lan.add((-0.6, -0.9, -1.15), (1.2, 1.8, 2.3), mat="rune")
            lan.add((-0.4, 1.4, -0.4), (0.8, 0.8, 0.8), mat="steel")


'''
s = s.replace("def build(career, realm):", FUNC + "def build(career, realm):")
s = s.replace("    rear_leg_cover(geo, career)\n",
              "    rear_leg_cover(geo, career)\n    add_dynamic_bones(geo, career, realm)\n")
io.open(P, "w", encoding="utf-8").write(s)

# ---------- 2) gen_unique_armor.py：写入动态动画 ----------
P2 = BASE + r"\gen_unique_armor.py"
g = io.open(P2, encoding="utf-8").read()
WRITE = '''

def write_anim(family, geo):
    names = {b.name for b in geo.bones}
    bones = {}
    if "fx" in names:
        bones["fx"] = {
            "rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]},
            "scale": {"0.0": [1, 1, 1], "1.0": [1.15, 1.15, 1.15],
                      "2.0": [1, 1, 1], "3.0": [1.15, 1.15, 1.15],
                      "4.0": [1, 1, 1]},
        }
    if "cape" in names:
        bones["cape"] = {"rotation": {"0.0": [-2, 0, 0], "1.0": [2, 0, 1.5],
                                      "2.0": [-2, 0, 0], "3.0": [2, 0, -1.5],
                                      "4.0": [-2, 0, 0]}}
    if "veil" in names:
        bones["veil"] = {"rotation": {"0.0": [2, 0, -1], "1.0": [-2, 0, 1],
                                      "2.0": [2, 0, -1], "3.0": [-2, 0, 1],
                                      "4.0": [2, 0, -1]}}
    for name in ("lantern_l", "lantern_r"):
        if name in names:
            bones[name] = {
                "position": {"0.0": [0, 0, 0], "1.0": [0, 0.25, 0],
                              "2.0": [0, 0, 0], "3.0": [0, 0.25, 0],
                              "4.0": [0, 0, 0]},
                "rotation": {"0.0": [0, 0, 0], "4.0": [0, 360, 0]},
            }
    anim = {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 4.0, "bones": bones}}}
    import json as _json
    import os as _os
    with open(_os.path.join(RES, "animations", family + ".animation.json"),
              "w", encoding="utf-8") as f:
        _json.dump(anim, f, ensure_ascii=False, indent=2)


'''
g = g.replace("def main():", WRITE + "def main():")
g = g.replace('''            anim = {"format_version": "1.8.0", "animations": {
                "idle": {"loop": True, "animation_length": 2.0, "bones": {}}}}
            with open(os.path.join(RES, "animations", family + ".animation.json"),
                      "w", encoding="utf-8") as f:
                json.dump(anim, f, ensure_ascii=False, indent=2)''',
              "            write_anim(family, geo)")
io.open(P2, "w", encoding="utf-8").write(g)
print("armor dynamic bones + animations patched")
