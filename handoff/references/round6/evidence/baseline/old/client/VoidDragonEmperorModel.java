package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * <b>虚空龙帝 - 独立骨架</b>（由 tools/gen_emperor_models.py 生成）。
 *
 * <p>脚底基准：模型空间 y 越大越低，最低点必须正好 24
 * （原版画模型前 translate(0,-1.501,0) = 24.016 px）。</p>
 */
public class VoidDragonEmperorModel extends EntityModel<CultivationMonsterEntity> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart spine;
    private final ModelPart legFL;
    private final ModelPart legFR;
    private final ModelPart legBL;
    private final ModelPart legBR;
    private final ModelPart neck0;
    private final ModelPart neck1;
    private final ModelPart neck2;
    private final ModelPart neck3;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart eyeL;
    private final ModelPart eyeR;
    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart tail0;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart wingL0;
    private final ModelPart wingL1;
    private final ModelPart wingL2;
    private final ModelPart wingL3;
    private final ModelPart wingR0;
    private final ModelPart wingR1;
    private final ModelPart wingR2;
    private final ModelPart wingR3;

    public VoidDragonEmperorModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.spine = root.getChild("body").getChild("spine");
        this.legFL = root.getChild("body").getChild("legFL");
        this.legFR = root.getChild("body").getChild("legFR");
        this.legBL = root.getChild("body").getChild("legBL");
        this.legBR = root.getChild("body").getChild("legBR");
        this.neck0 = root.getChild("body").getChild("neck0");
        this.neck1 = root.getChild("body").getChild("neck0").getChild("neck1");
        this.neck2 = root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2");
        this.neck3 = root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3");
        this.head = root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head");
        this.jaw = root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").getChild("jaw");
        this.eyeL = root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").getChild("eyeL");
        this.eyeR = root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").getChild("eyeR");
        this.hornL = root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").getChild("hornL");
        this.hornR = root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").getChild("hornR");
        this.tail0 = root.getChild("body").getChild("tail0");
        this.tail1 = root.getChild("body").getChild("tail0").getChild("tail1");
        this.tail2 = root.getChild("body").getChild("tail0").getChild("tail1").getChild("tail2");
        this.tail3 = root.getChild("body").getChild("tail0").getChild("tail1").getChild("tail2").getChild("tail3");
        this.wingL0 = root.getChild("body").getChild("wingL0");
        this.wingL1 = root.getChild("body").getChild("wingL0").getChild("wingL1");
        this.wingL2 = root.getChild("body").getChild("wingL0").getChild("wingL1").getChild("wingL2");
        this.wingL3 = root.getChild("body").getChild("wingL0").getChild("wingL1").getChild("wingL2").getChild("wingL3");
        this.wingR0 = root.getChild("body").getChild("wingR0");
        this.wingR1 = root.getChild("body").getChild("wingR0").getChild("wingR1");
        this.wingR2 = root.getChild("body").getChild("wingR0").getChild("wingR1").getChild("wingR2");
        this.wingR3 = root.getChild("body").getChild("wingR0").getChild("wingR1").getChild("wingR2").getChild("wingR3");
    }

    /** EntityModel 是抽象类，必须自己实现这一条。 */
    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation NONE = CubeDeformation.NONE;
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -12.0F, -10.0F, 12.0F, 12.0F, 20.0F, NONE),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        root.getChild("body").addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(65, 0).addBox(-3.0F, -3.0F, -10.0F, 6.0F, 3.0F, 20.0F, NONE),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        root.getChild("body").addOrReplaceChild("legFL", CubeListBuilder.create()
                .texOffs(118, 0).addBox(-2.4F, -9.0F, -2.4F, 4.8F, 9.0F, 4.8F, NONE),
                PartPose.offset(5.0F, 12.0F, -8.0F));
        root.getChild("body").addOrReplaceChild("legFR", CubeListBuilder.create()
                .texOffs(139, 0).addBox(-2.4F, -9.0F, -2.4F, 4.8F, 9.0F, 4.8F, NONE),
                PartPose.offset(-5.0F, 12.0F, -8.0F));
        root.getChild("body").addOrReplaceChild("legBL", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-2.4F, -9.0F, -2.4F, 4.8F, 9.0F, 4.8F, NONE),
                PartPose.offset(5.0F, 12.0F, 8.0F));
        root.getChild("body").addOrReplaceChild("legBR", CubeListBuilder.create()
                .texOffs(0, 33).addBox(-2.4F, -9.0F, -2.4F, 4.8F, 9.0F, 4.8F, NONE),
                PartPose.offset(-5.0F, 12.0F, 8.0F));
        root.getChild("body").addOrReplaceChild("neck0", CubeListBuilder.create()
                .texOffs(21, 33).addBox(-3.2F, -3.2F, -9.0F, 6.4F, 6.4F, 9.0F, NONE),
                PartPose.offset(0.0F, -12.0F, -10.0F));
        root.getChild("body").getChild("neck0").addOrReplaceChild("neck1", CubeListBuilder.create()
                .texOffs(53, 33).addBox(-2.8F, -2.8F, -9.0F, 5.6F, 5.6F, 9.0F, NONE),
                PartPose.offset(0.0F, -4.0F, -9.0F));
        root.getChild("body").getChild("neck0").getChild("neck1").addOrReplaceChild("neck2", CubeListBuilder.create()
                .texOffs(84, 33).addBox(-2.4F, -2.4F, -8.0F, 4.8F, 4.8F, 8.0F, NONE),
                PartPose.offset(0.0F, -4.0F, -8.0F));
        root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").addOrReplaceChild("neck3", CubeListBuilder.create()
                .texOffs(111, 33).addBox(-2.2F, -2.2F, -6.0F, 4.4F, 4.4F, 6.0F, NONE),
                PartPose.offset(0.0F, -3.0F, -6.0F));
        root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(133, 33).addBox(-4.5F, -4.5F, -14.0F, 9.0F, 9.0F, 14.0F, NONE),
                PartPose.offset(0.0F, -3.0F, -6.0F));
        root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 57).addBox(-3.8F, 0.0F, -13.0F, 7.6F, 2.6F, 13.0F, NONE),
                PartPose.offset(0.0F, 4.0F, -1.0F));
        root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").addOrReplaceChild("eyeL", CubeListBuilder.create()
                .texOffs(43, 57).addBox(-1.4F, -1.4F, -0.6F, 2.8F, 2.8F, 1.2F, NONE),
                PartPose.offset(2.6F, -1.5F, -12.2F));
        root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").addOrReplaceChild("eyeR", CubeListBuilder.create()
                .texOffs(52, 57).addBox(-1.4F, -1.4F, -0.6F, 2.8F, 2.8F, 1.2F, NONE),
                PartPose.offset(-2.6F, -1.5F, -12.2F));
        root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").addOrReplaceChild("hornL", CubeListBuilder.create()
                .texOffs(61, 57).addBox(-1.1F, -7.0F, -1.1F, 2.2F, 7.0F, 2.2F, NONE),
                PartPose.offset(3.0F, -4.0F, -2.0F));
        root.getChild("body").getChild("neck0").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").addOrReplaceChild("hornR", CubeListBuilder.create()
                .texOffs(71, 57).addBox(-1.1F, -7.0F, -1.1F, 2.2F, 7.0F, 2.2F, NONE),
                PartPose.offset(-3.0F, -4.0F, -2.0F));
        root.getChild("body").addOrReplaceChild("tail0", CubeListBuilder.create()
                .texOffs(81, 57).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 10.0F, NONE),
                PartPose.offset(0.0F, -4.0F, 10.0F));
        root.getChild("body").getChild("tail0").addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(114, 57).addBox(-2.4F, -2.4F, 0.0F, 4.8F, 4.8F, 10.0F, NONE),
                PartPose.offset(0.0F, 0.0F, 10.0F));
        root.getChild("body").getChild("tail0").getChild("tail1").addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(145, 57).addBox(-1.8F, -1.8F, 0.0F, 3.6F, 3.6F, 9.0F, NONE),
                PartPose.offset(0.0F, 0.0F, 10.0F));
        root.getChild("body").getChild("tail0").getChild("tail1").getChild("tail2").addOrReplaceChild("tail3", CubeListBuilder.create()
                .texOffs(0, 74).addBox(-1.2F, -1.2F, 0.0F, 2.4F, 2.4F, 8.0F, NONE),
                PartPose.offset(0.0F, 0.0F, 9.0F));
        root.getChild("body").addOrReplaceChild("wingL0", CubeListBuilder.create()
                .texOffs(22, 74).addBox(0.0F, 0.0F, -6.0F, 16.0F, 2.0F, 12.0F, NONE),
                PartPose.offset(5.0F, -9.0F, -4.0F));
        root.getChild("body").getChild("wingL0").addOrReplaceChild("wingL1", CubeListBuilder.create()
                .texOffs(79, 74).addBox(0.0F, 0.0F, -5.0F, 15.0F, 1.8F, 10.0F, NONE),
                PartPose.offset(16.0F, 0.0F, 3.0F));
        root.getChild("body").getChild("wingL0").getChild("wingL1").addOrReplaceChild("wingL2", CubeListBuilder.create()
                .texOffs(130, 74).addBox(0.0F, 0.0F, -4.0F, 13.0F, 1.6F, 8.0F, NONE),
                PartPose.offset(15.0F, 0.0F, 3.0F));
        root.getChild("body").getChild("wingL0").getChild("wingL1").getChild("wingL2").addOrReplaceChild("wingL3", CubeListBuilder.create()
                .texOffs(0, 89).addBox(0.0F, 0.0F, -3.0F, 11.0F, 1.4F, 6.0F, NONE),
                PartPose.offset(13.0F, 0.0F, 3.0F));
        root.getChild("body").addOrReplaceChild("wingR0", CubeListBuilder.create()
                .texOffs(35, 89).addBox(-16.0F, 0.0F, -6.0F, 16.0F, 2.0F, 12.0F, NONE),
                PartPose.offset(-5.0F, -9.0F, -4.0F));
        root.getChild("body").getChild("wingR0").addOrReplaceChild("wingR1", CubeListBuilder.create()
                .texOffs(92, 89).addBox(-15.0F, 0.0F, -5.0F, 15.0F, 1.8F, 10.0F, NONE),
                PartPose.offset(-16.0F, 0.0F, 3.0F));
        root.getChild("body").getChild("wingR0").getChild("wingR1").addOrReplaceChild("wingR2", CubeListBuilder.create()
                .texOffs(143, 89).addBox(-13.0F, 0.0F, -4.0F, 13.0F, 1.6F, 8.0F, NONE),
                PartPose.offset(-15.0F, 0.0F, 3.0F));
        root.getChild("body").getChild("wingR0").getChild("wingR1").getChild("wingR2").addOrReplaceChild("wingR3", CubeListBuilder.create()
                .texOffs(0, 104).addBox(-11.0F, 0.0F, -3.0F, 11.0F, 1.4F, 6.0F, NONE),
                PartPose.offset(-13.0F, 0.0F, 3.0F));
        return LayerDefinition.create(mesh, 192, 192);
    }

    @Override
    public void setupAnim(CultivationMonsterEntity entity, float limbSwing,
                         float limbSwingAmount, float ageInTicks,
                         float netHeadYaw, float headPitch) {
        float t = ageInTicks;
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.55F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.35F;
        float bob = Mth.sin(t * 0.05F) * 0.04F
                + Mth.cos(limbSwing * 0.45F) * walk * 0.07F;
        this.neck0.xRot = 0.12F + bob;
        this.neck1.xRot = 0.10F + bob * 1.2F;
        this.neck2.xRot = 0.08F + bob * 1.4F;
        this.neck3.xRot = 0.06F + bob * 1.6F;
        this.jaw.xRot = 0.05F + 0.06F * Mth.sin(t * 0.10F);
        float sway = Mth.sin(t * 0.08F) * 0.10F;
        this.tail0.yRot = sway;
        this.tail1.yRot = sway * 1.3F;
        this.tail2.yRot = sway * 1.7F;
        this.tail3.yRot = sway * 2.1F;
        float flap = Mth.sin(t * 0.10F) * 0.26F;
        this.wingL0.zRot = -0.08F - flap;
        this.wingR0.zRot = 0.08F + flap;
        this.wingL1.zRot = -0.10F - flap * 1.4F;
        this.wingR1.zRot = 0.10F + flap * 1.4F;
        this.wingL2.zRot = -0.12F - flap * 1.8F;
        this.wingR2.zRot = 0.12F + flap * 1.8F;
        this.wingL3.zRot = -0.14F - flap * 2.2F;
        this.wingR3.zRot = 0.14F + flap * 2.2F;
        float s = Mth.cos(limbSwing * 0.6662F) * walk * 0.65F;
        this.legFL.xRot = s;
        this.legBR.xRot = s;
        this.legFR.xRot = -s;
        this.legBL.xRot = -s;
    }
}