package com.hongmeng.xiuxian.entity;

/**
 * 怪物品阶。
 *
 * <p>修仙界里的妖兽也分三六九等：同样一只狼，普通种与首领种完全不是一个量级。
 * 品阶决定四件事——血量、攻击、速度、经验。</p>
 *
 * <p>本类刻意<b>不引用任何 Minecraft 类</b>：纯数据表，方便离线校验倍率是否被改动，
 * 也方便在任何地方（配置、命令、HUD）安全地使用。</p>
 *
 * <h3>外观表（第二张表，与数值表并列）</h3>
 *
 * <p>玩家原话："不同品质的怪物外观也不一样，比如精英和普通怪物的外观差别"——
 * 所以"品阶只差一个名字前缀"是不合格的。这里把外观也做成<b>纯数据</b>：
 * 体型倍率、光晕颜色、发光等级、护甲等级全部写在枚举常量里，
 * <b>渲染层只读这张表，不许自己写死数字</b>。</p>
 *
 * <p>于是"精英和普通长得不一样"这件事可以在<b>离线</b>断言：
 * 见 {@code tools/check_rank_appearance.py}（用 javac 单独编译本文件 + 探针，
 * 因为本类没有任何 Minecraft 依赖，这是唯一能真正离线跑起来的部分）。</p>
 *
 * <h4>为什么首领的血条颜色不是名牌的金色</h4>
 *
 * <p>名牌前缀沿用 {@link #colorCode()}（首领 = 原版 {@code §6} 金色，不改动既有表现），
 * 但<b>血条 / BOSS 条 / 光晕</b>走 {@link #auraColor()}：玩家明确要求
 * "头目=紫、首领=红"，所以首领的显示色是红。两套颜色都出自本表，
 * 别在渲染代码里再抄一份。</p>
 */
public enum MobRank {

    /** 普通：随手可杀的杂鱼。外观就是模型本来的样子，不加任何东西。 */
    NORMAL("普通", '7', 1.0D, 1.0D, 1.0D, 1.0D,
            new Look(1.00F, 1.00F, 0x9AA0A6, 0, 0, 0, BossTint.NONE), 0),

    /** 精英：需要点真本事。眼睛发光（附加发光贴图）——一眼就能从杂鱼里认出来。 */
    ELITE("精英", 'a', 3.0D, 1.5D, 1.05D, 2.0D,
            new Look(1.05F, 1.03F, 0x4CD94C, 1, 1, 0, BossTint.NONE), 1),

    /** 队长：小头目，多半带一队手下。体型明显放大 + 肩甲 + 足下光环。 */
    CAPTAIN("队长", 'b', 8.0D, 2.0D, 1.10D, 4.0D,
            new Look(1.12F, 1.07F, 0x33D6C8, 1, 2, 1, BossTint.NONE), 2),

    /** 头目：区域霸主，掉落可观。更壮 + 体表微光 + 腰带甲 + 光晕，并拥有专属 BOSS 条（紫）。 */
    BOSS("头目", '5', 20.0D, 2.4D, 1.15D, 8.0D,
            new Look(1.20F, 1.12F, 0xA855F7, 2, 3, 2, BossTint.PURPLE), 3),

    /** 首领：一界之尊，见了先跑。最大 + 双重甲 + 最强光晕，BOSS 条是红的。 */
    OVERLORD("首领", '6', 64.0D, 3.2D, 1.20D, 16.0D,
            new Look(1.32F, 1.20F, 0xFF3B30, 3, 4, 3, BossTint.RED), 4);

    private final String cn;
    private final char color;
    private final double health;
    private final double damage;
    private final double speed;
    private final double xp;
    private final Look look;
    /** 境界等阶加成（见 {@link #realmTierBonus()}）。 */
    private final int realmBonus;

    MobRank(String cn, char color, double health, double damage, double speed, double xp,
            Look look, int realmBonus) {
        this.cn = cn;
        this.color = color;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.xp = xp;
        this.look = look;
        this.realmBonus = realmBonus;
    }

    /** 中文品阶名。 */
    public String cn() {
        return this.cn;
    }

    /** 原版颜色代码字符，例如 {@code '7'} 表示白色。 */
    public char colorCode() {
        return this.color;
    }

