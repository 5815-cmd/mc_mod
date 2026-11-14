package com.hongmeng.xiuxian.talent;

import net.minecraft.network.chat.Component;

/**
 * <b>副业</b>—— 玩家要求「阵法师变成副业吧，阵法和炼丹还有炼器什么的都可以兼修」。
 *
 * <h2>主修 vs 副业</h2>
 * <table border="1">
 *   <caption>两层结构</caption>
 *   <tr><th></th><th>主修（{@link Path}）</th><th>副业（本类）</th></tr>
 *   <tr><td>数量</td><td>只能选 <b>1</b> 条</td><td>可以<b>同时修多门</b></td></tr>
 *   <tr><td>作用</td><td>战斗风格</td><td>生产与辅助</td></tr>
 *   <tr><td>成长</td><td>天赋树加点</td><td>独立熟练度等级</td></tr>
 *   <tr><td>冲突</td><td>与其它主修互斥</td><td><b>互不冲突</b></td></tr>
 * </table>
 *
 * <h2>为什么副业要有独立熟练度，而不是也用天赋点</h2>
 * <p>天赋点是<b>有限</b>资源，如果副业也吃天赋点，那么"我想炼丹"就要和
 * "我想变强"抢点数 —— 玩家会理性地永远不点副业。</p>
 * <p>参考作品里作坊类系统的通行做法是<b>熟能生巧</b>：做得越多越熟练。
 * 所以这里用独立熟练度：<b>你用哪门副业，哪门就涨</b>，
 * 于是"兼修"是自然的（多花时间就多会），而不是零和的。</p>
 *
 * <h2>阵法为什么归副业</h2>
 * <p>玩家明确要求「阵法师变成副业」。原本它是一条主修（阵修），
 * 但那样"想布阵"就得放弃"想当法修" —— 而修仙题材里
 * 阵法、炼丹、炼器本来就都是<b>手艺</b>，跟你是剑修还是法修不冲突。</p>
 * <p>注意：主修里仍有"阵修"（战斗向的领域与剑阵强化），
 * 副业里的"阵法"是<b>制作向</b>的（布阵盘、阵旗、传送阵）——
 * 两者<b>互相加成</b>，阵修练阵法副业会更快。</p>
 */
public enum Profession {

    /** 阵法：布阵盘、阵旗、结界。 */
    ZHENFA("阵法", 'a', "布阵 · 制作阵盘阵旗、布置结界与传送阵",
            "阵盘", "阵旗"),
    /** 炼丹：丹药。 */
    LIANDAN("炼丹", '6', "炼丹 · 以灵草炼丹药，辅助修炼与战斗",
            "丹炉", "灵草"),
    /** 炼器：法器。 */
    LIANQI("炼器", 'b', "炼器 · 锻造法器、飞剑与护具",
            "锻炉", "器胚");

    private final String cn;
    private final char color;
    private final String desc;
    private final String mainTool;
    private final String mainMat;

    Profession(String cn, char color, String desc, String mainTool, String mainMat) {
        this.cn = cn;
        this.color = color;
        this.desc = desc;
        this.mainTool = mainTool;
        this.mainMat = mainMat;
    }

    public String cn() {
        return this.cn;
    }

    public String desc() {
        return this.desc;
    }

    public Component colored() {
        return Component.literal("\u00a7" + this.color + this.cn);
    }

    /** 主要工具名（用于提示与将来的物品）。 */
    public String mainTool() {
        return this.mainTool;
    }

    /** 主要材料名。 */
    public String mainMat() {
        return this.mainMat;
    }

    /** 最高熟练度等级。 */
    public static int maxRank() {
        return 10;
    }

    /**
     * 各等级需要的<b>累计</b>干活次数（索引 = 等级）。
     *
     * <p>⚠ 旧公式是递归的 {@code prev + 3 + (rank-1)*2}，单级
     * 3/8/15/24/35/48/63/80/99/120 —— <b>累计 495 次</b>才能顶满 10 级。
     * 对一个"副业"来说太重了（要肝好几天），所以改成查表。</p>
     *
     * <p>新曲线：前三级极快（3/5/8，几分钟就见效），后三级仍有分量
     * （20/23/25），总量 <b>140</b> —— 约旧值的 28%。</p>
     *
     * <p>为什么改成查表：递归公式<b>看不出总量</b>（要手算 10 层），
     * 查表之后总量直接读得到，也能被断言校验。</p>
     */
    private static final int[] CUMULATIVE = {
            0, 3, 8, 16, 26, 39, 54, 72, 92, 115, 140,
    };

    /**
     * 达到某等级需要的<b>累计</b>干活次数。
     *
     * @param rank 目标等级（1 {@link #maxRank()}）
     */
    public static int workNeeded(int rank) {
        int r = Math.max(1, Math.min(maxRank(), rank));
        return CUMULATIVE[r];
    }

    /**
     * 顶满熟练度需要的总次数（给界面/断言用）。
     *
     * <p>原来这个数字要手算，而且改完曲线很容易忘；
     * 现在它就是一个可读的常量。</p>
     */
    public static int totalToMax() {
        return CUMULATIVE[maxRank()];
    }

    /** 各等级的效果：产出数量 / 品质加成。 */
    public static double yieldBonus(int rank) {
        return rank * 0.06D;          // 每级 +6% 产出
    }

    /** 各等级的失败率削减。 */
    public static double failCut(int rank) {
        return Math.min(0.85D, rank * 0.09D);
    }

    /** 按名字找。 */
    public static Profession byName(String name) {
        if (name == null) {
            return null;
        }
        for (Profession p : values()) {
            if (p.name().equalsIgnoreCase(name) || p.cn.equals(name)) {
                return p;
            }
        }
        return null;
    }

    public static String allNames() {
        StringBuilder sb = new StringBuilder();
        for (Profession p : values()) {
            if (sb.length() > 0) {
                sb.append("\u00a77 / ");
            }
            sb.append(p.colored().getString());
        }
        return sb.toString();
    }
}
