package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.dungeon.BossRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;

/**
 * <b>下界 / 末地原生 Boss 的「巢穴」</b>（08 总表 D-6）。
 *
 * <h2>为什么要有这个类</h2>
 * <p>焚天炎帝、虚空龙帝是「自己寻找」的原生 Boss（见 {@link NativeBossSpawner}），
 * 但它们刷出来时只有<b>一个怪</b>，脚下是下界要塞/末地外岛的原始地形 —— 玩家找到了
 * 也没有「这是它的老巢」的仪式感。这个类负责在 Boss <b>成功刷出之后</b>，
 * 在它出生点附近现场盖一座巢穴：</p>
 * <ul>
 *   <li><b>焚天炎帝 → 熔岩王座</b>（下界）：15×15 黑石台基 + 三级王座台阶 + 王座 +
 *       锁链灯柱 + 火盆 + 背后岩浆瀑布（黑石槽 6 高泻入 1 格深岩浆池） + 四周矮墙。</li>
 *   <li><b>虚空龙帝 → 末地骨巢</b>（末地外岛）：半径 11 的紫珀圆台 + 中央凹坑
 *       （末地石 / 紫珀台阶 / 骨块 / 紫珀祭台） + 悬浮 7 格高的黑曜环（4 根 2 格粗黑曜柱支撑）
 *       + 骨块与末地烛点缀。</li>
 * </ul>
 *
 * <h2>硬约束（照本批施工约定）</h2>
 * <ul>
 *   <li><b>幂等</b>：巢穴中心有标记方块就跳过；另在出生点附近扫一遍同款标记，
 *       避免 Boss 反复刷新把同一个地方叠成一堆建筑。</li>
 *   <li><b>不覆盖原版结构</b>：巢穴中心离刷怪点 9 格（任务要求至少 6 格），
 *       并从 8 个方向里挑「上方最空」的那个方向，尽量不压到下界要塞 / 末地城。</li>
 *   <li><b>永远不静默放弃</b>：8 个候选方向都没加载（末地大片虚空 / 命令召唤在 y=0 这类情况）时，
 *       焚天炎帝改为「刷怪点 + 固定偏移」原地建；虚空龙帝改为以刷怪点 X/Z 造
 *       <b>悬浮圆台</b>（基准高度向下找最近实心面，找不到取 Boss Y-3 并夹到世界底上方），
 *       并强制加载足迹覆盖的区块。所有跳过分支都打 <b>WARN + 具体原因</b>。</li>
 *   <li><b>自己铺地基</b>：台基至少 3 层，悬空/岩浆面上也能立住；下界池底先垫黑石。</li>
 *   <li>固定 {@link RandomSource#create(long)} 种子；每格 {@code setBlock} 前判
 *       {@code isLoaded}；整体 {@code try/catch(Throwable)}，失败只 warn，不影响战斗。</li>
 *   <li>方块预算：每座巢穴 ≤ {@value #BUDGET}（实际约 1000~1700）。</li>
 * </ul>
 */
public final class BossLair {

    /** 固定随机种子：同一存档每次生成的巢穴一致（确定性）。 */
    private static final long SEED = 20261101L;

    /** 单座巢穴的方块预算（任务上限 4000）。 */
    private static final int BUDGET = 4000;

    /** 王座台基半边长（15×15）。 */
    private static final int HALF = 7;

    /** 末地巢穴圆台半径。 */
    private static final int NEST_R = 11;

    /**
     * 巢穴中心相对刷怪点的 8 个候选偏移（任务要求离刷怪点至少 6 格）。
     *
     * <p>正方向 9 格、斜方向 6 格（水平距离都是 9 格）——
     * 台基半边长 7，所以刷怪点永远在台基外/边缘，不会被建筑埋住。</p>
     */
    private static final int[][] SITES = {
            {9, 0}, {-9, 0}, {0, 9}, {0, -9},
            {6, 6}, {6, -6}, {-6, 6}, {-6, -6}
    };

    private BossLair() {
    }

    // ------------------------------------------------------------------
    // 入口：按实体类型分派
    // ------------------------------------------------------------------

