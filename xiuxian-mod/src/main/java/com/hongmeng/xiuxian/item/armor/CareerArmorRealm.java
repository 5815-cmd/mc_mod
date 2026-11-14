package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.core.RealmStats;
import com.hongmeng.xiuxian.talent.Career;
import net.minecraft.world.item.ArmorItem;

/**
 * <b>职业甲的「按境界」数值表</b>——把护甲从"随玩家境界动态成长"改成
 * "和最早的道途甲一样：每个境界一件、固定护甲加成"。
 *
 * <h2>为什么改</h2>
 * <p>玩家要求：「盔甲需要分境界，和最开始的盔甲一样，每个境界都有不同的护甲加成」。
 * 最早的道途甲是 5 道途 × 10 境界 × 4 部位 = 200 件，每件在对应境界有固定数值；
 * 现在的职业甲改成同构：5 职业 × 10 境界 × 4 部位 = <b>200 件</b>。</p>
 *
 * <h2>外观与数值</h2>
 * <ul>
 *   <li>外观仍只有 15 套（5 职业 × 初/进/名器），按境界分段复用：
 *       1~3 境 = 初阶外观、4~6 境 = 进阶外观、7~10 境 = 名器外观；</li>
 *   <li>护甲：{@code 本境标准值 × 0.50 / 0.58 / 0.64}（与 06 表的三档系数一致），
 *       四件按 3/6/5/2 分配，累积取整（与最早道途甲同一算法，避免低境取整误差）；</li>
 *   <li>韧性 0.5/件、击退抗性 0.025/件、附魔 18（与最早道途甲一致）；</li>
 *   <li>生命：每件固定 +4 / +6 / +8（按外观档；穿齐 +16 / +24 / +32，ADDITION）；</li>
 *   <li>耐久：按境界温和放大（与最早道途甲同一公式）。</li>
 * </ul>
 *
 * <p><b>纯数据类</b>：不引用注册表，离线验证器可直接断言 10 境 × 5 职业的数值。</p>
 */
public final class CareerArmorRealm {

    public static final int MAX_REALM = 10;

    /** 四件分配比例（头盔/胸甲/护腿/靴），与 06/最早道途甲一致。 */
    private static final double[] SHARE = {
            3.0D / 16.0D, 6.0D / 16.0D, 5.0D / 16.0D, 2.0D / 16.0D};

    public static final String[] SLOTS = {"helmet", "chestplate", "leggings", "boots"};
    public static final String[] SLOT_CN = {"冠", "胸甲", "护腿", "靴"};

    private static final String[] REALM_CN = {
            "练气", "筑基", "金丹", "元婴", "化神",
            "道印", "飞升", "仙人", "帝境", "鸿蒙"};

    private CareerArmorRealm() {
    }

    public static int clamp(int realm) {
        return Math.max(1, Math.min(MAX_REALM, realm));
    }

    /** 境界 → 外观档（1~3 初阶 / 4~6 进阶 / 7~10 名器）。 */
    public static CareerArmorSet.Tier bandOf(int realm) {
        int r = clamp(realm);
        if (r <= 3) {
            return CareerArmorSet.Tier.BASIC;
        }
        if (r <= 6) {
            return CareerArmorSet.Tier.ADVANCED;
        }
        return CareerArmorSet.Tier.MASTER;
    }

    /** 境界 → 该境界的外观/套装效果表（15 套之一）。 */
    public static CareerArmorSet setOf(Career career, int realm) {
        return CareerArmorSet.of(career, bandOf(realm));
    }

    /** 该境界整套 4 件的护甲值总和（固定值）。 */
    public static int totalArmorAt(int realm) {
        int r = clamp(realm);
        long std = RealmStats.standard(r);
        return Math.max(5, (int) Math.round(std * bandOf(r).armorFactor()));
    }

    /** 指定位数（0 头 / 1 胸 / 2 腿 / 3 靴）在指定境界的单件护甲值。 */
    public static int armorFor(int realm, int slotIndex) {
        int i = Math.max(0, Math.min(3, slotIndex));
        int target = Math.max(5, totalArmorAt(realm));
        double cum = 0.0D;
        int prev = 0;
        for (int k = 0; k <= i; k++) {
            cum += SHARE[k];
            int bound = (int) Math.round(target * cum);
            if (k == i) {
                return Math.max(1, bound - prev);
            }
            prev = bound;
        }
        return 1;
    }

    /** 单件生命加成点数（按外观档 4/6/8；穿齐 16/24/32，ADDITION）。 */
    public static double healthBonusPerPiece(int realm) {
        return bandOf(realm).healthBonus();
    }

    /** 单件韧性（和最早道途甲一致：0.5）。 */
    public static double toughness() {
        return 0.5D;
    }

    /** 击退抗性（和最早道途甲一致）。 */
    public static double knockbackResistance() {
        return 0.025D;
    }

    /** 附魔能力。 */
    public static int enchantmentValue() {
        return 18;
    }

    /** 单件耐久：与最早道途甲同一公式（境界越高越耐用）。 */
    public static int durabilityAt(int realm, int slotIndex) {
        int[] base = {13, 16, 15, 13};
        int b = base[Math.max(0, Math.min(3, slotIndex))];
        int r = clamp(realm);
        double std = RealmStats.standard(r);
        double scale = Math.sqrt(std / 8.0D);
        return (int) Math.max(b * 26L, Math.round(b * 26.0D * scale));
    }

    public static String realmCn(int realm) {
        return REALM_CN[clamp(realm) - 1];
    }

    /** 物品 ID：{@code career_armor_<career>_<realm>_<slot>}。 */
    public static String itemName(Career career, int realm, String slot) {
        return "career_armor_" + career.id() + "_" + clamp(realm) + "_" + slot;
    }

    public static int indexOf(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
        };
    }
}
