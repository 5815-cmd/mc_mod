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
 * 炼器师 · ArtifactRefiner / ArtifactRefinerModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>赤膊皮围裙 + 5px 宽皮护肩（NPC 里最壮）+ 背后两柄交叉剑胚（剑尖探出双肩）+ 右手铁锤 + 额前皮头带。</p>
 */
public class ArtifactRefinerModel extends HumanoidModel<CultivationNpcEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "artifact_refiner"), "main");

    private final ModelPart root;
    private final ModelPart apron;
    private final ModelPart eyes;
    private final ModelPart hammer;
    private final ModelPart headBand;
    private final ModelPart pauldrons;
    private final ModelPart swordA;
    private final ModelPart swordB;

    public ArtifactRefinerModel(ModelPart root) {
        super(root);
        this.root = root;
        this.apron = root.getChild("body").getChild("apron");
        this.eyes = root.getChild("head").getChild("eyes");
        this.hammer = root.getChild("right_arm").getChild("hammer");
        this.headBand = root.getChild("head").getChild("head_band");
        this.pauldrons = root.getChild("body").getChild("pauldrons");
        this.swordA = root.getChild("body").getChild("sword_a");
        this.swordB = root.getChild("body").getChild("sword_b");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition apron = root.getChild("body").addOrReplaceChild("apron",
CubeListBuilder.create()
        .texOffs(0, 56).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 7.0F, 1.0F, NONE)
        .texOffs(6, 41).addBox(-2.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F, NONE)
        .texOffs(10, 41).addBox(1.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 4.0F, -2.0F));
        PartDefinition eyes = root.getChild("head").addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(56, 30).addBox(-2.0F, -5.0F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(60, 30).addBox(1.0F, -5.0F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition hammer = root.getChild("right_arm").addOrReplaceChild("hammer",
CubeListBuilder.create()
        .texOffs(6, 32).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, NONE)
        .texOffs(36, 32).addBox(-3.0F, 7.0F, -1.5F, 4.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(-1.0F, 10.0F, 0.0F));
        PartDefinition headBand = root.getChild("head").addOrReplaceChild("head_band",
CubeListBuilder.create()
        .texOffs(0, 46).addBox(-4.5F, -6.0F, -4.5F, 9.0F, 1.0F, 9.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition pauldrons = root.getChild("body").addOrReplaceChild("pauldrons",
CubeListBuilder.create()
        .texOffs(14, 56).addBox(-9.0F, -1.0F, -2.5F, 5.0F, 2.0F, 5.0F, NONE)
        .texOffs(34, 56).addBox(4.0F, -1.0F, -2.5F, 5.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition swordA = root.getChild("body").addOrReplaceChild("sword_a",
CubeListBuilder.create()
        .texOffs(56, 16).addBox(-1.0F, -9.0F, -0.5F, 2.0F, 13.0F, 1.0F, NONE)
        .texOffs(14, 32).addBox(-0.5F, 4.0F, -0.5F, 1.0F, 3.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 5.0F, 2.6F, 0.0F, 0.0F, -0.8F));
        PartDefinition swordB = root.getChild("body").addOrReplaceChild("sword_b",
CubeListBuilder.create()
        .texOffs(0, 32).addBox(-1.0F, -9.0F, -0.5F, 2.0F, 13.0F, 1.0F, NONE)
        .texOffs(14, 36).addBox(-0.5F, 4.0F, -0.5F, 1.0F, 3.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 5.0F, 2.6F, 0.0F, 0.0F, 0.8F));
        return LayerDefinition.create(mesh, 64, 64);
    }


    @Override
    public void setupAnim(CultivationNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 动作总览：
        //   idle   —— 呼吸；每 200 tick 抡一下铁锤（抬 6 → 砸 4 → 收势 8 tick）
        //   walk   —— 握锤小摆（不走原版大摆臂）+ 背后剑胚随步晃动 + 围裙晃
        //   talk   —— 每 240 tick 抬左手示意一趟（NPC 没有状态机，计时器当 special）
        //   hurt   —— 缩肩、头往下压
        //   death  —— 松锤垂臂
        // 全部"先取本帧 super 算好的基准再绝对赋值"，没有任何 += / *=（历史事故原因）。
        boolean walking = limbSwingAmount > 0.04F;
        float t = ageInTicks;

        // ---- 1. 头：站着慢慢点头；围裙随呼吸/步伐轻晃 ---------------------
        float breath = walking ? 0.0F : Mth.sin(t * 0.055F);
        this.head.xRot = this.head.xRot + (walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.03F * limbSwingAmount
                : breath * 0.035F);
        this.apron.xRot = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.03F * limbSwingAmount
                : 0.02F + breath * 0.02F;

        // ---- 2. 背后两柄剑胚：交叉角固定，随步伐/呼吸轻晃 ------------------
        float swordSway = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.04F * limbSwingAmount
                : breath * 0.02F;
        this.swordA.zRot = -SWORD_BASE_ZROT + swordSway;
        this.swordB.zRot = SWORD_BASE_ZROT + swordSway;

        // ---- 3. 右手铁锤：idle 每 200 tick 抡一下；走路改成握锤小摆 --------
        float phase = t % 200.0F;
        float tap;
        if (phase < 6.0F) {
            tap = -0.95F * (phase / 6.0F);                       // 抬起
        } else if (phase < 10.0F) {
            tap = -0.95F + 1.55F * ((phase - 6.0F) / 4.0F);      // 下砸
        } else if (phase < 18.0F) {
            tap = 0.60F * (1.0F - (phase - 10.0F) / 8.0F);       // 收势回位
        } else {
            tap = 0.0F;
        }
        this.rightArm.xRot = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.30F * limbSwingAmount
                : tap;
        this.rightArm.zRot = -0.10F;
        this.rightArm.yRot = 0.0F;
        // 锤子跟手：砸下去时锤头略朝前，抬起时回正（绝对赋值）
        this.hammer.xRot = walking ? 0.12F : -0.55F * tap;

        // ---- 4. 左臂：招呼时整条抬起来示意 ---------------------------------
        float leftArmBaseX = this.leftArm.xRot;
        float leftArmBaseZ = this.leftArm.zRot;
        boolean hailing = (t % 240.0F) < 40.0F && !walking;
        if (hailing) {
            leftArmBaseX = -2.10F + Mth.sin(t * 0.40F) * 0.14F;
            leftArmBaseZ = 0.24F;
        }
        this.leftArm.xRot = leftArmBaseX;
        this.leftArm.zRot = leftArmBaseZ;

        // ---- 5. hurt：缩肩、头往下压 ---------------------------------------
        if (entity.hurtTime > 0) {
            float hurt = 1.0F - entity.hurtTime / 10.0F;
            this.head.xRot = this.head.xRot + 0.28F * hurt;
            this.rightArm.xRot = this.rightArm.xRot + 0.35F * hurt;
        }

        // ---- 6. death：松锤垂臂（倒地旋转交给原版渲染器）-------------------
        if (entity.isDeadOrDying()) {
            this.rightArm.xRot = 0.95F;
            this.rightArm.zRot = -0.55F;
            this.leftArm.xRot = leftArmBaseX - 0.85F;
            this.leftArm.zRot = leftArmBaseZ + 0.45F;
            this.hammer.xRot = 0.60F;
        }

    }


    /** 模型最低点（脚底）= 24。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 最高点：头顶 -8（透明 hat 层外扩到 -8.5，不算可见高度）→ 可见总高 32px。 */
    public static final int HIGHEST_PIXEL_Y = -8;

    /** 背后两柄剑胚的基准交叉角（rad）：动画里要用它做绝对基准。 */
    public static final float SWORD_BASE_ZROT = 0.80F;

}
