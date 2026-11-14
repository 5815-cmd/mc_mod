package com.hongmeng.xiuxian.quality;

import com.hongmeng.xiuxian.alchemy.HerbCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <b>灵植品质 → 炼丹出锅品质</b>（纯数据，可离线断言）。
 *
 * <p>玩家要的是"其他的东西增加品质什么的，多一些内容"。炼丹这一环的规则是：
 * <b>投入的灵植越好，出锅的丹药品质越高</b>——高阶灵植不该只值"多一个丹方"。</p>
 *
 * <h2>规则（一条公式，不另做表）</h2>
 * <pre>
 *   bestTier = max(主料品阶, 辅料品阶)        // 辅料常常是矿石/灵叶，品阶按 0 算
 *   shift    = clamp(bestTier - 1, 0, 5)     // 一阶 → 0，六阶 → 5
 *   品质     = QualitySource.ALCHEMY.roll(rng, shift)
 * </pre>
 *
 * <p>于是：</p>
 * <ul>
 *   <li><b>一阶雪莲草</b>（凡品花瓣）→ shift 0 → 大多是凡品，偶尔良品；</li>
 *   <li><b>六阶血珞芝</b>（神品花瓣）→ shift 5 → 神品概率显著，
 *       但<b>仍然可能出锅凡品</b>（炼丹不是必胜，玩家才有反复开炉的理由）；</li>
 *   <li>品阶每升一阶，期望品质严格上升（{@link #validate()} 会断言）。</li>
 * </ul>
 *
 * <h2>雷劫与品质是两件事</h2>
 * <p>{@code AlchemyTribulation} 管的是<b>这一炉出几颗</b>（只有最高品阶的丹方挨雷），
 * 跟品质不冲突：一颗丹药可以既是"雷劫劈出的五颗"又是神品。
 * 所以本类只管<b>掷品质</b>，不管数量、不管闪电。</p>
 *
 * <p><b>接入点</b>：{@code alchemy/AlchemyFurnaceBlockEntity.applyOutcomePills(...)}
 * 里 {@code new ItemStack(result, count)} 之后加一行
 * {@code ItemQuality.set(out, AlchemyQuality.roll(mainTier, auxTier, rng))} 即可。
 * 详见 {@code docs/quality-and-biomes-spec.md} 的"需要别人配合的两处"。</p>
 */
public final class AlchemyQuality {

    private AlchemyQuality() {
    }

    /**
     * 主料 / 辅料的灵植品阶（非灵植按 0 算）。
     *
     * @param itemId 物品注册名（可带可不带命名空间）
     */
    public static int herbTierOf(String itemId) {
        String id = QualityTable.normalize(itemId);
        HerbCatalog.Herb herb = HerbCatalog.byId(id);
        if (herb == null) {
            // 种子 / 花瓣各算同一株灵植：<herb>_seed 与 <herb>_petal 都要能查到品阶，
            // 否则"拿种子当辅料"会被算成 0 阶，高阶丹的偏移凭空掉一截。
            if (id.endsWith("_seed")) {
                herb = HerbCatalog.byId(id.substring(0, id.length() - "_seed".length()));
            } else if (id.endsWith("_petal")) {
                herb = HerbCatalog.byId(id.substring(0, id.length() - "_petal".length()));
            }
        }
        return herb == null ? 0 : herb.tier();
    }

    /** 最优材料的品阶（0 = 两边都不是灵植）。 */
    public static int bestTier(int mainTier, int auxTier) {
        return Math.max(Math.max(mainTier, 0), Math.max(auxTier, 0));
    }

    /** 难度偏移：一阶 → 0，二阶 → 1，…… 六阶 → 5（正好用满六档灵植）。 */
    public static int shiftFor(int mainTier, int auxTier) {
        return QualitySource.clampShift(bestTier(mainTier, auxTier) - 1);
    }

    /** 掷一炉的品质（rng 为 null 时返回凡品，不抛异常）。 */
    public static Quality roll(int mainTier, int auxTier, Random rng) {
        return QualitySource.ALCHEMY.roll(rng, shiftFor(mainTier, auxTier));
    }

    /** 按物品 id 掷一炉的品质（运行时由丹炉调用，纯字符串入参）。 */
    public static Quality rollByItems(String mainId, String auxId, Random rng) {
        return roll(herbTierOf(mainId), herbTierOf(auxId), rng);
    }

    /** 界面 / 提示用的一句话，例如 "血珞芝（六阶）入炉，品质偏移 +4"。 */
    public static String describe(int mainTier, int auxTier) {
        int best = bestTier(mainTier, auxTier);
        if (best <= 0) {
            return "\u00a77主辅料都不是灵植，品质按凡品计。";
        }
        return "\u00a77最优材料 \u00a7f" + best + " 阶\u00a77，品质偏移 \u00a7a+"
                + shiftFor(mainTier, auxTier) + "\u00a77（"
                + QualitySource.ALCHEMY.cn() + "）";
    }

    /** 指定灵植品阶下的品质概率（‰），供界面与文档展示。 */
    public static int chancePerMille(int herbTier, Quality quality) {
        return QualitySource.ALCHEMY.sampledPerMille(quality, shiftFor(herbTier, 0), 20261010L, 100_000);
    }

    /**
     * 自检：**任何一条坏掉就抛异常**。纯数据，离线验证器可直接调用。
     *
     * @throws IllegalStateException 炼丹品质规则不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();

        // 六阶灵植必须都能查到品阶（否则"越好的灵植出越好的丹"就是空话）
        for (HerbCatalog.Herb herb : HerbCatalog.ALL) {
            int tier = herbTierOf(herb.petalId());
            if (tier != herb.tier()) {
                problems.add("灵植品阶查不到：" + herb.petalId()
                        + " 表里是 " + herb.tier() + "，查出来是 " + tier);
            }
            if (herbTierOf(herb.seedId()) != herb.tier()) {
                problems.add("种子也应当算灵植（否则拿种子当辅料会算错）：" + herb.seedId());
            }
        }
        // 非灵植一律 0 阶
        for (String id : new String[]{"lingye", "spirit_crystal", "kongming_essence",
                "minecraft:blaze_powder", "daoyin", "jidan", ""}) {
            if (herbTierOf(id) != 0) {
                problems.add("不是灵植的东西不应有品阶：" + id);
            }
        }

        // 偏移随品阶单调不降，且夹在 0..MAX_SHIFT
        int prev = -1;
        for (int tier = 0; tier <= 8; tier++) {
            int shift = shiftFor(tier, 0);
            if (shift < 0 || shift > QualitySource.MAX_SHIFT) {
                problems.add("炼丹品质偏移越界：" + tier + " 阶 → " + shift);
            }
            if (shift < prev) {
                problems.add("炼丹品质偏移随品阶下降：" + tier + " 阶 → " + shift);
            }
            prev = shift;
        }
        if (shiftFor(1, 0) != 0) {
            problems.add("一阶灵植的偏移必须是 0");
        }
        if (shiftFor(6, 0) != QualitySource.MAX_SHIFT) {
            problems.add("六阶灵植必须拿到最高偏移");
        }

        // 期望品质随品阶严格上升（解析解，零抽样噪声）
        double prevRank = -1.0D;
        for (int tier = 1; tier <= 6; tier++) {
            double e = QualitySource.ALCHEMY.expectedRankExact(shiftFor(tier, 0));
            if (e <= prevRank) {
                problems.add("炼丹期望品质没有随品阶上升：" + tier + " 阶期望 " + e);
            }
            prevRank = e;
        }
        // 六阶也必须"可能出锅凡品"（炼丹不是必胜），否则玩家没有重开炉的理由
        if (chancePerMille(6, Quality.MORTAL) <= 0) {
            problems.add("六阶灵植炼出凡品的概率变成 0 了（应当只是很小）");
        }
        // 一阶灵植炼出神品的概率必须极低（不能一步登天）
        if (chancePerMille(1, Quality.DIVINE) > 5) {
            problems.add("一阶灵植炼出神品的概率过高："
                    + chancePerMille(1, Quality.DIVINE) + "‰");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 炼丹品质规则不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }
}
