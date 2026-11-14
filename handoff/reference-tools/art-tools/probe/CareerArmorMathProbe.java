import com.hongmeng.xiuxian.core.RealmStats;
import com.hongmeng.xiuxian.item.armor.CareerArmorSet;
import com.hongmeng.xiuxian.item.armor.DaoArmorTier;
import com.hongmeng.xiuxian.item.gear.WeaponForm;

/**
 * R-2 数值离线探针：不启动游戏，只加载纯数据类，逐条核对 06/08 的数值口径。
 *
 * 用法（见 _cache/probe.args）：
 *   javac @probe_compile.args  →  java -cp ... CareerArmorMathProbe
 */
public final class CareerArmorMathProbe {

    private static int bad = 0;
    private static final StringBuilder log = new StringBuilder();

    public static void main(String[] args) {
        checkArmorCurves();
        checkToughness();
        checkWeapons();
        System.out.println(log);
        System.out.println(bad == 0 ? "PROBE: OK (0 mismatch)" : ("PROBE: " + bad + " mismatch"));
        if (bad != 0) {
            System.exit(1);
        }
    }

    private static void checkArmorCurves() {
        double[][] table = {
                {8, 6, 4}, {40, 28, 20}, {136, 95, 68}, {424, 297, 212},
                {1288, 902, 644}, {3900, 2730, 1950}, {11800, 8260, 5900},
                {35500, 24850, 17750}, {107000, 74900, 53500},
                {320000, 224000, 160000}
        };
        for (CareerArmorSet set : CareerArmorSet.values()) {
            for (int tier = 1; tier <= 10; tier++) {
                double total = set.armorPerPieceAt(tier) * 4.0D;
                long expect = Math.round(RealmStats.standard(tier) * set.tier().armorFactor());
                if (Math.abs(total - expect) > 0.51D) {
                    fail("甲 " + set + " 境" + tier + " 全套=" + total + " 期望=" + expect);
                }
            }
        }
        // 入门/进阶/道袍 同一境：4件套总值必须 入门<进阶<道袍
        for (int tier = 1; tier <= 10; tier++) {
            double dao = table[tier - 1][1];
            for (var career : com.hongmeng.xiuxian.talent.Career.values()) {
                var basic = CareerArmorSet.of(career, CareerArmorSet.Tier.BASIC);
                var adv = CareerArmorSet.of(career, CareerArmorSet.Tier.ADVANCED);
                if (basic == null || adv == null) {
                    fail("缺套：" + career);
                    continue;
                }
                double b = basic.totalArmorAt(tier);
                double a = adv.totalArmorAt(tier);
                if (!(b < a && a < dao)) {
                    fail("曲线 " + career + " 境" + tier + " basic=" + b + " adv=" + a + " dao=" + dao);
                }
                long std = RealmStats.standard(tier);
                if (tier <= 10) {
                    log.append(String.format(
                            "%-7s 境%-2d std=%-7d 入门=%-6d 进阶=%-6d 道袍=%-6d%n",
                            career.cn(), tier, std, (long) b, (long) a, (long) dao));
                }
            }
        }
    }

    private static void checkToughness() {
        for (CareerArmorSet set : CareerArmorSet.values()) {
            for (int tier = 1; tier <= 10; tier++) {
                double got = set.toughnessPerPieceAt(tier);
                double dao = DaoArmorTier.of(tier).toughness();
                double expect = dao * set.tier().toughnessRatio();
                if (Math.abs(got - expect) > 1e-9) {
                    fail("韧性 " + set + " 境" + tier + " got=" + got + " expect=" + expect);
                }
                if (got > dao + 1e-9) {
                    fail("韧性超过道袍 " + set + " 境" + tier);
                }
            }
        }
    }

    private static void checkWeapons() {
        for (WeaponForm form : WeaponForm.values()) {
            double dps = form.damageFactor() * form.speedFactor();
            if (Math.abs(dps - 1.0D) > 0.005D) {
                fail("DPS 偏移 " + form.key() + "=" + dps);
            }
            if (!form.twoHanded()) {
                fail("武器不是双手：" + form.key());
            }
        }
        // 攻击顺序：同型 10 阶 = 1 阶 × 40000（standard 比例 320000/8）
        log.append(String.format("%n五型：%n"));
        for (WeaponForm form : WeaponForm.values()) {
            log.append(String.format(
                    "  %-10s 伤害×%.2f 攻速×%.2f 距离%.1f 群伤=%s 动作=%s%n",
                    form.key(), form.damageFactor(), form.speedFactor(), form.reach(),
                    form.sweeping() ? "扇形" : "单体", form.action()));
        }
    }

    private static void fail(String msg) {
        bad++;
        log.append("[BAD] ").append(msg).append('\n');
    }
}
