package com.hongmeng.xiuxian.realworld;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.block.EvilAltarBlockEntity;
import com.hongmeng.xiuxian.entity.EvilGazerEntity;
import com.hongmeng.xiuxian.network.CinematicPacket;
import com.hongmeng.xiuxian.network.EvilFallPacket;
import com.hongmeng.xiuxian.network.EvilFlashPacket;
import com.hongmeng.xiuxian.network.EvilSkyPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import com.hongmeng.xiuxian.technique.ParticleBatch;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <b>「恶」的祭坛召唤演出</b>（17 号契约 §3，唯一触发仍是"祭坛召唤"）。
 *
 * <h2>入口（一个字节都不许改）</h2>
 * <pre>
 *  五凭证 → 恶之祭坛 5 格（{@link EvilAltarBlockEntity#ready()}）
 *  → 客户端「召唤」按钮 → {@code EvilSummonPacket}
 *  → 服务端再校验 {@code ready()} → {@code EvilSummon.begin(...)}
 * </pre>
 * <p>本类只负责 <b>begin 之后</b>的整段演出与兜底；不改入口、不改别的技能/模型。</p>
 *
 * <h2>七步时间轴（总 {@value #SHOW_TICKS}t）</h2>
 * <ol>
 *   <li><b>T0 祭坛校验 / 消耗 / 感染 / 火盆 / 光柱</b>：服务端再次确认五格 → 建立演出会话
 *       → <b>演出确认开始后才消耗五格</b>（失败/取消按兜底退还）→ 分层感染（内圈腐肉、中圈
 *       血红石、外圈骨块/菌土，只动天然方块、绝不碰祭坛）→ 四周点亮火盆 → 直冲天顶的光柱
 *       + 低鸣；</li>
 *   <li><b>T0~40 血红天</b>：{@link EvilSky} 强度 0→1 逐 tick 上涨（同时是裂口张开度），
 *       每 4t 全维度同步；<b>T20 起全屏闪烁 3 次（间隔 8t）+ 音效</b>；</li>
 *   <li><b>T40 天裂 + 巨眼</b>：强度拉到 1.0、{@link CinematicPacket} 仰视运镜（60t）、
 *       「§c你被恶看见了」；</li>
 *   <li><b>T50 坠落过场</b>：{@link EvilFallPacket} 播 {@value #FALL_TICKS}t（≥50t），
 *       客户端相机被拽上天 + 气流 / 风压；</li>
 *   <li><b>T90 高天战场</b>：{@link EvilArena#ensure}（幂等建场）→ 把<b>召唤者 + 祭坛 64 格内
 *       同维度、非旁观玩家</b>一起拉上台面外圈 6~10 格（朝向恶，各记回城点）→
 *       {@link EvilArena#spawnGazer} 把「恶」刷在<b>台面中心正上方 45 格</b>（阶段 1、无重力、
 *       无 AI、无敌）；</li>
 *   <li><b>T90~130 降临动画</b>：40t 里每 tick 从上方压下来 + 粒子柱/闪电/吼叫，
 *       <b>终点是台面上方 {@link EvilArena#HOVER_ABOVE} 格的悬停点</b>（不是台面！）——
 *       一阶段只有一条手臂，悬空后正好从"天空裂缝"垂到台面附近；
 *       T130 解除无敌与 AI 锁、播报「§4§l恶 · 降临」；</li>
 *   <li><b>T130~200 收尾</b>：天空稳定在 0.9（避开客户端 FxWatchdog 的"满强度"强清阈值），
 *       阶段 1 保持、「恶」保持悬空（本类每 20t 兜底夹回 {@link EvilArena#hoverY}，
 *       战斗组每 tick 也会夹）；T200 会话结束，进入正常 Boss 战。</li>
 * </ol>
 *
 * <h2>失败兜底（§3.8）</h2>
 * <ul>
 *   <li>演出中（T0~130，未成功）玩家<b>死亡 / 下线 / 换维度 / 被远程拉走</b> →
 *       {@link #cancel(ServerLevel, String, Show)}：撤掉已刷出的「恶」、{@link EvilSky#clear()}
 *       + 全维度同步、把<b>所有被拉进裂隙的参与者</b>送回各自记录点、<b>退还祭坛五格</b>、WARN 日志；</li>
 *   <li>建场 / 刷怪 / 传送失败同样走 cancel（不吞物品）；</li>
 *   <li>{@code RealWorldScheduler} 排的演出音效全部包一层 {@link #isCurrent(Show)} 守卫：
 *       取消后 token 失效、任务自动变成空操作（= 可取消调度）。</li>
 * </ul>
 *
 * <h2>结束流程（§3.9，保留加强）</h2>
 * <p>「恶」逃走 / 死亡 → {@link #endEscaped(ServerLevel, BlockPos)}：闪屏 2 次 → 运镜跟着它
 * 升回裂口 → 天空恢复 → 播报「这一切结束了。」「真的结束了吗！？」→ 把<b>所有被传送参与者</b>
 * 按各自 persistentData 记录点送回主世界（多人各回各家），战利品先入背包。</p>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID)
public final class EvilSummon {

    /** 感染半径（最外圈）。 */
    public static final int INFECT_R = 12;

    /** 演出总时长（tick）。160 → 200（契约 §3）。 */
    public static final int SHOW_TICKS = 200;

    /** 坠落过场时长（≥50t）。 */
    public static final int FALL_TICKS = 60;

    /** 降临动画时长（从上方压下来）。 */
    public static final int APPEAR_TICKS = 40;

    /** 天裂满 + 仰视运镜的时刻。 */
    public static final int STEP_CRACK = 40;
    /** 坠落过场开始的时刻。 */
    public static final int STEP_FALL = 50;
    /** 建场 + 传送 + 刷「恶」的时刻。 */
    public static final int STEP_ARENA = 90;
    /** 降临动画结束（解除 AI/无敌锁）的时刻。 */
    public static final int STEP_ARRIVED = STEP_ARENA + APPEAR_TICKS;

    /** 战斗期天空强度：0.9 而不是 1.0 —— 避开客户端 {@code FxWatchdog} 的 >0.98 强清阈值。 */
    private static final float SKY_FIGHT_STRENGTH = 0.9F;

    /** 演出会话（一次只有一场）。 */
    private static Show ACTIVE;

    /** 「恶」已降临、正在战斗（用于结束后持续推进天空动画）。 */
    private static boolean fightActive;
    private static ResourceKey<Level> fightDim;

    /** 玩家 persistentData 里的「回主世界」记录点。 */
    private static final String PD_DIM = "hongmeng_evil_return_dim";
    private static final String PD_X = "hongmeng_evil_return_x";
    private static final String PD_Y = "hongmeng_evil_return_y";
    private static final String PD_Z = "hongmeng_evil_return_z";
    private static final String PD_YAW = "hongmeng_evil_return_yaw";
    private static final String PD_PITCH = "hongmeng_evil_return_pitch";

    private EvilSummon() {
    }

    // ==================================================================
    // 对外状态（给命令 / 自检 / 其它模块读）
    // ==================================================================

    /** 当前是否有一场召唤演出在进行（T0~200）。 */
    public static boolean isShowing() {
        return ACTIVE != null;
    }

    /** 演出进度（tick）；没有演出返回 -1。 */
    public static int showAge() {
        Show s = ACTIVE;
        return s == null ? -1 : s.age;
    }

    /** 「恶」是否已经降临（战斗期，天空仍红）。 */
    public static boolean inFight() {
        return fightActive;
    }

    /** 外部（命令/调试）中断演出；返回是否真的中断了一场演出。 */
    public static boolean cancelShow(String reason) {
        Show s = ACTIVE;
        if (s == null) {
            return false;
        }
        ServerLevel level = resolve(s);
        cancel(level, reason == null ? "外部中断" : reason, s);
        return true;
    }

    // ==================================================================
    // 入口：EvilSummonPacket 调（签名不变）
    // ==================================================================

    /**
     * 开始召唤演出（{@code EvilSummonPacket} 在服务端校验 {@code ready()} 后调用）。
     *
     * <p>注意<b>消耗时机</b>：旧版 {@code EvilSummonPacket} 会先 {@code consumeAll()} 再调本方法；
     * 本方法两种状态都兼容：</p>
     * <ul>
     *   <li>祭坛五格<b>还在</b>（新版入口 / 命令调用）→ 本方法在会话建立成功后自己消耗；</li>
     *   <li>五格<b>已被包预先消耗</b> → 记 {@code itemsPreConsumed}，失败/取消时按兜底<b>退还</b>。</li>
     * </ul>
     */
    public static void begin(ServerPlayer player, ServerLevel level, BlockPos altar) {
        if (player == null || level == null || altar == null) {
            return;
        }
        if (ACTIVE != null) {
            // 已有演出：拒绝 + 补偿（此时包可能已经把这一台的五格吞了）
            HongmengMod.LOGGER.warn("[evil] 已有召唤演出在进行（{}t），拒绝来自 {} 的新请求",
                    ACTIVE.age, player.getName().getString());
            refundAltar(level, altar);
            player.displayClientMessage(Component.literal(
                    "\u00a7c祭坛的共鸣被打断了（已有一场召唤在进行）。"), true);
            return;
        }
        EvilAltarBlockEntity be = findAltar(level, altar);
        boolean preConsumed = false;
        if (be != null && be.ready()) {
            // 五格齐全：演出确认开始后由本方法消耗
        } else if (be != null && be.isEmpty()) {
            // 兼容旧入口：EvilSummonPacket 在 ready() 校验后立刻 consumeAll 了
            preConsumed = true;
            HongmengMod.LOGGER.info("[evil] 祭坛五格已被召唤包预先消耗（兼容旧入口），失败时按兜底退还");
        } else {
            HongmengMod.LOGGER.warn("[evil] 召唤被拒绝：祭坛五格未就绪 @{}", altar);
            player.displayClientMessage(Component.literal(
                    "\u00a7c祭坛尚未集齐五个维度的凭证。"), true);
            return;
        }

        Show show = new Show(player.getUUID(), level, altar);
        show.itemsPreConsumed = preConsumed;
        ACTIVE = show;
        try {
            if (be != null && !preConsumed) {
                be.consumeAll();          // 校验通过 + 会话建立 = 演出确认开始，才消耗
                show.consumedBySelf = true;
                HongmengMod.LOGGER.info("[evil] 演出开始，五格已消耗 @{}", altar);
            }
        } catch (Throwable t) {
            cancel(level, "消耗五格失败：" + t, show);
            return;
        }
        saveReturnPoint(level, player);
        HongmengMod.LOGGER.info("[evil] begin 演出 @{} dim={} player={} preConsumed={}",
                altar, level.dimension().location(), player.getName().getString(), preConsumed);

        // ---- 第 1 步：开场的对白 / 感染 / 火盆 / 光柱 / 低鸣 ----
        player.displayClientMessage(Component.literal(
                "\u00a74\u00a7l天穹裂开，一只眼睛看向了你……"), false);
        player.displayClientMessage(Component.literal(
                "\u00a77祭坛的五个凭证开始共鸣。"), true);
        level.playSound(null, altar, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.55F);
        try {
            infect(level, altar);         // 分层感染（只动天然方块）
            brazier(level, altar);        // 四周火盆
            pillar(level, altar);         // 直冲天顶的光柱
            level.playSound(null, altar, SoundEvents.BEACON_ACTIVATE,
                    SoundSource.AMBIENT, 2.0F, 0.6F);
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[evil] 开场布景部分失败（继续演出）：{}", t.toString());
        }
        EvilSky.set(0.05F, 0L, 1);
        syncSky(level);

        // 可取消的调度音效：取消后 {@link #isCurrent} 守卫会让它变成空操作
        schedule(show, 46, () -> level.playSound(null, altar, SoundEvents.END_PORTAL_SPAWN,
                SoundSource.AMBIENT, 2.0F, 0.5F));
        schedule(show, 74, () -> level.playSound(null, altar, SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE, 1.6F, 0.6F));
    }

    // ==================================================================
    // 每 tick 推进演出（本类自带 FORGE 事件挂接，不改 ModEvents）
    // ==================================================================

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        try {
            Show show = ACTIVE;
            if (show != null && show.dim.equals(level.dimension())) {
                tickShow(level, show);
                return;
            }
            if (show == null && fightActive && fightDim != null
                    && fightDim.equals(level.dimension())) {
                // 战斗期：继续推进"裂口张开 tick"并低频同步（客户端巨眼要动）
                EvilSky.tick();
                if (level.getGameTime() % 20L == 0L) {
                    syncSky(level);
                }
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[evil] 演出 tick 异常（尝试兜底取消）：{}", t.toString());
            Show show = ACTIVE;
            if (show != null) {
                cancel(level, "演出 tick 异常：" + t, show);
            }
        }
    }

    /** 演出主体：按 age 推进时间轴。 */
    private static void tickShow(ServerLevel level, Show s) {
        if (s.cancelled || s.successful) {
            // 成功后进入收尾尾巴：天空继续"呼吸"、裂口巨眼持续看人
            s.age++;
            EvilSky.tick();
            if (s.age % 20 == 0) {
                syncSky(level);
                level.playSound(null, s.altar, SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.AMBIENT, 0.7F, 0.5F);
                // 19 号契约 §3.2：降临后「恶」保持悬空 —— 这里每 20t 兜底夹一次
                // （战斗组每 tick 也会夹回同一点，见 EvilArena.hoverY）
                Entity boss = s.boss();
                if (boss != null && s.arenaCenter != null) {
                    boss.moveTo(s.arenaCenter.getX() + 0.5D,
                            EvilArena.hoverY(s.arenaCenter),
                            s.arenaCenter.getZ() + 0.5D, boss.getYRot(), 0.0F);
                    boss.setNoGravity(true);
                }
            }
            if (s.age >= SHOW_TICKS) {
                finishSession(level, s);
            }
            return;
        }
        // ---- 失败兜底：演出中玩家死亡 / 下线 / 换维度 ----
        if (!checkStillValid(level, s)) {
            return;
        }
        s.age++;
        int age = s.age;

        // ---- 第 2 步：血红天逐 tick 拉满（0~40t），低频同步 ----
        if (age <= STEP_CRACK) {
            EvilSky.set(Math.min(1.0F, age / (float) STEP_CRACK), age, 1);
            EvilSky.tick();
            if (age % 4 == 0) {
                syncSky(level);
            }
        } else {
            EvilSky.tick();
            if (age % 10 == 0) {
                syncSky(level);
            }
        }
        // 低鸣：开场到坠落前，每 15t 一次（低频心跳）
        if (age < STEP_ARENA && age % 15 == 0) {
            level.playSound(null, s.altar, SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.AMBIENT, 1.4F, 0.4F);
        }
        // 光柱持续到坠落
        if (age <= STEP_FALL && age % 10 == 0) {
            pillar(level, s.altar);
        }

        // ---- 第 2 步尾：全屏闪烁 3 次（间隔 8t）+ 音效 ----
        if (age == 20) {
            sendFlash(level, s, 3, 8);
            level.playSound(null, s.altar, SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.AMBIENT, 2.4F, 0.45F);
            level.playSound(null, s.altar, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.AMBIENT, 1.6F, 0.5F);
        }
        // ---- 第 3 步：天裂满 + 巨眼看向玩家 + 仰视运镜 ----
        if (age == STEP_CRACK) {
            EvilSky.set(1.0F, age, 1);
            syncSky(level);
            sendCinematicUp(level, s);
            ServerPlayer player = playerOf(s);
            if (player != null) {
                player.displayClientMessage(Component.literal(
                        "\u00a7c\u00a7l你被恶看见了。"), true);
                player.displayClientMessage(Component.literal(
                        "\u00a78裂口里的东西缓缓转了过来……"), false);
            }
            level.playSound(null, s.altar, SoundEvents.WARDEN_ROAR,
                    SoundSource.HOSTILE, 2.6F, 0.45F);
        }
        // ---- 第 4 步：坠落过场（≥50t）----
        if (age == STEP_FALL) {
            ServerPlayer player = playerOf(s);
            if (player != null) {
                sendTo(player, new EvilFallPacket(FALL_TICKS));
                player.displayClientMessage(Component.literal(
                        "\u00a77脚下的地面消失了。"), true);
            }
            level.playSound(null, s.altar, SoundEvents.ELYTRA_FLYING,
                    SoundSource.AMBIENT, 2.0F, 0.6F);
            level.playSound(null, s.altar, SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.AMBIENT, 1.4F, 0.4F);
        }
        // ---- 第 5 步：高天战场 + 传送 + 「恶」在台上方刷出 ----
        if (age == STEP_ARENA) {
            if (!stepArena(level, s)) {
                return;                   // stepArena 内部已 cancel
            }
        }
        // ---- 第 6 步：40t 降临动画（从上方压下来）----
        if (s.bossSpawned && age > STEP_ARENA && age < STEP_ARRIVED) {
            tickAppear(level, s, age);
        }
        // ---- 第 7 步：降临完成，进入战斗 ----
        if (s.bossSpawned && age == STEP_ARRIVED && !s.successful) {
            finishAppear(level, s);
        }
        // 尾巴：偶发心跳 + 天空稳定
        if (age >= STEP_ARRIVED && age % 40 == 0 && age < SHOW_TICKS) {
            level.playSound(null, s.altar, SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.AMBIENT, 0.8F, 0.5F);
        }
        if (age >= SHOW_TICKS) {
            finishSession(level, s);
        }
    }

    /** 第 5 步：建场 + 多人传送 + 刷「恶」。返回 false 表示已取消。 */
    private static boolean stepArena(ServerLevel level, Show s) {
        ServerPlayer summoner = playerOf(s);
        if (summoner == null) {
            cancel(level, "召唤者不在线", s);
            return false;
        }
        BlockPos arena;
        try {
            arena = EvilArena.ensure(level, s.altar);
        } catch (Throwable t) {
            cancel(level, "战场生成失败：" + t, s);
            return false;
        }
        if (arena == null) {
            cancel(level, "战场生成失败（ensure 返回 null）", s);
            return false;
        }
        s.arenaCenter = arena;

        // ---- 19 号契约 §3.3：召唤者 + 祭坛 64 格内同维度、非旁观者玩家，一起拉上天 ----
        List<ServerPlayer> group = new ArrayList<>();
        group.add(summoner);                    // 召唤者必须传
        Vec3 altarCenter = Vec3.atCenterOf(s.altar);
        final double rallyR2 = 64.0D * 64.0D;
        for (ServerPlayer p : List.copyOf(level.players())) {
            if (p == summoner || !p.isAlive() || p.isSpectator()) {
                continue;
            }
            if (p.distanceToSqr(altarCenter) > rallyR2) {
                continue;
            }
            group.add(p);
        }
        s.participants.clear();
        int teleported = 0;
        for (int i = 0; i < group.size(); i++) {
            ServerPlayer p = group.get(i);
            BlockPos landing = ringLanding(level, arena, i, group.size());
            if (landing == null) {
                // 19 号契约 §3.3：传送失败 → 取消演出 + 退还五凭证 + 报错日志
                HongmengMod.LOGGER.warn("[evil] 传送失败：{} 在台面外圈找不到安全落点",
                        p.getName().getString());
                cancel(level, "传送失败：" + p.getName().getString() + " 找不到台面安全落点", s);
                return false;
            }
            saveReturnPoint(level, p);          // 每人各记一份回城点（persistentData）
            float yaw = yawTowards(landing, arena);   // 朝向恶（中心正上方）
            try {
                p.teleportTo(level, landing.getX() + 0.5D, landing.getY(),
                        landing.getZ() + 0.5D, yaw, -18.0F);
                p.setPortalCooldown();
            } catch (Throwable t) {
                HongmengMod.LOGGER.warn("[evil] 传送异常：{} → {}：{}",
                        p.getName().getString(), landing, t.toString());
                cancel(level, "传送失败：" + p.getName().getString() + " 传送异常", s);
                return false;
            }
            // 传送到位校验（同维度 + 落在目标附近）；失败按契约取消
            boolean arrived = p.level().dimension().equals(s.dim)
                    && p.distanceToSqr(landing.getX() + 0.5D, landing.getY(),
                            landing.getZ() + 0.5D) <= 36.0D;
            if (!arrived) {
                HongmengMod.LOGGER.warn("[evil] 传送失败：{} 没有到达 {}",
                        p.getName().getString(), landing);
                cancel(level, "传送失败：" + p.getName().getString() + " 没有到达高天战场", s);
                return false;
            }
            s.participants.add(p.getUUID());
            teleported++;
            p.displayClientMessage(Component.literal(
                    "\u00a75\u00a7l你被拉进了天空裂隙"), false);
            p.displayClientMessage(Component.literal(
                    "\u00a78抬头 —— 台面上方就是「恶」。"), true);
            HongmengMod.LOGGER.info("[evil] 传送 {} → {}（yaw={}）",
                    p.getName().getString(), landing, yaw);
        }
        if (teleported <= 0) {
            cancel(level, "传送失败：没有任何玩家被拉进天空裂隙", s);
            return false;
        }
        level.playSound(null, arena, SoundEvents.END_PORTAL_SPAWN,
                SoundSource.AMBIENT, 2.0F, 0.6F);
        HongmengMod.LOGGER.info(
                "[evil] 多人传送完成：{} 人 → 高天战场 {}（台面 {} / 恶悬停 {}）",
                teleported, arena, EvilArena.floorY(arena), EvilArena.hoverY(arena));

        // ---- 刷「恶」：中心正上方 45 格（悬空），40t 后压到 hoverY ----
        EvilGazerEntity boss;
        try {
            boss = EvilArena.spawnGazer(level, arena, summoner);
        } catch (Throwable t) {
            cancel(level, "「恶」生成失败：" + t, s);
            return false;
        }
        if (boss == null) {
            cancel(level, "「恶」生成失败（实体创建返回 null）", s);
            return false;
        }
        s.bossId = boss.getUUID();
        s.bossSpawned = true;
        s.appearFromY = boss.getY();
        s.appearToY = EvilArena.hoverY(arena);      // 19 号契约：悬停 13 格，不落地
        level.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE, 3.0F, 0.4F);
        return true;
    }

    /** 第 6 步：40t 从上方压入。 */
    private static void tickAppear(ServerLevel level, Show s, int age) {
        Entity e = s.boss();
        if (!(e instanceof EvilGazerEntity boss) || boss.isRemoved()) {
            cancel(level, "降临中的「恶」消失了", s);
            return;
        }
        float t = Math.min(1.0F, (age - STEP_ARENA) / (float) APPEAR_TICKS);
        double y = s.appearFromY + (s.appearToY - s.appearFromY) * t;
        boss.moveTo(boss.getX(), y, boss.getZ(), boss.getYRot(), 0.0F);
        boss.hurtMarked = true;
        // 粒子：上方压下来的"云层被挤开"+ 血红火星
        if (age % 2 == 0) {
            ParticleBatch pb = new ParticleBatch(level);
            double cx = boss.getX();
            double cz = boss.getZ();
            for (int i = 0; i < 10; i++) {
                double a = level.random.nextDouble() * Math.PI * 2.0D;
                double r = 2.0D + level.random.nextDouble() * 6.0D;
                double px = cx + Math.cos(a) * r;
                double pz = cz + Math.sin(a) * r;
                pb.add(ParticleTypes.CLOUD, px, y + 1.5D, pz);
                pb.add(ParticleTypes.SOUL_FIRE_FLAME, px, y + 0.5D, pz);
                if (i % 3 == 0) {
                    pb.add(ParticleTypes.REVERSE_PORTAL, px, y + 2.5D, pz);
                }
            }
            pb.flush();
        }
        if (age % 10 == 0) {
            level.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.HOSTILE, 1.6F, 0.35F);
        }
    }

    /** 第 7 步：降临完成 → 解锁 AI / 无敌，进入阶段 1 战斗。 */
    private static void finishAppear(ServerLevel level, Show s) {
        Entity e = s.boss();
        if (e == null) {
            cancel(level, "降临完成前「恶」消失了", s);
            return;
        }
        s.successful = true;
        Vec3 anchor = e.position();
        if (e instanceof EvilGazerEntity boss) {
            // 19 号契约 §3.2：**不吸附台面** —— 夹到台面上方 HOVER_ABOVE 格的悬停点
            if (s.arenaCenter != null) {
                boss.moveTo(s.arenaCenter.getX() + 0.5D, EvilArena.hoverY(s.arenaCenter),
                        s.arenaCenter.getZ() + 0.5D, boss.getYRot(), 0.0F);
            } else {
                boss.moveTo(boss.getX(), s.appearToY, boss.getZ(), boss.getYRot(), 0.0F);
            }
            boss.setNoGravity(true);
            boss.hurtMarked = true;
            boss.setNoAi(false);
            boss.setInvulnerable(false);
            boss.getPersistentData().remove("evil_appearing");
            boss.setPhase(1);
            boss.setPersistenceRequired();
            HongmengMod.LOGGER.info("[evil] 降临完成：「恶」悬停在 y={}（台面 {}，悬空 {} 格）",
                    boss.getY(), EvilArena.floorY(s.arenaCenter), EvilArena.HOVER_ABOVE);
        }
        EvilSky.set(SKY_FIGHT_STRENGTH, s.age, 1);
        syncSky(level);
        fightActive = true;
        fightDim = s.dim;
        HongmengMod.LOGGER.info("[evil] 降临完成：阶段 1 战斗开始（演出 {}t / 总 {}t）",
                s.age, SHOW_TICKS);
        // 播报给"看得到这场面的人"：Boss 周围 256 格（高空战场）而不是祭坛附近
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(anchor) > 256.0D * 256.0D) {
                continue;
            }
            p.displayClientMessage(Component.literal("\u00a74\u00a7l恶 · 降临"), false);
            p.displayClientMessage(Component.literal(
                    "\u00a78它只伸进了一条手臂 —— 天空还没合上。"), true);
        }
        level.playSound(null, BlockPos.containing(anchor), SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE, 3.0F, 0.35F);
    }

    /** 演出会话结束（成功或自然到点）。 */
    private static void finishSession(ServerLevel level, Show s) {
        if (ACTIVE == s) {
            ACTIVE = null;
        }
        HongmengMod.LOGGER.info("[evil] 召唤演出结束：age={} successful={} cancelled={}",
                s.age, s.successful, s.cancelled);
    }

    /** 演出中玩家是否还"有效"；无效则取消。 */
    private static boolean checkStillValid(ServerLevel level, Show s) {
        ServerPlayer player = playerOf(s);
        if (player == null) {
            cancel(level, "召唤者已离线", s);
            return false;
        }
        if (player.isDeadOrDying() || !player.isAlive()) {
            cancel(level, "召唤者在演出中死亡", s);
            return false;
        }
        if (!player.level().dimension().equals(s.dim)) {
            cancel(level, "召唤者在演出中离开维度", s);
            return false;
        }
        return true;
    }

    // ==================================================================
    // 失败兜底：取消演出（撤 boss / 恢复天空 / 送回 / 退还五格 / 日志）
    // ==================================================================

    /**
     * 取消一场还没成功的演出。
     *
     * <p>兜底动作：撤掉已刷出的「恶」→ {@link EvilSky#clear()} + 同步 → 把召唤者送回
     * 进演出前的位置 → <b>退还祭坛五格</b>（失败不吞物品）→ WARN 日志。</p>
     */
    private static void cancel(ServerLevel level, String reason, Show s) {
        if (s == null || s.cancelled) {
            return;
        }
        if (s.successful) {
            // 已经降临成功：之后是正常战斗，死亡/离开不算"演出失败"
            return;
        }
        s.cancelled = true;
        s.token++;                          // 让所有已排队的调度任务失效（可取消）
        if (ACTIVE == s) {
            ACTIVE = null;
        }
        HongmengMod.LOGGER.warn("[evil] 召唤演出取消：{}（age={}t, player={}）",
                reason, s.age, s.playerId);
        // ① 撤掉已刷出的「恶」
        Entity boss = s.boss();
        if (boss != null && !boss.isRemoved()) {
            try {
                boss.discard();
                HongmengMod.LOGGER.info("[evil] 已撤掉演出中的「恶」");
            } catch (Throwable ignored) {
                // 忽略
            }
        }
        // ② 天空恢复
        EvilSky.clear();
        fightActive = false;
        fightDim = null;
        if (level != null) {
            syncSky(level);
        }
        // ③ 把所有被拉进裂隙的参与者（含召唤者）送回各自记录点
        int participantCount = s.participants.size();
        int returned = level == null ? 0 : returnHome(level, s, false);
        HongmengMod.LOGGER.info("[evil] 取消兜底：送回 {} 人（原参与者 {} 人）",
                returned, participantCount);
        ServerPlayer player = playerOf(s);
        if (player != null) {
            player.displayClientMessage(Component.literal(
                    "\u00a77召唤被中断了……"), true);
        }
        // ④ 退还祭坛五格（失败/取消不吞物品）
        if (level != null) {
            boolean refunded = refundAltar(level, s.altar);
            HongmengMod.LOGGER.info("[evil] 取消兜底：退还五格 = {}（itemsPreConsumed={}）",
                    refunded, s.itemsPreConsumed);
        }
    }

    // ==================================================================
    // 结束流程：逃走 → 闪屏 → 运镜升回裂口 → 天空恢复 → 播报 → 回主世界
    // ==================================================================

    /**
     * <b>「恶」逃回裂口后的收尾</b>（由 {@code EvilGazerEntity} 调，签名不变）。
     *
     * <p>闪屏 2 次 → 运镜跟着它升回裂口 → 天空恢复 → 「这一切结束了。」
     * 「真的结束了吗！？」→ 把还在高空的玩家<b>送回主世界记录点</b>。</p>
     */
    public static void endEscaped(ServerLevel level, BlockPos at) {
        Show s = ACTIVE;
        try {
            Vec3 anchor = at == null
                    ? new Vec3(0.0D, EvilArena.Y, 0.0D) : Vec3.atCenterOf(at);
            float[] shots = {
                    10.0F, 0.0F, 30.0F, 1.0F, -30.0F,
                    12.0F, 45.0F, 46.0F, 1.2F, -34.0F,
                    14.0F, 90.0F, 62.0F, 1.4F, -38.0F,
                    12.0F, 140.0F, 40.0F, 1.0F, -30.0F,
            };
            CinematicPacket cine = CinematicPacket.play(anchor, shots, 70,
                    "\u00a78\u00a7l它退回了裂口", "\u00a77天空重新合上");
            EvilFlashPacket flash = new EvilFlashPacket(2, 10);
            for (ServerPlayer p : level.players()) {
                sendTo(p, cine);
                sendTo(p, flash);
                p.displayClientMessage(Component.literal(
                        "\u00a7d\u00a7l这一切结束了。"), false);
                p.displayClientMessage(Component.literal(
                        "\u00a7c\u00a7l真的结束了吗！？"), true);
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[evil] 逃走收尾演出失败（继续清理）：{}", t.toString());
        }
        // 天空恢复
        EvilSky.clear();
        fightActive = false;
        fightDim = null;
        syncSky(level);
        // 清掉演出会话（如果还挂着）
        if (s != null) {
            s.cancelled = true;
            s.token++;
            if (ACTIVE == s) {
                ACTIVE = null;
            }
        }
        // 19 号契约 §3.4：所有被传送参与者（含召唤者）→ 各自记录点
        // 2026-11-02：恶收尾的即时战利品（evil_claim 表）在送回地面之前直接进背包
        try {
            int returned = returnHome(level, s, true);
            HongmengMod.LOGGER.info("[evil] endEscaped：送回 {} 名参与者到各自记录点", returned);
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[evil] 送回地面失败：{}", t.toString());
        }
        HongmengMod.LOGGER.info("[evil] endEscaped 收尾完成 @{}", at);
    }

    /** 结束（boss 逃走 / 玩家离开）。保留旧签名；escaped 时走完整收尾。 */
    public static void end(ServerLevel level, boolean escaped) {
        if (escaped) {
            endEscaped(level, null);
            return;
        }
        EvilSky.clear();
        fightActive = false;
        fightDim = null;
        syncSky(level);
    }

    // ==================================================================
    // 事件：演出中玩家死亡 / 下线 / 换维度 → 立刻取消
    // ==================================================================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            Show s = ACTIVE;
            if (s == null || s.successful || !s.playerId.equals(player.getUUID())) {
                return;
            }
            if (player.level() instanceof ServerLevel level) {
                cancel(level, "召唤者在演出中死亡（死亡事件）", s);
            }
        } catch (Throwable ignored) {
            // 兜底逻辑本身不能再抛
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            Show s = ACTIVE;
            if (s == null || s.successful || !s.playerId.equals(player.getUUID())) {
                return;
            }
            ServerLevel level = resolve(s);
            if (level != null) {
                cancel(level, "召唤者在演出中下线", s);
            }
        } catch (Throwable ignored) {
            // 忽略
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            Show s = ACTIVE;
            if (s == null || s.successful || !s.playerId.equals(player.getUUID())) {
                return;
            }
            if (s.dim.equals(event.getTo())) {
                return;
            }
            ServerLevel level = resolve(s);
            if (level != null) {
                cancel(level, "召唤者在演出中换维度", s);
            }
        } catch (Throwable ignored) {
            // 忽略
        }
    }

    // ==================================================================
    // 第 1 步布景：分层感染 / 火盆 / 光柱
    // ==================================================================

    /**
     * 分层感染：内圈（r≤4）腐肉化、中圈（r≤9）血红石/菌土、外圈（r≤{@link #INFECT_R}）
     * 骨块与零星菌土；<b>只改天然方块</b>（白名单见 {@link #cursed}），不碰祭坛与人工建筑。
     */
    private static void infect(ServerLevel level, BlockPos altar) {
        RandomSource rnd = RandomSource.create(altar.asLong() ^ 0x5DEECE66DL);
        int placed = 0;
        for (int dx = -INFECT_R; dx <= INFECT_R; dx++) {
            for (int dz = -INFECT_R; dz <= INFECT_R; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > INFECT_R * INFECT_R) {
                    continue;
                }
                // 越往外越稀疏：内圈必改、中圈 60%、外圈 25%
                double chance = dist2 <= 16 ? 1.0D : (dist2 <= 81 ? 0.6D : 0.25D);
                if (rnd.nextDouble() > chance) {
                    continue;
                }
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos p = altar.offset(dx, dy, dz);
                    if (!level.hasChunkAt(p)) {
                        continue;
                    }
                    BlockState st = level.getBlockState(p);
                    if (st.is(com.hongmeng.xiuxian.ModBlocks.EVIL_ALTAR.get())) {
                        continue;                      // 绝不覆盖祭坛本体
                    }
                    BlockState to = cursed(st, dist2 <= 16 ? 0 : (dist2 <= 81 ? 1 : 2));
                    if (to != null) {
                        level.setBlock(p, to, 2);
                        placed++;
                    }
                }
            }
        }
        HongmengMod.LOGGER.info("[evil] 分层感染完成：{} 格", placed);
    }

    /** 感染白名单：天然方块 → 腐化方块；层级 0 内圈 / 1 中圈 / 2 外圈。 */
    private static BlockState cursed(BlockState s, int ring) {
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT)
                || s.is(Blocks.MYCELIUM) || s.is(Blocks.PODZOL)) {
            return (ring >= 2 && s.is(Blocks.DIRT))
                    ? Blocks.SOUL_SOIL.defaultBlockState()
                    : Blocks.SCULK.defaultBlockState();
        }
        if (s.is(Blocks.STONE) || s.is(Blocks.DEEPSLATE)
                || s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE)
                || s.is(Blocks.GRANITE) || s.is(Blocks.TUFF)) {
            return ring >= 2 ? Blocks.CRIMSON_NYLIUM.defaultBlockState()
                    : Blocks.SCULK_CATALYST.defaultBlockState();
        }
        if (s.is(Blocks.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY)) {
            return Blocks.SOUL_SOIL.defaultBlockState();
        }
        if (s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE)) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (s.is(Blocks.OAK_LEAVES) || s.is(Blocks.BIRCH_LEAVES)
                || s.is(Blocks.SPRUCE_LEAVES) || s.is(Blocks.JUNGLE_LEAVES)) {
            return ring <= 1 ? Blocks.SCULK_VEIN.defaultBlockState() : null;
        }
        return null;
    }

    /** 祭坛四周点亮火盆（黑石基座 + 灵魂营火，只放在地表空气格）。 */
    private static void brazier(ServerLevel level, BlockPos altar) {
        int[][] at = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}, {4, 4}, {4, -4}, {-4, 4}, {-4, -4}};
        int placed = 0;
        for (int[] o : at) {
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    altar.offset(o[0], 0, o[1]));
            if (Math.abs(surface.getY() - altar.getY()) > 6) {
                continue;                          // 地形起伏太大就跳过，免得火盆悬空
            }
            BlockPos base = surface.below();
            if (!level.isLoaded(surface)) {
                continue;
            }
            if (!level.getBlockState(base).isAir()) {
                level.setBlock(base, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
            }
            if (level.getBlockState(surface).isAir()
                    || level.getBlockState(surface).canBeReplaced()) {
                level.setBlock(surface, Blocks.SOUL_CAMPFIRE.defaultBlockState(), 2);
                placed++;
            }
        }
        HongmengMod.LOGGER.info("[evil] 火盆点亮：{}/{} 处", placed, at.length);
    }

    /** 直冲天顶的光柱 + 低鸣（粒子批量发送，服务端不卡）。 */
    private static void pillar(ServerLevel level, BlockPos altar) {
        ParticleBatch pb = new ParticleBatch(level);
        double cx = altar.getX() + 0.5D;
        double cz = altar.getZ() + 0.5D;
        // 主光柱：从祭坛顶一直打到 y=320 附近
        for (int k = 0; k < 120; k++) {
            pb.add(ParticleTypes.END_ROD, cx, altar.getY() + 1.0D + k * 2.0D, cz);
        }
        // 外旋火焰：螺旋上升，看起来像"信标光柱"
        for (int k = 0; k < 40; k++) {
            double a = k * 0.55D;
            double r = 1.2D + (k % 5) * 0.25D;
            pb.add(ParticleTypes.SOUL_FIRE_FLAME,
                    cx + Math.cos(a) * r, altar.getY() + 1.0D + k * 2.2D,
                    cz + Math.sin(a) * r);
        }
        // 底部冲击环
        for (int i = 0; i < 24; i++) {
            double a = i * (Math.PI * 2.0D / 24.0D);
            pb.add(ParticleTypes.SOUL, cx + Math.cos(a) * 3.0D,
                    altar.getY() + 1.0D, cz + Math.sin(a) * 3.0D);
        }
        pb.flush();
    }

    // ==================================================================
    // 网络：天空同步 / 闪屏 / 仰视运镜 / 坠落
    // ==================================================================

    /** 把当前天空状态同步给该维度所有玩家（强度 + 裂口张开 tick + 阶段）。 */
    private static void syncSky(ServerLevel level) {
        if (level == null) {
            return;
        }
        try {
            EvilSkyPacket pkt = new EvilSkyPacket(EvilSky.strength(),
                    EvilSky.openTicks(), EvilSky.phase());
            for (ServerPlayer p : level.players()) {
                sendTo(p, pkt);
            }
        } catch (Throwable ignored) {
            // 同步失败不影响演出
        }
    }

    private static void sendFlash(ServerLevel level, Show s, int count, int interval) {
        try {
            EvilFlashPacket pkt = new EvilFlashPacket(count, interval);
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(Vec3.atCenterOf(s.altar)) <= 96.0D * 96.0D) {
                    sendTo(p, pkt);
                }
            }
        } catch (Throwable ignored) {
            // 忽略
        }
    }

    /** 第 3 步的仰视运镜：镜头压到地面，向上拍天幕裂口里的巨眼。 */
    private static void sendCinematicUp(ServerLevel level, Show s) {
        ServerPlayer player = playerOf(s);
        if (player == null) {
            return;
        }
        try {
            Vec3 anchor = Vec3.atCenterOf(s.altar).add(0.0D, 2.0D, 0.0D);
            // (dist, yawDeg, pitchDeg, lookY, fov)；pitch 越大越靠下往上拍
            float[] shots = {
                    4.0F, 0.0F, 18.0F, 30.0F, 78.0F,
                    3.0F, 14.0F, 52.0F, 42.0F, 76.0F,
                    2.5F, 26.0F, 76.0F, 52.0F, 74.0F,
                    2.0F, 40.0F, 84.0F, 62.0F, 82.0F,
            };
            sendTo(player, CinematicPacket.play(anchor, shots, 60,
                    "\u00a74\u00a7l天裂", "\u00a7c你被恶看见了"));
        } catch (Throwable ignored) {
            // 运镜失败不影响演出
        }
    }

    private static void sendTo(ServerPlayer player, Object packet) {
        try {
            ModNetwork.sendToPlayer(player, packet);
        } catch (Throwable ignored) {
            // 忽略
        }
    }

    // ==================================================================
    // 玩家 / 实体 / 物品 小工具
    // ==================================================================

    private static ServerPlayer playerOf(Show s) {
        MinecraftServer server = s.level == null ? null : s.level.getServer();
        return server == null ? null
                : server.getPlayerList().getPlayer(s.playerId);
    }

    private static ServerLevel resolve(Show s) {
        return s.level;
    }

    /** 在祭坛坐标 ±3 内找方块实体（防止坐标轻微错位）。 */
    private static EvilAltarBlockEntity findAltar(ServerLevel level, BlockPos pos) {
        if (pos == null || level == null) {
            return null;
        }
        if (level.getBlockEntity(pos) instanceof EvilAltarBlockEntity direct) {
            return direct;
        }
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-3, -3, -3),
                pos.offset(3, 3, 3))) {
            if (level.getBlockEntity(q) instanceof EvilAltarBlockEntity found) {
                return found;
            }
        }
        return null;
    }

    /**
     * 退还五格（失败/取消不吞物品）。
     *
     * <p>按 {@link EvilAltarBlockEntity#SLOTS} + {@link EvilAltarBlockEntity#acceptedFor(int)}
     * 逐格补回 —— <b>不硬编码 4 槽时代的旧表</b>，将来加物品也不用改这里。</p>
     */
    private static boolean refundAltar(ServerLevel level, BlockPos altar) {
        try {
            EvilAltarBlockEntity be = findAltar(level, altar);
            if (be == null) {
                HongmengMod.LOGGER.warn("[evil] 退还五格失败：祭坛方块实体不在了 @{}", altar);
                return false;
            }
            int refunded = 0;
            for (int i = 0; i < EvilAltarBlockEntity.SLOTS; i++) {
                if (be.getItem(i).isEmpty()) {
                    Item item = EvilAltarBlockEntity.acceptedFor(i);
                    if (item != null) {
                        be.setItem(i, new ItemStack(item));
                        refunded++;
                    }
                }
            }
            if (refunded > 0) {
                HongmengMod.LOGGER.info("[evil] 已退还祭坛 {} 格凭证 @{}", refunded, altar);
            }
            return refunded > 0;
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[evil] 退还五格异常：{}", t.toString());
            return false;
        }
    }

    /**
     * 台面外圈 6~10 格的传送落点（19 号契约 §3.3）。
     *
     * <p>按参与者序号绕圈均分，再在台面上找"脚踏实心 + 头顶空"的格子：
     * 半径 6~10、朝向恶（台面中心正上方）、y = 台面 {@code floorY} 那层。</p>
     *
     * @return 安全落点（玩家脚底位置）；找不到返回 null
     */
    private static BlockPos ringLanding(ServerLevel level, BlockPos center, int index, int count) {
        if (level == null || center == null) {
            return null;
        }
        double baseAngle = count <= 1 ? 0.0D : (Math.PI * 2.0D * index / count);
        double floor = EvilArena.floorY(center);
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = baseAngle + attempt * (Math.PI / 7.0D);
            double radius = 6.0D + Math.floorMod(index + attempt, 5);   // 6~10 格
            int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            BlockPos candidate = BlockPos.containing(x + 0.5D, floor, z + 0.5D);
            BlockPos safe = safeLanding(level, candidate);
            if (safe == null) {
                continue;
            }
            // 必须还在台面高度附近（不能被高度图带到别的层）
            if (Math.abs(safe.getY() - floor) <= 2.0D) {
                return safe;
            }
        }
        return null;
    }

    /** 从落点看向恶（台面中心正上方）的水平朝向（MC yaw：0=+Z，90=-X，180=-Z，270=+X）。 */
    private static float yawTowards(BlockPos from, BlockPos target) {
        double dx = (target.getX() + 0.5D) - (from.getX() + 0.5D);
        double dz = (target.getZ() + 0.5D) - (from.getZ() + 0.5D);
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /**
     * 把所有被拉进天空裂隙的参与者送回各自记录点（19 号契约 §3.4）。
     *
     * <p>{@link #returnHome(ServerLevel, ServerPlayer)} 的<b>多人重载</b>：
     * 名单 = 会话参与者 ∪ 召唤者 ∪ 仍在高空（{@code y > EvilArena.Y-80}）的兜底玩家；
     * 每人用自己的 persistentData 记录点，所以多人各回各家。离线的跳过（记录点还在，
     * 下次上线若仍被判定高空会被兜底送回）。</p>
     *
     * @param grantEvilLoot true = 送回前先把「恶」的即时战利品塞进背包（endEscaped 用）
     * @return 实际送回的人数
     */
    private static int returnHome(ServerLevel level, Show s, boolean grantEvilLoot) {
        if (level == null) {
            return 0;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return 0;
        }
        Set<UUID> ids = new LinkedHashSet<>();
        if (s != null) {
            ids.addAll(s.participants);
            ids.add(s.playerId);              // 召唤者始终算（可能还没被传送）
        }
        for (ServerPlayer p : List.copyOf(level.players())) {
            if (p.getY() > EvilArena.Y - 80) {
                ids.add(p.getUUID());
            }
        }
        int returned = 0;
        for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                continue;
            }
            if (grantEvilLoot) {
                try {
                    com.hongmeng.xiuxian.dungeon.BossRewards.grantEvilLoot(p);
                } catch (Throwable ignored) {
                    // 战利品失败不影响送回
                }
            }
            returnHome(level, p);
            p.sendSystemMessage(Component.literal("\u00a77你被送回了地面。"));
            returned++;
        }
        if (s != null) {
            s.participants.clear();
        }
        return returned;
    }

    /** 记录「回主世界」的位置（写进玩家 persistentData；多人各记一份）。 */
    private static void saveReturnPoint(ServerLevel level, ServerPlayer player) {
        try {
            CompoundTag pd = player.getPersistentData();
            Level from = level != null ? level : player.level();
            pd.putString(PD_DIM, from.dimension().location().toString());
            pd.putDouble(PD_X, player.getX());
            pd.putDouble(PD_Y, player.getY());
            pd.putDouble(PD_Z, player.getZ());
            pd.putFloat(PD_YAW, player.getYRot());
            pd.putFloat(PD_PITCH, player.getXRot());
        } catch (Throwable ignored) {
            // 忽略
        }
    }

    /** 把玩家送回进演出前的位置（没有记录就回世界出生点）。 */
    private static void returnHome(ServerLevel level, ServerPlayer player) {
        try {
            MinecraftServer server = level == null ? null : level.getServer();
            if (server == null) {
                return;
            }
            CompoundTag pd = player.getPersistentData();
            ServerLevel target = level;
            double x;
            double y;
            double z;
            float yaw;
            float pitch;
            if (pd.contains(PD_DIM)) {
                ResourceLocation rl = ResourceLocation.tryParse(pd.getString(PD_DIM));
                ServerLevel other = rl == null ? null
                        : server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
                if (other != null) {
                    target = other;
                }
                x = pd.getDouble(PD_X);
                y = pd.getDouble(PD_Y);
                z = pd.getDouble(PD_Z);
                yaw = pd.getFloat(PD_YAW);
                pitch = pd.getFloat(PD_PITCH);
            } else {
                BlockPos spawn = target.getSharedSpawnPos();
                x = spawn.getX() + 0.5D;
                y = spawn.getY();
                z = spawn.getZ() + 0.5D;
                yaw = player.getYRot();
                pitch = player.getXRot();
            }
            // 落点兜底：记录点被填了就往地表找
            BlockPos safe = safeLanding(target, BlockPos.containing(x, y, z));
            player.teleportTo(target, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                    yaw, pitch);
            player.setPortalCooldown();
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[evil] 送回主世界失败：{}", t.toString());
        }
    }

    /** 安全落点：优先用给定坐标，卡住就往上找 5 格，再不行用地表高度图。 */
    private static BlockPos safeLanding(ServerLevel level, BlockPos pos) {
        if (level == null) {
            return pos;
        }
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos p = pos.above(dy);
            if (!level.isLoaded(p)) {
                return p;
            }
            boolean feet = level.getBlockState(p).getCollisionShape(level, p).isEmpty();
            boolean head = level.getBlockState(p.above())
                    .getCollisionShape(level, p.above()).isEmpty();
            boolean ground = !level.getBlockState(p.below())
                    .getCollisionShape(level, p.below()).isEmpty();
            if (feet && head && ground) {
                return p;
            }
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
    }

    // ==================================================================
    // 可取消的调度（RealWorldScheduler 本身没有 cancel API：
    // 这里用"会话 token 守卫"实现取消语义 —— 取消后任务自动变空操作）
    // ==================================================================

    /** 排队一个演出动作；取消后（token 失效）执行时直接跳过。 */
    private static void schedule(Show show, int delayTicks, Runnable action) {
        try {
            RealWorldScheduler.after(show.level, delayTicks, () -> {
                if (!isCurrent(show)) {
                    HongmengMod.LOGGER.debug("[evil] 跳过已取消的调度任务（+{}t）", delayTicks);
                    return;
                }
                try {
                    action.run();
                } catch (Throwable t) {
                    HongmengMod.LOGGER.warn("[evil] 调度任务执行失败：{}", t.toString());
                }
            });
        } catch (Throwable ignored) {
            // 调度失败不影响演出
        }
    }

    /** 会话是否仍是"当前有效"的那一场（token 匹配 + 未取消 + 未结束）。 */
    private static boolean isCurrent(Show show) {
        return show != null && ACTIVE == show && !show.cancelled;
    }

    // ==================================================================
    // 演出会话
    // ==================================================================

    /** 一场召唤演出的状态。 */
    private static final class Show {
        final UUID playerId;
        final ServerLevel level;
        final ResourceKey<Level> dim;
        final BlockPos altar;
        /** 高天战场中心（台面中心；恶锚点悬停在它正上方 {@link EvilArena#hoverY}）。 */
        BlockPos arenaCenter;
        /** 被拉进天空裂隙的玩家（每人各自在 persistentData 里有一份回城点）。 */
        final List<UUID> participants = new ArrayList<>();
        int age;
        boolean itemsPreConsumed;
        boolean consumedBySelf;
        boolean bossSpawned;
        boolean successful;
        boolean cancelled;
        UUID bossId;
        double appearFromY;
        double appearToY;
        /** 调度 token：取消时 +1，让已排队的任务失效。 */
        long token;

        Show(UUID playerId, ServerLevel level, BlockPos altar) {
            this.playerId = playerId;
            this.level = level;
            this.dim = level.dimension();
            this.altar = altar.immutable();
        }

        Entity boss() {
            if (bossId == null || level == null) {
                return null;
            }
            Entity e = level.getEntity(bossId);
            return e instanceof EvilGazerEntity ? e : null;
        }
    }
}
