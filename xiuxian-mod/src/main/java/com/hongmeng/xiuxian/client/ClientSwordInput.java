package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.ClientSetup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * <b>飞剑剑阵的键盘回传</b>。
 *
 * <h2>键位（学自御剑工艺）</h2>
 * <p>御剑工艺的默认键位是 {@code V} 展开/收回剑阵、{@code Ctrl+R} 切换阵型、
 * {@code H} 释放剑阵术式。本项目已有 {@code R}（修炼面板）等键，
 * 所以这里用<b>不冲突</b>的组合：</p>
 * <table border="1">
 *   <caption>按键</caption>
 *   <tr><th>键</th><th>作用</th></tr>
 *   <tr><td>{@code V}</td><td>展开 / 收回剑阵</td></tr>
 *   <tr><td>{@code Ctrl+R}</td><td>切换阵型（环形 / 扇阵 / 天罡）</td></tr>
 *   <tr><td>{@code Ctrl+H}</td><td>锁定准心目标</td></tr>
 * </table>
 *
 * <h2>为什么用 {@code sendCommand} 而不是新开网络包</h2>
 * <p>项目里 {@code ClientBeastInput} 已经用了这个办法（它自己的注释解释了理由：
 * 用 {@code InputEvent.Key} 的<b>按下沿</b>，一次按键 = 一次请求，
 * 而不是在 {@code ClientTickEvent} 里轮询 {@code isDown()} 把"按住"变成
 * "每 tick 一次请求"）。这里沿用同一套约定，不再新增包类型。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSwordInput {

    private ClientSwordInput() {
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.player.connection == null) {
            return;
        }
        // ⚠ 全部改成**裸键**（不带 Ctrl），从根本上消除与既有键位的冲突。
        //
        //   原来：V=展开（撞法术槽 4）、Ctrl+R=阵型（撞 R 修炼面板）、
        //   Ctrl+H=锁定。带修饰键的写法还有个隐患：
        //   `InputEvent.Key` 早退不会清掉 KeyMapping 的点击队列，
        //   所以 Ctrl+R 照样会把修炼面板打开。
        //
        //   现在：N=展开/收回、M=阵型、L=锁定 —— 三个键当前全空闲。
        if (keyMatches(event, ClientSetup.SWORD_PATTERN_KEY, GLFW.GLFW_KEY_M)) {
            mc.player.connection.sendCommand("hongmeng sword pattern");
            return;
        }
        if (keyMatches(event, ClientSetup.SWORD_LOCK_KEY, GLFW.GLFW_KEY_L)) {
            mc.player.connection.sendCommand("hongmeng sword lock");
            return;
        }
        if (keyMatches(event, ClientSetup.SWORD_FORMATION_KEY, GLFW.GLFW_KEY_N)) {
            mc.player.connection.sendCommand("hongmeng sword toggle");
        }
    }

    /** 按键是否匹配（同时认默认 GLFW 键码与玩家改过的键位映射）。 */
    private static boolean keyMatches(InputEvent.Key event,
                                      net.minecraft.client.KeyMapping mapping,
                                      int defaultKey) {
        return event.getKey() == defaultKey
                || mapping.matches(event.getKey(), event.getScanCode());
    }
}
