package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * <b>虚空水母（void_jelly）</b> —— 虚空浮岛 / 归墟的漂游体（普通品阶）。
 *
 * <p>虚空里泡大的水母：钟形伞盖分 3 层、伞缘一圈紫色虚空光，4 条两段长触须垂到
 * 地面，伞盖正面一只紫青色的虚空眼。<b>伞盖 + 垂须</b>是它的剪影。</p>
 *
 * <p>战斗特色：触须命中带 <b>侵蚀</b>（缓慢 + 虚弱，各 3 秒）；移速很慢（0.26），
 * 但护甲 2.0，是虚空里最"磨人"的小怪。飘浮感由模型动画的伞盖开合 + 整身起伏做，
 * 渲染时最低点仍贴地（y=24）。</p>
 *
 * <p>模型：{@code client.VoidJellyModel}（64×64 + _glow，由
 * {@code 工程/角色设计/round2/new-mobs/void_jelly/spec.py} 生成）。</p>
 */
public class VoidJellyEntity extends CultivationMonsterEntity {

    public VoidJellyEntity(EntityType<? extends VoidJellyEntity> type, Level level) {
        super(type, level, 9);
    }

    @Override
    public MobRank rank() {
        return MobRank.NORMAL;
    }

    /** 普通品阶但靠发光件立辨识度（提灯/灵晶/虚空眼）：强制绘制发光层。 */
    @Override
    public boolean forceGlow() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.NORMAL, 18.0D, 4.0D, 0.26D, 30.0D, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 命中：虚空侵蚀 → 缓慢 I + 虚弱 I（各 60 tick）。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
        return hit;
    }

    /** 环境粒子：伞盖周围飘散的虚空微粒（只在客户端画）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(6) == 0) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.9D;
            double y = this.getY() + 0.8D + this.random.nextDouble() * 0.6D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.9D;
            this.level().addParticle(ParticleTypes.PORTAL, x, y, z, 0.0D, -0.01D, 0.0D);
        }
    }
}
