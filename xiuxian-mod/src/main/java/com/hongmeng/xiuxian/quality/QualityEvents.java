package com.hongmeng.xiuxian.quality;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 品质系统的<b>运行时入口</b>：把纯数据表接到游戏事件上。
 *
 * <p>三件事：</p>
 * <ol>
 *   <li><b>tooltip</b>（{@link ItemTooltipEvent}）：有品质的东西多一行彩色的
 *       「品质：上品 效果 ×3」。走事件而不是改每一个物品类，
 *       是因为有品质的东西会越来越多（丹药 / 法宝 / 法宝剑 / 花瓣 / 妖丹），
 *       逐个改 tooltip 迟早漏掉一个。</li>
 *   <li><b>合成盖章</b>（{@link PlayerEvent.ItemCraftedEvent}）：
 *       合成品盖上"合成品质"（凡品，但不会把完美修为丹打成凡品）。</li>
 *   <li><b>妖兽掉落掷骰</b>（{@link LivingDropsEvent}）：只有表里认得的物品才掷，
 *       并且<b>只掷一次</b>（已经有品质的不覆盖），避免玩家用"捡了再丢"刷品质。</li>
 * </ol>
 *
 * <p>炼丹出锅的品质由丹炉那边调用 {@link ItemQuality#applyAlchemy}——
 * 见 {@code docs/quality-and-biomes-spec.md}（那一处需要炼丹模块配合一行）。</p>
 */
public final class QualityEvents {

    /** 需要被事件总线实例化。 */
    public QualityEvents() {
    }

    /**
     * 纯数据自检总入口（模组加载期与服务器启动期各跑一次）。
     *
     * <p>只调用纯数据类，不碰注册表。</p>
     *
     * @throws IllegalStateException 任何一张表不合法
     */
    public static void validateAll() {
        Quality.validate();
        QualitySource.validate();
        QualityTable.validate();
        AlchemyQuality.validate();
    }

    /** 启动时自检 + 往日志里留一条可 grep 的汇总。 */
    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        validateAll();
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[hongmeng] 品质系统就绪：{} 档（{}），覆盖 {} 件物品，"
                        + "合成固定 {}、炼丹/掉落掷骰，回收价神品 {}",
                Quality.COUNT, Quality.ladderText(), QualityTable.ids().size(),
                QualitySource.CRAFTED.fixedQuality().cn(),
                Quality.DIVINE.sellText());
    }

    /** tooltip：把品质行插在物品名下面。 */
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!ItemQuality.showsTooltip(stack)) {
            return;
        }
        String line = ItemQuality.tooltipLine(stack);
        var tooltip = event.getToolTip();
        if (tooltip.size() >= 1) {
            tooltip.add(1, Component.literal(line));
        } else {
            tooltip.add(Component.literal(line));
        }
    }

    /** 合成：盖章（不掷骰子）。 */
    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (crafted == null || crafted.isEmpty()) {
            return;
        }
        ItemQuality.applyCrafted(crafted);
    }

    /**
     * 妖兽掉落：按怪物强度掷品质。
     *
     * <p>刻意跳过玩家：玩家死亡掉落 / 主动丢出的东西不该被重新掷骰，
     * 否则"丢出去再捡回来"就能把凡品刷成神品。</p>
     */
    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        double maxHealth = event.getEntity().getMaxHealth();
        int shift = QualityTable.dropShift(maxHealth);
        Random rng = ThreadLocalRandom.current();
        for (ItemEntity drop : event.getDrops()) {
            if (drop == null) {
                continue;
            }
            ItemStack stack = drop.getItem();
            if (stack.isEmpty() || !ItemQuality.canHaveQuality(stack) || ItemQuality.hasTag(stack)) {
                continue;
            }
            ItemQuality.rollOnce(stack, QualitySource.MOB_DROP, shift, rng);
        }
    }
}
