package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.beast.JiaolongSpitEntity;
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
 * 蛟龙之息 · JiaolongSpit / JiaolongSpitModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>蛟龙吐出的青绿灵叶：6 个盒子的扁平叶片（叶面 + 中脉 + 前后尖端 + 叶柄 + 灵光）。</p>
 */
public class JiaolongSpitModel extends EntityModel<JiaolongSpitEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "jiaolong_spit"), "main");

    private final ModelPart root;
    private final ModelPart leaf;

    public JiaolongSpitModel(ModelPart root) {
        this.root = root;
        this.leaf = root.getChild("leaf");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition leaf = root.addOrReplaceChild("leaf",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-4.0F, 22.0F, -3.0F, 8.0F, 2.0F, 6.0F, NONE)
        .texOffs(29, 0).addBox(-1.0F, 21.0F, -3.0F, 2.0F, 1.0F, 7.0F, NONE)
        .texOffs(48, 6).addBox(-2.0F, 22.0F, -5.0F, 4.0F, 2.0F, 2.0F, NONE)
        .texOffs(0, 9).addBox(-2.0F, 22.0F, 3.0F, 4.0F, 2.0F, 2.0F, NONE)
        .texOffs(48, 0).addBox(-0.5F, 22.0F, 5.0F, 1.0F, 1.0F, 4.0F, NONE)
        .texOffs(13, 9).addBox(-3.0F, 20.0F, -1.0F, 6.0F, 1.0F, 2.0F, NONE)
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
    public void setupAnim(JiaolongSpitEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // 灵叶的"灵光"呼吸：整体缩放脉动（不动位置）
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.5F) * 0.08F;
        this.leaf.xScale = pulse;
        this.leaf.zScale = pulse;
        this.leaf.yScale = 1.0F + Mth.cos(ageInTicks * 0.5F) * 0.06F;

    }
}
