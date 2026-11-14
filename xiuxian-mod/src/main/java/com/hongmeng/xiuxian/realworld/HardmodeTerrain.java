package com.hongmeng.xiuxian.realworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>真世界的地形变化</b>（参考泰拉瑞亚「肉后」——世界在你眼皮底下变了）。
 *
 * <h2>玩家要求</h2>
 * <blockquote>「地形变化（肉后新矿/新怪）」</blockquote>
 *
 * <h2>为什么不走 worldgen</h2>
 * <p>生物群系特征（{@code BiomeModifier}）在<b>世界生成时</b>就烘焙进区块了，
 * 而"真世界"是<b>运行时开关</b> —— worldgen 读不到这个状态。
 * 所以"新开区块才有新矿"的做法<必>不可能是"肉后"</必>：旧区块永远没矿。</p>
 *
 * <h2>这里怎么做：回溯式播种</h2>
 * <p>真世界开启后，<b>在玩家附近的地下把石头换成稀有矿</b>。
 * 这才是"肉后"的真正含义 —— 你脚下的世界变了，而不是"去远处挖新地"。</p>
 *
 * <p>安全约束（每一条都不能少）：</p>
 * <ul>
 *   <li>只替换<b>石头/深板岩/凝灰岩</b> —— 绝不碰玩家建筑；</li>
 *   <li>只在<b>已生成的区块</b>里改（不改未加载区块，不触发区块生成）；</li>
 *   <li>每玩家总量有上限（{@value #MAX_VEINS_PER_PLAYER} 条），避免把地下挖空；</li>
 *   <li>节流：每 {@value #SEED_INTERVAL} tick 最多播一条。</li>
 * </ul>
 */
public final class HardmodeTerrain {

    /** 每个玩家最多播多少条矿脉（一次真世界周期内）。 */
    public static final int MAX_VEINS_PER_PLAYER = 36;

    /** 播种间隔（tick）—— 20 秒一条，慢慢"渗透"而不是瞬间刷满。 */
    public static final int SEED_INTERVAL = 400;

    /** 一条矿脉最多几块。 */
    public static final int VEIN_SIZE = 4;

    /** 水平搜索半径（格）。 */
    public static final int SEARCH_RADIUS = 18;

    /** 竖直搜索深度（玩家脚下）。 */
    public static final int SEARCH_DEPTH = 26;

    /** 刷精英的间隔（tick）—— 60 秒一次，不能太频繁。 */
    public static final int ELITE_INTERVAL = 1200;

    /** 每个玩家最多刷几只（一次真世界周期内）。 */
    public static final int MAX_ELITE_PER_PLAYER = 6;

    /** 每玩家已刷精英数。 */
    private static final Map<UUID, Integer> ELITE_DONE = new ConcurrentHashMap<>();
    /** 每玩家下次允许刷的 tick。 */
    private static final Map<UUID, Long> ELITE_NEXT = new ConcurrentHashMap<>();

    /** 每玩家的播种计数。 */
    private static final Map<UUID, Integer> SEEDED = new ConcurrentHashMap<>();
    /** 每玩家下次允许播种的 tick。 */
    private static final Map<UUID, Long> NEXT = new ConcurrentHashMap<>();

    private HardmodeTerrain() {
    }

    /**
     * 每 tick 调用（由 ModEvents 的玩家 tick 驱动）。
     *
     * <p>只有真世界开启、且在主世界时才播种 —— 副本里改地形没有意义，
     * 而且副本有自己的一套资源逻辑。</p>
     */
    public static void tick(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!RealWorld.active(level)) {
            return;
        }
        long now = level.getGameTime();
        Long next = NEXT.get(player.getUUID());
        if (next != null && now < next) {
            return;
        }
        int done = SEEDED.getOrDefault(player.getUUID(), 0);
        if (done >= MAX_VEINS_PER_PLAYER) {
            return;
        }
        NEXT.put(player.getUUID(), now + SEED_INTERVAL);

        // 刷精英（独立节流，比播种慢得多）
        tickElite(level, player, now);

        int placed = seedOneVein(level, player);
        if (placed > 0) {
            SEEDED.put(player.getUUID(), done + 1);
            // 提示一次：玩家得知道"世界变了、脚下有新矿"
            if (done == 0) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "\u00a78\u00a7o大地深处传来一阵震颤……"), false);
            }
        }
    }

    /** 尝试在玩家附近播一条矿脉，返回放置的方块数。 */
    private static int seedOneVein(ServerLevel level, ServerPlayer player) {
        RandomSource rnd = level.getRandom();
        BlockPos base = player.blockPosition();
        Block ore = pickOre(rnd);
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = rnd.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int dz = rnd.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int dy = -(rnd.nextInt(SEARCH_DEPTH) + 2);
            BlockPos pos = base.offset(dx, dy, dz);
            // 只在已加载的区块里动手（不触发新区块生成）
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            if (pos.getY() < level.getMinBuildHeight() + 2) {
                continue;
            }
            if (!isReplaceableStone(level.getBlockState(pos))) {
                continue;
            }
            // 挖出一条小矿脉（相邻 3 块也尝试替换）
            int placed = 0;
            placed += replace(level, pos, ore);
            for (int k = 1; k < VEIN_SIZE; k++) {
                BlockPos near = pos.offset(rnd.nextInt(3) - 1, rnd.nextInt(3) - 1,
                        rnd.nextInt(3) - 1);
                if (level.hasChunkAt(near) && isReplaceableStone(level.getBlockState(near))) {
                    placed += replace(level, near, ore);
                }
            }
            if (placed > 0) {
                // 表现：一点尘与微光（暗示"地下有东西变了"）
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ASH,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        4, 0.3D, 0.3D, 0.3D, 0.0D);
                return placed;
            }
        }
        return 0;
    }

    /**
     * 按稀有度挑一种矿（越稀有越少）。
     *
     * <p>真元晶矿（{@code zhenyuan_ore}）= <b>肉后新矿</b>：它不在 worldgen 里，
     * 只能靠这里播种（见 {@code ModBlocks.ZHENYUAN_ORE} 的注释）。之前这一档
     * 漏接了 —— 矿方块注册了、掉落表也有，但世界上永远不会出现，
     * 等于「真元晶」只能从 Boss 掉落、矿脉是死内容。这里补上 12%。</p>
     */
    private static Block pickOre(RandomSource rnd) {
        int roll = rnd.nextInt(100);
        if (roll < 12) {
            return com.hongmeng.xiuxian.ModBlocks.ZHENYUAN_ORE.get();   // 12% 肉后新矿
        }
        if (roll < 20) {
            return com.hongmeng.xiuxian.ModBlocks.XUELUO_ORE.get();      // 8%
        }
        if (roll < 34) {
            return com.hongmeng.xiuxian.ModBlocks.KONGMING_ORE.get();    // 14%
        }
        return com.hongmeng.xiuxian.ModBlocks.SPIRIT_CRYSTAL_ORE.get();  // 66%
    }

    /** 只换"天然石头"。玩家建筑的砖、玻璃、箱子一律不碰。 */
    private static boolean isReplaceableStone(BlockState st) {
        Block b = st.getBlock();
        return b == Blocks.STONE || b == Blocks.DEEPSLATE || b == Blocks.TUFF
                || b == Blocks.GRANITE || b == Blocks.DIORITE || b == Blocks.ANDESITE
                || b == Blocks.CALCITE || b == Blocks.DRIPSTONE_BLOCK;
    }

    private static int replace(ServerLevel level, BlockPos pos, Block ore) {
        level.setBlock(pos, ore.defaultBlockState(), Block.UPDATE_ALL);
        return 1;
    }

    /**
     * 真世界的「新怪」：定期在玩家附近刷一只精英。
     *
     * <p>为什么不用 Boss 类型：那会把"真世界"变成"天天打 Boss"。
     * 只刷<b>普通怪种</b>，境界交给既有阶梯系统 ——
     * 所以"新怪"体现为<b>更强的同一批怪</b>，而不是"一堆从没见过的"。</p>
     *
     * <p>约束：60 秒一次、每人最多 6 只、只在主世界、只在玩家附近有地面的地方。</p>
     */
    private static void tickElite(ServerLevel level, ServerPlayer player, long now) {
        Long next = ELITE_NEXT.get(player.getUUID());
        if (next != null && now < next) {
            return;
        }
        int done = ELITE_DONE.getOrDefault(player.getUUID(), 0);
        if (done >= MAX_ELITE_PER_PLAYER) {
            return;
        }
        ELITE_NEXT.put(player.getUUID(), now + ELITE_INTERVAL);
        // 在玩家周围 12~20 格找一个"能站"的地面
        RandomSource rnd = level.getRandom();
        for (int attempt = 0; attempt < 12; attempt++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0D;
            double dist = 12.0D + rnd.nextDouble() * 8.0D;
            BlockPos base = BlockPos.containing(
                    player.getX() + Math.cos(ang) * dist,
                    player.getY(),
                    player.getZ() + Math.sin(ang) * dist);
            // 往上/下找地面
            BlockPos ground = null;
            for (int dy = 6; dy >= -8; dy--) {
                BlockPos p = base.offset(0, dy, 0);
                if (!level.hasChunkAt(p)) {
                    continue;
                }
                BlockPos below = p.below();
                if (level.getBlockState(p).isAir()
                        && level.getBlockState(below).isSolidRender(level, below)) {
                    ground = p;
                    break;
                }
            }
            if (ground == null) {
                continue;
            }
            if (spawnElite(level, ground)) {
                ELITE_DONE.put(player.getUUID(), done + 1);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "\u00a78\u00a7o有什么东西闻到了你的气息……"), false);
                return;
            }
        }
    }

    /** 重置（关掉真世界 / 换档时）。 */
    public static void reset() {
        SEEDED.clear();
        NEXT.clear();
        ELITE_DONE.clear();
        ELITE_NEXT.clear();
    }

    /**
     * 真世界的「新怪」：在玩家附近刷一只精英。
     *
     * <p>为什么不用 Boss 类型：那会把"真世界"变成"天天打 Boss"。
     * 这里只刷<b>普通怪种</b>，境界交给既有的阶梯系统（副本按维度、
     * 主世界按品阶），所以"新怪"体现为<b>更强的同一批怪</b>，
     * 而不是"一堆从没见过的"。</p>
     *
     * @param at 出生点（通常是玩家附近地面）
     */
    public static boolean spawnElite(ServerLevel level, BlockPos at) {
        if (level == null || at == null) {
            return false;
        }
        try {
            // 真世界专属：劫灰修士 45% / 堕仙 20%（第二梯队新怪，真世界唯一精英）/
            // 旧的血魔修士 35%。三种都是"更强的同一批怪"，不会把真世界变成 Boss 场。
            float r = level.getRandom().nextFloat();
            EntityType<?> type = r < 0.45F
                    ? com.hongmeng.xiuxian.entity.ModEntities
                    .TRIBULATION_ASH_CULTIVATOR.get()
                    : r < 0.65F
                            ? com.hongmeng.xiuxian.entity.ModEntitiesExtra2
                            .FALLEN_IMMORTAL.get()
                            : com.hongmeng.xiuxian.entity.ModEntities.BLOOD_DEMON_CULTIVATOR.get();
            return type.spawn(level, at, MobSpawnType.EVENT) != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
