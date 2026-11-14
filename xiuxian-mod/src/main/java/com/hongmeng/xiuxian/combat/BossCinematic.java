package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.ModEntities;
import com.hongmeng.xiuxian.network.CinematicPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>BOSS 出场演出（服务端）</b> —— 影视级运镜。
 *
 * <h2>参考</h2>
 * <p>玩家要求「BOSS 出场也要做出场动画，影视级别的运镜和特效」，
 * 参考灾变那套过场。查到的关键做法（{@code Cinematic Cataclysm}）：</p>
 * <ul>
 *   <li>演出<b>由服务端驱动、按 tick 同步</b>（否则会出现"服务端与客户端严重不同步"）；</li>
 *   <li>演出期间玩家<b>无敌且无法被选中</b>；</li>
 *   <li>只对<b>生存模式</b>玩家做传送/强制演出。</li>
 * </ul>
 * <p>镜头插值用关键帧 + 缓入缓出（{@code ImmersiveCinematics} 的做法）。</p>
 *
 * <h2>这段运镜怎么设计</h2>
 * <pre>
 *   tick 0    远处低角度仰拍（boss 在画面里很小，先给"体量"）
 *   tick 20   拉近 + 抬高（开始看清轮廓）
 *   tick 40   环绕到侧面（交代三维形体，这是"立体感"的关键）
 *   tick 60   环绕到背面 + 俯视（看到背部结构 —— 每只 boss 的背部都不一样）
 *   tick 80   回到正面 + 更近（进入对峙）
 *   tick 100  贴近仰拍（最有压迫感的角度）
 *   tick 130  拉远收束（把镜头还给玩家）
 * </pre>
 *
 * <p>总时长 140 tick（7 秒）—— 刚好与 {@link BossDescent} 的降落时长一致，
 * 于是"降落动画"与"镜头演出"是同一段，不会出现落地了镜头还在飞。</p>
 */
public final class BossCinematic {

    /** 演出总时长（tick）。与 {@link BossDescent#TICKS} 对齐。 */
    public static final int TICKS = 140;

    /** 已经在演出中的玩家（防止重复触发）。 */
    private static final Map<UUID, Long> PLAYING = new HashMap<>();
    /** 已经演过的 boss（每只只演一次）。 */
    private static final java.util.Set<UUID> DONE = new java.util.HashSet<>();

    private BossCinematic() {
    }

    /**
     * 七首领白名单：四个秘境 Boss + 下界/末地 Boss + 恶。
     *
     * <p>玩家 2026-11-01 要求：<b>除了这七只，其他 boss 的出场运镜全部取消</b>。
     * 魔道统领/虚空行者/虚空霸主/虚空蛟等只是 BOSS/OVERLORD 品阶的散怪，
     * 仍保留血条与出场粒子，但不再播环绕镜头。</p>
     */
    public static boolean isFeaturedBoss(Entity boss) {
        if (boss == null) {
            return false;
        }
        net.minecraft.world.entity.EntityType<?> type = boss.getType();
        return type == ModEntities.EPOCH_BOSS.get()
                || type == ModEntities.BEAST_BOSS.get()
                || type == ModEntities.DEMON_BOSS.get()
                || type == ModEntities.VOID_BOSS.get()
                || type == ModEntities.BLAZE_EMPEROR.get()
                || type == ModEntities.VOID_DRAGON_EMPEROR.get()
                || type == ModEntities.EVIL_GAZER.get()
                // 第四轮「天外天」：仙庭之主也走 Boss 出场运镜（HeavenRealm 召唤时调 play）
                || type == com.hongmeng.xiuxian.entity.ModEntitiesExtra3.HEAVEN_LORD.get();
    }

    /**
     * 给附近的生存模式玩家播放 BOSS 出场演出。
     *
     * <p><b>只给七首领。</b> 其他 BOSS/OVERLORD 品阶的散怪直接跳过，
     * 不进入 {@link #DONE} 记账，也不发镜头包。</p>
     *
     * @param boss  出场的那只 boss
     * @param title 大标题（例如「傀儡祖师」）
     */
    public static void play(ServerLevel level, Entity boss, String title, String subtitle) {
        if (boss == null || !isFeaturedBoss(boss)) {
            if (boss != null) {
                // 非七首领：出场不演，死亡也不补演。记进 DONE 是为了保持
                // 「同一只只演一次」的旧语义，避免它死亡时突然冒出一段镜头。
                DONE.add(boss.getUUID());
                HongmengMod.LOGGER.debug("[cinematic] 非七首领，跳过出场运镜：{}",
                        boss.getType());
            }
            return;
        }
        playInternal(level, boss, title, subtitle, true);
    }

    private static void playInternal(ServerLevel level, Entity boss, String title,
                                     String subtitle, boolean requireFeatured) {
        if (level == null || boss == null) {
            return;
        }
        if (requireFeatured && !isFeaturedBoss(boss)) {
            return;
        }
        if (!DONE.add(boss.getUUID())) {
            return;                         // 这只已经演过了
        }
        try {
            Vec3 anchor = boss.position();
            float[] shots = buildShots();
            CinematicPacket pkt = CinematicPacket.play(anchor, shots, TICKS,
                    title, subtitle);
            int sent = 0;
            for (ServerPlayer p : nearbySurvivalPlayers(level, anchor, 64.0D)) {
                ModNetwork.sendToPlayer(p, pkt);
                PLAYING.put(p.getUUID(), level.getGameTime());
                // 演出期间无敌（灾变的过场也是这么做的）
                p.setInvulnerable(true);
                sent++;
            }
            HongmengMod.LOGGER.info("[cinematic] {} 出场演出：{} 名玩家，锚点 {}",
                    title, sent, anchor);
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[cinematic] 演出启动失败：{}", t.toString());
        }
    }

