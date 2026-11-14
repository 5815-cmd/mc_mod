package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumSet;
import java.util.Set;

/**
 * <b>战斗场地前庭</b>：把「竞技场」和「外围主题地貌」之间那段没处理过的噪声地形补成
 * 一座有仪式感、能跑能打的前庭广场。
 *
 * <p>玩家反馈：「副本维度什么的战斗场地维度地形继续完善优化」。之前的层次是：</p>
 * <ul>
 *   <li>{@link DungeonArena}：中央战斗场地 + 五层看台，半径 ≤24；</li>
 *   <li>{@link RealmTheme}：外围主题地貌，半径 44~112；</li>
 *   <li>于是 <b>r=25~43</b> 是一圈纯噪声地形：坑洼、断崖、树、水——就在看台脚下，
 *       打第一波怪、绕场跑位全在这里，观感最差；</li>
 *   <li>boss 维度里 {@link ArenaBattle} 的看台外沿 r=84，而 {@link ArenaTheme}
 *       的主题环从 r=120 才开始，<b>r=85~110 也是一片空白平地</b>。</li>
 * </ul>
 *
 * <h3>本次补的两层前庭</h3>
 * <ol>
 *   <li><b>副本前庭（r=25~43）</b>：整平接上看台地面、铺主题地砖、中轴/同心纹样；
 *       东/西/南三条出场的<b>踏道 + 栏板 + 灯柱 + 旗幡</b>；外圈矮城墙（三条门洞）；
 *       六个主题小景（玉灯亭 / 兽骨祭坛 / 血石尖碑 / 虚空晶碑）；对角刷怪圈地纹。</li>
 *   <li><b>决战前庭（r=85~110）</b>：四向<b>神道</b>从看台外沿伸向主题环，两侧灯柱/栏板，
 *       尽头一座<b>门阙</b>；外圈环形步道；四角主题丰碑；地面补平、杂树清空。</li>
 * </ol>
 *
 * <h3>为什么单独一个类 / 单独的 SavedData</h3>
 * <p>前庭是"新增的一层"，不能靠 {@code DungeonArena}（它用中央核心方块判幂等，老存档
 * 早就建完、不会再动）或 {@link RealmTheme}（它有自己的版本号，动它会连整圈外围一起重铺）。
 * 所以这里用<b>自己的 SavedData + 自己的 VERSION + 每座副本一位掩码</b>：
 * 老存档第一次进门只补建一次前庭，之后只读标记；以后改生成逻辑 VERSION+1 即可重铺。</p>
 *
 * <h3>安全边界</h3>
 * <ul>
 *   <li>副本侧：内圈让开 r≤{@value DungeonArena#BUILD_R}（看台），外圈不碰
 *       r≥{@link RealmTheme#RING_INNER}（主题地貌），大本营禁区
 *       {@code |x|≤30 && z∈[-56,-8]} 整块让开；</li>
 *   <li>决战侧：内圈让开 r≤84（看台外沿），外圈不碰 r≥100（{@code ArenaTheme} 安全边界），
 *       主题环本体（r≥120）一格不写。</li>
 * </ul>
 */
public final class DungeonForecourt {

    /** 生成器版本：改了前庭生成逻辑必须 +1，否则老存档看不到新内容。 */
    public static final int VERSION = 1;

    /** 存档键（每个维度各自一份）。 */
    private static final String DATA_KEY = "hongmeng_dungeon_forecourt";

    // ------------------------------------------------------------------
    // 副本侧（r=25~43）
    // ------------------------------------------------------------------

    /** 内圈：让开战斗场地 + 看台（看台外沿 BUILD_R=24）。 */
    private static final int REALM_INNER = DungeonArena.BUILD_R + 1;     // 25

    /** 外圈：让开 RealmTheme 的主题地貌（RING_INNER=44）。 */
    private static final int REALM_OUTER = RealmTheme.RING_INNER - 1;    // 43

    /** 中轴踏道半宽（0..4 = 9 格宽，正好接看台踏道口的 5 格 + 两侧栏板）。 */
    private static final int CORRIDOR_HALF = 4;

    /** 大本营禁区（与 RealmTheme 一致）。 */
    private static final int LANDMARK_X = 30;
    private static final int LANDMARK_Z0 = -56;
    private static final int LANDMARK_Z1 = -8;

