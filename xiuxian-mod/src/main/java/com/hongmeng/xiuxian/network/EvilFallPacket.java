package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>坠落穿越</b>（服务端 → 客户端）：告诉客户端开始"被吸进裂口"的表演。
 *
 * <p>第五轮契约 §3 要求坠落过场 <b>≥50t</b>，所以这里在协议层就把时长夹到
 * {@link #MIN_TICKS} 以上 —— 服务端传错也演不出"一闪而过"。</p>
 */
public class EvilFallPacket {

    /** 坠落过场最短时长（契约 §3：≥50t）。 */
    public static final int MIN_TICKS = 50;

    private final int ticks;

    public EvilFallPacket(int ticks) {
        this.ticks = Math.max(MIN_TICKS, ticks);
    }

    /** 过场时长（tick），调用方只读。 */
    public int ticks() {
        return ticks;
    }

    public static void encode(EvilFallPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.ticks);
    }

    public static EvilFallPacket decode(FriendlyByteBuf buf) {
        return new EvilFallPacket(buf.readVarInt());
    }

    public static void handle(EvilFallPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.EvilFallFx.begin(p.ticks)));
        c.setPacketHandled(true);
    }
}
