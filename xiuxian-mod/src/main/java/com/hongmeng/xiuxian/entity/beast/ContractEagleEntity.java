package com.hongmeng.xiuxian.entity.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 契约雷鹰 —— <b>空中侦察</b>职能的坐骑。
 *
 * <h3>它的生态位</h3>
 *
 * <p>规格原文：<b>"雷鹰 → 能飞，但脆、载重低（不能带箱子）"</b>。
 * 于是它的强项与短板都被写死在这一处：</p>
 *
 * <table border="1">
 *   <tr><th></th><th>雷鹰</th><th>蛟龙</th></tr>
 *   <tr><td>生命</td><td>40</td><td><b>24</b></td><td>90</td></tr>
 *   <tr><td>速度</td><td>1.45</td><td>1.15</td><td>1.35</td></tr>
 *   <tr><td>载重</td><td>3 格</td><td><b>0 格</b></td><td>6 格</td></tr>
 *   <tr><td>灵力/tick</td><td>0</td><td><b>2.5（悬停 1.25）</b></td><td>3.0</td></tr>
 *   <tr><td>悬停</td><td>—</td><td><b>能</b></td><td>不能</td></tr>
 *   <tr><td>契约门槛</td><td>温顺可驯</td><td>高出 1 境</td><td>高出 3 境</td></tr>
 * </table>
 *
 * <p>所以它不是"更快更强的陆地灵兽"，而是<b>唯一能悬停</b>的那一只：
 * 悬停时灵力消耗减半（{@link BeastKind#hoverManaDrain()}），
 * 可以停在空中看地形、盖空中建筑、做定点侦察。代价是它 24 点生命
 * 挨两箭就得收进袋子里养着。</p>
 *
 * <p>原型来自已有的妖兽 {@code thunder_eagle}（队长阶），契约成功后
 * 羽毛上的电弧会跟着变成蓝色的灵光——这是它"契约雷鹰"名字的由来。</p>
 */
public class ContractEagleEntity extends SpiritBeastEntity {

    public ContractEagleEntity(EntityType<? extends ContractEagleEntity> type, Level level) {
        super(type, level);
        this.xpReward = 18;
    }

    @Override
    public BeastKind kind() {
        return BeastKind.CONTRACT_EAGLE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BeastKind.CONTRACT_EAGLE.baseHealth())
                .add(Attributes.ATTACK_DAMAGE, BeastKind.CONTRACT_EAGLE.baseDamage())
                .add(Attributes.MOVEMENT_SPEED, BeastKind.CONTRACT_EAGLE.baseSpeed())
                .add(Attributes.FLYING_SPEED, 0.42D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    /** 雷鹰是唯一能悬停的灵兽。 */
    @Override
    public boolean hoverCapable() {
        return true;
    }

    /**
     * 鞍位：雷鹰的背脊是身体的顶面（模型 y=5），
     * 乘客落在 {@code (24-5)/16 ≈ 1.1875} 格处。
     *
     * <p>与 {@code ContractEagleRenderer.EagleModel} 的
     * {@code "body"} 那块 {@code PartPose.offset(0, 5, 0)} +
     * {@code addBox(-4, 0, -6, 8, 10, 12)} 对应（顶面 = 5 + 0）。
     * 改模型必须同时改这里。</p>
     */
    @Override
    protected double riderYOffset() {
        return feetToRider(5.0D);
    }

    /** 名不虚传：契约雷鹰飞起来的时候翅膀上带电弧。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 8 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 1.8D,
                    this.getY() + 0.5D + this.random.nextDouble() * 0.5D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 1.8D,
                    2, 0.1D, 0.05D, 0.1D, 0.02D);
        }
    }

    /** 挨打叫一声，短促，方便主人判断它被什么咬了。 */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            this.playSound(SoundEvents.PHANTOM_HURT, 0.9F, 1.45F);
            // 掉到半血以下自己会往上蹿一截——它是鸟，本能是拉开距离
            if (this.getHealth() < this.getMaxHealth() * 0.5F
                    && this.getControllingPassenger() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.35D, 0.0D));
                this.hasImpulse = true;
            }
        }
        return hurt;
    }

    /** 落地待久了会自己蹬起来（否则会被 AI 留在地面上走一路，很掉价）。 */
    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        if (this.onGround() && !this.isBeingRidden()
                && (this.getTarget() != null || this.tickCount % 100 == 0)) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.34D, 0.0D));
            this.hasImpulse = true;
        }
    }
}
