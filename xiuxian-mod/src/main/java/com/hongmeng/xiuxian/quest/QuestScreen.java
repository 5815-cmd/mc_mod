package com.hongmeng.xiuxian.quest;

import com.hongmeng.xiuxian.network.ClientCultivationCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务面板（J 键），内含两个页签。
 *
 * <p>布局参考成熟的 quest mod（FTB Quests 那一类）：
 * 左边是列表，右边是详情。</p>
 *
 * <ul>
 *   <li><b>任务页签</b>：章节 + 任务列表，右侧是描述、目标进度条、奖励、领取按钮</li>
 *   <li><b>坊市页签</b>：商品列表，右侧是价格、解锁条件、购买按钮；灵叶商品走"余额 + 背包实物"
 *       （余额不足时自动花背包里的实物），纪元令商品直接用令牌换法宝材料；
 *       下方还有"存入灵叶"按钮，把背包里的实物灵叶转成余额</li>
 * </ul>
 *
 * <p>把商店并进任务界面，是因为两者本来就是"该干什么"和"换什么"的同一件事，
 * 玩家按一个键就够了。</p>
 */
@OnlyIn(Dist.CLIENT)
public class QuestScreen extends Screen {

    private static final int W = 320;
    private static final int H = 200;
    private static final int LIST_W = 118;
    private static final int BG = 0xF0121622;
    private static final int PANEL = 0xF01A1F30;
    private static final int BORDER = 0xFF8A6FD8;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int DIM = 0xFF8A90A8;
    private static final int GOLD = 0xFFFFD24A;
    private static final int GREEN = 0xFF6FE07F;
    private static final int RED = 0xFFE06060;

    /** 页签：0 = 任务，1 = 坊市。 */
    private int tab = 0;
    private static final String[] TAB_NAMES = {"任务", "坊市"};

    /**
     * 坊市页里的子页签：0 = 买入，1 = 回收。
     *
     * <p>玩家要的是"商店既能买也能卖"。回收列表来自
     * {@link com.hongmeng.xiuxian.shop.SellPrices}（纯数据价目表），
     * 快捷栏与身上的装备不在回收范围内——想卖请拿在手上点"卖出手上物品"。</p>
     */
    private int shopTab = 0;
    private static final String[] SHOP_TAB_NAMES = {"买入", "回收"};

    private final Map<String, List<Quest>> chapters = new LinkedHashMap<>();
    private final List<Quest> flat = new ArrayList<>();

    private int selected = 0;
    private int shopSelected = 0;
    private int scroll = 0;

    private int listLeft;
    private int listTop;
    private int listHeight;
    private int claimBtnX;
    private int claimBtnY;
    private int claimBtnW;
    private int claimBtnH;
    private boolean claimVisible = false;
    private int buyBtnX;
    private int buyBtnY;
    private int buyBtnW;
    private int buyBtnH;
    private boolean buyVisible = false;
    /** 坊市页的"存入灵叶"按钮命中区。 */
    private int depositBtnX;
    private int depositBtnY;
    private int depositBtnW;
    private int depositBtnH;
    private boolean depositVisible = false;
    /** 坊市·回收页：卖出一整堆。 */
    private int sellBtnX;
    private int sellBtnY;
    private int sellBtnW;
    private int sellBtnH;
    private boolean sellVisible = false;
    /** 坊市·回收页：一键回收背包。 */
    private int recycleBtnX;
    private int recycleBtnY;
    private int recycleBtnW;
    private int recycleBtnH;
    private boolean recycleVisible = false;

    // ---- 坊市·回收页：部分出售（卖 N / 全部 / 保留 N） ----
    // 与 ShopScreen 同一套交互：一个数字、三个按钮、Shift ×10。
    /** 要卖 / 要留的数量。 */
    private int shopSellCount = 1;
    /** 换了一件东西就把数字重新上膛成"全部可卖"。 */
    private String shopSellCountItem = "";
    private boolean shopSellCountNeedsInit = true;

    private int shopSellBtnX;
    private int shopSellBtnY;
    private int shopSellBtnW;
    private int shopSellBtnH;
    private boolean shopSellVisible = false;
    private int shopAllBtnX;
    private int shopAllBtnY;
    private int shopAllBtnW;
    private int shopAllBtnH;
    private boolean shopAllVisible = false;
    private int shopKeepBtnX;
    private int shopKeepBtnY;
    private int shopKeepBtnW;
    private int shopKeepBtnH;
    private boolean shopKeepVisible = false;
    private int shopMinusX;
    private int shopMinusY;
    private int shopPlusX;
    private int shopPlusY;
    private int shopCountBoxX;
    private int shopCountBoxY;
    /** 坊市·买入/回收 子页签命中区。 */
    private final int[] shopTabX = new int[SHOP_TAB_NAMES.length];
    private int shopTabY;
    private static final int SHOPTAB_W = 40;
    private static final int SHOPTAB_H = 12;
    /** 页签按钮命中区。 */
    private final int[] tabX = new int[TAB_NAMES.length];
    private int tabY;
    private int tabW = 46;
    private int tabH = 13;

