package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.ManaTable;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
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
 * 回灵丹：立刻回复<b>灵力上限的 30%</b>。
 *
 * <p>改过一次：以前它其实是"回血丹"（+8 点生命），跟名字完全对不上 ——
 * 名字叫「回灵」却在回血，玩家买它治不了御剑的空蓝。
 * 现在它按规格回灵力：{@link ManaTable#PILL_RESTORE_FRACTION} = 30%。</p>
 *
 * <ul>
 *   <li>金丹 12 000 上限 → 一颗回 3 600（御剑 30 分钟变 3 分钟，够救急）</li>
 *   <li>满灵力时<b>不消耗</b>丹药，直接提示，避免浪费（10 灵叶一颗呢）</li>
 * </ul>
 *
 * <p><b>品质</b>（玩家要的"品质要铺开"）：回灵丹和修为丹共用同一套六档品质阶梯，
 * 回复量 = 基础回复 × 品质倍率（凡品 ×1 → 神品 ×20）。
 * 炼丹掷出的品质写在物品 NBT 上，所以"神品回灵丹一口回满"是真的。
 * 天生品质是凡品（×1），老存档与商店买到的回灵丹<b>回复量一个数字都没变</b>。</p>
 *
 * <p>纯数据 + 服务端结算：客户端只在 tooltip 里算个数字给玩家看，
 * 真正的加减在 {@link #use} 里由服务端做。</p>
 */
public class ManaPillItem extends Item {

    public ManaPillItem(Properties properties) {
        super(properties);
    }

    /** 这一颗回灵丹的有效品质（NBT → 凡品）。 */
    public com.hongmeng.xiuxian.quality.Quality effectiveQuality(ItemStack stack) {
        return com.hongmeng.xiuxian.quality.ItemQuality.of(stack);
    }

    /**
     * 回复量 = {@link ManaTable#pillRestore(int)} × 品质倍率。
     *
     * <p>上限仍然是灵力上限本身（{@code addMana} 会夹住），
     * 所以神品只是"一口回满"，不会把上限顶破。</p>
     */
    public int restoreFor(ItemStack stack, int tier) {
        double base = ManaTable.pillRestore(tier);
        double scaled = base * effectiveQuality(stack).effectMultiplier();
        return (int) Math.max(1.0D, Math.round(scaled));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        CultivationData data = CultivationCapability.get(serverPlayer);
        int max = data.maxMana();
        int cur = data.getMana();
        if (cur >= max) {
            serverPlayer.displayClientMessage(
                    Component.literal("\u00a77灵力已满，无需回灵丹。"), true);
            return InteractionResultHolder.fail(stack);
        }

        com.hongmeng.xiuxian.quality.Quality effective = effectiveQuality(stack);
        int restore = restoreFor(stack, data.realm().tier());
        data.addMana(restore);
        stack.shrink(1);

        serverPlayer.displayClientMessage(Component.literal(
                "\u00a7b灵力 +" + Math.min(restore, max - cur)
                        + "\u00a77（" + data.getMana() + " / " + max + "\u00a77）"
                        + (effective.rank() > 0 ? "\u00a78 " + effective.colored() : "")), true);
        // 立刻把灵力推给客户端，蓝条不用等下一个 10 tick 的同步
        com.hongmeng.xiuxian.network.ManaSyncPacket.send(serverPlayer, data.getMana());
        // 其它面板（背包里的数值）也顺手刷新一次
        ModNetworkBridge.sync(serverPlayer);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        com.hongmeng.xiuxian.quality.Quality effective = effectiveQuality(stack);
        tooltip.add(Component.literal("\u00a77品质：" + effective.colored()
                + "  \u00a77回灵 " + effective.multiplierText()
                + (com.hongmeng.xiuxian.quality.ItemQuality.isRolled(stack)
                ? "  \u00a78(掷出)" : "")));
        tooltip.add(Component.literal("\u00a77立刻回复 \u00a7b"
                + Math.round(30 * effective.effectMultiplier()) + "%\u00a77 灵力上限"
                + (effective.rank() > 0
                ? "\u00a78（基础 30% × " + effective.multiplierText() + "）" : "")));
        int tier = clientTier();
        if (tier > 0) {
            // 把百分比换算成玩家当前境界的具体数字，买之前就知道值不值
            tooltip.add(Component.literal("\u00a78当前境界回 \u00a7f"
                    + restoreFor(stack, tier) + "\u00a78 点灵力"));
        }
        tooltip.add(Component.literal("\u00a78灵力已满时不会消耗"));
    }

    /**
     * tooltip 里的大境序号。
     *
     * <p>客户端读自己缓存的那份数据；服务端（或缓存还没同步过来）读不到就返回 0，
     * 只少一行说明文字，不影响功能。</p>
     */
    private static int clientTier() {
        try {
            return com.hongmeng.xiuxian.network.ClientCultivationCache.get().realm().tier();
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
