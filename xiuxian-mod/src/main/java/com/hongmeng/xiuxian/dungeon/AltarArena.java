package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>祭坛 boss 的独立副本场地</b>（玩家选的是「独立副本」，不是就地生成建筑）。
 *
 * <h2>为什么放在副本维度</h2>
 * <p>就地生成会把建筑塞进玩家自己的存档里 —— 毁房子、卡地形、没法回收。
 * 副本维度是<b>临时战场</b>，随便改都不会伤到主世界。</p>
 *
 * <h2>每个 boss 一套主题</h2>
 * <table border="1">
 *   <caption>主题配色</caption>
 *   <tr><th>Boss</th><th>地板</th><th>立柱</th><th>点缀</th></tr>
 *   <tr><td>傀儡祖师</td><td>深板岩砖</td><td>铜块</td><td>青金石</td></tr>
 *   <tr><td>万兽之主</td><td>苔石砖</td><td>骨块</td><td>橡木原木</td></tr>
 *   <tr><td>血魔老祖</td><td>红地狱砖</td><td>黑石</td><td>下界疣块</td></tr>
 *   <tr><td>虚空道主</td><td>末地石</td><td>黑曜石</td><td>紫珀块</td></tr>
 * </table>
 *
 * <h2>幂等</h2>
 * <p>中心放一块<b>主题标志块</b>；已经是他就跳过 —— 反复召唤同一只 boss
 * 不会把整个区域重刷一遍（那会卡服）。</p>
 */
public final class AltarArena {

    /** 场地半径（格）。比副本主竞技场（44）小一圈，是"小型独立房间"。 */
    public static final int RADIUS = 26;

    /** 外圈墙高。 */
    public static final int WALL_H = 3;

    /** 立柱高度。 */
    public static final int PILLAR_H = 9;

    /** 立柱数量。 */
    public static final int PILLARS = 12;

    /** 距副本原点小于这个距离就不建（避免覆盖主竞技场）。 */
    public static final double KEEP_OUT = 56.0D;

    /** 主题。 */
    public enum Theme {
        /** 傀儡祖师：机关。 */
        EPOCH,
        /** 万兽之主：兽骨。 */
        BEAST,
        /** 血魔老祖：血与火。 */
        DEMON,
        /** 虚空道主：虚空。 */
        VOID;

        /** 地板。 */
        BlockState floor() {
            return switch (this) {
                case EPOCH -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                case BEAST -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                case DEMON -> Blocks.RED_NETHER_BRICKS.defaultBlockState();
                case VOID -> Blocks.END_STONE_BRICKS.defaultBlockState();
            };
        }

        /** 立柱。 */
        BlockState pillar() {
            return switch (this) {
                case EPOCH -> Blocks.COPPER_BLOCK.defaultBlockState();
                case BEAST -> Blocks.BONE_BLOCK.defaultBlockState();
                case DEMON -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                case VOID -> Blocks.OBSIDIAN.defaultBlockState();
            };
        }

        /** 点缀（外圈墙 / 花纹）。 */
        BlockState accent() {
            return switch (this) {
                case EPOCH -> Blocks.LAPIS_BLOCK.defaultBlockState();
                case BEAST -> Blocks.OAK_LOG.defaultBlockState();
                case DEMON -> Blocks.NETHER_WART_BLOCK.defaultBlockState();
                case VOID -> Blocks.PURPUR_BLOCK.defaultBlockState();
            };
        }

        /** 中心标志块（幂等判据）。 */
        BlockState marker() {
            return switch (this) {
                case EPOCH -> Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                case BEAST -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
                case DEMON -> Blocks.CHISELED_NETHER_BRICKS.defaultBlockState();
                case VOID -> Blocks.PURPUR_PILLAR.defaultBlockState();
            };
        }

        /** 火盆。 */
        BlockState brazier() {
            return switch (this) {
                case EPOCH -> Blocks.SEA_LANTERN.defaultBlockState();
                case BEAST -> Blocks.CAMPFIRE.defaultBlockState();
                case DEMON -> Blocks.FIRE.defaultBlockState();
                case VOID -> Blocks.SOUL_CAMPFIRE.defaultBlockState();
            };
        }

        // ---- 2026-11-02 内容层（宝箱 / 宝座 / 陈设 / 旗帜）----

        /** 主题 → 对应副本（决定战利品表与宝箱档位）。 */
        Dungeon dungeon() {
            return switch (this) {
                case EPOCH -> Dungeon.EPOCH;
                case BEAST -> Dungeon.BEAST;
                case DEMON -> Dungeon.DEMON;
                case VOID -> Dungeon.VOID;
            };
        }

