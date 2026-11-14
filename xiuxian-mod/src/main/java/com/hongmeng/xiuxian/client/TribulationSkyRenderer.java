package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>雷劫天象</b>：劫云实体化 + 落雷闪光 + 地面焦痕。
 *
 * <h2>三件事（玩家要求）</h2>
 * <ol>
 *   <li><b>随境界换天象色</b>：第 6 境青、第 9 境紫、第 10 境血；</li>
 *   <li><b>劫云实体化</b>：头顶一片旋转的暗色几何体，比粒子更像"云"；</li>
 *   <li><b>雷击地形</b>：落点留焦痕/裂纹贴花，<b>不破坏方块</b>。</li>
 * </ol>
 *
 * <h2>为什么云用 alpha 混合而不是 additive</h2>
 * <p>乌云是<b>暗</b>的。additive 只能变亮 —— 用它画云会变成"发光的白团"。
 * 所以云和焦痕都用 {@code entityTranslucentEmissive}（普通 alpha 混合），
 * 只有闪电闪光才用 additive。</p>
 *
 * <h2>为什么焦痕会淡出</h2>
 * <p>永久贴花会在存档里无限堆积（还无法清理）。这里做成
 * {@value #SCORCH_TICKS} tick（约 30 秒）自然消散 ——
 * 玩家看得见"刚劈过"，但地上不会永远留着疤。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TribulationSkyRenderer {

    /** 焦痕保留时长（tick）：30 秒。 */
    public static final int SCORCH_TICKS = 600;

    /** 闪光持续（tick）。 */
    public static final int FLASH_TICKS = 6;

    private static final ResourceLocation CLOUD_TEX =
            new ResourceLocation("hongmeng", "textures/fx/trib_cloud.png");
    private static final ResourceLocation SCORCH_TEX =
            new ResourceLocation("hongmeng", "textures/fx/scorch.png");

    /** 雷劫结束的 gameTime（0 = 未在渡劫）。 */
    private static long activeUntil = 0L;
    private static long startTick = 0L;
    private static int tier = 6;

    /** 落雷记录：{x, y, z, startTick, tier}。 */
    private static final List<double[]> STRIKES = new ArrayList<>();

    private TribulationSkyRenderer() {
    }

    /** 现在是否在渡劫（音乐导演要用）。 */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }
        return activeUntil != 0L && mc.level.getGameTime() <= activeUntil;
    }

    /** 收到开始/结束。 */
    public static void onState(boolean active, int tierIn, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        if (!active) {
            activeUntil = 0L;
            tier = 6;
            return;
        }
        tier = tierIn;
        startTick = now;
        activeUntil = now + Math.max(20, ticks);
    }

    /** 收到一道落雷。 */
    public static void onStrike(double x, double y, double z, int tierIn) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        // 允许在"未收到开始包"时也能显示（比如重连后）
        if (activeUntil == 0L) {
            activeUntil = now + 200L;
            startTick = now;
        }
        tier = tierIn;
        STRIKES.add(new double[]{x, y, z, now, tierIn});
        // 控制列表长度（防御性）
        while (STRIKES.size() > 64) {
            STRIKES.remove(0);
        }
    }

    /** 天象色（按境界换色：青 -> 紫 -> 血）。 */
    public static float[] colorFor(int tierIn) {
        if (tierIn >= 10) {
            return new float[]{0.95F, 0.16F, 0.14F};   // 血雷
        }
        if (tierIn >= 9) {
            return new float[]{0.62F, 0.28F, 0.95F};   // 紫雷
        }
        if (tierIn >= 7) {
            return new float[]{0.35F, 0.55F, 1.00F};   // 蓝雷
        }
        return new float[]{0.30F, 0.90F, 0.85F};       // 青雷
    }

    /** 挂到 {@code RenderLevelStageEvent(AFTER_PARTICLES)}。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderStage(
            net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event
                .RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        long now = mc.level.getGameTime();
        if (activeUntil == 0L) {
            return;
        }
        if (now > activeUntil + SCORCH_TICKS) {
            activeUntil = 0L;
            STRIKES.clear();
            return;
        }
        try {
            render(event.getPoseStack(), event.getCamera().getPosition(),
                    event.getPartialTick(), now);
        } catch (Throwable ignored) {
            // 渲染失败绝不能崩客户端
        }
    }

    private static void render(PoseStack pose, Vec3 cam, float partial, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float[] rgb = colorFor(tier);
        boolean any = false;

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        // ---- ① 劫云：玩家头顶一圈旋转的暗色云盘 ----
        if (now <= activeUntil) {
            any |= drawCloud(pose, buffers, cam, mc, now, partial);
        }

        // ---- ② 落雷闪光 + ③ 地面焦痕 ----
        var it = STRIKES.iterator();
        while (it.hasNext()) {
            double[] s = it.next();
            long age = now - (long) s[3];
            if (age > SCORCH_TICKS) {
                it.remove();
                continue;
            }
            float[] c = colorFor((int) s[4]);
            any |= drawScorch(pose, buffers, s[0], s[1], s[2], age, c);
            any |= drawFlash(pose, buffers, s[0], s[1], s[2], age, c, partial);
        }

        pose.popPose();
        if (any) {
            buffers.endBatch(RenderType.lightning());
        }
        // 云与焦痕是 alpha 混合的，要单独 endBatch
        buffers.endBatch(RenderType.entityTranslucentEmissive(CLOUD_TEX));
        buffers.endBatch(RenderType.entityTranslucentEmissive(SCORCH_TEX));
        if (rgb.length == 0) {
            // 防御：避免未使用告警（正常不会走到）
            return;
        }
    }

    /** 头顶的劫云：8 片云团绕圈 + 缓慢自转，越接近落雷越聚拢。 */
    private static boolean drawCloud(PoseStack pose, MultiBufferSource buffers,
                                     Vec3 cam, Minecraft mc, long now, float partial) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(CLOUD_TEX));
        float[] c = colorFor(tier);
        double t = (now + partial) / 20.0;
        // 越接近结束聚得越紧（"云在收拢"）
        float lifeLeft = Mth.clamp((activeUntil - now) / 300.0F, 0.0F, 1.0F);
        double radius = 5.2D - 1.6D * (1.0F - lifeLeft);
        double cx = mc.player.getX();
        double cz = mc.player.getZ();
        double cy = mc.player.getY() + 7.5D;
        var m = pose.last().pose();
        int puffs = 8;
        for (int i = 0; i < puffs; i++) {
            double a = t * 0.55D + 2.0D * Math.PI * i / puffs;
            double px = cx + Math.cos(a) * radius;
            double pz = cz + Math.sin(a) * radius;
            // 每片云团是一个面向相机的方片
            float size = 4.2F;
            float alpha = 0.72F;
            billboardQuad(vc, m, px, cy + Math.sin(a * 2.0D) * 0.35D, pz, size,
                    c[0] * 0.5F, c[1] * 0.5F, c[2] * 0.55F, alpha);
        }
        // 中心再叠一片大的，把中间填满（否则看起来是个环）
        billboardQuad(vc, m, cx, cy + 0.5D, cz, 7.0F,
                c[0] * 0.42F, c[1] * 0.42F, c[2] * 0.48F, 0.66F);
        return true;
    }

    /** 一个面向相机（绕 Y 轴对齐玩家）的方片。 */
    private static void billboardQuad(VertexConsumer vc, org.joml.Matrix4f m,
                                      double x, double y, double z, float size,
                                      float r, float g, float b, float a) {
        float h = size * 0.5F;
        // 用固定的两轴（近似面向相机）—— 云片是"雾状"，不需要精确 billboard
        vc.vertex(m, (float) x - h, (float) y - h, (float) z - h)
                .color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x + h, (float) y - h, (float) z - h)
                .color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x + h, (float) y + h, (float) z + h)
                .color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x - h, (float) y + h, (float) z + h)
                .color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    /** 地面焦痕：贴在落点上方 0.02 格，随年龄淡出。 */
    private static boolean drawScorch(PoseStack pose, MultiBufferSource buffers,
                                      double x, double y, double z, long age,
                                      float[] c) {
        float k = Mth.clamp(age / (float) SCORCH_TICKS, 0.0F, 1.0F);
        float alpha = 0.85F * (1.0F - k);
        if (alpha <= 0.01F) {
            return false;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(SCORCH_TEX));
        var m = pose.last().pose();
        float r = 2.1F;
        float yy = (float) y + 0.02F;
        // 轻微染色，让焦痕带上该境界的雷色
        float cr = 0.75F + 0.25F * c[0];
        float cg = 0.75F + 0.25F * c[1];
        float cb = 0.75F + 0.25F * c[2];
        vc.vertex(m, (float) x - r, yy, (float) z - r)
                .color(cr, cg, cb, alpha).uv(0.0F, 0.0F).overlayCoords(0)
                .uv2(15728880).normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x + r, yy, (float) z - r)
                .color(cr, cg, cb, alpha).uv(1.0F, 0.0F).overlayCoords(0)
                .uv2(15728880).normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x + r, yy, (float) z + r)
                .color(cr, cg, cb, alpha).uv(1.0F, 1.0F).overlayCoords(0)
                .uv2(15728880).normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x - r, yy, (float) z + r)
                .color(cr, cg, cb, alpha).uv(0.0F, 1.0F).overlayCoords(0)
                .uv2(15728880).normal(0.0F, 1.0F, 0.0F).endVertex();
        return true;
    }

    /** 落雷闪光：一道竖直光柱 + 一圈扩散环（additive）。 */
    private static boolean drawFlash(PoseStack pose, MultiBufferSource buffers,
                                     double x, double y, double z, long age,
                                     float[] c, float partial) {
        if (age > FLASH_TICKS) {
            return false;
        }
        float k = Mth.clamp((age + partial) / (float) FLASH_TICKS, 0.0F, 1.0F);
        float alpha = 0.9F * (1.0F - k);
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        var m = pose.last().pose();
        // 光柱（四个面）
        float w = 0.28F * (1.0F - 0.4F * k);
        float top = (float) y + 9.0F;
        for (int i = 0; i < 4; i++) {
            double a0 = Math.PI * 0.5D * i;
            double a1 = Math.PI * 0.5D * (i + 1);
            float x0 = (float) (x + Math.cos(a0) * w);
            float z0 = (float) (z + Math.sin(a0) * w);
            float x1 = (float) (x + Math.cos(a1) * w);
            float z1 = (float) (z + Math.sin(a1) * w);
            vc.vertex(m, x0, (float) y, z0).color(c[0], c[1], c[2], alpha).endVertex();
            vc.vertex(m, x1, (float) y, z1).color(c[0], c[1], c[2], alpha).endVertex();
            vc.vertex(m, x1, top, z1).color(c[0], c[1], c[2], alpha * 0.15F)
                    .endVertex();
            vc.vertex(m, x0, top, z0).color(c[0], c[1], c[2], alpha * 0.15F)
                    .endVertex();
        }
        // 地面扩散环
        double radius = 0.6D + 3.2D * k;
        int seg = 18;
        for (int i = 0; i < seg; i++) {
            double a0 = 2.0D * Math.PI * i / seg;
            double a1 = 2.0D * Math.PI * (i + 1) / seg;
            float yy = (float) y + 0.06F;
            float x0 = (float) (x + Math.cos(a0) * radius);
            float z0 = (float) (z + Math.sin(a0) * radius);
            float x1 = (float) (x + Math.cos(a1) * radius);
            float z1 = (float) (z + Math.sin(a1) * radius);
            vc.vertex(m, x0, yy, z0).color(c[0], c[1], c[2], alpha).endVertex();
            vc.vertex(m, x1, yy, z1).color(c[0], c[1], c[2], alpha).endVertex();
            vc.vertex(m, x1, yy + 0.12F, z1).color(c[0], c[1], c[2], alpha * 0.4F)
                    .endVertex();
            vc.vertex(m, x0, yy + 0.12F, z0).color(c[0], c[1], c[2], alpha * 0.4F)
                    .endVertex();
        }
        return true;
    }
}
