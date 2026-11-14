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
 * <b>泽魅（marsh_wraith）</b> —— 云梦泽的怨魂（普通品阶）。
 *
 * <p>淹死在泽里的修士，怨气不散。破斗笠压眉、脸只剩两点青磷；身体是一堆向下收窄的
 * 破袍，袍底散成 4 条碎布；右手永远挑着一盏青磷灯笼——<b>斗笠 + 灯</b>就是它的剪影。</p>
 *
 * <p>战斗特色：命中带 <b>迟缓 I（2 秒）</b>（被怨气缠住腿），走得很慢（0.30）但很皮实
 * （护甲 1.0）；不主动追远，专在沼泽雾里贴脸。</p>
 *
 * <p>模型：{@code client.MarshWraithModel}（64×64 + _glow，由
 * {@code 工程/角色设计/round2/new-mobs/marsh_wraith/spec.py} 生成）。</p>
 */
public class MarshWraithEntity extends CultivationMonsterEntity {

    public MarshWraithEntity(EntityType<? extends MarshWraithEntity> type, Level level) {
        super(type, level, 8);
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
        return scaledAttributes(MobRank.NORMAL, 14.0D, 4.0D, 0.30D, 32.0D, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 命中：怨气缠身 → 迟缓 I（40 tick）。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        }
        return hit;
    }

    /** 环境粒子：斗笠上飘出的青色魂火（只在客户端画）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(7) == 0) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.6D;
            double y = this.getY() + 0.9D + this.random.nextDouble() * 1.0D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.6D;
            this.level().addParticle(ParticleTypes.SOUL, x, y, z, 0.0D, 0.012D, 0.0D);
        }
    }
}
