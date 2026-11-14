package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
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
 * 石傀（队长）：被灵气泡活了的守山石像。
 *
 * <p>它的设计目标只有一个——<b>逼玩家换打法</b>。</p>
 * <ul>
 *   <li>生命 45×8 = 360，护甲 12：拿木剑慢慢磨要磨到天亮，必须上法宝或功法；</li>
 *   <li>击退抗性 0.95：推不动、打不飞，没法用"打了就跑"拖死它；</li>
 *   <li>移速 0.19×1.10 ≈ 0.21：慢得离谱，所以你永远可以选择不打它——
 *       它站在那儿就是一道"此路不通"的告示。</li>
 * </ul>
 *
 * <p>石傀是石头不是鬼，所以 {@link #getMobType()} 特意<b>不</b>跟随父类的亡灵设定：
 * 拿圣光附魔砍石头没有加成，治疗药水也不会反过来伤它。</p>
 */
public class StoneGolemEntity extends CultivationMonsterEntity {

    public StoneGolemEntity(EntityType<? extends StoneGolemEntity> type, Level level) {
        super(type, level, 25);
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    /** 基础属性；击退抗性不在 {@code scaledAttributes} 里，单独补一条。 */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 45.0D, 6.0D, 0.19D, 32.0D, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D);
    }

    /** 傀儡之属，非亡灵：不踩圣光的便宜，也不怕治疗。 */
    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    /** 推不动的石头：玩家挤不开它，它也不会被羊群顶走。 */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.9D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 挨打时掉石屑，走路时偶尔漏出胸口灵核的光。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 12 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.6D, this.getZ(),
                    1, 0.18D, 0.12D, 0.18D, 0.005D);
        }
    }

    /** 砸人的闷响：低音、慢半拍，听起来就很重。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide()) {
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.65F);
        }
        return hit;
    }

    /** 队长级掉落：多给一块灵铁锭，玩家会更愿意啃这块硬骨头。 */
    @Override
    protected net.minecraft.world.item.ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 30) {
            return new net.minecraft.world.item.ItemStack(
                    com.hongmeng.xiuxian.item.ModItems.SPIRIT_IRON_INGOT.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
