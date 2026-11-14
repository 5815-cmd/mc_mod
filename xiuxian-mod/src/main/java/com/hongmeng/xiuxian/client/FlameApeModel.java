package com.hongmeng.xiuxian.client;

import com.google.common.collect.ImmutableList;
import com.hongmeng.xiuxian.entity.FlameApeEntity;
import net.minecraft.client.model.ColorableAgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 烈焰猿 · FlameApe / FlameApeModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>驼背长臂熔岩猿：躯干前倾 16°、头低垂陷在双肩之间，前臂极长垂到脚踝、指节撑地；背脊 3 根熔岩骨刺 + 胸腹岩浆裂 + 肩后火焰鬃毛光片。</p>
 */
public class FlameApeModel extends ColorableAgeableListModel<FlameApeEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart thighL;
    private final ModelPart thighR;
    private final ModelPart torso;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart jaw;
    private final ModelPart maneL;
    private final ModelPart maneR;
    private final ModelPart maneTop;
    private final ModelPart shinL;
    private final ModelPart shinR;
    private final ModelPart footL;
    private final ModelPart footR;
    private final ModelPart foreL;
    private final ModelPart foreR;
    private final ModelPart fistL;
    private final ModelPart fistR;

    public FlameApeModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.thighL = root.getChild("thigh_l");
        this.thighR = root.getChild("thigh_r");
        this.torso = root.getChild("torso");
        this.armL = root.getChild("torso").getChild("arm_l");
        this.armR = root.getChild("torso").getChild("arm_r");
        this.jaw = root.getChild("head").getChild("jaw");
        this.maneL = root.getChild("torso").getChild("mane_l");
        this.maneR = root.getChild("torso").getChild("mane_r");
        this.maneTop = root.getChild("torso").getChild("mane_top");
        this.shinL = root.getChild("thigh_l").getChild("shin_l");
        this.shinR = root.getChild("thigh_r").getChild("shin_r");
        this.footL = root.getChild("thigh_l").getChild("shin_l").getChild("foot_l");
        this.footR = root.getChild("thigh_r").getChild("shin_r").getChild("foot_r");
        this.foreL = root.getChild("torso").getChild("arm_l").getChild("fore_l");
        this.foreR = root.getChild("torso").getChild("arm_r").getChild("fore_r");
        this.fistL = root.getChild("torso").getChild("arm_l").getChild("fore_l").getChild("fist_l");
        this.fistR = root.getChild("torso").getChild("arm_r").getChild("fore_r").getChild("fist_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(107, 0).addBox(-4.5F, -8.0F, -7.0F, 9.0F, 8.0F, 9.0F, NONE)
        .texOffs(78, 40).addBox(-4.5F, -8.0F, -8.0F, 9.0F, 2.0F, 2.0F, NONE)
        .texOffs(103, 33).addBox(-3.5F, -4.0F, -9.0F, 7.0F, 3.0F, 2.0F, NONE)
        .texOffs(24, 44).addBox(-3.5F, -7.0F, -8.5F, 3.0F, 2.0F, 1.0F, NONE)
        .texOffs(33, 44).addBox(0.5F, -7.0F, -8.5F, 3.0F, 2.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -3.0F, -6.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition thighL = root.addOrReplaceChild("thigh_l",
CubeListBuilder.create()
        .texOffs(108, 18).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 6.0F, 8.0F, NONE)
                , PartPose.offset(4.0F, 8.0F, 0.0F));
        PartDefinition thighR = root.addOrReplaceChild("thigh_r",
CubeListBuilder.create()
        .texOffs(83, 18).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 6.0F, 8.0F, NONE)
                , PartPose.offset(-4.0F, 8.0F, 0.0F));
        PartDefinition torso = root.addOrReplaceChild("torso",
CubeListBuilder.create()
        .texOffs(0, 24).addBox(-6.0F, -4.0F, -4.5F, 12.0F, 4.0F, 9.0F, NONE)
        .texOffs(144, 0).addBox(-6.5F, -9.0F, -5.0F, 13.0F, 5.0F, 10.0F, NONE)
        .texOffs(58, 0).addBox(-7.0F, -16.0F, -5.0F, 14.0F, 7.0F, 10.0F, NONE)
        .texOffs(226, 0).addBox(-11.0F, -21.0F, -4.0F, 4.0F, 6.0F, 8.0F, NONE)
        .texOffs(226, 15).addBox(7.0F, -21.0F, -4.0F, 4.0F, 6.0F, 8.0F, NONE)
        .texOffs(43, 33).addBox(-6.0F, -22.0F, 3.0F, 12.0F, 5.0F, 5.0F, NONE)
        .texOffs(169, 16).addBox(-1.0F, -16.0F, -6.0F, 2.0F, 8.0F, 1.0F, NONE)
        .texOffs(13, 44).addBox(-2.0F, -7.0F, -6.0F, 4.0F, 2.0F, 1.0F, NONE)
        .texOffs(176, 16).addBox(-1.5F, -26.0F, 2.0F, 3.0F, 5.0F, 3.0F, NONE)
        .texOffs(133, 18).addBox(-1.0F, -29.0F, 2.5F, 2.0F, 3.0F, 2.0F, NONE)
        .texOffs(43, 24).addBox(-6.0F, -26.0F, 2.0F, 3.0F, 4.0F, 3.0F, NONE)
        .texOffs(176, 25).addBox(-5.5F, -28.0F, 2.5F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(156, 31).addBox(3.0F, -26.0F, 2.0F, 3.0F, 4.0F, 3.0F, NONE)
        .texOffs(121, 39).addBox(3.5F, -28.0F, 2.5F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(251, 0).addBox(-0.5F, -26.0F, 1.0F, 1.0F, 3.0F, 1.0F, NONE)
        .texOffs(251, 5).addBox(-4.5F, -26.0F, 1.0F, 1.0F, 3.0F, 1.0F, NONE)
        .texOffs(251, 10).addBox(3.5F, -26.0F, 1.0F, 1.0F, 3.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition armL = torso.addOrReplaceChild("arm_l",
CubeListBuilder.create()
        .texOffs(58, 18).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 8.0F, 6.0F, NONE)
                , PartPose.offsetAndRotation(9.5F, -18.0F, 0.0F, -0.3F, 0.0F, 0.0F));
        PartDefinition armR = torso.addOrReplaceChild("arm_r",
CubeListBuilder.create()
        .texOffs(144, 16).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 8.0F, 6.0F, NONE)
                , PartPose.offsetAndRotation(-9.5F, -18.0F, 0.0F, -0.3F, 0.0F, 0.0F));
        PartDefinition jaw = head.addOrReplaceChild("jaw",
CubeListBuilder.create()
        .texOffs(78, 33).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 2.0F, 4.0F, NONE)
        .texOffs(251, 15).addBox(-3.0F, -2.0F, -3.0F, 1.0F, 2.0F, 1.0F, NONE)
        .texOffs(251, 19).addBox(2.0F, -2.0F, -3.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -2.0F, -7.0F, 0.1F, 0.0F, 0.0F));
        PartDefinition maneL = torso.addOrReplaceChild("mane_l",
CubeListBuilder.create()
        .texOffs(122, 33).addBox(-1.5F, 1.0F, -0.5F, 3.0F, 4.0F, 1.0F, NONE)
        .texOffs(250, 40).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(8.0F, -22.0F, 4.5F, -0.1F, 0.0F, 0.1F));
        PartDefinition maneR = torso.addOrReplaceChild("mane_r",
CubeListBuilder.create()
        .texOffs(133, 24).addBox(-1.5F, 1.0F, -0.5F, 3.0F, 4.0F, 1.0F, NONE)
        .texOffs(169, 26).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-8.0F, -22.0F, 4.5F, -0.1F, 0.0F, -0.1F));
        PartDefinition maneTop = torso.addOrReplaceChild("mane_top",
CubeListBuilder.create()
        .texOffs(0, 44).addBox(-2.5F, 0.0F, -0.5F, 5.0F, 2.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -23.5F, 6.5F, -0.2F, 0.0F, 0.0F));
        PartDefinition shinL = thighL.addOrReplaceChild("shin_l",
CubeListBuilder.create()
        .texOffs(133, 31).addBox(-2.0F, 0.0F, -3.5F, 4.0F, 6.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.1F, 0.0F, 0.0F));
        PartDefinition shinR = thighR.addOrReplaceChild("shin_r",
CubeListBuilder.create()
        .texOffs(227, 30).addBox(-2.0F, 0.0F, -3.5F, 4.0F, 6.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.1F, 0.0F, 0.0F));
        PartDefinition footL = shinL.addOrReplaceChild("foot_l",
CubeListBuilder.create()
        .texOffs(191, 15).addBox(-3.5F, 0.0F, -4.0F, 7.0F, 4.0F, 10.0F, NONE)
        .texOffs(27, 38).addBox(-3.0F, 1.0F, -6.0F, 2.0F, 3.0F, 2.0F, NONE)
        .texOffs(103, 39).addBox(-1.0F, 1.0F, -6.0F, 2.0F, 3.0F, 2.0F, NONE)
        .texOffs(112, 39).addBox(1.0F, 1.0F, -6.0F, 2.0F, 3.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, -1.0F, -0.1F, 0.0F, 0.0F));
        PartDefinition footR = shinR.addOrReplaceChild("foot_r",
CubeListBuilder.create()
        .texOffs(191, 0).addBox(-3.5F, 0.0F, -4.0F, 7.0F, 4.0F, 10.0F, NONE)
        .texOffs(0, 38).addBox(-3.0F, 1.0F, -6.0F, 2.0F, 3.0F, 2.0F, NONE)
        .texOffs(9, 38).addBox(-1.0F, 1.0F, -6.0F, 2.0F, 3.0F, 2.0F, NONE)
        .texOffs(18, 38).addBox(1.0F, 1.0F, -6.0F, 2.0F, 3.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, -1.0F, -0.1F, 0.0F, 0.0F));
        PartDefinition foreL = armL.addOrReplaceChild("fore_l",
CubeListBuilder.create()
        .texOffs(29, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 16.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.2F, 0.0F, 0.0F));
        PartDefinition foreR = armR.addOrReplaceChild("fore_r",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 16.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.2F, 0.0F, 0.0F));
        PartDefinition fistL = foreL.addOrReplaceChild("fist_l",
CubeListBuilder.create()
        .texOffs(198, 30).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 6.0F, 7.0F, NONE)
        .texOffs(36, 38).addBox(-2.5F, 1.0F, -4.5F, 2.0F, 3.0F, 1.0F, NONE)
        .texOffs(156, 39).addBox(0.5F, 1.0F, -4.5F, 2.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition fistR = foreR.addOrReplaceChild("fist_r",
CubeListBuilder.create()
        .texOffs(169, 30).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 6.0F, 7.0F, NONE)
        .texOffs(250, 30).addBox(-2.5F, 1.0F, -4.5F, 2.0F, 3.0F, 1.0F, NONE)
        .texOffs(250, 35).addBox(0.5F, 1.0F, -4.5F, 2.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 256, 192);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return ImmutableList.of(this.head);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList.of(this.torso, this.thighR, this.thighL);
    }


    @Override
    public void setupAnim(FlameApeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // ================================================================
        // 0. 输入信号（全部来自实体公开状态，模型自己不存"上一帧"）
        // ================================================================
        float atk = entity.getAttackAnim(0.0F);                  // 一次挥击 0..1
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float gait = Mth.cos(limbSwing * 0.6662F);               // 步相位 -1..1
        float bob2 = Mth.sin(limbSwing * 1.3324F);               // 双倍频（上下颠）
        float breath = Mth.sin(ageInTicks * 0.085F);             // 呼吸
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);
        boolean air = !entity.onGround();
        // 动作时间轴自己走：一次出手 = 一段固定时长（捶地 32t / 横扫 18t / 爆发 30t），
        // 不跟着原版 mob 那 6t 的短挥击被压扁 —— 巨物要有沉得住的前摇和后摇。
        float[] st = this.flameApeState(entity, atk, ageInTicks);
        int combo = (int) st[0];                                 // 0 捶地 / 1 横扫 / 2 爆发
        float actTicks = (float) flameApeActionTicks(combo);
        float phase = Mth.clamp((ageInTicks - st[3]) / actTicks, 0.0F, 1.0F);
        boolean acting = (ageInTicks - st[3]) >= 0.0F && (ageInTicks - st[3]) < actTicks;

        // ================================================================
        // 1. 基准姿态：idle 呼吸 + 指节行走（walk）
        //    —— 前臂一直弯、拳在身前，是「撑着走」而不是「甩着走」
        // ================================================================
        float torsoX = 0.28F + breath * 0.014F;                  // 前倾 16° + 呼吸
        float torsoY = 8.0F;                                     // 髋关节基准高度
        float torsoZ = gait * 0.055F * walk;                     // 左右晃
        float sx = 1.0F + breath * 0.012F;                       // 呼吸用缩放，不动 y
        float sz = 1.0F + breath * 0.012F;
        float headX = 0.18F + breath * 0.02F - walk * 0.05F;
        float headY = -3.0F - bob2 * 0.45F * walk;               // 走起来头一上一下
        float headTurn = netHeadYaw * ((float) Math.PI / 180.0F) * 0.55F;

        float legSwing = gait * 0.55F * walk;
        float thighRX = legSwing;
        float thighLX = -legSwing;
        float shinRX = 0.06F + Math.max(0.0F, -legSwing) * 1.45F;
        float shinLX = 0.06F + Math.max(0.0F, legSwing) * 1.45F;
        float footRX = -0.06F - shinRX * 0.35F;
        float footLX = -0.06F - shinLX * 0.35F;

        float armSwing = gait * 0.32F * walk;                    // 与同侧腿反相（对角步）
        float armRX = -0.26F - armSwing;
        float armLX = -0.26F + armSwing;
        float armRZ = 0.02F;                                     // 略外张，别夹进躯干
        float armLZ = -0.02F;
        float foreRX = -0.22F - walk * 0.16F + Math.max(0.0F, armSwing) * 0.55F;
        float foreLX = -0.22F - walk * 0.16F + Math.max(0.0F, -armSwing) * 0.55F;
        float fistRX = 0.0F;
        float fistLX = 0.0F;
        float jawOpen = 0.12F + Mth.sin(ageInTicks * 0.11F) * 0.05F;
        float maneFlare = 1.0F + Mth.sin(ageInTicks * 0.13F) * 0.05F;   // 岩浆脉动

        // ================================================================
        // 2. 攻击层（连招路由：0 捶地 / 1 横扫 / 2 熔岩爆发三连）
        // ================================================================
        if (acting && dead <= 0.0F) {
            if (combo == 0) {
                // ---- attack1 双拳捶地：前摇 14t / 砸 6t / 后摇 12t（共 32t）----
                // 注意「臂的世界角 = torso.xRot + arm.xRot」：躯干前倾 0.72 时，
                // 上臂要 -1.05 才让拳真的砸向**身前**地面（不是往后甩）。
                if (phase < 0.4375F) {                             // 前摇 14/32
                    float t = ss(phase / 0.4375F);
                    torsoX = Mth.lerp(t, torsoX, 0.00F);         // 直起身（相对 0.28 的后仰）
                    torsoY = Mth.lerp(t, torsoY, 7.20F);         // 身体抬 0.8px
                    headX = Mth.lerp(t, headX, -0.20F);          // 抬头
                    armRX = Mth.lerp(t, armRX, -1.90F);          // 双拳抬到胸前上方
                    armLX = Mth.lerp(t, armLX, -1.90F);
                    foreRX = Mth.lerp(t, foreRX, -0.60F);        // 屈肘把拳收到头侧（世界角 -2.50）
                    foreLX = Mth.lerp(t, foreLX, -0.60F);
                    armRZ = Mth.lerp(t, armRZ, 0.22F);
                    armLZ = Mth.lerp(t, armLZ, -0.22F);
                    fistRX = Mth.lerp(t, fistRX, -0.20F);
                    fistLX = Mth.lerp(t, fistLX, -0.20F);
                    sz = Mth.lerp(t, sz, 1.05F);                 // 拉长
                    sx = Mth.lerp(t, sx, 0.97F);
                } else if (phase < 0.625F) {                       // 砸 6/32
                    float t = ss((phase - 0.4375F) / 0.1875F);
                    torsoX = Mth.lerp(t, 0.00F, 0.72F);          // 猛地前压（16°→41°）
                    torsoY = Mth.lerp(t, 7.20F, 9.30F);          // 身体下沉 2.1px
                    headX = Mth.lerp(t, -0.20F, 0.70F);          // 头随之下砸
                    armRX = Mth.lerp(t, -1.90F, -1.05F);         // 双拳砸到身前地面（世界角 -0.33）
                    armLX = Mth.lerp(t, -1.90F, -1.05F);
                    foreRX = Mth.lerp(t, -0.60F, -0.08F);        // 肘伸直
                    foreLX = Mth.lerp(t, -0.60F, -0.08F);
                    armRZ = Mth.lerp(t, 0.22F, 0.05F);
                    armLZ = Mth.lerp(t, -0.22F, -0.05F);
                    fistRX = Mth.lerp(t, -0.30F, 0.20F);
                    fistLX = Mth.lerp(t, -0.30F, 0.20F);
                    sz = Mth.lerp(t, 1.05F, 0.90F);              // 砸下压扁
                    sx = Mth.lerp(t, 0.97F, 1.06F);
                } else {                                         // 后摇 12/32
                    float t = ss((phase - 0.625F) / 0.375F);
                    torsoX = Mth.lerp(t, 0.72F, 0.28F + breath * 0.014F);
                    torsoY = Mth.lerp(t, 9.30F, 8.0F);
                    headX = Mth.lerp(t, 0.70F, 0.18F + breath * 0.02F);
                    armRX = Mth.lerp(t, -1.05F, -0.26F);
                    armLX = Mth.lerp(t, -1.05F, -0.26F);
                    foreRX = Mth.lerp(t, -0.08F, -0.22F);
                    foreLX = Mth.lerp(t, -0.08F, -0.22F);
                    fistRX = Mth.lerp(t, 0.20F, 0.0F);
                    fistLX = Mth.lerp(t, 0.20F, 0.0F);
                    sz = Mth.lerp(t, 0.90F, 1.0F);
                    sx = Mth.lerp(t, 1.06F, 1.0F);
                }
            } else if (combo == 1) {
                // ---- attack2 横扫：前摇 10t / 挥 8t（共 18t）----
                if (phase < 0.5556F) {                             // 前摇
                    float t = ss(phase / 0.5556F);
                    torsoZ = Mth.lerp(t, torsoZ, 0.34F);         // 向蓄力侧倾
                    headTurn = Mth.lerp(t, headTurn, -0.34F);
                    armRX = Mth.lerp(t, armRX, -0.70F);
                    armRZ = Mth.lerp(t, armRZ, 0.52F);           // 右臂向外拉开
                    foreRX = Mth.lerp(t, foreRX, -0.95F);
                    torsoY = Mth.lerp(t, torsoY, 7.70F);
                    sz = Mth.lerp(t, sz, 1.03F);
                } else {                                         // 挥
                    float t = ss((phase - 0.5556F) / 0.4444F);
                    torsoZ = Mth.lerp(t, 0.34F, -0.40F);         // 转体扫过
                    headTurn = Mth.lerp(t, -0.34F, 0.30F);
                    armRX = Mth.lerp(t, -0.70F, -1.30F);
                    armRZ = Mth.lerp(t, 0.52F, -0.55F);
                    foreRX = Mth.lerp(t, -0.95F, -0.30F);
                    sx = Mth.lerp(t, sx, 1.04F);
                    sz = Mth.lerp(t, 1.03F, 0.97F);
                }
            } else {
                // ---- special 熔岩爆发：一次挥击内三连砸（每段 1/3：抬→砸）----
                int idx = Mth.floor(phase * 3.0F);
                float sl = Mth.clamp(phase * 3.0F - (float) idx, 0.0F, 1.0F);
                float rise = ss(Mth.clamp(sl / 0.42F, 0.0F, 1.0F));
                float drop = ss(Mth.clamp((sl - 0.42F) / 0.58F, 0.0F, 1.0F));
                float depth = 0.62F * (float) idx;               // 越连越低、越砸越深
                armRX = Mth.lerp(rise, armRX, -1.90F);
                armLX = Mth.lerp(rise, armLX, -1.90F);
                armRX = Mth.lerp(drop, armRX, -1.05F);
                armLX = Mth.lerp(drop, armLX, -1.05F);
                foreRX = Mth.lerp(rise, foreRX, -0.60F);
                foreLX = Mth.lerp(rise, foreLX, -0.60F);
                foreRX = Mth.lerp(drop, foreRX, -0.08F);
                foreLX = Mth.lerp(drop, foreLX, -0.08F);
                torsoX = Mth.lerp(rise, torsoX, 0.00F - depth * 0.06F);
                torsoX = Mth.lerp(drop, torsoX, 0.72F + depth * 0.08F);
                torsoY = Mth.lerp(rise, torsoY, 7.20F - depth * 0.30F);
                torsoY = Mth.lerp(drop, torsoY, 9.20F);
                headX = Mth.lerp(rise, headX, -0.20F);
                headX = Mth.lerp(drop, headX, 0.70F);
                sx = Mth.lerp(drop, sx, 1.06F + depth * 0.02F);
                sz = Mth.lerp(drop, sz, 0.92F);
                maneFlare = 1.0F + 0.18F * drop;                 // 爆发时鬃毛燃起来
            }
        }

        // ================================================================
        // 3. 滞空层：跳起捶地的 AoE —— 双臂过顶、身体拉长、双腿收起
        // ================================================================
        if (air && dead <= 0.0F) {
            float fall = Mth.clamp((float) (-entity.getDeltaMovement().y) * 2.5F, 0.0F, 1.0F);
            armRX = Mth.lerp(fall, -2.15F, -2.60F);
            armLX = Mth.lerp(fall, -2.15F, -2.60F);
            foreRX = Mth.lerp(fall, -0.75F, -1.05F);
            foreLX = Mth.lerp(fall, -0.75F, -1.05F);
            fistRX = -0.25F;
            fistLX = -0.25F;
            torsoX = Mth.lerp(fall, 0.14F, -0.06F);
            torsoY = Mth.lerp(fall, 7.20F, 6.90F);
            headX = -0.12F;
            headY = -3.45F;
            headTurn = netHeadYaw * ((float) Math.PI / 180.0F) * 0.80F;
            thighRX = -0.55F;
            thighLX = -0.62F;
            shinRX = 1.30F;
            shinLX = 1.45F;
            footRX = -0.55F;
            footLX = -0.60F;
            sx = 0.97F;
            sz = Mth.lerp(fall, 1.04F, 1.07F);
            maneFlare = 1.22F;
        }

        // ================================================================
        // 4. hurt 层：后仰 + 吃痛张嘴 + 熔岩闪（鬃毛光片瞬时放大）
        // ================================================================
        if (hurt > 0.0F) {
            torsoX = torsoX - 0.30F * hurt;
            torsoY = torsoY + 0.30F * hurt;
            headX = headX - 0.45F * hurt;
            headY = headY - 0.20F * hurt;
            armRZ = Mth.lerp(hurt, armRZ, 0.42F);
            armLZ = Mth.lerp(hurt, armLZ, -0.42F);
            foreRX = foreRX + 0.25F * hurt;
            foreLX = foreLX + 0.25F * hurt;
            jawOpen = jawOpen + 0.30F * hurt;
            maneFlare = maneFlare + 0.30F * hurt;
            sx = sx * (1.0F + 0.04F * hurt);
            sz = sz * (1.0F + 0.04F * hurt);
        }

        // ================================================================
        // 5. death 层：垂死塌陷（前扑 → 跪伏 → 火熄）
        // ================================================================
        if (dead > 0.0F) {
            float fold = ss(dead);
            torsoX = Mth.lerp(fold, torsoX, 0.95F);              // 前扑塌下（世界角 ~54°）
            torsoY = Mth.lerp(fold, torsoY, 9.20F);
            headX = Mth.lerp(fold, headX, 0.85F);                // 头垂到地上
            headY = Mth.lerp(fold, headY, -2.60F);
            armRX = Mth.lerp(fold, armRX, -1.35F);               // 双臂脱力撑在前方（世界角 -0.40）
            armLX = Mth.lerp(fold, armLX, -1.35F);
            armRZ = Mth.lerp(fold, armRZ, 0.55F);
            armLZ = Mth.lerp(fold, armLZ, -0.55F);
            foreRX = Mth.lerp(fold, foreRX, -0.15F);
            foreLX = Mth.lerp(fold, foreLX, -0.15F);
            thighRX = Mth.lerp(fold, thighRX, -1.25F);
            thighLX = Mth.lerp(fold, thighLX, -1.25F);
            shinRX = Mth.lerp(fold, shinRX, 1.85F);
            shinLX = Mth.lerp(fold, shinLX, 1.85F);
            footRX = Mth.lerp(fold, footRX, -0.60F);
            footLX = Mth.lerp(fold, footLX, -0.60F);
            headTurn = Mth.lerp(fold, headTurn, 0.0F);
            sx = Mth.lerp(fold, sx, 1.05F);
            sz = Mth.lerp(fold, sz, 1.03F);
            maneFlare = Mth.lerp(fold, maneFlare, 0.55F);        // 火熄
        }

        // ================================================================
        // 6. 写回：每字段只写一次，全部绝对赋值
        // ================================================================
        this.torso.xRot = torsoX;
        this.torso.y = torsoY;
        this.torso.zRot = torsoZ;
        this.torso.xScale = sx;
        this.torso.zScale = sz;

        this.head.xRot = headX + headPitch * ((float) Math.PI / 180.0F) * 0.45F;
        this.head.y = headY;
        this.head.yRot = headTurn;
        this.jaw.xRot = jawOpen;

        this.maneR.xRot = -0.10F + Mth.sin(ageInTicks * 0.17F) * 0.08F;
        this.maneR.zRot = -0.12F + Mth.sin(ageInTicks * 0.21F) * 0.06F;
        this.maneR.xScale = maneFlare;
        this.maneR.zScale = maneFlare;
        this.maneL.xRot = -0.10F + Mth.sin(ageInTicks * 0.17F + 1.1F) * 0.08F;
        this.maneL.zRot = 0.12F - Mth.sin(ageInTicks * 0.21F + 1.1F) * 0.06F;
        this.maneL.xScale = maneFlare;
        this.maneL.zScale = maneFlare;
        this.maneTop.xRot = -0.16F + Mth.sin(ageInTicks * 0.19F) * 0.09F;
        this.maneTop.xScale = maneFlare;
        this.maneTop.zScale = maneFlare;

        this.armR.xRot = armRX;
        this.armR.zRot = armRZ;
        this.armL.xRot = armLX;
        this.armL.zRot = armLZ;
        this.foreR.xRot = foreRX;
        this.foreL.xRot = foreLX;
        this.fistR.xRot = fistRX;
        this.fistL.xRot = fistLX;

        this.thighR.xRot = thighRX;
        this.thighL.xRot = thighLX;
        this.shinR.xRot = shinRX;
        this.shinL.xRot = shinLX;
        this.footR.xRot = footRX;
        this.footL.xRot = footLX;

    }


    /** smoothstep 缓动（0..1 → 0..1，起收都慢），动画分层混合用。 */
    private static float ss(float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    /** 一段动作的时长（tick）：就是设计稿里那一列，14+6+12 = 32 / 10+8 = 18 / 3×10 = 30。 */
    private static final int FA_SMASH_TICKS = 32;
    private static final int FA_SWEEP_TICKS = 18;
    private static final int FA_BURST_TICKS = 30;

    private static int flameApeActionTicks(int combo) {
        if (combo == 1) {
            return FA_SWEEP_TICKS;
        }
        if (combo == 2) {
            return FA_BURST_TICKS;
        }
        return FA_SMASH_TICKS;
    }

    /**
     * 连招状态：{连招序号, 上一次出手的 ageInTicks, 上一帧 attackAnim*1000, 本段动作起始 age}。
     *
     * <p>信号只有原版 {@code attackAnim}：它由 {@code swing()} 触发、客户端天然同步。
     * 这里只拿它做两件事 ——</p>
     * <ol>
     *   <li><b>检测"出手了"</b>：attackAnim 从 0 跳到 >0.08 的那一帧记一次出手，
     *       连招序号顺手推进（0 捶地 → 1 横扫 → 2 熔岩爆发 → 回到 0；间隔超过 40t 重数）；</li>
     *   <li><b>给本段动作打时间戳</b>：之后的进度用自己的 tick 时间轴算
     *       （见 {@link #flameApeActionTicks}），于是 32t 的捶地真的是 32t，
     *       不会被原版 mob 那 6t 的短挥击压成 0.3 秒的抽搐。</li>
     * </ol>
     *
     * <p>状态按 entityId 存（渲染器实例是全实体共享的，不能把状态挂在
     * "上一个渲染的实体"上）；只在渲染线程读写。</p>
     */
    private final java.util.Map<Integer, float[]> flameApeStates = new java.util.HashMap<>();

    private float[] flameApeState(FlameApeEntity entity, float atk, float age) {
        int id = entity.getId();
        float[] st = this.flameApeStates.get(id);
        if (st == null) {
            st = new float[]{0.0F, -9999.0F, 0.0F, -9999.0F};
            this.flameApeStates.put(id, st);
        }
        int now = (int) (atk * 1000.0F);
        if (now > 80 && st[2] < 80.0F) {                       // attackAnim 0 → >0：出手了
            st[0] = (age - st[1] > 40.0F) ? 0.0F : (float) ((((int) st[0]) + 1) % 3);
            st[1] = age;
            st[3] = age;                                       // 本段动作从这一帧开始算
        }
        st[2] = (float) now;
        if (this.flameApeStates.size() > 64) {                 // 实体多了不让 map 无限长
            this.flameApeStates.clear();
        }
        return st;
    }

}
