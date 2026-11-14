package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CrystalSpiderEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 晶蛛 · Crystal Spider / CrystalSpiderModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>低伏八腿的矿脉化蛛：宽背 3 簇发光灵晶 + 一排 4 只发光眼 + 浑圆腹部</p>
 */
public class CrystalSpiderModel extends EntityModel<CrystalSpiderEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "crystal_spider"), "main");

    private final ModelPart root;
    private final ModelPart spider;
    private final ModelPart abdomen;
    private final ModelPart eyes;
    private final ModelPart leg1UL;
    private final ModelPart leg1UR;
    private final ModelPart leg2UL;
    private final ModelPart leg2UR;
    private final ModelPart crystalA;
    private final ModelPart crystalB;
    private final ModelPart crystalC;
    private final ModelPart leg1DL;
    private final ModelPart leg1DR;
    private final ModelPart leg2DL;
    private final ModelPart leg2DR;
    private final ModelPart leg3UL;
    private final ModelPart leg3UR;
    private final ModelPart leg4UL;
    private final ModelPart leg4UR;
    private final ModelPart leg3DL;
    private final ModelPart leg3DR;
    private final ModelPart leg4DL;
    private final ModelPart leg4DR;

    public CrystalSpiderModel(ModelPart root) {
        this.root = root;
        this.spider = root.getChild("spider");
        this.abdomen = root.getChild("spider").getChild("abdomen");
        this.eyes = root.getChild("spider").getChild("eyes");
        this.leg1UL = root.getChild("spider").getChild("leg1_u_l");
        this.leg1UR = root.getChild("spider").getChild("leg1_u_r");
        this.leg2UL = root.getChild("spider").getChild("leg2_u_l");
        this.leg2UR = root.getChild("spider").getChild("leg2_u_r");
        this.crystalA = root.getChild("spider").getChild("abdomen").getChild("crystal_a");
        this.crystalB = root.getChild("spider").getChild("abdomen").getChild("crystal_b");
        this.crystalC = root.getChild("spider").getChild("abdomen").getChild("crystal_c");
        this.leg1DL = root.getChild("spider").getChild("leg1_u_l").getChild("leg1_d_l");
        this.leg1DR = root.getChild("spider").getChild("leg1_u_r").getChild("leg1_d_r");
        this.leg2DL = root.getChild("spider").getChild("leg2_u_l").getChild("leg2_d_l");
        this.leg2DR = root.getChild("spider").getChild("leg2_u_r").getChild("leg2_d_r");
        this.leg3UL = root.getChild("spider").getChild("abdomen").getChild("leg3_u_l");
        this.leg3UR = root.getChild("spider").getChild("abdomen").getChild("leg3_u_r");
        this.leg4UL = root.getChild("spider").getChild("abdomen").getChild("leg4_u_l");
        this.leg4UR = root.getChild("spider").getChild("abdomen").getChild("leg4_u_r");
        this.leg3DL = root.getChild("spider").getChild("abdomen").getChild("leg3_u_l").getChild("leg3_d_l");
        this.leg3DR = root.getChild("spider").getChild("abdomen").getChild("leg3_u_r").getChild("leg3_d_r");
        this.leg4DL = root.getChild("spider").getChild("abdomen").getChild("leg4_u_l").getChild("leg4_d_l");
        this.leg4DR = root.getChild("spider").getChild("abdomen").getChild("leg4_u_r").getChild("leg4_d_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition spider = root.addOrReplaceChild("spider",
CubeListBuilder.create()
        .texOffs(37, 0).addBox(-4.0F, 15.0F, -5.0F, 8.0F, 4.0F, 7.0F, NONE)
        .texOffs(101, 20).addBox(-2.0F, 16.0F, -7.0F, 4.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition abdomen = spider.addOrReplaceChild("abdomen",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-5.0F, -2.0F, -3.0F, 10.0F, 5.0F, 8.0F, NONE)
        .texOffs(86, 11).addBox(-4.0F, -1.5F, -2.5F, 8.0F, 1.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 16.0F, 5.0F));
        PartDefinition eyes = spider.addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(122, 8).addBox(-4.0F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(122, 0).addBox(-3.0F, -0.5F, -0.5F, 1.5F, 1.5F, 1.0F, NONE)
        .texOffs(122, 4).addBox(1.5F, -0.5F, -0.5F, 1.5F, 1.5F, 1.0F, NONE)
        .texOffs(63, 12).addBox(3.0F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 16.0F, -7.0F));
        PartDefinition leg1UL = spider.addOrReplaceChild("leg1_u_l",
CubeListBuilder.create()
        .texOffs(13, 20).addBox(0.0F, 0.0F, -1.0F, 5.0F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(4.0F, 17.0F, -4.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition leg1UR = spider.addOrReplaceChild("leg1_u_r",
CubeListBuilder.create()
        .texOffs(86, 20).addBox(-5.0F, 0.0F, -1.0F, 5.0F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-4.0F, 17.0F, -4.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition leg2UL = spider.addOrReplaceChild("leg2_u_l",
CubeListBuilder.create()
        .texOffs(114, 20).addBox(0.0F, 0.0F, -1.0F, 4.5F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(4.0F, 17.0F, -1.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition leg2UR = spider.addOrReplaceChild("leg2_u_r",
CubeListBuilder.create()
        .texOffs(0, 21).addBox(-4.5F, 0.0F, -1.0F, 4.5F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-4.0F, 17.0F, -1.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition crystalA = abdomen.addOrReplaceChild("crystal_a",
CubeListBuilder.create()
        .texOffs(13, 14).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 1.0F, 4.0F, NONE)
        .texOffs(37, 12).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
        .texOffs(117, 11).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 1.0F));
        PartDefinition crystalB = abdomen.addOrReplaceChild("crystal_b",
CubeListBuilder.create()
        .texOffs(30, 19).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 1.0F, 4.0F, NONE)
        .texOffs(50, 12).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
        .texOffs(77, 22).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(-3.5F, -2.0F, 3.0F));
        PartDefinition crystalC = abdomen.addOrReplaceChild("crystal_c",
CubeListBuilder.create()
        .texOffs(47, 19).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 1.0F, 4.0F, NONE)
        .texOffs(0, 14).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
        .texOffs(52, 25).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(3.5F, -2.0F, 3.0F));
        PartDefinition leg1DL = leg1UL.addOrReplaceChild("leg1_d_l",
CubeListBuilder.create()
        .texOffs(68, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.5F, 2.0F, NONE)
                , PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition leg1DR = leg1UR.addOrReplaceChild("leg1_d_r",
CubeListBuilder.create()
        .texOffs(77, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.5F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition leg2DL = leg2UL.addOrReplaceChild("leg2_d_l",
CubeListBuilder.create()
        .texOffs(86, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.3F, 2.0F, NONE)
                , PartPose.offsetAndRotation(4.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition leg2DR = leg2UR.addOrReplaceChild("leg2_d_r",
CubeListBuilder.create()
        .texOffs(95, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.3F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-4.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition leg3UL = abdomen.addOrReplaceChild("leg3_u_l",
CubeListBuilder.create()
        .texOffs(64, 22).addBox(0.0F, 0.0F, -1.0F, 4.0F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(5.0F, 1.0F, -3.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition leg3UR = abdomen.addOrReplaceChild("leg3_u_r",
CubeListBuilder.create()
        .texOffs(13, 25).addBox(-4.0F, 0.0F, -1.0F, 4.0F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-5.0F, 1.0F, -3.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition leg4UL = abdomen.addOrReplaceChild("leg4_u_l",
CubeListBuilder.create()
        .texOffs(26, 25).addBox(0.0F, 0.0F, -1.0F, 3.5F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(5.0F, 1.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition leg4UR = abdomen.addOrReplaceChild("leg4_u_r",
CubeListBuilder.create()
        .texOffs(39, 25).addBox(-3.5F, 0.0F, -1.0F, 3.5F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition leg3DL = leg3UL.addOrReplaceChild("leg3_d_l",
CubeListBuilder.create()
        .texOffs(104, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.2F, 2.0F, NONE)
                , PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition leg3DR = leg3UR.addOrReplaceChild("leg3_d_r",
CubeListBuilder.create()
        .texOffs(113, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.2F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition leg4DL = leg4UL.addOrReplaceChild("leg4_d_l",
CubeListBuilder.create()
        .texOffs(68, 11).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(3.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition leg4DR = leg4UR.addOrReplaceChild("leg4_d_r",
CubeListBuilder.create()
        .texOffs(77, 11).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-3.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    @Override
    public void setupAnim(CrystalSpiderEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float t = ageInTicks;
        float stepA = Mth.cos(limbSwing * 0.62F) * limbSwingAmount;
        float stepB = Mth.cos(limbSwing * 0.62F + 3.1416F) * limbSwingAmount;
        float idle = Mth.sin(t * 0.05F) * 0.02F;

        // ---- 低伏身体：走动时轻轻起伏，静止时几乎不动 ----
        this.spider.y = -0.35F * Mth.abs(stepA);
        this.spider.zRot = 0.02F * Mth.sin(t * 0.04F);

        // ---- 八腿交替：左右反相、前后错相（左 1/3 与右 2/4 同相）----
        this.leg1UL.zRot = -0.30F + 0.10F * stepA + idle;
        this.leg1UR.zRot = 0.30F - 0.10F * stepA - idle;
        this.leg1DL.xRot = 0.22F * stepA;
        this.leg1DR.xRot = -0.22F * stepA;

        this.leg2UL.zRot = -0.30F + 0.10F * stepB + idle;
        this.leg2UR.zRot = 0.30F - 0.10F * stepB - idle;
        this.leg2DL.xRot = 0.22F * stepB;
        this.leg2DR.xRot = -0.22F * stepB;

        this.leg3UL.zRot = -0.30F + 0.10F * stepA - idle;
        this.leg3UR.zRot = 0.30F - 0.10F * stepA + idle;
        this.leg3DL.xRot = -0.22F * stepA;
        this.leg3DR.xRot = 0.22F * stepA;

        this.leg4UL.zRot = -0.30F + 0.10F * stepB - idle;
        this.leg4UR.zRot = 0.30F - 0.10F * stepB + idle;
        this.leg4DL.xRot = -0.22F * stepB;
        this.leg4DR.xRot = 0.22F * stepB;

        // ---- 腹部呼吸（只动 xScale / zScale）----
        float breath = 1.0F + 0.03F * Mth.sin(t * 0.06F);
        this.abdomen.xScale = breath;
        this.abdomen.zScale = breath;

        // ---- 背驮灵晶：微闪（三簇错相）----
        this.crystalA.xScale = 1.0F + 0.06F * Mth.sin(t * 0.10F);
        this.crystalA.zScale = 1.0F + 0.06F * Mth.sin(t * 0.10F + 0.4F);
        this.crystalB.xScale = 1.0F + 0.06F * Mth.sin(t * 0.11F + 1.6F);
        this.crystalB.zScale = 1.0F + 0.06F * Mth.sin(t * 0.11F + 2.0F);
        this.crystalC.xScale = 1.0F + 0.06F * Mth.sin(t * 0.12F + 3.2F);
        this.crystalC.zScale = 1.0F + 0.06F * Mth.sin(t * 0.12F + 3.6F);

        // ---- 4 眼微亮（跳一下）----
        float eyePulse = 1.0F + 0.05F * Mth.sin(t * 0.13F);
        this.eyes.xScale = eyePulse;
        this.eyes.zScale = eyePulse;

        // ---- 受击：甲壳一缩 + 身体抖 ----
        if (entity.hurtTime > 0) {
            float hurt = (float) entity.hurtTime / 10.0F;
            this.spider.y = -0.35F * Mth.abs(stepA) - 0.8F * hurt;
            this.spider.zRot = 0.12F * hurt * Mth.sin(t * 0.8F);
            this.abdomen.xScale = breath - 0.10F * hurt;
            this.abdomen.zScale = breath - 0.10F * hurt;
        }

        // ---- 死亡：八腿收拢（倒地旋转由渲染器负责）----
        if (entity.isDeadOrDying()) {
            this.leg1UL.zRot = 0.55F;
            this.leg1UR.zRot = -0.55F;
            this.leg2UL.zRot = 0.55F;
            this.leg2UR.zRot = -0.55F;
            this.leg3UL.zRot = 0.55F;
            this.leg3UR.zRot = -0.55F;
            this.leg4UL.zRot = 0.55F;
            this.leg4UR.zRot = -0.55F;
        }

    }


    /** 模型最低点像素 y（八条胫节末端触地）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 灵晶尖 = 8（模型总高 16px）。 */
    public static final int HIGHEST_PIXEL_Y = 8;

}
