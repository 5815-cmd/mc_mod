package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * <b>魔蛛（demon_spider）</b> —— 黑石魔窟的八腿猎手（普通品阶）。
 *
 * <p>设计真源：{@code 工程/角色设计/round2/新怪物第二梯队设计稿.md} §3.5。
 * 八条长腿撑起一颗黑曜石身体，腹部中段一圈熔岩裂（发光），头胸前端一排 4 只发光眼；
 * 它不追，它在必经之路上结网等——所以它的攻击方式是<b>咬住就不松口的缠网</b>。</p>
 *
 * <p>战斗特色：</p>
 * <ul>
 *   <li>近战命中附加 <b>缓慢 II 40t</b>（"缠网"）；</li>
 *   <li>每 {@value #WEB_SPIT_COOLDOWN}t 一次<b>喷网</b>：3 格内目标缓慢 III + 挖掘疲劳 40t
 *       （视觉是 {@code poof} + 白色短线段粒子，不真的放蛛网方块）；</li>
 *   <li>普通品阶但靠发光件立辨识度（4 眼 + 熔岩腹）→ {@link #forceGlow()} 返回 true。</li>
 * </ul>
 *
 * <p>模型：{@code client.DemonSpiderModel}（128×64 + _glow，由
 * {@code 工程/角色设计/round2/new-mobs/demon_spider/spec.py} 生成）。</p>
 *
 * <p>数值（设计稿 §3.5.7）：生命 14 / 攻击 4.0 / 移速 0.32 / 追击 32 / 护甲 2 / 经验 8。</p>
 */
public class DemonSpiderEntity extends CultivationMonsterEntity {

    /** 缠网（命中缓慢 II）持续时间（tick）。 */
    private static final int WEB_SLOW_TICKS = 40;

    /** 喷网 special 冷却（tick，设计稿：200t）。 */
    private static final int WEB_SPIT_COOLDOWN = 200;

    /** 喷网判定半径（格，设计稿：3 格内）。 */
    private static final double WEB_SPIT_RANGE = 3.0D;

    private int webCooldown = 0;

    public DemonSpiderEntity(EntityType<? extends DemonSpiderEntity> type, Level level) {
        super(type, level, 8);
    }

    @Override
    public MobRank rank() {
        return MobRank.NORMAL;
    }

    /** 普通品阶但靠发光件立辨识度（4 眼 + 熔岩腹）：强制绘制发光层。 */
    @Override
    public boolean forceGlow() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.NORMAL, 14.0D, 4.0D, 0.32D, 32.0D, 2.0D);
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

    /** 命中：蛛牙一咬 = 缠网（缓慢 II 40t）。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, WEB_SLOW_TICKS, 1));
        }
        return hit;
    }

    /**
     * 客户端：熔岩腹偶尔冒 1 颗余烬粒子（纯视觉，不需要同步）；
     * 服务端：维持喷网冷却，目标进 3 格就喷一次网（缓慢 III + 挖掘疲劳）。
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(14) == 0) {
                double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.45D;
                double y = this.getY() + 0.42D + this.random.nextDouble() * 0.15D;
                double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.45D;
                this.level().addParticle(ParticleTypes.LAVA, x, y, z, 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        if (this.webCooldown > 0) {
            this.webCooldown--;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.distanceToSqr(target) > WEB_SPIT_RANGE * WEB_SPIT_RANGE) {
            return;
        }
        this.webCooldown = WEB_SPIT_COOLDOWN;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0));
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF,
                    target.getX(), target.getY() + 1.0D, target.getZ(),
                    6, 0.4D, 0.4D, 0.4D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 0.6D, this.getZ(),
                    8, 0.5D, 0.3D, 0.5D, 0.02D);
        }
    }

    /** 掉落（设计稿 §3.5.7）：55% 线 ×1~3 / 25% 煤 ×0~2 / 15% 空冥精 / 其余灵叶。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        int roll = this.random.nextInt(100);
        if (roll < 55) {
            return new ItemStack(Items.STRING, 1 + this.random.nextInt(3));
        }
        if (roll < 80) {
            return new ItemStack(Items.COAL, this.random.nextInt(3));
        }
        if (roll < 95) {
            return new ItemStack(com.hongmeng.xiuxian.item.ModItems.KONGMING_ESSENCE.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
