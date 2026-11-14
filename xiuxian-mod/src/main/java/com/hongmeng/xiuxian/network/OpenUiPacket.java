package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>服务端 → 客户端：打开某个界面</b>。
 *
 * <h2>为什么需要这个包</h2>
 * <p>副业界面的入口是命令 {@code /hongmeng ui profession}，而命令在服务端执行。
 * 服务端<b>不能直接 import 客户端 Screen 类</b> —— 专用服务器上没有那些类，
 * 会 {@code NoClassDefFoundError}（项目里已经踩过这个坑）。</p>
 *
 * <p>所以走一条单向消息：命令发包 → 客户端收到后
 * 在 {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 里打开界面。
 * 这样服务端<b>完全不引用</b>客户端类。</p>
 *
 * <h2>为什么参数是字符串而不是枚举</h2>
 * <p>将来还要加别的界面（比如"副业界面"之后可能是"阵法图鉴"）。
 * 用字符串可以让客户端忽略不认识的界面 —— 版本不一致时<b>静默降级</b>，
 * 而不是抛异常把玩家踢出去。</p>
 */
public class OpenUiPacket {

    private final String which;

    public OpenUiPacket(String which) {
        this.which = which == null ? "" : which;
    }

    public static void encode(OpenUiPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.which, 32);
    }

    public static OpenUiPacket decode(FriendlyByteBuf buf) {
        return new OpenUiPacket(buf.readUtf(32));
    }

    public static void handle(OpenUiPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hongmeng.xiuxian.client.ClientScreens.open(packet.which)));
        context.setPacketHandled(true);
    }

    /** 服务端调用：让某个玩家打开界面。 */
    public static void open(ServerPlayer player, String which) {
        ModNetwork.sendToPlayer(player, new OpenUiPacket(which));
    }
}
