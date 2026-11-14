package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.talent.Path;
import net.minecraft.world.item.ArmorItem;

/**
 * <b>五条道途的专属护甲</b> —— 玩家要求「每种分类都要有独立的装备什么的」。
 *
 * <h2>设计原则（延续"无代价、靠侧重区分"）</h2>
 * <p>玩家前面明确说过：「要平衡均衡，可以不用有原本的代价，区别可以在天赋加点上
 * 区分出来就行」+「每种战力要平衡，还要有明显差别，侧重方面突出」。
 * 所以五套护甲遵循同一条规则：</p>
 * <ul>
 *   <li><b>总战力对齐</b>：五套的"护甲 + 韧性 + 生命"合计<b>完全相同</b>；
 *       差别只在<b>加成类型</b>上；</li>
 *   <li><b>各自强化本道途的机制</b>：法修加灵力、体修加生命、
 *       血修加吸血、阵修加领域、魔修加破防 —— 全是"给自己的强项再加一层"；</li>
 *   <li><b>不给任何减益</b>。</li>
 * </ul>
 *
 * <h2>为什么"总计相同"很重要</h2>
 * <p>如果某套的护甲值高一点、另一套低一点，玩家会立刻算出"哪套最强"，
 * 于是四套变成摆设。所以 {@link #armor()} / {@link #toughness()} /
 * {@link #health()} 三项之和刻意做成常量，只在
 * {@link #spellBonus()} 这类"道途专属加成"上分化。</p>
 */
public enum PathArmorTier {

    /** 法修 · 云纹法袍。 */
    FA(Path.FA, "云纹法袍", 1.00D, 0.00D, 4.0D),
    /** 体修 · 玄铁重甲。 */
    TI(Path.TI, "玄铁重甲", 1.00D, 0.00D, 4.0D),
    /** 魔修 · 骸骨魔铠。 */
    MO(Path.MO, "骸骨魔铠", 1.00D, 0.00D, 4.0D),
    /** 血修 · 血纹战衣。 */
    XUE(Path.XUE, "血纹战衣", 1.00D, 0.00D, 4.0D),
    /** 阵修 · 阵纹道袍。 */
    ZHEN(Path.ZHEN, "阵纹道袍", 1.00D, 0.00D, 4.0D);

    /** 境界上限（与 PathArmorTable.MAX_TIER 一致）。 */
    public static final int MAX_TIER = 10;

    private final Path path;
    private final String cn;
    private final double armorMul;
    private final double toughnessMul;
    private final double healthPerPiece;

    PathArmorTier(Path path, String cn, double armorMul, double toughnessMul,
                  double healthPerPiece) {
        this.path = path;
        this.cn = cn;
        this.armorMul = armorMul;
        this.toughnessMul = toughnessMul;
        this.healthPerPiece = healthPerPiece;
    }

    public Path path() {
        return this.path;
    }

    public String cn() {
        return this.cn;
    }

    /** 材质名（决定护甲图层贴图路径，见 {@code PathArmorMaterial}）。 */
    public String materialName() {
        return "path_armor_" + this.name().toLowerCase(java.util.Locale.ROOT);
    }

    // ------------------------------------------------------------------
    // 基础数值：五套**完全相同**（只在加成类型上分化）
    // ------------------------------------------------------------------

    /**
     * 单件护甲值。
     *
     * <p>参考原版：皮革 1/3/2/1、铁 2/6/5/2、钻石 3/8/6/3。
     * 这里取"略强于铁、略弱于钻石"，因为真正的强度来自道途加成与法宝。</p>
     */
    /**
     * 四件护甲的分配比例（头盔 / 胸甲 / 护腿 / 靴子）。
     *
     * <p>与 {@code DaoArmorTier} 用**同一套比例**（3/6/5/2 十六分）——
     * 这样"哪件给得多"的直觉与既有装备一致，玩家不用重新学。</p>
     */
    private static final double[] SHARE = {
            3.0D / 16.0D, 6.0D / 16.0D, 5.0D / 16.0D, 2.0D / 16.0D};

    /**
     * 道途护甲相对道袍的**防御折扣**。
     *
     * <p>道袍是 {@code standard(tier) × 0.70}（纯防御，无道途加成）；
     * 道途护甲目标取 {@code standard(tier) × 0.55}（约 78%），
     * 比道袍少约 22% 护甲，但带道途专属加成与 2/4 件套装效果。</p>
     *
     * <p>于是玩家面临一个<b>真实的选择</b>：要纯防御穿道袍，
     * 要机制加成穿道途护甲 —— 这正是"侧重"该有的样子。</p>
     *
     * <p>⚠ <b>这个常数是 0.55/0.70，不是 0.55。</b>
     * 因为 {@code RealmStats.armorTotal(tier)} <b>本身已经乘过 0.70</b>
     * （它就是"道袍的全套护甲值"）。我第一版直接写 0.55，
     * 实际得到的是 0.70 × 0.55 = 0.385 —— 只有道袍的 55%，
     * 而不是我想要的 78.6%。<b>这个偏差是断言抓出来的。</b></p>
     */
    private static final double TOTAL_FACTOR = 0.55D / 0.70D;

