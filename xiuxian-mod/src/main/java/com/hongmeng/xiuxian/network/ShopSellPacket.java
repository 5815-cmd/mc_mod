package com.hongmeng.xiuxian.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：<b>坊市回收</b>（把东西卖给坊市换灵叶）。
 *
 * <p>五种卖法用一个包解决，避免为了几个按钮注册几个包：</p>
 *
 * <ul>
 *   <li>{@link #MODE_ENTRY}：卖掉背包里某一 <b>种</b>物品的<b>一整堆</b>。
 *       客户端只传<b>物品注册名</b>，价格由服务端查 {@code SellPrices} 表——
 *       客户端改包也改不出价格。</li>
 *   <li>{@link #MODE_ALL}：一键回收（背包里除快捷栏与护甲之外的一切可回收物）。</li>
 *   <li>{@link #MODE_HELD}：卖出手上拿着的那一堆（唯一允许卖快捷栏的入口）。</li>
 *   <li>{@link #MODE_COUNT}：卖掉某一种物品的<b>指定数量</b>（"卖 N"）。
 *       数量由服务端夹到"背包里实际可卖的数量"，客户端填天文数字没用。</li>
 *   <li>{@link #MODE_KEEP}：卖掉某一种物品，<b>但保留 N 个</b>（"保留 N"）。
 *       语义上是"全部卖掉，除了留下这 N 个"——玩家原话要的正是这个。</li>
 * </ul>
 *
 * <p>包里<b>不传价格</b>，只传模式 + 物品 id +（可选）数量——和
 * {@link ShopBuyPacket} 同一个安全口径。</p>
 *
 * <h2>协议兼容</h2>
 *
 * <p>新增的 {@code amount} 字段<b>追加在末尾</b>，而且 {@code decode} 用
 * {@code buf.isReadable()} 兜住老格式（只有 mode + itemId 两个字段的包）。
 * 所以 {@code ModNetwork} 里的注册 id 一个都不用动、也不会挪位置——
 * 加模式比加包安全。</p>
 */
public class ShopSellPacket {

    /** 卖背包里某一整堆（需要 {@link #itemId()}）。 */
    public static final int MODE_ENTRY = 0;

    /** 一键回收背包里全部可回收物。 */
    public static final int MODE_ALL = 1;

    /** 卖出手上拿着的那一堆。 */
    public static final int MODE_HELD = 2;

    /** 卖掉某一种物品的指定数量（"卖 N"）。 */
    public static final int MODE_COUNT = 3;

    /** 卖掉某一种物品但保留 N 个（"保留 N"）。 */
    public static final int MODE_KEEP = 4;

    /** 数量字段的合法上限（防手滑 / 防改包里的天文数字）。 */
    public static final int MAX_AMOUNT = 1_000_000;

    private final int mode;
    private final String itemId;
    private final int amount;

    public ShopSellPacket(int mode, String itemId) {
        this(mode, itemId, 0);
    }

    public ShopSellPacket(int mode, String itemId, int amount) {
        this.mode = mode;
        this.itemId = itemId == null ? "" : itemId;
        this.amount = Math.max(0, Math.min(MAX_AMOUNT, amount));
    }

    public int mode() {
        return mode;
    }

    public String itemId() {
        return itemId;
    }

    /** 数量（只有 {@link #MODE_COUNT} / {@link #MODE_KEEP} 用得上）。 */
    public int amount() {
        return amount;
    }

    public static void encode(ShopSellPacket packet, FriendlyByteBuf buf) {
        // 顺序：mode → itemId → amount。前两个是老字段，amount 追加在末尾。
        buf.writeVarInt(packet.mode);
        buf.writeUtf(packet.itemId, 128);
        if (packet.mode == MODE_COUNT || packet.mode == MODE_KEEP) {
            buf.writeVarInt(packet.amount);
        }
    }

    public static ShopSellPacket decode(FriendlyByteBuf buf) {
        // 顺序与 encode 严格一致：先 mode 后 itemId
        int mode = buf.readVarInt();
        String id = buf.readUtf(128);
        // 只有这两种模式带 amount；同时容忍没有 amount 的老格式
        if ((mode == MODE_COUNT || mode == MODE_KEEP) && buf.isReadable()) {
            return new ShopSellPacket(mode, id, buf.readVarInt());
        }
        return new ShopSellPacket(mode, id);
    }

    public static void handle(ShopSellPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            switch (packet.mode) {
                case MODE_ALL -> com.hongmeng.xiuxian.shop.ShopManager.sellAllSellable(player);
                case MODE_HELD -> com.hongmeng.xiuxian.shop.ShopManager.sellHeld(player);
                case MODE_COUNT -> com.hongmeng.xiuxian.shop.ShopManager
                        .sellEntryCount(player, packet.itemId, packet.amount);
                case MODE_KEEP -> com.hongmeng.xiuxian.shop.ShopManager
                        .sellEntryKeep(player, packet.itemId, packet.amount);
                default -> com.hongmeng.xiuxian.shop.ShopManager.sellEntry(player, packet.itemId);
            }
            // 卖完立刻把新余额推给客户端，界面上的"灵叶余额"要当场变
            ModNetworkBridge.sync(player);
        });
        context.setPacketHandled(true);
    }

    /** 卖背包里某一种物品（整堆）。 */
    public static void sendEntry(String itemId) {
        send(new ShopSellPacket(MODE_ENTRY, itemId));
    }

    /** 一键回收。 */
    public static void sendAll() {
        send(new ShopSellPacket(MODE_ALL, ""));
    }

    /** 卖出手上物品。 */
    public static void sendHeld() {
        send(new ShopSellPacket(MODE_HELD, ""));
    }

    /** 卖某一种物品的指定数量（"卖 N"）。 */
    public static void sendCount(String itemId, int count) {
        send(new ShopSellPacket(MODE_COUNT, itemId, count));
    }

    /** 卖某一种物品但保留 N 个（"保留 N"）。 */
    public static void sendKeep(String itemId, int keep) {
        send(new ShopSellPacket(MODE_KEEP, itemId, keep));
    }

    private static void send(ShopSellPacket packet) {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(packet);
        }
    }
}
