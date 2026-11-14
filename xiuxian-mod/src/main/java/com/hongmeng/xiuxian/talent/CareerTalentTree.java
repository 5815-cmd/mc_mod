package com.hongmeng.xiuxian.talent;

/**
 * <b>职业天赋树（5 职业 × 4 支 × 2 节点 = 40 节点）</b>。
 *
 * <h2>结构（玩家方案 §1 的落地）</h2>
 * <ul>
 *   <li>每职业 <b>4 支</b>、每支 <b>2 个节点</b>，共 8 节点；</li>
 *   <li>每节点 2~5 级，满境 21 点，<b>点不满</b>，必须取舍；</li>
 *   <li>同支第 2 节点需要该支累计投入 <b>3 点</b>；</li>
 *   <li>技能不占节点：分支投入 <b>2 / 4 点</b>解锁该支被动、
 *       <b>5 点</b>解锁该支主动（见 {@link CareerSkill} / {@link CareerActive}）。</li>
 * </ul>
 *
 * <p>节点 id 会被存档引用，<b>定稿后不要改名</b>（改名会让老存档点数指向空节点）。
 * 本类不引用 Minecraft 类型，可离线断言。</p>
 */
public final class CareerTalentTree {

    /** 同支第 2 节点所需的分支累计投入。 */
    public static final int SECOND_NODE_REQUIRES = 3;

    /** 每职业 4 支。 */
    public static final int BRANCHES_PER_CAREER = 4;

    /** 每支 2 节点。 */
    public static final int NODES_PER_BRANCH = 2;

    private CareerTalentTree() {
    }

    /** 节点效果种类（运行期解释；只用于数值节点，技能解锁走分支门槛）。 */
    public enum Kind {
        /** 近战伤害（比例）。 */
        MELEE,
        /** 剑意获取（比例）。 */
        SWORD_INTENT,
        /** 剑罡吸收率（比例）。 */
        SWORD_AURA,
        /** 剑气伤害（比例）。 */
        SWORD_QI,
        /** 减伤/护甲（比例）。 */
        ARMOR,
        /** 灵力上限（比例）。 */
        MANA,
        /** 攻击速度（比例；R-1 阶段主要由套装提供，树里预留）。 */
        ATTACK_SPEED,
        /** 抗击退（比例；R-1 阶段主要由套装/职业加成提供）。 */
        KNOCKBACK_RESIST,
        /** 移动速度（比例）。 */
        MOVE_SPEED,
        /** 冷却缩减（比例）。 */
        COOLDOWN,
        /** 血池上限（比例）。 */
        BLOOD_MAX,
        /** 血池获取（比例）。 */
        BLOOD_GAIN,
        /** 缓伤比例（比例）。 */
        STAGGER,
        /** 低血增伤（比例）。 */
        LOW_HP_DAMAGE,
        /** 吸血（比例）。 */
        LIFESTEAL,
        /** 魔气获取（比例）。 */
        DEMON_GAIN,
        /** 魔气上限（比例）。 */
        DEMON_CAP,
        /** 护甲穿透（比例）。 */
        PENETRATION,
        /** 法术伤害（比例）。 */
        SPELL,
        /** 负面抗性（比例）。 */
        CONTROL_RESIST,
        /** 魂印伤害（比例）。 */
        SOUL_MARK,
        /** 鬼域威力（比例）。 */
        GHOST_REALM,
        /** 领域范围（比例）。 */
        AREA,
        /** 虚化冷却缩减（比例）。 */
        PHASE_CD,
        /** 魂爆伤害（比例）。 */
        SOUL_BURST,
        /** 鬼域减速（比例）。 */
        GHOST_SLOW,
        /** 激荡获取（比例）。 */
        SURGE_GAIN,
        /** 激荡引爆（比例）。 */
        SURGE_BURST,
        /** 锁血时长（tick）。 */
        LAST_STAND_TICKS,
        /** 生命上限（比例）。 */
        HEALTH,
        /** 受伤反弹（比例）。 */
        REFLECT
    }

    /**
     * 一个天赋节点。
     *
     * @param id         存档 id（定稿后不改）
     * @param cn         中文名
     * @param desc       说明
     * @param career     所属职业
     * @param branch     所属分支
     * @param order      分支内序号（1 或 2）
     * @param maxRank    最高等级
     * @param perRank    每级效果量（含义由 {@link Kind} 决定）
     * @param kind       效果种类
     */
    public record Node(String id, String cn, String desc, Career career,
                       CareerBranch branch, int order, int maxRank,
                       double perRank, Kind kind) {
        /** 点这个节点所需的分支累计投入。 */
        public int branchRequired() {
            return this.order <= 1 ? 0 : SECOND_NODE_REQUIRES;
        }
    }

    /** 全部 40 个节点（按职业/分支/顺序排好）。 */
    private static final java.util.List<Node> NODES = build();

    /** 某职业的 8 个节点（按分支顺序）。 */
    public static java.util.List<Node> ofCareer(Career career) {
        java.util.List<Node> out = new java.util.ArrayList<>();
        for (Node n : NODES) {
            if (n.career() == career) {
                out.add(n);
            }
        }
        return out;
    }

