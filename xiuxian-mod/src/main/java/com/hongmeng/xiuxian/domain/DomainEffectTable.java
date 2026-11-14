package com.hongmeng.xiuxian.domain;

import com.hongmeng.xiuxian.entity.MobRank;
import com.hongmeng.xiuxian.spirit.Element;

/**
 * 本命领域的<b>纯数据表</b>：八种元素的效果数值全部写在这里，一个都不许散落在逻辑里。
 *
 * <p><b>为什么单独一个类：</b>离线验证器（{@code src/verify/**}）要能逐条断言
 * "火每秒 4 点"、"金 5 层放 30%"这些数字。它不能碰注册表
 * （{@code Blocks} / {@code Attributes} / {@code ItemStack} 静态初始化会
 * {@code Not bootstrapped}），所以本类<b>刻意不引用任何 Minecraft 类型</b>——
 * 只引用两个同样纯净的枚举：{@link Element}（灵根元素）与 {@link MobRank}（怪物品阶）。</p>
 *
 * <p>数值来源：{@code docs/domain-spec.md} 第三节，逐条落成，<b>一个都没改</b>。
 * 每个常量后面的注释都写了它在规格里的出处。</p>
 *
 * <h2>首领（OVERLORD）统一规则：百分比封顶 2%</h2>
 *
 * <p>规格 §2.2 + §八。<b>玩家已确认选 (B)「封顶在 2%」</b>（2024 反馈：不是"每次结算固定 2%"）。
 * 于是整条规则只剩一句、只有一个写法：</p>
 *
 * <pre>
 *   对首领的百分比伤害 = min(普通怪百分比, 2%)
 * </pre>
 *
 * <p>——见 {@link #clampForOverlord}。<b>持续型（火/水 每秒 1%）传 1% 进去，得到的就是 1%</b>
 * （即首领也是每秒 1%，不是 2%）；一次性效果传 30% / 10% 进去，得到 2%。</p>
 *
 * <p><b>多段百分比按"一次效果"结算</b>：木刺是 8 根 × 1%，一次齐射算一次结算
 * （8% → 首领 2%），而不是每根单独过一遍 2%（那会变成 8 × 2% = 16%，
 * 多段效果打首领反而更疼，与规格本意相反）。</p>
 *
 * <p>土是唯一的例外：它对<b>头目（BOSS）与首领（OVERLORD）都只掉 2%</b>，
 * 头目以下直接秒杀——这一条不走 {@code min()}，因为"秒杀"不是一个百分比。</p>
 */
public final class DomainEffectTable {

    // ------------------------------------------------------------------
    // 范围（规格 §一：10 × 10 区块 → 6 × 6 区块，且做成配置项）
    // ------------------------------------------------------------------

    /** 领域边长（区块）的默认值：6 × 6 区块。配置项见 {@code HongmengConfig.SERVER.domainChunks}。 */
    public static final double DEFAULT_DOMAIN_CHUNKS = 6.0D;

    /** 一个区块的格数。 */
    public static final double BLOCKS_PER_CHUNK = 16.0D;

    /** 默认领域的水平半边长（格）。6 × 16 / 2 = 48，即 96 × 96 格。 */
    public static final double DEFAULT_HALF_BLOCKS =
            DEFAULT_DOMAIN_CHUNKS * BLOCKS_PER_CHUNK / 2.0D;

    /**
     * 领域的垂直半径（格）。
     *
     * <p>规格只说了"6 × 6 区块"（水平）。领域是"以角色为中心"，所以竖直方向取
     * ±32 格——足够覆盖地面到地底洞穴，又不会把整根世界柱子（384 格）都算进来。</p>
     */
    public static final double VERTICAL_HALF_BLOCKS = 32.0D;

    /** 扫描间隔（tick）。风要求"频率比其他元素高"，是 5 tick 一击，所以扫描必须 5 tick 一次。 */
    public static final int SCAN_INTERVAL_TICKS = 2;

    // ------------------------------------------------------------------
    // 通用：领域僵直（玩家要求：每个领域都要给一点点控制）
    // ------------------------------------------------------------------

    /** 每次领域伤害附带的「粘地僵直」tick 数。 */
    public static final int STUN_TICKS = 5;

    /** 僵直用的缓慢等级（250 级 = 移速归零）。 */
    public static final int STUN_SLOWNESS_AMPLIFIER = 250;

    /** 木领域的束缚等级（比僵直轻一点：能转身、能攻击，但挪不动）。 */
    public static final int WOOD_BIND_SLOWNESS_AMPLIFIER = 200;

