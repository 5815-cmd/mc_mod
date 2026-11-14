package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：道宫操作（凝聚 / 破碎 / 开关领域 / 领域友伤开关）。 */
public class DomainActionPacket {

    public enum Action {
        CONDENSE,
        SHATTER,
        TOGGLE,
        /**
         * 切换领域的友伤开关（规格 §2.1，默认关）。
         *
         * <p><b>只许追加在最后</b>：{@code FriendlyByteBuf.writeEnum} 写的是 ordinal，
         * 往中间插值会让老客户端的操作错位。</p>
         */
        FRIENDLY_FIRE
    }

    private final Action action;
    private final int index;

    public DomainActionPacket(Action action, int index) {
        this.action = action;
        this.index = index;
    }

    public static void encode(DomainActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeVarInt(packet.index);
    }

    public static DomainActionPacket decode(FriendlyByteBuf buf) {
        return new DomainActionPacket(buf.readEnum(Action.class), buf.readVarInt());
    }

    public static void handle(DomainActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(player);
            switch (packet.action) {
                case CONDENSE -> com.hongmeng.xiuxian.domain.DomainManager.condense(player, data);
                case SHATTER -> com.hongmeng.xiuxian.domain.DomainManager
                        .shatter(player, data, packet.index);
                case TOGGLE -> com.hongmeng.xiuxian.domain.DomainManager.toggleDomain(player, data);
                case FRIENDLY_FIRE -> com.hongmeng.xiuxian.domain.DomainManager
                        .toggleFriendlyFire(player, data);
            }
        });
        context.setPacketHandled(true);
    }

    public static void send(Action action, int index) {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(new DomainActionPacket(action, index));
        }
    }
}
