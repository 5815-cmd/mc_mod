package com.hongmeng.xiuxian.item.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * <b>职业甲材质（一境一材质）</b>。
 *
 * <p>改成"按境界固定数值"之后，护甲/韧性不再由 Handler 动态加，
 * 而是像最早的道途甲一样写进 {@link ArmorMaterial}：
 * 每个境界一个材质实例（{@link #of(int)}），数值见 {@link CareerArmorRealm}。</p>
 *
 * <p>四件护甲共用同境界材质；不同职业同境界的数值相同（差异只在套装效果/外观）。</p>
 */
public final class CareerArmorMaterial implements ArmorMaterial {

    private static final CareerArmorMaterial[] BY_REALM = new CareerArmorMaterial[11];

    static {
        for (int r = 1; r <= CareerArmorRealm.MAX_REALM; r++) {
            BY_REALM[r] = new CareerArmorMaterial(r);
        }
    }

    private final int realm;

    private CareerArmorMaterial(int realm) {
        this.realm = CareerArmorRealm.clamp(realm);
    }

    /** 取某个境界的材质（单例，不重复建）。 */
    public static CareerArmorMaterial of(int realm) {
        return BY_REALM[CareerArmorRealm.clamp(realm)];
    }

    public int realm() {
        return realm;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return CareerArmorRealm.durabilityAt(realm, CareerArmorRealm.indexOf(type));
    }

    /** 固定护甲值：按境界 + 部位（与最早道途甲同一分配比例）。 */
    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return CareerArmorRealm.armorFor(realm, CareerArmorRealm.indexOf(type));
    }

    @Override
    public int getEnchantmentValue() {
        return CareerArmorRealm.enchantmentValue();
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.IRON_INGOT);
    }

    @Override
    public String getName() {
        return "hongmeng:career_armor_realm_" + realm;
    }

    @Override
    public float getToughness() {
        return (float) CareerArmorRealm.toughness();
    }

    @Override
    public float getKnockbackResistance() {
        return (float) CareerArmorRealm.knockbackResistance();
    }
}
