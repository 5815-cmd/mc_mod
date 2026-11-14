package com.hongmeng.xiuxian.difficulty;

import com.hongmeng.xiuxian.network.ChooseDifficultyPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 开局难度选择界面。
 *
 * <p>进世界后的第一件事：选难度。难度决定灵根/道宫的抽取概率与保底，以及怪物强度。
 * 选完不能改（要改只能开新档）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class DifficultyScreen extends Screen {

    private static final int W = 330;
    private static final int H = 210;
    private static final int BG = 0xF0101420;
    private static final int BORDER = 0xFF8A6FD8;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int DIM = 0xFF8A90A8;

    private int selected = 0;

    // 按钮命中区
    private final int[] btnX = new int[6];
    private final int[] btnY = new int[6];
    private int btnW;
    private int btnH;
    private int enterX;
    private int enterY;
    private int enterW;
    private int enterH;

    public DifficultyScreen() {
        super(Component.literal("选择难度"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, BG);

        g.drawString(font, "§6§l【鸿蒙修仙传】§f 选择难度", left + 10, top + 8, TEXT, false);
        g.fill(left, top + 20, left + W, top + 21, 0x668A6FD8);
        g.drawString(font, "§7难度越高，灵根与道宫越好，但怪物也越强。§c选定后不可更改。",
                left + 10, top + 25, DIM, false);

        CultivationDifficulty[] all = CultivationDifficulty.values();

        // ---- 左侧：难度列表 ----
        int lx = left + 10;
        int ly = top + 40;
        btnW = 96;
        btnH = 24;
        for (int i = 0; i < all.length; i++) {
            CultivationDifficulty d = all[i];
            int by = ly + i * (btnH + 3);
            btnX[i] = lx;
            btnY[i] = by;
            boolean isSel = i == selected;
            boolean hover = mouseX >= lx && mouseX <= lx + btnW
                    && mouseY >= by && mouseY <= by + btnH;
            int bg = isSel ? 0xFF3A2A5A : (hover ? 0xFF2A2438 : 0xFF1A1826);
            g.fill(lx, by, lx + btnW, by + btnH, bg);
            g.renderOutline(lx, by, btnW, btnH, isSel ? d.rgb() : 0xFF40404A);
            g.drawString(font, (isSel ? "§l" : "") + d.colored(), lx + 8, by + 4, TEXT, false);
            g.drawString(font, "§8怪 ×" + String.format("%.2f", d.mobHealthMultiplier()),
                    lx + 8, by + 14, DIM, false);
        }

        // ---- 右侧：详情 ----
        int rx = lx + btnW + 12;
        int ry = top + 40;
        int rw = W - (rx - left) - 12;
        CultivationDifficulty sel = all[selected];

        g.drawString(font, "§f§l" + sel.cn(), rx, ry, TEXT, false);
        ry += 12;
        for (FormattedCharSequence line : font.split(
                Component.literal("§7" + sel.desc()), rw)) {
            g.drawString(font, line, rx, ry, TEXT, false);
            ry += 10;
        }
        ry += 6;

        g.drawString(font, "§b灵根概率", rx, ry, TEXT, false);
        ry += 11;
        for (FormattedCharSequence line : font.split(
                Component.literal("§f" + sel.rootWeightText()), rw)) {
            g.drawString(font, line, rx, ry, TEXT, false);
            ry += 10;
        }
        g.drawString(font, "§7保底：§e" + sel.rootFloorName(), rx, ry, TEXT, false);
        ry += 13;

        g.drawString(font, "§b道宫保底", rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, "§7最低品质：§e" + sel.domainFloorName(), rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, "§7（100 次保底仙品、500 次保底鸿蒙，与难度无关）", rx, ry, DIM, false);
        ry += 13;

        g.drawString(font, "§b怪物强度", rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, "§7生命 §c×" + String.format("%.2f", sel.mobHealthMultiplier())
                        + "   §7攻击 §c×" + String.format("%.2f", sel.mobDamageMultiplier()),
                rx, ry, TEXT, false);
        ry += 14;

        if (sel.isMowingTier()) {
            g.drawString(font, "§d★ 混沌及以上：道印之后开始质变，可以割草。", rx, ry, TEXT, false);
        } else {
            g.drawString(font, "§8此难度下抽不到混沌/鸿蒙灵根。", rx, ry, DIM, false);
        }

        // ---- 确认按钮 ----
        enterW = 120;
        enterH = 18;
        enterX = left + (W - enterW) / 2;
        enterY = top + H - 26;
        boolean hoverEnter = mouseX >= enterX && mouseX <= enterX + enterW
                && mouseY >= enterY && mouseY <= enterY + enterH;
        g.fill(enterX, enterY, enterX + enterW, enterY + enterH,
                hoverEnter ? 0xFF3E7A4E : 0xFF2A5A38);
        g.renderOutline(enterX, enterY, enterW, enterH, 0xFF6FE07F);
        String label = "确认：以「" + sel.cn() + "」开始";
        g.drawString(font, label, enterX + (enterW - font.width(label)) / 2,
                enterY + 5, TEXT, false);

        g.drawString(font, "§8↑↓ 切换 · 回车确认", left + 10, top + H - 12, DIM, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        CultivationDifficulty[] all = CultivationDifficulty.values();
        for (int i = 0; i < all.length; i++) {
            if (mouseX >= btnX[i] && mouseX <= btnX[i] + btnW
                    && mouseY >= btnY[i] && mouseY <= btnY[i] + btnH) {
                selected = i;
                return true;
            }
        }
        if (mouseX >= enterX && mouseX <= enterX + enterW
                && mouseY >= enterY && mouseY <= enterY + enterH) {
            confirm();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        CultivationDifficulty[] all = CultivationDifficulty.values();
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP) {
            selected = (selected - 1 + all.length) % all.length;
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
            selected = (selected + 1) % all.length;
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        ChooseDifficultyPacket.send(CultivationDifficulty.values()[selected].name());
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
