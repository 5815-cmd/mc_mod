package com.hongmeng.xiuxian.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * <b>灵犀剑（GeckoLib 版）</b>：12 段弧刃骨骼 + 挥砍动画 + 宝石发光。
 *
 * <p>和旧版（静态 JSON 模型）的区别：整把剑是一个<b>骨架</b>，
 * 每段刃都是独立骨骼，可以做出"刃身逐段延迟"的挥砍感。</p>
 */
public class SpiritSwordGeoItem extends SwordItem implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SWING = RawAnimation.begin().thenPlay("swing");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpiritSwordGeoItem(Properties properties) {
        super(Tiers.DIAMOND, 3, -2.4F, properties);
    }

    /**
     * <b>客户端接线</b>：把 GeckoLib 渲染器挂到这个物品上。
     *
     * <p>4.8.4 的 {@code GeoItem} 没有 {@code createGeoRenderer}；
     * 正确做法是覆盖 {@code Item#initializeClient}，
     * 由 {@code getCustomRenderer()} 交出 {@code GeoItemRenderer}。
     * 漏了这一步，物品会渲染成"缺失模型"的紫黑方块。</p>
     */
    @Override
    public void initializeClient(java.util.function.Consumer<
            net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common
                .IClientItemExtensions() {
            private com.hongmeng.xiuxian.client.geo.GeoWeaponRenderer<SpiritSwordGeoItem>
                    renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
            getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new com.hongmeng.xiuxian.client.geo
                            .GeoWeaponRenderer<>(new com.hongmeng.xiuxian.client.geo
                                    .SpiritSwordGeoModel());
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2,
                state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(this, "attack", 0,
                state -> state.setAndContinue(SWING)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
