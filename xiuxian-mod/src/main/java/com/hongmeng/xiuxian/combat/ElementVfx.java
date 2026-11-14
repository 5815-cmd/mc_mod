package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.spirit.Element;

/**
 * 八元素特效表（{@code docs/combat-feel-spec.md} §四）。
 *
 * <h2>为什么是"粒子编号"而不是 {@code ParticleTypes}</h2>
 *
 * <p>{@code ParticleTypes} 的静态字段是<b>注册表对象</b>：离线验证器一加载就
 * {@code Not bootstrapped}。所以表里存的是<b>编号</b>（{@link #P_FLAME} 这种
 * {@code int} 常量），编号 → {@code ParticleOptions} 的映射放在
 * {@link VfxParticles} 里（那个类只在游戏里被加载，离线永远不碰）。</p>
 *
 * <p>这样"八种元素的前摇/命中/余韵分别是什么"就是一张可以离线断言的纯数据表，
 * 而"粒子编号 3 到底是哪个原版粒子"是一个运行时映射。</p>
 *
 * <h2>八元素的口径（规格 §四 的表格，逐行落地）</h2>
 *
 * <pre>
 *   元素   前摇（起）      命中（转）              余韵（合）
 *   火     火星汇聚        爆燃 + 焦痕             余烬飘落
 *   冰     霜花凝结        冰晶碎裂                地面结霜
 *   雷     电弧跳跃        白闪 + 强震             电弧余辉
 *   风     气流盘旋        切割线                  落叶盘旋
 *   金     细刃虚影        金属火花                刃光残留
 *   木     藤蔓破土        木刺迸射                花瓣散落
 *   水     水珠汇聚        水幕炸开                涟漪扩散
 *   土     碎石悬浮        尘爆 + 地裂             沙尘沉降
 * </pre>
 *
 * <p>{@code weight} 是"这一系的粒子该比别的系多/少多少"：雷火土是重系（爆发大），
 * 金木是细系（数量少但更锐）。它乘在基准数量上，再乘配置里的密度倍率，
 * 最后夹在 {@link CombatFeel#BURST_HARD_CAP} 之内——所以"华丽"永远撞不到帧率墙上。</p>
 */
public final class ElementVfx {

    private ElementVfx() {
    }

    // ---- 粒子编号（与 VfxParticles 的 switch 一一对应）----
    public static final int P_ENCHANT = 0;
    public static final int P_FLAME = 1;
    public static final int P_SMALL_FLAME = 2;
    public static final int P_LAVA = 3;
    public static final int P_EXPLOSION = 4;
    public static final int P_SNOWFLAKE = 5;
    public static final int P_CRIT = 6;
    public static final int P_ENCHANTED_HIT = 7;
    public static final int P_ELECTRIC_SPARK = 8;
    public static final int P_FLASH = 9;
    public static final int P_CLOUD = 10;
    public static final int P_SWEEP = 11;
    public static final int P_END_ROD = 12;
    public static final int P_FIREWORK = 13;
    public static final int P_HAPPY_VILLAGER = 14;
    public static final int P_SPORE = 15;
    public static final int P_CHERRY = 16;
    public static final int P_SPLASH = 17;
    public static final int P_BUBBLE = 18;
    public static final int P_FALLING_WATER = 19;
    public static final int P_LARGE_SMOKE = 20;
    public static final int P_CAMPFIRE_SMOKE = 21;
    public static final int P_WHITE_ASH = 22;
    public static final int P_SMOKE = 23;
    public static final int P_SOUL_FLAME = 24;
    public static final int P_TOTEM = 25;
    public static final int P_DRAGON_BREATH = 26;
    public static final int P_SCULK_SOUL = 27;
    public static final int P_WITCH = 28;
    public static final int P_POOF = 29;
    public static final int P_SONIC = 30;
    public static final int P_GLOW = 31;
    public static final int P_ASH = 32;
    public static final int P_DRIPPING_WATER = 33;

    /**
     * 一个元素的特效配方。
     *
     * @param cn               表现名（写进日志/命令/报告用）
     * @param weight           粒子数量权重（0.8~1.3）
     * @param windupParticle   前摇粒子编号
     * @param windupBase       前摇基准数量
     * @param trailParticle    拖尾粒子编号
     * @param trailBase        拖尾每一步的基准数量
     * @param burstParticle    命中主粒子编号
     * @param burstAccent      命中副粒子编号（叠在主粒子上的"点睛"）
     * @param burstBase        命中基准数量
     * @param afterglowParticle 余韵粒子编号
     * @param afterglowBase    余韵每一缕的基准数量
     */
    public record Flavor(String cn, float weight,
                         int windupParticle, int windupBase,
                         int trailParticle, int trailBase,
                         int burstParticle, int burstAccent, int burstBase,
                         int afterglowParticle, int afterglowBase) {
    }

