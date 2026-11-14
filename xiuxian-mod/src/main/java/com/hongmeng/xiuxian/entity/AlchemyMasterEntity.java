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
 * 炼丹师：守着一口丹炉的中年修士，擅长把玩家手里的破烂灵叶说成宝贝。
 *
 * <p>右键弹一段关于丹药的闲谈（{@link Panel#ALCHEMY}）。他不出售任何东西——
 * 真要交易请去找云游商人，这条分工是刻意的：一个给知识，一个给货。</p>
 */
public class AlchemyMasterEntity extends CultivationNpcEntity {

    public AlchemyMasterEntity(EntityType<? extends AlchemyMasterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String npcTitle() {
        return "炼丹师";
    }

    @Override
    public Panel panel() {
        return Panel.ALCHEMY;
    }

    @Override
    public SoundEvent interactSound() {
        return SoundEvents.BREWING_STAND_BREW;
    }

    @Override
    public List<String> dialogueLines() {
        return List.of(
                "丹药一道，三分靠火候，七分靠药材。",
                "回灵丹治伤，筑基丹补修为，破境丹只在突破那一刻有用——别提前吃。",
                "修为丹三档品质：普通补四分之一重，完美四分之三，仙品一颗能顶两重半。",
                "炉子里的火不能大，也不能小。人修行，也是一样。");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}
