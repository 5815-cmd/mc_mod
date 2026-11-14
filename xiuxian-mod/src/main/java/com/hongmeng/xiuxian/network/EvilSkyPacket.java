package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** <b>「恶」的天空状态</b>（服务端 → 所有客户端）。 */
public class EvilSkyPacket {

    private final float strength;
    private final long ticks;
    private final int phase;

    public EvilSkyPacket(float strength, long ticks, int phase) {
        this.strength = strength;
        this.ticks = ticks;
        this.phase = phase;
    }

    /** 服务端：用当前 {@code EvilSky} 状态构造一份同步包（供演出/战斗期复用）。 */
    public static EvilSkyPacket ofServerState() {
        return new EvilSkyPacket(com.hongmeng.xiuxian.realworld.EvilSky.strength(),
                com.hongmeng.xiuxian.realworld.EvilSky.openTicks(),
                com.hongmeng.xiuxian.realworld.EvilSky.phase());
    }

    public float strength() {
        return strength;
    }

    public long ticks() {
        return ticks;
    }

    public int phase() {
        return phase;
    }

    public static void encode(EvilSkyPacket p, FriendlyByteBuf buf) {
        buf.writeFloat(p.strength);
        buf.writeVarLong(p.ticks);
        buf.writeVarInt(p.phase);
    }

    public static EvilSkyPacket decode(FriendlyByteBuf buf) {
        return new EvilSkyPacket(buf.readFloat(), buf.readVarLong(), buf.readVarInt());
    }

    public static void handle(EvilSkyPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.realworld.EvilSky.applyFromNetwork(
                        p.strength, p.ticks, p.phase)));
        c.setPacketHandled(true);
    }
}
