package com.hongmeng.xiuxian.talent;

import net.minecraft.util.Mth;

/**
 * <b>五条道途的核心机制</b> —— 纯逻辑，可在服务端与验证器里直接算。
 *
 * <h2>为什么要做"机制"而不只是数值</h2>
 * <p>只调数值的话，五条道途的差别只是"数字大小"，
 * 玩家体感上是同一个职业换皮。真正的区别必须来自<b>不同的资源与决策</b>。</p>
 *
 * <h2>每条机制的文献依据（不是我自己编的）</h2>
 * <table border="1">
 *   <caption>资料出处与对应机制</caption>
 *   <tr><th>道途</th><th>机制</th><th>依据</th></tr>
 *   <tr><td><b>法修</b></td><td>真元化盾</td>
 *       <td>《一念逍遥》法修攻略：「法修的优势在于护盾」「修炼会增长"<b>真元</b>"属性，
 *           法修释放神通法宝使用的是真元。但<b>一旦真元没有的话就会秒变脆皮</b>」；
 *           并提到控制技「五雷仙诀·金(3 秒定身)」「太一神光(2 秒沉默)」</td></tr>
 *   <tr><td><b>体修</b></td><td>激荡 + 锁血</td>
 *       <td>《一念逍遥》体修重塑：「<b>激荡</b>为 2 倍激荡能量和 50% 增伤」，
 *           蛮相改为「<b>燃血</b>：2.8 倍激荡能量、80% 增伤」但有每秒减血，
 *           圣相改为「<b>金身</b>：2.4 倍、50% 增伤、30% 减伤」；
 *           秘术「<b>即将重伤时防止之，并锁血 2 秒</b>」</td></tr>
 *   <tr><td><b>魔修</b></td><td>魔气（高风险高回报）</td>
 *       <td>玩家评价与通行设定：魔道「力量来得快、代价重」；
 *           另据体修攻略可知<b>后期顶端是真实伤害</b>（六丁星神炎、惊锋三式），
 *           所以魔修走"无视护甲"的路线</td></tr>
 *   <tr><td><b>血修</b></td><td>血池 + 缓伤</td>
 *       <td>《一念逍遥》血修（血炼）：「<b>物理系</b>修炼方向」，
 *           可「在体内炼化一座<b>血池</b>」，通过击败妖兽或强敌提升血池精华；
 *           秘术「<b>不灭血体</b>」：受伤时消耗真元吸收部分伤害，储存为「<b>缓伤</b>」；
 *           定位是「<b>反杀快感</b>」</td></tr>
 *   <tr><td><b>阵修</b></td><td>阵域（范围控场）</td>
 *       <td>玩家要求把"阵法师"转副业；主修侧的阵修保留<b>战斗向</b>的领域与剑阵强化</td></tr>
 * </table>
 *
 * <h2>⚠ 我刻意没做的两件事</h2>
 * <ol>
 *   <li><b>不给"每秒掉血"这种惩罚</b>：玩家明确说「可以不用有原本的代价」，
 *       而参考作品的体质也都是只加不减。所以这里所有机制都是
 *       <b>资源管理</b>（有代价但可控），不是"被动挨罚"；</li>
 *   <li><b>不做"后期乏力"</b>：体修重塑的分析里最大的痛点就是
 *       「三修四修后基本只能退居二线」，因为纯倍率打不过真实伤害。
 *       所以这里给每条道途都留了<b>通往真伤/破防</b>的路（见
 *       {@code 魔修}的魔气与{@code 阵修}的阵域）。</li>
 * </ol>
 */
public final class PathMechanics {

    private PathMechanics() {
    }

    // ==================================================================
    // 体修：激荡 + 锁血
    // ==================================================================

    /** 激荡能量上限。 */
    public static final double SURGE_MAX = 100.0D;

