package com.hongmeng.xiuxian.item.beast;

import com.hongmeng.xiuxian.entity.beast.SpiritBeastEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 灵兽护甲 —— 装在灵兽身上的一件护具。
 *
 * <p>规格里把它列为 P1："复用已有的护甲数值思路"。具体做法是
 * <b>直接覆盖灵兽自身的 ARMOR 属性</b>（见
 * {@link SpiritBeastEntity#setBeastArmor(boolean)}），而不是挂 AttributeModifier——
 * 灵兽的属性只由三件事决定（成长阶段、护甲、有没有被骑），
 * 直接算目标值比维护一堆带固定 UUID 的修饰符稳得多，存档迁移时也不会残留。</p>
 *
 * <p>用法：手持它右键自己的灵兽装上；再右键一次脱下来还给玩家。
 * 灵兽名牌上会多一个 \u26e8 记号，图鉴与灵兽袋 tooltip 里也看得到。</p>
 */
public class BeastArmorItem extends Item {

    public BeastArmorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77\u53f3\u952e\u81ea\u5df1\u7684\u7075\u517d\u88c5\u5907")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("\u00a78\u62a4\u7532 +\u00a7b"
                + (int) SpiritBeastEntity.BEAST_ARMOR_VALUE
                + " \u00a78\uff0c\u51fb\u9000\u6297\u6027 +"
                + (int) (SpiritBeastEntity.BEAST_ARMOR_KNOCKBACK * 100) + "%"));
        tooltip.add(Component.literal("\u00a78\u518d\u53f3\u952e\u4e00\u6b21\u5378\u4e0b\u3002"));
    }
}
