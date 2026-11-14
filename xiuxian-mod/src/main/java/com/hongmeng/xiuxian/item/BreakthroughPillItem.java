package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.core.Breakthrough;
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
 * 破境丹：服下后一段时间内突破成功率拉满。
 * 丹药能顶一时，顶不了一世——业力该清还得清。
 */
public class BreakthroughPillItem extends Item {

    public static final int DURATION_TICKS = 20 * 60;

    public BreakthroughPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            Breakthrough.grantPillBoost(serverPlayer, DURATION_TICKS);
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7d破境丹药力化开，一炷香之内，你的突破必成。"));
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77服下后 60 秒内突破必定成功"));
        tooltip.add(Component.literal("\u00a78但业力不会因此消失。"));
    }
}
