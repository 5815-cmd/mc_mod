package com.hongmeng.xiuxian.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 云游商人：挑着担子走四方的散修，什么都收，什么都卖。
 *
 * <p>右键<b>直接打开既有的坊市界面</b>（{@code ShopScreen}，和按 K 键是同一张）——
 * 交易逻辑、灵叶结算、解锁条件全都复用那套，这里只负责"把人带到柜台前"。
 * 面板类型给的是 {@link Panel#MERCHANT}，客户端收到之后直接换屏。</p>
 */
public class WanderingMerchantEntity extends CultivationNpcEntity {

    public WanderingMerchantEntity(EntityType<? extends WanderingMerchantEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String npcTitle() {
        return "云游商人";
    }

    @Override
    public Panel panel() {
        return Panel.MERCHANT;
    }

    @Override
    public SoundEvent interactSound() {
        return SoundEvents.VILLAGER_TRADE;
    }

    @Override
    public List<String> dialogueLines() {
        // 商人走的是"直接开坊市"那条路，这几句只在界面里当开场白用
        return List.of("客官，看看货？灵叶结账，童叟无欺。");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}
