package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * 功法秘籍：右键修习对应功法。
 *
 * <p>参考其它修仙模组的做法——功法不是自动学会的，要拿到**秘籍**才能修习。
 * 秘籍本身是一次性的（学完就消失），并且有境界门槛：境界不够时读不动，
 * 会把门槛写在提示里，玩家知道该练到什么程度。</p>
 */
public class TechniqueManualItem extends Item {

    private final Technique technique;

    public TechniqueManualItem(Technique technique, Properties properties) {
        super(properties);
        this.technique = technique;
    }

    public Technique technique() {
        return technique;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        CultivationData data = CultivationCapability.get(sp);
        if (data == null || !data.isAwakened()) {
            sp.displayClientMessage(Component.literal(
                    "\u00a77你还没踏上修行路，读不懂这上面的字。"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (data.getTechniques().has(technique)) {
            sp.displayClientMessage(Component.literal(
                    "\u00a77你已修习" + technique.colored() + "\u00a77，无需再读。"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (data.realm().tier() < technique.minTier()) {
            sp.displayClientMessage(Component.literal(
                    "\u00a7c境界不足：需要第 " + technique.minTier()
                            + " 大境（当前 " + data.realm().cn() + "\u00a7c）"), true);
            return InteractionResultHolder.fail(stack);
        }

        data.getTechniques().learn(technique);
        sp.displayClientMessage(Component.literal(
                "\u00a7a\u00a7l修习成功：" + technique.colored()), false);
        sp.displayClientMessage(Component.literal(
                "\u00a77特性：\u00a7f" + technique.trait()), false);
        sp.displayClientMessage(Component.literal(
                "\u00a78按 \u00a7fV \u00a78施展，按 \u00a7fC \u00a78切换功法。"), false);

        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.4F);

        if (!sp.isCreative()) {
            stack.shrink(1);
        }
        com.hongmeng.xiuxian.network.ModNetworkBridge.sync(sp);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77功法秘籍 · \u00a7f右键修习"));
        tooltip.add(Component.literal("\u00a78元素：" + technique.element().cn()
                + "   \u00a78形态：" + technique.kind().glyph()));
        tooltip.add(Component.literal("\u00a77门槛：\u00a7e第 " + technique.minTier() + " 大境"));
        tooltip.add(Component.literal("\u00a77冷却：\u00a7b"
                + String.format("%.1f", technique.cooldownSeconds()) + "s"));
        if (!technique.isSupport()) {
            tooltip.add(Component.literal("\u00a77威力：\u00a7c×"
                    + String.format("%.1f", technique.damageMultiplier())
                    + " \u00a78（本境标准值）"));
        }
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("\u00a78" + technique.flavor()));
        tooltip.add(Component.literal("\u00a76特性：\u00a7f" + technique.trait()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 秘籍带附魔光效，在一堆物品里显眼
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("\u00a7" + technique.colorCode()
                + technique.cn() + "秘籍");
    }

    /** 供配方与调试用的描述。 */
    public String describe() {
        return ChatFormatting.stripFormatting(technique.cn()) + " [" + technique.element().cn()
                + "/" + technique.kind().glyph() + "]";
    }
}
