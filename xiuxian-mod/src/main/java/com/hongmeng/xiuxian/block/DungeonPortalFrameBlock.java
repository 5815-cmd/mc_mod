package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.ModBlocks;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.dungeon.Dungeon;
import com.hongmeng.xiuxian.dungeon.DungeonRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * 「秘境之门」框架的共同实现——打火石 / 火焰弹点着，门内注满
 * {@link DungeonPortalBlock}，玩家<b>走进去</b>才算进门。
 *
 * <p>结构校验：门内空腔必须 <b>2~4 格宽、3~5 格高</b>，四边（含四个角）
 * 全部由<b>同一种</b>框架块围死。三个副本各自一个子类，
 * 只是把 {@link Dungeon} 传进来（见 {@link BeastPortalFrameBlock} 等）。</p>
 *
 * <p><b>与 {@link EpochPortalFrameBlock} 的差异（刻意）：</b>纪元之门那把校验器
 * 只检查"横向一条 2~6 长的空气带 + 上下各一块框架"，既没有宽度/高度上限，
 * 也没有检查左右两侧——一个 L 形都能点着。这里按规格里写的
 * 2~4 × 3~5 做了完整的矩形闭环校验（外框 + 四角），
 * 然后把空腔整片填成传送门方块。点火的交互风格（打火石/火焰弹、
 * 未觉醒拒绝、粒子与音效、{@code InteractionResult} 的用法）与它保持一致。</p>
 */
public abstract class DungeonPortalFrameBlock extends Block {

    /** 门内空腔的最小 / 最大尺寸。 */
    public static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 4;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 5;

    /** 拆框时向四周找门的搜索半径：门最宽 4、最高 5，7 格足够覆盖。 */
    private static final int CLEANUP_RADIUS = 7;
    /** 单次拆除最多清掉多少个传送门方块，防止误伤过头。 */
    private static final int CLEANUP_LIMIT = 160;

    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private final Dungeon dungeon;

    protected DungeonPortalFrameBlock(Properties properties, Dungeon dungeon) {
        super(properties);
        this.dungeon = dungeon;
    }

    /** 这座门通往哪座副本。 */
    public Dungeon dungeon() {
        return dungeon;
    }

