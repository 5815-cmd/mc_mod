package com.hongmeng.xiuxian.technique;

import net.minecraftforge.common.MinecraftForge;

/**
 * 法术系统的事件挂载点。
 *
 * <p><b>为什么单独开一个类：</b>{@code core/ModEvents.java} 同时在改灵力与御剑飞行，
 * 法术这边的每 tick 推进（蓄力结算 + 被动重算）与登录/重生/掉线清理
 * 全部收在 {@link SpellCastHandler} 里，由本类挂到 Forge 事件总线上——
 * 那边一行都不用动，也就不会互相回退。</p>
 *
 * <p>调用时机：{@code HongmengMod} 构造阶段的任意位置调一次
 * {@link #register()} 即可；重复调用是无害的（幂等）。</p>
 */
public final class SpellSystem {

    /** 已经挂过的那一份实例，保证只挂一次。 */
    private static SpellCastHandler handler;

    private SpellSystem() {
    }

    /**
     * 把法术系统挂上事件总线。
     *
     * <p>刻意**再 new 一个实例**而不是复用既有的：Forge 事件总线允许
     * 同一个事件有多个监听器，多挂一份不会顶掉别人的。</p>
     */
    public static synchronized void register() {
        if (handler != null) {
            return;
        }
        handler = new SpellCastHandler();
        MinecraftForge.EVENT_BUS.register(handler);
    }

    /** 是不是已经挂上了（诊断用）。 */
    public static boolean isRegistered() {
        return handler != null;
    }
}
