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
 * <b>焚天炎帝 - 独立骨架</b>（由 tools/gen_emperor_models.py 生成）。
 *
 * <p>脚底基准：模型空间 y 越大越低，最低点必须正好 24
 * （原版画模型前 translate(0,-1.501,0) = 24.016 px）。</p>
 */
public class BlazeEmperorModel extends EntityModel<CultivationMonsterEntity> {

    private final ModelPart root;
    private final ModelPart legL;
    private final ModelPart hoofL;
    private final ModelPart legR;
    private final ModelPart hoofR;
    private final ModelPart hips;
    private final ModelPart torso;
    private final ModelPart plateA;
    private final ModelPart plateB;
    private final ModelPart core;
    private final ModelPart pauldL0;
    private final ModelPart pauldL1;
    private final ModelPart pauldL2;
    private final ModelPart pauldR0;
    private final ModelPart pauldR1;
    private final ModelPart pauldR2;
    private final ModelPart armL;
    private final ModelPart foreL;
    private final ModelPart fistL;
    private final ModelPart armR;
    private final ModelPart foreR;
    private final ModelPart fistR;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart eyeL;
    private final ModelPart eyeR;
    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart cape0;
    private final ModelPart cape1;
    private final ModelPart cape2;
    private final ModelPart swordGrip;
    private final ModelPart swordGuard;
    private final ModelPart swordBlade;

