package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.heaven.HeavenRealm;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * <b>登天令</b> —— 进 / 出第五维度「天外天」的唯一道具（13 号设计稿 §1.3）。
 *
 * <h2>为什么是道具而不是传送门</h2>
 * <p>天外天是「终局后的仙人空中仙庭」，规格要求的是<b>登天</b>这个动作本身，
 * 而不是在人间再找一座门。所以做成一块随时能掏出来的青玉令牌：</p>
 * <ul>
 *   <li><b>人间右键</b>：境界 ≥ 仙人（大境 8）→ 传送到 {@code hongmeng:heaven_realm}
 *       的登天台（场地由 {@link HeavenRealm} 幂等现铺，不用预先盖）；</li>
 *   <li><b>天外天内右键</b>：回到「进天之前站的那个位置」（记录点存在玩家 persistentData），
 *       <b>冷却 60t</b> 防误触。</li>
 * </ul>
 *
 * <p><b>本类只管"什么时候调"</b>：境界判定、建台、Boss 召唤、环境刷怪全在
 * {@link HeavenRealm}（服务端逻辑集中在一个地方，别在道具里再写一份）。
 * 道具不消耗——这是终局道具，不是一次性符纸。</p>
 */
public class HeavenTokenItem extends Item {

    public HeavenTokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 客户端只负责"抬手"（播放使用动作/音效），结算全部在服务端
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            HeavenRealm.useToken(serverPlayer, stack);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal("§7登天令 · 青玉为凭，云纹为引").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§b右键 §7：飞升 §e天外天 §8（需 §e仙人 §8境界）"));
        tooltip.add(Component.literal("§b在天外天内右键 §7：返回人间 §8（冷却 60 秒）"));
        tooltip.add(Component.literal("§8「天门不开，非仙莫入。」"));
    }

    /**
     * 道具属性：不消耗、不堆叠。
     *
     * <p>不堆叠是刻意的：令牌是"身份凭证"，身上带一摞没有意义；
     * 稀有度给 RARE 让名字带颜色，跟其它秘境门钥匙一个档。</p>
     */
    public static Item.Properties defaultProperties() {
        return new Item.Properties().stacksTo(1).rarity(Rarity.RARE);
    }
}
