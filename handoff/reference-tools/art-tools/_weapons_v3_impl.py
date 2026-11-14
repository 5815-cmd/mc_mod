SPECS = [
    # ---------------- 长剑 ----------------
    dict(key="qingfeng", form="sword", cn="青锋长剑", tiers=(1, 2),
         color=P((150, 195, 212), (218, 238, 246), (34, 58, 72), (130, 225, 238),
                 (150, 235, 245), glow=(150, 235, 245), gold=(190, 200, 120),
                 grip=(30, 52, 66), tassel=(190, 235, 245)),
         shape=dict(blade_len=24, w0=3.6, w1=2.6, t0=1.4, t1=1.0, segs=4,
                    guard="cloud", tassel=True, rune_bars=0)),
    dict(key="chixiao", form="sword", cn="赤霄灵纹剑", tiers=(3, 4),
         color=P((196, 126, 92), (242, 206, 178), (86, 18, 14), (255, 126, 62),
                 (255, 120, 40), glow=(255, 120, 40), hem=(160, 50, 24),
                 gold=(232, 186, 96), grip=(74, 16, 12), tassel=(255, 150, 90)),
         shape=dict(blade_len=27, w0=4.8, w1=3.2, t0=1.6, t1=1.1, segs=6,
                    guard="flame", tassel=True, rune_bars=5, ribs=3)),
    dict(key="xuantie", form="sword", cn="玄铁重锋剑", tiers=(5, 6),
         color=P((74, 76, 82), (138, 142, 150), (40, 40, 44), (150, 150, 150),
                 (170, 180, 185), glow=(120, 130, 140), gold=(150, 130, 90),
                 grip=(48, 34, 26), tassel=(120, 125, 130)),
         shape=dict(blade_len=21, w0=7.2, w1=5.2, t0=2.6, t1=1.9, segs=5,
                    guard="square", tassel=False, rune_bars=0, rivets=True)),
    dict(key="xuecao", form="sword", cn="血槽魔纹剑", tiers=(7, 8),
         color=P((128, 66, 66), (198, 148, 148), (48, 8, 10), (255, 74, 74),
                 (226, 32, 44), glow=(230, 40, 50), hem=(120, 12, 18),
                 gold=(180, 150, 90), grip=(58, 10, 12), tassel=(230, 70, 70)),
         shape=dict(blade_len=28, w0=5.6, w1=3.6, t0=1.9, t1=1.2, segs=6,
                    guard="small", tassel=True, rune_bars=3, groove=True,
                    serrated=True, skull=True)),
    dict(key="shuangwen", form="sword", cn="霜纹古篆剑", tiers=(9, 10),
         color=P((198, 214, 230), (240, 246, 252), (53, 36, 97), (165, 232, 248),
                 (110, 216, 246), glow=(127, 227, 242), hem=(59, 180, 224),
                 gold=(201, 162, 74), grip=(53, 36, 97), tassel=(206, 232, 247)),
         shape=dict(w01r=True)),

    # ---------------- 巨剑 ----------------
    dict(key="zhenyue", form="greatsword", cn="镇岳巨剑", tiers=(1, 3),
         color=P((140, 123, 92), (212, 196, 160), (78, 58, 30), (150, 190, 120),
                 (200, 210, 160), glow=(200, 210, 160), gold=(190, 160, 90),
                 grip=(72, 50, 26), tassel=(200, 190, 150)),
         shape=dict(blade_len=32, w0=15.0, w1=10.5, t0=4.2, t1=3.0, segs=6,
                    guard="stone", rocks=True, rune_bars=0)),
    dict(key="duanyue", form="greatsword", cn="断岳玄重剑", tiers=(4, 5),
         color=P((80, 84, 92), (150, 158, 170), (38, 40, 48), (130, 140, 150),
                 (160, 175, 185), glow=(120, 140, 150), gold=(120, 120, 120),
                 grip=(40, 32, 28), tassel=(140, 150, 160)),
         shape=dict(blade_len=35, w0=14.0, w1=10.0, t0=3.8, t1=2.8, segs=7,
                    guard="bar", groove=True, rune_bars=0)),
    dict(key="longji", form="greatsword", cn="龙脊裂地剑", tiers=(6, 8),
         color=P((108, 120, 88), (192, 204, 150), (48, 58, 38), (110, 255, 170),
                 (120, 255, 180), glow=(120, 255, 180), hem=(60, 130, 90),
                 gold=(226, 190, 96), grip=(44, 56, 32), tassel=(160, 240, 190)),
         shape=dict(blade_len=35, w0=15.0, w1=10.5, t0=4.0, t1=3.0, segs=7,
                    guard="dragon", spine=6, rune_bars=2)),
    dict(key="mogu", form="greatsword", cn="魔骨血刃巨剑", tiers=(9, 10),
         color=P((200, 190, 170), (240, 235, 220), (58, 10, 14), (255, 70, 70),
                 (240, 50, 60), glow=(255, 60, 60), hem=(110, 10, 16),
                 gold=(150, 140, 110), grip=(56, 18, 16), tassel=(230, 90, 80)),
         shape=dict(blade_len=36, w0=15.5, w1=11.0, t0=4.2, t1=3.2, segs=7,
                    guard="bone", bone_spikes=True, rune_bars=2)),

    # ---------------- 镰刀 ----------------
    dict(key="gouhun", form="scythe", cn="勾魂镰", tiers=(1, 3),
         color=P((120, 100, 150), (192, 174, 222), (40, 28, 60), (190, 120, 255),
                 (190, 120, 255), glow=(190, 120, 255), hem=(70, 44, 110),
                 gold=(190, 170, 120), grip=(44, 30, 60)),
         shape=dict(shaft=34, arc_r=17.0, sweep=110, blade_h=6.2, skull=True,
                    rune_ring=False, moon=False)),
    dict(key="xueyue", form="scythe", cn="血月弯镰", tiers=(4, 6),
         color=P((150, 62, 62), (222, 142, 132), (50, 8, 10), (255, 90, 90),
                 (255, 84, 84), glow=(255, 80, 80), hem=(120, 12, 18),
                 gold=(210, 170, 100), grip=(58, 10, 12)),
         shape=dict(shaft=34, arc_r=18.0, sweep=118, blade_h=6.8, skull=False,
                    moon=True)),
    dict(key="yougu", form="scythe", cn="幽骨冥纹镰", tiers=(7, 8),
         color=P((190, 184, 166), (232, 228, 212), (30, 50, 50), (102, 242, 200),
                 (102, 242, 200), glow=(102, 242, 200), hem=(36, 92, 84),
                 gold=(150, 160, 120), grip=(40, 60, 56)),
         shape=dict(shaft=34, arc_r=17.5, sweep=112, blade_h=6.4, bone_shaft=True)),
    dict(key="xukong", form="scythe", cn="虚空割裂镰", tiers=(9, 10),
         color=P((52, 44, 66), (120, 104, 160), (18, 14, 26), (176, 96, 255),
                 (176, 96, 255), glow=(176, 96, 255), hem=(60, 30, 90),
                 gold=(140, 120, 180), grip=(24, 18, 34)),
         shape=dict(shaft=35, arc_r=19.0, sweep=124, blade_h=7.0,
                    floating=3, rune_ring=True)),

    # ---------------- 长枪 ----------------
    dict(key="pojun", form="spear", cn="破军枪", tiers=(1, 3),
         color=P((126, 124, 120), (206, 202, 194), (56, 42, 24), (214, 172, 88),
                 (240, 210, 120), glow=(240, 210, 120), hem=(96, 72, 34),
                 gold=(232, 196, 110), grip=(58, 42, 24), tassel=(210, 46, 40)),
         shape=dict(shaft=42, head_len=16, head_w=6.0, tassel="red",
                    cloud_rings=0, lightning=False, lugs=True)),
    dict(key="youlong", form="spear", cn="游龙穿云枪", tiers=(4, 6),
         color=P((168, 210, 200), (236, 250, 246), (28, 66, 58), (120, 255, 220),
                 (140, 255, 230), glow=(140, 255, 230), hem=(40, 110, 96),
                 gold=(200, 220, 160), grip=(30, 60, 54), tassel=(160, 250, 225)),
         shape=dict(shaft=44, head_len=18, head_w=5.0, tassel="white",
                    cloud_rings=2, lightning=False, snake_head=True, lugs=True)),
    dict(key="leiji", form="spear", cn="雷脊玄铁枪", tiers=(7, 8),
         color=P((88, 88, 128), (168, 168, 220), (20, 20, 50), (150, 132, 255),
                 (170, 150, 255), glow=(170, 150, 255), hem=(50, 40, 100),
                 gold=(180, 170, 220), grip=(28, 24, 60), tassel=(170, 160, 255)),
         shape=dict(shaft=44, head_len=15, head_w=5.4, tassel="blue",
                    lightning=True, serrated_head=True, lugs=True)),
    dict(key="xueying", form="spear", cn="血缨魔纹枪", tiers=(9, 10),
         color=P((132, 72, 72), (202, 152, 152), (58, 10, 12), (255, 62, 62),
                 (255, 70, 70), glow=(255, 66, 66), hem=(110, 10, 16),
                 gold=(170, 150, 100), grip=(52, 10, 14), tassel=(240, 40, 50)),
         shape=dict(shaft=46, head_len=16, head_w=6.2, tassel="big",
                    groove=True, rune_bars=4, lugs=True)),

    # ---------------- 棍 ----------------
    dict(key="jiangmo", form="staff", cn="降魔棍", tiers=(1, 3),
         color=P((124, 94, 52), (194, 162, 112), (70, 50, 26), (220, 180, 90),
                 (240, 210, 120), glow=(240, 210, 120), hem=(100, 70, 30),
                 gold=(226, 190, 96), grip=(64, 46, 24)),
         shape=dict(length=40, radius=2.7, bands=3, square_ends=True, sutra=3)),
    dict(key="panlong", form="staff", cn="盘龙棍", tiers=(4, 6),
         color=P((152, 122, 42), (232, 202, 116), (44, 58, 38), (130, 245, 170),
                 (150, 255, 190), glow=(150, 255, 190), hem=(70, 100, 50),
                 gold=(240, 206, 110), grip=(54, 40, 26)),
         shape=dict(length=41, radius=2.8, bands=2, coil=10, dragon_head=True)),
    dict(key="zhenhun", form="staff", cn="镇魂玄木棍", tiers=(7, 8),
         color=P((62, 52, 42), (112, 96, 76), (34, 28, 20), (140, 255, 200),
                 (140, 255, 200), glow=(140, 255, 200), hem=(50, 70, 56),
                 gold=(130, 120, 90), grip=(40, 32, 22)),
         shape=dict(length=42, radius=2.7, bands=2, ghost_fire=2, chain=2)),
    dict(key="shougu", form="staff", cn="兽骨铃纹棍", tiers=(9, 10),
         color=P((204, 192, 164), (242, 236, 216), (66, 52, 34), (210, 180, 120),
                 (230, 200, 130), glow=(230, 200, 130), hem=(90, 72, 44),
                 gold=(200, 170, 110), grip=(60, 46, 30)),
         shape=dict(length=42, radius=2.8, bones=5, bells=True)),
]


