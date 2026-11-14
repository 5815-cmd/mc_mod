package com.hongmeng.xiuxian.talent;

import net.minecraft.network.chat.Component;

/**
 * <b>职业（新五职业）</b>—— 剑修 / 血修 / 魔修 / 鬼修 / 体修。
 *
 * <h2>为什么新建本枚举，而不是改旧 {@link Path}</h2>
 * <p>旧 {@code Path} 的 5 个值（FA/TI/MO/XUE/ZHEN）<b>被 250 件已注册装备引用</b>：
 * 注册名、材质名、贴图路径全部由 {@code name()} 派生（{@code path_armor_ti_*}、
 * {@code path_weapon_spirit_sword_*}…）。改旧枚举的值名 = 改注册表 ID =
 * 老存档物品错位，所以旧枚举<b>整体冻结为 legacy</b>，新系统用本枚举。</p>
 *
 * <p>旧枚举只服务旧装备；新运行时（{@code Talents}/{@code CareerRuntime}/
 * {@code CareerMechanics}/{@code CareerTalentTree}/界面/网络包）一律用本枚举。</p>
 *
 * <h2>五维乘数</h2>
 * <p>旧 {@code Path.multipliers()} 只有 4 项（法术/灵力/生命/护甲），
 * 表达不了"剑修近战强、鬼修法术强"。本枚举扩成 5 项：
 * <b>近战 / 法术 / 灵力 / 生命 / 护甲</b>。</p>
 *
 * <h2>设计红线（沿用玩家要求）</h2>
 * <ul>
 *   <li><b>无代价</b>：任何一项乘数不得低于 1.0；</li>
 *   <li><b>战力相当</b>：五职业乘数的几何平均极差 &lt; 8%；</li>
 *   <li><b>侧重突出</b>：最高项 / 最低项 &ge; 1.35；</li>
 *   <li><b>专属加成</b>：每个职业一条永久加成，种类互不相同（"多给你一样"）。</li>
 * </ul>
 *
 * <p>断言见 {@code LogicVerifier#careerRules}（R1-A 新增）。</p>
 */
public enum Career {

    /** 剑修：连击 / 剑气 / 御剑。 */
    JIAN_XIU("剑修", 'b',
            "以剑证道 · 连击叠剑意，剑气纵横、剑罡护体",
            1.45D, 1.00D, 1.00D, 1.12D, 1.12D,
            "剑意获取 +10%", BonusKind.SWORD_INTENT,
            0.62F, 0.85F, 1.00F),

    /** 血修：吸血 / 反杀。 */
    XUE_XIU("血修", 'c',
            "以血养功 · 攻击吸血、缓伤反杀，越战越活",
            1.20D, 1.02D, 1.02D, 1.48D, 1.10D,
            "物理攻击吸取 15% 生命，缓伤延迟 30% 伤害", BonusKind.LIFESTEAL,
            0.90F, 0.13F, 0.18F),

    /** 魔修：爆发 / 破防。 */
    MO_XIU("魔修", '5',
            "以魔证道 · 魔气爆发、破防增伤，满仓自伤",
            1.15D, 1.52D, 1.05D, 1.05D, 1.05D,
            "无视目标护甲 8%", BonusKind.PENETRATION,
            0.62F, 0.20F, 0.85F),

    /** 鬼修：诅咒 / 虚化 / 魂爆。 */
    GUI_XIU("鬼修", 'a',
            "以魂御鬼 · 魂印引爆、鬼域控场、虚化免死",
            1.00D, 1.48D, 1.40D, 1.04D, 1.04D,
            "魂印伤害 +12%", BonusKind.SOUL_MARK,
            0.45F, 1.00F, 0.72F),

    /** 体修：坦克 / 反击。 */
    TI_XIU("体修", '6',
            "以身为炉 · 激荡反击、锁血不死，越挨打越猛",
            1.25D, 1.00D, 1.00D, 1.52D, 1.24D,
            "抗击退 +50%，受创减免 3%", BonusKind.KNOCKBACK_RESIST,
            0.95F, 0.78F, 0.35F);

