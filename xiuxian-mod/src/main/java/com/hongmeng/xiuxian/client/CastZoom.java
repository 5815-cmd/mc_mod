package com.hongmeng.xiuxian.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>施法时把视野拉远</b>。
 *
 * <p>玩家反馈「放法术时全在特效里面，啥也看不见」。与其移动相机（会穿模），
 * 不如把 <b>FOV 抬上去</b>：视野变宽，特效占比自然变小。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CastZoom {

    /** 持续 tick。 */
    public static final int TICKS = 40;

    /** 普通法术加多少度。 */
    public static final float MAX_ADD = 18.0F;

    /** 灭世级法术加多少度（玩家要求"要在很远的地方看"）。 */
    public static final float MAX_ADD_BIG = 42.0F;

    private static int remaining = 0;

    private CastZoom() {
    }

    private static float add = MAX_ADD;

    public static void begin(boolean big) {
        add = big ? MAX_ADD_BIG : MAX_ADD;
        remaining = TICKS;
    }

    public static void begin() {
        begin(false);
    }

    public static void tick() {
        if (remaining > 0) {
            remaining--;
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (remaining <= 0) {
            return;
        }
        float p = remaining / (float) TICKS;      // 1 -> 0
        event.setFOV(event.getFOV() + add * p);
    }
}
