def build_scythe(spec):
    """死神镰刀（1-8 阶）：杆略弯，刀身【横装】在杆顶 75~90°，套筒连接、楔形厚刃。"""
    key = spec["key"]
    if key == "xukong":
        return build_war_scythe(spec)
    g = Geo("geometry.career_scythe_" + key, tex=(256, 256))
    S = 21.0          # 杆顶（套筒下沿）
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    blade = g.bone("blade", (0, S, 0), parent="head")

    # ---- 杆：略弯（每段 x 漂移），底部更粗 ----
    segs = 7
    for i in range(segs):
        y0 = i * S / segs
        y1 = (i + 1) * S / segs
        drift0 = (i / segs) ** 1.6 * 1.9
        drift1 = ((i + 1) / segs) ** 1.6 * 1.9
        r = 1.55 - 0.25 * (i / segs)
        add(shaft, drift0 - r, drift1 + r, y0, y1, -r, r, "cloth")
    # 上下握把（缠绳，垂直杆身的握钉）
    for y, pegs in ((7.5, True), (13.5, True)):
        add(shaft, **{"x0": -1.9, "x1": 2.1, "y0": y, "y1": y + 3.2,
                      "z0": -1.9, "z1": 1.9, "mat": "cloth"})
        if pegs:
            add(shaft, -0.4, 0.4, y + 1.2, y + 2.0, -2.6, 2.6, "steel")
    # 尾锤
    add(shaft, -2.0, 2.0, -2.2, 0.3, -2.0, 2.0, "metal")
    add(shaft, -1.4, 1.4, -3.6, -2.2, -1.4, 1.4, "metal")
    # 套筒（把刀身接进杆，0 间隙）
    add(head, -2.3, 2.3, S - 1.4, S + 2.8, -2.3, 2.3, "metal")
    add(head, -1.7, 1.7, S + 2.8, S + 4.4, -1.7, 1.7, "steel")
    add(head, -2.6, 2.6, S + 0.4, S + 1.4, -2.6, 2.6, "steel")

    # ---- 刀身：横装（沿 +X），根部厚、向尖收；刀背(厚)在上、刃口(薄)在下 ----
    total = {"gouhun": 9.6, "xueyue": 11.6, "yougu": 12.2}[key]
    root_w = {"gouhun": 3.2, "xueyue": 4.0, "yougu": 4.2}[key]
    n = 7
    for i in range(n):
        f0, f1 = i / n, (i + 1) / n
        x0 = 1.2 + total * f0
        x1 = 1.2 + total * f1
        w = root_w * (1.0 - 0.55 * f0)
        drop = -1.6 * f0 * f0            # 刀尖向下弯
        spine = drop + w * 0.5
        edge = drop - w * 0.5
        t = 1.7 - 0.9 * f0
        # 刀身主体（背面到刃面）
        blade.add((x0, edge, -t / 2), (x1 - x0, spine - edge, t),
                  mat="metal")
        # 刀背加厚（脊）
        blade.add((x0, spine - w * 0.34, -t * 0.44),
                  (x1 - x0, w * 0.34, t * 0.88), mat="steel")
        # 刃口薄片
        blade.add((x0, edge - 0.32, -0.28), (x1 - x0, 0.75, 0.56),
                  mat="steel")
        if key == "xueyue" and i in (2, 4):
            blade.add((x0 + 0.2, edge + w * 0.18, -t / 2 - 0.06),
                      (x1 - x0 - 0.4, w * 0.34, t + 0.12), mat="rune")
        if key == "yougu" and i % 2 == 0:
            blade.add((x0, spine - 0.2, -t * 0.36), (x1 - x0, 1.1, t * 0.72),
                      mat="steel")   # 锯齿背
            blade.add((x0 + 0.3, spine + 0.6, -0.3), (0.9, 1.2, 0.6), mat="jade")
    # 刀尖收锋
    tip_x = 1.2 + total
    tip_y = -1.6
    blade.add((tip_x - 0.4, tip_y - 0.5, -0.35), (2.6, 0.9, 0.7), mat="steel",
              rotation=(0, 0, -18), pivot=(tip_x, tip_y, 0))
    blade.add((tip_x + 1.9, tip_y - 1.1, -0.2), (1.6, 0.7, 0.4), mat="steel",
              rotation=(0, 0, -18), pivot=(tip_x, tip_y, 0))
    # 名字件
    if key == "gouhun":
        add(head, -1.5, 1.5, S - 0.6, S + 2.6, -3.0, -2.3, "steel")
        add(head, -0.8, 0.8, S + 0.2, S + 1.8, -3.4, -3.0, "jade")
        for i in range(3):
            add(head, 1.6 - 0.4 * i, 2.8 - 0.2 * i, -3.4 - i * 1.6,
                -2.4 - i * 1.6, -0.35, 0.35, "steel")
    elif key == "xueyue":
        for i in range(16):
            a = 2 * math.pi * i / 16
            cx, cy = math.cos(a) * 3.1, S + 3.4 + math.sin(a) * 3.1
            head.add((cx - 0.5, cy - 0.5, -0.6), (1.0, 1.0, 1.2), mat="jade")
        add(head, -1.2, 1.2, S - 1.0, S + 2.2, -3.2, -2.6, "rune")
    else:  # yougu
        for i in range(3):
            add(head, -2.2 + i * 1.8, -1.2 + i * 1.8, S + 2.2, S + 3.4, -2.6,
                -2.0, "rune")
        add(head, -1.0, 1.0, S - 1.2, S + 3.0, 2.2, 2.8, "rune")
        add(head, -2.4, 2.4, S + 3.0, S + 4.2, -2.4, 2.4, "steel")
    return g


