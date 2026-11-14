package com.hongmeng.xiuxian.spirit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 灵根池。
 *
 * <p>灵根由两个维度组成：</p>
 * <ul>
 *   <li><b>品质</b>（{@link Rarity}）：七档，决定生命与攻击加成；</li>
 *   <li><s>元素</s>：<b>已移除</b> —— 灵根不再决定属性加成（玩家要求）。</li>
 * </ul>
 *
 * <p>抽取流程：先按权重抽品质，再从该品质的池子里取一条。
 * <b>属性维度已删</b>：抽到哪条只影响名字与品质，不再影响元素加成；
 * 元素加成全部由<b>领域（道宫）</b>提供。</p>
 *
 * <p>主角同款——{@code 仙剑灵根}（先天灵根·冰·异化·隐藏状态）躺在先天池里，
 * 抽中概率约 5% × 1/3，对应原文里"开局赌一把"的手感。</p>
 */
public final class SpiritRoot {

    private static final Map<String, SpiritRoot> REGISTRY = new LinkedHashMap<>();
    private static final Random RANDOM = new Random();

    // ---------------- 伪灵根（最弱） ----------------
    public static final SpiritRoot FALSE_MIXED = add(new SpiritRoot(
            "false_mixed", "伪灵根", null, Rarity.FALSE_ROOT,
            "杂而不纯，几乎感应不到灵气。聊胜于无，修就是了。"));

    // ---------------- 凡品灵根 ----------------
    public static final SpiritRoot MORTAL_EARTH = add(new SpiritRoot(
            "mortal_earth", "凡品土灵根", null, Rarity.MORTAL,
            "根基沉稳，起步慢但不易出岔子。"));
    public static final SpiritRoot MORTAL_WOOD = add(new SpiritRoot(
            "mortal_wood", "凡品木灵根", null, Rarity.MORTAL,
            "生机绵长，恢复力不错。"));

    // ---------------- 地灵根 ----------------
    public static final SpiritRoot EARTH_METAL = add(new SpiritRoot(
            "earth_metal", "地品金灵根", null, Rarity.EARTH,
            "杀伐之气稍显，适合练兵器。"));
    public static final SpiritRoot EARTH_WATER = add(new SpiritRoot(
            "earth_water", "地品水灵根", null, Rarity.EARTH,
            "灵力绵密，法术消耗低。"));
    public static final SpiritRoot EARTH_FIRE = add(new SpiritRoot(
            "earth_fire", "地品火灵根", null, Rarity.EARTH,
            "火性暴烈，炼丹事半功倍。"));
    public static final SpiritRoot EARTH_WIND = add(new SpiritRoot(
            "earth_wind", "地品风灵根", null, Rarity.EARTH,
            "身法轻灵，风系功法与你相合。"));
    public static final SpiritRoot EARTH_THUNDER = add(new SpiritRoot(
            "earth_thunder", "地品雷灵根", null, Rarity.EARTH,
            "雷法天成，破邪第一。"));

    // ---------------- 天灵根 ----------------
    public static final SpiritRoot HEAVEN_FIRE = add(new SpiritRoot(
            "heaven_fire", "天品火灵根", null, Rarity.HEAVEN,
            "烈焰之资，富哥最爱的炫技款之一。"));
    public static final SpiritRoot HEAVEN_ICE = add(new SpiritRoot(
            "heaven_ice", "天品冰灵根", null, Rarity.HEAVEN,
            "冰封三尺，非一日之寒。"));
    public static final SpiritRoot HEAVEN_THUNDER = add(new SpiritRoot(
            "heaven_thunder", "天品雷灵根", null, Rarity.HEAVEN,
            "雷法之资，一剑出鞘雷声大作。"));

