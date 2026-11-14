package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 雷鹰（队长）：妖兽秘境上空的掠食者。
 *
 * <p><b>第一只会飞的敌怪。</b>飞行这件事不能靠"给个 noGravity 就完事"——
 * 那样它会像气球一样贴在原地。这里走的是蜜蜂（{@code Bee}）那一套标准做法：</p>
 * <ul>
 *   <li>{@link FlyingMoveControl} 负责"往哪飞、飞多快"，并自带一点重力；</li>
 *   <li>{@link FlyingPathNavigation} 负责三维寻路（默认的地面寻路在空中<b>找不到路</b>，
 *       怪会原地抽搐——这是"会飞的怪看起来像卡住"的最常见原因）；</li>
 *   <li>{@link #travel} 在空中时不用原版的落地摩擦，而是"沿视线方向推进 + 阻尼"，
 *       于是它低头看你就是在俯冲，抬头就是在拉升。</li>
 * </ul>
 *
 * <p>攻击逻辑就是俯冲：{@link DiveBombGoal} 在目标处于下方时收翅给一记大速度，
 * 撞上就是一口；咬完拉升，绕一圈再来。</p>
 */
public class ThunderEagleEntity extends CultivationMonsterEntity {

    public ThunderEagleEntity(EntityType<? extends ThunderEagleEntity> type, Level level) {
        super(type, level, 20);
        // 20 = 每秒最多转多少度（越大越灵活），true = 受一点重力（不然会飘成气球）
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = Math.max(this.xpReward, 30);
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    /**
     * 基础属性。
     *
     * <p>生命 16×8 = 128、攻击 4×2 = 8——比同阶的石傀脆得多，
     * 但移速 0.30×1.10 ≈ 0.33 配上俯冲，是唯一能追上御剑逃跑玩家的怪。</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 16.0D, 4.0D, 0.30D, 48.0D, 2.0D);
    }

    /** 空中寻路：不换这个，飞行怪会在离地三格的地方原地打转。 */
    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.45D, true));
        this.goalSelector.addGoal(2, new DiveBombGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 现在算不算"在飞"：离地了，或者正盯着谁。 */
    private boolean isFlying() {
        return !this.onGround() || this.getTarget() != null;
    }

    /**
     * 飞行推进。
     *
     * <p>照蜜蜂那套写：空中的 {@code moveRelative} 用的是自身速度而不是地面摩擦，
     * 再用 0.91 的阻尼收敛；没有目标时才吃一点点下沉力，免得悬停时越飘越高。</p>
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isControlledByLocalInstance() && this.isFlying()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }
    }

    /** 落地不摔伤：它是鸟。 */
    @Override
    public boolean causeFallDamage(float distance, float multiplier,
                                  net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /**
     * 蹬地起飞。
     *
     * <p>飞行怪的经典翻车点：落地之后 {@link #travel} 走的是原版分支，
     * 没有外力就再也起不来，于是"雷鹰在地上走了一路"。这里盯着目标（或闲得慌）时
     * 直接给一记向上的速度，保证它总能回到天上。</p>
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.onGround() && (this.getTarget() != null || this.tickCount % 80 == 0)) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.46D, 0.0D));
            this.hasImpulse = true;
        }
        if (this.tickCount % 10 == 0 && this.level() instanceof ServerLevel serverLevel) {
            // 羽尖放电：雷鹰的标志
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 1.6D,
                    this.getY() + 0.4D + this.random.nextDouble() * 0.6D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 1.6D,
                    2, 0.1D, 0.05D, 0.1D, 0.02D);
        }
    }

    /** 被打中时短促地叫一声，方便玩家判断它绕到背后了。 */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            this.playSound(SoundEvents.PHANTOM_HURT, 0.9F, 1.35F);
        }
        return hurt;
    }

    /** 俯冲扑击：目标在下方时给一记朝下的速度。 */
    static final class DiveBombGoal extends Goal {

        private final ThunderEagleEntity eagle;
        private int cooldown;

        DiveBombGoal(ThunderEagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(java.util.EnumSet.noneOf(Goal.Flag.class));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.eagle.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double dist = this.eagle.distanceToSqr(target);
            return dist >= 25.0D && dist <= 400.0D
                    && this.eagle.getY() > target.getY() + 3.0D;
        }

        @Override
        public void start() {
            LivingEntity target = this.eagle.getTarget();
            if (target == null) {
                return;
            }
            Vec3 delta = new Vec3(target.getX() - this.eagle.getX(),
                    target.getY() + 0.6D - this.eagle.getY(),
                    target.getZ() - this.eagle.getZ());
            if (delta.lengthSqr() > 1.0E-4D) {
                delta = delta.normalize().scale(0.95D);
            }
            this.eagle.setDeltaMovement(delta);
            this.eagle.hasImpulse = true;
            this.eagle.playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 1.25F);
            this.cooldown = 120;
        }

        @Override
        public boolean canContinueToUse() {
            return false;   // 一次性给速度，剩下的交给 travel 与寻路
        }
    }

    /** 给渲染器用：翅膀该扇多快、身体该不该前倾，都看这一句。 */
    public boolean airborne() {
        return this.isFlying();
    }
}
