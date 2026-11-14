package com.hongmeng.xiuxian.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 傀儡队长：被炼成一具会自己走路的青铜战偶，队长品阶。
 *
 * <p>它不快，也不聪明，但极硬——护甲十点、抗击退半成，还自带缓慢抗性，
 * 想靠击退溜它是不可能的。打它要的是穿透和爆发，不是风筝。</p>
 */
public class PuppetCaptainEntity extends CultivationMonsterEntity {

    public PuppetCaptainEntity(EntityType<? extends PuppetCaptainEntity> type, Level level) {
        super(type, level, 25);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 12.0D, 8.0D, 0.26D, 32.0D, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 傀儡之躯：毒与凋零对它没用，缓慢也压不住它。 */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        net.minecraft.world.effect.MobEffect type = effect.getEffect();
        if (type == MobEffects.POISON || type == MobEffects.WITHER
                || type == MobEffects.MOVEMENT_SLOWDOWN) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    /** 一斧子把人砸出去老远。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.knockback(1.6D, this.getX() - living.getX(), this.getZ() - living.getZ());
        }
        return hit;
    }
}