    // ---------------- 先天灵根 ----------------
    /** 主角同款：仙剑灵根（隐藏状态），冰属性异化。 */
    public static final SpiritRoot INNATE_ICE_SWORD = add(new SpiritRoot(
            "innate_ice_sword", "仙剑灵根", null, Rarity.INNATE,
            "隐藏状态。冰（异化）——剑术伤害转化为治疗与百分比伤害。一剑出鞘，剑气纵横。"));
    public static final SpiritRoot INNATE_METAL = add(new SpiritRoot(
            "innate_metal", "先天金灵根", null, Rarity.INNATE,
            "先天庚金之资，杀伐第一。"));
    public static final SpiritRoot INNATE_WIND = add(new SpiritRoot(
            "innate_wind", "先天风灵根", null, Rarity.INNATE,
            "风过无痕，身法快到看不清。"));

    // ---------------- 混沌灵根 ----------------
    public static final SpiritRoot CHAOS_ICE = add(new SpiritRoot(
            "chaos_ice", "混沌冰灵根", null, Rarity.CHAOS,
            "混沌寒气，一念冰封千里。"));
    public static final SpiritRoot CHAOS_FIRE = add(new SpiritRoot(
            "chaos_fire", "混沌火灵根", null, Rarity.CHAOS,
            "混沌真火，焚尽有形之物。"));

    // ---------------- 鸿蒙灵根（最强） ----------------
    public static final SpiritRoot HONGMENG_THUNDER = add(new SpiritRoot(
            "hongmeng_thunder", "鸿蒙雷灵根", null, Rarity.HONGMENG,
            "鸿蒙紫雷绕身，生者勿近。"));
    public static final SpiritRoot HONGMENG_WIND = add(new SpiritRoot(
            "hongmeng_wind", "鸿蒙风灵根", null, Rarity.HONGMENG,
            "鸿蒙罡风，吹散的不只是肉身。"));


    // ------------------------------------------------------------------
    // 品质 × 属性 全矩阵（玩家要求：灵根品质与属性不挂钩，什么属性都能是鸿蒙）
    // ------------------------------------------------------------------
    //
    // 上面那批常量是**手写**的，所以历史上只有十几个组合：木只有凡品、
    // 鸿蒙只有雷风、混沌只有冰火 —— 于是高难度开局（有保底品质）永远刷不到木灵根，
    // 也就开不了木道宫。
    //
    // 这里把**缺失的组合补全**：6 个品质 × 8 个属性。
    // key 规则与既有常量一致（`<品质小写>_<属性小写>`），所以旧存档照样读得出来；
    // 已经存在的 key 会被 `add()` 覆盖成同一条目，不会重复。
    static {
        ensureMatrix();
    }

