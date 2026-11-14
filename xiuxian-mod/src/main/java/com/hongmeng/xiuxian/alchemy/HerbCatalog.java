package com.hongmeng.xiuxian.alchemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 六阶灵植的**纯数据表**（品阶 / 用途 / 生长概率）。
 *
 * <p>为什么要单独开一个类，而不是把数值直接写在 {@code ModBlocks} 里？
 * 因为离线验证器<b>不能碰方块类</b>：{@code SpiritHerbBlock} 继承 {@code CropBlock}，
 * 一路会把 {@code Block} / {@code BuiltInRegistries} 拖进静态初始化，
 * 在没有游戏的环境里直接抛 {@code Not bootstrapped}（实测：
 * {@code tools/HerbOfflineProbe.java} 里 {@code SpiritHerbBlock} 加载失败、
 * {@code HerbCatalog} 加载成功）。</p>
 *
 * <p>所以口径是：</p>
 * <ul>
 *   <li><b>数值与文案放这里</b>（纯 Java，零 Minecraft 类型，离线可断言）；</li>
 *   <li><b>方块实例在 {@code ModBlocks} 里注册</b>，把 {@link Herb} 记录与两个
 *       物品 Supplier 交给 {@code SpiritHerbBlock}——数值只有这一份，
 *       不存在"表里写 0.3、方块里写 0.25"的漂移；</li>
 *   <li>掉落表里"成熟掉几片花瓣"由 {@link Herb#petalMax()} 给出，
 *       与 {@code tools/gen_herb_alchemy_data.py} 生成的 JSON 一致
 *       （{@code tools/check_herb_alchemy.py} 会对撞两者）。</li>
 * </ul>
 *
 * @param id            注册名，例：{@code xuelian_grass}
 * @param cn            中文名
 * @param tier          品阶 1..6（六株各占一阶，不重复）
 * @param use           用途方向（写进 tooltip 与文档）
 * @param growthChance  种在**耕地**上时每次随机刻的进阶概率；灵田土上 ×3
 */
public final class HerbCatalog {

    /** 一株灵植的静态数据。 */
    public record Herb(String id, String cn, int tier, String use, float growthChance) {

        /** 长熟（age=7）时一次掉多少花瓣的上限：1 + ⌈品阶/2⌉ → 一阶 2 片、六阶 4 片。 */
        public int petalMax() {
            return 1 + (tier + 1) / 2;
        }

        public String seedId() {
            return id + "_seed";
        }

        public String petalId() {
            return id + "_petal";
        }
    }

    /** 一阶 · 雪莲草：回灵 / 疗伤。最容易种，也最常用。 */
    public static final Herb XUELIAN_GRASS =
            new Herb("xuelian_grass", "雪莲草", 1, "回灵 / 疗伤", 0.30F);

    /** 二阶 · 火焰花：攻击 / 火系丹药的主料。 */
    public static final Herb HUOYAN_HUA =
            new Herb("huoyan_hua", "火焰花", 2, "攻击 / 火系", 0.27F);

    /** 三阶 · 寒冰藤：控制 / 冰系。 */
    public static final Herb HANBING_TENG =
            new Herb("hanbing_teng", "寒冰藤", 3, "控制 / 冰系", 0.24F);

    /** 四阶 · 雷音草：雷系 / 突破丹。 */
    public static final Herb LEIYIN_CAO =
            new Herb("leiyin_cao", "雷音草", 4, "雷系 / 突破", 0.21F);

    /** 五阶 · 空冥兰：高阶 / 空间。 */
    public static final Herb KONGMING_LAN =
            new Herb("kongming_lan", "空冥兰", 5, "高阶 / 空间", 0.18F);

    /** 六阶 · 血珞芝：顶级 / 血道。最难种，也最贵。 */
    public static final Herb XUELUO_ZHI =
            new Herb("xueluo_zhi", "血珞芝", 6, "顶级 / 血道", 0.15F);

    /** 全部六株，顺序 = 品阶顺序 = 文档顺序。 */
    public static final List<Herb> ALL = List.of(
            XUELIAN_GRASS, HUOYAN_HUA, HANBING_TENG, LEIYIN_CAO, KONGMING_LAN, XUELUO_ZHI);

    private HerbCatalog() {
    }

    public static Herb byId(String id) {
        for (Herb herb : ALL) {
            if (herb.id().equals(id)) {
                return herb;
            }
        }
        return null;
    }

    public static int maxTier() {
        int max = 0;
        for (Herb herb : ALL) {
            max = Math.max(max, herb.tier());
        }
        return max;
    }

    /**
     * 自检：品阶必须正好是 1..6 各一个、生长概率必须在 (0,1]、名字必须是蛇形小写。
     *
     * <p>与 {@link AlchemyRecipes#validate()} 一样在模组构造期调用——
     * 表写错的表现是"模组加载失败 + 说明原因的异常"，而不是"某株灵植长不出来"。</p>
     *
     * @throws IllegalStateException 表不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();
        if (ALL.size() != 6) {
            problems.add("灵植表应有 6 株，实际 " + ALL.size());
        }
        boolean[] tierSeen = new boolean[7];
        for (Herb herb : ALL) {
            if (herb.tier() < 1 || herb.tier() > 6) {
                problems.add("品阶不在 1..6：" + herb.id() + " = " + herb.tier());
            } else if (tierSeen[herb.tier()]) {
                problems.add("品阶重复：" + herb.tier() + "（" + herb.id() + "）");
            } else {
                tierSeen[herb.tier()] = true;
            }
            if (!herb.id().equals(herb.id().toLowerCase(Locale.ROOT))
                    || herb.id().contains(" ")
                    || herb.id().contains(":")) {
                problems.add("注册名不是蛇形小写：" + herb.id());
            }
            if (herb.cn() == null || herb.cn().isBlank()) {
                problems.add("缺中文名：" + herb.id());
            }
            if (herb.use() == null || herb.use().isBlank()) {
                problems.add("缺用途说明：" + herb.id());
            }
            if (!(herb.growthChance() > 0.0F) || herb.growthChance() > 1.0F) {
                problems.add("生长概率不在 (0,1]：" + herb.id() + " = " + herb.growthChance());
            }
        }
        for (int tier = 1; tier <= 6; tier++) {
            if (!tierSeen[tier]) {
                problems.add("没有灵植占第 " + tier + " 阶");
            }
        }
        if (byId("xuelian_grass") == null) {
            problems.add("找不到一阶灵植 xuelian_grass（丹方表的第一条主料就是它）");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 灵植表不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }
}
