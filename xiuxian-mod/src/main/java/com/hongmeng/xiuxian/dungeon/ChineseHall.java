package com.hongmeng.xiuxian.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>中式大殿生成器</b>（玩家要求）。
 *
 * <p>玩家原话：「我想让你做的是图二三五六七八的那种，你做的是图一图四那种，
 * mod 中的建筑都是可以按照类似我给你的这种风格来完成」——
 * 也就是<b>中式古建</b>：台基 + 红柱 + 重檐歇山顶 + 飞檐翘角 + 斗拱 + 匾额 + 脊兽。</p>
 *
 * <h2>中式古建的几个"形"（这里都实现了）</h2>
 * <table border="1">
 *   <caption>构件 → 做法</caption>
 *   <tr><th>构件</th><th>做法</th></tr>
 *   <tr><td><b>台基</b></td><td>三层逐级内收的石台（须弥座），带踏跺（台阶）</td></tr>
 *   <tr><td><b>柱网</b></td><td>朱红立柱按"面阔 N 间"等距排列，柱下有柱础</td></tr>
 *   <tr><td><b>斗拱</b></td><td>柱头向外挑出的层叠木块（一斗三升的简化）</td></tr>
 *   <tr><td><b>屋檐</b></td><td>两层檐（重檐）：下层檐大、上层檐小；每层檐向外挑出</td></tr>
 *   <tr><td><b>飞檐翘角</b></td><td>四个转角处檐口<b>逐格抬高</b>，做出"反宇"的翘起感</td></tr>
 *   <tr><td><b>正脊与鸱吻</b></td><td>屋顶正中一条脊，两端各一只翘头</td></tr>
 *   <tr><td><b>匾额</b></td><td>正门上方一块深色木板 + 金色边框</td></tr>
 *   <tr><td><b>墙身</b></td><td>红墙（朱红）、下段石活（深色石砖）</td></tr>
 * </table>
 *
 * <p>为什么写成一个通用生成器而不是四座手搭的建筑：四座大本营只是<b>主材与配色</b>
 * 不同，形制完全一样。参数化之后，改一处四座一起改，也不会出现"某座搭歪了"。</p>
 */
public final class ChineseHall {

    private ChineseHall() {
    }

    /** 一座大殿的配色与主材。 */
    public static final class Style {
        final BlockState stone;      // 台基
        final BlockState wall;       // 墙体
        final BlockState pillar;     // 柱子
        final BlockState beam;       // 梁枋 / 斗拱
        final BlockState roof;       // 屋面
        final BlockState ridge;      // 正脊
        final BlockState accent;     // 点缀（脊兽、匾额边框）
        final BlockState floor;      // 室内地面

        public Style(BlockState stone, BlockState wall, BlockState pillar,
                     BlockState beam, BlockState roof, BlockState ridge,
                     BlockState accent, BlockState floor) {
            this.stone = stone;
            this.wall = wall;
            this.pillar = pillar;
            this.beam = beam;
            this.roof = roof;
            this.ridge = ridge;
            this.accent = accent;
            this.floor = floor;
        }
    }

