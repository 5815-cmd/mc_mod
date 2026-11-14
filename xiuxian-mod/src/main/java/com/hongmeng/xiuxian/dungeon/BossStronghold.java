package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * <b>Boss 大本营的保护</b>（玩家要求）。
 *
 * <p>玩家原话：「这种建筑物如果是 boss 大本营，就设置一个<b>未击杀最终 boss 前无法破坏</b>」。</p>
 *
 * <h2>为什么用"区域白名单"而不是给每个方块打标记</h2>
 * <p>大本营有 2500~9700 个方块，逐个写 persistentData 会非常重（而且方块没有
 * persistentData 这个概念）。所以这里**按区域判定**：</p>
 * <ul>
 *   <li>区域由 {@link DungeonLandmark#center(int)} 与三个尺寸常量确定，
 *       是一个 48×34×16 的长方体；</li>
 *   <li>任何破坏方块的路径（法术、爆炸、玩家挖掘）只要命中这个长方体就**拒绝**；</li>
 *   <li><b>击杀最终首领后解除</b>：{@link #unlock} 会在该维度记一个标记。</li>
 * </ul>
 *
 * <p>与本项目里 {@code ServerProtections} 的口径一致：**只保护，不弹窗骂人** ——
 * 玩家挖不动自然会明白。</p>
 */
public final class BossStronghold {

    /** 已解锁的维度（击杀最终首领后写入）。 */
    private static final java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>>
            UNLOCKED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private BossStronghold() {
    }

    /** 大本营区域（含高度范围），由 {@link DungeonLandmark} 的常量推导。 */
    public static boolean inside(int floorY, int x, int y, int z) {
        BlockPos c = DungeonLandmark.center(floorY);
        // 比建筑本体略放宽 2 格，避免"贴着墙的方块能挖、墙不能挖"的怪现象
        int halfX = 26;
        int halfZ = 19;
        int top = 18;
        return Math.abs(x - c.getX()) <= halfX
                && Math.abs(z - c.getZ()) <= halfZ
                && y >= c.getY() - 3 && y <= c.getY() + top;
    }

    /**
     * 这个位置现在能不能被破坏。
     *
     * @param level  所在维度
     * @param floorY 该副本的地面高度
     * @return true = 允许破坏
     */
    public static boolean canBreak(ServerLevel level, int floorY, BlockPos pos) {
        if (level == null || pos == null) {
            return true;
        }
        try {
            if (!(level.dimension().location().getPath().contains("realm"))) {
                return true;      // 不是副本维度，不管
            }
            if (UNLOCKED.contains(level.dimension())) {
                return true;      // 已击杀最终首领，解除保护
            }
            return !inside(floorY, pos.getX(), pos.getY(), pos.getZ());
        } catch (Throwable t) {
            return true;
        }
    }

    /** 一个方块是否属于"大本营本体"（用材质快速判定，用于日志/调试）。 */
    public static boolean isLandmarkBlock(Block b) {
        return b == Blocks.POLISHED_BLACKSTONE_BRICKS || b == Blocks.CRIMSON_HYPHAE
                || b == Blocks.MOSSY_STONE_BRICKS || b == Blocks.SMOOTH_QUARTZ
                || b == Blocks.BONE_BLOCK || b == Blocks.OBSIDIAN
                || b == Blocks.CRYING_OBSIDIAN || b == Blocks.HAY_BLOCK;
    }

    /** 击杀最终首领后解除该维度的保护。 */
    public static void unlock(ServerLevel level) {
        if (level == null) {
            return;
        }
        UNLOCKED.add(level.dimension());
        HongmengMod.LOGGER.info("[arena] {} 最终首领已被击杀，大本营保护解除",
                level.dimension().location());
    }

    public static void clear() {
        UNLOCKED.clear();
    }
}
