package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.HeavenLordEntity;
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
 * 仙庭之主 · Heaven Lord / HeavenLordModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>仙庭之主：月白仙袍 3 层袍摆（6.5 格宽）+ 鎏金宽肩甲/垂袖 + 冕旒冠 + 背后日轮 + 悬空玉圭/宝珠</p>
 */
public class HeavenLordModel extends EntityModel<HeavenLordEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "heaven_lord"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart halo;
    private final ModelPart head;
    private final ModelPart shoulderL;
    private final ModelPart shoulderR;
    private final ModelPart crown;
    private final ModelPart forearmL;
    private final ModelPart forearmR;
    private final ModelPart sleeveL;
    private final ModelPart sleeveR;
    private final ModelPart handL;
    private final ModelPart handR;
    private final ModelPart gui;
    private final ModelPart orb;

    public HeavenLordModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.chest = root.getChild("body").getChild("chest");
        this.armL = root.getChild("body").getChild("chest").getChild("armL");
        this.armR = root.getChild("body").getChild("chest").getChild("armR");
        this.halo = root.getChild("body").getChild("chest").getChild("halo");
        this.head = root.getChild("body").getChild("chest").getChild("head");
        this.shoulderL = root.getChild("body").getChild("chest").getChild("shoulderL");
        this.shoulderR = root.getChild("body").getChild("chest").getChild("shoulderR");
        this.crown = root.getChild("body").getChild("chest").getChild("head").getChild("crown");
        this.forearmL = root.getChild("body").getChild("chest").getChild("armL").getChild("forearmL");
        this.forearmR = root.getChild("body").getChild("chest").getChild("armR").getChild("forearmR");
        this.sleeveL = root.getChild("body").getChild("chest").getChild("shoulderL").getChild("sleeveL");
        this.sleeveR = root.getChild("body").getChild("chest").getChild("shoulderR").getChild("sleeveR");
        this.handL = root.getChild("body").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL");
        this.handR = root.getChild("body").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR");
        this.gui = root.getChild("body").getChild("chest").getChild("armR").getChild("forearmR").getChild("handR").getChild("gui");
        this.orb = root.getChild("body").getChild("chest").getChild("armL").getChild("forearmL").getChild("handL").getChild("orb");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(0, 177).addBox(-52.0F, -8.0F, -2.0F, 104.0F, 8.0F, 2.0F, NONE)
        .texOffs(0, 188).addBox(-52.0F, -8.0F, 0.0F, 104.0F, 8.0F, 2.0F, NONE)
        .texOffs(0, 210).addBox(-50.0F, -15.0F, -2.0F, 100.0F, 7.0F, 2.0F, NONE)
        .texOffs(0, 220).addBox(-50.0F, -15.0F, 0.0F, 100.0F, 7.0F, 2.0F, NONE)
        .texOffs(0, 230).addBox(-48.0F, -21.0F, -2.0F, 96.0F, 6.0F, 2.0F, NONE)
        .texOffs(0, 239).addBox(-48.0F, -21.0F, 0.0F, 96.0F, 6.0F, 2.0F, NONE)
        .texOffs(182, 77).addBox(-52.0F, -8.0F, -1.0F, 4.0F, 8.0F, 2.0F, NONE)
        .texOffs(0, 165).addBox(48.0F, -8.0F, -1.0F, 4.0F, 8.0F, 2.0F, NONE)
        .texOffs(0, 64).addBox(-20.0F, -38.0F, -5.0F, 40.0F, 18.0F, 10.0F, NONE)
        .texOffs(105, 199).addBox(-21.0F, -40.0F, -4.0F, 42.0F, 2.0F, 8.0F, NONE)
        .texOffs(0, 199).addBox(-22.0F, -24.0F, -4.0F, 44.0F, 2.0F, 8.0F, NONE)
        .texOffs(59, 56).addBox(-42.0F, -7.0F, -3.0F, 4.0F, 6.0F, 1.0F, NONE)
        .texOffs(70, 56).addBox(-28.0F, -7.0F, -3.0F, 4.0F, 6.0F, 1.0F, NONE)
        .texOffs(81, 56).addBox(-14.0F, -7.0F, -3.0F, 4.0F, 6.0F, 1.0F, NONE)
        .texOffs(206, 199).addBox(10.0F, -7.0F, -3.0F, 4.0F, 6.0F, 1.0F, NONE)
        .texOffs(217, 199).addBox(24.0F, -7.0F, -3.0F, 4.0F, 6.0F, 1.0F, NONE)
        .texOffs(228, 199).addBox(38.0F, -7.0F, -3.0F, 4.0F, 6.0F, 1.0F, NONE)
        .texOffs(74, 150).addBox(-34.0F, -15.0F, -3.0F, 4.0F, 7.0F, 1.0F, NONE)
        .texOffs(30, 165).addBox(-10.0F, -15.0F, -3.0F, 4.0F, 7.0F, 1.0F, NONE)
        .texOffs(41, 165).addBox(8.0F, -15.0F, -3.0F, 4.0F, 7.0F, 1.0F, NONE)
        .texOffs(52, 165).addBox(30.0F, -15.0F, -3.0F, 4.0F, 7.0F, 1.0F, NONE)
        .texOffs(0, 0).addBox(26.0F, -74.0F, 7.0F, 2.0F, 46.0F, 1.0F, NONE)
        .texOffs(7, 0).addBox(-28.0F, -74.0F, 7.0F, 2.0F, 46.0F, 1.0F, NONE)
        .texOffs(236, 163).addBox(-38.0F, -60.0F, 4.0F, 5.0F, 5.0F, 5.0F, NONE)
        .texOffs(13, 165).addBox(34.0F, -42.0F, 2.0F, 4.0F, 4.0F, 4.0F, NONE)
        .texOffs(239, 199).addBox(-32.0F, -28.0F, -4.0F, 3.0F, 3.0F, 3.0F, NONE)
        .texOffs(74, 159).addBox(-50.0F, -56.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(219, 228).addBox(-50.0F, -52.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(248, 234).addBox(-46.0F, -56.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(13, 248).addBox(-46.0F, -52.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(22, 248).addBox(-42.0F, -56.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(31, 248).addBox(-50.0F, -48.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(40, 248).addBox(-42.0F, -52.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(49, 248).addBox(-46.0F, -48.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(58, 248).addBox(-42.0F, -48.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(67, 248).addBox(-38.0F, -56.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(76, 248).addBox(-50.0F, -44.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(85, 248).addBox(-38.0F, -52.0F, 6.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition chest = body.addOrReplaceChild("chest",
CubeListBuilder.create()
        .texOffs(95, 33).addBox(-21.0F, -24.0F, -5.0F, 42.0F, 20.0F, 10.0F, NONE)
        .texOffs(0, 93).addBox(-23.0F, -22.0F, -7.0F, 46.0F, 18.0F, 2.0F, NONE)
        .texOffs(183, 130).addBox(-7.0F, -19.0F, -8.0F, 14.0F, 15.0F, 2.0F, NONE)
        .texOffs(0, 114).addBox(-23.0F, -22.0F, 5.0F, 46.0F, 18.0F, 2.0F, NONE)
        .texOffs(74, 164).addBox(-22.0F, -24.0F, -5.0F, 44.0F, 2.0F, 10.0F, NONE)
        .texOffs(97, 133).addBox(-9.0F, -30.0F, -4.0F, 18.0F, 6.0F, 8.0F, NONE)
        .texOffs(150, 130).addBox(-5.0F, -42.0F, -3.0F, 10.0F, 12.0F, 6.0F, NONE)
        .texOffs(94, 248).addBox(-16.0F, -22.0F, -8.0F, 3.0F, 3.0F, 1.0F, NONE)
        .texOffs(103, 248).addBox(13.0F, -22.0F, -8.0F, 3.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -42.0F, 0.0F));
        PartDefinition armL = chest.addOrReplaceChild("armL",
CubeListBuilder.create()
        .texOffs(222, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 8.0F, NONE)
                , PartPose.offset(28.0F, -20.0F, -2.0F));
        PartDefinition armR = chest.addOrReplaceChild("armR",
CubeListBuilder.create()
        .texOffs(59, 35).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 8.0F, NONE)
                , PartPose.offset(-28.0F, -20.0F, -2.0F));
        PartDefinition halo = chest.addOrReplaceChild("halo",
CubeListBuilder.create()
        .texOffs(200, 33).addBox(-13.0F, -13.0F, 0.0F, 26.0F, 26.0F, 2.0F, NONE)
        .texOffs(150, 114).addBox(-6.0F, -6.0F, -1.0F, 12.0F, 12.0F, 2.0F, NONE)
        .texOffs(239, 206).addBox(16.0F, -2.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(206, 207).addBox(14.0F, 7.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(219, 207).addBox(7.0F, 14.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(232, 213).addBox(-2.0F, 16.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(205, 214).addBox(-11.0F, 14.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(218, 214).addBox(-18.0F, 7.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(231, 220).addBox(-20.0F, -2.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(244, 220).addBox(-18.0F, -11.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(205, 221).addBox(-11.0F, -18.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(218, 221).addBox(-2.0F, -20.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(231, 227).addBox(7.0F, -18.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(244, 227).addBox(14.0F, -11.0F, 1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(0, 48).addBox(21.0F, -1.0F, 1.0F, 2.0F, 10.0F, 2.0F, NONE)
        .texOffs(245, 62).addBox(15.0F, 15.0F, 1.0F, 2.0F, 10.0F, 2.0F, NONE)
        .texOffs(182, 64).addBox(-1.0F, 21.0F, 1.0F, 2.0F, 10.0F, 2.0F, NONE)
        .texOffs(191, 64).addBox(-17.0F, 15.0F, 1.0F, 2.0F, 10.0F, 2.0F, NONE)
        .texOffs(245, 75).addBox(-23.0F, -1.0F, 1.0F, 2.0F, 10.0F, 2.0F, NONE)
        .texOffs(240, 102).addBox(-17.0F, -17.0F, 1.0F, 2.0F, 10.0F, 2.0F, NONE)
        .texOffs(240, 115).addBox(-1.0F, -23.0F, 1.0F, 2.0F, 10.0F, 2.0F, NONE)
        .texOffs(85, 135).addBox(15.0F, -17.0F, 1.0F, 2.0F, 10.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -34.0F, 8.0F));
        PartDefinition head = chest.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(14, 0).addBox(-14.0F, -22.0F, -6.0F, 28.0F, 22.0F, 12.0F, NONE)
        .texOffs(97, 114).addBox(-12.0F, -19.0F, -7.0F, 24.0F, 16.0F, 2.0F, NONE)
        .texOffs(197, 235).addBox(-11.0F, -22.0F, -6.0F, 22.0F, 2.0F, 3.0F, NONE)
        .texOffs(197, 241).addBox(-10.0F, -5.0F, -7.0F, 20.0F, 3.0F, 2.0F, NONE)
        .texOffs(242, 241).addBox(-7.0F, -15.0F, -8.0F, 5.0F, 4.0F, 1.0F, NONE)
        .texOffs(197, 247).addBox(2.0F, -15.0F, -8.0F, 5.0F, 4.0F, 1.0F, NONE)
        .texOffs(210, 252).addBox(-3.0F, -20.0F, -8.0F, 6.0F, 2.0F, 1.0F, NONE)
        .texOffs(63, 165).addBox(-12.0F, -2.0F, -5.0F, 3.0F, 6.0F, 2.0F, NONE)
        .texOffs(236, 174).addBox(9.0F, -2.0F, -5.0F, 3.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -42.0F, 0.0F));
        PartDefinition shoulderL = chest.addOrReplaceChild("shoulderL",
CubeListBuilder.create()
        .texOffs(101, 64).addBox(-2.0F, -12.0F, -5.0F, 30.0F, 14.0F, 10.0F, NONE)
        .texOffs(183, 148).addBox(0.0F, -17.0F, -4.0F, 26.0F, 6.0F, 8.0F, NONE)
        .texOffs(182, 110).addBox(22.0F, -15.0F, -4.0F, 6.0F, 11.0F, 8.0F, NONE)
        .texOffs(232, 89).addBox(2.0F, -23.0F, -3.0F, 5.0F, 7.0F, 5.0F, NONE)
        .texOffs(222, 21).addBox(14.0F, -21.0F, 3.0F, 4.0F, 7.0F, 4.0F, NONE)
                , PartPose.offset(24.0F, -30.0F, 0.0F));
        PartDefinition shoulderR = chest.addOrReplaceChild("shoulderR",
CubeListBuilder.create()
        .texOffs(101, 89).addBox(-28.0F, -12.0F, -5.0F, 30.0F, 14.0F, 10.0F, NONE)
        .texOffs(85, 149).addBox(-26.0F, -17.0F, -4.0F, 26.0F, 6.0F, 8.0F, NONE)
        .texOffs(211, 110).addBox(-4.0F, -15.0F, -4.0F, 6.0F, 11.0F, 8.0F, NONE)
        .texOffs(154, 149).addBox(-24.0F, -23.0F, -3.0F, 5.0F, 7.0F, 5.0F, NONE)
        .texOffs(239, 21).addBox(-12.0F, -21.0F, 3.0F, 4.0F, 7.0F, 4.0F, NONE)
                , PartPose.offset(-24.0F, -30.0F, 0.0F));
        PartDefinition crown = head.addOrReplaceChild("crown",
CubeListBuilder.create()
        .texOffs(0, 135).addBox(-16.0F, -4.0F, -6.0F, 32.0F, 4.0F, 10.0F, NONE)
        .texOffs(183, 163).addBox(-9.0F, 0.0F, -4.0F, 18.0F, 4.0F, 8.0F, NONE)
        .texOffs(0, 248).addBox(-2.0F, -2.0F, -7.0F, 4.0F, 2.0F, 2.0F, NONE)
        .texOffs(121, 252).addBox(-15.0F, 2.0F, -7.0F, 30.0F, 2.0F, 1.0F, NONE)
        .texOffs(121, 248).addBox(-17.0F, 2.0F, 5.0F, 34.0F, 2.0F, 1.0F, NONE)
        .texOffs(232, 102).addBox(-14.0F, 4.0F, -7.0F, 2.0F, 6.0F, 1.0F, NONE)
        .texOffs(249, 102).addBox(-10.0F, 4.0F, -7.0F, 2.0F, 6.0F, 1.0F, NONE)
        .texOffs(249, 110).addBox(-6.0F, 4.0F, -7.0F, 2.0F, 6.0F, 1.0F, NONE)
        .texOffs(249, 118).addBox(-2.0F, 4.0F, -7.0F, 2.0F, 6.0F, 1.0F, NONE)
        .texOffs(249, 126).addBox(2.0F, 4.0F, -7.0F, 2.0F, 6.0F, 1.0F, NONE)
        .texOffs(249, 134).addBox(6.0F, 4.0F, -7.0F, 2.0F, 6.0F, 1.0F, NONE)
        .texOffs(175, 149).addBox(10.0F, 4.0F, -7.0F, 2.0F, 6.0F, 1.0F, NONE)
        .texOffs(247, 174).addBox(14.0F, 4.0F, -7.0F, 2.0F, 6.0F, 1.0F, NONE)
        .texOffs(175, 157).addBox(-12.0F, 4.0F, 5.0F, 2.0F, 5.0F, 1.0F, NONE)
        .texOffs(245, 213).addBox(-6.0F, 4.0F, 5.0F, 2.0F, 5.0F, 1.0F, NONE)
        .texOffs(205, 228).addBox(0.0F, 4.0F, 5.0F, 2.0F, 5.0F, 1.0F, NONE)
        .texOffs(212, 228).addBox(6.0F, 4.0F, 5.0F, 2.0F, 5.0F, 1.0F, NONE)
        .texOffs(210, 247).addBox(-24.0F, 0.0F, -1.0F, 8.0F, 2.0F, 2.0F, NONE)
        .texOffs(231, 247).addBox(16.0F, 0.0F, -1.0F, 8.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -18.0F, 0.0F));
        PartDefinition forearmL = armL.addOrReplaceChild("forearmL",
CubeListBuilder.create()
        .texOffs(182, 89).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 12.0F, -1.0F));
        PartDefinition forearmR = armR.addOrReplaceChild("forearmR",
CubeListBuilder.create()
        .texOffs(207, 89).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 12.0F, -1.0F));
        PartDefinition sleeveL = shoulderL.addOrReplaceChild("sleeveL",
CubeListBuilder.create()
        .texOffs(95, 0).addBox(-9.0F, 0.0F, -5.0F, 18.0F, 22.0F, 10.0F, NONE)
        .texOffs(14, 35).addBox(-7.0F, 22.0F, -4.0F, 14.0F, 18.0F, 8.0F, NONE)
        .texOffs(213, 183).addBox(-8.0F, 40.0F, -2.0F, 16.0F, 3.0F, 4.0F, NONE)
                , PartPose.offset(9.0F, 4.0F, 0.0F));
        PartDefinition sleeveR = shoulderR.addOrReplaceChild("sleeveR",
CubeListBuilder.create()
        .texOffs(152, 0).addBox(-9.0F, 0.0F, -5.0F, 18.0F, 22.0F, 10.0F, NONE)
        .texOffs(200, 62).addBox(-7.0F, 22.0F, -4.0F, 14.0F, 18.0F, 8.0F, NONE)
        .texOffs(213, 191).addBox(-8.0F, 40.0F, -2.0F, 16.0F, 3.0F, 4.0F, NONE)
                , PartPose.offset(-9.0F, 4.0F, 0.0F));
        PartDefinition handL = forearmL.addOrReplaceChild("handL",
CubeListBuilder.create()
        .texOffs(0, 150).addBox(-5.0F, 0.0F, -4.0F, 10.0F, 6.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, 14.0F, -2.0F));
        PartDefinition handR = forearmR.addOrReplaceChild("handR",
CubeListBuilder.create()
        .texOffs(37, 150).addBox(-5.0F, 0.0F, -4.0F, 10.0F, 6.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, 14.0F, -2.0F));
        PartDefinition gui = handR.addOrReplaceChild("gui",
CubeListBuilder.create()
        .texOffs(209, 0).addBox(-2.0F, -28.0F, -1.0F, 4.0F, 28.0F, 2.0F, NONE)
        .texOffs(213, 176).addBox(-3.0F, -32.0F, -1.0F, 6.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, -8.0F));
        PartDefinition orb = handL.addOrReplaceChild("orb",
CubeListBuilder.create()
        .texOffs(216, 130).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE)
        .texOffs(112, 248).addBox(-6.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, -7.0F));
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
    public void setupAnim(HeavenLordEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float t = ageInTicks;
        final float DEG = 0.017453292F;
        // 悬浮呼吸（根部件绝对位移：基础 y=24）
        this.body.y = 24.0F + 0.8F * Mth.sin(t * 0.030F);
        this.body.xRot = 0.015F * Mth.sin(t * 0.021F);
        // 袍摆/大袖缓慢浮动
        this.chest.xRot = 0.010F * Mth.sin(t * 0.019F + 0.6F);
        this.sleeveL.xRot = 0.03F + 0.03F * Mth.sin(t * 0.028F);
        this.sleeveR.xRot = 0.03F - 0.03F * Mth.sin(t * 0.028F + 0.9F);
        // 巨颅随视线（巨物转头幅度克制）
        this.head.yRot = netHeadYaw * DEG * 0.30F;
        this.head.xRot = headPitch * DEG * 0.18F;
        // 双臂缓摆 + 玉圭/宝珠随动
        this.armL.xRot = 0.03F + 0.04F * Mth.sin(t * 0.026F);
        this.armR.xRot = 0.03F - 0.04F * Mth.sin(t * 0.026F + 1.1F);
        this.forearmL.xRot = -0.06F + 0.02F * Mth.sin(t * 0.031F + 0.4F);
        this.forearmR.xRot = -0.06F + 0.02F * Mth.sin(t * 0.031F + 1.5F);
        // 冕旒/日轮/宝珠
        this.crown.xRot = 0.02F * Mth.sin(t * 0.024F + 0.3F);
        this.halo.zRot = 0.06F * Mth.sin(t * 0.012F);
        float pulse = 1.0F + 0.06F * Mth.sin(t * 0.05F);
        this.orb.xScale = pulse;
        this.orb.yScale = pulse;
        this.orb.zScale = pulse;
        this.gui.y = 0.0F + 1.2F * Mth.sin(t * 0.035F + 0.8F);
        // 阶段 3：袍袖张开、日轮加速（读服务端阶段字段）
        int phase = com.hongmeng.xiuxian.entity.BossPhase.phaseOf(entity);
        if (phase >= 3) {
            this.sleeveL.xRot = 0.18F + 0.05F * Mth.sin(t * 0.05F);
            this.sleeveR.xRot = 0.18F - 0.05F * Mth.sin(t * 0.05F + 0.7F);
            this.halo.zRot = 0.12F * Mth.sin(t * 0.03F);
        }
        // 死亡：日轮熄灭、玉圭倾倒、袍塌
        if (entity.isDeadOrDying()) {
            this.body.xRot = 0.12F;
            this.body.xScale = 0.94F;
            this.body.zScale = 0.94F;
            this.head.xRot = 0.28F;
            this.crown.xRot = 0.42F;
            this.halo.zRot = 0.0F;
            this.halo.xScale = 0.30F;
            this.halo.zScale = 0.30F;
            this.gui.xRot = 0.9F;
            this.orb.xScale = 0.25F;
            this.orb.yScale = 0.25F;
            this.orb.zScale = 0.25F;
        }

    }

    /** 模型最低点像素 y（袍摆底 / 垂地飘带；脚底基准 24）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

    /** 冠顶明珠上缘（模型总高 110px = 6.875 格）。 */
    public static final int HIGHEST_PIXEL_Y = -110;

}
