package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 渲染器（骨架见 BlazeEmperorModel）。 */
public class BlazeEmperorRenderer extends MobRenderer<CultivationMonsterEntity, BlazeEmperorModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    new ResourceLocation(HongmengMod.MOD_ID, "blaze_emperor"), "main");

    private static final ResourceLocation TEX =
            new ResourceLocation(HongmengMod.MOD_ID, "textures/entity/blaze_emperor.png");

    public BlazeEmperorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BlazeEmperorModel(ctx.bakeLayer(LAYER)), 2.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationMonsterEntity entity) {
        return TEX;
    }
}
