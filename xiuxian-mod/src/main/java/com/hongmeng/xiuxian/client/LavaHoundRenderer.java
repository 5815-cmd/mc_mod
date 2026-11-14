package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.LavaHoundEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 熔岩犬的渲染器——<b>不新建模型</b>，直接复用灵狼骨架
 * {@link SpiritWolfRenderer.WolfBody} 与 {@link SpiritWolfRenderer#LAYER}。
 *
 * <p>数据流：{@code ModEntitiesExtra.LAVA_HOUND} → 本渲染器 →
 * {@code WolfBody<LavaHoundEntity>}（同一套 64×32 原版狼 UV 布局）+
 * {@code textures/entity/lava_hound.png}（黑曜皮 + 熔岩裂纹 + 橙红眼）。
 * 模型侧一行没改，所以"狼形/步态/尾巴"与灵狼完全一致；
 * 玩家区分它们靠颜色与火星粒子，不靠剪影。</p>
 *
 * <p>贴图重绘脚本：{@code 工程/角色设计/round2/new-mobs/lava_hound/make_textures.py}
 * （可重跑；严格套用 {@code spirit_wolf.png} 的 UV 模板，只换色 + 加裂纹 + 换眼睛）。
 * 精英品阶 → 挂 {@link RankGlowLayer}，发光层 {@code lava_hound_glow.png}
 * 只含"熔岩裂纹 + 橙红眼"。</p>
 */
@OnlyIn(Dist.CLIENT)
public class LavaHoundRenderer
        extends MobRenderer<LavaHoundEntity, SpiritWolfRenderer.WolfBody<LavaHoundEntity>> {

    /** 离地修正量（格）：狼骨架脚底已在 y=24，保持 0。 */
    private static final double GROUND_LIFT = 0.00D;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/lava_hound.png");

    public LavaHoundRenderer(EntityRendererProvider.Context context) {
        super(context,
                new SpiritWolfRenderer.WolfBody<LavaHoundEntity>(
                        context.bakeLayer(SpiritWolfRenderer.LAYER)),
                0.6F);
        // 精英：眼睛/裂纹会亮（RankGlowLayer 读 lava_hound_glow.png）
        this.addLayer(new RankGlowLayer<>(this));
    }

    /** 地面归一化：把模型顶到地面（放在 setupRotations 里，避免与 scale 叠加）。 */
    @Override
    protected void setupRotations(LavaHoundEntity entity,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    public ResourceLocation getTextureLocation(LavaHoundEntity entity) {
        return TEXTURE;
    }

    /** 精英怪：名牌只在被注视／命名时显示。 */
    @Override
    protected boolean shouldShowName(LavaHoundEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：倍率表在 {@link com.hongmeng.xiuxian.entity.MobRank}，本类不写死数字。 */
    @Override
    protected void scale(LavaHoundEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
