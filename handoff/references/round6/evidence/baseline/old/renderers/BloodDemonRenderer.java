package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>血魔老祖渲染器</b> —— 独立骨架 {@link BloodDemonModel}（三头六臂）。
 *
 * <h2>离地修正</h2>
 * <p>原版画模型前 {@code translate(0, -1.501, 0)}（≈24.016 像素）→
 * <b>模型最低点必须停在 y=24</b>。{@link BloodDemonModel} 的最低点是三条腿的盒底，
 * 落在 {@code y=24}，所以修正量为 <b>0</b>。</p>
 *
 * <p>⚠ 修正必须放在 {@code setupRotations} 里（放 {@code scale} 里会与动画叠加）。
 * 数值由 {@code tools/audit_model_ground.py} 实测复核。</p>
 */
public class BloodDemonRenderer
        extends MobRenderer<CultivationMonsterEntity, BloodDemonModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "blood_demon"), "main");

    /** 离地修正量（格）。= (模型最低点像素 y − 24) / 16。 */
    private static final double GROUND_LIFT =
            (BloodDemonModel.LOWEST_PIXEL_Y - 24) / 16.0D;

    private final ResourceLocation texture;

    public BloodDemonRenderer(EntityRendererProvider.Context ctx, String texName) {
        super(ctx, new BloodDemonModel(ctx.bakeLayer(LAYER)), 1.0F);
        this.texture = new ResourceLocation(HongmengMod.MOD_ID,
                "textures/entity/" + texName + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationMonsterEntity entity) {
        return this.texture;
    }

    @Override
    protected void setupRotations(CultivationMonsterEntity entity, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    protected void scale(CultivationMonsterEntity entity, PoseStack poseStack,
                         float partialTicks) {
        MobRank rank = entity.effectiveRank();
        // 三头六臂本来就够大，只放大一点
        poseStack.scale(rank.scaleXZ() * 1.10F, rank.scaleY() * 1.10F,
                rank.scaleXZ() * 1.10F);
    }
}
