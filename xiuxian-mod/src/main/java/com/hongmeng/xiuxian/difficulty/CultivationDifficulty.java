package com.hongmeng.xiuxian.difficulty;

import com.hongmeng.xiuxian.domain.DomainQuality;
import net.minecraft.ChatFormatting;

import java.util.Random;

/**
 * 开局难度。
 *
 * <p>原文里主角选的是最高难度"鸿蒙模式"——赌一个极品初始号。
 * 这里把那个设定做成开局选择：**难度越高，越容易抽到好灵根与好道宫，
 * 但怪物也越强**。</p>
 *
 * <p>低难度（普通/困难）抽不到顶级灵根，也就享受不到"割草"的爽感；
 * 混沌及以上才会出现混沌灵根与鸿蒙灵根，道印之后开始质变。</p>
 */
public enum CultivationDifficulty {

    /** 普通：安逸。最高只能抽到天灵根，怪物最弱。 */
    NORMAL("普通", ChatFormatting.WHITE, 0xFFFFFF,
            new double[]{0.45D, 0.28D, 0.17D, 0.10D, 0.00D, 0.00D, 0.00D},
            0,
            // 道宫权重顺序：凡品 地品 天品 仙品 混沌 鸿蒙
            new double[]{0.60D, 0.28D, 0.12D, 0.00D, 0.00D, 0.00D},
            0,
            1.00D, 1.00D,
            "安逸路线。灵根上限不高，但怪物也最弱，适合先熟悉玩法。"),

    /** 困难：开始有压力。削弱伪灵根，能抽到先天。 */
    HARD("困难", ChatFormatting.YELLOW, 0xFFFFD24A,
            new double[]{0.25D, 0.30D, 0.22D, 0.15D, 0.08D, 0.00D, 0.00D},
            0,
            new double[]{0.45D, 0.32D, 0.18D, 0.05D, 0.00D, 0.00D},
            0,
            1.30D, 1.25D,
            "伪灵根概率大幅下降，先天灵根开始出现。怪物更强。"),

    /** 极限：凡品起步。 */
    LIMIT("极限", ChatFormatting.GOLD, 0xFFFF9A3A,
            new double[]{0.00D, 0.30D, 0.28D, 0.22D, 0.15D, 0.05D, 0.00D},
            1,
            // 地品保底，仙品概率显著提升
            new double[]{0.24D, 0.38D, 0.28D, 0.10D, 0.00D, 0.00D},
            1,
            1.65D, 1.55D,
            "最低凡品灵根。仙品道宫开始可期。怪物已经不好惹。"),

    /** 混沌：地品起步，混沌灵根概率被放大。 */
    CHAOS("混沌", ChatFormatting.LIGHT_PURPLE, 0xFFC060FF,
            new double[]{0.00D, 0.00D, 0.28D, 0.27D, 0.25D, 0.15D, 0.05D},
            2,
            // 地品保底；混沌/鸿蒙道宫开始出现
            new double[]{0.00D, 0.35D, 0.35D, 0.23D, 0.06D, 0.01D},
            1,
            2.20D, 2.00D,
            "最低地品。混沌灵根的概率被显著放大——从这里开始才能割草。"),

    /** 鸿蒙：最高难度。灵根与道宫的上限全部打开。 */
    HONGMENG("鸿蒙", ChatFormatting.DARK_PURPLE, 0xFF8A4FE8,
            new double[]{0.00D, 0.00D, 0.16D, 0.22D, 0.30D, 0.22D, 0.10D},
            2,
            // 地品保底；混沌/鸿蒙道宫概率最高
            new double[]{0.00D, 0.22D, 0.34D, 0.28D, 0.12D, 0.04D},
            1,
            3.00D, 2.80D,
            "最高难度。怪物最凶，灵根上限最高——只有这里才配得上「碾压一切」。");

