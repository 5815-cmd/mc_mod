package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.VoidJellyEntity;
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
 * 虚空水母 · Void Jelly / VoidJellyModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>钟形伞盖（3 层）+ 4 条两段长触须 + 伞盖正面一只紫青虚空眼；伞盖一开一合的虚空漂游体</p>
 */
public class VoidJellyModel extends EntityModel<VoidJellyEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "void_jelly"), "main");

    private final ModelPart root;
    private final ModelPart jelly;
    private final ModelPart bell;
    private final ModelPart tentA;
    private final ModelPart tentB;
    private final ModelPart tentC;
    private final ModelPart tentD;
    private final ModelPart eye;
    private final ModelPart rimGlow;
    private final ModelPart tentA2;
    private final ModelPart tentB2;
    private final ModelPart tentC2;
    private final ModelPart tentD2;

    public VoidJellyModel(ModelPart root) {
        this.root = root;
        this.jelly = root.getChild("jelly");
        this.bell = root.getChild("jelly").getChild("bell");
        this.tentA = root.getChild("jelly").getChild("tent_a");
        this.tentB = root.getChild("jelly").getChild("tent_b");
        this.tentC = root.getChild("jelly").getChild("tent_c");
        this.tentD = root.getChild("jelly").getChild("tent_d");
        this.eye = root.getChild("jelly").getChild("bell").getChild("eye");
        this.rimGlow = root.getChild("jelly").getChild("bell").getChild("rim_glow");
        this.tentA2 = root.getChild("jelly").getChild("tent_a").getChild("tent_a2");
        this.tentB2 = root.getChild("jelly").getChild("tent_b").getChild("tent_b2");
        this.tentC2 = root.getChild("jelly").getChild("tent_c").getChild("tent_c2");
        this.tentD2 = root.getChild("jelly").getChild("tent_d").getChild("tent_d2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition jelly = root.addOrReplaceChild("jelly",
CubeListBuilder.create()
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition bell = jelly.addOrReplaceChild("bell",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 3.0F, 12.0F, NONE)
        .texOffs(0, 30).addBox(-4.5F, -3.0F, -4.5F, 9.0F, 3.0F, 9.0F, NONE)
        .texOffs(37, 30).addBox(-3.0F, -5.0F, -3.0F, 6.0F, 2.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition tentA = jelly.addOrReplaceChild("tent_a",
CubeListBuilder.create()
        .texOffs(54, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(3.5F, 13.0F, 3.5F, 0.1F, 0.0F, 0.0F));
        PartDefinition tentB = jelly.addOrReplaceChild("tent_b",
CubeListBuilder.create()
        .texOffs(59, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-3.5F, 13.0F, 3.5F, 0.1F, 0.0F, 0.0F));
        PartDefinition tentC = jelly.addOrReplaceChild("tent_c",
CubeListBuilder.create()
        .texOffs(54, 15).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(3.5F, 13.0F, -3.5F, 0.1F, 0.0F, 0.0F));
        PartDefinition tentD = jelly.addOrReplaceChild("tent_d",
CubeListBuilder.create()
        .texOffs(59, 15).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-3.5F, 13.0F, -3.5F, 0.1F, 0.0F, 0.0F));
        PartDefinition eye = bell.addOrReplaceChild("eye",
CubeListBuilder.create()
        .texOffs(54, 22).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 4.0F, 1.0F, NONE)
        .texOffs(37, 39).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 1.0F, -6.0F));
        PartDefinition rimGlow = bell.addOrReplaceChild("rim_glow",
CubeListBuilder.create()
        .texOffs(0, 43).addBox(-6.0F, 2.0F, -6.5F, 12.0F, 1.0F, 1.0F, NONE)
        .texOffs(27, 43).addBox(-6.0F, 2.0F, 5.5F, 12.0F, 1.0F, 1.0F, NONE)
        .texOffs(0, 16).addBox(5.5F, 2.0F, -6.0F, 1.0F, 1.0F, 12.0F, NONE)
        .texOffs(27, 16).addBox(-6.5F, 2.0F, -6.0F, 1.0F, 1.0F, 12.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tentA2 = tentA.addOrReplaceChild("tent_a2",
CubeListBuilder.create()
        .texOffs(49, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition tentB2 = tentB.addOrReplaceChild("tent_b2",
CubeListBuilder.create()
        .texOffs(54, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition tentC2 = tentC.addOrReplaceChild("tent_c2",
CubeListBuilder.create()
        .texOffs(59, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition tentD2 = tentD.addOrReplaceChild("tent_d2",
CubeListBuilder.create()
        .texOffs(49, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
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
    public void setupAnim(VoidJellyEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float t = ageInTicks;

        // ---- 整体飘浮（基准 y=0；模型最低点静态仍在 24）----
        this.jelly.y = -0.6F + 0.6F * Mth.sin(t * 0.045F + 0.5F);
        this.jelly.xRot = 0.03F * limbSwingAmount;
        this.jelly.zRot = 0.025F * Mth.sin(t * 0.032F);

        // ---- 伞盖一开一合（只动 xScale / zScale）----
        float pulse = 0.90F + 0.10F * Mth.sin(t * 0.06F);
        this.bell.xScale = pulse;
        this.bell.zScale = pulse;
        this.bell.yRot = 0.10F * Mth.sin(t * 0.030F);
        this.bell.xRot = 0.04F * Mth.sin(t * 0.037F + 1.0F);

        // ---- 虚空眼：脉动（眼瞳比伞盖亮得快）----
        float eyePulse = 1.0F + 0.10F * Mth.sin(t * 0.09F);
        this.eye.xScale = eyePulse;
        this.eye.zScale = eyePulse;
        this.eye.yRot = 0.12F * Mth.sin(t * 0.026F);

        // ---- 4 条触须拖尾（错相；下段比上段慢半拍）----
        float s = 0.10F * Mth.sin(t * 0.050F) + 0.12F * limbSwingAmount;
        this.tentA.xRot = 0.10F + s;
        this.tentA2.xRot = -0.10F + s * 1.5F;
        this.tentB.xRot = 0.10F - s * 0.8F;
        this.tentB2.xRot = -0.10F - s * 1.2F;
        this.tentC.xRot = 0.10F + s * 0.6F;
        this.tentC2.xRot = -0.10F + s * 0.9F;
        this.tentD.xRot = 0.10F - s * 1.1F;
        this.tentD2.xRot = -0.10F - s * 1.6F;
        this.tentA.zRot = 0.05F * Mth.sin(t * 0.043F);
        this.tentB.zRot = 0.05F * Mth.sin(t * 0.043F + 1.6F);
        this.tentC.zRot = 0.05F * Mth.sin(t * 0.043F + 3.1F);
        this.tentD.zRot = 0.05F * Mth.sin(t * 0.043F + 4.6F);

        // ---- 受击：伞盖一缩、触须乱甩 ----
        if (entity.hurtTime > 0) {
            float hurt = (float) entity.hurtTime / 10.0F;
            this.bell.xScale = pulse - 0.15F * hurt;
            this.bell.zScale = pulse - 0.15F * hurt;
            this.tentA2.xRot = -0.10F + s * 1.5F + 0.5F * hurt;
            this.tentB2.xRot = -0.10F - s * 1.2F + 0.4F * hurt;
        }

        // ---- 死亡：伞盖塌下、触须垂落（倒地旋转由渲染器负责）----
        if (entity.isDeadOrDying()) {
            this.jelly.y = 0.3F;
            this.bell.xScale = 0.75F;
            this.bell.zScale = 0.75F;
            this.tentA.xRot = 0.55F;
            this.tentB.xRot = 0.55F;
            this.tentC.xRot = 0.55F;
            this.tentD.xRot = 0.55F;
        }

    }


    /** 模型最低点像素 y（触须末端触地）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 伞盖顶尖 = 5（模型总高 19px）。 */
    public static final int HIGHEST_PIXEL_Y = 5;

}
