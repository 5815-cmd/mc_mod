package com.hongmeng.xiuxian.core;

/**
 * <b>境界解锁的额外能力</b>（玩家要求：仙人起法术完全解放，帝境/鸿蒙解锁规则之力）。
 *
 * <h2>玩家原话</h2>
 * <ul>
 *   <li>「法术华丽分级到仙人就可完全解放了，包括法术限制」；</li>
 *   <li>「帝境鸿蒙解锁可以设置一点新东西，比如规则之力」；</li>
 *   <li>「鸿蒙太久都到大后期完结了」—— 所以鸿蒙再额外给一层。</li>
 * </ul>
 *
 * <h2>法术解放的档位</h2>
 * <pre>
 *   飞升(7)：冷却 ×0（完全无冷却）、灵力消耗 ×0.25、特效拉到最高档
 *   帝境(9)：冷却 ×0、灵力消耗 ×0（不再耗蓝）
 *   鸿蒙(10)：同上，且法术伤害 +20%（"万法之始"）
 * </pre>
 *
 * <p><b>玩家要求</b>：「飞升境界就可以解锁所有的法术和特效级别」——
 * 所以门槛从仙人(8)提到了<b>飞升(7)</b>。</p>
 *
 * <h2>规则之力（帝境起）</h2>
 * <pre>
 *   帝境(9)：寂灭 —— 造成伤害 +30%
 *   鸿蒙(10)：再解锁 虚无（受伤 -30%）与 因果（25% 概率伤害翻倍）
 * </pre>
 *
 * <p>所有门槛只写在这一个类里，方便调整与断言。</p>
 */
public final class RealmPowers {

    /** 法术完全解放（无冷却 + 大幅降耗）的最低境界。 */
    public static final int SPELL_FREEDOM_TIER = 7;

    /** 规则之力解锁的最低境界。 */
    public static final int RULE_POWER_TIER = 9;

    /** 全部规则之力解锁的最低境界。 */
    public static final int RULE_POWER_ALL_TIER = 10;

    // ---- 规则之力数值 ----
    /** 寂灭：造成伤害 +30%。 */
    public static final double RULE_ANNIHILATE_BONUS = 0.30D;
    /** 虚无：受到伤害 -30%。 */
    public static final double RULE_VOID_REDUCTION = 0.30D;
    /** 因果：25% 概率伤害翻倍。 */
    public static final double RULE_CAUSAL_CHANCE = 0.25D;

    private RealmPowers() {
    }

    /** 法术是否已"完全解放"（无冷却、降耗）。 */
    public static boolean spellsFreed(int tier) {
        return tier >= SPELL_FREEDOM_TIER;
    }

    /** 冷却倍率（1.0 = 原样，0.0 = 无冷却）。 */
    public static double cooldownMultiplier(int tier) {
        return spellsFreed(tier) ? 0.0D : 1.0D;
    }

    /** 灵力消耗倍率。 */
    public static double manaCostMultiplier(int tier) {
        if (tier >= RULE_POWER_ALL_TIER) {
            return 0.0D;      // 鸿蒙：不再耗蓝
        }
        if (tier >= RULE_POWER_TIER) {
            return 0.0D;      // 帝境：不再耗蓝
        }
        if (spellsFreed(tier)) {
            return 0.25D;     // 仙人：只花四分之一
        }
        return 1.0D;
    }

    /** 鸿蒙的"万法之始"：法术伤害加成。 */
    public static double spellDamageMultiplier(int tier) {
        return tier >= RULE_POWER_ALL_TIER ? 1.20D : 1.0D;
    }

    // ---- 规则之力 ----

    /** 寂灭（帝境起）：造成伤害的倍率。 */
    public static double annihilateMultiplier(int tier) {
        return tier >= RULE_POWER_TIER ? 1.0D + RULE_ANNIHILATE_BONUS : 1.0D;
    }

    /** 虚无（鸿蒙）：受到伤害的倍率。 */
    public static double voidMultiplier(int tier) {
        return tier >= RULE_POWER_ALL_TIER ? 1.0D - RULE_VOID_REDUCTION : 1.0D;
    }

    /** 因果（鸿蒙）：伤害翻倍的概率。 */
    public static double causalChance(int tier) {
        return tier >= RULE_POWER_ALL_TIER ? RULE_CAUSAL_CHANCE : 0.0D;
    }

    /** 已解锁的规则之力列表（面板/提示用）。 */
    public static java.util.List<String> unlockedNames(int tier) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (tier >= RULE_POWER_TIER) {
            out.add("寂灭（造成伤害 +30%）");
        }
        if (tier >= RULE_POWER_ALL_TIER) {
            out.add("虚无（受到伤害 -30%）");
            out.add("因果（25% 概率伤害翻倍）");
        }
        return out;
    }
}
