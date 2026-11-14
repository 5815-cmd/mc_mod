package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>副本专属建筑</b>：每个副本入口处一座符合它主题的大本营。
 *
 * <p>玩家要求：「进副本这里要有对应副本的建筑，比如血魔就是血魔教的大本营」。</p>
 *
 * <p>建在竞技场北侧（z 为负方向），体量 48×34×16。四座风格完全不同 ——
 * 黑石殿基 / 苔石神庙 / 骨堆围栏 / 黑曜尖碑，一眼能认出进的是哪个副本。</p>
 *
 * <p>幂等：靠"中心那根主柱是否已是目标方块"判断是否已建，重复调用不会重复施工。
 * 失败只记日志、不影响副本本身能玩（与 {@code DungeonArena} 一个口径）。</p>
 */
public final class DungeonLandmark {

    /** 建筑中心相对竞技场中心的偏移（北侧）。 */
    // **紧贴竞技场北缘正前方**：原来放在 -(BUILD_R + 46) = 北侧 70 格外，
    // 玩家传送进来看不到（截图里正对南边）。现在挪到北缘外 8 格，
    // 并在传送时把朝向转向它（见 DungeonRealm）。
    private static final int ORIGIN_Z = -(DungeonArena.BUILD_R + 8);

    /** 建筑半宽 / 半长 / 高度。 */
    private static final int HALF_X = 24;
    private static final int HALF_Z = 17;
    private static final int HEIGHT = 16;

    private DungeonLandmark() {
    }

    /** 建筑中心（世界坐标）。 */
    public static BlockPos center(int floorY) {
        return new BlockPos(0, floorY, ORIGIN_Z);
    }

    /**
     * 建这座大本营（幂等）。
     *
     * @param floorY 地面高度（站人层）
     * @return 实际放置的方块数（0 = 已经建过）
     */
    public static int build(ServerLevel level, Dungeon dungeon, int floorY) {
        BlockPos c = center(floorY);
        // 幂等判据：主柱顶点（中式大殿的正脊位置）
        if (level.getBlockState(c.offset(0, HEIGHT - 1, 0)).is(coreOf(dungeon))) {
            return 0;
        }
        int placed = 0;
        try {
            // ---- 中式大殿（玩家要求：图二三五六那种古建风格）----
            //
            // 四座大本营形制相同、只是配色与主材不同，所以走同一个参数化生成器
            // （见 ChineseHall）：台基 + 柱网 + 斗拱 + 重檐 + 飞檐翘角 + 正脊鸱吻 + 匾额。
            //
            // 为什么保留旧的四个方法：中式大殿是"主殿"，旧方法建的是**配殿/院落**
            // （血池祭坛、日晷、兽栏、方尖碑），两者叠在一起才像一座完整的"大本营"。
            placed += ChineseHall.build(level, c, ChineseHall.styleFor(dungeon),
                    HALF_X - 4, HALF_Z - 3, dungeon == Dungeon.DEMON ? 4 : 3);
            placed += switch (dungeon) {
                case DEMON -> bloodCult(level, c);
                case EPOCH -> epochTemple(level, c);
                case BEAST -> beastLair(level, c);
                case VOID -> voidObelisk(level, c);
            };
            HongmengMod.LOGGER.info("[landmark] {} 大本营已建成：{} 格，中心 {}",
                    dungeon.dimensionId(), placed, c);
        } catch (Throwable t) {
            HongmengMod.LOGGER.error("[landmark] {} 大本营生成失败（不影响副本可玩）",
                    dungeon.dimensionId(), t);
        }
        return placed;
    }

    /** 幂等判据用的方块（每座建筑的主柱材质）。 */
    private static net.minecraft.world.level.block.Block coreOf(Dungeon dungeon) {
        return switch (dungeon) {
            case DEMON -> Blocks.CRIMSON_HYPHAE;
            case EPOCH -> Blocks.CHISELED_QUARTZ_BLOCK;
            case BEAST -> Blocks.BONE_BLOCK;
            case VOID -> Blocks.OBSIDIAN;
        };
    }

    // ------------------------------------------------------------------
    // 四座建筑
    // ------------------------------------------------------------------

