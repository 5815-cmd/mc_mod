package com.hongmeng.xiuxian.alchemy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 丹炉界面。
 *
 * <p><b>刻意不画贴图</b>：整张界面用 {@link GuiGraphics#fill} 的色块拼出来。
 * 原因很实在——GUI 贴图一旦尺寸和 176×166 对不上，或者 PNG 没落盘，
 * 客户端渲染直接崩；而这个界面只有四个槽 + 一根进度条，
 * 色块反而更好维护（也方便按品阶改配色）。</p>
 */
public class AlchemyFurnaceScreen extends AbstractContainerScreen<AlchemyFurnaceMenu> {

    private static final int PANEL = 0xFF15121D;
    private static final int BORDER = 0xFF4A3F63;
    private static final int SLOT_BG = 0xFF2A2438;
    private static final int SLOT_EDGE = 0xFF3E3556;
    private static final int ARROW_BG = 0xFF2A2438;
    private static final int ARROW_FILL = 0xFFE8B44A;
    private static final int FUEL_FILL = 0xFFFF7A22;
    private static final int TEXT_TITLE = 0xFFE8D9A0;
    private static final int TEXT_HINT = 0xFF9A8FBF;

    public AlchemyFurnaceScreen(AlchemyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x, y, x + imageWidth, y + 2, BORDER);
        graphics.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, BORDER);
        graphics.fill(x, y, x + 2, y + imageHeight, BORDER);
        graphics.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, BORDER);

        for (int[] slot : AlchemyFurnaceMenu.SLOT_POS) {
            slotFrame(graphics, x + slot[0], y + slot[1]);
        }

        // 进度条：主料/辅料 -> 产物 之间那条
        int barX = x + 72;
        int barY = y + 36;
        int barW = 36;
        graphics.fill(barX, barY, barX + barW, barY + 10, ARROW_BG);
        int progress = menu.progress();
        int total = menu.brewTotal();
        if (progress > 0) {
            int filled = Math.max(1, barW * Math.min(progress, total) / total);
            graphics.fill(barX, barY, barX + filled, barY + 10, ARROW_FILL);
        }
        graphics.fill(barX + barW, barY + 2, barX + barW + 4, barY + 8, ARROW_FILL);

        // 燃料条：炉子左边那一竖条
        int fuelX = x + 30;
        int fuelY = y + 63;
        int fuelH = 16;
        graphics.fill(fuelX, fuelY, fuelX + 8, fuelY + fuelH, ARROW_BG);
        int fuel = menu.fuel();
        int fuelMax = menu.fuelTotal();
        if (fuel > 0) {
            int filled = Math.max(1, fuelH * Math.min(fuel, fuelMax) / fuelMax);
            graphics.fill(fuelX, fuelY + fuelH - filled, fuelX + 8, fuelY + fuelH, FUEL_FILL);
        }
    }

    private void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        graphics.fill(x, y, x + 16, y + 16, SLOT_BG);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 面板是深色底，标签必须自己上浅色，不能用 AbstractContainerScreen 的默认深灰
        graphics.drawString(this.font, this.title, 8, 5, TEXT_TITLE, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, TEXT_HINT, false);
        graphics.drawString(this.font, Component.translatable("gui.hongmeng.alchemy.hint"),
                72, 60, TEXT_HINT, false);
    }

    /** 空格子给一句说明——不然玩家不知道哪个格子放什么。 */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < AlchemyFurnaceMenu.SLOT_COUNT; i++) {
            int[] pos = AlchemyFurnaceMenu.SLOT_POS[i];
            if (menu.getSlot(i).hasItem()) {
                continue;
            }
            if (isHovering(pos[0], pos[1], 16, 16, mouseX, mouseY)) {
                graphics.renderTooltip(this.font,
                        Component.translatable("gui.hongmeng.alchemy.slot." + i),
                        mouseX, mouseY);
            }
        }
        AlchemyRecipes.Brew brew = menu.previewBrew();
        if (brew != null && isHovering(110, 30, 22, 22, mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    new net.minecraft.world.item.ItemStack(
                            AlchemyFurnaceBlockEntity.resolveItem(brew.result())).getHoverName(),
                    mouseX, mouseY);
        }
    }
}
