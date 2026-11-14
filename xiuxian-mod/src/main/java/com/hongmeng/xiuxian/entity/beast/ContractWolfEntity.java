package com.hongmeng.xiuxian.entity.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * 契约灵狼 —— <b>战斗协助</b>职能，全套灵兽里唯一不能被骑的一只。
 *
 * <h3>它的生态位</h3>
 *
 * <p>规格第三节把它写成"主动攻击敌对生物，<b>伤害随主人境界</b>"。
 * 落实下来是两件事：</p>
 *
 * <ul>
 *   <li><b>会主动咬人</b>：{@code NearestAttackableTargetGoal} 盯着主人附近的
 *       {@link Monster}，还会用 {@link PackTacticsGoal} 扑上去——灵感来自
 *       已有的妖兽灵狼（{@code SpiritWolfEntity.LeapAtTargetGoal}），
 *       但这里是"护主"而不是"猎食"；</li>
 *   <li><b>伤害随主人境界涨</b>：见 {@link #refreshGrowthAttributes()} 的覆盖——
 *       主人的大境每高一层，攻击力就多一截。这样"练气期收的狼"到了金丹期
 *       仍然有用，而不是变成一只跟不上的摆设。</li>
 * </ul>
 *
 * <p>代价：它<b>不能骑</b>（{@code BeastRole.COMBAT_AIDE.rideable() == false}），
 * 不能驮货，也不会飞。想要机动性请去找雷鹰。</p>
 *
 * <p>来源妖兽是 {@code spirit_wolf}（普通品阶），所以它是玩家能收服的
 * <b>第一只</b>灵兽——也是整个契约玩法的教学关。</p>
 */
public class ContractWolfEntity extends SpiritBeastEntity {

    /** 主人每高一个大境，攻击力额外乘多少。 */
    public static final double TIER_DAMAGE_BONUS = 0.12D;

    public ContractWolfEntity(EntityType<? extends ContractWolfEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    @Override
    public BeastKind kind() {
        return BeastKind.CONTRACT_WOLF;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BeastKind.CONTRACT_WOLF.baseHealth())
                .add(Attributes.ATTACK_DAMAGE, BeastKind.CONTRACT_WOLF.baseDamage())
                .add(Attributes.MOVEMENT_SPEED, BeastKind.CONTRACT_WOLF.baseSpeed())
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    /** 战斗协助不给人骑。 */
    @Override
    public boolean canBeRiddenBy(net.minecraft.world.entity.player.Player player) {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.45D, true));
        this.goalSelector.addGoal(4, new PackTacticsGoal(this));
        // 主人的敌人就是它的敌人；另外主动清掉主人附近的敌对生物
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class,
                12, true, false, living -> this.isTame() && this.getOwner() != null));
    }

    /**
     * 攻击力随主人境界上浮。
     *
     * <p>这是规格里"伤害随主人境界"那一句的实现。做法是在父类算完
     * （成长阶段 × 基础值）之后，再乘一个只跟主人境界有关的系数——
     * <b>主人的境界拿不到时按 1.0 处理</b>（主人离线、实体刚生成、
     * 或者被 {@code /summon} 出来还没认主），不会把狼变成一只弱鸡。</p>
     */
    @Override
    protected void refreshGrowthAttributes() {
        super.refreshGrowthAttributes();
        if (this.level().isClientSide()) {
            return;
        }
        int tier = ownerTier();
        double multiplier = tier > 1 ? 1.0D + (tier - 1) * TIER_DAMAGE_BONUS : 1.0D;
        var attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(kind().baseDamage() * growth().statMultiplier() * multiplier);
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide()) {
            refreshGrowthAttributes();
        }
    }

    /** 扑击：短距离直接朝目标跳过去，落地就是一口。 */
    static final class PackTacticsGoal extends Goal {

        private final ContractWolfEntity wolf;
        private int cooldown;

        PackTacticsGoal(ContractWolfEntity wolf) {
            this.wolf = wolf;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.wolf.getTarget();
            if (target == null || !target.isAlive() || !this.wolf.onGround()) {
                return false;
            }
            double dist = this.wolf.distanceToSqr(target);
            return dist >= 4.0D && dist <= 36.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.wolf.onGround();
        }

        @Override
        public void start() {
            LivingEntity target = this.wolf.getTarget();
            if (target == null) {
                return;
            }
            var delta = new net.minecraft.world.phys.Vec3(
                    target.getX() - this.wolf.getX(), 0.0D, target.getZ() - this.wolf.getZ());
            if (delta.lengthSqr() > 1.0E-4D) {
                delta = delta.normalize().scale(0.55D);
            }
            this.wolf.setDeltaMovement(delta.x, 0.42D, delta.z);
            this.wolf.hasImpulse = true;
            this.wolf.playSound(SoundEvents.WOLF_GROWL, 0.85F, 1.15F);
            this.cooldown = 60;
        }
    }

    /** 与妖兽灵狼同一套剪影，但契约后的个体带一点青色灵光。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || !this.isTame() || this.tickCount % 20 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + this.getBbHeight() * 0.7D, this.getZ(),
                    1, 0.2D, 0.15D, 0.2D, 0.0D);
        }
    }
}
