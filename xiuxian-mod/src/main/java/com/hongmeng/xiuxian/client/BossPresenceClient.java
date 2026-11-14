package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.combat.CombatFeel;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Boss 巨物的"存在感"</b>（第四轮 §2.2）。
 *
 * <p>巨化（{@code giantScale}）解决的是"体型"，这个类解决的是"<b>它一站起来，
 * 整个世界都在抖</b>"。全部在<b>客户端</b>做，不需要改 ClientSetup（
 * {@link Mod.EventBusSubscriber} 自动注册），也不需要任何新包。</p>
 *
 * <h2>五层压迫</h2>
 * <ol>
 *   <li><b>低鸣震动</b>：40 格内最高大的 BOSS/OVERLORD 按体型分档，周期性调用
 *       {@link CombatShake#punch} —— 轻压 40t / 重压 25t / 极重压 15t；
 *       FOV 用 {@link CombatShake#punchFov} 做"蓄压"（跟随 hongmeng-client.toml 的开关）；</li>
 *   <li><b>招式判定震动</b>：读同步字段 {@code bossState()/bossAttack()}（A 的
 *       {@code BossAttackKit} 写）——判定帧 {@code state==2} 按招式给不同力度的震动
 *       （slam 重档 / cataclysm·charge 中档 / 其它中低档），前摇给一下轻微前置震动；</li>
 *   <li><b>脚下尘</b>：每 8t 在巨物脚下一圈客户端粒子（{@code CLOUD}，极重压追加 {@code LARGE_SMOKE}），
 *       每 30t 一声低频踏步（{@code WARDEN_STEP}；极重压再叠 {@code WARDEN_HEARTBEAT}）；</li>
 *   <li><b>威压暗角</b>：重压/极重压时额外压暗屏幕四边（与 {@code BossFightAtmosphere}
 *       的"距离暗角"叠加的是<b>体型</b>这一维，画在本类里，不改那个类）；</li>
 *   <li><b>阶段入场</b>：读同步的 {@code bossPhase()}，2/3 阶段各一次 title/subtitle +
 *       最强一档镜头震动（BOSS 条的文字/颜色在服务端 {@code CultivationMonsterEntity} 里）。</li>
 * </ol>
 *
 * <h2>分档口径</h2>
 * <p>契约 §2.2 的口径是"按 {@code getBbHeight()} 分档（≥4/≥8/≥12 格）"。
 * 这里用<b>威压身高</b> = {@code getBbHeight() × 品阶 scaleY × giantScale} 分档
 * （见 {@link #presenceHeight}）：hitbox 高度对 epoch 只有 3.90 格、对仙庭之主只有 9.32 格，
 * 直接分档会让祭坛 Boss 掉出轻压档、仙庭之主进不了极重压档；乘上品阶与巨化后才是
 * "玩家看到的压迫感"。阈值仍是 4 / 8 / 12 格。</p>
 *
 * <p>所有阈值都只是"要不要抖"的开关；屏幕震动本身还受
 * {@link CombatShake#enabled()}（无障碍开关）与强度配置控制。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossPresenceClient {

    /** 感知半径（格）。 */
    private static final double RANGE = 40.0D;

    /** 分档高度（格）：轻压 / 重压 / 极重压。 */
    private static final float TIER_LIGHT = 4.0F;
    private static final float TIER_HEAVY = 8.0F;
    private static final float TIER_EXTREME = 12.0F;

    /** 分档周期（tick）：越重越频。 */
    private static final int PERIOD_LIGHT = 40;
    private static final int PERIOD_HEAVY = 25;
    private static final int PERIOD_EXTREME = 15;

    /** 脚下尘 / 低频音的周期（tick）。 */
    private static final int DUST_PERIOD = 8;
    private static final int SOUND_PERIOD = 30;

    /** 平滑后的压迫强度（0~3；3 = 极重压贴脸）。 */
    private static float strength;

    /**
     * 玩家 2026-11-02：除 Boss 召唤/登场与自己挨打外，其他镜头反馈全关。
     * 关掉的是"常驻威压"和"招式判定"的镜头震动/FOV；
     * 暗角、低频音、尘土、阶段标题、恶的判定红闪都保留。
     */
    private static final boolean PRESENCE_CAMERA_ENABLED = false;
    private static final boolean ATTACK_CAMERA_ENABLED = false;

    /** 威压暗角起始强度：只有"重压"及以上才额外压暗（轻压交给 BossFightAtmosphere）。 */
    private static final float VIGNETTE_MIN = 1.55F;

    /** 当前感知到的最近巨物 + 它的压迫色。 */
    private static LivingEntity current;
    private static float[] tint = {1.0F, 0.25F, 0.22F};

    /** 每只 Boss 上一次看到的阶段（id → phase），用于"刚转阶段"检测。 */
    private static final Map<Integer, Integer> LAST_PHASE = new ConcurrentHashMap<>();

    /** 每只 Boss 上一次看到的招式状态（id → (state<<8)|attack），用于"判定帧沿触发"。 */
    private static final Map<Integer, Integer> LAST_ATTACK = new ConcurrentHashMap<>();

    /** 节流计数。 */
    private static int ticks;

    private BossPresenceClient() {
    }

    // ------------------------------------------------------------------
    // 每客户端 tick
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) {
            reset();
            return;
        }
        ticks++;
        LivingEntity best = null;
        float bestH = 0.0F;
        com.hongmeng.xiuxian.entity.EvilGazerEntity skyEvil = null;
        try {
            for (var e : mc.level.entitiesForRendering()) {
                if (!(e instanceof CultivationMonsterEntity cm) || !cm.isAlive()
                        || cm.isInvisible()) {
                    continue;
                }
                // 第五轮：恶的"小眼/爪牙"不算 BOSS 压迫（B 会把它降成 ELITE/NORMAL）
                if (cm instanceof com.hongmeng.xiuxian.entity.EvilGazerEntity eg
                        && EvilGazerRenderer.isMinion(eg)) {
                    continue;
                }
                // 天空巨眼的状态喂给 EvilSkyRenderer：不受 40 格感知半径限制（演出要一直连续）
                if (cm instanceof com.hongmeng.xiuxian.entity.EvilGazerEntity eg) {
                    skyEvil = eg;
                }
                MobRank r = cm.effectiveRank();
                if (r != MobRank.BOSS && r != MobRank.OVERLORD) {
                    continue;
                }
                if (cm.distanceToSqr(mc.player) > RANGE * RANGE) {
                    continue;
                }
                float h = presenceHeight(cm);
                if (h > bestH) {
                    bestH = h;
                    best = cm;
                }
            }
        } catch (Throwable ignored) {
            return;
        }
        if (skyEvil != null) {
            EvilSkyRenderer.setBossAttackState(skyEvil.bossState(), skyEvil.bossAttack());
        } else {
            EvilSkyRenderer.setBossAttackState((byte) 0, (byte) 0);
        }
        if (best == null) {
            current = null;
            strength += (0.0F - strength) * 0.10F;
            if (strength < 0.01F) {
                strength = 0.0F;
                LAST_PHASE.clear();
                LAST_ATTACK.clear();
            }
            return;
        }
        current = best;
        // 每 200t 清一次历史缓存（只留当前这只），避免长时间游戏后 Map 里堆一堆已死实体
        if (ticks % 200 == 0) {
            prunePhaseCache(best.getId());
        }
        tint = colorOf(best);
        int tier = tierOf(bestH);
        boolean evil = best instanceof com.hongmeng.xiuxian.entity.EvilGazerEntity;
        if (evil) {
            // 契约 §4.5：恶（模型约 13 格 + 阶段放大）单独抬到"极重压"档
            tier = Math.max(tier, 3);
        }
        double dist = Math.sqrt(best.distanceToSqr(mc.player));
        float near = (float) Mth.clamp((RANGE - dist) / (RANGE - 6.0D), 0.0D, 1.0D);
        // 平滑：越近越强；高档位再加权（极重压的近身威慑最强）
        float target = tier * (0.45F + 0.55F * near);
        strength += (target - strength) * 0.08F;

        if (tier > 0) {
            // 第五轮：恶低血量 / 阶段 3 → 心跳更频（判定节奏加快，压迫感升级）
            boolean evilEnraged = false;
            if (best instanceof com.hongmeng.xiuxian.entity.EvilGazerEntity eg) {
                evilEnraged = EvilGazerRenderer.phaseOf(eg) >= 3
                        || eg.getHealth() / Math.max(1.0F, eg.getMaxHealth()) < 0.25F;
            }
            int period = tier >= 3 ? PERIOD_EXTREME : tier >= 2 ? PERIOD_HEAVY : PERIOD_LIGHT;
            if (evilEnraged) {
                period = Math.max(8, period / 2);
            }
            if (ticks % period == 0 && PRESENCE_CAMERA_ENABLED) {
                CombatFeel.Shake shake = tier >= 3 ? CombatFeel.Shake.HEAVY
                        : tier >= 2 ? CombatFeel.Shake.MEDIUM : CombatFeel.Shake.LIGHT;
                double mult = (tier >= 3 ? 1.15D : tier >= 2 ? 1.00D : 0.80D)
                        * (0.55D + 0.45D * near)
                        * (evilEnraged ? 1.15D : 1.0D);
                CombatShake.punch(shake, mult);
                // FOV 蓄压（是否生效由 hongmeng-client.toml 的 combat.fovPunch 决定）
                CombatShake.punchFov(0.010F * tier * (0.5F + 0.5F * near));
            }
            if (ticks % DUST_PERIOD == 0) {
                spawnDust(mc, best, tier);
            }
            if (ticks % (evilEnraged ? 16 : SOUND_PERIOD) == 0) {
                playLowRumble(mc, best, tier);
            }
            if (!evil) {
                // 恶的阶段冲击走 EvilSky 阶段同步 + EvilFlashOverlay（见 §4.4），这里不重复播报
                watchPhase(mc, best);
            }
            watchAttack(mc, best);
        } else {
            // 不够大：清掉阶段/招式缓存，避免下次误报"刚转阶段 / 刚出招"
            prunePhaseCache(best.getId());
        }
    }

    /** 分档：轻 ≥4 / 重 ≥8 / 极重 ≥12 格（用渲染身高近似，见类注释）。 */
    private static int tierOf(float h) {
        if (h >= TIER_EXTREME) {
            return 3;
        }
        if (h >= TIER_HEAVY) {
            return 2;
        }
        return h >= TIER_LIGHT ? 1 : 0;
    }

    /**
     * 巨物的"威压身高"（格），用于 4/8/12 分档。
     *
     * <pre>
     *   威压身高 = getBbHeight()（已含巨化 ×min(2.2, giant)）
     *            × 品阶 scaleY（渲染器会乘的纵向倍率）
     *            × giantScale（观感加权，见下）
     * </pre>
     *
     * <p><b>为什么 giant 乘两次</b>：hitbox 里的 giant 只到 {@code min(2.2, giant)}，而契约要求
     * 仙庭之主（base 6.9 格、OVERLORD×1.2、giant 1.35）巨化后必须命中"极重压"档；
     * 只按裸 {@code getBbHeight()} 算只有 9.3 格（重压档），再乘一次巨化倍率后
     * 9.3×1.2×1.35≈**15.1 格 → 极重压**。这是"威压/压迫感"的观感加权，不参与命中判定
     * （命中箱只走 {@code CultivationMonsterEntity.getDimensions}）。</p>
     */
    private static float presenceHeight(LivingEntity e) {
        float h = e.getBbHeight();
        if (e instanceof CultivationMonsterEntity mob) {
            try {
                h *= Math.max(1.0F, mob.effectiveRank().scaleY());
                // 第五轮：阶段放大（bossRenderScale 1.18/1.42/…）也算进"威压身高"，
                // 这样恶在三阶段会从 13 格长到 22 格（极重压档更稳）。
                float phaseScale = mob.bossRenderScale();
                if (phaseScale > 1.001F) {
                    h *= phaseScale;
                }
                h *= Math.max(1.0F, mob.giantScale());
            } catch (Throwable ignored) {
                // 品阶/巨化取不到就用 hitbox 高度
            }
        }
        return h;
    }

    // ------------------------------------------------------------------
    // 脚下尘 + 低频音
    // ------------------------------------------------------------------

    private static void spawnDust(Minecraft mc, LivingEntity e, int tier) {
        if (mc.level == null) {
            return;
        }
        double x = e.getX();
        double y = e.getY();
        double z = e.getZ();
        float width = Math.max(0.8F, e.getBbWidth() * 0.55F);
        int n = tier >= 2 ? 8 : 4;
        for (int i = 0; i < n; i++) {
            double a = (i + (ticks % 7) * 0.17D) / n * Math.PI * 2.0D;
            double rx = x + Math.cos(a) * width;
            double rz = z + Math.sin(a) * width;
            mc.level.addParticle(ParticleTypes.CLOUD, rx, y + 0.06D, rz,
                    Math.cos(a) * 0.06D, 0.012D * (1 + tier), Math.sin(a) * 0.06D);
            if (tier >= 3) {
                mc.level.addParticle(ParticleTypes.LARGE_SMOKE, rx, y + 0.15D, rz,
                        0.0D, 0.010D, 0.0D);
            }
        }
    }

    private static void playLowRumble(Minecraft mc, LivingEntity e, int tier) {
        if (mc.level == null) {
            return;
        }
        float volume = tier >= 2 ? 1.10F : 0.75F;
        float pitch = tier >= 3 ? 0.45F : 0.55F;
        try {
            mc.level.playLocalSound(e.getX(), e.getY(), e.getZ(),
                    SoundEvents.WARDEN_STEP, SoundSource.HOSTILE,
                    volume, pitch, false);
            if (tier >= 3 && ticks % (SOUND_PERIOD * 2) == 0) {
                // 极重压：一步一心跳（低频，20 格内才听得清）
                mc.level.playLocalSound(e.getX(), e.getY(), e.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE,
                        0.9F, 0.5F, false);
            }
        } catch (Throwable ignored) {
            // 音效失败不影响玩法
        }
    }

    // ------------------------------------------------------------------
    // 阶段入场（title / subtitle）+ BOSS 条文字由服务端负责
    // ------------------------------------------------------------------

    private static void watchPhase(Minecraft mc, LivingEntity e) {
        if (!(e instanceof CultivationMonsterEntity cm)) {
            return;
        }
        int phase = cm.bossPhase();
        Integer last = LAST_PHASE.put(cm.getId(), phase);
        if (last == null || phase <= 1 || phase == last) {
            return;
        }
        try {
            if (mc.gui != null) {
                String name = cm.getName().getString();
                if (phase >= 3) {
                    mc.gui.setTitle(Component.literal("\u00a74\u00a7l" + name
                            + " \u00a7c\u00a7l彻底堕落"));
                    mc.gui.setSubtitle(Component.literal(
                            "\u00a77第三阶段 \u00a78·\u00a77 巨躯压境"));
                } else {
                    mc.gui.setTitle(Component.literal("\u00a75\u00a7l" + name
                            + " \u00a7d\u00a7l魔化"));
                    mc.gui.setSubtitle(Component.literal(
                            "\u00a77第二阶段 \u00a78·\u00a77 巨躯压境"));
                }
                mc.gui.setTimes(8, 55, 12);
            }
            // 2026-11-02 玩家要求：取消 Boss 转阶段运镜。
            // 只保留标题/字幕提示；镜头震动与 FOV 不再触发。
        } catch (Throwable ignored) {
            // 演出失败不影响玩法
        }
    }

    /**
     * 招式判定帧的震动（读 A 的 {@code BossAttackKit} 同步出来的
     * {@code bossState()/bossAttack()}；**只读字段，不碰战斗逻辑**）。
     *
     * <pre>
     *   state 0 idle / 1 windup / 2 active / 3 recover
     *   attack 1 slam → 重档 HEAVY×1.20
     *          7 cataclysm / 8 charge → 中档 MEDIUM×1.15
     *          其它非 0 招式 → MEDIUM×0.90
     *   windup（1）刚进入 → LIGHT×0.45（"要来了"的前置提示）
     * </pre>
     *
     * <p>只在"状态/招式变化"的<b>沿</b>触发一次，否则判定帧每帧都震会晕。</p>
     */
    private static void watchAttack(Minecraft mc, LivingEntity e) {
        if (!(e instanceof CultivationMonsterEntity cm)) {
            return;
        }
        // 第五轮：恶有自己的一套招式 id（见 EvilGazerRenderer.ATTACK_*），单独处理
        if (e instanceof com.hongmeng.xiuxian.entity.EvilGazerEntity eg) {
            watchEvilAttack(mc, eg);
            return;
        }
        byte state = cm.bossState();
        byte attack = cm.bossAttack();
        int now = ((state & 0xFF) << 8) | (attack & 0xFF);
        Integer last = LAST_ATTACK.put(cm.getId(), now);
        int prev = last == null ? -1 : last;
        int prevState = last == null ? -1 : ((prev >> 8) & 0xFF);
        if (!ATTACK_CAMERA_ENABLED) {
            return;
        }
        try {
            if (state == 2 && prevState != 2) {
                // 判定帧：不同招式不同力度
                CombatFeel.Shake shake;
                double mult;
                if (attack == 1) {                       // slam 震地重击
                    shake = CombatFeel.Shake.HEAVY;
                    mult = 1.20D;
                } else if (attack == 7 || attack == 8) { // cataclysm / charge
                    shake = CombatFeel.Shake.MEDIUM;
                    mult = 1.15D;
                } else if (attack != 0) {                // shockwave / summon / spell / breath / ambush / enrage
                    shake = CombatFeel.Shake.MEDIUM;
                    mult = 0.90D;
                } else {
                    shake = CombatFeel.Shake.LIGHT;
                    mult = 0.70D;
                }
                CombatShake.punch(shake, mult);
            } else if (state == 1 && prevState != 1 && attack != 0) {
                // 前摇：轻微前置震动（提示"要来了"）
                CombatShake.punch(CombatFeel.Shake.LIGHT, 0.45D);
            }
        } catch (Throwable ignored) {
            // 震动失败不影响玩法
        }
    }

    /**
     * 「恶」的招式判定：读同步 {@code bossState()/bossAttack()}（映射见
     * {@link EvilGazerRenderer#ATTACK_GAZE} 等常量），只做客户端演出：
     * 重档 → SLAM/METEOR/ENRAGE；中档 → SWEEP/BEAM；轻档 → GAZE/SUMMON；
     * 判定帧额外闪屏、SLAM/METEOR 在地面撒"地裂"粒子。
     */
    private static void watchEvilAttack(Minecraft mc,
                                        com.hongmeng.xiuxian.entity.EvilGazerEntity eg) {
        byte state = eg.bossState();
        byte atk = eg.bossAttack();
        int now = ((state & 0xFF) << 8) | (atk & 0xFF);
        Integer last = LAST_ATTACK.put(eg.getId(), now);
        int prevState = last == null ? -1 : ((last >> 8) & 0xFF);
        try {
            if (state == EvilGazerRenderer.STATE_ACTIVE
                    && prevState != EvilGazerRenderer.STATE_ACTIVE) {
                boolean heavy = atk == EvilGazerRenderer.ATTACK_SLAM
                        || atk == EvilGazerRenderer.ATTACK_METEOR
                        || atk == EvilGazerRenderer.ATTACK_ENRAGE;
                boolean medium = atk == EvilGazerRenderer.ATTACK_SWEEP
                        || atk == EvilGazerRenderer.ATTACK_BEAM;
                if (ATTACK_CAMERA_ENABLED) {
                    CombatFeel.Shake shake = heavy ? CombatFeel.Shake.HEAVY
                            : medium ? CombatFeel.Shake.MEDIUM : CombatFeel.Shake.LIGHT;
                    double mult = heavy ? 1.25D : medium ? 1.10D : 0.80D;
                    CombatShake.punch(shake, mult);
                    CombatShake.punchFov(heavy ? 0.050F : medium ? 0.032F : 0.020F);
                }
                // 判定帧全屏闪一下（血红偏白，短促）
                EvilFlashOverlay.triggerRedFlash();
                if (atk == EvilGazerRenderer.ATTACK_SLAM
                        || atk == EvilGazerRenderer.ATTACK_METEOR) {
                    spawnGroundCracks(mc, eg);
                }
            } else if (state == EvilGazerRenderer.STATE_WINDUP
                    && prevState != EvilGazerRenderer.STATE_WINDUP) {
                // 起手：轻微前置震动（眼睛在 EvilSkyRenderer 里同步变亮）
                if (ATTACK_CAMERA_ENABLED) {
                    CombatShake.punch(CombatFeel.Shake.LIGHT, 0.50D);
                }
            }
        } catch (Throwable ignored) {
            // 演出失败不影响玩法
        }
    }

    /** 地裂粒子：以恶脚下为中心撒一圈裂缝/余烬（纯客户端）。 */
    private static void spawnGroundCracks(Minecraft mc, LivingEntity e) {
        if (mc.level == null) {
            return;
        }
        double x = e.getX();
        double y = e.getY();
        double z = e.getZ();
        float radius = Math.max(2.0F, e.getBbWidth() * 1.6F);
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0D * Math.PI * 2.0D;
            double rr = radius * (0.35D + 0.65D * ((i * 37 % 17) / 17.0D));
            double px = x + Math.cos(a) * rr;
            double pz = z + Math.sin(a) * rr;
            mc.level.addParticle(ParticleTypes.LARGE_SMOKE, px, y + 0.06D, pz,
                    0.0D, 0.06D, 0.0D);
            if (i % 3 == 0) {
                mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, y + 0.10D, pz,
                        0.0D, 0.05D, 0.0D);
            }
        }
    }

    private static void prunePhaseCache(int keepId) {
        try {
            Iterator<Map.Entry<Integer, Integer>> it = LAST_PHASE.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getKey() != keepId) {
                    it.remove();
                }
            }
            Iterator<Map.Entry<Integer, Integer>> it2 = LAST_ATTACK.entrySet().iterator();
            while (it2.hasNext()) {
                if (it2.next().getKey() != keepId) {
                    it2.remove();
                }
            }
        } catch (Throwable ignored) {
            LAST_PHASE.clear();
            LAST_ATTACK.clear();
        }
    }

    // ------------------------------------------------------------------
    // 威压暗角（体型那一维；不改 BossFightAtmosphere 内部）
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (strength < VIGNETTE_MIN || current == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        // 呼吸脉动：让压迫是"活的"
        float pulse = 0.78F + 0.22F * Mth.sin((mc.level.getGameTime()
                + event.getPartialTick()) * 0.26F);
        float k = Mth.clamp(strength / 3.0F, 0.0F, 1.0F) * pulse;
        int baseAlpha = (int) (0x62 * k);          // 最大 ~38% 不透明度
        if (baseAlpha <= 3) {
            return;
        }
        int r = (int) (tint[0] * 255.0F);
        int gg = (int) (tint[1] * 255.0F);
        int b = (int) (tint[2] * 255.0F);
        int bandW = Math.max(6, (int) (w * 0.10F * k));
        int bandH = Math.max(6, (int) (h * 0.12F * k));
        for (int i = 0; i < 3; i++) {
            int a = (int) (baseAlpha * (3 - i) / 3.0F);
            if (a <= 0) {
                continue;
            }
            int argb = (Math.min(255, a) << 24) | (r << 16) | (gg << 8) | b;
            int dx = bandW * i / 3;
            int dy = bandH * i / 3;
            int tw = Math.max(1, bandW / 3);
            int th = Math.max(1, bandH / 3);
            g.fill(dx, dy, w - dx, dy + th, argb);                 // 上
            g.fill(dx, h - dy - th, w - dx, h - dy, argb);         // 下
            g.fill(dx, dy, dx + tw, h - dy, argb);                 // 左
            g.fill(w - dx - tw, dy, w - dx, h - dy, argb);         // 右
        }
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 按 Boss 类型取压迫色（与 BossAuraRenderer / BossFightAtmosphere 同一套语义）。 */
    private static float[] colorOf(LivingEntity e) {
        try {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(e.getType()).getPath();
            if (id.contains("void") || id.contains("dragon")) {
                return new float[]{0.55F, 0.30F, 1.00F};
            }
            if (id.contains("beast") || id.contains("serpent")) {
                return new float[]{1.00F, 0.72F, 0.20F};
            }
            if (id.contains("epoch") || id.contains("puppet")) {
                return new float[]{0.30F, 1.00F, 0.80F};
            }
            if (id.contains("evil") || id.contains("gazer")) {
                return new float[]{0.90F, 0.10F, 0.75F};
            }
            return new float[]{1.00F, 0.25F, 0.22F};
        } catch (Throwable t) {
            return new float[]{1.00F, 0.25F, 0.22F};
        }
    }

    private static void reset() {
        strength = 0.0F;
        current = null;
        LAST_PHASE.clear();
        LAST_ATTACK.clear();
    }
}
