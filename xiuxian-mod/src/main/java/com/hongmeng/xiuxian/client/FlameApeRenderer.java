package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.FlameApeEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 烈焰猿的渲染器。
 *
 * <p>模型在 {@link FlameApeModel}（试稿② 起从内嵌类拆成独立文件，由
 * {@code 工程/角色设计/02-烈焰猿/spec.py} 生成；模型与贴图共用同一张 UV 表）。</p>
 *
 * <p>剪影要点：<b>前倾、驼背、指节接地</b>。躯干整体前倾 16°，头低垂前探、陷在
 * 双肩之间，前臂极长垂到脚踝、拳几乎撑到地面；背脊 3 根熔岩骨刺 + 肩后火焰鬃毛。
 * 跟灵狼那种贴地四足、跟石傀那种方宽直立块都完全不像。</p>
 *
 * <p>贴图 {@code 256×192}（20 个部件 / 50 个盒子，UV 全由
 * {@code tools/build_entity.py} 的同表排布并逐纹素校验，见
 * {@code 工程/角色设计/02-烈焰猿/审计.md}）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class FlameApeRenderer extends MobRenderer<FlameApeEntity, FlameApeModel> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(FlameApeEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        groundAlign(poseStack);
    }

    /**
     * 把模型**顶到地面上**（基准：模型最低点应落在模型空间 y=24，原版渲染管线会把 24 像素对齐到地面）。
     *
     * <p>数值 = (模型最低点像素 y - 24) / 16，由
     * {@code 工程/角色设计/tools/build_entity.py} 实测写出，逐个可核。</p>
     */
    private static void groundAlign(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    /** 离地修正量（格）：正数=上移，负数=下移。= (模型最低点像素 y - 24) / 16。 */
    private static final double GROUND_LIFT = 0.00D;


    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "flame_ape"), "main");

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/flame_ape.png");

    public FlameApeRenderer(EntityRendererProvider.Context context) {
        super(context, new FlameApeModel(context.bakeLayer(LAYER)), 0.9F);
        // 品阶发光层（精英及以上眼睛会亮；烈焰猿的胸腹岩浆裂/背脊骨缝/火焰鬃毛
        // 也画在 _glow.png 里，所以精英阶一眼就是「活的岩浆」）
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(FlameApeEntity entity) {
        return TEXTURE;
    }

    /** 精英怪：名牌只在被注视或命名时显示，避免野外一大堆名字。 */
    @Override
    protected boolean shouldShowName(FlameApeEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：倍率表在 {@link com.hongmeng.xiuxian.entity.MobRank}，本类不写死数字。 */
    @Override
    protected void scale(FlameApeEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }
}
