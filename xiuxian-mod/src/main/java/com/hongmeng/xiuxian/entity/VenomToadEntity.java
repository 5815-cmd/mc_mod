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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * 毒涎蟾（队长）——<b>远程喷吐型</b>妖兽。
 *
 * <p>职能分化的第二格：既有妖兽<b>全部是近战</b>（扑、捶、俯冲、撞、缠），
 * 玩家对付它们的套路永远是"后退 + 风筝"——没有一只怪会惩罚"站着不动"。
 * 毒涎蟾就是来堵这条路的：它<b>主动保持 4~6 格</b>，
 * 喷出的毒涎带中毒与减速，逼玩家要么冲上去近战，要么换远程。
 * 反过来，它近战很弱（体质全点在血量与毒上），这就是它的破绽。</p>
 *
 * <h3>数值口径</h3>
 * <ul>
 *   <li>生命 22×8 = 176、攻击 4×2 = 8——队长里血最厚，但移速只有 0.22（×1.10），
 *       跑是跑不掉的；</li>
 *   <li>毒涎伤害 = 施法者攻击力 × {@code JiaolongSpitEntity.DAMAGE_RATIO}，
 *       走 {@code hurt} 的标准路径，<b>吃护甲减伤，也不绕过 35% 软上限</b>。</li>
 * </ul>
 */
public class VenomToadEntity extends CultivationMonsterEntity {

    /** 两次喷吐之间的间隔（tick）。 */
    public static final int SPIT_INTERVAL = 45;

    /** 目标近于这个距离就往后退（格）。 */
    private static final double TOO_CLOSE = 4.0D;

    /** 目标远于这个距离就往前挪（格）。 */
    private static final double TOO_FAR = 7.0D;

    /** 喷吐的最大射程（格）——超出就干脆不进入这个目标。 */
    public static final double SPIT_RANGE = 18.0D;

    public VenomToadEntity(EntityType<? extends VenomToadEntity> type, Level level) {
        super(type, level, 18);
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 22.0D, 4.0D, 0.22D, 32.0D, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SpitVenomGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 喷一口毒涎。
     *
     * <p>弹体复用已有的 {@code JiaolongSpitEntity} 机制（它是"妖兽吐出的东西"
     * 这条链路的既有实现：飞行时间、命中判定、中毒与护甲减伤都已经调好），
     * 只换成自己的实体类型与外观，免得玩家看到一只蟾蜍吐"蛟龙之息"。</p>
     */
    void spitAt(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        VenomSpitEntity spit = new VenomSpitEntity(
                ModEntities.VENOM_SPIT.get(), this.level(), this);
        spit.shootAt(target, 0.85D);
        this.level().addFreshEntity(spit);
        this.playSound(SoundEvents.LLAMA_SPIT, 1.0F, 0.65F);
    }

    /** 远程怪不追击：追到天边反而把自己的射程优势丢了。 */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /**
     * 保持距离 + 定时喷吐。
     *
     * <p>没有直接用原版 {@code RangedAttackGoal}：那个只负责"站定开火"，
     * 不会后退。风筝型怪物的核心恰恰是"你进我退"，所以这里自己写，
     * 三条距离带：</p>
     * <pre>
     *    &lt; 4 格   后退（贴脸时它其实打不过你）
     *    4~7 格   站定喷吐 ← 这就是它想待的位置
     *    &gt; 7 格   往前挪到射程内
     * </pre>
     */
    static final class SpitVenomGoal extends Goal {

        /**
         * PERF(B)：重寻路间隔下限（tick）。原来 {@link #tick()} 每 tick 都调
         * {@code getNavigation().moveTo(...)}，而 moveTo 会立刻做一次 A*。
         * 毒蟾成群时就是每 tick 几百次寻路；这里对齐原版 {@code MeleeAttackGoal}
         * 的 4~10 tick 节奏（路径由 {@code PathNavigation.tick()} 继续推进）。
         */
        private static final int PATH_MIN_TICKS = 4;

        /** PERF(B)：重寻路间隔随机跨度 → 实际 4~10 tick。 */
        private static final int PATH_SPAN_TICKS = 7;

        private final VenomToadEntity toad;
        private int cooldown;

        /** PERF(B)：距离下次允许重新寻路的 tick 计数。 */
        private int pathRetry;

        SpitVenomGoal(VenomToadEntity toad) {
            this.toad = toad;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.toad.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double dist = this.toad.distanceToSqr(target);
            return dist >= 2.25D && dist <= SPIT_RANGE * SPIT_RANGE
                    && this.toad.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.toad.getTarget();
            return target != null && target.isAlive()
                    && this.toad.distanceToSqr(target) <= SPIT_RANGE * SPIT_RANGE * 1.4D;
        }

        @Override
        public void start() {
            this.cooldown = 12;
        }

        @Override
        public void stop() {
            this.toad.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.toad.getTarget();
            if (target == null) {
                return;
            }
            this.toad.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double dist = Math.sqrt(this.toad.distanceToSqr(target));
            if (dist < TOO_CLOSE) {
                // 朝向反方向退：用"目标 → 自己"这条射线延长 6 格作为落点
                // PERF(B)：寻路每 4~10 tick 才刷新一次（原来每 tick 一次 A*）
                if (--this.pathRetry <= 0) {
                    this.pathRetry = PATH_MIN_TICKS
                            + this.toad.getRandom().nextInt(PATH_SPAN_TICKS);
                    double dx = this.toad.getX() - target.getX();
                    double dz = this.toad.getZ() - target.getZ();
                    double len = Math.max(0.05D, Math.sqrt(dx * dx + dz * dz));
                    this.toad.getNavigation().moveTo(
                            this.toad.getX() + dx / len * 6.0D,
                            this.toad.getY(),
                            this.toad.getZ() + dz / len * 6.0D, 1.0D);
                }
            } else if (dist > TOO_FAR) {
                // PERF(B)：同上
                if (--this.pathRetry <= 0) {
                    this.pathRetry = PATH_MIN_TICKS
                            + this.toad.getRandom().nextInt(PATH_SPAN_TICKS);
                    this.toad.getNavigation().moveTo(target, 1.0D);
                }
            } else {
                this.pathRetry = 0;      // 站定：下次需要移动时立刻寻路
                this.toad.getNavigation().stop();
            }

            if (--this.cooldown > 0) {
                return;
            }
            this.cooldown = SPIT_INTERVAL;
            this.toad.spitAt(target);
        }
    }
}
