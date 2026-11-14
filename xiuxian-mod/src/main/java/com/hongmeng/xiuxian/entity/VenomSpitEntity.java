package com.hongmeng.xiuxian.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 毒涎 —— 毒涎蟾喷出来的那一口毒。
 *
 * <p>直接继承 {@code JiaolongSpitEntity}：飞行时间、命中判定、
 * 伤害口径（施法者攻击力 × {@code DAMAGE_RATIO}，走 {@code hurt} 的标准路径、
 * 吃护甲减伤）与中毒结算都已经在那边调好了，这里只改两件事：</p>
 * <ul>
 *   <li><b>实体类型自成一档</b>——否则玩家被蟾蜍打死时，死亡信息会写
 *       "被 蛟龙之息 杀死"，一眼就穿帮；</li>
 *   <li><b>外观换一枚血珞芝花瓣</b>（暗红色的丸子），与蛟龙的青绿灵叶分开，
 *       场上有蛟龙又有蟾蜍时能一眼看出哪一发是谁吐的。</li>
 * </ul>
 *
 * <p>它<b>不注册属性</b>：投掷物不是 {@code LivingEntity}，
 * 给非生物实体注册属性会在启动时抛 {@code ClassCastException}。</p>
 */
public class VenomSpitEntity extends com.hongmeng.xiuxian.entity.beast.JiaolongSpitEntity {

    public VenomSpitEntity(EntityType<? extends VenomSpitEntity> type, Level level) {
        super(type, level);
    }

    public VenomSpitEntity(EntityType<? extends VenomSpitEntity> type, Level level,
                           LivingEntity shooter) {
        super(type, level, shooter);
    }

    /** 渲染用的物品：血珞芝花瓣（注册表还没起来时退回空栈，渲染器会跳过）。 */
    private static ItemStack venomItem() {
        try {
            net.minecraft.world.item.Item petal =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                            net.minecraft.resources.ResourceLocation.tryBuild(
                                    com.hongmeng.xiuxian.HongmengMod.MOD_ID, "xueluo_zhi_petal"));
            if (petal != null) {
                return new ItemStack(petal);
            }
        } catch (Throwable ignored) {
            // 注册表没起来：退回空栈
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected ItemStack getItemRaw() {
        return venomItem();
    }

    @Override
    protected net.minecraft.world.item.Item getDefaultItem() {
        ItemStack stack = venomItem();
        return stack.isEmpty() ? net.minecraft.world.item.Items.AIR : stack.getItem();
    }
}