    /** 补齐 6 品质 × 8 属性的缺失组合；已存在的 key 不动语义（同名同义）。 */
    // ENSURE_MATRIX
    private static void ensureMatrix() {
        // ---- 凡品 ----
        String[][] MORTAL_SET = {
                {"mortal_metal", "凡品金灵根", "METAL"},
                {"mortal_wood", "凡品木灵根", "WOOD"},
                {"mortal_water", "凡品水灵根", "WATER"},
                {"mortal_fire", "凡品火灵根", "FIRE"},
                {"mortal_earth", "凡品土灵根", "EARTH"},
                {"mortal_ice", "凡品冰灵根", "ICE"},
                {"mortal_thunder", "凡品雷灵根", "THUNDER"},
                {"mortal_wind", "凡品风灵根", "WIND"},
        };
        for (String[] row : MORTAL_SET) {
            if (!REGISTRY.containsKey(row[0])) {
                add(new SpiritRoot(row[0], row[1], null,
                        Rarity.MORTAL, "凡品之资，聊胜于无，修就是了。"));
            }
        }
        // ---- 地品 ----
        String[][] EARTH_SET = {
                {"earth_metal", "地品金灵根", "METAL"},
                {"earth_wood", "地品木灵根", "WOOD"},
                {"earth_water", "地品水灵根", "WATER"},
                {"earth_fire", "地品火灵根", "FIRE"},
                {"earth_earth", "地品土灵根", "EARTH"},
                {"earth_ice", "地品冰灵根", "ICE"},
                {"earth_thunder", "地品雷灵根", "THUNDER"},
                {"earth_wind", "地品风灵根", "WIND"},
        };
        for (String[] row : EARTH_SET) {
            if (!REGISTRY.containsKey(row[0])) {
                add(new SpiritRoot(row[0], row[1], null,
                        Rarity.EARTH, "地品灵根，根基已稳，可入修行之门。"));
            }
        }
        // ---- 天品 ----
        String[][] HEAVEN_SET = {
                {"heaven_metal", "天品金灵根", "METAL"},
                {"heaven_wood", "天品木灵根", "WOOD"},
                {"heaven_water", "天品水灵根", "WATER"},
                {"heaven_fire", "天品火灵根", "FIRE"},
                {"heaven_earth", "天品土灵根", "EARTH"},
                {"heaven_ice", "天品冰灵根", "ICE"},
                {"heaven_thunder", "天品雷灵根", "THUNDER"},
                {"heaven_wind", "天品风灵根", "WIND"},
        };
        for (String[] row : HEAVEN_SET) {
            if (!REGISTRY.containsKey(row[0])) {
                add(new SpiritRoot(row[0], row[1], null,
                        Rarity.HEAVEN, "天品灵根，一方之资，宗门必争。"));
            }
        }
        // ---- 先天 ----
        String[][] INNATE_SET = {
                {"innate_metal", "先天金灵根", "METAL"},
                {"innate_wood", "先天木灵根", "WOOD"},
                {"innate_water", "先天水灵根", "WATER"},
                {"innate_fire", "先天火灵根", "FIRE"},
                {"innate_earth", "先天土灵根", "EARTH"},
                {"innate_ice", "先天冰灵根", "ICE"},
                {"innate_thunder", "先天雷灵根", "THUNDER"},
                {"innate_wind", "先天风灵根", "WIND"},
        };
        for (String[] row : INNATE_SET) {
            if (!REGISTRY.containsKey(row[0])) {
                add(new SpiritRoot(row[0], row[1], null,
                        Rarity.INNATE, "先天灵根，生而知之，道途坦荡。"));
            }
        }
        // ---- 混沌 ----
        String[][] CHAOS_SET = {
                {"chaos_metal", "混沌金灵根", "METAL"},
                {"chaos_wood", "混沌木灵根", "WOOD"},
                {"chaos_water", "混沌水灵根", "WATER"},
                {"chaos_fire", "混沌火灵根", "FIRE"},
                {"chaos_earth", "混沌土灵根", "EARTH"},
                {"chaos_ice", "混沌冰灵根", "ICE"},
                {"chaos_thunder", "混沌雷灵根", "THUNDER"},
                {"chaos_wind", "混沌风灵根", "WIND"},
        };
        for (String[] row : CHAOS_SET) {
            if (!REGISTRY.containsKey(row[0])) {
                add(new SpiritRoot(row[0], row[1], null,
                        Rarity.CHAOS, "混沌灵根，天地未分之气，万法皆可入。"));
            }
        }
        // ---- 鸿蒙 ----
        String[][] HONGMENG_SET = {
                {"hongmeng_metal", "鸿蒙金灵根", "METAL"},
                {"hongmeng_wood", "鸿蒙木灵根", "WOOD"},
                {"hongmeng_water", "鸿蒙水灵根", "WATER"},
                {"hongmeng_fire", "鸿蒙火灵根", "FIRE"},
                {"hongmeng_earth", "鸿蒙土灵根", "EARTH"},
                {"hongmeng_ice", "鸿蒙冰灵根", "ICE"},
                {"hongmeng_thunder", "鸿蒙雷灵根", "THUNDER"},
                {"hongmeng_wind", "鸿蒙风灵根", "WIND"},
        };
        for (String[] row : HONGMENG_SET) {
            if (!REGISTRY.containsKey(row[0])) {
                add(new SpiritRoot(row[0], row[1], null,
                        Rarity.HONGMENG, "鸿蒙灵根，万法之始，诸道之源。"));
            }
        }
    }

    private final String key;
    private final String cn;
    /**
     * ⚠ 已不再使用 —— 灵根的"属性"维度被移除了（玩家要求）。
     *
     * <p>保留这个字段只是为了让构造器签名不变（改动面最小），它恒为 {@code null}。
     * 想拿元素请走<b>领域</b>：{@code data.getDomainData().domains().get(0).element()}。</p>
     *
     * @deprecated 灵根不再有属性；领域才有属性。
     */
    @Deprecated
    private final Element element;
    private final Rarity rarity;
    private final String desc;

