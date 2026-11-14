package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
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

import java.util.EnumSet;

/**
 * 烈焰猿（精英）：胸口裂着岩浆纹的巨猿。
 *
 * <p>兽型怪的第二款，也是第一只<b>两足兽</b>：灵狼是四足的快速杂鱼，烈焰猿反过来——
 * 慢、笨、血厚，一拳能把筑基修士砸出五六格远。它的压迫感来自三件事：</p>
 * <ol>
 *   <li><b>火免</b>：岩浆里泡澡，火把烧不动它，还会把挨打的人点着；</li>
 *   <li><b>捶地</b>：跳起来砸地，一圈范围伤害 + 击退，逼玩家不能贴脸磨；</li>
 *   <li><b>胸口的火光</b>：每 8 刻从裂缝里冒火星，隔老远就能认出它。</li>
 * </ol>
 *
 * <p>模型见 {@code FlameApeRenderer.ApeModel}：长臂、驼背、短腿，
 * 与灵狼的四足剪影完全不像。</p>
 */
public class FlameApeEntity extends CultivationMonsterEntity {

    public FlameApeEntity(EntityType<? extends FlameApeEntity> type, Level level) {
        super(type, level, 14);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性（品阶倍率由 {@link #scaledAttributes} 乘进去）。
     *
     * <p>生命 30×3 = 90、攻击 5.5×1.5 = 8.25：比血魔修士（精英同阶）更肉更疼，
     * 代价是移速只有 0.24×1.05 ≈ 0.25——玩家跑得掉，但跑不掉它的捶地。</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 30.0D, 5.5D, 0.24D, 34.0D, 4.0D);
    }

    /** 火免：岩浆、火焰、烈焰人的火球都当挠痒。 */
    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GroundSmashGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 打中就把人点着。
     *
     * <p>注意先调 {@code super.doHurtTarget}——<b>单次伤害软上限在父类里</b>
     * （{@link CultivationMonsterEntity#MAX_HIT_RATIO}），这里只加燃烧，不碰数值。</p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.setSecondsOnFire(5);
        }
        return hit;
    }

    /** 胸口裂缝的火星：纯演出，服务端发粒子让附近玩家都看得见。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 8 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    this.getY() + 1.35D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    2, 0.12D, 0.10D, 0.12D, 0.01D);
        }
    }

    /**
     * 捶地：跳起来，落地时一圈范围伤害 + 击退。
     *
     * <p>刻意做成"有前摇、有落点"的招式：从起跳到落地大约半秒，
     * 玩家有时间跑开——挨打是因为贪刀，不是因为看不见。</p>
     */
    static final class GroundSmashGoal extends Goal {

        private final FlameApeEntity ape;
        private int cooldown;
        private int airborneTicks;
        private boolean smashed;

        GroundSmashGoal(FlameApeEntity ape) {
            this.ape = ape;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.ape.getTarget();
            if (target == null || !target.isAlive() || !this.ape.onGround()) {
                return false;
            }
            double dist = this.ape.distanceToSqr(target);
            return dist >= 4.0D && dist <= 36.0D;
        }

        @Override
        public void start() {
            LivingEntity target = this.ape.getTarget();
            if (target == null) {
                return;
            }
            Vec3 delta = new Vec3(target.getX() - this.ape.getX(), 0.0D,
                    target.getZ() - this.ape.getZ());
            if (delta.lengthSqr() > 1.0E-4D) {
                delta = delta.normalize().scale(0.42D);
            }
            this.ape.setDeltaMovement(delta.x, 0.62D, delta.z);
            this.ape.hasImpulse = true;
            this.smashed = false;
            this.airborneTicks = 0;
            this.ape.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 0.85F);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.smashed && this.airborneTicks < 60;
        }

        @Override
        public void tick() {
            this.airborneTicks++;
            if (this.ape.onGround() && this.airborneTicks > 4) {
                this.smash();
            }
        }

        private void smash() {
            this.smashed = true;
            this.cooldown = 90;
            Level level = this.ape.level();
            this.ape.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.7F);
            if (level.isClientSide()) {
                return;
            }
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                    this.ape.getBoundingBox().inflate(3.2D, 1.5D, 3.2D))) {
                if (victim == this.ape) {
                    continue;
                }
                // doHurtTarget 里仍会走父类的伤害软上限，范围伤害也不会秒人
                if (this.ape.doHurtTarget(victim)) {
                    double dx = victim.getX() - this.ape.getX();
                    double dz = victim.getZ() - this.ape.getZ();
                    double len = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
                    victim.knockback(0.9D, dx / len, dz / len);
                }
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.ape.getX(), this.ape.getY() + 0.2D, this.ape.getZ(),
                        18, 1.6D, 0.15D, 1.6D, 0.04D);
            }
        }

        @Override
        public void stop() {
            if (!this.smashed) {
                this.cooldown = 40;
            }
        }
    }
}
