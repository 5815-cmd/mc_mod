package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.StarDustWraithEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>星尘游魂渲染器</b> —— 骨架 {@link StarDustWraithModel}
 * （无腿人形 + 3 条星尘尾 + 胸口星核 + 头顶 3 星点）。
 *
 * <p>贴图 64×64 + {@code _glow.png}（星核 #7FE3F2 / 双手+星点+尾端+眼睛 #C9A0FF）。
 * <b>精英阶自带发光眼</b>：{@link RankGlowLayer} 在 {@code MobRank.ELITE} 上自动生效，
 * 不需要 {@code forceGlow()}。</p>
 *
 * <p>模型最低点 = 24（3 条星尘尾尾端贴地），所以不需要离地修正；
 * 飘浮感由 {@code setupAnim} 的整身 0~1px 起落做（静态模型仍严格踩线）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class StarDustWraithRenderer
        extends MobRenderer<StarDustWraithEntity, StarDustWraithModel> {

    public StarDustWraithRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new StarDustWraithModel(ctx.bakeLayer(StarDustWraithModel.LAYER)), 0.4F);
        // 精英/队长等的品阶发光层：星核、双手、头顶星点、尾端、眼睛都在 _glow.png 里
        this.addLayer(new RankGlowLayer<>(this));
    }

    /**
     * 贴图由实体按注册名给（{@code CultivationMonsterEntity#textureLocation()}），
     * 与项目里其它人形怪同一套约定：一只怪一张贴图，改名只改注册名一处。
     */
    @Override
    public ResourceLocation getTextureLocation(StarDustWraithEntity entity) {
        return entity.textureLocation();
    }

    @Override
    protected boolean shouldShowName(StarDustWraithEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：精英 ×1.05 / ×1.03（倍率表在 {@code MobRank}，不写死数字）。 */
    @Override
    protected void scale(StarDustWraithEntity entity, PoseStack pose, float partialTick) {
        var rank = entity.rank();
        pose.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
