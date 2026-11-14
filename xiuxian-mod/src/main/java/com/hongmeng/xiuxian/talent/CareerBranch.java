package com.hongmeng.xiuxian.talent;

/**
 * <b>五职业 × 4 支天赋分支</b>（共 20 支）。
 *
 * <p>分支是"点数投入"的容器：玩家把点数投进某一支，
 * 达到 <b>2 / 4 点</b>解锁该支被动、达到 <b>5 点</b>解锁该支主动。
 * 技能本身不占天赋节点（否则 6 被动 + 4 主动会撑爆 8 节点上限）。</p>
 *
 * <p>分支 id 用于存档/界面/断言；<b>定稿后不要改名</b>（存的是 id）。</p>
 */
public enum CareerBranch {

    // ---------------- 剑修 ----------------
    JIAN_INTENT(Career.JIAN_XIU, "剑意支", "jian_intent"),
    JIAN_AURA(Career.JIAN_XIU, "剑罡支", "jian_aura"),
    JIAN_QI(Career.JIAN_XIU, "剑气支", "jian_qi"),
    JIAN_YU(Career.JIAN_XIU, "御剑支", "jian_yu"),

    // ---------------- 血修 ----------------
    XUE_POOL(Career.XUE_XIU, "血池支", "xue_pool"),
    XUE_STAGGER(Career.XUE_XIU, "缓伤支", "xue_stagger"),
    XUE_LEECH(Career.XUE_XIU, "吸血支", "xue_leech"),
    XUE_STEP(Career.XUE_XIU, "血遁支", "xue_step"),

    // ---------------- 魔修 ----------------
    MO_DEMON(Career.MO_XIU, "魔气支", "mo_demon"),
    MO_PEN(Career.MO_XIU, "破防支", "mo_pen"),
    MO_BURST(Career.MO_XIU, "爆发支", "mo_burst"),
    MO_HEART(Career.MO_XIU, "心魔支", "mo_heart"),

    // ---------------- 鬼修 ----------------
    GUI_MARK(Career.GUI_XIU, "魂印支", "gui_mark"),
    GUI_REALM(Career.GUI_XIU, "鬼域支", "gui_realm"),
    GUI_PHASE(Career.GUI_XIU, "虚化支", "gui_phase"),
    GUI_BURST(Career.GUI_XIU, "魂爆支", "gui_burst"),

    // ---------------- 体修 ----------------
    TI_SURGE(Career.TI_XIU, "激荡支", "ti_surge"),
    TI_STAND(Career.TI_XIU, "锁血支", "ti_stand"),
    TI_BODY(Career.TI_XIU, "肉身支", "ti_body"),
    TI_COUNTER(Career.TI_XIU, "反击支", "ti_counter");

    private final Career career;
    private final String cn;
    private final String id;

    CareerBranch(Career career, String cn, String id) {
        this.career = career;
        this.cn = cn;
        this.id = id;
    }

    public Career career() {
        return this.career;
    }

    public String cn() {
        return this.cn;
    }

    public String id() {
        return this.id;
    }

    /** 该职业的 4 支（按声明顺序）。 */
    public static CareerBranch[] ofCareer(Career career) {
        java.util.List<CareerBranch> out = new java.util.ArrayList<>();
        for (CareerBranch b : values()) {
            if (b.career == career) {
                out.add(b);
            }
        }
        return out.toArray(new CareerBranch[0]);
    }

    /** 按 id 找（存档/界面用）。 */
    public static CareerBranch byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (CareerBranch b : values()) {
            if (b.id.equals(id) || b.name().equalsIgnoreCase(id)) {
                return b;
            }
        }
        return null;
    }
}
