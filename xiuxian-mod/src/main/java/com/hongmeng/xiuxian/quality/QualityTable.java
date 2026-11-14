package com.hongmeng.xiuxian.quality;

import com.hongmeng.xiuxian.alchemy.AlchemyRecipes;
import com.hongmeng.xiuxian.alchemy.HerbCatalog;
import com.hongmeng.xiuxian.item.gear.GearNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>哪些东西有品质、天生是什么品质、掉落时偏移多少</b>——纯数据表。
 *
 * <p>和 {@code SellPrices} / {@code AlchemyRecipes} 一个口径：
 * <b>这个类不引用任何 Minecraft 类型</b>（没有 {@code Item}、没有 {@code ItemStack}、
 * 没有 {@code ResourceLocation}），所以离线验证器可以直接断言它，
 * 而不会踩 {@code Not bootstrapped}。运行时把 id 变成对象的事情在
 * {@link ItemQuality} 里做。</p>
 *
 * <h2>覆盖范围（路线图 2.2 的"至少覆盖"）</h2>
 * <ol>
 *   <li><b>丹药</b>：{@link AlchemyRecipes#PILL_IDS} 的九颗，一颗不落；</li>
 *   <li><b>法宝</b>：5 部位 × 10 境 = 50 件；</li>
 *   <li><b>法宝剑</b>：10 把；</li>
 *   <li><b>灵植花瓣</b>：六阶各一种，品质 = 灵植品阶（{@link Quality#forTier(int)}）；</li>
 *   <li><b>妖丹</b>：{@code jidan}（灵兽饲料 / 换材料的货币）。</li>
 * </ol>
 *
 * <h2>天生品质（intrinsic）</h2>
 * <p>"天生品质"是<b>没有 NBT 时的默认档</b>。绝大多数东西是凡品，
 * 只有两类有更高的天生档：</p>
 * <ul>
 *   <li><b>三档修为丹</b>：1 / 3 / 10 的倍率锚点，改成别的数字会让
 *       既有平衡与 tooltip 全部失真；</li>
 *   <li><b>六阶灵植花瓣</b>：一阶凡品 → 六阶神品，与灵植品阶一一对应。</li>
 * </ul>
 *
 * <h2>合成品质</h2>
 * <p>{@link #craftedQuality(String)} = <b>max(凡品, 天生品质)</b>。
 * 合成<b>不会</b>凭空出好货（合成 = 凡品），但也<b>不会把好东西拉低</b>——
 * 否则合成一颗完美修为丹会把它从 ×3 打成 ×1，那是纯粹的数值事故。</p>
 */
public final class QualityTable {

    /** 本模组命名空间（与炼丹表同一个常量口径）。 */
    public static final String NS = "hongmeng:";

    /** 妖丹：妖兽掉落，品质影响回收价。 */
    public static final String JIDAN = "jidan";

    /** 天生品质表（id → 品质）。顺序 = 文档顺序。 */
    private static final Map<String, Quality> INTRINSIC = buildIntrinsic();

    /**
     * 妖兽掉落的强度阈值（生命上限 → 难度偏移 0..4）。
     *
     * <p>用生命上限当"怪有多强"的代理指标，是因为它<b>已经被难度与境界缩放过了</b>
     * （{@code MobScaling}），副本里的怪血量高得多，于是副本掉落天然偏移更高。
     * 阈值是纯数据，可离线断言"单调不降"。</p>
     */
    public static final double[] DROP_SHIFT_HEALTH = {40.0D, 120.0D, 400.0D, 1500.0D};

    private QualityTable() {
    }

    private static Map<String, Quality> buildIntrinsic() {
        Map<String, Quality> t = new LinkedHashMap<>();

        // ---------------- 九颗丹药 ----------------
        // 只有三档修为丹天生带倍率（1 / 3 / 10 是既有平衡的锚点）；
        // 其余丹药天生凡品，品质靠炼丹掷骰子升上去。
        t.put("pill_huiling", Quality.MORTAL);
        t.put("pill_exp_common", Quality.MORTAL);
        t.put("zhaohua_dan", Quality.MORTAL);
        t.put("pill_exp_perfect", Quality.SUPERIOR);
        t.put("pill_zhuji", Quality.MORTAL);
        t.put("pill_xiling", Quality.MORTAL);
        t.put("pill_pojing", Quality.MORTAL);
        t.put("pill_exp_immortal", Quality.IMMORTAL);
        t.put("pill_shuangdao", Quality.MORTAL);

        // ---------------- 六阶灵植花瓣：品质 = 品阶 ----------------
        for (HerbCatalog.Herb herb : HerbCatalog.ALL) {
            t.put(herb.petalId(), Quality.forTier(herb.tier()));
        }

        // ---------------- 法宝 / 法宝剑：天生凡品，靠炼制与掉落掷 ----------------
        for (int tier = 1; tier <= GearNames.GRADE_NAMES.length; tier++) {
            t.put(GearNames.swordId(tier), Quality.MORTAL);
            for (String[] slot : GearNames.SLOTS) {
                t.put(GearNames.artifactId(tier, slot[0]), Quality.MORTAL);
            }
        }

        // ---------------- 妖丹 ----------------
        t.put(JIDAN, Quality.MORTAL);

        return Collections.unmodifiableMap(t);
    }

    /** 全部"有品质"的物品 id（保持插入顺序 = 文档顺序）。 */
    public static List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<>(INTRINSIC.keySet()));
    }

    /** 天生品质表（只读）。 */
    public static Map<String, Quality> intrinsicTable() {
        return INTRINSIC;
    }

    /** 归一化物品 id：裸名补上 {@code hongmeng:}，并去掉命名空间后统一比较。 */
    public static String normalize(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String s = id.trim().toLowerCase(Locale.ROOT);
        return s.startsWith(NS) ? s.substring(NS.length()) : s;
    }

    /** 这个东西有没有品质（表里有没有它）。 */
    public static boolean canHaveQuality(String itemId) {
        return INTRINSIC.containsKey(normalize(itemId));
    }

    /** 天生品质；表里没有的返回凡品（"没品质的东西"就当凡品，不会抛异常）。 */
    public static Quality intrinsic(String itemId) {
        Quality q = INTRINSIC.get(normalize(itemId));
        return q == null ? Quality.MORTAL : q;
    }

    /**
     * 合成出来的品质：<b>max(凡品, 天生品质)</b>。
     *
     * <p>合成永远不掷骰子——好品质只能从炼丹、妖兽、副本、任务里来。
     * 但合成也不能把物品自带的品质打低（完美修为丹合出来仍是上品）。</p>
     */
    public static Quality craftedQuality(String itemId) {
        return Quality.max(QualitySource.CRAFTED.fixedQuality(), intrinsic(itemId));
    }

    /** 妖兽掉落用的难度偏移：生命上限越高偏移越高（0..{@link QualitySource#MAX_SHIFT}）。 */
    public static int dropShift(double maxHealth) {
        int shift = 0;
        for (double threshold : DROP_SHIFT_HEALTH) {
            if (maxHealth >= threshold) {
                shift++;
            }
        }
        return QualitySource.clampShift(shift);
    }

    /** 妖兽掉落掷一次品质。 */
    public static Quality rollMobDrop(double maxHealth, java.util.Random rng) {
        return QualitySource.MOB_DROP.roll(rng, dropShift(maxHealth));
    }

    /**
     * 品质影响<b>回收价</b>：{@code 基础价 × 品质百分比 / 100}，向下取整、下限 1。
     *
     * <p><b>凡品必须原样返回</b>：坊市的防套利断言（回收价 &lt; 买入价）是按基础价算的，
     * 而商店卖出/取出永远只给凡品（{@code ×100%}），所以加了品质倍率之后
     * "买进再卖出"依然必亏。品质加价只对"掷过骰子的东西"生效。</p>
     *
     * @param baseUnitPrice 基础单件回收价（{@link com.hongmeng.xiuxian.shop.SellPrices#unitPrice}）
     */
    public static int sellPrice(int baseUnitPrice, Quality quality) {
        if (baseUnitPrice <= 0) {
            return 0;
        }
        Quality q = quality == null ? Quality.MORTAL : quality;
        int scaled = (int) Math.floor(baseUnitPrice * (q.sellPercent() / 100.0D));
        return Math.max(1, scaled);
    }

    /**
     * 自检：**任何一条坏掉就抛异常**。
     *
     * <p>纯数据、不碰注册表，可由离线验证器与模组加载期共同调用。</p>
     *
     * @throws IllegalStateException 品质物品表不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();

        if (INTRINSIC.isEmpty()) {
            problems.add("品质物品表是空的");
        }

        for (Map.Entry<String, Quality> e : INTRINSIC.entrySet()) {
            String id = e.getKey();
            if (id == null || id.isBlank()) {
                problems.add("有物品 id 是空的");
                continue;
            }
            if (id.indexOf(':') >= 0) {
                problems.add("表里的 id 必须是裸名（运行时再补命名空间）：" + id);
            }
            if (!id.equals(id.toLowerCase(Locale.ROOT))) {
                problems.add("物品 id 必须全小写：" + id);
            }
            if (e.getValue() == null) {
                problems.add("有物品没写天生品质：" + id);
            }
        }

        // ---- 九颗丹药必须全部覆盖 ----
        for (String pill : AlchemyRecipes.PILL_IDS) {
            if (!canHaveQuality(pill)) {
                problems.add("丹药没有品质条目（路线图要求丹药全覆盖）：" + pill);
            }
        }
        // 三档修为丹的倍率锚点：普通=凡品、完美=上品、仙品=仙品
        if (!intrinsic("pill_exp_common").equals(Quality.MORTAL)) {
            problems.add("普通修为丹的天生品质必须是凡品（×1）");
        }
        if (!intrinsic("pill_exp_perfect").equals(Quality.SUPERIOR)) {
            problems.add("完美修为丹的天生品质必须是上品（×3）");
        }
        if (!intrinsic("pill_exp_immortal").equals(Quality.IMMORTAL)) {
            problems.add("仙品修为丹的天生品质必须是仙品（×10）");
        }

        // ---- 六阶灵植花瓣：品质 = 品阶，且逐阶上升 ----
        int prevRank = -1;
        for (HerbCatalog.Herb herb : HerbCatalog.ALL) {
            Quality q = INTRINSIC.get(herb.petalId());
            if (q == null) {
                problems.add("灵植花瓣没有品质条目：" + herb.petalId());
                continue;
            }
            if (q.rank() != herb.tier() - 1) {
                problems.add("花瓣品质必须等于灵植品阶：" + herb.petalId()
                        + " tier=" + herb.tier() + " 品质=" + q.cn());
            }
            if (q.rank() <= prevRank) {
                problems.add("花瓣品质没有随品阶上升：" + herb.petalId());
            }
            prevRank = q.rank();
        }
        if (!intrinsic(HerbCatalog.XUELUO_ZHI.petalId()).equals(Quality.DIVINE)) {
            problems.add("六阶血珞芝花瓣必须是最高档（神品）");
        }

        // ---- 法宝 / 法宝剑全覆盖 ----
        int gearCount = 0;
        for (int tier = 1; tier <= GearNames.GRADE_NAMES.length; tier++) {
            if (!canHaveQuality(GearNames.swordId(tier))) {
                problems.add("法宝剑没有品质条目：" + GearNames.swordId(tier));
            } else {
                gearCount++;
            }
            for (String[] slot : GearNames.SLOTS) {
                if (!canHaveQuality(GearNames.artifactId(tier, slot[0]))) {
                    problems.add("法宝没有品质条目：" + GearNames.artifactId(tier, slot[0]));
                } else {
                    gearCount++;
                }
            }
        }
        if (gearCount != 60) {
            problems.add("法宝+剑应有 60 件，实际 " + gearCount + " 件");
        }
        if (!canHaveQuality(JIDAN)) {
            problems.add("妖丹没有品质条目");
        }

        // ---- 合成品质：不掷骰子，但也不许把好东西拉低 ----
        for (String id : INTRINSIC.keySet()) {
            Quality crafted = craftedQuality(id);
            if (!crafted.atLeast(intrinsic(id))) {
                problems.add("合成品质把天生品质拉低了：" + id);
            }
        }
        if (!craftedQuality("pill_exp_perfect").equals(Quality.SUPERIOR)) {
            problems.add("合成完美修为丹必须是上品（不能被打回凡品）");
        }
        if (!craftedQuality(GearNames.swordId(1)).equals(Quality.MORTAL)) {
            problems.add("合成练气法器应当是凡品");
        }

        // ---- 掉落偏移：单调不降，且永远落在合法区间 ----
        double prevHealth = -1.0D;
        int prevShift = -1;
        for (double health : new double[]{0.0D, 10.0D, 39.9D, 40.0D, 119.0D, 120.0D,
                399.0D, 400.0D, 1499.0D, 1500.0D, 100_000.0D}) {
            int shift = dropShift(health);
            if (shift < 0 || shift > QualitySource.MAX_SHIFT) {
                problems.add("掉落偏移越界：" + health + " → " + shift);
            }
            if (health > prevHealth && shift < prevShift) {
                problems.add("掉落偏移随生命上限下降了：" + health + " → " + shift);
            }
            prevHealth = health;
            prevShift = shift;
        }
        if (dropShift(100_000.0D) != DROP_SHIFT_HEALTH.length) {
            problems.add("超高血量怪物应当拿到最高偏移（" + DROP_SHIFT_HEALTH.length + "）");
        }
        for (int i = 1; i < DROP_SHIFT_HEALTH.length; i++) {
            if (DROP_SHIFT_HEALTH[i] <= DROP_SHIFT_HEALTH[i - 1]) {
                problems.add("掉落强度阈值必须严格递增（下标 " + i + "）");
            }
        }

        // ---- 回收价：凡品原价、越强越贵、且不越界 ----
        for (int base : new int[]{1, 3, 5, 25, 90, 120, 200}) {
            if (sellPrice(base, Quality.MORTAL) != base) {
                problems.add("凡品的回收价必须等于基础价：" + base
                        + " → " + sellPrice(base, Quality.MORTAL));
            }
            int prev = 0;
            for (Quality q : Quality.values()) {
                int price = sellPrice(base, q);
                if (price < prev) {
                    problems.add("回收价没有随品质上升：" + base + " 在 " + q.cn() + " 处回落");
                }
                if (price > 800) {
                    // SellPrices.MAX_PRICE 是 800；超了不是 bug，但必须在报告里说清楚
                    continue;
                }
                prev = price;
            }
        }
        if (sellPrice(0, Quality.DIVINE) != 0 || sellPrice(-5, Quality.DIVINE) != 0) {
            problems.add("收不了的东西（基础价 0）必须返回 0，不能凭空变出价格");
        }
        if (sellPrice(1, Quality.DIVINE) < 1) {
            problems.add("回收价下限必须是 1");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 品质物品表不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }
}
