package com.hongmeng.xiuxian.talent;

import net.minecraft.server.level.ServerPlayer;

/**
 * <b>30 条职业被动（{@link CareerSkill}）的纯计算读取口</b>。
 *
 * <h2>为什么单独一个类</h2>
 * <p>30 条被动分三种生效方式：</p>
 * <ol>
 *   <li><b>属性型</b>（移速 / 生命上限 / 抗击退 / 护甲）→ 由
 *       {@link TalentAttributes#refresh(ServerPlayer)} 挂成属性修饰符；</li>
 *   <li><b>事件型</b>（吸血 / 增伤 / 减速 / 击杀回资源…）→ 由
 *       {@link PathRuntime} 或 {@code ModEvents} 的既有结算口当场乘；</li>
 *   <li><b>核心机制型</b>（{@code CORE_MECHANIC} 11 条）→ 已经由
 *       {@link PathRuntime} 的资源/机制实现，本类<b>不重复计算</b>。</li>
 * </ol>
 *
 * <p>接线方需要的是"这个玩家身上某类加成的<b>合计值</b>"，而不是逐个
 * {@code CareerSkill} 去问 —— 所以集中到这里，避免各结算口各写一套循环、
 * 迟早漏一条或者重复叠加。</p>
 *
 * <h2>⚠ 口径：未解锁 = 0</h2>
 * <p>所有方法都返回<b>加法比例</b>（{@code 0.10 = +10%}）或点数
 * （{@code DEMON_KILL_GAIN / SOUL_KILL_GAIN}），<b>未解锁 / 没选职业 /
 * 传 null 一律返回 {@code 0.0}</b>。因此接线方可以无条件调用：
 * 无技能玩家的行为与接线前完全一致（乘 1 / 加 0）。</p>
 *
 * <p>解锁判定统一走 {@link Talents#skillUnlocked(ServerPlayer, CareerSkill)}：
 * 核心被动自带、分支被动按该分支累计投入 2 / 4 点解锁 —— 洗点后自动回到 0。</p>
 *
 * <h2>数值来源</h2>
 * <p>全部从 {@link CareerSkill#value()} 读，<b>本类不写死任何数字</b>
 * （交接表 {@code 工程/07-法术内容交接表.md} §1 的数值就是枚举里的数值），
 * 这样以后调平衡只改 {@code CareerSkill} 一处。</p>
 */
public final class CareerPassives {

    private CareerPassives() {
    }

    // ------------------------------------------------------------------
    // 通用入口
    // ------------------------------------------------------------------

    /**
     * 该玩家<b>已解锁</b>的、指定 {@link CareerSkill.EffectKind} 的所有被动数值之和。
     *
     * @param player 服务端玩家；{@code null} 返回 0
     * @param kind   效果种类；{@code null} 或 {@code CORE_MECHANIC} 返回 0
     * @return 加法比例或点数；未解锁为 0
     */
    public static double bonus(ServerPlayer player, CareerSkill.EffectKind kind) {
        if (player == null || kind == null
                || kind == CareerSkill.EffectKind.CORE_MECHANIC) {
            return 0.0D;
        }
        Career career = Talents.pathOf(player);
        if (career == null) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (CareerSkill skill : CareerSkill.values()) {
            // 先按职业过滤：没选这个职业时，其余 4 个职业的 24 条根本不可能解锁
            if (skill.career() != career || skill.kind() != kind) {
                continue;
            }
            if (Talents.skillUnlocked(player, skill)) {
                sum += skill.value();
            }
        }
        return sum;
    }

    /** 该玩家是否已解锁某一类被动（等价于 {@code bonus(...) > 0}，读起来更清楚）。 */
    public static boolean has(ServerPlayer player, CareerSkill.EffectKind kind) {
        return bonus(player, kind) > 0.0D;
    }

    /** 该玩家已解锁的职业被动总条数（含核心；给命令/调试/探针用）。 */
    public static int unlockedCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        Career career = Talents.pathOf(player);
        if (career == null) {
            return 0;
        }
        int n = 0;
        for (CareerSkill skill : CareerSkill.values()) {
            if (skill.career() == career && Talents.skillUnlocked(player, skill)) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // 剑修
    // ------------------------------------------------------------------

    /** 剑心通明 · 剑意获取 +10%（叠在职业专属的 +10% 之上）。 */
    public static double swordIntent(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.SWORD_INTENT);
    }