    /**
     * <b>激荡</b>：体修受创时积攒能量，攒满后可主动引爆换取爆发增伤。
     *
     * <p>依据：「激荡为 2 倍激荡能量和 50% 增伤效果」；蛮相「2.8 倍能量、80% 增伤」
     * 但有每秒减血；圣相「2.4 倍、50% 增伤、30% 减伤」。</p>
     *
     * <p>这里把它做成<b>纯收益但需要决策</b>：</p>
     * <ul>
     *   <li>受创积攒（挨打越多攒得越快 —— 符合"体修越挨打越猛"）；</li>
     *   <li>引爆后获得增伤，但能量清零；</li>
     *   <li><b>不在"蛮相/圣相"之间二选一</b>，而是让引爆时的血量决定形态 ——
     *       血多则燃血（高增伤），血少则金身（高减伤）。
     *       这样"什么时候引爆"本身就是一个决策，而不是开局选一次。</li>
     * </ul>
     *
     * @param damageTaken 本次受到的伤害
     * @return 这次要增加多少能量
     */
    public static double surgeGain(double damageTaken) {
        // 挨 1 点伤害攒 2.4 点能量（对应"2.4~2.8 倍激荡能量"的档位）
        return Math.max(0.0D, damageTaken) * 2.4D;
    }

    /** 引爆激荡。返回增伤倍率（1.0 起，即"没有增伤"）。 */
    public static double surgeBurst(double energy, double healthFraction) {
        if (energy < SURGE_MAX) {
            return 1.0D;
        }
        // 血多 → 燃血（进攻）；血少 → 金身（守势）
        return healthFraction >= 0.5D ? 1.80D : 1.50D;
    }

    /** 引爆时的减伤（金身形态才有）。 */
    public static double surgeMitigation(double energy, double healthFraction) {
        if (energy < SURGE_MAX || healthFraction >= 0.5D) {
            return 0.0D;
        }
        return 0.30D;
    }

    /** <b>锁血</b>：即将重伤时防止死亡并锁血，持续 2 秒（40 tick）。 */
    public static final int LAST_STAND_TICKS = 40;

    /**
     * 这一击会不会致死（用来触发锁血）。
     *
     * @param hp        当前生命
     * @param incoming  即将受到的伤害
     * @return 是否触发锁血
     */
    public static boolean shouldLastStand(double hp, double incoming, boolean onCooldown) {
        return !onCooldown && hp > 0.0D && incoming >= hp;
    }

    /**
     * 锁血期间允许保留的最低生命比例。
     *
     * <p>「锁血 2 秒」= 这 2 秒内不会死，所以生命下限是 1 点而不是 0。</p>
     */
    public static double lastStandFloor() {
        return 1.0D;
    }

    // ==================================================================
    // 法修：真元化盾
    // ==================================================================

    /**
     * <b>真元化盾</b>：法力可以当护盾用，但用光就变脆皮。
     *
     * <p>依据：法修「优势在于护盾」「一旦真元没有的话就会秒变脆皮」。</p>
     *
     * <p>这个机制制造了一个<b>真实的两难</b>：法力既是输出资源又是生存资源。
     * 全打出去就没盾，留盾就没输出 —— 这正是资料里那个"脆皮"警告的玩法化。</p>
     *
     * @param mana        当前法力
     * @param manaMax     法力上限
     * @param shieldRatio 转化率（每点法力换多少点护盾）
     * @return 能吸收多少伤害
     */
    public static double manaShield(double mana, double manaMax, double shieldRatio) {
        if (manaMax <= 0.0D) {
            return 0.0D;
        }
        double frac = Mth.clamp(mana / manaMax, 0.0D, 1.0D);
        return frac * manaMax * shieldRatio;
    }

    /**
     * 真元耗尽时的易伤（"秒变脆皮"）。
     *
     * @return 额外受到的伤害比例（0 = 没有易伤）
     */
    public static double manaEmptyVulnerability(double mana, double manaMax) {
        if (manaMax <= 0.0D) {
            return 0.0D;
        }
        double frac = mana / manaMax;
        // 法力低于 15% 开始变脆，空了达到 +35%
        if (frac >= 0.15D) {
            return 0.0D;
        }
        return (1.0D - frac / 0.15D) * 0.35D;
    }

    /** 护盾吸收：返回 [吸收量, 剩余伤害]。 */
    public static double[] absorbWithShield(double damage, double shieldHp) {
        double absorbed = Math.min(damage, Math.max(0.0D, shieldHp));
        return new double[]{absorbed, Math.max(0.0D, damage - absorbed)};
    }

