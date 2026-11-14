package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.CultivationPanelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * R 面板（修行面板）里的<b>「屏幕震动」开关按钮</b>。
 *
 * <h2>为什么这个按钮画在这里，而不是写进 {@code CultivationPanelScreen}</h2>
 *
 * <p>那是另一个代理正在改的文件（领域/灵兽页签都在里面）。规格要求"震动必须能关，
 * 这是无障碍问题不是可选项"，而本模组对"往别人的界面里加东西"早就有既成做法：
 * 法宝装备栏就是这么挂进原版背包界面的（{@code ScreenEvent.Render.Post} +
 * {@code ScreenEvent.MouseButtonPressed.Pre}，见 {@code item/gear/EquipmentPanel}）。</p>
 *
 * <p>所以这里照抄那条路：<b>零改动</b>地把按钮画进 R 面板，点击事件也自己吃掉。
 * 唯一的前提是位置——放在标题行里"鸿蒙修仙"与"退出键：无"之间的空白处，
 * 两块文字之间的这段空隙本来就是空的，不会盖住任何既有内容。</p>
 *
 * <h2>三个入口都给到</h2>
 *
 * <ol>
 *   <li><b>R 面板上的这个按钮</b>（最顺手，玩家一定会打开这个界面）；</li>
 *   <li><b>配置文件</b> {@code config/hongmeng-client.toml} 的 {@code screenShake}——
 *       进游戏之前就能改（晕 3D 的玩家不该被迫先挨一次震动才能关）；</li>
 *   <li>Forge 的"模组 → 鸿蒙修仙 → 配置 → 客户端"界面（Forge 自带，不用我们写）。</li>
 * </ol>
 */
@OnlyIn(Dist.CLIENT)
public final class CombatFeelPanel {

    /** 按钮尺寸与偏移（面板坐标系）。 */
    private static final int BTN_W = 76;
    private static final int BTN_H = 11;
    private static final int BTN_OFFSET_X = 72;
    private static final int BTN_OFFSET_Y = 8;

    /**
     * 面板尺寸——<b>必须与 {@code CultivationPanelScreen} 的常量保持一致</b>
     * （那边现在是 296 × 300；页签行在 top+20，所以按钮放在 title 行 top+8 不会撞上）。
     *
     * <p>不一致的后果只是"按钮位置偏一点"，不会崩也不会挡住别的控件：
     * 位置由屏幕中心推出，面板变宽时按钮仍是画在面板内的。</p>
     */
    private static final int PANEL_W = 296;
    private static final int PANEL_H = 300;

    /** 悬停提示画在页签行下面（页签行在 top+20 ~ top+34）。 */
    private static final int HINT_OFFSET_Y = 36;

    /** 每帧更新的命中区域。 */
    private static int btnX;
    private static int btnY;
    private static boolean visible = false;

    private CombatFeelPanel() {
    }

    /** 由 {@code ScreenEvent.Render.Post} 调。 */
    public static void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        visible = false;
        if (!(screen instanceof CultivationPanelScreen)) {
            return;
        }
        int left = (screen.width - PANEL_W) / 2;
        int top = (screen.height - PANEL_H) / 2;
        btnX = left + BTN_OFFSET_X;
        btnY = top + BTN_OFFSET_Y;
        visible = true;

        boolean forced = CombatShake.forcedDisabled();
        boolean on = !forced && CombatShake.enabled();
        boolean hover = hovered(mouseX, mouseY);
        int bg = on ? (hover ? 0xFF2E5A3A : 0xFF24402C) : (hover ? 0xFF5A2E2E : 0xFF3A2424);
        graphics.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, bg);
        graphics.renderOutline(btnX, btnY, BTN_W, BTN_H, hover ? 0xFFFFD24A : 0xFF8A6FD8);

        Font font = Minecraft.getInstance().font;
        String text = forced ? "§7震动 §c已强制关闭"
                : "§7震动 " + (on ? "§a开" : "§c关");
        graphics.drawString(font, Component.literal(text),
                btnX + (BTN_W - font.width(text)) / 2, btnY + 2, 0xFFE8E8F0, false);

        if (hover) {
            String hint = forced
                    ? "§8项目已全局关闭屏幕震动（代码硬开关，面板不可开启）"
                    : "§8点击切换屏幕震动 §7（无障碍选项：会晕 3D 就关掉）";
            graphics.drawString(font, Component.literal(hint), btnX, top + HINT_OFFSET_Y,
                    0xFFE8E8F0, false);
        }
    }

    /**
     * 由 {@code ScreenEvent.MouseButtonPressed.Pre} 调。
     *
     * @return true 表示这一下点击被本按钮吃掉了
     */
    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (button != 0 || !visible || !(screen instanceof CultivationPanelScreen)) {
            return false;
        }
        if (!hovered(mouseX, mouseY)) {
            return false;
        }
        if (CombatShake.forcedDisabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "§c屏幕震动已全局关闭（代码硬开关）"), true);
            }
            return true;
        }
        boolean now = CombatShake.toggle();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(now
                    ? "§a屏幕震动：已开启 §7（强度可在配置里调）"
                    : "§c屏幕震动：已关闭 §7（已写回 hongmeng-client.toml）"), true);
        }
        return true;
    }

    private static boolean hovered(double mouseX, double mouseY) {
        return mouseX >= btnX && mouseX <= btnX + BTN_W
                && mouseY >= btnY && mouseY <= btnY + BTN_H;
    }

    /** 按钮中心（诊断/测试用）。 */
    public static int buttonX() {
        return btnX;
    }

    /** 按钮纵向位置。 */
    public static int buttonY() {
        return btnY;
    }
}