    /** 某分支的 2 个节点（order 升序）。 */
    public static java.util.List<Node> ofBranch(CareerBranch branch) {
        java.util.List<Node> out = new java.util.ArrayList<>();
        for (Node n : NODES) {
            if (n.branch() == branch) {
                out.add(n);
            }
        }
        out.sort(java.util.Comparator.comparingInt(Node::order));
        return out;
    }

    /** 全部节点（只读）。 */
    public static java.util.List<Node> all() {
        return java.util.Collections.unmodifiableList(NODES);
    }

    private static java.util.List<Node> build() {
        java.util.List<Node> out = new java.util.ArrayList<>();

        // ================= 剑修 =================
        add(out, "jian_intent_1", "剑意·凝", "近战伤害 +6%/级",
                Career.JIAN_XIU, CareerBranch.JIAN_INTENT, 1, 5, 0.06D, Kind.MELEE);
        add(out, "jian_intent_2", "剑意·涌", "剑意获取 +10%/级",
                Career.JIAN_XIU, CareerBranch.JIAN_INTENT, 2, 3, 0.10D, Kind.SWORD_INTENT);
        add(out, "jian_aura_1", "罡气", "剑罡吸收率 +3%/级",
                Career.JIAN_XIU, CareerBranch.JIAN_AURA, 1, 4, 0.03D, Kind.SWORD_AURA);
        add(out, "jian_aura_2", "罡体", "减伤 +4%/级",
                Career.JIAN_XIU, CareerBranch.JIAN_AURA, 2, 3, 0.04D, Kind.ARMOR);
        add(out, "jian_qi_1", "剑气·锋", "剑气伤害 +8%/级",
                Career.JIAN_XIU, CareerBranch.JIAN_QI, 1, 5, 0.08D, Kind.SWORD_QI);
        add(out, "jian_qi_2", "剑气·涌", "剑气伤害 +10%/级（可叠）",
                Career.JIAN_XIU, CareerBranch.JIAN_QI, 2, 3, 0.10D, Kind.SWORD_QI);
        add(out, "jian_yu_1", "御风", "移动速度 +5%/级",
                Career.JIAN_XIU, CareerBranch.JIAN_YU, 1, 4, 0.05D, Kind.MOVE_SPEED);
        add(out, "jian_yu_2", "剑遁", "冷却缩减 +5%/级",
                Career.JIAN_XIU, CareerBranch.JIAN_YU, 2, 3, 0.05D, Kind.COOLDOWN);

        // ================= 血修 =================
        add(out, "xue_pool_1", "血池·深", "血池上限 +8%/级",
                Career.XUE_XIU, CareerBranch.XUE_POOL, 1, 4, 0.08D, Kind.BLOOD_MAX);
        add(out, "xue_pool_2", "血池·涌", "血池获取 +10%/级",
                Career.XUE_XIU, CareerBranch.XUE_POOL, 2, 3, 0.10D, Kind.BLOOD_GAIN);
        add(out, "xue_stagger_1", "缓伤·韧", "缓伤比例 +5%/级",
                Career.XUE_XIU, CareerBranch.XUE_STAGGER, 1, 4, 0.05D, Kind.STAGGER);
        add(out, "xue_stagger_2", "缓伤·怒", "低血增伤 +8%/级",
                Career.XUE_XIU, CareerBranch.XUE_STAGGER, 2, 3, 0.08D, Kind.LOW_HP_DAMAGE);
        add(out, "xue_leech_1", "嗜血", "吸血 +5%/级",
                Career.XUE_XIU, CareerBranch.XUE_LEECH, 1, 5, 0.05D, Kind.LIFESTEAL);
        add(out, "xue_leech_2", "血河", "吸血 +4%/级（可叠）",
                Career.XUE_XIU, CareerBranch.XUE_LEECH, 2, 3, 0.04D, Kind.LIFESTEAL);
        add(out, "xue_step_1", "血影", "移动速度 +5%/级",
                Career.XUE_XIU, CareerBranch.XUE_STEP, 1, 4, 0.05D, Kind.MOVE_SPEED);
        add(out, "xue_step_2", "血遁", "冷却缩减 +6%/级",
                Career.XUE_XIU, CareerBranch.XUE_STEP, 2, 3, 0.06D, Kind.COOLDOWN);

        // ================= 魔修 =================
        add(out, "mo_demon_1", "魔气·涌", "魔气获取 +8%/级",
                Career.MO_XIU, CareerBranch.MO_DEMON, 1, 5, 0.08D, Kind.DEMON_GAIN);
        add(out, "mo_demon_2", "魔气·海", "魔气上限 +10%/级",
                Career.MO_XIU, CareerBranch.MO_DEMON, 2, 3, 0.10D, Kind.DEMON_CAP);
        add(out, "mo_pen_1", "破防", "护甲穿透 +5%/级",
                Career.MO_XIU, CareerBranch.MO_PEN, 1, 5, 0.05D, Kind.PENETRATION);
        add(out, "mo_pen_2", "蚀甲", "护甲穿透 +4%/级（可叠）",
                Career.MO_XIU, CareerBranch.MO_PEN, 2, 3, 0.04D, Kind.PENETRATION);
        add(out, "mo_burst_1", "魔威", "法术伤害 +7%/级",
                Career.MO_XIU, CareerBranch.MO_BURST, 1, 5, 0.07D, Kind.SPELL);
        add(out, "mo_burst_2", "万魔", "法术伤害 +9%/级（深层）",
                Career.MO_XIU, CareerBranch.MO_BURST, 2, 3, 0.09D, Kind.SPELL);
        add(out, "mo_heart_1", "心魔", "负面抗性 +6%/级",
                Career.MO_XIU, CareerBranch.MO_HEART, 1, 4, 0.06D, Kind.CONTROL_RESIST);
        add(out, "mo_heart_2", "魔影", "冷却缩减 +5%/级",
                Career.MO_XIU, CareerBranch.MO_HEART, 2, 3, 0.05D, Kind.COOLDOWN);

        // ================= 鬼修 =================
        add(out, "gui_mark_1", "魂印·刻", "魂印伤害 +6%/级",
                Career.GUI_XIU, CareerBranch.GUI_MARK, 1, 5, 0.06D, Kind.SOUL_MARK);
        add(out, "gui_mark_2", "魂印·深", "魂印伤害 +8%/级（可叠）",
                Career.GUI_XIU, CareerBranch.GUI_MARK, 2, 3, 0.08D, Kind.SOUL_MARK);
        add(out, "gui_realm_1", "鬼域·扩", "鬼域威力 +8%/级",
                Career.GUI_XIU, CareerBranch.GUI_REALM, 1, 5, 0.08D, Kind.GHOST_REALM);
        add(out, "gui_realm_2", "鬼域·幽", "领域范围 +8%/级",
                Career.GUI_XIU, CareerBranch.GUI_REALM, 2, 3, 0.08D, Kind.AREA);
        add(out, "gui_phase_1", "阴气", "移动速度 +4%/级",
                Career.GUI_XIU, CareerBranch.GUI_PHASE, 1, 4, 0.04D, Kind.MOVE_SPEED);
        add(out, "gui_phase_2", "虚化", "虚化冷却缩减 +10%/级",
                Career.GUI_XIU, CareerBranch.GUI_PHASE, 2, 2, 0.10D, Kind.PHASE_CD);
        add(out, "gui_burst_1", "魂爆·烈", "魂爆伤害 +10%/级",
                Career.GUI_XIU, CareerBranch.GUI_BURST, 1, 5, 0.10D, Kind.SOUL_BURST);
        add(out, "gui_burst_2", "黄泉", "鬼域减速 +6%/级",
                Career.GUI_XIU, CareerBranch.GUI_BURST, 2, 3, 0.06D, Kind.GHOST_SLOW);

        // ================= 体修 =================
        add(out, "ti_surge_1", "激荡·蓄", "激荡获取 +8%/级",
                Career.TI_XIU, CareerBranch.TI_SURGE, 1, 5, 0.08D, Kind.SURGE_GAIN);
        add(out, "ti_surge_2", "激荡·燃", "激荡引爆 +8%/级",
                Career.TI_XIU, CareerBranch.TI_SURGE, 2, 3, 0.08D, Kind.SURGE_BURST);
        add(out, "ti_stand_1", "锁血·固", "锁血时长 +2 tick/级",
                Career.TI_XIU, CareerBranch.TI_STAND, 1, 3, 2.0D, Kind.LAST_STAND_TICKS);
        add(out, "ti_stand_2", "不灭", "生命上限 +7%/级",
                Career.TI_XIU, CareerBranch.TI_STAND, 2, 3, 0.07D, Kind.HEALTH);
        add(out, "ti_body_1", "铁骨", "减伤 +3%/级",
                Career.TI_XIU, CareerBranch.TI_BODY, 1, 5, 0.03D, Kind.ARMOR);
        add(out, "ti_body_2", "金刚", "生命上限 +8%/级",
                Career.TI_XIU, CareerBranch.TI_BODY, 2, 3, 0.08D, Kind.HEALTH);
        add(out, "ti_counter_1", "震荡", "受伤反弹 +5%/级",
                Career.TI_XIU, CareerBranch.TI_COUNTER, 1, 5, 0.05D, Kind.REFLECT);
        add(out, "ti_counter_2", "崩山", "护甲穿透 +4%/级",
                Career.TI_XIU, CareerBranch.TI_COUNTER, 2, 3, 0.04D, Kind.PENETRATION);

        return out;
    }

    private static void add(java.util.List<Node> out, String id, String cn, String desc,
                            Career career, CareerBranch branch, int order,
                            int maxRank, double perRank, Kind kind) {
        out.add(new Node(id, cn, desc, career, branch, order, maxRank, perRank, kind));
    }
}
