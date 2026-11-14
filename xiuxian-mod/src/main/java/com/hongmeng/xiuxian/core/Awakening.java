package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.ModSounds;
import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.network.BreakthroughFxPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import com.hongmeng.xiuxian.spirit.SpiritRoot;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * 灵根觉醒。第一次进入世界时自动触发，是整个玩法的起点。
 * 抽到什么就是什么——这正是原文里"赌初始号"的紧张感来源。
 */
public final class Awakening {

    private Awakening() {
    }

    /** 觉醒入口。source 用于文案（"天地""造化丹""测灵台"）。 */
    public static void awaken(ServerPlayer player, CultivationData data, String source) {
        if (player == null || data == null || data.isAwakened()) {
            return;
        }
        SpiritRoot root = rollWithPity(new java.util.Random(), data);
        applyRoot(player, data, root, source, true);
    }

    /**
     * 带保底的抽取。
     *
     * <p>连续 10 次没抽到"天灵根及以上"就直接走高档池
     * （天 40% / 先天 35% / 混沌 20% / 鸿蒙 5%）。
     * 抽到好货就把计数清零，否则继续累加。</p>
     */
    public static SpiritRoot rollWithPity(java.util.Random random, CultivationData data) {
        SpiritRoot root;
        if (data.isPityReady()) {
            root = SpiritRoot.rollPity(random);
            data.resetRootPity();
        } else {
            root = SpiritRoot.rollByDifficulty(random, data.difficulty());
            if (SpiritRoot.isGood(root.rarity())) {
                data.resetRootPity();
            } else {
                data.bumpRootPity();
            }
        }
        return root;
    }

    /** 重抽（任务保底 / 测灵台 / 洗灵丹）。 */
    public static void reroll(ServerPlayer player, CultivationData data, boolean guaranteed) {
        if (player == null || data == null) {
            return;
        }
        java.util.Random random = new java.util.Random();
        SpiritRoot root;
        if (guaranteed) {
            // 保底重抽：至少地灵根，且也吃十连保底
            root = SpiritRoot.rollGuaranteed(random);
            data.resetRootPity();
        } else {
            root = rollWithPity(random, data);
        }
        data.addReroll();
        applyRoot(player, data, root, "重测", false);
    }

    private static void applyRoot(ServerPlayer player, CultivationData data, SpiritRoot root,
                                  String source, boolean firstTime) {
        data.setRoot(root);
        data.setAwakened(true);
        ModNetworkBridge.sync(player);

        // 特效
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.2D, player.getZ(),
                    60, 0.5D, 0.8D, 0.5D, 0.08D);
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    40, 0.6D, 0.6D, 0.6D, 0.5D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // 文案
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00a7b\u00a7l═══ 灵 根 觉 醒 ═══"));
        if (firstTime) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77一股热流从丹田冲上头顶，你听见自己经脉里" + "\u201c嗡\u201d" + "的一声。"));
        } else {
            player.sendSystemMessage(Component.literal("\u00a77你借" + source + "之法，重新测了一次灵根。"));
        }
        player.sendSystemMessage(Component.literal(""));
        // ⚠ 原来这里还会显示「属性：<灵根元素>」——灵根的元素维度已被移除
        //   （玩家要求：把灵根的属性设定删了，保留领域的属性设定）。
        player.sendSystemMessage(Component.literal(
                "\u00a7f品质：" + root.rarity().colored()
                        + "   \u00a78元素加成由「道宫·领域」提供"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f灵根：\u00a7f\u00a7l" + root.cn()));
        player.sendSystemMessage(Component.literal("\u00a77" + root.desc()));
        player.sendSystemMessage(Component.literal(
                "\u00a7a生命加成：\u00a7e" + root.rarity().attackBonusText()
                        + "   \u00a7a攻击加成：\u00a7e" + root.rarity().attackBonusText()));
        player.sendSystemMessage(Component.literal(
                "\u00a7a修炼速度倍率：\u00a7e×" + String.format("%.2f", root.multiplier())));
        player.sendSystemMessage(Component.literal(""));

        if (root == SpiritRoot.INNATE_ICE_SWORD) {
            player.sendSystemMessage(Component.literal(
                    "\u00a75\u00a7l【隐藏状态】仙剑灵根 · 冰（变异）"));
            player.sendSystemMessage(Component.literal(
                    "\u00a7d这玩意儿，论坛上有人出价上亿收。"));
        } else if (root.rarity() == com.hongmeng.xiuxian.spirit.Rarity.FALSE_ROOT) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77" + "\u201c……伪灵根。行吧，伪灵根也是灵根。\u201d"));
        } else if (root.rarity().rank() >= com.hongmeng.xiuxian.spirit.Rarity.INNATE.rank()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a76你这个初始号，能卖不少钱。可惜——这里没有买家。"));
        }

        // 觉醒礼物：一枚造化丹
        if (firstTime) {
            ItemStack gift = new ItemStack(ModItems.ZHAOHUA_DAN.get(), 1);
            if (!player.getInventory().add(gift)) {
                player.drop(gift, false);
            }
            player.sendSystemMessage(Component.literal("\u00a7a获得：造化丹 ×1"));
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal(
                    "\u00a78按 \u00a7fR \u00a78打开修炼面板；找块蒲团坐下就能打坐吐纳。"));
            player.sendSystemMessage(Component.literal(
                    "\u00a78打坐涨修为，杀怪涨修为，做任务、撞奇遇、嗑丹药都涨。"));
            player.sendSystemMessage(Component.literal(""));
            ModNetwork.sendToPlayer(player, new BreakthroughFxPacket(
                    BreakthroughFxPacket.Kind.SUCCESS, root.cn()));
        }
        ModNetworkBridge.sync(player);
    }
}
