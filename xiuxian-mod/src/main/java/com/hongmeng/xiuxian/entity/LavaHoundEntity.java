package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
 * 熔岩犬（精英）——下界的熔岩猎犬。
 *
 * <p><b>复用灵狼骨架</b>（{@code SpiritWolfRenderer.WolfBody}）：四足扑击、
 * 尾巴摆、跑动步伐全部同一套，只有贴图换成"黑曜石皮 + 熔岩裂纹 + 橙红眼"。
 * 这样它一眼就是"狼形妖兽"，但玩家绝不会把下界的它和纪元草原的灵狼看混。</p>
 *
 * <p>属性：生命 22×3 = 66、攻击 7×1.5 = 10.5、移速 0.36×1.05 ≈ 0.378
 * （比灵狼更快、更硬，是下界里"追着你咬"的那只）。
 * 命中<b>点燃 3 秒</b>；自身火焰免疫由 {@code ModEntitiesExtra} 注册的
 * {@code .fireImmune()} 负责（共享文件，本类不改）。</p>
 *
 * <p>环境粒子：身上偶尔掉火星（{@link ParticleTypes#FLAME}），
 * 跑动时脚下留下少量余烬——下界里远远就能看见一团火星在靠近。</p>
 */
public class LavaHoundEntity extends CultivationMonsterEntity {

    /** 咬中点燃时长（秒）。 */
    public static final int IGNITE_SECONDS = 3;

    public LavaHoundEntity(EntityType<? extends LavaHoundEntity> type, Level level) {
        super(type, level, 12);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（精英 ×3 生命 / ×1.5 攻击 / ×1.05 移速）。
     *
     * <pre>
     *   生命 22 × 3.0 = 66
     *   攻击 7  × 1.5 = 10.5
     *   移速 0.36 × 1.05 ≈ 0.378   （下界里跑得比灵狼快）
     *   护甲 1.0
     * </pre>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 22.0D, 7.0D, 0.36D, 36.0D, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 咬中点燃 3 秒（火免目标不吃这一层）。
     *
     * <p>参考 {@code CrimsonFlameWolfEntity#doHurtTarget} 的写法：
     * 先走父类结算伤害（父类有单次伤害软上限），再补点燃。</p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && !living.fireImmune()) {
            living.setSecondsOnFire(IGNITE_SECONDS);
            this.playSound(SoundEvents.FLINTANDSTEEL_USE, 0.8F, 1.4F);
        }
        return hit;
    }

    /** 火星：每 5 tick 从背部冒 1~2 颗（下界里"一团火星在靠近"的辨识度）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 5 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.7D,
                    this.getY() + 0.55D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.7D,
                    2, 0.10D, 0.10D, 0.10D, 0.01D);
        }
    }
}
