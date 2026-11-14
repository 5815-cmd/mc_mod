package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;

/**
 * 虚空行者（头目）——从虚空裂隙里走出来的"人形空洞"。
 *
 * <p>与既有的虚空道主（首领）拉开距离的做法不是"数值小一号"，而是
 * <b>换一套位移方式</b>：</p>
 * <ul>
 *   <li><b>瞬移</b>：被咬中时有 {@value #BLINK_CHANCE_PERCENT}% 的概率闪现到攻击者背后
 *       4 格（原版 {@code randomTeleport}，会自己找安全落点），
 *       于是"贴脸连打"这件事对它不太管用——玩家得学会打完就撤一步；</li>
 *   <li><b>虚空粒子</b>：闪现前后各撒一串传送门粒子，给玩家一个"它要走了"的预告，
 *       不是无征兆的瞬移（无征兆的瞬移只会让人觉得"这怪在瞬移挂"）。</li>
 * </ul>
 *
 * <p>模型是 {@code CultivationHumanoidModel} 的<b>部件全开</b>变体
 * （护肩 + 披风 + 剑 + 伸爪架势）：头目档位本来就会点亮护肩与披风，
 * 剑与架势是本变体额外要的。贴图 {@code void_walker.png}（64×64）。</p>
 */
public class VoidWalkerEntity extends CultivationMonsterEntity {

    /** 每次被打中时闪现的概率（百分比）。 */
    public static final int BLINK_CHANCE_PERCENT = 30;

    /** 闪现的冷却（tick）——不设冷却会变成"打不中的怪"。 */
    public static final int BLINK_COOLDOWN = 60;

    /** 闪现落点在攻击者背后多远（格）。 */
    private static final double BLINK_DISTANCE = 4.0D;

    /** 闪现冷却计时。 */
    private int blinkCooldown;

    public VoidWalkerEntity(EntityType<? extends VoidWalkerEntity> type, Level level) {
        super(type, level, 45);
    }

    @Override
    public MobRank rank() {
        return MobRank.BOSS;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（头目 ×25 生命 / ×3 攻击 / ×1.15 移速）。
     *
     * <pre>
     *   生命 10  × 25.0 = 250
     *   攻击 2.6 × 3.0  = 7.8
     *   移速 0.31 × 1.15 ≈ 0.357    （头目里最快的一档，配瞬移）
     *   护甲 8.0，击退抗性 0.4
     * </pre>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.BOSS, 10.0D, 2.6D, 0.31D, 40.0D, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 冷却每 tick 递减（放在 {@link #aiStep()} 里统一走，不散在几个分支里）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.blinkCooldown > 0) {
            this.blinkCooldown--;
        }
    }

    /**
     * 被打中时按概率闪现到攻击者背后。
     *
     * <p>几个刻意的约束：</p>
     * <ul>
     *   <li>只在<b>服务端</b>搬动实体，客户端靠原版的传送同步跟上；</li>
     *   <li>冷却 {@value #BLINK_COOLDOWN} tick，且{@link #randomTeleport} 自带
     *       "找不到安全落点就返回 false"的检查——不会闪进石头里；</li>
     *   <li>闪现后<b>把目标重新指回攻击者</b>：原版 {@code randomTeleport} 会清掉目标，
     *       不补这一句就会出现"瞬移一次之后它掉头走了"的呆样。</li>
     * </ul>
     */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!hurt || this.level().isClientSide() || this.blinkCooldown > 0) {
            return hurt;
        }
        if (this.random.nextInt(100) >= BLINK_CHANCE_PERCENT) {
            return hurt;
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        if (attacker == null) {
            return hurt;
        }
        // 目标点：攻击者背后 4 格（用攻击者的视线方向反推）
        Vec3 look = attacker.getLookAngle();
        Vec3 behind = new Vec3(attacker.getX() - look.x * BLINK_DISTANCE,
                attacker.getY(), attacker.getZ() - look.z * BLINK_DISTANCE);
        this.blinkAway();
        if (this.randomTeleport(behind.x, behind.y, behind.z, false)) {
            this.blinkAway();
            this.blinkCooldown = BLINK_COOLDOWN;
            this.setTarget(attacker);
        }
        return hurt;
    }

    /** 闪现的视觉预告：身前身后各一串传送门粒子。 */
    private void blinkAway() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    24, 0.4D, 0.7D, 0.4D, 0.35D);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    12, 0.35D, 0.6D, 0.35D, 0.05D);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.9F, 1.1F);
        }
    }

    /** 遭遇音：末影人那一档的空洞声响。 */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    /** 头目掉落：空冥石为主，偶尔一枚纪元令。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 35) {
            return new ItemStack(ModItems.KONG_MING_SHI.get(), 1);
        }
        if (this.random.nextInt(100) < 25) {
            return new ItemStack(ModItems.SPIRIT_CRYSTAL.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
