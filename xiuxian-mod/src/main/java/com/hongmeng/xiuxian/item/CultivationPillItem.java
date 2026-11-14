package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.core.CultivationSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 直接补修为的丹药。原文里丹药是正经的修为来源之一，不是打坐的替代品而是补充。
 */
public class CultivationPillItem extends Item {

    private final double amount;
    private final String flavor;

    public CultivationPillItem(Properties properties, double amount, String flavor) {
        super(properties);
        this.amount = amount;
        this.flavor = flavor;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(serverPlayer);
            if (!data.isAwakened()) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "\u00a7c你连灵根都没觉醒，吞下去只会肚子疼。"));
                return InteractionResultHolder.fail(stack);
            }
            if (data.isMaxRealm()) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "\u00a7c已至鸿蒙绝巅，再多的丹药也是浪费。"));
                return InteractionResultHolder.fail(stack);
            }
            double mult = HongmengConfig.SERVER.cultivationMultiplier.get();
            CultivationSource.PILL.grant(serverPlayer, amount * mult, false);
            serverPlayer.sendSystemMessage(Component.literal("\u00a77" + flavor));
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.EAT;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77服下后获得修为 \u00a7a+" + (long) amount));
        tooltip.add(Component.literal("\u00a78" + flavor));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 24;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return super.finishUsingItem(stack, level, entity);
    }
}
