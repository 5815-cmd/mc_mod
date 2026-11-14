package com.hongmeng.xiuxian.alchemy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 炼丹雷劫：<b>炼出最好的丹药时，天雷劈丹</b>。
 *
 * <p>玩家给的精确数值（原话整理见 {@code docs/modpack-roadmap.md} 第三节）：</p>
 *
 * <table border="1">
 *   <caption>雷劫结果表（只有"最好的丹药"会挨雷）</caption>
 *   <tr><th>概率</th><th>结果</th></tr>
 *   <tr><td><b>5%</b></td><td><b>失败</b>——丹药被雷劈碎，一颗不留</td></tr>
 *   <tr><td><b>15%</b></td><td>一颗变 <b>5 个</b></td></tr>
 *   <tr><td><b>30%</b></td><td>变 <b>3 个</b></td></tr>
 *   <tr><td><b>50%</b></td><td>就给 <b>1 个</b></td></tr>
 * </table>
 *
 * <p>其他（非最好）的丹药<b>没有雷</b>，但"变五个"的概率是 <b>20%</b>。</p>
 *
 * <h2>"最好的丹药"怎么定</h2>
 * <p>{@link AlchemyRecipes.Brew} 有一个 {@code tier} 字段（1..6）。
 * 这里把 <b>{@code tier} 等于全表最大品阶的丹方</b> 定义为"最好的丹药"
 * ——也就是说，<b>规则是从表里推出来的，不是写死"tier == 6"</b>：
 * 以后加了七阶灵植与七阶丹方，雷劫会自动跟着上移到七阶，
 * 不会出现"最高阶丹药反而没雷"的哑火。</p>
 *
 * <p>当前实际数据（{@link AlchemyRecipes#BREWS}）：最高品阶 = <b>6</b>，
 * 对应<b>两条</b>血珞芝丹方——「筑基丹（血道方）」与「仙品修为丹（血道方）」。
 * 两条都算最好的丹药，都会挨雷。所以"最好的丹药"是<b>一个品阶（第六阶）</b>，
 * 而不是某一颗具体的丹药；玩家说的"最好的丹药"在数据上就是这一档。</p>
 *
 * <h2>为什么这个类是纯数据</h2>
 * <p>和 {@link AlchemyRecipes} 一样：没有 {@code ItemStack}、没有 {@code Level}、
 * 没有 {@code EntityType}——概率表与期望产出可以离线断言
 * （四档概率之和必须正好 100%、非雷劫的"五个"概率必须是 20%、
 * 期望产出必须落在 1 与最大倍数之间）。真正的闪电与粒子在
 * {@link AlchemyFurnaceBlockEntity} 里放。</p>
 */
public final class AlchemyTribulation {

    /** 雷劫一次产出的结果：倍数 + 发生概率（单位千分之一，避免浮点误差）。 */
    public record Outcome(int pills, int perMille, String cn, String message) {

        /** 概率（0~1）。 */
        public double chance() {
            return perMille / 1000.0D;
        }

        /** 是不是"炸炉"（一颗都不剩）。 */
        public boolean isFailure() {
            return pills <= 0;
        }
    }

    /**
     * 雷劫结果表：<b>四条概率之和必须正好 1000‰（= 100%）</b>。
     *
     * <p>用千分比而不是 double，是为了让"和正好 100%"成为一个可以精确断言的
     * 整数等式（{@code 50+150+300+500 == 1000}），不会被浮点误差烦到。</p>
     */
    public static final List<Outcome> OUTCOMES = List.of(
            new Outcome(1, 500, "一颗",
                    "\u00a7a一炉得丹 \u00a7f×1\u00a7a，丹成。"),
            new Outcome(3, 300, "三颗",
                    "\u00a7a天雷淬丹，一炉得丹 \u00a7f×3\u00a7a！"),
            new Outcome(5, 150, "五颗",
                    "\u00a7a一炉得丹 \u00a7f×5\u00a7a！天雷反而助了药力！"),
            new Outcome(0, 50, "失败",
                    "\u00a7c丹药被天雷劈碎……这一炉废了。")
    );

    /** 雷劫触发的征兆（劈雷前先说一句，玩家知道"要出事了"）。 */
    public static final String OMEN = "\u00a76天雷淬丹！";

    /** 非雷劫丹方"一颗变五个"的概率（千分比）：200‰ = 20%。 */
    public static final int PLAIN_FIVE_PER_MILLE = 200;

    /** 非雷劫丹方的产出倍数。 */
    public static final int PLAIN_NORMAL_PILLS = 1;
    public static final int PLAIN_LUCKY_PILLS = 5;

    /** 一粒丹最多被雷劈成几个（界面/产物槽上限判断用）。 */
    public static final int MAX_PILLS = 5;

    /** 雷劫动画总时长（tick）：丹药飞上天 → 挨雷 → 落回。 */
    public static final int ANIM_TICKS = 40;

    /** 丹药"飞上天"的顶点高度（格）。 */
    public static final double SKY_HEIGHT = 9.0D;

    private AlchemyTribulation() {
    }

    // ------------------------------------------------------------------
    // 纯数据：规则判定
    // ------------------------------------------------------------------

    /**
     * 全表最高品阶（"最好的丹药"的门槛）。
     *
     * <p>空表返回 0。</p>
     */
    public static int bestTier() {
        int best = 0;
        for (AlchemyRecipes.Brew b : AlchemyRecipes.BREWS) {
            best = Math.max(best, b.tier());
        }
        return best;
    }

    /** 这个丹方是不是"最好的丹药"（会挨雷的那一档）。 */
    public static boolean isTribulation(AlchemyRecipes.Brew brew) {
        return brew != null && brew.tier() >= bestTier() && bestTier() > 0;
    }

    /** 所有会挨雷的丹方（离线断言与文档用）。 */
    public static List<AlchemyRecipes.Brew> tribulationBrews() {
        List<AlchemyRecipes.Brew> out = new ArrayList<>();
        for (AlchemyRecipes.Brew b : AlchemyRecipes.BREWS) {
            if (isTribulation(b)) {
                out.add(b);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** 非雷劫丹方"一颗变五个"的概率（0~1）：0.20。 */
    public static double plainFiveChance() {
        return PLAIN_FIVE_PER_MILLE / 1000.0D;
    }

    // ------------------------------------------------------------------
    // 纯数据：概率与期望
    // ------------------------------------------------------------------

    /** 四档概率之和（‰）。必须是 1000。 */
    public static int totalPerMille() {
        int sum = 0;
        for (Outcome o : OUTCOMES) {
            sum += o.perMille;
        }
        return sum;
    }

    /** 四档概率之和是否为 100%。 */
    public static boolean sumsToHundredPercent() {
        return totalPerMille() == 1000;
    }

    /**
     * 雷劫的<b>期望产出</b>（每投入一炉主料+辅料）。
     *
     * <p>当前表：1×0.50 + 3×0.30 + 5×0.15 + 0×0.05 = <b>2.15 颗</b>。
     * 也就是说"最好的丹药"平均一炉出 2.15 颗，比丹方写死的 1 颗多一倍有余——
     * 这正是玩家要的"挨雷有额外收益"。</p>
     */
    public static double expectedPills() {
        double sum = 0.0D;
        for (Outcome o : OUTCOMES) {
            sum += o.pills * o.chance();
        }
        return sum;
    }

    /** 非雷劫丹方的期望产出：1×(1-20%) + 5×20% = 1.80 颗。 */
    public static double expectedPlainPills() {
        return PLAIN_NORMAL_PILLS * (1.0D - plainFiveChance())
                + PLAIN_LUCKY_PILLS * plainFiveChance();
    }

    /**
     * 抽一次结果。
     *
     * <p>用 {@link java.util.Random} 而不是 Minecraft 的 {@code RandomSource}，
     * 这样离线验证器能跑同一个函数（和 {@code LeafRewards.roll} 一个口径）。</p>
     *
     * @param rng 随机源；为 null 时按第一档（最保守）返回
     * @return 命中的结果，永不为 null
     */
    public static Outcome roll(Random rng) {
        if (rng == null) {
            return OUTCOMES.get(0);
        }
        int pick = rng.nextInt(1000);
        int acc = 0;
        for (Outcome o : OUTCOMES) {
            acc += o.perMille;
            if (pick < acc) {
                return o;
            }
        }
        // 只可能因为表被改坏（和 < 1000）才走到这里——兜底给最保守的一档
        return OUTCOMES.get(0);
    }

    /**
     * 非雷劫丹方的产出数量。
     *
     * @param baseCount 丹方本身产几个（{@link AlchemyRecipes.Brew#count()}）
     */
    public static int rollPlainCount(int baseCount, Random rng) {
        int base = Math.max(1, baseCount);
        if (rng != null && rng.nextInt(1000) < PLAIN_FIVE_PER_MILLE) {
            return PLAIN_LUCKY_PILLS;
        }
        return base;
    }

    /** 雷劫结果的中文一句（写日志与文档用）。 */
    public static String describe(Outcome o) {
        return o == null ? "无" : (o.cn() + "（" + (o.perMille / 10.0D) + "%）");
    }

    /**
     * 自检。**任何一条坏掉就抛异常**。
     *
     * <p>在 {@link ModAlchemy#register(net.minecraftforge.eventbus.api.IEventBus)}
     * 里调用（和 {@link AlchemyRecipes#validate()} 并排），
     * 所以概率表写错的表现是"模组加载失败 + 一条说得清原因的异常"，
     * 而不是"雷劫概率悄悄变成 87%"。</p>
     *
     * @throws IllegalStateException 概率表不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();

        if (OUTCOMES.size() != 4) {
            problems.add("雷劫结果必须是 4 档（失败 / 1 / 3 / 5），实际 " + OUTCOMES.size());
        }
        if (totalPerMille() != 1000) {
            problems.add("四档概率之和必须是 1000‰（100%），实际 " + totalPerMille() + "‰");
        }
        if (PLAIN_FIVE_PER_MILLE != 200) {
            problems.add("非雷劫丹方的『五个』概率必须是 200‰（20%），实际 "
                    + PLAIN_FIVE_PER_MILLE + "‰");
        }
        for (Outcome o : OUTCOMES) {
            if (o.perMille() <= 0 || o.perMille() > 1000) {
                problems.add("概率越界：" + o.cn() + " = " + o.perMille() + "‰");
            }
            if (o.pills() < 0 || o.pills() > MAX_PILLS) {
                problems.add("产出数量越界（0.." + MAX_PILLS + "）：" + o.cn()
                        + " = " + o.pills());
            }
            if (o.cn() == null || o.cn().isBlank()) {
                problems.add("有结果档没写中文名");
            }
            if (o.message() == null || o.message().isBlank()) {
                problems.add("有结果档没写提示语：" + o.cn());
            }
        }
        // 失败必须是"0 颗"，且只能有一档失败
        int failures = 0;
        for (Outcome o : OUTCOMES) {
            if (o.isFailure()) {
                failures++;
            }
        }
        if (failures != 1) {
            problems.add("必须有且只有一档是失败（0 颗），实际 " + failures + " 档");
        }
        // 期望产出必须落在 [1, MAX_PILLS] 里——低于 1 等于"炼最好的丹纯亏"，
        // 高于 5 等于"雷劫必赚"，两种都不合理。
        double expected = expectedPills();
        if (expected < 1.0D || expected > MAX_PILLS) {
            problems.add("雷劫期望产出不合法（应在 [1," + MAX_PILLS + "]）：" + expected);
        }
        // 非雷劫的期望也必须合理：1×(1-20%) + 5×20% = 1.8
        double plainExpected = expectedPlainPills();
        if (plainExpected < 1.0D || plainExpected > MAX_PILLS) {
            problems.add("非雷劫期望产出不合法（应在 [1," + MAX_PILLS + "]）：" + plainExpected);
        }
        // 最好的丹药必须真的存在
        if (bestTier() <= 0 || bestTier() > 6) {
            problems.add("最高丹方品阶不合法（应在 1..6）：" + bestTier());
        }
        if (tribulationBrews().isEmpty()) {
            problems.add("没有任何丹方会被雷劈（最高品阶那一档是空的）");
        }
        // 雷劫的门槛必须是最高的那一档：不能有比它更高的品阶
        for (AlchemyRecipes.Brew b : AlchemyRecipes.BREWS) {
            if (b.tier() > bestTier()) {
                problems.add("有丹方品阶高于『最好』的门槛（雷劫漏了一档）："
                        + b.result() + " tier=" + b.tier());
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 炼丹雷劫表不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }

    /** 概率文案（干分比 → 百分比字符串），界面与日志用。 */
    public static String percentText(Outcome o) {
        if (o == null) {
            return "0%";
        }
        double pct = o.perMille / 10.0D;
        if (pct == Math.floor(pct)) {
            return ((long) pct) + "%";
        }
        return String.format(Locale.ROOT, "%.1f%%", pct);
    }
}
