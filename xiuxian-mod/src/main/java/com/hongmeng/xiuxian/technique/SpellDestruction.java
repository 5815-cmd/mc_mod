package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * <b>灭世法术的方块破坏</b>（玩家要求：光柱/陨石砸下可以破坏方块，尤其是灭世法术）。
 *
 * <h2>为什么单独一个类</h2>
 * <p>破坏方块是本项目里**风险最高**的一类副作用（可能毁玩家基地、可能卡爆服务器），
 * 所以全部集中在这一个类里，带三重护栏：</p>
 * <ol>
 *   <li>{@link #PROTECTED} —— 绝不破坏的方块（水/岩浆/光源/容器/基岩/传送门…）；</li>
 *   <li>{@link #MAX_BLOCKS} —— 单次施法的破坏上限；</li>
 *   <li>掉落只在近距离给，远处直接湮灭（避免一地掉落物）。</li>
 * </ol>
 *
 * <h2>只有灭世/超位才破坏</h2>
 * <p>普通法术走 {@code SpellDestruction} 之外的路径，一律不动方块。</p>
 */
public final class SpellDestruction {

    /** 单次施法最多破坏多少方块（防止一发黑洞把服务器卡死）。 */
    /**
     * 单次施法的破坏上限。
     *
     * <p>⚠ 原值 6000 太小：半径 110 的陨石坑**理论要挖 34 万格**，
     * 结果只挖了 1.7%，玩家看到的就是"没破坏地形"（日志实测：
     * <code>crater 破坏 6000 格</code> 卡在上限上）。</p>
     *
     * <p>提到 40 万，仍保留硬上限防卡死。</p>
     */
    public static final int MAX_BLOCKS = 400_000;

    /**
     * <b>绝不破坏</b>的方块。
     *
     * <p>水与岩浆不破坏是项目既有铁律（水领域从不碰方块/火把/作物）；
     * 光源、容器、传送门、基岩类是常识性保护。</p>
     */
    private static final Set<Block> PROTECTED = Set.of(
            Blocks.WATER, Blocks.LAVA, Blocks.BEDROCK, Blocks.BARRIER,
            Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK,
            Blocks.STRUCTURE_VOID, Blocks.JIGSAW, Blocks.LIGHT,
            Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME, Blocks.END_GATEWAY,
            Blocks.NETHER_PORTAL, Blocks.SPAWNER, Blocks.CHEST, Blocks.TRAPPED_CHEST,
            Blocks.BARREL, Blocks.SHULKER_BOX, Blocks.ENDER_CHEST,
            Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER, Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.BEACON,
            Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
            Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.GLOWSTONE, Blocks.SEA_LANTERN, Blocks.SHROOMLIGHT,
            Blocks.REDSTONE_LAMP, Blocks.OCHRE_FROGLIGHT, Blocks.VERDANT_FROGLIGHT,
            Blocks.PEARLESCENT_FROGLIGHT, Blocks.JACK_O_LANTERN
    );

    /** 破坏时按命名空间额外保护的模组方块（灵植/灵矿是玩家产出）。 */
    private static final String OWN_NAMESPACE = "hongmeng";

    private SpellDestruction() {
    }

    /**
     * 带坐标的版本：除了方块本身的规则，还要查「是不是 Boss 大本营」。
     *
     * <p>玩家要求：「这种建筑物如果是 boss 大本营，就设置一个
     * <b>未击杀最终 boss 前无法破坏</b>」。</p>
     */
    public static boolean canBreakAt(net.minecraft.server.level.ServerLevel level,
                                     net.minecraft.core.BlockPos pos) {
        if (level == null || pos == null) {
            return true;
        }
        try {
            int fy = com.hongmeng.xiuxian.dungeon.DungeonArena
                    .floorYForProtection(level);
            return com.hongmeng.xiuxian.dungeon.BossStronghold
                    .canBreak(level, fy, pos);
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * <b>真正把一格方块抹掉</b>。
     *
     * <p>⚠ 为什么不能用统一的 {@code destroyBlock}：玩家要求
     * 「岩浆和水这种方块都是可以破坏的」，而原版
     * {@code Level.destroyBlock(pos, drop)} 对<b>流体方块</b>（水/岩浆）不生效 ——
     * 它走的是"可破坏方块"的那条路径，流体到不了 {@code removeBlock}。
     * 只改判据不改动作的话，打上去是"通过了检查但方块还在"。</p>
     *
     * <p>所以这里分流：</p>
     * <ul>
     *   <li><b>含流体</b> → 直接置空（并顺手把这一格重新标记，让流体不再回流）；</li>
     *   <li><b>普通方块</b> → 仍走 {@code destroyBlock(pos, false)}，
     *       保持与原版一致的事件与副作用（只是不掉落物品）。</li>
     * </ul>
     */
    private static void erase(ServerLevel level, BlockPos pos, BlockState st) {
        if (level == null || pos == null || st == null) {
            return;
        }
        // 含流体的一律强制置空
        if (!st.getFluidState().isEmpty() || st.getBlock() instanceof
                net.minecraft.world.level.block.LiquidBlock) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR
                    .defaultBlockState(), 3);
            return;
        }
        // 玩家要求：破坏不掉落物品（避免上千实体）
        //
        // PERF(Lead)：大范围破坏法术**不能**逐格走 destroyBlock —— 它每格都会给附近客户端
        //   发"方块破碎"事件包（碎屑粒子 + 音效），一次 1200 格就是 1200+ 包/tick，
        //   服务端序列化 + 客户端逐格爆粒子，正是"破坏类法术一放就卡/客户端掉帧"的主因之一。
        //   removeBlock 仍是完整移除（含流体处理 / 方块实体 / 邻居与光照更新），
        //   只是不发每格的碎屑与音效；法术自己的特效已经覆盖观感。
        level.removeBlock(pos, false);
    }

    /** 这个方块能不能被法术破坏。 */
    public static boolean canBreak(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (PROTECTED.contains(state.getBlock())) {
            return false;
        }
        // ⚠ 玩家要求「岩浆和水这种方块都是可以破坏的」——
        // 原来这里有一条"液体一律不动"，现在去掉。
        // （注意：只放行**作为方块存在的**液体，即源头方块；
        //  `canBreak` 拿到的是 BlockState，岩浆块/水块都会走到这里。）
        // 硬度过大（基岩 -1）不动
        if (state.getDestroySpeed(null, null) < 0.0F) {
            return false;
        }
        // 本项目自己的方块（灵植、灵矿、法器等）不动 —— 那是玩家的产出
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id == null || !OWN_NAMESPACE.equals(id.getNamespace());
    }

    /**
     * 球形/半球破坏（**太虚黑洞**的"吞噬"、**天陨星落**的陨石坑）。
     *
     * @param radius    水平半径
     * @param depth     向下深度
     * @param crater    是否做成"碗形"（边缘浅、中心深）—— 陨石坑用 true，黑洞用 false
     * @param dropRange 这个距离内破坏的方块会掉落物品，之外直接湮灭
     * @return 实际破坏的方块数
     */
    public static int crater(ServerLevel level, Vec3 center, double radius, int depth,
                             boolean crater, double dropRange) {
        if (level == null || center == null) {
            return 0;
        }
        // &&& 球形破坏（玩家要求）
        //
        // 玩家原话：「陨星这种破坏类法术的破坏不能仅仅只是对地下的，
        // 他们的破坏范围就比如陨星，星星类似圆形，
        // 他的破坏范围应该是一个圆形，不应该是半圆」。
        //
        // 所以现在挖的是**完整球体**：以落点为中心，向上向下都挖。
        // `crater=false`（黑洞那种）仍然用"直筒"，但也是**上下对称**的。
        // ⚠ 破坏半径必须封顶。
        //
        //   调用方（天陨星落）传的是 `METEOR_RADIUS * 0.85` = 110 格 ——
        //   那是一个 **440 万格**的球，既会铲掉半座山，也必然把收集阶段
        //   的 MAX_BLOCKS 名额耗尽。这里统一封到 MAX_CRATER_RADIUS。
        double capped = Math.min(radius, MAX_CRATER_RADIUS);
        double r3 = crater ? Math.max(capped * 0.92D, depth + 1.0D) : capped;
        int ri = (int) Math.ceil(Math.min(r3, MAX_CRATER_RADIUS + 8.0D));
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);

        // ---- 先收集要挖的方块，再交给分段队列（避免一帧挖几十万格卡死）----
        java.util.List<BlockPos> todo = new java.util.ArrayList<>();
        // 从中心向外：0, +1, -1, +2, -2 … 这样最近的方块先入队，
        // 即使被 MAX_BLOCKS 截断，挖掉的也是以落点为圆心的一团，
        // 而不是"落点上方一大片空气"（原来那样什么也挖不到）。
        for (int step = 0; step <= ri * 2 + 1; step++) {
            int dy = (step % 2 == 0) ? (step / 2) : -((step + 1) / 2);
            if (dy > ri || dy < -ri) {
                continue;
            }
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    // 从中心向外遍历：dy 先 0、再 ±1、±2…，保证近处先入队
                    // （原来从球顶往下，前 40 万个名额全被空气占满，
                    //   地面根本进不了队列 —— 这就是"不破坏地形"的根因）
                    double distSq = (double) dx * dx + (double) dy * dy + (double) dz * dz;
                    if (distSq > r3 * r3) {
                        continue;
                    }
                    if (!crater) {
                        // 直筒：只要水平在半径内
                        if ((double) dx * dx + (double) dz * dz > radius * radius) {
                            continue;
                        }
                    }
                    BlockPos pos = new BlockPos(cx + dx, cy + dy, cz + dz);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    // ⚠ 跳过空气/不可破坏的，别让它们吃掉 MAX_BLOCKS 名额
                    if (!canBreak(level.getBlockState(pos))) {
                        continue;
                    }
                    todo.add(pos);
                    if (todo.size() >= MAX_BLOCKS) {
                        break;
                    }
                }
                if (todo.size() >= MAX_BLOCKS) {
                    break;
                }
            }
            if (todo.size() >= MAX_BLOCKS) {
                break;
            }
        }
        // 入队：法术瞬间"生效"，地形在随后若干 tick 内塌陷完（消除卡顿）
        return enqueue(level, todo);
    }

    // ------------------------------------------------------------------
    // 分段破坏队列（消除"放法术卡一下"）
    // ------------------------------------------------------------------

    /** 每 tick 最多实际破坏多少格（超出部分排队）。 */
    private static final int PER_TICK = 1200;

    /**
     * <b>破坏半径上限</b>（格）。
     *
     * <p>调用方传进来的可能是"伤害半径"（天陨星落是 130 格），
     * 直接拿来破坏会得到一个 440 万格的球：既铲掉半座山，也必然把
     * {@link #MAX_BLOCKS} 名额耗尽（空气还先占满），最终表现成
     * <b>"法术不破坏地形"</b>。</p>
     *
     * <p>28 格半径 = 直径 56 格的天坑，视觉上已经足够震撼。</p>
     */
    public static final double MAX_CRATER_RADIUS = 28.0D;

    /** 待处理队列：按维度分开（维度不同不能混）。 */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<
            net.minecraft.world.level.Level>, java.util.ArrayDeque<BlockPos>> QUEUE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static int enqueue(ServerLevel level, java.util.List<BlockPos> todo) {
        if (todo.isEmpty()) {
            return 0;
        }
        java.util.ArrayDeque<BlockPos> q = QUEUE.computeIfAbsent(
                level.dimension(), k -> new java.util.ArrayDeque<>());
        // PERF(Lead)：队列总量封顶。连续施法时旧队列还没消化完、新施法又塞进来，
        // 队列会无限增长（每格都要保存 BlockPos + 逐个处理）。超过 MAX_BLOCKS 就不再追加，
        // 避免"越放法术越卡、卡了还在排队"的雪球；已有队列仍会按预算慢慢挖完。
        int room = MAX_BLOCKS - q.size();
        if (room <= 0) {
            return 0;
        }
        if (todo.size() > room) {
            todo = todo.subList(0, room);
        }
        q.addAll(todo);
        // 立刻消化一部分，保证"放下去就看到破坏"
        return drain(level);
    }

    /** 由 {@code ModEvents} 每 tick 调一次，把排队的地形挖完。 */
    public static void tickQueue(ServerLevel level) {
        if (level == null) {
            return;
        }
        java.util.ArrayDeque<BlockPos> q = QUEUE.get(level.dimension());
        if (q == null || q.isEmpty()) {
            return;
        }
        drain(level);
    }

    /** 消化一部分队列，返回本次破坏的格数。 */
    private static int drain(ServerLevel level) {
        java.util.ArrayDeque<BlockPos> q = QUEUE.get(level.dimension());
        if (q == null) {
            return 0;
        }
        int done = 0;
        // PERF(Lead)：破坏队列设"每 tick 时间预算 + 掉帧自适应限流"。
        //
        // 实机日志（2026-11-07 20:25~20:29 虚空秘境）：玩家一边打怪潮一边连续放
        // 破坏法术，服务端从 2s 一路堆到 26s behind。破坏队列每 tick 固定挖 1200 格，
        // 在服务器已经吃不消时等于继续加码。现在：
        //   · 平均 tick > 80ms → 每 tick 只挖 120 格（把大卡顿摊成小开销）；
        //   · 平均 tick > 40ms → 每 tick 360 格；
        //   · 任何时候单 tick 最多花 5ms 在破坏上，剩下的下一 tick 继续（不会丢，只是慢一点）。
        int perTick = PER_TICK;
        try {
            net.minecraft.server.MinecraftServer server = level.getServer();
            if (server != null) {
                float avg = server.getAverageTickTime();
                if (avg > 80.0F) {
                    perTick = 120;
                } else if (avg > 40.0F) {
                    perTick = 360;
                }
            }
        } catch (Throwable ignored) {
            // 拿不到平均 tick 就用默认值（不影响正确性）
        }
        long t0 = System.nanoTime();
        while (done < perTick && !q.isEmpty()) {
            if ((done & 31) == 0 && System.nanoTime() - t0 > 5_000_000L) {
                break;                  // 本 tick 已花 5ms：剩下的下一 tick 继续
            }
            BlockPos pos = q.poll();
            if (pos == null || !level.isLoaded(pos)) {
                continue;
            }
            BlockState st = level.getBlockState(pos);
            if (!canBreak(st) || !canBreakAt(level, pos)) {
                continue;
            }
            // 玩家要求：破坏不掉落物品（避免上千实体）
            erase(level, pos, st);
            done++;
        }
        return done;
    }

    /** 还有多少格排队等着挖（诊断）。 */
    public static int pending(ServerLevel level) {
        java.util.ArrayDeque<BlockPos> q = QUEUE.get(level.dimension());
        return q == null ? 0 : q.size();
    }

    /**
     * 换存档 / 停服清理。
     *
     * <p>PERF(B)/正确性：{@link #QUEUE} 里最多会排 {@link #MAX_BLOCKS} 个待挖坐标；
     * 不清的话，<b>同一个 JVM 里换存档</b>（单人退出重进另一个世界）时会残留到新世界 ——
     * 那些**旧世界的坐标会继续在新世界被挖掉**，同时白占十几 MB 内存。</p>
     */
    public static void clear() {
        QUEUE.clear();
    }

    /**
     * 隧道破坏（**轨道炮击**的贯穿、**斩星**的深渊沟壑）。
     *
     * @param halfWidth 截面半宽（斩星用大值 + depth 做大沟壑）
     */
    public static int tunnel(ServerLevel level, Vec3 from, Vec3 to, double halfWidth,
                             int depth, double dropRange) {
        // 兼容旧签名：pierce 取 0（不额外贯穿），guard 取 3 格
        return tunnel(level, from, to, halfWidth, depth, dropRange, 0.0D, 3.0D);
    }

    /**
     * <b>掘进一条通道</b>（轨道炮击 / 斩星）。
     *
     * <h2>形状（玩家要求："从我身前射出的一道破坏光线"）</h2>
     * <ol>
     *   <li><b>保护圈</b>：起点周围 {@code guard} 格内**一律不动** ——
     *       绝不掏施法者脚下（这是上一版最大的问题）；</li>
     *   <li><b>只挖到命中点</b>：{@code to} 一般是射线命中的方块，
     *       不再像以前那样沿无限长的直线一路挖到射程尽头；</li>
     *   <li><b>贯穿</b>：命中点之后再向前挖 {@code pierce} 格，做出"打穿"的观感；</li>
     *   <li><b>深度渐变</b>：起点处深度很小（`depth*0.25`），中段最深（`depth`），
     *       末端收口到 `depth*0.4` —— 形成"前细后粗再收"的通道，
     *       而不是一条等径的深沟。</li>
     * </ol>
     *
     * @param halfWidth 通道半宽
     * @param depth     最大向下深度
     * @param dropRange 保留参数（当前掉落已统一关闭）
     * @param pierce    命中点之后再向前挖多少格
     * @param guard     起点周围的保护半径
     */
    public static int tunnel(ServerLevel level, Vec3 from, Vec3 to, double halfWidth,
                             int depth, double dropRange, double pierce, double guard) {
        if (level == null || from == null || to == null) {
            return 0;
        }
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.5D) {
            return 0;
        }
        Vec3 step = dir.normalize();
        // 总长度 = 射线段 + 贯穿段
        double total = len + Math.max(0.0D, pierce);
        int broken = 0;
        for (double sBy = 0.0D; sBy <= total; sBy += 1.0D) {
            if (broken >= MAX_BLOCKS) {
                break;
            }
            // 进度 0→1（射线段占 len/total）
            double prog = sBy / Math.max(1.0D, total);
            // 起点附近先跳过（保护圈）
            double horizFromStart = sBy;
            if (horizFromStart < guard) {
                continue;
            }
            // 深度渐变：0.25 → 1.0（中段）→ 0.4（末端）
            double taper;
            if (prog < 0.35D) {
                taper = 0.25D + (prog / 0.35D) * 0.75D;
            } else if (prog < 0.75D) {
                taper = 1.0D;
            } else {
                taper = 1.0D - ((prog - 0.75D) / 0.25D) * 0.6D;
            }
            int d = Math.max(1, (int) Math.round(depth * Math.max(0.15D, taper)));

            Vec3 p = from.add(step.scale(sBy));
            int cx = (int) Math.floor(p.x);
            int cy = (int) Math.floor(p.y);
            int cz = (int) Math.floor(p.z);
            int hw = (int) Math.ceil(halfWidth);
            for (int dx = -hw; dx <= hw; dx++) {
                for (int dz = -hw; dz <= hw; dz++) {
                    if (Math.sqrt(dx * dx + dz * dz) > halfWidth) {
                        continue;
                    }
                    for (int dy = 0; dy >= -d; dy--) {
                        if (broken >= MAX_BLOCKS) {
                            break;
                        }
                        // 保护圈（三维）：起点球体范围内一律不动
                        double gx = cx + dx - from.x;
                        double gy = cy + dy - from.y;
                        double gz = cz + dz - from.z;
                        if (gx * gx + gy * gy + gz * gz < guard * guard) {
                            continue;
                        }
                        BlockPos pos = new BlockPos(cx + dx, cy + dy, cz + dz);
                        if (!level.isLoaded(pos)) {
                            continue;
                        }
                        BlockState st = level.getBlockState(pos);
                        if (!canBreak(st) || !canBreakAt(level, pos)) {
                            continue;
                        }
                        // 玩家要求：**法术破坏不掉落掉落物**
                        erase(level, pos, st);
                        broken++;
                    }
                }
            }
        }
        return broken;
    }

    /**
     * 焦土（**焚天煮海**）：只烧掉**表层植被/雪/木/叶**，**不动石头**。
     */
    public static int scorch(ServerLevel level, Vec3 center, double radius, int depth) {
        if (level == null || center == null) {
            return 0;
        }
        int broken = 0;
        int r = (int) Math.ceil(radius);
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        try {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.sqrt(dx * dx + dz * dz) > radius) {
                        continue;
                    }
                    for (int dy = 3; dy >= -depth; dy--) {
                        if (broken >= MAX_BLOCKS) {
                            return broken;
                        }
                        BlockPos pos = new BlockPos(cx + dx, cy + dy, cz + dz);
                        if (!level.isLoaded(pos)) {
                            continue;
                        }
                        BlockState st = level.getBlockState(pos);
                        if (!canBreak(st) || !canBreakAt(level, pos)) {
                            continue;
                        }
                        // 只烧"软"的东西：坚决不动石头/矿石/砖
                        if (!isBurnable(st)) {
                            continue;
                        }
                        erase(level, pos, st);
                        broken++;
                    }
                }
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[hongmeng] 焦土异常：{}", t.toString());
        }
        return broken;
    }

    /** 是不是"能被烧掉"的软方块（植被/雪/木/叶/土）。 */
    private static boolean isBurnable(BlockState st) {
        Block b = st.getBlock();
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(b);
        if (id == null) {
            return false;
        }
        String p = id.getPath();
        return p.contains("leaves") || p.contains("log") || p.contains("wood")
                || p.contains("grass") || p.contains("flower") || p.contains("sapling")
                || p.contains("fern") || p.contains("snow") || p.contains("ice")
                || p.contains("dirt") || p.contains("sand") || p.contains("moss")
                || p.contains("vine") || p.contains("bush") || p.contains("crop")
                || p.contains("wheat") || p.contains("carrot") || p.contains("potato")
                || p.contains("mushroom") || p.contains("cactus") || p.contains("bamboo");
    }
}
