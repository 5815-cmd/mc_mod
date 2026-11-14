package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.core.RealmStats;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Rarity;

/**
 * 「道袍」系列可穿戴护甲的**设计数据**：十境 × 四件套。
 *
 * <h2>命名方案</h2>
 *
 * <p>与 {@link com.hongmeng.xiuxian.core.Realm} 的十境阶梯一一对应，
 * 每境一个「材质/意象」名，四件套在该名后统一加部位字：</p>
 *
 * <pre>
 *   大境界       套名          冠     胸     腿     靴
 *   1  练气     布道          布道冠  布道袍  布道护腿  布道靴
 *   2  筑基     青铜道        青铜道冠 青铜道甲 青铜护腿  青铜道靴
 *   3  金丹     白玉道        白玉道冠 白玉道甲 白玉护腿  白玉道靴
 *   4  元婴     青玉道        青玉道冠 青玉道甲 青玉护腿  青玉道靴
 *   5  化神     青纹道        青纹道冠 青纹道甲 青纹护腿  青纹道靴
 *   6  道印     金纹道        金纹道冠 金纹道甲 金纹护腿  金纹道靴
 *   7  飞升     紫霄道        紫霄道冠 紫霄道甲 紫霄护腿  紫霄道靴
 *   8  仙人     银河道        银河道冠 银河道甲 银河道护腿  银河道靴
 *   9  帝境     玄金帝甲       玄金帝冠 玄金帝甲 玄金帝护腿  玄金帝靴
 *  10  鸿蒙     鸿蒙道        鸿蒙道冠 鸿蒙道甲 鸿蒙道护腿  鸿蒙道靴
 * </pre>
 *
 * <h2>数值锚点</h2>
 *
 * <p>四件套的护甲值之和 <b>恰好等于</b> {@code round(RealmStats.armorTotal(tier))}
 * （即本境标准值 × 0.70），按原版的四件占比切分：</p>
 *
 * <pre>
 *   头盔 3/16 = 18.75%   胸甲 6/16 = 37.50%   护腿 5/16 = 31.25%   靴子 2/16 = 12.50%
 * </pre>
 *
 * <p>先按占比各自四舍五入，再把舍入残差补到头盔上，保证「四件之和 == 目标总值」
 * 而不是「大约相等」。切分比例刻意用原版的 3/6/5/2，这样护甲条上的格数分布
 * 与原版铁甲/钻石甲一致，玩家一眼就能读出强弱。</p>
 *
 * <h2>调色板</h2>
 *
 * <p>{@link #palette()} 给出 9 级明度阶梯（深→亮），供
 * {@code tools/gen_armor_textures.py} 重映射原版盔甲图层、以及绘制物品图标时共用。
 * 这样「装备栏图标」与「穿在身上的颜色」永远是同一套配色。</p>
 *
 * <p><b>本类刻意不引用任何注册表</b>（只用到 {@link Rarity} 这个纯枚举与
 * {@link ArmorItem.Type}），这样离线校验器可以安全地断言整张数值表。</p>
 *
 * @param tier      大境序号 1~10
 * @param suitCn    套名（中文），例如「布道」
 * @param suitEn    套名（英文），用于 en_us 语言文件
 * @param cn        四件中文名，顺序固定为 头盔 / 胸甲 / 护腿 / 靴子
 * @param en        四件英文名，顺序同上
 * @param defense   四件护甲值，顺序同上；四者之和 == {@code round(armorTotal(tier))}
 * @param toughness 护甲韧性（四件相同）
 * @param durability 耐久系数：以原版四件基础耐久（11/16/15/13）为基准的倍率
 * @param enchantability 附魔能力
 * @param rarity    物品稀有度（决定名字颜色）
 * @param palette   9 级明暗阶梯，下标 0 最深、8 最亮
 */
