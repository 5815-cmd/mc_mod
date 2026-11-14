package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * 血蛭（普通）——血食泥沼的吸血蠕虫。
 *
 * <p>定位：**5 节鼓包 + 口器环 6 齿 + 尾端血囊**。长条怪，hitbox 只包身体中段
 * （0.9 × 0.5；长度 30 px 不进命中箱）。剪影是"糖葫芦"而不是蛇。</p>
 *
 * <p>打法：贴地猛冲 + 吸血咬（命中回自身 1 点）；更阴的一手是<b>钻地伏击</b>——
 * 钻进泥里消失，从你脚边 2.5 格内破土而出，带一圈泥浆粒子并挂缓慢 II。
 * 它很脆（16 血、护甲 2），但成群刷（weight 10 / min2 max3），
 * 真正烦人的是"不知道下一只从哪冒出来"。</p>
 *
 * <p>普通品阶但口器/血囊/眼睛依赖发光层，所以重写 {@link #forceGlow()} 返回 true
 * （否则 {@code RankGlowLayer} 对普通怪不绘制）。</p>
 */
public class BloodLeechEntity extends CultivationMonsterEntity {

    /** 钻地伏击冷却（tick，设计稿 §3.3.6：240t）。 */
    public static final int DIG_COOLDOWN = 240;

    /** 一次钻地全流程时长（tick）：4 钻入 + 24 潜伏 + 4 破土 + 8 收势。 */
    public static final int DIG_DURATION = 40;

    /** 破土命中半径（格，设计稿：2 格）。 */
    public static final double ERUPT_RADIUS = 2.5D;

    /** 吸血量（点/次，设计稿：命中回自身 1 点）。 */
    public static final float LIFESTEAL = 1.0F;

    public BloodLeechEntity(EntityType<? extends BloodLeechEntity> type, Level level) {
        super(type, level, 7);
    }

    @Override
    public MobRank rank() {
        return MobRank.NORMAL;
    }

    /** 普通品阶，但发光层（口器环/血囊/眼）必须亮——见设计稿 §2.2。 */
    @Override
    public boolean forceGlow() {
        return true;
    }

    /**
     * 基础属性（普通倍率 ×1，最终值 = 基础值）：
     * 生命 16 / 攻击 3.5 / 移速 0.26 / 追击 28 / 护甲 2。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.NORMAL, 16.0D, 3.5D, 0.26D, 28.0D, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DigAmbushGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 吸血咬：伤害走父类（含单次伤害软上限），命中后自身回 {@value #LIFESTEAL} 点。
     * 血蛭的攻击因此是"回血手段"——和它 16 血的脆皮正好互补。
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.heal(LIFESTEAL);
            this.playSound(SoundEvents.SLIME_ATTACK, 0.7F, 1.35F);
        }
        return hit;
    }

    /** 泥沼里的血蛭：偶尔从泥里冒个气泡（视觉提示"它在附近"）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.tickCount % 30 != 0) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    this.getY() + 0.15D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D,
                    1, 0.02D, 0.02D, 0.02D, 0.005D);
        }
    }

    /**
     * 钻地伏击（设计稿 §3.3.6 special）：
     * 钻入 4t（隐身）→ 潜伏潜行 24t（隐身加速靠近）→ 在目标 2.5 格内破土 →
     * 造成一次伤害 + 缓慢 II 60t → 收势 8t；冷却 {@value #DIG_COOLDOWN}t。
     *
     * <p>用 Goal 而不是 tick：它要抢 MOVE 旗标，才能在潜伏期"从地里钻过去"
     * 而不是被其它移动 goal 拽着绕圈。</p>
     */
    static final class DigAmbushGoal extends Goal {

        private final BloodLeechEntity leech;
        private int digTicks;
        private int cooldown;

        DigAmbushGoal(BloodLeechEntity leech) {
            this.leech = leech;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.leech.getTarget();
            if (target == null || !target.isAlive() || !this.leech.onGround()) {
                return false;
            }
            double dist = this.leech.distanceToSqr(target);
            return dist >= 9.0D && dist <= 144.0D;
        }

        @Override
        public void start() {
            this.digTicks = 0;
            this.leech.setInvisible(true);
            this.leech.getNavigation().stop();
            this.leech.playSound(SoundEvents.SNIFFER_DIGGING, 0.9F, 1.5F);
            this.burst(8);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.leech.getTarget();
            return this.digTicks < DIG_DURATION && target != null && target.isAlive();
        }

        @Override
        public void tick() {
            this.digTicks++;
            LivingEntity target = this.leech.getTarget();
            if (target == null) {
                this.digTicks = DIG_DURATION;
                return;
            }
            // 0..4 钻入：原地不动，只冒泥
            if (this.digTicks <= 4) {
                this.leech.getNavigation().stop();
                return;
            }
            // 4..28 潜伏：隐身加速摸过去（贴地走，不瞬移，玩家能听到/看到泥浆）
            if (this.digTicks < 28) {
                this.leech.getNavigation().moveTo(target, 1.8D);
                if (this.digTicks % 4 == 0) {
                    this.burst(2);
                }
                return;
            }
            // 28..32 破土：冒出来，脚边 2.5 格内结算一次伤害 + 缓慢 II
            if (this.digTicks == 28) {
                this.leech.getNavigation().stop();
                this.leech.setInvisible(false);
                this.burst(18);
                if (this.leech.distanceToSqr(target) <= ERUPT_RADIUS * ERUPT_RADIUS) {
                    this.leech.doHurtTarget(target);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            60, 1), this.leech);
                }
            }
            // 32..40 收势：原地喘气
        }

        /** 泥浆/血雾：钻入、潜行、破土三处的视觉与听觉反馈。 */
        private void burst(int count) {
            if (this.leech.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF,
                        this.leech.getX(), this.leech.getY() + 0.15D, this.leech.getZ(),
                        count, 0.4D, 0.15D, 0.4D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        this.leech.getX(), this.leech.getY() + 0.3D, this.leech.getZ(),
                        count / 2, 0.3D, 0.1D, 0.3D, 0.01D);
            }
            this.leech.playSound(SoundEvents.SLIME_SQUISH, 0.8F, 1.4F);
        }

        @Override
        public void stop() {
            if (this.leech.isInvisible()) {
                this.leech.setInvisible(false);
            }
            if (this.cooldown <= 0) {
                this.cooldown = DIG_COOLDOWN;
            }
        }
    }

    /** 掉落（设计稿 §3.3.7）：45% 血珞精 / 30% 黏液球 ×1~2 / 其余回退普通（灵叶）。 */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 45) {
            return new ItemStack(ModItems.XUELUO_ESSENCE.get());
        }
        if (this.random.nextInt(100) < 30) {
            return new ItemStack(net.minecraft.world.item.Items.SLIME_BALL,
                    1 + this.random.nextInt(2));
        }
        return super.rankDrop(rank);
    }
}