    /** 带颜色的品阶名，例如 {@code "\u00a7a精英"}。 */
    public String colored() {
        return "\u00a7" + this.color + this.cn;
    }

    /** 生命倍率。 */
    public double healthMultiplier() {
        return this.health;
    }

    /** 攻击倍率。 */
    public double damageMultiplier() {
        return this.damage;
    }

    /** 移速倍率（幅度最小，否则高阶怪会瞬移一样）。 */
    public double speedMultiplier() {
        return this.speed;
    }

    /** 击杀经验倍率。 */
    public double xpMultiplier() {
        return this.xp;
    }

    /** 队长及以上算"稀有"：值得单独提示玩家。 */
    public boolean isRare() {
        return this.ordinal() >= CAPTAIN.ordinal();
    }

    // ------------------------------------------------------------------
    // 外观（纯数据，渲染层只读）
    // ------------------------------------------------------------------

    /** 完整外观参数（给渲染层与离线断言用）。 */
    public Look look() {
        return this.look;
    }

    /**
     * 横向体型倍率。
     *
     * <p>刻意压在 1.32 以内：实体命中箱是按<b>类型</b>注册的（{@code ModEntities} 里
     * 每种怪已经按品阶手调过尺寸），渲染再放大会让模型超出命中箱。
     * 1.32 是"看得出来更大"与"打起来不别扭"的折中。</p>
     */
    public float scaleXZ() {
        return this.look.scaleXz();
    }

    /** 纵向体型倍率（比横向略小，否则高阶怪会变成细高竹竿）。 */
    public float scaleY() {
        return this.look.scaleY();
    }

    /** 光晕 / 血条 / BOSS 条的显示色（0xRRGGBB）。 */
    public int auraColor() {
        return this.look.auraColor();
    }

    /** 发光等级：0 无、1 眼睛、2 眼睛 + 体表微光、3 眼睛 + 体表强光。 */
    public int glowLevel() {
        return this.look.glow();
    }

    /** 有没有发光眼睛（精英及以上）。 */
    public boolean glowingEyes() {
        return this.look.glow() > 0;
    }

    /** 光晕等级：粒子密度 + 足下光环 + 体表光晕，0 表示什么都不加。 */
    public int auraLevel() {
        return this.look.aura();
    }

    /** 护甲等级：0 无、1 肩甲、2 肩甲 + 腰带、3 双重甲。 */
    public int armorLevel() {
        return this.look.armor();
    }

    /** 头顶血条要不要写数字（精英及以上）。 */
    public boolean showsHealthNumbers() {
        return ordinal() >= ELITE.ordinal();
    }

    /** 要不要一条横贯屏幕上方的 BOSS 血条（头目与首领）。 */
    public boolean hasBossBar() {
        return this.look.bossTint() != BossTint.NONE;
    }

    /** BOSS 条的颜色（纯数据；由实体层映射到原版 {@code BossBarColor}）。 */
    public BossTint bossTint() {
        return this.look.bossTint();
    }

    // ------------------------------------------------------------------
    // 境界（规格 §2.3：怪物也要有境界 + 境界压制减伤）
    // ------------------------------------------------------------------

    /**
     * 品阶自带的"境界等阶加成"。
     *
     * <p>怪物境界 = <b>进世界时锚定玩家的境界 + 本品阶的加成</b>（夹在 1~10 境）。
     * 加成取 0/1/2/3/4，不是拍脑袋：品阶倍率是 1 / 3 / 8 / 20 / 64
     * （≈ 3⁰~3⁴ 量级；头目/首领 2026-11-07 从 25/80 下调到 20/64），
     * 而 {@code RealmStats} 的标准值恰好每境 ×3 —— 两条曲线本来就是同一把尺子，
     * 所以"高一个品阶 ≈ 高一个境界"。</p>
     *
     * <p>于是：<b>普通怪正好对标本境"标准强度修为"</b>（{@code RealmStats.standard(tier)}），
     * 精英及以上<b>高于</b>标准 —— 规格 §2.3 要求的"强力怪物最低也要对标标准强度修为"
     * 由 {@code CultivationMonsterEntity} 在进世界时按本表补一个数值下限来保证。</p>
     */
    public int realmTierBonus() {
        return this.realmBonus;
    }

