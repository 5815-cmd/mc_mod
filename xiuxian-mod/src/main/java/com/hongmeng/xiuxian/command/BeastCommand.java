package com.hongmeng.xiuxian.command;

import com.hongmeng.xiuxian.entity.beast.BeastManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 灵兽的"输入回传"命令。
 *
 * <h3>为什么是命令，而不是一个新的网络包</h3>
 *
 * <p>骑乘时的"跳跃 / 上升"必须由客户端主动上报：玩家是<b>乘客</b>，
 * 他自己跳不起来，服务端因此读不到任何跳跃信号
 * （{@code ServerboundPlayerInputPacket} 只同步 Shift 那一项）。
 * 猪、炽足兽那类坐骑靠 {@code ItemSteerable} 的 {@code onPlayerJump} 解决，
 * 本模组的灵兽走的是 {@code getControllingPassenger} + 自写行进那条路
 * （见 {@code SpiritBeastEntity} 的选型说明），拿不到那个回调。</p>
 *
 * <p>上报可以走"新包"或"既有命令通道"。规格明确要求
 * <b>不得改动 {@code ModNetwork} 的协议 id 顺序</b>，而突破键早就在用
 * "客户端发命令"这条路（{@code ClientSetup} 里 BREAKTHROUGH_KEY 直接
 * {@code sendCommand("hongmeng breakthrough")}）。这里沿用同一条既有通道。</p>
 *
 * <p>命令根名刻意<b>不是</b> {@code hongmeng}：那个根命令已经由
 * {@code HongmengCommand} 注册（且 {@code requires(hasPermission(2))}），
 * 重复注册同一个字面量会让 Brigadier 抛异常。用一个独立根名
 * 既避开冲突，也不用去动那个文件。</p>
 */
public final class BeastCommand {

    private BeastCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 骑乘系统 2026-11-07 起关闭：/hmbeast 整个命令根在游戏内隐藏。
        if (!com.hongmeng.xiuxian.entity.beast.BeastRiding.ENABLED) {
            return;
        }
        dispatcher.register(Commands.literal("hmbeast")
                // 权限 0：普通玩家骑上灵兽就能用
                .requires(src -> src.hasPermission(0))
                .then(Commands.literal("jump").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!BeastManager.ridingFlyingBeast(player)
                            && !(player.getVehicle()
                            instanceof com.hongmeng.xiuxian.entity.beast.SpiritBeastEntity)) {
                        return 0;
                    }
                    BeastManager.requestJump(player);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!(player.getVehicle()
                            instanceof com.hongmeng.xiuxian.entity.beast.SpiritBeastEntity beast)) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00a77你现在没有骑着灵兽。"), false);
                        return 0;
                    }
                    String line = "\u00a7b" + beast.growth().colored() + " \u00a7f"
                            + beast.kind().cn()
                            + " \u00a77指令=" + beast.command().colored()
                            + " \u00a77生命=" + (int) beast.getHealth() + "/"
                            + (int) beast.getMaxHealth()
                            + " \u00a77护甲=" + (beast.hasBeastArmor() ? "有" : "无")
                            + (beast.kind().flying()
                            ? " \u00a7d灵力=" + beast.kind().manaDrain() + "/tick" : "");
                    ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    return 1;
                })));
    }
}
