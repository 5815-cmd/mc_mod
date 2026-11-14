package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.MarshWraithEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>泽魅渲染器</b> —— 骨架 {@link MarshWraithModel}（破斗笠 + 破袍堆 + 青磷灯笼）。
 *
 * <p>贴图 64×64 + {@code _glow.png}（青磷灯 / 双点青光）。模型最低点 = 24（袍角触地），
 * 所以不需要离地修正；飘浮感由 {@code setupAnim} 的整身 0.5px 起落做。</p>
 */
@OnlyIn(Dist.CLIENT)
public class MarshWraithRenderer extends MobRenderer<MarshWraithEntity, MarshWraithModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/marsh_wraith.png");

    public MarshWraithRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MarshWraithModel(ctx.bakeLayer(MarshWraithModel.LAYER)), 0.45F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(MarshWraithEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(MarshWraithEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    @Override
    protected void scale(MarshWraithEntity entity, PoseStack pose, float partialTick) {
        var rank = entity.rank();
        pose.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
