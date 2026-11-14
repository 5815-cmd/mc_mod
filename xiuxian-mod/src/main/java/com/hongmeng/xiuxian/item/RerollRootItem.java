package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import com.hongmeng.xiuxian.spirit.SpiritRoot;
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
 * 洗灵丹：重抽灵根。保底玄品以上，不会把人洗废。
 */
public class RerollRootItem extends Item {

    public RerollRootItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CultivationData data = CultivationCapability.get(serverPlayer);
            SpiritRoot old = data.getRoot();
            SpiritRoot fresh = SpiritRoot.rollGuaranteed();
            data.setRoot(fresh);
            data.addReroll();
            ModNetworkBridge.sync(serverPlayer);

            serverPlayer.sendSystemMessage(Component.literal(""));
            serverPlayer.sendSystemMessage(Component.literal("\u00a7d洗灵丹药力冲刷经脉……"));
            if (old != null) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "\u00a78旧：" + old.rarity().colored() + " \u00a7f" + old.cn()));
            }
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7f新：" + fresh.rarity().colored() + " \u00a7f\u00a7l" + fresh.cn()));
            serverPlayer.sendSystemMessage(Component.literal("\u00a77" + fresh.desc()));
            serverPlayer.sendSystemMessage(Component.literal(""));
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77重抽一次灵根，保底 \u00a7b玄品\u00a77 以上"));
        tooltip.add(Component.literal("\u00a78" + "\u201c这号还能救一救。\u201d"));
    }
}
