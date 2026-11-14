package com.hongmeng.xiuxian.realworld;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <b>真世界演出的延迟调度器</b>（服务端每 tick 推进一次）。
 *
 * <h2>为什么需要它</h2>
 * <p>过场要"三次闷雷间隔越来越短""两次红光脉冲"—— 这些都是<b>未来某个时刻</b>要发生的事。
 * 直接在 {@code activate()} 里连发三次雷声，玩家听到的是<b>一声混响</b>，压迫感全没了。</p>
 *
 * <h2>为什么不复用 SpellFxRuntime 的队列</h2>
 * <p>那个队列只发"法术特效包"，而这里要发<b>声音、闪屏包、粒子</b>——
 * 三种不同的东西。硬塞进去会把那个队列的语义搞乱。</p>
 *
 * <p>所以这个调度器收的是 {@link Runnable}，通用、干净。
 * 队列量很小（一场演出几条），线性扫足够。</p>
 */
public final class RealWorldScheduler {

    /** 一条待执行的动作。 */
    private record Job(long dueTick, Runnable action) {
    }

    private static final List<Job> JOBS = new ArrayList<>();

    private RealWorldScheduler() {
    }

    /** 排队一个动作（delayTicks 之后执行）。 */
    public static void after(ServerLevel level, int delayTicks, Runnable action) {
        if (level == null || action == null) {
            return;
        }
        if (JOBS.size() > 64) {
            JOBS.clear();   // 兜底：异常情况下不堆积
        }
        JOBS.add(new Job(level.getGameTime() + Math.max(0, delayTicks), action));
    }

    /** 每 tick 调用。 */
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
                // 单条失败不能拖垮整场演出
            }
        }
    }

    /** 世界重置 / 服务器关闭时清空。 */
    public static void clear() {
        JOBS.clear();
    }
}
