package com.hongmeng.xiuxian.item.gear;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 把武器的"附加攻击"真正接到属性系统上。
 *
 * <p><b>这是一个真实存在过的 bug：</b>{@link RealmWeaponItem} 里存了 {@code attack}
 * 字段（例如鸿蒙道剑 +320000），但它只被用于 tooltip 显示，
 * 从没注册成属性修饰符——玩家看到的"攻击力"是假的，实际打出来的还是原版石剑的伤害。</p>
 *
 * <h2>⚠ 这个 bug 在道途武器上<b>又发生了一次</b></h2>
 * <p>{@link ModPathWeapon.PathWeaponItem#attack()} 算出了正确的境界伤害
 * （第 1 境 8 → 第 10 境 320000），但本类原来<b>只认 {@link RealmWeaponItem}</b>，
 * 于是 50 把道途武器全都退回原版 {@code SwordItem} 的整数值（5~10）。</p>
 *
 * <p>后果比"数值偏低"严重得多：项目的伤害量级是 {@code standard(tier)}
 * （第 10 境 320 000），而道途武器打出去只有 <b>10</b> ——
 * <b>体修与血修这两条物理系道途在第 6 境之后完全打不动怪</b>。
 * 而它们的设计恰恰是"物理为主、法术为辅"。</p>
 *
 * <p>修法：让本类同时认 {@code PathWeaponItem}，把它的境界伤害也接成属性修饰符。</p>
 *
 * <p>这里监听 Forge 的 {@link ItemAttributeModifierEvent}，在物品被评估属性时
 * 动态加上 {@code ADDITION} 修饰符。用事件而不是重写
 * {@code getDefaultAttributeModifiers}，好处是不必碰原版 NBT 存档格式。</p>
 */
public final class WeaponAttributeHandler {

    /** 模组内所有武器共用一个稳定的 UUID，保证"先移除再加"，绝不叠加。 */
    private static final UUID WEAPON_ATTACK_ID = UUID.nameUUIDFromBytes(
            "hongmeng_weapon_attack".getBytes(StandardCharsets.UTF_8));
    private static final String WEAPON_ATTACK_NAME = "hongmeng_weapon_attack";

    /** 需要被事件总线实例化，所以是 public。 */
    public WeaponAttributeHandler() {
    }

    @SubscribeEvent
    public void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        // 只在主手生效，避免副手/饰品栏重复计算
        if (!event.getSlotType().equals(net.minecraft.world.entity.EquipmentSlot.MAINHAND)) {
            return;
        }
        double attack = addedAttackOf(stack);
        if (attack <= 0.0D) {
            return;
        }
        event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                WEAPON_ATTACK_ID, WEAPON_ATTACK_NAME, attack,
                AttributeModifier.Operation.ADDITION));
    }

    /**
     * 一件物品的<b>附加攻击</b>（境界量级）。两种武器都认。
     *
     * <p>道途武器的 {@code SwordItem} 伤害修饰符已被设为 0
     * （见 {@code ModPathWeapon.PathWeaponItem} 的构造），
     * 所以这里加的就是"全部"境界伤害，与 {@code realm_sword} 同一口径。</p>
     */
    public static double addedAttackOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }
        if (stack.getItem() instanceof RealmWeaponItem w) {
            return w.attack(stack);
        }
        if (stack.getItem() instanceof ModPathWeapon.PathWeaponItem w) {
            return w.attack();
        }
        return 0.0D;
    }

    /** 给面板/自测用：查一件物品**当前**的附加攻击（含品质倍率）。 */
    public static double attackOf(ItemStack stack) {
        return addedAttackOf(stack);
    }
}
