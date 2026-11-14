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
 * <b>二阶段：裂口中的巨颅 + 一条臂</b> —— 「恶」的三套阶段骨架之一（由 
 * {@code tools/gen_evil_phases.py} 从 parts 表生成：模型/贴图/动画同一张表）。
 *
 * <p><b>阶段入场压入接口</b>：renderer 用阶段字段做 {@code translate(0, ENTRANCE_DROP*(1-p), 0)} 从上方压入；
 * 本模型不改 root 位置，只把部件姿态按常态计算（绝对赋值，禁止 += / *=）。</p>
 */
public class EvilHeadModel extends EntityModel<CultivationMonsterEntity> {

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart cranium;
    private final ModelPart skullPlateL;
    private final ModelPart skullPlateR;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart maw;
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
    private final ModelPart eyeH8;
    private final ModelPart eyeH9;
    private final ModelPart fangU0;
    private final ModelPart fangU1;
    private final ModelPart fangU2;
    private final ModelPart fangU3;
    private final ModelPart fangL0;
    private final ModelPart fangL1;
    private final ModelPart fangL2;
    private final ModelPart fangL3;
    private final ModelPart hornL1;
    private final ModelPart hornL2;
    private final ModelPart hornR1;
    private final ModelPart hornR2;
    private final ModelPart tentH0;
    private final ModelPart tentH02;
    private final ModelPart tentH1;
    private final ModelPart tentH12;
    private final ModelPart tentH2;
    private final ModelPart tentH22;
    private final ModelPart arm2;
    private final ModelPart forearm2;
    private final ModelPart hand2;
    private final ModelPart claw2a;
    private final ModelPart claw2b;
    private final ModelPart claw2c;
    private final ModelPart eyeA2a;
    private final ModelPart eyeA2b;
    private final ModelPart eyeA2c;
    private final ModelPart eyeA2d;

