package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;

/**
 * <b>boss 维度的主题地貌（v2：把"超平坦 + 稀疏装饰"改成"能读出位面身份"的场景）</b>。
 *
 * <h2>为什么重写</h2>
 * <p>v1 只在半径 120~200 的环里随机撒了几十根柱子/骨拱/尖峰，而且
 * {@code ArenaRealm.forceLoad} 当时只加载中心 9×9 区块（方块 −64~79），
 * 环带里 {@code level.isLoaded()} 全部为 {@code false} ——
 * <b>v1 实际上几乎一块都没建出来</b>（玩家反馈"主题地貌像没有"）。
 * v2 做了三件事：</p>
 * <ol>
 *   <li><b>先按环带预加载区块</b>（只加载与环带相交的，flat 生成器很便宜），
 *       保证 {@code isLoaded} 判据不会把格子静默跳过；</li>
 *   <li><b>四套场景各有"一眼记住"的焦点</b>：日晷 / 巨骨拱 / 血池祭坛 / 王座环；</li>
 *   <li><b>硬安全边界</b>：半径 {@value #SAFE_R}（数值 100）以内一格都不写，
 *       半径 {@value #MAX_REACH} 以外不写；每次 {@code setBlock} 前都判
 *       {@code isLoaded}，失败的格子计数后跳过。</li>
 * </ol>
 *
 * <h2>布置范围（绝不干扰竞技场）</h2>
 * <p>竞技场：沙场半径 {@code 44}、看台外沿 {@code 84}（{@code ArenaBattle}）。
 * 主题环：半径 {@value #INNER}~{@value #OUTER}；实际场景全部收在半径 176 以内
 * （= 预加载环带上限，见 {@link #PRELOAD_OUTER}），保证预加载能覆盖每一个写点、
 * {@code skippedUnloaded == 0}。</p>
 *
 * <h2>四套场景</h2>
 * <table border="1">
 *   <caption>副本 → 主题环场景</caption>
 *   <tr><th>副本</th><th>场景</th><th>焦点/主要构件</th></tr>
 *   <tr><td>纪元</td><td>机关天宫</td>
 *       <td>三重圜丘台阶（r=124/136/148）、日晷（15 宽圆台 + 12 时辰金刻 +
 *           深板岩晷针 + 黑石日影线）、8 座齿轮柱、2 段弧形残柱列、铜/青苔散落</td></tr>
 *   <tr><td>妖兽</td><td>骨原</td>
 *       <td>6 座巨骨拱（跨度 20、高 14，一端 5×5 颅骨）、8 处兽巢、4 根图腾、
 *           枯树/乱石/骨堆、2 处篝火</td></tr>
 *   <tr><td>魔道</td><td>血狱</td>
 *       <td>6 片熔岩湖（碗形挖深 4~6、湖底 2 层岩浆、黑石/玄武岩湖沿、一道熔岩瀑布）、
 *           2 组黑石尖峰（越高越细）、血池祭坛（三重八角坛 + 绯红柱 + 门式拱架锁链 +
 *           灵魂灯笼 + 血池）、火盆</td></tr>
 *   <tr><td>虚空</td><td>虚空王座</td>
 *       <td>12 座浮空岛（末地石/紫珀顶 + 黑曜倒锥底）、3 条径向裂隙、
 *           8 根紫晶柱、巨型浮空王座环（黑曜连续环 + 紫珀台 + 紫水晶王座 + 末地烛）</td></tr>
 * </table>
 *
 * <h2>工程约束（踩过的坑）</h2>
 * <ul>
 *   <li><b>确定性</b>：一律 {@code RandomSource.create(SEED + dungeon.ordinal())}，
 *       不用 {@code level.random}；同存档重复生成结果一致。</li>
 *   <li><b>幂等 + 版本号</b>：维度级 {@code SavedData} 记 {@code themeVersion}；
 *       <b>改了本类的生成逻辑必须把 {@link #THEME_VERSION} +1</b>，
 *       否则老存档的"已生成"标记会让玩家永远看不到新场景（v1 的教训）。</li>
 *   <li><b>预算</b>：单维度一次性 ≤ {@value #BLOCK_BUDGET} 次 setBlock；
 *       光源（灯笼/末地烛/火盆）≤ {@value #LIGHT_BUDGET} 盏。</li>
 *   <li><b>不许单块悬空</b>：地面构件一律从地面向上砌；浮空岛底部是倒锥；
 *       齿轮盘压在石柱上；王座环是连续环 + 辐条连到中央紫珀台。</li>
 *   <li>{@code setBlock(pos, state, 2)} 且先判 {@code isLoaded}；
 *       绝不用 {@code setBlockAndUpdate}（会刷方块更新，环带几千格会卡服）。</li>
 *   <li>整体包 {@code try/catch(Throwable)}，失败只 {@code LOGGER.warn}，
 *       <b>绝不影响进入维度/战斗流程</b>。</li>
 * </ul>
 */
public final class ArenaTheme {

    /**
     * <b>主题地貌生成逻辑版本号</b>（与竞技场本体的
     * {@code ArenaRealm.GEN_VERSION} 分开，互不影响）。
     *
     * <p>⚠ 改本类任何几何/材料/随机顺序，必须 +1；否则老存档里
     * {@code themeVersion == 当前值} 会直接跳过重建。</p>
     * <p>历史：v1 = 老版稀疏装饰（只有 {@code themeBuilt} 布尔）；v2 = 四套完整场景。</p>
     */
    public static final int THEME_VERSION = 2;

    /** 主题环内半径（竞技场看台外沿 84 + 余量）。 */
    private static final int INNER = 120;
    /** 主题环外半径（用户任务书口径：120~168）。 */
    private static final int OUTER = 168;
    /** 绝对禁止写入的半径：场地/看台/四门必须保持干净。 */
    private static final int SAFE_R = 100;
    /** 绝对禁足上限（约定文档 §1.3：主题环 120~200）。 */
    private static final int MAX_REACH = 200;
    /** 预加载环带内半径（覆盖全部场景 + 一点余量）。 */
    private static final int PRELOAD_INNER = 112;
    /** 预加载环带外半径（= 设计上限；保证 skippedUnloaded == 0）。 */
    private static final int PRELOAD_OUTER = 176;

