package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.spirit.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <b>法术视觉定妆表（显式数据，取代"名字哈希自动派生"）</b>。
 *
 * <h2>为什么要有这张表</h2>
 * <p>原来每个法术的蓄力图案、层数、旋向、调色、释放身份全部由
 * {@code key.hashCode()} 派生 —— 好处是"天生不重复"，坏处是：</p>
 * <ul>
 *   <li><b>没法单独调</b>：想给「天陨星落」换一套图案，就得改哈希算法，等于全改；</li>
 *   <li><b>图案和法术内容对不上</b>：哈希不知道这是剑法还是冰牢，
 *       于是"冰封诀"可能抽到雨滴、"万剑归宗"可能抽到藤蔓；</li>
 *   <li><b>无法验收</b>：设计表上写的"视觉身份"和代码里实际跑的不是同一份数据。</li>
 * </ul>
 *
 * <p>现在把 52 门基础功法 + 20 门职业主动的视觉身份<b>逐门写成数据行</b>，
 * 改一门只动一行；{@link SpellFxRuntime} 与 {@link SpellSignature} 只负责"读表执行"。
 * 未登记的键（未来的新法术）自动退回旧的哈希推导，行为与以前完全一致。</p>
 *
 * <h2>字段（与《04-法术设计总表》表 C 一一对应）</h2>
 * <table border="1">
 *   <caption>一行 = 一门法术的视觉身份</caption>
 *   <tr><th>字段</th><th>范围</th><th>含义</th></tr>
 *   <tr><td>{@code identity}</td><td>0~5</td><td>释放身份的"相位号"，决定主形状（见 {@code primaryShapeFor}）</td></tr>
 *   <tr><td>{@code layers}</td><td>2~5</td><td>释放层数（层数越多越壮观，也会追加穹顶/符文环）</td></tr>
 *   <tr><td>{@code sizeStep}</td><td>0~2</td><td>尺寸档：0=×0.75 / 1=×1.0 / 2=×1.35</td></tr>
 *   <tr><td>{@code phase}</td><td>0~5</td><td>相位：每层寿命/尺寸的固定偏置（同门法术每次一致）</td></tr>
 *   <tr><td>{@code motifs}</td><td>三个不同图案</td><td>蓄力三图案（RING/SPIKE/PILLAR/RAIN/SPIRAL/VINE/FUNNEL/GLYPH）</td></tr>
 *   <tr><td>{@code chargeLayers}</td><td>3~6</td><td>蓄力环层数</td></tr>
 *   <tr><td>{@code spin}</td><td>±1</td><td>蓄力旋转方向</td></tr>
 *   <tr><td>{@code paletteShift}</td><td>0~3</td><td>调色板偏移：四档（主/副/光晕/残迹）在元素基色上错开</td></tr>
 *   <tr><td>{@code pitch}</td><td>0.8~1.3</td><td>施法音变调（同一元素音，不同法术音高不同）</td></tr>
 *   <tr><td>{@code setpiece}</td><td>枚举</td><td>专属演出：这门法术额外叠的那套"签名层"</td></tr>
 * </table>
 *
 * <p><b>改这一张表 = 改 52 门法术的观感</b>；不需要重排枚举、不需要改算法。</p>
 */
public final class SpellLook {

