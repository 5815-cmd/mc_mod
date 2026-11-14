package com.hongmeng.xiuxian.item.gear;

/**
 * <b>通用双手武器五型</b>（06 §2 / 08 §2）——R-2 数据契约。
 *
 * <p>五型全部 <b>双手</b>（持有时副手失效，由 R-3 的 {@code OffhandGate} 接线）、
 * <b>不锁职业</b>；DPS 全部对齐 1.00，差异只在
 * 「单次 vs 频率 / 距离 / 群伤 / 动作分型」。</p>
 *
 * <p>数值口径：攻击 = {@code RealmStats.standard(阶) × damageFactor}；
 * 攻速由 {@code speedFactor} 通过属性修饰符施加。</p>
 */
public enum WeaponForm {

    /** 长剑：均衡、连招顺，单体。 */
    SWORD("sword", "长剑", "SWORD", 1.00D, 1.00D, 3.0D, false),
    /** 巨剑：高单伤、击退，小扇形。 */
    GREATSWORD("greatsword", "巨剑", "HEAVY", 1.43D, 0.70D, 3.2D, true),
    /** 镰刀：多目标、割裂，大扇形。 */
    SCYTHE("scythe", "镰刀", "SCYTHE", 1.18D, 0.85D, 3.4D, true),
    /** 长枪：远、窄、直线贯穿。 */
    SPEAR("spear", "长枪", "STAB", 1.11D, 0.90D, 4.0D, false),
    /** 棍：快、击退/踉跄，小扇形。 */
    STAFF("staff", "棍", "STAFF", 0.91D, 1.10D, 3.2D, false),
    /**
     * 战镰（C 扩充第 6 型）：刀身沿杆同向、外侧开刃，重劈/大扇形；
     * DPS 仍对齐 1.00（1.30 × 0.77 ≈ 1.001）。
     */
    WAR_SCYTHE("war_scythe", "战镰", "SCYTHE", 1.30D, 0.77D, 3.6D, true);

    private final String key;
    private final String cn;
    private final String action;
    private final double damageFactor;
    private final double speedFactor;
    private final double reach;
    private final boolean sweeping;

    WeaponForm(String key, String cn, String action, double damageFactor,
               double speedFactor, double reach, boolean sweeping) {
        this.key = key;
        this.cn = cn;
        this.action = action;
        this.damageFactor = damageFactor;
        this.speedFactor = speedFactor;
        this.reach = reach;
        this.sweeping = sweeping;
    }

    public String key() {
        return key;
    }

    public String cn() {
        return cn;
    }

    /** 动作分型（SWORD / HEAVY / SCYTHE / STAB / STAFF）。 */
    public String action() {
        return action;
    }

    public double damageFactor() {
        return damageFactor;
    }

    public double speedFactor() {
        return speedFactor;
    }

    /** 设计距离（格），R-3 接攻击距离时用。 */
    public double reach() {
        return reach;
    }

    /** 是否小/大扇形群伤（R-3 接判定时用）。 */
    public boolean sweeping() {
        return sweeping;
    }

    /** 全部五型都是双手：持有时副手失效（R-3 接线）。 */
    public boolean twoHanded() {
        return true;
    }

    /** 按 key（sword/greatsword/...）找型；找不到返回 {@link #SWORD}。 */
    public static WeaponForm byKey(String key) {
        if (key != null) {
            for (WeaponForm f : values()) {
                if (f.key.equalsIgnoreCase(key)) {
                    return f;
                }
            }
        }
        return SWORD;
    }
}
