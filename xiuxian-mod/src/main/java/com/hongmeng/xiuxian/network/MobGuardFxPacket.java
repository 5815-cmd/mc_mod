package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>怪物格挡的气盾表现</b>（服务端 → 附近客户端）。
 *
 * <h2>为什么单独一个包</h2>
 * <p>格挡的<b>判定</b>在服务端，但<b>气盾是渲染</b>。
 * 客户端不知道哪只怪在挡（那存在服务端的持久化数据里），所以必须同步。</p>
 *
 * <h2>为什么只有起手时发一次</h2>
 * <p>格挡持续 16 tick，客户端拿到"entityId + 时长"就能自己算淡入淡出 ——
 * 比每 tick 同步省几十倍带宽。</p>
 */
public class MobGuardFxPacket {

    private final int entityId;
    private final int ticks;

    public MobGuardFxPacket(int entityId, int ticks) {
        this.entityId = entityId;
        this.ticks = ticks;
    }

    public static void encode(MobGuardFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.ticks);
    }

    public static MobGuardFxPacket decode(FriendlyByteBuf buf) {
        return new MobGuardFxPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MobGuardFxPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.MobShieldRenderer
                        .onGuard(packet.entityId, packet.ticks)));
        context.setPacketHandled(true);
    }

    public int entityId() {
        return entityId;
    }

    public int ticks() {
        return ticks;
    }
}
