package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 血魔修士：以血为引的邪修，精英品阶。
 *
 * <p>看着是近战，其实剑气能递出四五格远——玩家按老经验贴脸躲刀，
 * 往往就被这一下隔着距离削掉半条命。这也是"精英"和杂鱼的分水岭。</p>
 */
public class BloodDemonCultivatorEntity extends CultivationMonsterEntity {

    public BloodDemonCultivatorEntity(EntityType<? extends BloodDemonCultivatorEntity> type,
                                      Level level) {
        super(type, level, 12);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 26.0D, 8.0D, 0.30D, 40.0D, 6.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BloodQiStrikeGoal(this, 1.2D, 5.0D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 剑气：隔着四五格挥出一记血光，附带轻微击退。 */
    static final class BloodQiStrikeGoal extends Goal {

        /**
         * PERF(B)：重寻路间隔下限（tick）。原来 {@link #tick()} 每 tick 都调
         * {@code getNavigation().moveTo(...)}，而 moveTo 会立刻做一次 A*。
         * 血魔修士成群时等于每 tick 几百次寻路；这里对齐原版 {@code MeleeAttackGoal}
         * 的 4~10 tick 节奏（路径由 {@code PathNavigation.tick()} 继续推进）。
         */
        private static final int PATH_MIN_TICKS = 4;

        /** PERF(B)：重寻路间隔随机跨度 → 实际 4~10 tick。 */
        private static final int PATH_SPAN_TICKS = 7;

        private final BloodDemonCultivatorEntity mob;
        private final double speedModifier;
        private final double reach;
        private int attackCooldown;
        private int seeTicks;

        /** PERF(B)：距离下次允许重新寻路的 tick 计数。 */
        private int pathRetry;

        BloodQiStrikeGoal(BloodDemonCultivatorEntity mob, double speedModifier, double reach) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.reach = reach;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target.isAlive()
                    && this.mob.distanceToSqr(target) <= this.reach * this.reach;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target.isAlive()
                    && this.mob.distanceToSqr(target)
                    <= (this.reach + 2.0D) * (this.reach + 2.0D);
        }

        @Override
        public void start() {
            this.seeTicks = 0;
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
            this.seeTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double dist = this.mob.distanceToSqr(target);
            if (dist > 9.0D) {
                // PERF(B)：寻路每 4~10 tick 才刷新一次（原来每 tick 一次 A*）
                if (--this.pathRetry <= 0) {
                    this.pathRetry = PATH_MIN_TICKS
                            + this.mob.getRandom().nextInt(PATH_SPAN_TICKS);
                    this.mob.getNavigation().moveTo(target, this.speedModifier);
                }
            } else {
                this.pathRetry = 0;      // 站定：下次需要移动时立刻寻路
                this.mob.getNavigation().stop();
            }
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            if (dist <= this.reach * this.reach
                    && this.mob.getSensing().hasLineOfSight(target)) {
                this.seeTicks++;
            } else {
                this.seeTicks = 0;
            }
            if (this.seeTicks >= 12 && this.attackCooldown <= 0) {
                this.strike(target);
                this.attackCooldown = 40;
                this.seeTicks = 0;
            }
        }

        private void strike(LivingEntity target) {
            float damage = (float) this.mob.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            this.mob.swing(InteractionHand.MAIN_HAND);
            if (target.hurt(this.mob.damageSources().mobAttack(this.mob), damage)) {
                Vec3 push = new Vec3(target.getX() - this.mob.getX(), 0.0D,
                        target.getZ() - this.mob.getZ());
                if (push.lengthSqr() > 1.0E-4D) {
                    push = push.normalize().scale(0.35D);
                }
                target.push(push.x, 0.12D, push.z);
            }
            this.mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
            if (this.mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                Vec3 from = this.mob.position().add(0.0D, this.mob.getBbHeight() * 0.6D, 0.0D);
                Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                int steps = 8;
                for (int i = 1; i <= steps; i++) {
                    double t = i / (double) steps;
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            from.x + (to.x - from.x) * t,
                            from.y + (to.y - from.y) * t,
                            from.z + (to.z - from.z) * t,
                            2, 0.05D, 0.05D, 0.05D, 0.0D);
                }
            }
        }
    }
}
