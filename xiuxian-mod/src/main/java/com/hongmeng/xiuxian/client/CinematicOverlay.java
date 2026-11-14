package com.hongmeng.xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>演出黑边 + 标题</b>（过场动画的"电影感"）。
 *
 * <h2>为什么黑边这么重要</h2>
 * <p>灾变那套过场之所以"像电影"，很大一部分不在特效上，而在
 * <b>上下黑边（letterbox）</b>：它一出现，玩家的脑子立刻知道"这不是操作时间，是在看演出"，
 * 于是会认真看画面。这一步几乎零成本，但观感提升很大。</p>
 *
 * <p>标题分两段出现（大标题先、副标题后），并且都带<b>淡入淡出</b>——
 * 直接"啪"地出现会显得很廉价。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CinematicOverlay {

    private CinematicOverlay() {
    }

    /** 黑边高度占屏幕的比例（上下各一条）。 */
    private static final float BAR_RATIO = 0.115F;

    public static void render(GuiGraphics gfx, float partial) {
        if (!Cinematic.active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int w = gfx.guiWidth();
        int h = gfx.guiHeight();

        float p = Cinematic.progress();
        // 黑边：进场 12% 内滑入、最后 12% 滑出
        float bar = Mth.clamp(Math.min(p / 0.12F, (1.0F - p) / 0.12F), 0.0F, 1.0F);
        int barH = (int) (h * BAR_RATIO * bar);
        if (barH > 0) {
            gfx.fill(0, 0, w, barH, 0xFF000000);
            gfx.fill(0, h - barH, w, h, 0xFF000000);
        }

        // 标题：中偏下（电影字幕的常规位置）
        String title = Cinematic.title();
        if (!title.isEmpty()) {
            float a = Mth.clamp(Math.min((p - 0.10F) / 0.10F, (0.82F - p) / 0.12F),
                    0.0F, 1.0F);
            if (a > 0.01F) {
                int alpha = (int) (a * 255.0F) << 24;
                int ty = h - barH - 54;
                gfx.drawCenteredString(mc.font, Component.literal(title),
                        w / 2, ty, alpha | 0xFFFFFF);
            }
        }
        String sub = Cinematic.subtitle();
        if (!sub.isEmpty()) {
            float a = Mth.clamp(Math.min((p - 0.20F) / 0.10F, (0.80F - p) / 0.12F),
                    0.0F, 1.0F);
            if (a > 0.01F) {
                int alpha = (int) (a * 220.0F) << 24;
                int ty = h - barH - 36;
                gfx.drawCenteredString(mc.font, Component.literal(sub),
                        w / 2, ty, alpha | 0xBBBBBB);
            }
        }
    }
}