def build_war_scythe(spec):
    """战镰（9-10 阶）：刀身沿杆延长线（同向），外侧开刃、内侧刀脊，根部护手。"""
    g = Geo("geometry.career_scythe_xukong", tex=(256, 256))
    S = 18.0
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    blade = g.bone("blade", (0, S, 0), parent="head")
    segs = 6
    for i in range(segs):
        y0 = i * S / segs
        y1 = (i + 1) * S / segs
        drift0 = (i / segs) ** 1.5 * 2.4
        drift1 = ((i + 1) / segs) ** 1.5 * 2.4
        r = 1.55 - 0.2 * (i / segs)
        add(shaft, drift0 - r, drift1 + r, y0, y1, -r, r, "cloth")
    for y in (6.0, 12.0):
        add(shaft, -2.0, 2.4, y, y + 3.0, -2.0, 2.0, "cloth")
        add(shaft, -0.4, 0.4, y + 1.2, y + 1.9, -2.7, 2.7, "steel")
    add(shaft, -2.2, 2.2, -2.4, 0.2, -2.2, 2.2, "metal")
    add(shaft, -1.5, 1.5, -3.9, -2.4, -1.5, 1.5, "metal")
    add(head, -2.6, 2.6, S - 1.6, S + 3.0, -2.6, 2.6, "metal")
    add(head, -3.6, 3.6, S + 0.4, S + 1.8, -1.0, 1.0, "steel")   # 护手
    add(head, -1.6, 1.6, S + 3.0, S + 4.6, -1.6, 1.6, "steel")
    # 刀身沿 +Y 延长线，向外(+X)弯；外侧薄刃、内侧厚脊
    n = 9
    total = 14.0
    for i in range(n):
        f0, f1 = i / n, (i + 1) / n
        y0 = S + 3.0 + total * f0
        y1 = S + 3.0 + total * f1
        bend0 = 1.6 * f0 * f0
        bend1 = 1.6 * f1 * f1
        w = 3.8 * (1.0 - 0.62 * f0)
        t = 2.0 - 1.0 * f0
        blade.add((bend0 - w * 0.35, y0, -t / 2),
                  (bend1 - bend0 + w * 0.7, y1 - y0, t), mat="metal")
        # 内侧厚脊
        blade.add((bend0 - w * 0.35, y0, -t * 0.42),
                  (w * 0.42, y1 - y0, t * 0.84), mat="steel")
        # 外侧薄刃
        blade.add((bend1 + w * 0.02, y0, -0.30), (0.9, y1 - y0, 0.60),
                  mat="steel")
        if i in (3, 6):
            blade.add((bend0 + 0.3, y0 + 0.3, -t / 2 - 0.08),
                      (0.7, y1 - y0 - 0.6, t + 0.16), mat="rune")
    # 刀尖
    tx = 1.6 + 0.7
    ty = S + 3.0 + total
    blade.add((tx - 0.6, ty - 0.4, -0.4), (2.2, 3.0, 0.8), mat="steel",
              rotation=(0, 0, -22), pivot=(tx, ty, 0))
    for i in range(3):
        a = math.radians(30 + i * 22)
        px = math.cos(a) * 5.0 + 2.0
        py = S + 8.0 + math.sin(a) * 5.0
        add(blade, px - 0.9, px + 0.9, py - 0.9, py + 0.9, -0.5, 0.5, "jade")
    return g


