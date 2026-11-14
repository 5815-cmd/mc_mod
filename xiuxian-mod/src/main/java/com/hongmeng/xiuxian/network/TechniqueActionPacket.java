package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.technique.SpellSystem;
import com.hongmeng.xiuxian.technique.Technique;
import com.hongmeng.xiuxian.technique.TechniqueCaster;
import com.hongmeng.xiuxian.technique.TechniqueData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：功法操作。
 *
 * <p>原本三种动作共用一个包（施展、切换、直接选中）；
 * 法术栏扩充时**追加**了两种（{@link Action#CAST_SLOT} 按槽位施展、
 * {@link Action#EQUIP} 装备/卸下），既有动作的含义一个字都没改。</p>
 *
 * <h3>槽位编码</h3>
 * <pre>
 *   index 0..4   被动槽 1~5     （仅 EQUIP 用）
 *   index 10..13 主动槽 1~4     （EQUIP 与 CAST_SLOT 都用）
 * </pre>
 */
public class TechniqueActionPacket {

    /** 主动槽在 index 里的偏移：10 号起是主动槽 1~4。 */
    public static final int ACTIVE_SLOT_BASE = 10;

    public enum Action {
        /** 施展当前选中的功法。 */
        CAST,
        /** 切换到下一门。 */
        NEXT,
        /** 切换到上一门。 */
        PREV,
        /** 直接选中（携带序号）。 */
        SELECT,
        /** 施展第 N 号主动槽（index = 10..13）。 */
        CAST_SLOT,
        /** 装备/卸下第 N 号槽（index 见类注释；再次装备同一门 = 卸下）。 */
        EQUIP
    }

    private final Action action;
    private final int index;

    public TechniqueActionPacket(Action action, int index) {
        this.action = action;
        this.index = index;
    }

    public static void encode(TechniqueActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeVarInt(packet.index);
    }

    public static TechniqueActionPacket decode(FriendlyByteBuf buf) {
        return new TechniqueActionPacket(buf.readEnum(Action.class), buf.readVarInt());
    }

    public static void handle(TechniqueActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            // 法术系统的事件挂载点：保证蓄力推进一定在跑（幂等）
            SpellSystem.register();
            CultivationData data = CultivationCapability.get(player);
            if (data == null || !data.isAwakened()) {
                return;
            }
            var tech = data.getTechniques();

            switch (packet.action) {
                case CAST -> {
                    Technique t = tech.selectedTechnique();
                    if (t == null) {
                        player.displayClientMessage(Component.literal(
                                "\u00a77你还没修习任何功法。\u00a78（找功法秘籍右键修习）"), true);
                        return;
                    }
                    TechniqueCaster.cast(player, t);
                }
                case NEXT -> {
                    Technique t = tech.next();
                    announce(player, t);
                }
                case PREV -> {
                    Technique t = tech.previous();
                    announce(player, t);
                }
                case SELECT -> {
                    var list = tech.knownList();
                    if (packet.index >= 0 && packet.index < list.size()) {
                        tech.select(list.get(packet.index));
                        announce(player, list.get(packet.index));
                    }
                }
                case CAST_SLOT -> castSlot(player, tech, packet.index);
                case EQUIP -> equip(player, data, tech, packet.index);
            }
            ModNetworkBridge.sync(player);
        });
        context.setPacketHandled(true);
    }

    // ------------------------------------------------------------------
    // 法术栏
    // ------------------------------------------------------------------

    /** 按主动槽施展（Z X C V）。 */
    private static void castSlot(ServerPlayer player, TechniqueData tech, int index) {
        int slot = index - ACTIVE_SLOT_BASE;
        if (slot < 0 || slot >= TechniqueData.ACTIVE_SLOTS) {
            return;
        }
        Technique t = tech.activeAt(slot);
        if (t == null) {
            player.displayClientMessage(Component.literal(
                    "\u00a78第 " + (slot + 1) + " 号法术槽是空的。\u00a77按 O 打开法术栏装备。"), true);
            return;
        }
        TechniqueCaster.cast(player, t);
    }

    /**
     * 装备 / 卸下。
     *
     * <p>规则（服务端权威，客户端界面只是把它画出来）：</p>
     * <ul>
     *   <li>只有**已修习**的功法能装备；</li>
     *   <li>被动只能进被动槽，主动只能进主动槽；</li>
     *   <li>槽上已经是同一门功法 → 再点一次 = 卸下；</li>
     *   <li>装备成功会立刻重算被动（属性生效 / 失效）。</li>
     * </ul>
     */
    private static void equip(ServerPlayer player, CultivationData data,
                              TechniqueData tech, int index) {
        boolean passive;
        int slot;
        if (index >= ACTIVE_SLOT_BASE) {
            passive = false;
            slot = index - ACTIVE_SLOT_BASE;
        } else {
            passive = true;
            slot = index;
        }
        Technique current = passive ? tech.passiveAt(slot) : tech.activeAt(slot);

        // 槽上已经有东西 → 点击 = 卸下（规则简单，不会"点一下就换掉"误操作）
        if (current != null) {
            unequip(player, tech, passive, slot);
            return;
        }

        Technique target = tech.selectedTechnique();
        if (target == null) {
            player.displayClientMessage(Component.literal(
                    "\u00a77先在下面的列表里点一门功法，再点槽位。"), true);
            return;
        }
        if (!tech.has(target)) {
            player.displayClientMessage(Component.literal(
                    "\u00a77尚未修习" + target.colored() + "\u00a77，装备不了。"), true);
            return;
        }
        if (target.isPassive() != passive) {
            warnSlotMismatch(player, target);
            return;
        }
        boolean ok = passive ? tech.equipPassive(target, slot) : tech.equipActive(target, slot);
        if (!ok) {
            player.displayClientMessage(Component.literal("\u00a7c装备失败。"), true);
            return;
        }
        // 被动装上/卸下：立刻重算属性
        com.hongmeng.xiuxian.technique.TechniquePassive.apply(player, tech);
        com.hongmeng.xiuxian.technique.ManaBridge.clampToCap(player, data);
        player.displayClientMessage(Component.literal(
                "\u00a7a已装备 " + target.colored() + " \u00a78→ "
                        + (passive ? "被动槽 " : "主动槽 ") + (slot + 1)), true);
    }

    private static void unequip(ServerPlayer player, TechniqueData tech,
                               boolean passive, int slot) {
        Technique removed = passive ? tech.unequipPassive(slot) : tech.unequipActive(slot);
        if (removed == null) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        if (data != null) {
            com.hongmeng.xiuxian.technique.TechniquePassive.apply(player, tech);
            com.hongmeng.xiuxian.technique.ManaBridge.clampToCap(player, data);
        }
        player.displayClientMessage(Component.literal(
                "\u00a77已卸下 " + removed.colored()), true);
    }

    /**
     * 「槽位不符」的统一判词：服务端装备裁决与客户端拖拽落点共用这一份。
     *
     * <p>两边各写一遍迟早走样，玩家看到的就是两套说法，所以文案只留这里一处。</p>
     *
     * <p><b>为什么判据是功法自己的种类：</b>这句话的主语是"这门功法"，
     * 原来却跟着**槽位**的种类挑句子（{@code passive ? 被动 : 主动}），
     * 于是被动功法拖进主动槽会说成"是被动功法，只能进上面的被动槽"——正好说反。
     * 主语是功法，判据就该用 {@link Technique#isPassive()}。</p>
     *
     * @param player 收提示的玩家（服务端是 ServerPlayer，客户端是 LocalPlayer）
     * @param target 被放错槽的那门功法
     */
    public static void warnSlotMismatch(Player player, Technique target) {
        if (player == null || target == null) {
            return;
        }
        player.displayClientMessage(Component.literal(
                "\u00a7c槽位不符\u00a78：" + target.colored()
                        + (target.isPassive()
                        ? " \u00a78是被动功法，只能进上面的被动槽。"
                        : " \u00a78是主动功法，只能进下面的主动槽。")), true);
    }

    private static void announce(ServerPlayer player, Technique t) {
        if (t == null) {
            player.displayClientMessage(Component.literal("\u00a77没有可用的功法。"), true);
            return;
        }
        player.displayClientMessage(Component.literal(
                "\u00a77当前功法：" + t.colored()
                        + " \u00a78[" + t.element().cn() + "/" + t.kind().glyph() + "]"), true);
    }

    // ------------------------------------------------------------------
    // 客户端发送
    // ------------------------------------------------------------------

    public static void cast() {
        send(new TechniqueActionPacket(Action.CAST, 0));
    }

    public static void next() {
        send(new TechniqueActionPacket(Action.NEXT, 0));
    }

    public static void prev() {
        send(new TechniqueActionPacket(Action.PREV, 0));
    }

    public static void select(int index) {
        send(new TechniqueActionPacket(Action.SELECT, index));
    }

    /** 按主动槽施展（slot 从 0 开始）。 */
    public static void castActiveSlot(int slot) {
        send(new TechniqueActionPacket(Action.CAST_SLOT, ACTIVE_SLOT_BASE + slot));
    }

    /** 装备到被动槽（slot 从 0 开始）。 */
    public static void equipPassiveSlot(int slot) {
        send(new TechniqueActionPacket(Action.EQUIP, slot));
    }

    /** 装备到主动槽（slot 从 0 开始）。 */
    public static void equipActiveSlot(int slot) {
        send(new TechniqueActionPacket(Action.EQUIP, ACTIVE_SLOT_BASE + slot));
    }

    private static void send(TechniqueActionPacket packet) {
        if (ModNetwork.CHANNEL != null) {
            ModNetwork.CHANNEL.sendToServer(packet);
        }
    }
}