    public QuestScreen() {
        super(Component.literal("修行录"));
        for (Quest q : Quests.ALL) {
            chapters.computeIfAbsent(q.chapter(), k -> new ArrayList<>()).add(q);
            flat.add(q);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int left = (width - W) / 2;
        int top = (height - H) / 2;

        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, BG);

        QuestProgress progress = ClientCultivationCache.get().getQuestProgress();

        // ---------- 标题栏 + 页签 ----------
        g.drawString(font, "§6§l修行录", left + 8, top + 6, TEXT, false);

        tabY = top + 4;
        int tx = left + 64;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            tabX[i] = tx;
            boolean active = i == tab;
            boolean hover = mouseX >= tx && mouseX <= tx + tabW
                    && mouseY >= tabY && mouseY <= tabY + tabH;
            g.fill(tx, tabY, tx + tabW, tabY + tabH,
                    active ? 0xFF3A2A5A : (hover ? 0xFF2A2438 : 0xFF1A1826));
            g.renderOutline(tx, tabY, tabW, tabH, active ? BORDER : 0xFF40404A);
            String label = (active ? "§f§l" : "§7") + TAB_NAMES[i];
            g.drawString(font, label, tx + (tabW - font.width(label)) / 2, tabY + 3,
                    TEXT, false);
            tx += tabW + 4;
        }

        if (tab == 0) {
            int done = QuestManager.completedCount(progress);
            String counter = "§7进度 §f" + done + "§7/§f" + Quests.total();
            g.drawString(font, counter, left + W - 8 - font.width(counter), top + 7, TEXT, false);
        } else {
            String balance = "§7灵叶余额：§e"
                    + com.hongmeng.xiuxian.shop.ClientLeafWallet.balance();
            g.drawString(font, balance, left + W - 8 - font.width(balance), top + 7, TEXT, false);
        }
        g.fill(left, top + 20, left + W, top + 21, 0x558A6FD8);

        // ---------- 左侧列表 ----------
        listLeft = left + 4;
        listTop = top + 24;
        listHeight = H - 30;
        g.fill(listLeft, listTop, listLeft + LIST_W, listTop + listHeight, PANEL);

        if (tab == 0) {
            renderQuestList(g, progress);
        } else {
            renderShopList(g);
        }

        // ---------- 右侧详情 ----------
        int rx = left + LIST_W + 10;
        int rw = W - LIST_W - 16;

