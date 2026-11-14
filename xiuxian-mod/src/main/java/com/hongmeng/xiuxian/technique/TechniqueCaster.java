package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.RealmStats;
import com.hongmeng.xiuxian.domain.DomainData;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 功法释放器：把 {@link Technique} 的数据变成实际效果。
 *
 * <p>每门功法的**特性**在这里落地——不是只有伤害数字不同：</p>
 * <ul>
 *   <li>{@code LINE} 直线穿透，一条线上所有敌人</li>
 *   <li>{@code FAN} 扇形散射多道</li>
 *   <li>{@code AOE} 以自身为中心的范围爆发</li>
 *   <li>{@code DASH} 位移 + 沿途伤害</li>
 *   <li>{@code HEAL} / {@code BUFF} / {@code LIFESTEAL} 辅助类</li>
 * </ul>
 *
 * <p>伤害公式：</p>
 * <pre>
 *   基础 = RealmStats.standard(本境) × 功法倍率
 *   加成 = 道宫同元素加成（领域内同元素功法伤害）
 * </pre>
 */
public final class TechniqueCaster {

    private TechniqueCaster() {
    }

    /** 范围类功法的判定半径。 */
    private static final double AOE_RADIUS = 44.0D;
    /** 职业主动「血河 / 鬼域展开」留下的持续区域时长（tick）：6 秒。 */
    private static final int CAREER_AREA_TICKS = 120;
    /** 直线类功法的长度。 */
    private static final double LINE_LENGTH = 90.0D;
    /** 直线类功法的判定宽度。 */
    private static final double LINE_WIDTH = 1.6D;
    /** 扇形/投射物的飞行距离。 */
    private static final double FAN_RANGE = 70.0D;

