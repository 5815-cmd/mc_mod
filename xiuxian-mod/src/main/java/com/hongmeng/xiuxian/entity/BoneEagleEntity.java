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
 * 骨鹰（精英）——兽骨荒原上空的白骨秃鹫。
 *
 * <p>定位：**骷髅头 + 长喙 / 骨翼 4 指 + 破翼膜 / 尾 3 根骨刺**。
 * 飞行怪：地面出生后蹬地起飞，用 {@link FlyingMoveControl} + {@link FlyingPathNavigation}
 * 做三维机动，<b>高空盘旋 12 格 → 收翼俯冲 → 落地骨爪抓人</b>。
 * 这里按设计稿 §3.4.2 独立实现飞行（<b>不继承</b> {@code ThunderEagleEntity}，
 * 避免共享它的属性表与 AI 参数）。</p>
 *
 * <p>数值（精英 ×3 生命 / ×1.5 攻击 / ×1.05 移速）：生命 17×3=51、攻击 5×1.5=7.5、
 * 移速 0.34×1.05≈0.357、护甲 2。骨鹰是"空中压制"定位：打不到它时最烦，
 * 俯冲落地那一下才是玩家的输出窗口。</p>
 */
public class BoneEagleEntity extends CultivationMonsterEntity {

    /** 俯冲冷却（tick）。 */
    public static final int DIVE_COOLDOWN = 140;

    /** 俯冲起手的最小垂直优势（格）。 */
    public static final double DIVE_MIN_HEIGHT = 3.0D;

    public BoneEagleEntity(EntityType<? extends BoneEagleEntity> type, Level level) {
        super(type, level, 16);
        // 20 = 每秒最多转多少度（越大越灵活），true = 受一点重力（不然会飘成气球）
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性（精英倍率）：生命 17×3=51 / 攻击 5×1.5=7.5 / 移速 0.34×1.05≈0.357 /
     * 追击 44 / 护甲 2。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 17.0D, 5.0D, 0.34D, 44.0D, 2.0D);
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
     * 飞行推进（蜜蜂那套）：空中的 {@code moveRelative} 用自身速度而不是地面摩擦，
     * 再用 0.91 阻尼收敛；没有目标时吃一点点下沉力，免得悬停时越飘越高。
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
     * 蹬地起飞 + 骨粉粒子。
     *
     * <p>飞行怪经典翻车点：落地后 travel 走原版分支，没有外力就再也起不来。
     * 这里在盯着目标（或闲得慌）时给一记向上速度，保证它总能回到天上。</p>
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
            // 骨粉/尘土：飞行时翼骨抖落的骨屑
            serverLevel.sendParticles(ParticleTypes.POOF,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 1.6D,
                    this.getY() + 0.5D + this.random.nextDouble() * 0.5D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 1.6D,
                    1, 0.05D, 0.02D, 0.05D, 0.01D);
        }
    }

    /** 被打中时短促地叫一声（骨头碰撞声），方便判断它绕到背后了。 */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            this.playSound(SoundEvents.PHANTOM_HURT, 0.9F, 1.25F);
        }
        return hurt;
    }

    /** 骨爪抓击：俯冲落地那一下额外把目标推开半格（设计稿 attack2「击退 0.8」的轻量版）。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            double dx = living.getX() - this.getX();
            double dz = living.getZ() - this.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1.0E-4D) {
                living.push(dx / len * 0.45D, 0.18D, dz / len * 0.45D);
            }
        }
        return hit;
    }

    /** 给渲染/调试用：当前是不是在飞。 */
    public boolean airborne() {
        return this.isFlying();
    }

    /** 俯冲：目标在下方且距离够远时，收翼给一记朝下的速度，撞上就是一口。 */
    static final class DiveGoal extends Goal {

        private final BoneEagleEntity eagle;
        private int cooldown;

        DiveGoal(BoneEagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.noneOf(Goal.Flag.class));
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
            return dist >= 25.0D && dist <= 625.0D
                    && this.eagle.getY() > target.getY() + DIVE_MIN_HEIGHT;
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
            this.eagle.playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 1.1F);
            this.cooldown = DIVE_COOLDOWN;
        }

        @Override
        public boolean canContinueToUse() {
            return false;   // 一次性给速度，剩下的交给 travel 与寻路
        }
    }

    /** 掉落（设计稿 §3.4.7）：45% 骨头 ×1~3 / 30% 羽毛 ×1~2 / 20% 灵晶 / 其余回退精英（回灵丹）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 45) {
            return new ItemStack(net.minecraft.world.item.Items.BONE,
                    1 + this.random.nextInt(3));
        }
        if (this.random.nextInt(100) < 30) {
            return new ItemStack(net.minecraft.world.item.Items.FEATHER,
                    1 + this.random.nextInt(2));
        }
        if (this.random.nextInt(100) < 20) {
            return new ItemStack(ModItems.SPIRIT_CRYSTAL.get());
        }
        return super.rankDrop(rank);
    }
}
