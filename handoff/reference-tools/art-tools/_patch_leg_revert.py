# -*- coding: utf-8 -*-
import io

BASE = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace\工程\art\tools"
P = BASE + r"\armor_careers.py"
s = io.open(P, encoding="utf-8").read()

# 1) 披风恢复为"上身披风"：不再挂腿（腿上的下摆会让腿部动作看起来不贴合）
s = s.replace('''        cape = geo.bone("cape", (0, 23.0, 3.0), parent="armorBody")
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
            add(leg, 3.3 * sign, 4.1 * sign, 2.2, 14.0, 2.8, 3.35, "cloth")''','''        cape = geo.bone("cape", (0, 23.0, 3.0), parent="armorBody")
        w = 4.6 if career != "ti" else 5.4
        # 只保留披肩上段（不过腰），避免下摆被误看成"不会动的腿甲"
        for i in range(4):
            x0 = -w + i * (2 * w / 4)
            cape.add((x0, 18.6, 3.1), (2 * w / 4 - 0.2, 4.4, 0.55), mat="cloth")''')

# 2) 躯干上的低腰甲片抬到腰线以上，避免遮挡/误判成腿甲
s = s.replace('add(body, -4.4, 4.4, 13.0, 14.4, -4.1, 3.2, "steel" if not soft else "cloth")',
              'add(body, -4.4, 4.4, 14.2, 15.0, -4.1, 3.2, "steel" if not soft else "cloth")')
s = s.replace('add(body, -5.6, -4.8, 13.6, 22.4, -2.4, 2.4, plate)',
              'add(body, -5.6, -4.8, 15.0, 22.4, -2.4, 2.4, plate)')
s = s.replace('add(body, 4.8, 5.6, 13.6, 22.4, -2.4, 2.4, plate)',
              'add(body, 4.8, 5.6, 15.0, 22.4, -2.4, 2.4, plate)')
s = s.replace('add(body, -3.2, 3.2, 13.6, 22.0, 3.0, 3.6, plate)',
              'add(body, -3.2, 3.2, 15.0, 22.0, 3.0, 3.6, plate)')
io.open(P, "w", encoding="utf-8").write(s)
print("cape/body low pieces adjusted")
