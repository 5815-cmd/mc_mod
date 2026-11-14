package com.hongmeng.xiuxian.shop;

/**
 * 客户端缓存的"可用灵叶总数"。
 *
 * <p>服务端在同步修炼数据时顺带推送这个值（= 坊市余额 + 背包实物），界面直接读缓存，
 * 不必每帧扫背包。要分开显示"余额 / 背包"请看 {@link ClientLeafWallet}。</p>
 */
public final class ClientShopCache {

    private static int leaves = 0;

    private ClientShopCache() {
    }

    public static int leaves() {
        return leaves;
    }

    public static void set(int value) {
        leaves = Math.max(0, value);
    }
}