    /**
     * 死亡运镜总开关。
     *
     * <p>玩家 2026-11-01 要求：<b>死亡运镜全关</b>（七首领与非七首领都不播）。
     * 改成 {@code true} 即恢复。</p>
     */
    public static final boolean DEATH_CINEMATIC_ENABLED = false;

    /**
     * <b>首领陨落</b>的运镜（玩家限定：死亡也要运镜）。
     *
     * <p>复用出场那套关键帧，标题换成"陨落"，收尾同样会还原 HUD。
     * 死亡运镜不套七首领白名单——因为本次要求是<b>死亡运镜全关</b>。</p>
     */
    public static void playDeath(ServerLevel level, Entity boss, String name) {
        if (!DEATH_CINEMATIC_ENABLED) {
            return;
        }
        try {
            playInternal(level, boss, "\u00a74\u00a7l\u9996\u9886\u9668\u843d",
                    "\u00a77" + name + " \u5df2\u88ab\u65a9\u6740", false);
        } catch (Throwable ignored) {
            // 演出失败不影响结算
        }
    }

    /**
     * 造关键帧：绕 boss 一圈的环绕运镜。
     *
     * <p>每 5 个浮点是一帧：{@code (距离, 水平角, 俯仰角, 看向高度, 视场角)}。</p>
     */
    private static float[] buildShots() {
        // 距离一开始远、最后也远（收束），中间贴近 —— 经典的"推近再拉回"
        List<float[]> shots = new ArrayList<>();
        shots.add(new float[]{16.0F, -140.0F, 22.0F, 1.4F, 62.0F});   // 远处低角度
        shots.add(new float[]{12.0F, -100.0F, 12.0F, 1.6F, 58.0F});   // 拉近 + 抬高
        shots.add(new float[]{8.5F, -50.0F, 6.0F, 1.8F, 54.0F});      // 环绕到侧面
        shots.add(new float[]{7.5F, 15.0F, 14.0F, 2.0F, 52.0F});      // 绕到背面 + 俯视
        shots.add(new float[]{7.0F, 75.0F, 4.0F, 1.7F, 50.0F});       // 回到正面
        shots.add(new float[]{5.5F, 110.0F, -8.0F, 2.2F, 46.0F});     // 贴近仰拍（压迫感）
        shots.add(new float[]{14.0F, 150.0F, 16.0F, 1.5F, 60.0F});    // 拉远收束
        float[] flat = new float[shots.size() * 5];
        int i = 0;
        for (float[] s : shots) {
            for (float v : s) {
                flat[i++] = v;
            }
        }
        return flat;
    }

    /** 附近的**生存模式**玩家（创造/旁观不打扰）。 */
    private static List<ServerPlayer> nearbySurvivalPlayers(ServerLevel level, Vec3 at,
                                                            double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        AABB box = new AABB(at.x - radius, at.y - radius, at.z - radius,
                at.x + radius, at.y + radius, at.z + radius);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (!p.isSpectator() && !p.isCreative()) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * 每 tick 收尾：演出结束就解除无敌、发 stop。
     *
     * <p>由 {@code ModEvents.onPlayerTick} 调。</p>
     */
    public static void tick(ServerLevel level) {
        if (PLAYING.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        java.util.Iterator<Map.Entry<UUID, Long>> it = PLAYING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (now - e.getValue() < TICKS) {
                continue;
            }
            it.remove();
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                ModNetwork.sendToPlayer(p, CinematicPacket.stop());
                p.setInvulnerable(false);
            }
        }
    }

    /**
     * 玩家掉线时清账。
     *
     * <p>⚠ <b>只删记录是不够的。</b>无敌是 {@code setInvulnerable(true)} 挂上去的，
     * 而解除它在 {@code tick} 的收尾段 —— 一旦这条记录被删掉，
     * 那段 tick <b>永远不会再跑</b>，于是 {@code setInvulnerable(false)} 也就永远不执行。</p>
     *
     * <p>更糟的是 {@code Invulnerable} 会<b>写进 NBT</b>：重新登录还带着，
     * 等于<b>永久无敌</b>，而且玩家自己都不知道怎么来的。</p>
     *
     * <p>所以这里必须<b>同时解除无敌</b>。</p>
     */
    public static void forget(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) {
            return;
        }
        playStateRemove(player.getUUID());
        // 关键：真正把无敌摘掉
        player.setInvulnerable(false);
    }

    /** 只清记录（拿不到实体时的兜底；**不能**用它来防止无敌残留）。 */
    public static void forget(UUID uuid) {
        playStateRemove(uuid);
    }

    private static void playStateRemove(UUID uuid) {
        if (uuid != null) {
            PLAYING.remove(uuid);
        }
    }
}
