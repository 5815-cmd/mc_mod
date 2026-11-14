package com.hongmeng.xiuxian.heaven;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.dungeon.BossRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>天外天「地形与场景」总控</b>——把维度数据里那三个"只有颜色"的群系，
 * 盖成一座真的天上仙庭（13/16 号设计契约里"维度数据 + 运行逻辑"之外的地形/建筑层）。
 *
 * <h2>它建什么（相对 {@code HeavenRealm} 的坐标契约）</h2>
 * <p>{@link HeavenRealm} 已经负责"能进、能刷怪、能打 Boss"的功能骨架：</p>
 * <ul>
 *   <li>登天台：中心 {@code (0, fy, 0)}，15×15（half=7）；</li>
 *   <li>凌霄斗台：31×31（half=15），在登天台之外 24+高差 的正南/北/东/西某一侧。</li>
 * </ul>
 * <p>本类在它们之上补"仙庭本体"，全部以登天台为原点、按 {@code frontDir}（斗台反方向）铺开：</p>
 * <ol>
 *   <li><b>南天门</b>（沿 frontDir 30 格）：双向门阙 + 重檐 + 金匾 + 小阙；</li>
 *   <li><b>白玉天阶</b>（8→112 格）：13 宽登天大道，随地形自动抬高/凿山，望柱栏板 + 云柱灯；</li>
 *   <li><b>凌霄宝殿</b>（130 格，65×65）：三层白玉台基 + 41×25 重檐主殿 + 两配殿 + 日晷 + 金树 + 广场；</li>
 *   <li><b>日轮</b>（宝殿上方 +48）：半径 20 金环 + 12 道金芒；</li>
 *   <li><b>星陨天阶</b>：8 级天阶 + 17 高方尖碑观星台 + 紫晶；</li>
 *   <li><b>云海天池</b>：三池 + 云桥 + 花海；</li>
 *   <li><b>云海浮岛</b> 12 座（含 3 座小亭）；</li>
 *   <li><b>天兵营地 / 云鹤栖台</b>：给怪物侧的环境怪一个"看起来是它家"的锚点；</li>
 *   <li><b>凌霄斗台外围装饰</b>：四云柱 + 白云环 + 玉碑（不碰功能台 31×31）。</li>
 * </ol>
 *
 * <h2>三条硬规矩</h2>
 * <ol>
 *   <li><b>绝不覆盖功能骨架</b>：登天台 ±7、斗台 ±15（含两座台之间的连桥走廊）一格不写；
 *       斗台装饰只写 |dx|,|dz| &gt; 16 的外圈。所有 {@code set} 走 {@link Site#set} 统一判边界。</li>
 *   <li><b>分帧施工</b>：首次进入只同步铺"核心区"（平台环 + 天阶 + 南天门 + 营地，r≤50，约 80 区块），
 *       其余场景在之后的 tick 里<b>一帧一个场景块</b>地铺（每块先只加载自己的包围盒区块），
 *       避免一次性生成几百区块把服务端卡住。</li>
 *   <li><b>可恢复/幂等</b>：进度存维度级 SavedData（{@code builtMask} + {@code version}），
 *       重启后从中断处继续；改了生成逻辑必须把 {@link #VERSION} +1（老存档重进会按新版本重铺）。</li>
 * </ol>
 *
 * <h2>给怪物 / 运行侧的接口</h2>
 * <p>见 {@link #landingCenter}、{@link #frontDirection}、{@link #gateCenter}、{@link #avenuePoint}、
 * {@link #palaceCenter}、{@link #arenaCenter}、{@link #guardAnchors}、{@link #cranePerches}、
 * {@link #sunRingCenter}、{@link #isSceneBuilt}——环境怪/加 Boss 演出需要坐标时直接调，不要自己算。</p>
 *
 * <p><b>注意</b>：本类与 {@link HeavenRealm} 同包，读它的 {@code HeavenFlag}（包内可见）拿
 * 登天台高度与斗台中心；<b>不修改</b>它的任何字段与逻辑。</p>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID)
public final class HeavenScenes {

    // ==================================================================
    // 常量
    // ==================================================================

    /**
     * 场景生成器版本：改动任何 builder 的形状/材料都要 +1（老存档会按新版本重铺）。
     *
     * <p>v2（2026-11-02，Boss 场地完善 D4）：凌霄斗台两侧「天库」石台（各 1 宝箱 + 天兵雕像 +
     * 仙庭旗 + 海晶灯 + 云纹地面）、凌霄宝殿两座配殿各 1 箱、云海浮岛带亭的 3 座各 1 箱、
     * 南天门小阙暗格 1 箱；宝箱统一走 {@link BossRewards#ensureChest} +
     * {@code LOOT_HEAVEN_SUPPLY}（见 {@code 工程/22} §2 第 7/8 行）。</p>
     */
    public static final int VERSION = 3;

    /** 固定随机种子：同一坐标永远同一结果（树/浮岛/星陨分布可复现）。 */
    private static final long SEED = 20261102L;

    /** 场景进度 SavedData 的键（与 HeavenRealm 的 {@code hongmeng_heaven} 分开存）。 */
    private static final String DATA_KEY = "hongmeng_heaven_scenes";

    /** 与 HeavenRealm 共用的维度级标记键（读它的登天台/斗台坐标，写入由它负责）。 */
    private static final String REALM_DATA_KEY = "hongmeng_heaven";

    // ---- 进度位（每个 bit = 一个"场景块"，一帧只铺一块）----
    private static final int BIT_CORE = 0;       // 平台环 + 白玉阶 + 南天门 + 天兵营地
    private static final int BIT_AVENUE = 1;     // 白玉天阶 32→112
    private static final int BIT_PALACE = 2;     // 凌霄宝殿（含广场/配殿/日晷）
    private static final int BIT_SUN = 3;        // 日轮
    private static final int BIT_STARFALL = 4;   // 星陨天阶 + 观星台
    private static final int BIT_POOLS = 5;      // 云海天池 + 云桥
    private static final int BIT_ISLES_A = 6;    // 云海浮岛 1~4
    private static final int BIT_ISLES_B = 7;    // 云海浮岛 5~8
    private static final int BIT_ISLES_C = 8;    // 云海浮岛 9~12
    private static final int BIT_CRANE = 9;      // 云鹤栖台
    private static final int BIT_ARENA = 10;     // 凌霄斗台外围装饰（要等斗台建成）
    private static final int BITS = 11;
    private static final int ALL_DONE = (1 << BITS) - 1;

    // ---- 布局常量（相对登天台中心，沿 frontDir 记 "along"，垂直记 "side"）----
    private static final int PLATFORM_HALF = 7;      // HeavenRealm 的登天台 half
    private static final int ARENA_HALF = 15;        // HeavenRealm 的斗台 half
    private static final int GATE_ALONG = 30;        // 南天门门轴距离
    private static final int GATE_HALF = 10;         // 门洞半宽（沿 side）
    private static final int AVENUE_START = 8;       // 白玉阶起点（平台边）
    /** 白玉天阶终点：宝殿一层台基边缘（台基从 PALACE_ALONG-PALACE_HALF=98 开始）。 */
    private static final int AVENUE_END = 98;
    private static final int AVENUE_HALF = 7;        // 白玉天阶半宽（15 宽，与南天门门洞 ±7 对齐）
    private static final int PALACE_ALONG = 130;     // 宝殿中心距离
    private static final int PALACE_HALF = 32;       // 宝殿台基 half（65×65）
    private static final int SUN_HEIGHT = 48;        // 日轮相对宝殿台基的高度
    private static final int ARENA_DECOR_GAP = 16;   // 斗台装饰离中心的最小距离（footprint half=15）

    private static final Direction[] CARDINALS = {
            Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    private HeavenScenes() {
    }

    // ==================================================================
    // 对怪物 / 运行侧的公开坐标 API
    // ==================================================================

    /** 登天台中心（walk 的高度取台面：楼板 y + 1）。 */
    public static BlockPos landingCenter(ServerLevel level) {
        HeavenRealm.HeavenFlag flag = realmFlag(level);
        if (flag == null || flag.platformFloorY == 0) {
            return new BlockPos(0, 64, 0);
        }
        return new BlockPos(0, flag.platformFloorY + 1, 0);
    }

    /**
     * 仙庭正面方向（南天门/白玉阶/宝殿沿它铺开）= 斗台方向的反方向。
     * <p>斗台未建时用与 HeavenRealm 同一套"四方向取地形最低"规则预测，保证不压在斗台上。</p>
     */
    public static Direction frontDirection(ServerLevel level) {
        HeavenRealm.HeavenFlag flag = realmFlag(level);
        if (flag != null && flag.arenaBuilt) {
            return opposite(new BlockPos(0, 0, 0),
                    new BlockPos(flag.arenaCenterX, 0, flag.arenaCenterZ));
        }
        return oppositePredicted(level);
    }

    /** 斗台中心（转调 HeavenRealm 的标记；未建返回 null）。 */
    public static BlockPos arenaCenter(ServerLevel level) {
        HeavenRealm.HeavenFlag flag = realmFlag(level);
        if (flag == null || !flag.arenaBuilt) {
            return null;
        }
        return new BlockPos(flag.arenaCenterX, flag.arenaFloorY, flag.arenaCenterZ);
    }

    /** 南天门门轴中心（地面高度 = 台面 + 3）。 */
    public static BlockPos gateCenter(ServerLevel level) {
        HeavenRealm.HeavenFlag flag = realmFlag(level);
        int base = flag != null && flag.platformFloorY != 0 ? flag.platformFloorY + 1 : 64;
        Direction f = frontDirection(level);
        return new BlockPos(f.getStepX() * GATE_ALONG, base + 3, f.getStepZ() * GATE_ALONG);
    }

    /** 白玉天阶上距平台 {@code along} 格的点（along ∈ [8,112]）。 */
    public static BlockPos avenuePoint(ServerLevel level, int along) {
        HeavenRealm.HeavenFlag flag = realmFlag(level);
        int base = flag != null && flag.platformFloorY != 0 ? flag.platformFloorY + 1 : 64;
        Direction f = frontDirection(level);
        int d = Math.max(AVENUE_START, Math.min(AVENUE_END, along));
        return new BlockPos(f.getStepX() * d, base, f.getStepZ() * d);
    }

    /** 凌霄宝殿广场中心（台基顶面高度）。 */
    public static BlockPos palaceCenter(ServerLevel level) {
        HeavenRealm.HeavenFlag flag = realmFlag(level);
        int base = flag != null && flag.platformFloorY != 0 ? flag.platformFloorY + 1 : 64;
        Direction f = frontDirection(level);
        return new BlockPos(f.getStepX() * PALACE_ALONG, palaceFloorY(level, base, f),
                f.getStepZ() * PALACE_ALONG);
    }

    /** 日轮圆心（给 Boss 演出/氛围用）。 */
    public static BlockPos sunRingCenter(ServerLevel level) {
        BlockPos p = palaceCenter(level);
        return new BlockPos(p.getX(), p.getY() + SUN_HEIGHT, p.getZ());
    }

    /** 天兵营地的 3 个出生锚（南天门外两侧；给环境怪/守卫用）。 */
    public static BlockPos[] guardAnchors(ServerLevel level) {
        HeavenRealm.HeavenFlag flag = realmFlag(level);
        int base = flag != null && flag.platformFloorY != 0 ? flag.platformFloorY + 1 : 64;
        Direction f = frontDirection(level);
        Direction side = f.getClockWise();
        BlockPos mid = new BlockPos(f.getStepX() * 42, base + 3, f.getStepZ() * 42);
        return new BlockPos[]{
                mid.offset(side.getStepX() * 14, 0, side.getStepZ() * 14),
                mid.offset(-side.getStepX() * 14, 0, -side.getStepZ() * 14),
                mid
        };
    }

    /** 云鹤栖台 2 个出生锚（高处，给飞行怪用）。 */
    public static BlockPos[] cranePerches(ServerLevel level) {
        HeavenRealm.HeavenFlag flag = realmFlag(level);
        int base = flag != null && flag.platformFloorY != 0 ? flag.platformFloorY + 1 : 64;
        Direction f = frontDirection(level);
        Direction side = f.getClockWise();
        int along = 62;
        int y = base + 28;
        return new BlockPos[]{
                new BlockPos(f.getStepX() * along + side.getStepX() * 32, y,
                        f.getStepZ() * along + side.getStepZ() * 32),
                new BlockPos(f.getStepX() * along - side.getStepX() * 32, y,
                        f.getStepZ() * along - side.getStepZ() * 32)
        };
    }

    /** 地形场景是否已全部铺完。 */
    public static boolean isSceneBuilt(ServerLevel level) {
        SceneFlag flag = sceneFlag(level);
        return flag != null && flag.version == VERSION && flag.builtMask == ALL_DONE;
    }

    /** 场景版本（给调试/交接文档引用）。 */
    public static int sceneVersion() {
        return VERSION;
    }

    // ==================================================================
    // 事件入口：进入维度 / 维度节拍
    // ==================================================================

    /** 玩家进入天外天：先把功能骨架（天台/斗台）兜一遍，再启动场景流水线。 */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        try {
            if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            if (!HeavenRealm.DIMENSION.equals(event.getTo())) {
                return;
            }
            if (player.level() instanceof ServerLevel level) {
                kick(level, player);
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven-scene] 进入处理失败：{}", t.toString());
        }
    }

    /** 维度节拍：每 5t 推进一个场景块（只在还有没铺完的块时干活）。 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (!(event.level instanceof ServerLevel level) || !HeavenRealm.isIn(level)) {
                return;
            }
            if (level.getGameTime() % 5L != 0L) {
                return;
            }
            if (level.players().isEmpty() && sceneFlag(level).builtMask == 0) {
                return;                     // 没人、也没开过工：别在空维度里硬生成区块
            }
            kick(level, level.players().isEmpty() ? null : level.players().get(0));
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven-scene] 维度节拍失败：{}", t.toString());
        }
    }

    /**
     * 一次推进：确保功能骨架 → 铺下一块场景。
     *
     * @param player 触发玩家（可为 null：命令/无人在线时的续建）
     */
    private static void kick(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        HeavenRealm.HeavenFlag realm = realmFlag(level);
        if (realm == null) {
            return;
        }
        // ① 功能骨架兜底：登天台 + 凌霄斗台（HeavenRealm 自己也会做，幂等）
        if (!realm.platformBuilt && player != null) {
            try {
                HeavenRealm.onEntered(level, player);
            } catch (Throwable t) {
                HongmengMod.LOGGER.warn("[heaven-scene] 功能骨架兜底失败：{}", t.toString());
            }
            realm = realmFlag(level);
        }
        if (realm == null || !realm.platformBuilt) {
            return;                         // 登天台还没好，场景无法定位
        }
        // ② 斗台还没建（例如上个会话只建了登天台）：先让它建出来，别用预测方向铺核心区
        if (!realm.arenaBuilt && player != null) {
            try {
                HeavenRealm.onEntered(level, player);
            } catch (Throwable t) {
                HongmengMod.LOGGER.warn("[heaven-scene] 补建斗台失败：{}", t.toString());
            }
            realm = realmFlag(level);
            if (realm == null) {
                return;
            }
        }
        // ③ 核心块要求先知道斗台方向（避免压到斗台）
        Direction front = frontDirection(level);
        SceneFlag flag = sceneFlag(level);
        if (flag.version != VERSION) {
            // 版本变了：清进度重铺（老装饰不主动拆，新场景会覆盖同位置）
            flag.version = VERSION;
            flag.builtMask = 0;
            flag.chests = 0;                // v2 起重新统计新放的探索宝箱
            flag.setDirty();
            HongmengMod.LOGGER.info("[heaven-scene] 场景版本升级到 v{}，重新铺场景（前方={}，台面y={}）",
                    VERSION, front, realm.platformFloorY + 1);
        }
        if (flag.builtMask == ALL_DONE) {
            return;
        }
        Site site = new Site(level, realm.platformFloorY + 1, front);
        // 每个 tick 只允许加载 6 个“尚未加载”的区块：
        // 旧版一块场景会一口气生成几十个区块（实测单块最高 1541ms，服务端
        // "Can't keep up! 2611ms / 52 ticks behind"）。被预算掐断时本块不标记完成，
        // 下一 tick 从断点继续 —— 玩家看到的是场景“逐渐铺开”，而不是卡顿。
        site.chunkBudget = 6;
        int bit = nextPending(flag, realm);
        if (bit < 0) {
            return;
        }
        long t0 = System.currentTimeMillis();
        int placed;
        try {
            placed = buildStep(level, site, bit, realm);
        } catch (SceneDefer deferred) {
            HongmengMod.LOGGER.debug(
                    "[heaven-scene] 场景块 {} 预载延后：本 tick 区块预算用完，下一 tick 继续",
                    nameOf(bit));
            return;                     // 不标记完成
        }
        flag.builtMask |= (1 << bit);
        flag.setDirty();
        if (site.chests > 0) {
            flag.chests += site.chests;
            flag.setDirty();
        }
        long cost = System.currentTimeMillis() - t0;
        HongmengMod.LOGGER.info(
                "[heaven-scene] 场景块 {} 完成：placed={} cleared={} skippedUnloaded={} rejected={} 耗时={}ms（进度 {}/{}）",
                nameOf(bit), placed, site.cleared, site.skippedUnloaded, site.rejected, cost,
                Integer.bitCount(flag.builtMask), BITS);
        if (site.chests > 0) {
            // 验收口径（任务 D4）：[detail] heaven 宝箱：N 个
            HongmengMod.LOGGER.info("[detail] heaven 宝箱：{} 个（本块「{}」新增 {}）",
                    flag.chests, nameOf(bit), site.chests);
        }
        if (flag.builtMask == ALL_DONE) {
            HongmengMod.LOGGER.info("[heaven-scene] 天外天地形场景全部完成（v{}）", VERSION);
            HongmengMod.LOGGER.info("[detail] heaven 宝箱：{} 个（场景 v{} 全部铺完）",
                    flag.chests, VERSION);
        }
    }

    /** 下一个该铺的块；{@code BIT_ARENA} 要等斗台建成，{@code BIT_CORE} 要等方向可知。 */
    private static int nextPending(SceneFlag flag, HeavenRealm.HeavenFlag realm) {
        for (int bit = 0; bit < BITS; bit++) {
            if ((flag.builtMask & (1 << bit)) != 0) {
                continue;
            }
            if (bit == BIT_ARENA && !realm.arenaBuilt) {
                continue;                   // 斗台还没建好：这一块先跳过，等其他块铺完再来
            }
            return bit;
        }
        return -1;
    }

    private static String nameOf(int bit) {
        return switch (bit) {
            case BIT_CORE -> "核心区(平台环/白玉阶/南天门/天兵营地)";
            case BIT_AVENUE -> "白玉天阶";
            case BIT_PALACE -> "凌霄宝殿";
            case BIT_SUN -> "日轮";
            case BIT_STARFALL -> "星陨天阶";
            case BIT_POOLS -> "云海天池";
            case BIT_ISLES_A -> "云海浮岛(1-4)";
            case BIT_ISLES_B -> "云海浮岛(5-8)";
            case BIT_ISLES_C -> "云海浮岛(9-12)";
            case BIT_CRANE -> "云鹤栖台";
            case BIT_ARENA -> "凌霄斗台装饰";
            default -> "?" + bit;
        };
    }

    // ==================================================================
    // 调试/验收：一次性同步铺完（/hongmeng scene heaven 调）
    // ==================================================================

    /**
     * 同步把整套场景铺完（幂等）。给服务端 headless 验收与救档补建用；
     * 正常游戏不要调（会一次性生成较多区块，可能卡 1~3 秒）。
     */
    public static int ensureAll(ServerLevel level) {
        HeavenRealm.HeavenFlag realm = realmFlag(level);
        if (realm == null || !realm.platformBuilt) {
            HongmengMod.LOGGER.warn("[heaven-scene] ensureAll：登天台未建，先铺登天台");
            HeavenRealm.ensurePlatform(level);
            realm = realmFlag(level);
        }
        if (realm == null) {
            return 0;
        }
        SceneFlag flag = sceneFlag(level);
        if (flag.version != VERSION) {
            flag.version = VERSION;
            flag.builtMask = 0;
            flag.chests = 0;
            flag.setDirty();
        }
        Direction front = frontDirection(level);
        Site site = new Site(level, realm.platformFloorY + 1, front);
        long t0 = System.currentTimeMillis();
        for (int bit = 0; bit < BITS; bit++) {
            if ((flag.builtMask & (1 << bit)) != 0) {
                continue;
            }
            if (bit == BIT_ARENA && !realm.arenaBuilt) {
                HongmengMod.LOGGER.warn("[heaven-scene] ensureAll：斗台未建，跳过斗台装饰块");
                continue;
            }
            long s0 = System.currentTimeMillis();
            int placed = buildStep(level, site, bit, realm);
            flag.builtMask |= (1 << bit);
            flag.setDirty();
            HongmengMod.LOGGER.info("[heaven-scene] ensureAll 块 {}：placed={} skippedUnloaded={} rejected={} 耗时={}ms",
                    nameOf(bit), placed, site.skippedUnloaded, site.rejected,
                    System.currentTimeMillis() - s0);
        }
        if (site.chests > 0) {
            flag.chests += site.chests;
            flag.setDirty();
        }
        HongmengMod.LOGGER.info("[heaven-scene] ensureAll 完成：总 placed={} cleared={} skippedUnloaded={} rejected={} 总耗时={}ms",
                site.placed, site.cleared, site.skippedUnloaded, site.rejected,
                System.currentTimeMillis() - t0);
        HongmengMod.LOGGER.info("[detail] heaven 宝箱：{} 个（ensureAll 本次新增 {}）",
                flag.chests, site.chests);
        return site.placed;
    }

    private static int buildStep(ServerLevel level, Site site, int bit, HeavenRealm.HeavenFlag realm) {
        try {
            return switch (bit) {
                case BIT_CORE -> core(site);
                case BIT_AVENUE -> avenue(site);
                case BIT_PALACE -> palace(site);
                case BIT_SUN -> sunRing(site);
                case BIT_STARFALL -> starfall(site);
                case BIT_POOLS -> pools(site);
                case BIT_ISLES_A -> islands(site, 0);
                case BIT_ISLES_B -> islands(site, 4);
                case BIT_ISLES_C -> islands(site, 8);
                case BIT_CRANE -> cranePerches(site);
                case BIT_ARENA -> arenaDecor(site, realm);
                default -> 0;
            };
        } catch (SceneDefer deferred) {
            // PERF(A) 修复「延后被当成失败」的正确性 bug：
            // 老实现这里直接 `catch (Throwable)` 把 SceneDefer 吞了并返回 site.placed，
            // 于是 kick() 误以为这一块已经建完 —— 实机日志出现
            // 「场景块 白玉天阶 生成失败（不影响其它块）：HeavenScenes$SceneDefer」紧接着
            // 「场景块 白玉天阶 完成：placed=0 ...」→ 这一块场景永远缺失，而且 944ms 白卡。
            // 现在把预算延后原样抛回给 kick()：不标记完成，下一 tick 从断点继续。
            throw deferred;
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven-scene] 场景块 {} 生成失败（不影响其它块）：{}",
                    nameOf(bit), t.toString());
            return site.placed;
        }
    }

    // ==================================================================
    // 场景 1：核心区（平台环 + 白玉阶 8→30 + 南天门 + 天兵营地）
    // ==================================================================

    private static int core(Site s) {
        int before = s.placed;
        s.preloadBox(-12, -24, 58, 24);
        // ---- ① 登天台外围一圈玉栏（不碰 half=7 的台面）----
        for (int a = -12; a <= 12; a++) {
            for (int b = -12; b <= 12; b++) {
                int ring = Math.max(Math.abs(a), Math.abs(b));
                if (ring < PLATFORM_HALF + 1 || ring > PLATFORM_HALF + 4) {
                    continue;
                }
                // 正面方向留 7 格宽的门（跟着白玉阶走）
                if (a >= PLATFORM_HALF + 1 && Math.abs(b) <= 3) {
                    continue;
                }
                s.set(a, s.baseY - 1, b, (ring == PLATFORM_HALF + 4) ? M.SMOOTH : M.JADE);
                if (ring == PLATFORM_HALF + 4) {
                    s.set(a, s.baseY, b, (Math.floorMod(a + b, 6) == 0) ? M.GOLD : M.SLAB);
                }
            }
        }
        // 四角云柱（十字对角，离台面 12 格）
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int px = sx * 12;
                int pz = sz * 12;
                for (int y = 0; y <= 6; y++) {
                    s.setAbs(px, s.baseY + y, pz, M.PILLAR);
                }
                s.setAbs(px, s.baseY + 7, pz, M.LAMP);
                s.setAbs(px, s.baseY + 8, pz, M.CLOUD);
            }
        }

        // ---- ② 白玉阶：8 → GATE_ALONG（3 级缓升）----
        for (int d = AVENUE_START; d <= GATE_ALONG; d++) {
            int stepY = s.baseY + Math.min(3, (d - AVENUE_START) / 7);
            for (int side = -GATE_HALF; side <= GATE_HALF; side++) {
                s.clearAbove(s.front().getStepX() * d + s.side().getStepX() * side,
                        stepY + 1, s.front().getStepZ() * d + s.side().getStepZ() * side, 6);
                s.set(d, stepY, side, (Math.abs(side) == GATE_HALF) ? M.CHISEL
                        : (Math.abs(side) % 4 == 0 ? M.JADE : M.SMOOTH));
                for (int k = 1; k <= 3; k++) {
                    s.set(d, stepY - k, side, M.SMOOTH);
                }
            }
            // 望柱（每 6 格一对，柱头灯）
            if (d % 6 == 0) {
                for (int sgn : new int[]{-1, 1}) {
                    int side = sgn * (GATE_HALF + 1);
                    s.set(d, stepY, side, M.PILLAR);
                    s.set(d, stepY + 1, side, M.PILLAR);
                    s.set(d, stepY + 2, side, M.LAMP);
                }
            }
        }

        // ---- ③ 南天门（门轴 d=30，门洞 21 宽 × 16 高）----
        int gateY = s.baseY + 3;
        // 门柱：两根 5×5 方柱
        for (int sgn : new int[]{-1, 1}) {
            for (int side = sgn * (GATE_HALF - 2); side != sgn * (GATE_HALF + 3);
                 side += sgn) {
                for (int a = GATE_ALONG - 2; a <= GATE_ALONG + 2; a++) {
                    for (int y = 0; y <= 15; y++) {
                        boolean trim = Math.abs(side) == GATE_HALF + 2
                                || y == 0 || y == 15;
                        s.set(a, gateY + y, side, trim ? M.GOLD : M.PILLAR);
                    }
                    s.set(a, gateY + 16, side, M.CHISEL);
                }
            }
        }
        // 额枋（跨门洞）
        for (int a = GATE_ALONG - 2; a <= GATE_ALONG + 2; a++) {
            for (int side = -GATE_HALF - 2; side <= GATE_HALF + 2; side++) {
                s.set(a, gateY + 15, side, M.CHISEL);
                s.set(a, gateY + 16, side, M.JADE);
            }
        }
        // 金匾（正面朝向平台：-front 一侧）
        for (int a = GATE_ALONG - 3; a >= GATE_ALONG - 3; a--) {
            for (int side = -4; side <= 4; side++) {
                s.setAbs(s.front().getStepX() * a + s.side().getStepX() * side,
                        gateY + 12, s.front().getStepZ() * a + s.side().getStepZ() * side,
                        M.GOLD);
            }
        }
        // 重檐：下层大檐（d±5，side ±14），上层收分（d±3，side ±11）
        buildEave(s, GATE_ALONG, gateY + 17, GATE_HALF + 3, 5, 11);
        buildEave(s, GATE_ALONG, gateY + 20, GATE_HALF + 1, 3, 8);
        // 屋脊 + 鸱吻
        for (int side = -GATE_HALF - 1; side <= GATE_HALF + 1; side++) {
            s.set(GATE_ALONG, gateY + 23, side, M.GOLD);
        }
        for (int sgn : new int[]{-1, 1}) {
            s.set(GATE_ALONG, gateY + 24, sgn * (GATE_HALF + 1), M.GOLD);
        }
        // 两侧小阙（5×5×9）
        for (int sgn : new int[]{-1, 1}) {
            int base = sgn * (GATE_HALF + 7);
            for (int a = GATE_ALONG - 2; a <= GATE_ALONG + 2; a++) {
                for (int side = base - 2; side <= base + 2; side++) {
                    for (int y = 0; y <= 8; y++) {
                        boolean wall = Math.abs(a - GATE_ALONG) == 2
                                || Math.abs(side - base) == 2;
                        if (!wall && y > 0) {
                            s.clear(a, gateY + y, side, false);
                        } else {
                            s.set(a, gateY + y, side, y == 8 ? M.CHISEL : M.JADE);
                        }
                    }
                }
            }
            s.set(GATE_ALONG, gateY + 9, base, M.GOLD);
        }

        // ---- ③.1 南天门门楼「暗格」：门柱内侧凿一个 1×1 壁龛，嵌 1 个天库宝箱 ----
        // 小阙内部 y=8 的 3×3 是露顶的，补一层瓦封顶（顺手把悬空金顶坐实）
        int cacheSide = GATE_HALF + 7;                    // 与上面小阙 base 一致（+17）
        for (int a = GATE_ALONG - 1; a <= GATE_ALONG + 1; a++) {
            for (int side = cacheSide - 1; side <= cacheSide + 1; side++) {
                s.set(a, gateY + 8, side, M.CHISEL);
            }
        }
        // 暗格开在 +side 门柱的"门洞内侧"（side=GATE_HALF-2=8），从白玉阶走进门洞就够得着；
        // 壁龛只有 1 格深，柱身后面 4 格仍是实心，不改变门柱承重与外观轮廓。
        s.clear(GATE_ALONG, gateY + 1, GATE_HALF - 2, false);
        BlockPos gateCacheChest = new BlockPos(
                s.worldX(GATE_ALONG, GATE_HALF - 2), gateY + 1,
                s.worldZ(GATE_ALONG, GATE_HALF - 2));
        if (s.chestWritable(gateCacheChest)
                && BossRewards.ensureChest(s.level(), gateCacheChest,
                        BossRewards.LOOT_HEAVEN_SUPPLY, s.chestSeed(gateCacheChest))) {
            s.chests++;
        }

        // ---- ④ 天兵营地：南天门内侧左右各一座（3 帐 + 营火 + 兵器架）----
        for (int sgn : new int[]{-1, 1}) {
            int sideBase = sgn * 14;
            for (int t = 0; t < 3; t++) {
                int a = 40 + t * 6;
                int side = sideBase + (t - 1) * 3;
                tent(s, a, gateY - 1, side, 4);
            }
            // 营火 + 兵器架
            s.set(46, gateY - 1, sideBase, M.CAMPFIRE);
            for (int k = -1; k <= 1; k++) {
                s.set(50, gateY, sideBase + k, M.IRON_BARS);
                s.set(50, gateY + 1, sideBase + k, M.IRON_BARS);
            }
            s.set(52, gateY, sideBase, M.RED_WOOL);
            s.set(52, gateY + 1, sideBase, M.RED_WOOL);
        }
        return s.placed - before;
    }

    /** 重檐：沿 side 铺一层"瓦"（梯形收分 + 檐角翘起）。 */
    private static void buildEave(Site s, int along, int y, int sideHalf, int alongHalf, int rise) {
        for (int a = along - alongHalf; a <= along + alongHalf; a++) {
            for (int side = -sideHalf; side <= sideHalf; side++) {
                boolean edge = Math.abs(side) == sideHalf || Math.abs(a - along) == alongHalf;
                s.set(a, y, side, edge ? M.CHISEL : M.JADE);
                if (edge) {
                    // 檐角向上翘一格
                    s.set(a, y + 1, side, M.CHISEL);
                }
            }
        }
        // 屋面上层再收一格（斜面感）
        for (int a = along - alongHalf + 1; a <= along + alongHalf - 1; a++) {
            for (int side = -sideHalf + 2; side <= sideHalf - 2; side++) {
                s.set(a, y + rise, side, M.SMOOTH);
            }
        }
    }

    /** 天兵军帐：5×5 的白色羊毛帐 + 玉柱杆 + 门口火把。 */
    private static void tent(Site s, int along, int y, int side, int r) {
        for (int a = along - r; a <= along + r; a++) {
            for (int b = side - r; b <= side + r; b++) {
                if (Math.abs(a - along) + Math.abs(b - side) <= r) {
                    s.set(a, y, b, M.WOOL);
                    if (Math.abs(a - along) + Math.abs(b - side) <= r - 1) {
                        s.set(a, y + 1, b, M.WOOL);
                    }
                }
            }
        }
        s.set(along, y + 2, side, M.PILLAR);
        s.set(along + r, y + 1, side, M.LANTERN);
    }

    // ==================================================================
    // 场景 2：白玉天阶（32 → 112，随地形抬高 / 必要时凿山）
    // ==================================================================

    /**
     * 白玉天阶（d=31 → 98）。
     *
     * <p>修“台阶错位”的关键点（详见本批报告）：</p>
     * <ul>
     *   <li><b>不碰南天门</b>：从 d=GATE_ALONG+1=31 开始（门柱在 28..32 的两侧），
     *       d=31/32 只平铺门洞地面（=gateY），从 33 起才上坡；</li>
     *   <li><b>严格单调</b>：y(d) = gateY + ⌊(d-33)·rise/run⌋，只升不降、不跟地形起伏
     *       （地形高就凿开，不把台阶顶歪）；</li>
     *   <li><b>宽度对齐门洞</b>：±7（15 宽）；栏杆在 ±8，门口两格不放栏杆（会压到门柱）；</li>
     *   <li><b>每级台阶是真正的石英楼梯块</b>（FACING=前进方向），不再是“整墙台阶”；</li>
     *   <li><b>终点对齐宝殿台基</b>：AVENUE_END=98 高度 = palaceFloorY-1（台基面），
     *       与丹陛/台基无缝；</li>
     *   <li>建完做一次逐格自检（脚下实心 / 身位空 / 单格≤1 / 无缺口），日志可查。</li>
     * </ul>
     */
    private static int avenue(Site s) {
        int before = s.placed;
        int d0 = GATE_ALONG + 1;                 // 31：门柱之后，门洞地面从这里开始铺
        int rampFrom = GATE_ALONG + 3;           // 33：门洞后开始上坡
        int end = AVENUE_END;                    // 98：宝殿一层台基边缘
        int y0 = s.baseY + 3;                    // gateY：与南天门门槛同高
        int y1 = palaceFloorY(s.level(), s.baseY, s.front()) - 1;   // 台基面
        // 最后 6 格（92..98）做成**与丹陛同高的水平落地平台**：
        // 否则斜坡到 98 才收顶，而丹陛从 92 就开始抬升，会在丹陛两侧形成“越来越高”的错位墙。
        int rampEnd = end - 6;
        int rise = Math.max(0, y1 - y0);
        int run = Math.max(1, rampEnd - rampFrom);
        if (rise > run) {
            // 极端地形：台基被抬得比 65 格跑道还高 → 只能每格升 1，终点会低于台基面；
            // 记录警告，避免悄悄出现“走不上去”的断崖。
            HongmengMod.LOGGER.warn(
                    "[heaven-scene] 白玉天阶高差 {} 大于跑道 {}，终点将低于台基面（地形极端）",
                    rise, run);
            rise = run;
        }
        s.preloadBox(d0 - 2, -AVENUE_HALF - 8, end + 8, AVENUE_HALF + 8);
        BlockState stair = stairFor(s.front());

        int prevY = y0;
        int riseRows = 0;
        int lastY = y0;
        for (int d = d0; d <= end; d++) {
            int y;
            if (d < rampFrom) {
                y = y0;                          // 门洞内平铺，与门槛同高
            } else if (d >= rampEnd) {
                y = y1;                          // 丹陛前的水平落地平台
            } else {
                long num = (long) (d - rampFrom) * rise;
                y = y0 + (int) (num / run);
            }
            if (y < prevY) {
                y = prevY;                       // 绝对不允许下坡（下坡就是“错位”）
            }
            // 每格最多升 1（用楼梯块做竖面，1 格也能走）
            if (y > prevY + 1) {
                y = prevY + 1;
            }
            boolean riseRow = y > prevY;
            if (riseRow) {
                riseRows++;
            }
            prevY = y;
            lastY = y;

            int terr = terrainHeight(s.level(), s, d);
            for (int side = -AVENUE_HALF; side <= AVENUE_HALF; side++) {
                int wx = s.worldX(d, side);
                int wz = s.worldZ(d, side);
                // 地形/树挡路就凿开；地形越高清得越高（不再跟着地形起伏）
                int clearH = Math.max(10, terr - y + 2);
                s.clearAbove(wx, y + 1, wz, clearH);
                if (riseRow) {
                    // 竖面用楼梯块：从上一级平滑走上这一级
                    s.set(d, y, side, stair);
                } else {
                    s.set(d, y, side, (Math.abs(side) == AVENUE_HALF) ? M.CHISEL
                            : (Math.abs(side) % 3 == 0 ? M.JADE : M.SMOOTH));
                }
                // 下方垫实 4 层（阶梯厚度），保证没有空心台阶
                for (int k = 1; k <= 4; k++) {
                    s.set(d, y - k, side, M.SMOOTH);
                }
            }
            // 栏杆：±8（门洞两格不放，免得压到门柱；门柱在侧向 8..12、沿 28..32）
            if (d >= GATE_ALONG + 3) {
                for (int sgn : new int[]{-1, 1}) {
                    int side = sgn * (AVENUE_HALF + 1);
                    boolean post = d % 6 == 0;
                    s.set(d, y, side, post ? M.PILLAR : M.SLAB);
                    if (post) {
                        s.set(d, y + 1, side, M.PILLAR);
                        s.set(d, y + 2, side, M.LAMP);
                    }
                }
            }
            // 云柱灯（每 24 格一对，立在道外）
            if (d % 24 == 12) {
                for (int sgn : new int[]{-1, 1}) {
                    int side = sgn * (AVENUE_HALF + 4);
                    for (int h = 0; h <= 4; h++) {
                        s.set(d, y + h, side, M.PILLAR);
                    }
                    s.set(d, y + 5, side, M.LAMP);
                }
            }
        }

        // ---- 自检：逐格验证可走性，日志可查（错位/空洞会直接体现在计数里） ----
        int gaps = 0;
        int badRise = 0;
        int prev = Integer.MIN_VALUE;
        for (int d = d0; d <= end; d++) {
            int y = yAt(d, d0, rampFrom, end, y0, rise, run);
            if (prev != Integer.MIN_VALUE && y > prev + 1) {
                badRise++;
            }
            prev = y;
            int cx = s.worldX(d, 0);
            int cz = s.worldZ(d, 0);
            // 坐标约定：y 是“脚下方块”的坐标（block top = y+1），
            // 所以判定是：y 处有地板（整块/楼梯/台阶）且 y+1、y+2 是空、y-1 有支撑。
            BlockPos below = new BlockPos(cx, y - 1, cz);
            BlockPos floor = new BlockPos(cx, y, cz);
            BlockPos head1 = new BlockPos(cx, y + 1, cz);
            BlockPos head2 = new BlockPos(cx, y + 2, cz);
            if (!s.level().isLoaded(floor)) {
                continue;
            }
            var floorState = s.level().getBlockState(floor);
            boolean okFloor = !floorState.isAir();
            boolean okBelow = !s.level().getBlockState(below).isAir()
                    && !s.level().getBlockState(below).getCollisionShape(s.level(), below).isEmpty();
            boolean okHead = s.level().getBlockState(head1).isAir()
                    && s.level().getBlockState(head2).isAir();
            if (!okFloor || !okBelow || !okHead) {
                gaps++;
            }
        }
        HongmengMod.LOGGER.info(
                "[heaven-scene] 白玉天阶自检：d={}..{}，起点 y={}，终点 y={}，台阶 {} 级，"
                        + "落地平台 {} 格，自检缺口 {}，异常抬升 {}",
                d0, end, y0, lastY, riseRows, end - rampEnd + 1, gaps, badRise);
        return s.placed - before;
    }

    /** 白玉天阶在沿轴 d 处的地面 y（与建造公式完全一致，供自检/接口复用）。 */
    private static int yAt(int d, int d0, int rampFrom, int end, int y0, int rise, int run) {
        int rampEnd = end - 6;
        if (d < rampFrom) {
            return y0;
        }
        if (d >= rampEnd) {
            return y0 + rise;                // 落地平台（= palaceFloorY-1）
        }
        long num = (long) (d - rampFrom) * rise;
        return y0 + (int) (num / run);
    }

    /** 石英楼梯（朝向 = 前进方向，即“向上走”的方向）。 */
    private static BlockState stairFor(Direction facing) {
        return Blocks.QUARTZ_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, facing);
    }

    // ==================================================================
    // 场景 3：凌霄宝殿（130 格，65×65 台基 + 主殿 + 双配殿 + 广场）
    // ==================================================================

    private static int palace(Site s) {
        int before = s.placed;
        s.preloadBox(PALACE_ALONG - PALACE_HALF - 10, -PALACE_HALF - 10,
                PALACE_ALONG + PALACE_HALF + 10, PALACE_HALF + 10);
        int y = palaceFloorY(s.level(), s.baseY, s.front());
        int A = PALACE_ALONG;
        // ---- ① 三层台基（half 32/28/24，逐层 +1）----
        for (int tier = 0; tier < 3; tier++) {
            int half = PALACE_HALF - tier * 4;
            int ty = y - 1 + tier;
            for (int a = A - half; a <= A + half; a++) {
                for (int b = -half; b <= half; b++) {
                    boolean edge = Math.abs(a - A) == half || Math.abs(b) == half;
                    s.set(a, ty, b, edge ? M.CHISEL : M.JADE);
                    if (edge) {
                        s.set(a, ty + 1, b, M.GOLD);
                    }
                }
            }
            // 台基下方裙边（不收空，只做 3 层）
            for (int a = A - half; a <= A + half; a++) {
                for (int b = -half; b <= half; b++) {
                    for (int k = 1; k <= 3; k++) {
                        s.set(a, ty - k, b, M.SMOOTH);
                    }
                }
            }
        }
        int top = y + 2;                    // 台面（广场）高度
        // 台基下方倒锥（悬浮感）
        for (int k = 0; k < 10; k++) {
            int half = PALACE_HALF - 3 - k * 3;
            if (half <= 0) {
                break;
            }
            for (int a = A - half; a <= A + half; a++) {
                for (int b = -half; b <= half; b++) {
                    if (Math.abs(a - A) == half || Math.abs(b) == half
                            || Math.abs(a - A) + Math.abs(b) > half + (half / 2)) {
                        s.set(a, y - 4 - k, b, (k % 2 == 0) ? M.SMOOTH : M.CLOUD);
                    }
                }
            }
        }
        // ---- ② 广场铺装（金线 + 放射纹）----
        for (int a = A - PALACE_HALF + 1; a <= A + PALACE_HALF - 1; a++) {
            for (int b = -PALACE_HALF + 1; b <= PALACE_HALF - 1; b++) {
                if (!s.isAir(a, top, b)) {
                    continue;               // 已经放了台基/殿身
                }
                boolean line = (a - A) % 8 == 0 || b % 8 == 0;
                s.set(a, top, b, line ? M.GOLD : M.SMOOTH);
            }
        }
        // ---- ③ 丹陛：广场正面向白玉阶开口 ----
        for (int d = A - PALACE_HALF - 6; d <= A - PALACE_HALF + 2; d++) {
            int step = Math.max(0, Math.min(y + 1, y - 1 + (d - (A - PALACE_HALF - 6)) / 2));
            for (int b = -5; b <= 5; b++) {
                s.set(d, step, b, M.CHISEL);
                for (int k = 1; k <= 2; k++) {
                    s.set(d, step - k, b, M.SMOOTH);
                }
            }
        }
        // ---- ④ 主殿：41(side) × 25(along)，墙到 +14，重檐到 +23 ----
        int hallA = A;
        int hallHalfA = 12;
        int hallHalfB = 20;
        for (int a = hallA - hallHalfA; a <= hallA + hallHalfA; a++) {
            for (int b = -hallHalfB; b <= hallHalfB; b++) {
                boolean wallA = Math.abs(a - hallA) == hallHalfA;
                boolean wallB = Math.abs(b) == hallHalfB;
                // 殿身地板
                s.set(a, top, b, M.JADE);
                if (!wallA && !wallB) {
                    continue;               // 殿内空
                }
                // 正门（朝白玉阶：-along 侧）
                boolean door = wallA && a < hallA - hallHalfA + 1 && Math.abs(b) <= 3;
                for (int yy = 1; yy <= 14; yy++) {
                    if (door && yy <= 6) {
                        s.clear(a, top + yy, b, false);
                        continue;
                    }
                    boolean trim = yy == 14 || yy % 7 == 0;
                    s.set(a, top + yy, b, trim ? M.CHISEL : M.JADE);
                }
            }
        }
        // 前廊柱（8 根）
        for (int b = -hallHalfB + 2; b <= hallHalfB - 2; b += 6) {
            for (int a : new int[]{hallA - hallHalfA + 2, hallA + hallHalfA - 2}) {
                for (int yy = 1; yy <= 13; yy++) {
                    s.set(a, top + yy, b, M.PILLAR);
                }
                s.set(a, top + 14, b, M.GOLD);
            }
        }
        // 匾额（正门上方，朝 -along）
        for (int b = -3; b <= 3; b++) {
            s.set(hallA - hallHalfA, top + 12, b, M.GOLD);
        }
        // 重檐
        buildEave(s, hallA, top + 15, hallHalfB + 4, hallHalfA + 4, 11);
        buildEave(s, hallA, top + 19, hallHalfB + 1, hallHalfA + 1, 8);
        // 正脊 + 鸱吻
        for (int b = -hallHalfB - 1; b <= hallHalfB + 1; b++) {
            s.set(hallA, top + 23, b, M.GOLD);
        }
        for (int sgn : new int[]{-1, 1}) {
            s.set(hallA, top + 24, sgn * (hallHalfB + 1), M.GOLD);
            s.set(hallA, top + 25, sgn * (hallHalfB + 1), M.GOLD);
        }
        // ---- ⑤ 双配殿（沿 side 两侧，17×13）----
        for (int sgn : new int[]{-1, 1}) {
            int a0 = A;
            int b0 = sgn * 26;
            for (int a = a0 - 6; a <= a0 + 6; a++) {
                for (int b = b0 - 8; b <= b0 + 8; b++) {
                    boolean wall = Math.abs(a - a0) == 6 || Math.abs(b - b0) == 8;
                    s.set(a, top, b, M.JADE);
                    if (!wall) {
                        continue;
                    }
                    for (int yy = 1; yy <= 9; yy++) {
                        s.set(a, top + yy, b, yy == 9 ? M.CHISEL : M.JADE);
                    }
                }
                s.set(a, top + 10, b0 - 10, M.CHISEL);
                s.set(a, top + 10, b0 + 10, M.CHISEL);
            }
            for (int a = a0 - 7; a <= a0 + 7; a++) {
                for (int b = b0 - 10; b <= b0 + 10; b++) {
                    boolean edge = Math.abs(a - a0) == 7 || Math.abs(b - b0) == 10;
                    if (edge) {
                        s.set(a, top + 11, b, M.CHISEL);
                    }
                }
            }
            for (int b = b0 - 8; b <= b0 + 8; b++) {
                s.set(a0, top + 12, b, M.GOLD);
            }
            // 配殿正门：朝广场（-along 一侧）开一个 2 高门洞 —— 玩家能走进去开箱
            for (int yy = 1; yy <= 2; yy++) {
                s.clear(a0 - 6, top + yy, b0, false);
            }
            // 殿内两盏座灯（贴内墙脚，不挡门、不挡箱）
            s.set(a0 - 4, top + 1, b0 - 5, M.LANTERN);
            s.set(a0 + 4, top + 1, b0 + 5, M.LANTERN);
        }
        // ⑤.1 两座配殿内各藏 1 个天库宝箱（室内地板上方 1 格、殿心，不挡门/不堵路）
        for (int sgn : new int[]{-1, 1}) {
            BlockPos wingChest = new BlockPos(
                    s.worldX(A, sgn * 26), top + 1, s.worldZ(A, sgn * 26));
            if (s.chestWritable(wingChest)
                    && BossRewards.ensureChest(s.level(), wingChest,
                            BossRewards.LOOT_HEAVEN_SUPPLY, s.chestSeed(wingChest))) {
                s.chests++;
            }
        }
        // ---- ⑥ 日晷（广场前部）：φ11 圆台 + 12 时辰金刻 + 晷针 ----
        int dialA = A - 22;
        for (int da = -5; da <= 5; da++) {
            for (int db = -5; db <= 5; db++) {
                int d2 = da * da + db * db;
                if (d2 > 25) {
                    continue;
                }
                s.set(dialA + da, top + 1, db, d2 > 20 ? M.CHISEL : M.SMOOTH);
            }
        }
        for (int i = 0; i < 12; i++) {
            double ang = Math.PI * 2 * i / 12.0;
            int da = (int) Math.round(Math.cos(ang) * 4);
            int db = (int) Math.round(Math.sin(ang) * 4);
            s.set(dialA + da, top + 2, db, M.GOLD);
        }
        for (int yy = 0; yy <= 4; yy++) {
            s.set(dialA, top + 2 + yy, 0, M.DEEP);
        }
        // ---- ⑦ 四角云柱灯 + 金树 ----
        for (int sa : new int[]{-1, 1}) {
            for (int sb : new int[]{-1, 1}) {
                int a = A + sa * 24;
                int b = sb * 26;
                for (int yy = 0; yy <= 5; yy++) {
                    s.set(a, top + yy, b, M.PILLAR);
                }
                s.set(a, top + 6, b, M.LAMP);
            }
        }
        for (int sgn : new int[]{-1, 1}) {
            int a = A - 14;
            int b = sgn * 16;
            for (int yy = 0; yy <= 4; yy++) {
                s.set(a, top + 1 + yy, b, M.CHERRY_LOG);
            }
            for (int da = -3; da <= 3; da++) {
                for (int db = -3; db <= 3; db++) {
                    if (da * da + db * db <= 9) {
                        s.set(a + da, top + 6, b + db, M.CHERRY_LEAVES);
                    }
                }
            }
            s.set(a + 1, top + 5, b + 2, M.CHERRY_LEAVES);
            s.set(a - 2, top + 5, b - 1, M.CHERRY_LEAVES);
        }
        return s.placed - before;
    }

    // ==================================================================
    // 场景 4：日轮（宝殿正上方）
    // ==================================================================

    private static int sunRing(Site s) {
        int before = s.placed;
        s.preloadBox(PALACE_ALONG - 26, -26, PALACE_ALONG + 26, 26);
        int y = palaceFloorY(s.level(), s.baseY, s.front()) + SUN_HEIGHT;
        int cx = s.front().getStepX() * PALACE_ALONG;
        int cz = s.front().getStepZ() * PALACE_ALONG;
        int r = 20;
        // 环体（2 层厚，金/石英/海晶灯相间）
        for (int i = 0; i < 90; i++) {
            double ang = Math.PI * 2 * i / 90.0;
            int x = cx + (int) Math.round(Math.cos(ang) * r);
            int z = cz + (int) Math.round(Math.sin(ang) * r);
            boolean lamp = i % 9 == 0;
            s.setAbs(x, y, z, lamp ? M.LAMP : (i % 2 == 0 ? M.GOLD : M.SMOOTH));
            s.setAbs(x, y + 1, z, lamp ? M.LAMP : M.SMOOTH);
        }
        // 内环镂空 + 12 道金芒
        for (int i = 0; i < 12; i++) {
            double ang = Math.PI * 2 * i / 12.0;
            int dx = (int) Math.round(Math.cos(ang));
            int dz = (int) Math.round(Math.sin(ang));
            for (int k = 8; k <= 18; k++) {
                s.setAbs(cx + dx * k, y, cz + dz * k, k % 3 == 0 ? M.GOLD : M.SMOOTH);
            }
        }
        // 内星盘（半径 8 的 8 角星）
        for (int i = 0; i < 8; i++) {
            double ang = Math.PI * 2 * i / 8.0;
            int dx = (int) Math.round(Math.cos(ang));
            int dz = (int) Math.round(Math.sin(ang));
            for (int k = 1; k <= 8; k++) {
                s.setAbs(cx + dx * k, y, cz + dz * k, k == 8 ? M.GOLD : M.LAMP);
            }
        }
        return s.placed - before;
    }

    // ==================================================================
    // 场景 5：星陨天阶（8 级天阶 + 观星台 + 紫晶）
    // ==================================================================

    private static int starfall(Site s) {
        int before = s.placed;
        Direction sideDir = s.front().getClockWise();      // 垂直方向之一（避开斗台）
        s.preloadDir(sideDir, 38, -14, 128, 14);
        // 8 级天阶：每级 5 深 × 17 宽，逐级 +2
        int y = s.baseY + 2;
        for (int step = 0; step < 8; step++) {
            int d0 = 45 + step * 5;
            int maxT = 0;
            for (int d = d0; d < d0 + 5; d++) {
                for (int b = -8; b <= 8; b += 4) {
                    maxT = Math.max(maxT, s.worldTerrain(s.worldXOn(sideDir, d, b),
                            s.worldZOn(sideDir, d, b)));
                }
            }
            y = Math.max(y + 2, maxT + 2);
            for (int d = d0; d < d0 + 5; d++) {
                for (int b = -8; b <= 8; b++) {
                    int wx = s.worldXOn(sideDir, d, b);
                    int wz = s.worldZOn(sideDir, d, b);
                    s.clearAbove(wx, y + 1, wz, 12);
                    boolean edge = d == d0 || d == d0 + 4 || Math.abs(b) == 8;
                    s.setAbs(wx, y, wz, edge ? M.DARK : (Math.abs(b) % 4 == 0 ? M.CHISEL : M.JADE));
                    s.setAbs(wx, y - 1, wz, M.SMOOTH);
                    if (edge && Math.abs(b) % 4 == 0) {
                        s.setAbs(wx, y + 1, wz, M.AMETHYST);
                    }
                }
            }
            // 侧栏（紫珀）
            for (int d = d0; d < d0 + 5; d++) {
                for (int sgn : new int[]{-1, 1}) {
                    s.setAbs(s.worldXOn(sideDir, d, sgn * 9), y, s.worldZOn(sideDir, d, sgn * 9),
                            M.PURPUR);
                }
            }
        }
        // 观星台（8 级末端，半径 9）
        int topY = y + 4;
        int dTop = 45 + 8 * 5 + 10;
        for (int da = -9; da <= 9; da++) {
            for (int db = -9; db <= 9; db++) {
                if (da * da + db * db > 81) {
                    continue;
                }
                int wx = s.worldXOn(sideDir, dTop + da, db);
                int wz = s.worldZOn(sideDir, dTop + da, db);
                s.clearAbove(wx, topY + 1, wz, 16);
                s.setAbs(wx, topY, wz, (da * da + db * db > 64) ? M.DARK : M.SMOOTH);
            }
        }
        // 4 根方尖碑（17 高，黑曜/紫珀/紫水晶顶 + 末地烛）
        for (int i = 0; i < 4; i++) {
            double ang = Math.PI * 2 * i / 4.0 + Math.PI / 4;
            int da = (int) Math.round(Math.cos(ang) * 6);
            int db = (int) Math.round(Math.sin(ang) * 6);
            for (int h = 1; h <= 17; h++) {
                int wx = s.worldXOn(sideDir, dTop + da, db);
                int wz = s.worldZOn(sideDir, dTop + da, db);
                int yy = topY + h;
                s.setAbs(wx, yy, wz, h % 5 == 0 ? M.PURPUR : M.OBSIDIAN);
            }
            int wx = s.worldXOn(sideDir, dTop + da, db);
            int wz = s.worldZOn(sideDir, dTop + da, db);
            s.setAbs(wx, topY + 18, wz, M.AMETHYST);
            s.setAbs(wx, topY + 19, wz, M.END_ROD);
        }
        // 中央星盘
        for (int i = 0; i < 8; i++) {
            double ang = Math.PI * 2 * i / 8.0;
            int da = (int) Math.round(Math.cos(ang) * 4);
            int db = (int) Math.round(Math.sin(ang) * 4);
            s.setAbs(s.worldXOn(sideDir, dTop + da, db), topY + 1,
                    s.worldZOn(sideDir, dTop + da, db), M.AMETHYST);
        }
        s.setAbs(s.worldXOn(sideDir, dTop, 0), topY + 2, s.worldZOn(sideDir, dTop, 0), M.END_ROD);
        return s.placed - before;
    }

    // ==================================================================
    // 场景 6：云海天池（三池 + 云桥 + 花海）
    // ==================================================================

    private static int pools(Site s) {
        int before = s.placed;
        Direction sideDir = s.front().getCounterClockWise();
        s.preloadDir(sideDir, 34, -20, 104, 20);
        int[][] spec = {{50, 13}, {72, 9}, {94, 6}};
        int[] waterY = new int[spec.length];
        for (int i = 0; i < spec.length; i++) {
            int d = spec[i][0];
            int r = spec[i][1];
            int maxT = 0;
            for (int da = -r; da <= r; da += 3) {
                for (int db = -r; db <= r; db += 3) {
                    maxT = Math.max(maxT, s.worldTerrain(
                            s.worldXOn(sideDir, d + da, db), s.worldZOn(sideDir, d + da, db)));
                }
            }
            int wy = maxT;
            waterY[i] = wy;
            for (int da = -r; da <= r; da++) {
                for (int db = -r; db <= r; db++) {
                    int d2 = da * da + db * db;
                    if (d2 > r * r) {
                        continue;
                    }
                    int wx = s.worldXOn(sideDir, d + da, db);
                    int wz = s.worldZOn(sideDir, d + da, db);
                    boolean rim = d2 > (r - 1) * (r - 1);
                    if (rim) {
                        s.setAbs(wx, wy, wz, M.JADE);
                        s.setAbs(wx, wy + 1, wz, M.CHISEL);
                    } else {
                        s.clearAbove(wx, wy + 1, wz, 8);
                        s.setAbs(wx, wy, wz, M.WATER);
                        s.setAbs(wx, wy - 1, wz, M.SMOOTH);
                        s.setAbs(wx, wy - 2, wz, M.JADE);
                        if (d2 < (r - 3) * (r - 3) && (da + db) % 7 == 0) {
                            s.setAbs(wx, wy + 1, wz, M.LILY);
                        }
                    }
                }
            }
            // 池边灯柱（四正）
            for (int k = 0; k < 4; k++) {
                double ang = Math.PI * 2 * k / 4.0;
                int da = (int) Math.round(Math.cos(ang) * (r + 2));
                int db = (int) Math.round(Math.sin(ang) * (r + 2));
                int wx = s.worldXOn(sideDir, d + da, db);
                int wz = s.worldZOn(sideDir, d + da, db);
                s.setAbs(wx, wy + 1, wz, M.PILLAR);
                s.setAbs(wx, wy + 2, wz, M.LAMP);
            }
        }
        // 云桥：池 1 → 池 2 → 池 3（3 宽白玻璃，架在水面上方 1 格）
        for (int i = 0; i < spec.length - 1; i++) {
            int d0 = spec[i][0];
            int d1 = spec[i + 1][0];
            int y = Math.max(waterY[i], waterY[i + 1]) + 1;
            for (int d = d0 + spec[i][1] - 1; d <= d1 - spec[i + 1][1] + 1; d++) {
                for (int db = -1; db <= 1; db++) {
                    s.setAbs(s.worldXOn(sideDir, d, db), y,
                            s.worldZOn(sideDir, d, db), M.GLASS);
                }
            }
        }
        // 花海：池边 40 簇粉花瓣
        RandomSource rnd = RandomSource.create(SEED + 41);
        for (int i = 0; i < 40; i++) {
            int d = 45 + rnd.nextInt(55);
            int db = -20 + rnd.nextInt(40);
            int wx = s.worldXOn(sideDir, d, db);
            int wz = s.worldZOn(sideDir, d, db);
            int ty = s.worldTerrain(wx, wz);
            if (s.level().isLoaded(new BlockPos(wx, ty, wz))
                    && s.level().getBlockState(new BlockPos(wx, ty, wz)).is(Blocks.GRASS_BLOCK)) {
                s.setAbs(wx, ty + 1, wz, M.PETAL);
            }
        }
        return s.placed - before;
    }

    // ==================================================================
    // 场景 7~9：云海浮岛（12 座，分 3 批）
    // ==================================================================

    private static int islands(Site s, int from) {
        int before = s.placed;
        RandomSource rnd = RandomSource.create(SEED + 7 + from);
        for (int i = from; i < from + 4 && i < 12; i++) {
            // 极角分布：避开正面轴 ±30° 与斗台方向 ±45°
            int quad = i % 4;
            double ang = Math.toRadians(quad * 90 + 25 + (i / 4) * 12 + rnd.nextInt(10));
            int r = 95 + (i % 3) * 30 + rnd.nextInt(25);
            int cx = (int) Math.round(Math.cos(ang) * r);
            int cz = (int) Math.round(Math.sin(ang) * r);
            int y = s.baseY + 14 + (i % 4) * 9 + rnd.nextInt(6);
            y = Math.max(y, s.worldTerrain(cx, cz) + 8);
            int rad = 6 + rnd.nextInt(5);
            s.preloadWorld(cx - rad - 6, cz - rad - 6, cx + rad + 6, cz + rad + 6);
            buildIsland(s, cx, y, cz, rad, i % 4 == 1, rnd);
        }
        return s.placed - before;
    }

    private static void buildIsland(Site s, int cx, int y, int cz, int rad, boolean pavilion,
                                    RandomSource rnd) {
        // 岛面（草 + 玉边）
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > rad * rad) {
                    continue;
                }
                s.clearAbove(cx + dx, y + 1, cz + dz, 14);
                boolean edge = d2 > (rad - 1) * (rad - 1);
                s.setAbs(cx + dx, y, cz + dz, edge ? M.JADE : M.GRASS);
                s.setAbs(cx + dx, y - 1, cz + dz, (d2 > (rad - 2) * (rad - 2)) ? M.JADE : M.DIRT);
            }
        }
        // 岛底倒锥（黑曜/末地石，收成 1 格）
        for (int k = 1; k <= rad + 4; k++) {
            int rr = rad - k;
            if (rr <= 0) {
                rr = 1;
            }
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (dx * dx + dz * dz <= rr * rr) {
                        s.setAbs(cx + dx, y - 2 - k, cz + dz,
                                (k % 2 == 0) ? M.OBSIDIAN : M.ENDSTONE);
                    }
                }
            }
            if (rr == 1) {
                break;
            }
        }
        // 岛面小景：1~2 棵樱花树
        int treeX = cx + rnd.nextInt(5) - 2;
        int treeZ = cz + rnd.nextInt(5) - 2;
        tree(s, treeX, y + 1, treeZ, rnd);
        if (rnd.nextBoolean()) {
            tree(s, cx + rnd.nextInt(7) - 3, y + 1, cz + rnd.nextInt(7) - 3, rnd);
        }
        // 云帘（边缘垂 2~4 格白羊毛）
        for (int k = 0; k < 6; k++) {
            double a = Math.PI * 2 * k / 6.0;
            int dx = (int) Math.round(Math.cos(a) * (rad - 1));
            int dz = (int) Math.round(Math.sin(a) * (rad - 1));
            int h = 2 + rnd.nextInt(3);
            for (int yy = 1; yy <= h; yy++) {
                s.setAbs(cx + dx, y - 2 - yy, cz + dz, M.CLOUD);
            }
        }
        // 小亭（玉柱 + 四角攒尖顶 + 石桌）
        if (pavilion) {
            int px = cx;
            int pz = cz;
            int py = y + 1;
            for (int sx : new int[]{-2, 2}) {
                for (int sz : new int[]{-2, 2}) {
                    for (int yy = 0; yy <= 3; yy++) {
                        s.setAbs(px + sx, py + yy, pz + sz, M.PILLAR);
                    }
                }
            }
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                    if (edge) {
                        s.setAbs(px + dx, py + 4, pz + dz, M.CHISEL);
                    }
                    if (Math.abs(dx) + Math.abs(dz) <= 2) {
                        s.setAbs(px + dx, py + 5, pz + dz, M.GOLD);
                    }
                }
            }
            s.setAbs(px, py + 1, pz, M.SLAB);
            s.setAbs(px, py, pz, M.PILLAR);
            // 亭内藏 1 个天库宝箱：室内地板上方 1 格；避开樱花树干（最多占 1 个候选格）
            for (int[] c : new int[][]{{-1, -1}, {1, 1}, {-1, 1}, {1, -1}}) {
                BlockPos pavChest = new BlockPos(px + c[0], py, pz + c[1]);
                if (!s.level().getBlockState(pavChest).isAir()) {
                    continue;                       // 被树干/花叶占着，换下一格
                }
                if (s.chestWritable(pavChest)
                        && BossRewards.ensureChest(s.level(), pavChest,
                                BossRewards.LOOT_HEAVEN_SUPPLY, s.chestSeed(pavChest))) {
                    s.chests++;
                }
                break;
            }
        }
    }

    private static void tree(Site s, int x, int y, int z, RandomSource rnd) {
        int h = 4 + rnd.nextInt(3);
        for (int i = 0; i < h; i++) {
            s.setAbs(x, y + i, z, M.CHERRY_LOG);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (dx * dx + dz * dz + dy * dy <= 5) {
                        s.setAbs(x + dx, y + h - 1 + dy, z + dz, M.CHERRY_LEAVES);
                    }
                }
            }
        }
    }

    // ==================================================================
    // 场景 10：云鹤栖台（2 座高柱巢台，给飞行怪一个家）
    // ==================================================================

    private static int cranePerches(Site s) {
        int before = s.placed;
        Direction sideDir = s.front().getClockWise();
        for (int sgn : new int[]{-1, 1}) {
            int d = 62;
            int b = sgn * 32;
            int wx = s.worldXOn(sideDir, d, b);
            int wz = s.worldZOn(sideDir, d, b);
            int ground = s.worldTerrain(wx, wz);
            s.preloadWorld(wx - 8, wz - 8, wx + 8, wz + 8);
            int top = Math.max(ground, s.baseY) + 26;
            for (int yy = ground + 1; yy <= top; yy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        s.setAbs(wx + dx, yy, wz + dz, M.PILLAR);
                    }
                }
            }
            // 巢台 7×7
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                    s.setAbs(wx + dx, top + 1, wz + dz, edge ? M.JADE : M.HAY);
                }
            }
            // 巢边灯 + 骨饰
            for (int sx : new int[]{-3, 3}) {
                for (int sz : new int[]{-3, 3}) {
                    s.setAbs(wx + sx, top + 2, wz + sz, M.LAMP);
                }
            }
            s.setAbs(wx, top + 2, wz, M.BONE);
            s.setAbs(wx + 1, top + 2, wz, M.BONE);
            s.setAbs(wx - 1, top + 2, wz - 1, M.BONE);
        }
        return s.placed - before;
    }

    // ==================================================================
    // 场景 11：凌霄斗台外围装饰（斗台 half=15；只写 |dx|,|dz| > 16）
    // ==================================================================

    private static int arenaDecor(Site s, HeavenRealm.HeavenFlag realm) {
        int before = s.placed;
        if (!realm.arenaBuilt) {
            return 0;
        }
        int cx = realm.arenaCenterX;
        int cz = realm.arenaCenterZ;
        int cy = realm.arenaFloorY;
        s.preloadWorld(cx - 30, cz - 30, cx + 30, cz + 30);
        // 四角云柱（对角 24 格，12 高）
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int px = cx + sx * 24;
                int pz = cz + sz * 24;
                for (int h = -6; h <= 8; h++) {
                    s.setAbs(px, cy + h, pz, M.PILLAR);
                }
                s.setAbs(px, cy + 9, pz, M.GOLD);
                s.setAbs(px, cy + 10, pz, M.LAMP);
                s.setAbs(px, cy - 7, pz, M.CLOUD);
            }
        }
        // 白云环（r=20~22，台面下 2 格，不碰台面）
        for (int dx = -22; dx <= 22; dx++) {
            for (int dz = -22; dz <= 22; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 < 20 * 20 || d2 > 22 * 22) {
                    continue;
                }
                if (Math.max(Math.abs(dx), Math.abs(dz)) <= ARENA_DECOR_GAP) {
                    continue;               // 安全冗余：绝不进斗台 footprint
                }
                s.setAbs(cx + dx, cy - 2, cz + dz,
                        Math.floorMod(dx + dz, 10) == 0 ? M.GLASS : M.CLOUD);
                s.setAbs(cx + dx, cy - 3, cz + dz, M.CLOUD);
            }
        }
        // 四面玉碑（离边 6 格，2 高 + 金顶）
        for (int k = 0; k < 4; k++) {
            int dx = k == 0 ? 21 : (k == 1 ? -21 : 0);
            int dz = k == 2 ? 21 : (k == 3 ? -21 : 0);
            for (int h = 0; h <= 2; h++) {
                s.setAbs(cx + dx, cy + h, cz + dz, M.JADE);
            }
            s.setAbs(cx + dx, cy + 3, cz + dz, M.GOLD);
            s.setAbs(cx + dx, cy + 4, cz + dz, M.LAMP);
        }
        // ---- 斗台「天库」（v2，任务 D4）：斗台两侧外圈各 1 座 5×5 白玉石台 ----
        // 斗台 31×31（half=15）：天库整块落在 footprint 之外（侧向 16..20），
        // 且沿"与桥/入口垂直"的一侧铺 —— 绝不挡桥、不挡斗台入口。
        Direction f = s.front();                          // 桥/入口朝向（origin 一侧）
        Direction sideDir = s.side();                     // 与桥垂直的一侧
        for (int sgn : new int[]{-1, 1}) {
            // 5×5 云纹台面：沿 front 轴 -2..2，侧向 16..20（紧贴斗台边缘，y=斗台楼板层）
            for (int da = -2; da <= 2; da++) {
                for (int ds = 16; ds <= 20; ds++) {
                    int wx = cx + f.getStepX() * da + sideDir.getStepX() * ds * sgn;
                    int wz = cz + f.getStepZ() * da + sideDir.getStepZ() * ds * sgn;
                    boolean outer = ds == 20;
                    boolean goldLine = da == 0 || ds == 18;
                    BlockState st = outer
                            ? (((da & 1) == 0) ? M.CHISEL : M.GOLD)
                            : (goldLine ? M.GOLD : (Math.abs(da) == 2 ? M.CHISEL : M.SMOOTH));
                    s.setAbs(wx, cy, wz, st);
                    s.setAbs(wx, cy - 1, wz, M.CLOUD);     // 台底云垫（与斗台白云环同风格）
                }
            }
            // 天库宝箱：台面靠斗台一侧、正中（y = 楼板 +1 = 站立格）
            int chestX = cx + sideDir.getStepX() * 17 * sgn;
            int chestZ = cz + sideDir.getStepZ() * 17 * sgn;
            BlockPos arenaChest = new BlockPos(chestX, cy + 1, chestZ);
            if (s.chestWritable(arenaChest)
                    && BossRewards.ensureChest(s.level(), arenaChest,
                            BossRewards.LOOT_HEAVEN_SUPPLY, s.chestSeed(arenaChest))) {
                s.chests++;
            }
            // 天兵雕像 ×1（3 格高：石座 + 金甲 + 玉盔，头顶红缨），站在石台后部
            int stX = cx + sideDir.getStepX() * 19 * sgn;
            int stZ = cz + sideDir.getStepZ() * 19 * sgn;
            s.setAbs(stX, cy + 1, stZ, M.SMOOTH);         // 石座
            s.setAbs(stX, cy + 2, stZ, M.GOLD);           // 金甲
            s.setAbs(stX, cy + 3, stZ, M.CHISEL);         // 玉盔
            s.setAbs(stX, cy + 4, stZ, M.RED_WOOL);       // 红缨
            // 天兵长戟（铁栏杆柱，高过头顶）
            for (int h = 1; h <= 4; h++) {
                s.setAbs(stX + sideDir.getStepX() * sgn, cy + h,
                        stZ + sideDir.getStepZ() * sgn, M.IRON_BARS);
            }
            // 仙庭旗帜 ×2：石台后两角（玉旗杆 + 浅蓝仙旗）
            for (int da : new int[]{-2, 2}) {
                int bx = cx + f.getStepX() * da + sideDir.getStepX() * 20 * sgn;
                int bz = cz + f.getStepZ() * da + sideDir.getStepZ() * 20 * sgn;
                s.setAbs(bx, cy + 1, bz, M.PILLAR);
                s.setAbs(bx, cy + 2, bz, M.BANNER);
            }
            // 海晶灯 ×2：石台前两角（矮柱 + 海晶灯）
            for (int da : new int[]{-2, 2}) {
                int lx = cx + f.getStepX() * da + sideDir.getStepX() * 16 * sgn;
                int lz = cz + f.getStepZ() * da + sideDir.getStepZ() * 16 * sgn;
                s.setAbs(lx, cy + 1, lz, M.PILLAR);
                s.setAbs(lx, cy + 2, lz, M.LAMP);
            }
        }
        return s.placed - before;
    }

    // ==================================================================
    // 高度/方向计算
    // ==================================================================

    /** 宝殿台基高度：取宝殿足迹内地形最高点 + 2，且至少比登天台高一截。 */
    private static int palaceFloorY(ServerLevel level, int baseY, Direction front) {
        int max = level.getMinBuildHeight();
        int cx = front.getStepX() * PALACE_ALONG;
        int cz = front.getStepZ() * PALACE_ALONG;
        for (int a = -PALACE_HALF; a <= PALACE_HALF; a += 4) {
            for (int b = -PALACE_HALF; b <= PALACE_HALF; b += 4) {
                max = Math.max(max, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        cx + a, cz + b));
            }
        }
        return Math.max(baseY + 6, max + 2);
    }

    private static Direction opposite(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.WEST : Direction.EAST;
        }
        return dz >= 0 ? Direction.NORTH : Direction.SOUTH;
    }

    /**
     * 斗台未建时预测"斗台会在哪一侧"，然后取反。
     * <p>规则与 {@link HeavenRealm#ensureArena} 完全一致：四个正方向各取一次
     * 距离 24 处的地表高度，取最低（相同高度按 EAST/WEST/SOUTH/NORTH 顺序）。</p>
     */
    private static Direction oppositePredicted(ServerLevel level) {
        Direction best = Direction.EAST;
        int bestH = Integer.MAX_VALUE;
        for (Direction dir : CARDINALS) {
            BlockPos probe = new BlockPos(dir.getStepX() * 24, 0, dir.getStepZ() * 24);
            level.getChunk(probe.getX() >> 4, probe.getZ() >> 4,
                    ChunkStatus.FULL, true);            // 预测前必须真的加载，否则高度是底部
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    probe.getX(), probe.getZ());
            if (h < bestH) {
                bestH = h;
                best = dir;
            }
        }
        return best.getOpposite();
    }

    // ==================================================================
    // SavedData
    // ==================================================================

    private static SceneFlag sceneFlag(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SceneFlag::load, SceneFlag::new, DATA_KEY);
    }

    /** 天外天场景进度（维度级）：只记版本、已铺的块位、已放出的探索宝箱数。 */
    public static final class SceneFlag extends SavedData {
        int version;
        int builtMask;
        /** 累计放出的天外天探索宝箱数（版本重铺时清零；只用于日志/交接核对）。 */
        int chests;

        static SceneFlag load(CompoundTag tag) {
            SceneFlag flag = new SceneFlag();
            flag.version = tag.getInt("version");
            flag.builtMask = tag.getInt("built_mask");
            flag.chests = tag.getInt("chests");
            return flag;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("version", version);
            tag.putInt("built_mask", builtMask);
            tag.putInt("chests", chests);
            return tag;
        }
    }

    /** 读 HeavenRealm 的维度级标记（同包可访问；只读，不写）。 */
    private static HeavenRealm.HeavenFlag realmFlag(ServerLevel level) {
        try {
            return level.getDataStorage().computeIfAbsent(
                    HeavenRealm.HeavenFlag::load, HeavenRealm.HeavenFlag::new, REALM_DATA_KEY);
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[heaven-scene] 读取 HeavenFlag 失败：{}", t.toString());
            return null;
        }
    }

    // ==================================================================
    // 写方块的小工具（唯一的写入口：边界/加载/计数都在这里）
    // ==================================================================

    /** 每种材料只在这里取一次默认 state（性能 + 一致性）。 */
    private static final class M {
        static final BlockState JADE = Blocks.QUARTZ_BLOCK.defaultBlockState();
        static final BlockState SMOOTH = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        static final BlockState CHISEL = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        static final BlockState PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
        static final BlockState SLAB = Blocks.QUARTZ_SLAB.defaultBlockState();
        static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
        static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();
        static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
        static final BlockState CLOUD = Blocks.WHITE_WOOL.defaultBlockState();
        static final BlockState WOOL = Blocks.WHITE_WOOL.defaultBlockState();
        static final BlockState GLASS = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        static final BlockState HAY = Blocks.HAY_BLOCK.defaultBlockState();
        static final BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState();
        static final BlockState CAMPFIRE = Blocks.CAMPFIRE.defaultBlockState();
        static final BlockState IRON_BARS = Blocks.IRON_BARS.defaultBlockState();
        static final BlockState RED_WOOL = Blocks.RED_WOOL.defaultBlockState();
        static final BlockState BANNER = Blocks.LIGHT_BLUE_BANNER.defaultBlockState();
        static final BlockState DARK = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        static final BlockState DEEP = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
        static final BlockState ENDSTONE = Blocks.END_STONE.defaultBlockState();
        static final BlockState PURPUR = Blocks.PURPUR_BLOCK.defaultBlockState();
        static final BlockState AMETHYST = Blocks.AMETHYST_BLOCK.defaultBlockState();
        static final BlockState END_ROD = Blocks.END_ROD.defaultBlockState();
        static final BlockState WATER = Blocks.WATER.defaultBlockState();
        static final BlockState LILY = Blocks.LILY_PAD.defaultBlockState();
        static final BlockState PETAL = Blocks.PINK_PETALS.defaultBlockState();
        static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
        static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
        static final BlockState CHERRY_LOG = Blocks.CHERRY_LOG.defaultBlockState();
        static final BlockState CHERRY_LEAVES = Blocks.CHERRY_LEAVES.defaultBlockState();
    }

    /**
     * 场景施工坐标助手：相对登天台中心，把 (along, side) 换成世界坐标，
     * 并保证"不写登天台/斗台 footprint、只写加载中的区块、越界拒写"。
     */
    private static final class Site {
        private final ServerLevel level;
        private final int baseY;
        private final Direction front;
        private final Direction side;
        /** 斗台信息一次性缓存（避免每格都去查 SavedData）。 */
        private final boolean arenaBuilt;
        private final int arenaCX;
        private final int arenaCZ;
        private final int arenaFloorY;
        int placed;
        int cleared;
        int skippedUnloaded;
        int rejected;
        /** 本块施工真正新放出的天外天探索宝箱数（已存在而跳过的幂等箱不计）。 */
        int chests;

        Site(ServerLevel level, int baseY, Direction front) {
            this.level = level;
            this.baseY = baseY;
            this.front = front;
            this.side = front.getClockWise();
            HeavenRealm.HeavenFlag realm = realmFlag(level);
            this.arenaBuilt = realm != null && realm.arenaBuilt;
            this.arenaCX = realm == null ? 0 : realm.arenaCenterX;
            this.arenaCZ = realm == null ? 0 : realm.arenaCenterZ;
            this.arenaFloorY = realm == null ? 0 : realm.arenaFloorY;
        }

        ServerLevel level() {
            return level;
        }

        int baseY() {
            return baseY;
        }

        Direction front() {
            return front;
        }

        Direction side() {
            return side;
        }

        int worldX(int along, int sideOffset) {
            return front.getStepX() * along + side.getStepX() * sideOffset;
        }

        int worldZ(int along, int sideOffset) {
            return front.getStepZ() * along + side.getStepZ() * sideOffset;
        }

        /** 以任意方向 dir 为 along、side 为垂直的分量组合出世界 X。 */
        int worldXOn(Direction dir, int along, int sideOffset) {
            return dir.getStepX() * along + dir.getClockWise().getStepX() * sideOffset;
        }

        int worldZOn(Direction dir, int along, int sideOffset) {
            return dir.getStepZ() * along + dir.getClockWise().getStepZ() * sideOffset;
        }

        /** 写一格（along/side 版）。 */
        void set(int along, int y, int sideOffset, BlockState state) {
            setAbs(worldX(along, sideOffset), y, worldZ(along, sideOffset), state);
        }

        /** 清一格（along/side 版）。 */
        void clear(int along, int y, int sideOffset, boolean force) {
            clearAbs(worldX(along, sideOffset), y, worldZ(along, sideOffset));
        }

        /** 世界坐标写一格：越界/未加载/已在保护范围 → 拒绝并计数。 */
        void setAbs(int x, int y, int z, BlockState state) {
            if (protectedCell(x, y, z)) {
                rejected++;
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()
                    || !level.isLoaded(pos)) {
                skippedUnloaded++;
                return;
            }
            if (level.getBlockState(pos).equals(state)) {
                return;
            }
            level.setBlock(pos, state, 2);
            placed++;
        }

        /** 世界坐标清一格（只清非空气）。 */
        void clearAbs(int x, int y, int z) {
            if (protectedCell(x, y, z)) {
                rejected++;
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()
                    || !level.isLoaded(pos)) {
                skippedUnloaded++;
                return;
            }
            if (level.getBlockState(pos).isAir()) {
                return;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            cleared++;
        }

        /**
         * 从 <b>(x,y,z) 开始</b>往上清 height 格（含 y 自身；凿开地形/树，给建筑腾空）。
         *
         * <p>⚠ 语义修正（“白玉天阶台阶错位”的直接原因）：旧实现从 y+1 开始清，
         * 而所有调用方都按“从 y 开始清”的语义传参（例如传 floorY+1 想清头顶一格），
         * 结果 <b>紧贴台阶上方那一格永远没被清</b> —— 地形比台阶高时就会在台阶正上方
         * 留一层泥土/草方块，玩家看到的就是“台阶被错位/堵住的方块”。</p>
         */
        void clearAbove(int x, int y, int z, int height) {
            for (int k = 0; k < height; k++) {
                clearAbs(x, y + k, z);
            }
        }

        /** 该格是不是"功能骨架保护区"：登天台 ±7 / 斗台 ±15 / 两台之间的连桥走廊。 */
        boolean protectedCell(int x, int y, int z) {
            // 登天台：只保护台面及上下 6 格（场景可以围着它做栏杆/云柱）
            if (Math.abs(x) <= PLATFORM_HALF && Math.abs(z) <= PLATFORM_HALF
                    && y >= baseY - 5 && y <= baseY + 6) {
                return true;
            }
            if (!arenaBuilt) {
                return false;
            }
            int dx = Math.abs(x - arenaCX);
            int dz = Math.abs(z - arenaCZ);
            // 斗台 footprint ±15（含楼板上下 3 格）；装饰块自己会避开 16 以内
            if (dx <= ARENA_HALF && dz <= ARENA_HALF
                    && y >= arenaFloorY - 3 && y <= arenaFloorY + 14) {
                return true;
            }
            // 连桥走廊：斗台中心与原点之间 3 格宽直线
            if (onBridge(x, z)
                    && y >= Math.min(arenaFloorY, baseY) - 2
                    && y <= Math.max(arenaFloorY, baseY) + 8) {
                return true;
            }
            return false;
        }

        private boolean onBridge(int x, int z) {
            if (Math.abs(arenaCZ) <= 1) {
                return Math.abs(z - arenaCZ) <= 2 && between(x, 0, arenaCX);
            }
            if (Math.abs(arenaCX) <= 1) {
                return Math.abs(x - arenaCX) <= 2 && between(z, 0, arenaCZ);
            }
            return false;
        }

        /**
         * 本次施工允许新加载的区块数（{@link Integer#MAX_VALUE} = 不限制）。
         *
         * <p>只给 {@link #kick} 用；{@code ensureAll} 是调试/救档入口，保持同步铺完。</p>
         */
        int chunkBudget = Integer.MAX_VALUE;

        /**
         * 加载一段区块；预算用完且还有“未加载”区块时抛 {@link SceneDefer}。
         *
         * <p>只统计<b>尚未加载</b>的区块：已经加载的区块不花预算，
         * 所以重试/重铺时开销很小。</p>
         */
        private void preloadBudgeted(int minX, int minZ, int maxX, int maxZ) {
            int cx0 = minX >> 4;
            int cz0 = minZ >> 4;
            int cx1 = maxX >> 4;
            int cz1 = maxZ >> 4;
            for (int cx = cx0; cx <= cx1; cx++) {
                for (int cz = cz0; cz <= cz1; cz++) {
                    if (!level.hasChunk(cx, cz)) {
                        if (chunkBudget <= 0) {
                            throw SceneDefer.INSTANCE;
                        }
                        chunkBudget--;
                    }
                    level.getChunk(cx, cz, ChunkStatus.FULL, true);
                }
            }
        }

        /** 加载本场景块用到的区块（along/side 矩形 + 四周 1 区块余量）。 */
        void preloadBox(int a0, int b0, int a1, int b1) {
            preloadDir(front, a0, b0, a1, b1);
        }

        /** 按任意 dir 定义的 (along, side) 矩形预载区块。 */
        void preloadDir(Direction dir, int a0, int b0, int a1, int b1) {
            int x1 = worldXOn(dir, a0, b0);
            int z1 = worldZOn(dir, a0, b0);
            int x2 = worldXOn(dir, a1, b1);
            int z2 = worldZOn(dir, a1, b1);
            int x3 = worldXOn(dir, a0, b1);
            int z3 = worldZOn(dir, a0, b1);
            int x4 = worldXOn(dir, a1, b0);
            int z4 = worldZOn(dir, a1, b0);
            int minX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
            int maxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
            int minZ = Math.min(Math.min(z1, z2), Math.min(z3, z4));
            int maxZ = Math.max(Math.max(z1, z2), Math.max(z3, z4));
            preloadWorld(minX, minZ, maxX, maxZ);
        }

        void preloadWorld(int minX, int minZ, int maxX, int maxZ) {
            preloadBudgeted(minX - 16, minZ - 16, maxX + 16, maxZ + 16);
        }

        private boolean between(int v, int a, int b) {
            return v >= Math.min(a, b) && v <= Math.max(a, b);
        }

        int terrainHeightAt(int x, int z) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }

        int worldTerrain(int x, int z) {
            return terrainHeightAt(x, z);
        }

        /** 相对字面量：该格是否空气（广场铺装用）。 */
        boolean isAir(int along, int y, int sideOffset) {
            return level.getBlockState(new BlockPos(worldX(along, sideOffset), y,
                    worldZ(along, sideOffset))).isAir();
        }

        // ---- 天外天探索宝箱（统一走 dungeon/BossRewards.ensureChest）----

        /** 宝箱 loot 的固定种子：同一坐标可复现（首开战利品一致，便于验收/回档）。 */
        long chestSeed(BlockPos pos) {
            return SEED + pos.asLong() * 31L;
        }

        /**
         * 宝箱落点预检（不写地图）：越界/功能骨架保护区/未加载都不放。
         *
         * <p>真正的放箱与幂等由 {@link BossRewards#ensureChest} 负责（只放空气格，
         * 已存在的容器跳过——所以旧存档重铺不会把玩家开过的箱子刷新）。
         * 返回 true 时调用方再调 {@code BossRewards.ensureChest}，成功则 {@link #chests}++。</p>
         */
        boolean chestWritable(BlockPos pos) {
            if (protectedCell(pos.getX(), pos.getY(), pos.getZ())) {
                rejected++;
                return false;
            }
            if (!level.isLoaded(pos)) {
                skippedUnloaded++;
                return false;
            }
            return true;
        }
    }

    /**
     * 本 tick 的区块预载预算用完：本次场景块施工先暂停，下一 tick 继续。
     *
     * <p>单例 + 不建栈：这是正常的控制流（不是异常），高频抛出也不能有栈开销。</p>
     */
    private static final class SceneDefer extends RuntimeException {
        static final SceneDefer INSTANCE = new SceneDefer();

        private SceneDefer() {
            super(null, null, false, false);
        }
    }

    /** 沿 front 方向按 d 取地形高度（白玉阶用；带 ±半宽采样）。 */
    private static int terrainHeight(ServerLevel level, Site s, int d) {
        int max = level.getMinBuildHeight();
        for (int side = -AVENUE_HALF; side <= AVENUE_HALF; side += 3) {
            max = Math.max(max, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    s.worldX(d, side), s.worldZ(d, side)));
        }
        return max;
    }

    // ==================================================================
    // 区块预载（每个场景块只加载自己的包围盒）
    // ==================================================================

    /** 加载 [minX,minZ]-[maxX,maxZ] 之间的区块（给一个场景块施工前调）。 */
    private static int preload(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        int cx0 = minX >> 4;
        int cz0 = minZ >> 4;
        int cx1 = maxX >> 4;
        int cz1 = maxZ >> 4;
        int n = 0;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                level.getChunk(cx, cz, ChunkStatus.FULL, true);
                n++;
            }
        }
        return n;
    }

    /** 给调试/交接用：列出本场景会碰到的区块数量（不做加载）。 */
    public static int estimateChunks(ServerLevel level) {
        Direction f = frontDirection(level);
        int minX = Math.min(-200, f.getStepX() * 200);
        int maxX = Math.max(200, f.getStepX() * 200);
        int minZ = Math.min(-200, f.getStepZ() * 200);
        int maxZ = Math.max(200, f.getStepZ() * 200);
        return ((maxX >> 4) - (minX >> 4) + 1) * ((maxZ >> 4) - (minZ >> 4) + 1);
    }

    /** 供 {@link HeavenRealm} / 怪物侧判断"这个 level 是不是天外天"（避免跨包引用它的私有常量）。 */
    public static boolean isHeaven(Level level) {
        return HeavenRealm.isIn(level);
    }

    /** 预载工具给 {@link #ensureAll} 的"整体范围"用（不做逐块预载时的兜底）。 */
    private static int preloadAll(ServerLevel level) {
        Direction f = frontDirection(level);
        int reach = 200;
        int minX = Math.min(-reach, f.getStepX() * reach);
        int maxX = Math.max(reach, f.getStepX() * reach);
        int minZ = Math.min(-reach, f.getStepZ() * reach);
        int maxZ = Math.max(reach, f.getStepZ() * reach);
        return preload(level, minX - 16, minZ - 16, maxX + 16, maxZ + 16);
    }
}
