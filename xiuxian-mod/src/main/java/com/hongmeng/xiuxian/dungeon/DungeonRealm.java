package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import com.hongmeng.xiuxian.core.Realm;
import com.hongmeng.xiuxian.difficulty.MobScaling;
import com.hongmeng.xiuxian.entity.ModEntities;
import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 副本维度运行时：{@link EpochRealm} 的通用版，任意 {@link Dungeon} 共用这一套。
 *
 * <p>流程跟纪元秘境一模一样——进门 → 连打若干波 → 通关结算，
 * 区别只在三处：</p>
 * <ol>
 *   <li><b>维度、波数、基准境界、奖励</b>全部从 {@link Dungeon} 取，不再写死；</li>
 *   <li><b>难度按"进门时的境界"缩放</b>：{@link Dungeon#mobScaleFor(int)} 决定副本层的倍率，
 *       身上原有的"品阶 × 玩家境界"倍率（{@code CultivationMonsterEntity} 自己会补）
 *       继续生效，两层相乘而不是互相覆盖；</li>
 *   <li><b>怪用的是修仙界自己那五阶野怪</b>（灵狼 → 虚空道主），不再刷原版僵尸骷髅。</li>
 * </ol>
 *
 * <p>怎么退：{@code 潜行 + 空手右键}（由 {@link DungeonRealmEvents} 接住），
 * 或者干脆死出来——死了挑战直接作废。</p>
 */
public final class DungeonRealm {

    /** 每座秘境里的固定挑战坐标（每座副本各有自己的维度，所以 0,0 不冲突）。 */
    /**
     * <b>禁足半径</b>：20×20 区块 = 320×320 格，所以半边长 160。
     *
     * <p>玩家要求：「设置一个 20*20 区块的禁足，打完 boss 之前不能出去这个范围」。
     * 挑战进行中越过边界会被**拉回内侧**（只拦不罚），打完首领自动解除。</p>
     */
    private static final double BAN_HALF = 160.0D;

    /** 距上次提示的 tick（避免每 tick 刷屏）。 */
    private static final java.util.Map<java.util.UUID, Long> BAN_NOTICE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 首领战结束后自动离开的倒计时（30 秒 = 600 tick）。 */
    private static final long EXIT_COUNTDOWN_TICKS = 20L * 30L;

    /** 玩家 → 首领战后“离开倒计时”。 */
    private static final Map<UUID, ExitCountdown> EXIT_COUNTDOWN = new HashMap<>();

    /** 玩家 → 进副本时的落点（倒计时结束后回到这里，让玩家在副本维度自由探索）。 */
    private static final Map<UUID, BlockPos> REALM_LANDING = new HashMap<>();

    /** 进门落点校正：换维度那一 tick 的传送确认竞态会把玩家留在旧坐标，这里兜一层。 */
    private static final Map<UUID, EntryGuard> ENTRY_GUARD = new HashMap<>();

    /** 进门后的禁足宽限（到哪个 gameTime）：避免刚进门时被旧坐标误判越界拉走。 */
    private static final Map<UUID, Long> BAN_GRACE = new HashMap<>();

    /** 进门落点校正半径（格）。 */
    private static final double ENTRY_GUARD_RADIUS = 24.0D;

    /** 进门后宽限/校正持续多少 tick（100 tick = 5 秒）。 */
    private static final int ENTRY_GUARD_TICKS = 100;

    /** 一次进门的落点校正记录。 */
    private record EntryGuard(Dungeon dungeon, BlockPos landing, long untilTick) {
    }

    /** 首领战后的离开倒计时：结束时刻 + 所属副本。 */
    private record ExitCountdown(Dungeon dungeon, long endTick) {
    }

    private static final int ARENA_X = 0;
    private static final int ARENA_Z = 0;

    /** 每波之间的间隔 tick。 */
    private static final int WAVE_GAP_TICKS = 20 * 8;

    /** 通关冷却（tick）：3 分钟，避免刷奖励。 */
    private static final long CLEAR_COOLDOWN = 20L * 60L * 3L;

    /** 首领波首领的额外强度。 */
    private static final double LEADER_BONUS = 1.8D;

    /** 每推进一波，怪再强一点。 */
    private static final double WAVE_BONUS = 0.12D;

    /** 维度键缓存。 */
    private static final Map<Dungeon, ResourceKey<Level>> KEYS = new EnumMap<>(Dungeon.class);

    /** 进行中的挑战。 */
    private record Challenge(Dungeon dungeon, int wave, Set<UUID> mobs,
                             long lastWaveTick, int enterTier, boolean arenaSent) {
        /** 老构造器（兼容已有调用点）——arenaSent 默认 false。 */
        Challenge(Dungeon dungeon, int wave, Set<UUID> mobs,
                  long lastWaveTick, int enterTier) {
            this(dungeon, wave, mobs, lastWaveTick, enterTier, false);
        }
    }

    private static final Map<UUID, Challenge> CHALLENGES = new HashMap<>();
    private static final Map<UUID, Vec3> RETURN_POINTS = new HashMap<>();

    // PERF(A): 战场/大本营改成跨 tick 分片任务后，新维度第一次进场要等任务结算再传送，
    // 否则会落进"正在施工"的噪声地形，且 floorY 还是临时值。这里记延后的进场请求。
    /** 玩家 → 延后的进场请求（等战场分片任务结算）。 */
    private static final Map<UUID, PendingEnter> PENDING_ENTRY = new HashMap<>();

    /** 延后进场最长等多久（60 秒），超时就通知失败、不无限挂着。 */
    private static final long PENDING_ENTRY_TIMEOUT_TICKS = 20L * 60L;

    /** 一次延后的进场：战场/大本营还在分片施工，结算后再走原传送流程。 */
    private record PendingEnter(Dungeon dungeon, CultivationData data, int enterTier, long deadlineTick) {
    }
    /**
     * 通关冷却：键是“玩家 UUID#副本名”。
     *
     * <p>以前共用一份玩家级冷却：连续打第二座不同副本会提示“余韵未散”拿不到奖励
     * （审计 A-8）。按副本分开才对——同一座副本仍要等 3 分钟。</p>
     */
    private static final Map<String, Long> LAST_CLEAR = new HashMap<>();
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private DungeonRealm() {
    }

    // ------------------------------------------------------------------
    // 维度
    // ------------------------------------------------------------------

    /** 副本维度的 {@link ResourceKey}，例如 {@code hongmeng:beast_realm}。 */
    public static ResourceKey<Level> key(Dungeon dungeon) {
        return KEYS.computeIfAbsent(dungeon, d -> ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation("hongmeng", d.dimensionId())));
    }

    public static ServerLevel level(MinecraftServer server, Dungeon dungeon) {
        return server == null ? null : server.getLevel(key(dungeon));
    }

    /** 这个玩家此刻是不是就在这座副本里。 */
    public static boolean isIn(ServerPlayer player, Dungeon dungeon) {
        return player != null && player.level().dimension().equals(key(dungeon));
    }

    /** 玩家现在身处哪座副本（不在副本里则返回 {@code null}）。 */
    public static Dungeon dungeonAt(Level level) {
        return level == null ? null : Dungeon.byDimension(level.dimension().location().toString());
    }

    /**
     * 大境序号 → 中文境名（"金丹""道印"…）。
     *
     * <p>用公开 API 反查，不去碰 {@code Realm} 的私有表。</p>
     */
    public static String tierName(int tier) {
        for (int step = 0; step < Realm.TOTAL_STEPS; step++) {
            Realm realm = Realm.ofStep(step);
            if (realm.tier() >= tier) {
                return realm.tierCn();
            }
        }
        return Realm.ofStep(Realm.TOTAL_STEPS - 1).tierCn();
    }

    // ------------------------------------------------------------------
    // 进出秘境
    // ------------------------------------------------------------------

    /**
     * 处理一次「延后进场」：战场分片任务结算后重走 {@link #enter}（此时 floorY / 区块都就绪）。
     *
     * <p>PERF(A)：老进场流程同步施工会卡 1.5~8 秒；现在先给玩家一句提示，
     * 场景在后台分片铺开，结算后自动传送，不会卡住也不会重复传送
     *（{@link #enter} 内部还会做一次 {@code isIn} 校验）。</p>
     */
    private static void handlePendingEntry(ServerPlayer player, PendingEnter pending) {
        UUID id = player.getUUID();
        if (isIn(player, pending.dungeon())) {
            PENDING_ENTRY.remove(id);   // 玩家已经在该维度（命令/其它路径）：不再重复传送
            return;
        }
        if (DungeonArena.isSettled(pending.dungeon())) {
            PENDING_ENTRY.remove(id);
            enter(player, pending.dungeon(), pending.data());
            return;
        }
        if (player.level().getGameTime() > pending.deadlineTick()) {
            PENDING_ENTRY.remove(id);
            player.sendSystemMessage(Component.literal(
                    "\u00a7c秘境生成超时，请稍后再试。"));
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[arena] {} 进场等待超时（玩家={}），已取消",
                    pending.dungeon().dimensionId(), player.getName().getString());
            return;
        }
        if (player.level().getGameTime() % 40L == 0L) {
            player.displayClientMessage(Component.literal(
                    "\u00a77秘境场景正在分片生成…"), true);
        }
    }

    /**
     * 把玩家送进副本并开始挑战。
     *
     * <p>签名与 {@code HongmengCommand} 里 {@code /hongmeng dungeon enter} 的调用一致：
     * {@code enter(player, dungeon, data)}。</p>
     */
    public static void enter(ServerPlayer player, Dungeon dungeon, CultivationData data) {
        if (!(player.level() instanceof ServerLevel current)) {
            return;
        }
        ServerLevel target = level(current.getServer(), dungeon);
        if (target == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c" + dungeon.cn() + " 未能开启（维度未加载）。"));
            return;
        }
        if (isIn(player, dungeon)) {
            player.sendSystemMessage(Component.literal("\u00a77你已经在秘境里了。"));
            return;
        }
        if (!data.isAwakened()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c你连灵根都没有，进去就是送菜。"));
            return;
        }
        if (!dungeon.canEnter(data.realm().tier())) {
            player.sendSystemMessage(Component.literal(dungeon.colored()
                    + "\u00a7c需要 \u00a7e" + tierName(Math.max(1,
                            dungeon.minTier() - Dungeon.ENTRY_GRACE))
                    + " \u00a7c以上的修为。你目前是 \u00a7f" + data.realm().fullCn() + "\u00a7c。"
                    + "\u00a78（建议 " + tierName(dungeon.minTier())
                    + " 再来，收获会更好）"));
            return;
        }
        // 低于**建议**境界但被宽容放行：明确告诉玩家怪已相应削弱、收获会打折。
        // 不提示的话玩家会以为"这副本就这么简单"，也就失去了提升境界的引导。
        String warn = dungeon.underTierWarning(data.realm().tier());
        if (warn != null) {
            player.sendSystemMessage(Component.literal(dungeon.colored() + warn));
        }

        // 已经在别的副本里就先撤出来，免得挑战状态串味
        if (inChallenge(player)) {
            abort(player);
        }

        int enterTier = data.realm().tier();
        // 每次进门都刷新返回点：以前用 putIfAbsent，上一次挑战没走 leave() 就断线/死掉时
        // 会留下旧的返回点，下一次通关被送回"很久以前的位置"（玩家反馈过落点很远）。
        RETURN_POINTS.put(player.getUUID(), player.position());

        // 必须先把目标区块立起来：否则 getHeight 会把我们放在海平面上，
        // 实体也会因为没有区块而被丢弃。
        // PERF(A): 战斗场地/大本营改成跨 tick 分片任务（不再同步写 1.6~1.8 万格）。
        // 先入队；还没结算就记下延后进场（tick 里结算后自动继续），避免落进未建好的地形。
        DungeonArena.ensureBuilt(target, dungeon);
        if (!DungeonArena.isSettled(dungeon)) {
            PENDING_ENTRY.put(player.getUUID(), new PendingEnter(dungeon, data, enterTier,
                    target.getGameTime() + PENDING_ENTRY_TIMEOUT_TICKS));
            player.sendSystemMessage(Component.literal(
                    "\u00a77秘境正在成形（场景分片生成中），请稍候…"));
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[arena] {} 进场延后：等待战场分片任务结算（玩家={}）",
                    dungeon.dimensionId(), player.getName().getString());
            return;
        }
        // 战场已结算：目标区块此时已被分片任务的 ticket 预载过，这次 forceLoadArea
        // 只会命中已加载区块（同步 getChunk 立即返回，不再现场生成）。
        forceLoadArea(target, new BlockPos(ARENA_X, 0, ARENA_Z));
        // 外围主题地貌：r=44~112 的环带场景（灵田梯田 / 巨骨拱 / 熔岩裂缝 / 浮空岛…），
        // 自带 SavedData + 版本号的幂等标记，重复进门只读标记、不会重复施工。
        RealmTheme.ensure(target, dungeon, DungeonArena.floorY(target, dungeon));
        // 战斗场地前庭：把 r=25~43 那圈"看台脚下的噪声地形"整平成主题广场
        // （铺地/踏道/门阙/主题小景）。独立的 SavedData + VERSION，老存档第一次进门补建一次。
        try {
            DungeonForecourt.ensureRealm(target, dungeon, DungeonArena.floorY(target, dungeon));
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[forecourt] {} 副本前庭生成失败（不影响副本可玩）：{}",
                    dungeon.dimensionId(), t.toString());
        }
        BlockPos landing = findLanding(target, ARENA_X, ARENA_Z);
        // ---- 落点诊断 + 兜底 ----
        // 玩家反馈过"进维度落点离设计好的出生点很远"。竞技场建好时落点必须钉在场地内：
        // 旧存档地形/水面/树把 findLanding 逼到 64 格外的径向落点时，这里直接改回中央阵眼上方。
        int enterFloorY = DungeonArena.floorY(target, dungeon);
        double landingDist = Math.hypot(landing.getX() - ARENA_X, landing.getZ() - ARENA_Z);
        if (landingDist > 20.0D && DungeonArena.isBuilt(dungeon)) {
            BlockPos fixed = new BlockPos(ARENA_X, enterFloorY + 4, ARENA_Z);
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[arena] {} 进门落点 {} 距场地中心 {} 格，改用中央落点 {}",
                    dungeon.dimensionId(), landing, String.format("%.1f", landingDist), fixed);
            landing = fixed;
            landingDist = Math.hypot(landing.getX() - ARENA_X, landing.getZ() - ARENA_Z);
        }
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 进门落点 {}（距场地中心 {} 格，floorY={}）",
                dungeon.dimensionId(), landing, String.format("%.1f", landingDist), enterFloorY);
        // 记下副本维度落点：首领战后的 30 秒倒计时结束时回到这里自由探索。
        REALM_LANDING.put(player.getUUID(), landing);
        EXIT_COUNTDOWN.remove(player.getUUID());
        // 落地就给一个朝向：**正对大本营（北侧）**。
        // 玩家要求「进去维度就是面对 boss 大本营」。
        // Minecraft 里 yaw=0 朝南、yaw=180 朝北，所以这里给 180。
        player.moveTo(landing.getX() + 0.5D, landing.getY(),
                landing.getZ() + 0.5D, 180.0F, 0.0F);

        player.teleportTo(target, landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        // 跟原版下界门一样给一段传送冷却，免得刚落地就被反向吸回去
        player.setPortalCooldown();
        // 传送后立刻留痕：下次“人不在竞技场”时可直接对比 实际坐标 vs 目标坐标
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 传送完成：维度={} 实际位置={} 目标={}",
                dungeon.dimensionId(), player.level().dimension().location(),
                player.blockPosition(), landing);
        // 换维度那一 tick 的传送确认竞态：先把禁足宽限 + 落点校正排上
        long nowTick = target.getGameTime();
        ENTRY_GUARD.put(player.getUUID(),
                new EntryGuard(dungeon, landing, nowTick + ENTRY_GUARD_TICKS));
        BAN_GRACE.put(player.getUUID(), nowTick + ENTRY_GUARD_TICKS);
        if (player.level() != target) {
            // 极少见：次元传送被事件取消/没生效。排到下一 tick 再传一次。
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[arena] {} 进门次元传送未生效（仍在 {}），下一 tick 重试",
                    dungeon.dimensionId(), player.level().dimension().location());
            final BlockPos retry = landing;
            final ServerLevel retryTarget = target;
            MinecraftServer srv = current.getServer();
            if (srv != null) {
                srv.execute(() -> {
                    if (player.isRemoved() || player.level() == retryTarget) {
                        return;
                    }
                    player.teleportTo(retryTarget, retry.getX() + 0.5D, retry.getY(),
                            retry.getZ() + 0.5D, 180.0F, 0.0F);
                    player.setPortalCooldown();
                    com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                            "[arena] {} 进门次元传送重试完成：{}",
                            dungeon.dimensionId(), player.blockPosition());
                });
            }
        }

        double scale = dungeon.mobScaleFor(enterTier);
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00a79\u00a7l═══ " + dungeon.cn() + " ═══"));
        player.sendSystemMessage(Component.literal("\u00a77" + dungeon.desc() + "。"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f你的境界：\u00a7e" + data.realm().fullCn()
                        + "   \u00a7f副本基准：\u00a7b" + tierName(dungeon.baseTier())
                        + "   \u00a7f怪物强度：\u00a7c×" + String.format("%.2f", scale)));
        // ⚠ 以前这里只写"副本基准"，但怪的境界其实锚定**玩家**，
        //   于是显示与实现不一致（玩家到了鸿蒙，进副本怪也全是鸿蒙）。
        //   现在怪的锚点就是副本基准，所以这句写的是真的。
        player.sendSystemMessage(Component.literal(
                "\u00a77本副本怪物境界：\u00a7b" + tierName(dungeon.baseTier())
                        + "\u00a78（副本内不再跟随你的境界）"));
        player.sendSystemMessage(Component.literal(
                "\u00a77共 \u00a7e" + dungeon.waves() + " \u00a77波，最后一波是首领。"
                        + "通关奖励修为 \u00a7a" + dungeon.rewardFor(enterTier)));
        player.sendSystemMessage(Component.literal(
                "\u00a77潜行 + 空手右键可以随时退出（进度作废）。"));
        player.sendSystemMessage(Component.literal(""));

        target.playSound(null, landing, SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS, 1.0F, 0.8F);

        startChallenge(player, data, dungeon, enterTier);
    }

    /**
     * 退出副本：回到进门的位置，挑战作废。
     *
     * <p>签名与 {@code HongmengCommand} 里 {@code /hongmeng dungeon leave} 的调用一致。</p>
     */
    public static void leave(ServerPlayer player, Dungeon dungeon) {
        ServerLevel overworld = player.getServer() == null ? null
                : player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        Vec3 back = RETURN_POINTS.remove(player.getUUID());
        BlockPos raw = back != null ? BlockPos.containing(back) : overworld.getSharedSpawnPos();
        BlockPos pos = safeReturn(overworld, raw);
        cleanupChallenge(player);
        player.teleportTo(overworld, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        // 关键：进门的位置就在门里，不给冷却的话一出来就会被再吸进去，来回死循环。
        player.setPortalCooldown();
        player.sendSystemMessage(Component.literal("\u00a77你退出了 " + dungeon.colored()
                + "\u00a77。（挑战进度已作废。）"));
        ModNetworkBridge.sync(player);
    }

    /**
     * 退出落点兜底：进门的位置就在传送门方块里，直接放回去会跟门重叠。
     *
     * <p>先看原位；重叠的话在附近找一格"不压着门、脚下有地、头顶两格空"的位置。
     * 实在找不到就原样返回——上面的传送冷却还会兜最后一道。</p>
     */
    private static BlockPos safeReturn(ServerLevel level, BlockPos pos) {
        if (!isPortal(level, pos) && !isPortal(level, pos.above())) {
            return pos;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            if (standable(level, side)) {
                return side;
            }
        }
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos up = pos.above(dy);
            if (standable(level, up)) {
                return up;
            }
        }
        return pos;
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        if (isPortal(level, pos) || isPortal(level, pos.above())) {
            return false;
        }
        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
            return false;
        }
        return !level.getBlockState(pos.below()).isAir();
    }

    private static boolean isPortal(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(com.hongmeng.xiuxian.ModBlocks.DUNGEON_PORTAL.get());
    }

    /** 强制加载以 center 为中心的 5×5 区块，保证挑战场地是活的。 */
    private static void forceLoadArea(ServerLevel level, BlockPos center) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz,
                        net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
            }
        }
    }

    /**
     * 找一块能站的地面。
     *
     * <p>先看原点，不行就以 8 格为步长向外螺旋找——秘境是噪声地形，
     * 落点正赶上水面 / 岩浆面上时得挪一挪，不然一进门就呛水。</p>
     */
    private static BlockPos findLanding(ServerLevel level, int x, int z) {
        BlockPos direct = solidColumn(level, x, z);
        if (direct != null) {
            return direct;
        }
        for (int radius = 8; radius <= 64; radius += 8) {
            for (int dx = -radius; dx <= radius; dx += 8) {
                for (int dz = -radius; dz <= radius; dz += 8) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    BlockPos found = solidColumn(level, x + dx, z + dz);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
    }

    /** 这一列有没有"脚底下是实心、头顶两格空着"的落点。 */
    private static BlockPos solidColumn(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos base = new BlockPos(x, top, z);
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos candidate = base.above(dy);
            BlockPos below = candidate.below();
            BlockState ground = level.getBlockState(below);
            if (ground.isAir() || !ground.getFluidState().isEmpty()) {
                continue;
            }
            if (ground.getCollisionShape(level, below).isEmpty()) {
                continue;
            }
            if (level.isEmptyBlock(candidate) && level.isEmptyBlock(candidate.above())) {
                return candidate;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 挑战流程
    // ------------------------------------------------------------------

    public static void startChallenge(ServerPlayer player, CultivationData data, Dungeon dungeon,
                                      int enterTier) {
        cleanupChallenge(player);
        long now = player.level().getGameTime();
        // 诊断：每次进本都明确打一行，便于核对“后续进入是否仍有禁足”
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[dungeon] {} 挑战开始：玩家={} 共 {} 波（禁足 ±{} 格生效，打完首领解除）",
                dungeon.dimensionId(), player.getName().getString(), dungeon.waves(),
                (long) BAN_HALF);
        CHALLENGES.put(player.getUUID(),
                new Challenge(dungeon, 0, new HashSet<>(), now, enterTier));
        showBossBar(player, dungeon, 1, waveCount(dungeon, 0));
        spawnWave(player, data, 0);
    }

    /**
     * 进门落点校正：换维度后的 100 tick 内检查一次落点。
     *
     * <p>换维度时 `teleportTo` 的服务端位置/客户端确认与同一 tick 的禁足判定存在竞态：
     * 玩家可能被留在“旧坐标 + 新维度”，而竞技场（0,0）在 160 格之外，
     * 表现为“传送过来却不在远处的竞技场”。这里在下一 tick 直接把人钉回落点。</p>
     */
    private static void enforceEntryGuard(ServerPlayer player) {
        EntryGuard guard = ENTRY_GUARD.get(player.getUUID());
        if (guard == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now > guard.untilTick()) {
            ENTRY_GUARD.remove(player.getUUID());
            return;
        }
        if (!(player.level() instanceof ServerLevel level)
                || dungeonAt(level) != guard.dungeon()) {
            // 已经离开该副本维度：不再校正
            ENTRY_GUARD.remove(player.getUUID());
            return;
        }
        double dx = player.getX() - (guard.landing().getX() + 0.5D);
        double dz = player.getZ() - (guard.landing().getZ() + 0.5D);
        if (dx * dx + dz * dz <= ENTRY_GUARD_RADIUS * ENTRY_GUARD_RADIUS) {
            ENTRY_GUARD.remove(player.getUUID());
            return;
        }
        com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                "[arena] {} 进门落点校正：{} 实际在 ({}, {})，拉回竞技场落点 {}",
                guard.dungeon().dimensionId(), player.getName().getString(),
                String.format("%.1f", player.getX()), String.format("%.1f", player.getZ()),
                guard.landing());
        player.teleportTo(level, guard.landing().getX() + 0.5D, guard.landing().getY(),
                guard.landing().getZ() + 0.5D, 180.0F, 0.0F);
        player.setPortalCooldown();
        ENTRY_GUARD.remove(player.getUUID());
    }

    /** 由 {@link DungeonRealmEvents} 每 tick 驱动。 */
    public static void tick(ServerPlayer player, CultivationData data) {
        // ---- PERF(A): 延后进场（等战场分片任务结算后才传送）----
        PendingEnter pending = PENDING_ENTRY.get(player.getUUID());
        if (pending != null) {
            handlePendingEntry(player, pending);
            return;                     // 等待期间不推进波次
        }
        // ---- 进门落点校正（换维度竞态兜底）----
        enforceEntryGuard(player);
        // ---- 首领战后的 30 秒战利品倒计时 ----
        // 期间挑战已经结算完（complete 已清），玩家仍留在首领位面开宝箱；
        // 倒计时结束自动送回副本维度落点，解除禁足、可自由探索。
        ExitCountdown exit = EXIT_COUNTDOWN.get(player.getUUID());
        if (exit != null) {
            long now = player.level().getGameTime();
            if (!inArenaOf(player, exit.dungeon())) {
                // 已经不在首领位面（死亡 / 命令传送 / 提前撤离）：清掉倒计时，不重复传送
                EXIT_COUNTDOWN.remove(player.getUUID());
            } else if (now >= exit.endTick()) {
                finishExitCountdown(player, exit);
                return;
            } else {
                if (now % 20L == 0L) {
                    long sec = (exit.endTick() - now + 19L) / 20L;
                    player.displayClientMessage(Component.literal(
                            "\u00a76首领已伏诛 \u00a7e" + sec
                                    + "\u00a77 秒后自动离开首领位面"), true);
                }
                return;
            }
        }
        Challenge ch = CHALLENGES.get(player.getUUID());
        if (ch == null) {
            // ---- 兜底：人在 boss 维度里、但已经没有挑战了 → 送回原世界 ----
            //
            // 为什么需要：任何一条遗漏的路径（boss 被别的怪打死、玩家自己死掉、
            // 挑战被作废……）都可能把玩家**永久困在 boss 维度**里出不去。
            // 玩家实测过「打完 boss 也走不了」，所以这里必须兜住。
            if (ArenaRealm.isInArena(player)
                    && player.level().getGameTime() % 20L == 0L
                    && ArenaReturn.has(player.getUUID())) {
                // 只兜底“从副本正常传送进来、却没挑战了”的情况。
                // 命令/创造模式直接进 boss 维度探索的玩家不应该每 20 tick 被弹走
                // —— 玩家反馈的「进去其他维度一开始就被传送走」有一部分就是它。
                // 直接进来的人没有来源记录，走登录兜底（escapeStrandedArena）。
                ArenaReturn.sendBack(player, "\u00a77你被送回了原来的世界。");
            }
            return;
        }
        // ---- 决战阶段：挑战允许存在于“本副本对应的 boss 决战维度”里 ----
        //
        // ⚠ 这是旧版最大的坑：清完最后一波传送进 arena_* 维度后，
        //   下一 tick 这里发现“玩家不在副本维度”就把挑战清掉，
        //   再下一 tick 的兜底又把玩家送回原来的世界 ——
        //   玩家看到的就是「打完所有波次根本进不了决战场地」「进去一秒就被弹走」。
        //   现在只有真的离开决战维度才作废；人还在 arena 里就保留挑战，
        //   等祭坛召唤首领、打死首领后由 onArenaBossKilled 结算。
        if (ch.arenaSent()) {
            if (inArenaOf(player, ch.dungeon())) {
                return;          // 决战阶段：波次不再推进，等祭坛召唤
            }
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[arena] {} 的决战挑战作废：玩家已离开决战维度（当前 {}）",
                    ch.dungeon().dimensionId(), player.level().dimension().location());
            ArenaReturn.forget(player);
            ArenaRealm.forget(player);
            cleanupChallenge(player);
            return;
        }
        if (!isIn(player, ch.dungeon())) {
            // 跑到别处去了，作废（连带清掉可能残留的来源记录）
            cleanupChallenge(player);
            ArenaReturn.forget(player);
            ArenaRealm.forget(player);
            return;
        }

        // 清理已死的记录
        ch.mobs().removeIf(id -> {
            Entity entity = ((ServerLevel) player.level()).getEntity(id);
            return entity == null || !entity.isAlive();
        });

        long now = player.level().getGameTime();
        int nextWave = ch.wave() + 1;

        if (!ch.mobs().isEmpty()) {
            // PERF(Lead)：Boss 条每 10 tick 更新一次就够（原来每 tick 发一次进度包）
            if (now % 10L == 0L) {
                updateBossBar(player, ch.dungeon(), ch.wave() + 1, ch.mobs().size());
            }
            return;
        }

        // 诊断：把每一波清完时的判定实测值打出来 ——
        // 玩家报告"最后一波传送不到祭坛"，这一行能直接看出是判定没进还是传送失败。
        //
        // PERF(Lead)：这行原来打在前面 —— 而"清完 → 等下一波"的 8 秒里判定每 tick 都成立，
        // 实机日志刷了 66~82 行/10 秒（还会拖慢日志线程）。现在只在真正推进波次时打一次。
        if (nextWave >= ch.dungeon().waves()) {
            // ---- 最后一波清完：**传送到决战竞技场**，不再直接通关 ----
            //
            // 玩家要求：「这个模块可以做成在最后一波怪潮击杀完成后传送到专门战斗的场地，
            // 场地……最终 boss 战场地然后有个祭坛，右击互动召唤 boss」。
            //
            // 所以这里改成：把玩家送进竞技场，让他在中央祭坛右键召唤首领；
            // **打完首领才算通关**（见 ArenaBattle / killBossInArena）。
            // ⚠ 只送一次！
            //
            // 原来传送后把 mobs 清空，于是**下一 tick 条件再次成立** →
            // 每 tick 重复传送 + 重复发消息。玩家实测"被传送过去根本没办法走动"，
            // 因为每秒被传送 20 次、聊天栏还刷屏（真实事故）。
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[arena] {} 最后一波清完（arenaSent={}），准备进决战竞技场",
                    ch.dungeon().dimensionId(), ch.arenaSent());
            if (!ch.arenaSent() && player.level() instanceof ServerLevel arenaLevel) {
                int fy = DungeonArena.floorY(arenaLevel, ch.dungeon());
                com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                        "[arena] 准备传送：floorY={} 目标中心={}",
                        fy, com.hongmeng.xiuxian.dungeon.ArenaBattle.center(fy));
                // 先记下来源坐标，打完 boss 好送回去（否则玩家会被困在 boss 维度）
                com.hongmeng.xiuxian.dungeon.ArenaReturn.remember(player);
                boolean began = ArenaBattle.begin(player, arenaLevel, ch.dungeon(), fy);
                com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                        "[arena] begin(...) 返回 {}", began);
                if (began) {
                    // 怪潮的 Boss 条到此为止；后面交给祭坛首领自己的 BOSS 条。
                    clearBossBar(player);
                    CHALLENGES.put(player.getUUID(), new Challenge(ch.dungeon(),
                            ch.wave(), new HashSet<>(), now, ch.enterTier(), true));
                    com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                            "[arena] {} 决战阶段开始：挑战转入 arenaSent=true（玩家 {}）",
                            ch.dungeon().dimensionId(), player.getName().getString());
                    return;
                }
            }
            // 已经在竞技场里等着打首领 —— 什么都不做（不要重复触发）
            if (ch.arenaSent()) {
                return;
            }
            // 竞技场建不出来（异常/维度不对）时退回原逻辑，别把玩家卡住
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[arena] 竞技场不可用（begin 返回 false），退回直接通关");
            ArenaReturn.forget(player);
            complete(player, data, ch);
            return;
        }
        if (now - ch.lastWaveTick() < WAVE_GAP_TICKS) {
            // PERF(Lead)：等待下一波的 8 秒里原来每 tick 都发动作栏消息（含每 tick new Component），
            // 现在每 10 tick 发一次，观感不变。
            if (now % 10L == 0L) {
                long remain = (WAVE_GAP_TICKS - (now - ch.lastWaveTick())) / 20L;
                player.displayClientMessage(Component.literal(
                        "\u00a77下一波还有 " + remain + " 秒…"), true);
            }
            return;
        }
        // 真正推进波次时打一条"上一波清完"的诊断（每波仅一次，替代原来每 tick 的刷屏）
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 第 {} 波清完：nextWave={} waves={} arenaSent={}",
                ch.dungeon().dimensionId(), ch.wave() + 1, nextWave,
                ch.dungeon().waves(), ch.arenaSent());
        CHALLENGES.put(player.getUUID(), new Challenge(ch.dungeon(), nextWave,
                new HashSet<>(), now, ch.enterTier()));
        showBossBar(player, ch.dungeon(), nextWave + 1, waveCount(ch.dungeon(), nextWave));
        spawnWave(player, data, nextWave);
    }

    /** 第 wave 波刷几只。首领波固定 3 只，其余随波次递增；基准境界越高人越多。 */
    /**
     * 一波多少只 —— **真正的怪潮**（玩家：『副本里的怪物多一点…要有真的被怪物攻城的感觉』）。
     *
     * <p>旧值是"最后一波 3 个、其它 3+wave 个"，后期副本也就 3~9 只，谈不上攻城。
     * 现在按<b>副本基础档位</b>放大：{@code (12 + baseTier*2) + wave*(4 + baseTier)}。</p>
     *
     * <p>纪元(baseTier 1) 第 1 波 14 只；虚空(baseTier 8) 第 4 波 64 只。
     * 上限 {@link #SIEGE_CAP} 兜底，避免某天档位调大之后一次刷出几百只。</p>
     */
    private static int waveCount(Dungeon dungeon, int wave) {
        int count = (12 + dungeon.baseTier() * 2) + wave * (4 + dungeon.baseTier());
        return Math.min(SIEGE_CAP, Math.max(6, count));
    }

    /** 单波硬上限。 */
    private static final int SIEGE_CAP = 96;

    /**
     * PERF(Lead)：<b>同场存活怪潮上限</b>。
     *
     * <p>实机日志（2026-11-07 20:25~20:29 虚空秘境）：高境界玩家 AOE 秒清一波，
     * 波次立刻续刷，怪物 + 掉落 + 死亡特效在场上叠加，服务端从 2s 一路恶化到 26s
     * behind。这里限制"同时活着"的怪潮怪数量：超过上限就把下一批往后排，
     * 总怪数/波次完全不变，只是把"同时存在"的实体数压在一个稳定值，
     * 避免实体/掉落越打越多形成雪球。</p>
     */
    private static final int SIEGE_CONCURRENT_CAP = 48;

    /** 每批之间间隔多少 tick（分批入场，做出"潮水一波波涌上来"的观感）。 */
    private static final int SIEGE_BATCH_TICKS = 20;

    /** 分几批。 */
    private static final int SIEGE_BATCHES = 4;

    /**
     * 刷一波 —— **分批入场的怪潮**（玩家：『打副本要有真的被怪物攻城的感觉』）。
     *
     * <p>旧实现一次性刷完、每波只有 1 只写死的"首领"。新版：</p>
     * <ol>
     *   <li>按 {@link #SIEGE_TIERS} 给<b>每一只</b>定品阶（小兵/精英/队长/头目），
     *       最后一波再塞 1~2 只首领；</li>
     *   <li>品阶走 {@link #siegePick} 选怪、{@link #siegeTierBonus} 加属性；</li>
     *   <li><b>分 4 批</b>（每批 20 tick）依次落下 —— 潮水一波波涌上来，
     *       而不是"啪一下全出现"；分批清单挂在 {@link Challenge} 上，
     *       由 {@code tickSiege} 按 tick 推进。</li>
     * </ol>
     */
    private static void spawnWave(ServerPlayer player, CultivationData data, int wave) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Challenge ch = CHALLENGES.get(player.getUUID());
        if (ch == null) {
            return;
        }
        Dungeon dungeon = ch.dungeon();
        int count = waveCount(dungeon, wave);
        double[] tierMix = siegeTier(dungeon, wave);
        int bosses = siegeBossCount(dungeon, wave);
        double scale = dungeon.mobScaleFor(ch.enterTier()) * (1.0D + wave * WAVE_BONUS);

        // ---- 先给每一只定好"品阶"，再分批发 ----
        List<Integer> tiers = new ArrayList<>();
        for (int i = 0; i < Math.max(0, count - bosses); i++) {
            tiers.add(rollSiegeTier(level, tierMix));
        }
        // 首领固定放在最后一批（压轴登场）
        for (int i = 0; i < bosses; i++) {
            tiers.add(4);
        }

        ch.mobs().clear();
        SIEGE_PENDING.put(player.getUUID(),
                new SiegeQueue(dungeon, scale, tiers, level.getGameTime()));

        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE, 1.0F, 1.0F);
        player.sendSystemMessage(Component.literal("\u00a7c【第 " + (wave + 1) + " 波】\u00a7f"
                + (bosses > 0 ? "首领现身——" : "敌潮涌来——") + count + " 个"
                + "\u00a78（分 " + SIEGE_BATCHES + " 批压上）"));
        updateBossBar(player, dungeon, wave + 1, count);
    }

    /** 按配比掷一个品阶（0 小兵 / 1 精英 / 2 队长 / 3 头目）。 */
    private static int rollSiegeTier(ServerLevel level, double[] mix) {
        double p = level.random.nextDouble();
        double acc = 0.0D;
        for (int t = 0; t < mix.length; t++) {
            acc += mix[t];
            if (p < acc) {
                return t;
            }
        }
        return 0;
    }

    /**
     * 每 tick 推进"怪潮批次"。
     *
     * @return true 表示这一 tick 放了人（调用方不必再做别的）
     */
    public static boolean tickSiege(ServerPlayer player) {
        SiegeQueue q = SIEGE_PENDING.get(player.getUUID());
        if (q == null) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            SIEGE_PENDING.remove(player.getUUID());
            return false;
        }
        long now = level.getGameTime();
        if (now < q.nextAt()) {
            return false;
        }
        int total = q.tiers().size();
        int perBatch = Math.max(1, (total + SIEGE_BATCHES - 1) / SIEGE_BATCHES);
        int from = q.cursor();
        int to = Math.min(total, from + perBatch);
        if (from >= total) {
            SIEGE_PENDING.remove(player.getUUID());
            return false;
        }

        Challenge ch = CHALLENGES.get(player.getUUID());
        if (ch == null) {
            SIEGE_PENDING.remove(player.getUUID());
            return false;
        }
        // PERF(Lead)：同场存活上限 —— 超过就把这一批往后排（总怪数不变，只是分批放慢）
        if (ch.mobs().size() >= SIEGE_CONCURRENT_CAP) {
            q.setNextAt(now + SIEGE_BATCH_TICKS);
            return true;
        }
        List<Mob> spawned = new ArrayList<>();
        for (int i = from; i < to; i++) {
            int tier = q.tiers().get(i);
            Mob mob = spawnOne(level, ch.dungeon(), player, q.scale(), tier, i, total);
            if (mob != null) {
                spawned.add(mob);
            }
        }
        for (Mob mob : spawned) {
            ch.mobs().add(mob.getUUID());
        }
        q.setCursor(to);
        q.setNextAt(now + SIEGE_BATCH_TICKS);
        // 每批落地的闷响 + 末批怒吼
        level.playSound(null, player.blockPosition(),
                to >= total ? SoundEvents.RAVAGER_ROAR : SoundEvents.RAVAGER_STEP,
                SoundSource.HOSTILE, 0.9F, to >= total ? 0.8F : 1.3F);
        if (to >= total) {
            SIEGE_PENDING.remove(player.getUUID());
        }
        return true;
    }

    /** 刷一只，含品阶属性与站位。 */
    private static Mob spawnOne(ServerLevel level, Dungeon dungeon, ServerPlayer player,
                                double scale, int tier, int index, int total) {
        EntityType<? extends Mob> type = tier >= 4
                ? siegeBossType(dungeon) : siegePick(level, dungeon, tier);
        if (type == null) {
            return null;
        }
        Mob mob = type.create(level);
        if (mob == null) {
            return null;
        }
        mob.setPersistenceRequired();
        // 2026-11-07 玩家要求：去除怪物外圈白框（不再施加 GLOWING）。
        Vec3 pos = ringPosition(player, 8.0D + (index % 9) * 1.5D, index, total);
        mob.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)),
                MobSpawnType.EVENT, null, null);
        // 2026-11-07：boss 槽的 12 倍 → 6 倍（头目/首领削弱）。
        // 12 是在"护甲 99% 减伤"时代定的，加上品阶倍率后头目血量是普通怪的 300 倍；
        // 现在护甲 92% 封顶，头目又太肉/太疼，减到 6 倍让"头目"更像精英 Plus 而不是血牛。
        double tierMul = tier >= 4 ? BOSS_SLOT_HP_MUL : siegeTierBonus(tier);
        double healthMul = scale * tierMul;
        // 伤害只跟"副本难度 scale"缓慢放大，**不再**跟着 healthMul（乘了品阶倍率）指数涨。
        // 旧公式 1+(healthMul-1)×0.6 在护甲 92% 封顶后会打出同境玩家 300%~500% 生命的单发，
        // 头目一击秒人；新公式下头目单发约 25%~40%，三阶段狂暴 50%~70%，"疼但能反应"。
        double damageMul = 1.0D + (scale - 1.0D) * 0.10D;
        MobScaling.applyScaling(mob, healthMul, damageMul,
                "hongmeng_dungeon_" + dungeon.dimensionId());
        mob.setHealth(mob.getMaxHealth());
        // ---- 按槽位**降级**（实例级品阶）----
        //
        // ⚠ 刷怪表里会"用 boss 级实体当普通怪"，例如虚空裂隙：
        //     case VOID -> ... : (roll < 55 ? DEMON_GENERAL : BLOOD_DEMON_ELDER);
        //   而 DEMON_GENERAL / VOID_WALKER 都是 BOSS 品阶 —— 于是**每只都带 boss 血条**
        //   （玩家截图里 6 条血条叠在一起就是这个）。
        //
        // 现在按它所在的**槽位**把品阶压下来：普通槽→普通、精英槽→精英、队长槽→队长，
        // 只有 boss 槽保留 boss 级。属性缩放照旧走 tierMul，不受影响。
        if (mob instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity cm) {
            com.hongmeng.xiuxian.entity.MobRank slot = switch (tier) {
                case 4 -> com.hongmeng.xiuxian.entity.MobRank.BOSS;
                case 3 -> com.hongmeng.xiuxian.entity.MobRank.CAPTAIN;
                case 2 -> com.hongmeng.xiuxian.entity.MobRank.ELITE;
                default -> com.hongmeng.xiuxian.entity.MobRank.NORMAL;
            };
            if (cm.rank().ordinal() > slot.ordinal()) {
                cm.demoteRank(slot);
            }
        }
        return level.addFreshEntity(mob) ? mob : null;
    }

    /** 首领的实体类型。 */
    private static EntityType<? extends Mob> siegeBossType(Dungeon dungeon) {
        return switch (dungeon) {
            case EPOCH, BEAST -> ModEntities.PUPPET_CAPTAIN.get();
            case DEMON -> ModEntities.DEMON_GENERAL.get();
            case VOID -> ModEntities.VOID_OVERLORD.get();
        };
    }

    /**
     * <b>禁足：20×20 区块（320×320 格）</b>。
     *
     * <p>玩家要求：「打完 boss 之前不能出去这个 20*20 的范围」。</p>
     *
     * <p>只在**挑战进行中**生效；越界就把人拉回边界内侧并提示（只拦不罚，
     * 免得在边界上被来回弹）。主动退出副本（潜行+空手右键）不受影响 ——
     * 那会先把 Challenge 清掉，这里自然就不再拦。</p>
     */
    public static void tickBanField(ServerPlayer player) {
        try {
            Challenge ch = CHALLENGES.get(player.getUUID());
            if (ch == null) {
                return;      // 没有挑战 = 自由活动
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            long nowTick = level.getGameTime();
            Long grace = BAN_GRACE.get(player.getUUID());
            if (grace != null) {
                if (nowTick < grace) {
                    return;  // 刚进门：等传送/落点校正落定再判禁足
                }
                BAN_GRACE.remove(player.getUUID());
            }
            double x = player.getX();
            double z = player.getZ();
            if (Math.abs(x) <= BAN_HALF && Math.abs(z) <= BAN_HALF) {
                return;
            }
            // 拉回边界内侧（留 2 格余量，免得贴着边界反复触发）
            double nx = Math.max(-BAN_HALF + 2.0D, Math.min(BAN_HALF - 2.0D, x));
            double nz = Math.max(-BAN_HALF + 2.0D, Math.min(BAN_HALF - 2.0D, z));
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[dungeon] 禁足拦截：{} 在 {} 越界 ({}, {}) → ({}, {})",
                    player.getName().getString(),
                    player.level().dimension().location(),
                    String.format("%.1f", x), String.format("%.1f", z),
                    String.format("%.1f", nx), String.format("%.1f", nz));
            player.teleportTo(nx, player.getY(), nz);
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.hurtMarked = true;
            long now = level.getGameTime();
            Long last = BAN_NOTICE.get(player.getUUID());
            if (last == null || now - last > 60L) {
                BAN_NOTICE.put(player.getUUID(), now);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7c【禁足】\u00a77挑战未结束，无法离开这片战场"
                                + "\u00a78（打完首领自动解除）"), true);
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 禁足检查异常：{}", t.toString());
        }
    }

    /** 待刷批次（每个玩家一份）。 */
    private static final java.util.Map<java.util.UUID, SiegeQueue> SIEGE_PENDING =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 分批入场的队列。 */
    private static final class SiegeQueue {
        private final Dungeon dungeon;
        private final double scale;
        private final List<Integer> tiers;
        private int cursor;
        private long nextAt;

        SiegeQueue(Dungeon dungeon, double scale, List<Integer> tiers, long now) {
            this.dungeon = dungeon;
            this.scale = scale;
            this.tiers = tiers;
            this.nextAt = now;
        }

        Dungeon dungeon() {
            return dungeon;
        }

        double scale() {
            return scale;
        }

        List<Integer> tiers() {
            return tiers;
        }

        int cursor() {
            return cursor;
        }

        void setCursor(int v) {
            cursor = v;
        }

        long nextAt() {
            return nextAt;
        }

        void setNextAt(long v) {
            nextAt = v;
        }
    }

    /** 按副本挑怪：全部复用修仙界已有那五阶野怪，不新增实体。 */
    private static Mob createWaveMob(ServerLevel level, Dungeon dungeon, boolean leader) {
        EntityType<? extends Mob> type = pickType(level, dungeon, leader);
        if (type == null) {
            return null;
        }
        Mob mob = type.create(level);
        if (mob == null) {
            return null;
        }
        mob.setPersistenceRequired();
        // 2026-11-07 玩家要求：去除怪物外圈白框（不再施加 GLOWING）。
        return mob;
    }


    /**
     * 怪潮的**品阶配比**（玩家：「boss 和强力头目小队长比例在怪潮里适当」）。
     *
     * <p>四档，按波次取：普通波 / 倒数第二波 / 最后一波。
     * 每档给 {小兵, 精英, 队长, 头目} 的占比，最后一波额外保证 1~2 只首领。</p>
     *
     * <p>品阶直接决定属性倍率（走既有的 {@code MobScaling}），
     * 所以"队长 / 头目"是**真的更硬**，不是换个名字。</p>
     */
    private static final double[][] SIEGE_TIERS = {
            // 小兵, 精英, 队长, 头目
            {0.70D, 0.22D, 0.08D, 0.00D},   // 普通波
            {0.50D, 0.28D, 0.16D, 0.06D},   // 倒数第二波
            {0.40D, 0.25D, 0.20D, 0.12D},   // 最后一波（另有首领）
    };

    /** 按波次选配比档。 */
    private static double[] siegeTier(Dungeon dungeon, int wave) {
        int last = dungeon.waves() - 1;
        if (wave >= last) {
            return SIEGE_TIERS[2];
        }
        if (wave == last - 1) {
            return SIEGE_TIERS[1];
        }
        return SIEGE_TIERS[0];
    }

    /** 这一波该不该出首领（只有最后一波，且 1~2 只）。 */
    private static int siegeBossCount(Dungeon dungeon, int wave) {
        return wave >= dungeon.waves() - 1 ? 1 + (wave % 2) : 0;
    }

    /**
     * 副本里可用的**品阶怪物池**：按副本给"小兵 / 精英 / 队长 / 头目"四档的候选。
     *
     * <p>直接用既有的修仙界怪物（它们各自带 {@code MobRank}），
     * 品阶倍率由 {@code MobScaling} 施加，不另写一套。</p>
     */
    private static EntityType<? extends Mob> siegePick(ServerLevel level, Dungeon dungeon,
                                                       int tier) {
        boolean elite = tier >= 1;
        boolean captain = tier >= 2;
        boolean boss = tier >= 3;
        int roll = level.random.nextInt(100);
        return switch (dungeon) {
            case EPOCH -> boss ? ModEntities.DEMON_GENERAL.get()
                    : captain ? ModEntities.PUPPET_CAPTAIN.get()
                    : elite ? (roll < 50 ? ModEntities.DEMON_STEWARD.get()
                            : ModEntities.CRIMSON_FLAME_WOLF.get())
                    : (roll < 40 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra
                            .SCARECROW_PUPPET.get()
                            : roll < 70 ? ModEntities.SPIRIT_WOLF.get()
                            : ModEntities.BLOOD_DEMON_CULTIVATOR.get());
            case BEAST -> boss ? ModEntities.DEMON_GENERAL.get()
                    : captain ? ModEntities.STORM_EAGLE.get()
                    : elite ? (roll < 30 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra2
                            .BONE_EAGLE.get()
                            : roll < 65 ? ModEntities.CRIMSON_FLAME_WOLF.get()
                            : ModEntities.FROST_PYTHON.get())
                    : (roll < 30 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra2
                            .BLOOD_LEECH.get()
                            : roll < 70 ? ModEntities.SPIRIT_WOLF.get()
                            : ModEntities.CRIMSON_FLAME_WOLF.get());
            case DEMON -> boss ? ModEntities.DEMON_GENERAL.get()
                    : captain ? ModEntities.BLOOD_DEMON_ELDER.get()
                    : elite ? (roll < 60 ? ModEntities.DEMON_STEWARD.get()
                            : ModEntities.SWORD_TOMB_GUARDIAN.get())
                    : (roll < 30 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra2
                            .DEMON_SPIDER.get()
                            : roll < 55 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra2
                            .CHARRED_SKELETON.get()
                            : ModEntities.BLOOD_DEMON_CULTIVATOR.get());
            case VOID -> boss ? ModEntities.VOID_OVERLORD.get()
                    : captain ? ModEntities.VOID_WALKER.get()
                    : elite ? (roll < 30 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra2
                            .STAR_DUST_WRAITH.get()
                            : roll < 55 ? ModEntities.VOID_WALKER.get()
                            : ModEntities.SWORD_TOMB_GUARDIAN.get())
                    : (roll < 35 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra
                            .VOID_JELLY.get()
                            : roll < 65 ? ModEntities.DEMON_GENERAL.get()
                            : ModEntities.BLOOD_DEMON_ELDER.get());
        };
    }

    /**
     * boss 槽（最后一波的头目/首领）在 {@link #siegeTierBonus(int)} 之上的额外生命倍率。
     *
     * <p>2026-11-07 从 12 下调到 6：旧值叠加品阶倍率（头目 ×20 / 首领 ×64）后，
     * 头目生命是普通怪的 300 倍以上；加上当时按玩家境界缩放，会变成"元婴头目 1.5 亿血"。</p>
     */
    public static final double BOSS_SLOT_HP_MUL = 6.0D;

    /** 品阶 → 额外的属性倍率（在副本倍率之上再乘）。 */
    private static double siegeTierBonus(int tier) {
        return switch (tier) {
            case 1 -> 1.6D;      // 精英
            case 2 -> 3.0D;      // 队长
            case 3 -> 6.0D;      // 头目
            default -> 1.0D;     // 小兵
        };
    }

    private static EntityType<? extends Mob> pickType(ServerLevel level, Dungeon dungeon,
                                                      boolean leader) {
        int roll = level.random.nextInt(100);
        return switch (dungeon) {
            // 纪元秘境：练气就能进，狼群 + 零星魔修，首领是傀儡队长
            case EPOCH -> leader ? ModEntities.PUPPET_CAPTAIN.get()
                    : (roll < 40 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra
                    .SCARECROW_PUPPET.get()
                    : roll < 75 ? ModEntities.SPIRIT_WOLF.get()
                    : ModEntities.BLOOD_DEMON_CULTIVATOR.get());
            // 妖兽秘境：以灵狼为主的兽潮
            case BEAST -> leader ? ModEntities.PUPPET_CAPTAIN.get()
                    : (roll < 82 ? ModEntities.SPIRIT_WOLF.get()
                    : ModEntities.BLOOD_DEMON_CULTIVATOR.get());
            // 魔道巢穴：魔修 + 傀儡，首领是魔道统领
            case DEMON -> leader ? ModEntities.DEMON_GENERAL.get()
                    : (roll < 55 ? ModEntities.BLOOD_DEMON_CULTIVATOR.get()
                    : ModEntities.PUPPET_CAPTAIN.get());
            // 虚空裂隙：头目成群，首领是虚空道主
            case VOID -> leader ? ModEntities.VOID_OVERLORD.get()
                    : (roll < 40 ? com.hongmeng.xiuxian.entity.ModEntitiesExtra
                    .VOID_JELLY.get()
                    : roll < 70 ? ModEntities.DEMON_GENERAL.get()
                    : ModEntities.VOID_OVERLORD.get());
        };
    }

    private static Vec3 ringPosition(ServerPlayer player, double radius, int index, int total) {
        double angle = (Math.PI * 2 / Math.max(1, total)) * index;
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
        ServerLevel level = (ServerLevel) player.level();
        // 有战斗场地就落到场地的地面上：柱子顶、看台、擂台上都不算数，
        // 否则怪会刷在石柱顶上 / 看台上，玩家只能干瞪眼。
        Dungeon dungeon = dungeonAt(level);
        if (dungeon != null && DungeonArena.onPlaza(x, z)) {
            BlockPos spot = DungeonArena.spawnSpot(level, dungeon, x, z);
            if (spot != null) {
                return new Vec3(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
            }
        }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new Vec3(x + 0.5D, top, z + 0.5D);
    }

    // ------------------------------------------------------------------
    // 通关与结束
    // ------------------------------------------------------------------

    private static void complete(ServerPlayer player, CultivationData data, Challenge ch) {
        Dungeon dungeon = ch.dungeon();
        cleanupChallenge(player);

        long now = player.level().getGameTime();
        String cdKey = clearKey(player, dungeon);
        Long last = LAST_CLEAR.get(cdKey);
        if (last != null && now - last < CLEAR_COOLDOWN) {
            long remain = (CLEAR_COOLDOWN - (now - last)) / 20L;
            player.sendSystemMessage(Component.literal(
                    "\u00a77秘境余韵未散，还需 " + remain + " 秒才能再次结算。"));
            return;
        }
        LAST_CLEAR.put(cdKey, now);

        long reward = dungeon.rewardFor(ch.enterTier());
        long granted = CultivationSource.DUNGEON.grant(player, reward, false);
        data.addDungeonClear();

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    120, 1.0D, 1.0D, 1.0D, 0.4D);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a7b\u00a7l═══ " + dungeon.cn() + " · 通 关 ═══").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal(
                "\u00a7f" + dungeon.waves() + " 波敌潮尽数伏诛。你站在秘境中央，喘着粗气。"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f本次收获：\u00a7a修为 +" + granted
                        + "\u00a77（应得 " + reward + "）"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f累计通关：\u00a7e" + data.getDungeonClears() + " \u00a7f次"));

        give(player, new ItemStack(ModItems.JIDAN.get(), 1), "纪元令 ×1");
        rewardExtras(player, dungeon);
        player.sendSystemMessage(Component.literal(""));

        ModNetworkBridge.sync(player);
    }

    /** 各副本的额外掉落。 */
    private static void rewardExtras(ServerPlayer player, Dungeon dungeon) {
        switch (dungeon) {
            case EPOCH -> {
                if (player.getRandom().nextInt(100) < 35) {
                    give(player, new ItemStack(ModItems.PILL_POJING.get(), 1), "破境丹 ×1");
                }
            }
            case BEAST -> {
                if (player.getRandom().nextInt(100) < 40) {
                    give(player, new ItemStack(ModItems.PILL_POJING.get(), 1), "破境丹 ×1");
                }
                if (player.getRandom().nextInt(100) < 30) {
                    give(player, new ItemStack(ModItems.SPIRIT_CRYSTAL.get(), 4), "灵晶 ×4");
                }
            }
            case DEMON -> {
                if (player.getRandom().nextInt(100) < 55) {
                    give(player, new ItemStack(ModItems.PILL_POJING.get(), 2), "破境丹 ×2");
                }
                if (player.getRandom().nextInt(100) < 35) {
                    give(player, new ItemStack(ModItems.DAOYIN.get(), 1), "道印 ×1");
                }
                if (player.getRandom().nextInt(100) < 30) {
                    give(player, new ItemStack(ModItems.XUELUO_ESSENCE.get(), 2), "血珞精 ×2");
                }
            }
            case VOID -> {
                if (player.getRandom().nextInt(100) < 70) {
                    give(player, new ItemStack(ModItems.PILL_POJING.get(), 3), "破境丹 ×3");
                }
                if (player.getRandom().nextInt(100) < 55) {
                    give(player, new ItemStack(ModItems.DAOYIN.get(), 2), "道印 ×2");
                }
                if (player.getRandom().nextInt(100) < 45) {
                    give(player, new ItemStack(ModItems.KONGMING_ESSENCE.get(), 3), "空冥精 ×3");
                }
                if (player.getRandom().nextInt(100) < 20) {
                    give(player, new ItemStack(ModItems.PILL_SHUANGDAO.get(), 1), "双道宫丹 ×1");
                }
            }
        }
    }

    private static void give(ServerPlayer player, ItemStack stack, String label) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendSystemMessage(Component.literal("\u00a7a获得：" + label));
    }

    /** 决战竞技场的首领被打死了：走正常通关流程。 */
    public static void onArenaBossKilled(ServerPlayer player) {
        try {
            Challenge ch = CHALLENGES.get(player.getUUID());
            if (ch == null) {
                return;
            }
            com.hongmeng.xiuxian.core.CultivationData data =
                    com.hongmeng.xiuxian.core.CultivationCapability.get(player);
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[arena] {} 决战首领被击杀：结算并进入 30 秒战利品倒计时（玩家 {}）",
                    ch.dungeon().dimensionId(), player.getName().getString());
            complete(player, data, ch);
            // 击杀即时战利品：竞技场宝箱是“探索用”的预置箱；这里直接滚一次档位表进背包。
            BossRewards.grantKillLoot(player, ch.dungeon());
            // 不立刻送回：给玩家 30 秒开场地宝箱（宝库/神龛），然后自动离开首领位面、
            // 回到**副本维度**（禁足随之解除，可自由探索；潜行+空手右键离开副本）。
            ArenaReturn.forget(player);          // 防止 tick 兜底立刻送走
            startExitCountdown(player, ch.dungeon());
            // 通关**虚空裂隙**是解锁「真实世界」的条件（玩家指定的触发点）。
            // 注意：解锁后每次通关都会再问一次 —— 玩家说过
            //「如果没有选择开启，下次打完虚空裂隙也可以选择」。
            if (ch.dungeon() == com.hongmeng.xiuxian.dungeon.Dungeon.VOID) {
                com.hongmeng.xiuxian.realworld.RealWorld.onVoidCleared(player);
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[arena] 决战结算失败：{}", t.toString());
        }
    }

    /**
     * 首领战结束后启动 30 秒“战利品倒计时”。
     *
     * <p>玩家要求：「打完 boss 后 30 秒倒计时自动退出 boss 副本，回到维度可以自由探索」。
     * 倒计时期间保持人在首领位面（可以先开竞技场宝库的箱子），结束后
     * {@link #finishExitCountdown} 把人送回副本维度落点，并解除禁足。</p>
     */
    private static void startExitCountdown(ServerPlayer player, Dungeon dungeon) {
        long now = player.level().getGameTime();
        EXIT_COUNTDOWN.put(player.getUUID(),
                new ExitCountdown(dungeon, now + EXIT_COUNTDOWN_TICKS));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00a76\u00a7l✦ 首领已伏诛 ✦"));
        player.sendSystemMessage(Component.literal(
                "\u00a77你可以先开启场地宝箱；\u00a7e30 秒\u00a77后自动离开首领位面，"
                        + "回到\u00a7b" + dungeon.cn() + "\u00a77自由探索。"));
        player.sendSystemMessage(Component.literal(
                "\u00a78（潜行 + 空手右键可提前离开；期间不会禁足）"));
        player.displayClientMessage(Component.literal(
                "\u00a76首领已伏诛 \u00a7e30\u00a77 秒后自动离开首领位面"), true);
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 战利品倒计时开始：{} 秒（玩家 {}）",
                dungeon.dimensionId(), EXIT_COUNTDOWN_TICKS / 20L,
                player.getName().getString());
    }

    /**
     * 倒计时结束：送回副本维度落点，挑战已经完结（不再禁足，可自由探索）。
     *
     * <p>落点优先用进门时记录的 {@link #REALM_LANDING}；区块/落点失效时重新
     * {@link #findLanding} 兜底。回到副本维度后，玩家用“潜行 + 空手右键”即可离开副本。</p>
     */
    private static void finishExitCountdown(ServerPlayer player, ExitCountdown exit) {
        EXIT_COUNTDOWN.remove(player.getUUID());
        Dungeon dungeon = exit.dungeon();
        MinecraftServer server = player.getServer();
        ServerLevel realm = level(server, dungeon);
        boolean ok = false;
        if (realm != null) {
            try {
                forceLoadArea(realm, new BlockPos(ARENA_X, 0, ARENA_Z));
                BlockPos landing = REALM_LANDING.get(player.getUUID());
                if (landing == null || !realm.isLoaded(landing) || !standable(realm, landing)) {
                    landing = findLanding(realm, ARENA_X, ARENA_Z);
                }
                player.teleportTo(realm, landing.getX() + 0.5D, landing.getY(),
                        landing.getZ() + 0.5D, 180.0F, 0.0F);
                player.setPortalCooldown();
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal(
                        "\u00a7a你离开了首领位面，回到 \u00a7b" + dungeon.cn()
                                + "\u00a7a。现在可以自由探索；"
                                + "\u00a77（潜行 + 空手右键离开副本）"));
                ok = true;
            } catch (Throwable t) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                        "[arena] 倒计时结束回副本维度失败，改走兜底：{}", t.toString());
            }
        }
        if (!ok) {
            escapeArena(player, "\u00a77你被送回了主世界。");
        }
        ArenaBattle.leave(player);
        ArenaRealm.forget(player);
        ArenaReturn.forget(player);
        cleanupChallenge(player);
        clearBossBar(player);
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 战利品倒计时结束：{} 已回到副本维度（自由探索）",
                dungeon.dimensionId(), player.getName().getString());
    }

    /** 通关冷却键（玩家 + 副本）。 */
    private static String clearKey(ServerPlayer player, Dungeon dungeon) {
        return player.getUUID() + "#" + dungeon.name();
    }

    private static void cleanupChallenge(ServerPlayer player) {
        Challenge ch = CHALLENGES.remove(player.getUUID());
        if (ch != null && player.level() instanceof ServerLevel level) {
            for (UUID id : ch.mobs()) {
                Entity entity = level.getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
        }
        if (ch != null && ch.arenaSent()) {
            // 决战阶段作废/结算时，清掉该 boss 维度里残留的祭坛首领，
            // 免得下一场进来先看到上一场的“幽灵 Boss”。
            ServerLevel arena = player.getServer() == null ? null
                    : ArenaRealm.level(player.getServer(), ch.dungeon());
            discardArenaBosses(arena);
        }
        clearBossBar(player);
    }

    /**
     * 主动撤离决战维度：送回来源世界 + 作废挑战 + 清掉本场残留的祭坛首领。
     *
     * <p>给“潜行 + 空手右键”用。以前这段逻辑写死在 {@code BossSummonBlock} 的
     * 召唤函数里，祭坛点亮后根本走不到（先提示“已经用过了”就 return），
     * 玩家会被困在首领位面 —— 现在由事件层统一处理（审计 A-2）。</p>
     */
    public static void abandonArena(ServerPlayer player, String reason) {
        if (player == null) {
            return;
        }
        // 首领战后的倒计时期间“提前离开”：直接回副本维度（保留“自由探索”语义）
        ExitCountdown exit = EXIT_COUNTDOWN.get(player.getUUID());
        if (exit != null) {
            finishExitCountdown(player, exit);
            return;
        }
        Challenge ch = CHALLENGES.get(player.getUUID());
        ServerLevel arenaLevel = player.level() instanceof ServerLevel l ? l : null;
        boolean back = ArenaReturn.sendBack(player, reason);
        if (!back) {
            escapeArena(player, reason);
        }
        if (ch != null) {
            cleanupChallenge(player);
        }
        ArenaRealm.forget(player);
        ArenaReturn.forget(player);
        discardArenaBosses(arenaLevel);
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 主动撤离首领位面（有来源记录={}）",
                player.getName().getString(), back);
    }

    /** 清掉某个 boss 维度里残留的祭坛首领（异常退出 / 非玩家击杀的兜底）。 */
    private static void discardArenaBosses(ServerLevel level) {
        if (level == null) {
            return;
        }
        try {
            var area = new net.minecraft.world.phys.AABB(-256.0D,
                    level.getMinBuildHeight(), -256.0D,
                    256.0D, level.getMaxBuildHeight(), 256.0D);
            int removed = 0;
            for (Entity e : level.getEntities((Entity) null, area,
                    en -> en.getPersistentData().getBoolean("hm_arena_boss"))) {
                e.discard();
                removed++;
            }
            if (removed > 0) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                        "[arena] 清掉 {} 只残留的祭坛首领", removed);
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[arena] 清理残留祭坛首领失败：{}", t.toString());
        }
    }

    /** 玩家死亡或放弃时清场（连 boss 维度的进出记录一起清，避免串味）。 */
    public static void abort(ServerPlayer player) {
        if (player != null) {
            EXIT_COUNTDOWN.remove(player.getUUID());
            ENTRY_GUARD.remove(player.getUUID());
            BAN_GRACE.remove(player.getUUID());
        }
        cleanupChallenge(player);
        ArenaReturn.forget(player);
        ArenaRealm.forget(player);
    }

    /** 下线时把玩家的临时记录清干净。 */
    public static void forget(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUUID();
        cleanupChallenge(player);
        RETURN_POINTS.remove(id);
        PENDING_ENTRY.remove(id);
        LAST_CLEAR.keySet().removeIf(k -> k.startsWith(id + "#"));
        ArenaReturn.forget(player);
        ArenaRealm.forget(player);
        EXIT_COUNTDOWN.remove(id);
        REALM_LANDING.remove(id);
        ENTRY_GUARD.remove(id);
        BAN_GRACE.remove(id);
        BAN_NOTICE.remove(id);
        com.hongmeng.xiuxian.block.DungeonPortalBlock.forget(id);
    }

    // ------------------------------------------------------------------
    // Boss 条
    // ------------------------------------------------------------------

    private static void showBossBar(ServerPlayer player, Dungeon dungeon, int wave, int count) {
        ServerBossEvent bar = BARS.get(player.getUUID());
        if (bar == null) {
            bar = new ServerBossEvent(barTitle(dungeon, wave, count),
                    barColor(dungeon), BossEvent.BossBarOverlay.NOTCHED_10);
            BARS.put(player.getUUID(), bar);
            bar.addPlayer(player);
        }
        bar.setName(barTitle(dungeon, wave, count));
        bar.setProgress(Math.min(1.0F, count / 10.0F));
        bar.setVisible(true);
    }

    private static void updateBossBar(ServerPlayer player, Dungeon dungeon, int wave, int count) {
        ServerBossEvent bar = BARS.get(player.getUUID());
        if (bar == null) {
            return;
        }
        bar.setName(barTitle(dungeon, wave, count));
        bar.setProgress(Math.min(1.0F, count / 10.0F));
    }

    private static Component barTitle(Dungeon dungeon, int wave, int count) {
        return Component.literal("\u00a7" + dungeon.colorCode() + dungeon.cn()
                + " \u00a7f第 " + wave + " 波 \u00a77剩余 " + count);
    }

    private static BossEvent.BossBarColor barColor(Dungeon dungeon) {
        return switch (dungeon) {
            case EPOCH -> BossEvent.BossBarColor.BLUE;
            case BEAST -> BossEvent.BossBarColor.GREEN;
            case DEMON -> BossEvent.BossBarColor.RED;
            case VOID -> BossEvent.BossBarColor.PURPLE;
        };
    }

    private static void clearBossBar(ServerPlayer player) {
        ServerBossEvent bar = BARS.remove(player.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
            bar.setVisible(false);
        }
    }

    // ------------------------------------------------------------------
    // 查询（给命令 / 调试用）
    // ------------------------------------------------------------------

    /** 是否正在挑战中。 */
    public static boolean inChallenge(ServerPlayer player) {
        return player != null && CHALLENGES.containsKey(player.getUUID());
    }

    /** 当前挑战的副本（不在挑战里返回 {@code null}）。 */
    public static Dungeon activeDungeon(ServerPlayer player) {
        Challenge ch = player == null ? null : CHALLENGES.get(player.getUUID());
        return ch == null ? null : ch.dungeon();
    }

    /** 当前波次（从 1 开始；不在挑战里返回 0）。 */
    public static int currentWave(ServerPlayer player) {
        Challenge ch = player == null ? null : CHALLENGES.get(player.getUUID());
        return ch == null ? 0 : ch.wave() + 1;
    }

    /** 还剩几只怪。 */
    public static int remainingMobs(ServerPlayer player) {
        Challenge ch = player == null ? null : CHALLENGES.get(player.getUUID());
        return ch == null ? 0 : ch.mobs().size();
    }

    /**
     * 调试用：直接清掉当前这一波。
     *
     * <p>签名与 {@code HongmengCommand} 里 {@code /hongmeng dungeon skipwave} 的调用一致；
     * {@code dungeon} 只用来确认玩家确实在这座副本里。</p>
     */
    public static int debugKillWave(ServerPlayer player, Dungeon dungeon) {
        Challenge ch = player == null ? null : CHALLENGES.get(player.getUUID());
        if (ch == null || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        if (dungeon != null && ch.dungeon() != dungeon) {
            return 0;
        }
        int killed = 0;
        for (UUID id : new HashSet<>(ch.mobs())) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
                killed++;
            }
        }
        ch.mobs().clear();
        return killed;
    }

    /**
     * 登录兜底：玩家上次在 boss 维度离线、且挑战已经不在了 → 送回主世界出生点。
     *
     * <p>为什么不在 tick 里直接送：那样会让“用命令/创造模式进去看场地”的玩家
     * 也被反复弹走。只有登录这种“确实可能永远困住”的时机才强送。</p>
     */
    public static void escapeStrandedArena(ServerPlayer player) {
        if (player == null || !ArenaRealm.isInArena(player)) {
            return;
        }
        if (CHALLENGES.containsKey(player.getUUID()) || ArenaReturn.has(player.getUUID())) {
            return;
        }
        escapeArena(player, "\u00a77你上次在首领位面离线，已把你送回主世界。");
    }

    /** 离开 boss 维度后清掉过期的来源记录（不影响进行中的挑战）。 */
    public static void clearStaleArenaRecords(ServerPlayer player) {
        if (player != null && !CHALLENGES.containsKey(player.getUUID())) {
            ArenaReturn.forget(player);
            ArenaRealm.forget(player);
        }
    }

    /** 玩家是不是正待在本副本对应的“boss 决战维度”里。 */
    private static boolean inArenaOf(ServerPlayer player, Dungeon dungeon) {
        return player != null && ArenaRealm.dungeonOf(player) == dungeon;
    }

    /**
     * 兜底撤离：没有来源记录（比如命令直接传送进 boss 维度）时，
     * 把人送回主世界的世界出生点，避免永远卡在 arena 维度里。
     */
    private static void escapeArena(ServerPlayer player, String reason) {
        MinecraftServer server = player.getServer();
        ServerLevel overworld = server == null ? null : server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        BlockPos pos = safeReturn(overworld, overworld.getSharedSpawnPos());
        player.teleportTo(overworld, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        player.setPortalCooldown();
        if (reason != null) {
            player.displayClientMessage(Component.literal(reason), false);
        }
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 兜底送回主世界 {}（原因：{}）",
                player.getName().getString(), pos, reason);
    }

    /** 断线/换维度时按「挑战阶段 + 当前维度」决定是否把挑战收掉。 */
    public static void abortIfOutside(ServerPlayer player) {
        Challenge ch = player == null ? null : CHALLENGES.get(player.getUUID());
        if (ch == null) {
            // 倒计时期间人离开了首领位面（命令/传送）：清掉倒计时，不重复传送
            ExitCountdown exit = player == null ? null : EXIT_COUNTDOWN.get(player.getUUID());
            if (exit != null && !inArenaOf(player, exit.dungeon())) {
                EXIT_COUNTDOWN.remove(player.getUUID());
            }
            return;
        }
        boolean inRealm = isIn(player, ch.dungeon());
        boolean inArena = inArenaOf(player, ch.dungeon());
        // 转场瞬间：ArenaRealm.enter 的 teleportTo 会**先**触发换维度事件，
        // 而挑战里的 arenaSent 要等本 tick 后面才写进去。此时不能作废、更不能清
        // ArenaReturn 的来源记录 —— 否则刚记下的“从哪来”被清掉，打完首领回不去。
        if (inArena && !ch.arenaSent()) {
            return;
        }
        if (!ArenaStage.shouldAbort(inRealm, inArena, ch.arenaSent())) {
            // 决战阶段人还在 arena 维度：这是合法的第二种位置，保留挑战。
            return;
        }
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[arena] {} 挑战作废：inRealm={} inArena={} arenaSent={}（当前 {}）",
                ch.dungeon().dimensionId(), inRealm, inArena, ch.arenaSent(),
                player.level().dimension().location());
        cleanupChallenge(player);
        ArenaReturn.forget(player);
        ArenaRealm.forget(player);
    }

    /** 供离线校验/测试用：清空全部状态。 */
    static void clearAll() {
        CHALLENGES.clear();
        RETURN_POINTS.clear();
        LAST_CLEAR.clear();
        BARS.clear();
        EXIT_COUNTDOWN.clear();
        REALM_LANDING.clear();
        ENTRY_GUARD.clear();
        BAN_GRACE.clear();
    }
}
