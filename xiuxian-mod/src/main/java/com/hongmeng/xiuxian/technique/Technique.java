package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.core.Realm;
import com.hongmeng.xiuxian.spirit.Element;

/**
 * 功法目录。
 *
 * <p><b>刻意做成纯数据枚举</b>——不含任何 Minecraft 类型，所以离线验证器
 * 可以直接断言全部功法（数量、境界门槛、冷却、倍率、元素覆盖、蓄力、灵力定价），
 * 不需要注册表引导。</p>
 *
 * <p>每门功法都有明确的**特性**（{@link #trait()}），不是单纯的"伤害数字不同"：</p>
 *
 * <ul>
 *   <li>有的带位移（金虹遁、风影步）</li>
 *   <li>有的穿透多个目标（万剑归宗）</li>
 *   <li>有的自伤换伤害（焚天诀）</li>
 *   <li>有的生命越低伤害越高（血魔噬）</li>
 *   <li>有的根据目标数量增伤（九霄雷狱）</li>
 * </ul>
 *
 * <h3>两个价格字段</h3>
 *
 * <ul>
 *   <li>{@link #castTicks()} 蓄力时间：0 = 瞬发；小法术 10~20；大法术 40~60；
 *       辅助 20~40。施法期间移动/受伤/换手会中断。</li>
 *   <li>{@link #manaCost()} 灵力消耗：按"境界门槛 × 威力"定价，
 *       练气十来点、飞升两千上下、鸿蒙两万上下。</li>
 * </ul>
 *
 * <p><b>枚举顺序不可重排</b>：{@code TechniqueItems} 按 {@code manual_<小写枚举名>}
 * 注册物品，商店按 {@code values()} 生成条目，网络协议里传的是 ordinal。
 * 新增功法一律**追加在末尾**。</p>
 *
 * @see TechniqueKind 释放形态
 * @see TechniqueData 已修习与法术栏
 */
public enum Technique {

    // ---------------- 金 ----------------

    JIN_GANG_QIAN("金刚指", '7', Element.METAL, 1, 24, 1.0D, TechniqueKind.LINE, "凝气于指，一线穿出", "穿透 3 格内的所有敌人", 32, 80),

    WAN_JIAN_GUI_ZONG("万剑归宗", 'b', Element.METAL, 5, 24, 2.4D, TechniqueKind.FAN, "万千剑影归于一处", "扇形射出 7 道剑气", 60, 800),

    JIN_HONG_DUN("金虹遁", 'e', Element.METAL, 6, 24, 1.2D, TechniqueKind.DASH, "化虹而行，瞬息百丈", "向前突进并灼伤沿途敌人", 10, 440),

    // ---------------- 木 ----------------

    QING_MU_SHENG("青木生", 'a', Element.WOOD, 1, 24, 0.8D, TechniqueKind.HEAL, "引草木之息自愈", "回复 25% 生命上限", 10, 70),

    WAN_MU_KUN_SUO("万木困锁", '2', Element.WOOD, 4, 24, 0.8D, TechniqueKind.AOE, "地生藤蔓，缚敌于原地", "范围定身 3 秒", 60, 260),

    CHUN_FENG_HUA_YU("春风化雨", 'a', Element.WOOD, 6, 24, 1.6D, TechniqueKind.BUFF, "灵雨润身，久战不疲", "范围持续治疗 10 秒", 10, 310),

    // ---------------- 水 ----------------

    HAN_BING_JIAN("寒冰箭", '3', Element.WATER, 3, 24, 1.4D, TechniqueKind.PROJECTILE, "凝水成冰，射而出之", "命中减速 4 秒", 40, 165),

    BING_FENG_JUE("冰封诀", 'b', Element.WATER, 5, 24, 1.6D, TechniqueKind.AOE, "方圆之内尽成冰狱", "范围冻结 2.5 秒", 60, 540),

