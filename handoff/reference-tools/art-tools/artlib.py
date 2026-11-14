# -*- coding: utf-8 -*-
"""
G1 白模工具链（武器盔甲师自用）
================================
两个东西，都能离线跑、不启动游戏：

1. **Geo 构造器**：生成 Blockbench / GeckoLib 1.12.0 格式的 `.geo.json`
   （立方体 + 骨骼 + 旋转，坐标一律"模型空间绝对值"，与官方示例 bat.geo.json 一致）。
2. **软件正交光栅器**：把 geo 渲染成三视图/白模 PNG（z-buffer + 平面着色），
   支持把"玩家占位体"一起画进来查穿模。

约定
----
- 1 unit = 1/16 格；**脚底 y=0、头顶 y=32**（玩家空间，GeckoLib 护甲标准）。
- 武器：**握把底 [0,0,0]，刃身朝 +Y**（GeoWeaponItem 契约）。
- 所有 cube 的 origin 是"最小角"，size 是"尺寸"；uv 在 G1 阶段随便填，G2 再上图集。
- 骨骼 pivot / cube 坐标都是**模型绝对值**（不是父级相对），
  因为 GeckoLib 的骨骼变换是 T(pivot)·R·T(-pivot) 逐级相乘，绝对值才能对得上官方示例。

用法
----
    from artlib import Geo, box, blade, render_views, contact_sheet
"""

from __future__ import annotations

import json
import math
import os
from dataclasses import dataclass, field

import numpy as np
from PIL import Image, ImageDraw, ImageFont

# ---------------------------------------------------------------------------
# 基础数学
# ---------------------------------------------------------------------------

def _deg(a):
    return np.deg2rad(float(a))


def rot_x(a):
    m = np.eye(4)
    c, s = math.cos(a), math.sin(a)
    m[1, 1], m[1, 2] = c, -s
    m[2, 1], m[2, 2] = s, c
    return m


def rot_y(a):
    m = np.eye(4)
    c, s = math.cos(a), math.sin(a)
    m[0, 0], m[0, 2] = c, s
    m[2, 0], m[2, 2] = -s, c
    return m


def rot_z(a):
    m = np.eye(4)
    c, s = math.cos(a), math.sin(a)
    m[0, 0], m[0, 1] = c, -s
    m[1, 0], m[1, 1] = s, c
    return m


def trans(x, y, z):
    m = np.eye(4)
    m[0, 3], m[1, 3], m[2, 3] = x, y, z
    return m


def euler_deg(rot):
    """Blockbench 欧拉角（度）。GeckoLib 渲染顺序 Z→Y→X，最终矩阵 = Rx·Ry·Rz。"""
    if not rot:
        return np.eye(4)
    rx, ry, rz = (_deg(rot[0]), _deg(rot[1]), _deg(rot[2]))
    return rot_x(rx) @ rot_y(ry) @ rot_z(rz)


# ---------------------------------------------------------------------------
# Geo 数据模型
# ---------------------------------------------------------------------------

@dataclass
class Cube:
    origin: tuple
    size: tuple
    uv: tuple = (0, 0)
    rotation: tuple | None = None
    pivot: tuple | None = None
    inflate: float = 0.0
    mirror: bool = False
    mat: str = "metal"          # 只给渲染器用，不写进 json
    uv_faces: dict | None = None  # face -> (u, v, w, h) 像素，G2 逐面 UV

    def to_json(self):
        d = {
            "origin": [round(float(v), 3) for v in self.origin],
            "size": [round(float(v), 3) for v in self.size],
        }
        if self.uv_faces:
            faces = {}
            for k, (u, v, w, h) in self.uv_faces.items():
                faces[k] = {"uv": [int(u), int(v)], "uv_size": [int(w), int(h)]}
            d["uv"] = faces
        else:
            d["uv"] = [int(self.uv[0]), int(self.uv[1])]
        if self.rotation:
            d["rotation"] = [round(float(v), 2) for v in self.rotation]
        if self.pivot:
            d["pivot"] = [round(float(v), 3) for v in self.pivot]
        if self.inflate:
            d["inflate"] = round(float(self.inflate), 3)
        if self.mirror:
            d["mirror"] = True
        return d


