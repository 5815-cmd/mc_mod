package com.hongmeng.xiuxian.shop;

import com.hongmeng.xiuxian.technique.CareerTechnique;

import java.util.List;

/**
 * 商店商品目录。
 *
 * <p>货币是**灵叶**（{@code hongmeng:lingye}）。灵叶现在有两条形态：
 * <b>存在坊市的余额</b>（杀怪、任务、副本奖励直接进余额）与
 * <b>背包里的实物</b>（采灵草掉的是实物，摸到坊市点"存入灵叶"就变成余额）。
 * 买东西时先扣余额，再扣背包实物，所以玩家不必特意去存。</p>
 *
 * <h2>价格是怎么定出来的</h2>
 * <p>旧价格表是"一次采药 1~3 片"的节奏定的（回灵丹 20、普通修为丹 30），
 * 但杀怪几乎不掉灵叶（8% × 1~2 片），玩家抱怨"商店太贵、灵叶难赚"。
 * 现在杀怪收益按品阶走（见 {@link LeafRewards}），
 * 期望收益：原版敌对生物 0.5 片/只、普通妖兽 1.05、精英 2.8、队长 7、头目 17.5、首领 43.75。
 * 价格整体下调约四成，让练气期玩家"打一会儿怪就能买得起第一颗丹"。</p>
 *
 * <table border="1">
 *   <caption>够买什么（按期望收益折算：均值 × 0.35 概率）</caption>
 *   <tr><th>商品</th><th>价格</th><th>要杀多少</th></tr>
 *   <tr><td>回灵丹</td><td>10</td><td>约 10 只普通妖兽 / 19 只原版僵尸 / 5 次采灵草</td></tr>
 *   <tr><td>普通修为丹</td><td>20</td><td>约 19 只普通妖兽 / 38 只原版僵尸</td></tr>
 *   <tr><td>造化丹</td><td>40</td><td>约 38 只普通妖兽 / 14 只精英</td></tr>
 *   <tr><td>筑基丹</td><td>100</td><td>约 14 只队长 / 95 只普通妖兽</td></tr>
 *   <tr><td>破境丹</td><td>250</td><td>约 36 只队长 / 14 只头目</td></tr>
 *   <tr><td>仙品修为丹</td><td>300</td><td>约 17 只头目 / 7 只首领</td></tr>
 *   <tr><td>双道宫丹</td><td>2000</td><td>约 46 只首领 / 114 只头目（长期目标）</td></tr>
 * </table>
 *
 * <p><b>这里只写物品注册名，不引用 {@code ModItems}</b>——见 {@link ShopEntry} 的说明。</p>
 */
public final class ShopCatalog {

