package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 真实痛觉：受伤时触发客户端画面反馈。
 * 原文里主角被斩马刀划伤胳膊，第一次意识到"这是真的"。
 */
public class PainFlashPacket {

    private final int ticks;
    private final float amount;

    public PainFlashPacket(int ticks, float amount) {
        this.ticks = ticks;
        this.amount = amount;
    }

    public static void encode(PainFlashPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.ticks);
        buf.writeFloat(packet.amount);
    }

    public static PainFlashPacket decode(FriendlyByteBuf buf) {
        return new PainFlashPacket(buf.readVarInt(), buf.readFloat());
    }

    public static void handle(PainFlashPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ClientCultivationCache.flashPain(packet.ticks);
            ClientPainState.setIntensity(packet.amount);
        });
        context.setPacketHandled(true);
    }
}