    public EvilHeadModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.cranium = root.getChild("head").getChild("cranium");
        this.skullPlateL = root.getChild("head").getChild("cranium").getChild("skullPlateL");
        this.skullPlateR = root.getChild("head").getChild("cranium").getChild("skullPlateR");
        this.upperJaw = root.getChild("head").getChild("upperJaw");
        this.lowerJaw = root.getChild("head").getChild("upperJaw").getChild("lowerJaw");
        this.maw = root.getChild("head").getChild("upperJaw").getChild("maw");
        this.mainEye = root.getChild("head").getChild("mainEye");
        this.pupil = root.getChild("head").getChild("mainEye").getChild("pupil");
        this.eyeH0 = root.getChild("head").getChild("eyeH0");
        this.eyeH1 = root.getChild("head").getChild("eyeH1");
        this.eyeH2 = root.getChild("head").getChild("eyeH2");
        this.eyeH3 = root.getChild("head").getChild("eyeH3");
        this.eyeH4 = root.getChild("head").getChild("cranium").getChild("eyeH4");
        this.eyeH5 = root.getChild("head").getChild("cranium").getChild("eyeH5");
        this.eyeH6 = root.getChild("head").getChild("cranium").getChild("eyeH6");
        this.eyeH7 = root.getChild("head").getChild("eyeH7");
        this.eyeH8 = root.getChild("head").getChild("eyeH8");
        this.eyeH9 = root.getChild("head").getChild("eyeH9");
        this.fangU0 = root.getChild("head").getChild("upperJaw").getChild("fangU0");
        this.fangU1 = root.getChild("head").getChild("upperJaw").getChild("fangU1");
        this.fangU2 = root.getChild("head").getChild("upperJaw").getChild("fangU2");
        this.fangU3 = root.getChild("head").getChild("upperJaw").getChild("fangU3");
        this.fangL0 = root.getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL0");
        this.fangL1 = root.getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL1");
        this.fangL2 = root.getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL2");
        this.fangL3 = root.getChild("head").getChild("upperJaw").getChild("lowerJaw").getChild("fangL3");
        this.hornL1 = root.getChild("head").getChild("cranium").getChild("hornL1");
        this.hornL2 = root.getChild("head").getChild("cranium").getChild("hornL1").getChild("hornL2");
        this.hornR1 = root.getChild("head").getChild("cranium").getChild("hornR1");
        this.hornR2 = root.getChild("head").getChild("cranium").getChild("hornR1").getChild("hornR2");
        this.tentH0 = root.getChild("head").getChild("tentH0");
        this.tentH02 = root.getChild("head").getChild("tentH0").getChild("tentH02");
        this.tentH1 = root.getChild("head").getChild("tentH1");
        this.tentH12 = root.getChild("head").getChild("tentH1").getChild("tentH12");
        this.tentH2 = root.getChild("head").getChild("tentH2");
        this.tentH22 = root.getChild("head").getChild("tentH2").getChild("tentH22");
        this.arm2 = root.getChild("head").getChild("arm2");
        this.forearm2 = root.getChild("head").getChild("arm2").getChild("forearm2");
        this.hand2 = root.getChild("head").getChild("arm2").getChild("forearm2").getChild("hand2");
        this.claw2a = root.getChild("head").getChild("arm2").getChild("forearm2").getChild("hand2").getChild("claw2a");
        this.claw2b = root.getChild("head").getChild("arm2").getChild("forearm2").getChild("hand2").getChild("claw2b");
        this.claw2c = root.getChild("head").getChild("arm2").getChild("forearm2").getChild("hand2").getChild("claw2c");
        this.eyeA2a = root.getChild("head").getChild("arm2").getChild("eyeA2a");
        this.eyeA2b = root.getChild("head").getChild("arm2").getChild("eyeA2b");
        this.eyeA2c = root.getChild("head").getChild("arm2").getChild("forearm2").getChild("eyeA2c");
        this.eyeA2d = root.getChild("head").getChild("arm2").getChild("forearm2").getChild("eyeA2d");
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
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-34.0F, -56.0F, -30.0F, 68.0F, 56.0F, 60.0F, NONE),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.getChild("head").addOrReplaceChild("cranium", CubeListBuilder.create()
                .texOffs(0, 302).addBox(-28.0F, -24.0F, -22.0F, 56.0F, 24.0F, 44.0F, NONE),
                PartPose.offset(0.0F, -56.0F, 0.0F));
        root.getChild("head").getChild("cranium").addOrReplaceChild("skullPlateL", CubeListBuilder.create()
                .texOffs(782, 434).addBox(0.0F, -12.0F, -18.0F, 8.0F, 14.0F, 36.0F, NONE),
                PartPose.offset(28.0F, -12.0F, 0.0F));
        root.getChild("head").getChild("cranium").addOrReplaceChild("skullPlateR", CubeListBuilder.create()
                .texOffs(872, 434).addBox(-8.0F, -12.0F, -18.0F, 8.0F, 14.0F, 36.0F, NONE),
                PartPose.offset(-28.0F, -12.0F, 0.0F));
        root.getChild("head").addOrReplaceChild("upperJaw", CubeListBuilder.create()
                .texOffs(638, 214).addBox(-30.0F, 0.0F, -28.0F, 60.0F, 18.0F, 50.0F, NONE),
                PartPose.offset(0.0F, 0.0F, -8.0F));
        root.getChild("head").getChild("upperJaw").addOrReplaceChild("lowerJaw", CubeListBuilder.create()
                .texOffs(736, 302).addBox(-26.0F, 0.0F, -24.0F, 52.0F, 16.0F, 46.0F, NONE),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.getChild("head").getChild("upperJaw").addOrReplaceChild("maw", CubeListBuilder.create()
                .texOffs(608, 434).addBox(-24.0F, 0.0F, -18.0F, 48.0F, 14.0F, 38.0F, NONE),
                PartPose.offset(0.0F, 16.0F, -6.0F));
        root.getChild("head").addOrReplaceChild("mainEye", CubeListBuilder.create()
                .texOffs(876, 542).addBox(-16.0F, -16.0F, -3.0F, 32.0F, 32.0F, 3.0F, NONE),
                PartPose.offset(0.0F, -24.0F, -30.0F));
        root.getChild("head").getChild("mainEye").addOrReplaceChild("pupil", CubeListBuilder.create()
                .texOffs(296, 678).addBox(-7.0F, -7.0F, -2.0F, 14.0F, 14.0F, 2.0F, NONE),
                PartPose.offset(0.0F, 0.0F, -3.0F));
        root.getChild("head").addOrReplaceChild("eyeH0", CubeListBuilder.create()
                .texOffs(650, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-24.0F, -42.0F, -30.0F));
        root.getChild("head").addOrReplaceChild("eyeH1", CubeListBuilder.create()
                .texOffs(682, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-24.0F, -14.0F, -30.0F));
        root.getChild("head").addOrReplaceChild("eyeH2", CubeListBuilder.create()
                .texOffs(714, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(24.0F, -42.0F, -30.0F));
        root.getChild("head").addOrReplaceChild("eyeH3", CubeListBuilder.create()
                .texOffs(746, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(24.0F, -14.0F, -30.0F));
        root.getChild("head").getChild("cranium").addOrReplaceChild("eyeH4", CubeListBuilder.create()
                .texOffs(522, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(-16.0F, -12.0F, -22.0F));
        root.getChild("head").getChild("cranium").addOrReplaceChild("eyeH5", CubeListBuilder.create()
                .texOffs(550, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(16.0F, -12.0F, -22.0F));
        root.getChild("head").getChild("cranium").addOrReplaceChild("eyeH6", CubeListBuilder.create()
                .texOffs(578, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(0.0F, -14.0F, -22.0F));
        root.getChild("head").addOrReplaceChild("eyeH7", CubeListBuilder.create()
                .texOffs(758, 626).addBox(0.0F, -6.0F, -6.0F, 3.0F, 12.0F, 12.0F, NONE),
                PartPose.offset(30.0F, -20.0F, 6.0F));
        root.getChild("head").addOrReplaceChild("eyeH8", CubeListBuilder.create()
                .texOffs(790, 626).addBox(-3.0F, -6.0F, -6.0F, 3.0F, 12.0F, 12.0F, NONE),
                PartPose.offset(-30.0F, -20.0F, 6.0F));
        root.getChild("head").addOrReplaceChild("eyeH9", CubeListBuilder.create()
                .texOffs(778, 678).addBox(-6.0F, -6.0F, 0.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(0.0F, -30.0F, 30.0F));
        root.getChild("head").getChild("upperJaw").addOrReplaceChild("fangU0", CubeListBuilder.create()
                .texOffs(554, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 16.0F, 8.0F, NONE),
                PartPose.offset(-20.0F, 18.0F, -24.0F));
        root.getChild("head").getChild("upperJaw").addOrReplaceChild("fangU1", CubeListBuilder.create()
                .texOffs(34, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 20.0F, 8.0F, NONE),
                PartPose.offset(-7.0F, 18.0F, -26.0F));
        root.getChild("head").getChild("upperJaw").addOrReplaceChild("fangU2", CubeListBuilder.create()
                .texOffs(68, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 20.0F, 8.0F, NONE),
                PartPose.offset(7.0F, 18.0F, -26.0F));
        root.getChild("head").getChild("upperJaw").addOrReplaceChild("fangU3", CubeListBuilder.create()
                .texOffs(588, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 16.0F, 8.0F, NONE),
                PartPose.offset(20.0F, 18.0F, -24.0F));
        root.getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL0", CubeListBuilder.create()
                .texOffs(136, 656).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 12.0F, 8.0F, NONE),
                PartPose.offset(-18.0F, 16.0F, -22.0F));
        root.getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL1", CubeListBuilder.create()
                .texOffs(670, 656).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, NONE),
                PartPose.offset(-6.0F, 16.0F, -24.0F));
        root.getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL2", CubeListBuilder.create()
                .texOffs(704, 656).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, NONE),
                PartPose.offset(6.0F, 16.0F, -24.0F));
        root.getChild("head").getChild("upperJaw").getChild("lowerJaw").addOrReplaceChild("fangL3", CubeListBuilder.create()
                .texOffs(170, 656).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 12.0F, 8.0F, NONE),
                PartPose.offset(18.0F, 16.0F, -22.0F));
        root.getChild("head").getChild("cranium").addOrReplaceChild("hornL1", CubeListBuilder.create()
                .texOffs(470, 626).addBox(-5.0F, -14.0F, -5.0F, 10.0F, 14.0F, 10.0F, NONE),
                PartPose.offset(22.0F, -18.0F, 0.0F));
        root.getChild("head").getChild("cranium").getChild("hornL1").addOrReplaceChild("hornL2", CubeListBuilder.create()
                .texOffs(534, 678).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F, NONE),
                PartPose.offset(0.0F, -14.0F, 0.0F));
        root.getChild("head").getChild("cranium").addOrReplaceChild("hornR1", CubeListBuilder.create()
                .texOffs(512, 626).addBox(-5.0F, -14.0F, -5.0F, 10.0F, 14.0F, 10.0F, NONE),
                PartPose.offset(-22.0F, -18.0F, 0.0F));
        root.getChild("head").getChild("cranium").getChild("hornR1").addOrReplaceChild("hornR2", CubeListBuilder.create()
                .texOffs(560, 678).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F, NONE),
                PartPose.offset(0.0F, -14.0F, 0.0F));
        root.getChild("head").addOrReplaceChild("tentH0", CubeListBuilder.create()
                .texOffs(286, 542).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 30.0F, 10.0F, NONE),
                PartPose.offset(-22.0F, -20.0F, 30.0F));
        root.getChild("head").getChild("tentH0").addOrReplaceChild("tentH02", CubeListBuilder.create()
                .texOffs(458, 590).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 24.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        root.getChild("head").addOrReplaceChild("tentH1", CubeListBuilder.create()
                .texOffs(90, 542).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 34.0F, 10.0F, NONE),
                PartPose.offset(0.0F, -24.0F, 30.0F));
        root.getChild("head").getChild("tentH1").addOrReplaceChild("tentH12", CubeListBuilder.create()
                .texOffs(178, 590).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 26.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 34.0F, 0.0F));
        root.getChild("head").addOrReplaceChild("tentH2", CubeListBuilder.create()
                .texOffs(328, 542).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 30.0F, 10.0F, NONE),
                PartPose.offset(22.0F, -20.0F, 30.0F));
        root.getChild("head").getChild("tentH2").addOrReplaceChild("tentH22", CubeListBuilder.create()
                .texOffs(492, 590).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 24.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        root.getChild("head").addOrReplaceChild("arm2", CubeListBuilder.create()
                .texOffs(842, 118).addBox(-14.0F, 0.0F, -14.0F, 28.0F, 60.0F, 28.0F, NONE),
                PartPose.offset(-44.0F, -30.0F, 0.0F));
        root.getChild("head").getChild("arm2").addOrReplaceChild("forearm2", CubeListBuilder.create()
                .texOffs(178, 372).addBox(-12.0F, 0.0F, -12.0F, 24.0F, 36.0F, 24.0F, NONE),
                PartPose.offset(0.0F, 60.0F, 0.0F));
        root.getChild("head").getChild("arm2").getChild("forearm2").addOrReplaceChild("hand2", CubeListBuilder.create()
                .texOffs(806, 490).addBox(-16.0F, 0.0F, -16.0F, 32.0F, 14.0F, 32.0F, NONE),
                PartPose.offset(0.0F, 36.0F, 0.0F));
        root.getChild("head").getChild("arm2").getChild("forearm2").getChild("hand2").addOrReplaceChild("claw2a", CubeListBuilder.create()
                .texOffs(738, 656).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 8.0F, 10.0F, NONE),
                PartPose.offset(-10.0F, 14.0F, -14.0F));
        root.getChild("head").getChild("arm2").getChild("forearm2").getChild("hand2").addOrReplaceChild("claw2b", CubeListBuilder.create()
                .texOffs(636, 656).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 9.0F, 10.0F, NONE),
                PartPose.offset(0.0F, 14.0F, -16.0F));
        root.getChild("head").getChild("arm2").getChild("forearm2").getChild("hand2").addOrReplaceChild("claw2c", CubeListBuilder.create()
                .texOffs(772, 656).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 8.0F, 10.0F, NONE),
                PartPose.offset(10.0F, 14.0F, -14.0F));
        root.getChild("head").getChild("arm2").addOrReplaceChild("eyeA2a", CubeListBuilder.create()
                .texOffs(810, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 14.0F, -14.0F));
        root.getChild("head").getChild("arm2").addOrReplaceChild("eyeA2b", CubeListBuilder.create()
                .texOffs(842, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 40.0F, -14.0F));
        root.getChild("head").getChild("arm2").getChild("forearm2").addOrReplaceChild("eyeA2c", CubeListBuilder.create()
                .texOffs(606, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 12.0F, -12.0F));
        root.getChild("head").getChild("arm2").getChild("forearm2").addOrReplaceChild("eyeA2d", CubeListBuilder.create()
                .texOffs(634, 697).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 26.0F, -12.0F));
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
        // 巨颅随视线微转（裂口里的头）
        this.head.yRot = netHeadYaw * DEG * 0.30F;
        this.head.xRot = headPitch * DEG * 0.22F;
        this.cranium.xRot = 0.02F * Mth.sin(t * 0.021F);
        // 上下颚开合（低频，像在呼吸/低吼）
        this.upperJaw.xRot = 0.03F * Mth.sin(t * 0.023F);
        this.lowerJaw.xRot = 0.10F + 0.10F * Mth.sin(t * 0.023F + 0.8F);
        // 主眼缓慢收缩 + 瞳孔脉动
        float eyePulse = 1.0F + 0.05F * Mth.sin(t * 0.035F);
        this.mainEye.xScale = eyePulse;
        this.mainEye.zScale = eyePulse;
        this.pupil.yScale = 1.0F - 0.45F * Mth.clamp(Mth.sin(t * 0.017F) * 2.6F - 2.0F, 0.0F, 1.0F);
        // 小眼分件眨眼
        float blinkh0 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 0.6F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinkh1 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 2.7F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinkh2 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 4.8F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinkh3 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 6.9F) * 3.2F - 2.4F, 0.0F, 1.0F);
        this.eyeH0.yScale = blinkh0;
        this.eyeH0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.0F);
        this.eyeH1.yScale = blinkh1;
        this.eyeH1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.7F);
        this.eyeH2.yScale = blinkh2;
        this.eyeH2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 1.4F);
        this.eyeH3.yScale = blinkh3;
        this.eyeH3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.1F);
        this.eyeH4.yScale = blinkh0;
        this.eyeH4.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.8F);
        this.eyeH5.yScale = blinkh1;
        this.eyeH5.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 3.5F);
        this.eyeH6.yScale = blinkh2;
        this.eyeH6.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 4.2F);
        this.eyeH7.yScale = blinkh3;
        this.eyeH7.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 4.9F);
        this.eyeH8.yScale = blinkh0;
        this.eyeH8.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 5.6F);
        this.eyeH9.yScale = blinkh1;
        this.eyeH9.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 6.3F);
        // 二阶段那条臂慢摆 + 爪抓握
        this.arm2.xRot = 0.05F + 0.10F * Mth.sin(t * 0.043F + 1.4F);
        this.arm2.zRot = 0.06F * Mth.sin(t * 0.029F);
        this.forearm2.xRot = 0.05F * Mth.sin(t * 0.037F + 2.2F);
        this.hand2.xRot = 0.06F * Mth.sin(t * 0.051F);
        this.claw2a.xRot = 0.12F + 0.20F * Mth.sin(t * 0.07F + 0.0F);
        this.claw2b.xRot = 0.12F + 0.20F * Mth.sin(t * 0.07F + 0.9F);
        this.claw2c.xRot = 0.12F + 0.20F * Mth.sin(t * 0.07F + 1.8F);
        // 颅后裂口触须
        this.tentH0.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 0.0F);
        this.tentH0.zRot = 0.08F * Mth.sin(t * 0.04F + 1.0F);
        this.tentH02.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 0.9F);
        this.tentH02.zRot = -0.10F * Mth.sin(t * 0.05F + 1.7F);
        this.tentH1.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 1.0F);
        this.tentH1.zRot = 0.08F * Mth.sin(t * 0.04F + 2.0F);
        this.tentH12.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 1.9F);
        this.tentH12.zRot = -0.10F * Mth.sin(t * 0.05F + 2.7F);
        this.tentH2.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 2.0F);
        this.tentH2.zRot = 0.08F * Mth.sin(t * 0.04F + 3.0F);
        this.tentH22.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 2.9F);
        this.tentH22.zRot = -0.10F * Mth.sin(t * 0.05F + 3.7F);
        float blink20 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 1.2F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blink21 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 3.3F) * 3.2F - 2.4F, 0.0F, 1.0F);
        this.eyeA2a.yScale = blink20;
        this.eyeA2a.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.0F);
        this.eyeA2b.yScale = blink21;
        this.eyeA2b.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.7F);
        this.eyeA2c.yScale = blink20;
        this.eyeA2c.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 1.4F);
        this.eyeA2d.yScale = blink21;
        this.eyeA2d.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.1F);
    }
}