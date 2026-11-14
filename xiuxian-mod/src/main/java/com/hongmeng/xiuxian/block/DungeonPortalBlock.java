package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.dungeon.Dungeon;
import com.hongmeng.xiuxian.dungeon.DungeonRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 秘境传送门——「按下界门的方式」进入副本的那层门面。
 *
 * <p>框架被点着之后，{@link DungeonPortalFrameBlock} 会把门内的空腔整片换成这个方块；
 * <b>玩家要自己走进去</b>才会被传走，而不是点火即入场。</p>
 *
 * <p><b>贴图是自绘的</b>（玩家要求：「我们的传送门的方块贴图也要换成自己绘制的，
 * 不要套用原本的」）。四个副本各一套，风格一眼可分：</p>
 * <ul>
 *   <li>纪元 —— 青苔石底 + 淡金符文十字；</li>
 *   <li>妖兽 —— 暗褐骨纹 + 琥珀獠牙；</li>
 *   <li>魔道 —— 黑石 + 从亮核炸开的血红裂纹；</li>
 *   <li>虚空 —— 黑曜 + 紫黑漩涡与星芒。</li>
 * </ul>
 *
 * <p>模型见 {@code assets/hongmeng/models/block/dungeon_portal_<主题>_ns.json}
 * 与 {@code _ew.json}（共 8 个），全部通过 {@code render_type: translucent}
 * 与半透明像素保留"能看穿的门"的观感 —— 不是一堵实心墙。</p>
 *
 * <p>一个方块撑起三座副本：目标副本存在 {@link #DUNGEON} 状态里
 * （{@code dungeon=beast|demon|void|epoch}），所以不需要三个方块。</p>
 */
public class DungeonPortalBlock extends Block {

    /** 这座门通往哪座副本。序列化名即副本维度名（{@code beast_realm} 等）。 */
    public static final EnumProperty<PortalTarget> DUNGEON =
            EnumProperty.create("dungeon", PortalTarget.class);

    /** 门面朝向。语义同原版下界门：{@code axis=x} 表示门面沿 X 展开（贴图薄面朝 Z）。 */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    /** 门面厚度 4px，与原版下界门一致。 */
    private static final VoxelShape X_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    private static final VoxelShape Z_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    /** 被境界门槛挡住时的提示冷却（tick）。 */
    private static final long REFUSE_COOLDOWN = 120L;

    /** 每个玩家的上次提示时刻。只在服务端维护，条目数 = 在线人数级别。 */
    private static final Map<UUID, Long> LAST_HINT = new HashMap<>();

    public DungeonPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(DUNGEON, PortalTarget.BEAST)
                .setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DUNGEON, AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    // ------------------------------------------------------------------
    // 走进去就传送
    // ------------------------------------------------------------------

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        // 坐骑/乘客不处理：跟原版下界门一个规矩，免得把船连人一起吞了
        if (player.isPassenger() || player.isVehicle()) {
            return;
        }
        // 原版下界门的传送冷却。**这一步不能省**：退出秘境时玩家会被送回
        // 他进门前站的位置——那正是门里——没有冷却就会立刻被再吸进去，来回死循环。
        if (player.isOnPortalCooldown()) {
            return;
        }

        Dungeon dungeon = state.getValue(DUNGEON).dungeon();
        CultivationData data = CultivationCapability.get(player);

        if (!data.isAwakened() || !dungeon.canEnter(data.realm().tier())) {
            hint(player, level, dungeon, data);
            return;
        }

        // 已经在目标副本里就别再传一次
        if (DungeonRealm.isIn(player, dungeon)) {
            return;
        }

        // 跟原版一样：传送出去之后给 300 tick（15 秒）冷却
        player.setPortalCooldown();
        DungeonRealm.enter(player, dungeon, data);
    }

    /** 进不去的时候说清楚原因，同一条提示 6 秒内不重复刷。 */
    private static void hint(ServerPlayer player, Level level, Dungeon dungeon,
                             CultivationData data) {
        long now = level.getGameTime();
        Long last = LAST_HINT.get(player.getUUID());
        if (last != null && now - last < REFUSE_COOLDOWN) {
            return;
        }
        LAST_HINT.put(player.getUUID(), now);
        if (!data.isAwakened()) {
            player.displayClientMessage(Component.literal(
                    "\u00a7c你连灵根都没有，跨不过这道门。"), true);
            return;
        }
        player.displayClientMessage(Component.literal(
                "\u00a7c" + dungeon.cn() + " \u00a7c需要 \u00a7e"
                        + DungeonRealm.tierName(dungeon.minTier())
                        + " \u00a7c以上。你目前 \u00a7f" + data.realm().fullCn()), true);
    }

    // ------------------------------------------------------------------
    // 客户端表现：跟下界门一样的紫色粒子和环境音
    // ------------------------------------------------------------------

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    0.5F, random.nextFloat() * 0.4F + 0.8F, false);
        }
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double vx = (random.nextDouble() - 0.5D) * 0.5D;
            double vy = (random.nextDouble() - 0.5D) * 0.5D;
            double vz = (random.nextDouble() - 0.5D) * 0.5D;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
        }
    }

    /** 玩家下线时清掉提示记录，别让表一直涨。 */
    public static void forget(UUID player) {
        LAST_HINT.remove(player);
    }
}
