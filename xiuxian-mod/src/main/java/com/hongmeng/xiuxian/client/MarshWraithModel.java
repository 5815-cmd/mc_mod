package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.MarshWraithEntity;
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
 * 泽魅 · Marsh Wraith / MarshWraithModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>破斗笠压眉、脸只剩两点青磷、向下收窄的破袍堆 + 4 条碎布袍角，右手挑一盏青磷灯笼</p>
 */
public class MarshWraithModel extends EntityModel<MarshWraithEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "marsh_wraith"), "main");

    private final ModelPart root;
    private final ModelPart wraith;
    private final ModelPart arm;
    private final ModelPart head;
    private final ModelPart robeTails;
    private final ModelPart hat;
    private final ModelPart lantern;

    public MarshWraithModel(ModelPart root) {
        this.root = root;
        this.wraith = root.getChild("wraith");
        this.arm = root.getChild("wraith").getChild("arm");
        this.head = root.getChild("wraith").getChild("head");
        this.robeTails = root.getChild("wraith").getChild("robe_tails");
        this.hat = root.getChild("wraith").getChild("head").getChild("hat");
        this.lantern = root.getChild("wraith").getChild("arm").getChild("lantern");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition wraith = root.addOrReplaceChild("wraith",
CubeListBuilder.create()
        .texOffs(0, 14).addBox(-4.0F, 3.0F, -3.5F, 8.0F, 3.0F, 7.0F, NONE)
        .texOffs(33, 25).addBox(-3.5F, 6.0F, -3.0F, 7.0F, 3.0F, 6.0F, NONE)
        .texOffs(25, 35).addBox(-3.0F, 9.0F, -2.5F, 6.0F, 3.0F, 5.0F, NONE)
        .texOffs(0, 44).addBox(-2.5F, 12.0F, -2.0F, 5.0F, 3.0F, 4.0F, NONE)
        .texOffs(48, 35).addBox(-2.0F, 15.0F, -1.5F, 4.0F, 3.0F, 3.0F, NONE)
        .texOffs(48, 42).addBox(-1.5F, 18.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition arm = wraith.addOrReplaceChild("arm",
CubeListBuilder.create()
        .texOffs(49, 0).addBox(-0.5F, 0.0F, -2.5F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(32, 50).addBox(-1.0F, 4.0F, -3.5F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(0, 52).addBox(-2.0F, 5.5F, -5.0F, 1.0F, 1.0F, 3.0F, NONE)
        .texOffs(49, 9).addBox(-1.0F, 6.5F, -5.0F, 1.0F, 1.5F, 1.0F, NONE)
                , PartPose.offset(-5.0F, 4.0F, 0.0F));
        PartDefinition head = wraith.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(31, 14).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 4.0F, 6.0F, NONE)
        .texOffs(58, 0).addBox(-2.5F, -3.0F, -3.5F, 2.0F, 1.5F, 1.0F, NONE)
        .texOffs(58, 4).addBox(0.5F, -3.0F, -3.5F, 2.0F, 1.5F, 1.0F, NONE)
                , PartPose.offset(0.0F, 3.0F, 0.0F));
        PartDefinition robeTails = wraith.addOrReplaceChild("robe_tails",
CubeListBuilder.create()
        .texOffs(56, 9).addBox(-2.5F, 0.0F, -1.5F, 1.5F, 3.0F, 1.5F, NONE)
        .texOffs(56, 15).addBox(1.0F, 0.0F, -1.5F, 1.5F, 3.0F, 1.5F, NONE)
        .texOffs(19, 44).addBox(-2.5F, 0.0F, 0.0F, 1.5F, 3.0F, 1.5F, NONE)
        .texOffs(28, 44).addBox(1.0F, 0.0F, 0.0F, 1.5F, 3.0F, 1.5F, NONE)
                , PartPose.offset(0.0F, 21.0F, 0.0F));
        PartDefinition hat = head.addOrReplaceChild("hat",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-6.0F, -1.0F, -6.0F, 12.0F, 1.0F, 12.0F, NONE)
        .texOffs(0, 25).addBox(-4.0F, -2.0F, -4.0F, 8.0F, 1.0F, 8.0F, NONE)
        .texOffs(0, 35).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 2.0F, 6.0F, NONE)
        .texOffs(56, 21).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 1.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition lantern = arm.addOrReplaceChild("lantern",
CubeListBuilder.create()
        .texOffs(46, 49).addBox(-1.0F, 1.5F, -1.0F, 3.0F, 1.0F, 3.0F, NONE)
        .texOffs(37, 44).addBox(-0.5F, 2.5F, -0.5F, 2.0F, 3.0F, 2.0F, NONE)
        .texOffs(19, 50).addBox(-1.0F, 5.5F, -1.0F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offset(-1.0F, 6.5F, -5.0F));
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
    public void setupAnim(MarshWraithEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        final float DEG = 0.017453292F;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        float t = ageInTicks;
        boolean walking = limbSwingAmount > 0.04F;

        // ---- 飘浮：整体 0.5px 起落（模型最低点静态仍是 y=24，不整体上移）----
        this.wraith.y = -0.5F + 0.5F * Mth.sin(t * 0.05F);
        this.wraith.xRot = -0.03F * limbSwingAmount;
        this.wraith.zRot = 0.02F * Mth.sin(t * 0.031F);

        // ---- 头：斗笠压眉，随视线缓慢转 ----
        float headX = pitch * 0.40F;
        float headY = yaw * 0.60F;
        this.head.xRot = headX;
        this.head.yRot = headY;
        this.head.zRot = 0.02F * Mth.sin(t * 0.037F);

        // ---- 破斗笠：比头慢半拍（压在眉骨上的旧斗笠）----
        this.hat.xRot = headX + 0.02F + 0.02F * Mth.sin(t * 0.041F);
        this.hat.yRot = headY;
        this.hat.zRot = 0.03F * Mth.sin(t * 0.033F + 0.9F);

        // ---- 碎布袍角：飘着时轻摆，追击时向后拖 ----
        float drag = walking ? 0.26F * limbSwingAmount : 0.04F;
        this.robeTails.xRot = drag + 0.06F * Mth.sin(t * 0.043F);
        this.robeTails.zRot = 0.05F * Mth.sin(t * 0.037F + 1.3F);

        // ---- 右臂 + 青磷灯笼：灯笼像钟摆（灯是这只怪的第二剪影点）----
        this.arm.xRot = -0.06F + 0.05F * Mth.sin(t * 0.05F);
        this.arm.zRot = 0.03F * Mth.sin(t * 0.043F + 0.5F);
        this.lantern.xRot = 0.20F * Mth.sin(t * 0.062F + 0.4F);
        this.lantern.zRot = 0.14F * Mth.sin(t * 0.051F + 1.1F);

        // ---- 受击：斗笠后仰、灯笼乱晃 ----
        if (entity.hurtTime > 0) {
            float hurt = (float) entity.hurtTime / 10.0F;
            this.head.xRot = headX + 0.28F * hurt;
            this.hat.xRot = headX + 0.30F * hurt;
            this.wraith.xRot = -0.03F * limbSwingAmount - 0.10F * hurt;
            this.lantern.zRot = 0.35F * hurt + 0.20F * Mth.sin(t * 0.5F);
        }

        // ---- 死亡：提灯松脱（倒地旋转由渲染器负责）----
        if (entity.isDeadOrDying()) {
            this.lantern.xRot = 0.90F;
            this.robeTails.xRot = 0.18F;
            this.hat.zRot = 0.10F;
        }

    }


    /** 模型最低点像素 y（袍角末端触地）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 斗笠尖 = -6（模型总高 30px）。 */
    public static final int HIGHEST_PIXEL_Y = -6;

}
