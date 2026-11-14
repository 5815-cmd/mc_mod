package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.network.ClientCastCache;
import com.hongmeng.xiuxian.technique.Technique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 蓄力条：法术蓄力期间画在**准星下方**，显示法术名与进度。
 *
 * <p>位置在准星正下方（屏幕中心 + 一小段距离），不挡视线、也不用低头找。</p>
 *
 * <h3>为什么画在 {@code RenderGuiEvent.Post}</h3>
 *
 * <p>和 {@link HealthBarOverlay} 同一个理由：Forge 的 {@code ForgeGui} 派发逻辑是
 * {@code if (pre(entry, g)) return; overlay.render(); post(entry, g);}——
 * <b>一旦在某个覆盖层的 Pre 里取消了它，同名覆盖层的 Post 就永远不会触发</b>
 * （原版血条/护甲图标就被 {@link HealthBarOverlay} 取消过，踩过一次这个坑）。
 * {@code RenderGuiEvent.Post} 在所有覆盖层之后必定触发，不受任何取消影响。</p>
 *
 * <p>数据来自 {@link ClientCastCache}（服务端在起手/中断/完成时同步一次），
 * 进度用本地游戏刻补帧，所以不需要每 tick 发包。</p>
 */
@OnlyIn(Dist.CLIENT)
public class CastBarOverlay {

    /** 条宽。 */
    private static final int BAR_WIDTH = 124;
    private static final int BAR_HEIGHT = 8;
    /** 条相对屏幕中心的纵向偏移：十字准星下方。 */
    private static final int TOP_OFFSET = 34;

    private static final int BORDER = 0xFF8A6FD8;
    private static final int BG = 0xFF12060A;
    private static final int TRACK = 0xFF2A1030;

    // PERF(C): 蓄力条只在施法时出现，但施法期间是每帧画的：
    // 法术名与"剩余 0.1s"文字都只随值变化，按值缓存（String.format 每帧都调很浪费）。
    private Technique cachedTechnique;
    private String cachedName = "";
    private int cachedTenths = Integer.MIN_VALUE;
    private String cachedRemaining = "";

    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        Technique technique = ClientCastCache.technique();
        if (technique == null) {
            return;
        }
        float progress = Mth.clamp(ClientCastCache.progress(), 0.0F, 1.0F);

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int left = screenWidth / 2 - BAR_WIDTH / 2;
        int top = screenHeight / 2 + TOP_OFFSET;

        // 法术名（居中，带元素色）
        // PERF(C): 法术对象不变时名字不变，不再每帧调 colored() 重建字符串
        if (cachedTechnique != technique) {
            cachedTechnique = technique;
            cachedName = technique.colored();
        }
        String name = cachedName;
        int nameW = font.width(name);
        g.drawString(font, name, screenWidth / 2 - nameW / 2, top - 11, 0xFFFFFFFF, true);

        // 条本体
        g.fill(left - 1, top - 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, BORDER);
        g.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, BG);
        g.fill(left + 1, top + 1, left + BAR_WIDTH - 1, top + BAR_HEIGHT - 1, TRACK);

        int fillW = (int) ((BAR_WIDTH - 2) * progress);
        if (fillW > 0) {
            // 越接近完成越亮：紫 → 金
            int color = progress > 0.75F ? 0xFFFFD24A
                    : (progress > 0.40F ? 0xFFB07AE8 : 0xFF6F8AE8);
            g.fill(left + 1, top + 1, left + 1 + fillW, top + BAR_HEIGHT - 1, color);
            g.fill(left + 1, top + 1, left + 1 + fillW, top + 3, 0x40FFFFFF);
        }

        // 剩余秒数（条右侧，小字）
        // PERF(C): 只剩 0.1 秒精度，按 0.1s 取整缓存（原来每帧一次 String.format）
        int tenths = Math.round(ClientCastCache.remainingSeconds() * 10.0F);
        if (tenths != cachedTenths) {
            cachedTenths = tenths;
            cachedRemaining = String.format("%.1fs", tenths / 10.0F);
        }
        g.drawString(font, cachedRemaining, left + BAR_WIDTH + 4, top, 0xFFB0B0C0, true);
    }
}
