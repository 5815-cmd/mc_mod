package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.domain.DomainData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 双道宫丹。
 *
 * <p>道印可以凝第二座道宫的钥匙。来源：化神试炼（道印之前的那个副本）通关奖励。</p>
 *
 * <p>注意：只是**解锁资格**，第二座道宫仍然要自己攒能量去凝。</p>
 */
public class DualDomainPillItem extends Item {

    public DualDomainPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CultivationData data = CultivationCapability.get(serverPlayer);
            DomainData domains = data.getDomainData();

            if (!data.isAwakened()) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "\u00a7c灵根未觉醒，服下去只会烧穿经脉。"));
                return InteractionResultHolder.fail(stack);
            }
            if (domains.isDualUnlocked()) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "\u00a77你的灵海早已开过一次——双道宫之资已在。"));
                return InteractionResultHolder.fail(stack);
            }

            domains.setDualUnlocked(true);
            com.hongmeng.xiuxian.network.ModNetworkBridge.sync(serverPlayer);

            serverPlayer.sendSystemMessage(Component.literal(""));
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a75\u00a7l═══ 灵 海 扩 容 ═══"));
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a77丹药化开的一瞬间，你的灵海像被人从内侧撑开了一道缝。"));
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7f从此可以容纳 \u00a7e两座道宫\u00a7f。"));
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a78（第二座仍需攒够能量自己凝。按 G 打开道宫界面。）"));
            serverPlayer.sendSystemMessage(Component.literal(""));

            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77解锁第二座道宫（双道宫之资）"));
        tooltip.add(Component.literal("\u00a78来源：化神试炼"));
        tooltip.add(Component.literal("\u00a78" + "\u201c两座道宫，两种法则。\u201d"));
    }
}
