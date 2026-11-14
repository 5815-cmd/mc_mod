package com.hongmeng.xiuxian.talent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * <b>天赋/职业带来的属性修饰符</b>（生命上限 / 抗击退 / 攻速）。
 *
 * <h2>为什么单独一个类</h2>
 * <p>天赋里有两类加成：</p>
 * <ul>
 *   <li><b>事件型</b>（伤害、减伤、吸血…）—— 在 {@code ModEvents} 里当场乘；</li>
 *   <li><b>属性型</b>（生命上限 / 抗击退 / 攻速）—— 必须挂成
 *       {@code AttributeModifier} 才会被原版结算。</li>
 * </ul>
 *
 * <h2>⚠ 幂等</h2>
 * <p>天赋点随时会变，所以每 10 tick 重算一次 —— 用<b>固定 UUID + 先摘后挂</b>，
 * 不会叠加（重复 addPermanentModifier 抛异常被 try/catch 吃掉、
 * 于是 buff 静默失效，这个坑项目里踩过）。</p>
 */
public final class TalentAttributes {

    private static final UUID HEALTH_ID = UUID.nameUUIDFromBytes(
            "hongmeng_talent_health".getBytes(StandardCharsets.UTF_8));
    private static final UUID KNOCKBACK_ID = UUID.nameUUIDFromBytes(
            "hongmeng_talent_knockback".getBytes(StandardCharsets.UTF_8));
    private static final UUID ATTACK_SPEED_ID = UUID.nameUUIDFromBytes(
            "hongmeng_talent_attack_speed".getBytes(StandardCharsets.UTF_8));
    /** 职业被动·御剑（剑修）的移速加成。 */
    private static final UUID MOVE_SPEED_ID = UUID.nameUUIDFromBytes(
            "hongmeng_career_move_speed".getBytes(StandardCharsets.UTF_8));
    /** 职业被动·金刚（体修）的护甲点数加成（百分比转点数）。 */
    private static final UUID ARMOR_ID = UUID.nameUUIDFromBytes(
            "hongmeng_career_armor".getBytes(StandardCharsets.UTF_8));
    /** 职业被动·金刚（体修）的韧性点数加成（百分比转点数）。 */
    private static final UUID TOUGHNESS_ID = UUID.nameUUIDFromBytes(
            "hongmeng_career_toughness".getBytes(StandardCharsets.UTF_8));

    private TalentAttributes() {
    }

    /** 每 10 tick（或加点/换装/择业时立刻）刷新一次。 */
    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            apply(player, Attributes.MAX_HEALTH, HEALTH_ID, "天赋生命",
                    healthBonus(player), AttributeModifier.Operation.MULTIPLY_TOTAL);
            apply(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "天赋抗击退",
                    knockbackBonus(player), AttributeModifier.Operation.ADDITION);
            apply(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "天赋攻速",
                    attackSpeedBonus(player), AttributeModifier.Operation.MULTIPLY_TOTAL);
            // ---- 职业被动（属性型）----
            // 御剑（剑修分支被动）：移速 +10%。比例口径，MULTIPLY_TOTAL 直接乘总移速。
            apply(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_ID, "御剑移速",
                    moveSpeedBonus(player), AttributeModifier.Operation.MULTIPLY_TOTAL);
            // 金刚（体修分支被动）：护甲/韧性 +8%。护甲是点数不是比例，见方法注释。
            refreshArmorPoints(player);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[talent] 属性刷新失败：{}", t.toString());
        }
    }

    /** 生命加成（MULTIPLY_TOTAL 比例）：天赋生命 + 职业被动·气血（体修 +10%）。 */
    private static double healthBonus(ServerPlayer player) {
        return Math.max(0.0D, TalentBonus.health(player)
                + CareerPassives.health(player));
    }

    /** 抗击退：天赋/职业 + 职业被动·铁骨（体修 +25%），沿用既有 0.85 封顶。 */
    private static double knockbackBonus(ServerPlayer player) {
        return Math.min(0.85D, TalentBonus.knockbackResist(player)
                + CareerPassives.knockbackResist(player));
    }

    /** 攻速（职业甲 2 件套/天赋，比例）。 */
    private static double attackSpeedBonus(ServerPlayer player) {
        return Math.max(0.0D, Math.min(1.0D, TalentBonus.attackSpeed(player)));
    }

    /** 御剑（剑修被动）移速（比例，封顶 100% 防失控）。 */
    private static double moveSpeedBonus(ServerPlayer player) {
        return Math.max(0.0D, Math.min(1.0D, CareerPassives.moveSpeed(player)));
    }

    /**
     * 金刚（体修被动）：护甲 / 韧性 +8%，<b>百分比转点数</b>口径。
     *
     * <p>⚠ 为什么不能直接 {@code MULTIPLY_TOTAL}：玩家基础 {@code Attributes.ARMOR}
     * 是 <b>0</b>（护甲点数全部来自装备/境界修饰符），百分比乘 0 还是 0 ——
     * 面板看不到、结算也不生效。所以这里按"当前护甲点数 × 8%"折算成点数，
     * 再用 {@code ADDITION} 挂回去，观感与减伤公式都对得上。</p>
     *
     * <p>⚠ 幂等：每次刷新<b>先摘掉自己的旧修饰符</b>再读底子，否则读数里
     * 含上一次的加成，每 10 tick 滚一次会复利膨胀。</p>
     */
    private static void refreshArmorPoints(ServerPlayer player) {
        double frac = Math.max(0.0D, Math.min(1.0D, CareerPassives.armor(player)));
        // 先摘：保证下面读到的"底子"（装备 + 境界 + 法宝）不含本被动的旧加成
        removeModifier(player, Attributes.ARMOR, ARMOR_ID);
        removeModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID);
        if (frac < 1.0E-6D) {
            return;
        }
        double armorBase = player.getAttributeValue(Attributes.ARMOR);
        double toughBase = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        addPoints(player, Attributes.ARMOR, ARMOR_ID, "金刚护甲", armorBase * frac);
        addPoints(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "金刚韧性",
                toughBase * frac);
    }

    /** 摘掉一个固定 UUID 的修饰符（没有就跳过）。 */
    private static void removeModifier(ServerPlayer player,
                                       net.minecraft.world.entity.ai.attributes.Attribute attr,
                                       UUID id) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null && inst.getModifier(id) != null) {
            inst.removeModifier(id);
        }
    }

    /** 挂一条 ADDITION 点数修饰符（固定 UUID，先摘后挂，幂等）。 */
    private static void addPoints(ServerPlayer player,
                                  net.minecraft.world.entity.ai.attributes.Attribute attr,
                                  UUID id, String name, double points) {
        if (Math.abs(points) < 1.0E-6D) {
            return;
        }
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) {
            return;
        }
        if (inst.getModifier(id) != null) {
            inst.removeModifier(id);
        }
        inst.addPermanentModifier(new AttributeModifier(id, name, points,
                AttributeModifier.Operation.ADDITION));
    }

    /** 固定 UUID + 先摘后挂（幂等）。 */
    private static void apply(ServerPlayer player,
                              net.minecraft.world.entity.ai.attributes.Attribute attr,
                              UUID id, String name, double fraction,
                              AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) {
            return;
        }
        if (inst.getModifier(id) != null) {
            inst.removeModifier(id);
        }
        if (Math.abs(fraction) < 1.0E-6D) {
            return;
        }
        inst.addPermanentModifier(new AttributeModifier(id, name, fraction, op));
    }
}
