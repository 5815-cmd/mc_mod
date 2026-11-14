package com.hongmeng.xiuxian.talent;

/**
 * <b>职业被动（30 条 = 5 职业 × 6 条）</b>——独立系统，<b>不占 52 门法术的 5 个被动槽</b>。
 *
 * <h2>解锁规则（不占天赋节点）</h2>
 * <ul>
 *   <li>每职业 <b>2~3 条核心被动</b>：职业自带，{@code unlockPoints = 0}；</li>
 *   <li>其余 3~4 条：挂在 4 支天赋分支上，
 *       该支累计投入 {@code 2} 点解锁第 1 条、{@code 4} 点解锁第 2 条；</li>
 *   <li>每支投入 5 点解锁该支<b>主动</b>（见 {@link CareerActive}）。</li>
 * </ul>
 * <p>这样"6 被动 + 4 主动"不需要占 8 个天赋节点，节点只放数值。</p>
 *
 * <h2>生效</h2>
 * <p>全部效果走已有结算口（{@code CareerRuntime} / {@code ModEvents} /
 * {@code TalentAttributes}），不新造第二套战斗结算。属性型用固定 UUID 修饰符；
 * 事件型挂已有钩子。数据类本身<b>不引用任何 Minecraft 类型</b>，可离线断言。</p>
 */
public enum CareerSkill {

    // ================= 剑修 =================

    /** 剑修核心：剑意。 */
    JIAN_YI(Career.JIAN_XIU, CareerBranch.JIAN_INTENT, 0.0D, "剑意",
            "命中叠层：每层近战伤害 +1.5%，满 10 层下一次命中放剑气并清空",
            EffectKind.CORE_MECHANIC, 0.0D),
    /** 剑修核心：剑罡。 */
    JIAN_GANG(Career.JIAN_XIU, CareerBranch.JIAN_AURA, 0.0D, "剑罡",
            "受伤时以灵力化盾；灵力空时更容易受伤",
            EffectKind.CORE_MECHANIC, 0.0D),
    JIAN_XIN(Career.JIAN_XIU, CareerBranch.JIAN_INTENT, 2.0D, "剑心通明",
            "剑意获取 +10%，暴击率提升", EffectKind.SWORD_INTENT, 0.10D),
    JIAN_SHI(Career.JIAN_XIU, CareerBranch.JIAN_AURA, 2.0D, "剑势",
            "受击时凝聚 1 层剑意（5 秒一次），越被压制越能反打",
            EffectKind.SWORD_MOMENTUM, 0.15D),
    JIAN_FENG(Career.JIAN_XIU, CareerBranch.JIAN_QI, 2.0D, "锋芒",
            "近战护甲穿透 +10%", EffectKind.PENETRATION, 0.10D),
    JIAN_YU(Career.JIAN_XIU, CareerBranch.JIAN_YU, 2.0D, "御剑",
            "移速提升，可短距御剑飞行/突进", EffectKind.MOVE_SPEED, 0.10D),

    // ================= 血修 =================

    /** 血修核心：血池。 */
    XUE_POOL(Career.XUE_XIU, CareerBranch.XUE_POOL, 0.0D, "血池",
            "造成/受到伤害积攒血池，可用于回血与技能消耗",
            EffectKind.CORE_MECHANIC, 0.0D),
    /** 血修核心：缓伤。 */
    XUE_STAGGER(Career.XUE_XIU, CareerBranch.XUE_STAGGER, 0.0D, "缓伤",
            "受创伤害延后 30% 结算，残血自动回血", EffectKind.CORE_MECHANIC, 0.0D),
    XUE_SHI(Career.XUE_XIU, CareerBranch.XUE_LEECH, 2.0D, "嗜血",
            "近战吸血 +5%", EffectKind.LIFESTEAL, 0.05D),
    XUE_NU(Career.XUE_XIU, CareerBranch.XUE_STAGGER, 2.0D, "血怒",
            "低血时增伤 +15%", EffectKind.LOW_HP_DAMAGE, 0.15D),
    XUE_HEN(Career.XUE_XIU, CareerBranch.XUE_STEP, 2.0D, "血痕",
            "移动留下血痕，区域减速/微伤", EffectKind.AURA_SLOW, 0.10D),
    XUE_CHANG(Career.XUE_XIU, CareerBranch.XUE_POOL, 2.0D, "血偿",
            "受击时反伤/回补血池", EffectKind.HEAL_ON_HURT, 0.03D),

