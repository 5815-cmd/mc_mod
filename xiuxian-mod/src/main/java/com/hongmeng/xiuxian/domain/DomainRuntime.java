package com.hongmeng.xiuxian.domain;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>道宫领域的限制</b>：时长上限 + 冷却 + 每秒能量消耗。
 *
 * <h2>为什么要有这个类</h2>
 * <p>{@code DomainManager} 里定义了 {@code DOMAIN_DURATION = 20 * 30}，
 * 但<b>全文没有任何地方引用它</b> —— 领域其实<b>可以无限开着</b>：
 * 没有时长上限、没有冷却、没有持续消耗。</p>
 *
 * <p>而领域给的是「领域内同元素功法伤害加成 + 释放速度加成 + 八种元素效果」。
 * 于是到了道印境之后，领域从"本命绝技"退化成<b>永久被动</b> ——
 * 开一次就再也不用管了。</p>
 *
 * <h2>三条限制（都是"可经营"，不是硬惩罚）</h2>
 * <table border="1">
 *   <caption>限制项</caption>
 *   <tr><th>限制</th><th>数值</th><th>作用</th></tr>
 *   <tr><td>时长上限</td><td>30 秒</td><td>逼玩家"选时机开"，而不是一直挂</td></tr>
 *   <tr><td>收起后冷却</td><td>60 秒</td><td>防止"关了立刻再开"绕过时长上限</td></tr>
 *   <tr><td>每秒耗能量</td><td>1 点/秒</td><td>让「道宫能量」从一次性货币变成需要经营的资源</td></tr>
 * </table>
 *
 * <p>注意最后一条的效果：能量上限 9999、凝聚一次 10 点，
 * 但 30 秒领域要烧 30 点 —— 于是"攒能量"和"开领域"开始互相竞争，
 * 杀怪攒来的能量要决定是拿去<b>凝新道宫</b>还是<b>开领域</b>。</p>
 *
 * <h2>⚠ 为什么不用"每秒掉血"这类惩罚</h2>
 * <p>玩家明确要求「可以不用有原本的代价，区别可以在天赋加点上区分出来就行」，
 * 参考作品（凡人修仙篇的体质系统）也是只加不减。
 * 所以三条限制全部是<b>资源管理</b>：玩家随时可以收手，也能靠多打怪续上 ——
 * 是"经营"，不是"挨罚"。</p>
 */
public final class DomainRuntime {

    /** 每个玩家的领域计时。 */
    private static final class State {
        /** 本次已持续 tick。 */
        int elapsed;
        /** 冷却剩余 tick。 */
        int cooldown;
        /** 一秒（20 tick）的扣能量累加器（避免用浮点存能量）。 */
        int drainAccum;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private DomainRuntime() {
    }

    /**
     * 领域控制（{@code TalentBonus.control}）拉长僵直时长。
     *
     * <p>⚠ {@code TalentBonus.control} 以前零调用 —— 天赋点了没用。
     * 这里只拉**时长**不拉等级：等级已经夸张到"走不动"，再加级没意义。</p>
     */
    public static int controlTicks(ServerPlayer player, int baseTicks) {
        if (player == null) {
            return baseTicks;
        }
        try {
            double frac = com.hongmeng.xiuxian.talent.TalentBonus.control(player);
            if (frac > 0.0D) {
                // 封顶 3 倍：别让控制变成无限连
                return Math.max(baseTicks,
                        (int) Math.round(baseTicks * Math.min(3.0D, 1.0D + frac)));
            }
        } catch (Throwable ignored) {
            // 天赋系统不可用 -> 用基础时长
        }
        return baseTicks;
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), k -> new State());
    }

    /** 冷却还剩多少 tick。 */
    public static int cooldownLeft(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, state(player).cooldown);
    }

    /** 领域已持续多少 tick（面板/提示用）。 */
    public static int elapsed(ServerPlayer player) {
        return player == null ? 0 : state(player).elapsed;
    }

    /** 开关领域时记账。 */
    public static void onToggled(ServerPlayer player, boolean active) {
        if (player == null) {
            return;
        }
        State st = state(player);
        if (active) {
            st.elapsed = 0;
            st.drainAccum = 0;
        } else {
            // 收起（无论手动还是超时）→ 进入冷却
            st.cooldown = DomainManager.DOMAIN_COOLDOWN;
            st.elapsed = 0;
            st.drainAccum = 0;
        }
    }

    /**
     * 每 tick 推进（由 {@code ModEvents} 调）。
     *
     * <p>三件事：冷却递减、超时自动收、按秒扣能量。</p>
     */
    public static void tick(ServerPlayer player, CultivationData data) {
        if (player == null || data == null) {
            return;
        }
        State st = state(player);
        if (st.cooldown > 0) {
            st.cooldown--;
        }
        DomainData domains = data.getDomainData();
        if (domains == null || !domains.isDomainActive()) {
            st.elapsed = 0;
            st.drainAccum = 0;
            return;
        }
        st.elapsed++;

        // ---- ① 超时自动收 ----
        if (st.elapsed >= DomainManager.DOMAIN_DURATION) {
            close(player, domains, "\u00a77领域力竭，自行散去。");
            return;
        }

        // ---- ② 按秒扣能量（用整数累加，避免浮点误差攒着攒着少扣一次）----
        st.drainAccum++;
        if (st.drainAccum >= 20) {
            st.drainAccum = 0;
            int spent = domains.spendEnergy(DomainManager.DOMAIN_DRAIN_PER_SECOND);
            if (spent <= 0) {
                close(player, domains, "\u00a7c道宫能量枯竭，领域溃散。");
                return;
            }
        }

        // ---- ③ 剩余时间提示（最后 5 秒每秒报一次）----
        int left = DomainManager.DOMAIN_DURATION - st.elapsed;
        if (left > 0 && left <= 100 && left % 20 == 0) {
            player.displayClientMessage(Component.literal(
                    "\u00a75领域剩余 \u00a7f" + (left / 20) + " \u00a75秒"), true);
            // 视觉：最后 5 秒让地面阵纹闪一下。
            // 只有一行小字的话，玩家往往在领域"突然消失"时才发现。
            com.hongmeng.xiuxian.technique.PathMechanicFx.domainExpiring(player,
                    left / 20);
        }
    }

    /** 统一收场：关掉领域、清束缚效果、进冷却、通报原因。 */
    private static void close(ServerPlayer player, DomainData domains, String reason) {
        domains.setDomainActive(false);
        DomainField.releaseAll(player);
        onToggled(player, false);
        com.hongmeng.xiuxian.network.ModNetworkBridge.sync(player);
        player.sendSystemMessage(Component.literal(reason));
        try {
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 0.9F);
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[domain] 收场音效失败：{}", t.toString());
        }
    }

    /** 面板用的一行状态。 */
    public static String summary(ServerPlayer player, CultivationData data) {
        if (player == null || data == null || data.getDomainData() == null
                || data.getDomainData().domainCount() == 0) {
            return "";
        }
        State st = state(player);
        if (data.getDomainData().isDomainActive()) {
            int left = Math.max(0, DomainManager.DOMAIN_DURATION - st.elapsed);
            return "\u00a7a领域开启中 \u00a77剩余 \u00a7f" + (left / 20) + "s";
        }
        if (st.cooldown > 0) {
            return "\u00a78冷却 " + (st.cooldown / 20) + "s";
        }
        return "\u00a77可开启（能量 \u00a7f" + data.getDomainData().energy() + "\u00a77）";
    }

    public static void forget(UUID uuid) {
        STATES.remove(uuid);
    }
}
