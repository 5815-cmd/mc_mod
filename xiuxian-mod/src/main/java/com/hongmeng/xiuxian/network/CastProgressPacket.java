package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.technique.Technique;
import com.hongmeng.xiuxian.technique.TechniqueData;

/**
 * 服务端 → 客户端：**蓄力进度**。
 *
 * <p>蓄力条是客户端画的，但"在不在蓄力、蓄到哪了"只有服务端知道
 * （权威状态在 {@link TechniqueData} 上）。所以起手 / 中断 / 完成时各发一包，
 * 客户端按"开始刻 + 总时长"自己补帧，不必每 tick 发包。</p>
 *
 * <p>中断 / 完成后发一个"空"包（{@code techniqueOrdinal = -1}）把条收掉。</p>
 */
public class CastProgressPacket {

    /** 空包：没有在蓄力。 */
    public static final int NONE = -1;

    private final int techniqueOrdinal;
    private final long startTick;
    private final int totalTicks;

    public CastProgressPacket(int techniqueOrdinal, long startTick, int totalTicks) {
        this.techniqueOrdinal = techniqueOrdinal;
        this.startTick = startTick;
        this.totalTicks = totalTicks;
    }

    /** 没有在蓄力。 */
    public static CastProgressPacket idle() {
        return new CastProgressPacket(NONE, 0L, 0);
    }

    /** 正在蓄力。 */
    public static CastProgressPacket casting(Technique technique, long startTick, int totalTicks) {
        if (technique == null || totalTicks <= 0) {
            return idle();
        }
        return new CastProgressPacket(technique.ordinal(), startTick, totalTicks);
    }

    public int techniqueOrdinal() {
        return techniqueOrdinal;
    }

    public long startTick() {
        return startTick;
    }

    public int totalTicks() {
        return totalTicks;
    }

    /** 这一包对应的功法；空包返回 null。 */
    public Technique technique() {
        if (techniqueOrdinal < 0 || techniqueOrdinal >= Technique.values().length) {
            return null;
        }
        return Technique.values()[techniqueOrdinal];
    }

    public static void encode(CastProgressPacket packet, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeVarInt(packet.techniqueOrdinal + 1);
        buf.writeVarLong(Math.max(0L, packet.startTick));
        buf.writeVarInt(Math.max(0, packet.totalTicks));
    }

    public static CastProgressPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        int code = buf.readVarInt() - 1;
        long start = buf.readVarLong();
        int total = buf.readVarInt();
        return new CastProgressPacket(code, start, total);
    }

    public static void handle(CastProgressPacket packet,
                              java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
        net.minecraftforge.network.NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> ClientCastCache.set(
                packet.technique(), packet.startTick(), packet.totalTicks()));
        context.setPacketHandled(true);
    }

    // ------------------------------------------------------------------
    // 服务端发送
    // ------------------------------------------------------------------

    /** 起手蓄力。 */
    public static void sendStart(net.minecraft.server.level.ServerPlayer player,
                                 Technique technique, long startTick) {
        sendStart(player, technique, startTick,
                technique == null ? 0 : technique.castTicks());
    }

    /**
     * 开始蓄力（指定<b>实际总时长</b>）。
     *
     * <p>道宫的「释放速度」会缩短蓄力 —— 客户端画进度条必须收到
     * 这个实际值，否则条走完了法术还没放（或反过来）。</p>
     */
    public static void sendStart(net.minecraft.server.level.ServerPlayer player,
                                 Technique technique, long startTick,
                                 int totalTicks) {
        if (player == null || technique == null) {
            return;
        }
        ModNetwork.sendToPlayer(player,
                new CastProgressPacket(technique.ordinal(), startTick,
                        Math.max(1, totalTicks)));
    }

    /** 蓄力结束（完成 / 中断 / 掉线）。 */
    public static void sendStop(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) {
            return;
        }
        ModNetwork.sendToPlayer(player, idle());
    }
}
