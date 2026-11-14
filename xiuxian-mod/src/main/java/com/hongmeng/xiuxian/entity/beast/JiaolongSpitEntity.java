package com.hongmeng.xiuxian.entity.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 蛟龙之息 —— 蛟龙喷出的那一口虚空毒。
 *
 * <p>刻意做成独立的 {@link Projectile} 而不是"瞬发伤害"：
 * 喷吐要有飞行时间，玩家才能看见那条紫黑色的弹道并躲开；
 * 瞬发只会让人觉得"莫名其妙掉了半管血"。</p>
 *
 * <p>数值口径：伤害 = 蛟龙攻击力的 {@value #DAMAGE_RATIO}（再乘成长倍率），
 * 命中附加 {@value #POISON_TICKS} tick 中毒。这个倍率<b>不绕过</b>
 * {@code CultivationMonsterEntity} 的 35% 单次伤害软上限——
 * 蛟龙是玩家的灵兽，它的伤害本来就不该走怪物那条缩放链路；
 * 但为了不变成"玩家用蛟龙一击秒头目"，伤害仍然乘 {@value #DAMAGE_RATIO}
 * 并且吃目标的护甲减伤（走 {@code hurt} 的标准路径）。</p>
 */
public class JiaolongSpitEntity extends net.minecraft.world.entity.projectile.ThrowableItemProjectile {

    /** 伤害 = 施法者攻击力 × 这个比例。 */
    public static final float DAMAGE_RATIO = 0.75F;

    /** 命中附加的中毒时长（tick）。 */
    public static final int POISON_TICKS = 120;

    /** 存活上限，防止没打中任何东西时永远飞下去。 */
    private static final int MAX_LIFE = 100;

    public JiaolongSpitEntity(EntityType<? extends JiaolongSpitEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public JiaolongSpitEntity(EntityType<? extends JiaolongSpitEntity> type, Level level,
                              LivingEntity shooter) {
        this(type, level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getY() + shooter.getBbHeight() * 0.7D,
                shooter.getZ());
    }

    /** 朝目标发射。 */
    public void shootAt(LivingEntity target, double speed) {
        Vec3 delta = new Vec3(target.getX() - this.getX(),
                target.getY(0.5D) - this.getY(),
                target.getZ() - this.getZ());
        if (delta.lengthSqr() < 1.0E-6D) {
            return;
        }
        this.setDeltaMovement(delta.normalize().scale(speed));
        this.hasImpulse = true;
        // 朝向与速度一致，渲染出来才不是一颗"横着飞"的球
        double horizontal = this.getDeltaMovement().horizontalDistance();
        this.setYRot((float) (Math.atan2(this.getDeltaMovement().x, this.getDeltaMovement().z)
                * (180.0D / Math.PI)));
        this.setXRot((float) (Math.atan2(this.getDeltaMovement().y, horizontal)
                * (180.0D / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > MAX_LIFE) {
            discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        Vec3 next = this.position().add(motion);

        // 1) 方块碰撞
        BlockHitResult blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                this.position(), next, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            onImpact(blockHit.getLocation());
            return;
        }

        // 2) 实体碰撞
        AABB sweep = this.getBoundingBox().expandTowards(motion).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this.level(), this,
                this.position(), next, sweep,
                e -> e instanceof LivingEntity && e.isAlive() && e != this.getOwner());
        if (entityHit != null) {
            onImpact(entityHit.getLocation());
            return;
        }

        this.setPos(next.x, next.y, next.z);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY(), this.getZ(),
                    4, 0.12D, 0.12D, 0.12D, 0.02D);
        }
    }

    /** 命中结算：伤害 + 中毒 + 粒子 + 音效，然后消失。 */
    private void onImpact(Vec3 at) {
        Level level = this.level();
        if (!level.isClientSide()) {
            float damage = 4.0F;
            if (this.getOwner() instanceof LivingEntity shooter) {
                double attack = shooter.getAttributeValue(
                        net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                damage = (float) Math.max(1.0D, attack * DAMAGE_RATIO);
            }
            AABB area = new AABB(at.x - 0.6D, at.y - 0.6D, at.z - 0.6D,
                    at.x + 0.6D, at.y + 0.6D, at.z + 0.6D);
            DamageSource source = level.damageSources().mobProjectile(this,
                    this.getOwner() instanceof LivingEntity shooter ? shooter : null);
            boolean hitAny = false;
            for (Entity e : level.getEntities(this, area)) {
                if (!(e instanceof LivingEntity living) || living == this.getOwner()) {
                    continue;
                }
                if (living.hurt(source, damage)) {
                    hitAny = true;
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0),
                            this.getOwner() instanceof LivingEntity shooter ? shooter : null);
                }
            }
            if (hitAny) {
                this.playSound(SoundEvents.PLAYER_HURT, 0.5F, 0.6F);
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                    at.x, at.y, at.z, 14, 0.25D, 0.25D, 0.25D, 0.03D);
        }
        discard();
    }

    @Override
    protected void defineSynchedData() {
        // ThrowableItemProjectile 已经定义了 DATA_ITEM_STACK，这里没有额外的同步字段
        super.defineSynchedData();
    }

    /**
     * 渲染用哪件物品。
     *
     * <p>刻意挑 {@code lingye}（灵叶）当弹体外观：它是一枚青绿色的叶片，
     * 16×16 里没有一处留白贴边，旋转着飞出去正好读作"一口灵光"。
     * <b>不新造贴图</b>——风格上它和"蛟龙吐息"完全对得上，
     * 也省掉一个纯粹为了好看而存在的 asset。</p>
     *
     * <p>查不到就退回 {@code air}——原版 {@code ThrownItemRenderer}
     * 对空物品栈会直接跳过绘制，不会崩。</p>
     */
    @Override
    protected ItemStack getItemRaw() {
        return spitRenderItem();
    }

    /**
     * {@code ThrowableItemProjectile} 还要求一个"默认物品"：
     * 实体刚创建、{@code setItem} 还没被调用时用它。
     */
    @Override
    protected net.minecraft.world.item.Item getDefaultItem() {
        ItemStack stack = spitRenderItem();
        return stack.isEmpty() ? net.minecraft.world.item.Items.AIR : stack.getItem();
    }

    /** 弹体外观用的物品（见 {@link #getItemRaw()} 的说明）。 */
    private static ItemStack spitRenderItem() {
        try {
            net.minecraft.world.item.Item lingye =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                            net.minecraft.resources.ResourceLocation.tryBuild(
                                    com.hongmeng.xiuxian.HongmengMod.MOD_ID, "lingye"));
            if (lingye != null) {
                return new ItemStack(lingye);
            }
        } catch (Throwable ignored) {
            // 注册表没起来：退回空栈，渲染器会跳过
        }
        return ItemStack.EMPTY;
    }

    /** 弹体不带重力：它是被"喷"出去的，不是被扔出去的。 */
    @Override
    protected float getGravity() {
        return 0.0F;
    }

    /** 进水不熄灭，也不产生气泡音。 */
    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
