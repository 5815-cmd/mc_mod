package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 云鹤（队长）——天外天的飞行怪：长颈、长喙、双翼、三根尾羽。
 *
 * <p>定位：仙境的"高空哨兵"。它先在高空盘旋，再收翼俯冲，用长喙啄击；
 * 打不到它时最烦，俯冲落地那一下才是玩家的输出窗口。
 * 数值（队长 ×8 生命 / ×2 攻击 / ×1.10 移速）：生命 14×8=112、攻击 5.5×2=11、
 * 移速 0.34×1.10≈0.374、护甲 4。</p>
 *
 * <p><b>飞行实现</b>：按 {@code BoneEagleEntity} 的现网写法独立实现（不继承它，
 * 避免共享属性表/AI）：{@link FlyingMoveControl} + {@link FlyingPathNavigation}
 * 做三维机动，{@link #travel} 空中沿视线推进 + 阻尼，{@link #aiStep} 蹬地起飞。</p>
 */
public class CloudCraneEntity extends CultivationMonsterEntity {

    /** 俯冲冷却（tick）。 */
    public static final int DIVE_COOLDOWN = 140;

    /** 俯冲起手的最小垂直优势（格）。 */
    public static final double DIVE_MIN_HEIGHT = 3.0D;

    public CloudCraneEntity(EntityType<? extends CloudCraneEntity> type, Level level) {
        super(type, level, 24);
        // 20 = 每秒最多转多少度（越大越灵活），true = 受一点重力（不然会飘成气球）
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    /**
     * 基础属性（队长倍率）：生命 14×8=112 / 攻击 5.5×2=11 / 移速 0.34×1.10≈0.374 /
     * 追击 44 / 护甲 4。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 14.0D, 5.5D, 0.34D, 44.0D, 4.0D);
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
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.35D, true));
        this.goalSelector.addGoal(2, new DiveGoal(this));
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
     * 飞行推进（蜜蜂/雷鹰那套）：空中的 {@code moveRelative} 用自身速度而不是地面摩擦，
     * 再用 0.91 阻尼收敛；没有目标时吃一点点下沉力，免得悬停越飘越高。
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

    /** 蹬地起飞 + 云雾粒子（仙鹤的"云气"）。 */
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
        if (this.tickCount % 8 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 1.0D,
                    this.getY() + 0.4D + this.random.nextDouble() * 0.4D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 1.0D,
                    1, 0.03D, 0.01D, 0.03D, 0.01D);
        }
    }

    /** 被击中时短鸣（鹤唳）：用幻翼叫声调高，避免新增音效资源。 */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            this.playSound(SoundEvents.PHANTOM_HURT, 0.85F, 1.45F);
        }
        return hurt;
    }

    /** 长喙啄击：命中额外推开目标（俯冲那一下的"击退"）。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            double dx = living.getX() - this.getX();
            double dz = living.getZ() - this.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1.0E-4D) {
                living.push(dx / len * 0.50D, 0.22D, dz / len * 0.50D);
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        living.getX(), living.getY() + 1.0D, living.getZ(),
                        4, 0.2D, 0.15D, 0.2D, 0.01D);
            }
        }
        return hit;
    }

    /** 给渲染/调试用：当前是不是在飞。 */
    public boolean airborne() {
        return this.isFlying();
    }

    /** 高空盘旋 → 收翼俯冲：目标在下方且距离够远时给一记朝下的速度。 */
    static final class DiveGoal extends Goal {

        private final CloudCraneEntity crane;
        private int cooldown;

        DiveGoal(CloudCraneEntity crane) {
            this.crane = crane;
            this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.crane.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double dist = this.crane.distanceToSqr(target);
            return dist >= 25.0D && dist <= 625.0D
                    && this.crane.getY() > target.getY() + DIVE_MIN_HEIGHT;
        }

        @Override
        public void start() {
            LivingEntity target = this.crane.getTarget();
            if (target == null) {
                return;
            }
            Vec3 delta = new Vec3(target.getX() - this.crane.getX(),
                    target.getY() + 0.6D - this.crane.getY(),
                    target.getZ() - this.crane.getZ());
            if (delta.lengthSqr() > 1.0E-4D) {
                delta = delta.normalize().scale(0.95D);
            }
            this.crane.setDeltaMovement(delta);
            this.crane.hasImpulse = true;
            this.crane.playSound(SoundEvents.PHANTOM_SWOOP, 0.9F, 1.35F);
            this.cooldown = DIVE_COOLDOWN;
        }

        @Override
        public boolean canContinueToUse() {
            return false;   // 一次性给速度，剩下的交给 travel 与寻路
        }
    }

    /** 掉落（队长档）：30% 灵晶 / 20% 羽毛，其余走父类（队长 = 回灵丹）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 30) {
            return new ItemStack(ModItems.SPIRIT_CRYSTAL.get());
        }
        if (this.random.nextInt(100) < 20) {
            return new ItemStack(net.minecraft.world.item.Items.FEATHER, 1);
        }
        return super.rankDrop(rank);
    }
}
