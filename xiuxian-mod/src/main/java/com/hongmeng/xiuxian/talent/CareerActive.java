package com.hongmeng.xiuxian.talent;

import com.hongmeng.xiuxian.technique.TechniqueKind;

/**
 * <b>职业主动（20 条 = 5 职业 × 4 条）</b>——加入原法术列表，但<b>不是普通功法</b>。
 *
 * <h2>规则（玩家方案 §1）</h2>
 * <ul>
 *   <li>作为新法术条目存在（{@code Technique} 枚举<b>末尾追加</b>，不重排旧 52 条）；</li>
 *   <li><b>必须在天赋树学习后才能装配到法术栏</b>：所在分支累计投入 5 点解锁；</li>
 *   <li>没学不会出现在可用列表；法术栏装配与服务端释放各校验一次；</li>
 *   <li><b>不生成秘籍、不进商店</b>（否则买书就能绕过天赋树）；图鉴标"职业功法"；</li>
 *   <li>形态复用现有 12 形态，不新增 {@code TechniqueKind}。</li>
 * </ul>
 *
 * <p>本类只是"职业主动 → 法术条目"的映射数据；真正的数值条目在 R1-D
 * 追加到 {@code Technique} 枚举末尾。{@code reusesExisting} 表示该主动
 * 复用已有功法（如万剑归宗/金刚护体），但装配门槛仍由本职业天赋树控制。</p>
 */
public enum CareerActive {

    // ================= 剑修 =================

    JIAN_QI_ZHAN(Career.JIAN_XIU, CareerBranch.JIAN_INTENT, "剑气斩",
            TechniqueKind.LINE, false, "JIAN_QI_ZHAN", 6,
            2, 30, 20, 1.6D,
            "前方直线剑气，命中叠剑意"),
    JIAN_WAN_JIAN(Career.JIAN_XIU, CareerBranch.JIAN_QI, "万剑归宗",
            TechniqueKind.FAN, true, "WAN_JIAN_GUI_ZONG", 5,
            9, 30, 24, 2.4D,
            "扇形多段剑气（复用现有功法形态）"),
    JIAN_YU_JIAN(Career.JIAN_XIU, CareerBranch.JIAN_YU, "御剑术",
            TechniqueKind.DASH, false, "JIAN_YU_SHU", 7,
            6, 10, 0, 0.9D,
            "短距位移/飞剑突进"),
    JIAN_YU(Career.JIAN_XIU, CareerBranch.JIAN_AURA, "剑域",
            TechniqueKind.AOE, false, "JIAN_YU", 6,
            5, 40, 40, 2.0D,
            "罡气化域：范围内持续剑气伤害"),

    // ================= 血修 =================

    XUE_DUN(Career.XUE_XIU, CareerBranch.XUE_STEP, "血遁",
            TechniqueKind.DASH, false, "XUE_DUN", 5,
            8, 20, 0, 1.2D,
            "消耗血池短距冲刺，路径留血痕"),
    XUE_BAO(Career.XUE_XIU, CareerBranch.XUE_POOL, "血爆",
            TechniqueKind.AOE, false, "XUE_BAO", 6,
            8, 50, 40, 2.6D,
            "以自身为中心血爆 AOE"),
    XUE_SHI(Career.XUE_XIU, CareerBranch.XUE_LEECH, "血噬",
            TechniqueKind.LIFESTEAL, false, "XUE_SHI", 7,
            6, 40, 60, 1.8D,
            "吸血斩击"),
    XUE_HE(Career.XUE_XIU, CareerBranch.XUE_STAGGER, "血河",
            TechniqueKind.AOE, false, "XUE_HE", 7,
            5, 60, 60, 2.2D,
            "持续血河区域，减速 + DoT"),

    // ================= 魔修 =================

    MO_BAO_FA(Career.MO_XIU, CareerBranch.MO_DEMON, "魔气爆发",
            TechniqueKind.AOE, false, "MO_QI_BAO_FA", 7,
            8, 60, 60, 3.0D,
            "消耗魔气范围爆发"),
    MO_SHI_HUN(Career.MO_XIU, CareerBranch.MO_PEN, "噬魂",
            TechniqueKind.LIFESTEAL, false, "MO_SHI_HUN", 7,
            6, 40, 60, 1.9D,
            "单体高伤 + 回气"),
    MO_YING_DUN(Career.MO_XIU, CareerBranch.MO_BURST, "魔影遁",
            TechniqueKind.DASH, false, "MO_YING_DUN", 6,
            8, 20, 0, 1.0D,
            "短距位移 + 留魔影"),
    MO_XIN_MO(Career.MO_XIU, CareerBranch.MO_HEART, "心魔引",
            TechniqueKind.GRAVITY, false, "MO_XIN_MO_YIN", 7,
            7, 60, 50, 2.0D,
            "控制/拉拽/恐惧"),

