package com.hongmeng.xiuxian.alchemy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 炼丹配方表：<b>主料 + 辅料 → 丹药</b>。
 *
 * <p><b>这个类刻意不引用任何 Minecraft 类型</b>（没有 {@code ItemStack}、
 * 没有 {@code Item}、没有 {@code ResourceLocation}）——全是字符串。
 * 原因有两个，都很实际：</p>
 *
 * <ol>
 *   <li><b>离线可断言</b>：{@code Blocks} / {@code ItemStack} 这些类的静态初始化会抛
 *       {@code Not bootstrapped}（注册表没起来）。只要表里出现 Minecraft 类型，
 *       离线校验器一碰就炸。纯字符串的表可以在没有游戏的情况下跑
 *       {@link #validate()} 与 {@code tools/check_herb_alchemy.py}。</li>
 *   <li><b>配方要好改</b>：丹方是策划数值，不是代码逻辑。加一行 {@link Brew}
 *       就能加一个丹方，不必碰方块实体里的任何一行。</li>
 * </ol>
 *
 * <p>id 写法：本模组的物品写**裸名**（{@code xuelian_grass_petal}），
 * 原版物品写**全名**（{@code minecraft:blaze_powder}）。{@link #fullId(String)}
 * 负责补前缀，{@link #normalize(String)} 负责比较时统一口径。</p>
 *
 * <h2>丹药覆盖率</h2>
 * <p>{@link #PILL_IDS} 是 {@code item/} 下**所有**丹药物品的注册名。
 * {@link #validate()} 会在模组加载（和离线校验）时检查它们**全部**可炼，
 * 漏一颗就直接抛异常——宁可开服就炸，也不要玩家把材料丢进丹炉才发现炼不出来。</p>
 */
public final class AlchemyRecipes {

    /** 本模组命名空间。 */
    public static final String NS = "hongmeng:";

    /** 一炉的炼制时间：200 tick = 10 秒。 */
    public static final int BREW_TICKS = 200;

    /** 燃料：灵叶。1 片 = 正好一炉（200 tick）。 */
    public static final String FUEL_LEAF = "lingye";
    public static final int FUEL_LEAF_TICKS = BREW_TICKS;

    /** 燃料：烈焰粉。原版酿造台的老燃料，1 份 = 12 炉。 */
    public static final String FUEL_BLAZE = "minecraft:blaze_powder";
    public static final int FUEL_BLAZE_TICKS = BREW_TICKS * 12;

    /**
     * 一个丹方。
     *
     * @param main   主料（物品 id）
     * @param aux    辅料（物品 id）
     * @param result 产物（物品 id）
     * @param count  一炉产出几个
     * @param tier   丹方品阶 1..6，用于界面提示与文档排序
     * @param note   中文备注（写进文档与自检输出，游戏里不显示）
     */
    public record Brew(String main, String aux, String result, int count, int tier, String note) {

        /**
         * 炼制时间：**所有丹方统一 200 tick（10 秒）**。
         *
         * <p>刻意不按品阶拉长：丹炉是"多次重复"的生产设施，10 秒一炉是玩家
         * 站在炉边等一等的合理长度；品阶的差别体现在"材料难得"和"产出多少"，
         * 而不是让高阶丹药变成挂机。</p>
         */
        public int brewTicks() {
            return BREW_TICKS;
        }
    }

    /**
     * 全部丹药物品（{@code item/} 下的九颗，含三档修为丹与造化丹）。
     *
     * <p>改 {@code ModItems} 里的丹药时，这里必须同步——离线校验器
     * {@code tools/check_herb_alchemy.py} 会拿 {@code ModItems.java} 的实际注册名
     * 和这张表对撞。</p>
     */
    public static final List<String> PILL_IDS = List.of(
            "pill_huiling",
            "pill_exp_common",
            "zhaohua_dan",
            "pill_exp_perfect",
            "pill_zhuji",
            "pill_xiling",
            "pill_pojing",
            "pill_exp_immortal",
            "pill_shuangdao");

    /**
     * 丹方总表。顺序 = 文档里的顺序 = 自检报告里的顺序。
     *
     * <p>设计口径：</p>
     * <ul>
     *   <li>**每颗丹药至少一条方子**，九颗全覆盖；</li>
     *   <li>**每一阶灵植都当过一次主料**（雪莲草→火焰花→寒冰藤→雷音草→空冥兰→血珞芝），
     *       否则高阶灵植就没有存在意义；</li>
     *   <li>低阶丹用低阶灵植 + 常见材料（灵叶、小麦、萤石粉），
     *       高阶丹才要灵晶 / 空冥精 / 道印这些副本产物；</li>
     *   <li>一炉的产出（1~2 颗）**明显优于**工作台直接合成（见 docs/herb-alchemy-spec.md
     *       的"为什么不删工作台配方"一节）。</li>
     * </ul>
     */
    public static final List<Brew> BREWS = List.of(
            // ---- 一阶：雪莲草（回灵 / 疗伤） ----
            new Brew("xuelian_grass_petal", "lingye", "pill_huiling", 2, 1,
                    "回灵丹：雪莲草养脉，灵叶引气，一炉出两颗"),
            new Brew("xuelian_grass_petal", "minecraft:wheat", "pill_exp_common", 2, 1,
                    "普通修为丹：五谷精气打底，最便宜的修为来源"),

            // ---- 二阶：火焰花（攻击 / 火系） ----
            new Brew("huoyan_hua_petal", "xuelian_grass_petal", "zhaohua_dan", 1, 2,
                    "造化丹：火性冲关 + 雪莲护脉，未觉醒者开脉"),
            new Brew("huoyan_hua_petal", "minecraft:glowstone_dust", "pill_exp_perfect", 1, 2,
                    "完美修为丹：萤石粉聚灵，把火焰花的药力压进丹心"),

            // ---- 三阶：寒冰藤（控制 / 冰系） ----
            new Brew("hanbing_teng_petal", "spirit_crystal", "pill_zhuji", 1, 3,
                    "筑基丹：灵晶为骨、寒冰藤为引，压住筑基时的躁气"),
            new Brew("hanbing_teng_petal", "minecraft:amethyst_shard", "pill_xiling", 1, 3,
                    "洗灵丹：紫水晶洗脉，寒冰藤镇住重测灵根的反噬"),

            // ---- 四阶：雷音草（雷系 / 突破） ----
            new Brew("leiyin_cao_petal", "minecraft:blaze_powder", "pill_pojing", 1, 4,
                    "破境丹：雷音引动天雷之势，烈焰粉点火，突破时顶一手"),
            new Brew("leiyin_cao_petal", "kongming_essence", "pill_exp_immortal", 1, 4,
                    "仙品修为丹：空冥精把雷音草的药力锁在丹里不散"),

            // ---- 五阶：空冥兰（高阶 / 空间） ----
            new Brew("kongming_lan_petal", "daoyin", "pill_shuangdao", 1, 5,
                    "双道宫丹：道印为引、空冥兰开空间，凝第二座道宫"),
            new Brew("kongming_lan_petal", "lingye", "pill_exp_perfect", 1, 5,
                    "完美修为丹（高阶方）：空冥兰替代萤石，出丹更稳"),

            // ---- 六阶：血珞芝（顶级 / 血道） ----
            new Brew("xueluo_zhi_petal", "spirit_crystal", "pill_zhuji", 1, 6,
                    "筑基丹（血道方）：血珞芝强行养脉，快但奢侈"),
            new Brew("xueluo_zhi_petal", "kongming_essence", "pill_exp_immortal", 1, 6,
                    "仙品修为丹（血道方）：血珞芝与空冥精同炉，顶级修为丹")
    );

    private AlchemyRecipes() {
    }

    /** 补全命名空间：裸名 → {@code hongmeng:<裸名>}。 */
    public static String fullId(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        return id.indexOf(':') >= 0 ? id : NS + id;
    }

    /** 比较用的统一口径：本模组物品一律去掉 {@code hongmeng:} 前缀。 */
    public static String normalize(String id) {
        if (id == null) {
            return "";
        }
        String lower = id.toLowerCase(Locale.ROOT);
        return lower.startsWith(NS) ? lower.substring(NS.length()) : lower;
    }

    /**
     * 查方子。主料 / 辅料**顺序无关**（谁放主料槽、谁放辅料槽都能炼），
     * 这样玩家不必背"哪个放左边"。
     *
     * @return 命中的丹方；没有方子返回 {@code null}
     */
    public static Brew find(String mainId, String auxId) {
        String a = normalize(mainId);
        String b = normalize(auxId);
        if (a.isEmpty() || b.isEmpty()) {
            return null;
        }
        for (Brew brew : BREWS) {
            String m = normalize(brew.main());
            String n = normalize(brew.aux());
            if ((m.equals(a) && n.equals(b)) || (m.equals(b) && n.equals(a))) {
                return brew;
            }
        }
        return null;
    }

    /** 这份材料能不能当燃料。 */
    public static boolean isFuel(String itemId) {
        return fuelTicks(itemId) > 0;
    }

    /** 燃料能烧多久（tick）；不是燃料返回 0。 */
    public static int fuelTicks(String itemId) {
        String id = normalize(itemId);
        if (id.equals(normalize(FUEL_LEAF))) {
            return FUEL_LEAF_TICKS;
        }
        if (id.equals(normalize(FUEL_BLAZE))) {
            return FUEL_BLAZE_TICKS;
        }
        return 0;
    }

    /** 表里出现过的所有产物（去重、保持顺序）。 */
    public static List<String> coveredPills() {
        Set<String> out = new LinkedHashSet<>();
        for (Brew brew : BREWS) {
            out.add(brew.result());
        }
        return Collections.unmodifiableList(new ArrayList<>(out));
    }

    /** 表里出现过的所有材料（去重、保持顺序）。 */
    public static List<String> allIngredients() {
        Set<String> out = new LinkedHashSet<>();
        for (Brew brew : BREWS) {
            out.add(brew.main());
            out.add(brew.aux());
        }
        return Collections.unmodifiableList(new ArrayList<>(out));
    }

    /** 一炉的产出数量上限（用于产物槽的堆叠判断）。 */
    public static int maxResultCount() {
        int max = 1;
        for (Brew brew : BREWS) {
            max = Math.max(max, brew.count());
        }
        return max;
    }

    /**
     * 自检。**发现任何一处坏掉就抛异常**，绝不"静默跳过"。
     *
     * <p>在 {@link ModAlchemy#register(net.minecraftforge.eventbus.api.IEventBus)}
     * 里调用，所以表写错的表现是"模组加载失败 + 一条说得清原因的异常"，
     * 而不是"玩家炼不出丹"。{@code tools/check_herb_alchemy.py} 离线跑同一套断言。</p>
     *
     * @throws IllegalStateException 表不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();

        if (BREWS.isEmpty()) {
            problems.add("丹方表是空的");
        }

        Set<String> combos = new LinkedHashSet<>();
        for (Brew brew : BREWS) {
            String label = brew.main() + " + " + brew.aux();
            if (brew.main() == null || brew.main().isBlank()
                    || brew.aux() == null || brew.aux().isBlank()) {
                problems.add("有丹方缺主料或辅料：" + label);
                continue;
            }
            if (brew.result() == null || brew.result().isBlank()) {
                problems.add("有丹方没有产物：" + label);
                continue;
            }
            if (brew.main().equals(brew.aux())) {
                problems.add("主料与辅料相同（无法区分）：" + label);
            }
            if (brew.main().equals(brew.result()) || brew.aux().equals(brew.result())) {
                problems.add("产物与材料相同：" + label + " -> " + brew.result());
            }
            if (brew.count() < 1 || brew.count() > 4) {
                problems.add("产出数量不在 1..4：" + label + " -> " + brew.count());
            }
            if (brew.tier() < 1 || brew.tier() > 6) {
                problems.add("丹方品阶不在 1..6：" + label + " -> " + brew.tier());
            }
            if (brew.brewTicks() <= 0) {
                problems.add("炼制时间为 0：" + label);
            }
            if (!PILL_IDS.contains(brew.result())) {
                problems.add("产物不是已注册的丹药（PILL_IDS 里没有）：" + brew.result());
            }
            // 主料/辅料互换视作同一个组合，重复就是歧义
            String key = normalize(brew.main()) + "+" + normalize(brew.aux());
            String mirror = normalize(brew.aux()) + "+" + normalize(brew.main());
            if (combos.contains(key) || combos.contains(mirror)) {
                problems.add("重复丹方（同一对材料出现两次，result 会打架）：" + label);
            }
            combos.add(key);
        }

        Set<String> covered = new LinkedHashSet<>(coveredPills());
        for (String pill : PILL_IDS) {
            if (!covered.contains(pill)) {
                problems.add("丹药没有任何丹方（玩家炼不出来）：" + pill);
            }
        }
        for (String pill : covered) {
            if (!PILL_IDS.contains(pill)) {
                problems.add("丹方产物不在 PILL_IDS 里（大概率是拼写错误）：" + pill);
            }
        }

        if (!isFuel(FUEL_LEAF) || !isFuel(FUEL_BLAZE)) {
            problems.add("燃料配错了：灵叶或烈焰粉查不到燃烧时间");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 炼丹配方表不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }
}
