package com.hongmeng.xiuxian.entity.beast;

/**
 * <b>灵兽骑乘系统总开关</b>。
 *
 * <p>玩家 2026-11-07 要求：<b>把骑乘系统删了，不再进行提升，在游戏内隐藏，
 * 以后有想法再挑出来做。</b></p>
 *
 * <p>所以这里没有真的删代码，而是加了一个统一的总开关：</p>
 * <ul>
 *   <li>{@link #ENABLED} = {@code false}：不能上马、不能飞、不能二段跳；
 *       骑乘相关的按键、命令、灵力消耗、名册里的载重/灵力栏、图鉴里的
 *       飞行/速度/载重列全部隐藏；</li>
 *   <li>灵兽本体、契约、灵兽袋、成长、灵兽护甲、契约灵狼仍保留；</li>
 *   <li>以后要恢复：只把 {@link #ENABLED} 改回 {@code true}，骑乘代码原样还在。</li>
 * </ul>
 *
 * <p>把所有判断集中到这一个常量，避免"只关了上马、飞行扣蓝还在跑"这种半关状态。</p>
 */
public final class BeastRiding {

    /** 骑乘系统是否开放。2026-11-07 起关闭；改回 true 即恢复。 */
    public static final boolean ENABLED = false;

    private BeastRiding() {
    }

    public static boolean enabled() {
        return ENABLED;
    }
}
