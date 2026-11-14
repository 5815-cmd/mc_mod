package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.network.AttackAnimPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>攻击动作的服务端部分</b>：判定武器分型、推进连击、广播给周围玩家。
 *
 * <h2>为什么要服务端做</h2>
 * <p>武器类型与连击序号必须<b>两端一致</b>：如果客户端自己猜，别人看你攻击时
 * 会因为你没拿同样武器而播错招式。所以由服务端判一次，广播给 48 格内的玩家
 * （含自己，用于第三人称看自己）。</p>
 *
 * <h2>武器专属连招（本轮新增）</h2>
 * <p>原来五把武器用<b>同一张</b>倍率表，于是"剑走轻灵、刀走刚猛"只是外观差异。
 * 现在每条道途有自己的<b>连段长度、节奏窗口、倍率曲线</b>，但
 * <b>平均倍率完全相同（都是 1.100）</b> —— 差异化在"怎么打"，不在"谁更强"。</p>
 *
 * <table border="1">
 *   <caption>五条道途的连招</caption>
 *   <tr><th>道途</th><th>武器</th><th>段数</th><th>倍率</th><th>节奏</th><th>平均</th></tr>
 *   <tr><td>法修</td><td>剑</td><td>3</td><td>1.00 / 1.06 / 1.24</td>
 *       <td>均衡</td><td>1.100</td></tr>
 *   <tr><td>体修</td><td>杵</td><td>2</td><td>0.95 / 1.25</td>
 *       <td>短而重，窗口最长</td><td>1.100</td></tr>
 *   <tr><td>魔修</td><td>刃</td><td>4</td><td>1.00 / 1.04 / 1.08 / 1.28</td>
 *       <td>快连，窗口最短</td><td>1.100</td></tr>
 *   <tr><td>血修</td><td>双刀</td><td>3</td><td>1.10 / 1.10 / 1.10</td>
 *       <td>平稳输出，无爆发</td><td>1.100</td></tr>
 *   <tr><td>阵修</td><td>轮刃</td><td>5</td><td>1.00 / 1.02 / 1.04 / 1.06 / 1.38</td>
 *       <td>最长连段，终结最重</td><td>1.100</td></tr>
 * </table>
 *
 * <p><b>平均倍率必须相同</b>：这是项目一贯的设计原则 ——
 * 五条道途只该在"手感与玩法"上不同，不该有强弱。</p>
 */
public final class CombatAnim {

    /** 默认连击窗口（tick）：没有专属武器时用。 */
    public static final long COMBO_WINDOW = 20L;

    /** 广播半径（格）。超出这个距离的玩家收不到动作包（省带宽）。 */
    public static final double BROADCAST_RANGE = 48.0D;

    /** 武器分型（与客户端 {@code CombatAnimClient.KIND_*} 一一对应）。 */
    public static final int KIND_SWORD = 0;
    public static final int KIND_HEAVY = 1;
    public static final int KIND_STAB = 2;
    public static final int KIND_TWIN = 3;
    public static final int KIND_RING = 4;

    /**
     * 五张连招表（顺序与 {@code KIND_*} 一致）。
     *
     * <p>所有表的<b>平均值都等于 1.100</b> —— 用 {@code averageOf} 验证过。
     * 改任何一张表都必须重新配平，否则某条道途会凭空强一截。</p>
     */
    private static final double[][] COMBO_TABLES = {
            // 0 法修·剑：均衡三段
            {1.00D, 1.06D, 1.24D},
            // 1 体修·杵：两段重击（每下都沉）
            {0.95D, 1.25D},
            // 2 魔修·刃：四段快连，最后一下爆发
            {1.00D, 1.04D, 1.08D, 1.28D},
            // 3 血修·双刀：三段等额，平稳输出
            {1.10D, 1.10D, 1.10D},
            // 4 阵修·轮刃：五段，终结最重
            {1.00D, 1.02D, 1.04D, 1.06D, 1.38D},
    };

    /**
     * 五张节奏窗口（tick）。
     *
     * <p>重武器挥得慢 → 窗口给宽；快武器 → 窗口收紧（必须打得更连贯）。
     * 这是"手感差异"的主要来源，不影响总伤害。</p>
     */
    private static final long[] COMBO_WINDOWS = {
            20L,   // 剑
            26L,   // 杵（最长：挥一锤要时间）
            16L,   // 刃（最短：快刺要连着）
            18L,   // 双刀
            22L,   // 轮刃
    };

    // ------------------------------------------------------------------
    // 贪刀惩罚（连招不是无风险操作）
    // ------------------------------------------------------------------

    /** 收招硬直：上一击之后这么多 tick 内算"还在收招"。 */
    public static final long RECOVERY_TICKS = 10L;

    /** 贪刀时额外吃的伤害比例（提醒，不是处决）。 */
    public static final double GREEDY_EXTRA = 0.18D;

    /** 贪刀后的减速时长（tick）。 */
    public static final int GREEDY_STAGGER_TICKS = 24;