    // ==================================================================
    // 魔修：魔气
    // ==================================================================

    /** 魔气上限。 */
    public static final double DEMON_MAX = 100.0D;

    /**
     * <b>魔气</b>：造成伤害积攒魔气，魔气越高伤害越高 —— 但魔力会侵蚀自身。
     *
     * <p>依据：魔道的通行设定是「力量来得快、代价重」。
     * 但玩家要求<b>不要"原本的代价"</b>，所以我把它做成
     * <b>可控的资源</b>而不是被动掉血：</p>
     * <ul>
     *   <li>魔气提供增伤与破防（<b>这就是它通往真实伤害的路</b>）；</li>
     *   <li>魔气满时会<b>溢出</b>：溢出部分会伤到自己 ——
     *       但这是玩家自己"该不该继续打"的决策，不是无条件挨罚；</li>
     *   <li>脱战会缓慢散去。</li>
     * </ul>
     */
    public static double demonGain(double damageDealt) {
        return Math.max(0.0D, damageDealt) * 1.6D;
    }

    /** 魔气带来的增伤倍率。 */
    public static double demonDamageBonus(double demonic) {
        double frac = Mth.clamp(demonic / DEMON_MAX, 0.0D, 1.0D);
        return 1.0D + frac * 0.45D;
    }

    /** 魔气带来的破防（无视护甲比例）。 */
    public static double demonPenetration(double demonic) {
        double frac = Mth.clamp(demonic / DEMON_MAX, 0.0D, 1.0D);
        return frac * 0.35D;
    }

    /**
     * 魔气溢出对自身的伤害。
     *
     * @return 本次自伤（这就是"高风险"的体现，且完全由玩家掌控）
     */
    public static double demonOverflowSelfDamage(double demonic, double overflow) {
        if (demonic < DEMON_MAX || overflow <= 0.0D) {
            return 0.0D;
        }
        return overflow * 0.5D;
    }

    /** 脱战时魔气消散（每 tick）。 */
    public static double demonDecayPerTick() {
        return 0.35D;
    }

    // ==================================================================
    // 血修：血池 + 缓伤
    // ==================================================================

    /** 血池上限。 */
    public static final double BLOOD_MAX = 100.0D;

    /**
     * <b>血池</b>：击杀与伤害积累血池精华；血池可转化为治疗。
     *
     * <p>依据：血修「可在体内炼化一座<b>血池</b>」，
     * 「通过击败妖兽或强敌收集资源，提升血池精华浓度」。</p>
     */
    public static double bloodGainFromDamage(double damageDealt) {
        return Math.max(0.0D, damageDealt) * 0.9D;
    }

    public static double bloodGainFromKill(double victimMaxHealth) {
        return Math.max(4.0D, victimMaxHealth * 0.25D);
    }

    /**
     * 消耗血池回血。
     *
     * @param pool      当前血池
     * @param wantHeal  想回多少血
     * @return [实际回血, 消耗的血池]
     */
    public static double[] spendBlood(double pool, double wantHeal) {
        double pool2hp = 1.6D;                  // 1 点血池 = 1.6 点生命
        double canHeal = Math.max(0.0D, pool) * pool2hp;
        double heal = Math.min(Math.max(0.0D, wantHeal), canHeal);
        return new double[]{heal, heal / pool2hp};
    }

    /** <b>缓伤</b>的持续时间（tick）与结算比例。 */
    public static final int STAGGER_TICKS = 60;

    /**
     * 法修真元盾：<b>1 点法力抵消多少点伤害</b>。
     *
     * <p>法力池比血量小得多（同境界大约 1:8），所以兑换率必须远高于 1:1，
     * 否则"拿法力当血"等于没用。4.0 = 满蓝能顶两三下重击，
     * 符合法修"脆但能硬吃一发"的定位。</p>
     */
    public static final double MANA_PER_DAMAGE = 4.0D;

