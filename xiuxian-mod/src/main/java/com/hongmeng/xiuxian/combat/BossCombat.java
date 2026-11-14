package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * <b>Boss 战斗的"激荡"</b>（玩家要求：「战斗也要有激荡的音效」）。
 *
 * <p>boss 不是一路挨打到死，而是有**阶段**：血量跨过 2/3 与 1/3 时各爆发一次，
 * 音效 + 粒子 + 震退 + 狂暴（1/3 时加速），让战斗有起伏。</p>
 *
 * <p>每次跨阈值只触发一次（标记写在 persistentData 里）。</p>
 */
public final class BossCombat {

    /** 第一次阶段：血量降到 2/3。 */
    private static final double PHASE_ONE = 2.0D / 3.0D;
    /** 第二次阶段：血量降到 1/3（狂暴）。 */
    private static final double PHASE_TWO = 1.0D / 3.0D;

    private BossCombat() {
    }

    /**
     * boss 受伤时调用（由 {@code ModEvents} 的 LivingHurtEvent 转发）。
     */
    public static void onBossHurt(LivingEntity boss, ServerLevel level) {
        try {
            if (!(boss instanceof CultivationMonsterEntity cm)) {
                return;
            }
            // 「恶」的受击反馈由 EvilGazerEntity/EvilAbilities 自己负责，避免旧系统叠加"狂怒/震地"加戏
            if (boss instanceof com.hongmeng.xiuxian.entity.EvilGazerEntity) {
                return;
            }
            MobRank rank = cm.rank();
            if (rank != MobRank.BOSS && rank != MobRank.OVERLORD) {
                return;
            }
            double max = boss.getMaxHealth();
            if (max <= 0.0D) {
                return;
            }
            double frac = boss.getHealth() / max;
            var pd = boss.getPersistentData();
            Vec3 at = boss.position();

            if (frac <= PHASE_ONE && !pd.getBoolean("hongmeng_phase1")) {
                pd.putBoolean("hongmeng_phase1", true);
                CombatSounds.bossSlam(level, at);
                burst(level, at, 60, 2.2D);
                knockAway(level, boss, 6.0D, 1.2D);
                announce(level, at, "\u00a7c" + boss.getName().getString()
                        + " \u00a77发出震地怒吼！", 48.0D);
            }
            if (frac <= PHASE_TWO && !pd.getBoolean("hongmeng_phase2")) {
                pd.putBoolean("hongmeng_phase2", true);
                CombatSounds.phaseShift(level, at);
                CombatSounds.bossRoar(level, at);
                burst(level, at, 120, 3.2D);
                knockAway(level, boss, 9.0D, 1.8D);
                // 狂暴：加速 + 抗性（"濒死反扑"）
                boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        20 * 30, 2, false, false));
                boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                        20 * 20, 1, false, false));
                announce(level, at, "\u00a74" + boss.getName().getString()
                        + " \u00a7c陷入了狂怒！", 64.0D);
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[hongmeng] boss 阶段处理失败：{}", t.toString());
        }
    }

    private static void burst(ServerLevel level, Vec3 at, int count, double spread) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.0D, at.z,
                2, spread, 0.5D, spread, 0.0D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 1.0D, at.z,
                count, spread, 1.2D, spread, 0.05D);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.5D, at.z,
                count, spread, 0.6D, spread, 0.10D);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1.0D, at.z,
                3, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** 把周围的小怪震开（清场感）。 */
    private static void knockAway(ServerLevel level, LivingEntity boss, double radius,
                                  double power) {
        AABB box = boss.getBoundingBox().inflate(radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != boss && x.isAlive() && !(x instanceof ServerPlayer))) {
            Vec3 away = e.position().subtract(boss.position());
            double d = Math.max(0.5D, away.length());
            Vec3 dir = away.normalize().scale(power * (1.0D - d / (radius + 0.5D)));
            e.push(dir.x, 0.55D, dir.z);
            e.hurtMarked = true;
        }
    }

    private static void announce(ServerLevel level, Vec3 at, String text, double radius) {
        for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(at) <= radius * radius)) {
            sp.displayClientMessage(Component.literal(text), false);
        }
    }
}
