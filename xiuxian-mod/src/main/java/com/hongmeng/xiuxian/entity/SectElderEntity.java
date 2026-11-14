package com.hongmeng.xiuxian.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 门派长老：坐在小院主殿里的老者，负责给刚入门的修士指条明路。
 *
 * <p>右键弹一块文字面板（{@link Panel#ELDER}），内容会随玩家的境界变化——
 * 炼气期听的是"先去砍狼攒灵叶"，金丹之后听的是"别去碰虚空裂隙"。
 * 境界这一层判断放在客户端（读 {@code ClientCultivationCache}），
 * 服务端不必为此同步任何东西。</p>
 */
public class SectElderEntity extends CultivationNpcEntity {

    public SectElderEntity(EntityType<? extends SectElderEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String npcTitle() {
        return "门派长老";
    }

    @Override
    public Panel panel() {
        return Panel.ELDER;
    }

    @Override
    public SoundEvent interactSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public List<String> dialogueLines() {
        return List.of(
                "老朽在此守了六百年，见过太多急着突破的年轻人。",
                "修行有三忌：贪快、贪多、贪那点虚名。",
                "（面板下方会根据你的境界补一句当下的建议）");
    }

    /** 长老不该有攻击力，也不该乱跑：血厚一点，站得住就行。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}
