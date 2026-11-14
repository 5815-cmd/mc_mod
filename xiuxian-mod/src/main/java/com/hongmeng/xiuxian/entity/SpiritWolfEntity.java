package com.hongmeng.xiuxian.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 灵狼：山野里成群出没的低阶妖兽。
 *
 * <p>血薄得很，但快，而且会扑——被三四只围住的时候，筑基修士也会掉半管血。
 * 它是最典型的"普通"品阶参照物：单挑谁都能赢，一拥而上就得认真对待。</p>
 */
public class SpiritWolfEntity extends CultivationMonsterEntity {

    public SpiritWolfEntity(EntityType<? extends SpiritWolfEntity> type, Level level) {
        super(type, level, 5);
    }

    @Override
    public MobRank rank() {
        return MobRank.NORMAL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.NORMAL, 10.0D, 4.0D, 0.35D, 32.0D, 0.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.45F, 3.0F));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.15D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 扑击：仗着身子轻，直接朝目标跳过去，落地就是一口。 */
    static final class LeapAtTargetGoal extends Goal {

        private final SpiritWolfEntity wolf;
        private final float verticalPower;
        private final float maxRange;
        private int cooldown;

        LeapAtTargetGoal(SpiritWolfEntity wolf, float verticalPower, float maxRange) {
            this.wolf = wolf;
            this.verticalPower = verticalPower;
            this.maxRange = maxRange;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.wolf.getTarget();
            if (target == null || !target.isAlive() || !this.wolf.onGround()) {
                return false;
            }
            double dist = this.wolf.distanceToSqr(target);
            return dist >= 4.0D && dist <= (double) (this.maxRange * this.maxRange);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.wolf.onGround();
        }

        @Override
        public void start() {
            LivingEntity target = this.wolf.getTarget();
            if (target == null) {
                return;
            }
            Vec3 delta = new Vec3(target.getX() - this.wolf.getX(), 0.0D,
                    target.getZ() - this.wolf.getZ());
            if (delta.lengthSqr() > 1.0E-4D) {
                delta = delta.normalize().scale(0.6D);
            }
            this.wolf.setDeltaMovement(delta.x, this.verticalPower, delta.z);
            this.wolf.hasImpulse = true;
            this.wolf.playSound(SoundEvents.WOLF_GROWL, 0.8F, 1.2F);
            this.cooldown = 40;
        }
    }
}
