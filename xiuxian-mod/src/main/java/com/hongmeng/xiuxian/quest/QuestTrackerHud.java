package com.hongmeng.xiuxian.quest;

import com.hongmeng.xiuxian.network.ClientCultivationCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 任务追踪 HUD：屏幕右侧常驻显示"当前该做的任务"。
 *
 * <p>参考任务 mod 的做法：只显示一条正在进行的任务 + 进度，不抢视线。</p>
 */
@OnlyIn(Dist.CLIENT)
public class QuestTrackerHud implements IGuiOverlay {

    private static final int WIDTH = 132;

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics g,
                       float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        QuestProgress progress = ClientCultivationCache.get().getQuestProgress();
        Quest quest = QuestManager.tracked(progress);
        if (quest == null) {
            return;
        }

        int x = screenWidth - WIDTH - 6;
        int y = 6;

        int state = progress.stateOf(quest.id());
        boolean met = quest.requirement().isMet(progress);

        // 底框
        g.fill(x - 3, y - 3, x + WIDTH + 3, y + 40, 0xC0101420);
        g.fill(x - 3, y - 3, x - 1, y + 40, met ? 0xFF6FE07F : 0xFF8A6FD8);

        g.drawString(mc.font, Component.literal("§7▸ 当前任务"), x, y, 0xFFFFFF, true);
        y += 11;

        // 标题（状态色）
        String title = "§f" + quest.title();
        if (state >= 2) {
            title = "§a✔ " + quest.title();
        } else if (met) {
            title = "§e✦ " + quest.title();
        }
        g.drawString(mc.font, Component.literal(title), x, y, 0xFFFFFF, true);
        y += 11;

        // 目标
        g.drawString(mc.font, Component.literal(
                (met ? "§a" : "§7") + quest.requirement().describe(progress).getString()),
                x, y, 0xFFFFFF, true);
        y += 10;

        // 进度条
        int barW = WIDTH - 4;
        float ratio = quest.requirement().ratio(progress);
        g.fill(x, y, x + barW, y + 4, 0xAA0A0E18);
        g.fill(x, y, x + (int) (barW * ratio), y + 4, met ? 0xFF6FE07F : 0xFF66CCFF);
        y += 6;

        if (met) {
            g.drawString(mc.font, Component.literal("§a按 §fJ §a领取奖励"), x, y, 0xFFFFFF, true);
        } else {
            g.drawString(mc.font, Component.literal("§8按 §fJ §8查看全部任务"), x, y, 0xFFFFFF, true);
        }
    }
}
