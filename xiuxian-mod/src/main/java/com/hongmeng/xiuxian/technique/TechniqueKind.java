package com.hongmeng.xiuxian.technique;

/**
 * 功法的释放形态。
 *
 * <p>决定 {@link TechniqueCaster} 用哪段代码去实现效果，也是面板上显示的图标字母。</p>
 */
public enum TechniqueKind {

    /** 直线穿透：从施法者向前一条线，穿多个目标。 */
    LINE("线"),

    /** 扇形散射：多道投射物呈扇形飞出。 */
    FAN("扇"),

    /** 单个投射物。 */
    PROJECTILE("弹"),

    /** 以自身为中心的范围爆发。 */
    AOE("域"),

    /** 位移：向前突进或瞬移。 */
    DASH("遁"),

    /** 治疗自身。 */
    HEAL("愈"),

    /** 增益（抗性/速度等）。 */
    BUFF("御"),

    /** 吸血：命中后按比例回血。 */
    LIFESTEAL("噬"),

    /**
     * 被动：装备即生效，**不主动释放、不需要目标、释放时不消耗灵力**。
     *
     * <p><b>必须追加在最后</b>——形态序号在存档/协议里不存，但面板的
     * "八种形态覆盖"断言按 ordinal 建表，往中间插值会打乱已有统计。</p>
     */
    PASSIVE("诀"),

    /**
     * 灭世：从天而降／贯穿大地的大招。
     *
     * <p>玩家要求「法术可以增加一点灭世法术，可以参考轨道炮模组」。
     * 追加在<b>最末尾</b>（与 PASSIVE 一样，往中间插会打乱按 ordinal 建的统计表）。</p>
     */
    ULTIMATE("滅"),

    /** 超位·引力：生成奇点，把敌人吸进去。 */
    GRAVITY("引"),

    /** 超位·斩击：从天顶垂落的巨刃，斩出一条深渊。 */
    SLASH("斬");

    private final String glyph;

    TechniqueKind(String glyph) {
        this.glyph = glyph;
    }

    /** 面板上显示的单字标记。 */
    public String glyph() {
        return glyph;
    }

    /** 是否需要目标才能释放（辅助类不需要）。 */
    public boolean needsTarget() {
        return this == LINE || this == FAN || this == PROJECTILE || this == AOE
                || this == LIFESTEAL || this == ULTIMATE
                || this == GRAVITY || this == SLASH;
    }

    /** 是不是被动（不能主动释放，只占被动槽）。 */
    public boolean isPassive() {
        return this == PASSIVE;
    }

    /** 是不是可以装备到主动槽（1~4 号键）的形态。除了被动，都行。 */
    public boolean isActiveCastable() {
        return this != PASSIVE;
    }

    /**
     * 蓄力特效的<b>烈度</b>（玩家要求：「不是灭世的法术蓄力也要有华丽特效」）。
     *
     * <p>所有形态都保留**完整**的七层蓄力表现（阵盘/符环/三重穹环/高空巨环/
     * 流光围绕/内旋符文/汇聚光柱/满蓄爆闪），只是尺寸按等级缩放 ——
     * 所以没有哪个法术是"寒酸"的，区别只在气魄大小。</p>
     *
     * @return 1.0 = 灭世级；越小越紧凑
     */
    public double chargeGrandeur() {
        return switch (this) {
            case ULTIMATE, SLASH, GRAVITY -> 1.00D;
            case AOE -> 0.62D;
            case LINE, FAN, PROJECTILE -> 0.50D;
            case DASH, HEAL, BUFF, LIFESTEAL -> 0.42D;
            case PASSIVE -> 0.42D;
        };
    }

    /** 形态的中文分类：攻击 / 防御 / 辅助 / 位移 / 控制。 */
    public String archetype() {
        return switch (this) {
            case LINE, FAN, PROJECTILE, AOE -> "攻击";
            case BUFF -> "防御";
            case HEAL -> "辅助";
            case DASH -> "位移";
            case LIFESTEAL -> "控制";
            case ULTIMATE -> "灭世";
            case GRAVITY -> "灭世";
            case SLASH -> "灭世";
            case PASSIVE -> "被动";
        };
    }
}