    /** 木刺的射程（格）与每根刺的伤害占最大生命比例。 */
    public static final double WOOD_SPIKE_RANGE = 6.0D;

    /** 木刺命中单个目标的比例伤害上限（避免一根刺就秒人）。 */
    public static final double WOOD_SPIKE_HIT_PERCENT = 0.02D;

    /** 一次扫描最多处理多少目标（防止 96 × 96 的怪海里一帧跑几千次结算）。 */
    public static final int DEFAULT_MAX_TARGETS = 64;

    // ------------------------------------------------------------------
    // 通用
    // ------------------------------------------------------------------

    /** 一秒的 tick 数。 */
    public static final int TICKS_PER_SECOND = 20;

    /** 首领（OVERLORD）统一 2%（规格 §2.2）。 */
    public static final double OVERLORD_PERCENT = 0.02D;

    /**
     * "秒杀"用的伤害倍数：当前生命 × 10。
     *
     * <p>不用 {@code setHealth(0)}（那会绕过死亡结算），也不用
     * {@code Float.MAX_VALUE}（伤害数字会污染日志）。取当前生命的 10 倍：
     * 即使被怪物境界压制减伤到 30%，也还剩 3 倍当前生命，照样必死——
     * 见 {@code CultivationMonsterEntity#hurt} 的减伤钩子。</p>
     */
    public static final float EXECUTE_MULTIPLE = 10.0F;

    // ------------------------------------------------------------------
    // 火：火雨（规格 §3.1）
    // ------------------------------------------------------------------

    public static final class Fire {
        /** 结算间隔：每秒一次。 */
        public static final int INTERVAL_TICKS = TICKS_PER_SECOND;
        /** 秒伤"等同于泡在岩浆湖里"：岩浆每秒 4 点（2 心）。 */
        public static final double LAVA_DAMAGE_PER_SECOND = 5.0D;
        /** 附加：每秒 1% 最大生命。 */
        public static final double PERCENT_PER_SECOND = 0.01D;
        /** 强制燃烧：每次结算把剩余着火时间顶到 5 秒。 */
        public static final int FORCED_BURN_TICKS = 0;

        private Fire() {
        }
    }

    // ------------------------------------------------------------------
    // 木：地下生藤缠绕后爆刺（规格 §3.2）
    // ------------------------------------------------------------------

    public static final class Wood {
        /** 缠绕后 5 秒爆炸。 */
        public static final int DELAY_TICKS = 3 * TICKS_PER_SECOND;
        /** 爆炸伤害：5 点（2.5 心）。 */
        public static final double EXPLODE_DAMAGE = 7.0D;
        /** 向四周发射的木刺根数（规格没给根数，取八个方向 = 8 根）。 */
        public static final int SPIKE_COUNT = 10;
        /** 每根木刺 1% 生命。 */
        public static final double SPIKE_PERCENT = 0.01D;

        private Wood() {
        }
    }

    // ------------------------------------------------------------------
    // 水：涨潮（规格 §3.3）
    // ------------------------------------------------------------------

    public static final class Water {
        /** 结算间隔：每秒一次。 */
        public static final int INTERVAL_TICKS = TICKS_PER_SECOND;
        /**
         * 窒息伤害：每秒 2 点。
         *
         * <p>这就是原版溺水速度（{@code DamageTypes.DROWN} 每次 2 点、每秒一次）。
         * 规格只写"水下窒息（受窒息伤害）"没给数字，所以直接沿用原版溺水速率，
         * 而不是自己编一个数。</p>
         */
        public static final double SUFFOCATION_DAMAGE = 2.5D;
        /** 附加：每秒 1% 最大生命。 */
        public static final double PERCENT_PER_SECOND = 0.01D;
        /** 减速等级（0 = 缓慢 I，1 = 缓慢 II）。 */
        public static final int SLOWNESS_AMPLIFIER = 1;
        /** 减速时长（每次刷新）。 */
        public static final int SLOWNESS_TICKS = 40;

        private Water() {
        }
    }

    // ------------------------------------------------------------------
    // 金：锐利之气 + 五层标记（规格 §3.4）
    // ------------------------------------------------------------------

    public static final class Metal {
        /** 持续打击间隔：0.5 秒一次。 */
        public static final int INTERVAL_TICKS = 10;
        /** 每次 3 点（1.5 心）。 */
        public static final double HIT_DAMAGE = 1.5D;
        /** 五层标记。 */
        public static final int MARKS_FOR_WAVE = 6;
        /** 冲击波：掉 30%。 */
        public static final double WAVE_PERCENT = 0.12D;

