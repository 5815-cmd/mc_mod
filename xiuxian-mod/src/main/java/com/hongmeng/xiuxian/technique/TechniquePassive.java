package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 被动功法（诀）的实际效果。
 *
 * <p>被动**不主动释放、不需要目标、释放时不消耗灵力**；装备到
 * {@link TechniqueData#PASSIVE_SLOTS} 个被动槽上即生效。</p>
 *
 * <h3>怎么生效</h3>
 *
 * <p>走本模组既有的那套属性修饰符（{@code core/ModEvents#applyModifier} 的写法）：
 * <b>每个被动一个固定的 UUID</b>，重新施放时先按 UUID 移除、再按需加回，
 * 所以反复调用是幂等的，多个被动之间也各挂各的、互不覆盖。</p>
 *
 * <pre>
 *   灵海无量   灵力上限 ×1.25      → {@link #manaCapMultiplier}（灵力池归 CultivationData，由它取用）
 *   吐纳诀     灵力回复 ×1.50      → {@link #manaRegenMultiplier}
 *   金刚体     护甲 +8             → Attributes.ARMOR（ADDITION）
 *   疾风步     移速 +15%           → Attributes.MOVEMENT_SPEED（MULTIPLY_TOTAL）
 *   剑心通明   持剑伤害 +20%       → {@link #swordDamageMultiplier}（TechniqueCaster 的伤害公式取用）
 *   五行调和   护甲 +5 / 韧性 +4   → ARMOR / ARMOR_TOUGHNESS（ADDITION）
 *   神识凝练   法术伤害 +12%       → {@link #spellDamageMultiplier}（TechniqueCaster 的伤害公式取用）
 *   气血充盈   生命上限 +15%       → Attributes.MAX_HEALTH（MULTIPLY_TOTAL）
 * </pre>
 *
 * <p><b>为什么灵力上限/回复不在这里直接改数值：</b>灵力池是
 * {@link CultivationData} 的纯数据字段（见 {@code ManaTable}），
 * 那边的 {@code maxMana()} / 回复 tick 才是唯一的权威。
 * 这里只提供倍率，让那边取用——不另起一套灵力。</p>
 */
public final class TechniquePassive {

    /** 灵海无量：灵力上限 +25%。 */
    public static final double MANA_CAP_BONUS = 0.25D;
    /** 吐纳诀：灵力回复 +50%。 */
    public static final double MANA_REGEN_BONUS = 0.50D;
    /** 神识凝练：法术伤害 +12%。 */
    public static final double SPELL_DAMAGE_BONUS = 0.12D;
    /** 剑心通明：持剑伤害 +20%。 */
    public static final double SWORD_DAMAGE_BONUS = 0.20D;

    /** 血魔功：造成伤害的 8% 转为自身生命。 */
    public static final double LIFESTEAL_FRACTION = 0.08D;

    /** 破军诀：造成伤害 +18%。 */
    public static final double DAMAGE_AMPLIFY = 0.18D;

    /** 龟息功：受到伤害 -15%。 */
    public static final double DAMAGE_REDUCE = 0.15D;

    /** 凌波微步：受击时 12% 概率完全闪避。 */
    public static final double DODGE_CHANCE = 0.12D;

    private TechniquePassive() {
    }

    /**
     * 一门被动功法的效果定义。
     *
     * <p>{@code amountOf} 返回 0 表示"这个被动不挂属性修饰符"
     * （灵力类与伤害类走 {@link TechniquePassive} 的静态倍率）。</p>
     */
    public enum Effect {

        MANA_CAP("灵海无量", null, null, 0.0D),
        REGEN("吐纳诀", null, null, 0.0D),
        ARMOR("金刚体", Attributes.ARMOR, AttributeModifier.Operation.ADDITION, 8.0D),
        SPEED("疾风步", Attributes.MOVEMENT_SPEED,
                AttributeModifier.Operation.MULTIPLY_TOTAL, 0.15D),
        SWORD("剑心通明", null, null, 0.0D),
        ELEMENTS("五行调和", Attributes.ARMOR,
                AttributeModifier.Operation.ADDITION, 5.0D),
        SPELL("神识凝练", null, null, 0.0D),
        HEALTH("气血充盈", Attributes.MAX_HEALTH,
                AttributeModifier.Operation.MULTIPLY_TOTAL, 0.15D),

        // ---- 新增：战斗类被动 ----
        // 前三门不挂属性（走下面的静态倍率，由伤害结算处读取），
        // 战意沸腾能直接挂在 ATTACK_DAMAGE 上，所以用属性实现。
        LIFESTEAL("血魔功", null, null, 0.0D),
        AMPLIFY("破军诀", null, null, 0.0D),
        REDUCE("龟息功", null, null, 0.0D),
        DODGE("凌波微步", null, null, 0.0D),
        ATTACK("战意沸腾", Attributes.ATTACK_DAMAGE,
                AttributeModifier.Operation.MULTIPLY_TOTAL, 0.15D);

        private final String cn;
        /** 主属性；null 表示这个被动不通过属性生效。 */
        private final Attribute attribute;
        private final AttributeModifier.Operation operation;
        private final double amount;

        Effect(String cn, Attribute attribute, AttributeModifier.Operation operation,
               double amount) {
            this.cn = cn;
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
        }

        public String cn() {
            return cn;
        }

        public Attribute attribute() {
            return attribute;
        }

        public AttributeModifier.Operation operation() {
            return operation;
        }

        public double amount() {
            return amount;
        }

        /** 五行调和额外挂一点护甲韧性。 */
        public double toughnessAmount() {
            return this == ELEMENTS ? 4.0D : 0.0D;
        }

        /** 固定修饰符 id：名字里带被动名，重挂不会互相顶掉。 */
        public UUID modifierId() {
            return UUID.nameUUIDFromBytes(
                    ("hongmeng_passive_" + name().toLowerCase(java.util.Locale.ROOT))
                            .getBytes(StandardCharsets.UTF_8));
        }

        public UUID toughnessModifierId() {
            return UUID.nameUUIDFromBytes(
                    ("hongmeng_passive_" + name().toLowerCase(java.util.Locale.ROOT) + "_toughness")
                            .getBytes(StandardCharsets.UTF_8));
        }

        public String modifierName() {
            return "hongmeng_passive_" + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** 这门功法是不是被动。 */
    public static boolean isPassive(Technique technique) {
        return technique != null && technique.isPassive();
    }

    /** 被动功法 → 效果定义；不是被动返回 null。 */
    public static Effect effectOf(Technique technique) {
        if (technique == null || !technique.isPassive()) {
            return null;
        }
        for (Effect e : Effect.values()) {
            if (e.cn().equals(technique.cn())) {
                return e;
            }
        }
        return null;
    }

    /**
     * 把已装备的被动全部应用到玩家身上（幂等）。
     *
     * <p>要么全挂、要么全摘：把 {@code Effect.values()} 扫一遍，
     * 没装备的用 {@code amount = 0} 调一次（= 按 UUID 摘掉），
     * 装备了的挂上真实数值。所以本方法可以在登录、重生、
     * 每秒 tick 里无脑重复调用。</p>
     */
    public static void apply(ServerPlayer player, TechniqueData techniques) {
        if (player == null || techniques == null) {
            return;
        }
        for (Effect effect : Effect.values()) {
            boolean active = isEquippedByCn(techniques, effect);
            applyAttribute(player, effect.attribute(), effect.modifierId(), effect.modifierName(),
                    active ? effect.amount() : 0.0D, effect.operation());
            if (effect.toughnessAmount() > 0.0D) {
                applyAttribute(player, Attributes.ARMOR_TOUGHNESS,
                        effect.toughnessModifierId(), effect.modifierName() + "_toughness",
                        active ? effect.toughnessAmount() : 0.0D,
                        AttributeModifier.Operation.ADDITION);
            }
        }
        // 生命上限变了，把当前血量夹回上限内（和 ModEvents 的做法一致）
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** 把被动全部摘掉（死亡 / 掉线 / 洗点）。 */
    public static void clear(ServerPlayer player) {
        apply(player, new TechniqueData());
    }

    private static boolean isEquippedByCn(TechniqueData techniques, Effect effect) {
        for (Technique t : techniques.equippedPassives()) {
            if (t.cn().equals(effect.cn())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按固定 UUID 挂一个修饰符（与 {@code ModEvents#applyModifier} 同一套做法）。
     *
     * <p>{@code amount == 0} 时只摘不挂，所以"取消装备"也是走这里。</p>
     */
    private static void applyAttribute(ServerPlayer player, Attribute attribute,
                                       UUID id, String name, double amount,
                                       AttributeModifier.Operation op) {
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0D) {
            instance.addPermanentModifier(new AttributeModifier(id, name, amount, op));
        }
    }

    // ------------------------------------------------------------------
    // 倍率（给灵力池与伤害公式取用）
    // ------------------------------------------------------------------

    /** 灵海无量：灵力上限倍率（没装备 = 1.0）。 */
    public static double manaCapMultiplier(TechniqueData techniques) {
        return hasEquipped(techniques, Effect.MANA_CAP) ? 1.0D + MANA_CAP_BONUS : 1.0D;
    }

    /** 吐纳诀：灵力回复倍率（没装备 = 1.0）。 */
    public static double manaRegenMultiplier(TechniqueData techniques) {
        return hasEquipped(techniques, Effect.REGEN) ? 1.0D + MANA_REGEN_BONUS : 1.0D;
    }

    /** 神识凝练：法术伤害倍率（没装备 = 1.0）。 */
    public static double spellDamageMultiplier(TechniqueData techniques) {
        return hasEquipped(techniques, Effect.SPELL) ? 1.0D + SPELL_DAMAGE_BONUS : 1.0D;
    }

    /** 剑心通明：持剑伤害倍率（没装备 = 1.0）。 */
    public static double swordDamageMultiplier(TechniqueData techniques) {
        return hasEquipped(techniques, Effect.SWORD) ? 1.0D + SWORD_DAMAGE_BONUS : 1.0D;
    }

    /** 这门功法对应的效果有没有装备在被动槽上。 */
    /** 吸血比例（没装备血魔功就是 0）。 */
    public static double lifesteal(TechniqueData techniques) {
        return hasEquipped(techniques, Effect.LIFESTEAL) ? LIFESTEAL_FRACTION : 0.0D;
    }

    /** 增伤倍率（1.0 = 不变）。 */
    public static double amplifyMultiplier(TechniqueData techniques) {
        return hasEquipped(techniques, Effect.AMPLIFY) ? 1.0D + DAMAGE_AMPLIFY : 1.0D;
    }

    /** 减伤倍率（1.0 = 不变，越小越抗打）。 */
    public static double reduceMultiplier(TechniqueData techniques) {
        return hasEquipped(techniques, Effect.REDUCE) ? 1.0D - DAMAGE_REDUCE : 1.0D;
    }

    /** 闪避概率（0 = 不闪）。 */
    public static double dodgeChance(TechniqueData techniques) {
        return hasEquipped(techniques, Effect.DODGE) ? DODGE_CHANCE : 0.0D;
    }

    public static boolean hasEquipped(TechniqueData techniques, Effect effect) {
        return techniques != null && isEquippedByCn(techniques, effect);
    }
}
