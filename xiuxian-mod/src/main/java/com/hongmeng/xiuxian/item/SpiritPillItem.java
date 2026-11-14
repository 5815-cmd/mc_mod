package com.hongmeng.xiuxian.item;

/**
 * <b>灵石系修为丹</b>（聚灵丹 / 凝元丹）——沿用 {@link ExperiencePillItem} 的
 * “按当前小境界百分比给修为”机制，只换一个丹药名用于服丹日志与 tooltip。
 *
 * <p>为什么要单独一个类而不是直接 new {@link ExperiencePillItem}：
 * 基类的服丹消息写死了「修为丹」三个字；聚灵丹/凝元丹如果也显示「服下 完美修为丹」
 * 会让玩家以为拿到的是别的丹。这里只覆盖名字，数值/品质/封顶规则全部继承。</p>
 */
public class SpiritPillItem extends ExperiencePillItem {

    private final String label;

    /**
     * @param quality   自带品质档（合成出来的丹按凡品算，与既有工作台丹药一致）
     * @param baseRatio 给“当前小境界需求”的百分比（0.15 = 15%）
     * @param label     丹药中文名（用于服丹日志 / tooltip）
     */
    public SpiritPillItem(PillQuality quality, double baseRatio, String label,
                          Properties properties) {
        super(quality, baseRatio, properties);
        this.label = label;
    }

    @Override
    protected String pillLabel() {
        return this.label;
    }
}