    // ------------------------------------------------------------------
    // 决战侧（r=85~110）
    // ------------------------------------------------------------------

    /** 内圈：让开 ArenaBattle 看台（外沿 r=84）。 */
    private static final int ARENA_INNER = 85;

    /** 外圈：不碰 ArenaTheme 安全边界（SAFE_R=100）以外的主题场景；这里做到 110，留足过渡。 */
    private static final int ARENA_OUTER = 110;

    /** 决战神道半宽（0..5 = 11 格宽）。 */
    private static final int ARENA_CORRIDOR_HALF = 5;

    /** 本次会话生成失败的副本（不反复重试，和 DungeonArena/RealmTheme 同口径）。 */
    private static final Set<Dungeon> FAILED = EnumSet.noneOf(Dungeon.class);

    private DungeonForecourt() {
    }

    // ==================================================================
    // 对外入口
    // ==================================================================

    /**
     * 副本前庭：补 {@code r=25~43} 的过渡带（只建一次，SavedData 记掩码）。
     *
     * @param floorY 竞技场地面方块层（{@link DungeonArena#floorY}）
     */
    public static void ensureRealm(ServerLevel level, Dungeon dungeon, int floorY) {
        if (level == null || dungeon == null || FAILED.contains(dungeon)) {
            return;
        }
        // PERF(A)/正确性保护：战场 + 大本营现在是跨 tick 分片任务，结算前 floorY 可能还是
        // 临时值（{@link DungeonArena#floorY} 不再同步生成区块探测）。前庭整平完全依赖
        // floorY，用临时值会把 r=25~43 整到错误高度 —— 所以先不动工，等战场结算后
        // 下一次进场/命令再补建（幂等，老存档/正常流程不受影响）。
        if (!DungeonArena.isSettled(dungeon)) {
            HongmengMod.LOGGER.info(
                    "[forecourt] {} 副本前庭暂缓：等待战场/大本营分片任务结算", dungeon.dimensionId());
            return;
        }
        try {
            ForecourtFlag flag = flag(level);
            int bit = 1 << dungeon.ordinal();
            if ((flag.realmMask & bit) != 0) {
                return;
            }
            long t0 = System.nanoTime();
            preload(level, REALM_OUTER + 1);
            Palette pal = palette(dungeon);
            Stats st = new Stats();
            buildRealm(level, dungeon, floorY, pal, st);
            flag.realmMask |= bit;
            flag.setDirty();
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            HongmengMod.LOGGER.info(
                    "[forecourt] {} 副本前庭完成：{} 格 / {} ms（r={}~{}，floorY={}，"
                            + "未加载跳过 {}）",
                    dungeon.dimensionId(), st.placed, ms, REALM_INNER, REALM_OUTER, floorY,
                    st.skipped);
        } catch (Throwable t) {
            FAILED.add(dungeon);
            HongmengMod.LOGGER.warn("[forecourt] {} 副本前庭生成失败（不影响副本可玩）：{}",
                    dungeon.dimensionId(), t.toString());
        }
    }

    /**
     * 决战前庭：补竞技场外 {@code r=85~110} 的四向神道/门阙/环形步道/四角丰碑。
     *
     * @param center {@link ArenaRealm#center(ServerLevel)} 给出的竞技场中心
     */
    public static void ensureArena(ServerLevel level, Dungeon dungeon, BlockPos center) {
        if (level == null || dungeon == null || center == null || FAILED.contains(dungeon)) {
            return;
        }
        try {
            ForecourtFlag flag = flag(level);
            int bit = 1 << dungeon.ordinal();
            if ((flag.arenaMask & bit) != 0) {
                return;
            }
            long t0 = System.nanoTime();
            preload(level, ARENA_OUTER + 2);
            Palette pal = palette(dungeon);
            Stats st = new Stats();
            buildArena(level, dungeon, center, pal, st);
            flag.arenaMask |= bit;
            flag.setDirty();
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            HongmengMod.LOGGER.info(
                    "[forecourt] {} 决战前庭完成：{} 格 / {} ms（r={}~{}，中心 {}，"
                            + "未加载跳过 {}）",
                    dungeon.dimensionId(), st.placed, ms, ARENA_INNER, ARENA_OUTER, center,
                    st.skipped);
        } catch (Throwable t) {
            FAILED.add(dungeon);
            HongmengMod.LOGGER.warn("[forecourt] {} 决战前庭生成失败（不影响决战）：{}",
                    dungeon.dimensionId(), t.toString());
        }
    }