    // ================= 魔修 =================

    /** 魔修核心：魔气。 */
    MO_QI(Career.MO_XIU, CareerBranch.MO_DEMON, 0.0D, "魔气",
            "造成伤害攒魔气，增伤 + 破防；满仓溢出伤自己",
            EffectKind.CORE_MECHANIC, 0.0D),
    /** 魔修核心：破防。 */
    MO_PO(Career.MO_XIU, CareerBranch.MO_PEN, 0.0D, "破防",
            "攻击无视部分护甲", EffectKind.CORE_MECHANIC, 0.0D),
    MO_YI(Career.MO_XIU, CareerBranch.MO_DEMON, 2.0D, "魔溢",
            "魔气满仓时爆发增伤 +20%（自伤仍保留）",
            EffectKind.DEMON_OVERFLOW_BURST, 0.20D),
    MO_SHI(Career.MO_XIU, CareerBranch.MO_PEN, 2.0D, "噬魂",
            "击杀回魔气", EffectKind.DEMON_KILL_GAIN, 20.0D),
    MO_XIN(Career.MO_XIU, CareerBranch.MO_BURST, 2.0D, "心魔",
            "负面抗性 +20%，反噬敌人", EffectKind.CONTROL_RESIST, 0.20D),
    MO_WEI(Career.MO_XIU, CareerBranch.MO_HEART, 2.0D, "魔威",
            "周围敌人被削弱/恐惧", EffectKind.ENEMY_WEAKEN, 0.10D),

    // ================= 鬼修 =================

    /** 鬼修核心：魂印。 */
    GUI_MARK(Career.GUI_XIU, CareerBranch.GUI_MARK, 0.0D, "魂印",
            "攻击叠印，3 层引爆额外伤害", EffectKind.CORE_MECHANIC, 0.0D),
    /** 鬼修核心：鬼域。 */
    GUI_REALM(Career.GUI_XIU, CareerBranch.GUI_REALM, 0.0D, "鬼域",
            "展开鬼域，持续 DoT + 减速", EffectKind.CORE_MECHANIC, 0.0D),
    /** 鬼修核心：虚化。 */
    GUI_PHASE(Career.GUI_XIU, CareerBranch.GUI_PHASE, 0.0D, "虚化",
            "致死伤害时免死并短暂无敌（冷却长）", EffectKind.CORE_MECHANIC, 0.0D),
    GUI_SUO(Career.GUI_XIU, CareerBranch.GUI_MARK, 2.0D, "锁魂",
            "击杀带印目标回魂印/掉魂粒子", EffectKind.SOUL_KILL_GAIN, 15.0D),
    GUI_YIN(Career.GUI_XIU, CareerBranch.GUI_REALM, 2.0D, "阴气",
            "攻击附带减速/虚弱", EffectKind.SLOW_ON_HIT, 0.10D),
    GUI_BAO(Career.GUI_XIU, CareerBranch.GUI_BURST, 2.0D, "魂爆",
            "魂印引爆时产生范围伤害", EffectKind.SOUL_BURST_AOE, 0.30D),

    // ================= 体修 =================

    /** 体修核心：激荡。 */
    TI_JI(Career.TI_XIU, CareerBranch.TI_SURGE, 0.0D, "激荡",
            "受击攒能量，满 100 引爆", EffectKind.CORE_MECHANIC, 0.0D),
    /** 体修核心：锁血。 */
    TI_SUO(Career.TI_XIU, CareerBranch.TI_STAND, 0.0D, "锁血",
            "致死伤害锁血 2 秒", EffectKind.CORE_MECHANIC, 0.0D),
    TI_GU(Career.TI_XIU, CareerBranch.TI_BODY, 2.0D, "铁骨",
            "抗击退 +25%，减伤 +3%", EffectKind.KNOCKBACK_RESIST, 0.25D),
    TI_QI(Career.TI_XIU, CareerBranch.TI_BODY, 4.0D, "气血",
            "生命上限 +10%", EffectKind.HEALTH, 0.10D),
    TI_ZHEN(Career.TI_XIU, CareerBranch.TI_COUNTER, 2.0D, "震荡",
            "受击概率反震，反弹 10% 伤害", EffectKind.REFLECT, 0.10D),
    TI_JIN(Career.TI_XIU, CareerBranch.TI_STAND, 2.0D, "金刚",
            "护甲/韧性提升", EffectKind.ARMOR, 0.08D);

