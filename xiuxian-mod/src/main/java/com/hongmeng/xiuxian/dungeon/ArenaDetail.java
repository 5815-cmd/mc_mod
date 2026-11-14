package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>四座 boss 决战竞技场的「细节 pass」</b>（玩家要求：Boss 场地要完善，屋内有奖励箱）。
 *
 * <p>竞技场本体由 {@link ArenaBattle} 建（沙场 r=44、8 级看台到 r≈83、四向门、中央祭坛），
 * 但<b>看台空荡、没有侧室/宝库、没有奖励箱</b>。本类在场地建成之后，自己按 tick 分帧补四类内容：</p>
 *
 * <ol>
 *   <li><b>4 座看台宝库</b>：4 个对角线方向、看台第 1~2 级内部（r≈48~56）各挖一个
 *       <b>9×5×5</b> 侧室（外框 9 宽 × 5 深 × 5 高，内部净空 7×3×3），门洞朝场地中心
 *       （穿过第一级看台的缺口 + 门廊），每室 <b>2 个宝箱</b>（{@link BossRewards#arenaLootFor}，
 *       地面上一格），配王座/旗帜/烛台/藏宝堆；</li>
 *   <li><b>看台陈设</b>：每级隔 16 根柱一面主题战旗、每 8 根柱一只火盆、
 *       坐席每隔 4 根柱换主题色（贴地换色，不挡路）、顶层一圈战利品架；</li>
 *   <li><b>四门门楼</b>：门洞两侧石柱灯 + 门楣挂灯 + 门内 3×3 地毯；</li>
 *   <li><b>场地装饰</b>：中央祭坛后方（东门内 6 格）3 级胜利台座；
 *       四个对角 r≈30 各 1 座 3 格高主题雕像；中心 r&lt;30 只做贴地纹样（4 条对角金线 + 2 道环）。</li>
 * </ol>
 *
 * <h2>为什么是「自驱式 detail pass」</h2>
 * <p>不能改 {@link ArenaBattle}（文件所有权：本批只许新建本文件），所以这里自己挂
 * {@code @Mod.EventBusSubscriber} 监听 {@link TickEvent.LevelTickEvent}：</p>
 * <ul>
 *   <li>每 20 tick 检查一次，4 座维度错开 5 tick，避免同一 tick 连写四座；</li>
 *   <li>触发条件：本维度是 {@code arena_epoch/beast/demon/void} 之一 +
 *       {@link ArenaRealm#isBuilt(ServerLevel)}（竞技场本体已建）+
 *       本维度有玩家（旧存档不会在服务端启动时白加载 169 个区块）；</li>
 *   <li>独立 {@link SavedData}（数据键 {@value #DATA_KEY}）：存 <b>版本号 + 计划游标 + 已写方块数 + 已放箱数</b>，
 *       服务端重启后能从中断处继续，不会重复施工；</li>
 *   <li><b>每 tick ≤ {@value #BLOCKS_PER_TICK} 次方块写入、≤ {@value #CHESTS_PER_TICK} 个宝箱</b>，
 *       写不完下一 tick 继续；只写 {@code level.isLoaded(pos)} 的格子，未加载就暂停等下次；</li>
 *   <li>所有写入都只覆盖「空气 / 可替换方块 / 竞技场本体材料」——玩家自己搭的方块不覆盖，
 *       宝箱只放空气格（{@link BossRewards#ensureChest} 保证）。</li>
 * </ul>
 *
 * <h2>版本号</h2>
 * <p>⚠ 改了本类的任何几何/材料/顺序，必须把 {@link #VERSION} +1；否则老存档里
 * {@code version == 当前值} 会直接跳过补建（和 {@code ArenaRealm.GEN_VERSION} 同一个坑）。
 * 竞技场本体重建（{@code ArenaBattle} 改版）后本 pass 不会自动重跑，需要手动 bump {@link #VERSION}。</p>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID)
public final class ArenaDetail {

    /** 细节 pass 版本号（改了本类生成逻辑必须 +1）。 */
    public static final int VERSION = 1;

    /** SavedData 数据键（每个维度各自一份；与 ArenaRealm 的建成标记分开）。 */
    private static final String DATA_KEY = "hongmeng_arena_detail";

    /** 每 tick 最多写多少格（设计约定 §6）。 */
    private static final int BLOCKS_PER_TICK = 2000;
    /** 每 tick 最多放几个宝箱（设计约定 §6）。 */
    private static final int CHESTS_PER_TICK = 2;
    /** 每 tick 最多处理多少条计划（防止大量「空写」把一 tick 拖太长）。 */
    private static final int OPS_PER_TICK = 6000;
    /** 开工前预加载的区块半径（竞技场外沿 83 → chunk −6..6 覆盖方块 −96..111）。 */
    private static final int PRELOAD_CHUNK_R = 6;
    /** 宝箱战利品种子基值（与 /hongmeng bossreward 命令的种子同源，方便对账）。 */
    private static final long CHEST_SEED = 20261102L;

    // ------------------------------------------------------------------
    // 与 ArenaBattle 对齐的几何常量（那些是 private，这里只读对齐，不改本体）
    // ------------------------------------------------------------------
    /** 看台级数（ArenaBattle.TIERS）。 */
    private static final int TIERS = 8;
    /** 每级看台宽（ArenaBattle.TIER_W）。 */
    private static final int TIER_W = 5;
    /** 每级立柱数（ArenaBattle.PILLARS）。 */
    private static final int PILLARS = 32;
    /** 四向大门所在半径（ArenaBattle 用 FLOOR_R−1）。 */
    private static final int GATE_R = 43;

    // ------------------------------------------------------------------
    // 宝库模板（NE 象限的局部坐标，其余 3 座用 90° 旋转复制）
    // 外框 x 32..40（9 宽）× z 34..38（5 深）；dy 0 地板、1..3 墙/净空、4 天花
    // ------------------------------------------------------------------
    private static final int VAULT_X0 = 32;
    private static final int VAULT_Z0 = 34;
    private static final int VAULT_W = 9;
    private static final int VAULT_D = 5;
    private static final int VAULT_H = 5;

    /** 本会话内的施工计划缓存（维度 → 计划）；随机/几何全部确定性，重建结果一致。 */
    private static final Map<ResourceKey<Level>, List<Op>> PLANS = new ConcurrentHashMap<>();

    /** 允许被本 pass 覆盖/挖掉的方块（竞技场本体材料 + 四座维度地面材料）。 */
    private static final Set<Block> ARENA_MATERIALS = Set.of(
            // 四套看台/地面配色（ArenaBattle.styleFor）
            Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_BLOCK, Blocks.DEEPSLATE_TILES,
            Blocks.DEEPSLATE_BRICKS, Blocks.GOLD_BLOCK, Blocks.LANTERN,
            Blocks.COARSE_DIRT, Blocks.PACKED_MUD, Blocks.MOSSY_STONE_BRICKS,
            Blocks.STRIPPED_SPRUCE_LOG, Blocks.EMERALD_BLOCK,
            Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.RED_NETHER_BRICKS,
            Blocks.CRIMSON_HYPHAE, Blocks.SOUL_LANTERN,
            Blocks.END_STONE, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK,
            Blocks.CRYING_OBSIDIAN, Blocks.AMETHYST_BLOCK,
            // 四座竞技场维度的 flat 地面层
            Blocks.DEEPSLATE, Blocks.POLISHED_DEEPSLATE, Blocks.STONE,
            Blocks.NETHERRACK, Blocks.POLISHED_BLACKSTONE);

    private ArenaDetail() {
    }

    // ==================================================================
    // 事件驱动：每个维度每 20 tick 推进一片
    // ==================================================================

    /** 决战竞技场细节 pass 的驱动入口（Forge 事件总线，END 阶段）。 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        Dungeon dungeon = arenaDungeon(level);
        if (dungeon == null || !ArenaRealm.isBuilt(level)) {
            return;
        }
        // 20 tick 检查一次；四座维度错开 5 tick，避免同一 tick 连写四座
        if ((level.getGameTime() + dungeon.ordinal() * 5L) % 20L != 0L) {
            return;
        }
        // 没有玩家在场就不加载区块、不施工（旧存档第一次进竞技场时再补）
        if (level.players().isEmpty()) {
            return;
        }
        try {
            advance(level, dungeon);
        } catch (Throwable t) {
            // 细节只是内容层：失败绝不打断战斗/进维度
            HongmengMod.LOGGER.warn("[detail] arena_{} 细节 pass 异常（不影响战斗）：{}",
                    shortName(dungeon), t.toString());
        }
    }

    /**
     * <b>验收/救档用：忽略“维度内有玩家”门槛，把本维度的细节 pass 强制跑完。</b>
     *
     * <p>正常玩法由 {@code LevelTickEvent} 自驱（有玩家才施工，避免旧存档开服时
     * 白加载 4×169 区块）；headless RCON 没有玩家，需要这个入口给服务端验收/救档用。
     * 会在调用线程里循环 {@link #advance} 直到版本完成（最多 64 轮保护）。</p>
     *
     * @return 本次累计写入方块数（已完成的返回 0）
     */
    public static int forceRun(ServerLevel level, Dungeon dungeon) {
        if (level == null || dungeon == null) {
            return 0;
        }
        try {
            DetailFlag flag = flag(level);
            if (flag.version >= VERSION) {
                return 0;
            }
            for (int guard = 0; guard < 64 && flag.version < VERSION; guard++) {
                advance(level, dungeon);
                flag = flag(level);
            }
            HongmengMod.LOGGER.info("[detail] arena_{} 强制细节完成：{} 方块，{} 宝箱",
                    shortName(dungeon), flag.blocks, flag.chests);
            return flag.blocks;
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[detail] arena_{} 强制细节失败：{}",
                    shortName(dungeon), t.toString());
            return 0;
        }
    }

    /** 推进本维度的一片施工（最多 2000 方块 / 2 宝箱）。 */
    private static void advance(ServerLevel level, Dungeon dungeon) {
        DetailFlag flag = flag(level);
        if (flag.version >= VERSION) {
            return;
        }
        // 版本变了（本类改过）：旧游标与旧计划全部作废，从头发车
        if (flag.cursorVersion != VERSION) {
            flag.cursorVersion = VERSION;
            flag.cursor = 0;
            flag.blocks = 0;
            flag.chests = 0;
            flag.setDirty();
            PLANS.remove(level.dimension());
        }
        List<Op> plan = PLANS.computeIfAbsent(level.dimension(),
                k -> buildPlan(level, dungeon));
        if (flag.cursor <= 0 && flag.blocks <= 0) {
            preloadArenaChunks(level);
            HongmengMod.LOGGER.info("[detail] arena_{} 细节施工开始：计划 {} 项",
                    shortName(dungeon), plan.size());
        }

        int writes = 0;
        int chests = 0;
        int start = flag.cursor;
        int i = start;
        for (; i < plan.size(); i++) {
            if (writes >= BLOCKS_PER_TICK || chests >= CHESTS_PER_TICK
                    || i - start >= OPS_PER_TICK) {
                break;
            }
            Op op = plan.get(i);
            BlockPos pos = new BlockPos(op.x(), op.y(), op.z());
            if (!level.isLoaded(pos)) {
                // 区块没加载：原地暂停（玩家大概率离开了维度），下次进来继续
                break;
            }
            int r = apply(level, op);
            if (r > 0) {
                writes++;
                if (op.loot() != null) {
                    chests++;
                }
            }
        }
        flag.cursor = i;
        flag.blocks += writes;
        flag.chests += chests;
        flag.setDirty();

        if (i >= plan.size()) {
            flag.version = VERSION;
            flag.setDirty();
            PLANS.remove(level.dimension());        // 施工完成，释放计划缓存
            HongmengMod.LOGGER.info("[detail] arena_{} 细节完成：{} 方块，{} 宝箱",
                    shortName(dungeon), flag.blocks, flag.chests);
        } else {
            HongmengMod.LOGGER.debug("[detail] arena_{} 进度 {}/{}（本 tick {} 方块 / {} 宝箱）",
                    shortName(dungeon), i, plan.size(), writes, chests);
        }
    }

    /** 真正写一格；返回 1=写了，0=跳过（已有/被玩家占用），-1=区块未加载（暂停）。 */
    private static int apply(ServerLevel level, Op op) {
        BlockPos pos = new BlockPos(op.x(), op.y(), op.z());
        if (op.loot() != null) {
            return BossRewards.ensureChest(level, pos, op.loot(), CHEST_SEED ^ pos.asLong())
                    ? 1 : 0;
        }
        BlockState cur = level.getBlockState(pos);
        if (op.carve()) {
            if (cur.isAir() || !ARENA_MATERIALS.contains(cur.getBlock())) {
                return 0;                       // 已经是空气 / 玩家方块：不挖
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return 1;
        }
        if (!cur.isAir() && !cur.canBeReplaced()
                && !ARENA_MATERIALS.contains(cur.getBlock())) {
            return 0;                           // 玩家方块：不覆盖
        }
        level.setBlock(pos, op.state(), 2);
        return 1;
    }

    /**
     * 开工前把竞技场外沿（r≈83）需要的区块全部加载。
     *
     * <p>正常情况下 {@code ArenaRealm.enter} 已经加载过 chunk −6..6，这里是兜底
     * （玩家从旧存档登录/命令直接传送等路径），只加载一次，之后所有 write 都有
     * {@code isLoaded} 保护。</p>
     */
    private static void preloadArenaChunks(ServerLevel level) {
        long start = System.currentTimeMillis();
        int cx = ArenaRealm.CENTER_X >> 4;
        int cz = ArenaRealm.CENTER_Z >> 4;
        int n = 0;
        for (int dx = -PRELOAD_CHUNK_R; dx <= PRELOAD_CHUNK_R; dx++) {
            for (int dz = -PRELOAD_CHUNK_R; dz <= PRELOAD_CHUNK_R; dz++) {
                level.getChunk(cx + dx, cz + dz, ChunkStatus.FULL, true);
                n++;
            }
        }
        HongmengMod.LOGGER.info("[detail] arena 预加载 {} 区块，耗时 {} ms",
                n, System.currentTimeMillis() - start);
    }

    // ==================================================================
    // 计划（确定性；同一维度任何一次重建结果一致）
    // ==================================================================

    /** 本维度对应的 dungeon（不是竞技场维度返回 null）。 */
    private static Dungeon arenaDungeon(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        for (Dungeon d : Dungeon.values()) {
            if (ArenaRealm.keyOf(d) != null && ArenaRealm.keyOf(d).equals(key)) {
                return d;
            }
        }
        return null;
    }

    /** 构建一座竞技场的全部细节（方块清单 + 宝箱清单）。 */
    private static List<Op> buildPlan(ServerLevel level, Dungeon dungeon) {
        List<Op> ops = new ArrayList<>(4096);
        int fy = floorTopY(level);
        Theme th = themeOf(dungeon);
        buildVaults(ops, fy, dungeon, th);
        buildStands(ops, fy, th);
        buildGates(ops, fy, th);
        buildVictoryDais(ops, fy, th);
        buildStatues(ops, fy, th);
        buildCenterPattern(ops, fy, th);
        return ops;
    }

    /**
     * 竞技场地面「顶方块」的 y。
     *
     * <p>{@code ArenaBattle} 以建场前的站人高度 {@link ArenaRealm#FLOOR_Y} 作为地面顶方块
     * （四座维度都是固定 flat：顶层 −41、竞技场地面顶方块 −40）。这里再问一次世界，
     * 万一以后维度高度改了也不至于错位；问不到（区块没加载 / 兜底值）就用常量。</p>
     */
    private static int floorTopY(ServerLevel level) {
        int walk = ArenaRealm.surfaceY(level, ArenaRealm.CENTER_X + 34,
                ArenaRealm.CENTER_Z);
        if (walk <= ArenaRealm.FLOOR_Y) {
            return ArenaRealm.FLOOR_Y;              // surfaceY 的兜底值 = 未加载
        }
        int fy = walk - 1;
        if (fy < ArenaRealm.FLOOR_Y - 2 || fy > ArenaRealm.FLOOR_Y + 2) {
            HongmengMod.LOGGER.warn("[detail] arena 地面高度异常（surfaceY={}），回退 FLOOR_Y={}",
                    walk, ArenaRealm.FLOOR_Y);
            return ArenaRealm.FLOOR_Y;
        }
        return fy;
    }

    // ==================================================================
    // ① 看台宝库 ×4（9×5×5 侧室 + 2 宝箱/室）
    // ==================================================================

    private static void buildVaults(List<Op> ops, int fy, Dungeon dungeon, Theme th) {
        for (int q = 0; q < 4; q++) {
            buildVault(ops, fy, dungeon, th, q);
        }
    }

    /**
     * 一座宝库（q=0 是 NE 象限，q=1..3 依次 90° 旋转）。
     *
     * <p>模板：外框 x 32..40 × z 34..38；地板 dy=0（主题地板）、墙 dy=1..3、
     * 天花 dy=4；西墙（朝场地中心一侧）留 3 宽 × 2 高门洞；门前接一段穿过
     * 第一级看台的缺口；屋顶上方清掉被宝库压住的看台柱/柱顶灯。</p>
     */
    private static void buildVault(List<Op> ops, int fy, Dungeon dungeon, Theme th, int q) {
        int x1 = VAULT_X0 + VAULT_W - 1;            // 40
        int z1 = VAULT_Z0 + VAULT_D - 1;            // 38
        // 地板 + 天花
        for (int x = VAULT_X0; x <= x1; x++) {
            for (int z = VAULT_Z0; z <= z1; z++) {
                put(ops, fy, q, x, 0, z, th.floor());
                put(ops, fy, q, x, VAULT_H - 1, z, th.wall());
            }
        }
        // 四壁（dy 1..3）：西墙留门洞（z 35..37、dy 1..2）
        for (int dy = 1; dy <= VAULT_H - 2; dy++) {
            for (int z = VAULT_Z0; z <= z1; z++) {
                boolean door = z >= VAULT_Z0 + 1 && z <= VAULT_Z0 + 3 && dy <= 2;
                if (door) {
                    carve(ops, fy, q, VAULT_X0, dy, z);
                } else {
                    put(ops, fy, q, VAULT_X0, dy, z, th.wall());
                }
                put(ops, fy, q, x1, dy, z, th.wall());
            }
            for (int x = VAULT_X0 + 1; x < x1; x++) {
                put(ops, fy, q, x, dy, VAULT_Z0, th.wall());
                put(ops, fy, q, x, dy, z1, th.wall());
            }
        }
        // 内部清空（dy 1..3）
        for (int x = VAULT_X0 + 1; x < x1; x++) {
            for (int z = VAULT_Z0 + 1; z < z1; z++) {
                for (int dy = 1; dy <= VAULT_H - 2; dy++) {
                    carve(ops, fy, q, x, dy, z);
                }
            }
        }
        // 屋顶上方清掉被压住的看台柱/柱顶灯（dy 5..7）
        for (int x = VAULT_X0; x <= x1; x++) {
            for (int z = VAULT_Z0; z <= z1; z++) {
                for (int dy = VAULT_H; dy <= VAULT_H + 2; dy++) {
                    carve(ops, fy, q, x, dy, z);
                }
            }
        }
        // 门廊（x=31）：两侧石柱 + 门楣 + 两盏挂灯 + 门楣上的主题徽记
        int px = VAULT_X0 - 1;
        for (int dy = 1; dy <= VAULT_H - 2; dy++) {
            put(ops, fy, q, px, dy, VAULT_Z0, th.step());
            put(ops, fy, q, px, dy, z1, th.step());
        }
        for (int z = VAULT_Z0; z <= z1; z++) {
            put(ops, fy, q, px, VAULT_H - 1, z, th.accent());
        }
        put(ops, fy, q, px, VAULT_H, VAULT_Z0 + 2, th.trophy());
        put(ops, fy, q, px, VAULT_H - 2, VAULT_Z0 + 1, hanging(th.light()));
        put(ops, fy, q, px, VAULT_H - 2, VAULT_Z0 + 3, hanging(th.light()));
        // 门前通道：穿过第一级看台的缺口（x 26..31，宽 3，高 2）
        for (int x = VAULT_X0 - 6; x < VAULT_X0; x++) {
            for (int z = VAULT_Z0 + 1; z <= VAULT_Z0 + 3; z++) {
                for (int dy = 1; dy <= 2; dy++) {
                    carve(ops, fy, q, x, dy, z);
                }
            }
        }
        // 室内陈设（dy=1 立于地板上）
        put(ops, fy, q, VAULT_X0 + 1, 1, VAULT_Z0 + 1, litCandle(th.candle()));
        put(ops, fy, q, VAULT_X0 + 1, 1, z1 - 1, litCandle(th.candle()));
        put(ops, fy, q, VAULT_X0 + 2, 1, VAULT_Z0 + 1, banner(th.banner(), (12 + q * 4) & 15));
        put(ops, fy, q, VAULT_X0 + 2, 1, z1 - 1, banner(th.banner(), (12 + q * 4) & 15));
        // 后墙藏宝堆（2 高小丘）+ 主题王座
        put(ops, fy, q, x1 - 2, 1, VAULT_Z0 + 1, th.accent());
        put(ops, fy, q, x1 - 2, 1, z1 - 1, th.accent());
        put(ops, fy, q, x1 - 2, 1, VAULT_Z0 + 2, th.accent());
        put(ops, fy, q, x1 - 2, 2, VAULT_Z0 + 2, th.accent());
        put(ops, fy, q, x1 - 1, 1, VAULT_Z0 + 2, th.step());
        put(ops, fy, q, x1 - 1, 2, VAULT_Z0 + 2, th.accent());
        put(ops, fy, q, x1 - 1, 1, VAULT_Z0 + 1, th.step());
        put(ops, fy, q, x1 - 1, 1, z1 - 1, th.step());
        // 屋顶四角 + 边中点缀（让宝库从看台上看像一个「包厢」）
        int[] cx = {VAULT_X0, x1, VAULT_X0, x1};
        int[] cz = {VAULT_Z0, VAULT_Z0, z1, z1};
        for (int i = 0; i < 4; i++) {
            put(ops, fy, q, cx[i], VAULT_H, cz[i], th.accent());
        }
        put(ops, fy, q, VAULT_X0 + 4, VAULT_H, VAULT_Z0, th.step());
        put(ops, fy, q, VAULT_X0 + 4, VAULT_H, z1, th.step());
        put(ops, fy, q, VAULT_X0, VAULT_H, VAULT_Z0 + 2, th.step());
        put(ops, fy, q, x1, VAULT_H, VAULT_Z0 + 2, th.step());
        // 宝箱 ×2（地面方块 y+1 = dy 1；BossRewards 只放空气格 + 幂等）
        chest(ops, fy, dungeon, q, VAULT_X0 + 4, 1, VAULT_Z0 + 1);
        chest(ops, fy, dungeon, q, VAULT_X0 + 4, 1, z1 - 1);
    }

    // ==================================================================
    // ② 看台陈设（战旗 / 火盆 / 坐席纹理 / 顶层战利品架）
    // ==================================================================

    private static void buildStands(List<Op> ops, int fy, Theme th) {
        for (int t = 1; t <= TIERS; t++) {
            int r0 = ArenaBattle.floorRadius() + (t - 1) * TIER_W;   // 内沿
            int r1 = r0 + TIER_W - 1;                                // 外沿
            int seatDy = t;                                          // 该级座面顶方块
            // 主题战旗：每 16 根柱一面（用 45°/225° 柱，避开四门轴线）
            for (int i = 4; i < PILLARS; i += 16) {
                int[] p = polar(r0, i);
                if (insideVault(p[0], p[1])) {
                    continue;                       // 该位置被宝库挖掉/盖住：不放，避免悬空旗
                }
                put(ops, fy, 0, p[0], seatDy + 1, p[1],
                        banner(th.banner(), (10 + i) & 15));
            }
            // 火盆 / 灵魂灯：每 8 根柱一只（放在外沿内一圈的座面上）
            for (int i = 2; i < PILLARS; i += 8) {
                int[] p = polar(r1 - 1, i);
                if (insideVault(p[0], p[1])) {
                    continue;
                }
                put(ops, fy, 0, p[0], seatDy + 1, p[1], th.brazier());
            }
            // 坐席纹理：每 4 根柱一处贴地换主题色（不增高、不挡路）
            for (int i = 1; i < PILLARS; i += 4) {
                int[] p = polar(r0 + 2, i);
                if (insideVault(p[0], p[1])) {
                    continue;
                }
                put(ops, fy, 0, p[0], seatDy, p[1], th.accent());
            }
            // 顶层一圈战利品架：底座 + 主题战利品（金块/骨块/鎏金黑石/紫水晶）
            if (t == TIERS) {
                for (int i = 1; i < PILLARS; i += 2) {
                    int[] p = polar(r0 + 2, i);
                    if (insideVault(p[0], p[1])) {
                        continue;
                    }
                    put(ops, fy, 0, p[0], seatDy + 1, p[1], th.step());
                    put(ops, fy, 0, p[0], seatDy + 2, p[1], th.trophy());
                }
            }
        }
    }

    /**
     * 这个水平坐标是否落在四座宝库的范围内（含门廊）。
     *
     * <p>看台装饰（旗/火盆/坐席纹理/战利品架）是沿圆周均匀撒的，45° 对角线附近
     * 会正好落进宝库屋顶/室内：不跳过的话会出现「悬空旗插在宝库天花板里」
     * 或者「装饰被宝库挖掉」的怪象。</p>
     */
    private static boolean insideVault(int x, int z) {
        for (int q = 0; q < 4; q++) {
            int[] t = invRot(q, x, z);
            if (t[0] >= VAULT_X0 - 1 && t[0] <= VAULT_X0 + VAULT_W
                    && t[1] >= VAULT_Z0 - 1 && t[1] <= VAULT_Z0 + VAULT_D) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    // ③ 四门门楼 + 地毯
    // ==================================================================

    private static void buildGates(List<Op> ops, int fy, Theme th) {
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2.0D * i;
            int dx = (int) Math.round(Math.cos(a));
            int dz = (int) Math.round(Math.sin(a));
            int px = -dz;                       // 门的横向单位向量
            int pz = dx;
            // 门洞两侧石柱 + 柱顶灯（门楼）
            for (int side = -1; side <= 1; side += 2) {
                int gx = dx * GATE_R + px * side * 4;
                int gz = dz * GATE_R + pz * side * 4;
                for (int dy = 1; dy <= 6; dy++) {
                    put(ops, fy, 0, gx, dy, gz, th.wall());
                }
                put(ops, fy, 0, gx, 7, gz, th.accent());
                put(ops, fy, 0, gx, 8, gz, th.light());
            }
            // 门楣上方主题饰带
            for (int w = -2; w <= 2; w++) {
                put(ops, fy, 0, dx * GATE_R + px * w, 8, dz * GATE_R + pz * w, th.accent());
            }
            // 门内两盏挂灯（挂在门框上）
            for (int side = -1; side <= 1; side += 2) {
                put(ops, fy, 0, dx * GATE_R + px * side * 2, 5,
                        dz * GATE_R + pz * side * 2, hanging(th.light()));
            }
            // 门前 3 格地毯（贴地，不挡路）
            for (int r = GATE_R - 4; r <= GATE_R - 2; r++) {
                for (int w = -1; w <= 1; w++) {
                    put(ops, fy, 0, dx * r + px * w, 1, dz * r + pz * w, th.carpet());
                }
            }
        }
    }

    // ==================================================================
    // ④ 中央祭坛后方「胜利台座」（东门内 6 格，3 级小台）
    // ==================================================================

    private static void buildVictoryDais(List<Op> ops, int fy, Theme th) {
        int cx = ArenaBattle.floorRadius() - 7;       // 37：东门（r=43）内侧 6 格
        int cz = 0;
        // 3 级台：5×5 底？—— 用 3×3 → 1×1 → 1×1 逐级 +1，台顶只放装饰
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                put(ops, fy, 0, cx + dx, 1, cz + dz, th.step());
            }
        }
        put(ops, fy, 0, cx, 2, cz, th.step());
        put(ops, fy, 0, cx, 3, cz, th.accent());
        put(ops, fy, 0, cx, 4, cz, Blocks.END_ROD.defaultBlockState());
        // 四角烛台（立于第一级台面）
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                put(ops, fy, 0, cx + sx, 2, cz + sz, litCandle(th.candle()));
            }
        }
    }

    // ==================================================================
    // ⑤ 主题雕像 ×4（对角 r≈30，3 格高，不挡走位）
    // ==================================================================

    private static void buildStatues(List<Op> ops, int fy, Theme th) {
        int[][] spots = {{21, 21}, {21, -21}, {-21, 21}, {-21, -21}};
        for (int[] spot : spots) {
            int cx = spot[0];
            int cz = spot[1];
            // 1 格高 3×3 台基
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    put(ops, fy, 0, cx + dx, 1, cz + dz, th.step());
                }
            }
            // 十字「肩」+ 躯干（第 2 格）+ 头（第 3 格，主题点缀）
            put(ops, fy, 0, cx + 1, 2, cz, th.wall());
            put(ops, fy, 0, cx - 1, 2, cz, th.wall());
            put(ops, fy, 0, cx, 2, cz + 1, th.wall());
            put(ops, fy, 0, cx, 2, cz - 1, th.wall());
            put(ops, fy, 0, cx, 2, cz, th.wall());
            put(ops, fy, 0, cx, 3, cz, th.accent());
        }
    }

    // ==================================================================
    // ⑥ 中心地面纹样（r<30 只做贴地纹样：4 条对角主题线 + 2 道环）
    // ==================================================================

    private static void buildCenterPattern(List<Op> ops, int fy, Theme th) {
        Set<Long> seen = new java.util.HashSet<>();
        // 4 条对角射线：r=12..28
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4.0D + Math.PI / 2.0D * k;
            for (int r = 12; r <= 28; r++) {
                int x = (int) Math.round(Math.cos(a) * r);
                int z = (int) Math.round(Math.sin(a) * r);
                addPattern(ops, seen, fy, x, z, th.accent());
            }
        }
        // 2 道环：r=16 / r=26（每 5° 一格，贴地换色）
        for (int ringR : new int[]{16, 26}) {
            for (int deg = 0; deg < 360; deg += 5) {
                double a = Math.toRadians(deg);
                int x = (int) Math.round(Math.cos(a) * ringR);
                int z = (int) Math.round(Math.sin(a) * ringR);
                addPattern(ops, seen, fy, x, z, th.accent());
            }
        }
    }

    private static void addPattern(List<Op> ops, Set<Long> seen, int fy,
                                   int x, int z, BlockState st) {
        long key = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        if (!seen.add(key)) {
            return;
        }
        ops.add(Op.block(ArenaRealm.CENTER_X + x, fy, ArenaRealm.CENTER_Z + z, st));
    }

    // ==================================================================
    // 主题（与 ArenaBattle.styleFor 呼应；方块全部是原版）
    // ==================================================================

    /** 一座竞技场的细节配色。 */
    private record Theme(BlockState floor, BlockState wall, BlockState step, BlockState accent,
                         BlockState light, BlockState trophy, BlockState banner,
                         BlockState carpet, BlockState candle, BlockState brazier) {
    }

    private static Theme themeOf(Dungeon dungeon) {
        return switch (dungeon) {
            case EPOCH -> new Theme(
                    Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    Blocks.DEEPSLATE_TILES.defaultBlockState(),
                    Blocks.QUARTZ_BLOCK.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.WHITE_BANNER.defaultBlockState(),
                    Blocks.WHITE_CARPET.defaultBlockState(),
                    Blocks.WHITE_CANDLE.defaultBlockState(),
                    Blocks.CAMPFIRE.defaultBlockState());
            case BEAST -> new Theme(
                    Blocks.PACKED_MUD.defaultBlockState(),
                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.PACKED_MUD.defaultBlockState(),
                    Blocks.EMERALD_BLOCK.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState(),
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    Blocks.GREEN_BANNER.defaultBlockState(),
                    Blocks.GREEN_CARPET.defaultBlockState(),
                    Blocks.GREEN_CANDLE.defaultBlockState(),
                    Blocks.CAMPFIRE.defaultBlockState());
            case DEMON -> new Theme(
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                    Blocks.BLACKSTONE.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState(),
                    Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.RED_BANNER.defaultBlockState(),
                    Blocks.RED_CARPET.defaultBlockState(),
                    Blocks.RED_CANDLE.defaultBlockState(),
                    Blocks.SOUL_CAMPFIRE.defaultBlockState());
            case VOID -> new Theme(
                    Blocks.END_STONE_BRICKS.defaultBlockState(),
                    Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.PURPUR_PILLAR.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.PURPLE_BANNER.defaultBlockState(),
                    Blocks.PURPLE_CARPET.defaultBlockState(),
                    Blocks.PURPLE_CANDLE.defaultBlockState(),
                    Blocks.SOUL_CAMPFIRE.defaultBlockState());
        };
    }

    // ==================================================================
    // 方块状态小工具
    // ==================================================================

    /** 点燃的蜡烛（装饰烛台；不新注册，直接用原版蜡烛的 LIT 状态）。 */
    private static BlockState litCandle(BlockState candle) {
        return candle.hasProperty(CandleBlock.LIT)
                ? candle.setValue(CandleBlock.LIT, Boolean.TRUE) : candle;
    }

    /** 悬挂的灯笼（挂在门楣/天花下；不新注册，直接用原版灯笼 HANGING 状态）。 */
    private static BlockState hanging(BlockState lantern) {
        return lantern.hasProperty(LanternBlock.HANGING)
                ? lantern.setValue(LanternBlock.HANGING, Boolean.TRUE) : lantern;
    }

    /** 站立的主题旗帜（按 16 方位旋转，只影响外观）。 */
    private static BlockState banner(BlockState banner, int rotation) {
        return banner.hasProperty(BannerBlock.ROTATION)
                ? banner.setValue(BannerBlock.ROTATION, rotation & 15) : banner;
    }

    /** 看台第 {@code idx} 根柱对应的圆周坐标（32 等分）。 */
    private static int[] polar(int r, int idx) {
        double a = Math.PI * 2.0D * idx / PILLARS;
        return new int[]{(int) Math.round(Math.cos(a) * r), (int) Math.round(Math.sin(a) * r)};
    }

    /** 90° 旋转（q=0..3）：(x,z) → (−z,x) → (−x,−z) → (z,−x)。 */
    private static int[] rot(int q, int x, int z) {
        return switch (q & 3) {
            case 0 -> new int[]{x, z};
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            default -> new int[]{z, -x};
        };
    }

    /** {@link #rot} 的逆变换（把世界坐标转回 NE 模板坐标）。 */
    private static int[] invRot(int q, int x, int z) {
        return switch (q & 3) {
            case 0 -> new int[]{x, z};
            case 1 -> new int[]{z, -x};
            case 2 -> new int[]{-x, -z};
            default -> new int[]{-z, x};
        };
    }

    /** 模板坐标写块（带 90° 旋转）。 */
    private static void put(List<Op> ops, int fy, int q, int x, int dy, int z, BlockState st) {
        int[] p = rot(q, x, z);
        ops.add(Op.block(ArenaRealm.CENTER_X + p[0], fy + dy,
                ArenaRealm.CENTER_Z + p[1], st));
    }

    /** 模板坐标挖空（带 90° 旋转）。 */
    private static void carve(List<Op> ops, int fy, int q, int x, int dy, int z) {
        int[] p = rot(q, x, z);
        ops.add(Op.carve(ArenaRealm.CENTER_X + p[0], fy + dy, ArenaRealm.CENTER_Z + p[1]));
    }

    /** 模板坐标放宝箱（带 90° 旋转；y = 地面方块 +1，表按副本取）。 */
    private static void chest(List<Op> ops, int fy, Dungeon dungeon, int q,
                              int x, int dy, int z) {
        int[] p = rot(q, x, z);
        ops.add(Op.chest(ArenaRealm.CENTER_X + p[0], fy + dy,
                ArenaRealm.CENTER_Z + p[1], BossRewards.arenaLootFor(dungeon)));
    }

    // ==================================================================
    // 计划项
    // ==================================================================

    /**
     * 一条施工计划：写方块 / 挖空 / 放宝箱。
     *
     * @param loot 非空 = 放宝箱（表 + 固定种子）
     */
    private record Op(int x, int y, int z, BlockState state, boolean carve,
                      ResourceLocation loot) {

        static Op block(int x, int y, int z, BlockState st) {
            return new Op(x, y, z, st, false, null);
        }

        static Op carve(int x, int y, int z) {
            return new Op(x, y, z, null, true, null);
        }

        static Op chest(int x, int y, int z, ResourceLocation loot) {
            return new Op(x, y, z, null, false, loot);
        }
    }

    // ==================================================================
    // SavedData：版本号 + 游标 + 统计
    // ==================================================================

    private static DetailFlag flag(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                DetailFlag::load, DetailFlag::new, DATA_KEY);
    }

    private static String shortName(Dungeon dungeon) {
        return dungeon.name().toLowerCase(Locale.ROOT);
    }

    /** 维度级进度：版本号、计划游标（分帧）、已写方块数、已放宝箱数。 */
    public static final class DetailFlag extends SavedData {
        /** 已完成的 version（>= {@link #VERSION} 表示无需再施工）。 */
        int version;
        /** 游标对应的 version（版本变了游标作废、从头开始）。 */
        int cursorVersion;
        /** 下一个要执行的计划下标。 */
        int cursor;
        /** 累计写入方块数（含中断重开前的部分）。 */
        int blocks;
        /** 累计放置宝箱数。 */
        int chests;

        static DetailFlag load(CompoundTag tag) {
            DetailFlag f = new DetailFlag();
            f.version = tag.getInt("version");
            f.cursorVersion = tag.getInt("cursor_version");
            f.cursor = tag.getInt("cursor");
            f.blocks = tag.getInt("blocks");
            f.chests = tag.getInt("chests");
            return f;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("version", version);
            tag.putInt("cursor_version", cursorVersion);
            tag.putInt("cursor", cursor);
            tag.putInt("blocks", blocks);
            tag.putInt("chests", chests);
            return tag;
        }
    }
}
