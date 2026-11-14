package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * 命中音效分层（{@code docs/combat-feel-spec.md} §零 第 5 条）。
 *
 * <h2>三层是怎么分的</h2>
 *
 * <pre>
 *   第一层 破空  高、短、尖     —— "有东西飞过来了"
 *   第二层 撞击  低、闷、响     —— "砸实了"（音量随伤害占比涨、音调随之下沉）
 *   第三层 余韵  长、低、悠     —— 只有 10% 以上的命中才有："这一下很重"
 * </pre>
 *
 * <p>规格的"一次命中叠 2~3 层音"就是这三层。全部用<b>原版音效</b>
 * （约束：不下载任何素材）。</p>
 *
 * <h2>为什么平砍不放第一层</h2>
 *
 * <p>近战本来就有原版的挥击/命中音（{@code Player.attack} 里放的），
 * 我们再叠一层破空只会让声音糊掉。所以 {@code meleeHit == true} 时第一层跳过，
 * 只补"撞击"与"余韵"这两层——这正是"打得有分量"所需要的。</p>
 */
public final class CombatSounds {

    /**
     * PERF(B)：<b>每维度每 tick 最多放几组"命中音效"</b>。
     *
     * <p>{@link #impact} 一次命中最多放 4 个音（破空 / 撞击 / 余韵 / 闷雷）。
     * 成片 AOE 一发打几十只怪时，同一 tick 就会向附近玩家连发上百个
     * {@code ClientboundSoundPacket} —— 服务端要构造/广播，客户端音频系统也会被同一 tick
     * 灌满，实际听感只是糊成噪音。超过上限的命中<b>只跳过音效</b>，
     * 粒子（有 ParticleBudget 预算）、伤害、伤害飘字、屏幕震动全部不变。</p>
     */
    public static final int MAX_IMPACT_GROUPS_PER_TICK = 8;

    /** 每维度本 tick 已放了几组命中音：[0]=tick，[1]=已放组数。 */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<
            net.minecraft.world.level.Level>, long[]> IMPACT_COUNT = new java.util.HashMap<>();

    private CombatSounds() {
    }

    /** PERF(B)：这一 tick 还允不允许再放一组命中音。 */
    private static boolean allowImpact(ServerLevel level) {
        var key = level.dimension();
        long now = level.getGameTime();
        long[] slot = IMPACT_COUNT.get(key);
        if (slot == null) {
            // 维度就那几个；超过 32 个直接清空重建，防脏数据把表撑大
            if (IMPACT_COUNT.size() > 32) {
                IMPACT_COUNT.clear();
            }
            slot = new long[]{now, 0L};
            IMPACT_COUNT.put(key, slot);
        }
        if (slot[0] != now) {
            slot[0] = now;
            slot[1] = 0L;
        }
        if (slot[1] >= MAX_IMPACT_GROUPS_PER_TICK) {
            return false;
        }
        slot[1]++;
        return true;
    }

    /** 命中音效是否启用（跟战斗特效总开关走）。 */
    private static boolean on() {
        return ParticleBudget.enabled();
    }

    // ------------------------------------------------------------------
    // 命中：三层
    // ------------------------------------------------------------------

    /**
     * 一次命中的分层音效。
     *
     * @param element  元素（null / NONE → 无属性那套）
     * @param fraction 伤害占目标最大生命的比例
     * @param meleeHit 是不是近战（近战不叠"破空"，因为原版已经放了）
     */
    public static void impact(ServerLevel level, Vec3 at, Element element,
                             double fraction, boolean meleeHit) {
        if (level == null || at == null || !on()) {
            return;
        }
        // PERF(B)：同一 tick 的命中音组数上限（AOE 成群命中时防止音效/包风暴）
        if (!allowImpact(level)) {
            return;
        }
        float t = CombatFeel.clamp01((float) (fraction / CombatFeel.KNOCKBACK_FULL_FRACTION));
        float volume = 0.55F + 0.75F * t;
        // 越重的命中越"低"——这是"分量感"最直接的来源
        float pitch = 1.15F - 0.30F * t;

        // ---- 第一层：破空 ----
        if (!meleeHit) {
            play(level, at, whoosh(element), SoundSource.PLAYERS, volume * 0.8F, pitch * 1.25F);
        }

        // ---- 第二层：撞击 ----
        if (meleeHit) {
            play(level, at, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS,
                    volume * 0.65F, 1.35F - 0.25F * t);
        } else {
            play(level, at, impactOf(element), SoundSource.PLAYERS, volume, pitch);
        }

        // ---- 第三层：余韵（只有重击才有）----
        if (fraction >= CombatFeel.HITSTOP_MEDIUM) {
            play(level, at, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.9F, 0.55F);
        }
        // 25% 以上：天上一声闷雷（"这一下连天都听见了"）
        if (fraction > CombatFeel.HITSTOP_HEAVY) {
            play(level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.7F, 1.35F);
        }
    }

    // ------------------------------------------------------------------
    // 起（前摇）与 承（释放）
    // ------------------------------------------------------------------

    /** 起手：破空前的"吸气"声。 */
    public static void windupStart(ServerLevel level, Vec3 at) {
        if (level == null || at == null || !on()) {
            return;
        }
        play(level, at, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.55F, 0.75F);
    }

    /** 蓄力中的每一次脉冲（音调随进度升高 → "越蓄越满"）。 */
    public static void windupPulse(ServerLevel level, Vec3 at, float progress) {
        if (level == null || at == null || !on()) {
            return;
        }
        float p = CombatFeel.clamp01(progress);
        play(level, at, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                0.18F + 0.22F * p, 0.9F + 0.6F * p);
    }