    QIAN_LI_BING_FENG("千里冰封", '3', Element.WATER, 8, 24, 3.6D, TechniqueKind.AOE, "冰封千里，万物寂灭", "大范围重创并减速", 60, 4400),

    // ---------------- 火 ----------------

    LIE_YAN_ZHANG("烈焰掌", 'c', Element.FIRE, 3, 24, 1.6D, TechniqueKind.AOE, "掌出烈焰，扑面而来", "近身范围并点燃", 40, 190),

    FEN_TIAN_JUE("焚天诀", '6', Element.FIRE, 7, 24, 5.0D, TechniqueKind.AOE, "以己身为薪，焚尽一切", "自伤 15% 换极高伤害", 60, 2600),

    FEN_JI_LIAO_YUAN("焚寂燎原", '6', Element.FIRE, 9, 24, 6.0D, TechniqueKind.AOE, "一息之间，原野成灰", "超大范围火海", 60, 6600),

    // ---------------- 土 ----------------

    HOU_TU_DUN("厚土盾", 'e', Element.EARTH, 2, 24, 0.0D, TechniqueKind.BUFF, "地气加身，坚不可摧", "抗性提升 II 持续 12 秒", 10, 130),

    LUO_DI_SHEN_ZHEN("落地生根", '2', Element.EARTH, 5, 24, 0.6D, TechniqueKind.AOE, "足踏大地，震动四野", "震退周围敌人", 60, 340),

    SHAN_HE_YIN("山河印", 'e', Element.EARTH, 8, 24, 4.0D, TechniqueKind.AOE, "一印落下，山岳倾颓", "高伤 + 击飞", 60, 4360),

    // ---------------- 冰（变异） ----------------

    XUAN_BING_DUN("玄冰盾", 'b', Element.ICE, 6, 24, 0.0D, TechniqueKind.BUFF, "万载玄冰为甲", "抗性 III 持续 10 秒", 10, 310),

    BING_HUN_ZHAN("冰魂斩", '3', Element.ICE, 7, 24, 3.2D, TechniqueKind.PROJECTILE, "冰魂所过，血凝不流", "穿透 3 个目标并冻伤", 60, 2320),

    // ---------------- 雷（变异） ----------------

    ZI_XIAO_LEI_FA("紫霄雷法", 'd', Element.THUNDER, 6, 24, 2.8D, TechniqueKind.AOE, "引天雷入体，再吐而出", "落雷并麻痹", 60, 880),

    JIU_XIAO_LEI_YU("九霄雷狱", '5', Element.THUNDER, 9, 24, 4.5D, TechniqueKind.AOE, "九天雷狱，无处可逃", "目标越多伤害越高", 60, 5500),

    // ---------------- 风（变异） ----------------

    FENG_REN_ZHAN("风刃斩", 'f', Element.WIND, 4, 24, 1.5D, TechniqueKind.FAN, "风凝成刃，无形无相", "扇形 3 道风刃", 48, 240),

    QING_FENG_JUE("轻风诀", 'f', Element.WIND, 5, 24, 0.0D, TechniqueKind.BUFF, "身轻如风", "速度提升 II 持续 15 秒", 10, 260),

    FENG_YING_BU("风影步", '7', Element.WIND, 7, 24, 0.9D, TechniqueKind.DASH, "风行无影", "瞬移 12 格并留下残影", 10, 360),

    // ---------------- 无属性 ----------------

    XUE_MO_SHI("血魔噬", '4', Element.NONE, 7, 24, 1.8D, TechniqueKind.LIFESTEAL, "噬敌之血以补己身", "伤害的 60% 转为治疗", 60, 1900),

    SHEN_SHI_ZHEN("神识震", '5', Element.NONE, 8, 24, 3.0D, TechniqueKind.AOE, "神识外放，直击魂魄", "无视护甲的真实伤害", 60, 3600),

    // ---------------- 鸿蒙（绝巅） ----------------

