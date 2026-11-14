package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.network.BreakthroughFxPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 破境播报：修为攒够、自动跨层时喊一嗓子。
 * 原文里每次境界变化都是大事件，所以这里给足排面。
 */
public final class BreakthroughAnnouncer {

    private BreakthroughAnnouncer() {
    }

    public static void announce(ServerPlayer player, Realm realm) {
        if (player == null || realm == null) {
            return;
        }
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a7e\u00a7l═══ 破 境 ═══").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal(
                "\u00a7f你的修为突破了——现在是 \u00a7e\u00a7l" + realm.cn()));
        player.sendSystemMessage(Component.literal(
                "\u00a77下一境：\u00a7f" + (realm.isMaxRealm() ? "已至绝巅" : realm.nextCn())));
        player.sendSystemMessage(Component.literal(""));

        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        ModNetwork.sendToPlayer(player, new BreakthroughFxPacket(
                BreakthroughFxPacket.Kind.SUCCESS, realm.cn()));
        ModNetworkBridge.sync(player);
    }

    /** 突破失败。 */
    public static void fail(ServerPlayer player, CultivationData data, long lost) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a7c\u00a7l═══ 突破失败 ═══"));
        player.sendSystemMessage(Component.literal(
                "\u00a77灵气溃散，你稳不住那道关口。"));
        player.sendSystemMessage(Component.literal(
                "\u00a77修为 \u00a7c-" + lost + " \u00a78（损失本层三分之一）"));
        player.sendSystemMessage(Component.literal(
                "\u00a77当前业力：\u00a7c" + data.getKarma() + " \u00a77层，下次失败率：\u00a7c"
                        + Karma.failurePercentText(data)));
        player.sendSystemMessage(Component.literal(
                "\u00a78业力可以靠附魔金苹果、劫掠胜利来削。"));
        player.sendSystemMessage(Component.literal(""));

        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
        ModNetwork.sendToPlayer(player, new BreakthroughFxPacket(
                BreakthroughFxPacket.Kind.FAIL, ""));
        ModNetworkBridge.sync(player);
    }

    /** 业力满时连尝试都不敢尝试。 */
    public static void karmaSealed(ServerPlayer player, CultivationData data) {
        player.sendSystemMessage(Component.literal(
                "\u00a74业力满溢（" + data.getKarma() + " 层），道基蒙尘，此境已不可破。"));
        player.sendSystemMessage(Component.literal(
                "\u00a78先去把业力清了——附魔金苹果，或者替村庄解决一次劫掠。"));
    }
}
