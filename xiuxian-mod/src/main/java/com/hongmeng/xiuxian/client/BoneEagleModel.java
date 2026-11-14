package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.BoneEagleEntity;
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
 * 骨鹰 · Bone Eagle / BoneEagleModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>骷髅头+长喙 / 骨翼4指+破翼膜 / 尾3根骨刺；白骨秃鹫，会高空盘旋后俯冲。</p>
 */
public class BoneEagleModel extends EntityModel<BoneEagleEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "bone_eagle"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart pelvis;
    private final ModelPart membraneL;
    private final ModelPart membraneR;
    private final ModelPart neck;
    private final ModelPart ribcage;
    private final ModelPart skull;
    private final ModelPart spine;
    private final ModelPart tail0;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart talonL;
    private final ModelPart talonR;
    private final ModelPart wingArmL;
    private final ModelPart wingArmR;
    private final ModelPart beakLower;
    private final ModelPart beakUpper;
    private final ModelPart crest;
    private final ModelPart eyeSocketL;
    private final ModelPart eyeSocketR;
    private final ModelPart wingTipL;
    private final ModelPart wingTipR;

    public BoneEagleModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.pelvis = root.getChild("pelvis");
        this.membraneL = root.getChild("body").getChild("membrane_l");
        this.membraneR = root.getChild("body").getChild("membrane_r");
        this.neck = root.getChild("body").getChild("neck");
        this.ribcage = root.getChild("body").getChild("ribcage");
        this.skull = root.getChild("body").getChild("skull");
        this.spine = root.getChild("body").getChild("spine");
        this.tail0 = root.getChild("body").getChild("tail_0");
        this.tail1 = root.getChild("body").getChild("tail_1");
        this.tail2 = root.getChild("body").getChild("tail_2");
        this.talonL = root.getChild("body").getChild("talon_l");
        this.talonR = root.getChild("body").getChild("talon_r");
        this.wingArmL = root.getChild("body").getChild("wing_arm_l");
        this.wingArmR = root.getChild("body").getChild("wing_arm_r");
        this.beakLower = root.getChild("body").getChild("skull").getChild("beak_lower");
        this.beakUpper = root.getChild("body").getChild("skull").getChild("beak_upper");
        this.crest = root.getChild("body").getChild("skull").getChild("crest");
        this.eyeSocketL = root.getChild("body").getChild("skull").getChild("eye_socket_l");
        this.eyeSocketR = root.getChild("body").getChild("skull").getChild("eye_socket_r");
        this.wingTipL = root.getChild("body").getChild("wing_arm_l").getChild("wing_tip_l");
        this.wingTipR = root.getChild("body").getChild("wing_arm_r").getChild("wing_tip_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition pelvis = root.addOrReplaceChild("pelvis",
CubeListBuilder.create()
        .texOffs(0, 17).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition membraneL = body.addOrReplaceChild("membrane_l",
CubeListBuilder.create()
        .texOffs(46, 23).addBox(0.0F, 0.0F, -4.0F, 8.0F, 1.0F, 8.0F, NONE)
                , PartPose.offset(4.0F, -1.0F, 0.0F));
        PartDefinition membraneR = body.addOrReplaceChild("membrane_r",
CubeListBuilder.create()
        .texOffs(79, 23).addBox(-8.0F, 0.0F, -4.0F, 8.0F, 1.0F, 8.0F, NONE)
                , PartPose.offset(-4.0F, -1.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
CubeListBuilder.create()
        .texOffs(112, 23).addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, -5.0F));
        PartDefinition ribcage = body.addOrReplaceChild("ribcage",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 6.0F, 10.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition skull = body.addOrReplaceChild("skull",
CubeListBuilder.create()
        .texOffs(54, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 3.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, -7.0F));
        PartDefinition spine = body.addOrReplaceChild("spine",
CubeListBuilder.create()
        .texOffs(37, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 4.0F));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0",
CubeListBuilder.create()
        .texOffs(15, 28).addBox(-2.0F, 0.0F, 0.0F, 1.0F, 1.0F, 5.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 9.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition tail1 = body.addOrReplaceChild("tail_1",
CubeListBuilder.create()
        .texOffs(28, 28).addBox(0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 5.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 9.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition tail2 = body.addOrReplaceChild("tail_2",
CubeListBuilder.create()
        .texOffs(112, 32).addBox(2.0F, 0.0F, 0.0F, 1.0F, 1.0F, 5.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 9.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition talonL = body.addOrReplaceChild("talon_l",
CubeListBuilder.create()
        .texOffs(83, 0).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 8.0F, 3.0F, NONE)
                , PartPose.offset(2.0F, 10.0F, 0.0F));
        PartDefinition talonR = body.addOrReplaceChild("talon_r",
CubeListBuilder.create()
        .texOffs(94, 0).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 8.0F, 3.0F, NONE)
                , PartPose.offset(-2.0F, 10.0F, 0.0F));
        PartDefinition wingArmL = body.addOrReplaceChild("wing_arm_l",
CubeListBuilder.create()
        .texOffs(54, 12).addBox(0.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, NONE)
                , PartPose.offsetAndRotation(4.0F, -2.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition wingArmR = body.addOrReplaceChild("wing_arm_r",
CubeListBuilder.create()
        .texOffs(87, 12).addBox(-8.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, NONE)
                , PartPose.offsetAndRotation(-4.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition beakLower = skull.addOrReplaceChild("beak_lower",
CubeListBuilder.create()
        .texOffs(58, 33).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition beakUpper = skull.addOrReplaceChild("beak_upper",
CubeListBuilder.create()
        .texOffs(0, 28).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition crest = skull.addOrReplaceChild("crest",
CubeListBuilder.create()
        .texOffs(41, 33).addBox(-3.0F, -5.0F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeSocketL = skull.addOrReplaceChild("eye_socket_l",
CubeListBuilder.create()
        .texOffs(120, 11).addBox(-3.0F, -1.0F, -4.2F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeSocketR = skull.addOrReplaceChild("eye_socket_r",
CubeListBuilder.create()
        .texOffs(37, 13).addBox(2.0F, -1.0F, -4.2F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition wingTipL = wingArmL.addOrReplaceChild("wing_tip_l",
CubeListBuilder.create()
        .texOffs(105, 0).addBox(0.0F, -1.0F, -4.0F, 2.0F, 2.0F, 8.0F, NONE)
                , PartPose.offsetAndRotation(8.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1F));
        PartDefinition wingTipR = wingArmR.addOrReplaceChild("wing_tip_r",
CubeListBuilder.create()
        .texOffs(25, 17).addBox(-2.0F, -1.0F, -4.0F, 2.0F, 2.0F, 8.0F, NONE)
                , PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F));
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
    public void setupAnim(BoneEagleEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // ---- 输入信号 ----
        float atk = entity.getAttackAnim(0.0F);
        float grab = Mth.sin(atk * (float) Math.PI);          // 0→1→0 的抓击曲线
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float gait = Mth.cos(limbSwing * 0.6662F);
        float breath = Mth.sin(ageInTicks * 0.085F);
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);
        boolean air = !entity.onGround();
        float dive = Mth.clamp((float) (-entity.getDeltaMovement().y * 1.6D), 0.0F, 1.0F);
        float flap = Mth.sin(ageInTicks * 0.55F) * (air ? 0.42F : 0.06F);

        // ---- 整体：盘旋上下浮 + 俯冲前倾 / 受击后仰 / 死亡趴下 ----
        this.body.y = 10.0F + Mth.sin(ageInTicks * 0.5F) * (air ? 0.6F : 0.12F) - dead * 1.5F;
        this.body.xRot = dive * 0.35F - hurt * 0.18F + dead * 0.40F;
        this.body.zRot = gait * 0.03F * walk;
        this.ribcage.xScale = 1.0F + breath * 0.012F;
        this.ribcage.zScale = 1.0F + breath * 0.012F;

        // ---- 双翼：落地收翼（下垂 0.30rad），空中展平扇动；俯冲时后掠 ----
        // 静态上反角 ±0.35 rad 与 createBodyLayer 里的 PartPose 保持一致（剪影 = V 形骨架翼）
        float fold = air ? 0.0F : 0.30F;
        float rear = dive * 0.35F - grab * 0.50F;            // 收翼俯冲 / 抓击收翼
        float spar = fold + flap + rear;                     // >0 = 收翼下垂
        this.wingArmL.zRot = -0.35F + spar;
        this.wingArmR.zRot = 0.35F - spar;
        this.wingTipL.zRot = -0.15F + spar * 1.25F;
        this.wingTipR.zRot = 0.15F - spar * 1.25F;
        // 破翼膜：兜风抖动（不跟着骨臂硬折，破布感）
        this.membraneL.zRot = 0.04F + Mth.sin(ageInTicks * 0.30F) * 0.03F + hurt * 0.10F;
        this.membraneR.zRot = -0.04F - Mth.sin(ageInTicks * 0.30F + 0.7F) * 0.03F - hurt * 0.10F;

        // ---- 头颈：视线跟随 + 俯冲时低头锁定 ----
        this.skull.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.60F;
        this.skull.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.60F + dive * 0.18F
                + Mth.sin(ageInTicks * 0.09F) * 0.02F;
        this.neck.xRot = Mth.sin(limbSwing * 0.6662F) * 0.04F * walk - dive * 0.10F;
        this.crest.zRot = 0.02F * Mth.sin(ageInTicks * 0.07F) + hurt * 0.06F;

        // ---- 尾 3 根骨刺：静态上翘 0.35 rad + 错相轻摆（空中方向舵手感）----
        this.tail0.xRot = 0.35F + 0.03F * Mth.sin(ageInTicks * 0.11F) + dive * 0.10F;
        this.tail1.xRot = 0.35F + 0.03F * Mth.sin(ageInTicks * 0.11F + 1.0F) + dive * 0.10F;
        this.tail2.xRot = 0.35F + 0.03F * Mth.sin(ageInTicks * 0.11F + 2.0F) + dive * 0.10F;

        // ---- 双爪：落地走步交替；空中收爪；抓击前伸（xRot 负 = 向前）----
        float talonStep = gait * 0.25F * walk * (air ? 0.2F : 1.0F);
        this.talonL.xRot = -grab * 0.60F + talonStep;
        this.talonR.xRot = -grab * 0.60F - talonStep;
        this.talonL.zRot = 0.0F;
        this.talonR.zRot = 0.0F;

    }
}
