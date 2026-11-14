package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.DemonSpiderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>魔蛛渲染器</b> —— 骨架 {@link DemonSpiderModel}（8 条长腿 + 浑圆熔岩腹 + 4 眼一排）。
 *
 * <p>贴图 128×64 + {@code _glow.png}（4 只红眼 + 腹部熔岩裂橙红）。
 * 模型最低点 = 24（八条胫节末端触地），不需要离地修正。
 * 普通品阶但实体 {@code forceGlow()} 返回 true，所以 {@link RankGlowLayer} 会真的亮。</p>
 */
@OnlyIn(Dist.CLIENT)
public class DemonSpiderRenderer extends MobRenderer<DemonSpiderEntity, DemonSpiderModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/demon_spider.png");

    public DemonSpiderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DemonSpiderModel(ctx.bakeLayer(DemonSpiderModel.LAYER)), 0.6F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(DemonSpiderEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(DemonSpiderEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    @Override
    protected void scale(DemonSpiderEntity entity, PoseStack pose, float partialTick) {
        var rank = entity.rank();
        pose.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
