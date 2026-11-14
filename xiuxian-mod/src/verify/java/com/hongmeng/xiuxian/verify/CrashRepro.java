package com.hongmeng.xiuxian.verify;

import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.Realm;

/**
 * 复现崩溃现场：模拟"连吃修为丹"的序列，看修为/层号会不会失控。
 *
 * <p>崩溃日志里有两个异常现象：</p>
 * <ol>
 *   <li>同一条「你的修为突破了——现在是 练气中期」在 0.5 秒内出现了 3 次</li>
 *   <li>面板显示「修为 0 / 150，累计 110」——层号说需要 150，但累计只有 110</li>
 * </ol>
 *
 * <p>这个探针把日志里的序列逐步跑一遍并打印每一步的状态，
 * 用来看在哪一步开始不自洽。</p>
 */
public final class CrashRepro {

    private CrashRepro() {
    }

    public static void main(String[] args) {
        System.out.println("=== 复现：连吃修为丹 ===");
        System.out.println();

        CultivationData data = new CultivationData();
        data.setAwakened(true);
        data.setCultivation(0L);
        data.setUnlockedStep(0);

        dump("初始", data);

        // 造化丹：日志显示它在破境时给了修为
        System.out.println("--- 造化丹 ---");
        grant(data, 60L);          // 练气初期 need = 60
        dump("造化丹后", data);

        System.out.println("--- 第一次突破（模拟 attemptMinor 成功）---");
        advance(data);
        dump("突破 1", data);

        System.out.println("--- 第二次突破 ---");
        advance(data);
        dump("突破 2", data);

        System.out.println("--- 第三次突破 ---");
        advance(data);
        dump("突破 3", data);

        System.out.println("--- 仙品修为丹 ×2（每颗 375）---");
        grant(data, 375L);
        dump("丹 1 后", data);
        grant(data, 375L);
        dump("丹 2 后", data);

        System.out.println();
        System.out.println("=== 关键自洽性检查 ===");
        check(data);

        System.out.println();
        System.out.println("=== 极端情况：修为远大于 99 层上限 ===");
        CultivationData big = new CultivationData();
        big.setAwakened(true);
        big.setCultivation(Long.MAX_VALUE / 4);
        big.setUnlockedStep(0);
        System.out.println("  修为 = " + big.getCultivation()
                + "  层号 = " + big.unlockedStep()
                + "  境界 = " + big.realm().cn()
                + "  本层上限 = " + big.cultivationCap()
                + "  进度 = " + big.progress() + "/" + big.needed());
        big.clampCultivationToStep();
        System.out.println("  夹取后：修为 = " + big.getCultivation()
                + "  进度 = " + big.progress() + "/" + big.needed());

        System.out.println();
        System.out.println("=== 极端情况：连续 advanceStep 到满级 ===");
        CultivationData steps = new CultivationData();
        steps.setAwakened(true);
        steps.setUnlockedStep(0);
        int n = 0;
        while (steps.advanceStep()) {
            n++;
            if (n > 100) {
                System.out.println("  !! advanceStep 超过 100 次仍未停止，有死循环风险");
                break;
            }
        }
        System.out.println("  推进了 " + n + " 次到 " + steps.realm().cn()
                + "（TOTAL_STEPS = " + Realm.TOTAL_STEPS + "）");
    }

    private static void grant(CultivationData data, long amount) {
        long cap = data.cultivationCap();
        long room = cap - data.getCultivation();
        long gain = Math.min(amount, Math.max(0L, room));
        data.addCultivation(gain);
        System.out.println("    加修为 " + amount + " → 实际 " + gain
                + "（上限 " + cap + "，之前 " + (cap - room) + "）");
    }

    /** 复刻 Breakthrough.applyAdvance 的逻辑。 */
    private static void advance(CultivationData data) {
        long overflow = Math.max(0L, data.getCultivation() - data.realm().need());
        boolean moved = data.advanceStep();
        long cap2 = data.cultivationCap();
        long newValue = Math.min(overflow, cap2);
        data.setCultivation(newValue);
        System.out.println("    overflow=" + overflow + " advanceStep=" + moved
                + " 新层上限=" + cap2 + " 写入修为=" + newValue);
    }

    private static void dump(String tag, CultivationData data) {
        System.out.println(String.format(
                "  %-12s 层号=%2d  境界=%-6s  修为=%8d  本层 %8d/%8d  累计=%d",
                tag, data.unlockedStep(), data.realm().cn(),
                data.getCultivation(), data.progress(), data.needed(),
                data.getTotalCultivation()));
    }

    private static void check(CultivationData data) {
        boolean ok1 = data.progress() >= 0 && data.progress() <= data.needed();
        System.out.println("  [1] 进度落在 [0, need] 内: " + ok1
                + "  (" + data.progress() + " / " + data.needed() + ")");
        boolean ok2 = data.getCultivation() <= data.cultivationCap();
        System.out.println("  [2] 修为不超过本层上限: " + ok2
                + "  (" + data.getCultivation() + " <= " + data.cultivationCap() + ")");
        boolean ok3 = data.getCultivation() >= data.realm().minTotal();
        System.out.println("  [3] 修为不低于本层起点: " + ok3
                + "  (" + data.getCultivation() + " >= " + data.realm().minTotal() + ")");
        boolean ok4 = data.getTotalCultivation() >= data.getCultivation();
        System.out.println("  [4] 累计不低于当前: " + ok4);
    }
}
