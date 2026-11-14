package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.EvilGazerEntity;
import com.hongmeng.xiuxian.realworld.EvilSky;
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
 * <b>天空裂缝 + 巨眼</b>（第五轮升级版 / 实机修复 task-40）。
 *
 * <h2>实机问题</h2>
 * <p>旧版在视线前方 220 格画一块 <b>halfW×halfH 的填充矩形</b>当"裂口"，实机看就是
 * 天空里一块巨大的半透明红方块，完全不像"天空被撕开"。本轮彻底改为<b>竖直锯齿撕裂带</b>。</p>
 *
 * <h2>裂缝结构（只画"裂缝条"，绝不画整块矩形）</h2>
 * <ol>
 *   <li><b>中心脊柱</b>：从上到下的锯齿折线（每段随机折点，越往下越窄收束到"裂缝口"）；</li>
 *   <li><b>暗黑核心</b>：沿脊柱的窄裂缝（整宽约 1.5~4 格，越往上越宽）；</li>
 *   <li><b>暗红血肉内壁</b>：脊柱两侧到锯齿外缘之间用<b>梯形条</b>填充（不是规则四边形），
 *       每段随机色彩/透明度 + 撕裂肉瓣；</li>
 *   <li><b>亮红外缘</b>：沿锯齿外缘 3 层加法发光（亮红→血红→暗紫外晕）+ 少量滴落触须；</li>
 *   <li><b>巨眼</b>：中上部一只（比旧版小），瞳孔看向玩家、会眨眼、前摇收缩/判定爆闪；</li>
 *   <li><b>血红天壳</b>：世界固定的顶部+四壁渐变暗带（保留），提供氛围。</li>
 * </ol>
 *
 * <h2>锚定到恶（不再固定相机前方）</h2>
 * <pre>
 *   找 128 格内最近的 EvilGazerEntity（跳过小眼爪牙）
 *   锚点 base = 恶位置 + (0, 模型顶部高度(按阶段渲染倍率算) + RIFT_BOTTOM_GAP, 0)
 *   裂缝从 base 向上生长：半宽 16~28 格、半高 22~38 格（随阶段与开幕 open 增长）
 *   billboard 面向相机（right/up 由 base→相机方向构造），但位置跟着恶走
 * </pre>
 * <p>裂缝下缘只比恶模型顶部高 {@link #RIFT_BOTTOM_GAP} 格，所以看起来<b>手臂/身体从裂缝里伸出来</b>。</p>
 * <p>模型顶部像素来自模型组交付的 `round5/evil-model/模型数据.md`（Arm minY=-54 / Head -92 /
 * Upper -148），再乘 {@link EvilGazerRenderer#stageScaleOf(int)}（1.35/1.15/1.05）；模型本轮不改。</p>
 *
 * <p>找不到恶时回退"视线前方 220 格"的 billboard，但同样<b>只画裂缝条</b>（尺寸 ×1.8 补偿距离）。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EvilSkyRenderer {

    /** 天壳半径（格）。 */
    private static final float SHELL_R = 260.0F;
    /** 找不到恶时的回退 billboard 距离（格）。 */
    private static final float PLANE_D = 220.0F;
    /** 回退距离下的尺寸补偿（否则 220 格外只剩一条细线）。 */
    private static final float FALLBACK_SIZE_BOOST = 1.8F;

    // ------------------------------------------------------------------
    // 裂缝锚定/尺寸常量（契约 §1/§2）
    // ------------------------------------------------------------------
    /** 找恶的半径（格）。 */
    private static final double RIFT_SEARCH_RANGE = 128.0D;
    /** 裂缝下缘距恶模型顶部的间隙（格）。 */
    private static final float RIFT_BOTTOM_GAP = 1.0F;
    /** 满开时裂缝半宽/半高的阶段范围（阶段 1 → 阶段 3）。 */
    private static final float RIFT_HALF_W_MIN = 16.0F;
    private static final float RIFT_HALF_W_MAX = 28.0F;
    private static final float RIFT_HALF_H_MIN = 22.0F;
    private static final float RIFT_HALF_H_MAX = 38.0F;
    /** 裂缝竖向分段数（锯齿折点数）。 */
    private static final int RIFT_SEGMENTS = 16;

    /**
     * 模型"锚点上方最高点"的像素值（minY 取反；来自模型组 `模型数据.md`，本轮模型不改）。
     * Arm y[-54..157] / Head y[-92..95] / Upper y[-148..102]。
     */
    private static final float[] MODEL_TOP_PX = {54.0F, 92.0F, 148.0F};
    private static final float PX_PER_BLOCK = 16.0F;

    /** 天空首次出现时记录的游戏时间（驱动裂口张开/眨眼）。 */
    private static long firstOpenTime = Long.MIN_VALUE;

    /**
     * PERF(C): "最近的恶"扫描缓存。
     *
     * <p>天空裂缝要跟着恶走，原来每帧都遍历 {@code entitiesForRendering()}
     * 找最近的一只（恶战场景里实体不少）。恶的位置变化很慢，这里按 4 tick
     * 节流刷新；命中缓存时还会校验那只恶是否还活着（死了立刻重扫）。</p>
     */
    private static EvilGazerEntity cachedEvil;
    private static long lastEvilScanTick = Long.MIN_VALUE;
    private static final int EVIL_SCAN_INTERVAL = 4;

    /** 当前恶的攻击状态（由 {@link BossPresenceClient} 每 tick 写入；0=idle/1=windup/2=active/3=recover）。 */
    private static volatile byte bossState = 0;
    /** 当前恶的招式 id（同上；0=无）。 */
    private static volatile byte bossAttack = 0;
    /** 平滑后的"前摇蓄力"0~1（眼睛变亮/瞳孔收缩）。 */
    private static float windupGlow = 0.0F;
    /** 平滑后的"判定闪光"0~1。 */
    private static float activeFlash = 0.0F;

    private static final double[] SIN = new double[33];
    private static final double[] COS = new double[33];

    static {
        for (int i = 0; i <= 32; i++) {
            double a = Math.PI * 2.0D * i / 32.0D;
            SIN[i] = Math.sin(a);
            COS[i] = Math.cos(a);
        }
    }

    private EvilSkyRenderer() {
    }

    /** 供 {@link BossPresenceClient} 每 tick 调用：把恶的起手/判定状态喂给天空巨眼。 */
    public static void setBossAttackState(byte state, byte attack) {
        bossState = state;
        bossAttack = attack;
    }

    /** 重置（恶离场 / 天空恢复时）。 */
    public static void reset() {
        bossState = 0;
        bossAttack = 0;
        windupGlow = 0.0F;
        activeFlash = 0.0F;
        firstOpenTime = Long.MIN_VALUE;
        // PERF(C): 一并清掉"最近的恶"缓存，下次重扫
        cachedEvil = null;
        lastEvilScanTick = Long.MIN_VALUE;
    }

    // ------------------------------------------------------------------
    // 入口
    // ------------------------------------------------------------------

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onStage(
            net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event
                .RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        float s = EvilSky.strength();
        if (s <= 0.01F) {
            if (firstOpenTime != Long.MIN_VALUE) {
                reset();
            }
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        try {
            render(event.getPoseStack(), event.getCamera().getPosition(),
                    event.getPartialTick(), s, mc);
        } catch (Throwable ignored) {
            // 渲染失败不能崩客户端
        }
    }

    // ------------------------------------------------------------------
    // 主渲染
    // ------------------------------------------------------------------

    private static void render(PoseStack pose, Vec3 cam, float partial, float s,
                               Minecraft mc) {
        long gt = mc.level.getGameTime();
        if (firstOpenTime == Long.MIN_VALUE) {
            firstOpenTime = gt;
        }
        float openAge = (gt - firstOpenTime) + partial;
        float t = (gt + partial) / 20.0F;
        float open = Mth.clamp(openAge / 60.0F, 0.0F, 1.0F);

        // 平滑攻击状态（服务端只会给 0/1/2/3，客户端按帧缓动）
        float windTarget = bossState == 1 ? 1.0F : 0.0F;
        float flashTarget = bossState == 2 ? 1.0F : 0.0F;
        windupGlow += (windTarget - windupGlow) * 0.20F;
        activeFlash += (flashTarget - activeFlash) * 0.35F;

        Vec3 eye = mc.player.getEyePosition(partial);
        Vec3 look = mc.player.getViewVector(partial);
        if (look.lengthSqr() < 1.0E-4D) {
            return;
        }
        look = look.normalize();

        // ============ ① 找恶，算裂缝锚点与尺寸 ============
        EvilGazerEntity evil = findNearestEvil(mc, eye);
        int riftPhase;
        float halfW;
        float halfH;
        Vec3 base;
        Vec3 right;
        Vec3 up;
        if (evil != null) {
            riftPhase = EvilGazerRenderer.phaseOf(evil);
            float top = modelTopAboveAnchor(riftPhase);
            // 锚点 = 恶位置 + (0, 模型顶部高度 + RIFT_BOTTOM_GAP, 0)
            base = new Vec3(evil.getX(), evil.getY() + top + RIFT_BOTTOM_GAP, evil.getZ());
            Vec3 fwd = base.subtract(eye);
            if (fwd.lengthSqr() < 1.0E-4D) {
                return;
            }
            fwd = fwd.normalize();
            right = new Vec3(0.0D, 1.0D, 0.0D).cross(fwd);
            if (right.lengthSqr() < 1.0E-4D) {
                right = new Vec3(1.0D, 0.0D, 0.0D);
            }
            right = right.normalize();
            up = fwd.cross(right).normalize();
            float phaseK = Mth.clamp((riftPhase - 1) / 2.0F, 0.0F, 1.0F);
            // 尺寸随阶段线性增长；入场 open 0→1 时从"一线缝"张开
            halfW = Mth.lerp(open, 1.2F,
                    Mth.lerp(phaseK, RIFT_HALF_W_MIN, RIFT_HALF_W_MAX));
            halfH = Mth.lerp(open, 1.5F,
                    Mth.lerp(phaseK, RIFT_HALF_H_MIN, RIFT_HALF_H_MAX));
        } else {
            // ============ 回退：视线前方 220 格 billboard（同样只画裂缝条）============
            riftPhase = Mth.clamp(EvilSky.phase(), 1, 3);
            Vec3 origin = eye.add(look.scale(PLANE_D));
            Vec3 fwd = eye.subtract(origin).normalize();       // 朝向相机的方向
            right = new Vec3(0.0D, 1.0D, 0.0D).cross(fwd);
            if (right.lengthSqr() < 1.0E-4D) {
                right = new Vec3(1.0D, 0.0D, 0.0D);
            }
            right = right.normalize();
            up = fwd.cross(right).normalize();
            float phaseK = Mth.clamp((riftPhase - 1) / 2.0F, 0.0F, 1.0F);
            halfW = Mth.lerp(open, 1.2F,
                    Mth.lerp(phaseK, RIFT_HALF_W_MIN, RIFT_HALF_W_MAX))
                    * FALLBACK_SIZE_BOOST;
            halfH = Mth.lerp(open, 1.5F,
                    Mth.lerp(phaseK, RIFT_HALF_H_MIN, RIFT_HALF_H_MAX))
                    * FALLBACK_SIZE_BOOST;
            base = origin.add(up.scale(-halfH));               // origin 是中心，base 是下缘
        }

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        var m = pose.last().pose();

        // ============ ② 暗部：血红天壳 + 裂缝血肉 ============
        // 绝不画整块矩形：只画"裂缝条"（task-40 实机修复）
        VertexConsumer dark = buf.getBuffer(RenderType.debugQuads());
        bloodShell(dark, m, eye, s, riftPhase, t);
        riftDark(dark, m, base, right, up, halfW, halfH, riftPhase, open, t);
        buf.endBatch(RenderType.debugQuads());

        // ============ ③ 亮部：亮红外缘 + 滴落触须 + 巨眼 ============
        VertexConsumer glow = buf.getBuffer(RenderType.lightning());
        riftGlow(glow, m, base, right, up, halfW, halfH, open, t);
        float blinkK = blinkAmount(openAge);
        riftEye(glow, m, base, right, up, halfW, halfH, eye, t,
                blinkK, windupGlow, activeFlash);
        pose.popPose();
        buf.endBatch(RenderType.lightning());
    }

    /** 找 128 格内最近的恶（跳过小眼爪牙；找不到返回 null）。 */
    private static EvilGazerEntity findNearestEvil(Minecraft mc, Vec3 eye) {
        // PERF(C): 按 4 tick 节流：原来每帧全实体遍历一次
        long now = mc.level.getGameTime();
        if (cachedEvil != null
                && (!cachedEvil.isAlive() || cachedEvil.isRemoved())) {
            cachedEvil = null;
            lastEvilScanTick = Long.MIN_VALUE;   // 目标死了立刻重扫
        }
        if (lastEvilScanTick != Long.MIN_VALUE
                && now >= lastEvilScanTick && now - lastEvilScanTick < EVIL_SCAN_INTERVAL) {
            return cachedEvil;
        }
        lastEvilScanTick = now;
        EvilGazerEntity best = null;
        double bestD = RIFT_SEARCH_RANGE * RIFT_SEARCH_RANGE;
        try {
            for (var e : mc.level.entitiesForRendering()) {
                if (!(e instanceof EvilGazerEntity eg) || !eg.isAlive()
                        || eg.isInvisible()) {
                    continue;
                }
                if (EvilGazerRenderer.isMinion(eg)) {
                    continue;                       // 小眼不画天空裂缝
                }
                double d = eg.position().distanceToSqr(eye);
                if (d < bestD) {
                    bestD = d;
                    best = eg;
                }
            }
        } catch (Throwable ignored) {
            // 取不到就当没有恶
        }
        cachedEvil = best;
        return best;
    }

    /** 恶模型顶部在锚点上方的世界高度（按阶段渲染倍率算）。 */
    public static float modelTopAboveAnchor(int phase) {
        int i = Mth.clamp(phase, 1, 3) - 1;
        return MODEL_TOP_PX[i] / PX_PER_BLOCK * EvilGazerRenderer.stageScaleOf(phase);
    }

    // ------------------------------------------------------------------
    // ① 血红天壳（保留：世界固定的顶部 + 四壁渐变暗带）
    // ------------------------------------------------------------------
    private static void bloodShell(VertexConsumer vc, org.joml.Matrix4f m, Vec3 eye,
                                   float s, int phase, float t) {
        float pulse = 0.90F + 0.10F * Mth.sin(t * 0.9F);
        float aBase = (0.16F + 0.26F * s) * pulse;
        vc.vertex(m, (float) eye.x - SHELL_R, (float) eye.y + SHELL_R, (float) eye.z - SHELL_R)
                .color(0.35F * s, 0.02F, 0.025F, aBase).endVertex();
        vc.vertex(m, (float) eye.x + SHELL_R, (float) eye.y + SHELL_R, (float) eye.z - SHELL_R)
                .color(0.35F * s, 0.02F, 0.025F, aBase).endVertex();
        vc.vertex(m, (float) eye.x + SHELL_R, (float) eye.y + SHELL_R, (float) eye.z + SHELL_R)
                .color(0.35F * s, 0.02F, 0.025F, aBase).endVertex();
        vc.vertex(m, (float) eye.x - SHELL_R, (float) eye.y + SHELL_R, (float) eye.z + SHELL_R)
                .color(0.35F * s, 0.02F, 0.025F, aBase).endVertex();
        for (int wall = 0; wall < 4; wall++) {
            double dx = (wall == 0) ? -SHELL_R : (wall == 1) ? SHELL_R : 0.0D;
            double dz = (wall == 2) ? -SHELL_R : (wall == 3) ? SHELL_R : 0.0D;
            for (int band = 0; band < 3; band++) {
                float y0 = (float) eye.y - 30.0F + band * 90.0F;
                float y1 = y0 + 90.0F;
                float a = aBase * (1.0F - band * 0.22F);
                float r = 0.42F * s + 0.05F * (phase - 1);
                if (wall == 0 || wall == 1) {
                    float z0 = (float) (eye.z - SHELL_R);
                    float z1 = (float) (eye.z + SHELL_R);
                    quad(vc, m, (float) eye.x + (float) dx, y0, z0,
                            (float) eye.x + (float) dx, y0, z1,
                            (float) eye.x + (float) dx, y1, z1,
                            (float) eye.x + (float) dx, y1, z0, r, 0.02F, 0.03F, a);
                } else {
                    float x0 = (float) (eye.x - SHELL_R);
                    float x1 = (float) (eye.x + SHELL_R);
                    quad(vc, m, x0, y0, (float) eye.z + (float) dz,
                            x1, y0, (float) eye.z + (float) dz,
                            x1, y1, (float) eye.z + (float) dz,
                            x0, y1, (float) eye.z + (float) dz, r, 0.02F, 0.03F, a);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // ② 裂缝暗部：竖直锯齿撕裂带（脊柱/血肉内壁/暗黑核心/撕裂肉瓣）
    // ------------------------------------------------------------------

    /**
     * 竖直撕裂带：<b>绝不画 halfW×halfH 整块矩形</b>。
     *
     * <p>每段（y0→y1）画：左血肉梯形条、右血肉梯形条、暗黑核心条（沿锯齿脊柱），
     * 外加随机"撕裂肉瓣"（伸出外缘的三角巾）。所有边都带随机折点，整体从下往上张开
     * （底部收束到"裂缝口"）。</p>
     */
    private static void riftDark(VertexConsumer vc, org.joml.Matrix4f m,
                                 Vec3 base, Vec3 right, Vec3 up,
                                 float halfW, float halfH, int phase,
                                 float open, float t) {
        float h = halfH * 2.0F;
        float alphaK = 0.30F + 0.70F * Mth.clamp(open, 0.0F, 1.0F);
        float pulse = 0.90F + 0.10F * Mth.sin(t * 1.6F);
        float phaseK = Mth.clamp((phase - 1) / 2.0F, 0.0F, 1.0F);
        for (int i = 0; i < RIFT_SEGMENTS; i++) {
            int i2 = i + 1;
            float y0 = h * i / RIFT_SEGMENTS;
            float y1 = h * i2 / RIFT_SEGMENTS;
            // 锯齿脊柱（每段折点随机；相邻段共享端点 → 折线连续）
            float sx0 = spineX(i, halfW, phaseK);
            float sx1 = spineX(i2, halfW, phaseK);
            // 外缘宽度（越往上越宽；底部收束到裂缝口）
            float env0 = envHalfWidth(y0, h, halfW, i);
            float env1 = envHalfWidth(y1, h, halfW, i2);
            // 暗黑核心半宽（整宽约 1.5~4 格，越往上略宽）
            float core0 = coreHalfWidth(y0, h, phaseK);
            float core1 = coreHalfWidth(y1, h, phaseK);
            float l0 = sx0 - env0;
            float l1 = sx1 - env1;
            float r0 = sx0 + env0;
            float r1 = sx1 + env1;
            float cl0 = sx0 - core0;
            float cl1 = sx1 - core1;
            float cr0 = sx0 + core0;
            float cr1 = sx1 + core1;
            // 左/右血肉内壁（梯形条，颜色每段随机 → 不规则）
            float red = 0.20F + 0.12F * (float) hash(i, 3, 9);
            float gg = 0.02F + 0.03F * (float) hash(i, 5, 11);
            float bb = 0.03F + 0.03F * (float) hash(i, 7, 13);
            float a = (0.52F + 0.30F * (float) hash(i, 11, 17)) * alphaK * pulse;
            quadP(vc, m, base, right, up, l0, y0, cl0, y0, cl1, y1, l1, y1,
                    red, gg, bb, a);                              // 左内壁
            quadP(vc, m, base, right, up, cr0, y0, r0, y0, r1, y1, cr1, y1,
                    red, gg, bb, a);                              // 右内壁
            // 内壁靠近核心的一圈稍亮的"伤口边缘"
            quadP(vc, m, base, right, up, cl0, y0, sx0, y0, sx1, y1, cl1, y1,
                    red + 0.10F, gg + 0.02F, bb + 0.02F, a * 0.55F);
            quadP(vc, m, base, right, up, sx0, y0, cr0, y0, cr1, y1, sx1, y1,
                    red + 0.10F, gg + 0.02F, bb + 0.02F, a * 0.55F);
            // 暗黑核心（裂缝本体）
            quadP(vc, m, base, right, up, cl0, y0, cr0, y0, cr1, y1, cl1, y1,
                    0.012F, 0.0F, 0.006F, 0.96F);
        }
        // 撕裂肉瓣：随机从外缘伸出的棱角（三角巾），让边缘"毛糙"
        for (int i = 1; i < RIFT_SEGMENTS; i += 2) {
            float y0 = h * i / RIFT_SEGMENTS;
            float y1 = h * (i + 1) / RIFT_SEGMENTS;
            float sx = spineX(i, halfW, phaseK);
            float env = envHalfWidth(y0, h, halfW, i);
            int side = hash(i, 13, 19) > 0.5D ? 1 : -1;
            float xE = sx + side * env;
            float spike = (0.22F + 0.55F * (float) hash(i, 17, 23)) * halfW * 0.18F;
            float my = (y0 + y1) * 0.5F;
            float a = (0.45F + 0.30F * (float) hash(i, 19, 29)) * alphaK;
            // 两个退化四边形拼成三角巾（底边贴外缘，尖端向外伸出）
            quadP(vc, m, base, right, up, xE, y0, xE, y1, xE + side * spike, my, xE, my,
                    0.26F, 0.03F, 0.04F, a);
            quadP(vc, m, base, right, up, xE, my, xE + side * spike, my, xE, y1, xE, y1,
                    0.26F, 0.03F, 0.04F, a);
        }
    }

    /** 脊柱中心 x 偏移（锯齿折点；每段随机但幅度随高度增大）。 */
    private static float spineX(int seg, float halfW, float phaseK) {
        float tSeg = seg / (float) RIFT_SEGMENTS;
        double r = hash(seg, 31, 7) * 2.0D - 1.0D;
        return (float) r * halfW * (0.10F + 0.16F * tSeg) * (1.0F + 0.15F * phaseK);
    }

    /** 外缘半宽：底部收束到裂缝口，越往上越宽（锯齿随机抖动）。 */
    private static float envHalfWidth(float y, float h, float halfW, int seg) {
        float tSeg = Mth.clamp(y / Math.max(1.0F, h), 0.0F, 1.0F);
        float taper = 0.16F + 0.84F * (float) Math.pow(tSeg, 0.75D);
        float jitter = 0.82F + 0.36F * (float) hash(seg, 37, 41);
        return halfW * taper * jitter;
    }

    /** 暗黑核心半宽（整宽约 1.5~4 格，越往上越宽）。 */
    private static float coreHalfWidth(float y, float h, float phaseK) {
        float tSeg = Mth.clamp(y / Math.max(1.0F, h), 0.0F, 1.0F);
        return Mth.lerp(0.75F, 2.0F, tSeg) * (1.0F + 0.20F * phaseK);
    }

    // ------------------------------------------------------------------
    // ③ 裂缝亮部：亮红外缘 3 层 + 滴落触须
    // ------------------------------------------------------------------

    /** 亮红外缘：沿锯齿外缘 3 层加法发光 + 随机滴落触须（"血浆/触须"感）。 */
    private static void riftGlow(VertexConsumer vc, org.joml.Matrix4f m,
                                 Vec3 base, Vec3 right, Vec3 up,
                                 float halfW, float halfH, float open, float t) {
        float h = halfH * 2.0F;
        float alphaK = 0.25F + 0.75F * Mth.clamp(open, 0.0F, 1.0F);
        float flick = 0.68F + 0.32F * Mth.sin(t * 7.3F) * Mth.sin(t * 2.1F);
        for (int i = 0; i < RIFT_SEGMENTS; i++) {
            int i2 = i + 1;
            float y0 = h * i / RIFT_SEGMENTS;
            float y1 = h * i2 / RIFT_SEGMENTS;
            float sx0 = spineX(i, halfW, 0.0F);
            float sx1 = spineX(i2, halfW, 0.0F);
            float e0 = envHalfWidth(y0, h, halfW, i);
            float e1 = envHalfWidth(y1, h, halfW, i2);
            for (int side = -1; side <= 1; side += 2) {
                float xa = sx0 + side * e0;
                float xb = sx1 + side * e1;
                // 内层：亮红
                quadP(vc, m, base, right, up, xa, y0, xb, y1,
                        xb + side * 0.55F, y1, xa + side * 0.55F, y0,
                        1.00F, 0.28F, 0.18F, 0.85F * alphaK * flick);
                // 中层：血红
                quadP(vc, m, base, right, up, xa, y0, xb, y1,
                        xb + side * 1.55F, y1, xa + side * 1.55F, y0,
                        1.00F, 0.10F, 0.10F, 0.40F * alphaK * flick);
                // 外层：暗紫外晕
                quadP(vc, m, base, right, up, xa, y0, xb, y1,
                        xb + side * 3.20F, y1, xa + side * 3.20F, y0,
                        0.85F, 0.04F, 0.10F, 0.16F * alphaK);
            }
            // 滴落触须：从外缘某点向下垂一小段（随机、低密度）
            if (hash(i, 43, 47) > 0.62D) {
                int side = hash(i, 53, 59) > 0.5D ? 1 : -1;
                float xE = sx0 + side * e0;
                float drip = (0.5F + 1.8F * (float) hash(i, 61, 67)) * halfH * 0.14F;
                float w = 0.22F + 0.25F * (float) hash(i, 71, 73);
                float bx = xE + side * 0.6F;
                quadP(vc, m, base, right, up, bx - w, y0, bx + w, y0,
                        bx + w * 0.5F, y0 - drip, bx - w * 0.5F, y0 - drip,
                        0.95F, 0.16F, 0.12F, 0.55F * alphaK);
            }
        }
    }

    // ------------------------------------------------------------------
    // ④ 巨眼（裂缝中上部，缩小；瞳孔看向玩家 + 眨眼 + 前摇/判定）
    // ------------------------------------------------------------------
    private static void riftEye(VertexConsumer vc, org.joml.Matrix4f m,
                                Vec3 base, Vec3 right, Vec3 up,
                                float halfW, float halfH, Vec3 playerEye, float t,
                                float blinkK, float windup, float active) {
        float h = halfH * 2.0F;
        float cy = h * 0.66F;                              // 中上部
        float cx = spineX(12, halfW, 0.0F) * 0.5F;         // 大致贴着脊柱
        float er = halfW * 0.16F;                          // 比旧版小
        if (er < 1.2F) {
            return;
        }
        float erY = er * 0.72F * (1.0F - 0.88F * blinkK);
        if (erY < 0.6F) {
            return;
        }
        Vec3 c = base.add(right.scale(cx)).add(up.scale(cy));
        // 瞳孔看向玩家：眼中心 → 玩家视点 的方向，投影到裂缝平面基
        Vec3 toPlayer = playerEye.subtract(c);
        float dx = (float) toPlayer.dot(right);
        float dy = (float) toPlayer.dot(up);
        float len = Math.max(0.001F, (float) Math.sqrt(dx * dx + dy * dy));
        float lookK = Mth.clamp(1.0F - len / 160.0F, 0.0F, 1.0F);
        float pupilX = Mth.clamp(dx / len, -1.0F, 1.0F) * er * 0.30F * lookK;
        float pupilY = Mth.clamp(dy / len, -1.0F, 1.0F) * erY * 0.30F * lookK;
        float pupilK = 0.34F - 0.10F * windup;
        float glow = 0.55F + 0.35F * windup + 0.25F * active;
        // 眼白（暗红）
        ellipse(vc, m, c, right, up, 0.0F, 0.0F, er, erY,
                0.55F, 0.04F, 0.05F, 0.62F * glow, 28);
        // 虹膜（亮红）
        ellipse(vc, m, c, right, up, 0.0F, 0.0F, er * 0.62F, erY * 0.62F,
                1.00F, 0.18F, 0.12F, 0.55F * glow, 22);
        // 瞳孔（近黑，看向玩家）
        ellipse(vc, m, c, right, up, pupilX, pupilY, er * pupilK, erY * pupilK,
                0.02F, 0.0F, 0.01F, 0.98F, 18);
        // 金圈 + 高光
        ellipseRing(vc, m, c, right, up, pupilX, pupilY,
                er * pupilK * 1.25F, erY * pupilK * 1.25F,
                1.00F, 0.50F, 0.10F, 0.32F * glow, 18);
        ellipse(vc, m, c, right, up, -er * 0.30F, erY * 0.26F,
                er * 0.16F, erY * 0.16F, 1.00F, 0.92F, 0.80F, 0.55F * glow, 10);
        // 判定爆闪
        if (active > 0.02F) {
            ellipseRing(vc, m, c, right, up, 0.0F, 0.0F, er * 1.35F, erY * 1.35F,
                    1.00F, 0.30F, 0.20F, 0.30F * active, 28);
        }
        // 眨眼眼睑
        if (blinkK > 0.25F) {
            float lidY = erY * 1.05F;
            float lidA = 0.85F * blinkK;
            quadP(vc, m, c, right, up,
                    -er * 1.15F, lidY * (1.0F - 2.0F * blinkK),
                    er * 1.15F, lidY * (1.0F - 2.0F * blinkK),
                    er * 1.15F, lidY, -er * 1.15F, lidY,
                    0.10F, 0.01F, 0.02F, lidA);
            quadP(vc, m, c, right, up,
                    -er * 1.15F, -lidY, er * 1.15F, -lidY,
                    er * 1.15F, -lidY * (1.0F - 2.0F * blinkK),
                    -er * 1.15F, -lidY * (1.0F - 2.0F * blinkK),
                    0.10F, 0.01F, 0.02F, lidA);
        }
    }

    // ------------------------------------------------------------------
    // 眨眼 / 工具
    // ------------------------------------------------------------------

    /** 每 ~92t 眨一次（开合 7t），返回"闭合程度"0~1。 */
    private static float blinkAmount(float openAge) {
        float c = openAge % 92.0F;
        if (c >= 7.0F) {
            return 0.0F;
        }
        return (float) Math.sin(Math.PI * (c + 0.4F) / 7.4F);
    }

    private static double hash(int a, int b, int salt) {
        double v = Math.sin(a * 12.9898D + b * 78.233D + salt * 37.719D) * 43758.5453D;
        return v - Math.floor(v);
    }

    /** 平面四边形（用裂缝平面基向量定位；origin/right/up + 平面内 (x,y)）。 */
    private static void quadP(VertexConsumer vc, org.joml.Matrix4f m,
                              Vec3 o, Vec3 r, Vec3 u,
                              float x0, float y0, float x1, float y1,
                              float x2, float y2, float x3, float y3,
                              float red, float g, float b, float a) {
        put(vc, m, o, r, u, x0, y0, red, g, b, a);
        put(vc, m, o, r, u, x1, y1, red, g, b, a);
        put(vc, m, o, r, u, x2, y2, red, g, b, a);
        put(vc, m, o, r, u, x3, y3, red, g, b, a);
    }

    /** 椭圆（三角扇，每片用退化四边形表示）。 */
    private static void ellipse(VertexConsumer vc, org.joml.Matrix4f m,
                                Vec3 o, Vec3 r, Vec3 u,
                                float cx, float cy, float rx, float ry,
                                float red, float g, float b, float a, int seg) {
        int n = Math.min(seg, 32);
        for (int i = 0; i < n; i++) {
            int i2 = (i + 1) % n;
            float x0 = cx + (float) (COS[i] * rx);
            float y0 = cy + (float) (SIN[i] * ry);
            float x1 = cx + (float) (COS[i2] * rx);
            float y1 = cy + (float) (SIN[i2] * ry);
            put(vc, m, o, r, u, cx, cy, red, g, b, a);
            put(vc, m, o, r, u, x0, y0, red, g, b, a);
            put(vc, m, o, r, u, x1, y1, red, g, b, a);
            put(vc, m, o, r, u, cx, cy, red, g, b, a);
        }
    }

    /** 椭圆环。 */
    private static void ellipseRing(VertexConsumer vc, org.joml.Matrix4f m,
                                    Vec3 o, Vec3 r, Vec3 u,
                                    float cx, float cy, float rx, float ry,
                                    float red, float g, float b, float a, int seg) {
        int n = Math.min(seg, 32);
        float rx2 = rx * 0.88F;
        float ry2 = ry * 0.88F;
        for (int i = 0; i < n; i++) {
            int i2 = (i + 1) % n;
            float x0 = cx + (float) (COS[i] * rx);
            float y0 = cy + (float) (SIN[i] * ry);
            float x1 = cx + (float) (COS[i2] * rx);
            float y1 = cy + (float) (SIN[i2] * ry);
            float ix0 = cx + (float) (COS[i] * rx2);
            float iy0 = cy + (float) (SIN[i] * ry2);
            float ix1 = cx + (float) (COS[i2] * rx2);
            float iy1 = cy + (float) (SIN[i2] * ry2);
            put(vc, m, o, r, u, ix0, iy0, red, g, b, a);
            put(vc, m, o, r, u, ix1, iy1, red, g, b, a);
            put(vc, m, o, r, u, x1, y1, red, g, b, a);
            put(vc, m, o, r, u, x0, y0, red, g, b, a);
        }
    }

    private static void put(VertexConsumer vc, org.joml.Matrix4f m,
                            Vec3 o, Vec3 r, Vec3 u,
                            float x, float y, float red, float g, float b, float a) {
        vc.vertex(m,
                        (float) (o.x + r.x * x + u.x * y),
                        (float) (o.y + r.y * x + u.y * y),
                        (float) (o.z + r.z * x + u.z * y))
                .color(red, g, b, a).endVertex();
    }

    /** 世界坐标四边形（天壳用）。 */
    private static void quad(VertexConsumer vc, org.joml.Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float r, float g, float b, float a) {
        vc.vertex(m, x0, y0, z0).color(r, g, b, a).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, a).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, a).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, a).endVertex();
    }
}
