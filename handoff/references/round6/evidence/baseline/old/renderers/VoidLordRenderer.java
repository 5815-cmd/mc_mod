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
 * <b>虚空道主渲染器</b> —— 独立骨架 {@link VoidLordModel}。
 *
 * <h2>离地修正怎么定（这个项目最容易做错的地方）</h2>
 * <p>原版画模型前会 {@code translate(0, -1.501, 0)}（1.501 格 ≈ 24.016 像素），
 * 所以<b>模型空间的最低点必须停在 y=24</b> 才正好踩在地上。</p>
 *
 * <p>{@link VoidLordModel} 的最低点是<b>尾锥底面</b>，落在 {@code y=24}，
 * 所以修正量为 <b>0</b>：{@code GROUND_LIFT = (24 - 24) / 16 = 0}。</p>
 *
 * <p>⚠ 修正必须放在 {@code setupRotations} 里 —— 放 {@code scale} 里会与动画叠加，
 * 这是本项目踩过的坑。数值由 {@code tools/audit_model_ground.py} 实测复核。</p>
 */
public class VoidLordRenderer extends MobRenderer<CultivationMonsterEntity, VoidLordModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "void_lord"), "main");

    /** 离地修正量（格）。= (模型最低点像素 y − 24) / 16。 */
    private static final double GROUND_LIFT =
            (VoidLordModel.LOWEST_PIXEL_Y - 24) / 16.0D;

    private final ResourceLocation texture;

    public VoidLordRenderer(EntityRendererProvider.Context ctx, String texName) {
        super(ctx, new VoidLordModel(ctx.bakeLayer(LAYER)), 1.0F);
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
        // 地面归一化：**必须在 setupRotations 里**
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    protected void scale(CultivationMonsterEntity entity, PoseStack poseStack,
                         float partialTicks) {
        MobRank rank = entity.effectiveRank();
        // 首领品阶本来就比头目大一圈，这里再放大一点做出"压迫感"
        poseStack.scale(rank.scaleXZ() * 1.15F, rank.scaleY() * 1.15F,
                rank.scaleXZ() * 1.15F);
    }
}
