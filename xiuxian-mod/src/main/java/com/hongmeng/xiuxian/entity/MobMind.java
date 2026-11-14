package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * <b>怪物心智</b>：让怪"有自己的想法"，而不是直线冲上来挨打。
 *
 * <h2>玩家要求</h2>
 * <blockquote>「怪物逻辑动作什么的是否可以内置 ai？让他们有自己的想法」</blockquote>
 *
 * <h2>为什么不能只靠原版 goal</h2>
 * <p>原版 {@code MeleeAttackGoal} 是<b>直线追</b>：永远朝玩家当前位置走，
 * 到 3 格就挥一下。于是所有怪的行为完全一样，玩家一眼看穿 ——
 * 打起来像打木桩。</p>
 *
 * <p>这里不改原版 goal（改坏了会连锁影响寻路、避障、攻击判定），
 * 而是在 <b>goal 之上加一层"心情"</b>：每隔一段时间决定</p>
 * <ul>
 *   <li><b>逼近</b>：直线压上去；</li>
 *   <li><b>绕行</b>：斜着绕到侧后方（原版绝不会这么做）；</li>
 *   <li><b>撤退</b>：血少了先退开，缓过来再上；</li>
 *   <li><b>蓄力</b>：站定慢下来、有起手表现，然后<b>猛扑</b>。</li>
 * </ul>
 *
 * <h2>为什么"心情"要存在持久化数据里</h2>
 * <p>区块卸载 / 存档读回时，怪的对象会被重建。心情放在字段里就丢了 ——
 * 表现是"刚蓄力到一半，读档后突然又在散步"。存 {@code getPersistentData()} 就不会。</p>
 *
 * <h2>危险的边界</h2>
 * <p>这一层只做三件事：改写寻路目标、短暂改速度、打一次近战。
 * <b>不改伤害数值、不改属性、不加新攻击类型</b> —— 平衡仍由既有系统管。</p>
 */
public final class MobMind {

    /** 心情（持久化里存 ordinal）。 */
    public enum Mood {
        /** 直线压上。 */
        APPROACH,
        /** 斜向绕到侧后。 */
        CIRCLE,
        /** 退开缓气。 */
        WITHDRAW,
        /** 站定蓄力（有起手表现）。 */
        TELEGRAPH,
        /** 猛扑。 */
        COMMIT
    }

    private static final String TAG_MOOD = "hm_mind_mood";
    private static final String TAG_UNTIL = "hm_mind_until";
    private static final String TAG_COOLDOWN = "hm_mind_cd";
    private static final String TAG_RETREATED = "hm_mind_retreated";
    /** 这次猛扑是否已经打出去（每段猛扑只打一次，防止一扑多段伤害）。 */
    private static final String TAG_HIT_DONE = "hm_mind_hit";
    /** 被打断后的踉跄到期 tick。 */
    private static final String TAG_STAGGER = "hm_mind_stagger";

    /**
     * PERF(B)：<b>下一次允许重新寻路的 tick</b>（写在怪物自己的持久化数据里）。
     *
     * <p>原来 {@link #Mood#APPROACH}/{@link #Mood#CIRCLE}/{@link #Mood#WITHDRAW}
     * 三种心情**每 tick** 都调一次 {@code getNavigation().moveTo(...)}，
     * 而 {@code PathNavigation.moveTo(...)} 会<b>立刻做一次 A* 寻路</b>
     * （{@code createPath}）。副本一波几十上百只怪同时追人时，
     * 等于每 tick 几百次 A* —— 这是服务端 tick 的持续大头，且完全没必要：
     * 原版 {@code MeleeAttackGoal} 的重寻路节奏本来就是 4~10 tick 一次。
     * 现在对齐原版节奏（4~10 tick），怪物照常沿已有路径走，观感不变。</p>
     */
    private static final String TAG_NAV = "hm_mind_nav";

    /** PERF(B)：寻路目标刷新间隔下限（tick）。与 {@code MeleeAttackGoal} 的 4 一致。 */
    private static final int NAV_MIN_TICKS = 4;

    /** PERF(B)：寻路目标刷新间隔的随机跨度 → 实际 4~10 tick，与 {@code MeleeAttackGoal} 一致。 */
    private static final int NAV_SPAN_TICKS = 7;

