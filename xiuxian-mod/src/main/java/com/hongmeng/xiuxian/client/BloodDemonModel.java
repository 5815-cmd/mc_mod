package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 血魔老祖 · Blood Demon Elder / BloodDemonModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>三头六臂血魔：中央戴冠主头+左右小头、宽肩胸甲+外露肋骨+血核、六臂三姿态、血袍裙甲四层下摆到 y=24、背部骨刺脊/蝠翼残片/血雾薄板；站高 96px、肩宽 64px。</p>
 */
public class BloodDemonModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart hip;
    private final ModelPart mistA;
    private final ModelPart mistB;
    private final ModelPart waist;
    private final ModelPart armU1Left;
    private final ModelPart armU1Right;
    private final ModelPart armU2Left;
    private final ModelPart armU2Right;
    private final ModelPart armU3Left;
    private final ModelPart armU3Right;
    private final ModelPart chestPlate1;
    private final ModelPart chestPlate2;
    private final ModelPart collar;
    private final ModelPart core;
    private final ModelPart neckMid;
    private final ModelPart ribL0;
    private final ModelPart ribL1;
    private final ModelPart ribL2;
    private final ModelPart ribR0;
    private final ModelPart ribR1;
    private final ModelPart ribR2;
    private final ModelPart shoulderLeft;
    private final ModelPart shoulderRight;
    private final ModelPart sideNeckL;
    private final ModelPart sideNeckR;
    private final ModelPart skirt1;
    private final ModelPart spine;
    private final ModelPart wingBone1Left;
    private final ModelPart wingBone1Right;
    private final ModelPart armF1Left;
    private final ModelPart armF1Right;
    private final ModelPart armF2Left;
    private final ModelPart armF2Right;
    private final ModelPart armF3Left;
    private final ModelPart armF3Right;
    private final ModelPart headL;
    private final ModelPart headMid;
    private final ModelPart headR;
    private final ModelPart shoulderLeft2;
    private final ModelPart shoulderRight2;
    private final ModelPart shoulderSpikeLeft0;
    private final ModelPart shoulderSpikeLeft1;
    private final ModelPart shoulderSpikeLeft2;
    private final ModelPart shoulderSpikeRight0;
    private final ModelPart shoulderSpikeRight1;
    private final ModelPart shoulderSpikeRight2;
    private final ModelPart skirt2;
    private final ModelPart spineSpike0;
    private final ModelPart spineSpike1;
    private final ModelPart spineSpike2;
    private final ModelPart spineSpike3;
    private final ModelPart spineSpike4;
    private final ModelPart spineSpike5;
    private final ModelPart wingBone2Left;
    private final ModelPart wingBone2Right;
    private final ModelPart wingMemLeft;
    private final ModelPart wingMemRight;
    private final ModelPart blade1Left;
    private final ModelPart blade1Right;
    private final ModelPart crownBase;
    private final ModelPart eyeL;
    private final ModelPart eyeMid;
    private final ModelPart eyeR;
    private final ModelPart eyeSideL1;
    private final ModelPart eyeSideL2;
    private final ModelPart eyeSideR1;
    private final ModelPart eyeSideR2;
    private final ModelPart hand2Left;
    private final ModelPart hand2Right;
    private final ModelPart hand3Left;
    private final ModelPart hand3Right;
    private final ModelPart hornL1;
    private final ModelPart hornR1;
    private final ModelPart hornSideLeft1;
    private final ModelPart hornSideRight1;
    private final ModelPart jawL;
    private final ModelPart jawMid;
    private final ModelPart jawR;
    private final ModelPart skirt3;
    private final ModelPart crownGem;
    private final ModelPart crownSpike0;
    private final ModelPart crownSpike1;
    private final ModelPart crownSpike2;
    private final ModelPart crownSpike3;
    private final ModelPart crownSpike4;
    private final ModelPart edge1Left;
    private final ModelPart edge1Right;
    private final ModelPart fangMidL;
    private final ModelPart fangMidR;
    private final ModelPart finger2Left0;
    private final ModelPart finger2Left1;
    private final ModelPart finger2Right0;
    private final ModelPart finger2Right1;
    private final ModelPart finger3Left0;
    private final ModelPart finger3Left1;
    private final ModelPart finger3Right0;
    private final ModelPart finger3Right1;
    private final ModelPart hornL2;
    private final ModelPart hornR2;
    private final ModelPart hornSideLeft2;
    private final ModelPart hornSideRight2;
    private final ModelPart skirt4;
    private final ModelPart toothL0;
    private final ModelPart toothL1;
    private final ModelPart toothMid0;
    private final ModelPart toothMid1;
    private final ModelPart toothMid2;
    private final ModelPart toothMid3;
    private final ModelPart toothR0;
    private final ModelPart toothR1;
    private final ModelPart footL;
    private final ModelPart footR;

    public BloodDemonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.chest = root.getChild("body").getChild("chest");
        this.hip = root.getChild("body").getChild("hip");
        this.mistA = root.getChild("body").getChild("mist_a");
        this.mistB = root.getChild("body").getChild("mist_b");
        this.waist = root.getChild("body").getChild("waist");
        this.armU1Left = root.getChild("body").getChild("chest").getChild("arm_u1_left");
        this.armU1Right = root.getChild("body").getChild("chest").getChild("arm_u1_right");
        this.armU2Left = root.getChild("body").getChild("chest").getChild("arm_u2_left");
        this.armU2Right = root.getChild("body").getChild("chest").getChild("arm_u2_right");
        this.armU3Left = root.getChild("body").getChild("chest").getChild("arm_u3_left");
        this.armU3Right = root.getChild("body").getChild("chest").getChild("arm_u3_right");
        this.chestPlate1 = root.getChild("body").getChild("chest").getChild("chest_plate1");
        this.chestPlate2 = root.getChild("body").getChild("chest").getChild("chest_plate2");
        this.collar = root.getChild("body").getChild("chest").getChild("collar");
        this.core = root.getChild("body").getChild("chest").getChild("core");
        this.neckMid = root.getChild("body").getChild("chest").getChild("neck_mid");
        this.ribL0 = root.getChild("body").getChild("chest").getChild("rib_l0");
        this.ribL1 = root.getChild("body").getChild("chest").getChild("rib_l1");
        this.ribL2 = root.getChild("body").getChild("chest").getChild("rib_l2");
        this.ribR0 = root.getChild("body").getChild("chest").getChild("rib_r0");
        this.ribR1 = root.getChild("body").getChild("chest").getChild("rib_r1");
        this.ribR2 = root.getChild("body").getChild("chest").getChild("rib_r2");
        this.shoulderLeft = root.getChild("body").getChild("chest").getChild("shoulder_left");
        this.shoulderRight = root.getChild("body").getChild("chest").getChild("shoulder_right");
        this.sideNeckL = root.getChild("body").getChild("chest").getChild("side_neck_l");
        this.sideNeckR = root.getChild("body").getChild("chest").getChild("side_neck_r");
        this.skirt1 = root.getChild("body").getChild("hip").getChild("skirt1");
        this.spine = root.getChild("body").getChild("chest").getChild("spine");
        this.wingBone1Left = root.getChild("body").getChild("chest").getChild("wing_bone1_left");
        this.wingBone1Right = root.getChild("body").getChild("chest").getChild("wing_bone1_right");
        this.armF1Left = root.getChild("body").getChild("chest").getChild("arm_u1_left").getChild("arm_f1_left");
        this.armF1Right = root.getChild("body").getChild("chest").getChild("arm_u1_right").getChild("arm_f1_right");
        this.armF2Left = root.getChild("body").getChild("chest").getChild("arm_u2_left").getChild("arm_f2_left");
        this.armF2Right = root.getChild("body").getChild("chest").getChild("arm_u2_right").getChild("arm_f2_right");
        this.armF3Left = root.getChild("body").getChild("chest").getChild("arm_u3_left").getChild("arm_f3_left");
        this.armF3Right = root.getChild("body").getChild("chest").getChild("arm_u3_right").getChild("arm_f3_right");
        this.headL = root.getChild("body").getChild("chest").getChild("side_neck_l").getChild("head_l");
        this.headMid = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid");
        this.headR = root.getChild("body").getChild("chest").getChild("side_neck_r").getChild("head_r");
        this.shoulderLeft2 = root.getChild("body").getChild("chest").getChild("shoulder_left").getChild("shoulder_left2");
        this.shoulderRight2 = root.getChild("body").getChild("chest").getChild("shoulder_right").getChild("shoulder_right2");
        this.shoulderSpikeLeft0 = root.getChild("body").getChild("chest").getChild("shoulder_left").getChild("shoulder_spike_left_0");
        this.shoulderSpikeLeft1 = root.getChild("body").getChild("chest").getChild("shoulder_left").getChild("shoulder_spike_left_1");
        this.shoulderSpikeLeft2 = root.getChild("body").getChild("chest").getChild("shoulder_left").getChild("shoulder_spike_left_2");
        this.shoulderSpikeRight0 = root.getChild("body").getChild("chest").getChild("shoulder_right").getChild("shoulder_spike_right_0");
        this.shoulderSpikeRight1 = root.getChild("body").getChild("chest").getChild("shoulder_right").getChild("shoulder_spike_right_1");
        this.shoulderSpikeRight2 = root.getChild("body").getChild("chest").getChild("shoulder_right").getChild("shoulder_spike_right_2");
        this.skirt2 = root.getChild("body").getChild("hip").getChild("skirt1").getChild("skirt2");
        this.spineSpike0 = root.getChild("body").getChild("chest").getChild("spine").getChild("spine_spike_0");
        this.spineSpike1 = root.getChild("body").getChild("chest").getChild("spine").getChild("spine_spike_1");
        this.spineSpike2 = root.getChild("body").getChild("chest").getChild("spine").getChild("spine_spike_2");
        this.spineSpike3 = root.getChild("body").getChild("chest").getChild("spine").getChild("spine_spike_3");
        this.spineSpike4 = root.getChild("body").getChild("chest").getChild("spine").getChild("spine_spike_4");
        this.spineSpike5 = root.getChild("body").getChild("chest").getChild("spine").getChild("spine_spike_5");
        this.wingBone2Left = root.getChild("body").getChild("chest").getChild("wing_bone1_left").getChild("wing_bone2_left");
        this.wingBone2Right = root.getChild("body").getChild("chest").getChild("wing_bone1_right").getChild("wing_bone2_right");
        this.wingMemLeft = root.getChild("body").getChild("chest").getChild("wing_bone1_left").getChild("wing_mem_left");
        this.wingMemRight = root.getChild("body").getChild("chest").getChild("wing_bone1_right").getChild("wing_mem_right");
        this.blade1Left = root.getChild("body").getChild("chest").getChild("arm_u1_left").getChild("arm_f1_left").getChild("blade1_left");
        this.blade1Right = root.getChild("body").getChild("chest").getChild("arm_u1_right").getChild("arm_f1_right").getChild("blade1_right");
        this.crownBase = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("crown_base");
        this.eyeL = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("eye_l");
        this.eyeMid = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("eye_mid");
        this.eyeR = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("eye_r");
        this.eyeSideL1 = root.getChild("body").getChild("chest").getChild("side_neck_l").getChild("head_l").getChild("eye_side_l1");
        this.eyeSideL2 = root.getChild("body").getChild("chest").getChild("side_neck_l").getChild("head_l").getChild("eye_side_l2");
        this.eyeSideR1 = root.getChild("body").getChild("chest").getChild("side_neck_r").getChild("head_r").getChild("eye_side_r1");
        this.eyeSideR2 = root.getChild("body").getChild("chest").getChild("side_neck_r").getChild("head_r").getChild("eye_side_r2");
        this.hand2Left = root.getChild("body").getChild("chest").getChild("arm_u2_left").getChild("arm_f2_left").getChild("hand2_left");
        this.hand2Right = root.getChild("body").getChild("chest").getChild("arm_u2_right").getChild("arm_f2_right").getChild("hand2_right");
        this.hand3Left = root.getChild("body").getChild("chest").getChild("arm_u3_left").getChild("arm_f3_left").getChild("hand3_left");
        this.hand3Right = root.getChild("body").getChild("chest").getChild("arm_u3_right").getChild("arm_f3_right").getChild("hand3_right");
        this.hornL1 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("horn_l1");
        this.hornR1 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("horn_r1");
        this.hornSideLeft1 = root.getChild("body").getChild("chest").getChild("side_neck_l").getChild("head_l").getChild("horn_side_left1");
        this.hornSideRight1 = root.getChild("body").getChild("chest").getChild("side_neck_r").getChild("head_r").getChild("horn_side_right1");
        this.jawL = root.getChild("body").getChild("chest").getChild("side_neck_l").getChild("head_l").getChild("jaw_l");
        this.jawMid = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("jaw_mid");
        this.jawR = root.getChild("body").getChild("chest").getChild("side_neck_r").getChild("head_r").getChild("jaw_r");
        this.skirt3 = root.getChild("body").getChild("hip").getChild("skirt1").getChild("skirt2").getChild("skirt3");
        this.crownGem = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("crown_base").getChild("crown_gem");
        this.crownSpike0 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("crown_base").getChild("crown_spike_0");
        this.crownSpike1 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("crown_base").getChild("crown_spike_1");
        this.crownSpike2 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("crown_base").getChild("crown_spike_2");
        this.crownSpike3 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("crown_base").getChild("crown_spike_3");
        this.crownSpike4 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("crown_base").getChild("crown_spike_4");
        this.edge1Left = root.getChild("body").getChild("chest").getChild("arm_u1_left").getChild("arm_f1_left").getChild("blade1_left").getChild("edge1_left");
        this.edge1Right = root.getChild("body").getChild("chest").getChild("arm_u1_right").getChild("arm_f1_right").getChild("blade1_right").getChild("edge1_right");
        this.fangMidL = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("jaw_mid").getChild("fang_mid_l");
        this.fangMidR = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("jaw_mid").getChild("fang_mid_r");
        this.finger2Left0 = root.getChild("body").getChild("chest").getChild("arm_u2_left").getChild("arm_f2_left").getChild("hand2_left").getChild("finger2_left_0");
        this.finger2Left1 = root.getChild("body").getChild("chest").getChild("arm_u2_left").getChild("arm_f2_left").getChild("hand2_left").getChild("finger2_left_1");
        this.finger2Right0 = root.getChild("body").getChild("chest").getChild("arm_u2_right").getChild("arm_f2_right").getChild("hand2_right").getChild("finger2_right_0");
        this.finger2Right1 = root.getChild("body").getChild("chest").getChild("arm_u2_right").getChild("arm_f2_right").getChild("hand2_right").getChild("finger2_right_1");
        this.finger3Left0 = root.getChild("body").getChild("chest").getChild("arm_u3_left").getChild("arm_f3_left").getChild("hand3_left").getChild("finger3_left_0");
        this.finger3Left1 = root.getChild("body").getChild("chest").getChild("arm_u3_left").getChild("arm_f3_left").getChild("hand3_left").getChild("finger3_left_1");
        this.finger3Right0 = root.getChild("body").getChild("chest").getChild("arm_u3_right").getChild("arm_f3_right").getChild("hand3_right").getChild("finger3_right_0");
        this.finger3Right1 = root.getChild("body").getChild("chest").getChild("arm_u3_right").getChild("arm_f3_right").getChild("hand3_right").getChild("finger3_right_1");
        this.hornL2 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("horn_l1").getChild("horn_l2");
        this.hornR2 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("horn_r1").getChild("horn_r2");
        this.hornSideLeft2 = root.getChild("body").getChild("chest").getChild("side_neck_l").getChild("head_l").getChild("horn_side_left1").getChild("horn_side_left2");
        this.hornSideRight2 = root.getChild("body").getChild("chest").getChild("side_neck_r").getChild("head_r").getChild("horn_side_right1").getChild("horn_side_right2");
        this.skirt4 = root.getChild("body").getChild("hip").getChild("skirt1").getChild("skirt2").getChild("skirt3").getChild("skirt4");
        this.toothL0 = root.getChild("body").getChild("chest").getChild("side_neck_l").getChild("head_l").getChild("jaw_l").getChild("tooth_l0");
        this.toothL1 = root.getChild("body").getChild("chest").getChild("side_neck_l").getChild("head_l").getChild("jaw_l").getChild("tooth_l1");
        this.toothMid0 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("jaw_mid").getChild("tooth_mid_0");
        this.toothMid1 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("jaw_mid").getChild("tooth_mid_1");
        this.toothMid2 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("jaw_mid").getChild("tooth_mid_2");
        this.toothMid3 = root.getChild("body").getChild("chest").getChild("neck_mid").getChild("head_mid").getChild("jaw_mid").getChild("tooth_mid_3");
        this.toothR0 = root.getChild("body").getChild("chest").getChild("side_neck_r").getChild("head_r").getChild("jaw_r").getChild("tooth_r0");
        this.toothR1 = root.getChild("body").getChild("chest").getChild("side_neck_r").getChild("head_r").getChild("jaw_r").getChild("tooth_r1");
        this.footL = root.getChild("body").getChild("hip").getChild("skirt1").getChild("skirt2").getChild("skirt3").getChild("skirt4").getChild("foot_l");
        this.footR = root.getChild("body").getChild("hip").getChild("skirt1").getChild("skirt2").getChild("skirt3").getChild("skirt4").getChild("foot_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition chest = body.addOrReplaceChild("chest",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-13.0F, -24.0F, -8.0F, 26.0F, 24.0F, 16.0F, NONE)
                , PartPose.offset(0.0F, -16.0F, 0.0F));
        PartDefinition hip = body.addOrReplaceChild("hip",
CubeListBuilder.create()
        .texOffs(0, 115).addBox(-12.0F, -8.0F, -7.0F, 24.0F, 7.0F, 15.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition mistA = body.addOrReplaceChild("mist_a",
CubeListBuilder.create()
        .texOffs(75, 227).addBox(-18.0F, -2.0F, -16.0F, 36.0F, 3.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -18.0F, 0.0F, 0.0F, 0.2F, 0.0F));
        PartDefinition mistB = body.addOrReplaceChild("mist_b",
CubeListBuilder.create()
        .texOffs(66, 232).addBox(-16.0F, -2.0F, 12.0F, 32.0F, 3.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -12.0F, 0.0F, 0.0F, -0.3F, 0.0F));
        PartDefinition waist = body.addOrReplaceChild("waist",
CubeListBuilder.create()
        .texOffs(0, 138).addBox(-10.0F, -8.0F, -6.0F, 20.0F, 7.0F, 13.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition armU1Left = chest.addOrReplaceChild("arm_u1_left",
CubeListBuilder.create()
        .texOffs(210, 56).addBox(0.0F, 0.0F, -4.0F, 8.0F, 13.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(13.0F, -24.0F, 0.0F, 0.0F, 0.0F, -0.6F));
        PartDefinition armU1Right = chest.addOrReplaceChild("arm_u1_right",
CubeListBuilder.create()
        .texOffs(53, 68).addBox(-8.0F, 0.0F, -4.0F, 8.0F, 13.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(-13.0F, -24.0F, 0.0F, 0.0F, 0.0F, 0.6F));
        PartDefinition armU2Left = chest.addOrReplaceChild("arm_u2_left",
CubeListBuilder.create()
        .texOffs(67, 138).addBox(0.0F, 0.0F, -3.0F, 7.0F, 13.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(12.0F, -14.0F, 2.0F, 0.2F, 0.0F, -0.1F));
        PartDefinition armU2Right = chest.addOrReplaceChild("arm_u2_right",
CubeListBuilder.create()
        .texOffs(195, 138).addBox(-7.0F, 0.0F, -3.0F, 7.0F, 13.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(-12.0F, -14.0F, 2.0F, 0.2F, 0.0F, 0.1F));
        PartDefinition armU3Left = chest.addOrReplaceChild("arm_u3_left",
CubeListBuilder.create()
        .texOffs(166, 138).addBox(0.0F, 0.0F, -3.0F, 7.0F, 13.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(13.0F, -4.0F, -1.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition armU3Right = chest.addOrReplaceChild("arm_u3_right",
CubeListBuilder.create()
        .texOffs(224, 138).addBox(-7.0F, 0.0F, -3.0F, 7.0F, 13.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(-13.0F, -4.0F, -1.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition chestPlate1 = chest.addOrReplaceChild("chest_plate1",
CubeListBuilder.create()
        .texOffs(107, 215).addBox(-13.0F, -8.0F, -2.0F, 26.0F, 8.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -18.0F, -9.0F));
        PartDefinition chestPlate2 = chest.addOrReplaceChild("chest_plate2",
CubeListBuilder.create()
        .texOffs(191, 77).addBox(-11.0F, -10.0F, -2.0F, 22.0F, 9.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, -9.0F));
        PartDefinition collar = chest.addOrReplaceChild("collar",
CubeListBuilder.create()
        .texOffs(0, 159).addBox(-15.0F, -3.0F, -9.0F, 30.0F, 3.0F, 16.0F, NONE)
                , PartPose.offset(0.0F, -24.0F, 0.0F));
        PartDefinition core = chest.addOrReplaceChild("core",
CubeListBuilder.create()
        .texOffs(215, 197).addBox(-5.0F, -5.0F, -3.0F, 10.0F, 10.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -14.0F, -9.0F));
        PartDefinition neckMid = chest.addOrReplaceChild("neck_mid",
CubeListBuilder.create()
        .texOffs(182, 197).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, -24.0F, 0.0F));
        PartDefinition ribL0 = chest.addOrReplaceChild("rib_l0",
CubeListBuilder.create()
        .texOffs(0, 70).addBox(0.0F, 0.0F, -8.0F, 2.0F, 2.0F, 16.0F, NONE)
                , PartPose.offset(14.0F, -18.0F, 0.0F));
        PartDefinition ribL1 = chest.addOrReplaceChild("rib_l1",
CubeListBuilder.create()
        .texOffs(130, 178).addBox(0.0F, 0.0F, -8.0F, 2.0F, 2.0F, 16.0F, NONE)
                , PartPose.offset(14.0F, -10.0F, 0.0F));
        PartDefinition ribL2 = chest.addOrReplaceChild("rib_l2",
CubeListBuilder.create()
        .texOffs(204, 178).addBox(0.0F, 0.0F, -8.0F, 2.0F, 2.0F, 16.0F, NONE)
                , PartPose.offset(14.0F, -2.0F, 0.0F));
        PartDefinition ribR0 = chest.addOrReplaceChild("rib_r0",
CubeListBuilder.create()
        .texOffs(93, 160).addBox(-2.0F, 0.0F, -8.0F, 2.0F, 2.0F, 16.0F, NONE)
                , PartPose.offset(-14.0F, -18.0F, 0.0F));
        PartDefinition ribR1 = chest.addOrReplaceChild("rib_r1",
CubeListBuilder.create()
        .texOffs(167, 178).addBox(-2.0F, 0.0F, -8.0F, 2.0F, 2.0F, 16.0F, NONE)
                , PartPose.offset(-14.0F, -10.0F, 0.0F));
        PartDefinition ribR2 = chest.addOrReplaceChild("rib_r2",
CubeListBuilder.create()
        .texOffs(0, 179).addBox(-2.0F, 0.0F, -8.0F, 2.0F, 2.0F, 16.0F, NONE)
                , PartPose.offset(-14.0F, -2.0F, 0.0F));
        PartDefinition shoulderLeft = chest.addOrReplaceChild("shoulder_left",
CubeListBuilder.create()
        .texOffs(178, 89).addBox(0.0F, -6.0F, -8.0F, 18.0F, 8.0F, 16.0F, NONE)
                , PartPose.offset(14.0F, -24.0F, 0.0F));
        PartDefinition shoulderRight = chest.addOrReplaceChild("shoulder_right",
CubeListBuilder.create()
        .texOffs(97, 114).addBox(-18.0F, -6.0F, -8.0F, 18.0F, 8.0F, 16.0F, NONE)
                , PartPose.offset(-14.0F, -24.0F, 0.0F));
        PartDefinition sideNeckL = chest.addOrReplaceChild("side_neck_l",
CubeListBuilder.create()
        .texOffs(96, 139).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 14.0F, 6.0F, NONE)
                , PartPose.offset(15.0F, -26.0F, -3.0F));
        PartDefinition sideNeckR = chest.addOrReplaceChild("side_neck_r",
CubeListBuilder.create()
        .texOffs(121, 139).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 14.0F, 6.0F, NONE)
                , PartPose.offset(-15.0F, -26.0F, -3.0F));
        PartDefinition skirt1 = hip.addOrReplaceChild("skirt1",
CubeListBuilder.create()
        .texOffs(97, 89).addBox(-12.0F, 0.0F, -8.0F, 24.0F, 8.0F, 16.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition spine = chest.addOrReplaceChild("spine",
CubeListBuilder.create()
        .texOffs(150, 0).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 26.0F, 6.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -24.0F, 9.0F, -0.1F, 0.0F, 0.0F));
        PartDefinition wingBone1Left = chest.addOrReplaceChild("wing_bone1_left",
CubeListBuilder.create()
        .texOffs(241, 46).addBox(0.0F, 0.0F, 0.0F, 2.0F, 12.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(10.0F, -10.0F, 9.0F, 0.0F, 0.0F, -0.5F));
        PartDefinition wingBone1Right = chest.addOrReplaceChild("wing_bone1_right",
CubeListBuilder.create()
        .texOffs(241, 61).addBox(0.0F, 0.0F, 0.0F, 2.0F, 12.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-10.0F, -10.0F, 9.0F, 0.0F, 0.0F, 0.5F));
        PartDefinition armF1Left = armU1Left.addOrReplaceChild("arm_f1_left",
CubeListBuilder.create()
        .texOffs(37, 179).addBox(-4.0F, 0.0F, -4.0F, 7.0F, 11.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.3F, 0.0F, 0.3F));
        PartDefinition armF1Right = armU1Right.addOrReplaceChild("arm_f1_right",
CubeListBuilder.create()
        .texOffs(66, 179).addBox(-4.0F, 0.0F, -4.0F, 7.0F, 11.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.3F, 0.0F, -0.3F));
        PartDefinition armF2Left = armU2Left.addOrReplaceChild("arm_f2_left",
CubeListBuilder.create()
        .texOffs(95, 179).addBox(-3.0F, 0.0F, -3.0F, 7.0F, 10.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5F, 0.0F, 0.1F));
        PartDefinition armF2Right = armU2Right.addOrReplaceChild("arm_f2_right",
CubeListBuilder.create()
        .texOffs(124, 197).addBox(-3.0F, 0.0F, -3.0F, 7.0F, 10.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5F, 0.0F, -0.1F));
        PartDefinition armF3Left = armU3Left.addOrReplaceChild("arm_f3_left",
CubeListBuilder.create()
        .texOffs(95, 197).addBox(-3.0F, 0.0F, -3.0F, 7.0F, 10.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.2F, 0.0F, -0.2F));
        PartDefinition armF3Right = armU3Right.addOrReplaceChild("arm_f3_right",
CubeListBuilder.create()
        .texOffs(153, 197).addBox(-3.0F, 0.0F, -3.0F, 7.0F, 10.0F, 7.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.2F, 0.0F, 0.2F));
        PartDefinition headL = sideNeckL.addOrReplaceChild("head_l",
CubeListBuilder.create()
        .texOffs(179, 0).addBox(-6.0F, -7.0F, -7.0F, 12.0F, 14.0F, 14.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition headMid = neckMid.addOrReplaceChild("head_mid",
CubeListBuilder.create()
        .texOffs(85, 0).addBox(-8.0F, -14.0F, -8.0F, 16.0F, 16.0F, 16.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition headR = sideNeckR.addOrReplaceChild("head_r",
CubeListBuilder.create()
        .texOffs(0, 41).addBox(-6.0F, -7.0F, -7.0F, 12.0F, 14.0F, 14.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition shoulderLeft2 = shoulderLeft.addOrReplaceChild("shoulder_left2",
CubeListBuilder.create()
        .texOffs(146, 159).addBox(0.0F, -6.0F, -6.0F, 14.0F, 5.0F, 13.0F, NONE)
                , PartPose.offset(1.0F, -6.0F, 0.0F));
        PartDefinition shoulderRight2 = shoulderRight.addOrReplaceChild("shoulder_right2",
CubeListBuilder.create()
        .texOffs(201, 159).addBox(-14.0F, -6.0F, -6.0F, 14.0F, 5.0F, 13.0F, NONE)
                , PartPose.offset(-1.0F, -6.0F, 0.0F));
        PartDefinition shoulderSpikeLeft0 = shoulderLeft.addOrReplaceChild("shoulder_spike_left_0",
CubeListBuilder.create()
        .texOffs(247, 88).addBox(0.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(4.0F, -6.0F, -7.0F));
        PartDefinition shoulderSpikeLeft1 = shoulderLeft.addOrReplaceChild("shoulder_spike_left_1",
CubeListBuilder.create()
        .texOffs(247, 97).addBox(0.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(9.0F, -6.0F, 0.0F));
        PartDefinition shoulderSpikeLeft2 = shoulderLeft.addOrReplaceChild("shoulder_spike_left_2",
CubeListBuilder.create()
        .texOffs(79, 115).addBox(0.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(14.0F, -6.0F, 7.0F));
        PartDefinition shoulderSpikeRight0 = shoulderRight.addOrReplaceChild("shoulder_spike_right_0",
CubeListBuilder.create()
        .texOffs(88, 115).addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-4.0F, -6.0F, -7.0F));
        PartDefinition shoulderSpikeRight1 = shoulderRight.addOrReplaceChild("shoulder_spike_right_1",
CubeListBuilder.create()
        .texOffs(239, 123).addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-9.0F, -6.0F, 0.0F));
        PartDefinition shoulderSpikeRight2 = shoulderRight.addOrReplaceChild("shoulder_spike_right_2",
CubeListBuilder.create()
        .texOffs(248, 123).addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-14.0F, -6.0F, 7.0F));
        PartDefinition skirt2 = skirt1.addOrReplaceChild("skirt2",
CubeListBuilder.create()
        .texOffs(0, 89).addBox(-15.0F, 0.0F, -9.0F, 30.0F, 7.0F, 18.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition spineSpike0 = spine.addOrReplaceChild("spine_spike_0",
CubeListBuilder.create()
        .texOffs(155, 148).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 2.0F, 0.0F));
        PartDefinition spineSpike1 = spine.addOrReplaceChild("spine_spike_1",
CubeListBuilder.create()
        .texOffs(130, 160).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition spineSpike2 = spine.addOrReplaceChild("spine_spike_2",
CubeListBuilder.create()
        .texOffs(130, 168).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition spineSpike3 = spine.addOrReplaceChild("spine_spike_3",
CubeListBuilder.create()
        .texOffs(241, 178).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition spineSpike4 = spine.addOrReplaceChild("spine_spike_4",
CubeListBuilder.create()
        .texOffs(241, 186).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition spineSpike5 = spine.addOrReplaceChild("spine_spike_5",
CubeListBuilder.create()
        .texOffs(242, 194).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 22.0F, 0.0F));
        PartDefinition wingBone2Left = wingBone1Left.addOrReplaceChild("wing_bone2_left",
CubeListBuilder.create()
        .texOffs(191, 63).addBox(0.0F, 0.0F, 0.0F, 2.0F, 10.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.4F));
        PartDefinition wingBone2Right = wingBone1Right.addOrReplaceChild("wing_bone2_right",
CubeListBuilder.create()
        .texOffs(200, 63).addBox(0.0F, 0.0F, 0.0F, 2.0F, 10.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.4F));
        PartDefinition wingMemLeft = wingBone1Left.addOrReplaceChild("wing_mem_left",
CubeListBuilder.create()
        .texOffs(210, 29).addBox(0.0F, 0.0F, -7.0F, 1.0F, 12.0F, 14.0F, NONE)
                , PartPose.offset(1.0F, 0.0F, 0.0F));
        PartDefinition wingMemRight = wingBone1Right.addOrReplaceChild("wing_mem_right",
CubeListBuilder.create()
        .texOffs(53, 41).addBox(-1.0F, 0.0F, -7.0F, 1.0F, 12.0F, 14.0F, NONE)
                , PartPose.offset(-1.0F, 0.0F, 0.0F));
        PartDefinition blade1Left = armF1Left.addOrReplaceChild("blade1_left",
CubeListBuilder.create()
        .texOffs(232, 0).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 18.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 11.0F, 0.0F));
        PartDefinition blade1Right = armF1Right.addOrReplaceChild("blade1_right",
CubeListBuilder.create()
        .texOffs(241, 23).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 18.0F, 4.0F, NONE)
                , PartPose.offset(0.0F, 11.0F, 0.0F));
        PartDefinition crownBase = headMid.addOrReplaceChild("crown_base",
CubeListBuilder.create()
        .texOffs(166, 114).addBox(-9.0F, -5.0F, -9.0F, 18.0F, 5.0F, 18.0F, NONE)
                , PartPose.offset(0.0F, -14.0F, -1.0F));
        PartDefinition eyeL = headMid.addOrReplaceChild("eye_l",
CubeListBuilder.create()
        .texOffs(232, 23).addBox(-6.0F, -9.0F, -9.0F, 3.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeMid = headMid.addOrReplaceChild("eye_mid",
CubeListBuilder.create()
        .texOffs(79, 133).addBox(-2.0F, -13.0F, -9.0F, 4.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeR = headMid.addOrReplaceChild("eye_r",
CubeListBuilder.create()
        .texOffs(31, 219).addBox(3.0F, -9.0F, -9.0F, 3.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeSideL1 = headL.addOrReplaceChild("eye_side_l1",
CubeListBuilder.create()
        .texOffs(179, 29).addBox(-4.0F, -4.0F, -8.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeSideL2 = headL.addOrReplaceChild("eye_side_l2",
CubeListBuilder.create()
        .texOffs(186, 29).addBox(2.0F, -4.0F, -8.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeSideR1 = headR.addOrReplaceChild("eye_side_r1",
CubeListBuilder.create()
        .texOffs(193, 29).addBox(-4.0F, -4.0F, -8.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition eyeSideR2 = headR.addOrReplaceChild("eye_side_r2",
CubeListBuilder.create()
        .texOffs(200, 29).addBox(2.0F, -4.0F, -8.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition hand2Left = armF2Left.addOrReplaceChild("hand2_left",
CubeListBuilder.create()
        .texOffs(47, 198).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 5.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition hand2Right = armF2Right.addOrReplaceChild("hand2_right",
CubeListBuilder.create()
        .texOffs(215, 211).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 5.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition hand3Left = armF3Left.addOrReplaceChild("hand3_left",
CubeListBuilder.create()
        .texOffs(47, 211).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 5.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition hand3Right = armF3Right.addOrReplaceChild("hand3_right",
CubeListBuilder.create()
        .texOffs(0, 212).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 5.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition hornL1 = headMid.addOrReplaceChild("horn_l1",
CubeListBuilder.create()
        .texOffs(37, 70).addBox(0.0F, -8.0F, 0.0F, 3.0F, 8.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(7.0F, -12.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition hornR1 = headMid.addOrReplaceChild("horn_r1",
CubeListBuilder.create()
        .texOffs(241, 76).addBox(-3.0F, -8.0F, 0.0F, 3.0F, 8.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(-7.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition hornSideLeft1 = headL.addOrReplaceChild("horn_side_left1",
CubeListBuilder.create()
        .texOffs(146, 139).addBox(0.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(5.0F, -6.0F, 0.0F, 0.0F, 0.0F, -0.4F));
        PartDefinition hornSideRight1 = headR.addOrReplaceChild("horn_side_right1",
CubeListBuilder.create()
        .texOffs(155, 139).addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-5.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.4F));
        PartDefinition jawL = headL.addOrReplaceChild("jaw_l",
CubeListBuilder.create()
        .texOffs(31, 224).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 3.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, -7.0F));
        PartDefinition jawMid = headMid.addOrReplaceChild("jaw_mid",
CubeListBuilder.create()
        .texOffs(0, 198).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 4.0F, 9.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, -8.0F));
        PartDefinition jawR = headR.addOrReplaceChild("jaw_r",
CubeListBuilder.create()
        .texOffs(211, 224).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 3.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, -7.0F));
        PartDefinition skirt3 = skirt2.addOrReplaceChild("skirt3",
CubeListBuilder.create()
        .texOffs(84, 63).addBox(-17.0F, 0.0F, -9.0F, 34.0F, 6.0F, 19.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition crownGem = crownBase.addOrReplaceChild("crown_gem",
CubeListBuilder.create()
        .texOffs(239, 132).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, -9.0F));
        PartDefinition crownSpike0 = crownBase.addOrReplaceChild("crown_spike_0",
CubeListBuilder.create()
        .texOffs(239, 114).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 5.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -5.0F, 0.0F));
        PartDefinition crownSpike1 = crownBase.addOrReplaceChild("crown_spike_1",
CubeListBuilder.create()
        .texOffs(37, 82).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(-6.0F, -5.0F, -6.0F));
        PartDefinition crownSpike2 = crownBase.addOrReplaceChild("crown_spike_2",
CubeListBuilder.create()
        .texOffs(78, 198).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(6.0F, -5.0F, 6.0F));
        PartDefinition crownSpike3 = crownBase.addOrReplaceChild("crown_spike_3",
CubeListBuilder.create()
        .texOffs(242, 202).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(-6.0F, -5.0F, 6.0F));
        PartDefinition crownSpike4 = crownBase.addOrReplaceChild("crown_spike_4",
CubeListBuilder.create()
        .texOffs(78, 205).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(6.0F, -5.0F, -6.0F));
        PartDefinition edge1Left = blade1Left.addOrReplaceChild("edge1_left",
CubeListBuilder.create()
        .texOffs(245, 0).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 18.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition edge1Right = blade1Right.addOrReplaceChild("edge1_right",
CubeListBuilder.create()
        .texOffs(250, 0).addBox(0.0F, 0.0F, -3.0F, 1.0F, 18.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition fangMidL = jawMid.addOrReplaceChild("fang_mid_l",
CubeListBuilder.create()
        .texOffs(246, 209).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(-6.0F, -2.0F, -6.0F));
        PartDefinition fangMidR = jawMid.addOrReplaceChild("fang_mid_r",
CubeListBuilder.create()
        .texOffs(31, 212).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(4.0F, -2.0F, -6.0F));
        PartDefinition finger2Left0 = hand2Left.addOrReplaceChild("finger2_left_0",
CubeListBuilder.create()
        .texOffs(164, 215).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(-2.0F, 5.0F, -2.0F));
        PartDefinition finger2Left1 = hand2Left.addOrReplaceChild("finger2_left_1",
CubeListBuilder.create()
        .texOffs(173, 215).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(1.0F, 5.0F, -2.0F));
        PartDefinition finger2Right0 = hand2Right.addOrReplaceChild("finger2_right_0",
CubeListBuilder.create()
        .texOffs(173, 222).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(-2.0F, 5.0F, -2.0F));
        PartDefinition finger2Right1 = hand2Right.addOrReplaceChild("finger2_right_1",
CubeListBuilder.create()
        .texOffs(246, 223).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(1.0F, 5.0F, -2.0F));
        PartDefinition finger3Left0 = hand3Left.addOrReplaceChild("finger3_left_0",
CubeListBuilder.create()
        .texOffs(246, 216).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(-2.0F, 5.0F, -2.0F));
        PartDefinition finger3Left1 = hand3Left.addOrReplaceChild("finger3_left_1",
CubeListBuilder.create()
        .texOffs(164, 222).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(1.0F, 5.0F, -2.0F));
        PartDefinition finger3Right0 = hand3Right.addOrReplaceChild("finger3_right_0",
CubeListBuilder.create()
        .texOffs(66, 224).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(-2.0F, 5.0F, -2.0F));
        PartDefinition finger3Right1 = hand3Right.addOrReplaceChild("finger3_right_1",
CubeListBuilder.create()
        .texOffs(0, 225).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, NONE)
                , PartPose.offset(1.0F, 5.0F, -2.0F));
        PartDefinition hornL2 = hornL1.addOrReplaceChild("horn_l2",
CubeListBuilder.create()
        .texOffs(79, 124).addBox(0.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(1.0F, -7.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition hornR2 = hornR1.addOrReplaceChild("horn_r2",
CubeListBuilder.create()
        .texOffs(88, 124).addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-1.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition hornSideLeft2 = hornSideLeft1.addOrReplaceChild("horn_side_left2",
CubeListBuilder.create()
        .texOffs(247, 106).addBox(0.0F, -5.0F, 0.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition hornSideRight2 = hornSideRight1.addOrReplaceChild("horn_side_right2",
CubeListBuilder.create()
        .texOffs(146, 148).addBox(-2.0F, -5.0F, 0.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition skirt4 = skirt3.addOrReplaceChild("skirt4",
CubeListBuilder.create()
        .texOffs(85, 33).addBox(-20.0F, 0.0F, -11.0F, 40.0F, 7.0F, 22.0F, NONE)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition toothL0 = jawL.addOrReplaceChild("tooth_l0",
CubeListBuilder.create()
        .texOffs(250, 62).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(-3.0F, -1.0F, -5.0F));
        PartDefinition toothL1 = jawL.addOrReplaceChild("tooth_l1",
CubeListBuilder.create()
        .texOffs(250, 66).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(2.0F, -1.0F, -5.0F));
        PartDefinition toothMid0 = jawMid.addOrReplaceChild("tooth_mid_0",
CubeListBuilder.create()
        .texOffs(250, 46).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(-5.0F, -2.0F, -7.0F));
        PartDefinition toothMid1 = jawMid.addOrReplaceChild("tooth_mid_1",
CubeListBuilder.create()
        .texOffs(250, 50).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(-1.7F, -2.0F, -7.0F));
        PartDefinition toothMid2 = jawMid.addOrReplaceChild("tooth_mid_2",
CubeListBuilder.create()
        .texOffs(250, 54).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(1.6F, -2.0F, -7.0F));
        PartDefinition toothMid3 = jawMid.addOrReplaceChild("tooth_mid_3",
CubeListBuilder.create()
        .texOffs(250, 58).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(4.9F, -2.0F, -7.0F));
        PartDefinition toothR0 = jawR.addOrReplaceChild("tooth_r0",
CubeListBuilder.create()
        .texOffs(250, 70).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(-3.0F, -1.0F, -5.0F));
        PartDefinition toothR1 = jawR.addOrReplaceChild("tooth_r1",
CubeListBuilder.create()
        .texOffs(46, 82).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(2.0F, -1.0F, -5.0F));
        PartDefinition footL = skirt4.addOrReplaceChild("foot_l",
CubeListBuilder.create()
        .texOffs(182, 214).addBox(-9.0F, 0.0F, -6.0F, 6.0F, 3.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition footR = skirt4.addOrReplaceChild("foot_r",
CubeListBuilder.create()
        .texOffs(78, 215).addBox(3.0F, 0.0F, -6.0F, 6.0F, 3.0F, 8.0F, NONE)
                , PartPose.offset(0.0F, 4.0F, 0.0F));
        return LayerDefinition.create(mesh, 256, 256);
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
    public void setupAnim(CultivationMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float t = ageInTicks;
        float breathe = Mth.sin(t * 0.05F);
        float yaw = netHeadYaw * ((float) Math.PI / 180.0F);
        float pitch = headPitch * ((float) Math.PI / 180.0F);
        float atk = entity.getAttackAnim(0.0F);
        float swing = Mth.sin(atk * (float) Math.PI);
        float hurt = Mth.clamp((float) entity.hurtTime / 10.0F, 0.0F, 1.0F);
        float dead = Mth.clamp((float) entity.deathTime / 20.0F, 0.0F, 1.0F);

        // ---- 躯干呼吸（缩放，不动 y）+ 受击后仰 / 死亡前扑 ----
        this.chest.xScale = 1.0F + breathe * 0.012F;
        this.chest.zScale = 1.0F + breathe * 0.012F;
        this.chest.xRot = -0.02F + breathe * 0.01F - hurt * 0.10F + dead * 0.25F;
        this.chest.zRot = 0.0F;
        this.body.y = -dead * 2.0F;

        // ---- 三头：中央跟视线最紧，左右错相微转（相位差 2.1 rad）----
        float phaseL = Mth.sin(t * 0.07F);
        float phaseR = Mth.sin(t * 0.07F + 2.1F);
        this.headMid.yRot = yaw * 0.80F;
        this.headMid.xRot = pitch * 0.60F + breathe * 0.010F - swing * 0.08F;
        this.headMid.zRot = 0.0F;
        this.headL.yRot = yaw * 0.45F - 0.30F + phaseL * 0.06F;
        this.headL.xRot = pitch * 0.35F + phaseL * 0.04F - hurt * 0.10F;
        this.headL.zRot = phaseL * 0.02F;
        this.headR.yRot = yaw * 0.45F + 0.30F + phaseR * 0.06F;
        this.headR.xRot = pitch * 0.35F + phaseR * 0.04F - hurt * 0.10F;
        this.headR.zRot = phaseR * 0.02F;
        this.neckMid.yRot = yaw * 0.25F;
        this.neckMid.xRot = breathe * 0.015F;
        this.crownBase.yRot = yaw * 0.10F;
        this.crownGem.yRot = yaw * 0.10F;
        this.hornL1.zRot = -0.35F + phaseL * 0.02F;
        this.hornR1.zRot = 0.35F - phaseR * 0.02F;
        this.jawMid.xRot = 0.06F + swing * 0.35F;
        this.jawMid.yRot = 0.0F;
        this.jawL.xRot = 0.05F + swing * 0.20F;
        this.jawR.xRot = 0.05F + swing * 0.20F;
        this.eyeMid.zScale = 1.0F + Mth.sin(t * 0.13F) * 0.05F;

        // ---- 六臂：缓慢张合 + 出手时上抬/前压（三条臂相位错开）----
        float open1 = Mth.sin(t * 0.06F);
        float open2 = Mth.sin(t * 0.06F + 1.4F);
        float open3 = Mth.sin(t * 0.06F + 2.8F);
        this.armU1Left.zRot = -0.55F + open1 * 0.05F - swing * 0.20F;
        this.armU1Right.zRot = 0.55F - open1 * 0.05F + swing * 0.20F;
        this.armF1Left.xRot = 0.35F + breathe * 0.03F + swing * 0.30F;
        this.armF1Right.xRot = 0.35F + breathe * 0.03F + swing * 0.30F;
        this.blade1Left.xRot = 0.05F - swing * 0.45F;
        this.blade1Right.xRot = 0.05F - swing * 0.45F;
        this.armU2Left.zRot = -0.15F + open2 * 0.05F;
        this.armU2Right.zRot = 0.15F - open2 * 0.05F;
        this.armF2Left.xRot = 0.45F + open2 * 0.04F + swing * 0.20F;
        this.armF2Right.xRot = 0.45F + open2 * 0.04F + swing * 0.20F;
        this.hand2Left.xRot = swing * 0.15F;
        this.hand2Right.xRot = swing * 0.15F;
        this.armU3Left.zRot = 0.35F + open3 * 0.05F;
        this.armU3Right.zRot = -0.35F - open3 * 0.05F;
        this.armF3Left.xRot = 0.20F + open3 * 0.04F;
        this.armF3Right.xRot = 0.20F + open3 * 0.04F;
        this.hand3Left.xRot = open3 * 0.06F;
        this.hand3Right.xRot = -open3 * 0.06F;
        this.shoulderLeft.xRot = breathe * 0.02F;
        this.shoulderRight.xRot = breathe * 0.02F;

        // ---- 血袍裙甲 4 层：逐层错相摆动（下摆最飘）----
        this.skirt1.zRot = Mth.sin(t * 0.05F) * 0.02F;
        this.skirt2.zRot = Mth.sin(t * 0.05F + 0.6F) * 0.03F;
        this.skirt3.zRot = Mth.sin(t * 0.05F + 1.2F) * 0.035F;
        this.skirt4.zRot = Mth.sin(t * 0.05F + 1.8F) * 0.045F;
        this.skirt2.xRot = Mth.sin(t * 0.04F + 0.3F) * 0.015F;
        this.skirt3.xRot = Mth.sin(t * 0.04F + 0.9F) * 0.02F;
        this.skirt4.xRot = Mth.sin(t * 0.04F + 1.5F) * 0.025F;

        // ---- 胸口血核：脉动（比其它 Boss 更快、更像心跳）----
        float beat = 1.0F + Mth.sin(t * 0.25F) * 0.10F + swing * 0.12F;
        this.core.xScale = beat;
        this.core.yScale = beat;
        this.core.zScale = beat;
        this.core.yRot = t * 0.05F;

        // ---- 背部骨刺脊 / 蝠翼残片 / 血雾薄板：缓慢浮动 ----
        this.spine.xRot = -0.12F + Mth.sin(t * 0.04F) * 0.02F;
        this.wingBone1Left.zRot = -0.50F + Mth.sin(t * 0.08F) * 0.06F;
        this.wingBone1Right.zRot = 0.50F - Mth.sin(t * 0.08F + 1.3F) * 0.06F;
        this.wingBone2Left.zRot = -0.40F + Mth.sin(t * 0.08F + 0.7F) * 0.05F;
        this.wingBone2Right.zRot = 0.40F - Mth.sin(t * 0.08F + 2.0F) * 0.05F;
        this.wingMemLeft.zRot = Mth.sin(t * 0.06F) * 0.04F;
        this.wingMemRight.zRot = -Mth.sin(t * 0.06F + 0.9F) * 0.04F;
        this.mistA.yRot = 0.25F + Mth.sin(t * 0.04F) * 0.10F;
        this.mistB.yRot = -0.30F - Mth.sin(t * 0.04F + 1.0F) * 0.10F;

    }


    /** 最低点像素 y（血袍下摆底 = 24）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

}