    HONG_MENG_YI_QI("鸿蒙一气", '6', Element.NONE, 10, 24, 9.0D, TechniqueKind.AOE, "开天辟地之前，本就只有这一口气", "超大范围 · 施展后回满生命", 60, 23500),

    // ==================================================================
    // 法术栏扩充（追加，不重排上面的任何一条）
    //
    // 覆盖 攻击 / 防御 / 辅助 / 位移 / 控制 五类，外加 8 门被动
    // （被动至少 8 门，5 个被动槽才有得选）。
    // ==================================================================

    // ---------------- 新增 · 攻击 ----------------

    YU_JIAN_ZHAN("御剑斩", 'b', Element.METAL, 4, 24, 2.2D, TechniqueKind.LINE, "剑随心动，斩于百步之外", "直线贯穿并附加剑气震荡", 60, 400),

    LEI_FA("雷罚", 'd', Element.THUNDER, 6, 24, 3.2D, TechniqueKind.PROJECTILE, "天雷有司，代天行罚", "单体高伤并连锁麻痹", 56, 980),

    BING_FENG_QIAN_LI("冰封千里", '3', Element.ICE, 7, 24, 3.0D, TechniqueKind.AOE, "千里冰封，万里雪飘", "大范围冻结战场", 60, 2500),

    FEN_TIAN("焚天", '6', Element.FIRE, 7, 24, 5.2D, TechniqueKind.AOE, "此火一起，天亦为炉", "大范围灼烧 · 持续引燃", 60, 3100),

    // ---------------- 新增 · 位移 ----------------

    YI_XING_HUAN_YING("移形换影", '5', Element.WIND, 5, 24, 1.6D, TechniqueKind.DASH, "影随形动，形随影移", "瞬移并留下残影迷惑敌人", 10, 420),

    // ---------------- 新增 · 防御 ----------------

    JIN_GANG_HU_TI("金刚护体", 'e', Element.METAL, 5, 24, 0.0D, TechniqueKind.BUFF, "金刚不坏，万法不侵", "抗性提升 III 持续 8 秒", 10, 260),

    // ---------------- 新增 · 辅助 ----------------

    HUI_CHUN_SHU("回春术", 'a', Element.WOOD, 4, 24, 0.0D, TechniqueKind.HEAL, "枯木逢春，断骨重续", "回复 35% 生命上限并清除虚弱", 10, 200),

    // ---------------- 新增 · 控制 ----------------

    SHE_HUN("摄魂", '5', Element.NONE, 6, 24, 2.0D, TechniqueKind.LIFESTEAL, "魂为所摄，身不由己", "命中后吸取生命并令其失神", 60, 640),

    // ---------------- 新增 · 被动（诀） ----------------
    // 被动不主动释放、不需要目标、释放时不消耗灵力；装备到 5 个被动槽即生效。

    LING_HAI_WU_LIANG("灵海无量", 'b', Element.NONE, 2, 14, 0.0D, TechniqueKind.PASSIVE, "灵海无涯，纳百川而不竭", "灵力上限 +25%", 14, 0),

    TU_NA_JUE("吐纳诀", 'a', Element.NONE, 1, 14, 0.0D, TechniqueKind.PASSIVE, "一呼一吸，皆合天道", "灵力回复 +50%", 14, 0),

    JIN_GANG_TI("金刚体", '7', Element.METAL, 3, 14, 0.0D, TechniqueKind.PASSIVE, "身如金刚，刀兵难伤", "护甲 +8", 14, 0),

    JI_FENG_BU("疾风步", 'f', Element.WIND, 2, 14, 0.0D, TechniqueKind.PASSIVE, "身轻如燕，踏风而行", "移动速度 +15%", 14, 0),

    JIAN_XIN_TONG_MING("剑心通明", 'e', Element.METAL, 4, 14, 0.0D, TechniqueKind.PASSIVE, "心中有剑，万物皆可为剑", "持剑时伤害 +20%", 14, 0),

