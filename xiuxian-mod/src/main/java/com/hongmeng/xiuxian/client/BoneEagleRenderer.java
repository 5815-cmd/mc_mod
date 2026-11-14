package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.BoneEagleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 骨鹰的渲染器。
 *
 * <p>模型在 {@link BoneEagleModel}（由
 * {@code 工程/角色设计/round2/new-mobs/bone_eagle/spec.py} 生成；模型/贴图共用一张 UV 表，
 * 审计见该目录 {@code 审计.md}）。</p>
 *
 * <p>剪影三识别点：**骷髅头 + 长喙 / 骨翼 4 指 + 破翼膜 / 尾 3 根骨刺**。
 * 精英品阶 → {@link RankGlowLayer} 会画发光眼窝与翼骨端（{@code bone_eagle_glow.png}）。
 * 飞行姿态（扇翅/俯冲/收翼）全部在模型的 {@code setupAnim} 里按
 * {@code onGround()} 与垂直速度实时算，渲染器不需要额外状态。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BoneEagleRenderer extends MobRenderer<BoneEagleEntity, BoneEagleModel> {

    /** 离地修正量（格）：双爪落地已在 y=24，保持 0（飞行高度由实体 y 决定）。 */
    private static final double GROUND_LIFT = 0.00D;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/bone_eagle.png");

    public BoneEagleRenderer(EntityRendererProvider.Context context) {
        super(context, new BoneEagleModel(context.bakeLayer(BoneEagleModel.LAYER)), 0.6F);
        // 精英：眼窝 + 翼骨端发光
        this.addLayer(new RankGlowLayer<>(this));
    }

    /** 地面归一化（落地时双爪贴合地面；飞行时由实体 y 决定高度）。 */
    @Override
    protected void setupRotations(BoneEagleEntity entity,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    public ResourceLocation getTextureLocation(BoneEagleEntity entity) {
        return TEXTURE;
    }

    /** 精英怪：被注视／命名时才显示名牌（空中盘旋时不给它当靶子的提示）。 */
    @Override
    protected boolean shouldShowName(BoneEagleEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：倍率表在 {@link com.hongmeng.xiuxian.entity.MobRank}，本类不写死数字。 */
    @Override
    protected void scale(BoneEagleEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
