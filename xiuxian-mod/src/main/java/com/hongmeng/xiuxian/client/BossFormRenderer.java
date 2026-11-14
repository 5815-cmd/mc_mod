package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Boss 形态变化的表现层</b>（玩家要求：二阶段、三阶段"形态真的会变"）。
 *
 * <h2>问题</h2>
 * <p>{@code BossPhase} 早就在算三阶段，也对服务端做了属性变化 ——
 * 但<b>客户端没有任何渲染</b>。文档里写的"体型 +18%、颜色转紫黑、长出角与翼"
 * <b>从来没有画出来过</b>。玩家看到的只是"血条段数变了"。</p>
 *
 * <h2>✅ 不需要联网同步</h2>
 * <p>血量比例本身<b>已经被原版同步到客户端</b>（血条就是靠它画的），
 * 所以阶段可以在客户端<b>本地推导</b>：
 * {@code phase = f(health / maxHealth)}。
 * 这样两端不可能不一致，也不用新增包。</p>
 *
 * <h2>画什么（都没有外部模型，全靠代码几何）</h2>
 * <table border="1">
 *   <caption>三阶段外形</caption>
 *   <tr><th>阶段</th><th>体型</th><th>额外几何</th></tr>
 *   <tr><td>一</td><td>×1.00</td><td>无（保持人形）</td></tr>
 *   <tr><td>二</td><td>×1.18</td><td>头顶<b>六片魔角光环</b>（缓慢旋转，紫）</td></tr>
 *   <tr><td>三</td><td>×1.30</td><td>光环 + 背后<b>两片能量翼</b> + 脚下<b>堕落法阵</b>（深红）</td></tr>
 * </table>
 *
 * <p>几何用 {@code RenderType.lightning()}：additive、无贴图 —— 正适合"能量形态"。
 * 这些是<b>附加装饰</b>，不替换原模型，所以不会和任何资源包冲突。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class BossFormRenderer {

    /** 第二阶段体型倍率。 */
    public static final float SCALE_P2 = 1.18F;
    /** 第三阶段体型倍率。 */
    public static final float SCALE_P3 = 1.30F;

    /** 光环片数。 */
    private static final int CROWN_SHARDS = 6;

    private BossFormRenderer() {
    }

    /**
     * 从血量比例推阶段（1/2/3）。
     *
     * <p>为什么在客户端推而不是读服务端字段：血量比例已经同步，
     * 本地推导<b>不可能出现两端不一致</b>，也省一个包。</p>
     */
    public static int phaseOfLocal(LivingEntity e) {
        if (e == null) {
            return 1;
        }
        float max = Math.max(1.0F, e.getMaxHealth());
        float ratio = Mth.clamp(e.getHealth() / max, 0.0F, 1.0F);
        if (ratio > 0.66F) {
            return 1;
        }
        return ratio > 0.33F ? 2 : 3;
    }

    /**
     * Boss 的<b>实际渲染高度</b>（格）。
     *
     * <p>阶段装饰（冠 / 翼 / 法阵 / 冲击环）必须按这个高度放，不能用 {@code getBbHeight()}：
     * 模型普遍比命中箱高（傀儡祖师模型 2.87 格 vs hitbox 2.6），而且还会被品阶倍率
     * （头目 ×1.12）与阶段倍率（×1.18 / ×1.42）放大。之前用 hitbox 高度 → 紫色冠/红色翼
     * 被画在模型**肚子里**，实机看着像"模型裂开/散架"。</p>
     */
    private static float visualHeight(LivingEntity e) {
        float h = e.getBbHeight();
        if (e instanceof CultivationMonsterEntity mob) {
            try {
                // ⚠ 第四轮：先拿**注册基准高度**再乘——getBbHeight() 现在已含巨化
                // （CultivationMonsterEntity.getDimensions 按 min(2.2, giant) 放大），
                // 再乘一次 giant 会把冠/翼/法阵顶到天上去。
                h = mob.getType().getDimensions().height;
                h *= mob.effectiveRank().scaleY();
                float s = mob.bossRenderScale();
                if (s > 1.001F) {
                    h *= s;
                }
                float g = mob.giantScale();
                if (g > 1.001F) {
                    h *= g;
                }
            } catch (Throwable ignored) {
                // 取不到就退回 hitbox 高度
                h = e.getBbHeight();
            }
        }
        return h;
    }

    /** 这只怪是不是"有阶段"的（首领级）。 */
    private static boolean hasPhase(LivingEntity e) {
        if (!(e instanceof CultivationMonsterEntity mob)) {
            return false;
        }
        try {
            return mob.effectiveRank().hasBossBar();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 体型缩放（在 {@code RenderLivingEvent.Pre} 里改 poseStack）。
     *
     * <p>⚠ <b>已停用</b>：体型缩放统一交给 {@link BossPhaseRenderer}（读同步倍率
     * {@code bossRenderScale}，其中已包含转阶段 ×1.18/×1.42 与世界 Boss 入场
     * ×1.55/×1.75）。这里原来还会按**本地血量比例**再乘一次
     * （1.18 / 1.30），两处叠加后二阶段实际 ×1.39、三阶段 ×1.85，
     * 实机里 Boss 会变得过大且和阶段装饰对不上。</p>
     *
     * @return 恒 false（不再 push，调用方无需 pop）
     */
    public static boolean applyScale(LivingEntity e, PoseStack pose) {
        return false;
    }

    /** 上一帧看到的阶段（用来检测"刚刚转阶段"）。 */
    private static final java.util.Map<Integer, Integer> LAST_PHASE =
            new java.util.concurrent.ConcurrentHashMap<>();
    /** 转阶段的起始 tick。 */
    private static final java.util.Map<Integer, Long> TRANSITION =
            new java.util.concurrent.ConcurrentHashMap<>();
    /** 冲击环持续（tick）—— 与服务端的定格时长对齐。 */
    public static final int TRANSITION_TICKS = 20;

    /**
     * PERF(C): 附近 Boss 的扫描间隔（tick）。
     *
     * <p>原来每帧都在做 {@code getEntitiesOfClass(..., inflate(64))} ——
     * 一个 128×128×128 的 AABB 实体查询，140 FPS 下就是每秒 140 次。
     * Boss 阶段装饰会持续几十秒、Boss 移动很慢，5 tick（0.25 秒）刷新一次
     * 在观感上完全看不出延迟，查询次数降到原来的 1/25。</p>
     */
    private static final int SCAN_INTERVAL_TICKS = 5;

    /** PERF(C): 扫描结果缓存（复用同一个 List，不再每帧 new ArrayList）。 */
    private static final List<LivingEntity> CACHED_BOSSES = new ArrayList<>();
    private static long lastScanTick = Long.MIN_VALUE;

    /** 检测刚转阶段的 Boss（本地推导，不需要同步包）。 */
    private static boolean justTransitioned(LivingEntity e, long now) {
        int phase = phaseOfLocal(e);
        Integer last = LAST_PHASE.get(e.getId());
        // PERF(C): 阶段没变时不写 Map —— 原来每帧每只 Boss 都 put 一次
        if (last != null && last == phase) {
            return false;
        }
        LAST_PHASE.put(e.getId(), phase);
        if (last != null && phase > 1) {
            TRANSITION.put(e.getId(), now);
            return true;
        }
        return false;
    }

    /** 已 push 过缩放、等待 pop 的实体 id。 */
    private static final java.util.Set<Integer> SCALED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 体型放大（{@code RenderLivingEvent.Pre}）。
     *
     * <p>⚠ 必须与 Post 的 pop 配对 —— 只 push 不 pop 会让<b>后面所有实体</b>
     * 都被放大，这是渲染里最典型的资源泄漏。</p>
     */
    @SubscribeEvent
    public static void onLivingPre(
            net.minecraftforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
        try {
            if (applyScale(event.getEntity(), event.getPoseStack())) {
                SCALED.add(event.getEntity().getId());
            }
        } catch (Throwable ignored) {
            // 缩放失败按原体型画
        }
    }

    /** 体型恢复（与 Pre 的 push 配对）。 */
    @SubscribeEvent
    public static void onLivingPost(
            net.minecraftforge.client.event.RenderLivingEvent.Post<?, ?> event) {
        try {
            if (SCALED.remove(event.getEntity().getId())) {
                event.getPoseStack().popPose();
            }
        } catch (Throwable ignored) {
            // 同上
        }
    }

    /**
     * 收集视野内需要画形态装饰的 Boss。
     *
     * <p>PERF(C): 结果按 {@value #SCAN_INTERVAL_TICKS} tick 缓存，复用同一个 List；
     * 原来每帧一次 128 格 AABB 查询 + new ArrayList。</p>
     */
    private static List<LivingEntity> nearbyBosses(Minecraft mc) {
        long now = mc.level.getGameTime();
        // 同一 tick 的后续帧、以及 5 tick 内的帧都直接吃缓存（gameTime 回退 = 换世界，重扫）
        // 注意首次扫描时 lastScanTick 是哨兵值，必须显式判断，不能靠差值（会溢出）
        if (lastScanTick != Long.MIN_VALUE
                && now >= lastScanTick && now - lastScanTick < SCAN_INTERVAL_TICKS) {
            return CACHED_BOSSES;
        }
        lastScanTick = now;
        CACHED_BOSSES.clear();
        var box = mc.player.getBoundingBox().inflate(64.0D);
        for (var e : mc.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!e.isAlive() || !hasPhase(e)) {
                continue;
            }
            if (phaseOfLocal(e) <= 1) {
                continue;
            }
            CACHED_BOSSES.add(e);
        }
        // 顺手清掉已经不在列表里的实体的阶段/转场缓存（原版逻辑只在渲染时清理，会随刷怪缓慢堆积）
        java.util.Set<Integer> alive = new java.util.HashSet<>();
        for (LivingEntity e : CACHED_BOSSES) {
            alive.add(e.getId());
        }
        LAST_PHASE.keySet().removeIf(id -> !alive.contains(id));
        TRANSITION.keySet().removeIf(id -> !alive.contains(id));
        return CACHED_BOSSES;
    }

    /** 挂到 {@code RenderLevelStageEvent(AFTER_PARTICLES)}。 */
    @SubscribeEvent
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
        var list = nearbyBosses(mc);
        if (list.isEmpty()) {
            return;
        }
        try {
            render(event.getPoseStack(), event.getCamera().getPosition(),
                    event.getPartialTick(), list);
        } catch (Throwable ignored) {
            // 渲染失败绝不能崩客户端
        }
    }

    private static void render(PoseStack pose, Vec3 cam, float partial,
                               List<LivingEntity> bosses) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float time = (mc.level.getGameTime() + partial) / 20.0F;

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        boolean any = false;
        for (LivingEntity e : bosses) {
            // PERF(C): 名单是 5 tick 前的缓存，这里跳过已死/已移除的实体
            if (!e.isAlive() || e.isRemoved()) {
                continue;
            }
            int phase = phaseOfLocal(e);
            long now = mc.level.getGameTime();
            justTransitioned(e, now);
            // 转阶段冲击环：一圈快速扩散的白环（"发生了什么"的信号）
            Long t0 = TRANSITION.get(e.getId());
            if (t0 != null) {
                float k = (now - t0 + partial) / (float) TRANSITION_TICKS;
                if (k >= 0.0F && k <= 1.0F) {
                    any |= drawShockRing(pose, buffers, e, k, partial);
                } else {
                    TRANSITION.remove(e.getId());
                }
            }
            any |= drawCrown(pose, buffers, e, phase, time, partial);
            if (phase >= 3) {
                any |= drawWings(pose, buffers, e, time, partial);
                // 2026-11-07 玩家要求：去掉脚下那层紫色堕落法阵。
                // drawFloorRune(...) 仍保留在类里，以后要恢复只把这一行放回来。
            }
        }
        pose.popPose();
        if (any) {
            buffers.endBatch(RenderType.lightning());
        }
    }

    /** 转阶段的扩散冲击环（白色，快速外扩并淡出）。 */
    private static boolean drawShockRing(PoseStack pose, MultiBufferSource buffers,
                                         LivingEntity e, float k, float partial) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        double x = Mth.lerp(partial, e.xOld, e.getX());
        double y = Mth.lerp(partial, e.yOld, e.getY());
        double z = Mth.lerp(partial, e.zOld, e.getZ());
        float h = visualHeight(e);
        // 半径从 0.6 倍身高扩到 4 倍
        double radius = h * (0.6D + 3.4D * k);
        float alpha = 0.75F * (1.0F - k);
        if (alpha <= 0.01F) {
            return false;
        }
        float r = 1.0F;
        float g = 0.95F;
        float b = 0.90F;
        double cy = y + h * 0.45D;
        var m = pose.last().pose();
        int seg = 20;
        for (int i = 0; i < seg; i++) {
            double a0 = 2.0D * Math.PI * i / seg;
            double a1 = 2.0D * Math.PI * (i + 1) / seg;
            float x0 = (float) (x + Math.cos(a0) * radius);
            float z0 = (float) (z + Math.sin(a0) * radius);
            float x1 = (float) (x + Math.cos(a1) * radius);
            float z1 = (float) (z + Math.sin(a1) * radius);
            // 竖直方向一点厚度，避免从侧面看是一条线
            float thick = (float) (h * 0.10D);
            vc.vertex(m, x0, (float) cy - thick, z0).color(r, g, b, alpha * 0.3F)
                    .endVertex();
            vc.vertex(m, x1, (float) cy - thick, z1).color(r, g, b, alpha * 0.3F)
                    .endVertex();
            vc.vertex(m, x1, (float) cy + thick, z1).color(r, g, b, alpha).endVertex();
            vc.vertex(m, x0, (float) cy + thick, z0).color(r, g, b, alpha).endVertex();
        }
        return true;
    }

    /** 头顶六片魔角光环（缓慢旋转）。 */
    private static boolean drawCrown(PoseStack pose, MultiBufferSource buffers,
                                     LivingEntity e, int phase, float time,
                                     float partial) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        double x = Mth.lerp(partial, e.xOld, e.getX());
        double y = Mth.lerp(partial, e.yOld, e.getY());
        double z = Mth.lerp(partial, e.zOld, e.getZ());
        float h = visualHeight(e);
        double cy = y + h * (phase >= 3 ? 1.02D : 0.94D);
        double radius = h * (phase >= 3 ? 0.42D : 0.34D);
        // 二阶段偏紫，三阶段偏血红
        float r = phase >= 3 ? 0.95F : 0.62F;
        float g = phase >= 3 ? 0.16F : 0.22F;
        float b = phase >= 3 ? 0.14F : 0.95F;
        float alpha = 0.55F;
        var m = pose.last().pose();
        double spin = time * 0.9D;
        for (int i = 0; i < CROWN_SHARDS; i++) {
            double a0 = spin + 2.0D * Math.PI * i / CROWN_SHARDS;
            double a1 = a0 + 0.28D;
            // 每片是一个"向上收窄的三角带"，看起来像角
            float x0 = (float) (x + Math.cos(a0) * radius);
            float z0 = (float) (z + Math.sin(a0) * radius);
            float x1 = (float) (x + Math.cos(a1) * radius);
            float z1 = (float) (z + Math.sin(a1) * radius);
            float tipY = (float) (cy + h * 0.30D);
            vc.vertex(m, x0, (float) cy, z0).color(r, g, b, alpha).endVertex();
            vc.vertex(m, x1, (float) cy, z1).color(r, g, b, alpha).endVertex();
            vc.vertex(m, (float) x, tipY, (float) z).color(r, g, b, alpha * 0.25F)
                    .endVertex();
            vc.vertex(m, (float) x, tipY, (float) z).color(r, g, b, alpha * 0.25F)
                    .endVertex();
        }
        return true;
    }

    /** 背后两片能量翼（第三阶段）。 */
    private static boolean drawWings(PoseStack pose, MultiBufferSource buffers,
                                     LivingEntity e, float time, float partial) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        double x = Mth.lerp(partial, e.xOld, e.getX());
        double y = Mth.lerp(partial, e.yOld, e.getY());
        double z = Mth.lerp(partial, e.zOld, e.getZ());
        float yaw = Mth.lerp(partial, e.yRotO, e.getYRot());
        double yawRad = Math.toRadians(yaw);
        float h = visualHeight(e);
        // 扇动的呼吸感
        float flap = 0.20F * Mth.sin(time * 3.0F);
        float r = 0.85F;
        float g = 0.12F;
        float b = 0.12F;
        float alpha = 0.42F;
        var m = pose.last().pose();
        for (int side = -1; side <= 1; side += 2) {
            // 翼根在背后偏上
            double bx = x - Math.sin(yawRad) * h * 0.18D;
            double bz = z + Math.cos(yawRad) * h * 0.18D;
            double by = y + h * 0.72D;
            // 翼展方向：垂直于朝向的左右
            double sx = Math.cos(yawRad) * side;
            double sz = -Math.sin(yawRad) * side;
            float tipX = (float) (bx + sx * h * 1.05D);
            float tipZ = (float) (bz + sz * h * 1.05D);
            float tipY = (float) (by + h * (0.34D + flap));
            vc.vertex(m, (float) bx, (float) by, (float) bz)
                    .color(r, g, b, alpha).endVertex();
            vc.vertex(m, (float) bx, (float) (by - h * 0.22D), (float) bz)
                    .color(r, g, b, alpha * 0.6F).endVertex();
            vc.vertex(m, tipX, tipY, tipZ).color(r, g, b, alpha * 0.20F).endVertex();
            vc.vertex(m, tipX, tipY, tipZ).color(r, g, b, alpha * 0.20F).endVertex();
        }
        return true;
    }

    /** 脚下堕落法阵（第三阶段，随血量加速旋转）。 */
    private static boolean drawFloorRune(PoseStack pose, MultiBufferSource buffers,
                                         LivingEntity e, float time, float partial) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        double x = Mth.lerp(partial, e.xOld, e.getX());
        double y = Mth.lerp(partial, e.yOld, e.getY());
        double z = Mth.lerp(partial, e.zOld, e.getZ());
        float h = visualHeight(e);
        double radius = h * 0.95D;
        float r = 0.72F;
        float g = 0.10F;
        float b = 0.10F;
        float alpha = 0.30F;
        var m = pose.last().pose();
        double spin = -time * 0.6D;
        int seg = 24;
        for (int i = 0; i < seg; i++) {
            double a0 = spin + 2.0D * Math.PI * i / seg;
            double a1 = spin + 2.0D * Math.PI * (i + 1) / seg;
            float x0 = (float) (x + Math.cos(a0) * radius);
            float z0 = (float) (z + Math.sin(a0) * radius);
            float x1 = (float) (x + Math.cos(a1) * radius);
            float z1 = (float) (z + Math.sin(a1) * radius);
            float inner = (float) (radius * 0.82D);
            float ix0 = (float) (x + Math.cos(a0) * inner);
            float iz0 = (float) (z + Math.sin(a0) * inner);
            float ix1 = (float) (x + Math.cos(a1) * inner);
            float iz1 = (float) (z + Math.sin(a1) * inner);
            vc.vertex(m, ix0, (float) y + 0.03F, iz0).color(r, g, b, alpha * 0.3F)
                    .endVertex();
            vc.vertex(m, ix1, (float) y + 0.03F, iz1).color(r, g, b, alpha * 0.3F)
                    .endVertex();
            vc.vertex(m, x1, (float) y + 0.03F, z1).color(r, g, b, alpha).endVertex();
            vc.vertex(m, x0, (float) y + 0.03F, z0).color(r, g, b, alpha).endVertex();
        }
        return true;
    }
}
