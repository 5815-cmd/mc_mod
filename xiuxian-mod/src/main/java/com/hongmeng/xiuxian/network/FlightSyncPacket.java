package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：告知「你被允许飞行」。
 *
 * <p><b>为什么需要这个包：</b>飞升解锁飞行时，服务端调的是
 * {@code ServerPlayer#onUpdateAbilities()}。这个调用在**玩家刚登录/刚重生**的时刻
 * 经常不生效——客户端此时还没准备好接收 abilities 包，或者被后续的
 * {@code ClientboundPlayerAbilitiesPacket} 覆盖回 {@code mayfly=false}。</p>
 *
 * <p>玩家看到的现象就是：**按空格跳不起来，除非先开创造飞一次再切回生存**
 * （切模式会强制重发一次 abilities，恰好绕过了这个时序问题）。</p>
 *
 * <p>所以这里用一个独立的包把"能不能飞"直接告诉客户端，
 * 客户端每秒核对并自行纠正——不再依赖那个有时序问题的原版同步。</p>
 */
public class FlightSyncPacket {

    private final boolean mayFly;

    public FlightSyncPacket(boolean mayFly) {
        this.mayFly = mayFly;
    }

    public static void encode(FlightSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.mayFly);
    }

    public static FlightSyncPacket decode(FriendlyByteBuf buf) {
        return new FlightSyncPacket(buf.readBoolean());
    }

    public static void handle(FlightSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> ClientFlightCache.set(packet.mayFly));
        context.setPacketHandled(true);
    }

    public static void send(boolean mayFly) {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                    new FlightSyncPacket(mayFly));
        }
    }

    /**
     * 只发给一个玩家。
     *
     * <p><b>为什么需要这个：</b>原来只有 {@link #send(boolean)}（广播全服），
     * 那是给"境界统一的全局状态"用的。但御剑（{@code SwordFlight}）是<b>每人各自</b>
     * 的飞行许可 —— 张三御剑不该让李四也收到 {@code mayfly=true}，
     * 否则李四的客户端会把自己纠正成"能飞"。</p>
     */
    public static void sendTo(ServerPlayer player, boolean mayFly) {
        if (player == null || ModNetwork.CHANNEL == null) {
            return;
        }
        ModNetwork.sendToPlayer(player, new FlightSyncPacket(mayFly));
    }

    /**
     * 客户端侧的飞行许可缓存。
     *
     * <p>客户端每 tick 都会对照这个值纠正自己的 abilities，
     * 这样即使原版的 abilities 包丢了或被覆盖，飞行也能自己恢复。</p>
     */
    public static final class ClientFlightCache {
        private static volatile boolean mayFly = false;

        private ClientFlightCache() {
        }

        public static void set(boolean value) {
            mayFly = value;
        }

        public static boolean mayFly() {
            return mayFly;
        }
    }
}
