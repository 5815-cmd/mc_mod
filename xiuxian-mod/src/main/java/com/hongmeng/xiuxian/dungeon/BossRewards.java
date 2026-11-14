package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * <b>Boss 场地奖励箱 / 击杀战利品的统一实现</b>（唯一入口）。
 *
 * <p>为什么单独一个类：Boss 场地有 9 类（怪潮战场、4 座决战竞技场、4 座小祭坛房间、
 * 下界/末地巢穴、恶之竞技场、天外天斗台/场景），如果每处各写一遍“放箱子 +
 * 塞 LootTable”，样式、幂等、失败兜底全都会不一致。这里统一三件事：</p>
 * <ol>
 *   <li><b>放箱子</b>：{@link #ensureChest} 幂等、只往空气格放、带朝向与 LootTable；</li>
 *   <li><b>成组放</b>：{@link #placeChests} 按相对坐标批量放（各场地生成器直接调）；</li>
 *   <li><b>击杀战利品</b>：{@link #grantKillLoot} 按副本档位从对应战利品表滚一次，
 *       直接给击杀者（因为打完祭坛首领会被立刻送回原世界，没法再回竞技场开箱）。</li>
 * </ol>
 *
 * <h2>设计红线</h2>
 * <p>五凭证（傀儡核心/兽王之心/血魔之核/虚空之眼/仙庭之冕）<b>绝不放进宝箱</b>——
 * 它们必须由对应 Boss 本体掉落，否则会绕过「恶」的五凭证召唤链。
 * 生成器 {@code tools/gen_boss_venue_loot.py} 里有对应断言。</p>
 *
 * <h2>箱子谁放</h2>
 * <ul>
 *   <li>决战竞技场两侧宝库：{@code ArenaDetail}（新）</li>
 *   <li>怪潮战场补给箱：{@code RealmDetail}（新）</li>
 *   <li>小祭坛房间：{@code AltarArena}（改）</li>
 *   <li>下界/末地巢穴：{@code BossLair}（改）</li>
 *   <li>恶之竞技场：{@code EvilArena}（改）</li>
 *   <li>天外天斗台/场景：{@code HeavenScenes}（Lead 改）</li>
 * </ul>
 *
 * <p>本类不主动订阅事件、不写 SavedData：幂等由“该格已是宝箱就跳过”保证，
 * 场地生成器自身已有版本标记，不会重复调用。</p>
 */
public final class BossRewards {

    /** 四座决战竞技场的击杀战利品表（可重复；见 tools/gen_boss_venue_loot.py）。 */
    public static final ResourceLocation LOOT_ARENA_EPOCH = rl("chests/arena_epoch");
    public static final ResourceLocation LOOT_ARENA_BEAST = rl("chests/arena_beast");
    public static final ResourceLocation LOOT_ARENA_DEMON = rl("chests/arena_demon");
    public static final ResourceLocation LOOT_ARENA_VOID = rl("chests/arena_void");
    /** 怪潮战场补给箱（一次性 / 每世界）。 */
    public static final ResourceLocation LOOT_SUPPLY = rl("chests/supply");
    /** 下界/末地世界 Boss 巢穴宝箱（一次性）。 */
    public static final ResourceLocation LOOT_NATIVE_LAIR = rl("chests/native_lair");
    /** 仙庭之主首杀奖励（首杀一次）。 */
    public static final ResourceLocation LOOT_HEAVEN_LORD = rl("chests/heaven_lord");
    /** 恶之竞技场宝箱（一次）。 */
    public static final ResourceLocation LOOT_EVIL = rl("chests/evil_claim");
    /** 天外天场景隐藏宝箱（探索奖励）。 */
    public static final ResourceLocation LOOT_HEAVEN_SUPPLY = rl("chests/heaven_supply");

    private BossRewards() {
    }

    private static ResourceLocation rl(String path) {
        return new ResourceLocation(HongmengMod.MOD_ID, path);
    }

    /** 副本 → 决战竞技场击杀战利品表。 */
    public static ResourceLocation arenaLootFor(Dungeon dungeon) {
        return switch (dungeon) {
            case EPOCH -> LOOT_ARENA_EPOCH;
            case BEAST -> LOOT_ARENA_BEAST;
            case DEMON -> LOOT_ARENA_DEMON;
            case VOID -> LOOT_ARENA_VOID;
        };
    }

    /**
     * 幂等放一个宝箱。
     *
     * <p>规则：只往空气/可替换方块上放；已经放了<b>同样战利品表</b>的箱子就跳过
     * （这样场地生成器重复调用不会把玩家开过的箱子重置成新战利品）。</p>
     *
     * @param level 维度
     * @param pos   目标格（通常取地面上一格）
     * @param loot  战利品表
     * @param seed  随机种子（同一场地固定种子，保证首开战利品可复现）
     * @return 本次是否真的新增了箱子
     */
    public static boolean ensureChest(ServerLevel level, BlockPos pos, ResourceLocation loot,
                                      long seed) {
        if (level == null || pos == null || loot == null || !level.isLoaded(pos)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity) {
            // 该格已经是容器（我们之前放的箱子 / 玩家自己放的箱子）：幂等跳过，绝不覆盖。
            // 注：1.20.1 官方映射没有公开的 getLootTable()，所以按“是不是容器”判断，
            // 不区分表——生成器每个场地只放一次，这个判定已经足够。
            return false;
        }
        BlockState st = level.getBlockState(pos);
        if (!st.isAir() && !st.canBeReplaced()) {
            // 目标格被占：尝试往上找一格（地形起伏时的兜底）
            BlockPos up = pos.above();
            if (level.getBlockEntity(up) instanceof RandomizableContainerBlockEntity) {
                return false;
            }
            if (!level.getBlockState(up).isAir() && !level.getBlockState(up).canBeReplaced()) {
                HongmengMod.LOGGER.debug("[reward] 宝箱位置被占，跳过：{}", pos);
                return false;
            }
            pos = up;
        }
        BlockState chestState = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.SOUTH);
        level.setBlock(pos, chestState, 3);
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
            chest.setLootTable(loot, seed);
            HongmengMod.LOGGER.info("[reward] 奖励箱已放置：{} @ {}（维度 {}）",
                    loot, pos, level.dimension().location());
            return true;
        }
        HongmengMod.LOGGER.warn("[reward] 宝箱方块实体会话未就绪，保留空箱：{}", pos);
        return false;
    }

    /**
     * 按相对坐标成组放箱（场地生成器用）。
     *
     * @param origin 场地基准（通常是竞技场中心/巢穴中心，y=地面方块层）
     * @param offsets {dx,dy,dz} 数组；每项放一个箱子
     * @return 实际新增数量
     */
    public static int placeChests(ServerLevel level, BlockPos origin, int[][] offsets,
                                  ResourceLocation loot, long seed) {
        int placed = 0;
        for (int[] o : offsets) {
            BlockPos p = origin.offset(o[0], o[1], o[2]);
            if (ensureChest(level, p, loot, seed + p.asLong())) {
                placed++;
            }
        }
        return placed;
    }

    /**
     * 击杀祭坛首领的即时战利品。
     *
     * <p>为什么直接给而不是在竞技场放箱：打完首领 `DungeonRealm.onArenaBossKilled`
     * 会立刻把玩家送回原世界（否则被困 boss 维度），箱子留在竞技场等于拿不到。
     * 场地里的“宝库箱”由场地生成器提前放好（玩家探索时开）。</p>
     */
    public static void grantKillLoot(ServerPlayer player, Dungeon dungeon) {
        if (player == null || dungeon == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ResourceLocation loot = arenaLootFor(dungeon);
        rollToPlayer(level, player, loot, player.getUUID().getLeastSignificantBits()
                ^ level.getGameTime());
    }

    /** 仙庭之主首杀额外战利品（首杀由 HeavenRealm 的 lordDefeated 保证只触发一次）。 */
    public static void grantHeavenLordLoot(ServerPlayer player) {
        if (player == null) {
            return;
        }
        rollToPlayer(player.serverLevel(), player, LOOT_HEAVEN_LORD,
                player.getUUID().getLeastSignificantBits() ^ 0x48454C4CL);
    }

    /** 恶之竞技场结算战利品（由恶的收尾流程调用）。 */
    public static void grantEvilLoot(ServerPlayer player) {
        if (player == null) {
            return;
        }
        rollToPlayer(player.serverLevel(), player, LOOT_EVIL,
                player.getUUID().getLeastSignificantBits() ^ 0x4556494CL);
    }

    /**
     * 在 (x,z) 从上往下找第一个“脚下实心、身位是空气”的站立格。
     *
     * <p>给 {@code /hongmeng bossreward} 用：命令只能拿到“竞技场中心”（可能是祭坛顶面），
     * 需要落到实际地面再放箱子；只读已加载区块，未加载就返回 hintY 兜底。</p>
     *
     * @param hintY 从这里往下最多找 8 格（通常传 中心 y + 2）
     */
    public static BlockPos findFloorStand(ServerLevel level, int x, int z, int hintY) {
        for (int dy = 0; dy <= 8; dy++) {
            int y = hintY - dy;
            BlockPos p = new BlockPos(x, y, z);
            if (!level.isLoaded(p)) {
                continue;
            }
            if (!level.getBlockState(p).isAir()) {
                continue;
            }
            BlockPos below = p.below();
            if (level.isLoaded(below) && !level.getBlockState(below).isAir()
                    && !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return p;
            }
        }
        return new BlockPos(x, hintY, z);
    }

    /**
     * 验收辅助：读一个宝箱方块实体里的 LootTable（没有则返回 "-"）。
     *
     * <p>给 {@code /hongmeng bossreward} 用：headless RCON 跑测时，
     * 没有玩家去开箱，只能靠这个把“坐标 + 表 id”直接打进命令回显。</p>
     */
    public static String lootTableAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return "unloaded";
        }
        if (!(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest)) {
            return "-";
        }
        try {
            net.minecraft.nbt.CompoundTag tag = chest.saveWithoutMetadata();
            String table = tag.getString("LootTable");
            return table.isEmpty() ? "-" : table;
        } catch (Throwable t) {
            return "err:" + t.getClass().getSimpleName();
        }
    }

    /**
     * 验收辅助：把一组坐标上的宝箱描述成一行（坐标=表）。
     *
     * @return 例如 {@code (-7,66,0)=hongmeng:chests/arena_epoch, ...}
     */
    public static String describeChests(ServerLevel level, BlockPos origin, int[][] offsets) {
        StringBuilder sb = new StringBuilder();
        for (int[] o : offsets) {
            BlockPos p = origin.offset(o[0], o[1], o[2]);
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p).append('=').append(lootTableAt(level, p));
        }
        return sb.toString();
    }

    /** 把一张战利品表滚出来的物品直接塞给玩家（背包满则掉落脚下）。 */
    public static void rollToPlayer(ServerLevel level, ServerPlayer player,
                                    ResourceLocation loot, long seed) {
        if (level == null || player == null || loot == null) {
            return;
        }
        try {
            LootTable table = level.getServer().getLootData().getLootTable(loot);
            if (table == LootTable.EMPTY) {
                HongmengMod.LOGGER.warn("[reward] 战利品表不存在或为空：{}", loot);
                return;
            }
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .create(LootContextParamSets.CHEST);
            List<ItemStack> items = table.getRandomItems(params, seed);
            int given = 0;
            for (ItemStack stack : items) {
                if (stack.isEmpty()) {
                    continue;
                }
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                given += stack.getCount();
            }
            if (given > 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a76\u00a7l【战利品】\u00a7e" + given + " \u00a77件物品已入背包。"), false);
            }
            HongmengMod.LOGGER.info("[reward] {} 领取 {}：{} 件", player.getName().getString(),
                    loot, given);
        } catch (Throwable t) {
            HongmengMod.LOGGER.error("[reward] 发放战利品失败：{}", loot, t);
        }
    }
}