    /** 被动效果的种类（由运行期解释；数据层只配对与断言）。 */
    public enum EffectKind {
        /** 核心机制本身（解锁即拥有，数值无意义）。 */
        CORE_MECHANIC,
        /** 剑意获取（比例）。 */
        SWORD_INTENT,
        /** 剑势：受击凝聚剑意（开关型，数值仅表示"已解锁"）。 */
        SWORD_MOMENTUM,
        /** 近战护甲穿透（比例）。 */
        PENETRATION,
        /** 移动速度（比例）。 */
        MOVE_SPEED,
        /** 吸血（比例）。 */
        LIFESTEAL,
        /** 低血增伤（比例）。 */
        LOW_HP_DAMAGE,
        /** 移动留痕的减速/微伤（比例）。 */
        AURA_SLOW,
        /** 受击回补生命（比例）。 */
        HEAL_ON_HURT,
        /** 满仓爆发增伤（比例）。 */
        DEMON_OVERFLOW_BURST,
        /** 击杀回魔气（点）。 */
        DEMON_KILL_GAIN,
        /** 负面抗性（比例）。 */
        CONTROL_RESIST,
        /** 敌人削弱/恐惧（比例）。 */
        ENEMY_WEAKEN,
        /** 击杀带印目标回魂印（点）。 */
        SOUL_KILL_GAIN,
        /** 攻击附带减速（比例）。 */
        SLOW_ON_HIT,
        /** 魂爆范围伤害（比例）。 */
        SOUL_BURST_AOE,
        /** 抗击退（比例）。 */
        KNOCKBACK_RESIST,
        /** 生命上限（比例）。 */
        HEALTH,
        /** 减伤/护甲（比例）。 */
        ARMOR,
        /** 受伤反弹（比例）。 */
        REFLECT
    }

    private final Career career;
    private final CareerBranch branch;
    private final double unlockPoints;
    private final String cn;
    private final String desc;
    private final EffectKind kind;
    private final double value;

    CareerSkill(Career career, CareerBranch branch, double unlockPoints,
                String cn, String desc, EffectKind kind, double value) {
        this.career = career;
        this.branch = branch;
        this.unlockPoints = unlockPoints;
        this.cn = cn;
        this.desc = desc;
        this.kind = kind;
        this.value = value;
    }

    public Career career() {
        return this.career;
    }

    public CareerBranch branch() {
        return this.branch;
    }

    /** 解锁门槛：该分支累计投入多少点（0 = 职业自带核心）。 */
    public double unlockPoints() {
        return this.unlockPoints;
    }

    public String cn() {
        return this.cn;
    }

    public String desc() {
        return this.desc;
    }

    public EffectKind kind() {
        return this.kind;
    }

    public double value() {
        return this.value;
    }

    /** 是否职业自带的核心被动。 */
    public boolean isCore() {
        return this.unlockPoints <= 0.0D;
    }

    /** 某职业的 6 条被动（按声明顺序）。 */
    public static CareerSkill[] ofCareer(Career career) {
        java.util.List<CareerSkill> out = new java.util.ArrayList<>();
        for (CareerSkill s : values()) {
            if (s.career == career) {
                out.add(s);
            }
        }
        return out.toArray(new CareerSkill[0]);
    }

    /** 某分支上的被动（按解锁门槛升序）。 */
    public static CareerSkill[] ofBranch(CareerBranch branch) {
        java.util.List<CareerSkill> out = new java.util.ArrayList<>();
        for (CareerSkill s : values()) {
            if (s.branch == branch) {
                out.add(s);
            }
        }
        out.sort(java.util.Comparator.comparingDouble(CareerSkill::unlockPoints));
        return out.toArray(new CareerSkill[0]);
    }
}
