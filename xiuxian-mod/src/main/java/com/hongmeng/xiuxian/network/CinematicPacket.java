package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.client.Cinematic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>演出镜头包</b>（服务端 → 客户端）。
 *
 * <h2>为什么走网络而不是纯客户端演出</h2>
 * <p>灾变的过场模组踩过一个坑：集成包若不把过场实体加进 tick 剔除白名单，
 * 就会出现「<b>服务器与客户端之间严重不同步，过场动画无法正常运行</b>」。
 * 根因是<b>演出状态只在客户端</b>，服务端不知道演到哪了。</p>
 *
 * <p>所以这里反过来：<b>服务端算好整段镜头</b>（关键帧数组）一次发给客户端，
 * 客户端只负责插值与呈现。演出结束也由服务端发 {@code end} 收尾，
 * 客户端另有超时兜底。</p>
 *
 * <h2>为什么"只有生存模式被传送"这条也照做</h2>
 * <p>创造/旁观玩家多半在造东西或者旁观，被强行拖进演出很烦。
 * 这个包只影响<b>收到包的玩家自己</b>的镜头，不动任何实体 ——
 * 所以不会与其它模组的实体 tick 剔除冲突。</p>
 */
public class CinematicPacket {

    /** 关键帧展平成的一维数组，每 5 个一组：(dist, yawDeg, pitchDeg, lookY, fov)。 */
    private final double ax;
    private final double ay;
    private final double az;
    private final float[] shots;
    private final int ticks;
    private final String title;
    private final String subtitle;
    private final boolean stop;

    private CinematicPacket(boolean stop, Vec3 anchor, float[] shots, int ticks,
                           String title, String subtitle) {
        this.stop = stop;
        this.ax = anchor.x;
        this.ay = anchor.y;
        this.az = anchor.z;
        this.shots = shots;
        this.ticks = ticks;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
    }

    public static CinematicPacket play(Vec3 anchor, float[] shots, int ticks,
                                       String title, String subtitle) {
        return new CinematicPacket(false, anchor, shots, ticks, title, subtitle);
    }

    public static CinematicPacket stop() {
        return new CinematicPacket(true, Vec3.ZERO, new float[0], 0, "", "");
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.stop);
        buf.writeDouble(this.ax);
        buf.writeDouble(this.ay);
        buf.writeDouble(this.az);
        buf.writeVarInt(this.shots.length);
        for (float f : this.shots) {
            buf.writeFloat(f);
        }
        buf.writeVarInt(this.ticks);
        buf.writeUtf(this.title, 64);
        buf.writeUtf(this.subtitle, 128);
    }

    public static CinematicPacket decode(FriendlyByteBuf buf) {
        boolean stop = buf.readBoolean();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        int n = Math.min(4096, buf.readVarInt());      // 防御：别让坏包把内存打爆
        float[] shots = new float[n];
        for (int i = 0; i < n; i++) {
            shots[i] = buf.readFloat();
        }
        int ticks = buf.readVarInt();
        String title = buf.readUtf(64);
        String sub = buf.readUtf(128);
        return new CinematicPacket(stop, new Vec3(x, y, z), shots, ticks, title, sub);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        // 客户端类必须**反射式**碰 —— 专服上不能直接引用 client 包
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.stop) {
                Cinematic.end();
            } else {
                Cinematic.begin(new Vec3(this.ax, this.ay, this.az), this.shots,
                        this.ticks, this.title, this.subtitle);
            }
        });
        c.setPacketHandled(true);
    }
}
