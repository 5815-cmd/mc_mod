package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求突破。
 *
 * <p>修炼面板里的"破境"按钮走这个包，不必再绕一圈命令。
 * 服务端仍然会自己做全部校验（修为是否满、业力是否封顶）。</p>
 */
public class BreakthroughPacket {

    public static void encode(BreakthroughPacket packet, FriendlyByteBuf buf) {
        // 无载荷
    }

    public static BreakthroughPacket decode(FriendlyByteBuf buf) {
        return new BreakthroughPacket();
    }

    public static void handle(BreakthroughPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                com.hongmeng.xiuxian.core.Breakthrough.attempt(
                        player, com.hongmeng.xiuxian.core.CultivationCapability.get(player));
                ModNetworkBridge.sync(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void send() {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(new BreakthroughPacket());
        }
    }
}
