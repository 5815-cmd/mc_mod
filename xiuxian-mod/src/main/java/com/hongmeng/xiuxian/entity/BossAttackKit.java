package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.combat.CombatSounds;
import com.hongmeng.xiuxian.technique.ParticleBatch;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Boss 攻击状态机</b>（第四轮 · Boss 战斗系统，唯一契约
 * {@code 工程/13-第五维度与Boss战斗系统设计.md} §3）。
 *
 * <p>把原来的「按 tick 轮询放技能」（{@link BossAbilities}）升级成
 * <b>IDLE → WINDUP → ACTIVE → RECOVER → IDLE</b> 的可读、可躲循环：
 * 每次攻击都有明确的<b>前摇</b>（地面预警环 + 起手音 + 音调递升的蓄力脉冲），
 * 玩家能靠走位跑出范围；判定只在前摇结束的那一瞬发生。</p>
 *
 * <h2>覆盖对象（按实体注册名分派，不引用具体实体类，跨 agent 也不怕）</h2>
 * <table border="1">
 *   <caption>主题 → 攻击池（p1 | p2 追加 | p3 追加）与召唤物</caption>
 *   <tr><th>id</th><th>主题</th><th>攻击池 p1 / +p2 / +p3</th><th>召唤</th></tr>
 *   <tr><td>epoch_boss</td><td>纪元</td><td>slam, spell, summon | shockwave | cataclysm</td>
 *       <td>spirit_wolf / puppet_captain</td></tr>
 *   <tr><td>beast_boss</td><td>妖兽</td><td>slam, breath, summon | shockwave | cataclysm</td>
 *       <td>crimson_flame_wolf / bone_eagle</td></tr>
 *   <tr><td>demon_boss</td><td>魔道</td><td>slam, spell, summon | ambush | cataclysm</td>
 *       <td>blood_demon_cultivator / demon_spider</td></tr>
 *   <tr><td>void_boss</td><td>虚空</td><td>slam, spell, summon | ambush | cataclysm</td>
 *       <td>void_jelly / star_dust_wraith</td></tr>
 *   <tr><td>blaze_emperor</td><td>下界</td><td>slam, breath, summon, charge | shockwave | cataclysm</td>
 *       <td>minecraft:magma_cube</td></tr>
 *   <tr><td>void_dragon_emperor</td><td>末地</td><td>slam, breath, charge, summon | shockwave | cataclysm</td>
 *       <td>void_serpent</td></tr>
 *   <tr><td>heaven_lord</td><td>仙庭</td><td>slam, shockwave, summon, charge | spell | cataclysm</td>
 *       <td>heaven_soldier / cloud_crane</td></tr>
 *   <tr><td>evil_gazer</td><td>恶</td><td colspan="2"><b>最小接入</b>：只接狂暴；
 *       技能仍归 {@code EvilAbilities}，旧的轮询也保留</td></tr>
 * </table>
 *
 * <h2>冷却 / 运转条件</h2>
 * <ul>
 *   <li>冷却：阶段一 <b>140t</b> / 阶段二 <b>100t</b> / 阶段三 <b>70t</b>；
 *       狂暴后 ×0.6（{@value #ENRAGE_COOLDOWN_MUL}）；</li>
 *   <li>只在<b>有目标且距离 ≤ {@value #ENGAGE_RANGE} 格</b>时运转；
 *       被定格（{@code isNoAi}）/ 无敌 / 死亡时停转；</li>
 *   <li>阶段切换：{@link BossPhase#enter} 里调 {@link #reset} 清前摇 + 重置冷却。</li>
 * </ul>
 *
 * <h2>状态存哪</h2>
 * <p>存在 Boss 的 {@code persistentData}（服务端、几个 int，随实体存档走）——
 * <b>不用静态 Map</b>：静态 Map 要自己处理死亡/卸载清理，一旦漏清就会内存泄漏、
 * 甚至把旧状态带给复用的实体。这里 6 个标量字段不会随 tick 累积增长（只在状态切换时写）。</p>
 *
 * <h2>伤害口径（不越权）</h2>
 * <p>所有伤害都走 <code>boss.hurt(level.damageSources().mobAttack(boss), dmg)</code>
 * 或 {@code boss.doHurtTarget(...)}：吃护甲、吃境界压制、**吃 35% 单次软上限**，
 * 和玩家自己的近战完全同一条结算路径。本类不做任何直接 {@code setHealth}。</p>
 *
 * <h2>镜头震动</h2>
 * <p>{@code CombatShake} 是 {@code @OnlyIn(CLIENT)} 的，服务端调不到；本稿只做
 * <b>服务端粒子 + 音效</b>（震屏留给客户端从声音/受击链路推断，见交接说明）。</p>
 */
public final class BossAttackKit {

    // ==================================================================
    // 1. 数据结构
    // ==================================================================

    /** 状态机四态（序号存进 persistentData）。 */
    public enum State {
        /** 待机：冷却递减、条件满足就选下一招。 */
        IDLE,
        /** 前摇：地面预警 + 起手音，玩家可跑出范围。 */
        WINDUP,
        /** 判定：到点出伤害/召唤（冲击波/天崩地裂持续多 tick，按波次）。 */
        ACTIVE,
        /** 后摇：收招，期间不再起手。 */
        RECOVER
    }

    /** 攻击池（每个主题至少 3 种；阶段二/三各追加一种）。 */
    public enum Attack {
        /** 震地重击：前摇 30t → 半径 5 的 AoE → 后摇 20t。 */
        SLAM(30, 1, 20),
        /** 扩散冲击波：前摇 20t → 半径 3 / 9 两段扩散环 → 后摇 14t。 */
        SHOCKWAVE(20, 16, 14),
        /** 召唤：前摇 20t → 召 2~4 只主题小怪（phase3 翻倍）→ 后摇 40t。 */
        SUMMON(20, 1, 40),
        /** 法术（沿用 BossAbilities 的法阵法术，现在有前摇）。 */
        SPELL(20, 1, 20),
        /** 元素吐息（沿用 BossAbilities 的投射物吐息，现在有前摇）。 */
        BREATH(10, 1, 10),
        /** 魔影突袭（二阶段起）：前摇 12t → 瞬移到背后重击 → 后摇 16t。 */
        AMBUSH(12, 1, 16),
        /** 天崩地裂（三阶段）：前摇 20t → 3 圈冲击波 → 后摇 20t。 */
        CATACLYSM(20, 36, 20),
        /** 冲撞（世界 Boss / 仙庭之主）：前摇 15t 直线预警 → 10t 冲刺 → 后摇 15t。 */
        CHARGE(15, 10, 15),
        /** 狂暴（HP&lt;25% 一次）：前摇 20t 怒吼 → 攻击 +30%、移速 +20%、间隔 ×0.6。 */
        ENRAGE(20, 1, 30);

        /** 前摇 tick。 */
        public final int windup;
        /** 判定 tick（多波次攻击在这一段里逐波结算）。 */
        public final int active;
        /** 后摇 tick。 */
        public final int recover;

        Attack(int windup, int active, int recover) {
            this.windup = windup;
            this.active = active;
            this.recover = recover;
        }
    }

    /** Boss 主题（决定攻击池与召唤物；按实体注册名分派）。 */
    public enum Theme {
        /** 不在覆盖名单里：走旧的轮询逻辑。 */
        NONE,
        EPOCH,
        BEAST,
        DEMON,
        VOID,
        BLAZE,
        DRAGON,
        HEAVEN,
        /** 恶：只接狂暴。 */
        EVIL
    }

    /** 本 kit 对一只 Boss 的接管程度。 */
    public enum Role {
        /** 状态机全面接管（覆盖名单）。 */
        KIT,
        /** 只接狂暴，其余保持原样（恶）。 */
        MINIMAL,
        /** 完全不接管（非覆盖名单的 BOSS/首领，继续走旧轮询）。 */
        LEGACY
    }

    // ==================================================================
    // 2. 常量（check_boss_kit.py 会核对）
    // ==================================================================

    /** 阶段一攻击间隔（tick）。 */
    public static final int COOLDOWN_PHASE1 = 140;
    /** 阶段二攻击间隔（tick）。 */
    public static final int COOLDOWN_PHASE2 = 100;
    /** 阶段三攻击间隔（tick）。 */
    public static final int COOLDOWN_PHASE3 = 70;
    /** 狂暴后攻击间隔倍率。 */
    public static final double ENRAGE_COOLDOWN_MUL = 0.6D;
    /** 狂暴触发血量比例（HP &lt; 25%）。 */
    public static final double ENRAGE_HP_FRACTION = 0.25D;
    /** 狂暴：攻击 +30%。 */
    public static final double ENRAGE_ATK_BONUS = 0.30D;
    /** 狂暴：移速 +20%。 */
    public static final double ENRAGE_SPD_BONUS = 0.20D;
    /** 运转/交战距离上限（格）。 */
    public static final double ENGAGE_RANGE = 40.0D;
    /** 震地重击半径（格）。 */
    public static final double SLAM_RADIUS = 5.0D;
    /** 震地重击伤害倍率（攻击力 × 1.2）。 */
    public static final double SLAM_DAMAGE_MUL = 1.2D;
    /** 冲击波内圈半径（格）。 */
    public static final double SHOCKWAVE_INNER = 3.0D;
    /** 冲击波外圈半径（格）。 */
    public static final double SHOCKWAVE_OUTER = 9.0D;
    /** 冲击波/天崩地裂的环带判定半宽（格）。 */
    public static final double RING_BAND = 1.6D;
    /** 同场召唤物上限。 */
    public static final int MAX_MINIONS = 6;
    /** 阶段三召唤数量（阶段一/二为它的一半）。 */
    public static final int SUMMON_PHASE3_COUNT = 4;
    /** 召唤物相对 Boss 的落点半径（格）。 */
    private static final double SUMMON_RING = 2.6D;

    // persistentData 键（服务端状态，随实体存档；不用静态 Map，避免清理泄漏）
    private static final String TAG_STAGE = "hm_kit_stage";
    private static final String TAG_ATTACK = "hm_kit_attack";
    private static final String TAG_TIMER = "hm_kit_timer";
    private static final String TAG_ELAPSED = "hm_kit_elapsed";
    private static final String TAG_CD = "hm_kit_cd";
    private static final String TAG_LAST = "hm_kit_last";
    private static final String TAG_PHASE = "hm_kit_phase";
    private static final String TAG_ENRAGED = "hm_boss_enraged";
    private static final String TAG_MINION = "hm_boss_minion";

    /** 狂暴的永久属性修饰符（与 BossPhase 的阶段修饰符分开，互不覆盖）。 */
    private static final UUID ENRAGE_ATK_Id = UUID.nameUUIDFromBytes(
            "hm_boss_enrage_atk".getBytes(StandardCharsets.UTF_8));
    private static final UUID ENRAGE_SPD_Id = UUID.nameUUIDFromBytes(
            "hm_boss_enrage_spd".getBytes(StandardCharsets.UTF_8));
    /** 召唤物减攻的永久修饰符（每只自己一份，名字相同也无妨：不同实体互不影响）。 */
    private static final UUID MINION_ATK_Id = UUID.nameUUIDFromBytes(
            "hm_boss_minion_atk".getBytes(StandardCharsets.UTF_8));

    // ==================================================================
    // 3. 数据表（唯一真源；解析在静态块里，check_boss_kit.py 逐条核对）
    // ==================================================================

    /**
     * 攻击池表：{@code 实体id=p1 攻击(逗号分隔)|p2 追加|p3 追加}。
     *
     * <p>格式刻意做成字符串：{@code tools/check_boss_kit.py} 能逐条静态核对
     * （每只 ≥3 种、p1⊂p2⊂p3、名字都在 {@link Attack} 里），而运行时只解析一次。</p>
     */
    public static final String POOL_TABLE =
            "epoch_boss=SLAM,SPELL,SUMMON|SHOCKWAVE|CATACLYSM;"
            + "beast_boss=SLAM,BREATH,SUMMON|SHOCKWAVE|CATACLYSM;"
            + "demon_boss=SLAM,SPELL,SUMMON|AMBUSH|CATACLYSM;"
            + "void_boss=SLAM,SPELL,SUMMON|AMBUSH|CATACLYSM;"
            + "blaze_emperor=SLAM,BREATH,SUMMON,CHARGE|SHOCKWAVE|CATACLYSM;"
            + "void_dragon_emperor=SLAM,BREATH,CHARGE,SUMMON|SHOCKWAVE|CATACLYSM;"
            + "heaven_lord=SLAM,SHOCKWAVE,SUMMON,CHARGE|SPELL|CATACLYSM";

    /**
     * 召唤表：{@code 实体id=小怪id,小怪id}（运行时通过注册表按 id 解析，
     * 找不到就安静跳过 —— 别的 agent 的实体晚交付也不会让 Boss 崩）。
     */
    public static final String SUMMON_TABLE =
            "epoch_boss=spirit_wolf,puppet_captain;"
            + "beast_boss=crimson_flame_wolf,bone_eagle;"
            + "demon_boss=blood_demon_cultivator,demon_spider;"
            + "void_boss=void_jelly,star_dust_wraith;"
            + "blaze_emperor=minecraft:magma_cube;"
            + "void_dragon_emperor=void_serpent;"
            + "heaven_lord=heaven_soldier,cloud_crane";

    /** 主题分派表：{@code 实体id=Theme 名}（覆盖名单 + 恶）。 */
    public static final String THEME_TABLE =
            "epoch_boss=EPOCH;beast_boss=BEAST;demon_boss=DEMON;void_boss=VOID;"
            + "blaze_emperor=BLAZE;void_dragon_emperor=DRAGON;heaven_lord=HEAVEN;"
            + "evil_gazer=EVIL";

    /** 解析后的攻击池：key = 实体 id，value = [p1, p2, p3] 三段。 */
    private static final Map<String, Attack[][]> POOLS = new LinkedHashMap<>();
    /** 解析后的召唤池：key = 实体 id，value = 小怪 id 数组。 */
    private static final Map<String, String[]> SUMMONS = new LinkedHashMap<>();
    /** 解析后的主题表。 */
    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();

    static {
        for (String entry : POOL_TABLE.split(";")) {
            if (entry.isBlank()) {
                continue;
            }
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String id = entry.substring(0, eq).trim();
            String[] seg = entry.substring(eq + 1).split("\\|", -1);
            Attack[] p1 = parseAttacks(seg.length > 0 ? seg[0] : "");
            Attack[] p2 = concat(p1, parseAttacks(seg.length > 1 ? seg[1] : ""));
            Attack[] p3 = concat(p2, parseAttacks(seg.length > 2 ? seg[2] : ""));
            POOLS.put(id, new Attack[][]{p1, p2, p3});
        }
        for (String entry : SUMMON_TABLE.split(";")) {
            if (entry.isBlank()) {
                continue;
            }
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            SUMMONS.put(entry.substring(0, eq).trim(),
                    entry.substring(eq + 1).trim().split(","));
        }
        for (String entry : THEME_TABLE.split(";")) {
            if (entry.isBlank()) {
                continue;
            }
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                THEMES.put(entry.substring(0, eq).trim(),
                        Theme.valueOf(entry.substring(eq + 1).trim()));
            } catch (IllegalArgumentException ignored) {
                // 主题名写错时跳过该行（运行时按 NONE 处理，不崩）
            }
        }
    }

    private BossAttackKit() {
    }

    // ==================================================================
    // 4. 对外入口
    // ==================================================================

    /**
     * 本 kit 对某只 Boss 的接管程度。
     *
     * <p>看实体<b>注册名</b>，不看类：{@code heaven_lord} 等其它 agent 的实体
     * 只要注册名对上，就自动进入覆盖名单，本文件不需要 import 它们。</p>
     */
    public static Role roleOf(LivingEntity boss) {
        Theme theme = themeOf(boss);
        if (theme == Theme.EVIL) {
            return Role.MINIMAL;
        }
        if (theme == Theme.NONE) {
            return Role.LEGACY;
        }
        return Role.KIT;
    }

    /** Boss 主题（不在表里 = {@link Theme#NONE}）。 */
    public static Theme themeOf(LivingEntity boss) {
        if (boss == null) {
            return Theme.NONE;
        }
        Theme t = THEMES.get(idOf(boss));
        return t == null ? Theme.NONE : t;
    }

    /**
     * 每 tick 一次（唯一入口：{@link BossAbilities#tick} 转发）。
     *
     * <p>给覆盖名单里的 Boss 跑完整状态机。任何异常都在这里吞掉并记 debug ——
     * 一个 Boss 的攻击状态机出问题，绝不能把整个服务端 tick 带崩。</p>
     */
    public static void tick(CultivationMonsterEntity boss) {
        try {
            run(boss);
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[boss-kit] tick 失败：{}", t.toString());
        }
        syncClientState(boss);
    }

    /**
     * 把服务端状态机的 stage/attack 同步给客户端（供 {@code BossPresenceClient} 做
     * 招式判定镜头震动、以及模型/演出读取）。
     *
     * <p>把 {@code TAG_ATTACK} 的 ordinal 映射成"1=slam … 9=enrage"，{@code -1} 映射成 0。
     * 放在 tick 末尾统一写：状态机内部的所有分支（含各种提前 return）都能覆盖到。</p>
     */
    private static void syncClientState(CultivationMonsterEntity boss) {
        if (boss == null || boss.level().isClientSide()) {
            return;
        }
        try {
            net.minecraft.nbt.CompoundTag pd = boss.getPersistentData();
            int stage = pd.getInt(TAG_STAGE);
            int attack = pd.getInt(TAG_ATTACK);
            boss.setBossState((byte) Math.max(0, Math.min(3, stage)));
            if (attack < 0) {
                boss.setBossAttack((byte) 0);
            } else {
                boss.setBossAttack((byte) (Math.max(0, Math.min(8, attack)) + 1));
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[boss-kit] 同步客户端状态失败：{}", t.toString());
        }
    }

    /**
     * 狂暴检查（恶的"最小接入"用它；覆盖名单里的 Boss 由状态机内部调用）。
     *
     * <p>HP &lt; 25% 时<b>只触发一次</b>（标记 {@code hm_boss_enraged}）：
     * 攻击 +30%、移速 +20%、攻击间隔 ×0.6，并放一次怒吼 + 红色粒子。</p>
     */
    public static void tickEnrage(CultivationMonsterEntity boss) {
        try {
            if (boss == null || boss.level().isClientSide()) {
                return;
            }
            if (!(boss.level() instanceof ServerLevel level)) {
                return;
            }
            applyEnrageIfNeeded(level, boss);
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[boss-kit] 狂暴检查失败：{}", t.toString());
        }
    }

    /**
     * 阶段切换时调用（{@link BossPhase#enter}）：<b>清前摇 + 重置冷却</b>。
     *
     * <p>为什么必须清：Boss 转阶段会定格 1 秒并回满血；如果这时它正处在"举拳前摇"
     * 里，定格结束后会立刻砸下来，玩家等于被白打一下。清掉之后它从新的阶段重新起手。</p>
     */
    public static void reset(CultivationMonsterEntity boss) {
        try {
            if (boss == null) {
                return;
            }
            CompoundTag pd = boss.getPersistentData();
            pd.putInt(TAG_STAGE, State.IDLE.ordinal());
            pd.putInt(TAG_ATTACK, -1);
            pd.putInt(TAG_TIMER, 0);
            pd.putInt(TAG_ELAPSED, 0);
            pd.putInt(TAG_CD, 0);
            pd.putInt(TAG_LAST, -1);
            pd.putInt(TAG_PHASE, Math.max(1, BossPhase.phaseOf(boss)));
            boss.setBossState((byte) 0);
            boss.setBossAttack((byte) 0);
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[boss-kit] reset 失败：{}", t.toString());
        }
    }

    /** 召唤物标记键（清理/计数用；BossPhase 或调试命令可以按它清场）。 */
    public static String minionTag() {
        return TAG_MINION;
    }

    // ==================================================================
    // 5. 状态机
    // ==================================================================

    private static void run(CultivationMonsterEntity boss) {
        if (boss == null || boss.level().isClientSide()) {
            return;
        }
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        if (roleOf(boss) != Role.KIT) {
            return;
        }
        // 狂暴不占状态机：任何时候血线到了就立刻触发（怒吼也算一次攻击，见下）
        applyEnrageIfNeeded(level, boss);
        // 死亡 / 定格 / 无敌：停转（不清状态，解除后接着走）
        if (!boss.isAlive()) {
            // Boss 倒下：把它的召唤物带走（按 hm_boss_minion 标记，一次性）
            clearMinionsOnce(level, boss);
            return;
        }
        if (boss.isNoAi() || boss.isInvulnerable()) {
            return;
        }
        CompoundTag pd = boss.getPersistentData();
        // 狂暴 aura：变红（服务端粒子；客户端不需要新 packet）
        if (pd.getBoolean(TAG_ENRAGED) && boss.tickCount % 10 == 0) {
            level.sendParticles(ParticleTypes.FLAME,
                    boss.getX(), boss.getY() + boss.getBbHeight() * 0.55D, boss.getZ(),
                    3, boss.getBbWidth() * 0.5D, 0.5D, boss.getBbWidth() * 0.5D, 0.01D);
        }
        LivingEntity target = boss.getTarget();
        boolean engaged = target != null && target.isAlive()
                && boss.distanceTo(target) <= ENGAGE_RANGE;
        if (!engaged) {
            // 没目标/跑太远：把前摇清掉（否则它会对空气挥完一整套）
            if (pd.getInt(TAG_STAGE) != State.IDLE.ordinal()) {
                toIdle(pd, 0);
            }
            return;
        }
        int phase = Math.max(1, BossPhase.phaseOf(boss));
        if (pd.getInt(TAG_PHASE) != phase) {
            // 双保险：BossPhase.enter 里也会 reset，这里防止漏接
            pd.putInt(TAG_PHASE, phase);
            toIdle(pd, 0);
        }
        State state = stateOf(pd);
        switch (state) {
            case IDLE -> tickIdle(level, boss, target, pd, phase);
            case WINDUP -> tickWindup(level, boss, target, pd);
            case ACTIVE -> tickActive(level, boss, target, pd);
            case RECOVER -> tickRecover(level, boss, pd, phase);
            default -> toIdle(pd, 0);
        }
    }

    /** 待机：冷却递减；到点按阶段选一招起手。 */
    private static void tickIdle(ServerLevel level, CultivationMonsterEntity boss,
                                 LivingEntity target, CompoundTag pd, int phase) {
        int cd = pd.getInt(TAG_CD);
        if (cd > 0) {
            pd.putInt(TAG_CD, cd - 1);
            return;
        }
        Attack pick;
        if (!pd.getBoolean(TAG_ENRAGED)
                && boss.getHealth() / Math.max(1.0F, boss.getMaxHealth()) <= ENRAGE_HP_FRACTION) {
            // 狂暴优先占一次起手（有怒吼前摇，玩家看得见它"暴怒"）
            pick = Attack.ENRAGE;
        } else {
            pick = pickAttack(boss, phase, pd);
        }
        if (pick == null) {
            return;
        }
        startWindup(level, boss, target, pd, pick);
    }

    /** 起手：写状态 + 起手音 + 第一帧预警。 */
    private static void startWindup(ServerLevel level, CultivationMonsterEntity boss,
                                    LivingEntity target, CompoundTag pd, Attack attack) {
        pd.putInt(TAG_STAGE, State.WINDUP.ordinal());
        pd.putInt(TAG_ATTACK, attack.ordinal());
        pd.putInt(TAG_TIMER, attack.windup);
        pd.putInt(TAG_ELAPSED, 0);
        Vec3 at = boss.position();
        CombatSounds.windupStart(level, at);
        if (attack == Attack.AMBUSH) {
            // 突袭前摇：原地冒墨（"它要消失了"）
            level.sendParticles(ParticleTypes.SQUID_INK, at.x,
                    at.y + boss.getBbHeight() * 0.5D, at.z,
                    24, 0.6D, 0.9D, 0.6D, 0.02D);
        }
        // 前摇第 0 帧：地面预警环从最小半径开始（随进度扩散，玩家看得出"圈在变大"）
        telegraph(level, boss, attack, 0.0F);
    }

    /** 前摇：倒计时 + 周期性预警环 + 蓄力音（音调随进度升高）。 */
    private static void tickWindup(ServerLevel level, CultivationMonsterEntity boss,
                                   LivingEntity target, CompoundTag pd) {
        int timer = pd.getInt(TAG_TIMER) - 1;
        int attackOrd = pd.getInt(TAG_ATTACK);
        Attack attack = attackOf(attackOrd);
        if (attack == null) {
            toIdle(pd, 0);
            return;
        }
        pd.putInt(TAG_TIMER, timer);
        if (timer <= 0) {
            pd.putInt(TAG_STAGE, State.ACTIVE.ordinal());
            pd.putInt(TAG_ELAPSED, 0);
            // 判定的第 0 帧立即结算一次（震地重击/法术/召唤都在这一帧发生）
            payload(level, boss, target, pd, attack, 0);
            return;
        }
        float progress = 1.0F - (float) timer / (float) attack.windup;
        if (timer % 6 == 0) {
            CombatSounds.windupPulse(level, boss.position(), progress);
        }
        if (timer % 3 == 0) {
            telegraph(level, boss, attack, progress);
        }
        // 前摇期间盯住目标：起手一旦开始就转体面向玩家（看得见"它冲你来了"）
        if (target != null && timer % 4 == 0) {
            boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    /** 判定：多波次攻击在这一段里逐波结算；单发攻击下一 tick 就转后摇。 */
    private static void tickActive(ServerLevel level, CultivationMonsterEntity boss,
                                   LivingEntity target, CompoundTag pd) {
        int elapsed = pd.getInt(TAG_ELAPSED) + 1;
        int attackOrd = pd.getInt(TAG_ATTACK);
        Attack attack = attackOf(attackOrd);
        if (attack == null) {
            toIdle(pd, 0);
            return;
        }
        pd.putInt(TAG_ELAPSED, elapsed);
        if (elapsed < attack.active) {
            payload(level, boss, target, pd, attack, elapsed);
        }
        if (elapsed >= attack.active) {
            pd.putInt(TAG_STAGE, State.RECOVER.ordinal());
            pd.putInt(TAG_TIMER, attack.recover);
        }
    }

    /** 后摇：收招结束 → 回待机并开始下一轮冷却。 */
    private static void tickRecover(ServerLevel level, CultivationMonsterEntity boss,
                                    CompoundTag pd, int phase) {
        int timer = pd.getInt(TAG_TIMER) - 1;
        pd.putInt(TAG_TIMER, timer);
        if (timer > 0) {
            return;
        }
        int cd = cooldownFor(phase);
        if (pd.getBoolean(TAG_ENRAGED)) {
            cd = Math.max(20, (int) Math.round(cd * ENRAGE_COOLDOWN_MUL));
        }
        toIdle(pd, cd);
    }

    private static void toIdle(CompoundTag pd, int cooldown) {
        pd.putInt(TAG_STAGE, State.IDLE.ordinal());
        pd.putInt(TAG_ATTACK, -1);
        pd.putInt(TAG_TIMER, 0);
        pd.putInt(TAG_ELAPSED, 0);
        pd.putInt(TAG_CD, Math.max(0, cooldown));
    }

    /** 本阶段的攻击间隔。 */
    public static int cooldownFor(int phase) {
        return switch (phase) {
            case 3 -> COOLDOWN_PHASE3;
            case 2 -> COOLDOWN_PHASE2;
            default -> COOLDOWN_PHASE1;
        };
    }

    /** 从本阶段攻击池里随机挑一招（不连续重复同一招）。 */
    private static Attack pickAttack(CultivationMonsterEntity boss, int phase, CompoundTag pd) {
        Attack[] pool = poolOf(boss, phase);
        if (pool.length == 0) {
            return null;
        }
        int last = pd.getInt(TAG_LAST);
        int idx = boss.getRandom().nextInt(pool.length);
        if (pool.length > 1 && pool[idx].ordinal() == last) {
            idx = (idx + 1) % pool.length;
        }
        pd.putInt(TAG_LAST, pool[idx].ordinal());
        return pool[idx];
    }

    /** 某只 Boss 在当前阶段的攻击池（阶段之间的池子是"只加不减"的）。 */
    public static Attack[] poolOf(CultivationMonsterEntity boss, int phase) {
        Attack[][] pools = POOLS.get(idOf(boss));
        if (pools == null) {
            return new Attack[0];
        }
        int i = Math.max(0, Math.min(2, phase - 1));
        return pools[i];
    }

    // ==================================================================
    // 6. 判定（payload）
    // ==================================================================

    /** 单次判定/波次结算。{@code wave} 是 ACTIVE 段的第几 tick（0 = 判定首帧）。 */
    private static void payload(ServerLevel level, CultivationMonsterEntity boss,
                                LivingEntity target, CompoundTag pd, Attack attack, int wave) {
        switch (attack) {
            case SLAM -> {
                if (wave == 0) {
                    doSlam(level, boss);
                }
            }
            case SHOCKWAVE -> {
                if (wave == 0) {
                    shockwaveRing(level, boss, SHOCKWAVE_INNER);
                } else if (wave == 8) {
                    shockwaveRing(level, boss, SHOCKWAVE_OUTER);
                }
            }
            case CATACLYSM -> {
                if (wave == 0) {
                    cataclysmRing(level, boss, 3.0D);
                } else if (wave == 12) {
                    cataclysmRing(level, boss, 5.6D);
                } else if (wave == 24) {
                    cataclysmRing(level, boss, 8.2D);
                }
            }
            case SUMMON -> {
                if (wave == 0) {
                    doSummon(level, boss, Math.max(1, BossPhase.phaseOf(boss)));
                }
            }
            case SPELL -> {
                if (wave == 0) {
                    BossAbilities.castSpell(level, boss, target);
                }
            }
            case BREATH -> {
                if (wave == 0) {
                    BossAbilities.fireBreath(level, boss, target);
                }
            }
            case AMBUSH -> {
                if (wave == 0) {
                    doAmbush(level, boss, target);
                }
            }
            case CHARGE -> doChargeTick(level, boss, target, wave);
            case ENRAGE -> {
                if (wave == 0) {
                    applyEnrageBuff(level, boss, pd);
                }
            }
            default -> {
                // 不会到这里
            }
        }
    }

    /** <b>震地重击</b>：半径 5 的 AoE（攻击力 ×1.2 + 击退 + 上抛），走标准 hurt 路径。 */
    private static void doSlam(ServerLevel level, CultivationMonsterEntity boss) {
        Vec3 at = boss.position();
        double r = SLAM_RADIUS;
        float dmg = (float) (boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * SLAM_DAMAGE_MUL);
        CombatSounds.bossSlam(level, at);
        // 冲天的碎石与尘
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.4D, at.z,
                2, 0.6D, 0.2D, 0.6D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.3D, at.z,
                40, r * 0.8D, 0.25D, r * 0.8D, 0.08D);
        ParticleBatch batch = new ParticleBatch(level);
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            double a = 2.0D * Math.PI * i / seg;
            batch.add(ParticleTypes.FLAME,
                    at.x + Math.cos(a) * r, at.y + 0.15D, at.z + Math.sin(a) * r);
        }
        batch.flush();
        damageInRadius(level, boss, at, r, false, dmg);
        // 击退 + 上抛（"一拳掀飞"）
        for (LivingEntity victim : victimsInRadius(level, boss, at, r)) {
            Vec3 away = victim.position().subtract(at);
            double len = Math.max(0.1D, Math.sqrt(away.x * away.x + away.z * away.z));
            victim.knockback(1.1D, away.x / len, away.z / len);
            victim.push(0.0D, 0.55D, 0.0D);
            victim.hurtMarked = true;
        }
    }

    /** <b>扩散冲击波</b>：只打环带（|距离−半径| ≤ 1.6），命中给击退 + 缓慢。 */
    private static void shockwaveRing(ServerLevel level, CultivationMonsterEntity boss,
                                      double radius) {
        Vec3 at = boss.position();
        CombatSounds.cast(level, at, boss.elementForPhaseFx());
        ParticleBatch batch = new ParticleBatch(level);
        int seg = Math.max(18, (int) Math.round(radius * 6.0D));
        for (int i = 0; i < seg; i++) {
            double a = 2.0D * Math.PI * i / seg;
            batch.add(ParticleTypes.SOUL_FIRE_FLAME,
                    at.x + Math.cos(a) * radius, at.y + 0.2D, at.z + Math.sin(a) * radius);
        }
        batch.flush();
        float dmg = (float) (boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75D);
        damageInRing(level, boss, at, radius, dmg, true);
    }

    /** <b>天崩地裂</b>的一圈：环带伤害（攻击力 ×0.55）+ 火光。 */
    private static void cataclysmRing(ServerLevel level, CultivationMonsterEntity boss,
                                      double radius) {
        Vec3 at = boss.position();
        CombatSounds.bossSlam(level, at);
        ParticleBatch batch = new ParticleBatch(level);
        int seg = Math.max(20, (int) Math.round(radius * 6.0D));
        for (int i = 0; i < seg; i++) {
            double a = 2.0D * Math.PI * i / seg;
            batch.add(ParticleTypes.FLAME,
                    at.x + Math.cos(a) * radius, at.y + 0.15D, at.z + Math.sin(a) * radius);
            batch.add(ParticleTypes.LAVA,
                    at.x + Math.cos(a) * radius, at.y + 0.25D, at.z + Math.sin(a) * radius);
        }
        batch.flush();
        float dmg = (float) (boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55D);
        damageInRing(level, boss, at, radius, dmg, false);
    }

    /**
     * <b>召唤</b>：召 2~4 只主题小怪（阶段三翻倍），同场最多 {@value #MAX_MINIONS} 只。
     *
     * <p>小怪 id 通过注册表按字符串解析：找不到就安静跳过 —— 别的 agent 的实体
     * 还没交付时，Boss 只是少召一只，不会抛异常也不会崩。</p>
     */
    private static void doSummon(ServerLevel level, CultivationMonsterEntity boss, int phase) {
        Vec3 at = boss.position();
        String[] pool = SUMMONS.get(idOf(boss));
        if (pool == null || pool.length == 0) {
            return;
        }
        int want = phase >= 3 ? SUMMON_PHASE3_COUNT : Math.max(2, SUMMON_PHASE3_COUNT / 2);
        int existing = countMinions(level, boss);
        int room = Math.min(want, MAX_MINIONS - existing);
        CombatSounds.phaseShift(level, at);
        level.playSound(null, boss.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 1.8F, 0.7F);
        for (int i = 0; i < room; i++) {
            String id = pool[boss.getRandom().nextInt(pool.length)];
            EntityType<?> type = resolveType(id);
            if (type == null) {
                continue;   // 该 id 还没注册（跨 agent 交付中）：跳过，不影响其它召唤
            }
            Entity spawned = type.create(level);
            if (!(spawned instanceof Mob minion)) {
                continue;
            }
            double angle = 2.0D * Math.PI * i / Math.max(1, room);
            double x = at.x + Math.cos(angle) * SUMMON_RING;
            double z = at.z + Math.sin(angle) * SUMMON_RING;
            minion.moveTo(x, at.y, z, boss.getRandom().nextFloat() * 360.0F, 0.0F);
            minion.getPersistentData().putBoolean(TAG_MINION, true);
            // 召唤物比本体弱一档：只减 20% 攻击。
            // 刻意**不动血量**：MAX_HEALTH 的永久修饰符会把"当前血量"留在旧上限上
            // （要修就得 setHealth，那会绕开本类"不直接改血"的口径），小怪本来也不肉。
            addPermanentModifier(minion, Attributes.ATTACK_DAMAGE, MINION_ATK_Id,
                    "hm_boss_minion_atk", -0.20D);
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.PORTAL, x, at.y + 1.0D, z,
                    16, 0.4D, 0.8D, 0.4D, 0.12D);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.6D, at.z,
                1, 1.2D, 0.3D, 1.2D, 0.0D);
    }

    /** <b>魔影突袭</b>：瞬移到目标背后重击（背击穿格挡，逼玩家转身）。 */
    private static void doAmbush(ServerLevel level, CultivationMonsterEntity boss,
                                 LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 look = target.getLookAngle();
        Vec3 behind = target.position().subtract(look.normalize().scale(2.0D));
        boss.teleportTo(behind.x, target.getY(), behind.z);
        boss.getNavigation().stop();
        boss.doHurtTarget(target);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + 1.2D, target.getZ(),
                3, 0.3D, 0.3D, 0.3D, 0.0D);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    /**
     * <b>冲撞</b>：ACTIVE 段里每 tick 朝目标方向推进、撞飞路径上的人。
     *
     * <p>前摇 15t 是"直线预告"，玩家横移就能侧闪；冲刺 10t 走完接后摇。</p>
     */
    private static void doChargeTick(ServerLevel level, CultivationMonsterEntity boss,
                                     LivingEntity target, int wave) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (wave == 0) {
            boss.getNavigation().stop();
            level.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ROAR,
                    SoundSource.HOSTILE, 1.6F, 0.8F);
            return;
        }
        Vec3 delta = target.position().subtract(boss.position());
        Vec3 dir = new Vec3(delta.x, 0.0D, delta.z);
        if (dir.lengthSqr() < 1.0E-4D) {
            return;
        }
        dir = dir.normalize();
        // 冲刺：给一个水平速度（每 tick 覆写，走 10t），并把速度同步给客户端
        boss.setDeltaMovement(dir.x * 0.78D, boss.getDeltaMovement().y, dir.z * 0.78D);
        boss.hurtMarked = true;
        // 撞到的人：标准 hurt 路径 + 击退
        AABB box = boss.getBoundingBox().inflate(1.4D, 1.0D, 1.4D);
        float dmg = (float) (boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != boss && e.isAlive()
                        && (!(e instanceof CultivationMonsterEntity) || e == boss))) {
            if (victim.hurt(level.damageSources().mobAttack(boss), dmg)) {
                victim.knockback(1.0D, dir.x, dir.z);
                victim.hurtMarked = true;
            }
        }
        level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 0.4D, boss.getZ(),
                4, 0.3D, 0.2D, 0.3D, 0.02D);
    }

    // ==================================================================
    // 7. 狂暴
    // ==================================================================

    /**
     * HP &lt; 25% 时一次性狂暴。
     *
     * <p>覆盖名单里的 Boss <b>不在这里直接加 buff</b>：先让状态机起一次
     * {@link Attack#ENRAGE} 招式（20t 怒吼前摇，玩家看得见它"暴怒"），
     * 判定帧再调 {@link #applyEnrageBuff}。恶（最小接入）没有状态机 → 直接生效。</p>
     */
    private static void applyEnrageIfNeeded(ServerLevel level, CultivationMonsterEntity boss) {
        CompoundTag pd = boss.getPersistentData();
        if (pd.getBoolean(TAG_ENRAGED)) {
            return;
        }
        double frac = boss.getHealth() / Math.max(1.0F, boss.getMaxHealth());
        if (frac > ENRAGE_HP_FRACTION) {
            return;
        }
        if (roleOf(boss) == Role.KIT) {
            return;   // 由 tickIdle 优先选 ENRAGE，判定帧再 applyEnrageBuff
        }
        applyEnrageBuff(level, boss, pd);
    }

    /** 真正加狂暴 buff（属性修饰符 + 红色粒子 + 音效 + 播报）。 */
    private static void applyEnrageBuff(ServerLevel level, CultivationMonsterEntity boss,
                                        CompoundTag pd) {
        if (pd.getBoolean(TAG_ENRAGED)) {
            return;
        }
        pd.putBoolean(TAG_ENRAGED, true);
        addPermanentModifier(boss, Attributes.ATTACK_DAMAGE, ENRAGE_ATK_Id,
                "hm_boss_enrage_atk", ENRAGE_ATK_BONUS);
        addPermanentModifier(boss, Attributes.MOVEMENT_SPEED, ENRAGE_SPD_Id,
                "hm_boss_enrage_spd", ENRAGE_SPD_BONUS);
        // 近身压迫感：力量 + 速度（短时药水，叠加在永久修饰符之上）
        boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 1, false, false));
        boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 1, false, false));
        Vec3 at = boss.position();
        CombatSounds.bossRoar(level, at);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 1.0D, at.z,
                30, 1.6D, 1.2D, 1.6D, 0.02D);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.2D, at.z,
                60, 1.8D, 1.4D, 1.8D, 0.05D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.0D, at.z,
                2, 1.4D, 0.6D, 1.4D, 0.0D);
        announce(level, at, "\u00a74\u00a7l" + boss.getName().getString()
                + " \u00a7c陷入狂暴！\u00a77（攻击 +30% / 移速 +20% / 出手更快）", 64.0D);
        HongmengMod.LOGGER.info("[boss-kit] {} 进入狂暴", boss.getName().getString());
    }

    // ==================================================================
    // 8. 表现工具（前摇预警 / 粒子 / 播报）
    // ==================================================================

    /** 前摇预警：不同攻击画不同的地面提示。 */
    private static void telegraph(ServerLevel level, CultivationMonsterEntity boss,
                                  Attack attack, float progress) {
        Vec3 at = boss.position();
        ParticleOptions ringType = switch (attack) {
            case SLAM -> ParticleTypes.CRIT;                 // 白色火星圈 = "这一圈会砸"
            case SHOCKWAVE, CATACLYSM -> ParticleTypes.SOUL_FIRE_FLAME;
            case CHARGE -> ParticleTypes.END_ROD;            // 直线预告
            case ENRAGE -> ParticleTypes.FLAME;
            default -> ParticleTypes.ENCHANT;
        };
        ParticleBatch batch = new ParticleBatch(level);
        if (attack == Attack.CHARGE) {
            // 冲撞：沿面向画一条 8 格直线（侧闪提示）
            Vec3 look = boss.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0D, look.z);
            if (flat.lengthSqr() < 1.0E-4D) {
                flat = new Vec3(0.0D, 0.0D, 1.0D);
            }
            flat = flat.normalize();
            for (int i = 1; i <= 8; i++) {
                batch.add(ringType, at.x + flat.x * i, at.y + 0.15D, at.z + flat.z * i);
                batch.add(ringType, at.x + flat.x * i + flat.z * 0.6D, at.y + 0.15D,
                        at.z + flat.z * i - flat.x * 0.6D);
            }
        } else {
            double radius = switch (attack) {
                case SLAM -> SLAM_RADIUS;
                case SHOCKWAVE -> SHOCKWAVE_INNER + (SHOCKWAVE_OUTER - SHOCKWAVE_INNER) * progress;
                case CATACLYSM -> 3.0D + 5.2D * progress;
                default -> 2.0D + 2.0D * progress;
            };
            int seg = Math.max(16, (int) Math.round(radius * 5.0D));
            for (int i = 0; i < seg; i++) {
                double a = 2.0D * Math.PI * i / seg;
                batch.add(ringType,
                        at.x + Math.cos(a) * radius, at.y + 0.15D, at.z + Math.sin(a) * radius);
            }
        }
        batch.flush();
    }

    // ==================================================================
    // 9. 判定工具
    // ==================================================================

    /** 半径内的敌对目标（排除 Boss 自己与其它怪，除非是玩家召唤物）。 */
    private static List<LivingEntity> victimsInRadius(ServerLevel level, CultivationMonsterEntity boss,
                                                      Vec3 at, double radius) {
        AABB box = new AABB(at.x - radius, at.y - 2.0D, at.z - radius,
                at.x + radius, at.y + 3.0D, at.z + radius);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != boss && x.isAlive())) {
            if (e instanceof CultivationMonsterEntity) {
                continue;   // 不打自己人（Boss 的小怪也不会被自家 AoE 打死）
            }
            double d = Math.hypot(e.getX() - at.x, e.getZ() - at.z);
            if (d <= radius && e.getY() - at.y <= 4.0D) {
                out.add(e);
            }
        }
        return out;
    }

    /** 半径内全部结算一次标准伤害（{@code mobAttack}：吃护甲/软上限/境界压制）。 */
    private static void damageInRadius(ServerLevel level, CultivationMonsterEntity boss,
                                       Vec3 at, double radius, boolean slow, float dmg) {
        for (LivingEntity victim : victimsInRadius(level, boss, at, radius)) {
            victim.hurt(level.damageSources().mobAttack(boss), dmg);
            if (slow) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        20 * 3, 0, false, true));
            }
        }
    }

    /** 环带判定：只打 |距离 − 半径| ≤ {@value #RING_BAND} 的目标（可以"卡环缝"躲）。 */
    private static void damageInRing(ServerLevel level, CultivationMonsterEntity boss,
                                     Vec3 at, double radius, float dmg, boolean slow) {
        AABB box = new AABB(at.x - radius - RING_BAND, at.y - 2.0D, at.z - radius - RING_BAND,
                at.x + radius + RING_BAND, at.y + 3.0D, at.z + radius + RING_BAND);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != boss && e.isAlive() && !(e instanceof CultivationMonsterEntity))) {
            double d = Math.hypot(victim.getX() - at.x, victim.getZ() - at.z);
            if (Math.abs(d - radius) > RING_BAND) {
                continue;
            }
            if (victim.getY() - at.y > 3.0D) {
                continue;
            }
            if (victim.hurt(level.damageSources().mobAttack(boss), dmg)) {
                Vec3 away = victim.position().subtract(at);
                double len = Math.max(0.1D, Math.sqrt(away.x * away.x + away.z * away.z));
                victim.knockback(0.9D, away.x / len, away.z / len);
                if (slow) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            20 * 3, 0, false, true));
                }
            }
        }
    }

    /** 同场召唤物数量（按 persistentData 标记统计，半径 24 格内）。 */
    private static int countMinions(ServerLevel level, CultivationMonsterEntity boss) {
        return level.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(24.0D),
                m -> m != boss && m.getPersistentData().getBoolean(TAG_MINION)).size();
    }

    /**
     * Boss 倒下时把召唤物带走（一次性）。
     *
     * <p>只清带 {@code hm_boss_minion} 标记的（也就是本 kit 召出来的），
     * 不会误伤自然刷出来的同种怪。</p>
     */
    private static void clearMinionsOnce(ServerLevel level, CultivationMonsterEntity boss) {
        CompoundTag pd = boss.getPersistentData();
        if (pd.getBoolean("hm_kit_minions_cleared")) {
            return;
        }
        pd.putBoolean("hm_kit_minions_cleared", true);
        for (Mob minion : level.getEntitiesOfClass(Mob.class,
                boss.getBoundingBox().inflate(48.0D),
                m -> m != boss && m.getPersistentData().getBoolean(TAG_MINION))) {
            level.sendParticles(ParticleTypes.POOF,
                    minion.getX(), minion.getY() + minion.getBbHeight() * 0.5D, minion.getZ(),
                    8, 0.3D, 0.4D, 0.3D, 0.02D);
            minion.discard();
        }
    }

    /**
     * 按 id 解析实体类型；找不到返回 null（调用处必须判空）。
     *
     * <p>用 {@code ForgeRegistries.ENTITY_TYPES.getValue} 而不是
     * {@code BuiltInRegistries...getOptional}：前者返回可空值、语义直接，
     * 也不用在服务端构造 {@code Optional}（这条路径每只 Boss 每次召唤才走一次，无热路径问题）。</p>
     */
    private static EntityType<?> resolveType(String id) {
        try {
            if (id == null || id.isBlank()) {
                return null;
            }
            String namespace = HongmengMod.MOD_ID;
            String path = id;
            int colon = id.indexOf(':');
            if (colon > 0) {
                namespace = id.substring(0, colon);
                path = id.substring(colon + 1);
            }
            ResourceLocation rl = ResourceLocation.tryBuild(namespace, path);
            if (rl == null) {
                return null;
            }
            return net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(rl);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void announce(ServerLevel level, Vec3 at, String text, double radius) {
        for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(at) <= radius * radius)) {
            sp.displayClientMessage(Component.literal(text), false);
        }
    }

    private static void addPermanentModifier(LivingEntity entity, Attribute attr,
                                             UUID id, String name, double amount) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst == null || inst.getModifier(id) != null) {
            return;
        }
        inst.addPermanentModifier(new AttributeModifier(id, name, amount,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    // ==================================================================
    // 10. 解析 / 小工具
    // ==================================================================

    /** 实体注册名（{@code hongmeng:epoch_boss} → {@code epoch_boss}）。 */
    public static String idOf(LivingEntity entity) {
        if (entity == null) {
            return "";
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? "" : key.getPath();
    }

    private static State stateOf(CompoundTag pd) {
        int ord = pd.getInt(TAG_STAGE);
        State[] all = State.values();
        if (ord < 0 || ord >= all.length) {
            return State.IDLE;
        }
        return all[ord];
    }

    private static Attack attackOf(int ord) {
        Attack[] all = Attack.values();
        if (ord < 0 || ord >= all.length) {
            return null;
        }
        return all[ord];
    }

    private static Attack[] parseAttacks(String csv) {
        if (csv == null || csv.isBlank()) {
            return new Attack[0];
        }
        List<Attack> out = new ArrayList<>();
        for (String name : csv.split(",")) {
            String n = name.trim();
            if (n.isEmpty()) {
                continue;
            }
            try {
                out.add(Attack.valueOf(n));
            } catch (IllegalArgumentException ignored) {
                // 表里写错的攻击名跳过（运行时少一招，不崩）
            }
        }
        return out.toArray(new Attack[0]);
    }

    private static Attack[] concat(Attack[] a, Attack[] b) {
        if (b.length == 0) {
            return a;
        }
        Attack[] out = new Attack[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
