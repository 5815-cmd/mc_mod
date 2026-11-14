package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.core.CultivationCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 副本维度的服务端驱动。
 *
 * <p><b>为什么单独开一个类：</b>{@code core/ModEvents.java} 只驱动纪元秘境
 * （{@code EpochRealm.tick}），而那个文件是别人负责的、不能改。
 * 这里用 {@link Mod.EventBusSubscriber} 注解自行挂到 Forge 事件总线上，
 * 不动任何公共文件。</p>
 *
 * <p>管五件事：每 tick 推进波次、换维度 / 死亡时作废挑战、
 * 潜行 + 空手右键退出秘境、下线时清临时状态，以及
 * <b>每 tick 限时驱动副本「战场 / 大本营 / 外围地貌」的跨 tick 分片任务</b>（卡顿治理）。</p>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID)
public final class DungeonRealmEvents {

    /**
     * 本 game tick 的分片施工预算截止时间（nanoTime）。
     *
     * <p>PERF(A)：LevelTickEvent 每个维度都会触发一次；用 gameTime 做标识让<b>所有维度共享</b>
     * 同一个 8ms 预算，避免 4 个副本维度各花 8ms 叠成 32ms/tick。首个维度的事件里开始计时，
     * 其余维度复用。</p>
     */
    private static long budgetGameTime = Long.MIN_VALUE;
    private static long budgetDeadlineNanos;

    private DungeonRealmEvents() {
    }

    /** 波次推进 + 战斗场地兜底（从传送门那条路进来的玩家不经过 DungeonRealm.enter）。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            Dungeon dungeon = DungeonRealm.dungeonAt(level);
            if (dungeon != null) {
                // PERF(A): 只做"确保已入队"（一个 EnumMap 查询），施工由 onLevelTick 分片推进；
                // 以前的 tickEnsure 会同步建 1.6~1.8 万格（~1.5s 卡顿）。
                DungeonArena.tickEnsure(level, dungeon);
            }
        }
        DungeonRealm.tick(player, CultivationCapability.get(player));
    }

    /**
     * 每维度 tick：分片推进「战场 + 大本营 + 外围地貌」任务（每 tick 全服共享 8ms 预算）。
     *
     * <p>PERF(A)：世界初始化时 4 个副本维度曾各自同步施工，合计卡出
     * {@code Can't keep up! Running 2896ms or 57 ticks behind}。现在：</p>
     * <ul>
     *   <li>仍保留「每 20 tick 错开一座维度」的入队节流（避免 4 个维度同时开工抢 CPU）；</li>
     *   <li>入队后按 {@link TickWork#BUDGET_NANOS} 限时推进，单 tick 只做一小片；</li>
     *   <li>外围地貌任务先在后台 ticket 预载 289 区块，等战场结算后才能动工（保持老顺序）。</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        // PERF(Lead) 诊断：用主世界当"时钟"，服务端明显掉帧时每 5 秒打一行全局实体分类计数。
        // 2026-11-07 实机在虚空秘境打怪潮时从 2s 恶化到 26s behind，日志里只有 Can't keep up，
        // 看不出到底是什么在堆积；有了这行，下次直接看到是 怪 / 掉落物 / 经验球 / 其它 哪类爆了。
        // MSPT 正常时零输出、零分配。
        if (level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            try {
                net.minecraft.server.MinecraftServer srv = level.getServer();
                long gt = level.getGameTime();
                if (srv != null && srv.isRunning()
                        && gt % 100L == 0L && srv.getAverageTickTime() > 150.0F) {
                    logLagBreakdown(srv);
                }
            } catch (Throwable ignored) {
                // 诊断失败绝不拖累主逻辑
            }
        }
        Dungeon dungeon = DungeonRealm.dungeonAt(level);
        if (dungeon == null) {
            return;
        }
        net.minecraft.server.MinecraftServer server = level.getServer();
        if (server == null || !server.isRunning()) {
            return;
        }
        // 本 game tick 的共享预算（所有维度共用 8ms，故用 gameTime 做标识）
        long gameTime = level.getGameTime();
        if (budgetGameTime != gameTime) {
            budgetGameTime = gameTime;
            budgetDeadlineNanos = System.nanoTime() + TickWork.BUDGET_NANOS;
        }
        // 保留「每 20 tick 错开一座维度」的入队节流：玩家进场 / 命令路径会立即入队，不等这里。
        if (!DungeonArena.isSettled(dungeon)
                && gameTime % 20L == dungeon.ordinal() * 5L) {
            DungeonArena.requestBuild(level, dungeon);
        }
        // 分片推进（每 tick 一小片，单 tick 服务端开销 = 预算内的 8ms + 一次单 tick 施工）
        DungeonArena.advanceTask(level, dungeon, budgetDeadlineNanos);
        RealmTheme.advanceTask(level, dungeon, budgetDeadlineNanos);
    }

    /** PERF(Lead)：慢 tick 时打印每个维度的实体分类计数（只诊断、不改世界）。 */
    private static void logLagBreakdown(net.minecraft.server.MinecraftServer server) {
        StringBuilder sb = new StringBuilder();
        for (net.minecraft.server.level.ServerLevel lvl : server.getAllLevels()) {
            int mobs = 0;
            int items = 0;
            int orbs = 0;
            int other = 0;
            for (net.minecraft.world.entity.Entity e : lvl.getAllEntities()) {
                if (e instanceof net.minecraft.world.entity.item.ItemEntity) {
                    items++;
                } else if (e instanceof net.minecraft.world.entity.ExperienceOrb) {
                    orbs++;
                } else if (e instanceof net.minecraft.world.entity.Mob) {
                    mobs++;
                } else {
                    other++;
                }
            }
            int total = mobs + items + orbs + other;
            if (total >= 40) {
                sb.append(String.format(java.util.Locale.ROOT,
                        " [%s mobs=%d items=%d orbs=%d other=%d]",
                        lvl.dimension().location(), mobs, items, orbs, other));
            }
        }
        HongmengMod.LOGGER.warn("[perf] MSPT={} 实体分类:{}",
                String.format(java.util.Locale.ROOT, "%.0f", server.getAverageTickTime()),
                sb.length() == 0 ? " （各维度实体都不多，瓶颈可能在单次事件而非常驻实体）" : sb);
    }