@dataclass
class Bone:
    name: str
    pivot: tuple
    parent: str | None = None
    rotation: tuple | None = None
    mirror: bool | None = None
    cubes: list = field(default_factory=list)

    def add(self, origin, size, mat="metal", **kw):
        self.cubes.append(Cube(tuple(origin), tuple(size), mat=mat, **kw))
        return self

    def to_json(self):
        d = {"name": self.name, "pivot": [round(float(v), 3) for v in self.pivot]}
        if self.parent:
            d["parent"] = self.parent
        if self.rotation:
            d["rotation"] = [round(float(v), 2) for v in self.rotation]
        if self.mirror is not None:
            d["mirror"] = bool(self.mirror)
        if self.cubes:
            d["cubes"] = [c.to_json() for c in self.cubes]
        return d


class Geo:
    def __init__(self, identifier, tex=(128, 128), visible=(0, 0, 0)):
        self.identifier = identifier
        self.tex = tex
        self.visible = visible
        self.bones: list[Bone] = []
        self._by_name = {}

    def bone(self, name, pivot, parent=None, rotation=None, mirror=None):
        if name in self._by_name:
            raise ValueError("bone 重复: " + name)
        b = Bone(name, tuple(pivot), parent, rotation, mirror)
        self.bones.append(b)
        self._by_name[name] = b
        return b

    def get(self, name):
        return self._by_name[name]

    def cube_count(self):
        return sum(len(b.cubes) for b in self.bones)

    def bounds(self):
        lo = np.array([1e9, 1e9, 1e9])
        hi = np.array([-1e9, -1e9, -1e9])
        for b in self.bones:
            for c in b.cubes:
                o = np.array(c.origin, dtype=float)
                s = np.array(c.size, dtype=float)
                lo = np.minimum(lo, o)
                hi = np.maximum(hi, o + s)
        return lo, hi

    def to_json(self):
        lo, hi = self.bounds()
        w = max(1, int(round(hi[0] - lo[0])))
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": self.identifier,
                    "texture_width": self.tex[0],
                    "texture_height": self.tex[1],
                    "visible_bounds_width": max(4, w),
                    "visible_bounds_height": max(4, w),
                    "visible_bounds_offset": [0, self.visible[1], 0],
                },
                "bones": [b.to_json() for b in self.bones],
            }],
        }

    def save(self, path):
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(self.to_json(), f, ensure_ascii=False, indent=2)
        return path


# ---------------------------------------------------------------------------
# 建模小工具
# ---------------------------------------------------------------------------

def box(geo_or_bone, origin, size, mat="metal", **kw):
    """在 bone 上加一个立方体。"""
    geo_or_bone.add(origin, size, mat=mat, **kw)
    return geo_or_bone


def tube(bone, x0, x1, y0, y1, z0, z1, mat="metal", **kw):
    """轴对齐盒子（等价于 box，只是参数顺序更直观）。"""
    return box(bone, (x0, y0, z0), (x1 - x0, y1 - y0, z1 - z0), mat=mat, **kw)


def ring_cubes(bone, count, y, radius, size, mat="rune", tilt=0.0, phase=0.0):
    """在 XZ 平面上摆一圈小方块（符文环）。每块绕 Y 轴切向对齐。"""
    for i in range(count):
        a = phase + 2 * math.pi * i / count
        cx = math.cos(a) * radius
        cz = math.sin(a) * radius
        deg = math.degrees(a)
        rot = [tilt, -deg, 0.0]
        bone.add((cx - size / 2, y - size / 2, cz - size / 2),
                 (size, size, size), mat=mat, rotation=rot)


def taper_blade(bone, y0, y1, w0, w1, t0, t1, segments, mat="metal"):
    """由若干段组成的锥形剑身：中间厚脊 + 两侧薄刃，横截面近似菱形。"""
    for i in range(segments):
        f0 = i / segments
        f1 = (i + 1) / segments
        ya, yb = y0 + (y1 - y0) * f0, y0 + (y1 - y0) * f1
        w = w0 + (w1 - w0) * (f0 + f1) / 2
        t = t0 + (t1 - t0) * (f0 + f1) / 2
        half = w / 2
        inner = w * 0.18
        bone.add((-inner, ya, -t / 2), (inner * 2, yb - ya, t), mat="steel")
        for s in (-1, 1):
            x0 = -half if s < 0 else inner
            x1 = -inner if s < 0 else half
            bone.add((x0, ya, -t * 0.28), (x1 - x0, yb - ya, t * 0.56), mat=mat)


