package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.combat.CombatFeel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 强者登场演出（{@code docs/combat-feel-spec.md} §3.2）：<b>天空短暂变暗 + 名牌从上方压下</b>。
 *
 * <h2>为什么不做成"移动原版 BOSS 血条"</h2>
 *
 * <p>规格写的是"BOSS 血条从屏幕上方压下"，而原版 {@code ServerBossEvent} 的条
 * 是原版 HUD 画的、位置写死在顶部——想让它"压下来"只能整个接管那一条的渲染，
 * 代价是丢掉原版的所有行为（堆叠、淡入淡出、N 条共存）。</p>
 *
 * <p>所以这里做的是<b>同一时刻、同一位置的横幅</b>：在血条正上方压下一块
 * 品阶色的名牌（{@code §5头目 §7虚空之主}），12 tick 内从屏幕外砸到位、
 * 停 3 秒、淡出。视觉上就是"血条砸下来了"，而血条本身仍然是原版那条——
 * 两者的出现时刻是同一个 tick（同一包触发 + 原版血条在实体被追踪时就挂上了）。</p>
 *
 * <p>变暗用整屏压暗（{@link CombatFeel#skyDarkenAlpha(int, int)}：<b>快进慢出</b>），
 * 不动天空渲染器——那样要碰 {@code ViewportEvent.RenderFog}，收益不值那个复杂度。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class BossEntranceOverlay {

    /** 横幅宽度（像素）。 */
    private static final int BANNER_WIDTH = 240;
    private static final int BANNER_HEIGHT = 20;
    /** 砸到位之后停在屏幕上方多少像素。 */
    private static final int REST_Y = 6;

    private static int darkenElapsed = -1;
    private static int bannerElapsed = -1;
    private static String label = "";
    private static int color = 0xFF3B30;

    private BossEntranceOverlay() {
    }

    /** 收到"强者登场"包。 */
    public static void trigger(String bossLabel, int auraColor) {
        if (!enabled()) {
            return;
        }
        label = bossLabel == null ? "" : bossLabel;
        color = auraColor == 0 ? 0xFF3B30 : auraColor;
        darkenElapsed = 0;
        bannerElapsed = 0;
    }

    /** 特效开关（客户端配置）。 */
    public static boolean enabled() {
        try {
            return com.hongmeng.xiuxian.compat.HongmengConfig.CLIENT.bossEntrance.get();
        } catch (Throwable t) {
            return true;
        }
    }

    public static void tick() {
        if (darkenElapsed >= 0) {
            darkenElapsed++;
            if (darkenElapsed >= CombatFeel.ENTRANCE_DARKEN_TICKS) {
                darkenElapsed = -1;
            }
        }
        if (bannerElapsed >= 0) {
            bannerElapsed++;
            if (bannerElapsed >= CombatFeel.ENTRANCE_BANNER_TICKS) {
                bannerElapsed = -1;
            }
        }
    }

    public static void clear() {
        darkenElapsed = -1;
        bannerElapsed = -1;
        label = "";
    }

    /** 有没有在演出（诊断）。 */
    public static boolean active() {
        return darkenElapsed >= 0 || bannerElapsed >= 0;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    /** 由 {@code RenderGuiEvent.Post} 调。 */
    public static void render(GuiGraphics graphics, int width, int height) {
        if (darkenElapsed >= 0) {
            float alpha = CombatFeel.skyDarkenAlpha(darkenElapsed,
                    CombatFeel.ENTRANCE_DARKEN_TICKS);
            int a = (int) (alpha * 255.0F);
            if (a > 2) {
                // 紫黑压暗：像天被一只手盖住
                graphics.fill(0, 0, width, height, (a << 24) | 0x0A0410);
            }
        }
        if (bannerElapsed >= 0) {
            renderBanner(graphics, width);
        }
    }

    /** 从屏幕上方砸下来的名牌。 */
    private static void renderBanner(GuiGraphics graphics, int width) {
        Font font = Minecraft.getInstance().font;
        float y = CombatFeel.bannerY(bannerElapsed, CombatFeel.ENTRANCE_SLAM_TICKS,
                -BANNER_HEIGHT - 2.0F, REST_Y);
        int x = (width - BANNER_WIDTH) / 2;
        int top = Math.round(y);

        // 淡出：最后 20 tick 渐隐
        int life = CombatFeel.ENTRANCE_BANNER_TICKS;
        float fade = bannerElapsed > life - 20
                ? Math.max(0.0F, (life - bannerElapsed) / 20.0F) : 1.0F;
        int bgAlpha = (int) (0.78F * fade * 255.0F);
        int lineAlpha = (int) (fade * 255.0F);

        graphics.fill(x, top, x + BANNER_WIDTH, top + BANNER_HEIGHT,
                (bgAlpha << 24) | 0x0A0A14);
        // 品阶色描边 + 下缘一条亮线（"砸下来"的落点）
        graphics.renderOutline(x, top, BANNER_WIDTH, BANNER_HEIGHT,
                (lineAlpha << 24) | (color & 0xFFFFFF));
        graphics.fill(x + 2, top + BANNER_HEIGHT - 2, x + BANNER_WIDTH - 2, top + BANNER_HEIGHT - 1,
                (lineAlpha << 24) | (color & 0xFFFFFF));
        graphics.fill(x + 2, top + 1, x + BANNER_WIDTH - 2, top + 2,
                (lineAlpha / 3 << 24) | (color & 0xFFFFFF));

        // 名牌文字：§ 颜色码直接写在字符串里（Font 会解析），所以"§5头目 §7虚空之主"
        // 的品阶色与名字色都是原样保留的
        String text = label;
        int textColor = 0xFFFFFFFF;
        if (text.isEmpty()) {
            text = "§4强者降临";
        }
        int textX = x + (BANNER_WIDTH - font.width(text)) / 2;
        int textY = top + (BANNER_HEIGHT - font.lineHeight) / 2 + 1;
        graphics.drawString(font, Component.literal(text), textX + 1, textY + 1,
                (int) (fade * 0.7F * 255.0F) << 24, false);
        graphics.drawString(font, Component.literal(text), textX, textY,
                ((int) (fade * 255.0F) << 24) | (textColor & 0xFFFFFF), false);

        // 砸到位的那几 tick 加一条横向"冲击线"
        if (bannerElapsed >= CombatFeel.ENTRANCE_SLAM_TICKS
                && bannerElapsed < CombatFeel.ENTRANCE_SLAM_TICKS + 4) {
            int spark = (int) (fade * 90.0F) << 24 | (color & 0xFFFFFF);
            graphics.fill(0, top + BANNER_HEIGHT, width, top + BANNER_HEIGHT + 1, spark);
        }
    }
}
