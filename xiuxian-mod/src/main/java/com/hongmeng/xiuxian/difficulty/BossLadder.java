package com.hongmeng.xiuxian.difficulty;

/**
 * <b>天外天七首领 + 最终 Boss「恶」的强度阶梯</b>（纯数据表）。
 *
 * <h2>为什么要有这张表</h2>
 * <p>玩家原话：「天外天的 Boss 强度也要跟其他副本对上呈现阶梯式，你排列组合一下」。
 * 但在本表之前，Boss 的有效强度是<b>各自为政</b>的：</p>
 * <pre>
 *   有效值 = createAttributes 基础值 × 品阶倍率 × BossScaling(维度档位 × 品阶系数)
 * </pre>
 * <p>四个祭坛 Boss 走副本维度档位（2.2 / 4.0 / 6.5），而<b>下界、末地、天外天、真世界</b>
 * 在 {@code MobScaling.dimensionTierOf} 里一律落到「主世界 1.0」——
 * 于是终局 Boss「仙庭之主」（基础 70/10）只有
 * {@code 70×80×1.6 = 8,960} 血，比虚空裂隙的「虚空道主」（{@code 40×80×6.5×1.6 = 33,280}）
 * 还弱 3.7 倍；焚天炎帝更只有 2,250 血，比副本头目还低。
 * <b>这不是数值大小的问题，是六只 Boss 各自用了不同的尺子。</b></p>
 *
 * <h2>这张表做什么</h2>
 * <p>把「七首领 + 恶」按剧情/门槛排成一条<b>严格单调递增</b>的阶梯，并给出每一段
 * 的<b>目标有效值</b>（固定层口径：不含玩家境界缩放、不含 Boss 三阶段加成、不含灵根随机）。
 * {@link BossScaling} 对表中的 Boss 用「目标值 ÷ (基础值×品阶倍率)」做一次归一化，
 * 于是它们的最终固定层数值<b>精确等于</b>本表的目标值 —— 台阶就是看这张表。
 * 其它 BOSS / 首领品阶散怪（魔道统领、虚空霸主、末地虚空巨蟒…）不受影响，仍走
 * 「维度档位 × 品阶系数」的老公式。</p>
 *
 * <h2>阶梯顺序（为什么不是按现有数值大小排）</h2>
 * <p>顺序取「设计挑战境界 + 维度门槛」，而不是现有有效值大小（现有值是乱的）：</p>
 * <ol>
 *   <li><b>傀儡祖师</b>（纪元秘境，练气~筑基）—— 玩家第一个见到的首领；</li>
 *   <li><b>万兽之主</b>（妖兽秘境，金丹）；</li>
 *   <li><b>血魔老祖</b>（魔道巢穴，道印）；</li>
 *   <li><b>焚天炎帝</b>（下界世界 Boss，飞升~仙人）—— 下界先于末地，且是「要自己去找」的世界 Boss；</li>
 *   <li><b>虚空龙帝</b>（末地外岛世界 Boss，仙人）；</li>
 *   <li><b>虚空道主</b>（虚空裂隙终局副本，仙人门槛）—— <b>本线最强副本 Boss</b>，也是「虚空之眼」凭证的产地；</li>
 *   <li><b>仙庭之主</b>（天外天，仙人+）—— 07 总表定位「比虚空裂隙高一档」，且是「仙庭之冕」凭证产地；</li>
 *   <li><b>恶</b>（五凭证终局 Boss，帝境~鸿蒙）。</li>
 * </ol>
 * <p>刻意把虚空道主放在第 6 位（而不是第 4）：它和仙庭之主是仅有的两只「仙人门槛」Boss，
 * 放在相邻两阶既能表达「天外天比虚空裂隙高一档」，也避免为了塞进 1.3~1.7 的步长
 * 把前三只早期 Boss 抬成血牛。这个顺序与 {@code 工程/07-角色设计总表.md §2}
 * 的「基础境」列（头目 3/4/5、首领 6、世界 Boss「随玩家」、恶 10）一致。</p>
 *
 * <h2>步长</h2>
 * <p>HP 与 ATK 都严格递增，相邻两段的倍率都落在 <b>1.30 ~ 1.70</b>（见
 * {@link Step#hpStep()} / {@link Step#atkStep()}）：
 * 「不要跳崖、也不要一段只强一点点」是玩家的明确要求。
 * 其中前五段贴近 1.7（虚空道主的 33,280 是玩家已经认可的最强副本 Boss，保持不动，
 * 前五段只能把步长用满），末两段是 1.62 / 1.61（仙庭之主与恶回到旧实机绝对值，见工程/19 §8 风险 2）。</p>
 *
 * <h2>维护约定</h2>
 * <ul>
 *   <li>改本表<b>必须</b>把 {@link #VERSION} +1 ——
 *       {@code BossScaling} 用 persistentData 记版本号，老存档里已经刷出来的 Boss
 *       会按新表再归一化一次；不 +1 的话老 Boss 会停在旧数值上；</li>
 *   <li>离线校对脚本：{@code tools/check_boss_ladder.py}（从源码常量重新算一遍，
 *       断言单调、步长、区间，并输出阶梯表）。</li>
 * </ul>
 *
 * <p><b>本类不引用任何 Minecraft 类型</b>：它要能被离线脚本/普通 JUnit 直接读取，
 * 也方便以后把阶梯搬到配置表。最终数值怎么落到实体上，见 {@link BossScaling#applyLadder}。</p>
 */
