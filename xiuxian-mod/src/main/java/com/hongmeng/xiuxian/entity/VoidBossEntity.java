package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
 * 虚空道主：虚空裂隙的最终首领（祭坛召唤）。
 *
 * <p><b>专属实体</b> —— 玩家要求「每一个 boss 都要有自己的模型」，
 * 所以它不是复用某只小怪，而是独立的实体类型，
 * 配专属骨架 {@code client.VoidLordModel}（浮空体）与专属贴图。</p>
 *
 * <p>虚空道主：**首领品阶**，血量是头目的三倍多。它会把伤害的一部分**折算成虚空侵蚀**（短时失明），而且免疫火焰。</p>
 */
public class VoidBossEntity extends CultivationMonsterEntity {

    public VoidBossEntity(EntityType<? extends VoidBossEntity> type, Level level) {
        super(type, level, 100);
        this.setPersistenceRequired();
    }

    @Override
    public MobRank rank() {
        return MobRank.OVERLORD;
    }

    /** 客户端渲染用的部件组合名。 */
    public String bossVariant() {
        return "void";
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.OVERLORD, 40.0, 15.0, 0.32, 64.0, 18.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 10.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** 虚空侵蚀：命中让人短暂失明。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 20 * 3, 0,
                    false, true));
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5,
                        living.getZ(), 30, 0.6, 0.6, 0.6, 0.2);
            }
        }
        return hit;
    }
}
