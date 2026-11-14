package com.hongmeng.xiuxian.talent;

import net.minecraft.network.chat.Component;

/**
 * <b>修炼路线（道途）</b>—— 玩家要求的"法修 / 体修 / 魔修 / 血修"，以及"阵法"等副业。
 *
 * <h2>参考了哪些作品（不闭门造车）</h2>
 * <ol>
 *   <li><b>ACME 修仙传</b>：有<b>体质选择</b>与<b>出生种族选择</b>
 *       （人 / 妖 / 仙 / 魔 / 散修），而且<b>种族的开局功法不同</b>。</li>
 *   <li><b>凡人修仙人界篇</b>的体质系统：<b>几乎全是加成与专精，不给惩罚</b>
 *       —— 平庸之体没有任何附加、三阳之体修炼更快、
 *       锻金之体金属性功法修炼需求 −20%、火灵之体火属性 −20%……
 *       只有"龙吟之体"这种特殊体质才带副作用（扣寿元）。</li>
 * </ol>
 *
 * <h2>⚠ 我因此改掉了自己上一版的设计</h2>
 * <p>我上一版给每条道途配了"代价"（法修护甲 ×0.85、体修法术 ×0.75……）。
 * 玩家明确说：</p>
 * <blockquote>
 * 「要平衡均衡，可以不用有原本的代价，<b>区别可以在天赋加点上区分出来就行</b>，
 * 他们的天赋技能加成什么的」
 * </blockquote>
 * <p>这与参考作品的做法一致 —— <b>体质/道途负责"你擅长什么"，不负责"你哪里残废"</b>。
 * 所以现在：</p>
 * <ul>
 *   <li>{@link #multipliers()} <b>全部 ≥ 1.0</b>，没有任何一项低于 1；</li>
 *   <li>差异体现在 <b>{@link #bonus()}：专属加成</b>
 *       （例如血修自带吸血、魔修自带破防）—— 是"多给你一样"，不是"拿走你一样"；</li>
 *   <li>真正的区分靠<b>天赋树</b>（{@link TalentTree}）：点数有限、树有分支，
 *       所以两条道途的玩家最终面板完全不同。</li>
 * </ul>
 *
 * <h2>主修与副业</h2>
 * <p>玩家要求「阵法师变成副业吧，阵法和炼丹还有炼器什么的都可以兼修」。</p>
 * <p>所以拆成两层：</p>
 * <ul>
 *   <li><b>主修（{@link Path}）</b>：只能选一条，决定战斗风格；</li>
 *   <li><b>副业（{@link Profession}）</b>：可以<b>同时修多门</b>，
 *       各自有独立等级 —— 阵法 / 炼丹 / 炼器。</li>
 * </ul>
 */
public enum Path {

    /** 法修：御法为先。 */
    FA("法修", 'b',
            "御法为先 · 法术伤害与灵力上限最高，天赋树偏法术与冷却",
            1.04D, 1.55D, 1.04D, 1.04D),
    /** 体修：以身为炉。 */
    TI("体修", '6',
            "以身为炉 · 生命与护甲最高，天赋树偏近战与抗击退",
            1.00D, 1.00D, 1.52D, 1.24D),
    /** 魔修：以魔证道。 */
    MO("魔修", '5',
            "以魔证道 · 伤害与破防最高，天赋树偏爆发",
            1.52D, 1.05D, 1.05D, 1.05D),
    /**
     * 血修：以血养功。
     *
     * <p>⚠ <b>主项是「生命」而不是「法术」</b> —— 这是我修正过的一处实质错误。
     * 参考《一念逍遥》的血修（血炼）：官方明确说它是「<b>物理系</b>修炼方向」，
     * 专属法宝是双刀 / 杵 / 弓 / 轮刃，神通把血液凝形为弓刀伞斧。</p>
     *
     * <p>我上一版把主项给了法术，那会让它变成"另一个法修"，
     * 与其它道途的区分度反而下降。血修的识别度在于
     * <b>越打越活、濒死反杀</b>，所以主项是生命。</p>
     */
    XUE("血修", 'c',
            "以血养功 · 物理近战，攻击吸血、击杀回血，越打越活",
            1.02D, 1.02D, 1.48D, 1.10D),
    /** 阵修：以阵困敌（与阵法副业互相加成）。 */
    ZHEN("阵修", 'a',
            "以阵困敌 · 领域与剑阵威力最高，天赋树偏范围与控制",
            1.06D, 1.48D, 1.06D, 1.06D);

