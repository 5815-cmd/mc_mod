package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

import java.util.EnumSet;

/**
 * 稻草傀儡（普通）——纪元稻田/草原的田间机关。
 *
 * <p>定位：**十字架 + 斗笠**。它是被"插在地里"的：竖桩一直插到地面，
 * 追你的时候是整根竹架在挪。特色是<b>"装死"</b>——
 * 目标离得远（&gt; {@value #FREEZE_DISTANCE} 格）时它<b>原地不动</b>，
 * 只在很近的时候才突然挥袖管扑上来；玩家远远看到常常以为是个稻草人。</p>
 *
 * <p>数值是全模组最弱的一档（生命 12、攻击 3、护甲 0.5），
 * 但"看起来无害"本身就是它的强度：走到 8 格内它才动手。</p>
 */
public class ScarecrowPuppetEntity extends CultivationMonsterEntity {

    /** 目标超过这个距离就"装死"不动（格）。 */
    public static final double FREEZE_DISTANCE = 8.0D;

    /** 乌鸦叫（tick 间隔，随机）。 */
    public static final int CAW_INTERVAL = 140;

    public ScarecrowPuppetEntity(EntityType<? extends ScarecrowPuppetEntity> type, Level level) {
        super(type, level, 6);
    }

    @Override
    public MobRank rank() {
        return MobRank.NORMAL;
    }

    /**
     * 基础属性；普通品阶倍率 ×1，所以最终值 = 基础值。
     *
     * <pre>
     *   生命 12，攻击 3，移速 0.22，追击 24，护甲 0.5
     * </pre>
     */
    /** 普通品阶但靠发光件立辨识度（黑鸦黄绿眼 / 斗笠下两点暖灯）：强制绘制发光层。 */
    @Override
    public boolean forceGlow() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.NORMAL, 12.0D, 3.0D, 0.22D, 24.0D, 0.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 优先级 1 的"装死"：目标 >8 格时抢住 MOVE 旗标，把移动目标全部压住
        this.goalSelector.addGoal(1, new FreezeWhenFarGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 被吓到才动：攻击时给目标一点"稻草扑脸"的粒子（纯表现）。
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.0D, target.getZ(),
                    6, 0.25D, 0.2D, 0.25D, 0.02D);
        }
        return hit;
    }

    /** 每隔一段时间"嘎"一声（乌鸦叫）：装死时的唯一动静。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % CAW_INTERVAL != 0) {
            return;
        }
        this.playSound(net.minecraft.sounds.SoundEvents.PARROT_IMITATE_PHANTOM, 0.35F, 1.6F);
    }

    /**
     * 装死：目标 &gt;8 格时原地站定、只转头盯着目标。
     *
     * <p>用 GOAL 的 MOVE 旗标压住 MeleeAttackGoal / RandomStrollGoal，
     * 而不是在 {@code customServerAiStep} 里 {@code navigation.stop()}
     * ——后者下一 tick 就会被其它 goal 重新起导航，表现是"抽搐式挪动"。</p>
     */
    static final class FreezeWhenFarGoal extends Goal {

        private final ScarecrowPuppetEntity puppet;

        FreezeWhenFarGoal(ScarecrowPuppetEntity puppet) {
            this.puppet = puppet;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.puppet.getTarget();
            return target != null && target.isAlive()
                    && this.puppet.distanceToSqr(target) > FREEZE_DISTANCE * FREEZE_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.puppet.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.puppet.getTarget();
            this.puppet.getNavigation().stop();
            if (target != null) {
                this.puppet.getLookControl().setLookAt(target, 10.0F, this.puppet.getMaxHeadXRot());
            }
        }

        @Override
        public void stop() {
            // 不额外做什么：解除"装死"后交给下一优先级的攻击/漫游 goal
        }
    }
}
