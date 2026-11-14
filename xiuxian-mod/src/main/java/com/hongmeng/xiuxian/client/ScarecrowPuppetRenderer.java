package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.ScarecrowPuppetEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 稻草傀儡的渲染器。
 *
 * <p>模型在 {@link ScarecrowPuppetModel}（由
 * {@code 工程/角色设计/round2/new-mobs/scarecrow_puppet/spec.py} 生成，
 * 模型与贴图共用同一张 UV 表；审计见该目录 {@code 审计.md}）。</p>
 *
 * <p>剪影：**十字架 + 斗笠**。一根竖桩插到地面（脚底 y=24）+ 肩高横梁 + 草把躯干
 * + 宽檐破斗笠 + 两只空袖管 + 右肩黑鸦；没有腿——它整根竹架在挪，这是它最好认的地方。</p>
 *
 * <p>发光层 {@code scarecrow_puppet_glow.png} 画了黑鸦双眼与斗笠下两盏小灯。
 * 注意：本怪是<b>普通品阶</b>，{@link RankGlowLayer} 只在精英及以上
 * （{@code MobRank.glowingEyes()}）才绘制——与灵狼同一套机制，
 * 所以发光层当前是"预留给品阶/后续特效"的资产，普通阶不会自动亮。
 * 若以后要让它常亮，把这一层换成不受品阶限制的自定义层即可（本类已单独成文件）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ScarecrowPuppetRenderer
        extends MobRenderer<ScarecrowPuppetEntity, ScarecrowPuppetModel> {

    /** 离地修正量（格）：竖桩脚底已在 y=24，保持 0。 */
    private static final double GROUND_LIFT = 0.00D;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/scarecrow_puppet.png");

    public ScarecrowPuppetRenderer(EntityRendererProvider.Context context) {
        super(context, new ScarecrowPuppetModel(context.bakeLayer(ScarecrowPuppetModel.LAYER)), 0.4F);
        // 有 _glow.png 才加；普通品阶下它默认不画（见类注释）
        this.addLayer(new RankGlowLayer<>(this));
    }

    /** 地面归一化：把模型顶到地面（放在 setupRotations 里，避免与 scale 叠加）。 */
    @Override
    protected void setupRotations(ScarecrowPuppetEntity entity,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    @Override
    public ResourceLocation getTextureLocation(ScarecrowPuppetEntity entity) {
        return TEXTURE;
    }

    /** 普通怪：被注视／命名时才显示名牌（"装死"时连名字都不显）。 */
    @Override
    protected boolean shouldShowName(ScarecrowPuppetEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：倍率表在 {@link com.hongmeng.xiuxian.entity.MobRank}，本类不写死数字。 */
    @Override
    protected void scale(ScarecrowPuppetEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
