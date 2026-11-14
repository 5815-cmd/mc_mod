package com.hongmeng.xiuxian.quest;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 任务服务端逻辑：进度评估、领奖、播报。
 */
public final class QuestManager {

    private QuestManager() {
    }

    /** 每秒调一次，评估所有任务是否达成。 */
    public static void tick(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        QuestProgress progress = data.getQuestProgress();
        progress.refresh(data);

        boolean changed = false;
        for (Quest quest : Quests.ALL) {
            int before = progress.stateOf(quest.id());
            boolean met = progress.evaluate(quest);
            if (met && before == 0) {
                // 刚刚达成
                announceReady(player, quest);
                changed = true;
            }
        }
        if (changed) {
            ModNetworkBridge.sync(player);
        }
    }

    private static void announceReady(ServerPlayer player, Quest quest) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a7e\u00a7l【任务完成】\u00a7f" + quest.title()));
        player.sendSystemMessage(Component.literal(
                "\u00a77" + quest.requirement().describe(
                        CultivationCapability.get(player).getQuestProgress()).getString()));
        player.sendSystemMessage(Component.literal(
                "\u00a7a按 \u00a7fJ \u00a7a打开任务面板领取奖励。"));
        player.sendSystemMessage(Component.literal(""));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    /** 玩家在任务面板点了"领取奖励"。 */
    public static boolean claim(ServerPlayer player, String questId) {
        Quest quest = Quests.byId(questId);
        if (quest == null) {
            return false;
        }
        CultivationData data = CultivationCapability.get(player);
        QuestProgress progress = data.getQuestProgress();
        progress.refresh(data);

        if (progress.isClaimed(questId)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77这个任务的奖励已经领过了。"));
            return false;
        }
        if (!progress.evaluate(quest)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c任务条件还没达成：\u00a77"
                            + quest.requirement().describe(progress).getString()));
            return false;
        }

        progress.setState(questId, 2);

        // 修为奖励
        if (quest.rewardCultivation() > 0) {
            CultivationSource.TASK.grant(player, quest.rewardCultivation(), true);
        }
        // 物品奖励
        if (!quest.rewardItemId().isEmpty() && quest.rewardItemCount() > 0) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(quest.rewardItemId()));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack stack = new ItemStack(item, quest.rewardItemCount());
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        // 领取后刷新一次实时值，方便后续任务立刻判定
        progress.refresh(data);

        // 灵叶奖励（坊市货币）：直接进坊市余额，不占背包格子
        if (quest.rewardLeaves() > 0) {
            com.hongmeng.xiuxian.shop.ShopManager.giveLeaves(player, quest.rewardLeaves());
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                        "\u00a76\u00a7l【任务奖励已领取】\u00a7f" + quest.title())
                .withStyle(ChatFormatting.GOLD));
        if (quest.rewardCultivation() > 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7a修为 \u00a7e+" + quest.rewardCultivation()));
        }
        if (!quest.rewardItemId().isEmpty() && quest.rewardItemCount() > 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7a物品 \u00a7e" + quest.rewardItemName()));
        }
        if (quest.rewardLeaves() > 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7a灵叶 \u00a7e+" + quest.rewardLeaves()
                            + " \u00a78（已存入坊市余额，按 J 切到坊市页签消费）"));
        }
        player.sendSystemMessage(Component.literal(
                "\u00a77进度：" + Quests.indexOf(quest) + " / " + Quests.total()));
        player.sendSystemMessage(Component.literal(""));

        player.playNotifySound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);

        ModNetworkBridge.sync(player);
        return true;
    }

    /** 击杀敌对生物时调用。 */
    public static void onKill(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        data.getQuestProgress().addKill();
    }

    /** 打坐每满一秒调用。 */
    public static void onMeditateSecond(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        data.getQuestProgress().addMeditateSecond();
    }

    /** 采集灵草时调用。 */
    public static void onGrassGathered(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        data.getQuestProgress().addGrass();
    }

    /** 当前应追踪的任务（第一条未领奖的）。 */
    public static Quest tracked(QuestProgress progress) {
        for (Quest quest : Quests.ALL) {
            if (!progress.isClaimed(quest.id())) {
                return quest;
            }
        }
        return null;
    }

    /** 已完成（含待领奖）数量。 */
    public static int completedCount(QuestProgress progress) {
        int n = 0;
        for (Quest quest : Quests.ALL) {
            if (progress.stateOf(quest.id()) >= 1) {
                n++;
            }
        }
        return n;
    }
}
