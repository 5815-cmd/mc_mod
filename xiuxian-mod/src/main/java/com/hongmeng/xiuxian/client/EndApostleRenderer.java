package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.EndApostleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>终界使徒渲染器</b> —— 骨架 {@link EndApostleModel}（无脚黑袍 + 尖顶兜帽 + 背后断环 + 4 浮眼）。
 *
 * <p>贴图 128×64 + {@code _glow.png}（4 浮眼紫、环缘与核心青）。模型最低点 = 24（袍摆底），
 * 不需要离地修正；飘浮感由 {@code setupAnim} 的整身 0~1px 起落做。</p>
 */
@OnlyIn(Dist.CLIENT)
public class EndApostleRenderer extends MobRenderer<EndApostleEntity, EndApostleModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/end_apostle.png");

    public EndApostleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EndApostleModel(ctx.bakeLayer(EndApostleModel.LAYER)), 0.55F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(EndApostleEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(EndApostleEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    @Override
    protected void scale(EndApostleEntity entity, PoseStack pose, float partialTick) {
        var rank = entity.rank();
        pose.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
