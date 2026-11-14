package com.hongmeng.xiuxian.realworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <b>腐化地带</b>（真实世界开启后缓慢蔓延的污染）。
 *
 * <h2>为什么这个功能一直没做</h2>
 * <p>玩家很早就要过"肉后腐化扩散"，但我一直压着 —— 因为它的默认实现
 * （泰拉瑞亚式）会<b>不可逆地吃掉玩家建筑</b>。这不是"难做"，是"做错了会毁存档"。</p>
 *
 * <h2>三条硬约束（这一版的设计核心）</h2>
 * <ol>
 *   <li><b>不转换"人工方块"</b>：只动天然地表（草/土/石/沙/砂砾/深板岩）。
 *       箱子里、墙上、地毯、玻璃、木板一律跳过；</li>
 *   <li><b>附近有人工痕迹就整列跳过</b>：5×5 水平范围内检测到任意"玩家常用方块"
 *       （木板/玻璃/砖/门/栅栏/箱子/火把/工作台…）就放弃这一列 ——
 *       宁可少腐化一片，也不能啃掉半间屋子；</li>
 *   <li><b>硬上限</b>：每次 tick 最多转换 {@value #MAX_PER_TICK} 块、
 *       蔓延半径最多 {@value #MAX_RADIUS} 格、种子最多 {@value #MAX_SEEDS} 个。</li>
 * </ol>
 *
 * <h2>为什么用原版方块</h2>
 * <p>用 {@code 草 -> 菌丝}、{@code 石 -> 黑石}、{@code 沙 -> 灵魂沙} 这类
 * <b>原版方块</b>，不新增注册项 —— 不新增 ID 就不会打乱存档，
 * 也不需要新贴图。</p>
 */
public final class Corruption {

    /** 每次 tick 全局最多转换的方块数。 */
    public static final int MAX_PER_TICK = 24;

    /** 单个种子的最大蔓延半径（格）。 */
    public static final int MAX_RADIUS = 48;

    /** 最多同时存在的种子数。 */
    public static final int MAX_SEEDS = 8;

    /** 蔓延间隔（tick）—— 10 秒一次，慢到玩家能感知"在长"但不烦。 */
    public static final int INTERVAL = 200;

    private static final Random RNG = new Random();

    /** 种子（世界坐标）。 */
    private static final List<BlockPos> SEEDS = new ArrayList<>();

    /** 受污染的天然方块 → 腐化后的方块。 */
    private static BlockState corruptOf(BlockState s) {
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT)) {
            return Blocks.MYCELIUM.defaultBlockState();
        }
        if (s.is(Blocks.STONE) || s.is(Blocks.ANDESITE)
                || s.is(Blocks.GRANITE) || s.is(Blocks.DIORITE)) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (s.is(Blocks.DEEPSLATE)) {
            return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (s.is(Blocks.SAND) || s.is(Blocks.RED_SAND)) {
            return Blocks.SOUL_SAND.defaultBlockState();
        }
        if (s.is(Blocks.GRAVEL)) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.FERN)) {
            return Blocks.AIR.defaultBlockState();
        }
        return null;
    }

    /** 判断方块是否"人工痕迹"（出现就不动这一列）。 */
    private static boolean artificial(BlockState s) {
        return s.is(BlockTags.PLANKS) || s.is(BlockTags.LOGS)
                || s.is(BlockTags.WOOL) || s.is(BlockTags.BEDS)
                || s.is(BlockTags.DOORS) || s.is(BlockTags.FENCES)
                || s.is(BlockTags.STAIRS) || s.is(BlockTags.SLABS)
                || s.is(BlockTags.CANDLES) || s.is(BlockTags.BUTTONS)
                || s.is(Blocks.GLASS) || s.is(Blocks.GLASS_PANE)
                || s.is(Blocks.BRICKS) || s.is(Blocks.STONE_BRICKS)
                || s.is(Blocks.CHEST) || s.is(Blocks.BARREL)
                || s.is(Blocks.FURNACE) || s.is(Blocks.CRAFTING_TABLE)
                || s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH)
                || s.is(Blocks.LANTERN) || s.is(Blocks.CAMPFIRE)
                || s.is(Blocks.BOOKSHELF) || s.is(Blocks.ANVIL)
                || s.is(Blocks.ENCHANTING_TABLE);
    }

    private Corruption() {
    }

    /** 加一个种子（去重 + 上限）。 */
    public static void addSeed(BlockPos pos) {
        if (pos == null || SEEDS.size() >= MAX_SEEDS) {
            return;
        }
        for (BlockPos s : SEEDS) {
            if (s.distSqr(pos) < 64.0D) {
                return;
            }
        }
        SEEDS.add(pos.immutable());
    }

    public static int seedCount() {
        return SEEDS.size();
    }

    public static void clear() {
        SEEDS.clear();
    }

    /**
     * 每 {@value #INTERVAL} tick 调一次（由 {@code ModEvents} 转发）。
     *
     * @return 本 tick 实际转换的块数
     */
    public static int tick(ServerLevel level) {
        if (level == null || SEEDS.isEmpty() || !RealWorld.active(level)) {
            return 0;
        }
        // ⚠ 世界刚加载时（前 30 秒）不推进：此时 Forge 的 LootModifierManager
        //   可能还没就绪，任何“方块被破坏 -> 掉落战利品”都会抛
        //   IllegalStateException: Can not retrieve LootModifierManager。
        //   我们改方块会连带让草/花掉落，属于同一类风险，直接跳过。
        if (level.getGameTime() < 600L) {
            return 0;
        }
        if (level.getGameTime() % INTERVAL != 0) {
            return 0;
        }
        int done = 0;
        for (BlockPos seed : SEEDS) {
            if (done >= MAX_PER_TICK) {
                break;
            }
            done += spreadFrom(level, seed, MAX_PER_TICK - done);
        }
        return done;
    }

    /** 从一个种子向外扩散。 */
    private static int spreadFrom(ServerLevel level, BlockPos seed, int budget) {
        int done = 0;
        for (int attempt = 0; attempt < budget * 6 && done < budget; attempt++) {
            double ang = RNG.nextDouble() * Math.PI * 2.0D;
            double r = 4.0D + RNG.nextDouble() * (MAX_RADIUS - 4.0D);
            int x = seed.getX() + (int) Math.round(Math.cos(ang) * r);
            int z = seed.getZ() + (int) Math.round(Math.sin(ang) * r);
            // 从种子高度上下扫 8 格找地表
            for (int dy = 8; dy >= -8; dy--) {
                BlockPos p = new BlockPos(x, seed.getY() + dy, z);
                if (!level.hasChunkAt(p)) {
                    break;
                }
                BlockState st = level.getBlockState(p);
                BlockState to = corruptOf(st);
                if (to == null) {
                    continue;
                }
                // 必须"上方是空气"（即它是地表，而不是埋在地里的石头）
                if (!level.getBlockState(p.above()).isAir()) {
                    continue;
                }
                if (nearArtificial(level, p)) {
                    continue;
                }
                level.setBlock(p, to, 2);
                done++;
                break;
            }
        }
        return done;
    }

    /** 5×5 水平范围、上下 3 格内有没有人工方块。 */
    private static boolean nearArtificial(ServerLevel level, BlockPos p) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    BlockPos q = new BlockPos(p.getX() + dx, p.getY() + dy,
                            p.getZ() + dz);
                    if (artificial(level.getBlockState(q))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 真实世界开启时，给玩家附近撒几个种子。 */
    public static void seedAround(ServerPlayer player, ServerLevel level) {
        if (player == null || level == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            addSeed(player.blockPosition().offset(
                    24 + RNG.nextInt(24) * (RNG.nextBoolean() ? 1 : -1),
                    0,
                    24 + RNG.nextInt(24) * (RNG.nextBoolean() ? 1 : -1)));
        }
    }
}
