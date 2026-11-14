package com.hongmeng.xiuxian.entity.beast;

import com.hongmeng.xiuxian.spirit.Rarity;

/**
 * 契约（收服）的<b>纯数学</b>：成功率、耗符、激怒。
 *
 * <h3>为什么单独一个类</h3>
 *
 * <p>规格第五节第 5 条写死了"离线验证器不能碰注册表"。成功率是这套系统里
 * 唯一值得反复校验的数值（境界差 / 灵根品质 / 血量阈值），所以它必须是
 * <b>不依赖任何注册表对象的纯函数</b>——参数全是 {@code int} / {@code double} /
 * 枚举，验证器可以直接把整张成功率表打出来比对。</p>
 *
 * <h3>设计意图（对齐规格第四节）</h3>
 *
 * <ol>
 *   <li>目标必须是<b>妖兽</b>（{@code CultivationMonsterEntity}）或本模组的野生灵兽；</li>
 *   <li>目标血量必须被打到 <b>25% 以下</b>——"以力服之"，不是喂食；</li>
 *   <li>成功率主要看<b>境界差</b>：同境界 55%，每高一境 +15%，每低一境 −25%；</li>
 *   <li>灵根品质给一点加成（鸿蒙灵根 +12%），让"重抽灵根"在高阶契约上有回报；</li>
 *   <li>灵兽自身的门槛（{@link BeastKind#contractRealmGap()}）直接从成功率里扣——
 *       蛟龙门槛最高，所以金丹修士几乎不可能契约成功；</li>
 *   <li>失败<b>照样消耗契灵符</b>并激怒目标（规格明写）。</li>
 * </ol>
 *
 * <p>概率被夹在 {@value #MIN_CHANCE} ~ {@value #MAX_CHANCE} 之间：
 * 永远给一点希望，也永远不让人靠堆数量白嫖。</p>
 */
public final class BeastContract {

    /** 血量阈值：必须低于这个比例才能上符。规格写的是 25%。 */
    public static final double WEAKEN_THRESHOLD = 0.25D;

    /** 同境界的基础成功率。 */
    public static final double BASE_CHANCE = 0.55D;

    /** 主人每高出一境，加多少。 */
    public static final double PER_TIER_ABOVE = 0.15D;

    /** 主人每低一境，扣多少（比加成狠，避免"越级吊打"）。 */
    public static final double PER_TIER_BELOW = 0.25D;

    /** 成功率下限。 */
    public static final double MIN_CHANCE = 0.05D;

    /** 成功率上限：留 5% 的失败面，破符是仪式的一部分。 */
    public static final double MAX_CHANCE = 0.95D;

    /** 妖兽品阶的每一档额外难度。 */
    public static final double PER_RANK_PENALTY = 0.06D;

    /** 失败后目标被激怒的时长（tick）。 */
    public static final int ENRAGE_TICKS = 20 * 20;

    /** 契约成功后灵兽必定从幼年开始（规格第四节第 4 条）。 */
    public static final BeastGrowth STARTING_GROWTH = BeastGrowth.JUVENILE;

    private BeastContract() {
    }

    // ------------------------------------------------------------------
    // 门槛判定
    // ------------------------------------------------------------------

    /** 血量够不够虚弱。<b>严格小于</b>25%，不是小于等于。 */
    public static boolean isWeakened(float health, float maxHealth) {
        if (maxHealth <= 0.0F) {
            return false;
        }
        return health < maxHealth * WEAKEN_THRESHOLD;
    }

    /**
     * 主人境界够不够格契约这只灵兽。
     *
     * @param ownerTier  主人大境序号（1 = 练气）
     * @param targetTier 契约对象的大境序号
     * @param kind       目标灵兽种类（门槛来自它）
     */
    public static boolean realmAllows(int ownerTier, int targetTier, BeastKind kind) {
        if (kind == null) {
            return false;
        }
        return ownerTier - targetTier >= kind.contractRealmGap();
    }

    /** 门槛还差几境（够了返回 0），用于给玩家的提示文本。 */
    public static int realmShortfall(int ownerTier, int targetTier, BeastKind kind) {
        if (kind == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, kind.contractRealmGap() - (ownerTier - targetTier));
    }

    // ------------------------------------------------------------------
    // 成功率
    // ------------------------------------------------------------------

    /**
     * 成功率。
     *
     * @param ownerTier 主人大境序号
     * @param targetTier 目标大境序号
     * @param targetRank 目标品阶序号（{@code MobRank.ordinal()}；普通 0 … 首领 4）
     * @param rarity    主人的灵根品质，可为 null（按凡品算）
     * @param kind      目标灵兽种类，可为 null（按无门槛算）
     * @return 夹在 [{@value #MIN_CHANCE}, {@value #MAX_CHANCE}] 之间的概率
     */
    public static double successChance(int ownerTier, int targetTier, int targetRank,
                                       Rarity rarity, BeastKind kind) {
        double chance = BASE_CHANCE;

        int gap = ownerTier - targetTier;
        if (gap >= 0) {
            chance += gap * PER_TIER_ABOVE;
        } else {
            chance += gap * PER_TIER_BELOW;   // gap 是负数，等于扣
        }

        // 品阶惩罚：头目比普通难接近 1/4
        chance -= Math.max(0, targetRank) * PER_RANK_PENALTY;

        // 灵根品质：用攻击加成当"灵气浓度"的代理量，最高档给满 12%
        chance += rarityBonus(rarity);

        // 灵兽自身的门槛：每差一境扣 18%
        int shortfall = realmShortfall(ownerTier, targetTier, kind);
        if (shortfall > 0 && shortfall != Integer.MAX_VALUE) {
            chance -= shortfall * 0.18D;
        }

        return clamp(chance);
    }

    /** 灵根品质带来的成功率加成。 */
    public static double rarityBonus(Rarity rarity) {
        if (rarity == null) {
            return 0.0D;
        }
        // attackBonus: 伪灵根 0.10 → 鸿蒙灵根 5.00
        double raw = rarity.attackBonus();
        return Math.min(0.12D, raw * 0.024D);
    }

    /** 把概率夹进合法区间。 */
    public static double clamp(double chance) {
        if (Double.isNaN(chance)) {
            return MIN_CHANCE;
        }
        return Math.max(MIN_CHANCE, Math.min(MAX_CHANCE, chance));
    }

    // ------------------------------------------------------------------
    // 失败后果
    // ------------------------------------------------------------------

    /**
     * 失败时该不该激怒目标。
     *
     * <p>永远激怒——规格写的是"失败消耗符纸并激怒它"，没有例外。
     * 留这个方法是为了让"以后想做灵根豁免"时有唯一的落点。</p>
     */
    public static boolean enragesOnFailure() {
        return true;
    }

    /** 把成功率印成人话，聊天栏与图鉴共用。 */
    public static String describeChance(double chance) {
        return Math.round(chance * 100.0D) + "%";
    }

    /** 成功率对应的颜色代码，让文本一眼分得出难易。 */
    public static char chanceColor(double chance) {
        if (chance >= 0.75D) {
            return 'a';
        }
        if (chance >= 0.45D) {
            return 'e';
        }
        if (chance >= 0.20D) {
            return '6';
        }
        return 'c';
    }
}
