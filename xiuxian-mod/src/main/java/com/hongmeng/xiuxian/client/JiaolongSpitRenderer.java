package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.beast.JiaolongSpitEntity;
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
 * 蛟龙之息的渲染器（替换原来的 {@code ThrownItemRenderer}）。
 *
 * <p>模型 {@link JiaolongSpitModel} 是 6 个盒子的扁平"青绿灵叶"，最低点按离线审计
 * 摆在 y=24；这里先整体上移 <b>23/16 格</b>把叶心挪到实体中心（hitbox 0.5×0.5），
 * 再让叶片绕着飞行方向慢慢翻 —— 一片被吐出来的灵叶，而不是一块飞行的物品。</p>
 */
@OnlyIn(Dist.CLIENT)
public class JiaolongSpitRenderer extends EntityRenderer<JiaolongSpitEntity> {

    /** 模型层：LayerDefinition 由 {@link JiaolongSpitModel#createBodyLayer()} 提供（需在 ClientSetup 注册）。 */
    public static final ModelLayerLocation LAYER = JiaolongSpitModel.LAYER;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/jiaolong_spit.png");

    /** 模型叶心在模型空间里的 y（叶片 y 20..24）。 */
    private static final double CENTER_Y = 23.0D / 16.0D;

    private final JiaolongSpitModel model;

    public JiaolongSpitRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new JiaolongSpitModel(context.bakeLayer(LAYER));
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(JiaolongSpitEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, CENTER_Y, 0.0D);
        // 叶片翻滚：绕自身长轴慢慢转，显得轻
        float spin = (entity.tickCount + partialTick) * 11.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw + spin * 0.5F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(JiaolongSpitEntity entity) {
        return TEXTURE;
    }
}