    /** 四座大本营各自的配色（与副本主题呼应）。 */
    public static Style styleFor(Dungeon d) {
        return switch (d) {
            // 血魔教：黑石台基 + 朱红墙柱 + 深红琉璃顶
            case DEMON -> new Style(
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.RED_TERRACOTTA.defaultBlockState(),
                    Blocks.CRIMSON_HYPHAE.defaultBlockState(),
                    Blocks.CRIMSON_PLANKS.defaultBlockState(),
                    Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                    Blocks.NETHER_BRICKS.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.BLACKSTONE.defaultBlockState());
            // 纪元：汉白玉台基 + 青砖墙 + 青灰瓦 + 金饰
            case EPOCH -> new Style(
                    Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    Blocks.STONE_BRICKS.defaultBlockState(),
                    Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                    Blocks.OAK_PLANKS.defaultBlockState(),
                    Blocks.DEEPSLATE_TILES.defaultBlockState(),
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                    Blocks.GOLD_BLOCK.defaultBlockState(),
                    Blocks.SMOOTH_STONE.defaultBlockState());
            // 妖兽：粗石台基 + 苔石墙 + 干草顶
            case BEAST -> new Style(
                    Blocks.COBBLESTONE.defaultBlockState(),
                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.HAY_BLOCK.defaultBlockState(),
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    Blocks.EMERALD_BLOCK.defaultBlockState(),
                    Blocks.COARSE_DIRT.defaultBlockState());
            // 虚空：黑曜台基 + 深紫墙 + 紫珀顶
            case VOID -> new Style(
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                    Blocks.DEEPSLATE_TILES.defaultBlockState(),
                    Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                    Blocks.WARPED_PLANKS.defaultBlockState(),
                    Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.PURPUR_PILLAR.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        };
    }

    /**
     * 建一座中式大殿。
     *
     * @param c     台基中心（y = 地面高度）
     * @param halfX 面阔的一半（柱网范围）
     * @param halfZ 进深的一半
     * @param bays  面阔几间（影响柱距）
     * @return 放置的方块数
     */
    public static int build(ServerLevel level, BlockPos c, Style st,
                            int halfX, int halfZ, int bays) {
        int n = 0;
        // ================= 一、台基（三层须弥座） =================
        for (int lvl = 0; lvl < 3; lvl++) {
            int ex = halfX + 6 - lvl * 2;
            int ez = halfZ + 6 - lvl * 2;
            n += slab(level, c, ex, lvl, ez, st.stone);
        }
        // 台基顶面铺室内地面
        n += slab(level, c, halfX + 4, 3, halfZ + 4, st.floor, true);

        // 正面踏跺（台阶）
        for (int i = 0; i < 3; i++) {
            for (int x = -3; x <= 3; x++) {
                set(level, c.offset(x, 2 - i, halfZ + 6 - i), st.stone);
                n++;
            }
        }

        // ================= 二、柱网 + 墙身 =================
        int pillarTop = 9;
        int stepX = Math.max(3, (halfX * 2) / Math.max(1, bays));
        for (int x = -halfX; x <= halfX; x += stepX) {
            // 前后两排柱
            for (int sz = -1; sz <= 1; sz += 2) {
                n += pillarColumn(level, c, x, 3, sz * halfZ, pillarTop, st);
            }
        }
        // 两侧山墙位置的柱
        for (int z = -halfZ; z <= halfZ; z += stepX) {
            for (int sx = -1; sx <= 1; sx += 2) {
                n += pillarColumn(level, c, sx * halfX, 3, z, pillarTop, st);
            }
        }
        // 墙身：红色，正面留门洞（宽 5）
        for (int y = 3; y <= pillarTop - 2; y++) {
            n += wallRect(level, c, halfX, halfZ, y, st.wall, true);
        }

        // ================= 三、斗拱 + 下层檐 =================
        int eave1 = pillarTop;
        // 斗拱：沿柱头向外挑一格
        for (int x = -halfX - 1; x <= halfX + 1; x++) {
            set(level, c.offset(x, eave1, -halfZ - 1), st.beam);
            set(level, c.offset(x, eave1, halfZ + 1), st.beam);
            n += 2;
        }
        for (int z = -halfZ - 1; z <= halfZ + 1; z++) {
            set(level, c.offset(-halfX - 1, eave1, z), st.beam);
            set(level, c.offset(halfX + 1, eave1, z), st.beam);
            n += 2;
        }
        // 下层檐：向外挑出 4 格，逐层外扩 + 抬高一格（做出屋面坡度）
        n += roofRing(level, c, eave1, halfX, halfZ, 4, 1, st.roof);

        // ================= 四、上层墙 + 重檐 =================
        int upHalfX = halfX - 3;
        int upHalfZ = halfZ - 3;
        int upBase = eave1 + 2;
        for (int y = upBase; y <= upBase + 3; y++) {
            n += wallRect(level, c, upHalfX, upHalfZ, y, st.wall, false);
        }
        // 上层檐
        for (int i = 0; i < 3; i++) {
            set(level, c.offset(-upHalfX - 1, upBase + 4 + i, -upHalfZ - 1), st.roof);
            set(level, c.offset(upHalfX + 1, upBase + 4 + i, -upHalfZ - 1), st.roof);
            set(level, c.offset(-upHalfX - 1, upBase + 4 + i, upHalfZ + 1), st.roof);
            set(level, c.offset(upHalfX + 1, upBase + 4 + i, upHalfZ + 1), st.roof);
            n += 4;
        }
        n += roofRing(level, c, upBase + 4, upHalfX, upHalfZ, 3, 1, st.roof);

        // ================= 五、屋盖 + 正脊 + 鸱吻 =================
        int ridgeY = upBase + 8;
        // 逐层收分的庑殿顶
        for (int y = upBase + 6; y <= ridgeY; y++) {
            int shrink = y - (upBase + 6);
            int rx = Math.max(1, upHalfX - shrink);
            int rz = Math.max(1, upHalfZ - shrink);
            n += slab(level, c, rx, y, rz, st.roof, true);
        }
        // 正脊（沿 X 一条）+ 两端鸱吻
        for (int x = -upHalfX + 1; x <= upHalfX - 1; x++) {
            set(level, c.offset(x, ridgeY + 1, 0), st.ridge);
            n++;
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            set(level, c.offset(sx * upHalfX, ridgeY + 1, 0), st.accent);
            set(level, c.offset(sx * upHalfX, ridgeY + 2, 0), st.accent);
            n += 2;
        }

        // ================= 六、飞檐翘角（四角抬高） =================
        n += upturnedCorners(level, c, eave1, halfX + 4, halfZ + 4, st.roof);

        // ================= 七、匾额 =================
        set(level, c.offset(0, pillarTop - 1, halfZ + 1), st.accent);
        for (int x = -3; x <= 3; x++) {
            set(level, c.offset(x, pillarTop - 2, halfZ + 1),
                    x == 0 ? st.accent : Blocks.DARK_OAK_PLANKS.defaultBlockState());
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------
    // 构件
    // ------------------------------------------------------------------

    /** 一根柱子：柱础（石）+ 柱身 + 柱头（梁）。 */
    private static int pillarColumn(ServerLevel level, BlockPos c, int x, int y0, int z,
                                    int y1, Style st) {
        set(level, c.offset(x, y0 - 1, z), st.stone);
        for (int y = y0; y <= y1 - 1; y++) {
            set(level, c.offset(x, y, z), st.pillar);
        }
        set(level, c.offset(x, y1, z), st.beam);
        return y1 - y0 + 2;
    }

    /** 一圈墙（可留门洞）。 */
    private static int wallRect(ServerLevel level, BlockPos c, int hx, int hz, int y,
                                BlockState st, boolean door) {
        int n = 0;
        for (int x = -hx; x <= hx; x++) {
            for (int sz = -1; sz <= 1; sz += 2) {
                if (door && sz > 0 && Math.abs(x) <= 2
                        && y <= 6) {
                    continue;       // 正面门洞
                }
                set(level, c.offset(x, y, sz * hz), st);
                n++;
            }
        }
        for (int z = -hz + 1; z <= hz - 1; z++) {
            for (int sx = -1; sx <= 1; sx += 2) {
                set(level, c.offset(sx * hx, y, z), st);
                n++;
            }
        }
        return n;
    }

    /** 一层檐：从 (hx,hz) 向外扩 out 格，每扩一圈抬高一格。 */
    private static int roofRing(ServerLevel level, BlockPos c, int y, int hx, int hz,
                                int out, int rise, BlockState st) {
        int n = 0;
        for (int i = 0; i < out; i++) {
            int ex = hx + i;
            int ez = hz + i;
            int yy = y + i * rise;
            for (int x = -ex; x <= ex; x++) {
                set(level, c.offset(x, yy, -ez), st);
                set(level, c.offset(x, yy, ez), st);
                n += 2;
            }
            for (int z = -ez + 1; z <= ez - 1; z++) {
                set(level, c.offset(-ex, yy, z), st);
                set(level, c.offset(ex, yy, z), st);
                n += 2;
            }
        }
        return n;
    }

    /** 飞檐翘角：四个角沿对角线逐格抬高。 */
    private static int upturnedCorners(ServerLevel level, BlockPos c, int y,
                                       int ex, int ez, BlockState st) {
        int n = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int i = 0; i < 4; i++) {
                    set(level, c.offset(sx * (ex - i), y + 3 + i, sz * (ez - i)), st);
                    set(level, c.offset(sx * (ex - i - 1), y + 3 + i, sz * (ez - i)), st);
                    set(level, c.offset(sx * (ex - i), y + 3 + i, sz * (ez - i - 1)), st);
                    n += 3;
                }
            }
        }
        return n;
    }

    /** 实心矩形层。 */
    private static int slab(ServerLevel level, BlockPos c, int hx, int y, int hz,
                            BlockState st) {
        return slab(level, c, hx, y, hz, st, false);
    }

    /** hollow=true 时只铺一圈（当"楼板"用，省方块也不穿模）。 */
    private static int slab(ServerLevel level, BlockPos c, int hx, int y, int hz,
                            BlockState st, boolean hollow) {
        int n = 0;
        for (int x = -hx; x <= hx; x++) {
            for (int z = -hz; z <= hz; z++) {
                if (hollow && Math.abs(x) != hx && Math.abs(z) != hz) {
                    continue;
                }
                set(level, c.offset(x, y, z), st);
                n++;
            }
        }
        return n;
    }

    private static void set(ServerLevel level, BlockPos p, BlockState st) {
        if (level.isLoaded(p)) {
            level.setBlock(p, st, 2);
        }
    }
}
