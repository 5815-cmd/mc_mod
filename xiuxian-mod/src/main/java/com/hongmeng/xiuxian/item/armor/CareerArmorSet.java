package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.core.RealmStats;
import com.hongmeng.xiuxian.talent.Career;

/**
 * <b>职业甲 10 套的纯数据表</b>——5 职业 × 入门/进阶 2 套 × 4 件 = 40 件。
 *
 * <h2>为什么做成"纯数据 + 纯函数"</h2>
 * <p>本类<b>不引用任何注册表/Minecraft 类型</b>，所以离线验证器可以直接断言：
 * 10 套齐全、护甲系数恰为 0.50 / 0.58、每境数值 < 道袍、2/4 件套表完整。
 * 物品注册（{@code ModCareerArmor}）与资源文件由 R-2 负责，只按本表生成。</p>
 *
 * <h2>动态数值（玩家方案 §5）</h2>
 * <ul>
 *   <li>护甲：{@code 本境标准值 × 0.50（入门） / × 0.58（进阶） / × 0.64（名器）}，道袍是 ×0.70；</li>
 *   <li>生命：每件固定 +4（入门） / +6（进阶） / +8（名器），按 ADDITION 叠
 *       （穿齐 4 件 = +16 / +24 / +32）；</li>
 *   <li>韧性：取<b>同境道袍单件的同一比例</b>——
 *       {@code 道袍单件韧性 × (0.50/0.70)}（入门） / {@code × (0.58/0.70)}（进阶）。
 *       这样它能跟着道袍的指数曲线一起成长，又永远不超过道袍（纯防御仍归道袍）；</li>
 *   <li>物品自身材质防御 = 0，全部由 Handler 动态结算，避免双算。</li>
 * </ul>
 *
 * <h2>套装规则</h2>
 * <ul>
 *   <li>2 件套<b>按套独立</b>：可以 2+2 混搭，两条不同的 2 件套都生效；</li>
 *   <li>4 件套<b>必须穿齐同一套</b>（入门 4 件或进阶 4 件），且与同套 2 件套叠加；</li>
 *   <li>穿错职业：不给套装加成，基础护甲仍生效（不硬禁穿）。</li>
 * </ul>
 */
public enum CareerArmorSet {

    // ---------------- 剑修 ----------------
    JIAN_BASIC(Career.JIAN_XIU, Tier.BASIC, "青锋剑袍", "Azure Blade Robe",
            new Effect[]{new Effect(EffectKind.ATTACK_SPEED, 0.08D)},
            new Effect[]{new Effect(EffectKind.SWORD_INTENT, 0.20D)}),
    JIAN_ADVANCED(Career.JIAN_XIU, Tier.ADVANCED, "剑心道袍", "Swordheart Robe",
            new Effect[]{new Effect(EffectKind.SWORD_INTENT_FULL_DAMAGE, 0.15D)},
            new Effect[]{new Effect(EffectKind.SWORD_QI_RANGE, 0.30D),
                    new Effect(EffectKind.SWORD_AURA, 0.25D)}),

    // ---------------- 血修 ----------------
    XUE_BASIC(Career.XUE_XIU, Tier.BASIC, "血纹战衣", "Bloodrun Vestment",
            new Effect[]{new Effect(EffectKind.LIFESTEAL, 0.08D)},
            new Effect[]{new Effect(EffectKind.BLOOD_MAX, 0.20D)}),
    XUE_ADVANCED(Career.XUE_XIU, Tier.ADVANCED, "血海战衣", "Bloodsea Vestment",
            new Effect[]{new Effect(EffectKind.STAGGER_RATIO, 0.15D)},
            new Effect[]{new Effect(EffectKind.LOW_HP_DAMAGE, 0.25D),
                    new Effect(EffectKind.BLOOD_DASH_CD, 0.20D)}),

