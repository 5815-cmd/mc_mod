package com.hongmeng.xiuxian.combat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 服务端的"延迟特效"小调度器。
 *
 * <p>规格 §四 要求法术有<b>起承转合</b>：命中之后还有"合（余韵）"——
 * 命中点缓慢消散的粒子、地面残留的焦痕。余韵天然是"未来若干 tick 里反复来几缕"，
 * 所以需要一个极小的定时队列。</p>
 *
 * <h2>为什么不用 {@code Thread.sleep} / 调度线程</h2>
 *
 * <p>绝对不能在游戏线程之外碰世界。这里只是"记一个到期刻 + 一个回调"，
 * 到点在 {@code LevelTickEvent} 里同步执行（和别的 tick 逻辑同一线程）。</p>
 *
 * <h2>边界</h2>
 *
 * <p>每个维度最多 {@value #MAX_TASKS} 个待办；超了直接丢新的（宁可少一次余韵，
 * 也不让某个刷怪塔把队列堆成内存泄漏）。任务数在正常情况下是"个位数"。</p>
 */
public final class CombatFxScheduler {

    /** 每个维度同时挂起的最大任务数。 */
    public static final int MAX_TASKS = 256;

    private static final Map<ResourceKey<Level>, List<Task>> TASKS = new HashMap<>();

    private CombatFxScheduler() {
    }

    private static final class Task {
        final long dueTick;
        final Consumer<ServerLevel> action;

        Task(long dueTick, Consumer<ServerLevel> action) {
            this.dueTick = dueTick;
            this.action = action;
        }
    }

    /**
     * 安排一个延迟任务。
     *
     * @param delayTicks 至少 1 tick
     */
    public static void schedule(ServerLevel level, int delayTicks, Consumer<ServerLevel> action) {
        if (level == null || action == null || delayTicks <= 0) {
            return;
        }
        List<Task> list = TASKS.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
        if (list.size() >= MAX_TASKS) {
            return;
        }
        list.add(new Task(level.getGameTime() + delayTicks, action));
    }

    /** 每 tick 推进（由 {@link CombatFeelEvents} 调）。 */
    public static void tick(ServerLevel level) {
        List<Task> list = TASKS.get(level.dimension());
        if (list == null || list.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Task> it = list.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.dueTick > now) {
                continue;
            }
            it.remove();
            try {
                task.action.accept(level);
            } catch (Throwable ignored) {
                // 单次特效失败不影响游戏：丢掉它，继续跑后面的
            }
        }
        if (list.isEmpty()) {
            TASKS.remove(level.dimension());
        }
    }

    /** 换存档 / 停服清理。 */
    public static void clear() {
        TASKS.clear();
    }

    /** 待办数量（诊断）。 */
    public static int pending(ServerLevel level) {
        List<Task> list = TASKS.get(level.dimension());
        return list == null ? 0 : list.size();
    }
}
