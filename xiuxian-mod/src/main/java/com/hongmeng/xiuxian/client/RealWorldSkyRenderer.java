package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.realworld.RealWorldFxState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>真世界的天空替换</b>（没有 shader 的做法）。
 *
 * <h2>玩家要求</h2>
 * <blockquote>「天空贴图替换 —— 需要 shader 或自定义天空渲染，与光影冲突风险高」</blockquote>
 *
 * <h2>为什么不用 shader</h2>
 * <p>自定义 core shader 会与 OptiFine / Iris 光影直接冲突，而且是"装了光影就坏"的类型。
 * 项目一贯避免这种方案。</p>
 *
 * <h2>这里怎么做</h2>
 * <p>在 {@code AFTER_SKY} 阶段画一个<b>大半径渐变穹顶</b>：
 * <ul>
 *   <li>半径 {@value #RADIUS} 格（远大于地形，但小于远平面，所以不会裁剪）；</li>
 *   <li>用 {@code RenderType.lightning()} —— 自带 additive、无贴图、不写深度；</li>
 *   <li>alpha 很低（{@value #MAX_ALPHA} 上限）：这是<b>染色</b>，不是"盖一层红布"；</li>
 *   <li>竖直方向做梯度（近地平线更浓）—— 真实大气就是这样的，
 *       均匀染色会像贴了张滤色纸。</li>
 * </ul>
 *
 * <p>地形会正常遮住穹顶下半部分（因为它在 AFTER_SKY，深度测试已经在跑），
 * 所以不会出现"红色的墙挡在地平线上"。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class RealWorldSkyRenderer {

    /** 穹顶半径（格）。要大于视距内的地形、小于远平面。 */
    private static final float RADIUS = 190.0F;

    /** 穹顶分段：经 24 / 纬 8 —— 渐变为主，不需要太细。 */
    private static final int LON = 24;
    private static final int LAT = 8;

    /** 最大不透明度（染色，不是覆盖）。 */
    private static final float MAX_ALPHA = 0.30F;

    private RealWorldSkyRenderer() {
    }

    /**
     * 挂在 {@code RenderLevelStageEvent(AFTER_SKY)}。
     *
     * <p>为什么是 AFTER_SKY：天空刚画完、地形还没画 ——
     * 这时候铺一层染色，地形会正确遮挡它。</p>
     */
    public static void onRenderStage(
            net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event
                .RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        float strength = RealWorldFxState.strength();
        if (strength <= 0.01F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        try {
            render(event.getPoseStack(), strength);
        } catch (Throwable ignored) {
            // 天空渲染失败绝不能崩客户端
        }
    }

    private static void render(PoseStack pose, float strength) {
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());

        // 目标色：暗红偏紫的"天在渗血"
        float r = 0.42F;
        float g = 0.13F;
        float b = 0.12F;

        pose.pushPose();
        var m = pose.last().pose();
        for (int i = 0; i < LAT; i++) {
            // 纬度：从地平线（0）到天顶（1）
            double t0 = (double) i / LAT;
            double t1 = (double) (i + 1) / LAT;
            float y0 = (float) (Math.sin(t0 * Math.PI * 0.5D) * RADIUS);
            float y1 = (float) (Math.sin(t1 * Math.PI * 0.5D) * RADIUS);
            float rr0 = (float) (Math.cos(t0 * Math.PI * 0.5D) * RADIUS);
            float rr1 = (float) (Math.cos(t1 * Math.PI * 0.5D) * RADIUS);
            // 近地平线更浓（大气厚度更大），天顶更淡
            float a0 = MAX_ALPHA * strength * (1.0F - 0.55F * (float) t0);
            float a1 = MAX_ALPHA * strength * (1.0F - 0.55F * (float) t1);
            for (int j = 0; j < LON; j++) {
                double u0 = 2.0D * Math.PI * j / LON;
                double u1 = 2.0D * Math.PI * (j + 1) / LON;
                float x00 = (float) (Math.cos(u0) * rr0);
                float z00 = (float) (Math.sin(u0) * rr0);
                float x01 = (float) (Math.cos(u1) * rr0);
                float z01 = (float) (Math.sin(u1) * rr0);
                float x10 = (float) (Math.cos(u0) * rr1);
                float z10 = (float) (Math.sin(u0) * rr1);
                float x11 = (float) (Math.cos(u1) * rr1);
                float z11 = (float) (Math.sin(u1) * rr1);
                vc.vertex(m, x00, y0, z00).color(r, g, b, a0).endVertex();
                vc.vertex(m, x01, y0, z01).color(r, g, b, a0).endVertex();
                vc.vertex(m, x11, y1, z11).color(r, g, b, a1).endVertex();
                vc.vertex(m, x10, y1, z10).color(r, g, b, a1).endVertex();
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.lightning());
    }
}