def step_point(bone, y0, y1, w, t, steps, mat="metal"):
    """用阶梯块收出剑尖（方块模型里最接近"尖"的做法）。"""
    for i in range(steps):
        f = i / steps
        ww = w * (1 - f)
        ya = y0 + (y1 - y0) * f
        yb = y0 + (y1 - y0) * (i + 1) / steps
        bone.add((-ww / 2, ya, -t * (1 - f * 0.5) / 2),
                 (ww, yb - ya, t * (1 - f * 0.5)), mat=mat)


# ---------------------------------------------------------------------------
# 网格化（geo -> 三角形）
# ---------------------------------------------------------------------------

_FACES = [
    # (法线, 4 个角的 [x,y,z] 位标志)
    ((-1, 0, 0), [(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)]),
    ((1, 0, 0), [(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)]),
    ((0, -1, 0), [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)]),
    ((0, 1, 0), [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)]),
    ((0, 0, -1), [(0, 0, 0), (0, 1, 0), (1, 1, 0), (1, 0, 0)]),
    ((0, 0, 1), [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]),
]

#: 与 _FACES 同序：west / east / down / up / north / south
_FACE_NAMES = ["west", "east", "down", "up", "north", "south"]


def _face_st(face_idx, flag):
    """面内的 (s, t) 纹理坐标：s 沿 u，t 沿 v（t=1 为面的上边）。"""
    x, y, z = flag
    if face_idx in (0, 1):      # ±X 面：s=深度 z，t=高度 y
        return (z, y)
    if face_idx in (2, 3):      # ±Y 面：s=宽 x，t=深 z
        return (x, z)
    return (x, y)               # ±Z 面：s=宽 x，t=高 y


def _bone_matrices(geo):
    out = {}
    for b in geo.bones:
        m = trans(*b.pivot) @ euler_deg(b.rotation) @ trans(*[-v for v in b.pivot])
        if b.parent:
            m = out[b.parent] @ m
        out[b.name] = m
    return out


def build_mesh(geo, hidden=()):
    """返回 [(v0,v1,v2, normal, mat, uv0,uv1,uv2)]，顶点为模型空间坐标（未 y 翻转）。

    uv 为归一化纹理坐标 (u, v)（v 向下）；没有逐面 UV 的 cube 返回 None。
    """
    mats = _bone_matrices(geo)
    tw, th = float(geo.tex[0]), float(geo.tex[1])
    tris = []
    for b in geo.bones:
        if b.name in hidden:
            continue
        bm = mats[b.name]
        for c in b.cubes:
            o = np.array(c.origin, dtype=float)
            s = np.array(c.size, dtype=float)
            if c.rotation or c.pivot:
                cp = np.array(c.pivot, dtype=float) if c.pivot else o + s / 2
                cm = trans(*cp) @ euler_deg(c.rotation) @ trans(*[-v for v in cp])
            else:
                cm = np.eye(4)
            world = bm @ cm
            corners = []
            for x in (0, 1):
                for y in (0, 1):
                    for z in (0, 1):
                        p = o + np.array([x * s[0], y * s[1], z * s[2]])
                        corners.append((p, (x, y, z)))
            nmat = world[:3, :3]

            def vert(idx):
                p4 = world @ np.array([*(corners[idx][0]), 1.0])
                return p4[:3]

            for fi, (n, flags) in enumerate(_FACES):
                vflags = list(flags)
                idxs = [f[0] * 4 + f[1] * 2 + f[2] for f in vflags]
                vs = [vert(i) for i in idxs]
                nn = nmat @ np.array(n, dtype=float)
                ln = np.linalg.norm(nn)
                if ln < 1e-9:
                    continue
                nn = nn / ln
                e1 = vs[1] - vs[0]
                e2 = vs[2] - vs[0]
                if np.dot(np.cross(e1, e2), nn) < 0:
                    vs[1], vs[2] = vs[2], vs[1]
                    vflags[1], vflags[2] = vflags[2], vflags[1]
                if c.uv_faces and _FACE_NAMES[fi] in c.uv_faces:
                    u0, v0, w, h = c.uv_faces[_FACE_NAMES[fi]]
                    uvs = []
                    for f in vflags:
                        fs, ft = _face_st(fi, f)
                        uvs.append(((u0 + fs * w) / tw, (v0 + (1 - ft) * h) / th))
                else:
                    uvs = [None, None, None, None]
                tris.append((vs[0], vs[1], vs[2], nn, c.mat,
                             uvs[0], uvs[1], uvs[2]))
                tris.append((vs[0], vs[2], vs[3], nn, c.mat,
                             uvs[0], uvs[2], uvs[3]))
    return tris


