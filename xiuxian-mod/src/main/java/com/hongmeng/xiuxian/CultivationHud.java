package com.hongmeng.xiuxian;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.core.Karma;
import com.hongmeng.xiuxian.core.Realm;
import com.hongmeng.xiuxian.network.ClientBreakthroughFx;
import com.hongmeng.xiuxian.network.ClientCultivationCache;
import com.hongmeng.xiuxian.network.ClientPainState;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.spirit.SpiritRoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 屏幕上的修为 HUD：境界、修为进度条、业力、打坐状态。
 * 位置固定在左上角，不挡视野。
 */
@OnlyIn(Dist.CLIENT)
public class CultivationHud implements IGuiOverlay {

    private static final int X = 6;
    private static final int Y = 6;
    private static final int BAR_WIDTH = 96;
    private static final int BAR_HEIGHT = 6;

    // ------------------------------------------------------------------
    // PERF(C): HUD 文字缓存
    //
    // 这个 HUD 常驻屏幕，每帧都会重建 3~5 个 Component 与若干拼接字符串；
    // 而其中的数值大多在几十帧里都不变（修为进度、境界、业力）。
    // 这里按"值是否变化"缓存，命中时完全不分配。
    // 注意：业力那行还带"突破失败率"，它受门派加成/配置影响，
    // 所以即使业力数值没变也每 20 tick 重算一次。
    // ------------------------------------------------------------------
    private SpiritRoot cachedRoot;
    private Component rootLine;
    private Realm cachedRealm;
    private long cachedProgress = Long.MIN_VALUE;
    private long cachedNeeded = Long.MIN_VALUE;
    private Component realmLine;
    private int cachedKarma = Integer.MIN_VALUE;
    private int karmaRefreshedTick = Integer.MIN_VALUE;
    private Component karmaLine;
    /** 固定文字只建一次。 */
    private static final Component MEDITATING_LINE = Component.literal("\u00a7b吐纳中…");

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics,
                       float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        CultivationData data = ClientCultivationCache.get();
        if (!data.isAwakened()) {
            return;
        }

        SpiritRoot root = data.getRoot();
        Realm realm = data.realm();

        int y = Y;

        // 第一行：品质 + 灵根名 + 境界
        if (rootLine == null || cachedRoot != root) {
            cachedRoot = root;
            String rootText = root == null ? "未觉醒"
                    : root.rarity().colored() + " " + root.cn();
            rootLine = Component.literal("\u00a7f" + rootText);
        }
        graphics.drawString(mc.font, rootLine, X, y, 0xFFFFFF, true);
        y += 10;

        long progress = data.progress();
        long needed = data.needed();
        if (realmLine == null || cachedRealm != realm
                || cachedProgress != progress || cachedNeeded != needed) {
            cachedRealm = realm;
            cachedProgress = progress;
            cachedNeeded = needed;
            realmLine = Component.literal("\u00a7e" + realm.cn() + " \u00a77| \u00a7f修为 "
                    + progress + "\u00a77/\u00a7f" + needed);
        }
        graphics.drawString(mc.font, realmLine, X, y, 0xFFFFFF, true);
        y += 11;

        // 进度条
        long need = Math.max(1L, needed);
        float ratio = Math.min(1.0F, (float) progress / (float) need);
        graphics.fill(X, y, X + BAR_WIDTH, y + BAR_HEIGHT, 0xAA101018);
        int fillColor = root == null ? 0xFF66CCFF : (0xFF000000 | (root.rarity().rgb() & 0xFFFFFF));
        graphics.fill(X + 1, y + 1, X + 1 + (int) ((BAR_WIDTH - 2) * ratio), y + BAR_HEIGHT - 1, fillColor);
        graphics.renderOutline(X, y, BAR_WIDTH, BAR_HEIGHT, 0xFF8A6FD8);
        y += BAR_HEIGHT + 3;

        // 业力
        int karma = data.getKarma();
        if (karma > 0) {
            // PERF(C): 失败率会受门派/配置影响，所以每 20 tick 也强制重算一次
            int tickNow = mc.player.tickCount;
            if (karmaLine == null || cachedKarma != karma
                    || tickNow - karmaRefreshedTick >= 20) {
                cachedKarma = karma;
                karmaRefreshedTick = tickNow;
                karmaLine = Component.literal(
                        "\u00a7c业力 " + karma + " \u00a77(突破失败率 "
                                + Karma.failurePercentText(data) + ")");
            }
            graphics.drawString(mc.font, karmaLine, X, y, 0xFFFFFF, true);
            y += 10;
        }

