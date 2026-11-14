package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * <b>副本维度的外围主题地貌</b>（玩家要求：「每个副本的地形生态要符合它的主题」）。
 *
 * <p>副本维度本来只有两样东西：{@link DungeonArena 中央战斗场地}（r≤24）和
 * {@link DungeonLandmark 北侧大本营}（中心 z=-32，保护盒 52×38×18）。
 * 场地之外是一望无际的噪声地形 —— 玩家说「进去以后除了竞技场什么都没有」。</p>
 *
 * <h2>本类负责什么</h2>
 * <p>在<b>半径 {@value #RING_INNER}~{@value #RING_OUTER} 的环带</b>里，按副本题材
 * 现场生成 4 套可以「一眼读出来」的地貌场景：</p>
 *
 * <table border="1">
 *   <caption>副本 → 场景</caption>
 *   <tr><th>副本</th><th>外围场景</th></tr>
 *   <tr><td>纪元</td><td>灵田梯田 ×2、灵泉、药圃、残碑、小观星台残台</td></tr>
 *   <tr><td>妖兽</td><td>巨骨拱 ×4、兽巢 ×6、图腾 ×3、泥沼塘 ×2、枯树乱石</td></tr>
 *   <tr><td>魔道</td><td>焦土裂缝 ×3、黑石尖峰群 ×2、血池 ×2、枯井、锁链/灵魂灯笼</td></tr>
 *   <tr><td>虚空</td><td>浮空岛 ×8、方尖碑 ×4、裂隙痕迹 ×3、紫水晶簇 ×6</td></tr>
 * </table>
 *
 * <h2>坐标纪律（硬约束）</h2>
 * <ol>
 *   <li><b>不写 r&lt;{@value #RING_INNER}</b>：中央竞技场（r≤24）与它的看台/角塔全部让开，
 *       留了 20 格缓冲；</li>
 *   <li><b>不写大本营禁区</b>：{@code |x|≤30 且 z∈[-56,-8]} 是 {@link DungeonLandmark}
 *       的中式大殿 + 保护盒范围，一格都不碰；</li>
 *   <li><b>不越 r={@value #RING_OUTER}</b>：远在禁足 ±160 以内；</li>
 *   <li>所有写方块都走 {@link Site#set}，以上三条 + {@code isLoaded} 在唯一入口处兜底，
 *       越界计数在日志里打出来（正常应为 0）。</li>
 * </ol>
 *
 * <h2>鲁棒性（第二版，实测教训）</h2>
 * <p>第一版只找「干燥 + 平整」的地放场景。换一个随机世界种子、副本周边全是低地 / 海平面 / 缓丘时
 * （竞技场 floorY 从 91 掉到 63 的那种），绝大多数候选点被拒 → 整座副本外围几乎全空
 * （实测：纪元 6052 格 → 0 格、魔道 3570 → 14 格）。第二版改成：</p>
 * <ol>
 *   <li>{@link Site#findSpot} <b>四轮放宽</b>：严格 → 坡度×2/尝试×3 → 只看落点中心干燥实心 → 均布角度兜底；</li>
 *   <li>所有「柱子 / 台基 / 拱腿」类方块改走 {@link Site#groundY}：地表是水就往下摸到水底再往上填，
 *       所以落在水域 / 浅滩上也能填实，不悬空、不泡在水面上；</li>
 *   <li><b>场景清点</b>：每个场景成功/跳过都记账，跳过打 WARN（带场景名 + 最后候选坐标），
 *       收尾再打一行 {@code scenes=实际/期望} 供验收。</li>
 * </ol>
 *
 * <h2>幂等 + 版本</h2>
 * <p>用维度级 {@link RealmThemeFlag SavedData}（数据键 {@value #DATA_KEY}）记
 * {@code built + version}。改了生成逻辑必须把 {@link #VERSION} +1，否则老存档
 * 永远看不到新场景（这个坑 {@code ArenaRealm} 里踩过两次）。</p>
 *
 * <h2>性能</h2>
 * <ul>
 *   <li>进场时先把 r≤{@value #LOAD_CHUNK_R} 个区块（{@code 17×17}）预载成
 *       {@link ChunkStatus#FULL}，否则 {@code getHeight} 会把地表算成世界底部，
 *       整片场景会埋进地下；预载耗时会写进日志。</li>
 *   <li>单维度 {@code setBlock} 硬上限 {@value #MAX_SETBLOCKS}（实际四套场景各几千格）；</li>
 *   <li>固定 {@code RandomSource.create(SEED + n)}，同一存档重复生成结果一致；</li>
 *   <li>整体 {@code try/catch(Throwable)}：只 {@code warn}，绝不让进副本流程崩。</li>
 * </ul>
 */
public final class RealmTheme {

    /**
     * 生成器版本。
     *
     * <p>⚠ 每次改场景的形状 / 材料 / 位置 / 数量，<b>必须把这个数 +1</b>，
     * 否则存档里的「已生成」标记会让新代码永远不重建 —— 玩家看到的还是旧地貌。</p>
     *
     * <p>v2：落位鲁棒性改造（四轮放宽 + 水域 fillDown + 场景清点日志），
     * 老存档靠版本号自动重建。</p>
     */
    public static final int VERSION = 2;

    /** SavedData 数据键（每个维度各自一份）。 */
    private static final String DATA_KEY = "hongmeng_realm_theme";

    /** 固定随机种子：同一份存档每次生成的地貌完全一致。 */
    private static final long SEED = 20261101L;

    /** 场景内半径：中央场地 r=24 + 大本营外扩，再留 20 格缓冲。 */
    public static final int RING_INNER = 44;

    /** 场景外半径：远在禁足 ±160 以内。 */
    public static final int RING_OUTER = 112;

    /** 预载区块半径（区块数）：17×17 = 289 个区块，覆盖 r≤128。 */
    private static final int LOAD_CHUNK_R = 8;

    /** 单维度 setBlock 硬上限（施工约定 §1.7）。 */
    private static final int MAX_SETBLOCKS = 400_000;

    /** 落在水域上时，水面向下最多摸多少格找水底（找不到就退回水面当落脚面）。 */
    private static final int FILLDOWN_MAX = 40;

    /** 大本营禁区：|x|≤30 且 z∈[-56,-8]（保护盒 52×38 外再放 5 格余量）。 */
    private static final int LANDMARK_X = 30;
    private static final int LANDMARK_Z0 = -56;
    private static final int LANDMARK_Z1 = -8;

    /** 「这一列没加载 / 不可用」的哨兵值。 */
    private static final int NONE = Integer.MIN_VALUE;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** 本次服务端会话里已建好的维度（内存缓存，省掉反复读 SavedData）。 */
    private static final Set<Dungeon> BUILT = EnumSet.noneOf(Dungeon.class);

    /** 建失败的：本次会话不再重试，免得每 tick / 每次进门都抛一次。 */
    private static final Set<Dungeon> FAILED = EnumSet.noneOf(Dungeon.class);

    // PERF(A): 进场/调试命令不再同步生成 289 个区块 + 上万格方块，改为跨 tick 分片任务。
    /** 正在分片推进的任务（每个副本维度一份）。 */
    private static final Map<Dungeon, BuildTask> TASKS = new EnumMap<>(Dungeon.class);

    /** 任务阶段：0 = 异步预载区块，1 = 铺主题场景。 */
    private static final int PHASE_PRELOAD = 0;
    private static final int PHASE_BUILD = 1;

    /** 一个副本维度的外围地貌分片任务：异步预载 289 区块 → 等战场结算 → 铺场景 → 标 built。 */
    private static final class BuildTask {

        final ServerLevel level;
        final Dungeon dungeon;
        /** 17×17 = 289 个区块（与老实现的 {@link #LOAD_CHUNK_R} 口径一致）。 */
        final TickWork.ChunkPreload preload = new TickWork.ChunkPreload(
                -LOAD_CHUNK_R, -LOAD_CHUNK_R, LOAD_CHUNK_R, LOAD_CHUNK_R);
        final long startedNanos = System.nanoTime();
        long lastNanos;
        long maxTickGapMs;
        long preloadMs;
        long buildMs;
        int ticks;
        int waitTicks;
        int phase = PHASE_PRELOAD;
        /** 收尾日志口径（与升级前一致）。 */
        int placed;

        BuildTask(ServerLevel level, Dungeon dungeon) {
            this.level = level;
            this.dungeon = dungeon;
        }
    }

    private RealmTheme() {
    }

    // ------------------------------------------------------------------
    // 对外入口
    // ------------------------------------------------------------------

    /**
     * 确保这个副本维度的外围主题地貌已经生成（幂等）。
     *
     * <p><b>PERF(A)：不再同步施工。</b>老实现会在调用线程里一口气
     * {@code getChunk(FULL,true)} 289 个区块（实测 7150ms）再铺几千格方块，
     * 进场/调试命令直接把服务端卡 7~8 秒。现在这里只「入队」，
     * 真正的推进交给 {@link #advanceTask}（由 {@link DungeonRealmEvents#onLevelTick}
     * 每 tick 限时驱动）：先把 289 个区块用 region ticket 异步生成，全部就绪后
     * 再一次性铺场景（场景写字只有 10~30ms，远低于单 tick 门槛），最后才
     * {@link #markBuilt}。中途退出/关服不会写标记，下次进来幂等重来。</p>
     *
     * @param level   副本维度（{@link DungeonRealm#level} 拿到的那个）
     * @param dungeon 哪座副本
     * @param floorY  竞技场地面高度（只用于日志；地貌按各自的原生地表面高度落位）
     */
    public static void ensure(ServerLevel level, Dungeon dungeon, int floorY) {
        if (level == null || dungeon == null) {
            return;
        }
        if (FAILED.contains(dungeon)) {
            return;
        }
        if (BUILT.contains(dungeon) || isBuilt(level)) {
            BUILT.add(dungeon);
            return;
        }
        // PERF(A): 老代码的顺序是「战场/大本营建完 → 再铺外围地貌」（地貌靠 getHeight 落位，
        // 顺序变了地形取样会变）。这里只确保战场任务已入队，本任务预载完成后会等它结算再动工。
        DungeonArena.requestBuild(level, dungeon);
        if (TASKS.containsKey(dungeon)) {
            return;                     // 已在推进：幂等，不重复入队
        }
        BuildTask task = new BuildTask(level, dungeon);
        TASKS.put(dungeon, task);
        HongmengMod.LOGGER.info(
                "[realm] {} 外围地貌任务已入队：异步预载 {} 个区块（ticket 限流），"
                        + "就绪后分片铺场景（预载完成前不阻塞服务端）",
                dungeon.dimensionId(), task.preload.total());
    }

    /**
     * 每 tick 推进一次分片任务（由 {@link DungeonRealmEvents#onLevelTick} 统一驱动）。
     *
     * @param level         副本维度
     * @param dungeon       哪座副本
     * @param deadlineNanos 本 tick 共享预算的截止 nanoTime（{@link TickWork#BUDGET_NANOS}）
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
            // ---------------- ① 异步预载 289 个区块（ticket + 轮询，绝不阻塞） ----------------
            if (task.phase == PHASE_PRELOAD) {
                if (!task.preload.advance(level)) {
                    if (task.ticks % 40 == 0) {     // 约每 2 秒一行进度（headless 验收可抓）
                        HongmengMod.LOGGER.info(
                                "[realm] {} 外围地貌预载中：{} 区块（{} tick，最慢相邻 tick {} ms）",
                                dungeon.dimensionId(), task.preload.progress(),
                                task.ticks, task.maxTickGapMs);
                    }
                    return false;
                }
                task.preloadMs = TickWork.ms(System.nanoTime() - task.startedNanos);
                HongmengMod.LOGGER.info(
                        "[realm] {} 外围地貌预载完成：{} 个区块 / {} tick / {} ms（异步 ticket）",
                        dungeon.dimensionId(), task.preload.total(), task.ticks, task.preloadMs);
                task.phase = PHASE_BUILD;
            }
            // ---------------- ② 等战场 + 大本营先结算（保持老代码的地形生成顺序） ----------------
            if (!DungeonArena.isSettled(dungeon)) {
                task.waitTicks++;
                if (task.waitTicks % 100 == 0) {
                    HongmengMod.LOGGER.debug(
                            "[realm] {} 外围地貌暂缓：等待战场/大本营分片任务结算（{} tick）",
                            dungeon.dimensionId(), task.waitTicks);
                }
                return false;
            }
            // ---------------- ③ 铺主题场景（单 tick，写字量几千格 / 10~30ms） ----------------
            long buildStart = System.nanoTime();
            // 此时 289 区块全部 FULL 就绪，floorY 缓存必然有效（不再触发同步探测）。
            int fy = DungeonArena.floorY(level, dungeon);
            RandomSource rnd = RandomSource.create(SEED + dungeon.ordinal());
            Site site = new Site(level, dungeon);
            switch (dungeon) {
                case EPOCH -> epoch(site, rnd);
                case BEAST -> beast(site, rnd);
                case DEMON -> demon(site, rnd);
                case VOID -> voidRealm(site, rnd);
            }
            task.buildMs = TickWork.ms(System.nanoTime() - buildStart);
            task.placed = site.placed;
            markBuilt(level);
            BUILT.add(dungeon);
            task.preload.release(level);
            TASKS.remove(dungeon);
            long totalMs = TickWork.ms(System.nanoTime() - task.startedNanos);
            // 与升级前日志同口径（便于验收前后对比）：格数 / 环带 / floorY / 预载 / 各拦截计数
            HongmengMod.LOGGER.info(
                    "[realm] {} 外围地貌完成：{} 格 / {} ms（环带 r={}~{}，竞技场 floorY={}；"
                            + "预载 {} 区块 {} ms；未加载跳过 {}；环外拦截 {}；大本营禁区拦截 {}；预算截断 {}）",
                    dungeon.dimensionId(), site.placed, totalMs, RING_INNER, RING_OUTER, fy,
                    task.preload.total(), task.preloadMs, site.skippedUnloaded, site.rejectedRing,
                    site.rejectedLandmark, site.limitHits);
            HongmengMod.LOGGER.info("[realm] {} 场景清点：scenes={}",
                    dungeon.dimensionId(), site.sceneSummary());
            // PERF(A): 分片任务完成日志（可观测口径：tick 数 / 最慢相邻 tick / 预载 / 施工 / 总耗时）
            HongmengMod.LOGGER.info(
                    "[realm] {} 外围地貌分片任务完成：tick={} 最慢相邻tick={}ms 预载={}ms "
                            + "施工={}ms（单 tick） 总={}ms",
                    dungeon.dimensionId(), task.ticks, task.maxTickGapMs,
                    task.preloadMs, task.buildMs, totalMs);
            return true;
        } catch (Throwable t) {
            FAILED.add(dungeon);
            TASKS.remove(dungeon);
            task.preload.release(level);
            HongmengMod.LOGGER.warn("[realm] {} 外围地貌生成失败（不影响副本可玩，本次会话不再重试）",
                    dungeon.dimensionId(), t);
            return false;
        }
    }

    /** 这个维度的外围地貌是不是已经生成过（调试 / 校验用）。 */
    public static boolean isBuilt(ServerLevel level) {
        try {
            RealmThemeFlag flag = level.getDataStorage()
                    .computeIfAbsent(RealmThemeFlag::load, RealmThemeFlag::new, DATA_KEY);
            return flag.built && flag.version == VERSION;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 写「已生成 + 版本号」标记。 */
    private static void markBuilt(ServerLevel level) {
        try {
            RealmThemeFlag flag = level.getDataStorage()
                    .computeIfAbsent(RealmThemeFlag::load, RealmThemeFlag::new, DATA_KEY);
            flag.built = true;
            flag.version = VERSION;
            flag.setDirty();
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[realm] 生成标记写入失败：{}", t.toString());
        }
    }

    // PERF(A): 旧的同步预载方法 preload(level) 已删除 —— 它逐个 getChunk(FULL, true)
    // 同步生成 289 个区块（实测 7150ms，Can't keep up 148 ticks）。
    // 现在由 BuildTask#preload（TickWork.ChunkPreload，region ticket 异步生成）代替。

    /** 维度级 SavedData：只存「建过没有 + 版本号」。 */
    public static final class RealmThemeFlag extends net.minecraft.world.level.saveddata.SavedData {
        boolean built;
        int version;

        static RealmThemeFlag load(CompoundTag tag) {
            RealmThemeFlag flag = new RealmThemeFlag();
            flag.built = tag.getBoolean("built");
            flag.version = tag.getInt("version");
            return flag;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("built", built);
            tag.putInt("version", version);
            return tag;
        }
    }

    // ------------------------------------------------------------------
    // 建造上下文：唯一的写方块入口（坐标纪律 + 计数都在这里）
    // ------------------------------------------------------------------

    /**
     * 一次建造的上下文。
     *
     * <p>所有场景方法都只通过 {@link #set} 写字 —— 半径环带、大本营禁区、
     * 加载状态、总预算四条约束在这里统一兜底，任何一条不满足就<b>不写</b>并计数。</p>
     */
    private static final class Site {

        final ServerLevel level;
        final Dungeon dungeon;
        /** 实际写入的方块数。 */
        int placed;
        /** 区块没加载而跳过的格子数。 */
        int skippedUnloaded;
        /** 落在 r&lt;44 或 r&gt;112 环外的拦截数（正常应 0）。 */
        int rejectedRing;
        /** 落在北侧大本营禁区的拦截数（正常应 0）。 */
        int rejectedLandmark;
        /** 撞到总预算被截断的次数。 */
        int limitHits;
        /** 已经占用的场地（x, z, 半径），避免两套场景叠在一起。 */
        private final List<int[]> occupied = new ArrayList<>();
        /** 场景清点：kind → {成功数, 期望数}。 */
        private final Map<String, int[]> scenes = new LinkedHashMap<>();
        /** 当前场景名（findSpot 放宽时打日志用）。 */
        String sceneName = "";
        /** 最后一次候选落点（场景被跳过时打日志用）。 */
        int lastX;
        int lastZ;

        Site(ServerLevel level, Dungeon dungeon) {
            this.level = level;
            this.dungeon = dungeon;
        }

        // ---------------- 场景清点 ----------------

        /** 登记某类场景的期望数量（收尾日志 scenes=实际/期望）。 */
        void expect(String kind, int expected) {
            scenes.put(kind, new int[]{0, expected});
        }

        /** 记一次成功。 */
        void made(String kind) {
            int[] c = scenes.get(kind);
            if (c != null) {
                c[0]++;
            }
        }

        /** 收尾清点串，例：{@code terrace=2/2 spring=1/1}。 */
        String sceneSummary() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, int[]> e : scenes.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(e.getKey()).append('=').append(e.getValue()[0]).append('/').append(e.getValue()[1]);
            }
            return sb.length() == 0 ? "(none)" : sb.toString();
        }

        // ---------------- 写方块 ----------------

        /** 唯一写方块入口。 */
        void set(int x, int y, int z, BlockState state) {
            if (placed >= MAX_SETBLOCKS) {
                limitHits++;
                return;
            }
            double r = Math.sqrt((double) x * (double) x + (double) z * (double) z);
            if (r < RING_INNER || r > RING_OUTER) {
                rejectedRing++;
                return;
            }
            if (inLandmark(x, z)) {
                rejectedLandmark++;
                return;
            }
            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1) {
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isLoaded(pos)) {
                skippedUnloaded++;
                return;
            }
            if (level.getBlockState(pos) == state) {
                return;      // 已经是目标方块：不重复写、也不用刷客户端
            }
            level.setBlock(pos, state, 2);
            placed++;
        }

        // ---------------- 地形查询 ----------------

        /** 这一列地表最高实心方块的 y；未加载返回 {@link #NONE}。 */
        int topSolid(int x, int z) {
            if (!level.isLoaded(new BlockPos(x, 0, z))) {
                return NONE;
            }
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (h <= level.getMinBuildHeight() + 2) {
                return NONE;
            }
            return h - 1;
        }

        /** 这一列在 ts 处是不是「干 + 实心」（水池 / 岩浆 / 树叶上都不要盖东西）。 */
        boolean drySolid(int x, int z, int ts) {
            BlockPos pos = new BlockPos(x, ts, z);
            if (!level.isLoaded(pos)) {
                return false;
            }
            if (!level.getFluidState(pos).isEmpty()) {
                return false;
            }
            return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        }

        /**
         * 这一列「真正可以往上填」的落脚面 y。
         *
         * <p>干地表就用原地表；<b>水面 / 岩浆面就往下摸到水底</b>（最多 {@value #FILLDOWN_MAX} 格），
         * 这样场景落在浅滩 / 湖面 / 海面上时会从水底一路填实到目标高度，
         * 而不是悬在水面上 —— 这就是「兜底落位」能建出东西的关键。</p>
         *
         * @return 落脚面的 y；没法定（未加载 / 摸不到实心）返回 {@link #NONE}
         */
        int groundY(int x, int z) {
            int ts = topSolid(x, z);
            if (ts == NONE) {
                return NONE;
            }
            BlockPos surf = new BlockPos(x, ts, z);
            if (!level.isLoaded(surf)) {
                return NONE;
            }
            if (level.getFluidState(surf).isEmpty()) {
                return ts;
            }
            for (int y = ts - 1, n = 0; y > level.getMinBuildHeight() + 2 && n < FILLDOWN_MAX; y--, n++) {
                BlockPos q = new BlockPos(x, y, z);
                if (!level.isLoaded(q)) {
                    break;
                }
                BlockState st = level.getBlockState(q);
                if (!st.isAir() && st.getFluidState().isEmpty()
                        && !st.getCollisionShape(level, q).isEmpty()) {
                    return y;
                }
            }
            return ts;      // 摸不到底：退回水面（最坏也只是浅水上的平台，不会崩）
        }

        // ---------------- 场地选择 ----------------

        /** 附近有没有已占用的场景。 */
        boolean free(int x, int z, int radius) {
            for (int[] other : occupied) {
                double dx = x - other[0];
                double dz = z - other[1];
                double need = radius + other[2];
                if (dx * dx + dz * dz < need * need) {
                    return false;
                }
            }
            return true;
        }

        /** 占一块圆形的场地。 */
        void occupy(int x, int z, int radius) {
            occupied.add(new int[]{x, z, radius});
        }

        /**
         * 找一块能放场景的场地（<b>鲁棒性第二版：四轮放宽</b>）。
         *
         * <p>第一版只找「干燥 + 平整」的地，遇到低地 / 海平面 / 缓丘的世界种子会大量落空，
         * 整座副本外围可能全空（实测：纪元 6052 格 → 0 格、魔道 3570 → 14 格）。现在逐轮放宽：</p>
         * <ol>
         *   <li><b>第 1 轮（严格）</b>：中心 + 四方向共 5 点全干、高差 ≤ {@code maxSlope}；</li>
         *   <li><b>第 2 轮</b>：高差上限 ×2、尝试次数 ×3；</li>
         *   <li><b>第 3 轮</b>：不看坡度，只要求落点中心脚下是「干燥实心地面」，尝试次数 ×6；</li>
         *   <li><b>第 4 轮（兜底）</b>：环带内「3 圈半径 × 24 个均布角度」直接取点，不筛坡、不管干湿，
         *       场景自身用 {@link #groundY} 从原地表（水面则从水底）填实。</li>
         * </ol>
         * <p>第 2/3/4 轮会打 WARN（带场景名 + 坐标），方便按日志反推哪些场景是靠放宽才落下的。</p>
         *
         * @param halfExtent 场景半宽（切比雪夫），用于算环带余量与场地保持距离
         * @param maxSlope   中心 + 四个方向采样点的最大高差；太陡的地方不放平整场景
         * @return {@code {x, z, 基准y}}；连兜底都找不到返回 {@code null}
         */
        int[] findSpot(int halfExtent, int maxSlope, RandomSource rnd) {
            // 第 1 轮：严格
            int[] spot = trySpot(halfExtent, maxSlope, 64, rnd);
            if (spot != null) {
                return spot;
            }
            // 第 2 轮：坡度放宽一倍、尝试 ×3
            spot = trySpot(halfExtent, maxSlope * 2 + 4, 192, rnd);
            if (spot != null) {
                warnRelaxed(2, spot);
                return spot;
            }
            // 第 3 轮：只看中心脚下是不是干燥实心地面、尝试 ×6
            spot = trySpotCenter(halfExtent, 384, rnd);
            if (spot != null) {
                warnRelaxed(3, spot);
                return spot;
            }
            // 第 4 轮：均布角度兜底，不筛坡
            spot = fallbackSpot(halfExtent, rnd);
            if (spot != null) {
                warnRelaxed(4, spot);
            }
            return spot;
        }

        /** 第 1/2 轮：随机撒点 + 5 点采样（干 + 高差）。 */
        private int[] trySpot(int halfExtent, int maxSlope, int attempts, RandomSource rnd) {
            // 场景最远的方块大致在 1.5×halfExtent 之内，环带留这个余量就够；
            // 真正的越界还有 Site#set 兜底（越界会计数，正常应为 0）。
            int pad = (int) Math.ceil(halfExtent * 1.5D) + 2;
            int lo = RING_INNER + pad;
            int hi = RING_OUTER - pad;
            if (hi - lo < 8) {
                lo = RING_INNER + pad / 2;
                hi = RING_OUTER - pad / 2;
            }
            if (hi - lo < 4) {
                return null;
            }
            int reserve = Math.max(6, pad);   // 场地互不重叠的保持距离
            for (int i = 0; i < attempts; i++) {
                double angle = rnd.nextDouble() * Math.PI * 2.0D;
                int r = lo + rnd.nextInt(hi - lo + 1);
                int x = (int) Math.round(Math.cos(angle) * r);
                int z = (int) Math.round(Math.sin(angle) * r);
                lastX = x;
                lastZ = z;
                if (inLandmark(x, z) || nearLandmark(x, z, pad)) {
                    continue;
                }
                if (!free(x, z, reserve)) {
                    continue;
                }
                int base = sampleBase(x, z, halfExtent, maxSlope);
                if (base == NONE) {
                    continue;
                }
                occupy(x, z, reserve);
                return new int[]{x, z, base};
            }
            return null;
        }

        /** 第 3 轮：不看坡度，只要求落点中心脚下有干燥实心地面。 */
        private int[] trySpotCenter(int halfExtent, int attempts, RandomSource rnd) {
            int pad = (int) Math.ceil(halfExtent * 1.5D) + 2;
            int lo = RING_INNER + pad;
            int hi = RING_OUTER - pad;
            if (hi - lo < 2) {
                return null;
            }
            int reserve = Math.max(6, pad);
            for (int i = 0; i < attempts; i++) {
                double angle = rnd.nextDouble() * Math.PI * 2.0D;
                int r = lo + rnd.nextInt(hi - lo + 1);
                int x = (int) Math.round(Math.cos(angle) * r);
                int z = (int) Math.round(Math.sin(angle) * r);
                lastX = x;
                lastZ = z;
                if (inLandmark(x, z) || nearLandmark(x, z, pad)) {
                    continue;
                }
                if (!free(x, z, reserve)) {
                    continue;
                }
                int ts = topSolid(x, z);
                if (ts == NONE || !drySolid(x, z, ts)) {
                    continue;
                }
                occupy(x, z, reserve);
                return new int[]{x, z, ts + 1};
            }
            return null;
        }

        /**
         * 第 4 轮兜底：环带内按「3 个半径 × 24 个均布角度」扫一圈。
         * 不筛坡、不管干湿，只要地形加载了就行 —— 剩下的交给场景自己的
         * {@link #groundY}（水底填实）/ {@link #column}（整平 + 填实）。
         * 实在全被占满时，退而求其次用「有地形、不压大本营」的候选（允许轻微重叠），
         * 保证场景一定能落下。
         */
        private int[] fallbackSpot(int halfExtent, RandomSource rnd) {
            int pad = (int) Math.ceil(halfExtent * 1.5D) + 2;
            int lo = RING_INNER + pad;
            int hi = RING_OUTER - pad;
            if (hi - lo < 2) {
                return null;
            }
            int reserve = Math.max(6, pad);
            int start = rnd.nextInt(24) * 15;
            int[] best = null;
            for (int ring = 0; ring < 3; ring++) {
                int radius = lo + (hi - lo) * ring / 2;      // 内、中、外三圈
                for (int i = 0; i < 24; i++) {
                    double angle = Math.toRadians((start + i * 15) % 360);
                    int x = (int) Math.round(Math.cos(angle) * radius);
                    int z = (int) Math.round(Math.sin(angle) * radius);
                    lastX = x;
                    lastZ = z;
                    if (inLandmark(x, z) || nearLandmark(x, z, pad)) {
                        continue;
                    }
                    int ts = topSolid(x, z);
                    if (ts == NONE) {
                        continue;
                    }
                    if (free(x, z, reserve)) {
                        occupy(x, z, reserve);
                        return new int[]{x, z, ts + 1};
                    }
                    if (best == null) {
                        best = new int[]{x, z, ts + 1};      // 备选：有地形但挤
                    }
                }
            }
            if (best != null) {
                occupy(best[0], best[1], reserve);
            }
            return best;
        }

        /** 放宽到第 2/3/4 轮时打 WARN（带场景名 + 坐标），便于按日志反推。 */
        private void warnRelaxed(int round, int[] spot) {
            HongmengMod.LOGGER.warn("[realm] {} 场景「{}」落点放宽到第 {} 轮：({}, {})",
                    dungeon.dimensionId(), sceneName, round, spot[0], spot[1]);
        }

        /** 中心 + 四个方向采样：要求全干、全实心，高差不超限；返回「最高点 + 1」。 */
        private int sampleBase(int x, int z, int half, int maxSlope) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int i = 0; i < 5; i++) {
                int ox = switch (i) {
                    case 1 -> half;
                    case 2 -> -half;
                    default -> 0;
                };
                int oz = switch (i) {
                    case 3 -> half;
                    case 4 -> -half;
                    default -> 0;
                };
                int ts = topSolid(x + ox, z + oz);
                if (ts == NONE || !drySolid(x + ox, z + oz, ts)) {
                    return NONE;
                }
                if (ts < min) {
                    min = ts;
                }
                if (ts > max) {
                    max = ts;
                }
            }
            if (max - min > maxSlope) {
                return NONE;
            }
            return max + 1;
        }

        /**
         * 随机撒一个小物件（枯树 / 乱石 / 灯柱）。
         *
         * @return {@code {x, z, 地表y}}，找不着返回 {@code null}
         */
        int[] scatter(RandomSource rnd, int pad) {
            int lo = RING_INNER + pad + 4;
            int hi = RING_OUTER - pad - 4;
            if (hi <= lo) {
                return null;
            }
            for (int i = 0; i < 48; i++) {
                double angle = rnd.nextDouble() * Math.PI * 2.0D;
                int r = lo + rnd.nextInt(hi - lo + 1);
                int x = (int) Math.round(Math.cos(angle) * r);
                int z = (int) Math.round(Math.sin(angle) * r);
                if (nearLandmark(x, z, pad + 2) || !free(x, z, pad + 2)) {
                    continue;
                }
                int ts = topSolid(x, z);
                if (ts == NONE || !drySolid(x, z, ts)) {
                    continue;
                }
                occupy(x, z, pad + 2);
                return new int[]{x, z, ts};
            }
            return null;
        }
    }

    /** 大本营禁区判定（与 {@link #inLandmark} 同一口径）。 */
    private static boolean nearLandmark(int x, int z, int pad) {
        return Math.abs(x) <= LANDMARK_X + pad
                && z >= LANDMARK_Z0 - pad && z <= LANDMARK_Z1 + pad;
    }

    /** 大本营禁区：{@code |x|≤30 且 z∈[-56,-8]}。 */
    private static boolean inLandmark(int x, int z) {
        return Math.abs(x) <= LANDMARK_X && z >= LANDMARK_Z0 && z <= LANDMARK_Z1;
    }

    /** 装饰类小景单独兜底：失败只 warn，其它场景继续。 */
    private static void safe(String name, Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[realm] 场景「{}」生成失败（跳过，不影响副本）：{}",
                    name, t.toString());
        }
    }

    /**
     * 跑一套「算数」的场景：捕获异常、成功记入清点、失败打 WARN（带场景名 + 最后候选坐标）。
     *
     * <p>验收日志里的 {@code scenes=实际/期望} 就是这里记的账。</p>
     */
    private static boolean scene(Site s, String name, String kind, BooleanSupplier body) {
        s.sceneName = name;
        boolean ok;
        try {
            ok = body.getAsBoolean();
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[realm] 场景「{}」生成失败（{}，最后候选坐标={},{}）：{}",
                    name, s.dungeon.dimensionId(), s.lastX, s.lastZ, t.toString());
            ok = false;
        }
        if (ok) {
            s.made(kind);
        } else {
            HongmengMod.LOGGER.warn("[realm] 场景跳过：{}（{}，最后候选坐标={},{}）",
                    name, s.dungeon.dimensionId(), s.lastX, s.lastZ);
        }
        return ok;
    }

    // ------------------------------------------------------------------
    // 通用几何：整平一列 / 地表替换 / 朝向 / 灵植年龄
    // ------------------------------------------------------------------

    /**
     * 把一列整平到 topY 并铺 surface（鲁棒性第二版：水域也能填实）：
     * <ul>
     *   <li>地形比 topY 高 → 把 topY 以上挖掉（梯田切坡、台面找平）；</li>
     *   <li>地形比 topY 低 → 从「落脚面」填到 topY，下面用 fill，绝不悬空；</li>
     *   <li>地表是水 → 落脚面自动下沉到水底（{@link Site#groundY}），
     *       整根水柱都被填实，不会出现「漂在水面上的台子」。</li>
     * </ul>
     */
    private static void column(Site s, int x, int z, int topY, BlockState surface, BlockState fill) {
        int surf = s.topSolid(x, z);
        if (surf == NONE) {
            return;
        }
        int from = s.groundY(x, z);
        if (from == NONE) {
            return;
        }
        if (surf > topY) {
            for (int y = topY + 1; y <= surf; y++) {
                s.set(x, y, z, AIR);
            }
        }
        if (from > topY) {
            from = topY;
        }
        for (int y = from; y < topY; y++) {
            s.set(x, y, z, fill);
        }
        s.set(x, topY, z, surface);
    }

    /** 把一列「落脚面」的方块换成 state（不填不挖，只做地表替换；水底同理）。 */
    private static void replaceSurface(Site s, int x, int z, BlockState state) {
        int y = s.groundY(x, z);
        if (y == NONE) {
            return;
        }
        s.set(x, y, z, state);
    }

    /** 取一个 0..3 的朝向，返回 {ux, uz, vx, vz}：u = 抬升/延伸方向，v = 垂直方向。 */
    private static int[] dirVec(int dir) {
        return switch (dir & 3) {
            case 0 -> new int[]{1, 0, 0, 1};
            case 1 -> new int[]{0, 1, 1, 0};
            case 2 -> new int[]{-1, 0, 0, 1};
            default -> new int[]{0, -1, 1, 0};
        };
    }

    /** 成熟的灵植（随机 age 2..7，看起来像长到了一半以上）。 */
    private static BlockState mature(BlockState herb, RandomSource rnd) {
        return herb.setValue(BlockStateProperties.AGE_7, 2 + rnd.nextInt(6));
    }

    /** 横置的骨块（做脊椎 / 倒地残柱用）。 */
    private static BlockState boneAlong(boolean alongX) {
        return Blocks.BONE_BLOCK.defaultBlockState()
                .setValue(BlockStateProperties.AXIS,
                        alongX ? Direction.Axis.X : Direction.Axis.Z);
    }

    // ==================================================================
    // 纪元秘境：灵田梯田 / 灵泉 / 药圃 / 残碑 / 小观星台
    // ==================================================================

    private static void epoch(Site s, RandomSource rnd) {
        s.expect("terrace", 2);
        s.expect("spring", 1);
        s.expect("garden", 1);
        s.expect("steles", 1);
        s.expect("observatory", 1);
        scene(s, "纪元·灵田梯田（甲）", "terrace", () -> terrace(s, rnd));
        scene(s, "纪元·灵田梯田（乙）", "terrace", () -> terrace(s, rnd));
        scene(s, "纪元·灵泉", "spring", () -> spring(s, rnd));
        scene(s, "纪元·药圃", "garden", () -> herbGarden(s, rnd));
        scene(s, "纪元·残碑", "steles", () -> brokenSteles(s, rnd));
        scene(s, "纪元·小观星台", "observatory", () -> observatory(s, rnd));
    }

    /**
     * <b>灵田梯田</b>：3~4 级、每级 5 格宽的等高条田。
     *
     * <p>做法：沿 u 方向逐级抬 1 格，每级 5 格宽、17 格长；下面从原地表填实，
     * 所以不管是平地还是缓坡都不会悬空。田面用灵田土（灵植生长 ×3），
     * 每三行夹一条耕地并种上雪莲草。</p>
     */
    private static boolean terrace(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(11, 8, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        int levels = 3 + rnd.nextInt(2);          // 3~4 级
        int bandW = 5;
        int halfLen = 8;                          // 17 格长
        int[] dir = dirVec(rnd.nextInt(4));
        int ux = dir[0];
        int uz = dir[1];
        int vx = dir[2];
        int vz = dir[3];
        BlockState soil = ModBlocks.LINGTIAN_SOIL.get().defaultBlockState();
        BlockState farm = Blocks.FARMLAND.defaultBlockState();
        BlockState ridge = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState herb = ModBlocks.XUELIAN_GRASS.get().defaultBlockState();
        int start = -(levels * bandW) / 2;
        for (int level = 0; level < levels; level++) {
            int topY = base + level;
            for (int a = 0; a < bandW; a++) {
                int u = start + level * bandW + a;
                boolean edge = a == 0;            // 每级外侧一格做田埂
                for (int b = -halfLen; b <= halfLen; b++) {
                    int x = cx + ux * u + vx * b;
                    int z = cz + uz * u + vz * b;
                    column(s, x, z, topY, edge ? ridge : soil, dirt);
                    if (edge) {
                        continue;
                    }
                    boolean useFarm = ((b + level) % 3 == 0);
                    if (useFarm) {
                        s.set(x, topY, z, farm);
                        if (rnd.nextFloat() < 0.85F) {
                            s.set(x, topY + 1, z, mature(herb, rnd));
                        }
                    } else if (rnd.nextFloat() < 0.55F) {
                        s.set(x, topY + 1, z, mature(herb, rnd));
                    }
                }
            }
        }
        return true;
    }

    /**
     * <b>灵泉</b>：半径 3 的水池，石英围边（外圈再抬一格做池沿），四角海晶灯。
     *
     * <p>水面与围边同高、外面还有一圈 +1 的池沿，所以水不会流出来
     * （即使之后有方块更新，也是被石英围死的死水）。</p>
     */
    private static boolean spring(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(6, 6, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        int r = 4;
        BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState deep = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r * r) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                if (d2 <= 9) {                    // 池内（半径 3）：深板岩池底 + 水面
                    column(s, x, z, base - 1, deep, deep);
                    s.set(x, base, z, water);
                } else {                          // 池沿：石英
                    column(s, x, z, base, quartz, quartz);
                }
            }
        }
        // 外圈再抬一格，把水彻底围住
        int outer = r + 1;
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= r * r || d2 > outer * outer) {
                    continue;
                }
                column(s, cx + dx, cz + dz, base + 1, quartz, quartz);
            }
        }
        // 池边海晶灯（四个正方向，放在抬高的池沿上）
        s.set(cx + r, base + 1, cz, lamp);
        s.set(cx - r, base + 1, cz, lamp);
        s.set(cx, base + 1, cz + r, lamp);
        s.set(cx, base + 1, cz - r, lamp);
        return true;
    }

    /**
     * <b>药圃</b>：9×7 的灵田土方阵，六阶灵植按行排开，四周橡木栅栏 + 四角石灯。
     *
     * <p>灵植是 {@code CropBlock} 那一套（有 age），这里直接放成熟的年龄，
     * 远看就是一片长起来的药田；栅栏留一格门朝竞技场方向。</p>
     */
    private static boolean herbGarden(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(6, 6, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        int halfX = 4;
        int halfZ = 3;
        BlockState soil = ModBlocks.LINGTIAN_SOIL.get().defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState[] herbs = {
                ModBlocks.XUELIAN_GRASS.get().defaultBlockState(),
                ModBlocks.HUOYAN_HUA.get().defaultBlockState(),
                ModBlocks.HANBING_TENG.get().defaultBlockState(),
                ModBlocks.LEIYIN_CAO.get().defaultBlockState(),
                ModBlocks.KONGMING_LAN.get().defaultBlockState(),
                ModBlocks.XUELUO_ZHI.get().defaultBlockState(),
        };
        // 门开在朝向竞技场（原点）的那一侧
        int doorX = 0;
        int doorZ = 0;
        if (Math.abs(cx) >= Math.abs(cz)) {
            doorX = cx > 0 ? -1 : 1;
        } else {
            doorZ = cz > 0 ? -1 : 1;
        }
        for (int dx = -halfX; dx <= halfX; dx++) {
            for (int dz = -halfZ; dz <= halfZ; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                column(s, x, z, base, soil, dirt);
                boolean border = Math.abs(dx) == halfX || Math.abs(dz) == halfZ;
                boolean corner = Math.abs(dx) == halfX && Math.abs(dz) == halfZ;
                if (corner) {
                    continue;                     // 四角留给石灯
                }
                if (border) {
                    boolean door = (dx != 0 && Integer.signum(dx) == doorX && dz == 0)
                            || (dz != 0 && Integer.signum(dz) == doorZ && dx == 0);
                    if (!door) {
                        s.set(x, base + 1, z, fence);
                    }
                    continue;
                }
                // 内部：一行一阶灵植
                int row = (dz + halfZ) % herbs.length;
                s.set(x, base + 1, z, mature(herbs[row], rnd));
            }
        }
        // 四角石灯（3 格高 + 海晶灯）
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int x = cx + sx * halfX;
                int z = cz + sz * halfZ;
                s.set(x, base + 1, z, brick);
                s.set(x, base + 2, z, brick);
                s.set(x, base + 3, z, lamp);
            }
        }
        return true;
    }

    /**
     * <b>残碑</b>：7×7 石台上一组断柱 —— 4~6 根高低不一的石英柱 + 倒地的残柱。
     *
     * <p>断口用石英台阶 / 方块收尾，做出「断掉」的参差感。</p>
     */
    private static boolean brokenSteles(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(5, 8, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState slab = Blocks.QUARTZ_SLAB.defaultBlockState();
        BlockState chisel = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        BlockState lying = pillar.setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                column(s, cx + dx, cz + dz, base, edge ? stone : quartz, stone);
            }
        }
        int count = 4 + rnd.nextInt(3);                 // 4~6 根断柱
        for (int i = 0; i < count; i++) {
            int x = cx + rnd.nextInt(5) - 2;
            int z = cz + rnd.nextInt(5) - 2;
            int h = 2 + rnd.nextInt(5);
            s.set(x, base, z, chisel);
            for (int y = 1; y <= h; y++) {
                s.set(x, base + y, z, pillar);
            }
            s.set(x, base + h + 1, z, (h % 2 == 0) ? slab : quartz);
        }
        // 两根倒地的残柱
        for (int i = 0; i < 2; i++) {
            int x0 = cx + rnd.nextInt(3) - 1;
            int z0 = cz + rnd.nextInt(5) - 2;
            int len = 2 + rnd.nextInt(2);
            for (int t = 0; t < len; t++) {
                s.set(x0 + t, base + 1, z0, lying);
            }
            s.set(x0 + len, base + 1, z0, slab);
        }
        return true;
    }

    /**
     * <b>小观星台残台</b>：两级圆台（半径 6 / 5）+ 金块日晷 + 一圈断柱。
     *
     * <p>焦点是台心的金块日晷：中心金块上立三格石英晷针，四正方向各一块金块当日晷刻度，
     * 内圈再摆 8 块石英台阶当方位标记。外圈 5 根残柱高低不一，2 根顶上挂海晶灯。</p>
     */
    private static boolean observatory(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(9, 8, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        BlockState tile = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState slab = Blocks.QUARTZ_SLAB.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        // 两级圆台
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 36) {
                    continue;
                }
                if (d2 > 25) {
                    column(s, cx + dx, cz + dz, base, tile, tile);
                } else if (d2 > 16) {
                    column(s, cx + dx, cz + dz, base + 1, tile, tile);
                } else {
                    column(s, cx + dx, cz + dz, base + 1, quartz, tile);
                }
            }
        }
        // 金块日晷：中宫 + 四正刻度
        s.set(cx, base + 2, cz, gold);
        s.set(cx + 2, base + 2, cz, gold);
        s.set(cx - 2, base + 2, cz, gold);
        s.set(cx, base + 2, cz + 2, gold);
        s.set(cx, base + 2, cz - 2, gold);
        // 晷针：三格石英柱 + 台阶顶
        for (int y = 3; y <= 5; y++) {
            s.set(cx, base + y, cz, pillar);
        }
        s.set(cx, base + 6, cz, slab);
        // 方位标记：半径 4 的 8 个石英台阶
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0D * i / 8.0D;
            int x = cx + (int) Math.round(Math.cos(a) * 4.0D);
            int z = cz + (int) Math.round(Math.sin(a) * 4.0D);
            s.set(x, base + 2, z, slab);
        }
        // 外圈断柱（5 根，其中 2 根挂灯）
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2.0D * i / 5.0D + 0.5D;
            int x = cx + (int) Math.round(Math.cos(a) * 5.0D);
            int z = cz + (int) Math.round(Math.sin(a) * 5.0D);
            int h = 2 + rnd.nextInt(4);
            for (int y = 1; y <= h; y++) {
                s.set(x, base + 1 + y, z, pillar);
            }
            if (i % 2 == 0) {
                s.set(x, base + 2 + h, z, lamp);
            }
        }
        return true;
    }

    // ==================================================================
    // 妖兽秘境：巨骨拱 / 兽巢 / 图腾 / 泥沼塘 / 枯树乱石
    // ==================================================================

    private static void beast(Site s, RandomSource rnd) {
        s.expect("arch", 4);
        s.expect("den", 6);
        s.expect("totem", 3);
        s.expect("mire", 2);
        // 巨骨拱：落位失败（极陡/异常）时换点重试，直到放满 4 座或用完 16 次
        for (int made = 0, tries = 0; made < 4 && tries < 16; tries++) {
            if (scene(s, "妖兽·巨骨拱", "arch", () -> boneArch(s, rnd))) {
                made++;
            }
        }
        for (int i = 0; i < 6; i++) {
            scene(s, "妖兽·兽巢", "den", () -> den(s, rnd));
        }
        for (int i = 0; i < 3; i++) {
            scene(s, "妖兽·图腾", "totem", () -> totem(s, rnd));
        }
        for (int i = 0; i < 2; i++) {
            scene(s, "妖兽·泥沼塘", "mire", () -> mire(s, rnd));
        }
        safe("妖兽·枯树乱石", () -> deadWoods(s, rnd));
    }

    /**
     * <b>巨骨拱</b>：跨度 14~18、高 10~12 的半圆骨拱，拱身 2 格厚，脊椎节横置骨块。
     *
     * <p>沿拱脚的每一列都从<b>原地表</b>往上填到拱线，所以拱脚一定落地、
     * 不会出现悬空的骨头；落点在水面/浅滩上时，落脚面自动下沉到水底再往上填
     * （见 {@link Site#groundY}），拱脚同样落地。拱顶再点一节「脊椎骨节」和头骨。</p>
     *
     * @return 是否成功放下一座（只有极端地形才返回 false，调用方会换点重试）
     */
    private static boolean boneArch(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(10, 999, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int span = 14 + rnd.nextInt(5);        // 14~18
        int half = span / 2;
        int height = 10 + rnd.nextInt(3);      // 10~12
        boolean alongX = rnd.nextBoolean();
        int maxG = Integer.MIN_VALUE;
        for (int t = -half; t <= half; t++) {
            for (int w = -1; w <= 0; w++) {
                int x = alongX ? cx + t : cx + w;
                int z = alongX ? cz + w : cz + t;
                int g = s.groundY(x, z);
                if (g == NONE) {
                    return false;
                }
                maxG = Math.max(maxG, g);
            }
        }
        int base = maxG + 1;
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        BlockState joint = boneAlong(alongX);
        for (int t = -half; t <= half; t++) {
            double f = (double) t / (double) half;
            double arc = Math.sqrt(Math.max(0.0D, 1.0D - f * f));
            int top = base + 2 + (int) Math.round(arc * (height - 2));   // 2 格腿 + 半圆
            for (int w = -1; w <= 0; w++) {
                int x = alongX ? cx + t : cx + w;
                int z = alongX ? cz + w : cz + t;
                int g = s.groundY(x, z);
                if (g == NONE) {
                    continue;
                }
                // 拱腿最多 26 格，避免落在陡坡/深水上拉出一根通天骨柱
                int from = Math.max(g + 1, top - 26);
                for (int y = from; y <= top; y++) {
                    boolean vertebra = (Math.abs(t) % 2 == 0) && y >= base + 3;
                    s.set(x, y, z, vertebra ? joint : bone);
                }
            }
        }
        // 拱顶的脊椎骨节 + 头骨
        int apexTop = base + height;
        s.set(cx, apexTop + 1, cz, boneAlong(!alongX));
        s.set(cx, apexTop + 2, cz, bone);
        // 沿拱背每 3 格一根骨刺
        for (int t = -half; t <= half; t += 3) {
            double f = (double) t / (double) half;
            double arc = Math.sqrt(Math.max(0.0D, 1.0D - f * f));
            int top = base + 2 + (int) Math.round(arc * (height - 2));
            int x = alongX ? cx + t : cx;
            int z = alongX ? cz : cz + t;
            s.set(x, top + 1, z, boneAlong(!alongX));
        }
        return true;
    }

    /**
     * <b>兽巢</b>：7×7 粗泥台、泥坯/粗泥围墙（朝竞技场一侧留 2 格门洞）、
     * 干草窝、骨块兽骨、菌光体、中央篝火。
     */
    private static boolean den(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(5, 10, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        BlockState dirt = Blocks.COARSE_DIRT.defaultBlockState();
        BlockState mud = Blocks.PACKED_MUD.defaultBlockState();
        BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        BlockState shroom = Blocks.SHROOMLIGHT.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                column(s, cx + dx, cz + dz, base, dirt, dirt);
            }
        }
        // 门洞朝向竞技场（原点）那一边
        int doorX = 0;
        int doorZ = 0;
        if (Math.abs(cx) >= Math.abs(cz)) {
            doorX = cx > 0 ? -1 : 1;
        } else {
            doorZ = cz > 0 ? -1 : 1;
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean border = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                if (!border) {
                    continue;
                }
                int sgnX = Integer.signum(dx);
                int sgnZ = Integer.signum(dz);
                boolean door = (sgnX != 0 && sgnX == doorX && Math.abs(dz) <= 1)
                        || (sgnZ != 0 && sgnZ == doorZ && Math.abs(dx) <= 1);
                if (door) {
                    continue;
                }
                int h = 1 + rnd.nextInt(2);
                for (int y = 1; y <= h; y++) {
                    s.set(cx + dx, base + y, cz + dz, ((dx + dz) & 1) == 0 ? mud : dirt);
                }
                if (rnd.nextInt(4) == 0) {
                    s.set(cx + dx, base + h + 1, cz + dz, bone);
                }
            }
        }
        // 四角骨柱
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = 1; y <= 3; y++) {
                    s.set(cx + sx * 3, base + y, cz + sz * 3, bone);
                }
            }
        }
        // 干草窝 + 兽骨 + 菌光体 + 篝火
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                s.set(cx - 2 + dx, base + 1, cz - 2 + dz, hay);
            }
        }
        int bx = cx + 1;
        int bz = cz + 1;
        s.set(bx, base + 1, bz, bone);
        s.set(bx, base + 2, bz, bone);
        s.set(bx + 1, base + 1, bz, bone);
        s.set(bx - 1, base + 1, bz, bone);
        s.set(cx + 2, base + 1, cz - 2, shroom);
        s.set(cx, base + 1, cz, Blocks.CAMPFIRE.defaultBlockState());
        return true;
    }

    /**
     * <b>图腾</b>：云杉去皮原木主柱（8~12 高）+ 骨块横臂 + 顶端绿宝石。
     */
    private static boolean totem(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(4, 10, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        BlockState brick = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        BlockState emerald = Blocks.EMERALD_BLOCK.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                column(s, cx + dx, cz + dz, base, brick, brick);
            }
        }
        int h = 8 + rnd.nextInt(5);            // 8~12
        for (int y = 1; y <= h; y++) {
            s.set(cx, base + y, cz, log);
        }
        // 骨块横臂（高低各一）
        int armLow = base + Math.max(3, h - 5);
        int armHigh = base + Math.max(5, h - 2);
        for (int d = -2; d <= 2; d++) {
            if (d == 0) {
                continue;
            }
            s.set(cx + d, armLow, cz, bone);
            if (Math.abs(d) == 1) {
                s.set(cx, armLow, cz + d, bone);
            }
        }
        for (int d = -1; d <= 1; d += 2) {
            s.set(cx + d, armHigh, cz, bone);
        }
        s.set(cx, base + h + 1, cz, emerald);
        s.set(cx, base + h + 2, cz, bone);
        // 头顶两只角
        s.set(cx + 1, base + h + 2, cz, bone);
        s.set(cx - 1, base + h + 2, cz, bone);
        return true;
    }

    /**
     * <b>泥沼塘</b>：半径 4 的浅水塘（泥/泥坯沿），水里撒海草，塘边倒一根原木。
     */
    private static boolean mire(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(7, 5, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        int r = 6;
        BlockState mud = Blocks.MUD.defaultBlockState();
        BlockState packed = Blocks.PACKED_MUD.defaultBlockState();
        BlockState coarse = Blocks.COARSE_DIRT.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState weed = Blocks.SEAGRASS.defaultBlockState();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r * r) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                if (d2 <= 16) {                   // 塘内（半径 4）：泥底 + 一层水
                    column(s, x, z, base - 1, mud, mud);
                    if (rnd.nextFloat() < 0.3F) {
                        s.set(x, base, z, weed);  // 水草
                    } else {
                        s.set(x, base, z, water);
                    }
                } else {                          // 塘沿：泥坯 / 粗泥
                    column(s, x, z, base, (d2 > 25) ? coarse : packed, mud);
                }
            }
        }
        // 塘边倒木
        int logLen = 4 + rnd.nextInt(3);
        int lz = cz + rnd.nextInt(7) - 3;
        int lx = cx - 6;
        BlockState log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        for (int t = 0; t < logLen; t++) {
            int x = lx + t;
            int g = s.groundY(x, lz);
            if (g == NONE) {
                continue;
            }
            s.set(x, g + 1, lz, log);
        }
        return true;
    }

    /** <b>枯树乱石</b>：10 棵枯树（歪斜裸干）+ 14 堆碎石，散布在环带里。 */
    private static void deadWoods(Site s, RandomSource rnd) {
        BlockState log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        BlockState darkLog = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState[] rubble = {
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
                Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
        };
        for (int i = 0; i < 10; i++) {
            int[] p = s.scatter(rnd, 3);
            if (p == null) {
                continue;
            }
            int x = p[0];
            int z = p[1];
            int ts = p[2];
            int h = 4 + rnd.nextInt(4);
            for (int y = 1; y <= h; y++) {
                s.set(x, ts + y, z, (y % 3 == 0) ? darkLog : log);
            }
            // 光秃秃的枝干
            for (int b = 0; b < 3; b++) {
                int bx = x + rnd.nextInt(3) - 1;
                int bz = z + rnd.nextInt(3) - 1;
                int by = ts + 2 + rnd.nextInt(Math.max(1, h - 2));
                s.set(bx, by, bz, log);
            }
            s.set(x, ts + h + 1, z, log);
        }
        for (int i = 0; i < 14; i++) {
            int[] p = s.scatter(rnd, 2);
            if (p == null) {
                continue;
            }
            int x = p[0];
            int z = p[1];
            int ts = p[2];
            BlockState st = rubble[rnd.nextInt(rubble.length)];
            int h = 1 + rnd.nextInt(3);
            for (int y = 1; y <= h; y++) {
                s.set(x, ts + y, z, st);
            }
            if (rnd.nextBoolean()) {
                s.set(x + 1, ts + 1, z, st);
                s.set(x, ts + 1, z + 1, st);
            }
        }
    }

    // ==================================================================
    // 魔道秘境：焦土裂缝 / 黑石尖峰 / 血池 / 枯井 / 锁链灯柱
    // ==================================================================

    private static void demon(Site s, RandomSource rnd) {
        s.expect("crack", 3);
        s.expect("spikes", 2);
        s.expect("bloodpool", 2);
        s.expect("well", 1);
        for (int i = 0; i < 3; i++) {
            scene(s, "魔道·焦土裂缝", "crack", () -> scorchedCrack(s, rnd));
        }
        for (int i = 0; i < 2; i++) {
            scene(s, "魔道·黑石尖峰群", "spikes", () -> blackstoneSpikes(s, rnd));
        }
        for (int i = 0; i < 2; i++) {
            scene(s, "魔道·血池", "bloodpool", () -> bloodPool(s, rnd));
        }
        scene(s, "魔道·枯井", "well", () -> dryWell(s, rnd));
        safe("魔道·锁链灯柱", () -> demonLamps(s, rnd));
    }

    /**
     * <b>焦土裂缝</b>：2 格宽、2 格深的黑石沟，底部一层岩浆，两侧黑石沟沿。
     *
     * <p>槽沿按整条裂缝的<b>最高地面 +2</b> 统一找平，沟底一律填实，
     * 所以岩浆永远被黑石包着（两侧 + 两端都有壁），不会流出来烧到路。</p>
     */
    private static boolean scorchedCrack(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(13, 14, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int len = 18 + rnd.nextInt(7);
        int half = len / 2;
        int[] dir = dirVec(rnd.nextInt(4));
        int ux = dir[0];
        int uz = dir[1];
        int vx = dir[2];
        int vz = dir[3];
        int maxG = Integer.MIN_VALUE;
        for (int t = -half; t <= half; t++) {
            for (int a = -1; a <= 2; a++) {
                int x = cx + ux * t + vx * a;
                int z = cz + uz * t + vz * a;
                int g = s.groundY(x, z);       // 水面上自动下沉到水底，沟壁能填实
                if (g == NONE) {
                    return false;
                }
                maxG = Math.max(maxG, g);
            }
        }
        int base = maxG + 2;                   // 槽沿（比最高落脚面高 1）
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.BLACKSTONE.defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockState rim = Blocks.MAGMA_BLOCK.defaultBlockState();
        for (int t = -half; t <= half; t++) {
            for (int a = -1; a <= 2; a++) {
                int x = cx + ux * t + vx * a;
                int z = cz + uz * t + vz * a;
                int g = s.groundY(x, z);
                if (g == NONE) {
                    continue;
                }
                if (a == -1 || a == 2) {
                    // 沟壁：从落脚面（水面时=水底）砌到槽沿，顶上偶尔压一块岩浆块当余烬
                    for (int y = g; y <= base; y++) {
                        s.set(x, y, z, wall);
                    }
                    if (t % 3 == 0) {
                        s.set(x, base + 1, z, rim);
                    }
                } else {
                    // 沟内 2 格宽：槽底 -> 1 层岩浆 -> 沟口
                    if (g < base - 3) {
                        for (int y = g; y < base - 3; y++) {
                            s.set(x, y, z, floor);
                        }
                    }
                    s.set(x, base - 3, z, floor);
                    s.set(x, base - 2, z, lava);
                    int surf = s.topSolid(x, z);
                    if (surf > base - 1) {
                        // 落点在水面以下：把沟口封死，不挖穿水柱（避免水与岩浆相邻）
                        s.set(x, base - 1, z, floor);
                    } else {
                        s.set(x, base - 1, z, AIR);
                    }
                }
            }
            // 裂缝两侧焦土边缘
            if (t % 2 == 0) {
                for (int a = -2; a <= 3; a += 5) {
                    int x = cx + ux * t + vx * a;
                    int z = cz + uz * t + vz * a;
                    replaceSurface(s, x, z, (rnd.nextBoolean() ? floor : wall));
                }
            }
        }
        // 两端封口（防止岩浆从端头露出）
        for (int end = -1; end <= 1; end += 2) {
            int t = end * (half + 1);
            for (int a = -1; a <= 2; a++) {
                int x = cx + ux * t + vx * a;
                int z = cz + uz * t + vz * a;
                int g = s.groundY(x, z);
                if (g == NONE) {
                    continue;
                }
                for (int y = g; y <= base; y++) {
                    s.set(x, y, z, wall);
                }
            }
        }
        return true;
    }

    /**
     * <b>黑石尖峰群</b>：每组 8~14 根高 10~24 的黑石尖柱（粗基细顶）。
     *
     * <p>每根都从自己的原地表长起来，所以整组看起来像一片突兀的黑色石林；
     * 中间夹金镶黑石 / 玄武岩做纹理。</p>
     */
    private static boolean blackstoneSpikes(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(10, 14, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        BlockState rock = Blocks.BLACKSTONE.defaultBlockState();
        BlockState smooth = Blocks.SMOOTH_BASALT.defaultBlockState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState basalt = Blocks.BASALT.defaultBlockState();
        int count = 8 + rnd.nextInt(7);        // 8~14 根
        for (int i = 0; i < count; i++) {
            int ox = rnd.nextInt(21) - 10;
            int oz = rnd.nextInt(21) - 10;
            if (ox * ox + oz * oz > 100) {
                continue;
            }
            int x = cx + ox;
            int z = cz + oz;
            int g = s.groundY(x, z);           // 水面自动下沉到水底，石柱照样落地
            if (g == NONE) {
                continue;
            }
            int h = 10 + rnd.nextInt(15);      // 10~24
            // 底部 3×3 粗基（高柱才有，避免细长悬空感）
            if (h >= 16) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        int bx = x + dx;
                        int bz = z + dz;
                        int bg = s.groundY(bx, bz);
                        if (bg == NONE) {
                            continue;
                        }
                        for (int y = bg + 1; y <= g + 4; y++) {
                            s.set(bx, y, bz, (y == g + 4) ? smooth : rock);
                        }
                    }
                }
            }
            for (int y = g + 1; y <= g + h; y++) {
                int yRel = y - g;
                BlockState st = rock;
                if (yRel == h) {
                    st = smooth;
                } else if (yRel % 7 == 0) {
                    st = gilded;
                } else if (rnd.nextInt(6) == 0) {
                    st = basalt;
                }
                s.set(x, y, z, st);
            }
        }
        return true;
    }

    /**
     * <b>血池</b>：半径 3 的血肉池（地狱疣块），红地狱砖围边抬高 1 格，
     * 中央一根石英柱，池沿四角灵魂灯笼。
     */
    private static boolean bloodPool(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(6, 6, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        int r = 5;
        BlockState wart = Blocks.NETHER_WART_BLOCK.defaultBlockState();
        BlockState brick = Blocks.RED_NETHER_BRICKS.defaultBlockState();
        BlockState black = Blocks.BLACKSTONE.defaultBlockState();
        BlockState quartz = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r * r) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                if (d2 <= 9) {                    // 血肉池（半径 3）
                    column(s, x, z, base - 1, black, black);
                    s.set(x, base, z, wart);
                } else if (d2 <= 16) {            // 围边（半径 4），抬高 1 格
                    column(s, x, z, base, brick, black);
                    s.set(x, base + 1, z, brick);
                } else {
                    column(s, x, z, base, black, black);
                }
            }
        }
        // 中央石英柱
        for (int y = 1; y <= 4; y++) {
            s.set(cx, base + y, cz, quartz);
        }
        s.set(cx, base + 5, cz, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        // 池沿四角灵魂灯笼
        s.set(cx + 4, base + 2, cz, lantern);
        s.set(cx - 4, base + 2, cz, lantern);
        s.set(cx, base + 2, cz + 4, lantern);
        s.set(cx, base + 2, cz - 4, lantern);
        return true;
    }

    /**
     * <b>枯井</b>：5×5 井台 + 3×3 井口（往下 4 格）+ 两根黑石柱抬横梁，
     * 横梁下挂锁链，井底一点灵魂火（灵魂灯笼）。
     */
    private static boolean dryWell(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(5, 12, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        BlockState black = Blocks.BLACKSTONE.defaultBlockState();
        BlockState brick = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState red = Blocks.RED_NETHER_BRICKS.defaultBlockState();
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
        // 井台 5×5
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                column(s, cx + dx, cz + dz, base, edge ? brick : red, black);
            }
        }
        // 3×3 井口：从井台面往下挖 4 格（含台面那一层，井口才是通的）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                for (int y = base; y >= base - 3; y--) {
                    s.set(x, y, z, AIR);
                }
                s.set(x, base - 4, z, black);
            }
        }
        // 井底灵魂灯笼（放在井底黑石上）
        s.set(cx, base - 3, cz, lantern);
        // 井架：两根柱子 + 横梁
        for (int side = -1; side <= 1; side += 2) {
            int x = cx + side * 3;
            int z = cz;
            int g = s.groundY(x, z);
            if (g == NONE) {
                continue;
            }
            for (int y = g + 1; y <= base + 4; y++) {
                s.set(x, y, z, brick);
            }
        }
        for (int x = cx - 3; x <= cx + 3; x++) {
            s.set(x, base + 4, cz, brick);
        }
        // 横梁下挂一根锁链
        for (int y = base + 1; y <= base + 3; y++) {
            s.set(cx, y, cz, chain);
        }
        return true;
    }

    /** <b>锁链灯柱 / 火盆</b>：黑石灯柱 + 灵魂灯笼，另有灵魂火盆。 */
    private static void demonLamps(Site s, RandomSource rnd) {
        BlockState black = Blocks.BLACKSTONE.defaultBlockState();
        BlockState brick = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
        for (int i = 0; i < 8; i++) {
            int[] p = s.scatter(rnd, 2);
            if (p == null) {
                continue;
            }
            int x = p[0];
            int z = p[1];
            int ts = p[2];
            int h = 3 + rnd.nextInt(2);
            for (int y = 1; y <= h; y++) {
                s.set(x, ts + y, z, brick);
            }
            // 灯柱旁挂一截锁链
            s.set(x + 1, ts + h, z, chain);
            s.set(x + 1, ts + h - 1, z, chain);
            s.set(x, ts + h + 1, z, lantern);
        }
        for (int i = 0; i < 4; i++) {
            int[] p = s.scatter(rnd, 2);
            if (p == null) {
                continue;
            }
            int x = p[0];
            int z = p[1];
            int ts = p[2];
            s.set(x, ts + 1, z, black);
            s.set(x, ts + 2, z, Blocks.SOUL_CAMPFIRE.defaultBlockState());
        }
    }

    // ==================================================================
    // 虚空秘境：浮空岛 / 方尖碑 / 裂隙痕迹 / 紫水晶簇
    // ==================================================================

    private static void voidRealm(Site s, RandomSource rnd) {
        s.expect("island", 8);
        s.expect("obelisk", 4);
        s.expect("rift", 3);
        s.expect("amethyst", 6);
        for (int i = 0; i < 8; i++) {
            scene(s, "虚空·浮空岛", "island", () -> floatingIsland(s, rnd));
        }
        for (int i = 0; i < 4; i++) {
            scene(s, "虚空·方尖碑", "obelisk", () -> obelisk(s, rnd));
        }
        for (int i = 0; i < 3; i++) {
            scene(s, "虚空·裂隙痕迹", "rift", () -> riftTrace(s, rnd));
        }
        for (int i = 0; i < 6; i++) {
            scene(s, "虚空·紫水晶簇", "amethyst", () -> amethystCluster(s, rnd));
        }
    }

    /**
     * <b>浮空岛</b>：悬在地面之上 10~40 格的末地石/紫珀岛，底下接黑曜石倒锥。
     *
     * <p>岛面是「末地石 + 紫珀」混铺，锥底逐层收分接到 1 格尖，不会出现单块悬空；
     * 岛面插几根末地烛当坐标灯，远看就是一座座漂浮的碎岛。</p>
     */
    private static boolean floatingIsland(Site s, RandomSource rnd) {
        // halfExtent 取 7：浮空岛半径最大 9，但它是「浮」的，占地可以比地面场景小，
        // 用更小的 halfExtent 换取更宽的可用环带（否则 8 座岛放不下）。
        int[] spot = s.findSpot(7, 12, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int ground = spot[2];
        int radius = 5 + rnd.nextInt(5);       // 半径 5~9
        int clearance = 10 + rnd.nextInt(21);  // 岛底离地 10~30 格
        // 岛面 = 地表 + 净空 + 岛体厚度，保证倒锥尖也悬在地面上方
        int cy = ground - 1 + clearance + radius;
        BlockState end = Blocks.END_STONE.defaultBlockState();
        BlockState endBrick = Blocks.END_STONE_BRICKS.defaultBlockState();
        BlockState purpur = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r2) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                // 岛面
                s.set(x, cy, z, (rnd.nextInt(100) < 32) ? purpur : end);
                if (d2 <= (radius - 1) * (radius - 1)) {
                    s.set(x, cy - 1, z, endBrick);
                }
                // 倒锥：每下一层半径 -1
                for (int k = 0; k < radius - 1; k++) {
                    int rr = radius - 1 - k;
                    if (d2 > rr * rr) {
                        break;
                    }
                    BlockState st = (k >= 2)
                            ? (rnd.nextInt(10) == 0 ? crying : obsidian)
                            : endBrick;
                    s.set(x, cy - 2 - k, z, st);
                }
            }
        }
        // 岛面末地烛 + 紫水晶点缀
        int lamps = 2 + rnd.nextInt(3);
        for (int i = 0; i < lamps; i++) {
            int a = rnd.nextInt(360);
            double rad = Math.toRadians(a);
            int x = cx + (int) Math.round(Math.cos(rad) * (radius - 2));
            int z = cz + (int) Math.round(Math.sin(rad) * (radius - 2));
            s.set(x, cy + 1, z, Blocks.END_ROD.defaultBlockState());
        }
        if (rnd.nextBoolean()) {
            s.set(cx, cy + 1, cz, Blocks.AMETHYST_BLOCK.defaultBlockState());
        }
        return true;
    }

    /**
     * <b>方尖碑</b>：落地黑曜石方尖碑，高 12~24，主体 3×3 收分到 1×1，
     * 每 5 格一道哭泣黑曜石纹，紫珀封顶 + 末地烛。
     */
    private static boolean obelisk(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(5, 12, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        int h = 12 + rnd.nextInt(13);          // 12~24
        BlockState black = Blocks.BLACKSTONE.defaultBlockState();
        BlockState polished = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState purpur = Blocks.PURPUR_BLOCK.defaultBlockState();
        // 5×5 落地台基
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                column(s, cx + dx, cz + dz, base, edge ? polished : black, black);
            }
        }
        // 碑身：3×3 -> 1×1
        for (int yRel = 1; yRel <= h; yRel++) {
            int half = (yRel < h * 55 / 100) ? 1 : 0;
            int y = base + yRel;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    BlockState st;
                    if (yRel == h) {
                        st = purpur;
                    } else if (yRel % 5 == 4) {
                        st = crying;
                    } else {
                        st = obsidian;
                    }
                    s.set(cx + dx, y, cz + dz, st);
                }
            }
        }
        s.set(cx, base + h + 1, cz, Blocks.PURPUR_PILLAR.defaultBlockState());
        s.set(cx, base + h + 2, cz, Blocks.END_ROD.defaultBlockState());
        return true;
    }

    /**
     * <b>裂隙痕迹</b>：一条 16~22 格的哭泣黑曜石裂缝横过地表，两侧撒黑曜石碎点，
     * 偶尔竖起 1~3 格高的黑曜石碎片。
     */
    private static boolean riftTrace(Site s, RandomSource rnd) {
        // 裂隙只替换地表一层（瘦长），halfExtent 给 8 就够：实际最长 22 格的一半是 11，
        // 仍小于 findSpot 算出的 14 格保护带。
        int[] spot = s.findSpot(8, 14, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int len = 16 + rnd.nextInt(7);
        int half = len / 2;
        int[] dir = dirVec(rnd.nextInt(4));
        int ux = dir[0];
        int uz = dir[1];
        int vx = dir[2];
        int vz = dir[3];
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        for (int t = -half; t <= half; t++) {
            int x = cx + ux * t;
            int z = cz + uz * t;
            replaceSurface(s, x, z, crying);
            if (t % 3 == 0) {
                int side = rnd.nextBoolean() ? 1 : -1;
                replaceSurface(s, cx + ux * t + vx * side,
                        cz + uz * t + vz * side, obsidian);
            }
            if (rnd.nextInt(6) == 0) {
                int g = s.groundY(x, z);
                if (g == NONE) {
                    continue;
                }
                int shard = 1 + rnd.nextInt(3);
                for (int y = 1; y <= shard; y++) {
                    s.set(x, g + y, z, (y == shard) ? crying : obsidian);
                }
            }
        }
        return true;
    }

    /** <b>紫水晶簇</b>：3×3 紫水晶块台座 + 1~3 株紫水晶簇。 */
    private static boolean amethystCluster(Site s, RandomSource rnd) {
        int[] spot = s.findSpot(4, 10, rnd);
        if (spot == null) {
            return false;
        }
        int cx = spot[0];
        int cz = spot[1];
        int base = spot[2];
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState cluster = Blocks.AMETHYST_CLUSTER.defaultBlockState();
        BlockState end = Blocks.END_STONE.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                column(s, cx + dx, cz + dz, base, amethyst, end);
            }
        }
        int count = 1 + rnd.nextInt(3);
        for (int i = 0; i < count; i++) {
            int dx = rnd.nextInt(3) - 1;
            int dz = rnd.nextInt(3) - 1;
            s.set(cx + dx, base + 1, cz + dz, cluster);
        }
        return true;
    }
}
