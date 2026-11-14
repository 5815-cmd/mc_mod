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
 * <p>110px 巨型仙庭之主：三眼玉面 + 平天冕板 + 背后十二旒/日轮 + 宽袍大袖 + 手持玉圭</p>
 */
public class HeavenLordModel extends EntityModel<HeavenLordEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "heaven_lord"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart cape;
    private final ModelPart head;
    private final ModelPart ribbonL;
    private final ModelPart ribbonR;
    private final ModelPart crown;
    private final ModelPart eyes;
    private final ModelPart gui;
    private final ModelPart haloMount;
    private final ModelPart mianliu;
    private final ModelPart haloDisc;
    private final ModelPart haloRing;

    public HeavenLordModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.armL = root.getChild("body").getChild("arm_l");
        this.armR = root.getChild("body").getChild("arm_r");
        this.cape = root.getChild("body").getChild("cape");
        this.head = root.getChild("body").getChild("head");
        this.ribbonL = root.getChild("body").getChild("ribbon_l");
        this.ribbonR = root.getChild("body").getChild("ribbon_r");
        this.crown = root.getChild("body").getChild("head").getChild("crown");
        this.eyes = root.getChild("body").getChild("head").getChild("eyes");
        this.gui = root.getChild("body").getChild("arm_r").getChild("gui");
        this.haloMount = root.getChild("body").getChild("head").getChild("crown").getChild("halo_mount");
        this.mianliu = root.getChild("body").getChild("head").getChild("crown").getChild("mianliu");
        this.haloDisc = root.getChild("body").getChild("head").getChild("crown").getChild("halo_mount").getChild("halo_disc");
        this.haloRing = root.getChild("body").getChild("head").getChild("crown").getChild("halo_mount").getChild("halo_ring");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(85, 111).addBox(-17.0F, -4.0F, -12.0F, 34.0F, 4.0F, 24.0F, NONE)
        .texOffs(0, 140).addBox(-18.0F, -5.0F, -12.0F, 36.0F, 1.0F, 24.0F, NONE)
        .texOffs(63, 0).addBox(-15.0F, -22.0F, -11.0F, 30.0F, 18.0F, 22.0F, NONE)
        .texOffs(116, 76).addBox(-13.0F, -36.0F, -10.0F, 26.0F, 14.0F, 20.0F, NONE)
        .texOffs(0, 78).addBox(-12.0F, -52.0F, -9.0F, 24.0F, 16.0F, 18.0F, NONE)
        .texOffs(209, 85).addBox(-9.0F, -48.0F, -12.0F, 18.0F, 12.0F, 2.0F, NONE)
        .texOffs(121, 140).addBox(-14.0F, -39.0F, -11.0F, 28.0F, 3.0F, 22.0F, NONE)
        .texOffs(0, 166).addBox(-18.0F, -56.0F, -10.0F, 36.0F, 4.0F, 20.0F, NONE)
        .texOffs(113, 187).addBox(-8.0F, -60.0F, -7.0F, 16.0F, 4.0F, 14.0F, NONE)
        .texOffs(222, 130).addBox(-4.0F, -64.0F, -4.0F, 8.0F, 4.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition armL = body.addOrReplaceChild("arm_l",
CubeListBuilder.create()
        .texOffs(209, 100).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 6.0F, 8.0F, NONE)
        .texOffs(221, 39).addBox(-3.0F, 4.0F, -3.0F, 6.0F, 16.0F, 6.0F, NONE)
        .texOffs(168, 39).addBox(-6.0F, 14.0F, -7.0F, 12.0F, 22.0F, 14.0F, NONE)
        .texOffs(174, 187).addBox(-7.0F, 36.0F, -8.0F, 14.0F, 2.0F, 16.0F, NONE)
        .texOffs(39, 69).addBox(-2.0F, 38.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(15.0F, -54.0F, 0.0F));
        PartDefinition armR = body.addOrReplaceChild("arm_r",
CubeListBuilder.create()
        .texOffs(202, 115).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 6.0F, 8.0F, NONE)
        .texOffs(221, 62).addBox(-3.0F, 4.0F, -3.0F, 6.0F, 16.0F, 6.0F, NONE)
        .texOffs(63, 41).addBox(-6.0F, 14.0F, -7.0F, 12.0F, 22.0F, 14.0F, NONE)
        .texOffs(0, 191).addBox(-7.0F, 36.0F, -8.0F, 14.0F, 2.0F, 16.0F, NONE)
        .texOffs(85, 94).addBox(-2.0F, 38.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(-15.0F, -54.0F, 0.0F));
        PartDefinition cape = body.addOrReplaceChild("cape",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-14.0F, 0.0F, 0.0F, 28.0F, 52.0F, 3.0F, NONE)
        .texOffs(134, 206).addBox(-15.0F, 50.0F, 0.0F, 30.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -54.0F, 10.0F));
        PartDefinition head = body.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(168, 0).addBox(-8.0F, -22.0F, -8.0F, 16.0F, 22.0F, 16.0F, NONE)
        .texOffs(85, 78).addBox(-6.0F, -18.0F, -9.0F, 12.0F, 14.0F, 1.0F, NONE)
        .texOffs(222, 150).addBox(-7.0F, -22.0F, -9.0F, 14.0F, 2.0F, 1.0F, NONE)
        .texOffs(161, 41).addBox(-8.0F, -18.0F, -9.5F, 1.0F, 14.0F, 1.0F, NONE)
        .texOffs(246, 54).addBox(7.0F, -18.0F, -9.5F, 1.0F, 14.0F, 1.0F, NONE)
        .texOffs(85, 103).addBox(-4.0F, -6.0F, -9.5F, 8.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -64.0F, 0.0F));
        PartDefinition ribbonL = body.addOrReplaceChild("ribbon_l",
CubeListBuilder.create()
        .texOffs(246, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 24.0F, 2.0F, NONE)
                , PartPose.offset(14.0F, -24.0F, -12.0F));
        PartDefinition ribbonR = body.addOrReplaceChild("ribbon_r",
CubeListBuilder.create()
        .texOffs(246, 27).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 24.0F, 2.0F, NONE)
                , PartPose.offset(-14.0F, -24.0F, -12.0F));
        PartDefinition crown = head.addOrReplaceChild("crown",
CubeListBuilder.create()
        .texOffs(113, 166).addBox(-13.0F, -4.0F, -8.0F, 26.0F, 4.0F, 16.0F, NONE)
        .texOffs(61, 206).addBox(-11.0F, -5.0F, -7.0F, 22.0F, 1.0F, 14.0F, NONE)
        .texOffs(0, 113).addBox(-7.0F, -14.0F, -7.0F, 14.0F, 9.0F, 14.0F, NONE)
        .texOffs(57, 134).addBox(-3.0F, -12.0F, -8.0F, 6.0F, 4.0F, 1.0F, NONE)
        .texOffs(39, 56).addBox(-2.0F, -22.0F, -2.0F, 4.0F, 8.0F, 4.0F, NONE)
        .texOffs(233, 33).addBox(-1.0F, -24.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -22.0F, 0.0F));
        PartDefinition eyes = head.addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(246, 70).addBox(-5.0F, -12.0F, -10.0F, 3.0F, 2.0F, 1.0F, NONE)
        .texOffs(246, 74).addBox(2.0F, -12.0F, -10.0F, 3.0F, 2.0F, 1.0F, NONE)
        .texOffs(56, 68).addBox(-1.0F, -17.0F, -10.0F, 2.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition gui = armR.addOrReplaceChild("gui",
CubeListBuilder.create()
        .texOffs(233, 0).addBox(-2.0F, -30.0F, -1.0F, 4.0F, 30.0F, 2.0F, NONE)
        .texOffs(222, 143).addBox(-2.0F, -34.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(-4.0F, 40.0F, -9.0F));
        PartDefinition haloMount = crown.addOrReplaceChild("halo_mount",
CubeListBuilder.create()
        .texOffs(0, 56).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 15.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition mianliu = crown.addOrReplaceChild("mianliu",
CubeListBuilder.create()
        .texOffs(251, 54).addBox(-10.5F, 0.0F, -9.5F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(56, 56).addBox(-6.5F, 0.0F, -9.5F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(161, 57).addBox(-2.5F, 0.0F, -9.5F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(116, 64).addBox(1.5F, 0.0F, -9.5F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(121, 64).addBox(5.5F, 0.0F, -9.5F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(126, 64).addBox(9.5F, 0.0F, -9.5F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(131, 64).addBox(-10.5F, 0.0F, 7.0F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(136, 64).addBox(-6.5F, 0.0F, 7.0F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(141, 64).addBox(-2.5F, 0.0F, 7.0F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(146, 64).addBox(1.5F, 0.0F, 7.0F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(151, 64).addBox(5.5F, 0.0F, 7.0F, 1.0F, 10.0F, 1.0F, NONE)
        .texOffs(156, 64).addBox(9.5F, 0.0F, 7.0F, 1.0F, 10.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition haloDisc = haloMount.addOrReplaceChild("halo_disc",
CubeListBuilder.create()
        .texOffs(116, 41).addBox(-10.0F, -10.0F, -1.0F, 20.0F, 20.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 15.0F));
        PartDefinition haloRing = haloMount.addOrReplaceChild("halo_ring",
CubeListBuilder.create()
        .texOffs(102, 94).addBox(18.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(242, 100).addBox(15.5F, 8.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(242, 107).addBox(8.0F, 15.5F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(57, 113).addBox(-2.0F, 18.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(70, 113).addBox(-12.0F, 15.5F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(242, 114).addBox(-19.5F, 8.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(57, 120).addBox(-22.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(70, 120).addBox(-19.5F, -12.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(235, 121).addBox(-12.0F, -19.5F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(57, 127).addBox(-2.0F, -22.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(70, 127).addBox(8.0F, -19.5F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(202, 130).addBox(15.5F, -12.0F, -1.0F, 4.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 15.0F));
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

        final float DEG = 0.017453292F;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        float t = ageInTicks;
        float walk = limbSwingAmount;
        float hurt = entity.hurtTime > 0 ? (float) entity.hurtTime / 10.0F : 0.0F;
        int phase = com.hongmeng.xiuxian.entity.BossPhase.phaseOf(entity);
        if (phase < 1) {
            phase = 1;
        }
        float phaseBoost = 1.0F + 0.45F * (float) (phase - 1);   // 二/三阶段：日轮更快、眼更亮

        // ---- 袍身：巨物呼吸（极慢）+ 受击后仰 ----
        float breathe = 1.0F + 0.012F * Mth.sin(t * 0.022F);
        this.body.xScale = breathe;
        this.body.zScale = breathe;
        this.body.xRot = 0.008F * Mth.sin(t * 0.018F) - 0.045F * hurt;
        this.body.zRot = 0.006F * Mth.sin(t * 0.024F + 0.7F);

        // ---- 头：缓慢俯视（压迫感来自"它在看你"）----
        this.head.yRot = yaw * 0.45F;
        this.head.xRot = pitch * 0.35F + 0.010F * Mth.sin(t * 0.026F);
        this.head.zRot = 0.008F * Mth.sin(t * 0.021F + 1.2F);

        // ---- 三眼：仙金脉动（阶段越高越亮）----
        float eyePulse = (1.0F + 0.06F * Mth.sin(t * 0.05F)) * phaseBoost;
        this.eyes.xScale = eyePulse;
        this.eyes.zScale = eyePulse;

        // ---- 冕旒玉冠：轻微摇晃 ----
        this.crown.xRot = 0.010F * Mth.sin(t * 0.02F);
        this.crown.zRot = 0.008F * Mth.sin(t * 0.017F + 0.5F);

        // ---- 十二旒：前后两排玉旒轻摆 ----
        this.mianliu.xRot = 0.02F + 0.02F * Mth.sin(t * 0.03F);
        this.mianliu.zRot = 0.015F * Mth.sin(t * 0.027F + 1.0F);

        // ---- 日轮：整环绕 Z 轴匀速转动（阶段越后越快）+ 心盘脉动 ----
        this.haloRing.zRot = t * 0.006F * phaseBoost;
        float discPulse = (1.0F + 0.04F * Mth.sin(t * 0.035F)) * phaseBoost;
        this.haloDisc.xScale = discPulse;
        this.haloDisc.zScale = discPulse;
        this.haloMount.zRot = 0.010F * Mth.sin(t * 0.019F);

        // ---- 宽袍大袖：idle 慢摆；行走时反向摆 ----
        float armSwing = 0.10F * Mth.cos(limbSwing * 0.5F) * walk;
        this.armL.xRot = 0.02F * Mth.sin(t * 0.021F) + armSwing;
        this.armR.xRot = 0.02F * Mth.sin(t * 0.021F + 0.9F) - armSwing;
        this.armL.zRot = 0.020F * Mth.sin(t * 0.018F + 0.4F);
        this.armR.zRot = -0.020F * Mth.sin(t * 0.018F + 0.4F);
        this.gui.xRot = 0.012F * Mth.sin(t * 0.02F + 1.5F);

        // ---- 披风 / 垂地飘带：随动作微摆 ----
        this.cape.xRot = -0.02F - 0.03F * walk + 0.012F * Mth.sin(t * 0.023F);
        this.cape.zRot = 0.010F * Mth.sin(t * 0.019F + 0.8F);
        this.ribbonL.xRot = 0.02F * Mth.sin(t * 0.025F);
        this.ribbonL.zRot = 0.03F * Mth.sin(t * 0.028F + 2.1F);
        this.ribbonR.xRot = 0.02F * Mth.sin(t * 0.025F + 1.1F);
        this.ribbonR.zRot = -0.03F * Mth.sin(t * 0.028F + 2.1F);

        // ---- 死亡：玉衡倾倒、日轮熄灭、袍塌（倒地由渲染器负责）----
        if (entity.isDeadOrDying()) {
            this.body.xRot = 0.14F;
            this.body.xScale = 0.92F;
            this.body.zScale = 0.92F;
            this.head.xRot = 0.30F;
            this.crown.xRot = 0.45F;
            this.haloRing.zRot = 0.0F;
            this.haloDisc.xScale = 0.40F;
            this.haloDisc.zScale = 0.40F;
            this.eyes.xScale = 0.30F;
            this.eyes.zScale = 0.30F;
            this.mianliu.xRot = 0.35F;
        }

    }


    /** 模型最低点像素 y（袍摆底 / 垂地飘带）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 冠顶玉衡 + 日轮上缘 = -86（模型总高 110px = 6.875 格）。 */
    public static final int HIGHEST_PIXEL_Y = -86;

}