public final class BossLadder {

    /**
     * 阶梯版本。
     *
     * <p>每次改动 {@link Step} 里的目标值都必须 +1：{@code BossScaling} 会在
     * persistentData 里写这个数，读到旧版本的 Boss 会清掉旧加成、按新表重算一次。
     * 这样玩家存档里已经召唤过的 Boss 也能跟上新阶梯，不需要等它重新刷。</p>
     *
     * <p>当前值 2：首版（1）从未构建进 jar（Lead 统一构建前就按最终数值落盘了），
     * 这里仍是"未发布，仅保险"——万一有人在本地用旧 jar 起过存档，版本号不同也会重算一次。</p>
     */
    public static final int VERSION = 2;

    // ------------------------------------------------------------------
    // 维度档位（BossScaling 的「自然倍率」口径；MobScaling 的路径路由表）
    // ------------------------------------------------------------------

    /** 主世界 / 真世界。 */
    public static final double TIER_OVERWORLD = 1.0D;
    /** 纪元秘境 / 妖兽秘境。 */
    public static final double TIER_LOW = 2.2D;
    /** 魔道巢穴。 */
    public static final double TIER_MID = 4.0D;
    /** 下界（世界 Boss：焚天炎帝）。新增：原来落到 1.0，世界 Boss 比副本头目还弱。 */
    public static final double TIER_NETHER = 5.0D;
    /** 末地（世界 Boss：虚空龙帝）。新增：略高于下界、略低于虚空裂隙（后者是终局副本）。 */
    public static final double TIER_END = 6.0D;
    /** 虚空裂隙（终局副本）。 */
    public static final double TIER_HIGH = 6.5D;
    /** 天外天。新增：07 总表定位「比虚空裂隙高一档」。 */
    public static final double TIER_HEAVEN = 8.0D;

    /**
     * 首领品阶在 BossScaling 里的额外系数（与 {@link BossScaling#multiplierOf} 同源）。
     *
     * <p>放在这里是因为「自然值」的推导要用它（{@code 基础×品阶×维度×1.6}），
     * 阶梯表的离线校对脚本也按这个口径复算。改它等于改所有首领的自然倍率，
     * 不是只改阶梯 Boss —— 谨慎。</p>
     */
    public static final double OVERLORD_RANK_SCALE = 1.6D;

    // ------------------------------------------------------------------
    // 阶梯
    // ------------------------------------------------------------------

    /**
     * 阶梯上的一级。
     *
     * <p>字段全部是「设计口径」，不含运行时状态：</p>
     * <ul>
     *   <li>{@code order}：1 基阶位（便于日志/文档引用）；</li>
     *   <li>{@code id}：实体注册名（short id，与 {@code ModEntities} 一致）；</li>
     *   <li>{@code cn}：中文名（给日志/文档用，不参与战斗）；</li>
     *   <li>{@code rank}：品阶名（BOSS / OVERLORD），仅用于文档与离线校对；</li>
     *   <li>{@code designTier}：设计挑战境界（大境 1~10），用于「预期 TTK」与图鉴对账；</li>
     *   <li>{@code dimensionPath} / {@code dimensionTier}：所在维度与它对应的
     *       {@link BossScaling} 维度档位。BossScaling 对阶梯 Boss 用目标值归一化，
     *       所以这个档位只用于「自然值」推导与文档；对<b>非阶梯</b> BOSS 级散怪它才是生效倍率；</li>
     *   <li>{@code targetHp} / {@code targetAtk}：<b>固定层目标有效值</b>
     *       （= 玩家境界缩放、阶段加成、灵根随机<b>之前</b>的最终值）。</li>
     * </ul>
     */
    public enum Step {

