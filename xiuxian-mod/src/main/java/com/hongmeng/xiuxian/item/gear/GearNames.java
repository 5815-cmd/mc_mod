package com.hongmeng.xiuxian.item.gear;

/**
 * 装备的名称与品阶数据。
 *
 * <p><b>刻意不含任何 Minecraft 注册表引用</b>——这样离线验证器可以安全地断言这些表，
 * 而不会触发注册表初始化。需要 {@code Tiers} 之类的逻辑留在物品类里。</p>
 */
public final class GearNames {

    /** 十境的品阶名（下标 0 = 练气），直接用"炼制者所需境界"命名。 */
    public static final String[] GRADE_NAMES = {
            "练气法器", "筑基灵器", "金丹宝器", "元婴法宝", "化神灵宝",
            "道印道器", "飞升仙器", "仙人仙宝", "帝境帝兵", "鸿蒙至宝"
    };

    /** 十境的武器名。 */
    public static final String[] SWORD_NAMES = {
            "青锋剑", "桃木剑", "解牛刀", "血木剑", "燃山剑",
            "五行剑", "圣皇枪", "撼地锤", "裂虎爪", "鸿蒙道剑"
    };

    /** 五个法宝部位（注册名 key → 中文名）。 */
    public static final String[][] SLOTS = {
            {"crown", "法冠"},
            {"robe", "道袍"},
            {"pants", "法裤"},
            {"boots", "云履"},
            {"jade", "玉佩"},
    };

    private GearNames() {
    }

    /** 大境序号（1~10）→ 品阶名。 */
    public static String gradeOf(int realmTier) {
        int idx = Math.max(1, Math.min(10, realmTier)) - 1;
        return GRADE_NAMES[idx];
    }

    /** 大境序号（1~10）→ 武器名。 */
    public static String swordOf(int realmTier) {
        int idx = Math.max(1, Math.min(10, realmTier)) - 1;
        return SWORD_NAMES[idx];
    }

    /** 中文部位名 → 注册名 key。 */
    public static String slotKey(String slotName) {
        for (String[] pair : SLOTS) {
            if (pair[1].equals(slotName)) {
                return pair[0];
            }
        }
        return "misc";
    }

    /** 注册名 key → 中文部位名。 */
    public static String slotName(String key) {
        for (String[] pair : SLOTS) {
            if (pair[0].equals(key)) {
                return pair[1];
            }
        }
        return key;
    }

    /** 注册名：第 N 境的剑。 */
    public static String swordId(int realmTier) {
        return "realm_sword_" + realmTier;
    }

    /** 注册名：第 N 境的某部位法宝。 */
    public static String artifactId(int realmTier, String slotKey) {
        return "artifact_" + realmTier + "_" + slotKey;
    }
}