        /** 入口两侧旗帜。 */
        BlockState banner() {
            return switch (this) {
                case EPOCH -> Blocks.WHITE_BANNER.defaultBlockState();
                case BEAST -> Blocks.BROWN_BANNER.defaultBlockState();
                case DEMON -> Blocks.RED_BANNER.defaultBlockState();
                case VOID -> Blocks.PURPLE_BANNER.defaultBlockState();
            };
        }

        /** 宝座坐面 / 高背。 */
        BlockState throne() {
            return switch (this) {
                case EPOCH -> Blocks.GOLD_BLOCK.defaultBlockState();
                case BEAST -> Blocks.BONE_BLOCK.defaultBlockState();
                case DEMON -> Blocks.GILDED_BLACKSTONE.defaultBlockState();
                case VOID -> Blocks.PURPUR_BLOCK.defaultBlockState();
            };
        }

        /** 宝座扶手（台阶）。 */
        BlockState throneStair() {
            return switch (this) {
                case EPOCH -> Blocks.QUARTZ_STAIRS.defaultBlockState();
                case BEAST -> Blocks.STONE_BRICK_STAIRS.defaultBlockState();
                case DEMON -> Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState();
                case VOID -> Blocks.END_STONE_BRICK_STAIRS.defaultBlockState();
            };
        }

        /** 藏宝堆主方块。 */
        BlockState treasure() {
            return switch (this) {
                case EPOCH -> Blocks.GOLD_BLOCK.defaultBlockState();
                case BEAST -> Blocks.HAY_BLOCK.defaultBlockState();
                case DEMON -> Blocks.GILDED_BLACKSTONE.defaultBlockState();
                case VOID -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            };
        }
    }

    private AltarArena() {
    }

    /**
     * 在 {@code center} 建（或跳过）一座祭坛场地。
     *
     * <p><b>旧存档兼容</b>：房间本体（中心标志块）已存在时不再重铺地板/墙，
     * 只补一次“宝箱 + 陈设”——幂等判据是宝箱是否已放（{@link #detail} 返回 0 即已补过）。
     * 这样老世界不用重开也能拿到奖励箱，且不会覆盖玩家在房间里的改动。</p>
     *
     * @return 是否新建了房间本体（旧存档只补内容时返回 false）
     */
    public static boolean ensure(ServerLevel level, BlockPos center, Theme theme) {
        if (level == null || center == null || theme == null) {
            return false;
        }
        // 不覆盖副本主竞技场
        double dx = center.getX() - 0;
        double dz = center.getZ() - 0;
        if (dx * dx + dz * dz < KEEP_OUT * KEEP_OUT) {
            return false;
        }
        BlockPos floorY = center.below();
        if (level.getBlockState(floorY).is(theme.marker().getBlock())) {
            // 旧存档：房间已建过 → 只补宝箱/陈设（不重铺地板/墙）
            int added = detail(level, floorY, theme);
            if (added > 0) {
                HongmengMod.LOGGER.info(
                        "[altar] {} 小祭坛房间旧存档补建：宝箱/陈设 {} 格 @{}",
                        theme, added, floorY);
            }
            return false;
        }
        build(level, floorY, theme);
        return true;
    }

