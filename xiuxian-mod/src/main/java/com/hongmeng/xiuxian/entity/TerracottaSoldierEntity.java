package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

import java.util.EnumSet;

/**
 * 兵俑（精英）——荒古战场的陶土军阵残兵。
 *
 * <p>定位：**方 + 戈**。方肩大甲片、短粗四肢、面甲一道裂纹；右手一杆青铜戈。
 * 打法上它是"重步卒"：血厚（20×3 = 60）、护甲 3.0、击退抗性 0.6，
 * 走得慢（0.24），但有一套**蓄力突刺**——站定、举戈前摇，然后向前冲一小段把戈砸出去。</p>
 *
 * <p>与其它怪的区分：石傀是"不动如山的墙"，兵俑是"会突然缩短距离的军阵"；
 * 玩家可以从"举戈动作"读到前摇并侧移躲开（这就是它的可打点）。</p>
 */
public class TerracottaSoldierEntity extends CultivationMonsterEntity {

    /** 蓄力前摇（tick）：抬戈慢。 */
    public static final int THRUST_WINDUP = 16;

    /** 突刺后摇（tick）：落戈重，之后才能再蓄力。 */
    public static final int THRUST_COOLDOWN = 70;

    /** 突刺的冲刺速度（格/tick）。 */
    public static final double THRUST_DASH = 0.75D;

    /** 突刺起手距离（格，平方在 goal 里比较）。 */
    public static final double THRUST_MIN_RANGE = 2.0D;
    public static final double THRUST_MAX_RANGE = 6.0D;

    public TerracottaSoldierEntity(EntityType<? extends TerracottaSoldierEntity> type, Level level) {
        super(type, level, 12);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（精英 ×3 生命 / ×1.5 攻击 / ×1.05 移速）。
     *
     * <pre>
     *   生命 20 × 3.0 = 60
     *   攻击 6  × 1.5 = 9
     *   移速 0.24 × 1.05 ≈ 0.252   （沉步，比普通怪慢）
     *   护甲 3.0，击退抗性 0.6      （陶俑很重，打不飞）
     * </pre>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 20.0D, 6.0D, 0.24D, 32.0D, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ThrustGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 陶土身：火不烧。陶俑是烧出来的，被火点着很出戏。
     *
     * <p>实体类型注册里没有 `.fireImmune()`（注册表是 Lead 的共享文件），
     * 所以在这里覆写——只影响这一只怪，不动共享文件。</p>
     */
    @Override
    public boolean fireImmune() {
        return true;
    }

    /**
     * 蓄力突刺：站定 → 举戈前摇（{@value #THRUST_WINDUP} tick）→ 向前冲刺 → 命中。
     *
     * <p>为什么做成 Goal 而不是写在 tick 里：它要抢 MOVE 旗标，
     * 才不会出现"一边突刺一边绕圈"。前摇期间导航被 stop 且只转向目标，
     * 玩家看到"它站住不动"就是该侧移的信号。</p>
     */
    static final class ThrustGoal extends Goal {

        private final TerracottaSoldierEntity soldier;
        private int windup;
        private int cooldown;

        ThrustGoal(TerracottaSoldierEntity soldier) {
            this.soldier = soldier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.soldier.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double dist = this.soldier.distanceToSqr(target);
            return dist >= THRUST_MIN_RANGE * THRUST_MIN_RANGE
                    && dist <= THRUST_MAX_RANGE * THRUST_MAX_RANGE
                    && this.soldier.hasLineOfSight(target);
        }

        @Override
        public void start() {
            this.windup = THRUST_WINDUP;
            this.soldier.getNavigation().stop();
            this.soldier.playSound(SoundEvents.RAVAGER_ATTACK, 0.8F, 0.7F);
        }

        @Override
        public boolean canContinueToUse() {
            return this.windup > 0 && this.soldier.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = this.soldier.getTarget();
            if (target == null) {
                this.windup = 0;
                return;
            }
            this.soldier.getNavigation().stop();
            this.soldier.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.windup--;
            if (this.windup <= 0) {
                this.thrust(target);
            }
        }

        /** 冲刺 + 命中（距离够近才算伤害，冲刺本身只负责缩短距离）。 */
        private void thrust(LivingEntity target) {
            double dx = target.getX() - this.soldier.getX();
            double dz = target.getZ() - this.soldier.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1.0E-4D) {
                dx /= len;
                dz /= len;
            } else {
                dx = 0.0D;
                dz = 0.0D;
            }
            this.soldier.setDeltaMovement(dx * THRUST_DASH,
                    this.soldier.getDeltaMovement().y, dz * THRUST_DASH);
            if (this.soldier.distanceToSqr(target) <= 3.0D * 3.0D) {
                this.soldier.doHurtTarget(target);
            }
            this.soldier.playSound(SoundEvents.STONE_BREAK, 1.0F, 0.6F);
            if (this.soldier.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        this.soldier.getX(), this.soldier.getY() + 0.9D, this.soldier.getZ(),
                        10, 0.3D, 0.2D, 0.3D, 0.05D);
            }
            this.cooldown = THRUST_COOLDOWN;
        }

        @Override
        public void stop() {
            this.windup = 0;
            if (this.cooldown <= 0) {
                this.cooldown = THRUST_COOLDOWN;
            }
        }
    }

    /** 落地时扬起一点陶土尘（不影响战斗，只是"重"的手感）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 40 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.1D, this.getZ(),
                    2, 0.2D, 0.02D, 0.2D, 0.01D);
        }
    }

    /** 突刺是"戈"，不是拳头：命中时把目标顶开半格。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            double dx = living.getX() - this.getX();
            double dz = living.getZ() - this.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1.0E-4D) {
                living.push(dx / len * 0.35D, 0.12D, dz / len * 0.35D);
            }
        }
        return hit;
    }
}
