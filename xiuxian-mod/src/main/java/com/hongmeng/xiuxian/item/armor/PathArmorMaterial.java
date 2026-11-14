package com.hongmeng.xiuxian.item.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * 道途护甲的 {@link ArmorMaterial} 实现（Forge 1.20.1）。
 *
 * <p>1.20.1 的 {@code ArmorMaterial} 是<b>接口</b>（不是 1.21 的 record），
 * 需要实现八个方法。这里照 {@code DaoArmorMaterial} 的写法，只是数值来源换成
 * {@link PathArmorTier}。</p>
 *
 * <p><b>{@code getName()} 是唯一"会渲染"的方法</b>：原版
 * {@code HumanoidArmorLayer#getArmorResource} 会把它当贴图名，拼出</p>
 * <pre>
 *   hongmeng:textures/models/armor/path_armor_&lt;path&gt;_layer_1.png   ← 头盔/胸甲/靴子
 *   hongmeng:textures/models/armor/path_armor_&lt;path&gt;_layer_2.png   ← 护腿
 * </pre>
 * <p>两张都是 64×32（原版盔甲 UV 布局），由
 * {@code tools/gen_path_armor_textures.py} 程序化生成。</p>
 *
 * <p><b>修复材料用 {@code Supplier}</b>：{@code Ingredient} 一旦延迟到注册事件里
 * 再解析，早期的物品引用会拿到空注册对象，所以每次调用都重新问一遍 supplier。</p>
 */
public final class PathArmorMaterial implements ArmorMaterial {

    private final PathArmorTier tier;
    /** 这档材质的<b>境界</b>（决定护甲值 / 耐久）。 */
    private final int gearTier;
    private final Supplier<Ingredient> repair;

    public PathArmorMaterial(PathArmorTier tier, int gearTier, Supplier<Ingredient> repair) {
        this.tier = tier;
        this.gearTier = gearTier;
        this.repair = repair;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return this.tier.durabilityAt(this.gearTier, type);
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.tier.defenseAt(this.gearTier, type);
    }

    @Override
    public int getEnchantmentValue() {
        return this.tier.enchantmentValue();
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repair.get();
    }

    /** 材质名 —— 同时决定护甲图层贴图路径（见类注释）。 */
    @Override
    public String getName() {
        return "hongmeng:" + this.tier.materialName();
    }

    @Override
    public float getToughness() {
        return (float) this.tier.toughness();
    }

    @Override
    public float getKnockbackResistance() {
        return (float) this.tier.knockbackResistance();
    }

    public PathArmorTier tier() {
        return this.tier;
    }
}
