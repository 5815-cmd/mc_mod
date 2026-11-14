package com.hongmeng.xiuxian.worldgen;

import com.hongmeng.xiuxian.alchemy.HerbCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 一个新群系的策划数据（纯数据记录，可离线断言）。
 *
 * <p>字段含义与整体设计见 {@link BiomePlan}。</p>
 *
 * @param id            注册名（蛇形小写）
 * @param cn            中文名
 * @param en            英文名
 * @param dangerTier    危险度 1..3（越高怪越强、高阶灵植占比越大）
 * @param theme         一句话主题（写进文档与日志）
 * @param donors        可以接管的原版群系（按优先级；运行时取第一个"点位够多"的）
 * @param herbCounts    六株灵植的 count（顺序 = {@link HerbCatalog#ALL}）
 * @param spawnStrength 强怪补充档数（0 = 只吃原版刷怪，1~3 = 额外挂几档强怪）
 */
public record BiomeSpec(String id, String cn, String en, int dangerTier, String theme,
                        List<String> donors, int[] herbCounts, int spawnStrength) {

    /** 灵植 id 顺序（与 {@link HerbCatalog#ALL} 一致）。 */
    public static List<String> herbOrder() {
        List<String> ids = new ArrayList<>();
        for (HerbCatalog.Herb herb : HerbCatalog.ALL) {
            ids.add(herb.id());
        }
        return List.copyOf(ids);
    }

    /** 某一株灵植在这个群系里的 count；不认识的灵植返回 0。 */
    public int herbCount(String herbId) {
        int idx = herbOrder().indexOf(herbId == null ? "" : herbId);
        if (idx < 0 || herbCounts == null || idx >= herbCounts.length) {
            return 0;
        }
        return herbCounts[idx];
    }

    /** 全部灵植 count 之和（"这个群系有多肥"的粗略指标）。 */
    public int totalHerbCount() {
        int sum = 0;
        if (herbCounts != null) {
            for (int c : herbCounts) {
                sum += c;
            }
        }
        return sum;
    }

    /** 低阶灵植（一~三阶）的 count 之和。 */
    public int lowTierTotal() {
        return herbCounts[0] + herbCounts[1] + herbCounts[2];
    }

    /** 高阶灵植（四~六阶）的 count 之和。 */
    public int highTierTotal() {
        return herbCounts[3] + herbCounts[4] + herbCounts[5];
    }

    /** 高阶占比：危险度越高，这个值必须越大。 */
    public double highTierRatio() {
        int low = lowTierTotal();
        if (low <= 0) {
            return highTierTotal() > 0 ? Double.MAX_VALUE : 0.0D;
        }
        return (double) highTierTotal() / low;
    }

    /** 群系的完整注册名。 */
    public String fullId() {
        return "hongmeng:" + id;
    }

    /** 语言文件键。 */
    public String langKey() {
        return "biome.hongmeng." + id;
    }

    /** 展开成 "灵脉山谷（hongmeng:lingmai_valley，危险 1）"。 */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s（%s，危险 %d）", cn, fullId(), dangerTier);
    }
}
