package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 突破演出：成功/失败/天劫。 */
public class BreakthroughFxPacket {

    public enum Kind {
        SUCCESS,
        FAIL,
        TRIBULATION
    }

    private final Kind kind;
    private final String realmName;

    public BreakthroughFxPacket(Kind kind, String realmName) {
        this.kind = kind;
        this.realmName = realmName == null ? "" : realmName;
    }

    public static void encode(BreakthroughFxPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.kind);
        buf.writeUtf(packet.realmName);
    }

    public static BreakthroughFxPacket decode(FriendlyByteBuf buf) {
        return new BreakthroughFxPacket(buf.readEnum(Kind.class), buf.readUtf());
    }

    public static void handle(BreakthroughFxPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> ClientBreakthroughFx.trigger(packet.kind, packet.realmName));
        context.setPacketHandled(true);
    }
}
