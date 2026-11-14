package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import com.hongmeng.xiuxian.network.BossEntrancePacket;
import com.hongmeng.xiuxian.network.CombatFeedbackPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 强者登场（{@code docs/combat-feel-spec.md} §3.2）。
 *
 * <p>规格要求四件事一起上：<b>天空短暂变暗 · 一次低沉的钟声/雷鸣 · 地面径向冲击波 ·
 * BOSS 血条从屏幕上方压下</b>。四件事的分工是：</p>
 *
 * <ul>
 *   <li><b>钟声 / 雷鸣</b> + <b>径向冲击波粒子</b>：服务端（{@link CombatSounds#bossEntrance}
 *       / {@link CombatParticles#bossShockwave}）——所有人都在同一个世界里听见、看见；</li>
 *   <li><b>天空变暗</b> + <b>横幅压下</b>：客户端（{@code client.BossEntranceOverlay}），
 *       由一包 {@link CombatFeedbackPacket#KIND_BOSS_ENTRANCE} 触发；</li>
 *   <li><b>血条本身</b>：原版 {@code ServerBossEvent}（头目 / 首领本来就有，
 *       见 {@code CultivationMonsterEntity}），横幅只是"压下来"的那个演出动作。</li>
 * </ul>
 *
 * <h2>触发条件（三个都必须满足）</h2>
 *
 * <ol>
 *   <li>品阶是 {@link MobRank#BOSS} 或 {@link MobRank#OVERLORD}（头目 / 首领）；</li>
 *   <li><b>不是读档</b>（{@code loadedFromDisk() == false}）——否则每次靠近旧怪都敲一次钟；</li>
 *   <li>附近 {@value CombatFeel#ENTRANCE_RADIUS} 格内至少有<b>一个活着的玩家</b>。</li>
 * </ol>
 *
 * <p>同一只怪一辈子只登场一次（UUID 记账），表大小有上限。</p>
 */
public final class BossEntrance {

    /** 记录表上限（超过就整体清空：宁可多敲一次钟，也不能把表撑成内存泄漏）。 */
    private static final int MAX_REMEMBERED = 512;

    private static final Set<UUID> ANNOUNCED = new HashSet<>();

    private BossEntrance() {
    }

    /** 由 {@code EntityJoinLevelEvent} 调。 */
    public static void onEntityJoin(Entity entity, net.minecraft.world.level.Level level,
                                    boolean loadedFromDisk) {
        if (loadedFromDisk || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(entity instanceof CultivationMonsterEntity monster)) {
            return;
        }
        if (!ParticleBudget.enabled()) {
            return;
        }
        if (!entranceEnabled()) {
            return;
        }
        MobRank rank = monster.rank();
        if (rank != MobRank.BOSS && rank != MobRank.OVERLORD) {
            return;
        }
        // 2026-11-01 玩家要求：只有七首领保留出场演出；
        // 魔道统领/虚空行者/虚空霸主/虚空蛟等 BOSS 品阶散怪不再播。
        if (!BossCinematic.isFeaturedBoss(monster)) {
            return;
        }
        if (monster.isRemoved() || !monster.isAlive()) {
            return;
        }
        double rangeSq = CombatFeel.ENTRANCE_RADIUS * CombatFeel.ENTRANCE_RADIUS;
        List<ServerPlayer> near = serverLevel.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.distanceToSqr(monster) <= rangeSq);
        if (near.isEmpty()) {
            return;
        }
        if (ANNOUNCED.size() >= MAX_REMEMBERED) {
            ANNOUNCED.clear();
        }
        if (!ANNOUNCED.add(monster.getUUID())) {
            return;
        }

        Vec3 at = monster.position();
        // ---- 钟声 + 雷鸣 + 径向冲击波 ----
        CombatSounds.bossEntrance(serverLevel, at);
        CombatParticles.bossShockwave(serverLevel, at);
        // 天空变暗由客户端做（全屏压暗 + 横幅），这里只把"谁来了"说清楚
        CombatFeedbackPacket packet = CombatFeedbackPacket.bossEntrance(
                monster, rank.auraColor(), monster.getName().getString());
        // ---- 出场序列：虚空裂缝 + 缓慢降落（玩家要求）----
        // 时长 140 tick（7 秒）：改天换日 → 晴天霹雳 → 缓慢降落 → 落地冲击。
        int entranceTicks = 140;
        BossEntrancePacket entrance = new BossEntrancePacket(at, entranceTicks);
        for (ServerPlayer player : near) {
            ModNetwork.sendToPlayer(player, packet);
            ModNetwork.sendToPlayer(player, entrance);
        }
    }

    /**
     * <b>显式触发一次出场演出</b>（忽略品阶判定）。
     *
     * <p>为什么需要它：{@link #onEntityJoin} 只在"加入世界"那一刻触发，而且要求
     * BOSS/OVERLORD 品阶。祭坛召唤的流程是"先加入世界 → 再拉上天降落"，
     * 加入时可能还没打上标记，演出就漏掉了 ——
     * 玩家实测「他为什么是直接出生在祭坛召唤石头上，不是从天上缓缓下降」。</p>
     */
    public static void force(ServerLevel level, Entity entity) {
        if (level == null || entity == null) {
            return;
        }
        // 显式触发也只给七首领：避免通用祭坛召出魔道统领时又演一遍。
        if (!BossCinematic.isFeaturedBoss(entity)) {
            return;
        }
        try {
            Vec3 at = entity.position();
            double rangeSq = CombatFeel.ENTRANCE_RADIUS * CombatFeel.ENTRANCE_RADIUS;
            List<ServerPlayer> near = level.getPlayers(p -> p.isAlive() && !p.isSpectator()
                    && p.distanceToSqr(at) <= rangeSq);
            if (near.isEmpty()) {
                return;
            }
            CombatSounds.bossEntrance(level, at);
            CombatParticles.bossShockwave(level, at);
            BossEntrancePacket entrance = new BossEntrancePacket(at, 140);
            for (ServerPlayer player : near) {
                if (entity instanceof CultivationMonsterEntity monster) {
                    ModNetwork.sendToPlayer(player, CombatFeedbackPacket.bossEntrance(
                            monster, monster.rank().auraColor(),
                            monster.getName().getString()));
                }
                ModNetwork.sendToPlayer(player, entrance);
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[boss] 强制出场演出失败：{}", t.toString());
        }
    }

    /** 演出开关（配置：{@code combat.bossEntrance}）。 */
    private static boolean entranceEnabled() {
        try {
            return com.hongmeng.xiuxian.compat.HongmengConfig.SERVER.bossEntrance.get();
        } catch (Throwable t) {
            return true;
        }
    }

    /** 换存档 / 停服清理。 */
    public static void clear() {
        ANNOUNCED.clear();
    }

    /** 已登场过的怪数量（诊断）。 */
    public static int announcedCount() {
        return ANNOUNCED.size();
    }
}
