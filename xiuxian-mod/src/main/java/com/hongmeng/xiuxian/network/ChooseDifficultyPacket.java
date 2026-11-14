package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：选定开局难度。 */
public class ChooseDifficultyPacket {

    private final String difficultyId;

    public ChooseDifficultyPacket(String difficultyId) {
        this.difficultyId = difficultyId;
    }

    public static void encode(ChooseDifficultyPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.difficultyId);
    }

    public static ChooseDifficultyPacket decode(FriendlyByteBuf buf) {
        return new ChooseDifficultyPacket(buf.readUtf());
    }

    public static void handle(ChooseDifficultyPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(player);
            if (data.hasChosenDifficulty()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7c难度已经选过，不能改了。"));
                return;
            }
            com.hongmeng.xiuxian.difficulty.CultivationDifficulty diff =
                    com.hongmeng.xiuxian.difficulty.CultivationDifficulty.byId(packet.difficultyId);
            data.setDifficultyId(diff.name());

            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7b\u00a7l═══ 难度已定 ═══"));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7f你选择了 " + diff.colored() + "\u00a7f。"));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a77" + diff.desc()));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7f灵根保底：\u00a7e" + diff.rootFloorName()
                            + "   \u00a7f道宫保底：\u00a7e" + diff.domainFloorName()
                            + "   \u00a7f怪物强度：\u00a7c×"
                            + String.format("%.2f", diff.mobHealthMultiplier())));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));

            com.hongmeng.xiuxian.network.ModNetworkBridge.sync(player);
            // 立刻按新难度抽灵根
            com.hongmeng.xiuxian.core.Awakening.awaken(player, data, "天地");
            // 选完难度就从云层上落回地面（开局演出的下半场）
            com.hongmeng.xiuxian.core.SkyIntro.descend(player);
        });
        context.setPacketHandled(true);
    }

    public static void send(String difficultyId) {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(new ChooseDifficultyPacket(difficultyId));
        }
    }
}
