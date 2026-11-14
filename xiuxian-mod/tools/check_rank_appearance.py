#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
鸿蒙修仙 —— 品阶外观表离线断言
=============================

玩家原话："不同品质的怪物外观也不一样，比如精英和普通怪物的外观差别"。
"外观不一样"如果只靠形容词（"更威猛"、"更唬人"）就没法验收，所以这件事
被拆成两层，两层都能离线断言：

**第一层：数据表（javac + java 真跑一遍）**

``entity/MobRank.java`` 刻意<b>不引用任何 Minecraft 类</b>，所以它可以被单独
编译、单独执行——本脚本现场生成一个探针类，把外观表的每条不变量都断言一遍：

* 普通怪必须是"什么都不加"（体型 1.0 / 发光 0 / 光晕 0 / 护甲 0）；
* 体型、发光、光晕、护甲四列随品阶<b>单调不减</b>；
* 五档显示色互不相同；头目偏紫、首领偏红（玩家明确要求的口径）；
* 只有精英及以上写血量数字；只有头目/首领有 BOSS 条；
* 首领是唯一 3 级发光、唯一最大体型。

**第二层：渲染层确实读了这张表（源码扫描）**

数据再漂亮，渲染代码里写死一个 ``scale(1.3F, ...)`` 就全废了。所以这里再扫一遍
客户端源码，强制要求：

* 每个重写了 ``scale(...)`` 的渲染器里必须出现 ``scaleXZ()``（不许写死数字）；
* 血条渲染器必须用 ``rank.auraColor()`` 与 ``showsHealthNumbers()``；
* BOSS 条必须走 ``ServerBossEvent`` + ``startSeenByPlayer``/``removeAllPlayers``。

用法::

    python tools/check_rank_appearance.py
