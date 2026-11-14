package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.technique.Technique;
import net.minecraft.client.Minecraft;

/**
 * 客户端缓存的蓄力进度（蓄力条读它）。
 *
 * <p>进度不用每 tick 同步：服务端只在起手 / 中断 / 完成时发包，
 * 客户端按"开始刻 + 总时长"用本地游戏刻自己算比例。
 * 两边的 {@code getGameTime()} 在同一个维度里是一致的。</p>
 */
public final class ClientCastCache {

    private static Technique technique;
    private static long startTick;
    private static int totalTicks;

    private ClientCastCache() {
    }

    /** 服务端发包时调用。 */
    public static void set(Technique newTechnique, long newStartTick, int newTotalTicks) {
        if (newTechnique == null || newTotalTicks <= 0) {
            clear();
            return;
        }
        technique = newTechnique;
        startTick = newStartTick;
        totalTicks = newTotalTicks;
    }

    /**
     * 释放**完成/结束**的那一刻，在脚下铺一组超位魔法式法阵。
     *
     * <p>参考效果：头顶巨环 + 多层递减水平环 + 底部密集曼荼罗 + 斜插轨道环。
     * 半径按法术等级给（灭世级最大），普通法术只是小一圈。</p>
     *
     * <p>放在这里是为了**不新增网络包** —— 客户端本来就会收到"蓄力结束"这个信号。</p>
     */
    private static void spawnBurstCircles() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            float radius = 11.0F;
            if (technique != null) {
                radius = switch (technique.kind()) {
                    case ULTIMATE, GRAVITY, SLASH -> 30.0F;
                    case AOE -> 18.0F;
                    default -> 11.0F;
                };
            }
            // 用**法术自己的签名**生成法阵（图案/层数/转向/配色都按名字派生），
            // 寿命 90 tick ≈ 4.5 秒 —— 这就是玩家要的"释放后有残留，过一会才散"。
            if (technique != null) {
                com.hongmeng.xiuxian.client.MagicCircles.spellCast(
                        mc.player.position(), technique.element(), technique.name(),
                        radius, 1.0D);
            } else {
                com.hongmeng.xiuxian.client.MagicCircles.burst(
                        mc.player.position(), null, radius);
            }
        } catch (Throwable ignored) {
            // 表现层绝不影响游戏
        }
    }

    public static void clear() {
        // ⚠ 只有"蓄力真的走完了"才铺法阵。
        //
        // clear() 的调用时机包括 登录 / 换维度 / 重生 / 蓄力被打断 ——
        // 那些都不是"释放"，却都会走到这里。原来无条件铺，
        // 结果玩家什么都没放，脚下就炸出一整圈法阵（还特别挡视野）。
        boolean finished = technique != null && progress() >= 0.95F;
        if (finished) {
            spawnBurstCircles();
        }
        technique = null;
        startTick = 0L;
        totalTicks = 0;
    }

    /** 正在蓄力的功法；没有返回 null。 */
    public static Technique technique() {
        return technique;
    }

    public static boolean isCasting() {
        return technique != null;
    }

    /** 蓄力进度 0~1。 */
    public static float progress() {
        if (technique == null || totalTicks <= 0) {
            return 0.0F;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? startTick : mc.level.getGameTime();
        float ratio = (float) (now - startTick) / (float) totalTicks;
        if (ratio < 0.0F) {
            return 0.0F;
        }
        return Math.min(1.0F, ratio);
    }

    /** 还要多少秒（向上取整，至少 0.1 秒的显示粒度）。 */
    public static float remainingSeconds() {
        if (technique == null || totalTicks <= 0) {
            return 0.0F;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? startTick : mc.level.getGameTime();
        long left = Math.max(0L, startTick + totalTicks - now);
        return left / 20.0F;
    }
}
