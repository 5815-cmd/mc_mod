package com.hongmeng.xiuxian.core;

/**
 * 境界标准值：整套装备与怪物数值的**唯一锚点**。
 *
 * <p>这套数字来自对成熟修仙模组的实测反推（凡人修仙人界篇的秘境伤害、炼丹阁、
 * 炼器阁三处都重复使用同一组"本境标准伤害值"）：</p>
 *
 * <pre>
 *   境界     标准值    递增比
 *   练气        8      —
 *   筑基       40      ×5.00     ← 实测
 *   金丹      136      ×3.40     ← 实测
 *   元婴      424      ×3.12     ← 实测
 *   化神     1288      ×3.04     ← 实测
 *   道印     3900      ×3.03     ← 外推
 *   飞升    11800      ×3.03     ← 外推
 *   仙人    35500      ×3.01     ← 外推
 *   帝境   107000      ×3.01     ← 外推
 *   鸿蒙   320000      ×2.99     ← 外推
 * </pre>
 *
 * <p>由它派生出的四条规律（同样来自实测）：</p>
 * <ul>
 *   <li><b>装备攻击</b> ≈ 标准值 × 1.0 ~ 1.4</li>
 *   <li><b>全套防具总防御</b> ≈ 标准值 × 0.65 ~ 0.75</li>
 *   <li><b>武器增幅</b>：每境 +5%（练气 0% → 化神 20% → 鸿蒙 45%）</li>
 *   <li><b>怪物</b>：普通妖兽 HP = 标准值 × 5、攻击 = ×1.0；BOSS HP = × 7、攻击 = × 0.8</li>
 * </ul>
 */
public final class RealmStats {

    /** 十境的境界标准伤害值（下标 0 = 练气）。 */
    private static final long[] STANDARD = {
            8L, 40L, 136L, 424L, 1288L,
            3900L, 11_800L, 35_500L, 107_000L, 320_000L
    };

    /**
     * 普通妖兽倍率：生命。
     *
     * <p><b>这个值调过两次。</b>最早期是 5.0（"标准值 × 5"），和玩家血量不是同一刻度，
     * 终局会出现"鸿蒙妖兽 160 万血、玩家 48 万血"；后来压到 0.45。</p>
     *
     * <p><b>2026-11-06 玩家实机反馈「小怪数值可以下调一下」→ 0.45 → 0.32（−29%）。</b>
     * 用途只有一个：{@code CultivationMonsterEntity.applyStandardStrengthFloor} 的
     * <b>本境生命下限</b>（基础值 + 品阶 + 境界缩放的乘积低于这条线时才补）。
     * 下调后中高境普通怪/精英怪的血量同步变薄，清场更快；对 Boss 无影响
     * （阶梯 Boss 走 {@code BossLadder} 固定层目标值，远高于下限）。</p>
     *
     * <p>校验：{@code tools/check_balance.py} 的「小怪」一节。</p>
     */
    public static final double MOB_HEALTH_RATIO = 0.32D;

    /**
     * 普通妖兽倍率：攻击（<b>已废弃</b>，不再参与计算）。
     *
     * <p>旧口径 = 本境标准值 × 0.06，但标准值每境 ×3、玩家生命每境 ×2.5~10，
     * 两者不是同一刻度：金丹小怪只掉玩家 1.4% 血，道印/飞升却掉 6~7%，
     * 仙人的品阶精英（境界 +1）更是能打掉 20%。见 {@link #MOB_ATTACK_FLOOR}。</p>
     *
     * @deprecated 下限已改为按"同境玩家护甲后 2% 生命/击"锚定的显式表 {@link #MOB_ATTACK_FLOOR}。
     */
    @Deprecated
    public static final double MOB_ATTACK_RATIO = 0.06D;

    /**
     * 普通妖兽的<b>本境攻击下限</b>（下标 0 = 练气，单位：护甲前单次伤害）。
     *
     * <h2>2026-11-06 为什么从公式改成表</h2>
     * <p>玩家反馈「小怪数值可以下调一下」。根因是旧公式 {@code 标准值 × 0.06} 和
     * 玩家生命曲线错位（标准值每境 ×3，玩家生命每境 ×2.5~10），于是：</p>
     * <pre>
     *   金丹小怪一击 ≈ 1.4% 玩家生命   ← 太弱
     *   道印/飞升    ≈ 6~7%            ← 太痛（玩家体感来源）
     *   鸿蒙         ≈ 0.2%            ← 太弱
     * </pre>
     *
     * <p>现在按同一套生存口径反推：<b>同境普通怪护甲后每击 ≈ 2% 玩家生命</b>。</p>
     * <pre>
     *   floor(t) = 玩家生命(t) × 2% ÷ (1 - 护甲减伤T(t)) ÷ 灵根期望(1.621)
     * </pre>
     * <ul>
     *   <li>玩家生命 / 护甲减伤取 {@code 工程/13-职业甲数值平衡-结论.md} 的口径
     *       （基础 20 + 境界生命 + 职业甲固定生命，护甲 = 职业甲 + 境界护甲）；</li>
     *   <li>灵根期望 1.621 = 混沌 ×1.54 · 60% + 鸿蒙 ×1.90 · 30% + 先天 ×1.27 · 10%
     *       （{@code MonsterSpiritRoot}），先除掉它，让灵根随机继续在"下限"上体现强弱；</li>
     *   <li>后期（帝境/鸿蒙）旧值本来就远低于 2%，**按玩家要求只降不升**，保留旧值封顶；</li>
     *   <li>练气旧值 0.48 也保留（1 境玩家没有职业甲，0.48 已经不痛）。</li>
     * </ul>
     *
     * <p>精英怪的攻击下限 = 本表[锚定境界] × 品阶伤害倍率（精英 ×1.5），
     * 由 {@code CultivationMonsterEntity.applyStandardStrengthFloor} 组合。</p>
     */
    private static final double[] MOB_ATTACK_FLOOR = {
            0.48D,      // 1 练气（保留旧值：不升）
            2.12D,      // 2 筑基
            7.05D,      // 3 金丹
            16.66D,     // 4 元婴
            26.53D,     // 5 化神
            46.27D,     // 6 道印
            126.5D,     // 7 飞升
            1192.5D,    // 8 仙人
            6420.0D,    // 9 帝境（封顶：旧值更低，不再上调）
            19200.0D    // 10 鸿蒙（封顶：旧值更低，不再上调）
    };
    /**
     * BOSS 倍率：生命（约 8 次满伤技能打死，再高就成木桩了）。
     *
     * <p>同样按玩家血量锚定过。见 {@link #MOB_HEALTH_RATIO} 的说明——
     * 首领会额外吃品阶倍率（首领 ×80），所以基础值必须先压下来，
     * 否则终局会算出 1.79 亿血这种数字。</p>
     */
    public static final double BOSS_HEALTH_RATIO = 1.20D;

