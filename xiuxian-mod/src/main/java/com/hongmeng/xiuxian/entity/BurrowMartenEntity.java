package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

import java.util.EnumSet;

/**
 * 土行貂（精英）——<b>遁地伏击型</b>妖兽。
 *
 * <p>照 {@code docs/spirit-beast-spec.md} 的"职能分化"原则设计：
 * 它不是"再画一只四足兽"，而是<b>换一套打法</b>。既有五只妖兽的职能是
 * 扑击（灵狼）、重锤（烈焰猿）、俯冲（雷鹰）、硬顶（石傀）、缠斗（虚空蛟），
 * 唯独没有"看不见的敌人"——土行貂补的就是这一格。</p>
 *
 * <h3>打法（三步）</h3>
 * <ol>
 *   <li><b>遁地</b>：{@code setInvisible(true)} + {@link #getSpeed()} 提速，
 *       身体没入土中（原版隐身是<b>同步</b>的，客户端不需要任何额外数据），
 *       只留一串翻土的碎屑粒子提示"它在往你这儿钻"；</li>
 *   <li><b>逼近</b>：遁地期间由 {@link BurrowGoal} 直接驱动寻路，速度 ×1.8；</li>
 *   <li><b>破土</b>：靠近到 2.5 格以内猛地钻出来——一击 + 减速 + 顶飞，
 *       然后进入 {@value #BURROW_COOLDOWN} tick 的冷却（这段时间它是一只普通的貂）。</li>
 * </ol>
 *
 * <p>对付它的办法也很明确：<b>它在土里也能被打到</b>（只是看不见），
 * 所以范围伤害与乱砍都有效——"看不见"不等于"无敌"，这是刻意的平衡。</p>
 */
public class BurrowMartenEntity extends CultivationMonsterEntity {

    /** 破土之后要在地面上待多久才允许再次遁地（tick）。 */
    public static final int BURROW_COOLDOWN = 160;

    /** 一次遁地最长持续（tick）——久了玩家会以为它消失了。 */
    public static final int BURROW_MAX_TICKS = 90;

    /** 遁地时的提速倍率。 */
    private static final float BURROW_SPEED = 1.8F;

    /** 破土的距离（格）。 */
    private static final double ERUPT_RANGE = 2.5D;

    /** 现在在不在土里。不落盘：读档后从地面开始，免得存档里"永远在地下"。 */
    private boolean burrowed;

