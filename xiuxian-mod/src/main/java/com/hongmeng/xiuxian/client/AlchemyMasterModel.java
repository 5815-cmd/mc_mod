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
 * 炼丹师 · AlchemyMaster / AlchemyMasterModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>暖橙药袍矮胖丹师：左腰朱砂三节丹葫芦 + 右腰木药匣（药绿标签） + 左手托青铜丹炉盖 + 两圈熏黑袖口 + 药绿头巾。</p>
 */
public class AlchemyMasterModel extends HumanoidModel<CultivationNpcEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "alchemy_master"), "main");

    private final ModelPart root;
    private final ModelPart cuffL;
    private final ModelPart cuffR;
    private final ModelPart eyes;
    private final ModelPart furnaceLid;
    private final ModelPart gourd;
    private final ModelPart headband;
    private final ModelPart medicineBox;

    public AlchemyMasterModel(ModelPart root) {
        super(root);
        this.root = root;
        this.cuffL = root.getChild("left_arm").getChild("cuff_l");
        this.cuffR = root.getChild("right_arm").getChild("cuff_r");
        this.eyes = root.getChild("head").getChild("eyes");
        this.furnaceLid = root.getChild("left_arm").getChild("furnace_lid");
        this.gourd = root.getChild("body").getChild("gourd");
        this.headband = root.getChild("head").getChild("headband");
        this.medicineBox = root.getChild("body").getChild("medicine_box");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition cuffL = root.getChild("left_arm").addOrReplaceChild("cuff_l",
CubeListBuilder.create()
        .texOffs(37, 40).addBox(-1.5F, 8.0F, -2.5F, 5.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition cuffR = root.getChild("right_arm").addOrReplaceChild("cuff_r",
CubeListBuilder.create()
        .texOffs(37, 32).addBox(-3.5F, 8.0F, -2.5F, 5.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyes = root.getChild("head").addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(56, 28).addBox(-2.0F, -4.8F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(58, 31).addBox(1.0F, -4.8F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition furnaceLid = root.getChild("left_arm").addOrReplaceChild("furnace_lid",
CubeListBuilder.create()
        .texOffs(0, 44).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 5.0F, 2.0F, NONE)
        .texOffs(56, 21).addBox(-1.0F, -3.5F, -4.6F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(1.0F, 8.0F, 0.0F));
        PartDefinition gourd = root.getChild("body").addOrReplaceChild("gourd",
CubeListBuilder.create()
        .texOffs(13, 44).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 3.0F, 2.0F, NONE)
        .texOffs(58, 34).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(56, 16).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(2.6F, 9.5F, -2.9F));
        PartDefinition headband = root.getChild("head").addOrReplaceChild("headband",
CubeListBuilder.create()
        .texOffs(0, 32).addBox(-4.4F, 0.0F, -4.4F, 8.8F, 2.0F, 8.8F, NONE)
                , PartPose.offset(0.0F, -7.0F, 0.0F));
        PartDefinition medicineBox = root.getChild("body").addOrReplaceChild("medicine_box",
CubeListBuilder.create()
        .texOffs(24, 44).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 3.0F, 2.0F, NONE)
        .texOffs(56, 25).addBox(-1.0F, 1.0F, -1.3F, 2.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(-2.6F, 9.0F, -2.9F));
        return LayerDefinition.create(mesh, 64, 64);
    }


    @Override
    public void setupAnim(CultivationNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 动作总览：
        //   idle   —— 低头捣药（右腕画圈 20t 周期）、左臂托盖稳住、葫芦轻晃
        //   walk   —— 左臂护着炉盖不动、右臂原版摆幅收一半，身子微晃
        //   talk   —— 每 180t 抬高左臂把炉盖/丹药递出来一次（20t，当 special）
        //   hurt   —— 后仰，葫芦/药匣一晃
        //   death  —— 松手垂臂、低头
        // 所有赋值都是"本帧绝对姿态"，没有任何 += / *=（历史事故原因）。
        boolean walking = limbSwingAmount > 0.04F;
        boolean presenting = !walking && (ageInTicks % 180.0F) < 20.0F;

        // ---- 1. 头：低头看炉（idle 一直微低），递丹时抬起 -----------------
        float headBase = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.03F * limbSwingAmount
                : 0.14F + Mth.sin(ageInTicks * 0.05F) * 0.02F;
        if (presenting) {
            headBase = -0.08F;
        }
        this.head.xRot = this.head.xRot + headBase;

        // ---- 2. 药绿头巾：随头轻晃（绝对赋值）-----------------------------
        this.headband.xRot = Mth.sin(ageInTicks * 0.03F) * 0.012F;
        this.eyes.xRot = 0.0F;

        // ---- 3. 右臂：捣药（画小圈），走路收一半摆幅 ----------------------
        if (walking) {
            this.rightArm.xRot = -0.20F + Mth.cos(limbSwing * 0.6662F) * 0.35F * limbSwingAmount;
            this.rightArm.zRot = -0.06F;
        } else if (presenting) {
            this.rightArm.xRot = -0.60F + Mth.sin(ageInTicks * 0.30F) * 0.05F;
            this.rightArm.zRot = -0.10F;
        } else {
            float grind = ageInTicks * 0.25F;
            this.rightArm.xRot = -0.45F + Mth.sin(grind) * 0.22F;
            this.rightArm.zRot = -0.12F + Mth.cos(grind) * 0.12F;
        }
        this.rightArm.yRot = 0.0F;

        // ---- 4. 左臂：托炉盖，基准前伸角 + 微颤；递丹时抬高 --------------
        float lidArmX = LID_ARM_BASE_X + Mth.sin(ageInTicks * 0.06F) * 0.02F;
        float lidArmZ = 0.14F;
        if (presenting) {
            lidArmX = -1.05F + Mth.sin(ageInTicks * 0.42F) * 0.06F;
            lidArmZ = 0.22F;
        }
        this.leftArm.xRot = lidArmX;
        this.leftArm.zRot = lidArmZ;
        this.leftArm.yRot = 0.0F;
        // 炉盖反向补偿：抵消左臂前伸量，让 4×5 盖面基本保持朝向正前方
        // （绝对赋值；模型里盖面本来就平行于躯干正面，见 spec 4.5）
        this.furnaceLid.xRot = -0.45F - lidArmX * 0.65F;
        this.furnaceLid.zRot = -lidArmZ * 0.60F;

        // ---- 5. 丹葫芦 / 药匣：跟着走动晃（绝对赋值，不累加）-------------
        float sway = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.09F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.gourd.xRot = sway;
        this.gourd.zRot = sway * 0.6F;
        this.medicineBox.xRot = -sway * 0.7F;

        // ---- 6. 袖口：跟随手臂，不单独动（只做绝对归零）-------------------
        this.cuffR.xRot = 0.0F;
        this.cuffL.xRot = 0.0F;

        // ---- 7. hurt：后仰 + 葫芦/药匣一荡 ---------------------------------
        if (entity.hurtTime > 0) {
            float hurt = 1.0F - entity.hurtTime / 10.0F;
            this.head.xRot = this.head.xRot + 0.30F * hurt;
            this.gourd.xRot = sway + 0.30F * hurt;
            this.medicineBox.xRot = -sway * 0.7F - 0.25F * hurt;
            this.rightArm.xRot = this.rightArm.xRot - 0.15F * hurt;
        }

        // ---- 8. death：松手垂臂、低头 --------------------------------------
        if (entity.isDeadOrDying()) {
            this.head.xRot = 0.45F;
            this.rightArm.xRot = -0.05F;
            this.rightArm.zRot = -0.30F;
            this.leftArm.xRot = -0.05F;
            this.leftArm.zRot = 0.30F;
            this.furnaceLid.xRot = 0.0F;
            this.furnaceLid.zRot = 0.0F;
            this.gourd.xRot = 0.0F;
        }

    }


    /** 模型最低点（脚底）= 24。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 头顶 = -8（头巾只到 -8.4 的布料边缘，不另外抬总高），模型总高 32px。 */
    public static final int HIGHEST_PIXEL_Y = -8;

    /** 左臂托炉盖的固定前伸角（弧度）：动画里所有姿态都以它为基准。 */
    public static final float LID_ARM_BASE_X = -0.55F;

}
