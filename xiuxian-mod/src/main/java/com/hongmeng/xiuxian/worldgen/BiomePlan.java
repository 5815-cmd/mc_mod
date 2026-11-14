package com.hongmeng.xiuxian.worldgen;

import com.hongmeng.xiuxian.alchemy.HerbCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>三个新群系的策划表</b>（纯数据，可离线断言）。
 *
 * <p>玩家原话："在地面生成灵植什么的"。既有的六种灵植已经有
 * {@code biome_modifier} 挂在<b>原版群系</b>上（雪莲草 count=8 等），
 * 但那些数字是"野外偶尔碰到"的量级。新群系要的是<b>成片</b>的灵植，
 * 于是每个新群系 × 每株灵植都有一份自己的密度，全都<b>高于</b>原版的对应值。</p>
 *
 * <h2>三个群系</h2>
 * <table border="1">
 *   <caption>新群系（危险度越高，高阶灵植占比越大）</caption>
 *   <tr><th>id</th><th>中文名</th><th>危险度</th><th>主题</th></tr>
 *   <tr><td>lingmai_valley</td><td>灵脉山谷</td><td>1</td><td>灵植密集的低危采集地</td></tr>
 *   <tr><td>yunmeng_marsh</td><td>云梦泽</td><td>2</td><td>水泽湿地，稀有灵植</td></tr>
 *   <tr><td>huanggu_battlefield</td><td>荒古战场</td><td>3</td><td>废墟 + 强怪，高阶灵植</td></tr>
 * </table>
 *
 * <h2>密度表（每区块的 count，顺序 = {@link HerbCatalog#ALL}）</h2>
 * <pre>
 *                        雪莲草 火焰花 寒冰藤 雷音草 空冥兰 血珞芝   高阶占比
 *   原版 overworld           8     5     6     5     3     2      0.53
 *   灵脉山谷（危险 1）       32    24    18    12     6     3      0.28
 *   云梦泽（危险 2）         20    10    26    10    16     8      0.61
 *   荒古战场（危险 3）       12    16    18    22    12     6      0.87
 * </pre>
 *
 * <p>"高阶占比" = (雷音草 + 空冥兰 + 血珞芝) / (雪莲草 + 火焰花 + 寒冰藤)。
 * 它必须<b>随危险度严格上升</b>——这就是"按群系难度做品阶门控"的可断言形式：
 * 低危群系刷低阶灵植，高危群系才刷得出高阶灵植。</p>
 *
 * <h2>地形怎么来</h2>
 * <p>新群系<b>不新造噪声设置</b>，而是接管主世界已有的一个"气候点位"
 * （见 {@link BiomeSpec#donors()}）：地形、高度、地表演变全部沿用被接管点位的原样，
 * 只是把那个点位的群系换成新的。这样做的两个理由：</p>
 * <ol>
 *   <li><b>安全</b>：不碰 {@code noise_settings}、不覆盖 {@code minecraft:overworld}
 *       维度文件，就不会把别人的世界生成搅坏；</li>
 *   <li><b>可验证</b>：群系一定出现在被接管点位原本出现的纬度/高度/地表上，
 *       用 {@code /locate biome} 必然找得到（不是"理论上存在"）。</li>
 * </ol>
 */
public final class BiomePlan {

    private BiomePlan() {
    }

    // ------------------------------------------------------------------
    // 三个群系
    // ------------------------------------------------------------------

    /** 灵脉山谷：地脉灵气汇聚的谷地，六种灵植成片长，危险度最低（新手采药场）。 */
    public static final BiomeSpec LINGMAI_VALLEY = new BiomeSpec(
            "lingmai_valley", "灵脉山谷", "Spirit Vein Valley", 1,
            "地脉灵气外溢的谷地，六阶灵植成片生长",
            List.of("minecraft:forest", "minecraft:birch_forest", "minecraft:flower_forest",
                    "minecraft:plains", "minecraft:taiga", "minecraft:meadow"),
            // 雪莲草 火焰花 寒冰藤 雷音草 空冥兰 血珞芝
            new int[]{32, 24, 18, 12, 6, 3}, 1);

    /** 云梦泽：水泽湿地，寒冰藤与空冥兰最多（水属灵植的产地）。 */
    public static final BiomeSpec YUNMENG_MARSH = new BiomeSpec(
            "yunmeng_marsh", "云梦泽", "Cloud Dream Marsh", 2,
            "终年水汽不散的大泽，水属与空间属灵植的产地",
            // 首选**红树林沼泽**而不是河流：实测接管河流点位时，地表是被切下去的河道
            // （一格 grass_block 都没有，水+沙+泥），灵植是作物类方块，长不出来——
            // 实机 forceload 数出来是 0 株。沼泽类点位才是"有地有水"的水泽：
            // 草地 + 浅水 + 睡莲，灵植能落地。
            // 顺序：红树林沼泽（最贴合云梦泽，且不动经典沼泽的女巫小屋）
            //      → 经典沼泽 → 河流 → 沙滩 → 森林 → 平原。
            List.of("minecraft:mangrove_swamp", "minecraft:swamp", "minecraft:river",
                    "minecraft:beach", "minecraft:forest", "minecraft:plains"),
            new int[]{20, 10, 26, 10, 16, 8}, 2);

    /** 荒古战场：上古大战的废墟，最强怪与最高阶灵植都在这里。 */
    public static final BiomeSpec HUANGGU_BATTLEFIELD = new BiomeSpec(
            "huanggu_battlefield", "荒古战场", "Ancient Battlefield", 3,
            "上古大战遗留的焦土与废墟，灵气暴烈，高阶灵植与强敌同处",
            List.of("minecraft:savanna", "minecraft:desert", "minecraft:windswept_savanna",
                    "minecraft:badlands", "minecraft:plains", "minecraft:forest"),
            new int[]{12, 16, 18, 22, 12, 6}, 2);

    /** 三个群系，顺序 = 文档顺序 = 危险度顺序。 */
    public static final List<BiomeSpec> ALL = List.of(
            LINGMAI_VALLEY, YUNMENG_MARSH, HUANGGU_BATTLEFIELD);

    /**
     * 原版 overworld 的灵植 count（顺序同 {@link HerbCatalog#ALL}）。
     *
     * <p>镜像自 {@code data/hongmeng/worldgen/placed_feature/*.json} 的
     * {@code minecraft:count}：雪莲草 8 / 火焰花 5 / 寒冰藤 6 / 雷音草 5 /
     * 空冥兰 3 / 血珞芝 2。新群系的每一个数字都必须<b>严格大于</b>它。</p>
     */
    public static final int[] OVERWORLD_HERB_COUNTS = {8, 5, 6, 5, 3, 2};

    /** 被接管的原版群系至少要剩下几个气候点位（保证原版群系不会因此消失）。 */
    public static final int MIN_DONOR_POINTS = 2;

    public static BiomeSpec byId(String id) {
        if (id == null) {
            return null;
        }
        String s = id.toLowerCase(Locale.ROOT);
        if (s.startsWith("hongmeng:")) {
            s = s.substring("hongmeng:".length());
        }
        for (BiomeSpec spec : ALL) {
            if (spec.id().equals(s)) {
                return spec;
            }
        }
        return null;
    }

    /** 群系 id → 灵植密度（文档与工具用）。 */
    public static Map<String, int[]> densityTable() {
        Map<String, int[]> t = new LinkedHashMap<>();
        for (BiomeSpec spec : ALL) {
            t.put(spec.id(), spec.herbCounts().clone());
        }
        return t;
    }

    /** 密度表的可读文本（自检输出 / 文档）。 */
    public static String densityReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%-20s", "群系"));
        for (HerbCatalog.Herb herb : HerbCatalog.ALL) {
            sb.append(String.format(Locale.ROOT, "%8s", herb.cn()));
        }
        sb.append(String.format(Locale.ROOT, "%10s", "高阶占比"));
        sb.append('\n');
        for (BiomeSpec spec : ALL) {
            sb.append(String.format(Locale.ROOT, "%-20s", spec.cn()));
            for (int c : spec.herbCounts()) {
                sb.append(String.format(Locale.ROOT, "%8d", c));
            }
            sb.append(String.format(Locale.ROOT, "%10.2f", spec.highTierRatio()));
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 自检：**任何一条坏掉就抛异常**。纯数据，离线验证器可直接调用。
     *
     * @throws IllegalStateException 群系策划表不合法
     */
    public static void validate() {
        List<String> problems = new ArrayList<>();

        if (ALL.size() != 3) {
            problems.add("新群系应当是 3 个，实际 " + ALL.size());
        }
        List<String> herbIds = BiomeSpec.herbOrder();
        if (herbIds.size() != 6) {
            problems.add("灵植应当是 6 株，实际 " + herbIds.size());
        }

        List<String> seenIds = new ArrayList<>();
        List<String> seenDonors = new ArrayList<>();
        for (BiomeSpec spec : ALL) {
            if (spec.id() == null || !spec.id().equals(spec.id().toLowerCase(Locale.ROOT))
                    || spec.id().contains(" ") || spec.id().contains(":")) {
                problems.add("群系 id 必须是蛇形小写：(" + spec.id() + ")");
            }
            if (seenIds.contains(spec.id())) {
                problems.add("群系 id 重复：" + spec.id());
            }
            seenIds.add(spec.id());
            if (spec.cn() == null || spec.cn().isBlank() || spec.en() == null || spec.en().isBlank()) {
                problems.add("群系缺中英文名：" + spec.id());
            }
            if (spec.dangerTier() < 1 || spec.dangerTier() > 3) {
                problems.add("危险度必须在 1..3：" + spec.id() + " = " + spec.dangerTier());
            }
            if (spec.spawnStrength() < 0 || spec.spawnStrength() > 3) {
                problems.add("强怪补充倍率越界（0..3）：" + spec.id());
            }
            if (spec.donors() == null || spec.donors().isEmpty()) {
                problems.add("群系没有可接管的原版群系：" + spec.id());
            } else {
                for (String donor : spec.donors()) {
                    if (!donor.startsWith("minecraft:")) {
                        problems.add("接管来源必须是原版群系（minecraft: 前缀）：" + donor);
                    }
                }
                // 只要求**首选**来源互不相同：备选链可以重叠（运行时还会跳过已被接管的点位）
                String primary = spec.donors().get(0);
                if (seenDonors.contains(primary)) {
                    problems.add("同一个原版群系被两个新群系首选接管：" + primary);
                }
                seenDonors.add(primary);
            }
            // 六株灵植一个不落，且每一个都严格高于原版密度
            if (spec.herbCounts() == null || spec.herbCounts().length != herbIds.size()) {
                problems.add("灵植密度表的长度必须是 " + herbIds.size() + "：" + spec.id());
            } else {
                for (int i = 0; i < herbIds.size(); i++) {
                    int count = spec.herbCounts()[i];
                    String herb = herbIds.get(i);
                    if (count <= 0) {
                        problems.add("灵植密度必须为正：" + spec.id() + " / " + herb);
                    }
                    if (count <= OVERWORLD_HERB_COUNTS[i]) {
                        problems.add("新群系的灵植密度必须高于原版："
                                + spec.id() + " / " + herb + " = " + count
                                + "（原版 " + OVERWORLD_HERB_COUNTS[i] + "）");
                    }
                }
            }
            if (spec.totalHerbCount() <= 0) {
                problems.add("群系一株灵植都不长：" + spec.id());
            }
        }

        // 高阶占比必须随危险度严格上升（"按难度做品阶门控"的可断言形式）
        double prevRatio = -1.0D;
        int prevDanger = 0;
        for (BiomeSpec spec : ALL) {
            if (spec.dangerTier() <= prevDanger) {
                problems.add("群系必须按危险度递增排列：" + spec.id());
            }
            if (spec.highTierRatio() <= prevRatio) {
                problems.add("高阶灵植占比没有随危险度上升：" + spec.id()
                        + " 占比 " + spec.highTierRatio());
            }
            prevRatio = spec.highTierRatio();
            prevDanger = spec.dangerTier();
        }

        // 灵脉山谷是"采集场"：低阶灵植总量必须最高
        int valleyLow = LINGMAI_VALLEY.lowTierTotal();
        for (BiomeSpec spec : ALL) {
            if (spec != LINGMAI_VALLEY && spec.lowTierTotal() >= valleyLow) {
                problems.add("灵脉山谷的低阶灵植总量应当是最高的：" + spec.id()
                        + " = " + spec.lowTierTotal() + " >= " + valleyLow);
            }
        }
        // 荒古战场是"高危区"：高阶灵植总量必须最高
        int battlefieldHigh = HUANGGU_BATTLEFIELD.highTierTotal();
        for (BiomeSpec spec : ALL) {
            if (spec != HUANGGU_BATTLEFIELD && spec.highTierTotal() >= battlefieldHigh) {
                problems.add("荒古战场的高阶灵植总量应当是最高的：" + spec.id()
                        + " = " + spec.highTierTotal() + " >= " + battlefieldHigh);
            }
        }
        // 云梦泽的水属灵植（寒冰藤）必须是三个群里最多的
        int marshFrost = YUNMENG_MARSH.herbCount("hanbing_teng");
        for (BiomeSpec spec : ALL) {
            if (spec != YUNMENG_MARSH && spec.herbCount("hanbing_teng") >= marshFrost) {
                problems.add("云梦泽的寒冰藤应当是最多的：" + spec.id());
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("鸿蒙修仙 · 新群系策划表不合法（" + problems.size()
                    + " 处）：\n  - " + String.join("\n  - ", problems));
        }
    }
}
