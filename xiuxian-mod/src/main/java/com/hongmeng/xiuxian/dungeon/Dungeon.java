package com.hongmeng.xiuxian.dungeon;

import java.util.ArrayList;
import java.util.List;

/**
 * 副本目录。
 *
 * <p><b>刻意做成纯数据枚举</b>——不含任何 Minecraft 类型，所以离线验证器能直接断言
 * 全部副本（数量、门槛、波次、奖励、难度曲线），不需要注册表引导。</p>
 *
 * <h3>难度与境界挂钩</h3>
 *
 * <p>每座副本有两层难度：</p>
 * <ol>
 *   <li><b>门槛</b>（{@link #minTier()}）——不到这个境界连门都进不去</li>
 *   <li><b>动态缩放</b>——进去之后，怪物的强度按
 *       {@code 进入者境界} 与 {@code 副本基准境界} 的差距再缩放一次
 *       （见 {@link #mobScaleFor(int)}）</li>
 * </ol>
 *
 * <p>这样同一座副本，金丹期进去是苦战，鸿蒙期进去就是割草——
 * 但也不会完全没收益，因为稀有掉落是按<b>副本</b>定的，不随境界缩水。</p>
 */
public enum Dungeon {

    /** 纪元秘境：最早的副本，练气就能进。 */
    EPOCH("epoch_realm", "纪元秘境", 'b', 1, 3, 2, 800, 1.0D,
            "三波守关，纪元残响未尽"),

    /** 妖兽秘境：兽潮无尽，考验清场速度。 */
    BEAST("beast_realm", "妖兽秘境", 'a', 3, 4, 3, 2_500, 1.15D,
            "兽潮无尽，杀到尽头才见出口"),

    /** 魔道巢穴：魔修盘踞，精英与头目密集。 */
    DEMON("demon_realm", "魔道巢穴", '5', 6, 5, 4, 12_000, 1.30D,
            "魔修盘踞之地，血气冲天"),

    /** 虚空裂隙：终局副本，首领坐镇。 */
    VOID("void_realm", "虚空裂隙", 'd', 8, 6, 5, 60_000, 1.50D,
            "虚空侵蚀现实，道主在此等候");

    private final String dimensionId;
    private final String cn;
    private final char colorCode;
    /** 进入所需的**大境序号**。 */
    private final int minTier;
    /** 波次数量。 */
    private final int waves;
    /** 基准境界：第几大境的怪是这个副本的"标准强度"。 */
    private final int baseTier;
    /** 通关奖励的修为（按基准境界算，实际会按进入者缩放）。 */
    private final long rewardCultivation;
    /** 该副本的额外强度系数（越靠后的副本越狠）。 */
    private final double dangerFactor;
    private final String desc;

    Dungeon(String dimensionId, String cn, char colorCode, int minTier, int waves,
            int baseTier, long rewardCultivation, double dangerFactor, String desc) {
        this.dimensionId = dimensionId;
        this.cn = cn;
        this.colorCode = colorCode;
        this.minTier = minTier;
        this.waves = waves;
        this.baseTier = baseTier;
        this.rewardCultivation = rewardCultivation;
        this.dangerFactor = dangerFactor;
        this.desc = desc;
    }

    public String dimensionId() {
        return dimensionId;
    }

    /** 维度注册名的完整形式，例如 {@code hongmeng:beast_realm}。 */
    public String fullDimensionId() {
        return "hongmeng:" + dimensionId;
    }

    public String cn() {
        return cn;
    }

    public char colorCode() {
        return colorCode;
    }

    public String colored() {
        return "\u00a7" + colorCode + cn;
    }

    public int minTier() {
        return minTier;
    }

    public int waves() {
        return waves;
    }

    /** 基准境界：这座副本的怪按第几大境来定基础强度。 */
    public int baseTier() {
        return baseTier;
    }

