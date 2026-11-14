package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 剑冢守灵（队长）——剑冢里那柄断剑养出来的剑意。
 *
 * <p>它的人形是"一副空壳撑着一柄剑"，所以整套数值都在表达同一件事：
 * <b>慢、硬、越打越快</b>。</p>
 * <ul>
 *   <li><b>慢</b>：基础移速 0.24（人形怪里最慢的一档），追击全靠 followRange 40；</li>
 *   <li><b>硬</b>：护甲 9，击退抗性 0.8——一具被剑气撑着的躯壳推不动；</li>
 *   <li><b>剑意</b>：血量掉到 {@value #SWORD_INTENT_RATIO} 以下时，
 *       {@code MOVEMENT_SPEED} 上挂一条永久修饰符（一次性，见
 *       {@link #applySwordIntent()}），从"慢吞吞"变成"贴脸追"。
 *       这是它的招牌机制，也是玩家"该在什么时候收手"的判断点。</li>
 * </ul>
 *
 * <p>模型仍是 {@code CultivationHumanoidModel}：队长档位自动点亮护肩，
 * 再额外要一柄<b>背后剑</b>（变体的 {@code blade}）——剑冢守灵身上没剑就说不通了。
 * 贴图 {@code sword_tomb_guardian.png}（64×64）。</p>
 */
public class SwordTombGuardianEntity extends CultivationMonsterEntity {

    /** 掉到这个血量比例以下就"剑意爆发"。 */
    public static final float SWORD_INTENT_RATIO = 0.5F;

    /** 剑意爆发的移速加成（乘在基础移速上，百分比）。 */
    public static final double SWORD_INTENT_SPEED_BONUS = 0.60D;

    /** 修饰符固定 id：重复触发不会叠加（同一只怪一生只爆一次）。 */
    private static final UUID SWORD_INTENT_ID =
            UUID.nameUUIDFromBytes("hongmeng_sword_intent".getBytes(StandardCharsets.UTF_8));

    /** 已经爆发过就不再爆。 */
    private boolean swordIntent;

    public SwordTombGuardianEntity(EntityType<? extends SwordTombGuardianEntity> type, Level level) {
        super(type, level, 35);
    }

    @Override
    public MobRank rank() {
        return MobRank.CAPTAIN;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（队长 ×8 生命 / ×2 攻击 / ×1.10 移速）。
     *
     * <pre>
     *   生命 11  × 8.0  = 88
     *   攻击 3.5 × 2.0  = 7
     *   移速 0.24 × 1.10 ≈ 0.264     （全部人形怪里最慢）
     *   护甲 9.0，击退抗性 0.8
     * </pre>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.CAPTAIN, 11.0D, 3.5D, 0.24D, 40.0D, 9.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 剑意爆发 + 剑气粒子。
     *
     * <p>修饰符用 {@code MULTIPLY_BASE}：加的是"基础移速的 60%"，
     * 与 {@link MobRank} 的移速倍率、{@code MobScaling} 的境界倍率互不干扰
     * （那两条走的是不同的修饰符，叠加顺序由原版保证）。</p>
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        if (!this.swordIntent
                && this.getHealth() <= this.getMaxHealth() * SWORD_INTENT_RATIO) {
            this.applySwordIntent();
        }
        if (this.swordIntent && this.tickCount % 6 == 0
                && this.level() instanceof ServerLevel serverLevel) {
            // 剑意：贴着身侧往上走的一道细碎白芒
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    this.getY() + 0.4D + this.random.nextDouble() * 1.4D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    1, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }

    /** 挂上剑意的移速修饰符，并给一声"剑鸣"。 */
    private void applySwordIntent() {
        this.swordIntent = true;
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SWORD_INTENT_ID);
            speed.addPermanentModifier(new AttributeModifier(SWORD_INTENT_ID,
                    "hongmeng_sword_intent", SWORD_INTENT_SPEED_BONUS,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
        this.playSound(SoundEvents.ANVIL_LAND, 1.0F, 1.6F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    6, 0.5D, 0.35D, 0.5D, 0.02D);
        }
    }

    /** 剑意是否已经爆发（给渲染/HUD 留的查询口）。 */
    public boolean hasSwordIntent() {
        return this.swordIntent;
    }

    /** 遭遇音：铁器相击，不是活物的叫声。 */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ANVIL_PLACE;
    }

    /** 队长掉落：灵铁与空冥石（剑冢里全是炼器材料）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 35) {
            return new ItemStack(ModItems.SPIRIT_IRON_INGOT.get(), 1);
        }
        if (this.random.nextInt(100) < 20) {
            return new ItemStack(ModItems.KONG_MING_SHI.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
