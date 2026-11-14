package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.network.SyncCultivationPacket;
import com.hongmeng.xiuxian.talent.Path;
import com.hongmeng.xiuxian.talent.Talents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * <b>洗忆丹</b> —— 洗去天赋，把点数全部退回。
 *
 * <h2>为什么需要这个物品</h2>
 * <p>之前天赋点是<b>一次性</b>的：点错了就没有任何补救，只能重开档。
 * 这是体验上最危险的一个缺口 —— 玩家前面反复强调"要平衡、要有选择"，
 * 而"选错就废档"会让玩家<b>不敢选</b>，选择的意义反而消失。</p>
 *
 * <h2>为什么做成物品而不是免费命令</h2>
 * <ul>
 *   <li><b>免费洗点</b>会让"点数有限所以要取舍"这个前提失效 ——
 *       玩家可以随便试，反正免费退；</li>
 *   <li><b>物品</b>让洗点成为一个<b>有成本的决策</b>：
 *       值得洗，但不该天天洗。</li>
 * </ul>
 *
 * <h2>⚠ 天赋点是怎么退的</h2>
 * <p>点数<b>不是存成一个"余额"</b>，而是"应得总数 − 已花费"反推出来的
 * （见 {@code Talents} 的类注释）。所以"洗点"只需要<b>把所有节点等级清零</b>，
 * 点数就自动全额回来了 —— 不需要额外记账，也不可能出现"负数"或"白送"。</p>
 *
 * <h2>⚠ 道途本身不洗</h2>
 * <p>这个丹只洗<b>天赋点</b>，<b>不改道途</b>。转修需要另一件东西
 * （{@code PathSwitchItem}）—— 两者的代价应该不同：
 * 调一调加点很轻，换一条道是大事。</p>
 */
public class TalentResetPillItem extends Item {

    public TalentResetPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                 InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        com.hongmeng.xiuxian.talent.Career career =
                Talents.pathOf(serverPlayer);
        if (career == null) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a77尚未择业，无忆可洗。"));
            return InteractionResultHolder.fail(stack);
        }
        int spent = Talents.spentPoints(serverPlayer);
        if (spent <= 0) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a77你还没点过天赋，不必服此丹。"));
            return InteractionResultHolder.fail(stack);
        }

        // 洗点：把所有节点等级清零。
        // 点数不单独记账，所以清零之后"剩余点数"自动等于"应得总数"。
        Talents.clearRanks(serverPlayer);
        stack.shrink(1);
        SyncCultivationPacket.send(serverPlayer,
                com.hongmeng.xiuxian.core.CultivationCapability.get(serverPlayer));

        serverPlayer.sendSystemMessage(Component.literal(""));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a75\u00a7l═══ 灵 台 涤 尘 ═══"));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a77丹力流转，你脑中那些刚成形的念头被一只无形的手抹平了。"));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7f天赋点 \u00a7e" + spent + " \u00a7f点已尽数退回。"
                        + "\u00a77（职业未变）"));
        com.hongmeng.xiuxian.technique.PathMechanicFx.talentReset(serverPlayer);
        try {
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.3F);
        } catch (Throwable ignored) {
            // 音效失败不影响功能
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("\u00a77服下后 \u00a7e天赋点全部退回\u00a77。"));
        tooltip.add(Component.literal("\u00a78职业不变 —— 换职业要用别的办法。"));
    }
}
