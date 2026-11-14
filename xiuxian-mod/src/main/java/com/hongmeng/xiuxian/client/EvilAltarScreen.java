package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.menu.EvilAltarMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * <b>恶之祭坛界面</b>：五个凭证槽 + 一个「召唤」按钮。
 *
 * <p>按钮只在五格就绪时可用；点击后客户端发 {@code EvilSummonPacket}，
 * 服务端会<b>重新校验</b>（客户端说能点不算数）。 </p>
 */
public class EvilAltarScreen extends AbstractContainerScreen<EvilAltarMenu> {

    private Button summon;

    public EvilAltarScreen(EvilAltarMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 188;
    }

    @Override
    protected void init() {
        super.init();
        this.summon = this.addRenderableWidget(Button.builder(
                Component.literal("\u00a74\u00a7l召唤"),
                b -> summon()).bounds(this.leftPos + 58, this.topPos + 58, 60, 20)
                .build());
    }

    private void summon() {
        try {
            com.hongmeng.xiuxian.network.ModNetwork.sendToServer(
                    new com.hongmeng.xiuxian.network.EvilSummonPacket(
                            this.menu.altarPos()));
        } catch (Throwable ignored) {
            // 连接未就绪忽略
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        // 深色底 + 血边框
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight, 0xE0100814);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + 2, 0xFF8B1A1A);
        g.fill(this.leftPos, this.topPos + this.imageHeight - 2,
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight,
                0xFF8B1A1A);
        // 槽位凹槽（一排 5 格）
        for (int i = 0; i < com.hongmeng.xiuxian.block.EvilAltarBlockEntity.SLOTS; i++) {
            int x = this.leftPos + 29 + i * 22;
            int y = this.topPos + 29;
            g.fill(x, y, x + 18, y + 18, 0xFF2A1A2E);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF160C18);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);
        // 按钮可用性：五格就绪
        if (this.summon != null) {
            this.summon.active = this.menu.ready();
        }
        g.drawString(this.font, this.menu.ready()
                        ? "\u00a7a五凭证齐备" : "\u00a77需要五个维度的凭证",
                this.leftPos + 8, this.topPos + 84, 0xFFFFFF, false);
    }
}
