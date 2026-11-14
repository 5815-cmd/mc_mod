package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>怪潮战场「细节 pass」</b>（Boss 场地完善 D2）。
 *
 * <p>玩家需求：「这些 boss 的场地和其他类似的都需要去完善，屋内有奖励箱啊之类的」。
 * 四座副本维度（epoch/beast/demon/void_realm）的中央战场由 {@link DungeonArena}
 * 建出<b>骨架</b>（广场 r=19、外沿 r=24、中央祭坛、四座擂台、角塔、看台），
 * 但广场本身是空的 —— 这一层负责在后面补：</p>
 *
 * <ol>
 *   <li><b>四角补给箱 ×4</b>（每座副本）：{@link BossRewards#LOOT_SUPPLY} 表，
 *       每个补给点 = 1 宝箱 + 1 木桶 + 1 火盆 + 1 座主题战利品堆；</li>
 *   <li><b>战场陈设</b>（r=6~18 环带）：四向中轴 3 格高战旗架 ×4、火盆 ×4、
 *       3×2×1 断墙掩体 ×8（留足绕行通道）、兵器架（栅栏 + 铁栏杆）×2；</li>
 *   <li><b>主题配色</b>：与 {@link DungeonArena} 现有材料一致 ——
 *       纪元=石英/金/深板岩、妖兽=苔石/云杉/骨、魔道=黑石/红地狱砖/绯红、虚空=末地石/紫珀/黑曜。</li>
 * </ol>
 *
 * <h2>坐标纪律（硬约束）</h2>
 * <ul>
 *   <li><b>只动切比雪夫 r=6~18</b>（与 {@link DungeonArena#onPlaza} 同一口径，广场 r=19）：
 *       r&lt;6 的中央祭坛 / 怪潮出生区一格不写；r≥20 的看台、角塔、石灯柱也不碰；</li>
 *   <li><b>不写大本营保护盒</b>：{@link DungeonLandmark} 保护盒为
 *       {@code 52×38×18，中心 (0, floorY, -32)} → {@code |x|≤26 且 z∈[-51,-13]}。
 *       ⚠ 这条把广场<b>北侧</b> z∈[-19,-13] 那一条也盖住了，所以本层所有北侧布景
 *       一律内收到 {@code z=-12}（北侧两个补给点因此比南侧内收 3 格，
 *       视觉上仍是四角，且都在保护盒外）；运行时另有 {@link BossStronghold#inside} 兜底；</li>
 *   <li>只写 {@code level.isLoaded(pos)} 的格子；已存在的方块（玩家建筑 / 场地本体）不覆盖，
 *       装饰只填空气或可替换方块（宝箱由 {@link BossRewards#ensureChest} 保证）；</li>
 *   <li>不新增注册方块/实体，只用原版方块。</li>
 * </ul>
 *
 * <h2>自驱 + 幂等 + 分帧</h2>
 * <p>本类自己是 {@code @Mod.EventBusSubscriber}，监听 {@link TickEvent.LevelTickEvent}（END），
 * 每 {@value #CHECK_INTERVAL} tick 检查一次：</p>
 * <ol>
 *   <li>该维度必须是某座副本（{@link DungeonRealm#dungeonAt}）且基础场地已建（{@link DungeonArena#isBuilt}）；</li>
 *   <li>用维度级 {@link DetailFlag SavedData}（键 {@value #DATA_KEY}，含 version/cursor）记进度；
 *       每次最多写 {@value #BATCH} 个方块，写不完下一轮继续（cursor 分帧）；</li>
 *   <li>全部写完才把 {@code version} 标成 {@value #VERSION}（改布局必须 +1，旧存档会从 cursor=0 重铺）。</li>
 * </ol>
 *
 * <p>日志：`[detail] &lt;副本&gt; 战场细节完成：N 方块，M 补给箱`。</p>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID)
public final class RealmDetail {

    /**
     * 细节 pass 版本。
     *
     * <p>⚠ 改了补给点 / 陈设的<b>位置、材料、数量</b>必须 +1 —— 老存档的
     * {@code version} 对不上就会从 cursor=0 重铺（已经放过的宝箱由
     * {@link BossRewards#ensureChest} 幂等跳过，不会重置玩家开过的箱子）。</p>
     */
    /** 1 -> 2：修复“区块未加载时 0 方块也标记完成”的 bug，旧标记需要重跑。 */
    public static final int VERSION = 2;

    /** SavedData 键（每个副本维度各自一份）。 */
    private static final String DATA_KEY = "hongmeng_realm_detail";

    /** 布景环带（切比雪夫，与 {@link DungeonArena#PLAZA_R} 的口径一致）。 */
    private static final int RING_MIN = 6;
    private static final int RING_MAX = 18;

    /** 每轮写入上限（施工约定：分帧，每 tick ≤2000 方块）。 */
    private static final int BATCH = 2000;

    /** 检查间隔 tick（每 20t = 1 秒检查一次）。 */
    private static final long CHECK_INTERVAL = 20L;

    /** 补给箱选种基数（固定 + 坐标哈希：同一场地首开战利品可复现）。 */
    private static final long CHEST_SEED = 0x52454C4D44L;      // "RELMD"

    /** 本会话已完成 / 已放弃的维度（避免每 20t 重复读 SavedData）。 */
    private static final Set<Dungeon> DONE = EnumSet.noneOf(Dungeon.class);
    private static final Set<Dungeon> FAILED = EnumSet.noneOf(Dungeon.class);

    /** 布局缓存（纯数据，不依赖世界；同一维度每次重建结果一致）。 */
    private static final Map<Dungeon, List<Op>> OPS = new EnumMap<>(Dungeon.class);

    private RealmDetail() {
    }

    // ------------------------------------------------------------------
    // 对外（验收 / 调试辅助）
    // ------------------------------------------------------------------

    /**
     * 4 个补给箱相对「竞技场地面 (0, floorY, 0)」的偏移：{@code {dx, dy, dz}}。
     *
     * <p>Y 偏移固定 +1（宝箱站在地面上）；给 {@code /hongmeng bossreward} 验收命令用，
     * 实际坐标 = {@code (dx, DungeonArena.floorY(level, dungeon) + dy, dz)}。</p>
     */
    public static int[][] supplyOffsets() {
        return new int[][]{
                {15, 1, 15},      // 东南角
                {-15, 1, 15},     // 西南角
                {15, 1, -12},     // 东北角（内收 3 格避开大本营保护盒 z∈[-51,-13]）
                {-15, 1, -12},    // 西北角（同上）
        };
    }

    /** 这个维度的战场细节是否已经完成（给验收/调试用；会读取 SavedData，但不写）。 */
    public static boolean isDetailBuilt(ServerLevel level) {
        try {
            return level != null && data(level).version == VERSION;
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 事件入口：维度节拍（每 20t 检查一次，分帧推进）
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL != 0L) {
            return;
        }
        Dungeon dungeon = DungeonRealm.dungeonAt(level);
        if (dungeon == null || DONE.contains(dungeon) || FAILED.contains(dungeon)) {
            return;
        }
        try {
            // ① 基础场地必须先建好（DungeonArena 在现场生成，进副本时才会建）
            if (!DungeonArena.isBuilt(dungeon)) {
                return;
            }
            DetailFlag flag = data(level);
            if (flag.version == VERSION) {
                DONE.add(dungeon);          // 本次会话已完成：不再查
                return;
            }
            // ② 版本升级（未来 VERSION 变化）：清进度重铺（宝箱幂等，不会重置玩家开过的箱）
            if (flag.version != 0 && flag.version != VERSION) {
                HongmengMod.LOGGER.info("[detail] {} 战场细节版本升级 v{} -> v{}，重新铺陈设",
                        dungeon.dimensionId(), flag.version, VERSION);
                flag.cursor = 0;
                flag.placed = 0;
                flag.chests = 0;
            }
            int floorY = DungeonArena.floorY(level, dungeon);
            List<Op> ops = OPS.computeIfAbsent(dungeon, RealmDetail::buildOps);
            int total = ops.size();
            int from = Math.max(0, Math.min(flag.cursor, total));
            if (from == 0 && flag.placed == 0 && flag.chests == 0) {
                // 开工前把 plaza 周围 5×5 区块真正加载出来（r≤18 的方块都在其中），
                // 否则 apply 会因 isLoaded=false 静默跳过（run6 实测 beast/demon/void
                // 被“完成 0 方块”并错误标记 version=1）。
                forceLoadArea(level);
                HongmengMod.LOGGER.info("[detail] {} 战场细节开始：{} 个方块 / 4 个补给箱",
                        dungeon.dimensionId(), total);
            }
            // ③ 分帧推进：本轮最多写 BATCH 个；遇到未加载区块就停在当前 cursor（下一轮重试）
            int to = Math.min(total, from + BATCH);
            boolean paused = false;
            for (int i = from; i < to; i++) {
                if (!apply(level, dungeon, floorY, ops.get(i), flag)) {
                    flag.cursor = i;
                    paused = true;
                    break;
                }
                flag.cursor = i + 1;
            }
            flag.setDirty();
            if (paused) {
                HongmengMod.LOGGER.debug("[detail] {} 区块未加载，暂停在 {}/{}（下一轮继续）",
                        dungeon.dimensionId(), flag.cursor, total);
                return;
            }
            if (flag.cursor >= total) {
                flag.version = VERSION;
                flag.setDirty();
                DONE.add(dungeon);
                HongmengMod.LOGGER.info("[detail] {} 战场细节完成：{} 方块，{} 补给箱",
                        dungeon.dimensionId(), flag.placed, flag.chests);
            } else {
                HongmengMod.LOGGER.debug("[detail] {} 分帧推进：{}/{}（已写 {} 方块 / {} 箱）",
                        dungeon.dimensionId(), flag.cursor, total, flag.placed, flag.chests);
            }
        } catch (Throwable t) {
            // 细节是锦上添花：失败只 warn，绝不影响副本可玩；本次会话不再重试
            FAILED.add(dungeon);
            HongmengMod.LOGGER.warn("[detail] {} 战场细节生成失败（不影响副本，本次会话不再重试）",
                    dungeon.dimensionId(), t);
        }
    }

    // ------------------------------------------------------------------
    // 写方块（唯一入口：坐标纪律 + 不覆盖 + 计数）
    // ------------------------------------------------------------------

    /**
     * 应用一个布局操作。
     *
     * <p>兜底纪律：切比雪夫 r=6~18、大本营保护盒、已加载、不覆盖已有方块 ——
     * 任何一条不满足就跳过（不计入 N）。宝箱走 {@link BossRewards#ensureChest}（自带幂等）。</p>
     */
    private static boolean apply(ServerLevel level, Dungeon dungeon, int floorY, Op op,
                                 DetailFlag flag) {
        int cheb = Math.max(Math.abs(op.dx), Math.abs(op.dz));
        if (cheb < RING_MIN || cheb > RING_MAX) {
            return true;     // 越界是布局错误，直接永久跳过（不阻塞进度）
        }
        BlockPos pos = new BlockPos(op.dx, floorY + op.dy, op.dz);
        if (!level.isLoaded(pos)) {
            return false;    // 未加载：本轮暂停，下一轮重试（绝不能“跳过并标记完成”）
        }
        if (op.chest) {
            long seed = CHEST_SEED + dungeon.ordinal() * 1_000_003L + pos.asLong();
            if (BossRewards.ensureChest(level, pos, BossRewards.LOOT_SUPPLY, seed)) {
                flag.chests++;
            }
            return true;
        }
        // 大本营保护盒兜底（正常布局不会命中：北侧布景已内收到 z=-12）
        if (BossStronghold.inside(floorY, pos.getX(), pos.getY(), pos.getZ())) {
            return true;
        }
        BlockState current = level.getBlockState(pos);
        if (!current.isAir() && !current.canBeReplaced()) {
            return true;     // 玩家建筑 / 场地本体：不覆盖
        }
        level.setBlock(pos, op.state, 2);
        flag.placed++;
        return true;
    }

    /** 加载 plaza 周围 5×5 区块（r≤18 的布局方块全部落在其中）。 */
    private static void forceLoadArea(ServerLevel level) {
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                level.getChunk(cx, cz,
                        net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
            }
        }
    }

    // ------------------------------------------------------------------
    // 布局：四座副本共用同一套坐标，只换主题材料
    // ------------------------------------------------------------------

    /** 一条布局操作：{@code chest=false} 是普通方块，{@code chest=true} 是补给箱。 */
    private record Op(int dx, int dy, int dz, BlockState state, boolean chest) {

        static Op block(int dx, int dy, int dz, BlockState state) {
            return new Op(dx, dy, dz, state, false);
        }

        static Op chest(int dx, int dy, int dz) {
            return new Op(dx, dy, dz, null, true);
        }
    }

    /** 每座副本的主题材料（与 {@link DungeonArena} 的配色对齐）。 */
    private record Theme(BlockState main, BlockState alt, BlockState accent,
                         BlockState fence, Block banner, BlockState campfire) {
    }

    private static Theme themeFor(Dungeon dungeon) {
        return switch (dungeon) {
            // 纪元：汉白玉 / 金 / 深板岩（天青旗）
            case EPOCH -> new Theme(
                    Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    Blocks.DEEPSLATE_TILES.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.OAK_FENCE.defaultBlockState(),
                    Blocks.LIGHT_BLUE_BANNER,
                    Blocks.CAMPFIRE.defaultBlockState());
            // 妖兽：苔石 / 云杉 / 骨（荒野粗野感）
            case BEAST -> new Theme(
                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    Blocks.SPRUCE_FENCE.defaultBlockState(),
                    Blocks.BROWN_BANNER,
                    Blocks.CAMPFIRE.defaultBlockState());
            // 魔道：黑石 / 红地狱砖 / 金镶黑石（血红旗 + 灵魂火）
            case DEMON -> new Theme(
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                    Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.CRIMSON_FENCE.defaultBlockState(),
                    Blocks.RED_BANNER,
                    Blocks.SOUL_CAMPFIRE.defaultBlockState());
            // 虚空：末地石砖 / 紫珀 / 紫水晶（紫旗 + 灵魂火）
            case VOID -> new Theme(
                    Blocks.END_STONE_BRICKS.defaultBlockState(),
                    Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.DARK_OAK_FENCE.defaultBlockState(),
                    Blocks.PURPLE_BANNER,
                    Blocks.SOUL_CAMPFIRE.defaultBlockState());
        };
    }

    /**
     * 生成一座副本的完整布局（纯数据、确定性；{@code y} 是相对地面的偏移）。
     *
     * <p>坐标全部以广场中心 (0, floorY, 0) 为原点、切比雪夫距离 6~16 内：
     * 中央祭坛（r≤3）、四座擂台（±12,±12 的 5×5）、四石灯柱（±17 轴）、看台（r≥20）
     * 全部避开；北侧（z 为负）一律不超过 z=-12（大本营保护盒从 z=-13 开始）。</p>
     */
    private static List<Op> buildOps(Dungeon dungeon) {
        Theme t = themeFor(dungeon);
        List<Op> ops = new ArrayList<>(128);

        // ===== ① 四角补给点：宝箱 + 木桶 + 火盆 + 主题战利品堆 =====
        // 南侧 (±15, +15)；北侧受保护盒限制内收到 z=-12（见类注释）
        int[][] supplies = {{15, 15}, {-15, 15}, {15, -12}, {-15, -12}};
        for (int[] c : supplies) {
            int x = c[0];
            int z = c[1];
            int sx = Integer.signum(x);          // 朝外方向（x）
            int sz = Integer.signum(z);          // 朝外方向（z）
            ops.add(Op.chest(x, 1, z));                                  // 补给宝箱（站在地面上）
            // 木桶 / 战利品堆放在「朝外 + 朝外斜」的一侧：内侧 r≈14 处是 DungeonArena 的
            // 2 格高擂台（±12,±12 的 5×5）与石灯柱，外斜位不会和它们抢格子。
            ops.add(Op.block(x + sx, 1, z, Blocks.BARREL.defaultBlockState()));   // 木桶（空桶）
            ops.add(Op.block(x, 1, z - sz, t.main()));                   // 火盆基座
            ops.add(Op.block(x, 2, z - sz, t.campfire()));               // 火盆
            ops.add(Op.block(x + sx, 1, z - sz, t.accent()));            // 战利品堆
            ops.add(Op.block(x + sx, 2, z - sz, t.alt()));
        }

        // ===== ② 四向中轴战旗架（3 格高：基座 + 战旗 + 横梁 + 栅栏立柱）=====
        int[][] axes = {{11, 0}, {-11, 0}, {0, 11}, {0, -11}};
        for (int[] a : axes) {
            int x = a[0];
            int z = a[1];
            int px = (x == 0) ? 1 : 0;           // 立柱沿垂直方向展开
            int pz = (z == 0) ? 1 : 0;
            int rot = (x > 0) ? 4 : (x < 0) ? 12 : (z > 0) ? 8 : 0;   // 旗面朝场心（装饰性）
            ops.add(Op.block(x, 1, z, t.main()));                          // 基座（给战旗做支撑）
            ops.add(Op.block(x, 2, z, t.banner().defaultBlockState()
                    .setValue(BannerBlock.ROTATION, rot)));                // 战旗
            ops.add(Op.block(x, 3, z, t.alt()));                           // 横梁中段
            for (int s = -1; s <= 1; s += 2) {
                ops.add(Op.block(x + px * s, 1, z + pz * s, t.fence()));
                ops.add(Op.block(x + px * s, 2, z + pz * s, t.fence()));
                ops.add(Op.block(x + px * s, 3, z + pz * s, t.alt()));     // 横梁两端
            }
        }

        // ===== ③ 四个火盆（中轴 r=8，在战旗架与中央祭坛之间）=====
        int[][] braziers = {{8, 0}, {-8, 0}, {0, 8}, {0, -8}};
        for (int[] b : braziers) {
            ops.add(Op.block(b[0], 1, b[1], t.main()));
            ops.add(Op.block(b[0], 2, b[1], t.campfire()));
        }

        // ===== ④ 八处断墙掩体（3 长 × 2 高 × 1 厚，顶上一格塌口；四周留足绕行通道）=====
        // {中心x, 中心z, 走向}：走向 0 = 沿 x，1 = 沿 z
        int[][] walls = {
                {6, 13, 0}, {13, 6, 1}, {-6, 13, 0}, {-13, 6, 1},
                {6, -12, 0}, {13, -6, 1}, {-6, -12, 0}, {-13, -6, 1},
        };
        for (int i = 0; i < walls.length; i++) {
            int wx = walls[i][0];
            int wz = walls[i][1];
            boolean alongX = walls[i][2] == 0;
            for (int s = -1; s <= 1; s++) {
                int x = wx + (alongX ? s : 0);
                int z = wz + (alongX ? 0 : s);
                BlockState st = (((s + i) & 1) == 0) ? t.main() : t.alt();
                ops.add(Op.block(x, 1, z, st));
                if (((s + i) & 1) == 0) {
                    ops.add(Op.block(x, 2, z, st));      // 断口：隔一列矮一格
                }
            }
        }

        // ===== ⑤ 两个兵器架（栅栏立柱 + 中间铁栏杆）=====
        addWeaponRack(ops, 7, 3, true, t);
        addWeaponRack(ops, -7, -3, true, t);

        return ops;
    }

    /** 兵器架：两根 2 高栅栏立柱 + 中间 2 高铁栏杆（1×3×2，不挡路）。 */
    private static void addWeaponRack(List<Op> ops, int x, int z, boolean alongX, Theme t) {
        int px = alongX ? 1 : 0;
        int pz = alongX ? 0 : 1;
        for (int s = -1; s <= 1; s += 2) {
            ops.add(Op.block(x + px * s, 1, z + pz * s, t.fence()));
            ops.add(Op.block(x + px * s, 2, z + pz * s, t.fence()));
        }
        ops.add(Op.block(x, 1, z, Blocks.IRON_BARS.defaultBlockState()));
        ops.add(Op.block(x, 2, z, Blocks.IRON_BARS.defaultBlockState()));
    }

    // ------------------------------------------------------------------
    // 进度保存（维度级 SavedData）
    // ------------------------------------------------------------------

    private static DetailFlag data(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(DetailFlag::load, DetailFlag::new, DATA_KEY);
    }

    /** 只存「完成版本 + 分帧进度 + 计数」的 SavedData。 */
    public static final class DetailFlag extends net.minecraft.world.level.saveddata.SavedData {
        /** 已完成的细节版本（0 = 还没铺完 / 正在铺）。 */
        int version;
        /** 已推进到的操作下标（分帧用）。 */
        int cursor;
        /** 实际写入的方块数。 */
        int placed;
        /** 实际放下的补给箱数。 */
        int chests;

        static DetailFlag load(CompoundTag tag) {
            DetailFlag flag = new DetailFlag();
            flag.version = tag.getInt("version");
            flag.cursor = tag.getInt("cursor");
            flag.placed = tag.getInt("placed");
            flag.chests = tag.getInt("chests");
            return flag;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("version", version);
            tag.putInt("cursor", cursor);
            tag.putInt("placed", placed);
            tag.putInt("chests", chests);
            return tag;
        }
    }
}
