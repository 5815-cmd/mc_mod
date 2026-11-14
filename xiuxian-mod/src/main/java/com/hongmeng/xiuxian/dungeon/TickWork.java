package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>跨 tick 分片施工的公共工具</b>（卡顿治理 / PERF(A)）。
 *
 * <p>背景：副本维度、天外天场景在「进场 / 世界初始化 / 调试命令」时原本会
 * <b>同步</b> {@code getChunk(FULL,true)} 上百个区块 + 一次写几千上万格方块，
 * 实测一帧卡 7.4 秒（{@code Can't keep up! Running 7445ms or 148 ticks behind}）。
 * 这里提供两件基础设施，把一次性同步生成拆成「每 tick 只花几毫秒」的持续任务：</p>
 *
 * <ol>
 *   <li><b>{@link ChunkPreload 异步区块预载}：用 {@code ServerChunkCache.addRegionTicket}
 *       给待生成区块挂 {@link TicketType}（distance=0 → 区块等级 33 = FULL，
 *       只生成到"完整区块"、不进入 ticking），让原版区块管线在 worker 线程里
 *       自己分摊生成；服务端每 tick 只做一次 {@code getChunkNow()} 轮询
 *       （只读、不阻塞、不触发生成），已就绪才允许后续写方块。
 *       同时挂着的 ticket 用窗口限流，避免一次把几百个区块全排进生成队列。</li>
 *   <li><b>{@link #BUDGET_NANOS 每 tick 时间预算}：施工循环用 {@code System.nanoTime()}
 *       实测控制，超预算就保存游标、下一 tick 从断点继续（幂等）。</li>
 * </ol>
 *
 * <p>注意：这里<b>不要</b>再调用阻塞式 {@code getChunk(x,z,FULL,true)} ——
 * 那正是 7 秒卡顿的来源。唯一例外是 {@link ChunkPreload} 在连续
 * {@value #STALL_LIMIT} tick 毫无进展时的兜底（正常不会触发，且只强制 1 个区块）。</p>
 */
public final class TickWork {

    /** 单 tick 施工预算：8ms（实测口径，含轮询 + 写方块）。 */
    public static final long BUDGET_NANOS = 8_000_000L;

    /** 同时挂着的预载 ticket 上限（控制区块生成并发，避免 CPU 尖峰）。 */
    private static final int MAX_INFLIGHT = 12;

    /** 每 tick 最多新挂 ticket 数（让生成队列平滑爬坡）。 */
    private static final int MAX_ADDS_PER_TICK = 4;

    /** 连续这么多 tick 一个区块都没生成出来 → 打 WARN（不退回同步加载，避免卡 tick）。 */
    private static final int STALL_LIMIT = 400;

    /**
     * 预载专用 ticket 类型。
     *
     * <p>{@code TicketType.create(name, comparator)} 的 timeout=0：常驻，不会被自动清理，
     * 任务结束（或会话退出）由 {@link ChunkPreload#release} 统一移除。用自定义类型是为了
     * 不跟原版 forceload / portal 的 ticket 计数互相干扰。</p>
     */
    private static final TicketType<ChunkPos> PRELOAD = TicketType.create(
            "hongmeng_tick_preload", Comparator.comparingLong(ChunkPos::toLong));

    private TickWork() {
    }

    /** 这个区块的 FULL 区块是否已经真的生成完（只读查询，不阻塞、不触发生成）。 */
    public static boolean chunkReady(ServerLevel level, int cx, int cz) {
        ServerChunkCache cache = level.getChunkSource();
        // 先问 hasChunk（ticket 已把区块纳入"可见"范围，快速否定用），
        // 再用 getChunkNow 确认 FULL 区块真的生成完 —— 只读、不阻塞、不触发生成。
        return cache.hasChunk(cx, cz) && cache.getChunkNow(cx, cz) != null;
    }

    /** 纳秒 → 毫秒（日志用，四舍五入到整毫秒）。 */
    public static long ms(long nanos) {
        return nanos / 1_000_000L;
    }

    /**
     * 一批区块的<b>异步预载</b>：挂 ticket + 每 tick 轮询，已就绪的区块从待办里摘掉。
     *
     * <p>用法：构造后每 tick 调一次 {@link #advance(ServerLevel)}；返回 {@code true}
     * 表示全部区块 FULL 就绪。任务完成 / 失败 / 放弃时必须调 {@link #release} 释放 ticket，
     * 否则这些区块会一直被服务端留在内存里。</p>
     *
     * <p>幂等：同一批区块可以反复 advance（内部去重），已就绪的不会重复挂 ticket。</p>
     */
    public static final class ChunkPreload {

        /** 还没就绪的区块。 */
        private final List<ChunkPos> pending = new ArrayList<>();
        /** 已挂 ticket、但还没就绪的区块。 */
        private final List<ChunkPos> inflight = new ArrayList<>();
        /** 已就绪、但 ticket 还留着（施工期间保证区块不被卸载）的区块。 */
        private final List<ChunkPos> held = new ArrayList<>();
        /** 已挂过 ticket 的 long 集合（去重用，避免每次 List.contains O(n)）。 */
        private final LongOpenHashSet ticketed = new LongOpenHashSet();
        private final int total;
        private int loaded;
        private int stallTicks;
        private int lastLoaded = -1;

        /**
         * @param minCx 最小区块 x（含）
         * @param minCz 最小区块 z（含）
         * @param maxCx 最大区块 x（含）
         * @param maxCz 最大区块 z（含）
         */
        public ChunkPreload(int minCx, int minCz, int maxCx, int maxCz) {
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    pending.add(new ChunkPos(cx, cz));
                }
            }
            this.total = pending.size();
        }

        /** 圆盘（按方块半径）覆盖到的区块：只挑跟圆相交的区块，少生成没用到的。 */
        public static ChunkPreload ofCircle(int radius) {
            int cr = (radius + 15) >> 4;
            List<ChunkPos> all = new ArrayList<>();
            long r2 = (long) radius * (long) radius;
            for (int cx = -cr; cx <= cr; cx++) {
                for (int cz = -cr; cz <= cr; cz++) {
                    // 区块包围盒离原点最近距离（0 表示区块本身覆盖原点方向）
                    int nx = Math.max(0, Math.max(cx * 16, -((cx * 16) + 15)));
                    int nz = Math.max(0, Math.max(cz * 16, -((cz * 16) + 15)));
                    if ((long) nx * nx + (long) nz * nz <= r2) {
                        all.add(new ChunkPos(cx, cz));
                    }
                }
            }
            return new ChunkPreload(all);
        }

        private ChunkPreload(List<ChunkPos> chunks) {
            this.pending.addAll(chunks);
            this.total = pending.size();
        }

        /** 总区块数（基线口径，例如 17×17 = 289）。 */
        public int total() {
            return total;
        }

        /** 已经生成就绪的区块数。 */
        public int loaded() {
            return loaded;
        }

        /** 还有多少区块没就绪。 */
        public int remaining() {
            return pending.size();
        }

        /**
         * 推进一次（每 tick 调一次）：给还没挂 ticket 的区块补 ticket（窗口限流），
         * 把本轮已经就绪的区块从待办移到 held。
         *
         * @return {@code true} = 所有区块都已 FULL 就绪
         */
        public boolean advance(ServerLevel level) {
            ServerChunkCache cache = level.getChunkSource();
            int adds = 0;
            // 1) 先摘掉已经就绪的区块（同 tick 就绪的也一次性摘掉，附带把 ticket 保留在 held）
            for (int i = pending.size() - 1; i >= 0; i--) {
                ChunkPos pos = pending.get(i);
                if (!chunkReady(level, pos.x, pos.z)) {
                    continue;
                }
                pending.remove(i);
                loaded++;
                inflight.remove(pos);
                held.add(pos);
            }
            // 2) 需要时补 ticket：最多 MAX_ADDS_PER_TICK 个新 ticket、最多 MAX_INFLIGHT 个在飞
            if (!pending.isEmpty()) {
                for (int i = 0; i < pending.size(); i++) {
                    if (adds >= MAX_ADDS_PER_TICK || inflight.size() >= MAX_INFLIGHT) {
                        break;
                    }
                    ChunkPos pos = pending.get(i);
                    if (ticketed.contains(pos.toLong())) {
                        continue;
                    }
                    cache.addRegionTicket(PRELOAD, pos, 0, pos);
                    ticketed.add(pos.toLong());
                    inflight.add(pos);
                    adds++;
                }
            }
            // 3) 只观测：连续很久没进展就打 WARN（绝不退回同步 getChunk，避免又卡 tick）
            if (loaded == lastLoaded) {
                stallTicks++;
            } else {
                stallTicks = 0;
                lastLoaded = loaded;
            }
            if (stallTicks > STALL_LIMIT && stallTicks % STALL_LIMIT == 1) {
                ChunkPos pos = pending.isEmpty() ? null : pending.get(0);
                HongmengMod.LOGGER.warn(
                        "[slice] 区块预载 {}/{} 长时间无进展（下一区块 {}），继续等待原版区块管线",
                        loaded, total, pos);
            }
            return pending.isEmpty();
        }

        /** 释放所有 ticket（任务结束/失败时必须调）。区块不再按需保留后会自然卸载。 */
        public void release(ServerLevel level) {
            ServerChunkCache cache = level.getChunkSource();
            for (ChunkPos pos : inflight) {
                cache.removeRegionTicket(PRELOAD, pos, 0, pos);
            }
            for (ChunkPos pos : held) {
                cache.removeRegionTicket(PRELOAD, pos, 0, pos);
            }
            inflight.clear();
            held.clear();
            ticketed.clear();
            pending.clear();
            loaded = total;
        }

        /** 进度日志：{@code 37/289 区块}。 */
        public String progress() {
            return loaded + "/" + total;
        }
    }
}
