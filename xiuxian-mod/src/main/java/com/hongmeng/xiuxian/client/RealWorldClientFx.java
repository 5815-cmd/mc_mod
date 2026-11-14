package com.hongmeng.xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>真实世界的环境表现</b>（参考泰拉瑞亚"肉后"的氛围变化）。
 *
 * <h2>玩家要求</h2>
 * <blockquote>「开启真实世界的话，世界要发生变化，你可以参考泰拉瑞亚的肉前和肉后」</blockquote>
 *
 * <h2>没有 shader 时能做到什么</h2>
 * <p>做不到"整片天空换材质"，但能做<b>雾色与能见度</b>——而这恰恰是氛围的第一信号。
 * 泰拉瑞亚肉后最直观的变化也是<b>天色与雾</b>，不是换模型。</p>
 *
 * <p>做法：订阅 {@link ViewportEvent.ComputeFogColor}，把雾色朝<b>暗红</b>插值，
 * 并按强度轻微压远平面 —— 世界看起来"沉"下去了。</p>
 *
 * <h2>为什么带一个渐变强度</h2>
 * <p>直接切换会像"戴了红眼镜"一样突兀。开启后 {@code blend} 从 0 缓慢升到 1
 * （约 8 秒），配合过场动画，观感是"天慢慢变色"，而不是一瞬间坏掉。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class RealWorldClientFx {

    /** 服务端同步过来的状态。 */
    private static boolean active = false;
    /** 远平面最多收近多少（12% —— 再收会挡战斗视野）。 */
    public static final float FOG_SHRINK = 0.12F;

    /** 0~1 的渐变强度（开启后缓慢升到 1）。 */
    private static float blend = 0.0F;

    private RealWorldClientFx() {
    }

    /** 收到同步包。 */
    public static void setActive(boolean value) {
        active = value;
        if (!value) {
            blend = 0.0F;   // 关掉就立刻恢复（没有"渐隐"的必要）
            com.hongmeng.xiuxian.realworld.RealWorldFxState.setStrength(0.0F);
        }
    }

    public static boolean active() {
        return active;
    }

    /** 每 tick 推进渐变（由客户端 tick 调用）。 */
    public static void tick() {
        if (!active) {
            return;
        }
        if (blend < 1.0F) {
            // 约 160 tick（8 秒）升满：配合过场动画的时长
            blend = Math.min(1.0F, blend + 1.0F / 160.0F);
        }
        // 把强度共享出去：天空穹顶等其它表现要读同一个值，
        // 否则会出现"天已经红了、雾还没变"的割裂感。
        com.hongmeng.xiuxian.realworld.RealWorldFxState.setStrength(blend);
    }


    /**
     * 雾的<b>距离</b>衰减：远平面收近，世界看起来更"闷"。
     *
     * <p>只用雾色不够 —— 颜色变了但视野还是通透的，像"戴了红眼镜"。
     * 把远平面收近 {@value #FOG_SHRINK} 才有"世界变小了"的潜意识信号。</p>
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!active || blend <= 0.001F) {
            return;
        }
        try {
            // 只压地形雾；水下/岩浆/失明那几种是玩法信息，不能动
            if (event.getMode() != net.minecraft.client.renderer.FogRenderer
                    .FogMode.FOG_TERRAIN) {
                return;
            }
            float k = 1.0F - FOG_SHRINK * blend;
            event.scaleFarPlaneDistance(k);
            event.scaleNearPlaneDistance(Math.max(0.5F, 1.0F - FOG_SHRINK * 0.4F * blend));
        } catch (Throwable ignored) {
            // 雾参数不可用时什么都不做
        }
    }

    /**
     * 雾色：朝暗红插值。
     *
     * <p>系数刻意不激进（最多 0.55）—— 压太狠会导致夜里完全看不见路，
     * 那不是"氛围"，是妨碍游玩。</p>
     */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!active || blend <= 0.001F) {
            return;
        }
        // 目标色：暗红偏褐（干涸血色，不是鲜红）
        float tr = 0.34F;
        float tg = 0.12F;
        float tb = 0.11F;
        float k = 0.55F * blend;
        event.setRed(event.getRed() + (tr - event.getRed()) * k);
        event.setGreen(event.getGreen() + (tg - event.getGreen()) * k);
        event.setBlue(event.getBlue() + (tb - event.getBlue()) * k);
    }

}
