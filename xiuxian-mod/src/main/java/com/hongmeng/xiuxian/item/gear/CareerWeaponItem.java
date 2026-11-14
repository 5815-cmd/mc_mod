package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.core.RealmStats;
import com.hongmeng.xiuxian.item.GeoWeaponItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>通用双手武器（5 型 × 10 阶 = 50 件）的通用物品类</b>——R-2 装备线。
 *
 * <p>一件 = 「型 {@link WeaponForm} + 阶」：
 * 攻击 = {@code RealmStats.standard(阶) × 型伤害系数}，
 * 攻速 = {@code 1.6（原版剑速）× 型攻速系数}，由 {@link GeoWeaponItem}
 * 的属性修饰符挂到主手；外观 family = {@code career_<form>_<low|mid|high>}
 * （15 个模型覆盖 50 件，低阶 1~3 / 中阶 4~6 / 高阶 7~10 共用一个外观档）。</p>
 *
 * <p><b>五型全部双手、不锁职业</b>；禁副手由 R-3 的 {@code OffhandGate} 接线，
 * 这里先暴露 {@link #isTwoHanded()} 查询。</p>
 */
public class CareerWeaponItem extends GeoWeaponItem {

    private final WeaponForm form;
    private final int tier;

    public CareerWeaponItem(String family, WeaponForm form, int tier) {
        super(family, RealmStats.standard(tier) * form.damageFactor(),
                1.6D * form.speedFactor());
        this.form = form;
        this.tier = tier;
    }

    /** 武器五型（数据契约，供 R-3 的攻击距离/群伤/禁副手接线使用）。 */
    public WeaponForm form() {
        return form;
    }

    /** 阶位 1~10（练气~鸿蒙）。 */
    public int tier() {
        return tier;
    }

    /** 是否双手：五型全部双手，持有时副手失效（R-3 接线）。 */
    public boolean isTwoHanded() {
        return form.twoHanded();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77类型：\u00a7b" + form.cn()
                + "\u00a77  阶位：\u00a7f第 " + tier + " 境"
                + "\u00a77  双手：\u00a7f是"));
        tooltip.add(Component.literal(String.format(
                "\u00a77攻击：\u00a7b%.0f\u00a77  攻速：\u00a7b×%.2f\u00a77  距离：\u00a7b%.1f 格"
                        + "\u00a77  群伤：\u00a7b%s",
                RealmStats.standard(tier) * form.damageFactor(),
                form.speedFactor(), form.reach(),
                form.sweeping() ? "扇形" : "单体")));
        tooltip.add(Component.literal("\u00a78动作分型：" + form.action()
                + "\u00a77 ｜ 不锁职业；持握时副手失效（R-3 接线）"));
        tooltip.add(Component.literal("\u00a78境界需求：\u00a7e第 " + tier
                + " 境\u00a78（境界不足会被自动收起）"));
    }
}
