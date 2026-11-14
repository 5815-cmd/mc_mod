package com.hongmeng.xiuxian.entity;

import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * <b>焦骨骷髅（charred_skeleton）</b> —— 焦土魔原的炭化弓手（普通品阶）。
 *
 * <p>设计真源：{@code 工程/角色设计/round2/新怪物第二梯队设计稿.md} §3.6。
 * 骨头烧成炭色、肋骨折了两根还挂着余烬（发光），左手一张短弓、右手一支燃烧箭；
 * 近身会后退，保持 8~16 格放箭。</p>
 *
 * <p>战斗特色：</p>
 * <ul>
 *   <li><b>远程燃烧箭</b>：{@link BurningBowGoal} 三段距离带（&lt;6 格后退 / 6~16 格站定
 *       射击 / &gt;16 格靠近），每 {@value #BOW_INTERVAL}t 射一箭；箭 = 原版 {@link Arrow}，
 *       {@code setSecondsOnFire(5)}（100t 点燃，设计稿 §3.6.6）；</li>
 *   <li>近身保底：{@link MeleeAttackGoal} 在没视线/贴脸时接管；</li>
 *   <li>普通品阶但靠发光件立辨识度（眼窝余烬 + 断肋余烬 + 燃烧箭头）→
 *       {@link #forceGlow()} 返回 true。</li>
 * </ul>
 *
 * <p>模型：{@code client.CharredSkeletonModel}（64×64 + _glow，由
 * {@code 工程/角色设计/round2/new-mobs/charred_skeleton/spec.py} 生成）。</p>
 *
 * <p>数值（设计稿 §3.6.7）：生命 12 / 攻击 4.5 / 移速 0.28 / 追击 36 / 护甲 1 / 经验 8。</p>
 */
public class CharredSkeletonEntity extends CultivationMonsterEntity implements RangedAttackMob {

    /** 两箭之间的间隔（tick，设计稿：attack 12t 动画、射击节奏约 2s）。 */
    public static final int BOW_INTERVAL = 40;

    /** 目标近于这个距离就往后退（格）。 */
    private static final double TOO_CLOSE = 6.0D;

    /** 目标远于这个距离就往前挪（格）。 */
    private static final double TOO_FAR = 16.0D;

    /** 箭初速（与原版骷髅同尺）。 */
    private static final float ARROW_VELOCITY = 1.6F;

    /** 点燃秒数（5 秒 = 100t，设计稿 §3.6.6）。 */
    private static final int ARROW_FIRE_SECONDS = 5;

    public CharredSkeletonEntity(EntityType<? extends CharredSkeletonEntity> type, Level level) {
        super(type, level, 8);
    }

    @Override
    public MobRank rank() {
        return MobRank.NORMAL;
    }

    /** 普通品阶但靠发光件立辨识度（眼窝/断肋余烬 + 燃烧箭头）：强制绘制发光层。 */
    @Override
    public boolean forceGlow() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.NORMAL, 12.0D, 4.5D, 0.28D, 36.0D, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BurningBowGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 射一支燃烧箭（原版 {@link Arrow} + 点燃 5 秒）。
     *
     * <p>弹道照抄原版 {@code AbstractSkeleton.performRangedAttack}：目标胸口 1/3 高度 +
     * 水平距离 20% 的抛物线补偿；难度决定散布（{@code 14 - difficulty*4}）。</p>
     */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Arrow arrow = new Arrow(this.level(), this);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.2D, dz, velocity,
                14.0F - (float) serverLevel.getDifficulty().getId() * 4.0F);
        arrow.setSecondsOnFire(ARROW_FIRE_SECONDS);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F,
                1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        serverLevel.addFreshEntity(arrow);
    }

    /** 掉落（设计稿 §3.6.7；设计稿 60+40+20=120% 收敛为 60/25/10 + 5% 回退）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        int roll = this.random.nextInt(100);
        if (roll < 60) {
            return new ItemStack(Items.BONE, 1 + this.random.nextInt(2));
        }
        if (roll < 85) {
            return new ItemStack(Items.COAL, 1);
        }
        if (roll < 95) {
            return new ItemStack(com.hongmeng.xiuxian.item.ModItems.HUOYAN_HUA_PETAL.get(), 1);
        }
        return super.rankDrop(rank);
    }

    /**
     * 燃烧弓：保持 6~16 格放箭，近了后退、远了靠近。
     *
     * <p>没有直接用原版 {@code RangedAttackGoal}（它只站定开火、不会后退）；风筝节奏照
     * {@code VenomToadEntity.SpitVenomGoal} 的三段距离带写。</p>
     */
    static final class BurningBowGoal extends Goal {

        /**
         * PERF(B)：重寻路间隔下限（tick）。原来 {@link #tick()} 每 tick 都调
         * {@code getNavigation().moveTo(...)}，而 moveTo 会立刻做一次 A*。
         * 弓手怪一多就是每 tick 几百次寻路；这里对齐原版 {@code MeleeAttackGoal}
         * 的 4~10 tick 节奏（路径由 {@code PathNavigation.tick()} 继续推进）。
         */
        private static final int PATH_MIN_TICKS = 4;

        /** PERF(B)：重寻路间隔随机跨度 → 实际 4~10 tick。 */
        private static final int PATH_SPAN_TICKS = 7;

        private final CharredSkeletonEntity skeleton;
        private int cooldown;

        /** PERF(B)：距离下次允许重新寻路的 tick 计数。 */
        private int pathRetry;

        BurningBowGoal(CharredSkeletonEntity skeleton) {
            this.skeleton = skeleton;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.skeleton.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double distSq = this.skeleton.distanceToSqr(target);
            return distSq >= 2.25D && this.skeleton.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.skeleton.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.cooldown = 10;
        }

        @Override
        public void stop() {
            this.skeleton.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.skeleton.getTarget();
            if (target == null) {
                return;
            }
            this.skeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double dist = Math.sqrt(this.skeleton.distanceToSqr(target));
            if (dist < TOO_CLOSE) {
                // 贴脸时朝目标反方向退 6 格（弓手不能被近身）
                // PERF(B)：寻路每 4~10 tick 才刷新一次（原来每 tick 一次 A*）
                if (--this.pathRetry <= 0) {
                    this.pathRetry = PATH_MIN_TICKS
                            + this.skeleton.getRandom().nextInt(PATH_SPAN_TICKS);
                    double dx = this.skeleton.getX() - target.getX();
                    double dz = this.skeleton.getZ() - target.getZ();
                    double len = Math.max(0.05D, Math.sqrt(dx * dx + dz * dz));
                    this.skeleton.getNavigation().moveTo(
                            this.skeleton.getX() + dx / len * 6.0D,
                            this.skeleton.getY(),
                            this.skeleton.getZ() + dz / len * 6.0D, 1.0D);
                }
            } else if (dist > TOO_FAR) {
                // PERF(B)：同上
                if (--this.pathRetry <= 0) {
                    this.pathRetry = PATH_MIN_TICKS
                            + this.skeleton.getRandom().nextInt(PATH_SPAN_TICKS);
                    this.skeleton.getNavigation().moveTo(target, 1.0D);
                }
            } else {
                this.pathRetry = 0;      // 站定：下次需要移动时立刻寻路
                this.skeleton.getNavigation().stop();
            }
            if (--this.cooldown > 0) {
                return;
            }
            this.cooldown = BOW_INTERVAL;
            this.skeleton.performRangedAttack(target, ARROW_VELOCITY);
        }
    }
}
