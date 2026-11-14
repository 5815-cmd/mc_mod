package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：法宝装备栏操作。
 *
 * <p>只允许两种动作，且都只带索引——具体物品由服务端从玩家背包里自己取，
 * 客户端没法凭空塞东西进来。</p>
 */
public class EquipmentActionPacket {

    public enum Action {
        /** 把背包某格的法宝装到它对应的槽位。 */
        EQUIP,
        /** 卸下某个槽位的法宝，退回背包。 */
        UNEQUIP
    }

    private final Action action;
    private final int index;

    public EquipmentActionPacket(Action action, int index) {
        this.action = action;
        this.index = index;
    }

    public static void encode(EquipmentActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeVarInt(packet.index);
    }

    public static EquipmentActionPacket decode(FriendlyByteBuf buf) {
        return new EquipmentActionPacket(buf.readEnum(Action.class), buf.readVarInt());
    }

    public static void handle(EquipmentActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            switch (packet.action) {
                case EQUIP -> com.hongmeng.xiuxian.item.gear.GearManager
                        .equipFrom(player, packet.index);
                case UNEQUIP -> com.hongmeng.xiuxian.item.gear.GearManager
                        .unequip(player, packet.index);
            }
        });
        context.setPacketHandled(true);
    }

    public static void send(Action action, int index) {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(new EquipmentActionPacket(action, index));
        }
    }
}
