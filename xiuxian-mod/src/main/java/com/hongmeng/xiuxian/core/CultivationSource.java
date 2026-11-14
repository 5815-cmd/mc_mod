package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.network.ModNetworkBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * 修为的来源。原文里嬴怀虚"不爱打坐"，修为主要靠杀怪、任务、奇遇、丹药堆上来。
 * 所以这里每一条途径都是正经来源，打坐只是最稳的那条。
 */
public enum CultivationSource {

    MEDITATION("打坐吐纳", ChatFormatting.AQUA),
    KILL("斩敌", ChatFormatting.RED),
    BOSS("诛杀大敌", ChatFormatting.DARK_RED),
    TASK("任务", ChatFormatting.GOLD),
    ADVENTURE("奇遇", ChatFormatting.LIGHT_PURPLE),
    PILL("丹药", ChatFormatting.GREEN),
    /** 直接服用灵石（挖灵石矿脉所得）；与丹药分开标，玩家一眼知道修为从哪来。 */
    SPIRIT_STONE("灵石", ChatFormatting.AQUA),
    BREAKTHROUGH("破境", ChatFormatting.YELLOW),
    DUNGEON("纪元任务", ChatFormatting.BLUE),
    SPECIAL("特殊", ChatFormatting.WHITE);

    private final String cn;
    private final ChatFormatting color;

    CultivationSource(String cn, ChatFormatting color) {
        this.cn = cn;
        this.color = color;
    }

    public String cn() {
        return cn;
    }

    public ChatFormatting color() {
        return color;
    }

    public Component label() {
        return Component.literal("[" + cn + "]").withStyle(color);
    }

    /**
     * 给玩家加修为，并把来源反馈到动作栏。
     *
     * <p><b>修为在当前小境界的上限处封顶</b>——满了就不再涨，
     * 必须玩家主动催动突破（按 B 或点面板按钮）才能进入下一层。
     * 这样"攒满 → 破境"才有仪式感，也不会让玩家一路无声无息滑到高境界。</p>
     *
     * @return 实际加上的修为（顶到上限时可能少于请求值）
     */
    public long grant(ServerPlayer player, double amount, boolean silent) {
        if (player == null || amount <= 0.0D) {
            return 0L;
        }
        CultivationData data = CultivationCapability.get(player);
        if (!data.isAwakened()) {
            return 0L;
        }

        // 已满则不再增长，只提示去破境
        long cap = data.cultivationCap();
        long room = cap - data.getCultivation();
        if (room <= 0L) {
            if (!silent) {
                player.displayClientMessage(
                        Component.literal("修为已满 · 按 B 破境").withStyle(ChatFormatting.YELLOW),
                        true);
            }
            return 0L;
        }
        if (data.isMaxRealm()) {
            return 0L;
        }

        // ---- 灵根倍率：**所有修为来源的统一入口** ----
        //
        // 玩家设计意图：「灵根强弱之别主要体现在修炼速度」。
        // 之前只有打坐乘了这个倍率，杀怪/采药/任务全都没乘 ——
        // 所以灵根在玩家体感里"没有用处"。放到这里就全部覆盖了。
        double rooted = amount;
        try {
            var root = data.getRoot();
            if (root != null) {
                rooted *= root.multiplier();
            }
        } catch (Throwable ignored) {
            // 取不到灵根就按原值，绝不影响修为结算
        }
        long gain = Math.min(Math.max(1L, Math.round(rooted)), room);
        boolean wasMax = data.isMaxRealm();
        Realm before = data.realm();
        data.addCultivation(gain);
        Realm after = data.realm();

        boolean reachedCap = data.getCultivation() >= cap;
        if (!silent) {
            // 玩家反馈「提升的显示字体为黄色，在血条上方白色看不清」：
            // 这两条都渲染在**血条正上方**的动作栏里，原来的绿字 / 金字在亮背景上不够醒目。
            // 统一成黄色（来源标签 [斩敌]、[打坐] 的颜色保留，那是信息不是强调）。
            player.displayClientMessage(reachedCap
                            ? Component.literal("修为已满 · 按 B 破境")
                            .withStyle(ChatFormatting.YELLOW)
                            : Component.literal("修为 +" + gain + " ")
                            .withStyle(ChatFormatting.YELLOW).append(label()),
                    true);
        }

        // **这里绝不能发「破境」公告。**
        //
        // 玩家反馈「杀一次怪就显示一次突破」就是这个：
        // `grant()` 是**每次获得修为**都会走的路径（击杀、任务、副本），
        // 而破境公告的职责属于 Breakthrough —— 它在三个成功路径里
        // （小境界成功 / 无雷劫大境界 / 渡劫大境界）已经各发过一次了。
        //
        // 这里的判断还依赖 Realm 对象比较（after != before），在值相同的情况下
        // 也会误判为"变了"，于是每杀一只怪就播一次突破消息。
        // 直接删掉：升境的公告由 Breakthrough 独家负责，这里只负责加修为。
        java.util.Objects.requireNonNull(after);
        java.util.Objects.requireNonNull(before);
        ModNetworkBridge.sync(player);
        return gain;
    }

    /** 无玩家对象的场景（比如定时器）也能发声。 */
    public void grant(ServerPlayer player, double amount, Consumer<ServerPlayer> extra) {
        grant(player, amount, false);
        if (extra != null) {
            extra.accept(player);
        }
    }
}