    /** 大境总数（练气~鸿蒙，10 境）。 */
    public static final int REALM_TIER_COUNT = 10;

    /** 十境的中文名（下标 0 = 练气）。刻意与 {@code core.Realm} 同序，但不依赖它。 */
    public static final String[] REALM_CN = {
            "练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"
    };

    /** 大境中文名（1~10；越界夹住）。 */
    public static String realmCn(int tier) {
        int idx = Math.max(1, Math.min(REALM_TIER_COUNT, tier)) - 1;
        return REALM_CN[idx];
    }

    /**
     * 怪物自身的境界（1~10，对应练气~鸿蒙）。
     *
     * @param anchorTier 锚定境界（通常是进世界时最近玩家的境界；没有玩家时传 1）
     * @param rank       品阶
     * @return 夹在 1~10 的怪物境界
     */
    /**
     * <b>任何怪的境界最多比玩家高几境</b>（硬上限，防止"被秒杀"）。
     *
     * <p>玩家反馈：「虚空裂隙门槛虽然是仙人，但里面怪境界都是鸿蒙，玩家根本打不过」
     * 「就算最简单的妖兽，里面怪物境界也是鸿蒙，如果我道印去了岂不是被秒杀」。</p>
     *
     * <p>根因是境界倍率表（{@code MobScaling.realmFactor}）是<b>指数</b>的
     * （练气 ×1 … 鸿蒙 ×520），而怪境界原先等于
     * {@code clamp(玩家境界 + 品阶加成, 1, 10)} —— 加成一越界就被夹到鸿蒙，
     * 怪直接吃满 ×520。实测最坏情况：<b>道印玩家遇到首领，怪是玩家的 40 倍</b>，
     * 而且<b>玩家越弱差距越大</b>，完全违背"强度对标玩家阶段"。</p>
     *
     * <p>⚠ <b>第一版修错了</b>：我一开始把上限设成 4 —— 但品阶加成本来就是
     * 0/1/2/3/4，全部 ≤4，<b>等于没改</b>（数字一个都没变）。</p>
     *
     * <p>真正的病根是<b>双重计算</b>：品阶已经给了 ×20（头目）/ ×64（首领）
     * 的血攻，<b>又</b>把怪的境界抬高 3~4 境 —— 而境界倍率是指数的（一境 ≈ ×3），
     * 于是 3~4 境就是 ×27 ~ ×81 的额外乘算，两层叠起来到 40 倍。</p>
     *
     * <p>所以上限收到 {@value} 境：品阶梯度还在（普通 +0 / 精英 +1 / 队长 +1 /
     * 头目 +2 / 首领 +2），但不再指数级叠加。难度交给副本
     * （{@code Dungeon.mobScaleFor}）与波次的固定倍率去提供。</p>
     *
     * <p>⚠ <b>第二版也修错了</b>：上限收到 2 境之后，队长/头目/首领
     * <b>全都拿到 +2</b>（{@code min(2, 2/3/4)} 都是 2），三者的"境界差"
     * 变得<b>完全一样</b> —— 品阶梯度被压平了，而且仍然是双重计算
     * （×8/×25/×80 的品阶倍率 <b>再</b>乘 ×6）。</p>
     *
     * <p>根子在于：<b>境界差与品阶倍率本来就是同一把尺子</b>
     * （品阶倍率 1/3/8/20/64 ≈ 3⁰~3⁴，而境界标准值每境 ×3）。
     * 两个都乘 = 把同一件事算了两遍。</p>
     */
    public static final int MAX_TIER_GAP = 1;

