package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端粒子预算（{@code docs/combat-feel-spec.md} §零 硬约束 1）。
 *
 * <h2>为什么必须有</h2>
 *
 * <p>规格原话："华丽特效如果每 tick 喷 2000 粒子，mod 直接没法玩。
 * 密度做成 {@code HongmengConfig} 项，整合包作者能往下调。"</p>
 *
 * <h2>两个数：预算 + 倍率</h2>
 *
 * <ul>
 *   <li><b>每维度每 tick 的硬预算</b>（{@code combat.maxParticlesPerTick}，默认
 *       {@value CombatFeel#PARTICLE_BUDGET_PER_TICK}）：所有战斗特效共用这一个池子，
 *       花完就静默丢弃——宁可少几颗粒子，也不能掉帧。</li>
 *   <li><b>密度倍率</b>（{@code combat.particleDensity}，默认 1.0）：乘在每一种特效的
 *       基准数量上，整合包可以调到 0.3 甚至 0。</li>
 * </ul>
 *
 * <p>记账是"每维度 + 每游戏刻"的：用 {@code gameTime} 做戳，一旦发现戳变了就归零。
 * 掉线/换维度不需要清理，戳本身就会把它冲掉（表大小按维度数量，最多几个）。</p>
 */
public final class ParticleBudget {

    /** 默认每 tick 预算（配置读不到时的兜底）。 */
    public static final int DEFAULT_BUDGET = CombatFeel.PARTICLE_BUDGET_PER_TICK;

    private static final Map<ResourceKey<Level>, Counter> COUNTERS = new HashMap<>();

    /** 记账失败次数（诊断用：读配置出问题时能看出来）。 */
    private static int configFallbacks = 0;

    private ParticleBudget() {
    }

    private static final class Counter {
        long tick = Long.MIN_VALUE;
        int spent = 0;
    }

    /** 每 tick 的粒子预算（配置可调，夹在 0~4096）。 */
    public static int budget() {
        try {
            return Math.max(0, Math.min(4096,
                    HongmengConfig.SERVER.maxParticlesPerTick.get()));
        } catch (Throwable t) {
            configFallbacks++;
            return DEFAULT_BUDGET;
        }
    }

    /** 密度倍率（配置可调，夹在 0~4）。 */
    public static double density() {
        try {
            double d = HongmengConfig.SERVER.particleDensity.get();
            if (Double.isNaN(d) || d < 0.0D) {
                return 0.0D;
            }
            return Math.min(4.0D, d);
        } catch (Throwable t) {
            configFallbacks++;
            return 1.0D;
        }
    }

    /** 总开关：战斗特效（粒子/音效/顿帧/震动）是否启用。 */
    public static boolean enabled() {
        try {
            return HongmengConfig.SERVER.combatFeelEnabled.get();
        } catch (Throwable t) {
            configFallbacks++;
            return true;
        }
    }

    /** 本 tick 还剩多少预算。 */
    public static int remaining(ServerLevel level) {
        if (level == null) {
            return 0;
        }
        Counter c = counter(level);
        long now = level.getGameTime();
        if (c.tick != now) {
            c.tick = now;
            c.spent = 0;
        }
        return Math.max(0, budget() - c.spent);
    }

    /**
     * 申请 {@code requested} 颗粒子，返回<b>真正批准</b>的数量（可能被砍到 0）。
     */
    public static int spend(ServerLevel level, int requested) {
        if (level == null || requested <= 0) {
            return 0;
        }
        int left = remaining(level);
        int granted = CombatFeel.clampToBudget(requested, left);
        if (granted <= 0) {
            return 0;
        }
        Counter c = counter(level);
        c.spent += granted;
        return granted;
    }

    /** 已花掉的预算（诊断）。 */
    public static int spent(ServerLevel level) {
        if (level == null) {
            return 0;
        }
        Counter c = counter(level);
        return c.tick == level.getGameTime() ? c.spent : 0;
    }

    /** 配置读取走了兜底几次（诊断；正常永远是 0）。 */
    public static int configFallbacks() {
        return configFallbacks;
    }

    /** 换存档 / 停服时清账（由事件监听在 ServerStopped 之类的地方调用）。 */
    public static void clear() {
        COUNTERS.clear();
    }

    private static Counter counter(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        Counter c = COUNTERS.get(key);
        if (c == null) {
            // 维度就那几个，但也不能无限长：超过 16 个就直接清空重建
            if (COUNTERS.size() > 16) {
                COUNTERS.clear();
            }
            c = new Counter();
            COUNTERS.put(key, c);
        }
        return c;
    }
}
