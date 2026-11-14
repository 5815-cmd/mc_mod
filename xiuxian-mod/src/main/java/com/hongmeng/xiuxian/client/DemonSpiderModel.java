package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.DemonSpiderEntity;
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
 * 魔蛛 · Demon Spider / DemonSpiderModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>黑石魔窟的八腿猎手：头胸 6×5×8 + 浑圆腹部 8×5×8（中段一圈熔岩发光环）+ 4 只发光眼一排 + 前端双螯；股节水平外伸 4px、胫节垂直落地到 y=24。</p>
 */
public class DemonSpiderModel extends EntityModel<DemonSpiderEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "demon_spider"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart abdomen;
    private final ModelPart eyes;
    private final ModelPart fangs;
    private final ModelPart leg1UL;
    private final ModelPart leg1UR;
    private final ModelPart leg2UL;
    private final ModelPart leg2UR;
    private final ModelPart leg3UL;
    private final ModelPart leg3UR;
    private final ModelPart lavaBand;
    private final ModelPart leg1DL;
    private final ModelPart leg1DR;
    private final ModelPart leg2DL;
    private final ModelPart leg2DR;
    private final ModelPart leg3DL;
    private final ModelPart leg3DR;
    private final ModelPart leg4UL;
    private final ModelPart leg4UR;
    private final ModelPart leg4DL;
    private final ModelPart leg4DR;

    public DemonSpiderModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.abdomen = root.getChild("body").getChild("abdomen");
        this.eyes = root.getChild("body").getChild("eyes");
        this.fangs = root.getChild("body").getChild("fangs");
        this.leg1UL = root.getChild("body").getChild("leg1_u_l");
        this.leg1UR = root.getChild("body").getChild("leg1_u_r");
        this.leg2UL = root.getChild("body").getChild("leg2_u_l");
        this.leg2UR = root.getChild("body").getChild("leg2_u_r");
        this.leg3UL = root.getChild("body").getChild("leg3_u_l");
        this.leg3UR = root.getChild("body").getChild("leg3_u_r");
        this.lavaBand = root.getChild("body").getChild("abdomen").getChild("lava_band");
        this.leg1DL = root.getChild("body").getChild("leg1_u_l").getChild("leg1_d_l");
        this.leg1DR = root.getChild("body").getChild("leg1_u_r").getChild("leg1_d_r");
        this.leg2DL = root.getChild("body").getChild("leg2_u_l").getChild("leg2_d_l");
        this.leg2DR = root.getChild("body").getChild("leg2_u_r").getChild("leg2_d_r");
        this.leg3DL = root.getChild("body").getChild("leg3_u_l").getChild("leg3_d_l");
        this.leg3DR = root.getChild("body").getChild("leg3_u_r").getChild("leg3_d_r");
        this.leg4UL = root.getChild("body").getChild("abdomen").getChild("leg4_u_l");
        this.leg4UR = root.getChild("body").getChild("abdomen").getChild("leg4_u_r");
        this.leg4DL = root.getChild("body").getChild("abdomen").getChild("leg4_u_l").getChild("leg4_d_l");
        this.leg4DR = root.getChild("body").getChild("abdomen").getChild("leg4_u_r").getChild("leg4_d_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(33, 0).addBox(-3.0F, -5.0F, -4.0F, 6.0F, 5.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, 18.0F, -6.0F));
        PartDefinition abdomen = body.addOrReplaceChild("abdomen",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-4.0F, -5.0F, 3.0F, 8.0F, 5.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 4.0F));
        PartDefinition eyes = body.addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(119, 9).addBox(-2.0F, -2.0F, -5.0F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(124, 9).addBox(0.0F, -2.5F, -5.0F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(120, 12).addBox(1.5F, -2.5F, -5.0F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(44, 14).addBox(-0.5F, -1.5F, -5.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition fangs = body.addOrReplaceChild("fangs",
CubeListBuilder.create()
        .texOffs(109, 9).addBox(-1.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, NONE)
        .texOffs(114, 9).addBox(0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition leg1UL = body.addOrReplaceChild("leg1_u_l",
CubeListBuilder.create()
        .texOffs(62, 12).addBox(0.0F, -1.0F, -1.0F, 4.2F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(3.0F, -1.0F, -2.0F, 0.0F, 0.5F, -0.1F));
        PartDefinition leg1UR = body.addOrReplaceChild("leg1_u_r",
CubeListBuilder.create()
        .texOffs(73, 12).addBox(-4.2F, -1.0F, -1.0F, 4.2F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-3.0F, -1.0F, -2.0F, 0.0F, -0.5F, 0.1F));
        PartDefinition leg2UL = body.addOrReplaceChild("leg2_u_l",
CubeListBuilder.create()
        .texOffs(84, 12).addBox(0.0F, -1.0F, -1.0F, 4.2F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(3.0F, -1.0F, 1.0F, 0.0F, 0.2F, -0.1F));
        PartDefinition leg2UR = body.addOrReplaceChild("leg2_u_r",
CubeListBuilder.create()
        .texOffs(109, 13).addBox(-4.2F, -1.0F, -1.0F, 4.2F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-3.0F, -1.0F, 1.0F, 0.0F, -0.2F, 0.1F));
        PartDefinition leg3UL = body.addOrReplaceChild("leg3_u_l",
CubeListBuilder.create()
        .texOffs(0, 14).addBox(0.0F, -1.0F, -1.0F, 4.2F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(3.0F, -1.0F, 4.0F, 0.0F, -0.2F, -0.0F));
        PartDefinition leg3UR = body.addOrReplaceChild("leg3_u_r",
CubeListBuilder.create()
        .texOffs(11, 14).addBox(-4.2F, -1.0F, -1.0F, 4.2F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-3.0F, -1.0F, 4.0F, 0.0F, 0.2F, 0.0F));
        PartDefinition lavaBand = abdomen.addOrReplaceChild("lava_band",
CubeListBuilder.create()
        .texOffs(62, 0).addBox(-4.5F, -3.5F, 2.5F, 9.0F, 2.0F, 9.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition leg1DL = leg1UL.addOrReplaceChild("leg1_d_l",
CubeListBuilder.create()
        .texOffs(99, 0).addBox(0.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(4.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F));
        PartDefinition leg1DR = leg1UR.addOrReplaceChild("leg1_d_r",
CubeListBuilder.create()
        .texOffs(104, 0).addBox(0.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-4.2F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1F));
        PartDefinition leg2DL = leg2UL.addOrReplaceChild("leg2_d_l",
CubeListBuilder.create()
        .texOffs(109, 0).addBox(0.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(4.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F));
        PartDefinition leg2DR = leg2UR.addOrReplaceChild("leg2_d_r",
CubeListBuilder.create()
        .texOffs(114, 0).addBox(0.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-4.2F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1F));
        PartDefinition leg3DL = leg3UL.addOrReplaceChild("leg3_d_l",
CubeListBuilder.create()
        .texOffs(119, 0).addBox(0.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(4.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg3DR = leg3UR.addOrReplaceChild("leg3_d_r",
CubeListBuilder.create()
        .texOffs(124, 0).addBox(0.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-4.2F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition leg4UL = abdomen.addOrReplaceChild("leg4_u_l",
CubeListBuilder.create()
        .texOffs(22, 14).addBox(0.0F, -1.0F, -1.0F, 4.2F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(3.0F, -1.0F, 3.0F, 0.0F, -0.5F, -0.0F));
        PartDefinition leg4UR = abdomen.addOrReplaceChild("leg4_u_r",
CubeListBuilder.create()
        .texOffs(33, 14).addBox(-4.2F, -1.0F, -1.0F, 4.2F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-3.0F, -1.0F, 3.0F, 0.0F, 0.5F, 0.0F));
        PartDefinition leg4DL = leg4UL.addOrReplaceChild("leg4_d_l",
CubeListBuilder.create()
        .texOffs(99, 9).addBox(0.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(4.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg4DR = leg4UR.addOrReplaceChild("leg4_d_r",
CubeListBuilder.create()
        .texOffs(104, 9).addBox(0.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-4.2F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
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
    public void setupAnim(DemonSpiderEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float t = ageInTicks;
        float stepA = Mth.cos(limbSwing * 0.62F) * limbSwingAmount;
        float stepB = Mth.cos(limbSwing * 0.62F + 3.1416F) * limbSwingAmount;
        float atk = entity.getAttackAnim(0.0F);
        float idle = Mth.sin(t * 0.05F) * 0.02F;

        // ---- 1. 身体：走动时低伏起伏（y 绝对赋值，基准 BODY_BASE_Y）----
        this.body.y = BODY_BASE_Y + 0.35F * Mth.abs(stepA);
        this.body.zRot = 0.02F * Mth.sin(t * 0.04F);
        this.body.xRot = 0.10F * atk;          // 扑咬前倾

        // ---- 2. 八腿：对角步态（绕竖轴 yRot 前后摆 → 足端高度不变）----
        //   静态外张：前两对朝前 / 后两对朝后（俯视 8 条放射线），走路再叠加摆动
        this.leg1UL.yRot = LEG1_SPLAY + 0.18F * stepA;
        this.leg1UR.yRot = -LEG1_SPLAY - 0.18F * stepB;
        this.leg2UL.yRot = LEG2_SPLAY + 0.16F * stepB;
        this.leg2UR.yRot = -LEG2_SPLAY - 0.16F * stepA;
        this.leg3UL.yRot = LEG3_SPLAY - 0.16F * stepA;
        this.leg3UR.yRot = -LEG3_SPLAY + 0.16F * stepB;
        this.leg4UL.yRot = LEG4_SPLAY - 0.18F * stepB;
        this.leg4UR.yRot = -LEG4_SPLAY + 0.18F * stepA;
        // 前两对腿保留"略抬"的基准 zRot，走路再叠一点点起伏
        this.leg1UL.zRot = -FRONT_LEG_LIFT - 0.03F * Mth.abs(stepA);
        this.leg1UR.zRot = FRONT_LEG_LIFT + 0.03F * Mth.abs(stepA);
        this.leg2UL.zRot = -FRONT_LEG_LIFT - 0.03F * Mth.abs(stepB);
        this.leg2UR.zRot = FRONT_LEG_LIFT + 0.03F * Mth.abs(stepB);
        // 胫节反向补偿一点，让摆动看起来像"腿在蹬地"
        this.leg1DL.yRot = -0.12F * stepA;
        this.leg1DR.yRot = 0.12F * stepB;
        this.leg2DL.yRot = -0.12F * stepB;
        this.leg2DR.yRot = 0.12F * stepA;
        this.leg3DL.yRot = 0.12F * stepA;
        this.leg3DR.yRot = -0.12F * stepB;
        this.leg4DL.yRot = 0.12F * stepB;
        this.leg4DR.yRot = -0.12F * stepA;

        // ---- 3. 腹部：呼吸（只动 xScale / zScale）+ 熔岩裂环随之鼓动 ----
        float breath = 1.0F + 0.03F * Mth.sin(t * 0.06F) + 0.06F * atk;
        this.abdomen.xScale = breath;
        this.abdomen.zScale = breath;
        this.lavaBand.xScale = 1.0F + 0.05F * Mth.sin(t * 0.10F);
        this.lavaBand.zScale = this.lavaBand.xScale;
        this.abdomen.y = ABDOMEN_BASE_Y;

        // ---- 4. 4 眼交替微亮（scale 脉动）+ 螯牙随攻击张合 ----
        this.eyes.xScale = 1.0F + 0.06F * Mth.sin(t * 0.11F);
        this.eyes.zScale = 1.0F + 0.06F * Mth.sin(t * 0.11F + 1.5F);
        this.fangs.xRot = 0.40F * atk;

        // ---- 5. 受击：伏低 + 甲壳一缩 ----
        if (entity.hurtTime > 0) {
            float hurt = (float) entity.hurtTime / 10.0F;
            this.body.y = BODY_BASE_Y + 0.35F * Mth.abs(stepA) + 0.50F * hurt;
            this.body.zRot = 0.10F * hurt * Mth.sin(t * 0.8F);
            this.abdomen.xScale = breath - 0.10F * hurt;
            this.abdomen.zScale = breath - 0.10F * hurt;
        }

        // ---- 6. 死亡：八腿蜷缩 + 腹部垂落（倒地旋转由渲染器负责）----
        if (entity.isDeadOrDying()) {
            this.leg1UL.zRot = 1.20F;
            this.leg1UR.zRot = -1.20F;
            this.leg2UL.zRot = 1.20F;
            this.leg2UR.zRot = -1.20F;
            this.leg3UL.zRot = 1.20F;
            this.leg3UR.zRot = -1.20F;
            this.leg4UL.zRot = 1.20F;
            this.leg4UR.zRot = -1.20F;
            this.abdomen.y = ABDOMEN_BASE_Y + 3.0F;
            this.lavaBand.xScale = 1.0F;
            this.lavaBand.zScale = 1.0F;
        }

    }


    /** 模型最低点（脚底）= 24：后两对腿的胫节末端触地。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 最高点 = 13（头胸 / 腹部顶面），模型总高 11px。 */
    public static final int HIGHEST_PIXEL_Y = 13;

    /** 根部件 body 的 pivot y（动画里 y 是绝对赋值，必须带上这个基准）。 */
    public static final float BODY_BASE_Y = 18.0F;

    /** 腹部 pivot 相对 body 的 y（死亡时腹部脱落用）。 */
    public static final float ABDOMEN_BASE_Y = 0.0F;

    /** 前两对腿的抬起角（弧度）：静止时略抬 0.4px，走路时保留。 */
    public static final float FRONT_LEG_LIFT = 0.10F;

    /** 四对腿的静态外张角（弧度）：前两对朝前、后两对朝后 → 俯视 8 条放射线。 */
    public static final float LEG1_SPLAY = 0.45F;
    public static final float LEG2_SPLAY = 0.25F;
    public static final float LEG3_SPLAY = -0.25F;
    public static final float LEG4_SPLAY = -0.45F;

}
