package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 魔道统领：魔道一脉坐镇一方的头目，头目品阶。
 *
 * <p>一刀下去能打穿筑基修士的护体灵光，火系法术对它完全无效——
 * 想拿下它，得先有金丹以上的硬实力，或者一支肯配合的队伍。</p>
 */
public class DemonGeneralEntity extends CultivationMonsterEntity {

    public DemonGeneralEntity(EntityType<? extends DemonGeneralEntity> type, Level level) {
        super(type, level, 60);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
    }

    @Override
    public MobRank rank() {
        return MobRank.BOSS;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.BOSS, 16.0D, 9.0D, 0.30D, 48.0D, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 魔体自燃不伤：火、岩浆、烈焰弹统统无效。 */
    @Override
    public boolean fireImmune() {
        return true;
    }

    /** 重击：劈中就把人砸退，并留下一道魔气。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.knockback(1.2D, this.getX() - living.getX(), this.getZ() - living.getZ());
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        12, 0.3D, 0.4D, 0.3D, 0.02D);
            }
        }
        return hit;
    }
}