    /**
     * <b>专属演出</b>：在"形态骨架"之上，这门法术额外叠的一层叙事化特效。
     *
     * <p>每一段都在 {@link SpellFxRuntime#playSetpiece} 里实现，
     * 用的是已有的 29 种贴图形状 —— 所以不新增协议、不新增贴图也能做出差异。
     * 这些层是<b>服务端派发、客户端按形状绘制</b>，与既有管线完全一致。</p>
     */
    public enum Setpiece {
        /** 没有专属演出（被动功法、或纯靠骨架就够的法术）。 */
        NONE("无"),
        /** 直线贯穿：细光柱 + 末端爆闪 + 能量丝（金刚指 / 御剑斩 / 锁魂链）。 */
        PIERCE_LINE("贯穿"),
        /** 扇形剑雨：多把飞剑沿扇形展开（万剑归宗 / 风刃斩）。 */
        SWORD_FAN("剑雨"),
        /** 冰牢：晶格笼 + 雪花 + 穹顶（冰封诀 / 千里冰封 / 冰魂斩）。 */
        FROST_PRISON("冰牢"),
        /** 雷牢：立体晶格 + 多道落雷光柱（紫霄雷法 / 九霄雷狱 / 雷罚）。 */
        THUNDER_CAGE("雷牢"),
        /** 火海：多道火柱自地面升起 + 火环（烈焰掌 / 焚天 / 焚天煮海）。 */
        FLAME_STORM("火海"),
        /** 震地：地面裂纹 + 层叠环塔 + 碎片（落地生根 / 山河印 / 震荡波）。 */
        QUAKE("震地"),
        /** 摄魂：螺旋吸魂 + 虚空裂缝 + 反向环（血魔噬 / 摄魂 / 噬魂）。 */
        SOUL_DRAIN("摄魂"),
        /** 血池：血纹地面 + 双螺旋 + 上升血滴（血爆 / 血河）。 */
        BLOOD_POOL("血池"),
        /** 遁术：光幕残影 + 环绕光球（金虹遁 / 风影步 / 移形换影）。 */
        AFTERIMAGE("残影"),
        /** 护体：层叠环塔 + 立体晶格 + 符文环（厚土盾 / 玄冰盾 / 金刚护体）。 */
        GUARD_AURA("护体"),
        /** 治疗：花瓣旋涡 + 上升光柱 + 符文环（青木生 / 回春术 / 春风化雨）。 */
        HEAL_BLOOM("回春"),
        /** 奇点：虚空裂缝 + 圆环体 + 阵法 + 环绕（太虚黑洞 / 万象寂灭 / 鸿蒙一气）。 */
        VOID_SINGULARITY("奇点"),
        /** 神识：螺旋上升 + 神识环 + 裂隙（神识震 / 心魔引）。 */
        SPIRIT_BURST("神识"),
        /** 天陨：燃烧陨石 + 陨石轨迹 + 碎片 + 爬行裂纹（天陨星落）。 */
        STAR_FALL("天陨"),
        /** 斩星：金色巨剑 + 横扫光柱 + 深渊裂缝（斩星）。 */
        MOON_SLASH("斩星"),
        /** 轨道炮：光柱扫过 + 贯穿光柱（轨道炮击）。 */
        RAILGUN("轨道"),
        /** 剑域：立体晶格剑阵 + 层叠环塔（剑域 / 剑罡阵）。 */
        GOLD_DOMAIN("剑域"),
        /** 缚藤：双螺旋 + 环塔 + 花瓣（万木困锁）。 */
        VINE_BIND("缚藤");

        private final String cn;

        Setpiece(String cn) {
            this.cn = cn;
        }

        public String cn() {
            return cn;
        }
    }

    /**
     * 一门法术的视觉身份（不可变数据行）。
     *
     * @param key           Technique 枚举名（表的主键）
     * @param identity      0~5，释放主形状相位
     * @param layers        2~5，释放层数
     * @param sizeStep      0~2，尺寸档
     * @param phase         0~5，相位偏置
     * @param motifs        蓄力三图案（固定顺序）
     * @param chargeLayers  3~6，蓄力层数
     * @param spin          +1/-1，蓄力旋向
     * @param paletteShift  0~3，调色板偏移
     * @param pitch         施法音变调
     * @param setpiece      专属演出
     */
    public record Look(String key, int identity, int layers, int sizeStep, int phase,
                       SpellSignature.Motif[] motifs, int chargeLayers, int spin,
                       int paletteShift, float pitch, Setpiece setpiece) {

        /** 尺寸倍率（与 {@code SpellFxRuntime} 同口径）。 */
        public float sizeMul() {
            return sizeStep == 0 ? 0.75F : (sizeStep == 1 ? 1.0F : 1.35F);
        }

        public SpellSignature.Motif motifA() {
            return motifs[0];
        }

        public SpellSignature.Motif motifB() {
            return motifs[1];
        }

        public SpellSignature.Motif motifC() {
            return motifs[2];
        }
    }

