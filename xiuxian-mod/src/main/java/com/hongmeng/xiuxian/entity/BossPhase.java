package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * <b>Boss 三阶段形态</b>（玩家要求）。
 *
 * <p>玩家原话：「boss 有二阶段，三阶段什么的，比如第一阶段是人形，
 * 第二阶段就是半人半兽或者是魔化，第三阶段就是堕落或者是完全兽化」，
 * 并补充：「血魔 boss 就可以按人形、魔化、然后彻底堕落；虚空的话就是人形、
 * 然后半兽、然后第三阶段解放全部兽形态」。</p>
 *
 * <table border="1">
 *   <caption>阶段划分（按血量百分比）</caption>
 *   <tr><th>阶段</th><th>触发血量</th><th>形态</th><th>表现</th></tr>
 *   <tr><td>一</td><td>100%~66%</td><td>人形</td><td>标准体型与颜色</td></tr>
 *   <tr><td>二</td><td>66%~33%</td><td>魔化 / 半兽</td><td>体型 +18%、颜色转紫黑、
 *       长出角与翼、攻击附带元素、开始远程</td></tr>
 *   <tr><td>三</td><td>33%~0%</td><td>完全兽化 / 堕落</td><td>体型 +42%、
 *       全场压暗、持续回血、范围法术、移速暴涨</td></tr>
 * </table>
 *
 * <h2>怎么"变形态"而不换实体</h2>
 * <p>换实体会丢掉仇恨、掉落物、血条连续性，还会让玩家看到"boss 突然消失又出现"。
 * 所以这里用**叠加属性修饰符 + 缩放 + 染色 + 生成装饰部件粒子**的方式做形态变化 ——
 * 一个实体走完全程，血条不断。</p>
 */
public final class BossPhase {

    /** 阶段二的起始血量比例。 */
    private static final double PHASE2 = 0.66D;
    /** 阶段三的起始血量比例。 */
    private static final double PHASE3 = 0.33D;

    private static final UUID HP2 = UUID.nameUUIDFromBytes(
            "hongmeng_phase2_hp".getBytes(StandardCharsets.UTF_8));
    private static final UUID ATK2 = UUID.nameUUIDFromBytes(
            "hongmeng_phase2_atk".getBytes(StandardCharsets.UTF_8));
    private static final UUID SPD2 = UUID.nameUUIDFromBytes(
            "hongmeng_phase2_spd".getBytes(StandardCharsets.UTF_8));
    private static final UUID HP3 = UUID.nameUUIDFromBytes(
            "hongmeng_phase3_hp".getBytes(StandardCharsets.UTF_8));
    private static final UUID ATK3 = UUID.nameUUIDFromBytes(
            "hongmeng_phase3_atk".getBytes(StandardCharsets.UTF_8));
    private static final UUID SPD3 = UUID.nameUUIDFromBytes(
            "hongmeng_phase3_spd".getBytes(StandardCharsets.UTF_8));

    private BossPhase() {
    }

    /**
     * 当前阶段（1 / 2 / 3）。
     *
     * <p><b>优先读同步字段</b>（{@code CultivationMonsterEntity#bossPhase()}）——
     * persistentData 不会下发到客户端，而阶段附件要在客户端模型里切；
     * 读不到同步值时再回退到 persistentData（老存档 / 服务端路径兼容）。</p>
     */
    public static int phaseOf(LivingEntity boss) {
        if (boss instanceof CultivationMonsterEntity monster) {
            int synced = monster.bossPhase();
            if (synced > 0) {
                return synced;
            }
        }
        return boss.getPersistentData().getInt("hongmeng_boss_phase");
    }

