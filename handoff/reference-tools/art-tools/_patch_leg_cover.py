# -*- coding: utf-8 -*-
import io

P = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace\工程\art\tools\armor_careers.py"
s = io.open(P, encoding="utf-8").read()

FUNC = '''def rear_leg_cover(geo, career):
    """腿甲后侧包裹：之前只做了前面/侧面，第三人称从背后看是空的。"""
    mat = "cloth" if career in ("jian", "gui") else "metal"
    boot_mat = "steel" if career not in ("jian", "gui") else "cloth"
    for b in geo.bones:
        if b.name in ("armorRightLeg", "armorLeftLeg"):
            sign = 1 if b.name.endswith("LeftLeg") else -1
            add(b, 0.4 * sign, 3.7 * sign, 3.8, 12.4, 2.15, 2.8, mat)
            add(b, 3.4 * sign, 4.2 * sign, 4.2, 12.2, 1.8, 2.7, mat)
            if career not in ("jian", "gui"):
                add(b, 0.6 * sign, 3.5 * sign, 6.6, 7.4, 2.6, 2.95, "steel")
        if b.name in ("armorRightBoot", "armorLeftBoot"):
            sign = 1 if b.name.endswith("LeftBoot") else -1
            add(b, 0.5 * sign, 4.0 * sign, 0.2, 3.6, 2.05, 2.7, boot_mat)


'''

s = s.replace("def build(career, realm):", FUNC + "def build(career, realm):")
s = s.replace("    geo = BUILDERS[career](realm, p)\n",
              "    geo = BUILDERS[career](realm, p)\n    rear_leg_cover(geo, career)\n")
io.open(P, "w", encoding="utf-8").write(s)
print("rear leg cover patched")
