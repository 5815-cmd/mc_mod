package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.compat.HongmengConfig;

/**
 * <b>法术特效档位</b>（玩家要求：「可以在设置里改」）。
 *
 * <p>0 = 关闭贴图特效（只留粒子，最省）· 1 = 低 · 2 = 中 · 3 = 高（默认）。</p>
 *
 * <p>读的是客户端配置 `spellFxQuality`；取不到就按"高"走 ——
 * 玩家的机器（i7-14650HX + RTX4060）带得动。</p>
 */
public final class SpellFxQuality {

    private SpellFxQuality() {
    }

    /**
     * PERF(C): 档位缓存（250ms 刷新一次）。
     *
     * <p>{@link #level()} 在渲染热路径上每帧会被调用几十次（每条特效的
     * 辉光层数 / 形状层数 / 画质判断），每次都去读 Forge 配置值。
     * 玩家在设置里改档位后最多 250ms 生效，肉眼看不出延迟。</p>
     */
    private static int cachedLevel = 3;
    private static long nextRefreshNanos = 0L;
    /** 缓存有效期（纳秒）。 */
    private static final long REFRESH_INTERVAL_NANOS = 250_000_000L;

    /** 当前档位 0~3。 */
    public static int level() {
        long now = System.nanoTime();
        if (now < nextRefreshNanos) {
            return cachedLevel;
        }
        nextRefreshNanos = now + REFRESH_INTERVAL_NANOS;
        int v;
        try {
            v = Math.max(0, Math.min(3, HongmengConfig.CLIENT.spellFxQuality.get()));
        } catch (Throwable ignored) {
            v = 3;
        }
        cachedLevel = v;
        return v;
    }

    /** 贴图特效的可见距离（档位越高看得越远）。 */
    public static double viewDistance() {
        return switch (level()) {
            case 0 -> 0.0D;
            case 1 -> 48.0D;
            case 2 -> 96.0D;
            default -> 176.0D;
        };
    }

    /** 叠画层数（档位越高层次越多）。 */
    public static int layers() {
        return switch (level()) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            default -> 3;
        };
    }

    /** 是否画"粒子点缀层"。 */
    public static boolean particles() {
        return level() >= 2;
    }
}