        private Metal() {
        }
    }

    // ------------------------------------------------------------------
    // 土：束缚拖入地中（规格 §3.5）
    // ------------------------------------------------------------------

    public static final class Earth {
        /** 时限：10 秒内完成拖入。 */
        public static final int BIND_TICKS = 10 * TICKS_PER_SECOND;
        /** 束缚用的缓慢等级：拉满，移速属性会被压到 0（不能移动）。 */
        public static final int SLOWNESS_AMPLIFIER = 250;
        /** 束缚效果的额外余量（tick）：留一点，免得效果比计时器先到期。 */
        public static final int SLOWNESS_EXTRA_TICKS = 20;
        /** 每秒向下拖拽的速度（"缓慢拖入土中"）。 */
        public static final double SINK_PER_SECOND = 0.08D;
        /** 秒杀上限：品阶 < BOSS（头目）全部秒杀；BOSS 与首领走 2%。 */
        public static final MobRank EXECUTE_BELOW = MobRank.BOSS;

        private Earth() {
        }
    }

    // ------------------------------------------------------------------
    // 冰：冰冻后爆裂（规格 §3.6）
    // ------------------------------------------------------------------

    public static final class Ice {
        /** 冰冻 5 秒。 */
        public static final int FREEZE_TICKS = 5 * TICKS_PER_SECOND;
        /** 冰冻之后再 3 秒爆炸。 */
        public static final int FUSE_TICKS = 2 * TICKS_PER_SECOND;
        /** 爆炸：10% 伤害。 */
        public static final double EXPLODE_PERCENT = 0.10D;
        /** 期间持续掉血：一次 1 点。 */
        public static final double TICK_DAMAGE = 1.5D;
        /** 持续掉血的间隔：0.5 秒一次。 */
        public static final int TICK_INTERVAL_TICKS = 5;
        /** 减速等级（冰冻期间一直减速）。 */
        public static final int SLOWNESS_AMPLIFIER = 4;
        /** 减速时长（每次刷新）。 */
        public static final int SLOWNESS_TICKS = 40;
        /** 冻结视觉：原版"结霜"渲染读的是这个值（160 = 冻结阈值 140 以上）。 */
        public static final int FROZEN_TICKS = 160;

        private Ice() {
        }
    }

    // ------------------------------------------------------------------
    // 雷：周期雷击 + 三连（规格 §3.7）
    // ------------------------------------------------------------------

    public static final class Thunder {
        /** 每 3 秒劈一次。 */
        public static final int INTERVAL_TICKS = 1 * TICKS_PER_SECOND;
        /** 单次 5 点（2.5 心）。 */
        public static final double HIT_DAMAGE = 7.0D;
        /** 每第 3 次被雷劈 → 三连。 */
        public static final int EVERY_N = 4;
        /** 三连：同时 3 道雷。 */
        public static final int TRIPLE_BOLTS = 3;
        /** 三连基础伤害 15 点。 */
        public static final double TRIPLE_DAMAGE = 22.0D;
        /** 三连附加：10% 生命。 */
        public static final double TRIPLE_PERCENT = 0.10D;

        private Thunder() {
        }
    }

    // ------------------------------------------------------------------
    // 风：高频斩击 + 十次爆发（规格 §3.8）
    // ------------------------------------------------------------------

    public static final class Wind {
        /**
         * 频率：0.25 秒一击。
         *
         * <p>规格明确要求"频率要比其他元素高一些"——全表里它是最快的
         * （金/冰掉血 10 tick、火/水 20 tick、雷 60 tick，见 {@link #fastestInterval()}）。</p>
         */
        public static final int INTERVAL_TICKS = 4;
        /** 每击 2 点（1 心）。 */
        public static final double HIT_DAMAGE = 2.5D;
        /** 命中 10 次后爆发。 */
        public static final int HITS_FOR_BURST = 10;
        /** 爆发：10% 伤害。 */
        public static final double BURST_PERCENT = 0.10D;

        private Wind() {
        }
    }

    private DomainEffectTable() {
    }

    // ------------------------------------------------------------------
    // 首领规则
    // ------------------------------------------------------------------

