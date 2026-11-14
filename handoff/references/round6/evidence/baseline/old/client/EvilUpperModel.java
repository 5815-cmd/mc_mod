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
 * <b>三阶段：挤满天空的上半身</b> —— 「恶」的三套阶段骨架之一（由 
 * {@code tools/gen_evil_phases.py} 从 parts 表生成：模型/贴图/动画同一张表）。
 *
 * <p><b>阶段入场压入接口</b>：renderer 用阶段字段做 {@code translate(0, ENTRANCE_DROP*(1-p), 0)} 从上方压入；
 * 本模型不改 root 位置，只把部件姿态按常态计算（绝对赋值，禁止 += / *=）。</p>
 */
public class EvilUpperModel extends EntityModel<CultivationMonsterEntity> {

    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart chest;
    private final ModelPart padL;
    private final ModelPart padR;
    private final ModelPart rib0;
    private final ModelPart rib1;
    private final ModelPart rib2;
    private final ModelPart rib3;
    private final ModelPart sternumL;
    private final ModelPart sternumR;
    private final ModelPart head;
    private final ModelPart cranium;
    private final ModelPart mainEye;
    private final ModelPart pupil;
    private final ModelPart eyeH0;
    private final ModelPart eyeH1;
    private final ModelPart eyeH2;
    private final ModelPart eyeH3;
    private final ModelPart eyeH4;
    private final ModelPart eyeH5;
    private final ModelPart eyeH6;
    private final ModelPart eyeH7;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart fangU0;
    private final ModelPart fangU1;
    private final ModelPart fangU2;
    private final ModelPart fangU3;
    private final ModelPart fangL0;
    private final ModelPart fangL1;
    private final ModelPart fangL2;
    private final ModelPart fangL3;
    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart armR;
    private final ModelPart forearmR;
    private final ModelPart handR;
    private final ModelPart armL;
    private final ModelPart forearmL;
    private final ModelPart handL;
    private final ModelPart clawR0;
    private final ModelPart clawR1;
    private final ModelPart clawR2;
    private final ModelPart clawR3;
    private final ModelPart clawR4;
    private final ModelPart clawL0;
    private final ModelPart clawL1;
    private final ModelPart clawL2;
    private final ModelPart clawL3;
    private final ModelPart clawL4;
    private final ModelPart eyeT0;
    private final ModelPart eyeT1;
    private final ModelPart eyeT2;
    private final ModelPart eyeT3;
    private final ModelPart eyeC0;
    private final ModelPart eyeC1;
    private final ModelPart eyeS0;
    private final ModelPart eyeS1;
    private final ModelPart eyeAR;
    private final ModelPart eyeAR2;
    private final ModelPart eyeAL;
    private final ModelPart eyeAL2;
    private final ModelPart tentB0;
    private final ModelPart tentB02;
    private final ModelPart tentB1;
    private final ModelPart tentB12;
    private final ModelPart tentB2;
    private final ModelPart tentB22;
    private final ModelPart spikeB0;
    private final ModelPart spikeB1;
    private final ModelPart spikeB2;
    private final ModelPart spikeB3;