def build_edges(geo, hidden=()):
    """返回立方体 12 条棱（模型空间线段），用于白模线框加深结构。"""
    mats = _bone_matrices(geo)
    pairs = [(0, 1), (0, 2), (0, 4), (1, 3), (1, 5), (2, 3),
             (2, 6), (3, 7), (4, 5), (4, 6), (5, 7), (6, 7)]
    edges = []
    for b in geo.bones:
        if b.name in hidden:
            continue
        bm = mats[b.name]
        for c in b.cubes:
            o = np.array(c.origin, dtype=float)
            s = np.array(c.size, dtype=float)
            if c.rotation or c.pivot:
                cp = np.array(c.pivot, dtype=float) if c.pivot else o + s / 2
                cm = trans(*cp) @ euler_deg(c.rotation) @ trans(*[-v for v in cp])
            else:
                cm = np.eye(4)
            world = bm @ cm
            cs = []
            for x in (0, 1):
                for y in (0, 1):
                    for z in (0, 1):
                        p = o + np.array([x * s[0], y * s[1], z * s[2]])
                        cs.append((world @ np.array([*p, 1.0]))[:3])
            for i, j in pairs:
                edges.append((cs[i], cs[j]))
    return edges


# ---------------------------------------------------------------------------
# 软件光栅器
# ---------------------------------------------------------------------------

#: 默认材质色（G1 白模：浅色模型 + 深色底，靠明暗读结构；占位体灰）
DEFAULT_COLORS = {
    "metal": (247, 250, 255),
    "steel": (255, 255, 255),
    "cloth": (233, 238, 246),
    "jade": (196, 234, 250),
    "rune": (150, 214, 242),
    "body": (126, 134, 148),
    "bg": (48, 54, 66),
}

#: 视角定义：forward（视线方向，从相机指向模型）
VIEWS = {
    "front": np.array([0.0, 0.0, 1.0]),
    "back": np.array([0.0, 0.0, -1.0]),
    "side": np.array([-1.0, 0.0, 0.0]),
    "side_r": np.array([1.0, 0.0, 0.0]),
}


def _view_basis(name, azim=32.0, elev=12.0):
    if name == "hero":
        az = math.radians(azim)
        el = math.radians(elev)
        fwd = np.array([math.sin(az) * math.cos(el),
                        -math.sin(el),
                        math.cos(az) * math.cos(el)])
        fwd = fwd / np.linalg.norm(fwd)
    elif name == "icon":
        fwd, right, up = _view_basis("hero", azim=26.0, elev=10.0)
        # 图标构图：整体再绕视线滚转 38°，让武器/甲呈左下→右上
        ang = math.radians(38.0)
        r2 = right * math.cos(ang) + up * math.sin(ang)
        u2 = -right * math.sin(ang) + up * math.cos(ang)
        return fwd, r2 / np.linalg.norm(r2), u2 / np.linalg.norm(u2)
    else:
        fwd = VIEWS[name].astype(float)
        fwd = fwd / np.linalg.norm(fwd)
    up_hint = np.array([0.0, 1.0, 0.0])
    right = np.cross(fwd, up_hint)
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    return fwd, right, up


