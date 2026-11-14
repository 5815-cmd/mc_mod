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
 * 万兽之主：妖兽秘境的最终首领（祭坛召唤）。
 *
 * <p><b>专属实体</b> —— 玩家要求「每一个 boss 都要有自己的模型」，
 * 所以它不是复用某只小怪，而是独立的实体类型，
 * 配专属骨架 {@code client.BeastLordModel}（四足兽形）与专属贴图。</p>
 *
 * <p>万兽之主：血最厚、攻击最狠，全身覆盖兽骨甲。它跑得比谁都快，而且**不免疫火** —— 用火系法术打它最划算。</p>
 */
public class BeastBossEntity extends CultivationMonsterEntity {

    public BeastBossEntity(EntityType<? extends BeastBossEntity> type, Level level) {
        super(type, level, 80);
        this.setPersistenceRequired();
    }

    @Override
    public MobRank rank() {
        return MobRank.BOSS;
    }

    /** 客户端渲染用的部件组合名。 */
    public String bossVariant() {
        return "beast";
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.BOSS, 30.0, 12.0, 0.34, 56.0, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 6.0);
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
        return false;
    }

    /** 撕咬：附带流血（短时虚弱）。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS, 20 * 5, 0,
                    false, true));
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5,
                        living.getZ(), 18, 0.4, 0.4, 0.4, 0.1);
            }
        }
        return hit;
    }
}
