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
 * <b>傀儡祖师渲染器</b> —— 独立骨架 {@link PuppetMasterModel}（多臂机关体 / 提线人偶）。
 *
 * <h2>离地修正</h2>
 * <p>原版画模型前 {@code translate(0, -1.501, 0)}（≈24.016 像素）→
 * <b>模型最低点必须停在 y=24</b>。{@link PuppetMasterModel} 的最低点是<b>立柱底面</b>，
 * 落在 {@code y=24}，所以修正量为 <b>0</b>。</p>
 *
 * <p>⚠ 修正必须放在 {@code setupRotations} 里（放 {@code scale} 里会与动画叠加）。</p>
 */
public class PuppetMasterRenderer
        extends MobRenderer<CultivationMonsterEntity, PuppetMasterModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "puppet_master"),
                    "main");

    /** 离地修正量（格）。= (模型最低点像素 y − 24) / 16。 */
    private static final double GROUND_LIFT =
            (PuppetMasterModel.LOWEST_PIXEL_Y - 24) / 16.0D;

    private final ResourceLocation texture;

    public PuppetMasterRenderer(EntityRendererProvider.Context ctx, String texName) {
        super(ctx, new PuppetMasterModel(ctx.bakeLayer(LAYER)), 1.1F);
        this.addLayer(new RankGlowLayer<>(this));
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
        // 机关体整体偏高（含提线杆），横向略收一点免得看起来太宽
        poseStack.scale(rank.scaleXZ() * 0.98F, rank.scaleY() * 1.05F,
                rank.scaleXZ() * 0.98F);
    }
}