    public BlazeEmperorModel(ModelPart root) {
        this.root = root;
        this.legL = root.getChild("legL");
        this.hoofL = root.getChild("legL").getChild("hoofL");
        this.legR = root.getChild("legR");
        this.hoofR = root.getChild("legR").getChild("hoofR");
        this.hips = root.getChild("hips");
        this.torso = root.getChild("hips").getChild("torso");
        this.plateA = root.getChild("hips").getChild("torso").getChild("plateA");
        this.plateB = root.getChild("hips").getChild("torso").getChild("plateB");
        this.core = root.getChild("hips").getChild("torso").getChild("core");
        this.pauldL0 = root.getChild("hips").getChild("torso").getChild("pauldL0");
        this.pauldL1 = root.getChild("hips").getChild("torso").getChild("pauldL0").getChild("pauldL1");
        this.pauldL2 = root.getChild("hips").getChild("torso").getChild("pauldL0").getChild("pauldL1").getChild("pauldL2");
        this.pauldR0 = root.getChild("hips").getChild("torso").getChild("pauldR0");
        this.pauldR1 = root.getChild("hips").getChild("torso").getChild("pauldR0").getChild("pauldR1");
        this.pauldR2 = root.getChild("hips").getChild("torso").getChild("pauldR0").getChild("pauldR1").getChild("pauldR2");
        this.armL = root.getChild("hips").getChild("torso").getChild("armL");
        this.foreL = root.getChild("hips").getChild("torso").getChild("armL").getChild("foreL");
        this.fistL = root.getChild("hips").getChild("torso").getChild("armL").getChild("foreL").getChild("fistL");
        this.armR = root.getChild("hips").getChild("torso").getChild("armR");
        this.foreR = root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR");
        this.fistR = root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR");
        this.neck = root.getChild("hips").getChild("torso").getChild("neck");
        this.head = root.getChild("hips").getChild("torso").getChild("neck").getChild("head");
        this.jaw = root.getChild("hips").getChild("torso").getChild("neck").getChild("head").getChild("jaw");
        this.eyeL = root.getChild("hips").getChild("torso").getChild("neck").getChild("head").getChild("eyeL");
        this.eyeR = root.getChild("hips").getChild("torso").getChild("neck").getChild("head").getChild("eyeR");
        this.hornL = root.getChild("hips").getChild("torso").getChild("neck").getChild("head").getChild("hornL");
        this.hornR = root.getChild("hips").getChild("torso").getChild("neck").getChild("head").getChild("hornR");
        this.cape0 = root.getChild("hips").getChild("torso").getChild("cape0");
        this.cape1 = root.getChild("hips").getChild("torso").getChild("cape0").getChild("cape1");
        this.cape2 = root.getChild("hips").getChild("torso").getChild("cape0").getChild("cape1").getChild("cape2");
        this.swordGrip = root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR").getChild("swordGrip");
        this.swordGuard = root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR").getChild("swordGrip").getChild("swordGuard");
        this.swordBlade = root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR").getChild("swordGrip").getChild("swordGuard").getChild("swordBlade");
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
        root.addOrReplaceChild("legL", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -16.0F, -3.0F, 6.0F, 16.0F, 6.0F, NONE),
                PartPose.offset(5.0F, 24.0F, 0.0F));
        root.getChild("legL").addOrReplaceChild("hoofL", CubeListBuilder.create()
                .texOffs(25, 0).addBox(-3.5F, -2.0F, -4.0F, 7.0F, 2.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("legR", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-3.0F, -16.0F, -3.0F, 6.0F, 16.0F, 6.0F, NONE),
                PartPose.offset(-5.0F, 24.0F, 0.0F));
        root.getChild("legR").addOrReplaceChild("hoofR", CubeListBuilder.create()
                .texOffs(81, 0).addBox(-3.5F, -2.0F, -4.0F, 7.0F, 2.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("hips", CubeListBuilder.create()
                .texOffs(112, 0).addBox(-9.0F, -4.0F, -6.0F, 18.0F, 4.0F, 12.0F, NONE),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        root.getChild("hips").addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 23).addBox(-10.0F, -18.0F, -7.0F, 20.0F, 18.0F, 14.0F, NONE),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("plateA", CubeListBuilder.create()
                .texOffs(69, 23).addBox(-8.0F, -6.0F, -2.0F, 16.0F, 6.0F, 2.0F, NONE),
                PartPose.offset(0.0F, -14.0F, -7.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("plateB", CubeListBuilder.create()
                .texOffs(106, 23).addBox(-6.0F, -5.0F, -2.0F, 12.0F, 5.0F, 2.0F, NONE),
                PartPose.offset(0.0F, -7.0F, -7.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(135, 23).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 6.0F, 3.0F, NONE),
                PartPose.offset(0.0F, -9.0F, -8.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("pauldL0", CubeListBuilder.create()
                .texOffs(154, 23).addBox(0.0F, -3.0F, -6.0F, 4.0F, 6.0F, 12.0F, NONE),
                PartPose.offset(10.0F, -17.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("pauldL0").addOrReplaceChild("pauldL1", CubeListBuilder.create()
                .texOffs(0, 56).addBox(0.0F, -4.0F, -5.0F, 5.0F, 8.0F, 10.0F, NONE),
                PartPose.offset(4.0F, -3.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("pauldL0").getChild("pauldL1").addOrReplaceChild("pauldL2", CubeListBuilder.create()
                .texOffs(31, 56).addBox(0.0F, -5.0F, -4.0F, 6.0F, 10.0F, 8.0F, NONE),
                PartPose.offset(5.0F, -5.0F, 0.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("pauldR0", CubeListBuilder.create()
                .texOffs(60, 56).addBox(-4.0F, -3.0F, -6.0F, 4.0F, 6.0F, 12.0F, NONE),
                PartPose.offset(-10.0F, -17.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("pauldR0").addOrReplaceChild("pauldR1", CubeListBuilder.create()
                .texOffs(93, 56).addBox(-5.0F, -4.0F, -5.0F, 5.0F, 8.0F, 10.0F, NONE),
                PartPose.offset(-4.0F, -3.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("pauldR0").getChild("pauldR1").addOrReplaceChild("pauldR2", CubeListBuilder.create()
                .texOffs(124, 56).addBox(-6.0F, -5.0F, -4.0F, 6.0F, 10.0F, 8.0F, NONE),
                PartPose.offset(-5.0F, -5.0F, 0.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("armL", CubeListBuilder.create()
                .texOffs(153, 56).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F, NONE),
                PartPose.offset(11.0F, -15.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("armL").addOrReplaceChild("foreL", CubeListBuilder.create()
                .texOffs(0, 75).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F, NONE),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("armL").getChild("foreL").addOrReplaceChild("fistL", CubeListBuilder.create()
                .texOffs(21, 75).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F, NONE),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("armR", CubeListBuilder.create()
                .texOffs(46, 75).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F, NONE),
                PartPose.offset(-11.0F, -15.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("armR").addOrReplaceChild("foreR", CubeListBuilder.create()
                .texOffs(67, 75).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F, NONE),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").addOrReplaceChild("fistR", CubeListBuilder.create()
                .texOffs(88, 75).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F, NONE),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(113, 75).addBox(-4.0F, -2.0F, -4.0F, 8.0F, 2.0F, 8.0F, NONE),
                PartPose.offset(0.0F, -18.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("neck").addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(146, 75).addBox(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 12.0F, NONE),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("neck").getChild("head").addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 98).addBox(-4.0F, 0.0F, -6.0F, 8.0F, 3.0F, 9.0F, NONE),
                PartPose.offset(0.0F, 0.0F, -1.0F));
        root.getChild("hips").getChild("torso").getChild("neck").getChild("head").addOrReplaceChild("eyeL", CubeListBuilder.create()
                .texOffs(35, 98).addBox(-1.2F, -1.2F, -0.6F, 2.4F, 2.4F, 1.2F, NONE),
                PartPose.offset(3.0F, -6.0F, -6.2F));
        root.getChild("hips").getChild("torso").getChild("neck").getChild("head").addOrReplaceChild("eyeR", CubeListBuilder.create()
                .texOffs(44, 98).addBox(-1.2F, -1.2F, -0.6F, 2.4F, 2.4F, 1.2F, NONE),
                PartPose.offset(-3.0F, -6.0F, -6.2F));
        root.getChild("hips").getChild("torso").getChild("neck").getChild("head").addOrReplaceChild("hornL", CubeListBuilder.create()
                .texOffs(53, 98).addBox(-1.2F, -9.0F, -1.2F, 2.4F, 9.0F, 2.4F, NONE),
                PartPose.offset(4.0F, -10.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("neck").getChild("head").addOrReplaceChild("hornR", CubeListBuilder.create()
                .texOffs(64, 98).addBox(-1.2F, -9.0F, -1.2F, 2.4F, 9.0F, 2.4F, NONE),
                PartPose.offset(-4.0F, -10.0F, 0.0F));
        root.getChild("hips").getChild("torso").addOrReplaceChild("cape0", CubeListBuilder.create()
                .texOffs(75, 98).addBox(-9.0F, 0.0F, -1.0F, 18.0F, 9.0F, 2.0F, NONE),
                PartPose.offset(0.0F, -18.0F, 7.0F));
        root.getChild("hips").getChild("torso").getChild("cape0").addOrReplaceChild("cape1", CubeListBuilder.create()
                .texOffs(116, 98).addBox(-10.0F, 0.0F, -1.0F, 20.0F, 9.0F, 2.0F, NONE),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("cape0").getChild("cape1").addOrReplaceChild("cape2", CubeListBuilder.create()
                .texOffs(0, 111).addBox(-11.0F, 0.0F, -1.0F, 22.0F, 9.0F, 2.0F, NONE),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR").addOrReplaceChild("swordGrip", CubeListBuilder.create()
                .texOffs(49, 111).addBox(-1.6F, -7.0F, -1.2F, 3.2F, 7.0F, 2.4F, NONE),
                PartPose.offset(0.0F, 2.0F, -3.0F));
        root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR").getChild("swordGrip").addOrReplaceChild("swordGuard", CubeListBuilder.create()
                .texOffs(62, 111).addBox(-6.0F, -1.6F, -1.4F, 12.0F, 1.6F, 2.8F, NONE),
                PartPose.offset(0.0F, -6.0F, 0.0F));
        root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR").getChild("swordGrip").getChild("swordGuard").addOrReplaceChild("swordBlade", CubeListBuilder.create()
                .texOffs(93, 111).addBox(-2.6F, -36.0F, -1.0F, 5.2F, 36.0F, 2.0F, NONE),
                PartPose.offset(0.0F, -1.4F, 0.0F));
        return LayerDefinition.create(mesh, 192, 192);
    }

    @Override
    public void setupAnim(CultivationMonsterEntity entity, float limbSwing,
                         float limbSwingAmount, float ageInTicks,
                         float netHeadYaw, float headPitch) {
        float t = ageInTicks;
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.6F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.4F;
        float step = Mth.cos(limbSwing * 0.5F) * walk * 0.55F;
        this.legL.xRot = step;
        this.legR.xRot = -step;
        this.armR.xRot = -0.55F + step * 0.25F;
        this.foreR.xRot = -0.35F;
        this.armL.xRot = -step * 0.45F;
        this.foreL.xRot = 0.25F;
        float cape = Mth.sin(t * 0.10F) * 0.12F;
        this.cape0.xRot = 0.16F + cape * 0.5F;
        this.cape1.xRot = 0.10F + cape * 1.0F;
        this.cape2.xRot = 0.06F + cape * 1.5F;
        float pulse = 1.0F + 0.10F * Mth.sin(t * 0.30F);
        this.core.xScale = pulse;
        this.core.yScale = pulse;
        this.pauldL2.zRot = 0.10F * Mth.sin(t * 0.12F);
        this.pauldR2.zRot = -0.10F * Mth.sin(t * 0.12F);
    }
}