    /**
     * Boss 成功刷出后调用：按实体类型现场盖对应的巢穴。
     *
     * <p>幂等 + 失败兜底：只在「还没建过」时动手，任何异常都只打日志 ——
     * 绝不能让刷怪流程崩掉。</p>
     */
    public static void ensureFor(ServerLevel level, LivingEntity boss) {
        if (level == null || boss == null) {
            return;
        }
        try {
            if (boss.getType() == ModEntities.BLAZE_EMPEROR.get()) {
                blazeThrone(level, boss.blockPosition());
            } else if (boss.getType() == ModEntities.VOID_DRAGON_EMPEROR.get()) {
                voidNest(level, boss.blockPosition());
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[lair] 巢穴生成失败（不影响战斗）：{}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 焚天炎帝：熔岩王座（下界）
    // ------------------------------------------------------------------

    /**
     * 熔岩王座。
     *
     * <p>布局（台面 = 刷怪点脚底高度 {@code y}，台基顶层方块在 {@code y-1}）：</p>
     * <pre>
     *       背墙(-X) ── 岩浆瀑布(6 高) ── 王座 ── 三级台阶 ──→ +X（朝向玩家来路）
     *       15×15 黑石台基；两侧各 1 根 5 高锁链灯柱；2 个灵魂火盆；四周黑石矮墙
     * </pre>
     */
    private static void blazeThrone(ServerLevel level, BlockPos spawn) {
        BlockPos site = chooseBlazeSite(level, spawn);
        if (site == null) {
            HongmengMod.LOGGER.warn("[lair] 焚天炎帝：8 个候选方向均不可用且兜底失败，跳过（刷怪点 {}）", spawn);
            return;
        }
        final BlockPos c = site;
        final int y = c.getY();
        // 保证王座足迹覆盖的区块已加载（Boss 可能刷在区块边界/未加载处）
        if (!ensureChunksLoaded(level, c, HALF + 2)) {
            HongmengMod.LOGGER.warn("[lair] 焚天炎帝：王座足迹区块加载失败，跳过（中心 {}，刷怪点 {}）", c, spawn);
            return;
        }
        // 幂等标记：王座正中那格「金块坐垫」。旧存档/重复调用命中就直接跳过；
        // 2026-11-02 起旧存档也补「宝箱 + 陈设」（不重铺台基；宝箱已存在则不再写）。
        BlockPos marker = pos(c, y, -3, 3, 0);
        if (level.getBlockState(marker).is(Blocks.GOLD_BLOCK)) {
            int added = blazeDetail(level, c, y);
            if (added > 0) {
                HongmengMod.LOGGER.info(
                        "[lair] 焚天炎帝·熔岩王座 旧存档补建：宝箱/陈设 {} 格 @{}", added, c);
            }
            return;
        }
        // 兜底：出生点附近已经有金块标记（= 已经盖过王座，或正好是堡垒遗迹），不再重复建
        if (hasBlockNear(level, spawn, Blocks.GOLD_BLOCK, 16, -2, 8)) {
            HongmengMod.LOGGER.warn("[lair] 焚天炎帝：附近已有金块标记（已有王座/堡垒遗迹），跳过重复建造（{}）", c);
            return;
        }
        RandomSource rnd = RandomSource.create(SEED);
        int placed = 0;
        placed += blazePlatform(level, c, y);
        placed += blazeSteps(level, c, y);
        placed += blazeSeat(level, c, y);
        placed += blazePillars(level, c, y);
        placed += blazeBraziers(level, c, y, rnd);
        placed += blazeLavaFall(level, c, y);
        placed += blazeRim(level, c, y);
        // ---- 内容层：2 宝箱 + 金堆/骨堆/旗帜/锁链/王座两侧火盆 ----
        placed += blazeDetail(level, c, y);
        if (placed > BUDGET) {
            HongmengMod.LOGGER.warn("[lair] 熔岩王座方块数 {} 超出预算 {}", placed, BUDGET);
        }
        HongmengMod.LOGGER.info("[lair] 焚天炎帝·熔岩王座 @{}（台面 y={}，共 {} 方块）",
                c, y, placed);
    }

    /** 15×15 台基：3 层（黑石 / 抛光黑石 / 玄武岩混搭）+ 四角向下扎根。 */
    private static int blazePlatform(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                placed += set(level, pos(c, y, dx, -1, dz),
                        mix(dx, dz, 31, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE,
                                Blocks.BASALT, Blocks.BLACKSTONE));
                placed += set(level, pos(c, y, dx, -2, dz),
                        mix(dx, dz, 32, Blocks.BLACKSTONE, Blocks.BASALT));
                placed += set(level, pos(c, y, dx, -3, dz),
                        Blocks.BLACKSTONE.defaultBlockState());
            }
        }
        // 四角再向下扎 3 格（跨在岩浆面上时不是「薄薄一张板」）
        int[][] corners = {{-HALF, -HALF}, {HALF, -HALF}, {-HALF, HALF}, {HALF, HALF}};
        for (int[] k : corners) {
            for (int h = 4; h <= 6; h++) {
                placed += set(level, pos(c, y, k[0], -h, k[1]),
                        Blocks.BASALT.defaultBlockState());
            }
        }
        return placed;
    }

    /** 三级王座台阶：一级宽 2 高 1 → 二级宽 2 高 2 → 三级（王座台）宽 3 高 3。 */
    private static int blazeSteps(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        BlockState polished = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        // 一级（最靠近 +X，朝向玩家来的方向）
        for (int dx = 1; dx <= 2; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                placed += set(level, pos(c, y, dx, 0, dz), polished);
            }
        }
        // 二级
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                placed += set(level, pos(c, y, dx, 0, dz), bricks);
                placed += set(level, pos(c, y, dx, 1, dz), bricks);
            }
        }
        // 三级（王座台）
        for (int dx = -4; dx <= -2; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                placed += set(level, pos(c, y, dx, 0, dz), bricks);
                placed += set(level, pos(c, y, dx, 1, dz), bricks);
                placed += set(level, pos(c, y, dx, 2, dz), polished);
            }
        }
        return placed;
    }

    /**
     * 王座：绯红菌柄椅背 + 镀金黑石扶手 + 金块坐垫 + 岩浆块王座脚。
     *
     * <p>王座正中的金块坐垫同时是<b>幂等标记</b>（坐标见 {@link #blazeThrone}）。</p>
     */
    private static int blazeSeat(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        BlockState back = Blocks.CRIMSON_HYPHAE.defaultBlockState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        // 椅背：4 高，两侧包镀金边
        for (int dz = -2; dz <= 2; dz++) {
            for (int h = 3; h <= 6; h++) {
                placed += set(level, pos(c, y, -4, h, dz),
                        Math.abs(dz) == 2 ? gilded : back);
            }
        }
        // 扶手（两侧，2 高）
        for (int side : new int[]{-2, 2}) {
            placed += set(level, pos(c, y, -3, 3, side), gilded);
            placed += set(level, pos(c, y, -3, 4, side), gilded);
        }
        // 坐垫（金块，正中那格是幂等标记）
        for (int dz = -1; dz <= 1; dz++) {
            placed += set(level, pos(c, y, -3, 3, dz), gold);
        }
        // 王座脚（岩浆块）
        placed += set(level, pos(c, y, -2, 3, -2), magma);
        placed += set(level, pos(c, y, -2, 3, 2), magma);
        return placed;
    }

    /** 两侧各 1 根 5 高锁链柱：抛光黑石柱 + 镀金横臂 + 垂挂锁链 + 灵魂灯笼。 */
    private static int blazePillars(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        BlockState pillar = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
        for (int side : new int[]{-5, 5}) {
            // 5 高柱身
            for (int h = 0; h <= 4; h++) {
                placed += set(level, pos(c, y, -3, h, side), pillar);
            }
            // 横臂 + 锁链 + 灯笼
            placed += set(level, pos(c, y, -2, 4, side), gilded);
            placed += set(level, pos(c, y, -2, 3, side), chain);
            placed += set(level, pos(c, y, -2, 2, side), chain);
            placed += set(level, pos(c, y, -2, 1, side), lantern);
        }
        return placed;
    }

    /** 2 个火盆（灵魂营火立在抛光黑石盆座上）。 */
    private static int blazeBraziers(ServerLevel level, BlockPos c, int y, RandomSource rnd) {
        int placed = 0;
        for (int side : new int[]{-5, 5}) {
            placed += set(level, pos(c, y, 5, 0, side),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            // 灵魂火盆 / 普通营火各一（一冷一暖，剪影更容易认）
            placed += set(level, pos(c, y, 5, 1, side),
                    (side < 0 || rnd.nextBoolean())
                            ? Blocks.SOUL_CAMPFIRE.defaultBlockState()
                            : Blocks.CAMPFIRE.defaultBlockState());
        }
        return placed;
    }

    /**
     * 背后 1 道岩浆瀑布：黑石槽（6 高）→ 1 格宽岩浆垂落 → 1 格深岩浆池。
     *
     * <p>岩浆源方块只有<b>两处</b>：槽口 1 格 + 池底 10 格（都是被黑石包住的静态源），
     * 垂落过程是自然流动的 1 格宽液柱，池子由同层台基方块与黑石矮墙围住，不会大面积漫流。</p>
     */
    private static int blazeLavaFall(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        BlockState black = Blocks.BLACKSTONE.defaultBlockState();
        BlockState polished = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        // 背墙：-X 端，7 高（顶层明暗错开，像一堵粗糙的黑石崖壁）
        for (int dz = -HALF; dz <= HALF; dz++) {
            for (int h = 0; h <= 6; h++) {
                BlockState st = (h == 6 && (dz & 1) == 0) ? polished
                        : mix(dz, h, 41, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE,
                                Blocks.CHISELED_POLISHED_BLACKSTONE);
                placed += set(level, pos(c, y, -HALF, h, dz), st);
            }
        }
        // 岩浆池（1 格深）：挖掉台面顶层（y-1），换成静态岩浆源
        for (int dx = -6; dx <= -5; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                placed += set(level, pos(c, y, dx, -1, dz), Blocks.LAVA.defaultBlockState());
            }
        }
        // 池边黑石矮墙（池子另外三面是同层台基 / 三级台阶的立面，已经封死）
        for (int dx = -6; dx <= -5; dx++) {
            placed += set(level, pos(c, y, dx, 0, -3), black);
            placed += set(level, pos(c, y, dx, 0, 3), black);
        }
        // 黑石槽：6 高，槽底托住岩浆源，两侧挡墙防止横向漫流（只朝 +X 开口）
        for (int dz = -1; dz <= 1; dz++) {
            placed += set(level, pos(c, y, -6, 5, dz), black);
        }
        placed += set(level, pos(c, y, -6, 6, -1), chiseled);
        placed += set(level, pos(c, y, -6, 6, 1), chiseled);
        placed += set(level, pos(c, y, -6, 6, 0), Blocks.LAVA.defaultBlockState());
        return placed;
    }

    /** 四周黑石矮墙压边（最外一圈 1 高，台基边沿更「实」）。 */
    private static int blazeRim(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != HALF) {
                    continue;
                }
                placed += set(level, pos(c, y, dx, 0, dz),
                        mix(dx, dz, 51, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE,
                                Blocks.BASALT));
            }
        }
        return placed;
    }

    /** 从 8 个候选方向里挑「上方最空」的一个作为王座中心；一个都没加载时原地兜底。 */
    private static BlockPos chooseBlazeSite(ServerLevel level, BlockPos spawn) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int[] s : SITES) {
            BlockPos c = new BlockPos(spawn.getX() + s[0], spawn.getY(), spawn.getZ() + s[1]);
            if (!level.isLoaded(c)) {
                continue;
            }
            int score = obstructionScore(level, c, HALF, 8);
            if (score < bestScore) {
                bestScore = score;
                best = c;
            }
        }
        if (best != null) {
            return best;
        }
        // 兜底（Lead 实测要求）：8 个候选方向所在区块都没加载（已加载空位 = 0）时，
        // 直接以「Boss 位置 + 固定偏移」原地建；足迹区块随后由 ensureChunksLoaded 强制加载。
        BlockPos fallback = new BlockPos(spawn.getX() + SITES[0][0], spawn.getY(),
                spawn.getZ() + SITES[0][1]);
        HongmengMod.LOGGER.warn(
                "[lair] 焚天炎帝：8 个候选方向均未加载（已加载空位=0）→ 改用刷怪点 +X {} 格兜底 @{}（刷怪点 {}）",
                SITES[0][0], fallback, spawn);
        return fallback;
    }

    // ------------------------------------------------------------------
    // 虚空龙帝：末地骨巢（末地外岛）
    // ------------------------------------------------------------------

    /**
     * 末地骨巢。
     *
     * <p>布局（台面 = 刷怪点脚底高度 {@code y}）：</p>
     * <pre>
     *        悬浮黑曜环（半径 9~10.8，y+7，4 根 2 格粗黑曜柱支撑）
     *              ╲             ╱
     *     ═════════════ 半径 11 圆台 ═════════════
     *     外圈黑石 | 末地石砖+紫珀 | 中央凹坑(下挖 2)：末地石/紫珀台阶/骨块/紫珀祭台
     * </pre>
     */
    private static void voidNest(ServerLevel level, BlockPos spawn) {
        BlockPos site = chooseVoidSite(level, spawn);
        final BlockPos c;
        final int y;
        if (site != null) {
            c = site;
            y = c.getY();
        } else {
            // 兜底（Lead 实测要求）：末地大片虚空/未加载时 8 方向全不合格 →
            // 直接以 Boss 所在位置的 X/Z 造一座「悬浮紫珀圆台」，基准高度向下找最近实心面，
            // 找不到就取 Boss Y-3 并夹到世界底部之上（末地浮空巢本来就该能悬空）。
            ensureChunksLoaded(level, spawn, NEST_R + 2);
            int gy = findSurfaceY(level, spawn);
            boolean guessed = gy == Integer.MIN_VALUE;
            if (guessed) {
                // 取 Boss Y-3 作为基准；再夹到「世界底 +3」之上，
                // 保证三层圆台（y-1 / y-2 / y-3）与 2 格深凹坑的坑底都在世界高度内，
                // 不会在末地最底层挖出通到虚空的洞。
                gy = Math.max(level.getMinBuildHeight() + 3, spawn.getY() - 3);
            }
            y = gy;
            c = new BlockPos(spawn.getX(), y, spawn.getZ());
            HongmengMod.LOGGER.warn(
                    "[lair] 虚空龙帝：8 个候选方向均未加载（已加载空位=0）→ 以刷怪点原地造悬空圆台 @{}"
                            + "（基准 y={}，{}，刷怪点 {}）",
                    c, y, guessed ? "下方 64 格内无实心面→取 Boss Y-3 并夹到世界底+3" : "取下方最近实心面",
                    spawn);
        }
        // 保证巢穴足迹覆盖的区块已加载（末地浮空巢可能落在区块边界/未加载处）
        if (!ensureChunksLoaded(level, c, NEST_R + 2)) {
            HongmengMod.LOGGER.warn("[lair] 虚空龙帝：巢穴足迹区块加载失败，跳过（中心 {}，刷怪点 {}）",
                    c, spawn);
            return;
        }
        // 幂等标记：中央紫珀祭台顶端的哭泣黑曜石。旧存档也补宝箱/陈设（不重铺圆台）。
        BlockPos marker = pos(c, y, 0, 0, 0);
        if (level.getBlockState(marker).is(Blocks.CRYING_OBSIDIAN)) {
            int added = voidDetail(level, c, y);
            if (added > 0) {
                HongmengMod.LOGGER.info(
                        "[lair] 虚空龙帝·末地巢穴 旧存档补建：宝箱/陈设 {} 格 @{}", added, c);
            }
            return;
        }
        // 兜底：出生点附近已有哭泣黑曜石（= 已经盖过巢穴），不再重复建
        if (hasBlockNear(level, spawn, Blocks.CRYING_OBSIDIAN, 20, -3, 9)) {
            HongmengMod.LOGGER.warn("[lair] 虚空龙帝：附近已有哭泣黑曜石标记（已有巢穴），跳过重复建造（{}）", c);
            return;
        }
        int placed = 0;
        // 先清出柱状空间：抹平岛面起伏、避开末地城的塔体（只清 y ~ y+10，不往下挖岛体）
        placed += clearSpace(level, c, y, NEST_R, 10);
        placed += voidPlatform(level, c, y);
        placed += voidPit(level, c, y);
        placed += voidRing(level, c, y);
        placed += voidDecor(level, c, y);
        // ---- 内容层：2 宝箱 + 龙蛋台座 / 紫晶簇 / 末地烛阵 / 骨堆 / 旗帜 ----
        placed += voidDetail(level, c, y);
        if (placed > BUDGET) {
            HongmengMod.LOGGER.warn("[lair] 末地巢穴方块数 {} 超出预算 {}", placed, BUDGET);
        }
        HongmengMod.LOGGER.info("[lair] 虚空龙帝·末地巢穴 @{}（台面 y={}，共 {} 方块）",
                c, y, placed);
    }

    /** 半径 11 圆台：3 层（末地石砖 / 末地石 / 外圈黑石）。 */
    private static int voidPlatform(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        for (int dx = -NEST_R; dx <= NEST_R; dx++) {
            for (int dz = -NEST_R; dz <= NEST_R; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > NEST_R * NEST_R) {
                    continue;
                }
                double r = Math.sqrt(d2);
                BlockState top;
                if (r > 9.5) {
                    // 外圈：黑石压边
                    top = mix(dx, dz, 61, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE);
                } else if (r > 4.5) {
                    // 台面：末地石砖 + 紫珀拼纹
                    top = mix(dx, dz, 62, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK,
                            Blocks.END_STONE_BRICKS);
                } else {
                    // 凹坑区：末地石（稍后下挖 2 格）
                    top = Blocks.END_STONE.defaultBlockState();
                }
                placed += set(level, pos(c, y, dx, -1, dz), top);
                placed += set(level, pos(c, y, dx, -2, dz),
                        mix(dx, dz, 63, Blocks.END_STONE_BRICKS, Blocks.END_STONE));
                placed += set(level, pos(c, y, dx, -3, dz),
                        Blocks.END_STONE.defaultBlockState());
            }
        }
        return placed;
    }

    /** 中央凹坑（下挖 2）：末地石坑底 + 紫珀台阶 + 骨块 + 紫珀祭台（顶端是幂等标记）。 */
    private static int voidPit(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        // 挖掉最上面 2 层（r ≤ 4.5），露出 y-3 的末地石坑底
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz > 20) {
                    continue;
                }
                placed += clear(level, pos(c, y, dx, -1, dz));
                placed += clear(level, pos(c, y, dx, -2, dz));
            }
        }
        BlockState stairs = Blocks.PURPUR_STAIRS.defaultBlockState();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0D * i / 8.0D;
            int dx = (int) Math.round(Math.cos(a) * 4.0D);
            int dz = (int) Math.round(Math.sin(a) * 4.0D);
            placed += set(level, pos(c, y, dx, -2, dz), stairs);
        }
        // 坑里散几块骨头
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        for (int[] k : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            placed += set(level, pos(c, y, k[0], -2, k[1]), bone);
        }
        // 紫珀祭台：3×3 紫珀底座 + 紫珀柱 + 顶端哭泣黑曜石（兼幂等标记）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placed += set(level, pos(c, y, dx, -2, dz),
                        Blocks.PURPUR_BLOCK.defaultBlockState());
            }
        }
        placed += set(level, pos(c, y, 0, -1, 0), Blocks.PURPUR_PILLAR.defaultBlockState());
        placed += set(level, pos(c, y, 0, 0, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        return placed;
    }

    /** 悬浮黑曜环（半径 9~10.8，y+7，1.8 格宽）+ 4 根 2 格粗黑曜柱支撑。 */
    private static int voidRing(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        final int ringDy = 7;          // 任务要求悬浮 6~10 高
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        // 环体：用半径带保证「连续成环」，不是一堆孤立方块
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r < 9.0D || r > 10.8D) {
                    continue;
                }
                BlockState st = Math.floorMod(hash(dx, ringDy, dz), 9) == 0
                        ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : obsidian;
                placed += set(level, pos(c, y, dx, ringDy, dz), st);
            }
        }
        // 4 根 2 格粗的黑曜柱（斜四方向），从台面顶撑到环底
        int[][] supports = {{6, 6}, {6, -6}, {-6, 6}, {-6, -6}};
        for (int[] k : supports) {
            for (int sx = 0; sx <= 1; sx++) {
                for (int sz = 0; sz <= 1; sz++) {
                    for (int h = 0; h <= ringDy - 1; h++) {
                        placed += set(level, pos(c, y, k[0] + sx, h, k[1] + sz), obsidian);
                    }
                }
            }
        }
        return placed;
    }

    /** 四周 6 处骨块 / 末地烛点缀。 */
    private static int voidDecor(ServerLevel level, BlockPos c, int y) {
        int placed = 0;
        int[][] spots = {{8, 0}, {-8, 0}, {0, 8}, {3, -8}, {-4, -7}, {-8, 3}};
        for (int[] k : spots) {
            int h = 1 + Math.floorMod(hash(k[0], 71, k[1]), 2);   // 1~2 高骨柱
            for (int i = 0; i < h; i++) {
                placed += set(level, pos(c, y, k[0], i, k[1]),
                        Blocks.BONE_BLOCK.defaultBlockState());
            }
            placed += set(level, pos(c, y, k[0], h, k[1]),
                    Blocks.END_ROD.defaultBlockState());
        }
        return placed;
    }

    /** 从 8 个候选方向里挑「上方最空」的一个作为巢穴中心（外岛地面起伏大，挑平的）。 */
    private static BlockPos chooseVoidSite(ServerLevel level, BlockPos spawn) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int[] s : SITES) {
            BlockPos c = new BlockPos(spawn.getX() + s[0], spawn.getY(), spawn.getZ() + s[1]);
            if (!level.isLoaded(c)) {
                continue;
            }
            int score = obstructionScore(level, c, NEST_R, 10);
            if (score < bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // 内容层（2026-11-02）：奖励箱 + 主题陈设（新档 / 旧存档补建共用）
    // ------------------------------------------------------------------

    /**
     * 熔岩王座的「内容层」：2 宝箱 + 金堆 / 骨堆 / 旗帜 / 锁链 / 王座两侧火盆。
     *
     * <p><b>幂等</b>：先放 2 个宝箱（{@link BossRewards#placeChests} 对已存在容器跳过）。
     * 一个都没新增 = 旧存档已经补过（或玩家自己放了箱子）→ 返回 0，不重复写陈设。</p>
     *
     * <p>坐标（相对王座台基中心 {@code c}，{@code y} 为台面脚底高度）：
     * 宝箱 {@code (4, y, ±4)}（台基前部，避开台阶/火盆）；金堆 {@code (-5, y, ±5..6)}；
     * 骨堆 {@code (3, y, ±6)}；旗帜 {@code (6, y, ±6)}；锁链柱 {@code (-5, y, ±4)}；
     * 王座两侧火盆 {@code (-2, y+3, ±3)}（三级台面上）。</p>
     */
    private static int blazeDetail(ServerLevel level, BlockPos c, int y) {
        int added = BossRewards.placeChests(level, c,
                new int[][]{{4, 0, 4}, {4, 0, -4}},
                BossRewards.LOOT_NATIVE_LAIR, SEED ^ 0x424C415AL);
        if (added == 0) {
            return 0;
        }
        int placed = added;
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        BlockState banner = Blocks.RED_BANNER.defaultBlockState();
        // 金堆（王座左侧/右侧，避开背后岩浆池 dx -6..-5 / dz -2..2）
        for (int side : new int[]{5, -5}) {
            placed += set(level, pos(c, y, -5, 0, side), gold);
            placed += set(level, pos(c, y, -5, 0, side + Integer.signum(side)), gold);
            placed += set(level, pos(c, y, -5, 1, side), gold);
        }
        // 骨堆（台基前部两侧：征伐战利品）
        for (int side : new int[]{6, -6}) {
            placed += set(level, pos(c, y, 3, 0, side), bone);
            placed += set(level, pos(c, y, 3, 1, side), bone);
        }
        // 旗帜（台基前沿两侧）
        placed += set(level, pos(c, y, 6, 0, 6), banner);
        placed += set(level, pos(c, y, 6, 0, -6), banner);
        // 锁链桩（黑石基座 + 垂挂锁链）
        for (int side : new int[]{4, -4}) {
            placed += set(level, pos(c, y, -5, 0, side),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            placed += set(level, pos(c, y, -5, 1, side),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            placed += set(level, pos(c, y, -5, 2, side), Blocks.CHAIN.defaultBlockState());
            placed += set(level, pos(c, y, -5, 3, side), Blocks.CHAIN.defaultBlockState());
        }
        // 王座两侧火盆（三级台面 y+3 上，紧贴王座脚）
        placed += set(level, pos(c, y, -2, 3, 3), Blocks.SOUL_CAMPFIRE.defaultBlockState());
        placed += set(level, pos(c, y, -2, 3, -3), Blocks.SOUL_CAMPFIRE.defaultBlockState());
        return placed;
    }

    /**
     * 末地巢穴的「内容层」：2 宝箱 + 龙蛋台座 / 紫晶簇 / 末地烛阵 / 骨堆 / 旗帜。
     *
     * <p>幂等同 {@link #blazeDetail}：宝箱已存在 → 返回 0，不重复写陈设。</p>
     *
     * <p>坐标（相对巢穴中心 {@code c}，{@code y} 为圆台脚底高度）：
     * 宝箱 {@code (±6, y, 0)}；龙蛋台座 {@code (0, y+1, 0)}（祭台顶端标记之上）；
     * 紫晶簇 {@code (±4, y, ±4)}；末地烛阵 {@code (±8, y, ±4)}；
     * 骨堆 {@code (±2, y, 7)}；旗帜 {@code (±9, y, 0)}。</p>
     */
    private static int voidDetail(ServerLevel level, BlockPos c, int y) {
        int added = BossRewards.placeChests(level, c,
                new int[][]{{6, 0, 0}, {-6, 0, 0}},
                BossRewards.LOOT_NATIVE_LAIR, SEED ^ 0x564F4944L);
        if (added == 0) {
            return 0;
        }
        int placed = added;
        // 龙蛋台座：祭台顶端（标记块）之上放一枚龙蛋，作为「虚空龙帝的卵」战利品展示
        placed += set(level, pos(c, y, 0, 1, 0), Blocks.DRAGON_EGG.defaultBlockState());
        // 紫晶簇（骨巢四角内侧）
        for (int[] k : new int[][]{{4, 4}, {-4, 4}, {4, -4}, {-4, -4}}) {
            placed += set(level, pos(c, y, k[0], 0, k[1]),
                    Blocks.AMETHYST_BLOCK.defaultBlockState());
            placed += set(level, pos(c, y, k[0], 1, k[1]),
                    Blocks.AMETHYST_CLUSTER.defaultBlockState());
        }
        // 末地烛阵（外圈黑石环内侧，4 处 × 2 高）
        for (int[] k : new int[][]{{8, 4}, {-8, 4}, {8, -4}, {-8, -4}}) {
            placed += set(level, pos(c, y, k[0], 0, k[1]), Blocks.END_ROD.defaultBlockState());
            placed += set(level, pos(c, y, k[0], 1, k[1]), Blocks.END_ROD.defaultBlockState());
        }
        // 骨堆（巢台边缘）
        placed += set(level, pos(c, y, 2, 0, 7), Blocks.BONE_BLOCK.defaultBlockState());
        placed += set(level, pos(c, y, 2, 1, 7), Blocks.BONE_BLOCK.defaultBlockState());
        placed += set(level, pos(c, y, -2, 0, 7), Blocks.BONE_BLOCK.defaultBlockState());
        // 巢旗（黑曜环正下方，左右各一）
        placed += set(level, pos(c, y, 9, 0, 0), Blocks.PURPLE_BANNER.defaultBlockState());
        placed += set(level, pos(c, y, -9, 0, 0), Blocks.PURPLE_BANNER.defaultBlockState());
        return placed;
    }

    // ------------------------------------------------------------------
    // 公共小工具
    // ------------------------------------------------------------------

    /**
     * 强制加载巢穴足迹覆盖的区块（施工约定允许的兜底手段：{@code getChunk(cx,cz,FULL,true)}）。
     *
     * <p>原生 Boss 可能刷在区块边界、甚至未加载处（Lead 实测：末地命令召唤在 y=0）。
     * 只加载足迹覆盖的 2~4 个区块，一次性开销很小；返回是否全部就绪。</p>
     */
    private static boolean ensureChunksLoaded(ServerLevel level, BlockPos c, int radius) {
        int minCx = (c.getX() - radius) >> 4;
        int maxCx = (c.getX() + radius) >> 4;
        int minCz = (c.getZ() - radius) >> 4;
        int maxCz = (c.getZ() + radius) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (level.getChunkSource().hasChunk(cx, cz)) {
                    continue;
                }
                try {
                    level.getChunk(cx, cz, ChunkStatus.FULL, true);
                } catch (Throwable t) {
                    HongmengMod.LOGGER.warn("[lair] 区块 ({}, {}) 加载失败：{}", cx, cz, t.toString());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 从某点向下找最近的实心面，返回「脚底站在该面上」的高度。
     *
     * @return 找不到（下方 64 格无实心面 / 已到世界底）返回 {@link Integer#MIN_VALUE}
     */
    private static int findSurfaceY(ServerLevel level, BlockPos spawn) {
        int minY = level.getMinBuildHeight();
        int from = Math.min(spawn.getY(), level.getMaxBuildHeight() - 1);
        for (int yy = from; yy - 1 >= minY; yy--) {
            BlockPos p = new BlockPos(spawn.getX(), yy, spawn.getZ());
            if (!level.isLoaded(p)) {
                continue;
            }
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && st.getFluidState().isEmpty()) {
                return yy + 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** 台面以上 11 格柱状空间的「阻挡分」：越高空越适合盖巢穴（同时避开要塞/末地城）。 */
    private static int obstructionScore(ServerLevel level, BlockPos c, int rad, int height) {
        int score = 0;
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                if (dx * dx + dz * dz > rad * rad) {
                    continue;
                }
                for (int h = 0; h <= height; h++) {
                    BlockPos p = new BlockPos(c.getX() + dx, c.getY() + h, c.getZ() + dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    BlockState st = level.getBlockState(p);
                    if (!st.isAir() && !st.is(Blocks.LAVA)) {
                        score++;
                    }
                }
            }
        }
        return score;
    }

    /** 出生点附近是否已经有某类方块（用于「同一座巢穴不要重复建」的兜底）。 */
    private static boolean hasBlockNear(ServerLevel level, BlockPos center, Block block,
                                        int rad, int dyMin, int dyMax) {
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                if (dx * dx + dz * dz > rad * rad) {
                    continue;
                }
                for (int dy = dyMin; dy <= dyMax; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    if (level.getBlockState(p).is(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 清空一个柱状空间（只清非空气，避免大量无意义的 setBlock）。 */
    private static int clearSpace(ServerLevel level, BlockPos c, int y, int rad, int height) {
        int placed = 0;
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                if (dx * dx + dz * dz > rad * rad) {
                    continue;
                }
                for (int h = 0; h <= height; h++) {
                    placed += clear(level, pos(c, y, dx, h, dz));
                }
            }
        }
        return placed;
    }

    /** 按局部坐标取绝对坐标（{@code y} = 台面脚底高度，dy 为相对偏移）。 */
    private static BlockPos pos(BlockPos c, int y, int dx, int dy, int dz) {
        return new BlockPos(c.getX() + dx, y + dy, c.getZ() + dz);
    }

    /** 放方块（幂等计数用）：返回实际写入的方块数；未加载 / 越界的格子跳过。 */
    private static int set(ServerLevel level, BlockPos p, BlockState st) {
        if (!inWorld(level, p) || !level.isLoaded(p)) {
            return 0;
        }
        level.setBlock(p, st, 2);
        return 1;
    }

    /** 清成空气：返回实际写入数（本来就是空气 = 0，不算方块）。 */
    private static int clear(ServerLevel level, BlockPos p) {
        if (!inWorld(level, p) || !level.isLoaded(p)) {
            return 0;
        }
        if (level.getBlockState(p).isAir()) {
            return 0;
        }
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        return 1;
    }

    /** 是否在世界高度范围内（下界只有 y=0~127，越界 setBlock 会被静默丢弃）。 */
    private static boolean inWorld(ServerLevel level, BlockPos p) {
        return p.getY() >= level.getMinBuildHeight() && p.getY() < level.getMaxBuildHeight();
    }

    /** 稳定的逐格哈希（同坐标永远同结果 → 台面纹理可复现）。 */
    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** 多材质混搭：用哈希在候选方块里挑一个（同坐标永远一致）。 */
    private static BlockState mix(int dx, int dz, int salt, Block... choices) {
        int i = Math.floorMod(hash(dx + salt * 31, salt, dz - salt * 17), choices.length);
        return choices[i].defaultBlockState();
    }
}