        if (tab == 0) {
            renderQuestDetail(g, progress, rx, top + 26, rw, mouseX, mouseY);
            g.drawString(font, "§8↑↓ / 滚轮切换 · Tab 切页签 · J 关闭",
                    left + 8, top + H - 12, DIM, false);
        } else {
            renderShopDetail(g, rx, top + 26, rw, mouseX, mouseY);
            String shopHint = shopTab == 0
                    ? "§8↑↓ / 滚轮切换 · 点击购买 · D 存入灵叶 · Tab 切页签 · J 关闭"
                    : "§8↑↓ / 滚轮切换 · 点击回收 · H 卖出手上 · Tab 切页签 · J 关闭";
            g.drawString(font, shopHint, left + 8, top + H - 12, DIM, false);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------------
    // 任务页签
    // ------------------------------------------------------------------

    private void renderQuestList(GuiGraphics g, QuestProgress progress) {
        int y = listTop + 4 - scroll;
        int i = 0;
        for (Map.Entry<String, List<Quest>> entry : chapters.entrySet()) {
            if (y > listTop - 10 && y < listTop + listHeight) {
                g.drawString(font, "§b§l" + entry.getKey(), listLeft + 5, y, TEXT, false);
            }
            y += 12;
            for (Quest q : entry.getValue()) {
                boolean isSel = i == selected;
                if (y + 11 > listTop && y < listTop + listHeight) {
                    if (isSel) {
                        g.fill(listLeft + 1, y - 1, listLeft + LIST_W - 1, y + 10, 0x40FFFFFF);
                    }
                    int state = progress.stateOf(q.id());
                    boolean unlocked = progress.isUnlocked(q);
                    String mark = state >= 2 ? "§a✔"
                            : (state == 1 ? "§e✦" : (unlocked ? "§7•" : "§8✖"));
                    String title = (unlocked ? (isSel ? "§f§l" : "§f") : "§8") + q.title();
                    g.drawString(font, mark + " " + title, listLeft + 7, y, TEXT, false);
                }
                y += 11;
                i++;
            }
            y += 4;
        }
    }

    private void renderQuestDetail(GuiGraphics g, QuestProgress progress,
                                   int rx, int ry, int rw, int mouseX, int mouseY) {
        claimVisible = false;
        buyVisible = false;
        depositVisible = false;
        Quest q = flat.isEmpty() ? null : flat.get(Math.min(selected, flat.size() - 1));
        if (q == null) {
            return;
        }

        boolean unlocked = progress.isUnlocked(q);
        int state = progress.stateOf(q.id());
        boolean met = q.requirement().isMet(progress);

        g.drawString(font, (unlocked ? "§f§l" : "§8§l") + q.title(), rx, ry, TEXT, false);
        ry += 12;
        g.drawString(font, "§8" + q.chapter() + " · 第 " + Quests.indexOf(q) + " 项",
                rx, ry, DIM, false);
        ry += 14;

        for (FormattedCharSequence line : font.split(
                Component.literal(unlocked ? "§7" + q.description() : "§8（前置任务未完成）"),
                rw)) {
            g.drawString(font, line, rx, ry, TEXT, false);
            ry += 10;
        }
        ry += 6;

        g.drawString(font, "§b§l目标", rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, (met ? "§a" : "§f") + q.requirement().describe(progress).getString(),
                rx, ry, TEXT, false);
        ry += 11;

        int barW = rw;
        g.fill(rx, ry, rx + barW, ry + 7, 0xFF0A0E18);
        g.fill(rx + 1, ry + 1, rx + 1 + (int) ((barW - 2) * q.requirement().ratio(progress)),
                ry + 6, met ? GREEN : 0xFF66CCFF);
        g.renderOutline(rx, ry, barW, 7, BORDER);
        ry += 13;

        if (unlocked && !met) {
            for (FormattedCharSequence line : font.split(
                    Component.literal("§8提示：" + q.hint()), rw)) {
                g.drawString(font, line, rx, ry, DIM, false);
                ry += 10;
            }
        }
        ry += 6;

        g.drawString(font, "§b§l奖励", rx, ry, TEXT, false);
        ry += 11;
        if (q.rewardCultivation() > 0) {
            g.drawString(font, "§7修为 §a+" + q.rewardCultivation(), rx, ry, TEXT, false);
            ry += 10;
        }
        if (!q.rewardItemId().isEmpty() && q.rewardItemCount() > 0) {
            g.drawString(font, "§7物品 §e" + q.rewardItemName(), rx, ry, TEXT, false);
            ry += 10;
        }
        if (q.rewardLeaves() > 0) {
            g.drawString(font, "§7灵叶 §e+" + q.rewardLeaves(), rx, ry, TEXT, false);
            ry += 10;
        }
        ry += 6;

        if (state >= 2) {
            g.drawString(font, "§a✔ 奖励已领取", rx, ry, TEXT, false);
        } else if (!unlocked) {
            g.drawString(font, "§8✖ 前置任务未完成", rx, ry, TEXT, false);
        } else if (met) {
            claimBtnX = rx;
            claimBtnY = ry;
            claimBtnW = 92;
            claimBtnH = 16;
            claimVisible = true;
            boolean hover = mouseX >= claimBtnX && mouseX <= claimBtnX + claimBtnW
                    && mouseY >= claimBtnY && mouseY <= claimBtnY + claimBtnH;
            g.fill(claimBtnX, claimBtnY, claimBtnX + claimBtnW, claimBtnY + claimBtnH,
                    hover ? 0xFF3E7A4E : 0xFF2A5A38);
            g.renderOutline(claimBtnX, claimBtnY, claimBtnW, claimBtnH, GOLD);
            String label = "§f领取奖励";
            g.drawString(font, label,
                    claimBtnX + (claimBtnW - font.width(label)) / 2, claimBtnY + 4, TEXT, false);
        } else {
            g.drawString(font, "§7进行中…", rx, ry, TEXT, false);
        }
    }

    // ------------------------------------------------------------------
    // 坊市页签
    // ------------------------------------------------------------------

    private void renderShopList(GuiGraphics g) {
        if (shopTab == 1) {
            renderSellList(g);
            return;
        }
        int tier = ClientCultivationCache.get().realm().tier();
        int y = listTop + 4 - scroll;
        for (int i = 0; i < com.hongmeng.xiuxian.shop.ShopCatalog.size(); i++) {
            var e = com.hongmeng.xiuxian.shop.ShopCatalog.all().get(i);
            boolean unlocked = e.unlockedFor(tier);
            boolean isSel = i == shopSelected;
            if (y + 12 > listTop && y < listTop + listHeight) {
                if (isSel) {
                    g.fill(listLeft + 1, y - 1, listLeft + LIST_W - 1, y + 11, 0x40FFFFFF);
                }
                String mark = unlocked ? "§e▪" : "§8✖";
                String name = (unlocked ? (isSel ? "§f§l" : "§f") : "§8") + e.cn();
                g.drawString(font, mark + " " + name, listLeft + 6, y, TEXT, false);
                String price = (unlocked ? "§6" : "§8") + e.priceShort();
                g.drawString(font, price,
                        listLeft + LIST_W - 8 - font.width(price), y, TEXT, false);
            }
            y += 13;
        }
    }

    /** 坊市·回收页的列表：价目表里每一件都列出来，有货的显示持有数量。 */
    private void renderSellList(GuiGraphics g) {
        var player = minecraft == null ? null : minecraft.player;
        int y = listTop + 4 - scroll;
        for (int i = 0; i < com.hongmeng.xiuxian.shop.SellPrices.size(); i++) {
            String id = com.hongmeng.xiuxian.shop.SellPrices.ids().get(i);
            // 品质感知的单价：和回收详情、服务端结算同一个口径。
            // 原来这里是 SellPrices.unitPrice(id)（只有凡品价），
            // 上品矿在列表里会显示成凡品价，和点进去看到的价不一样。
            int unit = com.hongmeng.xiuxian.shop.ShopManager
                    .sellTotal(id, representativeSellable(player, id), 1);
            boolean isSel = i == shopSelected;
            if (y + 12 > listTop && y < listTop + listHeight) {
                if (isSel) {
                    g.fill(listLeft + 1, y - 1, listLeft + LIST_W - 1, y + 11, 0x40FFFFFF);
                }
                int have = countSellable(player, id);
                String name = (isSel ? "§f§l" : "§f")
                        + com.hongmeng.xiuxian.shop.SellPrices.displayName(id);
                g.drawString(font, name, listLeft + 6, y, TEXT, false);
                String right = (have > 0 ? "§a" : "§6") + unit + "叶"
                        + (have > 0 ? " §7×" + have : "");
                g.drawString(font, right,
                        listLeft + LIST_W - 8 - font.width(right), y, TEXT, false);
            }
            y += 13;
        }
    }

    /** 背包里某物品的数量（不算快捷栏与身上的装备）。 */
    private int countSellable(net.minecraft.world.entity.player.Player player, String itemId) {
        if (player == null) {
            return 0;
        }
        var proto = com.hongmeng.xiuxian.shop.SellPrices.stackOf(itemId);
        if (proto.isEmpty()) {
            return 0;
        }
        int total = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (com.hongmeng.xiuxian.shop.ShopManager.sellsProtected(i)) {
                continue;
            }
            var s = inv.getItem(i);
            if (!s.isEmpty() && s.is(proto.getItem())) {
                total += s.getCount();
            }
        }
        return total;
    }

