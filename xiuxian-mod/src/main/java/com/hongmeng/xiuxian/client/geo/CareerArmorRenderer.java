package com.hongmeng.xiuxian.client.geo;

import com.hongmeng.xiuxian.item.armor.CareerArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * 职业甲渲染器：GeckoLib {@link GeoArmorRenderer} + 自发光层。
 *
 * <p>动态件（呼吸灯 fx / 披风 cape / 纱幔 veil / 魂灯 lantern）是额外骨骼，
 * GeckoLib 默认按 8 个铠甲槽位名隐藏"非标准骨"——这里在
 * {@link #applyBoneVisibilityBySlot} 里把动态骨重新显示出来。</p>
 */
public class CareerArmorRenderer extends GeoArmorRenderer<CareerArmorItem> {

    public CareerArmorRenderer() {
        super(new CareerArmorModel());
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        super.applyBoneVisibilityBySlot(slot);
        for (CoreGeoBone bone : this.getGeoModel().getAnimationProcessor().getRegisteredBones()) {
            String name = bone.getName();
            if (name.equals("fx") || name.equals("cape") || name.equals("veil")
                    || name.equals("lantern_l") || name.equals("lantern_r")) {
                bone.setHidden(false);
            }
        }
    }
}