    // ================= 鬼修 =================

    GUI_SUO_HUN(Career.GUI_XIU, CareerBranch.GUI_MARK, "锁魂链",
            TechniqueKind.LINE, false, "GUI_SUO_HUN_LIAN", 6,
            6, 30, 40, 1.7D,
            "直线锁链，叠魂印 + 拉拽"),
    GUI_GUI_YU(Career.GUI_XIU, CareerBranch.GUI_REALM, "鬼域展开",
            TechniqueKind.AOE, false, "GUI_YU_ZHAN_KAI", 7,
            6, 50, 60, 2.2D,
            "主动展开大范围鬼域"),
    GUI_XU_HUA(Career.GUI_XIU, CareerBranch.GUI_PHASE, "虚化",
            TechniqueKind.DASH, false, "GUI_XU_HUA", 6,
            8, 30, 0, 0.0D,
            "短无敌 + 位移"),
    GUI_HUN_BAO(Career.GUI_XIU, CareerBranch.GUI_BURST, "魂爆",
            TechniqueKind.PROJECTILE, false, "GUI_HUN_BAO", 8,
            8, 50, 50, 2.8D,
            "引爆所有魂印"),

    // ================= 体修 =================

    TI_ZHEN_DANG(Career.TI_XIU, CareerBranch.TI_SURGE, "震荡波",
            TechniqueKind.AOE, false, "TI_ZHEN_DANG_BO", 6,
            8, 50, 40, 2.4D,
            "消耗激荡放范围击退 + 伤害"),
    TI_JIN_GANG(Career.TI_XIU, CareerBranch.TI_BODY, "金刚护体",
            TechniqueKind.BUFF, true, "JIN_GANG_HU_TI", 5,
            9, 10, 20, 0.0D,
            "短时减伤/霸体（复用现有功法形态）"),
    TI_BENG_SHAN(Career.TI_XIU, CareerBranch.TI_COUNTER, "崩山击",
            TechniqueKind.SLASH, false, "TI_BENG_SHAN_JI", 7,
            6, 40, 60, 2.8D,
            "重击破防"),
    TI_BU_MIE(Career.TI_XIU, CareerBranch.TI_STAND, "不灭金身",
            TechniqueKind.HEAL, false, "TI_BU_MIE_JIN_SHEN", 8,
            8, 30, 40, 0.0D,
            "短时锁血 + 回血");

    private final Career career;
    private final CareerBranch branch;
    private final String cn;
    private final TechniqueKind form;
    private final boolean reusesExisting;
    private final String techniqueName;
    private final int minTier;
    private final int manaCost;
    private final int cooldown;
    private final int castTicks;
    private final double damageMul;
    private final String desc;

    CareerActive(Career career, CareerBranch branch, String cn, TechniqueKind form,
                 boolean reusesExisting, String techniqueName, int minTier,
                 int manaCost, int cooldown, int castTicks, double damageMul,
                 String desc) {
        this.career = career;
        this.branch = branch;
        this.cn = cn;
        this.form = form;
        this.reusesExisting = reusesExisting;
        this.techniqueName = techniqueName;
        this.minTier = minTier;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.castTicks = castTicks;
        this.damageMul = damageMul;
        this.desc = desc;
    }

    public Career career() {
        return this.career;
    }

    public CareerBranch branch() {
        return this.branch;
    }

    public String cn() {
        return this.cn;
    }

    public TechniqueKind form() {
        return this.form;
    }

    /** 是否复用已有功法条目（不新增 Technique 常量）。 */
    public boolean reusesExisting() {
        return this.reusesExisting;
    }

    /** 目标法术条目名（{@code Technique} 枚举常量名）。 */
    public String techniqueName() {
        return this.techniqueName;
    }

    public int minTier() {
        return this.minTier;
    }

    public int manaCost() {
        return this.manaCost;
    }

    public int cooldown() {
        return this.cooldown;
    }

    public int castTicks() {
        return this.castTicks;
    }

    public double damageMul() {
        return this.damageMul;
    }

    public String desc() {
        return this.desc;
    }

    /** 装配门槛：所在分支累计投入 5 点。 */
    public double unlockPoints() {
        return 5.0D;
    }

    /** 某职业的 4 条主动。 */
    public static CareerActive[] ofCareer(Career career) {
        java.util.List<CareerActive> out = new java.util.ArrayList<>();
        for (CareerActive a : values()) {
            if (a.career == career) {
                out.add(a);
            }
        }
        return out.toArray(new CareerActive[0]);
    }

    /** 某分支上的主动（正常只有 1 条）。 */
    public static CareerActive ofBranch(CareerBranch branch) {
        for (CareerActive a : values()) {
            if (a.branch == branch) {
                return a;
            }
        }
        return null;
    }
}
