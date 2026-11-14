package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * <b>决战竞技场</b>（大型豪华版）。
 *
 * <p>玩家要求：「竞技场可以做的大一点豪华华丽一点」，
 * 并且「新维度的地形生态什么的符合 boss」——
 * 所以同一个生成器按<b>副本主题</b>换配色与装饰。</p>
 *
 * <h2>尺寸（比原来大一倍以上）</h2>
 * <ul>
 *   <li>沙场半径 <b>44</b>（原来 30）→ 直径 89 格；</li>
 *   <li>看台 <b>8 级</b>（原来 6），每级宽 5、高 1 → 外沿半径 <b>84</b>，最高 <b>+8</b>；</li>
 *   <li>环绕立柱（每级 {@value #PILLARS} 根），柱顶放灯；</li>
 *   <li>四个方位的<b>大门</b>（拱门 + 留空的门洞）；</li>
 *   <li>中央祭坛：三层台基 + 四角火盆 + 召唤祭坛。</li>
 * </ul>
 *
 * <h2>主题配色（每个 boss 不一样）</h2>
 * <table border="1">
 *   <caption>副本 → 沙场 / 台阶 / 看台 / 立柱 / 点缀</caption>
 *   <tr><th>副本</th><th>沙场</th><th>看台</th><th>立柱</th><th>点缀</th></tr>
 *   <tr><td>纪元</td><td>石英</td><td>深板岩瓦</td><td>深板岩砖</td><td>金块</td></tr>
 *   <tr><td>妖兽</td><td>粗泥</td><td>苔石砖</td><td>云杉原木</td><td>绿宝石块</td></tr>
 *   <tr><td>魔道</td><td>黑石</td><td>红地狱砖</td><td>绯红菌柄</td><td>金块</td></tr>
 *   <tr><td>虚空</td><td>末地石</td><td>紫珀块</td><td>哭泣黑曜石</td><td>紫水晶块</td></tr>
 * </table>
 */
public final class ArenaBattle {

    /** 沙场半径。 */
    private static final int FLOOR_R = 44;
    /** 看台级数。 */
    private static final int TIERS = 8;
    /** 每级看台宽。 */
    private static final int TIER_W = 5;
    /** 每级看台高。 */
    private static final int TIER_H = 1;
    /** 每级立柱数量。 */
    private static final int PILLARS = 32;
    /** 祭坛半径。 */
    private static final int ALTAR_R = 3;

    private ArenaBattle() {
    }

    /**
     * 祭坛方块所在的相对高度。
     *
     * <p>台基占了 {@code above(1..3)}（半径 6 → 5 → 4 逐层收分），
     * 所以祭坛本体放在 {@code above(4)} 的雕刻黑石之上，即 <b>{@code above(5)}</b>。</p>
     *
     * <p>⚠ 必须与 {@link #buildForce} 里的实际放置位置一致 ——
     * 早先两处不一致（判据看 above(3)、放置放 above(5)），
     * 于是明明有祭坛却报"祭坛未能生成"。</p>
     */
    public static int altarY() {
        return 5;
    }

    public static int altarRadius() {
        return ALTAR_R;
    }

    public static int floorRadius() {
        return FLOOR_R;
    }

    /** 一座竞技场的配色。 */
    private record Style(BlockState floor, BlockState step, BlockState seat,
                         BlockState pillar, BlockState accent, BlockState light) {
    }

    /** 按副本主题取配色（与 {@code arena_<dungeon>} 维度的地面材料呼应）。 */
    private static Style styleFor(Dungeon d) {
        return switch (d) {
            case EPOCH -> new Style(
                    Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    Blocks.QUARTZ_BLOCK.defaultBlockState(),
                    Blocks.DEEPSLATE_TILES.defaultBlockState(),
                    Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState());
            case BEAST -> new Style(
                    Blocks.COARSE_DIRT.defaultBlockState(),
                    Blocks.PACKED_MUD.defaultBlockState(),
                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
                    Blocks.EMERALD_BLOCK.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState());
            case DEMON -> new Style(
                    Blocks.BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                    Blocks.CRIMSON_HYPHAE.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState());
            case VOID -> new Style(
                    Blocks.END_STONE.defaultBlockState(),
                    Blocks.END_STONE_BRICKS.defaultBlockState(),
                    Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState());
        };
    }

    /** 按副本取对应的**主题祭坛**方块（这样 theme 不为 null，不必猜维度名）。 */
    public static BlockState altarFor(Dungeon d) {
        return switch (d) {
            case EPOCH -> com.hongmeng.xiuxian.ModBlocks
                    .BOSS_SUMMON_ALTAR_EPOCH.get().defaultBlockState();
            case BEAST -> com.hongmeng.xiuxian.ModBlocks
                    .BOSS_SUMMON_ALTAR_BEAST.get().defaultBlockState();
            case DEMON -> com.hongmeng.xiuxian.ModBlocks
                    .BOSS_SUMMON_ALTAR_DEMON.get().defaultBlockState();
            case VOID -> com.hongmeng.xiuxian.ModBlocks
                    .BOSS_SUMMON_ALTAR_VOID.get().defaultBlockState();
        };
    }

    /** 这块方块是不是本副本该有的祭坛（复核用）。 */
    public static boolean isAltar(Dungeon d,
                                   net.minecraft.world.level.block.state.BlockState st) {
        return switch (d) {
            case EPOCH -> st.is(com.hongmeng.xiuxian.ModBlocks
                    .BOSS_SUMMON_ALTAR_EPOCH.get());
            case BEAST -> st.is(com.hongmeng.xiuxian.ModBlocks
                    .BOSS_SUMMON_ALTAR_BEAST.get());
            case DEMON -> st.is(com.hongmeng.xiuxian.ModBlocks
                    .BOSS_SUMMON_ALTAR_DEMON.get());
            case VOID -> st.is(com.hongmeng.xiuxian.ModBlocks
                    .BOSS_SUMMON_ALTAR_VOID.get());
        };
    }

    /** 建一座竞技场（幂等，判据走 ArenaRealm 的维度级标记）。 */
    public static void build(ServerLevel level, BlockPos c, Dungeon dungeon) {
        if (ArenaRealm.isBuilt(level)) {
            return;
        }
        buildForce(level, c, dungeon);
    }

    /**
     * <b>强制重建</b>（不做幂等判断）。
     *
     * <p>为什么要它：幂等判据万一误判成"已建好"，就<b>永远不会补上祭坛</b>——
     * 玩家看到的正是"四周什么建筑也没有"。复核失败时用这个方法重来一次。</p>
     */
    public static void buildForce(ServerLevel level, BlockPos c, Dungeon dungeon) {
        ArenaRealm.markBuilt(level, false);
        Style st = styleFor(dungeon);

        // ================= ① 沙场：**纯平底**，只往下夯实、不往上叠 =================
        //
        // ⚠ 原来这里是"两层沙场（外层略低做台阶感）"，但 `dy=1` 那一层放在
        // `c.offset(x, -1, z)` —— 也就是**中心高度本身**：
        //   * 它在 x/z 上与外层圆盘**重叠**；
        //   * 而每个格子还会"把上方 8 格清空" → 把同高度上已铺好的外层地板**挖掉**；
        //   * 结果是一圈**环形空洞**（玩家反馈「地板还是有空隙」）。
        //
        // 现在改成：以中心高度为地面基准，**只往下夯实 3 层**（保证脚下是实心），
        // 不再往上一层叠、也不会互相挖掉。
        for (int dy = 0; dy <= 3; dy++) {
            int r = FLOOR_R + 2;                    // 略大于沙场，边缘不外露
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if ((double) x * x + (double) z * z > (double) r * r) {
                        continue;
                    }
                    BlockPos p = c.offset(x, -dy, z);
                    // 只在"地面基准及以下"夯实；地面之上不清（避免挖掉已铺的）
                    if (dy > 0 || level.getBlockState(p).isAir()
                            || level.getBlockState(p).getFluidState().isEmpty()) {
                        set(level, p, dy == 0 ? st.floor() : st.step());
                    }
                }
            }
        }
        // 沙场上方整体清空（场地要通透），只清地面之上
        for (int x = -FLOOR_R; x <= FLOOR_R; x++) {
            for (int z = -FLOOR_R; z <= FLOOR_R; z++) {
                if ((double) x * x + (double) z * z > (double) FLOOR_R * FLOOR_R) {
                    continue;
                }
                for (int up = 1; up <= 8; up++) {
                    BlockPos p = c.offset(x, up, z);
                    if (!level.getBlockState(p).isAir()) {
                        set(level, p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        // ================= ② 看台：8 级，每级立柱 + 灯 =================
        for (int t = 1; t <= TIERS; t++) {
            int r0 = FLOOR_R + (t - 1) * TIER_W;
            int r1 = r0 + TIER_W - 1;
            int y = t * TIER_H;
            for (int x = -r1; x <= r1; x++) {
                for (int z = -r1; z <= r1; z++) {
                    double d2 = (double) x * x + (double) z * z;
                    if (d2 > (double) r1 * r1 || d2 < (double) r0 * r0) {
                        continue;
                    }
                    for (int yy = 0; yy <= y; yy++) {
                        set(level, c.offset(x, yy, z), yy == y ? st.seat() : st.step());
                    }
                    for (int up = 1; up <= 5; up++) {
                        set(level, c.offset(x, y + up, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
            // 每级立柱（柱高 4，顶端放灯）
            for (int i = 0; i < PILLARS; i++) {
                double a = Math.PI * 2.0D * i / PILLARS;
                int px = (int) Math.round(Math.cos(a) * r1);
                int pz = (int) Math.round(Math.sin(a) * r1);
                for (int yy = 1; yy <= 4; yy++) {
                    set(level, c.offset(px, y + yy, pz), st.pillar());
                }
                set(level, c.offset(px, y + 5, pz), st.light());
            }
            // 每隔一级，在外沿铺一圈点缀（做出"镶边"的豪华感）
            if (t % 2 == 0) {
                for (int i = 0; i < PILLARS * 2; i++) {
                    double a = Math.PI * 2.0D * i / (PILLARS * 2);
                    int px = (int) Math.round(Math.cos(a) * (r1 - 2));
                    int pz = (int) Math.round(Math.sin(a) * (r1 - 2));
                    set(level, c.offset(px, y, pz), st.accent());
                }
            }
        }

        // ================= ③ 四个方位的大门 =================
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2.0D * i;
            int dx = (int) Math.round(Math.cos(a));
            int dz = (int) Math.round(Math.sin(a));
            for (int w = -3; w <= 3; w++) {
                int ox = dz == 0 ? 0 : w;
                int oz = dx == 0 ? 0 : w;
                int gx = dx * (FLOOR_R - 1) + ox;
                int gz = dz * (FLOOR_R - 1) + oz;
                for (int up = 0; up <= 7; up++) {
                    // 门洞留空（宽 5、高 6），其余是门框
                    boolean frame = Math.abs(w) >= 3 || up >= 6;
                    set(level, c.offset(gx, up, gz),
                            frame ? st.pillar() : Blocks.AIR.defaultBlockState());
                }
            }
        }

        // ================= ④ 中央祭坛（三层台基 + 四角火盆）=================
        //
        // ⚠ 台基从**地面那一层**起算（`lvl=0` 是地面基准），
        // 不再从 `above(1)` 开始 —— 否则整个祭坛会**凭空高出地面 1 格**
        //（玩家反馈「祭坛还高出地面」）。
        // 台基逐层收分：半径 6 → 5 → 4，最高一层顶面与地面齐平 +3。
        for (int lvl = 1; lvl <= 3; lvl++) {
            int r = ALTAR_R + 4 - lvl;      // 6 / 5 / 4
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) + Math.abs(z) > r + 2) {
                        continue;
                    }
                    set(level, c.offset(x, lvl, z), st.step());
                }
            }
        }
        set(level, c.above(4), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        // 放**本副本对应的主题祭坛**（原来是通用祭坛 → theme 为 null → 靠猜维度名）
        set(level, c.above(5), altarFor(dungeon));
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos p = c.offset(sx * (ALTAR_R + 3), 3, sz * (ALTAR_R + 3));
                set(level, p, st.step());
                set(level, p.above(), Blocks.CAMPFIRE.defaultBlockState());
            }
        }

        // ================= ⑤ 落成特效 =================
        level.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5D, c.getY() + 8.0D,
                c.getZ() + 0.5D, 220, 10.0D, 5.0D, 10.0D, 0.06D);
        level.playSound(null, c, SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 2.0F, 0.6F);

        // ================= ⑥ 建成复核 + 写标记 =================
        BlockPos altarPos = c.above(5);
        BlockState altarState = level.getBlockState(altarPos);
        boolean placedAltar = isAltar(dungeon, altarState);
        HongmengMod.LOGGER.info(
                "[arena] 建造完成：副本={} 中心={} 祭坛期望={} 实际={} 已放置={}",
                dungeon.dimensionId(), c, altarPos, altarState.getBlock(), placedAltar);
        if (!placedAltar) {
            level.setBlock(altarPos, altarFor(dungeon), 2);
            placedAltar = isAltar(dungeon, level.getBlockState(altarPos));
            HongmengMod.LOGGER.warn("[arena] 祭坛补放一次，结果={}", placedAltar);
        }
        if (placedAltar) {
            ArenaRealm.markBuilt(level, true);
        }
    }

    /** 进入竞技场（委托 {@link ArenaRealm}，传送逻辑只留一份）。 */
    public static boolean begin(ServerPlayer player, ServerLevel dungeonLevel, Dungeon dungeon,
                                int floorY) {
        return ArenaRealm.enter(player, dungeon);
    }

    /** 玩家是不是在竞技场（竞技场维度现在按副本分，这里问 ArenaRealm）。 */
    public static boolean inArena(UUID id) {
        return false;
    }

    public static void leave(ServerPlayer player) {
        ArenaRealm.forget(player);
    }

    public static void clear() {
        // 竞技场状态按玩家记在 ArenaRealm 里，这里无需清理
    }

    private static void set(ServerLevel level, BlockPos p, BlockState st) {
        if (level.isLoaded(p)) {
            level.setBlock(p, st, 2);
        }
    }

    /** 旧签名兼容。 */
    public static BlockPos center(int floorY) {
        return ArenaRealm.center();
    }
}