    /** 固定随机种子（保证同一存档每次进来地貌一致）。 */
    private static final long SEED = 20261014L;
    /** 单维度 setBlock 预算（约定文档 §1.7）。 */
    private static final int BLOCK_BUDGET = 400_000;
    /** 光源点数预算（灯笼/末地烛/火盆）。 */
    private static final int LIGHT_BUDGET = 400;

    private ArenaTheme() {
    }

    /**
     * 按副本把主题环场景铺好（幂等：{@code themeVersion} 已是当前版本就跳过）。
     *
     * @param level   目标维度
     * @param dungeon 副本（决定用哪套场景）
     * @param center  竞技场中心（{@code ArenaRealm.center(level)}，y = 站人高度）
     */
    public static void ensure(ServerLevel level, Dungeon dungeon, BlockPos center) {
        if (level == null || dungeon == null || center == null) {
            return;
        }
        int built = ArenaRealm.themeVersion(level);
        if (built >= THEME_VERSION) {
            return;
        }
        long start = System.currentTimeMillis();
        Site site = new Site(level, dungeon, center);
        try {
            // ① 先按环带预加载区块（flat 生成器便宜；只加载与环带相交的）
            long preloadStart = System.currentTimeMillis();
            preloadRing(site);
            long preloadCost = System.currentTimeMillis() - preloadStart;

            // ② 再摆场景（所有写点都在预加载覆盖范围内）
            switch (dungeon) {
                case EPOCH -> epochHall(site);
                case BEAST -> beastBoneyard(site);
                case DEMON -> demonHell(site);
                case VOID -> voidRift(site);
            }

            // ③ 收尾统计 + 写版本标记
            long cost = System.currentTimeMillis() - start;
            HongmengMod.LOGGER.info(
                    "[arena] {} theme v{} done: placed={} skippedUnloaded={} "
                            + "rejectedOutOfRing={} lights={} budget={} preload={}ms cost={}ms",
                    dungeon.dimensionId(), THEME_VERSION, site.placed, site.skipped,
                    site.rejected, site.lights, BLOCK_BUDGET, preloadCost, cost);
            if (site.skipped > 0) {
                // 预加载没覆盖到 = 预加载范围/场景设计越界，必须查（Lead 服务端核对）
                HongmengMod.LOGGER.warn(
                        "[arena] {} 主题环有 {} 格因区块未加载被跳过（预加载范围不足）",
                        dungeon.dimensionId(), site.skipped);
            }
            if (site.placed > BLOCK_BUDGET) {
                HongmengMod.LOGGER.warn(
                        "[arena] {} 主题环 setBlock={} 超出预算 {}",
                        dungeon.dimensionId(), site.placed, BLOCK_BUDGET);
            }
            ArenaRealm.markThemeBuilt(level, THEME_VERSION);
            HongmengMod.LOGGER.info(
                    "[arena] {} 主题环 v{} 已生成（半径 {}-{}）",
                    dungeon.dimensionId(), THEME_VERSION, INNER, OUTER);
        } catch (Throwable t) {
            // 主题地貌只是装饰：失败绝不打断"进入维度 / 打 boss"
            HongmengMod.LOGGER.warn("[arena] {} 主题地貌生成失败（不影响战斗）：{}",
                    dungeon.dimensionId(), t.toString());
        }
    }

    // ==================================================================
    // 预加载：只加载"与环带相交"的区块，并打耗时日志
    // ==================================================================