        /** 1. 傀儡祖师：纪元秘境（练气~筑基）。 */
        PUPPET(1, "epoch_boss", "傀儡祖师", "BOSS", 2,
                "epoch_realm", TIER_LOW, 2_400.0D, 17.7D),

        /** 2. 万兽之主：妖兽秘境（金丹）。 */
        BEAST_LORD(2, "beast_boss", "万兽之主", "BOSS", 3,
                "beast_realm", TIER_LOW, 4_000.0D, 24.8D),

        /** 3. 血魔老祖：魔道巢穴（道印门槛）。 */
        BLOOD_DEMON(3, "demon_boss", "血魔老祖", "BOSS", 6,
                "demon_realm", TIER_MID, 6_800.0D, 34.8D),

        /** 4. 焚天炎帝：下界世界 Boss（飞升~仙人；要自己去找）。 */
        BLAZE_EMPEROR(4, "blaze_emperor", "焚天炎帝", "BOSS", 7,
                "the_nether", TIER_NETHER, 11_550.0D, 48.7D),

        /** 5. 虚空龙帝：末地外岛世界 Boss（仙人）。 */
        VOID_DRAGON(5, "void_dragon_emperor", "虚空龙帝", "OVERLORD", 8,
                "the_end", TIER_END, 19_600.0D, 68.2D),

        /**
         * 6. 虚空道主：虚空裂隙终局副本（仙人门槛）。
         *
         * <p>HP 保持 33,280 不变 —— 它是玩家认可的「最强副本 Boss」，
         * 也是五凭证里「虚空之眼」的产地。ATK 从 702 做归一化（见 §ATK 说明），
         * 702 原本是「恶」187.2 的 3.75 倍，属于维度倍率叠出来的异常值。</p>
         */
        VOID_LORD(6, "void_boss", "虚空道主", "OVERLORD", 8,
                "void_realm", TIER_HIGH, 33_280.0D, 95.5D),

        /**
         * 7. 仙庭之主：天外天（仙人+，07 总表「比虚空裂隙高一档」）。
         *
         * <p>HP 54,000 ≈ 旧实机的期望值 53,947（= 旧固定层 8,960 × 灵根期望 1.62）——
         * 保留"终局不被悄悄削弱"的绝对手感；与虚空道主（33,280）的步长 1.623 仍在 1.3~1.7 内。
         * 详见 {@code 工程/19-Boss强度阶梯表.md §8 风险 2}。</p>
         */
        HEAVEN_LORD(7, "heaven_lord", "仙庭之主", "OVERLORD", 8,
                "heaven_realm", TIER_HEAVEN, 54_000.0D, 133.7D),

        /**
         * 8. 恶：五凭证终局 Boss（帝境~鸿蒙）。
         *
         * <p>HP 87,000 ≈ 旧实机的期望值 87,145（= 旧固定层 53,760 × 灵根期望 1.62）；
         * ATK 187.2 完全不变（它是终局基准，见 {@code 工程/19 §4}）。</p>
         */
        EVIL(8, "evil_gazer", "恶", "OVERLORD", 10,
                "real_world", TIER_OVERWORLD, 87_000.0D, 187.2D);

        private final int order;
        private final String id;
        private final String cn;
        private final String rank;
        private final int designTier;
        private final String dimensionPath;
        private final double dimensionTier;
        private final double targetHp;
        private final double targetAtk;

        Step(int order, String id, String cn, String rank, int designTier,
             String dimensionPath, double dimensionTier,
             double targetHp, double targetAtk) {
            this.order = order;
            this.id = id;
            this.cn = cn;
            this.rank = rank;
            this.designTier = designTier;
            this.dimensionPath = dimensionPath;
            this.dimensionTier = dimensionTier;
            this.targetHp = targetHp;
            this.targetAtk = targetAtk;
        }

        /** 1 基阶位（越靠前 = 玩家越早遇到）。 */
        public int order() {
            return this.order;
        }

        /** 实体注册名（short id，例如 {@code epoch_boss}）。 */
        public String id() {
            return this.id;
        }

        /** 中文名。 */
        public String cn() {
            return this.cn;
        }

