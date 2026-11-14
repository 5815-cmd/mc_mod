package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 顿帧（hitstop）—— {@code docs/combat-feel-spec.md} §一，**单项收益最大**的手法。
 *
 * <h2>怎么做（以及绝不怎么做）</h2>
 *
 * <p>规格把它写死了：</p>
 * <blockquote>
 * 顿帧不能真的冻结游戏（那会卡）。做法是<b>冻结实体</b>：清速度 + 给几 tick 的免伤
 * + 客户端表现，绝不 {@code Thread.sleep} 或暂停 tick。
 * </blockquote>
 *
 * <p>所以这里的一条 tick 逻辑是：把冻结中的实体<b>每 tick 再清一次速度</b>
 * （只清一次的话它下一 tick 又被重力/寻路拉走了），到期时把<b>欠着的击退</b>一次性打出去。
 * 顺序刻意是"先冻住 → 再打飞"：命中瞬间定住，然后整个人被掀出去——
 * 这比"边打边退"有力得多，也是格斗游戏里顿帧的经典用法。</p>
 *
 * <h2>防叠死</h2>
 *
 * <p>同一实体 {@value CombatFeel#HITSTOP_ANTI_STACK_TICKS} tick 内不得重复顿帧
 * （{@link CombatFeel#hitstopAllowed(long, long)}）。没有这条，攻速快的武器会把
 * 战斗变成"永久僵直"——那是 bug 不是手感。</p>
 *
 * <h2>延迟的击退（为什么存在 pending 里）</h2>
 *
 * <p>如果在冻结的同一 tick 就把速度设成击退冲量，下一 tick 的冻结会把它清零，
 * 击退就"吞"了。所以冲量先记账，冻结<b>结束的那一 tick</b>才写进速度。</p>
 */
public final class HitstopController {

    /** 同时冻结的实体上限（防止极端情况下的表膨胀）。 */
    public static final int MAX_FROZEN = 256;

    /** 冻结中的实体：UUID → 到期刻 + 欠着的击退。 */
    private static final Map<UUID, Freeze> ACTIVE = new HashMap<>();

    /** 每只实体最近一次被顿帧的刻（防叠死）。 */
    private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

    private HitstopController() {
    }

    private static final class Freeze {
        final LivingEntity entity;
        long expireTick;
        double kbX;
        double kbY;
        double kbZ;

        Freeze(LivingEntity entity, long expireTick) {
            this.entity = entity;
            this.expireTick = expireTick;
        }
    }

    // ------------------------------------------------------------------
    // 配置
    // ------------------------------------------------------------------

    /** 顿帧总开关。 */
    public static boolean enabled() {
        try {
            return HongmengConfig.SERVER.hitstopEnabled.get();
        } catch (Throwable t) {
            return true;
        }
    }

    /** 顿帧时长倍率（0 = 关）。 */
    public static double scale() {
        try {
            double v = HongmengConfig.SERVER.hitstopScale.get();
            return Double.isNaN(v) ? 1.0D : Math.max(0.0D, Math.min(4.0D, v));
        } catch (Throwable t) {
            return 1.0D;
        }
    }

    /** 是否连攻击者一起冻。 */
    public static boolean affectsAttacker() {
        try {
            return HongmengConfig.SERVER.hitstopAttacker.get();
        } catch (Throwable t) {
            return true;
        }
    }

    /** 击退倍率。 */
    public static double knockbackScale() {
        try {
            double v = HongmengConfig.SERVER.knockbackScale.get();
            return Double.isNaN(v) ? 1.0D : Math.max(0.0D, Math.min(4.0D, v));
        } catch (Throwable t) {
            return 1.0D;
        }
    }

    // ------------------------------------------------------------------
    // 触发
    // ------------------------------------------------------------------

    /**
     * 按伤害占比触发顿帧。
     *
     * @param target            受击者
     * @param attacker          攻击者（可以是 null：环境伤害没有攻击者）
     * @param damageFraction    本次伤害占受击者最大生命的比例
     * @param knockbackImpulse  冻结结束后要打出去的冲量（已经算好方向与大小）
     * @return 实际的顿帧 tick 数；0 表示没触发
     */
    public static int apply(ServerLevel level, LivingEntity target, LivingEntity attacker,
                            double damageFraction, Vec3 knockbackImpulse) {
        if (level == null || target == null || !enabled()) {
            return 0;
        }
        int ticks = CombatFeel.scaledHitstopTicks(damageFraction, scale());
        if (ticks <= 0) {
            return 0;
        }
        long now = level.getGameTime();

        // 防叠死：受击者 10 tick 内已经顿过帧 → 整次都不顿
        // （只冻攻击者而放任受击者，会让玩家看到"我僵住了但它没事"，比不顿帧还糟）
        if (!canFreeze(target, now)) {
            return 0;
        }
        if (ACTIVE.size() >= MAX_FROZEN) {
            return 0;
        }

        Vec3 kb = knockbackImpulse == null ? Vec3.ZERO : knockbackImpulse;
        freeze(target, level, ticks, kb);

        if (attacker != null && attacker != target && affectsAttacker()
                && canFreeze(attacker, now)) {
            // 攻击者只"定住"，不欠击退
            freeze(attacker, level, Math.max(1, ticks / 2 + 1), Vec3.ZERO);
        }
        return ticks;
    }

    private static boolean canFreeze(LivingEntity entity, long now) {
        return CombatFeel.hitstopAllowed(now, lastTrigger(entity));
    }

    private static long lastTrigger(LivingEntity entity) {
        Long v = LAST_TRIGGER.get(entity.getUUID());
        return v == null ? Long.MIN_VALUE : v;
    }

    private static void freeze(LivingEntity entity, ServerLevel level, int ticks, Vec3 knockback) {
        // 第二道保险：无论谁调用，玩家都不进冻结表。
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return;
        }

        long now = level.getGameTime();
        Freeze f = ACTIVE.get(entity.getUUID());
        if (f == null) {
            f = new Freeze(entity, now + ticks);
            ACTIVE.put(entity.getUUID(), f);
        } else {
            f.expireTick = Math.max(f.expireTick, now + ticks);
        }
        f.kbX = knockback.x;
        f.kbY = knockback.y;
        f.kbZ = knockback.z;

        // 规格："受击者给 invulnerableTime 短暂延长（不影响正常伤害）"。
        // 用 max 而不是直接赋值：原版挨打本来就有 20 tick 的免伤，不能被我们调小。
        entity.invulnerableTime = Math.max(entity.invulnerableTime, ticks);
        zero(entity);
        LAST_TRIGGER.put(entity.getUUID(), now);
    }

    private static void zero(LivingEntity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        // 玩家要靠 hurtMarked 才会把速度同步给客户端，否则"冻结"只发生在服务端
        entity.hurtMarked = true;
    }

    // ------------------------------------------------------------------
    // 每 tick 推进
    // ------------------------------------------------------------------

    /** 由 {@code LevelTickEvent.Post} 调（每个维度一次）。 */
    public static void tick(ServerLevel level) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Freeze>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Freeze> entry = it.next();
            Freeze f = entry.getValue();
            LivingEntity e = f.entity;
            if (e == null || e.isRemoved() || !e.isAlive()) {
                it.remove();
                continue;
            }
            if (now < f.expireTick) {
                // 冻结中：每 tick 再清一次（否则重力/寻路下一 tick 就把它拉走了）
                zero(e);
                e.invulnerableTime = Math.max(e.invulnerableTime, 1);
                continue;
            }
            // 冻结结束：把欠着的击退打出去
            it.remove();
            if (f.kbX != 0.0D || f.kbY != 0.0D || f.kbZ != 0.0D) {
                e.setDeltaMovement(f.kbX, f.kbY, f.kbZ);
                e.hurtMarked = true;
            }
        }
    }

    /** 定期清理防叠死表（只保留最近 400 tick 内的记录）。 */
    public static void prune(long now) {
        if (LAST_TRIGGER.size() < 512) {
            return;
        }
        LAST_TRIGGER.entrySet().removeIf(e -> now - e.getValue() > 400L);
    }

    /** 正在冻结的实体数（诊断/HUD 用）。 */
    public static int activeCount() {
        return ACTIVE.size();
    }

    /** 某只实体现在是不是被冻着。 */
    public static boolean isFrozen(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        Freeze f = ACTIVE.get(entity.getUUID());
        return f != null && f.entity == entity;
    }

    /** 换存档 / 停服清理。 */
    public static void clear() {
        ACTIVE.clear();
        LAST_TRIGGER.clear();
    }
}