    // ---------------- 魔修 ----------------
    MO_BASIC(Career.MO_XIU, Tier.BASIC, "骸骨魔铠", "Bonefiend Armor",
            new Effect[]{new Effect(EffectKind.PENETRATION, 0.10D)},
            new Effect[]{new Effect(EffectKind.DEMON_GAIN, 0.20D)}),
    MO_ADVANCED(Career.MO_XIU, Tier.ADVANCED, "魔渊魔铠", "Abyssal Fiend Armor",
            new Effect[]{new Effect(EffectKind.DEMON_CAP, 0.25D)},
            new Effect[]{new Effect(EffectKind.OVERFLOW_BURST, 0.30D),
                    new Effect(EffectKind.SELF_DAMAGE_CUT, 0.50D)}),

    // ---------------- 鬼修 ----------------
    GUI_BASIC(Career.GUI_XIU, Tier.BASIC, "幽魂道袍", "Wraith Daoist Robe",
            new Effect[]{new Effect(EffectKind.SOUL_MARK_DURATION, 40.0D)},
            new Effect[]{new Effect(EffectKind.GHOST_REALM_RADIUS, 0.20D)}),
    GUI_ADVANCED(Career.GUI_XIU, Tier.ADVANCED, "黄泉鬼袍", "Yellow Springs Robe",
            new Effect[]{new Effect(EffectKind.SOUL_BURST_DAMAGE, 0.20D)},
            new Effect[]{new Effect(EffectKind.PHASE_CD, 0.25D),
                    new Effect(EffectKind.GHOST_SLOW, 0.15D)}),

    // ---------------- 体修 ----------------
    TI_BASIC(Career.TI_XIU, Tier.BASIC, "玄铁重甲", "Darkiron Heavy Armor",
            new Effect[]{new Effect(EffectKind.KNOCKBACK_RESIST, 0.50D)},
            new Effect[]{new Effect(EffectKind.SURGE_GAIN, 0.20D)}),
    TI_ADVANCED(Career.TI_XIU, Tier.ADVANCED, "不灭金身", "Undying Golden Body",
            new Effect[]{new Effect(EffectKind.LAST_STAND_TICKS, 20.0D)},
            new Effect[]{new Effect(EffectKind.SURGE_BURST, 0.30D),
                    new Effect(EffectKind.SHOCK_RADIUS, 0.20D)}),

    // ================= C 扩充：每职业第三套「名器」 =================
    JIAN_MASTER(Career.JIAN_XIU, Tier.MASTER, "剑仙羽衣", "Sword Immortal Feather Robe",
            new Effect[]{new Effect(EffectKind.ATTACK_SPEED, 0.10D)},
            new Effect[]{new Effect(EffectKind.SWORD_QI_RANGE, 0.40D),
                    new Effect(EffectKind.SWORD_AURA, 0.35D)}),
    XUE_MASTER(Career.XUE_XIU, Tier.MASTER, "血神袍", "Blood God Robe",
            new Effect[]{new Effect(EffectKind.LIFESTEAL, 0.10D)},
            new Effect[]{new Effect(EffectKind.LOW_HP_DAMAGE, 0.35D),
                    new Effect(EffectKind.BLOOD_MAX, 0.30D)}),
    MO_MASTER(Career.MO_XIU, Tier.MASTER, "天魔甲", "Heavenly Demon Armor",
            new Effect[]{new Effect(EffectKind.PENETRATION, 0.15D)},
            new Effect[]{new Effect(EffectKind.DEMON_CAP, 0.35D),
                    new Effect(EffectKind.OVERFLOW_BURST, 0.40D)}),
    GUI_MASTER(Career.GUI_XIU, Tier.MASTER, "幽冥纱", "Netherworld Shroud",
            new Effect[]{new Effect(EffectKind.SOUL_MARK_DURATION, 60.0D)},
            new Effect[]{new Effect(EffectKind.SOUL_BURST_DAMAGE, 0.30D),
                    new Effect(EffectKind.PHASE_CD, 0.35D)}),
    TI_MASTER(Career.TI_XIU, Tier.MASTER, "不灭圣铠", "Undying Sacred Armor",
            new Effect[]{new Effect(EffectKind.KNOCKBACK_RESIST, 0.70D)},
            new Effect[]{new Effect(EffectKind.SURGE_BURST, 0.45D),
                    new Effect(EffectKind.SHOCK_RADIUS, 0.30D)});