    public EvilUpperModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.chest = root.getChild("torso").getChild("chest");
        this.padL = root.getChild("torso").getChild("chest").getChild("padL");
        this.padR = root.getChild("torso").getChild("chest").getChild("padR");
        this.rib0 = root.getChild("torso").getChild("chest").getChild("rib0");
        this.rib1 = root.getChild("torso").getChild("chest").getChild("rib1");
        this.rib2 = root.getChild("torso").getChild("chest").getChild("rib2");
        this.rib3 = root.getChild("torso").getChild("chest").getChild("rib3");
        this.sternumL = root.getChild("torso").getChild("chest").getChild("sternumL");
        this.sternumR = root.getChild("torso").getChild("chest").getChild("sternumR");
        this.head = root.getChild("torso").getChild("chest").getChild("head");
        this.cranium = root.getChild("torso").getChild("chest").getChild("head").getChild("cranium");
        this.mainEye = root.getChild("torso").getChild("chest").getChild("head").getChild("mainEye");
        this.pupil = root.getChild("torso").getChild("chest").getChild("head").getChild("mainEye").getChild("pupil");
        this.eyeH0 = root.getChild("torso").getChild("chest").getChild("head").getChild("eyeH0");
        this.eyeH1 = root.getChild("torso").getChild("chest").getChild("head").getChild("eyeH1");
        this.eyeH2 = root.getChild("torso").getChild("chest").getChild("head").getChild("eyeH2");
        this.eyeH3 = root.getChild("torso").getChild("chest").getChild("head").getChild("eyeH3");
        this.eyeH4 = root.getChild("torso").getChild("chest").getChild("head").getChild("cranium").getChild("eyeH4");
        this.eyeH5 = root.getChild("torso").getChild("chest").getChild("head").getChild("cranium").getChild("eyeH5");
        this.eyeH6 = root.getChild("torso").getChild("chest").getChild("head").getChild("eyeH6");
        this.eyeH7 = root.getChild("torso").getChild("chest").getChild("head").getChild("eyeH7");
        this.upperJaw = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw");
        this.lowerJaw = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw");
        this.fangU0 = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("fangU0");
        this.fangU1 = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("fangU1");
        this.fangU2 = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("fangU2");
        this.fangU3 = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("fangU3");
        this.fangL0 = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL0");
        this.fangL1 = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL1");
        this.fangL2 = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL2");
        this.fangL3 = root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL3");
        this.hornL = root.getChild("torso").getChild("chest").getChild("head").getChild("cranium").getChild("hornL");
        this.hornR = root.getChild("torso").getChild("chest").getChild("head").getChild("cranium").getChild("hornR");
        this.armR = root.getChild("torso").getChild("armR");
        this.forearmR = root.getChild("torso").getChild("armR").getChild("forearmR");
        this.handR = root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR");
        this.armL = root.getChild("torso").getChild("armL");
        this.forearmL = root.getChild("torso").getChild("armL").getChild("forearmL");
        this.handL = root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL");
        this.clawR0 = root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR0");
        this.clawR1 = root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR1");
        this.clawR2 = root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR2");
        this.clawR3 = root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR3");
        this.clawR4 = root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR4");
        this.clawL0 = root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL0");
        this.clawL1 = root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL1");
        this.clawL2 = root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL2");
        this.clawL3 = root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL3");
        this.clawL4 = root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL4");
        this.eyeT0 = root.getChild("torso").getChild("eyeT0");
        this.eyeT1 = root.getChild("torso").getChild("eyeT1");
        this.eyeT2 = root.getChild("torso").getChild("eyeT2");
        this.eyeT3 = root.getChild("torso").getChild("eyeT3");
        this.eyeC0 = root.getChild("torso").getChild("chest").getChild("eyeC0");
        this.eyeC1 = root.getChild("torso").getChild("chest").getChild("eyeC1");
        this.eyeS0 = root.getChild("torso").getChild("chest").getChild("padL").getChild("eyeS0");
        this.eyeS1 = root.getChild("torso").getChild("chest").getChild("padR").getChild("eyeS1");
        this.eyeAR = root.getChild("torso").getChild("armR").getChild("eyeAR");
        this.eyeAR2 = root.getChild("torso").getChild("armR").getChild("forearmR").getChild("eyeAR2");
        this.eyeAL = root.getChild("torso").getChild("armL").getChild("eyeAL");
        this.eyeAL2 = root.getChild("torso").getChild("armL").getChild("forearmL").getChild("eyeAL2");
        this.tentB0 = root.getChild("torso").getChild("tentB0");
        this.tentB02 = root.getChild("torso").getChild("tentB0").getChild("tentB02");
        this.tentB1 = root.getChild("torso").getChild("tentB1");
        this.tentB12 = root.getChild("torso").getChild("tentB1").getChild("tentB12");
        this.tentB2 = root.getChild("torso").getChild("tentB2");
        this.tentB22 = root.getChild("torso").getChild("tentB2").getChild("tentB22");
        this.spikeB0 = root.getChild("torso").getChild("chest").getChild("spikeB0");
        this.spikeB1 = root.getChild("torso").getChild("chest").getChild("spikeB1");
        this.spikeB2 = root.getChild("torso").getChild("spikeB2");
        this.spikeB3 = root.getChild("torso").getChild("spikeB3");
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer buf,
                               int light, int overlay,
                               float r, float g, float b, float a) {
        this.root.render(pose, buf, light, overlay, r, g, b, a);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation NONE = CubeDeformation.NONE;
        root.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(352, 470).addBox(-35.0F, -60.0F, -26.0F, 70.0F, 60.0F, 52.0F, NONE),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.getChild("torso").addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(598, 470).addBox(-42.0F, -26.0F, -30.0F, 84.0F, 26.0F, 60.0F, NONE),
                PartPose.offset(0.0F, -60.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("padL", CubeListBuilder.create()
                .texOffs(888, 470).addBox(-10.0F, -14.0F, -20.0F, 20.0F, 22.0F, 40.0F, NONE),
                PartPose.offset(34.0F, 0.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("padR", CubeListBuilder.create()
                .texOffs(0, 584).addBox(-10.0F, -14.0F, -20.0F, 20.0F, 22.0F, 40.0F, NONE),
                PartPose.offset(-34.0F, 0.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("rib0", CubeListBuilder.create()
                .texOffs(122, 584).addBox(-30.0F, 0.0F, -2.0F, 60.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(0.0F, -24.0F, -30.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("rib1", CubeListBuilder.create()
                .texOffs(252, 584).addBox(-32.0F, 0.0F, -2.0F, 64.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(0.0F, -18.0F, -30.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("rib2", CubeListBuilder.create()
                .texOffs(390, 584).addBox(-30.0F, 0.0F, -2.0F, 60.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(0.0F, -12.0F, -30.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("rib3", CubeListBuilder.create()
                .texOffs(520, 584).addBox(-26.0F, 0.0F, -2.0F, 52.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(0.0F, -6.0F, -30.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("sternumL", CubeListBuilder.create()
                .texOffs(634, 584).addBox(-4.0F, 0.0F, -2.0F, 4.0F, 24.0F, 4.0F, NONE),
                PartPose.offset(-8.0F, -24.0F, -30.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("sternumR", CubeListBuilder.create()
                .texOffs(652, 584).addBox(-4.0F, 0.0F, -2.0F, 4.0F, 24.0F, 4.0F, NONE),
                PartPose.offset(8.0F, -24.0F, -30.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(670, 584).addBox(-30.0F, -40.0F, -26.0F, 60.0F, 40.0F, 52.0F, NONE),
                PartPose.offset(0.0F, -26.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("cranium", CubeListBuilder.create()
                .texOffs(0, 678).addBox(-26.0F, -18.0F, -22.0F, 52.0F, 18.0F, 44.0F, NONE),
                PartPose.offset(0.0F, -40.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("mainEye", CubeListBuilder.create()
                .texOffs(194, 678).addBox(-14.0F, -14.0F, -3.0F, 28.0F, 28.0F, 3.0F, NONE),
                PartPose.offset(0.0F, -20.0F, -26.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("mainEye").addOrReplaceChild("pupil", CubeListBuilder.create()
                .texOffs(258, 678).addBox(-6.0F, -6.0F, -2.0F, 12.0F, 12.0F, 2.0F, NONE),
                PartPose.offset(0.0F, 0.0F, -3.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("eyeH0", CubeListBuilder.create()
                .texOffs(288, 678).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-22.0F, -34.0F, -26.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("eyeH1", CubeListBuilder.create()
                .texOffs(316, 678).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(22.0F, -34.0F, -26.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("eyeH2", CubeListBuilder.create()
                .texOffs(344, 678).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-22.0F, -8.0F, -26.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("eyeH3", CubeListBuilder.create()
                .texOffs(372, 678).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(22.0F, -8.0F, -26.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("cranium").addOrReplaceChild("eyeH4", CubeListBuilder.create()
                .texOffs(400, 678).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-14.0F, -10.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("cranium").addOrReplaceChild("eyeH5", CubeListBuilder.create()
                .texOffs(428, 678).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(14.0F, -10.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("eyeH6", CubeListBuilder.create()
                .texOffs(456, 678).addBox(0.0F, -5.0F, -5.0F, 3.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(30.0F, -26.0F, 4.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("eyeH7", CubeListBuilder.create()
                .texOffs(484, 678).addBox(-3.0F, -5.0F, -5.0F, 3.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(-30.0F, -26.0F, 4.0F));
        root.getChild("torso").getChild("chest").getChild("head").addOrReplaceChild("upperJaw", CubeListBuilder.create()
                .texOffs(512, 678).addBox(-28.0F, 0.0F, -26.0F, 56.0F, 16.0F, 46.0F, NONE),
                PartPose.offset(0.0F, 0.0F, -6.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").addOrReplaceChild("lowerJaw", CubeListBuilder.create()
                .texOffs(718, 678).addBox(-24.0F, 0.0F, -22.0F, 48.0F, 14.0F, 42.0F, NONE),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").addOrReplaceChild("fangU0", CubeListBuilder.create()
                .texOffs(900, 678).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 14.0F, 8.0F, NONE),
                PartPose.offset(-18.0F, 16.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").addOrReplaceChild("fangU1", CubeListBuilder.create()
                .texOffs(934, 678).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 17.0F, 8.0F, NONE),
                PartPose.offset(-6.0F, 16.0F, -24.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").addOrReplaceChild("fangU2", CubeListBuilder.create()
                .texOffs(968, 678).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 17.0F, 8.0F, NONE),
                PartPose.offset(6.0F, 16.0F, -24.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").addOrReplaceChild("fangU3", CubeListBuilder.create()
                .texOffs(0, 742).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 14.0F, 8.0F, NONE),
                PartPose.offset(18.0F, 16.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL0", CubeListBuilder.create()
                .texOffs(34, 742).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, NONE),
                PartPose.offset(-16.0F, 14.0F, -20.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL1", CubeListBuilder.create()
                .texOffs(68, 742).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(-5.0F, 14.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL2", CubeListBuilder.create()
                .texOffs(102, 742).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(5.0F, 14.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL3", CubeListBuilder.create()
                .texOffs(136, 742).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, NONE),
                PartPose.offset(16.0F, 14.0F, -20.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("cranium").addOrReplaceChild("hornL", CubeListBuilder.create()
                .texOffs(170, 742).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(20.0F, -18.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("head").getChild("cranium").addOrReplaceChild("hornR", CubeListBuilder.create()
                .texOffs(212, 742).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(-20.0F, -18.0F, 0.0F));
        root.getChild("torso").addOrReplaceChild("armR", CubeListBuilder.create()
                .texOffs(254, 742).addBox(-16.0F, 0.0F, -16.0F, 32.0F, 50.0F, 32.0F, NONE),
                PartPose.offset(-38.0F, -34.0F, 0.0F));
        root.getChild("torso").getChild("armR").addOrReplaceChild("forearmR", CubeListBuilder.create()
                .texOffs(384, 742).addBox(-14.0F, 0.0F, -14.0F, 28.0F, 50.0F, 28.0F, NONE),
                PartPose.offset(0.0F, 50.0F, 0.0F));
        root.getChild("torso").getChild("armR").getChild("forearmR").addOrReplaceChild("handR", CubeListBuilder.create()
                .texOffs(498, 742).addBox(-18.0F, 0.0F, -18.0F, 36.0F, 18.0F, 36.0F, NONE),
                PartPose.offset(0.0F, 50.0F, 0.0F));
        root.getChild("torso").addOrReplaceChild("armL", CubeListBuilder.create()
                .texOffs(644, 742).addBox(-16.0F, 0.0F, -16.0F, 32.0F, 50.0F, 32.0F, NONE),
                PartPose.offset(38.0F, -34.0F, 0.0F));
        root.getChild("torso").getChild("armL").addOrReplaceChild("forearmL", CubeListBuilder.create()
                .texOffs(774, 742).addBox(-14.0F, 0.0F, -14.0F, 28.0F, 50.0F, 28.0F, NONE),
                PartPose.offset(0.0F, 50.0F, 0.0F));
        root.getChild("torso").getChild("armL").getChild("forearmL").addOrReplaceChild("handL", CubeListBuilder.create()
                .texOffs(0, 826).addBox(-18.0F, 0.0F, -18.0F, 36.0F, 18.0F, 36.0F, NONE),
                PartPose.offset(0.0F, 50.0F, 0.0F));
        root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR0", CubeListBuilder.create()
                .texOffs(146, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(-12.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR1", CubeListBuilder.create()
                .texOffs(180, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(-6.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR2", CubeListBuilder.create()
                .texOffs(214, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(0.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR3", CubeListBuilder.create()
                .texOffs(248, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(6.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR4", CubeListBuilder.create()
                .texOffs(282, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(12.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL0", CubeListBuilder.create()
                .texOffs(316, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(-12.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL1", CubeListBuilder.create()
                .texOffs(350, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(-6.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL2", CubeListBuilder.create()
                .texOffs(384, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(0.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL3", CubeListBuilder.create()
                .texOffs(418, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(6.0F, 18.0F, -14.0F));
        root.getChild("torso").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL4", CubeListBuilder.create()
                .texOffs(452, 826).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 12.0F, 10.0F, NONE),
                PartPose.offset(12.0F, 18.0F, -14.0F));
        root.getChild("torso").addOrReplaceChild("eyeT0", CubeListBuilder.create()
                .texOffs(486, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-18.0F, -30.0F, -26.0F));
        root.getChild("torso").addOrReplaceChild("eyeT1", CubeListBuilder.create()
                .texOffs(518, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(18.0F, -30.0F, -26.0F));
        root.getChild("torso").addOrReplaceChild("eyeT2", CubeListBuilder.create()
                .texOffs(550, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-18.0F, -6.0F, -26.0F));
        root.getChild("torso").addOrReplaceChild("eyeT3", CubeListBuilder.create()
                .texOffs(582, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(18.0F, -6.0F, -26.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("eyeC0", CubeListBuilder.create()
                .texOffs(614, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-20.0F, -20.0F, -30.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("eyeC1", CubeListBuilder.create()
                .texOffs(646, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(20.0F, -20.0F, -30.0F));
        root.getChild("torso").getChild("chest").getChild("padL").addOrReplaceChild("eyeS0", CubeListBuilder.create()
                .texOffs(678, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(12.0F, 6.0F, -18.0F));
        root.getChild("torso").getChild("chest").getChild("padR").addOrReplaceChild("eyeS1", CubeListBuilder.create()
                .texOffs(710, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(12.0F, 6.0F, -18.0F));
        root.getChild("torso").getChild("armR").addOrReplaceChild("eyeAR", CubeListBuilder.create()
                .texOffs(742, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 16.0F, -16.0F));
        root.getChild("torso").getChild("armR").getChild("forearmR").addOrReplaceChild("eyeAR2", CubeListBuilder.create()
                .texOffs(774, 826).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 16.0F, -14.0F));
        root.getChild("torso").getChild("armL").addOrReplaceChild("eyeAL", CubeListBuilder.create()
                .texOffs(802, 826).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 16.0F, -16.0F));
        root.getChild("torso").getChild("armL").getChild("forearmL").addOrReplaceChild("eyeAL2", CubeListBuilder.create()
                .texOffs(834, 826).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 16.0F, -14.0F));
        root.getChild("torso").addOrReplaceChild("tentB0", CubeListBuilder.create()
                .texOffs(862, 826).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 26.0F, 10.0F, NONE),
                PartPose.offset(-24.0F, -30.0F, 26.0F));
        root.getChild("torso").getChild("tentB0").addOrReplaceChild("tentB02", CubeListBuilder.create()
                .texOffs(904, 826).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 22.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 26.0F, 0.0F));
        root.getChild("torso").addOrReplaceChild("tentB1", CubeListBuilder.create()
                .texOffs(938, 826).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 30.0F, 10.0F, NONE),
                PartPose.offset(0.0F, -34.0F, 26.0F));
        root.getChild("torso").getChild("tentB1").addOrReplaceChild("tentB12", CubeListBuilder.create()
                .texOffs(980, 826).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 24.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        root.getChild("torso").addOrReplaceChild("tentB2", CubeListBuilder.create()
                .texOffs(0, 882).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 26.0F, 10.0F, NONE),
                PartPose.offset(24.0F, -30.0F, 26.0F));
        root.getChild("torso").getChild("tentB2").addOrReplaceChild("tentB22", CubeListBuilder.create()
                .texOffs(42, 882).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 22.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 26.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeB0", CubeListBuilder.create()
                .texOffs(76, 882).addBox(0.0F, -6.0F, -6.0F, 14.0F, 12.0F, 12.0F, NONE),
                PartPose.offset(-30.0F, -20.0F, 24.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeB1", CubeListBuilder.create()
                .texOffs(130, 882).addBox(-14.0F, -6.0F, -6.0F, 14.0F, 12.0F, 12.0F, NONE),
                PartPose.offset(30.0F, -20.0F, 24.0F));
        root.getChild("torso").addOrReplaceChild("spikeB2", CubeListBuilder.create()
                .texOffs(184, 882).addBox(0.0F, -10.0F, -5.0F, 10.0F, 20.0F, 10.0F, NONE),
                PartPose.offset(-26.0F, -46.0F, 20.0F));
        root.getChild("torso").addOrReplaceChild("spikeB3", CubeListBuilder.create()
                .texOffs(226, 882).addBox(-10.0F, -10.0F, -5.0F, 10.0F, 20.0F, 10.0F, NONE),
                PartPose.offset(26.0F, -46.0F, 20.0F));
        return LayerDefinition.create(mesh, 1024, 1024);
    }

    @Override
    public void setupAnim(CultivationMonsterEntity entity, float limbSwing,
                         float limbSwingAmount, float ageInTicks,
                         float netHeadYaw, float headPitch) {

        // ────────────────────────────────────────────────────────────────
        // 【阶段入场压入 · 接口约定】renderer 侧用阶段字段驱动：
        //   p = 入场进度 0..1（EvilSky / EvilFall 阶段开始后 40t 内线性推进）
        //   renderer: pose.translate(0, ENTRANCE_DROP * (1 - p), 0)  // 从上方压下来
        //   本模型**不**自己改 root 位置（避免与阶段缩放/镜头叠加），
        //   只把各部件姿态按 p 做"收拢→展开"（下方已按常态计算，入场时由 renderer 乘外部系数）。
        // ────────────────────────────────────────────────────────────────
        float t = ageInTicks;
        final float DEG = 0.017453292F;
        // 三阶段：整个上半身「挤满天空」——躯干呼吸（只动 xScale/zScale）
        float breath = 1.0F + 0.014F * Mth.sin(t * 0.024F);
        this.torso.xScale = breath;
        this.torso.zScale = breath;
        this.torso.xRot = 0.02F * Mth.sin(t * 0.019F);
        this.chest.xRot = 0.015F * Mth.sin(t * 0.017F + 0.7F);
        // 巨颅随视线 + 下颌开合
        this.head.yRot = netHeadYaw * DEG * 0.26F;
        this.head.xRot = headPitch * DEG * 0.18F;
        this.upperJaw.xRot = 0.03F * Mth.sin(t * 0.021F);
        this.lowerJaw.xRot = 0.09F + 0.09F * Mth.sin(t * 0.021F + 0.9F);
        // 主眼脉动
        float eyePulse = 1.0F + 0.05F * Mth.sin(t * 0.033F);
        this.mainEye.xScale = eyePulse;
        this.mainEye.zScale = eyePulse;
        this.pupil.yScale = 1.0F - 0.45F * Mth.clamp(Mth.sin(t * 0.015F) * 2.6F - 2.0F, 0.0F, 1.0F);
        // 双臂异相缓摆 + **张开的基础姿势**（P1-4：正面剪影一眼是"双臂张开的上半身"，不是"腿"）
        //   上臂 zRot 外张 ±0.18（右臂 -x 侧取正、左臂 +x 侧取负）；前臂 xRot 前收 -0.20（爪离开躯干下缘）；手掌略抬 -0.10
        this.armR.xRot = 0.02F + 0.09F * Mth.sin(t * 0.043F);
        this.armL.xRot = 0.02F - 0.09F * Mth.sin(t * 0.043F + 1.2F);
        this.armR.zRot = 0.18F + 0.03F * Mth.sin(t * 0.031F);
        this.armL.zRot = -0.18F - 0.03F * Mth.sin(t * 0.031F + 0.8F);
        this.forearmR.xRot = -0.20F + 0.04F * Mth.sin(t * 0.047F + 0.5F);
        this.forearmL.xRot = -0.20F + 0.04F * Mth.sin(t * 0.047F + 2.0F);
        this.handR.xRot = -0.10F + 0.03F * Mth.sin(t * 0.055F);
        this.handL.xRot = -0.10F + 0.03F * Mth.sin(t * 0.055F + 1.6F);
        // 双爪抓握（左右异相）
        this.clawR0.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 0.0F);
        this.clawR1.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 0.9F);
        this.clawR2.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 1.8F);
        this.clawR3.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 2.7F);
        this.clawR4.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 3.6F);
        this.clawL0.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 0.0F);
        this.clawL1.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 0.9F);
        this.clawL2.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 1.8F);
        this.clawL3.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 2.7F);
        this.clawL4.xRot = 0.10F + 0.20F * Mth.sin(t * 0.07F + 3.6F);
        // 背部裂口触须 + 颅后触须
        this.tentB0.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 0.0F);
        this.tentB0.zRot = 0.08F * Mth.sin(t * 0.04F + 1.0F);
        this.tentB02.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 0.9F);
        this.tentB02.zRot = -0.10F * Mth.sin(t * 0.05F + 1.7F);
        this.tentB1.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 1.0F);
        this.tentB1.zRot = 0.08F * Mth.sin(t * 0.04F + 2.0F);
        this.tentB12.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 1.9F);
        this.tentB12.zRot = -0.10F * Mth.sin(t * 0.05F + 2.7F);
        this.tentB2.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 2.0F);
        this.tentB2.zRot = 0.08F * Mth.sin(t * 0.04F + 3.0F);
        this.tentB22.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 2.9F);
        this.tentB22.zRot = -0.10F * Mth.sin(t * 0.05F + 3.7F);
        // 全身眼睛：分件眨眼（头 8 + 躯干/胸/肩 8 + 臂 4）
        float blinke0 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 0.4F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinke1 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 2.5F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinke2 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 4.6F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinke3 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 6.7F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinke4 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 8.8F) * 3.2F - 2.4F, 0.0F, 1.0F);
        this.eyeH0.yScale = blinke0;
        this.eyeH0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.0F);
        this.eyeH1.yScale = blinke1;
        this.eyeH1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.7F);
        this.eyeH2.yScale = blinke2;
        this.eyeH2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 1.4F);
        this.eyeH3.yScale = blinke3;
        this.eyeH3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.1F);
        this.eyeH4.yScale = blinke4;
        this.eyeH4.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.8F);
        this.eyeH5.yScale = blinke0;
        this.eyeH5.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 3.5F);
        this.eyeH6.yScale = blinke1;
        this.eyeH6.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 4.2F);
        this.eyeH7.yScale = blinke2;
        this.eyeH7.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 4.9F);
        this.eyeT0.yScale = blinke3;
        this.eyeT0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 5.6F);
        this.eyeT1.yScale = blinke4;
        this.eyeT1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 6.3F);
        this.eyeT2.yScale = blinke0;
        this.eyeT2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 7.0F);
        this.eyeT3.yScale = blinke1;
        this.eyeT3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 7.7F);
        this.eyeC0.yScale = blinke2;
        this.eyeC0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 8.4F);
        this.eyeC1.yScale = blinke3;
        this.eyeC1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 9.1F);
        this.eyeS0.yScale = blinke4;
        this.eyeS0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 9.8F);
        this.eyeS1.yScale = blinke0;
        this.eyeS1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 10.5F);
        this.eyeAR.yScale = blinke1;
        this.eyeAR.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 11.2F);
        this.eyeAR2.yScale = blinke2;
        this.eyeAR2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 11.9F);
        this.eyeAL.yScale = blinke3;
        this.eyeAL.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 12.6F);
        this.eyeAL2.yScale = blinke4;
        this.eyeAL2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 13.3F);
    }
}