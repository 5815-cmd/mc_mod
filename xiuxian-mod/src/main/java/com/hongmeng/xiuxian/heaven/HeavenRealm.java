package com.hongmeng.xiuxian.heaven;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.combat.BossCinematic;
import com.hongmeng.xiuxian.combat.BossDescent;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * <b>第五维度「天外天」的运行逻辑</b>（13 号设计稿 §1.3，唯一契约）。
 *
 * <h2>这个类管什么</h2>
 * <ol>
 *   <li><b>登天 / 回归</b>：{@code HeavenTokenItem} 右键调过来，这里做境界判定、
 *       记录回归点、传送、冷却；</li>
 *   <li><b>幂等登天台</b>：落点是<b>代码现铺</b>的 15×15 玉台（四角灯 + 中央回返符），
 *       同一坐标建过一次就只读标记，不重复施工（写法参考 {@code BossLair}）；</li>
 *   <li><b>环境刷怪</b>：每 {@value #ENV_SPAWN_INTERVAL}t 对每位玩家按 §1.3 的池子
 *       与权重刷 1~2 只，怪够多就不刷；</li>
 *   <li><b>仙庭之主</b>：玩家进天外天且维度内没有 {@code heaven_lord} 时，
 *       在登天台外建 31×31「凌霄斗台」（幂等），把 Boss 刷在中央、
 *       {@link BossDescent} 缓降出场、{@link BossCinematic} + 全维度播报；
 *       Boss 死亡后记 SavedData 标记，不再自动刷（{@code /summon} 手动仍可再战）。</li>
 * </ol>
 *
 * <h2>事件挂接</h2>
 * <p>本类自带 {@link Mod.EventBusSubscriber}（默认就是 Forge 总线），
 * <b>不需要改 {@code ModEvents}/{@code ClientSetup}</b>——照
 * {@code DungeonRealmEvents} 的做法，只驱动自己这条线。</p>
 *
 * <h2>坐标契约</h2>
 * <pre>
 *   登天台：中心 (0, *, 0)，15×15，楼板 y = 地表首空 y + 1（台面 = 地表 + 2）
 *   凌霄斗台：以登天台为中心、正东/西/南/北某一个方向，中心距 = 24 + 高差
 *             （高差从 4 个方向里挑"地形最低"的那个），31×31；
 *             两座台之间铺 3 格宽玉桥 + 逐级台阶。
 * </pre>
 *
 * <p><b>状态只落一份</b>：{@link HeavenFlag}（{@code hongmeng_heaven} 维度级
 * SavedData）记台面高度、斗台是否已建、Boss 是否召唤过 / 已击杀。
 * 玩家个人数据（回归点）存在 {@code player.getPersistentData()}。</p>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID)
public final class HeavenRealm {

    // ------------------------------------------------------------------
    // 常量（改这里等于改玩法，慎重）
    // ------------------------------------------------------------------

    /** 维度 id（数据文件：{@code data/hongmeng/dimension/heaven_realm.json}）。 */
    public static final String DIMENSION_ID = "heaven_realm";