    /**
     * BOSS 倍率：攻击。
     *
     * <p><b>这个值一度是 0.8，结果是"首领一击打掉玩家 672% 血量"。</b>
     * 因为攻击会被乘三次：品阶（首领 ×4.5）× 境界缩放（终局 ×6.5）
     * × 开局难度（鸿蒙 ×3）。所以基础值必须压得很低。</p>
     *
     * <p>取 0.04 时：鸿蒙首领基础一击 12800，三次乘完约 112 万——
     * 相对玩家 48 万血是"两下打死"，仍然危险但不是无解。</p>
     */
    public static final double BOSS_ATTACK_RATIO = 0.04D;

    /** 精英怪倍率：生命。 */
    public static final double ELITE_HEALTH_RATIO = 0.75D;
    /** 精英怪倍率：攻击。 */
    public static final double ELITE_ATTACK_RATIO = 1.5D;

    /** 装备攻击倍率区间。 */
    public static final double WEAPON_RATIO_MIN = 1.0D;
    public static final double WEAPON_RATIO_MAX = 1.4D;
    /** 全套防具总防御倍率区间。 */
    public static final double ARMOR_RATIO_MIN = 0.65D;
    public static final double ARMOR_RATIO_MAX = 0.75D;

    private RealmStats() {
    }

    /** 某大境（1~10）的标准伤害值。 */
    public static long standard(int tier) {
        int idx = Math.max(1, Math.min(10, tier)) - 1;
        return STANDARD[idx];
    }

    public static long standardOf(long cultivation) {
        return standard(Realm.of(cultivation).tier());
    }

    /** 该境界的武器"增幅"（每境 +5%，练气 0%）。 */
    public static double weaponAmplify(int tier) {
        return Math.max(0, Math.min(10, tier) - 1) * 0.05D;
    }

    /** 武器增幅的百分比文本。 */
    public static String weaponAmplifyText(int tier) {
        return "+" + (int) Math.round(weaponAmplify(tier) * 100.0D) + "%";
    }

    /** 该境界武器的推荐附加攻击（取下限，即标准值 × 1.0）。 */
    public static double weaponAttack(int tier) {
        return standard(tier) * WEAPON_RATIO_MIN;
    }

    /** 该境界武器的攻击上限（标准值 × 1.4，大师/唯一装备的档位）。 */
    public static double weaponAttackMax(int tier) {
        return standard(tier) * WEAPON_RATIO_MAX;
    }

    /** 该境界全套防具的总防御（取中值 标准值 × 0.70）。 */
    public static double armorTotal(int tier) {
        return standard(tier) * 0.70D;
    }

    /** 单件防具的护甲值（四件套均分；原版护甲上限是 30，所以这里会超，需要额外处理）。 */
    public static double armorPerPiece(int tier) {
        return armorTotal(tier) / 4.0D;
    }

    /** 该境界普通妖兽的生命。 */
    public static double mobHealth(int tier) {
        return standard(tier) * MOB_HEALTH_RATIO;
    }

    /**
     * 该境界普通妖兽的<b>攻击下限</b>（护甲前单次伤害，见 {@link #MOB_ATTACK_FLOOR}）。
     *
     * <p>注意：这是"普通怪（品阶 ×1.0）"的下限；精英/队长等由实体层再乘
     * 品阶伤害倍率，见 {@code CultivationMonsterEntity.applyStandardStrengthFloor}。</p>
     */
    public static double mobAttack(int tier) {
        int idx = Math.max(1, Math.min(10, tier)) - 1;
        return MOB_ATTACK_FLOOR[idx];
    }

    /** 该境界精英怪的生命。 */
    public static double eliteHealth(int tier) {
        return standard(tier) * ELITE_HEALTH_RATIO;
    }

    /** 该境界精英怪的攻击。 */
    public static double eliteAttack(int tier) {
        return standard(tier) * ELITE_ATTACK_RATIO;
    }

    /** 该境界 BOSS 的生命（给本境玩家打）。 */
    public static double bossHealth(int tier) {
        return standard(tier) * BOSS_HEALTH_RATIO;
    }

    /** 该境界 BOSS 的攻击。 */
    public static double bossAttack(int tier) {
        return standard(tier) * BOSS_ATTACK_RATIO;
    }
}
