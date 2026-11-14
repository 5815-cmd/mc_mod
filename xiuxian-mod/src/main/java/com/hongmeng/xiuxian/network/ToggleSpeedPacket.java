package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：切换"速度 II"的开关（帝境及以上可用）。
 */
public class ToggleSpeedPacket {

    private final boolean enabled;

    public ToggleSpeedPacket(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(ToggleSpeedPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
    }

    public static ToggleSpeedPacket decode(FriendlyByteBuf buf) {
        return new ToggleSpeedPacket(buf.readBoolean());
    }

    public static void handle(ToggleSpeedPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(player);
            data.setSpeedBoostEnabled(packet.enabled);
            // 立刻重算属性并回同步
            com.hongmeng.xiuxian.core.ModEvents.applyRealmAttributes(player, data);
            ModNetworkBridge.sync(player);
        });
        context.setPacketHandled(true);
    }

    /** 客户端调用：把自己的意图发给服务端。 */
    public static void send(boolean enabled) {
        if (ModNetwork.CHANNEL == null) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new ToggleSpeedPacket(enabled));
    }
}
