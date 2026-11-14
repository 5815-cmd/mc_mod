package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.VenomSpitEntity;
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
 * 毒涎 · VenomSpit / VenomSpitModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>毒涎蟾喷出的暗红蟾涎球：6 个盒子的不规则球体 + 四颗瘤块 + 一颗偏亮的毒核。</p>
 */
public class VenomSpitModel extends EntityModel<VenomSpitEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "venom_spit"), "main");

    private final ModelPart root;
    private final ModelPart core;

    public VenomSpitModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition core = root.addOrReplaceChild("core",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-4.0F, 16.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE)
        .texOffs(33, 9).addBox(-2.0F, 14.0F, -2.0F, 4.0F, 2.0F, 4.0F, NONE)
        .texOffs(50, 9).addBox(-2.0F, 18.0F, -6.0F, 4.0F, 4.0F, 2.0F, NONE)
        .texOffs(33, 0).addBox(-6.0F, 18.0F, -2.0F, 2.0F, 4.0F, 4.0F, NONE)
        .texOffs(46, 0).addBox(4.0F, 18.0F, -2.0F, 2.0F, 4.0F, 4.0F, NONE)
        .texOffs(33, 16).addBox(-2.0F, 20.0F, -7.0F, 4.0F, 2.0F, 1.0F, NONE)
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
    public void setupAnim(VenomSpitEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // 涎液在飞的时候还在鼓：缩放做脉动，不动位置
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.45F) * 0.10F;
        this.core.xScale = pulse;
        this.core.zScale = pulse;
        this.core.yScale = 2.0F - pulse;

    }
}