    /** 专属加成的种类（每个职业唯一，断言校验）。 */
    public enum BonusKind {
        /** 剑意获取（剑修）。 */
        SWORD_INTENT,
        /** 物理吸血（血修）。 */
        LIFESTEAL,
        /** 无视护甲（魔修）。 */
        PENETRATION,
        /** 魂印伤害（鬼修）。 */
        SOUL_MARK,
        /** 抗击退（体修）。 */
        KNOCKBACK_RESIST
    }

    private final String cn;
    private final char color;
    private final String desc;
    private final double melee;
    private final double spell;
    private final double mana;
    private final double health;
    private final double armor;
    private final String bonusText;
    private final BonusKind bonusKind;
    private final float[] rgb;

    Career(String cn, char color, String desc,
           double melee, double spell, double mana, double health, double armor,
           String bonusText, BonusKind bonusKind,
           float r, float g, float b) {
        this.cn = cn;
        this.color = color;
        this.desc = desc;
        this.melee = melee;
        this.spell = spell;
        this.mana = mana;
        this.health = health;
        this.armor = armor;
        this.bonusText = bonusText;
        this.bonusKind = bonusKind;
        this.rgb = new float[]{r, g, b};
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

    /** 注册名/ID 用的短名（jian / xue / mo / gui / ti）。 */
    public String id() {
        return this.name().toLowerCase(java.util.Locale.ROOT).replace("_xiu", "");
    }

    public double melee() {
        return this.melee;
    }

    public double spellDamage() {
        return this.spell;
    }

    public double manaMax() {
        return this.mana;
    }

    public double health() {
        return this.health;
    }

    public double armor() {
        return this.armor;
    }

    /** 五维乘数（验证器用来算"无代价 / 几何平均 / 侧重度"）。 */
    public double[] multipliers() {
        return new double[]{this.melee, this.spell, this.mana, this.health, this.armor};
    }

    /** 专属加成的一句话说明。 */
    public String bonus() {
        return this.bonusText;
    }

    /** 专属加成的种类（每个职业唯一）。 */
    public BonusKind bonusKind() {
        return this.bonusKind;
    }

    /** 专属加成的数值（含义由 {@link #bonusKind()} 决定）。 */
    public double bonusValue() {
        return switch (this.bonusKind) {
            case SWORD_INTENT -> 0.10D;
            case LIFESTEAL -> 0.15D;
            case PENETRATION -> 0.08D;
            case SOUL_MARK -> 0.12D;
            case KNOCKBACK_RESIST -> 0.50D;
        };
    }

    /** 主色（HUD 资源条 / 职业特效染色，RGB 0~1）。 */
    public float[] rgb() {
        return this.rgb.clone();
    }

    /** 按名字找（认枚举名、短 id、中文名；大小写不敏感）。 */
    public static Career byName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String s = name.trim();
        for (Career c : values()) {
            if (c.name().equalsIgnoreCase(s) || c.id().equalsIgnoreCase(s)
                    || c.cn.equals(s)) {
                return c;
            }
        }
        // 兼容手工输入：JIANXIU / jian_xiu / 剑修
        String compact = s.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        for (Career c : values()) {
            if (c.name().replace("_", "").equalsIgnoreCase(compact)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 旧道途 → 新职业的迁移映射。
     *
     * <p>只映射三个同名职业：旧 TI→体修、MO→魔修、XUE→血修。
     * 旧 FA（法修）/ ZHEN（阵修）在新体系里没有对应职业 → 返回 {@code null}，
     * 由迁移逻辑清空职业、全额退点并提示重选。</p>
     *
     * @param legacyPathName 旧存档 {@code TalentPath} 里的字符串（枚举名）
     */
    public static Career fromLegacyPathName(String legacyPathName) {
        if (legacyPathName == null) {
            return null;
        }
        return switch (legacyPathName.trim()) {
            case "TI" -> TI_XIU;
            case "MO" -> MO_XIU;
            case "XUE" -> XUE_XIU;
            default -> null;
        };
    }

    public static String allNames() {
        StringBuilder sb = new StringBuilder();
        for (Career c : values()) {
            if (sb.length() > 0) {
                sb.append("\u00a77 / ");
            }
            sb.append(c.colored().getString());
        }
        return sb.toString();
    }
}
