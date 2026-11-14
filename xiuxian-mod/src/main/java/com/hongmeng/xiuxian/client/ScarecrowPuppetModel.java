package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.ScarecrowPuppetEntity;
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
 * 稻草傀儡 · Scarecrow Puppet / ScarecrowPuppetModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>十字竹架 + 草把躯干 + 破斗笠 + 空袖管 + 肩头黑鸦；剪影=十字架+斗笠。</p>
 */
public class ScarecrowPuppetModel extends EntityModel<ScarecrowPuppetEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "scarecrow_puppet"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart crossbar;
    private final ModelPart crow;
    private final ModelPart post;
    private final ModelPart sleeveL;
    private final ModelPart sleeveR;
    private final ModelPart head;

    public ScarecrowPuppetModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.crossbar = root.getChild("crossbar");
        this.crow = root.getChild("crow");
        this.post = root.getChild("post");
        this.sleeveL = root.getChild("sleeve_l");
        this.sleeveR = root.getChild("sleeve_r");
        this.head = root.getChild("body").getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(36, 0).addBox(-3.0F, 6.0F, -2.0F, 6.0F, 8.0F, 4.0F, NONE)
        .texOffs(33, 33).addBox(-4.0F, 13.0F, -2.5F, 8.0F, 3.0F, 5.0F, NONE)
        .texOffs(25, 42).addBox(-3.5F, 9.0F, -2.5F, 7.0F, 1.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition crossbar = root.addOrReplaceChild("crossbar",
CubeListBuilder.create()
        .texOffs(0, 49).addBox(-4.0F, 4.0F, -1.0F, 8.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition crow = root.addOrReplaceChild("crow",
CubeListBuilder.create()
        .texOffs(0, 23).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, NONE)
        .texOffs(50, 42).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(57, 10).addBox(-1.0F, -4.0F, -2.0F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(57, 0).addBox(-1.0F, -2.0F, 1.0F, 1.0F, 1.0F, 2.0F, NONE)
        .texOffs(36, 13).addBox(-1.0F, -5.0F, -1.2F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(41, 13).addBox(0.0F, -5.0F, -1.2F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(4.5F, 4.0F, 0.0F));
        PartDefinition post = root.addOrReplaceChild("post",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 20.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition sleeveL = root.addOrReplaceChild("sleeve_l",
CubeListBuilder.create()
        .texOffs(54, 13).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, NONE)
                , PartPose.offset(-4.0F, 6.0F, 0.0F));
        PartDefinition sleeveR = root.addOrReplaceChild("sleeve_r",
CubeListBuilder.create()
        .texOffs(54, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, NONE)
                , PartPose.offset(4.0F, 6.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(9, 0).addBox(-3.5F, -8.0F, -3.0F, 7.0F, 10.0F, 6.0F, NONE)
        .texOffs(9, 17).addBox(-5.5F, -9.0F, -5.5F, 11.0F, 1.0F, 11.0F, NONE)
        .texOffs(0, 40).addBox(-3.0F, -11.0F, -3.0F, 6.0F, 2.0F, 6.0F, NONE)
        .texOffs(0, 30).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 1.0F, 8.0F, NONE)
        .texOffs(57, 4).addBox(-2.0F, -8.0F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(57, 7).addBox(1.0F, -8.0F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 3.0F, 0.0F));
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
    public void setupAnim(ScarecrowPuppetEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // 头：布袋脑袋只能转一半（僵硬感）
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.45F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.45F;
        this.head.zRot = Mth.sin(ageInTicks * 0.045F) * 0.025F;
        // 空袖管：小幅度摆（走路时反相）
        this.sleeveL.xRot = 0.06F + Mth.cos(limbSwing * 0.55F) * 0.32F * limbSwingAmount;
        this.sleeveR.xRot = 0.06F + Mth.cos(limbSwing * 0.55F + (float) Math.PI) * 0.32F * limbSwingAmount;
        this.sleeveL.zRot = 0.05F + Mth.sin(ageInTicks * 0.06F) * 0.02F;
        this.sleeveR.zRot = -0.05F - Mth.sin(ageInTicks * 0.06F + 0.8F) * 0.02F;
        // 黑鸦：左右张望 + 点头
        this.crow.yRot = Mth.sin(ageInTicks * 0.03F) * 0.40F;
        this.crow.xRot = Mth.sin(ageInTicks * 0.10F) * 0.05F;
        // 草把躯干：走路时整体轻晃（不破坏十字架）
        this.body.yRot = Mth.sin(limbSwing * 0.5F) * 0.05F * limbSwingAmount;
        this.body.xRot = 0.0F;
        this.body.zRot = 0.0F;
        // 竖桩：像插在地里一样，只随步伐轻微受力
        this.post.zRot = Mth.cos(limbSwing * 0.5F) * 0.03F * limbSwingAmount;
        this.crossbar.zRot = this.post.zRot;

    }
}
