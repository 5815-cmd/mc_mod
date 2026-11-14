package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 雷纹鹰（精英）——<b>不会飞</b>的鹰。
 *
 * <p>复用既有的 {@code ThunderEagleRenderer.EagleModel}（横向纺锤身 + 两对长翼 +
 * 长尾羽那套骨架），但<b>刻意不继承 {@code ThunderEagleEntity}</b>，
 * 三条理由都是硬的：</p>
 * <ol>
 *   <li>{@code ThunderEagleEntity} 装了 {@code FlyingMoveControl} + {@code FlyingPathNavigation}，
 *       并且每个 tick 都在往天上蹬（{@code onGround()} 时就给 +0.46 的上升速度）。
 *       继承它就必须把这些逻辑一条条关掉，改起来比另写一个类更容易出错；</li>
 *   <li>它是<b>队长</b>档（生命 16×8=128、攻击 4×2=8），而雷纹鹰要做的是"精英地面骚扰者"，
 *       品阶、尺寸、移动方式全不同——继承只会带来两套互相矛盾的数值；</li>
 *   <li>续上一条：{@code BeastManager} 会把 {@code ThunderEagleEntity} 的子类当成
 *       "可契约成契约雷鹰"的妖兽。雷纹鹰在地上跑，收服成飞行坐骑说不通。</li>
 * </ol>
 *
 * <p>所以它是<b>兄弟类</b>而不是子类：模型共用，逻辑自立。招牌动作是
 * {@link TalonLeapGoal}——贴地一记短扑（不是俯冲），落地有硬直，
 * 玩家只要"侧移一步"就能躲开，属于看得懂、躲得掉的攻击。</p>
 *
 * <p>贴图 {@code storm_eagle.png}（128×128，与雷鹰同一套 UV）。</p>
 */
public class StormEagleEntity extends CultivationMonsterEntity {

    public StormEagleEntity(EntityType<? extends StormEagleEntity> type, Level level) {
        super(type, level, 18);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（精英 ×3 生命 / ×1.5 攻击 / ×1.05 移速）。
     *
     * <pre>
     *   生命 8  × 3.0  = 24
     *   攻击 3.4 × 1.5 = 5.1
     *   移速 0.30 × 1.05 ≈ 0.315
     *   护甲 2.0
     * </pre>
     *
     * <p>比赤炎狼（24 血 / 6 攻 / 0.368 速）略软略慢，换来的是一记
     * 能把人推开的短扑——两只精英怪的手感刻意不同。</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 8.0D, 3.4D, 0.30D, 32.0D, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(2, new TalonLeapGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 落地不摔伤：不管会不会飞，它都是鸟。 */
    @Override
    public boolean causeFallDamage(float distance, float multiplier,
                                   net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /**
     * 羽尖放电 + 落地扬尘。
     *
     * <p>雷纹鹰的"雷纹"要看得见：每 10 tick 在翼尖附近撒两颗电火花
     * （与雷鹰同样的做法，颜色与密度一致，玩家不会觉得是两种特效）。</p>
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 10 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 1.6D,
                    this.getY() + 0.4D + this.random.nextDouble() * 0.8D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 1.6D,
                    1, 0.08D, 0.04D, 0.08D, 0.01D);
        }
    }

    /** 啄中时短促的一声，方便玩家判断它是不是又贴到背后来。 */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            this.playSound(SoundEvents.PHANTOM_HURT, 0.8F, 1.5F);
        }
        return hurt;
    }

    /** 精英掉落：雷音草花瓣（雷行）与灵叶。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 30) {
            return new ItemStack(ModItems.LEIYIN_CAO_PETAL.get(), 1);
        }
        return super.rankDrop(rank);
    }

    /**
     * 贴地短扑：朝目标方向给一记水平速度 + 一点点上抛，扑完落地有硬直。
     *
     * <p>与雷鹰的俯冲是两种东西：俯冲是三维的、从高处往下砸；
     * 短扑是二维的、贴着地面窜出去。躲法也不同——俯冲要"离开落点"，
     * 短扑只要"侧向挪一步"。</p>
     */
    static final class TalonLeapGoal extends Goal {

        /** 扑击的水平速度。 */
        private static final double LEAP_SPEED = 0.62D;

        /** 上抛速度（够越过一格台阶，不够飞起来）。 */
        private static final double LEAP_LIFT = 0.22D;

        /** 冷却（tick）。 */
        private static final int COOLDOWN = 70;

        private final StormEagleEntity eagle;
        private int cooldown;

        TalonLeapGoal(StormEagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.eagle.getTarget();
            if (target == null || !target.isAlive() || !this.eagle.onGround()) {
                return false;
            }
            double dist = this.eagle.distanceToSqr(target);
            // 3~7 格：太近了不需要扑，太远了扑过去也够不着
            return dist >= 9.0D && dist <= 49.0D;
        }

        @Override
        public void start() {
            LivingEntity target = this.eagle.getTarget();
            if (target == null) {
                return;
            }
            Vec3 delta = new Vec3(target.getX() - this.eagle.getX(), 0.0D,
                    target.getZ() - this.eagle.getZ());
            if (delta.lengthSqr() > 1.0E-4D) {
                delta = delta.normalize().scale(LEAP_SPEED);
            }
            this.eagle.setDeltaMovement(delta.x, LEAP_LIFT, delta.z);
            this.eagle.hasImpulse = true;
            this.eagle.playSound(SoundEvents.PHANTOM_SWOOP, 0.9F, 1.5F);
        }

        @Override
        public boolean canContinueToUse() {
            return false;   // 一次性给速度，剩下的交给寻路与落地
        }

        @Override
        public void stop() {
            this.cooldown = COOLDOWN;
        }
    }

    /** 给渲染器用：雷纹鹰永远不张嘴扇翅膀，模型因此走"收翅站姿"分支。 */
    public boolean airborne() {
        return false;
    }
}