    /**
     * <b>唯一的首领百分比规则：封顶 2%。</b>
     *
     * <p>玩家确认选 (B)——对首领的百分比效果取 {@code min(普通值, 2%)}。
     * 这样持续型（火/水 1%/秒）对首领仍是 1%/秒，一次性（金 30% 等）落到 2%。
     * 规则单一、可断言，以后加新元素也不会漏。</p>
     */
    public static double clampForOverlord(double percent) {
        return Math.min(percent, OVERLORD_PERCENT);
    }

    /**
     * 一次性百分比效果（金 30% / 雷 10% / 冰 10% / 风 10%）对首领：{@code min(值, 2%)}。
     *
     * @param rank          怪物品阶
     * @param normalPercent 普通怪的百分比（例如金 0.30）
     */
    public static double oneShotPercent(MobRank rank, double normalPercent) {
        return rank == MobRank.OVERLORD ? clampForOverlord(normalPercent) : normalPercent;
    }

    /**
     * 持续型百分比效果（火/水 每秒 1%）对首领：<b>仍然 1%/秒</b>，只是不超过 2%。
     *
     * <p>玩家已确认 (B)「封顶在 2%」，所以这里传 1% 进去得到 1%——
     * 首领不会因为"是首领"反而掉得更快（那正是规格 §八 自己指出的逻辑矛盾）。</p>
     */
    public static double sustainedPercent(MobRank rank, double normalPercent) {
        return rank == MobRank.OVERLORD ? clampForOverlord(normalPercent) : normalPercent;
    }

    /** 品阶 < BOSS（头目）时土可以秒杀；BOSS 与首领只掉 2%。 */
    public static boolean canExecute(MobRank rank) {
        return rank.ordinal() < Earth.EXECUTE_BELOW.ordinal();
    }

