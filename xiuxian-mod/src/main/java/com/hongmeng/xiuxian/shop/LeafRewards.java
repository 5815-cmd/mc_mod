package com.hongmeng.xiuxian.shop;

import com.hongmeng.xiuxian.entity.MobRank;

/**
 * 杀怪掉落的灵叶表（按怪物品阶）。
 *
 * <p><b>为什么要有这张表：</b>玩家反馈"灵叶不好获取"——旧版本所有怪一视同仁，
 * 8% 概率掉 1~2 片，杀一只首领和杀一只僵尸收益几乎一样。现在收益跟着
 * {@link MobRank} 走：越难打的怪越"有钱"，玩家才有动力去挑战高阶妖兽。</p>
 *
 * <table border="1">
 *   <caption>掉落区间（先过 {@code economy.leafDropChance} 概率，再在区间内随机）</caption>
 *   <tr><th>怪物品阶</th><th>灵叶</th><th>期望（概率 0.35）</th></tr>
 *   <tr><td>原版敌对生物</td><td>1 ~ 2</td><td>约 0.5 片/只</td></tr>
 *   <tr><td>普通</td><td>2 ~ 4</td><td>约 1.05 片/只</td></tr>
 *   <tr><td>精英</td><td>6 ~ 10</td><td>约 2.8 片/只</td></tr>
 *   <tr><td>队长</td><td>15 ~ 25</td><td>约 7 片/只</td></tr>
 *   <tr><td>头目</td><td>40 ~ 60</td><td>约 17.5 片/只</td></tr>
 *   <tr><td>首领</td><td>100 ~ 150</td><td>约 43.75 片/只</td></tr>
 * </table>
 *
 * <p><b>够买什么（按期望值算：均值 × 0.35 概率 = 每只收益）：</b>
 * 回灵丹 10 片 ≈ 10 只普通妖兽；普通修为丹 20 片 ≈ 19 只普通妖兽
 * （或 38 只原版僵尸，或 5 次采灵草——每次 1~3 片）；
 * 筑基丹 100 片 ≈ 14 只队长；破境丹 250 片 ≈ 14 只头目；
 * 双道宫丹 2000 片 ≈ 46 只首领（或 114 只头目）。</p>
 *
 * <p><b>这个类刻意不引用任何 Minecraft 类</b>（只引用同样是纯数据的 {@link MobRank}），
 * 所以离线验证器可以直接断言这张表，不用启动游戏。</p>
 */
public final class LeafRewards {

    /** 原版敌对生物（非修仙界妖兽）的掉落下限。 */
    public static final int VANILLA_MIN = 1;

    /** 原版敌对生物（非修仙界妖兽）的掉落上限。 */
    public static final int VANILLA_MAX = 2;

    /** 五档妖兽的掉落下限，下标 = {@link MobRank#ordinal()}。 */
    private static final int[] MIN = {2, 6, 15, 40, 100};

    /** 五档妖兽的掉落上限，下标 = {@link MobRank#ordinal()}。 */
    private static final int[] MAX = {4, 10, 25, 60, 150};

    private LeafRewards() {
    }

    /** 某个品阶的掉落下限（至少 1）。 */
    public static int min(MobRank rank) {
        return MIN[clamp(rank)];
    }

    /** 某个品阶的掉落上限（至少等于下限）。 */
    public static int max(MobRank rank) {
        return Math.max(min(rank), MAX[clamp(rank)]);
    }

    /** 平均掉落量（写文档与验证器用）。 */
    public static double average(MobRank rank) {
        return (min(rank) + max(rank)) / 2.0D;
    }

    /**
     * 在区间内随机取一个数。
     *
     * <p>用 {@link java.util.Random} 而不是 Minecraft 的 {@code RandomSource}，
     * 这样离线验证器也能跑同一个函数。</p>
     */
    public static int roll(MobRank rank, java.util.Random rng) {
        int lo = min(rank);
        int hi = max(rank);
        return lo + (hi <= lo ? 0 : rng.nextInt(hi - lo + 1));
    }

    private static int clamp(MobRank rank) {
        if (rank == null) {
            return 0;
        }
        int i = rank.ordinal();
        return Math.max(0, Math.min(MIN.length - 1, i));
    }
}
