package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>怪物/Boss 的挥击动作</b>。
 *
 * <h2>为什么单独一套</h2>
 * <p>玩家的动作（{@code CombatAnimClient}）挂在 {@code RenderPlayerEvent} 上，
 * 而且带有"武器分型 + 段位变体"那套复杂逻辑。
 * 怪物不需要那么细 —— 它们要的是<b>一眼看出"这一下打出来了"</b>。</p>
 *
 * <h2>动作幅度要比玩家大</h2>
 * <p>Boss 体型大，同样 25° 的转体在小体型玩家身上已经很明显，
 * 在 Boss 身上会被稀释。所以这里用 <b>1.6 倍</b> 的幅度。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class MobCombatAnim {

    /** 动作时长（tick）。比玩家略长 —— 大体型动作要"沉"。 */
    public static final int TICKS = 10;

    /** 幅度倍率（相对玩家）。 */
    public static final float AMP = 1.6F;

    private static final Map<Integer, long[]> ANIMS = new ConcurrentHashMap<>();
    private static final java.util.Set<Integer> PUSHED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private MobCombatAnim() {
    }

    /** 收到广播。 */
    public static void onAttack(int entityId, byte kind) {
        ANIMS.put(entityId, new long[]{now(), kind});
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    /** 进度 0~1；不在动作中返回 -1。 */
    private static float progress(int entityId, float partial) {
        long[] a = ANIMS.get(entityId);
        if (a == null) {
            return -1.0F;
        }
        float done = (now() - a[0]) + partial;
        if (done < 0.0F || done > TICKS) {
            ANIMS.remove(entityId);
            return -1.0F;
        }
        return done / (float) TICKS;
    }

    /** 在 {@code RenderLivingEvent.Pre} 里 push。 */
    @SubscribeEvent
    public static void onPre(RenderLivingEvent.Pre<?, ?> event) {
        try {
            LivingEntity e = event.getEntity();
            if (e == null || e instanceof net.minecraft.world.entity.player.Player) {
                return;
            }
            float p = progress(e.getId(), event.getPartialTick());
            if (p < 0.0F) {
                return;
            }
            long[] a = ANIMS.get(e.getId());
            int kind = a == null ? 0 : (int) a[1];
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            // 巨物（「恶」）的动作幅度再放大一倍 —— 体型越大，同样角度越看不出来
            float extra = (e instanceof com.hongmeng.xiuxian.entity.EvilGazerEntity)
                    ? 2.0F : 1.0F;
            PUSHED.add(e.getId());
            // 缓动：快起慢收
            float k = 1.0F - (1.0F - p) * (1.0F - p);
            switch (kind) {
                case 1 -> {   // 重砸：先前倾再下压
                    pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(
                            16.0F * AMP * extra * k));
                    pose.translate(0.0D, -0.10D * k, 0.14D * k);
                }
                case 2 -> {   // 突刺：整个身体前送
                    pose.translate(0.0D, 0.0D, 0.42D * AMP * k);
                    pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(
                            8.0F * AMP * k));
                }
                default -> {  // 横扫：转体
                    pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                            26.0F * AMP * k));
                    pose.translate(-0.12D * k, 0.0D, 0.16D * k);
                }
            }
        } catch (Throwable ignored) {
            // 动作失败按原姿态画
        }
    }

    /** 在 {@code RenderLivingEvent.Post} 里 pop（**必须配对**）。 */
    @SubscribeEvent
    public static void onPost(RenderLivingEvent.Post<?, ?> event) {
        try {
            LivingEntity e = event.getEntity();
            if (e == null) {
                return;
            }
            if (PUSHED.remove(e.getId())) {
                event.getPoseStack().popPose();
            }
        } catch (Throwable ignored) {
            // 同上
        }
    }

    /** 换世界时清空。 */
    public static void clear() {
        ANIMS.clear();
        PUSHED.clear();
    }
}
