package com.hongmeng.xiuxian.talent;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>职业机制的运行时状态</b> —— 每玩家一份资源，逐 tick 推进。
 *
 * <h2>它管什么（新五职业）</h2>
 * <table border="1">
 *   <caption>五种资源</caption>
 *   <tr><th>职业</th><th>资源</th><th>怎么涨</th><th>怎么用</th></tr>
 *   <tr><td>剑修</td><td>剑意</td><td>近战命中</td><td>每层加近战；满 10 层放剑气；剑罡以灵力抵伤</td></tr>
 *   <tr><td>血修</td><td>血池 + 缓伤</td><td>伤害 / 击杀 / 受创</td><td>残血回血；缓伤延后结算</td></tr>
 *   <tr><td>魔修</td><td>魔气</td><td>造成伤害</td><td>增伤 + 破防；满仓溢出自伤</td></tr>
 *   <tr><td>鬼修</td><td>魂印 + 鬼域</td><td>命中叠印 / 自然回能</td><td>3 层魂爆；鬼域 DoT + 减速；虚化免死</td></tr>
 *   <tr><td>体修</td><td>激荡</td><td>受创</td><td>满 100 引爆；致死锁血</td></tr>
 * </table>
 *
 * <h2>与旧实现的区别</h2>
 * <p>旧版 {@code PathRuntime} 是道途（法/体/魔/血/阵），本轮整体切到
 * {@link Career}（剑/血/魔/鬼/体）。类名保留 {@code PathRuntime} 以免大范围
 * 重命名（历史名），但内部<b>只认 {@link Career}</b>，且不再有法修/阵修分支。</p>
 *
 * <h2>⚠ 设计硬线：所有代价都必须"玩家可控"</h2>
 * <p>资源管理（法力既是盾又是输出、魔气满仓自伤）与时间重排（缓伤）可以；
 * 不做"无条件每秒掉血"这类被动惩罚。</p>
 */
public final class PathRuntime {

    /** 每个玩家的机制状态。 */
    private static final class State {
        /** 剑修：剑意层数（0~10，浮点以支持获取加成）。 */
        double swordIntent;
        /** 剑修：上次近战命中的游戏时间（用于脱战衰减）。 */
        long lastMeleeTick;
        /** 剑修：上次「剑势」受击凝意的游戏时间（5 秒冷却）。 */
        long lastMomentumTick = -9999L;

        /** 体修：激荡能量。 */
        double surge;
        int surgeActive;
        int lastStand;
        int lastStandCd;

        /** 魔修：魔气。 */
        double demonic;
        /** 血修：血池与缓伤池。 */
        double blood;
        double stagger;
        double staggerCarry;

        /** 鬼修：鬼域能量与展开状态。 */
        double ghostEnergy;
        boolean ghostOpen;
        /** 鬼修：虚化剩余无敌 tick 与冷却。 */
        int phaseTicks;
        int phaseCd;

        long lastCombatTick;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** 魂印：目标持久化数据键。 */
    public static final String SOUL_MARK = "hm_soul_mark";
    public static final String SOUL_EXPIRE = "hm_soul_expire";
    public static final String SOUL_LOCK = "hm_soul_lock";

    /** 鬼域展开门槛（沿用旧阵域数值）。 */
    private static final double GHOST_OPEN_COST = 60.0D;

    // ---- 职业被动（CareerSkill）接线用的口径常量 ----

    /** 命中/光环附带的负面效果时长（tick）——阴气·减速与魔威·削弱共用。 */
    private static final int HIT_DEBUFF_TICKS = 40;

    /** 血痕（血修被动·移动留痕）的减速半径（格）。 */
    private static final double BLOOD_TRAIL_RADIUS = 2.5D;

    /** 魂爆（鬼修被动·魂印引爆范围伤害）的半径（格）。 */
    private static final double SOUL_BURST_AOE_RADIUS = 3.0D;

