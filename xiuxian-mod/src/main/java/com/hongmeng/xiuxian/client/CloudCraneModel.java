package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CloudCraneEntity;
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
 * 云鹤 · Cloud Crane / CloudCraneModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>鸟形：长颈两段 + 长喙 + 双翼 + 三根尾羽 + 细长腿；飞行盘旋后俯冲啄击。</p>
 */
public class CloudCraneModel extends EntityModel<CloudCraneEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "cloud_crane"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart neck1;
    private final ModelPart tail0;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart thighL;
    private final ModelPart thighR;
    private final ModelPart wingArmL;
    private final ModelPart wingArmR;
    private final ModelPart neck2;
    private final ModelPart shinL;
    private final ModelPart shinR;
    private final ModelPart wingTipL;
    private final ModelPart wingTipR;
    private final ModelPart footL;
    private final ModelPart footR;
    private final ModelPart head;
    private final ModelPart beak;
    private final ModelPart crest;
    private final ModelPart eyeL;
    private final ModelPart eyeR;

    public CloudCraneModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.chest = root.getChild("body").getChild("chest");
        this.neck1 = root.getChild("body").getChild("neck1");
        this.tail0 = root.getChild("body").getChild("tail_0");
        this.tail1 = root.getChild("body").getChild("tail_1");
        this.tail2 = root.getChild("body").getChild("tail_2");
        this.thighL = root.getChild("body").getChild("thigh_l");
        this.thighR = root.getChild("body").getChild("thigh_r");
        this.wingArmL = root.getChild("body").getChild("wing_arm_l");
        this.wingArmR = root.getChild("body").getChild("wing_arm_r");
        this.neck2 = root.getChild("body").getChild("neck1").getChild("neck2");
        this.shinL = root.getChild("body").getChild("thigh_l").getChild("shin_l");
        this.shinR = root.getChild("body").getChild("thigh_r").getChild("shin_r");
        this.wingTipL = root.getChild("body").getChild("wing_arm_l").getChild("wing_tip_l");
        this.wingTipR = root.getChild("body").getChild("wing_arm_r").getChild("wing_tip_r");
        this.footL = root.getChild("body").getChild("thigh_l").getChild("shin_l").getChild("foot_l");
        this.footR = root.getChild("body").getChild("thigh_r").getChild("shin_r").getChild("foot_r");
        this.head = root.getChild("body").getChild("neck1").getChild("neck2").getChild("head");
        this.beak = root.getChild("body").getChild("neck1").getChild("neck2").getChild("head").getChild("beak");
        this.crest = root.getChild("body").getChild("neck1").getChild("neck2").getChild("head").getChild("crest");
        this.eyeL = root.getChild("body").getChild("neck1").getChild("neck2").getChild("head").getChild("eye_l");
        this.eyeR = root.getChild("body").getChild("neck1").getChild("neck2").getChild("head").getChild("eye_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 6.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition chest = body.addOrReplaceChild("chest",
CubeListBuilder.create()
        .texOffs(0, 14).addBox(-2.5F, -1.0F, -1.0F, 5.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition neck1 = body.addOrReplaceChild("neck1",
CubeListBuilder.create()
        .texOffs(42, 9).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 4.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -1.0F, -1.0F));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0",
CubeListBuilder.create()
        .texOffs(86, 9).addBox(-2.0F, 0.0F, 0.0F, 1.0F, 1.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 1.0F, 4.0F));
        PartDefinition tail1 = body.addOrReplaceChild("tail_1",
CubeListBuilder.create()
        .texOffs(99, 9).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 1.0F, 4.0F));
        PartDefinition tail2 = body.addOrReplaceChild("tail_2",
CubeListBuilder.create()
        .texOffs(112, 9).addBox(2.0F, 0.0F, 0.0F, 1.0F, 1.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 1.0F, 4.0F));
        PartDefinition thighL = body.addOrReplaceChild("thigh_l",
CubeListBuilder.create()
        .texOffs(55, 9).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(1.5F, 5.0F, 0.0F));
        PartDefinition thighR = body.addOrReplaceChild("thigh_r",
CubeListBuilder.create()
        .texOffs(64, 9).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(-1.5F, 5.0F, 0.0F));
        PartDefinition wingArmL = body.addOrReplaceChild("wing_arm_l",
CubeListBuilder.create()
        .texOffs(27, 0).addBox(0.0F, -1.0F, -3.0F, 6.0F, 2.0F, 6.0F, NONE)
                , PartPose.offsetAndRotation(3.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1F));
        PartDefinition wingArmR = body.addOrReplaceChild("wing_arm_r",
CubeListBuilder.create()
        .texOffs(52, 0).addBox(-6.0F, -1.0F, -3.0F, 6.0F, 2.0F, 6.0F, NONE)
                , PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F));
        PartDefinition neck2 = neck1.addOrReplaceChild("neck2",
CubeListBuilder.create()
        .texOffs(73, 9).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition shinL = thighL.addOrReplaceChild("shin_l",
CubeListBuilder.create()
        .texOffs(15, 14).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition shinR = thighR.addOrReplaceChild("shin_r",
CubeListBuilder.create()
        .texOffs(103, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition wingTipL = wingArmL.addOrReplaceChild("wing_tip_l",
CubeListBuilder.create()
        .texOffs(94, 0).addBox(0.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, NONE)
                , PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1F));
        PartDefinition wingTipR = wingArmR.addOrReplaceChild("wing_tip_r",
CubeListBuilder.create()
        .texOffs(111, 0).addBox(-2.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, NONE)
                , PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F));
        PartDefinition footL = shinL.addOrReplaceChild("foot_l",
CubeListBuilder.create()
        .texOffs(73, 16).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 3.0F, 0.0F));
        PartDefinition footR = shinR.addOrReplaceChild("foot_r",
CubeListBuilder.create()
        .texOffs(88, 16).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 3.0F, 0.0F));
        PartDefinition head = neck2.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(77, 0).addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, -3.0F, 0.0F));
        PartDefinition beak = head.addOrReplaceChild("beak",
CubeListBuilder.create()
        .texOffs(27, 9).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition crest = head.addOrReplaceChild("crest",
CubeListBuilder.create()
        .texOffs(112, 16).addBox(-1.0F, -5.0F, -2.0F, 2.0F, 1.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeL = head.addOrReplaceChild("eye_l",
CubeListBuilder.create()
        .texOffs(123, 16).addBox(-2.5F, -1.0F, -2.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeR = head.addOrReplaceChild("eye_r",
CubeListBuilder.create()
        .texOffs(24, 17).addBox(1.5F, -1.0F, -2.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
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
    public void setupAnim(CloudCraneEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float gait = Mth.cos(limbSwing * 0.6662F);
        float breath = Mth.sin(ageInTicks * 0.075F);
        float atk = entity.getAttackAnim(0.0F);
        float peck = Mth.sin(atk * (float) Math.PI);          // 0→1→0 的啄击曲线
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);
        boolean air = !entity.onGround();
        float dive = Mth.clamp((float) (-entity.getDeltaMovement().y * 1.6D), 0.0F, 1.0F);
        float flap = Mth.sin(ageInTicks * 0.52F) * (air ? 0.45F : 0.05F);

        // 整体：盘旋浮沉 / 俯冲前倾 / 受击后仰 / 死亡下坠
        this.body.y = 10.0F - breath * 0.35F - dead * 1.5F;
        this.body.xRot = dive * 0.30F - hurt * 0.16F + dead * 0.35F;
        this.body.zRot = gait * 0.03F * walk;

        // 长颈：S 形跟随视线 + 呼吸摆动；啄击时前送
        this.neck1.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.25F;
        this.neck1.xRot = -0.10F + breath * 0.02F + dive * 0.10F - peck * 0.20F;
        this.neck2.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.20F;
        this.neck2.xRot = -0.06F + Mth.sin(ageInTicks * 0.075F + 0.5F) * 0.02F + dive * 0.08F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.45F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.50F + dive * 0.10F
                + peck * 0.30F;
        this.head.zRot = 0.0F;
        this.crest.zRot = Mth.sin(ageInTicks * 0.06F) * 0.03F + hurt * 0.06F;

        // 长喙：啄击时张开 + 前伸
        this.beak.yRot = 0.0F;
        this.beak.xRot = peck * 0.25F;
        this.beak.xScale = 1.0F + peck * 0.15F;

        // 双翼：地面收翼（下垂 0.22），空中扇动；俯冲时后掠
        float fold = air ? 0.0F : 0.22F;
        float rear = dive * 0.30F - peck * 0.35F;
        float spar = fold + flap + rear;
        this.wingArmL.zRot = -0.12F + spar;
        this.wingArmR.zRot = 0.12F - spar;
        this.wingTipL.zRot = -0.08F + spar * 1.10F;
        this.wingTipR.zRot = 0.08F - spar * 1.10F;

        // 三根尾羽：错相轻摆（空中方向舵）；死亡时摊平
        this.tail0.xRot = 0.05F + 0.02F * Mth.sin(ageInTicks * 0.09F) + dive * 0.08F + dead * 0.2F;
        this.tail1.xRot = 0.05F + 0.02F * Mth.sin(ageInTicks * 0.09F + 1.0F) + dive * 0.08F + dead * 0.2F;
        this.tail2.xRot = 0.05F + 0.02F * Mth.sin(ageInTicks * 0.09F + 2.0F) + dive * 0.08F + dead * 0.2F;

        // 细长腿：地面交替迈步；空中收腿（啄击时前伸抓）
        float step = gait * 0.35F * walk * (air ? 0.15F : 1.0F);
        this.thighL.xRot = 0.40F + step - peck * 0.25F;
        this.thighR.xRot = 0.40F - step - peck * 0.25F;
        this.shinL.xRot = -0.55F + Math.max(0.0F, -step) * 0.8F + peck * 0.30F;
        this.shinR.xRot = -0.55F + Math.max(0.0F, step) * 0.8F + peck * 0.30F;
        this.footL.xRot = 0.15F;
        this.footR.xRot = 0.15F;

    }
}
