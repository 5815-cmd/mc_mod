package com.hongmeng.xiuxian.quality;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <b>品质是怎么来的</b>——获取途径表（纯数据，可离线断言）。
 *
 * <p>玩家问的是"其他品质的丹药没有雷劈……然后其他的东西增加品质什么的东西多一些内容"。
 * 所以品质不是摆设：每一种获取途径都有自己的概率表，而且**表是代码里唯一的一份**，
 * {@link #validate()} 与离线验证器跑的是同一套数字。</p>
 *
 * <h2>五条获取途径</h2>
 * <table border="1">
 *   <caption>基础权重（千分比，每行之和必须是 1000）</caption>
 *   <tr><th>途径</th><th>凡品</th><th>良品</th><th>上品</th><th>极品</th><th>仙品</th><th>神品</th></tr>
 *   <tr><td>合成</td><td>1000</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td></tr>
 *   <tr><td>炼丹</td><td>400</td><td>300</td><td>180</td><td>80</td><td>35</td><td>5</td></tr>
 *   <tr><td>妖兽掉落</td><td>520</td><td>260</td><td>130</td><td>60</td><td>25</td><td>5</td></tr>
 *   <tr><td>副本掉落</td><td>300</td><td>280</td><td>200</td><td>140</td><td>65</td><td>15</td></tr>
 *   <tr><td>任务奖励</td><td>260</td><td>300</td><td>230</td><td>130</td><td>65</td><td>15</td></tr>
 * </table>
 *
 * <h2>"难度偏移"（shift）</h2>
 * <p>难度不是另做一张表，而是一个 <b>0..5 的整数偏移</b>：掷出基础档位后，
 * 每级偏移有 {@link #PROMOTE_PER_MILLE}‰（25%）的概率把结果<b>往上抬一档</b>
 * （最多抬到神品）。这样：</p>
 * <ul>
 *   <li>shift = 0 时分布<b>就是</b>上面的基础表，没有任何额外随机；</li>
 *   <li>偏移每加一级，期望档位严格上升——离线可断言；</li>
 *   <li>表只有一份，加难度不用重抄概率。</li>
 * </ul>
 *
 * <p>难度从哪来：</p>
 * <ul>
 *   <li><b>炼丹</b>：最优灵植的品阶（一阶→偏移 0 …… 六阶→偏移 5，
 *       见 {@link AlchemyQuality#shiftFor(int, int)}），所以"六阶灵植"这一档
 *       确实比五阶更强，而不是被夹到同一级；</li>
 *   <li><b>妖兽掉落</b>：怪物的生命上限档位（见 {@link QualityTable#dropShift(double)}，
 *       四道阈值给出 0..4；副本里的怪血量被 {@code MobScaling} 放大过，
 *       所以"副本掉落"天然偏移更高）；</li>
 *   <li><b>合成</b>：固定凡品，没有偏移。</li>
 * </ul>
 */
public enum QualitySource {

    /** 工作台合成：固定凡品（但**不会**把物品自带的更高品质拉低，见 {@link QualityTable#craftedQuality}）。 */
    CRAFTED("合成", new int[]{1000, 0, 0, 0, 0, 0}),

    /** 炼丹：按灵植品阶偏移。 */
    ALCHEMY("炼丹", new int[]{400, 300, 180, 80, 35, 5}),

    /** 妖兽掉落：按怪物强度偏移。 */
    MOB_DROP("妖兽掉落", new int[]{520, 260, 130, 60, 25, 5}),

    /** 副本掉落（宝箱 / BOSS）：起点就比野怪高。 */
    DUNGEON("副本掉落", new int[]{300, 280, 200, 140, 65, 15}),

    /** 任务奖励：给得最稳（良品/上品为主）。 */
    QUEST("任务奖励", new int[]{260, 300, 230, 130, 65, 15});

    /** 每级难度偏移把结果抬一档的概率（千分比）：250‰ = 25%。 */
    public static final int PROMOTE_PER_MILLE = 250;

    /** 难度偏移的最大级数（0..5：炼丹正好用满六阶灵植，掉落只用得到 0..4）。 */
    public static final int MAX_SHIFT = 5;

    private final String cn;
    private final int[] weights;

    QualitySource(String cn, int[] weights) {
        this.cn = cn;
        this.weights = weights;
    }

    public String cn() {
        return cn;
    }

    /** 基础权重（长度 = {@link Quality#COUNT}，顺序 = 凡品..神品）。 */
    public int[] weights() {
        return weights.clone();
    }

    /** 某一档的基础权重（千分比）。 */
    public int weightOf(Quality q) {
        return weights[q.rank()];
    }

    /** 基础权重之和（必须 1000）。 */
    public int totalPerMille() {
        int sum = 0;
        for (int w : weights) {
            sum += w;
        }
        return sum;
    }

    /** 这个途径是不是"固定档位"（合成就是）。 */
    public boolean isFixed() {
        int nonZero = 0;
        for (int w : weights) {
            if (w > 0) {
                nonZero++;
            }
        }
        return nonZero == 1;
    }

    /** 固定档位途径的结果（非固定途径返回凡品，仅用于展示）。 */
    public Quality fixedQuality() {
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] > 0) {
                return Quality.byRank(i);
            }
        }
        return Quality.MORTAL;
    }

    /**
     * 掷一次品质。
     *
     * @param rng   随机源；为 null 时按"最保守的一档"返回（凡品），不抛异常
     * @param shift 难度偏移 0..4（越界自动夹住）
     */
    public Quality roll(Random rng, int shift) {
        if (rng == null) {
            return Quality.MORTAL;
        }
        Quality base = rollBase(rng);
        int steps = clampShift(shift);
        for (int i = 0; i < steps; i++) {
            if (base.isTop() || rng.nextInt(1000) >= PROMOTE_PER_MILLE) {
                break;
            }
            base = base.upgrade();
        }
        return base;
    }

    /** 只按基础权重掷，不做难度偏移。 */
    public Quality rollBase(Random rng) {
        if (rng == null) {
            return Quality.MORTAL;
        }
        int pick = rng.nextInt(1000);
        int acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (pick < acc) {
                return Quality.byRank(i);
            }
        }
        // 只可能因为表被改坏（和 < 1000）才走到这里
        return Quality.MORTAL;
    }

    /**
     * 掷 {@code samples} 次后的平均档位（界面展示 / 粗略检查用）。
     *
     * <p><b>自检不要用它</b>：抽样有噪声，20 万次也会有 ±0.002 的抖动，
     * 拿它断言"单调上升"会在 shift 4 与 5 这种差别很小的地方随机翻车。
     * 断言请用 {@link #expectedRankExact(int)}（解析解，零噪声）。</p>
     *
     * @return 平均 rank（0 = 全是凡品，5 = 全是神品）
     */
    public double expectedRank(int shift, long seed, int samples) {
        Random rng = new Random(seed);
        long sum = 0;
        for (int i = 0; i < samples; i++) {
            sum += roll(rng, shift).rank();
        }
        return samples <= 0 ? 0.0D : (double) sum / samples;
    }

    /**
     * 期望档位的<b>解析解</b>（不做抽样，因此可以拿来断言严格单调）。
     *
     * <p>推导：设 p = 25%，从基础档位 t 出发、还有 s 级偏移时可期待的提升量是</p>
     * <pre>
     *   e(t, s) = 0                        (t 已是最高档，或 s = 0)
     *   e(t, s) = p × (1 + e(t + 1, s - 1))  其他
     * </pre>
     * <p>再按基础权重加权求和即可。数值上完全确定：p &gt; 0、
     * 且基础表里确实有权重落在最高档以下，所以"shift 每加一级，期望严格上升"是可证的。</p>
     */
    public double expectedRankExact(int shift) {
        int steps = clampShift(shift);
        double total = 0.0D;
        for (int tier = 0; tier < Quality.COUNT; tier++) {
            // 基础档位本身 + 可期待的提升量
            total += weights[tier] / 1000.0D * (tier + expectedPromotions(tier, steps));
        }
        return total;
    }

    private static double expectedPromotions(int tier, int steps) {
        if (steps <= 0 || tier >= Quality.COUNT - 1) {
            return 0.0D;
        }
        double p = PROMOTE_PER_MILLE / 1000.0D;
        return p * (1.0D + expectedPromotions(tier + 1, steps - 1));
    }

    /** 某一档在给定偏移下的概率（‰）——用大样本抽样估出来，供界面与文档展示。 */
    public int sampledPerMille(Quality q, int shift, long seed, int samples) {
        if (samples <= 0) {
            return 0;
        }
        Random rng = new Random(seed);
        int hits = 0;
        for (int i = 0; i < samples; i++) {
            if (roll(rng, shift) == q) {
                hits++;
            }
        }
        return (int) Math.round(hits * 1000.0D / samples);
    }

    /** 夹住难度偏移。 */
    public static int clampShift(int shift) {
        if (shift < 0) {
            return 0;
        }
        return Math.min(shift, MAX_SHIFT);
    }

    /**
     * 自检：**任何一条坏掉就抛异常**。纯数据，离线验证器可以直接调用。
     *
     * @throws IllegalStateException 获取途径表不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();
        for (QualitySource s : values()) {
            if (s.weights.length != Quality.COUNT) {
                problems.add("权重表的长度必须是 " + Quality.COUNT + "：" + s.name());
                continue;
            }
            if (s.totalPerMille() != 1000) {
                problems.add("权重之和必须是 1000‰，实际 " + s.totalPerMille() + "‰：" + s.cn());
            }
            for (int w : s.weights) {
                if (w < 0) {
                    problems.add("权重不能是负数：" + s.cn());
                }
            }
            if (s.cn == null || s.cn.isBlank()) {
                problems.add("有途径没写中文名：" + s.name());
            }
        }
        // 合成的结果必须是固定凡品
        if (!CRAFTED.isFixed() || !CRAFTED.fixedQuality().equals(Quality.MORTAL)) {
            problems.add("合成途径必须固定出凡品");
        }
        // 难度偏移必须真的有用：每一级偏移的期望档位**严格**上升
        // （用解析解，不用抽样——抽样噪声会让 shift 4/5 这种细微差别随机翻车）
        double prev = -1.0D;
        for (int shift = 0; shift <= MAX_SHIFT; shift++) {
            double e = MOB_DROP.expectedRankExact(shift);
            if (e <= prev) {
                problems.add("难度偏移 " + shift + " 的期望档位没有严格高于上一级（"
                        + e + " <= " + prev + "）");
            }
            prev = e;
        }
        if (MOB_DROP.expectedRankExact(0) >= MOB_DROP.expectedRankExact(MAX_SHIFT)) {
            problems.add("难度偏移没有把期望档位抬起来（shift 0 与 " + MAX_SHIFT + " 一样）");
        }
        // shift = 0 的分布必须就是基础表（抽样误差内）
        int divine0 = MOB_DROP.sampledPerMille(Quality.DIVINE, 0, 99L, 200_000);
        int divineBase = MOB_DROP.weightOf(Quality.DIVINE);
        if (Math.abs(divine0 - divineBase) > 8) {
            problems.add("shift=0 时抽样分布偏离基础表太多（神品 " + divine0 + "‰ vs " + divineBase + "‰）");
        }
        // 副本 / 任务起点必须高于野怪
        if (DUNGEON.expectedRankExact(0) <= MOB_DROP.expectedRankExact(0)) {
            problems.add("副本掉落的品质期望必须高于普通妖兽掉落");
        }
        if (QUEST.expectedRankExact(0) <= MOB_DROP.expectedRankExact(0)) {
            problems.add("任务奖励的品质期望必须高于普通妖兽掉落");
        }
        if (DUNGEON.expectedRankExact(MAX_SHIFT) <= MOB_DROP.expectedRankExact(0)) {
            problems.add("最高难度副本的品质期望必须高于普通野怪");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 品质获取途径表不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }
}
