package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>全屏闪烁</b>（服务端 → 客户端）。
 *
 * <p>{@code count} 次、每 {@code interval} tick 一次。玩家要求"画面闪烁"，
 * 这是最直接的表达方式。</p>
 */
public class EvilFlashPacket {

    private final int count;
    private final int interval;

    public EvilFlashPacket(int count, int interval) {
        this.count = count;
        this.interval = interval;
    }

    public static void encode(EvilFlashPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.count);
        buf.writeVarInt(p.interval);
    }

    public static EvilFlashPacket decode(FriendlyByteBuf buf) {
        return new EvilFlashPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(EvilFlashPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.EvilFlashOverlay
                        .trigger(p.count, p.interval)));
        c.setPacketHandled(true);
    }
}
