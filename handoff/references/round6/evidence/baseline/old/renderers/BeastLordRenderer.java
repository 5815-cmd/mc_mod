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
 * <b>万兽之主渲染器</b> —— 独立骨架 {@link BeastLordModel}（四足兽形）。
 *
 * <h2>离地修正</h2>
 * <p>原版画模型前 {@code translate(0, -1.501, 0)}（≈24.016 像素）→
 * <b>模型最低点必须停在 y=24</b>。{@link BeastLordModel} 的最低点是四只爪子，
 * 落在 {@code y=24}，所以修正量为 <b>0</b>。</p>
 *
 * <p>⚠ 修正必须放在 {@code setupRotations} 里（放 {@code scale} 里会与动画叠加）。</p>
 */
public class BeastLordRenderer
        extends MobRenderer<CultivationMonsterEntity, BeastLordModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "beast_lord"), "main");

    /** 离地修正量（格）。= (模型最低点像素 y − 24) / 16。 */
    private static final double GROUND_LIFT =
            (BeastLordModel.LOWEST_PIXEL_Y - 24) / 16.0D;

    /** 四足兽身长，所以影子比人形大一些。 */
    private static final float SHADOW_RADIUS = 1.4F;

    private final ResourceLocation texture;

    public BeastLordRenderer(EntityRendererProvider.Context ctx, String texName) {
        super(ctx, new BeastLordModel(ctx.bakeLayer(LAYER)), SHADOW_RADIUS);
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
        // 四足巨兽本身够大，横向多放大一点做出"体量感"
        poseStack.scale(rank.scaleXZ() * 1.20F, rank.scaleY() * 1.05F,
                rank.scaleXZ() * 1.20F);
    }
}
