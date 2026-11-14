package com.hongmeng.xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>客户端界面的统一入口</b>。
 *
 * <h2>为什么需要这一层</h2>
 * <p>服务端命令（{@code /hongmeng ui profession}）要能打开客户端界面，
 * 但服务端<b>不能直接 import 客户端类</b>（专用服务器上没有它们）。
 * 所以走一条"服务端 → 客户端"的单向消息：命令发 {@code OpenUiPacket}，
 * 客户端收到后调用这里。</p>
 *
 * <h2>⚠ 为什么用 {@code DistExecutor} 而不是直接 new Screen</h2>
 * <p>项目里已经吃过这个教训：专用服务器加载到客户端类会
 * {@code NoClassDefFoundError}。所以客户端类只在这个 {@code @OnlyIn(CLIENT)}
 * 的类里出现，服务端那边通过 {@code OpenUiPacket} 的
 * {@code DistExecutor.unsafeRunWhenOn} 调用。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientScreens {

    private ClientScreens() {
    }

    /**
     * 按名字打开界面。
     *
     * @param which {@code profession} = 副业界面；未知名字忽略
     */
    public static void open(String which) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        switch (which) {
            case "profession" -> mc.setScreen(new ProfessionScreen());
            default -> {
                // 未知名字：静默忽略（服务端与客户端版本不一致时不该崩）
            }
        }
    }

    /** 掉线时关掉我们自己的界面，避免回到主菜单后仍挂着。 */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ProfessionScreen) {
            mc.setScreen(null);
        }
    }
}
