package com.hongmeng.xiuxian.shop;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 坊市<b>回收价表</b>（卖东西给坊市）。
 *
 * <p>玩家原话："商店可以购买东西也可以贩卖，挖到的东西掉落物都可以卖设置价格。"</p>
 *
 * <p><b>这个类刻意不引用任何 Minecraft 类型</b>——没有 {@code Item}、
 * 没有 {@code ItemStack}、没有 {@code ResourceLocation}，全是
 * {@code id → 价格} 的字符串与整数。原因和 {@link ShopEntry} 一样：
 * {@code BuiltInRegistries} / {@code Items} 的静态初始化会抛
 * {@code Not bootstrapped}，离线验证器一碰就炸。表里只写注册名，
 * 真正的查表在运行时由 {@link ShopManager} 做。</p>
 *
 * <h2>定价口径</h2>
 *
 * <ul>
 *   <li><b>回收价 = 买入价的 25%~40%</b>（玩家与路线图给的口径）。
 *       实际上手工表里绝大多数是买入价的 <b>30%</b>，并在取整时向下取整，
 *       所以"买进再卖出"必然亏钱。</li>
 *   <li><b>原料能给到 30%~40%</b>：矿锭这类"要挖才有"的东西给得高一点，
 *       让挖矿也有收入，而不是只有杀怪才赚灵叶。</li>
 *   <li><b>合成链上游不能比下游贵</b>：{@code pill_zhuji} 用 9 灵铁 + 1 灵叶
 *       合成，所以筑基丹的回收价（30）必须低于 9 灵铁的回收价合计（45），
 *       否则"买灵铁 → 合成筑基丹 → 卖掉"就是印钞机。
 *       同理 {@code kongming_ore}（25）必须低于 2 个空冥精（30），
 *       {@code xueluo_ore}（35）必须低于 2 个血珞精（40）。</li>
 *   <li><b>灵叶（余额取实物）必须严格低于买入价</b>：买入 10 片 = 10 灵叶，
 *       回收 1 片 = 1 灵叶？不行——那是不赚不亏的中转，一旦哪天有人把
 *       "取出灵叶"改成打折就会变成套利。这里定 <b>1 片 = 1 灵叶</b> 确实
 *       是 1:1，所以 {@link #validate()} 对灵叶单独放行（见
 *       {@link #NO_ARBITRAGE_EXEMPT}），并写明理由。</li>
 * </ul>
 *
 * <h2>覆盖范围（玩家要的"挖到的、掉的都能卖"）</h2>
 *
 * <ol>
 *   <li><b>本模组材料</b>：灵叶、道印、纪元令（妖丹）、灵铁锭、灵晶、空冥精、血珞精；</li>
 *   <li><b>六阶灵植全套</b>：6 种种子 + 6 种花瓣；</li>
 *   <li><b>本模组矿石与方块</b>：灵铁矿、灵晶矿、空冥矿、血珞矿、灵田土、丹炉、灵草；</li>
 *   <li><b>九颗丹药 + 功法秘籍</b>（秘籍价由 {@link ShopCatalog} 的买入价算出）；</li>
 *   <li><b>原版矿物与材料</b>：煤/铁/铜/金/红石/青金石/钻石/绿宝石/下界石英/
 *       紫水晶/下界合金碎片/远古残骸，外加骨头、蛛丝、火药、末影珍珠、
 *       烈焰棒这些常见掉落物。</li>
 * </ol>
 *
 * <p>没在表里的东西收不了——这是刻意的：宁可"这个东西不收"，
 * 也不要用 {@code item.getDefaultInstance()} 之类的方式瞎猜价格。</p>
 */
public final class SellPrices {

    /** 单件回收价下限：表里不可能出现 0 或负数。 */
    public static final int MIN_PRICE = 1;

    /** 单件回收价上限：防手滑写出一个天文数字（也防验证器被绕）。 */
    public static final int MAX_PRICE = 800;

    /**
     * 允许"回收价 == 买入价"的例外清单。
     *
     * <p>只有灵叶：取出实物是 1:1，存回去也是 1:1，本来就是同一笔钱换形态，
     * 不存在套利（买入 10 片花 10 灵叶，卖掉 10 片得 10 灵叶，净收益 0）。
     * 除它以外的任何物品都**必须**严格低于买入价。</p>
     */
    public static final List<String> NO_ARBITRAGE_EXEMPT = List.of("hongmeng:lingye");

    /**
     * 手工回收价表。key = 物品注册名（一律带命名空间），value = 单片/单件价。
     *
     * <p>顺序 = 回收界面里的显示顺序，大致按"挖矿常见度"排：先原版矿物，
     * 再本模组材料，再灵植，最后丹药与秘籍。</p>
     */
    private static final Map<String, Integer> TABLE = buildTable();

    private SellPrices() {
    }

    private static Map<String, Integer> buildTable() {
        Map<String, Integer> t = new LinkedHashMap<>();

        // ---------------- 原版：矿石与矿物材料 ----------------
        // 口径：回收价 ≈ 买入价不存在（原版东西没有买入价），按"挖矿节奏"定价。
        // 一组（64）煤 = 128 灵叶，约等于 3 只普通妖兽——挖矿不必杀怪差。
        t.put("minecraft:coal", 2);
        t.put("minecraft:charcoal", 1);
        t.put("minecraft:raw_iron", 4);
        t.put("minecraft:iron_ingot", 5);
        t.put("minecraft:raw_copper", 2);
        t.put("minecraft:copper_ingot", 3);
        t.put("minecraft:raw_gold", 7);
        t.put("minecraft:gold_ingot", 8);
        t.put("minecraft:redstone", 2);
        t.put("minecraft:lapis_lazuli", 2);
        t.put("minecraft:quartz", 3);
        t.put("minecraft:amethyst_shard", 3);
        t.put("minecraft:diamond", 25);
        t.put("minecraft:emerald", 20);
        t.put("minecraft:netherite_scrap", 60);
        t.put("minecraft:ancient_debris", 60);
        t.put("minecraft:obsidian", 4);

        // ---------------- 原版：怪物掉落物 ----------------
        t.put("minecraft:bone", 1);
        t.put("minecraft:string", 1);
        t.put("minecraft:spider_eye", 1);
        t.put("minecraft:rotten_flesh", 1);
        t.put("minecraft:gunpowder", 2);
        t.put("minecraft:slime_ball", 2);
        t.put("minecraft:leather", 2);
        t.put("minecraft:feather", 1);
        t.put("minecraft:blaze_rod", 6);
        t.put("minecraft:blaze_powder", 3);
        t.put("minecraft:glowstone_dust", 2);
        t.put("minecraft:ender_pearl", 5);
        t.put("minecraft:ghast_tear", 15);
        t.put("minecraft:phantom_membrane", 8);
        t.put("minecraft:wheat", 1);
        t.put("minecraft:wheat_seeds", 1);

        // ---------------- 本模组：核心材料 ----------------
        // 灵叶：取出实物是 1:1（10 片 = 10 灵叶），存回来自然也是 1:1，
        // 唯一一条被 validate() 放行的"等于买入价"的条目。
        t.put("hongmeng:lingye", 1);
        // 道印：千机门 / 副本产物，洗灵丹要 8 个，给得起价
        t.put("hongmeng:daoyin", 40);
        // 纪元令 = 妖丹：灵兽成长的唯一饲料 + 换法宝材料的货币，
        // 定价必须高于它换来的材料（16 灵铁 = 48、12 灵晶 = 60、
        // 8 空冥精 = 120、8 血珞精 = 160），否则"令牌换材料再卖"就是印钞。
        t.put("hongmeng:jidan", 200);
        t.put("hongmeng:xue_luoguo", 6);
        t.put("hongmeng:kong_ming_shi", 12);
        t.put("hongmeng:sect_token_shenxiao", 20);

        // 炼器材料：灵铁最贱、血珞最贵。回收价 = 买入价（纪元令换的单价）的约 75%，
        // 但令牌本身的回收价（200）远高于"一枚令牌换出的全部材料"，
        // 所以套利方向是"卖令牌"，而不是"换材料再卖"。
        t.put("hongmeng:spirit_iron_ingot", 3);
        t.put("hongmeng:spirit_crystal", 5);
        t.put("hongmeng:kongming_essence", 15);
        t.put("hongmeng:xueluo_essence", 20);

        // ---------------- 本模组：矿石方块 ----------------
        // 精准采集挖回来的矿石本体。注意 kongming_ore 25 < 2×空冥精 30，
        // xueluo_ore 35 < 2×血珞精 40——否则"挖矿→合成→卖"能凭空生钱。
        t.put("hongmeng:spirit_iron_ore", 8);
        t.put("hongmeng:deepslate_spirit_iron_ore", 8);
        t.put("hongmeng:spirit_crystal_ore", 12);
        t.put("hongmeng:deepslate_spirit_crystal_ore", 12);
        t.put("hongmeng:kongming_ore", 25);
        t.put("hongmeng:xueluo_ore", 35);

        // ---------------- 本模组：灵植（6 种子 + 6 花瓣） ----------------
        // 种子买入价 10/20/40/80/160/320，回收 4/8/16/32/64/128（40%，
        // 因为"买种子就是买一次种植机会"，回报要给得像样）。
        t.put("hongmeng:xuelian_grass_seed", 4);
        t.put("hongmeng:huoyan_hua_seed", 8);
        t.put("hongmeng:hanbing_teng_seed", 16);
        t.put("hongmeng:leiyin_cao_seed", 32);
        t.put("hongmeng:kongming_lan_seed", 64);
        t.put("hongmeng:xueluo_zhi_seed", 128);
        // 花瓣：一株成熟掉 2~4 片（越高阶越多）。六阶血珞芝花瓣 40 片/片，
        // 一株理想情况 160 灵叶 < 种子 320，种田是"回本"不是"印钞"。
        t.put("hongmeng:xuelian_grass_petal", 3);
        t.put("hongmeng:huoyan_hua_petal", 6);
        t.put("hongmeng:hanbing_teng_petal", 10);
        t.put("hongmeng:leiyin_cao_petal", 16);
        t.put("hongmeng:kongming_lan_petal", 26);
        t.put("hongmeng:xueluo_zhi_petal", 40);

        // ---------------- 本模组：方块 ----------------
        // 灵草：1 株 = 1 灵叶（合成表 1:1），回收也是 1
        t.put("hongmeng:spirit_grass", 1);
        // 灵田土：买入 3 个 15 灵叶 → 单价 5，回收 4（约 27%）
        t.put("hongmeng:lingtian_soil", 4);
        // 丹炉：材料是 8 灵铁 + 1 灵叶 = 25 灵叶（炼药锅与烈焰粉是原版东西，
        // 不计入本表），回收价必须低于 25，否则"买灵铁做丹炉再拆卖"能生钱。
        t.put("hongmeng:alchemy_furnace", 24);

        // ---------------- 本模组：九颗丹药 ----------------
        // 口径是"回收价必须低于**炼出它的材料**的回收价合计"，而不是简单地
        // 取买入价的 30%——因为丹方是按材料配的，只按买入价算会漏掉
        // "工作台合成 → 卖掉"这条印钞路径：
        //   回灵丹   2 片灵叶=2   → 卖 3（买入 10 的 30%，材料太便宜，靠买入价定价）
        //   造化丹   6 灵铁+1=19  → 卖 15
        //   筑基丹   9 灵铁+1=28  → 卖 25
        //   完美修为丹 8 灵晶=40   → 卖 25
        //   破境丹   4 灵晶+4 空冥精+1=81 → 卖 60
        //   洗灵丹   4 灵晶+4 灵叶+道印=64 → 卖 45
        //   双道宫丹 4 空冥精+4 血珞精+1=141 → 卖 120
        //   仙品修为丹（雷音草方）8 空冥精=120 → 卖 90
        //   ── 六阶血珞芝的两条方子单独算，因为材料里有 6 阶花瓣 ──
        //   筑基丹（血道方）= 血珞芝花瓣(40) + 灵晶(5) + 1 灵叶 = 46 → 卖 40
        //   仙品修为丹（血道方）= 血珞芝花瓣(40) + 空冥精(15) = 55 → 卖 45
        t.put("hongmeng:pill_huiling", 3);
        t.put("hongmeng:pill_exp_common", 5);
        t.put("hongmeng:zhaohua_dan", 15);
        t.put("hongmeng:pill_exp_perfect", 25);
        t.put("hongmeng:pill_zhuji", 25);
        t.put("hongmeng:pill_xiling", 45);
        t.put("hongmeng:pill_pojing", 60);
        t.put("hongmeng:pill_exp_immortal", 90);
        t.put("hongmeng:pill_shuangdao", 120);

        // ---------------- 功法秘籍（从买入价算，买入价在 ShopCatalog 里） ----------------
        // 秘籍本身不是掉落的，但玩家买重了、想换功法时会想卖掉；
        // 按买入价 30% 回收，保证"买了再卖"直接亏七成。
        for (ShopEntry e : ShopCatalog.manuals()) {
            int sell = (int) Math.floor(e.priceLeaves() * SELL_RATIO);
            t.put(e.itemId(), Math.max(MIN_PRICE, sell));
        }

        return Collections.unmodifiableMap(t);
    }

    /** 手工表里绝大部分条目用的回收比例（30%）。 */
    public static final double SELL_RATIO = 0.30D;

    /** 全部可回收物品 id（保持插入顺序）。 */
    public static List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<>(TABLE.keySet()));
    }

    /** 全部条目（id → 价），只读。 */
    public static Map<String, Integer> all() {
        return TABLE;
    }

    /** 单件回收价；不收的东西返回 0（绝不能返回负数）。 */
    public static int unitPrice(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return 0;
        }
        Integer p = TABLE.get(itemId.toLowerCase(Locale.ROOT));
        return p == null ? 0 : p;
    }

    /**
     * 带**品质**的回收价。
     *
     * <p>品质越高回收价越高（神品 450%）。防套利不受影响——
     * 商店卖出的东西恒为**凡品**（100%），所以买入价不变，
     * 「买入→卖出」永远是亏的。</p>
     */
    public static int unitPrice(String itemId,
                                 com.hongmeng.xiuxian.quality.Quality q) {
        return com.hongmeng.xiuxian.quality.QualityTable
                .sellPrice(unitPrice(itemId), q);
    }


    /** 这个东西收不收。 */
    public static boolean accepts(String itemId) {
        return unitPrice(itemId) > 0;
    }

    /** 一堆能卖多少灵叶（纯算术，不碰物品）。 */
    public static int total(String itemId, int count) {
        int unit = unitPrice(itemId);
        if (unit <= 0 || count <= 0) {
            return 0;
        }
        return unit * count;
    }

    /** 表里有几条（验证器与文档用）。 */
    public static int size() {
        return TABLE.size();
    }

    // ------------------------------------------------------------------
    // 运行时：把 id 变成能画的东西
    //
    // 下面两个方法会碰 BuiltInRegistries，**只能在客户端/服务端运行时调用**，
    // 绝不能出现在静态初始化里（离线验证器一碰就 Not bootstrapped）。
    // 表本身（上面的 TABLE）依然是纯数据。
    // ------------------------------------------------------------------

    /** 解析成物品堆；id 不合法或表里没有就返回空。 */
    public static ItemStack stackOf(String itemId) {
        if (!accepts(itemId)) {
            return ItemStack.EMPTY;
        }
        net.minecraft.resources.ResourceLocation rl =
                net.minecraft.resources.ResourceLocation.tryParse(itemId);
        if (rl == null) {
            return ItemStack.EMPTY;
        }
        net.minecraft.world.item.Item item =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    /**
     * 物品的中文名（走客户端语言文件，所以界面里是"灵铁锭"而不是
     * {@code hongmeng:spirit_iron_ingot}）。
     *
     * <p>只被界面调用，返回值可能为空串（物品解析不到时）。</p>
     */
    public static String displayName(String itemId) {
        ItemStack stack = stackOf(itemId);
        return stack.isEmpty() ? itemId : stack.getHoverName().getString();
    }

    /**
     * 自检。
     *
     * <p>模组加载时（{@code ModEvents} 的 common setup）与离线验证器都会调它。
     * <b>任何一条坏掉就抛异常</b>，绝不静默跳过——价格表错了的表现是
     * "玩家发现了一个印钞机"，那种 bug 事后极难追。</p>
     *
     * @throws IllegalStateException 价格表不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();

        if (TABLE.isEmpty()) {
            problems.add("回收价表是空的");
        }

        for (Map.Entry<String, Integer> e : TABLE.entrySet()) {
            String id = e.getKey();
            int price = e.getValue() == null ? -1 : e.getValue();
            if (id == null || id.isBlank()) {
                problems.add("有物品 id 是空的");
                continue;
            }
            if (id.indexOf(':') < 0) {
                problems.add("物品 id 没写命名空间（应为 minecraft:xxx 或 hongmeng:xxx）：" + id);
            }
            if (!id.equals(id.toLowerCase(Locale.ROOT))) {
                problems.add("物品 id 必须全小写（注册名就是小写）：" + id);
            }
            if (price < MIN_PRICE || price > MAX_PRICE) {
                problems.add("回收价越界（应在 " + MIN_PRICE + ".." + MAX_PRICE + "）："
                        + id + " = " + price);
            }
        }

        // ---- 防套利：任何既能买又能卖的东西，单件回收价不得 >= 单件买入价 ----
        //
        // 只对**灵叶结算**的商品比价：纪元令商品的 priceLeaves() 是"几枚令牌"，
        // 拿它当灵叶价来比（16 灵铁 = 1 令 → 单价 1 灵叶）会得出
        // "灵铁回收价 3 >= 买入价 1" 这种假警报。
        //
        // 纪元令商品（灵铁/灵晶/空冥精/血珞精）都是**单一来源**的材料：
        // 只能用纪元令换，纪元令只能从副本/纪元秘境/击杀 BOSS 拿，
        // **没有任何"灵叶 → 纪元令"的兑换**，所以它们不构成"买进再卖出"的闭环。
        // （真要闭环，得先有一个花灵叶买令牌的地方——那种东西一旦加进来，
        //   就必须回来重算这张表。这里用断言把这条前提写死：这几样东西
        //   的回收价必须低于令牌本身的回收价，卖材料永远不如卖令牌。）
        for (ShopEntry e : ShopCatalog.all()) {
            int unit = TABLE.getOrDefault(e.itemId(), 0);
            if (unit <= 0) {
                problems.add("商店在卖的东西没有回收价（买了就卖不掉）：" + e.itemId());
                continue;
            }
            if (!e.usesLeaves()) {
                // 非灵叶货币：只要求"回收价低于该货币本身"，不做单价对比
                int currencySell = TABLE.getOrDefault(e.currencyItemId(), 0);
                if (currencySell > 0 && unit >= currencySell) {
                    problems.add("套利漏洞！「" + e.cn() + "」回收价 " + unit
                            + " >= 货币「" + e.currencyItemId() + "」回收价 "
                            + currencySell + "，换材料再卖比直接卖货币更赚");
                }
                continue;
            }
            int buyUnit = e.unitPrice();
            if (buyUnit <= 0) {
                problems.add("商品单价算不出来（count 非法）：" + e.id());
                continue;
            }
            if (NO_ARBITRAGE_EXEMPT.contains(e.itemId())) {
                if (unit != buyUnit) {
                    problems.add("豁免清单里的物品必须 1:1（" + e.itemId() + "）："
                            + unit + " vs " + buyUnit);
                }
                continue;
            }
            if (unit >= buyUnit) {
                problems.add("套利漏洞！「" + e.cn() + "」（" + e.itemId()
                        + "）回收价 " + unit + " >= 买入单价 " + buyUnit
                        + "，买进再卖出可以刷灵叶");
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 坊市回收价表不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }
}