    WU_XING_TIAO_HE("五行调和", '2', Element.EARTH, 3, 14, 0.0D, TechniqueKind.PASSIVE, "五行相济，百邪不侵", "护甲 +5 · 护甲韧性 +4", 14, 0),

    SHEN_SHI_NING_LIAN("神识凝练", '5', Element.NONE, 6, 14, 0.0D, TechniqueKind.PASSIVE, "神识如刀，愈磨愈利", "法术伤害 +12%", 14, 0),

    QI_XUE_CHONG_YING("气血充盈", 'c', Element.FIRE, 4, 14, 0.0D, TechniqueKind.PASSIVE, "气血如潮，生生不息", "生命上限 +15%", 14, 0),

    // ---------------- 新增 · 战斗类被动（玩家要求：吸血/增伤/减伤/闪避/攻击） ----------------
    // 原来 8 门被动全是"灵力/护甲/移速/生命"，一件战斗向的都没有。

    XUE_MO_GONG("血魔功", '4', Element.FIRE, 5, 14, 0.0D, TechniqueKind.PASSIVE, "血海无涯，以敌之血养我之身", "造成伤害的 8% 转为自身生命", 14, 0),

    PO_JUN_JUE("破军诀", '6', Element.METAL, 5, 14, 0.0D, TechniqueKind.PASSIVE, "破军之势，无坚不摧", "造成伤害 +18%", 14, 0),

    GUI_XI_GONG("龟息功", '2', Element.EARTH, 4, 14, 0.0D, TechniqueKind.PASSIVE, "龟息守元，百击不破", "受到伤害 -15%", 14, 0),

    LING_BO_WEI_BU("凌波微步", '3', Element.WIND, 5, 14, 0.0D, TechniqueKind.PASSIVE, "凌波微步，罗袜生尘", "受击时 12% 概率完全闪避", 14, 0),

    ZHAN_YI_FEI_TENG("战意沸腾", '4', Element.FIRE, 6, 14, 0.0D, TechniqueKind.PASSIVE, "战意如沸，愈战愈勇", "攻击力 +15%", 14, 0),

    // ---------------- 灭世法术（玩家要求：参考超位魔法与轨道炮模组） ----------------
    // 三发都要求第 8 境（仙人）以上，灵力消耗极高、冷却极长。
    // 观感是"从天而降的毁灭"：光柱、冲击环、蘑菇云、贯穿光刃。

    TIAN_YUN_XING_LUO("天陨星落", '6', Element.FIRE, 8, 60, 3.5D, TechniqueKind.ULTIMATE, "天外星陨，一落成墟", "半径 12 格重击 + 三波余震", 120, 3600),

    GUI_DAO_PAO_JI("轨道炮击", 'b', Element.THUNDER, 8, 50, 3.0D, TechniqueKind.ULTIMATE, "自天顶垂落的一线之光，贯穿所见", "60 格贯穿，命中者定身", 100, 3200),

    WAN_XIANG_JI_MIE("万象寂灭", '5', Element.NONE, 10, 90, 4.5D, TechniqueKind.ULTIMATE, "万象归寂，唯余鸿蒙", "半径 20 格三段爆发 + 百分比伤害", 180, 6000),

    // ---- 超位魔法（玩家要求：参考超位魔法再加几发）----

    TAI_XU_HEI_DONG("太虚黑洞", '5', Element.NONE, 9, 80, 4.0D, TechniqueKind.GRAVITY, "太虚之中，连光都逃不出去", "生成奇点，半径 18 格持续牵引并绞碎", 160, 4800),

    ZHAN_XING("斩星", '6', Element.METAL, 9, 45, 3.2D, TechniqueKind.SLASH, "一剑斩落星辰，天地为之开", "80 格长、8 格宽的斩击深渊", 90, 4200),

