package com.hongmeng.xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>特效看门狗</b>：任何"全屏级"表现卡住不放，强制清掉。
 *
 * <h2>为什么需要</h2>
 * <p>玩家反馈："玩着玩着就出现莫名其妙的特效，还消失不掉"（血修、魔修都有）。
 * 逐个特效类去查衰减是对的，但<b>总会有遗漏</b>（新加的特效忘了写衰减、
 * 或者服务端一直重发把强度钉在 1.0）。</p>
 *
 * <p>所以加一道兜底：<b>换维度必定清空</b>；某个强度类连续 30 秒都是满值
 * 且期间没有任何"新触发"，也强制归零。宁可少一点氛围，也不能糊住视野。</p>
 */
public final class FxWatchdog {

    /** 满强度持续多少 tick 就强制归零（30 秒）。 */
    private static final int HOLD_LIMIT = 600;

    private static int pinnedTicks = 0;
    private static Object lastDimension = null;

    private FxWatchdog() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            lastDimension = null;
            pinnedTicks = 0;
            return;
        }

        // ① 换维度：全清（这一条能解决"从副本回来特效还挂着"）
        Object dim = mc.level.dimension();
        if (lastDimension != null && !lastDimension.equals(dim)) {
            clearAll();
        }
        lastDimension = dim;

        // ② 满强度长时间不动：强制衰减
        boolean pinned =
                com.hongmeng.xiuxian.realworld.RealWorldFxState.strength() > 0.98F
                        || com.hongmeng.xiuxian.realworld.EvilSky.strength() > 0.98F;
        if (pinned) {
            pinnedTicks++;
            if (pinnedTicks > HOLD_LIMIT) {
                clearAll();
                pinnedTicks = 0;
            }
        } else {
            pinnedTicks = 0;
        }
    }

    /** 把所有"会糊住视野"的客户端表现归零。 */
    public static void clearAll() {
        try {
            com.hongmeng.xiuxian.client.ClientSpellFx.clear();
        } catch (Throwable ignored) {
            // 忽略
        }
        try {
            com.hongmeng.xiuxian.client.MagicCircles.clear();
        } catch (Throwable ignored) {
            // 忽略
        }
        try {
            com.hongmeng.xiuxian.realworld.RealWorldFxState.setStrength(0.0F);
        } catch (Throwable ignored) {
            // 忽略
        }
        try {
            com.hongmeng.xiuxian.realworld.EvilSky.set(0.0F, 0, 2);
        } catch (Throwable ignored) {
            // 忽略
        }
    }
}
