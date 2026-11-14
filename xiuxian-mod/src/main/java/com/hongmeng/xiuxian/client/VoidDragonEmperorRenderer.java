package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 渲染器（骨架见 VoidDragonEmperorModel）。 */
public class VoidDragonEmperorRenderer extends MobRenderer<CultivationMonsterEntity, VoidDragonEmperorModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    new ResourceLocation(HongmengMod.MOD_ID, "void_dragon_emperor"), "main");

    private static final ResourceLocation TEX =
            new ResourceLocation(HongmengMod.MOD_ID, "textures/entity/void_dragon_emperor.png");

    public VoidDragonEmperorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VoidDragonEmperorModel(ctx.bakeLayer(LAYER)), 3.0F);
        // 第六轮重制：品阶发光层（void_dragon_emperor_glow.png）——眼/角尖/翼骨裂纹/尾刃
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationMonsterEntity entity) {
        return TEX;
    }
}
