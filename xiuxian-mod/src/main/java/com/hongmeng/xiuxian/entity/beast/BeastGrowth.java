package com.hongmeng.xiuxian.entity.beast;

/**
 * 灵兽的成长阶段：<b>幼年 → 成年 → 化形</b>。
 *
 * <p>契约成立时灵兽一定是<b>幼年</b>（见规格第四节），要靠喂<b>妖丹</b>成长。
 * 这样"妖丹"这个原本只能卖钱的掉落物就有了真正的用途——这也是规格
 * 第二节里明确写下的动机。</p>
 *
 * <p>每一段给的是<b>乘算属性倍率</b>，不是加算：化形要一眼看出比幼年强一大截，
 * 而不是"多两点血"。倍率同时作用在生命上限、攻击、体型缩放下。</p>
 */
public enum BeastGrowth {

    /** 幼年：刚契约下来，只有成年的一小半实力。 */
    JUVENILE("幼年", '7', 0.45D, 0.70F, 2),

    /** 成年：标准形态，契约后的主力阶段。 */
    ADULT("成年", 'b', 1.00D, 1.00F, 5),

    /** 化形：吞够妖丹之后的终极形态，体型更大、属性更高。 */
    TRANSFORMED("化形", '6', 1.60D, 1.15F, 12);

    private final String cn;
    private final char colorCode;
    /** 生命／攻击倍率。 */
    private final double statMultiplier;
    /** 体型缩放（渲染 + 命中箱一起缩，否则会出现"看着小打不着"）。 */
    private final float scale;
    /** 从这一阶升到下一阶需要的妖丹数量（最后一阶为 0）。 */
    private final int feedCost;

    BeastGrowth(String cn, char colorCode, double statMultiplier, float scale, int feedCost) {
        this.cn = cn;
        this.colorCode = colorCode;
        this.statMultiplier = statMultiplier;
        this.scale = scale;
        this.feedCost = feedCost;
    }

    public String cn() {
        return cn;
    }

    public char colorCode() {
        return colorCode;
    }

    public String colored() {
        return "\u00a7" + colorCode + cn;
    }

    /** 生命／攻击倍率。 */
    public double statMultiplier() {
        return statMultiplier;
    }

    /** 体型缩放。 */
    public float scale() {
        return scale;
    }

    /** 升到下一阶要几颗妖丹；已经是最后一阶返回 0。 */
    public int feedCost() {
        return feedCost;
    }

    /** 还能不能继续成长。 */
    public boolean canGrow() {
        return this != TRANSFORMED;
    }

    /** 下一阶；已是终阶则返回自身。 */
    public BeastGrowth next() {
        BeastGrowth[] all = values();
        return all[Math.min(all.length - 1, ordinal() + 1)];
    }

    /** 安全解析（存档缺字段时按幼年处理）。 */
    public static BeastGrowth byName(String name) {
        if (name != null) {
            for (BeastGrowth g : values()) {
                if (g.name().equals(name)) {
                    return g;
                }
            }
        }
        return JUVENILE;
    }
}