    private SpiritRoot(String key, String cn, Element element, Rarity rarity, String desc) {
        this.key = key;
        this.cn = cn;
        this.element = element;
        this.rarity = rarity;
        this.desc = desc;
    }

    private static SpiritRoot add(SpiritRoot root) {
        REGISTRY.put(root.key, root);
        return root;
    }

    public String key() {
        return key;
    }

    /**
     * 显示名：<b>只有品质，不带元素</b>。
     *
     * <p>玩家明确要求「把灵根的属性设定删了」——属性维度（金木水火土风雷冰）
     * 已经不再影响任何数值，但名字里还留着「地品<b>火</b>灵根」，
     * 看上去像还在区分属性（截图里就是这个问题）。</p>
     *
     * <p>所以显示名改为直接取品质名：{@code 地品火灵根 -> 地灵根}。
     * 内部 {@code key} 与 {@code cn} 字段<b>保持不变</b>，
     * 旧存档照样读得出来（读的是 key，不是名字）。</p>
     */
    public String cn() {
        return rarity.cn();
    }

    /**
     * @return 恒为 {@code null} —— 灵根不再有属性。
     * @deprecated 请改用领域的元素（{@code DomainData.Domain#element()}）。
     */
    @Deprecated
    public Element element() {
        return null;
    }

    public Rarity rarity() {
        return rarity;
    }

    /** 修炼速度最终倍率（含品质加成）。 */
    public double multiplier() {
        return rarity.cultivationMultiplier();
    }

    /**
     * 描述：<b>不再提元素</b>。
     *
     * <p>原来每条描述都带元素设定（「火性暴烈，炼丹事半功倍」之类），
     * 而属性维度已经删除 —— 那些话暗示了<b>并不存在</b>的加成，
     * 属于误导。现在只按品质给一句泛用的描述。</p>
     */
    public String desc() {
        return switch (rarity) {
            case FALSE_ROOT -> "灵根驳杂，修行如逆水行舟，需比别人多下苦功。";
            case MORTAL -> "根基尚可，按部就班便能稳步前行。";
            case EARTH -> "地脉钟秀，吐纳之间自有厚力。";
            case HEAVEN -> "天资卓然，一点即通，同辈难望其项背。";
            case INNATE -> "与生俱来的道种，尚未修行便已通灵。";
            case CHAOS -> "混沌未分之气，强横而不驯。";
            case HONGMENG -> "鸿蒙初判，此根几近于道本身。";
        };
    }

    /** 生命/攻击加成（来自品质，独立于境界）。 */
    public double attributeBonus() {
        return rarity.attackBonus();
    }

