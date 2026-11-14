package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>Boss 出场包</b>：告诉客户端"这里有个 boss 要从虚空裂缝里缓缓降下"。
 *
 * <p>玩家要求：「boss 从天空的虚空裂缝，缓慢出场降落」。</p>
 *
 * <p>本包只带位置与时长；具体每一段播什么贴图、boss 抬高多少，
 * 全由客户端的 {@code ClientBossEntrance} 计算 —— 服务端不必逐 tick 干预，
 * 也就不会和寻路/碰撞打架。</p>
 */
public class BossEntrancePacket {

    private final double x;
    private final double y;
    private final double z;
    /** 出场总时长（tick）。 */
    private final int duration;

    public BossEntrancePacket(Vec3 at, int duration) {
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
        this.duration = duration;
    }

    public Vec3 at() {
        return new Vec3(x, y, z);
    }

    public int duration() {
        return duration;
    }

    public static void encode(BossEntrancePacket p, FriendlyByteBuf buf) {
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeVarInt(p.duration);
    }

    public static BossEntrancePacket decode(FriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        int d = buf.readVarInt();
        return new BossEntrancePacket(new Vec3(x, y, z), d);
    }

    public static void handle(BossEntrancePacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            // 客户端专用类**必须反射引用** —— 直接 import 会让专用服务器崩
            //（项目里踩过一次 DistExecutor 提前链接的坑）。
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                    != net.minecraftforge.api.distmarker.Dist.CLIENT) {
                return;
            }
            try {
                Class.forName("com.hongmeng.xiuxian.client.ClientBossEntrance")
                        .getMethod("begin", Vec3.class, int.class)
                        .invoke(null, p.at(), p.duration());
            } catch (Throwable t) {
                HongmengMod.LOGGER.debug("[hongmeng] boss 出场包处理失败：{}", t.toString());
            }
        });
        c.setPacketHandled(true);
    }
}
