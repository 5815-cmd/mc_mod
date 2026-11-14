package com.hongmeng.xiuxian.sect;

import net.minecraft.ChatFormatting;

/**
 * 门派。原文里的两大势力：千机门（机关算计、擅长情报与杂学）与神霄剑宗（剑修正统）。
 *
 * <p>加入门派后靠"贡献"升级身份，身份越高福利越好。叛门会背"叛徒"名号，
 * 原门派会派执事来清理门户。</p>
 */
public enum Sect {

    NONE("", "无门无派", ChatFormatting.GRAY, "散修",
            "无牵无挂，也无人撑腰。",
            "散修：没有任何加成，但也没人管你。"),

    QIANJI("qianji", "千机门", ChatFormatting.AQUA, "千机",
            "以机关术与情报网闻名，暗地里早已叛变。",
            "反派势力——不可加入，只能打。"),

    SHENXIAO("shenxiao", "神霄剑宗", ChatFormatting.GOLD, "神霄",
            "剑修正统，宗内只认剑。一剑破万法，也一剑惹万人。",
            "神霄剑宗：攻击加成显著、突破成功率略高，但业力增长更快。"),

    MOYUAN("moyuan", "魔渊", ChatFormatting.DARK_PURPLE, "魔渊",
            "不入门派名册的暗处势力。功法凶险，进境极快。",
            "魔渊：修为获取大幅提升、突破成功率显著提升，但业力上限更低（更容易满业力封境）。");

    /** 身份阶梯：贡献度阈值。 */
    private static final int[] RANK_THRESHOLDS = {0, 100, 400, 1200, 4000, 12000};

    private static final String[] RANK_NAMES = {
            "外门弟子", "内门弟子", "真传弟子", "执事", "长老", "太上长老"
    };

    private final String id;
    private final String cn;
    private final ChatFormatting color;
    private final String shortName;
    private final String desc;
    private final String perk;

    Sect(String id, String cn, ChatFormatting color, String shortName, String desc, String perk) {
        this.id = id;
        this.cn = cn;
        this.color = color;
        this.shortName = shortName;
        this.desc = desc;
        this.perk = perk;
    }

    public String id() {
        return id;
    }

    public String cn() {
        return cn;
    }

    public ChatFormatting color() {
        return color;
    }

    public String shortName() {
        return shortName;
    }

    public String desc() {
        return desc;
    }

    public String perk() {
        return perk;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public String colored() {
        return "\u00a7" + color.getChar() + cn;
    }

    /** 可以正式加入的门派（排除散修、排除反派千机门）。 */
    public static Sect[] joinable() {
        return new Sect[]{SHENXIAO, MOYUAN};
    }

    /** 剧情/敌人用的势力标识。 */
    public boolean isHostile() {
        return this == QIANJI;
    }

    public static Sect byId(String id) {
        if (id == null || id.isEmpty()) {
            return NONE;
        }
        for (Sect s : values()) {
            if (s.id.equalsIgnoreCase(id) || s.name().equalsIgnoreCase(id) || s.cn.equals(id)) {
                return s;
            }
        }
        return NONE;
    }

    /** 由贡献度推算身份。 */
    public String rankName(int contribution) {
        int idx = 0;
        for (int i = 0; i < RANK_THRESHOLDS.length; i++) {
            if (contribution >= RANK_THRESHOLDS[i]) {
                idx = i;
            }
        }
        return RANK_NAMES[idx];
    }

    public int rankIndex(int contribution) {
        int idx = 0;
        for (int i = 0; i < RANK_THRESHOLDS.length; i++) {
            if (contribution >= RANK_THRESHOLDS[i]) {
                idx = i;
            }
        }
        return idx;
    }

    /** 下一个身份的贡献门槛；已满级返回 -1。 */
    public int nextRankThreshold(int contribution) {
        int idx = rankIndex(contribution);
        if (idx + 1 >= RANK_THRESHOLDS.length) {
            return -1;
        }
        return RANK_THRESHOLDS[idx + 1];
    }

    // ------------------------------------------------------------------
    // 门派加成
    // ------------------------------------------------------------------

    /** 修为获取倍率。 */
    public double cultivationMultiplier() {
        return switch (this) {
            case QIANJI -> 1.15D;
            case SHENXIAO -> 1.05D;
            case MOYUAN -> 1.35D;
            default -> 1.0D;
        };
    }

    /** 攻击加成（倍率，累加在境界加成之上）。 */
    public double attackBonus() {
        return switch (this) {
            case SHENXIAO -> 0.20D;
            case QIANJI -> 0.05D;
            case MOYUAN -> 0.10D;
            default -> 0.0D;
        };
    }

    /** 突破成功率的额外加成（可为负）。 */
    public double breakthroughBonus() {
        return switch (this) {
            case SHENXIAO -> 0.05D;
            case MOYUAN -> 0.10D;
            case QIANJI -> 0.0D;
            default -> 0.0D;
        };
    }

    /** 业力上限的修正（魔渊更容易满业力封境，契合原文"路走窄了"）。 */
    public int karmaCapModifier() {
        return switch (this) {
            case MOYUAN -> -3;
            case SHENXIAO -> -1;
            default -> 0;
        };
    }

    /** 击杀获得的贡献度。 */
    public int contributionPerKill() {
        return switch (this) {
            case QIANJI -> 2;
            case SHENXIAO -> 3;
            case MOYUAN -> 3;
            default -> 0;
        };
    }
}
