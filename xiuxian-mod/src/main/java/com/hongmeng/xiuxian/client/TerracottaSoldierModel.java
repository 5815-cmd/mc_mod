package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.TerracottaSoldierEntity;
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
 * 兵俑 · Terracotta Soldier / TerracottaSoldierModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>方肩大甲片 + 短粗四肢 + 面甲裂纹 + 右手青铜戈；剪影=方+戈。</p>
 */
public class TerracottaSoldierModel extends EntityModel<TerracottaSoldierEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "terracotta_soldier"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart head;
    private final ModelPart shoulderL;
    private final ModelPart shoulderR;
    private final ModelPart thighL;
    private final ModelPart thighR;
    private final ModelPart foreL;
    private final ModelPart foreR;
    private final ModelPart shinL;
    private final ModelPart shinR;
    private final ModelPart footL;
    private final ModelPart footR;
    private final ModelPart handL;
    private final ModelPart handR;
    private final ModelPart ge;

    public TerracottaSoldierModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.armL = root.getChild("body").getChild("arm_l");
        this.armR = root.getChild("body").getChild("arm_r");
        this.head = root.getChild("body").getChild("head");
        this.shoulderL = root.getChild("body").getChild("shoulder_l");
        this.shoulderR = root.getChild("body").getChild("shoulder_r");
        this.thighL = root.getChild("body").getChild("thigh_l");
        this.thighR = root.getChild("body").getChild("thigh_r");
        this.foreL = root.getChild("body").getChild("arm_l").getChild("fore_l");
        this.foreR = root.getChild("body").getChild("arm_r").getChild("fore_r");
        this.shinL = root.getChild("body").getChild("thigh_l").getChild("shin_l");
        this.shinR = root.getChild("body").getChild("thigh_r").getChild("shin_r");
        this.footL = root.getChild("body").getChild("thigh_l").getChild("shin_l").getChild("foot_l");
        this.footR = root.getChild("body").getChild("thigh_r").getChild("shin_r").getChild("foot_r");
        this.handL = root.getChild("body").getChild("arm_l").getChild("fore_l").getChild("hand_l");
        this.handR = root.getChild("body").getChild("arm_r").getChild("fore_r").getChild("hand_r");
        this.ge = root.getChild("body").getChild("arm_r").getChild("fore_r").getChild("hand_r").getChild("ge");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(5, 0).addBox(-4.0F, 1.0F, -3.0F, 8.0F, 12.0F, 6.0F, NONE)
        .texOffs(103, 32).addBox(-4.0F, 2.0F, -3.2F, 8.0F, 6.0F, 1.0F, NONE)
        .texOffs(5, 19).addBox(-3.5F, 11.0F, -2.5F, 7.0F, 4.0F, 5.0F, NONE)
        .texOffs(72, 32).addBox(-4.5F, 8.0F, -3.2F, 9.0F, 1.0F, 6.0F, NONE)
        .texOffs(30, 36).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition armL = body.addOrReplaceChild("arm_l",
CubeListBuilder.create()
        .texOffs(47, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F, NONE)
                , PartPose.offset(4.5F, 2.0F, 0.0F));
        PartDefinition armR = body.addOrReplaceChild("arm_r",
CubeListBuilder.create()
        .texOffs(34, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F, NONE)
                , PartPose.offset(-4.5F, 2.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(34, 0).addBox(-4.0F, -8.0F, -3.5F, 8.0F, 8.0F, 7.0F, NONE)
        .texOffs(65, 12).addBox(-4.5F, -8.0F, -4.0F, 9.0F, 2.0F, 8.0F, NONE)
        .texOffs(72, 40).addBox(-4.0F, -6.5F, -4.0F, 8.0F, 3.0F, 1.0F, NONE)
        .texOffs(0, 38).addBox(-3.5F, -3.5F, -4.0F, 7.0F, 2.0F, 1.0F, NONE)
        .texOffs(118, 0).addBox(-1.0F, -6.0F, -4.3F, 1.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition shoulderL = body.addOrReplaceChild("shoulder_l",
CubeListBuilder.create()
        .texOffs(100, 11).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 4.0F, 6.0F, NONE)
                , PartPose.offset(4.5F, 1.5F, 0.0F));
        PartDefinition shoulderR = body.addOrReplaceChild("shoulder_r",
CubeListBuilder.create()
        .texOffs(99, 0).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 4.0F, 6.0F, NONE)
                , PartPose.offset(-4.5F, 1.5F, 0.0F));
        PartDefinition thighL = body.addOrReplaceChild("thigh_l",
CubeListBuilder.create()
        .texOffs(82, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, NONE)
                , PartPose.offset(2.5F, 12.0F, 0.0F));
        PartDefinition thighR = body.addOrReplaceChild("thigh_r",
CubeListBuilder.create()
        .texOffs(65, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, NONE)
                , PartPose.offset(-2.5F, 12.0F, 0.0F));
        PartDefinition foreL = armL.addOrReplaceChild("fore_l",
CubeListBuilder.create()
        .texOffs(113, 22).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition foreR = armR.addOrReplaceChild("fore_r",
CubeListBuilder.create()
        .texOffs(100, 22).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition shinL = thighL.addOrReplaceChild("shin_l",
CubeListBuilder.create()
        .texOffs(55, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition shinR = thighR.addOrReplaceChild("shin_r",
CubeListBuilder.create()
        .texOffs(0, 29).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition footL = shinL.addOrReplaceChild("foot_l",
CubeListBuilder.create()
        .texOffs(30, 27).addBox(-2.5F, 0.0F, -3.5F, 5.0F, 1.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition footR = shinR.addOrReplaceChild("foot_r",
CubeListBuilder.create()
        .texOffs(60, 23).addBox(-2.5F, 0.0F, -3.5F, 5.0F, 1.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition handL = foreL.addOrReplaceChild("hand_l",
CubeListBuilder.create()
        .texOffs(17, 29).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition handR = foreR.addOrReplaceChild("hand_r",
CubeListBuilder.create()
        .texOffs(85, 23).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition ge = handR.addOrReplaceChild("ge",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-0.5F, -18.0F, -0.5F, 1.0F, 20.0F, 1.0F, NONE)
        .texOffs(17, 36).addBox(-1.5F, -20.0F, -5.0F, 2.0F, 2.0F, 4.0F, NONE)
        .texOffs(123, 0).addBox(-1.0F, -22.0F, -0.5F, 1.0F, 2.0F, 1.0F, NONE)
        .texOffs(123, 4).addBox(-0.5F, 2.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(-2.0F, 1.5F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
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
    public void setupAnim(TerracottaSoldierEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // 头：沉一点，跟随视线
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.8F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.8F;
        this.head.zRot = 0.0F;
        // 持戈的右臂：慢速小摆（戈不轻易动）
        this.armR.xRot = -0.12F + Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.armR.zRot = -0.10F;
        this.foreR.xRot = 0.22F + Mth.sin(ageInTicks * 0.05F + 1.0F) * 0.04F;
        this.handR.xRot = 0.0F;
        this.ge.zRot = 0.10F + Mth.sin(ageInTicks * 0.05F + 0.4F) * 0.035F;
        // 左臂：走路摆动
        this.armL.xRot = Mth.cos(limbSwing * 0.6662F) * 0.55F * limbSwingAmount;
        this.armL.zRot = 0.10F;
        this.foreL.xRot = 0.18F + Mth.cos(limbSwing * 0.6662F + 1.2F) * 0.35F * limbSwingAmount;
        // 腿：大步沉步；脚步不穿插
        this.thighR.xRot = Mth.cos(limbSwing * 0.6662F) * 0.62F * limbSwingAmount;
        this.shinR.xRot = Math.max(0.0F, Mth.cos(limbSwing * 0.6662F + 1.3F)) * 0.45F * limbSwingAmount;
        this.footR.xRot = -this.shinR.xRot * 0.55F;
        this.thighL.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.62F * limbSwingAmount;
        this.shinL.xRot = Math.max(0.0F, Mth.cos(limbSwing * 0.6662F + (float) Math.PI + 1.3F)) * 0.45F * limbSwingAmount;
        this.footL.xRot = -this.shinL.xRot * 0.55F;
        // 肩甲随躯干微沉（呼吸）
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.shoulderR.xRot = Mth.sin(ageInTicks * 0.05F) * 0.015F;
        this.shoulderL.xRot = Mth.sin(ageInTicks * 0.05F) * 0.015F;

    }
}