    /** 释放：一声破空。 */
    public static void cast(ServerLevel level, Vec3 at, Element element) {
        if (level == null || at == null || !on()) {
            return;
        }
        play(level, at, SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.7F, 1.1F);
        play(level, at, whoosh(element), SoundSource.PLAYERS, 0.9F, 1.45F);
    }

    // ------------------------------------------------------------------
    // 大场面
    // ------------------------------------------------------------------

    /** 首领登场：一声低沉的钟 + 一声雷鸣（规格 §3.2）。 */
    /**
     * <b>Boss 出场怒吼</b>（玩家要求「战斗也要有激荡的音效」）。
     *
     * <p>用**原版音效三层叠加**做出"激荡"感 —— 不需要新音频文件，
     * 也就不引入任何外部素材：龙吼（高）+ 凋灵生成（中）+ 劫掠兽咆哮（低），
     * 三层错开音高，合起来比单独一个厚得多。</p>
     */
    public static void bossRoar(ServerLevel level, Vec3 at) {
        if (level == null || at == null) {
            return;
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.2F, 0.7F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.6F, 0.55F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, 1.8F, 0.5F);
    }

    /** <b>Boss 重击落地</b>：爆炸 + 铁砧 + 低音冲击。 */
    public static void bossSlam(ServerLevel level, Vec3 at) {
        if (level == null || at == null) {
            return;
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.55F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.6F, 0.5F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    /** <b>阶段变化</b>：血条过半/濒死时的"激荡"提示（信标 + 钟 + 锚点耗尽）。 */
    public static void phaseShift(ServerLevel level, Vec3 at) {
        if (level == null || at == null) {
            return;
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.8F, 0.7F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.BELL_RESONATE,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.6F, 0.6F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_HURT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 0.8F);
    }

    public static void bossEntrance(ServerLevel level, Vec3 at) {
        if (level == null || at == null || !on()) {
            return;
        }
        play(level, at, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.6F, 0.45F);
        play(level, at, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3.0F, 0.40F);
        play(level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.1F, 0.70F);
    }

    /** 领域展开：一声悠长的"嗡"（规格 §3.3）。 */
    public static void domainHum(ServerLevel level, Vec3 at, Element element) {
        if (level == null || at == null || !on()) {
            return;
        }
        play(level, at, SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 1.5F, 0.60F);
        play(level, at, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 0.65F);
        play(level, at, whoosh(element), SoundSource.PLAYERS, 0.9F, 0.55F);
    }

    /** 玩家被重击：一声闷响（"疼"这件事由 {@code PainFlashPacket} 那套负责）。 */
    public static void playerHurt(ServerLevel level, Vec3 at, double fraction) {
        if (level == null || at == null || !on()) {
            return;
        }
        if (fraction < CombatFeel.HITSTOP_MEDIUM) {
            return;
        }
        play(level, at, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.8F, 0.9F);
        play(level, at, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.7F, 0.5F);
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 第一层（破空）按元素选。 */
    private static SoundEvent whoosh(Element element) {
        if (element == null) {
            return SoundEvents.PLAYER_ATTACK_SWEEP;
        }
        return switch (element) {
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case ICE -> SoundEvents.SNOW_BREAK;
            case THUNDER -> SoundEvents.TRIDENT_THUNDER;
            case WIND -> SoundEvents.PHANTOM_SWOOP;
            case METAL -> SoundEvents.TRIDENT_HIT_GROUND;
            case WOOD -> SoundEvents.GRASS_BREAK;
            case WATER -> SoundEvents.PLAYER_SPLASH;
            case EARTH -> SoundEvents.GRAVEL_BREAK;
            case NONE -> SoundEvents.PLAYER_ATTACK_SWEEP;
        };
    }

    /** 第二层（撞击）按元素选。 */
    private static SoundEvent impactOf(Element element) {
        if (element == null) {
            return SoundEvents.WARDEN_ATTACK_IMPACT;
        }
        return switch (element) {
            case FIRE -> SoundEvents.GENERIC_EXPLODE;
            case ICE -> SoundEvents.GLASS_BREAK;
            case THUNDER -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case WIND -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case METAL -> SoundEvents.ANVIL_LAND;
            case WOOD -> SoundEvents.WOOD_BREAK;
            case WATER -> SoundEvents.PLAYER_SPLASH;
            case EARTH -> SoundEvents.STONE_BREAK;
            case NONE -> SoundEvents.WARDEN_ATTACK_IMPACT;
        };
    }

    /** 第三层（余韵）按元素选（大招时叠在钟声之外）。 */
    public static SoundEvent resonanceOf(Element element) {
        if (element == null) {
            return SoundEvents.BELL_RESONATE;
        }
        return switch (element) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case ICE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case THUNDER -> SoundEvents.TRIDENT_THUNDER;
            case WIND -> SoundEvents.PHANTOM_FLAP;
            case METAL -> SoundEvents.IRON_GOLEM_ATTACK;
            case WOOD -> SoundEvents.BAMBOO_BREAK;
            case WATER -> SoundEvents.CONDUIT_AMBIENT;
            case EARTH -> SoundEvents.ROOTED_DIRT_BREAK;
            case NONE -> SoundEvents.BELL_RESONATE;
        };
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound,
                             SoundSource source, float volume, float pitch) {
        try {
            level.playSound(null, at.x, at.y, at.z, sound, source, volume, pitch);
        } catch (Throwable ignored) {
            // 音效失败不该影响战斗结算
        }
    }
}
