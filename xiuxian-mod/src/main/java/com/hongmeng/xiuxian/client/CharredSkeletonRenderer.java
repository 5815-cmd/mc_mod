package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CharredSkeletonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>焦骨骷髅渲染器</b> —— 骨架 {@link CharredSkeletonModel}
 * （炭化骨架 + 断肋余烬 + 左手短弓 + 右手燃烧箭 + 背后皮箭袋）。
 *
 * <p>贴图 64×64 + {@code _glow.png}（橙红眼窝/断肋余烬 + 金燃烧箭头）。
 * 模型最低点 = 24（双足触地），不需要离地修正。
 * 普通品阶但实体 {@code forceGlow()} 返回 true，所以 {@link RankGlowLayer} 会真的亮。</p>
 */
@OnlyIn(Dist.CLIENT)
public class CharredSkeletonRenderer
        extends MobRenderer<CharredSkeletonEntity, CharredSkeletonModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/charred_skeleton.png");

    public CharredSkeletonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CharredSkeletonModel(ctx.bakeLayer(CharredSkeletonModel.LAYER)), 0.5F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CharredSkeletonEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(CharredSkeletonEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    @Override
    protected void scale(CharredSkeletonEntity entity, PoseStack pose, float partialTick) {
        var rank = entity.rank();
        pose.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
