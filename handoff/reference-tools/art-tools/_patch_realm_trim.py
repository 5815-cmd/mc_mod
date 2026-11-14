# -*- coding: utf-8 -*-
import io

P = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace\工程\art\tools\armor_careers.py"
NEW = '''def realm_trim(geo, career, realm):
    """同一职业的不同境界也加境界专属件，避免同职业 10 套只是换色。"""
    body = next((b for b in geo.bones if b.name == "armorBody"), None)
    head = next((b for b in geo.bones if b.name == "armorHead"), None)
    arms = [b for b in geo.bones if b.name in ("armorRightArm", "armorLeftArm")]
    legs = [b for b in geo.bones if b.name in ("armorRightLeg", "armorLeftLeg")]
    if body is not None:
        for k in range(1 + realm // 4):
            y = 15.4 + k * 2.0
            add(body, -0.8, 0.8, y, y + 1.2, -3.75, -3.35, "jade")
        if realm >= 3:
            for s in (-1, 1):
                add(body, 5.4 * s - 0.6, 5.4 * s + 0.6, 18.0, 22.0,
                    -1.0, 1.0, "steel")
        if realm >= 6:
            add(body, -1.2, 1.2, 12.6, 13.4, 3.2, 3.8, "jade")
        if realm >= 8:
            for s in (-1, 1):
                add(body, 5.8 * s - 0.7, 5.8 * s + 0.7, 24.0, 25.4,
                    -0.7, 0.7, "rune")
    if head is not None:
        add(head, -0.7, 0.7, 28.8, 29.8, -4.9, -4.4, "jade")
        if realm >= 4:
            for s in (-1, 1):
                add(head, 4.4 * s, 5.4 * s, 30.2, 31.4, -0.7, 0.7, "steel")
        if realm >= 7:
            add(head, -1.0, 1.0, 31.6, 33.0, -4.6, -4.1, "rune")
        if realm >= 9:
            for k in range(6):
                a = math.pi * (k + 1) / 7.0
                add(head, math.cos(a) * 4.8 - 0.3, math.cos(a) * 4.8 + 0.3,
                    31.0 + math.sin(a) * 2.0, 31.6 + math.sin(a) * 2.0,
                    4.6, 5.1, "jade")
    for arm in arms:
        sign = 1 if arm.name.endswith("LeftArm") else -1
        if realm >= 5:
            add(arm, 3.6 * sign, 8.4 * sign, 12.6, 13.4, -2.6, 2.6, "steel")
        if realm >= 8:
            add(arm, 7.8 * sign - 0.5, 7.8 * sign + 0.5, 24.0, 25.2,
                -1.0, 1.0, "rune")
    for leg in legs:
        sign = 1 if leg.name.endswith("LeftLeg") else -1
        if realm >= 2:
            add(leg, 0.6 * sign, 3.8 * sign, 8.0, 9.0, -3.3, -2.7, "steel")
        if realm >= 6:
            add(leg, 0.6 * sign, 3.6 * sign, 4.0, 4.8, -3.3, -2.8, "jade")


'''

s = io.open(P, encoding="utf-8").read()
a = s.index("def realm_trim(")
b = s.index("BUILDERS = {", a)
s = s[:a] + NEW + s[b:]
io.open(P, "w", encoding="utf-8").write(s)
print("realm_trim fixed")
