package com.hongmeng.xiuxian.client;

import net.minecraft.client.Minecraft;

/**
 * 客户端界面的开屏入口。
 *
 * <p>存在的唯一理由：<b>让服务端分支不出现任何 {@code Screen} / {@code Minecraft}
 * 的引用</b>。灵兽袋的"潜行右键开名册"是在服务端确认、客户端渲染的，
 * 如果直接在物品类里 {@code new BeastRosterScreen(...)}，专用服务端加载物品类时
 * 就会碰到客户端类（1.20.1 的 Forge 对此并不宽容）。</p>
 *
 * <p>界面读的数据全部来自<b>玩家手里那只袋子的 NBT</b>，所以不需要新增网络包——
 * 协议 id 顺序一点没动（规格第五节第 4 条的硬约束）。</p>
 */
public final class ClientBeastScreens {

    private ClientBeastScreens() {
    }

    /** 打开灵兽名册。 */
    public static void openRoster(net.minecraft.world.item.ItemStack pouch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(new com.hongmeng.xiuxian.client.BeastRosterScreen(pouch));
    }

    /**
     * 在玩家身上找一只灵兽袋：主手 → 副手 → 背包。
     *
     * <p>找的是<b>物品本身</b>（按注册名比对），不依赖任何服务端数据——
     * 所以这个键位在离线/单人暂停时也不会出错。</p>
     */
    public static net.minecraft.world.item.ItemStack findPouch(
            net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        net.minecraft.world.item.Item pouch =
                com.hongmeng.xiuxian.entity.beast.BeastManager.pouchItem();
        if (pouch == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(pouch)) {
                return stack;
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
