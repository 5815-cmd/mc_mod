package com.hongmeng.xiuxian.domain;

import net.minecraft.ChatFormatting;

/**
 * 道宫品质。
 *
 * <p>道宫是在灵海里凝出的一座宫殿。品质决定两件事：</p>
 * <ul>
 *   <li>领域范围内**对应元素功法**的伤害加成；</li>
 *   <li>释放功法的速度加成。</li>
 * </ul>
 *
 * <pre>
 *   凡品    功法伤害 +100%    释放速度 +10%
 *   地品    功法伤害 +200%    释放速度 +20%
 *   天品    功法伤害 +400%    释放速度 +40%
 *   仙品    功法伤害 +600%    释放速度 +60%
 *   混沌    功法伤害 +800%    释放速度 +80%
 *   鸿蒙    功法伤害 +1000%   释放速度 +100%
 * </pre>
 *
 * <p>凡品到仙品是原设定；混沌是为了和灵根的七档（含混沌）对齐而补的一档，
 * 只有高难度才会出现。</p>
 */
public enum DomainQuality {

    MORTAL("凡品", '7', 0xFFAAAAAA, 1.00D, 0.10D, 0.34D),
    EARTH("地品", 'a', 0xFF7FE07F, 2.00D, 0.20D, 0.28D),
    HEAVEN("天品", 'd', 0xFFE080FF, 4.00D, 0.40D, 0.21D),
    IMMORTAL("仙品", '5', 0xFFFFD24A, 6.00D, 0.60D, 0.13D),
    CHAOS("混沌", 'b', 0xFFC060FF, 8.00D, 0.80D, 0.033D),
    HONGMENG("鸿蒙", '6', 0xFFFF6FE0, 10.00D, 1.00D, 0.007D);

    private final String cn;
    private final char colorCode;
    private final int rgb;
    /** 功法伤害加成（倍率，1.0 = +100%）。 */
    private final double techniqueDamage;
    /** 释放功法速度加成（1.0 = +100%）。 */
    private final double castSpeed;
    /** 随机权重（凝聚时的自然概率）。 */
    private final double weight;

    DomainQuality(String cn, char colorCode, int rgb,
                  double techniqueDamage, double castSpeed, double weight) {
        this.cn = cn;
        this.colorCode = colorCode;
        this.rgb = rgb;
        this.techniqueDamage = techniqueDamage;
        this.castSpeed = castSpeed;
        this.weight = weight;
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

    public double techniqueDamage() {
        return techniqueDamage;
    }

    public double castSpeed() {
        return castSpeed;
    }

    public double weight() {
        return weight;
    }

    public String colored() {
        return "\u00a7" + colorCode + cn;
    }

    public String damageText() {
        return "+" + (int) Math.round(techniqueDamage * 100.0D) + "%";
    }

    public String castText() {
        return "+" + (int) Math.round(castSpeed * 100.0D) + "%";
    }

    public int rank() {
        return ordinal();
    }

    /** 面板里用的颜色码。 */
    public ChatFormatting formatting() {
        return ChatFormatting.getByCode(colorCode) == null
                ? ChatFormatting.GRAY : ChatFormatting.getByCode(colorCode);
    }
}
