package com.hongmeng.xiuxian.shop;

import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.network.ClientCultivationCache;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 客户端钱包视图：把"灵叶余额"和"背包里的实物"分开显示。
 *
 * <p>余额是服务端权威数据，随 {@code SyncCultivationPacket} 一起同步过来
 * （存在 {@link com.hongmeng.xiuxian.core.CultivationData#getLeaves()} 里）；
 * 背包实物由客户端自己数——只有开着坊市界面时才算，开销可以忽略。</p>
 *
 * <p>只被 {@link ShopScreen} 与任务面板的坊市页签使用；不碰任何客户端专属类，
 * 所以不需要 {@code @OnlyIn(Dist.CLIENT)}。</p>
 */
public final class ClientLeafWallet {

    private ClientLeafWallet() {
    }

    /** 坊市余额（服务端同步过来的权威值）。 */
    public static int balance() {
        return ClientCultivationCache.get().getLeaves();
    }

    /** 背包里的实物灵叶数量。 */
    public static int heldItems(Player player) {
        return count(player, ModItems.LINGYE.get());
    }

    /** 能花的灵叶总数 = 余额 + 背包实物（与界面上的"可用"一致）。 */
    public static int total(Player player) {
        return balance() + heldItems(player);
    }

    /** 背包里的纪元令数量。 */
    public static int tokens(Player player) {
        return count(player, ModItems.JIDAN.get());
    }

    /** 某个商品需要的货币，玩家手上有多少（灵叶商品算"余额 + 背包实物"）。 */
    public static int heldFor(Player player, ShopEntry entry) {
        if (entry.usesLeaves()) {
            return total(player);
        }
        return count(player, entry.resolveCurrencyItem());
    }

    /** 数一数玩家背包里有多少个某物品。 */
    public static int count(Player player, Item item) {
        if (player == null || item == null || item == Items.AIR) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