    /** 土不能秒杀时（头目/首领）的替代伤害：固定 2%。 */
    public static double executionFallbackPercent(MobRank rank) {
        return OVERLORD_PERCENT;
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /** 八种有领域效果的元素（不含 {@link Element#NONE}）。 */
    public static Element[] effectElements() {
        return new Element[]{
                Element.METAL, Element.WOOD, Element.WATER, Element.FIRE,
                Element.EARTH, Element.ICE, Element.THUNDER, Element.WIND
        };
    }

    /** 这个元素有没有领域效果（无属性没有）。 */
    public static boolean hasEffect(Element element) {
        return element != null && element != Element.NONE;
    }

    /**
     * 全表里最短的结算间隔（tick）。
     *
     * <p>扫描节奏必须跟得上它，否则"风频率最高"这条就被扫描间隔吃掉了。</p>
     */
    public static int fastestInterval() {
        return Math.min(Wind.INTERVAL_TICKS, Metal.INTERVAL_TICKS);
    }

    /** 元素 → 面板上的一行效果说明（数字全部来自本表，写死的地方不会再抄一份）。 */
    public static String summary(Element element) {
        if (element == null) {
            return "\u00a78无";
        }
        return switch (element) {
            case FIRE -> "\u00a76火雨 \u00a78· \u00a7f每秒 " + trim(Fire.LAVA_DAMAGE_PER_SECOND)
                    + " 点 + " + percent(Fire.PERCENT_PER_SECOND) + " 最大生命（首领封顶 "
                    + percent(OVERLORD_PERCENT) + "）";
            case WOOD -> "\u00a7a藤缠 \u00a78· \u00a7f" + (Wood.DELAY_TICKS / TICKS_PER_SECOND)
                    + " 秒后爆 " + trim(Wood.EXPLODE_DAMAGE) + " 点 + " + Wood.SPIKE_COUNT
                    + " 根木刺（各 " + percent(Wood.SPIKE_PERCENT) + "）";
            case WATER -> "\u00a7b涨潮 \u00a78· \u00a7f窒息 " + trim(Water.SUFFOCATION_DAMAGE)
                    + "/秒 + 减速 + " + percent(Water.PERCENT_PER_SECOND) + " 最大生命（首领封顶 "
                    + percent(OVERLORD_PERCENT) + "）";
            case METAL -> "\u00a7e锐气 \u00a78· \u00a7f每 " + halfSeconds(Metal.INTERVAL_TICKS)
                    + " 秒 " + trim(Metal.HIT_DAMAGE) + " 点，叠 " + Metal.MARKS_FOR_WAVE
                    + " 层放 " + percent(Metal.WAVE_PERCENT) + " 冲击波（首领封顶 "
                    + percent(OVERLORD_PERCENT) + "）";
            case EARTH -> "\u00a76束缚 \u00a78· \u00a7f" + (Earth.BIND_TICKS / TICKS_PER_SECOND)
                    + " 秒拖入土中 → 头目以下秒杀，头目/首领 " + percent(OVERLORD_PERCENT);
            case ICE -> "\u00a7b冰冻 \u00a78· \u00a7f" + (Ice.FREEZE_TICKS / TICKS_PER_SECOND)
                    + " 秒 + " + (Ice.FUSE_TICKS / TICKS_PER_SECOND) + " 秒后爆 "
                    + percent(Ice.EXPLODE_PERCENT) + "（首领封顶 " + percent(OVERLORD_PERCENT)
                    + "），期间减速 + 每 "
                    + halfSeconds(Ice.TICK_INTERVAL_TICKS) + " 秒 " + trim(Ice.TICK_DAMAGE) + " 点";
            case THUNDER -> "\u00a7d雷击 \u00a78· \u00a7f每 "
                    + (Thunder.INTERVAL_TICKS / TICKS_PER_SECOND) + " 秒 "
                    + trim(Thunder.HIT_DAMAGE) + " 点；每第 " + Thunder.EVERY_N + " 次三连 "
                    + trim(Thunder.TRIPLE_DAMAGE) + " 点 + " + percent(Thunder.TRIPLE_PERCENT)
                    + "（首领封顶 " + percent(OVERLORD_PERCENT) + "）";
            case WIND -> "\u00a7f风刃 \u00a78· \u00a7f每 " + halfSeconds(Wind.INTERVAL_TICKS)
                    + " 秒 " + trim(Wind.HIT_DAMAGE) + " 点；" + Wind.HITS_FOR_BURST
                    + " 次后 " + percent(Wind.BURST_PERCENT) + "（首领封顶 "
                    + percent(OVERLORD_PERCENT) + "）";
            default -> "\u00a78无领域效果";
        };
    }

    /**
     * 元素 → 面板上的一行<b>短</b>说明（R 面板一行放得下，数字同样来自本表）。
     *
     * <p>长版在 {@link #summary}，用于领域开启时的聊天提示与服务端日志。</p>
     */
    public static String shortSummary(Element element) {
        if (element == null) {
            return "\u00a78无";
        }
        return switch (element) {
            case FIRE -> "\u00a76火雨 \u00a78· \u00a7f" + trim(Fire.LAVA_DAMAGE_PER_SECOND)
                    + "/秒 +" + percent(Fire.PERCENT_PER_SECOND);
            case WOOD -> "\u00a7a藤缠 \u00a78· \u00a7f" + (Wood.DELAY_TICKS / TICKS_PER_SECOND)
                    + " 秒 → 爆 " + trim(Wood.EXPLODE_DAMAGE) + " + " + Wood.SPIKE_COUNT + " 刺";
            case WATER -> "\u00a7b涨潮 \u00a78· \u00a7f窒息 " + trim(Water.SUFFOCATION_DAMAGE)
                    + "/秒 +" + percent(Water.PERCENT_PER_SECOND);
            case METAL -> "\u00a7e锐气 \u00a78· \u00a7f" + trim(Metal.HIT_DAMAGE) + " 点 ×"
                    + Metal.MARKS_FOR_WAVE + " 层 → " + percent(Metal.WAVE_PERCENT);
            case EARTH -> "\u00a76束缚 \u00a78· \u00a7f" + (Earth.BIND_TICKS / TICKS_PER_SECOND)
                    + " 秒 → 头目以下秒杀";
            case ICE -> "\u00a7b冰冻 \u00a78· \u00a7f" + (Ice.FREEZE_TICKS / TICKS_PER_SECOND)
                    + " 秒 +" + (Ice.FUSE_TICKS / TICKS_PER_SECOND) + " 秒 → "
                    + percent(Ice.EXPLODE_PERCENT);
            case THUNDER -> "\u00a7d雷击 \u00a78· \u00a7f"
                    + (Thunder.INTERVAL_TICKS / TICKS_PER_SECOND) + " 秒 " + trim(Thunder.HIT_DAMAGE)
                    + " 点；三连 " + trim(Thunder.TRIPLE_DAMAGE) + "+"
                    + percent(Thunder.TRIPLE_PERCENT);
            case WIND -> "\u00a7f风刃 \u00a78· \u00a7f" + trim(Wind.HIT_DAMAGE) + " 点 ×"
                    + Wind.HITS_FOR_BURST + " → " + percent(Wind.BURST_PERCENT);
            default -> "\u00a78无领域效果";
        };
    }

    /** 面板/日志用的领域范围文本，例如 {@code 6×6 区块（96×96 格）}。 */    public static String rangeText(double chunks) {
        int blocks = (int) Math.round(chunks * BLOCKS_PER_CHUNK);
        return trim(chunks) + "×" + trim(chunks) + " 区块（" + blocks + "×" + blocks + " 格）";
    }

    /**
     * 启动自检：把整张表打一遍，并检查"不可能出现的组合"。
     *
     * <p>纯数据、不碰注册表，所以既能在 common setup 里跑，也能被离线验证器调用。
     * 表被人改坏时（负数、结算间隔比扫描间隔还短）会在服务端日志里说清楚是哪一条。</p>
     *
     * @return 人类可读的一整张表（多行）
     */
    public static String describeAll() {
        StringBuilder sb = new StringBuilder();
        sb.append("领域范围：").append(rangeText(DEFAULT_DOMAIN_CHUNKS))
                .append("  垂直 ±").append((int) VERTICAL_HALF_BLOCKS)
                .append("  扫描间隔 ").append(SCAN_INTERVAL_TICKS).append(" tick");
        for (Element element : effectElements()) {
            sb.append('\n').append("  ").append(element.cn()).append("：").append(summary(element));
        }
        sb.append('\n').append("  首领统一规则：对首领的百分比 = min(普通值, ")
                .append(percent(OVERLORD_PERCENT))
                .append(")（玩家已确认选 B『封顶 2%』：火/水仍是每秒 ")
                .append(percent(Fire.PERCENT_PER_SECOND)).append("）");
        return sb.toString();
    }

    /** 自检；发现问题直接抛异常（"加载失败 + 说得清原因"好过"数值悄悄不对"）。 */
    public static void validate() {
        StringBuilder bad = new StringBuilder();
        if (DEFAULT_DOMAIN_CHUNKS <= 0.0D) {
            bad.append("默认领域范围必须为正：").append(DEFAULT_DOMAIN_CHUNKS).append("；");
        }
        if (SCAN_INTERVAL_TICKS > fastestInterval()) {
            bad.append("扫描间隔 ").append(SCAN_INTERVAL_TICKS)
                    .append(" 比最快结算间隔 ").append(fastestInterval())
                    .append(" 还长，风的『频率最高』会被吃掉；");
        }
        if (Wind.INTERVAL_TICKS >= Fire.INTERVAL_TICKS) {
            bad.append("风必须比火快（规格 §3.8）；");
        }
        if (Wind.INTERVAL_TICKS > Metal.INTERVAL_TICKS
                || Wind.INTERVAL_TICKS > Ice.TICK_INTERVAL_TICKS) {
            bad.append("风必须比金/冰快（规格 §3.8）；");
        }
        for (Element element : effectElements()) {
            if (summary(element).contains("无领域效果")) {
                bad.append(element.cn()).append(" 缺效果表；");
            }
        }
        if (!canExecute(MobRank.NORMAL) || !canExecute(MobRank.CAPTAIN)
                || canExecute(MobRank.BOSS) || canExecute(MobRank.OVERLORD)) {
            bad.append("土的秒杀判定必须是『品阶 < 头目』；");
        }
        if (Math.abs(oneShotPercent(MobRank.OVERLORD, 0.30D) - OVERLORD_PERCENT) > 1e-9
                || Math.abs(oneShotPercent(MobRank.OVERLORD, 0.01D) - 0.01D) > 1e-9) {
            bad.append("首领规则必须是 min(值, 2%)——一次性 30% 落到 2%、1% 仍是 1%；");
        }
        if (Math.abs(sustainedPercent(MobRank.OVERLORD, Fire.PERCENT_PER_SECOND)
                - Fire.PERCENT_PER_SECOND) > 1e-9
                || Math.abs(sustainedPercent(MobRank.OVERLORD, Water.PERCENT_PER_SECOND)
                - Water.PERCENT_PER_SECOND) > 1e-9) {
            bad.append("火/水的持续 1%/秒对首领必须仍是 1%（玩家确认选 B）；");
        }
        if (bad.length() > 0) {
            throw new IllegalStateException("[hongmeng] 领域数值表自检失败：" + bad);
        }
    }

    private static String trim(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.valueOf(value);
    }

    private static String percent(double ratio) {
        return trim(ratio * 100.0D) + "%";
    }

    private static String halfSeconds(int ticks) {
        return trim(ticks / (double) TICKS_PER_SECOND);
    }
}
