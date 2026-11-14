package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>灵石</b> —— 挖「灵石矿脉」得到的修炼资源。
 *
 * <h2>定位（玩家要求）</h2>
 * <blockquote>「可以设置灵石矿脉，让这些灵石就可以增加修为，也可以合成丹药增加更多」</blockquote>
 * <ul>
 *   <li><b>右键直接服用</b>：按“当前小境界需求”的 {@value #DIRECT_RATIO_TEXT} 涨修为
 *       （境界越高绝对值越大，永远有感）；</li>
 *   <li><b>合成成丹药更划算</b>：灵石 ×4 + 灵叶 ×2 → 聚灵丹（15%），
 *       灵石 ×6 + 灵晶 ×2 + 灵叶 ×1 → 凝元丹（45%）；</li>
 *   <li>属于可堆叠材料，同时吃{@link CultivationSource#grant} 的灵根倍率与
 *       “当前小境界封顶”规则（不会让玩家一口气滑到下一境）。</li>
 * </ul>
 *
 * <p>为什么按“百分比”而不是固定值：参考既有修为丹（
 * {@link ExperiencePillItem}）的设计——固定数值在后期等于没有，
 * 按比例给才能让每一颗灵石在任何一个境界都“有感觉”。</p>
 *
 * <p>继承 {@link StackableMaterialItem}：灵石是"会攒一大把"的资源，
 * 跟灵铁锭/灵晶一样吃配置里的材料堆叠上限（默认 127）。</p>
 */
public class SpiritStoneItem extends StackableMaterialItem {

    /** 单颗灵石直接服用时，给“当前小境界需求”的百分比。 */
    public static final double DIRECT_RATIO = 0.025D;

    /** 给 tooltip 用的百分比文本（避免两处写死不一致）。 */
    public static final String DIRECT_RATIO_TEXT = "2.5%";

    public SpiritStoneItem(Properties properties) {
        super(properties);
    }

    /** 当前境界下一颗灵石给多少修为（按百分比，最少 1）。 */
    public static long cultivationFor(CultivationData data) {
        double need;
        try {
            need = data == null ? 100.0D : data.realm().need();
        } catch (Throwable t) {
            need = 100.0D;
        }
        if (need <= 0.0D) {
            need = 100.0D;
        }
        return (long) Math.max(1.0D, Math.round(need * DIRECT_RATIO));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        CultivationData data = CultivationCapability.get(serverPlayer);
        if (!data.isAwakened()) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7c灵根未觉醒，灵石里的灵气化不开。先等系统分配灵根。"));
            return InteractionResultHolder.fail(stack);
        }

        long gain = cultivationFor(data);
        long need = Math.max(1L, data.realm().need());
        long applied = CultivationSource.SPIRIT_STONE.grant(serverPlayer, gain, false);

        if (applied > 0L) {
            // 反馈：音效 + 一小圈粒子（动作栏的"+修为 [灵石]"由 grant 负责）
            serverPlayer.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.8F, 1.6F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(),
                        6, 0.25D, 0.35D, 0.25D, 0.01D);
            }
            // 被“本层上限”截断时明确说明，别让玩家以为吞了灵石
            if (applied < gain) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "\u00a77灵石只化开了一部分：\u00a7f+" + applied
                                + "\u00a77（本层上限剩余；名义 +" + gain
                                + "）。\u00a78先按 B 破境再服用更划算。"));
            }
        }
        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77右键服用：\u00a7b+当前小境界需求的 "
                + DIRECT_RATIO_TEXT + "\u00a77 修为").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("\u00a78境界越高，单颗给的绝对值越大"));
        tooltip.add(Component.literal("\u00a78合成「聚灵丹 / 凝元丹」效率更高（见修行录·丹药）"));
        tooltip.add(Component.literal("\u00a77来源：\u00a7f挖「灵石矿脉」\u00a78（主世界地下）"));
        tooltip.add(Component.literal("\u00a78\u201c一枚灵石，抵一段打坐。\u201d"));
    }
}
