package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>每个 boss 一个专属维度</b>（玩家要求）。
 *
 * <p>玩家原话：「新维度的地形生态什么的符合 boss，打完最后一波自动传送到 boss 维度，
 * <b>每一个 boss 都要有需要自己的维度</b>」。</p>
 *
 * <h2>四个维度（与副本一一对应）</h2>
 * <table border="1">
 *   <caption>维度 → 祭坛主题 → 召唤的 boss（品阶）</caption>
 *   <tr><th>副本</th><th>维度</th><th>祭坛</th><th>boss</th></tr>
 *   <tr><td>纪元</td><td>{@code arena_epoch}</td><td>epoch</td>
 *       <td>魔道统领（头目 ×25）</td></tr>
 *   <tr><td>妖兽</td><td>{@code arena_beast}</td><td>beast</td>
 *       <td>虚空巨蟒（头目 ×25）</td></tr>
 *   <tr><td>魔道</td><td>{@code arena_demon}</td><td>demon</td>
 *       <td>虚空行者（头目 ×25）</td></tr>
 *   <tr><td>虚空</td><td>{@code arena_void}</td><td>void</td>
 *       <td>虚空道主（首领 ×80）</td></tr>
 * </table>
 *
 * <h2>⚠ 两个必须记住的坑（都踩过）</h2>
 * <ol>
 *   <li><b>高度不能硬编码</b>。超平坦层的实际地面取决于 {@code min_y} 与层厚：
 *       基岩1 + 主材20 + 顶面3 = 24，{@code min_y = -64}
 *       → 顶层方块在 {@code y = -64 + 24 - 1 = -41}，站人高度 −40。
 *       早先硬编码成 64，比真实地面高 105 格，<b>玩家被埋在实心层里</b>。
 *       现在一律走 {@link #surfaceY} 直接问世界。</li>
 *   <li><b>祭坛必须放"有主题"的那一个方块</b>。通用 {@code boss_summon_altar} 的
 *       theme 是 null，只能靠猜维度名 —— 而竞技场维度叫 {@code arena_beast}，
 *       猜出来的主题会召出<b>精英</b>（×3 生命），比怪潮头目（×25）弱 8 倍，
 *       而且没有血条、不触发三阶段。</li>
 * </ol>
 */
public final class ArenaRealm {

    /** 竞技场中心的水平坐标。 */
    public static final int CENTER_X = 0;
    public static final int CENTER_Z = 0;
    /** 兜底地面高度（真实值由 {@link #surfaceY} 决定）。 */
    public static final int FLOOR_Y = -40;

    /** 存档里的"已建成"标记键。 */
    private static final String BUILT_KEY = "hongmeng_arena_built";

    /**
     * <b>竞技场生成器版本</b>。
     *
     * <p>⚠ 每次改动 {@code ArenaBattle.buildForce} 的形状 / 材料 / 尺寸，
     * <b>必须把这个数 +1</b>。否则存档里的"已建成"标记会让新代码
     * <b>永远不重建</b>，玩家看到的还是旧场地 ——
     * 实测已经踩过两次（改了半径与祭坛高度，玩家进游戏发现还是老的）。</p>
     */
    private static final int GEN_VERSION = 3;

    /** 副本 → 维度键。 */
    private static final Map<Dungeon, ResourceKey<Level>> KEYS = new EnumMap<>(Dungeon.class);

    /** 已经进过竞技场的玩家 → 该副本。 */
    private static final Map<UUID, Dungeon> IN_ARENA = new ConcurrentHashMap<>();

    static {
        for (Dungeon d : Dungeon.values()) {
            KEYS.put(d, ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation("hongmeng", "arena_" + shortName(d))));
        }
    }

    private ArenaRealm() {
    }

    /** {@code demon_realm} → {@code demon}。 */
    private static String shortName(Dungeon d) {
        String id = d.dimensionId();
        int i = id.indexOf('_');
        return i > 0 ? id.substring(0, i) : id;
    }

    public static ResourceKey<Level> keyOf(Dungeon d) {
        return KEYS.get(d);
    }

    public static ServerLevel level(MinecraftServer server, Dungeon d) {
        if (server == null || d == null) {
            return null;
        }
        return server.getLevel(KEYS.get(d));
    }

    /** 玩家当前所在的竞技场副本（不在竞技场返回 null）。 */
    public static Dungeon dungeonOf(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        for (Map.Entry<Dungeon, ResourceKey<Level>> e : KEYS.entrySet()) {
            if (player.level().dimension().equals(e.getValue())) {
                return e.getKey();
            }
        }
        return null;
    }

    public static boolean isInArena(ServerPlayer player) {
        return dungeonOf(player) != null;
    }

    /** 这个维度键是不是某座 boss 决战维度（给换维度事件清过期记录用）。 */
    public static boolean isArenaDim(net.minecraft.resources.ResourceKey<Level> key) {
        if (key == null) {
            return false;
        }
        for (Map.Entry<Dungeon, ResourceKey<Level>> e : KEYS.entrySet()) {
            if (e.getValue().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static Dungeon recorded(ServerPlayer player) {
        return player == null ? null : IN_ARENA.get(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        if (player != null) {
            IN_ARENA.remove(player.getUUID());
        }
    }

    // ------------------------------------------------------------------
    // 高度：一律问世界，不硬编码
    // ------------------------------------------------------------------

    /**
     * <b>从世界顶往下找第一块实心地面</b>，返回可以站人的 y。
     *
     * <p>超平坦层的实际高度取决于 {@code min_y} 与各层厚度，算错一点就会把玩家
     * 埋进土里（实测就是这样），所以直接问世界最可靠。</p>
     */
    public static int surfaceY(ServerLevel level, int x, int z) {
        if (level == null) {
            return FLOOR_Y;
        }
        int top = Math.min(level.getMaxBuildHeight() - 2, 320);
        int bottom = level.getMinBuildHeight() + 1;
        for (int y = top; y > bottom; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (!level.isLoaded(p)) {
                continue;
            }
            if (!level.getBlockState(p).isAir()) {
                continue;                       // 这一格被占，继续往下
            }
            if (!level.getBlockState(p.above()).isAir()) {
                continue;                       // 头顶被占，站不下
            }
            BlockPos below = p.below();
            BlockState b = level.getBlockState(below);
            if (!b.isAir() && !b.getCollisionShape(level, below).isEmpty()) {
                return y;                       // 脚下实心、身位与头顶空着 → 就是这里
            }
        }
        return FLOOR_Y;                         // 兜底
    }

    /** 竞技场中心（y 取实际地表面）。 */
    public static BlockPos center(ServerLevel level) {
        if (level == null) {
            return new BlockPos(CENTER_X, FLOOR_Y, CENTER_Z);
        }
        return new BlockPos(CENTER_X, surfaceY(level, CENTER_X, CENTER_Z), CENTER_Z);
    }

    /** 竞技场中心（无世界上下文时的近似值，仅用于日志）。 */
    public static BlockPos center() {
        return new BlockPos(CENTER_X, FLOOR_Y, CENTER_Z);
    }

    // ------------------------------------------------------------------
    // 进入 / 建成
    // ------------------------------------------------------------------

    /** 把玩家送进对应副本的竞技场维度。 */
    public static boolean enter(ServerPlayer player, Dungeon dungeon) {
        if (player == null || player.getServer() == null || dungeon == null) {
            return false;
        }
        ServerLevel level = level(player.getServer(), dungeon);
        if (level == null) {
            HongmengMod.LOGGER.error("[arena] 竞技场维度未加载：hongmeng:arena_{}",
                    shortName(dungeon));
            return false;
        }
        try {
            forceLoad(level);
            BlockPos c = center(level);
            ensureBuilt(level, dungeon, c);
            // 主题地貌：布置在半径 120-200 的圆环里，不会碰到竞技场
            ArenaTheme.ensure(level, dungeon, c);
            // 决战前庭：r=85~110（看台外沿 → 主题环之间）的四向神道/门阙/环道/四角丰碑。
            // 独立 SavedData；会主动让开 AltarArena 的小祭坛房间（72,72 一带）。
            try {
                DungeonForecourt.ensureArena(level, dungeon, c);
            } catch (Throwable t) {
                HongmengMod.LOGGER.warn(
                        "[forecourt] {} 决战前庭生成失败（不影响决战）：{}",
                        dungeon.dimensionId(), t.toString());
            }
            // 小祭坛房间（r=26 独立房间 + 奖励箱）：历史上只在副本维度建、实机从不出现，
            // 这里在竞技场对角线外侧补一间，幂等（自己的 marker 判据）。
            try {
                AltarArena.ensureBesideArena(level, dungeon, c);
            } catch (Throwable t) {
                HongmengMod.LOGGER.warn("[arena] 决战维度小祭坛房间补建失败（不影响决战）：{}",
                        t.toString());
            }

            // 祭坛没生成就不要把玩家传送进来 —— 否则“进来了却没祭坛、也打不了 boss”，
            // 只能靠兜底送回，玩家会以为“被弹走”。先确认祭坛在位，再传送。
            BlockPos altarPos = c.above(ArenaBattle.altarY());
            boolean altar = ArenaBattle.isAltar(dungeon, level.getBlockState(altarPos));
            if (!altar) {
                HongmengMod.LOGGER.error(
                        "[arena] {} 决战祭坛未生成（位置 {} 实际方块 {}），本次不传送，退回正常通关",
                        dungeon.dimensionId(), altarPos, level.getBlockState(altarPos).getBlock());
                return false;
            }
            // 每次进决战都把祭坛重置为“未点亮”：旧版 lit=true 永久保留，
            // 导致同一副本第二次决战无法再召唤首领（审计 A-1，玩家实测“打完一次就再也打不了”）。
            level.setBlock(altarPos, ArenaBattle.altarFor(dungeon), 2);
            // 顺手清掉上一场残留的祭坛首领（非玩家击杀 / 异常退出时可能留下）。
            for (Entity e : level.getEntities((Entity) null,
                    new AABB(-96.0D, level.getMinBuildHeight(), -96.0D,
                            96.0D, level.getMaxBuildHeight(), 96.0D),
                    en -> en.getPersistentData().getBoolean("hm_arena_boss"))) {
                e.discard();
            }

            int landX = c.getX() - (ArenaBattle.altarRadius() + 4);
            int landY = surfaceY(level, landX, c.getZ());
            // 落点被主题装饰/残留方块抬太高时压回竞技场地面附近（防止“传送后掉下来/悬空”）
            if (landY > c.getY() + 12) {
                landY = c.getY() + 1;
            }
            BlockPos landing = new BlockPos(landX, landY, c.getZ());
            clearHeadroom(level, landing);
            HongmengMod.LOGGER.info(
                    "[arena] {} 决战落点 {}（祭坛 {}，水平距中心 {} 格，floorY={}）",
                    dungeon.dimensionId(), landing, altarPos,
                    Math.abs(landX - c.getX()), c.getY());

            player.teleportTo(level, landing.getX() + 0.5D, landing.getY(),
                    landing.getZ() + 0.5D, 0.0F, 8.0F);
            double dx = c.getX() + 0.5D - player.getX();
            double dz = c.getZ() + 0.5D - player.getZ();
            float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
            player.moveTo(player.getX(), player.getY(), player.getZ(), yaw, 8.0F);
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.hurtMarked = true;
            IN_ARENA.put(player.getUUID(), dungeon);

            level.playSound(null, c, SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT,
                    2.0F, 0.6F);
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D,
                    player.getZ(), 90, 0.7D, 1.2D, 0.7D, 0.4D);

            HongmengMod.LOGGER.info(
                    "[arena] {} 进入 {} 的 boss 维度（落点={} 祭坛={} 在位={}）",
                    player.getName().getString(), dungeon.dimensionId(),
                    landing, altarPos, altar);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a75【决战】\u00a77怪潮已尽 —— 你被卷入了\u00a7d首领的位面"
                            + "\u00a77。\u00a7d祭坛\u00a77就在你正前方，"
                            + "\u00a7f右键\u00a77唤醒此地的首领。"), false);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78（打完首领才算通关；潜行 + 空手右键可以退出）"), true);
            return true;
        } catch (Throwable t) {
            HongmengMod.LOGGER.error("[arena] 进入 boss 维度失败", t);
            return false;
        }
    }

    /** 保证竞技场已建成（有标记就跳过）。 */
    public static void ensureBuilt(ServerLevel level, Dungeon dungeon, BlockPos c) {
        forceLoad(level);
        if (isBuilt(level)) {
            return;
        }
        ArenaBattle.buildForce(level, c, dungeon);
        BlockPos altarPos = c.above(ArenaBattle.altarY());
        if (ArenaBattle.isAltar(dungeon, level.getBlockState(altarPos))) {
            markBuilt(level, true);
            HongmengMod.LOGGER.info("[arena] {} 的 boss 维度已建成（中心={} 祭坛={}）",
                    dungeon.dimensionId(), c, altarPos);
        } else {
            HongmengMod.LOGGER.error(
                    "[arena] 建成复核失败！副本={} 中心={} 祭坛位置实际方块={}",
                    dungeon.dimensionId(), c, level.getBlockState(altarPos).getBlock());
        }
    }

    /**
     * 强制加载竞技场本体周围的区块（**不含**主题环带）。
     *
     * <p>⚠ 加载范围必须装得下整座竞技场：看台外沿是 <b>r=84</b>（{@code ArenaBattle}），
     * 方块 84 已经在 chunk 5（80~95）里了；原来只加载中心 9×9（chunk −4..4 = 方块 −64~79），
     * <b>装不下看台最外两圈</b> —— {@code ArenaBattle.set()} 的 {@code isLoaded} 判据
     * 会把外圈静默跳过。现在扩到 chunk −6..6（方块 −96~111，圆内覆盖到 r≈96），
     * 留出余量。</p>
     *
     * <p>主题环带（r=120~168）由 {@code ArenaTheme.preloadRing} 自己按"与环带相交"加载，
     * 不在这里一把梭（否则每次进维度都要生成几百个区块）。</p>
     */
    private static void forceLoad(ServerLevel level) {
        int cx = CENTER_X >> 4;
        int cz = CENTER_Z >> 4;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                level.getChunk(cx + dx, cz + dz,
                        net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
            }
        }
    }

    /** 把落点头顶清空、脚下补实心。 */
    private static void clearHeadroom(ServerLevel level, BlockPos landing) {
        for (int up = 0; up <= 2; up++) {
            BlockPos p = landing.above(up);
            if (!level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (level.getBlockState(landing.below()).isAir()) {
            level.setBlock(landing.below(), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 2);
        }
    }

    // ------------------------------------------------------------------
    // 建成标记（Forge 的 DimensionDataStorage —— ServerLevel 没有 getPersistentData）
    // ------------------------------------------------------------------

    public static boolean isBuilt(ServerLevel level) {
        try {
            ArenaFlag f = level.getDataStorage()
                    .computeIfAbsent(ArenaFlag::load, ArenaFlag::new, BUILT_KEY);
            // 版本对不上 = 生成器改过 → 当作"没建"，让新代码重建
            return f.built && f.version == GEN_VERSION;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void markBuilt(ServerLevel level, boolean built) {
        try {
            ArenaFlag flag = level.getDataStorage()
                    .computeIfAbsent(ArenaFlag::load, ArenaFlag::new, BUILT_KEY);
            flag.built = built;
            flag.version = GEN_VERSION;
            flag.setDirty();
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[arena] 标记写入失败：{}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 主题地貌标记：**独立版本号**，与竞技场本体的 GEN_VERSION 互不影响
    // ------------------------------------------------------------------

    /**
     * 主题地貌已生成的版本号（0 = 从未生成）。
     *
     * <p>为什么要单独一个版本号：老存档里只有 {@code theme_built=true} 这个布尔，
     * 主题地貌从 v1 稀疏装饰改成 v2 完整场景后，布尔还是 {@code true}，
     * 新代码<b>永远看不到新场景</b>（和 {@link #GEN_VERSION} 踩过的坑一模一样）。
     * 现在把 {@code ArenaTheme.THEME_VERSION} 存进存档：老存档读出来是 0 → 小于当前版本
     * → 触发重建；改了生成逻辑只需 +1，竞技场本体不受影响。</p>
     */
    public static int themeVersion(ServerLevel level) {
        try {
            return level.getDataStorage()
                    .computeIfAbsent(ArenaFlag::load, ArenaFlag::new, BUILT_KEY)
                    .themeVersion;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 标记主题地貌已生成到指定版本（{@code ArenaTheme.THEME_VERSION}）。 */
    public static void markThemeBuilt(ServerLevel level, int version) {
        try {
            ArenaFlag f = level.getDataStorage()
                    .computeIfAbsent(ArenaFlag::load, ArenaFlag::new, BUILT_KEY);
            f.themeBuilt = true;
            f.themeVersion = version;
            f.setDirty();
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[arena] 地貌标记写入失败：{}", t.toString());
        }
    }

    /** 存一个布尔标记的 SavedData。 */
    public static final class ArenaFlag
            extends net.minecraft.world.level.saveddata.SavedData {
        boolean built;
        int version;
        boolean themeBuilt;
        /** 主题地貌版本号（0 = 老存档 / 从未生成）。 */
        int themeVersion;

        static ArenaFlag load(net.minecraft.nbt.CompoundTag tag) {
            ArenaFlag f = new ArenaFlag();
            f.built = tag.getBoolean("built");
            f.version = tag.getInt("version");
            f.themeBuilt = tag.getBoolean("theme_built");
            f.themeVersion = tag.getInt("theme_version");
            return f;
        }

        @Override
        public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) {
            tag.putBoolean("built", built);
            tag.putInt("version", version);
            tag.putBoolean("theme_built", themeBuilt);
            tag.putInt("theme_version", themeVersion);
            return tag;
        }
    }
}
