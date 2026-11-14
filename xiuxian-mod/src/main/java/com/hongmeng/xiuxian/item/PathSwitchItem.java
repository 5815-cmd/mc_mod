package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.network.SyncCultivationPacket;
import com.hongmeng.xiuxian.talent.Path;
import com.hongmeng.xiuxian.talent.Talents;
import com.hongmeng.xiuxian.talent.TalentTree;
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
 * <b>转道丹</b> —— 洗去旧道，可重新择一条道途。
 *
 * <h2>为什么这件东西必须存在</h2>
 * <p>之前道途是<b>永久</b>的（{@code Talents.choose} 里明确拒绝重选）。
 * 而道途决定的是<b>整条成长曲线</b>：法术/生命/灵力的乘数、
 * 六节点天赋树、专属护甲与武器、机制（激荡/魔气/血池/阵域）。</p>
 *
 * <p>永久锁定意味着玩家<b>在信息最少的时候做一个不可撤销的决定</b> ——
 * 这会让多数人不敢选，选择的意义反而消失。玩家反复强调"要平衡、要有侧重"，
 * 而"侧重"只有在<b>能反悔</b>时才敢追求。</p>
 *
 * <h2>⚠ 转修的代价：天赋点清零（但可再赚）</h2>
 * <p>转修时会把<b>已点天赋全部清空</b>（点数退回）。所以代价是
 * "重新分配"，不是"损失点数" —— 这与玩家"不要代价"的要求一致。</p>
 *
 * <h2>⚠ 转修后护甲/武器怎么办</h2>
 * <p><b>不回收</b>（那是玩家辛苦做的东西），但它们<b>不再给加成</b> ——
 * 因为护甲/武器的加成只对<b>本道途</b>生效（见 {@code PathArmorBonus}）。
 * 于是"转修"之后那套装备自然变成收藏品，玩家会去做新道途的那套。
 * 这比"一键回收"更宽容：万一反悔转回来，装备还在。</p>
 */
public class PathSwitchItem extends Item {

    public PathSwitchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                 InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        com.hongmeng.xiuxian.talent.Career old =
                com.hongmeng.xiuxian.talent.Talents.pathOf(serverPlayer);
        if (old == null) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a77尚未择业，直接选就是了，不必服丹。"));
            return InteractionResultHolder.fail(stack);
        }

        // 转职：清天赋 + 清职业
        int cleared = Talents.clearRanks(serverPlayer);
        Talents.setCareerAt(serverPlayer.getUUID(), null);
        stack.shrink(1);
        SyncCultivationPacket.send(serverPlayer,
                com.hongmeng.xiuxian.core.CultivationCapability.get(serverPlayer));

        serverPlayer.sendSystemMessage(Component.literal(""));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a75\u00a7l═══ 破 道 重 立 ═══"));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a77你亲手打碎了那身 \u00a7f" + old.cn()
                        + "\u00a77 的根基。痛，但清醒。"));
        if (cleared > 0) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7f已点天赋尽数化去，\u00a7e" + cleared
                            + " \u00a7f点天赋点已退回。"));
        }
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7f现在可以重新择业：\u00a7f/hongmeng path <"
                        + "剑修|血修|魔修|鬼修|体修>"));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a78（旧道途装备不会消失，但暂时吃不到新职业加成）"));
        com.hongmeng.xiuxian.technique.PathMechanicFx.pathSwitch(serverPlayer);
        try {
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.7F);
        } catch (Throwable ignored) {
            // 音效失败不影响功能
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("\u00a77服下后可 \u00a7e重新择道\u00a77，"
                + "已点天赋全部退回。"));
        tooltip.add(Component.literal("\u00a78旧道途的装备不会消失，但暂时不给加成。"));
    }
}
