package com.hongmeng.xiuxian.command;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.sect.Sect;
import com.hongmeng.xiuxian.sect.SectManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /sect —— 门派玩法入口。
 *
 * <p>用命令而不是 GUI：一是省一套界面，二是原文里"拜入山门"本来就是
 * 跑到地方、报上名号的事，用文字更有味道。</p>
 */
public final class SectCommand {

    private SectCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sect")
                .then(Commands.literal("list").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a76\u00a7l═══ 可 拜 入 的 门 派 ═══"), false);
                    for (Sect s : Sect.joinable()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "  " + s.colored() + "\u00a77 — " + s.desc()), false);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "    \u00a7b特性：" + s.perk()), false);
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a78用法：/sect join <qianji|shenxiao|moyuan>"), false);
                    return 1;
                }))

                .then(Commands.literal("join")
                        .then(Commands.argument("sect", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "sect");
                                    Sect sect = Sect.byId(id);
                                    if (sect.isNone()) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "没有这个门派。可选：qianji / shenxiao / moyuan"));
                                        return 0;
                                    }
                                    CultivationData data = CultivationCapability.get(player);
                                    SectManager.join(player, data, sect, "拜帖");
                                    return 1;
                                })))

                .then(Commands.literal("leave").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    SectManager.leave(player, CultivationCapability.get(player), false);
                    return 1;
                }))

                .then(Commands.literal("betray").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    CultivationData data = CultivationCapability.get(player);
                    if (data.sect().isNone()) {
                        ctx.getSource().sendFailure(Component.literal("你还没入门派。"));
                        return 0;
                    }
                    SectManager.leave(player, data, true);
                    return 1;
                }))

                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    CultivationData data = CultivationCapability.get(player);
                    Sect sect = data.sect();
                    if (sect.isNone()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a77散修一个。用 /sect list 看看有哪些门派。"), false);
                        return 1;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a76\u00a7l═══ 门 派 状 态 ═══"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f门派：" + sect.colored()), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f身份：\u00a7e" + sect.rankName(data.getContribution())
                                    + "  \u00a77(贡献 " + data.getContribution() + ")"), false);
                    int next = sect.nextRankThreshold(data.getContribution());
                    if (next > 0) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a77下一身份还需：\u00a7f" + (next - data.getContribution())), false);
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f修为倍率：\u00a7a×"
                                    + String.format("%.2f", SectManager.cultivationMultiplier(data))
                                    + "\u00a77（门派 "
                                    + String.format("%.2f", sect.cultivationMultiplier())
                                    + " × 身份 "
                                    + String.format("%.2f", SectManager.rankCultivationBonus(data)) + "）"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7f攻击加成：\u00a7c+" + (int) (sect.attackBonus() * 100) + "%"
                                    + "   \u00a7f突破加成：\u00a7d+" + (int) (sect.breakthroughBonus() * 100) + "%"), false);
                    if (data.isBetrayed()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a74\u00a7l叛徒：\u00a7c旧主正在派人清理门户。"), false);
                    }
                    return 1;
                })));
    }
}
