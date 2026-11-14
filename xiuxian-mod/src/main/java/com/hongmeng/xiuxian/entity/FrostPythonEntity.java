package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 玄冰蟒（队长）——虚空蛟的"寒冰亚种"。
 *
 * <p>与赤炎狼同一个套路：<b>骨架、游动动画、行波扭动全部复用</b>
 * （{@code VoidSerpentRenderer.SerpentModel} 的八节身体），
 * 玩家一眼能看出"这是那条长虫"，但颜色、品阶、手感全不一样：</p>
 *
 * <table border="1">
 *   <caption>与虚空蛟的对照</caption>
 *   <tr><th></th><th>虚空蛟（头目）</th><th>玄冰蟒（队长）</th></tr>
 *   <tr><td>生命</td><td>30 × 25 = 750</td><td>13 × 8 = 104</td></tr>
 *   <tr><td>攻击</td><td>5 × 3 = 15</td><td>3.2 × 2 = 6.4</td></tr>
 *   <tr><td>咬中效果</td><td>中毒 5 秒</td><td>缓慢 2 秒（冻住脚）</td></tr>
 *   <tr><td>粒子</td><td>紫色传送门</td><td>雪片</td></tr>
 *   <tr><td>贴图</td><td>void_serpent.png</td><td>frost_python.png</td></tr>
 * </table>
 *
 * <p>命中箱与虚空蛟<b>逐字相同</b>（1.6 × 1.4）：模型是同一份八节蛇身，
 * 命中箱就必须跟着模型走——「队长比头目小一圈」由
 * {@code MobRank.scaleXZ()} 在渲染层表达，不重复写进命中箱
 * （这条口径由 {@code tools/check_beast_hitboxes.py} 的"复用骨架"一节强制）。</p>
 *
 * <p><b>可契约性</b>：它继承 {@code VoidSerpentEntity}，于是
 * {@code BeastManager.contractTargetOf} 会把它认成蛟龙的来源妖兽之一。
 * 这是刻意保留的——"寒冰蛟"本来就该是蛟。</p>
 */
public class FrostPythonEntity extends VoidSerpentEntity {

    /** 咬中附加的缓慢持续（tick）。 */
    public static final int CHILL_TICKS = 40;

    public FrostPythonEntity(EntityType<? extends FrostPythonEntity> type, Level level) {
        super(type, level);
        // 父类按头目档给了 40×8 = 320 经验；玄冰蟒是队长，重算成 12 × 4.0 = 48。
        this.xpReward = Math.max(1, (int) Math.round(12 * rank().xpMultiplier()));
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（队长 ×8 生命 / ×2 攻击 / ×1.10 移速）。
     *
     * <pre>
     *   生命 13  × 8.0  = 104
     *   攻击 3.2 × 2.0  = 6.4
     *   移速 0.26 × 1.10 ≈ 0.286     （比虚空蛟慢：它是伏击的，不是追的）
     *   护甲 5.0，击退抗性 0.6
     * </pre>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 13.0D, 3.2D, 0.26D, 36.0D, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    /**
     * 寒毒：咬中附加缓慢。
     *
     * <p>刻意<b>不</b>调用父类的中毒版本——玄冰蟒不该让玩家中毒，
     * 那会与"冰"的主题打架。父类的那一层只剩伤害结算（单次伤害软上限照旧有效）。</p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            // 先把父类挂上的中毒去掉，再换成缓慢：玄冰蟒身上没有毒
            living.removeEffect(MobEffects.POISON);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL_TICKS, 0),
                    this);
        }
        return hit;
    }

    /** 寒气尾迹：雪片而不是紫色传送门。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 6 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            double back = 1.5D + this.random.nextDouble() * 3.0D;
            double yaw = Math.toRadians(this.getYRot());
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX() + Math.sin(yaw) * back,
                    this.getY() + 0.35D,
                    this.getZ() - Math.cos(yaw) * back,
                    3, 0.25D, 0.15D, 0.25D, 0.01D);
        }
    }

    /** 嘶声换成"结冰"的脆响。 */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 200 == 0 && this.getTarget() != null) {
            this.playSound(SoundEvents.GLASS_BREAK, 0.5F, 1.8F);
        }
    }

    /** 队长掉落：寒冰藤花瓣与空冥石。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 35) {
            return new ItemStack(ModItems.HANBING_TENG_PETAL.get(), 1);
        }
        if (this.random.nextInt(100) < 20) {
            return new ItemStack(ModItems.KONG_MING_SHI.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
