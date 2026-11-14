package com.hongmeng.xiuxian.core;

/**
 * 玩家触及距离（"手的长度"）的<b>纯数据表</b>。
 *
 * <p>规格 {@code docs/domain-spec.md} 第四节：正常史蒂夫摸得到身前 5 格
 * （原版 {@code BLOCK_REACH} = 4.5，取整 5）。十个大境的增加量如下：</p>
 *
 * <pre>
 *   境界        增加     累计
 *   练气(1)      0       4.5
 *   筑基(2)      0       4.5
 *   金丹(3)     +1       5.5   ← 规格"金丹 +1"
 *   元婴(4)      0       5.5   ← 规格"元婴/化神 不增加"
 *   化神(5)      0       5.5
 *   道印(6)     +1       6.5   ← 规格"道印 +1"
 *   飞升(7)      0       6.5   ← 规格"飞升 不增加"
 *   仙人(8)     +1       7.5   ← 规格"仙人 +1"
 *   帝境(9)     +1       8.5   ← 规格"仙人之后每境 +1"
 *   鸿蒙(10)    +1       9.5
 * </pre>
 *
 * <p>⚠️ <b>规格里的一处自相矛盾（已按表格读数实现，特此标出）：</b>
 * 第四节正文的表格把 +1 写成"本境增加"（所以是累加：道印 +2、仙人 +3），
 * 但同一节结尾又写"即：帝境 +2、鸿蒙 +3（相对仙人）"——按正文表格算，
 * 帝境/鸿蒙的累计值是 +4 / +5。两处对不上。</p>
 *
 * <p>本实现取<b>累加</b>读法，理由是：① 表格里专门标了"不增加"的行，
 * 说明其他行是"增加量"而不是"总量"；② 第七节优先级表把四个数并列写成
 * "金丹 +1 / 道印 +1 / 仙人 +1 / 之后每境 +1"，读起来就是四次各加一。
 * 如果玩家本意是后者（帝境 +2、鸿蒙 +3 为总量），把
 * {@link #BONUS_BY_TIER} 换成 {@code {0,0,1,1,1,1,1,1,2,3}} 即可，仅此一处。</p>
 *
 * <p>本类<b>不引用任何 Minecraft 类型</b>，所以离线验证器可以直接断言这张表
 * （见 {@code LogicVerifier} 的 {@code reachScaling()}）。属性上限的自检在
 * {@link #capReport}，由 {@code HongmengMod} 在 common setup 里打一行日志——
 * 那里才允许碰 {@code ForgeMod.BLOCK_REACH}（{@code RegistryObject.get()}）。</p>
 */
public final class ReachTable {

    /** 原版 {@code ForgeMod.BLOCK_REACH} 的默认值。 */
    public static final double BLOCK_REACH_BASE = 4.5D;

    /** 原版 {@code ForgeMod.ENTITY_REACH} 的默认值。 */
    public static final double ENTITY_REACH_BASE = 3.0D;

    /** 原版这两个属性的 {@code maxValue}（Forge 硬编码 1024）。 */
    public static final double EXPECTED_ATTRIBUTE_MAX = 1024.0D;

    /**
     * 十境各自增加的触及距离（下标 0 = 练气，即 tier - 1）。
     *
     * <p>金丹 +1、道印 +1、仙人 +1，仙人之后每境 +1（累加）。</p>
     */
    public static final double[] BONUS_BY_TIER = {
            0.0D,   // 1 练气
            0.0D,   // 2 筑基
            1.0D,   // 3 金丹
            1.0D,   // 4 元婴（不增加）
            1.0D,   // 5 化神（不增加）
            2.0D,   // 6 道印
            2.0D,   // 7 飞升（不增加）
            3.0D,   // 8 仙人
            4.0D,   // 9 帝境
            5.0D    // 10 鸿蒙
    };

    private ReachTable() {
    }

    /** 某大境（1~10）的触及距离加成（格）。 */
    public static double reachBonus(int tier) {
        int idx = Math.max(1, Math.min(BONUS_BY_TIER.length, tier)) - 1;
        return BONUS_BY_TIER[idx];
    }

    /** 全表最大的加成（用于属性上限自检）。 */
    public static double maxBonus() {
        double max = 0.0D;
        for (double v : BONUS_BY_TIER) {
            max = Math.max(max, v);
        }
        return max;
    }

    /** 某大境的最终方块触及距离（原版 4.5 + 加成）。 */
    public static double blockReach(int tier) {
        return BLOCK_REACH_BASE + reachBonus(tier);
    }

    /** 某大境的最终实体触及距离（原版 3.0 + 加成）。 */
    public static double entityReach(int tier) {
        return ENTITY_REACH_BASE + reachBonus(tier);
    }

    /** 各境加成的一行文本（日志/面板用）。 */
    public static String describe() {
        StringBuilder sb = new StringBuilder();
        String[] names = {"练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"};
        for (int tier = 1; tier <= BONUS_BY_TIER.length; tier++) {
            double bonus = reachBonus(tier);
            sb.append(names[tier - 1]).append('(').append(tier).append(")+")
                    .append(fmt(bonus)).append("  ");
        }
        return sb.toString().trim();
    }

    /**
     * 两个新属性的上限自检报告。
     *
     * <p>"属性上限已解除"这件事只覆盖了 MAX_HEALTH 等五个属性；
     * {@code BLOCK_REACH} / {@code ENTITY_REACH} <b>不在那份名单里</b>，
     * 它们仍然是原版的 {@code RangedAttribute(0, 1024)}。本模组最多加
     * {@link #maxBonus()} 格，离 1024 很远——但这件事必须<b>读出来</b>而不是猜。</p>
     *
     * @param blockReachMax  {@code AttributeCaps.maxOf(ForgeMod.BLOCK_REACH.get())}（非 RangedAttribute 返回 -1）
     * @param entityReachMax 同上，ENTITY_REACH
     */
    public static String capReport(double blockReachMax, double entityReachMax) {
        StringBuilder sb = new StringBuilder();
        sb.append(check(blockReachMax, BLOCK_REACH_BASE, "BLOCK_REACH"));
        sb.append(" | ");
        sb.append(check(entityReachMax, ENTITY_REACH_BASE, "ENTITY_REACH"));
        return sb.toString();
    }

    private static String check(double max, double base, String name) {
        double wanted = base + maxBonus();
        if (max < 0.0D) {
            return name + " 上限读不到（非 RangedAttribute）→ 无法确认，请人工核对";
        }
        boolean ok = wanted <= max;
        return name + " 上限 " + fmt(max) + "，本模组最高 " + fmt(base) + "+"
                + fmt(maxBonus()) + "=" + fmt(wanted) + (ok ? " → 不会被截断" : " → **会被截断！**");
    }

    private static String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.valueOf(value);
    }
}
