package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.core.RealmStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 可穿戴的道袍护甲。
 *
 * <p>与原版的 {@code ArmorItem} 唯一的区别是 tooltip：把「第几境」「品阶」
 * 「四件套总防御」写清楚，跟 {@link com.hongmeng.xiuxian.item.gear.ArtifactItem}
 * 的提示风格保持一致。护甲值/韧性/耐久全部由
 * {@link DaoArmorMaterial} 提供，不在这里重复。</p>
 *
 * <p>护甲值本身走原版属性系统（{@code ArmorItem#getDefaultAttributeModifiers}），
 * 所以穿进原版盔甲槽就立即生效，右下角护甲条也会亮起来。</p>
 */
public class DaoArmorItem extends ArmorItem {

    /** 大境序号 1~10。 */
    private final int realmTier;
    /** 套名（中文），例如「布道」。 */
    private final String suitCn;
    /** 部位名（中文），例如「道冠」。 */
    private final String slotCn;

    public DaoArmorItem(DaoArmorMaterial material, DaoArmorTier data, int slot,
                        Properties properties) {
        super(material, DaoArmorTier.TYPES[slot], properties);
        this.realmTier = data.tier();
        this.suitCn = data.suitCn();
        this.slotCn = data.cn()[slot];
    }

    public int realmTier() {
        return realmTier;
    }

    public String suitCn() {
        return suitCn;
    }

    public String slotCn() {
        return slotCn;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\u00a77品阶：\u00a7b第 " + realmTier + " 境 "
                + "\u00a77部位：\u00a7f" + slotCn + "\u00a77（" + suitCn + "套）"));
        tooltip.add(Component.literal("\u00a77护甲：\u00a7b+" + getDefense()
                + "   \u00a77韧性：\u00a7b+" + fmt(getToughness())));
        if (getMaterial().getKnockbackResistance() > 0.0F) {
            tooltip.add(Component.literal("\u00a77击退抗性：\u00a7b+"
                    + Math.round(getMaterial().getKnockbackResistance() * 100.0F) + "%"));
        }
        tooltip.add(Component.literal("\u00a7a穿在原版盔甲槽即可生效（头盔/胸甲/护腿/靴子）"));
        tooltip.add(Component.literal("\u00a78本境全套总防御 "
                + fmt(RealmStats.armorTotal(realmTier)) + "（练气 5.6 → 鸿蒙 22.4 万）"));
    }

    private static String fmt(double v) {
        if (v >= 10_000) {
            return String.format("%.1f万", v / 10_000.0D);
        }
        return String.valueOf((long) v);
    }

}
