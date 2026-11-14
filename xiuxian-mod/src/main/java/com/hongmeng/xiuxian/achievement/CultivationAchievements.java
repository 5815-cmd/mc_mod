package com.hongmeng.xiuxian.achievement;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>成就的达成判定</b>：每 tick 检查玩家状态，满足条件就发成就。
 *
 * <p>玩家要求「增加和原版一样的成就系统」。原版框架负责"界面 + 弹窗 + 存档"，
 * 这里只负责"什么时候算达成"。</p>
 *
 * <h2>为什么要每 tick 轮询，而不是事件驱动</h2>
 * <p>有些成就是"状态"而不是"事件" —— 例如"修为达到 100 万"（可能是一次性跳上去的，
 * 也可能是慢慢攒的）、"灵根是鸿蒙"（觉醒那一刻就定了）。事件驱动要挂很多钩子而且容易漏；
 * 轮询只要**节流**（每 20 tick 查一次）就几乎不花钱，而且不会漏。</p>
 */
public final class CultivationAchievements {

    /** 已发过的（避免重复触发；原版也会去重，但少跑一趟更好）。 */
    private static final Map<UUID, java.util.Set<String>> DONE = new ConcurrentHashMap<>();

    /** 轮询节流：每 20 tick（1 秒）查一次。 */
    private static final int PERIOD = 20;

    private CultivationAchievements() {
    }

    /** 由 {@code ModEvents} 的玩家 tick 转发。 */
    public static void tick(ServerPlayer player) {
        if (player == null || player.level().getGameTime() % PERIOD != 0L) {
            return;
        }
        try {
            CultivationData data = CultivationCapability.get(player);
            if (data == null || !data.isAwakened()) {
                return;
            }
            java.util.Set<String> done = DONE.computeIfAbsent(player.getUUID(),
                    k -> ConcurrentHashMap.newKeySet());

            int tier = data.realm().tier();
            long cult = data.getCultivation();
            var root = data.getRoot();

            // ---- 觉醒 ----
            award(player, done, "awaken", "初次觉醒");

            // ---- 境界里程碑（每跨一个大境给一个） ----
            for (int t = 2; t <= 10; t++) {
                if (tier >= t) {
                    award(player, done, "realm_" + t, "第 " + t + " 境");
                }
            }

            // ---- 修为里程碑 ----
            long[] marks = {10_000L, 100_000L, 1_000_000L, 10_000_000L};
            String[] markNames = {"cult_1w", "cult_10w", "cult_100w", "cult_1000w"};
            String[] markCn = {"修为十万", "修为百万", "修为千万", "修为亿级"};
            for (int i = 0; i < marks.length; i++) {
                if (cult >= marks[i]) {
                    award(player, done, markNames[i], markCn[i]);
                }
            }

            // ---- 灵根品质 ----
            if (root != null && root.rarity() != null) {
                switch (root.rarity()) {
                    case HEAVEN -> award(player, done, "root_heaven", "天灵根");
                    case INNATE -> award(player, done, "root_innate", "先天灵根");
                    case CHAOS -> award(player, done, "root_chaos", "混沌灵根");
                    case HONGMENG -> award(player, done, "root_hongmeng", "鸿蒙灵根");
                    default -> {
                        // 其余档位不给成就
                    }
                }
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[hongmeng] 成就判定失败：{}", t.toString());
        }
    }

    private static void award(ServerPlayer player, java.util.Set<String> done,
                              String id, String cn) {
        if (done.add(id)) {
            CultivationTrigger.grant(player, id);
            HongmengMod.LOGGER.debug("[成就] {} 达成 {}", player.getName().getString(), cn);
        }
    }

    public static void clear(UUID id) {
        DONE.remove(id);
    }
}
