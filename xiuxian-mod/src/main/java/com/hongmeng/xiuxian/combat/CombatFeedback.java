package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import com.hongmeng.xiuxian.network.CombatFeedbackPacket;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 战斗反馈总线：**一次命中要发生的所有事情，都在这里按规格的顺序发生**。
 *
 * <h2>顺序（就是规格 §零 的性价比排序）</h2>
 *
 * <ol>
 *   <li><b>顿帧</b>（{@link HitstopController}）—— 冻结实体，绝不暂停 tick；</li>
 *   <li><b>屏幕震动</b> —— 服务端<b>只发事实</b>（伤害占比），客户端自己决定震多强、震不震
 *       （所以"关闭屏幕震动"是无障碍开关，服务端不用知道）；</li>
 *   <li><b>命中闪白</b> —— 原版 {@code hurtTime} 红闪拉长 + 命中点一颗 {@code FLASH} 白闪；</li>
 *   <li><b>击退</b> —— 冲量在顿帧<b>结束</b>时才打出去（"先定住，再打飞"）；</li>
 *   <li><b>音效分层</b> —— 破空 / 撞击 / 余韵（{@link CombatSounds}）；</li>
 *   <li><b>粒子爆发</b> —— 只在<b>命中点</b>放射，走预算（{@link CombatParticles}）；</li>
 *   <li><b>FOV 冲击 / 伤害飘字</b> —— 客户端（{@code client.DamageNumbers} / {@code client.CombatShake}）。</li>
 * </ol>
 *
 * <h2>为什么"命中闪白"要排程到下一 tick</h2>
 *
 * <p>看过字节码：{@code LivingEntity.hurt} 里 {@code actuallyHurt}（也就是
 * {@code LivingDamageEvent} 的触发点）执行<b>之后</b>才写
 * {@code hurtDuration = 10; hurtTime = 10;}（字节码 323/331）。
 * 所以在事件里改会被原版覆盖掉——必须晚一 tick 再改。这也是"加强"而不是"替代"：
 * 原版那 10 tick 红闪照旧，我们在第 2 tick 把它接到 10~18 tick。</p>
 *
 * <h2>与软上限 / 减伤的关系</h2>
 *
 * <p>本类<b>不改伤害</b>：{@code LivingDamageEvent#getAmount()} 只是被读。
 * 怪物那边的 35% 软上限（{@code CultivationMonsterEntity.doHurtTarget}）与
 * 境界压制减伤（{@code hurt}）都已经结算完了，这里拿到的是最终值——
 * 所以"打出了多少比例"这件事和玩家看到掉的血是一致的。</p>
 */
public final class CombatFeedback {

    /** 每个玩家每 tick 最多收几包命中表现（AOE 打一群怪时防止刷屏刷包）。 */
    public static final int MAX_PACKETS_PER_TICK = 4;

    /** 伤害占比 → 红闪延长后的总时长（tick）。 */
    private static final int FLASH_BASE_TICKS = 10;
    private static final int FLASH_MAX_TICKS = 18;

    /** 元素提示：法术命中时由 {@code TechniqueCaster} 塞进来，用于选粒子/音效。 */
    private static final ThreadLocal<Element> ELEMENT_HINT = new ThreadLocal<>();

    /** 每 tick 已发包数。 */
    private static final Map<UUID, long[]> PACKET_COUNT = new HashMap<>();

    /** 首次命中打过日志没有（每个服务端会话只打一行，用来证明管线真的在跑）。 */
    private static boolean announced = false;

    /** 已经打过日志的最大顿帧档（0/2/4/6；只在"更强的一档第一次出现"时再打一行）。 */
    private static int maxStopLogged = -1;

    private CombatFeedback() {
    }

    // ------------------------------------------------------------------
    // 元素提示（法术命中）
    // ------------------------------------------------------------------

    /** 设元素提示（配合 {@link #clearElementHint()}，务必 try/finally）。 */
    public static void setElementHint(Element element) {
        if (element == null) {
            ELEMENT_HINT.remove();
        } else {
            ELEMENT_HINT.set(element);
        }
    }

    /** 清元素提示。 */
    public static void clearElementHint() {
        ELEMENT_HINT.remove();
    }

    private static Element currentElement() {
        return ELEMENT_HINT.get();
    }

    // ------------------------------------------------------------------
    // 主入口：一次伤害结算
    // ------------------------------------------------------------------

    /**
     * 由 {@code LivingDamageEvent} 调（服务端）。
     *
     * @param victim 受击者
     * @param source 伤害来源
     * @param amount 最终伤害（已经过护甲/减伤/软上限）
     */
    public static void onDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim == null || source == null || amount <= 0.0F) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (!ParticleBudget.enabled()) {
            return;
        }
        Element element = currentElement();
        double fraction = CombatFeel.damageFraction(amount, victim.getMaxHealth());
        LivingEntity attacker = attackerOf(source);
        Vec3 hitPoint = victim.position().add(0.0D, victim.getBbHeight() * 0.55D, 0.0D);

        // ---- 1) 顿帧 + 3) 闪白 + 4) 击退 ----
        int stopTicks = 0;
        if (attacker != null && attacker != victim) {
            Vec3 impulse = knockbackImpulse(victim, attacker, fraction);
            stopTicks = HitstopController.apply(level, victim, attacker, fraction, impulse);
        }
        strengthenFlash(level, victim, fraction);

        // 每个会话最多打四行（首次命中 + 更强的一档第一次出现）：
        // 服务端日志是"没有客户端"的环境里唯一能拿到的证据，也方便以后调数值。
        if (!announced || stopTicks > maxStopLogged) {
            announced = true;
            maxStopLogged = Math.max(maxStopLogged, stopTicks);
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[鸿蒙·战斗手感] 命中管线：damage={} maxHp={} 占比={} 顿帧={}t "
                            + "震动档={} 击退={} 元素={}（攻击者={}）",
                    String.format(java.util.Locale.ROOT, "%.2f", amount),
                    String.format(java.util.Locale.ROOT, "%.1f", victim.getMaxHealth()),
                    String.format(java.util.Locale.ROOT, "%.4f", fraction),
                    stopTicks,
                    CombatFeel.shakeForDamage(fraction),
                    String.format(java.util.Locale.ROOT, "%.3f",
                            CombatFeel.knockbackHorizontal(fraction)),
                    element == null ? "无" : element.cn(),
                    attacker == null ? "无" : attacker.getName().getString());
        }

        // ---- 6) 粒子：转（爆发 + 白闪）+ 合（余韵 + 地面残留）----
        CombatParticles.burst(level, hitPoint, element, fraction);
        CombatParticles.flash(level, hitPoint, fraction);
        CombatParticles.hitSpark(level, hitPoint, element);
        CombatParticles.afterglow(level, hitPoint, element, fraction);
        CombatParticles.groundResidue(level, victim.position(), element, fraction);

        // ---- 5) 音效分层 ----
        boolean melee = isMeleeHit(source, element);
        CombatSounds.impact(level, hitPoint, element, fraction, melee);

        // ---- 7) 客户端表现（只发给当事人，且每 tick 有上限）----
        if (attacker instanceof ServerPlayer hitter && hitter != victim) {
            if (allowPacket(hitter, level)) {
                com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(hitter,
                        CombatFeedbackPacket.impact(victim, amount, (float) fraction,
                                element, hitPoint));
            }
        }
        if (victim instanceof ServerPlayer hurtPlayer) {
            CombatSounds.playerHurt(level, hitPoint, fraction);
            if (allowPacket(hurtPlayer, level)) {
                com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(hurtPlayer,
                        CombatFeedbackPacket.taken(amount, (float) fraction, hitPoint));
            }
        }
    }

    // ------------------------------------------------------------------
    // 各部分
    // ------------------------------------------------------------------

    /** 伤害来源里的"人"（先看归属者，再看直接来源）。 */
    public static LivingEntity attackerOf(DamageSource source) {
        if (source == null) {
            return null;
        }
        if (source.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return source.getDirectEntity() instanceof LivingEntity living ? living : null;
    }

    /**
     * 击退冲量。
     *
     * <p>三件事一起决定"打飞多远"：伤害占比（主要）、目标的击退抗性（原版属性）、
     * 以及品阶——头目与首领只是"晃一下"（{@value #BOSS_KNOCKBACK_FACTOR} 倍），
     * 不然玩家能拿平砍把首领推着走，那就不叫首领了。</p>
     */
    public static Vec3 knockbackImpulse(LivingEntity victim, LivingEntity attacker,
                                        double fraction) {
        Vec3 delta = victim.position().subtract(attacker.position());
        Vec3 flat = new Vec3(delta.x, 0.0D, delta.z);
        if (flat.lengthSqr() < 1.0E-6D) {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }
        flat = flat.normalize();

        double horizontal = CombatFeel.knockbackHorizontal(fraction) * HitstopController.knockbackScale();
        double vertical = CombatFeel.knockbackVertical(fraction) * HitstopController.knockbackScale();

        // 原版击退抗性（穿了下界合金甲/首领怪身上有）
        double resistance = 0.0D;
        try {
            resistance = victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        } catch (Throwable ignored) {
            resistance = 0.0D;
        }
        double mass = Math.max(0.0D, 1.0D - Math.min(1.0D, resistance));

        // 头目 / 首领：只晃一下
        if (victim instanceof CultivationMonsterEntity monster) {
            MobRank rank = monster.rank();
            if (rank == MobRank.BOSS || rank == MobRank.OVERLORD) {
                mass *= BOSS_KNOCKBACK_FACTOR;
            }
        }
        // 玩家：不要被打得满天飞（这是"真实痛觉"那套的领域，不归这里）
        if (victim instanceof Player) {
            mass *= 0.55D;
        }
        return new Vec3(flat.x * horizontal * mass, vertical * mass, flat.z * horizontal * mass);
    }

    /** 头目 / 首领承受的击退倍率。 */
    public static final double BOSS_KNOCKBACK_FACTOR = 0.22D;

    /**
     * 加强命中闪白（服务端这一半）。
     *
     * <p>晚一 tick 把 {@code hurtTime / hurtDuration} 接到 10~18。
     * 为什么会晚一 tick：原版在 {@code actuallyHurt}（{@code LivingDamageEvent} 的触发点）
     * <b>之后</b>才写这两个字段，在事件里改会被覆盖。</p>
     *
     * <p><b>⚠️ 真正让玩家"看见"加强闪白的是客户端那一半</b>
     * （{@code client.CombatFeelClient.extendHitFlash}）：1.19.4 起客户端的红闪由
     * {@code ClientboundHurtAnimationPacket} → {@code LivingEntity.animateHurt} 驱动，
     * 服务端改这两个字段不影响渲染。服务端这一半保留着，是因为它同时把
     * {@code hurtTime} 用作"这一下挨得重不重"的<b>服务端</b>状态（NPC/怪物逻辑若要读，
     * 读到的就是加强后的值），并且与规格"延长 invulnerableTime / hurtTime"的文字口径一致。</p>
     */
    public static void strengthenFlash(ServerLevel level, LivingEntity victim, double fraction) {
        if (fraction < CombatFeel.HITSTOP_THRESHOLD) {
            return;         // 平砍那点伤害就用原版 10 tick，够了
        }
        float t = CombatFeel.clamp01((float) (fraction / CombatFeel.KNOCKBACK_FULL_FRACTION));
        int extend = FLASH_BASE_TICKS + Math.round((FLASH_MAX_TICKS - FLASH_BASE_TICKS) * t);
        CombatFxScheduler.schedule(level, 1, lvl -> {
            if (victim.isRemoved()) {
                return;
            }
            // 玩家不延长受伤闪白：原版受伤红屏由 hurtTime 驱动，
            // 延长它会让红屏更久更大、挡住视野（玩家反馈过）。
            if (!(victim instanceof net.minecraft.world.entity.player.Player)) {
                victim.hurtDuration = Math.max(victim.hurtDuration, extend);
                victim.hurtTime = Math.max(victim.hurtTime, extend);
            }
        });
    }

    /** 是不是近战（近战不叠"破空"层，因为原版已经放了挥击音）。 */
    private static boolean isMeleeHit(DamageSource source, Element element) {
        if (element != null) {
            return false;       // 法术：三层齐全
        }
        return source.getDirectEntity() == source.getEntity()
                && source.getEntity() instanceof LivingEntity;
    }

    /** 发包限流：同一玩家每 tick 最多 {@value #MAX_PACKETS_PER_TICK} 包。 */
    private static boolean allowPacket(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        long[] slot = PACKET_COUNT.get(player.getUUID());
        if (slot == null || slot[0] != now) {
            if (PACKET_COUNT.size() > 64) {
                PACKET_COUNT.clear();
            }
            PACKET_COUNT.put(player.getUUID(), new long[]{now, 1L});
            return true;
        }
        if (slot[1] >= MAX_PACKETS_PER_TICK) {
            return false;
        }
        slot[1]++;
        return true;
    }

    /** 换存档 / 停服清理。 */
    public static void clear() {
        PACKET_COUNT.clear();
        ELEMENT_HINT.remove();
    }
}
