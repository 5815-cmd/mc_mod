package com.hongmeng.xiuxian.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 炼丹材料（灵植花瓣）。除了名字和稀有度，tooltip 里还会写明
 * <b>品阶</b>与<b>用途方向</b>——六阶灵植光看图标分不出高低，
 * 玩家必须能在背包里查到"这片花瓣是几阶、该拿去炼什么"。
 *
 * <p>品阶与用途的数值来自 {@code ModItems} 里注册时传进来的参数，
 * 与 {@code docs/herb-alchemy-spec.md} 的表格一一对应。</p>
 *
 * <p>继承 {@link StackableMaterialItem}：花瓣是种田的主要产出，
 * 一茬能收几十片，所以吃高堆叠上限（默认 127，见那边的说明）。
 * tooltip 与堆叠上限互不冲突，两个都在这一类里。</p>
 */
public class AlchemyMaterialItem extends StackableMaterialItem {

    private final int tier;
    private final String use;
    private final String herbName;

    public AlchemyMaterialItem(Properties properties, int tier, String herbName, String use) {
        super(properties);
        this.tier = tier;
        this.herbName = herbName;
        this.use = use;
    }

    public int tier() {
        return tier;
    }

    public String use() {
        return use;
    }

    public String herbName() {
        return herbName;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal(herbName + " · " + tier + " 阶").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("用途：" + use).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("投入丹炉，与辅料同炼").withStyle(ChatFormatting.DARK_GRAY));
    }
}
