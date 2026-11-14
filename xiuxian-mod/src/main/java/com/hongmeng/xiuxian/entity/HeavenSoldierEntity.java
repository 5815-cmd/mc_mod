package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 天兵（精英）——天外天环境怪主力：原版人形骨架 + 金甲 / 缨盔 / 长戟。
 *
 * <p>定位：仙庭的天门卫卒。人形金甲，右手一杆长戟；近战靠戟尖突刺，
 * <b>命中带击退</b>——它的战斗压力来自"被戳一下就被推开、站位被打乱"，
 * 而不是高伤。数值（精英 ×3 生命 / ×1.5 攻击 / ×1.05 移速）：
 * 生命 18×3=54、攻击 6×1.5=9、移速 0.30×1.05≈0.315、护甲 6。</p>
 *
 * <p>外观：胸前金纹 + 盔顶红缨是发光件（{@code heaven_soldier_glow.png}），
 * 精英品阶的 {@code RankGlowLayer} 会自动画出来。</p>
 */
public class HeavenSoldierEntity extends CultivationMonsterEntity {

    public HeavenSoldierEntity(EntityType<? extends HeavenSoldierEntity> type, Level level) {
        super(type, level, 16);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性（精英倍率）：生命 18×3=54 / 攻击 6×1.5=9 / 移速 0.30×1.05≈0.315 /
     * 追击 40 / 护甲 6。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 18.0D, 6.0D, 0.30D, 40.0D, 6.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 长戟突刺：伤害走父类（含单次伤害软上限），命中把目标推开 + 轻微上挑。
     *
     * <p>击退用 {@code push} 而不是附魔：附魔的击退是"玩家属性"，
     * 怪的普攻带击退必须自己给速度，否则不同装备的玩家体验不一致。</p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            if (target instanceof LivingEntity living) {
                double dx = living.getX() - this.getX();
                double dz = living.getZ() - this.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1.0E-4D) {
                    living.push(dx / len * 0.65D, 0.30D, dz / len * 0.65D);
                }
            }
            this.playSound(SoundEvents.ANVIL_LAND, 0.7F, 1.35F);
        }
        return hit;
    }

    /** 金甲反光：偶尔掉一点金色电火花（天外天的"仙气"）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 20 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    this.getY() + 0.6D + this.random.nextDouble() * 0.6D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    1, 0.02D, 0.02D, 0.02D, 0.01D);
        }
    }

    /** 掉落（精英档）：25% 灵晶，其余走父类（精英 = 回灵丹）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 25) {
            return new ItemStack(ModItems.SPIRIT_CRYSTAL.get());
        }
        return super.rankDrop(rank);
    }
}
