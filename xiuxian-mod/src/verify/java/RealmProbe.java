import com.hongmeng.xiuxian.core.Realm;

/** 直接打印 Realm.of 在若干修为点上的判定，排查"境界名与加成分叉"的问题。 */
public class RealmProbe {
    public static void main(String[] args) {
        long[] probes = {
                0L, 50L, 99L, 100L, 200L, 300L, 400L, 600L, 1000L, 1200L,
                1336L, 1500L, 2000L, 3000L, 6000L, 20000L, 60000L,
                120000L, 240000L, 400000L, 700000L, 1200000L
        };
        System.out.println("修为      境界        第几境  阶段  境界生命  境界攻击  挖掘等级");
        System.out.println("------------------------------------------------------------------");
        for (long p : probes) {
            Realm r = Realm.of(p);
            System.out.printf("%-9d %-11s %-6d %-5d %-9.1f %-9.1f %d%n",
                    p, r.cn(), r.tier(), r.stage(), r.healthBonus(), r.damageBonus(),
                    r.harvestLevel());
        }

        // 反查：每一层的起点
        System.out.println();
        System.out.println("逐层起点（累计修为 -> 境界）：");
        long total = 0L;
        int shown = 0;
        for (int i = 0; i < 30 && shown < 30; i++) {
            Realm r = Realm.of(total);
            System.out.printf("  累计 %-10d %-11s (第%d境 第%d阶段)%n",
                    total, r.cn(), r.tier(), r.stage());
            total = r.minTotal() + r.need();
            shown++;
            if (r.tier() == 10) {
                break;
            }
        }

        // 关键断言
        System.out.println();
        Realm a = Realm.of(1336L);
        System.out.println("断言 Realm.of(1336) = " + a.cn()
                + "  → 期望是金丹期，实际第 " + a.tier() + " 境");
        Realm b = Realm.of(240000L);
        System.out.println("断言 Realm.of(240000) = " + b.cn()
                + "  → 期望是飞升期，实际第 " + b.tier() + " 境");
    }
}
