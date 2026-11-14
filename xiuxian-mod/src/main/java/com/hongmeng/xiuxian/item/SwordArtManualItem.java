package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.talent.SwordArt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * <b>御剑术秘籍</b>：解锁「飞剑剑阵」的一次性道具。
 *
 * <h2>为什么要它</h2>
 * <p>玩家问：「剑阵为什么开局就自带呢，是否设置一个获取方式？」</p>
 *
 * <p>原来的门槛只有"境界 ≥ 第 2 境"，等于**第二次突破就白送** ——
 * 剑阵是本作最有存在感的战斗系统之一，白送会让人不觉得它是一个"东西"。
 * 现在多一道手续：先得到这本秘籍，右键研读，剑阵才永久解锁。</p>
 *
 * <h2>为什么是"永久解锁"而不是"消耗品"</h2>
 * <p>剑阵不是一次性法术，是战斗方式。消耗品会让玩家每次打之前先吃一本，
 * 那不是"获取"，是"税"。所以研读一次、永久生效，状态存在角色数据里。</p>
 */
public class SwordArtManualItem extends Item {

    public SwordArtManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                  InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) {
            // 客户端只回 SUCCESS，避免手在空中"抖一下"
            return InteractionResultHolder.success(stack);
        }
        CultivationData data = CultivationCapability.get(sp);
        if (data == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (SwordArt.unlocked(data)) {
            sp.displayClientMessage(Component.literal(
                    "\u00a77你已经参透过御剑术了。"), true);
            return InteractionResultHolder.pass(stack);
        }
        SwordArt.unlock(data);
        if (!sp.getAbilities().instabuild) {
            stack.shrink(1);
        }
        sp.displayClientMessage(Component.literal(
                "\u00a7b\u00a7l御剑术 · 成"), true);
        sp.displayClientMessage(Component.literal(
                "\u00a77飞剑随心而动。按 \u00a7eN \u00a77展开剑阵，"
                        + "\u00a7eM \u00a77切换阵型。"), false);
        level.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.8F, 1.4F);
        // 解锁状态要立刻同步给客户端（否则剑阵提示还是"未解锁"）
        com.hongmeng.xiuxian.network.ModNetworkBridge.sync(sp);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77右键研读：永久解锁 §b飞剑剑阵"));
        tooltip.add(Component.literal("\u00a78解锁后按 N 展开 / 收回，M 切换阵型"));
        tooltip.add(Component.literal("\u00a78（剑阵本身仍需第 2 境以上）")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
