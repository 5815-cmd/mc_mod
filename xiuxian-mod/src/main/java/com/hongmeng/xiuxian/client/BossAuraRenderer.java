package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Boss 压迫感光环</b>：给所有 BOSS / OVERLORD 品阶的怪加一层持续的环境表现。
 *
 * <h2>为什么需要</h2>
 * <p>玩家对"华丽、有压迫感"的要求，很大一部分不来自模型本身，而来自
 * <b>它站着的时候周围有没有"重量"</b>。原版 Boss 只有血条，站定后什么都没，
 * 看起来就像一个放大的普通怪。</p>
 *
 * <h2>三层表现</h2>
 * <ol>
 *   <li><b>地面符文环</b>：脚下两圈符文，<b>反向旋转</b>。
 *       双环反向是最省事的"法阵感"——同向会看起来像唱片。</li>
 *   <li><b>环绕光点</b>：6 颗光点在不同高度绕它转，高度用正弦错开，
 *       让"压迫"是<b>三维</b>的而不是贴地一层。</li>
 *   <li><b>脉动核心</b>：躯干高度一团随呼吸涨落的光，指示它的"气"。</li>
 * </ol>
 *
 * <h2>配色</h2>
 * <p>按实体类型关键词取色：炎系橙红 / 虚空紫 / 血系深红 / 傀儡青金。
 * 一个渲染器覆盖全部 10 只 boss，不用逐个写。</p>
 *
 * <h2>性能</h2>
 * <p>只处理 48 格内、BOSS/OVERLORD 品阶的活体；每只约 30 个面片。
 * 即使同时出现 4 只也只有 120 个面片。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class BossAuraRenderer {

    /** 可见半径（格）。 */
    private static final double RANGE = 48.0D;

    /** 符文环分段。 */
    private static final int SEGMENTS = 24;

    /**
     * PERF(C): Boss 列表刷新间隔（tick）。
     *
     * <p>原来每帧遍历一次 {@code level.entitiesForRendering()} 并 new 一个
     * ArrayList；光环是持续表现、Boss 移动很慢，5 tick（0.25 秒）刷新一次
     * 完全看不出差别，每帧的实体遍历变成每 5 tick 一次。</p>
     */
    private static final int SCAN_INTERVAL_TICKS = 5;

    /** PERF(C): 扫描结果缓存（复用容器）。 */
    private static final List<LivingEntity> CACHED = new ArrayList<>();
    private static long lastScanTick = Long.MIN_VALUE;

    /** PERF(C): 实体类型 → 光晕颜色（原来每帧每只 Boss 都 new float[]{...} + 注册表查名）。 */
    private static final java.util.Map<net.minecraft.world.entity.EntityType<?>, float[]>
            COLORS = new java.util.concurrent.ConcurrentHashMap<>();

    private BossAuraRenderer() {
    }

    /** 按实体类型取色。 */
    private static float[] colorOf(LivingEntity e) {
        // PERF(C): 颜色只跟实体类型有关，按类型缓存（枚举型地图 key 是 identity hash）
        return COLORS.computeIfAbsent(e.getType(), type -> {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(type).getPath();
            if (id.contains("blaze") || id.contains("flame") || id.contains("demon")) {
                return new float[]{1.00F, 0.42F, 0.16F};   // 炎系：橙红
            }
            if (id.contains("void") || id.contains("dragon")) {
                return new float[]{0.62F, 0.32F, 1.00F};   // 虚空：紫
            }
            if (id.contains("beast") || id.contains("serpent")) {
                return new float[]{0.92F, 0.72F, 0.24F};   // 兽系：金
            }
            if (id.contains("epoch") || id.contains("puppet")) {
                return new float[]{0.45F, 0.92F, 0.78F};   // 傀儡：青金
            }
            return new float[]{0.86F, 0.20F, 0.24F};       // 默认：血红
        });
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
        List<LivingEntity> bosses = nearbyBosses(mc);
        if (bosses.isEmpty()) {
            return;
        }
        try {
            render(event.getPoseStack(), event.getCamera().getPosition(),
                    event.getPartialTick(), bosses, mc);
        } catch (Throwable ignored) {
            // 渲染失败绝不能崩客户端
        }
    }

    /** PERF(C): 48 格内 BOSS/OVERLORD 的列表，按 5 tick 节流刷新、复用同一个 List。 */
    private static List<LivingEntity> nearbyBosses(Minecraft mc) {
        long now = mc.level.getGameTime();
        // PERF(C): 首次扫描时 lastScanTick 是哨兵值，必须显式判断（差值会溢出）
        if (lastScanTick != Long.MIN_VALUE
                && now >= lastScanTick && now - lastScanTick < SCAN_INTERVAL_TICKS) {
            return CACHED;
        }
        lastScanTick = now;
        CACHED.clear();
        try {
            for (var e : mc.level.entitiesForRendering()) {
                if (!(e instanceof CultivationMonsterEntity cm) || !cm.isAlive()) {
                    continue;
                }
                MobRank r = cm.effectiveRank();
                if (r != MobRank.BOSS && r != MobRank.OVERLORD) {
                    continue;
                }
                if (cm.distanceToSqr(mc.player) > RANGE * RANGE) {
                    continue;
                }
                CACHED.add(cm);
            }
        } catch (Throwable ignored) {
            CACHED.clear();
        }
        return CACHED;
    }

    private static void render(PoseStack pose, Vec3 cam, float partial,
                               List<LivingEntity> bosses, Minecraft mc) {
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        float time = (mc.level.getGameTime() + partial) / 20.0F;

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        var m = pose.last().pose();

        for (LivingEntity e : bosses) {
            // PERF(C): 名单是 5 tick 前的缓存，跳过已死/已移除的实体
            if (!e.isAlive() || e.isRemoved()) {
                continue;
            }
            float[] c = colorOf(e);
            boolean overlord = ((CultivationMonsterEntity) e).effectiveRank()
                    == MobRank.OVERLORD;
            double px = Mth.lerp(partial, e.xOld, e.getX());
            double py = Mth.lerp(partial, e.yOld, e.getY());
            double pz = Mth.lerp(partial, e.zOld, e.getZ());
            // 第四轮 §2.2：光晕半径/高度按**体型加成**（巨化后的 Boss 包围盒更高；
            // getBbHeight() 已经含 giant，所以这里不再乘倍率，避免与巨化系统重复）。
            // 只在本来这一层里缩放，不另画第二层光环。
            float sizeBoost = Mth.clamp(e.getBbHeight() / 3.5F, 1.0F, 2.4F);
            float baseR = (float) (overlord ? 2.9D : 2.2D) * sizeBoost;

            // ② 六颗环绕光点（高度正弦错开）
            for (int i = 0; i < 6; i++) {
                double ang = time * 0.9D + i * 1.0472D;
                double rad = baseR * 0.82D;
                double mx = px + Math.cos(ang) * rad;
                double mz = pz + Math.sin(ang) * rad;
                double my = py + (0.7D + Math.sin(ang * 1.6D) * 0.55D
                        + (overlord ? 0.35D : 0.0D)) * sizeBoost;
                float r = 0.045F * (0.75F + 0.45F
                        * Mth.sin(time * 2.0F + i * 1.3F)) * Math.min(1.8F, sizeBoost);
                cross(vc, m, mx, my, mz, r, c, 0.45F);
            }

            // ③ 躯干高度脉动核心（高度/半径随体型加成）
            float breath = 0.55F + 0.45F * Mth.sin(time * 2.2F);
            float coreR = (overlord ? 0.42F : 0.32F) * sizeBoost
                    * (0.8F + 0.35F * breath);
            cross(vc, m, px, py + (overlord ? 2.2D : 1.6D) * sizeBoost, pz, coreR, c,
                    0.18F * breath);
        }

        pose.popPose();
        buffers.endBatch(RenderType.lightning());
    }

    /** 一圈由短横线组成的符文环。 */
    private static void ring(VertexConsumer vc, org.joml.Matrix4f m,
                             double cx, double cy, double cz, float radius,
                             float phase, float[] c, float alpha) {
        float w = radius * 0.055F;
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = 2.0D * Math.PI * i / SEGMENTS + phase;
            double a1 = 2.0D * Math.PI * (i + 1) / SEGMENTS + phase;
            // 每 3 段留空 —— 看起来像"符文"而不是实线
            if (i % 3 == 2) {
                continue;
            }
            double x0 = cx + Math.cos(a0) * radius;
            double z0 = cz + Math.sin(a0) * radius;
            double x1 = cx + Math.cos(a1) * radius;
            double z1 = cz + Math.sin(a1) * radius;
            double ix0 = cx + Math.cos(a0) * (radius - w);
            double iz0 = cz + Math.sin(a0) * (radius - w);
            double ix1 = cx + Math.cos(a1) * (radius - w);
            double iz1 = cz + Math.sin(a1) * (radius - w);
            vc.vertex(m, (float) ix0, (float) cy, (float) iz0)
                    .color(c[0], c[1], c[2], 0.0F).endVertex();
            vc.vertex(m, (float) ix1, (float) cy, (float) iz1)
                    .color(c[0], c[1], c[2], 0.0F).endVertex();
            vc.vertex(m, (float) x1, (float) cy, (float) z1)
                    .color(c[0], c[1], c[2], alpha).endVertex();
            vc.vertex(m, (float) x0, (float) cy, (float) z0)
                    .color(c[0], c[1], c[2], alpha).endVertex();
        }
    }

    /** 一个交叉方片发光点。 */
    private static void cross(VertexConsumer vc, org.joml.Matrix4f m,
                              double x, double y, double z, float r,
                              float[] c, float a) {
        vc.vertex(m, (float) x - r, (float) y - r, (float) z)
                .color(c[0], c[1], c[2], 0.0F).endVertex();
        vc.vertex(m, (float) x + r, (float) y - r, (float) z)
                .color(c[0], c[1], c[2], 0.0F).endVertex();
        vc.vertex(m, (float) x + r, (float) y + r, (float) z)
                .color(c[0], c[1], c[2], a).endVertex();
        vc.vertex(m, (float) x - r, (float) y + r, (float) z)
                .color(c[0], c[1], c[2], a).endVertex();
        vc.vertex(m, (float) x - r, (float) y, (float) z - r)
                .color(c[0], c[1], c[2], 0.0F).endVertex();
        vc.vertex(m, (float) x + r, (float) y, (float) z - r)
                .color(c[0], c[1], c[2], 0.0F).endVertex();
        vc.vertex(m, (float) x + r, (float) y, (float) z + r)
                .color(c[0], c[1], c[2], a).endVertex();
        vc.vertex(m, (float) x - r, (float) y, (float) z + r)
                .color(c[0], c[1], c[2], a).endVertex();
    }
}