def build_spear(spec):
    """长枪：总长 ~2.2 格；枪头在顶部占 20~25%（叶形+中脊+厚度），枪箍/红缨/尾锥/握把。"""
    g = Geo("geometry.career_spear_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    key = spec["key"]
    S = 27.5                     # 杆长（到枪箍下沿）
    hw, hl = 4.4, 8.2            # 枪头宽/长（占全长 ~22%）
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    tassel = g.bone("tassel", (0, S, 0), parent="shaft")
    # 杆：略收分 + 中段缠绳
    nseg = 6
    for i in range(nseg):
        y0 = i * S / nseg
        y1 = (i + 1) * S / nseg
        r0 = 1.75 - 0.28 * (i / nseg)
        r1 = 1.75 - 0.28 * ((i + 1) / nseg)
        add(shaft, -r0, r0, y0, y1, -r0, r0, "cloth")
    add(shaft, -2.05, 2.05, 9.0, 17.5, -2.05, 2.05, "cloth")
    for y in (7.0, 10.0, 13.0, 16.0):
        add(shaft, -2.2, 2.2, y, y + 0.7, -2.2, 2.2, "steel")
    # 尾锥（杆底）
    add(shaft, -2.3, 2.3, -1.6, 0.6, -2.3, 2.3, "metal")
    add(shaft, -1.2, 1.2, -4.2, -1.4, -1.2, 1.2, "steel")
    add(shaft, -0.4, 0.4, -5.4, -4.0, -0.4, 0.4, "steel")
    # 枪箍/套筒
    add(head, -2.4, 2.4, S - 2.6, S + 1.2, -2.4, 2.4, "metal")
    add(head, -2.7, 2.7, S - 1.8, S - 0.6, -2.7, 2.7, "steel")
    # 护片 langets
    for s in (-1, 1):
        add(head, 1.0 * s, 2.2 * s, S - 6.2, S - 2.0, -0.6, 0.6, "steel")
    # 枪头：叶形（根部宽、中脊、厚度），尖端阶梯收锋
    y0 = S + 1.2
    taper_blade(head, y0, y0 + hl * 0.72, hw, hw * 0.66, 2.1, 1.5, 4)
    add(head, -hw * 0.5, hw * 0.5, y0, y0 + hl * 0.30, -0.9, 0.9,
        "steel")  # 根部
    step_point(head, y0 + hl * 0.72, y0 + hl + 1.0, hw * 0.66, 1.5, 3)
    # 中脊
    add(head, -0.9, 0.9, y0 + 0.3, y0 + hl - 0.6, -1.15, 1.15, "steel")
    if key == "leiji":
        for i in range(4):
            add(head, hw * 0.52, hw * 0.96, y0 + 1.6 + i * 1.7,
                y0 + 2.4 + i * 1.7, -0.5, 0.5, "rune")
            add(head, -hw * 0.96, -hw * 0.52, y0 + 2.2 + i * 1.7,
                y0 + 3.0 + i * 1.7, -0.5, 0.5, "rune")
    if key == "xueying":
        add(head, -hw * 0.10, hw * 0.10, y0 + 0.4, y0 + hl - 1.0, -1.2, 1.2,
            "rune")
        for i in range(4):
            add(head, hw * 0.55, hw * 0.80, y0 + 1.2 + i * 1.4,
                y0 + 2.0 + i * 1.4, -0.5, 0.5, "steel")
    if key == "youlong":
        add(head, -hw * 0.45, hw * 0.45, y0 + 0.2, y0 + 2.0, -1.2, 1.2,
            "jade")
        for s in (-1, 1):
            add(head, hw * 0.35 * s, hw * 0.55 * s, y0 + 1.0, y0 + 1.8,
                -1.25, -0.95, "rune")
    if key == "pojun":
        for i in range(3):
            add(head, -hw * 0.55 + i * 1.2, -hw * 0.35 + i * 1.2, y0 + 0.4,
                y0 + 3.0, -1.2, 1.2, "steel")
    # 红缨（枪头下方一圈，多段下垂）
    for i in range(7):
        a = math.radians(i * 51.4)
        px, pz = math.cos(a) * 1.7, math.sin(a) * 1.7
        tassel.add((px - 0.5, -3.6 - (i % 2) * 0.7, pz - 0.5),
                   (1.0, 4.2, 1.0), mat="cloth",
                   rotation=(12 if i % 2 == 0 else -12, 0, 0),
                   pivot=(px, 0.0, pz))
    return g


def build_staff(spec):
    """棍：总长 ~1.7 格、杆细（半径 1.4）、两端端帽、中段握把、身份件（环/龙/符/铃）。"""
    g = Geo("geometry.career_staff_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    key = spec["key"]
    L = 24.0
    R = 1.4
    shaft = g.bone("shaft", (0, 0, 0))
    # 杆（略收分）
    nseg = 6
    for i in range(nseg):
        y0 = i * L / nseg
        y1 = (i + 1) * L / nseg
        r0 = R + 0.10 * (1 - i / nseg)
        r1 = R + 0.10 * (1 - (i + 1) / nseg)
        add(shaft, -r0, r0, y0, y1, -r0, r0, "cloth")
    # 中段握把
    add(shaft, -R - 0.35, R + 0.35, 9.5, 14.5, -R - 0.35, R + 0.35, "cloth")
    for y in range(10, 15, 1):
        add(shaft, -R - 0.45, R + 0.45, y, y + 0.35, -R - 0.45, R + 0.45,
            "steel")
    # 端帽（两端，略大 + 端环）
    for y0 in (0.0, L - 2.4):
        add(shaft, -2.1, 2.1, y0, y0 + 2.4, -2.1, 2.1, "metal")
        add(shaft, -1.4, 1.4, y0 + 2.4, y0 + 3.1, -1.4, 1.4, "steel")
        add(shaft, -2.35, 2.35, y0 + 0.7, y0 + 1.3, -2.35, 2.35, "jade")
    if key == "jiangmo":
        # 降魔环：两道铜环 + 顶部小环
        for y in (5.5, 18.5):
            ring_cubes(shaft, 10, y, 2.3, 0.55, mat="jade", tilt=90.0)
        add(shaft, -0.5, 0.5, L + 0.6, L + 2.6, -0.5, 0.5, "steel")
        add(shaft, -1.5, 1.5, L + 1.0, L + 1.6, -0.35, 0.35, "jade")
    elif key == "panlong":
        # 盘龙浮雕：杆身螺旋 + 龙首顶部
        for i in range(14):
            yy = 3.5 + i * 1.35
            a = i * 2.0
            cx, cz = math.cos(a) * (R + 0.55), math.sin(a) * (R + 0.55)
            add(shaft, cx - 0.45, cx + 0.45, yy, yy + 0.85, cz - 0.45,
                cz + 0.45, "metal")
        add(shaft, -1.9, 1.9, L + 0.2, L + 2.6, -1.5, 1.5, "metal")
        add(shaft, -1.2, 1.2, L + 2.6, L + 4.2, -1.1, 1.1, "metal")
        for s in (-1, 1):
            add(shaft, 1.3 * s, 2.4 * s, L + 1.4, L + 3.2, -0.6, 0.6,
                "steel")
            add(shaft, 0.5 * s, 1.1 * s, L + 2.8, L + 3.4, -1.4, -0.9,
                "rune")
        add(shaft, -0.7, 0.7, L + 4.2, L + 5.2, -0.7, 0.7, "rune")
    elif key == "zhenhun":
        # 镇魂符 + 骨端 + 幽绿环
        add(shaft, -1.0, 1.0, 5.0, 7.0, -2.2, -1.7, "cloth")
        add(shaft, -0.6, 0.6, 5.4, 6.6, -2.45, -2.2, "rune")
        for y in (4.0, 20.0):
            ring_cubes(shaft, 10, y, 2.2, 0.5, mat="jade", tilt=90.0)
        add(shaft, -1.8, 1.8, L - 2.0, L + 1.6, -1.8, 1.8, "steel")
        add(shaft, -1.0, 1.0, L + 1.6, L + 3.0, -1.0, 1.0, "rune")
        for i in range(3):
            add(shaft, -2.2 + i * 0.4, -1.4 + i * 0.4, L * 0.35 + i * 1.4,
                L * 0.35 + i * 1.4 + 0.9, 1.5, 2.0, "steel")
    else:  # shougu
        # 兽骨：骨节 + 骨铃 + 图腾刻
        for i in range(5):
            yy = 3.5 + i * (L - 7.0) / 5
            add(shaft, -1.9, 1.9, yy, yy + 1.1, -1.9, 1.9, "steel")
        add(shaft, -2.0, 2.0, L - 1.0, L + 2.0, -2.0, 2.0, "steel")
        add(shaft, -1.3, 1.3, L + 2.0, L + 3.4, -1.3, 1.3, "steel")
        for s in (-1, 1):
            add(shaft, 1.9 * s - 0.35, 1.9 * s + 0.35, 2.2, 4.6, -0.35,
                0.35, "steel")
            add(shaft, 1.9 * s - 0.2, 1.9 * s + 0.2, 2.4, 3.0, -0.6, 0.6,
                "rune")
        add(shaft, -0.6, 0.6, L - 0.2, L + 1.0, -1.5, -1.0, "rune")
        add(shaft, 0.6, 1.2, L - 0.2, L + 1.0, -1.5, -1.0, "rune")
    return g


