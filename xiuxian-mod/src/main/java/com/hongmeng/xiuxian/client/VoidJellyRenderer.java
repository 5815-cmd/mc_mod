package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.VoidJellyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>虚空水母渲染器</b> —— 骨架 {@link VoidJellyModel}（3 层钟形伞盖 + 4 条两段触须 + 虚空眼）。
 *
 * <p>贴图 64×64 + {@code _glow.png}（紫伞缘光 + 青蓝眼瞳）。模型最低点 = 24（触须末端触地），
 * 不需要离地修正；伞盖一开一合的飘浮感由 {@code setupAnim} 的 xScale/zScale 脉动做。</p>
 */
@OnlyIn(Dist.CLIENT)
public class VoidJellyRenderer extends MobRenderer<VoidJellyEntity, VoidJellyModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/void_jelly.png");

    public VoidJellyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VoidJellyModel(ctx.bakeLayer(VoidJellyModel.LAYER)), 0.5F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(VoidJellyEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(VoidJellyEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    @Override
    protected void scale(VoidJellyEntity entity, PoseStack pose, float partialTick) {
        var rank = entity.rank();
        pose.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