    /**
     * 每 tick 检查（由 {@code CultivationMonsterEntity.tick()} 转发）。
     *
     * <p>只在 BOSS / 首领品阶上生效。</p>
     */
    public static void tick(CultivationMonsterEntity boss) {
        // 「恶」有自己的一套阶段/转场（EvilGazerEntity.beginPhaseTransition + EvilAbilities 7 招），
        // 不走通用 BossPhase：否则会与它的 66%/33% 转场叠成"回满血+自定格+自冲击波"。
        // 阶段同步由 EvilGazerEntity.setPhase -> setBossPhase 负责；巨化数据恶 = 1.00，无需这里设置。
        if (boss instanceof EvilGazerEntity) {
            return;
        }
        MobRank rank = boss.effectiveRank();
        if (rank != MobRank.BOSS && rank != MobRank.OVERLORD) {
            return;
        }
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        double max = boss.getMaxHealth();
        if (max <= 0.0D) {
            return;
        }
        int phase = phaseOf(boss);
        if (phase <= 0) {
            boss.getPersistentData().putInt("hongmeng_boss_phase", 1);
            phase = 1;
            // ════════════════════════════════════════════════════════════════
            // B · 巨化（第四轮 §2.1）**全项目唯一的 giant 设置点**
            //   祭坛四 Boss 1.50 / 焚天炎帝·虚空龙帝 1.30 / 仙庭之主 1.35 / 恶 1.00
            //   设置一次后不再覆盖（存档重载由 persistentData 恢复；仙庭之主构造器
            //   若也调 setGiantScale(1.35f) 值相同、无害）。
            //   ⚠ 与 A（战斗系统）的分工：这里只加这几行，不碰攻击/阶段逻辑。
            // ════════════════════════════════════════════════════════════════
            if (boss.giantScale() <= 1.001F) {
                boss.setGiantScale(giantScaleFor(boss));
            }
        }
        // 同步给客户端（模型/渲染器读它切阶段附件；老存档在这里补一次）
        if (boss.bossPhase() != phase) {
            boss.setBossPhase(phase);
        }
        // 存档重载：entityData 不持久化，从 persistentData 把入场/阶段倍率补回同步字段
        float savedScale = boss.getPersistentData().getFloat("hongmeng_boss_scale");
        if (savedScale > 1.001F && boss.bossRenderScale() < 1.001F) {
            boss.setBossRenderScale(savedScale);
        }
        double frac = boss.getHealth() / max;
        if (phase == 1 && frac <= PHASE2) {
            enter(level, boss, 2);
        } else if (phase == 2 && frac <= PHASE3) {
            enter(level, boss, 3);
        }
        // 阶段三：持续回血（"堕落者以血养身"）
        if (phase == 3 && level.getGameTime() % 40L == 0L) {
            boss.heal((float) (max * 0.012D));
        }
    }

    /** 进入某个阶段：叠属性、放大体型、换色、放特效、全场播报。 */
    /** 转阶段时的定格时长（tick）—— 1 秒，够看清但不打断节奏。 */
    public static final int PHASE_FREEZE_TICKS = 20;

    /**
     * 这只 Boss 的<b>巨化倍率</b>（第四轮 §2.1 数值表，全项目唯一映射）。
     *
     * <p>按<b>实体注册名</b>映射（不是类名）—— 这样不需要引用别的 agent 的实体类，
     * 也不会把"被 hm_rank_override 降级成杂兵的 Boss 实体"误判成巨化 Boss
     * （降级的那些 {@link #tick} 早就 return 了，根本走不到这里）。</p>
     *
     * <pre>
     *   epoch_boss / beast_boss / demon_boss / void_boss  → 1.50  （四个祭坛 Boss）
     *   blaze_emperor / void_dragon_emperor               → 1.30  （下界 / 末地世界 Boss）
     *   heaven_lord                                       → 1.35  （天外天首领）
     *   evil_gazer                                        → 1.00  （本来就巨，不放大）
     *   其它                                              → 1.00  （安全兜底）
     * </pre>
     */
    private static float giantScaleFor(CultivationMonsterEntity boss) {
        try {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(boss.getType()).getPath();
            return switch (id) {
                case "epoch_boss", "beast_boss", "demon_boss", "void_boss" -> 1.50F;
                case "blaze_emperor", "void_dragon_emperor" -> 1.30F;
                case "heaven_lord" -> 1.35F;
                case "evil_gazer" -> 1.00F;   // 本来就巨，契约要求不放大
                default -> 1.0F;              // 其它（含被降级成杂兵的 Boss 实体）
            };
        } catch (Throwable t) {
            return 1.0F;
        }
    }