"""

from __future__ import annotations

import io
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
JAVA = os.path.join(MOD_ROOT, "src", "main", "java")
CLIENT = os.path.join(JAVA, "com", "hongmeng", "xiuxian", "client")
ENTITY = os.path.join(JAVA, "com", "hongmeng", "xiuxian", "entity")

#: 需要的渲染器：每个都必须在 scale() 里读品阶表
SCALE_RENDERERS = [
    "CultivationHumanoidRenderer.java",
    "SpiritWolfRenderer.java",
    "FlameApeRenderer.java",
    "ThunderEagleRenderer.java",
    "StoneGolemRenderer.java",
    "VoidSerpentRenderer.java",
    "BurrowMartenRenderer.java",
    "VenomToadRenderer.java",
]

PROBE = r'''
import com.hongmeng.xiuxian.entity.MobRank;

/** 品阶外观表的离线探针（由 tools/check_rank_appearance.py 现场生成）。 */
public final class RankAppearanceProbe {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        MobRank[] r = MobRank.values();

        check("品阶仍是 5 档", r.length == 5, "实际 " + r.length);

        MobRank normal = MobRank.NORMAL;
        check("普通怪：体型 1.0 / 无发光 / 无光晕 / 无护甲（= 模型本来的样子）",
                normal.scaleXZ() == 1.0F && normal.scaleY() == 1.0F
                        && normal.glowLevel() == 0 && normal.auraLevel() == 0
                        && normal.armorLevel() == 0 && !normal.glowingEyes(),
                "scaleXZ=" + normal.scaleXZ() + " glow=" + normal.glowLevel()
                        + " aura=" + normal.auraLevel() + " armor=" + normal.armorLevel());

        boolean scaleOk = true;
        boolean glowOk = true;
        boolean auraOk = true;
        boolean armorOk = true;
        for (int i = 0; i < r.length; i++) {
            if (r[i].scaleXZ() < 1.0F || r[i].scaleY() < 1.0F
                    || r[i].scaleY() > r[i].scaleXZ()) {
                scaleOk = false;
            }
            if (i > 0) {
                if (r[i].scaleXZ() < r[i - 1].scaleXZ()
                        || r[i].scaleY() < r[i - 1].scaleY()) {
                    scaleOk = false;
                }
                if (r[i].glowLevel() < r[i - 1].glowLevel()) {
                    glowOk = false;
                }
                if (r[i].auraLevel() < r[i - 1].auraLevel()) {
                    auraOk = false;
                }
                if (r[i].armorLevel() < r[i - 1].armorLevel()) {
                    armorOk = false;
                }
            }
        }
        check("体型倍率单调不减，且 1.0 ≤ 纵向 ≤ 横向 ≤ 1.35（不超出命中箱太多）",
                scaleOk && r[4].scaleXZ() <= 1.35F,
                "首领 ×" + r[4].scaleXZ());
        check("发光等级单调不减，普通 = 0", glowOk, "");
        check("光晕等级单调不减，普通 = 0", auraOk, "");
        check("护甲等级单调不减，普通 = 0", armorOk, "");
        check("首领是唯一 3 级发光（整体发光 + 最强光晕）",
                r[4].glowLevel() == 3 && r[4].auraLevel() == 4 && r[4].armorLevel() == 3, "");

        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (MobRank rank : r) {
            colors.add(rank.auraColor());
        }
        check("五档显示色互不相同", colors.size() == r.length,
                colors.size() + " / " + r.length);

        int boss = MobRank.BOSS.auraColor();
        int overlord = MobRank.OVERLORD.auraColor();
        check("头目偏紫（蓝最高、绿最低）",
                (boss & 0xFF) > ((boss >> 16) & 0xFF)
                        && (boss & 0xFF) > ((boss >> 8) & 0xFF),
                String.format("#%06X", boss));
        check("首领偏红（红远大于绿蓝）",
                ((overlord >> 16) & 0xFF) > 0xC0
                        && ((overlord >> 16) & 0xFF) > 2 * ((overlord >> 8) & 0xFF)
                        && ((overlord >> 16) & 0xFF) > 2 * (overlord & 0xFF),
                String.format("#%06X", overlord));

        check("只有精英及以上写血量数字",
                !r[0].showsHealthNumbers() && r[1].showsHealthNumbers()
                        && r[2].showsHealthNumbers() && r[3].showsHealthNumbers()
                        && r[4].showsHealthNumbers(), "");
        check("只有头目与首领有专属 BOSS 条",
                !r[0].hasBossBar() && !r[1].hasBossBar() && !r[2].hasBossBar()
                        && r[3].hasBossBar() && r[4].hasBossBar(), "");

        // ---- 显示色与名牌颜色码的关系 ----
        //
        // 名牌前缀用的是原版颜色码（colorCode），血条/光晕/BOSS 条用显示色（auraColor）。
        // 前四档两者同色系；首领是**唯一**一处刻意不同：
        // 名牌沿用原版金色 §6（不动既有表现），但血条与 BOSS 条按玩家要求用红色。
        // 这里把这条"例外"也断言下来，免得以后有人改着改着两边悄悄跑偏。
        check("普通/精英/队长/头目 的显示色与名牌颜色码同色系",
                sameFamily(r[0]) && sameFamily(r[1]) && sameFamily(r[2])
                        && sameFamily(r[3]), "");
        check("首领是唯一的例外：名牌金色 §6，显示色是红（玩家口径）",
                r[4].colorCode() == '6' && !sameFamily(r[4]), "");
        check("BOSS 条颜色：头目=紫、首领=红（玩家口径）",
                r[3].bossTint() == MobRank.BossTint.PURPLE
                        && r[4].bossTint() == MobRank.BossTint.RED
                        && r[0].bossTint() == MobRank.BossTint.NONE,
                r[3].bossTint() + " / " + r[4].bossTint());

        StringBuilder sb = new StringBuilder();
        for (MobRank rank : r) {
            sb.append(String.format("%s 体型×%.2f 发光%d 光晕%d 护甲%d #%06X | ",
                    rank.cn(), rank.scaleXZ(), rank.glowLevel(), rank.auraLevel(),
                    rank.armorLevel(), rank.auraColor()));
        }
        System.out.println("        外观表：" + sb);

        System.out.println("===== 品阶外观断言：通过 " + passed + " 项，失败 " + failed + " 项 =====");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * 显示色是否与名牌颜色码"同色系"。
     *
     * <p>这里把原版颜色码的 RGB 抄了一份（只用于断言，运行期不参与渲染）：
     * 灰 7 / 绿 a / 青 b / 紫 5。判定口径是"主导通道一致"，
     * 允许亮度不同（血条颜色本来就比名字颜色饱和一些才看得清）。</p>
     */
    private static boolean sameFamily(MobRank rank) {
        int c = rank.auraColor();
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        return switch (rank.colorCode()) {
            case '7' -> Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 0x28;
            case 'a' -> g > r && g > b;
            case 'b' -> g > r && b > r;
            case '5' -> b > g && b >= r;
            default -> false;
        };
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("  [OK ] " + name);
        } else {
            failed++;
            System.out.println("  [BAD] " + name + (detail.isEmpty() ? "" : "  -> " + detail));
        }
    }
}
'''

SOURCE_RULES = [
    # (文件, 必须出现的片段, 说明)
    ("MobHealthBarRenderer.java", ["rank.auraColor()", "showsHealthNumbers()",
                                  "RenderNameTagEvent"],
     "血条必须读品阶表，并且用 RenderNameTagEvent 这一个钩子"),
    ("RankGlowLayer.java", ["RenderType.eyes", "glowingEyes()"],
     "发光眼睛必须走原版 eyes 渲染类型，并由品阶表决定开关"),
    ("../entity/CultivationMonsterEntity.java",
     ["ServerBossEvent", "startSeenByPlayer", "stopSeenByPlayer", "removeAllPlayers",
      "bossBarColor", "bossBarOverlay", "if (this.bossEvent == null)",
      "BOSS_BAR_RANGE = 48.0D", "bar.removePlayer(p)"],
     "BOSS 条必须走原版 ServerBossEvent，一只怪只建一条、按 48 格收放、死亡时收干净"),
    ("../entity/CultivationMonsterEntity.java",
     ["case PURPLE -> BossEvent.BossBarColor.PURPLE",
      "case RED -> BossEvent.BossBarColor.RED",
      "BossEvent.BossBarOverlay.NOTCHED_6",
      "BossEvent.BossBarOverlay.NOTCHED_10"],
     "头目=紫+6 段刻度、首领=红+10 段刻度（玩家口径）"),
]


def read(path: str) -> str:
    return io.open(path, encoding="utf-8").read()


def check_sources() -> tuple[int, int]:
    passed = 0
    failed = 0

    print("== 渲染层确实读了品阶表 ==")
    for name in SCALE_RENDERERS:
        path = os.path.join(CLIENT, name)
        if not os.path.exists(path):
            print(f"  [BAD] 缺少渲染器 {name}")
            failed += 1
            continue
        src = read(path)
        if "protected void scale(" not in src:
            print(f"  [BAD] {name} 没有重写 scale(...)")
            failed += 1
            continue
        if "scaleXZ()" in src:
            print(f"  [OK ] {name} 的 scale() 读了品阶表")
            passed += 1
        else:
            print(f"  [BAD] {name} 的 scale() 没读品阶表（可能写死了数字）")
            failed += 1

    print("== 关键机制的实现方式 ==")
    for name, needles, why in SOURCE_RULES:
        path = os.path.normpath(os.path.join(CLIENT, name))
        if not os.path.exists(path):
            print(f"  [BAD] 缺少 {name}")
            failed += 1
            continue
        src = read(path)
        missing = [n for n in needles if n not in src]
        if missing:
            print(f"  [BAD] {name} 缺少 {missing}（{why}）")
            failed += 1
        else:
            print(f"  [OK ] {why}")
            passed += 1
    return passed, failed


def run_probe() -> tuple[int, int, int]:
    """javac 单独编译 MobRank.java + 探针，java 跑一遍。返回 (通过, 失败, 退出码)。"""
    javac = os.environ.get("JAVAC")
    java = os.environ.get("JAVA")
    if not javac or not java:
        java_home = os.environ.get("JAVA_HOME")
        if not java_home:
            java_home = os.path.join(os.environ.get("APPDATA", ""), ".minecraft",
                                     "runtime", "java-runtime-gamma-snapshot")
        javac = os.path.join(java_home, "bin", "javac.exe")
        java = os.path.join(java_home, "bin", "java.exe")
    if not os.path.exists(javac):
        print(f"!! 找不到 javac：{javac}（设 JAVA_HOME 或 JAVAC/JAVA）")
        return 0, 0, 2

    with tempfile.TemporaryDirectory() as tmp:
        src_dir = os.path.join(tmp, "src", "com", "hongmeng", "xiuxian", "entity")
        os.makedirs(src_dir)
        # MobRank 零 Minecraft 依赖 —— 这是它能被单独编译的原因，也是本断言成立的前提
        src = read(os.path.join(ENTITY, "MobRank.java"))
        if "net.minecraft" in src or "net.minecraftforge" in src:
            print("!! MobRank.java 引用了 Minecraft 类，纯数据表的前提被破坏了")
            return 0, 0, 2
        io.open(os.path.join(src_dir, "MobRank.java"), "w", encoding="utf-8").write(src)
        probe = os.path.join(tmp, "src", "RankAppearanceProbe.java")
        io.open(probe, "w", encoding="utf-8").write(PROBE)

        out = os.path.join(tmp, "out")
        os.makedirs(out)
        env = dict(os.environ)
        env["JAVA_TOOL_OPTIONS"] = "-Duser.language=en -Duser.country=US"
        comp = subprocess.run(
            [javac, "-encoding", "UTF-8", "-nowarn", "-d", out,
             os.path.join(src_dir, "MobRank.java"), probe],
            capture_output=True, text=True, encoding="utf-8", errors="replace", env=env)
        if comp.returncode != 0:
            print("!! 单独编译 MobRank.java 失败：")
            print(comp.stdout)
            print(comp.stderr)
            return 0, 0, 2

        run = subprocess.run([java, "-Dfile.encoding=UTF-8", "-cp", out,
                              "RankAppearanceProbe"],
                             capture_output=True, text=True, encoding="utf-8",
                             errors="replace", env=env)
        print(run.stdout, end="")
        if run.stderr.strip():
            print(run.stderr)
        passed = failed = 0
        for line in run.stdout.splitlines():
            if "通过" in line and "失败" in line:
                body = line.replace("=", " ").split()
                for i, tok in enumerate(body):
                    if tok == "通过" and i + 1 < len(body):
                        passed = int(body[i + 1])
                    if tok == "失败" and i + 1 < len(body):
                        failed = int(body[i + 1])
        return passed, failed, run.returncode


def main() -> int:
    print("========== 一、品阶外观表（离线真跑） ==========")
    passed, failed, code = run_probe()
    if code == 2:
        return 2

    print()
    print("========== 二、渲染层读表断言（源码扫描） ==========")
    p2, f2 = check_sources()
    passed += p2
    failed += f2

    print()
    print(f"========== 品阶外观：通过 {passed} 项，失败 {failed} 项 ==========")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