    // ------------------------------------------------------------------
    // 一、数据表
    // ------------------------------------------------------------------

    /** 主键 = Technique 枚举名（职业主动在并入枚举前也先登记，键同未来枚举名）。 */
    private static final Map<String, Look> TABLE = new LinkedHashMap<>();

    private SpellLook() {
    }

    /** 一行（基础功法，按 {Technique} 查）。 */
    private static void add(Technique t, int id, int layers, int sizeStep, int phase,
                            SpellSignature.Motif a, SpellSignature.Motif b,
                            SpellSignature.Motif c, int chargeLayers, int spin,
                            int paletteShift, float pitch, Setpiece setpiece) {
        TABLE.put(t.name(), new Look(t.name(), id, layers, sizeStep, phase,
                new SpellSignature.Motif[]{a, b, c}, chargeLayers, spin,
                paletteShift, pitch, setpiece));
    }

    /** 一行（职业主动：现在还没进 Technique，用未来的枚举名登记）。 */
    private static void add(String key, int id, int layers, int sizeStep, int phase,
                            SpellSignature.Motif a, SpellSignature.Motif b,
                            SpellSignature.Motif c, int chargeLayers, int spin,
                            int paletteShift, float pitch, Setpiece setpiece) {
        TABLE.put(key, new Look(key, id, layers, sizeStep, phase,
                new SpellSignature.Motif[]{a, b, c}, chargeLayers, spin,
                paletteShift, pitch, setpiece));
    }

    private static final SpellSignature.Motif RING = SpellSignature.Motif.RING;
    private static final SpellSignature.Motif SPIKE = SpellSignature.Motif.SPIKE;
    private static final SpellSignature.Motif PILLAR = SpellSignature.Motif.PILLAR;
    private static final SpellSignature.Motif RAIN = SpellSignature.Motif.RAIN;
    private static final SpellSignature.Motif SPIRAL = SpellSignature.Motif.SPIRAL;
    private static final SpellSignature.Motif VINE = SpellSignature.Motif.VINE;
    private static final SpellSignature.Motif FUNNEL = SpellSignature.Motif.FUNNEL;
    private static final SpellSignature.Motif GLYPH = SpellSignature.Motif.GLYPH;