    public BurrowMartenEntity(EntityType<? extends BurrowMartenEntity> type, Level level) {
        super(type, level, 12);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性。
     *
     * <p>生命 9×3 = 27、攻击 3.5×1.5 ≈ 5.25——比灵狼硬一档但仍是精英里最脆的，
     * 因为它靠"偷袭"而不是"扛"取胜。移速 0.34 是全部妖兽里最快的，
     * 遁地时再 ×1.8。</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 9.0D, 3.5D, 0.34D, 32.0D, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BurrowGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.05D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 遁地状态（渲染层用它决定要不要画"翻土的土堆"）。 */
    public boolean isBurrowed() {
        return this.burrowed;
    }

    /**
     * 遁地期间的额外速度。
     *
     * <p>覆写 {@link #getSpeed()} 而不是给自己加一个"速度提升"效果：
     * 效果会同步到客户端并冒出一堆药水粒子，而这里要的是"闷声钻过来"。</p>
     */
    @Override
    public float getSpeed() {
        return super.getSpeed() * (this.burrowed ? BURROW_SPEED : 1.0F);
    }

    /**
     * 地下的貂不咬人。
     *
     * <p>否则玩家会看到"空气在咬我"。破土那一击会先把标记清掉再走这里，
     * 所以 {@code doHurtTarget} 上的 <b>35% 单次伤害软上限照旧生效</b>。</p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.burrowed) {
            return false;
        }
        return super.doHurtTarget(target);
    }

    /** 进出土里；顺带把粒子与音效一次做完。 */
    void setBurrowed(boolean value) {
        if (this.burrowed == value) {
            return;
        }
        this.burrowed = value;
        this.setInvisible(value);
        if (this.level().isClientSide()) {
            return;
        }
        if (value) {
            this.playSound(SoundEvents.SNIFFER_DIGGING, 1.0F, 1.4F);
        } else {
            this.playSound(SoundEvents.RAVAGER_ATTACK, 0.9F, 1.5F);
            this.burst();
        }
    }

    /** 翻土的碎屑。 */
    private void burst() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF,
                    this.getX(), this.getY() + 0.2D, this.getZ(),
                    18, 0.45D, 0.25D, 0.45D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY() + 0.1D, this.getZ(),
                    12, 0.35D, 0.10D, 0.35D, 0.06D);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !this.burrowed) {
            return;
        }
        // 每 4 tick 冒一点土：玩家因此能判断"它正从哪个方向过来"，
        // 但又看不清本体——这就是"伏击"该有的信息量。
        if (this.tickCount % 4 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY() + 0.12D, this.getZ(),
                    3, 0.25D, 0.05D, 0.25D, 0.03D);
        }
    }

    /**
     * 遁地 → 逼近 → 破土。
     *
     * <p>刻意做成一个 {@link Goal} 而不是写在 {@code tick()} 里：
     * 这样它会与其他目标抢占 MOVE 旗标，不会出现"一边遁地一边绕圈"的鬼畜。</p>
     */
    static final class BurrowGoal extends Goal {

        /**
         * PERF(B)：重寻路间隔下限（tick）。原来 {@link #tick()} 遁地追击时**每 tick**
         * 调一次 {@code getNavigation().moveTo(...)}，而 moveTo 会立刻做一次 A*。
         * 地下多只同时追人时等于每 tick 几百次寻路；这里对齐原版
         * {@code MeleeAttackGoal} 的 4~10 tick 节奏（路径由 PathNavigation 继续推进）。
         */
        private static final int PATH_MIN_TICKS = 4;

        /** PERF(B)：重寻路间隔随机跨度 → 实际 4~10 tick。 */
        private static final int PATH_SPAN_TICKS = 7;

        private final BurrowMartenEntity marten;
        private int burrowTicks;
        private int cooldown;

        /** PERF(B)：距离下次允许重新寻路的 tick 计数。 */
        private int pathRetry;

        BurrowGoal(BurrowMartenEntity marten) {
            this.marten = marten;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.marten.getTarget();
            if (target == null || !target.isAlive() || !this.marten.onGround()) {
                return false;
            }
            double dist = this.marten.distanceToSqr(target);
            // 太近不用遁（都贴脸了），太远也追不上（32 格外早就被看见了）
            return dist >= 16.0D && dist <= 576.0D;
        }

        @Override
        public void start() {
            this.burrowTicks = 0;
            this.pathRetry = 0;      // PERF(B)：刚入地立刻寻路一次
            this.marten.setBurrowed(true);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.marten.getTarget();
            return this.burrowTicks < BURROW_MAX_TICKS
                    && target != null && target.isAlive();
        }

        @Override
        public void tick() {
            this.burrowTicks++;
            LivingEntity target = this.marten.getTarget();
            if (target == null) {
                return;
            }
            if (this.marten.distanceToSqr(target) <= ERUPT_RANGE * ERUPT_RANGE
                    && this.burrowTicks > 10) {
                this.erupt(target);
                return;
            }
            // PERF(B)：寻路每 4~10 tick 才刷新一次（原来每 tick 一次 A*）
            if (--this.pathRetry <= 0) {
                this.pathRetry = PATH_MIN_TICKS
                        + this.marten.getRandom().nextInt(PATH_SPAN_TICKS);
                this.marten.getNavigation().moveTo(target, 1.0D);
            }
        }

        /** 破土一击：伤害走父类（软上限有效），额外减速 + 顶飞。 */
        private void erupt(LivingEntity target) {
            this.marten.setBurrowed(false);
            if (this.marten.distanceToSqr(target) <= ERUPT_RANGE * ERUPT_RANGE * 1.6D) {
                this.marten.doHurtTarget(target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0),
                        this.marten);
                target.push(0.0D, 0.42D, 0.0D);
            }
            this.marten.getNavigation().stop();
            this.cooldown = BURROW_COOLDOWN;
        }

        @Override
        public void stop() {
            if (this.marten.isBurrowed()) {
                this.marten.setBurrowed(false);
            }
            if (this.cooldown <= 0) {
                this.cooldown = BURROW_COOLDOWN;
            }
        }
    }
}