def guard_sword(guard, style):
    if style == "cloud":
        add(guard, -2.2, 2.2, 0.0, 1.8, -1.6, 1.6)
        for s in (-1, 1):
            x0, x1 = (2.2, 4.6) if s > 0 else (-4.6, -2.2)
            add(guard, x0, x1, 0.4, 1.3, -1.1, 1.1)
            x0, x1 = (3.8, 5.2) if s > 0 else (-5.2, -3.8)
            add(guard, x0, x1, 0.8, 1.6, -0.7, 0.7, "jade")
    elif style == "flame":
        add(guard, -2.6, 2.6, 0.0, 2.0, -1.7, 1.7)
        for s in (-1, 1):
            for dx, dy, w, h in ((1.2, 0.4, 2.4, 1.6), (2.6, 1.3, 2.2, 2.4),
                                 (3.7, 3.0, 1.6, 3.0)):
                x0 = s * dx
                cx = x0 + s * w / 2
                add(guard, cx - w / 2, cx + w / 2, dy, dy + h, -1.0, 1.0)
            add(guard, 3.4 * s, 4.6 * s, 4.4, 5.4, -0.7, 0.7, "jade")
    elif style == "square":
        add(guard, -4.4, 4.4, 0.0, 2.0, -2.0, 2.0)
        for s in (-1, 1):
            x0 = 3.0 * s
            add(guard, min(x0, x0 + s * 0.6), max(x0, x0 + s * 0.6), 0.4, 1.4,
                -2.4, 2.4, "steel")
        for i in range(3):
            add(guard, -3.2 + i * 2.4, -2.7 + i * 2.4, 2.0, 2.7, -1.2, 1.2,
                "steel")
    elif style == "small":
        add(guard, -2.0, 2.0, 0.0, 1.6, -1.5, 1.5)
        add(guard, -0.9, 0.9, 0.2, 1.5, -1.9, -1.5, "jade")
    elif style == "bar":
        add(guard, -9.0, 9.0, 0.0, 2.2, -2.2, 2.2)
        for s in (-1, 1):
            x0 = 8.2 * s
            add(guard, min(x0, x0 + s * 1.4), max(x0, x0 + s * 1.4), 0.6, 2.8,
                -1.8, 1.8, "steel")
        add(guard, -1.0, 1.0, 0.4, 2.4, -2.4, -1.9, "steel")
    elif style == "stone":
        add(guard, -8.5, 8.5, 0.0, 3.0, -2.8, 2.8)
        for s in (-1, 1):
            x0 = 2.4 * s
            add(guard, min(x0, x0 + s * 3.6), max(x0, x0 + s * 3.6), 0.4, 3.0,
                -2.4, 2.4)
            add(guard, min(x0, x0 + s * 2.4), max(x0, x0 + s * 2.4), 3.0, 4.4,
                -1.6, 1.6, "steel")
            add(guard, min(6.6 * s, 7.6 * s), max(6.6 * s, 7.6 * s), 0.6, 2.4,
                -1.9, 1.9, "steel")
    elif style == "dragon":
        add(guard, -7.6, 7.6, 0.0, 2.6, -2.4, 2.4)
        for s in (-1, 1):
            x0 = 2.8 * s
            add(guard, min(x0, x0 + s * 3.0), max(x0, x0 + s * 3.0), 0.6, 2.8,
                -1.9, 1.9)
            add(guard, min(x0, x0 + s * 1.8), max(x0, x0 + s * 1.8), 2.8, 4.4,
                -1.2, 1.2, "jade")
            add(guard, min(5.8 * s, 6.9 * s), max(5.8 * s, 6.9 * s), 2.0, 3.8,
                -1.6, 1.6, "steel")
    elif style == "bone":
        add(guard, -7.0, 7.0, 0.0, 2.2, -2.2, 2.2, "steel")
        for s in (-1, 1):
            for i in range(4):
                x0 = s * (2.0 + i * 1.2)
                add(guard, min(x0, x0 + s * 0.9), max(x0, x0 + s * 0.9),
                    1.4 + i * 0.7, 2.4 + i * 1.0, -0.7, 0.7)