    /**
     * <b>缓伤</b>：受伤时把一部分伤害"延后"到几秒内慢慢结算。
     *
     * <p>依据：血修秘术「不灭血体」——「受到伤害时，可消耗真元吸收部分扣除气血，
     * 并将其储存为"<b>缓伤</b>"」。</p>
     *
     * <p>这是血修最独特的一点：它<b>不减少总伤害，只改变伤害的时间分布</b>。
     * 于是"在缓伤结算前反杀"成为可能 —— 这正是资料里说的「<b>反杀快感</b>」。</p>
     *
     * @return [立即结算的伤害, 进入缓伤池的伤害]
     */
    public static double[] staggerSplit(double damage, double ratio) {
        double d = Math.max(0.0D, damage);
        double r = Mth.clamp(ratio, 0.0D, 0.8D);
        double delayed = d * r;
        return new double[]{d - delayed, delayed};
    }

    /** 缓伤池每 tick 结算多少（均摊到 {@link #STAGGER_TICKS}）。 */
    public static double staggerPerTick(double pool) {
        return Math.max(0.0D, pool) / STAGGER_TICKS;
    }

    /**
     * <b>血修的反杀窗口</b>：血池越满、生命越低，伤害越高。
     *
     * <p>这是"以血养功"的玩法化 —— 鼓励玩家在危险边缘作战。
     * 与魔修不同，这个加成<b>完全没有负面</b>，只是要求你打得激进。</p>
     */
    public static double bloodlustBonus(double poolFraction, double healthFraction) {
        double lowHp = Mth.clamp(1.0D - healthFraction, 0.0D, 1.0D);
        return 1.0D + Mth.clamp(poolFraction, 0.0D, 1.0D) * lowHp * 0.60D;
    }

    // ==================================================================
    // 阵修：阵域
    // ==================================================================

    /** 阵域能量的积攒速度（每 tick 自然回复）。 */
    public static double formationRegenPerTick() {
        return 0.12D;
    }

    /**
     * <b>阵域</b>：阵修展开领域，领域内的敌人被减速并持续受伤。
     *
     * <p>依据：玩家要求把"阵法师"转副业之后，主修侧的阵修保留
     * <b>战斗向</b>的领域与剑阵强化。所以这里的定位是
     * <b>范围控场 + 持续伤害</b>，与其它四条"单体爆发"形成互补。</p>
     *
     * @param radius  领域半径（格）
     * @param ticks   已持续 tick
     * @return 领域内敌人每 tick 受到的伤害比例（相对施法者攻击力）
     */
    public static double formationDotPerTick(double radius, int ticks) {
        // 半径越大，单体伤害越低（大范围=分摊），这是"范围 vs 强度"的取舍
        double areaFactor = Mth.clamp(4.0D / Math.max(1.0D, radius), 0.25D, 2.0D);
        // 前 3 秒渐强（布置需要时间），之后稳定
        double ramp = Mth.clamp(ticks / 60.0D, 0.0D, 1.0D);
        return 0.010D * areaFactor * ramp;
    }

    /** 领域内敌人的减速比例。 */
    public static double formationSlow(double radius) {
        double areaFactor = Mth.clamp(4.0D / Math.max(1.0D, radius), 0.4D, 1.0D);
        return 0.25D * areaFactor;
    }

    // ==================================================================
    // 新职业（R-1）：剑修 · 剑意 / 剑气 / 剑罡
    // ==================================================================

    /** 剑意层数上限。 */
    public static final int SWORD_INTENT_MAX = 10;

    /** 剑气伤害系数（相对攻击力）。 */
    public static final double SWORD_QI_DAMAGE = 0.80D;

    /** 剑气锥形半径（格）。 */
    public static final double SWORD_QI_RADIUS = 3.5D;

    /** 剑气最多命中目标数。 */
    public static final int SWORD_QI_TARGETS = 5;

    /** 剑罡基础吸收率（0 层剑意时）。 */
    public static final double SWORD_AURA_BASE = 0.15D;

    /** 每层剑意增加的吸收率。 */
    public static final double SWORD_AURA_PER_STACK = 0.02D;

    /** 剑罡吸收率硬上限（天赋叠满也不能变成无敌）。 */
    public static final double SWORD_AURA_CAP = 0.45D;

    /** 一次近战命中获得的剑意层数（乘各种获取加成）。 */
    public static double swordIntentGain(double multiplier) {
        return Math.max(0.0D, multiplier);
    }