def render_views(tris, out_path, views=("front", "side", "back"),
                 size=(520, 700), ss=2, label=None, colors=None,
                 margin=0.90, bg=None, edges=None,
                 line_color=(24, 28, 38), line_eps=0.28,
                 texture=None, glow=None, transparent=False, unlit=False):
    """把三角形列表渲染成多视图 PNG（横向拼）。返回 out_path。

    texture / glow：numpy (H,W,3) 贴图与自发光层；有逐面 UV 时逐像素采样。
    transparent=True 时输出带 alpha 的 RGBA（用于 32×32 图标）。
    """
    colors = dict(DEFAULT_COLORS if colors is None else {**DEFAULT_COLORS, **colors})
    bg = bg or colors["bg"]
    allv = np.array([v for t in tris for v in t[:3]])
    lo, hi = allv.min(axis=0), allv.max(axis=0)
    center = (lo + hi) / 2.0
    W, H = size[0] * ss, size[1] * ss

    imgs = []
    for vname in views:
        fwd, right, up = _view_basis(vname)
        # 三点布光（不随视角移动，保证正/背/侧都能读结构）
        key = np.array([-0.48, 0.78, -0.62])
        key = key / np.linalg.norm(key)
        fill = np.array([0.72, 0.32, 0.62])
        fill = fill / np.linalg.norm(fill)
        rim = np.array([0.15, -0.55, 0.85])
        rim = rim / np.linalg.norm(rim)
        rel = allv - center
        sx = rel @ right
        sy = rel @ up
        span = max(sx.max() - sx.min(), sy.max() - sy.min(), 1e-6)
        scale = (min(W, H) * margin) / span
        cx, cy = W / 2.0, H / 2.0

        img = np.zeros((H, W, 4), dtype=np.uint8)
        if transparent:
            img[:] = (0, 0, 0, 0)
        else:
            img[:, :, :3] = bg
            img[:, :, 3] = 255
        zbuf = np.full((H, W), 1e9, dtype=np.float64)

        for rec in tris:
            v0, v1, v2, n, mat = rec[0], rec[1], rec[2], rec[3], rec[4]
            uvs = rec[5:8] if len(rec) >= 8 else None
            pts = []
            for v in (v0, v1, v2):
                r = v - center
                pts.append((cx + float(r @ right) * scale,
                            cy - float(r @ up) * scale,
                            float(r @ fwd)))
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = pts
            minx = max(0, int(math.floor(min(x0, x1, x2))))
            maxx = min(W - 1, int(math.ceil(max(x0, x1, x2))))
            miny = max(0, int(math.floor(min(y0, y1, y2))))
            maxy = min(H - 1, int(math.ceil(max(y0, y1, y2))))
            if minx > maxx or miny > maxy:
                continue
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            xs = np.arange(minx, maxx + 1, dtype=np.float64)
            ys = np.arange(miny, maxy + 1, dtype=np.float64)
            X, Y = np.meshgrid(xs, ys)
            w0 = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            w1 = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            w2 = 1.0 - w0 - w1
            mask = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not mask.any():
                continue
            Z = w0 * z0 + w1 * z1 + w2 * z2
            sub = zbuf[miny:maxy + 1, minx:maxx + 1]
            hit = mask & (Z < sub)
            if not hit.any():
                continue
            base = np.array(colors.get(mat, colors["metal"]), dtype=np.float64)
            nv = np.array([float(n[0]), float(n[1]), float(n[2])])
            sh = (0.45
                  + 0.42 * max(0.0, float(nv @ key))
                  + 0.18 * max(0.0, float(nv @ fill))
                  + 0.08 * max(0.0, float(nv @ rim)))
            sh = min(sh, 1.02)
            sh *= 0.95 + 0.05 * float(nv[1])
            if unlit:
                sh = 1.0
            sub[hit] = Z[hit]
            region = img[miny:maxy + 1, minx:maxx + 1]
            if texture is not None and uvs is not None and uvs[0] is not None:
                th, tw = texture.shape[:2]
                U = (w0 * uvs[0][0] + w1 * uvs[1][0] + w2 * uvs[2][0])
                V = (w0 * uvs[0][1] + w1 * uvs[1][1] + w2 * uvs[2][1])
                ix = np.clip((U * tw).astype(np.int32), 0, tw - 1)
                iy = np.clip((V * th).astype(np.int32), 0, th - 1)
                rgb = texture[iy, ix].astype(np.float64) * sh
                if glow is not None:
                    rgb = rgb + glow[iy, ix].astype(np.float64) * 0.9
                col = np.clip(rgb, 0, 255).astype(np.uint8)
                region[hit, :3] = col[hit]
                region[hit, 3] = 255
            else:
                col = np.clip(base * sh, 0, 255).astype(np.uint8)
                region[hit, :3] = col
                region[hit, 3] = 255

        # ---- 线框（带深度测试：被挡住的棱不画）----
        if edges:
            for (ea, eb) in edges:
                ra = ea - center
                rb = eb - center
                ax = cx + float(ra @ right) * scale
                ay = cy - float(ra @ up) * scale
                az = float(ra @ fwd)
                bx = cx + float(rb @ right) * scale
                by = cy - float(rb @ up) * scale
                bz = float(rb @ fwd)
                steps = int(max(abs(bx - ax), abs(by - ay))) + 1
                for i in range(steps + 1):
                    t = i / steps
                    ix = int(round(ax + (bx - ax) * t))
                    iy = int(round(ay + (by - ay) * t))
                    if not (0 <= ix < W and 0 <= iy < H):
                        continue
                    zz = az + (bz - az) * t
                    if zz <= zbuf[iy, ix] + line_eps:
                        img[iy, ix, :3] = line_color
                        img[iy, ix, 3] = 255
                        if ix + 1 < W:
                            img[iy, ix + 1, :3] = line_color
                            img[iy, ix + 1, 3] = 255
                        if iy + 1 < H:
                            img[iy + 1, ix, :3] = line_color
                            img[iy + 1, ix, 3] = 255

        im = Image.fromarray(img, "RGBA").resize(size, Image.LANCZOS)
        imgs.append(im)

    total_w = sum(i.width for i in imgs) + 12 * (len(imgs) - 1)
    fill = (0, 0, 0, 0) if transparent else (int(bg[0]), int(bg[1]), int(bg[2]), 255)
    sheet = Image.new("RGBA", (total_w, imgs[0].height), fill)
    x = 0
    for im in imgs:
        sheet.alpha_composite(im, (x, 0))
        x += im.width + 12
    if label:
        draw = ImageDraw.Draw(sheet)
        font = _font(18)
        draw.text((10, 8), label, fill=(226, 233, 244, 255), font=font)
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    if transparent:
        sheet.save(out_path)
    else:
        sheet.convert("RGB").save(out_path)
    return out_path