def build_sword(spec):
    if spec["shape"].get("w01r"):
        return build_frost_sword()
    g = Geo("geometry.career_sword_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 9.2, 0), parent="grip")
    top = {"cloud": 10.8, "flame": 11.2, "square": 10.8, "small": 10.6,
           "bar": 11.4, "stone": 11.8, "dragon": 11.6, "bone": 11.4}
    y0 = top.get(sh.get("guard", "cloud"), 10.8)
    blade = g.bone("blade", (0, y0, 0), parent="guard")
    add(grip, -1.7, 1.7, 0.0, 2.0, -1.7, 1.7)
    add(grip, -1.3, 1.3, 2.0, 2.4, -1.3, 1.3, "steel")
    add(grip, -1.1, 1.1, 2.4, 9.2, -1.1, 1.1, "cloth")
    for y in (3.4, 5.4, 7.4):
        add(grip, -1.3, 1.3, y, y + 0.4, -1.3, 1.3, "steel")
    guard_sword(guard, sh.get("guard", "cloud"))
    y1 = y0 + sh["blade_len"]
    taper_blade(blade, y0, y1, sh["w0"], sh["w1"], sh["t0"], sh["t1"],
                sh.get("segs", 4))
    # 刃口高光条（贴刃两侧，让剑"开刃"）
    for s in (-1, 1):
        x0 = sh["w0"] * 0.50 * s
        add(blade, min(x0, x0 - 0.16 * s), max(x0, x0 - 0.16 * s), y0 + 0.4,
            y1 - 0.6, -sh["t0"] * 0.30, sh["t0"] * 0.30, "steel")
    if sh.get("groove"):
        cw = sh["w0"] * 0.24
        add(blade, -cw, cw, y0 + 0.5, y1 - 0.5, -sh["t0"] * 0.40,
            sh["t0"] * 0.40, "steel")
        add(blade, -cw * 0.4, cw * 0.4, y0 + 0.8, y1 - 0.8,
            -sh["t0"] * 0.46, -sh["t0"] * 0.38, "metal")
    if sh.get("serrated"):
        for i in range(5):
            yy = y0 + 2.2 + i * (y1 - y0 - 3) / 5.0
            add(blade, sh["w0"] * 0.40, sh["w0"] * 0.68, yy, yy + 1.4,
                -0.7, 0.7, "steel")
    if sh.get("rune_bars"):
        for i in range(sh["rune_bars"]):
            yy = y0 + (i + 0.6) * (y1 - y0) / (sh["rune_bars"] + 0.2)
            add(blade, sh["w0"] * 0.16, sh["w0"] * 0.34, yy, yy + 1.2,
                -0.08, 0.08, "rune")
    step_point(blade, y1, y1 + 2.6, sh["w1"], sh["t1"], 3)
    if sh.get("skull"):
        add(grip, -2.2, 2.2, -0.7, 1.0, -2.2, 2.2, "steel")
        add(grip, -1.2, 1.2, 1.0, 2.2, -2.5, -1.7, "jade")
        add(grip, -1.2, 1.2, 1.0, 2.2, 1.7, 2.5, "jade")
    if sh.get("tassel"):
        t = g.bone("tassel", (0, 0, 0), parent="grip")
        for i in range(4):
            add(t, -0.7, 0.7, -1.1 - i * 1.0, -0.1 - i * 1.0,
                0.4 + i * 0.15, 1.2 + i * 0.15, "cloth")
    return g


def build_frost_sword():
    """霜纹古篆剑：连续剑身（无断口），符环贴身，玉简刻痕贴刃。"""
    g = Geo("geometry.career_sword_shuangwen", tex=(256, 256))
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 9.4, 0), parent="grip")
    blade = g.bone("blade", (0, 11.4, 0), parent="guard")
    ring_a = g.bone("ring_a", (0, 21.0, 0), parent="blade")
    ring_b = g.bone("ring_b", (0, 27.0, 0), parent="blade")
    add(grip, -1.6, 1.6, 0.0, 1.9, -1.6, 1.6)
    add(grip, -1.25, 1.25, 1.9, 2.3, -1.25, 1.25, "steel")
    add(grip, -1.05, 1.05, 2.3, 9.4, -1.05, 1.05, "cloth")
    for y in (3.5, 5.6, 7.7):
        add(grip, -1.25, 1.25, y, y + 0.4, -1.25, 1.25, "steel")
    add(guard, -2.3, 2.3, 9.4, 11.4, -1.8, 1.8)
    for s in (-1, 1):
        add(guard, 2.3 * s, 4.9 * s, 10.0, 11.2, -1.1, 1.1)
        add(guard, 3.2 * s, 4.6 * s, 11.2, 12.6, -0.8, 0.8, "jade")
        add(guard, 4.2 * s, 5.6 * s, 10.2, 11.6, -0.6, 0.6, "jade")
    add(guard, -0.9, 0.9, 9.6, 11.6, -2.1, -1.8, "rune")
    add(guard, -0.9, 0.9, 9.6, 11.6, 1.8, 2.1, "rune")
    y0, y1 = 11.4, 39.0
    taper_blade(blade, y0, y1, 4.8, 2.9, 1.9, 1.2, 7)
    for s in (-1, 1):
        x0 = 2.4 * s
        add(blade, min(x0, x0 - 0.2 * s), max(x0, x0 - 0.2 * s), y0 + 0.4,
            y1 - 0.6, -0.42, 0.42, "rune")
    # 篆文玉简：贴在刃面上（不悬空）
    for i, yy in enumerate((17.0, 23.5, 30.5)):
        add(blade, -0.8, 0.8, yy, yy + 1.6, -1.02, -0.9, "jade")
        add(blade, -0.5, 0.5, yy + 0.2, yy + 0.6, -1.06, -0.98, "rune")
        add(blade, -0.6, 0.6, yy + 0.9, yy + 1.2, -1.06, -0.98, "rune")
    step_point(blade, y1, 41.8, 2.9, 1.2, 3)
    ring_cubes(ring_a, 10, 21.0, 3.1, 0.5, mat="jade")
    ring_cubes(ring_b, 10, 27.0, 2.9, 0.5, mat="jade")
    t = g.bone("tassel", (0, 0, 0), parent="grip")
    for i in range(4):
        add(t, -0.65, 0.65, -1.1 - i * 1.0, -0.1 - i * 1.0,
            0.4 + i * 0.12, 1.1 + i * 0.12, "cloth")
    return g


