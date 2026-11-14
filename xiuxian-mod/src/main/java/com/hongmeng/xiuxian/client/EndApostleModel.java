package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.EndApostleEntity;
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
 * 终界使徒 · End Apostle / EndApostleModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>无脚黑袍 + 尖顶兜帽 + 背后断裂终界门环 + 4 只不与脸同步的浮眼（末地精英）</p>
 */
public class EndApostleModel extends EntityModel<EndApostleEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "end_apostle"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart core;
    private final ModelPart handL;
    private final ModelPart handR;
    private final ModelPart hem;
    private final ModelPart neck;
    private final ModelPart ring0;
    private final ModelPart ring1;
    private final ModelPart ring2;
    private final ModelPart ring3;
    private final ModelPart ring4;
    private final ModelPart ring5;
    private final ModelPart shoulderL;
    private final ModelPart shoulderR;
    private final ModelPart sleeveL;
    private final ModelPart sleeveR;
    private final ModelPart head;
    private final ModelPart ringShard;
    private final ModelPart eyeA;
    private final ModelPart eyeB;
    private final ModelPart eyeC;
    private final ModelPart eyeD;
    private final ModelPart face;
    private final ModelPart hoodFold;

    public EndApostleModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.core = root.getChild("body").getChild("core");
        this.handL = root.getChild("body").getChild("hand_l");
        this.handR = root.getChild("body").getChild("hand_r");
        this.hem = root.getChild("body").getChild("hem");
        this.neck = root.getChild("body").getChild("neck");
        this.ring0 = root.getChild("body").getChild("ring_0");
        this.ring1 = root.getChild("body").getChild("ring_1");
        this.ring2 = root.getChild("body").getChild("ring_2");
        this.ring3 = root.getChild("body").getChild("ring_3");
        this.ring4 = root.getChild("body").getChild("ring_4");
        this.ring5 = root.getChild("body").getChild("ring_5");
        this.shoulderL = root.getChild("body").getChild("shoulder_l");
        this.shoulderR = root.getChild("body").getChild("shoulder_r");
        this.sleeveL = root.getChild("body").getChild("sleeve_l");
        this.sleeveR = root.getChild("body").getChild("sleeve_r");
        this.head = root.getChild("body").getChild("neck").getChild("head");
        this.ringShard = root.getChild("body").getChild("ring_4").getChild("ring_shard");
        this.eyeA = root.getChild("body").getChild("neck").getChild("head").getChild("eye_a");
        this.eyeB = root.getChild("body").getChild("neck").getChild("head").getChild("eye_b");
        this.eyeC = root.getChild("body").getChild("neck").getChild("head").getChild("eye_c");
        this.eyeD = root.getChild("body").getChild("neck").getChild("head").getChild("eye_d");
        this.face = root.getChild("body").getChild("neck").getChild("head").getChild("face");
        this.hoodFold = root.getChild("body").getChild("neck").getChild("head").getChild("hood_fold");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-4.0F, -22.0F, -2.5F, 8.0F, 18.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition core = body.addOrReplaceChild("core",
CubeListBuilder.create()
        .texOffs(45, 17).addBox(-1.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -16.0F, -2.7F));
        PartDefinition handL = body.addOrReplaceChild("hand_l",
CubeListBuilder.create()
        .texOffs(115, 21).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(5.0F, -10.0F, 0.0F));
        PartDefinition handR = body.addOrReplaceChild("hand_r",
CubeListBuilder.create()
        .texOffs(45, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(-5.0F, -10.0F, 0.0F));
        PartDefinition hem = body.addOrReplaceChild("hem",
CubeListBuilder.create()
        .texOffs(52, 0).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
CubeListBuilder.create()
        .texOffs(82, 20).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -22.0F, 0.0F));
        PartDefinition ring0 = body.addOrReplaceChild("ring_0",
CubeListBuilder.create()
        .texOffs(54, 23).addBox(6.5F, 3.5F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -20.0F, 4.5F));
        PartDefinition ring1 = body.addOrReplaceChild("ring_1",
CubeListBuilder.create()
        .texOffs(116, 0).addBox(-1.0F, 7.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -20.0F, 4.5F));
        PartDefinition ring2 = body.addOrReplaceChild("ring_2",
CubeListBuilder.create()
        .texOffs(116, 7).addBox(-8.5F, 2.5F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -20.0F, 4.5F));
        PartDefinition ring3 = body.addOrReplaceChild("ring_3",
CubeListBuilder.create()
        .texOffs(116, 14).addBox(-8.5F, -6.5F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -20.0F, 4.5F));
        PartDefinition ring4 = body.addOrReplaceChild("ring_4",
CubeListBuilder.create()
        .texOffs(27, 17).addBox(-1.0F, -11.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -20.0F, 4.5F));
        PartDefinition ring5 = body.addOrReplaceChild("ring_5",
CubeListBuilder.create()
        .texOffs(36, 17).addBox(6.5F, -6.5F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -20.0F, 4.5F));
        PartDefinition shoulderL = body.addOrReplaceChild("shoulder_l",
CubeListBuilder.create()
        .texOffs(82, 12).addBox(-1.5F, -2.0F, -2.5F, 3.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(4.0F, -18.0F, 0.0F));
        PartDefinition shoulderR = body.addOrReplaceChild("shoulder_r",
CubeListBuilder.create()
        .texOffs(99, 12).addBox(-1.5F, -2.0F, -2.5F, 3.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(-4.0F, -18.0F, 0.0F));
        PartDefinition sleeveL = body.addOrReplaceChild("sleeve_l",
CubeListBuilder.create()
        .texOffs(52, 12).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F, NONE)
                , PartPose.offset(5.0F, -16.0F, 0.0F));
        PartDefinition sleeveR = body.addOrReplaceChild("sleeve_r",
CubeListBuilder.create()
        .texOffs(67, 12).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F, NONE)
                , PartPose.offset(-5.0F, -16.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(27, 0).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition ringShard = ring4.addOrReplaceChild("ring_shard",
CubeListBuilder.create()
        .texOffs(63, 23).addBox(-3.0F, -1.5F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(-4.0F, -11.0F, 0.0F));
        PartDefinition eyeA = head.addOrReplaceChild("eye_a",
CubeListBuilder.create()
        .texOffs(72, 23).addBox(-3.0F, -7.0F, -4.8F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeB = head.addOrReplaceChild("eye_b",
CubeListBuilder.create()
        .texOffs(0, 24).addBox(1.0F, -7.0F, -4.8F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeC = head.addOrReplaceChild("eye_c",
CubeListBuilder.create()
        .texOffs(7, 24).addBox(-5.0F, -6.0F, -1.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeD = head.addOrReplaceChild("eye_d",
CubeListBuilder.create()
        .texOffs(14, 24).addBox(3.0F, -6.0F, -1.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition face = head.addOrReplaceChild("face",
CubeListBuilder.create()
        .texOffs(95, 20).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, -3.0F));
        PartDefinition hoodFold = head.addOrReplaceChild("hood_fold",
CubeListBuilder.create()
        .texOffs(87, 0).addBox(-3.5F, -2.0F, -3.5F, 7.0F, 4.0F, 7.0F, NONE)
        .texOffs(106, 20).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -6.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
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
    public void setupAnim(EndApostleEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        final float DEG = 0.017453292F;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        float t = ageInTicks;
        float walk = limbSwingAmount;
        float hurt = entity.hurtTime > 0 ? (float) entity.hurtTime / 10.0F : 0.0F;

        // ---- 悬浮：整身 0~1px 起落（基准 24，永不沉到地面以下）----
        float bob = 0.5F - 0.5F * Mth.sin(t * 0.05F);
        this.body.y = 24.0F - bob;
        float bodyPitch = 0.06F * walk + 0.012F * Mth.sin(t * 0.031F);
        if (hurt > 0.0F) {
            bodyPitch = bodyPitch - 0.15F * hurt;
        }
        this.body.xRot = bodyPitch;
        this.body.zRot = 0.02F * Mth.sin(t * 0.027F);

        // ---- 袍呼吸（只动 xScale/zScale）----
        float breathe = 1.0F + 0.02F * Mth.sin(t * 0.06F);
        this.body.xScale = breathe;
        this.body.zScale = breathe;

        // ---- 头：兜帽随视线，但比视线慢（朝圣者的凝视）----
        this.head.yRot = yaw * 0.60F;
        this.head.xRot = pitch * 0.40F + 0.02F * Mth.sin(t * 0.041F);
        this.hoodFold.xRot = 0.02F * Mth.sin(t * 0.045F + 0.6F);

        // ---- 袍摆 ----
        this.hem.zRot = 0.04F * Mth.sin(t * 0.05F) + 0.05F * Mth.cos(limbSwing * 0.5F) * walk;

        // ---- 4 只浮眼：各自上下 ±1px，相位 0/π/π/0（不与脸同步）----
        float eyeBob = 1.0F * Mth.sin(t * 0.04F);
        this.eyeA.y = eyeBob;
        this.eyeB.y = -eyeBob;
        this.eyeC.y = -eyeBob;
        this.eyeD.y = eyeBob;

        // ---- 背后断环：绕 Y 轴匀速自转（飘移时快 50%）----
        float ringSpin = t * (0.02F + 0.01F * walk);
        this.ring0.yRot = ringSpin;
        this.ring1.yRot = ringSpin;
        this.ring2.yRot = ringSpin;
        this.ring3.yRot = ringSpin;
        this.ring4.yRot = ringSpin;
        this.ring5.yRot = ringSpin;
        this.ringShard.yRot = ringSpin * 1.2F;

        // ---- 胸口核心：脉冲（xScale/zScale）----
        float corePulse = 1.0F + 0.12F * (0.5F + 0.5F * Mth.sin(t * 0.08F));
        this.core.xScale = corePulse;
        this.core.zScale = corePulse;

        // ---- 袖管：飘移时前后摆 ----
        float sleeveSwing = 0.10F * Mth.cos(limbSwing * 0.5F) * walk;
        this.sleeveL.xRot = sleeveSwing;
        this.sleeveR.xRot = -sleeveSwing;

        // ---- 死亡：袍塌陷、4 眼飘散（倒地由渲染器负责）----
        if (entity.isDeadOrDying()) {
            this.body.y = 25.0F;
            this.body.xScale = 0.70F;
            this.body.zScale = 0.70F;
            this.hem.y = -5.0F;
            this.eyeA.y = 3.0F;
            this.eyeB.y = 3.0F;
            this.eyeC.y = 3.0F;
            this.eyeD.y = 3.0F;
        }

    }


    /** 模型最低点像素 y（袍摆底）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 兜帽尖 = -9（模型总高 33px）。 */
    public static final int HIGHEST_PIXEL_Y = -9;

}