    /** **血魔教大本营**：黑石殿基 + 绯红柱廊 + 血池祭坛 + 旗幡 + 火盆。 */
    private static int bloodCult(ServerLevel level, BlockPos c) {
        int n = 0;
        // 殿基（两层台阶）
        n += box(level, c, -HALF_X - 2, -1, -HALF_Z - 2, HALF_X + 2, 0, HALF_Z + 2,
                Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        n += box(level, c, -HALF_X - 1, 1, -HALF_Z - 1, HALF_X + 1, 1, HALF_Z + 1,
                Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        // 主殿墙（中空）
        for (int y = 2; y <= 10; y++) {
            n += ring(level, c, -HALF_X, -HALF_Z, HALF_X, HALF_Z, y,
                    y % 4 == 0 ? Blocks.CRIMSON_HYPHAE.defaultBlockState()
                            : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        }
        // 柱廊（绯红木柱，正面留门）
        for (int x = -HALF_X; x <= HALF_X; x += 6) {
            for (int y = 11; y <= HEIGHT - 1; y++) {
                if (Math.abs(x) <= 3) {
                    continue;       // 正门
                }
                set(level, c.offset(x, y, -HALF_Z), Blocks.CRIMSON_HYPHAE.defaultBlockState());
                set(level, c.offset(x, y, HALF_Z), Blocks.CRIMSON_HYPHAE.defaultBlockState());
                n += 2;
            }
        }
        // 屋顶（绯红木板 + 黑石压边）
        n += box(level, c, -HALF_X, HEIGHT, -HALF_Z, HALF_X, HEIGHT, HALF_Z,
                Blocks.CRIMSON_PLANKS.defaultBlockState());
        n += ring(level, c, -HALF_X - 1, -HALF_Z - 1, HALF_X + 1, HALF_Z + 1, HEIGHT,
                Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        // 中央血池祭坛
        n += box(level, c, -4, 2, -4, 4, 2, 4, Blocks.BLACKSTONE.defaultBlockState());
        n += box(level, c, -3, 3, -3, 3, 3, 3, Blocks.REDSTONE_BLOCK.defaultBlockState());
        n += box(level, c, -2, 4, -2, 2, 4, 2, Blocks.CRIMSON_HYPHAE.defaultBlockState());
        set(level, c.offset(0, 5, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        n += 78;
        // 火盆四角
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos p = c.offset(sx * (HALF_X - 3), 2, sz * (HALF_Z - 3));
                set(level, p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                set(level, p.above(), Blocks.CAMPFIRE.defaultBlockState());
                n += 2;
            }
        }
        // 旗幡（正面两侧）
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int y = 12; y <= HEIGHT - 1; y++) {
                set(level, c.offset(sx * 10, y, -HALF_Z), Blocks.CRIMSON_STEM.defaultBlockState());
            }
            set(level, c.offset(sx * 10, HEIGHT - 2, -HALF_Z + 1), Blocks.RED_WOOL.defaultBlockState());
            set(level, c.offset(sx * 10, HEIGHT - 3, -HALF_Z + 1), Blocks.RED_WOOL.defaultBlockState());
            n += 6;
        }
        return n;
    }

    /** **纪元祭坛神庙**：苔石与石英 + 四方立柱 + 中央日晷 + 壁龛。 */
    private static int epochTemple(ServerLevel level, BlockPos c) {
        int n = 0;
        n += box(level, c, -HALF_X - 2, -1, -HALF_Z - 2, HALF_X + 2, 0, HALF_Z + 2,
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        n += box(level, c, -HALF_X - 1, 1, -HALF_Z - 1, HALF_X + 1, 1, HALF_Z + 1,
                Blocks.STONE_BRICKS.defaultBlockState());
        for (int y = 2; y <= 8; y++) {
            n += ring(level, c, -HALF_X, -HALF_Z, HALF_X, HALF_Z, y,
                    y == 5 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                            : Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        }
        // 四方立柱（石英）
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = 9; y <= HEIGHT; y++) {
                    set(level, c.offset(sx * (HALF_X - 1), y, sz * (HALF_Z - 1)),
                            Blocks.QUARTZ_PILLAR.defaultBlockState());
                    n++;
                }
            }
        }
        // 屋顶
        n += box(level, c, -HALF_X, HEIGHT, -HALF_Z, HALF_X, HEIGHT, HALF_Z,
                Blocks.SMOOTH_QUARTZ.defaultBlockState());
        n += ring(level, c, -HALF_X - 1, -HALF_Z - 1, HALF_X + 1, HALF_Z + 1, HEIGHT,
                Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        // 中央日晷（石英台 + 金顶）
        n += box(level, c, -3, 2, -3, 3, 3, 3, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        set(level, c.offset(0, 4, 0), Blocks.GOLD_BLOCK.defaultBlockState());
        set(level, c.offset(0, 5, 0), Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
        n += 33;
        // 壁龛（书架）
        for (int x = -HALF_X + 4; x <= HALF_X - 4; x += 4) {
            set(level, c.offset(x, 4, -HALF_Z), Blocks.BOOKSHELF.defaultBlockState());
            set(level, c.offset(x, 4, HALF_Z), Blocks.BOOKSHELF.defaultBlockState());
            n += 2;
        }
        return n;
    }

    /** **妖兽巢穴围栏**：骨堆围墙 + 兽皮棚 + 篝火 + 悬骨门梁。 */
    private static int beastLair(ServerLevel level, BlockPos c) {
        int n = 0;
        n += box(level, c, -HALF_X - 2, -1, -HALF_Z - 2, HALF_X + 2, 0, HALF_Z + 2,
                Blocks.COARSE_DIRT.defaultBlockState());
        // 骨堆围墙
        for (int y = 1; y <= 5; y++) {
            n += ring(level, c, -HALF_X, -HALF_Z, HALF_X, HALF_Z, y,
                    y == 3 ? Blocks.BONE_BLOCK.defaultBlockState()
                            : Blocks.DEEPSLATE_TILES.defaultBlockState());
        }
        // 兽皮棚（干草顶）
        n += box(level, c, -HALF_X + 2, 6, -HALF_Z + 2, HALF_X - 2, 6, HALF_Z - 2,
                Blocks.HAY_BLOCK.defaultBlockState());
        n += box(level, c, -HALF_X + 3, 7, -HALF_Z + 3, HALF_X - 3, 7, HALF_Z - 3,
                Blocks.DRIED_KELP_BLOCK.defaultBlockState());
        // 悬骨门梁
        for (int x = -4; x <= 4; x++) {
            set(level, c.offset(x, 7, -HALF_Z), Blocks.BONE_BLOCK.defaultBlockState());
            n++;
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int y = 1; y <= 6; y++) {
                set(level, c.offset(sx * 4, y, -HALF_Z), Blocks.BONE_BLOCK.defaultBlockState());
                n++;
            }
        }
        // 篝火与骨堆
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos p = c.offset(sx * 12, 1, sz * 8);
                set(level, p.below(), Blocks.DEEPSLATE_TILES.defaultBlockState());
                set(level, p, Blocks.CAMPFIRE.defaultBlockState());
                set(level, p.offset(sx * 3, 1, 0), Blocks.BONE_BLOCK.defaultBlockState());
                n += 3;
            }
        }
        return n;
    }

    /** **虚空方尖碑**：黑曜石尖碑 + 悬浮碎裂平台 + 末地烛 + 裂隙环。 */
    private static int voidObelisk(ServerLevel level, BlockPos c) {
        int n = 0;
        // 碎裂的悬浮平台（三圈不同高度）
        n += box(level, c, -HALF_X, 0, -HALF_Z, HALF_X, 0, HALF_Z,
                Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        n += ring(level, c, -HALF_X, -HALF_Z, HALF_X, HALF_Z, 1,
                Blocks.CRYING_OBSIDIAN.defaultBlockState());
        // 中央方尖碑（逐层收分）
        for (int y = 1; y <= HEIGHT + 4; y++) {
            int half = Math.max(1, 4 - y / 4);
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    set(level, c.offset(x, y, z),
                            (y % 3 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN)
                                    .defaultBlockState());
                    n++;
                }
            }
        }
        set(level, c.offset(0, HEIGHT + 5, 0), Blocks.END_ROD.defaultBlockState());
        n++;
        // 裂隙环（悬浮的黑曜石环）
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2.0D * i / 24.0D;
            int x = (int) Math.round(Math.cos(a) * 18.0D);
            int z = (int) Math.round(Math.sin(a) * 13.0D);
            set(level, c.offset(x, 6 + (i % 3), z), Blocks.OBSIDIAN.defaultBlockState());
            n++;
        }
        // 末地烛四角
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos p = c.offset(sx * 15, 1, sz * 10);
                set(level, p, Blocks.PURPUR_PILLAR.defaultBlockState());
                set(level, p.above(), Blocks.END_ROD.defaultBlockState());
                n += 2;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // 基础操作
    // ------------------------------------------------------------------

    /** 以建筑中心为原点放一个实心长方体。 */
    private static int box(ServerLevel level, BlockPos c, int x0, int y0, int z0,
                           int x1, int y1, int z1, BlockState st) {
        int n = 0;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    set(level, c.offset(x, y, z), st);
                    n++;
                }
            }
        }
        return n;
    }

    /** 放一圈空心矩形（墙）。 */
    private static int ring(ServerLevel level, BlockPos c, int x0, int z0, int x1, int z1,
                            int y, BlockState st) {
        int n = 0;
        for (int x = x0; x <= x1; x++) {
            set(level, c.offset(x, y, z0), st);
            set(level, c.offset(x, y, z1), st);
            n += 2;
        }
        for (int z = z0; z <= z1; z++) {
            set(level, c.offset(x0, y, z), st);
            set(level, c.offset(x1, y, z), st);
            n += 2;
        }
        return n;
    }

    private static void set(ServerLevel level, BlockPos p, BlockState st) {
        if (level.isLoaded(p)) {
            level.setBlock(p, st, 2);
        }
    }
}
