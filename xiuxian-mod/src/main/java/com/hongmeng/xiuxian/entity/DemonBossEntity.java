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
 * 血魔老祖：魔道巢穴的最终首领（祭坛召唤）。
 *
 * <p><b>专属实体</b> —— 玩家要求「每一个 boss 都要有自己的模型」，
 * 所以它不是复用某只小怪，而是独立的实体类型，
 * 配专属骨架 {@code client.BloodDemonModel}（三头六臂）与专属贴图。</p>
 *
 * <p>血魔老祖：每打中一次就**吸取生命**补自己，火系完全无效。与它打消耗战是下策，要么秒它、要么别让它碰到你。</p>
 */
public class DemonBossEntity extends CultivationMonsterEntity {

    public DemonBossEntity(EntityType<? extends DemonBossEntity> type, Level level) {
        super(type, level, 85);
        this.setPersistenceRequired();
    }

    @Override
    public MobRank rank() {
        return MobRank.BOSS;
    }

    /** 客户端渲染用的部件组合名。 */
    public String bossVariant() {
        return "demon";
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.BOSS, 26.0, 11.0, 0.3, 54.0, 15.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
                .add(Attributes.ARMOR, 8.0);
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

    /** 血噬：命中即回血。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            this.heal(4.0F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5,
                        living.getZ(), 22, 0.5, 0.5, 0.5, 0.08);
            }
        }
        return hit;
    }
}
