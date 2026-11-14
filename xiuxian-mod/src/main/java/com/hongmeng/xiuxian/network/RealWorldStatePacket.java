package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>真实世界状态同步</b>（服务端 → 客户端）。
 *
 * <h2>为什么要单独一个包</h2>
 * <p>「真实世界开启后世界要发生变化」（玩家要求，参考泰拉瑞亚肉前/肉后）——
 * 这是<b>世界级</b>状态，客户端必须知道才能改雾色、改环境表现。</p>
 *
 * <p>塞进 {@code SyncCultivationPacket} 也能做，但那个包是<b>逐玩家高频</b>的，
 * 而真世界状态<b>一辈子只变一两次</b>。混在一起会让"这个字段为什么每次同步"
 * 变成后来人的困惑 —— 所以单独一个低频包。</p>
 */
public class RealWorldStatePacket {

    private final boolean active;

    public RealWorldStatePacket(boolean active) {
        this.active = active;
    }

    public static void encode(RealWorldStatePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.active);
    }

    public static RealWorldStatePacket decode(FriendlyByteBuf buf) {
        return new RealWorldStatePacket(buf.readBoolean());
    }

    public static void handle(RealWorldStatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.RealWorldClientFx
                        .setActive(packet.active)));
        context.setPacketHandled(true);
    }
}
