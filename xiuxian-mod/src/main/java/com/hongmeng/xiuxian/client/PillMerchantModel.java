package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import net.minecraft.client.model.HumanoidModel;
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
 * 丹药商人 · PillMerchant / PillMerchantModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>靛蓝矮胖丹药摊主：左右腰各一串朱砂丹葫芦 + 胸前木框金珠算盘 + 背后竹幡杆挑一面米白招牌小幡（左肩上方 5×5）。</p>
 */
public class PillMerchantModel extends HumanoidModel<CultivationNpcEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "pill_merchant"), "main");

    private final ModelPart root;
    private final ModelPart abacus;
    private final ModelPart bannerCloth;
    private final ModelPart bannerPole;
    private final ModelPart eyes;
    private final ModelPart gourdStringL;
    private final ModelPart gourdStringR;

    public PillMerchantModel(ModelPart root) {
        super(root);
        this.root = root;
        this.abacus = root.getChild("body").getChild("abacus");
        this.bannerCloth = root.getChild("body").getChild("banner_cloth");
        this.bannerPole = root.getChild("body").getChild("banner_pole");
        this.eyes = root.getChild("head").getChild("eyes");
        this.gourdStringL = root.getChild("body").getChild("gourd_string_l");
        this.gourdStringR = root.getChild("body").getChild("gourd_string_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition abacus = root.getChild("body").addOrReplaceChild("abacus",
CubeListBuilder.create()
        .texOffs(13, 38).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, NONE)
        .texOffs(35, 32).addBox(-2.0F, 0.5F, -0.5F, 4.0F, 3.0F, 1.0F, NONE)
        .texOffs(0, 39).addBox(-1.5F, 1.5F, -1.0F, 3.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 2.5F, -2.6F));
        PartDefinition bannerCloth = root.getChild("body").addOrReplaceChild("banner_cloth",
CubeListBuilder.create()
        .texOffs(0, 32).addBox(0.5F, -16.0F, -0.9F, 5.0F, 5.0F, 1.0F, NONE)
        .texOffs(18, 38).addBox(0.5F, -11.0F, -0.9F, 5.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(4.0F, 6.0F, 2.6F));
        PartDefinition bannerPole = root.getChild("body").addOrReplaceChild("banner_pole",
CubeListBuilder.create()
        .texOffs(56, 16).addBox(4.5F, -16.0F, -0.5F, 1.0F, 25.0F, 1.0F, NONE)
                , PartPose.offset(4.0F, 6.0F, 2.6F));
        PartDefinition eyes = root.getChild("head").addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(18, 41).addBox(-2.0F, -4.8F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(23, 41).addBox(1.0F, -4.8F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition gourdStringL = root.getChild("body").addOrReplaceChild("gourd_string_l",
CubeListBuilder.create()
        .texOffs(44, 37).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, NONE)
        .texOffs(46, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(13, 32).addBox(-1.5F, 2.0F, -1.0F, 3.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(2.6F, 9.5F, -2.9F));
        PartDefinition gourdStringR = root.getChild("body").addOrReplaceChild("gourd_string_r",
CubeListBuilder.create()
        .texOffs(49, 37).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, NONE)
        .texOffs(35, 37).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(24, 32).addBox(-1.5F, 2.0F, -1.0F, 3.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(-2.6F, 9.5F, -2.9F));
        return LayerDefinition.create(mesh, 64, 64);
    }


    @Override
    public void setupAnim(CultivationNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 动作总览：
        //   idle   —— 头微低看账、左手在胸前拨算盘（10t 一次）、幡面轻晃
        //   walk   —— 右臂摆、幡杆随步上下弹（双步频），两串葫芦反相荡
        //   talk   —— 每 160t 抬右手招呼一趟（20t，当 special）
        //   hurt   —— 后仰、两串葫芦一起荡、幡杆一沉
        //   death  —— 幡落、松手垂臂
        // 所有赋值都是"本帧绝对姿态"，没有任何 += / *=（历史事故原因）。
        boolean walking = limbSwingAmount > 0.04F;
        boolean welcoming = !walking && (ageInTicks % 160.0F) < 20.0F;

        // ---- 1. 头：站着微低头看账，招呼时抬起来 -------------------------
        float nod = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.03F * limbSwingAmount
                : 0.10F + Mth.sin(ageInTicks * 0.05F) * 0.02F;
        if (welcoming) {
            nod = -0.06F;
        }
        this.head.xRot = this.head.xRot + nod;
        this.eyes.xRot = 0.0F;

        // ---- 2. 左臂：在胸前拨算盘（基准前伸角 + 手腕小幅点动）-----------
        float flick = Mth.sin(ageInTicks * 0.63F) * 0.05F;
        this.leftArm.xRot = ABACUS_ARM_BASE_X + flick;
        this.leftArm.yRot = 0.0F;
        this.leftArm.zRot = 0.18F + Mth.cos(ageInTicks * 0.63F) * 0.05F;

        // ---- 3. 右臂：走路摆臂；招呼时整条手臂抬起来摇 -------------------
        float rightArmX;
        float rightArmZ = -0.05F;
        if (walking) {
            rightArmX = Mth.cos(limbSwing * 0.6662F) * 0.70F * limbSwingAmount;
        } else if (welcoming) {
            rightArmX = -2.20F + Mth.sin(ageInTicks * 0.42F) * 0.15F;
            rightArmZ = 0.18F;
        } else {
            rightArmX = -0.08F + Mth.sin(ageInTicks * 0.05F) * 0.02F;
        }
        this.rightArm.xRot = rightArmX;
        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = rightArmZ;

        // ---- 4. 算盘：挂在胸口，走路轻晃（绝对赋值）---------------------
        float sway = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.06F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.05F) * 0.02F;
        this.abacus.xRot = sway;
        this.abacus.zRot = sway * 0.6F;

        // ---- 5. 两串丹葫芦：走动时反相荡（绝对赋值）---------------------
        float swingL = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.16F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.06F) * 0.04F;
        this.gourdStringL.xRot = swingL;
        this.gourdStringL.zRot = swingL * 0.5F;
        this.gourdStringR.xRot = -swingL * 0.9F;
        this.gourdStringR.zRot = -swingL * 0.5F;

        // ---- 6. 招牌小幡：走一步弹两下（双步频），幡面滞后飘 -------------
        float bounce = walking
                ? Mth.cos(limbSwing * 1.3324F) * 1.20F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.06F) * 0.10F;
        this.bannerPole.y = BANNER_BASE_Y + bounce;
        this.bannerPole.zRot = Mth.sin(ageInTicks * 0.04F) * 0.02F;
        this.bannerCloth.y = BANNER_BASE_Y + bounce;
        this.bannerCloth.xRot = walking
                ? Mth.cos(limbSwing * 1.3324F - 1.0F) * 0.08F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.bannerCloth.zRot = Mth.sin(ageInTicks * 0.04F) * 0.05F;

        // ---- 7. hurt：后仰 + 葫芦/幡一起荡 --------------------------------
        if (entity.hurtTime > 0) {
            float hurt = 1.0F - entity.hurtTime / 10.0F;
            this.head.xRot = this.head.xRot + 0.30F * hurt;
            this.gourdStringL.xRot = swingL + 0.35F * hurt;
            this.gourdStringR.xRot = -swingL * 0.9F + 0.30F * hurt;
            this.bannerPole.y = BANNER_BASE_Y + bounce - 0.25F * hurt;
            this.bannerCloth.y = this.bannerPole.y;
        }

        // ---- 8. death：幡落、松手垂臂（倒地旋转交给原版渲染器）----------
        if (entity.isDeadOrDying()) {
            this.head.xRot = 0.35F;
            this.leftArm.xRot = -0.05F;
            this.leftArm.zRot = 0.30F;
            this.rightArm.xRot = -0.05F;
            this.rightArm.zRot = -0.30F;
            this.bannerPole.y = BANNER_BASE_Y - 1.0F;
            this.bannerPole.zRot = 0.45F;
            this.bannerCloth.y = BANNER_BASE_Y - 1.0F;
            this.bannerCloth.zRot = 0.45F;
            this.gourdStringL.xRot = 0.0F;
            this.gourdStringR.xRot = 0.0F;
        }

    }


    /** 模型最低点（脚底）= 24。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 幡顶 = -10（人形头顶仍是 -8）；模型总高 34px，多出的 2px 是细幡杆 + 布幡。 */
    public static final int HIGHEST_PIXEL_Y = -10;

    /** 幡杆基准 y（摆动用绝对赋值）。 */
    public static final float BANNER_BASE_Y = 6.0F;

    /** 左臂拨算盘的固定前伸角（弧度）：动画里所有姿态都以它为基准。 */
    public static final float ABACUS_ARM_BASE_X = -0.72F;

}
