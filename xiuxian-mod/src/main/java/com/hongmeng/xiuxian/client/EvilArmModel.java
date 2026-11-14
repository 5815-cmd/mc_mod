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
 * <b>一阶段：从云里垂下的巨臂</b> —— 「恶」的三套阶段骨架之一（由 
 * {@code tools/gen_evil_phases.py} 从 parts 表生成：模型/贴图/动画同一张表）。
 *
 * <p><b>阶段入场压入接口</b>：renderer 用阶段字段做 {@code translate(0, ENTRANCE_DROP*(1-p), 0)} 从上方压入；
 * 本模型不改 root 位置，只把部件姿态按常态计算（绝对赋值，禁止 += / *=）。</p>
 */
public class EvilArmModel extends EntityModel<CultivationMonsterEntity> {

    private final ModelPart root;
    private final ModelPart shoulder;
    private final ModelPart pauldronL;
    private final ModelPart pauldronR;
    private final ModelPart arm;
    private final ModelPart elbow;
    private final ModelPart forearm;
    private final ModelPart hand;
    private final ModelPart finger0;
    private final ModelPart claw0;
    private final ModelPart finger1;
    private final ModelPart claw1;
    private final ModelPart finger2;
    private final ModelPart claw2;
    private final ModelPart finger3;
    private final ModelPart claw3;
    private final ModelPart finger4;
    private final ModelPart claw4;
    private final ModelPart eyeA0;
    private final ModelPart eyeA1;
    private final ModelPart eyeA2;
    private final ModelPart eyeA3;
    private final ModelPart eyeA4;
    private final ModelPart eyeA5;
    private final ModelPart eyeE0;
    private final ModelPart eyeE1;
    private final ModelPart eyeF0;
    private final ModelPart eyeF1;
    private final ModelPart eyeF2;
    private final ModelPart eyeF3;
    private final ModelPart spike0;
    private final ModelPart spike1;
    private final ModelPart spike2;
    private final ModelPart spike3;
    private final ModelPart tent0;
    private final ModelPart tent02;
    private final ModelPart tent1;
    private final ModelPart tent12;
    private final ModelPart tent2;
    private final ModelPart tent22;