    /**
     * 怪物自身的境界（1~10，对应练气~鸿蒙）。
     *
     * <p><b>相对玩家锚定 + 差距封顶</b>：</p>
     * <pre>
     *   怪的境界 = clamp(玩家境界 + min(品阶加成, MAX_TIER_GAP), 1, 10)
     * </pre>
     *
     * <p>⚠ 原来是 {@code 玩家境界 + 品阶加成} 直接夹到 1~10，
     * 于是低境界玩家进高门槛副本时，高品阶怪会被夹成鸿蒙（见 {@link #MAX_TIER_GAP}）。</p>
     *
     * @param anchorTier 锚定境界（通常是进世界时最近玩家的境界；没有玩家时传 1）
     * @param rank       品阶
     * @return 夹在 1~10 的怪物境界
     */
    public static int monsterRealmTier(int anchorTier, MobRank rank) {
        int base = Math.max(1, Math.min(10, anchorTier));
        // ⚠ 境界差**只给精英怪**。
        //
        // 原因：品阶倍率（1/3/8/20/64）与境界倍率（每境 ≈×3）是**同一把尺子**
        // —— 头目 ×20 ≈ 高 2.7 境、首领 ×64 ≈ 高 3.8 境。两个都乘就是把同一件事算两遍，
        // 实测能到 40 倍（道印玩家遇首领），玩家原话就是"根本打不过"。
        //
        // 现在：精英 +1 境（做出"比杂鱼高一线"的观感，以及境界压制的减伤手感），
        // 队长/头目/首领 **不加境界差** —— 它们的强度完全由品阶自带的
        // ×8 / ×20 / ×64 血攻体现，梯度照样清楚。
        int bonus = (rank == MobRank.ELITE) ? MAX_TIER_GAP : 0;
        return Math.max(1, Math.min(10, base + bonus));
    }

    /**
     * 境界压制的减伤表：<b>下标 = 怪物境界 - 玩家境界</b>（0 表示同级或玩家更高）。
     *
     * <pre>
     *   境界差   怪物受到的伤害
     *     0        ×1.00   （玩家不低于怪物 → 不打折）
     *     1        ×0.80   （减伤 20%）
     *     2        ×0.65   （减伤 35%）
     *     3        ×0.50   （减伤 50%）
     *     4        ×0.40   （减伤 60%）
     *     ≥5       ×0.30   （减伤 70%，下限：再高也不会免伤）
     * </pre>
     *
     * <p>刻意留了下限 30%：不然"越境挑战"会变成"打不动"，
     * 而规格只要求"有减伤效果"，没说可以免伤。</p>
     */
    public static final double[] SUPPRESSION_BY_GAP = {
            1.00D, 0.80D, 0.65D, 0.50D, 0.40D, 0.30D
    };

    /**
     * 玩家境界低于怪物境界时的伤害系数（纯数据，可离线断言）。
     *
     * @param playerTier  玩家大境（1~10）
     * @param monsterTier 怪物大境（1~10）
     * @return 伤害倍率，1.0 表示不减伤
     */
    public static double suppressionMultiplier(int playerTier, int monsterTier) {
        int gap = monsterTier - playerTier;
        if (gap <= 0) {
            return 1.0D;
        }
        int idx = Math.min(gap, SUPPRESSION_BY_GAP.length - 1);
        return SUPPRESSION_BY_GAP[idx];
    }

    /** 玩家是否被这只怪压制（用于面板/HUD 提示）。 */
    public static boolean suppressedBy(int playerTier, int monsterTier) {
        return monsterTier > playerTier;
    }

    /** 减伤百分比的文本，例如 {@code 减伤 35%}。 */
    public static String suppressionText(int playerTier, int monsterTier) {
        double mul = suppressionMultiplier(playerTier, monsterTier);
        if (mul >= 1.0D) {
            return "无压制";
        }
        return "减伤 " + Math.round((1.0D - mul) * 100.0D) + "%";
    }

    @Override
    public String toString() {
        return this.cn;
    }

    /**
     * 一档品阶的全部外观参数。
     *
     * @param scaleXz   横向体型倍率（1.0 = 模型原样）
     * @param scaleY    纵向体型倍率
     * @param auraColor 显示色 0xRRGGBB（血条填充、光晕、护甲片、BOSS 条）
     * @param glow      发光等级 0..3
     * @param aura      光晕等级 0..4
     * @param armor     护甲等级 0..3
     * @param bossTint  BOSS 条颜色
     */
    public record Look(float scaleXz, float scaleY, int auraColor,
                       int glow, int aura, int armor, BossTint bossTint) {
    }

    /** BOSS 条颜色（刻意不直接用原版枚举，保持本类零 Minecraft 依赖）。 */
    public enum BossTint {
        /** 不要 BOSS 条。 */
        NONE,
        /** 紫色（头目）。 */
        PURPLE,
        /** 红色（首领）。 */
        RED
    }
}
