package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 数值血条 + 数值护甲条 + 数值灵力条。
 *
 * <p>原版血量是"一排爱心"，每颗心 2 点生命，一行最多 10 颗、多了往上堆。
 * 修仙到后期血量上万，爱心会铺满半个屏幕。护甲同理（一排护甲图标）。
 * 所以生命或护甲超过阈值时，把原版的图标行取消，换成数值条。</p>
 *
 * <h3>位置</h3>
 *
 * <p>三条共用快捷栏那一列，<b>从下往上</b>依次是：灵力 → 生命 → 护甲。
 * 只画"该画的那几条"，画一条往上挪一格，所以前期的玩家只看到一根灵力条，
 * 后期三条叠在一起也不会互相压。</p>
 *
 * <pre>
 *   ┌──────────────────────┐  ← 护甲条
 *   ├──────────────────────┤  ← 血条
 *   ├──────────────────────┤  ← 灵力条
 *   │  原版护甲 / 饥饿图标   │
 *   │      快捷栏           │
 * </pre>
 *
 * <h3>为什么画在 RenderGuiEvent.Post</h3>
 *
 * <p>Forge 的 {@code ForgeGui} 派发逻辑是：</p>
 * <pre>
 *   if (pre(entry, g)) return;     // Pre 被取消 → 直接跳过整段
 *   overlay.render(...);
 *   post(entry, g);                // ← 于是 Post 永远不会触发
 * </pre>
 * <p>一旦在 Pre 里取消了某个覆盖层，同名覆盖层的 Post 就再也不会来。
 * 所以必须画在 {@code RenderGuiEvent.Post}——它在所有覆盖层之后必定触发。
 * 灵力条也走这一处，<b>不要</b>改用 {@code RenderGuiOverlayEvent.Post}。</p>
 */
@OnlyIn(Dist.CLIENT)
public class HealthBarOverlay {

    /** 超过这个心数（=20 颗心 = 40 点生命）就把爱心换成数值条。 */
    public static final float HEART_THRESHOLD = 20.0F;

    /** 护甲超过这个值就把护甲图标换成数值条。 */
    public static final float ARMOR_THRESHOLD = 20.0F;

    /** 条宽（与快捷栏等宽）。 */
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 9;
    /** 两条之间的间距。 */
    private static final int GAP = 1;
    /** 两条整体距离屏幕底部的距离。 */
    private static final int BOTTOM_MARGIN = 58;

    private static final int HEALTH_BORDER = 0xFF8A6FD8;
    private static final int ARMOR_BORDER = 0xFF6FA8D8;
    private static final int MANA_BORDER = 0xFF7FD8C8;
    private static final int BG = 0xFF12060A;
    private static final int HEALTH_TRACK = 0xFF3A0E14;
    private static final int ARMOR_TRACK = 0xFF0E2233;
    private static final int MANA_TRACK = 0xFF0B2622;

    // ------------------------------------------------------------------
    // PERF(C): 数值条文字缓存
    //
    // 三条条每帧都要显示"生命 12345 / 67890"这类字符串，原来每帧都要
    // 2~4 次 String.format（Formatter 解析格式串，比拼接贵一个数量级）。
    // 数值在一帧里通常不变，按"数值 key"缓存整条字符串即可。
    // ------------------------------------------------------------------
    private long healthKeyA = Long.MIN_VALUE;
    private long healthKeyB = Long.MIN_VALUE;
    private String healthText = "";
    private long armorKey = Long.MIN_VALUE;
    private String armorText = "";
    private long manaKey = Long.MIN_VALUE;
    private String manaText = "";

    private String healthBarText(float cur, float max) {
        long a = Float.floatToIntBits(cur);
        long b = Float.floatToIntBits(max);
        if (a != healthKeyA || b != healthKeyB) {
            healthKeyA = a;
            healthKeyB = b;
            healthText = "生命 " + formatHp(cur) + " / " + formatHp(max);
        }
        return healthText;
    }

    private String armorBarText(float total) {
        long a = Float.floatToIntBits(total);
        if (a != armorKey) {
            armorKey = a;
            armorText = "护甲 " + formatHp(total);
        }
        return armorText;
    }

    private String manaBarText(int cur, int max) {
        long a = ((long) cur << 32) | (max & 0xFFFFFFFFL);
        if (a != manaKey) {
            manaKey = a;
            manaText = "灵力 " + formatHp(cur) + " / " + formatHp(max);
        }
        return manaText;
    }