    /** 全部商品，顺序即界面显示顺序（大致按价格从低到高，新手一眼看到买得起的）。 */
    public static final List<ShopEntry> ENTRIES = List.of(

            new ShopEntry("pill_huiling", "回灵丹",
                    "回复灵力，日常消耗品（一份 2 颗）",
                    "hongmeng:pill_huiling", 2, 10, 1),

            new ShopEntry("pill_exp_common", "普通修为丹",
                    "给当前小境界需求的 25%",
                    "hongmeng:pill_exp_common", 1, 20, 1),

            new ShopEntry("zhaohua_dan", "造化丹",
                    "新手期的开脉丹药：未觉醒者直接觉醒",
                    "hongmeng:zhaohua_dan", 1, 40, 1),

            new ShopEntry("pill_exp_perfect", "完美修为丹",
                    "给当前小境界需求的 75%",
                    "hongmeng:pill_exp_perfect", 1, 80, 2),

            new ShopEntry("pill_zhuji", "筑基丹",
                    "突破筑基时顶一手",
                    "hongmeng:pill_zhuji", 1, 100, 1),

            new ShopEntry("pill_xiling", "洗灵丹",
                    "重测灵根（至少地品，且吃十连保底）",
                    "hongmeng:pill_xiling", 1, 150, 1),

            new ShopEntry("pill_pojing", "破境丹",
                    "下一次突破必定成功",
                    "hongmeng:pill_pojing", 1, 250, 3),

            new ShopEntry("pill_exp_immortal", "仙品修为丹",
                    "给当前小境界需求的 250%",
                    "hongmeng:pill_exp_immortal", 1, 300, 3),

            new ShopEntry("pill_shuangdao", "双道宫丹",
                    "解锁第二座道宫",
                    "hongmeng:pill_shuangdao", 1, 2000, 6),
            // ---------------- 灵植 / 炼丹（见 docs/herb-alchemy-spec.md） ----------------
            // 商店只是"第一次不用跑图"的便利。野生灵植、花瓣->种子、
            // 小麦种子+灵叶->2 种子 三条路都能让玩家自己开工，
            // 所以这里**不是唯一来源**，删掉也不影响系统可用。
            new ShopEntry("seed_xuelian_grass", "雪莲草种子 x2",
                    "一阶灵植：回灵 / 疗伤丹药的原料",
                    "hongmeng:xuelian_grass_seed", 2, 10, 1),
            new ShopEntry("seed_huoyan_hua", "火焰花种子 x2",
                    "二阶灵植：攻击 / 火系丹药的原料",
                    "hongmeng:huoyan_hua_seed", 2, 20, 1),
            new ShopEntry("seed_hanbing_teng", "寒冰藤种子 x2",
                    "三阶灵植：控制 / 冰系丹药的原料",
                    "hongmeng:hanbing_teng_seed", 2, 40, 2),
            new ShopEntry("seed_leiyin_cao", "雷音草种子 x2",
                    "四阶灵植：雷系 / 突破丹药的原料",
                    "hongmeng:leiyin_cao_seed", 2, 80, 3),
            new ShopEntry("seed_kongming_lan", "空冥兰种子 x2",
                    "五阶灵植：高阶 / 空间丹药的原料",
                    "hongmeng:kongming_lan_seed", 2, 160, 5),
            new ShopEntry("seed_xueluo_zhi", "血珞芝种子 x2",
                    "六阶灵植：顶级 / 血道丹药的原料",
                    "hongmeng:xueluo_zhi_seed", 2, 320, 7),
            new ShopEntry("lingtian_soil", "灵田土 x3",
                    "灵植种在上面生长概率 x3（普通耕地只有 1 倍）",
                    "hongmeng:lingtian_soil", 3, 15, 1),

            // 取出实物灵叶：合成链里有十几个配方要拿 hongmeng:lingye 当材料
            // （筑基丹、血珞精……），所以余额必须能按 1:1 取回实物，否则玩家
            // 一旦把灵叶全存进去就卡死在合成上。价格 = 数量，绝不抽水。
            new ShopEntry("lingye", "灵叶 ×10（取出）",
                    "把坊市余额取成实物灵叶 ×10，供合成使用",
                    "hongmeng:lingye", 10, 10, 1),

            // ---------------- 纪元令兑换 ----------------
            // 纪元令（hongmeng:jidan）以前只能攒着看，现在能在坊市换炼制法宝的材料。
            // 一件法宝升一阶要 8 个材料（3×3 配方空中心），所以每枚令牌给的量
            // 至少够一次升阶；灵铁最便宜给得最多，血珞最稀有给得最少。
            // 纪元令本身来自副本通关 / 纪元秘境 / 击杀 BOSS，所以这里不再加境界门槛，
            // 令牌就是门槛。
            new ShopEntry("token_spirit_iron", "灵铁锭 ×16",
                    "用纪元令换：一次换够两次法宝升阶的灵铁",
                    "hongmeng:spirit_iron_ingot", 16, 1, 1, "hongmeng:jidan"),

            new ShopEntry("token_spirit_crystal", "灵晶 ×12",
                    "用纪元令换：中阶法宝的主料",
                    "hongmeng:spirit_crystal", 12, 1, 1, "hongmeng:jidan"),

            new ShopEntry("token_kongming", "空冥精 ×8",
                    "用纪元令换：一件高阶法宝的完整升阶材料",
                    "hongmeng:kongming_essence", 8, 1, 1, "hongmeng:jidan"),

            new ShopEntry("token_xueluo", "血珞精 ×8",
                    "用纪元令换：最稀有的一档炼器材料",
                    "hongmeng:xueluo_essence", 8, 1, 1, "hongmeng:jidan")
    );

