package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import com.hongmeng.xiuxian.core.QuestChain;
import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 灵草。野外的灵气节点：打坐时旁边有几株会加成，采下来还能推进任务。
 */
public class SpiritGrassBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public SpiritGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.tags.BlockTags.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.CLAY)
                || state.is(net.minecraft.world.level.block.Blocks.SAND);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.6D,
                    pos.getY() + 0.4D + random.nextDouble() * 0.5D,
                    pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.6D,
                    0.0D, 0.01D, 0.0D);
        }
    }

    @Override
    public void playerDestroy(Level level, net.minecraft.world.entity.player.Player player,
                              BlockPos pos, BlockState state,
                              net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                              ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CultivationData data = CultivationCapability.get(serverPlayer);
        CultivationSource.ADVENTURE.grant(serverPlayer, 25.0D, true);

        // 灵叶：2~4 片（2026-11-07 玩家「灵叶也可以采集什么的」：采集手感要够，
        // 从 1~3 提到 2~4；灵植方块也另加了一份灵叶掉落，见 loot_tables/blocks/*_grass.json）。
        ItemStack drop = new ItemStack(ModItems.LINGYE.get(),
                2 + serverPlayer.getRandom().nextInt(3));
        popResource(serverLevel, pos, drop);

        // 小概率掉丹药：普通修为丹最常见，完美偶尔，仙品极少
        int roll = serverPlayer.getRandom().nextInt(100);
        if (roll < 18) {
            popResource(serverLevel, pos, new ItemStack(ModItems.PILL_EXP_COMMON.get(), 1));
        } else if (roll < 23) {
            popResource(serverLevel, pos, new ItemStack(ModItems.PILL_EXP_PERFECT.get(), 1));
        } else if (roll < 24) {
            popResource(serverLevel, pos, new ItemStack(ModItems.PILL_EXP_IMMORTAL.get(), 1));
        } else if (roll < 36) {
            popResource(serverLevel, pos, new ItemStack(ModItems.PILL_HUILING.get(), 1));
        }
        QuestChain.onSpiritGrass(serverPlayer);
        com.hongmeng.xiuxian.quest.QuestManager.onGrassGathered(serverPlayer);
    }
}