    /**
     * <b>副本挑战境界</b>：副本怪境界缩放的封顶值 = {@code max(基准境界, 门槛境界)}。
     *
     * <h2>为什么需要它（2026-11-07）</h2>
     * <p>玩家反馈：「一个头目才元婴 1.5 亿的血，我鸿蒙境界居然不能秒杀」。</p>
     *
     * <p>根因：怪的<b>显示境界</b>用的是 {@link #baseTier()}（魔道巢穴 = 元婴），
     * 但 {@code CultivationMonsterEntity} 里真正乘上去的<b>境界缩放</b>却按
     * <b>最近玩家境界</b>（鸿蒙 ×520）—— 元婴头目被撑到一亿多血，而且玩家越强越夸张，
     * "副本分级"完全失效。</p>
     *
     * <p>现在副本内改按本方法封顶：怪最多只涨到"这座副本本来该有的挑战境界"。
     * 门槛境界通常不低于基准境界（魔道：门槛道印 / 基准元婴），取较大者才能保证
     * <b>按门槛境界进来的玩家仍面对原本的难度</b>；超过它的玩家走进来就是
     * "回头刷副本 = 割草"，符合玩家预期。</p>
     */
    public int challengeTier() {
        return Math.max(baseTier, minTier);
    }

    public long rewardCultivation() {
        return rewardCultivation;
    }

    public double dangerFactor() {
        return dangerFactor;
    }

    public String desc() {
        return desc;
    }

    /**
     * <b>门槛的宽容下限</b>：允许比 {@link #minTier()} 低这么多境进入。
     *
     * <h2>为什么要宽容</h2>
     * <p>玩家反馈：「传送门门槛……弄个动态强度或者是调整门槛」
     * 「我们可以设置阶段性难度，怪物强度要对标玩家阶段强度」。</p>
     *
     * <p>原来的门槛是<b>硬墙</b>：差一境都进不去。但"进不去"不是好的答案 ——
     * 玩家会卡在这儿反复刷低级副本，体验很差。
     * 而且 {@code minTier} 与 {@link #baseTier()}（怪的设计基准）本来就是两个数，
     * 中间那段（例如纪元：门槛 1 / 基准 3）本来就允许低境界玩家进来。</p>
     *
     * <p>所以门槛改成"<b>建议 + 宽容下限</b>"：低 {@value} 境以内仍然能进，
     * 但怪会按 {@link #mobScaleFor(int)} 相应变弱，并且进门前会明确提示
     * "低于建议境界"（见 {@link #underTierWarning(int)}）。</p>
     */
    public static final int ENTRY_GRACE = 2;

    /**
     * 怪物强度倍率：把"进入者的境界"映射成缩放系数。
     *
     * <p>以 {@link #baseTier()} 为 1.0（标准强度）：</p>
     * <ul>
     *   <li>进入者境界**低于**基准 —— 怪<b>跟着变弱</b>
     *       （每低一境 ×0.72，最低 0.40），这是"阶段性难度"的关键：</li>
     *   <li>进入者境界**高于**基准 —— 怪跟着涨，但涨得比玩家慢，
     *       所以高境界进去是"轻松但仍有收获"</li>
     * </ul>
     *
     * <p>公式：{@code 1 + (进入者境界 - 基准境界) × 0.35}，下限 <b>0.40</b>，
     * 最后再乘副本自己的 {@link #dangerFactor()}。</p>
     *
     * <p>⚠ 原来说明写着"下限 1.0，避免副本变成送死"，但代码里
     * {@code Math.max(0, ...)} 把<b>低于基准的差值直接吃掉</b>了 ——
     * 于是低境界玩家进来面对的是<b>基准强度</b>的怪，正是玩家说的"被秒杀"。
     * 现在低的那一侧也真的生效（而且下限留 0.40，不至于变成送分）。</p>
     *
     * <p>0.35 / 0.72 这两个系数是刻意的：境界差 3 境，怪只强/弱一倍左右，
     * 不会出现"跨两境进去就被秒"，也不会变成毫无压力。</p>
     */
    public double mobScaleFor(int enterTier) {
        int diff = enterTier - baseTier;
        double scale;
        if (diff >= 0) {
            scale = 1.0D + diff * 0.35D;
        } else {
            // 低于基准：每低一境 ×0.72（≈ 强/弱对称的另一种斜率）
            scale = Math.pow(0.72D, -diff);
        }
        return Math.max(0.40D, scale) * dangerFactor;
    }

    /**
     * 进入者境界是否低于门槛（但仍在宽容范围内）。
     *
     * @return 低于门槛的境数；不低就是 0
     */
    public int tiersBelowGate(int tier) {
        return Math.max(0, minTier - tier);
    }

