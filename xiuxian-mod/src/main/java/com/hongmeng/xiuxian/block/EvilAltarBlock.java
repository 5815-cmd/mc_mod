package com.hongmeng.xiuxian.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * <b>恶之祭坛</b>：放入五个维度 boss 的专属掉落物后可以召唤。
 *
 * <p>右击打开五格容器界面；放满五格后点击「召唤」按钮，
 * 消耗五个掉落物并触发整套演出（感染地形 → 光柱 → 血红天 → 天幕裂口 → 巨眼）。</p>
 */
public class EvilAltarBlock extends BaseEntityBlock {

    public EvilAltarBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EvilAltarBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof EvilAltarBlockEntity be) {
            // 用 NetworkHooks.openScreen 把方块坐标一起发过去 ——
            // 客户端才能查回方块实体（普通 openMenu 不带坐标）。
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                net.minecraftforge.network.NetworkHooks.openScreen(sp, be, pos);
            }
        }
        return InteractionResult.CONSUME;
    }
}
