package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.HeavenSoldierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 天兵（精英）的渲染器。
 *
 * <p>模型在 {@link HeavenSoldierModel}（由
 * {@code 工程/角色设计/round4/new-mobs/heaven_soldier/spec.py} 生成；模型/贴图共用一张
 * 64×64 UV 表，审计见同目录 {@code 审计.md}）。</p>
 *
 * <p>剪影三识别点：**方肩金甲 / 盔顶红缨 / 右手长戟（柄 + 斧刃 + 矛尖）**。
 * 发光层 {@code heaven_soldier_glow.png}：胸前金纹 + 盔顶红缨；精英品阶 →
 * {@link RankGlowLayer} 自动绘制。</p>
 */
@OnlyIn(Dist.CLIENT)
public class HeavenSoldierRenderer
        extends MobRenderer<HeavenSoldierEntity, HeavenSoldierModel> {

    /** 离地修正量（格）：脚底已在 y=24，保持 0。 */
    private static final double GROUND_LIFT = 0.00D;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/heaven_soldier.png");

    public HeavenSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new HeavenSoldierModel(context.bakeLayer(HeavenSoldierModel.LAYER)), 0.5F);
        // 精英：胸前金纹 + 盔顶红缨发光
        this.addLayer(new RankGlowLayer<>(this));
    }

    /** 地面归一化：把模型顶到地面（放在 setupRotations 里，避免与 scale 叠加）。 */
    @Override
    protected void setupRotations(HeavenSoldierEntity entity,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    public ResourceLocation getTextureLocation(HeavenSoldierEntity entity) {
        return TEXTURE;
    }

    /** 精英怪：名牌只在被注视／命名时显示。 */
    @Override
    protected boolean shouldShowName(HeavenSoldierEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：倍率表在 {@link com.hongmeng.xiuxian.entity.MobRank}，本类不写死数字。 */
    @Override
    protected void scale(HeavenSoldierEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
