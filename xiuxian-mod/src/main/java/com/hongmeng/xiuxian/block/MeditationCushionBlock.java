package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 打坐蒲团。右键坐下开始吐纳：站上去不走，修为就一秒一秒往上走。
 * "不爱加班的懒虫"用这个最省事。
 */
public class MeditationCushionBlock extends HorizontalDirectionalBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 4.0D, 15.0D);

    public MeditationCushionBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
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
                    "\u00a7c灵根未觉醒，坐上去也只是发呆。"));
            return InteractionResult.CONSUME;
        }
        if (data.isMaxRealm()) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a76已至鸿蒙。这一坐，是给自己的。"));
        }

        boolean meditating = serverPlayer.getPersistentData().getBoolean("hongmeng_meditating");
        if (meditating) {
            serverPlayer.getPersistentData().putBoolean("hongmeng_meditating", false);
            serverPlayer.sendSystemMessage(Component.literal("\u00a77你从蒲团上站了起来。"), true);
            return InteractionResult.CONSUME;
        }

        serverPlayer.getPersistentData().putBoolean("hongmeng_meditating", true);
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7b你盘膝坐下，气沉丹田。\u00a77（再右键一次起身；走动不会打断吐纳，但也不好）"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            for (Player player : level.players()) {
                if (player instanceof ServerPlayer sp && sp.blockPosition().closerThan(pos, 2.0D)) {
                    sp.getPersistentData().putBoolean("hongmeng_meditating", false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