    /**
     * 功法秘籍自动进商店。
     *
     * <p>秘籍有 22 本，手写 22 条 {@link ShopEntry} 又长又容易漏，
     * 所以这里**从功法目录生成**。价格按境界门槛递增——越高的功法越贵，
     * 给玩家一个"攒灵叶换高阶功法"的长期目标。</p>
     *
     * <p>价格曲线：第 1 境 50 片，之后每境 ×1.5（旧版是 80 × 1.6，随整体降价一起下调）。
     * 于是练气 50 / 筑基 70 / 金丹 110 / 元婴 150 / 化神 250 / 道印 350 /
     * 飞升 550 / 仙人 850 / 帝境 1200 / 鸿蒙 1900——鸿蒙级约等于 43 只首领，
     * 仍然是明确的长期目标，但不再遥不可及。</p>
     *
     * <p>用静态块而不是直接写进上面的 {@code List.of}，是因为
     * {@code List.of} 要求元素在编译期就是常量列表，而这里需要循环生成。</p>
     */
    private static final List<ShopEntry> MANUALS = buildManuals();

    private static List<ShopEntry> buildManuals() {
        java.util.List<ShopEntry> out = new java.util.ArrayList<>();
        for (com.hongmeng.xiuxian.technique.Technique t
                : com.hongmeng.xiuxian.technique.Technique.values()) {
            // 18 门职业主动不生成秘籍、不进坊市（见 CareerTechnique#isUnlockOnly）：
            // 只能靠天赋树解锁，否则买书就能绕过天赋树。2 门复用条目照旧。
            if (CareerTechnique.isUnlockOnly(t)) {
                continue;
            }
            String itemId = "hongmeng:manual_"
                    + t.name().toLowerCase(java.util.Locale.ROOT);
            // 价格曲线：第 1 境 50 片，之后每境约 ×1.5
            int price = (int) Math.round(50.0D * Math.pow(1.5D, t.minTier() - 1));
            // 价格取整到"好看"的数
            price = roundPrice(price);
            out.add(new ShopEntry(
                    "manual_" + t.name().toLowerCase(java.util.Locale.ROOT),
                    t.cn() + "秘籍",
                    t.trait() + "（" + t.element().cn() + "/" + t.kind().glyph() + "）",
                    itemId, 1, price, t.minTier()));
        }
        return java.util.Collections.unmodifiableList(out);
    }

    /** 把价格修整成 10 的整数倍，界面里好看。 */
    private static int roundPrice(int price) {
        if (price < 100) {
            return Math.max(10, (price / 10) * 10);
        }
        if (price < 1000) {
            return (price / 50) * 50;
        }
        return (price / 100) * 100;
    }

    /** 全部商品 = 基础商品 + 功法秘籍。 */
    public static List<ShopEntry> all() {
        java.util.List<ShopEntry> out = new java.util.ArrayList<>(ENTRIES);
        out.addAll(MANUALS);
        return java.util.Collections.unmodifiableList(out);
    }

    /** 秘籍条目（供验证器与界面分组用）。 */
    public static List<ShopEntry> manuals() {
        return MANUALS;
    }

    /** 用纪元令结算的条目（界面可分组显示、验证器可断言）。 */
    public static List<ShopEntry> tokenEntries() {
        java.util.List<ShopEntry> out = new java.util.ArrayList<>();
        for (ShopEntry e : all()) {
            if (!e.usesLeaves()) {
                out.add(e);
            }
        }
        return java.util.Collections.unmodifiableList(out);
    }

    private ShopCatalog() {
    }

    public static ShopEntry byIndex(int index) {
        List<ShopEntry> list = all();
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    public static ShopEntry byId(String id) {
        for (ShopEntry e : all()) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }

    /** 含秘籍在内的总条目数。 */
    public static int size() {
        return all().size();
    }

    /** 只算基础商品（不含秘籍）。 */
    public static int baseSize() {
        return ENTRIES.size();
    }
}
