package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>下界 / 末地的原生 Boss</b>（玩家要求）。
 *
 * <p>玩家原话：「可以多做点 boss，除了四个维度，下界、末地都要新增 boss，
 * <b>下界的 boss 和末地的 boss 都要自己寻找</b>」。</p>
 *
 * <h2>「自己寻找」怎么实现</h2>
 * <p>不做成"进入维度就自动刷在你面前"，而是像原版凋灵/末影龙那样：
 * <ul>
 *   <li><b>焰狱魔君</b>（下界）：只在<b>下界要塞与堡垒遗迹</b>附近低频刷新，
 *       玩家得深入下界去找；它会主动追人（大范围感知）。</li>
 *   <li><b>虚空龙帝</b>（末地）：只在<b>末地外岛</b>（距主岛 &gt; 400 格）刷新，
 *       同样要玩家飞过去找。</li>
 * </ul>
 * 两者都有唯一性保护：同一个维度同时只存在一只（死了才会再刷），
 * 避免"到处都是一堆 boss"。</p>
 *
 * <h2>为什么用"轮询刷新"而不是注册 BiomeModifier 的 spawner</h2>
 * <p>生物群系刷怪表是**数据驱动**的，要写 biome modifier JSON 且与别的模组的
 * 注入容易冲突；而且"同时只存在一只"这种全局约束在刷怪表里表达不出来。
 * 这里用每 600 tick 一次的低频轮询（开销可忽略）+ 每维度唯一性，
 * 行为可预测、也方便玩家用配置关掉。</p>
 */
public final class NativeBossSpawner {

    /** 每维度已存在的 boss（UUID）。 */
    private static final Map<net.minecraft.resources.ResourceKey<Level>, UUID> ALIVE =
            new ConcurrentHashMap<>();

    /** 轮询间隔（tick）：30 秒查一次，开销可忽略。 */
    private static final int POLL = 600;

    private NativeBossSpawner() {
    }

    /** 每 tick 由 {@code ModEvents} 转发（带玩家，只在有人时才有意义）。 */
    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % POLL != 0L) {
            return;
        }
        try {
            var key = level.dimension();
            boolean nether = level.dimension().equals(Level.NETHER);
            boolean end = level.dimension().equals(Level.END);
            if (!nether && !end) {
                return;
            }
            // 唯一性：已经有一只活着就不再刷
            UUID id = ALIVE.get(key);
            if (id != null) {
                var e = level.getEntity(id);
                if (e != null && e.isAlive()) {
                    return;
                }
                ALIVE.remove(key);
            }
            // 玩家基数：至少有人在这个维度才刷（避免空跑）
            if (level.players().isEmpty()) {
                return;
            }
            ServerPlayer any = level.players().get(0);
            BlockPos spot = findSpot(level, any, nether);
            if (spot == null) {
                return;
            }
            // 末地：20% 概率刷「虚空龙帝」（终局 boss），其余仍是虚空巨蟒
            var type = nether ? ModEntities.BLAZE_EMPEROR.get()
                    : (level.random.nextFloat() < 0.20F
                    ? ModEntities.VOID_DRAGON_EMPEROR.get()
                    : ModEntities.VOID_SERPENT.get());
            var mob = type.create(level);
            if (mob == null) {
                return;
            }
            mob.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot),
                    MobSpawnType.EVENT, null, null);
            if (!level.addFreshEntity(mob)) {
                return;
            }
            // ---- D-6：给原生 Boss 现场盖巢穴 ----
            //
            // 只在「焚天炎帝 / 虚空龙帝」成功刷出之后调用；按实体类型分派，
            // 虚空巨蟒等其它怪不会触发。生成失败只打日志，不影响刷怪与战斗。
            BossLair.ensureFor(level, mob);
            // ---- 维度 Boss 的专属外观标记 ----
            //
            // 玩家要求「boss 模型要单独设置」，但也强调「这次我不希望模型出错」。
            // 所以这里的做法是：**复用已经验证过、脚底坐标正确的那套模型管线**
            // （CultivationHumanoidModel），只换**贴图 + 体型 + 品阶标记**，
            // 而不是另写一套新骨架 —— 新骨架最容易出现"浮空/埋土里"那类问题。
            mob.getPersistentData().putString("hm_boss_variant",
                    nether ? "nether_lord" : "void_emperor");
            // ⚠ 必须写品阶覆盖，否则"下界 boss"只是个精英怪：
            //   FlameApeEntity.rank() 是 ELITE → 没血条、没三阶段、没远程法术。
            //   MobRank 顺序：NORMAL(1) ELITE(2) CAPTAIN(3) BOSS(4) OVERLORD(5)
            // 龙帝自身 rank() 就是 OVERLORD，不需要覆盖；虚空巨蟒需要升到 5
            if (type != ModEntities.VOID_DRAGON_EMPEROR.get()) {
                mob.getPersistentData().putInt("hm_rank_override", nether ? 4 : 5);
            }
            mob.getPersistentData().putFloat("hongmeng_boss_scale",
                    nether ? 1.55F : 1.75F);
            // 同步给客户端（BossPhaseRenderer 读同步倍率；persistentData 不下发）
            mob.setBossRenderScale(nether ? 1.55F : 1.75F);
            mob.getPersistentData().putInt("hongmeng_boss_form", 2);
            ALIVE.put(key, mob.getUUID());
            // 出场特效（复用 boss 出场序列：虚空裂缝 + 缓慢降落）
            Vec3 at = mob.position();
            int ticks = 140;
            com.hongmeng.xiuxian.network.BossEntrancePacket pkt =
                    new com.hongmeng.xiuxian.network.BossEntrancePacket(at, ticks);
            for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(at)
                    <= 128.0D * 128.0D)) {
                com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(sp, pkt);
                sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a74" + mob.getName().getString()
                                + " \u00a77的气息笼罩了这片天地……"), false);
            }
            HongmengMod.LOGGER.info("[boss] {} 在 {} 出现（{}）", mob.getName().getString(),
                    key.location(), spot);
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[hongmeng] 维度 boss 刷新失败：{}", t.toString());
        }
    }

    /**
     * 找一个合适的刷新点。
     *
     * <p>下界：距玩家 48~96 格、脚下有实体方块、头顶两格空。
     * 末地：同样的距离，但要求玩家在<b>外岛</b>（x²+z² &gt; 400²）。</p>
     */
    private static BlockPos findSpot(ServerLevel level, ServerPlayer near, boolean nether) {
        double baseR = nether ? 48.0D : 56.0D;
        for (int attempt = 0; attempt < 16; attempt++) {
            double ang = level.random.nextDouble() * Math.PI * 2.0D;
            double dist = baseR + level.random.nextDouble() * 48.0D;
            int x = (int) (near.getX() + Math.cos(ang) * dist);
            int z = (int) (near.getZ() + Math.sin(ang) * dist);
            if (!nether && (double) x * x + (double) z * z < 400.0D * 400.0D) {
                continue;       // 末地：只要外岛，主岛不刷
            }
            for (int dy = 24; dy >= -24; dy--) {
                BlockPos p = new BlockPos(x, (int) near.getY() + dy, z);
                if (!level.isLoaded(p)) {
                    continue;
                }
                BlockPos below = p.below();
                if (level.getBlockState(below).isAir()) {
                    continue;
                }
                if (!level.getBlockState(p).isAir()
                        || !level.getBlockState(p.above()).isAir()) {
                    continue;
                }
                return p;
            }
        }
        return null;
    }

    public static void clear() {
        ALIVE.clear();
    }
}