    /** 入门 / 进阶 / 名器（C 扩充）。 */
    public enum Tier {
        /** 入门：标准值 ×0.50，每件固定 +4 生命（穿齐 +16）。 */
        BASIC(1, "初阶", 0.50D, 4.0D),
        /** 进阶：标准值 ×0.58，每件固定 +6 生命（穿齐 +24）。 */
        ADVANCED(2, "进阶", 0.58D, 6.0D),
        /** 名器：标准值 ×0.64，每件固定 +8 生命（穿齐 +32；护甲仍低于道袍 0.70）。 */
        MASTER(3, "名器", 0.64D, 8.0D);

        private final int iv;
        private final String cn;
        private final double armorFactor;
        private final double healthBonus;

        Tier(int iv, String cn, double armorFactor, double healthBonus) {
            this.iv = iv;
            this.cn = cn;
            this.armorFactor = armorFactor;
            this.healthBonus = healthBonus;
        }

        /** 物品 ID 里的档位数字（1 = 入门，2 = 进阶）。 */
        public int iv() {
            return this.iv;
        }

        public String cn() {
            return this.cn;
        }

        /** 相对本境标准值的护甲系数（0.50 / 0.58）。 */
        public double armorFactor() {
            return this.armorFactor;
        }

        /** 每件生命加成（固定点数，ADDITION；穿齐 4 件 = ×4）。 */
        public double healthBonus() {
            return this.healthBonus;
        }

        /** 相对道袍的韧性比例（0.50/0.70、0.58/0.70）——永远低于道袍。 */
        public double toughnessRatio() {
            return this.armorFactor / 0.70D;
        }

        /**
         * 每件韧性：取同境道袍单件韧性的同一比例。
         *
         * <p>道袍韧性是<b>指数成长</b>（第 1 境 0.4 → 第 10 境 140），
         * 所以职业甲不能拍一个线性常数，否则低境超过道袍、高境等于没有。
         * 按比例取既跟得上曲线，又永远低于道袍。</p>
         */
        public double toughnessAt(int realmTier) {
            double dao = DaoArmorTier.of(
                    Math.max(1, Math.min(10, realmTier))).toughness();
            return dao * toughnessRatio();
        }
    }

    /**
     * 套装效果的种类。
     *
     * <p>数值单位由种类决定：比例类 = 小数；{@link #SOUL_MARK_DURATION} /
     * {@link #LAST_STAND_TICKS} = tick。运行期读取时按种类解释。</p>
     */
    public enum EffectKind {
        /** 攻速（比例）。 */
        ATTACK_SPEED,
        /** 剑意获取（比例）。 */
        SWORD_INTENT,
        /** 剑意满层时伤害（比例）。 */
        SWORD_INTENT_FULL_DAMAGE,
        /** 剑气范围（比例）。 */
        SWORD_QI_RANGE,
        /** 剑罡吸收率（比例）。 */
        SWORD_AURA,
        /** 吸血（比例）。 */
        LIFESTEAL,
        /** 血池上限（比例）。 */
        BLOOD_MAX,
        /** 缓伤转化比例（比例）。 */
        STAGGER_RATIO,
        /** 低血增伤（比例）。 */
        LOW_HP_DAMAGE,
        /** 血遁冷却缩减（比例）。 */
        BLOOD_DASH_CD,
        /** 破防（比例）。 */
        PENETRATION,
        /** 魔气获取（比例）。 */
        DEMON_GAIN,
        /** 魔气上限（比例）。 */
        DEMON_CAP,
        /** 满溢爆发（比例）。 */
        OVERFLOW_BURST,
        /** 满溢自伤削减（比例）。 */
        SELF_DAMAGE_CUT,
        /** 魂印持续时长（tick）。 */
        SOUL_MARK_DURATION,
        /** 鬼域范围（比例）。 */
        GHOST_REALM_RADIUS,
        /** 魂印引爆伤害（比例）。 */
        SOUL_BURST_DAMAGE,
        /** 虚化冷却缩减（比例）。 */
        PHASE_CD,
        /** 鬼域减速（比例）。 */
        GHOST_SLOW,
        /** 抗击退（比例）。 */
        KNOCKBACK_RESIST,
        /** 激荡获取（比例）。 */
        SURGE_GAIN,
        /** 锁血时长（tick）。 */
        LAST_STAND_TICKS,
        /** 激荡引爆（比例）。 */
        SURGE_BURST,
        /** 震荡范围（比例）。 */
        SHOCK_RADIUS
    }