    /**
     * 剑势 · 逆势凝意。
     *
     * <p>玩家侧格挡系统已整体移除，本被动改成：<b>受击时凝聚 1 层剑意</b>（5 秒冷却，
     * 见 {@code PathRuntime.onPlayerHurt}）—— 越被压制越能反打，和"势"这个名字一致。
     * 返回 {@code true} 表示已解锁。</p>
     */
    public static boolean swordMomentum(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.SWORD_MOMENTUM) > 0.0D;
    }

    /** 锋芒 · 近战护甲穿透 +10%（叠在职业专属 8% / 天赋之上）。 */
    public static double penetration(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.PENETRATION);
    }

    /** 御剑 · 移速 +10%（属性型，挂进 {@link TalentAttributes}）。 */
    public static double moveSpeed(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.MOVE_SPEED);
    }

    // ------------------------------------------------------------------
    // 血修
    // ------------------------------------------------------------------

    /** 嗜血 · 近战吸血 +5%（叠在职业专属 15% / 天赋之上）。 */
    public static double lifesteal(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.LIFESTEAL);
    }

    /** 血怒 · 生命 &lt;40% 时增伤 +15%（事件型，伤害乘区）。 */
    public static double lowHpDamage(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.LOW_HP_DAMAGE);
    }

    /** 血痕 · 移动留痕的区域减速 10%（tick 型，见 {@link PathRuntime#tick}）。 */
    public static double auraSlow(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.AURA_SLOW);
    }

    /** 血偿 · 受击回补 3% 生命（{@link PathRuntime#onPlayerHurt}）。 */
    public static double healOnHurt(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.HEAL_ON_HURT);
    }

    // ------------------------------------------------------------------
    // 魔修
    // ------------------------------------------------------------------

    /** 魔溢 · 魔气满仓时爆发增伤 +20%（伤害乘区，自伤仍保留）。 */
    public static double demonOverflowBurst(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.DEMON_OVERFLOW_BURST);
    }

    /** 噬魂 · 击杀回魔气（点数，不是比例）。 */
    public static double demonKillGain(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.DEMON_KILL_GAIN);
    }

    /**
     * 心魔 · 负面抗性 +20%。
     *
     * <p>接在 Forge 的 {@code MobEffectEvent.Applicable}（负面效果即将挂上前的
     * 唯一统一判定口）：按该比例概率拒绝。</p>
     */
    public static double controlResist(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.CONTROL_RESIST);
    }

    /** 魔威 · 命中附带"削弱"10%（以 WEAKNESS 近似，见 {@link PathRuntime}）。 */
    public static double enemyWeaken(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.ENEMY_WEAKEN);
    }

    // ------------------------------------------------------------------
    // 鬼修
    // ------------------------------------------------------------------

    /** 锁魂 · 击杀回魂印/鬼域能量（点数，不是比例）。 */
    public static double soulKillGain(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.SOUL_KILL_GAIN);
    }

    /** 阴气 · 攻击附带减速 10%（以 MOVEMENT_SLOWDOWN 近似，40 tick）。 */
    public static double slowOnHit(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.SLOW_ON_HIT);
    }

    /** 魂爆 · 魂印引爆时额外产生 30% 范围伤害。 */
    public static double soulBurstAoe(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.SOUL_BURST_AOE);
    }

    // ------------------------------------------------------------------
    // 体修
    // ------------------------------------------------------------------

    /** 铁骨 · 抗击退 +25%（叠在职业专属 50% 之上，总量仍封顶 85%）。 */
    public static double knockbackResist(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.KNOCKBACK_RESIST);
    }

    /** 气血 · 生命上限 +10%（属性型）。 */
    public static double health(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.HEALTH);
    }

    /** 震荡 · 受伤反弹 10%（叠在天赋反弹之上）。 */
    public static double reflect(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.REFLECT);
    }

    /** 金刚 · 护甲/韧性 +8%（百分比转点数，见 {@link TalentAttributes}）。 */
    public static double armor(ServerPlayer player) {
        return bonus(player, CareerSkill.EffectKind.ARMOR);
    }
}
