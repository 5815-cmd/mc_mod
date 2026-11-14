package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.beast.ContractWolfEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 契约灵狼的渲染器。
 *
 * <p>它<b>复用</b> {@link SpiritWolfRenderer.WolfBody} 那套自建四足模型与
 * 同一张 {@code spirit_wolf.png}——因为契约灵狼就是"被你收服的那只灵狼"，
 * 长得不一样反而说不通。区别只在于：名牌上多出成长阶段与当前指令
 * （由 {@code SpiritBeastEntity.getName()} 提供），以及契约后的青色灵光
 * （在实体那边撒粒子）。</p>
 *
 * <p>这样处理是刻意的：新增一只灵兽不该等于新增一张贴图 + 一套骨架，
 * 只有当它的<b>剪影需要不同</b>时才值得新建模型（雷鹰、蛟龙都是这种）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ContractWolfRenderer
        extends MobRenderer<ContractWolfEntity, SpiritWolfRenderer.WolfBody<ContractWolfEntity>> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(ContractWolfEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        groundAlign(poseStack);
    }

    /**
     * 把模型**顶到地面上**（基准：模型最低点应落在模型空间 y=24，原版渲染管线会把 24 像素对齐到地面）。
     *
     * <p>原版在画模型之前会 {@code translate(0, -1.501, 0)}（1.501 格 = 24.016 像素），
     * 所以**模型空间的最低点必须停在 y=24**才正好踩在地上。
     * 本模组的骨架已经照这个基准建好了，正常情况下这个值就是 0；
     * 只有整体最低点不在 24 的模型（蛟龙 / 虚空蛟这类没有腿的蛇身）才非 0。</p>
     */
    private static void groundAlign(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    /** 离地修正量（格）：正数=上移，负数=下移。= (模型最低点像素 y - 24) / 16。 */
    private static final double GROUND_LIFT = 0.00D;

    /**
     * 灵兽成长体型：幼年 ×0.45 / 成年 ×1.0 / 化形 ×1.6（{@code SpiritBeastEntity#getScale()}）。
     *
     * <p>灵兽<b>没有品阶</b>（不是 {@code CultivationMonsterEntity}），所以这里不再乘
     * {@code MobRank.scale*}，只乘成长倍率一次 —— 否则会叠乘两次。
     * {@code scale()} 在原版渲染管线里跑在 {@code translate(0, -1.501, 0)} <b>之前</b>，
     * 缩放天然以脚底为锚，{@code GROUND_LIFT} 的脚底基准不会被放大位移。</p>
     */
    @Override
    protected void scale(ContractWolfEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        super.scale(entity, poseStack, partialTick);
        float s = entity.getScale();
        poseStack.scale(s, s, s);
    }


    /** 复用灵狼的模型层——同一个 {@code ModelLayerLocation} 不会重复加载。 */
    public static final ModelLayerLocation LAYER = SpiritWolfRenderer.LAYER;

    /**
     * 契约灵狼自己的贴图（试稿轮补齐）：在灵狼 64×32 的同一套 UV 上做**青蓝契约纹**变体。
     *
     * <p>骨架/UV 与灵狼共用（同一 {@link SpiritWolfRenderer.LAYER}），所以换图即可换外观；
     * 发光层 `contract_wolf_glow.png` 目前没有挂载（{@code RankGlowLayer} 的泛型要求
     * {@code CultivationMonsterEntity}，而灵兽走 {@code SpiritBeastEntity} 那条继承链），
     * 契约纹只作为贴图上的亮青蓝呈现 —— 要发光需另写一个灵兽版 glow layer。</p>
     */
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/contract_wolf.png");

    public ContractWolfRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiritWolfRenderer.WolfBody<ContractWolfEntity>(context.bakeLayer(LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(ContractWolfEntity entity) {
        return TEXTURE;
    }

    /** 契约过的灵兽一直显示名牌。 */
    @Override
    protected boolean shouldShowName(ContractWolfEntity entity) {
        return super.shouldShowName(entity) || entity.isContracted();
    }
}
