package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.talent.Career;
import com.hongmeng.xiuxian.talent.Talents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>客户端 → 服务端：选定道途</b>。
 *
 * <h2>为什么需要这个包</h2>
 * <p>之前择道<b>只能打命令</b>（{@code /hongmeng path 法修}），
 * 而 {@code Talents.choose} 的唯一调用点就是命令处理器。</p>
 *
 * <p>于是在修炼面板里，玩家看到的是：</p>
 * <pre>
 *   道途
 *   尚未择道 — 用 /hongmeng path &lt;法修|体修|魔修|血修|阵修&gt; 选一条
 * </pre>
 * <p>—— 一个<b>让玩家去打字</b>的界面。这是"核心功能不可发现"的典型：
 * 玩家打开面板想选，却没有东西可点。</p>
 *
 * <h2>⚠ 只发序号，服务端重新校验</h2>
 * <p>客户端只发"我想要第几条道"，其余全部由服务端
 * {@link Talents#choose} 处理（它自己会拒绝已择道的情况）。
 * 客户端不能伪造"我已经点满了天赋"之类的东西。</p>
 */
public class PathChoosePacket {

    private final int ordinal;

    public PathChoosePacket(int ordinal) {
        this.ordinal = ordinal;
    }

    public static void encode(PathChoosePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.ordinal);
    }

    public static PathChoosePacket decode(FriendlyByteBuf buf) {
        return new PathChoosePacket(buf.readVarInt());
    }

    public static void handle(PathChoosePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            Career[] all = Career.values();
            if (packet.ordinal < 0 || packet.ordinal >= all.length) {
                return;                       // 非法序号：静默忽略
            }
            // 服务端重新校验（choose 内部会拒绝"已择业"）
            if (Talents.choose(player, all[packet.ordinal])) {
                var data = CultivationCapability.get(player);
                if (data != null) {
                    SyncCultivationPacket.send(player, data);
                }
            }
        });
        context.setPacketHandled(true);
    }

    /** 客户端调用。 */
    public static void send(int ordinal) {
        ModNetwork.sendToServer(new PathChoosePacket(ordinal));
    }
}
