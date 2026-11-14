package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.talent.Career;
import com.hongmeng.xiuxian.talent.Talents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>职业甲结算（按境界固定数值 + 同外观档 2/4 件套）</b>。
 *
 * <h2>改动说明（C 线）</h2>
 * <p>护甲/韧性已经写进 {@link CareerArmorMaterial}（按境界固定），
 * 这里<b>不再</b>往属性上叠 ARMOR / ARMOR_TOUGHNESS，避免双算；
 * 只负责：</p>
 * <ul>
 *   <li>每件生命加成（固定 +4/+6/+8，按外观档；穿齐 +16/+24/+32，ADDITION）；</li>
 *   <li>2/4 件套效果：<b>同职业 + 同外观档</b>才生效（1~3 / 4~6 / 7~10，
 *       跨档混穿不凑套；2+2 混搭保留，但同一套只结算一次）；</li>
 *   <li>机制型效果缓存 {@link #bonusOf} 给职业机制读取；</li>
 *   <li>攻速属性（来自 2 件套 ATTACK_SPEED）。</li>
 * </ul>
 */
public final class CareerArmorHandler {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final UUID ARMOR_ID = UUID.nameUUIDFromBytes(
            "hongmeng_career_armor_armor".getBytes(StandardCharsets.UTF_8));
    private static final UUID TOUGHNESS_ID = UUID.nameUUIDFromBytes(
            "hongmeng_career_armor_toughness".getBytes(StandardCharsets.UTF_8));
    private static final UUID HEALTH_ID = UUID.nameUUIDFromBytes(
            "hongmeng_career_armor_health".getBytes(StandardCharsets.UTF_8));
    private static final UUID SPEED_ID = UUID.nameUUIDFromBytes(
            "hongmeng_career_armor_speed".getBytes(StandardCharsets.UTF_8));

    private static final String HEALTH_NAME = "hongmeng_career_health";
    private static final String SPEED_NAME = "hongmeng_career_speed";

    /** 玩家 UUID -> 已生效的套装效果（按 EffectKind.ordinal() 索引）。 */
    private static final Map<UUID, double[]> CACHE = new ConcurrentHashMap<>();

    private CareerArmorHandler() {
    }

    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Career career = Talents.pathOf(player);

        // 生命：每件固定 +4/+6/+8，按 ADDITION 累加（混档 = 逐件相加）。
        double healthBonus = 0.0D;

        // 2/4 件套：按「职业 + 外观档」分组（CareerArmorItem.set()，共 15 套），
        // 每套只结算一次 —— 2+2 混搭（两套不同档各 2 件）两条 2 件套都生效，
        // 但同一套不会因为跨境界各 2 件而被结算两次。
        int[] setCounts = new int[CareerArmorSet.values().length];

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof CareerArmorItem item) {
                setCounts[item.set().ordinal()]++;
                healthBonus += CareerArmorRealm.healthBonusPerPiece(item.realm());
            }
        }

        double[] bonus = new double[CareerArmorSet.EffectKind.values().length];
        for (CareerArmorSet set : CareerArmorSet.values()) {
            int worn = setCounts[set.ordinal()];
            if (worn <= 0) {
                continue;
            }
            boolean careerMatch = career != null && career == set.career();
            if (careerMatch && worn >= 2) {
                addEffects(bonus, set.setTwo());
            }
            if (careerMatch && worn >= 4) {
                addEffects(bonus, set.setFour());
            }
        }
        CACHE.put(player.getUUID(), bonus);

        double attackSpeed = bonus[CareerArmorSet.EffectKind.ATTACK_SPEED.ordinal()];
        // 老版本动态护甲/韧性留下的修饰符：清掉（新版本由材质提供）
        remove(player, Attributes.ARMOR, ARMOR_ID);
        remove(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID);
        applyFlat(player, Attributes.MAX_HEALTH, HEALTH_ID, HEALTH_NAME, healthBonus);
        applyPercent(player, Attributes.ATTACK_SPEED, SPEED_ID, SPEED_NAME, attackSpeed);
    }

    /** 穿够 2 件且职业/外观档匹配 → true（同外观档才凑套）。 */
    public static boolean setTwoActive(net.minecraft.world.entity.player.Player player,
                                       CareerArmorSet set) {
        return wornSameSet(player, set) >= 2
                && Talents.pathOf(player) == set.career();
    }

    /** 穿够 4 件且职业/外观档匹配 → true。 */
    public static boolean setFourActive(net.minecraft.world.entity.player.Player player,
                                        CareerArmorSet set) {
        return wornSameSet(player, set) >= 4
                && Talents.pathOf(player) == set.career();
    }

    public static double bonusOf(net.minecraft.world.entity.player.Player player,
                                 CareerArmorSet.EffectKind kind) {
        if (player == null) {
            return 0.0D;
        }
        double[] arr = CACHE.get(player.getUUID());
        return arr == null ? 0.0D : arr[kind.ordinal()];
    }

    /** 清掉全部本系统属性修饰符（下线/换装用）。 */
    public static void clear(ServerPlayer player) {
        if (player == null) {
            return;
        }
        remove(player, Attributes.ARMOR, ARMOR_ID);
        remove(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID);
        remove(player, Attributes.MAX_HEALTH, HEALTH_ID);
        remove(player, Attributes.ATTACK_SPEED, SPEED_ID);
    }

    public static void forget(UUID uuid) {
        CACHE.remove(uuid);
    }

    /** 某个「职业 + 外观档」套装当前穿了几件（2/4 套判定）。 */
    private static int wornSameSet(net.minecraft.world.entity.player.Player player,
                                   CareerArmorSet set) {
        if (player == null) {
            return 0;
        }
        int worn = 0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof CareerArmorItem item && item.set() == set) {
                worn++;
            }
        }
        return worn;
    }

    private static void addEffects(double[] out, CareerArmorSet.Effect[] effects) {
        for (CareerArmorSet.Effect e : effects) {
            out[e.kind().ordinal()] += e.value();
        }
    }

    private static void applyPercent(ServerPlayer player, Attribute attr, UUID id,
                                     String name, double ratio) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (ratio > 0.0D) {
            inst.addTransientModifier(new AttributeModifier(
                    id, name, ratio, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** 挂一条固定点数的 ADDITION 修饰符（先摘后挂，幂等）。 */
    private static void applyFlat(ServerPlayer player, Attribute attr, UUID id,
                                  String name, double amount) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (Math.abs(amount) > 1.0E-6D) {
            inst.addTransientModifier(new AttributeModifier(
                    id, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void remove(ServerPlayer player, Attribute attr, UUID id) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) {
            inst.removeModifier(id);
        }
    }
}
