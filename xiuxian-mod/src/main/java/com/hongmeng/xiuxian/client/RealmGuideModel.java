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
 * 秘境向导 · RealmGuide / RealmGuideModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>清瘦风尘：窄檐旧草斗笠 + 背后竹卷轴筒（露出两卷地图轴头）+ 左手 2 格高路引竹竿（竿顶挂路引纸、竿底拄地）+ 右腰水囊；唯一'高杆+旗'剪影。</p>
 */
public class RealmGuideModel extends HumanoidModel<CultivationNpcEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "realm_guide"), "main");

    private final ModelPart root;
    private final ModelPart narrowHat;
    private final ModelPart roadPole;
    private final ModelPart scrollTube;
    private final ModelPart tubeStrap;
    private final ModelPart waterskin;

    public RealmGuideModel(ModelPart root) {
        super(root);
        this.root = root;
        this.narrowHat = root.getChild("head").getChild("narrow_hat");
        this.roadPole = root.getChild("left_arm").getChild("road_pole");
        this.scrollTube = root.getChild("body").getChild("scroll_tube");
        this.tubeStrap = root.getChild("body").getChild("tube_strap");
        this.waterskin = root.getChild("body").getChild("waterskin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition narrowHat = root.getChild("head").addOrReplaceChild("narrow_hat",
CubeListBuilder.create()
        .texOffs(12, 50).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 1.0F, 9.0F, NONE)
        .texOffs(16, 32).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 2.0F, 6.0F, NONE)
        .texOffs(12, 60).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 1.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -6.0F, 0.0F));
        PartDefinition roadPole = root.getChild("left_arm").addOrReplaceChild("road_pole",
CubeListBuilder.create()
        .texOffs(8, 32).addBox(3.0F, -22.0F, -3.0F, 2.0F, 16.0F, 2.0F, NONE)
        .texOffs(0, 32).addBox(3.0F, -6.0F, -3.0F, 2.0F, 18.0F, 2.0F, NONE)
        .texOffs(56, 16).addBox(3.0F, -22.0F, -4.0F, 2.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(-1.0F, 10.0F, 0.0F));
        PartDefinition scrollTube = root.getChild("body").addOrReplaceChild("scroll_tube",
CubeListBuilder.create()
        .texOffs(0, 52).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F, NONE)
        .texOffs(56, 22).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(56, 26).addBox(-1.0F, 8.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 3.0F, 2.6F, 0.0F, 0.0F, 0.1F));
        PartDefinition tubeStrap = root.getChild("body").addOrReplaceChild("tube_strap",
CubeListBuilder.create()
        .texOffs(40, 32).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(1.0F, 3.0F, -2.2F, 0.0F, 0.0F, -0.6F));
        PartDefinition waterskin = root.getChild("body").addOrReplaceChild("waterskin",
CubeListBuilder.create()
        .texOffs(44, 32).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(-2.5F, 7.0F, -2.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }


    @Override
    public void setupAnim(CultivationNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 动作总览：
        //   idle   —— 点头、窄檐斗笠轻晃、卷轴筒/水囊轻摆
        //   walk   —— 左臂摆但竹竿反向补偿保持近竖直（拄竿走）；卷轴筒随步晃
        //   talk   —— 每 260 tick 举竿指路一趟（竿从竖直摆到前指）
        //   hurt   —— 缩肩、头往下压
        //   death  —— 脱手弃竿
        // 全部"先取本帧 super 算好的基准再绝对赋值"，没有任何 += / *=。
        boolean walking = limbSwingAmount > 0.04F;
        float t = ageInTicks;
        float breath = walking ? 0.0F : Mth.sin(t * 0.05F);

        // ---- 1. 头 + 窄檐斗笠 ---------------------------------------------
        this.head.xRot = this.head.xRot + (walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.03F * limbSwingAmount
                : breath * 0.032F);
        this.narrowHat.xRot = 0.02F + (walking
                ? Mth.cos(limbSwing * 1.3324F) * 0.02F * limbSwingAmount
                : breath * 0.02F);
        this.narrowHat.zRot = Mth.sin(t * 0.033F) * 0.03F;

        // ---- 2. 背后卷轴筒 / 背带 / 水囊：随步伐与呼吸轻晃 -----------------
        float sway = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.05F * limbSwingAmount
                : breath * 0.02F;
        this.scrollTube.zRot = TUBE_BASE_ZROT + sway;
        this.tubeStrap.xRot = sway * 0.4F;
        this.waterskin.xRot = sway * 0.8F;

        // ---- 3. 左臂与路引竹竿：抬竿指路是 special，其余时间反向补偿 ------
        float leftArmBaseX = this.leftArm.xRot;
        float leftArmBaseZ = this.leftArm.zRot;
        boolean pointing = (t % 260.0F) < 55.0F && !walking;
        if (pointing) {
            leftArmBaseX = -1.45F + Mth.sin(t * 0.25F) * 0.12F;
            leftArmBaseZ = 0.10F;
        }
        this.leftArm.xRot = leftArmBaseX;
        this.leftArm.zRot = leftArmBaseZ;
        // 竹竿：平时保持近竖直（拄竿），指路时跟着手臂往前压成斜指
        this.roadPole.xRot = -leftArmBaseX * 0.85F;
        this.roadPole.zRot = -leftArmBaseZ * 0.85F + 0.03F;

        // ---- 4. hurt：缩肩、头往下压 ---------------------------------------
        if (entity.hurtTime > 0) {
            float hurt = 1.0F - entity.hurtTime / 10.0F;
            this.head.xRot = this.head.xRot + 0.28F * hurt;
            this.scrollTube.zRot = this.scrollTube.zRot + 0.12F * hurt;
        }

        // ---- 5. death：脱手弃竿（倒地旋转交给原版渲染器）-------------------
        if (entity.isDeadOrDying()) {
            this.leftArm.xRot = leftArmBaseX + 0.55F;
            this.leftArm.zRot = leftArmBaseZ - 0.55F;
            this.roadPole.xRot = 1.35F;
            this.roadPole.zRot = 0.35F;
        }

    }


    /** 模型最低点（脚底 / 竹竿底）= 24。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 最高点：竹竿顶 -10（斗笠尖 -9）→ 总高 34px。 */
    public static final int HIGHEST_PIXEL_Y = -10;

    /** 卷轴筒的基准斜度（rad）：动画里做绝对基准。 */
    public static final float TUBE_BASE_ZROT = 0.12F;

}
