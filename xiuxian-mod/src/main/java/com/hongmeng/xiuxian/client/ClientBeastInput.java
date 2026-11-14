package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.beast.BeastRiding;
import com.hongmeng.xiuxian.entity.beast.SpiritBeastEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 骑乘时的键盘输入回传。
 *
 * <p>只做一件事：骑着灵兽按<b>空格</b>时，把"跳/上升"这一个事件发给服务端
 * （走 {@code /hmbeast jump}，理由见 {@code BeastCommand} 的类注释）。</p>
 *
 * <p>为什么不在 {@code ClientTickEvent} 里轮询 {@code mc.options.keyJump.isDown()}：
 * 轮询会把"按住"变成"每 tick 一次请求"，于是飞行坐骑会一路狂升、
 * 空中坐骑会把二段跳瞬间用光。用 {@link InputEvent.Key} 的按下沿，
 * 一次按键 = 一次请求，语义才对。</p>
 *
 * <p>不用 {@code KeyMapping} 而是直接看 GLFW 键码是刻意的：
 * 空格的 {@code KeyMapping} 是原版的 {@code key.jump}，玩家可能改键，
 * 这里只关心"他按了跳跃键吗"，所以读原版映射本身就够了——
 * 下面用 {@code mc.options.keyJump.isDown()} 做二次确认，
 * 这样改过键的玩家也照样生效。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientBeastInput {

    /** 公开构造：Forge 事件总线要求实例注册。 */
    public ClientBeastInput() {
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        // 骑乘系统 2026-11-07 起关闭：不再发送任何骑乘输入。
        if (!BeastRiding.ENABLED) {
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.player.connection == null) {
            return;
        }
        // 必须在灵兽背上
        if (!(mc.player.getVehicle() instanceof SpiritBeastEntity beast)) {
            return;
        }
        if (!beast.kind().flying()) {
            return;   // 地面坐骑的二段跳由原版跳跃自然触发，不需要回传
        }
        // 只认"跳跃键"（默认空格，允许玩家改键）
        boolean jumpKey = event.getKey() == GLFW.GLFW_KEY_SPACE
                || mc.options.keyJump.matches(event.getKey(), event.getScanCode());
        if (!jumpKey) {
            return;
        }
        mc.player.connection.sendCommand("hmbeast jump");
    }
}
