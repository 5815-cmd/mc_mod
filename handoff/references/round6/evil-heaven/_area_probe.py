# -*- coding: utf-8 -*-
"""小工具：统计 HeavenLord spec 的 UV 足迹总面积与各部件占比（256² 预算）。"""
import importlib.util
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("hl_spec", os.path.join(HERE, "spec.py"))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
S = m.SPEC
tw, th = S.tex
rows = []
total = 0
for p in S.parts:
    for b in p.boxes:
        fw, fh = b.footprint()
        area = fw * fh
        total += area
        rows.append((area, fw, fh, p.name, (b.sx, b.sy, b.sz), b.mat))
rows.sort(reverse=True)
print("cube=%d  贴图=%dx%d  面积预算=%d  足迹总面积=%d (%.1f%% of tex)"
      % (S.total_cubes(), tw, th, tw * th, total, 100.0 * total / (tw * th)))
print("最大 25 个盒子：")
for area, fw, fh, name, size, mat in rows[:25]:
    print("  %5d px²  %3dx%-3d  %-12s %-18s %s" % (area, fw, fh, name, size, mat))