public record DaoArmorTier(int tier, String suitCn, String suitEn,
                           String[] cn, String[] en,
                           int[] defense, float toughness,
                           float durability, int enchantability,
                           Rarity rarity, int[] palette) {

    /** 四件的固定顺序：头盔 / 胸甲 / 护腿 / 靴子。 */
    public static final ArmorItem.Type[] TYPES = {
            ArmorItem.Type.HELMET,
            ArmorItem.Type.CHESTPLATE,
            ArmorItem.Type.LEGGINGS,
            ArmorItem.Type.BOOTS,
    };

    /** 注册名用的部位后缀，顺序与 {@link #TYPES} 一致。 */
    public static final String[] SLOT_IDS = {"helmet", "chestplate", "leggings", "boots"};

    /** 原版四件的基础耐久（{@code ArmorMaterials} 的「基础值」栏，除以 11 之前的原始数）。 */
    private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

    /** 四件护甲值占比（原版口径）：3/16、6/16、5/16、2/16。 */
    private static final double[] DEFENSE_SHARE = {3.0D / 16.0D, 6.0D / 16.0D, 5.0D / 16.0D, 2.0D / 16.0D};

    /** 十境的设计数据。下标 0 = 练气，下标 9 = 鸿蒙。 */
    private static final DaoArmorTier[] TIERS = {
            new DaoArmorTier(1, "布道", "Cloth Dao",
                    new String[]{"布道冠", "布道袍", "布道护腿", "布道靴"},
                    new String[]{"Cloth Dao Crown", "Cloth Dao Robe", "Cloth Dao Leggings", "Cloth Dao Boots"},
                    defense(1), 0.4F, 0.8F, 12, Rarity.COMMON,
                    new int[]{0x2A2118, 0x3A2D20, 0x4B3A2A, 0x5C4834, 0x6F573E, 0x8A6E4E, 0xA98A63, 0xC6A87C, 0xE2CBA1}),

            new DaoArmorTier(2, "青铜道", "Bronze Dao",
                    new String[]{"青铜道冠", "青铜道甲", "青铜护腿", "青铜道靴"},
                    new String[]{"Bronze Dao Crown", "Bronze Dao Chestplate", "Bronze Dao Leggings", "Bronze Dao Boots"},
                    defense(2), 1.2F, 0.9F, 15, Rarity.COMMON,
                    new int[]{0x231A10, 0x33261A, 0x453423, 0x57422C, 0x6B5237, 0x87683F, 0xA98752, 0xC7A96F, 0xE6CE96}),

            new DaoArmorTier(3, "白玉道", "White Jade Dao",
                    new String[]{"白玉道冠", "白玉道甲", "白玉护腿", "白玉道靴"},
                    new String[]{"White Jade Dao Crown", "White Jade Dao Chestplate", "White Jade Dao Leggings", "White Jade Dao Boots"},
                    defense(3), 3.0F, 1.0F, 18, Rarity.UNCOMMON,
                    new int[]{0x3C3A33, 0x4E4C43, 0x626054, 0x787668, 0x908E7E, 0xABA894, 0xC7C4AE, 0xE1DFC9, 0xF6F5E6}),

            new DaoArmorTier(4, "青玉道", "Azure Jade Dao",
                    new String[]{"青玉道冠", "青玉道甲", "青玉护腿", "青玉道靴"},
                    new String[]{"Azure Jade Dao Crown", "Azure Jade Dao Chestplate", "Azure Jade Dao Leggings", "Azure Jade Dao Boots"},
                    defense(4), 6.0F, 1.1F, 21, Rarity.UNCOMMON,
                    new int[]{0x08211F, 0x0C332F, 0x104741, 0x155D54, 0x1B766A, 0x249184, 0x36AD9E, 0x5FC9BC, 0x9BE4DB}),

            new DaoArmorTier(5, "青纹道", "Cyan Vein Dao",
                    new String[]{"青纹道冠", "青纹道甲", "青纹护腿", "青纹道靴"},
                    new String[]{"Cyan Vein Dao Crown", "Cyan Vein Dao Chestplate", "Cyan Vein Dao Leggings", "Cyan Vein Dao Boots"},
                    defense(5), 12.0F, 1.2F, 24, Rarity.RARE,
                    new int[]{0x04212B, 0x06333F, 0x094757, 0x0C5D71, 0x11768D, 0x1A92AB, 0x2BAFC8, 0x55CDE2, 0x9CEBF7}),

            new DaoArmorTier(6, "金纹道", "Gold Vein Dao",
                    new String[]{"金纹道冠", "金纹道甲", "金纹护腿", "金纹道靴"},
                    new String[]{"Gold Vein Dao Crown", "Gold Vein Dao Chestplate", "Gold Vein Dao Leggings", "Gold Vein Dao Boots"},
                    defense(6), 22.0F, 1.35F, 27, Rarity.RARE,
                    new int[]{0x2B1D05, 0x3E2B08, 0x553C0B, 0x6E4F0F, 0x8A6412, 0xA97C15, 0xC99718, 0xE6B41F, 0xFFD75C}),

            new DaoArmorTier(7, "紫霄道", "Violet Firmament Dao",
                    new String[]{"紫霄道冠", "紫霄道甲", "紫霄护腿", "紫霄道靴"},
                    new String[]{"Violet Firmament Dao Crown", "Violet Firmament Dao Chestplate", "Violet Firmament Dao Leggings", "Violet Firmament Dao Boots"},
                    defense(7), 38.0F, 1.5F, 30, Rarity.EPIC,
                    new int[]{0x1E0A38, 0x2C114F, 0x3B1768, 0x4C1E84, 0x5E27A2, 0x7434BF, 0x8F4ED8, 0xB077EC, 0xD8B0FF}),

            new DaoArmorTier(8, "银河道", "Silver River Dao",
                    new String[]{"银河道冠", "银河道甲", "银河道护腿", "银河道靴"},
                    new String[]{"Silver River Dao Crown", "Silver River Dao Chestplate", "Silver River Dao Leggings", "Silver River Dao Boots"},
                    defense(8), 60.0F, 1.65F, 33, Rarity.EPIC,
                    new int[]{0x2C3138, 0x3C434C, 0x4E5762, 0x626D7A, 0x7A8796, 0x96A3B3, 0xB4C1D1, 0xD2DEEC, 0xF2F7FF}),

            new DaoArmorTier(9, "玄金帝甲", "Dark Gold Imperial",
                    new String[]{"玄金帝冠", "玄金帝甲", "玄金帝护腿", "玄金帝靴"},
                    new String[]{"Dark Gold Imperial Crown", "Dark Gold Imperial Chestplate", "Dark Gold Imperial Leggings", "Dark Gold Imperial Boots"},
                    defense(9), 90.0F, 1.8F, 36, Rarity.EPIC,
                    new int[]{0x1C1405, 0x2C2108, 0x40300B, 0x57410F, 0x715515, 0x8D6B1D, 0xAB8528, 0xCBA542, 0xF0D283}),

            new DaoArmorTier(10, "鸿蒙道", "Hongmeng Dao",
                    new String[]{"鸿蒙道冠", "鸿蒙道甲", "鸿蒙道护腿", "鸿蒙道靴"},
                    new String[]{"Hongmeng Dao Crown", "Hongmeng Dao Chestplate", "Hongmeng Dao Leggings", "Hongmeng Dao Boots"},
                    defense(10), 140.0F, 1.9F, 40, Rarity.EPIC,
                    new int[]{0x17102B, 0x241A44, 0x352461, 0x4A2E82, 0x6339A8, 0x7F46C8, 0x3FB4E0, 0x7FE3F2, 0xE8FBFF}),
    };

    public DaoArmorTier {
        cn = cn.clone();
        en = en.clone();
        defense = defense.clone();
        palette = palette.clone();
    }

    @Override
    public String[] cn() {
        return cn.clone();
    }

    @Override
    public String[] en() {
        return en.clone();
    }

    @Override
    public int[] defense() {
        return defense.clone();
    }

    @Override
    public int[] palette() {
        return palette.clone();
    }

    /** 某大境（1~10）的设计数据。 */
    public static DaoArmorTier of(int tier) {
        return TIERS[Math.max(1, Math.min(10, tier)) - 1];
    }

    /** 全部十境。 */
    public static DaoArmorTier[] all() {
        return TIERS.clone();
    }

    /** 注册名：{@code dao_armor_<tier>_<slot>}。 */
    public static String idOf(int tier, int slot) {
        return "dao_armor_" + tier + "_" + SLOT_IDS[slot];
    }

    /** 全套护甲值总和（== {@link RealmStats#armorTotal(int)} 的四舍五入）。 */
    public int totalDefense() {
        int s = 0;
        for (int d : defense) {
            s += d;
        }
        return s;
    }

    /** 该境四件套的护甲值是否符合「总和 == round(标准值 × 0.70)」。 */
    public boolean matchesLadder() {
        return totalDefense() == (int) Math.round(RealmStats.armorTotal(tier));
    }

    /** 第 slot 件的耐久（原版公式：基础耐久 × 耐久系数）。 */
    public int durabilityFor(int slot) {
        return (int) (BASE_DURABILITY[slot] * durability);
    }

    /** 第 slot 件的护甲值。 */
    public int defenseFor(int slot) {
        return defense[slot];
    }

    /** 第 slot 件的原版部位类型。 */
    public ArmorItem.Type typeFor(int slot) {
        return TYPES[slot];
    }

    /** 材质名（护甲图层贴图的文件名）：{@code hongmeng:dao_armor_<tier>}。 */
    public String materialName() {
        return "hongmeng:dao_armor_" + tier;
    }

    /**
     * 切分护甲值：按原版占比四舍五入，残差补到头盔，保证四件之和精确等于目标。
     *
     * <p>目标 = {@code round(RealmStats.armorTotal(tier))} = {@code round(标准值 × 0.70)}。</p>
     */
    private static int[] defense(int tier) {
        double total = RealmStats.armorTotal(tier);
        int target = (int) Math.round(total);
        int[] out = new int[4];
        for (int i = 0; i < 4; i++) {
            out[i] = (int) Math.round(total * DEFENSE_SHARE[i]);
        }
        int sum = 0;
        for (int v : out) {
            sum += v;
        }
        out[0] += target - sum;   // 残差（通常只有几个点）全部记在头盔上
        if (out[0] < 0) {
            out[0] = 0;
        }
        return out;
    }
}