    public static SpiritRoot byKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return REGISTRY.get(key);
    }

    public static List<SpiritRoot> all() {
        return new ArrayList<>(REGISTRY.values());
    }

    /** 兼容旧存档：把旧版的 key 映射到新版灵根。 */
    public static SpiritRoot byKeyCompat(String key) {
        SpiritRoot direct = byKey(key);
        if (direct != null) {
            return direct;
        }
        if (key == null) {
            return null;
        }
        return switch (key) {
            case "mortal" -> FALSE_MIXED;
            case "yellow_earth" -> MORTAL_EARTH;
            case "yellow_wood" -> MORTAL_WOOD;
            case "yellow_metal" -> EARTH_METAL;
            case "mystic_water" -> EARTH_WATER;
            case "mystic_fire" -> EARTH_FIRE;
            case "mystic_thunder" -> EARTH_THUNDER;
            case "mystic_sword" -> EARTH_METAL;
            case "mystic_ice" -> HEAVEN_ICE;
            case "earth_sword" -> EARTH_METAL;
            case "immortal_ice_sword" -> INNATE_ICE_SWORD;
            case "immortal_sword" -> INNATE_METAL;
            case "immortal_thunder" -> HEAVEN_THUNDER;
            case "hongmeng_demon" -> CHAOS_FIRE;
            case "hongmeng_ghost" -> CHAOS_ICE;
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // 抽取
    // ------------------------------------------------------------------

    /** 按权重抽一个品质。 */
    public static Rarity rollRarity(Random random) {
        double total = 0.0D;
        for (Rarity r : Rarity.values()) {
            total += r.weight();
        }
        double pick = random.nextDouble() * total;
        double cursor = 0.0D;
        for (Rarity r : Rarity.values()) {
            cursor += r.weight();
            if (pick <= cursor) {
                return r;
            }
        }
        return Rarity.FALSE_ROOT;
    }

    /** 从指定品质的池子里随机取一条；池子为空则回落到最弱的伪灵根。 */
    public static SpiritRoot rollOf(Rarity rarity, Random random) {
        List<SpiritRoot> pool = new ArrayList<>();
        for (SpiritRoot root : REGISTRY.values()) {
            if (root.rarity == rarity) {
                pool.add(root);
            }
        }
        if (pool.isEmpty()) {
            return FALSE_MIXED;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    /** 完整抽一次。 */
    public static SpiritRoot roll(Random random) {
        return rollOf(rollRarity(random), random);
    }

    public static SpiritRoot roll() {
        return roll(RANDOM);
    }

    /**
     * 按开局难度抽取。
     *
     * <p>难度越高，品质权重越偏向高档，并且有保底下限（例如鸿蒙难度保底地品）。</p>
     */
    public static SpiritRoot rollByDifficulty(Random random,
                                              com.hongmeng.xiuxian.difficulty.CultivationDifficulty diff) {
        if (diff == null) {
            return roll(random);
        }
        Rarity rarity = Rarity.values()[diff.rollRootRarity(random)];
        return rollOf(rarity, random);
    }

    // ------------------------------------------------------------------
    // 保底：连抽不出货就强制走高档池
    // ------------------------------------------------------------------

    /** 触发保底所需的连续"没出好货"次数。 */
    public static final int PITY_THRESHOLD = 10;

    /** 判定"出货"的门槛：天灵根及以上。 */
    public static final Rarity PITY_GOOD_FROM = Rarity.HEAVEN;

    /** 保底池的权重（只在天灵根及以上里抽）。 */
    private static final double[] PITY_POOL_WEIGHTS = {
            0.40D,   // 天灵根
            0.35D,   // 先天灵根
            0.20D,   // 混沌灵根
            0.05D,   // 鸿蒙灵根
    };

    /** 该品质算不算"出货"。 */
    public static boolean isGood(Rarity rarity) {
        return rarity.ordinal() >= PITY_GOOD_FROM.ordinal();
    }

    /** 保底池抽取：天/先天/混沌/鸿蒙。 */
    public static SpiritRoot rollPity(Random random) {
        double pick = random.nextDouble();
        double cursor = 0.0D;
        Rarity chosen = PITY_GOOD_FROM;
        for (int i = 0; i < PITY_POOL_WEIGHTS.length; i++) {
            cursor += PITY_POOL_WEIGHTS[i];
            if (pick <= cursor) {
                chosen = Rarity.values()[PITY_GOOD_FROM.ordinal() + i];
                break;
            }
        }
        return rollOf(chosen, random);
    }

    /** 洗灵丹 / 保底重抽：至少给地灵根以上，避免玩家被反复恶心。 */
    public static SpiritRoot rollGuaranteed(Random random) {
        Rarity rolled = rollRarity(random);
        if (rolled.ordinal() < Rarity.EARTH.ordinal()) {
            rolled = Rarity.EARTH;
        }
        return rollOf(rolled, random);
    }

    public static SpiritRoot rollGuaranteed() {
        return rollGuaranteed(RANDOM);
    }

    /** 某个品质下有几条灵根，用于面板与验证。 */
    public static int countOf(Rarity rarity) {
        int n = 0;
        for (SpiritRoot root : REGISTRY.values()) {
            if (root.rarity == rarity) {
                n++;
            }
        }
        return n;
    }

    
}
