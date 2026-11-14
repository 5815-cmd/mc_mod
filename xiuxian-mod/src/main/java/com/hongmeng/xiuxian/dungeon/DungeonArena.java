package com.hongmeng.xiuxian.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 副本里的"战斗场地"。
 *
 * <p>四座秘境原来都是在噪声地形上刷怪——平的、没层次、打起来没感觉。
 * 这里给每座秘境在 {@code (0,0)} 现场起一座<b>设计过的斗场</b>：</p>
 *
 * <ul>
 *   <li><b>台基</b>：三层石台基 + 收分勒脚，和外面的中式建筑一个语法；</li>
 *   <li><b>中轴对称</b>：地面纹样、四向踏道、四座角塔全部关于中轴镜像；</li>
 *   <li><b>垂直层次</b>：中央三级祭坛（进场落点）、四个 2 格高的擂台、
 *       四座 14 格高的角塔、外圈五层看台；</li>
 *   <li><b>留白</b>：半径 19 以内是干净的战斗地面——够跑、够放风筝。</li>
 * </ul>
 *
 * <h3>为什么不写成结构 NBT</h3>
 * <p>场地要落在"进门点"的原地形高度上，而四个维度的地形都是噪声，
 * 用 NBT 得先算高度再挑位置；现场生成反而更稳（也不会因为别处改了结构 JSON 而失效）。
 * 结构 NBT 那套留给主世界的仙门建筑。</p>
 *
 * <h3>刷怪不会被搞坏</h3>
 * <p>半径 19 以内只有中央祭坛（半径 3）和四个 2 格高的擂台（在斜角 12 格处），
 * 所以老的"按高度图刷怪"照样落在场地上；{@link #spawnSpot} 又给
 * {@link DungeonRealm} 提供了一条"就在场地地面上找落脚点"的更稳路径。</p>
 */
public final class DungeonArena {

    /** 战斗地面半径（切比雪夫距离），这一段必须是干净的。 */
    public static final int PLAZA_R = 19;

    /** 场地外沿半径：看台、角塔、台基都在这之内。 */
    public static final int BUILD_R = 24;

    /** 采地形高度用的探测环：在场地之外，永远不会被我们改到，所以 floorY 稳定。 */
    private static final int PROBE_R0 = 28;
    private static final int PROBE_R1 = 32;

    /** 地面以上清空多少格（够打也够飞）。 */
    private static final int HEADROOM = 16;

    /**
     * 台基往下最多垫多少格。
     *
     * <p>副本是噪声地形（{@code minecraft:overworld} 噪声），场地按探测环中位数找平，
     * 边缘地形可能比中位数低十几格。原来垫 8 格，地形再低就会在场地外沿露出
     * 「悬空的台基裙边」；垫 12 格能盖住绝大多数起伏。</p>
     *
     * <p>预算可控：台基循环是 {@code (2*BUILD_R+1)² = 49×49 = 2401} 列，
     * 每列最多垫 {@code FOUNDATION_MAX} 格，所以最坏情况
     * {@code 2401 × 12 ≈ 2.9 万} 次 setBlock（相对单维度 40 万预算很小），
     * 且下面用 {@code Math.max(fy-FOUNDATION_MAX, ground-1)} 保证不会无谓地往地形里填。</p>
     */
    private static final int FOUNDATION_MAX = 12;

    /** 每座秘境算好的地面高度（服务端重启后靠探测环重算，结果一致）。 */
    private static final Map<Dungeon, Integer> FLOOR_Y = new EnumMap<>(Dungeon.class);

    /** 本次运行已经建好的（建完就不再重复写方块）。 */
    private static final Set<Dungeon> BUILT = EnumSet.noneOf(Dungeon.class);

    /** 建失败的：本次会话不再重试，免得每 tick 抛一次异常。 */
    private static final Set<Dungeon> FAILED = EnumSet.noneOf(Dungeon.class);

    // ------------------------------------------------------------------
    // PERF(A)：跨 tick 分片任务
    //
    // 老实现：世界初始化（DungeonRealmEvents.onLevelTick）/ 副本进场（DungeonRealm.enter）
    // 直接同步 ensureBuilt()，一口气写 1.6~1.8 万格方块（实测单维度 ~1.5s、
    // 4 个维度把服务端卡出 "Can't keep up! Running 2896ms or 57 ticks behind"）。
    //
    // 现在：ensureBuilt()/tickEnsure() 只「入队」，由 DungeonRealmEvents.onLevelTick
    // 每 tick 限时推进：
    //   ① 异步 ticket 预载 35 个区块（不阻塞，worker 线程分摊生成）；
    //   ② 战斗场地逐阶段 + 游标分片（每 tick ≤8ms，断点续跑，幂等）；
    //   ③ 大本营：中式主殿 1 tick（ChineseHall 不在本次可改范围，无法切内部），
    //      4 个主题配殿各 1 tick；
    //   ④ 全部完成后才 BUILT.add + 释放 ticket，幂等语义不变。
    // ------------------------------------------------------------------
    private static final Map<Dungeon, BuildTask> TASKS = new EnumMap<>(Dungeon.class);

    private static final int P_PRELOAD = 0;
    private static final int P_ARENA = 1;
    private static final int P_HALL = 2;
    private static final int P_DONE = 3;

    /** 一个副本维度的「战场 + 大本营」分片任务。 */
    private static final class BuildTask {

        final ServerLevel level;
        final Dungeon dungeon;
        /**
         * 非阻塞预载范围（区块）：x −3..3（方块 −48..63）、z −4..3（方块 −64..63）。
         * 覆盖战斗场地 r=24、大本营 x±26 / z −51..−13、floorY 探测环 r=32，
         * 以及 {@link DungeonForecourt#ensureRealm}（r≤43）开工前的 7×7 预载 ——
         * 这样进场路径里的前庭施工不会再同步生成区块。
         */
        final TickWork.ChunkPreload preload = new TickWork.ChunkPreload(-3, -4, 3, 3);
        final long startedNanos = System.nanoTime();
        long lastNanos;
        long maxTickGapMs;
        long preloadMs;
        long buildMs;
        int ticks;
        int phase = P_PRELOAD;
        int themePart;
        int fy;
        boolean arenaSkipped;
        int landmarkPlaced;

        BuildTask(ServerLevel level, Dungeon dungeon) {
            this.level = level;
            this.dungeon = dungeon;
        }
    }

    private DungeonArena() {
    }

    // ------------------------------------------------------------------
    // 材料：四座秘境四种气质
    // ------------------------------------------------------------------

    private static BlockState floorMain(Dungeon d) {
        return switch (d) {
            case EPOCH -> Blocks.SMOOTH_QUARTZ.defaultBlockState();
            case BEAST -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case DEMON -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case VOID -> Blocks.END_STONE_BRICKS.defaultBlockState();
        };
    }

    private static BlockState floorAlt(Dungeon d) {
        return switch (d) {
            case EPOCH -> Blocks.QUARTZ_BLOCK.defaultBlockState();
            case BEAST -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case DEMON -> Blocks.NETHER_BRICKS.defaultBlockState();
            case VOID -> Blocks.PURPUR_BLOCK.defaultBlockState();
        };
    }

    private static BlockState podium(Dungeon d) {
        return switch (d) {
            case EPOCH -> Blocks.STONE_BRICKS.defaultBlockState();
            case BEAST -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case DEMON -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case VOID -> Blocks.OBSIDIAN.defaultBlockState();
        };
    }

    private static BlockState podiumAlt(Dungeon d) {
        return switch (d) {
            case EPOCH -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            case BEAST -> Blocks.BONE_BLOCK.defaultBlockState();
            case DEMON -> Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            case VOID -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
        };
    }

    private static BlockState trim(Dungeon d) {
        return switch (d) {
            case EPOCH -> Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
            case BEAST -> Blocks.CHISELED_DEEPSLATE.defaultBlockState();
            case DEMON -> Blocks.GILDED_BLACKSTONE.defaultBlockState();
            case VOID -> Blocks.AMETHYST_BLOCK.defaultBlockState();
        };
    }

    /** 中央祭坛顶上的"阵眼"，同时也是"这座场地建过没有"的标记方块。 */
    private static BlockState core(Dungeon d) {
        return switch (d) {
            case EPOCH -> Blocks.GOLD_BLOCK.defaultBlockState();
            case BEAST -> Blocks.BONE_BLOCK.defaultBlockState();
            case DEMON -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case VOID -> Blocks.AMETHYST_BLOCK.defaultBlockState();
        };
    }

    private static BlockState light(Dungeon d) {
        return switch (d) {
            case EPOCH -> Blocks.SEA_LANTERN.defaultBlockState();
            case BEAST -> Blocks.SHROOMLIGHT.defaultBlockState();
            case DEMON -> Blocks.SOUL_LANTERN.defaultBlockState();
            case VOID -> Blocks.END_ROD.defaultBlockState();
        };
    }

    private static BlockState accent(Dungeon d) {
        return switch (d) {
            case EPOCH -> Blocks.QUARTZ_PILLAR.defaultBlockState();
            case BEAST -> Blocks.DEEPSLATE_TILE_WALL.defaultBlockState();
            case DEMON -> Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState();
            case VOID -> Blocks.END_STONE_BRICK_WALL.defaultBlockState();
        };
    }

    // ------------------------------------------------------------------
    // 对外
    // ------------------------------------------------------------------

    /** 玩家每 tick 调一次：建过就直接返回，几乎零开销。 */
    public static void tickEnsure(ServerLevel level, Dungeon dungeon) {
        if (level == null || dungeon == null || BUILT.contains(dungeon)) {
            return;
        }
        // PERF(A): 只入队，真正的施工由 DungeonRealmEvents.onLevelTick 每 tick 限时推进。
        requestBuild(level, dungeon);
    }

    /**
     * 入队「战场 + 大本营」分片任务（幂等，已入队/已建/已失败都直接返回）。
     *
     * <p>PERF(A)：这个世界初始化 / 进场 / 命令三条路径原来都会同步调 {@link #ensureBuilt}，
     * 现在是"入队即返回"，即时开销只有一个 Map 查询。</p>
     */
    public static void requestBuild(ServerLevel level, Dungeon dungeon) {
        if (level == null || dungeon == null) {
            return;
        }
        if (BUILT.contains(dungeon) || FAILED.contains(dungeon) || TASKS.containsKey(dungeon)) {
            return;
        }
        BuildTask task = new BuildTask(level, dungeon);
        TASKS.put(dungeon, task);
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 战场任务已入队：异步预载 {} 个区块（ticket 限流），就绪后分片施工",
                dungeon.dimensionId(), task.preload.total());
    }

    /**
     * 把这座秘境的战斗场地建起来（幂等：用中央阵眼方块判定，建过就跳过）。
     *
     * @return 场地的地面 y（站人层的下一格）
     */

    /**
     * 确保副本大本营已经建好（**独立于竞技场**）。
     *
     * <p>为什么单独一个方法：竞技场的幂等判据在"早就去过"的维度上会提前返回，
     * 连带着把大本营的建造也跳过了 —— 玩家因此看不到血魔教大本营之类的建筑。
     * 拆出来之后，两者各自幂等、互不影响。</p>
     */
    public static void ensureLandmark(ServerLevel level, Dungeon dungeon, int fy) {
        try {
            int placed = DungeonLandmark.build(level, dungeon, fy);
            if (placed > 0) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                        "[arena] {} 大本营补建完成：{} 格", dungeon.dimensionId(), placed);
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.error(
                    "[arena] {} 大本营补建失败（不影响副本可玩）", dungeon.dimensionId(), t);
        }
    }

    /**
     * 按维度反查地面高度（供 Boss 大本营保护用）。
     *
     * <p>保护判定只需要大概是哪一层，不需要精确到格 ——
     * 所以这里用维度名反查 {@link Dungeon}，拿不到就返回 64（常见地表高度），
     * 宁可放宽也不误伤。</p>
     */
    public static int floorYForProtection(ServerLevel level) {
        try {
            String path = level.dimension().location().getPath();
            for (Dungeon d : Dungeon.values()) {
                if (d.dimensionId().equals(path)) {
                    return floorY(level, d);
                }
            }
        } catch (Throwable ignored) {
            // 落在默认值上
        }
        return 64;
    }

    public static int ensureBuilt(ServerLevel level, Dungeon dungeon) {
        // PERF(A): 老实现同步写 1.6~1.8 万格（~1.5s / 维度），这里改成入队即返回；
        // 本方法返回值只是"当前已知 floorY"，可能还是临时值（施工时会用真值覆盖缓存）。
        requestBuild(level, dungeon);
        return floorY(level, dungeon);
    }

    /** 本会话是否已结算（建好 / 建失败）。外围地貌任务是"战场结算后才动工"。 */
    public static boolean isSettled(Dungeon dungeon) {
        return BUILT.contains(dungeon) || FAILED.contains(dungeon);
    }

    /** 本会话是否建失败（外围地貌任务据此决定是否继续，避免死等）。 */
    public static boolean isBuildFailed(Dungeon dungeon) {
        return FAILED.contains(dungeon);
    }

    /** 是否还有分片任务在推进（调试/日志用）。 */
    public static boolean isTaskPending(Dungeon dungeon) {
        return TASKS.containsKey(dungeon);
    }

    /**
     * 每 tick 推进一次「战场 + 大本营」分片任务（由 {@link DungeonRealmEvents#onLevelTick} 驱动）。
     *
     * <p>PERF(A)：老实现一次性同步写 1.6~1.8 万格（单维度实测 ~1.5s，世界初始化 4 个维度
     * 直接卡出 {@code Can't keep up! Running 2896ms or 57 ticks behind}）。现在拆成：</p>
     * <ol>
     *   <li>异步 ticket 预载 35 个区块（每 tick 只轮询，不阻塞）；</li>
     *   <li>一个 tick 建战斗场地（十几~几十 ms，实测值写进日志）；</li>
     *   <li>一个 tick 建大本营（ChineseHall 主殿 + 主题配殿）；</li>
     *   <li>全部完成才 {@code BUILT.add}，isBuilt 幂等语义不变。</li>
     * </ol>
     *
     * @param deadlineNanos 本 tick 共享预算截止时间（{@link TickWork#BUDGET_NANOS}）
     * @return 任务是否在这一 tick 刚刚完成
     */
    public static boolean advanceTask(ServerLevel level, Dungeon dungeon, long deadlineNanos) {
        BuildTask task = TASKS.get(dungeon);
        if (task == null || task.level != level) {
            return false;
        }
        long now = System.nanoTime();
        task.ticks++;
        if (task.lastNanos != 0L) {
            long gapMs = TickWork.ms(now - task.lastNanos);
            if (gapMs > task.maxTickGapMs) {
                task.maxTickGapMs = gapMs;
            }
        }
        task.lastNanos = now;
        try {
            // ---------------- ① 异步预载（35 区块，绝不阻塞） ----------------
            if (task.phase == P_PRELOAD) {
                if (!task.preload.advance(level)) {
                    if (task.ticks % 40 == 0) {
                        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                                "[arena] {} 战场预载中：{} 区块（{} tick，最慢相邻 tick {} ms）",
                                dungeon.dimensionId(), task.preload.progress(),
                                task.ticks, task.maxTickGapMs);
                    }
                    return false;
                }
                task.preloadMs = TickWork.ms(System.nanoTime() - task.startedNanos);
                com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                        "[arena] {} 战场预载完成：{} 个区块 / {} tick / {} ms（异步 ticket）",
                        dungeon.dimensionId(), task.preload.total(), task.ticks, task.preloadMs);
                task.fy = computeFloorY(level, dungeon);        // 区块已就绪：真值，写入缓存
                task.phase = P_ARENA;
            }
            // ---------------- ② 战斗场地（一个 tick：几百 ms 以下的写字量） ----------------
            if (task.phase == P_ARENA) {
                int coreY = findBuiltCoreY(level, dungeon, task.fy);
                if (coreY != Integer.MIN_VALUE) {
                    // 老存档已有场地：与原实现一样复用、不重建（幂等语义不变）
                    if (coreY != task.fy + 3) {
                        int oldFy = task.fy;
                        task.fy = coreY - 3;
                        FLOOR_Y.put(dungeon, task.fy);
                        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                                "[arena] {} 找到已建场地（核心 y={}），floorY 由 {} 修正为 {}，复用不重建",
                                dungeon.dimensionId(), coreY, oldFy, task.fy);
                    }
                    task.arenaSkipped = true;
                } else {
                    long t0 = System.nanoTime();
                    build(level, dungeon, task.fy);
                    rescue(level, dungeon, task.fy);
                    task.buildMs = TickWork.ms(System.nanoTime() - t0);
                    com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                            "[arena] {} 战斗场地就绪：floorY={} 中心=({},{}) 地面半径={} 外沿={}（单 tick {} ms）",
                            dungeon.dimensionId(), task.fy, 0, 0, PLAZA_R, BUILD_R, task.buildMs);
                }
                task.phase = P_HALL;
            }
            // ---------------- ③ 大本营（一个 tick：主殿 + 主题配殿） ----------------
            if (task.phase == P_HALL) {
                long t0 = System.nanoTime();
                task.landmarkPlaced = DungeonLandmark.build(level, dungeon, task.fy);
                if (task.landmarkPlaced > 0) {
                    com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                            "[arena] {} 大本营补建完成：{} 格（单 tick {} ms）",
                            dungeon.dimensionId(), task.landmarkPlaced,
                            TickWork.ms(System.nanoTime() - t0));
                }
                task.phase = P_DONE;
            }
            // ---------------- ④ 结算：与老实现同一时刻标记（建完才 isBuilt） ----------------
            if (task.phase == P_DONE) {
                BUILT.add(dungeon);
                task.preload.release(level);
                TASKS.remove(dungeon);
                com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                        "[arena] {} 战场分片任务完成：tick={} 最慢相邻tick={}ms 预载={}ms "
                                + "施工={}ms 总={}ms",
                        dungeon.dimensionId(), task.ticks, task.maxTickGapMs, task.preloadMs,
                        task.buildMs, TickWork.ms(System.nanoTime() - task.startedNanos));
                return true;
            }
        } catch (Throwable t) {
            FAILED.add(dungeon);
            TASKS.remove(dungeon);
            task.preload.release(level);
            com.hongmeng.xiuxian.HongmengMod.LOGGER.error(
                    "[arena] {} 战场分片任务失败，本次会话不再重试", dungeon.dimensionId(), t);
        }
        return false;
    }

    /** 找老存档里已建场地的核心方块 y；没找到返回 {@link Integer#MIN_VALUE}。 */
    private static int findBuiltCoreY(ServerLevel level, Dungeon dungeon, int fy) {
        if (level.getBlockState(new BlockPos(0, fy + 3, 0)).is(core(dungeon).getBlock())) {
            return fy + 3;
        }
        for (int dy = -24; dy <= 24; dy++) {
            int coreY = fy + 3 + dy;
            if (coreY <= level.getMinBuildHeight() || coreY >= level.getMaxBuildHeight()) {
                continue;
            }
            if (level.getBlockState(new BlockPos(0, coreY, 0)).is(core(dungeon).getBlock())) {
                return coreY;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** 这座秘境的场地是不是已经建好了（调试 / 校验用）。 */
    public static boolean isBuilt(Dungeon dungeon) {
        return BUILT.contains(dungeon);
    }

    /** 场地地面高度（站人层的下一格）。 */
    public static int floorY(ServerLevel level, Dungeon dungeon) {
        Integer cached = FLOOR_Y.get(dungeon);
        if (cached != null) {
            return cached;
        }
        // PERF(A): 老实现这里同步 getChunk(FULL,true) 5×5=25 个区块才敢采样
        //（首进一个维度要几百 ms ~ 2s，命令/进场路径直接卡 tick）。
        // 现在：探测区块都已 FULL 就绪时才算真值并缓存；否则入队异步预载、先返回
        // "临时高度"（只用于日志/构图参考，**不写缓存**；施工分片任务预载完成后会用真值覆盖）。
        if (probeReady(level)) {
            return computeFloorY(level, dungeon);
        }
        requestBuild(level, dungeon);
        return provisionalFloorY(level, dungeon);
    }

    /** floorY 探测环（r=28..32）覆盖的区块是否都已真正生成完（只读查询，不阻塞）。 */
    private static boolean probeReady(ServerLevel level) {
        int cr = PROBE_R1 >> 4;
        for (int cx = -cr; cx <= cr; cx++) {
            for (int cz = -cr; cz <= cr; cz++) {
                if (!TickWork.chunkReady(level, cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 真正计算 floorY（调用前提：探测区块已全部 FULL 就绪，getHeight 才可信）。 */
    private static int computeFloorY(ServerLevel level, Dungeon dungeon) {
        int[] heights = new int[2048];
        int n = 0;
        for (int x = -PROBE_R1; x <= PROBE_R1; x++) {
            for (int z = -PROBE_R1; z <= PROBE_R1; z++) {
                int r = Math.max(Math.abs(x), Math.abs(z));
                if (r < PROBE_R0 || r > PROBE_R1) {
                    continue;
                }
                if (((x + z) & 1) != 0) {
                    continue;   // 隔一个采一个，够用又便宜
                }
                if (n < heights.length) {
                    heights[n++] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                }
            }
        }
        int fy;
        if (n == 0) {
            fy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        } else {
            Arrays.sort(heights, 0, n);
            // 中位数：既不会被一条沟拉低，也不会被一座山抬高
            fy = heights[n / 2];
        }
        fy = Math.max(level.getMinBuildHeight() + 8, Math.min(fy, level.getMaxBuildHeight() - 40));
        FLOOR_Y.put(dungeon, fy);
        return fy;
    }

    /**
     * 预载还没完成时的「临时 floorY」：只用已经真正生成完的探测列采样。
     *
     * <p>PERF(A)：绝不因为探测而同步生成区块。这个值只用来打日志/给调试命令回显，
     * 不写入 {@link #FLOOR_Y}；真正的施工与主题地貌都用分片任务预载完成后的真值。</p>
     */
    private static int provisionalFloorY(ServerLevel level, Dungeon dungeon) {
        int[] heights = new int[512];
        int n = 0;
        for (int x = -PROBE_R1; x <= PROBE_R1; x++) {
            for (int z = -PROBE_R1; z <= PROBE_R1; z++) {
                int r = Math.max(Math.abs(x), Math.abs(z));
                if (r < PROBE_R0 || r > PROBE_R1 || ((x + z) & 1) != 0) {
                    continue;
                }
                if (!TickWork.chunkReady(level, x >> 4, z >> 4)) {
                    continue;
                }
                if (n < heights.length) {
                    heights[n++] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                }
            }
        }
        if (n == 0) {
            return 64;      // 一列都没就绪：给个常见地表高度，仅供日志（施工一定用真值）
        }
        Arrays.sort(heights, 0, n);
        int fy = heights[n / 2];
        return Math.max(level.getMinBuildHeight() + 8, Math.min(fy, level.getMaxBuildHeight() - 40));
    }

    /** 场地地面上能站的格子（给刷怪/传送用）。没建好就返回 {@code null}。 */
    public static BlockPos spawnSpot(ServerLevel level, Dungeon dungeon, int x, int z) {
        Integer fy = FLOOR_Y.get(dungeon);
        if (fy == null || !BUILT.contains(dungeon)) {
            return null;
        }
        for (int dy = 0; dy >= -3; dy--) {
            BlockPos p = new BlockPos(x, fy + 1 + dy, z);
            if (standable(level, p)) {
                return p;
            }
        }
        for (int dy = 1; dy <= 2; dy++) {
            BlockPos p = new BlockPos(x, fy + 1 + dy, z);
            if (standable(level, p)) {
                return p;
            }
        }
        return null;
    }

    /** 这个坐标在不在战斗地面上。 */
    public static boolean onPlaza(int x, int z) {
        return Math.max(Math.abs(x), Math.abs(z)) <= PLAZA_R;
    }

    // ------------------------------------------------------------------
    // 建造
    // ------------------------------------------------------------------

    private static void build(ServerLevel level, Dungeon dungeon, int fy) {
        BlockState fMain = floorMain(dungeon);
        BlockState fAlt = floorAlt(dungeon);
        BlockState pod = podium(dungeon);
        BlockState podAlt = podiumAlt(dungeon);
        BlockState trimB = trim(dungeon);
        BlockState coreB = core(dungeon);
        BlockState lightB = light(dungeon);
        BlockState accentB = accent(dungeon);
        BlockState air = Blocks.AIR.defaultBlockState();

        int outer = BUILD_R + 2;

        // ① 掏空场地范围（顺手把水面也削掉，免得打着打着掉水里）
        for (int x = -outer; x <= outer; x++) {
            for (int z = -outer; z <= outer; z++) {
                for (int y = fy + 1; y <= fy + HEADROOM; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.isEmptyBlock(p)) {
                        level.setBlock(p, air, 2);
                    }
                }
            }
        }

        // ② 台基：往下补到原地形（不会悬空），往上垫到场地层
        for (int x = -BUILD_R; x <= BUILD_R; x++) {
            for (int z = -BUILD_R; z <= BUILD_R; z++) {
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int bottom = Math.max(fy - FOUNDATION_MAX, ground - 1);
                for (int y = Math.max(level.getMinBuildHeight() + 1, bottom); y < fy; y++) {
                    BlockState b = (fy - y) == 1 ? podAlt : pod;
                    level.setBlock(new BlockPos(x, y, z), b, 2);
                }
            }
        }

        // ③ 战斗地面 + 中轴/同心纹样
        for (int x = -PLAZA_R; x <= PLAZA_R; x++) {
            for (int z = -PLAZA_R; z <= PLAZA_R; z++) {
                int r = Math.max(Math.abs(x), Math.abs(z));
                boolean ringLine = r == PLAZA_R || r == PLAZA_R - 6 || r == PLAZA_R - 12;
                boolean axis = x == 0 || z == 0;
                boolean diag = Math.abs(x) == Math.abs(z) && r > 4;
                BlockState b = fMain;
                if (ringLine) {
                    b = trimB;
                } else if (axis || diag) {
                    b = fAlt;
                }
                level.setBlock(new BlockPos(x, fy, z), b, 2);
            }
        }

        // ④ 外圈五层看台 + 顶层女儿墙（四向踏道口留空）
        for (int r = PLAZA_R + 1; r <= BUILD_R; r++) {
            int row = r - PLAZA_R;
            int y = fy + row;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) {
                        continue;
                    }
                    if (inStairGap(x, z, r)) {
                        continue;
                    }
                    level.setBlock(new BlockPos(x, y, z), row >= 4 ? podAlt : pod, 2);
                    if (r == BUILD_R) {
                        level.setBlock(new BlockPos(x, y + 1, z), trimB, 2);
                    } else {
                        level.setBlock(new BlockPos(x, y - 1, z), pod, 2);
                    }
                }
            }
        }

        // ⑤ 四向踏道：战斗地面 -> 看台（朝外逐级抬高）
        for (int i = 1; i <= 5; i++) {
            int r = PLAZA_R + i;
            for (int w = -2; w <= 2; w++) {
                level.setBlock(new BlockPos(w, fy + i, r), stair(Direction.SOUTH), 2);
                level.setBlock(new BlockPos(w, fy + i, -r), stair(Direction.NORTH), 2);
                level.setBlock(new BlockPos(r, fy + i, w), stair(Direction.EAST), 2);
                level.setBlock(new BlockPos(-r, fy + i, w), stair(Direction.WEST), 2);
            }
        }

        // ⑥ 中央祭坛（进场落点）：三级台阶，顶上 3x3 平地 + 四角灯
        fillArea(level, 0, 0, 3, fy + 1, podAlt);
        fillArea(level, 0, 0, 2, fy + 2, podAlt);
        fillArea(level, 0, 0, 1, fy + 3, podAlt);
        level.setBlock(new BlockPos(0, fy + 3, 0), coreB, 2);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                level.setBlock(new BlockPos(sx * 2, fy + 3, sz * 2), lightB, 2);
            }
        }

        // ⑦ 四个 2 格高的擂台（斜角 12 格，给"抢高地"用）
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                platform(level, sx * 12, sz * 12, fy, pod, podAlt, trimB);
            }
        }

        // ⑧ 四座角塔：3x3 基座 + 方柱 + 顶灯
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int cx = sx * 21;
                int cz = sz * 21;
                for (int x = cx - 1; x <= cx + 1; x++) {
                    for (int z = cz - 1; z <= cz + 1; z++) {
                        for (int y = fy; y <= fy + 5; y++) {
                            level.setBlock(new BlockPos(x, y, z), y == fy + 5 ? trimB : pod, 2);
                        }
                    }
                }
                for (int y = fy + 6; y <= fy + 13; y++) {
                    level.setBlock(new BlockPos(cx, y, cz), accentB, 2);
                }
                level.setBlock(new BlockPos(cx, fy + 14, cz), lightB, 2);
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    level.setBlock(new BlockPos(cx + d.getStepX(), fy + 13, cz + d.getStepZ()),
                            lightB, 2);
                }
            }
        }

        // ⑨ 四向石灯柱（在刷怪环之外：17 > 15.5）
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int px = d.getStepX() * 17;
            int pz = d.getStepZ() * 17;
            for (int y = fy + 1; y <= fy + 7; y++) {
                level.setBlock(new BlockPos(px, y, pz), accentB, 2);
            }
            level.setBlock(new BlockPos(px, fy + 8, pz), lightB, 2);
        }
    }

    /** 以 (cx,cz) 为中心、半径 r 的实心方块（切比雪夫方块）。 */
    private static void fillArea(ServerLevel level, int cx, int cz, int r, int y, BlockState b) {
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                level.setBlock(new BlockPos(x, y, z), b, 2);
            }
        }
    }

    /** 2 格高的擂台：5x5 台面 + 朝内的两级踏步。 */
    private static void platform(ServerLevel level, int cx, int cz, int fy,
                                 BlockState pod, BlockState podAlt, BlockState trimB) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                boolean edge = Math.max(Math.abs(x - cx), Math.abs(z - cz)) == 2;
                level.setBlock(new BlockPos(x, fy + 1, z), edge ? trimB : podAlt, 2);
                level.setBlock(new BlockPos(x, fy + 2, z), edge ? trimB : podAlt, 2);
            }
        }
        int inX = cx > 0 ? -1 : 1;
        int inZ = cz > 0 ? -1 : 1;
        for (int i = 1; i <= 2; i++) {
            level.setBlock(new BlockPos(cx + inX * (2 + i), fy + 1, cz),
                    stair(inX > 0 ? Direction.EAST : Direction.WEST), 2);
            level.setBlock(new BlockPos(cx, fy + 1, cz + inZ * (2 + i)),
                    stair(inZ > 0 ? Direction.SOUTH : Direction.NORTH), 2);
        }
    }

    /** 踏道口：四条中轴上的 5 格宽缺口。 */
    private static boolean inStairGap(int x, int z, int r) {
        boolean onAxisX = Math.abs(x) <= 2 && Math.abs(z) == r;
        boolean onAxisZ = Math.abs(z) <= 2 && Math.abs(x) == r;
        return onAxisX || onAxisZ;
    }

    private static BlockState stair(Direction facing) {
        return Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static boolean standable(ServerLevel level, BlockPos p) {
        BlockState below = level.getBlockState(p.below());
        if (below.isAir() || !below.getFluidState().isEmpty()
                || below.getCollisionShape(level, p.below()).isEmpty()) {
            return false;
        }
        return level.isEmptyBlock(p) && level.isEmptyBlock(p.above());
    }

    /**
     * 场地是"先有地形、后有场地"的：建完之后把被埋住的玩家/怪挪到地面上。
     *
     * <p>不然进门那一瞬间刷出来的怪会被台基埋掉，玩家就只能干等——那是最恶心的一种卡关。</p>
     */
    private static void rescue(ServerLevel level, Dungeon dungeon, int fy) {
        BlockPos landing = new BlockPos(0, fy + 4, 0);
        for (var player : level.players()) {
            if (Math.max(Math.abs(player.getBlockX()), Math.abs(player.getBlockZ())) <= BUILD_R + 2
                    && (player.getBlockY() < fy + 1 || !level.isEmptyBlock(player.blockPosition()))) {
                player.teleportTo(level, landing.getX() + 0.5D, landing.getY(),
                        landing.getZ() + 0.5D, player.getYRot(), player.getXRot());
            }
        }
        AABB area = new AABB(-BUILD_R - 4, level.getMinBuildHeight(), -BUILD_R - 4,
                BUILD_R + 4, fy + HEADROOM + 8, BUILD_R + 4);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            BlockPos p = mob.blockPosition();
            if (Math.max(Math.abs(p.getX()), Math.abs(p.getZ())) > BUILD_R) {
                continue;
            }
            if (!level.isEmptyBlock(p) || mob.getY() < fy) {
                BlockPos spot = spawnSpot(level, dungeon, p.getX(), p.getZ());
                if (spot == null) {
                    spot = landing;
                }
                mob.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D,
                        mob.getYRot(), mob.getXRot());
            }
        }
    }
}