def _font(px=18, bold=False):
    for p in ("C:/Windows/Fonts/msyhbd.ttc" if bold else "C:/Windows/Fonts/msyh.ttc",
              "C:/Windows/Fonts/msyh.ttc",
              "C:/Windows/Fonts/simhei.ttf"):
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, px)
            except Exception:
                pass
    return ImageFont.load_default()


# ---------------------------------------------------------------------------
# 缩略图 / 拼图
# ---------------------------------------------------------------------------

def thumb_32(tris, out_path, label="", bg=(48, 54, 66)):
    """32×32 识别自检缩略图（4 倍放大显示，旁边放原始 32px）。"""
    tmp = os.path.join(os.path.dirname(out_path), "_tmp32.png")
    render_views(tris, tmp, views=("front",), size=(160, 160), ss=2)
    im = Image.open(tmp).convert("RGB")
    os.remove(tmp)
    small = im.resize((32, 32), Image.LANCZOS)
    big = small.resize((160, 160), Image.NEAREST)
    sheet = Image.new("RGB", (320 + 150, 200), bg)
    sheet.paste(big, (10, 20))
    sheet.paste(small, (190, 100))
    draw = ImageDraw.Draw(sheet)
    draw.text((10, 2), label, fill=(226, 233, 244), font=_font(15))
    sheet.save(out_path)
    return small


def contact_sheet(images, out_path, cols=4, gap=10, bg=(48, 54, 66),
                  title=None, pad=14):
    """把多张等宽图拼成网格。"""
    ims = [Image.open(p) if isinstance(p, str) else p for p in images]
    w = max(i.width for i in ims)
    h = max(i.height for i in ims)
    rows = math.ceil(len(ims) / cols)
    head = 44 if title else 0
    sheet = Image.new("RGB", (pad * 2 + cols * w + (cols - 1) * gap,
                              head + pad * 2 + rows * h + (rows - 1) * gap), bg)
    draw = ImageDraw.Draw(sheet)
    if title:
        draw.text((pad, 12), title, fill=(235, 241, 250), font=_font(22, bold=True))
    for idx, im in enumerate(ims):
        r, c = divmod(idx, cols)
        sheet.paste(im, (pad + c * (w + gap), head + pad + r * (h + gap)))
    sheet.save(out_path)
    return out_path


def player_body_geo():
    """灰模玩家占位体（原版 64×32 模型尺寸，脚底 y=0）。"""
    g = Geo("geometry.player_placeholder")
    body = g.bone("armorBody", (0, 24, 0))
    body.add((-4, 12, -2), (8, 12, 4), mat="body")
    head = g.bone("armorHead", (0, 24, 0))
    head.add((-4, 24, -4), (8, 8, 8), mat="body")
    ra = g.bone("armorRightArm", (-5, 22, 0))
    ra.add((-8, 12, -2), (4, 12, 4), mat="body")
    la = g.bone("armorLeftArm", (5, 22, 0))
    la.add((4, 12, -2), (4, 12, 4), mat="body")
    rl = g.bone("armorRightLeg", (-1.9, 12, 0))
    rl.add((-3.9, 0, -2), (4, 12, 4), mat="body")
    ll = g.bone("armorLeftLeg", (1.9, 12, 0))
    ll.add((-0.1, 0, -2), (4, 12, 4), mat="body")
    g.bone("armorRightBoot", (-1.9, 12, 0))
    g.bone("armorLeftBoot", (1.9, 12, 0))
    return g