    FEN_TIAN_ZHU_HAI("焚天煮海", '4', Element.FIRE, 8, 55, 3.6D, TechniqueKind.ULTIMATE, "焚尽苍穹，煮沸沧海", "半径 24 格点燃 + 三段火焰爆发", 110, 3600),

    // ==================================================================
    // 职业主动 · 批次二（《07-法术内容交接表》§2 的 18 门**新增**条目）
    //
    // 职业主动·只在天赋树解锁后可用；不生成秘籍、不进坊市
    // （过滤见 {@code CareerTechnique#isUnlockOnly}，注册/商店各自跳过）。
    // 只追加在末尾：旧 52 条的字段、顺序、ordinal 一个字符都不动。
    // 另 2 门复用条目不在此处新增：WAN_JIAN_GUI_ZONG / JIN_GANG_HU_TI。
    // ==================================================================

    // ---------------- 剑修（3 新增；万剑归宗复用 WAN_JIAN_GUI_ZONG） ----------------

    JIAN_QI_ZHAN("剑气斩", '7', Element.METAL, 6, 30, 1.6D, TechniqueKind.LINE, "剑意凝于一线，出鞘即斩", "【职业功法】前方直线剑气，命中叠剑意", 20, 200),
    JIAN_YU_SHU("御剑术", 'b', Element.METAL, 7, 10, 0.9D, TechniqueKind.DASH, "人剑合一，踏剑而行", "【职业功法】短距位移/飞剑突进", 0, 600),
    JIAN_YU("剑域", 'b', Element.METAL, 6, 40, 2.0D, TechniqueKind.AOE, "罡气化域，万剑归位", "【职业功法】罡气化域：范围内持续剑气伤害", 40, 500),

    // ---------------- 血修（4） ----------------

    XUE_DUN("血遁", '4', Element.FIRE, 5, 20, 1.2D, TechniqueKind.DASH, "血化长虹，瞬息遁走", "【职业功法】消耗血池短距冲刺，路径留血痕", 0, 800),
    XUE_BAO("血爆", 'c', Element.FIRE, 6, 50, 2.6D, TechniqueKind.AOE, "血池倾覆，爆散成雨", "【职业功法】以自身为中心血爆 AOE", 40, 800),
    XUE_SHI("血噬", '4', Element.FIRE, 7, 40, 1.8D, TechniqueKind.LIFESTEAL, "血刃饮敌，反哺己身", "【职业功法】吸血斩击", 60, 600),
    XUE_HE("血河", '4', Element.FIRE, 7, 60, 2.2D, TechniqueKind.AOE, "血河横流，寸步难行", "【职业功法】持续血河区域，减速 + DoT", 60, 500),

    // ---------------- 魔修（4） ----------------

    MO_QI_BAO_FA("魔气爆发", 'd', Element.THUNDER, 7, 60, 3.0D, TechniqueKind.AOE, "魔气盈仓，一念崩天", "【职业功法】消耗魔气范围爆发", 60, 800),
    MO_SHI_HUN("噬魂", '5', Element.NONE, 7, 40, 1.9D, TechniqueKind.LIFESTEAL, "噬魂夺魄，魔气自归", "【职业功法】单体高伤 + 回气", 60, 600),
    MO_YING_DUN("魔影遁", '5', Element.NONE, 6, 20, 1.0D, TechniqueKind.DASH, "魔影随身，形遁于暗", "【职业功法】短距位移 + 留魔影", 0, 800),
    MO_XIN_MO_YIN("心魔引", '5', Element.NONE, 7, 60, 2.0D, TechniqueKind.GRAVITY, "心魔一引，万念俱焚", "【职业功法】控制/拉拽/恐惧", 50, 700),

    // ---------------- 鬼修（4） ----------------

