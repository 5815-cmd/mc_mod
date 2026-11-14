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
 * 云游商人 · WanderingMerchant / WanderingMerchantModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>斗笠宽檐压眉 + 一根竹扁担挑前后两只竹筐（卷轴/丹葫芦露头）+ 胸前斜挎褡裢 + 右手竹杖 + 右腰钱袋；横担剪影是 7 位 NPC 里唯一的。</p>
 */
public class WanderingMerchantModel extends HumanoidModel<CultivationNpcEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "wandering_merchant"), "main");

    private final ModelPart root;
    private final ModelPart basketBack;
    private final ModelPart basketFront;
    private final ModelPart beard;
    private final ModelPart cane;
    private final ModelPart coinPouch;
    private final ModelPart eyes;
    private final ModelPart satchel;
    private final ModelPart satchelStrap;
    private final ModelPart shoulderPole;
    private final ModelPart strawHat;

    public WanderingMerchantModel(ModelPart root) {
        super(root);
        this.root = root;
        this.basketBack = root.getChild("body").getChild("basket_back");
        this.basketFront = root.getChild("body").getChild("basket_front");
        this.beard = root.getChild("head").getChild("beard");
        this.cane = root.getChild("right_arm").getChild("cane");
        this.coinPouch = root.getChild("body").getChild("coin_pouch");
        this.eyes = root.getChild("head").getChild("eyes");
        this.satchel = root.getChild("body").getChild("satchel");
        this.satchelStrap = root.getChild("body").getChild("satchel_strap");
        this.shoulderPole = root.getChild("body").getChild("shoulder_pole");
        this.strawHat = root.getChild("head").getChild("straw_hat");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition basketBack = root.getChild("body").addOrReplaceChild("basket_back",
CubeListBuilder.create()
        .texOffs(44, 60).addBox(-0.5F, 0.0F, 2.5F, 1.0F, 3.0F, 1.0F, NONE)
        .texOffs(22, 54).addBox(-2.5F, 3.0F, 2.0F, 5.0F, 3.0F, 4.0F, NONE)
        .texOffs(46, 44).addBox(-1.0F, 4.0F, 3.0F, 2.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(-4.5F, 0.0F, 2.0F));
        PartDefinition basketFront = root.getChild("body").addOrReplaceChild("basket_front",
CubeListBuilder.create()
        .texOffs(40, 60).addBox(-0.5F, 0.0F, -3.5F, 1.0F, 3.0F, 1.0F, NONE)
        .texOffs(4, 54).addBox(-2.5F, 3.0F, -6.0F, 5.0F, 3.0F, 4.0F, NONE)
        .texOffs(58, 32).addBox(-1.0F, 4.0F, -5.5F, 1.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(-4.5F, 0.0F, -2.0F));
        PartDefinition beard = root.getChild("head").addOrReplaceChild("beard",
CubeListBuilder.create()
        .texOffs(4, 61).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, -3.5F));
        PartDefinition cane = root.getChild("right_arm").addOrReplaceChild("cane",
CubeListBuilder.create()
        .texOffs(56, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F, NONE)
                , PartPose.offset(-1.0F, 10.0F, 0.0F));
        PartDefinition coinPouch = root.getChild("body").addOrReplaceChild("coin_pouch",
CubeListBuilder.create()
        .texOffs(40, 56).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(-2.5F, 7.0F, -2.5F));
        PartDefinition eyes = root.getChild("head").addOrReplaceChild("eyes",
CubeListBuilder.create()
        .texOffs(58, 37).addBox(-2.0F, -5.0F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(46, 49).addBox(1.0F, -5.0F, -4.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition satchel = root.getChild("body").addOrReplaceChild("satchel",
CubeListBuilder.create()
        .texOffs(40, 51).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(3.0F, 7.0F, -2.4F));
        PartDefinition satchelStrap = root.getChild("body").addOrReplaceChild("satchel_strap",
CubeListBuilder.create()
        .texOffs(0, 54).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(2.0F, 8.0F, -2.2F, 0.0F, 0.0F, -0.6F));
        PartDefinition shoulderPole = root.getChild("body").addOrReplaceChild("shoulder_pole",
CubeListBuilder.create()
        .texOffs(32, 44).addBox(-1.0F, -2.0F, -5.0F, 2.0F, 2.0F, 5.0F, NONE)
        .texOffs(44, 32).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 5.0F, NONE)
                , PartPose.offset(-4.5F, 0.0F, 0.0F));
        PartDefinition strawHat = root.getChild("head").addOrReplaceChild("straw_hat",
CubeListBuilder.create()
        .texOffs(0, 32).addBox(-5.5F, 0.0F, -5.5F, 11.0F, 1.0F, 11.0F, NONE)
        .texOffs(0, 44).addBox(-4.0F, -2.0F, -4.0F, 8.0F, 2.0F, 8.0F, NONE)
        .texOffs(44, 39).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 1.0F, 4.0F, NONE)
        .texOffs(32, 51).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 1.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -6.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }


    @Override
    public void setupAnim(CultivationNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 动作总览：
        //   idle   —— 拄杖站着：慢慢点头、斗笠压檐轻晃、货担轻颤、短须飘
        //   walk   —— 扁担随步伐上下弹（双步频）+ 与迈出的腿反相的翘动，两只筐滞后摆
        //   talk   —— 每 11 秒抬手招呼一趟（NPC 没有状态机，用计时器当 special）
        //   hurt   —— 缩肩、担子往上一缩
        //   death  —— 松手卸担
        // 全部"先取本帧 super 算好的基准再绝对赋值"，没有任何 += / *=（历史事故原因）。
        boolean walking = limbSwingAmount > 0.04F;

        // ---- 1. 头：站着慢慢点头；走路随步频轻点 -------------------------
        float nod = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.045F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.045F) * 0.035F;
        this.head.xRot = this.head.xRot + nod;

        // ---- 2. 斗笠 / 短须：压到眉骨后随动作轻晃（部件自身旋转）----------
        this.strawHat.xRot = 0.02F + Mth.cos(ageInTicks * 0.04F) * 0.02F;
        this.strawHat.zRot = Mth.sin(ageInTicks * 0.033F) * 0.025F;
        this.beard.xRot = 0.08F + Mth.cos(ageInTicks * 0.05F) * 0.03F;

        // ---- 3. 右手拄杖：覆盖原版摆臂，改成"握杖前伸 + 小晃" -------------
        this.rightArm.xRot = -0.16F + (walking
                ? Mth.cos(limbSwing * 1.3324F) * 0.05F * limbSwingAmount : 0.0F);
        this.rightArm.zRot = -0.06F;
        this.rightArm.yRot = 0.0F;
        // 竹杖反向补偿，保持近竖直（杖底仍在 y≈24 的地面上）
        this.cane.xRot = -this.rightArm.xRot * 0.9F;
        this.cane.zRot = -this.rightArm.zRot * 0.9F + 0.04F;

        // ---- 4. 扁担 + 两只货筐：走一步弹两下（双步频），筐比担子慢半拍 ----
        float bounce = walking
                ? Mth.cos(limbSwing * 1.3324F) * 1.10F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.07F) * 0.12F;
        float swing = walking
                ? Mth.cos(limbSwing * 1.3324F - 1.0F) * 0.10F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.05F) * 0.02F;
        this.shoulderPole.y = CARRY_BASE_Y + bounce;
        this.shoulderPole.xRot = walking
                ? -Mth.cos(limbSwing * 0.6662F) * 0.05F * limbSwingAmount : 0.0F;
        this.shoulderPole.zRot = Mth.cos(ageInTicks * 0.05F) * 0.02F;
        this.basketFront.y = CARRY_BASE_Y + bounce;
        this.basketBack.y = CARRY_BASE_Y + bounce;
        this.basketFront.xRot = swing;
        this.basketBack.xRot = -swing * 0.85F;

        // ---- 5. 褡裢 / 钱袋：跟着躯干晃（绝对赋值，不累加）----------------
        float sway = walking
                ? Mth.cos(limbSwing * 0.6662F) * 0.05F * limbSwingAmount
                : Mth.sin(ageInTicks * 0.05F) * 0.02F;
        this.satchel.xRot = sway;
        this.satchelStrap.xRot = sway * 0.5F;
        this.coinPouch.xRot = sway * 0.8F;

        // ---- 6. 左臂：默认走原版摆臂；招呼时整条手臂抬起来摇 --------------
        float leftArmBaseX = this.leftArm.xRot;
        float leftArmBaseZ = this.leftArm.zRot;
        // 招呼周期 220 tick（11 秒），其中前 45 tick 抬手（NPC 没有状态机，
        // 这是"揽客"的 special / talk 动作）
        boolean hailing = (ageInTicks % 220.0F) < 45.0F && !walking;
        if (hailing) {
            leftArmBaseX = -2.30F + Mth.sin(ageInTicks * 0.42F) * 0.16F;
            leftArmBaseZ = 0.20F;
        }
        this.leftArm.xRot = leftArmBaseX;
        this.leftArm.zRot = leftArmBaseZ;

        // ---- 7. hurt：缩肩，担子被顶起来一下 ------------------------------
        if (entity.hurtTime > 0) {
            float hurt = 1.0F - entity.hurtTime / 10.0F;
            this.head.xRot = this.head.xRot + 0.30F * hurt;
            this.shoulderPole.y = this.shoulderPole.y + 0.9F * hurt;
            this.basketFront.y = CARRY_BASE_Y + (this.shoulderPole.y - CARRY_BASE_Y);
            this.basketBack.y = this.basketFront.y;
        }

        // ---- 8. death：松手卸担（原版渲染器负责倒地旋转，这里只放软姿态）----
        if (entity.isDeadOrDying()) {
            this.rightArm.xRot = -0.16F - 0.85F;
            this.rightArm.zRot = -0.06F - 0.45F;
            this.leftArm.xRot = leftArmBaseX - 0.85F;
            this.leftArm.zRot = leftArmBaseZ + 0.45F;
            this.shoulderPole.y = CARRY_BASE_Y - 0.7F;
            this.basketFront.y = this.shoulderPole.y;
            this.basketBack.y = this.shoulderPole.y;
        }

    }


    /** 扁担/货筐这一族的基准 y：三个部件的 pivot 都落在肩线 y=0 上。 */
    public static final float CARRY_BASE_Y = 0.0F;

    /** 模型最低点（脚底）= 24；给离地审计/文档用。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 斗笠尖 = -10（原版头顶 -8），模型总高 34px。 */
    public static final int HIGHEST_PIXEL_Y = -10;

}