    /** 该不该用数值血条显示（而不是爱心）。 */
    public static boolean useNumericBar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) {
            return false;
        }
        return mc.player.getMaxHealth() > HEART_THRESHOLD * 2.0F;
    }

    /** 该不该用数值护甲条显示。 */
    public static boolean useArmorBar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) {
            return false;
        }
        return mc.player.getArmorValue() > ARMOR_THRESHOLD;
    }

    /**
     * 该不该显示灵力条。
     *
     * <p>和血条/护甲条不同：灵力条<b>不设阈值</b>。灵力是修仙的核心资源
     * （御剑、法术都要花），从练气第一刻就该看得见 —— 藏起来玩家根本不知道有这东西。
     * 觉醒之后才显示，是为了避免开局那段"还没有灵根"的演出被一根空条打断。</p>
     */
    public static boolean useManaBar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) {
            return false;
        }
        return com.hongmeng.xiuxian.network.ClientCultivationCache.get().isAwakened();
    }

    /**
     * 取消原版爱心 / 护甲图标。
     *
     * <p>必须是 Pre——Post 阶段它们已经画上去了，再取消没用。</p>
     */
    @SubscribeEvent
    public void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                && useNumericBar()) {
            event.setCanceled(true);
            return;
        }
        if (event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id())
                && useArmorBar()) {
            event.setCanceled(true);
        }
    }

    /**
     * 在所有覆盖层画完之后画三条数值条。
     *
     * <p>{@code RenderGuiEvent.Post} 在 {@code ForgeGui} 的覆盖层循环结束后
     * 必定触发，不受任何覆盖层被取消的影响——这正是我们要的保证。</p>
     */
    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        boolean health = useNumericBar();
        boolean armor = useArmorBar();
        boolean mana = useManaBar();
        if (!health && !armor && !mana) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int left = screenWidth / 2 - 91;

        // 从下往上排：灵力在最下、血条居中、护甲在顶。
        // 每画一条就往上挪一格，"只画一条"的时候也不会浮在半空。
        int cursor = screenHeight - BOTTOM_MARGIN - BAR_HEIGHT;

        if (health) {
            float max = mc.player.getMaxHealth();
            float cur = mc.player.getHealth();
            float ratio = Mth.clamp(cur / max, 0.0F, 1.0F);
            // 血量越低越红
            int fill = ratio > 0.50F ? 0xFFE03030
                    : (ratio > 0.25F ? 0xFFE07020 : 0xFFB01010);
            drawBar(g, font, left, cursor, ratio, fill,
                    HEALTH_BORDER, HEALTH_TRACK,
                    healthBarText(cur, max));
            cursor -= GAP + BAR_HEIGHT;
        }

        if (armor) {
            // 护甲：本体 + 韧性一起算，和原版减伤口径一致
            float total = mc.player.getArmorValue()
                    + (float) mc.player.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
            // 以"原版 20 点满护甲"为满条基准，超出部分条满但数字继续涨
            float ratio = Mth.clamp(total / ARMOR_THRESHOLD, 0.0F, 1.0F);
            drawBar(g, font, left, cursor, ratio, 0xFF4FC3F7,
                    ARMOR_BORDER, ARMOR_TRACK,
                    armorBarText(total));
            cursor -= GAP + BAR_HEIGHT;
        }

        if (mana) {
            // 灵力：当前值来自服务端每 0.5 秒一次的小同步包，
            // 上限由大境推出（见 core/ManaTable），两边算法一致。
            com.hongmeng.xiuxian.core.CultivationData data =
                    com.hongmeng.xiuxian.network.ClientCultivationCache.get();
            int cur = data.getMana();
            int max = data.maxMana();
            float ratio = Mth.clamp(data.manaRatio(), 0.0F, 1.0F);
            // 快见底的时候变橙，御剑途中能一眼看出"要掉下去了"
            int fill = ratio > 0.25F ? 0xFF43D6B0 : 0xFFE0A040;
            drawBar(g, font, left, cursor, ratio, fill,
                    MANA_BORDER, MANA_TRACK,
                    manaBarText(cur, max));
            cursor -= GAP + BAR_HEIGHT;
        }

    }

    /** 画一条数值条。 */
    private static void drawBar(GuiGraphics g, Font font, int left, int top,
                                float ratio, int fill, int border, int track,
                                String text) {
        g.fill(left - 1, top - 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, border);
        g.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, BG);
        g.fill(left + 1, top + 1, left + BAR_WIDTH - 1, top + BAR_HEIGHT - 1, track);

        int fillW = (int) ((BAR_WIDTH - 2) * ratio);
        if (fillW > 0) {
            g.fill(left + 1, top + 1, left + 1 + fillW, top + BAR_HEIGHT - 1, fill);
            // 顶部高光，看起来有立体感
            g.fill(left + 1, top + 1, left + 1 + fillW, top + 3, 0x40FFFFFF);
        }

        int txtW = font.width(text);
        int txtX = left + (BAR_WIDTH - txtW) / 2;
        int txtY = top + (BAR_HEIGHT - 8) / 2 + 1;
        g.drawString(font, text, txtX + 1, txtY + 1, 0xFF000000, false);
        g.drawString(font, text, txtX, txtY, 0xFFFFFFFF, false);
    }

    /**
     * 大数字用"万/亿"简化，免得数字太长压不住条。
     *
     * <p>PERF(C): 加了 8 槽直接映射缓存 —— 同一只怪的血量数字在相邻很多帧里完全相同，
     * 而 {@code String.format} 每次都要解析格式串（比拼接贵一个数量级）。
     * 缓存 key 是取整后的数值，命中时连格式化都省掉（输出与原来完全一致）。</p>
     */
    private static final long[] FMT_KEYS = new long[8];
    private static final String[] FMT_VALS = new String[8];

    public static String formatHp(float value) {
        long v = Math.round(value);
        int slot = (int) (v & 7L);
        String cached = FMT_VALS[slot];
        if (cached != null && FMT_KEYS[slot] == v) {
            return cached;
        }
        FMT_KEYS[slot] = v;
        String text;
        if (v >= 100_000_000L) {
            text = String.format("%.2f亿", v / 100_000_000.0D);
        } else if (v >= 10_000L) {
            text = String.format("%.2f万", v / 10_000.0D);
        } else {
            text = String.valueOf(v);
        }
        FMT_VALS[slot] = text;
        return text;
    }

    static {
        // 保持 ResourceLocation 引用，避免未使用告警
        ResourceLocation probe = new ResourceLocation(HongmengMod.MOD_ID, "health_bar");
        probe.getPath();
    }
}