    static {
        // ==============================================================
        // 基础 52 门 · 金
        // ==============================================================
        // 金刚指：一指凝金线 —— 尖端聚集 + 符环 + 收束，释放走「贯穿」
        add(Technique.JIN_GANG_QIAN, 3, 5, 0, 0,
                SPIKE, GLYPH, FUNNEL, 4, -1, 1, 1.06F, Setpiece.PIERCE_LINE);
        // 万剑归宗：万千剑影 —— 剑柱 + 尖刺 + 收束，释放走「剑雨」
        add(Technique.WAN_JIAN_GUI_ZONG, 4, 5, 1, 2,
                PILLAR, SPIKE, FUNNEL, 5, 1, 2, 1.02F, Setpiece.SWORD_FAN);
        // 金虹遁：化虹 —— 漏斗 + 环 + 螺旋，释放走「残影」
        add(Technique.JIN_HONG_DUN, 0, 4, 0, 0,
                FUNNEL, RING, SPIRAL, 5, 1, 2, 1.08F, Setpiece.AFTERIMAGE);

        // ---- 木 ----
        // 青木生：草木自愈 —— 环 + 光柱 + 藤，释放走「回春」
        add(Technique.QING_MU_SHENG, 2, 4, 0, 0,
                RING, PILLAR, VINE, 3, 1, 0, 0.96F, Setpiece.HEAL_BLOOM);
        // 万木困锁：藤蔓缚敌 —— 藤 + 环 + 符，释放走「缚藤」
        add(Technique.WAN_MU_KUN_SUO, 2, 3, 0, 1,
                VINE, RING, GLYPH, 5, 1, 2, 0.98F, Setpiece.VINE_BIND);
        // 春风化雨：灵雨 —— 藤 + 尖 + 环，释放走「回春」
        add(Technique.CHUN_FENG_HUA_YU, 5, 4, 0, 3,
                VINE, SPIKE, RING, 4, -1, 1, 0.98F, Setpiece.HEAL_BLOOM);

        // ---- 水 ----
        // 寒冰箭：凝水成冰 —— 尖 + 雨 + 环，命中走「冰牢」
        add(Technique.HAN_BING_JIAN, 3, 5, 1, 3,
                SPIKE, RAIN, RING, 4, -1, 1, 1.06F, Setpiece.FROST_PRISON);
        // 冰封诀：方圆冰狱 —— 柱 + 漏斗 + 雨，释放走「冰牢」
        add(Technique.BING_FENG_JUE, 4, 4, 0, 1,
                PILLAR, FUNNEL, RAIN, 5, 1, 2, 1.00F, Setpiece.FROST_PRISON);
        // 千里冰封：冰封千里 —— 符 + 螺旋 + 漏斗，释放走「冰牢」
        add(Technique.QIAN_LI_BING_FENG, 1, 5, 1, 3,
                GLYPH, SPIRAL, FUNNEL, 6, -1, 3, 0.94F, Setpiece.FROST_PRISON);

        // ---- 火 ----
        // 烈焰掌：扑面烈焰 —— 螺旋 + 符 + 漏斗，释放走「火海」
        add(Technique.LIE_YAN_ZHANG, 0, 3, 2, 2,
                SPIRAL, GLYPH, FUNNEL, 3, 1, 0, 1.06F, Setpiece.FLAME_STORM);
        // 焚天诀：以身为薪 —— 藤 + 尖 + 符，释放走「火海」
        add(Technique.FEN_TIAN_JUE, 5, 3, 2, 5,
                VINE, SPIKE, GLYPH, 4, -1, 1, 0.96F, Setpiece.FLAME_STORM);
        // 焚寂燎原：原野成灰 —— 螺旋 + 环 + 雨，释放走「火海」
        add(Technique.FEN_JI_LIAO_YUAN, 3, 3, 2, 3,
                SPIRAL, RING, RAIN, 4, -1, 1, 1.00F, Setpiece.FLAME_STORM);

        // ---- 土 ----
        // 厚土盾：地气加身 —— 环 + 螺旋 + 柱，释放走「护体」
        add(Technique.HOU_TU_DUN, 0, 3, 2, 4,
                RING, SPIRAL, PILLAR, 3, 1, 0, 0.92F, Setpiece.GUARD_AURA);
        // 落地生根：震动四野 —— 符 + 环 + 柱，释放走「震地」
        add(Technique.LUO_DI_SHEN_ZHEN, 2, 5, 1, 4,
                GLYPH, RING, PILLAR, 6, -1, 3, 0.96F, Setpiece.QUAKE);
        // 山河印：山岳倾颓 —— 尖 + 柱 + 符，释放走「震地」
        add(Technique.SHAN_HE_YIN, 2, 4, 2, 3,
                SPIKE, PILLAR, GLYPH, 4, -1, 1, 1.00F, Setpiece.QUAKE);

        // ---- 冰 ----
        // 玄冰盾：万载玄冰 —— 雨 + 尖 + 螺旋，释放走「护体」
        add(Technique.XUAN_BING_DUN, 1, 4, 2, 0,
                RAIN, SPIKE, SPIRAL, 6, -1, 3, 0.94F, Setpiece.GUARD_AURA);
        // 冰魂斩：血凝不流 —— 环 + 符 + 尖，命中走「冰牢」
        add(Technique.BING_HUN_ZHAN, 2, 4, 2, 2,
                RING, GLYPH, SPIKE, 3, 1, 0, 1.04F, Setpiece.FROST_PRISON);

        // ---- 雷 ----
        // 紫霄雷法：引雷入体 —— 柱 + 螺旋 + 符，释放走「雷牢」
        add(Technique.ZI_XIAO_LEI_FA, 1, 3, 1, 5,
                PILLAR, SPIRAL, GLYPH, 5, 1, 2, 1.08F, Setpiece.THUNDER_CAGE);
        // 九霄雷狱：无处可逃 —— 柱 + 尖 + 符，释放走「雷牢」
        add(Technique.JIU_XIAO_LEI_YU, 2, 4, 2, 3,
                PILLAR, SPIKE, GLYPH, 5, 1, 2, 1.00F, Setpiece.THUNDER_CAGE);

        // ---- 风 ----
        // 风刃斩：风凝成刃 —— 漏斗 + 尖 + 螺旋，释放走「剑雨」
        add(Technique.FENG_REN_ZHAN, 0, 4, 2, 0,
                FUNNEL, SPIKE, SPIRAL, 5, 1, 2, 1.10F, Setpiece.SWORD_FAN);
        // 轻风诀：身轻如风 —— 尖 + 漏斗 + 螺旋，释放走「护体」
        add(Technique.QING_FENG_JUE, 1, 3, 1, 5,
                SPIKE, FUNNEL, SPIRAL, 4, -1, 1, 1.04F, Setpiece.GUARD_AURA);
        // 风影步：风行无影 —— 环 + 漏斗 + 螺旋，释放走「残影」
        add(Technique.FENG_YING_BU, 4, 5, 2, 5,
                RING, FUNNEL, SPIRAL, 5, 1, 2, 1.10F, Setpiece.AFTERIMAGE);

        // ---- 无 ----
        // 血魔噬：噬血补身 —— 漏斗 + 螺旋 + 符，释放走「摄魂」
        add(Technique.XUE_MO_SHI, 2, 4, 2, 4,
                FUNNEL, SPIRAL, GLYPH, 5, 1, 2, 0.90F, Setpiece.SOUL_DRAIN);
        // 神识震：直击魂魄 —— 符 + 螺旋 + 尖，释放走「神识」
        add(Technique.SHEN_SHI_ZHEN, 5, 3, 0, 3,
                GLYPH, SPIRAL, SPIKE, 6, -1, 3, 1.02F, Setpiece.SPIRIT_BURST);
        // 鸿蒙一气：开天辟地 —— 环 + 螺旋 + 符，释放走「奇点」
        add(Technique.HONG_MENG_YI_QI, 4, 5, 1, 5,
                RING, SPIRAL, GLYPH, 3, 1, 0, 0.90F, Setpiece.VOID_SINGULARITY);

        // ==============================================================
        // 追加 30 门（原"法术栏扩充"）
        // ==============================================================
        // 御剑斩：剑随心动 —— 尖 + 符 + 柱，释放走「贯穿」
        add(Technique.YU_JIAN_ZHAN, 3, 3, 2, 2,
                SPIKE, GLYPH, PILLAR, 6, -1, 3, 1.00F, Setpiece.PIERCE_LINE);
        // 雷罚：代天行罚 —— 柱 + 符 + 尖，释放走「雷牢」
        add(Technique.LEI_FA, 0, 4, 1, 4,
                PILLAR, GLYPH, SPIKE, 5, 1, 2, 1.10F, Setpiece.THUNDER_CAGE);
        // 冰封千里：万里雪飘 —— 符 + 尖 + 柱，释放走「冰牢」
        add(Technique.BING_FENG_QIAN_LI, 1, 4, 0, 2,
                GLYPH, SPIKE, PILLAR, 6, -1, 3, 0.92F, Setpiece.FROST_PRISON);
        // 焚天：天亦为炉 —— 螺旋 + 柱 + 雨，释放走「火海」
        add(Technique.FEN_TIAN, 2, 4, 1, 5,
                SPIRAL, PILLAR, RAIN, 5, 1, 2, 0.95F, Setpiece.FLAME_STORM);
        // 移形换影：影随形动 —— 环 + 漏斗 + 螺旋，释放走「残影」
        add(Technique.YI_XING_HUAN_YING, 4, 4, 1, 2,
                RING, FUNNEL, SPIRAL, 3, 1, 0, 1.08F, Setpiece.AFTERIMAGE);
        // 金刚护体：万法不侵 —— 环 + 柱 + 符，释放走「护体」
        add(Technique.JIN_GANG_HU_TI, 5, 5, 2, 3,
                RING, PILLAR, GLYPH, 6, -1, 3, 0.94F, Setpiece.GUARD_AURA);
        // 回春术：枯木逢春 —— 藤 + 环 + 柱，释放走「回春」
        add(Technique.HUI_CHUN_SHU, 2, 3, 2, 4,
                VINE, RING, PILLAR, 5, 1, 2, 0.98F, Setpiece.HEAL_BLOOM);
        // 摄魂：魂为所摄 —— 漏斗 + 螺旋 + 符，释放走「摄魂」
        add(Technique.SHE_HUN, 2, 3, 1, 3,
                FUNNEL, SPIRAL, GLYPH, 5, 1, 2, 0.92F, Setpiece.SOUL_DRAIN);

        // ---- 13 门被动（不释放，但蓄力签名仍要唯一；面板/图鉴会读）----
        add(Technique.LING_HAI_WU_LIANG, 2, 5, 1, 5,
                SPIRAL, SPIKE, PILLAR, 3, 1, 0, 0.95F, Setpiece.NONE);
        add(Technique.TU_NA_JUE, 0, 2, 1, 4,
                SPIRAL, VINE, RING, 3, 1, 0, 0.95F, Setpiece.NONE);
        add(Technique.JIN_GANG_TI, 5, 4, 0, 5,
                SPIKE, GLYPH, FUNNEL, 4, -1, 1, 0.95F, Setpiece.NONE);
        add(Technique.JI_FENG_BU, 0, 4, 1, 1,
                PILLAR, FUNNEL, GLYPH, 5, 1, 2, 1.00F, Setpiece.NONE);
        add(Technique.JIAN_XIN_TONG_MING, 3, 3, 2, 3,
                SPIKE, GLYPH, PILLAR, 6, -1, 3, 1.00F, Setpiece.NONE);
        add(Technique.WU_XING_TIAO_HE, 3, 5, 0, 2,
                VINE, SPIRAL, FUNNEL, 4, -1, 1, 0.95F, Setpiece.NONE);
        add(Technique.SHEN_SHI_NING_LIAN, 3, 5, 2, 4,
                SPIKE, SPIRAL, FUNNEL, 4, -1, 1, 1.00F, Setpiece.NONE);
        add(Technique.QI_XUE_CHONG_YING, 1, 5, 2, 5,
                RAIN, PILLAR, RING, 6, -1, 3, 0.95F, Setpiece.NONE);
        add(Technique.XUE_MO_GONG, 3, 3, 2, 1,
                GLYPH, SPIKE, RAIN, 6, -1, 3, 0.92F, Setpiece.NONE);
        add(Technique.PO_JUN_JUE, 0, 4, 2, 4,
                FUNNEL, SPIKE, PILLAR, 5, 1, 2, 1.00F, Setpiece.NONE);
        add(Technique.GUI_XI_GONG, 5, 2, 0, 5,
                RAIN, GLYPH, FUNNEL, 6, -1, 3, 0.90F, Setpiece.NONE);
        add(Technique.LING_BO_WEI_BU, 4, 4, 2, 2,
                RING, SPIKE, SPIRAL, 3, 1, 0, 1.02F, Setpiece.NONE);
        add(Technique.ZHAN_YI_FEI_TENG, 4, 5, 2, 5,
                PILLAR, SPIKE, RING, 5, 1, 2, 1.00F, Setpiece.NONE);

        // ---- 6 门灭世 / 超位 ----
        // 天陨星落：天外星陨 —— 螺旋 + 环 + 符，释放走「天陨」
        add(Technique.TIAN_YUN_XING_LUO, 0, 5, 1, 1,
                SPIRAL, RING, GLYPH, 6, -1, 3, 0.90F, Setpiece.STAR_FALL);
        // 轨道炮击：天顶一线 —— 尖 + 柱 + 藤，释放走「轨道」
        add(Technique.GUI_DAO_PAO_JI, 3, 3, 1, 5,
                SPIKE, PILLAR, VINE, 4, -1, 1, 1.06F, Setpiece.RAILGUN);
        // 万象寂灭：万象归寂 —— 柱 + 环 + 符，释放走「奇点」
        add(Technique.WAN_XIANG_JI_MIE, 2, 4, 0, 2,
                PILLAR, RING, GLYPH, 5, 1, 2, 0.90F, Setpiece.VOID_SINGULARITY);
        // 太虚黑洞：连光都逃不出 —— 漏斗 + 尖 + 螺旋，释放走「奇点」
        add(Technique.TAI_XU_HEI_DONG, 5, 5, 0, 2,
                FUNNEL, SPIKE, SPIRAL, 5, 1, 2, 0.88F, Setpiece.VOID_SINGULARITY);
        // 斩星：一剑开天 —— 尖 + 漏斗 + 符，释放走「斩星」
        add(Technique.ZHAN_XING, 2, 4, 1, 4,
                SPIKE, FUNNEL, GLYPH, 5, 1, 2, 1.00F, Setpiece.MOON_SLASH);
        // 焚天煮海：焚尽苍穹 —— 螺旋 + 漏斗 + 雨，释放走「火海」
        add(Technique.FEN_TIAN_ZHU_HAI, 4, 4, 1, 5,
                SPIRAL, FUNNEL, RAIN, 6, -1, 3, 0.90F, Setpiece.FLAME_STORM);

        // ==============================================================
        // 职业主动（《07-法术内容交接表》）：18 条新增条目，先登记视觉身份；
        // 另有 2 条「复用已有」——剑修·万剑归宗 = WAN_JIAN_GUI_ZONG、
        // 体修·金刚护体 = JIN_GANG_HU_TI，直接沿用上面基础行，不另登记。
        // 并入 Technique 后即自动生效。
        // ==============================================================
        // ---- 剑修 ----
        add("JIAN_QI_ZHAN", 3, 4, 1, 2,
                SPIKE, GLYPH, PILLAR, 5, 1, 2, 1.04F, Setpiece.PIERCE_LINE);
        add("JIAN_YU_SHU", 2, 4, 1, 1,
                RING, FUNNEL, SPIRAL, 4, -1, 2, 1.08F, Setpiece.AFTERIMAGE);
        add("JIAN_YU", 4, 5, 2, 0,
                PILLAR, SPIKE, RING, 5, 1, 2, 1.00F, Setpiece.GOLD_DOMAIN);

        // ---- 血修 ----
        add("XUE_DUN", 0, 4, 1, 4,
                FUNNEL, RING, SPIRAL, 4, -1, 3, 0.94F, Setpiece.AFTERIMAGE);
        add("XUE_BAO", 2, 4, 2, 2,
                GLYPH, SPIRAL, RING, 5, 1, 3, 0.92F, Setpiece.BLOOD_POOL);
        add("XUE_SHI", 3, 4, 2, 5,
                FUNNEL, GLYPH, SPIKE, 5, 1, 3, 0.90F, Setpiece.SOUL_DRAIN);
        add("XUE_HE", 1, 3, 2, 3,
                RING, VINE, GLYPH, 6, -1, 3, 0.90F, Setpiece.BLOOD_POOL);

        // ---- 魔修 ----
        add("MO_QI_BAO_FA", 5, 5, 2, 1,
                PILLAR, SPIRAL, GLYPH, 6, -1, 3, 0.86F, Setpiece.THUNDER_CAGE);
        add("MO_SHI_HUN", 2, 4, 1, 4,
                FUNNEL, SPIRAL, RAIN, 5, 1, 2, 0.92F, Setpiece.SOUL_DRAIN);
        add("MO_YING_DUN", 5, 3, 1, 2,
                RING, FUNNEL, GLYPH, 4, -1, 3, 0.90F, Setpiece.AFTERIMAGE);
        add("MO_XIN_MO_YIN", 2, 4, 1, 5,
                SPIRAL, FUNNEL, GLYPH, 5, 1, 3, 0.88F, Setpiece.SPIRIT_BURST);

        // ---- 鬼修 ----
        add("GUI_SUO_HUN_LIAN", 1, 4, 1, 3,
                GLYPH, SPIKE, RING, 5, 1, 2, 0.96F, Setpiece.PIERCE_LINE);
        add("GUI_YU_ZHAN_KAI", 3, 5, 2, 4,
                RING, SPIRAL, GLYPH, 6, -1, 3, 0.90F, Setpiece.VOID_SINGULARITY);
        add("GUI_XU_HUA", 0, 3, 1, 0,
                FUNNEL, RING, SPIRAL, 3, 1, 2, 0.95F, Setpiece.AFTERIMAGE);
        add("GUI_HUN_BAO", 2, 4, 2, 1,
                PILLAR, SPIKE, GLYPH, 5, 1, 3, 0.94F, Setpiece.SOUL_DRAIN);

        // ---- 体修 ----
        add("TI_ZHEN_DANG_BO", 4, 4, 2, 2,
                GLYPH, RING, PILLAR, 5, 1, 2, 0.94F, Setpiece.QUAKE);
        add("TI_BENG_SHAN_JI", 2, 4, 2, 4,
                SPIKE, PILLAR, GLYPH, 5, 1, 2, 0.98F, Setpiece.QUAKE);
        add("TI_BU_MIE_JIN_SHEN", 3, 4, 2, 5,
                RING, VINE, GLYPH, 5, 1, 2, 0.96F, Setpiece.HEAL_BLOOM);
    }

