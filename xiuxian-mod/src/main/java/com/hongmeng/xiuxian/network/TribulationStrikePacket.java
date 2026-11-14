package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>单道落雷的位置</b>（用于焦痕贴花）。
 *
 * <h2>为什么需要坐标</h2>
 * <p>玩家要求「雷击地形 —— 落点留下焦痕/裂纹（不破坏方块，只做贴花）」。
 * 贴花是纯客户端渲染，需要知道<b>每道雷落在哪</b>。</p>
 *
 * <h2>频次</h2>
 * <p>一场雷劫 9~45 道，也就是 9~45 个包 —— 已经比逐颗粒子发包省了几个数量级，
 * 而且雷劫是低频事件（一辈子十几次）。</p>
 */
public class TribulationStrikePacket {

    private final double x;
    private final double y;
    private final double z;
    private final int tier;

    public TribulationStrikePacket(double x, double y, double z, int tier) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
    }

    public static void encode(TribulationStrikePacket p, FriendlyByteBuf buf) {
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeVarInt(p.tier);
    }

    public static TribulationStrikePacket decode(FriendlyByteBuf buf) {
        return new TribulationStrikePacket(buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readVarInt());
    }

    public static void handle(TribulationStrikePacket p,
                              Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.TribulationSkyRenderer
                        .onStrike(p.x, p.y, p.z, p.tier)));
        context.setPacketHandled(true);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public int tier() {
        return tier;
    }
}
