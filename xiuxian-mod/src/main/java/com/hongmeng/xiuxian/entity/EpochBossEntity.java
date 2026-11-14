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
 * 傀儡祖师：纪元秘境的最终首领（祭坛召唤）。
 *
 * <p><b>专属实体</b> —— 玩家要求「每一个 boss 都要有自己的模型」，
 * 所以它不是复用某只小怪，而是独立的实体类型，
 * 配专属骨架 {@code client.PuppetMasterModel}（多臂机关体）与专属贴图。</p>
 *
 * <p>纪元一脉的造物之术登峰造极，祖师的身躯由无数傀儡零件拼成，护甲极高、动作却慢。它不惧火，但怕雷。</p>
 */
public class EpochBossEntity extends CultivationMonsterEntity {

    public EpochBossEntity(EntityType<? extends EpochBossEntity> type, Level level) {
        super(type, level, 70);
        this.setPersistenceRequired();
    }

    @Override
    public MobRank rank() {
        return MobRank.BOSS;
    }

    /** 客户端渲染用的部件组合名。 */
    public String bossVariant() {
        return "epoch";
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.BOSS, 22.0, 10.0, 0.26, 52.0, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.ARMOR, 12.0);
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

    /** 重击：把人震退并溅起机关碎屑。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.knockback(1.4, this.getX() - living.getX(),
                    this.getZ() - living.getZ());
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5,
                        living.getZ(), 24, 0.5, 0.5, 0.5, 0.15);
            }
        }
        return hit;
    }
}
