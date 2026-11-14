# -*- coding: utf-8 -*-
import io

P = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace\工程\art\tools\armor_careers.py"
s = io.open(P, encoding="utf-8").read()

FUNC = '''def detail_3d(geo, career):
    """3D 层次强化：给每套甲加前后凸起甲片、侧甲、铆钉、盔沿、膝甲、靴头，
    避免看起来像一层平面贴图。"""
    body = next((b for b in geo.bones if b.name == "armorBody"), None)
    head = next((b for b in geo.bones if b.name == "armorHead"), None)
    arms = [b for b in geo.bones if b.name in ("armorRightArm", "armorLeftArm")]
    legs = [b for b in geo.bones if b.name in ("armorRightLeg", "armorLeftLeg")]
    boots = [b for b in geo.bones if b.name in ("armorRightBoot", "armorLeftBoot")]

    soft = career in ("jian", "gui")
    plate = "cloth" if soft else "metal"
    rim = "gold" if career in ("jian", "xue", "ti") else "jade"

    if body is not None:
        add(body, -3.9, 3.9, 15.2, 21.8, -4.0, -3.5, plate)
        add(body, -4.4, 4.4, 13.0, 14.4, -4.1, 3.2, "steel" if not soft else "cloth")
        add(body, -3.6, 3.6, 18.6, 19.4, -4.2, -3.9, rim)
        add(body, -5.6, -4.8, 13.6, 22.4, -2.4, 2.4, plate)
        add(body, 4.8, 5.6, 13.6, 22.4, -2.4, 2.4, plate)
        add(body, -3.2, 3.2, 13.6, 22.0, 3.0, 3.6, plate)
        for i in range(4):
            x = -3.4 + i * 2.2
            add(body, x - 0.25, x + 0.25, 20.6, 21.2, -4.25, -4.0, rim)
            add(body, x - 0.25, x + 0.25, 14.0, 14.6, -4.25, -4.0, rim)
        add(body, -1.0, 1.0, 11.8, 13.2, -4.2, -3.6, "gold" if career in ("jian", "xue") else "jade")
    if head is not None:
        add(head, -4.6, 4.6, 29.0, 29.8, -5.2, 5.2, plate)
        add(head, -4.8, 4.8, 27.6, 29.0, -5.4, 5.4, "steel" if not soft else "cloth")
        add(head, -5.2, 5.2, 30.4, 31.2, -4.4, 4.4, rim)
        for side in (-1, 1):
            add(head, side * 4.3, side * 5.4, 26.0, 30.2, -2.2, 2.2, plate)
    for arm in arms:
        sign = 1 if arm.name.endswith("LeftArm") else -1
        add(arm, 6.8 * sign, 9.6 * sign, 21.4, 24.8, -2.8, 2.8, "steel" if not soft else "cloth")
        add(arm, 3.4 * sign, 8.8 * sign, 14.6, 15.4, -3.0, 3.0, rim)
        add(arm, 7.0 * sign, 7.8 * sign, 12.8, 14.8, -1.8, 1.8, "jade")
    for leg in legs:
        sign = 1 if leg.name.endswith("LeftLeg") else -1
        add(leg, 0.4 * sign, 3.8 * sign, 4.4, 11.8, -3.9, -3.35, plate)
        add(leg, 3.6 * sign, 4.6 * sign, 4.0, 11.6, -2.4, 2.4, plate)
        add(leg, 0.6 * sign, 3.6 * sign, 7.8, 8.8, -4.1, -3.7, "steel" if not soft else "cloth")
        add(leg, 0.5 * sign, 3.7 * sign, 5.0, 11.6, 3.0, 3.5, plate)
    for b in boots:
        sign = 1 if b.name.endswith("LeftBoot") else -1
        add(b, 0.3 * sign, 4.2 * sign, 0.0, 1.6, -3.8, 2.6, "steel" if not soft else "cloth")
        add(b, 0.6 * sign, 3.9 * sign, 2.0, 3.2, -3.8, -3.2, rim)


'''

s = s.replace("def build(career, realm):", FUNC + "def build(career, realm):")
s = s.replace("    add_dynamic_bones(geo, career, realm)\n",
              "    detail_3d(geo, career)\n    add_dynamic_bones(geo, career, realm)\n")
io.open(P, "w", encoding="utf-8").write(s)
print("armor 3d detail patched")