    // ------------------------------------------------------------------
    // 二、查询
    // ------------------------------------------------------------------

    /**
     * 取一门法术的视觉身份。
     *
     * <p>表里没有 → 用旧的哈希推导兜底（保证任何新法术都不会"没有特效"）。</p>
     */
    public static Look of(Technique technique) {
        if (technique == null) {
            return null;
        }
        Look look = TABLE.get(technique.name());
        return look != null ? look : synthesize(technique.name());
    }

    /** 按枚举名取（蓄力链路上 key 就是枚举名）。未登记返回 {@code null}。 */
    public static Look byKey(String key) {
        return key == null ? null : TABLE.get(key);
    }

    /** 按枚举名取，未登记时用哈希推导兜底。 */
    public static Look ofKey(String key) {
        if (key == null) {
            return null;
        }
        Look look = TABLE.get(key);
        return look != null ? look : synthesize(key);
    }

    /** 这门法术的视觉身份是否**已人工定妆**（false = 走哈希兜底）。 */
    public static boolean authored(Technique technique) {
        return technique != null && TABLE.containsKey(technique.name());
    }

    /** 已定妆的键（顺序 = 表顺序）。 */
    public static Collection<String> keys() {
        return Collections.unmodifiableSet(TABLE.keySet());
    }

    /** 已定妆行数。 */
    public static int size() {
        return TABLE.size();
    }