        // 打坐中
        if (mc.player.getPersistentData().getBoolean("hongmeng_meditating")) {
            float pulse = 0.6F + 0.4F * (float) Math.sin(System.currentTimeMillis() / 300.0D);
            int color = ((int) (pulse * 255) << 24) | 0x66CCFF;
            graphics.drawString(mc.font, MEDITATING_LINE, X, y, 0xFFFFFF, true);
            graphics.fill(X, y + 9, X + 40, y + 11, color);
        }

        renderPathMechanics(graphics, mc, y);

        renderPain(graphics, screenWidth, screenHeight);
        renderBreakthrough(graphics, mc, screenWidth, screenHeight);
    }

    /**
     * <b>道途机制的资源条</b>。
     *
     * <p>玩家要求能看到自己的机制状态，否则"激荡攒满了""血池快空了"
     * 这些关键信息全在盲区里 —— 机制再好也玩不出来。</p>
     *
     * <p>每条道途只画<b>一个主资源</b>（不并列四条），因为玩家同时只有一个道途，
     * 画多余的条只会挡视野。状态（引爆中 / 锁血 / 阵域展开）用文字后缀表示。</p>
     *
     * <p>资源数值是<b>服务端算好通过 {@code SyncCultivationPacket} 同步过来的</b>
     * （见 {@code ClientCultivationCache.setPathState}）—— 客户端不跑逻辑，
     * 避免两端算出不一样的数。</p>
     */
    private void renderPathMechanics(GuiGraphics graphics, Minecraft mc, int y) {
        int ordinal = com.hongmeng.xiuxian.network.ClientCultivationCache.pathOrdinal();
        if (ordinal < 0) {
            return;                      // 尚未择业，不画
        }
        var careers = CAREERS;
        if (ordinal >= careers.length) {
            return;
        }
        var career = careers[ordinal];
        int ri = resourceIndexOf(career);
        if (ri < 0) {
            return;
        }
        float value = com.hongmeng.xiuxian.network.ClientCultivationCache.resource(ri);
        float[] rgb = career.rgb();
        int color = 0xFF000000 | (((int) (rgb[0] * 255)) << 16)
                | (((int) (rgb[1] * 255)) << 8) | ((int) (rgb[2] * 255));

        y += 3;
        int percent = Math.round(value * 100.0F);
        // PERF(C): 只有资源百分比 / 状态旗标变化时才重建文字（原来每帧拼一次字符串 + Component）
        boolean flag0 = com.hongmeng.xiuxian.network.ClientCultivationCache.flag(0);
        boolean flag2 = com.hongmeng.xiuxian.network.ClientCultivationCache.flag(2);
        int flagKey = (flag0 ? 1 : 0) | (flag2 ? 4 : 0);
        if (pathLine == null || cachedPathOrdinal != ordinal
                || cachedPathPercent != percent || cachedPathFlags != flagKey) {
            cachedPathOrdinal = ordinal;
            cachedPathPercent = percent;
            cachedPathFlags = flagKey;
            String suffix = "";
            if (career == com.hongmeng.xiuxian.talent.Career.TI_XIU && flag0) {
                suffix = " \u00a7c\u00a7l引爆中";
            } else if (career == com.hongmeng.xiuxian.talent.Career.JIAN_XIU && flag0) {
                suffix = " \u00a7b\u00a7l剑意满";
            } else if (career == com.hongmeng.xiuxian.talent.Career.GUI_XIU && flag2) {
                suffix = " \u00a7a\u00a7l展开中";
            }
            pathLine = Component.literal("\u00a77" + resourceLabel(career) + " "
                    + percent + "%" + suffix);
        }
        graphics.drawString(mc.font, pathLine, X, y, 0xFFFFFF, true);
        y += 10;
        graphics.fill(X, y, X + BAR_WIDTH, y + BAR_HEIGHT, 0xAA101018);
        graphics.fill(X + 1, y + 1,
                X + 1 + (int) ((BAR_WIDTH - 2) * Math.max(0.0F, Math.min(1.0F, value))),
                y + BAR_HEIGHT - 1, color);
        graphics.renderOutline(X, y, BAR_WIDTH, BAR_HEIGHT, color);
    }

    /** PERF(C): Career.values() 每次调用都会克隆数组，缓存一份。 */
    private static final com.hongmeng.xiuxian.talent.Career[] CAREERS =
            com.hongmeng.xiuxian.talent.Career.values();

    /** PERF(C): 道途资源行缓存（见 renderPathMechanics）。 */
    private int cachedPathOrdinal = Integer.MIN_VALUE;
    private int cachedPathPercent = Integer.MIN_VALUE;
    private int cachedPathFlags = Integer.MIN_VALUE;
    private Component pathLine;

    /** 职业 → 资源下标（与 {@code PathRuntime.snapshot} 的顺序一致）。 */
    private static int resourceIndexOf(com.hongmeng.xiuxian.talent.Career career) {
        return switch (career) {
            case JIAN_XIU, TI_XIU -> 0;   // 剑意 / 激荡
            case MO_XIU -> 1;             // 魔气
            case XUE_XIU -> 2;            // 血池
            case GUI_XIU -> 3;            // 鬼域
        };
    }

    private static String resourceLabel(com.hongmeng.xiuxian.talent.Career career) {
        return switch (career) {
            case JIAN_XIU -> "剑意";
            case TI_XIU -> "激荡";
            case MO_XIU -> "魔气";
            case XUE_XIU -> "血池";
            case GUI_XIU -> "鬼域";
        };
    }

    /** 真实痛觉：屏幕边缘泛红 + 轻微错位。 */
    private void renderPain(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!HongmengConfig.SERVER.painEffects.get()) {
            return;
        }
        float pain = ClientPainState.intensity();
        if (pain <= 0.01F) {
            return;
        }
        int alpha = (int) Math.min(150.0F, pain * 60.0F);
        if (alpha <= 0) {
            return;
        }
        int color = (alpha << 24) | 0x8B0000;
        int band = (int) (24 + pain * 12);
        // 四边红色渐晕
        graphics.fill(0, 0, screenWidth, band, color);
        graphics.fill(0, screenHeight - band, screenWidth, screenHeight, color);
        graphics.fill(0, 0, band, screenHeight, color);
        graphics.fill(screenWidth - band, 0, screenWidth, screenHeight, color);
    }

    /** 破境演出：居中文字 + 闪光。 */
    private void renderBreakthrough(GuiGraphics graphics, Minecraft mc,
                                    int screenWidth, int screenHeight) {
        if (!ClientBreakthroughFx.active()) {
            return;
        }
        var kind = ClientBreakthroughFx.kind();
        if (kind == null) {
            return;
        }
        float progress = ClientBreakthroughFx.progress();
        int alpha = (int) ((1.0F - progress) * 255.0F);

        String text;
        int rgb;
        switch (kind) {
            case SUCCESS -> {
                // 玩家反馈「提升的显示字体为黄色」：原来这行是青绿色，压在天空 / 云上
                // 几乎看不见。改亮黄（和动作栏的"修为 +N"同一套语汇）。
                text = "破 境 · " + ClientBreakthroughFx.realm();
                rgb = 0xFFE24A;
            }
            case FAIL -> {
                text = "突破失败";
                rgb = 0xFF5555;
            }
            default -> {
                text = "天 劫 将 至";
                rgb = 0xC08CFF;
            }
        }

        int color = (Math.max(0, Math.min(255, alpha)) << 24) | rgb;
        int textWidth = mc.font.width(text);
        graphics.drawString(mc.font, Component.literal(text),
                (screenWidth - textWidth) / 2, screenHeight / 2 - 40, color, true);

        if (kind == com.hongmeng.xiuxian.network.BreakthroughFxPacket.Kind.TRIBULATION) {
            int flash = (int) ((1.0F - progress) * 60.0F);
            graphics.fill(0, 0, screenWidth, screenHeight, (flash << 24) | 0xC08CFF);
        }
    }

    /** 面板里复用的小工具：把境界画成一行摘要。 */
    public static String summarize(CultivationData data) {
        SpiritRoot root = data.getRoot();
        return (root == null ? "未觉醒" : root.rarity().cn() + "·" + root.cn())
                + " | " + data.realm().cn()
                + " | 修为 " + data.progress() + "/" + data.needed()
                + " | 业力 " + data.getKarma();
    }

}
