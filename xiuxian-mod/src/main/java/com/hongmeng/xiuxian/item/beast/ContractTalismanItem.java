package com.hongmeng.xiuxian.item.beast;

import com.hongmeng.xiuxian.entity.beast.BeastContract;
import com.hongmeng.xiuxian.entity.beast.BeastManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 契灵符 —— 收服妖兽的唯一道具。
 *
 * <p>用法（对齐规格第四节）：把妖兽打到 <b>25% 生命以下</b>，
 * 手持符纸右键它。成功率看境界差与灵根品质；<b>失败照样烧掉符纸并激怒它</b>。</p>
 *
 * <p>刻意<b>不做</b>食物驯服：规格里写得很明白，本模组走"以力服之"的仙侠逻辑，
 * 这样既不用给十种妖兽各造一个野生灵兽版本，也和"修仙界靠拳头说话"的世界观一致。</p>
 *
 * <p>契约逻辑本身放在 {@link BeastManager#tryContract} —— 那里才允许碰注册表，
 * 本类只负责"什么时候调用它"。</p>
 */
public class ContractTalismanItem extends Item {

    public ContractTalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level().isClientSide()) {
            // 客户端只负责"抬手"，结算全在服务端
            return InteractionResult.sidedSuccess(true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (BeastManager.tryContract(serverPlayer, target, stack)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** 对空气右键没有任何效果——符纸只在"对妖兽"时有意义。 */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "\u00a77\u5bf9\u7740\u865a\u5f31\u7684\u5996\u517d\u53f3\u952e\uff0c"
                            + "\u624d\u80fd\u7acb\u4e0b\u5951\u7ea6\u3002"), true);
        }
        return net.minecraft.world.InteractionResultHolder.success(
                player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77\u5c06\u5996\u517d\u6253\u5230 \u00a7c25% \u00a77\u751f\u547d\u4ee5\u4e0b")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("\u00a77\u518d\u6301\u6b64\u7b26\u53f3\u952e\u5b83\uff0c"
                + "\u5373\u53ef\u7acb\u4e0b\u5951\u7ea6\u3002"));
        tooltip.add(Component.literal("\u00a78\u6210\u529f\u7387\uff1a\u540c\u5883 "
                + BeastContract.describeChance(BeastContract.BASE_CHANCE)
                + "\uff0c\u6bcf\u9ad8\u4e00\u5883 +\u00a7a15%"
                + "\u00a78\uff0c\u6bcf\u4f4e\u4e00\u5883 \u00a7c-25%"));
        tooltip.add(Component.literal("\u00a78\u5931\u8d25\u4f1a\u70e7\u6389\u7b26\u7eb8\uff0c"
                + "\u5e76\u6fc0\u6012\u5b83\u3002"));
    }

    /** 符纸不叠太多：一次带一两张就够，逼玩家认真准备。 */
    public static Item.Properties defaultProperties() {
        return new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON);
    }
}
