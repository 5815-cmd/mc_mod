package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.FallenImmortalEntity;
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
 * 堕仙 · Fallen Immortal / FallenImmortalModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>残破仙袍 + 头顶仙环断成两截倒挂肩后 + 三条撕裂袍摆 + 右手断剑拖地（真世界队长）</p>
 */
public class FallenImmortalModel extends EntityModel<FallenImmortalEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "fallen_immortal"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart chest;
    private final ModelPart hemA;
    private final ModelPart hemB;
    private final ModelPart hemC;
    private final ModelPart neck;
    private final ModelPart shoulderL;
    private final ModelPart shoulderR;
    private final ModelPart waist;
    private final ModelPart handL;
    private final ModelPart handR;
    private final ModelPart head;
    private final ModelPart hair;
    private final ModelPart haloL;
    private final ModelPart haloR;
    private final ModelPart haloShard;
    private final ModelPart swordBlade;
    private final ModelPart swordGrip;
    private final ModelPart swordGuard;

    public FallenImmortalModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.armL = root.getChild("body").getChild("arm_l");
        this.armR = root.getChild("body").getChild("arm_r");
        this.chest = root.getChild("body").getChild("chest");
        this.hemA = root.getChild("body").getChild("hem_a");
        this.hemB = root.getChild("body").getChild("hem_b");
        this.hemC = root.getChild("body").getChild("hem_c");
        this.neck = root.getChild("body").getChild("neck");
        this.shoulderL = root.getChild("body").getChild("shoulder_l");
        this.shoulderR = root.getChild("body").getChild("shoulder_r");
        this.waist = root.getChild("body").getChild("waist");
        this.handL = root.getChild("body").getChild("arm_l").getChild("hand_l");
        this.handR = root.getChild("body").getChild("arm_r").getChild("hand_r");
        this.head = root.getChild("body").getChild("neck").getChild("head");
        this.hair = root.getChild("body").getChild("neck").getChild("head").getChild("hair");
        this.haloL = root.getChild("body").getChild("neck").getChild("head").getChild("halo_l");
        this.haloR = root.getChild("body").getChild("neck").getChild("head").getChild("halo_r");
        this.haloShard = root.getChild("body").getChild("neck").getChild("head").getChild("halo_shard");
        this.swordBlade = root.getChild("body").getChild("arm_r").getChild("hand_r").getChild("sword_blade");
        this.swordGrip = root.getChild("body").getChild("arm_r").getChild("hand_r").getChild("sword_grip");
        this.swordGuard = root.getChild("body").getChild("arm_r").getChild("hand_r").getChild("sword_guard");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-4.0F, -22.0F, -2.5F, 8.0F, 20.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition armL = body.addOrReplaceChild("arm_l",
CubeListBuilder.create()
        .texOffs(83, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, NONE)
                , PartPose.offset(4.5F, -18.0F, 0.0F));
        PartDefinition armR = body.addOrReplaceChild("arm_r",
CubeListBuilder.create()
        .texOffs(96, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, NONE)
                , PartPose.offset(-4.5F, -18.0F, 0.0F));
        PartDefinition chest = body.addOrReplaceChild("chest",
CubeListBuilder.create()
        .texOffs(52, 0).addBox(-4.5F, -6.0F, -3.0F, 9.0F, 6.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, -16.0F, 0.0F));
        PartDefinition hemA = body.addOrReplaceChild("hem_a",
CubeListBuilder.create()
        .texOffs(112, 11).addBox(-4.0F, -2.0F, -2.5F, 2.0F, 4.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition hemB = body.addOrReplaceChild("hem_b",
CubeListBuilder.create()
        .texOffs(109, 0).addBox(-1.0F, -3.0F, -2.5F, 2.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition hemC = body.addOrReplaceChild("hem_c",
CubeListBuilder.create()
        .texOffs(27, 15).addBox(2.0F, -2.0F, -2.5F, 2.0F, 4.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
CubeListBuilder.create()
        .texOffs(59, 23).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -22.0F, 0.0F));
        PartDefinition shoulderL = body.addOrReplaceChild("shoulder_l",
CubeListBuilder.create()
        .texOffs(112, 21).addBox(-1.5F, -2.0F, -2.5F, 3.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(4.5F, -20.0F, 0.0F));
        PartDefinition shoulderR = body.addOrReplaceChild("shoulder_r",
CubeListBuilder.create()
        .texOffs(42, 23).addBox(-1.5F, -2.0F, -2.5F, 3.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(-4.5F, -20.0F, 0.0F));
        PartDefinition waist = body.addOrReplaceChild("waist",
CubeListBuilder.create()
        .texOffs(52, 13).addBox(-4.5F, -1.0F, -3.0F, 9.0F, 3.0F, 6.0F, NONE)
        .texOffs(101, 23).addBox(-1.5F, -0.5F, -3.5F, 3.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition handL = armL.addOrReplaceChild("hand_l",
CubeListBuilder.create()
        .texOffs(42, 15).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition handR = armR.addOrReplaceChild("hand_r",
CubeListBuilder.create()
        .texOffs(72, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(27, 0).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 8.0F, 6.0F, NONE)
        .texOffs(0, 26).addBox(-2.0F, -5.0F, -3.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(5, 26).addBox(1.0F, -5.0F, -3.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition hair = head.addOrReplaceChild("hair",
CubeListBuilder.create()
        .texOffs(83, 12).addBox(-3.5F, -3.0F, -3.5F, 7.0F, 3.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition haloL = head.addOrReplaceChild("halo_l",
CubeListBuilder.create()
        .texOffs(86, 23).addBox(-6.0F, -1.0F, 2.0F, 5.0F, 1.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -9.0F, 0.0F));
        PartDefinition haloR = head.addOrReplaceChild("halo_r",
CubeListBuilder.create()
        .texOffs(27, 25).addBox(1.0F, -1.0F, 2.0F, 5.0F, 1.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -9.0F, 0.0F));
        PartDefinition haloShard = head.addOrReplaceChild("halo_shard",
CubeListBuilder.create()
        .texOffs(124, 7).addBox(-0.5F, -2.0F, 2.5F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -9.0F, 0.0F));
        PartDefinition swordBlade = handR.addOrReplaceChild("sword_blade",
CubeListBuilder.create()
        .texOffs(124, 0).addBox(-0.5F, 4.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition swordGrip = handR.addOrReplaceChild("sword_grip",
CubeListBuilder.create()
        .texOffs(81, 23).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition swordGuard = handR.addOrReplaceChild("sword_guard",
CubeListBuilder.create()
        .texOffs(42, 20).addBox(-1.5F, 3.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
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
    public void setupAnim(FallenImmortalEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        final float DEG = 0.017453292F;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        float t = ageInTicks;
        float walk = limbSwingAmount;
        float hurt = entity.hurtTime > 0 ? (float) entity.hurtTime / 10.0F : 0.0F;
        float swing = Mth.cos(limbSwing * 0.5F) * walk;

        // ---- 身体：idle 轻微呼吸；行走沉步左右晃 ----
        float bodyPitch = 0.02F * Mth.sin(t * 0.031F);
        if (hurt > 0.0F) {
            bodyPitch = bodyPitch - 0.14F * hurt;
        }
        this.body.xRot = bodyPitch;
        this.body.zRot = 0.03F * swing;
        this.body.xScale = 1.0F + 0.015F * Mth.sin(t * 0.06F);
        this.body.zScale = 1.0F + 0.015F * Mth.sin(t * 0.06F);

        // ---- 头 + 长发 ----
        this.head.yRot = yaw * 0.55F;
        this.head.xRot = pitch * 0.45F;
        this.hair.xRot = 0.03F * Mth.sin(t * 0.05F);

        // ---- 倒挂断环：两半反相轻摆（半边断环吊在肩后）----
        this.haloL.y = -9.0F + 0.4F * Mth.sin(t * 0.05F);
        this.haloR.y = -9.0F - 0.4F * Mth.sin(t * 0.05F);
        this.haloShard.y = -9.0F + 0.4F * Mth.sin(t * 0.05F + 1.0F);

        // ---- 三条撕裂袍摆：错相摆动；行走时后拖 ----
        float hemDrag = -0.06F * walk;
        this.hemA.xRot = hemDrag;
        this.hemB.xRot = hemDrag;
        this.hemC.xRot = hemDrag;
        this.hemA.zRot = 0.03F * Mth.sin(t * 0.05F);
        this.hemB.zRot = 0.03F * Mth.sin(t * 0.05F + 2.1F);
        this.hemC.zRot = 0.03F * Mth.sin(t * 0.05F + 4.2F);

        // ---- 双臂：左臂摆幅正常，提剑的右臂摆幅 ×0.6（护剑）----
        this.armL.xRot = 0.22F * swing;
        this.armR.xRot = -0.22F * swing * 0.6F;
        this.armL.zRot = 0.02F * Mth.sin(t * 0.04F);
        this.armR.zRot = -0.02F * Mth.sin(t * 0.04F);

        // ---- 断剑：轻晃（断刃始终比护手慢半拍）----
        this.swordBlade.zRot = 0.02F * Mth.sin(t * 0.07F);
        this.swordGrip.zRot = 0.01F * Mth.sin(t * 0.07F);

        // ---- 死亡：前扑、断剑脱手、袍摆拉长（倒地旋转由渲染器负责）----
        if (entity.isDeadOrDying()) {
            this.body.xRot = 0.20F;
            this.swordBlade.y = 6.0F;
            this.swordGrip.y = 6.0F;
            this.swordGuard.y = 6.0F;
            this.hemA.y = -4.0F;
            this.hemB.y = -4.0F;
            this.hemC.y = -4.0F;
            this.haloL.y = -7.0F;
            this.haloR.y = -7.0F;
        }

    }


    /** 模型最低点像素 y（中间那条撕裂袍摆触地）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 长发顶 = -11（模型总高 35px）。 */
    public static final int HIGHEST_PIXEL_Y = -11;

}