    public EvilArmModel(ModelPart root) {
        this.root = root;
        this.shoulder = root.getChild("shoulder");
        this.pauldronL = root.getChild("shoulder").getChild("pauldronL");
        this.pauldronR = root.getChild("shoulder").getChild("pauldronR");
        this.arm = root.getChild("shoulder").getChild("arm");
        this.elbow = root.getChild("shoulder").getChild("arm").getChild("elbow");
        this.forearm = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm");
        this.hand = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand");
        this.finger0 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger0");
        this.claw0 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger0").getChild("claw0");
        this.finger1 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger1");
        this.claw1 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger1").getChild("claw1");
        this.finger2 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger2");
        this.claw2 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger2").getChild("claw2");
        this.finger3 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger3");
        this.claw3 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger3").getChild("claw3");
        this.finger4 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger4");
        this.claw4 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger4").getChild("claw4");
        this.eyeA0 = root.getChild("shoulder").getChild("arm").getChild("eyeA0");
        this.eyeA1 = root.getChild("shoulder").getChild("arm").getChild("eyeA1");
        this.eyeA2 = root.getChild("shoulder").getChild("arm").getChild("eyeA2");
        this.eyeA3 = root.getChild("shoulder").getChild("arm").getChild("eyeA3");
        this.eyeA4 = root.getChild("shoulder").getChild("arm").getChild("eyeA4");
        this.eyeA5 = root.getChild("shoulder").getChild("arm").getChild("eyeA5");
        this.eyeE0 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("eyeE0");
        this.eyeE1 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("eyeE1");
        this.eyeF0 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("eyeF0");
        this.eyeF1 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("eyeF1");
        this.eyeF2 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("eyeF2");
        this.eyeF3 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("eyeF3");
        this.spike0 = root.getChild("shoulder").getChild("arm").getChild("spike0");
        this.spike1 = root.getChild("shoulder").getChild("arm").getChild("spike1");
        this.spike2 = root.getChild("shoulder").getChild("arm").getChild("spike2");
        this.spike3 = root.getChild("shoulder").getChild("arm").getChild("spike3");
        this.tent0 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent0");
        this.tent02 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent0").getChild("tent02");
        this.tent1 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent1");
        this.tent12 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent1").getChild("tent12");
        this.tent2 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent2");
        this.tent22 = root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent2").getChild("tent22");
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
        root.addOrReplaceChild("shoulder", CubeListBuilder.create()
                .texOffs(428, 214).addBox(-26.0F, -14.0F, -26.0F, 52.0F, 22.0F, 52.0F, NONE),
                PartPose.offset(0.0F, -40.0F, 0.0F));
        root.getChild("shoulder").addOrReplaceChild("pauldronL", CubeListBuilder.create()
                .texOffs(0, 490).addBox(0.0F, -12.0F, -14.0F, 14.0F, 22.0F, 28.0F, NONE),
                PartPose.offset(22.0F, -2.0F, 0.0F));
        root.getChild("shoulder").addOrReplaceChild("pauldronR", CubeListBuilder.create()
                .texOffs(86, 490).addBox(-14.0F, -12.0F, -14.0F, 14.0F, 22.0F, 28.0F, NONE),
                PartPose.offset(-22.0F, -2.0F, 0.0F));
        root.getChild("shoulder").addOrReplaceChild("arm", CubeListBuilder.create()
                .texOffs(258, 0).addBox(-18.0F, 0.0F, -18.0F, 36.0F, 70.0F, 36.0F, NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("elbow", CubeListBuilder.create()
                .texOffs(0, 372).addBox(-22.0F, -8.0F, -22.0F, 44.0F, 16.0F, 44.0F, NONE),
                PartPose.offset(0.0F, 70.0F, 0.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").addOrReplaceChild("forearm", CubeListBuilder.create()
                .texOffs(404, 0).addBox(-19.0F, 0.0F, -19.0F, 38.0F, 64.0F, 38.0F, NONE),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").addOrReplaceChild("hand", CubeListBuilder.create()
                .texOffs(234, 214).addBox(-24.0F, 0.0F, -24.0F, 48.0F, 34.0F, 48.0F, NONE),
                PartPose.offset(0.0F, 64.0F, 0.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").addOrReplaceChild("finger0", CubeListBuilder.create()
                .texOffs(982, 626).addBox(-3.0F, 0.0F, -8.0F, 6.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(-18.0F, 30.0F, -24.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger0").addOrReplaceChild("claw0", CubeListBuilder.create()
                .texOffs(402, 697).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 7.0F, 7.0F, NONE),
                PartPose.offset(0.0F, 10.0F, -6.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").addOrReplaceChild("finger1", CubeListBuilder.create()
                .texOffs(0, 656).addBox(-3.0F, 0.0F, -8.0F, 6.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(-9.0F, 30.0F, -24.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger1").addOrReplaceChild("claw1", CubeListBuilder.create()
                .texOffs(426, 697).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 7.0F, 7.0F, NONE),
                PartPose.offset(0.0F, 10.0F, -6.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").addOrReplaceChild("finger2", CubeListBuilder.create()
                .texOffs(34, 656).addBox(-3.0F, 0.0F, -8.0F, 6.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(0.0F, 30.0F, -24.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger2").addOrReplaceChild("claw2", CubeListBuilder.create()
                .texOffs(450, 697).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 7.0F, 7.0F, NONE),
                PartPose.offset(0.0F, 10.0F, -6.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").addOrReplaceChild("finger3", CubeListBuilder.create()
                .texOffs(68, 656).addBox(-3.0F, 0.0F, -8.0F, 6.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(9.0F, 30.0F, -24.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger3").addOrReplaceChild("claw3", CubeListBuilder.create()
                .texOffs(474, 697).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 7.0F, 7.0F, NONE),
                PartPose.offset(0.0F, 10.0F, -6.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").addOrReplaceChild("finger4", CubeListBuilder.create()
                .texOffs(102, 656).addBox(-3.0F, 0.0F, -8.0F, 6.0F, 10.0F, 10.0F, NONE),
                PartPose.offset(18.0F, 30.0F, -24.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("hand").getChild("finger4").addOrReplaceChild("claw4", CubeListBuilder.create()
                .texOffs(498, 697).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 7.0F, 7.0F, NONE),
                PartPose.offset(0.0F, 10.0F, -6.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("eyeA0", CubeListBuilder.create()
                .texOffs(396, 656).addBox(-8.0F, -8.0F, -3.0F, 16.0F, 16.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 8.0F, -18.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("eyeA1", CubeListBuilder.create()
                .texOffs(436, 656).addBox(-8.0F, -8.0F, -3.0F, 16.0F, 16.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 18.0F, -18.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("eyeA2", CubeListBuilder.create()
                .texOffs(476, 656).addBox(-8.0F, -8.0F, -3.0F, 16.0F, 16.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 28.0F, -18.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("eyeA3", CubeListBuilder.create()
                .texOffs(516, 656).addBox(-8.0F, -8.0F, -3.0F, 16.0F, 16.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 38.0F, -18.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("eyeA4", CubeListBuilder.create()
                .texOffs(556, 656).addBox(-8.0F, -8.0F, -3.0F, 16.0F, 16.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 48.0F, -18.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("eyeA5", CubeListBuilder.create()
                .texOffs(596, 656).addBox(-8.0F, -8.0F, -3.0F, 16.0F, 16.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 58.0F, -18.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").addOrReplaceChild("eyeE0", CubeListBuilder.create()
                .texOffs(586, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(-14.0F, 0.0F, -22.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").addOrReplaceChild("eyeE1", CubeListBuilder.create()
                .texOffs(618, 678).addBox(-6.0F, -6.0F, -3.0F, 12.0F, 12.0F, 3.0F, NONE),
                PartPose.offset(14.0F, 0.0F, -22.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").addOrReplaceChild("eyeF0", CubeListBuilder.create()
                .texOffs(926, 656).addBox(-7.0F, -7.0F, -3.0F, 14.0F, 14.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 10.0F, -19.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").addOrReplaceChild("eyeF1", CubeListBuilder.create()
                .texOffs(962, 656).addBox(-7.0F, -7.0F, -3.0F, 14.0F, 14.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 24.0F, -19.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").addOrReplaceChild("eyeF2", CubeListBuilder.create()
                .texOffs(0, 678).addBox(-7.0F, -7.0F, -3.0F, 14.0F, 14.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 38.0F, -19.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").addOrReplaceChild("eyeF3", CubeListBuilder.create()
                .texOffs(36, 678).addBox(-7.0F, -7.0F, -3.0F, 14.0F, 14.0F, 3.0F, NONE),
                PartPose.offset(0.0F, 52.0F, -19.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("spike0", CubeListBuilder.create()
                .texOffs(254, 626).addBox(0.0F, -6.0F, -6.0F, 14.0F, 12.0F, 12.0F, NONE),
                PartPose.offset(18.0F, 18.0F, 4.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("spike1", CubeListBuilder.create()
                .texOffs(308, 626).addBox(-14.0F, -6.0F, -6.0F, 14.0F, 12.0F, 12.0F, NONE),
                PartPose.offset(-18.0F, 18.0F, 4.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("spike2", CubeListBuilder.create()
                .texOffs(362, 626).addBox(0.0F, -6.0F, -6.0F, 14.0F, 12.0F, 12.0F, NONE),
                PartPose.offset(18.0F, 46.0F, 4.0F));
        root.getChild("shoulder").getChild("arm").addOrReplaceChild("spike3", CubeListBuilder.create()
                .texOffs(416, 626).addBox(-14.0F, -6.0F, -6.0F, 14.0F, 12.0F, 12.0F, NONE),
                PartPose.offset(-18.0F, 46.0F, 4.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").addOrReplaceChild("tent0", CubeListBuilder.create()
                .texOffs(332, 590).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 22.0F, 10.0F, NONE),
                PartPose.offset(-22.0F, 38.0F, 20.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent0").addOrReplaceChild("tent02", CubeListBuilder.create()
                .texOffs(930, 590).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 20.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").addOrReplaceChild("tent1", CubeListBuilder.create()
                .texOffs(374, 590).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 22.0F, 10.0F, NONE),
                PartPose.offset(0.0F, 38.0F, 20.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent1").addOrReplaceChild("tent12", CubeListBuilder.create()
                .texOffs(964, 590).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 20.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").addOrReplaceChild("tent2", CubeListBuilder.create()
                .texOffs(416, 590).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 22.0F, 10.0F, NONE),
                PartPose.offset(22.0F, 38.0F, 20.0F));
        root.getChild("shoulder").getChild("arm").getChild("elbow").getChild("forearm").getChild("tent2").addOrReplaceChild("tent22", CubeListBuilder.create()
                .texOffs(0, 626).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 20.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 22.0F, 0.0F));
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
        // 巨臂慢摆（整条臂从肩甲处垂下，像被天空拽着）
        this.arm.xRot = 0.06F + 0.08F * Mth.sin(t * 0.045F);
        this.arm.yRot = 0.12F * Mth.sin(t * 0.031F);
        this.arm.zRot = 0.05F * Mth.sin(t * 0.027F + 0.6F);
        this.shoulder.xRot = 0.02F * Mth.sin(t * 0.023F);
        this.elbow.xRot = 0.03F * Mth.sin(t * 0.037F + 1.0F);
        this.forearm.xRot = 0.05F + 0.04F * Mth.sin(t * 0.041F + 2.0F);
        this.hand.xRot = 0.05F * Mth.sin(t * 0.052F + 0.4F);
        // 五指抓握（张开-收拢循环）
        this.finger0.xRot = 0.12F + 0.18F * Mth.sin(t * 0.07F + 0.0F);
        this.finger1.xRot = 0.12F + 0.18F * Mth.sin(t * 0.07F + 0.9F);
        this.finger2.xRot = 0.12F + 0.18F * Mth.sin(t * 0.07F + 1.8F);
        this.finger3.xRot = 0.12F + 0.18F * Mth.sin(t * 0.07F + 2.7F);
        this.finger4.xRot = 0.12F + 0.18F * Mth.sin(t * 0.07F + 3.6F);
        // 爪尖（比指节慢半拍，收拢时先勾）
        this.claw0.xRot = 0.16F + 0.22F * Mth.sin(t * 0.07F + 0.0F);
        this.claw1.xRot = 0.16F + 0.22F * Mth.sin(t * 0.07F + 0.9F);
        this.claw2.xRot = 0.16F + 0.22F * Mth.sin(t * 0.07F + 1.8F);
        this.claw3.xRot = 0.16F + 0.22F * Mth.sin(t * 0.07F + 2.7F);
        this.claw4.xRot = 0.16F + 0.22F * Mth.sin(t * 0.07F + 3.6F);
        // 触须多相位波动
        this.tent0.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 0.0F);
        this.tent0.zRot = 0.08F * Mth.sin(t * 0.04F + 1.0F);
        this.tent02.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 0.9F);
        this.tent02.zRot = -0.10F * Mth.sin(t * 0.05F + 1.7F);
        this.tent1.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 1.0F);
        this.tent1.zRot = 0.08F * Mth.sin(t * 0.04F + 2.0F);
        this.tent12.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 1.9F);
        this.tent12.zRot = -0.10F * Mth.sin(t * 0.05F + 2.7F);
        this.tent2.xRot = 0.14F + 0.10F * Mth.sin(t * 0.05F + 2.0F);
        this.tent2.zRot = 0.08F * Mth.sin(t * 0.04F + 3.0F);
        this.tent22.xRot = -0.10F + 0.14F * Mth.sin(t * 0.05F + 2.9F);
        this.tent22.zRot = -0.10F * Mth.sin(t * 0.05F + 3.7F);
        // 臂上眼睛：分件眨眼 + 脉动（瞳孔高光在贴图上）
        float blinkg0 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 0.2F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinkg1 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 2.3F) * 3.2F - 2.4F, 0.0F, 1.0F);
        float blinkg2 = 1.0F - 0.55F * Mth.clamp(Mth.sin(t * 0.045F + 4.4F) * 3.2F - 2.4F, 0.0F, 1.0F);
        this.eyeA0.yScale = blinkg0;
        this.eyeA0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.0F);
        this.eyeA1.yScale = blinkg1;
        this.eyeA1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 0.7F);
        this.eyeA2.yScale = blinkg2;
        this.eyeA2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 1.4F);
        this.eyeA3.yScale = blinkg0;
        this.eyeA3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.1F);
        this.eyeA4.yScale = blinkg1;
        this.eyeA4.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 2.8F);
        this.eyeA5.yScale = blinkg2;
        this.eyeA5.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 3.5F);
        this.eyeE0.yScale = blinkg0;
        this.eyeE0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 4.2F);
        this.eyeE1.yScale = blinkg1;
        this.eyeE1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 4.9F);
        this.eyeF0.yScale = blinkg2;
        this.eyeF0.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 5.6F);
        this.eyeF1.yScale = blinkg0;
        this.eyeF1.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 6.3F);
        this.eyeF2.yScale = blinkg1;
        this.eyeF2.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 7.0F);
        this.eyeF3.yScale = blinkg2;
        this.eyeF3.xScale = 1.0F + 0.05F * Mth.sin(t * 0.03F + 7.7F);
    }
}