    // ------------------------------------------------------------------
    // 三、兜底：未登记法术沿用旧的哈希推导（行为与改造前一致）
    // ------------------------------------------------------------------

    private static Look synthesize(String key) {
        int h = key.hashCode();
        int identity = Math.floorMod(h, 6);
        int layers = 2 + Math.floorMod(h / 3, 4);
        int sizeStep = Math.floorMod(h / 11, 3);
        int phase = Math.floorMod(h / 17, 6);
        return new Look(key, identity, layers, sizeStep, phase,
                SpellSignature.motifsOf(key), SpellSignature.layersOf(key),
                SpellSignature.spinOf(key), Math.floorMod(h, 4), 1.0F,
                Setpiece.NONE);
    }

    // ------------------------------------------------------------------
    // 四、给验证器用的计数/摘要（不参与运行期表现）
    // ------------------------------------------------------------------

    /** 某个专属演出被多少门法术使用。 */
    public static int countOf(Setpiece setpiece) {
        int n = 0;
        for (Look look : TABLE.values()) {
            if (look.setpiece() == setpiece) {
                n++;
            }
        }
        return n;
    }

    /** 元素 → 这门元素里用了哪些调色偏移（验证"同元素不撞色"）。 */
    public static int paletteDistinct(Element element) {
        java.util.Set<Integer> set = new java.util.HashSet<>();
        for (Look look : TABLE.values()) {
            Technique t = Technique.byName(look.key());
            if (t != null && t.element() == element) {
                set.add(look.paletteShift());
            }
        }
        return set.size();
    }
}
