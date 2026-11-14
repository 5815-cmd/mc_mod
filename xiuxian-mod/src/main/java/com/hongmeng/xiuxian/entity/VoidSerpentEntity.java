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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 虚空蛟（头目）：从虚空裂隙钻出来的长虫，一身的虚空毒。
 *
 * <p>模型是<b>八节串起来的蛇身</b>（见 {@code VoidSerpentRenderer.SerpentBody}）：
 * 每一节都挂在前一节下面，靠一条行波公式扭动，所以它游起来是真的在"蜿蜒"，
 * 而不是整体平移——这是本模组第一个非人形、非四足、非鸟类的剪影。</p>
 *
 * <p>数值口径：生命 30×25 = 750、攻击 5×3 = 15，是头目档的标准血牛；
 * 咬中附带 5 秒中毒，配上一身虚空粒子，玩家远远看见那一串飘着的紫黑光点，
 * 就知道该绕路还是该拔剑。</p>
 */
public class VoidSerpentEntity extends CultivationMonsterEntity {

    public VoidSerpentEntity(EntityType<? extends VoidSerpentEntity> type, Level level) {
        super(type, level, 40);
    }

    @Override
    public MobRank rank() {
        return MobRank.BOSS;
    }

    /** 基础属性；击退抗性 0.6——蛇身贴地，比人形怪难推得多。 */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.BOSS, 30.0D, 5.0D, 0.30D, 40.0D, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 蛟毒：咬中附加中毒。
     *
     * <p>数值层依旧由 {@code super.doHurtTarget} 兜底（单次伤害不超过目标生命的
     * {@link CultivationMonsterEntity#MAX_HIT_RATIO}），这里只挂效果。
     * 蛟自己属亡灵（父类设定），原版的中毒判定对亡灵无效，所以它不会被自己的毒反噬。</p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), this);
        }
        return hit;
    }

    /** 虚空粒子：沿身体拖一条紫黑尾迹，让"它有多长"一眼可见。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 6 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            double back = 1.5D + this.random.nextDouble() * 3.0D;
            double yaw = Math.toRadians(this.getYRot());
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX() + Math.sin(yaw) * back,
                    this.getY() + 0.35D,
                    this.getZ() - Math.cos(yaw) * back,
                    3, 0.25D, 0.15D, 0.25D, 0.02D);
        }
    }

    /** 低沉的嘶声：比人形怪的吼叫更阴。 */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 200 == 0 && this.getTarget() != null) {
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 0.45F, 1.6F);
        }
    }

    /** 头目掉落：龙息与空冥石，符合"虚空"的路子。 */
    @Override
    protected net.minecraft.world.item.ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 25) {
            return new net.minecraft.world.item.ItemStack(
                    com.hongmeng.xiuxian.item.ModItems.KONG_MING_SHI.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
