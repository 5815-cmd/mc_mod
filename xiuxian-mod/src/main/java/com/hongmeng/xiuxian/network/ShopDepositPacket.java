package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：把背包里的实物灵叶全部存入坊市余额（界面上的"存入灵叶"）。
 *
 * <p>不带任何参数：存多少由服务端数背包决定，客户端改包也变不出钱来。</p>
 */
public class ShopDepositPacket {

    public ShopDepositPacket() {
    }

    public static void encode(ShopDepositPacket packet, FriendlyByteBuf buf) {
        // 空包：动作只有一个，没有参数
    }

    public static ShopDepositPacket decode(FriendlyByteBuf buf) {
        return new ShopDepositPacket();
    }

    public static void handle(ShopDepositPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                com.hongmeng.xiuxian.shop.ShopManager.depositAll(player);
                // 余额变了，立刻把修炼数据（含灵叶余额）同步回客户端
                ModNetworkBridge.sync(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void send() {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(new ShopDepositPacket());
        }
    }
}
