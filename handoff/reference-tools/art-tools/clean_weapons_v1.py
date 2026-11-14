# -*- coding: utf-8 -*-
"""清理 v1 的 15 个武器外观资源（已由 21 个名器外观取代），先备份到工程目录。"""
import os
import shutil

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
RES = os.path.join(WS, "xiuxian-mod", "src", "main", "resources", "assets", "hongmeng")
BAK = os.path.join(WS, "工程", "art", "w01r", "weapons_v1_backup")
os.makedirs(BAK, exist_ok=True)

forms = ["sword", "greatsword", "scythe", "spear", "staff"]
styles = ["low", "mid", "high"]
files = []
for form in forms:
    for style in styles:
        fam = "career_%s_%s" % (form, style)
        files += [
            ("geo", fam + ".geo.json"),
            ("textures/item", fam + ".png"),
            ("textures/item", fam + "_glowmask.png"),
            ("animations", fam + ".animation.json"),
        ]
moved = 0
for folder, name in files:
    src = os.path.join(RES, folder, name)
    if os.path.exists(src):
        dst = os.path.join(BAK, name if folder != "textures/item" else "tex_" + name)
        shutil.copyfile(src, dst)
        os.remove(src)
        moved += 1
print("backed up & removed", moved, "files ->", BAK)
