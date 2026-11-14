package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.quality.Quality;

import java.util.Random;

/**
 * 丹药品质。
 *
 * <p>三种档位：普通 / 完美 / 仙品。品质决定效果倍率，也影响抽到精品丹药的概率。</p>
 *
 * <pre>
 *   普通   效果 ×1     权重 70%     → 通用品质「凡品」
 *   完美   效果 ×3     权重 25%     → 通用品质「上品」
 *   仙品   效果 ×10    权重 5%      → 通用品质「仙品」
 * </pre>
 *
 * <p><b>倍率不再写在这里</b>：三档已经嵌进 {@link Quality} 的六档阶梯
 * （凡品 ×1 / 良品 ×1.5 / 上品 ×3 / 极品 ×5 / 仙品 ×10 / 神品 ×20），
 * 倍率<b>只有一份</b>。这样"丹药品质"和"法宝品质""花瓣品质"用的是同一套数字，
 * 不会再出现"丹药的仙品 ×10、法宝的仙品 ×2"这种两套口径。</p>
 */
public enum PillQuality {

    COMMON("普通", '7', 0xFFAAAAAA, Quality.MORTAL, 0.70D),
    PERFECT("完美", 'b', 0xFF66CCFF, Quality.SUPERIOR, 0.25D),
    IMMORTAL("仙品", '6', 0xFFFF6FE0, Quality.IMMORTAL, 0.05D);

    private final String cn;
    private final char colorCode;
    private final int rgb;
    private final Quality quality;
    private final double weight;

    PillQuality(String cn, char colorCode, int rgb, Quality quality, double weight) {
        this.cn = cn;
        this.colorCode = colorCode;
        this.rgb = rgb;
        this.quality = quality;
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

    /** 这一档对应的通用品质（凡品 / 上品 / 仙品）。 */
    public Quality quality() {
        return quality;
    }

    /** 效果倍率（取自通用品质阶梯：×1 / ×3 / ×10）。 */
    public double multiplier() {
        return quality.effectMultiplier();
    }

    public double weight() {
        return weight;
    }

    public String colored() {
        return "\u00a7" + colorCode + cn;
    }

    public int rank() {
        return ordinal();
    }

    /** 注册名后缀。 */
    public String key() {
        return name().toLowerCase();
    }

    /**
     * 随机抽一个品质。
     *
     * @param lucky 幸运加成（0.0 = 不额外偏向精品；0.5 = 明显偏向）
     */
    public static PillQuality roll(Random random, double lucky) {
        double w0 = Math.max(0.01D, COMMON.weight);
        double w1 = PERFECT.weight * (1.0D + lucky);
        double w2 = IMMORTAL.weight * (1.0D + lucky * 2.0D);
        double total = w0 + w1 + w2;
        double pick = random.nextDouble() * total;
        if (pick <= w0) {
            return COMMON;
        }
        if (pick <= w0 + w1) {
            return PERFECT;
        }
        return IMMORTAL;
    }

    public static PillQuality byId(String id) {
        if (id == null) {
            return COMMON;
        }
        for (PillQuality q : values()) {
            if (q.key().equalsIgnoreCase(id) || q.cn.equals(id)) {
                return q;
            }
        }
        return COMMON;
    }

    /** 效果倍率的显示文本，例如 "×3"。 */
    public String multiplierText() {
        return quality.multiplierText();
    }
}
