package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 蓄力（施法前摇）与被动功法的事件落地。
 *
 * <p>这样做是为了**不去改 {@code core/ModEvents.java}**（那边同时在加灵力与御剑飞行）。
 * 本类是独立的 {@code @SubscribeEvent} 持有者，由 {@code SpellSystem.register()}
 * 挂到 Forge 事件总线上（另注册一个实例，不与既有的那套打架）。</p>
 *
 * <h3>蓄力为什么要有快照</h3>
 *
 * <p>中断判据是"移动超过阈值 / 受伤 / 换手持物"，而这三件事都需要<b>起手那一刻</b>的
 * 基准值。基准放在 {@link #SNAPSHOT} 里（按 UUID），随蓄力结束一起清掉；
 * {@link TechniqueData} 只存"在不在蓄力、蓄的哪一门、什么时候完"这三样可存档的东西。</p>
 *
 * <h3>中断的代价</h3>
 *
 * <p>灵力在**起手时就扣掉了**，所以被打断不退灵力、冷却也照走——
 * 这是刻意的：不然"被打断就白嫖一次前摇"会成为最优解。</p>
 */
public final class SpellCastHandler {

    /** 蓄力期间的移动容差（格）。超过就打断。 */
    public static final double MOVE_TOLERANCE = 1.25D;

    /** 被动重算周期（tick）。属性修饰符反复摘挂会刷客户端同步包，所以 1 秒一次。 */
    private static final int PASSIVE_REFRESH_TICKS = 20;

    /** 玩家的蓄力基准快照。 */
    private static final Map<UUID, Snapshot> SNAPSHOT = new HashMap<>();

    /** 已经挂了被动修饰符的玩家（掉线时用来清账）。 */
    private static final Set<UUID> PASSIVE_APPLIED = new HashSet<>();

    private static int tickCounter = 0;

    /** 公开：{@link SpellSystem#register()} 要 new 一个挂到事件总线上。 */
    public SpellCastHandler() {
    }

    /**
     * 起手快照：<b>施法原点 + 朝向</b> + 生命 + 手持物。
     *
     * <h2>为什么要记朝向</h2>
     * <p>玩家要求：「释放的位置在玩家脚下，后续移动法术不跟随玩家移动」。</p>
     *
     * <p>于是法术的射线起点与方向都必须在<b>施法瞬间</b>定下来，
     * 之后一律用快照 —— 这样即使玩家（或演出镜头）移动了，
     * 法术仍然从原来那个位置、朝原来那个方向打出。</p>
     *
     * <p>注：{@code eyeY} 记的是<b>眼睛高度</b>而不是眼睛坐标，
     * 因为脚底位置已经在 {@code x/y/z} 里了，两者相加就是眼睛坐标。</p>
     */
    private static final class Snapshot {
        final double x;
        final double y;
        final double z;
        /** 眼睛相对脚底的高度（施法瞬间）。 */
        final double eyeY;
        /** 施法瞬间的朝向（已归一化）。 */
        final double lx;
        final double ly;
        final double lz;
        final float health;
        final String mainHand;
        final String offHand;
        /** 记位置基准时所在的维度：换维度后基准失效，不能当成"移动"打断。 */
        final net.minecraft.resources.ResourceLocation dimension;

        Snapshot(ServerPlayer player) {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.eyeY = player.getEyeHeight();
            var look = player.getLookAngle();
            double len = Math.sqrt(look.x * look.x + look.y * look.y + look.z * look.z);
            if (len < 1.0E-6D) {
                this.lx = 0.0D;
                this.ly = 0.0D;
                this.lz = 1.0D;
            } else {
                this.lx = look.x / len;
                this.ly = look.y / len;
                this.lz = look.z / len;
            }
            this.health = player.getHealth();
            this.mainHand = itemKey(player.getMainHandItem());
            this.offHand = itemKey(player.getOffhandItem());
            this.dimension = player.level().dimension().location();
        }
    }

    private static String itemKey(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id == null ? "" : id.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    /**
     * <b>施法原点</b>（脚底，施法瞬间的快照）。
     *
     * <p>玩家要求：「释放的位置在玩家脚下，后续移动法术不跟随玩家移动」。
     * 所有蓄力特效与结算射线都用它，<b>不用实时玩家位置</b> ——
     * 这样法术就"留在释放的地方"。</p>
     *
     * <p>没有快照时（例如非施法路径的调用）回落到玩家的实时位置，
     * 保证行为不变。</p>
     */
    public static net.minecraft.world.phys.Vec3 castOrigin(ServerPlayer player) {
        Snapshot snap = player == null ? null : SNAPSHOT.get(player.getUUID());
        if (snap == null) {
            return player == null ? net.minecraft.world.phys.Vec3.ZERO : player.position();
        }
        return new net.minecraft.world.phys.Vec3(snap.x, snap.y, snap.z);
    }

    /** <b>施法射线的起点</b>（原点 + 眼睛高度，用快照算）。 */
    public static net.minecraft.world.phys.Vec3 castEye(ServerPlayer player) {
        Snapshot snap = player == null ? null : SNAPSHOT.get(player.getUUID());
        if (snap == null) {
            return player == null ? net.minecraft.world.phys.Vec3.ZERO
                    : player.getEyePosition();
        }
        return new net.minecraft.world.phys.Vec3(snap.x, snap.y + snap.eyeY, snap.z);
    }

    /** <b>施法瞬间的朝向</b>（已归一化）。 */
    public static net.minecraft.world.phys.Vec3 castLook(ServerPlayer player) {
        Snapshot snap = player == null ? null : SNAPSHOT.get(player.getUUID());
        if (snap == null) {
            return player == null ? net.minecraft.world.phys.Vec3.ZERO
                    : player.getLookAngle();
        }
        return new net.minecraft.world.phys.Vec3(snap.lx, snap.ly, snap.lz);
    }

    /** 起手时记录基准（由 {@link TechniqueCaster#cast} 调用）。 */
    public static void beginCastSnapshot(ServerPlayer player) {
        if (player != null) {
            SNAPSHOT.put(player.getUUID(), new Snapshot(player));
        }
    }

    /** 蓄力结束（完成或中断）时清快照。 */
    public static void endCastSnapshot(ServerPlayer player) {
        if (player != null) {
            SNAPSHOT.remove(player.getUUID());
        }
    }

    // ------------------------------------------------------------------
    // 每 tick 推进
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        boolean refreshPassives = (++tickCounter % PASSIVE_REFRESH_TICKS) == 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CultivationData data = CultivationCapability.get(player);
            if (data == null) {
                continue;
            }
            tickPlayer(player, data, refreshPassives);
        }
    }

    /** 单个玩家的每 tick 推进（服务端）。 */
    public static void tickPlayer(ServerPlayer player, CultivationData data, boolean refreshPassives) {
        if (player == null || data == null) {
            return;
        }
        // 被动：装备即生效，重算是幂等的（固定 UUID 先摘后挂）
        if (refreshPassives) {
            TechniquePassive.apply(player, data.getTechniques());
            PASSIVE_APPLIED.add(player.getUUID());
            // 职业主动：天赋分支满 5 点自动授予 / 洗点后自动收回（幂等）
            CareerSpellGrant.refresh(player, data);
        }

        TechniqueData tech = data.getTechniques();
        if (!tech.isCasting()) {
            return;
        }
        long now = player.level().getGameTime();
        Technique technique = tech.castingTechnique();

        // 脏状态兜底：存档里带出来的、或长时间没推进的蓄力，直接丢掉
        if (technique == null || !tech.castStillRunning(now) || !tech.has(technique)) {
            tech.endCast();
            endCastSnapshot(player);
            com.hongmeng.xiuxian.network.CastProgressPacket.sendStop(player);
            return;
        }
        String reason = interruptReason(player, technique);
        if (reason != null) {
            tech.endCast();
            endCastSnapshot(player);
            com.hongmeng.xiuxian.network.CastProgressPacket.sendStop(player);
            TechniqueCaster.interruptFx(player, technique);
            player.displayClientMessage(Component.literal(
                    "\u00a7c蓄力被打断\u00a78：" + technique.colored()
                            + " \u00a78（" + reason + "）"), true);
            return;
        }
        if (tech.castFinished(now)) {
            tech.endCast();
            // ⚠ 不能在这里清快照 —— 下面的 resolve 还要用它算射线。
            //   原来这里调的是 endCastSnapshot(player)，那样"锚定施法点"等于没做
            //   （快照在结算前就被删了）。改成结算完再清。
            com.hongmeng.xiuxian.network.CastProgressPacket.sendStop(player);
            ServerLevel level = (ServerLevel) player.level();
            // 诊断：蓄力完成 → 准备结算。玩家反复报告"星陨没反应"，
            // 这条日志能确认"蓄力这一关到底过没过"。
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[cast] 蓄力完成 -> 结算 {}（{} 境）", technique.cn(),
                    data.realm().tier());
            boolean ok;
            try {
                ok = TechniqueCaster.resolve(player, data, technique, level);
            } finally {
                // 结算完再清：resolve 内部用的是这一步之前记下的快照
                endCastSnapshot(player);
            }
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[cast] 结算结果 {} = {}", technique.cn(), ok);
            return;
        }

        // ---- 蓄力中的光效（玩家要求「法术蓄力也要有特效」）----
        //
        // 按进度 0→1 逐渐变浓：脚下阵纹扩张、身周螺旋加速、快满时出现亮芯与收束环。
        // 每 2 tick 一次（10 次/秒）已经足够顺滑，也不至于把粒子预算吃光。
        //
        // 2026-11-01 第三轮：粒子减量后**每 8 tick 补一圈 3D 法阵几何**（环体/环塔/双螺旋），
        // 华丽感交给立体几何而不是粒子堆；法阵铺在脚下，不挡第一人称视线（不遮脸）。
        if (now % 2L == 0L && player.level() instanceof ServerLevel castLevel) {
            double progress = tech.castProgress(now);
            // 每个法术一套独立签名：自己的粒子调色板 + 自己的三种图案组合
            // 蓄力预算按境界：飞升(7) 起拉满（玩家要求：飞升解锁所有特效级别）
            com.hongmeng.xiuxian.technique.SpellVfx.setChargeBudget(
                    com.hongmeng.xiuxian.technique.ChargeStyles.chargeBudget(
                            data.realm().tier()));
            com.hongmeng.xiuxian.technique.SpellSignature.charge(
                    castLevel, castOrigin(player), technique.element(),
                    technique.name(), progress, technique.kind().chargeGrandeur());
            if (now % 12L == 0L) {
                // 3D 蓄力法阵（2026-11-07：8→12 tick，粒子再降后仍保留"看得见在聚气"；
                // 放身前 2.4 格，不挡视线）
                com.hongmeng.xiuxian.technique.SpellFxRuntime.chargeSigil(
                        castLevel, player, technique, progress, (int) now);
            }
        }
    }

    /**
     * 中断判据。
     *
     * <h2>玩家要求：前摇「不会被打断，除非自己走动」</h2>
     *
     * <p>所以判据只剩三类，且都与"被外力打断"无关：</p>
     * <ol>
     *   <li><b>自己移动</b>（超过 {@link #MOVE_TOLERANCE} 格）—— 这是玩家明确要保留的唯一打断；</li>
     *   <li><b>身死</b>（人没了，蓄力自然作废）；</li>
     *   <li><b>换维度</b>（位置基准失效，避免"跨维度还被判成移动"这种误伤）。</li>
     * </ol>
     *
     * <p><b>刻意删掉的两条</b>（原来有，导致"打怪时法术老是被自己的操作打断"）：</p>
     * <ul>
     *   <li><s>受创</s> —— 怪打你一下前摇就没了，等于"被怪物打断"，与要求相反；</li>
     *   <li><s>换手</s> —— 玩家在蓄力时切个物品栏就会取消，属于误伤。</li>
     * </ul>
     *
     * @return 中文原因；不中断返回 null
     */
    private static String interruptReason(ServerPlayer player, Technique technique) {
        Snapshot snap = SNAPSHOT.get(player.getUUID());
        if (snap == null) {
            // 没有基准（换维度/重连）就以当前位置重新记一次，避免误判
            SNAPSHOT.put(player.getUUID(), new Snapshot(player));
            return null;
        }
        // ① **移动不再打断施法**（玩家要求：「移动不会打断施法」）。
        //
        // 原来这里有"位移超过 MOVE_TOLERANCE 就中断"，也就是边跑边放会被打断。
        // 现在彻底去掉这条判据 —— 前摇只会在**身死**（或换维度）时中止。
        // 位置基准仍然每 tick 记一次，方便将来需要时恢复。
        // ② 换维度：位置基准不再有意义，按"重新记一次基准"处理，不算打断
        if (snap.dimension != null
                && !snap.dimension.equals(player.level().dimension().location())) {
            SNAPSHOT.put(player.getUUID(), new Snapshot(player));
            return null;
        }
        // ③ 身死
        if (!player.isAlive()) {
            return "身死";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 登录 / 重生 / 掉线
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        if (data == null) {
            return;
        }
        // 存档里的蓄力状态不跨会话延续；被动立刻挂上
        data.getTechniques().endCast();
        SNAPSHOT.remove(player.getUUID());
        TechniquePassive.apply(player, data.getTechniques());
        PASSIVE_APPLIED.add(player.getUUID());
        // 职业主动：登录时补一次授予（天赋已在存档里读回）
        CareerSpellGrant.refresh(player, data);
        ManaBridge.clampToCap(player, data);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        if (data == null) {
            return;
        }
        data.getTechniques().endCast();
        SNAPSHOT.remove(player.getUUID());
        TechniquePassive.apply(player, data.getTechniques());
        // 职业主动：重生后补一次授予（死亡不掉天赋，正常应为幂等无变化）
        CareerSpellGrant.refresh(player, data);
        // 玩家要求：**复活后灵力恢复满**（原来是接着死前的量，重生第一件事就是没蓝）
        refillMana(player, data);
    }

    /** 把灵力补到上限并同步给客户端。 */
    private static void refillMana(ServerPlayer player, CultivationData data) {
        try {
            data.setMana(data.maxMana());
            com.hongmeng.xiuxian.network.ManaSyncPacket.send(player, data.getMana());
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 复活补灵力失败：{}", t.toString());
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        // 死亡克隆：新身体上没有我们的修饰符，重新挂一次
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        if (data != null) {
            data.getTechniques().endCast();
            SNAPSHOT.remove(player.getUUID());
            TechniquePassive.apply(player, data.getTechniques());
        }
    }

    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        SNAPSHOT.remove(id);
        PASSIVE_APPLIED.remove(id);
        ManaBridge.clear(id);
        if (event.getEntity() instanceof ServerPlayer player) {
            com.hongmeng.xiuxian.network.CastProgressPacket.sendStop(player);
        }
    }

    /** 已经挂过被动修饰符的玩家数（诊断用）。 */
    public static int passiveAppliedCount() {
        return PASSIVE_APPLIED.size();
    }

    /** 正在蓄力的玩家数（诊断用）。 */
    public static int snapshotCount() {
        return SNAPSHOT.size();
    }
}
