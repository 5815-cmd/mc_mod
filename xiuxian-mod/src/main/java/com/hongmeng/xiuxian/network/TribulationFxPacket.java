package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>雷劫天象</b>（开始 / 结束）。
 *
 * <h2>为什么要同步 tier</h2>
 * <p>玩家要求「雷劫随境界换天象色：第 6 境青色雷、第 9 境紫雷、第 10 境血雷」。
 * 劫云与落雷的<b>颜色</b>由境界决定，而境界是服务端的 —— 客户端必须被告知。</p>
 *
 * <h2>为什么只在开始/结束各发一次</h2>
 * <p>整个雷劫的持续时间服务端已经算好（{@code TRIBULATION_TOTAL_TICKS} 家族），
 * 客户端拿到"还剩多少 tick"就能自己画完 —— 不需要每 tick 同步。</p>
 */
public class TribulationFxPacket {

    private final boolean active;
    private final int tier;
    private final int ticks;

    public TribulationFxPacket(boolean active, int tier, int ticks) {
        this.active = active;
        this.tier = tier;
        this.ticks = ticks;
    }

    public static void encode(TribulationFxPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.active);
        buf.writeVarInt(p.tier);
        buf.writeVarInt(p.ticks);
    }

    public static TribulationFxPacket decode(FriendlyByteBuf buf) {
        return new TribulationFxPacket(buf.readBoolean(), buf.readVarInt(),
                buf.readVarInt());
    }

    public static void handle(TribulationFxPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.TribulationSkyRenderer
                        .onState(p.active, p.tier, p.ticks)));
        context.setPacketHandled(true);
    }

    public boolean active() {
        return active;
    }

    public int tier() {
        return tier;
    }

    public int ticks() {
        return ticks;
    }
}
