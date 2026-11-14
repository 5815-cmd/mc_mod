package com.hongmeng.xiuxian.item.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * 按大境分阶的 {@link ArmorMaterial} 实现（Forge 1.20.1）。
 *
 * <p>1.20.1 的 {@code ArmorMaterial} 是一个**接口**（不是 1.21 的 record），
 * 需要实现八个方法：</p>
 *
 * <pre>
 *   getDurabilityForType(Type)   单件耐久
 *   getDefenseForType(Type)      单件护甲值
 *   getEnchantmentValue()        附魔能力
 *   getEquipSound()              穿脱音效
 *   getRepairIngredient()        铁砧修复材料
 *   getName()                    材质名 —— 同时决定护甲图层贴图路径
 *   getToughness()               韧性
 *   getKnockbackResistance()     击退抗性
 * </pre>
 *
 * <p><b>{@code getName()} 是这里唯一「会渲染」的方法。</b>原版
 * {@code HumanoidArmorLayer#getArmorResource} 会把它当作贴图名，拼出</p>
 *
 * <pre>
 *   &lt;namespace&gt;:textures/models/armor/&lt;path&gt;_layer_1.png   ← 头盔/胸甲/靴子
 *   &lt;namespace&gt;:textures/models/armor/&lt;path&gt;_layer_2.png   ← 护腿（内层模型）
 * </pre>
 *
 * <p>所以这里返回 {@code "hongmeng:dao_armor_<tier>"}，对应的文件是
 * {@code assets/hongmeng/textures/models/armor/dao_armor_<tier>_layer_{1,2}.png}，
 * 两张都是 64×32（原版盔甲 UV 布局）。</p>
 *
 * <p><b>修复材料用 {@code Supplier}</b>：{@code Ingredient} 一旦延迟到注册事件里再解析，
 * 早期的物品引用会拿到空注册对象。{@link DaoArmorMaterial#getRepairIngredient()}
 * 每次调用都重新问一遍 supplier，安全。</p>
 */
public final class DaoArmorMaterial implements ArmorMaterial {

    /** 该材质对应的设计数据。 */
    private final DaoArmorTier tier;

    /** 修复材料（延迟解析，避免注册表还没就绪）。 */
    private final Supplier<Ingredient> repair;

    public DaoArmorMaterial(DaoArmorTier tier, Supplier<Ingredient> repair) {
        this.tier = tier;
        this.repair = repair;
    }

    /** 用「灵铁锭」修复：十境通用，因为灵铁是全部炼器的起点。 */
    public static DaoArmorMaterial of(DaoArmorTier tier) {
        return new DaoArmorMaterial(tier, () -> Ingredient.of(Items.IRON_INGOT));
    }

    /** 指定修复材料。 */
    public static DaoArmorMaterial of(DaoArmorTier tier, Supplier<Ingredient> repair) {
        return new DaoArmorMaterial(tier, repair);
    }

    public DaoArmorTier tier() {
        return tier;
    }

    /** 大境序号 1~10。 */
    public int realmTier() {
        return tier.tier();
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return tier.durabilityFor(slotOf(type));
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return tier.defenseFor(slotOf(type));
    }

    @Override
    public int getEnchantmentValue() {
        return tier.enchantability();
    }

    /**
     * 穿脱音效按境界分档：
     *
     * <pre>
     *   1~2  皮革     凡俗布料
     *   3~4  锁甲     金属初成
     *   5~6  金       灵光流转
     *   7~8  钻石     仙气凛然
     *   9~10 下界合金 帝境/鸿蒙
     * </pre>
     */
    @Override
    public SoundEvent getEquipSound() {
        int t = tier.tier();
        if (t <= 2) {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }
        if (t <= 4) {
            return SoundEvents.ARMOR_EQUIP_CHAIN;
        }
        if (t <= 6) {
            return SoundEvents.ARMOR_EQUIP_GOLD;
        }
        if (t <= 8) {
            return SoundEvents.ARMOR_EQUIP_DIAMOND;
        }
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return tier.materialName();
    }

    @Override
    public float getToughness() {
        return tier.toughness();
    }

    /**
     * 击退抗性。
     *
     * <p>{@code ArmorItem} 只在 {@code knockbackResistance > 0} 时才挂上
     * 「击退抗性」属性修饰符（Forge 把原版写死的
     * {@code if (material == NETHERITE)} 改成了
     * {@code if (this.knockbackResistance > 0)}），
     * 所以这里的返回值直接决定这一档护甲能不能抗击退。</p>
     *
     * <p>练气/筑基是凡俗布料，一点不抗；从金丹开始给，到鸿蒙封顶 90%。</p>
     */
    @Override
    public float getKnockbackResistance() {
        int t = tier.tier();
        if (t <= 2) {
            return 0.0F;
        }
        return Math.min(0.9F, (t - 2) * 0.1F);
    }

    /** {@link ArmorItem.Type} → 四件顺序下标（头盔 0 / 胸甲 1 / 护腿 2 / 靴子 3）。 */
    private static int slotOf(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
        };
    }

    @Override
    public String toString() {
        return "DaoArmorMaterial[" + getName() + ", toughness=" + getToughness() + "]";
    }
}
