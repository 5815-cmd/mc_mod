package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * <b>终界使徒（end_apostle）</b> —— 末地的虚无朝圣者（精英品阶）。
 *
 * <p>一袭无脚黑袍飘在虚空里，尖顶兜帽压住虚空脸、四只眼睛不与脸同步转动，
 * 背后悬着一圈断掉的终界门环。它不追人，它<b>先凝视，再靠近</b>——
 * 命中时用「虚空凝视」让目标失明 2 秒（40 tick）。</p>
 *
 * <p>数值（设计稿 §3.1.7 基础值 → 精英 ×3.0 生命 / ×1.5 攻击 / ×1.05 移速）：
 * 22 血 / 6.0 攻 / 0.28 速 / 40 追击 / 6 护甲，经验 base 18。</p>
 *
 * <p>模型：{@code client.EndApostleModel}（128×64 + _glow，由
 * {@code 工程/角色设计/round2/new-mobs/end_apostle/spec.py} 生成）。</p>
 */
public class EndApostleEntity extends CultivationMonsterEntity {

    public EndApostleEntity(EntityType<? extends EndApostleEntity> type, Level level) {
        super(type, level, 18);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 22.0D, 6.0D, 0.28D, 40.0D, 6.0D);
    }

    @Override
    protected void registerGoals() {
        // 「先凝视再靠近」：凝视（LookAt）优先级高于追击（Melee），
        // 于是它会先站定看玩家一阵，再缓步压上来；Float 兜底防沉虚空。
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.9D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 虚空凝视：命中使目标失明 40 tick（2 秒）。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        }
        return hit;
    }

    /** 环境粒子：末地紫色虚空微粒（只在客户端画）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(8) == 0) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.7D;
            double y = this.getY() + 0.9D + this.random.nextDouble() * 1.1D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.7D;
            this.level().addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.01D, 0.0D);
        }
    }

    /** 精英掉落：55% 灵晶 ×1~2；20% 末影珍珠 ×1；其余回退基类（回灵丹）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 55) {
            return new ItemStack(ModItems.SPIRIT_CRYSTAL.get(), 1 + this.random.nextInt(2));
        }
        if (this.random.nextInt(100) < 20) {
            return new ItemStack(Items.ENDER_PEARL, 1);
        }
        return super.rankDrop(rank);
    }
}
