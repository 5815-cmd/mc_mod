package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>Boss 战环境压迫</b>：靠近 BOSS/OVERLORD 时画面开始"收紧"。
 *
 * <h2>为什么不改方块做场地</h2>
 * <p>「专属场地」如果靠放方块实现，就等于<b>改存档</b> ——
 * 玩家在自己家里刷一只 boss 会毁掉建筑。这个项目一直的原则是
 * <b>演出用渲染解决，不用世界编辑</b>（水领域也从不碰方块）。</p>
 *
 * <h2>三层压迫</h2>
 * <ol>
 *   <li><b>画面暗角</b>：屏幕四边压暗，越近越重 —— 视野被"收窄"；</li>
 *   <li><b>色调偏移</b>：按 boss 类型染红/紫，暗示威胁来源；</li>
 *   <li><b>呼吸脉动</b>：暗角强度缓慢起伏（1.1 Hz），
 *       让压迫感是"活的"，而不是一块静态黑边。</li>
 * </ol>
 *
 * <h2>为什么用渐变条而不是贴图</h2>
 * <p>暗角只需要"从边缘向中心衰减"，用 4 组、每组 6 层的半透明矩形叠加即可，
 * 不需要额外贴图，也不需要半透明全屏 shader。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class BossFightAtmosphere {

    /** 开始有压迫感的距离（格）。 */
    private static final double NEAR = 32.0D;

    /** 满强度距离（格）—— 贴脸时最重。 */
    private static final double CLOSE = 10.0D;

    /** 暗角最大不透明度。 */
    private static final float MAX_DARK = 0.42F;

    /** 每边渐变层数。 */
    private static final int LAYERS = 6;

    /** 平滑后的强度（0~1），避免距离抖动导致闪烁。 */
    private static float smooth = 0.0F;

    /** 当前强度对应色（r,g,b）。 */
    private static float[] tint = {1.0F, 0.25F, 0.22F};

    /**
     * PERF(C): 上一次扫描的 gameTime 与结果。
     *
     * <p>{@code CombatFeelClient} 每 tick 会在 {@code Phase.START} 和 {@code Phase.END}
     * 各转发一次本方法（同一个 gameTime），所以"遍历实体找最近的 Boss"其实每 tick
     * 跑了两遍。这里按 gameTime 去重：扫描每 tick 只做一次，但平滑推进仍按原节奏
     * 每 tick 走两次（观感与改动前完全一致）。</p>
     */
    private static long lastScanTick = Long.MIN_VALUE;
    private static float lastTarget = 0.0F;

    /** PERF(C): 实体类型 → 压迫色（原来每只 Boss 每 tick 都 new float[]{...}）。 */
    private static final java.util.Map<net.minecraft.world.entity.EntityType<?>, float[]>
            COLORS = new java.util.concurrent.ConcurrentHashMap<>();

    private BossFightAtmosphere() {
    }

    /** 每客户端 tick 调一次（由 CombatFeelClient 转发）。 */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            smooth = 0.0F;
            lastScanTick = Long.MIN_VALUE;
            return;
        }
        long now = mc.level.getGameTime();
        // PERF(C): 同一 gameTime 的第二次转发不再重复遍历实体，只重复平滑（保持原观感）
        if (now != lastScanTick) {
            lastScanTick = now;
            lastTarget = scan(mc);
        }
        float target = lastTarget;
        // 平滑：每 tick 靠近 12%（约 0.4 秒到位）
        smooth += (target - smooth) * 0.12F;
        if (smooth < 0.002F) {
            smooth = 0.0F;
        }
    }

    /** 扫出最近的 BOSS/OVERLORD 并算出目标强度（顺带更新 tint）。 */
    private static float scan(Minecraft mc) {
        double best = Double.MAX_VALUE;
        float[] bestColor = null;
        try {
            for (var e : mc.level.entitiesForRendering()) {
                if (!(e instanceof CultivationMonsterEntity cm) || !cm.isAlive()) {
                    continue;
                }
                MobRank r = cm.effectiveRank();
                if (r != MobRank.BOSS && r != MobRank.OVERLORD) {
                    continue;
                }
                double d = e.distanceToSqr(mc.player);
                if (d < best) {
                    best = d;
                    bestColor = colorOf(cm);
                }
            }
        } catch (Throwable ignored) {
            return 0.0F;
        }
        if (bestColor != null && best < NEAR * NEAR) {
            double d = Math.sqrt(best);
            // CLOSE 处 = 1，NEAR 处 = 0
            tint = bestColor;
            return (float) Mth.clamp((NEAR - d) / (NEAR - CLOSE), 0.0D, 1.0D);
        }
        return 0.0F;
    }

    /** 按 boss 类型取色（与压迫感光环同一套语义）。 */
    private static float[] colorOf(LivingEntity e) {
        // PERF(C): 颜色只跟实体类型有关，按类型缓存
        return COLORS.computeIfAbsent(e.getType(), type -> {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(type).getPath();
            if (id.contains("void") || id.contains("dragon")) {
                return new float[]{0.55F, 0.30F, 1.00F};
            }
            if (id.contains("beast") || id.contains("serpent")) {
                return new float[]{1.00F, 0.72F, 0.20F};
            }
            if (id.contains("epoch") || id.contains("puppet")) {
                return new float[]{0.30F, 1.00F, 0.80F};
            }
            return new float[]{1.00F, 0.25F, 0.22F};
        });
    }

    /** 当前强度（给别处用；0 表示不在 boss 战范围）。 */
    public static float strength() {
        return smooth;
    }

    /** 挂到 GUI 渲染后。 */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (smooth <= 0.01F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        // 呼吸脉动：0.75 ~ 1.0
        float pulse = 0.75F + 0.25F * Mth.sin(
                (mc.level == null ? 0.0F : mc.level.getGameTime()
                        + event.getPartialTick()) * 0.35F);
        float base = smooth * MAX_DARK * pulse;
        if (base <= 0.005F) {
            return;
        }
        int r = (int) (tint[0] * 255.0F);
        int gg = (int) (tint[1] * 255.0F);
        int b = (int) (tint[2] * 255.0F);
        // 四边各叠 LAYERS 层，逐层变淡（内浅外深）
        int bandW = Math.max(8, (int) (w * 0.16F * smooth));
        int bandH = Math.max(8, (int) (h * 0.18F * smooth));
        for (int i = 0; i < LAYERS; i++) {
            float k = (LAYERS - i) / (float) LAYERS;          // 外圈更重
            int a = (int) (base * k * 255.0F / LAYERS * 2.0F);
            if (a <= 0) {
                continue;
            }
            int argb = (Math.min(255, a) << 24) | (r << 16) | (gg << 8) | b;
            int dx = bandW * i / LAYERS;
            int dy = bandH * i / LAYERS;
            int thickW = Math.max(1, bandW / LAYERS);
            int thickH = Math.max(1, bandH / LAYERS);
            // 上
            g.fill(dx, dy, w - dx, dy + thickH, argb);
            // 下
            g.fill(dx, h - dy - thickH, w - dx, h - dy, argb);
            // 左
            g.fill(dx, dy, dx + thickW, h - dy, argb);
            // 右
            g.fill(w - dx - thickW, dy, w - dx, h - dy, argb);
        }
    }
}
