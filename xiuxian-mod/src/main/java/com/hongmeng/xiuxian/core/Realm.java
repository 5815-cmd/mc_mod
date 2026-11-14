package com.hongmeng.xiuxian.core;

/**
 * 修为境界。共 11 大境，每境"初期/中期/后期"三层，末境鸿蒙单独为 1 层。
 *
 * <p>境界由累计修为总量的阈值区间决定，而不是"每级所需"独立存储——
 * 这样境界永远是累计修为的纯函数，数值永远不会出现"卡在某一层"的诡异状态。</p>
 *
 * <p>境界同时决定"空手挖掘等级"（见 {@link #harvestTier()}）：
 * 修到金丹以后，一双肉掌就顶得上一把镐子。这就是原文里主角不靠工具
 * 也能刨开一切的那个设定。</p>
 */
public record Realm(String cn, int tier, int stage,
                    long minTotal, long need, double healthBonus, double damageBonus,
                    double scale, int harvestLevel) {

    public static final String[] STAGE_CN = {"初期", "中期", "后期"};

    private static final String[] REALM_CN = {
            "练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"
    };

    /**
     * 各大境的基需修为（该境第一阶段的量），以及每阶段的递增倍率。
     *
     * <p><b>这条曲线是重做过一次的。</b>最早那版每阶段 ×2、跨大境 ×3.5，
     * 结果练气三十层就要 1800、到筑基要 4200、到飞升要 618 万——
     * 玩家体感就是"卡在练气出不去"。</p>
     *
     * <p>现在改成：</p>
     * <ul>
     *   <li><b>每个小阶段 ×2.5</b>（练气初期 → 中期 → 后期 = 60 → 150 → 375）</li>
     *   <li><b>每个大境约 ×3.3</b>（练气 → 筑基 → 金丹 = 60 → 240 → 800）</li>
     *   <li>全程到鸿蒙约 <b>一千万</b>修为一以内</li>
     * </ul>
     *
     * <p>节奏参考（按"打坐 1/秒 + 杀怪 + 任务 + 吃丹"的正常玩法）：</p>
     * <pre>
     *   练气   几分钟          —— 开局
     *   筑基   十几分钟        —— 第一个真正的小目标
     *   金丹   一小时          —— 开始有金丹修士的样子
     *   元婴   三小时
     *   化神   六小时          —— 解锁常驻速度 I
     *   道印   十二小时        —— 解锁本命领域
     *   飞升   一天            —— 解锁飞行
     *   鸿蒙   几天            —— 终局
     * </pre>
     */
    private static final long[] REALM_BASE = {
            60L, 240L, 800L, 2_600L, 8_000L,
            25_000L, 80_000L, 240_000L, 800_000L, 2_600_000L
    };

    /** 每个小阶段相对上一阶段的倍率。 */
    private static final double STAGE_FACTOR = 2.5D;

    /**
     * 各大境对应的空手挖掘等级。
     *
     * <pre>
     *   练气 / 筑基  →  空手（-1，跟没修为一样，得老老实实拿工具）
     *   金丹         →  石制（1）
     *   元婴         →  石制（1）—— 原文这里是个平滑过渡，我按你的序列补的
     *   化神         →  铁制（2）
     *   道印         →  金制（3）
     *   飞升         →  钻石（4）
     *   仙人         →  下界合金（5）
     *   帝境         →  下界合金（5）
     *   鸿蒙         →  下界合金（5）+ 可破坏一切方块（含基岩）
     * </pre>
     *
     * <p><b>为什么用整数而不是 {@code Tier} 对象：</b>原版 {@code Tiers.GOLD} 的
     * 挖掘等级其实是 0（比铁还低），照搬会让"道印"比"化神"倒退。所以这里用一套
     * 自定义的递增等级，实际的 {@code Tier} 对象由
     * {@link RealmMining} 在运行时映射（那里才安全地接触游戏注册表）。</p>
     */
    private static final int[] HARVEST_BY_REALM = {
            -1,  // 1 练气
            -1,  // 2 筑基
            1,   // 3 金丹  石制
            1,   // 4 元婴  石制
            2,   // 5 化神  铁制
            3,   // 6 道印  金制（铁之上、钻石之下）
            4,   // 7 飞升  钻石
            5,   // 8 仙人  下界合金
            5,   // 9 帝境  下界合金
            5,   // 10 鸿蒙 下界合金
    };

    /** 可以破坏一切方块（基岩等）的境界序号：鸿蒙。 */
    public static final int TIER_ALL_BREAKING = 10;

    /**
     * 各大境的实力倍率（相对筑基 = ×1）。
     *
     * <pre>
     *   1 练气      ×0.1     —— 比凡人强一点，还没脱胎换骨
     *   2 筑基      ×1       —— +5 颗心（10 生命）、+3 攻击
     *   3 金丹      ×2       —— +10 颗心（这一境开始"翻倍"）
     *   4 元婴      ×4       —— 金丹的 2 倍
     *   5 化神      ×8       —— 元婴的 2 倍（解锁常驻速度 I）
     *   6 道印      ×16      —— 化神的 2 倍（解锁本命领域）
     *   7 飞升      ×48      —— ×3（解锁飞行）
     *   8 仙人      ×480     —— ×10
     *   9 帝境      ×4800    —— ×10（解锁可切换的常驻速度 II）
     *   10 鸿蒙     ×48000   —— ×10
     * </pre>
     *
     * <p>换算成心数：筑基 5 → 金丹 10 → 元婴 20 → 化神 40 → 道印 80 →
     * 飞升 240 → 仙人 2400 → 帝境 24000 → 鸿蒙 240000。</p>
     *
     * <p>这是一条极陡的指数曲线，原文本来就是"越往后越离谱"。
     * 觉得太夸张可以在 {@code hongmeng-server.toml} 的
     * {@code realm_scale} 段用系数整体缩放。</p>
     */
    private static final double[] MULTIPLIER_BY_REALM = {
            0.1D, 1.0D, 2.0D, 4.0D, 8.0D, 16.0D, 48.0D, 480.0D, 4800.0D, 48000.0D
    };

    /**
     * 筑基基准生命加成（点）。
     *
     * <p>玩家要求「提高一下玩家数值」：10 → 16（+60%）。
     * 逐境按 {@code MULTIPLIER_BY_REALM} 放大，所以鸿蒙境的生命
     * 从约 19 万涨到约 30 万。</p>
     */
    public static final double BASE_HEALTH_BONUS = 16.0D;
    /**
     * 筑基基准攻击加成（点）。
     *
     * <p>玩家要求提高数值：3 → 5（+67%）。鸿蒙境约 5700 → 9600。</p>
     */
    public static final double BASE_DAMAGE_BONUS = 5.0D;

    private static final int MAX_TIER = 10;
    private static final int MAX_STAGE = 3;

    /** 前 9 境各 3 层，第 10 境（鸿蒙）1 层，共 28 层。 */
    public static final int TOTAL_STEPS = 9 * MAX_STAGE + 1;

    // ---------------- 阈值表（静态初始化，之后只读） ----------------
    private static final long[] MIN_TOTAL = new long[TOTAL_STEPS];
    private static final long[] NEED = new long[TOTAL_STEPS];
    private static final String[] NAME = new String[TOTAL_STEPS];
    private static final int[] TIER_OF = new int[TOTAL_STEPS];
    private static final int[] STAGE_OF = new int[TOTAL_STEPS];
    private static final double[] HEALTH_BONUS = new double[TOTAL_STEPS];
    private static final double[] DAMAGE_BONUS = new double[TOTAL_STEPS];
    private static final double[] SCALE = new double[TOTAL_STEPS];
    private static final int[] HARVEST = new int[TOTAL_STEPS];

    static {
        long acc = 0L;
        int index = 0;
        for (int t = 1; t <= MAX_TIER; t++) {
            int stages = (t == MAX_TIER) ? 1 : MAX_STAGE;
            double mult = MULTIPLIER_BY_REALM[t - 1];
            double healthPer = BASE_HEALTH_BONUS * mult;
            double damagePer = BASE_DAMAGE_BONUS * mult;
            for (int s = 1; s <= stages; s++) {
                long need = (long) (REALM_BASE[t - 1] * Math.pow(STAGE_FACTOR, s - 1));
                MIN_TOTAL[index] = acc;
                NEED[index] = need;
                TIER_OF[index] = t;
                STAGE_OF[index] = s;
                NAME[index] = (t == MAX_TIER)
                        ? REALM_CN[t - 1]
                        : REALM_CN[t - 1] + STAGE_CN[s - 1];
                HEALTH_BONUS[index] = healthPer;
                DAMAGE_BONUS[index] = damagePer;
                SCALE[index] = mult;
                HARVEST[index] = HARVEST_BY_REALM[t - 1];
                acc += need;
                index++;
            }
        }
    }

    /** 累计修为总量能到达的最后一层（满级时返回 TOTAL_STEPS-1）。 */
    private static int indexOf(long total) {
        int idx = 0;
        for (int i = 0; i < TOTAL_STEPS; i++) {
            if (total >= MIN_TOTAL[i]) {
                idx = i;
            } else {
                break;
            }
        }
        return idx;
    }

    /**
     * 累计修为对应到第几层。
     *
     * <p>只用于"老存档迁移"——新档的境界由突破次数决定，不看修为。</p>
     */
    public static int indexOfTotal(long total) {
        return indexOf(total);
    }

    /** 按层号取境界（0 = 练气初期，{@link #TOTAL_STEPS}-1 = 鸿蒙）。 */
    public static Realm ofStep(int step) {
        int i = Math.max(0, Math.min(TOTAL_STEPS - 1, step));
        return new Realm(NAME[i], TIER_OF[i], STAGE_OF[i],
                MIN_TOTAL[i], NEED[i], HEALTH_BONUS[i], DAMAGE_BONUS[i],
                SCALE[i], HARVEST[i]);
    }

    public static Realm of(long total) {
        int i = indexOf(total);
        return new Realm(NAME[i], TIER_OF[i], STAGE_OF[i],
                MIN_TOTAL[i], NEED[i], HEALTH_BONUS[i], DAMAGE_BONUS[i],
                SCALE[i], HARVEST[i]);
    }

    // ------------------------------------------------------------------
    // 境界实力数值
    // ------------------------------------------------------------------

    /** 纯函数版：给定累计修为，返回相对筑基的实力倍率。 */
    public static double scaleOf(long total) {
        return SCALE[indexOf(total)];
    }

    /** 生命加成（半颗心 = 1 点生命；筑基 = +5 颗心 = 10 点）。 */
    public double healthBonus() {
        return healthBonus;
    }

    /** 加成的心数（用于显示），例如筑基 = 5 颗心。 */
    public double healthBonusHearts() {
        return healthBonus / 2.0D;
    }

    /** 攻击加成（点）。 */
    public double damageBonus() {
        return damageBonus;
    }

    /** 该境界是否解锁常驻速度 I（化神及以上）。 */
    public boolean hasSpeedOne() {
        return tier >= 5;
    }

    /**
     * 该境界是否解锁**免费的踏空飞行**（仙人及以上）。
     *
     * <p><b>这条按玩家要求改过：</b>原来飞升（第 7 境）就白送飞行，
     * 但现在飞升的定位是"<b>御剑 30 分钟</b>"——那是一种**消耗灵力**的飞行，
     * 不是白送的。真正"踏空而行"要到仙人（第 8 境）。</p>
     *
     * <pre>
     *   3 金丹 ~ 7 飞升   只能御剑（2.0 灵力/tick，时长见 ManaTable）
     *   8 仙人及以上      踏空飞行（免费）；也可以选择继续显形飞剑
     * </pre>
     */
    public boolean hasFlight() {
        // 玩家要求：「飞行的话飞升就可以飞行了」→ 第 7 境（飞升）起解锁飞行。
        // （原为 tier >= 8，即要等到仙人。）
        return tier >= 7;
    }

    // ------------------------------------------------------------------
    // 境界常驻药水效果的门槛（玩家指定）
    // ------------------------------------------------------------------
    //
    // 全部集中在这里，不要在事件处理里各写一遍数字：
    //   金丹(3)  生命回复 I
    //   元婴(4)  饱和 + 抗火 + 水下呼吸
    //   化神(5)  夜视
    //   飞升(7)  生命回复 II
    //   帝境(9)  生命回复 III

    /** 金丹起：生命回复 I。 */
    public boolean hasRegenOne() {
        return tier >= 3;
    }

    /** 元婴起：饱和 + 抗火 + 水下呼吸。 */
    public boolean hasElementalWard() {
        return tier >= 4;
    }

    /** 化神起：夜视。 */
    public boolean hasNightVision() {
        return tier >= 5;
    }

    /** 飞升起：生命回复 II。 */
    public boolean hasRegenTwo() {
        return tier >= 7;
    }

    /** 帝境起：生命回复 III。 */
    public boolean hasRegenThree() {
        return tier >= 9;
    }

    /** 当前境界该给的生命回复等级（0 = 不给）。 */
    public int regenerationAmplifier() {
        if (hasRegenThree()) {
            return 2;      // 生命回复 III
        }
        if (hasRegenTwo()) {
            return 1;      // 生命回复 II
        }
        if (hasRegenOne()) {
            return 0;      // 生命回复 I
        }
        return -1;
    }

    /**
     * 该境界是否享有**飞升的常驻效果**（生命回复 I + 夜视 + 饱和）。
     *
     * <p>刻意和 {@link #hasFlight()} 分开：飞行按玩家要求推迟到仙人，
     * 但"飞升"作为一个里程碑该有的待遇不该跟着一起推迟——
     * 否则飞升这一境会显得什么都没有。</p>
     */
    public boolean hasAscensionPerks() {
        return tier >= 7;
    }

    /** 该境界是否解锁可切换的常驻速度 II（帝境及以上）。 */
    public boolean hasSpeedTwo() {
        return tier >= 9;
    }

    /** 该境界是否凝聚了本命领域（道印及以上）。 */
    public boolean hasDomain() {
        return tier >= 6;
    }

    // ------------------------------------------------------------------
    // 挖掘等级（"一双肉掌顶一把镐子"）
    // ------------------------------------------------------------------

    /** 纯函数版：给定累计修为，返回空手挖掘等级；-1 表示空手。 */
    public static int harvestLevelOf(long total) {
        return HARVEST[indexOf(total)];
    }

    /** 该境界能否破坏一切方块（基岩、屏障等）。 */
    public boolean canBreakAnything() {
        return tier >= TIER_ALL_BREAKING;
    }

    /** 挖掘等级的可读名称，用于面板与提示。纯字符串，不碰游戏注册表。 */
    public String harvestTierName() {
        return switch (harvestLevel) {
            case 1 -> "石制";
            case 2 -> "铁制";
            case 3, 4 -> "金制";
            case 5 -> "效率V金镐";
            default -> "空手";
        };
    }

    // ------------------------------------------------------------------
    // 挖掘速度（纯数据，离线验证器可断言）
    // ------------------------------------------------------------------

    /** 金镐的挖掘速度（原版 Tiers.GOLD 就是 12.0）。 */
    public static final float GOLD_SPEED = 12.0F;

    /** 效率I 金镐：12 + 1² + 1。 */
    public static final float EFFICIENCY_I = 14.0F;
    /** 效率II 金镐：12 + 2² + 1。 */
    public static final float EFFICIENCY_II = 17.0F;
    /** 效率III 金镐：12 + 3² + 1。 */
    public static final float EFFICIENCY_III_GOLD_SPEED = 22.0F;
    /** 效率IV 金镐：12 + 4² + 1。 */
    public static final float EFFICIENCY_IV = 29.0F;
    /** 效率V 金镐：12 + 5² + 1。 */
    public static final float EFFICIENCY_V_GOLD_SPEED = 38.0F;
    /** 效率X 金镐：12 + 10² + 1。鸿蒙专属。 */
    public static final float EFFICIENCY_X = 113.0F;

    /**
     * 效率附魔的等效速度。
     *
     * <p>原版公式是 {@code 工具速度 + 等级² + 1}（只在挖对应工具方块时生效），
     * 金镐基础速度 12。这个方法把等级换算成速度，方便校对上面的常量。</p>
     */
    public static float efficiencySpeed(int level) {
        int l = Math.max(0, Math.min(20, level));
        return GOLD_SPEED + l * l + 1.0F;
    }

    /**
     * 大境序号 → 挖掘速度。
     *
     * <p><b>按玩家要求重排过一次</b>：鸿蒙 = 效率X 金镐、帝境 = 效率V，往前依次递减。</p>
     *
     * <pre>
     *    1 练气    2.0   木镐
     *    2 筑基    4.0   石镐
     *    3 金丹    6.0   铁镐
     *    4 元婴   12.0   金镐
     *    5 化神   14.0   效率I   金镐
     *    6 道印   17.0   效率II  金镐
     *    7 飞升   22.0   效率III 金镐
     *    8 仙人   29.0   效率IV  金镐
     *    9 帝境   38.0   效率V   金镐
     *   10 鸿蒙  113.0   效率X   金镐
     * </pre>
     *
     * <p><b>为什么按大境而不是按挖掘等级：</b>挖掘等级被多个境界共用
     * （仙人/帝境/鸿蒙都是下界合金档），但速度要求各不相同。</p>
     */
    public static float speedForTier(int tier) {
        return switch (Math.max(1, Math.min(10, tier))) {
            case 1 -> 2.0F;                       // 木镐
            case 2 -> 4.0F;                       // 石镐
            case 3 -> 6.0F;                       // 铁镐
            case 4 -> GOLD_SPEED;                 // 金镐
            case 5 -> EFFICIENCY_I;               // 效率I
            case 6 -> EFFICIENCY_II;              // 效率II
            case 7 -> EFFICIENCY_III_GOLD_SPEED;  // 效率III
            case 8 -> EFFICIENCY_IV;              // 效率IV
            case 9 -> EFFICIENCY_V_GOLD_SPEED;    // 效率V
            default -> EFFICIENCY_X;              // 效率X
        };
    }

    /**
     * 这一境挖掘时附带的**时运等级**。
     *
     * <p>按玩家要求：**飞升（第 7 境）起附带时运 III**，鸿蒙额外更高。</p>
     *
     * @return 0 表示没有时运加成
     */
    public static int fortuneForTier(int tier) {
        if (tier >= 10) {
            return 5;    // 鸿蒙：时运 V
        }
        if (tier >= 7) {
            return 3;    // 飞升 / 仙人 / 帝境：时运 III
        }
        return 0;
    }

    /** 这一境的挖掘时运等级。 */
    public int miningFortune() {
        return fortuneForTier(tier);
    }

    /**
     * 这一境是否自带**急迫 I**。
     *
     * <p>按玩家要求：**仙人（第 8 境）起自带急迫 1**。</p>
     */
    public boolean hasHaste() {
        return tier >= 8;
    }

    /** 这一层的挖掘速度。 */
    public float harvestSpeed() {
        return speedForTier(tier);
    }

    /**
     * 挖掘速度的可读文案。
     *
     * <p>按玩家要求：鸿蒙 = 效率X、帝境 = 效率V，往前依次递减。</p>
     */
    public String harvestSpeedLabel() {
        return switch (Math.max(1, Math.min(10, tier))) {
            case 1 -> "木镐速度";
            case 2 -> "石镐速度";
            case 3 -> "铁镐速度";
            case 4 -> "金镐速度";
            case 5 -> "效率I金镐速度";
            case 6 -> "效率II金镐速度";
            case 7 -> "效率III金镐速度";
            case 8 -> "效率IV金镐速度";
            case 9 -> "效率V金镐速度";
            default -> "效率X金镐速度";
        };
    }

    /** 挖掘附加效果的文案（时运 / 急迫）。 */
    public String miningBonusLabel() {
        StringBuilder sb = new StringBuilder();
        int f = miningFortune();
        if (f > 0) {
            sb.append("时运").append(romanOf(f));
        }
        if (hasHaste()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("急迫I");
        }
        return sb.length() == 0 ? "无" : sb.toString();
    }

    private static String romanOf(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    // ------------------------------------------------------------------
    // 境界护甲值（纯数据）
    // ------------------------------------------------------------------

    /**
     * 各大境带来的**护甲值**加成。
     *
     * <p>减伤走 {@link com.hongmeng.xiuxian.compat.ArmorMitigation} 的**总减伤口径**：
     * {@code 1 - 20/(20 + 护甲 × 1.5)}，上限 92%。所以这里可以给得大方一些——
     * 105 点护甲的总减伤约 88.7%，配上法宝也不会无敌。</p>
     *
     * <pre>
     *   练气 0    筑基 3    金丹 6    元婴 10   化神 15
     *   道印 24   飞升 36   仙人 52   帝境 72   鸿蒙 105
     * </pre>
     */
    public static final double[] ARMOR_BY_TIER = {
            0.0D,    // 1 练气
            3.0D,    // 2 筑基
            6.0D,    // 3 金丹
            10.0D,   // 4 元婴
            15.0D,   // 5 化神
            24.0D,   // 6 道印
            36.0D,   // 7 飞升
            52.0D,   // 8 仙人
            72.0D,   // 9 帝境
            105.0D,  // 10 鸿蒙
    };

    /** 大境序号 → 境界护甲值。 */
    public static double armorForTier(int tier) {
        int i = Math.max(1, Math.min(10, tier)) - 1;
        return ARMOR_BY_TIER[i];
    }

    /** 这一层的境界护甲值。 */
    public double armorBonus() {
        return armorForTier(tier);
    }

    /** 护甲的可读档位名，给面板用。纯字符串。 */
    public String armorRankName() {
        double a = armorBonus();
        if (a <= 0.0D) {
            return "无";
        }
        if (a < 5.0D) {
            return "皮甲";
        }
        if (a < 10.0D) {
            return "锁甲";
        }
        if (a < 16.0D) {
            return "铁甲";
        }
        if (a < 24.0D) {
            return "精铁";
        }
        if (a < 40.0D) {
            return "灵甲";
        }
        if (a < 60.0D) {
            return "仙甲";
        }
        return "鸿蒙道甲";
    }

    /**
     * 面板/提示用的完整文案（含挖掘速度与"可破坏一切"的说明）。
     * 刻意放在 Realm 里而不是 RealmMining 里：这样离线验证器也能安全调用，
     * 不会因为碰到 {@code Blocks} 而要求 Minecraft 注册表引导。
     */
    public String harvestTierLabel() {
        if (canBreakAnything()) {
            return harvestTierName() + " · 可破坏一切";
        }
        return harvestTierName();
    }

    /** 面板用的完整描述：等级 + 速度。 */
    public String harvestFullLabel() {
        if (harvestLevel <= 0) {
            return "空手（得用对工具）";
        }
        String s = harvestTierName() + " · " + harvestSpeedLabel();
        if (canBreakAnything()) {
            s += " · 可破坏一切";
        }
        return s;
    }

    public static long progress(long total) {
        int i = indexOf(total);
        return total - MIN_TOTAL[i];
    }

    public static long needed(long total) {
        int i = indexOf(total);
        return NEED[i];
    }

    public static boolean isMax(long total) {
        return total >= MIN_TOTAL[TOTAL_STEPS - 1] + NEED[TOTAL_STEPS - 1];
    }

    /** 距离满修为还差多少。 */
    public static long remaining(long total) {
        long cap = MIN_TOTAL[TOTAL_STEPS - 1] + NEED[TOTAL_STEPS - 1];
        return Math.max(0L, cap - total);
    }

    public static double completion(long total) {
        long cap = MIN_TOTAL[TOTAL_STEPS - 1] + NEED[TOTAL_STEPS - 1];
        return Math.min(1.0D, (double) total / (double) cap);
    }

    // ---------------- 实例方法 ----------------

    public boolean isMaxRealm() {
        return tier >= MAX_TIER;
    }

    /** 下一境的显示名（已经满级则为空）。 */
    public String nextCn() {
        int i = indexOf(minTotal);
        if (i + 1 >= TOTAL_STEPS) {
            return "";
        }
        return NAME[i + 1];
    }

    /** 完整称号，例如"练气三层"→"练气后期"。 */
    public String fullCn() {
        return cn;
    }

    /** 大境名，例如"练气"。 */
    public String tierCn() {
        return REALM_CN[Math.min(MAX_TIER, tier) - 1];
    }

    public String describe() {
        if (isMaxRealm()) {
            return cn + "（已至绝巅）";
        }
        return cn + " → " + nextCn();
    }
}
