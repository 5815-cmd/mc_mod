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
 * 虚空龙帝 · VoidDragonEmperor / VoidDragonEmperorModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>GoT 风虚空巨龙：前倾巨颅 + 长 S 颈 + 抬升 35~55° 的巨翼（4 指骨+4 片竖直指间膜）+ 粗后腿/小前爪 + 后下拖长尾 + 发光裂纹</p>
 */
public class VoidDragonEmperorModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart neck1;
    private final ModelPart tail1;
    private final ModelPart wingL;
    private final ModelPart wingR;
    private final ModelPart foreL;
    private final ModelPart foreR;
    private final ModelPart neck2;
    private final ModelPart shinL;
    private final ModelPart shinR;
    private final ModelPart tail2;
    private final ModelPart wingArmL;
    private final ModelPart wingArmR;
    private final ModelPart footL;
    private final ModelPart footR;
    private final ModelPart membraneArmL;
    private final ModelPart membraneArmR;
    private final ModelPart neck3;
    private final ModelPart tail3;
    private final ModelPart wingClawL;
    private final ModelPart wingClawR;
    private final ModelPart wingForeL;
    private final ModelPart wingForeR;
    private final ModelPart head;
    private final ModelPart membraneForeL;
    private final ModelPart membraneForeR;
    private final ModelPart tail4;
    private final ModelPart wingWristL;
    private final ModelPart wingWristR;
    private final ModelPart fingerL0;
    private final ModelPart fingerL1;
    private final ModelPart fingerL2;
    private final ModelPart fingerL3;
    private final ModelPart fingerR0;
    private final ModelPart fingerR1;
    private final ModelPart fingerR2;
    private final ModelPart fingerR3;
    private final ModelPart lowerJaw;
    private final ModelPart membraneL0;
    private final ModelPart membraneL1;
    private final ModelPart membraneL2;
    private final ModelPart membraneL3;
    private final ModelPart membraneL4;
    private final ModelPart membraneL5;
    private final ModelPart membraneR0;
    private final ModelPart membraneR1;
    private final ModelPart membraneR2;
    private final ModelPart membraneR3;
    private final ModelPart membraneR4;
    private final ModelPart membraneR5;
    private final ModelPart tail5;
    private final ModelPart tailBlade;

    public VoidDragonEmperorModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.armL = root.getChild("torso").getChild("armL");
        this.armR = root.getChild("torso").getChild("armR");
        this.legL = root.getChild("torso").getChild("legL");
        this.legR = root.getChild("torso").getChild("legR");
        this.neck1 = root.getChild("torso").getChild("neck1");
        this.tail1 = root.getChild("torso").getChild("tail1");
        this.wingL = root.getChild("torso").getChild("wingL");
        this.wingR = root.getChild("torso").getChild("wingR");
        this.foreL = root.getChild("torso").getChild("armL").getChild("foreL");
        this.foreR = root.getChild("torso").getChild("armR").getChild("foreR");
        this.neck2 = root.getChild("torso").getChild("neck1").getChild("neck2");
        this.shinL = root.getChild("torso").getChild("legL").getChild("shinL");
        this.shinR = root.getChild("torso").getChild("legR").getChild("shinR");
        this.tail2 = root.getChild("torso").getChild("tail1").getChild("tail2");
        this.wingArmL = root.getChild("torso").getChild("wingL").getChild("wingArmL");
        this.wingArmR = root.getChild("torso").getChild("wingR").getChild("wingArmR");
        this.footL = root.getChild("torso").getChild("legL").getChild("shinL").getChild("footL");
        this.footR = root.getChild("torso").getChild("legR").getChild("shinR").getChild("footR");
        this.membraneArmL = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("membraneArmL");
        this.membraneArmR = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("membraneArmR");
        this.neck3 = root.getChild("torso").getChild("neck1").getChild("neck2").getChild("neck3");
        this.tail3 = root.getChild("torso").getChild("tail1").getChild("tail2").getChild("tail3");
        this.wingClawL = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingClawL");
        this.wingClawR = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingClawR");
        this.wingForeL = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL");
        this.wingForeR = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR");
        this.head = root.getChild("torso").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head");
        this.membraneForeL = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("membraneForeL");
        this.membraneForeR = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("membraneForeR");
        this.tail4 = root.getChild("torso").getChild("tail1").getChild("tail2").getChild("tail3").getChild("tail4");
        this.wingWristL = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL");
        this.wingWristR = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR");
        this.fingerL0 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("fingerL0");
        this.fingerL1 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("fingerL1");
        this.fingerL2 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("fingerL2");
        this.fingerL3 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("fingerL3");
        this.fingerR0 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("fingerR0");
        this.fingerR1 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("fingerR1");
        this.fingerR2 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("fingerR2");
        this.fingerR3 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("fingerR3");
        this.lowerJaw = root.getChild("torso").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").getChild("lowerJaw");
        this.membraneL0 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("membraneL0");
        this.membraneL1 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("membraneL1");
        this.membraneL2 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("membraneL2");
        this.membraneL3 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("membraneL3");
        this.membraneL4 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("membraneL4");
        this.membraneL5 = root.getChild("torso").getChild("wingL").getChild("wingArmL").getChild("wingForeL").getChild("wingWristL").getChild("membraneL5");
        this.membraneR0 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("membraneR0");
        this.membraneR1 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("membraneR1");
        this.membraneR2 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("membraneR2");
        this.membraneR3 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("membraneR3");
        this.membraneR4 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("membraneR4");
        this.membraneR5 = root.getChild("torso").getChild("wingR").getChild("wingArmR").getChild("wingForeR").getChild("wingWristR").getChild("membraneR5");
        this.tail5 = root.getChild("torso").getChild("tail1").getChild("tail2").getChild("tail3").getChild("tail4").getChild("tail5");
        this.tailBlade = root.getChild("torso").getChild("tail1").getChild("tail2").getChild("tail3").getChild("tail4").getChild("tail5").getChild("tailBlade");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild("torso",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-13.0F, -22.0F, -20.0F, 26.0F, 22.0F, 20.0F, NONE)
        .texOffs(194, 36).addBox(-9.0F, -7.0F, -9.0F, 18.0F, 7.0F, 18.0F, NONE)
        .texOffs(316, 63).addBox(-8.0F, -4.0F, -4.0F, 16.0F, 4.0F, 16.0F, NONE)
        .texOffs(55, 43).addBox(-8.0F, -20.0F, -21.0F, 16.0F, 14.0F, 2.0F, NONE)
        .texOffs(486, 75).addBox(-1.0F, -32.0F, -12.0F, 2.0F, 2.0F, 10.0F, NONE)
        .texOffs(181, 87).addBox(-1.5F, -31.0F, -10.0F, 3.0F, 4.0F, 6.0F, NONE)
        .texOffs(486, 88).addBox(-1.5F, -29.0F, -2.0F, 3.0F, 4.0F, 6.0F, NONE)
        .texOffs(381, 63).addBox(12.0F, -27.0F, -9.0F, 11.0F, 8.0F, 12.0F, NONE)
        .texOffs(381, 63).mirror().addBox(-23.0F, -27.0F, -9.0F, 11.0F, 8.0F, 12.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition armL = torso.addOrReplaceChild("armL",
CubeListBuilder.create()
        .texOffs(114, 64).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 9.0F, 7.0F, NONE)
                , PartPose.offset(8.0F, -24.0F, -14.0F));
        PartDefinition armR = torso.addOrReplaceChild("armR",
CubeListBuilder.create()
        .texOffs(114, 64).mirror().addBox(-3.5F, 0.0F, -3.5F, 7.0F, 9.0F, 7.0F, NONE)
                , PartPose.offset(-8.0F, -24.0F, -14.0F));
        PartDefinition legL = torso.addOrReplaceChild("legL",
CubeListBuilder.create()
        .texOffs(433, 0).addBox(-9.0F, 0.0F, -7.0F, 18.0F, 16.0F, 15.0F, NONE)
                , PartPose.offset(11.0F, 0.0F, 0.0F));
        PartDefinition legR = torso.addOrReplaceChild("legR",
CubeListBuilder.create()
        .texOffs(433, 0).mirror().addBox(-9.0F, 0.0F, -7.0F, 18.0F, 16.0F, 15.0F, NONE)
                , PartPose.offset(-11.0F, 0.0F, 0.0F));
        PartDefinition neck1 = torso.addOrReplaceChild("neck1",
CubeListBuilder.create()
        .texOffs(271, 33).addBox(-6.0F, -13.0F, -6.5F, 12.0F, 13.0F, 13.0F, NONE)
        .texOffs(128, 81).addBox(-3.0F, -13.0F, -7.5F, 6.0F, 3.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -22.0F, -12.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition tail1 = torso.addOrReplaceChild("tail1",
CubeListBuilder.create()
        .texOffs(194, 0).addBox(-6.0F, -5.0F, 0.0F, 12.0F, 9.0F, 26.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 15.0F));
        PartDefinition wingL = torso.addOrReplaceChild("wingL",
CubeListBuilder.create()
        .texOffs(93, 38).addBox(-3.0F, -7.0F, -8.0F, 14.0F, 9.0F, 16.0F, NONE)
                , PartPose.offset(14.0F, -26.0F, -2.0F));
        PartDefinition wingR = torso.addOrReplaceChild("wingR",
CubeListBuilder.create()
        .texOffs(93, 38).mirror().addBox(-11.0F, -7.0F, -8.0F, 14.0F, 9.0F, 16.0F, NONE)
                , PartPose.offset(-14.0F, -26.0F, -2.0F));
        PartDefinition foreL = armL.addOrReplaceChild("foreL",
CubeListBuilder.create()
        .texOffs(486, 61).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F, NONE)
        .texOffs(500, 0).addBox(-2.5F, 8.0F, -10.0F, 2.0F, 3.0F, 4.0F, NONE)
        .texOffs(500, 8).addBox(0.0F, 8.0F, -10.0F, 2.0F, 3.0F, 4.0F, NONE)
        .texOffs(500, 16).addBox(2.5F, 8.0F, -10.0F, 2.0F, 3.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 9.0F, 0.0F));
        PartDefinition foreR = armR.addOrReplaceChild("foreR",
CubeListBuilder.create()
        .texOffs(486, 61).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F, NONE)
        .texOffs(500, 0).mirror().addBox(0.5F, 8.0F, -10.0F, 2.0F, 3.0F, 4.0F, NONE)
        .texOffs(500, 8).mirror().addBox(-2.0F, 8.0F, -10.0F, 2.0F, 3.0F, 4.0F, NONE)
        .texOffs(500, 16).mirror().addBox(-4.5F, 8.0F, -10.0F, 2.0F, 3.0F, 4.0F, NONE)
                , PartPose.offset(-0.0F, 9.0F, 0.0F));
        PartDefinition neck2 = neck1.addOrReplaceChild("neck2",
CubeListBuilder.create()
        .texOffs(267, 60).addBox(-6.0F, -13.0F, -6.0F, 12.0F, 13.0F, 12.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.1F, 0.0F, 0.0F));
        PartDefinition shinL = legL.addOrReplaceChild("shinL",
CubeListBuilder.create()
        .texOffs(154, 62).addBox(-6.0F, 0.0F, -5.0F, 12.0F, 12.0F, 12.0F, NONE)
                , PartPose.offset(0.0F, 16.0F, 2.0F));
        PartDefinition shinR = legR.addOrReplaceChild("shinR",
CubeListBuilder.create()
        .texOffs(154, 62).mirror().addBox(-6.0F, 0.0F, -5.0F, 12.0F, 12.0F, 12.0F, NONE)
                , PartPose.offset(-0.0F, 16.0F, 2.0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2",
CubeListBuilder.create()
        .texOffs(364, 0).addBox(-5.0F, -4.0F, -0.5F, 10.0F, 7.0F, 24.0F, NONE)
        .texOffs(19, 90).addBox(-1.5F, -5.0F, 5.0F, 3.0F, 4.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 4.0F, 26.0F));
        PartDefinition wingArmL = wingL.addOrReplaceChild("wingArmL",
CubeListBuilder.create()
        .texOffs(181, 104).addBox(0.0F, -4.0F, -4.0F, 32.0F, 8.0F, 8.0F, NONE)
        .texOffs(0, 130).addBox(0.0F, -5.0F, -5.0F, 32.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, -2.0F, 0.0F, 0.0F, 0.0F, -0.6F));
        PartDefinition wingArmR = wingR.addOrReplaceChild("wingArmR",
CubeListBuilder.create()
        .texOffs(181, 104).mirror().addBox(-32.0F, -4.0F, -4.0F, 32.0F, 8.0F, 8.0F, NONE)
        .texOffs(0, 130).mirror().addBox(-32.0F, -5.0F, -5.0F, 32.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, -2.0F, 0.0F, 0.0F, -0.0F, 0.6F));
        PartDefinition footL = shinL.addOrReplaceChild("footL",
CubeListBuilder.create()
        .texOffs(55, 64).addBox(-6.5F, 0.0F, -10.0F, 13.0F, 4.0F, 16.0F, NONE)
        .texOffs(490, 110).addBox(-5.0F, 1.0F, -16.0F, 3.0F, 3.0F, 6.0F, NONE)
        .texOffs(288, 86).addBox(-1.5F, 1.0F, -17.0F, 3.0F, 3.0F, 7.0F, NONE)
        .texOffs(481, 120).addBox(2.0F, 1.0F, -16.0F, 3.0F, 3.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 12.0F, -2.0F));
        PartDefinition footR = shinR.addOrReplaceChild("footR",
CubeListBuilder.create()
        .texOffs(55, 64).mirror().addBox(-6.5F, 0.0F, -10.0F, 13.0F, 4.0F, 16.0F, NONE)
        .texOffs(490, 110).mirror().addBox(2.0F, 1.0F, -16.0F, 3.0F, 3.0F, 6.0F, NONE)
        .texOffs(288, 86).mirror().addBox(-1.5F, 1.0F, -17.0F, 3.0F, 3.0F, 7.0F, NONE)
        .texOffs(481, 120).mirror().addBox(-5.0F, 1.0F, -16.0F, 3.0F, 3.0F, 6.0F, NONE)
                , PartPose.offset(-0.0F, 12.0F, -2.0F));
        PartDefinition membraneArmL = wingArmL.addOrReplaceChild("membraneArmL",
CubeListBuilder.create()
        .texOffs(364, 32).addBox(0.0F, -14.0F, -2.0F, 34.0F, 28.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition membraneArmR = wingArmR.addOrReplaceChild("membraneArmR",
CubeListBuilder.create()
        .texOffs(364, 32).mirror().addBox(-34.0F, -14.0F, -2.0F, 34.0F, 28.0F, 2.0F, NONE)
                , PartPose.offset(-0.0F, 0.0F, 0.0F));
        PartDefinition neck3 = neck2.addOrReplaceChild("neck3",
CubeListBuilder.create()
        .texOffs(437, 61).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F, NONE)
        .texOffs(0, 90).addBox(-1.5F, -13.0F, -3.0F, 3.0F, 4.0F, 6.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.1F, 0.0F, 0.0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3",
CubeListBuilder.create()
        .texOffs(437, 32).addBox(-4.0F, -3.5F, -0.5F, 8.0F, 6.0F, 22.0F, NONE)
        .texOffs(490, 99).addBox(-1.5F, -5.0F, 5.0F, 3.0F, 4.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 4.0F, 24.0F));
        PartDefinition wingClawL = wingArmL.addOrReplaceChild("wingClawL",
CubeListBuilder.create()
        .texOffs(116, 127).addBox(0.0F, -2.0F, -2.0F, 10.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.9F));
        PartDefinition wingClawR = wingArmR.addOrReplaceChild("wingClawR",
CubeListBuilder.create()
        .texOffs(116, 127).mirror().addBox(-10.0F, -2.0F, -2.0F, 10.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, -0.0F, -0.9F));
        PartDefinition wingForeL = wingArmL.addOrReplaceChild("wingForeL",
CubeListBuilder.create()
        .texOffs(262, 120).addBox(0.0F, -3.5F, -3.5F, 42.0F, 7.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition wingForeR = wingArmR.addOrReplaceChild("wingForeR",
CubeListBuilder.create()
        .texOffs(262, 120).mirror().addBox(-42.0F, -3.5F, -3.5F, 42.0F, 7.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(-32.0F, 0.0F, 0.0F, 0.0F, -0.0F, -0.0F));
        PartDefinition head = neck3.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(93, 0).addBox(-13.0F, -16.0F, -12.0F, 26.0F, 13.0F, 24.0F, NONE)
        .texOffs(203, 62).addBox(-8.0F, -11.0F, -25.0F, 16.0F, 7.0F, 13.0F, NONE)
        .texOffs(128, 87).addBox(-6.5F, -7.0F, -24.0F, 13.0F, 4.0F, 13.0F, NONE)
        .texOffs(85, 118).addBox(8.0F, -15.0F, -13.0F, 9.0F, 3.0F, 6.0F, NONE)
        .texOffs(145, 127).addBox(9.0F, -12.0F, -12.0F, 7.0F, 5.0F, 3.0F, NONE)
        .texOffs(498, 56).addBox(10.0F, -11.0F, -13.0F, 5.0F, 3.0F, 1.0F, NONE)
        .texOffs(262, 104).addBox(11.0F, -10.0F, -4.0F, 4.0F, 3.0F, 6.0F, NONE)
        .texOffs(66, 121).addBox(12.0F, -8.0F, 3.0F, 3.0F, 3.0F, 5.0F, NONE)
        .texOffs(457, 104).addBox(8.0F, -13.0F, -4.0F, 8.0F, 5.0F, 8.0F, NONE)
        .texOffs(85, 103).addBox(9.0F, -12.0F, 4.0F, 8.0F, 4.0F, 10.0F, NONE)
        .texOffs(422, 104).addBox(10.0F, -11.0F, 14.0F, 7.0F, 3.0F, 10.0F, NONE)
        .texOffs(181, 98).addBox(11.0F, -11.0F, 24.0F, 4.0F, 2.0F, 3.0F, NONE)
        .texOffs(500, 24).addBox(-8.0F, -6.0F, -37.0F, 2.0F, 5.0F, 2.0F, NONE)
        .texOffs(498, 32).addBox(-4.8F, -6.0F, -37.0F, 2.0F, 5.0F, 2.0F, NONE)
        .texOffs(355, 33).addBox(-1.6F, -6.0F, -37.0F, 2.0F, 5.0F, 2.0F, NONE)
        .texOffs(498, 40).addBox(1.6F, -6.0F, -37.0F, 2.0F, 5.0F, 2.0F, NONE)
        .texOffs(355, 41).addBox(4.8F, -6.0F, -37.0F, 2.0F, 5.0F, 2.0F, NONE)
        .texOffs(498, 48).addBox(8.0F, -6.0F, -37.0F, 2.0F, 5.0F, 2.0F, NONE)
        .texOffs(85, 118).mirror().addBox(-17.0F, -15.0F, -13.0F, 9.0F, 3.0F, 6.0F, NONE)
        .texOffs(145, 127).mirror().addBox(-16.0F, -12.0F, -12.0F, 7.0F, 5.0F, 3.0F, NONE)
        .texOffs(498, 56).mirror().addBox(-15.0F, -11.0F, -13.0F, 5.0F, 3.0F, 1.0F, NONE)
        .texOffs(262, 104).mirror().addBox(-15.0F, -10.0F, -4.0F, 4.0F, 3.0F, 6.0F, NONE)
        .texOffs(66, 121).mirror().addBox(-15.0F, -8.0F, 3.0F, 3.0F, 3.0F, 5.0F, NONE)
        .texOffs(457, 104).mirror().addBox(-16.0F, -13.0F, -4.0F, 8.0F, 5.0F, 8.0F, NONE)
        .texOffs(85, 103).mirror().addBox(-17.0F, -12.0F, 4.0F, 8.0F, 4.0F, 10.0F, NONE)
        .texOffs(422, 104).mirror().addBox(-17.0F, -11.0F, 14.0F, 7.0F, 3.0F, 10.0F, NONE)
        .texOffs(181, 98).mirror().addBox(-15.0F, -11.0F, 24.0F, 4.0F, 2.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition membraneForeL = wingForeL.addOrReplaceChild("membraneForeL",
CubeListBuilder.create()
        .texOffs(271, 0).addBox(0.0F, -15.0F, -2.0F, 44.0F, 30.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition membraneForeR = wingForeR.addOrReplaceChild("membraneForeR",
CubeListBuilder.create()
        .texOffs(271, 0).mirror().addBox(-44.0F, -15.0F, -2.0F, 44.0F, 30.0F, 2.0F, NONE)
                , PartPose.offset(-0.0F, 0.0F, 0.0F));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail4",
CubeListBuilder.create()
        .texOffs(0, 43).addBox(-3.5F, -2.5F, -0.5F, 7.0F, 5.0F, 20.0F, NONE)
                , PartPose.offset(0.0F, 3.0F, 22.0F));
        PartDefinition wingWristL = wingForeL.addOrReplaceChild("wingWristL",
CubeListBuilder.create()
        .texOffs(154, 38).addBox(-1.5F, -4.5F, -4.5F, 9.0F, 9.0F, 9.0F, NONE)
                , PartPose.offsetAndRotation(42.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition wingWristR = wingForeR.addOrReplaceChild("wingWristR",
CubeListBuilder.create()
        .texOffs(154, 38).mirror().addBox(-7.5F, -4.5F, -4.5F, 9.0F, 9.0F, 9.0F, NONE)
                , PartPose.offsetAndRotation(-42.0F, 0.0F, 0.0F, 0.0F, -0.0F, -0.0F));
        PartDefinition fingerL0 = wingWristL.addOrReplaceChild("fingerL0",
CubeListBuilder.create()
        .texOffs(122, 105).addBox(0.0F, -2.5F, -2.5F, 24.0F, 5.0F, 5.0F, NONE)
        .texOffs(0, 121).addBox(24.0F, -2.0F, -2.0F, 12.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F));
        PartDefinition fingerL1 = wingWristL.addOrReplaceChild("fingerL1",
CubeListBuilder.create()
        .texOffs(122, 116).addBox(0.0F, -2.5F, -2.5F, 24.0F, 5.0F, 5.0F, NONE)
        .texOffs(33, 121).addBox(24.0F, -2.0F, -2.0F, 12.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.9F));
        PartDefinition fingerL2 = wingWristL.addOrReplaceChild("fingerL2",
CubeListBuilder.create()
        .texOffs(422, 118).addBox(0.0F, -2.5F, -2.5F, 24.0F, 5.0F, 5.0F, NONE)
        .texOffs(181, 121).addBox(24.0F, -2.0F, -2.0F, 12.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5F));
        PartDefinition fingerL3 = wingWristL.addOrReplaceChild("fingerL3",
CubeListBuilder.create()
        .texOffs(361, 120).addBox(0.0F, -2.5F, -2.5F, 24.0F, 5.0F, 5.0F, NONE)
        .texOffs(214, 121).addBox(24.0F, -2.0F, -2.0F, 12.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.1F));
        PartDefinition fingerR0 = wingWristR.addOrReplaceChild("fingerR0",
CubeListBuilder.create()
        .texOffs(122, 105).mirror().addBox(-24.0F, -2.5F, -2.5F, 24.0F, 5.0F, 5.0F, NONE)
        .texOffs(0, 121).mirror().addBox(-36.0F, -2.0F, -2.0F, 12.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -0.5F));
        PartDefinition fingerR1 = wingWristR.addOrReplaceChild("fingerR1",
CubeListBuilder.create()
        .texOffs(122, 116).mirror().addBox(-24.0F, -2.5F, -2.5F, 24.0F, 5.0F, 5.0F, NONE)
        .texOffs(33, 121).mirror().addBox(-36.0F, -2.0F, -2.0F, 12.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -0.9F));
        PartDefinition fingerR2 = wingWristR.addOrReplaceChild("fingerR2",
CubeListBuilder.create()
        .texOffs(422, 118).mirror().addBox(-24.0F, -2.5F, -2.5F, 24.0F, 5.0F, 5.0F, NONE)
        .texOffs(181, 121).mirror().addBox(-36.0F, -2.0F, -2.0F, 12.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -1.5F));
        PartDefinition fingerR3 = wingWristR.addOrReplaceChild("fingerR3",
CubeListBuilder.create()
        .texOffs(361, 120).mirror().addBox(-24.0F, -2.5F, -2.5F, 24.0F, 5.0F, 5.0F, NONE)
        .texOffs(214, 121).mirror().addBox(-36.0F, -2.0F, -2.0F, 12.0F, 4.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -2.1F));
        PartDefinition lowerJaw = head.addOrReplaceChild("lowerJaw",
CubeListBuilder.create()
        .texOffs(373, 104).addBox(-6.5F, 0.0F, -2.0F, 13.0F, 4.0F, 11.0F, NONE)
        .texOffs(355, 49).addBox(-6.0F, -3.5F, -10.0F, 2.0F, 4.0F, 2.0F, NONE)
        .texOffs(322, 56).addBox(-3.0F, -3.5F, -10.0F, 2.0F, 4.0F, 2.0F, NONE)
        .texOffs(331, 56).addBox(0.0F, -3.5F, -10.0F, 2.0F, 4.0F, 2.0F, NONE)
        .texOffs(340, 56).addBox(3.0F, -3.5F, -10.0F, 2.0F, 4.0F, 2.0F, NONE)
        .texOffs(349, 56).addBox(6.0F, -3.5F, -10.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, -25.0F));
        PartDefinition membraneL0 = wingWristL.addOrReplaceChild("membraneL0",
CubeListBuilder.create()
        .texOffs(316, 84).addBox(0.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition membraneL1 = wingWristL.addOrReplaceChild("membraneL1",
CubeListBuilder.create()
        .texOffs(43, 85).addBox(0.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7F));
        PartDefinition membraneL2 = wingWristL.addOrReplaceChild("membraneL2",
CubeListBuilder.create()
        .texOffs(203, 86).addBox(0.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.2F));
        PartDefinition membraneL3 = wingWristL.addOrReplaceChild("membraneL3",
CubeListBuilder.create()
        .texOffs(401, 86).addBox(0.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.8F));
        PartDefinition membraneL4 = wingWristL.addOrReplaceChild("membraneL4",
CubeListBuilder.create()
        .texOffs(288, 102).addBox(0.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.2F));
        PartDefinition membraneL5 = wingWristL.addOrReplaceChild("membraneL5",
CubeListBuilder.create()
        .texOffs(0, 103).addBox(0.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.5F));
        PartDefinition membraneR0 = wingWristR.addOrReplaceChild("membraneR0",
CubeListBuilder.create()
        .texOffs(316, 84).mirror().addBox(-40.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -0.3F));
        PartDefinition membraneR1 = wingWristR.addOrReplaceChild("membraneR1",
CubeListBuilder.create()
        .texOffs(43, 85).mirror().addBox(-40.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -0.7F));
        PartDefinition membraneR2 = wingWristR.addOrReplaceChild("membraneR2",
CubeListBuilder.create()
        .texOffs(203, 86).mirror().addBox(-40.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -1.2F));
        PartDefinition membraneR3 = wingWristR.addOrReplaceChild("membraneR3",
CubeListBuilder.create()
        .texOffs(401, 86).mirror().addBox(-40.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -1.8F));
        PartDefinition membraneR4 = wingWristR.addOrReplaceChild("membraneR4",
CubeListBuilder.create()
        .texOffs(288, 102).mirror().addBox(-40.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -2.2F));
        PartDefinition membraneR5 = wingWristR.addOrReplaceChild("membraneR5",
CubeListBuilder.create()
        .texOffs(0, 103).mirror().addBox(-40.0F, -7.5F, -1.0F, 40.0F, 15.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, -0.0F, -2.5F));
        PartDefinition tail5 = tail4.addOrReplaceChild("tail5",
CubeListBuilder.create()
        .texOffs(0, 69).addBox(-2.5F, -2.0F, -0.5F, 5.0F, 4.0F, 16.0F, NONE)
                , PartPose.offset(0.0F, 3.0F, 20.0F));
        PartDefinition tailBlade = tail5.addOrReplaceChild("tailBlade",
CubeListBuilder.create()
        .texOffs(322, 33).addBox(-1.0F, -4.0F, 0.0F, 2.0F, 8.0F, 14.0F, NONE)
                , PartPose.offset(0.0F, 2.0F, 16.0F));
        return LayerDefinition.create(mesh, 512, 512);
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
    public void setupAnim(CultivationMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        final float DEG = 0.017453292F;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        float walk = limbSwingAmount;
        float t = ageInTicks;
        float breathe = Mth.sin(t * 0.026F);

        // ---- 呼吸：整条龙上下起伏（0.8px）+ 胸腹微鼓 ----
        this.torso.y = breathe * 0.8F;
        this.torso.xScale = 1.0F + 0.008F * breathe;
        this.torso.zScale = 1.0F + 0.008F * breathe;
        this.torso.xRot = 0.008F * Mth.sin(t * 0.019F);

        // ---- 长 S 颈：3 节相位错开的低频摆动（基础前倾角写回）----
        this.neck1.yRot = yaw * 0.12F;
        this.neck1.xRot = 0.18F + 0.020F * Mth.sin(t * 0.023F);
        this.neck2.yRot = yaw * 0.10F;
        this.neck2.xRot = 0.14F + 0.025F * Mth.sin(t * 0.021F + 0.6F);
        this.neck3.yRot = yaw * 0.09F;
        this.neck3.xRot = 0.08F + 0.020F * Mth.sin(t * 0.019F + 1.2F);

        // ---- 头随目标（netHeadYaw * 0.35）+ 略低头 + 低吼下颚开合 ----
        this.head.yRot = yaw * 0.35F;
        this.head.xRot = pitch * 0.30F;
        this.head.zRot = 0.02F * Mth.sin(t * 0.015F);
        this.lowerJaw.xRot = 0.05F + 0.05F * Mth.sin(t * 0.045F);

        // ---- 双巨翼：翼展呼吸（左右异相，基础张角写回）----
        this.wingL.zRot = 0.030F * Mth.sin(t * 0.027F);
        this.wingR.zRot = -0.030F * Mth.sin(t * 0.027F + 0.5F);
        this.wingArmL.zRot = -0.55F + 0.080F * Mth.sin(t * 0.030F);
        this.wingArmR.zRot = 0.55F - 0.080F * Mth.sin(t * 0.030F + 0.5F);
        this.wingForeL.zRot = 0.03F + 0.055F * Mth.sin(t * 0.034F + 0.8F);
        this.wingForeR.zRot = -0.03F - 0.055F * Mth.sin(t * 0.034F + 1.3F);
        this.wingWristL.zRot = 0.040F * Mth.sin(t * 0.038F + 0.4F);
        this.wingWristR.zRot = -0.040F * Mth.sin(t * 0.038F + 0.9F);
        // 指骨扇形微摆（基础角写回）
        this.fingerL0.zRot = 0.50F + 0.035F * Mth.sin(t * 0.041F);
        this.fingerL1.zRot = 0.94F + 0.035F * Mth.sin(t * 0.041F + 0.5F);
        this.fingerL2.zRot = 1.46F + 0.035F * Mth.sin(t * 0.041F + 1.0F);
        this.fingerL3.zRot = 2.07F + 0.035F * Mth.sin(t * 0.041F + 1.5F);
        this.fingerR0.zRot = -0.50F - 0.035F * Mth.sin(t * 0.041F + 2.0F);
        this.fingerR1.zRot = -0.94F - 0.035F * Mth.sin(t * 0.041F + 2.5F);
        this.fingerR2.zRot = -1.46F - 0.035F * Mth.sin(t * 0.041F + 3.0F);
        this.fingerR3.zRot = -2.07F - 0.035F * Mth.sin(t * 0.041F + 3.5F);

        // ---- 5 节尾左右摆（相位逐节滞后）----
        this.tail1.yRot = 0.050F * Mth.sin(t * 0.020F);
        this.tail2.yRot = 0.060F * Mth.sin(t * 0.020F - 0.5F);
        this.tail3.yRot = 0.070F * Mth.sin(t * 0.020F - 1.0F);
        this.tail4.yRot = 0.080F * Mth.sin(t * 0.020F - 1.5F);
        this.tail5.yRot = 0.090F * Mth.sin(t * 0.020F - 2.0F);
        this.tailBlade.zRot = 0.06F * Mth.sin(t * 0.016F);

        // ---- 数字行走：后腿 + 小前爪交替 ----
        float step = Mth.cos(limbSwing * 0.5F) * walk;
        this.legL.xRot = 0.14F * step;
        this.legR.xRot = -0.14F * step;
        this.shinL.xRot = -0.10F * step;
        this.shinR.xRot = 0.10F * step;
        this.footL.xRot = 0.06F * step;
        this.footR.xRot = -0.06F * step;
        this.armL.xRot = -0.10F * step;
        this.armR.xRot = 0.10F * step;

        // ---- 死亡：颈垂、翼垂、头低 ----
        if (entity.isDeadOrDying()) {
            this.torso.xRot = 0.10F;
            this.neck1.xRot = 0.45F;
            this.neck2.xRot = 0.35F;
            this.neck3.xRot = 0.25F;
            this.head.xRot = 0.40F;
            this.lowerJaw.xRot = 0.35F;
            this.wingArmL.zRot = -1.30F;
            this.wingArmR.zRot = 1.30F;
            this.wingForeL.zRot = -0.60F;
            this.wingForeR.zRot = 0.60F;
        }

    }


    /** 模型最低点像素 y（脚底基准；renderer 若加 GROUND_LIFT 用 (LOWEST_PIXEL_Y - 24) / 16）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

    /** 最高点（头骨顶 / 后掠角），模型总高 ≈112px = 7.0 格。 */
    public static final double HIGHEST_PIXEL_Y = -88.0;

}
