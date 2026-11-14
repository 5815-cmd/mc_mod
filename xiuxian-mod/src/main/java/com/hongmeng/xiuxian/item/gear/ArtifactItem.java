package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.quality.ItemQuality;
import com.hongmeng.xiuxian.quality.Quality;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 法宝 / 法器：**装备在「法宝栏」五个槽位里才生效**的通用饰品。
 *
 * <p>十境 × 五部位（法冠 / 道袍 / 法裤 / 云履 / 玉佩）。数值表见
 * {@link ArtifactStats}——它是纯数据类，护甲 / 韧性 / 生命% / 攻击% 全部按
 * 「本境标准值 × 系数」锚定，这样每一境的法宝都有明确收益：</p>
 *
 * <ul>
 *   <li><b>护甲</b>：五件合计 = 本境标准值 × 0.276（低于职业甲与道袍，
 *       前期补足防御，4 境后与职业甲一起顶到 92% 减伤封顶）；</li>
 *   <li><b>生命上限</b>：五件合计 +6% / +8% / +10%（按外观档）；</li>
 *   <li><b>攻击</b>：五件合计 +6% / +8% / +10%（按外观档）；</li>
 *   <li><b>韧性</b>：与护甲同口径的"手感数值"，单件很小。</li>
 * </ul>
 *
 * <p><b>品质</b>：法宝也吃六档品质（凡品 ×1.00 → 神品 ×1.70），乘在上面的
 * 全部数值上；没有 NBT 的品质时天生凡品（×1.00）。
 * 带品质的取值方法是 {@link #armor(ItemStack)} / {@link #toughness(ItemStack)} /
 * {@link #healthPercent(ItemStack)} / {@link #attackPercent(ItemStack)}，
 * 不带参数的旧方法返回基础值。</p>
 *
 * <p><b>境界门槛</b>：第 N 境法宝需要玩家达到第 N 境才能装备（{@link RealmGear}），
 * 所以"每个境界都有法宝可以穿"是真实的成长线，而不是练气期直接穿鸿蒙至宝。</p>
 */
public class ArtifactItem extends Item {

    /** 大境序号 1~10。 */
    private final int realmTier;
    private final String gradeName;
    /** 部位名：法冠 / 道袍 / 法裤 / 云履 / 玉佩。 */
    private final String slotName;
    /** 基础护甲值（未乘品质）。 */
    private final double armor;
    /** 基础韧性（未乘品质）。 */
    private final double toughness;
    /** 生命上限加成比例（未乘品质；0.03 = +3%）。 */
    private final double healthPercent;
    /** 攻击加成比例（未乘品质）。 */
    private final double attackPercent;

    public ArtifactItem(int realmTier, String gradeName, String slotName,
                        double armor, double toughness,
                        double healthPercent, double attackPercent,
                        Properties properties) {
        super(properties);
        this.realmTier = realmTier;
        this.gradeName = gradeName;
        this.slotName = slotName;
        this.armor = armor;
        this.toughness = toughness;
        this.healthPercent = healthPercent;
        this.attackPercent = attackPercent;
    }

    public int realmTier() {
        return realmTier;
    }

    public String gradeName() {
        return gradeName;
    }

    public String slotName() {
        return slotName;
    }

    public double armor() {
        return armor;
    }

    public double toughness() {
        return toughness;
    }

    public double healthPercent() {
        return healthPercent;
    }

    public double attackPercent() {
        return attackPercent;
    }

    /** 这一件法宝的有效品质（NBT → 天生凡品）。 */
    public Quality qualityOf(ItemStack stack) {
        return ItemQuality.of(stack);
    }

    /** 这件法宝的品质倍率（凡品 = 1.00，不影响老数值）。 */
    public double qualityMultiplier(ItemStack stack) {
        return ItemQuality.gearMultiplier(stack);
    }

    /** **实际**护甲：基础护甲 × 品质倍率。 */
    public double armor(ItemStack stack) {
        return armor * qualityMultiplier(stack);
    }

    /** **实际**韧性：基础韧性 × 品质倍率。 */
    public double toughness(ItemStack stack) {
        return toughness * qualityMultiplier(stack);
    }

    /** **实际**生命上限加成比例：基础值 × 品质倍率。 */
    public double healthPercent(ItemStack stack) {
        return healthPercent * qualityMultiplier(stack);
    }

    /** **实际**攻击加成比例：基础值 × 品质倍率。 */
    public double attackPercent(ItemStack stack) {
        return attackPercent * qualityMultiplier(stack);
    }

    /**
     * 这件法宝对应的装备槽位 key（英文，例如 {@code crown}）。
     *
     * <p><b>必须返回英文 key，不能返回中文部位名。</b>槽位表
     * （{@link EquipmentSlots}）用的是 {@code crown/robe/pants/boots/jade}，
     * 返回"法冠"的话查表永远返回 -1，装备栏会整个失效。</p>
     */
    public String slotKey() {
        return GearNames.slotKey(slotName);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        Quality quality = qualityOf(stack);
        tooltip.add(Component.literal("\u00a77品阶：\u00a7b" + gradeName
                + "   \u00a77部位：\u00a7f" + slotName));
        tooltip.add(Component.literal("\u00a77品质：" + quality.colored()
                + (quality.rank() > 0
                ? "  \u00a77法宝属性 " + quality.gearText()
                + (ItemQuality.isRolled(stack) ? "  \u00a78(掷出)" : "") : "")));
        tooltip.add(Component.literal("\u00a77护甲：\u00a7b+" + fmt(armor(stack))
                + "   \u00a77韧性：\u00a7b+" + fmt(toughness(stack))));
        tooltip.add(Component.literal("\u00a77生命上限：\u00a7c+"
                + pct(healthPercent(stack))
                + "   \u00a77攻击：\u00a7c+" + pct(attackPercent(stack))));
        tooltip.add(Component.literal(RealmGear.requirementText(stack)));
        tooltip.add(Component.literal("\u00a78本境五件合计：护甲 \u00a7b+"
                + fmt(ArtifactStats.setArmor(realmTier))
                + "\u00a78 · 生命 \u00a7c+" + pct(ArtifactStats.setHealthPercent(realmTier))
                + "\u00a78 · 攻击 \u00a7c+" + pct(ArtifactStats.setAttackPercent(realmTier))));
        tooltip.add(Component.literal("\u00a7a在背包界面右侧的「法宝」栏装备后生效"));
        tooltip.add(Component.literal("\u00a78左键点法宝栏里的空格位会自动装上"));
    }

    private static String fmt(double v) {
        if (v >= 10_000) {
            return String.format("%.1f万", v / 10_000.0D);
        }
        return String.valueOf((long) v);
    }

    private static String pct(double ratio) {
        double p = ratio * 100.0D;
        if (Math.abs(p - Math.rint(p)) < 1.0E-6D) {
            return String.valueOf((long) Math.rint(p)) + "%";
        }
        return String.format("%.1f%%", p);
    }
}