    // ---- 火：火星汇聚 → 爆燃 + 焦痕 → 余烬飘落 ----
    private static final Flavor FIRE = new Flavor("爆燃", 1.20F,
            P_SMALL_FLAME, 10, P_FLAME, 2,
            P_FLAME, P_LAVA, 22,
            P_FLAME, 4);
    // ---- 冰：霜花凝结 → 冰晶碎裂 → 地面结霜 ----
    private static final Flavor ICE = new Flavor("冰晶碎裂", 1.00F,
            P_SNOWFLAKE, 12, P_SNOWFLAKE, 2,
            P_SNOWFLAKE, P_CRIT, 18,
            P_SNOWFLAKE, 3);
    // ---- 雷：电弧跳跃 → 白闪 + 强震 → 电弧余辉 ----
    private static final Flavor THUNDER = new Flavor("白闪强震", 1.30F,
            P_ELECTRIC_SPARK, 14, P_ELECTRIC_SPARK, 3,
            P_FLASH, P_ELECTRIC_SPARK, 26,
            P_ELECTRIC_SPARK, 4);
    // ---- 风：气流盘旋 → 切割线 → 落叶盘旋 ----
    private static final Flavor WIND = new Flavor("切割线", 1.25F,
            P_CLOUD, 10, P_CLOUD, 2,
            P_SWEEP, P_CHERRY, 20,
            P_CHERRY, 3);
    // ---- 金：细刃虚影 → 金属火花 → 刃光残留 ----
    private static final Flavor METAL = new Flavor("金属火花", 0.95F,
            P_END_ROD, 10, P_END_ROD, 2,
            P_ENCHANTED_HIT, P_END_ROD, 16,
            P_END_ROD, 3);
    // ---- 木：藤蔓破土 → 木刺迸射 → 花瓣散落 ----
    private static final Flavor WOOD = new Flavor("木刺迸射", 0.90F,
            P_HAPPY_VILLAGER, 10, P_HAPPY_VILLAGER, 2,
            P_CRIT, P_CHERRY, 16,
            P_CHERRY, 3);
    // ---- 水：水珠汇聚 → 水幕炸开 → 涟漪扩散 ----
    private static final Flavor WATER = new Flavor("水幕炸开", 1.05F,
            P_DRIPPING_WATER, 12, P_SPLASH, 2,
            P_SPLASH, P_BUBBLE, 20,
            P_SPLASH, 4);
    // ---- 土：碎石悬浮 → 尘爆 + 地裂 → 沙尘沉降 ----
    private static final Flavor EARTH = new Flavor("尘爆地裂", 1.15F,
            P_CRIT, 10, P_LARGE_SMOKE, 2,
            P_LARGE_SMOKE, P_CRIT, 22,
            P_CAMPFIRE_SMOKE, 3);
    // ---- 无属性（血魔噬、神识震这类）：没有元素语汇，用最中性的那套 ----
    private static final Flavor NONE = new Flavor("灵力震荡", 1.00F,
            P_ENCHANT, 10, P_ENCHANT, 2,
            P_ENCHANTED_HIT, P_WITCH, 16,
            P_ENCHANT, 3);

    /** 元素 → 特效配方（不返回 null）。 */
    public static Flavor of(Element element) {
        if (element == null) {
            return NONE;
        }
        return switch (element) {
            case FIRE -> FIRE;
            case ICE -> ICE;
            case THUNDER -> THUNDER;
            case WIND -> WIND;
            case METAL -> METAL;
            case WOOD -> WOOD;
            case WATER -> WATER;
            case EARTH -> EARTH;
            case NONE -> NONE;
        };
    }

    /** 前摇粒子数（已乘权重与密度倍率）。 */
    public static int windupCount(Element element, double density) {
        Flavor f = of(element);
        return CombatFeel.clampCount(f.windupBase(), density * f.weight(), 48);
    }

    /** 拖尾每一步的粒子数。 */
    public static int trailCount(Element element, double density) {
        Flavor f = of(element);
        return CombatFeel.clampCount(f.trailBase(), density * f.weight(), 8);
    }

    /** 命中放射爆发的粒子数（已乘权重与密度倍率，夹在硬上限内）。 */
    public static int burstCount(Element element, double damageFraction, double density) {
        Flavor f = of(element);
        int base = CombatFeel.burstParticles(damageFraction, density);
        return CombatFeel.clampCount(base, f.weight(), CombatFeel.BURST_HARD_CAP);
    }

    /** 余韵每一缕的粒子数。 */
    public static int afterglowCount(Element element, double density) {
        Flavor f = of(element);
        return CombatFeel.clampCount(f.afterglowBase(), density * f.weight(), 10);
    }

    /** 全部 8 个真实元素（不含 {@code NONE}），供离线断言"一个都没漏"。 */
    public static Element[] eightElements() {
        return new Element[]{
                Element.METAL, Element.WOOD, Element.WATER, Element.FIRE,
                Element.EARTH, Element.ICE, Element.THUNDER, Element.WIND
        };
    }
}
