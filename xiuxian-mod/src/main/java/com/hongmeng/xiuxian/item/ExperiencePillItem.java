package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import com.hongmeng.xiuxian.quality.Quality;
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
 * 经验丹药（修为丹）。
 *
 * <p>三种品质，效果差 10 倍：</p>
 * <pre>
 *   普通修为丹   当前小境界需求的 25%      天生品质：凡品（×1）
 *   完美修为丹   ×3                       天生品质：上品（×3）
 *   仙品修为丹   ×10                      天生品质：仙品（×10）
 * </pre>
 *
 * <p>倍率来自通用的六档品质阶梯（{@link Quality}），丹药只是其中三档的<b>天生</b>档位。
 * 炼丹掷出的高品质会写进物品 NBT（{@code hongmeng_quality}），
 * 于是一颗"神品普通修为丹"能给出 ×20——这是"其他东西也加品质"的落点之一。</p>
 *
 * <p><b>关键设计</b>：给的是"当前小境界所需修为的百分比"，
 * 而不是一个固定数字。固定数字在后期会变得毫无感觉
 * （道印一个小境界要 14.8 万，一颗丹给几百等于没有），
 * 按比例给才能让每一颗丹在任何一个境界都"有感"。</p>
 *
 * <p>境界越高，单颗丹给的绝对值越大；但相对进度不变，不会破坏平衡。</p>
 */
public class ExperiencePillItem extends Item {

    /** 基础比例：当前小境界需求的百分比。 */
    private final double baseRatio;
    private final PillQuality quality;

    public ExperiencePillItem(PillQuality quality, double baseRatio, Properties properties) {
        super(properties);
        this.quality = quality;
        this.baseRatio = baseRatio;
    }

    public PillQuality quality() {
        return quality;
    }

    public double baseRatio() {
        return baseRatio;
    }

    /**
     * 实际给的修为 = 当前小境界需求 × 基础比例 × 品质倍率。
     *
     * <p>这里的品质用物品自带的 {@link PillQuality}（没有 NBT 时的天生档）。</p>
     */
    public long cultivationFor(CultivationData data) {
        return cultivationFor(net.minecraft.world.item.ItemStack.EMPTY, data);
    }

    /**
     * 实际给的修为（按<b>这一颗</b>的品质算）。
     *
     * <p>品质读取顺序：物品 NBT（掷骰子掷出来的）→ 物品自带的三档 → 凡品。
     * 所以炼丹掷出的"神品普通修为丹"（×20）是可能的，而老存档里
     * 没有 NBT 的普通 / 完美 / 仙品修为丹依然是 ×1 / ×3 / ×10，一个数字不变。</p>
     */
    public long cultivationFor(ItemStack stack, CultivationData data) {
        double need;
        try {
            need = data.realm().need();
        } catch (Throwable t) {
            need = 100.0D;
        }
        if (need <= 0.0D) {
            need = 100.0D;
        }
        return (long) Math.max(1.0D,
                Math.round(need * baseRatio * effectiveQuality(stack).effectMultiplier()));
    }

    /** 这一颗丹药的有效品质（NBT → 自带档位）。 */
    public Quality effectiveQuality(ItemStack stack) {
        return com.hongmeng.xiuxian.quality.ItemQuality.of(stack, quality.quality());
    }

    /**
     * 这颗丹在服丹日志 / tooltip 里叫什么名字。
     *
     * <p>默认「修为丹」；灵石系丹药（{@link SpiritPillItem}）覆盖成自己的名字，
     * 免得玩家看到“服下 完美修为丹”却吃的是聚灵丹。</p>
     */
    protected String pillLabel() {
        return "修为丹";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        CultivationData data = CultivationCapability.get(serverPlayer);
        if (!data.isAwakened()) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7c灵根未觉醒，丹药化不开。"));
            return InteractionResultHolder.fail(stack);
        }

        Quality effective = effectiveQuality(stack);
        long gain = cultivationFor(stack, data);
        long need = Math.max(1L, data.realm().need());

        // 2026-11-01 玩家要求：取消“快满/已满拒绝服用”的限制。
        // 仍然受当前小境界上限封顶（CultivationSource.grant 内 Math.min），
        // 但不再拦着玩家吃；实际被截断多少会在下面明确显示。
        var before = data.realm();
        // 用返回值：那才是**实际**进了多少（可能被上限截断）
        long applied = CultivationSource.PILL.grant(serverPlayer, gain, true);
        var after = data.realm();

        serverPlayer.sendSystemMessage(Component.literal(""));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7a服下 " + effective.colored() + "\u00a7a" + pillLabel()));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7f修为 \u00a7e+" + applied + " \u00a77(当前小境界需求 " + need
                        + "，这颗给了 " + Math.round(applied * 100.0D / need) + "%)"));
        if (applied < gain) {
            // 真的发生了截断：明确说清，别让玩家以为系统吞了
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a78（受本层上限限制，实际 +" + applied + "；名义 +" + gain + "）"));
            if (applied <= 0L) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "\u00a77修为已满，药力逸散；先破境再服更有效。"));
            }
        }
        if (after.tier() > before.tier() || !after.cn().equals(before.cn())) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7d\u00a7l境界推进：" + before.cn() + " \u00a77\u2192 \u00a7d" + after.cn()));
            serverPlayer.playNotifySound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.3F);
        }
        serverPlayer.sendSystemMessage(Component.literal(""));

        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        Quality effective = effectiveQuality(stack);
        tooltip.add(Component.literal("\u00a77品质：" + effective.colored()
                + "  \u00a77效果 " + effective.multiplierText()
                + (com.hongmeng.xiuxian.quality.ItemQuality.isRolled(stack)
                ? "  \u00a78(掷出)" : "")));
        tooltip.add(Component.literal("\u00a77修为：\u00a7e当前小境界需求的 "
                + Math.round(baseRatio * 100) + "% \u00d7 " + effective.multiplierText()));
        tooltip.add(Component.literal("\u00a78境界越高，单颗给的绝对值越大，永远有感"));
        tooltip.add(Component.literal("\u00a78\u201c系统给的" + pillLabel() + "，一颗顶三天。\u201d"));
    }
}