    /** 天外天维度 key。 */
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(HongmengMod.MOD_ID, DIMENSION_ID));

    /** 登天门槛：大境 8 = 仙人（{@code Realm} 的 tier 从 1 练气 起算）。 */
    public static final int MIN_REALM_TIER = 8;

    /** 回归冷却（t）= 3 秒，防误触。 */
    public static final int RETURN_COOLDOWN = 60;

    /** 台面半边长：15×15（{@code HALF=7}）。 */
    private static final int PLATFORM_HALF = 7;

    /** 斗台半边长：31×31（{@code HALF=15}）。 */
    private static final int ARENA_HALF = 15;

    /** 斗台中心离登天台中心的基准距离（两座台之间留 1 格空档 + 玉桥）。 */
    private static final int ARENA_BASE_DISTANCE = 24;

    /** 环境刷怪节拍（t）。 */
    private static final int ENV_SPAWN_INTERVAL = 200;

    /** 维度内（以玩家为圆心 96 格内算）环境怪上限，到了就不刷。 */
    private static final int ENV_MOB_CAP = 8;

    /** 安全落点距离区间（格）。 */
    private static final int ENV_MIN_DISTANCE = 20;
    private static final int ENV_MAX_DISTANCE = 40;

    /** SavedData 键。 */
    private static final String DATA_KEY = "hongmeng_heaven";

    /** 玩家 persistentData 里的回归点字段。 */
    private static final String PD_BACK_DIM = "hongmeng_heaven_back_dim";
    private static final String PD_BACK_X = "hongmeng_heaven_back_x";
    private static final String PD_BACK_Y = "hongmeng_heaven_back_y";
    private static final String PD_BACK_Z = "hongmeng_heaven_back_z";
    private static final String PD_BACK_YAW = "hongmeng_heaven_back_yaw";
    private static final String PD_BACK_PITCH = "hongmeng_heaven_back_pitch";

    /**
     * 环境怪池（权重合计 100）。
     *
     * <p>§1.3 的原表是 30/25/20/15/10 五只妖兽；本批合并时按 §1.4
     * 「天兵=环境怪主力、云鹤=空中威胁」补了两只天外天专属怪，
     * 权重调成 30/20/20/15/10/5（合计仍是 100）。
     * 要严格回退 §1.3 原表，只改这下面两行即可（顺序与长度保持一一对应）。</p>
     */
    private static final String[] ENV_POOL = {
            // 天外天自己的两只（天兵=环境怪主力 / 云鹤=空中威胁），再加四只高境妖兽
            "heaven_soldier", "cloud_crane", "sword_tomb_guardian",
            "blood_demon_elder", "star_dust_wraith", "bone_eagle"
    };
    private static final int[] ENV_WEIGHTS = {30, 20, 20, 15, 10, 5};

    /** 仙庭之主：只按注册名引用（实体类由别的 agent 写，本类不依赖它的类）。 */
    private static final ResourceLocation HEAVEN_LORD_ID =
            new ResourceLocation(HongmengMod.MOD_ID, "heaven_lord");

    /** 斗台候选方向（只挑正东南西北：连桥是直的，好走）。 */
    private static final Direction[] CARDINALS = {
            Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    private HeavenRealm() {
    }

    // ------------------------------------------------------------------
    // 对外小工具
    // ------------------------------------------------------------------

    /** 这个 level 是不是天外天。 */
    public static boolean isIn(Level level) {
        return level != null && DIMENSION.equals(level.dimension());
    }

    /** 取天外天维度（未加载返回 null）。 */
    public static ServerLevel serverLevel(MinecraftServer server) {
        return server == null ? null : server.getLevel(DIMENSION);
    }

    /** 维度里活着几只仙庭之主（给调试 / 汇报用）。 */
    public static boolean hasLivingLord(ServerLevel level) {
        return findLord(level) != null;
    }

    // ==================================================================
    // 1. 登天令入口：进 / 回
    // ==================================================================

    /**
     * 登天令右键的统一入口（{@code HeavenTokenItem#use} 调）。
     *
     * <p>在天外天里 → 回归；否则 → 登天。冷却中直接打回，两个方向共用一个冷却。</p>
     */
    public static void useToken(ServerPlayer player, ItemStack stack) {
        if (player == null) {
            return;
        }
        Item token = stack == null ? null : stack.getItem();
        if (token != null && player.getCooldowns().isOnCooldown(token)) {
            player.displayClientMessage(Component.literal("§7天符还在回气，稍候再试。"), true);
            return;
        }
        if (isIn(player.level())) {
            tryReturn(player, token);
            return;
        }
        tryEnter(player);
    }

    // ------------------------------------------------------------------
    // 登天
    // ------------------------------------------------------------------

    /** 登天：境界 ≥ 仙人才能进；落点是幂等登天台。 */
    private static void tryEnter(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        int tier = data.realm().tier();
        if (tier < MIN_REALM_TIER) {
            player.sendSystemMessage(Component.literal(
                    "§c境界不足 · 需 §e仙人§c（大境 8）。你目前是 §f" + data.realm().fullCn()
                            + "§c。§8（登天令只认仙人）"));
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
            return;
        }
        MinecraftServer server = player.getServer();
        ServerLevel target = serverLevel(server);
        if (target == null) {
            player.sendSystemMessage(Component.literal(
                    "§c天外天尚未开启（维度未加载）。§8（检查 data/hongmeng/dimension/heaven_realm.json）"));
            return;
        }

        // ① 记录回归点（进天之前站的位置）——每次登天都刷新
        saveReturnPoint(player);

        // ② 幂等登天台（先铺台，再传送；否则玩家会掉进噪声地形）
        BlockPos landing = ensurePlatform(target);
        // 传送前把落点区块立起来（重启后第一次登天时台子所在区块可能没加载）
        forceLoad(target, landing, 1);

        // ③ 传送（Forge 的跨维度 teleportTo，和 DungeonRealm 同一套写法）
        player.teleportTo(target, landing.getX() + 0.5D, landing.getY(),
                landing.getZ() + 0.5D, player.getYRot(), player.getXRot());
        // 跟原版门一样给一段传送冷却：刚落地不会被别的门/传送门反复吸走
        player.setPortalCooldown();

        // ④ 落地反馈
        target.playSound(null, landing, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 0.75F);
        target.sendParticles(ParticleTypes.END_ROD, landing.getX() + 0.5D, landing.getY() + 1.2D,
                landing.getZ() + 0.5D, 60, 1.4D, 1.6D, 1.4D, 0.02D);
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§b§l═══ 天外天 ═══"));
        player.sendSystemMessage(Component.literal("§7云海之上，星阶尽头，是仙庭的所在。"));
        player.sendSystemMessage(Component.literal("§f你的境界：§e" + data.realm().fullCn()
                + "   §f落点：§b登天台 §8(0, " + landing.getY() + ", 0)"));
        player.sendSystemMessage(Component.literal(
                "§7环境怪会循着仙气聚过来；斗台在台外，仙庭之主在等你。"));
        player.sendSystemMessage(Component.literal(
                "§7再右键一次登天令，可以回到人间（冷却 3 秒）。"));

        // ⑤ 首入：召唤仙庭之主（内部幂等 + 已击杀就不再刷）
        onEntered(target, player);
    }

    /** 把"进天之前的位置"写进玩家 persistentData（跨维度、跨下线都还在）。 */
    private static void saveReturnPoint(ServerPlayer player) {
        try {
            CompoundTag pd = player.getPersistentData();
            Level from = player.level();
            pd.putString(PD_BACK_DIM, from.dimension().location().toString());
            pd.putDouble(PD_BACK_X, player.getX());
            pd.putDouble(PD_BACK_Y, player.getY());
            pd.putDouble(PD_BACK_Z, player.getZ());
            pd.putFloat(PD_BACK_YAW, player.getYRot());
            pd.putFloat(PD_BACK_PITCH, player.getXRot());
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven] 回归点记录失败：{}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 回归
    // ------------------------------------------------------------------

    /** 回归：回到进天前的记录点；没有记录就回主世界出生点。 */
    private static void tryReturn(ServerPlayer player, Item token) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CompoundTag pd = player.getPersistentData();
        ServerLevel target = null;
        // 先给默认值：只有"记录点可用"或"回主世界出生点"两条路会给它们赋值，
        // 显式初始化是为了让编译器/读者都一眼看出这里不会读到未初始化值。
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        boolean recorded = false;
        if (pd.contains(PD_BACK_DIM)) {
            ResourceLocation dimId = ResourceLocation.tryParse(pd.getString(PD_BACK_DIM));
            if (dimId != null) {
                ServerLevel candidate = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
                if (candidate != null) {
                    target = candidate;
                    x = pd.getDouble(PD_BACK_X);
                    y = pd.getDouble(PD_BACK_Y);
                    z = pd.getDouble(PD_BACK_Z);
                    yaw = pd.getFloat(PD_BACK_YAW);
                    pitch = pd.getFloat(PD_BACK_PITCH);
                    recorded = true;
                }
            }
        }
        if (target == null) {
            target = server.getLevel(Level.OVERWORLD);
            if (target == null) {
                player.sendSystemMessage(Component.literal("§c回归失败：主世界不可用。"));
                return;
            }
            BlockPos spawn = target.getSharedSpawnPos();
            x = spawn.getX() + 0.5D;
            y = spawn.getY();
            z = spawn.getZ() + 0.5D;
        }

        // 落点安全检查：记录点可能被填/被拆，往里塞会卡住
        BlockPos safe = safeSpot(target, BlockPos.containing(x, y, z));
        player.teleportTo(target, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, yaw, pitch);
        player.setPortalCooldown();
        if (token != null) {
            player.getCooldowns().addCooldown(token, RETURN_COOLDOWN);
        }
        target.playSound(null, safe, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.8F, 1.2F);
        target.sendParticles(ParticleTypes.END_ROD, safe.getX() + 0.5D, safe.getY() + 1.0D,
                safe.getZ() + 0.5D, 24, 0.8D, 1.0D, 0.8D, 0.02D);
        player.sendSystemMessage(Component.literal(recorded
                ? "§7天符一展，你落回了进天之前站的地方。"
                : "§7天符一展，你落回了人间（没有找到进天记录，回的主世界出生点）。"));
    }

    /** 找一个"脚踏有地、头顶有空间"的落脚点（记录点被改动时兜底）。 */
    private static BlockPos safeSpot(ServerLevel level, BlockPos pos) {
        if (isStandable(level, pos)) {
            return pos;
        }
        for (int dy = 1; dy <= 4; dy++) {
            if (isStandable(level, pos.above(dy))) {
                return pos.above(dy);
            }
            if (isStandable(level, pos.below(dy))) {
                return pos.below(dy);
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            if (isStandable(level, side)) {
                return side;
            }
            if (isStandable(level, side.above())) {
                return side.above();
            }
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        boolean feetFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        boolean headFree = level.getBlockState(pos.above())
                .getCollisionShape(level, pos.above()).isEmpty();
        boolean groundSolid = !level.getBlockState(pos.below())
                .getCollisionShape(level, pos.below()).isEmpty();
        return feetFree && headFree && groundSolid;
    }

    // ==================================================================
    // 2. 事件驱动（FORGE 总线，本类自己挂）
    // ==================================================================

    /**
     * 每 tick 的维度驱动：环境刷怪 + "兜底召唤 Boss"。
     *
     * <p>只在 {@link TickEvent.Phase#END} 处理，每 {@value #ENV_SPAWN_INTERVAL}t 一轮。
     * 用维度节拍而不是玩家节拍：天外天只有一座，入口逻辑集中。</p>
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level) || !isIn(level)) {
            return;
        }
        if (level.players().isEmpty() || level.getGameTime() % ENV_SPAWN_INTERVAL != 0) {
            return;
        }
        try {
            for (ServerPlayer player : List.copyOf(level.players())) {
                // 兜底：/tp、命令、重生等方式进来的玩家也能触发 Boss 召唤
                onEntered(level, player);
                tryEnvSpawn(level, player);
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven] 维度节拍处理失败：{}", t.toString());
        }
    }

    /** 换维度：原版传送门/命令把玩家送进天外天时也补一次"首入"逻辑。 */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!DIMENSION.equals(event.getTo())) {
            return;
        }
        if (player.level() instanceof ServerLevel level && isIn(level)) {
            try {
                onEntered(level, player);
            } catch (Throwable t) {
                HongmengMod.LOGGER.warn("[heaven] 首入处理失败：{}", t.toString());
            }
        }
    }

    /** Boss 死亡：记"已击杀"，之后不再自动刷（手动 /summon 不受影响）。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        try {
            Entity dead = event.getEntity();
            if (dead == null || !(dead.level() instanceof ServerLevel level) || !isIn(level)) {
                return;
            }
            EntityType<?> lord = lordType();
            if (lord == null || dead.getType() != lord) {
                return;
            }
            HeavenFlag flag = flag(level);
            if (flag.lordDefeated) {
                return;
            }
            flag.lordDefeated = true;
            flag.setDirty();
            // 首杀额外战利品（lordDefeated 只写一次 → 只发一次；五凭证仍由 Boss 本体掉落）
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                com.hongmeng.xiuxian.dungeon.BossRewards.grantHeavenLordLoot(killer);
            }
            broadcast(level, "§6§l仙庭之主 · 陨落",
                    "§e斗台中央的金血散去，天外天的天幕重归澄澈。");
            level.playSound(null, dead.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            HongmengMod.LOGGER.info("[heaven] 仙庭之主被击杀，已记录击杀标记（不再自动刷）");
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven] 击杀标记写入失败：{}", t.toString());
        }
    }

    // ==================================================================
    // 3. 首入：凌霄斗台 + 仙庭之主召唤
    // ==================================================================

    /**
     * 玩家进入天外天时调（幂等）。
     *
     * <p>顺序：确保登天台在 → 已击杀就收工 → 维度里已有 Boss 就收工 →
     * 建斗台 → 刷 Boss + 缓降出场 + 播报。</p>
     */
    public static void onEntered(ServerLevel level, ServerPlayer player) {
        if (!isIn(level)) {
            return;
        }
        try {
            HeavenFlag flag = flag(level);
            // 登天台兜底（正常走地道具会先建，这里防"命令直接传送"）
            ensurePlatform(level);
            if (flag.lordDefeated) {
                // 打过了：不自动刷；顺手清掉老存档里重复召唤残留的多余 Boss。
                discardExtraLords(level);
                return;
            }
            // ⚠ 已经召唤过就不再召唤（哪怕 findLord 没找到）。
            //   原因：没有玩家时天外天区块会被卸载，findLord 的实体查询看不见已存在的
            //   仙庭之主；旧代码因此在“离开再进来”时重复召唤，玩家实测一次刷出 3 只
            //   （客户端日志 2026-11-02 16:32:12 连刷 3 条“仙庭之主已在斗台中央召唤”）。
            if (flag.lordSummoned) {
                return;
            }
            // 老存档/卸载后的兜底：先把斗台周围区块拉起来再查实体，否则查不到已有 Boss。
            if (flag.arenaBuilt) {
                forceLoad(level, new BlockPos(flag.arenaCenterX, flag.arenaFloorY,
                        flag.arenaCenterZ), 2);
            }
            if (findLord(level) != null) {
                return;                     // 维度里已经有一只了（可能是手动 /summon）
            }
            ensureArena(level, flag);
            if (summonLord(level, player, flag)) {
                flag.lordSummoned = true;
                flag.setDirty();
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven] 仙庭之主召唤流程失败（不影响进入）：{}", t.toString());
        }
    }

    /** 从注册表按名字取仙庭之主的 EntityType（不依赖实体类，别的 agent 在写）。 */
    private static EntityType<?> lordType() {
        return ForgeRegistries.ENTITY_TYPES.getValue(HEAVEN_LORD_ID);
    }

    /** 维度里活着的仙庭之主（在登天台 512 格范围内找就够）。 */
    private static Entity findLord(ServerLevel level) {
        EntityType<?> lord = lordType();
        if (lord == null) {
            return null;
        }
        try {
            AABB area = new AABB(-512.0D, level.getMinBuildHeight(), -512.0D,
                    512.0D, level.getMaxBuildHeight(), 512.0D);
            List<Entity> found = level.getEntities((Entity) null, area,
                    e -> e.isAlive() && !e.isRemoved() && e.getType() == lord);
            return found.isEmpty() ? null : found.get(0);
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven] 查询仙庭之主失败：{}", t.toString());
            return null;
        }
    }

    /**
     * 清掉重复召唤残留的仙庭之主（历史 bug：一次刷出多只）。
     *
     * <p>只在“已击杀”分支调用：首领已死，残留的重复个体不该继续在斗台上游荡。</p>
     */
    private static void discardExtraLords(ServerLevel level) {
        EntityType<?> lord = lordType();
        if (lord == null) {
            return;
        }
        try {
            AABB area = new AABB(-512.0D, level.getMinBuildHeight(), -512.0D,
                    512.0D, level.getMaxBuildHeight(), 512.0D);
            int removed = 0;
            for (Entity e : level.getEntities((Entity) null, area,
                    en -> en.isAlive() && !en.isRemoved() && en.getType() == lord)) {
                e.discard();
                removed++;
            }
            if (removed > 0) {
                HongmengMod.LOGGER.info(
                        "[heaven] 已清理 {} 只残留的仙庭之主（历史重复召唤）", removed);
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven] 清理残留仙庭之主失败：{}", t.toString());
        }
    }

    /** 把 Boss 刷在斗台中央，并让它从天而降。 */
    private static boolean summonLord(ServerLevel level, ServerPlayer player, HeavenFlag flag) {
        EntityType<?> lord = lordType();
        if (lord == null) {
            HongmengMod.LOGGER.warn("[heaven] 找不到实体类型 {}，跳过召唤（注册表是否已合入？）",
                    HEAVEN_LORD_ID);
            return false;
        }
        if (!flag.arenaBuilt) {
            HongmengMod.LOGGER.warn("[heaven] 斗台未建好，跳过召唤");
            return false;
        }
        if (flag.lordSummoned) {
            // 防御：已经召唤过就绝不重复召唤（findLord 在区块卸载时会漏检）。
            return false;
        }
        Entity created = lord.create(level);
        if (!(created instanceof Mob boss)) {
            HongmengMod.LOGGER.warn("[heaven] 仙庭之主创建失败，跳过召唤");
            return false;
        }
        BlockPos ground = new BlockPos(flag.arenaCenterX, flag.arenaFloorY, flag.arenaCenterZ);
        boss.moveTo(ground.getX() + 0.5D, flag.arenaFloorY + 1.0D, ground.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        boss.setPersistenceRequired();
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), MobSpawnType.EVENT,
                null, null);
        if (!level.addFreshEntity(boss)) {
            HongmengMod.LOGGER.warn("[heaven] 仙庭之主加入世界失败（区块未加载？）");
            return false;
        }

        // 出场：从天上缓缓降落（BossDescent 自己会锁 AI/无敌，落地后解锁）
        BossDescent.begin(level, boss, ground);
        // 运镜：只对"七首领"生效；仙庭之主需 Lead 在 BossCinematic.isFeaturedBoss 里补一行
        // （见 hunk.md ②）。没补也不会崩，只是不播镜头，靠下面的文字播报兜底。
        BossCinematic.play(level, boss, "仙庭之主", "天外天 · 凌霄宝殿的主宰");

        broadcast(level, "§6§l仙庭之主 · 降临",
                "§e凌霄斗台上方的天门裂开了 —— §6仙庭之主 §e正缓缓降临。");
        level.playSound(null, ground, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.6F, 0.55F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                ground.getX() + 0.5D, ground.getY() + 6.0D, ground.getZ() + 0.5D,
                260, 4.0D, 6.0D, 4.0D, 0.35D);
        HongmengMod.LOGGER.info("[heaven] 仙庭之主已在斗台中央召唤：{}（玩家 {}）",
                ground, player.getName().getString());
        return true;
    }

    /** 全维度播报（聊天栏 + 屏幕上方大字），不依赖运镜是否可用。 */
    private static void broadcast(ServerLevel level, String title, String subtitle) {
        for (ServerPlayer p : level.players()) {
            p.sendSystemMessage(Component.literal(title));
            p.sendSystemMessage(Component.literal(subtitle));
            p.displayClientMessage(Component.literal(title), false);
        }
    }

    // ==================================================================
    // 4. 幂等登天台（15×15 玉台 + 四角灯 + 中央回返符）
    // ==================================================================

    /**
     * 确保登天台存在，返回玩家落点（台面上方一格 = 玩家脚底位置）。
     *
     * <p>幂等三层：SavedData 记台面 y → 中心金牌标记 → 都没有才现场铺。
     * 台面高度取 {@code getHeight(MOTION_BLOCKING, 0, 0)} 再 +2（§1.3 原文）。</p>
     */
    public static BlockPos ensurePlatform(ServerLevel level) {
        HeavenFlag flag = flag(level);
        if (flag.platformFloorY != 0) {
            return new BlockPos(0, flag.platformFloorY + 1, 0);
        }
        // 先保证 (0,0) 所在区块是加载的，否则 getHeight 会把地表算成世界底部
        forceLoad(level, new BlockPos(0, 0, 0), 1);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        int guess = surface + 1;              // 楼板方块 y（台面 = 楼板顶面 = surface+2）
        // SavedData 丢了也能接上：中心金牌是幂等标记
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos marker = new BlockPos(0, guess - dy, 0);
            if (level.getBlockState(marker).is(Blocks.GOLD_BLOCK)) {
                flag.platformFloorY = marker.getY();
                flag.platformBuilt = true;
                flag.setDirty();
                HongmengMod.LOGGER.info("[heaven] 找到已有登天台（中心金牌 {}），台面 y={}",
                        marker, flag.platformFloorY);
                return new BlockPos(0, flag.platformFloorY + 1, 0);
            }
        }
        int placed = buildPlatform(level, guess);
        flag.platformFloorY = guess;
        flag.platformBuilt = true;
        flag.setDirty();
        HongmengMod.LOGGER.info("[heaven] 登天台已铺好：中心 (0, {}, 0)，台面 y={}，写入 {} 格",
                guess, guess + 1, placed);
        return new BlockPos(0, guess + 1, 0);
    }

    /** 现场铺 15×15 玉台；返回写入方块数。幂等由 {@link #ensurePlatform} 保证。 */
    private static int buildPlatform(ServerLevel level, int floorY) {
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState smooth = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState chisel = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        int placed = 0;
        for (int dx = -PLATFORM_HALF; dx <= PLATFORM_HALF; dx++) {
            for (int dz = -PLATFORM_HALF; dz <= PLATFORM_HALF; dz++) {
                boolean border = Math.abs(dx) == PLATFORM_HALF || Math.abs(dz) == PLATFORM_HALF;
                boolean cross = dx == 0 || dz == 0;
                BlockState floor;
                if (dx == 0 && dz == 0) {
                    floor = gold;                                  // 回返符中心 + 幂等标记
                } else if (border) {
                    floor = cross ? chisel : gold;                 // 金框玉边
                } else if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
                    floor = chisel;                                // 中央符盘
                } else if (cross && Math.abs(dx) + Math.abs(dz) <= 5) {
                    floor = gold;                                  // 十字金线（回返符的"符")
                } else {
                    floor = mix(dx, dz, 7, quartz, smooth);        // 玉面纹理
                }
                placed += set(level, new BlockPos(dx, floorY, dz), floor);
                placed += set(level, new BlockPos(dx, floorY - 1, dz), quartz);
                if (dx == 0 && dz == 0) {
                    placed += set(level, new BlockPos(dx, floorY - 1, dz), gold);  // 底层也留标记
                }
                if (border) {
                    placed += set(level, new BlockPos(dx, floorY - 2, dz), smooth);
                    placed += set(level, new BlockPos(dx, floorY - 3, dz), chisel); // 裙边
                } else {
                    placed += set(level, new BlockPos(dx, floorY - 2, dz), smooth);
                }
            }
        }
        // 四角灯：玉柱 + 海晶灯顶（4 根，2 高）
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int lx = sx * (PLATFORM_HALF - 1);
                int lz = sz * (PLATFORM_HALF - 1);
                placed += set(level, new BlockPos(lx, floorY + 1, lz), pillar);
                placed += set(level, new BlockPos(lx, floorY + 2, lz), lamp);
            }
        }
        // 台面上方清 4 格（树/地形卡住落点）；四角灯柱跳过
        for (int dx = -PLATFORM_HALF; dx <= PLATFORM_HALF; dx++) {
            for (int dz = -PLATFORM_HALF; dz <= PLATFORM_HALF; dz++) {
                if (Math.abs(dx) == PLATFORM_HALF - 1 && Math.abs(dz) == PLATFORM_HALF - 1) {
                    continue;
                }
                for (int dy = 1; dy <= 4; dy++) {
                    placed += clear(level, new BlockPos(dx, floorY + dy, dz));
                }
            }
        }
        return placed;
    }

    // ==================================================================
    // 5. 幂等凌霄斗台（31×31 玉台 + 四柱 + 外圈护栏 + 连桥台阶）
    // ==================================================================

    /**
     * 确保凌霄斗台存在（幂等：{@code flag.arenaBuilt}）。
     *
     * <p>方向从正东/西/南/北 4 个候选里挑「地形最高点最低」的那个；
     * 斗台楼板抬到地形最高点之上，所以<b>不会有地形穿出斗台</b>。
     * 两座台之间：3 格宽玉桥 + 逐级台阶（高差 = 斗台楼板 - 登天台楼板）。</p>
     */
    private static void ensureArena(ServerLevel level, HeavenFlag flag) {
        if (flag.arenaBuilt) {
            return;
        }
        int platformY = flag.platformFloorY;
        if (platformY == 0) {
            HongmengMod.LOGGER.warn("[heaven] 登天台未建，跳过斗台（下个 200t 再试）");
            return;
        }
        // ① 方向：4 个候选各取一次地表高度（只 4 次 getHeight，代价小），挑最低的那个
        Direction best = Direction.EAST;
        int bestH = Integer.MAX_VALUE;
        for (Direction dir : CARDINALS) {
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    dir.getStepX() * ARENA_BASE_DISTANCE, dir.getStepZ() * ARENA_BASE_DISTANCE);
            if (h < bestH) {
                bestH = h;
                best = dir;
            }
        }
        // ② 抬高：斗台楼板放到"斗台足迹内地形最高点"之上；抬多少，连桥就长多少。
        //    高差与足迹绑定（抬高会把斗台推远，地形又变），迭代两轮收敛。
        int delta = Math.max(0, bestH + 1 - platformY);
        for (int round = 0; round < 3; round++) {
            int dist = ARENA_BASE_DISTANCE + delta;
            forceLoad(level, new BlockPos(best.getStepX() * dist, platformY + delta,
                    best.getStepZ() * dist), 2);
            int maxH = maxTerrainHeight(level, best.getStepX() * dist, best.getStepZ() * dist);
            int wantY = Math.max(platformY, maxH + 1);
            if (wantY == platformY + delta) {
                break;
            }
            delta = wantY - platformY;
        }
        int dist = ARENA_BASE_DISTANCE + delta;
        int centerX = best.getStepX() * dist;
        int centerZ = best.getStepZ() * dist;
        int arenaY = platformY + delta;
        forceLoad(level, new BlockPos(centerX, arenaY, centerZ), 2);

        int placed = buildArena(level, centerX, centerZ, arenaY);
        placed += buildApproach(level, best, platformY, arenaY, centerX, centerZ);

        flag.arenaBuilt = true;
        flag.arenaCenterX = centerX;
        flag.arenaCenterZ = centerZ;
        flag.arenaFloorY = arenaY;
        flag.setDirty();
        HongmengMod.LOGGER.info(
                "[heaven] 凌霄斗台已铺好：方向 {}，中心 ({}, {}), 楼板 y={}（登天台 y={}，高差 {}），写入 {} 格",
                best, centerX, centerZ, arenaY, platformY, arenaY - platformY, placed);
    }

    /** 一块 31×31 足迹内的地形最高点（首空 y），每 2 格采样一次。 */
    private static int maxTerrainHeight(ServerLevel level, int cx, int cz) {
        int max = level.getMinBuildHeight();
        for (int dx = -ARENA_HALF; dx <= ARENA_HALF; dx += 2) {
            for (int dz = -ARENA_HALF; dz <= ARENA_HALF; dz += 2) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + dx, cz + dz);
                if (h > max) {
                    max = h;
                }
            }
        }
        return max;
    }

    /** 铺斗台楼板 + 骨架 + 护栏 + 四柱；返回写入方块数。 */
    private static int buildArena(ServerLevel level, int cx, int cz, int floorY) {
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState smooth = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState chisel = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        int placed = 0;
        for (int dx = -ARENA_HALF; dx <= ARENA_HALF; dx++) {
            for (int dz = -ARENA_HALF; dz <= ARENA_HALF; dz++) {
                boolean edge = Math.abs(dx) == ARENA_HALF || Math.abs(dz) == ARENA_HALF;
                boolean cross = dx == 0 || dz == 0;
                BlockState floor;
                if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3) {
                    floor = (dx == 0 || dz == 0) ? gold : chisel;   // 中央符阵
                } else if (edge) {
                    floor = (dx + dz) % 8 == 0 ? gold : chisel;     // 外圈金边
                } else if (Math.abs(dx) == 8 || Math.abs(dz) == 8) {
                    floor = gold;                                   // 内圈金环
                } else {
                    floor = mix(dx, dz, 23, quartz, smooth);        // 玉面纹理
                }
                placed += set(level, new BlockPos(cx + dx, floorY, cz + dz), floor);
                // 骨架：外圈 + 十字梁（浮空台看着不虚）
                if (edge || cross) {
                    placed += set(level, new BlockPos(cx + dx, floorY - 1, cz + dz), smooth);
                }
                // 护栏：外圈上一格（矮墙，玩家能跳过，怪不好直接走下去）
                if (edge && !isApproachColumn(dx, dz)) {
                    placed += set(level, new BlockPos(cx + dx, floorY + 1, cz + dz),
                            (dx + dz) % 16 == 0 ? gold : quartz);
                }
            }
        }
        // 四柱：四角各一根 9 高玉柱（柱头金 + 灯），给"凌霄"一点纵深感
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int px = cx + sx * (ARENA_HALF - 3);
                int pz = cz + sz * (ARENA_HALF - 3);
                for (int dy = 1; dy <= 7; dy++) {
                    placed += set(level, new BlockPos(px, floorY + dy, pz), pillar);
                }
                placed += set(level, new BlockPos(px, floorY + 8, pz), gold);
                placed += set(level, new BlockPos(px, floorY + 9, pz), lamp);
            }
        }
        return placed;
    }

    /** 护栏缺口：连桥/台阶那一侧留 3 格宽的门。 */
    private static boolean isApproachColumn(int dx, int dz) {
        // 斗台中心在 ±(24+delta)，桥沿 x 轴或 z 轴进来；护栏缺口开在"朝向登天台"的那条边
        // 的中间 3 格（dx/dz = -15 那条边，另一轴在 -1..1）。
        if (Math.abs(dx) == ARENA_HALF) {
            return Math.abs(dz) <= 1;
        }
        return Math.abs(dz) == ARENA_HALF && Math.abs(dx) <= 1;
    }

    /**
     * 连桥 + 台阶：从登天台边缘铺到斗台边缘。
     *
     * <p>登天台半径 {@value #PLATFORM_HALF}、斗台半径 {@value #ARENA_HALF}，
     * 中心距 {@code 24+delta} → 两座台之间正好隔 1 格；再铺 {@code delta} 级台阶
     * （每级 1 格高、3 格宽）爬上去。</p>
     */
    private static int buildApproach(ServerLevel level, Direction dir, int platformY,
                                     int arenaY, int centerX, int centerZ) {
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState smooth = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        int placed = 0;
        int delta = Math.max(0, arenaY - platformY);
        // 方向在这条轴上的正负：dir 的一侧全是整数坐标
        boolean alongX = dir.getStepX() != 0;
        int sign = alongX ? dir.getStepX() : dir.getStepZ();
        // 桥/台阶从登天台边缘外 1 格开始，一直铺到斗台边缘前 1 格
        int from = PLATFORM_HALF + 1;
        int to = Math.abs(centerX != 0 ? centerX : centerZ) - ARENA_HALF - 1;
        for (int d = from; d <= to; d++) {
            int stepIndex = d - from;
            int top = platformY + Math.min(stepIndex, delta);
            int x = alongX ? sign * d : centerX;
            int z = alongX ? centerZ : sign * d;
            for (int lane = -1; lane <= 1; lane++) {
                int bx = alongX ? x : centerX + lane;
                int bz = alongX ? centerZ + lane : z;
                placed += set(level, new BlockPos(bx, top, bz), gold);
                placed += set(level, new BlockPos(bx, top - 1, bz), smooth);
                // 台阶下方填实，看起来是"玉阶"不是浮空片
                for (int y = platformY; y < top; y++) {
                    placed += set(level, new BlockPos(bx, y, bz), quartz);
                }
                // 头顶清 3 格，避免地形卡头
                for (int dy = 1; dy <= 3; dy++) {
                    placed += clear(level, new BlockPos(bx, top + dy, bz));
                }
            }
        }
        return placed;
    }

    // ==================================================================
    // 6. 环境刷怪（§1.3：每 200t / 每位玩家，怪 < 8，20~40 格安全落点）
    // ==================================================================

    /** 每位玩家每轮的刷怪尝试：怪够多就不刷；找不到安全落点就放弃这一轮。 */
    private static void tryEnvSpawn(ServerLevel level, ServerPlayer player) {
        try {
            int near = level.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(96.0D, 48.0D, 96.0D),
                    e -> e.isAlive()).size();
            if (near >= ENV_MOB_CAP) {
                return;
            }
            RandomSource rnd = level.random;
            for (int attempt = 0; attempt < 6; attempt++) {
                double angle = rnd.nextDouble() * Math.PI * 2.0D;
                double dist = ENV_MIN_DISTANCE
                        + rnd.nextDouble() * (ENV_MAX_DISTANCE - ENV_MIN_DISTANCE);
                int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
                int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
                level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
                BlockPos pos = level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                if (!isSpawnable(level, pos)) {
                    continue;
                }
                int count = 1 + (rnd.nextFloat() < 0.35F ? 1 : 0);
                int spawned = 0;
                for (int i = 0; i < count; i++) {
                    BlockPos at = nearbySafe(level, pos, 3);
                    if (at != null && spawnEnvMob(level, player, at)) {
                        spawned++;
                    }
                }
                if (spawned > 0) {
                    return;
                }
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven] 环境刷怪失败：{}", t.toString());
        }
    }

    private static boolean isSpawnable(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        boolean feetFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        boolean headFree = level.getBlockState(pos.above())
                .getCollisionShape(level, pos.above()).isEmpty();
        boolean groundSolid = !level.getBlockState(pos.below())
                .getCollisionShape(level, pos.below()).isEmpty();
        boolean noFluid = level.getBlockState(pos).getFluidState().isEmpty();
        return feetFree && headFree && groundSolid && noFluid;
    }

    /** 在 pos 附近 ±radius 找另一个安全点（一次刷 2 只时错开）。 */
    private static BlockPos nearbySafe(ServerLevel level, BlockPos pos, int radius) {
        for (int i = 0; i < 8; i++) {
            int dx = level.random.nextInt(radius * 2 + 1) - radius;
            int dz = level.random.nextInt(radius * 2 + 1) - radius;
            BlockPos at = pos.offset(dx, 0, dz);
            if (isSpawnable(level, at)) {
                return at;
            }
        }
        return isSpawnable(level, pos) ? pos : null;
    }

    /**
     * 刷一只环境怪：池子加权。
     *
     * <p>境界缩放交给 {@code CultivationMonsterEntity.onAddedToWorld}（唯一入口），
     * 本方法**不再手动乘 {@code MobScaling}** —— 2026-11-06 修复了天外天环境怪
     * 被双重缩放（这里一次 + onAddedToWorld 一次）导致「精英 天兵 2,774 万血」的 bug。</p>
     */
    private static boolean spawnEnvMob(ServerLevel level, ServerPlayer player, BlockPos pos) {
        String id = pickEnvId(level.random);
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation(HongmengMod.MOD_ID, id));
        if (type == null) {
            HongmengMod.LOGGER.warn("[heaven] 环境怪池里的 {} 未注册，跳过", id);
            return false;
        }
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            return false;
        }
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();         // 天外天的怪不随距离消失
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT,
                null, null);
        // 境界缩放**只能有一处**：交给 CultivationMonsterEntity.onAddedToWorld。
        //
        // ⚠ 旧实现在这里先乘了一遍 realmScaleFor（生命 ×520、伤害按 ×0.6 阻尼），
        //   而 addFreshEntity 触发 onAddedToWorld 时又乘一次境界系数（生命 ×520 / 攻击 ×5129），
        //   于是天外天环境怪是**双重缩放**。2026-11-06 鸿蒙玩家实机数据：
        //     「精英 天兵」maxHp = 27,743,040（正常应为十几万），玩家 166 万一击只掉 6% 血，
        //     自身承伤也高到近乎秒杀 —— 玩家反馈的「小怪数值可以下调一下」。
        //   去掉这一层后，天外天环境怪和其他维度同境小怪同口径（本境下限 + 境界系数），
        //   血量回到十几万、伤害回到「挨十几下才死」的量级。
        //   （player 参数仍用于刷怪点/日志，不由本方法做缩放。）
        return level.addFreshEntity(mob);
    }

    /** 按 §1.3 的 30/25/20/15/10 加权从池子里挑一个。 */
    private static String pickEnvId(RandomSource random) {
        int roll = random.nextInt(100);
        int acc = 0;
        for (int i = 0; i < ENV_POOL.length; i++) {
            acc += ENV_WEIGHTS[i];
            if (roll < acc) {
                return ENV_POOL[i];
            }
        }
        return ENV_POOL[ENV_POOL.length - 1];
    }

    // ==================================================================
    // 7. 维度级 SavedData（台面高度 / 斗台 / Boss 状态）
    // ==================================================================

    private static HeavenFlag flag(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(HeavenFlag::load, HeavenFlag::new,
                DATA_KEY);
    }

    /** 天外天维度级标记（只有一个维度，所以不需要按维度分文件）。 */
    public static final class HeavenFlag extends SavedData {
        /** 登天台楼板方块 y；0 = 还没建。 */
        int platformFloorY;
        boolean platformBuilt;
        /** 凌霄斗台：是否已建 + 中心/楼板坐标。 */
        boolean arenaBuilt;
        int arenaCenterX;
        int arenaCenterZ;
        int arenaFloorY;
        /** Boss 是否召唤过 / 是否已被击杀（击杀后不再自动刷）。 */
        boolean lordSummoned;
        boolean lordDefeated;

        static HeavenFlag load(CompoundTag tag) {
            HeavenFlag flag = new HeavenFlag();
            flag.platformFloorY = tag.getInt("platform_floor_y");
            flag.platformBuilt = tag.getBoolean("platform_built");
            flag.arenaBuilt = tag.getBoolean("arena_built");
            flag.arenaCenterX = tag.getInt("arena_center_x");
            flag.arenaCenterZ = tag.getInt("arena_center_z");
            flag.arenaFloorY = tag.getInt("arena_floor_y");
            flag.lordSummoned = tag.getBoolean("lord_summoned");
            flag.lordDefeated = tag.getBoolean("lord_defeated");
            return flag;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("platform_floor_y", platformFloorY);
            tag.putBoolean("platform_built", platformBuilt);
            tag.putBoolean("arena_built", arenaBuilt);
            tag.putInt("arena_center_x", arenaCenterX);
            tag.putInt("arena_center_z", arenaCenterZ);
            tag.putInt("arena_floor_y", arenaFloorY);
            tag.putBoolean("lord_summoned", lordSummoned);
            tag.putBoolean("lord_defeated", lordDefeated);
            return tag;
        }
    }

    // ==================================================================
    // 8. 方块写入小工具（统一在这里做"加载 + 边界 + 幂等"三件套）
    // ==================================================================

    /** 写一格（区块没加载 / 越界 / 已经是这个方块 → 都不写）。返回 1 表示真写了。 */
    private static int set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!inWorld(level, pos) || !level.isLoaded(pos)) {
            return 0;
        }
        if (level.getBlockState(pos).equals(state)) {
            return 0;
        }
        level.setBlock(pos, state, 2);
        return 1;
    }

    /** 清成空气（只清"本来有东西"的格子）。 */
    private static int clear(ServerLevel level, BlockPos pos) {
        if (!inWorld(level, pos) || !level.isLoaded(pos)) {
            return 0;
        }
        if (level.getBlockState(pos).isAir()) {
            return 0;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        return 1;
    }

    private static boolean inWorld(ServerLevel level, BlockPos pos) {
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight();
    }

    /** 把以 center 为中心、半径 radius 个区块的圆盘强制加载成 FULL（建筑前必做）。 */
    private static void forceLoad(ServerLevel level, BlockPos center, int radius) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.getChunk(cx + dx, cz + dz, ChunkStatus.FULL, true);
            }
        }
    }

    /** 稳定的逐格哈希（同坐标永远同结果 → 玉面纹理可复现）。 */
    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** 多材质混搭：用哈希在候选方块里挑一个（同坐标永远一致）。 */
    private static BlockState mix(int dx, int dz, int salt, BlockState... choices) {
        int i = Math.floorMod(hash(dx + salt * 31, salt, dz - salt * 17), choices.length);
        return choices[i];
    }
}