    private final String cn;
    private final char color;
    private final String desc;
    private final double spellDamage;
    private final double manaMax;
    private final double health;
    private final double armor;

    Path(String cn, char color, String desc, double spellDamage, double manaMax,
         double health, double armor) {
        this.cn = cn;
        this.color = color;
        this.desc = desc;
        this.spellDamage = spellDamage;
        this.manaMax = manaMax;
        this.health = health;
        this.armor = armor;
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

    public double spellDamage() {
        return this.spellDamage;
    }

    public double manaMax() {
        return this.manaMax;
    }

    public double health() {
        return this.health;
    }

    public double armor() {
        return this.armor;
    }

    /** 四项乘数（验证器用来确认"没有一项低于 1"）。 */
    public double[] multipliers() {
        return new double[]{this.spellDamage, this.manaMax, this.health, this.armor};
    }

    /**
     * <b>道途的专属加成</b>——"多给你一样"，而不是拿走一样。
     *
     * <p>这是模仿"体质"的做法：每个体质都有自己的一条特殊效果。
     * 这里是纯文本描述，具体数值由 {@link #bonusValue()} 给出，
     * 供战斗结算读取。</p>
     */
    public String bonus() {
        return switch (this) {
            case FA -> "法术冷却 −8%";
            case TI -> "抗击退 +50%，受创减免 3%";
            case MO -> "无视目标护甲 8%";
            case XUE -> "物理攻击吸取 15% 生命，濒死时延迟结算 30% 伤害";
            case ZHEN -> "领域与剑阵威力 +30%";
        };
    }

    /** 专属加成的数值（含义由 {@link #bonusKind()} 决定）。 */
    public double bonusValue() {
        return switch (this) {
            case FA -> 0.08D;
            case TI -> 0.50D;
            case MO -> 0.08D;
            case XUE -> 0.15D;
            case ZHEN -> 0.30D;
        };
    }

    /** 专属加成的种类。 */
    public BonusKind bonusKind() {
        return switch (this) {
            case FA -> BonusKind.COOLDOWN;
            case TI -> BonusKind.KNOCKBACK_RESIST;
            case MO -> BonusKind.PENETRATION;
            case XUE -> BonusKind.LIFESTEAL;
            case ZHEN -> BonusKind.FORMATION;
        };
    }

    /** 专属加成的种类。 */
    public enum BonusKind {
        COOLDOWN, KNOCKBACK_RESIST, PENETRATION, LIFESTEAL, FORMATION
    }

    /** 这个道途推荐用什么剑型（与飞剑模型联动）。 */
    public String swordKind() {
        return switch (this) {
            case FA -> "SWORD";
            case TI -> "HEAVY";
            case MO -> "BONEBLADE";
            case XUE -> "BONEBLADE";
            case ZHEN -> "DAGGER";
        };
    }

    /** 道途主色（飞剑染色用，RGB 0~1）。 */
    public float[] rgb() {
        return switch (this) {
            case FA -> new float[]{0.55F, 0.82F, 1.00F};
            case TI -> new float[]{0.95F, 0.78F, 0.35F};
            case MO -> new float[]{0.62F, 0.20F, 0.85F};
            case XUE -> new float[]{0.90F, 0.13F, 0.18F};
            case ZHEN -> new float[]{0.35F, 0.95F, 0.65F};
        };
    }

    /** 按名字找（认中文名与枚举名）。 */
    public static Path byName(String name) {
        if (name == null) {
            return null;
        }
        for (Path p : values()) {
            if (p.name().equalsIgnoreCase(name) || p.cn.equals(name)) {
                return p;
            }
        }
        return null;
    }

    /** 全部主修的中文名。 */
    public static String allNames() {
        StringBuilder sb = new StringBuilder();
        for (Path p : values()) {
            if (sb.length() > 0) {
                sb.append("\u00a77 / ");
            }
            sb.append(p.colored().getString());
        }
        return sb.toString();
    }
}