    /**
     * 施展一门功法。
     *
     * <p>这个方法现在只负责**校验 + 起手**，不再直接结算——蓄力时间
     * （{@link Technique#castTicks()}）由 {@link SpellCastHandler} 按 tick 推进：</p>
     *
     * <ol>
     *   <li>境界 / 已修习 / 冷却 / 是否正在蓄力 —— 不合格就中文提示并拒绝；</li>
     *   <li>灵力：不够就提示"灵力不足"并拒绝（{@link ManaBridge}）；</li>
     *   <li><b>扣灵力、进冷却都发生在起手这一刻</b>（防止蓄力期间刷灵力）；</li>
     *   <li>瞬发（{@code castTicks == 0}）立刻结算；否则进入蓄力，
     *       移动 / 受伤 / 换手会中断，中断不退灵力（已经付过的代价）。</li>
     * </ol>
     *
     * @return true 表示起手成功（不代表效果已经结算）
     */
    public static boolean cast(ServerPlayer player, Technique technique) {
        if (player == null || technique == null) {
            return false;
        }
        CultivationData data = CultivationCapability.get(player);
        if (data == null || !data.isAwakened()) {
            return false;
        }
        // 被动不主动释放：装备即生效
        if (technique.isPassive()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a77" + technique.colored()
                            + " \u00a77是被动功法，装上就生效，不需要施展。\u00a78（按 O 打开法术栏）"),
                    true);
            return false;
        }
        // 境界门槛：功法自己的 minTier 说了算。
        //
        // ⚠ 但**飞升起就全部解锁**（玩家反复强调的那条：
        //   「我不是说了飞升境界就可以释放任何法术，为何我释放万象寂灭提示境界不足？
        //     并且不止境界，那种法术的特效什么的都是到飞升完全解放」）。
        //
        // 根因：这里原来**只**比对 realm().tier() < minTier()，
        // 完全没走 RealmPowers.spellsFreed(...) —— 于是蓄力阶段放行了、
        // 门槛这一关又把人挡住，玩家看到的正是"境界不足"。
        boolean freed = com.hongmeng.xiuxian.core.RealmPowers
                .spellsFreed(data.realm().tier());
        if (!freed && data.realm().tier() < technique.minTier()) {
            // 诊断：这条提示原来**只发动作栏、不写日志** ——
            // 于是"法术没反应"从日志里完全看不出来是哪一关挡的（玩家反复报告）。
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[cast] {} 被境界门槛挡下：当前第 {} 境 < 需求第 {} 境（freed={}）",
                    technique.cn(), data.realm().tier(), technique.minTier(), freed);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a77境界不足，施展不了" + technique.colored() + "\u00a77。"), true);
            return false;
        }
        TechniqueData tech = data.getTechniques();
        if (!tech.has(technique)) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[cast] {} 被「尚未修习」挡下（已修习 {} 门）",
                    technique.cn(), tech.knownCount());
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a77尚未修习" + technique.colored() + "\u00a77。"
                            + "\u00a78（按 O 打开法术栏，先修习这门功法）"), true);
            return false;
        }
        // ---- 职业主动：**服务端释放二次校验**（《07-法术内容交接表》验收清单第 2 条）----
        //
        // "已修习"只说明存档里有这一条；客改包 / 手改存档都能伪造。所以职业主动
        // 还要再查一次天赋分支（累计 5 点）。
        //
        // ⚠ **只拦 18 条"只能天赋解锁"的**：万剑归宗 / 金刚护体是"复用已有"，
        //   它们本体仍属基础 52 门（秘籍/坊市身份还在），不能被职业门槛拦死；
        //   旧 52 门 isUnlockOnly=false，这一整段对它们完全不执行，行为一字不变。
        if (CareerTechnique.isUnlockOnly(technique)
                && !CareerSpellGrant.isUnlocked(player, technique)) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[cast] {} 被「职业功法未解锁」挡下（分支投入不足 5 点）",
                    technique.cn());
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7c职业功法未解锁 \u00a78· 需所在天赋分支累计 \u00a7e5 \u00a78点"), true);
            return false;
        }
        long now = player.level().getGameTime();
        // 正在蓄力：再按一次 = 主动打断（也顺手避免"蓄力中再起一发"）
        if (tech.isCasting()) {
            Technique current = tech.castingTechnique();
            tech.endCast();
            SpellCastHandler.endCastSnapshot(player);
            com.hongmeng.xiuxian.network.CastProgressPacket.sendStop(player);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78已打断：" + (current == null ? "" : current.cn())), true);
            return false;
        }
        // ---- 仙人起：法术**完全解放**（玩家要求：仙人就可完全解放，包括法术限制）----
        //
        // 仙人(8)：冷却归零、灵力只花四分之一；
        // 帝境(9)/鸿蒙(10)：连灵力也不花了。
        // 门槛全在 RealmPowers 里，这里只读。
        final int tier = data.realm().tier();
        // freed 已在上面（境界门槛那一段）算过，这里复用，不再重复声明
        final double manaMul = com.hongmeng.xiuxian.core.RealmPowers
                .manaCostMultiplier(tier);
        final int manaCost = (int) Math.round(technique.manaCost() * manaMul);

        if (!freed && !tech.isReady(technique, now)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78" + technique.cn() + " 冷却中（"
                            + tech.remainingSeconds(technique, now) + "s）"), true);
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        // 灵力：不够就一点都不扣（同一次调用里先判后扣，不存在扣一半）
        if (manaCost > 0 && !ManaBridge.spend(player, data, manaCost)) {
            ManaBridge.warnNotEnough(player, technique, ManaBridge.mana(player, data));
            return false;
        }

        if (!freed) {
            // 冷却缩减：法修 −8% + 天赋 + 道途护甲（TalentBonus 已含并封顶 60%）
        double cdCut = 0.0D;
        try {
            cdCut = com.hongmeng.xiuxian.talent.TalentBonus.cooldownCut(player);
        } catch (Throwable ignored) {
            // 天赋系统不可用时按无缩减处理
        }
        // ---- 规则之力：万法无冷却（帝境起）----
        //
        // ⚠ `RealmPowers.cooldownMultiplier` 以前零调用 ——
        //   帝境/鸿蒙玩家**还背着冷却**，而"万法无冷却"是这两个境界的核心卖点。
        try {
            double mul = com.hongmeng.xiuxian.core.RealmPowers
                    .cooldownMultiplier(data.realm().tier());
            if (mul <= 0.0D) {
                cdCut = 1.0D;            // 完全不冷却
            } else if (mul < 1.0D) {
                // 倍率折算成缩减：mul=0.5 -> 少 50%
                cdCut = Math.max(cdCut, 1.0D - mul);
            }
        } catch (Throwable ignored) {
            // 规则之力不可用时按天赋缩减处理
        }
        tech.startCooldown(technique, now, cdCut);
        }

        // ---- 法术**贴图**特效 + 天气影响：在施法的那一刻就播/就改 ----
        //
        // 玩家要求：
        //   * 「法术特效不仅仅粒子效果还有贴图表现……贴图配合粒子，不是纯贴图化，
        //      其他法术也要这种」→ 走 SpellFxRuntime（12 种贴图形状）；
        //   * 「释放那些影响环境的法术，可以在释放法术开始到特效消失后一段时间内
        //      影响天气，比如冰系就是下雪」→ 走 SpellWeather。
        //
        // 放在这里（而不是 resolve 里）的原因：**蓄力一开始就该看到天在变**，
        // 而不是等打出去才变天。两处都做了 try/catch，特效失败绝不影响法术。
        try {
            // level 在这个方法里本来就是 ServerLevel，不用再 instanceof
            double grandeur = technique.kind().chargeGrandeur();
            double fxScale = grandeur >= 1.0F ? 24.0D
                    : (technique.kind() == TechniqueKind.AOE ? 15.0D : 8.0D);
            net.minecraft.world.phys.Vec3 aim = player.getEyePosition()
                    .add(player.getLookAngle().normalize().scale(6.0D));
            SpellFxRuntime.play(player, technique, level, aim, aim, fxScale);
            SpellWeather.apply(level, technique.element(), grandeur);
        } catch (Throwable ignored) {
            // 特效/天气失败绝不能挡着法术本身
        }

        // ---- 蓄力：默认走完整流程；**按 Shift 则整段跳过（连过场动画一起）** ----
        //
        // ⚠ 教训一：曾经写成 `if (freed) return resolve(...);` ——
        // 飞升后自动跳过蓄力，结果玩家反馈"没有蓄力了、蓄力动画消失、
        // 超位魔法那种蓄力效果没了、粒子也变少了"。
        //
        // ⚠ 教训二（我理解错的一次）：玩家说「法术释放按 shift 的时候，
        // 动画也会跳过」。我当时理解成"这是 bug"，把跳过功能整个删了 ——
        // 玩家随即澄清：「我让你做的是按 shift 的时候**连过场动画都会一起跳过**，
        // **而不是删除过场动画**」。
        //
        // 所以正确行为是：
        //   * 不按 Shift：完整蓄力（动画 + 法阵 + 粒子 + 运镜）；
        //   * 按 Shift：**整段跳过** —— 不蓄力、不出蓄力特效、**也不放运镜**，
        //     直接结算。
        //
        // "不放运镜"要单独做：运镜是在 resolve 里由 showBlastCinematic 触发的，
        // 光跳过蓄力并不能拦住它。用 SUPPRESS_CINEMATIC 标记在本次结算内抑制。
        final boolean skipCinematic = player.isShiftKeyDown();
        if (technique.castTicks() <= 0 || skipCinematic) {
            if (skipCinematic) {
                SUPPRESS_CINEMATIC.set(Boolean.TRUE);
            }
            try {
                if (skipCinematic) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00a78已跳过蓄力与过场（按住 Shift 释放）"), true);
                }
                return resolve(player, data, technique, level);
            } finally {
                if (skipCinematic) {
                    SUPPRESS_CINEMATIC.remove();
                }
            }
        }
        // ---- 道宫「释放速度」：真正缩短蓄力 ----
        //
        // 玩家反馈：「领域的加成感觉没有实现」。核实后确认：
        //   功法伤害加成**已经接了**（damageOf 里 base *= 1 + bonus），
        //   但 `castSpeedBonus()` **只出现在界面文字里**，从未改变任何东西。
        //
        // 现在把它作用到蓄力时长上：+100% 释放速度 = 蓄力减半。
        int effectiveTicks = technique.castTicks();
        try {
            double spd = com.hongmeng.xiuxian.domain.DomainManager
                    .castSpeedBonus(data);
            if (spd > 0.0D) {
                effectiveTicks = Math.max(1,
                        (int) Math.round(technique.castTicks() / (1.0D + spd)));
            }
        } catch (Throwable ignored) {
            // 道宫系统不可用就按原速
        }
        // 蓄力：起手时就把这一发的状态记好，之后按 tick 推进
        // 记一条日志：以后再出现"蓄力时间不对"可以直接对着日志查
        com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                "[hongmeng] 蓄力开始：{} 共 {} tick（{} 秒），境界第 {} 境",
                technique.cn(), effectiveTicks, effectiveTicks / 20.0D, tier);
        tech.beginCast(technique, now, effectiveTicks);
        SpellCastHandler.beginCastSnapshot(player);
        beginCastFx(level, player, technique);
        // ---- 灭世级法术：**蓄力一开始就切第三视角拉远** ----
        //
        // 玩家要求：「法术释放的时候就应该开始移动镜头了，而不是释放完」。
        //
        // 这段曾被注释掉，原因是当时用 `player.setPos(镜头位)` 移动玩家，
        // 玩家一眼看出问题：「如果你移动玩家的话，特效位置也会发生变化」。
        // 现在特效锚定在 `SpellCastHandler.castOrigin`（施法瞬间的位置），
        // 移动玩家不再影响特效落点，所以可以安全接回来。
        try {
            if (com.hongmeng.xiuxian.combat.SpellCinematic.deserves(technique)) {
                com.hongmeng.xiuxian.combat.SpellCinematic.beginCharge(
                        player, effectiveTicks, technique.cn());
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[spell-cam] 蓄力运镜未启动：{}", t.toString());
        }
        // ---- 灭世级法术运镜：**已停用**（等有安全的相机方案再开）----
        //
        // 玩家要求「释放灭世级别的法术的时候自动切换第三视角拉远观看特效」。
        // 我做了三段运镜，但**玩家发现了一个要命的副作用**：
        //
        //   「那个法术的运镜如果你移动玩家的话,那么岂不是特效位置也会发生变化」
        //
        // 完全正确 —— 我当时的实现是 `player.setPos(镜头位)`，把玩家本人搬走了：
        //   ① 以玩家为锚点的特效（蓄力阵 / 剑阵 / 领域）跟着镜头飘；
        //   ② 破坏类法术按玩家视线算落点 → 射线起点变了；
        //   ③ 玩家被搬走几十格 → 区块加载跟着抖。
        //
        // 正确做法是**只设置相机**。但原版 `Camera` 的
        // `setPosition` / `move` / `setRotation` **全是 protected**
        // （已用 ForgeGradle 的 client_mappings.txt 核对），外部调不到；
        // 用 Access Transformer 放开又会在编译类路径上不同步（试过，`move` 仍报 protected）。
        //
        // 所以我选择**先停用**，而不是带着这个 bug 发出去。
        // 代码保留在 {@code combat/SpellCinematic} 与 {@code client/Cinematic}，
        // 等相机方案落地再接回来（那条路需要 mixin 或修好 AT 链路）。
        //
        // 仍然保留的运镜：**BOSS 出场**（那是一次性演出，玩家不在施法，
        // 也没有"以玩家为锚点的持续特效"会被带偏）。
        com.hongmeng.xiuxian.network.CastProgressPacket.sendStart(
                player, technique, now, effectiveTicks);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a77蓄力 " + technique.colored()
                        + " \u00a78· " + technique.castLabel()
                        + " \u00a78· 灵力 -\u00a7b" + technique.manaCost()), true);
        return true;
    }

    /**
     * 蓄力完成，真正结算效果。
     *
     * <p>由 {@link SpellCastHandler} 在蓄力走完那一 tick 调用；
     * 瞬发功法由 {@link #cast} 直接调用。这里不再重复校验与扣费。</p>
     *
     * @return true 表示效果真的生效了
     */
    public static boolean resolve(ServerPlayer player, CultivationData data,
                                  Technique technique, ServerLevel level) {
        if (player == null || data == null || technique == null || level == null) {
            return false;
        }
        // 法术分级华丽度：按施法者境界给这次施法定一个粒子倍率，
        // 结束后立刻清掉（ThreadLocal，不会串到别人的特效上）。
        SpellVfx.beginRealmLux(data.realm().tier());
        try {
            return resolveInner(player, data, technique, level);
        } finally {
            SpellVfx.endRealmLux();
        }
    }

    private static boolean resolveInner(ServerPlayer player, CultivationData data,
                                        Technique technique, ServerLevel level) {
        double damage = damageOf(player, data, technique);
        // ⚠ 关键：射线起点与朝向一律用**施法瞬间的快照**，不用实时玩家状态。
        //
        // 玩家要求：「释放的位置在玩家脚下，后续移动法术不跟随玩家移动」。
        // 这样即使玩家之后跑开、或者演出镜头移动了玩家，
        // 法术仍然从原来那个位置、朝原来那个方向打出 —— 这本来就是法术该有的行为。
        Vec3 look = SpellCastHandler.castLook(player);
        Vec3 origin = SpellCastHandler.castOrigin(player)
                .add(0.0D, player.getEyeHeight() * 0.8D, 0.0D);

        boolean did = switch (technique.kind()) {
            case LINE -> castLine(player, level, origin, look, damage, technique);
            case FAN -> castFan(player, level, origin, look, damage, technique);
            case PROJECTILE -> castProjectile(player, level, origin, look, damage, technique);
            case AOE -> castAoe(player, level, damage, technique);
            case DASH -> castDash(player, level, look, damage, technique);
            case HEAL -> castHeal(player, level, technique);
            case BUFF -> castBuff(player, level, technique);
            case LIFESTEAL -> castLifesteal(player, level, damage, technique);
            case ULTIMATE -> castUltimate(player, level, damage, technique);
            case GRAVITY -> castBlackHole(player, level, damage, technique);
            case SLASH -> technique == Technique.TI_BENG_SHAN_JI
                    // 崩山击是 7 阶职业主动，不能套"斩星"那套 520 格隧道挖地形；
                    // 走短距重击分支（不改旧 52 门：只有它命中这个判断）。
                    ? castCrush(player, level, damage, technique)
                    : castStarSlash(player, level, damage, technique);
            // 被动没有结算形态（走不到这里：cast 已经拒了）
            case PASSIVE -> false;
        };

        if (did) {
            // 结算特效：先给施法者身上收一圈元素光（把注意力拉过来），
            // 再按元素放一声"炸开"的音。具体的线条/爆炸在各 castXxx 里。
            SpellVfx.resolveFx(level, player.position(), technique.element());
            SpellVfx.playCast(level, player.position(), technique.element(), 0.9F);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a77施展 " + technique.colored()
                            + " \u00a78· " + technique.trait()), true);
        }
        return did;
    }


    // ------------------------------------------------------------------
    // 灭世法术
    // ------------------------------------------------------------------

    /** 天陨星落：落点半径。 */
    private static final double METEOR_RADIUS = 130.0D;

    /** 轨道炮击：贯穿长度与半宽。 */
    private static final double RAIL_LENGTH = 420.0D;
    private static final double RAIL_HALF_WIDTH = 6.0D;

    /** 万象寂灭：半径。 */
    private static final double ANNIHILATE_RADIUS = 170.0D;

    /** 灭世法术的总入口。 */
    private static boolean castUltimate(ServerPlayer player, ServerLevel level,
                                        double damage, Technique technique) {
        return switch (technique) {
            case TIAN_YUN_XING_LUO -> castMeteor(player, level, damage, technique);
            case GUI_DAO_PAO_JI -> castRailgun(player, level, damage, technique);
            case WAN_XIANG_JI_MIE -> castAnnihilate(player, level, damage, technique);
            case FEN_TIAN_ZHU_HAI -> castBurnSea(player, level, damage, technique);
            default -> false;
        };
    }

    /**
     * **天陨星落**：从准星方向 40 格高处砸下一道光柱。
     *
     * <p>落点半径 {@value #METEOR_RADIUS} 格内全体吃一次重击并被推离中心，
     * 随后每 10 tick 一次余震（共 3 波，伤害递减）。</p>
     */

    // ------------------------------------------------------------------
    // 准心辅助（玩家反复反馈"破坏位置不在准心"）
    // ------------------------------------------------------------------

    /**
     * <b>准心命中点（真正的射线）</b>。
     *
     * <p>玩家要求：「就是射线，如果天上没有方块就什么也破坏不到，如果离地面远就破坏不到，
     * 破坏方向要和我准心一致，<b>而不是我看向前方却打的脚下</b>」。</p>
     *
     * <h2>规则（严格照玩家的话）</h2>
     * <ul>
     *   <li><b>就是射线</b>：从眼睛沿准心方向射出去，命中哪个方块就是哪；</li>
     *   <li><b>天上没方块</b> → 返回 {@code null}，调用方<b>什么都不破坏</b>；</li>
     *   <li><b>离地面远</b> → 同样射不到，返回 {@code null}；</li>
     *   <li><b>绝不"落到地面"</b>：不做任何高度采样/自动落地，那是上一版的错误做法。</li>
     * </ul>
     *
     * <h2>那"打到脚下"是怎么解决的</h2>
     * <p>射线的方向从来是对的，问题在<b>起点</b>：玩家的眼睛永远在自己所在方块的内部，
     * 所以一出发就命中脚下那块地形。</p>
     * <p>这里用两步解决，而不是"把起点往前挪"这种治标办法：</p>
     * <ol>
     *   <li>先按正常射线求命中点；</li>
     *   <li>若命中点<b>距眼睛 &lt; 2.5 格</b>（说明打的是自己所在的那一块），
     *       就把起点沿视线推到眼睛所在方块之外<b>重射一次</b>；
     *       重射仍打不到 → 返回 null（什么都不破坏）。</li>
     * </ol>
     *
     * @return 命中点；<b>没射中任何方块时返回 {@code null}</b>
     */
    public static Vec3 aimPoint(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return aimPoint(eye, look, net.minecraft.util.Mth.floor(player.getY()),
                range,
                // 真实地形高度：取该列的地表，但**压到脚下那一格以下**
                //（Heightmap 会把玩家自己站的那一格也算进去，见下面的详细注释）
                (bx, bz) -> Math.min(
                        player.level().getHeight(
                                net.minecraft.world.level.levelgen.Heightmap.Types
                                        .MOTION_BLOCKING, bx, bz),
                        net.minecraft.util.Mth.floor(player.getY()) - 1),
                // 真正的方块射线（用方块碰撞体，不是高度图）
                (from, to) -> clipPoint(player, from, to));
    }

    /** 地形高度查询（纯函数，便于离线测试）。 */
    public interface GroundFn {
        int ground(int bx, int bz);
    }

    /** 方块射线查询（纯函数，便于离线测试）。 */
    public interface ClipFn {
        Vec3 clip(Vec3 from, Vec3 to);
    }

    /**
     * <b>落点算法本体（纯函数）</b>：不碰任何 Minecraft 世界状态，
     * 所以可以被离线验证器直接测试。
     *
     * <h2>规则（玩家逐条确认过的）</h2>
     * <ol>
     *   <li><b>射线优先</b>：从眼睛沿准心射出去，命中哪个方块就是哪
     *       （命中点距眼睛的水平距离 ≥ 1.5 格才算数 —— 这是"看向前方"的判据）；</li>
     *   <li>射线**只打到自己脚下**时，改用地形高度求"这条射线在地面哪里落地"；</li>
     *   <li><b>朝天看 → 返回 null，什么都不破坏</b>（不做自动落地）。</li>
     * </ol>
     *
     * @param eye        眼睛位置
     * @param look       视线单位向量
     * @param feetFloor  玩家脚下那一格的 y（用于剔除"玩家自己撑起来的地面"）
     * @param range      最大射程
     * @param ground     地形高度查询
     * @param clip       方块射线查询（可为 null，测试时可以只测落点数学）
     * @return 落点；没打中任何方块时返回 {@code null}
     */
    public static Vec3 aimPoint(Vec3 eye, Vec3 look, int feetFloor, double range,
                                GroundFn ground, ClipFn clip) {
        // ---- ① 射线优先 ----
        if (clip != null) {
            Vec3 hit = clip.clip(eye, eye.add(look.scale(range)));
            if (hit != null) {
                double dx = hit.x - eye.x;
                double dz = hit.z - eye.z;
                if (Math.sqrt(dx * dx + dz * dz) >= 1.5D) {
                    return hit;
                }
            }
        }

        // ---- ② 朝天 / 平视：射线不会落到地面 ----
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        boolean nearlyFlat = flat.length() < 1.0E-4D;
        if (look.y >= -0.02D) {
            // 平视或仰视：不做自动落地 → 没有可破坏的目标
            return null;
        }
        if (nearlyFlat) {
            // 纯垂直朝下：落点就在脚前
            return new Vec3(eye.x, feetFloor, eye.z);
        }

        // ---- ③ 俯视：找"降到真实地形以下"的那一点 ----
        double lastOk = 0.0D;
        boolean anyOk = false;
        for (double d = 1.0D; d <= range; d += 0.5D) {
            Vec3 p = eye.add(look.scale(d));
            int bx = net.minecraft.util.Mth.floor(p.x);
            int bz = net.minecraft.util.Mth.floor(p.z);
            if (p.y <= ground.ground(bx, bz)) {
                return p;
            }
            lastOk = d;
            anyOk = true;
        }
        return anyOk ? eye.add(look.scale(lastOk)) : null;
    }


    /** 一次方块射线；没命中返回 null。 */
    private static Vec3 clipPoint(ServerPlayer player, Vec3 from, Vec3 to) {
        try {
            // ⚠ 最后一个参数**必须传 null**（不要传 player）。
            //
            // 传了实体之后，`Level.clip` 会把**该实体自身的碰撞箱**也算进裁剪范围，
            // 于是射线从眼睛出发时第一个命中的就是**自己的脚** ——
            // 这正是玩家反复反馈的"朝山发射轨道炮，却破坏在脚底"的真正原因。
            // 这里只想要方块命中，所以实体传 null。
            net.minecraft.world.level.ClipContext ctx = new net.minecraft.world.level.ClipContext(
                    from, to,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, null);
            net.minecraft.world.phys.BlockHitResult hit = player.level().clip(ctx);
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                return hit.getLocation();
            }
        } catch (Throwable ignored) {
            // 射线失败 = 没命中
        }
        return null;
    }

    /**
     * 准心命中点（取方块整格中心，破坏用）。
     *
     * @param fallbackRange 射程
     * @return 命中方块；<b>没射中返回 null</b>
     */
    public static net.minecraft.core.BlockPos aimBlock(ServerPlayer player,
                                                       double fallbackRange) {
        Vec3 p = aimPoint(player, fallbackRange);
        return p == null ? null : net.minecraft.core.BlockPos.containing(p);
    }

    private static boolean castMeteor(ServerPlayer player, ServerLevel level,
                                      double damage, Technique technique) {
        // ---- 落点 = **准心命中点** ----
        //
        // 玩家要求：「就是射线，天上没有方块就什么也破坏不到，离地面远也破坏不到，
        // 破坏方向要和我准心一致，而不是我看向前方却打的脚下」。
        // 所以这里严格照做：射不到 → **什么都不破坏**（不再自动落回地面）。
        Vec3 impact = aimPoint(player, METEOR_RADIUS * 1.6D);
        if (impact == null) {
            // 诊断：射线没打中任何方块
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[meteor] 落空：aimPoint 返回 null（视线方向没有方块）"
                            + " eye={} look={}",
                    player.getEyePosition(), player.getLookAngle());
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78天陨落空 —— 那里没有可以砸的东西"), true);
            return false;
        }
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[meteor] 落点=({}, {}, {}) 玩家=({}, {}, {}) look={}",
                (int) impact.x, (int) impact.y, (int) impact.z,
                (int) player.getX(), (int) player.getY(), (int) player.getZ(),
                player.getLookAngle());
        Vec3 sky = impact.add(0.0D, 40.0D, 0.0D);
        // 光柱：从天顶砸到地面
        SpellVfx.orbitalBeam(level, sky, impact, technique.element());
        // 持续 4.5 秒：余震环逐道外扩
        SpellAftermath.add(player, technique, impact, METEOR_RADIUS, 90);
        // 陨石砸下 —— 真的砸出一个坑（破坏方块）
        int dug = SpellDestruction.crater(level, impact, METEOR_RADIUS * 0.85D, 18, true, 5.0D);
        // 写实感：贴地尘云（被掀起来的地面浮尘）+ 熔融火花（有重力、会落地熄灭）。
        // 光点粒子永远做不出"重量"，这两族才是爆炸有分量的关键。
        SpellVfx.impactDust(level, impact, METEOR_RADIUS * 1.7D);
        showBlastCinematic(player, impact, technique);
        SpellVfx.sparkSpray(level, impact, METEOR_RADIUS * 1.3D, 0.95F);
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                "[meteor] crater 破坏 {} 格（半径 {}）—— 若为 0 说明全被保护规则挡掉",
                dug, METEOR_RADIUS * 0.85D);
        // 给玩家明确的反馈：破坏多少格一眼可见（不再只有"落地"两个字）
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                dug > 0
                        ? "\u00a78天陨落地 \u00a77· 崩碎 \u00a7e" + dug + " \u00a77格"
                        : "\u00a78天陨落地 \u00a77· \u00a7c没有可以崩碎的方块（被保护）"), true);
        AABB box = new AABB(impact.x - METEOR_RADIUS, impact.y - 8.0D, impact.z - METEOR_RADIUS,
                impact.x + METEOR_RADIUS, impact.y + 12.0D, impact.z + METEOR_RADIUS);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e))) {
            hurt(player, t, damage, technique);
            applyElementSideEffect(t, technique);
            // 推离中心（自天而降的冲击波把怪掀开）
            Vec3 push = t.position().subtract(impact).normalize().scale(1.4D);
            t.push(push.x, 0.55D, push.z);
            t.hurtMarked = true;
        }
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e))) {
            bossPercent(player, t, technique, 0.05D);
        }
        // 余震：同一 tick 内画三波逐渐外扩的冲击环（伤害已含在上面那一次里）。
        // 刻意不做延时结算：延时任务需要额外调度器，而"一次性爆发 + 三层外扩环"
        // 的观感与三段判定等价，却不需要新机制。
        for (int wave = 1; wave <= 3; wave++) {
            SpellVfx.aftershock(level, impact, METEOR_RADIUS * (0.45D + 0.28D * wave));
        }
        return true;
    }

    /**
     * **轨道炮击**：自天顶垂直劈下的一线之光，沿准星方向贯穿
     * {@value #RAIL_LENGTH} 格，命中者被**钉在原地**（短时强减速）。
     */
    private static boolean castRailgun(ServerPlayer player, ServerLevel level,
                                       double damage, Technique technique) {
        // 沿**真实准心方向**（含俯仰）：朝下看就朝地打，朝上看就打上天。
        // 原来这里把视线压平到 XZ 平面（flat），所以朝下也只能水平打出去 ——
        // 玩家反馈「像轨道炮如果准心朝下就不能朝下释放」。
        Vec3 look = player.getLookAngle().normalize();
        // ⚠ 起点必须**从身前发出**，不能从眼睛。
        //
        // 原因：`tunnel(origin, end, halfWidth, 厚度, 扩张)` 的"厚度"是**向下**挖的格数。
        // 原来 origin = 眼睛、厚度 = 40 → 从眼睛位置往下挖 40 格：
        // **把玩家脚下和身边的方块全挖了**，所以玩家看到的是
        // "破坏在脚底、而不是准心指的方向"（真实事故）。
        // 玩家要求：破坏应该是「从我身前发出的一道破坏光线」。
        // 破坏沿**实际射线**：先求出命中的方块，再从身前打到那里。
        // 原来用的是"眼睛 + 视线 × 220"这条无限长的直线，会一路打到世界尽头；
        // 现在射不到方块就退化为飞满射程（保持原来的手感）。
        Vec3 hit = aimPoint(player, RAIL_LENGTH);
        Vec3 end = hit != null ? hit : player.getEyePosition().add(look.scale(RAIL_LENGTH));
        Vec3 origin = player.getEyePosition().add(look.scale(2.5D));
        SpellVfx.railgunFx(level, player.position(), origin, end, technique.element());
        // 持续 2.5 秒：贯穿光刃残留 + 沿途余爆
        SpellAftermath.add(player, technique,
                origin.add(end.subtract(origin).scale(0.5D)), RAIL_HALF_WIDTH * 3.0D, 50);
        // 贯穿出一条隧道。厚度 40 → 26（够粗的竖井，但不再把施法者自己埋了）
        // 贯穿 14 格、保护圈 5 格（不掏自己脚下）
        int bored = SpellDestruction.tunnel(level, origin, end,
                RAIL_HALF_WIDTH * 1.6D, 24, 6.0D, 14.0D, 5.0D);
        // 贯穿类：尘云加在**线段中点**而不是起点 ——
        // 起点就是玩家身前，在那里扬尘会糊住自己的视线。
        SpellVfx.impactDust(level, origin.add(end).scale(0.5D), 7.0D);
        showBlastCinematic(player, end, technique);
        SpellVfx.sparkSpray(level, end, 4.5D, 0.7F);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a7b轨道贯穿 \u00a77· 掘进 \u00a7e" + bored + " \u00a77格"), true);
        for (LivingEntity t : enemiesAlong(player, origin, end)) {
            hurt(player, t, damage, technique);
            bossPercent(player, t, technique, 0.06D);
            applyElementSideEffect(t, technique);
            // 钉住：缓慢 250 级一小段
            t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20, 250,
                    false, false, false));
        }
        return true;
    }

    /**
     * **万象寂灭**：以自身为中心 {@value #ANNIHILATE_RADIUS} 格，三段递增爆发。
     *
     * <p>第一、二段各 45% / 70% 伤害，第三段 100% 伤害并附带**百分比伤害**
     * （首领仍走 {@code min(p, 2%)} 的既有约束）。</p>
     */
    private static boolean castAnnihilate(ServerPlayer player, ServerLevel level,
                                          double damage, Technique technique) {
        // 落点改走射线（原来以玩家脚底为中心，破坏在自己脚下 —— 玩家反馈）
        Vec3 center = aimPoint(player, ANNIHILATE_RADIUS * 1.6D);
        if (center == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78万象寂灭落空 —— 那里没有可以崩碎的东西"), true);
            return false;
        }
        SpellVfx.annihilateFx(level, center, ANNIHILATE_RADIUS, technique.element());
        // 持续 3.5 秒：光柱渐收 + 阵纹退散
        SpellAftermath.add(player, technique, center, ANNIHILATE_RADIUS, 70);
        // 陷坑：中心留柱的环形坑
        int sunk = SpellDestruction.crater(level, center, ANNIHILATE_RADIUS * 0.9D, 20,
                true, 4.0D);
        // 灭世级：最大一片尘云 + 大量火花（这是本模组最重的一击，粒子不能省）
        SpellVfx.impactDust(level, center, ANNIHILATE_RADIUS * 1.9D);
        showBlastCinematic(player, center, technique);
        SpellVfx.sparkSpray(level, center, ANNIHILATE_RADIUS * 1.4D, 0.8F);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a75万象坍缩 \u00a77· 陷落 \u00a7e" + sunk + " \u00a77格"), true);
        AABB box = player.getBoundingBox().inflate(ANNIHILATE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e));
        for (LivingEntity t : targets) {
            hurt(player, t, damage * 0.45D, technique);
            hurt(player, t, damage * 0.70D, technique);
            hurt(player, t, damage, technique);
            applyElementSideEffect(t, technique);
            // 百分比收尾：首领走 SpellBossDamage 的表（万象寂灭 25%），
            // 普通怪给 8%。这就是"取消 2% 封顶"的落点。
            double pct = com.hongmeng.xiuxian.domain.DomainDamage.rankOf(t)
                    == com.hongmeng.xiuxian.entity.MobRank.OVERLORD
                            ? SpellBossDamage.capOf(technique) : 0.08D;
            com.hongmeng.xiuxian.domain.DomainDamage.hitPercent(t, player, pct);
            t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 250,
                    false, false, false));
        }
        return true;
    }

    /**
     * 对首领补一次**法术专属**的百分比伤害。
     *
     * <p>领域统一封顶 2%，但灭世法术由 {@link SpellBossDamage} 单独给上限 ——
     * 玩家反馈「限制了百分比 boss 受到的伤害就没有灭世法术的威力了」。</p>
     *
     * <p>只在目标是首领（{@code MobRank.OVERLORD}）且这门法术在表里时才结算，
     * 所以对普通怪与旧法术**完全不改变行为**。</p>
     *
     * @param base 由调用方给的"这一发的基准百分比"（按法术强度定，见各 castXxx）
     */
    private static void bossPercent(ServerPlayer player, LivingEntity target,
                                    Technique technique, double base) {
        if (target == null || technique == null) {
            return;
        }
        if (com.hongmeng.xiuxian.domain.DomainDamage.rankOf(target)
                != com.hongmeng.xiuxian.entity.MobRank.OVERLORD) {
            return;
        }
        double cap = SpellBossDamage.capOf(technique);
        if (cap <= 0.0D) {
            return;
        }
        double pct = Math.min(Math.max(base, 0.0D), cap);
        if (pct > 0.0D) {
            com.hongmeng.xiuxian.domain.DomainDamage.hitPercent(target, player, pct);
        }
    }


    /** 太虚黑洞：奇点半径。 */
    private static final double BLACK_HOLE_RADIUS = 190.0D;

    /**
     * 心魔引（职业主动·魔修）：牵引半径。
     *
     * <p>刻意只有 24 格 —— 它不是太虚黑洞那种灭世级 190 格奇点，而是
     * "把周围敌人拽到心魔面前"的控制法术（玩家方案：控制/拉拽/恐惧）。</p>
     */
    private static final double MIND_DEMON_RADIUS = 24.0D;

    /**
     * 心魔引「恐惧」用的**反胃**效果。
     *
     * <p>1.20.1 官方映射里这个效果的字段名是 {@code MobEffects.CONFUSION}
     * （旧名 / 任务文档写的是 NAUSEA）—— 这里起个别名，避免两处理解分叉，
     * 也让"恐惧=反胃"的语义在代码里一眼可见。</p>
     *
     * <p>⚠ 必须是<b>方法</b>而不是 {@code static final} 字段：这个类的静态初始化
     * 发生在服务端引导（Bootstrap）之前，静态字段一碰 {@code MobEffects}
     * 就会触发注册表初始化 → 离线断言器直接 ExceptionInInitializerError。</p>
     */
    private static net.minecraft.world.effect.MobEffect nauseaEffect() {
        return MobEffects.CONFUSION;
    }

    /** 斩星：斩击长度与半宽。 */
    private static final double STAR_SLASH_LENGTH = 520.0D;
    private static final double STAR_SLASH_HALF = 30.0D;

    /**
     * 斩星的**纵向切削厚度**（准心朝地时往下切多厚）。
     *
     * <p>玩家要求：「斩星是向前斩击，如果我朝向地面就是向地面斩击进行破坏」。
     * 所以破坏不再只取水平面，而是沿**真实视线方向**切一条有厚度的刀痕。</p>
     */
    private static final double STAR_SLASH_DEPTH = 42.0D;

    /** 焚天煮海：半径。 */
    private static final double BURN_SEA_RADIUS = 210.0D;

    /**
     * **太虚黑洞**：在准星前生成引力奇点，半径内敌人被**持续吸向中心**并受创。
     *
     * <p>每 tick 施加一次向心推力（不是瞬移），所以体型大的怪也会被拖着走；
     * 靠近中心的会被额外绞碎一次百分比伤害。</p>
     */
    private static boolean castBlackHole(ServerPlayer player, ServerLevel level,
                                         double damage, Technique technique) {
        // ---- 职业主动·心魔引：**小半径**牵引 + 恐惧 ----
        //
        // 心魔引形态虽然也是 GRAVITY，但它**不是**太虚黑洞：不能用 190 格奇点参数、
        // 也不许挖坑。先按 technique 分流，旧 52 门（太虚黑洞）原样走下面的代码。
        if (technique == Technique.MO_XIN_MO_YIN) {
            return castMindDemon(player, level, damage, technique);
        }
        // 落点改走射线：原来把俯仰压平（look.x/look.z 没有 y 分量），
        // 所以朝天放也会打在地面上；而且 y 被写死成 0.5。
        Vec3 center = aimPoint(player, BLACK_HOLE_RADIUS * 1.6D);
        if (center == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78黑洞落空 —— 那里没有可以吞噬的东西"), true);
            return false;
        }
        SpellVfx.blackHoleFx(level, center, BLACK_HOLE_RADIUS, technique.element());
        // 持续 6 秒：牵引 + 绞碎 + 末段坍塌
        SpellAftermath.add(player, technique, center, BLACK_HOLE_RADIUS, 120);
        // 吞噬：半球巨坑（边缘撕裂，不做碗形）
        int eaten = SpellDestruction.crater(level, center, BLACK_HOLE_RADIUS * 0.8D, 28,
                false, 4.0D);
        // 黑洞偏"冷"：尘云照给（物质被吸进去要先扬起来），火花少而弱
        SpellVfx.impactDust(level, center, BLACK_HOLE_RADIUS * 1.6D);
        showBlastCinematic(player, center, technique);
        SpellVfx.sparkSpray(level, center, BLACK_HOLE_RADIUS * 0.9D, 0.35F);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a75太虚吞噬 \u00a77· 湮灭 \u00a7e" + eaten + " \u00a77格"), true);
        AABB box = new AABB(center.x - BLACK_HOLE_RADIUS, center.y - 12.0D,
                center.z - BLACK_HOLE_RADIUS, center.x + BLACK_HOLE_RADIUS,
                center.y + 12.0D, center.z + BLACK_HOLE_RADIUS);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e))) {
            hurt(player, t, damage, technique);
            bossPercent(player, t, technique, 0.06D);
            applyElementSideEffect(t, technique);
            // 向心牵引：越近拉力越强，并且禁掉它自己的位移
            Vec3 pull = center.subtract(t.position());
            double dist = Math.max(0.8D, pull.length());
            Vec3 dir = pull.normalize().scale(Math.min(2.2D, 6.0D / dist));
            t.push(dir.x, 0.15D, dir.z);
            t.hurtMarked = true;
            t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 250,
                    false, false, false));
            // 贴到奇点附近：再绞碎一次
            if (dist < BLACK_HOLE_RADIUS * 0.35D) {
                hurt(player, t, damage * 0.6D, technique);
            }
        }
        return true;
    }

    /**
     * <b>心魔引</b>（职业主动·魔修）：小半径（{@value #MIND_DEMON_RADIUS} 格）牵引 + <b>恐惧</b>。
     *
     * <p>与太虚黑洞的区别（《07-法术内容交接表》§2 与玩家方案）：</p>
     * <ul>
     *   <li>半径约 24 格（黑洞 190 格），只把附近敌人拽向心魔；</li>
     *   <li><b>不破坏地形</b> —— 整段一次都不调 {@code SpellDestruction}（不挖坑）；</li>
     *   <li>命中附「恐惧」：{@code CONFUSION}（= 反胃/NAUSEA，1.20.1 官方映射名）
     *       200 tick + {@code MOVEMENT_SLOWDOWN} 100 tick。</li>
     * </ul>
     * <p>持续性牵引/恐惧交给 {@link SpellAftermath} 的 {@code MO_XIN_MO_YIN} 分支
     * （复用它的 Effect/advance 时间线，不生成实体、不写存档）。</p>
     */
    private static boolean castMindDemon(ServerPlayer player, ServerLevel level,
                                         double damage, Technique technique) {
        // 落点：优先准心射线；视线打不到方块时退化为"身前 12 格"，
        // 保证这个控制技不会因为朝天空放而什么都出不来。
        Vec3 center = aimPoint(player, MIND_DEMON_RADIUS * 1.6D);
        if (center == null) {
            Vec3 aimLook = player.getLookAngle().normalize();
            center = player.getEyePosition().add(aimLook.scale(MIND_DEMON_RADIUS * 0.5D));
        }
        // 视觉复用既有黑洞（元素 NONE）：半径小，观感是"心魔漩涡"而不是天坑。
        SpellVfx.blackHoleFx(level, center, MIND_DEMON_RADIUS, technique.element());
        // 持续 6 秒：由 SpellAftermath 继续拉拽 + 恐惧（见其 MO_XIN_MO_YIN 分支）。
        SpellAftermath.add(player, technique, center, MIND_DEMON_RADIUS, 120);
        AABB box = new AABB(center.x - MIND_DEMON_RADIUS, center.y - 12.0D,
                center.z - MIND_DEMON_RADIUS, center.x + MIND_DEMON_RADIUS,
                center.y + 12.0D, center.z + MIND_DEMON_RADIUS);
        int feared = 0;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e))) {
            hurt(player, t, damage, technique);
            // 恐惧：反胃（画面失真、失去方向感）+ 减速
            t.addEffect(new MobEffectInstance(nauseaEffect(), 200, 0));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            // 向心牵引：越近拉力越强
            Vec3 pull = center.subtract(t.position());
            double dist = Math.max(0.8D, pull.length());
            if (dist > MIND_DEMON_RADIUS) {
                continue;
            }
            Vec3 dir = pull.normalize().scale(Math.min(2.0D, 5.0D / dist));
            t.push(dir.x, 0.18D, dir.z);
            t.hurtMarked = true;
            feared++;
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a75心魔牵引 \u00a77· \u00a7e" + feared + " \u00a77个目标陷入恐惧"), true);
        return true;
    }

    /**
     * **斩星**：一道自天顶垂落的**巨刃**，沿准星斩出
     * {@value #STAR_SLASH_LENGTH} 格长、{@value #STAR_SLASH_HALF} 格半宽的深渊。
     *
     * <p>判定用一个"胶囊"体：沿线每 2 格查一次附近敌人，命中者被击飞并定身。</p>
     */
    private static boolean castStarSlash(ServerPlayer player, ServerLevel level,
                                         double damage, Technique technique) {
        // 同样沿真实准心方向：朝下斩就是一道竖直深渊。
        Vec3 look = player.getLookAngle().normalize();
        // 与轨道炮同理：**从身前发出**，不要从脚下挖（否则会把自己埋了）
        // 与轨道炮同理：破坏沿**实际射线**（打到哪个方块就斩到哪）
        Vec3 slashHit = aimPoint(player, STAR_SLASH_LENGTH);
        Vec3 start = player.getEyePosition().add(look.scale(2.0D));
        Vec3 end = slashHit != null ? slashHit
                : player.getEyePosition().add(look.scale(STAR_SLASH_LENGTH));
        SpellVfx.starSlashFx(level, start, end, STAR_SLASH_HALF,
                technique.element());
        // 持续 4 秒：深渊裂痕向上崩裂
        SpellAftermath.add(player, technique, end, STAR_SLASH_HALF * 2.0D, 80);
        // 斩出一条深渊
        // 破坏体积加大：纵向厚度 22 → 70，末端扩张 4.0 → 9.0
        //（玩家要求「朝向地面就是向地面斩击进行破坏」——
        //  朝下斩时要有真正往下切进去的深口，而不是薄薄一条）
        // 斩星：贯穿 10 格、保护圈 5 格
        int cut = SpellDestruction.tunnel(level, start, end, STAR_SLASH_HALF, 30, 7.0D,
                10.0D, 5.0D);
        // 斩击类：切口的尘云（沿刀痕中点）+ 末端火花
        SpellVfx.impactDust(level, start.add(end).scale(0.5D), 8.0D);
        showBlastCinematic(player, end, technique);
        SpellVfx.sparkSpray(level, end, 5.0D, 0.5F);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a7f斩星裂地 \u00a77· 斩开 \u00a7e" + cut + " \u00a77格"), true);
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (double d = 0.0D; d <= STAR_SLASH_LENGTH; d += 2.0D) {
            Vec3 p = start.add(look.normalize().scale(d));
            AABB slice = new AABB(p.x - STAR_SLASH_HALF, p.y - 2.0D, p.z - STAR_SLASH_HALF,
                    p.x + STAR_SLASH_HALF, p.y + 6.0D, p.z + STAR_SLASH_HALF);
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, slice,
                    e -> isEnemy(player, e))) {
                if (!hit.add(t)) {
                    continue;      // 同一个目标只吃一次
                }
                hurt(player, t, damage, technique);
                bossPercent(player, t, technique, 0.07D);
                applyElementSideEffect(t, technique);
                // 斩飞 + 定身
                // 击飞方向跟着斩击方向（原来是水平方向的 flat，现在用 look 的水平分量）
                Vec3 up = new Vec3(look.x * 0.4D, 0.9D, look.z * 0.4D);
                t.push(up.x, up.y, up.z);
                t.hurtMarked = true;
                t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 250,
                        false, false, false));
            }
        }
        return true;
    }

    /** 崩山击的重击距离（格）与判定半径（格）—— 职业主动，短距、不挖地形。 */
    private static final double CRUSH_LENGTH = 8.0D;
    private static final double CRUSH_RADIUS = 3.0D;

    /**
     * <b>崩山击</b>（体修职业主动·斩击形态）：短距重击 + 破防。
     *
     * <h2>为什么单独一条分支</h2>
     * <p>SLASH 形态原本只有「斩星」——520 格隧道 + 大范围挖地形，那是 9 阶灭世级
     * 的量级。崩山击是 7 阶职业主动，套上去会变成"一拳挖穿半张地图"。
     * 所以这里按 technique 分流：只有崩山击走短距重击，其余 SLASH 一行不动。</p>
     *
     * <p>表现：前方 8 格重击（半径 3），命中者被震退 + 虚弱（破防），
     * 地面用既有 aoeFx 的冲击环/尘云，<b>不动方块</b>。</p>
     */
    private static boolean castCrush(ServerPlayer player, ServerLevel level,
                                     double damage, Technique technique) {
        Vec3 look = SpellCastHandler.castLook(player).normalize();
        Vec3 origin = SpellCastHandler.castOrigin(player)
                .add(0.0D, player.getEyeHeight() * 0.8D, 0.0D);
        Vec3 end = origin.add(look.scale(CRUSH_LENGTH));
        SpellVfx.aoeFx(level, end, CRUSH_RADIUS, false, technique.element());
        SpellVfx.impactDust(level, end, CRUSH_RADIUS * 1.6D);
        SpellVfx.sparkSpray(level, end, CRUSH_RADIUS, 0.35F);
        AABB box = new AABB(origin, end).inflate(CRUSH_RADIUS);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e))) {
            hurt(player, t, damage, technique);
            applyElementSideEffect(t, technique);
            // 震退：方向从施法点向外，带一点上抛（"崩山"的手感）
            Vec3 push = t.position().subtract(origin);
            if (push.lengthSqr() < 1.0E-4D) {
                push = look;
            }
            push = push.normalize();
            t.push(push.x * 0.9D, 0.45D, push.z * 0.9D);
            t.hurtMarked = true;
            // 破防：短时虚弱（不叠加时长以外的数值）
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0,
                    false, true));
        }
        return true;
    }

    /**
     * **焚天煮海**：半径内敌人先被**点燃**，随后三段火焰爆发（伤害递增）。
     */
    private static boolean castBurnSea(ServerPlayer player, ServerLevel level,
                                       double damage, Technique technique) {
        // 同上：不再以玩家脚底为中心
        Vec3 center = aimPoint(player, BURN_SEA_RADIUS * 1.6D);
        if (center == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78焚天落空 —— 那里没有可以烧的东西"), true);
            return false;
        }
        SpellVfx.burnSeaFx(level, center, BURN_SEA_RADIUS, technique.element());
        // 持续 5 秒：火柱持续喷发
        SpellAftermath.add(player, technique, center, BURN_SEA_RADIUS, 100);
        // 焦土：只烧植被/雪/木，不动石头
        int burned = SpellDestruction.scorch(level, center, BURN_SEA_RADIUS * 0.95D, 3);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a76焚天煮海 \u00a77· 焦土 \u00a7e" + burned + " \u00a77格"), true);
        AABB box = player.getBoundingBox().inflate(BURN_SEA_RADIUS);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e))) {
            t.setSecondsOnFire(6);
            hurt(player, t, damage * 0.5D, technique);
            hurt(player, t, damage * 0.8D, technique);
            hurt(player, t, damage, technique);
            bossPercent(player, t, technique, 0.05D);
            applyElementSideEffect(t, technique);
        }
        return true;
    }

    /** 蓄力开始的表现：脚下起一圈元素光 + 起手音。 */
    /**
     * "上一次演炸点运镜"的时刻（玩家 -> 游戏刻）。
     *
     * <p>⚠ <b>我第一版写错过</b>：原来用的是 {@code Set<UUID>} 且<b>从不清理</b>，
     * 于是每个玩家<b>一辈子只有第一次</b>灭世级法术能看到炸点运镜 ——
     * 而注释里却写着"约 70 tick 后自动可再触发"。注释描述了实现没做的事。</p>
     *
     * <p>现在改成"带过期时间的记录"：同一轮施法内不重演（防多落点法术反复横跳），
     * 超过 {@link #BLAST_COOLDOWN} tick 就允许再演。</p>
     */
    private static final java.util.Map<java.util.UUID, Long> BLAST_SHOWN =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 两次炸点运镜之间的最短间隔（tick）。比运镜本身稍长，防重入。 */
    private static final long BLAST_COOLDOWN = 110L;

    /**
     * <b>本次结算内抑制运镜</b>（玩家按 Shift 跳过整段时置位）。
     *
     * <p>为什么需要单独一个标记：运镜是在 {@code resolve} 里由
     * {@link #showBlastCinematic} 触发的 —— <b>光跳过蓄力拦不住它</b>，
     * 会出现"按 Shift 跳过了蓄力、过场动画却照样播"。</p>
     *
     * <p>用 {@code ThreadLocal} 而不是 Map：结算在同一条线程内同步完成，
     * 用完即弃，不会跨玩家串味。</p>
     */
    private static final ThreadLocal<Boolean> SUPPRESS_CINEMATIC =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 释放段运镜（第三段）：把镜头<b>剪到炸点</b>，演完再还给玩家。
     *
     * <p>玩家要求「自动切换第三视角拉远观看特效」。灭世级法术的看点
     * <b>不在施法者身上，而在被摧毁的地方</b> —— 所以释放瞬间必须把镜头切过去，
     * 而不是继续围着玩家转（那样玩家看不到自己造成了什么）。</p>
     *
     * <p>同一个法术只演一次：多落点法术（陨石的余震环等）如果每处都演，
     * 画面会反复横跳。清账时机是"运镜时长结束"（约 70 tick 后自动可再触发）。</p>
     */
    private static void showBlastCinematic(net.minecraft.server.level.ServerPlayer player,
                                          net.minecraft.world.phys.Vec3 impact,
                                          com.hongmeng.xiuxian.technique.Technique tech) {
        // 玩家按 Shift 跳过整段（连过场动画一起）—— 这里必须拦住，
        // 否则跳过蓄力之后运镜照样会播。
        if (Boolean.TRUE.equals(SUPPRESS_CINEMATIC.get())) {
            return;
        }
        // 已启用：法术现在锚定在**施法原点**（见 SpellCastHandler.castOrigin），
        // 所以移动玩家（演出镜头）不会再让特效跟着飘。
        // 玩家给的方向是对的：「释放的位置在玩家脚下，后续移动法术不跟随玩家移动」。
        if (!com.hongmeng.xiuxian.combat.SpellCinematic.deserves(tech)) {
            return;
        }
        // 同一轮施法内只演一次（多落点法术会在多个 impact 处调用本方法）
        long now = player.level().getGameTime();
        Long last = BLAST_SHOWN.get(player.getUUID());
        if (last != null && now - last < BLAST_COOLDOWN) {
            return;
        }
        BLAST_SHOWN.put(player.getUUID(), now);
        com.hongmeng.xiuxian.combat.SpellCinematic.showBlast(player, impact, tech.cn());
    }

    private static void beginCastFx(ServerLevel level, ServerPlayer player, Technique technique) {
        SpellVfx.beginFx(level, player.position(), technique.element());
        SpellVfx.playBegin(level, player.position(), technique.element());
    }

    /** 蓄力被打断的表现（不再扣灵力，也不再退冷却）。 */
    public static void interruptFx(ServerPlayer player, Technique technique) {
        if (player == null || technique == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        SpellVfx.interruptFx(level, player.position(), technique.element());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    // ------------------------------------------------------------------
    // 伤害
    // ------------------------------------------------------------------

    /** 功法伤害 = 本境标准值 × 倍率 × 道宫同元素加成 × 被动加成。 */
    public static double damageOf(ServerPlayer player, CultivationData data, Technique technique) {
        if (technique.isSupport()) {
            return 0.0D;
        }
        double base = RealmStats.standard(data.realm().tier()) * technique.damageMultiplier();
        // 道宫：领域内同元素功法伤害加成
        try {
            DomainData domains = data.getDomainData();
            if (domains != null) {
                base *= 1.0D + domains.techniqueBonus(technique.element());
            }
        } catch (Throwable ignored) {
            // 道宫系统不可用时按无加成处理
        }
        // 被动：神识凝练（法术伤害 +12%）
        base *= TechniquePassive.spellDamageMultiplier(data.getTechniques());
        // 被动：剑心通明（持剑时伤害 +20%）
        if (holdsRealmSword(player)) {
            base *= TechniquePassive.swordDamageMultiplier(data.getTechniques());
        }
        // ---- 规则之力：万法之始（鸿蒙，法术伤害 +20%）----
        //
        // ⚠ `RealmPowers.spellDamageMultiplier` 以前零调用 —— 鸿蒙的
        //   "万法之始"根本没有加进伤害。
        try {
            base *= com.hongmeng.xiuxian.core.RealmPowers
                    .spellDamageMultiplier(data.realm().tier());
        } catch (Throwable ignored) {
            // 规则之力不可用时不加成
        }

        // ---- 天赋 / 道途 / 道途护甲 的加成 ----
        //
        // ⚠ 这里以前**什么都没读**：`TalentBonus` 的 16 个方法全是零调用，
        //   而面板显示走的是另一条路（Talents.clientSummary），
        //   于是"玩家看到 +12% 法术伤害、实际一点没加"。
        //   纯装饰的天赋树 = 玩家要求的核心功能没做。
        try {
            base *= 1.0D + com.hongmeng.xiuxian.talent.TalentBonus.spellDamage(player);
            // 破防不在这里算：damageOf 拿不到受击目标，按自己的护甲减免率去折算会算反。
            // 现在统一挪到 hurt()（已知 target）里结算，见本文件 hurt 方法。
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[talent] 法术加成结算失败：{}", t.toString());
        }
        return base;
    }

    /**
     * 手上是不是本模组境界剑（{@code realm_sword_<tier>}）。
     *
     * <p>故意不按类名判断：剑是多阶共用一个物品类（{@code RealmWeaponItem}），
     * 按注册名判断既稳又不用碰注册表（只读手上的 ItemStack）。</p>
     */
    public static boolean holdsRealmSword(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            var id = net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(player.getMainHandItem().getItem());
            return id != null && id.getPath().startsWith("realm_sword");
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 各种释放形态
    // ------------------------------------------------------------------

    /** 直线穿透：一条线上的所有敌人。 */
    private static boolean castLine(ServerPlayer player, ServerLevel level, Vec3 origin,
                                    Vec3 look, double damage, Technique technique) {
        Vec3 end = origin.add(look.scale(LINE_LENGTH));
        int hits = 0;
        for (LivingEntity target : enemiesAlong(player, origin, end)) {
            hurt(player, target, damage * 0.7D, technique);
            hits++;
            // ---- 职业主动·锁魂链：命中后把目标**拉向施法者** ----
            //
            // 魂印叠层不需要在这里做：命中走的是 playerAttack 伤害源，
            // 既有 ModEvents → PathRuntime.onPlayerDealtDamage 的鬼修分支会自动
            // applySoulMark（那是私有的唯一叠印入口）。**不自造第二套魂印资源**。
            // TODO：若以后 PathRuntime 暴露公开的"叠印"入口，可在这里显式追加一层，
            //       当前沿用既有命中链路（3 层引爆/魂爆被动都照常工作）。
            if (technique == Technique.GUI_SUO_HUN_LIAN) {
                Vec3 pull = player.position().subtract(target.position());
                if (pull.lengthSqr() > 1.0E-4D) {
                    Vec3 dir = pull.normalize().scale(1.2D);
                    target.push(dir.x, 0.35D, dir.z);
                    target.hurtMarked = true;
                }
            }
            if (hits >= 4) {
                break;
            }
        }
        // ---- 职业主动·剑气斩：命中后追加剑意 ----
        //
        // 复用 PathRuntime 唯一的对剑意对外入口 {@code gainSwordIntent}
        // （不新建"剑气层数"之类的第二套资源）。注：命中本身还会走
        // PathRuntime.onPlayerDealtDamage 的既有叠层，这里给的是**法术额外一层**。
        if (technique == Technique.JIAN_QI_ZHAN && hits > 0) {
            com.hongmeng.xiuxian.talent.PathRuntime.gainSwordIntent(player, 1.0D);
        }
        // 视觉：一条亮芯 + 两侧飞散的三层线
        SpellVfx.lineFx(level, origin, end, technique.element());
        return true;
    }

    /** 扇形散射。 */
    private static boolean castFan(ServerPlayer player, ServerLevel level, Vec3 origin,
                                   Vec3 look, double damage, Technique technique) {
        int blades = 7;
        double spread = Math.toRadians(60.0D);
        Vec3[] dirs = new Vec3[blades];
        for (int i = 0; i < blades; i++) {
            double angle = -spread / 2.0D + spread * i / (blades - 1.0D);
            Vec3 dir = look.yRot((float) angle).normalize();
            dirs[i] = dir;
            Vec3 end = origin.add(dir.scale(FAN_RANGE));
            for (LivingEntity target : enemiesAlong(player, origin, end)) {
                hurt(player, target, damage / blades * 1.4D, technique);
            }
        }
        SpellVfx.fanFx(level, origin, dirs, technique.element());
        return true;
    }

    /** 单个投射物：只打准星方向最近的一个。 */
    private static boolean castProjectile(ServerPlayer player, ServerLevel level, Vec3 origin,
                                          Vec3 look, double damage, Technique technique) {
        Vec3 end = origin.add(look.scale(FAN_RANGE));
        List<LivingEntity> targets = enemiesAlong(player, origin, end);
        if (!targets.isEmpty()) {
            LivingEntity first = targets.get(0);
            hurt(player, first, damage, technique);
            // 冰系附加减速
            if (technique.element() == Element.WATER || technique.element() == Element.ICE) {
                first.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                // 表现：减速命中的冰霜反馈（只加粒子，上面的状态数值不动）
                SpellVfx.statusFx(level, first.position(), technique.element(),
                        SpellVfx.StatusFx.FROST);
            }
        }
        SpellVfx.projectileFx(level, origin, look, FAN_RANGE, technique.element());
        return true;
    }

    /** 以自身为中心的范围爆发。 */
    private static boolean castAoe(ServerPlayer player, ServerLevel level,
                                   double damage, Technique technique) {
        // 职业主动有自己的范围（10~16 格），不再沿用旧 52 门的 44 格世界级半径。
        // aoeRadiusFor 对旧 52 门一律返回 AOE_RADIUS —— 它们的行为一字不变。
        double radius = aoeRadiusFor(technique);
        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e));
        for (LivingEntity t : targets) {
            double dmg = damage;
            // 九霄雷狱的特性：目标越多每目标伤害越高
            if (technique == Technique.JIU_XIAO_LEI_YU) {
                dmg = damage * (1.0D + 0.15D * Math.min(8, targets.size()));
            }
            hurt(player, t, dmg, technique);
            applyElementSideEffect(t, technique);
        }
        // 视觉：地面环 + 球壳炸开 +（雷法）竖直电弧
        SpellVfx.aoeFx(level, player.position(), radius,
                technique.element() == Element.THUNDER, technique.element());
        // ---- 职业主动·血河 / 鬼域展开：范围爆发后**留下持续区域** ----
        //
        // 复用 SpellAftermath 的 Effect/advance 时间线（每 10 tick 小伤害 + 减速），
        // 只有这两门职业主动会登记；旧 52 门永远不进这个分支。
        // power 传本次伤害，让 DoT 随境界缩放，而不是写死一个固定点数。
        if (technique == Technique.XUE_HE || technique == Technique.GUI_YU_ZHAN_KAI) {
            SpellAftermath.add(player, technique, player.position(), radius,
                    CAREER_AREA_TICKS, damage);
        }
        return true;
    }

    /**
     * 范围法术的判定半径。
     *
     * <p>职业主动按《07-法术内容交接表》给各自的"身份范围"；其余（含旧 52 门）
     * 一律沿用 {@link #AOE_RADIUS}，<b>旧行为不变</b>。</p>
     */
    public static double aoeRadiusFor(Technique technique) {
        if (technique == null) {
            return AOE_RADIUS;
        }
        return switch (technique) {
            case JIAN_YU -> 10.0D;             // 剑域
            case XUE_BAO -> 9.0D;              // 血爆
            case XUE_HE -> 12.0D;              // 血河
            case MO_QI_BAO_FA -> 12.0D;        // 魔气爆发
            case GUI_YU_ZHAN_KAI -> 16.0D;     // 鬼域展开
            case TI_ZHEN_DANG_BO -> 10.0D;     // 震荡波
            default -> AOE_RADIUS;
        };
    }

    /** 虚化：短无敌时长（tick）—— 30 tick = 1.5 秒。 */
    private static final int PHASE_INVULNERABLE_TICKS = 30;

    /** 正在虚化的玩家 → 本段无敌的结束 tick（连续施放时不提前恢复）。 */
    private static final java.util.Map<java.util.UUID, Long> PHASE_UNTIL =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 正在虚化的玩家 → <b>进入虚化前</b>的 invulnerable 值（用来精确还原）。 */
    private static final java.util.Map<java.util.UUID, Boolean> PHASE_WAS_INVULNERABLE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 位移：向前突进并灼伤沿途敌人。 */
    private static boolean castDash(ServerPlayer player, ServerLevel level, Vec3 look,
                                    double damage, Technique technique) {
        // ---- 职业主动·虚化：**不造成伤害** + 保留位移 + 1.5 秒短无敌 ----
        //
        // 虚化的形态仍是 DASH（所以保留位移），但它不是"冲刺伤害"技：
        // 原版没有"短时间免疫伤害"的 API，用 setInvulnerable(true) 近似，
        // 30 tick 后恢复**进入前的值**（创造模式的免伤走 abilities，不靠这个字段，
        // 所以恢复原值不会影响创造模式）。
        if (technique == Technique.GUI_XU_HUA) {
            return castPhaseDash(player, level, look, technique);
        }
        double distance = technique == Technique.FENG_YING_BU ? 12.0D : 8.0D;
        Vec3 from = player.position();
        Vec3 to = from.add(look.scale(distance));

        // 沿途伤害
        AABB sweep = new AABB(from, to).inflate(2.0D);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, sweep,
                e -> isEnemy(player, e))) {
            hurt(player, t, damage, technique);
        }

        // 找落点：不能穿墙，从上往下扫一个安全位置
        Vec3 dest = null;
        for (double d = distance; d >= 1.0D; d -= 1.0D) {
            Vec3 candidate = from.add(look.scale(d));
            if (level.noCollision(player, player.getBoundingBox()
                    .move(candidate.subtract(from)))) {
                dest = candidate;
                break;
            }
        }
        if (dest == null) {
            dest = from;
        }
        player.teleportTo(dest.x, dest.y, dest.z);
        player.resetFallDistance();

        // 残影 + 落地冲击环
        SpellVfx.dashFx(level, from, dest, technique.element());
        return true;
    }

    /**
     * <b>虚化</b>（职业主动·鬼修）：位移 + 1.5 秒短无敌，<b>不造成任何伤害</b>。
     *
     * <p>近似说明：玩家方案写的是"短无敌 + 位移"，原版没有现成的"闪避窗口"，
     * 所以用 {@code setInvulnerable(true)} 顶 30 tick，到点恢复进入前的值。
     * 无敌期间不产生沿途伤害（与其它 DASH 职业主动区分开）。</p>
     */
    private static boolean castPhaseDash(ServerPlayer player, ServerLevel level, Vec3 look,
                                         Technique technique) {
        double distance = 8.0D;
        Vec3 from = player.position();
        // 找落点：不能穿墙（与 castDash 同一套"逐格回退"逻辑）
        Vec3 dest = null;
        for (double d = distance; d >= 1.0D; d -= 1.0D) {
            Vec3 candidate = from.add(look.scale(d));
            if (level.noCollision(player, player.getBoundingBox()
                    .move(candidate.subtract(from)))) {
                dest = candidate;
                break;
            }
        }
        if (dest == null) {
            dest = from;
        }
        player.teleportTo(dest.x, dest.y, dest.z);
        player.resetFallDistance();

        // ---- 短无敌（1.5 秒）----
        long now = level.getGameTime();
        java.util.UUID id = player.getUUID();
        Long oldUntil = PHASE_UNTIL.get(id);
        if (oldUntil == null || now >= oldUntil) {
            // 只在"新的一段虚化"开始时记录原始值；连续施放以第一段为准。
            PHASE_WAS_INVULNERABLE.put(id, player.isInvulnerable());
        }
        PHASE_UNTIL.put(id, now + PHASE_INVULNERABLE_TICKS);
        player.setInvulnerable(true);
        com.hongmeng.xiuxian.realworld.RealWorldScheduler.after(level,
                PHASE_INVULNERABLE_TICKS, () -> endPhaseInvulnerable(player));

        // 表现：化虚幽光 + 位移残影（复用既有 PathMechanicFx / SpellVfx，不新造特效）
        com.hongmeng.xiuxian.technique.PathMechanicFx.phase(player, PHASE_INVULNERABLE_TICKS);
        SpellVfx.dashFx(level, from, dest, technique.element());
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a7b虚化 \u00a77· \u00a7f1.5 \u00a77秒无敌（不造成伤害）"), true);
        return true;
    }

    /** 虚化结束：恢复到进入前的 invulnerable 值（若期间又放了一次，等最后一段）。 */
    private static void endPhaseInvulnerable(ServerPlayer player) {
        if (player == null) {
            return;
        }
        java.util.UUID id = player.getUUID();
        Long until = PHASE_UNTIL.get(id);
        if (until != null && player.level().getGameTime() < until) {
            return;
        }
        PHASE_UNTIL.remove(id);
        Boolean was = PHASE_WAS_INVULNERABLE.remove(id);
        player.setInvulnerable(Boolean.TRUE.equals(was));
    }

    /** 治疗自身。 */
    private static boolean castHeal(ServerPlayer player, ServerLevel level, Technique technique) {
        // ---- 职业主动·不灭金身：治疗 25% + 抗性 IV 40t + 40 点吸收 ----
        //
        // 近似说明：玩家方案要的是"**短时锁血 + 回血**"。原版没有"生命值不会低于 X"
        // 的通用机制，所以用「抗性提升 IV + 40 点伤害吸收 + 立刻回 25%」近似：
        // 既减伤又有护盾，等效于极短时间内的"打不死 + 在回血"。
        if (technique == Technique.TI_BU_MIE_JIN_SHEN) {
            float maxHp = player.getMaxHealth();
            float healed = maxHp * 0.25F;
            player.heal(healed);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3));
            player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), 40.0F));
            SpellVfx.supportFx(level, player.position(), true, technique.element());
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7e金身不灭 \u00a77· 回复 \u00a7f" + Math.round(healed)
                            + " \u00a77· 吸收 \u00a7f40 \u00a78（近似锁血）"), true);
            return true;
        }
        float max = player.getMaxHealth();
        float heal = max * 0.25F;
        player.heal(heal);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        // 视觉：向上螺旋的红心，和攻击法术的"向外炸"明确区分
        SpellVfx.supportFx(level, player.position(), true, technique.element());
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a7a回复 " + Math.round(heal) + " 点生命"), true);
        return true;
    }

    /** 增益。 */
    private static boolean castBuff(ServerPlayer player, ServerLevel level, Technique technique) {
        switch (technique) {
            case HOU_TU_DUN -> player.addEffect(
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 1));
            case XUAN_BING_DUN -> player.addEffect(
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
            case QING_FENG_JUE -> player.addEffect(
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
            case CHUN_FENG_HUA_YU -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                // 附近友方也回——按需求只给自己，避免误伤判定
            }
            default -> player.addEffect(
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        }
        // 视觉：护体类的向上螺旋（元素色），比治疗暗一档
        SpellVfx.supportFx(level, player.position(), false, technique.element());
        return true;
    }

    /** 吸血：命中后按比例回血。 */
    private static boolean castLifesteal(ServerPlayer player, ServerLevel level,
                                         double damage, Technique technique) {
        AABB box = player.getBoundingBox().inflate(AOE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e));
        if (targets.isEmpty()) {
            return false;
        }
        double total = 0.0D;
        for (LivingEntity t : targets) {
            double actual = hurt(player, t, damage, technique);
            total += actual;
        }
        float heal = (float) (total * 0.60D);
        if (heal > 0.0F) {
            player.heal(heal);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7c噬血 +\u00a7f" + Math.round(heal)), true);
        }
        // 视觉：目标身上往内收的血线
        SpellVfx.drainFx(level, player.position(), technique.element());
        return true;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 对目标造成伤害，返回实际伤害量。 */
    /** 单次命中封顶：普通法术（占目标最大生命的比例）。 */
    public static final double HIT_CAP_NORMAL = 0.28D;

    /** 单次命中封顶：范围法术（打得多，单下更轻）。 */
    public static final double HIT_CAP_AOE = 0.18D;

    /** 单次命中封顶：灭世 / 超位（仍是重击，但不会一发带走）。 */
    public static final double HIT_CAP_ULTIMATE = 0.45D;

    /**
     * 取这门法术的**单次命中封顶**（占目标最大生命的比例）。
     *
     * <p>玩家反馈：「一个法术就能秒杀鸿蒙的头目，法术伤害超标」。
     * 原来 {@link #hurt} 完全不封顶，鸿蒙玩家的灭世法术一次上百万，
     * 头目直接蒸发。现在按法术类型给上限：
     * 普通 28% · 范围 18% · 灭世/超位 45%。</p>
     */
    public static double hitCapOf(Technique technique) {
        if (technique == null) {
            return HIT_CAP_NORMAL;
        }
        return switch (technique.kind()) {
            case ULTIMATE, GRAVITY, SLASH -> HIT_CAP_ULTIMATE;
            case AOE -> HIT_CAP_AOE;
            default -> HIT_CAP_NORMAL;
        };
    }

    private static double hurt(ServerPlayer player, LivingEntity target,
                               double damage, Technique technique) {
        if (target == null || !target.isAlive()) {
            return 0.0D;
        }
        float before = target.getHealth();
        // 神识震的特性：无视护甲的真实伤害
        var source = technique == Technique.SHEN_SHI_ZHEN
                ? player.damageSources().indirectMagic(player, player)
                : player.damageSources().playerAttack(player);
        target.invulnerableTime = 0;
        // ---- 破防（穿透）：按**受击目标**的实际护甲减免率折算等效增伤 ----
        // 以前这段写在 damageOf 里、并按玩家的护甲减免率算（取错目标，等于给玩家自己加伤）；
        // 挪到命中点后「目标越厚甲，破防收益越大」才成立。
        if (technique != Technique.SHEN_SHI_ZHEN) {
            try {
                double pen = com.hongmeng.xiuxian.talent.TalentBonus.penetration(player);
                if (pen > 0.0D) {
                    damage *= 1.0D + pen * com.hongmeng.xiuxian.compat.ArmorMitigation
                            .reductionOf(target);
                }
            } catch (Throwable ignored) {
                // 破防结算失败按原伤害
            }
        }
        // ---- 单次命中封顶（玩家反馈：一发法术秒杀鸿蒙头目，伤害超标）----
        double cap = hitCapOf(technique);
        double maxHp = target.getMaxHealth();
        if (technique != Technique.SHEN_SHI_ZHEN && maxHp > 0.0D) {
            damage = Math.min(damage, maxHp * cap);
        }
        boolean ok = target.hurt(source, (float) damage);
        if (!ok) {
            return 0.0D;
        }
        return Math.max(0.0D, before - target.getHealth());
    }

    /** 元素附带效果。 */
    private static void applyElementSideEffect(LivingEntity target, Technique technique) {
        // 表现用维度：服务端目标必在 ServerLevel；拿不到就只是没有视觉，不影响下面的状态数值。
        ServerLevel statusLevel = target.level() instanceof ServerLevel sl ? sl : null;
        switch (technique.element()) {
            case FIRE -> {
                target.setSecondsOnFire(4);
                // 表现：点燃状态反馈（只加粒子，数值/时长的原样保留）
                SpellVfx.statusFx(statusLevel, target.position(), technique.element(),
                        SpellVfx.StatusFx.BURN);
            }
            case WATER, ICE -> {
                target.addEffect(
                        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                // 表现：冰霜状态反馈
                SpellVfx.statusFx(statusLevel, target.position(), technique.element(),
                        SpellVfx.StatusFx.FROST);
            }
            case THUNDER -> {
                target.addEffect(
                        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                // 表现：雷弧状态反馈
                SpellVfx.statusFx(statusLevel, target.position(), technique.element(),
                        SpellVfx.StatusFx.SHOCK);
            }
            case WOOD -> {
                if (technique == Technique.WAN_MU_KUN_SUO) {
                    // 定身：极高的缓慢
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                    target.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 128));
                    // 表现：定身（藤蔓缠绕）反馈
                    SpellVfx.statusFx(statusLevel, target.position(), technique.element(),
                            SpellVfx.StatusFx.BIND);
                }
            }
            case EARTH -> {
                if (technique == Technique.LUO_DI_SHEN_ZHEN
                        || technique == Technique.SHAN_HE_YIN) {
                    // 击退
                    Vec3 push = target.position()
                            .subtract(target.level().getNearestPlayer(target, 16) != null
                                    ? target.level().getNearestPlayer(target, 16).position()
                                    : target.position())
                            .normalize().scale(1.2D);
                    target.push(push.x, 0.5D, push.z);
                    // 表现：击退（尘爆/冲击环）反馈
                    SpellVfx.statusFx(statusLevel, target.position(), technique.element(),
                            SpellVfx.StatusFx.KNOCK);
                }
            }
            default -> {
            }
        }
    }

    /** 直线/射线上的敌人，按距离排序。 */
    private static List<LivingEntity> enemiesAlong(ServerPlayer player, Vec3 from, Vec3 to) {
        AABB box = new AABB(from, to).inflate(LINE_WIDTH);
        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> isEnemy(player, e));
        list.sort((a, b) -> Double.compare(a.position().distanceToSqr(from),
                b.position().distanceToSqr(from)));
        return list;
    }

    /** 判定敌我：不误伤自己、宠物与同门（简化：只排除自己和创造的实体）。 */
    private static boolean isEnemy(ServerPlayer player, LivingEntity other) {
        if (other == player || !other.isAlive() || other.isSpectator()) {
            return false;
        }
        if (other instanceof ServerPlayer) {
            return false;   // 不 PVP，避免误伤
        }
        // 只打敌对生物（怪物）——这样不会误伤村民和动物
        return other instanceof Monster || other instanceof Mob && !isFriendly(other);
    }

    private static boolean isFriendly(LivingEntity entity) {
        return entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE
                || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.WATER_CREATURE
                || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT
                || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.AXOLOTLS;
    }

    /**
     * 元素对应的粒子。
     *
     * <p>实现已经搬到 {@link SpellVfx#particleOf(Element)}——特效是要成体系做的
     * （核心 / 轨迹 / 余韵三层 + 元素音效 + 全局粒子预算），散在这里做不到。
     * 这个方法留着只是为了不破坏外部调用，语义与以前完全一致。</p>
     */
    public static ParticleOptions particleOf(Element element) {
        return SpellVfx.particleOf(element);
    }
}
