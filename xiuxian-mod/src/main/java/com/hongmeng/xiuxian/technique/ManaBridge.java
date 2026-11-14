package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.ManaTable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 法术侧的灵力出入口。
 *
 * <p><b>灵力池不是法术系统的。</b>权威实现在
 * {@link CultivationData}（{@code getMana / maxMana / spendMana / addMana /
 * refillMana}）与 {@link ManaTable}（各境上限、回复比例、御剑消耗）。
 * 这个类只做两件事：</p>
 *
 * <ol>
 *   <li>把"这一发要多少灵力、够不够、扣多少"收敛成**一个调用点**
 *       （{@link #spend}），法术系统里不出现第二套灵力数字；</li>
 *   <li>把被动对灵力的两个倍率
 *       （灵海无量 = 上限 ×1.25、吐纳诀 = 回复 ×1.5）接到同一条路径上。</li>
 * </ol>
 *
 * <h3>上限加成怎么落地</h3>
 *
 * <p>{@code CultivationData.setMana} 会把数值夹进 {@code [0, ManaTable.maxForTier(tier)]}，
 * 所以"超过基础上限的那一截"写不进 {@code CultivationData.mana}——写进去就被夹掉。</p>
 *
 * <p>于是这里记一小份**溢出账** {@link #overflow}（按玩家 UUID）：
 * 超上限的部分留在账上，读取时与 {@code CultivationData} 的值相加
 * （{@link #mana(ServerPlayer, CultivationData)}）。扣费与回复最后仍然写回
 * {@code CultivationData.spendMana / addMana}，所以客户端灵力条、御剑消耗、
 * 丹药回复看到的都是同一个数——这不是第二套灵力池，只是上限加成的存放处。</p>
 *
 * <p>溢出账是**会话级**的（不写存档）：重连后超出基础上限的零头不保留，
 * 满灵力登录不受影响（{@link #refill} 会一次性补满）。
 * 如果之后 {@code CultivationData.maxMana()} 自己把
 * {@link TechniquePassive#manaCapMultiplier} 乘进去（更干净的做法），
 * 删掉溢出账即可，调用点不用动。</p>
 */
public final class ManaBridge {

    /** 玩家 UUID → 超出基础上限的那部分灵力。 */
    private static final Map<UUID, Integer> OVERFLOW = new HashMap<>();

    private ManaBridge() {
    }

    // ------------------------------------------------------------------
    // 上限
    // ------------------------------------------------------------------

    /** 含被动加成的灵力上限（只需要数据，不依赖玩家实例）。 */
    public static int maxMana(CultivationData data) {
        if (data == null) {
            return 0;
        }
        return effectiveMax(data.maxMana(), data.getTechniques());
    }

    /** 按被动倍率换算上限。 */
    public static int effectiveMax(int baseMax, TechniqueData techniques) {
        double mult = TechniquePassive.manaCapMultiplier(techniques);
        int base = Math.max(0, baseMax);
        if (mult <= 1.0D) {
            return base;
        }
        return Math.max(0, (int) Math.floor(base * mult));
    }

    /**
     * 带<b>天赋加成</b>的灵力上限。
     *
     * <p>⚠ {@code TalentBonus.mana} 以前零调用 —— 天赋树里"灵力上限"的节点
     * 点了没有任何效果，而面板照样显示加成（"看起来在工作"的典型）。</p>
     *
     * <p>加一个带 Player 的重载：需要精确上限的地方（面板、HUD、吃药）走这个。
     * 仍走旧的 {@code maxMana(CultivationData)} 的地方只少了天赋那一份，
     * 不会算错（旧方法内部还是走 {@link #effectiveMax}）。</p>
     */
    public static int maxMana(ServerPlayer player, CultivationData data) {
        int base = maxMana(data);
        if (player == null || base <= 0) {
            return base;
        }
        try {
            double frac = com.hongmeng.xiuxian.talent.TalentBonus.mana(player);
            if (frac != 0.0D) {
                return Math.max(1, (int) Math.floor(base * (1.0D + frac)));
            }
        } catch (Throwable ignored) {
            // 天赋系统不可用 -> 退回基础上限
        }
        return base;
    }

    // ------------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------------

    /** 当前灵力（基础值 + 溢出账），已夹进含加成的上限。 */
    public static int mana(ServerPlayer player, CultivationData data) {
        if (data == null) {
            return 0;
        }
        int cap = maxMana(data);
        int total = Math.max(0, data.getMana()) + overflowOf(player);
        return Math.min(cap, total);
    }

    /** 这一发付得起吗。 */
    public static boolean affordable(ServerPlayer player, CultivationData data, int cost) {
        return cost <= 0 || mana(player, data) >= cost;
    }

    // ------------------------------------------------------------------
    // 扣费 / 回复
    // ------------------------------------------------------------------

    /**
     * 扣除灵力。
     *
     * @return true 表示真的扣成功了（不够就一点都不扣）
     */
    public static boolean spend(ServerPlayer player, CultivationData data, int cost) {
        if (cost <= 0) {
            return true;
        }
        if (data == null || mana(player, data) < cost) {
            return false;
        }
        // 先扣基础值（它自己会夹到 0），不够的部分从溢出账里出
        int base = data.getMana();
        int fromBase = Math.min(base, cost);
        data.spendMana(fromBase);
        int rest = cost - fromBase;
        if (rest > 0) {
            setOverflow(player, Math.max(0, overflowOf(player) - rest));
        }
        return true;
    }

    /** 回复灵力（丹药 / 打坐，不超过含加成的上限）。 */
    public static void add(ServerPlayer player, CultivationData data, double amount) {
        if (data == null || amount <= 0.0D) {
            return;
        }
        int cap = maxMana(data);
        int cur = mana(player, data);
        if (cur >= cap) {
            return;
        }
        int gain = (int) Math.min(Math.floor(amount), cap - cur);
        if (gain <= 0) {
            return;
        }
        int room = Math.max(0, data.maxMana() - data.getMana());
        int toBase = Math.min(room, gain);
        if (toBase > 0) {
            data.addMana(toBase);
        }
        int rest = gain - toBase;
        if (rest > 0) {
            setOverflow(player, overflowOf(player) + rest);
        }
    }

    /** 回满（突破 / 重生）。 */
    public static void refill(ServerPlayer player, CultivationData data) {
        if (data == null) {
            return;
        }
        data.refillMana();
        setOverflow(player, Math.max(0, maxMana(data) - data.getMana()));
    }

    /**
     * 被动装上/卸下、境界变化之后把灵力夹回上限。
     *
     * @return true 表示数值被改动了
     */
    public static boolean clampToCap(ServerPlayer player, CultivationData data) {
        if (data == null) {
            return false;
        }
        int excess = mana(player, data) - maxMana(data);
        if (excess <= 0) {
            return false;
        }
        int fromOverflow = Math.min(overflowOf(player), excess);
        setOverflow(player, overflowOf(player) - fromOverflow);
        int rest = excess - fromOverflow;
        if (rest > 0) {
            data.setMana(Math.max(0, data.getMana() - rest));
        }
        return true;
    }

    /** 登出时清账，避免 UUID 表无限增长（清掉的只是超出基础上限的零头）。 */
    public static void clear(UUID playerId) {
        if (playerId != null) {
            OVERFLOW.remove(playerId);
        }
    }

    // ------------------------------------------------------------------
    // 提示
    // ------------------------------------------------------------------

    /** 统一的"灵力不足"提示。 */
    public static void warnNotEnough(ServerPlayer player, Technique technique, int have) {
        if (player == null || technique == null) {
            return;
        }
        player.displayClientMessage(Component.literal(
                "\u00a7c灵力不足\u00a78：施展 " + technique.colored()
                        + " \u00a78需要 \u00a7b" + technique.manaCost()
                        + " \u00a78点灵力（当前 \u00a7b" + have + "\u00a78）"), true);
    }

    private static int overflowOf(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return OVERFLOW.getOrDefault(player.getUUID(), 0);
    }

    private static void setOverflow(ServerPlayer player, int value) {
        if (player == null) {
            return;
        }
        if (value <= 0) {
            OVERFLOW.remove(player.getUUID());
        } else {
            OVERFLOW.put(player.getUUID(), value);
        }
    }
}