    /** 一条套装效果。 */
    public record Effect(EffectKind kind, double value) {
    }

    /** 四件顺序（与原版/既有护甲一致）。 */
    public static final String[] SLOTS = {"helmet", "chestplate", "leggings", "boots"};

    private final Career career;
    private final Tier tier;
    private final String suitCn;
    private final String suitEn;
    private final Effect[] setTwo;
    private final Effect[] setFour;

    CareerArmorSet(Career career, Tier tier, String suitCn, String suitEn,
                   Effect[] setTwo, Effect[] setFour) {
        this.career = career;
        this.tier = tier;
        this.suitCn = suitCn;
        this.suitEn = suitEn;
        this.setTwo = setTwo;
        this.setFour = setFour;
    }

    public Career career() {
        return this.career;
    }

    public Tier tier() {
        return this.tier;
    }

    public String suitCn() {
        return this.suitCn;
    }

    public String suitEn() {
        return this.suitEn;
    }

    public Effect[] setTwo() {
        return this.setTwo.clone();
    }

    public Effect[] setFour() {
        return this.setFour.clone();
    }

    /** 第某境界时，整套 4 件的护甲值总和（四件之和恒等于 round(标准值 × 系数)）。 */
    public int totalArmorAt(int realmTier) {
        long std = RealmStats.standard(Math.max(1, Math.min(10, realmTier)));
        return (int) Math.round(std * this.tier.armorFactor());
    }

    /** 第某境界时，单件护甲值（小数，Handler 直接累加，不取整）。 */
    public double armorPerPieceAt(int realmTier) {
        return this.totalArmorAt(realmTier) / 4.0D;
    }

    /** 第某境界时，单件韧性。 */
    public double toughnessPerPieceAt(int realmTier) {
        return this.tier.toughnessAt(realmTier);
    }

    /** 单件生命加成点数（固定 4/6/8，ADDITION；穿齐 4 件 = 16/24/32）。 */
    public double healthBonus() {
        return this.tier.healthBonus();
    }

    /**
     * 物品 ID（R-2 注册用）：{@code career_armor_<职业短名>_<档位>_<部位>}。
     * 例：{@code career_armor_jian_1_helmet}。
     */
    public String itemName(String slot) {
        return "career_armor_" + this.career.id() + "_" + this.tier.iv() + "_" + slot;
    }

    /** 按职业 + 档位找套装。 */
    public static CareerArmorSet of(Career career, Tier tier) {
        for (CareerArmorSet s : values()) {
            if (s.career == career && s.tier == tier) {
                return s;
            }
        }
        return null;
    }

    /** 某职业的三套（入门、进阶、名器）。 */
    public static CareerArmorSet[] ofCareer(Career career) {
        return new CareerArmorSet[]{of(career, Tier.BASIC),
                of(career, Tier.ADVANCED), of(career, Tier.MASTER)};
    }
}
