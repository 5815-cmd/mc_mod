package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * <b>攻击动作广播</b>：谁挥了什么武器、第几段连击。
 *
 * <h2>为什么要发包</h2>
 * <p>攻击判定在服务端，动作表现在客户端渲染 —— 不放包的话别人看你攻击
 * 永远只有原版那一下摆臂，没有"招式"。</p>
 *
 * <h2>为什么带连击序号</h2>
 * <p>参考动作 mod（Better Combat / Epic Fight）的做法：<b>同一把武器连打三段
 * 换三种动作</b>，比"一段动作重复播"有节奏得多。连击在服务端计时
 * （{@code CombatAnim.comboOf}），客户端只负责按序号摆姿势。</p>
 */
public class AttackAnimPacket {

    private final UUID player;
    private final byte kind;
    private final byte combo;

    public AttackAnimPacket(UUID player, int kind, int combo) {
        this.player = player;
        this.kind = (byte) kind;
        this.combo = (byte) combo;
    }

    public static void encode(AttackAnimPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.player);
        buf.writeByte(packet.kind);
        buf.writeByte(packet.combo);
    }

    public static AttackAnimPacket decode(FriendlyByteBuf buf) {
        return new AttackAnimPacket(buf.readUUID(), buf.readByte(), buf.readByte());
    }

    public static void handle(AttackAnimPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // 渲染状态只在客户端存在 —— 用 DistExecutor 隔离，避免服务端加载客户端类
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.CombatAnimClient
                        .onAttack(packet.player, packet.kind, packet.combo)));
        context.setPacketHandled(true);
    }
}
