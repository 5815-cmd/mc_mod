package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：只同步「当前灵力」这一个数。
 *
 * <p>灵力每 tick 都在变（御剑 -2、打坐 +不菲），如果每秒全量重发
 * {@link SyncCultivationPacket}（灵根 + 任务 + 法宝 + 功法一大坨），
 * 带宽和 GC 都受不了。所以单独开一个 3 字节的小包，只在数值真的变了时发。</p>
 *
 * <p>客户端把它写回 {@link ClientCultivationCache} 里那份数据，
 * 灵力条（{@code HealthBarOverlay}）直接读，不需要额外缓存。</p>
 */
public class ManaSyncPacket {

    private final int mana;

    public ManaSyncPacket(int mana) {
        this.mana = mana;
    }

    public static void encode(ManaSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.mana);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buf) {
        return new ManaSyncPacket(buf.readVarInt());
    }

    public static void handle(ManaSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> ClientCultivationCache.get().setMana(packet.mana));
        context.setPacketHandled(true);
    }

    /**
     * PERF(C): 上次发给每个玩家的灵力值。
     *
     * <p>服务端每 10 tick 推一次灵力条，但满蓝挂机 / 打坐回满之后这个数根本不变，
     * 属于纯浪费的带宽。值没变就直接不发（客户端本来就有同一个数）。
     * 用 WeakHashMap 以免长期持有已下线的 {@link ServerPlayer}。</p>
     */
    private static final java.util.WeakHashMap<ServerPlayer, Integer> LAST_SENT =
            new java.util.WeakHashMap<>();

    public static void send(ServerPlayer player, int mana) {
        if (player == null) {
            return;
        }
        // PERF(C): 只在数值变化时发（同值直接跳过）
        Integer last = LAST_SENT.put(player, mana);
        if (last != null && last.intValue() == mana) {
            return;
        }
        ModNetwork.sendToPlayer(player, new ManaSyncPacket(mana));
    }
}
