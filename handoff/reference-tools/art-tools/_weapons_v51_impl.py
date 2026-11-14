def build_scythe(spec):
    """死神镰刀（1-8 阶）：杆略弯，刀身【横装】在杆顶 75~90°，套筒连接、楔形厚刃、平滑弧线。"""
    key = spec["key"]
    if key == "xukong":
        return build_war_scythe(spec)
    g = Geo("geometry.career_scythe_" + key, tex=(256, 256))
    S = 21.0
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    blade = g.bone("blade", (0, S, 0), parent="head")

    # ---- 杆：略弯（多段小漂移），上细下粗 ----
    segs = 10
    for i in range(segs):
        y0 = i * S / segs
        y1 = (i + 1) * S / segs
        drift0 = (i / segs) ** 1.7 * 1.5
        drift1 = ((i + 1) / segs) ** 1.7 * 1.5
        r = 1.6 - 0.3 * (i / segs)
        add(shaft, drift0 - r, drift1 + r, y0, y1, -r, r, "cloth")
        if i % 3 == 2:
            add(shaft, drift0 - r - 0.15, drift1 + r + 0.15, y0, y0 + 0.35,
                -r - 0.15, r + 0.15, "steel")
    # 上下握把 + 垂直握钉
    add(shaft, -1.95, 2.3, 7.0, 10.5, -1.95, 1.95, "cloth")
    add(shaft, -1.95, 2.1, 12.6, 15.8, -1.95, 1.95, "cloth")
    for y in (8.2, 14.0):
        add(shaft, -0.45, 0.45, y, y + 0.9, -2.65, 2.65, "steel")
        add(shaft, -0.7, 0.7, y + 0.3, y + 0.6, -2.85, 2.85, "metal")
    # 尾锤（配重锥）
    add(shaft, -2.0, 2.0, -1.4, 0.4, -2.0, 2.0, "metal")
    add(shaft, -1.5, 1.5, -2.6, -1.4, -1.5, 1.5, "metal")
    add(shaft, -0.8, 0.8, -4.4, -2.6, -0.8, 0.8, "steel")
    # 套筒（刀身插进去，0 间隙）
    add(head, -2.3, 2.3, S - 1.6, S + 3.0, -2.3, 2.3, "metal")
    add(head, -2.6, 2.6, S + 0.2, S + 1.2, -2.6, 2.6, "steel")
    add(head, -1.7, 1.7, S + 3.0, S + 4.6, -1.7, 1.7, "steel")

    # ---- 刀身：横向弧线（根部切线≈水平，向刀尖下弯），刀背厚/刃口薄 ----
    total = {"gouhun": 11.8, "xueyue": 14.6, "yougu": 15.4}[key]
    root_w = {"gouhun": 3.2, "xueyue": 4.6, "yougu": 4.8}[key]
    drop_k = {"gouhun": 2.8, "xueyue": 4.6, "yougu": 5.2}[key]
    rx, ry = 1.2, S + 2.6
    n = 12
    for i in range(n):
        f0, f1 = i / n, (i + 1) / n
        x0, x1 = rx + total * f0, rx + total * f1
        y0 = ry - drop_k * (f0 ** 1.7)
        y1 = ry - drop_k * (f1 ** 1.7)
        ang = math.degrees(math.atan2(y1 - y0, x1 - x0))
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2
        seg = math.dist((x0, y0), (x1, y1)) + 0.35
        h = root_w * (1.0 - 0.66 * f0)
        t = 2.2 - 1.1 * f0
        # 主体（楔形：靠上厚、靠下薄）
        blade.add((mx - seg / 2, my - h * 0.32, -t / 2),
                  (seg, h * 0.64, t), mat="metal",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        # 刀背加厚（上缘）
        blade.add((mx - seg / 2, my + h * 0.30, -t * 0.58),
                  (seg, h * 0.34, t * 1.16), mat="steel",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        # 刃口（下缘，薄）
        blade.add((mx - seg / 2, my - h * 0.46, -0.30),
                  (seg, 0.85, 0.60), mat="steel",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        if key == "xueyue" and i in (3, 6, 9):
            blade.add((mx - seg / 2, my - h * 0.08, -t / 2 - 0.07),
                      (seg, h * 0.26, t + 0.14), mat="rune",
                      rotation=(0, 0, ang), pivot=(mx, my, 0))
        if key == "yougu" and i % 2 == 0:
            blade.add((mx - seg / 2, my + h * 0.50, -t * 0.40),
                      (seg * 0.8, 1.2, t * 0.8), mat="steel",
                      rotation=(0, 0, ang), pivot=(mx, my, 0))
            add_blade_gem(blade, mx + seg * 0.25, my + h * 0.55)
    # 刀尖收锋
    tipx = rx + total + 0.4
    tipy = ry - drop_k
    blade.add((tipx - 1.2, tipy - 0.6, -0.45), (2.6, 1.1, 0.9), mat="steel",
              rotation=(0, 0, -24), pivot=(tipx, tipy, 0))
    blade.add((tipx + 1.4, tipy - 1.5, -0.28), (1.8, 0.8, 0.56), mat="steel",
              rotation=(0, 0, -24), pivot=(tipx, tipy, 0))
    # 刀根 tang：从套筒伸进刀身，保证 0 间隙连接
    blade.add((0.2, ry - 2.0, -1.0), (1.8, 3.8, 2.0), mat="steel")

    if key == "gouhun":
        add(head, -1.6, 1.6, S - 0.8, S + 2.8, -3.1, -2.3, "steel")
        add(head, -0.9, 0.9, S, S + 1.4, -3.5, -3.0, "jade")
        for i in range(3):
            add(head, 1.5 - 0.35 * i, 2.7 - 0.15 * i, -3.5 - i * 1.5,
                -2.5 - i * 1.5, -0.35, 0.35, "steel")
    elif key == "xueyue":
        for i in range(18):
            a = 2 * math.pi * i / 18
            cx, cy = math.cos(a) * 3.2, S + 3.4 + math.sin(a) * 3.2
            head.add((cx - 0.55, cy - 0.55, -0.7), (1.1, 1.1, 1.4), mat="jade")
        add(head, -1.2, 1.2, S - 1.2, S + 2.2, -3.3, -2.7, "rune")
    else:
        for i in range(3):
            add(head, -2.4 + i * 1.9, -1.3 + i * 1.9, S + 2.4, S + 3.6,
                -2.7, -2.0, "rune")
        add(head, -1.1, 1.1, S - 1.4, S + 3.0, 2.2, 2.9, "rune")
        add(head, -2.5, 2.5, S + 3.1, S + 4.3, -2.5, 2.5, "steel")
    return g


def add_blade_gem(blade, x, y):
    blade.add((x - 0.35, y - 0.35, -0.5), (0.7, 0.7, 1.0), mat="jade")


def build_war_scythe(spec):
    """战镰（9-10 阶）：刀身沿杆延长线（同向），外侧开刃、内侧刀脊，根部护手。"""
    g = Geo("geometry.career_scythe_xukong", tex=(256, 256))
    S = 18.0
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    blade = g.bone("blade", (0, S, 0), parent="head")
    segs = 8
    for i in range(segs):
        y0 = i * S / segs
        y1 = (i + 1) * S / segs
        drift0 = (i / segs) ** 1.5 * 2.2
        drift1 = ((i + 1) / segs) ** 1.5 * 2.2
        r = 1.55 - 0.2 * (i / segs)
        add(shaft, drift0 - r, drift1 + r, y0, y1, -r, r, "cloth")
    for y in (6.0, 12.0):
        add(shaft, -2.0, 2.4, y, y + 3.0, -2.0, 2.0, "cloth")
        add(shaft, -0.45, 0.45, y + 1.2, y + 2.0, -2.7, 2.7, "steel")
    add(shaft, -2.2, 2.2, -2.2, 0.4, -2.2, 2.2, "metal")
    add(shaft, -1.5, 1.5, -3.9, -2.2, -1.5, 1.5, "metal")
    add(head, -2.6, 2.6, S - 1.6, S + 3.0, -2.6, 2.6, "metal")
    add(head, -4.0, 4.0, S + 0.4, S + 1.8, -1.0, 1.0, "steel")
    add(head, -1.7, 1.7, S + 3.0, S + 4.6, -1.7, 1.7, "steel")
    n = 11
    total = 14.0
    rx, ry = 1.4, S + 3.0
    for i in range(n):
        f0, f1 = i / n, (i + 1) / n
        y0, y1 = ry + total * f0, ry + total * f1
        x0, x1 = rx + 1.4 * f0 * f0, rx + 1.4 * f1 * f1
        ang = math.degrees(math.atan2(y1 - y0, x1 - x0))
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2
        seg = math.dist((x0, y0), (x1, y1)) + 0.3
        w = 3.8 * (1.0 - 0.62 * f0)
        t = 2.2 - 1.1 * f0
        blade.add((mx - w * 0.35, my - seg / 2, -t / 2),
                  (w * 0.7, seg, t), mat="metal",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        blade.add((mx - w * 0.35, my - seg / 2, -t * 0.62),
                  (w * 0.42, seg, t * 1.24), mat="steel",
                  rotation=(0, 0, ang), pivot=(mx, my, 0))
        blade.add((mx + w * 0.30, my - seg / 2, -0.3), (0.9, seg, 0.6),
                  mat="steel", rotation=(0, 0, ang), pivot=(mx, my, 0))
        if i in (4, 7):
            blade.add((mx - w * 0.1, my - seg / 2, -t / 2 - 0.07),
                      (0.7, seg, t + 0.14), mat="rune",
                      rotation=(0, 0, ang), pivot=(mx, my, 0))
    tx, ty = rx + 1.4, ry + total
    blade.add((tx - 0.7, ty - 0.3, -0.45), (2.0, 3.2, 0.9), mat="steel",
              rotation=(0, 0, -26), pivot=(tx, ty, 0))
    for i in range(3):
        a = math.radians(30 + i * 22)
        px = math.cos(a) * 5.0 + 2.4
        py = S + 8.0 + math.sin(a) * 5.0
        add(blade, px - 0.9, px + 0.9, py - 0.9, py + 0.9, -0.5, 0.5, "jade")
    return g


def build_spear(spec):
    """长枪：总长 ~2.2 格；枪头在顶部占 20~25%（叶形+中脊+厚度），枪箍/红缨/尾锥/握把。"""
    g = Geo("geometry.career_spear_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    key = spec["key"]
    S = 25.8
    hw = 5.6 if key != "youlong" else 5.0
    hl = 9.4 if key != "youlong" else 10.0
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, S, 0), parent="shaft")
    tassel = g.bone("tassel", (0, S, 0), parent="shaft")
    nseg = 7
    for i in range(nseg):
        y0 = i * S / nseg
        y1 = (i + 1) * S / nseg
        r0 = 1.75 - 0.22 * (i / nseg)
        r1 = 1.75 - 0.22 * ((i + 1) / nseg)
        add(shaft, -r0, r0, y0, y1, -r0, r0, "cloth")
    add(shaft, -2.1, 2.1, 8.5, 16.8, -2.1, 2.1, "cloth")
    for y in (7.0, 10.5, 14.0):
        add(shaft, -2.25, 2.25, y, y + 0.7, -2.25, 2.25, "steel")
    # 尾锥
    add(shaft, -2.3, 2.3, -1.8, 0.8, -2.3, 2.3, "metal")
    add(shaft, -1.2, 1.2, -4.6, -1.6, -1.2, 1.2, "steel")
    add(shaft, -0.4, 0.4, -6.0, -4.4, -0.4, 0.4, "steel")
    # 枪箍/套筒 + 护片
    add(head, -2.4, 2.4, S - 2.4, S + 1.2, -2.4, 2.4, "metal")
    add(head, -2.75, 2.75, S - 1.4, S - 0.2, -2.75, 2.75, "steel")
    for s in (-1, 1):
        add(head, 1.0 * s, 2.4 * s, S - 6.4, S - 2.2, -0.65, 0.65, "steel")
        add(head, 2.1 * s, 2.6 * s, S - 3.2, S - 2.4, -0.9, 0.9, "jade")
    # 枪头：叶形（宽叶），中脊，尖端 3 段收锋
    y0 = S + 1.2
    prof = [0.42, 0.72, 0.96, 1.0, 0.88, 0.65, 0.34]
    for i, fw in enumerate(prof):
        ya = y0 + hl * i / len(prof)
        yb = y0 + hl * (i + 1) / len(prof)
        w = hw * fw
        th = 2.0 - 0.12 * i
        add(head, -w / 2, w / 2, ya, yb + 0.15, -th / 2, th / 2, "metal")
    # 中脊
    add(head, -0.85, 0.85, y0 + 0.3, y0 + hl - 0.7, -1.2, 1.2, "steel")
    # 刃口高光
    for s in (-1, 1):
        add(head, hw * 0.40 * s, hw * 0.52 * s, y0 + 0.5, y0 + hl - 1.2,
            -0.35, 0.35, "steel")
    # 尖锋
    step_point(head, y0 + hl, y0 + hl + 2.4, hw * 0.34, 1.0, 2)
    if key == "leiji":
        for i in range(4):
            add(head, hw * 0.44, hw * 0.72, y0 + 1.8 + i * 1.6,
                y0 + 2.6 + i * 1.6, -0.5, 0.5, "rune")
            add(head, -hw * 0.72, -hw * 0.44, y0 + 2.6 + i * 1.6,
                y0 + 3.4 + i * 1.6, -0.5, 0.5, "rune")
    if key == "xueying":
        add(head, -hw * 0.07, hw * 0.07, y0 + 0.4, y0 + hl - 0.8, -1.25, 1.25,
            "rune")
        for i in range(4):
            add(head, hw * 0.46, hw * 0.66, y0 + 1.4 + i * 1.5,
                y0 + 2.2 + i * 1.5, -0.5, 0.5, "steel")
    if key == "youlong":
        add(head, -hw * 0.40, hw * 0.40, y0 + 0.3, y0 + 2.2, -1.25, 1.25,
            "jade")
        for s in (-1, 1):
            add(head, hw * 0.30 * s, hw * 0.50 * s, y0 + 1.0, y0 + 1.8,
                -1.30, -0.95, "rune")
    if key == "pojun":
        for i in range(3):
            add(head, -hw * 0.60 + i * 1.5, -hw * 0.36 + i * 1.5, y0 + 0.4,
                y0 + 3.2, -1.25, 1.25, "steel")
    # 红缨：枪头下方一圈，多段下垂
    radius = 2.1
    for i in range(8):
        a = math.radians(i * 45)
        px, pz = math.cos(a) * radius, math.sin(a) * radius
        tassel.add((px - 0.55, -4.6 - (i % 2) * 0.8, pz - 0.55),
                   (1.1, 5.0, 1.1), mat="cloth",
                   rotation=(14 if i % 2 == 0 else -14, 0, 0),
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
    nseg = 6
    for i in range(nseg):
        y0 = i * L / nseg
        y1 = (i + 1) * L / nseg
        r0 = R + 0.10 * (1 - i / nseg)
        r1 = R + 0.10 * (1 - (i + 1) / nseg)
        add(shaft, -r0, r0, y0, y1, -r0, r0, "cloth")
    # 中段握把（缠绳）
    add(shaft, -R - 0.35, R + 0.35, 9.5, 14.5, -R - 0.35, R + 0.35, "cloth")
    for y in range(10, 15):
        add(shaft, -R - 0.45, R + 0.45, y, y + 0.35, -R - 0.45, R + 0.45,
            "steel")
    # 两端端帽
    for y0 in (0.0, L - 2.4):
        add(shaft, -2.1, 2.1, y0, y0 + 2.4, -2.1, 2.1, "metal")
        add(shaft, -1.4, 1.4, y0 + 2.4, y0 + 3.1, -1.4, 1.4, "steel")
        add(shaft, -2.35, 2.35, y0 + 0.7, y0 + 1.3, -2.35, 2.35, "jade")
    if key == "jiangmo":
        # 降魔锡杖：顶部三道大环 + 顶针 + 环坠
        add(shaft, -0.9, 0.9, L + 0.4, L + 2.4, -0.9, 0.9, "metal")
        for i, rr in enumerate((3.6, 3.0, 2.4)):
            ring_cubes(shaft, 12, L + 3.2 + i * 2.2, rr, 0.7,
                       mat="jade", tilt=90.0)
        add(shaft, -0.6, 0.6, L + 9.4, L + 12.0, -0.6, 0.6, "steel")
        add(shaft, -0.3, 0.3, L + 12.0, L + 13.2, -0.3, 0.3, "rune")
        for s in (-1, 1):
            for i in range(3):
                add(shaft, s * 2.6 - 0.3, s * 2.6 + 0.3,
                    L + 2.8 + i * 2.2 - 2.0, L + 2.8 + i * 2.2 - 1.1,
                    -0.3, 0.3, "steel")
    elif key == "panlong":
        for i in range(16):
            yy = 3.5 + i * 1.15
            a = i * 2.0
            cx, cz = math.cos(a) * (R + 0.55), math.sin(a) * (R + 0.55)
            add(shaft, cx - 0.45, cx + 0.45, yy, yy + 0.75, cz - 0.45,
                cz + 0.45, "metal")
        add(shaft, -2.0, 2.0, L + 0.2, L + 2.8, -1.6, 1.6, "metal")
        add(shaft, -1.3, 1.3, L + 2.8, L + 4.6, -1.2, 1.2, "metal")
        for s in (-1, 1):
            add(shaft, 1.4 * s, 2.6 * s, L + 1.6, L + 3.6, -0.65, 0.65,
                "steel")
            add(shaft, 0.6 * s, 1.2 * s, L + 3.0, L + 3.7, -1.5, -0.9,
                "rune")
        add(shaft, -0.8, 0.8, L + 4.6, L + 5.8, -0.8, 0.8, "rune")
    elif key == "zhenhun":
        # 镇魂：两端幽火灯（笼 + 核心 + 垂饰）
        for y0 in (-4.6, L - 0.6):
            add(shaft, -2.2, 2.2, y0, y0 + 5.2, -2.2, 2.2, "metal")
            add(shaft, -2.4, 2.4, y0 + 0.4, y0 + 1.2, -2.4, 2.4, "metal")
            add(shaft, -2.4, 2.4, y0 + 4.0, y0 + 4.8, -2.4, 2.4, "metal")
            add(shaft, -1.3, 1.3, y0 + 1.4, y0 + 3.6, -1.4, 1.4, "rune")
            for s in (-1, 1):
                add(shaft, 1.9 * s - 0.35, 1.9 * s + 0.35, y0 + 1.2,
                    y0 + 4.2, -0.35, 0.35, "steel")
        add(shaft, -0.9, 0.9, L + 4.8, L + 6.2, -0.9, 0.9, "rune")
        add(shaft, -0.9, 0.9, -8.4, -7.0, -0.9, 0.9, "rune")
        for i in range(3):
            add(shaft, -2.0 + i * 1.0, -1.4 + i * 1.0, L * 0.30 + i * 1.5,
                L * 0.30 + i * 1.5 + 1.0, 1.4, 2.0, "steel")
    else:  # shougu
        for i in range(6):
            yy = 3.2 + i * (L - 6.4) / 6
            add(shaft, -1.9, 1.9, yy, yy + 1.2, -1.9, 1.9, "steel")
            add(shaft, -2.2, 2.2, yy + 1.2, yy + 1.9, -2.2, 2.2, "cloth")
        add(shaft, -2.1, 2.1, L - 1.0, L + 2.2, -2.1, 2.1, "steel")
        add(shaft, -1.4, 1.4, L + 2.2, L + 4.0, -1.4, 1.4, "steel")
        for s in (-1, 1):
            add(shaft, 0.8 * s, 1.6 * s, L + 2.8, L + 3.6, -2.0, -1.4, "rune")
            add(shaft, 1.5 * s, 2.4 * s, L + 3.6, L + 6.2, -0.6, 0.6, "steel")
            add(shaft, 1.2 * s - 0.3, 1.2 * s + 0.3, -3.4, -1.0, -0.3, 0.3,
                "steel")
        add(shaft, -1.5, 1.5, -6.0, -3.4, -1.5, 1.5, "steel")
        add(shaft, -0.7, 0.7, -7.6, -6.0, -0.7, 0.7, "jade")
    return g


