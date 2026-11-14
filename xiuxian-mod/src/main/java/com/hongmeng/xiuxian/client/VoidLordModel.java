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
 * 虚空道主 · VoidLord / VoidLordModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>浮空独眼虚无法相：无腿下摆收束 y=24、层叠法袍 3 层、独眼头（巨眼/眼睑/瞳孔/额冠）、6 片分节悬浮翼片、4 条手臂持虚空晶刃、4 块悬浮符文石 + 背后虚空环</p>
 */
public class VoidLordModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart root;
    private final ModelPart halo;
    private final ModelPart haloRunes;
    private final ModelPart robe;
    private final ModelPart rune0;
    private final ModelPart rune1;
    private final ModelPart rune2;
    private final ModelPart rune3;
    private final ModelPart hemB;
    private final ModelPart hemF;
    private final ModelPart hemL;
    private final ModelPart hemR;
    private final ModelPart tendril0;
    private final ModelPart tendril1;
    private final ModelPart tendril2;
    private final ModelPart tendril3;
    private final ModelPart tendril4;
    private final ModelPart tendril5;
    private final ModelPart tentAUp;
    private final ModelPart tentBUp;
    private final ModelPart waist;
    private final ModelPart tentATip;
    private final ModelPart tentBTip;
    private final ModelPart torso;
    private final ModelPart armLlUp;
    private final ModelPart armLsUp;
    private final ModelPart armRlUp;
    private final ModelPart armRsUp;
    private final ModelPart neck;
    private final ModelPart pauldronL;
    private final ModelPart pauldronR;
    private final ModelPart wingL0A;
    private final ModelPart wingL1A;
    private final ModelPart wingL2A;
    private final ModelPart wingR0A;
    private final ModelPart wingR1A;
    private final ModelPart wingR2A;
    private final ModelPart armLlEl;
    private final ModelPart armLsEl;
    private final ModelPart armRlEl;
    private final ModelPart armRsEl;
    private final ModelPart head;
    private final ModelPart wingL0B;
    private final ModelPart wingL1B;
    private final ModelPart wingL2B;
    private final ModelPart wingR0B;
    private final ModelPart wingR1B;
    private final ModelPart wingR2B;
    private final ModelPart armLlHand;
    private final ModelPart armLsHand;
    private final ModelPart armRlHand;
    private final ModelPart armRsHand;
    private final ModelPart crackA;
    private final ModelPart crackB;
    private final ModelPart eye;
    private final ModelPart lidDn;
    private final ModelPart lidUp;
    private final ModelPart pupil;
    private final ModelPart wingL0C;
    private final ModelPart wingL1C;
    private final ModelPart wingL2C;
    private final ModelPart wingR0C;
    private final ModelPart wingR1C;
    private final ModelPart wingR2C;

    public VoidLordModel(ModelPart root) {
        this.root = root;
        this.halo = root.getChild("halo");
        this.haloRunes = root.getChild("halo_runes");
        this.robe = root.getChild("robe");
        this.rune0 = root.getChild("rune0");
        this.rune1 = root.getChild("rune1");
        this.rune2 = root.getChild("rune2");
        this.rune3 = root.getChild("rune3");
        this.hemB = root.getChild("robe").getChild("hem_b");
        this.hemF = root.getChild("robe").getChild("hem_f");
        this.hemL = root.getChild("robe").getChild("hem_l");
        this.hemR = root.getChild("robe").getChild("hem_r");
        this.tendril0 = root.getChild("robe").getChild("tendril0");
        this.tendril1 = root.getChild("robe").getChild("tendril1");
        this.tendril2 = root.getChild("robe").getChild("tendril2");
        this.tendril3 = root.getChild("robe").getChild("tendril3");
        this.tendril4 = root.getChild("robe").getChild("tendril4");
        this.tendril5 = root.getChild("robe").getChild("tendril5");
        this.tentAUp = root.getChild("robe").getChild("tent_a_up");
        this.tentBUp = root.getChild("robe").getChild("tent_b_up");
        this.waist = root.getChild("robe").getChild("waist");
        this.tentATip = root.getChild("robe").getChild("tent_a_up").getChild("tent_a_tip");
        this.tentBTip = root.getChild("robe").getChild("tent_b_up").getChild("tent_b_tip");
        this.torso = root.getChild("robe").getChild("waist").getChild("torso");
        this.armLlUp = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_ll_up");
        this.armLsUp = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_ls_up");
        this.armRlUp = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_rl_up");
        this.armRsUp = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_rs_up");
        this.neck = root.getChild("robe").getChild("waist").getChild("torso").getChild("neck");
        this.pauldronL = root.getChild("robe").getChild("waist").getChild("torso").getChild("pauldron_l");
        this.pauldronR = root.getChild("robe").getChild("waist").getChild("torso").getChild("pauldron_r");
        this.wingL0A = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_0_a");
        this.wingL1A = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_1_a");
        this.wingL2A = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_2_a");
        this.wingR0A = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_0_a");
        this.wingR1A = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_1_a");
        this.wingR2A = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_2_a");
        this.armLlEl = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_ll_up").getChild("arm_ll_el");
        this.armLsEl = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_ls_up").getChild("arm_ls_el");
        this.armRlEl = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_rl_up").getChild("arm_rl_el");
        this.armRsEl = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_rs_up").getChild("arm_rs_el");
        this.head = root.getChild("robe").getChild("waist").getChild("torso").getChild("neck").getChild("head");
        this.wingL0B = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_0_a").getChild("wing_l_0_b");
        this.wingL1B = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_1_a").getChild("wing_l_1_b");
        this.wingL2B = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_2_a").getChild("wing_l_2_b");
        this.wingR0B = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_0_a").getChild("wing_r_0_b");
        this.wingR1B = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_1_a").getChild("wing_r_1_b");
        this.wingR2B = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_2_a").getChild("wing_r_2_b");
        this.armLlHand = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_ll_up").getChild("arm_ll_el").getChild("arm_ll_hand");
        this.armLsHand = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_ls_up").getChild("arm_ls_el").getChild("arm_ls_hand");
        this.armRlHand = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_rl_up").getChild("arm_rl_el").getChild("arm_rl_hand");
        this.armRsHand = root.getChild("robe").getChild("waist").getChild("torso").getChild("arm_rs_up").getChild("arm_rs_el").getChild("arm_rs_hand");
        this.crackA = root.getChild("robe").getChild("waist").getChild("torso").getChild("neck").getChild("head").getChild("crack_a");
        this.crackB = root.getChild("robe").getChild("waist").getChild("torso").getChild("neck").getChild("head").getChild("crack_b");
        this.eye = root.getChild("robe").getChild("waist").getChild("torso").getChild("neck").getChild("head").getChild("eye");
        this.lidDn = root.getChild("robe").getChild("waist").getChild("torso").getChild("neck").getChild("head").getChild("lid_dn");
        this.lidUp = root.getChild("robe").getChild("waist").getChild("torso").getChild("neck").getChild("head").getChild("lid_up");
        this.pupil = root.getChild("robe").getChild("waist").getChild("torso").getChild("neck").getChild("head").getChild("pupil");
        this.wingL0C = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_0_a").getChild("wing_l_0_b").getChild("wing_l_0_c");
        this.wingL1C = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_1_a").getChild("wing_l_1_b").getChild("wing_l_1_c");
        this.wingL2C = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_l_2_a").getChild("wing_l_2_b").getChild("wing_l_2_c");
        this.wingR0C = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_0_a").getChild("wing_r_0_b").getChild("wing_r_0_c");
        this.wingR1C = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_1_a").getChild("wing_r_1_b").getChild("wing_r_1_c");
        this.wingR2C = root.getChild("robe").getChild("waist").getChild("torso").getChild("wing_r_2_a").getChild("wing_r_2_b").getChild("wing_r_2_c");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition halo = root.addOrReplaceChild("halo",
CubeListBuilder.create()
        .texOffs(200, 144).addBox(17.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
        .texOffs(0, 209).addBox(11.1F, 13.1F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
        .texOffs(17, 209).addBox(-3.0F, 19.0F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
        .texOffs(34, 209).addBox(-17.1F, 13.1F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
        .texOffs(51, 209).addBox(-23.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
        .texOffs(184, 212).addBox(-17.1F, -15.1F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
        .texOffs(201, 212).addBox(-3.0F, -21.0F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
        .texOffs(68, 213).addBox(11.1F, -15.1F, -1.0F, 6.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -56.0F, 14.0F));
        PartDefinition haloRunes = root.addOrReplaceChild("halo_runes",
CubeListBuilder.create()
        .texOffs(140, 207).addBox(18.0F, -2.0F, -2.0F, 4.0F, 4.0F, 1.0F, NONE)
        .texOffs(151, 207).addBox(-2.0F, 18.0F, -2.0F, 4.0F, 4.0F, 1.0F, NONE)
        .texOffs(162, 207).addBox(-22.0F, -2.0F, -2.0F, 4.0F, 4.0F, 1.0F, NONE)
        .texOffs(173, 207).addBox(-2.0F, -22.0F, -2.0F, 4.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -56.0F, 14.0F));
        PartDefinition robe = root.addOrReplaceChild("robe",
CubeListBuilder.create()
        .texOffs(0, 66).addBox(-20.0F, -4.0F, -9.0F, 40.0F, 4.0F, 18.0F, NONE)
        .texOffs(117, 68).addBox(-17.0F, -10.0F, -7.0F, 34.0F, 6.0F, 14.0F, NONE)
        .texOffs(53, 110).addBox(-14.0F, -16.0F, -6.0F, 28.0F, 6.0F, 12.0F, NONE)
        .texOffs(43, 145).addBox(-9.0F, -20.0F, -4.0F, 18.0F, 4.0F, 8.0F, NONE)
        .texOffs(122, 89).addBox(-12.0F, -22.0F, 8.0F, 24.0F, 18.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition rune0 = root.addOrReplaceChild("rune0",
CubeListBuilder.create()
        .texOffs(0, 147).addBox(-3.0F, -3.0F, -2.0F, 7.0F, 7.0F, 5.0F, NONE)
        .texOffs(244, 173).addBox(-2.0F, -2.0F, -3.0F, 5.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(30.0F, -50.0F, 6.0F));
        PartDefinition rune1 = root.addOrReplaceChild("rune1",
CubeListBuilder.create()
        .texOffs(200, 149).addBox(-3.0F, -3.0F, -2.0F, 7.0F, 7.0F, 5.0F, NONE)
        .texOffs(234, 199).addBox(-2.0F, -2.0F, -3.0F, 5.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(-32.0F, -40.0F, -5.0F));
        PartDefinition rune2 = root.addOrReplaceChild("rune2",
CubeListBuilder.create()
        .texOffs(225, 149).addBox(-3.0F, -3.0F, -2.0F, 7.0F, 7.0F, 5.0F, NONE)
        .texOffs(229, 206).addBox(-2.0F, -2.0F, -3.0F, 5.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(24.0F, -32.0F, 10.0F));
        PartDefinition rune3 = root.addOrReplaceChild("rune3",
CubeListBuilder.create()
        .texOffs(25, 158).addBox(-3.0F, -3.0F, -2.0F, 7.0F, 7.0F, 5.0F, NONE)
        .texOffs(242, 206).addBox(-2.0F, -2.0F, -3.0F, 5.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(-26.0F, -68.0F, 2.0F));
        PartDefinition hemB = robe.addOrReplaceChild("hem_b",
CubeListBuilder.create()
        .texOffs(176, 162).addBox(-18.0F, -8.0F, -1.0F, 36.0F, 8.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 9.0F));
        PartDefinition hemF = robe.addOrReplaceChild("hem_f",
CubeListBuilder.create()
        .texOffs(50, 159).addBox(-20.0F, -8.0F, -2.0F, 40.0F, 8.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, -9.0F));
        PartDefinition hemL = robe.addOrReplaceChild("hem_l",
CubeListBuilder.create()
        .texOffs(45, 39).addBox(-2.0F, -8.0F, -9.0F, 3.0F, 8.0F, 18.0F, NONE)
                , PartPose.offset(-20.0F, 0.0F, 0.0F));
        PartDefinition hemR = robe.addOrReplaceChild("hem_r",
CubeListBuilder.create()
        .texOffs(88, 39).addBox(-1.0F, -8.0F, -9.0F, 3.0F, 8.0F, 18.0F, NONE)
                , PartPose.offset(20.0F, 0.0F, 0.0F));
        PartDefinition tendril0 = robe.addOrReplaceChild("tendril0",
CubeListBuilder.create()
        .texOffs(184, 128).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, NONE)
                , PartPose.offset(-14.0F, 0.0F, -10.0F));
        PartDefinition tendril1 = robe.addOrReplaceChild("tendril1",
CubeListBuilder.create()
        .texOffs(118, 129).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, NONE)
                , PartPose.offset(13.0F, 0.0F, -11.0F));
        PartDefinition tendril2 = robe.addOrReplaceChild("tendril2",
CubeListBuilder.create()
        .texOffs(43, 131).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, NONE)
                , PartPose.offset(-11.0F, 0.0F, 12.0F));
        PartDefinition tendril3 = robe.addOrReplaceChild("tendril3",
CubeListBuilder.create()
        .texOffs(241, 135).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, NONE)
                , PartPose.offset(10.0F, 0.0F, 9.0F));
        PartDefinition tendril4 = robe.addOrReplaceChild("tendril4",
CubeListBuilder.create()
        .texOffs(96, 145).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 10.0F));
        PartDefinition tendril5 = robe.addOrReplaceChild("tendril5",
CubeListBuilder.create()
        .texOffs(105, 145).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, NONE)
                , PartPose.offset(-2.0F, 0.0F, -9.0F));
        PartDefinition tentAUp = robe.addOrReplaceChild("tent_a_up",
CubeListBuilder.create()
        .texOffs(214, 68).addBox(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F, NONE)
                , PartPose.offset(16.0F, 0.0F, 5.0F));
        PartDefinition tentBUp = robe.addOrReplaceChild("tent_b_up",
CubeListBuilder.create()
        .texOffs(211, 127).addBox(-1.0F, -13.0F, -1.0F, 2.0F, 13.0F, 2.0F, NONE)
                , PartPose.offset(-15.0F, 0.0F, 6.0F));
        PartDefinition waist = robe.addOrReplaceChild("waist",
CubeListBuilder.create()
        .texOffs(65, 89).addBox(-9.0F, -6.0F, -5.0F, 18.0F, 10.0F, 10.0F, NONE)
        .texOffs(53, 129).addBox(-10.0F, -6.0F, -6.0F, 20.0F, 3.0F, 12.0F, NONE)
        .texOffs(118, 144).addBox(-9.0F, -3.0F, -6.0F, 18.0F, 2.0F, 12.0F, NONE)
        .texOffs(82, 181).addBox(-2.0F, -6.0F, -9.0F, 4.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -20.0F, 0.0F));
        PartDefinition tentATip = tentAUp.addOrReplaceChild("tent_a_tip",
CubeListBuilder.create()
        .texOffs(127, 129).addBox(-1.0F, -9.0F, -1.0F, 1.0F, 9.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -14.0F, 0.0F));
        PartDefinition tentBTip = tentBUp.addOrReplaceChild("tent_b_tip",
CubeListBuilder.create()
        .texOffs(219, 25).addBox(-1.0F, -8.0F, -1.0F, 1.0F, 8.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -13.0F, 0.0F));
        PartDefinition torso = waist.addOrReplaceChild("torso",
CubeListBuilder.create()
        .texOffs(134, 0).addBox(-15.0F, -24.0F, -6.0F, 30.0F, 22.0F, 12.0F, NONE)
        .texOffs(134, 35).addBox(-16.0F, -22.0F, -7.0F, 32.0F, 18.0F, 14.0F, NONE)
        .texOffs(134, 110).addBox(-14.0F, -20.0F, -8.0F, 28.0F, 16.0F, 1.0F, NONE)
        .texOffs(237, 0).addBox(-2.0F, -24.0F, 7.0F, 4.0F, 20.0F, 1.0F, NONE)
        .texOffs(75, 171).addBox(-4.0F, -18.0F, -9.0F, 8.0F, 8.0F, 1.0F, NONE)
        .texOffs(218, 110).addBox(-15.0F, -20.0F, -8.0F, 2.0F, 14.0F, 2.0F, NONE)
        .texOffs(227, 118).addBox(13.0F, -20.0F, -8.0F, 2.0F, 14.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -6.0F, 0.0F));
        PartDefinition armLlUp = torso.addOrReplaceChild("arm_ll_up",
CubeListBuilder.create()
        .texOffs(228, 23).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F, NONE)
                , PartPose.offset(14.0F, -22.0F, 0.0F));
        PartDefinition armLsUp = torso.addOrReplaceChild("arm_ls_up",
CubeListBuilder.create()
        .texOffs(228, 101).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.0F, 6.0F, NONE)
                , PartPose.offset(15.0F, -14.0F, 4.0F));
        PartDefinition armRlUp = torso.addOrReplaceChild("arm_rl_up",
CubeListBuilder.create()
        .texOffs(0, 41).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F, NONE)
                , PartPose.offset(-14.0F, -22.0F, 0.0F));
        PartDefinition armRsUp = torso.addOrReplaceChild("arm_rs_up",
CubeListBuilder.create()
        .texOffs(193, 110).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.0F, 6.0F, NONE)
                , PartPose.offset(-15.0F, -14.0F, 4.0F));
        PartDefinition neck = torso.addOrReplaceChild("neck",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-6.0F, -30.0F, -5.0F, 12.0F, 30.0F, 10.0F, NONE)
        .texOffs(0, 89).addBox(-8.0F, -4.0F, -8.0F, 16.0F, 4.0F, 16.0F, NONE)
        .texOffs(219, 0).addBox(8.0F, -28.0F, 6.0F, 2.0F, 22.0F, 2.0F, NONE)
        .texOffs(248, 0).addBox(-9.0F, -28.0F, 6.0F, 2.0F, 18.0F, 2.0F, NONE)
        .texOffs(129, 207).addBox(-2.0F, -8.0F, -6.0F, 4.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -24.0F, 0.0F));
        PartDefinition pauldronL = torso.addOrReplaceChild("pauldron_l",
CubeListBuilder.create()
        .texOffs(175, 89).addBox(-11.0F, -3.0F, -7.0F, 12.0F, 6.0F, 14.0F, NONE)
        .texOffs(134, 128).addBox(-10.0F, -6.0F, -6.0F, 10.0F, 4.0F, 11.0F, NONE)
        .texOffs(25, 147).addBox(-11.0F, -9.0F, -3.0F, 3.0F, 3.0F, 4.0F, NONE)
                , PartPose.offset(-14.0F, -24.0F, 0.0F));
        PartDefinition pauldronR = torso.addOrReplaceChild("pauldron_r",
CubeListBuilder.create()
        .texOffs(0, 110).addBox(0.0F, -3.0F, -7.0F, 12.0F, 6.0F, 14.0F, NONE)
        .texOffs(0, 131).addBox(0.0F, -6.0F, -6.0F, 10.0F, 4.0F, 11.0F, NONE)
        .texOffs(229, 173).addBox(8.0F, -9.0F, -3.0F, 3.0F, 3.0F, 4.0F, NONE)
                , PartPose.offset(14.0F, -24.0F, 0.0F));
        PartDefinition wingL0A = torso.addOrReplaceChild("wing_l_0_a",
CubeListBuilder.create()
        .texOffs(94, 171).addBox(2.0F, -3.0F, -1.0F, 20.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(12.0F, -22.0F, 3.0F, 0.0F, 0.0F, -0.6F));
        PartDefinition wingL1A = torso.addOrReplaceChild("wing_l_1_a",
CubeListBuilder.create()
        .texOffs(139, 173).addBox(2.0F, -3.0F, -1.0F, 20.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(12.0F, -15.0F, 3.0F, 0.0F, 0.0F, -0.2F));
        PartDefinition wingL2A = torso.addOrReplaceChild("wing_l_2_a",
CubeListBuilder.create()
        .texOffs(139, 182).addBox(2.0F, -3.0F, -1.0F, 18.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(12.0F, -8.0F, 3.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition wingR0A = torso.addOrReplaceChild("wing_r_0_a",
CubeListBuilder.create()
        .texOffs(184, 173).addBox(-22.0F, -3.0F, -1.0F, 20.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-12.0F, -22.0F, 3.0F, 0.0F, 0.0F, 0.6F));
        PartDefinition wingR1A = torso.addOrReplaceChild("wing_r_1_a",
CubeListBuilder.create()
        .texOffs(94, 180).addBox(-22.0F, -3.0F, -1.0F, 20.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-12.0F, -15.0F, 3.0F, 0.0F, 0.0F, 0.2F));
        PartDefinition wingR2A = torso.addOrReplaceChild("wing_r_2_a",
CubeListBuilder.create()
        .texOffs(0, 191).addBox(-20.0F, -3.0F, -1.0F, 18.0F, 6.0F, 2.0F, NONE)
                , PartPose.offsetAndRotation(-12.0F, -8.0F, 3.0F, 0.0F, 0.0F, -0.3F));
        PartDefinition armLlEl = armLlUp.addOrReplaceChild("arm_ll_el",
CubeListBuilder.create()
        .texOffs(236, 65).addBox(-2.0F, 0.0F, -2.0F, 5.0F, 12.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition armLsEl = armLsUp.addOrReplaceChild("arm_ls_el",
CubeListBuilder.create()
        .texOffs(220, 135).addBox(-2.0F, 0.0F, -2.0F, 5.0F, 8.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition armRlEl = armRlUp.addOrReplaceChild("arm_rl_el",
CubeListBuilder.create()
        .texOffs(236, 83).addBox(-2.0F, 0.0F, -2.0F, 5.0F, 12.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition armRsEl = armRsUp.addOrReplaceChild("arm_rs_el",
CubeListBuilder.create()
        .texOffs(179, 144).addBox(-2.0F, 0.0F, -2.0F, 5.0F, 8.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(45, 0).addBox(-14.0F, -22.0F, -8.0F, 28.0F, 22.0F, 16.0F, NONE)
        .texOffs(115, 200).addBox(-15.0F, -19.0F, -9.0F, 30.0F, 4.0F, 2.0F, NONE)
        .texOffs(180, 200).addBox(-11.0F, -6.0F, -9.0F, 22.0F, 4.0F, 2.0F, NONE)
        .texOffs(70, 207).addBox(-4.0F, -24.0F, -9.0F, 8.0F, 3.0F, 2.0F, NONE)
        .texOffs(91, 207).addBox(-12.0F, -24.0F, -7.0F, 7.0F, 3.0F, 2.0F, NONE)
        .texOffs(110, 207).addBox(5.0F, -24.0F, -7.0F, 7.0F, 3.0F, 2.0F, NONE)
        .texOffs(25, 41).addBox(-19.0F, -20.0F, -5.0F, 4.0F, 16.0F, 4.0F, NONE)
        .texOffs(227, 44).addBox(15.0F, -20.0F, -5.0F, 4.0F, 16.0F, 4.0F, NONE)
        .texOffs(244, 44).addBox(11.0F, -20.0F, 8.0F, 2.0F, 18.0F, 2.0F, NONE)
        .texOffs(228, 0).addBox(0.0F, -22.0F, 9.0F, 2.0F, 20.0F, 2.0F, NONE)
        .texOffs(227, 65).addBox(-11.0F, -18.0F, 8.0F, 2.0F, 16.0F, 2.0F, NONE)
        .texOffs(184, 139).addBox(8.0F, -23.0F, -3.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(247, 199).addBox(-10.0F, -23.0F, 1.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(85, 213).addBox(-11.0F, -19.0F, -9.0F, 22.0F, 2.0F, 1.0F, NONE)
        .texOffs(132, 213).addBox(-11.0F, -7.0F, -9.0F, 22.0F, 2.0F, 1.0F, NONE)
        .texOffs(228, 84).addBox(-12.0F, -19.0F, -9.0F, 2.0F, 12.0F, 1.0F, NONE)
        .texOffs(177, 128).addBox(10.0F, -19.0F, -9.0F, 2.0F, 12.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -30.0F, 0.0F));
        PartDefinition wingL0B = wingL0A.addOrReplaceChild("wing_l_0_b",
CubeListBuilder.create()
        .texOffs(0, 182).addBox(2.0F, -3.0F, -1.0F, 18.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(22.0F, 0.0F, 0.0F));
        PartDefinition wingL1B = wingL1A.addOrReplaceChild("wing_l_1_b",
CubeListBuilder.create()
        .texOffs(41, 182).addBox(2.0F, -3.0F, -1.0F, 18.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(22.0F, 0.0F, 0.0F));
        PartDefinition wingL2B = wingL2A.addOrReplaceChild("wing_l_2_b",
CubeListBuilder.create()
        .texOffs(160, 191).addBox(2.0F, -3.0F, -1.0F, 16.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(20.0F, 0.0F, 0.0F));
        PartDefinition wingR0B = wingR0A.addOrReplaceChild("wing_r_0_b",
CubeListBuilder.create()
        .texOffs(180, 182).addBox(-20.0F, -3.0F, -1.0F, 18.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-22.0F, 0.0F, 0.0F));
        PartDefinition wingR1B = wingR1A.addOrReplaceChild("wing_r_1_b",
CubeListBuilder.create()
        .texOffs(82, 189).addBox(-20.0F, -3.0F, -1.0F, 18.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-22.0F, 0.0F, 0.0F));
        PartDefinition wingR2B = wingR2A.addOrReplaceChild("wing_r_2_b",
CubeListBuilder.create()
        .texOffs(0, 200).addBox(-18.0F, -3.0F, -1.0F, 16.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-20.0F, 0.0F, 0.0F));
        PartDefinition armLlHand = armLlEl.addOrReplaceChild("arm_ll_hand",
CubeListBuilder.create()
        .texOffs(0, 160).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F, NONE)
        .texOffs(236, 118).addBox(1.0F, 2.0F, -1.0F, 2.0F, 14.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition armLsHand = armLsEl.addOrReplaceChild("arm_ls_hand",
CubeListBuilder.create()
        .texOffs(25, 171).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F, NONE)
        .texOffs(193, 127).addBox(1.0F, 2.0F, -1.0F, 2.0F, 14.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition armRlHand = armRlEl.addOrReplaceChild("arm_rl_hand",
CubeListBuilder.create()
        .texOffs(0, 171).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F, NONE)
        .texOffs(245, 118).addBox(-3.0F, 2.0F, -1.0F, 2.0F, 14.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition armRsHand = armRsEl.addOrReplaceChild("arm_rs_hand",
CubeListBuilder.create()
        .texOffs(50, 171).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F, NONE)
        .texOffs(202, 127).addBox(-3.0F, 2.0F, -1.0F, 2.0F, 14.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition crackA = head.addOrReplaceChild("crack_a",
CubeListBuilder.create()
        .texOffs(220, 127).addBox(0.0F, -2.0F, 0.0F, 1.0F, 5.0F, 1.0F, NONE)
        .texOffs(214, 85).addBox(-1.0F, 2.0F, 0.0F, 3.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(-10.0F, -16.0F, -10.0F));
        PartDefinition crackB = head.addOrReplaceChild("crack_b",
CubeListBuilder.create()
        .texOffs(250, 135).addBox(0.0F, -1.0F, 0.0F, 1.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(10.0F, -14.0F, -10.0F));
        PartDefinition eye = head.addOrReplaceChild("eye",
CubeListBuilder.create()
        .texOffs(137, 159).addBox(-9.0F, -5.0F, 0.0F, 18.0F, 10.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -13.0F, -10.0F));
        PartDefinition lidDn = head.addOrReplaceChild("lid_dn",
CubeListBuilder.create()
        .texOffs(0, 62).addBox(-10.0F, 0.0F, 0.0F, 20.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -8.0F, -10.0F));
        PartDefinition lidUp = head.addOrReplaceChild("lid_up",
CubeListBuilder.create()
        .texOffs(184, 207).addBox(-10.0F, -2.0F, 0.0F, 20.0F, 3.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -18.0F, -10.0F));
        PartDefinition pupil = head.addOrReplaceChild("pupil",
CubeListBuilder.create()
        .texOffs(234, 191).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -13.0F, -11.0F));
        PartDefinition wingL0C = wingL0B.addOrReplaceChild("wing_l_0_c",
CubeListBuilder.create()
        .texOffs(41, 191).addBox(2.0F, -3.0F, -1.0F, 16.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(20.0F, 0.0F, 0.0F));
        PartDefinition wingL1C = wingL1B.addOrReplaceChild("wing_l_1_c",
CubeListBuilder.create()
        .texOffs(123, 191).addBox(2.0F, -3.0F, -1.0F, 16.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(20.0F, 0.0F, 0.0F));
        PartDefinition wingL2C = wingL2B.addOrReplaceChild("wing_l_2_c",
CubeListBuilder.create()
        .texOffs(221, 182).addBox(2.0F, -3.0F, -1.0F, 14.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(18.0F, 0.0F, 0.0F));
        PartDefinition wingR0C = wingR0B.addOrReplaceChild("wing_r_0_c",
CubeListBuilder.create()
        .texOffs(197, 191).addBox(-18.0F, -3.0F, -1.0F, 16.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-20.0F, 0.0F, 0.0F));
        PartDefinition wingR1C = wingR1B.addOrReplaceChild("wing_r_1_c",
CubeListBuilder.create()
        .texOffs(78, 198).addBox(-18.0F, -3.0F, -1.0F, 16.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-20.0F, 0.0F, 0.0F));
        PartDefinition wingR2C = wingR2B.addOrReplaceChild("wing_r_2_c",
CubeListBuilder.create()
        .texOffs(37, 200).addBox(-16.0F, -3.0F, -1.0F, 14.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(-18.0F, 0.0F, 0.0F));
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

        final float DEG = 0.017453292F;
        float yaw = netHeadYaw * DEG;
        float pitch = headPitch * DEG;
        int phase = Math.max(1, Math.min(3, entity.bossPhase()));
        float t = ageInTicks;
        float hurt = entity.hurtTime > 0 ? (float) entity.hurtTime / 10.0F : 0.0F;
        float floatY = 2.0F * Mth.sin(t * 0.05F);              // 悬浮上下 ±2px
        float spread = 1.0F + 0.16F * (float) (phase - 1);     // 阶段越高翼/臂张得越开
        float t2 = t * 0.04F;

        // ---- 下摆/布幔/触须：慢摆（错相）----
        this.robe.zRot = 0.010F * Mth.sin(t * 0.025F);
        this.robe.yRot = 0.012F * Mth.sin(t * 0.019F);
        this.hemF.xRot = 0.05F + 0.035F * Mth.sin(t * 0.045F);
        this.hemB.xRot = -0.04F + 0.030F * Mth.sin(t * 0.041F + 1.7F);
        this.hemL.zRot = 0.06F + 0.040F * Mth.sin(t * 0.037F + 0.8F);
        this.hemR.zRot = -0.06F - 0.040F * Mth.sin(t * 0.039F + 2.1F);
        this.tendril0.xRot = 0.12F * Mth.sin(t * 0.060F);
        this.tendril1.xRot = 0.10F * Mth.sin(t * 0.055F + 1.1F);
        this.tendril2.xRot = 0.11F * Mth.sin(t * 0.062F + 2.3F);
        this.tendril3.xRot = 0.09F * Mth.sin(t * 0.051F + 3.4F);
        this.tendril4.xRot = 0.13F * Mth.sin(t * 0.066F + 4.2F);
        this.tendril5.xRot = 0.10F * Mth.sin(t * 0.058F + 5.1F);
        this.tendril0.zRot = 0.06F * Mth.sin(t * 0.047F + 0.3F);
        this.tendril2.zRot = 0.05F * Mth.sin(t * 0.049F + 1.5F);
        this.tentAUp.xRot = 0.10F + 0.08F * Mth.sin(t * 0.042F);
        this.tentATip.xRot = -0.18F + 0.10F * Mth.sin(t * 0.042F + 1.4F);
        this.tentBUp.xRot = 0.08F + 0.07F * Mth.sin(t * 0.038F + 2.0F);
        this.tentBTip.xRot = -0.16F + 0.09F * Mth.sin(t * 0.038F + 3.1F);

        // ---- 上身悬浮（腰以上整体上下 ±2px）+ 呼吸 ----
        this.waist.y = 4.0F + floatY;
        this.waist.xRot = 0.02F * Mth.sin(t * 0.033F) - 0.05F * hurt;
        this.torso.xRot = 0.018F * Mth.sin(t * 0.028F) + 0.030F * Mth.sin(t * 0.021F);
        this.torso.yRot = yaw * 0.18F;
        this.torso.zRot = 0.010F * Mth.sin(t * 0.024F);
        this.pauldronL.xRot = 0.03F * Mth.sin(t * 0.026F);
        this.pauldronR.xRot = 0.03F * Mth.sin(t * 0.026F + 1.6F);

        // ---- 颈/头：跟随视线；独眼眨眼 + 瞳孔移动 ----
        this.neck.xRot = -pitch * 0.15F;
        this.head.yRot = yaw * 0.75F + 0.035F * Mth.sin(t * 0.045F);
        this.head.xRot = -pitch * 0.55F + 0.025F * Mth.sin(t * 0.041F);
        this.head.zRot = 0.020F * Mth.sin(t * 0.033F);
        float blinkCyc = t % 96.0F;
        float blink = blinkCyc < 7.0F
                ? Mth.sin(blinkCyc / 7.0F * (float) Math.PI) : 0.0F;
        this.lidUp.y = -18.0F + 4.5F * blink;
        this.lidDn.y = -8.0F - 3.5F * blink;
        this.lidUp.xScale = 1.0F + 0.06F * blink;
        this.lidDn.xScale = 1.0F + 0.06F * blink;
        this.eye.xScale = 1.0F + 0.05F * Mth.sin(t * 0.055F)
                + 0.09F * (float) (phase - 1);
        this.eye.yScale = 1.0F + 0.04F * Mth.sin(t * 0.048F + 0.7F)
                + 0.06F * (float) (phase - 1);
        this.pupil.x = 2.8F * Mth.sin(t * 0.023F);
        this.pupil.y = -13.0F + 2.0F * Mth.cos(t * 0.019F);
        this.pupil.xScale = 1.0F - 0.06F * Mth.sin(t * 0.031F);
        this.crackA.visible = phase >= 2;
        this.crackA.xScale = 1.0F + 0.15F * (float) (phase - 2);
        this.crackB.visible = phase >= 3;
        this.crackB.xScale = 1.0F + 0.25F * Mth.sin(t * 0.09F);

        // ---- 六片悬浮翼：错相浮动（每片 3 节各自相位）----
        this.wingL0A.zRot = -0.62F * spread + 0.06F * Mth.sin(t2);
        this.wingL0A.xRot = 0.05F * Mth.sin(t * 0.030F + 0.4F);
        this.wingL0B.zRot = 0.05F * Mth.sin(t * 0.045F + 0.9F);
        this.wingL0C.zRot = 0.04F * Mth.sin(t * 0.052F + 1.4F);
        this.wingR0A.zRot = 0.62F * spread - 0.06F * Mth.sin(t2 + 0.8F);
        this.wingR0A.xRot = 0.05F * Mth.sin(t * 0.030F + 1.2F);
        this.wingR0B.zRot = -0.05F * Mth.sin(t * 0.045F + 1.7F);
        this.wingR0C.zRot = -0.04F * Mth.sin(t * 0.052F + 2.2F);
        this.wingL1A.zRot = -0.20F * spread + 0.05F * Mth.sin(t2 + 1.1F);
        this.wingL1A.xRot = 0.04F * Mth.sin(t * 0.034F + 2.0F);
        this.wingL1B.zRot = 0.05F * Mth.sin(t * 0.048F + 2.8F);
        this.wingL1C.zRot = 0.04F * Mth.sin(t * 0.055F + 3.3F);
        this.wingR1A.zRot = 0.20F * spread - 0.05F * Mth.sin(t2 + 1.9F);
        this.wingR1A.xRot = 0.04F * Mth.sin(t * 0.034F + 2.8F);
        this.wingR1B.zRot = -0.05F * Mth.sin(t * 0.048F + 3.6F);
        this.wingR1C.zRot = -0.04F * Mth.sin(t * 0.055F + 4.1F);
        this.wingL2A.zRot = 0.26F * spread + 0.05F * Mth.sin(t2 + 2.2F);
        this.wingL2A.xRot = 0.04F * Mth.sin(t * 0.038F + 3.6F);
        this.wingL2B.zRot = 0.04F * Mth.sin(t * 0.051F + 4.4F);
        this.wingL2C.zRot = 0.03F * Mth.sin(t * 0.058F + 4.9F);
        this.wingR2A.zRot = -0.26F * spread - 0.05F * Mth.sin(t2 + 3.0F);
        this.wingR2A.xRot = 0.04F * Mth.sin(t * 0.038F + 4.4F);
        this.wingR2B.zRot = -0.04F * Mth.sin(t * 0.051F + 5.2F);
        this.wingR2C.zRot = -0.03F * Mth.sin(t * 0.058F + 5.7F);

        // ---- 四条手臂：缓慢张合（长臂主攻、短臂护体）----
        float armOut = 0.16F * spread;
        this.armLlUp.zRot = -armOut + 0.06F * Mth.sin(t * 0.033F);
        this.armLlUp.xRot = 0.06F * Mth.sin(t * 0.028F + 0.5F);
        this.armLlEl.xRot = 0.18F + 0.10F * Mth.sin(t * 0.037F + 1.1F);
        this.armLlHand.xRot = 0.10F * Mth.sin(t * 0.043F + 2.0F);
        this.armRlUp.zRot = armOut + 0.06F * Mth.sin(t * 0.033F + 1.7F);
        this.armRlUp.xRot = 0.06F * Mth.sin(t * 0.028F + 2.2F);
        this.armRlEl.xRot = 0.18F + 0.10F * Mth.sin(t * 0.037F + 2.8F);
        this.armRlHand.xRot = 0.10F * Mth.sin(t * 0.043F + 3.7F);
        this.armLsUp.zRot = -0.10F * spread + 0.05F * Mth.sin(t * 0.036F + 3.0F);
        this.armLsUp.xRot = 0.04F * Mth.sin(t * 0.031F + 4.0F);
        this.armLsEl.xRot = 0.12F + 0.08F * Mth.sin(t * 0.041F + 4.6F);
        this.armLsHand.xRot = 0.06F * Mth.sin(t * 0.047F + 5.2F);
        this.armRsUp.zRot = 0.10F * spread - 0.05F * Mth.sin(t * 0.036F + 4.4F);
        this.armRsUp.xRot = 0.04F * Mth.sin(t * 0.031F + 5.4F);
        this.armRsEl.xRot = 0.12F + 0.08F * Mth.sin(t * 0.041F + 5.8F);
        this.armRsHand.xRot = 0.06F * Mth.sin(t * 0.047F + 6.4F);

        // ---- 悬浮符文石：绕体公转 + 自旋 ----
        float o0 = t * 0.016F;
        this.rune0.x = 30.0F * Mth.cos(o0);
        this.rune0.y = -50.0F + 4.0F * Mth.sin(t * 0.031F);
        this.rune0.z = 6.0F + 30.0F * Mth.sin(o0);
        this.rune0.yRot = t * 0.5F;
        float o1 = t * 0.019F + 2.1F;
        this.rune1.x = 32.0F * Mth.cos(o1);
        this.rune1.y = -40.0F + 5.0F * Mth.sin(t * 0.027F + 1.2F);
        this.rune1.z = -5.0F + 32.0F * Mth.sin(o1);
        this.rune1.yRot = -t * 0.4F;
        float o2 = t * 0.013F + 4.2F;
        this.rune2.x = 24.0F * Mth.cos(o2);
        this.rune2.y = -32.0F + 3.0F * Mth.sin(t * 0.035F + 2.4F);
        this.rune2.z = 10.0F + 24.0F * Mth.sin(o2);
        this.rune2.yRot = t * 0.7F;
        float o3 = t * 0.017F + 5.6F;
        this.rune3.x = 26.0F * Mth.cos(o3);
        this.rune3.y = -68.0F + 3.0F * Mth.sin(t * 0.029F + 3.6F);
        this.rune3.z = 2.0F + 26.0F * Mth.sin(o3);
        this.rune3.yRot = -t * 0.6F;

        // ---- 背后虚空环：慢转 ----
        this.halo.yRot = t * 0.012F;
        this.halo.xRot = 0.08F * Mth.sin(t * 0.020F);
        this.haloRunes.yRot = -t * 0.020F;
        this.haloRunes.xRot = 0.08F * Mth.sin(t * 0.020F);

    }


    /** 模型最低点像素 y（袍摆触须收束处，浮空基准）。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

}
