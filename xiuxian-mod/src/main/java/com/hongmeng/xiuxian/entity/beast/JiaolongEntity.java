package com.hongmeng.xiuxian.entity.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * 蛟龙 —— <b>空战</b>职能的飞行坐骑，全套灵兽里最稀有的一只。
 *
 * <h3>它的生态位</h3>
 *
 * <p>规格写的是"蛟龙 → 全能，但<b>契约门槛高</b>（需要击败虚空裂隙的头目）"。
 * 落到数值上就是三件事同时成立：</p>
 *
 * <ul>
 *   <li><b>只有它能在飞的时候打人</b>：{@link SpitGoal} 会在骑乘中或战斗中
 *       喷出蛟龙之息（紫黑毒弹，{@code JiaolongSpitEntity}），
 *       雷鹰没有远程手段；</li>
 *   <li><b>血最厚（90）</b>：是雷鹰的近四倍，可以正面顶着头目打；</li>
 *   <li><b>门槛最高</b>：{@code contractRealmGap = 3}，也就是主人要比
 *       虚空蛟（头目）高出三个大境才有把握——对应"击败虚空裂隙头目"这条设定。</li>
 * </ul>
 *
 * <p>代价同样明确：它的灵力消耗最凶（3.0/tick，比雷鹰贵 20%），
 * 而且<b>不会悬停</b>——松开空格就会缓缓下沉，想停在空中看风景请换雷鹰。
 * 这是"全能但每一项都不是最好"的具体化，而不是一句形容词。</p>
 */
public class JiaolongEntity extends SpiritBeastEntity {

    /** 喷吐冷却（tick）。 */
    private static final int SPIT_COOLDOWN = 50;

    private int spitCooldown;

    public JiaolongEntity(EntityType<? extends JiaolongEntity> type, Level level) {
        super(type, level);
        this.xpReward = 45;
    }

    @Override
    public BeastKind kind() {
        return BeastKind.JIAOLONG;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BeastKind.JIAOLONG.baseHealth())
                .add(Attributes.ATTACK_DAMAGE, BeastKind.JIAOLONG.baseDamage())
                .add(Attributes.MOVEMENT_SPEED, BeastKind.JIAOLONG.baseSpeed())
                .add(Attributes.FLYING_SPEED, 0.46D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    /** 不悬停：蛟龙是"巡游"型，要停就得落地。 */
    @Override
    public boolean hoverCapable() {
        return false;
    }

    /**
     * 鞍位：九节身体的第一节顶面在模型 y=2（{@code PartPose.offset(0, 9, 1)}
     * 的盒子 {@code addBox(-3.5, 0, -3.5, 7, 7, 7)}，顶面 = 9 - 7 = 2），
     * 乘客落在 {@code (24-2)/16 ≈ 1.375} 格处——也就是骑在"靠头那一节"上。
     *
     * <p>与 {@code JiaolongRenderer.JiaolongModel.createBodyLayer()} 对应，
     * 改模型必须同时改这里。</p>
     */
    @Override
    protected double riderYOffset() {
        return feetToRider(2.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(4, new SpitGoal(this));
        // 蛟龙会替主人清场：妖兽与敌对生物都打
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class,
                12, true, false, living -> this.isTame() && this.getOwner() != null));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.spitCooldown > 0) {
            this.spitCooldown--;
        }
        // 虚空尾迹：沿身体拖一条紫黑光带，与虚空蛟一脉相承
        if (this.tickCount % 5 == 0 && this.level() instanceof ServerLevel serverLevel) {
            double yaw = Math.toRadians(this.getYRot());
            double back = 1.2D + this.random.nextDouble() * 2.4D;
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX() + Math.sin(yaw) * back,
                    this.getY() + 0.45D,
                    this.getZ() - Math.cos(yaw) * back,
                    2, 0.25D, 0.15D, 0.25D, 0.02D);
        }
    }

    /** 喷吐：真的吐出一发会飞的毒弹（不是瞬发伤害）。 */
    public boolean trySpit(LivingEntity target) {
        if (this.spitCooldown > 0 || this.level().isClientSide() || target == null
                || !target.isAlive()) {
            return false;
        }
        JiaolongSpitEntity spit = com.hongmeng.xiuxian.entity.beast.BeastManager
                .createSpit(this);
        if (spit == null) {
            return false;
        }
        spit.shootAt(target, 1.1D);
        this.level().addFreshEntity(spit);
        this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 0.8F, 1.4F);
        this.spitCooldown = SPIT_COOLDOWN;
        return true;
    }

    /** 喷吐 goal：有目标、在射程内、冷却好了就吐一口。 */
    static final class SpitGoal extends Goal {

        private final JiaolongEntity dragon;

        SpitGoal(JiaolongEntity dragon) {
            this.dragon = dragon;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.dragon.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double dist = this.dragon.distanceToSqr(target);
            return dist >= 9.0D && dist <= 400.0D;
        }

        @Override
        public void tick() {
            LivingEntity target = this.dragon.getTarget();
            if (target == null) {
                return;
            }
            this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.dragon.trySpit(target);
        }
    }
}