    /** 被打断后的踉跄时长（tick）—— "僵直一下"不算，要 1 秒。 */
    public static final int STAGGER_TICKS = 20;

    /** 重击判定：单次伤害至少占它最大生命的这个比例。 */
    public static final double HEAVY_DAMAGE_RATIO = 0.05D;

    /** 群体协同的检测半径（格）—— 同一小片区域里的同伴。 */
    public static final double GROUP_RADIUS = 12.0D;

    private static final int APPROACH_TICKS = 30;
    private static final int CIRCLE_TICKS = 26;
    private static final int WITHDRAW_TICKS = 34;
    private static final int TELEGRAPH_TICKS = 16;
    private static final int COMMIT_TICKS = 12;

    private MobMind() {
    }

    /** 每 tick 由实体的 {@code customServerAiStep} 调用。 */
    public static void tick(CultivationMonsterEntity mob) {
        if (mob == null || mob.level().isClientSide()) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(mob.getTarget() instanceof ServerPlayer target) || !target.isAlive()) {
            return;
        }
        // 首领级用既有的 BossAbilities 分档逻辑，不在这里叠一层，免得两种 AI 打架
        if (mob.effectiveRank().hasBossBar()) {
            return;
        }
        var pd = mob.getPersistentData();
        long now = level.getGameTime();
        long until = pd.getLong(TAG_UNTIL);
        Mood mood = Mood.values()[Math.floorMod(pd.getInt(TAG_MOOD), Mood.values().length)];

        // ---- 1. 到点重选心情 ----
        if (now >= until) {
            if (mood == Mood.TELEGRAPH) {
                // 蓄力走完 → **必然接猛扑**（不是重新决策，否则永远打不出来）
                mood = Mood.COMMIT;
                lunge(mob, target);
            } else {
                mood = decide(mob, target, pd);
            }
            pd.putInt(TAG_MOOD, mood.ordinal());
            pd.putLong(TAG_UNTIL, now + ticksOf(mood));
        }

