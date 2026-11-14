package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 血魔长老（队长）——血魔一脉真正管事的人。
 *
 * <p>他是精英血魔修士的<b>上位替代</b>，也是人形系里第一只"会吸回来"的怪：
 * 咬中玩家之后按伤害的 {@value #LIFESTEAL_RATIO} 回血，并且身上常驻一层
 * 血色粒子（纯客户端，见 {@link #aiStep()} 的服务端分支——粒子由服务端
 * {@code sendParticles} 广播，客户端不用自己算）。</p>
 *
 * <h3>为什么吸血不违反"单次伤害软上限"</h3>
 *
 * <p>{@link CultivationMonsterEntity#doHurtTarget} 那道 35% 的上限管的是
 * <b>打出去</b>的伤害，本类只在拿到最终的 {@code hurt} 结果之后回血，
 * 所以吸血量天然被同一道上限约束——不会出现"一口把自己吸成满血"。</p>
 *
 * <h3>外观</h3>
 *
 * <p>模型仍是 {@code CultivationHumanoidModel}，靠变体开关部件：
 * 队长档位会自动点亮<b>护肩</b>（{@link MobRank#armorLevel()} ≥ 1），
 * 再叠一件披风（变体自己多要的），所以剪影与精英明显不同一档。
 * 贴图 {@code blood_demon_elder.png}（64×64）。</p>
 */
public class BloodDemonElderEntity extends CultivationMonsterEntity {

    /** 吸血比例：造成伤害的多少变成自己的血。 */
    public static final double LIFESTEAL_RATIO = 0.35D;

    /** 单次吸血的绝对上限，防止以后数值改动导致一次回满。 */
    public static final float LIFESTEAL_CAP = 12.0F;

    public BloodDemonElderEntity(EntityType<? extends BloodDemonElderEntity> type, Level level) {
        super(type, level, 30);
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（队长 ×8 生命 / ×2 攻击 / ×1.10 移速）。
     *
     * <pre>
     *   生命 9  × 8.0   = 72
     *   攻击 3  × 2.0   = 6
     *   移速 0.28 × 1.10 ≈ 0.308     （比执事慢，靠吸血耗）
     *   护甲 5.0
     * </pre>
     *
     * <p>生命刻意比同阶的石傀（8 × 8 = 64 再乘 1.12 体型）略高一档：
     * 长老是"要打一会儿"的怪，而石傀是"推不动"的怪，两者压力来源不同。</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 9.0D, 3.0D, 0.28D, 32.0D, 5.0D)
                // 血魔一脉皮糙肉厚，被推开的幅度小一点
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 吸血：咬中之后按伤害回血。
     *
     * <p>{@code super.doHurtTarget} 返回 true 才回血——没打中就不该回，
     * 否则会出现"隔着空气回血"的观感问题。</p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        float before = target instanceof LivingEntity living ? living.getHealth() : 0.0F;
        boolean hit = super.doHurtTarget(target);
        if (!hit || this.level().isClientSide() || !(target instanceof LivingEntity living)) {
            return hit;
        }
        float dealt = Math.max(0.0F, before - living.getHealth());
        float heal = (float) Math.min(LIFESTEAL_CAP, dealt * LIFESTEAL_RATIO);
        if (heal > 0.0F && this.getHealth() < this.getMaxHealth()) {
            this.heal(heal);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        this.getX(), this.getY() + this.getBbHeight() * 0.8D, this.getZ(),
                        3, 0.25D, 0.15D, 0.25D, 0.02D);
            }
        }
        // 血魔的"血毒"：让玩家掉一点血上限的感觉，用虚弱表达
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), this);
        return hit;
    }

    /** 身上缭绕的血色粒子：告诉玩家"这只在回血"。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 8 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel
                && this.getHealth() < this.getMaxHealth()) {
            // 猩红孢子：原版现成的红色粒子（1.20.1 没有通用的"血雾"粒子，
            // 所以用下界猩红森林那一颗——颜色对得上，且不需要自定义粒子类型）
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.7D,
                    this.getY() + 0.6D + this.random.nextDouble() * 0.9D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.7D,
                    1, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }

    /** 遭遇音比执事更低一档。 */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VINDICATOR_CELEBRATE;
    }

    /** 队长掉落：血道丹药与空冥石。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 30) {
            return new ItemStack(ModItems.KONG_MING_SHI.get(), 1);
        }
        if (this.random.nextInt(100) < 40) {
            return new ItemStack(ModItems.PILL_HUILING.get(), 2);
        }
        return super.rankDrop(rank);
    }
}
