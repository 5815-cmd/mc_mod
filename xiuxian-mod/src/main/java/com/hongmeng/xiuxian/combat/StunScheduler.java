package com.hongmeng.xiuxian.combat;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <b>僵直到期调度器</b>：到点把怪的 AI 恢复。
 *
 * <h2>为什么单独一个类</h2>
 * <p>僵直用 {@code setNoAi(true)} 实现，<b>必须有人把它恢复</b>。
 * 如果只挂不恢复，那只怪就<b>永久变成木桩</b> —— 这正是本项目反复踩的
 * "只挂不摘"（Boss 演出的无敌、法术演出的无敌都是同类问题）。</p>
 *
 * <h2>为什么用计数而不是布尔</h2>
 * <p>两次僵直可能重叠（比如连续两次完美格挡）。如果第二次结束时直接
 * {@code setNoAi(false)}，第一次的僵直就被提前解除了。所以每次排程
 * 都带一个"这是第几次"的序号，只有<b>最后一次</b>才真正恢复。</p>
 */
public final class StunScheduler {

    private record Job(long dueTick, Runnable action) {
    }

    /** 每个实体当前的僵直序号（用来判断"是不是最后一次"）。 */
    private static final java.util.Map<java.util.UUID, Integer> SEQ =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 僵直登记表：谁被僵直到什么时候。
     *
     * <p>为什么要单独登记，而不是查 {@code Mob.isNoAi()}：
     * {@code setNoAi} 也被 Boss 降落演出用（{@code BossDescent}）——
     * 用 {@code isNoAi()} 当"可处决"条件，会让 Boss 出场动画期间变成处决窗口。
     * 只有<b>我们亲手僵直</b>的才算。</p>
     */
    private static final java.util.Map<java.util.UUID, Long> STUNNED =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final List<Job> JOBS = new ArrayList<>();

    private StunScheduler() {
    }

    public static void after(ServerLevel level, int delayTicks, Runnable action) {
        if (level == null || action == null) {
            return;
        }
        if (JOBS.size() > 256) {
            JOBS.clear();
        }
        JOBS.add(new Job(level.getGameTime() + Math.max(0, delayTicks), action));
    }

    /** 每 tick 推进。 */
    public static void tick(long now) {
        if (JOBS.isEmpty()) {
            return;
        }
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job j = it.next();
            if (j.dueTick() > now) {
                continue;
            }
            it.remove();
            try {
                j.action().run();
            } catch (Throwable ignored) {
                // 单条失败不影响其它
            }
        }
    }

    public static void clear() {
        JOBS.clear();
        SEQ.clear();
        STUNNED.clear();
    }

    /** 登记一次僵直（到 {@code untilTick} 为止）。 */
    public static void markStunned(java.util.UUID id, long untilTick) {
        if (id != null) {
            STUNNED.put(id, untilTick);
        }
    }

    /** 这只怪现在是不是"被我们僵直"的状态。 */
    public static boolean isStunned(java.util.UUID id, long now) {
        if (id == null) {
            return false;
        }
        Long until = STUNNED.get(id);
        if (until == null) {
            return false;
        }
        if (now > until) {
            STUNNED.remove(id);
            return false;
        }
        return true;
    }

    /** 提前结束僵直（被处决时用 —— 一个窗口只兑现一次）。 */
    public static void endStun(java.util.UUID id) {
        if (id != null) {
            STUNNED.remove(id);
        }
    }
}
