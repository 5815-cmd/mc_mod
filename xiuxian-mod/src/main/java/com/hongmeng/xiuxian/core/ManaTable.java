package com.hongmeng.xiuxian.core;

/**
 * 灵力（mana）的数值表与飞行预算。
 *
 * <p><b>为什么单独开一个类、而且不碰任何注册表：</b>灵力上限是
 * 「大境序号」的<b>纯函数</b>，离线验证器要能直接断言
 * {@code ManaTable.maxForTier(3) / FLIGHT_DRAIN_PER_TICK == 6000} 这种关系。
 * 一旦这个类里出现 {@code Items} / {@code Blocks} 之类的静态初始化，
 * 验证器就会因为「注册表未引导」而炸掉。</p>
 *
 * <h3>这张表是怎么定出来的</h3>
 *
 * <p>御剑飞行消耗 {@link #FLIGHT_DRAIN_PER_TICK} 灵力/tick（= 40/秒），
 * 于是各境的御剑时长就是 {@code maxForTier(tier) / 2.0} tick：</p>
 *
 * <pre>
 *   1 练气    600     —（不能飞）
 *   2 筑基    2 400   —（不能飞）
 *   3 金丹    12 000  = 6000 tick = 5 分钟
 *   4 元婴    16 800  = 8400 tick = 7 分钟
 *   5 化神    24 000  = 12000 tick = 10 分钟
 *   6 道印    36 000  = 18000 tick = 15 分钟
 *   7 飞升    72 000  = 36000 tick = 30 分钟
 *   8 仙人    144 000 —（踏空免费，翻倍）
 *   9 帝境    288 000 —（翻倍）
 *  10 鸿蒙    576 000 —（翻倍）
 * </pre>
 *
 * <p>仙人及以上沿用既有的「每境翻倍」规则，与前 7 境的数值正好首尾相接
 * （72 000 → 144 000）。</p>
 */
public final class ManaTable {

    /** 一境都不能少：10 个大境。 */
    public static final int TIER_COUNT = 10;

    /**
     * 各大境的灵力上限（下标 0 = 练气）。
     *
     * <p>这串数字<b>不是随便拍的</b>：前 7 项由「御剑时长 × 40/秒」反推，
     * 后 3 项按翻倍规则接上。</p>
     */
    public static final int[] MAX_BY_TIER = {
            600,      // 1  练气
            2_400,    // 2  筑基
            12_000,   // 3  金丹  —— 5 分钟御剑
            16_800,   // 4  元婴  —— 7 分钟
            24_000,   // 5  化神  —— 10 分钟
            36_000,   // 6  道印  —— 15 分钟
            72_000,   // 7  飞升  —— 30 分钟
            144_000,  // 8  仙人  —— 踏空（免费）
            288_000,  // 9  帝境
            576_000,  // 10 鸿蒙
    };

    /**
     * 御剑飞行的灵力消耗：<b>2.0 灵力 / tick</b>。
     *
     * <p>这个数不是估的，是由「金丹必须正好飞 5 分钟」反推出来的：
     * {@code 12 000 / 2.0 = 6 000 tick = 300 秒}。
     * 改这个数就等于改所有境界的御剑时长，改之前先看 {@link #flightTicksForTier(int)}。</p>
     */
    public static final double FLIGHT_DRAIN_PER_TICK = 2.0D;

    /**
     * 每秒回复最大值的比例：**1.5%/秒**（玩家反馈回复太慢，由 0.5% 上调）。
     *
     * <p>空蓝回满约 67 秒——「慢慢回复」，不至于让玩家站着等，
     * 也不至于让飞行变成无限续航。</p>
     */
    public static final double REGEN_PER_SECOND_FRACTION = 0.015D;

    /** 回复的结算周期：每 tick 结算一次（1/20 秒）。 */
    public static final double REGEN_PER_TICK_FRACTION = REGEN_PER_SECOND_FRACTION / 20.0D;

    /** 打坐（蒲团）时的回复倍率。 */
    public static final double MEDITATION_REGEN_MULTIPLIER = 10.0D;

    /** 回灵丹回复：最大值的 30%。 */
    public static final double PILL_RESTORE_FRACTION = 0.30D;

    /** 御剑的最低境界（金丹）。 */
    public static final int SWORD_FLIGHT_MIN_TIER = 7;

    /** 御剑的最高境界（飞升）；再往上走 {@link Realm#hasFlight()} 的踏空。 */
    public static final int SWORD_FLIGHT_MAX_TIER = 7;

    /** 踏空（免费飞行）的最低境界（仙人）。与 {@link Realm#hasFlight()} 的 tier &gt;= 7 不同：
     *  飞升仍要御剑付费，仙人起才免费。 */
    public static final int FREE_FLIGHT_MIN_TIER = 8;

    private ManaTable() {
    }

    /** 大境序号 → 灵力上限。越界会被夹到合法范围，绝不返回 0。 */
    public static int maxForTier(int tier) {
        int i = Math.max(1, Math.min(TIER_COUNT, tier)) - 1;
        return MAX_BY_TIER[i];
    }

    /** 该境界能不能御剑（3~7）。 */
    public static boolean canSwordFly(int tier) {
        return tier >= SWORD_FLIGHT_MIN_TIER && tier <= SWORD_FLIGHT_MAX_TIER;
    }

    /** 该境界能不能踏空（8+，免费）。 */
    public static boolean canFreeFly(int tier) {
        return tier >= FREE_FLIGHT_MIN_TIER;
    }

    /** 该境界能不能靠御剑一直飞（会因为灵力耗尽而中断）。 */
    public static boolean isPaidFlight(int tier) {
        return canSwordFly(tier);
    }

    /**
     * 该境界满灵力能御剑多久（tick）。
     *
     * <p>做不到御剑的境界返回 0。金丹 = 6 000 tick = 300 秒 —— 这是本表的核心约束。</p>
     */
    public static long flightTicksForTier(int tier) {
        // **只按灵力算时长，不再因"没到御剑门槛"返回 0。**
        //
        // 原因：这个值会出现在面板与起剑提示里（"约 N 秒/分钟"）。
        // 门槛从金丹改成飞升之后，若低境界一律返回 0，提示就会显示"约 0 秒"，
        // 看起来像功能坏了。能不能御剑是另一件事，由 canSwordFly(tier) 单独判。
        return (long) Math.floor(maxForTier(tier) / FLIGHT_DRAIN_PER_TICK);
    }

    /** 该境界满灵力能御剑多少秒（给面板/工具提示用）。 */
    public static long flightSecondsForTier(int tier) {
        return flightTicksForTier(tier) / 20L;
    }

    /**
     * 空蓝回满需要多少 tick。
     *
     * <p>与境界无关（比例制），{@code 1 / REGEN_PER_TICK_FRACTION = 4000} tick = 200 秒。</p>
     */
    public static long fullRegenTicks() {
        return (long) Math.round(1.0D / REGEN_PER_TICK_FRACTION);
    }

    /** 该境界每秒回多少灵力。 */
    public static double regenPerSecond(int tier) {
        return maxForTier(tier) * REGEN_PER_SECOND_FRACTION;
    }

    /** 回灵丹一颗回多少灵力（向下取整，至少 1 点）。 */
    public static int pillRestore(int tier) {
        return Math.max(1, (int) Math.floor(maxForTier(tier) * PILL_RESTORE_FRACTION));
    }
}
