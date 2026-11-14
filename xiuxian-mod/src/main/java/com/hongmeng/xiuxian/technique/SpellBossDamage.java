package com.hongmeng.xiuxian.technique;

import java.util.Map;

/**
 * <b>法术对首领的百分比伤害上限</b>（每门法术单独设置）。
 *
 * <h2>为什么要这张表</h2>
 * <p>{@code DomainEffectTable.oneShotPercent()} 把所有百分比效果对首领统一封顶
 * <b>2%</b>。这条对<b>领域</b>是必要的 —— 领域每秒都在结算，不封会失控；
 * 但对<b>灭世法术</b>等于把威力砍掉：一发 900 灵力的终极法术打在首领身上只有 2%，
 * 玩家反馈「限制了百分比 boss 受到的伤害就没有灭世法术的威力了」。</p>
 *
 * <h2>做法</h2>
 * <p>不动领域的 2% 封顶，只给法术开一张**单独的表**：命中首领时，
 * 在既有的一次性伤害之外**再补一次**百分比伤害，上限由这里给。</p>
 *
 * <p>不在表里的法术 → 不做百分比结算，行为与改动前完全一致。</p>
 *
 * <p><b>数值口径</b>：全部按"对首领最大生命的百分比"给，并且是**每次施法一次**，
 * 不是每 tick —— 所以即便 25% 也不会变成秒杀首领。想让灭世法术更狠，
 * 直接调这里的数字即可。</p>
 */
public final class SpellBossDamage {

    /** 法术 → 对首领的百分比上限。只放灭世法术，其余一律不结算百分比。 */
    private static final Map<Technique, Double> CAPS = Map.ofEntries(
            Map.entry(Technique.TIAN_YUN_XING_LUO, 0.10D),   // 天陨星落：10%
            Map.entry(Technique.GUI_DAO_PAO_JI, 0.12D),      // 轨道炮击：12%
            Map.entry(Technique.WAN_XIANG_JI_MIE, 0.25D),    // 万象寂灭：25%（鸿蒙专属，最狠）
            Map.entry(Technique.TAI_XU_HEI_DONG, 0.18D),     // 太虚黑洞：18%（持续牵引 + 绞碎）
            Map.entry(Technique.ZHAN_XING, 0.15D),           // 斩星：15%
            Map.entry(Technique.FEN_TIAN_ZHU_HAI, 0.15D)     // 焚天煮海：15%
    );

    private SpellBossDamage() {
    }

    /**
     * 这门法术对首领的百分比上限。
     *
     * @return 上限（0.10 = 10%）；{@code 0} 表示"这门法术不做百分比结算"
     */
    public static double capOf(Technique technique) {
        if (technique == null) {
            return 0.0D;
        }
        Double v = CAPS.get(technique);
        return v == null ? 0.0D : v;
    }

    /** 是不是"对首领有额外百分比"的法术（面板/日志用）。 */
    public static boolean hasBossPercent(Technique technique) {
        return capOf(technique) > 0.0D;
    }
}