    /**
     * 低于建议境界时给玩家的提示（进门那一刻显示）。
     *
     * <p>要让玩家<b>知道自己被宽容了</b> —— 否则他会以为"这副本就这么简单"，
     * 也就失去了"该去提升境界"的引导。同时明确告诉他怪已经被相应削弱。</p>
     *
     * @return 提示文本；不需要提示时返回 {@code null}
     */
    public String underTierWarning(int tier) {
        int below = tiersBelowGate(tier);
        if (below <= 0) {
            return null;
        }
        return "\u00a76\u00a7l境界不足 \u00a77· 本秘境建议 \u00a7e"
                + DungeonRealm.tierName(minTier) + "\u00a77，你还差 \u00a7c" + below
                + " \u00a77境。\n\u00a78怪物已按你的境界相应削弱 —— 但收获也会减少。";
    }

    /** 低境界进入时收获的折扣（0.55 起，逐境回补）。 */
    public double rewardPenaltyFor(int enterTier) {
        int below = tiersBelowGate(enterTier);
        return below <= 0 ? 1.0D : Math.max(0.55D, 1.0D - below * 0.15D);
    }

    /**
     * 通关修为奖励：跟副本走，但按进入者境界轻微上浮。
     *
     * <p>这样高境界玩家刷低级副本仍有意义（虽然不多），
     * 又不会出现"刷低级副本比打高级副本收益还高"的倒挂。</p>
     *
     * <p>低于门槛进来的会打折（见 {@link #rewardPenaltyFor(int)}）——
     * 否则低境界玩家可以一直刷高级副本白拿奖励。</p>
     */
    public long rewardFor(int enterTier) {
        double bonus = 1.0D + Math.max(0, enterTier - baseTier) * 0.20D;
        return Math.round(rewardCultivation * bonus * rewardPenaltyFor(enterTier));
    }

    /**
     * 这一境能不能进。<b>门槛是"建议值 + 宽容下限"</b>，不是硬墙。
     *
     * <p>见 {@link #ENTRY_GRACE}：比 {@link #minTier()} 低 2 境以内仍然可以进，
     * 只是怪会相应变弱（{@link #mobScaleFor(int)}）、收获打折
     * （{@link #rewardPenaltyFor(int)}），进门时也会明确提示。</p>
     *
     * <p>为什么不做成硬墙：玩家反馈「弄个动态强度或者是调整门槛」
     * 「我们可以设置阶段性难度」。把人挡在门外 = 玩家只能反复刷低级副本，
     * 体验很差；放进来并把强度对标他的阶段，才是"阶段性难度"。</p>
     */
    public boolean canEnter(int tier) {
        return tier >= minTier - ENTRY_GRACE;
    }

    /** 是否达到**建议**境界（没达到就是被宽容放行的）。 */
    public boolean meetsRecommended(int tier) {
        return tier >= minTier;
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /** 按维度名找副本（传入 {@code beast_realm} 或 {@code hongmeng:beast_realm} 都能认）。 */
    public static Dungeon byDimension(String dimension) {
        if (dimension == null) {
            return null;
        }
        String id = dimension.contains(":") ? dimension.substring(dimension.indexOf(':') + 1)
                : dimension;
        for (Dungeon d : values()) {
            if (d.dimensionId.equals(id)) {
                return d;
            }
        }
        return null;
    }

    /** 按中文名或枚举名找。 */
    public static Dungeon byName(String name) {
        if (name == null) {
            return null;
        }
        for (Dungeon d : values()) {
            if (d.cn.equals(name) || d.name().equalsIgnoreCase(name)) {
                return d;
            }
        }
        return null;
    }

    /** 该境界已经能进的副本。 */
    public static List<Dungeon> availableAt(int tier) {
        List<Dungeon> out = new ArrayList<>();
        for (Dungeon d : values()) {
            if (d.canEnter(tier)) {
                out.add(d);
            }
        }
        return out;
    }

    /** 该境界**新解锁**的副本（升境有获得感）。 */
    public static List<Dungeon> unlockedAt(int tier) {
        List<Dungeon> out = new ArrayList<>();
        for (Dungeon d : values()) {
            if (d.minTier == tier) {
                out.add(d);
            }
        }
        return out;
    }

    /** 最弱的副本，给新手用。 */
    public static Dungeon easiest() {
        Dungeon best = null;
        for (Dungeon d : values()) {
            if (best == null || d.minTier < best.minTier) {
                best = d;
            }
        }
        return best;
    }
}
