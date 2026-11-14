package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>客户端 → 服务端：制作一次副业配方</b>。
 *
 * <h2>为什么只发一个配方 id</h2>
 * <p>材料够不够、熟练度够不够、成败判定、扣材料、给产物 —— <b>全部在服务端做</b>
 * （{@code ProfessionCrafting.craft}）。客户端只发"我想做这条"。</p>
 *
 * <p>界面里虽然也画了"材料够不够"，但那只是<b>显示</b>：它读的是本地背包，
 * 目的是让玩家一眼看到缺什么。真正的判定不会信任客户端。</p>
 *
 * <h2>⚠ 与服务端校验的关系</h2>
 * <p>客户端可能伪造 id（比如发一个不存在的配方、或熟练度不够的）。
 * 服务端 {@code craft} 里会把这三件事都重新查一遍，所以不会出现
 * "客户端点一下就白给产物"。</p>
 */
public class CraftPacket {

    private final String recipeId;

    public CraftPacket(String recipeId) {
        this.recipeId = recipeId == null ? "" : recipeId;
    }

    public static void encode(CraftPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.recipeId, 64);
    }

    public static CraftPacket decode(FriendlyByteBuf buf) {
        return new CraftPacket(buf.readUtf(64));
    }

    public static void handle(CraftPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.recipeId.isEmpty()) {
                return;
            }
            // 服务端重新校验：配方存在、熟练度够、材料够、成败判定
            com.hongmeng.xiuxian.talent.ProfessionCrafting.craft(player, packet.recipeId);
            // 把新的熟练度与资源同步回去，界面立刻刷新
            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(player);
            if (data != null) {
                SyncCultivationPacket.send(player, data);
            }
        });
        context.setPacketHandled(true);
    }

    /** 客户端调用。 */
    public static void send(String recipeId) {
        ModNetwork.sendToServer(new CraftPacket(recipeId));
    }
}
