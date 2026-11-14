package com.hongmeng.xiuxian.entity.beast;

/**
 * 灵兽图鉴：**一只灵兽的全部静态数值，集中在这一处**。
 *
 * <p>规格里最重要的一条来自 Wyrmroost 的调研：<b>别做一堆只会飞的同质坐骑</b>。
 * 于是这个枚举的存在意义不只是"少写几个 if"——它把每只灵兽的
 * <b>职能 / 速度 / 载重 / 灵力消耗 / 契约门槛</b>并排放在一张表里，
 * 谁强谁弱、谁快谁慢一眼可见，改数值时必须同时面对这张表。</p>
 *
 * <pre>
 *   id             中文     职能          飞  速度  载重  灵力/tick  契约门槛  生命
 *   contract_eagle 契约雷鹰 空中侦察       ✓  1.15   0 格   2.5       1         24
 *   jiaolong       蛟龙     空战           ✓  1.35   6 格   3.0       3         90
 * </pre>
 *
 * <p><b>灵力消耗全走主人的那一条灵力池</b>（{@link com.hongmeng.xiuxian.core.ManaTable}），
 * 和御剑飞行共享预算。这样"骑灵兽"与"御剑"构成真实取舍：
 * 坐骑更快、能悬停、还能驮东西，但要先付出契约成本；御剑随时可用，但慢。</p>
 *
 * <p>这个类<b>绝不引用注册表</b>（没有 {@code Items.xxx}、没有 {@code ModEntities.xxx}）——
 * 离线验证器会直接断言各条目的属性关系，
 * 一旦这里出现注册表静态初始化，验证器就会以「Not bootstrapped」炸掉。
 * 需要物品/实体类型的场合一律走 {@code BeastManager} 里的反射桥。</p>
 */
public enum BeastKind {

    /**
     * 契约雷鹰：唯一能<b>悬停</b>的坐骑（悬停时消耗减半），
     * 代价是生命只有 24、载重为 0——规格里明确写了"脆、载重低（不能带箱子）"。
     */
    CONTRACT_EAGLE("contract_eagle", "契约雷鹰", BeastRole.AIR_SCOUT,
            true, 1.15D, 0, 2.5D, 1, 24.0D, 5.0D, 0.30D),

    /** 蛟龙：飞得快、血最厚、能喷吐、能驮货，但契约门槛最高（需要头目级妖兽）。 */
    JIAOLONG("jiaolong", "蛟龙", BeastRole.AIR_COMBAT,
            true, 1.35D, 6, 3.0D, 3, 90.0D, 9.0D, 0.32D),

    /**
     * 契约灵狼：<b>不骑乘</b>的战斗协助。规格里它是"主动攻击敌对生物，
     * 伤害随主人境界"的那一只——也是玩家能收服的第一只灵兽
     * （来源 {@code spirit_wolf} 是普通品阶，练气/筑基就够格）。
     *
     * <p>它的存在让灵兽系统不只有"坐骑"这一个用途：打不过的架可以放狗。</p>
     */
    CONTRACT_WOLF("contract_wolf", "契约灵狼", BeastRole.COMBAT_AIDE,
            false, 1.10D, 0, 0.0D, 0, 22.0D, 5.0D, 0.36D);

    private final String id;
    private final String cn;
    private final BeastRole role;
    private final boolean flying;
    /** 骑乘时的速度倍率（乘在实体自身的 MOVEMENT_SPEED 上）。 */
    private final double rideSpeed;
    /** 灵兽袋能替主人驮几格东西；0 = 纯粹的战斗/侦察单位。 */
    private final int cargoSlots;
    /** 飞行时每 tick 扣主人多少灵力；0 = 不耗（地面坐骑）。 */
    private final double manaDrain;
    /** 契约门槛：主人的大境序号至少要高过契约对象这么多档。 */
    private final int contractRealmGap;
    private final double baseHealth;
    private final double baseDamage;
    private final double baseSpeed;

    BeastKind(String id, String cn, BeastRole role, boolean flying, double rideSpeed,
              int cargoSlots, double manaDrain, int contractRealmGap,
              double baseHealth, double baseDamage, double baseSpeed) {
        this.id = id;
        this.cn = cn;
        this.role = role;
        this.flying = flying;
        this.rideSpeed = rideSpeed;
        this.cargoSlots = cargoSlots;
        this.manaDrain = manaDrain;
        this.contractRealmGap = contractRealmGap;
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
        this.baseSpeed = baseSpeed;
    }

    public String id() {
        return id;
    }

    public String cn() {
        return cn;
    }

    public BeastRole role() {
        return role;
    }

    public boolean flying() {
        return flying;
    }

    public double rideSpeed() {
        return rideSpeed;
    }

    public int cargoSlots() {
        return cargoSlots;
    }

    /** 飞行时每 tick 扣主人的灵力（0 = 不扣）。 */
    public double manaDrain() {
        return manaDrain;
    }

    /**
     * 契约门槛：主人的大境序号要<b>高出契约对象</b>这么多档。
     *
     * <p>数值为 0 表示"野外可以直接驯服，不需要契灵符"，
     * 这一项对它只用于图鉴展示。（原先唯一为 0 的地面坐骑灵驹已按玩家要求删除。）</p>
     */
    public int contractRealmGap() {
        return contractRealmGap;
    }

    public double baseHealth() {
        return baseHealth;
    }

    public double baseDamage() {
        return baseDamage;
    }

    public double baseSpeed() {
        return baseSpeed;
    }

    /** 能不能驮东西（决定灵兽袋里那一格"载重"显不显示）。 */
    public boolean canCarryCargo() {
        return cargoSlots > 0;
    }

    /** 悬停时的灵力消耗打折：雷鹰的招牌能力。 */
    public double hoverManaDrain() {
        return manaDrain * HOVER_DISCOUNT;
    }

    /** 悬停折扣（一半）。 */
    public static final double HOVER_DISCOUNT = 0.5D;

    /** 骑乘时的实际速度倍率。 */
    public double rideSpeedMultiplier() {
        return rideSpeed;
    }

    /** 按 id 找；找不到返回第一个条目（不返回 null，调用点不必判空）。 */
    public static BeastKind byId(String id) {
        if (id != null) {
            for (BeastKind k : values()) {
                if (k.id.equals(id)) {
                    return k;
                }
            }
        }
        // 找不到就退到第一个条目（不返回 null，调用点不必判空）
        return values()[0];
    }
}