    public int defense(ArmorItem.Type type) {
        return defenseAt(1, type);
    }

    /**
     * <b>指定境界</b>的单件护甲值。
     *
     * <p>⚠ <b>这一版改过：线性 → 指数。</b></p>
     * <p>我第一版用线性放大（第 1 境 ×1.0 → 第 10 境 ×3.0，全套 57），
     * 但项目既有的十境道袍走的是<b>指数</b>曲线
     * （{@code RealmStats.armorTotal}：第 1 境 6 → 第 10 境 224 000）。
     * 两者差了 <b>4000 倍</b> —— 我做的 200 件护甲在后期等于没有防御，
     * 玩家只会穿道袍，道途护甲沦为装饰品。</p>
     *
     * <p>现在改用与道袍同源的 {@code RealmStats.armorTotal}，
     * 乘 {@link #TOTAL_FACTOR} 打个折（用道途加成补回来）。</p>
     */
    public int defenseAt(int tier, ArmorItem.Type type) {
        int i = indexOf(type);
        if (i < 0) {
            return 1;
        }
        double total = com.hongmeng.xiuxian.core.RealmStats.armorTotal(
                Math.max(1, Math.min(MAX_TIER, tier))) * TOTAL_FACTOR;
        // ⚠ 用**累积取整**而不是每件各自 round。
        //
        // 原因：第 1 境全套只有 4.4 点，四件各自 round 并夹到 ≥1 之后
        // 合计变成 5，比目标 4.4 高出 14% —— 于是"道途护甲是道袍的 78.6%"
        // 这条关系在低境界全被取整误差破坏（实测第 1 境变成 89%）。
        //
        // 累积取整保证**四件之和 == round(目标)**：
        //   piece_i = round(total × 累积比例) − round(total × 上一段累积比例)
        int target = Math.max(4, (int) Math.round(total));
        double cum = 0.0D;
        int prevBound = 0;
        for (int k = 0; k <= i; k++) {
            cum += SHARE[k];
            int bound = (int) Math.round(target * cum);
            if (k == i) {
                return Math.max(1, bound - prevBound);
            }
            prevBound = bound;
        }
        return Math.max(1, (int) Math.round(total * SHARE[i]));
    }

