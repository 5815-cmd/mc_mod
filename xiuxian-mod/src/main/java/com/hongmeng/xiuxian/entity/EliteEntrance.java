package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.network.CinematicPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>精英怪出场演出</b>：第一次照面时，镜头绕它转 1.2 秒。
 *
 * <h2>为什么需要</h2>
 * <p>精英怪和杂鱼在画面上的区别只有「头顶多一个品阶标记」——
 * 玩家往往砍了两刀才发现"这只是精英"。</p>
 *
 * <p>出场演出把这个信息<b>提前</b>：镜头绕它一圈 + 报名字，
 * 玩家立刻知道"这一只要认真打"。</p>
 *
 * <h2>三条防打扰约束</h2>
 * <ol>
 *   <li><b>每只怪只演一次</b>（存在它的持久化数据里，区块重载也记得）；</li>
 *   <li><b>每个玩家 90 秒内只演一次</b> —— 否则连遇三只精英会连播三段；</li>
 *   <li><b>只对精英以上</b>，杂鱼永不触发。</li>
 * </ol>
 *
 * <h2>为什么用现成的 {@code CinematicPacket}</h2>
 * <p>项目里已经有一套"服务端算关键帧、客户端只插值"的镜头系统
 * （Boss 出场、真世界开启都在用）。再写一套只会多一条不一致的路径。</p>
 */
public final class EliteEntrance {

    /** 演出时长（tick）—— 1.2 秒，够看清但不烦。 */
    public static final int TICKS = 24;

    /** 触发距离（格）。 */
    public static final double TRIGGER_RANGE = 14.0D;

    /** 同一玩家的最短间隔（tick）—— 90 秒。 */
    public static final long PLAYER_COOLDOWN = 1800L;

    private static final String TAG_INTRO = "hm_intro_done";

    /** 每玩家上次演出的时间。 */
    private static final Map<UUID, Long> LAST_INTRO = new ConcurrentHashMap<>();

    private EliteEntrance() {
    }

    /**
     * 每 tick 调一次（由 {@code CultivationMonsterEntity.customServerAiStep} 转发）。
     */
    public static void tick(CultivationMonsterEntity mob) {
        if (mob == null || mob.level().isClientSide()) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        // 只对精英以上
        if (mob.effectiveRank().ordinal() < MobRank.ELITE.ordinal()) {
            return;
        }
        // 2026-11-01 玩家要求：非七首领一律不再播「第一次照面绕 1.2 秒」。
        // 这条路径不经过 BossCinematic，之前会绕过七首领白名单；
        // 精英/队长/头目/首领全部一起关，只留七首领。
        if (!com.hongmeng.xiuxian.combat.BossCinematic.isFeaturedBoss(mob)) {
            return;
        }
        var pd = mob.getPersistentData();
        if (pd.getBoolean(TAG_INTRO)) {
            return;
        }
        // 找 14 格内最近、且不在演出中的玩家
        ServerPlayer target = null;
        double best = TRIGGER_RANGE * TRIGGER_RANGE;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isDeadOrDying()) {
                continue;
            }
            double d = p.distanceToSqr(mob);
            if (d > best) {
                continue;
            }
            Long last = LAST_INTRO.get(p.getUUID());
            if (last != null && level.getGameTime() - last < PLAYER_COOLDOWN) {
                continue;   // 这个玩家刚看过别的怪出场
            }
            best = d;
            target = p;
        }
        if (target == null) {
            return;
        }
        // 标记 + 冷却
        pd.putBoolean(TAG_INTRO, true);
        LAST_INTRO.put(target.getUUID(), level.getGameTime());
        play(level, mob, target);
    }

    /** 播一段绕怪旋转的短镜头。 */
    private static void play(ServerLevel level, CultivationMonsterEntity mob,
                             ServerPlayer player) {
        try {
            Vec3 at = mob.position();
            // 绕它转 160°（半个侧面），同时略微升高：交代体型
            float[] shots = {
                    3.6F, 0.0F, 6.0F, 1.0F, 66.0F,
                    4.2F, 55.0F, 10.0F, 1.2F, 62.0F,
                    4.6F, 110.0F, 14.0F, 1.3F, 60.0F,
                    4.2F, 160.0F, 12.0F, 1.2F, 62.0F,
            };
            String rankName = switch (mob.effectiveRank()) {
                case OVERLORD -> "\u00a7c\u00a7l鸿蒙";
                case BOSS -> "\u00a74\u00a7l首领";
                case CAPTAIN -> "\u00a76\u00a7l队长";
                default -> "\u00a7e\u00a7l精英";
            };
            ModNetwork.sendToPlayer(player, CinematicPacket.play(at, shots, TICKS,
                    "\u00a77" + mob.getName().getString(),
                    rankName + "\u00a77 · \u00a78" + realmLabel(mob)));
            // 低吼：让"这一只不一样"有听觉锚点
            level.playSound(null, at.x, at.y, at.z,
                    SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.7F, 1.15F);
        } catch (Throwable ignored) {
            // 演出失败不影响战斗
        }
    }

    /** 它当前的境界名（用于副标题）。 */
    private static String realmLabel(CultivationMonsterEntity mob) {
        try {
            return mob.realmCn();
        } catch (Throwable t) {
            return "境界未知";
        }
    }

    /** 掉线清理。 */
    public static void forget(UUID id) {
        if (id != null) {
            LAST_INTRO.remove(id);
        }
    }
}