    /**
     * 预加载主题环带（方块半径 {@value #PRELOAD_INNER}~{@value #PRELOAD_OUTER}）相交的区块。
     *
     * <p>{@code flat} 生成器生成一个区块只填 24 层，很便宜；这里同步
     * {@code getChunk(cx, cz, FULL, true)}，保证随后 {@link Site#set} 的
     * {@code isLoaded} 判据不会把格子跳过（v1 就是全被跳过，等于没建）。</p>
     */
    private static void preloadRing(Site s) {
        long start = System.currentTimeMillis();
        int baseCx = s.cx >> 4;
        int baseCz = s.cz >> 4;
        int span = (PRELOAD_OUTER >> 4) + 1;
        int loaded = 0;
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                int x0 = (baseCx + dx) * 16 - s.cx;
                int x1 = x0 + 15;
                int z0 = (baseCz + dz) * 16 - s.cz;
                int z1 = z0 + 15;
                int nearX = (x0 <= 0 && 0 <= x1) ? 0 : Math.min(Math.abs(x0), Math.abs(x1));
                int nearZ = (z0 <= 0 && 0 <= z1) ? 0 : Math.min(Math.abs(z0), Math.abs(z1));
                int farX = Math.max(Math.abs(x0), Math.abs(x1));
                int farZ = Math.max(Math.abs(z0), Math.abs(z1));
                long near2 = (long) nearX * nearX + (long) nearZ * nearZ;
                long far2 = (long) farX * farX + (long) farZ * farZ;
                if (near2 > (long) PRELOAD_OUTER * PRELOAD_OUTER) {
                    continue;                   // 整块都在环带外
                }
                if (far2 < (long) PRELOAD_INNER * PRELOAD_INNER) {
                    continue;                   // 整块都在环带内（环内不需要写）
                }
                s.level.getChunk(baseCx + dx, baseCz + dz, ChunkStatus.FULL, true);
                loaded++;
            }
        }
        HongmengMod.LOGGER.info("[arena] theme preload {} chunks in {} ms",
                loaded, System.currentTimeMillis() - start);
    }

    // ==================================================================
    // 纪元：机关天宫（日晷 + 齿轮 + 三重圜丘 + 残柱列）
    // ==================================================================

    private static void epochHall(Site s) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState tiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        BlockState quartzPillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState copper = Blocks.WAXED_WEATHERED_COPPER.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();

        // ---------- ① 三重圜丘台阶：r=124/136/148，每重 6 宽、逐重升 1 格 ----------
        // 实心砌到地面（不悬空）：第 k 重高 k 格，顶面铺平滑石英，外沿深板岩瓦、内沿雕纹石英。
        int[][] tiers = {{124, 1}, {136, 2}, {148, 3}};
        for (int[] tier : tiers) {
            int rMin = tier[0];
            int h = tier[1];
            int rMax = rMin + 5;
            int min2 = rMin * rMin;
            int max2 = rMax * rMax;
            int edgeOut2 = (rMax - 1) * (rMax - 1);
            int edgeIn2 = (rMin + 1) * (rMin + 1);
            for (int dy = 0; dy < h; dy++) {
                boolean top = dy == h - 1;
                for (int dx = -rMax; dx <= rMax; dx++) {
                    for (int dz = -rMax; dz <= rMax; dz++) {
                        int d2 = dx * dx + dz * dz;
                        if (d2 < min2 || d2 > max2) {
                            continue;
                        }
                        BlockState st;
                        if (!top) {
                            st = tiles;                          // 台阶侧面/内芯
                        } else if (d2 > edgeOut2) {
                            st = tiles;                          // 外沿镶边
                        } else if (d2 < edgeIn2) {
                            st = chiseled;                       // 内沿雕纹石英
                        } else {
                            st = quartz;                         // 顶面平滑石英
                        }
                        s.set(dx, dy, dz, st);
                    }
                }
            }
        }

        // ---------- ② 日晷（r≈150，15 宽圆台）：12 时辰金刻 + 深板岩晷针 + 黑石日影 ----------
        int sx = 0;
        int sz = -150;
        int top = 4;                                        // 圆台顶面相对地高 4（高出第三重 1 格）
        for (int dy = 0; dy <= top; dy++) {
            disc(s, sx, sz, 7, dy, dy == top ? quartz : tiles);
        }
        // 12 时辰刻度：半径 7 的圆上 12 颗金块
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0D * i / 12.0D;
            s.set(sx + coord(Math.cos(a) * 7.0D), top, sz + coord(Math.sin(a) * 7.0D), gold);
        }
        // 晷针：中心深板岩柱（5 高）
        column(s, sx, sz, top + 1, top + 5, deepslate);
        // 日影：从晷针根部朝 +X 的一条黑石线（示意"影子落在刻度盘上"）
        for (int t = 1; t <= 5; t++) {
            s.set(sx + t, top, sz, blackstone);
        }

        // ---------- ③ 8 座齿轮柱：5~9 高石柱 + 11 宽铜齿轮（金轴心 + 8 颗轮齿）----------
        for (int g = 0; g < 8; g++) {
            double a = Math.PI * 2.0D * g / 8.0D + Math.PI / 8.0D;
            int px = coord(Math.cos(a) * 132.0D);
            int pz = coord(Math.sin(a) * 132.0D);
            int h = 5 + s.rnd.nextInt(5);                    // 石柱 5~9
            // 齿轮盘法线朝竞技场中心（取最近的正轴向）；盘厚 2，柱身也做成 2 厚，
            // 这样轮盘底部的两块正好压在柱头两块上（不会出现"轮盘悬在柱边"）。
            int axis = Math.abs(Math.cos(a)) >= Math.abs(Math.sin(a)) ? 0 : 1;
            int nx = axis == 0 ? 1 : 0;
            int nz = axis == 1 ? 1 : 0;
            for (int dy = 0; dy < h; dy++) {
                if (dy < 2) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            s.set(px + dx, dy, pz + dz, tiles);      // 3×3 柱础
                        }
                    }
                } else {
                    boolean cap = dy == h - 1;
                    s.set(px, dy, pz, cap ? chiseled : tiles);       // 柱身 2 厚
                    s.set(px + nx, dy, pz + nz, cap ? chiseled : tiles);
                }
            }
            // 齿轮盘：直径 11、厚 2，压在柱顶上
            int gearY = h + 5;
            verticalDisc(s, px, pz, gearY, 5, 2, axis, copper, gold);
            gearTeeth(s, px, pz, gearY, 5, axis, copper);
        }

        // ---------- ④ 2 段弧形残柱列：r=160，各 6 根 / 共 12 根高度 4~14 的石英残柱 ----------
        double[][] arcs = {
                {Math.toRadians(20.0D), Math.toRadians(80.0D)},
                {Math.toRadians(200.0D), Math.toRadians(260.0D)},
        };
        for (double[] arc : arcs) {
            for (int i = 0; i < 6; i++) {                    // 2 段 × 6 根 = 12 根
                double a = arc[0] + (arc[1] - arc[0]) * i / 5.0D;
                int px = coord(Math.cos(a) * 160.0D);
                int pz = coord(Math.sin(a) * 160.0D);
                int h = 4 + s.rnd.nextInt(11);                // 4~14（高低不一 = 残柱）
                s.set(px, 0, pz, chiseled);                   // 柱础
                column(s, px, pz, 1, h, quartzPillar);        // 柱身
                if (s.rnd.nextInt(3) == 0) {
                    // 断口：柱顶再歪一格雕纹石英，读作"断裂"
                    s.set(px + (s.rnd.nextBoolean() ? 1 : -1), h + 1, pz, chiseled);
                }
            }
        }

        // ---------- ⑤ 散落铜块 / 青苔块（只撒在台阶内侧与最外圈，别弄脏圜丘顶面）----------
        for (int i = 0; i < 120; i++) {
            double a = s.rnd.nextDouble() * Math.PI * 2.0D;
            int r = (i % 2 == 0)
                    ? INNER + s.rnd.nextInt(4)                    // 120~123（第一重内侧）
                    : 154 + s.rnd.nextInt(OUTER - 154 + 1);      // 154~168（第三重外侧）
            int px = coord(Math.cos(a) * r);
            int pz = coord(Math.sin(a) * r);
            BlockState st = s.rnd.nextInt(3) == 0 ? mossy : copper;
            s.set(px, 0, pz, st);
            if (s.rnd.nextInt(4) == 0) {
                s.set(px, 1, pz, st);                          // 少量叠块，做"散落零件"的体量
            }
        }
    }

    // ==================================================================
    // 妖兽：骨原（巨骨拱 + 兽巢 + 图腾 + 枯树/乱石/骨堆 + 篝火）
    // ==================================================================

    private static void beastBoneyard(Site s) {
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        BlockState deepBrick = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();
        BlockState mud = Blocks.PACKED_MUD.defaultBlockState();
        BlockState dirt = Blocks.COARSE_DIRT.defaultBlockState();
        BlockState mossyStone = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState emerald = Blocks.EMERALD_BLOCK.defaultBlockState();
        BlockState campfire = Blocks.CAMPFIRE.defaultBlockState();
        BlockState deadBush = Blocks.DEAD_BUSH.defaultBlockState();

        // ---------- ① 6 座巨骨拱：跨度 20、高 14，r≈136 均布，一端 5×5 颅骨 ----------
        for (int k = 0; k < 6; k++) {
            double a = Math.PI * 2.0D * k / 6.0D + Math.PI / 6.0D;
            int px = coord(Math.cos(a) * 136.0D);
            int pz = coord(Math.sin(a) * 136.0D);
            double tx = -Math.sin(a);                        // 切向（拱跨方向）
            double tz = Math.cos(a);
            double nx = Math.cos(a);                         // 径向（拱圈厚度方向）
            double nz = Math.sin(a);
            // 拱圈：21 列 × 半椭圆高度，厚度 2（骨块从地面砌起，绝不悬空）
            for (int t = -10; t <= 10; t++) {
                double f = 1.0D - (double) (t * t) / 100.0D;
                int h = coord(14.0D * Math.sqrt(Math.max(0.0D, f)));
                for (int thick = 0; thick < 2; thick++) {
                    int x = px + coord(t * tx + thick * nx);
                    int z = pz + coord(t * tz + thick * nz);
                    column(s, x, z, 0, h, bone);
                }
            }
            // 脊椎：拱背每隔 2 列向外凸 1 块
            for (int t = -8; t <= 8; t += 2) {
                double f = 1.0D - (double) (t * t) / 100.0D;
                int h = coord(14.0D * Math.sqrt(Math.max(0.0D, f)));
                int x = px + coord(t * tx + 2.0D * nx);
                int z = pz + coord(t * tz + 2.0D * nz);
                s.set(x, h + 1, z, bone);
            }
            // 一端颅骨：5 宽 × 5 高 × 2 厚的骨墙，挖一对眼窝 + 一张口（读作兽颅）
            for (int u = -2; u <= 2; u++) {
                for (int v = 0; v <= 4; v++) {
                    boolean eye = v == 3 && Math.abs(u) == 1;
                    boolean mouth = v == 1 && Math.abs(u) <= 1;
                    if (eye || mouth) {
                        continue;
                    }
                    for (int thick = 0; thick < 2; thick++) {
                        int x = px + coord((u - 10) * tx + thick * nx);
                        int z = pz + coord((u - 10) * tz + thick * nz);
                        s.set(x, v, z, bone);
                    }
                }
            }
        }

        // ---------- ② 8 处兽巢：粗泥/干草/压实泥围合 + 骨块 ----------
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0D * i / 8.0D + 0.2D;
            int r = 128 + s.rnd.nextInt(33);                 // 128~160
            int px = coord(Math.cos(a) * r);
            int pz = coord(Math.sin(a) * r);
            int rad = 5 + s.rnd.nextInt(3);                  // 巢半径 5~7
            for (int dx = -rad - 1; dx <= rad + 1; dx++) {
                for (int dz = -rad - 1; dz <= rad + 1; dz++) {
                    int d2 = dx * dx + dz * dz;
                    int d = coord(Math.sqrt((double) d2));
                    if (d > rad + 1) {
                        continue;
                    }
                    BlockState st = switch (s.rnd.nextInt(4)) {
                        case 0 -> hay;
                        case 1 -> mud;
                        default -> dirt;
                    };
                    if (d >= rad) {
                        s.set(px + dx, 0, pz + dz, st);      // 围合圈
                        if (d == rad) {
                            s.set(px + dx, 1, pz + dz, st);  // 围栏高 2
                        }
                    } else {
                        s.set(px + dx, 0, pz + dz, hay);     // 巢内干草窝
                    }
                }
            }
            // 巢内骨块（猎物残骸）+ 一根兽栏立柱
            int bones = 3 + s.rnd.nextInt(4);
            for (int b = 0; b < bones; b++) {
                int dx = s.rnd.nextInt(rad * 2 + 1) - rad;
                int dz = s.rnd.nextInt(rad * 2 + 1) - rad;
                s.set(px + dx, 1, pz + dz, bone);
            }
            int posterX = px + (s.rnd.nextBoolean() ? rad - 1 : 1 - rad);
            column(s, posterX, pz, 1, 3, log);
            s.set(posterX, 4, pz, bone);
        }

        // ---------- ③ 4 根图腾：云杉原木 + 骨块分段 + 绿宝石顶 ----------
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2.0D * i + Math.PI / 4.0D;
            int px = coord(Math.cos(a) * 145.0D);
            int pz = coord(Math.sin(a) * 145.0D);
            int h = 10 + s.rnd.nextInt(5);                   // 高 10~14
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    s.set(px + dx, 0, pz + dz, mud);         // 压实泥基座
                }
            }
            column(s, px, pz, 1, h, log);
            for (int dy = 2; dy <= h; dy += 4) {
                s.set(px, dy, pz, bone);                     // 骨环分段
            }
            s.set(px, h + 1, pz, emerald);                   // 绿宝石顶
            // 2/3 高处的四向"骨臂"
            int armY = Math.max(3, h * 2 / 3);
            for (int d = 0; d < 4; d++) {
                int ax = d == 0 ? 1 : (d == 1 ? -1 : 0);
                int az = d == 2 ? 1 : (d == 3 ? -1 : 0);
                s.set(px + ax, armY, pz + az, bone);
                s.set(px + ax * 2, armY, pz + az * 2, bone);
            }
        }

        // ---------- ④ 40 棵枯树 / 乱石 / 骨堆（骨原的地面"语法"）----------
        for (int i = 0; i < 40; i++) {
            double a = s.rnd.nextDouble() * Math.PI * 2.0D;
            int r = INNER + 2 + s.rnd.nextInt(OUTER - INNER - 1);   // 122~168
            int px = coord(Math.cos(a) * r);
            int pz = coord(Math.sin(a) * r);
            switch (s.rnd.nextInt(3)) {
                case 0 -> {
                    // 枯树：歪斜的原木树干 + 2 根枯枝 + 干枯灌木
                    int h = 3 + s.rnd.nextInt(4);
                    for (int dy = 0; dy < h; dy++) {
                        s.set(px + (dy >= h - 2 ? 1 : 0), dy, pz, log);
                    }
                    s.set(px + 1, h - 1, pz + 1, log);
                    s.set(px + 2, h, pz + 1, log);
                    s.set(px, 0, pz + 1, deadBush);
                }
                case 1 -> {
                    // 乱石：苔石砖 + 深板岩砖的 3×3 小丘
                    int h = 1 + s.rnd.nextInt(2);
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (s.rnd.nextInt(4) == 0) {
                                continue;
                            }
                            BlockState st = s.rnd.nextInt(3) == 0 ? deepBrick : mossyStone;
                            column(s, px + dx, pz + dz, 0, h - 1, st);
                        }
                    }
                    s.set(px, h, pz, mossyStone);
                }
                default -> {
                    // 骨堆：2~6 块骨块堆
                    int n = 2 + s.rnd.nextInt(5);
                    for (int b = 0; b < n; b++) {
                        int dx = s.rnd.nextInt(3) - 1;
                        int dz = s.rnd.nextInt(3) - 1;
                        s.set(px + dx, b / 3, pz + dz, bone);
                    }
                }
            }
        }
        // 骨原的地面骨屑（稀疏单块，不堆体量）
        for (int i = 0; i < 160; i++) {
            double a = s.rnd.nextDouble() * Math.PI * 2.0D;
            int r = INNER + s.rnd.nextInt(OUTER - INNER + 1);
            int px = coord(Math.cos(a) * r);
            int pz = coord(Math.sin(a) * r);
            s.set(px, 0, pz, s.rnd.nextInt(3) == 0 ? deepBrick : bone);
        }

        // ---------- ⑤ 2 处篝火（狩猎营地）----------
        for (int i = 0; i < 2; i++) {
            double a = Math.PI * i + 0.9D;
            int px = coord(Math.cos(a) * 156.0D);
            int pz = coord(Math.sin(a) * 156.0D);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    s.set(px + dx, 0, pz + dz, dirt);
                }
            }
            s.set(px + 1, 1, pz, log);                       // 柴堆
            s.set(px - 1, 1, pz, log);
            s.light(px, 1, pz, campfire);                    // 篝火（光源计入预算）
            column(s, px, pz + 2, 1, 3, log);                // 营地立柱
            s.set(px, 4, pz + 2, bone);
        }
    }

    // ==================================================================
    // 魔道：血狱（熔岩湖 + 黑石尖峰 + 血池祭坛 + 火盆）
    // ==================================================================

    private static void demonHell(Site s) {
        BlockState black = Blocks.BLACKSTONE.defaultBlockState();
        BlockState basalt = Blocks.BASALT.defaultBlockState();
        BlockState tuff = Blocks.TUFF.defaultBlockState();
        BlockState bricks = Blocks.RED_NETHER_BRICKS.defaultBlockState();
        BlockState hyphae = Blocks.CRIMSON_HYPHAE.defaultBlockState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState wart = Blocks.NETHER_WART_BLOCK.defaultBlockState();
        BlockState redConcrete = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState soulLantern = Blocks.SOUL_LANTERN.defaultBlockState();
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        BlockState polished = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState soulCampfire = Blocks.SOUL_CAMPFIRE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();

        // ---------- ① 6 片熔岩湖：r=132~156，碗形挖深 4~6，湖底 2 层岩浆 ----------
        int lakes = 6;
        for (int i = 0; i < lakes; i++) {
            double a = Math.PI * 2.0D * i / lakes + 0.25D;
            int r = 132 + s.rnd.nextInt(25);                 // 132~156
            int rad = 9 + s.rnd.nextInt(6);                  // 半径 9~14
            if (r < rad + 122) {
                // 硬约束：**所有岩浆必须距中心 > 120**（湖沿再留 2 格余量）——
                // 湖心最靠内时（r=132、rad=14）内沿会掉到 118，这里往外推。
                r = rad + 122;
            }
            int px = coord(Math.cos(a) * r);
            int pz = coord(Math.sin(a) * r);
            int depth = 4 + s.rnd.nextInt(3);                // 挖深 4~6
            int rad2 = rad * rad;
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > rad2) {
                        continue;
                    }
                    // 碗形：越靠中心越深（4~6），湖沿浅
                    int d = depth - coord((double) (depth - 2) * d2 / (double) rad2);
                    if (d < 2) {
                        d = 2;
                    }
                    // 挖掉地面以下 1..d 层（不动地面以上的东西）
                    for (int dy = -1; dy >= -d; dy--) {
                        s.set(px + dx, dy, pz + dz, air);
                    }
                    // 湖底 2 层岩浆（最深处 dy=-d 与 dy=-d+1）
                    s.set(px + dx, -d, pz + dz, lava);
                    s.set(px + dx, -d + 1, pz + dz, lava);
                }
            }
            // 湖沿：黑石/玄武岩围一圈（地面 1 层 + 稀疏 1 层凸起）
            for (int t = 0; t < 360; t += 3) {
                double ra = Math.toRadians(t);
                int x = px + coord(Math.cos(ra) * (rad + 1.0D));
                int z = pz + coord(Math.sin(ra) * (rad + 1.0D));
                s.set(x, 0, z, t % 9 == 0 ? basalt : black);
                if (t % 18 == 0) {
                    s.set(x, 1, z, t % 36 == 0 ? basalt : black);
                }
            }
            // 第一片湖加一道"熔岩瀑布"：湖边黑石导流台 + 2 格岩浆柱落进湖里
            if (i == 0) {
                lavaFall(s, px, pz, rad, a, black, lava, air);
            }
        }

        // ---------- ② 2 组黑石尖峰：每组 15~25 根，高 12~28，越高越细 ----------
        double[][] spikeGroups = {
                {Math.toRadians(30.0D), 146.0D},
                {Math.toRadians(210.0D), 146.0D},
        };
        for (double[] grp : spikeGroups) {
            int count = 15 + s.rnd.nextInt(11);              // 15~25 根
            for (int i = 0; i < count; i++) {
                double a = grp[0] + (s.rnd.nextDouble() - 0.5D) * Math.toRadians(38.0D);
                int r = (int) grp[1] + s.rnd.nextInt(35) - 17;
                int px = coord(Math.cos(a) * r);
                int pz = coord(Math.sin(a) * r);
                int h = 12 + s.rnd.nextInt(17);              // 12~28
                // 越高越细：很高的尖峰用 3 宽基座，其余 5 宽
                int baseHalf = h >= 24 ? 1 : 2;
                BlockState[] palette = {black, black, tuff, basalt};
                for (int dy = 0; dy < h; dy++) {
                    double frac = (double) dy / (double) h;
                    int half = (int) Math.floor(baseHalf * (1.0D - frac * 1.35D));
                    if (half < 0) {
                        half = 0;
                    }
                    for (int dx = -half; dx <= half; dx++) {
                        for (int dz = -half; dz <= half; dz++) {
                            s.set(px + dx, dy, pz + dz,
                                    palette[s.rnd.nextInt(palette.length)]);
                        }
                    }
                }
            }
        }

        // ---------- ③ 血池祭坛（r≈150）：三重八角黑石坛 + 绯红柱 + 门式拱架锁链 + 血池 ----------
        int ax = 0;
        int az = -150;
        // 三重八角坛：半径 12 → 9 → 6，逐重升 1 格；坛面黑石/地狱砖镶边
        int[] radii = {12, 9, 6};
        for (int tier = 0; tier < 3; tier++) {
            int rad = radii[tier];
            octagon(s, ax, az, rad, tier, tier == 2 ? gilded : polished);
            octagonRim(s, ax, az, rad, tier, tier == 0 ? black : bricks);
        }
        // 8 根绯红菌柄柱（坛外缘）+ 红地狱砖柱头 + 灵魂灯笼
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0D * i / 8.0D + Math.PI / 8.0D;
            int px = ax + coord(Math.cos(a) * 10.0D);
            int pz = az + coord(Math.sin(a) * 10.0D);
            int h = 4 + s.rnd.nextInt(4);                    // 4~7
            column(s, px, pz, 1, h, hyphae);
            s.set(px, h + 1, pz, bricks);
            s.light(px, h + 2, pz, soulLantern);              // 灵魂灯笼
        }
        // 血池：八角坛顶面中心的血红池（nether_wart_block）+ 红混凝土池沿
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 9) {
                    continue;
                }
                s.set(ax + dx, 3, az + dz, d2 > 4 ? redConcrete : wart);
            }
        }
        // 门式拱架：两根绯红柱 + 红地狱砖横梁，锁链垂下挂灵魂灯笼
        int gx = ax + 14;
        for (int side = -1; side <= 1; side += 2) {
            column(s, gx, az + side * 4, 0, 8, hyphae);
            s.set(gx, 9, az + side * 4, bricks);
        }
        for (int dz = -4; dz <= 4; dz++) {
            s.set(gx, 9, az + dz, bricks);
        }
        for (int dz = -3; dz <= 3; dz += 2) {
            for (int dy = 8; dy >= 5; dy--) {
                s.set(gx, dy, az + dz, chain);               // 锁链
            }
            s.light(gx, 4, az + dz, soulLantern);            // 链底灯笼
        }
        // 火盆 6 座（祭坛外一圈）
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2.0D * i / 6.0D;
            int px = ax + coord(Math.cos(a) * 16.0D);
            int pz = az + coord(Math.sin(a) * 16.0D);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    s.set(px + dx, 0, pz + dz, black);
                }
            }
            s.light(px, 1, pz, soulCampfire);                // 火盆（灵魂火，光源计入预算）
        }
    }

    /**
     * 熔岩瀑布（静态外观）：湖边立一座黑石导流台，2 格岩浆从台口落进湖里。
     *
     * <p>只放 2 个岩浆源、三面黑石包夹，流向被限制在湖盆内；不会漫到环带地面。</p>
     */
    private static void lavaFall(Site s, int px, int pz, int rad, double a,
                                 BlockState black, BlockState lava, BlockState air) {
        int fx = px + coord(Math.cos(a) * (rad - 3.0D));
        int fz = pz + coord(Math.sin(a) * (rad - 3.0D));
        int backX = px + coord(Math.cos(a) * (rad - 1.0D));
        int backZ = pz + coord(Math.sin(a) * (rad - 1.0D));
        for (int dy = 0; dy <= 4; dy++) {
            s.set(backX, dy, backZ, black);                  // 背后的导流壁
        }
        // 两侧包夹（垂直瀑布方向），只留朝湖心的开口
        int perpX = coord(-Math.sin(a));
        int perpZ = coord(Math.cos(a));
        for (int dy = 3; dy <= 4; dy++) {
            s.set(fx + perpX, dy, fz + perpZ, black);
            s.set(fx - perpX, dy, fz - perpZ, black);
            s.set(fx, dy, fz, lava);                         // 瀑布主体（2 格）
        }
        for (int dy = 5; dy <= 6; dy++) {
            s.set(fx, dy, fz, air);                          // 落口上方留空
        }
    }

    // ==================================================================
    // 虚空：虚空王座（浮空岛 + 裂隙 + 紫晶柱 + 王座环）
    // ==================================================================

    private static void voidRift(Site s) {
        BlockState end = Blocks.END_STONE.defaultBlockState();
        BlockState endBrick = Blocks.END_STONE_BRICKS.defaultBlockState();
        BlockState purpur = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState purpurPillar = Blocks.PURPUR_PILLAR.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState rod = Blocks.END_ROD.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // ---------- ① 12 座浮空岛：r=125~165，高 +15~+45，小岛偏多，黑曜倒锥底 ----------
        int islands = 12;
        for (int i = 0; i < islands; i++) {
            double a = Math.PI * 2.0D * i / islands
                    + (s.rnd.nextDouble() - 0.5D) * 0.5D;
            int r = 125 + s.rnd.nextInt(41);                 // 125~165
            int lift = 15 + s.rnd.nextInt(31);               // +15~+45
            int rad = 4 + s.rnd.nextInt(6);                  // 4~9（小岛偏多）
            if (rad > 6 && s.rnd.nextInt(3) != 0) {
                rad = 4 + s.rnd.nextInt(3);
            }
            int px = coord(Math.cos(a) * r);
            int pz = coord(Math.sin(a) * r);
            boolean purpurTop = s.rnd.nextBoolean();
            // 岛体：顶面 + 2 层体
            disc(s, px, pz, rad, lift, purpurTop ? purpur : end);
            disc(s, px, pz, Math.max(1, rad - 1), lift - 1, end);
            disc(s, px, pz, Math.max(1, rad - 2), lift - 2, end);
            // 倒锥底：越往下越细，最后收成 1 块 → 没有单块悬空
            for (int k = 3; ; k++) {
                double frac = (double) (k - 2) / (double) (rad + 1);
                int rr = coord((rad - 2) * (1.0D - frac));
                if (rr <= 0) {
                    s.set(px, lift - k, pz, obsidian);       // 锥尖
                    break;
                }
                disc(s, px, pz, rr, lift - k, k % 2 == 0 ? obsidian : end);
            }
            // 岛面点缀：紫珀柱 / 紫水晶簇 / 末地烛
            int deco = s.rnd.nextInt(3);
            if (deco == 0) {
                column(s, px, pz, lift + 1, lift + 2, purpurPillar);
                s.set(px, lift + 3, pz, amethyst);
            } else if (deco == 1) {
                s.set(px, lift + 1, pz, amethyst);
                s.set(px + 1, lift + 1, pz, endBrick);
            } else {
                s.light(px, lift + 1, pz, rod);
            }
        }

        // ---------- ② 3 条径向裂隙：黑曜 2~4 宽 + 哭泣黑曜点，由内向外 ----------
        double[] riftAngles = {
                Math.toRadians(90.0D), Math.toRadians(210.0D), Math.toRadians(330.0D),
        };
        for (double ra : riftAngles) {
            double ux = Math.cos(ra);
            double uz = Math.sin(ra);
            int perpX = coord(-uz);
            int perpZ = coord(ux);
            for (int rr = 120; rr <= OUTER; rr++) {
                int cx = coord(ux * rr);
                int cz = coord(uz * rr);
                int width = 2 + s.rnd.nextInt(3);             // 2~4 宽
                int half = width / 2;
                for (int w = -half; w <= width - 1 - half; w++) {
                    int x = cx + perpX * w;
                    int z = cz + perpZ * w;
                    s.set(x, -1, z, air);                     // 挖开地表 1 层
                    s.set(x, -2, z, s.rnd.nextInt(6) == 0 ? crying : obsidian);
                    if (s.rnd.nextInt(5) == 0) {
                        s.set(x, -1, z, obsidian);            // 裂隙里翻出的黑曜碎块
                    }
                }
            }
        }

        // ---------- ③ 8 根紫晶柱：amethyst_block + 紫珀，高 10~20 ----------
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0D * i / 8.0D + 0.35D;
            int r = 128 + s.rnd.nextInt(35);                 // 128~162
            int px = coord(Math.cos(a) * r);
            int pz = coord(Math.sin(a) * r);
            int h = 10 + s.rnd.nextInt(11);                  // 10~20
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    s.set(px + dx, 0, pz + dz, i % 2 == 0 ? purpur : endBrick);
                }
            }
            for (int dy = 1; dy <= h; dy++) {
                s.set(px, dy, pz, dy % 5 == 0 ? purpurPillar : amethyst);
            }
            s.set(px, h + 1, pz, amethyst);
            // 柱顶四向紫珀小齿
            for (int d = 0; d < 4; d++) {
                int ax = d == 0 ? 1 : (d == 1 ? -1 : 0);
                int az = d == 2 ? 1 : (d == 3 ? -1 : 0);
                s.set(px + ax, h, pz + az, purpur);
            }
        }

        // ---------- ④ 巨型浮空王座环（r≈155）：黑曜连续环 + 紫珀台 + 紫水晶王座 + 末地烛 ----------
        int tx = 0;
        int tz = 154;
        int ty = 25;                                          // 悬浮 +25
        // 环体：外径 14 / 内径 11 的连续圆环，2 层厚（每个方块都与邻块相连，不是单块悬空）
        annulus(s, tx, tz, 11, 14, ty, obsidian);
        annulus(s, tx, tz, 11, 14, ty + 1, obsidian);
        // 4 条紫珀辐条：从内环连到中央紫珀台（保证台面不是孤立悬空）
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : dirs) {
            for (int d = 9; d <= 11; d++) {
                s.set(tx + dir[0] * d, ty, tz + dir[1] * d, purpur);
            }
        }
        // 中央紫珀台（直径 19）+ 一圈紫珀柱栏杆
        disc(s, tx, tz, 9, ty, purpur);
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2.0D * i / 16.0D;
            s.set(tx + coord(Math.cos(a) * 9.0D), ty + 1,
                    tz + coord(Math.sin(a) * 9.0D), purpurPillar);
        }
        // 紫水晶王座：座面 3×3 + 椅背 3 宽 2 高 + 紫珀扶手
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                s.set(tx + dx, ty + 1, tz + dz, amethyst);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            s.set(tx + dx, ty + 2, tz + 2, amethyst);
            s.set(tx + dx, ty + 3, tz + 2, amethyst);
        }
        s.set(tx - 1, ty + 2, tz, purpurPillar);
        s.set(tx + 1, ty + 2, tz, purpurPillar);
        // 4 根末地烛
        for (int[] dir : dirs) {
            int rx = tx + dir[0] * 5;
            int rz = tz + dir[1] * 5;
            s.set(rx, ty + 1, rz, purpur);
            s.light(rx, ty + 2, rz, rod);
        }

        // ---------- ⑤ 地面散落紫珀 / 末地石碎石 ----------
        for (int i = 0; i < 140; i++) {
            double a = s.rnd.nextDouble() * Math.PI * 2.0D;
            int r = INNER + s.rnd.nextInt(OUTER - INNER + 1);
            int px = coord(Math.cos(a) * r);
            int pz = coord(Math.sin(a) * r);
            BlockState st = switch (s.rnd.nextInt(4)) {
                case 0, 1 -> endBrick;
                case 2 -> purpur;
                default -> end;
            };
            s.set(px, 0, pz, st);
            if (s.rnd.nextInt(5) == 0) {
                s.set(px, 1, pz, st);
            }
        }
    }

    // ==================================================================
    // 几何积木
    // ==================================================================

    /** 竖直圆盘（"齿轮"），像素圆：半径 {@code rad}（5 → 直径 11）、厚度 2（沿法线正方向）。 */
    private static void verticalDisc(Site s, int ox, int oz, int dyCenter, int rad, int thick,
                                     int axis, BlockState body, BlockState hub) {
        int r2 = rad * rad;
        for (int u = -rad; u <= rad; u++) {
            for (int v = -rad; v <= rad; v++) {
                if (u * u + v * v > r2) {
                    continue;
                }
                boolean core = Math.abs(u) <= 1 && Math.abs(v) <= 1;   // 金轴心 3×3
                for (int t = 0; t < thick; t++) {
                    int dx = axis == 0 ? t : u;
                    int dz = axis == 0 ? u : t;
                    s.set(ox + dx, dyCenter + v, oz + dz, core ? hub : body);
                }
            }
        }
    }

    /** 齿轮的 8 颗轮齿（沿圆盘外弧的 8 个方向凸出 1 格）。 */
    private static void gearTeeth(Site s, int ox, int oz, int dyCenter, int rad, int axis,
                                  BlockState st) {
        int tooth = rad + 1;
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0D * i / 8.0D;
            int u = coord(Math.cos(a) * tooth);
            int v = coord(Math.sin(a) * tooth);
            int dx = axis == 0 ? 0 : u;
            int dz = axis == 0 ? u : 0;
            for (int t = 0; t < 2; t++) {
                s.set(ox + dx + (axis == 0 ? t : 0), dyCenter + v,
                        oz + dz + (axis == 0 ? 0 : t), st);
            }
        }
    }

    /** 圆盘（实心）。 */
    private static void disc(Site s, int ox, int oz, int rad, int dy, BlockState st) {
        if (rad < 0) {
            return;
        }
        int r2 = rad * rad;
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                if (dx * dx + dz * dz <= r2) {
                    s.set(ox + dx, dy, oz + dz, st);
                }
            }
        }
    }

    /** 圆环（实心环带，半径 {@code rMin}~{@code rMax}）。 */
    private static void annulus(Site s, int ox, int oz, int rMin, int rMax, int dy,
                                BlockState st) {
        int max2 = rMax * rMax;
        int min2 = rMin * rMin;
        for (int dx = -rMax; dx <= rMax; dx++) {
            for (int dz = -rMax; dz <= rMax; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 >= min2 && d2 <= max2) {
                    s.set(ox + dx, dy, oz + dz, st);
                }
            }
        }
    }

    /** 1×1 竖直柱子（{@code dyFrom}~{@code dyTo} 闭区间）。 */
    private static void column(Site s, int ox, int oz, int dyFrom, int dyTo, BlockState st) {
        for (int dy = dyFrom; dy <= dyTo; dy++) {
            s.set(ox, dy, oz, st);
        }
    }

    /** 正八边形（{@code |dx|+|dz| <= rad*1.5} 的切角方盘）。 */
    private static void octagon(Site s, int ox, int oz, int rad, int dy, BlockState st) {
        int cut = rad + rad / 2;
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= cut) {
                    s.set(ox + dx, dy, oz + dz, st);
                }
            }
        }
    }

    /** 正八边形最外 2 圈的镶边（在同一个坛面上换色，不额外悬空）。 */
    private static void octagonRim(Site s, int ox, int oz, int rad, int dy, BlockState st) {
        int cut = rad + rad / 2;
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                int manhattan = Math.abs(dx) + Math.abs(dz);
                if (manhattan >= cut - 1 && manhattan <= cut) {
                    s.set(ox + dx, dy, oz + dz, st);
                }
            }
        }
    }

    /** 四舍五入到方块坐标。 */
    private static int coord(double value) {
        return (int) Math.round(value);
    }

    // ==================================================================
    // 写方块：环带守卫 + isLoaded 守卫 + 预算统计
    // ==================================================================

    /**
     * 一次场景建造的"施工现场"：持有维度、中心、固定随机源与计数器。
     *
     * <p>所有写入都走 {@link #set}：</p>
     * <ul>
     *   <li>半径 {@value #SAFE_R} 以内、{@value #MAX_REACH} 以外一律拒写（计数）；</li>
     *   <li>区块没加载则跳过（计数，正常应为 0 —— 预加载保证了这一点）；</li>
     *   <li>{@code setBlock(state, 2)}：flag=2 只做网络同步，不触发邻居更新，
     *       几千格也不会卡服。</li>
     * </ul>
     */
    private static final class Site {
        final ServerLevel level;
        final int cx;
        final int cz;
        /** 竞技场中心的"站人高度"（地面方块在 {@code y0 - 1}）。 */
        final int y0;
        /** 固定种子随机源（确定性）。 */
        final RandomSource rnd;
        int placed;
        int skipped;
        int rejected;
        int lights;

        Site(ServerLevel level, Dungeon dungeon, BlockPos center) {
            this.level = level;
            this.cx = center.getX();
            this.cz = center.getZ();
            this.y0 = center.getY();
            this.rnd = RandomSource.create(SEED + dungeon.ordinal() * 101L);
        }

        /** 相对竞技场中心的偏移写入（dy 相对"站人高度"）。 */
        void set(int dx, int dy, int dz, BlockState st) {
            setAt(cx + dx, y0 + dy, cz + dz, st);
        }

        /** 世界坐标写入（带全部守卫）。 */
        void setAt(int x, int y, int z, BlockState st) {
            long rx = (long) x - cx;
            long rz = (long) z - cz;
            long r2 = rx * rx + rz * rz;
            // 硬安全边界：半径 100 内一格都不许写（场地/看台/四门/祭坛必须干净）
            if (r2 < (long) SAFE_R * SAFE_R || r2 > (long) MAX_REACH * MAX_REACH) {
                rejected++;
                return;
            }
            BlockPos p = new BlockPos(x, y, z);
            if (!level.isLoaded(p)) {
                skipped++;
                return;
            }
            if (placed >= BLOCK_BUDGET) {
                throw new IllegalStateException("主题环 setBlock 超出预算 " + BLOCK_BUDGET);
            }
            if (level.setBlock(p, st, 2)) {
                placed++;
            }
        }

        /** 记一盏光源（灯笼/末地烛/火盆），超出光源预算就不再放。 */
        void light(int dx, int dy, int dz, BlockState st) {
            lights++;
            if (lights > LIGHT_BUDGET) {
                return;
            }
            set(dx, dy, dz, st);
        }
    }
}