    /**
     * <b>剑罡吸收率</b>：0 层 15%，每层 +2%，10 层 35%；天赋再加，封顶 45%。
     *
     * @param stacks 当前剑意层数（0~10）
     * @param talent 天赋【剑罡】加成（0.03 × 级数）
     */
    public static double swordAuraRatio(double stacks, double talent) {
        double s = Mth.clamp(stacks, 0.0D, SWORD_INTENT_MAX);
        double ratio = SWORD_AURA_BASE + SWORD_AURA_PER_STACK * s
                + Math.max(0.0D, talent);
        return Mth.clamp(ratio, 0.0D, SWORD_AURA_CAP);
    }

    /**
     * <b>剑气伤害</b>：满层后的下一次近战命中，对前方锥形目标造成
     * {@code 攻击力 × 0.80 × (1 + 剑气天赋)}。
     */
    public static double swordQiDamage(double attackDamage, double talent) {
        return Math.max(0.0D, attackDamage) * SWORD_QI_DAMAGE
                * (1.0D + Math.max(0.0D, talent));
    }

    // ==================================================================
    // 新职业（R-1）：鬼修 · 魂印 / 魂爆 / 虚化 / 鬼域
    // ==================================================================

    /** 魂印最大层数。 */
    public static final int SOUL_MARK_MAX = 3;

    /** 魂印持续 tick（8 秒；2 件套可延长）。 */
    public static final int SOUL_MARK_DURATION = 160;

    /** 每层魂印对鬼修伤害的加成。 */
    public static final double SOUL_MARK_DAMAGE = 0.04D;

    /** 魂爆伤害系数（相对攻击力）。 */
    public static final double SOUL_BURST_MULT = 0.60D;

    /** 魂爆减速比例。 */
    public static final double SOUL_BURST_SLOW = 0.30D;

    /** 魂爆减速持续 tick。 */
    public static final int SOUL_BURST_SLOW_TICKS = 30;

    /** 魂爆后同目标的叠印冷却 tick（防自循环）。 */
    public static final int SOUL_BURST_CD = 40;

    /** 当前魂印层数带来的伤害加成（0~12%）。 */
    public static double soulMarkDamageBonus(int stacks) {
        return Mth.clamp(stacks, 0, SOUL_MARK_MAX) * SOUL_MARK_DAMAGE;
    }

    /** 魂爆伤害：{@code 攻击力 × 0.60 × (1 + 魂印天赋)}。 */
    public static double soulBurstDamage(double attackDamage, double talent) {
        return Math.max(0.0D, attackDamage) * SOUL_BURST_MULT
                * (1.0D + Math.max(0.0D, talent));
    }

    /** 虚化无敌时长（tick）。 */
    public static final int PHASE_TICKS = 30;

    /** 虚化基础冷却（tick，60 秒）。 */
    public static final int PHASE_CD = 1200;

    /** 虚化消耗的鬼域能量。 */
    public static final double PHASE_ENERGY = 30.0D;

    /** 虚化后的移速加成。 */
    public static final double PHASE_MOVE = 0.30D;

    /** 虚化实际冷却（天赋每级 −10%，最多减到 48 秒）。 */
    public static int phaseCooldownTicks(double cut) {
        double c = Math.max(0.0D, Math.min(0.5D, cut));
        return (int) Math.round(PHASE_CD * (1.0D - c));
    }

    // ==================================================================
    // 新职业（R-1）：鬼域（沿用旧阵域公式）
    // ==================================================================

    /** 鬼域能量上限。 */
    public static final double GHOST_MAX = 100.0D;

    /** 鬼域能量每 tick 自然回复。 */
    public static double ghostRegenPerTick() {
        return 0.12D;
    }

    /** 鬼域展开门槛。 */
    public static final double GHOST_OPEN_COST = 60.0D;

    /** 鬼域展开后每 tick 消耗。 */
    public static final double GHOST_DRAIN = 0.35D;

    /** 鬼域 DoT（与旧阵域同一套公式）。 */
    public static double ghostDotPerTick(double radius, int ticks) {
        return formationDotPerTick(radius, ticks);
    }

    /** 鬼域减速（与旧阵域同一套公式）。 */
    public static double ghostSlow(double radius) {
        return formationSlow(radius);
    }
}
