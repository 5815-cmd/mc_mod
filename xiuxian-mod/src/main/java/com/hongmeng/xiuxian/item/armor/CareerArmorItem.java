package com.hongmeng.xiuxian.item.armor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * <b>职业甲（5 职业 × 10 境界 × 4 件 = 200 件）的通用物品类</b>。
 *
 * <p>一件 = 「职业 + 境界 + 部位」：</p>
 * <ul>
 *   <li><b>数值固定</b>：护甲/韧性/耐久来自 {@link CareerArmorMaterial#of(int)}，
 *       每个境界一套固定值（与最早的道途甲同一套算法）；</li>
 *   <li><b>外观复用</b>：1~3 境=初阶、4~6 境=进阶、7~10 境=名器，
 *       由 {@link CareerArmorRealm#bandOf(int)} 决定
 *       （客户端 {@code CareerArmorModel} 按 {@link #set()} 取 geo/贴图）；</li>
 *   <li><b>2/4 件套</b>：必须同职业、同外观档（见 {@code CareerArmorHandler}），
 *       效果来自 {@link #set()} 的档位表。</li>
 * </ul>
 */
public class CareerArmorItem extends ArmorItem implements GeoItem {

    private static final ArmorItem.Type[] TYPES = {
            ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
            ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS
    };

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 外观/套装效果档（同境界段共用）。 */
    private final CareerArmorSet set;
    /** 境界 1~10（本件要求的最低境界）。 */
    private final int realm;
    private final int slot;

    public CareerArmorItem(CareerArmorSet set, int realm, int slot, Rarity rarity) {
        super(CareerArmorMaterial.of(realm),
                TYPES[Math.max(0, Math.min(3, slot))],
                new Properties().stacksTo(1).rarity(rarity));
        this.set = set;
        this.realm = CareerArmorRealm.clamp(realm);
        this.slot = Math.max(0, Math.min(3, slot));
    }

    /** 外观/效果档（初阶/进阶/名器）。 */
    public CareerArmorSet set() {
        return set;
    }

    /** 本件境界（1~10）。 */
    public int realm() {
        return realm;
    }

    public int slot() {
        return slot;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int armor = CareerArmorRealm.armorFor(realm, slot);
        tooltip.add(Component.literal("\u00a77职业：\u00a7b" + set.career().cn()
                + "\u00a77  境界：\u00a7e第 " + realm + " 境（"
                + CareerArmorRealm.realmCn(realm) + "）"));
        tooltip.add(Component.literal("\u00a77套装：\u00a7f" + set.suitCn()
                + "\u00a77（" + set.tier().cn() + "外观）  部位：\u00a7f"
                + CareerArmorRealm.SLOT_CN[slot]));
        tooltip.add(Component.literal(String.format(
                "\u00a77护甲：\u00a7b+%d\u00a77  韧性：\u00a7b+%.1f\u00a77  生命：\u00a7b+%.0f"
                        + "\u00a77  耐久：\u00a7f%d",
                armor, CareerArmorRealm.toughness(),
                CareerArmorRealm.healthBonusPerPiece(realm),
                CareerArmorRealm.durabilityAt(realm, slot))));
        StringBuilder two = new StringBuilder();
        for (CareerArmorSet.Effect e : set.setTwo()) {
            if (two.length() > 0) {
                two.append("\u00a77 / \u00a7f");
            }
            two.append(effectText(e));
        }
        tooltip.add(Component.literal("\u00a76[2 件套·同外观档]\u00a7f " + two));
        StringBuilder four = new StringBuilder();
        for (CareerArmorSet.Effect e : set.setFour()) {
            if (four.length() > 0) {
                four.append("\u00a77 / \u00a7f");
            }
            four.append(effectText(e));
        }
        tooltip.add(Component.literal("\u00a76[4 件套·同外观档]\u00a7f " + four));
        tooltip.add(Component.literal("\u00a78境界需求：穿比自己境界高的甲会自动收起"));
    }

    /** 把套装效果翻译成人话（比例 → 百分比，tick → 秒）。 */
    private static String effectText(CareerArmorSet.Effect e) {
        String name = switch (e.kind()) {
            case ATTACK_SPEED -> "攻速";
            case SWORD_INTENT -> "剑意获取";
            case SWORD_INTENT_FULL_DAMAGE -> "剑意满层伤害";
            case SWORD_QI_RANGE -> "剑气范围";
            case SWORD_AURA -> "剑罡转化";
            case LIFESTEAL -> "吸血";
            case BLOOD_MAX -> "血池上限";
            case STAGGER_RATIO -> "缓伤比例";
            case LOW_HP_DAMAGE -> "低血增伤";
            case BLOOD_DASH_CD -> "血遁冷却";
            case PENETRATION -> "破防";
            case DEMON_GAIN -> "魔气获取";
            case DEMON_CAP -> "魔气上限";
            case OVERFLOW_BURST -> "满溢爆发";
            case SELF_DAMAGE_CUT -> "满溢自伤削减";
            case SOUL_MARK_DURATION -> "魂印持续";
            case GHOST_REALM_RADIUS -> "鬼域范围";
            case SOUL_BURST_DAMAGE -> "魂印引爆";
            case PHASE_CD -> "虚化冷却";
            case GHOST_SLOW -> "鬼域减速";
            case KNOCKBACK_RESIST -> "抗击退";
            case SURGE_GAIN -> "激荡获取";
            case LAST_STAND_TICKS -> "锁血时长";
            case SURGE_BURST -> "激荡引爆";
            case SHOCK_RADIUS -> "震荡范围";
        };
        boolean tick = e.kind() == CareerArmorSet.EffectKind.SOUL_MARK_DURATION
                || e.kind() == CareerArmorSet.EffectKind.LAST_STAND_TICKS;
        String value = tick
                ? String.format("%.1f 秒", e.value() / 20.0D)
                : String.format("%+.0f%%", e.value() * 100.0D);
        return name + " " + value;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 5,
                state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private com.hongmeng.xiuxian.client.geo.CareerArmorRenderer renderer;

            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity entity, ItemStack stack,
                    EquipmentSlot slotType, net.minecraft.client.model.HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new com.hongmeng.xiuxian.client.geo.CareerArmorRenderer();
                }
                this.renderer.prepForRender(entity, stack, slotType, original);
                return this.renderer;
            }
        });
    }
}
