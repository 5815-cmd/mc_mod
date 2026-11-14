package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.BloodLeechEntity;
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
 * 血蛭 · Blood Leech / BloodLeechModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>5 节鼓包蠕虫 + 口器环 6 齿 + 尾端血囊；无腿长条怪，贴地蠕动。</p>
 */
public class BloodLeechModel extends EntityModel<BloodLeechEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "blood_leech"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart eyeL;
    private final ModelPart eyeR;
    private final ModelPart hump;
    private final ModelPart mouthRing;
    private final ModelPart sac;
    private final ModelPart sacGlow;
    private final ModelPart seg1;
    private final ModelPart seg2;
    private final ModelPart seg3;
    private final ModelPart seg4;
    private final ModelPart tooth0;
    private final ModelPart tooth1;
    private final ModelPart tooth2;
    private final ModelPart tooth3;
    private final ModelPart tooth4;
    private final ModelPart tooth5;

    public BloodLeechModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.eyeL = root.getChild("body").getChild("eye_l");
        this.eyeR = root.getChild("body").getChild("eye_r");
        this.hump = root.getChild("body").getChild("hump");
        this.mouthRing = root.getChild("body").getChild("mouth_ring");
        this.sac = root.getChild("body").getChild("sac");
        this.sacGlow = root.getChild("body").getChild("sac_glow");
        this.seg1 = root.getChild("body").getChild("seg1");
        this.seg2 = root.getChild("body").getChild("seg2");
        this.seg3 = root.getChild("body").getChild("seg3");
        this.seg4 = root.getChild("body").getChild("seg4");
        this.tooth0 = root.getChild("body").getChild("tooth_0");
        this.tooth1 = root.getChild("body").getChild("tooth_1");
        this.tooth2 = root.getChild("body").getChild("tooth_2");
        this.tooth3 = root.getChild("body").getChild("tooth_3");
        this.tooth4 = root.getChild("body").getChild("tooth_4");
        this.tooth5 = root.getChild("body").getChild("tooth_5");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition eyeL = body.addOrReplaceChild("eye_l",
CubeListBuilder.create()
        .texOffs(48, 29).addBox(-2.5F, -9.0F, -5.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeR = body.addOrReplaceChild("eye_r",
CubeListBuilder.create()
        .texOffs(53, 29).addBox(1.5F, -9.0F, -5.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition hump = body.addOrReplaceChild("hump",
CubeListBuilder.create()
        .texOffs(0, 24).addBox(-3.0F, -8.0F, -5.0F, 6.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition mouthRing = body.addOrReplaceChild("mouth_ring",
CubeListBuilder.create()
        .texOffs(48, 23).addBox(-2.0F, -4.0F, -17.0F, 4.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition sac = body.addOrReplaceChild("sac",
CubeListBuilder.create()
        .texOffs(0, 12).addBox(-3.5F, -5.0F, 6.0F, 7.0F, 5.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition sacGlow = body.addOrReplaceChild("sac_glow",
CubeListBuilder.create()
        .texOffs(56, 12).addBox(-1.0F, -6.0F, 9.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition seg1 = body.addOrReplaceChild("seg1",
CubeListBuilder.create()
        .texOffs(27, 23).addBox(-3.0F, -4.0F, -16.0F, 6.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition seg2 = body.addOrReplaceChild("seg2",
CubeListBuilder.create()
        .texOffs(27, 12).addBox(-4.0F, -4.0F, -12.0F, 8.0F, 4.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition seg3 = body.addOrReplaceChild("seg3",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-5.0F, -5.0F, -6.0F, 10.0F, 5.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition seg4 = body.addOrReplaceChild("seg4",
CubeListBuilder.create()
        .texOffs(33, 0).addBox(-4.0F, -5.0F, 0.0F, 8.0F, 5.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tooth0 = body.addOrReplaceChild("tooth_0",
CubeListBuilder.create()
        .texOffs(56, 17).addBox(-2.0F, -3.0F, -18.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tooth1 = body.addOrReplaceChild("tooth_1",
CubeListBuilder.create()
        .texOffs(56, 20).addBox(0.0F, -4.0F, -18.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tooth2 = body.addOrReplaceChild("tooth_2",
CubeListBuilder.create()
        .texOffs(59, 23).addBox(2.0F, -3.0F, -18.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tooth3 = body.addOrReplaceChild("tooth_3",
CubeListBuilder.create()
        .texOffs(21, 24).addBox(-1.0F, -1.0F, -18.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tooth4 = body.addOrReplaceChild("tooth_4",
CubeListBuilder.create()
        .texOffs(59, 26).addBox(1.0F, -1.0F, -18.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tooth5 = body.addOrReplaceChild("tooth_5",
CubeListBuilder.create()
        .texOffs(21, 27).addBox(0.0F, -2.0F, -18.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
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
    public void setupAnim(BloodLeechEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // ---- 输入信号（全部来自实体公开状态 / 原版动画参数）----
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float breath = Mth.sin(ageInTicks * 0.09F);
        float atk = entity.getAttackAnim(0.0F);
        float bite = Mth.sin(atk * (float) Math.PI);          // 0→1→0 的咬合曲线
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);
        float wave = Mth.sin(ageInTicks * 0.22F) * (0.06F + 0.10F * walk);

        // ---- 整体：贴地起伏（蠕动）+ 受击后仰 + 死亡下沉 ----
        this.body.y = 24.0F - Math.abs(Mth.sin(ageInTicks * 0.18F)) * 0.5F * walk - dead * 1.0F;
        this.body.xRot = wave * 0.15F - hurt * 0.10F;
        this.body.z = -2.0F * bite;                          // 咬中时整条前窜 2px
        this.body.xScale = 1.0F + dead * 0.10F;
        this.body.zScale = 1.0F + dead * 0.10F;

        // ---- 5 节鼓包：行波（相位差 1.2 rad），节间不脱节 ----
        this.seg1.yRot = Mth.sin(ageInTicks * 0.22F + 0.0F) * wave;
        this.seg1.xRot = -0.25F * bite + hurt * 0.12F;
        this.seg2.yRot = Mth.sin(ageInTicks * 0.22F + 0.8F) * wave;
        this.seg2.xRot = Mth.sin(ageInTicks * 0.22F + 0.8F) * wave * 0.5F;
        this.seg3.yRot = Mth.sin(ageInTicks * 0.22F + 1.6F) * wave;
        this.seg3.xRot = Mth.sin(ageInTicks * 0.22F + 1.6F) * wave * 0.5F;
        this.seg4.yRot = Mth.sin(ageInTicks * 0.22F + 2.4F) * wave;
        this.seg4.xRot = Mth.sin(ageInTicks * 0.22F + 2.4F) * wave * 0.5F;
        this.hump.y = -0.3F * Math.abs(breath) * (0.4F + walk);

        // ---- 尾血囊：鼓动（呼吸 1.0→1.08）；死亡时瘪掉 ----
        float pulse = 1.0F + 0.08F * (0.5F + 0.5F * Mth.sin(ageInTicks * 0.16F));
        float sacScale = Mth.lerp(dead, pulse, 0.5F);
        this.sac.xScale = sacScale;
        this.sac.zScale = sacScale;
        this.sac.yScale = 1.0F - dead * 0.35F;
        this.sacGlow.xScale = sacScale;
        this.sacGlow.zScale = sacScale;

        // ---- 口器环：咀嚼预备 + 猛咬张开；受击收缩 ----
        float chew = 0.05F * Mth.sin(ageInTicks * 0.06F);
        float open = 1.0F + 0.35F * bite;
        float ringScale = Mth.lerp(hurt, open, 0.8F);
        this.mouthRing.xScale = ringScale;
        this.mouthRing.zScale = ringScale;
        this.mouthRing.xRot = chew - bite * 0.30F;

        // ---- 双眼：只在呼吸时轻微缩放（发光件，位置固定）----
        float eyeScale = 1.0F + 0.08F * breath;
        this.eyeL.xScale = eyeScale;
        this.eyeL.zScale = eyeScale;
        this.eyeR.xScale = eyeScale;
        this.eyeR.zScale = eyeScale;

    }
}
