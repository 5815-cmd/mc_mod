package com.hongmeng.xiuxian.realworld;

/**
 * <b>「恶」降临时的天空状态</b>（服务端写、客户端读）。
 *
 * <p>三个量：</p>
 * <ul>
 *   <li>{@code strength} 0~1：血红程度（同时也是裂口张开度）；</li>
 *   <li>{@code openTicks}：裂口已经张开的 tick（驱动眼睛"看向玩家"的动画）；</li>
 *   <li>{@code phase} 0~3：0 无 / 1 手臂 / 2 手臂+头 / 3 整个上半身。</li>
 * </ul>
 *
 * <p><b>tick 由谁推：</b>服务端只有 {@code EvilSummon}（召唤演出/战斗期）调
 * {@link #tick()}，然后经 {@code EvilSkyPacket} 同步给客户端。
 * （旧版没有任何地方调 {@code tick()} → {@code openTicks} 永远是 0，巨眼不会"动"；
 * 第五轮召唤演出升级时一并修掉。）</p>
 */
public final class EvilSky {

    private static volatile float strength = 0.0F;
    private static volatile long openTicks = 0L;
    private static volatile int phase = 0;

    private EvilSky() {
    }

    public static void set(float s, long ticks, int ph) {
        strength = Math.max(0.0F, Math.min(1.0F, s));
        openTicks = ticks;
        phase = ph;
    }

    public static float strength() {
        return strength;
    }

    public static long openTicks() {
        return openTicks;
    }

    public static int phase() {
        return phase;
    }

    /** 天空是否处于"恶"的状态（有强度或有阶段）。 */
    public static boolean active() {
        return strength > 0.0F || phase > 0;
    }

    public static void tick() {
        if (strength > 0.0F) {
            openTicks++;
        }
    }

    public static void clear() {
        strength = 0.0F;
        openTicks = 0L;
        phase = 0;
    }

    /** 网络同步用（客户端只读）。 */
    public static void applyFromNetwork(float s, long ticks, int ph) {
        set(s, ticks, ph);
    }
}
