package com.hongmeng.xiuxian.alchemy;

import com.hongmeng.xiuxian.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * 丹炉：炼丹系统的方块本体。
 *
 * <p>右键开界面，界面里四个格子：<b>主料 / 辅料 / 燃料 / 产物</b>。
 * 主料 + 辅料决定炼出哪颗丹药（配方表见 {@link AlchemyRecipes}），
 * 燃料是**灵叶**（1 片烧一炉）或**烈焰粉**（1 份烧 12 炉）。
 * 逻辑全在 {@link AlchemyFurnaceBlockEntity} 里。</p>
 *
 * <h2>几个刻意的选择</h2>
 * <ul>
 *   <li><b>整方块 + {@code RenderShape.MODEL}</b>：不用方块实体渲染器（BER）。
 *       模型直接走 blockstates/models 那一套，既省一个渲染器，也省掉
 *       "忘了注册 BER 结果炉子是隐形的"这种事故。
 *       历史事故里还有一条：{@code getShape} 里解引用 level 会导致左键 NPE——
 *       这里不覆盖 {@code getShape}，用标准立方体碰撞箱，从根上避开。</li>
 *   <li><b>{@code LIT} 状态 = 正在烧</b>：客户端据此换模型（炉口有火）并冒烟，
 *       服务端也靠它同步"炉子是不是点着的"。</li>
 *   <li><b>拆炉子掉内容物</b>：{@code onRemove} 里 {@code Containers.dropContents}，
 *       不然玩家一镐子下去，材料全没了（原版熔炉同款处理）。</li>
 * </ul>
 */
public class AlchemyFurnaceBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public AlchemyFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE));
    }

    /** 丹炉的方块属性（给 {@code ModBlocks} 用，样式集中在这里）。 */
    public static Properties furnaceProperties() {
        return Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(3.5F, 6.0F)
                .sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(LIT)
                        ? 13
                        : 0)
                .requiresCorrectToolForDrops();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /** 走普通方块模型，不用 BER。 */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, Boolean.FALSE);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemyFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModAlchemy.ALCHEMY_FURNACE_BE.get(),
                AlchemyFurnaceBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            // 客户端只负责"手挥一下"，真正的界面由服务端 openMenu 下发
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) {
            serverPlayer.openMenu(furnace);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) {
                Containers.dropContents(level, pos, furnace);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }

    /** 点着的时候：炉口冒烟 + 火星，远远看一眼就知道在炼丹。 */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.02D;
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.35D) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    0.35F, 1.0F, false);
        }
        level.addParticle(ParticleTypes.SMOKE,
                x + (random.nextDouble() - 0.5D) * 0.4D, y, z + (random.nextDouble() - 0.5D) * 0.4D,
                0.0D, 0.02D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    x + (random.nextDouble() - 0.5D) * 0.3D, y - 0.1D,
                    z + (random.nextDouble() - 0.5D) * 0.3D,
                    0.0D, 0.01D, 0.0D);
        }
    }
}
