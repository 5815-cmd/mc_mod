package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>怪物/Boss 的攻击动作</b>（服务端 → 附近客户端）。
 *
 * <h2>为什么需要</h2>
 * <p>{@code CombatAnim} 只处理 {@code ServerPlayer} —— 玩家挥砍有动作，
 * <b>Boss 挥击却没有任何动作</b>，只是走过去然后你的血少了一截。</p>
 *
 * <h2>为什么用 entityId 而不是 UUID</h2>
 * <p>渲染阶段（{@code RenderLivingEvent}）拿到的就是实体实例，
 * 用 id 查表最快；而且怪物的 UUID 在重生成时会变，id 足够用。</p>
 */
public class MobAttackAnimPacket {

    private final int entityId;
    private final byte kind;

    public MobAttackAnimPacket(int entityId, byte kind) {
        this.entityId = entityId;
        this.kind = kind;
    }

    public static void encode(MobAttackAnimPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeByte(p.kind);
    }

    public static MobAttackAnimPacket decode(FriendlyByteBuf buf) {
        return new MobAttackAnimPacket(buf.readVarInt(), buf.readByte());
    }

    public static void handle(MobAttackAnimPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.MobCombatAnim
                        .onAttack(p.entityId, p.kind)));
        context.setPacketHandled(true);
    }

    public int entityId() {
        return entityId;
    }

    public byte kind() {
        return kind;
    }
}
