package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.TerracottaSoldierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 兵俑的渲染器。
 *
 * <p>模型在 {@link TerracottaSoldierModel}（由
 * {@code 工程/角色设计/round2/new-mobs/terracotta_soldier/spec.py} 生成，
 * 模型与贴图共用同一张 UV 表；审计见该目录 {@code 审计.md}）。</p>
 *
 * <p>剪影：**方 + 戈**。方肩大甲片、短粗四肢、方正头 + 面甲，右手一杆长柄青铜戈
 * （竖柄 + 与头同高的前伸横刃）。规范里兵俑无发光层，所以本渲染器不加
 * {@code RankGlowLayer}（没有 _glow.png，加了反而会采样到缺失贴图）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class TerracottaSoldierRenderer
        extends MobRenderer<TerracottaSoldierEntity, TerracottaSoldierModel> {

    /** 离地修正量（格）：模型脚底已经压在 y=24，这里保持 0。 */
    private static final double GROUND_LIFT = 0.00D;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/terracotta_soldier.png");

    public TerracottaSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new TerracottaSoldierModel(context.bakeLayer(TerracottaSoldierModel.LAYER)), 0.5F);
    }

    /** 地面归一化：把模型顶到地面（放在 setupRotations 里，避免与 scale 叠加）。 */
    @Override
    protected void setupRotations(TerracottaSoldierEntity entity,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    public ResourceLocation getTextureLocation(TerracottaSoldierEntity entity) {
        return TEXTURE;
    }

    /** 精英怪：名牌只在被注视／命名时显示。 */
    @Override
    protected boolean shouldShowName(TerracottaSoldierEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：倍率表在 {@link com.hongmeng.xiuxian.entity.MobRank}，本类不写死数字。 */
    @Override
    protected void scale(TerracottaSoldierEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
