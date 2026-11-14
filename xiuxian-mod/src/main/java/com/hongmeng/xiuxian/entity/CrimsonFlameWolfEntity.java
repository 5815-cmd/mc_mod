package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 赤炎狼（精英）——灵狼的火行亚种。
 *
 * <p><b>复用灵狼那一整套骨架与 AI</b>（{@code SpiritWolfRenderer.WolfBody} +
 * {@link SpiritWolfEntity} 的扑击目标），所以它跑起来、扑起来的动作和灵狼
 * 一模一样——这是刻意的：玩家一眼就该认出"这是狼"，而不是"又是一只没见过的怪"。
 * 区别全在四件事上：</p>
 * <ol>
 *   <li><b>品阶</b>：精英（×3 生命 / ×1.5 攻击），不再是随手可杀的杂鱼；</li>
 *   <li><b>火免</b>：注册时带 {@code fireImmune()}，岩浆里也追得动；</li>
 *   <li><b>点燃</b>：扑中之后给目标 {@value #IGNITE_SECONDS} 秒着火；</li>
 *   <li><b>外观</b>：贴图 {@code crimson_flame_wolf.png}（64×32，与原版狼同一套 UV）。</li>
 * </ol>
 *
 * <p>属性表不复用灵狼的 {@code createAttributes()}：那一个是普通档的
 * （生命 10、攻击 4），这里的 8 × 3 = 24 才符合精英档。</p>
 *
 * <p><b>可契约性</b>：它继承 {@code SpiritWolfEntity}，因此
 * {@code BeastManager.contractTargetOf} 会把它认成"可以契约成契约灵狼的妖兽"——
 * 这是刻意的，火狼本来也是狼。</p>
 */
public class CrimsonFlameWolfEntity extends SpiritWolfEntity {

    /** 扑中之后目标着火多少秒。 */
    public static final int IGNITE_SECONDS = 3;

    public CrimsonFlameWolfEntity(EntityType<? extends CrimsonFlameWolfEntity> type, Level level) {
        super(type, level);
        // 父类按普通档给了 5×1.0 = 5 经验；赤炎狼是精英，经验跟着品阶走（10 × 2.0 = 20）。
        // 这一句是必须的：xpReward 只在构造器里算一次，之后不会被品阶表回头修正——
        // 不重算的话，一只精英狼会只给普通狼的经验。
        this.xpReward = Math.max(1, (int) Math.round(
                10 * rank().xpMultiplier()));
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（精英 ×3 / ×1.5 / ×1.05）。
     *
     * <pre>
     *   生命 8  × 3.0  = 24
     *   攻击 4  × 1.5  = 6
     *   移速 0.35 × 1.05 ≈ 0.368    （比灵狼还快一点，火行本色）
     * </pre>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 8.0D, 4.0D, 0.35D, 36.0D, 1.0D);
    }

    /**
     * 扑中即点燃。
     *
     * <p>只加"着火"这一件事，不加额外伤害：伤害那层已经由父类的
     * {@code doHurtTarget}（35% 单次软上限）兜住了，再叠一层容易失控。</p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.setSecondsOnFire(IGNITE_SECONDS);
        }
        return hit;
    }

    /** 身上飘火星：远远看过去就知道那只是"火的那只狼"。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 5 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.7D,
                    this.getY() + 0.25D + this.random.nextDouble() * 0.5D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.7D,
                    1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
    }

    /** 火狼的叫声比灵狼更低。 */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_GROWL;
    }

    /** 精英掉落：火行妖兽身上带火种（用现成的火焰花代表）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 30) {
            return new ItemStack(ModItems.HUOYAN_HUA_ITEM.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