    /** 换维度：按挑战阶段决定保留/作废；离开 boss 维度时清掉过期记录。 */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DungeonRealm.abortIfOutside(player);
            // 从 boss 维度离开、且手里已经没有挑战时，把“从哪来”的记录清掉，
            // 免得下次再进 boss 维度被旧记录弹到一个很远的地方。
            if (ArenaRealm.isArenaDim(event.getFrom())) {
                DungeonRealm.clearStaleArenaRecords(player);
            }
        }
    }

    /** 登录：上次在 boss 维度离线、挑战已不在 → 兜底送回主世界，避免永久困住。 */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DungeonRealm.escapeStrandedArena(player);
        }
    }

    /** 死亡：挑战作废。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DungeonRealm.abort(player);
        }
    }

    /** 下线：清掉冷却与返还点记录。 */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DungeonRealm.forget(player);
        }
    }

    /**
     * 退出机制：在副本维度里<b>潜行 + 空手右键</b>任意方块即退出。
     *
     * <p>用空手作为条件是为了不跟"边搭边打"打架——手里拿着方块时右键是放置，
     * 不会误触退出。这条事件在方块自己的 {@code use} 之前触发并取消掉它，
     * 所以对着框架门潜行右键也不会把门再点一次。</p>
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        Dungeon dungeon = DungeonRealm.dungeonAt(player.level());
        if (dungeon != null) {
            DungeonRealm.leave(player, dungeon);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }
        // boss 决战维度：没有“副本维度”可退，但同样允许潜行 + 空手右键撤离。
        // 以前这条逻辑写在 BossSummonBlock 的召唤函数里，祭坛点亮后永远走不到，
        // 玩家会被困在首领位面（审计 A-2）。
        if (ArenaRealm.isInArena(player)) {
            DungeonRealm.abandonArena(player, "\u00a77你主动撤离了首领位面（进度作废）。");
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }
}
