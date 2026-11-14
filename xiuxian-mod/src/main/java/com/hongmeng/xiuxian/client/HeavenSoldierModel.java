package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.HeavenSoldierEntity;
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
 * 天兵 · Heaven Soldier / HeavenSoldierModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>原版人形骨架 + 金甲/缨盔/长戟；剪影=方肩金甲+红缨+长戟。</p>
 */
public class HeavenSoldierModel extends EntityModel<HeavenSoldierEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "heaven_soldier"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart head;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart shoulderL;
    private final ModelPart shoulderR;
    private final ModelPart footL;
    private final ModelPart footR;
    private final ModelPart halberd;
    private final ModelPart wristL;
    private final ModelPart wristR;

    public HeavenSoldierModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.armL = root.getChild("body").getChild("arm_l");
        this.armR = root.getChild("body").getChild("arm_r");
        this.head = root.getChild("body").getChild("head");
        this.legL = root.getChild("body").getChild("leg_l");
        this.legR = root.getChild("body").getChild("leg_r");
        this.shoulderL = root.getChild("body").getChild("shoulder_l");
        this.shoulderR = root.getChild("body").getChild("shoulder_r");
        this.footL = root.getChild("body").getChild("leg_l").getChild("foot_l");
        this.footR = root.getChild("body").getChild("leg_r").getChild("foot_r");
        this.halberd = root.getChild("body").getChild("arm_r").getChild("halberd");
        this.wristL = root.getChild("body").getChild("arm_l").getChild("wrist_l");
        this.wristR = root.getChild("body").getChild("arm_r").getChild("wrist_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(38, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, NONE)
        .texOffs(56, 31).addBox(-1.5F, 2.0F, -2.6F, 3.0F, 3.0F, 1.0F, NONE)
        .texOffs(0, 46).addBox(-4.5F, 10.0F, -2.0F, 9.0F, 2.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition armL = body.addOrReplaceChild("arm_l",
CubeListBuilder.create()
        .texOffs(52, 17).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, NONE)
                , PartPose.offset(4.5F, 2.0F, 0.0F));
        PartDefinition armR = body.addOrReplaceChild("arm_r",
CubeListBuilder.create()
        .texOffs(39, 17).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, NONE)
                , PartPose.offset(-4.5F, 2.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(5, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE)
        .texOffs(0, 33).addBox(-4.5F, -8.0F, -4.5F, 9.0F, 3.0F, 9.0F, NONE)
        .texOffs(56, 36).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition legL = body.addOrReplaceChild("leg_l",
CubeListBuilder.create()
        .texOffs(22, 17).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F, NONE)
                , PartPose.offset(2.0F, 12.0F, 0.0F));
        PartDefinition legR = body.addOrReplaceChild("leg_r",
CubeListBuilder.create()
        .texOffs(5, 17).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F, NONE)
                , PartPose.offset(-2.0F, 12.0F, 0.0F));
        PartDefinition shoulderL = body.addOrReplaceChild("shoulder_l",
CubeListBuilder.create()
        .texOffs(37, 40).addBox(-1.5F, -1.0F, -2.5F, 3.0F, 3.0F, 5.0F, NONE)
                , PartPose.offset(4.5F, 1.0F, 0.0F));
        PartDefinition shoulderR = body.addOrReplaceChild("shoulder_r",
CubeListBuilder.create()
        .texOffs(39, 31).addBox(-1.5F, -1.0F, -2.5F, 3.0F, 3.0F, 5.0F, NONE)
                , PartPose.offset(-4.5F, 1.0F, 0.0F));
        PartDefinition footL = legL.addOrReplaceChild("foot_l",
CubeListBuilder.create()
        .texOffs(46, 49).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 1.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 11.0F, 0.0F));
        PartDefinition footR = legR.addOrReplaceChild("foot_r",
CubeListBuilder.create()
        .texOffs(27, 49).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 1.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 11.0F, 0.0F));
        PartDefinition halberd = armR.addOrReplaceChild("halberd",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-0.5F, -13.0F, -0.5F, 1.0F, 23.0F, 1.0F, NONE)
        .texOffs(54, 40).addBox(-0.5F, -13.0F, -4.0F, 1.0F, 4.0F, 4.0F, NONE)
        .texOffs(0, 25).addBox(-0.5F, -16.0F, -0.5F, 1.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(-1.0F, 12.0F, -1.5F));
        PartDefinition wristL = armL.addOrReplaceChild("wrist_l",
CubeListBuilder.create()
        .texOffs(13, 53).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition wristR = armR.addOrReplaceChild("wrist_r",
CubeListBuilder.create()
        .texOffs(0, 53).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 8.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
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
    public void setupAnim(HeavenSoldierEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float gait = Mth.cos(limbSwing * 0.6662F);
        float breath = Mth.sin(ageInTicks * 0.08F);
        float atk = entity.getAttackAnim(0.0F);
        float thrust = Mth.sin(atk * (float) Math.PI);        // 0→1→0 的突刺曲线
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);

        // 头：跟随视线 + 呼吸微沉
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.9F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.8F + breath * 0.01F;
        this.head.zRot = 0.0F;

        // 军阵沉步：腿大步交替；靴子跟随（同父链，不再单独转）
        this.legR.xRot = gait * 0.60F * walk;
        this.legL.xRot = -gait * 0.60F * walk;
        this.footR.xRot = -this.legR.xRot * 0.35F;
        this.footL.xRot = -this.legL.xRot * 0.35F;

        // 持戟右臂：平时稳、突刺时前送；左臂正常摆
        this.armR.xRot = 0.05F + breath * 0.015F - thrust * 0.55F;
        this.armR.zRot = -0.08F;
        this.wristR.xRot = -thrust * 0.20F;
        this.armL.xRot = gait * 0.55F * walk;
        this.armL.zRot = 0.08F;
        this.wristL.xRot = 0.0F;

        // 长戟：待机微垂，突刺时随臂前压
        this.halberd.xRot = -0.05F + breath * 0.02F - thrust * 0.25F;
        this.halberd.zRot = 0.10F;

        // 肩甲随呼吸轻抬
        this.shoulderR.xRot = breath * 0.02F;
        this.shoulderL.xRot = breath * 0.02F;

        // 受击后仰 / 死亡下沉
        this.body.xRot = -hurt * 0.10F + dead * 0.30F;
        this.body.zRot = 0.0F;
        this.body.y = -dead * 1.5F;

    }
}
