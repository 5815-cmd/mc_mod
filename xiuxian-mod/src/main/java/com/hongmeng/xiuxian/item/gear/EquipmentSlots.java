package com.hongmeng.xiuxian.item.gear;

/**
 * 法宝装备栏的槽位元数据。
 *
 * <p><b>刻意不含任何 Minecraft 类型。</b>这样离线验证器可以直接断言槽位表，
 * 而不会因为 {@code ItemStack} 的静态初始化把注册表拖进来（Not bootstrapped）。</p>
 *
 * <p>{@link PlayerEquipment} 只做存取，槽位定义全在这里。</p>
 */
public final class EquipmentSlots {

    /** 五个部位的注册名 key，顺序即界面里的顺序。 */
    public static final String[] SLOT_KEYS = {"crown", "robe", "pants", "boots", "jade"};

    /** 每个部位的中文名（界面与提示用）。 */
    public static final String[] SLOT_NAMES = {"法冠", "道袍", "法裤", "云履", "玉佩"};

    public static final int COUNT = SLOT_KEYS.length;

    private EquipmentSlots() {
    }

    /** 部位 key 的索引；找不到返回 -1。 */
    public static int indexOf(String slotKey) {
        if (slotKey == null) {
            return -1;
        }
        for (int i = 0; i < COUNT; i++) {
            if (SLOT_KEYS[i].equals(slotKey)) {
                return i;
            }
        }
        return -1;
    }

    /** 索引 → 中文名；越界返回兜底文本。 */
    public static String nameOf(int index) {
        return index >= 0 && index < COUNT ? SLOT_NAMES[index] : "法宝";
    }

    /** 索引是否合法。 */
    public static boolean isValid(int index) {
        return index >= 0 && index < COUNT;
    }
}
