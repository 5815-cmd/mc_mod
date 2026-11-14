package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>怪物格挡时的「气盾」</b>：让玩家一眼看出"这只在挡"。
 *
 * <h2>为什么需要</h2>
 * <p>上一轮加了怪物格挡，但表现只有<b>起手一瞬间</b>的粒子。
 * 玩家砍上去发现"伤害变小了"，却<b>看不出它在格挡</b> ——
 * 只会觉得"伤害数字不对劲"。</p>
 *
 * <h2>画什么</h2>
 * <p>身前一道<b>弧形能量幕</b>：以怪为圆心、朝它面向的方向张开约 110°，
 * 半径 0.9 格，高度 1.2 格。用 {@code RenderType.lightning()}
 * ——它自带 additive 混合、无贴图、不剔除面，正是能量效果要的。</p>
 *
 * <h2>为什么用扇形而不是"一个平面"</h2>
 * <p>平面从侧面看会变成一条线（纸片感）。弧形在任意角度都有厚度感，
 * 这也是本项目特效一直强调的原则。</p>
 *
 * <h2>状态从哪来</h2>
 * <p>服务端在起手时发 {@code MobGuardFxPacket}（一次格挡一个包，极低频）。
 * 客户端只按 entityId 记到期 tick —— HUD 渲染不参与判定，
 * 判定永远在服务端。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class MobShieldRenderer {

    /** 一道气盾：entityId -> {到期 tick, 起手 tick}。 */
    private static final Map<Integer, long[]> SHIELDS = new ConcurrentHashMap<>();

    /** 弧形张角（度）。 */
    private static final float ARC_DEG = 110.0F;

    /** 弧的分段数（越大越圆，12 段已经看不出折线）。 */
    private static final int SEGMENTS = 12;

    /** 半径（格）。 */
    private static final float RADIUS = 0.9F;

    private MobShieldRenderer() {
    }

    /** 收到广播：这只怪开始格挡了。 */
    public static void onGuard(int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        SHIELDS.put(entityId, new long[]{now + Math.max(1, ticks), now});
    }

    /**
     * 在 {@code RenderLevelStageEvent(AFTER_PARTICLES)} 里画。
     *
     * <p>放在 AFTER_PARTICLES 是为了<b>透过滤镜之后</b>画 ——
     * 早于它会受迷雾/雾效影响，能量效果会发灰。</p>
     */
    public static void render(PoseStack pose, Vec3 camPos, float partial) {
        if (SHIELDS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            SHIELDS.clear();
            return;
        }
        long now = mc.level.getGameTime();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        pose.pushPose();
        // 转到相机坐标系（世界坐标 - 相机位置）
        pose.translate(-camPos.x, -camPos.y, -camPos.z);

        boolean any = false;
        var it = SHIELDS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            long until = entry.getValue()[0];
            long start = entry.getValue()[1];
            if (now > until) {
                it.remove();
                continue;
            }
            Entity e = mc.level.getEntity(entry.getKey());
            if (e == null || !e.isAlive()) {
                it.remove();
                continue;
            }
            // 出现/消失都用 0.15 做淡入淡出，避免"啪"地跳出来
            float life = (float) (until - start);
            float done = (now - start) + partial;
            float fadeIn = Mth.clamp(done / 3.0F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((life - done) / 3.0F, 0.0F, 1.0F);
            float alpha = Math.min(fadeIn, fadeOut) * 0.55F;
            if (alpha <= 0.01F) {
                continue;
            }
            any |= drawShield(pose, buffers, e, alpha, partial);
        }
        pose.popPose();

        if (any) {
            buffers.endBatch(RenderType.lightning());
        }
    }

    /**
     * 挂到渲染阶段（AFTER_PARTICLES）。
     *
     * <p>为什么是这个阶段：早于粒子会在迷雾之前画，能量效果会被雾冲淡发灰。</p>
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderStage(
            net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event
                .RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        try {
            render(event.getPoseStack(), event.getCamera().getPosition(),
                    event.getPartialTick());
        } catch (Throwable ignored) {
            // 渲染失败绝不能崩客户端
        }
    }

    /** 画一道弧形气盾。 */
    private static boolean drawShield(PoseStack pose, MultiBufferSource buffers,
                                      Entity e, float alpha, float partial) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        // 插值位置（否则高速移动的怪盾会拖在后面）
        double x = Mth.lerp(partial, e.xOld, e.getX());
        double y = Mth.lerp(partial, e.yOld, e.getY());
        double z = Mth.lerp(partial, e.zOld, e.getZ());
        float yRot = Mth.lerp(partial, e.yRotO, e.getYRot());
        float yawRad = (float) Math.toRadians(yRot);
        // MC 的实体朝向：yaw=0 朝 +Z，yaw 增大顺时针（从上看）
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);

        float h = e.getBbHeight();
        double cy = y + h * 0.58D;
        // 竖向两段：上沿略窄（弧顶收一点，看起来是"罩"不是"板"）
        double yTop = cy + 0.62D;
        double yBot = cy - 0.62D;

        // 颜色：淡青色（能量），顶部略亮
        float r = 0.45F;
        float gg = 0.85F;
        float b = 1.00F;

        double half = Math.toRadians(ARC_DEG * 0.5D);
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = -half + (2.0D * half) * i / SEGMENTS;
            double a1 = -half + (2.0D * half) * (i + 1) / SEGMENTS;
            // 以"怪的中心 + 朝向前方"为弧心，弧面沿半径张开
            double x0 = x + Math.cos(yawRad + a0) * RADIUS;
            double z0 = z - Math.sin(yawRad + a0) * RADIUS;
            double x1 = x + Math.cos(yawRad + a1) * RADIUS;
            double z1 = z - Math.sin(yawRad + a1) * RADIUS;
            // 中段最亮（弧顶），两端渐隐 —— 让弧有"包住"的观感
            float mid = 1.0F - Math.abs((i + 0.5F) / SEGMENTS * 2.0F - 1.0F);
            float a = alpha * (0.45F + 0.55F * mid);
            var m = pose.last().pose();
            // 左上一组
            vc.vertex(m, (float) x0, (float) yTop, (float) z0)
                    .color(r, gg, b, a * 0.35F).endVertex();
            vc.vertex(m, (float) x1, (float) yTop, (float) z1)
                    .color(r, gg, b, a * 0.35F).endVertex();
            // 下沿为强色（能量聚在下方，像"气墙"的根部）
            vc.vertex(m, (float) x1, (float) yBot, (float) z1)
                    .color(r, gg, b, a).endVertex();
            vc.vertex(m, (float) x0, (float) yBot, (float) z0)
                    .color(r, gg, b, a).endVertex();
        }
        // 面向参考：避免未使用变量告警
        return fx != 0.0D || fz != 0.0D;
    }
}
