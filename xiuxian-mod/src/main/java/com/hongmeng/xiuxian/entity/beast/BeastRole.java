package com.hongmeng.xiuxian.entity.beast;

/**
 * 灵兽的<b>职能</b>。
 *
 * <p>这一条是整个系统的设计总纲，来自对 Wyrmroost 的调研结论
 * （见 {@code docs/spirit-beast-spec.md} 第一节）：
 * <b>「不只是长得不同，玩法也不同」</b>。如果每只灵兽都只是"能飞的坐骑"，
 * 玩家永远只会用数值最强的那一只，其余全部变成摆设。</p>
 *
 * <p>所以每一只灵兽在<b>数值、骑乘方式、灵力消耗、载重</b>四条轴上各占一个生态位：</p>
 *
 * <pre>
 *   雷鹰   AIR_SCOUT    能飞 + 能悬停 + 耗灵力最省             —— 但只有 24 血、不能驮货
 *   蛟龙   AIR_COMBAT   飞得快 + 血厚 + 会喷吐                 —— 但契约门槛最高、耗灵力最凶
 * </pre>
 *
 * <p>这个类<b>刻意不碰任何注册表</b>：它是纯枚举，离线验证器可以直接断言
 * "某只灵兽的契约门槛必须比另一只低"这类平衡关系。</p>
 */
public enum BeastRole {

    /** 空中侦察：契约雷鹰。能飞、能悬停，但脆且载重为零。 */
    AIR_SCOUT("空中侦察"),

    /** 空战：蛟龙。飞得快、能喷吐、血最厚，代价是门槛与灵力。 */
    AIR_COMBAT("空战"),

    /** 战斗协助：契约灵狼。不骑乘，主动替主人打敌对生物。 */
    COMBAT_AIDE("战斗协助");

    private final String cn;

    BeastRole(String cn) {
        this.cn = cn;
    }

    public String cn() {
        return cn;
    }

    /** 这一职能能不能飞（决定要不要走飞行分支与扣灵力）。 */
    public boolean aerial() {
        return this == AIR_SCOUT || this == AIR_COMBAT;
    }

    /** 这一职能能不能被骑。 */
    public boolean rideable() {
        return this != COMBAT_AIDE;
    }
}
