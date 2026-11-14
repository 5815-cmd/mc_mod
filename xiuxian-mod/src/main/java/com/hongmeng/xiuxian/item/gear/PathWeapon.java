package com.hongmeng.xiuxian.item.gear;

/**
 * <b>道途专属武器</b> —— 玩家要求「每种分类都要有独立的装备」+「武器种类多一点」。
 *
 * <h2>设计：总 DPS 对齐，手感各异</h2>
 * <p>延续"战力平衡但侧重突出"的要求，五把武器的
 * <b>{@link #dpsFactor()}（每秒伤害系数）刻意全部相同</b>，
 * 差别只体现在"怎么打出这个伤害"：</p>
 *
 * <table border="1">
 *   <caption>武器</caption>
 *   <tr><th>武器</th><th>道途</th><th>伤害</th><th>攻速</th><th>DPS</th><th>手感</th></tr>
 *   <tr><td>灵犀剑</td><td>法修</td><td>×1.00</td><td>×1.00</td><td>1.00</td>
 *       <td>平衡，配法术最稳</td></tr>
 *   <tr><td>崩山杵</td><td>体修</td><td>×1.45</td><td>×0.69</td><td>1.00</td>
 *       <td>一下是一下，配激荡爆发</td></tr>
 *   <tr><td>噬魂刃</td><td>魔修</td><td>×1.25</td><td>×0.80</td><td>1.00</td>
 *       <td>偏重，配魔气破防</td></tr>
 *   <tr><td>血饮双刀</td><td>血修</td><td>×0.75</td><td>×1.33</td><td>1.00</td>
 *       <td><b>极快</b>，吸血次数多、血池涨得快</td></tr>
 *   <tr><td>周天轮刃</td><td>阵修</td><td>×0.90</td><td>×1.11</td><td>1.00</td>
 *       <td>快而轻，配剑阵压制</td></tr>
 * </table>
 *
 * <p><b>为什么 DPS 必须对齐</b>：这把武器是玩家<b>天天拿在手里</b>的东西，
 * 只要有一把明显更强，其余四把就没人用，"独立装备"就白做了。
 * 而"侧重"体现在<b>攻速与单次伤害的分配</b>上 —— 快刀配吸血/叠层更赚，
 * 重武器配一次性爆发更赚，这是真实的手感差异，不是数值差异。</p>
 *
 * <h2>为什么血修是双刀</h2>
 * <p>参考《一念逍遥》血修（血炼）：官方明说它是「<b>物理系</b>」，
 * 专属法宝是「<b>双刀、杵、弓和轮刃</b>」，并强调攻势连绵不绝。
 * 所以血修拿<b>双刀（最快）</b>，而不是法杖。</p>
 */
public enum PathWeapon {

    /** 法修 · 灵犀剑：均衡。 */
    FA("灵犀剑", "spirit_sword", 1.00D, 1.00D,
            "均衡之剑 · 与法术配合最稳"),
    /** 体修 · 崩山杵：重击。 */
    TI("崩山杵", "mountain_pestle", 1.45D, 0.69D,
            "重击之杵 · 单次伤害最高，配激荡爆发"),
    /** 魔修 · 噬魂刃：偏重。 */
    MO("噬魂刃", "soul_devourer", 1.25D, 0.80D,
            "沉厉之刃 · 单击偏重，配魔气破防"),
    /** 血修 · 血饮双刀：极快。 */
    XUE("血饮双刀", "blood_twin_blades", 0.75D, 1.33D,
            "双持快刀 · 攻速最快，吸血与血池积累都更快"),
    /** 阵修 · 周天轮刃：快而轻。 */
    ZHEN("周天轮刃", "celestial_chakram", 0.90D, 1.11D,
            "环刃 · 轻快，配剑阵与领域压制");

    private final String cn;
    private final String key;
    private final double damageFactor;
    private final double speedFactor;
    private final String desc;

    PathWeapon(String cn, String key, double damageFactor, double speedFactor,
               String desc) {
        this.cn = cn;
        this.key = key;
        this.damageFactor = damageFactor;
        this.speedFactor = speedFactor;
        this.desc = desc;
    }

    public String cn() {
        return this.cn;
    }

    /** 注册名后缀（`path_weapon_<key>`）。 */
    public String key() {
        return this.key;
    }

    /** 伤害倍率（相对基准剑）。 */
    public double damageFactor() {
        return this.damageFactor;
    }

    /** 攻速倍率（相对基准剑）。 */
    public double speedFactor() {
        return this.speedFactor;
    }

    /**
     * 每秒伤害系数 —— 五把<b>刻意相同</b>。
     *
     * <p>验证器会断言它全部为 1.00，防止有人"顺手加强"某一把。</p>
     */
    public double dpsFactor() {
        return this.damageFactor * this.speedFactor;
    }

    public String desc() {
        return this.desc;
    }

    /** 道途 -> 武器。 */
    public static PathWeapon of(com.hongmeng.xiuxian.talent.Path path) {
        if (path == null) {
            return null;
        }
        for (PathWeapon w : values()) {
            if (w.name().equals(path.name())) {
                return w;
            }
        }
        return null;
    }
}
