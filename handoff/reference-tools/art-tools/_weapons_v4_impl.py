    # ---------------- 镰刀 ----------------
    dict(key="gouhun", form="scythe", cn="勾魂镰", tiers=(1, 3),
         color=P((120, 100, 150), (192, 174, 222), (40, 28, 60), (190, 120, 255),
                 (190, 120, 255), glow=(190, 120, 255), hem=(70, 44, 110),
                 gold=(190, 170, 120), grip=(44, 30, 60)),
         shape=dict(shaft=27, arc_r=20.0, sweep=108, blade_h=9.0, skull=True,
                    moon=False, floats=0, chains=True)),
    dict(key="xueyue", form="scythe", cn="血月弯镰", tiers=(4, 6),
         color=P((150, 62, 62), (222, 142, 132), (50, 8, 10), (255, 90, 90),
                 (255, 84, 84), glow=(255, 80, 80), hem=(120, 12, 18),
                 gold=(210, 170, 100), grip=(58, 10, 12)),
         shape=dict(shaft=27, arc_r=21.0, sweep=116, blade_h=9.6, skull=False,
                    moon=True, floats=0, chains=False)),
    dict(key="yougu", form="scythe", cn="幽骨冥纹镰", tiers=(7, 8),
         color=P((190, 184, 166), (232, 228, 212), (30, 50, 50), (102, 242, 200),
                 (102, 242, 200), glow=(102, 242, 200), hem=(36, 92, 84),
                 gold=(150, 160, 120), grip=(40, 60, 56)),
         shape=dict(shaft=28, arc_r=20.5, sweep=112, blade_h=9.2, bone_shaft=True,
                    skull=False, moon=False, floats=0, chains=False)),
    dict(key="xukong", form="scythe", cn="虚空割裂镰", tiers=(9, 10),
         color=P((52, 44, 66), (120, 104, 160), (18, 14, 26), (176, 96, 255),
                 (176, 96, 255), glow=(176, 96, 255), hem=(60, 30, 90),
                 gold=(140, 120, 180), grip=(24, 18, 34)),
         shape=dict(shaft=29, arc_r=22.0, sweep=120, blade_h=10.0, skull=False,
                    moon=False, floats=3, chains=False, rift=True)),

    # ---------------- 长枪 ----------------
    dict(key="pojun", form="spear", cn="破军枪", tiers=(1, 3),
         color=P((126, 124, 120), (206, 202, 194), (56, 42, 24), (214, 172, 88),
                 (240, 210, 120), glow=(240, 210, 120), hem=(96, 72, 34),
                 gold=(232, 196, 110), grip=(58, 42, 24), tassel=(210, 46, 40)),
         shape=dict(shaft=44, head_len=20, head_w=7.6, tassel="red",
                    cloud_rings=0, lightning=False, lugs=True, butt=True)),
    dict(key="youlong", form="spear", cn="游龙穿云枪", tiers=(4, 6),
         color=P((168, 210, 200), (236, 250, 246), (28, 66, 58), (120, 255, 220),
                 (140, 255, 230), glow=(140, 255, 230), hem=(40, 110, 96),
                 gold=(200, 220, 160), grip=(30, 60, 54), tassel=(160, 250, 225)),
         shape=dict(shaft=46, head_len=22, head_w=5.6, tassel="white",
                    cloud_rings=2, lightning=False, snake_head=True, lugs=True,
                    butt=True)),
    dict(key="leiji", form="spear", cn="雷脊玄铁枪", tiers=(7, 8),
         color=P((88, 88, 128), (168, 168, 220), (20, 20, 50), (150, 132, 255),
                 (170, 150, 255), glow=(170, 150, 255), hem=(50, 40, 100),
                 gold=(180, 170, 220), grip=(28, 24, 60), tassel=(170, 160, 255)),
         shape=dict(shaft=46, head_len=18, head_w=6.4, tassel="blue",
                    lightning=True, serrated_head=True, lugs=True, butt=True)),
    dict(key="xueying", form="spear", cn="血缨魔纹枪", tiers=(9, 10),
         color=P((132, 72, 72), (202, 152, 152), (58, 10, 12), (255, 62, 62),
                 (255, 70, 70), glow=(255, 66, 66), hem=(110, 10, 16),
                 gold=(170, 150, 100), grip=(52, 10, 14), tassel=(240, 40, 50)),
         shape=dict(shaft=48, head_len=21, head_w=7.0, tassel="big",
                    groove=True, rune_bars=5, lugs=True, butt=True)),

    # ---------------- 棍 ----------------
    dict(key="jiangmo", form="staff", cn="降魔棍", tiers=(1, 3),
         color=P((124, 94, 52), (194, 162, 112), (70, 50, 26), (220, 180, 90),
                 (240, 210, 120), glow=(240, 210, 120), hem=(100, 70, 30),
                 gold=(226, 190, 96), grip=(64, 46, 24)),
         shape=dict(length=42, radius=2.9)),
    dict(key="panlong", form="staff", cn="盘龙棍", tiers=(4, 6),
         color=P((152, 122, 42), (232, 202, 116), (44, 58, 38), (130, 245, 170),
                 (150, 255, 190), glow=(150, 255, 190), hem=(70, 100, 50),
                 gold=(240, 206, 110), grip=(54, 40, 26)),
         shape=dict(length=44, radius=3.0)),
    dict(key="zhenhun", form="staff", cn="镇魂玄木棍", tiers=(7, 8),
         color=P((62, 52, 42), (112, 96, 76), (34, 28, 20), (140, 255, 200),
                 (140, 255, 200), glow=(140, 255, 200), hem=(50, 70, 56),
                 gold=(130, 120, 90), grip=(40, 32, 22)),
         shape=dict(length=44, radius=2.9)),
    dict(key="shougu", form="staff", cn="兽骨铃纹棍", tiers=(9, 10),
         color=P((204, 192, 164), (242, 236, 216), (66, 52, 34), (210, 180, 120),
                 (230, 200, 130), glow=(230, 200, 130), hem=(90, 72, 44),
                 gold=(200, 170, 110), grip=(60, 46, 30)),
         shape=dict(length=44, radius=3.0)),
]


