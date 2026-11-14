package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.BloodLeechEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 血蛭的渲染器。
 *
 * <p>模型在 {@link BloodLeechModel}（由
 * {@code 工程/角色设计/round2/new-mobs/blood_leech/spec.py} 生成；模型/贴图共用一张 UV 表，
 * 审计见该目录 {@code 审计.md}）。</p>
 *
 * <p>剪影三识别点：**5 节鼓包 / 口器环 6 齿 / 尾端血囊一团红光**。
 * 它是普通品阶，但口器环与血囊靠发光层读出来，所以实体重写了
 * {@code forceGlow() = true}，这里正常挂 {@link RankGlowLayer} 即可真正发光。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BloodLeechRenderer extends MobRenderer<BloodLeechEntity, BloodLeechModel> {

    /** 离地修正量（格）：腹面已贴 y=24，保持 0。 */
    private static final double GROUND_LIFT = 0.00D;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/blood_leech.png");

    public BloodLeechRenderer(EntityRendererProvider.Context context) {
        super(context, new BloodLeechModel(context.bakeLayer(BloodLeechModel.LAYER)), 0.35F);
        // 普通品阶 + forceGlow() → 口器环/血囊/眼 会亮（否则 RankGlowLayer 对普通怪不绘制）
        this.addLayer(new RankGlowLayer<>(this));
    }

    /** 地面归一化：贴地怪（腹面 y=24），放在 setupRotations 里避免与 scale 叠加。 */
    @Override
    protected void setupRotations(BloodLeechEntity entity,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodLeechEntity entity) {
        return TEXTURE;
    }

    /** 普通怪：被注视／命名时才显示名牌（潜伏破土前不暴露身份）。 */
    @Override
    protected boolean shouldShowName(BloodLeechEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：倍率表在 {@link com.hongmeng.xiuxian.entity.MobRank}，本类不写死数字。 */
    @Override
    protected void scale(BloodLeechEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
