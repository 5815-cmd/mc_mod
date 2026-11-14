package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.core.RealmStats;
import com.hongmeng.xiuxian.quality.ItemQuality;
import com.hongmeng.xiuxian.quality.Quality;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 按境界分阶的武器。
 *
 * <p>品阶直接用**炼制者所需境界**命名（练气法器 / 筑基灵器 / ……），
 * 这是成熟修仙模组的通行做法——玩家一看就懂，也省去一套"凡器→灵器→宝器"的解释成本。</p>
 *
 * <p>数值来自实测规律：附击攻击 ≈ 本境标准值 × 1.0~1.4，
 * "增幅"每境 +5%（练气 0% → 鸿蒙 45%）。</p>
 *
 * <p><b>品质</b>：同一把剑也有六档品质，附加攻击与增幅都乘品质倍率
 * （凡品 ×1.00 → 神品 ×1.70）。没掷过骰子的剑天生凡品，
 * 所以既有数值一个数字都不变；掷出神品的剑是真正的"神兵"。</p>
 */
public class RealmWeaponItem extends net.minecraft.world.item.TieredItem {

    /** 大境序号 1~10。 */
    private final int realmTier;
    private final String gradeName;
    /** 附加攻击（点）。 */
    private final double attack;
    /** 增幅（倍率，0.05 = +5%）。 */
    private final double amplify;

    public RealmWeaponItem(int realmTier, String gradeName, double attack, double amplify,
                           Properties properties) {
        super(weaponTier(realmTier), properties);
        this.realmTier = realmTier;
        this.gradeName = gradeName;
        this.attack = attack;
        this.amplify = amplify;
    }

    /**
     * 把境界映射到原版工具材质，这样挖掘等级与耐久"看起来"合理。
     * 真正的强弱由 {@link #attack} 与 {@link #amplify} 决定。
     */
    private static net.minecraft.world.item.Tier weaponTier(int realmTier) {
        if (realmTier <= 2) {
            return Tiers.STONE;
        }
        if (realmTier <= 3) {
            return Tiers.IRON;
        }
        if (realmTier <= 5) {
            return Tiers.DIAMOND;
        }
        return Tiers.NETHERITE;
    }

    public int realmTier() {
        return realmTier;
    }

    public String gradeName() {
        return gradeName;
    }

    public double attack() {
        return attack;
    }

    public double amplify() {
        return amplify;
    }

    /** 这把剑的有效品质（NBT → 天生凡品）。 */
    public Quality qualityOf(ItemStack stack) {
        return ItemQuality.of(stack);
    }

    /** 品质倍率（凡品 = 1.00）。 */
    public double qualityMultiplier(ItemStack stack) {
        return ItemQuality.gearMultiplier(stack);
    }

    /** **实际**附加攻击：基础攻击 × 品质倍率。 */
    public double attack(ItemStack stack) {
        return attack * qualityMultiplier(stack);
    }

    /** **实际**增幅：基础增幅 × 品质倍率（鸿蒙道剑神品 = 45% × 1.70 = 76.5%）。 */
    public double amplify(ItemStack stack) {
        return amplify * qualityMultiplier(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        Quality quality = qualityOf(stack);
        tooltip.add(Component.literal("\u00a77品阶：\u00a7b" + gradeName));
        tooltip.add(Component.literal("\u00a77品质：" + quality.colored()
                + (quality.rank() > 0
                ? "  \u00a77兵刃属性 " + quality.gearText()
                + (ItemQuality.isRolled(stack) ? "  \u00a78(掷出)" : "") : "")));
        tooltip.add(Component.literal("\u00a77附加攻击：\u00a7c+" + fmt(attack(stack))
                + (quality.rank() > 0 ? "  \u00a78(基础 " + fmt(attack) + ")" : "")));
        tooltip.add(Component.literal("\u00a77增幅：\u00a7e+"
                + (int) Math.round(amplify(stack) * 100.0D) + "% \u00a78(持有时加成到总攻击)"));
        tooltip.add(Component.literal("\u00a78本境标准值 " + RealmStats.standard(realmTier)));
    }

    private static String fmt(double v) {
        if (v >= 10_000) {
            return String.format("%.1f万", v / 10_000.0D);
        }
        return String.valueOf((long) v);
    }

    /** 给外部（伤害计算）取值用。 */
    public static double amplifyOf(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof RealmWeaponItem w) {
            return w.amplify(main);
        }
        return 0.0D;
    }
}
