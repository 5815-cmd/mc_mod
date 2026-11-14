package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步玩家的全部修炼数据
 * （附带灵叶余额 + 真实世界旗标 + 道途与机制资源）。
 *
 * <h2>为什么在同一个包里塞这么多东西</h2>
 * <p>这个包已经在每次境界同步时发给玩家，客户端也已经有接收缓存 ——
 * 所以<b>加字段</b>比新开包更省，也更不容易漏发
 * （新包容易忘记在"玩家登录 / 世界切换"时补发）。</p>
 *
 * <p>⚠ 协议是<b>服务端与客户端同时发布</b>的（同一个 jar），
 * 所以加字段不会造成版本不匹配。</p>
 *
 * <h2>道途资源为什么要同步</h2>
 * <p>玩家要求能看到自己的机制状态（激荡能量 / 魔气 / 血池 / 阵域）。
 * 这些是<b>服务端算的</b>，客户端不跑逻辑 —— 所以必须同步过来才能画 HUD。</p>
 */
public class SyncCultivationPacket {

    private final CultivationData data;
    private final int leaves;
    /** 真实世界是否已开启（决定客户端的维度外观）。 */
    private final boolean realWorld;
    /** 主修序号（-1 = 尚未择道）。 */
    private final int path;
    /** 机制资源快照（激荡 / 魔气 / 血池 / 阵域，都是 0~1）。 */
    private final float[] resources;
    /** 状态旗标：bit0 激荡引爆中、bit1 锁血、bit2 阵域展开、bit3 缓伤池有货。 */
    private final int flags;
    /** 道途摘要（服务端算好，客户端面板直接显示）。 */
    private final String pathSummary;

    public SyncCultivationPacket(CultivationData data, int leaves, boolean realWorld,
                                 int path, float[] resources, int flags,
                                 String pathSummary) {
        this.data = data;
        this.leaves = leaves;
        this.realWorld = realWorld;
        this.path = path;
        this.resources = resources == null ? new float[4] : resources;
        this.flags = flags;
        this.pathSummary = pathSummary == null ? "" : pathSummary;
    }

    public static void encode(SyncCultivationPacket packet, FriendlyByteBuf buf) {
        packet.data.toNetwork(buf);
        buf.writeVarInt(packet.leaves);
        buf.writeBoolean(packet.realWorld);
        buf.writeVarInt(packet.path);
        for (int i = 0; i < 4; i++) {
            buf.writeFloat(i < packet.resources.length ? packet.resources[i] : 0.0F);
        }
        buf.writeVarInt(packet.flags);
        buf.writeUtf(packet.pathSummary, 512);
    }

    public static SyncCultivationPacket decode(FriendlyByteBuf buf) {
        CultivationData d = CultivationData.fromNetwork(buf);
        int leaves = buf.readVarInt();
        boolean rw = buf.readBoolean();
        int path = buf.readVarInt();
        float[] res = new float[4];
        for (int i = 0; i < 4; i++) {
            res[i] = buf.readFloat();
        }
        int flags = buf.readVarInt();
        String summary = buf.readUtf(512);
        return new SyncCultivationPacket(d, leaves, rw, path, res, flags, summary);
    }

    public static void handle(SyncCultivationPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ClientCultivationCache.set(packet.data);
            com.hongmeng.xiuxian.shop.ClientShopCache.set(packet.leaves);
            // 真实世界：转发给维度外观（走反射式客户端入口，专服上不会加载客户端类）
            com.hongmeng.xiuxian.network.ModNetworkBridge.setRealWorld(packet.realWorld);
            // 道途与机制资源（HUD 用）
            ClientCultivationCache.setPathState(packet.path, packet.resources,
                    packet.flags, packet.pathSummary);
        });
        context.setPacketHandled(true);
    }

    public static void send(ServerPlayer player, CultivationData data) {
        boolean rw = player.level().getServer() != null
                && com.hongmeng.xiuxian.realworld.RealWorld.active(player.level().getServer());
        // 职业与机制资源
        var path = com.hongmeng.xiuxian.talent.Talents.pathOf(player);
        double[] snap = com.hongmeng.xiuxian.talent.PathRuntime.snapshot(player);
        float[] res = new float[]{(float) snap[0], (float) snap[1],
                (float) snap[2], (float) snap[3]};
        int flags = 0;
        // bit0：体修激荡引爆中，或剑修剑意满层（可以放剑气了）
        if (com.hongmeng.xiuxian.talent.PathRuntime.surgeActive(player)
                || (path == com.hongmeng.xiuxian.talent.Career.JIAN_XIU
                && com.hongmeng.xiuxian.talent.PathRuntime.swordIntentOf(player)
                >= com.hongmeng.xiuxian.talent.PathMechanics.SWORD_INTENT_MAX - 1.0E-6D)) {
            flags |= 1;
        }
        if (com.hongmeng.xiuxian.talent.PathRuntime.formationOpen(player)) {
            flags |= 4;
        }
        ModNetwork.sendToPlayer(player, new SyncCultivationPacket(
                data, com.hongmeng.xiuxian.shop.ShopManager.leavesOf(player), rw,
                path == null ? -1 : path.ordinal(), res, flags,
                com.hongmeng.xiuxian.talent.Talents.clientSummary(player)));
    }
}
