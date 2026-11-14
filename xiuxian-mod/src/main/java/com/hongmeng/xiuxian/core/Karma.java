package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 业力：死亡的代价。
 *
 * <p>每死一次 +1 层，每层让突破失败率 +10%，满 10 层则突破必然失败。
 * 清除途径：附魔金苹果一次清空；解决一次劫掠袭击事件 -1 层。</p>
 */
public final class Karma {

    public static final int HARD_CAP = 100;

    private Karma() {
    }

    public static int get(CultivationData data) {
        return data == null ? 0 : data.getKarma();
    }

    /** 该玩家当前的业力上限（含门派修正：魔渊/神霄更窄）。 */
    public static int maxKarma(CultivationData data) {
        int base = HongmengConfig.SERVER.maxKarma.get();
        if (data == null) {
            return base;
        }
        return Math.max(3, base + com.hongmeng.xiuxian.sect.SectManager.karmaCapModifier(data));
    }

    /** 判断某层业力是否已经让突破变成"必败"。 */
    public static boolean isCapped(CultivationData data) {
        return get(data) >= maxKarma(data);
    }

    public static void add(CultivationData data, int amount) {
        if (data == null || amount == 0) {
            return;
        }
        int max = maxKarma(data);
        data.setKarma(Math.min(max, data.getKarma() + amount));
    }

    public static int reduce(CultivationData data, int amount) {
        if (data == null || amount <= 0) {
            return 0;
        }
        int before = data.getKarma();
        data.setKarma(Math.max(0, before - amount));
        return before - data.getKarma();
    }

    public static int clear(CultivationData data) {
        if (data == null) {
            return 0;
        }
        int before = data.getKarma();
        data.setKarma(0);
        return before;
    }

    /** 当前突破成功率（0~1）。门派会额外给一点（或扣一点）。 */
    public static double successRate(CultivationData data) {
        double base = HongmengConfig.SERVER.baseSuccessRate.get();
        double sectBonus = com.hongmeng.xiuxian.sect.SectManager.breakthroughBonus(data);
        double penalty = get(data) * HongmengConfig.SERVER.karmaPenaltyPerStack.get();
        double floor = HongmengConfig.SERVER.minSuccessRate.get();
        return Math.max(floor, Math.min(1.0D, base + sectBonus - penalty));
    }

    /** 突破失败率的百分数文本，例如 "30%"。 */
    public static String failurePercentText(CultivationData data) {
        int pct = (int) Math.round((1.0D - successRate(data)) * 100.0D);
        return pct + "%";
    }

    /** 玩家死亡时调用。 */
    public static void onDeath(ServerPlayer player, CultivationData data) {
        if (!HongmengConfig.SERVER.karmaEnabled.get() || !HongmengConfig.SERVER.karmaOnDeath.get()) {
            return;
        }
        int max = maxKarma(data);
        if (data.getKarma() >= max) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c业力已满 " + max + " 层。你的突破，已经不可能成功了。"
                            + "去找个村庄，替他们解决劫掠，或者……啃一颗附魔金苹果。"));
            return;
        }
        add(data, 1);
        int now = data.getKarma();
        player.sendSystemMessage(Component.literal(
                "\u00a7c你死了。业力 +1（当前 " + now + "/" + max + " 层）"));
        player.sendSystemMessage(Component.literal(
                "\u00a77当前突破失败率：\u00a7c" + failurePercentText(data)));
        if (now >= max) {
            player.sendSystemMessage(Component.literal(
                    "\u00a74\u00a7l业力满溢——从此境界突破，必败无疑。"));
        } else {
            player.sendSystemMessage(Component.literal(
                    "\u00a78（业力不清，突破难成。附魔金苹果可一次清空，劫掠事件一次消一层。）"));
        }
    }
}
