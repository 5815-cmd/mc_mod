package com.hongmeng.xiuxian.realworld;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.dungeon.BossRewards;
import com.hongmeng.xiuxian.entity.EvilGazerEntity;
import com.hongmeng.xiuxian.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>高天云层之上的战场</b>：一座悬在 y=240 的腐化圆台，头顶悬着一只「恶」的巨眼。
 *
 * <h2>玩家要求</h2>
 * <p>「场景在高天云层之上（需要做专属战斗场地）」+ 08 总表 D-7（原来的白色石英圆台太素）。
 * 保留硬锚点：<b>y=240、半径 40、中心可站立、无逃离路径、打完自动送回</b>；
 * 只重做外观：</p>
 * <ul>
 *   <li><b>台面</b>：外圈 6 格菌丝 / 黑石交错（腐化），内圈 34 格黑石 / 深板岩拼纹；
 *       中央半径 12 的三级下沉祭坛（祭坛坑底放哭泣黑曜石当幂等标记）。</li>
 *   <li><b>裂隙</b>：4 条 45°/135°/225°/315° 的沟，2 格深、约 3 格宽，从祭坛坑一路裂到台缘，
 *       黑曜 / 黑石沟底 —— 边缘的矮垛在裂隙出口处断开，看起来像「台面被撕开了」。</li>
 *   <li><b>云海</b>：y=234~238、半径 42~70 的错层白云（白羊毛 / 白色染色玻璃，2~3 层）。
 *       全部在台面之下、圆台之外，不挡台面也不挡巨眼。</li>
 *   <li><b>巨眼</b>：台面正上方悬空球壳（球心 y≈302，直径 32）：白色石英眼白 + 红混凝土 / 岩浆块
 *       虹膜 + 黑石瞳孔 + 少量哭泣黑曜石血泪痕，瞳孔朝下正对台面中心。</li>
 *   <li><b>边缘</b>：去掉白玻璃围栏，改成 1 高基座 + 3 高菌丝 / 黑石垛口（仍然掉下去就是虚空）。</li>
 * </ul>
 *
 * <h2>为什么不用 evil_altar 方块</h2>
 * <p>读代码确认：{@code EvilSummon} / {@code EvilSummonPacket} <b>不读取战场上的任何方块</b> ——
 * 召唤方块的判定发生在玩家脚下那座真祭坛（{@code EvilAltarBlockEntity}），
 * 战场中心只是「玩家的落点」。所以这里放纯装饰祭坛，不建第二座功能祭坛（否则等于开了第二个召唤点）。
 *
 * <h2>性能</h2>
 * <p>只在第一次进入时建一次（中心标记方块幂等）；逐格哈希、固定种子、同存档结果一致；
 * 每格 {@code setBlock} 前判 {@code isLoaded}；整体 try/catch，失败只 warn。
 * 巨眼球壳方块数由 {@link #eye} 统计并写日志（任务预算 ≤ 12000）。</p>
 */
public final class EvilArena {

    /** 战场高度（硬锚点，不能改）。 */
    public static final int Y = 240;

    /** 圆台半径（硬锚点，不能改）。 */
    public static final int R = 40;

    /** 中央祭坛半径（三级下沉台阶的最外一级）。 */
    private static final int ALTAR_R = 12;

    /** 云海带：内 / 外半径、层高范围。 */
    private static final int CLOUD_IN = R + 2;
    private static final int CLOUD_OUT = 70;
    private static final int CLOUD_Y0 = 234;
    private static final int CLOUD_Y1 = 238;

    /**
     * 巨眼：球心相对台面的高度、半径（直径 32）。
     *
     * <p>⚠ 不能取更大：原版世界高度上限是 y=319（-64 ~ 319），
     * 球心 Y+62 = 302、半径 16 → 球体占 y≈286~318，顶部还留 1 格余量。
     * 若按字面把球心放到 306，球顶 322 会超出高度上限被静默截掉。</p>
     */
    private static final int EYE_DY = 62;
    private static final int EYE_R = 16;

    /** 巨眼球壳方块预算。 */
    private static final int EYE_BUDGET = 12000;

    /** 固定随机种子（云海纹理可复现）。 */
    private static final long SEED = 20261101L;

    private EvilArena() {
    }

    /**
     * 建（或复用）战场，返回中心点（中心在祭坛坑底上方，玩家会被传送到这里）。
     *
     * <p>幂等：祭坛坑底正中那格哭泣黑曜石（全部建完才放的「已建成」标记）已存在就直接返回；
     * 旧版白色圆台没有这个标记，会按新外观重铺一遍。</p>
     *
     * <p><b>宝箱/陈设的重置语义（设计允许）</b>：只有「首次建 / 标记被挖掉」才会重建整座战场；
     * 重建时 {@link #detail} 会重新放回未开启的宝箱，等于重置宝箱——这是本场地的设计：
     * 恶之竞技场每次召唤都是新的一战，宝箱随之重置。正常重复召唤不会重建（标记还在），
     * 玩家开过的箱子保持已开状态，不会被无限刷。</p>
     */
    public static BlockPos ensure(ServerLevel level, BlockPos anchor) {
        if (level == null || anchor == null) {
            return null;
        }
        BlockPos c = new BlockPos(anchor.getX(), Y, anchor.getZ());
        // 幂等标记：中央祭坛坑底（Y-2）的哭泣黑曜石
        BlockPos marker = c.below(2);
        if (level.isLoaded(marker) && level.getBlockState(marker).is(Blocks.CRYING_OBSIDIAN)) {
            // 旧存档：战场本体已存在 → 只补一次宝箱/陈设（宝箱已存在则不加，不重复写）
            int added = detail(level, c);
            if (added > 0) {
                HongmengMod.LOGGER.info("[evil] 云上战场旧存档补建：宝箱/陈设 {} 格 @{}", added, c);
            }
            return c;
        }
        try {
            RandomSource rnd = RandomSource.create(SEED);
            int placed = 0;
            placed += clearLegacyRim(level, c);
            placed += platform(level, c);
            placed += altarFocus(level, c);
            placed += rim(level, c);
            placed += clouds(level, c, rnd);
            int eye = eye(level, c);
            if (eye > EYE_BUDGET) {
                HongmengMod.LOGGER.warn("[evil] 巨眼球壳方块数 {} 超出预算 {}", eye, EYE_BUDGET);
            }
            placed += eye;
            // ---- 内容层：2 宝箱 + 四角眼窝 + 锁链/血池装饰 ----
            placed += detail(level, c);
            // 「已建成」标记放到最后：中途异常时不会留下半成品却被当成建过的战场
            placed += set(level, marker, Blocks.CRYING_OBSIDIAN.defaultBlockState());
            HongmengMod.LOGGER.info(
                    "[evil] 云上战场重建完成 @{}：台面/三级祭坛/4 裂隙/垛口/云海/巨眼/宝箱陈设，共 {} 方块（其中巨眼 {}）",
                    c, placed, eye);
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[evil] 战场生成失败（不影响战斗）：{}", t.toString());
        }
        return c;
    }

    // ------------------------------------------------------------------
    // 台面 + 三级祭坛 + 4 条裂隙
    // ------------------------------------------------------------------

    /**
     * 台面：2~3 层腐化圆台，一次成形（顶层材质 / 祭坛台阶 / 裂隙沟底都在这一个循环里决定）。
     *
     * <p>高度约定（{@code c.y = Y} 是台面方块层，玩家脚底站在 {@code Y+1}）：</p>
     * <ul>
     *   <li>台面顶层方块在 {@code Y}，第二层在 {@code Y-1}；</li>
     *   <li>三级下沉祭坛：{@code r>9} 脚底 Y+1 → {@code 5<r≤9} 脚底 Y → {@code r≤5} 脚底 Y-1（坑底）；</li>
     *   <li>4 条裂隙：沟底在 {@code Y-2}（相对台面 2 格深），宽约 3 格；</li>
     *   <li>裂隙沟底 = 黑曜 / 黑石；祭坛坑底正中最后再盖一格哭泣黑曜石当「已建成」标记。</li>
     * </ul>
     */
    private static int platform(ServerLevel level, BlockPos c) {
        int placed = 0;
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > R * R) {
                    continue;
                }
                double r = Math.sqrt(d2);
                boolean rift = isRift(dx, dz);
                // 该列「下沉」几格：0 = 台面顶层，1 = 祭坛二级，2 = 祭坛坑底 / 裂隙沟底
                int depth;
                if (rift) {
                    depth = 2;
                } else if (r <= 5.0D) {
                    depth = 2;
                } else if (r <= 9.0D) {
                    depth = 1;
                } else {
                    depth = 0;
                }
                if (depth == 0) {
                    placed += set(level, pos(c, Y, dx, dz), topMaterial(r, dx, dz));
                    placed += set(level, pos(c, Y - 1, dx, dz),
                            mix(dx, dz, 91, Blocks.BLACKSTONE, Blocks.DEEPSLATE_TILES));
                } else if (depth == 1) {
                    // 祭坛二级：去掉顶层，露出 Y-1（切成深板岩瓦），并补一层底避免「空心环」
                    placed += clear(level, pos(c, Y, dx, dz));
                    placed += set(level, pos(c, Y - 1, dx, dz),
                            mix(dx, dz, 92, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_BLACKSTONE));
                    placed += set(level, pos(c, Y - 2, dx, dz),
                            Blocks.DEEPSLATE_TILES.defaultBlockState());
                } else {
                    // 祭坛坑底 / 裂隙沟底
                    placed += clear(level, pos(c, Y, dx, dz));
                    placed += clear(level, pos(c, Y - 1, dx, dz));
                    placed += set(level, pos(c, Y - 2, dx, dz),
                            rift ? riftFloor(dx, dz) : pitFloor(dx, dz));
                }
            }
        }
        return placed;
    }

    /** 顶层材质：外圈 6 格腐化菌丝/黑石；内圈 34 格黑石/深板岩拼纹；中央 12 格是祭坛面。 */
    private static BlockState topMaterial(double r, int dx, int dz) {
        if (r > R - 6) {
            // 外圈 6 格：菌丝 + 黑石交错（腐化压边）
            int h = Math.floorMod(hash(dx, 3, dz), 7);
            if (h == 0) {
                return Blocks.MYCELIUM.defaultBlockState();
            }
            if (h <= 2) {
                return Blocks.BLACKSTONE.defaultBlockState();
            }
            return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (r <= ALTAR_R) {
            // 中央祭坛面：抛光黑石 / 深板岩瓦 交替 + 少量镀金黑石纹
            int band = (int) (r / 3.0D);
            int h = Math.floorMod(hash(dx, 5, dz), 11);
            if (h == 0) {
                return Blocks.GILDED_BLACKSTONE.defaultBlockState();
            }
            return Math.floorMod(band, 2) == 0
                    ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                    : Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        // 内圈 34 格：同心带 + 每 90° 一条深板岩砖辐射线
        double ang = Math.atan2(dz, dx);
        int sector = (int) Math.floor((ang + Math.PI) / (Math.PI / 12.0D));
        if (Math.floorMod(sector, 6) == 0) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        int band = ((int) (r / 5.0D)) % 3;
        return switch (band) {
            case 0 -> Blocks.BLACKSTONE.defaultBlockState();
            case 1 -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            default -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        };
    }

    /** 祭坛坑底材质（黑石 / 深板岩瓦，带少量哭泣黑曜石「泪渍」）。 */
    private static BlockState pitFloor(int dx, int dz) {
        if (Math.floorMod(hash(dx, 7, dz), 13) == 0) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        return mix(dx, dz, 93, Blocks.BLACKSTONE, Blocks.DEEPSLATE_TILES);
    }

    /** 裂隙沟底：黑曜 / 黑石。 */
    private static BlockState riftFloor(int dx, int dz) {
        return mix(dx, dz, 94, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.BLACKSTONE);
    }

    /** 是否落在 45°/135°/225°/315° 四条径向裂隙上（沟宽约 3 格）。 */
    private static boolean isRift(int dx, int dz) {
        double a = Math.abs(dx - dz) / Math.sqrt(2.0D);   // 45° / 225°
        double b = Math.abs(dx + dz) / Math.sqrt(2.0D);   // 135° / 315°
        return Math.min(a, b) <= 1.05D;
    }

    /** 三级祭坛的四座装饰祭台（在祭坛最外一级的 4 个正方向，避开 45° 裂隙）。 */
    private static int altarFocus(ServerLevel level, BlockPos c) {
        int placed = 0;
        int[][] spots = {{10, 0}, {-10, 0}, {0, 10}, {0, -10}};
        for (int[] k : spots) {
            placed += set(level, pos(c, Y + 1, k[0], k[1]),
                    Blocks.GILDED_BLACKSTONE.defaultBlockState());
            placed += set(level, pos(c, Y + 2, k[0], k[1]),
                    Blocks.SOUL_LANTERN.defaultBlockState());
            // 祭台外侧压 1 格黑石，避免灯笼孤零零一根
            placed += set(level, pos(c, Y, k[0], k[1]),
                    Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        }
        return placed;
    }

    // ------------------------------------------------------------------
    // 边缘垛口 / 云海 / 巨眼
    // ------------------------------------------------------------------

    /** 边缘：1 高基座 + 3 高菌丝/黑石垛口（裂隙出口处断开）。 */
    private static int rim(ServerLevel level, BlockPos c) {
        int placed = 0;
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > R * R || d2 < (R - 2) * (R - 2)) {
                    continue;
                }
                // 裂隙冲出边缘的位置留缺口（否则矮垛会「浮」在沟上）
                if (isRift(dx, dz)) {
                    continue;
                }
                BlockState base = Math.floorMod(hash(dx, 11, dz), 5) == 0
                        ? Blocks.MYCELIUM.defaultBlockState()
                        : Blocks.BLACKSTONE.defaultBlockState();
                placed += set(level, pos(c, Y + 1, dx, dz), base);
                // 垛口：32 个齿，每个齿 2 格高（总高 3 格）
                double ang = Math.atan2(dz, dx);
                int tooth = (int) Math.floor((ang + Math.PI) / (Math.PI / 32.0D));
                if (Math.floorMod(tooth, 2) == 0) {
                    placed += set(level, pos(c, Y + 2, dx, dz), base);
                    placed += set(level, pos(c, Y + 3, dx, dz),
                            Blocks.MYCELIUM.defaultBlockState());
                }
            }
        }
        return placed;
    }

    /**
     * 云海：y=234~238、半径 42~70 的环带，错层白云（2~3 层，白羊毛 / 白色染色玻璃混搭）。
     *
     * <p>用两段 value-noise 拼出「成团」的云；全部在台面之下、圆台之外，
     * 既盖不到台面也不挡台面到巨眼之间的视线通道。</p>
     */
    private static int clouds(ServerLevel level, BlockPos c, RandomSource rnd) {
        int placed = 0;
        long salt = rnd.nextLong();
        for (int dx = -CLOUD_OUT; dx <= CLOUD_OUT; dx++) {
            for (int dz = -CLOUD_OUT; dz <= CLOUD_OUT; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r < CLOUD_IN || r > CLOUD_OUT) {
                    continue;
                }
                double n = valueNoise(dx / 13.0D, dz / 13.0D, salt) * 0.65D
                        + valueNoise(dx / 5.0D, dz / 5.0D, salt ^ 0x9E3779B97F4A7C15L) * 0.35D;
                if (n < 0.55D) {
                    continue;
                }
                int y0;
                int layers;
                if (n > 0.75D) {
                    y0 = CLOUD_Y0 + 1;      // 235，3 层
                    layers = 3;
                } else if (n > 0.66D) {
                    y0 = CLOUD_Y0 + 2;      // 236，2 层
                    layers = 2;
                } else {
                    y0 = CLOUD_Y0 + 3;      // 237，2 层
                    layers = 2;
                }
                for (int k = 0; k < layers; k++) {
                    int yy = y0 + k;
                    if (yy > CLOUD_Y1) {
                        break;
                    }
                    BlockState st = Math.floorMod(hash(dx, yy, dz), 4) == 0
                            ? Blocks.WHITE_STAINED_GLASS.defaultBlockState()
                            : Blocks.WHITE_WOOL.defaultBlockState();
                    placed += set(level, new BlockPos(c.getX() + dx, yy, c.getZ() + dz), st);
                }
            }
        }
        return placed;
    }

    /**
     * 巨眼：球心 (c.x, Y+62, c.z)、半径 16 的球壳（壳厚 2），瞳孔（黑石）朝正下。
     *
     * <p>球面方程 + 逐层壳生成；眼白 = 白色石英，虹膜 = 红混凝土 / 岩浆块，
     * 瞳孔 = 黑石 / 深板岩，另加一条条哭泣黑曜石「血泪痕」。
     * 允许悬空 —— 它是场景焦点；与台面之间没有实体阻挡，玩家在坑底抬头正对瞳孔。</p>
     *
     * @return 球壳实际方块数（任务预算 ≤ {@value #EYE_BUDGET}）
     */
    private static int eye(ServerLevel level, BlockPos c) {
        int placed = 0;
        int tears = 0;
        int cy = Y + EYE_DY;
        int r2 = EYE_R * EYE_R;
        for (int dx = -EYE_R; dx <= EYE_R; dx++) {
            for (int dy = -EYE_R; dy <= EYE_R; dy++) {
                for (int dz = -EYE_R; dz <= EYE_R; dz++) {
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r2) {
                        continue;
                    }
                    double dist = Math.sqrt(d2);
                    if (dist < EYE_R - 2.0D) {
                        continue;                       // 只做壳，内部空心
                    }
                    double down = -dy / dist;            // 1 = 正下方（瞳孔那边）
                    BlockState st;
                    if (down > 0.90D) {
                        // 瞳孔：黑色
                        st = mix(dx, dz, 95, Blocks.BLACKSTONE, Blocks.DEEPSLATE_TILES);
                    } else if (down > 0.74D) {
                        // 虹膜：红
                        st = Math.floorMod(hash(dx, dy, dz), 3) == 0
                                ? Blocks.MAGMA_BLOCK.defaultBlockState()
                                : Blocks.RED_CONCRETE.defaultBlockState();
                    } else {
                        // 眼白：白色石英（少量石英块做出云翳）
                        st = Math.floorMod(hash(dx, dy, dz), 6) == 0
                                ? Blocks.QUARTZ_BLOCK.defaultBlockState()
                                : Blocks.SMOOTH_QUARTZ.defaultBlockState();
                        // 血泪痕：下眼睑的哭泣黑曜石条纹
                        double phi = Math.atan2(dz, dx);
                        int sector = Math.floorMod(
                                (int) Math.round(phi / (Math.PI / 9.0D)), 6);
                        if (tears < 240 && sector == 0
                                && down > 0.25D && down < 0.72D
                                && Math.floorMod(hash(dx, dy, dz), 4) < 2) {
                            st = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                            tears++;
                        }
                    }
                    placed += set(level, new BlockPos(c.getX() + dx, cy + dy,
                            c.getZ() + dz), st);
                }
            }
        }
        return placed;
    }

    /** 旧版战场的白玻璃/云柱残留（只在旧存档升级时才有东西要清；新存档全是空气 = 0）。 */
    private static int clearLegacyRim(ServerLevel level, BlockPos c) {
        int placed = 0;
        for (int i = 0; i < 16; i++) {
            double a = 2.0D * Math.PI * i / 16.0D;
            int x = c.getX() + (int) (Math.cos(a) * (R - 1));
            int z = c.getZ() + (int) (Math.sin(a) * (R - 1));
            for (int h = 1; h <= 6; h++) {
                placed += clear(level, new BlockPos(x, Y + h, z));
            }
        }
        return placed;
    }

    // ------------------------------------------------------------------
    // 内容层（2026-11-02）：奖励箱 + 四角「眼窝」+ 血池 / 锁链装饰
    // ------------------------------------------------------------------

    /**
     * 恶之竞技场的内容层：2 宝箱 + 四角「眼窝」+ 2 处血池 + 2 根锁链桩。
     *
     * <p>坐标（{@code c.y = Y} 是台面方块层，站立格在 {@code Y+1}）：
     * 宝箱 {@code (±16, Y+1, 0)}；眼窝在正东南西北 r=26 的 3×3 眼形地砖；
     * 血池 2 处；锁链桩 2 根。全部避开 45° 四条裂隙与中央祭坛坑。</p>
     *
     * <p><b>幂等</b>：宝箱位置已经是容器（旧存档补过 / 玩家开过）→ 返回 0，不重复写陈设；
     * 只有「首次建 / 战场标记被挖掉后重建」时才会重新放回未开启的宝箱。</p>
     */
    private static int detail(ServerLevel level, BlockPos c) {
        int added = BossRewards.placeChests(level, c,
                new int[][]{{16, 1, 0}, {-16, 1, 0}},
                BossRewards.LOOT_EVIL, 0x4556494CL);
        if (added == 0) {
            return 0;
        }
        int placed = added;
        // 四角「眼窝」：黑石眼眶 + 红混凝土虹膜 + 岩浆块瞳孔 + 骨块眼睑 + 哭泣黑曜石血泪
        for (int[] k : new int[][]{{0, 26}, {26, 0}, {0, -26}, {-26, 0}}) {
            for (int ex = -1; ex <= 1; ex++) {
                for (int ez = -1; ez <= 1; ez++) {
                    BlockState st;
                    if (ex == 0 && ez == 0) {
                        st = Blocks.MAGMA_BLOCK.defaultBlockState();
                    } else if (Math.abs(ex) + Math.abs(ez) == 1) {
                        st = Blocks.RED_CONCRETE.defaultBlockState();
                    } else {
                        st = Blocks.BLACKSTONE.defaultBlockState();
                    }
                    placed += set(level, pos(c, Y + 1, k[0] + ex, k[1] + ez), st);
                }
            }
            placed += set(level, pos(c, Y + 2, k[0], k[1]),
                    Blocks.CRYING_OBSIDIAN.defaultBlockState());
            placed += set(level, pos(c, Y + 2, k[0], k[1] + 1),
                    Blocks.BONE_BLOCK.defaultBlockState());
        }
        // 2 处血池（黑石池沿 + 红混凝土池面 + 少量哭泣黑曜石）
        for (int[] k : new int[][]{{28, 12}, {-28, -12}}) {
            for (int ex = -1; ex <= 1; ex++) {
                for (int ez = -1; ez <= 1; ez++) {
                    boolean center = ex == 0 && ez == 0;
                    BlockState st = center
                            ? Blocks.RED_CONCRETE.defaultBlockState()
                            : ((ex == 0 || ez == 0)
                            ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState());
                    placed += set(level, pos(c, Y + 1, k[0] + ex, k[1] + ez), st);
                }
            }
        }
        // 2 根锁链桩（黑石基座 + 垂挂锁链，作为「被锁住的祭品」剪影）
        for (int[] k : new int[][]{{0, 32}, {0, -32}}) {
            placed += set(level, pos(c, Y + 1, k[0], k[1]),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            placed += set(level, pos(c, Y + 2, k[0], k[1]),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            placed += set(level, pos(c, Y + 3, k[0], k[1]), Blocks.CHAIN.defaultBlockState());
            placed += set(level, pos(c, Y + 4, k[0], k[1]), Blocks.CHAIN.defaultBlockState());
        }
        return placed;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /**
     * <b>恶锚点悬停高度：台面上方 13 格</b>（19 号实机修复契约 §1）。
     *
     * <p>一阶段只有「一条手臂」，臂长向下 ≈{@code 157px×1.35/16 ≈ 13.25} 格；
     * 锚点悬空 13 格 → 指尖刚好探到台面附近（"从天空裂缝垂下巨臂"），
     * 既不会整条插进平台里（实机截图的问题），也不会看着像站在地上。</p>
     */
    public static final double HOVER_ABOVE = 13.0D;

    /** 降临起点：台面上方多少格（从天空裂缝压下来）。 */
    public static final int APPEAR_ABOVE = 45;

    /** 台面高度（玩家脚底 / 恶锚点基准 = {@code arenaCenter.y + 1}）。 */
    public static double floorY(BlockPos center) {
        return (center == null ? Y : center.getY()) + 1.0D;
    }

    /** 恶锚点悬停 y：战斗组（乙）每 tick 夹回坐标时直接调用。 */
    public static double hoverY(BlockPos center) {
        return floorY(center) + HOVER_ABOVE;
    }

    /** 恶降临起点 y。 */
    public static double appearStartY(BlockPos center) {
        return floorY(center) + APPEAR_ABOVE;
    }

    /**
     * 战斗组每 tick 夹回坐标用：把「恶」放回台面中心正上方的悬停点（保留朝向、锁重力）。
     *
     * <p>悬停点坐标在 {@link #spawnGazer} 时已写进实体 persistentData
     * （{@code evil_center_x/y/z}），实体自己 tick 里也能直接读，不依赖本方法的调用方记得传 center。</p>
     */
    public static void clampToHover(Entity entity, BlockPos center) {
        if (entity == null || center == null) {
            return;
        }
        entity.moveTo(center.getX() + 0.5D, hoverY(center), center.getZ() + 0.5D,
                entity.getYRot(), entity.getXRot());
        entity.setNoGravity(true);
    }

    /**
     * 生成「恶」（召唤演出专用）：刷在<b>战场中心正上方</b>、阶段 1、
     * <b>无重力 + 无 AI + 无敌</b>，打上 {@code evil_appearing} 标记；
     * {@code EvilSummon} 用 40t 把它从天空裂缝压到 {@link #hoverY} 的悬停点，
     * 之后整场战斗都悬在空中（由战斗组每 tick 夹回坐标）。
     *
     * @return 生成出来的「恶」（失败 null）；调用方负责降临动画
     */
    public static EvilGazerEntity spawnGazer(ServerLevel level, BlockPos center, ServerPlayer target) {
        var mob = ModEntities.EVIL_GAZER.get().create(level);
        if (mob == null) {
            return null;
        }
        // 19 号契约 §3.1：中心正上方（去掉旧版的 z-18 偏移），y = 台面 + APPEAR_ABOVE
        mob.moveTo(center.getX() + 0.5D, appearStartY(center), center.getZ() + 0.5D,
                0.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.setPhase(1);
        // 降临期间：锁重力/AI + 无敌（否则它会一边下落一边放技能，也可能被玩家提前打断演出）
        mob.setNoGravity(true);
        mob.setNoAi(true);
        mob.setInvulnerable(true);
        mob.getPersistentData().putBoolean("evil_appearing", true);
        // 悬停点写进实体 NBT：战斗组（乙）在 EvilGazerEntity.tick 里直接读它夹坐标，
        // 不依赖"实体自己知道战场中心"这种脆弱假设。
        mob.getPersistentData().putDouble("evil_center_x", center.getX() + 0.5D);
        mob.getPersistentData().putDouble("evil_center_y", hoverY(center));
        mob.getPersistentData().putDouble("evil_center_z", center.getZ() + 0.5D);
        level.addFreshEntity(mob);
        level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_ROAR,
                net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.5F);
        HongmengMod.LOGGER.info("[evil] 「恶」刷在战场中心正上方：({}, {}, {})（台面 {} / 悬停 {}）",
                mob.getX(), mob.getY(), mob.getZ(), floorY(center), hoverY(center));
        return mob;
    }

    private static BlockPos pos(BlockPos c, int y, int dx, int dz) {
        return new BlockPos(c.getX() + dx, y, c.getZ() + dz);
    }

    /** 放方块：未加载 / 越界的格子跳过，返回实际写入数。 */
    private static int set(ServerLevel level, BlockPos p, BlockState st) {
        if (!inWorld(level, p) || !level.isLoaded(p)) {
            return 0;
        }
        level.setBlock(p, st, 2);
        return 1;
    }

    /** 清成空气：本来就是空气就不写（老存档升级时才会计数）。 */
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

    /** 是否在世界高度范围内（越界的 setBlock 会被原版静默丢弃）。 */
    private static boolean inWorld(ServerLevel level, BlockPos p) {
        return p.getY() >= level.getMinBuildHeight() && p.getY() < level.getMaxBuildHeight();
    }

    /** 稳定的逐格哈希（同坐标永远同结果 → 纹理可复现）。 */
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

    /** 0~1 的稳定哈希值（value-noise 的格点随机数）。 */
    private static double hash01(int x, int z, long salt) {
        long h = x * 374761393L + z * 668265263L + salt;
        h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
        h = (h ^ (h >>> 33)) * 0xc4ceb9fe1a85ec53L;
        h = h ^ (h >>> 33);
        return (h >>> 11) * 0x1.0p-53;
    }

    /** 双线性插值的 value-noise（平滑过渡，云才会成团而不是噪点）。 */
    private static double valueNoise(double x, double z, long salt) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = x - x0;
        double fz = z - z0;
        double u = fx * fx * (3.0D - 2.0D * fx);
        double v = fz * fz * (3.0D - 2.0D * fz);
        double a = hash01(x0, z0, salt);
        double b = hash01(x0 + 1, z0, salt);
        double d = hash01(x0, z0 + 1, salt);
        double e = hash01(x0 + 1, z0 + 1, salt);
        return (a + (b - a) * u) + ((d + (e - d) * u) - (a + (b - a) * u)) * v;
    }
}