    // ==================================================================
    // 副本前庭
    // ==================================================================

    private static void buildRealm(ServerLevel level, Dungeon dungeon, int fy, Palette pal,
                                   Stats st) {
        int outer = REALM_OUTER;
        // ---- ① 整平 + 垫实 + 清障碍（把噪声地形收成与竞技场同高的广场）----
        for (int x = -outer; x <= outer; x++) {
            for (int z = -outer; z <= outer; z++) {
                int cheb = Math.max(Math.abs(x), Math.abs(z));
                if (cheb <= DungeonArena.BUILD_R) {
                    continue;                       // 让开竞技场本体
                }
                if ((long) x * x + (long) z * z > (long) outer * outer) {
                    continue;                       // 圆形外沿，接 RealmTheme
                }
                if (inLandmark(x, z)) {
                    continue;                       // 让开大本营
                }
                int feet = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                // 上方凿空：地形/树/山包挡路的一律清掉（清到足够高，避免"前庭里冒山尖"）
                int clearTop = fy + Math.max(14, feet - fy + 4);
                for (int y = fy + 1; y <= clearTop; y++) {
                    clear(level, x, y, z, st);
                }
                // 下方垫实：与原地形接上（最多垫 16 层，避免深谷爆量）
                int groundTop = feet - 1;
                int bottom = Math.max(level.getMinBuildHeight() + 1,
                        Math.max(groundTop, fy - 16));
                for (int y = fy - 1; y >= bottom; y--) {
                    set(level, x, y, z, pal.foundation(), st);
                }
                // 地面铺装
                int eu = (int) Math.round(Math.sqrt((double) x * x + (double) z * z));
                set(level, x, fy, z, realmFloor(pal, x, z, eu), st);
            }
        }

        // ---- ② 三条中轴踏道：从看台顶下到前庭地面，两侧栏板 + 灯柱 ----
        for (Direction dir : new Direction[]{Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            buildRealmCauseway(level, fy, pal, st, dir);
        }

        // ---- ③ 外圈矮城墙 + 三处门阙 ----
        buildRealmParapet(level, fy, pal, st);

        // ---- ④ 六个主题小景（对角/两翼）----
        int[][] anchors = {
                {30, 30}, {-30, 30},                 // 南侧两角
                {34, 14}, {34, -14},                 // 东翼
                {-34, 14}, {-34, -14},               // 西翼
        };
        for (int[] a : anchors) {
            int x = a[0];
            int z = a[1];
            if (inLandmark(x, z) || Math.max(Math.abs(x), Math.abs(z)) <= DungeonArena.BUILD_R) {
                continue;
            }
            if ((long) x * x + (long) z * z > (long) REALM_OUTER * REALM_OUTER) {
                continue;
            }
            buildRealmMonument(level, dungeon, fy, pal, st, x, z);
        }

        // ---- ⑤ 刷怪圈地纹（对角地面纹样，告诉玩家"这里是战场"；不挡路）----
        int[][] rims = {{28, 14}, {28, -14}, {-28, 14}, {-28, -14}, {14, 30}, {-14, 30}};
        for (int[] r : rims) {
            int x = r[0];
            int z = r[1];
            if (inLandmark(x, z)) {
                continue;
            }
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 < 8 || d2 > 10) {
                        continue;
                    }
                    if (Math.max(Math.abs(x + dx), Math.abs(z + dz)) <= DungeonArena.BUILD_R) {
                        continue;
                    }
                    set(level, x + dx, fy, z + dz, pal.line(), st);
                }
            }
        }
    }

    /** 副本侧地面纹样：中轴踏道 / 同心环 / 对角纹 / 外圈铺装。 */
    private static BlockState realmFloor(Palette pal, int x, int z, int eu) {
        boolean south = z >= REALM_INNER && Math.abs(x) <= CORRIDOR_HALF;
        boolean east = x >= REALM_INNER && Math.abs(z) <= CORRIDOR_HALF;
        boolean west = x <= -REALM_INNER && Math.abs(z) <= CORRIDOR_HALF;
        if (south || east || west) {
            boolean axis = (south && x == 0) || (!south && z == 0);
            if (axis) {
                return pal.line();
            }
            if (Math.abs(south ? x : z) <= 1) {
                return pal.roadLine();
            }
            return pal.road();
        }
        if (eu >= 40) {
            return pal.floorAlt();
        }
        if (Math.abs(x) == Math.abs(z) || eu % 12 == 0) {
            return pal.floorAlt();
        }
        return pal.floor();
    }

    // ---- 坐标映射：dir + along（沿轴距离）+ perp（垂直偏移）→ 世界 x/z ----

    private static int xOf(Direction dir, int along, int perp) {
        return switch (dir) {
            case EAST -> along;
            case WEST -> -along;
            case SOUTH -> perp;
            case NORTH -> -perp;
            default -> perp;
        };
    }

    private static int zOf(Direction dir, int along, int perp) {
        return switch (dir) {
            case SOUTH -> along;
            case NORTH -> -along;
            case EAST -> perp;
            case WEST -> -perp;
            default -> perp;
        };
    }

    /** 从“朝外”方向取“朝内”的方向（楼梯 FACING = 上升方向；下坡楼梯的上升侧朝场内）。 */
    private static Direction inwardOf(Direction outward) {
        return outward.getOpposite();
    }

    /** 一条中轴踏道：看台顶（fy+5）→ 前庭地面（fy），随后是栏板 + 灯柱 + 旗幡。 */
    private static void buildRealmCauseway(ServerLevel level, int fy, Palette pal, Stats st,
                                           Direction outward) {
        // 台阶段：along=25..30，y 从 fy+5 降到 fy；楼梯 FACING = 上升方向 = 朝场内
        BlockState step = stair(pal, inwardOf(outward));
        for (int along = REALM_INNER; along <= REALM_INNER + 5; along++) {
            int y = fy + (REALM_INNER + 5 - along);      // 25→fy+5, 30→fy
            for (int perp = -2; perp <= 2; perp++) {
                int x = xOf(outward, along, perp);
                int z = zOf(outward, along, perp);
                set(level, x, y, z, step, st);
                // 踏步下方垫实
                for (int k = 1; k <= y - fy; k++) {
                    set(level, x, y - k, z, pal.foundation(), st);
                }
            }
        }
        // 平段栏板：along=31..43，两侧 ±5：矮墙 + 台沿 + 灯柱 + 旗幡
        for (int along = REALM_INNER + 6; along <= REALM_OUTER; along++) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                int perp = sgn * (CORRIDOR_HALF + 1);
                int x = xOf(outward, along, perp);
                int z = zOf(outward, along, perp);
                set(level, x, fy, z, pal.wall(), st);
                set(level, x, fy + 1, z, pal.kerb(), st);
                if (along % 8 == 0) {
                    set(level, x, fy + 2, z, pal.pillar(), st);
                    set(level, x, fy + 3, z, pal.light(), st);
                    if ((along / 8) % 2 == 0) {
                        set(level, x, fy + 4, z, pal.banner(), st);
                    }
                }
            }
        }
    }


    /** 副本外圈矮城墙：外沿两圈，三条踏道口开成门洞，门洞两侧立门阙。 */
    private static void buildRealmParapet(ServerLevel level, int fy, Palette pal, Stats st) {
        for (int x = -REALM_OUTER; x <= REALM_OUTER; x++) {
            for (int z = -REALM_OUTER; z <= REALM_OUTER; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) <= DungeonArena.BUILD_R) {
                    continue;
                }
                if (inLandmark(x, z)) {
                    continue;
                }
                int eu = (int) Math.round(Math.sqrt((double) x * x + (double) z * z));
                if (eu < REALM_OUTER - 1) {
                    continue;                       // 只在最外两圈做矮墙
                }
                boolean gate = realmGate(x, z);
                if (gate) {
                    continue;                       // 三条踏道口留门
                }
                set(level, x, fy + 1, z, pal.wall(), st);
                if (((x + z) & 1) == 0) {
                    set(level, x, fy + 2, z, pal.wall(), st);
                }
            }
        }
        // 门阙：踏道口两侧各一对望柱 + 灯
        for (Direction dir : new Direction[]{Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                int perp = sgn * (CORRIDOR_HALF + 1);
                int along = REALM_OUTER - 1;
                int x = xOf(dir, along, perp);
                int z = zOf(dir, along, perp);
                for (int y = fy + 1; y <= fy + 4; y++) {
                    set(level, x, y, z, pal.pillar(), st);
                }
                set(level, x, fy + 5, z, pal.light(), st);
                set(level, x, fy + 6, z, pal.accent(), st);
            }
        }
    }

    /** 三条踏道口（东/西/南）的外圈开口。 */
    private static boolean realmGate(int x, int z) {
        return (z >= 0 && Math.abs(x) <= CORRIDOR_HALF)
                || (x >= 0 && Math.abs(z) <= CORRIDOR_HALF)
                || (x <= 0 && Math.abs(z) <= CORRIDOR_HALF);
    }


    /** 副本侧主题小景：玉灯亭 / 兽骨祭坛 / 血石尖碑 / 虚空晶碑。 */
    private static void buildRealmMonument(ServerLevel level, Dungeon dungeon, int fy,
                                            Palette pal, Stats st, int cx, int cz) {
        // 5×5 台座（抬一格，做出"前庭里的景"）
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                set(level, cx + dx, fy + 1, cz + dz, edge ? pal.wall() : pal.floorAlt(), st);
                if (edge) {
                    set(level, cx + dx, fy + 2, cz + dz, pal.kerb(), st);
                }
            }
        }
        switch (dungeon) {
            case EPOCH -> {
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        for (int y = 2; y <= 4; y++) {
                            set(level, cx + sx, fy + y, cz + sz, pal.pillar(), st);
                        }
                    }
                }
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) <= 3) {
                            set(level, cx + dx, fy + 5, cz + dz, pal.accent(), st);
                        }
                    }
                }
                set(level, cx, fy + 2, cz, pal.light(), st);
            }
            case BEAST -> {
                // 骨拱：两根弯骨 + 骨顶
                for (int y = 2; y <= 5; y++) {
                    set(level, cx - 1, fy + y, cz, pal.pillar(), st);
                    set(level, cx + 1, fy + y, cz, pal.pillar(), st);
                }
                for (int dx = -1; dx <= 1; dx++) {
                    set(level, cx + dx, fy + 6, cz, pal.accent(), st);
                }
                set(level, cx, fy + 2, cz, Blocks.CAMPFIRE.defaultBlockState(), st);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        set(level, cx + sx * 2, fy + 1, cz + sz * 2, Blocks.HAY_BLOCK.defaultBlockState(), st);
                    }
                }
            }
            case DEMON -> {
                for (int y = 2; y <= 5; y++) {
                    set(level, cx, fy + y, cz, pal.pillar(), st);
                }
                set(level, cx, fy + 6, cz, Blocks.SOUL_LANTERN.defaultBlockState(), st);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        set(level, cx + sx, fy + 2, cz + sz, Blocks.MAGMA_BLOCK.defaultBlockState(), st);
                    }
                }
                set(level, cx, fy + 2, cz, Blocks.REDSTONE_BLOCK.defaultBlockState(), st);
            }
            case VOID -> {
                for (int y = 2; y <= 5; y++) {
                    set(level, cx, fy + y, cz, pal.pillar(), st);
                }
                set(level, cx, fy + 6, cz, Blocks.END_ROD.defaultBlockState(), st);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        set(level, cx + sx, fy + 2, cz + sz, Blocks.AMETHYST_BLOCK.defaultBlockState(), st);
                        set(level, cx + sx, fy + 3, cz + sz, pal.accent(), st);
                    }
                }
            }
        }
    }

    // ==================================================================
    // 决战前庭
    // ==================================================================

    private static void buildArena(ServerLevel level, Dungeon dungeon, BlockPos center,
                                   Palette pal, Stats st) {
        int base = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 90, 0);
        // ---- ① 环形带地面：补平到 base（站人层），清掉杂树/草坡 ----
        for (int x = -ARENA_OUTER; x <= ARENA_OUTER; x++) {
            for (int z = -ARENA_OUTER; z <= ARENA_OUTER; z++) {
                long d2 = (long) x * x + (long) z * z;
                if (d2 < (long) ARENA_INNER * ARENA_INNER
                        || d2 > (long) ARENA_OUTER * ARENA_OUTER) {
                    continue;
                }
                if (inArenaAltarRoom(x, z)) {
                    continue;                       // 让开 AltarArena 的小祭坛房间（52×52）
                }
                int feet = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                // 垫平到 base-1（走层 base），先把这一格上方的地形/草坡清掉
                int top = base - 1;
                int clearTo = Math.max(top + 10, feet + 6);
                for (int y = top + 1; y <= clearTo; y++) {
                    clear(level, x, y, z, st);
                }
                int bottom = Math.max(level.getMinBuildHeight() + 1,
                        Math.max(feet - 1, top - 6));
                for (int y = top; y >= bottom; y--) {
                    set(level, x, y, z, pal.foundation(), st);
                }
                set(level, x, top, z, arenaFloor(pal, x, z), st);
            }
        }

        // ---- ② 四向神道：看台外沿 → 主题环前的门阙 ----
        for (Direction dir : new Direction[]{Direction.SOUTH, Direction.NORTH,
                Direction.EAST, Direction.WEST}) {
            buildArenaCauseway(level, base, pal, st, dir);
        }

        // ---- ③ 外圈环形步道 + 栏板 ----
        for (int x = -ARENA_OUTER; x <= ARENA_OUTER; x++) {
            for (int z = -ARENA_OUTER; z <= ARENA_OUTER; z++) {
                long d2 = (long) x * x + (long) z * z;
                if (d2 < (long) (ARENA_OUTER - 3) * (ARENA_OUTER - 3)
                        || d2 > (long) ARENA_OUTER * ARENA_OUTER) {
                    continue;
                }
                if (onArenaCauseway(x, z) || inArenaAltarRoom(x, z)) {
                    continue;                       // 神道口留空 / 小祭坛房间让开
                }
                set(level, x, base - 1, z, pal.floorAlt(), st);
                if (((x + z) & 1) == 0) {
                    set(level, x, base, z, pal.wall(), st);
                }
            }
        }

        // ---- ④ 四角主题丰碑 ----
        // 四角丰碑：SE 角让给小祭坛房间（AltarArena 在 72,72）
        int[][] corners = {{-76, 76}, {76, -76}, {-76, -76}};
        for (int[] c : corners) {
            buildArenaMonument(level, dungeon, base, pal, st, c[0], c[1]);
        }
    }

    /** 决战侧地面纹样：神道 / 环带铺装。 */
    private static BlockState arenaFloor(Palette pal, int x, int z) {
        boolean onX = Math.abs(z) <= ARENA_CORRIDOR_HALF && Math.abs(x) >= ARENA_INNER;
        boolean onZ = Math.abs(x) <= ARENA_CORRIDOR_HALF && Math.abs(z) >= ARENA_INNER;
        if (onX || onZ) {
            boolean axis = (onX && z == 0) || (onZ && x == 0);
            if (axis) {
                return pal.line();
            }
            if (Math.abs(onX ? z : x) <= 1) {
                return pal.roadLine();
            }
            return pal.road();
        }
        return pal.floorAlt();
    }

    /** 决战侧一条神道：两侧栏板/灯柱 + 尽头门阙（横梁跨门洞）。 */
    private static void buildArenaCauseway(ServerLevel level, int base, Palette pal, Stats st,
                                           Direction dir) {
        for (int along = ARENA_INNER; along <= ARENA_OUTER - 2; along++) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                int perp = sgn * (ARENA_CORRIDOR_HALF + 1);
                int x = xOf(dir, along, perp);
                int z = zOf(dir, along, perp);
                set(level, x, base, z, pal.wall(), st);
                set(level, x, base + 1, z, pal.kerb(), st);
                if (along % 8 == 0) {
                    set(level, x, base + 2, z, pal.pillar(), st);
                    set(level, x, base + 3, z, pal.light(), st);
                }
            }
        }
        // 门阙：尽头两根方柱 + 横梁
        int along = ARENA_OUTER - 1;
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            int perp = sgn * (ARENA_CORRIDOR_HALF + 1);
            int x = xOf(dir, along, perp);
            int z = zOf(dir, along, perp);
            for (int y = base + 1; y <= base + 6; y++) {
                set(level, x, y, z, pal.pillar(), st);
            }
            set(level, x, base + 7, z, pal.light(), st);
        }
        for (int w = -ARENA_CORRIDOR_HALF; w <= ARENA_CORRIDOR_HALF; w++) {
            int x = xOf(dir, along, w);
            int z = zOf(dir, along, w);
            set(level, x, base + 6, z, pal.accent(), st);
        }
    }


    /** 决战侧四角丰碑：台座 + 主题柱 + 灯。 */
    private static void buildArenaMonument(ServerLevel level, Dungeon dungeon, int base,
                                            Palette pal, Stats st, int cx, int cz) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                set(level, cx + dx, base - 1, cz + dz, edge ? pal.wall() : pal.floorAlt(), st);
                if (edge) {
                    set(level, cx + dx, base, cz + dz, pal.kerb(), st);
                }
            }
        }
        switch (dungeon) {
            case EPOCH, VOID -> {
                for (int y = 1; y <= 8; y++) {
                    set(level, cx, base + y, cz, pal.pillar(), st);
                }
                set(level, cx, base + 9, cz, pal.light(), st);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        set(level, cx + sx * 2, base + 1, cz + sz * 2, pal.accent(), st);
                    }
                }
            }
            case BEAST -> {
                for (int y = 1; y <= 6; y++) {
                    set(level, cx - 2, base + y, cz, pal.pillar(), st);
                    set(level, cx + 2, base + y, cz, pal.pillar(), st);
                }
                for (int dx = -2; dx <= 2; dx++) {
                    set(level, cx + dx, base + 7, cz, pal.accent(), st);
                }
                set(level, cx, base + 1, cz, Blocks.CAMPFIRE.defaultBlockState(), st);
            }
            case DEMON -> {
                for (int y = 1; y <= 7; y++) {
                    set(level, cx, base + y, cz, pal.pillar(), st);
                }
                set(level, cx, base + 8, cz, Blocks.SOUL_LANTERN.defaultBlockState(), st);
                for (int sx = -1; sx <= 1; sx += 2) {
                    set(level, cx + sx * 2, base + 1, cz, Blocks.MAGMA_BLOCK.defaultBlockState(), st);
                }
            }
        }
    }

    // ==================================================================
    // 材料调色板
    // ==================================================================

    private record Palette(BlockState foundation, BlockState floor, BlockState floorAlt,
                           BlockState road, BlockState roadLine, BlockState line,
                           BlockState wall, BlockState kerb, BlockState pillar,
                           BlockState light, BlockState accent, BlockState banner,
                           BlockState stair) {
    }

    /** 按副本主题挑一套材料（和 {@link DungeonArena} 的配色同一语法）。 */
    private static Palette palette(Dungeon dungeon) {
        return switch (dungeon) {
            case EPOCH -> new Palette(
                    Blocks.STONE_BRICKS.defaultBlockState(),
                    Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    Blocks.QUARTZ_BLOCK.defaultBlockState(),
                    Blocks.QUARTZ_BRICKS.defaultBlockState(),
                    Blocks.QUARTZ_PILLAR.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(),
                    Blocks.QUARTZ_SLAB.defaultBlockState(),
                    Blocks.QUARTZ_PILLAR.defaultBlockState(),
                    Blocks.SEA_LANTERN.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.LIGHT_BLUE_BANNER.defaultBlockState(),
                    Blocks.QUARTZ_STAIRS.defaultBlockState());
            case BEAST -> new Palette(
                    Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.COARSE_DIRT.defaultBlockState(),
                    Blocks.MUD_BRICKS.defaultBlockState(),
                    Blocks.PACKED_MUD.defaultBlockState(),
                    Blocks.HAY_BLOCK.defaultBlockState(),
                    Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
                    Blocks.STONE_BRICK_SLAB.defaultBlockState(),
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState(),
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    Blocks.BROWN_BANNER.defaultBlockState(),
                    Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState());
            case DEMON -> new Palette(
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.NETHER_BRICKS.defaultBlockState(),
                    Blocks.BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                    Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState(),
                    Blocks.CRIMSON_HYPHAE.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState(),
                    Blocks.MAGMA_BLOCK.defaultBlockState(),
                    Blocks.RED_BANNER.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState());
            case VOID -> new Palette(
                    Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.END_STONE_BRICKS.defaultBlockState(),
                    Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.PURPUR_PILLAR.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.OBSIDIAN.defaultBlockState(),
                    Blocks.END_STONE_BRICK_SLAB.defaultBlockState(),
                    Blocks.PURPUR_PILLAR.defaultBlockState(),
                    Blocks.END_ROD.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.PURPLE_BANNER.defaultBlockState(),
                    Blocks.END_STONE_BRICK_STAIRS.defaultBlockState());
        };
    }

    // ==================================================================
    // 小工具
    // ==================================================================

    private static boolean inLandmark(int x, int z) {
        return Math.abs(x) <= LANDMARK_X && z >= LANDMARK_Z0 && z <= LANDMARK_Z1;
    }

    /** 决战维度里的 AltarArena 小房间：中心 (72,72)、约 52×52（含 2 格余量）。 */
    private static boolean inArenaAltarRoom(int x, int z) {
        return x >= 44 && x <= 100 && z >= 44 && z <= 100;
    }

    private static boolean onArenaCauseway(int x, int z) {
        return (Math.abs(z) <= ARENA_CORRIDOR_HALF && Math.abs(x) >= ARENA_INNER)
                || (Math.abs(x) <= ARENA_CORRIDOR_HALF && Math.abs(z) >= ARENA_INNER);
    }

    private static BlockState stair(Palette pal, Direction facing) {
        return pal.stair().setValue(StairBlock.FACING, facing);
    }


    /** 预加载以中心为圆心、半径 radius 覆盖到的区块（只加载 FULL，保证 heightmap 正确）。 */
    private static void preload(ServerLevel level, int radius) {
        int cr = (radius + 15) >> 4;
        for (int cx = -cr; cx <= cr; cx++) {
            for (int cz = -cr; cz <= cr; cz++) {
                level.getChunk(cx, cz, ChunkStatus.FULL, true);
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state, Stats st) {
        if (y < level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1) {
            return;
        }
        BlockPos p = new BlockPos(x, y, z);
        if (!level.isLoaded(p)) {
            st.skipped++;
            return;
        }
        if (level.getBlockState(p).equals(state)) {
            return;
        }
        level.setBlock(p, state, 2);
        st.placed++;
    }

    private static void clear(ServerLevel level, int x, int y, int z, Stats st) {
        if (y < level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1) {
            return;
        }
        BlockPos p = new BlockPos(x, y, z);
        if (!level.isLoaded(p)) {
            st.skipped++;
            return;
        }
        if (level.getBlockState(p).isAir()) {
            return;
        }
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        st.cleared++;
    }

    /** 一次施工的计数。 */
    private static final class Stats {
        int placed;
        int cleared;
        int skipped;
    }

    // ==================================================================
    // 存档标记
    // ==================================================================

    private static ForecourtFlag flag(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ForecourtFlag::load, ForecourtFlag::new, DATA_KEY);
    }

    /** 每个维度一份：realmMask / arenaMask 以副本序数位标记（一座维度只会用到其中一种）。 */
    public static final class ForecourtFlag extends SavedData {
        int version = VERSION;
        int realmMask;
        int arenaMask;

        static ForecourtFlag load(CompoundTag tag) {
            ForecourtFlag f = new ForecourtFlag();
            f.version = tag.getInt("version");
            f.realmMask = tag.getInt("realm_mask");
            f.arenaMask = tag.getInt("arena_mask");
            // 版本对不上：清空掩码让新代码重铺（老存档第一次进入时 version=0）
            if (f.version != VERSION) {
                f.version = VERSION;
                f.realmMask = 0;
                f.arenaMask = 0;
            }
            return f;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("version", VERSION);
            tag.putInt("realm_mask", realmMask);
            tag.putInt("arena_mask", arenaMask);
            return tag;
        }
    }
}