def build_greatsword(spec):
    g = Geo("geometry.career_greatsword_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    grip = g.bone("grip", (0, 0, 0))
    guard = g.bone("guard", (0, 13.5, 0), parent="grip")
    blade = g.bone("blade", (0, 17.0, 0), parent="guard")
    add(grip, -2.6, 2.6, 0.0, 2.6, -2.6, 2.6)
    add(grip, -2.0, 2.0, 2.6, 3.2, -2.0, 2.0, "steel")
    add(grip, -1.8, 1.8, 3.2, 13.5, -1.8, 1.8, "cloth")
    for y in (5.0, 7.6, 10.2):
        add(grip, -1.95, 1.95, y, y + 0.55, -1.95, 1.95, "steel")
    guard_sword(guard, sh.get("guard", "bar"))
    y0, y1 = 17.0, 17.0 + sh["blade_len"]
    w0, w1 = sh["w0"], sh["w1"]
    t0, t1 = sh["t0"], sh["t1"]
    taper_blade(blade, y0, y1, w0, w1, t0, t1, sh.get("segs", 6))
    # 剑根加宽（让巨剑在手里"压手"）
    add(blade, -w0 * 0.62, w0 * 0.62, y0, y0 + 4.2, -t0 * 0.62, t0 * 0.62,
        "metal")
    for s in (-1, 1):
        x0 = w0 * 0.5 * s
        add(blade, min(x0, x0 - 0.22 * s), max(x0, x0 - 0.22 * s), y0 + 0.4,
            y1 - 0.8, -t0 * 0.32, t0 * 0.32, "steel")
    add(blade, -1.6, 1.6, y0 + 0.6, y1 - 0.8, -t0 * 0.52, t0 * 0.52, "steel")
    if sh.get("groove"):
        add(blade, -2.6, 2.6, y0 + 0.8, y1 - 1.0, -t0 * 0.58, -t0 * 0.50,
            "metal")
        add(blade, -2.6, 2.6, y0 + 0.8, y1 - 1.0, t0 * 0.50, t0 * 0.58,
            "metal")
    if sh.get("spine"):
        for i in range(sh["spine"]):
            yy = y0 + 1.6 + i * (y1 - y0 - 3.2) / sh["spine"]
            add(blade, -w0 * 0.5, w0 * 0.5, yy, yy + 1.4,
                -t0 * 0.72, -t0 * 0.44, "jade")
    if sh.get("rocks"):
        add(blade, -w0 * 0.6, w0 * 0.6, y0 + 1.0, y0 + 5.0,
            -t0 * 0.9, t0 * 0.9, "steel")
        for s in (-1, 1):
            add(blade, w0 * 0.35 * s, w0 * 0.62 * s, y0 + 5.0, y0 + 7.4,
                -t0 * 0.7, t0 * 0.7, "metal")
    if sh.get("bone_spikes"):
        for i in range(5):
            yy = y0 + 2.0 + i * 6.4
            add(blade, -w0 * 0.66, -w0 * 0.50, yy, yy + 2.6, -0.7, 0.7,
                "steel")
            add(blade, w0 * 0.50, w0 * 0.66, yy + 3.0, yy + 5.6, -0.7, 0.7,
                "steel")
    if sh.get("rune_bars"):
        for i in range(sh["rune_bars"]):
            yy = y0 + (i + 0.6) * (y1 - y0) / (sh["rune_bars"] + 0.2)
            add(blade, w0 * 0.22, w0 * 0.36, yy, yy + 1.6, -0.08, 0.08,
                "rune")
    step_point(blade, y1, y1 + 3.2, w1, t1, 3)
    return g


def build_scythe(spec):
    g = Geo("geometry.career_scythe_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, sh["shaft"], 0), parent="shaft")
    blade = g.bone("blade", (0, sh["shaft"], 0), parent="head")
    add(shaft, -2.0, 2.0, 0.0, sh["shaft"], -2.0, 2.0, "cloth")
    for y in range(3, int(sh["shaft"]) - 3, 5):
        add(shaft, -2.3, 2.3, y, y + 0.8, -2.3, 2.3, "steel")
    add(shaft, -2.7, 2.7, 0.0, 2.4, -2.7, 2.7)
    add(shaft, -2.5, 2.5, 2.4, 3.0, -2.5, 2.5, "steel")
    add(head, -2.8, 2.8, -2.6, 2.6, -2.8, 2.8)
    add(head, -0.9, 0.9, -1.6, 1.6, -3.2, -2.6, "rune")
    if sh.get("bone_shaft"):
        for i in range(5):
            yy = 4.0 + i * (sh["shaft"] - 8) / 5.0
            add(shaft, -2.5, 2.5, yy, yy + 1.8, -2.5, 2.5, "steel")
            add(shaft, -1.2, 1.2, yy + 1.8, yy + 2.3, -1.2, 1.2, "jade")
    if sh.get("skull"):
        add(head, -1.6, 1.6, 2.6, 5.0, -1.9, 1.9, "steel")
        add(head, -0.9, 0.9, 3.2, 4.4, -2.3, -1.9, "jade")
    if sh.get("moon"):
        for i in range(18):
            a = 2 * math.pi * i / 18
            cx, cy = math.cos(a) * 4.4, sh["shaft"] + 3.6 + math.sin(a) * 4.4
            head.add((cx - 0.5, cy - 0.5, -0.5), (1.0, 1.0, 1.0), mat="jade")
    # 月牙刃：外缘薄刃 + 内缘厚脊，收出尖锋
    cx, cy, r = 0.0, sh["shaft"] - 12.0, sh["arc_r"]
    segs = 11
    for i in range(segs):
        a0 = math.radians(90 - i * (sh["sweep"] / segs))
        a1 = math.radians(90 - (i + 1) * (sh["sweep"] / segs))
        p0 = (cx + r * math.cos(a0), cy + r * math.sin(a0))
        p1 = (cx + r * math.cos(a1), cy + r * math.sin(a1))
        mx, my = (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2
        length = math.dist(p0, p1) + 0.5
        deg = math.degrees(math.atan2(p1[1] - p0[1], p1[0] - p0[0]))
        h = sh["blade_h"] * (1.0 - 0.84 * (i / segs))
        # 刃身（薄）
        blade.add((mx - length / 2, my - 1.0, -0.45),
                  (length, h, 0.9), mat="metal",
                  rotation=(0, 0, deg), pivot=(mx, my, 0))
        # 外缘亮刃（更薄、更靠外）
        ox = mx + (math.cos(math.radians(deg + 90)) * (h * 0.42))
        oy = my + (math.sin(math.radians(deg + 90)) * (h * 0.42))
        blade.add((ox - length / 2, oy - 0.5, -0.25),
                  (length, 1.0, 0.5), mat="steel",
                  rotation=(0, 0, deg), pivot=(ox, oy, 0))
        if i == segs - 1:
            blade.add((p1[0] - 1.0, p1[1] - 1.0, -0.2), (2.0, 2.0, 0.4),
                      mat="steel", rotation=(0, 0, deg), pivot=(p1[0], p1[1], 0))
    if sh.get("floating"):
        for i in range(sh["floating"]):
            a = math.radians(46 + i * 20)
            px = math.cos(a) * (sh["arc_r"] + 2.4)
            py = (sh["shaft"] - 12.0) + math.sin(a) * (sh["arc_r"] + 2.4)
            add(blade, px - 0.9, px + 0.9, py - 0.9, py + 0.9, -0.5, 0.5,
                "jade")
    if sh.get("rune_ring"):
        ring_cubes(head, 12, sh["shaft"] + 5.5, 3.0, 0.65, mat="rune")
    return g


def build_spear(spec):
    g = Geo("geometry.career_spear_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    shaft = g.bone("shaft", (0, 0, 0))
    head = g.bone("head", (0, sh["shaft"], 0), parent="shaft")
    add(shaft, -1.8, 1.8, 0.0, sh["shaft"], -1.8, 1.8, "cloth")
    for y in range(3, int(sh["shaft"]) - 3, 5):
        add(shaft, -2.1, 2.1, y, y + 0.7, -2.1, 2.1, "steel")
    add(shaft, -2.3, 2.3, 0.0, 2.4, -2.3, 2.3)
    add(shaft, -2.0, 2.0, 2.4, 3.0, -2.0, 2.0, "steel")
    hw, hl = sh["head_w"], sh["head_len"]
    add(head, -hw * 0.72, hw * 0.72, -2.8, 0.6, -hw * 0.72, hw * 0.72)
    add(head, -hw * 0.52, hw * 0.52, -0.2, 2.2, -hw * 0.52, hw * 0.52, "steel")
    if sh.get("lugs"):
        for s in (-1, 1):
            add(head, hw * 0.5 * s, hw * 1.15 * s, -0.6, 1.4, -0.7, 0.7,
                "metal")
            add(head, hw * 0.9 * s, hw * 1.3 * s, 1.4, 2.6, -0.5, 0.5, "rune")
    if sh.get("lightning"):
        for i in range(5):
            add(head, hw * 0.55 + i * 0.55, hw * 0.55 + i * 0.55 + 0.5,
                -2.0 + i * 0.8, -1.2 + i * 0.8, -0.6, 0.6, "rune")
            add(head, -hw * 0.55 - i * 0.55 - 0.5,
                -hw * 0.55 - i * 0.55, -2.0 + i * 0.8, -1.2 + i * 0.8,
                -0.6, 0.6, "rune")
    add(head, -hw * 0.42, hw * 0.42, 2.2, 3.6, -hw * 0.34, hw * 0.34, "steel")
    taper_blade(head, 3.6, 3.6 + hl, hw * 0.9, hw * 0.26, hw * 0.36,
                hw * 0.14, 5)
    step_point(head, 3.6 + hl, 3.6 + hl + 3.0, hw * 0.26, hw * 0.14, 3)
    if sh.get("groove"):
        add(head, -hw * 0.08, hw * 0.08, 4.0, 3.2 + hl, -hw * 0.20,
            hw * 0.20, "rune")
    if sh.get("snake_head"):
        add(head, -hw * 0.5, hw * 0.5, 2.6, 4.4, -hw * 0.45, hw * 0.45,
            "jade")
        for s in (-1, 1):
            add(head, hw * 0.4 * s, hw * 0.62 * s, 3.2, 3.8, -hw * 0.5,
                -hw * 0.34, "rune")
    if sh.get("cloud_rings"):
        for i in range(sh["cloud_rings"]):
            ring_cubes(head, 12, 7.0 + i * 6.0, 3.3, 0.65,
                       mat="jade", phase=i * 0.4)
    tassel = g.bone("tassel", (0, sh["shaft"], 0), parent="shaft")
    if sh.get("tassel") == "big":
        for i, dz in enumerate((-2.8, -1.0, 0.8, 2.6, -1.9, 1.7)):
            rot = 20 if i % 2 == 0 else -20
            add(tassel, -1.0, 1.0, -8.0, -1.2, dz - 0.6, dz + 0.6, "cloth")
            tassel.cubes[-1].rotation = (rot, 0, 0)
            tassel.cubes[-1].pivot = (0, -1.2, dz)
    elif sh.get("tassel") == "red":
        for dz in (-1.8, -0.3, 1.2, 2.7):
            add(tassel, -0.8, 0.8, -5.4, -1.2, dz - 0.55, dz + 0.55,
                "cloth")
    else:
        for dz in (-0.8, 0.8):
            add(tassel, -0.7, 0.7, -4.4, -1.2, dz - 0.6, dz + 0.6, "cloth")
    if sh.get("rune_bars"):
        for i in range(sh["rune_bars"]):
            yy = 5.0 + i * 3.2
            add(shaft, -1.92, -1.78, yy, yy + 1.1, -1.1, 1.1, "rune")
    return g


def build_staff(spec):
    g = Geo("geometry.career_staff_" + spec["key"], tex=(256, 256))
    sh = spec["shape"]
    key = spec["key"]
    shaft = g.bone("shaft", (0, 0, 0))
    L, R = sh["length"], sh["radius"]
    add(shaft, -R, R, 0.0, L, -R, R, "cloth")
    for y in range(3, int(L) - 3, 5):
        add(shaft, -R - 0.3, R + 0.3, y, y + 0.9, -R - 0.3, R + 0.3, "steel")
    # 端头（加粗，避免"一根棍"）
    for y in (0.0, L - 2.8):
        add(shaft, -R - 1.1, R + 1.1, y, y + 2.8, -R - 1.1, R + 1.1, "metal")
        add(shaft, -R - 0.5, R + 0.5, y + 2.8, y + 3.5, -R - 0.5, R + 0.5,
            "steel")
    if key == "jiangmo":
        for i in range(3):
            yy = L + 1.0 + i * 1.8
            for s in (-1, 1):
                add(shaft, -1.0, 1.0, yy, yy + 1.0, s * 1.9 - 0.35,
                    s * 1.9 + 0.35, "jade")
        add(shaft, -0.6, 0.6, L + 6.4, L + 8.6, -0.6, 0.6, "steel")
        add(shaft, -0.3, 0.3, L + 8.6, L + 9.6, -0.3, 0.3, "rune")
        for i in range(3):
            add(shaft, -R - 0.2, R + 0.2, L * (i + 1) / 4, L * (i + 1) / 4 + 1.0,
                -R - 0.2, R + 0.2, "jade")
    elif key == "panlong":
        for i in range(10):
            yy = 5.0 + i * (L - 15.0) / 10
            a = i * 2.1
            cx = math.cos(a) * (R + 0.9)
            cz = math.sin(a) * (R + 0.9)
            add(shaft, cx - 0.7, cx + 0.7, yy, yy + 1.3, cz - 0.7, cz + 0.7,
                "metal")
        # 龙头
        add(shaft, -2.2, 2.2, L - 1.0, L + 2.6, -2.2, 2.2, "metal")
        add(shaft, -1.6, 1.6, L + 2.6, L + 4.2, -1.8, 1.8, "metal")
        add(shaft, -0.9, 0.9, L + 2.0, L + 3.4, -2.6, -2.0, "jade")
        add(shaft, -0.9, 0.9, L + 2.0, L + 3.4, 2.0, 2.6, "jade")
        for s in (-1, 1):
            add(shaft, 1.2 * s, 2.6 * s, L + 3.0, L + 4.6, -0.7, 0.7, "steel")
        add(shaft, -0.7, 0.7, L + 4.2, L + 6.0, -0.6, 0.6, "rune")
    elif key == "zhenhun":
        for y0, y1 in ((0.0, 3.4), (L - 3.4, L)):
            add(shaft, -2.6, 2.6, y0, y1, -2.6, 2.6, "metal")
            add(shaft, -1.6, 1.6, y0 + 0.6, y1 - 0.6, -2.9, 2.9, "rune")
        for i in range(3):
            yy = 8.0 + i * 5.0
            add(shaft, R + 0.2, R + 0.9, yy, yy + 1.2, -0.5, 0.5, "steel")
            add(shaft, R + 0.9, R + 1.5, yy + 0.4, yy + 0.8, -0.3, 0.3,
                "steel")
    elif key == "shougu":
        for i in range(5):
            yy = 5.0 + i * (L - 12.0) / 5
            add(shaft, -R - 0.9, R + 0.9, yy, yy + 2.0, -R - 0.9, R + 0.9,
                "steel")
            add(shaft, -R - 0.4, R + 0.4, yy + 2.0, yy + 2.8, -R - 0.4,
                R + 0.4, "jade")
        # 兽骨头
        add(shaft, -2.4, 2.4, L - 1.0, L + 2.8, -2.2, 2.2, "steel")
        add(shaft, -1.7, 1.7, L + 2.8, L + 4.4, -1.6, 1.6, "steel")
        add(shaft, -0.9, 0.9, L + 1.4, L + 2.6, -2.7, -2.1, "rune")
        add(shaft, 0.9, 1.7, L + 1.4, L + 2.6, -2.7, -2.1, "rune")
        for s in (-1, 1):
            add(shaft, 2.0 * s, 3.2 * s, L + 1.0, L + 3.0, -0.6, 0.6, "steel")
        add(shaft, -1.6, 1.6, 0.2, 2.0, -1.6, 1.6, "jade")
        add(shaft, -0.5, 0.5, 2.0, 3.0, -0.5, 0.5, "rune")
    for i in range(3):
        add(shaft, -R - 0.4, R + 0.4, L * (i + 1) / 4, L * (i + 1) / 4 + 1.0,
            -R - 0.4, R + 0.4, "jade")
    return g
