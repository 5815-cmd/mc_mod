package com.hongmeng.xiuxian.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 把坠落穿越的相机角度接到 {@code ComputeCameraAngles}（第五轮"更稳"版）。
 *
 * <h2>稳在哪</h2>
 * <ul>
 *   <li>角度全部来自 {@link EvilFallFx#cameraAngles(float)} 的<b>缓入缓出</b>曲线，
 *       本类只做接线 + 防御；</li>
 *   <li>对 NaN/Inf 和非有限值直接忽略（实体/玩家瞬移、维度切换时数值可能异常）；</li>
 *   <li>pitch/roll 再夹一次范围（-60~60 / -6~6），防止叠加原版视角后穿到脑后；</li>
 *   <li>用上一帧的值做 25% 低通，掉帧时也不会"跳一下"。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class EvilFallCamera {

    /** 上一帧的附加角（低通用）。 */
    private static float lastPitch = 0.0F;
    private static float lastRoll = 0.0F;
    /** 平滑系数：越小越稳、越迟钝。 */
    private static final float SMOOTH = 0.25F;

    private EvilFallCamera() {
    }

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        float partial = (float) event.getPartialTick();
        float[] adj = EvilFallFx.cameraAngles(partial);
        if (adj == null || adj.length < 2) {
            lastPitch = 0.0F;
            lastRoll = 0.0F;
            return;
        }
        float pitch = adj[0];
        float roll = adj[1];
        if (!Float.isFinite(pitch) || !Float.isFinite(roll)) {
            return;
        }
        // 低通：吸收掉掉帧/瞬移造成的突变
        lastPitch += (pitch - lastPitch) * SMOOTH;
        lastRoll += (roll - lastRoll) * SMOOTH;
        float addPitch = clamp(lastPitch, -60.0F, 60.0F);
        float addRoll = clamp(lastRoll, -6.0F, 6.0F);
        event.setPitch(clamp(event.getPitch() + addPitch, -90.0F, 90.0F));
        event.setRoll(clamp(event.getRoll() + addRoll, -180.0F, 180.0F));
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    /** 演出结束时清零（避免下一次第一帧继承旧角度）。 */
    public static void reset() {
        lastPitch = 0.0F;
        lastRoll = 0.0F;
    }
}
