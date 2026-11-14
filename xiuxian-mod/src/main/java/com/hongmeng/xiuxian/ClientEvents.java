package com.hongmeng.xiuxian;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * "没有退出键"的落地实现。
 *
 * <p>进入暂停菜单时，原版的"保存并退出"会被换成需要长按 1.5 秒的
 * "断开意识"。毕竟这是个真游戏，不能真把人锁死在里面——
 * 但这个门槛足够让你在按之前犹豫一下。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ClientEvents {

    private static final long HOLD_MS = 1500L;

    private static final List<Button> MANAGED = new ArrayList<>();
    private static long holdStart = 0L;

    /** 换暂停界面时旧的按钮引用就作废了。 */
    private static net.minecraft.client.gui.screens.Screen lastScreen = null;

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) {
            return;
        }
        if (lastScreen != event.getScreen()) {
            MANAGED.clear();
            holdStart = 0L;
            lastScreen = event.getScreen();
        }
        if (!HongmengConfig.SERVER.hideExitButton.get()) {
            return;
        }
        for (var listener : event.getListenersList()) {
            if (listener instanceof Button button && isExitButton(button)) {
                MANAGED.add(button);
                button.setMessage(Component.literal("§8断开意识 §7(长按 1.5 秒)"));
            }
        }
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        // 法宝装备栏：画在背包界面里
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inv) {
            com.hongmeng.xiuxian.item.gear.EquipmentPanel.render(
                    inv, event.getGuiGraphics(), (int) event.getMouseX(), (int) event.getMouseY());
            return;
        }

        if (!(event.getScreen() instanceof PauseScreen) || MANAGED.isEmpty()) {
            return;
        }
        if (!HongmengConfig.SERVER.hideExitButton.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        boolean mouseDown = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        for (Button button : MANAGED) {
            boolean hovering = button.isMouseOver(mouseX, mouseY);
            if (!(hovering && mouseDown)) {
                continue;
            }
            long now = System.currentTimeMillis();
            if (holdStart == 0L) {
                holdStart = now;
            }
            float progress = Math.min(1.0F, (now - holdStart) / (float) HOLD_MS);
            int barY = button.getY() + button.getHeight() - 2;
            event.getGuiGraphics().fill(button.getX(), barY,
                    button.getX() + (int) (button.getWidth() * progress), barY + 2,
                    progress >= 1.0F ? 0xFF66FF99 : 0xFFFF6666);
        }
    }

    @SubscribeEvent
    public void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        // 法宝装备栏优先吃点击
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inv
                && com.hongmeng.xiuxian.item.gear.EquipmentPanel.mouseClicked(
                inv, event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }

        if (!(event.getScreen() instanceof PauseScreen) || MANAGED.isEmpty()) {
            return;
        }
        if (!HongmengConfig.SERVER.hideExitButton.get()) {
            return;
        }
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        for (Button button : MANAGED) {
            if (!button.isMouseOver(event.getMouseX(), event.getMouseY())) {
                continue;
            }
            long now = System.currentTimeMillis();
            boolean longEnough = holdStart != 0L && now - holdStart >= HOLD_MS;
            holdStart = now;
            if (longEnough) {
                // 放行原版逻辑：真的断开
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal("§c没有退出键。真要断开，就按住别松。"), true);
            }
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public void onScreenMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        holdStart = 0L;
    }

    private boolean isExitButton(Button button) {
        String text = button.getMessage().getString();
        return text.contains("保存并退出")
                || text.contains("Save and Quit")
                || text.contains("退出游戏")
                || text.contains("Quit to Title")
                || text.contains("返回标题")
                || text.contains("断开连接")
                || text.contains("Disconnect");
    }
}