    /**
     * 是否处于"贪刀"状态（收招中途，且已经挥出第二下以上）。
     *
     * <p>第一下不算 —— 那是"起手"，没有贪可言。
     * 惩罚该落在"你还想再来一下"的决策上。</p>
     */
    public static boolean greedy(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        long[] st = LAST.get(player.getUUID());
        if (st == null) {
            return false;
        }
        long now = player.level().getGameTime();
        return now - st[0] <= RECOVERY_TICKS && st[1] > 0L;
    }

    /**
     * 断连（被抓住破绽时调用）。
     *
     * <p>这是贪刀惩罚里<b>最痛的一下</b> —— 刚攒到终结技，被拍一下全没了。</p>
     */
    public static void breakCombo(ServerPlayer player) {
        if (player == null) {
            return;
        }
        long[] st = LAST.get(player.getUUID());
        if (st != null) {
            st[0] = -9999L;
            st[1] = -1L;
        }
    }

    /** 每个玩家：{上一次攻击 tick, 连击序号}。 */
    private static final Map<UUID, long[]> LAST = new ConcurrentHashMap<>();

    private CombatAnim() {
    }

    /** 这张表的平均倍率（用于配平断言）。 */
    public static double averageOf(int kind) {
        double[] t = tableOf(kind);
        double s = 0.0D;
        for (double v : t) {
            s += v;
        }
        return s / t.length;
    }

    /** 某分型的连招表。 */
    public static double[] tableOf(int kind) {
        return COMBO_TABLES[Math.floorMod(kind, COMBO_TABLES.length)];
    }

    /** 某分型的连击窗口（tick）。 */
    public static long windowOf(int kind) {
        return COMBO_WINDOWS[Math.floorMod(kind, COMBO_WINDOWS.length)];
    }

    /** 某分型的连段长度。 */
    public static int lengthOf(int kind) {
        return tableOf(kind).length;
    }

    /**
     * 按主手武器判定动作分型。
     *
     * <p>五条道途的专属武器各有自己的招式：剑横扫 / 杵重砸 / 刃突刺 /
     * 双刀左右交替 / 轮刃旋身。非道途武器（通用法宝剑、原版剑斧）统一按剑处理 ——
     * 这样"没有专属武器"的玩家也能看到动作，不会觉得系统没生效。</p>
     */
    public static int kindOf(ServerPlayer player) {
        if (player == null) {
            return KIND_SWORD;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return KIND_SWORD;
        }
        try {
            if (held.getItem() instanceof com.hongmeng.xiuxian.item.gear.ModPathWeapon
                    .PathWeaponItem pw) {
                return switch (pw.weapon()) {
                    case TI -> KIND_HEAVY;
                    case MO -> KIND_STAB;
                    case XUE -> KIND_TWIN;
                    case ZHEN -> KIND_RING;
                    case FA -> KIND_SWORD;
                };
            }
        } catch (Throwable ignored) {
            // 武器不可判定就按剑
        }
        return KIND_SWORD;
    }

    /**
     * 当前连击序号，<b>不推进</b>。
     *
     * <p>给伤害结算用：{@code AttackEntityEvent} 先跑（推进了序号），
     * 之后才是 {@code LivingHurtEvent} —— 所以那时候读到的是<b>这一下</b>的序号。</p>
     */
    public static int comboOf(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        long[] st = LAST.get(player.getUUID());
        if (st == null) {
            return 0;
        }
        return (int) Math.max(0L, st[1]);
    }

    /**
     * 这一下的伤害倍率（按武器分型 + 连击序号查表）。
     *
     * <p>这是<b>唯一的</b>对外入口 —— 伤害结算不该再去摸数组下标，
     * 否则连段长度不同的武器会越界。</p>
     */
    public static double comboMultiplier(ServerPlayer player) {
        int kind = kindOf(player);
        double[] table = tableOf(kind);
        int idx = Math.min(comboOf(player), table.length - 1);
        return table[idx];
    }

    /** 这一下是不是终结技（连段的最后一段）。 */
    public static boolean isFinisher(ServerPlayer player) {
        int kind = kindOf(player);
        return comboOf(player) >= lengthOf(kind) - 1;
    }

    /** 推进连击并广播。 */
    public static int onAttack(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return 0;
        }
        int kind = kindOf(player);
        long window = windowOf(kind);
        int len = lengthOf(kind);

        long now = player.level().getGameTime();
        long[] st = LAST.computeIfAbsent(player.getUUID(), k -> new long[]{-9999L, -1L});
        long lastTick = st[0];
        long combo = st[1];
        // 超出窗口就从头再来；否则在**该武器的连段长度**内递增
        combo = (now - lastTick <= window) ? ((combo + 1) % len) : 0;
        st[0] = now;
        st[1] = combo;

        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return (int) combo;
        }
        AttackAnimPacket pkt = new AttackAnimPacket(player.getUUID(), kind, (int) combo);
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(player) <= BROADCAST_RANGE * BROADCAST_RANGE) {
                ModNetwork.sendToPlayer(other, pkt);
            }
        }
        return (int) combo;
    }

    /** 掉线清账。 */
    public static void forget(UUID id) {
        if (id != null) {
            LAST.remove(id);
        }
    }
}
