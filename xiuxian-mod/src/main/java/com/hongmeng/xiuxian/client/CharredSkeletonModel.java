package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CharredSkeletonEntity;
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
 * 焦骨骷髅 · Charred Skeleton / CharredSkeletonModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>焦黑骨架弓手：4×6 炭化头颅（两点橙红眼火）+ 6×6 肋骨（右侧断口 2 颗余烬）+ 左手 2 段短弓 + 右手燃烧箭 + 背后皮箭袋；双脚落地 y=24，总高 30px。</p>
 */
public class CharredSkeletonModel extends EntityModel<CharredSkeletonEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "charred_skeleton"), "main");

    private final ModelPart root;
    private final ModelPart pelvis;
    private final ModelPart legUL;
    private final ModelPart legUR;
    private final ModelPart spine;
    private final ModelPart legLL;
    private final ModelPart legLR;
    private final ModelPart neck;
    private final ModelPart ribcage;
    private final ModelPart armUL;
    private final ModelPart armUR;
    private final ModelPart quiver;
    private final ModelPart ribEmber;
    private final ModelPart skull;
    private final ModelPart armLL;
    private final ModelPart armLR;
    private final ModelPart eyes;
    private final ModelPart jaw;
    private final ModelPart handL;
    private final ModelPart handR;
    private final ModelPart arrowShaft;
    private final ModelPart arrowhead;
    private final ModelPart bowLower;
    private final ModelPart bowUpper;

    public CharredSkeletonModel(ModelPart root) {
        this.root = root;
        this.pelvis = root.getChild("pelvis");
        this.legUL = root.getChild("pelvis").getChild("leg_u_l");
        this.legUR = root.getChild("pelvis").getChild("leg_u_r");
        this.spine = root.getChild("pelvis").getChild("spine");
        this.legLL = root.getChild("pelvis").getChild("leg_u_l").getChild("leg_l_l");
        this.legLR = root.getChild("pelvis").getChild("leg_u_r").getChild("leg_l_r");
        this.neck = root.getChild("pelvis").getChild("spine").getChild("neck");
        this.ribcage = root.getChild("pelvis").getChild("spine").getChild("ribcage");
        this.armUL = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_l");
        this.armUR = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_r");
        this.quiver = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("quiver");
        this.ribEmber = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("rib_ember");
        this.skull = root.getChild("pelvis").getChild("spine").getChild("neck").getChild("skull");
        this.armLL = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_l").getChild("arm_l_l");
        this.armLR = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_r").getChild("arm_l_r");
        this.eyes = root.getChild("pelvis").getChild("spine").getChild("neck").getChild("skull").getChild("eyes");
        this.jaw = root.getChild("pelvis").getChild("spine").getChild("neck").getChild("skull").getChild("jaw");
        this.handL = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_l").getChild("arm_l_l").getChild("hand_l");
        this.handR = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_r").getChild("arm_l_r").getChild("hand_r");
        this.arrowShaft = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_r").getChild("arm_l_r").getChild("hand_r").getChild("arrow_shaft");
        this.arrowhead = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_r").getChild("arm_l_r").getChild("hand_r").getChild("arrowhead");
        this.bowLower = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_l").getChild("arm_l_l").getChild("hand_l").getChild("bow_lower");
        this.bowUpper = root.getChild("pelvis").getChild("spine").getChild("ribcage").getChild("arm_u_l").getChild("arm_l_l").getChild("hand_l").getChild("bow_upper");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition pelvis = root.addOrReplaceChild("pelvis",
CubeListBuilder.create()
        .texOffs(24, 18).addBox(-2.0F, -2.0F, -1.5F, 4.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition legUL = pelvis.addOrReplaceChild("leg_u_l",
CubeListBuilder.create()
        .texOffs(39, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(1.5F, 0.0F, 0.0F));
        PartDefinition legUR = pelvis.addOrReplaceChild("leg_u_r",
CubeListBuilder.create()
        .texOffs(44, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(-1.5F, 0.0F, 0.0F));
        PartDefinition spine = pelvis.addOrReplaceChild("spine",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 10.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition legLL = legUL.addOrReplaceChild("leg_l_l",
CubeListBuilder.create()
        .texOffs(49, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition legLR = legUR.addOrReplaceChild("leg_l_r",
CubeListBuilder.create()
        .texOffs(54, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition neck = spine.addOrReplaceChild("neck",
CubeListBuilder.create()
        .texOffs(50, 18).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -10.0F, 0.0F));
        PartDefinition ribcage = spine.addOrReplaceChild("ribcage",
CubeListBuilder.create()
        .texOffs(9, 0).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 6.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition armUL = ribcage.addOrReplaceChild("arm_u_l",
CubeListBuilder.create()
        .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(2.0F, -2.0F, 0.0F));
        PartDefinition armUR = ribcage.addOrReplaceChild("arm_u_r",
CubeListBuilder.create()
        .texOffs(60, 7).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(-2.0F, -2.0F, 0.0F));
        PartDefinition quiver = ribcage.addOrReplaceChild("quiver",
CubeListBuilder.create()
        .texOffs(47, 0).addBox(-1.0F, -4.0F, -1.0F, 3.0F, 7.0F, 3.0F, NONE)
                , PartPose.offset(3.0F, 0.0F, 2.0F));
        PartDefinition ribEmber = ribcage.addOrReplaceChild("rib_ember",
CubeListBuilder.create()
        .texOffs(59, 14).addBox(-2.0F, -5.0F, -2.2F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(59, 17).addBox(-0.5F, -4.0F, -2.2F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition skull = neck.addOrReplaceChild("skull",
CubeListBuilder.create()
        .texOffs(30, 0).addBox(-2.0F, -8.0F, -2.0F, 4.0F, 6.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition armLL = armUL.addOrReplaceChild("arm_l_l",
CubeListBuilder.create()
        .texOffs(24, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition armLR = armUR.addOrReplaceChild("arm_l_r",
CubeListBuilder.create()
        .texOffs(29, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition eyes = skull.addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(0, 19).addBox(-2.0F, -6.0F, -2.2F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(5, 19).addBox(1.0F, -6.0F, -2.2F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition jaw = skull.addOrReplaceChild("jaw",
CubeListBuilder.create()
        .texOffs(39, 18).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition handL = armLL.addOrReplaceChild("hand_l",
CubeListBuilder.create()
        .texOffs(10, 19).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition handR = armLR.addOrReplaceChild("hand_r",
CubeListBuilder.create()
        .texOffs(15, 19).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition arrowShaft = handR.addOrReplaceChild("arrow_shaft",
CubeListBuilder.create()
        .texOffs(9, 11).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 1.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition arrowhead = handR.addOrReplaceChild("arrowhead",
CubeListBuilder.create()
        .texOffs(59, 20).addBox(-0.5F, -3.0F, -3.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition bowLower = handL.addOrReplaceChild("bow_lower",
CubeListBuilder.create()
        .texOffs(0, 13).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition bowUpper = handL.addOrReplaceChild("bow_upper",
CubeListBuilder.create()
        .texOffs(34, 11).addBox(-0.5F, -5.0F, -1.5F, 1.0F, 5.0F, 1.0F, NONE)
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
    public void setupAnim(CharredSkeletonEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float t = ageInTicks;
        float stepA = Mth.cos(limbSwing * 0.62F) * limbSwingAmount;
        float stepB = Mth.cos(limbSwing * 0.62F + 3.1416F) * limbSwingAmount;
        float atk = entity.getAttackAnim(0.0F);

        // ---- 1. 骨盆 / 脊柱：走动时骨架晃，站立时几乎不动 ----
        this.pelvis.y = PELVIS_BASE_Y;
        this.pelvis.zRot = 0.04F * stepA;
        this.spine.xRot = 0.04F * Mth.abs(stepB) + 0.10F * atk;

        // ---- 2. 头骨：点头（idle 0.05）+ 走路朝移动方向 + 攻击时盯着目标 ----
        this.skull.xRot = 0.05F * Mth.sin(t * 0.05F) - 0.12F * atk;
        this.skull.yRot = 0.10F * stepA;
        this.jaw.xRot = 0.20F * atk;
        this.eyes.xRot = 0.0F;

        // ---- 3. 双臂：走路反相摆；抬弓射箭（左手抬弓 1.2、右手拉弦 1.0）----
        this.armUL.xRot = -0.20F * stepA - 1.20F * atk;
        this.armUR.xRot = -0.20F * stepB - 1.00F * atk;
        this.armLL.xRot = 0.10F * stepA - 0.25F * atk;
        this.armLR.xRot = 0.10F * stepB - 0.25F * atk;
        // 拉弦：弓上下段在手心收 1px（xScale 收缩），放箭后回弹
        float draw = 1.0F - 0.12F * atk;
        this.bowUpper.xScale = draw;
        this.bowLower.xScale = draw;
        this.bowUpper.zRot = 0.02F + 0.04F * Mth.sin(t * 0.06F);
        this.bowLower.zRot = -0.02F - 0.04F * Mth.sin(t * 0.06F);
        // 燃烧箭：射击时箭头随手臂抬起（贴图/发光层始终亮）
        this.arrowShaft.xRot = 0.0F;
        this.arrowhead.xRot = 0.0F;

        // ---- 4. 双腿：走路交替迈步（xRot 前后摆）----
        this.legUL.xRot = 0.30F * stepA;
        this.legUR.xRot = 0.30F * stepB;
        this.legLL.xRot = -0.18F * stepA;
        this.legLR.xRot = -0.18F * stepB;

        // ---- 5. 肋骨残呼吸（只动 xScale/zScale；幅度很小，像骨架在"喘"）----
        float breath = 1.0F + 0.03F * Mth.sin(t * 0.05F);
        this.ribcage.xScale = breath;
        this.ribcage.zScale = 1.0F;

        // ---- 6. 受击：后仰 + 张嘴（骨架散一下又拼回来）----
        if (entity.hurtTime > 0) {
            float hurt = (float) entity.hurtTime / 10.0F;
            this.spine.xRot = 0.04F * Mth.abs(stepB) - 0.12F * hurt;
            this.jaw.xRot = 0.20F * atk + 0.20F * hurt;
            this.skull.xRot = 0.05F * Mth.sin(t * 0.05F) - 0.10F * hurt;
        }

        // ---- 7. 死亡：骨架散架（各段错相歪倒）+ 沉地（倒地旋转由渲染器负责）----
        if (entity.isDeadOrDying()) {
            this.pelvis.y = PELVIS_BASE_Y + 2.0F;
            this.spine.xRot = 0.30F;
            this.skull.xRot = 0.45F;
            this.jaw.xRot = 0.40F;
            this.armUL.xRot = -0.60F;
            this.armUR.xRot = 0.55F;
            this.armLL.xRot = -0.45F;
            this.armLR.xRot = 0.40F;
            this.legUL.xRot = 0.35F;
            this.legUR.xRot = -0.35F;
            this.legLL.xRot = 0.30F;
            this.legLR.xRot = -0.30F;
            this.bowUpper.xScale = 1.0F;
            this.bowLower.xScale = 1.0F;
        }

    }


    /** 模型最低点（脚底）= 24：两条小腿末端触地。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 最高点 = -6（头骨顶），模型总高 30px。 */
    public static final int HIGHEST_PIXEL_Y = -6;

    /** 根部件 pelvis 的 pivot y（动画里 y 是绝对赋值，必须带上这个基准）。 */
    public static final float PELVIS_BASE_Y = 14.0F;

}
