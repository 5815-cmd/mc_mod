package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 攻击动作的<b>渲染钩子</b>（第三人称整体姿态 + 第一人称手部）。
 *
 * <h2>为什么单独一个类</h2>
 * <p>{@link CombatAnimClient} 只存状态、算姿态；<b>注册到事件总线的必须是渲染侧</b>，
 * 而且必须 {@code @OnlyIn(CLIENT)} —— 混在一起容易在服务端被误加载
 * （{@code PoseStack} / {@code RenderHandEvent} 都是纯客户端类）。</p>
 *
 * <h2>Push / Pop 必须配对</h2>
 * <p>{@code RenderPlayerEvent.Pre} 里 push、{@code Post} 里 pop。用一个
 * {@code Set<UUID>} 记录"这一帧给谁 push 过"，避免 pop 多余导致
 * <b>后续所有渲染的坐标系都错位</b>（表现是整个世界渲染歪掉）。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CombatAnimRenderer {

    /** 本帧已经 push 过姿势的玩家（Post 里要配对 pop）。 */
    private static final Set<UUID> PUSHED = new HashSet<>();

    private CombatAnimRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        if (pose == null) {
            return;
        }

        if (CombatAnimClient.applyBody(player, pose, event.getPartialTick())) {
            PUSHED.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        if (PUSHED.remove(player.getUUID())) {
            event.getPoseStack().popPose();
        }
    }

    /** 第一人称：只动主手那把武器。 */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        if (pose == null) {
            return;
        }
        if (CombatAnimClient.applyHand(pose, event.getPartialTick())) {
            // 同一个事件里 push 完必须当场 pop —— 手部渲染是同步完成的
            pose.popPose();
        }
    }

    /**
     * 每帧画剑气拖尾。
     *
     * <p>放在 {@code AFTER_PARTICLES}：粒子之后画，拖尾会盖在粒子上面，
     * 观感更"亮"（加色混合本来就越叠越亮）。</p>
     */
    @SubscribeEvent
    public static void onRenderLevel(net.minecraftforge.client.event
                                             .RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event
                .RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        SlashTrailRenderer.render(event.getPoseStack(),
                event.getCamera().getPosition(),
                event.getPartialTick());
    }



    /** 本地连击计数（第一人称用；与服务端的计数各自独立，只求手感一致）。 */
    private static int localCombo = 0;
    private static long localLastTick = -9999L;

    /**
     * <b>自己按左键 → 立刻开始本地动作</b>。
     *
     * <p>为什么不等服务端的广播：网络回包有 1~2 tick 延迟，
     * 第一人称挥砍会明显"慢半拍"。自己这一下完全可以在本地先演，
     * 服务端的包只用来让<b>别人</b>看到你的动作。</p>
     */
    @SubscribeEvent
    public static void onInteraction(net.minecraftforge.client.event
                                             .InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        localCombo = (now - localLastTick <= 20L) ? ((localCombo + 1) % 3) : 0;
        localLastTick = now;
        CombatAnimClient.onLocalAttack(kindOf(mc.player.getMainHandItem()), localCombo);
    }

    /** 客户端也判一次武器分型（与服务端 {@code CombatAnim.kindOf} 规则一致）。 */
    private static int kindOf(net.minecraft.world.item.ItemStack held) {
        if (held == null || held.isEmpty()) {
            return CombatAnimClient.KIND_SWORD;
        }
        try {
            if (held.getItem() instanceof com.hongmeng.xiuxian.item.gear.ModPathWeapon
                    .PathWeaponItem pw) {
                return switch (pw.weapon()) {
                    case TI -> CombatAnimClient.KIND_HEAVY;
                    case MO -> CombatAnimClient.KIND_STAB;
                    case XUE -> CombatAnimClient.KIND_TWIN;
                    case ZHEN -> CombatAnimClient.KIND_RING;
                    case FA -> CombatAnimClient.KIND_SWORD;
                };
            }
        } catch (Throwable ignored) {
            // 判不出来按剑
        }
        return CombatAnimClient.KIND_SWORD;
    }
}
