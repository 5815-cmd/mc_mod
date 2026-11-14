package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
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
 * <b>晶蛛（crystal_spider）</b> —— 灵脉山谷的矿脉化蛛（普通品阶）。
 *
 * <p>灵脉旁的矿石被灵气养出了形：低伏八腿、浑圆腹部，背上驮着 3 簇会闪的灵晶
 * （中间青蓝、两侧紫），脸上一排 4 只发光眼。<b>宽背晶簇 + 张开的腿</b>是它的剪影。</p>
 *
 * <p>战斗特色：<b>扑咬</b>——目标在 2~7 格时偶尔蹬地突进（不是原版跳，是贴着地面弹一下），
 * 命中附加一次小击退；腿多但甲薄（护甲 1.5），怕被围着打。</p>
 *
 * <p>模型：{@code client.CrystalSpiderModel}（128×64 + _glow，由
 * {@code 工程/角色设计/round2/new-mobs/crystal_spider/spec.py} 生成）。</p>
 */
public class CrystalSpiderEntity extends CultivationMonsterEntity {

    /** 扑咬冷却（tick）。 */
    private static final int POUNCE_COOLDOWN = 80;

    private int pounceTimer = 0;

    public CrystalSpiderEntity(EntityType<? extends CrystalSpiderEntity> type, Level level) {
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
        return scaledAttributes(MobRank.NORMAL, 16.0D, 4.5D, 0.32D, 28.0D, 1.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 命中：蛛牙一咬，补一脚小击退。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.knockback(0.35D, this.getX() - living.getX(), this.getZ() - living.getZ());
        }
        return hit;
    }

    /** 扑咬 + 灵晶碎屑粒子（粒子只在客户端画）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.pounceTimer > 0) {
            this.pounceTimer--;
        }
        if (!this.level().isClientSide && this.pounceTimer == 0 && this.onGround()) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double d = this.distanceToSqr(target);
                // 2~7 格之间偶尔蹬地突进（贴地，不是原版跳）
                if (d > 4.0D && d < 49.0D && this.random.nextInt(20) == 0) {
                    this.push((target.getX() - this.getX()) * 0.12D, 0.32D,
                            (target.getZ() - this.getZ()) * 0.12D);
                    this.pounceTimer = POUNCE_COOLDOWN;
                }
            }
        }
        if (this.level().isClientSide && this.random.nextInt(9) == 0) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D;
            double y = this.getY() + 1.1D + this.random.nextDouble() * 0.3D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D;
            this.level().addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.015D, 0.0D);
        }
    }
}
