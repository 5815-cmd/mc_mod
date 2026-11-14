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
    private final ModelPart chestPlate;
    private final ModelPart chestPlateTop;
    private final ModelPart backPlate;
    private final ModelPart collar;
    private final ModelPart neck;
    private final ModelPart sternumL;
    private final ModelPart sternumR;
    private final ModelPart padL0;
    private final ModelPart padL1;
    private final ModelPart padL2;
    private final ModelPart spikePL0;
    private final ModelPart spikePL1;
    private final ModelPart eyePL;
    private final ModelPart padR0;
    private final ModelPart padR1;
    private final ModelPart padR2;
    private final ModelPart spikePR0;
    private final ModelPart spikePR1;
    private final ModelPart eyePR;
    private final ModelPart sleeveL;
    private final ModelPart sleeveL2;
    private final ModelPart sleeveR;
    private final ModelPart sleeveR2;
    private final ModelPart armL;
    private final ModelPart forearmL;
    private final ModelPart handL;
    private final ModelPart armR;
    private final ModelPart forearmR;
    private final ModelPart handR;
    private final ModelPart spikeAL0;
    private final ModelPart spikeAL1;
    private final ModelPart spikeAL2;
    private final ModelPart spikeAR0;
    private final ModelPart spikeAR1;
    private final ModelPart spikeAR2;
    private final ModelPart eyeAL;
    private final ModelPart eyeAL2;
    private final ModelPart eyeAL3;
    private final ModelPart eyeAR;
    private final ModelPart eyeAR2;
    private final ModelPart eyeAR3;
    private final ModelPart head;
    private final ModelPart cranium;
    private final ModelPart browL;
    private final ModelPart browR;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart maw;
    private final ModelPart fangU0;
    private final ModelPart fangU1;
    private final ModelPart fangU2;
    private final ModelPart fangU3;
    private final ModelPart fangL0;
    private final ModelPart fangL1;
    private final ModelPart fangL2;
    private final ModelPart fangL3;
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
    private final ModelPart hornL1;
    private final ModelPart hornL2;
    private final ModelPart hornR1;
    private final ModelPart hornR2;
    private final ModelPart cheekL;
    private final ModelPart cheekR;
    private final ModelPart eyeT0;
    private final ModelPart eyeT1;
    private final ModelPart eyeT2;
    private final ModelPart eyeT3;
    private final ModelPart eyeC0;
    private final ModelPart eyeC1;
    private final ModelPart eyeC2;
    private final ModelPart eyeC3;
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
    private final ModelPart spikeB4;
    private final ModelPart spikeB5;
    private final ModelPart spine0;
    private final ModelPart spine1;
    private final ModelPart spine2;
    private final ModelPart spine3;
    private final ModelPart coreL;
    private final ModelPart coreR;
    private final ModelPart spikeC0;
    private final ModelPart spikeC1;
    private final ModelPart ribL0;
    private final ModelPart ribR0;
    private final ModelPart ribL1;
    private final ModelPart ribR1;
    private final ModelPart ribL2;
    private final ModelPart ribR2;
    private final ModelPart ribL3;
    private final ModelPart ribR3;
    private final ModelPart clawL0;
    private final ModelPart clawL1;
    private final ModelPart clawL2;
    private final ModelPart clawL3;
    private final ModelPart clawL4;
    private final ModelPart clawR0;
    private final ModelPart clawR1;
    private final ModelPart clawR2;
    private final ModelPart clawR3;
    private final ModelPart clawR4;

    public EvilUpperModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.chest = root.getChild("torso").getChild("chest");
        this.chestPlate = root.getChild("torso").getChild("chest").getChild("chestPlate");
        this.chestPlateTop = root.getChild("torso").getChild("chest").getChild("chestPlateTop");
        this.backPlate = root.getChild("torso").getChild("chest").getChild("backPlate");
        this.collar = root.getChild("torso").getChild("chest").getChild("collar");
        this.neck = root.getChild("torso").getChild("chest").getChild("neck");
        this.sternumL = root.getChild("torso").getChild("chest").getChild("sternumL");
        this.sternumR = root.getChild("torso").getChild("chest").getChild("sternumR");
        this.padL0 = root.getChild("torso").getChild("chest").getChild("padL0");
        this.padL1 = root.getChild("torso").getChild("chest").getChild("padL0").getChild("padL1");
        this.padL2 = root.getChild("torso").getChild("chest").getChild("padL0").getChild("padL1").getChild("padL2");
        this.spikePL0 = root.getChild("torso").getChild("chest").getChild("padL0").getChild("padL1").getChild("spikePL0");
        this.spikePL1 = root.getChild("torso").getChild("chest").getChild("padL0").getChild("padL1").getChild("padL2").getChild("spikePL1");
        this.eyePL = root.getChild("torso").getChild("chest").getChild("padL0").getChild("eyePL");
        this.padR0 = root.getChild("torso").getChild("chest").getChild("padR0");
        this.padR1 = root.getChild("torso").getChild("chest").getChild("padR0").getChild("padR1");
        this.padR2 = root.getChild("torso").getChild("chest").getChild("padR0").getChild("padR1").getChild("padR2");
        this.spikePR0 = root.getChild("torso").getChild("chest").getChild("padR0").getChild("padR1").getChild("spikePR0");
        this.spikePR1 = root.getChild("torso").getChild("chest").getChild("padR0").getChild("padR1").getChild("padR2").getChild("spikePR1");
        this.eyePR = root.getChild("torso").getChild("chest").getChild("padR0").getChild("eyePR");
        this.sleeveL = root.getChild("torso").getChild("chest").getChild("padL0").getChild("sleeveL");
        this.sleeveL2 = root.getChild("torso").getChild("chest").getChild("padL0").getChild("sleeveL").getChild("sleeveL2");
        this.sleeveR = root.getChild("torso").getChild("chest").getChild("padR0").getChild("sleeveR");
        this.sleeveR2 = root.getChild("torso").getChild("chest").getChild("padR0").getChild("sleeveR").getChild("sleeveR2");
        this.armL = root.getChild("torso").getChild("chest").getChild("armL");
        this.forearmL = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL");
        this.handL = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL");
        this.armR = root.getChild("torso").getChild("chest").getChild("armR");
        this.forearmR = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR");
        this.handR = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR");
        this.spikeAL0 = root.getChild("torso").getChild("chest").getChild("armL").getChild("spikeAL0");
        this.spikeAL1 = root.getChild("torso").getChild("chest").getChild("armL").getChild("spikeAL1");
        this.spikeAL2 = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("spikeAL2");
        this.spikeAR0 = root.getChild("torso").getChild("chest").getChild("armR").getChild("spikeAR0");
        this.spikeAR1 = root.getChild("torso").getChild("chest").getChild("armR").getChild("spikeAR1");
        this.spikeAR2 = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("spikeAR2");
        this.eyeAL = root.getChild("torso").getChild("chest").getChild("armL").getChild("eyeAL");
        this.eyeAL2 = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("eyeAL2");
        this.eyeAL3 = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").getChild("eyeAL3");
        this.eyeAR = root.getChild("torso").getChild("chest").getChild("armR").getChild("eyeAR");
        this.eyeAR2 = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("eyeAR2");
        this.eyeAR3 = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").getChild("eyeAR3");
        this.head = root.getChild("torso").getChild("chest").getChild("neck").getChild("head");
        this.cranium = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium");
        this.browL = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("browL");
        this.browR = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("browR");
        this.upperJaw = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw");
        this.lowerJaw = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw");
        this.maw = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("maw");
        this.fangU0 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("fangU0");
        this.fangU1 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("fangU1");
        this.fangU2 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("fangU2");
        this.fangU3 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("fangU3");
        this.fangL0 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL0");
        this.fangL1 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL1");
        this.fangL2 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL2");
        this.fangL3 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL3");
        this.mainEye = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("mainEye");
        this.pupil = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("mainEye").getChild("pupil");
        this.eyeH0 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("eyeH0");
        this.eyeH1 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("eyeH1");
        this.eyeH2 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("eyeH2");
        this.eyeH3 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("eyeH3");
        this.eyeH4 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("eyeH4");
        this.eyeH5 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("eyeH5");
        this.eyeH6 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("eyeH6");
        this.eyeH7 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("eyeH7");
        this.hornL1 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("hornL1");
        this.hornL2 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("hornL1").getChild("hornL2");
        this.hornR1 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("hornR1");
        this.hornR2 = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("hornR1").getChild("hornR2");
        this.cheekL = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cheekL");
        this.cheekR = root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cheekR");
        this.eyeT0 = root.getChild("torso").getChild("eyeT0");
        this.eyeT1 = root.getChild("torso").getChild("eyeT1");
        this.eyeT2 = root.getChild("torso").getChild("eyeT2");
        this.eyeT3 = root.getChild("torso").getChild("eyeT3");
        this.eyeC0 = root.getChild("torso").getChild("chest").getChild("eyeC0");
        this.eyeC1 = root.getChild("torso").getChild("chest").getChild("eyeC1");
        this.eyeC2 = root.getChild("torso").getChild("chest").getChild("eyeC2");
        this.eyeC3 = root.getChild("torso").getChild("chest").getChild("eyeC3");
        this.tentB0 = root.getChild("torso").getChild("chest").getChild("tentB0");
        this.tentB02 = root.getChild("torso").getChild("chest").getChild("tentB0").getChild("tentB02");
        this.tentB1 = root.getChild("torso").getChild("chest").getChild("tentB1");
        this.tentB12 = root.getChild("torso").getChild("chest").getChild("tentB1").getChild("tentB12");
        this.tentB2 = root.getChild("torso").getChild("chest").getChild("tentB2");
        this.tentB22 = root.getChild("torso").getChild("chest").getChild("tentB2").getChild("tentB22");
        this.spikeB0 = root.getChild("torso").getChild("chest").getChild("spikeB0");
        this.spikeB1 = root.getChild("torso").getChild("chest").getChild("spikeB1");
        this.spikeB2 = root.getChild("torso").getChild("chest").getChild("spikeB2");
        this.spikeB3 = root.getChild("torso").getChild("chest").getChild("spikeB3");
        this.spikeB4 = root.getChild("torso").getChild("chest").getChild("spikeB4");
        this.spikeB5 = root.getChild("torso").getChild("chest").getChild("spikeB5");
        this.spine0 = root.getChild("torso").getChild("chest").getChild("spine0");
        this.spine1 = root.getChild("torso").getChild("chest").getChild("spine1");
        this.spine2 = root.getChild("torso").getChild("chest").getChild("spine2");
        this.spine3 = root.getChild("torso").getChild("chest").getChild("spine3");
        this.coreL = root.getChild("torso").getChild("chest").getChild("coreL");
        this.coreR = root.getChild("torso").getChild("chest").getChild("coreR");
        this.spikeC0 = root.getChild("torso").getChild("chest").getChild("spikeC0");
        this.spikeC1 = root.getChild("torso").getChild("chest").getChild("spikeC1");
        this.ribL0 = root.getChild("torso").getChild("chest").getChild("ribL0");
        this.ribR0 = root.getChild("torso").getChild("chest").getChild("ribR0");
        this.ribL1 = root.getChild("torso").getChild("chest").getChild("ribL1");
        this.ribR1 = root.getChild("torso").getChild("chest").getChild("ribR1");
        this.ribL2 = root.getChild("torso").getChild("chest").getChild("ribL2");
        this.ribR2 = root.getChild("torso").getChild("chest").getChild("ribR2");
        this.ribL3 = root.getChild("torso").getChild("chest").getChild("ribL3");
        this.ribR3 = root.getChild("torso").getChild("chest").getChild("ribR3");
        this.clawL0 = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL0");
        this.clawL1 = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL1");
        this.clawL2 = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL2");
        this.clawL3 = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL3");
        this.clawL4 = root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").getChild("clawL4");
        this.clawR0 = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR0");
        this.clawR1 = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR1");
        this.clawR2 = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR2");
        this.clawR3 = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR3");
        this.clawR4 = root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").getChild("clawR4");
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
                .texOffs(558, 0).addBox(-30.0F, -56.0F, -22.0F, 60.0F, 56.0F, 44.0F, NONE),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.getChild("torso").addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(0, 118).addBox(-50.0F, -34.0F, -30.0F, 100.0F, 34.0F, 60.0F, NONE),
                PartPose.offset(0.0F, -56.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("chestPlate", CubeListBuilder.create()
                .texOffs(560, 590).addBox(-48.0F, -26.0F, -4.0F, 96.0F, 26.0F, 4.0F, NONE),
                PartPose.offset(0.0F, -4.0F, -32.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("chestPlateTop", CubeListBuilder.create()
                .texOffs(276, 372).addBox(-44.0F, -6.0F, -26.0F, 88.0F, 6.0F, 52.0F, NONE),
                PartPose.offset(0.0F, -34.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("backPlate", CubeListBuilder.create()
                .texOffs(0, 590).addBox(-42.0F, -30.0F, -4.0F, 84.0F, 30.0F, 4.0F, NONE),
                PartPose.offset(0.0F, 0.0F, 30.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("collar", CubeListBuilder.create()
                .texOffs(0, 434).addBox(-30.0F, -6.0F, -24.0F, 60.0F, 6.0F, 48.0F, NONE),
                PartPose.offset(0.0F, -36.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(172, 490).addBox(-18.0F, -12.0F, -18.0F, 36.0F, 12.0F, 36.0F, NONE),
                PartPose.offset(0.0F, -30.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("sternumL", CubeListBuilder.create()
                .texOffs(212, 590).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 30.0F, 4.0F, NONE),
                PartPose.offset(-4.0F, -30.0F, -34.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("sternumR", CubeListBuilder.create()
                .texOffs(238, 590).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 30.0F, 4.0F, NONE),
                PartPose.offset(4.0F, -30.0F, -34.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("padL0", CubeListBuilder.create()
                .texOffs(760, 372).addBox(-2.0F, -8.0F, -18.0F, 22.0F, 20.0F, 36.0F, NONE),
                PartPose.offset(46.0F, -30.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padL0").addOrReplaceChild("padL1", CubeListBuilder.create()
                .texOffs(218, 434).addBox(-4.0F, -10.0F, -16.0F, 20.0F, 22.0F, 32.0F, NONE),
                PartPose.offset(10.0F, -10.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padL0").getChild("padL1").addOrReplaceChild("padL2", CubeListBuilder.create()
                .texOffs(936, 490).addBox(-5.0F, -8.0F, -14.0F, 16.0F, 18.0F, 28.0F, NONE),
                PartPose.offset(8.0F, -8.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padL0").getChild("padL1").addOrReplaceChild("spikePL0", CubeListBuilder.create()
                .texOffs(622, 626).addBox(-4.0F, -14.0F, -4.0F, 8.0F, 16.0F, 8.0F, NONE),
                PartPose.offset(18.0F, 2.0F, -8.0F));
        root.getChild("torso").getChild("chest").getChild("padL0").getChild("padL1").getChild("padL2").addOrReplaceChild("spikePL1", CubeListBuilder.create()
                .texOffs(874, 656).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 12.0F, 6.0F, NONE),
                PartPose.offset(16.0F, 0.0F, -6.0F));
        root.getChild("torso").getChild("chest").getChild("padL0").addOrReplaceChild("eyePL", CubeListBuilder.create()
                .texOffs(662, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(8.0F, -10.0F, -18.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("padR0", CubeListBuilder.create()
                .texOffs(878, 372).addBox(-20.0F, -8.0F, -18.0F, 22.0F, 20.0F, 36.0F, NONE),
                PartPose.offset(-46.0F, -30.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padR0").addOrReplaceChild("padR1", CubeListBuilder.create()
                .texOffs(324, 434).addBox(-16.0F, -10.0F, -16.0F, 20.0F, 22.0F, 32.0F, NONE),
                PartPose.offset(-10.0F, -10.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padR0").getChild("padR1").addOrReplaceChild("padR2", CubeListBuilder.create()
                .texOffs(0, 542).addBox(-11.0F, -8.0F, -14.0F, 16.0F, 18.0F, 28.0F, NONE),
                PartPose.offset(-8.0F, -8.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padR0").getChild("padR1").addOrReplaceChild("spikePR0", CubeListBuilder.create()
                .texOffs(656, 626).addBox(-4.0F, -14.0F, -4.0F, 8.0F, 16.0F, 8.0F, NONE),
                PartPose.offset(-18.0F, 2.0F, -8.0F));
        root.getChild("torso").getChild("chest").getChild("padR0").getChild("padR1").getChild("padR2").addOrReplaceChild("spikePR1", CubeListBuilder.create()
                .texOffs(900, 656).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 12.0F, 6.0F, NONE),
                PartPose.offset(-16.0F, 0.0F, -6.0F));
        root.getChild("torso").getChild("chest").getChild("padR0").addOrReplaceChild("eyePR", CubeListBuilder.create()
                .texOffs(690, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(2.0F, -10.0F, -18.0F));
        root.getChild("torso").getChild("chest").getChild("padL0").addOrReplaceChild("sleeveL", CubeListBuilder.create()
                .texOffs(318, 490).addBox(-16.0F, 0.0F, -16.0F, 32.0F, 16.0F, 32.0F, NONE),
                PartPose.offset(2.0F, 20.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padL0").getChild("sleeveL").addOrReplaceChild("sleeveL2", CubeListBuilder.create()
                .texOffs(578, 490).addBox(-14.0F, 0.0F, -14.0F, 28.0F, 20.0F, 28.0F, NONE),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padR0").addOrReplaceChild("sleeveR", CubeListBuilder.create()
                .texOffs(448, 490).addBox(-16.0F, 0.0F, -16.0F, 32.0F, 16.0F, 32.0F, NONE),
                PartPose.offset(-2.0F, 20.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("padR0").getChild("sleeveR").addOrReplaceChild("sleeveR2", CubeListBuilder.create()
                .texOffs(692, 490).addBox(-14.0F, 0.0F, -14.0F, 28.0F, 20.0F, 28.0F, NONE),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("armL", CubeListBuilder.create()
                .texOffs(322, 118).addBox(-17.0F, 0.0F, -17.0F, 34.0F, 60.0F, 34.0F, NONE),
                PartPose.offset(56.0F, -16.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armL").addOrReplaceChild("forearmL", CubeListBuilder.create()
                .texOffs(598, 118).addBox(-15.0F, 0.0F, -15.0F, 30.0F, 58.0F, 30.0F, NONE),
                PartPose.offset(0.0F, 60.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").addOrReplaceChild("handL", CubeListBuilder.create()
                .texOffs(202, 302).addBox(-20.0F, 0.0F, -20.0F, 40.0F, 26.0F, 40.0F, NONE),
                PartPose.offset(0.0F, 58.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("armR", CubeListBuilder.create()
                .texOffs(460, 118).addBox(-17.0F, 0.0F, -17.0F, 34.0F, 60.0F, 34.0F, NONE),
                PartPose.offset(-56.0F, -16.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armR").addOrReplaceChild("forearmR", CubeListBuilder.create()
                .texOffs(720, 118).addBox(-15.0F, 0.0F, -15.0F, 30.0F, 58.0F, 30.0F, NONE),
                PartPose.offset(0.0F, 60.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").addOrReplaceChild("handR", CubeListBuilder.create()
                .texOffs(364, 302).addBox(-20.0F, 0.0F, -20.0F, 40.0F, 26.0F, 40.0F, NONE),
                PartPose.offset(0.0F, 58.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armL").addOrReplaceChild("spikeAL0", CubeListBuilder.create()
                .texOffs(822, 626).addBox(0.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(17.0F, 12.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armL").addOrReplaceChild("spikeAL1", CubeListBuilder.create()
                .texOffs(330, 678).addBox(0.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(17.0F, 30.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").addOrReplaceChild("spikeAL2", CubeListBuilder.create()
                .texOffs(364, 678).addBox(0.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(15.0F, 24.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armR").addOrReplaceChild("spikeAR0", CubeListBuilder.create()
                .texOffs(864, 626).addBox(-10.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(-17.0F, 12.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armR").addOrReplaceChild("spikeAR1", CubeListBuilder.create()
                .texOffs(398, 678).addBox(-8.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(-17.0F, 30.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").addOrReplaceChild("spikeAR2", CubeListBuilder.create()
                .texOffs(432, 678).addBox(-8.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(-15.0F, 24.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("armL").addOrReplaceChild("eyeAL", CubeListBuilder.create()
                .texOffs(874, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 22.0F, -18.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").addOrReplaceChild("eyeAL2", CubeListBuilder.create()
                .texOffs(718, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 20.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("eyeAL3", CubeListBuilder.create()
                .texOffs(28, 714).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 8.0F, 2.0F, NONE),
                PartPose.offset(0.0F, 10.0F, -21.0F));
        root.getChild("torso").getChild("chest").getChild("armR").addOrReplaceChild("eyeAR", CubeListBuilder.create()
                .texOffs(906, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 22.0F, -18.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").addOrReplaceChild("eyeAR2", CubeListBuilder.create()
                .texOffs(746, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 20.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("eyeAR3", CubeListBuilder.create()
                .texOffs(50, 714).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 8.0F, 2.0F, NONE),
                PartPose.offset(0.0F, 10.0F, -21.0F));
        root.getChild("torso").getChild("chest").getChild("neck").addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 214).addBox(-32.0F, -34.0F, -26.0F, 64.0F, 34.0F, 52.0F, NONE),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("cranium", CubeListBuilder.create()
                .texOffs(526, 302).addBox(-28.0F, -16.0F, -24.0F, 56.0F, 16.0F, 48.0F, NONE),
                PartPose.offset(0.0F, -34.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").addOrReplaceChild("browL", CubeListBuilder.create()
                .texOffs(244, 697).addBox(-8.0F, -6.0F, -4.0F, 8.0F, 6.0F, 8.0F, NONE),
                PartPose.offset(-24.0F, -2.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").addOrReplaceChild("browR", CubeListBuilder.create()
                .texOffs(278, 697).addBox(-8.0F, -6.0F, -4.0F, 8.0F, 6.0F, 8.0F, NONE),
                PartPose.offset(24.0F, -2.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("upperJaw", CubeListBuilder.create()
                .texOffs(558, 372).addBox(-28.0F, 0.0F, -24.0F, 56.0F, 14.0F, 44.0F, NONE),
                PartPose.offset(0.0F, -2.0F, -20.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").addOrReplaceChild("lowerJaw", CubeListBuilder.create()
                .texOffs(430, 434).addBox(-24.0F, 0.0F, -22.0F, 48.0F, 12.0F, 40.0F, NONE),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").addOrReplaceChild("maw", CubeListBuilder.create()
                .texOffs(132, 542).addBox(-22.0F, 0.0F, -16.0F, 44.0F, 10.0F, 32.0F, NONE),
                PartPose.offset(0.0F, 12.0F, -4.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").addOrReplaceChild("fangU0", CubeListBuilder.create()
                .texOffs(690, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 16.0F, 8.0F, NONE),
                PartPose.offset(-18.0F, 14.0F, -20.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").addOrReplaceChild("fangU1", CubeListBuilder.create()
                .texOffs(102, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 19.0F, 8.0F, NONE),
                PartPose.offset(-6.0F, 14.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").addOrReplaceChild("fangU2", CubeListBuilder.create()
                .texOffs(136, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 19.0F, 8.0F, NONE),
                PartPose.offset(6.0F, 14.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").addOrReplaceChild("fangU3", CubeListBuilder.create()
                .texOffs(724, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 16.0F, 8.0F, NONE),
                PartPose.offset(18.0F, 14.0F, -20.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL0", CubeListBuilder.create()
                .texOffs(806, 656).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, NONE),
                PartPose.offset(-16.0F, 12.0F, -18.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL1", CubeListBuilder.create()
                .texOffs(466, 678).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(-5.0F, 12.0F, -20.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL2", CubeListBuilder.create()
                .texOffs(500, 678).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(5.0F, 12.0F, -20.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL3", CubeListBuilder.create()
                .texOffs(840, 656).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, NONE),
                PartPose.offset(16.0F, 12.0F, -18.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("mainEye", CubeListBuilder.create()
                .texOffs(264, 590).addBox(-15.0F, -15.0F, -3.0F, 30.0F, 30.0F, 3.0F, NONE),
                PartPose.offset(0.0F, -10.0F, -26.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("mainEye").addOrReplaceChild("pupil", CubeListBuilder.create()
                .texOffs(312, 697).addBox(-6.0F, -6.0F, -2.0F, 12.0F, 12.0F, 2.0F, NONE),
                PartPose.offset(0.0F, 0.0F, -3.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("eyeH0", CubeListBuilder.create()
                .texOffs(774, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-24.0F, -28.0F, -24.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("eyeH1", CubeListBuilder.create()
                .texOffs(802, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(24.0F, -28.0F, -24.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("eyeH2", CubeListBuilder.create()
                .texOffs(830, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-24.0F, -8.0F, -24.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("eyeH3", CubeListBuilder.create()
                .texOffs(858, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(24.0F, -8.0F, -24.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").addOrReplaceChild("eyeH4", CubeListBuilder.create()
                .texOffs(886, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-14.0F, -10.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").addOrReplaceChild("eyeH5", CubeListBuilder.create()
                .texOffs(914, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(14.0F, -10.0F, -22.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("eyeH6", CubeListBuilder.create()
                .texOffs(340, 656).addBox(0.0F, -5.0F, -5.0F, 3.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(-30.0F, -18.0F, 2.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("eyeH7", CubeListBuilder.create()
                .texOffs(368, 656).addBox(-3.0F, -5.0F, -5.0F, 3.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(30.0F, -18.0F, 2.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").addOrReplaceChild("hornL1", CubeListBuilder.create()
                .texOffs(906, 626).addBox(0.0F, -10.0F, -5.0F, 8.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(22.0F, -10.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("hornL1").addOrReplaceChild("hornL2", CubeListBuilder.create()
                .texOffs(342, 697).addBox(0.0F, -6.0F, -4.0F, 6.0F, 6.0F, 8.0F, NONE),
                PartPose.offset(6.0F, -10.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").addOrReplaceChild("hornR1", CubeListBuilder.create()
                .texOffs(944, 626).addBox(-8.0F, -10.0F, -5.0F, 8.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(-22.0F, -10.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").getChild("cranium").getChild("hornR1").addOrReplaceChild("hornR2", CubeListBuilder.create()
                .texOffs(372, 697).addBox(-6.0F, -6.0F, -4.0F, 6.0F, 6.0F, 8.0F, NONE),
                PartPose.offset(-6.0F, -10.0F, 0.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("cheekL", CubeListBuilder.create()
                .texOffs(220, 678).addBox(0.0F, -4.0F, -4.0F, 10.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(30.0F, -4.0F, -6.0F));
        root.getChild("torso").getChild("chest").getChild("neck").getChild("head").addOrReplaceChild("cheekR", CubeListBuilder.create()
                .texOffs(258, 678).addBox(-10.0F, -4.0F, -4.0F, 10.0F, 8.0F, 8.0F, NONE),
                PartPose.offset(-30.0F, -4.0F, -6.0F));
        root.getChild("torso").addOrReplaceChild("eyeT0", CubeListBuilder.create()
                .texOffs(938, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-18.0F, -30.0F, -24.0F));
        root.getChild("torso").addOrReplaceChild("eyeT1", CubeListBuilder.create()
                .texOffs(970, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(18.0F, -30.0F, -24.0F));
        root.getChild("torso").addOrReplaceChild("eyeT2", CubeListBuilder.create()
                .texOffs(0, 697).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-18.0F, -8.0F, -24.0F));
        root.getChild("torso").addOrReplaceChild("eyeT3", CubeListBuilder.create()
                .texOffs(32, 697).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(18.0F, -8.0F, -24.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("eyeC0", CubeListBuilder.create()
                .texOffs(942, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-30.0F, -24.0F, -33.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("eyeC1", CubeListBuilder.create()
                .texOffs(970, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(30.0F, -24.0F, -33.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("eyeC2", CubeListBuilder.create()
                .texOffs(998, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-14.0F, -14.0F, -34.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("eyeC3", CubeListBuilder.create()
                .texOffs(0, 714).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(14.0F, -14.0F, -34.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("tentB0", CubeListBuilder.create()
                .texOffs(792, 542).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 26.0F, 10.0F, NONE),
                PartPose.offset(-24.0F, -30.0F, 30.0F));
        root.getChild("torso").getChild("chest").getChild("tentB0").addOrReplaceChild("tentB02", CubeListBuilder.create()
                .texOffs(762, 590).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 22.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 26.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("tentB1", CubeListBuilder.create()
                .texOffs(370, 542).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 30.0F, 10.0F, NONE),
                PartPose.offset(0.0F, -34.0F, 30.0F));
        root.getChild("torso").getChild("chest").getChild("tentB1").addOrReplaceChild("tentB12", CubeListBuilder.create()
                .texOffs(526, 590).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 24.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("tentB2", CubeListBuilder.create()
                .texOffs(834, 542).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 26.0F, 10.0F, NONE),
                PartPose.offset(24.0F, -30.0F, 30.0F));
        root.getChild("torso").getChild("chest").getChild("tentB2").addOrReplaceChild("tentB22", CubeListBuilder.create()
                .texOffs(796, 590).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 22.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 26.0F, 0.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeB0", CubeListBuilder.create()
                .texOffs(830, 590).addBox(0.0F, -8.0F, -6.0F, 12.0F, 16.0F, 12.0F, NONE),
                PartPose.offset(-40.0F, -34.0F, 26.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeB1", CubeListBuilder.create()
                .texOffs(880, 590).addBox(-12.0F, -8.0F, -6.0F, 12.0F, 16.0F, 12.0F, NONE),
                PartPose.offset(40.0F, -34.0F, 26.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeB2", CubeListBuilder.create()
                .texOffs(170, 626).addBox(0.0F, -8.0F, -5.0F, 10.0F, 16.0F, 10.0F, NONE),
                PartPose.offset(-30.0F, -40.0F, 28.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeB3", CubeListBuilder.create()
                .texOffs(212, 626).addBox(-10.0F, -8.0F, -5.0F, 10.0F, 16.0F, 10.0F, NONE),
                PartPose.offset(30.0F, -40.0F, 28.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeB4", CubeListBuilder.create()
                .texOffs(204, 656).addBox(0.0F, -6.0F, -4.0F, 8.0F, 12.0F, 8.0F, NONE),
                PartPose.offset(-18.0F, -42.0F, 28.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeB5", CubeListBuilder.create()
                .texOffs(238, 656).addBox(-8.0F, -6.0F, -4.0F, 8.0F, 12.0F, 8.0F, NONE),
                PartPose.offset(18.0F, -42.0F, 28.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spine0", CubeListBuilder.create()
                .texOffs(72, 678).addBox(-16.0F, -5.0F, -4.0F, 32.0F, 10.0F, 6.0F, NONE),
                PartPose.offset(0.0F, -34.0F, 34.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spine1", CubeListBuilder.create()
                .texOffs(150, 678).addBox(-14.0F, -5.0F, -4.0F, 28.0F, 10.0F, 6.0F, NONE),
                PartPose.offset(0.0F, -26.0F, 34.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spine2", CubeListBuilder.create()
                .texOffs(128, 697).addBox(-12.0F, -4.0F, -4.0F, 24.0F, 8.0F, 6.0F, NONE),
                PartPose.offset(0.0F, -18.0F, 34.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spine3", CubeListBuilder.create()
                .texOffs(190, 697).addBox(-10.0F, -4.0F, -4.0F, 20.0F, 8.0F, 6.0F, NONE),
                PartPose.offset(0.0F, -10.0F, 34.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("coreL", CubeListBuilder.create()
                .texOffs(64, 697).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-10.0F, -22.0F, -36.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("coreR", CubeListBuilder.create()
                .texOffs(96, 697).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(10.0F, -22.0F, -36.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeC0", CubeListBuilder.create()
                .texOffs(272, 656).addBox(0.0F, -6.0F, -4.0F, 8.0F, 12.0F, 8.0F, NONE),
                PartPose.offset(-24.0F, -32.0F, 20.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("spikeC1", CubeListBuilder.create()
                .texOffs(306, 656).addBox(-8.0F, -6.0F, -4.0F, 8.0F, 12.0F, 8.0F, NONE),
                PartPose.offset(24.0F, -32.0F, 20.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("ribL0", CubeListBuilder.create()
                .texOffs(72, 714).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(-26.0F, -26.0F, -35.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("ribR0", CubeListBuilder.create()
                .texOffs(154, 714).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(26.0F, -26.0F, -35.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("ribL1", CubeListBuilder.create()
                .texOffs(236, 714).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(-26.0F, -19.0F, -35.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("ribR1", CubeListBuilder.create()
                .texOffs(318, 714).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(26.0F, -19.0F, -35.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("ribL2", CubeListBuilder.create()
                .texOffs(400, 714).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(-26.0F, -12.0F, -35.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("ribR2", CubeListBuilder.create()
                .texOffs(482, 714).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(26.0F, -12.0F, -35.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("ribL3", CubeListBuilder.create()
                .texOffs(564, 714).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(-26.0F, -5.0F, -35.0F));
        root.getChild("torso").getChild("chest").addOrReplaceChild("ribR3", CubeListBuilder.create()
                .texOffs(646, 714).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 4.0F, 4.0F, NONE),
                PartPose.offset(26.0F, -5.0F, -35.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL0", CubeListBuilder.create()
                .texOffs(412, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(-16.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL1", CubeListBuilder.create()
                .texOffs(450, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(-8.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL2", CubeListBuilder.create()
                .texOffs(488, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(0.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL3", CubeListBuilder.create()
                .texOffs(526, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(8.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").addOrReplaceChild("clawL4", CubeListBuilder.create()
                .texOffs(564, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(16.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR0", CubeListBuilder.create()
                .texOffs(602, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(-16.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR1", CubeListBuilder.create()
                .texOffs(640, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(-8.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR2", CubeListBuilder.create()
                .texOffs(678, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(0.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR3", CubeListBuilder.create()
                .texOffs(716, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(8.0F, 24.0F, -16.0F));
        root.getChild("torso").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").addOrReplaceChild("clawR4", CubeListBuilder.create()
                .texOffs(754, 542).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 26.0F, 12.0F, NONE),
                PartPose.offset(16.0F, 24.0F, -16.0F));
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
        // 三阶段：巨神上半身「挤满天空」——躯干呼吸（只动 xScale/zScale）
        float breath = 1.0F + 0.016F * Mth.sin(t * 0.024F);
        this.torso.xScale = breath;
        this.torso.zScale = breath;
        this.torso.xRot = 0.02F * Mth.sin(t * 0.019F);
        this.chest.xRot = 0.015F * Mth.sin(t * 0.017F + 0.7F);
        // 巨颅随视线 + 下颌开合
        this.head.yRot = netHeadYaw * DEG * 0.26F;
        this.head.xRot = headPitch * DEG * 0.18F;
        this.upperJaw.xRot = 0.03F + 0.03F * Mth.sin(t * 0.021F);
        this.lowerJaw.xRot = 0.10F + 0.10F * Mth.sin(t * 0.021F + 0.9F);
        // 主眼脉动
        float eyePulse = 1.0F + 0.05F * Mth.sin(t * 0.033F);
        this.mainEye.xScale = eyePulse;
        this.mainEye.zScale = eyePulse;
        this.pupil.yScale = 1.0F - 0.45F * Mth.clamp(Mth.sin(t * 0.015F) * 2.6F - 2.0F, 0.0F, 1.0F);
        // 巨肩甲/垂袖：极慢沉浮（左右异相，P1-4 后仍保留'双臂张开'的基础姿势）
        this.padL0.zRot = -0.020F + 0.012F * Mth.sin(t * 0.018F);
        this.padR0.zRot = 0.020F - 0.012F * Mth.sin(t * 0.018F + 0.8F);
        this.sleeveL.xRot = 0.02F + 0.02F * Mth.sin(t * 0.021F + 0.5F);
        this.sleeveR.xRot = 0.02F - 0.02F * Mth.sin(t * 0.021F + 1.3F);
        // 双臂异相缓摆 + 张开的基础姿势（正面剪影=巨神张开双臂）
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
        // 背部裂口触须（3 根错相）
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
        // 全身眼睛：分件眨眼（巨颅 8 + 主眼 + 躯 4 + 胸 4 + 肩 2 + 臂 6）
        float blinke0 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 0.4F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinke1 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 2.5F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinke2 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 4.6F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinke3 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 6.7F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinke4 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 8.8F) * 3.2F - 2.4F, 0.0F, 1.0F);
        this.eyePL.yScale = blinke0;
        this.eyePL.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.0F);
        this.eyePR.yScale = blinke1;
        this.eyePR.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.7F);
        this.eyeAL.yScale = blinke2;
        this.eyeAL.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 1.4F);
        this.eyeAL2.yScale = blinke3;
        this.eyeAL2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.1F);
        this.eyeAL3.yScale = blinke4;
        this.eyeAL3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.8F);
        this.eyeAR.yScale = blinke0;
        this.eyeAR.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 3.5F);
        this.eyeAR2.yScale = blinke1;
        this.eyeAR2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 4.2F);
        this.eyeAR3.yScale = blinke2;
        this.eyeAR3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 4.9F);
        this.eyeH0.yScale = blinke3;
        this.eyeH0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 5.6F);
        this.eyeH1.yScale = blinke4;
        this.eyeH1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 6.3F);
        this.eyeH2.yScale = blinke0;
        this.eyeH2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 7.0F);
        this.eyeH3.yScale = blinke1;
        this.eyeH3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 7.7F);
        this.eyeH4.yScale = blinke2;
        this.eyeH4.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 8.4F);
        this.eyeH5.yScale = blinke3;
        this.eyeH5.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 9.1F);
        this.eyeH6.yScale = blinke4;
        this.eyeH6.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 9.8F);
        this.eyeH7.yScale = blinke0;
        this.eyeH7.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 10.5F);
        this.eyeT0.yScale = blinke1;
        this.eyeT0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 11.2F);
        this.eyeT1.yScale = blinke2;
        this.eyeT1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 11.9F);
        this.eyeT2.yScale = blinke3;
        this.eyeT2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 12.6F);
        this.eyeT3.yScale = blinke4;
        this.eyeT3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 13.3F);
        this.eyeC0.yScale = blinke0;
        this.eyeC0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 14.0F);
        this.eyeC1.yScale = blinke1;
        this.eyeC1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 14.7F);
        this.eyeC2.yScale = blinke2;
        this.eyeC2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 15.4F);
        this.eyeC3.yScale = blinke3;
        this.eyeC3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 16.1F);
    }
}