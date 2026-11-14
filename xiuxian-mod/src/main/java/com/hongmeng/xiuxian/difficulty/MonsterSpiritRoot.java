package com.hongmeng.xiuxian.difficulty;

import com.hongmeng.xiuxian.spirit.Rarity;

import java.util.Random;

/**
 * 修仙界怪物的<b>灵根品质</b>（玩家要求：同境界的小怪也分强弱）。
 *
 * <h2>玩家指定的分布</h2>
 * <p>「怪物的强度每个境界的小怪 100 个里面都有<b>六成混沌灵根、三成鸿蒙灵根、
 * 一成先天灵根</b>的同境界怪物」。</p>
 *
 * <h2>为什么倍率要压缩</h2>
 * <p>玩家侧的灵根加成是 混沌 +200% / 鸿蒙 +500%，直接套到怪身上会变成碾压
 * （一只鸿蒙灵根的普通怪比玩家还硬 5 倍）。所以这里对怪物做一层压缩，
 * 并留一个统一系数 {@link #STRENGTH} 供整体收放：</p>
 *
 * <pre>
 *   先天  ×1.45
 *   混沌  ×1.90
 *   鸿蒙  ×2.50
 * </pre>
 *
 * <p>期望倍率 = 1.45×0.1 + 1.90×0.6 + 2.50×0.3 ≈ <b>2.04</b>，
 * 与"同境 5.5:4.5、配装 7:3"的手感对得上。</p>
 */
public final class MonsterSpiritRoot {

    /** 整体强度系数：1.0 = 上表原值。想整体收放只改这一个数。 */
    public static final double STRENGTH = 0.6D;

    /** 出现概率（玩家指定：混沌 60% / 鸿蒙 30% / 先天 10%）。 */
    public static final double CHAOS_CHANCE = 0.60D;
    public static final double HONGMENG_CHANCE = 0.30D;
    // 剩下 10% 是先天

    private MonsterSpiritRoot() {
    }

    /** 按玩家指定的分布掷一个灵根品质。 */
    public static Rarity roll(Random random) {
        double p = random.nextDouble();
        if (p < HONGMENG_CHANCE) {
            return Rarity.HONGMENG;
        }
        if (p < HONGMENG_CHANCE + CHAOS_CHANCE) {
            return Rarity.CHAOS;
        }
        return Rarity.INNATE;
    }

    /**
     * 灵根品质 → 怪物的属性倍率（已含 {@link #STRENGTH} 压缩）。
     *
     * @return 1.0 表示不加成
     */
    public static double multiplierOf(Rarity rarity) {
        if (rarity == null) {
            return 1.0D;
        }
        double raw = switch (rarity) {
            case HONGMENG -> 2.50D;
            case CHAOS -> 1.90D;
            case INNATE -> 1.45D;
            default -> 1.0D;
        };
        // 压缩：把"高出 1.0 的那部分"按 STRENGTH 缩放
        return 1.0D + (raw - 1.0D) * STRENGTH;
    }

    /** 名牌前缀（带灵根颜色），例如「§6鸿蒙§r 」。 */
    public static String labelOf(Rarity rarity) {
        if (rarity == null) {
            return "";
        }
        return "\u00a7" + rarity.colorCode() + rarity.cn() + "\u00a7r ";
    }
}