    /**
     * 背包里"第一种可卖的该物品"——用来取品质算价。
     *
     * <p>与 {@code ShopManager.sellEntry} 的结算顺序一致（第一堆先卖），
     * 所以这里显示的价就是真正到账的价。</p>
     */
    private net.minecraft.world.item.ItemStack representativeSellable(
            net.minecraft.world.entity.player.Player player, String itemId) {
        if (player == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        var proto = com.hongmeng.xiuxian.shop.SellPrices.stackOf(itemId);
        if (proto.isEmpty()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (com.hongmeng.xiuxian.shop.ShopManager.sellsProtected(i)) {
                continue;
            }
            var s = inv.getItem(i);
            if (!s.isEmpty() && s.is(proto.getItem())) {
                return s;
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    private void renderShopDetail(GuiGraphics g, int rx, int ry, int rw,
                                  int mouseX, int mouseY) {
        claimVisible = false;
        buyVisible = false;
        depositVisible = false;
        sellVisible = false;
        recycleVisible = false;
        // 部分出售那组控件只在回收子页有效：切回买入页必须清掉命中区
        shopSellVisible = false;
        shopAllVisible = false;
        shopKeepVisible = false;

        var data = ClientCultivationCache.get();
        int tier = data.realm().tier();
        var walletOwner = minecraft == null ? null : minecraft.player;
        int balance = com.hongmeng.xiuxian.shop.ClientLeafWallet.balance();
        int held = com.hongmeng.xiuxian.shop.ClientLeafWallet.heldItems(walletOwner);
        int tokens = com.hongmeng.xiuxian.shop.ClientLeafWallet.tokens(walletOwner);

        // 买入 / 回收 子页签
        shopTabY = ry - 12;
        int sx = rx;
        for (int i = 0; i < SHOP_TAB_NAMES.length; i++) {
            shopTabX[i] = sx;
            boolean active = i == shopTab;
            boolean hover = mouseX >= sx && mouseX <= sx + SHOPTAB_W
                    && mouseY >= shopTabY && mouseY <= shopTabY + SHOPTAB_H;
            g.fill(sx, shopTabY, sx + SHOPTAB_W, shopTabY + SHOPTAB_H,
                    active ? 0xFF3A2A5A : (hover ? 0xFF2A2438 : 0xFF1A1826));
            g.renderOutline(sx, shopTabY, SHOPTAB_W, SHOPTAB_H,
                    active ? BORDER : 0xFF40404A);
            String label = (active ? "§f§l" : "§7") + SHOP_TAB_NAMES[i];
            g.drawString(font, label, sx + (SHOPTAB_W - font.width(label)) / 2,
                    shopTabY + 2, TEXT, false);
            sx += SHOPTAB_W + 4;
        }
        ry += 4;

        if (shopTab == 1) {
            renderSellDetail(g, walletOwner, rx, ry, mouseX, mouseY);
            return;
        }

        int index = Math.max(0, Math.min(shopSelected,
                com.hongmeng.xiuxian.shop.ShopCatalog.size() - 1));
        var sel = com.hongmeng.xiuxian.shop.ShopCatalog.all().get(index);
        boolean unlocked = sel.unlockedFor(tier);

        g.drawString(font, "§f§l" + sel.cn(), rx, ry, TEXT, false);
        ry += 14;
        for (FormattedCharSequence line : font.split(
                Component.literal("§7" + sel.desc()), rw)) {
            g.drawString(font, line, rx, ry, TEXT, false);
            ry += 10;
        }
        ry += 6;

        g.drawString(font, "§b价格  §e" + sel.priceText(), rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, "§b数量  §f" + sel.count() + " §7个 / 份", rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, (unlocked ? "§a✔ §7" : "§c✖ §7") + sel.requirementText(),
                rx, ry, TEXT, false);
        ry += 15;

        if (!unlocked) {
            g.drawString(font, "§8境界不足，无法购买", rx, ry, TEXT, false);
        } else {
            buyBtnW = 104;
            buyBtnH = 18;
            buyBtnX = rx;
            buyBtnY = ry;
            buyVisible = true;
            // 灵叶商品看"余额 + 背包实物"，纪元令商品看背包里的令牌
            boolean afford = com.hongmeng.xiuxian.shop.ClientLeafWallet
                    .heldFor(walletOwner, sel) >= sel.priceLeaves();
            boolean hover = mouseX >= buyBtnX && mouseX <= buyBtnX + buyBtnW
                    && mouseY >= buyBtnY && mouseY <= buyBtnY + buyBtnH;
            g.fill(buyBtnX, buyBtnY, buyBtnX + buyBtnW, buyBtnY + buyBtnH,
                    afford ? (hover ? 0xFF5A8A3E : 0xFF3E6A2A) : 0xFF3A3028);
            g.renderOutline(buyBtnX, buyBtnY, buyBtnW, buyBtnH, afford ? GOLD : 0xFF605850);
            String label = afford ? "§f购买一份" : "§8" + sel.currencyName() + "不足";
            g.drawString(font, label,
                    buyBtnX + (buyBtnW - font.width(label)) / 2, buyBtnY + 5, TEXT, false);

            // 商品图标
            var preview = sel.stack();
            if (!preview.isEmpty()) {
                g.renderItem(preview, rx + buyBtnW + 14, ry - 4);
            }
        }

        // 存入灵叶：背包实物 → 坊市余额（与选中商品无关，随时可点）
        depositBtnW = 126;
        depositBtnH = 16;
        depositBtnX = rx;
        depositBtnY = ry + 24;
        depositVisible = true;
        boolean canDeposit = held > 0;
        boolean dHover = mouseX >= depositBtnX && mouseX <= depositBtnX + depositBtnW
                && mouseY >= depositBtnY && mouseY <= depositBtnY + depositBtnH;
        g.fill(depositBtnX, depositBtnY, depositBtnX + depositBtnW,
                depositBtnY + depositBtnH,
                canDeposit ? (dHover ? 0xFF7A5A2A : 0xFF5A4020) : 0xFF2A2620);
        g.renderOutline(depositBtnX, depositBtnY, depositBtnW, depositBtnH,
                canDeposit ? GOLD : 0xFF605850);
        String dLabel = canDeposit ? "§f存入灵叶 §7×" + held : "§8背包里没有实物灵叶";
        g.drawString(font, dLabel,
                depositBtnX + (depositBtnW - font.width(dLabel)) / 2,
                depositBtnY + 4, TEXT, false);

        // 钱包一览
        int wy = depositBtnY + 22;
        g.drawString(font, "§7灵叶余额 §e" + balance + " §8· §7纪元令 §b" + tokens,
                rx, wy, TEXT, false);
        g.drawString(font, "§8纪元令可在坊市换法宝材料", rx, wy + 10, DIM, false);
    }

    // ------------------------------------------------------------------
    // 坊市 · 回收页
    // ------------------------------------------------------------------

    private void renderSellDetail(GuiGraphics g,
                                  net.minecraft.world.entity.player.Player player,
                                  int rx, int ry, int mouseX, int mouseY) {
        int idx = Math.max(0, Math.min(shopSelected,
                com.hongmeng.xiuxian.shop.SellPrices.size() - 1));
        String id = com.hongmeng.xiuxian.shop.SellPrices.ids().get(idx);
        var rep = representativeSellable(player, id);
        // 品质感知的价：和 ShopScreen、和服务端结算同一个口径
        int unit = com.hongmeng.xiuxian.shop.ShopManager.sellTotal(id, rep, 1);
        int unitBase = com.hongmeng.xiuxian.shop.SellPrices.unitPrice(id);
        int have = countSellable(player, id);

        // 步进器上膛：换了一件东西 → 数字重置为"全部可卖"
        if (!id.equals(shopSellCountItem)) {
            shopSellCountItem = id;
            shopSellCountNeedsInit = true;
        }
        if (shopSellCountNeedsInit) {
            shopSellCount = Math.max(1, have);
            shopSellCountNeedsInit = false;
        }
        shopSellCount = Math.max(1, Math.min(shopSellCount, Math.max(1, have)));

        g.drawString(font, "§f§l回收 · "
                + com.hongmeng.xiuxian.shop.SellPrices.displayName(id), rx, ry, TEXT, false);
        ry += 14;
        g.drawString(font, "§b回收价  §e" + unit + " 灵叶 §7/ 件"
                        + (unit > unitBase ? " §d(品质+" + (unit - unitBase) + ")" : ""),
                rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, "§b背包数量  §f" + have + " §8（不含快捷栏与装备）",
                rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, "§b全部可卖  §e"
                + com.hongmeng.xiuxian.shop.ShopManager.sellTotal(id, rep, have)
                + " 灵叶", rx, ry, TEXT, false);
        ry += 15;

        // ---- 卖 N / 全部 / 保留 N（与坊市界面同一套交互） ----
        int bw = 62;
        boolean canSell = have > 0;
        int afterSell = Math.max(0, have - shopSellCount);
        boolean canKeep = canSell && afterSell > 0;

        shopSellBtnW = bw;
        shopSellBtnH = 18;
        shopSellBtnX = rx;
        shopSellBtnY = ry;
        shopSellVisible = true;
        shopActionButton(g, shopSellBtnX, shopSellBtnY, bw, 18,
                "§f卖 " + shopSellCount,
                canSell ? "§7+" + com.hongmeng.xiuxian.shop.ShopManager
                        .sellTotal(id, rep, Math.min(shopSellCount, have)) : "§8—",
                canSell, mouseX, mouseY);

        shopAllBtnX = rx + bw + 4;
        shopAllBtnY = ry;
        shopAllBtnW = bw;
        shopAllBtnH = 18;
        shopAllVisible = true;
        shopActionButton(g, shopAllBtnX, shopAllBtnY, bw, 18,
                "§f全部",
                canSell ? "§7+" + com.hongmeng.xiuxian.shop.ShopManager
                        .sellTotal(id, rep, have) : "§8—",
                canSell, mouseX, mouseY);

        shopKeepBtnX = rx + (bw + 4) * 2;
        shopKeepBtnY = ry;
        shopKeepBtnW = bw;
        shopKeepBtnH = 18;
        shopKeepVisible = true;
        shopActionButton(g, shopKeepBtnX, shopKeepBtnY, bw, 18,
                "§f保留 " + shopSellCount,
                canKeep ? "§7+" + com.hongmeng.xiuxian.shop.ShopManager
                        .sellTotal(id, rep, afterSell) : "§8—",
                canKeep, mouseX, mouseY);
        ry += 22;

        // 数量步进：− [ N ] ＋（和坊市界面一致：Shift ×10）
        shopMinusX = rx;
        shopMinusY = ry;
        shopPlusX = rx + 46;
        shopPlusY = ry;
        shopCountBoxX = rx + 20;
        shopCountBoxY = ry;
        shopStepButton(g, shopMinusX, shopMinusY, "§f−", canSell, mouseX, mouseY);
        shopStepButton(g, shopPlusX, shopPlusY, "§f+", canSell, mouseX, mouseY);
        g.fill(shopCountBoxX, shopCountBoxY, shopCountBoxX + 26, shopCountBoxY + 16,
                0xFF16131A);
        g.renderOutline(shopCountBoxX, shopCountBoxY, 26, 16, BORDER);
        String num = String.valueOf(shopSellCount);
        g.drawString(font, "§e" + num,
                shopCountBoxX + (26 - font.width(num)) / 2, shopCountBoxY + 4, TEXT, false);
        g.drawString(font, "§8数量（Shift ×10）", rx + 70, ry + 4, DIM, false);
        ry += 20;

        var preview = com.hongmeng.xiuxian.shop.SellPrices.stackOf(id);
        if (!preview.isEmpty()) {
            g.renderItem(preview, rx, ry);
            ry += 18;
        }

        // 一键回收（行为不变：卖光背包里除快捷栏与护甲之外的一切）
        recycleBtnW = 130;
        recycleBtnH = 18;
        recycleBtnX = rx;
        recycleBtnY = ry;
        recycleVisible = true;
        boolean rHover = mouseX >= recycleBtnX && mouseX <= recycleBtnX + recycleBtnW
                && mouseY >= recycleBtnY && mouseY <= recycleBtnY + recycleBtnH;
        g.fill(recycleBtnX, recycleBtnY, recycleBtnX + recycleBtnW,
                recycleBtnY + recycleBtnH,
                rHover ? 0xFF5A8A3E : 0xFF3E6A2A);
        g.renderOutline(recycleBtnX, recycleBtnY, recycleBtnW, recycleBtnH, GOLD);
        String rLabel = "§f一键回收背包";
        g.drawString(font, rLabel,
                recycleBtnX + (recycleBtnW - font.width(rLabel)) / 2,
                recycleBtnY + 5, TEXT, false);

        g.drawString(font, "§8快捷栏与身上的装备不会被卖掉。",
                rx, recycleBtnY + 24, DIM, false);
        g.drawString(font, "§8要卖快捷栏里的东西：拿在手上按 §7H §8卖出手上物品。",
                rx, recycleBtnY + 34, DIM, false);
    }

    /** 数量步进的小按钮（与坊市界面同一套尺寸）。 */
    private void shopStepButton(GuiGraphics g, int x, int y, String label,
                                boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX <= x + 18
                && mouseY >= y && mouseY <= y + 16;
        g.fill(x, y, x + 18, y + 16, enabled ? (hover ? 0xFF7A5A2A : 0xFF5A4020)
                : 0xFF2A2620);
        g.renderOutline(x, y, 18, 16, enabled ? GOLD : 0xFF605850);
        g.drawString(font, label, x + (18 - font.width(label)) / 2, y + 4, TEXT, false);
    }

    /** 动作按钮：上行动作、下行"能得多少叶"。 */
    private void shopActionButton(GuiGraphics g, int x, int y, int w, int h,
                                  String label, String sub, boolean enabled,
                                  int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX <= x + w
                && mouseY >= y && mouseY <= y + h;
        g.fill(x, y, x + w, y + h, enabled ? (hover ? 0xFF8A6A2A : 0xFF6A4E1E)
                : 0xFF2A2620);
        g.renderOutline(x, y, w, h, enabled ? GOLD : 0xFF605850);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 2, TEXT, false);
        g.drawString(font, sub, x + (w - font.width(sub)) / 2, y + 10, TEXT, false);
    }

    /** 列表内容总高度（用于把滚动夹在合理范围）。 */
    private int contentHeight() {
        if (tab == 0) {
            return flat.size() * 11 + chapters.size() * 16 + 8;
        }
        int rows = shopTab == 0
                ? com.hongmeng.xiuxian.shop.ShopCatalog.size()
                : com.hongmeng.xiuxian.shop.SellPrices.size();
        return rows * 13 + 8;
    }

    /** 当前坊市子页签里有多少行。 */
    private int shopRowCount() {
        return shopTab == 0
                ? com.hongmeng.xiuxian.shop.ShopCatalog.size()
                : com.hongmeng.xiuxian.shop.SellPrices.size();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 页签
        if (button == 0 && mouseY >= tabY && mouseY <= tabY + tabH) {
            for (int i = 0; i < TAB_NAMES.length; i++) {
                if (mouseX >= tabX[i] && mouseX <= tabX[i] + tabW) {
                    tab = i;
                    scroll = 0;
                    return true;
                }
            }
        }

        // 坊市页里的 买入 / 回收 子页签
        if (button == 0 && tab == 1
                && mouseY >= shopTabY && mouseY <= shopTabY + SHOPTAB_H) {
            for (int i = 0; i < SHOP_TAB_NAMES.length; i++) {
                if (mouseX >= shopTabX[i] && mouseX <= shopTabX[i] + SHOPTAB_W) {
                    shopTab = i;
                    shopSelected = 0;
                    scroll = 0;
                    return true;
                }
            }
        }

        // 左侧列表点选
        if (mouseX >= listLeft && mouseX <= listLeft + LIST_W
                && mouseY >= listTop && mouseY <= listTop + listHeight) {
            if (tab == 0) {
                int y = listTop + 4 - scroll;
                int i = 0;
                for (Map.Entry<String, List<Quest>> entry : chapters.entrySet()) {
                    y += 12;
                    for (int k = 0; k < entry.getValue().size(); k++) {
                        if (mouseY >= y - 1 && mouseY <= y + 10) {
                            selected = i;
                            return true;
                        }
                        y += 11;
                        i++;
                    }
                    y += 4;
                }
            } else {
                int y = listTop + 4 - scroll;
                for (int i = 0; i < shopRowCount(); i++) {
                    if (mouseY >= y - 1 && mouseY <= y + 11) {
                        shopSelected = i;
                        return true;
                    }
                    y += 13;
                }
            }
            return true;
        }

        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 领取按钮
        if (tab == 0 && claimVisible
                && mouseX >= claimBtnX && mouseX <= claimBtnX + claimBtnW
                && mouseY >= claimBtnY && mouseY <= claimBtnY + claimBtnH) {
            Quest q = flat.get(Math.min(selected, flat.size() - 1));
            com.hongmeng.xiuxian.network.ClaimQuestPacket.send(q.id());
            return true;
        }
        // 购买按钮（坊市·买入页）
        if (tab == 1 && shopTab == 0 && buyVisible
                && mouseX >= buyBtnX && mouseX <= buyBtnX + buyBtnW
                && mouseY >= buyBtnY && mouseY <= buyBtnY + buyBtnH) {
            com.hongmeng.xiuxian.network.ShopBuyPacket.send(shopSelected, 1);
            return true;
        }
        // 卖出一整堆（坊市·回收页）
        if (tab == 1 && shopTab == 1 && sellVisible
                && mouseX >= sellBtnX && mouseX <= sellBtnX + sellBtnW
                && mouseY >= sellBtnY && mouseY <= sellBtnY + sellBtnH) {
            int idx = Math.max(0, Math.min(shopSelected,
                    com.hongmeng.xiuxian.shop.SellPrices.size() - 1));
            com.hongmeng.xiuxian.network.ShopSellPacket.sendEntry(
                    com.hongmeng.xiuxian.shop.SellPrices.ids().get(idx));
            return true;
        }
        // 卖 N / 全部 / 保留 N（坊市·回收页）
        if (tab == 1 && shopTab == 1) {
            String selId = shopSellId();
            if (selId != null) {
                if (shopSellVisible
                        && mouseX >= shopSellBtnX && mouseX <= shopSellBtnX + shopSellBtnW
                        && mouseY >= shopSellBtnY && mouseY <= shopSellBtnY + shopSellBtnH) {
                    com.hongmeng.xiuxian.network.ShopSellPacket.sendCount(selId, shopSellCount);
                    return true;
                }
                if (shopAllVisible
                        && mouseX >= shopAllBtnX && mouseX <= shopAllBtnX + shopAllBtnW
                        && mouseY >= shopAllBtnY && mouseY <= shopAllBtnY + shopAllBtnH) {
                    com.hongmeng.xiuxian.network.ShopSellPacket.sendEntry(selId);
                    return true;
                }
                if (shopKeepVisible
                        && mouseX >= shopKeepBtnX && mouseX <= shopKeepBtnX + shopKeepBtnW
                        && mouseY >= shopKeepBtnY && mouseY <= shopKeepBtnY + shopKeepBtnH) {
                    com.hongmeng.xiuxian.network.ShopSellPacket.sendKeep(selId, shopSellCount);
                    return true;
                }
            }
            if (mouseX >= shopMinusX && mouseX <= shopMinusX + 18
                    && mouseY >= shopMinusY && mouseY <= shopMinusY + 16) {
                shopBumpCount(-(net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 10 : 1));
                return true;
            }
            if (mouseX >= shopPlusX && mouseX <= shopPlusX + 18
                    && mouseY >= shopPlusY && mouseY <= shopPlusY + 16) {
                shopBumpCount(net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 10 : 1);
                return true;
            }
            if (mouseX >= shopCountBoxX && mouseX <= shopCountBoxX + 26
                    && mouseY >= shopCountBoxY && mouseY <= shopCountBoxY + 16) {
                shopSellCount = 1;
                return true;
            }
        }
        // 一键回收（坊市·回收页）
        if (tab == 1 && shopTab == 1 && recycleVisible
                && mouseX >= recycleBtnX && mouseX <= recycleBtnX + recycleBtnW
                && mouseY >= recycleBtnY && mouseY <= recycleBtnY + recycleBtnH) {
            com.hongmeng.xiuxian.network.ShopSellPacket.sendAll();
            return true;
        }
        // 存入灵叶
        if (tab == 1 && shopTab == 0 && depositVisible
                && mouseX >= depositBtnX && mouseX <= depositBtnX + depositBtnW
                && mouseY >= depositBtnY && mouseY <= depositBtnY + depositBtnH) {
            com.hongmeng.xiuxian.network.ShopDepositPacket.send();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 回收子页当前选中的物品 id（越界/空表返回 null）。 */
    private String shopSellId() {
        int n = com.hongmeng.xiuxian.shop.SellPrices.size();
        if (n <= 0) {
            return null;
        }
        int idx = Math.max(0, Math.min(shopSelected, n - 1));
        return com.hongmeng.xiuxian.shop.SellPrices.ids().get(idx);
    }

    /** 调整"卖 N / 保留 N"的数量（夹在 1..可卖数量）。 */
    private void shopBumpCount(int delta) {
        int have = 0;
        var player = minecraft == null ? null : minecraft.player;
        String id = shopSellId();
        if (player != null && id != null) {
            have = countSellable(player, id);
        }
        int max = Math.max(1, have > 0 ? have
                : com.hongmeng.xiuxian.network.ShopSellPacket.MAX_AMOUNT);
        shopSellCount = Math.max(1, Math.min(max, shopSellCount + delta));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, contentHeight() - listHeight);
        scroll -= (int) (delta * 12);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP) {
            if (tab == 0) {
                selected = Math.max(0, selected - 1);
            } else {
                shopSelected = Math.max(0, shopSelected - 1);
            }
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
            if (tab == 0) {
                selected = Math.min(flat.size() - 1, selected + 1);
            } else {
                shopSelected = Math.min(shopRowCount() - 1, shopSelected + 1);
            }
            return true;
        }
        // Tab 切页签
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            tab = (tab + 1) % TAB_NAMES.length;
            scroll = 0;
            return true;
        }
        // 回收子页：← → 调数量（Shift ×10）
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT && tab == 1 && shopTab == 1) {
            shopBumpCount(-(net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 10 : 1));
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT && tab == 1 && shopTab == 1) {
            shopBumpCount(net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 10 : 1);
            return true;
        }
        // 回车：任务页领取，坊市页购买；回收页 = "卖 N"（最保守的那个动作）
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            if (tab == 0 && claimVisible) {
                Quest q = flat.get(Math.min(selected, flat.size() - 1));
                com.hongmeng.xiuxian.network.ClaimQuestPacket.send(q.id());
            } else if (tab == 1 && shopTab == 0 && buyVisible) {
                com.hongmeng.xiuxian.network.ShopBuyPacket.send(shopSelected, 1);
            } else if (tab == 1 && shopTab == 1) {
                String selId = shopSellId();
                if (selId != null) {
                    com.hongmeng.xiuxian.network.ShopSellPacket.sendCount(selId, shopSellCount);
                }
            }
            return true;
        }
        // A：坊市·回收页全部卖出（原来的回车行为，挪到 A 免得手滑）
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_A && tab == 1 && shopTab == 1) {
            String selId = shopSellId();
            if (selId != null) {
                com.hongmeng.xiuxian.network.ShopSellPacket.sendEntry(selId);
            }
            return true;
        }
        // K：坊市·回收页保留 N
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_K && tab == 1 && shopTab == 1) {
            String selId = shopSellId();
            if (selId != null) {
                com.hongmeng.xiuxian.network.ShopSellPacket.sendKeep(selId, shopSellCount);
            }
            return true;
        }
        // D：坊市·买入页把背包里的实物灵叶存进余额
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_D && tab == 1 && shopTab == 0) {
            com.hongmeng.xiuxian.network.ShopDepositPacket.send();
            return true;
        }
        // H：坊市·回收页卖出手上物品（唯一允许卖快捷栏的入口）
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_H && tab == 1 && shopTab == 1) {
            com.hongmeng.xiuxian.network.ShopSellPacket.sendHeld();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_J
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