def build_scythe(spec):
    """镰刀：短杆 + 巨大月牙刃（刃根厚脊、外缘薄刃、刀尖收锋）+ 下端配重尖刺。"""
    g = Geo("geometry.career_scythe_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    S = sh["shaft"]
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    blade = g.bone("blade", (0, S, 0), parent="head")
    # 杆：深色木 + 皮革缠握 + 金属箍
    add(shaft, -2.2, 2.2, 0.0, S, -2.2, 2.2, "cloth")
    for y in range(3, int(S) - 4, 4):
        add(shaft, -2.55, 2.55, y, y + 1.9, -2.55, 2.55, "cloth")
        add(shaft, -2.65, 2.65, y - 0.35, y + 0.1, -2.65, 2.65, "steel")
    add(shaft, -2.8, 2.8, S - 5.0, S - 4.0, -2.8, 2.8, "metal")
    add(shaft, -2.8, 2.8, 2.0, 3.0, -2.8, 2.8, "metal")
    # 底部配重 + 尖刺
    add(shaft, -2.0, 2.0, -2.0, 0.4, -2.0, 2.0, "metal")
    add(shaft, -2.6, 2.6, -3.4, -1.6, -2.6, 2.6, "metal")
    add(shaft, -1.1, 1.1, -6.6, -3.2, -1.1, 1.1, "steel")
    add(shaft, -0.45, 0.45, -8.4, -6.4, -0.45, 0.45, "steel")
    # 刃座（把刃"接"在杆上）
    add(head, -3.4, 3.4, S - 5.0, S + 3.0, -3.4, 3.4, "metal")
    add(head, -2.6, 2.6, S + 3.0, S + 4.6, -2.6, 2.6, "steel")
    add(head, -1.8, 1.8, S + 4.6, S + 6.2, -1.4, 1.4, "steel")
    if sh.get("skull"):
        add(head, -1.9, 1.9, S + 2.6, S + 6.4, -2.6, -1.6, "steel")
        add(head, -1.1, 1.1, S + 3.2, S + 5.0, -3.0, -2.6, "jade")
        add(head, -1.1, 1.1, S + 3.2, S + 5.0, 2.6, 3.0, "jade")
        for s in (-1, 1):
            add(head, 1.9 * s, 2.7 * s, S + 4.6, S + 6.8, -0.7, 0.7, "steel")
    if sh.get("moon"):
        for i in range(20):
            a = 2 * math.pi * i / 20
            cx, cy = math.cos(a) * 3.8, S + 5.2 + math.sin(a) * 3.8
            head.add((cx - 0.55, cy - 0.55, -0.55), (1.1, 1.1, 1.1), mat="jade")
    if sh.get("rift"):
        add(head, -1.6, 1.6, S + 2.2, S + 6.0, -3.6, -3.0, "rune")
        add(head, -0.8, 0.8, S + 3.0, S + 5.2, -4.1, -3.6, "jade")
        add(head, -2.4, 2.4, S + 4.0, S + 4.8, -3.4, 3.4, "jade")
    if sh.get("chains"):
        for s in (-1, 1):
            for i in range(4):
                add(head, 2.4 * s - 0.35, 2.4 * s + 0.35, S - 4.0 - i * 1.3,
                    S - 3.0 - i * 1.3, -0.35, 0.35, "steel")
    # 巨月牙：从刃座上方 (0,S+6) 起弧，扫出大弯
    r = sh["arc_r"]
    cy_arc = (S + 6.0) - r
    segs = 13
    prev_tip = None
    for i in range(segs):
        a0 = math.radians(92 - i * (sh["sweep"] / segs))
        a1 = math.radians(92 - (i + 1) * (sh["sweep"] / segs))
        p0 = (r * math.cos(a0), cy_arc + r * math.sin(a0))
        p1 = (r * math.cos(a1), cy_arc + r * math.sin(a1))
        mx, my = (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2
        length = math.dist(p0, p1) + 0.6
        deg = math.degrees(math.atan2(p1[1] - p0[1], p1[0] - p0[0]))
        h = sh["blade_h"] * (1.0 - 0.87 * (i / segs))
        # 刃体（薄）
        blade.add((mx - length / 2, my - 0.8, -0.5), (length, h, 1.0),
                  mat="metal", rotation=(0, 0, deg), pivot=(mx, my, 0))
        # 内缘厚脊（从刃体向圆心方向加厚）
        blade.add((mx - length / 2, my - 0.8, -0.9), (length, h * 0.42, 0.5),
                  mat="metal", rotation=(0, 0, deg), pivot=(mx, my, 0))
        # 外缘亮刃（更薄、更靠外）
        rad = math.radians(deg - 90)
        ox = mx + math.cos(rad) * (h * 0.46)
        oy = my + math.sin(rad) * (h * 0.46)
        blade.add((ox - length / 2, oy - 0.6, -0.28), (length, 1.2, 0.56),
                  mat="steel", rotation=(0, 0, deg), pivot=(ox, oy, 0))
        # 刀脊符文
        if i in (2, 5, 8, 10):
            blade.add((mx - 0.6, my - 0.55, -1.0), (1.2, 0.7, 0.45),
                      mat="rune", rotation=(0, 0, deg), pivot=(mx, my, 0))
        if i == segs - 1:
            prev_tip = (p1[0], p1[1], deg)
    # 收出尖锐锋尖
    if prev_tip:
        tx, ty, tdeg = prev_tip
        blade.add((tx - 1.6, ty + 0.4, -0.35), (3.0, 0.9, 0.7), mat="steel",
                  rotation=(0, 0, tdeg), pivot=(tx, ty, 0))
        blade.add((tx - 0.6, ty + 1.3, -0.25), (1.3, 0.8, 0.5), mat="steel",
                  rotation=(0, 0, tdeg), pivot=(tx, ty, 0))
    if sh.get("floats"):
        for i in range(sh["floats"]):
            a = math.radians(30 + i * 18)
            px = math.cos(a) * (r + 2.6)
            py = cy_arc + math.sin(a) * (r + 2.6)
            add(blade, px - 1.1, px + 1.1, py - 1.1, py + 1.1, -0.6, 0.6,
                "jade")
    if sh.get("bone_shaft"):
        for i in range(6):
            yy = 4.0 + i * (S - 8) / 6
            add(shaft, -2.4, 2.4, yy, yy + 2.2, -2.4, 2.4, "steel")
            add(shaft, -1.2, 1.2, yy + 2.2, yy + 2.8, -1.2, 1.2, "jade")
    return g


def build_spear(spec):
    """长枪：大枪头（叶形+中脊+尖锋）+ 枪头箍/护耳 + 杆箍 + 红缨 + 枪镦。"""
    g = Geo("geometry.career_spear_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    S = sh["shaft"]
    hw, hl = sh["head_w"], sh["head_len"]
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    add(shaft, -2.0, 2.0, 0.0, S, -2.0, 2.0, "cloth")
    for y in range(3, int(S) - 4, 4):
        add(shaft, -2.35, 2.35, y, y + 1.5, -2.35, 2.35, "cloth")
        add(shaft, -2.45, 2.45, y - 0.3, y + 0.1, -2.45, 2.45, "steel")
    # 枪镦（杆底）
    add(shaft, -2.6, 2.6, -2.6, 0.4, -2.6, 2.6, "metal")
    add(shaft, -1.4, 1.4, -5.4, -2.4, -1.4, 1.4, "steel")
    add(shaft, -0.5, 0.5, -6.8, -5.2, -0.5, 0.5, "steel")
    # 枪头座 + 三箍
    add(head, -2.6, 2.6, S - 5.0, S + 1.4, -2.6, 2.6, "metal")
    for k in range(3):
        add(head, -2.9, 2.9, S - 4.4 + k * 2.0, S - 3.4 + k * 2.0, -2.9, 2.9,
            "steel")
    # 护耳/翼
    if sh.get("lugs"):
        for s in (-1, 1):
            add(head, hw * 0.55 * s, hw * 1.25 * s, S - 1.6, S + 2.2, -0.7, 0.7,
                "metal")
            add(head, hw * 1.0 * s, hw * 1.5 * s, S + 0.4, S + 3.2, -0.5, 0.5,
                "rune")
            add(head, hw * 0.7 * s, hw * 1.05 * s, S - 3.2, S - 1.4, -0.5, 0.5,
                "steel")
    # 枪头：叶形，中脊，阶梯尖
    y0 = S + 1.4
    taper_blade(head, y0, y0 + hl, hw, hw * 0.20, hw * 0.52, hw * 0.14, 7)
    add(head, -hw * 0.55, hw * 0.55, y0, y0 + hl * 0.35, -hw * 0.42,
        hw * 0.42, "steel")
    for s in (-1, 1):
        x0 = hw * 0.46 * s
        add(head, min(x0, x0 - 0.18 * s), max(x0, x0 - 0.18 * s), y0 + 0.3,
            y0 + hl - 1.0, -hw * 0.20, hw * 0.20, "steel")
    step_point(head, y0 + hl, y0 + hl + 3.4, hw * 0.20, hw * 0.14, 3)
    if sh.get("groove"):
        add(head, -hw * 0.08, hw * 0.08, y0 + 0.4, y0 + hl - 0.8,
            -hw * 0.26, hw * 0.26, "rune")
    if sh.get("serrated_head"):
        for i in range(5):
            yy = y0 + 3.0 + i * 2.6
            add(head, -hw * 0.72, -hw * 0.48, yy, yy + 1.6, -0.5, 0.5, "steel")
            add(head, hw * 0.48, hw * 0.72, yy + 1.2, yy + 2.8, -0.5, 0.5,
                "steel")
    if sh.get("snake_head"):
        add(head, -hw * 0.42, hw * 0.42, S + 2.0, S + 4.6, -hw * 0.40,
            hw * 0.40, "jade")
        for s in (-1, 1):
            add(head, hw * 0.30 * s, hw * 0.52 * s, S + 3.0, S + 3.8,
                -hw * 0.5, -hw * 0.32, "rune")
    if sh.get("cloud_rings"):
        for i in range(sh["cloud_rings"]):
            ring_cubes(head, 12, 7.0 + i * 6.0, 3.8, 0.7,
                       mat="jade", phase=i * 0.4)
    if sh.get("rune_bars"):
        for i in range(sh["rune_bars"]):
            yy = 6.0 + i * 3.2
            add(shaft, -2.14, -1.96, yy, yy + 1.2, -1.2, 1.2, "rune")
            add(shaft, 1.96, 2.14, yy, yy + 1.2, -1.2, 1.2, "rune")
    # 红缨（多段布条，从枪头座下垂）
    tassel = g.bone("tassel", (0, S, 0), parent="shaft")
    if sh.get("tassel") == "big":
        for i, dz in enumerate((-3.0, -1.4, 0.2, 1.8, 3.2, -2.2, 2.4)):
            rot = 22 if i % 2 == 0 else -22
            add(tassel, -1.1, 1.1, -9.0 - (i % 3) * 1.0, -1.6, dz - 0.6,
                dz + 0.6, "cloth")
            tassel.cubes[-1].rotation = (rot, 0, 0)
            tassel.cubes[-1].pivot = (0, -1.6, dz)
    else:
        for i, dz in enumerate((-2.4, -0.9, 0.6, 2.1)):
            rot = 18 if i % 2 == 0 else -18
            add(tassel, -0.95, 0.95, -7.0 - (i % 2) * 0.8, -1.6, dz - 0.55,
                dz + 0.55, "cloth")
            tassel.cubes[-1].rotation = (rot, 0, 0)
            tassel.cubes[-1].pivot = (0, -1.6, dz)
    return g


def build_staff(spec):
    """棍：粗杆 + 缠绕握把 + 两种"法器头"（按名字）+ 下端配重/尾饰。"""
    g = Geo("geometry.career_staff_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    key = spec["key"]
    L, R = sh["length"], sh["radius"]
    shaft = g.bone("shaft", (0, 0, 0))
    # 杆身
    add(shaft, -R, R, 0.0, L, -R, R, "cloth")
    for y in range(4, int(L) - 4, 5):
        add(shaft, -R - 0.35, R + 0.35, y, y + 1.2, -R - 0.35, R + 0.35,
            "steel")
    # 中段握把（更粗的缠绕）
    add(shaft, -R - 0.5, R + 0.5, L * 0.42, L * 0.62, -R - 0.5, R + 0.5,
        "cloth")
    for y in range(int(L * 0.42), int(L * 0.62), 2):
        add(shaft, -R - 0.6, R + 0.6, y, y + 0.5, -R - 0.6, R + 0.6, "steel")
    # 两端金属箍 + 底配重
    add(shaft, -R - 1.1, R + 1.1, 0.0, 2.6, -R - 1.1, R + 1.1, "metal")
    add(shaft, -R - 0.5, R + 0.5, 2.6, 3.3, -R - 0.5, R + 0.5, "steel")
    add(shaft, -R - 1.1, R + 1.1, L - 2.6, L, -R - 1.1, R + 1.1, "metal")
    if key == "jiangmo":
        # 锡杖：三道大环 + 顶针 + 环坠
        add(shaft, -2.0, 2.0, L, L + 2.2, -2.0, 2.0, "metal")
        for i, rr in enumerate((4.4, 3.8, 3.2)):
            ring_cubes(shaft, 14, L + 3.2 + i * 2.6, rr, 0.8,
                       mat="jade", tilt=90.0)
        add(shaft, -0.7, 0.7, L + 11.0, L + 13.6, -0.7, 0.7, "steel")
        add(shaft, -0.35, 0.35, L + 13.6, L + 14.8, -0.35, 0.35, "rune")
        for s in (-1, 1):
            for i in range(3):
                add(shaft, s * 3.0 - 0.35, s * 3.0 + 0.35,
                    L + 3.0 + i * 2.6 - 1.8, L + 3.0 + i * 2.6 - 0.9,
                    -0.35, 0.35, "steel")
        add(shaft, -R - 0.4, R + 0.4, 8.0, 9.6, -R - 0.4, R + 0.4, "jade")
    elif key == "panlong":
        # 盘龙：杆身螺旋 + 龙首 + 龙尾
        for i in range(12):
            yy = 6.0 + i * (L - 18.0) / 12
            a = i * 2.2
            cx = math.cos(a) * (R + 1.1)
            cz = math.sin(a) * (R + 1.1)
            add(shaft, cx - 0.8, cx + 0.8, yy, yy + 1.5, cz - 0.8, cz + 0.8,
                "metal")
        add(shaft, -2.4, 2.4, L - 2.0, L + 2.4, -2.4, 2.4, "metal")
        add(shaft, -1.8, 1.8, L + 2.4, L + 5.2, -2.0, 2.0, "metal")   # 头
        add(shaft, -1.2, 1.2, L + 5.2, L + 7.0, -1.6, 1.6, "metal")   # 吻
        add(shaft, -1.6, 1.6, L + 3.0, L + 4.0, L and -2.6 or 2.6,
            L and 2.6 or 2.6, "jade") if False else None
        for s in (-1, 1):
            add(shaft, 1.3 * s, 2.7 * s, L + 4.4, L + 6.4, -0.8, 0.8, "steel")
            add(shaft, 0.7 * s, 1.5 * s, L + 3.4, L + 4.4, -2.5, -1.9, "jade")
        add(shaft, -0.8, 0.8, L + 7.0, L + 8.4, -1.0, 1.0, "rune")
        for s in (-1, 1):
            add(shaft, 1.0 * s, 1.6 * s, L + 6.2, L + 6.8, -2.2, 2.2, "steel")
        # 龙尾
        for i in range(6):
            add(shaft, -1.6 + i * 0.16, 1.6 - i * 0.16, -2.0 - i * 1.2,
                -0.8 - i * 1.2, -1.0 + i * 0.1, 1.0 - i * 0.1, "metal")
    elif key == "zhenhun":
        # 镇魂：两端幽火灯 + 铁链
        for y0 in (-3.4, L - 1.2):
            add(shaft, -2.4, 2.4, y0, y0 + 4.6, -2.4, 2.4, "metal")
            add(shaft, -1.5, 1.5, y0 + 0.8, y0 + 3.8, -1.6, 1.6, "rune")
            add(shaft, -1.9, 1.9, y0 + 3.8, y0 + 4.6, -1.9, 1.9, "steel")
            for s in (-1, 1):
                add(shaft, 2.4 * s - 0.3, 2.4 * s + 0.3, y0 + 0.6,
                    y0 + 3.6, -0.3, 0.3, "steel")
        for i in range(4):
            add(shaft, -2.0, -1.3, L * 0.30 + i * 1.5, L * 0.30 + i * 1.5 + 1.0,
                1.0, 1.6, "steel")
            add(shaft, 1.3, 2.0, L * 0.34 + i * 1.5, L * 0.34 + i * 1.5 + 1.0,
                -1.6, -1.0, "steel")
    elif key == "shougu":
        # 兽骨：兽首 + 骨节 + 骨铃
        for i in range(5):
            yy = 6.0 + i * (L - 16.0) / 5
            add(shaft, -R - 0.8, R + 0.8, yy, yy + 2.2, -R - 0.8, R + 0.8,
                "steel")
            add(shaft, -R - 0.3, R + 0.3, yy + 2.2, yy + 3.0, -R - 0.3,
                R + 0.3, "jade")
        add(shaft, -2.6, 2.6, L - 2.0, L + 2.6, -2.4, 2.4, "steel")
        add(shaft, -1.9, 1.9, L + 2.6, L + 5.0, -1.8, 1.8, "steel")
        add(shaft, -1.2, 1.2, L + 5.0, L + 6.6, -1.3, 1.3, "steel")
        for s in (-1, 1):
            add(shaft, 0.7 * s, 1.5 * s, L + 3.4, L + 4.2, -2.4, -1.8, "rune")
            add(shaft, 1.3 * s, 2.0 * s, L + 4.6, L + 6.8, -0.6, 0.6, "steel")
        for s in (-1, 1):
            add(shaft, 2.2 * s - 0.5, 2.2 * s + 0.5, -2.0, 2.0, -1.2, 1.2,
                "jade")
            add(shaft, 2.2 * s - 0.3, 2.2 * s + 0.3, 2.0, 3.0, -0.6, 0.6,
                "rune")
        add(shaft, -1.6, 1.6, -3.4, -1.4, -1.6, 1.6, "steel")
    for i in range(3):
        add(shaft, -R - 0.4, R + 0.4, L * (i + 1) / 4, L * (i + 1) / 4 + 1.0,
            -R - 0.4, R + 0.4, "jade")
    return g


