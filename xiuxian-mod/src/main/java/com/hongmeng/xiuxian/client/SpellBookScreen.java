package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.network.ClientCultivationCache;
import com.hongmeng.xiuxian.network.TechniqueActionPacket;
import com.hongmeng.xiuxian.technique.Technique;
import com.hongmeng.xiuxian.technique.TechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 法术栏（O 键）。
 *
 * <pre>
 *   ┌────────────────────────────────────────────┐
 *   │ 法术栏                    已修习 12 / 37   │
 *   │ 被动（5）                                   │
 *   │ [1 灵海无量][2 吐纳诀][3 空][4 空][5 空]   │
 *   │ 主动（4）                                   │
 *   │ [Z 御剑斩][X 雷罚][C 空][V 空]             │
 *   ├────────────────────────────────────────────┤
 *   │ 全部 / 主动 / 被动            滚轮翻页      │
 *   │ ▸ 御剑斩  金线  蓄力0.9s 灵力400           │
 *   │   雷罚    雷弹  蓄力0.7s 灵力980           │
 *   │   …                                        │
 *   ├────────────────────────────────────────────┤
 *   │ 御剑斩  金/线  门槛第4境                    │
 *   │ 蓄力 0.9s · 灵力 400 · 冷却 4.5s            │
 *   │ 剑随心动，斩于百步之外                       │
 *   │ 特性：直线贯穿并附加剑气震荡                  │
 *   └────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>已装备的在上面（5 被动 + 4 主动，都带标签），已学会的在下面（可滚动列表）。</b>
 * 两条路都通：①先点下面列表里的一门功法选中它，再点上面的槽位装备；
 * ②左键按住功法拖到槽上松手，拖动时有一份半透明虚影跟着光标。
 * 位移不到 {@link #DRAG_THRESHOLD} 像素的仍按"点一下"处理，所以原来的点选路径一点没变。
 * 点已经装了的槽位 = 卸下；把槽上的功法拖到空白处也 = 卸下，拖回原来那一格 = 什么也不做。</p>
 *
 * <p>装备规则由服务端裁决（只有已修习的能装备、被动只能进被动槽），
 * 这里只负责发意图与画状态——拖拽走的还是 {@code select} + {@code equip} 那两个既有包，
 * 没有新增网络包，客户端也不自己实现一套规则。</p>
 *
 * <p>界面读的是 {@link ClientCultivationCache} 里的那份 {@link TechniqueData}——
 * 和 R 面板的功法栏同一个来源，服务端每次改动都会回同步，
 * 所以装备完不需要自己刷新。</p>
 */
@OnlyIn(Dist.CLIENT)
public class SpellBookScreen extends Screen {

    // ---- 配色（与修炼面板同一套） ----
    private static final int BG = 0xF0141A2E;
    private static final int BORDER = 0xFF8A6FD8;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int SLOT_BG = 0xFF1C2438;
    private static final int SLOT_BORDER = 0xFF3A4460;
    private static final int SLOT_SELECTED = 0x40FFD24A;
    private static final int ROW_SELECTED = 0x40FFD24A;
    private static final int ROW_HOVER = 0x20FFFFFF;
    private static final int GOLD = 0xFFFFD24A;

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 232;

    /** 被动/主动槽方块的尺寸。 */
    private static final int SLOT_W = 68;
    private static final int SLOT_H = 22;
    private static final int SLOT_GAP = 3;

    /** 列表每行高度。 */
    private static final int ROW_H = 11;
    /** 一屏能显示多少行。 */
    private static final int VISIBLE_ROWS = 8;

    /** 按下点到松开点的位移小于这么多像素就算"点一下"，原来的点选路径照旧走。 */
    private static final int DRAG_THRESHOLD = 4;

    /** 按下时命中的是什么。 */
    private static final int PRESS_NONE = 0;
    private static final int PRESS_ROW = 1;
    private static final int PRESS_SLOT = 2;

    /** 拖拽中种类对不上的槽：描边标红，和服务端的拒绝是同一件事的两种说法。 */
    private static final int DRAG_INVALID = 0xFFE05A5A;
    /** 跟手虚影的配色（都带 alpha，要求是半透明）。 */
    private static final int GHOST_BORDER = 0xB0FFD24A;
    private static final int GHOST_BG = 0xD0141A2E;
    private static final int GHOST_TEXT = 0xE0E8E8F0;

    /** 列表过滤：0 全部 / 1 主动 / 2 被动。 */
    private int filter = 0;
    /** 列表滚动偏移（行）。 */
    private int scroll = 0;
    /** 下面列表里被点中的那一门（准备装备的）。 */
    private Technique picked;

    /**
     * 拖拽只留一组"按下时的快照"，不另存"现在是不是在拖"这种状态。
     *
     * <p>是否算拖拽每帧拿鼠标位置现算（{@link #draggedNow}），渲染和松手裁决读的是
     * 同一个判据，不会出现"画面已经在拖、松手却按点击处理"这种两套状态打架。</p>
     */
    private int pressKind = PRESS_NONE;
    /** 按下时那门功法：列表行上就是这一行，槽位上就是槽里那门，空白处为 null。 */
    private Technique pressTechnique;
    /** 按下时命中的槽位编码（被动 0..4 / 主动 10..13）；不是槽位就是 -1。 */
    private int pressSlot = -1;
    private double pressX;
    private double pressY;
    /** 槽位命中区域算过没有：首帧渲染之前不响应命中，免得按一堆全 0 的坐标去撞。 */
    private boolean layoutReady;

    /** 每个槽位的命中区域（每帧渲染时更新）。 */
    private final int[] passiveSlotX = new int[TechniqueData.PASSIVE_SLOTS];
    private final int[] passiveSlotY = new int[TechniqueData.PASSIVE_SLOTS];
    private final int[] activeSlotX = new int[TechniqueData.ACTIVE_SLOTS];
    private final int[] activeSlotY = new int[TechniqueData.ACTIVE_SLOTS];
    private int[] rowY = new int[0];
    private int listX;
    private int listW;
    private int filterX;
    private int filterY;

    public SpellBookScreen() {
        super(Component.literal("鸿蒙修仙 · 法术栏"));
    }

    // ------------------------------------------------------------------
    // 数据
    // ------------------------------------------------------------------

    private TechniqueData techniques() {
        return ClientCultivationCache.get().getTechniques();
    }

    /** 过滤后的已修习列表。 */
    private List<Technique> filtered() {
        List<Technique> out = new ArrayList<>();
        for (Technique t : techniques().knownList()) {
            if (filter == 1 && t.isPassive()) {
                continue;
            }
            if (filter == 2 && !t.isPassive()) {
                continue;
            }
            out.add(t);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        // 窗口失焦时 GLFW 不会再补发 mouseReleased，拖拽状态必须自己丢掉：
        // 否则切回来会拖着一个"幽灵功法"，随手一松就装到别的槽上
        if (minecraft != null && !minecraft.isWindowActive()) {
            clearPress();
        }
        // 这一帧被拖的那门（没在拖就是 null），下面画槽位和虚影都用它
        Technique drag = draggedNow(mouseX, mouseY);

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        g.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, BORDER);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, BG);

        TechniqueData tech = techniques();
        var known = tech.knownList();

        // ---- 标题 ----
        int x = left + 10;
        int y = top + 8;
        g.drawString(font, Component.literal("§6§l法术栏"), x, y, TEXT, false);
        String count = "§8已修习 " + known.size() + " / " + Technique.values().length;
        g.drawString(font, Component.literal(count),
                left + PANEL_W - 10 - font.width(count), y + 2, TEXT, false);
        y += 16;

        // ---- 被动（5 槽） ----
        g.drawString(font, Component.literal("§b§l被动 §8（装备即生效 · 不耗灵力）"),
                x, y, TEXT, false);
        y += 11;
        for (int i = 0; i < TechniqueData.PASSIVE_SLOTS; i++) {
            int sx = x + i * (SLOT_W + SLOT_GAP);
            passiveSlotX[i] = sx;
            passiveSlotY[i] = y;
            drawSlot(g, sx, y, "被动" + (i + 1), tech.passiveAt(i), mouseX, mouseY, false, drag);
        }
        y += SLOT_H + 8;

        // ---- 主动（4 槽） ----
        g.drawString(font, Component.literal("§b§l主动 §8（按 Z X C V 释放）"), x, y, TEXT, false);
        y += 11;
        for (int i = 0; i < TechniqueData.ACTIVE_SLOTS; i++) {
            int sx = x + i * (SLOT_W + SLOT_GAP);
            activeSlotX[i] = sx;
            activeSlotY[i] = y;
            drawSlot(g, sx, y, castKeyName(i), tech.activeAt(i), mouseX, mouseY, true, drag);
        }
        y += SLOT_H + 8;

        // 9 个槽的命中区域到这里就齐了；在这之前鼠标事件一律按"没命中槽位"处理
        layoutReady = true;

        // ---- 分隔线 ----
        g.fill(x, y, x + PANEL_W - 20, y + 1, 0x558A6FD8);
        y += 5;

        // ---- 过滤页签 ----
        filterX = x;
        filterY = y;
        String[] tabs = {"全部", "主动", "被动"};
        int tx = x;
        for (int i = 0; i < tabs.length; i++) {
            int w = font.width(tabs[i]) + 10;
            boolean on = filter == i;
            boolean hover = mouseX >= tx && mouseX <= tx + w && mouseY >= y && mouseY <= y + 11;
            g.fill(tx, y, tx + w, y + 11, on ? 0x60FFD24A : (hover ? 0x30FFFFFF : 0x20FFFFFF));
            g.drawString(font, Component.literal((on ? "§e" : "§7") + tabs[i]),
                    tx + 5, y + 2, TEXT, false);
            tx += w + 4;
        }
        // 提示和原来一样宽（只是"滚轮翻页"挪到了下面那行计数上），
        // 这样右对齐之后不会往左顶到页签上
        String hint = "§81~5 装被动 · Z X C V 装主动 · 0 清空";
        g.drawString(font, Component.literal(hint),
                left + PANEL_W - 10 - font.width(hint), y + 2, TEXT, false);
        y += 14;

        // 数字键直装的反馈：5 秒内有效，避免玩家"按了但不知道按到了什么"。
        if (!hotkeyNote.isEmpty()
                && System.currentTimeMillis() - hotkeyNoteAt < 8000L) {
            // 画在面板**顶部**：底部容易和其它提示重叠/被裁掉
            g.drawString(font, Component.literal(hotkeyNote),
                    x, top + 2, TEXT, false);
        }

        // ---- 已修习列表 ----
        listX = x;
        listW = PANEL_W - 20;
        var list = filtered();
        int maxScroll = Math.max(0, list.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        rowY = new int[Math.min(VISIBLE_ROWS, list.size())];

        if (list.isEmpty()) {
            g.drawString(font, Component.literal("§7这里还没有功法。§8（右键功法秘籍修习）"),
                    x, y, TEXT, false);
        }
        var equipped = tech.equippedPassives();
        var equippedActives = tech.equippedActives();
        for (int row = 0; row < rowY.length; row++) {
            int idx = row + scroll;
            Technique t = list.get(idx);
            int ry = y + row * ROW_H;
            rowY[row] = ry;
            boolean pickedRow = t == picked;
            boolean hover = mouseX >= x && mouseX <= x + listW
                    && mouseY >= ry && mouseY <= ry + ROW_H - 1;
            if (pickedRow) {
                g.fill(x, ry - 1, x + listW, ry + ROW_H - 1, ROW_SELECTED);
            } else if (hover) {
                g.fill(x, ry - 1, x + listW, ry + ROW_H - 1, ROW_HOVER);
            }
            boolean onSlot = equipped.contains(t) || equippedActives.contains(t);
            // 行首编号 = 该按的数字键（1~9）。玩家要求「挨个按数字 123456789 装备」，
            // 所以编号必须与"列表里第几个"一致，包括受页签筛选之后的结果。
            int keyNo = idx + 1;
            String keyTag = keyNo <= 9 ? ("§8[" + keyNo + "] ") : "§8[ ] ";
            String head = keyTag
                    + (pickedRow ? "§e▶ " : (onSlot ? "§a● " : "§8  ")) + t.colored()
                    + " §8[" + t.element().cn() + t.kind().glyph() + "]";
            g.drawString(font, Component.literal(head), x + 2, ry, TEXT, false);
            // 右对齐：蓄力 / 灵力 / 冷却
            String price = "§7蓄力" + t.castLabel() + " §b灵力" + t.manaCost()
                    + " §8冷却" + String.format("%.1fs", t.cooldownSeconds());
            g.drawString(font, Component.literal(price),
                    x + listW - 4 - font.width(price), ry, TEXT, false);
        }
        if (maxScroll > 0) {
            String more = "§8" + (scroll + 1) + "-" + (scroll + rowY.length) + " / " + list.size()
                    + " §8滚轮翻页";
            g.drawString(font, Component.literal(more), x, y + VISIBLE_ROWS * ROW_H + 1,
                    TEXT, false);
        }
        y += VISIBLE_ROWS * ROW_H + 11;

        // ---- 详情 ----
        renderDetails(g, x, y, tech);

        // ---- 拖拽虚影 ----
        renderDragGhost(g, drag, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    /**
     * 跟着光标画的那份半透明虚影。
     *
     * <p>只走 {@link GuiGraphics} 现成的 {@code fill} / {@code drawString}：
     * 拖拽时这段代码每帧都在跑，自己 {@code new} 出来的 {@code RenderType} /
     * {@code BufferSource} 攒在那里不会被回收——这个项目已经因为"每帧新建 RenderType"
     * 崩过一次客户端，所以这里一根毛都不 new。</p>
     */
    private void renderDragGhost(GuiGraphics g, Technique drag, int mouseX, int mouseY) {
        if (drag == null) {
            return;
        }
        // 稍微偏开光标，不然虚影正好压在指针下面，看不出在拖什么
        int x = mouseX + 8;
        int y = mouseY + 8;
        String name = font.plainSubstrByWidth(drag.colored(), 90)
                + " §8" + (drag.isPassive() ? "被动" : "主动");
        int w = font.width(name) + 8;
        int h = 12;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, GHOST_BORDER);
        g.fill(x, y, x + w, y + h, GHOST_BG);
        g.drawString(font, Component.literal(name), x + 4, y + 2, GHOST_TEXT, false);
    }

    /** 画一个槽位方块；{@code drag} 是这一帧被拖的那门功法（没在拖就是 null）。 */
    private void drawSlot(GuiGraphics g, int sx, int sy, String label,
                          Technique t, int mouseX, int mouseY, boolean active, Technique drag) {
        boolean hover = mouseX >= sx && mouseX <= sx + SLOT_W
                && mouseY >= sy && mouseY <= sy + SLOT_H;
        // 高亮跟着"将要放上去"的那门走：拖拽时是被拖的那门，没拖时还是列表里点中的那门
        Technique want = drag != null ? drag : picked;
        boolean kindOk = want != null && want.isPassive() != active;
        // 拖过去落在同种类的槽上总归有结果（空的装上、有人的换下来），所以都点亮；
        // 而"点一下"落在有人的槽上是卸下而不是装备，所以只有空槽才亮
        boolean wantHere = kindOk && (drag != null ? t != want : (t == null || t == want));
        g.fill(sx, sy, sx + SLOT_W, sy + SLOT_H, hover ? 0xFF26304A : SLOT_BG);
        // 拖拽中种类对不上的槽直接标红：服务端也会拒，提前告诉玩家这里放不进去
        int outline = drag != null && !kindOk
                ? DRAG_INVALID
                : (wantHere ? GOLD : (t == null ? SLOT_BORDER : BORDER));
        g.renderOutline(sx, sy, SLOT_W, SLOT_H, outline);
        if (wantHere) {
            g.fill(sx + 1, sy + 1, sx + SLOT_W - 1, sy + SLOT_H - 1, SLOT_SELECTED);
        }
        // 左上角：槽位名（被动1 / Z）
        g.drawString(font, Component.literal("§8" + label), sx + 3, sy + 2, TEXT, false);
        if (t == null) {
            g.drawString(font, Component.literal("§8（空）"), sx + 3, sy + 11, TEXT, false);
            return;
        }
        String name = font.plainSubstrByWidth(t.colored(), SLOT_W - 22);
        g.drawString(font, Component.literal(name), sx + 16, sy + 2, TEXT, false);
        g.drawString(font, Component.literal("§8灵力" + t.manaCost()
                + (t.isPassive() ? "" : " §8蓄" + t.castLabel())),
                sx + 3, sy + 11, TEXT, false);
    }

    /** 底部详情：选中（或第一门）功法的全部数值 + 风味 + 特性。 */
    private void renderDetails(GuiGraphics g, int x, int y, TechniqueData tech) {
        Technique t = picked != null ? picked : tech.selectedTechnique();
        if (t == null) {
            g.drawString(font, Component.literal("§8点下面列表里的一门功法查看详情。"),
                    x, y, TEXT, false);
            return;
        }
        g.drawString(font, Component.literal("§f§l" + t.colored()
                + " §8[" + t.element().cn() + "/" + t.kind().glyph() + "/" + t.archetype() + "]"),
                x, y, TEXT, false);
        String tier = "§8门槛 第" + t.minTier() + "大境";
        g.drawString(font, Component.literal(tier),
                x + PANEL_W - 20 - font.width(tier), y, TEXT, false);
        y += 11;
        String stats = "§7蓄力 §f" + t.castLabel()
                + "  §7灵力 §b" + (t.isPassive() ? "0（被动）" : String.valueOf(t.manaCost()))
                + "  §7冷却 §f" + String.format("%.1fs", t.cooldownSeconds())
                + (t.isSupport() ? "" : "  §7威力 §c×" + String.format("%.1f", t.damageMultiplier()));
        g.drawString(font, Component.literal(stats), x, y, TEXT, false);
        y += 11;
        g.drawString(font, Component.literal("§8" + t.flavor()), x, y, TEXT, false);
        y += 11;
        g.drawString(font, Component.literal("§6特性：§f" + t.trait()), x, y, TEXT, false);
        if (tech.isEquipped(t)) {
            int code = tech.equippedSlotCode(t);
            String where = code >= 10 ? "主动槽 " + (code - 9) : "被动槽 " + (code + 1);
            String label = "§a已装备 · " + where;
            g.drawString(font, Component.literal(label),
                    x + PANEL_W - 20 - font.width(label), y, TEXT, false);
        }
    }

    /** 主动槽对应的按键名。 */
    private static String castKeyName(int slot) {
        return switch (slot) {
            case 0 -> "Z";
            case 1 -> "X";
            case 2 -> "C";
            default -> "V";
        };
    }

    // ------------------------------------------------------------------
    // 交互
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 拖拽只认左键，别的键交回父类（和修行面板的处理保持一致）
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // 上一轮万一因为失焦之类没收到 mouseReleased，这里兜底丢掉，不让旧状态活到下一次按下
        clearPress();

        // 过滤页签
        String[] tabs = {"全部", "主动", "被动"};
        int tx = filterX;
        for (int i = 0; i < tabs.length; i++) {
            int w = font.width(tabs[i]) + 10;
            if (mouseX >= tx && mouseX <= tx + w && mouseY >= filterY && mouseY <= filterY + 11) {
                filter = i;
                scroll = 0;
                return true;
            }
            tx += w + 4;
        }

        // 槽位：这里只记下"按在哪个槽"，装备 / 卸下 / 换槽 / 拖出去统统等松手时决定。
        // 点击和拖拽因此共用一个入口，不会各写一套判定
        int slot = slotCodeAt(mouseX, mouseY);
        if (slot >= 0) {
            pressKind = PRESS_SLOT;
            pressSlot = slot;
            pressTechnique = techniqueAtSlot(slot);
            pressX = mouseX;
            pressY = mouseY;
            return true;
        }

        // 已修习列表：点一下选中（原来就这样），从这里也能直接拖到槽上装备
        var list = filtered();
        for (int row = 0; row < rowY.length; row++) {
            int ry = rowY[row];
            if (mouseX >= listX && mouseX <= listX + listW
                    && mouseY >= ry - 1 && mouseY <= ry + ROW_H - 1) {
                int idx = row + scroll;
                if (idx >= 0 && idx < list.size()) {
                    Technique t = list.get(idx);
                    picked = t;
                    pressKind = PRESS_ROW;
                    pressTechnique = t;
                    pressSlot = -1;
                    pressX = mouseX;
                    pressY = mouseY;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        // 先把按下时的快照抄出来再清状态：下面每条分支都要用，清早了就只剩默认值可猜
        int kind = pressKind;
        Technique t = pressTechnique;
        int from = pressSlot;
        boolean moved = movedEnough(mouseX, mouseY);
        clearPress();

        if (!moved) {
            // 位移不到 4 像素：还是一次"点一下"，走原来那两条路
            if (kind == PRESS_ROW) {
                // 点功法选中。这里补发一发 select：服务端装备时用的是它自己记的选中项，
                // 不同步过去的话，"点功法 → 点槽位"装上的可能不是刚点的那门
                selectIfNeeded(t);
                return true;
            }
            if (kind == PRESS_SLOT) {
                equipSlot(from);
                return true;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }
        drop(t, from, mouseX, mouseY);
        return true;
    }

    /**
     * 松手落点裁决。
     *
     * <p>只发**既有**的 {@code select} 与 {@code equip} 两个包（见 {@link TechniqueActionPacket}），
     * "只有已修习的能装备""被动只能进被动槽"这些规则依旧由服务端说了算，
     * 客户端不另写一套——规则要改只改服务端一处。</p>
     *
     * @param t    被拖的那门功法；从空槽位拖出来的是 null（本来就没东西可放）
     * @param from 拖拽起点的槽位编码；从下面列表拖出来的是 -1
     */
    private void drop(Technique t, int from, double mouseX, double mouseY) {
        if (t == null) {
            return;                       // 空槽位拖出去：什么也没拖，当作取消
        }
        int to = slotCodeAt(mouseX, mouseY);
        if (to < 0) {
            // 没落在任何槽上：从槽位里拖出来 = 卸下（对应原来"点已装备槽位 = 卸下"），
            // 从列表里拖出来 = 取消，什么都不做
            if (from >= 0 && techniqueAtSlot(from) != null) {
                // 先 select 再发：万一服务端那格已经空了，也不会顺手把"当前选中"那门塞进去
                selectIfNeeded(t);
                equipSlot(from);
            }
            return;
        }
        if (to == from) {
            return;                       // 拖回原来那一格：什么也不做，免得被服务端当成"再点一次 = 卸下"
        }
        boolean toPassive = to < TechniqueActionPacket.ACTIVE_SLOT_BASE;
        if (t.isPassive() != toPassive) {
            // 被动 / 主动不混，这一条必须自己拦：
            // 服务端的规则是"槽上有东西 → 这一发 EQUIP 就是卸下"，而且它排在种类校验前面，
            // 真发过去的话占位的那门会被卸掉、被拖的这门又装不上，玩家白丢一门功法。
            // 提示文案走服务端同一个方法，两边说法不会走样
            warnSlotMismatch(t);
            return;
        }
        Technique sitting = techniqueAtSlot(to);
        if (sitting == t) {
            return;                       // 已经在这一格上了
        }
        selectIfNeeded(t);
        equipSlot(to);
        if (sitting != null) {
            // 这一格被别的功法占着：按服务端规则，第一发只会把占位的卸下来，
            // 想真装上去得再补一发（两发同一条连接、同一个方向，顺序是保住的）
            equipSlot(to);
        }
    }

    // ------------------------------------------------------------------
    // 拖拽用的小工具
    // ------------------------------------------------------------------

    /** 发"选中"给服务端；服务端已经选中同一门就不发（省掉一条没用的动作栏提示）。 */
    private void selectIfNeeded(Technique t) {
        if (t == null) {
            return;
        }
        TechniqueData tech = techniques();
        if (tech.selectedTechnique() == t) {
            return;
        }
        int idx = tech.knownList().indexOf(t);
        if (idx >= 0) {
            TechniqueActionPacket.select(idx);
        }
    }

    /** 槽位编码 → 包里那两个方法；装备和卸下都是同一发 EQUIP，由服务端看槽上有没有东西。 */
    private static void equipSlot(int code) {
        if (code < 0) {
            return;                       // 越界绝不发包：发出去也只能换来一句"装备失败"
        }
        if (code >= TechniqueActionPacket.ACTIVE_SLOT_BASE) {
            TechniqueActionPacket.equipActiveSlot(code - TechniqueActionPacket.ACTIVE_SLOT_BASE);
        } else {
            TechniqueActionPacket.equipPassiveSlot(code);
        }
    }

    /** 种类不符的落点：在本地就说清楚，不发包（理由见 {@link #drop}）。 */
    private void warnSlotMismatch(Technique t) {
        if (minecraft != null && minecraft.player != null) {
            TechniqueActionPacket.warnSlotMismatch(minecraft.player, t);
        }
    }

    /**
     * 坐标命中的槽位编码：被动 0..4、主动 10..13，没命中返回 -1。
     *
     * <p>编码直接沿用 {@link TechniqueActionPacket} 那一套，省得界面一套序号、
     * 包里又一套序号来回换算——换错一次就是装到别的槽上去。</p>
     */
    private int slotCodeAt(double mx, double my) {
        if (!layoutReady) {
            return -1;                    // 首帧渲染之前命中区域还是空的，别拿全 0 坐标去撞
        }
        // 循环上界取数组长度：和渲染时写进去的格数天生一致，不会有半格命中区
        for (int i = 0; i < passiveSlotX.length; i++) {
            if (inside(mx, my, passiveSlotX[i], passiveSlotY[i], SLOT_W, SLOT_H)) {
                return i;
            }
        }
        for (int i = 0; i < activeSlotX.length; i++) {
            if (inside(mx, my, activeSlotX[i], activeSlotY[i], SLOT_W, SLOT_H)) {
                return TechniqueActionPacket.ACTIVE_SLOT_BASE + i;
            }
        }
        return -1;
    }

    /** 槽位编码 → 槽上那门功法；空槽与越界一律 null（{@link TechniqueData} 自己会挡）。 */
    private Technique techniqueAtSlot(int code) {
        if (code < 0) {
            return null;
        }
        TechniqueData tech = techniques();
        return code >= TechniqueActionPacket.ACTIVE_SLOT_BASE
                ? tech.activeAt(code - TechniqueActionPacket.ACTIVE_SLOT_BASE)
                : tech.passiveAt(code);
    }

    /** 这一帧被拖的那门功法；没在拖（没按下 / 按在空白上 / 还没挪够 4 像素）返回 null。 */
    private Technique draggedNow(double mouseX, double mouseY) {
        return pressKind != PRESS_NONE && pressTechnique != null && movedEnough(mouseX, mouseY)
                ? pressTechnique
                : null;
    }

    /** 按下点到当前位置的位移够不够算拖拽；不够就还是一次点击。 */
    private boolean movedEnough(double mouseX, double mouseY) {
        double dx = mouseX - pressX;
        double dy = mouseY - pressY;
        return dx * dx + dy * dy >= (double) DRAG_THRESHOLD * DRAG_THRESHOLD;
    }

    /** 丢掉按下 / 拖拽状态：松手、关界面、失焦、以及下一次按下，都要走到这里。 */
    private void clearPress() {
        pressKind = PRESS_NONE;
        pressTechnique = null;
        pressSlot = -1;
        pressX = 0.0D;
        pressY = 0.0D;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        var list = filtered();
        int maxScroll = Math.max(0, list.size() - VISIBLE_ROWS);
        if (maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta)));
        }
        return true;
    }

    /** 数字键直装的反馈（画在界面底部，持续若干秒，免得玩家不知道自己按到了什么）。 */
    private String hotkeyNote = "";
    private long hotkeyNoteAt = 0L;

    /**
     * 数字键直装：**1~5 → 被动槽 1~5，6~9 → 主动槽 1~4**。
     *
     * <p>玩家要求「在法术页面挨个按数字 123456789 可以装备法术，前五个是被动，
     * 后四个是主动」。数字取的是**列表里第几个**（受页签筛选影响），
     * 所以"挨个按过去"就是顺着列表从上往下装。</p>
     *
     * @return true 表示这次按键被吃掉了（不要再传给上层）
     */
    private boolean handleHotkeyEquip(int keyCode) {
        var list = filtered();

        // 0 / Backspace：一键清空
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_0
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            for (int i = 0; i < TechniqueData.PASSIVE_SLOTS; i++) {
                equipSlot(i);
            }
            for (int i = 0; i < TechniqueData.ACTIVE_SLOTS; i++) {
                equipSlot(TechniqueActionPacket.ACTIVE_SLOT_BASE + i);
            }
            note("\u00a77已清空全部法术槽");
            return true;
        }

        // ---- Z X C V：装主动槽 1~4（玩家："zxcv 装备主动技能"）----
        int activeSlot = -1;
        switch (keyCode) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_Z -> activeSlot = 0;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_X -> activeSlot = 1;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_C -> activeSlot = 2;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_V -> activeSlot = 3;
            default -> {
                // 不是 ZXCV，往下走数字键分支
            }
        }
        if (activeSlot >= 0) {
            Technique picked = pickedOrFirstActive(list);
            if (picked == null) {
                note("\u00a7c没有可装的主动功法 —— 先修习一门主动功法");
                return true;
            }
            selectIfNeeded(picked);
            equipSlot(TechniqueActionPacket.ACTIVE_SLOT_BASE + activeSlot);
            note("\u00a7a已装备 \u00a7f" + picked.cn()
                    + " \u00a78→ 主动槽 " + (activeSlot + 1)
                    + " \u00a77（按 " + (char) ('Z' + activeSlot) + " 施放）");
            return true;
        }

        // ---- 数字键：装被动槽（1~5；6~9 也认，兼容旧习惯）----
        int digit = -1;
        if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_1
                && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_9) {
            digit = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1 + 1;      // 1..9
        }
        if (digit < 0) {
            return false;
        }
        if (list.isEmpty()) {
            note("\u00a7c列表是空的（试试滚轮或点上面的页签切换「全部/主动/被动」）");
            return true;
        }
        if (digit > list.size()) {
            note("\u00a7c列表里只有 " + list.size() + " 门，没有第 " + digit + " 门");
            return true;
        }
        Technique t = list.get(digit - 1);
        int passiveSlot = digit <= TechniqueData.PASSIVE_SLOTS
                ? digit - 1
                : digit - 1;        // 6~9 也落到第 6~9 个被动槽（越界由服务端拒绝）
        if (!t.isPassive()) {
            note("\u00a7c" + t.cn() + " 是主动功法 —— 请按 \u00a7fZ X C V \u00a7c装主动槽");
            return true;
        }
        selectIfNeeded(t);
        equipSlot(passiveSlot);
        note("\u00a7a已装备 \u00a7f" + t.cn() + " \u00a78→ 被动槽 " + (passiveSlot + 1));
        return true;
    }

    /** 优先用"当前选中的"，否则取列表里第一门主动功法。 */
    private Technique pickedOrFirstActive(java.util.List<Technique> list) {
        Technique sel = picked;
        if (sel != null && !sel.isPassive()) {
            return sel;
        }
        for (Technique t : list) {
            if (!t.isPassive()) {
                return t;
            }
        }
        // 列表被筛成"只看被动"时，退回全部已学列表里找
        for (Technique t : techniques().knownList()) {
            if (!t.isPassive()) {
                return t;
            }
        }
        return null;
    }

    private void note(String text) {
        hotkeyNote = text;
        hotkeyNoteAt = System.currentTimeMillis();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_O
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        // 数字键直装（放在最前面：数字键在这界面里没有别的用途）
        if (handleHotkeyEquip(keyCode)) {
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
            mouseScrolled(0, 0, -1);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP) {
            mouseScrolled(0, 0, 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        // 按住左键时按 O / ESC 关界面：GLFW 不会再补一个 mouseReleased，
        // 拖拽状态必须在关的这一下丢掉，否则会拖到下一局界面上
        clearPress();
        super.onClose();
    }

    @Override
    public void removed() {
        // O 键那条路是直接 setScreen(null)，不走 onClose；换界面 / 被顶掉也走这里
        clearPress();
        super.removed();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // 拖到一半改窗口大小 / 改 GUI 缩放：按下的老坐标已经不成立了，
        // 再按它算位移和落点只会装错槽，直接当这次拖拽没发生过
        clearPress();
        super.resize(minecraft, width, height);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
