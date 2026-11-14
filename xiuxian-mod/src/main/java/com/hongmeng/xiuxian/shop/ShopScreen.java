package com.hongmeng.xiuxian.shop;

import com.hongmeng.xiuxian.network.ClientCultivationCache;
import com.hongmeng.xiuxian.network.ShopBuyPacket;
import com.hongmeng.xiuxian.network.ShopDepositPacket;
import com.hongmeng.xiuxian.network.ShopSellPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 商店界面（`/shop`）。
 *
 * <p>布局沿用任务面板那一套：左栏列表，右栏详情 + 按钮。</p>
 *
 * <h2>买入 / 回收 两个页签</h2>
 * <ul>
 *   <li><b>买入</b>：原来的商品列表，点"购买一份"扣灵叶余额（余额不够自动花
 *       背包里的实物灵叶）；纪元令商品用令牌结算。</li>
 *   <li><b>回收</b>：{@link SellPrices} 价目表。挖到的矿物、掉的材料、
 *       种出来的灵植花瓣、炼废的丹药都能卖，一件一件标价。</li>
 * </ul>
 *
 * <p><b>误卖保护</b>：回收列表的"卖出一堆"与"一键回收"都<b>跳过快捷栏
 * （0..8）与护甲槽（36..39）</b>——那两处是玩家正在用的镐子、武器、法宝与
 * 身上的护甲。想卖快捷栏里的东西只有一条路：拿在手上点"卖出手上物品"，
 * 那是一个明确指向手持物的动作。规则写在 {@link ShopManager#HOTBAR_SIZE}。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ShopScreen extends Screen {

    private static final int W = 320;
    /**
     * 面板高度。
     *
     * <p>原来是 200。回收页加了"部分出售"那一组控件（数量步进 + 卖N / 全部 /
     * 保留N 三个按钮）之后 200 装不下了，抬到 260。宽度不变，所以小分辨率下
     * 的左右边界与原来一致；高度 260 在 320×240 以上的 GUI 缩放里都放得下
     * （原版最小可玩逻辑分辨率是 320×240，正好贴边，且 Screen 会居中，
     * 溢出的是上下空白而不是面板本身被截断）。</p>
     */
    private static final int H = 260;
    private static final int LIST_W = 128;
    private static final int BG = 0xF0141210;
    private static final int PANEL = 0xF01E1A16;
    private static final int BORDER = 0xFFD8A85A;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int DIM = 0xFF9A9088;
    private static final int GOLD = 0xFFFFD24A;
    private static final int GREEN = 0xFF6FE07F;

    /** 页签：0 = 买入，1 = 回收。 */
    private static final String[] TAB_NAMES = {"买入", "回收"};

    private int tab = 0;

    private int selected = 0;
    private int scroll = 0;
    private int buyX;
    private int buyY;
    private int buyW;
    private int buyH;
    private boolean buyVisible = false;
    private int depositX;
    private int depositY;
    private int depositW;
    private int depositH;
    private boolean depositVisible = false;

    /** 回收页：卖出选中的一整堆。 */
    private int sellX;
    private int sellY;
    private int sellW;
    private int sellH;
    private boolean sellVisible = false;
    /** 回收页：卖出手上物品。 */
    private int heldX;
    private int heldY;
    private int heldW;
    private int heldH;
    private boolean heldVisible = false;
    /** 回收页：一键回收。 */
    private int allX;
    private int allY;
    private int allW;
    private int allH;
    private boolean allVisible = false;

    /** 页签按钮命中区。 */
    private final int[] tabX = new int[TAB_NAMES.length];
    private int tabY;
    private static final int TAB_W = 44;
    private static final int TAB_H = 13;

    // ------------------------------------------------------------------
    // 回收页 · 部分出售（"卖 N" / "保留 N"）
    //
    // 玩家原话："商店的东西也可以一键售卖，比如背包里有很多可以选择
    // 全部售卖或者是保留/售卖多少"。
    //
    // 交互：一个数字步进器（− / 数字 / ＋，点 − ＋ 各挪 1；按住 Shift 挪 10），
    // 下面三个按钮把**同一个数字**派上三种用场：
    //   卖 N   → 卖这么多
    //   全部   → 全卖（原来的行为，一键回收除外）
    //   保留 N → 全卖，但留下 N 个
    // 这样只需要一个数字，玩家不用在"卖多少"和"留多少"之间切模式。
    // ------------------------------------------------------------------

    /** 要卖 / 要留的数量。默认 1。 */
    private int sellCount = 1;

    /** 步进器命中区（每帧渲染时更新）。 */
    private int stepMinusX;
    private int stepMinusY;
    private int stepPlusX;
    private int stepPlusY;
    private static final int STEP_W = 18;
    private static final int STEP_H = 16;
    /** 数字框：点它当成"重置为 1"。 */
    private int stepValX;
    private int stepValY;
    private int stepValW;

    /** 回收页三个动作按钮的命中区。 */
    private int cntSellX;
    private int cntSellY;
    private int cntSellW;
    private int cntSellH;
    private boolean cntSellVisible = false;
    private int cntAllX;
    private int cntAllY;
    private int cntAllW;
    private int cntAllH;
    private boolean cntAllVisible = false;
    private int cntKeepX;
    private int cntKeepY;
    private int cntKeepW;
    private int cntKeepH;
    private boolean cntKeepVisible = false;

    /**
     * 数字要不要跟"可卖数量"走。
     *
     * <p>第一次打开某一项时把数字设成"全部可卖"，并把这个标记清掉；
     * 之后玩家自己调过就尊重玩家的输入。切换选中项时会重新上膛。</p>
     */
    private boolean sellCountNeedsInit = true;
    /** 上一次上膛所对应的物品 id，用来判断"换东西了"。 */
    private String sellCountItem = "";

    private static final int CNT_H = 16;

    public ShopScreen() {
        super(Component.literal("坊市"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, BG);

        var data = ClientCultivationCache.get();
        int tier = data.realm().tier();
        var walletOwner = minecraft == null ? null : minecraft.player;
        int balance = ClientLeafWallet.balance();
        int held = ClientLeafWallet.heldItems(walletOwner);
        int tokens = ClientLeafWallet.tokens(walletOwner);

        // 标题 + 页签 + 余额（余额是主角，右上角显著位置）
        g.drawString(font, "§6§l坊市 §7· §f灵叶交易", left + 8, top + 7, TEXT, false);

        tabY = top + 4;
        int tx = left + 108;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            tabX[i] = tx;
            boolean active = i == tab;
            boolean hover = mouseX >= tx && mouseX <= tx + TAB_W
                    && mouseY >= tabY && mouseY <= tabY + TAB_H;
            g.fill(tx, tabY, tx + TAB_W, tabY + TAB_H,
                    active ? 0xFF5A4020 : (hover ? 0xFF322A22 : 0xFF201C18));
            g.renderOutline(tx, tabY, TAB_W, TAB_H, active ? BORDER : 0xFF55483A);
            String label = (active ? "§f§l" : "§7") + TAB_NAMES[i];
            g.drawString(font, label, tx + (TAB_W - font.width(label)) / 2, tabY + 3,
                    TEXT, false);
            tx += TAB_W + 4;
        }

        String wallet = held > 0
                ? "§7灵叶余额：§e" + balance + " §8(背包 " + held + ")"
                : "§7灵叶余额：§e" + balance;
        g.drawString(font, wallet, left + W - 8 - font.width(wallet), top + 8, TEXT, false);
        g.fill(left, top + 20, left + W, top + 21, 0x66D8A85A);

        // 左栏
        int listLeft = left + 4;
        int listTop = top + 24;
        int listHeight = H - 30;
        g.fill(listLeft, listTop, listLeft + LIST_W, listTop + listHeight, PANEL);

        if (tab == 0) {
            renderBuyList(g, tier, listLeft, listTop, listHeight);
        } else {
            renderSellList(g, walletOwner, listLeft, listTop, listHeight);
        }

        // 右栏
        int rx = left + LIST_W + 12;
        int ry = top + 28;
        int rw = W - LIST_W - 20;

        if (tab == 0) {
            renderBuyDetail(g, tier, walletOwner, rx, ry, rw, mouseX, mouseY);
        } else {
            renderSellDetail(g, walletOwner, rx, ry, rw, mouseX, mouseY);
        }

        String hint = tab == 0
                ? "§8↑↓ 选择 · 点击购买 · D 存入灵叶 · Tab 切页签 · /shop 开关"
                : "§8↑↓ 选择 · 点 ± 调数量(Shift×10) · 卖N/全部/保留N · ←→ 调数量 · H 卖出手上 · Tab 切页签";
        g.drawString(font, hint, left + 8, top + H - 12, DIM, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------------
    // 买入页
    // ------------------------------------------------------------------

    private void renderBuyList(GuiGraphics g, int tier, int listLeft, int listTop, int listHeight) {
        int y = listTop + 4 - scroll;
        for (int i = 0; i < ShopCatalog.size(); i++) {
            ShopEntry e = ShopCatalog.all().get(i);
            boolean unlocked = e.unlockedFor(tier);
            boolean isSel = i == selected;
            if (y + 12 > listTop && y < listTop + listHeight) {
                if (isSel) {
                    g.fill(listLeft + 1, y - 1, listLeft + LIST_W - 1, y + 12, 0x40FFFFFF);
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

    private void renderBuyDetail(GuiGraphics g, int tier, net.minecraft.world.entity.player.Player walletOwner,
                                 int rx, int ry, int rw, int mouseX, int mouseY) {
        sellVisible = false;
        heldVisible = false;
        allVisible = false;
        // 部分出售那组控件只在回收页有效：离开回收页必须清掉命中区，
        // 否则在买入页点到同一个坐标会误触
        cntSellVisible = false;
        cntAllVisible = false;
        cntKeepVisible = false;

        ShopEntry sel = ShopCatalog.all().get(
                Math.max(0, Math.min(selected, ShopCatalog.size() - 1)));
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

        // 购买按钮
        buyVisible = false;
        if (!unlocked) {
            g.drawString(font, "§8境界不足，无法购买", rx, ry, TEXT, false);
        } else {
            buyW = 110;
            buyH = 18;
            buyX = rx;
            buyY = ry;
            buyVisible = true;
            // 灵叶商品看"余额 + 背包实物"，纪元令商品看背包里的令牌
            boolean afford = ClientLeafWallet.heldFor(walletOwner, sel) >= sel.priceLeaves();
            boolean hover = mouseX >= buyX && mouseX <= buyX + buyW
                    && mouseY >= buyY && mouseY <= buyY + buyH;
            int color = afford ? (hover ? 0xFF5A8A3E : 0xFF3E6A2A) : 0xFF3A3028;
            g.fill(buyX, buyY, buyX + buyW, buyY + buyH, color);
            g.renderOutline(buyX, buyY, buyW, buyH, afford ? GOLD : 0xFF605850);
            String label = afford ? "§f购买一份" : "§8" + sel.currencyName() + "不足";
            g.drawString(font, label,
                    buyX + (buyW - font.width(label)) / 2, buyY + 5, TEXT, false);

            // 预览物品图标
            ItemStack preview = sel.stack();
            if (!preview.isEmpty()) {
                g.renderItem(preview, rx + buyW + 12, ry - 4);
            }
        }

        // 存入灵叶：把背包里的实物灵叶转成坊市余额（跟选中哪件商品无关）
        depositW = 132;
        depositH = 16;
        depositX = rx;
        depositY = ry + 24;
        depositVisible = true;
        boolean canDeposit = heldItems(walletOwner) > 0;
        boolean dHover = mouseX >= depositX && mouseX <= depositX + depositW
                && mouseY >= depositY && mouseY <= depositY + depositH;
        g.fill(depositX, depositY, depositX + depositW, depositY + depositH,
                canDeposit ? (dHover ? 0xFF7A5A2A : 0xFF5A4020) : 0xFF2A2620);
        g.renderOutline(depositX, depositY, depositW, depositH,
                canDeposit ? GOLD : 0xFF605850);
        String dLabel = canDeposit
                ? "§f存入灵叶 §7×" + heldItems(walletOwner) : "§8背包里没有实物灵叶";
        g.drawString(font, dLabel,
                depositX + (depositW - font.width(dLabel)) / 2, depositY + 4, TEXT, false);

        // 钱包一览
        int wy = depositY + 22;
        g.drawString(font, walletLine(balance(), walletOwner), rx, wy, TEXT, false);
        g.drawString(font, "§8灵叶来源：杀怪 / 采灵草 / 任务 / 回收页卖东西", rx, wy + 10, DIM, false);
    }

    // ------------------------------------------------------------------
    // 回收页
    // ------------------------------------------------------------------

    private void renderSellList(GuiGraphics g, net.minecraft.world.entity.player.Player player,
                                int listLeft, int listTop, int listHeight) {
        int y = listTop + 4 - scroll;
        for (int i = 0; i < SellPrices.size(); i++) {
            int price = SellPrices.all().get(SellPrices.ids().get(i));
            boolean isSel = i == selected;
            if (y + 12 > listTop && y < listTop + listHeight) {
                if (isSel) {
                    g.fill(listLeft + 1, y - 1, listLeft + LIST_W - 1, y + 12, 0x40FFFFFF);
                }
                String id = SellPrices.ids().get(i);
                int have = countOf(player, id);
                String name = (isSel ? "§f§l" : "§f") + SellPrices.displayName(id);
                g.drawString(font, name, listLeft + 6, y, TEXT, false);
                String right = (have > 0 ? "§a" + price + "叶" : "§6" + price + "叶")
                        + (have > 0 ? " §7×" + have : "");
                g.drawString(font, right,
                        listLeft + LIST_W - 8 - font.width(right), y, TEXT, false);
            }
            y += 13;
        }
    }

    private void renderSellDetail(GuiGraphics g, net.minecraft.world.entity.player.Player player,
                                  int rx, int ry, int rw, int mouseX, int mouseY) {
        buyVisible = false;
        depositVisible = false;

        int idx = Math.max(0, Math.min(selected, SellPrices.size() - 1));
        String id = SellPrices.ids().get(idx);
        ItemStack repStack = representativeStack(player, id);
        int have = countOf(player, id);

        // ---- 品质感知的单价与总价 ----
        //
        // 以前这里用的是 SellPrices.unitPrice(id)（只有基础/凡品价），
        // 而服务端结算走的是品质重载——上品矿在界面上显示 2 叶、实际到账 5 叶。
        // 现在两边同一个口径：界面读玩家背包里那一堆的品质。
        int unit = ShopManager.sellTotal(id, repStack, 1);
        int unitBase = SellPrices.unitPrice(id);
        boolean qualityUp = unit > unitBase;

        // 步进器的数字：换了一件东西就重新上膛成"全部可卖"
        if (!id.equals(sellCountItem)) {
            sellCountItem = id;
            sellCountNeedsInit = true;
        }
        if (sellCountNeedsInit) {
            sellCount = Math.max(1, have);
            sellCountNeedsInit = false;
        }
        sellCount = Math.max(1, Math.min(sellCount, Math.max(1, have)));

        g.drawString(font, "§f§l回收 · " + SellPrices.displayName(id), rx, ry, TEXT, false);
        ry += 14;
        g.drawString(font, "§b回收价  §e" + unit + " 灵叶 §7/ 件"
                + (qualityUp ? " §d(品质+" + (unit - unitBase) + ")" : ""),
                rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, "§b背包数量  §f" + have + " §8（不含快捷栏与身上的装备）",
                rx, ry, TEXT, false);
        ry += 11;
        g.drawString(font, "§b全部可卖  §e" + ShopManager.sellTotal(id, repStack, have)
                + " 灵叶", rx, ry, TEXT, false);
        ry += 15;

        // ---- 数字步进器：− [ N ] ＋ ----
        boolean canSell = have > 0;
        stepMinusX = rx;
        stepMinusY = ry;
        stepPlusX = rx + 58;
        stepPlusY = ry;
        stepValX = rx + STEP_W;
        stepValY = ry;
        stepValW = 40;

        stepButton(g, stepMinusX, stepMinusY, "§f−", canSell, mouseX, mouseY);
        stepButton(g, stepPlusX, stepPlusY, "§f+", canSell, mouseX, mouseY);

        g.fill(stepValX, stepValY, stepValX + stepValW, stepValY + STEP_H, 0xFF16131A);
        g.renderOutline(stepValX, stepValY, stepValW, STEP_H, BORDER);
        String num = String.valueOf(sellCount);
        g.drawString(font, "§e" + num,
                stepValX + (stepValW - font.width(num)) / 2, stepValY + 4, TEXT, false);
        g.drawString(font, "§8− / + 调数量（Shift ×10）· 点数字归 1",
                rx + 82, ry + 4, DIM, false);
        ry += STEP_H + 4;

        // ---- 三个动作按钮：卖 N / 全部 / 保留 N ----
        //
        // 三个按钮把**同一个数字**派上三种用场：
        //   卖 N   = 卖这么多
        //   全部   = 全卖
        //   保留 N = 全卖，但留下 N 个
        // 玩家不用在"卖多少"和"留多少"之间切模式，一个数字够了。
        int afterSell = Math.max(0, have - sellCount);       // "保留 N" 之后剩下的
        boolean canSellN = canSell && sellCount > 0;
        boolean canKeep = canSell && afterSell > 0;

        int bw = (rw - 8) / 3;
        cntSellX = rx;
        cntSellY = ry;
        cntSellW = bw;
        cntSellH = CNT_H;
        cntSellVisible = true;
        actionButton(g, cntSellX, cntSellY, cntSellW, cntSellH,
                canSellN ? "§f卖 " + sellCount : "§8卖 0",
                canSellN ? "§7+" + ShopManager.sellTotal(id, repStack, sellCount) : "§8—",
                canSellN, canSellN ? 0xFF6A4E1E : 0xFF2A2620, mouseX, mouseY);

        cntAllX = rx + bw + 4;
        cntAllY = ry;
        cntAllW = bw;
        cntAllH = CNT_H;
        cntAllVisible = true;
        actionButton(g, cntAllX, cntAllY, cntAllW, cntAllH,
                canSell ? "§f全部" : "§8全部",
                canSell ? "§7+" + ShopManager.sellTotal(id, repStack, have) : "§8—",
                canSell, canSell ? 0xFF6A4E1E : 0xFF2A2620, mouseX, mouseY);

        cntKeepX = rx + (bw + 4) * 2;
        cntKeepY = ry;
        cntKeepW = bw;
        cntKeepH = CNT_H;
        cntKeepVisible = true;
        actionButton(g, cntKeepX, cntKeepY, cntKeepW, cntKeepH,
                canKeep ? "§f保留 " + sellCount : "§8保留",
                canKeep ? "§7+" + ShopManager.sellTotal(id, repStack, afterSell) : "§8—",
                canKeep, canKeep ? 0xFF2E4A6A : 0xFF2A2620, mouseX, mouseY);
        ry += CNT_H + 4;

        // 物品图标
        ItemStack preview = SellPrices.stackOf(id);
        if (!preview.isEmpty()) {
            g.renderItem(preview, rx, ry);
            ry += 18;
        }

        // ---- 卖出手上物品（唯一允许卖快捷栏的入口） ----
        int half = (rw - 6) / 2;
        heldW = half;
        heldH = 16;
        heldX = rx;
        heldY = ry;
        heldVisible = true;
        String heldName = heldName(player);
        boolean hHover = mouseX >= heldX && mouseX <= heldX + heldW
                && mouseY >= heldY && mouseY <= heldY + heldH;
        g.fill(heldX, heldY, heldX + heldW, heldY + heldH,
                hHover ? 0xFF7A5A2A : 0xFF5A4020);
        g.renderOutline(heldX, heldY, heldW, heldH, GOLD);
        String hLabel = font.plainSubstrByWidth(
                "§f卖出手上 " + (heldName.isEmpty() ? "（空手）" : heldName), heldW - 8);
        g.drawString(font, hLabel, heldX + 4, heldY + 4, TEXT, false);

        // ---- 一键回收（原来的行为，一行不减） ----
        allW = half;
        allH = 18;
        allX = rx + half + 6;
        allY = heldY - 1;
        allVisible = true;
        boolean aHover = mouseX >= allX && mouseX <= allX + allW
                && mouseY >= allY && mouseY <= allY + allH;
        g.fill(allX, allY, allX + allW, allY + allH,
                aHover ? 0xFF5A8A3E : 0xFF3E6A2A);
        g.renderOutline(allX, allY, allW, allH, GOLD);
        String aLabel = "§f一键回收 §7(不含快捷栏)";
        g.drawString(font, aLabel,
                allX + (allW - font.width(aLabel)) / 2, allY + 5, TEXT, false);
        ry = Math.max(heldY + heldH, allY + allH) + 6;

        g.drawString(font, walletLine(balance(), player), rx, ry, TEXT, false);
        g.drawString(font, "§8快捷栏与身上的装备不会被卖掉；要卖请拿在手上。",
                rx, ry + 10, DIM, false);
    }

    /** 步进器上的 − / ＋ 小按钮。 */
    private void stepButton(GuiGraphics g, int x, int y, String label,
                            boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX <= x + STEP_W
                && mouseY >= y && mouseY <= y + STEP_H;
        g.fill(x, y, x + STEP_W, y + STEP_H,
                enabled ? (hover ? 0xFF7A5A2A : 0xFF5A4020) : 0xFF2A2620);
        g.renderOutline(x, y, STEP_W, STEP_H, enabled ? GOLD : 0xFF605850);
        g.drawString(font, label, x + (STEP_W - font.width(label)) / 2, y + 4, TEXT, false);
    }

    /** 动作按钮：上行标题、下行"能得多少叶"，一眼看清后果再点。 */
    private void actionButton(GuiGraphics g, int x, int y, int w, int h,
                              String label, String sub, boolean enabled,
                              int baseColor, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX <= x + w
                && mouseY >= y && mouseY <= y + h;
        g.fill(x, y, x + w, y + h, enabled ? (hover ? 0xFF8A6A2A : baseColor) : 0xFF2A2620);
        g.renderOutline(x, y, w, h, enabled ? GOLD : 0xFF605850);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 2, TEXT, false);
        g.drawString(font, sub, x + (w - font.width(sub)) / 2, y + 10, TEXT, false);
    }

    /**
     * 取"代表这一种东西的那一堆"——用来算品质价。
     *
     * <p>和 {@code ShopManager.sellEntry} 的结算顺序一致（第一堆先卖），
     * 所以界面上显示的价就是真正到账的价。</p>
     */
    private ItemStack representativeStack(net.minecraft.world.entity.player.Player player,
                                          String itemId) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack proto = SellPrices.stackOf(itemId);
        if (proto.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ShopManager.sellsProtected(i)) {
                continue;
            }
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(proto.getItem())) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private int balance() {
        return ClientLeafWallet.balance();
    }

    private int heldItems(net.minecraft.world.entity.player.Player player) {
        return ClientLeafWallet.heldItems(player);
    }

    private String walletLine(int balance, net.minecraft.world.entity.player.Player player) {
        return "§7灵叶余额 §e" + balance + " §8· §7纪元令 §b"
                + ClientLeafWallet.tokens(player);
    }

    /** 背包里某物品的数量（回收页显示用，快捷栏与护甲不算）。 */
    private int countOf(net.minecraft.world.entity.player.Player player, String itemId) {
        if (player == null) {
            return 0;
        }
        ItemStack proto = SellPrices.stackOf(itemId);
        if (proto.isEmpty()) {
            return 0;
        }
        int total = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ShopManager.sellsProtected(i)) {
                continue;
            }
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(proto.getItem())) {
                total += s.getCount();
            }
        }
        return total;
    }

    /** 手上物品的名字（空手返回空串）。 */
    private String heldName(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return "";
        }
        ItemStack s = player.getMainHandItem();
        return s.isEmpty() ? "" : s.getHoverName().getString();
    }

    /** 当前页签的列表内容高度。 */
    private int contentHeight() {
        return (tab == 0 ? ShopCatalog.size() : SellPrices.size()) * 13 + 8;
    }

    // ------------------------------------------------------------------
    // 输入
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 页签
        if (button == 0 && mouseY >= tabY && mouseY <= tabY + TAB_H) {
            for (int i = 0; i < TAB_NAMES.length; i++) {
                if (mouseX >= tabX[i] && mouseX <= tabX[i] + TAB_W) {
                    tab = i;
                    scroll = 0;
                    return true;
                }
            }
        }

        int left = (width - W) / 2;
        int top = (height - H) / 2;
        int listLeft = left + 4;
        int listTop = top + 24;
        int listHeight = H - 30;

        if (mouseX >= listLeft && mouseX <= listLeft + LIST_W
                && mouseY >= listTop && mouseY <= listTop + listHeight) {
            int y = listTop + 4 - scroll;
            int n = tab == 0 ? ShopCatalog.size() : SellPrices.size();
            for (int i = 0; i < n; i++) {
                if (mouseY >= y - 1 && mouseY <= y + 12) {
                    selected = i;
                    return true;
                }
                y += 13;
            }
            return true;
        }

        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 买入页
        if (tab == 0) {
            if (buyVisible
                    && mouseX >= buyX && mouseX <= buyX + buyW
                    && mouseY >= buyY && mouseY <= buyY + buyH) {
                ShopBuyPacket.send(selected, 1);
                return true;
            }
            if (depositVisible
                    && mouseX >= depositX && mouseX <= depositX + depositW
                    && mouseY >= depositY && mouseY <= depositY + depositH) {
                ShopDepositPacket.send();
                return true;
            }
        } else {
            // 回收页
            if (sellVisible
                    && mouseX >= sellX && mouseX <= sellX + sellW
                    && mouseY >= sellY && mouseY <= sellY + sellH) {
                int idx = Math.max(0, Math.min(selected, SellPrices.size() - 1));
                ShopSellPacket.sendEntry(SellPrices.ids().get(idx));
                return true;
            }
            if (heldVisible
                    && mouseX >= heldX && mouseX <= heldX + heldW
                    && mouseY >= heldY && mouseY <= heldY + heldH) {
                ShopSellPacket.sendHeld();
                return true;
            }
            if (allVisible
                    && mouseX >= allX && mouseX <= allX + allW
                    && mouseY >= allY && mouseY <= allY + allH) {
                ShopSellPacket.sendAll();
                return true;
            }
            // ---- 数量步进器 ----
            if (mouseX >= stepMinusX && mouseX <= stepMinusX + STEP_W
                    && mouseY >= stepMinusY && mouseY <= stepMinusY + STEP_H) {
                bumpSellCount(-(shiftHeld() ? 10 : 1));
                return true;
            }
            if (mouseX >= stepPlusX && mouseX <= stepPlusX + STEP_W
                    && mouseY >= stepPlusY && mouseY <= stepPlusY + STEP_H) {
                bumpSellCount(shiftHeld() ? 10 : 1);
                return true;
            }
            if (mouseX >= stepValX && mouseX <= stepValX + stepValW
                    && mouseY >= stepValY && mouseY <= stepValY + STEP_H) {
                sellCount = 1;
                return true;
            }
            // ---- 卖 N / 全部 / 保留 N ----
            String selId = selectedSellId();
            if (selId == null) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            if (cntSellVisible
                    && mouseX >= cntSellX && mouseX <= cntSellX + cntSellW
                    && mouseY >= cntSellY && mouseY <= cntSellY + cntSellH) {
                ShopSellPacket.sendCount(selId, sellCount);
                return true;
            }
            if (cntAllVisible
                    && mouseX >= cntAllX && mouseX <= cntAllX + cntAllW
                    && mouseY >= cntAllY && mouseY <= cntAllY + cntAllH) {
                ShopSellPacket.sendEntry(selId);
                return true;
            }
            if (cntKeepVisible
                    && mouseX >= cntKeepX && mouseX <= cntKeepX + cntKeepW
                    && mouseY >= cntKeepY && mouseY <= cntKeepY + cntKeepH) {
                ShopSellPacket.sendKeep(selId, sellCount);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 回收页当前选中的物品 id（越界返回 null）。 */
    private String selectedSellId() {
        if (SellPrices.size() <= 0) {
            return null;
        }
        int idx = Math.max(0, Math.min(selected, SellPrices.size() - 1));
        return SellPrices.ids().get(idx);
    }

    /**
     * 调整数量。
     *
     * <p>上下限：最小 1，最大取"背包里可卖的数量"（没有就放到
     * {@link ShopSellPacket#MAX_AMOUNT}）。夹上限是为了让"卖 N"与"保留 N"
     * 永远有意义——玩家不会填出一个按下去什么也不发生的数字。</p>
     */
    private void bumpSellCount(int delta) {
        int have = 0;
        var player = minecraft == null ? null : minecraft.player;
        String id = selectedSellId();
        if (player != null && id != null) {
            have = countOf(player, id);
        }
        int max = Math.max(1, have > 0 ? have : ShopSellPacket.MAX_AMOUNT);
        sellCount = Math.max(1, Math.min(max, sellCount + delta));
    }

    /** 左/右 Shift 是否按下（用于 ×10 步进）。 */
    private static boolean shiftHeld() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    @Override    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, contentHeight() - (H - 30));
        scroll -= (int) (delta * 12);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int n = tab == 0 ? ShopCatalog.size() : SellPrices.size();
        if (n <= 0) {
            n = 1;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP) {
            selected = (selected - 1 + n) % n;
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
            selected = (selected + 1) % n;
            return true;
        }
        // 回收页：← → 调数量（Shift ×10）
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT && tab == 1) {
            bumpSellCount(-(shiftHeld() ? 10 : 1));
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT && tab == 1) {
            bumpSellCount(shiftHeld() ? 10 : 1);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            tab = (tab + 1) % TAB_NAMES.length;
            scroll = 0;
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            if (tab == 0) {
                ShopBuyPacket.send(selected, 1);
            } else {
                // 回车 = "卖 N"（不是全卖）。全卖请按 A 或点「全部」按钮——
                // 回车是最容易手滑的键，让它干最保守的那件事。
                String id = selectedSellId();
                if (id != null) {
                    ShopSellPacket.sendCount(id, sellCount);
                }
            }
            return true;
        }
        // 回收页：A = 全部卖出（原来的回车行为）
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_A && tab == 1) {
            String id = selectedSellId();
            if (id != null) {
                ShopSellPacket.sendEntry(id);
            }
            return true;
        }
        // 回收页：K = 保留 N
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_K && tab == 1) {
            String id = selectedSellId();
            if (id != null) {
                ShopSellPacket.sendKeep(id, sellCount);
            }
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_D && tab == 0) {
            ShopDepositPacket.send();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_H && tab == 1) {
            ShopSellPacket.sendHeld();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
