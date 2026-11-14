package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.FallenImmortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>堕仙渲染器</b> —— 骨架 {@link FallenImmortalModel}（残破仙袍 + 倒挂断环 + 三条撕裂摆 + 断剑）。
 *
 * <p>贴图 128×64 + {@code _glow.png}（倒环仙金、玉扣/眼/刃口青）。模型最低点 = 24
 * （中间那条撕裂摆触地），不需要离地修正。</p>
 */
@OnlyIn(Dist.CLIENT)
public class FallenImmortalRenderer extends MobRenderer<FallenImmortalEntity, FallenImmortalModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/fallen_immortal.png");

    public FallenImmortalRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FallenImmortalModel(ctx.bakeLayer(FallenImmortalModel.LAYER)), 0.6F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(FallenImmortalEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(FallenImmortalEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    @Override
    protected void scale(FallenImmortalEntity entity, PoseStack pose, float partialTick) {
        var rank = entity.rank();
        pose.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
