package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：购买商店里第 index 条商品。
 *
 * <p>刻意只传索引与数量——价格、物品、解锁条件全部由服务端查表决定，
 * 客户端改包也改不出便宜。</p>
 */
public class ShopBuyPacket {

    private final int entryIndex;
    private final int count;

    public ShopBuyPacket(int entryIndex, int count) {
        this.entryIndex = entryIndex;
        this.count = count;
    }

    public static void encode(ShopBuyPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entryIndex);
        buf.writeVarInt(packet.count);
    }

    public static ShopBuyPacket decode(FriendlyByteBuf buf) {
        return new ShopBuyPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ShopBuyPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                com.hongmeng.xiuxian.shop.ShopManager.buy(
                        player, packet.entryIndex, packet.count);
                ModNetworkBridge.sync(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void send(int entryIndex, int count) {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(new ShopBuyPacket(entryIndex, count));
        }
    }
}
