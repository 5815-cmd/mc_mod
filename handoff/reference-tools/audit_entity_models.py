"""审计所有自写实体模型：找出"建模失误"。

不需要进游戏也能查的几何错误（都是这一类 bug 的典型成因）：
  1. **零件脱离躯干**：`PartPose.offset()` 给的坐标离"上一级零件的包围盒"太远
     （例如头挂在 y=+20，而身体只有 8 高 → 空中飘着一颗头）；
  2. **零件过大/过小**：cube 尺寸相对于该生物 `getBbWidth/getBbHeight` 明显失衡
     （例如 1.5 格宽的小怪，头上却挂着 12×12×12 的方块）；
  3. **同一零件重复定义**（后一个覆盖前一个 → 观感与代码意图不符）；
  4. **贴图 UV 越界**（texOffs 超出贴图尺寸 → 糊色/错位）。
"""
import io
import os
import re
import sys

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"
MODELS = os.path.join(WS, r"xiuxian-mod\src\main\java\com\hongmeng\xiuxian\client")

# 每个模型对应的实体体积（宽, 高）——用来判断"零件是不是大得离谱"
SIZE = {
    "SpiritWolfRenderer": (0.9, 0.85),
    "ContractWolfRenderer": (0.9, 0.85),
    "FlameApeRenderer": (1.4, 2.4),
    "ThunderEagleRenderer": (1.2, 1.4),
    "StoneGolemRenderer": (1.6, 2.8),
    "VoidSerpentRenderer": (1.6, 1.6),
    "BurrowMartenRenderer": (0.9, 0.7),
    "VenomToadRenderer": (1.3, 0.9),
    "SpiritSteedRenderer": (1.4, 1.6),
    "ContractEagleRenderer": (1.2, 1.4),
    "JiaolongRenderer": (1.8, 1.2),
    "CultivationHumanoidModel": (0.6, 1.8),
    "CultivationNpcModel": (0.6, 1.8),
}

# texOffs 用的贴图尺寸（本项目统一 64×64，个别 128×128）
TEX = 64


def read(path):
    return io.open(path, encoding="utf-8").read()


def analyse(name, src):
    issues = []
    w, h = SIZE.get(name, (1.0, 1.8))

    # --- 1) 收集 addOrReplaceChild("x", CubeListBuilder...texOffs(a,b).addBox(dx,dy,dz, sx,sy,sz...) , PartPose.offset(px,py,pz)) ---
    parts = []
    for m in re.finditer(
            r'addOrReplaceChild\(\s*"([^"]+)"(.*?)(PartPose\.(?:offset|rotation|offsetAndRotation)\(([^)]*)\))',
            src, re.S):
        part = m.group(1)
        mid = m.group(2)
        pose_args = [a.strip() for a in m.group(4).split(",") if a.strip()]
        nums = []
        for a in pose_args:
            try:
                nums.append(float(a.rstrip("Ff")))
            except ValueError:
                nums.append(0.0)
        boxes = [(float(a), float(b), float(c), float(d), float(e), float(f))
                 for a, b, c, d, e, f in re.findall(
                     r'addBox\(\s*(-?[\d.]+)F?\s*,\s*(-?[\d.]+)F?\s*,\s*(-?[\d.]+)F?\s*,'
                     r'\s*(-?[\d.]+)F?\s*,\s*(-?[\d.]+)F?\s*,\s*(-?[\d.]+)F?\s*\)', mid)]
        texoffs = [(float(a), float(b)) for a, b in re.findall(
            r'texOffs\(\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\)', mid)]
        parts.append((part, nums, boxes, texoffs))

    if not parts:
        return issues, parts

    # --- 2) 重复定义 ---
    seen = {}
    for p, _n, _b, _t in parts:
        seen[p] = seen.get(p, 0) + 1
    for p, c in seen.items():
        if c > 1:
            issues.append("零件 %s 被定义 %d 次（后者覆盖前者）" % (p, c))

    # --- 3) 尺寸失衡 / 脱离躯干 ---
    for part, pose, boxes, texoffs in parts:
        if part in ("root", "body"):
            continue
        px = pose[0] if len(pose) > 0 else 0.0
        py = pose[1] if len(pose) > 1 else 0.0
        pz = pose[2] if len(pose) > 2 else 0.0
        # 与身体中心的距离（像素；1 格 = 16 像素）
        dist_px = (px * px + py * py + pz * pz) ** 0.5
        # 半径上限：身体高 + 半宽，再放宽 1.5 倍
        limit = (h * 16 + w * 8) * 1.6
        if dist_px > limit:
            issues.append("零件 %s 的 PartPose(%.1f,%.1f,%.1f) 距原点 %.0f 像素 > 上限 %.0f"
                          "（疑似飘在半空）" % (part, px, py, pz, dist_px, limit))
        for (bx, by, bz, sx, sy, sz) in boxes:
            if sx <= 0 or sy <= 0 or sz <= 0:
                issues.append("零件 %s 的 cube 尺寸非正: %.1f x %.1f x %.1f" % (part, sx, sy, sz))
            # 单个 cube 超过生物高度的一半就算过大
            if sy > h * 16 * 0.75:
                issues.append("零件 %s 的 cube 高 %.1f 像素（生物总高 %.1f 像素）→ 头/身过大"
                              % (part, sy, h * 16))
            if max(sx, sz) > w * 16 * 1.6:
                issues.append("零件 %s 的 cube 宽/深 %.1f/%.1f 像素（生物宽 %.1f 像素）→ 过宽"
                              % (part, sx, sz, w * 16))
        for (tu, tv) in texoffs:
            if tu + 16 > TEX or tv + 16 > TEX:
                issues.append("零件 %s 的 texOffs(%.0f,%.0f) 超出 %d×%d 贴图" % (part, tu, tv, TEX, TEX))
    return issues, parts


def main():
    total_issues = 0
    for fname in sorted(os.listdir(MODELS)):
        if not fname.endswith(".java"):
            continue
        name = fname[:-5]
        if name not in SIZE:
            continue
        src = read(os.path.join(MODELS, fname))
        issues, parts = analyse(name, src)
        tag = "OK  " if not issues else "!!  "
        print("%s%-26s 零件 %2d 个，问题 %d" % (tag, name, len(parts), len(issues)))
        for i in issues:
            print("      - " + i)
        total_issues += len(issues)
    print()
    print("问题合计：%d" % total_issues)
    return 0


if __name__ == "__main__":
    sys.exit(main())
