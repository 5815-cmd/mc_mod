package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.combat.CombatSounds;
import com.hongmeng.xiuxian.realworld.EvilSky;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * <b>恶</b>：从天空裂口伸进来的巨大存在。
 *
 * <h2>三阶段（对应玩家要求）</h2>
 * <ul>
 *   <li><b>一阶段</b>：只伸进<b>一条手臂</b>；</li>
 *   <li><b>二阶段</b>：<b>头</b>也伸进来；</li>
 *   <li><b>三阶段</b>：<b>整个上半身</b>进来。</li>
 * </ul>
 *
 * <h2>战斗（第五轮重写，契约 §2）</h2>
 * <ul>
 *   <li>技能由 {@link EvilAbilities} 的 <b>IDLE→WINDUP→ACTIVE→RECOVER</b> 状态机驱动，
 *       7 招全部有可躲前摇；</li>
 *   <li><b>阶段转场</b>：血量 66% / 33% 各触发一次（{@link #beginPhaseTransition}）——
 *       {@value #TRANSITION_TICKS}t 无敌（≥40t 契约）+ 冲击波 + 天空阶段提升 +
 *       标题/字幕 + 技能停转；</li>
 *   <li><b>阶段锁血</b>：一阶段打不过 66%、二阶段打不过 33%；<b>单次受击上限</b> 8%；
 *       血量见底 → {@link #startEscape()} 逃走（{@code EvilSummon.endEscaped} 收尾）；</li>
 *   <li><b>同步</b>：招式/状态写 {@code setBossAttack(byte)/setBossState(byte)}（映射见下）。</li>
 * </ul>
 *
 * <h2>小眼（爪牙模式，契约 §2 的小眼修复）</h2>
 * <p>以前唤眼直接刷 3 只 {@code OVERLORD} 品阶的自己 → 多 3 条 BOSS 条、还能递归召唤。
 * 现在小眼走<b>爪牙模式</b>：持久化标记 {@value #MINION_TAG}、品阶降为 {@code ELITE}
 * （不显示 BOSS 条）、血量 ≤ {@value #MINION_MAX_HEALTH}、只会「弱化凝视」、
 * 不递归、不参与阶段/EvilSky、Boss 逃走或死亡时一并清除。</p>
 *
 * <h2>同步映射（给客户端镜头/特效，契约 §2.4）</h2>
 * <table border="1">
 *   <caption>setBossState / setBossAttack</caption>
 *   <tr><th>setBossState</th><th>含义</th></tr>
 *   <tr><td>0</td><td>IDLE</td></tr>
 *   <tr><td>1</td><td>WINDUP（前摇，客户端可以开始震屏/预警）</td></tr>
 *   <tr><td>2</td><td>ACTIVE（判定，命中瞬间）</td></tr>
 *   <tr><td>3</td><td>RECOVER（后摇）/ 阶段转场中</td></tr>
 * </table>
 * <table border="1">
 *   <tr><th>setBossAttack</th><th>招式</th></tr>
 *   <tr><td>0</td><td>无</td></tr>
 *   <tr><td>1</td><td>凝视 GAZE</td></tr>
 *   <tr><td>2</td><td>巨掌横扫 SWEEP</td></tr>
 *   <tr><td>3</td><td>眼棱扫射 BEAM</td></tr>
 *   <tr><td>4</td><td>唤眼 SUMMON</td></tr>
 *   <tr><td>5</td><td>裂口重压 SLAM</td></tr>
 *   <tr><td>6</td><td>天崩 METEOR</td></tr>
 *   <tr><td>7</td><td>狂暴 ENRAGE</td></tr>
 *   <tr><td>8</td><td>阶段转场冲击波（非技能，信号用）</td></tr>
 * </table>
 *
 * <h2>悬空几何（实机修复 A，契约 {@code 工程/19-恶实机修复契约.md} §1/§4）</h2>
 * <ul>
 *   <li><b>永不受重力</b>（构造器 + 每 tick {@code setNoGravity(true)}）；
 *       召唤组可用 {@link #setHoverAnchor(cx, anchorY, cz)} 把恶钉在悬停点
 *       （{@code anchorY} = {@code 台面 + HOVER_ABOVE(13.0)}，即 EvilSummon 的 {@code appearToY}；
 *       手里只有台面高度时用 {@link #setHoverAnchorOnFloor(double, double, double)}）；</li>
 *   <li><b>不移动</b>：tick 里 X/Z 固定、Y 夹回锚点、速度清零（防击退/爆炸/水流推动），
 *       降临（{@code evil_appearing}）期间不锁，让 EvilSummon 把它压下来；</li>
 *   <li><b>目标获取</b>：每 {@value #TARGET_SCAN_INTERVAL}t 用<b>水平距离</b>取 ≤{@value #TARGET_RANGE_H} 格的
 *       最近生存玩家（原版 3D 目标 AI 会把台面玩家判成"够不到"）；</li>
 *   <li><b>几何基准</b>：{@link #groundCenter()} = 恶在台面上的脚下投影，
 *       技能判定与地面预警都用它；{@link #hoverFloorY()} = 台面高度。</li>
 * </ul>
 */
public class EvilGazerEntity extends CultivationMonsterEntity {

    /** 逃走总时长（tick）。 */
    public static final int ESCAPE_TOTAL = 45;

    /** 单次受击上限（占最大生命的比例）—— 终极 boss 不许被一刀秒。 */
    public static final float MAX_HIT_RATIO = 0.08F;

    /** 阶段锁血阈值：一阶段 66%、二阶段 33%。 */
    public static final float PHASE2_LOCK = 0.66F;
    public static final float PHASE3_LOCK = 0.33F;

    /** 小眼/爪牙的持久化标记。 */
    public static final String MINION_TAG = "evil_minion";
    /** 小眼血量上限（契约：≤60）。 */
    public static final float MINION_MAX_HEALTH = 60.0F;
    /** 小眼攻击力（基础值，弱化凝视再乘倍率）。 */
    public static final double MINION_ATTACK = 30.0D;
    /** 同场小眼上限。 */
    public static final int MINION_CAP = 3;

    /** 阶段转场总时长（契约要求无敌 ≥40t）。 */
    public static final int TRANSITION_TICKS = 60;
    /** 转场冲击波发生在第几 tick（0 基）。 */
    public static final int TRANSITION_SHOCK_TICK = 20;
    /** 转场冲击波半径。 */
    public static final double TRANSITION_SHOCK_RADIUS = 30.0D;
    /** 同步信号：阶段转场冲击波（非技能 id，从 8 起）。 */
    public static final byte SYNC_ATTACK_TRANSITION = 8;

    // ------------------------------------------------------------------
    // 悬空几何（实机修复 A，契约 `工程/19-恶实机修复契约.md` §1/§4）
    // ------------------------------------------------------------------

    /**
     * 悬停高度：锚点悬在<b>台面</b>上方的格数（契约 §1 的 {@code HOVER_ABOVE}）。
     *
     * <p>为什么是 13：一阶段手臂模型向下最长 157px × 渲染倍率 1.35 ≈ 13.25 格，
     * 锚点悬空 13 格 → 指尖正好垂到台面附近（"撑在/扫过台面"），不会插进平台；
     * 二/三阶段换成头/上身模型（向下约 6.8 格）仍悬在台面上方，依旧巨大。</p>
     */
    public static final double HOVER_ABOVE = 13.0D;
    /** 目标获取间隔（tick）。 */
    public static final int TARGET_SCAN_INTERVAL = 10;
    /** 目标获取：水平距离上限（格）。 */
    public static final double TARGET_RANGE_H = 80.0D;
    /** 台面判定容差（格）：玩家脚底与台面的高度差在此范围内算"站在台面上"。 */
    public static final double PLATFORM_TOLERANCE = 4.0D;

    /** 跳过 MobScaling 的开场难度的标记（小眼用：属性已经是钳好的固定值）。 */
    private static final String SCALED_TAG = "hongmeng_scaled";
    private static final String BOSS_SCALED_TAG = "hongmeng_boss_scaled";

    /** 当前阶段 1~3。 */
    private int evilPhase = 1;

    /** 是否正在逃走（致命伤害被拦截后进入）。 */
    private boolean escaping = false;

    /** 逃走的剩余 tick。 */
    private int escapeTicks = 0;

    /** 小眼/爪牙模式（持久化标记的本地缓存）。 */
    private boolean minion = false;

    /** 阶段转场剩余 tick（>0 = 转场中，技能停转、无敌）。 */
    private int transitionTicks = 0;
    /** 转场目标阶段（2/3）。 */
    private int transitionTo = 2;

    // ------------------------------------------------------------------
    // 悬停锚点（实机修复 A）
    // ------------------------------------------------------------------

    /** 悬停锚点是否已锁定（召唤组调过 setHoverAnchor，或首次 tick 自动捕获）。 */
    private boolean hoverLocked = false;
    /** 锚点 X（固定，不移动）。 */
    private double hoverX;
    /** 锚点 Z（固定，不移动）。 */
    private double hoverZ;
    /** 锚点 Y = 台面 + {@value #HOVER_ABOVE}。 */
    private double hoverY;
    /** 台面 Y（技能的地面基准；等于玩家站立的脚底高度）。 */
    private double floorY;

    // ------------------------------------------------------------------
    // 战斗状态机字段（由 EvilAbilities 读写；包内可见，不对外暴露）
    // ------------------------------------------------------------------

    /** 状态：0 IDLE / 1 WINDUP / 2 ACTIVE / 3 RECOVER。 */
    int state = EvilAbilities.STATE_IDLE;
    /** 当前招式 id（0 = 无，1..7 见 {@link EvilAbilities.Skill}）。 */
    int attack = 0;
    /** 当前阶段的剩余/经过 tick。 */
    int stageTicks = 0;
    /** ACTIVE 段已经过的 tick。 */
    int elapsed = 0;
    /** IDLE 冷却。 */
    int cooldown = 0;
    /** 上一招（避免连续重复）。 */
    int lastAttack = 0;
    /** 狂暴是否已触发（HP<15% 一次）。 */
    boolean enraged = false;
    /** 天崩 METEOR 的预定点（前摇期间标好，判定帧砸）。 */
    final Vec3[] meteorSpots = new Vec3[]{Vec3.ZERO, Vec3.ZERO, Vec3.ZERO};
    /** 天崩预定点数量。 */
    int meteorSpotCount = 0;

    public EvilGazerEntity(EntityType<? extends EvilGazerEntity> type, Level level) {
        super(type, level, 26);
        // 悬空 Boss：永不受重力（契约 §4.1）；位置由 tick 里的 hover 锁定负责
        this.setNoGravity(true);
        // 初始阶段同步（客户端第一帧就按阶段 1 画手臂模型）
        this.setBossPhase(1);
    }

    /**
     * 命中箱整体下移量（格）。「恶」悬停在台面 +13 格、模型从锚点向下垂 6~13 格，
     * 普通"以脚为底"的包围盒会整个落在模型上方（玩家打不到垂下来的手臂/上身）。
     * 这里把盒子整体下压 9 格，覆盖悬空模型的中下段。
     */
    public static final double HITBOX_DROP = 9.0D;

    @Override
    protected net.minecraft.world.phys.AABB makeBoundingBox() {
        return super.makeBoundingBox().move(0.0D, -HITBOX_DROP, 0.0D);
    }

    @Override
    public MobRank rank() {
        // 小眼是爪牙：ELITE 品阶 → hasBossBar()=false，不会多出 BOSS 条
        return this.isMinion() ? MobRank.ELITE : MobRank.OVERLORD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        // 世界终极 boss（血量按玩家反馈下调：680 -> 420，仍是全场最高）
        return scaledAttributes(MobRank.OVERLORD, 420.0D, 26.0D, 1.0D, 120.0D, 18.0D);
    }

    public int phase() {
        return evilPhase;
    }

    /**
     * 切阶段：模型（客户端读 {@link #phase()}）+ 天空阶段（{@link EvilSky}）+ 播报。
     *
     * <p>小眼没有阶段：直接 return（它们不能动天幕）。</p>
     */
    public void setPhase(int p) {
        if (this.isMinion()) {
            return;
        }
        int np = Math.max(1, Math.min(3, p));
        if (np == this.evilPhase) {
            return;
        }
        this.evilPhase = np;
        // 同步给客户端（渲染/天空/压迫感都读 bossPhase()）：
        // 第五轮起 BossPhase.tick 对「恶」早退，客户端阶段同步由这里负责
        this.setBossPhase(np);
        EvilSky.set(EvilSky.strength(), EvilSky.openTicks(), np);
        if (this.level() instanceof ServerLevel sl) {
            String msg = switch (np) {
                case 2 -> "\u00a74它把头探了进来。";
                case 3 -> "\u00a74\u00a7l它整个上半身挤进了这片天空。";
                default -> "";
            };
            for (var p2 : sl.players()) {
                if (!msg.isEmpty()) {
                    p2.displayClientMessage(Component.literal(msg), false);
                }
                try {
                    com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(p2,
                            new com.hongmeng.xiuxian.network.EvilSkyPacket(
                                    EvilSky.strength(), EvilSky.openTicks(), np));
                } catch (Throwable ignored) {
                    // 忽略
                }
            }
        }
    }

    /** 是否在逃走。 */
    public boolean escaping() {
        return escaping;
    }

    /** 是否小眼/爪牙。 */
    public boolean isMinion() {
        if (!this.minion && !this.level().isClientSide()) {
            this.minion = this.getPersistentData().getBoolean(MINION_TAG);
        }
        return this.minion;
    }

    /** 标记为小眼/爪牙（持久化，区块重载后仍是小眼）。 */
    public void setMinion(boolean value) {
        this.minion = value;
        this.getPersistentData().putBoolean(MINION_TAG, value);
    }

    /** 是否在阶段转场（无敌 + 技能停转）。 */
    public boolean transitioning() {
        return this.transitionTicks > 0;
    }

    /** 转场剩余 tick（0 = 不在转场）。 */
    public int transitionTicks() {
        return this.transitionTicks;
    }

    /** 是否已狂暴。 */
    public boolean enraged() {
        return this.enraged;
    }

    // ==================================================================
    // 悬停几何（实机修复 A，契约 §1/§4.1）
    // ==================================================================

    /** 是否已锁定悬停锚点。 */
    public boolean hoverLocked() {
        return this.hoverLocked;
    }

    /** 悬停锚点 Y（= 台面 + {@value #HOVER_ABOVE}）；未锁定时退化为当前 Y。 */
    public double hoverAnchorY() {
        return this.hoverLocked ? this.hoverY : this.getY();
    }

    /** 台面 Y（玩家站立脚底高度）；未锁定时退化为"当前 Y - {@value #HOVER_ABOVE}"。 */
    public double hoverFloorY() {
        return this.hoverLocked ? this.floorY : this.getY() - HOVER_ABOVE;
    }

    /** 悬停锚点（中心）。 */
    public Vec3 hoverAnchor() {
        return new Vec3(this.hoverLocked ? this.hoverX : this.getX(),
                hoverAnchorY(),
                this.hoverLocked ? this.hoverZ : this.getZ());
    }

    /**
     * 恶在<b>台面上的脚下投影</b>：所有地面判定（重压圆心、地面预警环、陨石落点）都以它为准。
     *
     * <p>不能再用 {@code this.position()}：恶悬在台面上方 13 格，
     * 以身体位置为圆心画的地面环会飘在半空。</p>
     */
    public Vec3 groundCenter() {
        return new Vec3(this.hoverLocked ? this.hoverX : this.getX(),
                hoverFloorY(),
                this.hoverLocked ? this.hoverZ : this.getZ());
    }

    /** 到某点的<b>水平</b>距离（忽略高度差）。 */
    public double horizontalDistanceTo(Vec3 pos) {
        return Math.hypot(pos.x - this.getX(), pos.z - this.getZ());
    }

    /**
     * 设置悬停锚点（召唤组在降临结束/开战时调用；契约 §4.1 的 {@code setHoverAnchor(cx,cy,cz)}）。
     *
     * <p><b>{@code anchorY} 传"悬停点 Y"</b>，也就是 {@code EvilSummon} 里的
     * {@code appearToY = floor + HOVER_ABOVE} 那个值（直接传进来即可）。
     * 台面高度自动反推：{@code floorY = anchorY - HOVER_ABOVE}。</p>
     *
     * <p>如果调用方手里只有台面高度，用
     * {@link #setHoverAnchorOnFloor(double, double, double)} 更方便（不必自己加 13）。</p>
     *
     * @param cx      台面中心 X（恶的 X 固定在这里，不移动）
     * @param anchorY 悬停点 Y（= 台面 + {@value #HOVER_ABOVE}）
     * @param cz      台面中心 Z（恶的 Z 固定在这里，不移动）
     */
    public void setHoverAnchor(double cx, double anchorY, double cz) {
        this.hoverX = cx;
        this.hoverZ = cz;
        this.hoverY = anchorY;
        this.floorY = anchorY - HOVER_ABOVE;
        this.hoverLocked = true;
        this.setNoGravity(true);
        if (!this.level().isClientSide()) {
            this.setPos(this.hoverX, this.hoverY, this.hoverZ);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            this.hurtMarked = true;
        }
    }

    /**
     * 按<b>台面高度</b>设置悬停锚点：内部换算成 {@code 台面 + HOVER_ABOVE}。
     *
     * @param cx     台面中心 X
     * @param floorY 台面高度（玩家站立脚底）
     * @param cz     台面中心 Z
     */
    public void setHoverAnchorOnFloor(double cx, double floorY, double cz) {
        this.setHoverAnchor(cx, floorY + HOVER_ABOVE, cz);
    }

    /**
     * 每 tick 锁定悬停点：X/Z 固定、Y 夹回锚点、速度清零。
     *
     * <p>为什么不靠 AI：恶"从天空裂缝伸出来的存在"不该走动/被击退位移；
     * 而悬空 + 无重力时，任何 push/爆炸/水流都可能把它推开，所以每 tick 硬夹回。</p>
     *
     * <p><b>降临期间不锁</b>（{@code evil_appearing} 标记）：那时是 {@code EvilSummon}
     * 把它从天上压到悬停高度的过程。降临结束后如果召唤组没调
     * {@link #setHoverAnchor}，这里按"当前位置 = 悬停点"自动捕获锚点兜底。</p>
     */
    private void lockHover() {
        this.setNoGravity(true);
        if (!this.hoverLocked) {
            if (this.getPersistentData().getBoolean("evil_appearing")) {
                return;   // 还在降临下降，别锁
            }
            // 自动捕获兜底：当前位置 = 悬停点；台面 = 悬停点下方 HOVER_ABOVE
            // TODO(修复 B · agent 丙)：EvilArena.hoverY 落地后应由召唤组显式调
            //   setHoverAnchor(centerX, appearToY, centerZ)，这里保留兜底（老存档/手动刷怪）。
            this.setHoverAnchor(this.getX(), this.getY(), this.getZ());
        }
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (Math.abs(this.getX() - this.hoverX) > 1.0E-3D
                || Math.abs(this.getY() - this.hoverY) > 1.0E-3D
                || Math.abs(this.getZ() - this.hoverZ) > 1.0E-3D) {
            this.setPos(this.hoverX, this.hoverY, this.hoverZ);
            this.hurtMarked = true;
        }
    }

    /**
     * 每 {@value #TARGET_SCAN_INTERVAL}t 用<b>水平距离</b>找 ≤{@value #TARGET_RANGE_H} 格的
     * 最近<b>生存</b>玩家设为 target（契约 §4.1）。
     *
     * <p>为什么不用原版目标 AI：恶悬在台面上方 13 格，原版的 3D 距离 + 视线
     * （{@code NearestAttackableTargetGoal}）会把台面玩家判成"够不到"，
     * 结果它整场都没有目标。</p>
     */
    private void updateTargetByHorizontalDistance(ServerLevel level) {
        if (this.tickCount % TARGET_SCAN_INTERVAL != 0) {
            return;
        }
        ServerPlayer best = null;
        double bestH2 = TARGET_RANGE_H * TARGET_RANGE_H;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) {
                continue;   // 只锁"生存"玩家（旁观/创造不打）
            }
            double dx = p.getX() - this.getX();
            double dz = p.getZ() - this.getZ();
            double h2 = dx * dx + dz * dz;
            if (h2 <= bestH2) {
                bestH2 = h2;
                best = p;
            }
        }
        if (best != null) {
            this.setTarget(best);
            return;
        }
        // 一个都没有：把已经失效/跑出 80 格的目标清掉
        var cur = this.getTarget();
        if (cur != null && (!cur.isAlive()
                || this.horizontalDistanceTo(cur.position()) > TARGET_RANGE_H)) {
            this.setTarget(null);
        }
    }

    /**
     * 悬空也要"转头看人"（契约 §4.1："只允许转头 look"）。
     *
     * <p>降临后恶通常带着 {@code setNoAi(true)}，原版 {@code LookControl} 不一定在跑，
     * 所以这里干脆手动写 yaw/headYaw/bodyYaw —— 服务端会通过
     * {@code ClientboundRotateHeadPacket} 同步给客户端。</p>
     */
    private void updateHoverLook() {
        var target = this.getTarget();
        if (target == null || !target.isAlive() || this.tickCount % 5 != 0) {
            return;
        }
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(this.position());
        if (to.lengthSqr() < 1.0E-4D) {
            return;
        }
        float yaw = (float) (Math.atan2(-to.x, to.z) * (180.0D / Math.PI));
        this.setYRot(yaw);
        this.yHeadRot = yaw;
        this.yBodyRot = yaw;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    // ==================================================================
    // 阶段转场（契约 §2.3）
    // ==================================================================

    /**
     * 进入阶段转场：<b>无敌 ≥40t + 全屏冲击波 + 天空阶段提升 + 标题/字幕 + 技能停转</b>。
     *
     * <p>为什么要有这一下：以前 66%/33% 只是发一句聊天就换形态，玩家打得太快会
     * 完全没注意到 Boss 变了。现在它停 3 秒、全场冲击、屏幕中间打标题。</p>
     */
    public void beginPhaseTransition(int to) {
        if (this.isMinion() || this.escaping || this.transitionTicks > 0) {
            return;
        }
        this.transitionTo = Math.max(2, Math.min(3, to));
        this.transitionTicks = TRANSITION_TICKS;
        this.setNoAi(true);
        this.setInvulnerable(true);
        // 技能停转：清掉当前前摇/连招
        EvilAbilities.reset(this);
        // 同步给客户端：转场中 = state 3（busy）+ attack 8（转场冲击波信号）
        this.setBossState((byte) EvilAbilities.STATE_RECOVER);
        this.setBossAttack(SYNC_ATTACK_TRANSITION);
        // 形态 + 天空阶段立刻提升（模型切换在客户端读 phase）
        this.setPhase(this.transitionTo);
        if (this.level() instanceof ServerLevel level) {
            CombatSounds.phaseShift(level, this.position());
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR,
                    SoundSource.HOSTILE, 3.0F, 0.5F);
            this.announceTransition(level, this.transitionTo);
            this.syncSky(level);
        }
    }

    /** 标题 + 字幕（屏幕中央）+ 天空阶段提升后的同步包。 */
    private void announceTransition(ServerLevel level, int to) {
        Component title = Component.literal(to >= 3
                ? "\u00a74\u00a7l上 半 身 · 降 临" : "\u00a74\u00a7l巨 颅 · 降 临");
        Component subtitle = Component.literal(to >= 3
                ? "\u00a77它整个上半身挤进了这片天空。" : "\u00a77它把头探了进来。");
        for (ServerPlayer sp : level.players()) {
            try {
                sp.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 20));
                sp.connection.send(new ClientboundSetTitleTextPacket(title));
                sp.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            } catch (Throwable ignored) {
                // 单人/连接异常不影响战斗
            }
        }
    }

    private void syncSky(ServerLevel level) {
        try {
            EvilSky.set(EvilSky.strength(), EvilSky.openTicks(), this.evilPhase);
            for (ServerPlayer sp : level.players()) {
                com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(sp,
                        new com.hongmeng.xiuxian.network.EvilSkyPacket(
                                EvilSky.strength(), EvilSky.openTicks(), this.evilPhase));
            }
        } catch (Throwable ignored) {
            // 同步失败不影响战斗
        }
    }

    /** 转场每 tick：定格、粒子、第 {@value #TRANSITION_SHOCK_TICK} tick 全屏冲击波。 */
    private void tickTransition() {
        if (!(this.level() instanceof ServerLevel level)) {
            this.transitionTicks = 0;
            return;
        }
        int elapsed = TRANSITION_TICKS - this.transitionTicks;
        // 每 tick 重新压住"定格 + 无敌"：别的 boss 阶段系统（BossPhase）可能在同一时刻
        // 解冻/解无敌，这里每帧重申一次，保证转场 60t 内它一定不动、不掉血。
        this.setNoAi(true);
        this.setInvulnerable(true);
        if (this.transitionTicks % 6 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    12, 2.0D, 1.4D, 2.0D, 0.04D);
        }
        if (elapsed == TRANSITION_SHOCK_TICK) {
            this.transitionShockwave(level);
        }
        if (--this.transitionTicks > 0) {
            return;
        }
        // ---- 转场结束：解除定格/无敌，技能从 IDLE 重新开始 ----
        this.setInvulnerable(false);
        this.setNoAi(false);
        this.setBossState((byte) EvilAbilities.STATE_IDLE);
        this.setBossAttack((byte) 0);
        EvilAbilities.reset(this);
    }

    /**
     * 全屏冲击波：以恶的<b>台面投影</b>为圆心、半径 {@value #TRANSITION_SHOCK_RADIUS}
     * （悬空 Boss 适配：命中台面上水平 ≤30 且高度在台面附近的玩家）。
     */
    private void transitionShockwave(ServerLevel level) {
        Vec3 at = this.position();
        Vec3 c = this.groundCenter();
        CombatSounds.bossSlam(level, c);
        // 天空（恶身上）爆开 + 台面（脚下投影）铺开
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x,
                at.y + this.getBbHeight() * 0.4D, at.z, 4, 2.0D, 1.0D, 2.0D, 0.0D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 0.3D, c.z,
                160, 2.0D, 0.6D, 2.0D, 1.1D);
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 3.0F, 0.4F);
        double r = TRANSITION_SHOCK_RADIUS;
        float dmg = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6D);
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            double dh = Math.hypot(p.getX() - c.x, p.getZ() - c.z);
            if (dh > r || Math.abs(p.getY() - c.y) > PLATFORM_TOLERANCE + 4.0D) {
                continue;
            }
            if (p.hurt(level.damageSources().mobAttack(this), dmg)) {
                Vec3 push = p.position().subtract(c);
                Vec3 dir = new Vec3(push.x, 0.0D, push.z);
                if (dir.lengthSqr() > 1.0E-4D) {
                    dir = dir.normalize().scale(1.3D);
                    p.push(dir.x, 0.6D, dir.z);
                    p.hurtMarked = true;
                }
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
        }
    }

    // ==================================================================
    // 受击：单次上限 + 阶段锁血 + 逃走
    // ==================================================================

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource src, float amount) {
        if (this.escaping || this.transitioning() || this.isInvulnerable()) {
            return false;
        }
        // 小眼是普通爪牙：不吃 Boss 的锁血/上限/逃走规则
        if (this.isMinion()) {
            return super.hurt(src, amount);
        }
        // ① 单次受击上限
        float cap = this.getMaxHealth() * MAX_HIT_RATIO;
        if (amount > cap) {
            amount = cap;
        }
        // ② 阶段锁血：一阶段打不到 66% 以下，二阶段打不到 33% 以下
        float floor = switch (evilPhase) {
            case 1 -> PHASE2_LOCK;
            case 2 -> PHASE3_LOCK;
            default -> 0.0F;
        };
        if (floor > 0.0F) {
            float minHp = this.getMaxHealth() * floor;
            if (this.getHealth() - amount < minHp) {
                amount = Math.max(0.0F, this.getHealth() - minHp);
                // 打不穿时给个反馈（不然玩家以为卡了）
                if (amount <= 0.05F && this.tickCount % 20 == 0) {
                    if (src.getEntity() instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.literal(
                                "\u00a77它还不是完整形态。"), true);
                    }
                }
            }
        }
        boolean wouldDie = amount >= this.getHealth();
        boolean ok = super.hurt(src, wouldDie ? Math.max(0.0F, this.getHealth() - 1.0F)
                : amount);
        if (ok && wouldDie && !this.isDeadOrDying()) {
            startEscape();
        }
        return ok;
    }

    /** 开始逃走（同时把小眼一起带走）。 */
    public void startEscape() {
        if (escaping()) {
            return;
        }
        escaping = true;
        escapeTicks = ESCAPE_TOTAL;
        this.getPersistentData().putBoolean("evil_escaping", true);
        EvilAbilities.reset(this);
        this.clearMinionsOnce();
        if (this.level() instanceof ServerLevel sl) {
            for (var p : sl.players()) {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a77它退回了裂口。"), false);
            }
        }
    }

    // ==================================================================
    // 主循环
    // ==================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // ---- 悬空：永不受重力（小眼也浮在台面上，不落地）----
        this.setNoGravity(true);
        // ---- 小眼：只跑弱化凝视，不碰阶段/天空/召唤/悬停 ----
        if (this.isMinion()) {
            EvilAbilities.tick(this);
            return;
        }
        // ---- 逃走：一路升回裂口 ----
        if (escaping) {
            this.setNoAi(true);
            this.setInvulnerable(true);
            this.setDeltaMovement(0.0D, 0.42D, 0.0D);
            this.hurtMarked = true;
            escapeTicks--;
            if (escapeTicks <= 0) {
                com.hongmeng.xiuxian.realworld.EvilSummon.endEscaped(
                        this.level() instanceof ServerLevel s2 ? s2 : null,
                        this.blockPosition());
                this.discard();
            }
            return;
        }
        // ---- 悬停锁定 + 水平距离目标获取 + 转头（契约 §4.1）----
        if (this.level() instanceof ServerLevel serverLevel) {
            this.lockHover();
            this.updateTargetByHorizontalDistance(serverLevel);
            this.updateHoverLook();
        }
        // ---- 阶段转场：无敌 + 技能停转，转场结束才恢复 ----
        if (transitionTicks > 0) {
            tickTransition();
            return;
        }
        // ---- 66% / 33% 换阶段：先进转场（演出），转场结束才是新阶段 ----
        float hp = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        if (evilPhase == 1 && hp <= PHASE2_LOCK) {
            beginPhaseTransition(2);
            return;
        }
        if (evilPhase == 2 && hp <= PHASE3_LOCK) {
            beginPhaseTransition(3);
            return;
        }
        // ---- 技能状态机 ----
        EvilAbilities.tick(this);
    }

    // ==================================================================
    // 小眼：召唤 / 计数 / 清除（契约 §2.2 小眼修复）
    // ==================================================================

    /**
     * 召唤小眼（2~3 只，同场 ≤{@value #MINION_CAP}）。
     *
     * <p>小眼的属性在这里<b>一次性钳死</b>：品阶降为 ELITE（无 BOSS 条）、
     * 血量 = {@value #MINION_MAX_HEALTH}、攻击 = {@value #MINION_ATTACK}；
     * 并在入世前打上"跳过开场难度"的标记，避免被灵根/BossScaling 重新放大。</p>
     *
     * @return 实际生成的只数
     */
    public static int spawnMinions(ServerLevel level, EvilGazerEntity boss, int count) {
        if (level == null || boss == null || boss.isMinion()) {
            return 0;   // 小眼不能递归召唤
        }
        int room = Math.max(0, MINION_CAP - countMinions(level, boss));
        int n = Math.min(count, room);
        int spawned = 0;
        // 小眼刷在**台面**上（契约 §4.2 SUMMON）：以恶的台面投影为圆心、半径 8 的一圈，
        // 高度 = 台面 + 1.5（浮在半空一点），不是刷在恶身边 13 格高的空中。
        Vec3 ground = boss.groundCenter();
        for (int i = 0; i < n; i++) {
            EvilGazerEntity minion = ModEntities.EVIL_GAZER.get().create(level);
            if (minion == null) {
                continue;
            }
            double a = 2.0D * Math.PI * i / Math.max(1, n);
            minion.moveTo(ground.x + Math.cos(a) * 8.0D,
                    ground.y + 1.5D, ground.z + Math.sin(a) * 8.0D,
                    (float) (a * 180.0D / Math.PI), 0.0F);
            minion.setMinion(true);
            minion.setNoGravity(true);                  // 浮在台面上，不落地
            minion.demoteRank(MobRank.ELITE);           // 不显示 BOSS 条
            minion.getPersistentData().putBoolean(SCALED_TAG, true);
            minion.getPersistentData().putBoolean(BOSS_SCALED_TAG, true);
            // 属性钳制：血量 ≤60、攻击弱化（小眼只配"弱化凝视"）
            AttributeInstance hp = minion.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(MINION_MAX_HEALTH);
            }
            AttributeInstance atk = minion.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
                atk.setBaseValue(MINION_ATTACK);
            }
            minion.setHealth(MINION_MAX_HEALTH);        // 出生即满血（唯一允许的 setHealth：出生钳血）
            minion.setBossAttack((byte) 0);
            minion.setBossState((byte) 0);
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.PORTAL,
                    minion.getX(), minion.getY() + 1.0D, minion.getZ(),
                    20, 0.4D, 0.8D, 0.4D, 0.12D);
            spawned++;
        }
        return spawned;
    }

    /** 同场小眼数量（半径 64 格内，按标记统计）。 */
    public static int countMinions(ServerLevel level, EvilGazerEntity boss) {
        return level.getEntitiesOfClass(EvilGazerEntity.class,
                boss.getBoundingBox().inflate(64.0D),
                e -> e != boss && e.isMinion()).size();
    }

    /**
     * Boss 逃走/死亡时把小眼一并清除（一次性）。
     *
     * <p>不清会留下一堆没人管的小眼；只清带 {@value #MINION_TAG} 标记的，
     * 不会误伤自然刷出来的「恶」。</p>
     */
    public void clearMinionsOnce() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.getPersistentData().getBoolean("evil_minions_cleared")) {
            return;
        }
        this.getPersistentData().putBoolean("evil_minions_cleared", true);
        Vec3 at = this.position();
        List<EvilGazerEntity> all = level.getEntitiesOfClass(EvilGazerEntity.class,
                new AABB(at.x - 64.0D, at.y - 32.0D, at.z - 64.0D,
                        at.x + 64.0D, at.y + 32.0D, at.z + 64.0D),
                e -> e != this && e.isMinion());
        for (EvilGazerEntity e : all) {
            level.sendParticles(ParticleTypes.POOF,
                    e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                    10, 0.3D, 0.5D, 0.3D, 0.02D);
            e.discard();
        }
    }

    // ==================================================================
    // 移除 / 掉落
    // ==================================================================

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.isMinion()) {
            try {
                this.clearMinionsOnce();
            } catch (Throwable ignored) {
                // 清小眼失败不影响移除
            }
        }
        super.remove(reason);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource src,
                                       int looting, boolean recentlyHit) {
        // 小眼：无掉落、不收尾（它们不是祭坛召唤出来的本体）
        if (this.isMinion()) {
            return;
        }
        super.dropCustomDeathLoot(src, looting, recentlyHit);
        // 真的死了（没走逃走流程）时的兜底
        if (this.level() instanceof ServerLevel sl) {
            com.hongmeng.xiuxian.realworld.EvilSummon.endEscaped(sl, this.blockPosition());
        }
    }

    /** 小眼不给经验（不给玩家刷经验的漏洞）。1.20.1 的签名是无参的。 */
    @Override
    public int getExperienceReward() {
        if (this.isMinion()) {
            return 0;
        }
        return super.getExperienceReward();
    }
}
