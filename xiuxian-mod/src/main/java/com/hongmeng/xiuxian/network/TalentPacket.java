package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>客户端 → 服务端：给某个天赋加一点</b>。
 *
 * <h2>为什么需要这个包</h2>
 * <p>现有的动作包（{@code TechniqueActionPacket} / {@code BreakthroughPacket}）
 * 都是"无参动作"，而加点必须带<b>天赋节点 id</b>。</p>
 *
 * <h2>⚠ 为什么不在客户端直接改数据</h2>
 * <p>天赋点存在服务端（{@code Talents}），客户端只显示同步过来的摘要。
 * 点击 → 发包 → 服务端校验并加点 → 同步回来。
 * 这样"点数够不够""前置够不够"只有<b>一份实现</b>（在服务端），
 * 不会出现"客户端显示能点、服务端其实拒绝"的错位 ——
 * 那种错位比直接不支持点击更糟。</p>
 *
 * <p>服务端仍然会<b>重新校验</b>：客户端发的 id 可能是伪造的、
 * 或者玩家点的时候点数已经被别处花掉了。</p>
 */
public class TalentPacket {

    private final String nodeId;

    public TalentPacket(String nodeId) {
        this.nodeId = nodeId == null ? "" : nodeId;
    }

    public static void encode(TalentPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.nodeId, 64);
    }

    public static TalentPacket decode(FriendlyByteBuf buf) {
        return new TalentPacket(buf.readUtf(64));
    }

    public static void handle(TalentPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.nodeId.isEmpty()) {
                return;
            }
            // 服务端重新校验并加点（点数不足 / 前置不足都会在这里被拒）
            com.hongmeng.xiuxian.talent.Talents.rankUp(player, packet.nodeId);
            // 加点后**立刻**刷新属性型加成（生命上限 / 抗击退）。
            //
            // 否则要等最多 10 tick 才生效（`ModEvents` 那边是降频兜底的）——
            // 玩家点完生命天赋看血条没变，会以为"没加上"。
            try {
                com.hongmeng.xiuxian.talent.TalentAttributes.refresh(player);
            } catch (Throwable ignored) {
                // 属性刷新失败不该影响加点本身
            }
            // 加完点把新的摘要同步回去，面板立刻刷新
            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(player);
            if (data != null) {
                SyncCultivationPacket.send(player, data);
            }
        });
        context.setPacketHandled(true);
    }

    /** 客户端调用：请求给某个节点加一点。 */
    public static void send(String nodeId) {
        ModNetwork.sendToServer(new TalentPacket(nodeId));
    }
}