    private static void enter(ServerLevel level, CultivationMonsterEntity boss, int phase) {
        try {
            boss.getPersistentData().putInt("hongmeng_boss_phase", phase);
            // 阶段同步给客户端（客户端模型按 phase 切柜门/齿轮/面具/裂纹等附件）
            boss.setBossPhase(phase);
            // ---- 战斗系统联动（第四轮）----
            // 转阶段会定格 1 秒并回满血：这时如果它还停在"举拳前摇"里，定格结束会立刻
            // 砸下来，玩家等于被白打一下。所以这里清前摇 + 重置冷却，让它从新阶段重新起手。
            BossAttackKit.reset(boss);
            Vec3 at = boss.position();
            boolean third = phase >= 3;

            // ---- 属性：叠乘（永久修饰符，不刷新就一直在）----
            double hpMul = third ? 0.55D : 0.30D;
            double atkMul = third ? 0.50D : 0.25D;
            double spdMul = third ? 0.45D : 0.18D;
            addMod(boss, Attributes.MAX_HEALTH, third ? HP3 : HP2,
                    "hm_phase_hp", hpMul);
            addMod(boss, Attributes.ATTACK_DAMAGE, third ? ATK3 : ATK2,
                    "hm_phase_atk", atkMul);
            addMod(boss, Attributes.MOVEMENT_SPEED, third ? SPD3 : SPD2,
                    "hm_phase_spd", spdMul);
            boss.setHealth(boss.getMaxHealth());

            // ---- 形态外观：体型放大（客户端渲染读取，见 BossPhaseRenderer）----
            //
            // ⚠ 必须**乘在已有倍率上**：世界 Boss 入场时 NativeBossSpawner 已经把倍率设成
            // 1.55/1.75；如果这里直接覆盖成 1.18/1.42，Boss 转阶段时会"缩水"。
            // 上限 2.6 兜底，防止 1.75×1.42×… 叠到离谱。
            float baseScale = boss.bossRenderScale();
            if (baseScale < 1.001F) {
                baseScale = boss.getPersistentData().getFloat("hongmeng_boss_scale");
            }
            if (baseScale < 1.001F) {
                baseScale = 1.0F;
            }
            float targetScale = Math.min(2.6F, baseScale * (third ? 1.42F : 1.18F));
            boss.getPersistentData().putFloat("hongmeng_boss_scale", targetScale);
            // 同步给客户端（BossPhaseRenderer 在客户端读倍率；persistentData 不同步）
            boss.setBossRenderScale(targetScale);
            boss.getPersistentData().putInt("hongmeng_boss_form",
                    third ? 3 : 2);

            // ---- 阶段增益：抗性 + 力量 +（三阶段）迅捷 ----
            boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    20 * 20, third ? 2 : 1, false, false));
            boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                    20 * 25, third ? 2 : 1, false, false));
            if (third) {
                boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        20 * 30, 2, false, false));
                // 2026-11-07 玩家要求：去除 Boss 外圈白框（GLOWING 的原版描边）。
            }

            // 转阶段音效（自行合成：55Hz 长音 + 低频隆隆）
            try {
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        com.hongmeng.xiuxian.ModSounds.PHASE.get(),
                        SoundSource.HOSTILE, 1.4F, 1.0F);
            } catch (Throwable ignored) {
                // 音效失败不影响转阶段
            }

            // ---- 转阶段演出：定格 + 免伤 + 气浪击退 + 喊话 ----
            //
            // 原来 enter() 是**一帧内完成**的：改属性、回满血、加药水、播特效。
            // 玩家正在躲技能，很可能**没注意到换形态了**。
            // 这里把它变成一次"事件"：它停 1 秒，玩家也停下来看。
            try {
                boss.setNoAi(true);
                // 免伤：转阶段会回满血，不免伤的话这 1 秒里血会被打回去
                boss.setInvulnerable(true);
                com.hongmeng.xiuxian.combat.StunScheduler.after(level,
                        PHASE_FREEZE_TICKS, () -> {
                            try {
                                boss.setNoAi(false);
                                boss.setInvulnerable(false);
                            } catch (Throwable ignored) {
                                // 实体已移除
                            }
                        });
                // 气浪：把附近玩家推开（"被冲击波掀了一下"）
                for (ServerPlayer p : level.players()) {
                    if (p.distanceToSqr(boss) > 64.0D) {
                        continue;
                    }
                    Vec3 push = p.position().subtract(boss.position());
                    if (push.lengthSqr() < 0.01D) {
                        push = new Vec3(0.0D, 0.0D, 1.0D);
                    }
                    push = push.normalize().scale(0.85D);
                    p.setDeltaMovement(p.getDeltaMovement().add(push.x, 0.35D, push.z));
                    p.hurtMarked = true;
                    // 喊话：让玩家知道"它变了"
                    p.displayClientMessage(Component.literal(
                            "\u00a74\u00a7l" + boss.getName().getString()
                                    + " \u00a7c"
                                    + (third ? "彻底堕落！" : "魔化！")), false);
                }
            } catch (Throwable ignored) {
                // 演出失败不影响属性变化
            }

            // ---- 特效：爆闪 + 冲击环 + 冲击穹顶（复用贴图特效那套）----
            com.hongmeng.xiuxian.technique.SpellFxRuntime.playPhase(level, at,
                    boss.elementForPhaseFx(), third ? 1.8F : 1.2F);

            // ---- 粒子点缀：魔气上冲 ----
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 1.0D, at.z,
                    third ? 90 : 45, 2.2D, 1.4D, 2.2D, 0.06D);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.0D, at.z,
                    2, 1.5D, 0.5D, 1.5D, 0.0D);

            // ---- 全场播报（压迫感）----
            String form = third ? "彻底堕落" : "魔化";
            for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(at)
                    <= 64.0D * 64.0D)) {
                sp.displayClientMessage(Component.literal(
                        "\u00a74" + boss.getName().getString() + " \u00a7c" + form
                                + "\u00a77了！\u00a78（第 " + phase + " 阶段）"), false);
            }
            com.hongmeng.xiuxian.combat.CombatSounds.bossRoar(level, at);
            HongmengMod.LOGGER.info("[boss] {} 进入第 {} 阶段（形态 {}）",
                    boss.getName().getString(), phase, form);
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[hongmeng] boss 阶段切换失败：{}", t.toString());
        }
    }

    private static void addMod(LivingEntity e, net.minecraft.world.entity.ai.attributes.Attribute attr,
                               UUID id, String name, double mul) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null || inst.getModifier(id) != null) {
            return;
        }
        inst.addPermanentModifier(new AttributeModifier(id, name, mul,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    /** 把周围生物震开（阶段切换的冲击感）。 */
    private static void knock(ServerLevel level, Vec3 at, double radius, double power) {
        AABB box = new AABB(at.x - radius, at.y - 4.0D, at.z - radius,
                at.x + radius, at.y + 4.0D, at.z + radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> !(x instanceof ServerPlayer))) {
            Vec3 away = e.position().subtract(at);
            if (away.lengthSqr() < 0.01D) {
                continue;
            }
            Vec3 dir = away.normalize().scale(power);
            e.push(dir.x, 0.7D, dir.z);
            e.hurtMarked = true;
        }
    }
}
