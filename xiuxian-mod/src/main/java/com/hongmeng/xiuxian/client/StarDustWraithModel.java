package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.StarDustWraithEntity;
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
 * 星尘游魂 · Star-dust Wraith / StarDustWraithModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>星尘聚合体：无腿人形轮廓 + 3 条星尘尾 + 胸口大星核 + 头顶 3 颗悬浮星点；飘浮。</p>
 */
public class StarDustWraithModel extends EntityModel<StarDustWraithEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "star_dust_wraith"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart core;
    private final ModelPart handL;
    private final ModelPart handR;
    private final ModelPart head;
    private final ModelPart shoulderL;
    private final ModelPart shoulderR;
    private final ModelPart tailL;
    private final ModelPart tailM;
    private final ModelPart tailR;
    private final ModelPart torso;
    private final ModelPart waist;
    private final ModelPart starL;
    private final ModelPart starM;
    private final ModelPart starR;

    public StarDustWraithModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.armL = root.getChild("body").getChild("arm_l");
        this.armR = root.getChild("body").getChild("arm_r");
        this.core = root.getChild("body").getChild("core");
        this.handL = root.getChild("body").getChild("hand_l");
        this.handR = root.getChild("body").getChild("hand_r");
        this.head = root.getChild("body").getChild("head");
        this.shoulderL = root.getChild("body").getChild("shoulder_l");
        this.shoulderR = root.getChild("body").getChild("shoulder_r");
        this.tailL = root.getChild("body").getChild("tail_l");
        this.tailM = root.getChild("body").getChild("tail_m");
        this.tailR = root.getChild("body").getChild("tail_r");
        this.torso = root.getChild("body").getChild("torso");
        this.waist = root.getChild("body").getChild("waist");
        this.starL = root.getChild("body").getChild("head").getChild("star_l");
        this.starM = root.getChild("body").getChild("head").getChild("star_m");
        this.starR = root.getChild("body").getChild("head").getChild("star_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-2.0F, -22.0F, -2.5F, 4.0F, 20.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition armL = body.addOrReplaceChild("arm_l",
CubeListBuilder.create()
        .texOffs(40, 13).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, NONE)
                , PartPose.offset(3.0F, -16.0F, 0.0F));
        PartDefinition armR = body.addOrReplaceChild("arm_r",
CubeListBuilder.create()
        .texOffs(49, 13).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, NONE)
                , PartPose.offset(-3.0F, -16.0F, 0.0F));
        PartDefinition core = body.addOrReplaceChild("core",
CubeListBuilder.create()
        .texOffs(18, 28).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -15.0F, -2.6F));
        PartDefinition handL = body.addOrReplaceChild("hand_l",
CubeListBuilder.create()
        .texOffs(0, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(3.0F, -9.0F, 0.0F));
        PartDefinition handR = body.addOrReplaceChild("hand_r",
CubeListBuilder.create()
        .texOffs(9, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(-3.0F, -9.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(19, 0).addBox(-2.5F, -8.0F, -2.5F, 5.0F, 8.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, -22.0F, 0.0F));
        PartDefinition shoulderL = body.addOrReplaceChild("shoulder_l",
CubeListBuilder.create()
        .texOffs(43, 23).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(3.0F, -17.0F, 0.0F));
        PartDefinition shoulderR = body.addOrReplaceChild("shoulder_r",
CubeListBuilder.create()
        .texOffs(54, 23).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(-3.0F, -17.0F, 0.0F));
        PartDefinition tailL = body.addOrReplaceChild("tail_l",
CubeListBuilder.create()
        .texOffs(19, 14).addBox(-3.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition tailM = body.addOrReplaceChild("tail_m",
CubeListBuilder.create()
        .texOffs(28, 14).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition tailR = body.addOrReplaceChild("tail_r",
CubeListBuilder.create()
        .texOffs(19, 21).addBox(1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition torso = body.addOrReplaceChild("torso",
CubeListBuilder.create()
        .texOffs(40, 0).addBox(-3.0F, -8.0F, -2.0F, 6.0F, 8.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, -10.0F, 0.0F));
        PartDefinition waist = body.addOrReplaceChild("waist",
CubeListBuilder.create()
        .texOffs(28, 23).addBox(-2.0F, -2.0F, -1.5F, 4.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition starL = head.addOrReplaceChild("star_l",
CubeListBuilder.create()
        .texOffs(58, 13).addBox(-2.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -10.0F, 0.0F));
        PartDefinition starM = head.addOrReplaceChild("star_m",
CubeListBuilder.create()
        .texOffs(58, 16).addBox(0.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -10.0F, 0.0F));
        PartDefinition starR = head.addOrReplaceChild("star_r",
CubeListBuilder.create()
        .texOffs(58, 19).addBox(2.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -10.0F, 0.0F));
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
    public void setupAnim(StarDustWraithEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        final float DEG = 0.017453292F;
        float t = ageInTicks;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float atk = entity.getAttackAnim(0.0F);                       // 一次挥击 0..1
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);
        int burstLeft = entity.burstTicks();                          // 星尘爆发 40→0
        boolean bursting = burstLeft > 0;
        float burst = bursting
                ? Mth.clamp(1.0F - (float) burstLeft / 40.0F, 0.0F, 1.0F) : 0.0F;
        float burstPulse = bursting ? Mth.sin(burst * (float) Math.PI) : 0.0F;

        // ================================================================
        // 1. 飘浮基准：整体离地 0~1px 起落（静态模型最低点仍是 y=24）
        // ================================================================
        float lift = 0.5F + 0.5F * Mth.sin(t * 0.05F)
                + walk * 0.5F * Mth.sin(limbSwing * 1.3324F);
        this.body.y = 24.0F - lift;
        this.body.xRot = 0.04F * walk - 0.10F * hurt + 0.06F * burstPulse;
        this.body.zRot = 0.02F * Mth.sin(t * 0.031F);

        // 呼吸/溃散/死亡塌陷：全部用缩放（不动 y）
        float breath = 1.0F + Mth.sin(t * 0.06F) * 0.025F;
        float squash = breath * (1.0F - 0.08F * hurt) * (1.0F + 0.10F * dead);
        this.body.xScale = squash;
        this.body.zScale = squash;

        // ================================================================
        // 2. 头：随视线缓慢转；walk 时朝移动方向偏 0.12；死时垂下
        // ================================================================
        this.head.xRot = pitch * 0.50F + 0.10F * hurt + 0.55F * dead;
        this.head.yRot = yaw * 0.60F + 0.12F * walk;
        this.head.zRot = 0.02F * Mth.sin(t * 0.037F) + 0.10F * hurt;

        // ================================================================
        // 3. 3 条星尘尾：idle 慢波 → walk 行波 → hurt 溃散 → death 断开下沉
        // ================================================================
        float waveA = Mth.sin(t * 0.07F);
        float waveB = Mth.sin(t * 0.07F + 2.094F);
        float waveC = Mth.sin(t * 0.07F + 4.189F);
        float walkWave = Mth.sin(limbSwing * 0.6662F);
        this.tailL.xRot = waveA * 0.10F - 0.10F * walk;
        this.tailM.xRot = waveB * 0.10F - 0.14F * walk;
        this.tailR.xRot = waveC * 0.10F - 0.10F * walk;
        this.tailL.zRot = (walk * 0.12F * walkWave + 0.20F * hurt) * -1.0F;
        this.tailM.zRot = walk * 0.12F * Mth.sin(limbSwing * 0.6662F + 1.047F);
        this.tailR.zRot = walk * 0.12F * Mth.sin(limbSwing * 0.6662F + 2.094F) + 0.20F * hurt;
        this.tailL.y = -4.0F - 2.0F * dead;
        this.tailM.y = -4.0F - 2.6F * dead;
        this.tailR.y = -4.0F - 2.0F * dead;

        // ================================================================
        // 4. 双臂：walk 反向轻摆 → attack 抬右掌前推 → 爆发时双臂张开 → death 垂下
        // ================================================================
        float raise = Mth.sin(atk * (float) Math.PI);                 // 0→1→0
        this.armL.xRot = -0.08F * walk + 0.20F * hurt + 0.30F * dead;
        this.armR.xRot = 0.08F * walk - 0.90F * raise + 0.20F * hurt + 0.30F * dead;
        this.armL.zRot = -1.10F * burstPulse;
        this.armR.zRot = 1.10F * burstPulse;
        this.handL.xRot = -0.20F * raise;
        this.handR.xRot = -0.55F * raise + 0.15F * burstPulse;
        this.handL.zRot = this.armL.zRot * 0.5F;
        this.handR.zRot = this.armR.zRot * 0.5F;
        this.shoulderL.zRot = this.armL.zRot * 0.4F;
        this.shoulderR.zRot = this.armR.zRot * 0.4F;

        // ================================================================
        // 5. 胸口星核：idle 脉冲（1.0→1.15）→ 攻击/爆发更亮 → 死亡熄灭
        // ================================================================
        float corePulse = 1.0F + Mth.sin(t * 0.09F) * 0.12F
                + 0.30F * raise + 0.60F * burstPulse + 0.15F * hurt;
        float coreScale = corePulse * (1.0F - 0.60F * dead);
        this.core.xScale = coreScale;
        this.core.zScale = coreScale;
        this.core.yScale = coreScale;

        // ================================================================
        // 6. 头顶 3 颗星点：±1px 错相浮沉；爆发时绕头公转；死亡时四散
        // ================================================================
        float orbit = burst * 12.566F;                                // 0→2π
        this.starL.y = -10.0F - 1.0F * Mth.sin(t * 0.08F) - 1.5F * dead;
        this.starM.y = -10.0F - 1.0F * Mth.sin(t * 0.08F + 2.094F) - 2.0F * dead;
        this.starR.y = -10.0F - 1.0F * Mth.sin(t * 0.08F + 4.189F) - 1.5F * dead;
        this.starL.yRot = orbit;
        this.starM.yRot = orbit;
        this.starR.yRot = orbit;
        this.starL.zRot = 0.10F * dead;
        this.starM.zRot = -0.10F * dead;
        this.starR.zRot = 0.10F * dead;

        // ================================================================
        // 7. 腰带：跟着躯干（父件），只补一点随呼吸的外扩
        // ================================================================
        this.waist.xScale = 1.0F + Mth.sin(t * 0.06F) * 0.02F;
        this.waist.zScale = this.waist.xScale;

    }


    /** 模型最低点像素 y（3 条星尘尾尾端触地）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 最高点像素 y（中央那颗悬浮星点）。模型总高 33px。 */
    public static final int HIGHEST_PIXEL_Y = -9;

}