        /** 品阶名（BOSS / OVERLORD；仅文档与离线校对用）。 */
        public String rank() {
            return this.rank;
        }

        /** 设计挑战境界（大境 1~10，练气=1、鸿蒙=10）。 */
        public int designTier() {
            return this.designTier;
        }

        /** 所在维度 path（例如 {@code heaven_realm}）。 */
        public String dimensionPath() {
            return this.dimensionPath;
        }

        /** 该维度对应的自然档位（见 {@link BossLadder#tierOfDimension}）。 */
        public double dimensionTier() {
            return this.dimensionTier;
        }

        /** 固定层目标生命（不含玩家境界缩放 / 阶段 / 灵根）。 */
        public double targetHp() {
            return this.targetHp;
        }

        /** 固定层目标攻击（不含玩家境界缩放 / 阶段 / 灵根）。 */
        public double targetAtk() {
            return this.targetAtk;
        }

        /**
         * HP 相对上一阶的倍率（第一阶返回 1.0）。
         *
         * <p>阶梯的「不跳崖」就是靠它检查的：每段都应落在 1.30~1.70。</p>
         */
        public double hpStep() {
            int i = ordinal();
            if (i <= 0) {
                return 1.0D;
            }
            return this.targetHp / Step.values()[i - 1].targetHp;
        }

        /** ATK 相对上一阶的倍率（第一阶返回 1.0）。 */
        public double atkStep() {
            int i = ordinal();
            if (i <= 0) {
                return 1.0D;
            }
            return this.targetAtk / Step.values()[i - 1].targetAtk;
        }
    }

    /** 缓存的 {@link Step#values()}（避免每次查表都克隆数组）。 */
    private static final Step[] STEPS = Step.values();

    private BossLadder() {
    }

    /** 全部台阶（按阶位升序 = 由弱到强）。 */
    public static Step[] all() {
        return STEPS.clone();
    }

    /** 台阶总数（= 8：七首领 + 恶）。 */
    public static int size() {
        return STEPS.length;
    }

    /**
     * 按实体注册名查台阶。
     *
     * @param entityId 实体注册名 short id（例如 {@code void_boss}）
     * @return 对应的台阶；不是阶梯 Boss 时返回 {@code null}
     */
    public static Step byId(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return null;
        }
        for (Step s : STEPS) {
            if (s.id.equals(entityId)) {
                return s;
            }
        }
        return null;
    }

    /** 这只 Boss 是否在强度阶梯上（是则 BossScaling 不再叠旧维度倍率）。 */
    public static boolean isLadderBoss(String entityId) {
        return byId(entityId) != null;
    }

    /**
     * 维度 path → 自然档位（{@link BossScaling} 的固定倍率口径）。
     *
     * <p>路由器只认 path（不看资源位置），和改动前的 {@code MobScaling.dimensionTierOf}
     * 完全同构，只是补了三个维度：下界 5.0、末地 6.0、天外天 8.0。
     * 顺序上先判新维度，再判旧的 void/demon/epoch|beast：</p>
     * <pre>
     *   heaven_*        → 8.0（天外天，比虚空裂隙高一档）
     *   the_nether      → 5.0（下界世界 Boss）
     *   the_end         → 6.0（末地世界 Boss）
     *   *void*          → 6.5（虚空裂隙，终局副本）
     *   *demon*         → 4.0（魔道巢穴）
     *   *epoch*|*beast* → 2.2（纪元 / 妖兽秘境）
     *   其余（主世界、真世界） → 1.0
     * </pre>
     *
     * @param dimensionPath 维度注册名 path（{@code level.dimension().location().getPath()}）
     * @return 自然档位；null/空路径按主世界处理
     */
    public static double tierOfDimension(String dimensionPath) {
        if (dimensionPath == null || dimensionPath.isEmpty()) {
            return TIER_OVERWORLD;
        }
        if (dimensionPath.contains("heaven")) {
            return TIER_HEAVEN;
        }
        if (dimensionPath.contains("nether")) {
            return TIER_NETHER;
        }
        if (dimensionPath.contains("the_end") || dimensionPath.equals("end")) {
            return TIER_END;
        }
        if (dimensionPath.contains("void")) {
            return TIER_HIGH;
        }
        if (dimensionPath.contains("demon")) {
            return TIER_MID;
        }
        if (dimensionPath.contains("epoch") || dimensionPath.contains("beast")) {
            return TIER_LOW;
        }
        return TIER_OVERWORLD;
    }
}
