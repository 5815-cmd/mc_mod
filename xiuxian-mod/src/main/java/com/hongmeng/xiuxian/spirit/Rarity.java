package com.hongmeng.xiuxian.spirit;

/**
 * 灵根品质：七档，从最弱到最强。
 *
 * <p>品质给出的生命与攻击加成是"实力加成"，<b>独立于境界成长</b>——
 * 境界突破只在境界自身的基础上翻倍，不会把品质加成也一起翻。</p>
 *
 * <pre>
 *   伪灵根      攻击 +10%   生命 +10%
 *   凡品灵根    攻击 +30%   生命 +30%
 *   地灵根      攻击 +50%   生命 +50%
 *   天灵根      攻击 +70%   生命 +70%
 *   先天灵根    攻击 +100%  生命 +100%
 *   混沌灵根    攻击 +200%  生命 +200%
 *   鸿蒙灵根    攻击 +500%  生命 +500%
 * </pre>
 *
 * <p>抽取权重是设计补的（原文只写了品质强弱，没写概率）：越强越罕见。</p>
 */
public enum Rarity {

    FALSE_ROOT("伪灵根", '7', 0xAAAAAA, 0.10D, 0.30D, 1.00D),
    MORTAL("凡品灵根", 'f', 0xFFF0E0A0, 0.30D, 0.25D, 1.50D),
    EARTH("地灵根", 'a', 0xFF7FE07F, 0.40D, 0.18D, 2.00D),
    HEAVEN("天灵根", 'd', 0xFFE080FF, 0.50D, 0.12D, 3.00D),
    INNATE("先天灵根", 'b', 0xFF8CD8FF, 0.60D, 0.08D, 4.00D),
    CHAOS("混沌灵根", '5', 0xFFFFD24A, 0.80D, 0.05D, 5.00D),
    HONGMENG("鸿蒙灵根", '6', 0xFFFF6FE0, 1.00D, 0.02D, 6.00D);

    private final String cn;
    private final char colorCode;
    private final int rgb;
    /** 攻击加成（倍率，1.0 = +100%）。 */
    private final double attackBonus;
    /** 抽取权重（相对值）。 */
    private final double weight;
    /** 修炼速度倍率。 */
    private final double cultivationMultiplier;

    Rarity(String cn, char colorCode, int rgb,
           double attackBonus, double weight, double cultivationMultiplier) {
        this.cn = cn;
        this.colorCode = colorCode;
        this.rgb = rgb;
        this.attackBonus = attackBonus;
        this.weight = weight;
        this.cultivationMultiplier = cultivationMultiplier;
    }

    public String cn() {
        return cn;
    }

    public char colorCode() {
        return colorCode;
    }

    public int rgb() {
        return rgb;
    }

    /** 攻击加成（倍率，1.0 表示 +100%）。 */
    public double attackBonus() {
        return attackBonus;
    }

    /** 生命加成（与攻击同数值）。 */
    public double healthBonus() {
        return attackBonus;
    }

    /** 攻击加成的百分比文本，例如 "+500%"。 */
    public String attackBonusText() {
        return "+" + (int) Math.round(attackBonus * 100.0D) + "%";
    }

    /** 抽取权重（相对值）。 */
    public double weight() {
        return weight;
    }

    /** 修炼速度倍率。 */
    public double cultivationMultiplier() {
        return cultivationMultiplier;
    }

    public String colored() {
        return "\u00a7" + colorCode + cn;
    }

    /** 序号：0 = 伪灵根（最弱）… 6 = 鸿蒙灵根（最强）。 */
    public int rank() {
        return ordinal();
    }
}
