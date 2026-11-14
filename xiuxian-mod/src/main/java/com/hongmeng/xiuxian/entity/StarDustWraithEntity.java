package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * <b>星尘游魂（star_dust_wraith）</b> —— 虚空裂隙 · 星尘荒原的星尘聚合体（精英）。
 *
 * <p>设计真源：{@code 工程/角色设计/round2/新怪物第二梯队设计稿.md} §3.7。</p>
 *
 * <p>它没有腿：人形轮廓由星尘压成，下摆散成 3 条星尘尾，胸口一颗大星核，头顶浮着 3 颗星点。
 * <b>"飘浮"是视觉的</b>——实体本身照常贴地行走（出生也是 ON_GROUND 地面怪，
 * 这样寻路/刷怪规则都不出怪），飘感由模型 {@code setupAnim} 的 ±1px 起落 + 尾摆做
 * （静态模型最低点仍是 y=24，见 {@code StarDustWraithModel.LOWEST_PIXEL_Y}）。</p>
 *
 * <p>战斗：</p>
 * <ul>
 *   <li><b>星尘侵蚀</b>——每次命中附加「缓慢 I + 虚弱 I」各 <b>40t</b>（被星尘灌进关节）；</li>
 *   <li><b>星尘爆发</b>——贴脸时蓄力 40t（模型双臂张开、星核变亮、头顶三星公转），
 *       第 15t 释放：半径 3 格范围伤害 + 击退 1.2 格 + 缓慢 II 60t，粒子 {@code end_rod}；
 *       冷却 240t。爆发进度通过同步字段给渲染层（{@link #burstTicks()}）。</li>
 * </ul>
 *
 * <p>数值（§3.7.7，基础值 × 精英倍率）：生命 20×3.0=<b>60</b>、攻击 5.5×1.5=<b>8.25</b>、
 * 移速 0.30×1.05=<b>0.315</b>、追击 40、护甲 4、经验 17×2=<b>34</b>。</p>
 */
public class StarDustWraithEntity extends CultivationMonsterEntity {

    /** 星尘爆发总时长（tick）。 */
    public static final int BURST_TICKS = 40;
    /** 爆发冷却（tick）。 */
    public static final int BURST_COOLDOWN = 240;
    /** 爆发释放时刻：第 15t（§3.7.6 的 12→18t 窗口中间）。 */
    public static final int BURST_RELEASE_TICK = 15;
    /** 星尘侵蚀：缓慢 I + 虚弱 I 的时长（tick）。 */
    public static final int EROSION_TICKS = 40;
    /** 爆发半径（格）。 */
    private static final double BURST_RADIUS = 3.0D;

    /** 爆发剩余 tick（同步到客户端，0 = 没在爆发）。 */
    private static final EntityDataAccessor<Integer> DATA_BURST =
            SynchedEntityData.defineId(StarDustWraithEntity.class, EntityDataSerializers.INT);

    private int burstCooldown = 0;

    public StarDustWraithEntity(EntityType<? extends StarDustWraithEntity> type, Level level) {
        // 基础经验 17 → 精英 EXP 倍率 ×2 = 34（§3.7.7）
        super(type, level, 17);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /** 生命 20 / 攻击 5.5 / 移速 0.30 / 追击 40 / 护甲 4（品阶倍率由 scaledAttributes 乘）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 20.0D, 5.5D, 0.30D, 40.0D, 4.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BURST, 0);
    }

    /** 星尘爆发剩余 tick（0 = 没在爆发）；模型（{@code StarDustWraithModel}）据此驱动爆发动画。 */
    public int burstTicks() {
        return this.entityData.get(DATA_BURST);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------
    // 飘浮：视觉做（模型 anim），实体这边不放重力、也不改 Y
    // ------------------------------------------------------------------

    /** 飘着的东西不怕摔（§3.7：视觉飘浮，不开飞行）。 */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier,
                                   net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /** 命中附加"星尘侵蚀"：缓慢 I + 虚弱 I 各 40t。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EROSION_TICKS, 0));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EROSION_TICKS, 0));
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            // 星尘：身体周围缓缓冒星点（纯演出，服务端不参与）
            if (this.random.nextInt(5) == 0) {
                double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.9D;
                double y = this.getY() + 0.35D + this.random.nextDouble() * 1.5D;
                double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.9D;
                this.level().addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.01D, 0.0D);
            }
            return;
        }

        if (this.burstCooldown > 0) {
            this.burstCooldown--;
        }

        int burst = this.burstTicks();
        if (burst > 0) {
            this.entityData.set(DATA_BURST, burst - 1);
            if (burst == BURST_TICKS - BURST_RELEASE_TICK) {
                this.releaseBurst();
            }
        } else if (this.burstCooldown <= 0 && this.getTarget() != null
                && this.getTarget().isAlive()
                && this.distanceToSqr(this.getTarget()) <= BURST_RADIUS * BURST_RADIUS) {
            // 贴脸 → 开始蓄力（40t 后释放）
            this.entityData.set(DATA_BURST, BURST_TICKS);
            this.burstCooldown = BURST_COOLDOWN;
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.65F);
        }
    }

    /** 星尘爆发释放：半径 3 格范围伤害 + 击退 1.2 格 + 缓慢 II 60t + end_rod 粒子。 */
    private void releaseBurst() {
        this.playSound(SoundEvents.GENERIC_EXPLODE, 1.1F, 1.6F);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BURST_RADIUS, 1.5D, BURST_RADIUS))) {
            if (victim == this || victim instanceof StarDustWraithEntity) {
                continue;
            }
            // doHurtTarget 会走父类伤害软上限 + 附加星尘侵蚀，范围伤害也不会秒人
            if (this.doHurtTarget(victim)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                double dx = victim.getX() - this.getX();
                double dz = victim.getZ() - this.getZ();
                double len = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
                victim.knockback(1.2D, dx / len, dz / len);
            }
        }
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + 1.1D, this.getZ(),
                12, 1.2D, 0.7D, 1.2D, 0.02D);
    }

    // ------------------------------------------------------------------
    // 声音
    // ------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    // ------------------------------------------------------------------
    // 掉落（§3.7.7：只覆写 rankDrop，不动基类的 rolls / 空冥石概率）
    // ------------------------------------------------------------------

    /** 50% 空冥精 ×1 / 30% 萤石粉 ×1~2 / 其余 灵晶 ×1。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        int roll = this.random.nextInt(100);
        if (roll < 50) {
            return new ItemStack(ModItems.KONGMING_ESSENCE.get(), 1);
        }
        if (roll < 80) {
            return new ItemStack(Items.GLOWSTONE_DUST, 1 + this.random.nextInt(2));
        }
        if (roll < 100) {
            return new ItemStack(ModItems.SPIRIT_CRYSTAL.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
