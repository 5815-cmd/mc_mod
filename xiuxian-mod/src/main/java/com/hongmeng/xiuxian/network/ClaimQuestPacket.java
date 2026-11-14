package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：领取某个任务的奖励。 */
public class ClaimQuestPacket {

    private final String questId;

    public ClaimQuestPacket(String questId) {
        this.questId = questId;
    }

    public static void encode(ClaimQuestPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.questId);
    }

    public static ClaimQuestPacket decode(FriendlyByteBuf buf) {
        return new ClaimQuestPacket(buf.readUtf());
    }

    public static void handle(ClaimQuestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                com.hongmeng.xiuxian.quest.QuestManager.claim(player, packet.questId);
            }
        });
        context.setPacketHandled(true);
    }

    public static void send(String questId) {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(new ClaimQuestPacket(questId));
        }
    }
}