    /** 部位 -> SHARE 下标。 */
    private static int indexOf(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
        };
    }

    /** 全套护甲值（四件之和，含折扣）。 */
    public int totalDefenseAt(int tier) {
        int sum = 0;
        for (ArmorItem.Type t : new ArmorItem.Type[]{
                ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
                ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            sum += defenseAt(tier, t);
        }
        return sum;
    }

    /** 单件耐久（同样随境界放大，否则高阶护甲会先坏）。 */
    public int durability(ArmorItem.Type type) {
        return durabilityAt(1, type);
    }

    /** 指定境界的单件耐久。 */
    /**
     * 指定境界的单件耐久。
     *
     * <p>与护甲值同源：境界越高，耐久按 {@code RealmStats.standard} 的
     * 量级放大 —— 否则高阶护甲会因为"打几下就坏"而没人用。</p>
     *
     * <p>但耐久不能像护甲那样 ×224000（数字会溢出且没意义），
     * 所以取<b>平方根级</b>的温和放大：第 10 境约 ×18。</p>
     */
    public int durabilityAt(int tier, ArmorItem.Type type) {
        int base = switch (type) {
            case HELMET -> 13;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
        int t = Math.max(1, Math.min(MAX_TIER, tier));
        // standard 是指数表，开平方后变成温和增长：8→1.0，320000→18.6
        double std = com.hongmeng.xiuxian.core.RealmStats.standard(t);
        double scale = Math.sqrt(std / 8.0D);
        return Math.max(base * 26, (int) Math.round(base * 26 * scale));
    }

    /** 韧性（全套合计 2.0 —— 比钻石略低，留给法宝发挥空间）。 */
    public double toughness() {
        return 0.5D * this.toughnessMul;
    }

    /** 击退抗性（全套合计）。 */
    public double knockbackResistance() {
        return 0.025D;
    }

    /** 附魔能力。 */
    public int enchantmentValue() {
        return 18;
    }

    /** 单件提供的额外生命。 */
    public double healthPerPiece() {
        return this.healthPerPiece;
    }

    // ------------------------------------------------------------------
    // 道途专属加成（穿齐 4 件才吃满；见 PathArmorBonus）
    // ------------------------------------------------------------------

    /**
     * 这套护甲强化的"道途机制"。
     *
     * <p>数值刻意做得<b>温和</b>（穿齐也才百分之十几），
     * 因为它的作用是"让本道途的特色更明显"，而不是替代天赋树。</p>
     */
    public double spellBonus() {
        return this.path == Path.FA ? 0.12D : 0.0D;
    }

    public double manaBonus() {
        return this.path == Path.FA ? 0.18D : 0.0D;
    }

    public double meleeBonus() {
        return (this.path == Path.TI || this.path == Path.XUE) ? 0.10D : 0.0D;
    }

    public double armorBonus() {
        return this.path == Path.TI ? 0.08D : 0.0D;
    }

    public double lifestealBonus() {
        return this.path == Path.XUE ? 0.08D : 0.0D;
    }

    public double penetrationBonus() {
        return this.path == Path.MO ? 0.10D : 0.0D;
    }

    public double formationBonus() {
        return this.path == Path.ZHEN ? 0.20D : 0.0D;
    }

    // ------------------------------------------------------------------
    // 套装效果（2 件 / 4 件两档）
    // ------------------------------------------------------------------

    /**
     * <b>2 件套</b>的加成（小）。
     *
     * <p>设计意图：鼓励玩家"先凑两件"。如果只有 4 件套，
     * 集齐之前的小件完全没有额外价值，玩家会觉得"穿了也没用"。</p>
     *
     * @return 加成比例（含义由 {@link #setTwoKind()} 决定）
     */
    public double setTwoPiece() {
        return this.path == Path.TI ? 0.15D : 0.05D;
    }

    /** 2 件套强化的项目。 */
    public String setTwoKind() {
        return switch (this.path) {
            case FA -> "法术冷却缩减";
            case TI -> "抗击退";
            case MO -> "破防";
            case XUE -> "吸血";
            case ZHEN -> "领域范围";
        };
    }

    /**
     * <b>4 件套</b>的加成（强化本道途的核心机制）。
     *
     * <p>⚠ 刻意<b>不再加基础数值</b>：那与按件数线性缩放重复，
     * 叠加起来容易失控。这里全部强化<b>机制参数</b> ——
     * 改变的是手感，而不是数字。</p>
     */
    public double setFourPiece() {
        return switch (this.path) {
            // 法修：真元化盾更厚（法力能当更多护盾用）
            case FA -> 0.25D;
            // 体修：激荡攒得更快（挨同样多的打，攒更多能量）
            case TI -> 0.30D;
            // 魔修：魔气上限更高（能攒更多，爆发窗口更长）
            case MO -> 0.25D;
            // 血修：缓伤比例更高（能拖住更多伤害，反杀窗口更长）
            case XUE -> 0.40D;
            // 阵修：阵域能量上限更高（能开更久）
            case ZHEN -> 0.30D;
        };
    }

    /** 4 件套强化的是<b>哪一项机制</b>（给 tooltip 用）。 */
    public String setFourKind() {
        return switch (this.path) {
            case FA -> "真元护盾转化率";
            case TI -> "激荡能量获取";
            case MO -> "魔气上限";
            case XUE -> "缓伤转化比例";
            case ZHEN -> "阵域能量上限";
        };
    }

    /** 套装效果的一行说明（tooltip 用）。 */
    public String setText() {
        return String.format("2 件：%s +%d%%   4 件：%s +%d%%",
                setTwoKind(), pct(setTwoPiece()),
                setFourKind(), pct(setFourPiece()));
    }

    /** 一行说明（物品 tooltip 与提示用）。 */
    public String bonusText() {
        StringBuilder sb = new StringBuilder();
        if (spellBonus() > 0) {
            sb.append("法术伤害 +").append(pct(spellBonus())).append("%  ");
        }
        if (manaBonus() > 0) {
            sb.append("灵力上限 +").append(pct(manaBonus())).append("%  ");
        }
        if (meleeBonus() > 0) {
            sb.append("近战伤害 +").append(pct(meleeBonus())).append("%  ");
        }
        if (armorBonus() > 0) {
            sb.append("减伤 +").append(pct(armorBonus())).append("%  ");
        }
        if (lifestealBonus() > 0) {
            sb.append("吸血 +").append(pct(lifestealBonus())).append("%  ");
        }
        if (penetrationBonus() > 0) {
            sb.append("破防 +").append(pct(penetrationBonus())).append("%  ");
        }
        if (formationBonus() > 0) {
            sb.append("领域/剑阵 +").append(pct(formationBonus())).append("%  ");
        }
        sb.append("生命 +").append((int) (this.healthPerPiece * 4)).append("（穿齐）");
        // ⚠ 这里**不能**拼 setText()：
        //
        //   本方法的两个去向都不处理换行 ——
        //     · PathArmorBonus.summary() -> 面板的单行 drawString
        //     · 物品 tooltip 的单个 Component.literal
        //   `\n` 在两者里都会画成 notdef 豆腐块，后面的文字还会溢出面板。
        //   套装说明由调用方用 setText() 自己加一行（见 ModPathArmor 的 tooltip）。
        return sb.toString().trim();
    }

    private static int pct(double v) {
        return (int) Math.round(v * 100.0D);
    }

    /** 按道途找护甲。 */
    public static PathArmorTier of(Path path) {
        for (PathArmorTier t : values()) {
            if (t.path == path) {
                return t;
            }
        }
        return null;
    }
}
