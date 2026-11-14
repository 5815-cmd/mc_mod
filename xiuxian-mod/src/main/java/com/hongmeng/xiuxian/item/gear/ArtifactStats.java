package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.core.RealmStats;

/**
 * <b>法宝「按境界 × 部位」的纯数值表</b>（10 境 × 5 部位 = 50 件）。
 *
 * <h2>2026-11-07 重做背景</h2>
 *
 * <p>玩家反馈：「从最基础的境界到鸿蒙每个境界都有法宝可以穿，然后调整他们的数值」。
 * 审计发现旧版法宝有两个真正的数值问题：</p>
 *
 * <ol>
 *   <li><b>护甲给得太大、而且后期完全浪费</b>：旧版五件合计
 *       {@code 0.805 × 本境标准值}（比"纯防御"的道袍 0.70 还高），
 *       但 4 境之后职业甲自己就顶到 92% 减伤上限，法宝那几十万护甲**一点用都没有**；</li>
 *   <li><b>额外生命用固定值 {@code 0.16 × 标准值}</b>，和玩家生命不是一个刻度：
 *       道印境约等于 +208% 生命（严重超模），鸿蒙境只剩 +4%（形同虚设）。</li>
 * </ol>
 *
 * <p>重做后法宝定位 = <b>通用五件套（护甲 + 生命% + 攻击%）</b>：</p>
 *
 * <pre>
 *   五件合计护甲     = 本境标准值 × 0.276   （低于职业甲 0.50/0.58/0.64，也低于道袍 0.70）
 *   五件合计生命上限 = +6% / +8% / +10%     （按外观档 1~3 / 4~6 / 7~10 境）
 *   五件合计攻击     = +6% / +8% / +10%     （同上）
 *   单件韧性         = 护甲 × 权重 × 0.10   （与原版韧性同口径的"手感数值"）
 * </pre>
 *
 * <p>这样每一境的法宝都有明确收益：低境靠护甲，高境靠生命%/攻击%，
 * 不再出现"中段超模、后期白给"的两头翘。</p>
 *
 * <h2>护甲分配（五件合计 4.60 × per）</h2>
 * <pre>
 *   法冠 0.90    道袍 1.15    法裤 1.05    云履 0.85    玉佩 0.65
 * </pre>
 *
 * <p><b>纯数据类</b>：不引用任何注册表，离线验证器（{@code LogicVerifier}）与
 * {@code tools/check_balance.py} 都可以直接断言。</p>
 */
public final class ArtifactStats {

    /** 法宝境界档数（练气~鸿蒙）。 */
    public static final int MAX_TIER = 10;

    /** 五个部位的注册名 key（与 {@link GearNames#SLOTS} 同序）。 */
    public static final String[] SLOT_KEYS = {"crown", "robe", "pants", "boots", "jade"};

    /** 五个部位的中文名。 */
    public static final String[] SLOT_NAMES = {"法冠", "道袍", "法裤", "云履", "玉佩"};

    /**
     * 单件护甲系数：护甲 = 本境标准值 × 该系数 × 部位权重。
     *
     * <p>0.060 × 4.60 权重 = 全套 0.276 × 标准值。</p>
     */
    public static final double ARMOR_PER_STD = 0.060D;

    /** 五部位护甲权重（合计 4.60）。 */
    private static final double[] ARMOR_WEIGHT = {0.90D, 1.15D, 1.05D, 0.85D, 0.65D};

    /** 五部位韧性权重（相对单件护甲；合计 0.40）。 */
    private static final double[] TOUGHNESS_WEIGHT = {0.10D, 0.10D, 0.10D, 0.05D, 0.05D};

    /**
     * 生命 / 攻击的<b>全套</b>百分比，按外观档：1~3 初阶、4~6 进阶、7~10 名器。
     */
    private static final double[] SET_PERCENT_BY_BAND = {0.06D, 0.08D, 0.10D};

    /** 五部位生命占比（合计 1.0）：道袍/法裤偏生命，法冠/玉佩偏攻击。 */
    private static final double[] HEALTH_WEIGHT = {0.10D, 0.30D, 0.30D, 0.10D, 0.20D};

    /** 五部位攻击占比（合计 1.0）。 */
    private static final double[] ATTACK_WEIGHT = {0.25D, 0.10D, 0.10D, 0.15D, 0.40D};

    private ArtifactStats() {
    }

    /** 第 tier 境（1~10）属于哪个外观档（0 初阶 / 1 进阶 / 2 名器）。 */
    public static int band(int tier) {
        int t = clamp(tier);
        if (t <= 3) {
            return 0;
        }
        if (t <= 6) {
            return 1;
        }
        return 2;
    }

    public static int clamp(int tier) {
        return Math.max(1, Math.min(MAX_TIER, tier));
    }

    /** 第 tier 境单件法宝的"护甲基准"（未乘部位权重）。 */
    public static double armorPer(int tier) {
        return RealmStats.standard(clamp(tier)) * ARMOR_PER_STD;
    }

    /** 第 tier 境、第 slot 件法宝的护甲值。 */
    public static double armor(int tier, int slot) {
        return armorPer(tier) * ARMOR_WEIGHT[clampSlot(slot)];
    }

    /** 第 tier 境、第 slot 件法宝的韧性值。 */
    public static double toughness(int tier, int slot) {
        return armorPer(tier) * TOUGHNESS_WEIGHT[clampSlot(slot)];
    }

    /** 第 tier 境、第 slot 件法宝的生命上限加成（比例，例如 0.03 = +3%）。 */
    public static double healthPercent(int tier, int slot) {
        return SET_PERCENT_BY_BAND[band(tier)] * HEALTH_WEIGHT[clampSlot(slot)];
    }

    /** 第 tier 境、第 slot 件法宝的攻击加成（比例）。 */
    public static double attackPercent(int tier, int slot) {
        return SET_PERCENT_BY_BAND[band(tier)] * ATTACK_WEIGHT[clampSlot(slot)];
    }

    /** 第 tier 境五件法宝合计护甲。 */
    public static double setArmor(int tier) {
        double sum = 0.0D;
        for (int i = 0; i < SLOT_KEYS.length; i++) {
            sum += armor(tier, i);
        }
        return sum;
    }

    /** 第 tier 境五件法宝合计生命上限百分比。 */
    public static double setHealthPercent(int tier) {
        return SET_PERCENT_BY_BAND[band(tier)];
    }

    /** 第 tier 境五件法宝合计攻击百分比。 */
    public static double setAttackPercent(int tier) {
        return SET_PERCENT_BY_BAND[band(tier)];
    }

    /** 第 tier 境法宝的穿戴境界门槛（就是它自己的境界）。 */
    public static int requiredRealm(int tier) {
        return clamp(tier);
    }

    /** 法宝部位数量（5）。 */
    public static int slotCount() {
        return SLOT_KEYS.length;
    }

    private static int clampSlot(int slot) {
        return Math.max(0, Math.min(SLOT_KEYS.length - 1, slot));
    }

    /** 注册名：第 tier 境的某部位法宝（与 {@link GearNames#artifactId} 同构）。 */
    public static String itemName(int tier, int slot) {
        return "artifact_" + clamp(tier) + "_" + SLOT_KEYS[clampSlot(slot)];
    }
}
