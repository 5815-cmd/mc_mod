package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
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
 * 虚空道主：从界外虚空里走出来的存在，首领品阶——本模组最高一档的野怪。
 *
 * <p>完全免疫火焰，也完全免疫击退：推不动、烧不着、躲不掉。
 * 血量是头目的三倍多，一击能送化神修士回城。它出现在哪，哪就是禁区。</p>
 *
 * <p>击杀它的玩家会收到一条全服公告——这种级别的存在死了，值得记一笔。</p>
 */
public class VoidOverlordEntity extends CultivationMonsterEntity {

    public VoidOverlordEntity(EntityType<? extends VoidOverlordEntity> type, Level level) {
        super(type, level, 200);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
    }

    @Override
    public MobRank rank() {
        return MobRank.OVERLORD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.OVERLORD, 20.0D, 10.0D, 0.34D, 64.0D, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 虚空之躯：无视一切火焰伤害。 */
    @Override
    public boolean fireImmune() {
        return true;
    }

    /** 虚空压制：命中的目标会被短暂拖慢。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        24, 0.4D, 0.5D, 0.4D, 0.06D);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 火系伤害连擦伤都留不下
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            this.playSound(SoundEvents.WITHER_DEATH, 1.0F, 0.7F);
            // 击杀首领值得全区知道一声
            Component message = Component.literal("\u00a76虚空道主 \u00a77已被 ")
                    .append(source.getEntity() instanceof Player killer
                            ? killer.getDisplayName()
                            : Component.literal("\u00a77某位修士"))
                    .append(Component.literal("\u00a77 斩杀！"));
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(message, false);
        }
    }
}
