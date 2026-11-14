package com.hongmeng.xiuxian.client.geo;

import com.hongmeng.xiuxian.item.SpiritSwordGeoItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 灵犀剑（十二段弧刃）的 GeckoLib 模型。
 *
 * <p><b>不要用 {@code DefaultedItemGeoModel}</b>：它会把路径自动拼成
 * {@code geo/item/xxx.geo.json} 和 {@code animations/item/xxx.animation.json}，
 * 而本项目的资源放在 {@code geo/}、{@code animations/} 根目录下——
 * 一旦物品真的走 {@code renderByItem}，就会抛
 * {@code GeckoLibException: ... Unable to find model} 直接崩客户端。</p>
 *
 * <p>所以这里和 {@link GeoWeaponModel} 一样，全部写死绝对路径。</p>
 */
public class SpiritSwordGeoModel extends GeoModel<SpiritSwordGeoItem> {

    private static final ResourceLocation GEO =
            new ResourceLocation("hongmeng", "geo/spirit_sword_v4.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("hongmeng", "textures/item/spirit_sword_v4.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation("hongmeng", "animations/spirit_sword.animation.json");

    @Override
    public ResourceLocation getModelResource(SpiritSwordGeoItem animatable) {
        return GEO;
    }

    @Override
    public ResourceLocation getTextureResource(SpiritSwordGeoItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SpiritSwordGeoItem animatable) {
        return ANIMATION;
    }
}