        // ---- 踉跄中：什么都不做（被打断的惩罚）----
        if (staggered(mob)) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(target, 20.0F, 20.0F);
            return;
        }

        // ---- 2. 按心情行动 ----
        double dist = mob.distanceTo(target);
        switch (mood) {
            case APPROACH -> {
                // PERF(B)：寻路每 4~10 tick 才刷新一次（原来每 tick 一次 A*）
                if (navReady(mob, pd, now)) {
                    mob.getNavigation().moveTo(target, 1.0D);
                }
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            case CIRCLE -> {
                // 斜向绕行：以外积方向为目标点，形成包抄观感
                // PERF(B)：同上，且"目标点"也只在刷新寻路时才需要算
                if (navReady(mob, pd, now)) {
                    Vec3 side = strafePoint(mob, target, dist);
                    if (side != null) {
                        mob.getNavigation().moveTo(side.x, side.y, side.z, 1.0D);
                    }
                }
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            case WITHDRAW -> {
                // PERF(B)：同上，撤退点也只在刷新寻路时算
                if (navReady(mob, pd, now)) {
                    Vec3 away = mob.position().subtract(target.position()).normalize()
                            .scale(6.0D).add(mob.position());
                    mob.getNavigation().moveTo(away.x, away.y, away.z, 1.15D);
                }
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                pd.putBoolean(TAG_RETREATED, true);
            }
            case TELEGRAPH -> {
                // 站定、面向玩家、有起手表现（玩家要求"看得出它要动手"）
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(target, 45.0F, 45.0F);
                if (now % 2L == 0L) {
                    telegraphFx(level, mob, target);
                }
            }
            case COMMIT -> {
                // 猛扑：初速在**进入这一心情时**给一次（见 lunge），
                // 之后几 tick 只负责贴上去打一下，不持续加速（否则会飞天）。
                if (dist <= 3.2D && !pd.getBoolean(TAG_HIT_DONE)) {
                    mob.doHurtTarget(target);
                    pd.putBoolean(TAG_HIT_DONE, true);
                }
            }
            default -> {
                // 不会到这里
            }
        }
    }

    /**
     * PERF(B)：这一 tick 允不允许重新寻路。
     *
     * <p>间隔 4~10 tick，和原版 {@code MeleeAttackGoal#ticksUntilNextPathRecalculation}
     * （{@code 4 + random(7)}）完全同节奏。路径本身由 {@code PathNavigation.tick()}
     * 每 tick 继续推进，所以怪物不会停。</p>
     */
    private static boolean navReady(CultivationMonsterEntity mob,
                                    net.minecraft.nbt.CompoundTag pd, long now) {
        if (now < pd.getLong(TAG_NAV)) {
            return false;
        }
        pd.putLong(TAG_NAV, now + NAV_MIN_TICKS + mob.getRandom().nextInt(NAV_SPAN_TICKS));
        return true;
    }

    /** 这只怪现在是不是在<b>蓄力</b>（玩家可以打断的窗口）。 */
    public static boolean telegraphing(CultivationMonsterEntity mob) {
        if (mob == null || !(mob.level() instanceof net.minecraft.server.level.ServerLevel lv)) {
            return false;
        }
        var pd = mob.getPersistentData();
        long now = lv.getGameTime();
        return now <= pd.getLong(TAG_UNTIL)
                && pd.getInt(TAG_MOOD) == Mood.TELEGRAPH.ordinal();
    }

    /** 是不是正在踉跄（被打断后的硬直）。 */
    public static boolean staggered(CultivationMonsterEntity mob) {
        if (mob == null || !(mob.level() instanceof net.minecraft.server.level.ServerLevel lv)) {
            return false;
        }
        return lv.getGameTime() < mob.getPersistentData().getLong(TAG_STAGGER);
    }

    /**
     * <b>打断蓄力猛扑</b>：让它踉跄 {@value #STAGGER_TICKS} tick。
     *
     * @return true 表示真的打断了（调用方可据此加伤害或特效）
     */
    public static boolean interrupt(CultivationMonsterEntity mob,
                                    net.minecraft.server.level.ServerPlayer by) {
        if (!telegraphing(mob)) {
            return false;
        }
        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return false;
        }
        var pd = mob.getPersistentData();
        long now = level.getGameTime();
        // 踉跄：不算"格挡僵直"，所以不进 StunScheduler 的登记表
        // （否则玩家可以对它处决 —— 那是完美格挡的专属收益）
        pd.putLong(TAG_STAGGER, now + STAGGER_TICKS);
        pd.putInt(TAG_MOOD, Mood.WITHDRAW.ordinal());
        pd.putLong(TAG_UNTIL, now + STAGGER_TICKS);
        mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                STAGGER_TICKS, 2, false, false, true));
        try {
            mob.getNavigation().stop();
        } catch (Throwable ignored) {
            // 停寻路失败不影响判定
        }
        // 表现
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.6D, mob.getZ(),
                16, 0.4D, 0.35D, 0.4D, 0.16D);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                net.minecraft.sounds.SoundEvents.SHIELD_BREAK,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 1.3F);
        if (by != null) {
            by.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7e\u00a7l打断 \u00a77· \u00a78它踉跄了"), true);
        }
        return true;
    }

    /** 附近有没有同伴已经在蓄力/猛扑（群体协同用）。 */
    private static boolean allyPouncing(CultivationMonsterEntity mob) {
        try {
            if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel level)) {
                return false;
            }
            var box = mob.getBoundingBox().inflate(GROUP_RADIUS);
            var allies = level.getEntitiesOfClass(CultivationMonsterEntity.class, box);
            for (CultivationMonsterEntity other : allies) {
                if (other == mob) {
                    continue;
                }
                int mood = other.getPersistentData().getInt(TAG_MOOD);
                if (mood == Mood.TELEGRAPH.ordinal()
                        || mood == Mood.COMMIT.ordinal()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // 检测失败就当作没有同伴
        }
        return false;
    }

    /** 重新决定心情：距离 + 血量 + 品阶 + 冷却一起决定。 */
    private static Mood decide(CultivationMonsterEntity mob, ServerPlayer target,
                               net.minecraft.nbt.CompoundTag pd) {
        double hp = mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
        double dist = mob.distanceTo(target);
        MobRank rank = mob.effectiveRank();
        boolean retreated = pd.getBoolean(TAG_RETREATED);

        // 血少 + 还没退过 → 先退开（一次，不是无限后退：否则怪会变成逃兵）
        if (hp < 0.28D && !retreated) {
            return Mood.WITHDRAW;
        }
        // 回到半血以上就允许再次撤退
        if (hp > 0.55D) {
            pd.putBoolean(TAG_RETREATED, false);
        }

        // 近身：高品阶会"蓄力猛扑"，杂鱼直接上
        if (dist <= 4.6D) {
            if (rank.ordinal() >= MobRank.ELITE.ordinal() && mob.getRandom().nextFloat() < 0.45F) {
                // 群体协同：同伴已经在蓄力/猛扑时，我先绕圈等着 ——
                // 否则四只怪一起扑上来，玩家必死。
                if (allyPouncing(mob)) {
                    return Mood.CIRCLE;
                }
                return Mood.TELEGRAPH;
            }
            return Mood.APPROACH;
        }
        // 中距离：精英以上更爱绕后（包抄感），杂鱼多半直冲
        if (dist <= 14.0D) {
            double circleChance = rank.ordinal() >= MobRank.ELITE.ordinal() ? 0.55D : 0.28D;
            return mob.getRandom().nextDouble() < circleChance ? Mood.CIRCLE : Mood.APPROACH;
        }
        // 远距离：直冲
        return Mood.APPROACH;
    }

    /** 猛扑：朝目标给一次初速（只给一次，避免持续加速飞天）。 */
    private static void lunge(CultivationMonsterEntity mob, ServerPlayer target) {
        Vec3 push = target.position().subtract(mob.position());
        if (push.lengthSqr() > 0.01D) {
            Vec3 dir = push.normalize().scale(0.62D);
            mob.setDeltaMovement(mob.getDeltaMovement().add(dir.x, 0.22D, dir.z));
        }
        mob.getPersistentData().putBoolean(TAG_HIT_DONE, false);
    }

    /** 心情对应持续 tick（蓄力结束后自动转猛扑）。 */
    private static int ticksOf(Mood mood) {
        return switch (mood) {
            case APPROACH -> APPROACH_TICKS;
            case CIRCLE -> CIRCLE_TICKS;
            case WITHDRAW -> WITHDRAW_TICKS;
            // 蓄力结束直接进猛扑：把两段的时长接起来
            case TELEGRAPH -> TELEGRAPH_TICKS;
            case COMMIT -> COMMIT_TICKS;
        };
    }

    /** 绕行目标点：以玩家为圆心、当前方位旋转约 100° 的位置。 */
    private static Vec3 strafePoint(CultivationMonsterEntity mob, ServerPlayer target,
                                    double dist) {
        Vec3 rel = mob.position().subtract(target.position());
        double radius = Math.max(4.0D, Math.min(7.0D, dist));
        double ang = Math.atan2(rel.z, rel.x) + (mob.getRandom().nextBoolean() ? 1.7D : -1.7D);
        return new Vec3(target.getX() + Math.cos(ang) * radius,
                target.getY(), target.getZ() + Math.sin(ang) * radius);
    }

    /** 蓄力起手表现：脚下聚气 + 头顶红光（让玩家看得出"要动手了"）。 */
    private static void telegraphFx(ServerLevel level, CultivationMonsterEntity mob,
                                    LivingEntity target) {
        // SpellVfx.take 是包内可见的预算闸门 —— 这里直接少量发（4 颗/2 tick，
        // 一只怪一秒也就 20 颗，远低于预算，不需要走闸门）。
        level.sendParticles(ParticleTypes.CRIT,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.7D, mob.getZ(),
                4, 0.35D, 0.25D, 0.35D, 0.02D);
    }

    /** 死亡/移除时清掉心智状态（避免复用时带着旧心情）。 */
    public static void forget(CultivationMonsterEntity mob) {
        if (mob == null) {
            return;
        }
        var pd = mob.getPersistentData();
        pd.remove(TAG_MOOD);
        pd.remove(TAG_UNTIL);
        pd.remove(TAG_COOLDOWN);
        pd.remove(TAG_RETREATED);
        pd.remove(TAG_STAGGER);
        pd.remove(TAG_NAV);           // PERF(B)：寻路节流计时一起清，复用实体时不会带着旧窗口
    }
}