    GUI_SUO_HUN_LIAN("锁魂链", '5', Element.NONE, 6, 30, 1.7D, TechniqueKind.LINE, "锁魂成链，缚敌于渊", "【职业功法】直线锁链，叠魂印 + 拉拽", 40, 600),
    GUI_YU_ZHAN_KAI("鬼域展开", '5', Element.NONE, 7, 50, 2.2D, TechniqueKind.AOE, "鬼域展开，生人勿近", "【职业功法】主动展开大范围鬼域", 60, 600),
    GUI_XU_HUA("虚化", '3', Element.ICE, 6, 30, 0.0D, TechniqueKind.DASH, "虚实之间，不染尘埃", "【职业功法】短无敌 + 位移", 0, 800),
    GUI_HUN_BAO("魂爆", '5', Element.NONE, 8, 50, 2.8D, TechniqueKind.PROJECTILE, "魂印既满，一爆成灰", "【职业功法】引爆所有魂印", 50, 800),

    // ---------------- 体修（3 新增；金刚护体复用 JIN_GANG_HU_TI） ----------------

    TI_ZHEN_DANG_BO("震荡波", '2', Element.EARTH, 6, 50, 2.4D, TechniqueKind.AOE, "激荡满溢，震散千军", "【职业功法】消耗激荡放范围击退 + 伤害", 40, 800),
    TI_BENG_SHAN_JI("崩山击", 'e', Element.EARTH, 7, 40, 2.8D, TechniqueKind.SLASH, "拳出崩山，甲裂骨穿", "【职业功法】重击破防", 60, 600),
    TI_BU_MIE_JIN_SHEN("不灭金身", '7', Element.METAL, 8, 30, 0.0D, TechniqueKind.HEAL, "金身不灭，血战到底", "【职业功法】短时锁血 + 回血", 40, 800);

    // ------------------------------------------------------------------

    private final String cn;
    private final char colorCode;
    private final Element element;
    /** 需要第几大境才能修习。 */
    private final int minTier;
    /** 冷却（tick）。 */
    private final int cooldown;
    /** 伤害倍率，相对本境标准值；0 表示纯辅助。 */
    private final double damageMultiplier;
    private final TechniqueKind kind;
    /** 一句话风貌。 */
    private final String flavor;
    /** **特性**：这门功法区别于其它功法的机制。 */
    private final String trait;
    /** 蓄力时间（tick）。0 = 瞬发。 */
    private final int castTicks;
    /** 灵力消耗。被动为 0（装备即生效，不消耗）。 */
    private final int manaCost;

    Technique(String cn, char colorCode, Element element, int minTier, int cooldown,
              double damageMultiplier, TechniqueKind kind, String flavor, String trait,
              int castTicks, int manaCost) {
        this.cn = cn;
        this.colorCode = colorCode;
        this.element = element;
        this.minTier = minTier;
        this.cooldown = cooldown;
        this.damageMultiplier = damageMultiplier;
        this.kind = kind;
        this.flavor = flavor;
        this.trait = trait;
        this.castTicks = castTicks;
        this.manaCost = manaCost;
    }

    public String cn() {
        return cn;
    }

    public char colorCode() {
        return colorCode;
    }

    public String colored() {
        return "\u00a7" + colorCode + cn;
    }

    public Element element() {
        return element;
    }

    public int minTier() {
        return minTier;
    }

