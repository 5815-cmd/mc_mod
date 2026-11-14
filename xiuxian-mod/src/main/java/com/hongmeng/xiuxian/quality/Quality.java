package com.hongmeng.xiuxian.quality;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 物品<b>品质</b>：六档，从最弱到最强。
 *
 * <p>玩家原话："其他品质的丹药没有雷劈……然后其他的东西增加品质什么的东西多一些内容"、
 * "丹药是品阶比如一种丹药有普通和完美这样"。也就是说：<b>同一种东西应该有多个品质档位</b>，
 * 而且要铺开到丹药以外（法宝、法宝剑、灵植花瓣、妖丹）。</p>
 *
 * <h2>口径：和既有的两套中文阶梯对齐</h2>
 * <ul>
 *   <li>灵根 7 品（{@code spirit.Rarity}：伪灵根 / 凡品灵根 / 地灵根 / 天灵根 /
 *       先天灵根 / 混沌灵根 / 鸿蒙灵根）；</li>
 *   <li>道宫 6 品（{@code domain.DomainQuality}：凡品 / 地品 / 天品 / 仙品 / 混沌 / 鸿蒙）。</li>
 * </ul>
 * <p>物品品质取<b>六档</b>，与"六阶灵植"一对一：
 * <b>凡品 / 良品 / 上品 / 极品 / 仙品 / 神品</b>。
 * 「凡品」与「仙品」两个词与上面两套完全一致，是它们之间可以互认的锚点；
 * 而 6 档正好等于灵植的 6 个品阶，于是"一阶雪莲草花瓣 = 凡品、六阶血珞芝花瓣 = 神品"
 * 是一条**不需要额外数据表**的对应关系（见 {@link #forTier(int)}）。</p>
 *
 * <h2>三张数值表</h2>
 * <table border="1">
 *   <caption>品质数值</caption>
 *   <tr><th>品质</th><th>丹药/效果倍率</th><th>法宝倍率</th><th>回收价</th><th>权重‰</th></tr>
 *   <tr><td>凡品</td><td>×1</td><td>×1.00</td><td>100%</td><td>600</td></tr>
 *   <tr><td>良品</td><td>×1.5</td><td>×1.06</td><td>130%</td><td>220</td></tr>
 *   <tr><td>上品</td><td>×3</td><td>×1.15</td><td>170%</td><td>100</td></tr>
 *   <tr><td>极品</td><td>×5</td><td>×1.28</td><td>230%</td><td>50</td></tr>
 *   <tr><td>仙品</td><td>×10</td><td>×1.45</td><td>320%</td><td>25</td></tr>
 *   <tr><td>神品</td><td>×20</td><td>×1.70</td><td>450%</td><td>5</td></tr>
 * </table>
 *
 * <p><b>为什么效果倍率必须是 1 / 3 / 10</b>：既有的三档修为丹
 * （普通 / 完美 / 仙品）早就写死了"25% / 75% / 250% 当前小境界需求"，
 * 也就是倍率 1 / 3 / 10。这里把三档<b>嵌进六档阶梯</b>
 * （普通→凡品、完美→上品、仙品→仙品），倍率一个数字都没变，
 * 老存档、老 tooltip、老数值全部原样。</p>
 *
 * <p>权重之和必须正好 1000‰：{@link #validate()} 会断言，
 * 表坏了的表现是"模组加载失败 + 一条说得清原因的异常"，而不是"神品悄悄变成 30%"。</p>
 */
public enum Quality {

    /** 凡品：合成的默认品质，也是所有"没掷过骰子"的东西。 */
    MORTAL("凡品", "mortal", '7', 0xFFAAAAAA, 1.00D, 1.00D, 100, 600),

    /** 良品：比凡品好一档，掉落里最常见的好东西。 */
    FINE("良品", "fine", 'f', 0xFFF0E0A0, 1.50D, 1.06D, 130, 220),

    /** 上品：完美修为丹所在档（×3）。 */
    SUPERIOR("上品", "superior", 'a', 0xFF7FE07F, 3.00D, 1.15D, 170, 100),

    /** 极品：难得，但还看得见。 */
    EXQUISITE("极品", "exquisite", 'd', 0xFFE080FF, 5.00D, 1.28D, 230, 50),

    /** 仙品：仙品修为丹所在档（×10），也是道宫第四档的名字。 */
    IMMORTAL("仙品", "immortal", '5', 0xFFFFD24A, 10.00D, 1.45D, 320, 25),

    /** 神品：最高档，只有最高难度/最深处才掷得出来。 */
    DIVINE("神品", "divine", '6', 0xFFFF6FE0, 20.00D, 1.70D, 450, 5);

    /** 档位数量（六档 = 六阶灵植）。 */
    public static final int COUNT = 6;

    private final String cn;
    private final String key;
    private final char colorCode;
    private final int rgb;
    private final double effectMultiplier;
    private final double gearMultiplier;
    private final int sellPercent;
    private final int weightPerMille;

    Quality(String cn, String key, char colorCode, int rgb,
            double effectMultiplier, double gearMultiplier, int sellPercent, int weightPerMille) {
        this.cn = cn;
        this.key = key;
        this.colorCode = colorCode;
        this.rgb = rgb;
        this.effectMultiplier = effectMultiplier;
        this.gearMultiplier = gearMultiplier;
        this.sellPercent = sellPercent;
        this.weightPerMille = weightPerMille;
    }

    public String cn() {
        return cn;
    }

    /** 稳定的小写英文键（写进 NBT，**不要**用中文当键）。 */
    public String key() {
        return key;
    }

    public char colorCode() {
        return colorCode;
    }

    public int rgb() {
        return rgb;
    }

    /** 丹药 / 通用效果倍率（1.0 = 原样）。 */
    public double effectMultiplier() {
        return effectMultiplier;
    }

    /**
     * 法宝 / 法宝剑的属性倍率。
     *
     * <p>刻意比效果倍率保守得多（神品也只 +70%）：装备是<b>五件套叠加</b>的，
     * 丹药是一次性的。按 ×20 放大的话，五件神品法宝叠起来就是 100 倍护甲，
     * 减伤直接顶到无敌，{@code ArmorMitigation} 的软上限都救不回来。</p>
     */
    public double gearMultiplier() {
        return gearMultiplier;
    }

    /** 回收价百分比（100 = 原价）。凡品必须是 100，否则会动摇坊市的防套利断言。 */
    public int sellPercent() {
        return sellPercent;
    }

    /** 随机权重（千分比，六档之和必须是 1000）。 */
    public int weightPerMille() {
        return weightPerMille;
    }

    /** 序号：0 = 凡品（最弱）… 5 = 神品（最强）。 */
    public int rank() {
        return ordinal();
    }

    /** 是不是最高档。 */
    public boolean isTop() {
        return ordinal() == COUNT - 1;
    }

    /** 高一档；已经是神品就还是神品（不返回 null，调用方不必判空）。 */
    public Quality upgrade() {
        return isTop() ? this : values()[ordinal() + 1];
    }

    /** 是否至少达到 other 档。 */
    public boolean atLeast(Quality other) {
        return other != null && ordinal() >= other.ordinal();
    }

    /** 取两档里更高的那个（null 视作凡品）。 */
    public static Quality max(Quality a, Quality b) {
        Quality left = a == null ? MORTAL : a;
        Quality right = b == null ? MORTAL : b;
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    public String colored() {
        return "\u00a7" + colorCode + cn;
    }

    /** 效果倍率的显示文本，例如 "×3"、"×1.5"。 */
    public String multiplierText() {
        return "\u00d7" + trim(effectMultiplier);
    }

    /** 法宝倍率的显示文本，例如 "+15%"。 */
    public String gearText() {
        return "+" + Math.round((gearMultiplier - 1.0D) * 100.0D) + "%";
    }

    /** 回收价文本，例如 "170%"。 */
    public String sellText() {
        return sellPercent + "%";
    }

    private static String trim(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    /**
     * 灵植品阶（1..6）→ 品质（凡品..神品）。
     *
     * <p>越界一律夹住，绝不抛异常：这是被 tooltip 与掉落表随时调用的映射。</p>
     */
    public static Quality forTier(int tier) {
        int idx = tier - 1;
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= COUNT) {
            idx = COUNT - 1;
        }
        return values()[idx];
    }

    /** 反查（英文键优先，其次中文名、枚举名）；认不出来回落到凡品。 */
    public static Quality byKey(String id) {
        if (id == null || id.isBlank()) {
            return MORTAL;
        }
        String s = id.trim();
        for (Quality q : values()) {
            if (q.key.equalsIgnoreCase(s) || q.name().equalsIgnoreCase(s)
                    || q.cn.equals(s)) {
                return q;
            }
        }
        return MORTAL;
    }

    /** 按序号取（越界夹住）。 */
    public static Quality byRank(int rank) {
        if (rank <= 0) {
            return MORTAL;
        }
        return rank >= COUNT ? values()[COUNT - 1] : values()[rank];
    }

    /** 六档中文名，用 " / " 连起来（文档与自检输出用）。 */
    public static String ladderText() {
        StringBuilder sb = new StringBuilder();
        for (Quality q : values()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(q.cn);
        }
        return sb.toString();
    }

    /** 全部档位（顺序 = 由弱到强）。 */
    public static List<Quality> ladder() {
        return List.of(values());
    }

    /**
     * 自检：**任何一条坏掉就抛异常**。
     *
     * <p>纯数据、不碰注册表，所以可以被离线验证器直接调用。</p>
     *
     * @throws IllegalStateException 品质表不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();
        if (values().length != COUNT) {
            problems.add("品质必须是 " + COUNT + " 档，实际 " + values().length);
        }
        int sum = 0;
        for (Quality q : values()) {
            sum += q.weightPerMille;
            if (q.cn == null || q.cn.isBlank()) {
                problems.add("有档位没写中文名：" + q.name());
            }
            if (q.key == null || !q.key.equals(q.key.toLowerCase(Locale.ROOT))
                    || q.key.contains(" ") || q.key.contains(":")) {
                problems.add("品质键必须是蛇形小写英文：" + q.key);
            }
            if (q.effectMultiplier < 1.0D) {
                problems.add("效果倍率不能小于 1（品质只该变强）：" + q.cn);
            }
            if (q.gearMultiplier < 1.0D) {
                problems.add("法宝倍率不能小于 1：" + q.cn);
            }
            if (q.sellPercent < 100) {
                problems.add("回收价百分比不能低于 100：" + q.cn);
            }
            if (q.weightPerMille <= 0) {
                problems.add("每一档都必须抽得到（权重大于 0）：" + q.cn);
            }
        }
        if (sum != 1000) {
            problems.add("六档权重之和必须是 1000‰，实际 " + sum + "‰");
        }
        // 单调递增：倍率 / 法宝倍率 / 回收价都必须严格随档位上升
        for (int i = 1; i < values().length; i++) {
            Quality lo = values()[i - 1];
            Quality hi = values()[i];
            if (hi.effectMultiplier <= lo.effectMultiplier) {
                problems.add("效果倍率没有严格递增：" + lo.cn + " → " + hi.cn);
            }
            if (hi.gearMultiplier <= lo.gearMultiplier) {
                problems.add("法宝倍率没有严格递增：" + lo.cn + " → " + hi.cn);
            }
            if (hi.sellPercent <= lo.sellPercent) {
                problems.add("回收价没有严格递增：" + lo.cn + " → " + hi.cn);
            }
            if (hi.weightPerMille >= lo.weightPerMille) {
                problems.add("越强的档位必须越罕见（权重应递减）：" + lo.cn + " → " + hi.cn);
            }
        }
        // 三档修为丹的锚点：1 / 3 / 10 一个数字都不许动
        if (MORTAL.effectMultiplier != 1.0D) {
            problems.add("凡品倍率必须是 ×1（普通修为丹的锚点）");
        }
        if (SUPERIOR.effectMultiplier != 3.0D) {
            problems.add("上品倍率必须是 ×3（完美修为丹的锚点）");
        }
        if (IMMORTAL.effectMultiplier != 10.0D) {
            problems.add("仙品倍率必须是 ×10（仙品修为丹的锚点）");
        }
        // 灵植六阶 ↔ 六档品质
        if (!forTier(1).equals(MORTAL) || !forTier(6).equals(DIVINE)) {
            problems.add("灵植品阶映射错了：一阶应为凡品、六阶应为神品");
        }
        if (!byKey("superior").equals(SUPERIOR) || !byKey("上品").equals(SUPERIOR)) {
            problems.add("byKey 反查坏了（英文键 / 中文名都要认）");
        }
        if (!byKey("no_such_quality").equals(MORTAL)) {
            problems.add("byKey 未知输入应回落凡品");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 品质表不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }
}
