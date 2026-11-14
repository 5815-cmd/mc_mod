package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 祝福与奇遇：探索世界时的随机好事。
 * 原文里主角靠"奇遇任务"一波肥，这里把那种手感做成可触发事件。
 */
public final class BlessingTracker {

    /** 附魔金苹果：一次清空全部业力。 */
    public static void onEnchantedApple(ServerPlayer player, ItemStack stack) {
        if (!HongmengConfig.SERVER.karmaEnabled.get()
                || !HongmengConfig.SERVER.enchantedAppleClearsAll.get()) {
            return;
        }
        if (!stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        int cleared = Karma.clear(data);
        if (cleared <= 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77附魔金苹果下肚，你身清气和——本来也没什么业力。"));
            return;
        }
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a76\u00a7l业力尽消"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f附魔金苹果的金光洗过你的道基，业力 \u00a7c-" + cleared
                        + " \u00a7f层，如今归零。"));
        player.sendSystemMessage(Component.literal(
                "\u00a77当前突破失败率：\u00a7a" + Karma.failurePercentText(data)));
        player.sendSystemMessage(Component.literal(""));
        com.hongmeng.xiuxian.network.ModNetworkBridge.sync(player);
    }

    /** 解决一次劫掠袭击：-1 层业力（可配置）。 */
    public static void onRaidCleared(ServerPlayer player) {
        if (!HongmengConfig.SERVER.karmaEnabled.get()) {
            return;
        }
        int reduce = HongmengConfig.SERVER.raidKarmaReduction.get();
        if (reduce <= 0) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        int actually = Karma.reduce(data, reduce);
        if (actually <= 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77劫掠者退了。你身上干干净净，没什么可消的业力。"));
            return;
        }
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a7a村庄的劫掠者被击退了。"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f村民们围上来道谢。你身上那点业力随之淡了一分：\u00a7a-" + actually
                        + " \u00a77（剩余 " + data.getKarma() + " 层）"));
        player.sendSystemMessage(Component.literal(
                "\u00a77当前突破失败率：\u00a7e" + Karma.failurePercentText(data)));
        player.sendSystemMessage(Component.literal(""));
        CultivationSource.TASK.grant(player, 60.0D, true);
        com.hongmeng.xiuxian.network.ModNetworkBridge.sync(player);
    }

    /** 奇遇：探索时随机触发，给修为 + 小礼物。 */
    public static void adventure(ServerPlayer player, String title, String text, double exp) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a7d\u00a7l✦ 奇 遇 ✦ \u00a7f" + title).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("\u00a77" + text));
        CultivationSource.ADVENTURE.grant(player, exp, true);
        player.sendSystemMessage(Component.literal(""));
    }
}
