package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CloudCraneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 云鹤（队长）的渲染器。
 *
 * <p>模型在 {@link CloudCraneModel}（由
 * {@code 工程/角色设计/round4/new-mobs/cloud_crane/spec.py} 生成；模型/贴图共用一张
 * 128×128 UV 表，审计见同目录 {@code 审计.md}）。</p>
 *
 * <p>剪影三识别点：**长颈两段 + 长喙 / 双翼 + 三根尾羽 / 细长腿**。
 * 飞行姿态（盘旋浮沉/扇翅/俯冲）全部在模型的 {@code setupAnim} 里按
 * {@code onGround()} 与垂直速度实时算，渲染器不需要额外状态。
 * 队长品阶 → {@link RankGlowLayer} 画发光眼（{@code cloud_crane_glow.png}）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class CloudCraneRenderer
        extends MobRenderer<CloudCraneEntity, CloudCraneModel> {

    /** 离地修正量（格）：双爪落地已在 y=24，保持 0（飞行高度由实体 y 决定）。 */
    private static final double GROUND_LIFT = 0.00D;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/cloud_crane.png");

    public CloudCraneRenderer(EntityRendererProvider.Context context) {
        super(context, new CloudCraneModel(context.bakeLayer(CloudCraneModel.LAYER)), 0.5F);
        // 队长：发光眼
        this.addLayer(new RankGlowLayer<>(this));
    }

    /** 地面归一化（落地时双爪贴地；飞行时由实体 y 决定高度）。 */
    @Override
    protected void setupRotations(CloudCraneEntity entity,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    public ResourceLocation getTextureLocation(CloudCraneEntity entity) {
        return TEXTURE;
    }

    /** 队长：被注视／命名时才显示名牌（高空盘旋时不给它当靶子的提示）。 */
    @Override
    protected boolean shouldShowName(CloudCraneEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：倍率表在 {@link com.hongmeng.xiuxian.entity.MobRank}，本类不写死数字。 */
    @Override
    protected void scale(CloudCraneEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
