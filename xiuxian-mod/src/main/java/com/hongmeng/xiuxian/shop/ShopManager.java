package com.hongmeng.xiuxian.shop;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.quality.ItemQuality;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 商店的服务端逻辑。
 *
 * <p>所有校验都在服务端做——客户端只发"我要买第几条"，不传价格也不传物品，
 * 避免被改包。</p>
 *
 * <h2>灵叶 = 存在坊市的余额</h2>
 * <p>灵叶以前是纯物品，杀怪／任务奖励一片片塞进背包，背包很快被占满，
 * 玩家抱怨"灵叶不用在背包里放着"。现在：</p>
 * <ul>
 *   <li>{@link #giveLeaves} 直接<b>加余额</b>（存在 {@link CultivationData#getLeaves()}，随存档走）；</li>
 *   <li>{@link #leavesOf} = 余额 + 背包里的实物灵叶（老存档与采灵草掉的实物照样能花）；</li>
 *   <li>{@link #depositAll} 把背包实物一次性转成余额（界面上的"存入灵叶"按钮）；</li>
 *   <li>{@link #spendLeaves} <b>先扣余额、再扣实物</b>，所以存不存都能买东西。</li>
 * </ul>
 */
public final class ShopManager {

    private ShopManager() {
    }

    // ------------------------------------------------------------------
    // 灵叶：余额 + 实物
    // ------------------------------------------------------------------

    /** 坊市余额（存在玩家数据里的那部分）。 */
    public static int balanceOf(ServerPlayer player) {
        return CultivationCapability.get(player).getLeaves();
    }

    /** 背包里的实物灵叶数量。 */
    public static int heldItems(ServerPlayer player) {
        return countItems(player, ModItems.LINGYE.get());
    }

    /** 玩家能花的灵叶总数 = 坊市余额 + 背包实物。 */
    public static int leavesOf(ServerPlayer player) {
        return balanceOf(player) + heldItems(player);
    }

    /**
     * 消耗灵叶。先扣余额，余额不够再扣背包实物。
     *
     * @return true 表示扣够了
     */
    public static boolean spendLeaves(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (leavesOf(player) < amount) {
            return false;
        }
        CultivationData data = CultivationCapability.get(player);
        int remaining = amount;

        // 1) 余额优先——这是"钱"，花掉不留痕迹
        int fromBalance = Math.min(data.getLeaves(), remaining);
        if (fromBalance > 0) {
            data.takeLeaves(fromBalance);
            remaining -= fromBalance;
        }

        // 2) 余额不够才动背包里的实物（老存档、采灵草掉的那些）
        if (remaining > 0) {
            consumeItems(player, ModItems.LINGYE.get(), remaining);
        }
        return true;
    }

    /**
     * 给灵叶。
     *
     * <p><b>现在直接进余额，不再往背包里塞物品</b>——杀怪、任务、副本奖励都走这里，
     * 背包不会再被灵叶占满。想要实物（合成要用）就在坊市里"取出灵叶"。</p>
     *
     * @return 发放后的余额
     */
    public static int giveLeaves(ServerPlayer player, int amount) {
        CultivationData data = CultivationCapability.get(player);
        if (amount <= 0) {
            return data.getLeaves();
        }
        return data.addLeaves(amount);
    }

    /**
     * 把背包里的实物灵叶全部存入坊市余额。
     *
     * @return 实际存入的数量（0 = 背包里没有实物灵叶）
     */
    public static int depositAll(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        int moved = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(ModItems.LINGYE.get())) {
                continue;
            }
            moved += stack.getCount();
            stack.setCount(0);
        }
        if (moved > 0) {
            syncInventory(player);
            data.addLeaves(moved);
            player.sendSystemMessage(Component.literal(
                    "\u00a7a已存入 \u00a7e" + moved + " \u00a7a片灵叶，当前余额 \u00a7e"
                            + data.getLeaves() + " \u00a7a片。"));
            player.playNotifySound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.4F);
        } else {
            player.sendSystemMessage(Component.literal(
                    "\u00a77背包里没有实物灵叶可存（采灵草掉的才是实物）。"));
        }
        return moved;
    }

    /** 数一数玩家身上有多少个某种物品（含背包与副手）。 */
    public static int countItems(ServerPlayer player, Item item) {
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** 消耗若干个某种物品。调用前请先用 {@link #countItems} 确认够。 */
    private static boolean consumeItems(ServerPlayer player, Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (countItems(player, item) < amount) {
            return false;
        }
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        syncInventory(player);
        return true;
    }

    /**
     * 改完背包之后把变化推给客户端。
     *
     * <p>{@code Inventory.setChanged()} 只把 {@code timesChanged} 加一，本身不同步任何东西；
     * 客户端要立刻看到"灵叶从背包消失"必须让容器菜单广播一次槽位变化。</p>
     */
    private static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
    }

    // ------------------------------------------------------------------
    // 购买
    // ------------------------------------------------------------------

    /**
     * 购买。
     *
     * <p>价格与货币都从 {@link ShopCatalog} 查表：灵叶商品扣余额／实物，
     * 纪元令商品扣背包里的纪元令（用于换炼制法宝的材料）。</p>
     *
     * @return true 表示成交
     */
    public static boolean buy(ServerPlayer player, int entryIndex, int count) {
        ShopEntry entry = ShopCatalog.byIndex(entryIndex);
        if (entry == null) {
            player.sendSystemMessage(Component.literal("\u00a7c没有这件商品。"));
            return false;
        }
        int qty = Math.max(1, Math.min(64, count));

        CultivationData data = CultivationCapability.get(player);
        int tier = data.realm().tier();
        if (!entry.unlockedFor(tier)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c境界不够，买不了「" + entry.cn() + "」。"));
            player.sendSystemMessage(Component.literal(
                    "\u00a77需要：\u00a7e" + entry.requirementText()
                            + "\u00a77，你现在是 \u00a7f" + data.realm().cn()));
            return false;
        }

        // 先看背包塞不塞得下，避免扣了钱东西掉了
        ItemStack proto = entry.stack();
        if (proto.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7c这件商品配置有误，请联系作者。"));
            return false;
        }
        int totalCount = proto.getCount() * qty;
        if (!hasRoomFor(player, proto, totalCount)) {
            player.sendSystemMessage(Component.literal("\u00a7c背包满了，先腾出位置。"));
            return false;
        }

        int price = entry.priceLeaves() * qty;

        // ---- 扣钱 ----
        if (entry.usesLeaves()) {
            int have = leavesOf(player);
            if (have < price) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c灵叶不够。「" + entry.cn() + "」需要 \u00a7e" + price
                                + " \u00a7c片，你有 \u00a7f" + have + " \u00a7c片。"));
                player.sendSystemMessage(Component.literal(
                        "\u00a78灵叶来源：杀怪（品阶越高掉得越多）、采灵草、任务与副本奖励。"));
                player.sendSystemMessage(Component.literal(
                        "\u00a78背包里的实物灵叶也能直接花；没存的话点坊市里的「存入灵叶」。"));
                return false;
            }
            if (!spendLeaves(player, price)) {
                return false;
            }
        } else {
            Item currency = entry.resolveCurrencyItem();
            if (currency == net.minecraft.world.item.Items.AIR) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c这件商品的货币配置有误，请联系作者。"));
                return false;
            }
            int have = countItems(player, currency);
            if (have < price) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c" + entry.currencyName() + "不够。「" + entry.cn()
                                + "」需要 \u00a7e" + price + " \u00a7c枚，你有 \u00a7f"
                                + have + " \u00a7c枚。"));
                player.sendSystemMessage(Component.literal(
                        "\u00a78纪元令来源：通关秘境、纪元秘境奖励、击杀 BOSS。"));
                return false;
            }
            if (!consumeItems(player, currency, price)) {
                return false;
            }
        }

        // ---- 给货 ----
        int remaining = totalCount;
        int perStack = Math.max(1, proto.getMaxStackSize());
        while (remaining > 0) {
            int give = Math.min(remaining, perStack);
            ItemStack stack = proto.copyWithCount(give);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            remaining -= give;
        }

        int left = entry.usesLeaves() ? leavesOf(player) : countItems(player, entry.resolveCurrencyItem());
        player.sendSystemMessage(Component.literal(
                "\u00a7a买入 \u00a7f" + entry.cn() + " \u00a77×" + qty
                        + " \u00a77花费 \u00a7e" + price + " " + entry.currencyName()
                        + "\u00a78（余 " + left + "）"));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.VILLAGER_TRADE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    /** 背包能不能塞下 totalCount 个 proto（空格算整堆，同类堆算剩余空间）。 */
    private static boolean hasRoomFor(ServerPlayer player, ItemStack proto, int totalCount) {
        int perStack = Math.max(1, proto.getMaxStackSize());
        int space = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.isEmpty()) {
                space += perStack;
            } else if (ItemStack.isSameItemSameTags(s, proto)) {
                space += Math.max(0, s.getMaxStackSize() - s.getCount());
            }
            if (space >= totalCount) {
                return true;
            }
        }
        return false;
    }

    /** 卖出（把矿石类材料换成灵叶，直接进余额）。 */
    public static boolean sell(ServerPlayer player, ItemStack stack, int unitPrice) {
        if (stack.isEmpty() || unitPrice <= 0) {
            return false;
        }
        int qty = stack.getCount();
        int total = unitPrice * qty;
        stack.shrink(qty);
        syncInventory(player);
        giveLeaves(player, total);
        player.sendSystemMessage(Component.literal(
                "\u00a7a卖出 \u00a7f" + qty + " \u00a77个，得到 \u00a7e" + total
                        + " \u00a77灵叶\u00a78（余 " + leavesOf(player) + "）"));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.1F);
        return true;
    }

    // ------------------------------------------------------------------
    // 回收（把挖到的 / 掉的东西卖给坊市）
    // ------------------------------------------------------------------

    /**
     * 快捷栏占用的槽位数（0..8）。
     *
     * <p><b>保护规则（重要）：</b>{@link #sellAllSellable} 与
     * {@link #sellEntry} 都 <b>不动快捷栏（0..8）与护甲槽（36..39）</b>。
     * 理由是这两处放的是玩家"正在用"的东西——镐子、武器、法宝、身上穿的护甲。
     * 玩家点一下"卖出全部"就把手里的镐子卖了，是绝对不可接受的体验。</p>
     *
     * <p>想卖快捷栏里的东西，走"卖出手上物品"（{@link #sellHeld}），
     * 那是一个明确指向当前手持物的动作，不存在误伤。</p>
     */
    public static final int HOTBAR_SIZE = 9;

    /** 护甲槽在 {@code Inventory} 里的下标范围（含头）。 */
    public static final int ARMOR_SLOT_START = 36;

    /** 护甲槽在 {@code Inventory} 里的下标范围（含尾）。 */
    public static final int ARMOR_SLOT_END = 39;

    /** 某件物品能不能回收（只看表，不碰注册表）。 */
    public static boolean canSell(String itemId) {
        return SellPrices.accepts(itemId);
    }

    /** 某件物品的单件回收价；不收返回 0。 */
    public static int sellUnitPrice(String itemId) {
        return com.hongmeng.xiuxian.shop.SellPrices.unitPrice(itemId);
    }

    /**
     * 带**品质**的单件回收价。
     *
     * <p>卖出时用这个重载，品质越高回收价越高（神品 450%）。
     * 防套利不受影响：商店卖出的东西恒为凡品（100%），
     * 所以「买入→卖出」永远是亏的。</p>
     */
    public static int sellUnitPrice(String itemId, net.minecraft.world.item.ItemStack stack) {
        return com.hongmeng.xiuxian.shop.SellPrices.unitPrice(itemId,
                com.hongmeng.xiuxian.quality.ItemQuality.of(stack));
    }

    /** 这个槽位在"卖出全部"时是否受保护（快捷栏 / 护甲）。 */
    public static boolean sellsProtected(int slot) {
        return slot < HOTBAR_SIZE
                || (slot >= ARMOR_SLOT_START && slot <= ARMOR_SLOT_END);
    }

    /**
     * 物品的注册名（带命名空间），例：{@code hongmeng:lingye}。
     *
     * <p>和 {@code AlchemyFurnaceBlockEntity.itemId} 同一个写法：
     * 回收价表是纯数据的字符串表，所以每次比较都要现查一次注册名。</p>
     */
    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString();
    }

    /** 背包里"可回收且不在保护槽位"的某个物品总数（界面显示用）。 */
    public static int sellableCount(ServerPlayer player, String itemId) {
        if (player == null || !SellPrices.accepts(itemId)) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (sellsProtected(i)) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && itemId.equals(itemId(stack))) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 卖出<b>某一格整堆</b>（回收列表里点选某条 → 卖一条）。
     *
     * @return 实际卖出的件数（0 = 没卖成）
     */
    public static int sellEntry(ServerPlayer player, String itemId) {
        if (player == null) {
            return 0;
        }
        if (!SellPrices.accepts(itemId)) {
            player.sendSystemMessage(Component.literal("\u00a7c坊市不收这样东西。"));
            return 0;
        }
        // 从前往后找第一个"不在保护槽位里"的同类堆
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (sellsProtected(i)) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !itemId.equals(itemId(stack))) {
                continue;
            }
            return sellStack(player, stack, itemId);
        }
        // 只可能在快捷栏/护甲里 → 明确告诉玩家为什么没卖
        if (heldSlotWouldSell(player, itemId)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7e这样东西在快捷栏里。想卖它请用「卖出手上物品」，或先挪到背包。"));
        } else {
            player.sendSystemMessage(Component.literal(
                    "\u00a77背包里没有可回收的这样东西"
                            + "\u00a78（快捷栏与身上的装备不在回收范围内）"));
        }
        return 0;
    }

    /** 快捷栏里有没有这件东西（用来给出"为什么卖不掉"的准确提示）。 */
    private static boolean heldSlotWouldSell(ServerPlayer player, String itemId) {
        for (int i = 0; i < HOTBAR_SIZE && i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && itemId.equals(itemId(stack))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 卖出<b>手上拿着的那一堆</b>（唯一允许卖快捷栏的入口）。
     *
     * <p>这是一个明确指向当前手持物的动作：玩家自己拿着东西点"卖出手上物品"，
     * 是本人的选择，不算误伤——所以这一条<b>不</b>受
     * {@link #HOTBAR_SIZE 快捷栏保护}限制。</p>
     *
     * @return 卖出的件数
     */
    public static int sellHeld(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7c手上没有东西。"));
            return 0;
        }
        String id = itemId(stack);
        if (!SellPrices.accepts(id)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c坊市不收「" + stack.getHoverName().getString() + "」。"));
            player.sendSystemMessage(Component.literal(
                    "\u00a78收的是矿物、材料、灵植、丹药——回收页签里有完整价目表。"));
            return 0;
        }
        return sellStack(player, stack, id);
    }

    /** 真正结算一堆东西：扣物品、进余额、报数。 */
    private static int sellStack(ServerPlayer player, ItemStack stack, String itemId) {
        if (stack.isEmpty() || !SellPrices.accepts(itemId)) {
            return 0;
        }
        int qty = stack.getCount();
        // 带品质的单价：上品/仙品的回收价比凡品高（见 QualityTable.sellPrice）
        int total = sellTotal(itemId, stack, qty);
        String cn = stack.getHoverName().getString();
        stack.shrink(qty);
        syncInventory(player);
        giveLeaves(player, total);
        player.sendSystemMessage(Component.literal(
                "\u00a7a卖出 \u00a7f" + cn + " \u00a77×" + qty
                        + " \u00a77得到 \u00a7e" + total
                        + " \u00a77灵叶\u00a78（余 " + leavesOf(player) + "）"));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.1F);
        return qty;
    }

    // ------------------------------------------------------------------
    // 部分出售（"卖 N" / "保留 N"）
    //
    // 玩家原话："商店的东西也可以一键售卖，比如背包里有很多可以选择
    // 全部售卖或者是保留/售卖多少"。
    //
    // 三个入口的分工，**保护规则一条都不放宽**：
    //   全部   → sellEntry（整堆，本来就跳过快捷栏与护甲）
    //   卖 N   → sellEntryCount
    //   保留 N → sellEntryKeep（卖掉"除了这 N 个之外"的全部）
    // 想在快捷栏里卖东西仍然只有一条路：sellHeld（"卖出手上物品"）。
    // ------------------------------------------------------------------

    /**
     * 带品质的总价 = 品质单价 × 数量。
     *
     * <p>{@link SellPrices#total} 只有基础价（凡品价）。界面与结算都必须走这个，
     * 否则上品矿和凡品矿在界面上显示同一个价，结算时却多给钱——
     * 那种"显示与到账不一致"的 bug 玩家一定会报。</p>
     *
     * <p>品质倍率只对 {@code QualityTable} 里"能有品质"的物品生效
     * （{@link ItemQuality#of} 对表外物品返回凡品 = ×1），所以原版矿物
     * 不受影响。</p>
     */
    public static int sellTotal(String itemId, ItemStack stack, int count) {
        if (count <= 0) {
            return 0;
        }
        int unit = (stack == null || stack.isEmpty())
                ? SellPrices.unitPrice(itemId)
                : SellPrices.unitPrice(itemId, ItemQuality.of(stack));
        return Math.max(0, unit) * count;
    }

    /**
     * 背包里"这一种、不在保护槽位里"的总数。
     *
     * <p>与 {@link #sellableCount} 同一个口径，但不需要传玩家之外的上下文——
     * 界面用它显示"可卖 N"。</p>
     */
    public static int sellableCountOf(ServerPlayer player, String itemId) {
        return sellableCount(player, itemId);
    }

    /**
     * 背包里"第一种可卖的该物品"（用于取品质）。
     *
     * <p>同一件东西可能分好几堆、品质还不一样（凡品一炉、仙品一炉），
     * 这里取**第一堆**的品质当代表——和 {@link #sellEntry} 的结算顺序一致，
     * 所以界面显示的价与真正到账的价永远对得上。</p>
     *
     * @return 找不到返回 {@link ItemStack#EMPTY}
     */
    public static ItemStack firstSellableStack(ServerPlayer player, String itemId) {
        if (player == null || itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (sellsProtected(i)) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && itemId.equals(itemId(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 卖出某一种物品的<b>指定数量</b>（"卖 N"）。
     *
     * <p>数量会被夹到"背包里实际可卖的数量"：客户端填 999999 也只能卖到手的那点。
     * 空掉的位置顺手清成空栈。</p>
     *
     * @return 实际卖出的件数
     */
    public static int sellEntryCount(ServerPlayer player, String itemId, int requested) {
        if (player == null) {
            return 0;
        }
        if (requested <= 0) {
            player.sendSystemMessage(Component.literal("\u00a7c数量得大于 0。"));
            return 0;
        }
        if (!SellPrices.accepts(itemId)) {
            notSellableMessage(player, itemId);
            return 0;
        }
        int available = sellableCount(player, itemId);
        if (available <= 0) {
            notSellableMessage(player, itemId);
            return 0;
        }
        return sellSome(player, itemId, Math.min(requested, available));
    }

    /**
     * 卖出某一种物品，但<b>保留 N 个</b>（"保留 N"）。
     *
     * <p>语义：把"除了这 N 个之外"的全部卖掉。{@code keep == 0} 等价于全部卖掉。
     * 想留下的比背包里的还多 → 一件都不卖（这是玩家的本意，直接告诉他）。</p>
     *
     * @return 实际卖出的件数
     */
    public static int sellEntryKeep(ServerPlayer player, String itemId, int keep) {
        if (player == null) {
            return 0;
        }
        if (!SellPrices.accepts(itemId)) {
            notSellableMessage(player, itemId);
            return 0;
        }
        int available = sellableCount(player, itemId);
        if (available <= 0) {
            notSellableMessage(player, itemId);
            return 0;
        }
        int want = available - Math.max(0, keep);
        if (want <= 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77背包里只有 \u00a7f" + available + " \u00a77个，保留 \u00a7f"
                            + keep + " \u00a77个就没什么可卖了。"));
            return 0;
        }
        return sellSome(player, itemId, want);
    }

    /** "为什么卖不掉"的统一提示（快捷栏 / 收不收 / 根本没有 三种情况分开说）。 */
    private static void notSellableMessage(ServerPlayer player, String itemId) {
        if (!SellPrices.accepts(itemId)) {
            player.sendSystemMessage(Component.literal("\u00a7c坊市不收这样东西。"));
            return;
        }
        if (heldSlotWouldSell(player, itemId)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7e这样东西在快捷栏里。想卖它请用「卖出手上物品」，或先挪到背包。"));
        } else {
            player.sendSystemMessage(Component.literal(
                    "\u00a77背包里没有可回收的这样东西"
                            + "\u00a78（快捷栏与身上的装备不在回收范围内）"));
        }
    }

    /**
     * 从非保护槽位里按数量取货并结算。
     *
     * <p><b>保护规则</b>：{@link #sellsProtected} 一跳都不跳——
     * 快捷栏与护甲永远不参与"部分出售"，和"一键回收"完全一致。</p>
     *
     * @param want 想卖多少（调用方已经夹过，这里再夹一次兜底）
     * @return 实际卖出的件数
     */
    private static int sellSome(ServerPlayer player, String itemId, int want) {
        int remaining = want;
        int sold = 0;
        int total = 0;
        String cn = null;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            if (sellsProtected(i)) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !itemId.equals(itemId(stack))) {
                continue;
            }
            if (cn == null) {
                cn = stack.getHoverName().getString();
            }
            int take = Math.min(remaining, stack.getCount());
            // 品质按这一堆算：每卖一堆就按那一堆的品质结算
            total += sellTotal(itemId, stack, take);
            stack.shrink(take);
            remaining -= take;
            sold += take;
        }
        if (sold <= 0) {
            return 0;
        }
        syncInventory(player);
        giveLeaves(player, total);
        player.sendSystemMessage(Component.literal(
                "\u00a7a卖出 \u00a7f" + (cn == null ? itemId : cn) + " \u00a77×" + sold
                        + " \u00a77得到 \u00a7e" + total
                        + " \u00a77灵叶\u00a78（余 " + leavesOf(player) + "）"));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.1F);
        return sold;
    }

    /**
     * <b>卖出全部可回收物</b>（背包里除了快捷栏与护甲之外的一切）。
     *
     * <p>一次扫完背包、一次结算、一条消息——不要把 30 条"卖出 X"刷屏。
     * 空掉的位置顺手清成空栈，免得留下 count=0 的幽灵堆。</p>
     *
     * @return 卖出的总件数
     */
    public static int sellAllSellable(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        int kinds = 0;
        int items = 0;
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (sellsProtected(i)) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String id = itemId(stack);
            int unit = SellPrices.unitPrice(id, ItemQuality.of(stack));
            if (unit <= 0) {
                continue;
            }
            int qty = stack.getCount();
            kinds++;
            items += qty;
            total += unit * qty;
            stack.setCount(0);
        }
        if (items <= 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77背包里没有可回收的东西\u00a78（快捷栏与身上的装备不在回收范围内）"));
            player.sendSystemMessage(Component.literal(
                    "\u00a78矿物、材料、灵植、丹药都能卖——打开回收页签看价目表。"));
            return 0;
        }
        syncInventory(player);
        giveLeaves(player, total);
        player.sendSystemMessage(Component.literal(
                "\u00a7a一键回收：\u00a7f" + kinds + " \u00a77种共 \u00a7f" + items
                        + " \u00a77件，得到 \u00a7e" + total
                        + " \u00a77灵叶\u00a78（余 " + leavesOf(player) + "）"));
        player.sendSystemMessage(Component.literal(
                "\u00a78快捷栏与身上的装备已跳过，不会被卖掉。"));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.VILLAGER_TRADE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        return items;
    }
}

