package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.HeavenLordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>仙庭之主渲染器</b> —— 巨型 Boss 骨架 {@link HeavenLordModel}。
 *
 * <p>贴图 256×256 + {@code _glow.png}（三眼 / 日轮 / 冠前玉 / 玉圭首）。</p>
 *
 * <p><b>巨化不在这里乘</b>：模型本身 110px（6.875 格），品阶倍率（OVERLORD ×1.32/×1.20）
 * 由本类 {@link #scale} 给，B 的统一巨化系统（giant 1.35）由 {@code BossPhaseRenderer}
 * 在外面乘一次——这里再乘会重复放大。</p>
 *
 * <p>离地修正 {@code GROUND_LIFT = (LOWEST_PIXEL_Y - 24) / 16 = 0}（袍摆底就在 y=24）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class HeavenLordRenderer extends MobRenderer<HeavenLordEntity, HeavenLordModel> {

    /** 离地修正量（格）：模型最低点像素 y 与 24 的差值。本模型 = 0。 */
    private static final double GROUND_LIFT =
            (HeavenLordModel.LOWEST_PIXEL_Y - 24) / 16.0D;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/heaven_lord.png");

    public HeavenLordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HeavenLordModel(ctx.bakeLayer(HeavenLordModel.LAYER)), 1.6F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(HeavenLordEntity entity) {
        return TEXTURE;
    }

    /** 离地修正必须放在 setupRotations 里（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(HeavenLordEntity entity, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    protected boolean shouldShowName(HeavenLordEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 只乘品阶倍率；巨化（giant）由 B 的统一系统在外面乘一次。 */
    @Override
    protected void scale(HeavenLordEntity entity, PoseStack poseStack, float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
