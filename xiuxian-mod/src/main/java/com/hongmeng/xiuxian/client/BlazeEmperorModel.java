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
 * 焚天炎帝 · Blaze Emperor · Lava Titan / BlazeEmperorModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>灾变风熔岩泰坦：黑曜石重甲 + 胸口 28x28 发光熔炉 + 层叠巨肩甲/骨刺 + 双臂三节巨拳 + 牛角冠/火焰鬃毛/发光眼 + 背部排气管与熔岩喷口。</p>
 */
public class BlazeEmperorModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart root;
    private final ModelPart hips;
    private final ModelPart chainL;
    private final ModelPart thighL;
    private final ModelPart thighR;
    private final ModelPart torso;
    private final ModelPart waist;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart back;
    private final ModelPart furnace;
    private final ModelPart head;
    private final ModelPart pauldronL;
    private final ModelPart pauldronR;
    private final ModelPart shinL;
    private final ModelPart shinR;
    private final ModelPart ventL;
    private final ModelPart ventR;
    private final ModelPart crown;
    private final ModelPart foreL;
    private final ModelPart foreR;
    private final ModelPart hoofL;
    private final ModelPart hoofR;
    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart jaw;
    private final ModelPart mane;
    private final ModelPart pauldronL2;
    private final ModelPart pauldronR2;
    private final ModelPart fistL;
    private final ModelPart fistR;
    private final ModelPart fingersL;
    private final ModelPart fingersR;

    public BlazeEmperorModel(ModelPart root) {
        this.root = root;
        this.hips = root.getChild("hips");
        this.chainL = root.getChild("hips").getChild("chainL");
        this.thighL = root.getChild("hips").getChild("thighL");
        this.thighR = root.getChild("hips").getChild("thighR");
        this.torso = root.getChild("hips").getChild("torso");
        this.waist = root.getChild("hips").getChild("waist");
        this.armL = root.getChild("hips").getChild("torso").getChild("armL");
        this.armR = root.getChild("hips").getChild("torso").getChild("armR");
        this.back = root.getChild("hips").getChild("torso").getChild("back");
        this.furnace = root.getChild("hips").getChild("torso").getChild("furnace");
        this.head = root.getChild("hips").getChild("torso").getChild("head");
        this.pauldronL = root.getChild("hips").getChild("torso").getChild("pauldronL");
        this.pauldronR = root.getChild("hips").getChild("torso").getChild("pauldronR");
        this.shinL = root.getChild("hips").getChild("thighL").getChild("shinL");
        this.shinR = root.getChild("hips").getChild("thighR").getChild("shinR");
        this.ventL = root.getChild("hips").getChild("torso").getChild("ventL");
        this.ventR = root.getChild("hips").getChild("torso").getChild("ventR");
        this.crown = root.getChild("hips").getChild("torso").getChild("head").getChild("crown");
        this.foreL = root.getChild("hips").getChild("torso").getChild("armL").getChild("foreL");
        this.foreR = root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR");
        this.hoofL = root.getChild("hips").getChild("thighL").getChild("shinL").getChild("hoofL");
        this.hoofR = root.getChild("hips").getChild("thighR").getChild("shinR").getChild("hoofR");
        this.hornL = root.getChild("hips").getChild("torso").getChild("head").getChild("hornL");
        this.hornR = root.getChild("hips").getChild("torso").getChild("head").getChild("hornR");
        this.jaw = root.getChild("hips").getChild("torso").getChild("head").getChild("jaw");
        this.mane = root.getChild("hips").getChild("torso").getChild("head").getChild("mane");
        this.pauldronL2 = root.getChild("hips").getChild("torso").getChild("pauldronL").getChild("pauldronL2");
        this.pauldronR2 = root.getChild("hips").getChild("torso").getChild("pauldronR").getChild("pauldronR2");
        this.fistL = root.getChild("hips").getChild("torso").getChild("armL").getChild("foreL").getChild("fistL");
        this.fistR = root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR");
        this.fingersL = root.getChild("hips").getChild("torso").getChild("armL").getChild("foreL").getChild("fistL").getChild("fingersL");
        this.fingersR = root.getChild("hips").getChild("torso").getChild("armR").getChild("foreR").getChild("fistR").getChild("fingersR");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition hips = root.addOrReplaceChild("hips",
CubeListBuilder.create()
        .texOffs(0, 82).addBox(-14.0F, -6.0F, -7.0F, 28.0F, 6.0F, 12.0F, NONE)
        .texOffs(180, 135).addBox(-13.0F, -5.0F, -7.0F, 26.0F, 2.0F, 12.0F, NONE)
                , PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition chainL = hips.addOrReplaceChild("chainL",
CubeListBuilder.create()
        .texOffs(170, 152).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
        .texOffs(75, 159).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
        .texOffs(232, 190).addBox(-1.0F, 6.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
        .texOffs(221, 150).addBox(-4.0F, 9.0F, -4.0F, 8.0F, 8.0F, 6.0F, NONE)
        .texOffs(13, 172).addBox(-2.5F, 11.0F, -5.0F, 5.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(14.0F, -6.0F, -6.0F));
        PartDefinition thighL = hips.addOrReplaceChild("thighL",
CubeListBuilder.create()
        .texOffs(0, 101).addBox(-8.0F, 0.0F, -5.0F, 16.0F, 8.0F, 10.0F, NONE)
        .texOffs(41, 166).addBox(-8.0F, 4.0F, -6.0F, 16.0F, 3.0F, 8.0F, NONE)
                , PartPose.offset(12.0F, 0.0F, 0.0F));
        PartDefinition thighR = hips.addOrReplaceChild("thighR",
CubeListBuilder.create()
        .texOffs(53, 107).addBox(-8.0F, 0.0F, -5.0F, 16.0F, 8.0F, 10.0F, NONE)
        .texOffs(90, 166).addBox(-8.0F, 4.0F, -6.0F, 16.0F, 3.0F, 8.0F, NONE)
                , PartPose.offset(-12.0F, 0.0F, 0.0F));
        PartDefinition torso = hips.addOrReplaceChild("torso",
CubeListBuilder.create()
        .texOffs(0, 43).addBox(-20.0F, -28.0F, -6.0F, 40.0F, 28.0F, 10.0F, NONE)
        .texOffs(153, 165).addBox(-18.0F, -32.0F, -5.0F, 36.0F, 4.0F, 8.0F, NONE)
        .texOffs(0, 207).addBox(-20.0F, -28.0F, -7.0F, 40.0F, 2.0F, 1.0F, NONE)
        .texOffs(154, 209).addBox(-20.0F, -6.0F, -7.0F, 40.0F, 2.0F, 1.0F, NONE)
        .texOffs(248, 31).addBox(-22.0F, -26.0F, -5.0F, 2.0F, 18.0F, 2.0F, NONE)
        .texOffs(248, 52).addBox(20.0F, -26.0F, -5.0F, 2.0F, 18.0F, 2.0F, NONE)
        .texOffs(163, 43).addBox(-22.0F, -22.0F, -6.0F, 1.0F, 8.0F, 1.0F, NONE)
        .texOffs(224, 62).addBox(21.0F, -22.0F, -6.0F, 1.0F, 8.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -6.0F, 0.0F));
        PartDefinition waist = hips.addOrReplaceChild("waist",
CubeListBuilder.create()
        .texOffs(0, 144).addBox(-16.0F, -4.0F, -8.0F, 32.0F, 4.0F, 10.0F, NONE)
        .texOffs(111, 190).addBox(-12.0F, -8.0F, -9.0F, 24.0F, 4.0F, 4.0F, NONE)
        .texOffs(224, 72).addBox(-17.0F, -3.0F, -2.0F, 2.0F, 3.0F, 1.0F, NONE)
        .texOffs(94, 82).addBox(15.0F, -3.0F, -2.0F, 2.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -6.0F, 0.0F));
        PartDefinition armL = torso.addOrReplaceChild("armL",
CubeListBuilder.create()
        .texOffs(101, 43).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 15.0F, 8.0F, NONE)
        .texOffs(49, 178).addBox(-7.0F, 2.0F, -4.0F, 14.0F, 3.0F, 8.0F, NONE)
        .texOffs(163, 53).addBox(-1.0F, 5.0F, -6.0F, 2.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(23.0F, -22.0F, 0.0F));
        PartDefinition armR = torso.addOrReplaceChild("armR",
CubeListBuilder.create()
        .texOffs(142, 62).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 15.0F, 8.0F, NONE)
        .texOffs(94, 178).addBox(-7.0F, 2.0F, -4.0F, 14.0F, 3.0F, 8.0F, NONE)
        .texOffs(120, 126).addBox(-1.0F, 5.0F, -6.0F, 2.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(-23.0F, -22.0F, 0.0F));
        PartDefinition back = torso.addOrReplaceChild("back",
CubeListBuilder.create()
        .texOffs(183, 77).addBox(-10.0F, -14.0F, 0.0F, 20.0F, 14.0F, 6.0F, NONE)
        .texOffs(120, 67).addBox(-7.0F, -32.0F, 2.0F, 4.0F, 16.0F, 4.0F, NONE)
        .texOffs(236, 77).addBox(3.0F, -32.0F, 2.0F, 4.0F, 16.0F, 4.0F, NONE)
        .texOffs(41, 159).addBox(-7.0F, -34.0F, 2.0F, 4.0F, 2.0F, 4.0F, NONE)
        .texOffs(58, 159).addBox(3.0F, -34.0F, 2.0F, 4.0F, 2.0F, 4.0F, NONE)
        .texOffs(17, 190).addBox(-5.0F, -8.0F, 6.0F, 10.0F, 7.0F, 3.0F, NONE)
        .texOffs(127, 152).addBox(-11.0F, -10.0F, 5.0F, 3.0F, 10.0F, 3.0F, NONE)
        .texOffs(140, 152).addBox(8.0F, -10.0F, 5.0F, 3.0F, 10.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 6.0F));
        PartDefinition furnace = torso.addOrReplaceChild("furnace",
CubeListBuilder.create()
        .texOffs(170, 31).addBox(-14.0F, -13.0F, -2.0F, 28.0F, 28.0F, 2.0F, NONE)
        .texOffs(156, 204).addBox(-15.0F, -14.0F, -2.5F, 30.0F, 2.0F, 2.0F, NONE)
        .texOffs(89, 206).addBox(-15.0F, 12.0F, -2.5F, 30.0F, 2.0F, 2.0F, NONE)
        .texOffs(235, 0).addBox(-15.0F, -14.0F, -2.5F, 2.0F, 28.0F, 2.0F, NONE)
        .texOffs(244, 0).addBox(13.0F, -14.0F, -2.5F, 2.0F, 28.0F, 2.0F, NONE)
        .texOffs(0, 211).addBox(-14.0F, -5.0F, -3.0F, 28.0F, 2.0F, 1.0F, NONE)
        .texOffs(59, 211).addBox(-14.0F, 3.0F, -3.0F, 28.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -14.0F, -6.0F));
        PartDefinition head = torso.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(170, 0).addBox(-10.0F, -18.0F, -6.0F, 20.0F, 18.0F, 12.0F, NONE)
        .texOffs(0, 138).addBox(-11.0F, -16.0F, -8.0F, 22.0F, 3.0F, 2.0F, NONE)
        .texOffs(0, 159).addBox(-9.0F, -14.0F, -8.5F, 18.0F, 10.0F, 2.0F, NONE)
        .texOffs(26, 172).addBox(-8.0F, -12.0F, -9.0F, 5.0F, 3.0F, 1.0F, NONE)
        .texOffs(219, 197).addBox(3.0F, -12.0F, -9.0F, 5.0F, 3.0F, 1.0F, NONE)
        .texOffs(98, 126).addBox(-9.0F, -5.0F, -8.5F, 3.0F, 5.0F, 2.0F, NONE)
        .texOffs(109, 126).addBox(6.0F, -5.0F, -8.5F, 3.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -32.0F, 0.0F));
        PartDefinition pauldronL = torso.addOrReplaceChild("pauldronL",
CubeListBuilder.create()
        .texOffs(127, 117).addBox(-4.0F, 0.0F, -6.0F, 18.0F, 7.0F, 10.0F, NONE)
        .texOffs(111, 199).addBox(-4.0F, -1.0F, -5.0F, 18.0F, 2.0F, 4.0F, NONE)
                , PartPose.offset(22.0F, -26.0F, 0.0F));
        PartDefinition pauldronR = torso.addOrReplaceChild("pauldronR",
CubeListBuilder.create()
        .texOffs(184, 117).addBox(-14.0F, 0.0F, -6.0F, 18.0F, 7.0F, 10.0F, NONE)
        .texOffs(44, 200).addBox(-14.0F, -1.0F, -5.0F, 18.0F, 2.0F, 4.0F, NONE)
                , PartPose.offset(-22.0F, -26.0F, 0.0F));
        PartDefinition shinL = thighL.addOrReplaceChild("shinL",
CubeListBuilder.create()
        .texOffs(0, 120).addBox(-7.0F, 0.0F, -5.0F, 14.0F, 7.0F, 10.0F, NONE)
        .texOffs(221, 204).addBox(-7.0F, 2.0F, -5.5F, 14.0F, 2.0F, 1.0F, NONE)
        .texOffs(232, 197).addBox(-2.0F, 3.0F, -7.5F, 4.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 9.0F, 0.0F));
        PartDefinition shinR = thighR.addOrReplaceChild("shinR",
CubeListBuilder.create()
        .texOffs(49, 126).addBox(-7.0F, 0.0F, -5.0F, 14.0F, 7.0F, 10.0F, NONE)
        .texOffs(220, 213).addBox(-7.0F, 2.0F, -5.5F, 14.0F, 2.0F, 1.0F, NONE)
        .texOffs(156, 199).addBox(-2.0F, 3.0F, -7.5F, 4.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 9.0F, 0.0F));
        PartDefinition ventL = torso.addOrReplaceChild("ventL",
CubeListBuilder.create()
        .texOffs(231, 31).addBox(-3.0F, -10.0F, -2.0F, 6.0F, 20.0F, 2.0F, NONE)
        .texOffs(237, 208).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 1.0F, 1.0F, NONE)
        .texOffs(132, 217).addBox(-3.0F, 4.0F, -3.0F, 6.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(16.0F, -14.0F, -6.0F));
        PartDefinition ventR = torso.addOrReplaceChild("ventR",
CubeListBuilder.create()
        .texOffs(231, 54).addBox(-3.0F, -10.0F, -2.0F, 6.0F, 20.0F, 2.0F, NONE)
        .texOffs(147, 217).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 1.0F, 1.0F, NONE)
        .texOffs(162, 217).addBox(-3.0F, 4.0F, -3.0F, 6.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(-16.0F, -14.0F, -6.0F));
        PartDefinition crown = head.addOrReplaceChild("crown",
CubeListBuilder.create()
        .texOffs(101, 67).addBox(-2.5F, -16.0F, -1.5F, 5.0F, 16.0F, 4.0F, NONE)
        .texOffs(219, 190).addBox(-1.5F, -19.0F, -0.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -16.0F, 2.0F));
        PartDefinition foreL = armL.addOrReplaceChild("foreL",
CubeListBuilder.create()
        .texOffs(98, 135).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 8.0F, 8.0F, NONE)
        .texOffs(39, 215).addBox(-7.0F, 2.0F, -4.5F, 14.0F, 2.0F, 1.0F, NONE)
        .texOffs(243, 199).addBox(-2.0F, 3.0F, -6.5F, 4.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition foreR = armR.addOrReplaceChild("foreR",
CubeListBuilder.create()
        .texOffs(139, 135).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 8.0F, 8.0F, NONE)
        .texOffs(70, 215).addBox(-7.0F, 2.0F, -4.5F, 14.0F, 2.0F, 1.0F, NONE)
        .texOffs(89, 200).addBox(-2.0F, 3.0F, -6.5F, 4.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition hoofL = shinL.addOrReplaceChild("hoofL",
CubeListBuilder.create()
        .texOffs(81, 88).addBox(-9.0F, 0.0F, -7.0F, 18.0F, 4.0F, 14.0F, NONE)
        .texOffs(0, 202).addBox(-9.0F, 1.0F, -8.0F, 18.0F, 2.0F, 1.0F, NONE)
        .texOffs(81, 82).addBox(-9.0F, 0.0F, -9.5F, 4.0F, 3.0F, 2.0F, NONE)
        .texOffs(53, 101).addBox(5.0F, 0.0F, -9.5F, 4.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition hoofR = shinR.addOrReplaceChild("hoofR",
CubeListBuilder.create()
        .texOffs(146, 98).addBox(-9.0F, 0.0F, -7.0F, 18.0F, 4.0F, 14.0F, NONE)
        .texOffs(0, 215).addBox(-9.0F, 1.0F, -8.0F, 18.0F, 2.0F, 1.0F, NONE)
        .texOffs(66, 101).addBox(-9.0F, 0.0F, -9.5F, 4.0F, 3.0F, 2.0F, NONE)
        .texOffs(0, 172).addBox(5.0F, 0.0F, -9.5F, 4.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition hornL = head.addOrReplaceChild("hornL",
CubeListBuilder.create()
        .texOffs(85, 152).addBox(-2.0F, -8.0F, -2.0F, 5.0F, 8.0F, 5.0F, NONE)
        .texOffs(153, 152).addBox(-2.0F, -15.0F, -1.0F, 4.0F, 7.0F, 4.0F, NONE)
        .texOffs(244, 177).addBox(-1.5F, -22.0F, -0.5F, 3.0F, 7.0F, 3.0F, NONE)
        .texOffs(241, 126).addBox(2.0F, -13.0F, -1.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(7.0F, -16.0F, 0.0F));
        PartDefinition hornR = head.addOrReplaceChild("hornR",
CubeListBuilder.create()
        .texOffs(106, 152).addBox(-3.0F, -8.0F, -2.0F, 5.0F, 8.0F, 5.0F, NONE)
        .texOffs(0, 190).addBox(-2.0F, -15.0F, -1.0F, 4.0F, 7.0F, 4.0F, NONE)
        .texOffs(244, 188).addBox(-1.5F, -22.0F, -0.5F, 3.0F, 7.0F, 3.0F, NONE)
        .texOffs(85, 144).addBox(-4.0F, -13.0F, -1.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(-7.0F, -16.0F, 0.0F));
        PartDefinition jaw = head.addOrReplaceChild("jaw",
CubeListBuilder.create()
        .texOffs(0, 178).addBox(-8.0F, 0.0F, -3.0F, 16.0F, 3.0F, 8.0F, NONE)
        .texOffs(101, 217).addBox(-7.0F, 1.0F, -4.0F, 14.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, -6.0F));
        PartDefinition mane = head.addOrReplaceChild("mane",
CubeListBuilder.create()
        .texOffs(142, 43).addBox(-13.0F, -10.0F, 1.0F, 6.0F, 14.0F, 4.0F, NONE)
        .texOffs(106, 107).addBox(7.0F, -10.0F, 1.0F, 6.0F, 14.0F, 4.0F, NONE)
        .texOffs(211, 98).addBox(-6.0F, -16.0F, 2.0F, 12.0F, 14.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 6.0F));
        PartDefinition pauldronL2 = pauldronL.addOrReplaceChild("pauldronL2",
CubeListBuilder.create()
        .texOffs(183, 62).addBox(-2.0F, 0.0F, -5.0F, 10.0F, 4.0F, 10.0F, NONE)
        .texOffs(244, 98).addBox(2.0F, -10.0F, -3.0F, 3.0F, 10.0F, 3.0F, NONE)
        .texOffs(242, 165).addBox(-3.0F, -8.0F, 4.0F, 3.0F, 8.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition pauldronR2 = pauldronR.addOrReplaceChild("pauldronR2",
CubeListBuilder.create()
        .texOffs(180, 150).addBox(-8.0F, 0.0F, -5.0F, 10.0F, 4.0F, 10.0F, NONE)
        .texOffs(244, 112).addBox(-5.0F, -10.0F, -3.0F, 3.0F, 10.0F, 3.0F, NONE)
        .texOffs(139, 166).addBox(0.0F, -8.0F, 4.0F, 3.0F, 8.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition fistL = foreL.addOrReplaceChild("fistL",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-12.0F, 0.0F, -9.0F, 24.0F, 24.0F, 18.0F, NONE)
        .texOffs(118, 213).addBox(-12.0F, 17.0F, -9.5F, 24.0F, 2.0F, 1.0F, NONE)
        .texOffs(168, 190).addBox(-12.0F, 1.0F, -9.5F, 24.0F, 5.0F, 1.0F, NONE)
        .texOffs(59, 190).addBox(-10.0F, 3.0F, -10.0F, 5.0F, 8.0F, 1.0F, NONE)
        .texOffs(72, 190).addBox(5.0F, 3.0F, -10.0F, 5.0F, 8.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition fistR = foreR.addOrReplaceChild("fistR",
CubeListBuilder.create()
        .texOffs(85, 0).addBox(-12.0F, 0.0F, -9.0F, 24.0F, 24.0F, 18.0F, NONE)
        .texOffs(169, 213).addBox(-12.0F, 17.0F, -9.5F, 24.0F, 2.0F, 1.0F, NONE)
        .texOffs(168, 197).addBox(-12.0F, 1.0F, -9.5F, 24.0F, 5.0F, 1.0F, NONE)
        .texOffs(85, 190).addBox(-10.0F, 3.0F, -10.0F, 5.0F, 8.0F, 1.0F, NONE)
        .texOffs(98, 190).addBox(5.0F, 3.0F, -10.0F, 5.0F, 8.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition fingersL = fistL.addOrReplaceChild("fingersL",
CubeListBuilder.create()
        .texOffs(146, 86).addBox(-12.0F, 0.0F, -5.0F, 11.0F, 5.0F, 6.0F, NONE)
        .texOffs(139, 178).addBox(1.0F, 0.0F, -5.0F, 11.0F, 5.0F, 6.0F, NONE)
        .texOffs(127, 107).addBox(-12.0F, 1.0F, -3.0F, 2.0F, 4.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 13.0F, -6.0F));
        PartDefinition fingersR = fistR.addOrReplaceChild("fingersR",
CubeListBuilder.create()
        .texOffs(174, 178).addBox(-12.0F, 0.0F, -5.0F, 11.0F, 5.0F, 6.0F, NONE)
        .texOffs(209, 178).addBox(1.0F, 0.0F, -5.0F, 11.0F, 5.0F, 6.0F, NONE)
        .texOffs(44, 190).addBox(10.0F, 1.0F, -3.0F, 2.0F, 4.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 13.0F, -6.0F));
        return LayerDefinition.create(mesh, 256, 256);
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
        float t = ageInTicks;
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float atk = entity.getAttackAnim(0.0F);
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);
        int phase = Math.max(1, entity.bossPhase());          // 1..3
        float rage = phase >= 3 ? 1.0F : (phase == 2 ? 0.6F : 0.3F);

        // ================================================================
        // 1. 呼吸 / 站立基准：整身缓慢起伏，行走时另加步频
        // ================================================================
        float breathe = Mth.sin(t * 0.045F);
        float stepBob = walk * 1.2F * Mth.sin(limbSwing * 0.9F);
        this.hips.y = 4.0F + 0.8F * breathe + stepBob;
        this.hips.zRot = 0.02F * Mth.sin(t * 0.031F) - 0.04F * hurt;
        this.hips.xRot = 0.03F * walk * Mth.sin(limbSwing * 0.9F + 1.5708F);

        // ================================================================
        // 2. 躯干：熔炉呼吸（xScale/zScale 脉动）+ 受击后仰 + 死亡塌陷
        // ================================================================
        float breath = 1.0F + (0.020F + 0.012F * rage) * Mth.sin(t * 0.06F);
        this.torso.xScale = breath * (1.0F - 0.05F * hurt) * (1.0F + 0.06F * dead);
        this.torso.zScale = this.torso.xScale;
        this.torso.xRot = 0.02F * Mth.sin(t * 0.033F) + 0.10F * hurt + 0.35F * dead;
        this.torso.yRot = 0.02F * Mth.sin(t * 0.027F);
        this.torso.y = -6.0F + 0.5F * breathe;

        // ================================================================
        // 3. 胸口熔炉：核心呼吸脉动（阶段越高越亮/越大）
        // ================================================================
        float corePulse = 1.0F + (0.045F + 0.030F * rage) * Mth.sin(t * 0.09F)
                + 0.10F * hurt;
        float coreScale = corePulse * (1.0F - 0.55F * dead);
        this.furnace.xScale = coreScale;
        this.furnace.yScale = coreScale;
        this.furnace.zScale = coreScale;
        this.ventL.xScale = 1.0F + 0.05F * Mth.sin(t * 0.11F);
        this.ventR.xScale = 1.0F + 0.05F * Mth.sin(t * 0.11F + 1.5708F);

        // ================================================================
        // 4. 头部环视 + 下颌 + 鬃毛/冠：慢速环视，攻击时低头
        // ================================================================
        this.head.xRot = headPitch * 0.30F * DEG + 0.04F * Mth.sin(t * 0.021F)
                + 0.08F * hurt + 0.20F * dead + 0.10F * atk;
        this.head.yRot = netHeadYaw * 0.40F * DEG + 0.22F * Mth.sin(t * 0.017F);
        this.head.zRot = 0.03F * Mth.sin(t * 0.023F);
        this.jaw.xRot = 0.06F + 0.18F * Mth.sin(t * 0.05F) + 0.25F * atk + 0.10F * hurt;
        this.mane.xRot = 0.05F * Mth.sin(t * 0.04F) + 0.10F * dead;
        this.mane.zRot = 0.04F * Mth.sin(t * 0.033F);
        this.crown.y = -16.0F + 0.3F * Mth.sin(t * 0.05F);

        // ================================================================
        // 5. 双臂：待机微摆 → 攻击展开（zRot）+ 握拳（拳缩放 + 手指内扣）
        // ================================================================
        float swing = Mth.sin(atk * (float) Math.PI);          // 0→1→0
        float clench = Mth.sin(atk * (float) Math.PI * 1.6F);
        this.armL.zRot = 0.05F * Mth.sin(t * 0.04F) - (0.70F + 0.15F * rage) * swing;
        this.armR.zRot = 0.05F * Mth.sin(t * 0.04F + 1.5708F) + (0.70F + 0.15F * rage) * swing;
        this.armL.xRot = 0.04F * Mth.sin(t * 0.037F) + 0.18F * atk + 0.10F * hurt;
        this.armR.xRot = 0.04F * Mth.sin(t * 0.037F + 1.5708F) + 0.18F * atk + 0.10F * hurt;
        this.foreL.xRot = -0.05F * walk * Mth.sin(limbSwing * 0.9F) - 0.25F * atk;
        this.foreR.xRot = 0.05F * walk * Mth.sin(limbSwing * 0.9F) - 0.25F * atk;
        float clenchScale = 1.0F - 0.10F * clench - 0.20F * dead;
        this.fistL.xScale = clenchScale;
        this.fistL.zScale = clenchScale;
        this.fistR.xScale = clenchScale;
        this.fistR.zScale = clenchScale;
        this.fingersL.xRot = 0.90F * clench + 0.25F * dead;
        this.fingersR.xRot = 0.90F * clench + 0.25F * dead;

        // ================================================================
        // 6. 肩甲：轻微上下 + 攻击时后张（层叠甲板错相）
        // ================================================================
        this.pauldronL.y = -26.0F + 0.6F * Mth.sin(t * 0.05F) - 0.8F * swing;
        this.pauldronR.y = -26.0F + 0.6F * Mth.sin(t * 0.05F + 1.5708F) - 0.8F * swing;
        this.pauldronL.zRot = 0.03F * Mth.sin(t * 0.045F) - 0.10F * swing;
        this.pauldronR.zRot = 0.03F * Mth.sin(t * 0.045F + 1.5708F) + 0.10F * swing;
        this.pauldronL2.y = 7.0F + 0.4F * Mth.sin(t * 0.05F + 0.6F);
        this.pauldronR2.y = 7.0F + 0.4F * Mth.sin(t * 0.05F + 2.2F);
        this.pauldronL2.zRot = 0.04F * Mth.sin(t * 0.043F + 0.8F);
        this.pauldronR2.zRot = 0.04F * Mth.sin(t * 0.043F + 2.5F);

        // ================================================================
        // 7. 腿：粗腿慢步（幅度小，重量感）；死亡下蹲
        // ================================================================
        this.thighL.xRot = 0.10F * walk * Mth.sin(limbSwing * 0.9F) + 0.12F * dead;
        this.thighR.xRot = 0.10F * walk * Mth.sin(limbSwing * 0.9F + (float) Math.PI)
                + 0.12F * dead;
        this.shinL.xRot = -0.08F * walk * Mth.sin(limbSwing * 0.9F) - 0.10F * dead;
        this.shinR.xRot = -0.08F * walk * Mth.sin(limbSwing * 0.9F + (float) Math.PI)
                - 0.10F * dead;
        this.hoofL.xRot = 0.05F * walk * Mth.sin(limbSwing * 0.9F + 0.5F);
        this.hoofR.xRot = 0.05F * walk * Mth.sin(limbSwing * 0.9F + (float) Math.PI + 0.5F);

        // ================================================================
        // 8. 背部排气管/喷口 + 腰链：炉气脉动、链条摆动
        // ================================================================
        this.back.xRot = 0.02F * Mth.sin(t * 0.04F) + 0.05F * hurt;
        this.back.yScale = 1.0F + 0.02F * Mth.sin(t * 0.07F);
        this.chainL.zRot = 0.10F * Mth.sin(t * 0.05F)
                + 0.20F * walk * Mth.sin(limbSwing * 0.9F);
        this.chainL.xRot = 0.03F * Mth.sin(t * 0.043F);
        this.waist.xScale = 1.0F + 0.015F * Mth.sin(t * 0.06F);
        this.waist.zScale = this.waist.xScale;

    }


    /** 模型最低点像素 y（蹄足底面，正好踩地：24 → 世界 0；renderer GROUND_LIFT=0）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

    /** 最高点像素 y（牛角冠角尖）。模型总高 96px = 6.0 格。 */
    public static final double HIGHEST_PIXEL_Y = -72.0;

    /** 肩宽（像素）：肩甲外缘 ±36 → 72px = 4.5 格。 */
    public static final double SHOULDER_WIDTH_PX = 72.0;

}
