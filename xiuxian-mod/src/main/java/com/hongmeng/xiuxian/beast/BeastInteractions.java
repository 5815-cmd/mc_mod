package com.hongmeng.xiuxian.beast;

import com.hongmeng.xiuxian.entity.beast.BeastManager;
import com.hongmeng.xiuxian.entity.beast.SpiritBeastEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 灵兽交互的<b>事件入口</b>。
 *
 * <h3>为什么需要这一层</h3>
 *
 * <p>三种灵兽道具有三种完全不同的"右键"语义：收服（妖兽）、收纳（灵兽）、
 * 装备（护甲）。它们都发生在 {@code Player#interactOn} 的第一阶段，也就是
 * {@code Item#interactLivingEntity} 之前——所以物品类的覆写<b>不够用</b>：
 * 一旦实体那边的 {@code mobInteract} 先返回了结果，物品就再也没机会了。</p>
 *
 * <p>{@link PlayerInteractEvent.EntityInteract} 正好卡在这个位置：
 * 它在 {@code interactOn} 里、在 {@code interactAt} 与 {@code mobInteract}
 * 之前触发，而且可以取消。用它把"灵兽系统认领的交互"截下来，
 * 剩下的（原版行为、别的模组）一律放行。</p>
 *
 * <p>这不是在绕开既有机制，而是补上 Forge 给的那个标准钩子——
 * 本模组 {@code ModEvents} 里已经在用同一族事件（{@code RightClickItem}）。</p>
 */
public final class BeastInteractions {

    /** 公开构造：Forge 事件总线要求实例注册（见 HongmengMod 的 forgeBus.register）。 */
    public BeastInteractions() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity target = event.getTarget() instanceof LivingEntity living ? living : null;
        if (target == null || target == player) {
            return;
        }
        ItemStack stack = event.getItemStack();

        // 1) 灵兽（已契约）：护甲 / 妖丹 / 袋子
        if (target instanceof SpiritBeastEntity beast) {
            if (!beast.isOwnedBy(player)) {
                return;   // 别人的灵兽不接管，交给原版（会走 PASS）
            }
            if (BeastManager.isBeastArmor(stack)) {
                if (BeastManager.toggleArmor(player, beast, stack)) {
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
                }
                return;
            }
            if (BeastManager.isGrowthItem(stack)) {
                if (BeastManager.feedGrowth(player, beast, stack)) {
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
                }
                return;
            }
            if (BeastManager.isPouch(stack)) {
                BeastManager.storeBeast(stack, beast, player);
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
                return;
            }
            // 其余（潜行循环指令 / 空手骑乘）交给 SpiritBeastEntity.mobInteract
            return;
        }

        // 2) 野生妖兽：契灵符收服
        if (BeastManager.isTalisman(stack)
                && com.hongmeng.xiuxian.entity.beast.BeastManager
                .contractTargetOf(target) != null) {
            if (BeastManager.tryContract(player, target, stack)) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
            }
        }
    }
}