    private final String cn;
    private final ChatFormatting color;
    private final int rgb;
    /** 七档灵根的抽取权重（顺序同 Rarity）。 */
    private final double[] rootWeights;
    /** 灵根保底下限（Rarity 序号）。 */
    private final int rootFloor;
    /** 五档道宫的抽取权重（顺序同 DomainQuality）。 */
    private final double[] domainWeights;
    /** 道宫保底下限（DomainQuality 序号）。 */
    private final int domainFloor;
    /** 怪物生命倍率。 */
    private final double mobHealth;
    /** 怪物攻击倍率。 */
    private final double mobDamage;
    private final String desc;

    CultivationDifficulty(String cn, ChatFormatting color, int rgb,
                          double[] rootWeights, int rootFloor,
                          double[] domainWeights, int domainFloor,
                          double mobHealth, double mobDamage, String desc) {
        this.cn = cn;
        this.color = color;
        this.rgb = rgb;
        this.rootWeights = rootWeights;
        this.rootFloor = rootFloor;
        this.domainWeights = domainWeights;
        this.domainFloor = domainFloor;
        this.mobHealth = mobHealth;
        this.mobDamage = mobDamage;
        this.desc = desc;
    }

    public String cn() {
        return cn;
    }

    public ChatFormatting color() {
        return color;
    }

    public int rgb() {
        return rgb;
    }

    public int rootFloor() {
        return rootFloor;
    }

    public int domainFloor() {
        return domainFloor;
    }

    public double mobHealthMultiplier() {
        return mobHealth;
    }

    public double mobDamageMultiplier() {
        return mobDamage;
    }

    public String desc() {
        return desc;
    }

    public String colored() {
        return "\u00a7" + color.getChar() + cn;
    }

    /** 灵根保底的中文名。 */
    public String rootFloorName() {
        return com.hongmeng.xiuxian.spirit.Rarity.values()[rootFloor].cn();
    }

    /** 道宫保底的中文名。 */
    public String domainFloorName() {
        return DomainQuality.values()[domainFloor].cn();
    }

    /** 是否是"割草"级难度（混沌及以上）。 */
    public boolean isMowingTier() {
        return this == CHAOS || this == HONGMENG;
    }

    // ------------------------------------------------------------------
    // 抽取
    // ------------------------------------------------------------------

    /** 按本难度的权重抽一个灵根品质序号（已应用保底下限）。 */
    public int rollRootRarity(Random random) {
        int rolled = rollIndex(rootWeights, random);
        return Math.max(rolled, rootFloor);
    }

    /** 按本难度的权重抽一个道宫品质序号（已应用保底下限）。 */
    public int rollDomainQuality(Random random) {
        int rolled = rollIndex(domainWeights, random);
        return Math.max(rolled, domainFloor);
    }

    private static int rollIndex(double[] weights, Random random) {
        double total = 0.0D;
        for (double w : weights) {
            total += w;
        }
        if (total <= 0.0D) {
            return 0;
        }
        double pick = random.nextDouble() * total;
        double cursor = 0.0D;
        for (int i = 0; i < weights.length; i++) {
            cursor += weights[i];
            if (pick <= cursor) {
                return i;
            }
        }
        return weights.length - 1;
    }

    public static CultivationDifficulty byId(String id) {
        if (id == null || id.isEmpty()) {
            return NORMAL;
        }
        for (CultivationDifficulty d : values()) {
            if (d.name().equalsIgnoreCase(id) || d.cn.equals(id)) {
                return d;
            }
        }
        return NORMAL;
    }

    /** 显示用的权重表（百分比文本），给难度选择界面用。 */
    public String rootWeightText() {
        StringBuilder sb = new StringBuilder();
        com.hongmeng.xiuxian.spirit.Rarity[] rarities =
                com.hongmeng.xiuxian.spirit.Rarity.values();
        double total = 0.0D;
        for (double w : rootWeights) {
            total += w;
        }
        for (int i = 0; i < rootWeights.length; i++) {
            if (rootWeights[i] <= 0.0D) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append(rarities[i].cn()).append(" ")
                    .append(String.format("%.0f%%", rootWeights[i] / total * 100.0D));
        }
        return sb.toString();
    }
}
