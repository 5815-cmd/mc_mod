package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CrystalSpiderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>晶蛛渲染器</b> —— 骨架 {@link CrystalSpiderModel}（低伏八腿 + 宽背灵晶簇 + 4 眼）。
 *
 * <p>贴图 128×64 + {@code _glow.png}（青蓝/紫灵晶簇 + 4 只发光眼）。模型最低点 = 24
 * （八条胫节触地），不需要离地修正。</p>
 */
@OnlyIn(Dist.CLIENT)
public class CrystalSpiderRenderer extends MobRenderer<CrystalSpiderEntity, CrystalSpiderModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/crystal_spider.png");

    public CrystalSpiderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CrystalSpiderModel(ctx.bakeLayer(CrystalSpiderModel.LAYER)), 0.6F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CrystalSpiderEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(CrystalSpiderEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    @Override
    protected void scale(CrystalSpiderEntity entity, PoseStack pose, float partialTick) {
        var rank = entity.rank();
        pose.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
