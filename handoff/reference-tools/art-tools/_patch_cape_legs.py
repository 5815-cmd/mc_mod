# -*- coding: utf-8 -*-
import io

P = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace\工程\art\tools\armor_careers.py"
s = io.open(P, encoding="utf-8").read()

# 披风缩短到腰；下摆/后摆改挂到左右腿骨上，跟着腿走（解决"腿甲后面不随人物移动"）
s = s.replace('''        cape = geo.bone("cape", (0, 23.0, 3.0), parent="armorBody")
        w = 4.6 if career != "ti" else 5.4
        for i in range(4):
            x0 = -w + i * (2 * w / 4)
            length = 9.0 + (i % 2) * 1.6
            cape.add((x0, 22.0 - length, 3.1), (2 * w / 4 - 0.2, length, 0.55),
                     mat="cloth")
        # 披风底部破边
        for i in range(3):
            cape.add((-w + i * 3.0, 22.0 - 10.6 - (i % 2) * 1.2, 3.15),
                     (1.6, 1.6, 0.5), mat="cloth")''','''        cape = geo.bone("cape", (0, 23.0, 3.0), parent="armorBody")
        w = 4.6 if career != "ti" else 5.4
        for i in range(4):
            x0 = -w + i * (2 * w / 4)
            cape.add((x0, 21.0, 3.1), (2 * w / 4 - 0.2, 2.6, 0.55), mat="cloth")
        # 披风下摆分到左右腿骨：走路时跟着腿摆动，不再"定在身后"
        for leg in (b for b in geo.bones if b.name in ("armorRightLeg", "armorLeftLeg")):
            sign = 1 if leg.name.endswith("LeftLeg") else -1
            for k in range(2):
                add(leg, (0.5 + k * 1.6) * sign, (1.9 + k * 1.6) * sign,
                    2.0, 14.0, 2.85, 3.4, "cloth")
            add(leg, 3.3 * sign, 4.1 * sign, 2.2, 14.0, 2.8, 3.35, "cloth")''')

io.open(P, "w", encoding="utf-8").write(s)
print("cape tails moved to legs")