    /** 真正的建造。 */
    private static void build(ServerLevel level, BlockPos c, Theme theme) {
        BlockState floor = theme.floor();
        BlockState pillar = theme.pillar();
        BlockState accent = theme.accent();
        BlockState treasure = theme.treasure();
        int y = c.getY();

        // ---- 地板圆盘（同时清掉上方 6 格，避免被埋）----
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                int d2 = x * x + z * z;
                if (d2 > RADIUS * RADIUS) {
                    continue;
                }
                BlockPos p = new BlockPos(c.getX() + x, y, c.getZ() + z);
                // 中心花纹：同心环用 accent；2026-11-02 加一圈「藏宝色」同心环
                boolean ring = nearRing(d2, RADIUS);
                BlockState top = isRingAt(d2, RADIUS / 2) ? treasure
                        : (ring ? accent : floor);
                level.setBlock(p, top, 2);
                // 清空上方
                for (int h = 1; h <= PILLAR_H + 2; h++) {
                    level.setBlock(p.above(h), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // ---- 外圈墙（3 高，隔一格用 accent）----
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                int d2 = x * x + z * z;
                if (d2 <= (RADIUS - 2) * (RADIUS - 2) || d2 > RADIUS * RADIUS) {
                    continue;
                }
                BlockState w = (((x + z) & 3) == 0) ? accent : pillar;
                for (int h = 1; h <= WALL_H; h++) {
                    level.setBlock(new BlockPos(c.getX() + x, y + h, c.getZ() + z),
                            w, 2);
                }
            }
        }

        // ---- 入口：朝副本原点方向开 3 宽门洞 + 2 格门廊（门楣保留外墙整体性）----
        carveEntrance(level, c, theme, y);

        // ---- 12 根立柱 ----
        int pr = RADIUS - 5;
        for (int i = 0; i < PILLARS; i++) {
            double a = 2.0D * Math.PI * i / PILLARS;
            int px = (int) Math.round(Math.cos(a) * pr);
            int pz = (int) Math.round(Math.sin(a) * pr);
            BlockPos base = new BlockPos(c.getX() + px, y, c.getZ() + pz);
            for (int h = 1; h <= PILLAR_H; h++) {
                level.setBlock(base.above(h),
                        (h == PILLAR_H) ? accent : pillar, 2);
            }
            // 柱顶火盆
            level.setBlock(base.above(PILLAR_H + 1), theme.brazier(), 2);
        }

        // ---- 中央祭坛（5×5 三级台阶）----
        for (int step = 0; step < 3; step++) {
            int half = 2 - step;
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    level.setBlock(new BlockPos(c.getX() + x, y + 1 + step,
                            c.getZ() + z), step == 2 ? accent : pillar, 2);
                }
            }
        }
        // 中心标志块（幂等判据）
        level.setBlock(c, theme.marker(), 2);
        level.setBlock(c.above(4), Blocks.AIR.defaultBlockState(), 2);

        // ---- 内容层：2 宝箱 + 2 宝座 + 8 处主题陈设 + 入口旗帜 ----
        detail(level, c, theme);
    }

    // ==================================================================
    // 2026-11-02 内容层：奖励箱 + 主题陈设 + 入口门廊
    // ==================================================================

    /**
     * 宝箱 + 陈设（新档/旧档共用）。
     *
     * <p><b>幂等</b>：先放 2 个宝箱（{@link BossRewards#placeChests} 对已存在容器会跳过）。
     * 两个箱子一个都没新增，说明这个房间之前已经补过内容（或玩家自己放了容器）→
     * 直接跳过整段陈设，不重复写方块、不覆盖玩家改动。</p>
     *
     * <p>宝箱位置：中央祭坛左右两侧 ±(RADIUS-9) = ±17 格（避开 r=21 的立柱与 r≥24 的墙），
     * 站在地面方块上方一格（{@code y+1}）。</p>
     *
     * @return 本次实际写入的方块数（0 = 已补过，无写入）
     */
    private static int detail(ServerLevel level, BlockPos c, Theme theme) {
        int y = c.getY();
        int added = BossRewards.placeChests(level, c,
                new int[][]{
                        {RADIUS - 9, 1, 0},
                        {-(RADIUS - 9), 1, 0},
                },
                BossRewards.arenaLootFor(theme.dungeon()),
                0x414C5441L + theme.ordinal());
        if (added == 0) {
            return 0;
        }
        int placed = added;
        // 2 座主题宝座：祭坛前后（朝中心），3×3 台基 + 双层坐面 + 两侧台阶扶手
        placed += throne(level, theme, new BlockPos(c.getX(), y, c.getZ() + 12));
        placed += throne(level, theme, new BlockPos(c.getX(), y, c.getZ() - 12));
        // 8 处主题陈设：22.5°+45°×i、半径 15（绕开 12 根 30° 立柱与中央祭坛）
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(22.5D + i * 45.0D);
            int px = (int) Math.round(Math.cos(a) * 15.0D);
            int pz = (int) Math.round(Math.sin(a) * 15.0D);
            placed += trinket(level, theme,
                    new BlockPos(c.getX() + px, y, c.getZ() + pz), i);
        }
        // 入口两侧旗帜（入口朝向副本原点；旧档补建时旗帜落到室内同一侧，同样成立）
        int[] d = entranceDir(c);
        int perpX = d[1];
        int perpZ = -d[0];
        int px = d[0] * (RADIUS - 5);
        int pz = d[1] * (RADIUS - 5);
        placed += set(level, new BlockPos(c.getX() + px + perpX * 2, y + 1,
                c.getZ() + pz + perpZ * 2), theme.banner());
        placed += set(level, new BlockPos(c.getX() + px - perpX * 2, y + 1,
                c.getZ() + pz - perpZ * 2), theme.banner());
        return placed;
    }

    /** 一座主题宝座（3×3 台基 + 双层坐面 + 两侧台阶扶手）。 */
    private static int throne(ServerLevel level, Theme theme, BlockPos base) {
        int placed = 0;
        for (int ex = -1; ex <= 1; ex++) {
            for (int ez = -1; ez <= 1; ez++) {
                placed += set(level, base.offset(ex, 1, ez), theme.accent());
            }
        }
        placed += set(level, base.offset(0, 2, 0), theme.throne());
        placed += set(level, base.offset(0, 3, 0), theme.throne());
        placed += set(level, base.offset(-1, 2, 0), theme.throneStair());
        placed += set(level, base.offset(1, 2, 0), theme.throneStair());
        return placed;
    }

    /** 一处主题陈设（骨堆 / 锁链柱 / 藏宝堆 + 紫晶簇 / 金堆 + 青金石）。 */
    private static int trinket(ServerLevel level, Theme theme, BlockPos base, int index) {
        int placed = 0;
        switch (theme) {
            case BEAST -> {
                // 骨堆：2~3 格骨块（粗野、不对称）
                placed += set(level, base, Blocks.BONE_BLOCK.defaultBlockState());
                placed += set(level, base.above(), Blocks.BONE_BLOCK.defaultBlockState());
                if ((index & 1) == 1) {
                    placed += set(level, base.above(2), Blocks.BONE_BLOCK.defaultBlockState());
                }
            }
            case DEMON -> {
                // 锁链柱：黑石基座 + 垂挂锁链 + 灵魂灯
                placed += set(level, base, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                placed += set(level, base.above(), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                placed += set(level, base.above(2), Blocks.CHAIN.defaultBlockState());
                placed += set(level, base.above(3), Blocks.SOUL_LANTERN.defaultBlockState());
            }
            case EPOCH -> {
                // 机关藏宝堆：金块 + 青金石（纪元主题的铜金/青金配色）
                placed += set(level, base, theme.treasure());
                placed += set(level, base.above(), Blocks.LAPIS_BLOCK.defaultBlockState());
                if ((index & 1) == 1) {
                    placed += set(level, base.east(), theme.treasure());
                }
            }
            default -> {
                // VOID：藏宝堆 + 紫晶簇（奇偶各一/二）
                placed += set(level, base, theme.treasure());
                placed += set(level, base.above(), Blocks.AMETHYST_CLUSTER.defaultBlockState());
                if ((index & 1) == 1) {
                    placed += set(level, base.east(), theme.treasure());
                    placed += set(level, base.east().above(),
                            Blocks.AMETHYST_CLUSTER.defaultBlockState());
                }
            }
        }
        return placed;
    }

    /**
     * 朝副本原点方向开 3 宽门洞 + 2 格门廊。
     *
     * <p>只朝一个方向（取 |x| 与 |z| 中较大的轴），门洞上方补一层 accent 作门楣，
     * 保留外墙整体性；门廊向外延伸 2 格地面、上方清空。</p>
     */
    private static int carveEntrance(ServerLevel level, BlockPos c, Theme theme, int y) {
        int[] d = entranceDir(c);
        int perpX = d[1];
        int perpZ = -d[0];
        int placed = 0;
        // 打穿 3 层墙厚（r = RADIUS-2 .. RADIUS），3 格宽
        for (int w = -1; w <= 1; w++) {
            for (int r = RADIUS - 2; r <= RADIUS; r++) {
                int wx = c.getX() + d[0] * r + perpX * w;
                int wz = c.getZ() + d[1] * r + perpZ * w;
                for (int h = 1; h <= WALL_H; h++) {
                    placed += set(level, new BlockPos(wx, y + h, wz),
                            Blocks.AIR.defaultBlockState());
                }
            }
            // 2 格门廊：地面 + 净空
            for (int step = 1; step <= 2; step++) {
                BlockPos p = new BlockPos(
                        c.getX() + d[0] * (RADIUS + step) + perpX * w, y,
                        c.getZ() + d[1] * (RADIUS + step) + perpZ * w);
                placed += set(level, p, theme.floor());
                placed += set(level, p.above(), Blocks.AIR.defaultBlockState());
                placed += set(level, p.above(2), Blocks.AIR.defaultBlockState());
            }
        }
        // 门楣（入口正上方补 accent，门洞有“框”）
        for (int w = -1; w <= 1; w++) {
            placed += set(level, new BlockPos(
                    c.getX() + d[0] * RADIUS + perpX * w, y + WALL_H + 1,
                    c.getZ() + d[1] * RADIUS + perpZ * w), theme.accent());
        }
        return placed;
    }

    /** 入口方向（朝副本原点，返回单位轴向 {dx,dz}）。 */
    private static int[] entranceDir(BlockPos c) {
        if (Math.abs(c.getX()) >= Math.abs(c.getZ())) {
            return new int[]{c.getX() > 0 ? -1 : 1, 0};
        }
        return new int[]{0, c.getZ() > 0 ? -1 : 1};
    }

    /** 判断是否落在半径 r 的同心环带上。 */
    private static boolean isRingAt(int d2, int r) {
        int lo = (r - 1) * (r - 1);
        int hi = (r + 1) * (r + 1);
        return d2 >= lo && d2 <= hi;
    }

    /** 放方块：未加载的格子跳过。 */
    private static int set(ServerLevel level, BlockPos p, BlockState st) {
        if (!level.isLoaded(p)) {
            return 0;
        }
        level.setBlock(p, st, 2);
        return 1;
    }

    /** 判断是否落在某个同心花纹环上。 */
    private static boolean nearRing(int d2, int radius) {
        for (int r : new int[]{radius / 3, radius * 2 / 3, radius - 3}) {
            int lo = (r - 1) * (r - 1);
            int hi = (r + 1) * (r + 1);
            if (d2 >= lo && d2 <= hi) {
                return true;
            }
        }
        return false;
    }

    /**
     * <b>在决战竞技场旁边的空地上补一间小祭坛房间</b>（修“死触达”）。
     *
     * <p>为什么需要：本类历史上只在 {@code *_realm} 副本维度被
     * {@code ModEvents.onEntityJoin} 调用；但四只主题 boss 实际是在 {@code arena_*}
     * 决战维度由 {@code BossSummonBlock} 召唤的 —— 于是小祭坛房间/宝箱在实机里
     * 从来没有出现过（D3 审计发现）。这里由 {@link ArenaRealm#enter} 在进决战维度时
     * 顺带补一间：放在竞技场对角线外侧约 102 格（大竞技场 r=84、主题环 r≥120 之间），
     * 不压跑道/看台，玩家从竞技场外圈可以走过去开箱。</p>
     *
     * <p>幂等：{@link #ensure} 自带 marker 判据，重复调用 0 写入。</p>
     *
     * @param arenaCenter 竞技场中心（ArenaRealm.center(level)）
     * @return 本次是否真的新建
     */
    public static boolean ensureBesideArena(ServerLevel level, Dungeon dungeon,
                                            BlockPos arenaCenter) {
        if (level == null || dungeon == null || arenaCenter == null) {
            return false;
        }
        Theme theme = switch (dungeon) {
            case EPOCH -> Theme.EPOCH;
            case BEAST -> Theme.BEAST;
            case DEMON -> Theme.DEMON;
            case VOID -> Theme.VOID;
        };
        // 对角方向 72,72 ≈ 102 格：大于大竞技场 r=84，小于 ArenaTheme 环带 r=120
        int sx = arenaCenter.getX() + 72;
        int sz = arenaCenter.getZ() + 72;
        // 先把整间房要用的 5×5 区块立起来（r=26 → 约 52×52 格），
        // 否则 getHeight 会把地面算成世界底部，且 build 里的 setBlock 会逐个同步加载区块（卡顿）。
        int ccx = sx >> 4;
        int ccz = sz >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(ccx + dx, ccz + dz,
                        net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
            }
        }
        int h = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                sx, sz);
        // ensure 的约定：center.below() 是地面层，所以传“站立格”
        BlockPos stand = new BlockPos(sx, h, sz);
        boolean built = ensure(level, stand, theme);
        if (built) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[arena] 决战维度小祭坛房间已建：{} 位于 {}（主题 {}）",
                    dungeon.dimensionId(), stand, theme);
        }
        return built;
    }

    /** 实体类型 → 主题（不是祭坛 boss 返回 null）。 */
    public static Theme themeOf(String entityPath) {
        if (entityPath == null) {
            return null;
        }
        return switch (entityPath) {
            case "epoch_boss" -> Theme.EPOCH;
            case "beast_boss" -> Theme.BEAST;
            case "demon_boss" -> Theme.DEMON;
            case "void_boss" -> Theme.VOID;
            default -> null;
        };
    }
}
