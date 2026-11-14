package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.combat.CombatSounds;
import com.hongmeng.xiuxian.combat.StunScheduler;
import com.hongmeng.xiuxian.technique.ParticleBatch;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <b>「恶」的技能组 · 状态机版</b>（第五轮重写 + 实机修复 A 的悬空几何，契约
 * {@code 工程/17-最终Boss恶-完善实现契约.md} §2 与 {@code 工程/19-恶实机修复契约.md} §4）。
 *
 * <p>第五轮之前是 {@code gameTime % CD} 轮询 4 招：无前摇、无预警、不可躲。现在改成
 * <b>IDLE → WINDUP（前摇） → ACTIVE（判定） → RECOVER（后摇） → IDLE</b> 的循环，
 * 每一招都有 <b>20~35t 的可躲前摇</b>（锁定线 / 地面环 / 向下斜线 / 落点预警），
 * 玩家靠走位、转身、绕后都能躲开。</p>
 *
 * <h2>悬空几何（实机修复 A）</h2>
 * <p>恶悬在<b>台面上方 13 格</b>、不落地不移动，所以本类的距离/高度口径全部改成
 * <b>水平距离 + 台面高度基准</b>（不再用 {@code position().distanceTo()} 的 3D 距离）：</p>
 * <ul>
 *   <li><b>地面基准</b>：{@code EvilGazerEntity.groundCenter()} = 恶在台面上的脚下投影；
 *       {@code hoverFloorY()} = 台面高度（玩家脚底）。</li>
 *   <li><b>水平距离</b>：判定统一用 {@code Math.hypot(dx, dz)} / {@code horizontalDistanceTo}。</li>
 *   <li><b>台面判定</b>：{@code Math.abs(玩家脚底 Y − 台面 Y) ≤ EvilGazerEntity.PLATFORM_TOLERANCE}。</li>
 * </ul>
 *
 * <h2>技能表（7 招，阶段按血量 66%/33% 解锁）</h2>
 * <table border="1">
 *   <caption>前摇 / 判定 / 后摇（tick）+ 悬空几何口径</caption>
 *   <tr><th>技能</th><th>id</th><th>阶段</th><th>前摇</th><th>判定</th><th>后摇</th><th>几何 / 可躲方式</th></tr>
 *   <tr><td>凝视 GAZE</td><td>1</td><td>1+</td><td>25</td><td>12</td><td>20</td>
 *       <td><b>水平 ≤40</b>；它看着你（3D 视线含向下）且你面向它 → 掉血；背对/绕后躲</td></tr>
 *   <tr><td>巨掌横扫 SWEEP</td><td>2</td><td>1+</td><td>20</td><td>8</td><td>18</td>
 *       <td><b>水平 ≤12</b> 的前方 120° 扇形（按水平方向）；命中给台面击退</td></tr>
 *   <tr><td>眼棱扫射 BEAM</td><td>3</td><td>2+</td><td>22</td><td>10</td><td>16</td>
 *       <td>从<b>眼睛指向玩家</b>（含向下俯角）的 3D 线段，命中 = 玩家到线段距离 ≤2.2；横移躲</td></tr>
 *   <tr><td>唤眼 SUMMON</td><td>4</td><td>2+</td><td>24</td><td>1</td><td>30</td>
 *       <td>小眼刷在<b>台面</b>（恶脚下投影一圈、台面 +1.5，无重力浮着）</td></tr>
 *   <tr><td>裂口重压 SLAM</td><td>5</td><td>3+</td><td>30</td><td>1</td><td>24</td>
 *       <td>以<b>台面投影为圆心</b>、半径 26 的地面环；命中台面上水平 ≤26 的玩家；跑出环躲</td></tr>
 *   <tr><td>天崩 METEOR</td><td>6</td><td>3+</td><td>35</td><td>1</td><td>20</td>
 *       <td>落点在<b>台面</b>（玩家脚下附近），从天空下落；3 波走位躲</td></tr>
 *   <tr><td>狂暴 ENRAGE</td><td>7</td><td>HP&lt;15% 一次</td><td>20</td><td>1</td><td>30</td>
 *       <td>—（攻击 +30%、冷却 ×0.6；几何不变）</td></tr>
 * </table>
 *
 * <h2>伤害口径（契约 §2.1）</h2>
 * <p>全部走 {@code victim.hurt(level.damageSources().mobAttack(self), dmg)}：
 * 吃护甲、吃境界压制、吃 35% 单次软上限；本类<b>零 {@code setHealth}</b>。</p>
 *
 * <h2>节奏</h2>
 * <p>出手间隔（IDLE 冷却）：阶段一 <b>{@value #COOLDOWN_PHASE1}t</b> /
 * 阶段二 <b>{@value #COOLDOWN_PHASE2}t</b> / 阶段三 <b>{@value #COOLDOWN_PHASE3}t</b>；
 * 狂暴后 ×{@value #ENRAGE_CD_MUL}。小眼是弱化版：只有凝视、节奏更慢。</p>
 */
public final class EvilAbilities {

    // ==================================================================
    // 1. 状态机常量（与同步字段取值一致）
    // ==================================================================

    /** 待机。 */
    public static final int STATE_IDLE = 0;
    /** 前摇（客户端可开始震屏/预警）。 */
    public static final int STATE_WINDUP = 1;
    /** 判定（命中瞬间）。 */
    public static final int STATE_ACTIVE = 2;
    /** 后摇（含阶段转场）。 */
    public static final int STATE_RECOVER = 3;

    /** 出手间隔：阶段一。 */
    public static final int COOLDOWN_PHASE1 = 120;
    /** 出手间隔：阶段二。 */
    public static final int COOLDOWN_PHASE2 = 95;
    /** 出手间隔：阶段三。 */
    public static final int COOLDOWN_PHASE3 = 70;
    /** 狂暴后出手间隔倍率。 */
    public static final double ENRAGE_CD_MUL = 0.6D;
    /** 狂暴触发血量比例（HP &lt; 15%）。 */
    public static final double ENRAGE_HP_FRACTION = 0.15D;
    /** 狂暴：攻击 +30%。 */
    public static final double ENRAGE_ATK_BONUS = 0.30D;

    /** 小眼出手间隔。 */
    public static final int MINION_COOLDOWN = 160;
    /** 小眼前摇。 */
    public static final int MINION_WINDUP = 12;
    /** 小眼判定。 */
    public static final int MINION_ACTIVE = 8;
    /** 小眼后摇。 */
    public static final int MINION_RECOVER = 10;
    /** 小眼凝视伤害倍率（弱化）。 */
    public static final double MINION_GAZE_MUL = 0.5D;

    // ==================================================================
    // 2. 招式表（唯一真源：check_evil_fight.py 逐条核对）
    // ==================================================================

    /**
     * 7 招。{@code id()} = ordinal + 1，正好对上 {@code setBossAttack(byte)} 的 1~7。
     *
     * <p>字段顺序：解锁阶段 / 前摇 / 判定 / 后摇。</p>
     */
    public enum Skill {
        /** 凝视：锁定玩家，面向它的持续掉血 + 缓慢；背对/绕后可躲。 */
        GAZE(1, 25, 12, 20),
        /** 巨掌横扫：前方 120° 扇形击退 + 伤害。 */
        SWEEP(1, 20, 8, 18),
        /** 眼棱扫射：直线 34 格光束，可横移躲。 */
        BEAM(2, 22, 10, 16),
        /** 唤眼：召 2~3 只小眼（爪牙模式，不递归）。 */
        SUMMON(2, 24, 1, 30),
        /** 裂口重压：半径 26 地面环冲击 + 强击退，可跑出/跳起躲。 */
        SLAM(3, 30, 1, 24),
        /** 天崩：3 波陨石落点，可走位躲。 */
        METEOR(3, 35, 1, 20),
        /** 狂暴：HP&lt;15% 一次，攻击 +30%、冷却 ×0.6。 */
        ENRAGE(0, 20, 1, 30);

        /** 解锁阶段（0 = 特殊条件触发，不按阶段）。 */
        public final int minPhase;
        /** 前摇 tick。 */
        public final int windup;
        /** 判定 tick。 */
        public final int active;
        /** 后摇 tick。 */
        public final int recover;

        Skill(int minPhase, int windup, int active, int recover) {
            this.minPhase = minPhase;
            this.windup = windup;
            this.active = active;
            this.recover = recover;
        }

        /** 同步 id（1..7）。 */
        public byte id() {
            return (byte) (this.ordinal() + 1);
        }
    }

    /** 阶段一攻击池。 */
    private static final Skill[] POOL_P1 = {Skill.GAZE, Skill.SWEEP};
    /** 阶段二攻击池。 */
    private static final Skill[] POOL_P2 = {Skill.GAZE, Skill.SWEEP, Skill.BEAM, Skill.SUMMON};
    /** 阶段三攻击池。 */
    private static final Skill[] POOL_P3 = {Skill.GAZE, Skill.SWEEP, Skill.BEAM, Skill.SUMMON,
            Skill.SLAM, Skill.METEOR};
    /** 小眼攻击池：只有弱化凝视（不递归、不狂暴）。 */
    private static final Skill[] POOL_MINION = {Skill.GAZE};

    // ==================================================================
    // 3. 数值
    // ==================================================================

    /** 凝视半径（<b>水平</b>距离；实机修复 A：悬空 Boss 不能按 3D 距离算）。 */
    public static final double GAZE_RANGE = 40.0D;
    /** 凝视"面向"判定阈值（恶看着玩家 & 玩家面向恶）。 */
    public static final double GAZE_FACE_DOT = 0.45D;
    /** 凝视：玩家"面向它"的判定阈值（背对 → 点积为负 → 躲开）。 */
    public static final double GAZE_PLAYER_DOT = 0.0D;
    /** 巨掌横扫半径（<b>水平</b>距离；契约 §4.2：水平 ≤12）。 */
    public static final double SWEEP_RANGE = 12.0D;
    /** 巨掌横扫半角（度）—— 120° 扇形。 */
    public static final double SWEEP_HALF_ANGLE = 60.0D;
    /** 眼棱最大长度。 */
    public static final double BEAM_LENGTH = 34.0D;
    /** 眼棱线宽（半径）：玩家中心到光束 3D 线段的距离 ≤ 它即命中。 */
    public static final double BEAM_HALF_WIDTH = 2.2D;
    /** 裂口重压半径（台面地面环）。 */
    public static final double SLAM_RADIUS = 26.0D;
    /** 天崩波数。 */
    public static final int METEOR_WAVES = 3;
    /** 天崩波间隔（tick）。 */
    public static final int METEOR_WAVE_GAP = 8;
    /** 天崩落点半径。 */
    public static final double METEOR_RADIUS = 4.0D;
    /** 台面判定容差（格），与 {@code EvilGazerEntity.PLATFORM_TOLERANCE} 同口径。 */
    public static final double PLATFORM_TOL = EvilGazerEntity.PLATFORM_TOLERANCE;

    /** 伤害倍率（× 攻击力；走 mobAttack → 吃护甲/境界压制/35% 软上限）。 */
    private static final double GAZE_MUL = 0.25D;
    private static final double SWEEP_MUL = 0.8D;
    private static final double BEAM_MUL = 1.0D;
    private static final double SLAM_MUL = 1.2D;
    private static final double METEOR_MUL = 0.7D;

    /** 狂暴的攻击力永久修饰符（与其它系统分开 UUID）。 */
    private static final UUID ENRAGE_ATK_ID = UUID.nameUUIDFromBytes(
            "evil_enrage_atk".getBytes(StandardCharsets.UTF_8));

    // 旧版轮询冷却：第五轮已改成状态机节奏，这几个常量只为兼容保留
    /** @deprecated 见 {@link #COOLDOWN_PHASE1}。 */
    @Deprecated
    public static final int CD_GAZE = 200;
    /** @deprecated 见 {@link #COOLDOWN_PHASE2}。 */
    @Deprecated
    public static final int CD_BEAM = 160;
    /** @deprecated 见 {@link #COOLDOWN_PHASE2}。 */
    @Deprecated
    public static final int CD_SUMMON = 320;
    /** @deprecated 见 {@link #COOLDOWN_PHASE3}。 */
    @Deprecated
    public static final int CD_SLAM = 220;

    private EvilAbilities() {
    }

    // ==================================================================
    // 4. 主循环
    // ==================================================================

    /** 每 tick 推进状态机（由 {@code EvilGazerEntity.tick} 调用）。 */
    public static void tick(EvilGazerEntity self) {
        if (self == null || !(self.level() instanceof ServerLevel level)) {
            return;
        }
        if (!self.isAlive() || self.escaping() || self.transitioning()) {
            return;   // 死亡/逃走/阶段转场：技能停转
        }
        // 附近（水平）没玩家就不空放技能（清掉前摇，别对着空气挥完一整套）。
        // 悬空 Boss 不能用 3D 距离判断：它在台面上方 13 格，3D 距离会把台面边缘的玩家
        // 误算成"没人"（水平 40 格时 3D 距离已是 42 格）。
        if (!hasNearbyPlayer(self, level, GAZE_RANGE)) {
            if (self.state != STATE_IDLE) {
                reset(self);
            }
            return;
        }
        try {
            switch (self.state) {
                case STATE_WINDUP -> tickWindup(self, level);
                case STATE_ACTIVE -> tickActive(self, level);
                case STATE_RECOVER -> tickRecover(self);
                default -> tickIdle(self, level);
            }
            // 狂暴后的"全程血红特效"：低频红色粒子
            if (self.enraged() && self.tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.LAVA,
                        self.getX(), self.getY() + self.getBbHeight() * 0.6D, self.getZ(),
                        6, 1.6D, 1.2D, 1.6D, 0.02D);
            }
        } catch (Throwable t) {
            // 一个 Boss 的技能出错绝不能带崩服务端
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug("[evil] 技能状态机异常：{}",
                    t.toString());
            reset(self);
        }
    }

    /** 清空状态机（阶段转场/逃走/异常时用）：回 IDLE、招式清 0、同步字段归零。 */
    public static void reset(EvilGazerEntity self) {
        if (self == null) {
            return;
        }
        self.state = STATE_IDLE;
        self.attack = 0;
        self.stageTicks = 0;
        self.elapsed = 0;
        self.cooldown = 0;
        self.meteorSpotCount = 0;
        self.setBossState((byte) STATE_IDLE);
        self.setBossAttack((byte) 0);
    }

    /** 待机：冷却递减，条件满足就选下一招起手。 */
    private static void tickIdle(EvilGazerEntity self, ServerLevel level) {
        if (self.cooldown > 0) {
            self.cooldown--;
            return;
        }
        Skill pick;
        if (!self.isMinion() && !self.enraged() && hpFraction(self) <= ENRAGE_HP_FRACTION) {
            // 狂暴优先占一次起手（有 20t 怒吼前摇）
            pick = Skill.ENRAGE;
        } else {
            Skill[] pool = poolFor(self);
            if (pool.length == 0) {
                return;
            }
            int idx = self.getRandom().nextInt(pool.length);
            if (pool.length > 1 && pool[idx].id() == self.lastAttack) {
                idx = (idx + 1) % pool.length;   // 不连续重复同一招
            }
            pick = pool[idx];
        }
        startWindup(self, level, pick);
    }

    /** 起手：写状态 + 同步字段 + 起手音 + 首帧预警。 */
    private static void startWindup(EvilGazerEntity self, ServerLevel level, Skill skill) {
        self.state = STATE_WINDUP;
        self.attack = skill.id();
        self.stageTicks = windupFor(self, skill);
        self.elapsed = 0;
        self.lastAttack = skill.id();
        sync(self);
        if (skill == Skill.METEOR) {
            pickMeteorSpots(self, level);
        }
        level.playSound(null, self.blockPosition(), windupSound(skill),
                SoundSource.HOSTILE, 2.2F, windupPitch(skill));
        telegraph(self, level, skill, 0.0F);
    }

    /** 前摇：倒计时 + 预警（每 3t）+ 蓄力音（每 4t）。 */
    private static void tickWindup(EvilGazerEntity self, ServerLevel level) {
        Skill skill = skillOf(self.attack);
        if (skill == null) {
            reset(self);
            return;
        }
        self.stageTicks--;
        if (self.stageTicks % 4 == 0) {
            level.playSound(null, self.blockPosition(), windupSound(skill),
                    SoundSource.HOSTILE, 0.9F, windupPitch(skill) + 0.15F);
        }
        if (self.stageTicks % 3 == 0) {
            float progress = 1.0F - (float) self.stageTicks
                    / Math.max(1, windupFor(self, skill));
            telegraph(self, level, skill, Math.max(0.0F, Math.min(1.0F, progress)));
        }
        if (self.stageTicks > 0) {
            return;
        }
        // ---- 进入判定：首帧立即结算 ----
        self.state = STATE_ACTIVE;
        self.elapsed = 0;
        sync(self);
        payload(self, level, skill, 0);
    }

    /** 判定：多段招式在这里逐段结算。 */
    private static void tickActive(EvilGazerEntity self, ServerLevel level) {
        Skill skill = skillOf(self.attack);
        if (skill == null) {
            reset(self);
            return;
        }
        self.elapsed++;
        if (self.elapsed < activeFor(self, skill)) {
            payload(self, level, skill, self.elapsed);
            return;
        }
        self.state = STATE_RECOVER;
        self.stageTicks = recoverFor(self, skill);
        sync(self);
    }

    /** 后摇结束：回 IDLE，写下一轮冷却。 */
    private static void tickRecover(EvilGazerEntity self) {
        if (--self.stageTicks > 0) {
            return;
        }
        self.state = STATE_IDLE;
        self.attack = 0;
        self.elapsed = 0;
        int cd = cooldownFor(self);
        if (self.enraged()) {
            cd = Math.max(20, (int) Math.round(cd * ENRAGE_CD_MUL));
        }
        self.cooldown = cd;
        sync(self);
    }

    /** 写同步字段（供客户端镜头/特效；只在状态切换时写，不逐 tick 刷）。 */
    private static void sync(EvilGazerEntity self) {
        self.setBossState((byte) self.state);
        self.setBossAttack((byte) self.attack);
    }

    // ==================================================================
    // 5. 判定（payload）
    // ==================================================================

    /** 单招判定。{@code elapsed} 是 ACTIVE 段第几 tick（0 = 判定首帧）。 */
    private static void payload(EvilGazerEntity self, ServerLevel level, Skill skill, int elapsed) {
        switch (skill) {
            case GAZE -> {
                if (elapsed % 4 == 0) {
                    gazeHit(self, level);
                }
            }
            case SWEEP -> {
                if (elapsed == 0) {
                    sweepHit(self, level);
                }
            }
            case BEAM -> {
                if (elapsed == 0) {
                    beamHit(self, level, -12.0F);   // 第一束：偏左
                } else if (elapsed == 5) {
                    beamHit(self, level, 12.0F);    // 第二束：偏右（扫射）
                }
            }
            case SUMMON -> {
                if (elapsed == 0) {
                    summonEyes(self, level);
                }
            }
            case SLAM -> {
                if (elapsed == 0) {
                    slamHit(self, level);
                }
            }
            case METEOR -> {
                if (elapsed == 0) {
                    meteorLaunch(self, level);
                }
            }
            case ENRAGE -> {
                if (elapsed == 0) {
                    applyEnrage(self, level);
                }
            }
            default -> {
                // 不会到这里
            }
        }
    }

    /**
     * <b>凝视</b>：面向它的玩家掉血 + 缓慢。
     *
     * <p>悬空几何（实机修复 A）：范围用<b>水平距离 ≤{@value #GAZE_RANGE}</b>；
     * 视线判定仍是 3D（从眼睛指向玩家，自然含向下俯角）。
     * 可躲：① 玩家自己<b>转头</b>（不再面向它）；② 绕到它<b>背后</b>（不在它的朝向里）。</p>
     */
    private static void gazeHit(EvilGazerEntity self, ServerLevel level) {
        Vec3 eye = eyePos(self);
        Vec3 look = self.getLookAngle();
        float dmg = dmg(self, GAZE_MUL);
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            if (horizontalDistance(self, p.position()) > GAZE_RANGE) {
                continue;   // 水平距离（不再用 3D 距离：恶在台面上方 13 格）
            }
            Vec3 toPlayer = p.position().add(0.0D, 1.0D, 0.0D).subtract(eye);
            if (toPlayer.lengthSqr() < 1.0E-4D) {
                continue;
            }
            toPlayer = toPlayer.normalize();
            // ① 它要"看着"这个玩家（绕后可躲）
            if (toPlayer.dot(look) < GAZE_FACE_DOT) {
                continue;
            }
            // ② 玩家要"面向"它（抬头/平视都行，背对它点积为负 → 躲开）
            Vec3 playerLook = p.getLookAngle();
            Vec3 toBoss = self.position().subtract(p.position());
            if (toBoss.lengthSqr() > 1.0E-4D
                    && playerLook.dot(toBoss.normalize()) < GAZE_PLAYER_DOT) {
                continue;
            }
            if (p.hurt(level.damageSources().mobAttack(self), dmg)) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                // 一条从它眼睛到玩家的粒子线（批量）：天 → 地
                ParticleBatch pb = new ParticleBatch(level);
                for (int i = 0; i < 18; i++) {
                    double t = i / 18.0D;
                    pb.add(ParticleTypes.END_ROD,
                            eye.x + (p.getX() - eye.x) * t,
                            eye.y + (p.getY() + 1.0D - eye.y) * t,
                            eye.z + (p.getZ() - eye.z) * t);
                }
                pb.flush();
                p.displayClientMessage(Component.literal(
                        "\u00a74\u5b83\u770b\u7740\u4f60\u3002"), true);
            }
        }
        level.playSound(null, self.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    /**
     * <b>巨掌横扫</b>：台面上水平 ≤{@value #SWEEP_RANGE} 格、恶前方 120° 扇形内的玩家，
     * 伤害 + <b>台面击退</b>。
     *
     * <p>悬空几何（实机修复 A）：恶的"前方"取水平朝向（{@code flat(getLookAngle())}），
     * 扇形圆心用它在台面上的脚下投影 {@link EvilGazerEntity#groundCenter()}；
     * 命中再叠一层"在台面上"的高度判定，台面玩家一定能被打到。</p>
     */
    private static void sweepHit(EvilGazerEntity self, ServerLevel level) {
        Vec3 c = self.groundCenter();
        Vec3 look = flat(self.getLookAngle());
        double cosHalf = Math.cos(Math.toRadians(SWEEP_HALF_ANGLE));
        float dmg = dmg(self, SWEEP_MUL);
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            Vec3 to = new Vec3(p.getX() - c.x, 0.0D, p.getZ() - c.z);
            double dh = to.length();
            if (dh < 1.0E-4D || dh > SWEEP_RANGE) {
                continue;   // 水平距离（悬空 Boss 只按水平算）
            }
            Vec3 dir = to.normalize();
            if (dir.dot(look) < cosHalf) {
                continue;   // 不在前方扇形里
            }
            if (Math.abs(p.getY() - c.y) > PLATFORM_TOL) {
                continue;   // 不在台面上（例如从平台跳下去了）
            }
            if (p.hurt(level.damageSources().mobAttack(self), dmg)) {
                p.push(dir.x * 1.1D, 0.3D, dir.z * 1.1D);   // 台面击退（小上挑，不掀飞）
                p.hurtMarked = true;
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
        }
        level.playSound(null, self.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.4F, 0.7F);
        // 台面扇形尘爆（画在台面上，不是恶身上）
        ParticleBatch pb = new ParticleBatch(level);
        double yaw = Math.atan2(-look.x, look.z);
        for (int i = 0; i <= 24; i++) {
            double a = yaw - Math.toRadians(SWEEP_HALF_ANGLE)
                    + 2.0D * Math.toRadians(SWEEP_HALF_ANGLE) * i / 24.0D;
            for (double r = 2.0D; r <= SWEEP_RANGE; r += 2.0D) {
                pb.add(ParticleTypes.SWEEP_ATTACK,
                        c.x + Math.sin(a) * r, c.y + 0.4D, c.z + Math.cos(a) * r);
            }
        }
        pb.flush();
    }

    /**
     * <b>眼棱扫射</b>：从恶的眼睛指向玩家方向（<b>含向下俯角</b>）的 3D 光束，两束扫射。
     *
     * <p>悬空几何（实机修复 A）：以前用 {@code getLookAngle()} 的水平方向画一条贴地直线，
     * 恶悬在台面上方 13 格时这条线永远打不到台面玩家。现在：</p>
     * <ol>
     *   <li>方向 = <b>眼睛 → 目标</b>（或最近玩家）身体中心，自带向下角；</li>
     *   <li>只绕 Y 轴偏转 {@code yawOffset}（保留俯角），两束形成扫射；</li>
     *   <li>命中 = 玩家中心到这段 3D 线段的距离 ≤ {@value #BEAM_HALF_WIDTH}；</li>
     *   <li>长度截到台面（打在地上就停），不再穿地。</li>
     * </ol>
     */
    private static void beamHit(EvilGazerEntity self, ServerLevel level, float yawOffset) {
        Vec3 eye = eyePos(self);
        Vec3 dir = beamDirection(self, level, yawOffset);
        double len = beamGroundLength(self, eye, dir);
        // 光束粒子（天 → 地）
        ParticleBatch pb = new ParticleBatch(level);
        int seg = 68;
        for (int i = 0; i <= seg; i++) {
            double d = len * i / seg;
            pb.add(ParticleTypes.SOUL_FIRE_FLAME,
                    eye.x + dir.x * d, eye.y + dir.y * d, eye.z + dir.z * d);
        }
        pb.add(ParticleTypes.SONIC_BOOM, eye.x + dir.x * len,
                eye.y + dir.y * len, eye.z + dir.z * len);
        pb.flush();
        float dmg = dmg(self, BEAM_MUL);
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            Vec3 center = p.position().add(0.0D, 1.0D, 0.0D);
            if (distanceToSegment(center, eye, dir, len) > BEAM_HALF_WIDTH) {
                continue;   // 横向移出光束即可躲
            }
            if (p.hurt(level.damageSources().mobAttack(self), dmg)) {
                p.setSecondsOnFire(4);
            }
        }
        level.playSound(null, self.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, 2.4F, 0.5F);
    }

    /** <b>唤眼</b>：召 2~3 只小眼（爪牙模式；不递归、同场 ≤3）。 */
    private static void summonEyes(EvilGazerEntity self, ServerLevel level) {
        if (self.isMinion()) {
            return;   // 契约：小眼不能递归召唤
        }
        int want = 2 + self.getRandom().nextInt(2);   // 2~3
        int spawned = EvilGazerEntity.spawnMinions(level, self, want);
        if (spawned <= 0) {
            return;
        }
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.literal(
                    "\u00a7c\u5b83\u53ec\u6765\u4e86\u66f4\u591a\u7684\u773c\u775b\u3002"), false);
        }
        level.playSound(null, self.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    /**
     * <b>裂口重压</b>：以恶的<b>台面投影</b>为圆心、半径 {@value #SLAM_RADIUS} 的地面环冲击，
     * 伤害 + 强击退。
     *
     * <p>悬空几何（实机修复 A）：圆心不再是恶的身体位置（那在半空），
     * 而是脚下投影 {@link EvilGazerEntity#groundCenter()}；
     * 命中 = 玩家在台面上（|脚底 − 台面| ≤ {@value #PLATFORM_TOL}）且水平 ≤26。</p>
     */
    private static void slamHit(EvilGazerEntity self, ServerLevel level) {
        Vec3 c = self.groundCenter();
        float dmg = dmg(self, SLAM_MUL);
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            if (Math.hypot(p.getX() - c.x, p.getZ() - c.z) > SLAM_RADIUS) {
                continue;   // 水平距离（台面地面环）
            }
            if (!onPlatform(self, p)) {
                continue;   // 不在台面上（跳下平台 / 飞走）
            }
            if (p.hurt(level.damageSources().mobAttack(self), dmg)) {
                Vec3 dir = new Vec3(p.getX() - c.x, 0.0D, p.getZ() - c.z);
                if (dir.lengthSqr() > 1.0E-4D) {
                    dir = dir.normalize().scale(1.4D);
                    p.push(dir.x, 0.9D, dir.z);
                    p.hurtMarked = true;
                }
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
        }
        CombatSounds.bossSlam(level, c);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                c.x, c.y + 0.4D, c.z, 4, 2.5D, 0.5D, 2.5D, 0.0D);
        ParticleBatch pb = new ParticleBatch(level);
        // 天 → 地：从恶身上直落台面的冲击柱
        for (int i = 0; i <= 12; i++) {
            double t = i / 12.0D;
            pb.add(ParticleTypes.SOUL_FIRE_FLAME,
                    self.getX() + (c.x - self.getX()) * t,
                    self.getY() + (c.y - self.getY()) * t,
                    self.getZ() + (c.z - self.getZ()) * t);
        }
        // 台面三圈冲击环
        for (int ring = 0; ring < 3; ring++) {
            double r = SLAM_RADIUS * (0.35D + ring * 0.3D);
            int n = 72;
            for (int i = 0; i < n; i++) {
                double a = 2.0D * Math.PI * i / n;
                pb.add(ParticleTypes.SOUL,
                        c.x + Math.cos(a) * r, c.y + 0.3D + ring * 0.6D,
                        c.z + Math.sin(a) * r);
            }
        }
        pb.flush();
        level.playSound(null, self.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 3.0F, 0.4F);
    }

    /**
     * <b>天崩</b>：前摇期间标好 3 个落点，判定帧开始按波次砸下（每 {@value #METEOR_WAVE_GAP}t 一波）。
     *
     * <p>落点在前摇（35t）就画在地上了，玩家走过去就躲开 —— 这是契约里的"走位躲"。</p>
     */
    private static void meteorLaunch(EvilGazerEntity self, ServerLevel level) {
        if (self.meteorSpotCount <= 0) {
            pickMeteorSpots(self, level);
        }
        for (int w = 0; w < self.meteorSpotCount; w++) {
            final Vec3 spot = self.meteorSpots[w];
            final int wave = w;
            StunScheduler.after(level, wave * METEOR_WAVE_GAP,
                    () -> meteorImpact(level, self, spot, wave));
        }
    }

    /** 一波陨石落地（<b>从天空砸向台面</b>，悬空几何：落点已是台面高度）。 */
    private static void meteorImpact(ServerLevel level, EvilGazerEntity self, Vec3 spot,
                                      int wave) {
        try {
            level.sendParticles(ParticleTypes.FLAME, spot.x, spot.y + 1.0D, spot.z,
                    40, 1.4D, 1.0D, 1.4D, 0.08D);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    spot.x, spot.y + 0.6D, spot.z, 2, 0.8D, 0.3D, 0.8D, 0.0D);
            level.playSound(null, net.minecraft.core.BlockPos.containing(spot),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.6F);
            // 天空拖尾（"从天上砸下来"）：从台面上方 40 格一路连到落点
            ParticleBatch pb = new ParticleBatch(level);
            for (int i = 0; i < 24; i++) {
                pb.add(ParticleTypes.SOUL_FIRE_FLAME,
                        spot.x, spot.y + 2.0D + i * 1.6D, spot.z);
            }
            pb.flush();
            float dmg = dmg(self, METEOR_MUL);
            for (ServerPlayer p : level.players()) {
                if (p.isCreative() || p.isSpectator()) {
                    continue;
                }
                if (Math.hypot(p.getX() - spot.x, p.getZ() - spot.z) > METEOR_RADIUS) {
                    continue;
                }
                if (!onPlatform(self, p)) {
                    continue;   // 不在台面上
                }
                if (p.hurt(level.damageSources().mobAttack(self), dmg)) {
                    p.setSecondsOnFire(3);
                }
            }
        } catch (Throwable ignored) {
            // 单波失败不影响后续波次
        }
    }

    /** 狂暴：HP&lt;15% 一次 —— 攻击 +30%、冷却 ×0.6、全程血红粒子。 */
    private static void applyEnrage(EvilGazerEntity self, ServerLevel level) {
        if (self.enraged()) {
            return;
        }
        self.enraged = true;
        AttributeInstance atk = self.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && atk.getModifier(ENRAGE_ATK_ID) == null) {
            atk.addPermanentModifier(new AttributeModifier(ENRAGE_ATK_ID, "evil_enrage_atk",
                    ENRAGE_ATK_BONUS, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        Vec3 at = self.position();
        level.playSound(null, self.blockPosition(), SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE, 3.0F, 0.45F);
        level.playSound(null, self.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 2.2F, 0.6F);
        level.sendParticles(ParticleTypes.LAVA, at.x,
                at.y + self.getBbHeight() * 0.5D, at.z, 60, 2.0D, 1.6D, 2.0D, 0.02D);
        level.sendParticles(ParticleTypes.FLAME, at.x,
                at.y + self.getBbHeight() * 0.5D, at.z, 120, 2.4D, 2.0D, 2.4D, 0.06D);
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.literal(
                    "\u00a74\u00a7l\u5b83\u5f7b\u5e95\u72c2\u66b4\u4e86\u3002"), false);
        }
    }

    // ==================================================================
    // 6. 前摇预警（telegraph）
    // ==================================================================

    /**
     * 前摇预警：按"<b>从天空打向地面</b>"重画（实机修复 A，契约 §4.3）。
     *
     * <ul>
     *   <li>GAZE：眼睛 → 玩家的 3D 锁定线（天→地）+ 玩家脚下落点圈；</li>
     *   <li>SWEEP：<b>台面</b>上的 120° 扇形（圆心 = 恶的脚下投影）；</li>
     *   <li>BEAM：真实的 <b>3D 向下斜线</b>（和判定同一条线）+ 台面落点圈；</li>
     *   <li>SUMMON：<b>台面</b>上的三个召唤圈；</li>
     *   <li>SLAM：<b>台面</b>上的半径 26 地面预警环 + 天→地柱；</li>
     *   <li>METEOR：<b>台面</b>落点圈 + 从天空到落点的落线；</li>
     *   <li>ENRAGE：台面红环 + 恶身上火焰。</li>
     * </ul>
     */
    private static void telegraph(EvilGazerEntity self, ServerLevel level, Skill skill,
                                  float progress) {
        Vec3 c = self.groundCenter();    // 台面圆心（恶的脚下投影）
        Vec3 at = self.position();       // 恶的身体（半空）
        double floor = self.hoverFloorY();
        ParticleBatch pb = new ParticleBatch(level);
        switch (skill) {
            case GAZE -> {
                // 锁定线：从它的眼睛（半空）到当前被锁定的玩家（台面）
                Vec3 eye = eyePos(self);
                ServerPlayer lock = lockTarget(self, level);
                if (lock != null) {
                    Vec3 to = lock.position().add(0.0D, 1.0D, 0.0D).subtract(eye);
                    for (int i = 0; i <= 24; i++) {
                        double t = i / 24.0D;
                        pb.add(ParticleTypes.END_ROD,
                                eye.x + to.x * t, eye.y + to.y * t, eye.z + to.z * t);
                    }
                    // 玩家脚下落点圈（告诉他要动）
                    for (int i = 0; i < 16; i++) {
                        double a = 2.0D * Math.PI * i / 16.0D;
                        pb.add(ParticleTypes.END_ROD,
                                lock.getX() + Math.cos(a) * 1.6D, floor + 0.2D,
                                lock.getZ() + Math.sin(a) * 1.6D);
                    }
                }
                pb.add(ParticleTypes.SONIC_BOOM, eye.x, eye.y, eye.z);
            }
            case SWEEP -> {
                // 台面 120° 扇形（圆心 = 脚下投影，不是半空的身体位置）
                Vec3 look = flat(self.getLookAngle());
                double yaw = Math.atan2(-look.x, look.z);
                for (int i = 0; i <= 20; i++) {
                    double a = yaw - Math.toRadians(SWEEP_HALF_ANGLE)
                            + 2.0D * Math.toRadians(SWEEP_HALF_ANGLE) * i / 20.0D;
                    double r = SWEEP_RANGE * (0.35D + 0.65D * progress);
                    pb.add(ParticleTypes.SMOKE,
                            c.x + Math.sin(a) * r, c.y + 0.12D, c.z + Math.cos(a) * r);
                }
                // 天 → 地：手臂从半空扫下来的落点
                pb.add(ParticleTypes.CLOUD, at.x, at.y, at.z);
            }
            case BEAM -> {
                // 和判定**同一条** 3D 向下斜线（所见即所挨）
                Vec3 eye = eyePos(self);
                Vec3 dir = beamDirection(self, level, 0.0F);
                double len = beamGroundLength(self, eye, dir);
                for (int i = 0; i <= 40; i++) {
                    double d = len * i / 40.0D;
                    pb.add(ParticleTypes.SOUL_FIRE_FLAME,
                            eye.x + dir.x * d, eye.y + dir.y * d, eye.z + dir.z * d);
                }
                // 台面落点圈
                double gx = eye.x + dir.x * len;
                double gz = eye.z + dir.z * len;
                for (int i = 0; i < 20; i++) {
                    double a = 2.0D * Math.PI * i / 20.0D;
                    pb.add(ParticleTypes.SOUL_FIRE_FLAME,
                            gx + Math.cos(a) * BEAM_HALF_WIDTH, floor + 0.2D,
                            gz + Math.sin(a) * BEAM_HALF_WIDTH);
                }
                pb.add(ParticleTypes.SONIC_BOOM, eye.x, eye.y, eye.z);
            }
            case SUMMON -> {
                // 台面上的三个召唤圈（小眼就刷在这）
                for (int k = 0; k < 3; k++) {
                    double a = 2.0D * Math.PI * k / 3.0D;
                    double cx = c.x + Math.cos(a) * 8.0D;
                    double cz = c.z + Math.sin(a) * 8.0D;
                    for (int i = 0; i < 16; i++) {
                        double b = 2.0D * Math.PI * i / 16.0D;
                        pb.add(ParticleTypes.PORTAL,
                                cx + Math.cos(b) * 2.0D, floor + 0.2D,
                                cz + Math.sin(b) * 2.0D);
                    }
                }
            }
            case SLAM -> {
                // 台面地面预警环（半径 26，两圈）+ 天→地柱
                for (int ring = 0; ring < 2; ring++) {
                    double r = SLAM_RADIUS * (0.5D + 0.5D * ring);
                    int n = 64;
                    for (int i = 0; i < n; i++) {
                        double a = 2.0D * Math.PI * i / n;
                        pb.add(ParticleTypes.SOUL_FIRE_FLAME,
                                c.x + Math.cos(a) * r, floor + 0.15D,
                                c.z + Math.sin(a) * r);
                    }
                }
                for (int i = 0; i <= 10; i++) {
                    double t = i / 10.0D;
                    pb.add(ParticleTypes.SOUL,
                            at.x + (c.x - at.x) * t, at.y + (c.y - at.y) * t,
                            at.z + (c.z - at.z) * t);
                }
            }
            case METEOR -> {
                // 台面落点圈 + 从天空到落点的落线
                for (int w = 0; w < self.meteorSpotCount; w++) {
                    Vec3 spot = self.meteorSpots[w];
                    for (int i = 0; i < 20; i++) {
                        double a = 2.0D * Math.PI * i / 20.0D;
                        pb.add(ParticleTypes.FLAME,
                                spot.x + Math.cos(a) * METEOR_RADIUS, floor + 0.2D,
                                spot.z + Math.sin(a) * METEOR_RADIUS);
                    }
                    for (int i = 1; i <= 10; i++) {
                        pb.add(ParticleTypes.SOUL_FIRE_FLAME,
                                spot.x, floor + 2.0D + i * 1.8D, spot.z);
                    }
                }
            }
            case ENRAGE -> {
                // 狂暴不变：台面红环 + 恶身上火焰
                for (int i = 0; i < 24; i++) {
                    double a = 2.0D * Math.PI * i / 24.0D;
                    pb.add(ParticleTypes.LAVA,
                            c.x + Math.cos(a) * 4.0D, floor + 0.4D,
                            c.z + Math.sin(a) * 4.0D);
                }
                pb.add(ParticleTypes.FLAME, at.x, at.y + self.getBbHeight() * 0.3D, at.z);
            }
            default -> {
                // 不会到这里
            }
        }
        pb.flush();
    }

    // ==================================================================
    // 7. 工具
    // ==================================================================

    /** 本阶段可用的攻击池（小眼只有弱化凝视）。 */
    private static Skill[] poolFor(EvilGazerEntity self) {
        if (self.isMinion()) {
            return POOL_MINION;
        }
        return switch (self.phase()) {
            case 3 -> POOL_P3;
            case 2 -> POOL_P2;
            default -> POOL_P1;
        };
    }

    /** 招式 id → 枚举（0/越界 = null）。 */
    public static Skill skillOf(int id) {
        int i = id - 1;
        Skill[] all = Skill.values();
        return (i >= 0 && i < all.length) ? all[i] : null;
    }

    /** 出手间隔（小眼更慢）。 */
    public static int cooldownFor(EvilGazerEntity self) {
        if (self.isMinion()) {
            return MINION_COOLDOWN;
        }
        return switch (self.phase()) {
            case 3 -> COOLDOWN_PHASE3;
            case 2 -> COOLDOWN_PHASE2;
            default -> COOLDOWN_PHASE1;
        };
    }

    private static int windupFor(EvilGazerEntity self, Skill skill) {
        return self.isMinion() ? MINION_WINDUP : skill.windup;
    }

    private static int activeFor(EvilGazerEntity self, Skill skill) {
        return self.isMinion() ? MINION_ACTIVE : skill.active;
    }

    private static int recoverFor(EvilGazerEntity self, Skill skill) {
        return self.isMinion() ? MINION_RECOVER : skill.recover;
    }

    /** 伤害 = 攻击力 × 倍率（小眼 ×{@value #MINION_GAZE_MUL}）。全部走 mobAttack 结算。 */
    private static float dmg(EvilGazerEntity self, double mul) {
        double base = self.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (self.isMinion()) {
            mul *= MINION_GAZE_MUL;
        }
        return (float) (base * mul);
    }

    private static double hpFraction(EvilGazerEntity self) {
        return self.getHealth() / Math.max(1.0F, self.getMaxHealth());
    }

    /** 眼睛位置（模型 70% 高度处）。 */
    private static Vec3 eyePos(EvilGazerEntity self) {
        return self.position().add(0.0D, self.getBbHeight() * 0.7D, 0.0D);
    }

    /** 水平化（去掉 y 分量）。 */
    private static Vec3 flat(Vec3 v) {
        Vec3 out = new Vec3(v.x, 0.0D, v.z);
        return out.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : out.normalize();
    }

    // ------------------------------------------------------------------
    // 悬空几何工具（实机修复 A：水平距离 / 台面高度基准）
    // ------------------------------------------------------------------

    /** 到某点的<b>水平</b>距离（忽略恶与玩家之间的高度差）。 */
    private static double horizontalDistance(EvilGazerEntity self, Vec3 pos) {
        return Math.hypot(pos.x - self.getX(), pos.z - self.getZ());
    }

    /** 玩家是否站在<b>台面</b>上（|脚底 − 台面| ≤ {@value #PLATFORM_TOL}）。 */
    private static boolean onPlatform(EvilGazerEntity self, ServerPlayer p) {
        return Math.abs(p.getY() - self.hoverFloorY()) <= PLATFORM_TOL;
    }

    /** 水平距离最近（≤ {@code rangeH}）的生存玩家；没有返回 null。 */
    private static ServerPlayer nearestPlayer(EvilGazerEntity self, ServerLevel level,
                                              double rangeH) {
        ServerPlayer best = null;
        double bestH2 = rangeH * rangeH;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) {
                continue;
            }
            double dx = p.getX() - self.getX();
            double dz = p.getZ() - self.getZ();
            double h2 = dx * dx + dz * dz;
            if (h2 <= bestH2) {
                bestH2 = h2;
                best = p;
            }
        }
        return best;
    }

    /** 水平范围里有没有生存玩家（悬空 Boss 的"附近有没有人"判定）。 */
    private static boolean hasNearbyPlayer(EvilGazerEntity self, ServerLevel level,
                                           double rangeH) {
        return nearestPlayer(self, level, rangeH) != null;
    }

    /** 眼棱瞄准点：当前目标 → 水平最近玩家 → 正下方（兜底）。 */
    private static Vec3 aimPoint(EvilGazerEntity self, ServerLevel level) {
        var target = self.getTarget();
        if (target != null && target.isAlive()) {
            return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        }
        ServerPlayer p = nearestPlayer(self, level, GAZE_RANGE);
        if (p != null) {
            return p.position().add(0.0D, 1.0D, 0.0D);
        }
        return self.position().add(0.0D, -self.getBbHeight(), 0.0D);
    }

    /**
     * 一束眼棱的方向：<b>眼睛 → 瞄准点</b>（自带向下俯角），只绕 Y 轴偏转 {@code yawOffset}。
     *
     * <p>悬空修复的关键：以前方向只有水平分量，恶在半空就永远打不到台面；
     * 现在保留俯角，波束从天空斜插到台面。</p>
     */
    private static Vec3 beamDirection(EvilGazerEntity self, ServerLevel level,
                                      float yawOffset) {
        Vec3 eye = eyePos(self);
        Vec3 d = aimPoint(self, level).subtract(eye);
        if (d.lengthSqr() < 1.0E-4D) {
            d = new Vec3(0.0D, -1.0D, 0.0D);
        }
        d = d.normalize();
        double yaw = Math.atan2(-d.x, d.z) + Math.toRadians(yawOffset);
        double pitch = Math.asin(-d.y);     // 向下为正
        double cosP = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosP, -Math.sin(pitch), Math.cos(yaw) * cosP);
    }

    /** 光束长度：向下打时截到台面（打在地上就停，不穿地）。 */
    private static double beamGroundLength(EvilGazerEntity self, Vec3 eye, Vec3 dir) {
        double len = BEAM_LENGTH;
        if (dir.y < -1.0E-3D) {
            double toGround = (self.hoverFloorY() + 0.2D - eye.y) / dir.y;
            if (toGround > 0.0D) {
                len = Math.min(len, toGround);
            }
        }
        return Math.max(1.0D, len);
    }

    /** 点到线段（起点 origin、方向 dir 归一化、长 len）的距离。 */
    private static double distanceToSegment(Vec3 point, Vec3 origin, Vec3 dir, double len) {
        Vec3 to = point.subtract(origin);
        double proj = to.dot(dir);
        if (proj <= 0.0D) {
            return to.length();
        }
        if (proj >= len) {
            return point.subtract(origin.add(dir.scale(len))).length();
        }
        return to.subtract(dir.scale(proj)).length();
    }

    /** 凝视前摇时锁定谁（它在"看"的最近玩家，水平 ≤{@value #GAZE_RANGE}）。 */
    private static ServerPlayer lockTarget(EvilGazerEntity self, ServerLevel level) {
        Vec3 eye = eyePos(self);
        Vec3 look = self.getLookAngle();
        ServerPlayer best = null;
        double bestScore = -1.0E9D;
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            if (horizontalDistance(self, p.position()) > GAZE_RANGE) {
                continue;   // 水平距离（悬空 Boss 不能按 3D 算）
            }
            Vec3 to = p.position().add(0.0D, 1.0D, 0.0D).subtract(eye);
            if (to.lengthSqr() < 1.0E-4D) {
                continue;
            }
            double score = to.normalize().dot(look) * 100.0D
                    - horizontalDistance(self, p.position()) * 0.1D;
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    /** 天崩：给 3 波各挑一个<b>台面</b>落点（优先玩家脚下附近，逼他们走位）。 */
    private static void pickMeteorSpots(EvilGazerEntity self, ServerLevel level) {
        double floor = self.hoverFloorY();   // 悬空几何：落点一律钉在台面
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isCreative() && !p.isSpectator()
                    && horizontalDistance(self, p.position()) <= 64.0D) {
                players.add(p);
            }
        }
        int n = Math.min(METEOR_WAVES, Math.max(1, players.isEmpty() ? 1 : players.size()));
        self.meteorSpotCount = n;
        for (int w = 0; w < METEOR_WAVES; w++) {
            double sx;
            double sz;
            if (!players.isEmpty()) {
                ServerPlayer p = players.get(w % players.size());
                double off = 2.0D + self.getRandom().nextDouble() * 2.0D;
                double a = self.getRandom().nextDouble() * Math.PI * 2.0D;
                sx = p.getX() + Math.cos(a) * off;
                sz = p.getZ() + Math.sin(a) * off;
            } else {
                Vec3 g = self.groundCenter();
                double a = self.getRandom().nextDouble() * Math.PI * 2.0D;
                double r = 8.0D + self.getRandom().nextDouble() * 12.0D;
                sx = g.x + Math.cos(a) * r;
                sz = g.z + Math.sin(a) * r;
            }
            // Y 钉在台面：陨石是"从天空砸向台面"，不是砸在半空中的恶身上
            self.meteorSpots[w] = new Vec3(sx, floor, sz);
        }
    }

    /** 前摇音（不同招式不同音色）。 */
    private static SoundEvent windupSound(Skill skill) {
        return switch (skill) {
            case GAZE -> SoundEvents.WARDEN_HEARTBEAT;
            case SWEEP -> SoundEvents.EVOKER_CAST_SPELL;
            case BEAM -> SoundEvents.BEACON_ACTIVATE;
            case SUMMON -> SoundEvents.ELDER_GUARDIAN_CURSE;
            case SLAM -> SoundEvents.WARDEN_SONIC_BOOM;
            case METEOR -> SoundEvents.ENDER_DRAGON_GROWL;
            case ENRAGE -> SoundEvents.RAVAGER_ROAR;
        };
    }

    private static float windupPitch(Skill skill) {
        return switch (skill) {
            case GAZE -> 0.6F;
            case SWEEP -> 0.8F;
            case BEAM -> 0.5F;
            case SUMMON -> 0.7F;
            case SLAM -> 0.45F;
            case METEOR -> 0.55F;
            case ENRAGE -> 0.5F;
        };
    }
}
