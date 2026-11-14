package com.hongmeng.xiuxian.client.geo;

import com.hongmeng.xiuxian.item.GeoWeaponItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 按 family 定位 geo / 贴图 / 动画。 */
public class GeoWeaponModel extends GeoModel<GeoWeaponItem> {

    private final ResourceLocation geo;
    private final ResourceLocation tex;
    private final ResourceLocation anim;

    public GeoWeaponModel(String family) {
        this.geo = new ResourceLocation("hongmeng", "geo/" + family + ".geo.json");
        this.tex = new ResourceLocation("hongmeng", "textures/item/" + family + ".png");
        this.anim = new ResourceLocation("hongmeng",
                "animations/" + family + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(GeoWeaponItem animatable) {
        return this.geo;
    }

    @Override
    public ResourceLocation getTextureResource(GeoWeaponItem animatable) {
        return this.tex;
    }

    @Override
    public ResourceLocation getAnimationResource(GeoWeaponItem animatable) {
        return this.anim;
    }
}
