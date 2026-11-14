package com.hongmeng.xiuxian.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 商店里的一条商品。
 *
 * <p>价格默认用灵叶结算（灵叶是存在坊市的<b>余额</b>，背包里的实物灵叶也能花，
 * 见 {@link ShopManager}）。有些商品要用别的货币买——比如用<b>纪元令</b>换
 * 炼制法宝的材料。</p>
 *
 * <p><b>刻意只存物品的注册名而不是 {@code RegistryObject}</b>：
 * 如果在静态字段里直接引用 {@code ModItems.XXX}，类初始化时就会去碰注册表，
 * 离线验证器一加载就炸（Not bootstrapped）。存名字、运行时再查表就没这个问题——
 * 货币字段 {@link #currencyItemId} 也遵守同一条规矩。</p>
 */
public record ShopEntry(
        String id,
        String cn,
        String desc,
        /** 物品注册名，例如 {@code hongmeng:pill_exp_common}。 */
        String itemId,
        int count,
        /**
         * 价格<b>数值</b>；单位由 {@link #currencyItemId} 决定。
         *
         * <p>字段名保留了历史的 {@code priceLeaves}，因为绝大多数商品确实是灵叶价，
         * 改名会牵连界面、命令与验证器——显示价格请一律用 {@link #priceText()}。</p>
         */
        int priceLeaves,
        /** 解锁所需的大境序号（1 = 练气即可买）。 */
        int minRealmTier,
        /**
         * 货币物品注册名；<b>空串 = 灵叶（余额）</b>，例如 {@code hongmeng:jidan} = 纪元令。
         */
        String currencyItemId
) {

    /** 灵叶价（老写法仍可用；不写货币一律是灵叶）。 */
    public ShopEntry(String id, String cn, String desc, String itemId, int count,
                     int priceLeaves, int minRealmTier) {
        this(id, cn, desc, itemId, count, priceLeaves, minRealmTier, "");
    }

    /** 造一个物品堆；注册名无效时返回空。 */
    public ItemStack stack() {
        Item it = resolveItem();
        return it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it, Math.max(1, count));
    }

    /** 解析物品。只应在服务端/客户端运行时调用。 */
    public Item resolveItem() {
        return resolve(itemId);
    }

    /** 解析货币物品；灵叶价时返回空气。 */
    public Item resolveCurrencyItem() {
        return resolve(currencyItemId);
    }

    private static Item resolve(String name) {
        if (name == null || name.isEmpty()) {
            return Items.AIR;
        }
        ResourceLocation rl = ResourceLocation.tryParse(name);
        if (rl == null) {
            return Items.AIR;
        }
        Item it = BuiltInRegistries.ITEM.get(rl);
        return it == null ? Items.AIR : it;
    }

    /** 是否用灵叶（余额）结算。 */
    public boolean usesLeaves() {
        return currencyItemId == null || currencyItemId.isEmpty();
    }

    /** 货币中文名（用于界面与提示）。 */
    public String currencyName() {
        if (usesLeaves()) {
            return "灵叶";
        }
        return switch (currencyItemId) {
            case "hongmeng:jidan" -> "纪元令";
            case "hongmeng:lingye" -> "灵叶";
            default -> currencyItemId;
        };
    }

    /**
     * <b>单件</b>买入价 = 总价 ÷ 一份的数量（向上取整）。
     *
     * <p>防套利断言要的就是这个数：一份商品可能是 16 个灵铁锭，
     * 比较回收价时必须换算到"一件"上，否则 16 个锭卖 48 灵叶会被
     * 误判成"低于买入价 1 灵叶"。</p>
     */
    public int unitPrice() {
        int n = Math.max(1, count);
        return (priceLeaves + n - 1) / n;
    }

    /** 一句可显示的价格，例如 {@code "20 灵叶"} 或 {@code "1 纪元令"}。 */
    public String priceText() {
        return priceLeaves + " " + currencyName();
    }

    /** 列表里用的紧凑价格，例如 {@code "20叶"} / {@code "1令"}。 */
    public String priceShort() {
        return priceLeaves + currencyGlyph();
    }

    /** 货币的单字缩写（灵叶 = 叶，纪元令 = 令）。 */
    public String currencyGlyph() {
        if (usesLeaves()) {
            return "叶";
        }
        return switch (currencyItemId) {
            case "hongmeng:jidan" -> "令";
            case "hongmeng:lingye" -> "叶";
            default -> currencyName();
        };
    }

    /** 是否达到解锁境界。 */
    public boolean unlockedFor(int realmTier) {
        return realmTier >= minRealmTier;
    }

    /** 解锁条件的中文描述。 */
    public String requirementText() {
        if (minRealmTier <= 1) {
            return "无";
        }
        String[] names = {"", "练气", "筑基", "金丹", "元婴", "化神",
                "道印", "飞升", "仙人", "帝境", "鸿蒙"};
        int idx = Math.max(1, Math.min(10, minRealmTier));
        return names[idx] + "及以上";
    }
}
