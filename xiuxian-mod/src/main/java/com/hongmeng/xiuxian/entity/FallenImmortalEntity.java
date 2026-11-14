package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.Level;

/**
 * <b>堕仙（fallen_immortal）</b> —— 真世界的堕落仙人（队长品阶）。
 *
 * <p>仙袍还在，下摆撕成三条；头顶的仙环断成两截、倒挂在肩后；右手提一柄断剑，剑尖拖地。
 * 战斗特色：<b>短距突进</b>——目标在 3~10 格时偶尔贴地冲过去（冷却 60 tick），
 * 冲到脸上再挥断剑。真世界首个队长级散怪。</p>
 *
 * <p>数值（设计稿 §3.2.7 基础值 → 队长 ×8.0 生命 / ×2.0 攻击 / ×1.10 移速）：
 * 13 血 / 7.5 攻 / 0.30 速 / 44 追击 / 9 护甲，经验 base 30。</p>
 *
 * <p>模型：{@code client.FallenImmortalModel}（128×64 + _glow，由
 * {@code 工程/角色设计/round2/new-mobs/fallen_immortal/spec.py} 生成）。</p>
 */
public class FallenImmortalEntity extends CultivationMonsterEntity {

    /** 突进冷却（tick）。 */
    private static final int DASH_COOLDOWN = 60;

    private int dashTimer = 0;

    public FallenImmortalEntity(EntityType<? extends FallenImmortalEntity> type, Level level) {
        super(type, level, 30);
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 13.0D, 7.5D, 0.30D, 44.0D, 9.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 断剑命中：额外一点小幅击退（仙剑余威）。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.knockback(0.3D, this.getX() - living.getX(), this.getZ() - living.getZ());
        }
        return hit;
    }

    /** 短距突进 + 环境粒子（粒子只在客户端画）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.dashTimer > 0) {
            this.dashTimer--;
        }
        if (!this.level().isClientSide && this.dashTimer == 0 && this.onGround()) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double d = this.distanceToSqr(target);
                // 目标 3~10 格：贴地冲一段（不是原版跳，是"仙人踏空"式短突进）
                if (d > 9.0D && d < 100.0D && this.random.nextInt(16) == 0) {
                    this.push((target.getX() - this.getX()) * 0.10D, 0.18D,
                            (target.getZ() - this.getZ()) * 0.10D);
                    this.dashTimer = DASH_COOLDOWN;
                }
            }
        }
        if (this.level().isClientSide && this.random.nextInt(10) == 0) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.7D;
            double y = this.getY() + 1.2D + this.random.nextDouble() * 0.8D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.7D;
            this.level().addParticle(ParticleTypes.SOUL, x, y, z, 0.0D, 0.012D, 0.0D);
        }
    }

    /** 队长掉落：45% 真元晶 ×1~2；25% 血珞精 ×1；其余回退基类（回灵丹 + 空冥石不动）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 45) {
            return new ItemStack(ModItems.ZHENYUAN_CRYSTAL.get(), 1 + this.random.nextInt(2));
        }
        if (this.random.nextInt(100) < 25) {
            return new ItemStack(ModItems.XUELUO_ESSENCE.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
