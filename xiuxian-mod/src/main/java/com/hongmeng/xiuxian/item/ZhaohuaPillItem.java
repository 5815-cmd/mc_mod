package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 造化丹：原文开局那个"服下就觉醒灵根"的东西。
 * 这里没觉醒灵根的玩家可以用它直接觉醒，已觉醒的则换成修为 + 灵叶。
 */
public class ZhaohuaPillItem extends Item {

    public ZhaohuaPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CultivationData data = CultivationCapability.get(serverPlayer);
            if (!data.isAwakened()) {
                com.hongmeng.xiuxian.core.Awakening.awaken(serverPlayer, data, "造化丹");
                stack.shrink(1);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            CultivationSource.PILL.grant(serverPlayer, 120.0D, false);
            ItemStack leaf = new ItemStack(ModItems.LINGYE.get(), 2);
            if (!serverPlayer.getInventory().add(leaf)) {
                serverPlayer.drop(leaf, false);
            }
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7a造化丹化开，灵气沁入四肢百骸。灵叶 +2"));
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77未觉醒者：直接觉醒灵根"));
        tooltip.add(Component.literal("\u00a77已觉醒者：修为 +120，灵叶 +2"));
        tooltip.add(Component.literal("\u00a78" + "\u201c这东西，一颗就够改命。\u201d"));
    }
}