    private PathRuntime() {
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), k -> new State());
    }

    private static Career careerOf(ServerPlayer player) {
        return Talents.pathOf(player);
    }

    // ------------------------------------------------------------------
    // 查询（给 HUD / 命令用）
    // ------------------------------------------------------------------

    /**
     * 资源快照（都是 0~1 的比例）。槽位约定：
     * <pre>
     *   [0] 剑修·剑意（层数/10） 或 体修·激荡
     *   [1] 魔修·魔气
     *   [2] 血修·血池
     *   [3] 鬼修·鬼域能量
     * </pre>
     */
    public static double[] snapshot(ServerPlayer player) {
        Career c = careerOf(player);
        if (c == null) {
            return new double[]{0.0D, 0.0D, 0.0D, 0.0D};
        }
        State st = state(player);
        return new double[]{
                clamp01(switch (c) {
                    case JIAN_XIU -> st.swordIntent / PathMechanics.SWORD_INTENT_MAX;
                    case TI_XIU -> st.surge / PathMechanics.SURGE_MAX;
                    default -> 0.0D;
                }),
                clamp01(st.demonic / PathMechanics.DEMON_MAX),
                clamp01(st.blood / PathMechanics.BLOOD_MAX),
                clamp01(st.ghostEnergy / PathMechanics.GHOST_MAX)};
    }

    /** 当前魔气值（0~DEMON_MAX）。给伤害结算读。 */
    public static double demonicOf(ServerPlayer player) {
        return player == null ? 0.0D : state(player).demonic;
    }

    /** 当前血池比例（0~1）。给伤害结算读。 */
    public static double bloodFraction(ServerPlayer player) {
        if (player == null) {
            return 0.0D;
        }
        return clamp01(state(player).blood / PathMechanics.BLOOD_MAX);
    }

    /** 当前激荡值（0~SURGE_MAX）。 */
    public static double surgeOf(ServerPlayer player) {
        return player == null ? 0.0D : state(player).surge;
    }

    /** 当前剑意层数（0~10）。 */
    public static double swordIntentOf(ServerPlayer player) {
        return player == null ? 0.0D : state(player).swordIntent;
    }

    /**
     * <b>外部回剑意</b>（格挡/弹反等非"命中"来源）。
     *
     * <p>接线点：{@code ModEvents.onPlayerTakesDamage} 的完美格挡分支 ——
     * 职业被动【剑势】要求"弹反回剑意"，那是受击事件、不在
     * {@link #onPlayerDealtDamage} 里，所以开这个入口复用同一份剑意状态，
     * <b>不新建资源</b>。</p>
     *
     * <p>只对剑修生效；上限与命中获取一致，不会越过 10 层（满层由下一次
     * 近战命中照常放剑气，不在受击里额外触发）。</p>
     */
    public static void gainSwordIntent(ServerPlayer player, double amount) {
        if (player == null || amount <= 0.0D
                || careerOf(player) != Career.JIAN_XIU) {
            return;
        }
        State st = state(player);
        st.lastMeleeTick = player.level().getGameTime();
        st.swordIntent = Math.min(PathMechanics.SWORD_INTENT_MAX,
                st.swordIntent + amount);
    }

    /** 当前鬼域能量（0~100）。 */
    public static double ghostEnergyOf(ServerPlayer player) {
        return player == null ? 0.0D : state(player).ghostEnergy;
    }

    /** 体修激荡是否正在生效。 */
    public static boolean surgeActive(ServerPlayer player) {
        return state(player).surgeActive > 0;
    }

    /** 鬼域是否展开（旧名保留，给同步包/命令用）。 */
    public static boolean formationOpen(ServerPlayer player) {
        return state(player).ghostOpen;
    }

    /** 虚化是否正在无敌。 */
    public static boolean phaseActive(ServerPlayer player) {
        return player != null && state(player).phaseTicks > 0;
    }

    // ------------------------------------------------------------------
    // 事件：受创
    // ------------------------------------------------------------------

    /**
     * 玩家受创时调用（由 {@code ModEvents.onPlayerHurt} 接）。
     *
     * @param damage 即将受到的伤害
     * @return 实际应该受到的伤害（血修缓伤会削减"立即"部分；虚化会归零）
     */
    public static double onPlayerHurt(ServerPlayer player, double damage) {
        Career c = careerOf(player);
        State st = state(player);
        st.lastCombatTick = player.level().getGameTime();
        if (c == null || damage <= 0.0D) {
            return damage;
        }
        // 虚化期间完全免伤（先于一切结算）
        if (st.phaseTicks > 0) {
            return 0.0D;
        }
        double remaining = damage;

        // ---- 剑修：剑罡（灵力抵伤 + 空灵力易伤）----
        if (c == Career.JIAN_XIU) {
            try {
                var data = CultivationCapability.get(player);
                int mana = com.hongmeng.xiuxian.technique.ManaBridge.mana(player, data);
                if (mana > 0) {
                    double ratio = PathMechanics.swordAuraRatio(st.swordIntent,
                            TalentBonus.swordAura(player));
                    double wantAbsorb = remaining * ratio;
                    double canAbsorb = mana * PathMechanics.MANA_PER_DAMAGE;
                    double absorb = Math.min(wantAbsorb, canAbsorb);
                    if (absorb >= 1.0D) {
                        int cost = (int) Math.ceil(absorb / PathMechanics.MANA_PER_DAMAGE);
                        if (com.hongmeng.xiuxian.technique.ManaBridge
                                .spend(player, data, cost)) {
                            remaining -= absorb;
                            com.hongmeng.xiuxian.technique.PathMechanicFx
                                    .swordAura(player, absorb);
                        }
                    }
                    // 空灵力易伤：灵力 <15% 开始变脆，最多 +35%（玩家方案要求）
                    int after = com.hongmeng.xiuxian.technique.ManaBridge.mana(player, data);
                    int max = com.hongmeng.xiuxian.technique.ManaBridge
                            .maxMana(player, data);
                    double vuln = PathMechanics.manaEmptyVulnerability(after, max);
                    if (vuln > 0.0D) {
                        remaining *= 1.0D + vuln;
                    }
                }
                // ---- 剑势（职业被动 · 逆势凝意）：受击凝 1 层剑意，5 秒一次 ----
                //
                // 放在剑罡之后：真正吃到伤害（remaining > 0）才算"被压制"；
                // 剑罡全挡掉时不触发，避免"站着挨打刷层"。
                if (remaining > 0.0D
                        && CareerPassives.swordMomentum(player)
                        && st.lastMomentumTick + 100L <= player.level().getGameTime()) {
                    st.lastMomentumTick = player.level().getGameTime();
                    gainSwordIntent(player, 1.0D);
                    player.displayClientMessage(Component.literal(
                            "\u00a7b\u00a7l剑势 \u00a77· \u00a78受击凝意，剑意 +1"), true);
                }
            } catch (Throwable ignored) {
                // 法力系统不可用就按普通受伤
            }
        }

        // ---- 体修：受创攒激荡 ----
        if (c == Career.TI_XIU) {
            double surgeMul = 1.0D;
            st.surge = Math.min(PathMechanics.SURGE_MAX,
                    st.surge + PathMechanics.surgeGain(damage) * surgeMul);
            if (st.surge >= PathMechanics.SURGE_MAX && st.surgeActive <= 0) {
                st.surgeActive = 100;
                st.surge = 0.0D;
                double frac = healthFraction(player);
                boolean golden = frac < 0.5D;
                player.displayClientMessage(Component.literal(
                        golden ? "\u00a76\u00a7l金身激荡 \u00a77· 减伤 30%"
                                : "\u00a7c\u00a7l燃血激荡 \u00a77· 增伤 80%"), true);
                com.hongmeng.xiuxian.technique.PathMechanicFx
                        .surgeBurst(player, golden);
            }
        }

        // ---- 血修：缓伤 ----
        if (c == Career.XUE_XIU) {
            double staggerRatio = 0.30D;
            double[] split = PathMechanics.staggerSplit(remaining, staggerRatio);
            remaining = split[0];
            st.stagger += split[1];
        }

        // ---- 鬼修：虚化（致死伤害触发）----
        if (c == Career.GUI_XIU && st.phaseCd <= 0
                && st.ghostEnergy >= PathMechanics.PHASE_ENERGY
                && remaining >= player.getHealth() && player.getHealth() > 0.0F) {
            st.phaseTicks = PathMechanics.PHASE_TICKS;
            st.phaseCd = PathMechanics.phaseCooldownTicks(
                    TalentBonus.cooldownCut(player));
            st.ghostEnergy -= PathMechanics.PHASE_ENERGY;
            remaining = 0.0D;
            player.displayClientMessage(Component.literal(
                    "\u00a7a\u00a7l虚化 \u00a77· 万法不侵"), true);
            com.hongmeng.xiuxian.technique.PathMechanicFx
                    .phase(player, PathMechanics.PHASE_TICKS);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    PathMechanics.PHASE_TICKS, 1, false, false, true));
        }

        // ---- 职业被动·血偿（受击回补 3%）----
        //
        // 接在"受创最终结算口"：上面所有减伤/缓伤/虚化都算完，remaining 才是
        // 真正会掉的血。按它 3% 立刻回血（先回后扣，净效果 ≈ 少掉 3%）。
        // 未解锁时 CareerPassives 返回 0 → 不进分支，行为与本线接入前一致。
        double healOnHurt = CareerPassives.healOnHurt(player);
        if (healOnHurt > 0.0D && remaining > 0.0D) {
            player.heal((float) (remaining * healOnHurt));
        }

        return remaining;
    }

    /** 玩家打出伤害后调用（由 {@code ModEvents.onPlayerDealtDamage} 接）。 */
    public static void onPlayerDealtDamage(ServerPlayer player, LivingEntity target,
                                           double damage, boolean melee) {
        Career c = careerOf(player);
        State st = state(player);
        st.lastCombatTick = player.level().getGameTime();
        if (c == null || damage <= 0.0D) {
            return;
        }
        switch (c) {
            case MO_XIU -> {
                double before = st.demonic;
                double capMul = 1.0D;
                double demonicCap = PathMechanics.DEMON_MAX * capMul;
                double scaled = st.demonic / Math.max(1.0E-9D, capMul);
                st.demonic = Math.min(demonicCap,
                        scaled + PathMechanics.demonGain(damage));
                double overflow = PathMechanics.demonGain(damage) - (st.demonic - before);
                double self = PathMechanics.demonOverflowSelfDamage(st.demonic, overflow);
                if (self > 0.0D) {
                    player.hurt(player.damageSources().magic(), (float) self);
                    com.hongmeng.xiuxian.technique.PathMechanicFx
                            .demonicOverflow(player);
                }
                // ---- 职业被动·魔威（魔修）：命中附带削弱 10% ----
                // 接在既有"造成伤害"结算口（这里拿到的是活着的 target）。
                // 10% 用 WEAKNESS I 近似：原版 WEAKNESS 是按固定攻击点数减，
                // 不是百分比，取观感/强度最接近的一档；40t 后自动过期，无残留。
                if (target != null && target.isAlive()
                        && CareerPassives.enemyWeaken(player) > 0.0D) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                            HIT_DEBUFF_TICKS, 0, false, false, true));
                }
            }
            case XUE_XIU -> st.blood = Math.min(PathMechanics.BLOOD_MAX,
                    st.blood + PathMechanics.bloodGainFromDamage(damage));
            case JIAN_XIU -> {
                if (melee && holdingWeapon(player)) {
                    st.lastMeleeTick = player.level().getGameTime();
                    // 剑意获取 = 1 + 职业专属(+10%) + 天赋 + 职业被动·剑心通明(+10%)
                    // 三者都是加法比例（CareerPassives 未解锁时为 0）。
                    double mul = 1.0D + TalentBonus.swordIntent(player)
                            + CareerPassives.swordIntent(player);
                    st.swordIntent = Math.min(PathMechanics.SWORD_INTENT_MAX,
                            st.swordIntent + PathMechanics.swordIntentGain(mul));
                    if (st.swordIntent >= PathMechanics.SWORD_INTENT_MAX - 1.0E-6D) {
                        triggerSwordQi(player);
                        st.swordIntent = 0.0D;
                    }
                }
            }
            case GUI_XIU -> {
                if (target != null && target.isAlive()) {
                    applySoulMark(player, target, st);
                    // ---- 职业被动·阴气（鬼修）：攻击附带减速 10% ----
                    // 接在既有"叠魂印"同一命中口；10% 用 MOVEMENT_SLOWDOWN I
                    // 近似（原版无百分比减速瞬时效果），40t 过期，不叠加成永久。
                    if (CareerPassives.slowOnHit(player) > 0.0D) {
                        target.addEffect(new MobEffectInstance(
                                MobEffects.MOVEMENT_SLOWDOWN,
                                HIT_DEBUFF_TICKS, 0, false, false, true));
                    }
                }
            }
            default -> {
            }
        }
    }

    /** 剑修：满层剑气（前方锥形，最多 5 个目标）。 */
    private static void triggerSwordQi(ServerPlayer player) {
        if (!holdingWeapon(player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double dmg = PathMechanics.swordQiDamage(attack, TalentBonus.swordQi(player));
        Vec3 look = player.getLookAngle();
        int hit = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(PathMechanics.SWORD_QI_RADIUS))) {
            if (e == player || !e.isAlive() || e instanceof net.minecraft.world.entity.player.Player
                    && e != player) {
                continue;
            }
            Vec3 dir = e.position().subtract(player.position());
            if (dir.lengthSqr() > PathMechanics.SWORD_QI_RADIUS * PathMechanics.SWORD_QI_RADIUS) {
                continue;
            }
            if (dir.lengthSqr() > 1.0E-4D
                    && dir.normalize().dot(look) < 0.30D) {
                continue;                    // 背后/侧面不中（锥形判定）
            }
            e.hurt(level.damageSources().magic(), (float) dmg);
            hit++;
            if (hit >= PathMechanics.SWORD_QI_TARGETS) {
                break;
            }
        }
        com.hongmeng.xiuxian.technique.PathMechanicFx
                .swordQi(player, PathMechanics.SWORD_QI_RADIUS);
    }

    /** 鬼修：给目标叠魂印；第 3 层引爆。 */
    private static void applySoulMark(ServerPlayer player, LivingEntity target, State st) {
        long now = player.level().getGameTime();
        var pd = target.getPersistentData();
        long expire = pd.getLong(SOUL_EXPIRE);
        int marks = now > expire ? 0 : pd.getInt(SOUL_MARK);
        if (now < pd.getLong(SOUL_LOCK)) {
            return;                          // 魂爆后的叠印冷却
        }
        marks++;
        pd.putInt(SOUL_MARK, marks);
        pd.putLong(SOUL_EXPIRE, now + PathMechanics.SOUL_MARK_DURATION);
        com.hongmeng.xiuxian.technique.PathMechanicFx.soulMark(player, target, marks);
        if (marks >= PathMechanics.SOUL_MARK_MAX) {
            double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double dmg = PathMechanics.soulBurstDamage(attack,
                    TalentBonus.soulMark(player));
            target.hurt(player.damageSources().magic(), (float) dmg);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    PathMechanics.SOUL_BURST_SLOW_TICKS, 1, false, false, true));
            pd.putInt(SOUL_MARK, 0);
            pd.putLong(SOUL_EXPIRE, now);
            pd.putLong(SOUL_LOCK, now + PathMechanics.SOUL_BURST_CD);
            com.hongmeng.xiuxian.technique.PathMechanicFx
                    .soulBurst(player, target);
            // ---- 职业被动·魂爆（鬼修）：引爆时产生 30% 范围伤害 ----
            // 接在"魂印第 3 层引爆"这个现成结算点之后，用同一次 dmg 折算，
            // 不新造第二套魂印/爆炸系统。
            triggerSoulBurstAoe(player, target, dmg, now);
        }
    }

    /**
     * 魂印引爆的范围伤害（职业被动·魂爆）。
     *
     * @param center      被引爆的目标（范围以此为中心）
     * @param burstDamage 本次单体魂爆伤害
     * @param now         当前游戏时间（给范围目标打魂爆冷却锁用）
     */
    private static void triggerSoulBurstAoe(ServerPlayer player, LivingEntity center,
                                            double burstDamage, long now) {
        double ratio = CareerPassives.soulBurstAoe(player);
        if (ratio <= 0.0D || burstDamage <= 0.0D
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        var box = center.getBoundingBox().inflate(SOUL_BURST_AOE_RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == player || e == center || !e.isAlive()
                    || e instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }
            if (e.distanceTo(center) > SOUL_BURST_AOE_RADIUS) {
                continue;
            }
            // ⚠ 防连锁：范围伤害会再次走 LivingHurtEvent → 叠印流程，
            //   给范围目标先打上"魂爆后叠印冷却"锁，避免一爆接一爆的递归。
            e.getPersistentData().putLong(SOUL_LOCK, now + PathMechanics.SOUL_BURST_CD);
            e.hurt(player.damageSources().magic(), (float) (burstDamage * ratio));
        }
    }

    /** 击杀后调用。 */
    public static void onPlayerKill(ServerPlayer player, double victimMaxHealth) {
        Career c = careerOf(player);
        State st = state(player);
        st.lastCombatTick = player.level().getGameTime();
        if (c == null) {
            return;
        }
        switch (c) {
            case XUE_XIU -> st.blood = Math.min(PathMechanics.BLOOD_MAX,
                    st.blood + PathMechanics.bloodGainFromKill(victimMaxHealth));
            case MO_XIU -> {
                // ---- 职业被动·噬魂（魔修）：击杀回魔气 20 点 ----
                // 用现有魔气字段（不新建资源），满仓照常 clamp。
                double gain = CareerPassives.demonKillGain(player);
                if (gain > 0.0D) {
                    st.demonic = Math.min(PathMechanics.DEMON_MAX,
                            st.demonic + gain);
                }
            }
            case TI_XIU -> st.lastStandCd = Math.max(0, st.lastStandCd - 200);
            case GUI_XIU -> {
                st.ghostEnergy = Math.min(PathMechanics.GHOST_MAX,
                        st.ghostEnergy + 10.0D);
                // ---- 职业被动·锁魂（鬼修）：击杀回魂印/鬼域能量 15 点 ----
                // 既有击杀钩子只有 victimMaxHealth、拿不到 victim 实体，
                // 无法校验"目标是否带印"；按击杀给点数（注释留痕，不新增字段）。
                double soulGain = CareerPassives.soulKillGain(player);
                if (soulGain > 0.0D) {
                    st.ghostEnergy = Math.min(PathMechanics.GHOST_MAX,
                            st.ghostEnergy + soulGain);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------
    // 每 tick
    // ------------------------------------------------------------------

    /** 由 {@code ModEvents.onPlayerTick} 调。 */
    public static void tick(ServerPlayer player) {
        Career c = careerOf(player);
        if (c == null) {
            return;
        }
        State st = state(player);
        long now = player.level().getGameTime();

        // ---- 剑修：脱战衰减 ----
        if (st.swordIntent > 0.0D) {
            if (now - st.lastMeleeTick > 80L) {
                st.swordIntent = Math.max(0.0D, st.swordIntent - 0.1D);
            }
        }

        // ---- 体修：激荡计时、锁血冷却 ----
        if (st.surgeActive > 0) {
            st.surgeActive--;
        }
        if (st.lastStand > 0) {
            st.lastStand--;
            if (player.getHealth() < PathMechanics.lastStandFloor()) {
                player.setHealth((float) PathMechanics.lastStandFloor());
            }
        }
        if (st.lastStandCd > 0) {
            st.lastStandCd--;
        }

        // ---- 鬼修：虚化计时 ----
        if (st.phaseTicks > 0) {
            st.phaseTicks--;
        }
        if (st.phaseCd > 0) {
            st.phaseCd--;
        }

        // ---- 视觉：血池与魔气（每 10 tick 一次，避免每 tick 发包）----
        if (now % 10L == 0L) {
            if (c == Career.XUE_XIU) {
                double frac = clamp01(st.blood / PathMechanics.BLOOD_MAX);
                if (frac >= 0.15D) {
                    com.hongmeng.xiuxian.technique.PathMechanicFx
                            .bloodPool(player, frac);
                }
            } else if (c == Career.MO_XIU) {
                double frac = clamp01(st.demonic / PathMechanics.DEMON_MAX);
                if (frac >= 0.15D) {
                    com.hongmeng.xiuxian.technique.PathMechanicFx
                            .demonicAura(player, frac);
                }
            }
        }

        // ---- 血修：缓伤结算 + 血池自动回血 ----
        if (st.stagger > 0.0D) {
            st.staggerCarry += PathMechanics.staggerPerTick(st.stagger);
            st.stagger = Math.max(0.0D, st.stagger - PathMechanics.staggerPerTick(st.stagger));
            if (st.staggerCarry >= 1.0D) {
                float dmg = (float) Math.floor(st.staggerCarry);
                st.staggerCarry -= dmg;
                player.hurt(player.damageSources().magic(), dmg);
                com.hongmeng.xiuxian.technique.PathMechanicFx.staggerTick(player, dmg);
            }
        }
        if (c == Career.XUE_XIU) {
            double hf = healthFraction(player);
            if (hf < 0.55D && st.blood > 0.0D) {
                double want = player.getMaxHealth() * 0.02D;
                double[] sp = PathMechanics.spendBlood(st.blood, want);
                if (sp[0] > 0.0D) {
                    st.blood = Math.max(0.0D, st.blood - sp[1]);
                    player.setHealth((float) Math.min(player.getMaxHealth(),
                            player.getHealth() + sp[0]));
                    if (now % 10L == 0L) {
                        com.hongmeng.xiuxian.technique.PathMechanicFx
                                .bloodHeal(player, sp[0]);
                    }
                }
            }
        }

        // ---- 魔修：脱战消散 ----
        if (c == Career.MO_XIU && now - st.lastCombatTick > 100L && st.demonic > 0.0D) {
            st.demonic = Math.max(0.0D, st.demonic - PathMechanics.demonDecayPerTick());
        }

        // ---- 血修：脱战消散 ----
        if (c == Career.XUE_XIU && now - st.lastCombatTick > 100L && st.blood > 0.0D) {
            st.blood = Math.max(0.0D, st.blood - PathMechanics.BLOOD_MAX * 0.0025D);
        }

        // ---- 职业被动·血痕（血修）：移动留痕，区域减速 ----
        //
        // 接在逐 tick 推进里（移动判定天然属于 tick）。每 10 tick 检查一次，
        // **真的在移动才留痕**（站着不动不该白嫖减速场）；表现直接复用既有的
        // 血雾 PathMechanicFx.bloodPool，不新建粒子/资源系统。
        if (c == Career.XUE_XIU
                && now % 10L == 0L
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.0025D
                && CareerPassives.auraSlow(player) > 0.0D) {
            applyBloodTrail(player);
        }

        // ---- 鬼修：鬼域自然回复 / 展开消耗 ----
        if (c == Career.GUI_XIU) {
            if (st.ghostOpen) {
                st.ghostEnergy = Math.max(0.0D, st.ghostEnergy - PathMechanics.GHOST_DRAIN);
                if (st.ghostEnergy <= 0.0D) {
                    st.ghostOpen = false;
                    player.displayClientMessage(Component.literal(
                            "\u00a77鬼域消散。"), true);
                } else {
                    applyGhostRealm(player, st);
                }
            } else {
                st.ghostEnergy = Math.min(PathMechanics.GHOST_MAX,
                        st.ghostEnergy + PathMechanics.ghostRegenPerTick());
            }
        }

        // ---- 体修：锁血判定（每次 tick 检查是否被一击致死）----
        if (c == Career.TI_XIU && st.lastStand <= 0 && st.lastStandCd <= 0
                && player.getHealth() <= 1.0F && player.getHealth() > 0.0F) {
            st.lastStand = PathMechanics.LAST_STAND_TICKS;
            st.lastStandCd = 600;
            player.displayClientMessage(Component.literal(
                    "\u00a7c\u00a7l不灭 \u00a77· 锁血 2 秒"), true);
            com.hongmeng.xiuxian.technique.PathMechanicFx.lastStand(player);
        }
    }

    /** 鬼域（旧阵域代码复用）：每 4 tick 结算一次 DoT + 减速。 */
    private static void applyGhostRealm(ServerPlayer player, State st) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 4L != 0L) {
            return;
        }
        double radius = 4.0D + TalentBonus.area(player) * 8.0D;
        int ticks = (int) (100.0D - st.ghostEnergy);
        double per = PathMechanics.ghostDotPerTick(radius, ticks) * 4.0D;
        per *= 1.0D + TalentBonus.ghostRealm(player);
        com.hongmeng.xiuxian.technique.PathMechanicFx.formationTick(player, radius,
                clamp01(st.ghostEnergy / PathMechanics.GHOST_MAX));
        double slow = PathMechanics.ghostSlow(radius) + TalentBonus.control(player);
        var box = player.getBoundingBox().inflate(radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == player || !e.isAlive() || e instanceof ServerPlayer) {
                continue;
            }
            if (e.distanceTo(player) > radius) {
                continue;
            }
            var speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                com.hongmeng.xiuxian.difficulty.MobScaling.applyRootModifier(speed,
                        GHOST_SLOW_ID, "鬼域减速", -Math.min(0.8D, slow),
                        net.minecraft.world.entity.ai.attributes.AttributeModifier
                                .Operation.MULTIPLY_TOTAL);
            }
            if (per > 0.0D) {
                e.hurt(level.damageSources().magic(),
                        (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * per));
            }
        }
    }

    /** 鬼域减速的修饰符 id（固定，保证幂等）。 */
    private static final UUID GHOST_SLOW_ID =
            UUID.fromString("8b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * 血痕（血修分支被动）：移动时在脚下留一圈"减速血痕"。
     *
     * <p>接在 {@link #tick} 的移动判定里。10% 减速用 {@code MOVEMENT_SLOWDOWN I}
     * 近似（原版没有"百分比减速"的瞬时效果），30 tick 过期 —— 敌人离开血痕
     * 就恢复，不会留永久 debuff；也不对每个敌人挂属性修饰符，避免属性系统抖动。</p>
     */
    private static void applyBloodTrail(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 表现：复用既有血雾（PathMechanicFx），不新建粒子系统
        com.hongmeng.xiuxian.technique.PathMechanicFx.bloodPool(player, 0.18D);
        var box = player.getBoundingBox().inflate(BLOOD_TRAIL_RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == player || !e.isAlive()
                    || e instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }
            if (e.distanceTo(player) > BLOOD_TRAIL_RADIUS) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    30, 0, false, false, true));
        }
    }

    /** 开关鬼域（命令/按键用）。 */
    public static void toggleFormation(ServerPlayer player) {
        if (careerOf(player) != Career.GUI_XIU) {
            player.displayClientMessage(Component.literal(
                    "\u00a77只有 \u00a7a鬼修\u00a77 能展开鬼域。"), true);
            return;
        }
        State st = state(player);
        if (st.ghostOpen) {
            st.ghostOpen = false;
            player.displayClientMessage(Component.literal("\u00a77鬼域收回。"), true);
            return;
        }
        if (st.ghostEnergy < GHOST_OPEN_COST) {
            player.displayClientMessage(Component.literal(
                    "\u00a77鬼域能量不足（" + (int) st.ghostEnergy + "/"
                            + (int) GHOST_OPEN_COST + "）"), true);
            return;
        }
        st.ghostOpen = true;
        player.displayClientMessage(Component.literal(
                "\u00a7a\u00a7l鬼域展开 \u00a77· 范围内敌人持续受伤并减速"), true);
        double openRadius = 4.0D + TalentBonus.area(player) * 8.0D;
        com.hongmeng.xiuxian.technique.PathMechanicFx.formationOpen(player, openRadius);
    }

    /** 手动消耗血池回血（血修）。 */
    public static void bloodHeal(ServerPlayer player) {
        if (careerOf(player) != Career.XUE_XIU) {
            player.displayClientMessage(Component.literal(
                    "\u00a77只有 \u00a7c血修\u00a77 能动用血池。"), true);
            return;
        }
        State st = state(player);
        double want = player.getMaxHealth() - player.getHealth();
        double[] sp = PathMechanics.spendBlood(st.blood, want);
        if (sp[0] <= 0.0D) {
            player.displayClientMessage(Component.literal("\u00a77血池是空的。"), true);
            return;
        }
        st.blood = Math.max(0.0D, st.blood - sp[1]);
        player.setHealth((float) Math.min(player.getMaxHealth(),
                player.getHealth() + sp[0]));
        player.displayClientMessage(Component.literal(
                "\u00a7c血池涌动 \u00a77· 回复 \u00a7f" + Math.round(sp[0])
                        + " \u00a77生命（血池剩 " + (int) st.blood + "）"), true);
    }

    /** 打印机制状态。 */
    public static void show(ServerPlayer player) {
        Career c = careerOf(player);
        if (c == null) {
            player.displayClientMessage(Component.literal(
                    "\u00a77尚未选择职业。"), false);
            return;
        }
        double[] s = snapshot(player);
        State st = state(player);
        player.sendSystemMessage(Component.literal(
                "\u00a76\u00a7l" + c.cn() + " \u00a77· 机制状态"));
        switch (c) {
            case JIAN_XIU -> player.sendSystemMessage(Component.literal(
                    "  \u00a77剑意 \u00a7f" + String.format("%.1f", st.swordIntent)
                            + "/" + PathMechanics.SWORD_INTENT_MAX
                            + " \u00a78剑罡吸收 "
                            + Math.round(PathMechanics.swordAuraRatio(st.swordIntent,
                                    TalentBonus.swordAura(player)) * 100.0D) + "%"));
            case TI_XIU -> player.sendSystemMessage(Component.literal(
                    "  \u00a77激荡能量 \u00a7f" + (int) (s[0] * PathMechanics.SURGE_MAX)
                            + "/" + (int) PathMechanics.SURGE_MAX
                            + (st.surgeActive > 0 ? " \u00a7c【引爆中 " + st.surgeActive + "】" : "")
                            + (st.lastStand > 0 ? " \u00a7c【锁血 " + st.lastStand + "】" : "")));
            case MO_XIU -> player.sendSystemMessage(Component.literal(
                    "  \u00a77魔气 \u00a7f" + (int) (s[1] * PathMechanics.DEMON_MAX)
                            + "/" + (int) PathMechanics.DEMON_MAX
                            + " \u00a78增伤 ×" + String.format("%.2f",
                                    PathMechanics.demonDamageBonus(st.demonic))
                            + " 破防 "
                            + Math.round(PathMechanics.demonPenetration(st.demonic) * 100.0D)
                            + "%"));
            case XUE_XIU -> player.sendSystemMessage(Component.literal(
                    "  \u00a77血池 \u00a7f" + (int) (s[2] * PathMechanics.BLOOD_MAX)
                            + "/" + (int) PathMechanics.BLOOD_MAX
                            + " \u00a78缓伤池 " + String.format("%.1f", st.stagger)
                            + " \u00a78反杀 ×" + String.format("%.2f",
                                    PathMechanics.bloodlustBonus(s[2],
                                            healthFraction(player)))));
            case GUI_XIU -> player.sendSystemMessage(Component.literal(
                    "  \u00a77鬼域能量 \u00a7f" + (int) st.ghostEnergy + "/100"
                            + (st.ghostOpen ? " \u00a7a【已展开】" : " \u00a78（未展开）")
                            + " \u00a78虚化冷却 " + Math.max(0, st.phaseCd / 20) + "s"));
        }
    }

    /**
     * 按 UUID 取原始资源值（存档层用，不 clamp）。
     *
     * @return {剑意(或体修激荡), 魔气, 血池, 鬼域}
     */
    public static double[] snapshotRaw(UUID uuid) {
        State st = uuid == null ? null : STATES.get(uuid);
        if (st == null) {
            return new double[]{0.0D, 0.0D, 0.0D, 0.0D};
        }
        Career c = Talents.careerAt(uuid);
        double slot0 = c == Career.JIAN_XIU
                ? st.swordIntent * 10.0D          // 统一按 0~100 存
                : st.surge;
        return new double[]{slot0, st.demonic, st.blood, st.ghostEnergy};
    }

    /**
     * 存档回读：按职业解释槽 0（剑意 vs 激荡），其余按各自上限 clamp。
     */
    public static void restore(UUID uuid, double a, double b, double c, double d) {
        if (uuid == null) {
            return;
        }
        State st = STATES.computeIfAbsent(uuid, k -> new State());
        Career career = Talents.careerAt(uuid);
        if (career == Career.JIAN_XIU) {
            st.swordIntent = clampRange(a / 10.0D, 0.0D, PathMechanics.SWORD_INTENT_MAX);
        } else {
            st.surge = clampRange(a, 0.0D, PathMechanics.SURGE_MAX);
        }
        st.demonic = clampRange(b, 0.0D, PathMechanics.DEMON_MAX);
        st.blood = clampRange(c, 0.0D, PathMechanics.BLOOD_MAX);
        st.ghostEnergy = clampRange(d, 0.0D, PathMechanics.GHOST_MAX);
    }

    private static double clampRange(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static void forget(UUID uuid) {
        STATES.remove(uuid);
    }

    private static double healthFraction(ServerPlayer player) {
        float max = player.getMaxHealth();
        return max <= 0.0F ? 0.0D : clamp01(player.getHealth() / max);
    }

    /**
     * 主手是否拿着武器。剑意 / 剑气只在持械时积累与释放；
     * 空手近战不再攒剑意、也不再放剑气拖尾。
     */
    private static boolean holdingWeapon(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.SwordItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.CareerWeaponItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.ModPathWeapon.PathWeaponItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.RealmWeaponItem) {
            return true;
        }
        return com.hongmeng.xiuxian.item.gear.WeaponAttributeHandler.attackOf(stack) > 0.0D;
    }

    private static double clamp01(double v) {
        return v < 0.0D ? 0.0D : (v > 1.0D ? 1.0D : v);
    }
}
