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
 * 门派长老 · SectElder / SectElderModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>长白须垂胸（8px 两段）+ 右手白马鬃拂尘（竹柄） + 背后斜挎无锋木剑；月白素袍 + 灰蓝腰带交领，清瘦老者剪影。</p>
 */
public class SectElderModel extends HumanoidModel<CultivationNpcEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "sect_elder"), "main");

    private final ModelPart root;
    private final ModelPart beard;
    private final ModelPart brows;
    private final ModelPart eyes;
    private final ModelPart lapelL;
    private final ModelPart lapelR;
    private final ModelPart sash;
    private final ModelPart topknot;
    private final ModelPart whisk;
    private final ModelPart woodSword;

    public SectElderModel(ModelPart root) {
        super(root);
        this.root = root;
        this.beard = root.getChild("head").getChild("beard");
        this.brows = root.getChild("head").getChild("brows");
        this.eyes = root.getChild("head").getChild("eyes");
        this.lapelL = root.getChild("body").getChild("lapel_l");
        this.lapelR = root.getChild("body").getChild("lapel_r");
        this.sash = root.getChild("body").getChild("sash");
        this.topknot = root.getChild("head").getChild("topknot");
        this.whisk = root.getChild("right_arm").getChild("whisk");
        this.woodSword = root.getChild("body").getChild("wood_sword");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition beard = root.getChild("head").addOrReplaceChild("beard",
CubeListBuilder.create()
        .texOffs(17, 32).addBox(-1.5F, 0.0F, -1.4F, 3.0F, 5.0F, 2.0F, NONE)
        .texOffs(33, 39).addBox(-2.0F, 4.5F, -1.6F, 4.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -3.0F, -3.4F));
        PartDefinition brows = root.getChild("head").addOrReplaceChild("brows",
CubeListBuilder.create()
        .texOffs(9, 41).addBox(-3.0F, -6.4F, -4.6F, 2.0F, 1.0F, 1.0F, NONE)
        .texOffs(11, 46).addBox(1.0F, -6.4F, -4.6F, 2.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyes = root.getChild("head").addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(18, 46).addBox(-2.0F, -5.2F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(23, 46).addBox(1.0F, -5.2F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition lapelL = root.getChild("body").addOrReplaceChild("lapel_l",
CubeListBuilder.create()
        .texOffs(51, 39).addBox(-0.5F, 0.0F, -0.4F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 0.5F, -2.5F, 0.0F, 0.0F, 0.6F));
        PartDefinition lapelR = root.getChild("body").addOrReplaceChild("lapel_r",
CubeListBuilder.create()
        .texOffs(46, 39).addBox(-0.5F, 0.0F, -0.4F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 0.5F, -2.4F, 0.0F, 0.0F, -0.6F));
        PartDefinition sash = root.getChild("body").addOrReplaceChild("sash",
CubeListBuilder.create()
        .texOffs(33, 32).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 1.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 9.0F, 0.0F));
        PartDefinition topknot = root.getChild("head").addOrReplaceChild("topknot",
CubeListBuilder.create()
        .texOffs(17, 40).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition whisk = root.getChild("right_arm").addOrReplaceChild("whisk",
CubeListBuilder.create()
        .texOffs(28, 32).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
        .texOffs(56, 45).addBox(-1.0F, 5.0F, -1.0F, 2.0F, 1.0F, 2.0F, NONE)
        .texOffs(0, 41).addBox(-1.0F, 6.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(0, 32).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
                , PartPose.offset(-1.6F, 6.0F, 0.0F));
        PartDefinition woodSword = root.getChild("body").addOrReplaceChild("wood_sword",
CubeListBuilder.create()
        .texOffs(56, 39).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F, NONE)
        .texOffs(0, 46).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 1.0F, 1.0F, NONE)
        .texOffs(56, 16).addBox(-1.0F, 1.0F, -0.5F, 2.0F, 14.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-3.0F, 0.0F, 2.3F, 0.0F, 0.0F, -0.6F));
        return LayerDefinition.create(mesh, 64, 64);
    }


    @Override
    public void setupAnim(CultivationNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 动作总览：
        //   idle   —— 慢慢点头、白须飘、拂尘垂摆；每 100t 抬右手捋须一次（30t）
        //   walk   —— 慢步（原版摆臂），拂尘反向补偿保持近垂，木剑随躯干轻晃
        //   hurt   —— 后仰、须尾一抖
        //   death  —— 松手垂臂、微微弓身
        // 所有赋值都是"本帧绝对姿态"，没有任何 += / *=（历史事故原因）。
        boolean walking = limbSwingAmount > 0.04F;

        // ---- 1. 头：站着慢点头；走路随步频轻点 ---------------------------
        float nod = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.04F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.035F) * 0.03F;
        this.head.xRot = this.head.xRot + nod;

        // ---- 2. 长白须 / 白眉 / 道髻：须根 + 须尾反相飘（长者感）---------
        this.beard.xRot = 0.06F + Mth.cos(ageInTicks * 0.045F) * 0.025F;
        this.beard.zRot = Mth.sin(ageInTicks * 0.03F) * 0.02F;
        this.brows.xRot = Mth.sin(ageInTicks * 0.035F) * 0.02F;
        this.eyes.xRot = 0.0F;
        this.topknot.xRot = Mth.sin(ageInTicks * 0.03F) * 0.015F;

        // ---- 3. 背后木剑：斜挎基准 + 轻晃（绝对赋值）--------------------
        this.woodSword.zRot = SWORD_BASE_ZROT + Mth.sin(ageInTicks * 0.03F) * 0.02F;
        this.woodSword.xRot = 0.0F;

        // ---- 4. 右手拂尘：握柄前伸，每 100t 抬起来捋须一次 ---------------
        float rightBaseX = -0.18F + (walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.06F * limbSwingAmount : 0.0F);
        float rightBaseZ = -0.10F;
        boolean stroking = !walking && (ageInTicks % 100.0F) < 30.0F;
        if (stroking) {
            rightBaseX = -0.95F + Mth.sin(ageInTicks * 0.35F) * 0.12F;
            rightBaseZ = -0.28F;
        }
        this.rightArm.xRot = rightBaseX;
        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = rightBaseZ;
        // 拂尘反向补偿，保持近垂直下垂（鬃尾不戳进大腿）
        this.whisk.xRot = -rightBaseX * 0.85F;
        this.whisk.zRot = -rightBaseZ * 0.85F + 0.03F;

        // ---- 5. 腰带 / 两片斜襟：随躯干轻晃（绝对赋值，不累加）-----------
        float sway = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.03F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.05F) * 0.015F;
        this.sash.xRot = sway;
        this.lapelR.xRot = sway * 0.5F;
        this.lapelL.xRot = sway * 0.5F;

        // ---- 6. hurt：后仰 + 须尾一抖 ------------------------------------
        if (entity.hurtTime > 0) {
            float hurt = 1.0F - entity.hurtTime / 10.0F;
            this.head.xRot = this.head.xRot + 0.30F * hurt;
            this.beard.xRot = 0.06F + 0.28F * hurt;
            this.rightArm.xRot = rightBaseX - 0.20F * hurt;
        }

        // ---- 7. death：松手垂臂、弓身（倒地旋转交给原版渲染器）----------
        if (entity.isDeadOrDying()) {
            this.head.xRot = 0.35F;
            this.beard.xRot = 0.40F;
            this.rightArm.xRot = -0.05F;
            this.rightArm.zRot = -0.30F;
            this.leftArm.xRot = -0.05F;
            this.leftArm.zRot = 0.30F;
            this.whisk.xRot = 0.0F;
            this.whisk.zRot = 0.0F;
            this.woodSword.zRot = SWORD_BASE_ZROT - 0.20F;
        }

    }


    /** 模型最低点（脚底）= 24。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 头顶道髻 = -9（原版头顶 -8），模型总高 33px。 */
    public static final int HIGHEST_PIXEL_Y = -9;

    /** 木剑的斜挎基准角（弧度）：动画里用绝对赋值，别在这里累加。 */
    public static final float SWORD_BASE_ZROT = -0.55F;

}
