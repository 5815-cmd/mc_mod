package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.QuestChain;
import com.hongmeng.xiuxian.spirit.SpiritRoot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 测灵台：站上去重测灵根，保底玄品以上。
 * 这是任务链给的"洗灵根保底"，不至于让人抽到凡品就废档。
 */
public class RootAltarBlock extends Block {

    public RootAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        CultivationData data = CultivationCapability.get(serverPlayer);
        if (!data.isAwakened()) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7c灵根都没觉醒，测什么？"));
            return InteractionResult.CONSUME;
        }

        // 冷切：同一座台子一分钟只能测一次
        int lastUse = serverPlayer.getPersistentData().getInt("hongmeng_altar_cd");
        if (serverPlayer.tickCount - lastUse < 1200 && lastUse != 0) {
            int left = (1200 - (serverPlayer.tickCount - lastUse)) / 20;
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a77测灵台尚有余韵，静候 " + left + " 秒再来。"));
            return InteractionResult.CONSUME;
        }
        serverPlayer.getPersistentData().putInt("hongmeng_altar_cd", serverPlayer.tickCount);

        SpiritRoot old = data.getRoot();
        SpiritRoot fresh = SpiritRoot.rollGuaranteed();
        data.setRoot(fresh);
        data.addReroll();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                    40, 0.4D, 0.5D, 0.4D, 0.4D);
            serverLevel.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS, 1.0F, 1.2F);
        }

        serverPlayer.sendSystemMessage(Component.literal(""));
        serverPlayer.sendSystemMessage(Component.literal("\u00a7b测灵台嗡鸣，符文逐一亮起——"));
        if (old != null) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a78此前：" + old.rarity().colored() + " \u00a7f" + old.cn()));
        }
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7f如今：" + fresh.rarity().colored() + " \u00a7f\u00a7l" + fresh.cn()));
        serverPlayer.sendSystemMessage(Component.literal("\u00a77" + fresh.desc()));
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7a修炼速度倍率：\u00a7e×" + String.format("%.2f", fresh.multiplier())));
        serverPlayer.sendSystemMessage(Component.literal(""));

        if (fresh == SpiritRoot.INNATE_ICE_SWORD) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a75\u00a7l【隐藏状态】仙剑灵根 · 冰（变异）——就是你听说过的那个。"));
        }

        QuestChain.onAltar(serverPlayer);
        com.hongmeng.xiuxian.network.ModNetworkBridge.sync(serverPlayer);
        return InteractionResult.CONSUME;
    }
}