    // ------------------------------------------------------------------
    // 点火
    // ------------------------------------------------------------------

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.FLINT_AND_STEEL) && !held.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        CultivationData data = CultivationCapability.get(serverPlayer);
        if (!data.isAwakened()) {
            serverPlayer.sendSystemMessage(Component.literal("\u00a7c你连灵根都没有，进去就是送菜。"));
            return InteractionResult.CONSUME;
        }

        // 境界门槛：不到门槛连门都不给开，免得白跑一趟
        if (!dungeon.canEnter(data.realm().tier())) {
            serverPlayer.sendSystemMessage(Component.literal(dungeon.colored()
                    + "\u00a7c需要 \u00a7e" + DungeonRealm.tierName(dungeon.minTier())
                    + " \u00a7c以上的修为才能开启。你目前是 \u00a7f"
                    + data.realm().fullCn() + "\u00a7c。"));
            serverPlayer.sendSystemMessage(Component.literal("\u00a77（" + dungeon.desc() + "）"));
            return InteractionResult.CONSUME;
        }

        Frame frame = findFrame(level, pos);
        if (frame == null) {
            serverPlayer.sendSystemMessage(Component.literal("\u00a7c门没搭对。需要 "
                    + MIN_WIDTH + "~" + MAX_WIDTH + " 格宽、" + MIN_HEIGHT + "~" + MAX_HEIGHT
                    + " 格高的框架，四边围死，中间空心。"));
            return InteractionResult.CONSUME;
        }

        light(level, frame);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    frame.centerX(), frame.centerY(), frame.centerZ(),
                    160, 0.7D, 1.4D, 0.7D, 0.5D);
            serverLevel.playSound(null, pos, SoundEvents.PORTAL_TRIGGER,
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        serverPlayer.sendSystemMessage(Component.literal(dungeon.colored()
                + " \u00a77的门已经开了。\u00a7f走进去\u00a77就会被送进秘境，"
                + "\u00a7f潜行 + 空手右键\u00a77可以随时退出。"));
        return InteractionResult.CONSUME;
    }

    /** 把门内空腔整片换成传送门方块。 */
    protected void light(Level level, Frame frame) {
        BlockState portal = ModBlocks.DUNGEON_PORTAL.get().defaultBlockState()
                .setValue(DungeonPortalBlock.DUNGEON, PortalTarget.of(dungeon))
                .setValue(DungeonPortalBlock.AXIS, frame.axis());
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (int h = 0; h < frame.height(); h++) {
            for (int w = 0; w < frame.width(); w++) {
                level.setBlock(frame.corner().relative(frame.along(), w).above(h), portal, flags);
            }
        }
    }

    // ------------------------------------------------------------------
    // 结构校验
    // ------------------------------------------------------------------

    /**
     * 从被点着的框架块出发找门内空腔。
     *
     * <p>六个方向都试：先落到空腔底部、再沿候选轴退到空腔边缘，
     * 然后按 2~4 × 3~5 的矩形闭环校验。找不到就返回 {@code null}。</p>
     */
    protected Frame findFrame(Level level, BlockPos framePos) {
        for (Direction dir : Direction.values()) {
            BlockPos seed = framePos.relative(dir);
            if (!isOpen(level, seed)) {
                continue;
            }
            // 落到空腔底行（点的是侧柱中段也能对上）
            BlockPos bottom = seed;
            for (int i = 0; i <= MAX_HEIGHT && isOpen(level, bottom.below()); i++) {
                bottom = bottom.below();
            }

            if (dir.getAxis().isVertical()) {
                // 框架块在门内空腔的正上 / 正下方 → 门面内那条水平轴四种都可能
                for (Direction horizontal : HORIZONTALS) {
                    Frame frame = validate(level, bottom, horizontal.getClockWise());
                    if (frame != null) {
                        return frame;
                    }
                }
            } else {
                // 框架块在门面内（空腔在它侧面）：dir 就是门面内那条水平轴
                Frame frame = validate(level, bottom, dir);
                if (frame != null) {
                    return frame;
                }
                // 框架块在门面外一格（为了跟纪元之门的老搭法兼容）：dir 是法线
                Frame outer = validate(level, bottom, dir.getClockWise());
                if (outer != null) {
                    return outer;
                }
            }
        }
        return null;
    }

    /**
     * 以 {@code seed} 为起点，沿 {@code along} 退回空腔边缘，再校验整个矩形。
     *
     * @param along  空腔宽度延伸的方向（水平）
     */
    private Frame validate(Level level, BlockPos seed, Direction along) {
        if (!along.getAxis().isHorizontal()) {
            return null;
        }
        BlockPos corner = seed;
        for (int i = 0; i <= MAX_WIDTH && isOpen(level, corner.relative(along, -1)); i++) {
            corner = corner.relative(along, -1);
        }

        int width = 0;
        while (width <= MAX_WIDTH && isOpen(level, corner.relative(along, width))) {
            width++;
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            return null;
        }

        int height = 0;
        while (height <= MAX_HEIGHT && isOpen(level, corner.above(height))) {
            height++;
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            return null;
        }

        // 左右两列（h = -1 与 h = height 同时把上下两排的角也带上）
        for (int h = -1; h <= height; h++) {
            if (!isFrame(level, corner.relative(along, -1).above(h))) {
                return null;
            }
            if (!isFrame(level, corner.relative(along, width).above(h))) {
                return null;
            }
        }
        // 上下两排
        for (int w = 0; w < width; w++) {
            if (!isFrame(level, corner.relative(along, w).below())) {
                return null;
            }
            if (!isFrame(level, corner.relative(along, w).above(height))) {
                return null;
            }
        }
        return new Frame(corner, along, width, height, along.getAxis());
    }

    /** 只认自己这一种框架块：三种门不能混着搭。 */
    protected boolean isFrame(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(this);
    }

    /** 空着的格子：空气、可替换方块，或者已经填好的传送门（重复点火要能对上）。 */
    protected boolean isOpen(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced()
                || state.is(ModBlocks.DUNGEON_PORTAL.get());
    }

    /** 门内空腔：左下角 + 宽度方向 + 尺寸 + 门面朝向。 */
    protected record Frame(BlockPos corner, Direction along, int width, int height,
                           Direction.Axis axis) {

        double centerX() {
            return corner.getX() + 0.5D + along.getStepX() * (width - 1) / 2.0D;
        }

        double centerY() {
            return corner.getY() + height / 2.0D;
        }

        double centerZ() {
            return corner.getZ() + 0.5D + along.getStepZ() * (width - 1) / 2.0D;
        }
    }

    // ------------------------------------------------------------------
    // 拆框即拆门
    // ------------------------------------------------------------------

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean isMoving) {
        if (!isMoving && !level.isClientSide && !state.is(newState.getBlock())) {
            removeNearbyPortals(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * 框架被拆掉时，把周围属于本副本的传送门方块一并收掉。
     *
     * <p>尽力而为：先在小盒子里找种子，再从种子泛洪，所以整扇门会一起消失，
     * 不会留下半扇"走进去还能传送"的残门。</p>
     */
    private void removeNearbyPortals(Level level, BlockPos pos) {
        int removed = 0;
        BlockPos min = pos.offset(-CLEANUP_RADIUS, -CLEANUP_RADIUS, -CLEANUP_RADIUS);
        BlockPos max = pos.offset(CLEANUP_RADIUS, CLEANUP_RADIUS, CLEANUP_RADIUS);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            if (removed >= CLEANUP_LIMIT) {
                break;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.DUNGEON_PORTAL.get())
                    && state.getValue(DungeonPortalBlock.DUNGEON).dungeon() == dungeon) {
                removed += collapse(level, cursor.immutable());
            }
        }
    }

    /** 从一块传送门开始泛洪拆除同一座副本的门，返回拆掉的格数。 */
    private int collapse(Level level, BlockPos start) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);

        int removed = 0;
        while (!queue.isEmpty() && removed < CLEANUP_LIMIT) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);
            if (!state.is(ModBlocks.DUNGEON_PORTAL.get())
                    || state.getValue(DungeonPortalBlock.DUNGEON).dungeon() != dungeon) {
                continue;
            }
            level.setBlock(current, Blocks.AIR.defaultBlockState(), flags);
            removed++;
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return removed;
    }
}
