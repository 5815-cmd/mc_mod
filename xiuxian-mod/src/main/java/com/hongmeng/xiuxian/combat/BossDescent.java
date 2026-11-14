package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Boss 从天上缓缓降落</b>（玩家要求）。
 *
 * <p>玩家原话：「他为什么是直接出生在祭坛召唤石头上，不是从天上缓缓下降」。</p>
 *
 * <h2>演出流程（共 {@value #TICKS} tick ≈ 7 秒）</h2>
 * <ol>
 *   <li><b>0~40t</b>：虚空裂缝在祭坛上方张开（粒子 + 雷鸣），boss 悬在高处不可见；</li>
 *   <li><b>40~120t</b>：boss <b>缓缓降落</b>（缓入缓出），身周拖出暗色尾迹；</li>
 *   <li><b>120~140t</b>：落地冲击环 + 半球气浪 + 怒吼，boss 解除无敌、开始攻击。</li>
 * </ol>
 *
 * <p>降落期间 boss <b>无敌且不攻击</b> —— 否则玩家在它降下来之前就挨打了。</p>
 */
public final class BossDescent {

    /** 演出总时长（tick）。 */
    public static final int TICKS = 140;
    /** 起始高度（祭坛上方多少格）。 */
    private static final double START_LIFT = 26.0D;
    /** 开始降落的时间点。 */
    private static final int DROP_START = 40;
    /** 降落结束的时间点。 */
    private static final int DROP_END = 120;

    /** 正在降落的 boss：id → 状态。 */
    private static final Map<UUID, State> FLYING = new ConcurrentHashMap<>();

    private BossDescent() {
    }

    /** 一只正在降落的 boss 的状态。 */
    private static final class State {
        final UUID id;
        final double groundY;
        final double startY;
        int age;

        State(UUID id, double groundY, double startY) {
            this.id = id;
            this.groundY = groundY;
            this.startY = startY;
        }
    }

    /**
     * 让一只 boss 从天上降落到指定位置。
     *
     * @param ground 落地点（祭坛中心）
     */
    public static void begin(ServerLevel level, Mob boss, BlockPos ground) {
        if (level == null || boss == null || ground == null) {
            return;
        }
        try {
            Vec3 at = Vec3.atCenterOf(ground);
            double groundY = at.y + 1.0D;
            // ① 先放到天上（客户端看不到它突然出现，因为演出同时在放）
            boss.moveTo(at.x, groundY + START_LIFT, at.z, level.random.nextFloat() * 360.0F, 0.0F);
            boss.setNoAi(true);                       // 降落期间不动、不攻击
            boss.setInvulnerable(true);               // 也别被玩家提前打死
            // 2026-11-07 玩家要求：去除 Boss 外圈白框（不再施加 GLOWING）。
            FLYING.put(boss.getUUID(), new State(boss.getUUID(), groundY,
                    groundY + START_LIFT));

            // ② 开场：虚空裂缝 + 雷鸣（客户端演出由 BossEntrancePacket 负责）
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0D, at.z,
                    260, 3.2D, 5.0D, 3.2D, 0.35D);
            level.playSound(null, ground, SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.HOSTILE, 3.0F, 0.5F);

            for (ServerPlayer sp : level.getPlayers(p ->
                    p.distanceToSqr(at) <= 96.0D * 96.0D)) {
                sp.displayClientMessage(Component.literal(
                        "\u00a75\u00a7l虚空裂开了 —— \u00a7f"
                                + boss.getName().getString()
                                + " \u00a75\u00a7l正从天上降临……"), false);
            }
            HongmengMod.LOGGER.info("[boss] {} 开始从天而降（落点 {}）",
                    boss.getName().getString(), ground);
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[boss] 出场降落启动失败：{}", t.toString());
            boss.setNoAi(false);
            boss.setInvulnerable(false);
        }
    }

    /** 由 {@code ModEvents} 每 tick 调；推进所有正在降落的 boss。 */
    public static void tick(ServerLevel level) {
        if (level == null || FLYING.isEmpty()) {
            return;
        }
        for (State st : java.util.List.copyOf(FLYING.values())) {
            var e = level.getEntity(st.id);
            if (!(e instanceof Mob boss) || boss.isRemoved() || !boss.isAlive()) {
                FLYING.remove(st.id);
                continue;
            }
            st.age++;
            if (st.age >= TICKS) {
                finish(level, boss, st);
                continue;
            }
            if (st.age < DROP_START) {
                // 还没开始降：悬在高处
                boss.moveTo(boss.getX(), st.startY, boss.getZ(), boss.getYRot(), 0.0F);
                continue;
            }
            if (st.age <= DROP_END) {
                double t = (double) (st.age - DROP_START) / (DROP_END - DROP_START);
                // 缓入缓出：先慢后快再收
                double ease = t * t * (3.0D - 2.0D * t);
                double y = st.startY - (st.startY - st.groundY) * ease;
                boss.moveTo(boss.getX(), y, boss.getZ(), boss.getYRot(), 0.0F);
                // 身周拖尾
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        boss.getX(), y + 0.4D, boss.getZ(),
                        6, 0.5D, 0.4D, 0.5D, 0.02D);
            } else {
                boss.moveTo(boss.getX(), st.groundY, boss.getZ(), boss.getYRot(), 0.0F);
            }
        }
    }

    /** 落地：冲击环 + 怒吼 + 解除无敌。 */
    private static void finish(ServerLevel level, Mob boss, State st) {
        FLYING.remove(st.id);
        boss.moveTo(boss.getX(), st.groundY, boss.getZ(), boss.getYRot(), 0.0F);
        boss.setNoAi(false);
        boss.setInvulnerable(false);
        Vec3 at = boss.position();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.3D, at.z,
                140, 5.0D, 0.6D, 5.0D, 0.12D);
        level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 2.5F, 0.6F);
        for (ServerPlayer sp : level.getPlayers(p ->
                p.distanceToSqr(at) <= 96.0D * 96.0D)) {
            sp.displayClientMessage(Component.literal(
                    "\u00a74" + boss.getName().getString()
                            + " \u00a77降临了！"), false);
        }
        // 出场强化：抗性 + 加速一小段（"出场即巅峰"）
        boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                20 * 12, 1, false, false));
        boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                20 * 20, 1, false, false));
        HongmengMod.LOGGER.info("[boss] {} 已落地", boss.getName().getString());
    }

    /** 这只 boss 是不是还在降落。 */
    public static boolean isDescending(UUID id) {
        return id != null && FLYING.containsKey(id);
    }

    public static void clear() {
        FLYING.clear();
    }
}