    public int cooldown() {
        return cooldown;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public TechniqueKind kind() {
        return kind;
    }

    public String flavor() {
        return flavor;
    }

    public String trait() {
        return trait;
    }

    /** 蓄力时间（tick）。0 = 瞬发。 */
    public int castTicks() {
        return castTicks;
    }

    /** 灵力消耗。被动为 0。 */
    public int manaCost() {
        return manaCost;
    }

    /** 蓄力秒数（显示用）。 */
    public double castSeconds() {
        return castTicks / 20.0D;
    }

    /** 蓄力时间的可读文案。 */
    public String castLabel() {
        if (castTicks <= 0) {
            return "瞬发";
        }
        return String.format("%.1fs", castSeconds());
    }

    /**
     * 是不是**纯辅助**（不造成伤害）。
     *
     * <p>判据是**形态**而不是倍率：{@code HEAL}/{@code BUFF}/{@code PASSIVE}
     * 是纯辅助，其余形态都打伤害。之前用"倍率是否为 0"来判断是错的——
     * 血魔噬、青木生这类"有伤害但以辅助为主"的功法会被误判。</p>
     */
    public boolean isSupport() {
        return kind == TechniqueKind.HEAL || kind == TechniqueKind.BUFF
                || kind == TechniqueKind.PASSIVE;
    }

    /** 是不是被动功法：不能主动释放，只占被动槽。 */
    public boolean isPassive() {
        return kind == TechniqueKind.PASSIVE;
    }

    /** 能不能装备到 4 个主动槽。 */
    public boolean isActiveCastable() {
        return !isPassive();
    }

    /**
     * 伤害是否随境界缩放。
     *
     * <p>除纯辅助外都要缩放；纯辅助（治疗/增益/被动）的效果按生命上限或属性算，
     * 不需要倍率。</p>
     */
    public boolean scalesWithRealm() {
        return !isSupport();
    }

    /** 冷却秒数（显示用）。 */
    public double cooldownSeconds() {
        return cooldown / 20.0D;
    }

    /** 五类归档：攻击 / 防御 / 辅助 / 位移 / 控制 / 被动。 */
    public String archetype() {
        return kind.archetype();
    }

    /** 一句话摘要：元素 · 形态 · 蓄力 · 灵力 · 冷却。 */
    public String priceLabel() {
        return element.cn() + kind.glyph()
                + " · 蓄力 " + castLabel()
                + " · 灵力 " + manaCost
                + " · 冷却 " + String.format("%.1fs", cooldownSeconds());
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /** 这个境界能不能修习这门功法。 */
    public boolean availableAt(Realm realm) {
        return realm != null && realm.tier() >= minTier;
    }

    /** 该境界可以修习的全部功法。 */
    public static java.util.List<Technique> availableAtTier(int tier) {
        java.util.List<Technique> out = new java.util.ArrayList<>();
        for (Technique t : values()) {
            if (tier >= t.minTier) {
                out.add(t);
            }
        }
        return out;
    }

    /** 某个大境**新解锁**的功法。 */
    public static java.util.List<Technique> unlockedAtTier(int tier) {
        java.util.List<Technique> out = new java.util.ArrayList<>();
        for (Technique t : values()) {
            if (t.minTier == tier) {
                out.add(t);
            }
        }
        return out;
    }

    /** 按名字找（支持中文名或枚举名，大小写不敏感）。 */
    public static Technique byName(String name) {
        if (name == null) {
            return null;
        }
        for (Technique t : values()) {
            if (t.cn.equals(name) || t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    /** 元素对应的功法（用于道宫的同元素加成）。 */
    public static java.util.List<Technique> ofElement(Element element) {
        java.util.List<Technique> out = new java.util.ArrayList<>();
        for (Technique t : values()) {
            if (t.element == element) {
                out.add(t);
            }
        }
        return out;
    }

    /** 全部被动功法。 */
    public static java.util.List<Technique> passives() {
        java.util.List<Technique> out = new java.util.ArrayList<>();
        for (Technique t : values()) {
            if (t.isPassive()) {
                out.add(t);
            }
        }
        return out;
    }

    /** 全部可放进主动槽的功法。 */
    public static java.util.List<Technique> actives() {
        java.util.List<Technique> out = new java.util.ArrayList<>();
        for (Technique t : values()) {
            if (t.isActiveCastable()) {
                out.add(t);
            }
        }
        return out;
    }

    /** 某一类（攻击/防御/辅助/位移/控制/被动）的全部功法。 */
    public static java.util.List<Technique> ofArchetype(String archetype) {
        java.util.List<Technique> out = new java.util.ArrayList<>();
        for (Technique t : values()) {
            if (t.archetype().equals(archetype)) {
                out.add(t);
            }
        }
        return out;
    }
}
