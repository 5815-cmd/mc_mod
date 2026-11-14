package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.VenomSpitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 毒涎的渲染器（替换原来的 {@code ThrownItemRenderer}：以前是把
 * {@code xueluo_zhi_petal} 那个物品当石头扔出去，现在是自己的"暗红蟾涎球"）。
 *
 * <p>模型 {@link VenomSpitModel} 是 6 个盒子的小球，最低点按离线审计摆在 y=24；
 * 这里先整体上移 <b>20/16 格</b>把<b>球心</b>挪到实体中心（hitbox 0.5×0.5），
 * 再让它在飞行里慢慢翻滚 —— 飞行中一直在鼓（缩放脉动，见模型的 setupAnim）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class VenomSpitRenderer extends EntityRenderer<VenomSpitEntity> {

    /** 模型层：LayerDefinition 由 {@link VenomSpitModel#createBodyLayer()} 提供（需在 ClientSetup 注册）。 */
    public static final ModelLayerLocation LAYER = VenomSpitModel.LAYER;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/venom_spit.png");

    /** 模型球心在模型空间里的 y（球 y 16..24）。 */
    private static final double CENTER_Y = 20.0D / 16.0D;

    private final VenomSpitModel model;

    public VenomSpitRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new VenomSpitModel(context.bakeLayer(LAYER));
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(VenomSpitEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // 模型的"脚底"在 y=24，先把自己抬到球心落在实体中心
        poseStack.translate(0.0D, CENTER_Y, 0.0D);
        // 飞行中慢慢翻滚：两颗轴不对称，看起来像一口黏稠的涎
        float spin = (entity.tickCount + partialTick) * 16.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.55F));

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(VenomSpitEntity entity) {
        return TEXTURE;
    }
}
