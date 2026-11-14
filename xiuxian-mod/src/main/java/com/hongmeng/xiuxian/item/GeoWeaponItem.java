package com.hongmeng.xiuxian.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * <b>通用 GeckoLib 武器</b>：一把武器 = 一个 family 名 + 一套 geo/贴图/动画。
 *
 * <p>这样 50 把武器只需要 5 个 family，不用写 50 个类。</p>
 */
public class GeoWeaponItem extends SwordItem implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    /**
     * 长武器共用底座，短武器（阵盘）需要单独放大。
     * 只影响体积，不影响握点（握点由 GeoWeaponRenderer 的 TP_* 决定）。
     */
    private static final java.util.Map<String, Float> SCALE = java.util.Map.of();

    private final String family;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GeoWeaponItem(String family, double attackDamage, double speed) {
        super(Tiers.NETHERITE, (int) Math.round(attackDamage) - 1,
                (float) speed - 4.0F, new Item.Properties().stacksTo(1));
        this.family = family;
    }

    public String family() {
        return this.family;
    }

    /**
     * family -> 体积系数。
     *
     * <p><b>必须容忍 null</b>：Forge 有可能在很早期就调用 {@code initializeClient}，
     * 那一刻 {@code family} 还没赋值。所以这里既要用普通 Map.get（不能用
     * {@code Map.of().getOrDefault()}，它对 null key 会直接抛 NPE），
     * 又要保证真正取值的时机推迟到第一次渲染。</p>
     */
    private static float scaleOf(String fam) {
        if (fam == null) {
            return 1.0F;
        }
        Float v = SCALE.get(fam);
        return v == null ? 1.0F : v.floatValue();
    }

    /**
     * family -> 模型原始长度（单位 1/16 格），仅用于物品栏 GUI 自适应缩放。
     * <p>数值取各型最长一件的包围盒高度（见 build_weapons_v2 / build_war_scythes 输出）。</p>
     */
    /** career_<form>_t<n> -> form（sword/greatsword/scythe/spear/staff/war_scythe）。 */
    private static String formOf(String fam) {
        if (fam == null) {
            return "";
        }
        if (fam.startsWith("career_war_scythe_")) {
            return "war_scythe";
        }
        String rest = fam.startsWith("career_") ? fam.substring("career_".length()) : fam;
        int i = rest.indexOf('_');
        return i < 0 ? rest : rest.substring(0, i);
    }

    private static float guiLengthOf(String fam) {
        if (fam == null) {
            return 45.0F;
        }
        if (fam.startsWith("career_war_scythe_")) {
            return 39.0F;
        }
        if (fam.startsWith("career_sword_")) {
            return 45.0F;
        }
        if (fam.startsWith("career_greatsword_")) {
            return 58.0F;
        }
        if (fam.startsWith("career_scythe_")) {
            return 33.0F;
        }
        if (fam.startsWith("career_spear_")) {
            return 39.0F;
        }
        if (fam.startsWith("career_staff_")) {
            return 30.0F;
        }
        return 45.0F;
    }

    @Override
    public void registerControllers(
            software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "idle", 6, s -> s.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** 客户端接线：GeckoLib 渲染器（1.20.1 走 initializeClient）。 */
    @Override
    public void initializeClient(java.util.function.Consumer<
            net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common
                .IClientItemExtensions() {
            private com.hongmeng.xiuxian.client.geo.GeoWeaponRenderer<GeoWeaponItem>
                    renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
            getCustomRenderer() {
                if (this.renderer == null) {
                    // 关键：在这里（第一次真正渲染时）才读 family，
                    // 不能在外层 initializeClient 时抓取 —— 那时可能是 null。
                    String fam = GeoWeaponItem.this.family;
                    if (fam == null) {
                        // 兜底：注册名就是 family，直接从注册表反查，绝不会再是 null
                        fam = net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(GeoWeaponItem.this).getPath();
                    }
                    this.renderer = new com.hongmeng.xiuxian.client.geo
                            .GeoWeaponRenderer<>(new com.hongmeng.xiuxian.client.geo
                                    .GeoWeaponModel(fam))
                            .withModelScale(scaleOf(fam))
                            .withGuiLength(guiLengthOf(fam))
                            .withForm(formOf(fam));
                }
                return this.renderer;
            }
        });
    }
}
