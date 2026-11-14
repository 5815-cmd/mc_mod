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
 * 傀儡祖师 · PuppetMaster / PuppetMasterModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>提线傀儡：T 型提线横梁 + 被吊在柜前的球形机关首 + 镂空齿轮柜 + 四对节肢悬臂 + 八边形机关底盘</p>
 */
public class PuppetMasterModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart orbA;
    private final ModelPart orbB;
    private final ModelPart orbC;
    private final ModelPart baseRing;
    private final ModelPart cabinet;
    private final ModelPart footBl;
    private final ModelPart footBr;
    private final ModelPart footFl;
    private final ModelPart footFr;
    private final ModelPart core;
    private final ModelPart crackA;
    private final ModelPart crackB;
    private final ModelPart crackC;
    private final ModelPart doorL;
    private final ModelPart doorR;
    private final ModelPart gearDeep;
    private final ModelPart gearFront;
    private final ModelPart gearMid;
    private final ModelPart limb1UL;
    private final ModelPart limb1UR;
    private final ModelPart limb2UL;
    private final ModelPart limb2UR;
    private final ModelPart limb3UL;
    private final ModelPart limb3UR;
    private final ModelPart limb4UL;
    private final ModelPart limb4UR;
    private final ModelPart rigPost;
    private final ModelPart tarm1UL;
    private final ModelPart tarm1UR;
    private final ModelPart tarm2UL;
    private final ModelPart tarm2UR;
    private final ModelPart limb1DL;
    private final ModelPart limb1DR;
    private final ModelPart limb1GL;
    private final ModelPart limb1GR;
    private final ModelPart limb2DL;
    private final ModelPart limb2DR;
    private final ModelPart limb2GL;
    private final ModelPart limb2GR;
    private final ModelPart limb3DL;
    private final ModelPart limb3DR;
    private final ModelPart limb3GL;
    private final ModelPart limb3GR;
    private final ModelPart limb4DL;
    private final ModelPart limb4DR;
    private final ModelPart limb4GL;
    private final ModelPart limb4GR;
    private final ModelPart rigBar;
    private final ModelPart tarm1DL;
    private final ModelPart tarm1DR;
    private final ModelPart tarm2DL;
    private final ModelPart tarm2DR;
    private final ModelPart bannerL;
    private final ModelPart bannerR;
    private final ModelPart stringHL;
    private final ModelPart stringHR;
    private final ModelPart stringSL;
    private final ModelPart stringSR;
    private final ModelPart head;
    private final ModelPart eyeArray;
    private final ModelPart mask;
    private final ModelPart eyeL;
    private final ModelPart eyeR;

    public PuppetMasterModel(ModelPart root) {
        this.root = root;
        this.base = root.getChild("base");
        this.orbA = root.getChild("orbA");
        this.orbB = root.getChild("orbB");
        this.orbC = root.getChild("orbC");
        this.baseRing = root.getChild("base").getChild("base_ring");
        this.cabinet = root.getChild("base").getChild("cabinet");
        this.footBl = root.getChild("base").getChild("foot_bl");
        this.footBr = root.getChild("base").getChild("foot_br");
        this.footFl = root.getChild("base").getChild("foot_fl");
        this.footFr = root.getChild("base").getChild("foot_fr");
        this.core = root.getChild("base").getChild("cabinet").getChild("core");
        this.crackA = root.getChild("base").getChild("cabinet").getChild("crack_a");
        this.crackB = root.getChild("base").getChild("cabinet").getChild("crack_b");
        this.crackC = root.getChild("base").getChild("cabinet").getChild("crack_c");
        this.doorL = root.getChild("base").getChild("cabinet").getChild("door_l");
        this.doorR = root.getChild("base").getChild("cabinet").getChild("door_r");
        this.gearDeep = root.getChild("base").getChild("cabinet").getChild("gear_deep");
        this.gearFront = root.getChild("base").getChild("cabinet").getChild("gear_front");
        this.gearMid = root.getChild("base").getChild("cabinet").getChild("gear_mid");
        this.limb1UL = root.getChild("base").getChild("cabinet").getChild("limb1_u_l");
        this.limb1UR = root.getChild("base").getChild("cabinet").getChild("limb1_u_r");
        this.limb2UL = root.getChild("base").getChild("cabinet").getChild("limb2_u_l");
        this.limb2UR = root.getChild("base").getChild("cabinet").getChild("limb2_u_r");
        this.limb3UL = root.getChild("base").getChild("cabinet").getChild("limb3_u_l");
        this.limb3UR = root.getChild("base").getChild("cabinet").getChild("limb3_u_r");
        this.limb4UL = root.getChild("base").getChild("cabinet").getChild("limb4_u_l");
        this.limb4UR = root.getChild("base").getChild("cabinet").getChild("limb4_u_r");
        this.rigPost = root.getChild("base").getChild("cabinet").getChild("rig_post");
        this.tarm1UL = root.getChild("base").getChild("cabinet").getChild("tarm1_u_l");
        this.tarm1UR = root.getChild("base").getChild("cabinet").getChild("tarm1_u_r");
        this.tarm2UL = root.getChild("base").getChild("cabinet").getChild("tarm2_u_l");
        this.tarm2UR = root.getChild("base").getChild("cabinet").getChild("tarm2_u_r");
        this.limb1DL = root.getChild("base").getChild("cabinet").getChild("limb1_u_l").getChild("limb1_d_l");
        this.limb1DR = root.getChild("base").getChild("cabinet").getChild("limb1_u_r").getChild("limb1_d_r");
        this.limb1GL = root.getChild("base").getChild("cabinet").getChild("limb1_u_l").getChild("limb1_g_l");
        this.limb1GR = root.getChild("base").getChild("cabinet").getChild("limb1_u_r").getChild("limb1_g_r");
        this.limb2DL = root.getChild("base").getChild("cabinet").getChild("limb2_u_l").getChild("limb2_d_l");
        this.limb2DR = root.getChild("base").getChild("cabinet").getChild("limb2_u_r").getChild("limb2_d_r");
        this.limb2GL = root.getChild("base").getChild("cabinet").getChild("limb2_u_l").getChild("limb2_g_l");
        this.limb2GR = root.getChild("base").getChild("cabinet").getChild("limb2_u_r").getChild("limb2_g_r");
        this.limb3DL = root.getChild("base").getChild("cabinet").getChild("limb3_u_l").getChild("limb3_d_l");
        this.limb3DR = root.getChild("base").getChild("cabinet").getChild("limb3_u_r").getChild("limb3_d_r");
        this.limb3GL = root.getChild("base").getChild("cabinet").getChild("limb3_u_l").getChild("limb3_g_l");
        this.limb3GR = root.getChild("base").getChild("cabinet").getChild("limb3_u_r").getChild("limb3_g_r");
        this.limb4DL = root.getChild("base").getChild("cabinet").getChild("limb4_u_l").getChild("limb4_d_l");
        this.limb4DR = root.getChild("base").getChild("cabinet").getChild("limb4_u_r").getChild("limb4_d_r");
        this.limb4GL = root.getChild("base").getChild("cabinet").getChild("limb4_u_l").getChild("limb4_g_l");
        this.limb4GR = root.getChild("base").getChild("cabinet").getChild("limb4_u_r").getChild("limb4_g_r");
        this.rigBar = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar");
        this.tarm1DL = root.getChild("base").getChild("cabinet").getChild("tarm1_u_l").getChild("tarm1_d_l");
        this.tarm1DR = root.getChild("base").getChild("cabinet").getChild("tarm1_u_r").getChild("tarm1_d_r");
        this.tarm2DL = root.getChild("base").getChild("cabinet").getChild("tarm2_u_l").getChild("tarm2_d_l");
        this.tarm2DR = root.getChild("base").getChild("cabinet").getChild("tarm2_u_r").getChild("tarm2_d_r");
        this.bannerL = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("banner_l");
        this.bannerR = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("banner_r");
        this.stringHL = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_h_l");
        this.stringHR = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_h_r");
        this.stringSL = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_s_l");
        this.stringSR = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_s_r");
        this.head = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_h_l").getChild("head");
        this.eyeArray = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_h_l").getChild("head").getChild("eye_array");
        this.mask = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_h_l").getChild("head").getChild("mask");
        this.eyeL = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_h_l").getChild("head").getChild("mask").getChild("eye_l");
        this.eyeR = root.getChild("base").getChild("cabinet").getChild("rig_post").getChild("rig_bar").getChild("string_h_l").getChild("head").getChild("mask").getChild("eye_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition base = root.addOrReplaceChild("base",
CubeListBuilder.create()
        .texOffs(171, 0).addBox(-10.0F, -5.0F, -8.0F, 20.0F, 2.0F, 16.0F, NONE)
        .texOffs(98, 0).addBox(-8.0F, -5.0F, -10.0F, 16.0F, 2.0F, 20.0F, NONE)
        .texOffs(0, 25).addBox(-8.0F, -7.0F, -8.0F, 16.0F, 2.0F, 16.0F, NONE)
        .texOffs(134, 25).addBox(-6.0F, -9.0F, -6.0F, 12.0F, 2.0F, 12.0F, NONE)
        .texOffs(37, 58).addBox(-4.0F, -11.0F, -4.0F, 8.0F, 2.0F, 8.0F, NONE)
        .texOffs(0, 58).addBox(-4.5F, -12.0F, -4.5F, 9.0F, 1.0F, 9.0F, NONE)
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition orbA = root.addOrReplaceChild("orbA",
CubeListBuilder.create()
        .texOffs(91, 58).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, NONE)
        .texOffs(0, 69).addBox(-3.5F, -0.5F, -3.5F, 7.0F, 1.0F, 7.0F, NONE)
        .texOffs(153, 78).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
                , PartPose.offset(12.5F, 3.0F, -3.0F));
        PartDefinition orbB = root.addOrReplaceChild("orbB",
CubeListBuilder.create()
        .texOffs(80, 69).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
        .texOffs(87, 78).addBox(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F, NONE)
        .texOffs(26, 87).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(-12.0F, 8.0F, 2.0F));
        PartDefinition orbC = root.addOrReplaceChild("orbC",
CubeListBuilder.create()
        .texOffs(13, 87).addBox(-1.6F, -1.6F, -1.6F, 3.2F, 3.2F, 3.2F, NONE)
        .texOffs(158, 78).addBox(-2.6F, -0.5F, -2.6F, 5.2F, 1.0F, 5.2F, NONE)
                , PartPose.offset(0.0F, 12.0F, 10.0F));
        PartDefinition baseRing = base.addOrReplaceChild("base_ring",
CubeListBuilder.create()
        .texOffs(106, 87).addBox(-11.0F, -1.0F, -11.0F, 22.0F, 2.0F, 2.0F, NONE)
        .texOffs(155, 87).addBox(-11.0F, -1.0F, 9.0F, 22.0F, 2.0F, 2.0F, NONE)
        .texOffs(0, 0).addBox(9.0F, -1.0F, -11.0F, 2.0F, 2.0F, 22.0F, NONE)
        .texOffs(49, 0).addBox(-11.0F, -1.0F, -11.0F, 2.0F, 2.0F, 22.0F, NONE)
        .texOffs(31, 87).addBox(8.0F, -1.0F, 8.0F, 3.0F, 2.0F, 3.0F, NONE)
        .texOffs(44, 87).addBox(-11.0F, -1.0F, 8.0F, 3.0F, 2.0F, 3.0F, NONE)
        .texOffs(57, 87).addBox(8.0F, -1.0F, -11.0F, 3.0F, 2.0F, 3.0F, NONE)
        .texOffs(70, 87).addBox(-11.0F, -1.0F, -11.0F, 3.0F, 2.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition cabinet = base.addOrReplaceChild("cabinet",
CubeListBuilder.create()
        .texOffs(30, 44).addBox(-8.0F, -14.0F, -5.0F, 16.0F, 2.0F, 10.0F, NONE)
        .texOffs(83, 44).addBox(-8.0F, -2.0F, -5.0F, 16.0F, 2.0F, 10.0F, NONE)
        .texOffs(98, 25).addBox(6.0F, -14.0F, -5.0F, 2.0F, 14.0F, 2.0F, NONE)
        .texOffs(107, 25).addBox(-8.0F, -14.0F, -5.0F, 2.0F, 14.0F, 2.0F, NONE)
        .texOffs(116, 25).addBox(6.0F, -14.0F, 3.0F, 2.0F, 14.0F, 2.0F, NONE)
        .texOffs(125, 25).addBox(-8.0F, -14.0F, 3.0F, 2.0F, 14.0F, 2.0F, NONE)
        .texOffs(136, 44).addBox(-6.0F, -12.0F, 3.0F, 12.0F, 10.0F, 2.0F, NONE)
        .texOffs(33, 94).addBox(-6.0F, -13.0F, -6.0F, 12.0F, 2.0F, 2.0F, NONE)
        .texOffs(62, 94).addBox(-6.0F, -3.0F, -6.0F, 12.0F, 2.0F, 2.0F, NONE)
        .texOffs(217, 99).addBox(-6.0F, -9.0F, 5.0F, 12.0F, 1.0F, 1.0F, NONE)
        .texOffs(0, 104).addBox(-6.0F, -4.0F, 5.0F, 12.0F, 1.0F, 1.0F, NONE)
        .texOffs(220, 44).addBox(-1.0F, -12.0F, 5.0F, 2.0F, 10.0F, 1.0F, NONE)
        .texOffs(27, 99).addBox(-7.0F, -3.0F, -6.6F, 14.0F, 2.0F, 1.0F, NONE)
        .texOffs(202, 99).addBox(-5.0F, -1.0F, -6.6F, 1.0F, 2.0F, 1.0F, NONE)
        .texOffs(207, 99).addBox(4.0F, -1.0F, -6.6F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -11.0F, 0.0F));
        PartDefinition footBl = base.addOrReplaceChild("foot_bl",
CubeListBuilder.create()
        .texOffs(205, 78).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(9.0F, -3.0F, 9.0F));
        PartDefinition footBr = base.addOrReplaceChild("foot_br",
CubeListBuilder.create()
        .texOffs(218, 78).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(-9.0F, -3.0F, 9.0F));
        PartDefinition footFl = base.addOrReplaceChild("foot_fl",
CubeListBuilder.create()
        .texOffs(179, 78).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(9.0F, -3.0F, -9.0F));
        PartDefinition footFr = base.addOrReplaceChild("foot_fr",
CubeListBuilder.create()
        .texOffs(192, 78).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
                , PartPose.offset(-9.0F, -3.0F, -9.0F));
        PartDefinition core = cabinet.addOrReplaceChild("core",
CubeListBuilder.create()
        .texOffs(46, 69).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, NONE)
        .texOffs(91, 94).addBox(-3.5F, -0.5F, -1.5F, 7.0F, 1.0F, 3.0F, NONE)
        .texOffs(216, 58).addBox(-0.5F, -3.5F, -1.5F, 1.0F, 7.0F, 3.0F, NONE)
        .texOffs(13, 99).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 2.0F, NONE)
        .texOffs(27, 104).addBox(-3.0F, -0.5F, -3.5F, 6.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -6.5F, -4.0F));
        PartDefinition crackA = cabinet.addOrReplaceChild("crack_a",
CubeListBuilder.create()
        .texOffs(22, 99).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F, NONE)
        .texOffs(168, 104).addBox(0.0F, 2.0F, -1.0F, 3.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(-4.0F, -13.0F, -6.0F));
        PartDefinition crackB = cabinet.addOrReplaceChild("crack_b",
CubeListBuilder.create()
        .texOffs(212, 99).addBox(0.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, NONE)
        .texOffs(177, 104).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(2.0F, -3.0F, -6.0F));
        PartDefinition crackC = cabinet.addOrReplaceChild("crack_c",
CubeListBuilder.create()
        .texOffs(101, 87).addBox(0.0F, 0.0F, -1.0F, 1.0F, 4.0F, 1.0F, NONE)
        .texOffs(184, 104).addBox(0.0F, 3.0F, -1.0F, 2.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(-7.0F, -8.0F, -6.0F));
        PartDefinition doorL = cabinet.addOrReplaceChild("door_l",
CubeListBuilder.create()
        .texOffs(0, 44).addBox(-6.0F, -6.0F, -2.0F, 6.0F, 12.0F, 1.0F, NONE)
        .texOffs(83, 87).addBox(-4.0F, -3.0F, -3.0F, 3.0F, 4.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(8.0F, -7.0F, -5.0F, 0.0F, -0.2F, 0.0F));
        PartDefinition doorR = cabinet.addOrReplaceChild("door_r",
CubeListBuilder.create()
        .texOffs(15, 44).addBox(0.0F, -6.0F, -2.0F, 6.0F, 12.0F, 1.0F, NONE)
        .texOffs(92, 87).addBox(1.0F, -3.0F, -3.0F, 3.0F, 4.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-8.0F, -7.0F, -5.0F, 0.0F, 0.2F, 0.0F));
        PartDefinition gearDeep = cabinet.addOrReplaceChild("gear_deep",
CubeListBuilder.create()
        .texOffs(70, 58).addBox(-4.5F, -4.5F, -0.5F, 9.0F, 9.0F, 1.0F, NONE)
        .texOffs(76, 99).addBox(4.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(83, 99).addBox(-6.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(90, 99).addBox(-1.0F, 4.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(97, 99).addBox(-1.0F, -6.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(1.0F, -8.0F, 1.0F));
        PartDefinition gearFront = cabinet.addOrReplaceChild("gear_front",
CubeListBuilder.create()
        .texOffs(231, 78).addBox(-2.5F, -2.5F, -0.5F, 5.0F, 5.0F, 1.0F, NONE)
        .texOffs(132, 99).addBox(2.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(139, 99).addBox(-4.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(146, 99).addBox(-1.0F, 2.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(153, 99).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, -8.5F, -2.5F));
        PartDefinition gearMid = cabinet.addOrReplaceChild("gear_mid",
CubeListBuilder.create()
        .texOffs(29, 69).addBox(-3.5F, -3.5F, -0.5F, 7.0F, 7.0F, 1.0F, NONE)
        .texOffs(104, 99).addBox(3.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(111, 99).addBox(-5.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(118, 99).addBox(-1.0F, 3.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(125, 99).addBox(-1.0F, -5.0F, -0.5F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(-2.0F, -7.0F, -1.0F));
        PartDefinition limb1UL = cabinet.addOrReplaceChild("limb1_u_l",
CubeListBuilder.create()
        .texOffs(97, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(112, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(8.0F, -12.0F, 0.0F, 0.1F, 0.2F, -1.1F));
        PartDefinition limb1UR = cabinet.addOrReplaceChild("limb1_u_r",
CubeListBuilder.create()
        .texOffs(115, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(125, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(-8.0F, -12.0F, 0.0F, 0.1F, -0.2F, 1.1F));
        PartDefinition limb2UL = cabinet.addOrReplaceChild("limb2_u_l",
CubeListBuilder.create()
        .texOffs(133, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(138, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(7.0F, -9.0F, 5.0F, 0.3F, -1.2F, -0.8F));
        PartDefinition limb2UR = cabinet.addOrReplaceChild("limb2_u_r",
CubeListBuilder.create()
        .texOffs(151, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(151, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(-7.0F, -9.0F, 5.0F, 0.3F, 1.2F, 0.8F));
        PartDefinition limb3UL = cabinet.addOrReplaceChild("limb3_u_l",
CubeListBuilder.create()
        .texOffs(169, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(164, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(7.5F, -5.5F, -1.0F, -0.1F, 1.2F, -0.8F));
        PartDefinition limb3UR = cabinet.addOrReplaceChild("limb3_u_r",
CubeListBuilder.create()
        .texOffs(187, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(177, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(-7.5F, -5.5F, -1.0F, -0.1F, -1.2F, 0.8F));
        PartDefinition limb4UL = cabinet.addOrReplaceChild("limb4_u_l",
CubeListBuilder.create()
        .texOffs(205, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(190, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(6.0F, -2.5F, 0.0F, 0.1F, 0.0F, -1.2F));
        PartDefinition limb4UR = cabinet.addOrReplaceChild("limb4_u_r",
CubeListBuilder.create()
        .texOffs(223, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(203, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(-6.0F, -2.5F, 0.0F, 0.1F, -0.0F, 1.2F));
        PartDefinition rigPost = cabinet.addOrReplaceChild("rig_post",
CubeListBuilder.create()
        .texOffs(183, 25).addBox(-1.2F, -12.0F, -1.2F, 2.4F, 12.0F, 2.4F, NONE)
        .texOffs(112, 78).addBox(-2.5F, -1.5F, -2.5F, 5.0F, 1.5F, 5.0F, NONE)
                , PartPose.offset(0.0F, -14.0F, 0.0F));
        PartDefinition tarm1UL = cabinet.addOrReplaceChild("tarm1_u_l",
CubeListBuilder.create()
        .texOffs(241, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(216, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(7.0F, -9.0F, 4.0F, 0.0F, 0.0F, 2.9F));
        PartDefinition tarm1UR = cabinet.addOrReplaceChild("tarm1_u_r",
CubeListBuilder.create()
        .texOffs(0, 78).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(229, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(-7.0F, -9.0F, 4.0F, 0.0F, 0.0F, -2.9F));
        PartDefinition tarm2UL = cabinet.addOrReplaceChild("tarm2_u_l",
CubeListBuilder.create()
        .texOffs(9, 78).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(242, 94).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(7.0F, -9.0F, 4.0F, 0.0F, 0.0F, 2.9F));
        PartDefinition tarm2UR = cabinet.addOrReplaceChild("tarm2_u_r",
CubeListBuilder.create()
        .texOffs(18, 78).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(0, 99).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, NONE)
                , PartPose.offsetAndRotation(-7.0F, -9.0F, 4.0F, 0.0F, 0.0F, -2.9F));
        PartDefinition limb1DL = limb1UL.addOrReplaceChild("limb1_d_l",
CubeListBuilder.create()
        .texOffs(106, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(60, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(191, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, 1.0F));
        PartDefinition limb1DR = limb1UR.addOrReplaceChild("limb1_d_r",
CubeListBuilder.create()
        .texOffs(124, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(69, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(196, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, -1.0F));
        PartDefinition limb1GL = limb1UL.addOrReplaceChild("limb1_g_l",
CubeListBuilder.create()
        .texOffs(112, 58).addBox(-0.5F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition limb1GR = limb1UR.addOrReplaceChild("limb1_g_r",
CubeListBuilder.create()
        .texOffs(125, 58).addBox(-0.5F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition limb2DL = limb2UL.addOrReplaceChild("limb2_d_l",
CubeListBuilder.create()
        .texOffs(142, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(78, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(201, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, 1.0F));
        PartDefinition limb2DR = limb2UR.addOrReplaceChild("limb2_d_r",
CubeListBuilder.create()
        .texOffs(160, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(87, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(206, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, -1.0F));
        PartDefinition limb2GL = limb2UL.addOrReplaceChild("limb2_g_l",
CubeListBuilder.create()
        .texOffs(138, 58).addBox(-0.5F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition limb2GR = limb2UR.addOrReplaceChild("limb2_g_r",
CubeListBuilder.create()
        .texOffs(151, 58).addBox(-0.5F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition limb3DL = limb3UL.addOrReplaceChild("limb3_d_l",
CubeListBuilder.create()
        .texOffs(178, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(96, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(211, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, 1.0F));
        PartDefinition limb3DR = limb3UR.addOrReplaceChild("limb3_d_r",
CubeListBuilder.create()
        .texOffs(196, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(105, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(216, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, -1.0F));
        PartDefinition limb3GL = limb3UL.addOrReplaceChild("limb3_g_l",
CubeListBuilder.create()
        .texOffs(164, 58).addBox(-0.5F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition limb3GR = limb3UR.addOrReplaceChild("limb3_g_r",
CubeListBuilder.create()
        .texOffs(177, 58).addBox(-0.5F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition limb4DL = limb4UL.addOrReplaceChild("limb4_d_l",
CubeListBuilder.create()
        .texOffs(214, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(114, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(221, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, 1.0F));
        PartDefinition limb4DR = limb4UR.addOrReplaceChild("limb4_d_r",
CubeListBuilder.create()
        .texOffs(232, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(123, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(226, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, -1.0F));
        PartDefinition limb4GL = limb4UL.addOrReplaceChild("limb4_g_l",
CubeListBuilder.create()
        .texOffs(190, 58).addBox(-0.5F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition limb4GR = limb4UR.addOrReplaceChild("limb4_g_r",
CubeListBuilder.create()
        .texOffs(203, 58).addBox(-0.5F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, NONE)
                , PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition rigBar = rigPost.addOrReplaceChild("rig_bar",
CubeListBuilder.create()
        .texOffs(204, 87).addBox(-11.0F, -2.0F, -1.0F, 22.0F, 2.0F, 2.0F, NONE)
        .texOffs(244, 78).addBox(10.5F, -2.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
        .texOffs(0, 87).addBox(-13.5F, -2.0F, -1.5F, 3.0F, 3.0F, 3.0F, NONE)
        .texOffs(192, 25).addBox(2.5F, -2.0F, -12.0F, 2.0F, 2.0F, 11.0F, NONE)
        .texOffs(219, 25).addBox(-4.5F, -2.0F, -12.0F, 2.0F, 2.0F, 11.0F, NONE)
        .texOffs(0, 94).addBox(-7.0F, -2.0F, -11.5F, 14.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition tarm1DL = tarm1UL.addOrReplaceChild("tarm1_d_l",
CubeListBuilder.create()
        .texOffs(133, 78).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
        .texOffs(132, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(231, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, -2.8F));
        PartDefinition tarm1DR = tarm1UR.addOrReplaceChild("tarm1_d_r",
CubeListBuilder.create()
        .texOffs(138, 78).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
        .texOffs(141, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(236, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 2.8F));
        PartDefinition tarm2DL = tarm2UL.addOrReplaceChild("tarm2_d_l",
CubeListBuilder.create()
        .texOffs(143, 78).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
        .texOffs(150, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(241, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, -2.8F));
        PartDefinition tarm2DR = tarm2UR.addOrReplaceChild("tarm2_d_r",
CubeListBuilder.create()
        .texOffs(148, 78).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, NONE)
        .texOffs(159, 104).addBox(-1.5F, 6.0F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
        .texOffs(246, 104).addBox(-0.5F, 7.0F, -0.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 2.8F));
        PartDefinition bannerL = rigBar.addOrReplaceChild("banner_l",
CubeListBuilder.create()
        .texOffs(225, 58).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 9.0F, 1.0F, NONE)
        .texOffs(42, 104).addBox(-1.5F, 8.5F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(11.5F, 0.0F, 0.0F));
        PartDefinition bannerR = rigBar.addOrReplaceChild("banner_r",
CubeListBuilder.create()
        .texOffs(232, 58).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 9.0F, 1.0F, NONE)
        .texOffs(51, 104).addBox(-1.5F, 8.5F, -0.5F, 3.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(-11.5F, 0.0F, 0.0F));
        PartDefinition stringHL = rigBar.addOrReplaceChild("string_h_l",
CubeListBuilder.create()
        .texOffs(27, 78).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offset(3.5F, 0.0F, -10.5F));
        PartDefinition stringHR = rigBar.addOrReplaceChild("string_h_r",
CubeListBuilder.create()
        .texOffs(32, 78).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F, NONE)
                , PartPose.offset(-3.5F, 0.0F, -10.5F));
        PartDefinition stringSL = rigBar.addOrReplaceChild("string_s_l",
CubeListBuilder.create()
        .texOffs(239, 58).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(6.5F, 0.0F, -10.5F, 0.0F, 0.0F, 0.2F));
        PartDefinition stringSR = rigBar.addOrReplaceChild("string_s_r",
CubeListBuilder.create()
        .texOffs(244, 58).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F, NONE)
                , PartPose.offsetAndRotation(-6.5F, 0.0F, -10.5F, 0.0F, 0.0F, -0.2F));
        PartDefinition head = stringHL.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(65, 25).addBox(-7.5F, 0.0F, -3.0F, 8.0F, 8.0F, 8.0F, NONE)
        .texOffs(37, 78).addBox(-6.5F, -1.0F, -2.0F, 6.0F, 1.0F, 6.0F, NONE)
        .texOffs(62, 78).addBox(-6.5F, 8.0F, -2.0F, 6.0F, 1.0F, 6.0F, NONE)
        .texOffs(165, 44).addBox(0.5F, 1.0F, -2.0F, 1.0F, 6.0F, 6.0F, NONE)
        .texOffs(180, 44).addBox(-8.5F, 1.0F, -2.0F, 1.0F, 6.0F, 6.0F, NONE)
                , PartPose.offset(0.0F, 7.0F, -1.0F));
        PartDefinition eyeArray = head.addOrReplaceChild("eye_array",
CubeListBuilder.create()
        .texOffs(167, 99).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(174, 99).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(181, 99).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(188, 99).addBox(2.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, NONE)
        .texOffs(195, 99).addBox(-4.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(-3.5F, 5.5F, -3.0F));
        PartDefinition mask = head.addOrReplaceChild("mask",
CubeListBuilder.create()
        .texOffs(195, 44).addBox(-5.5F, 0.0F, -1.0F, 11.0F, 10.0F, 1.0F, NONE)
        .texOffs(63, 69).addBox(-3.5F, 1.5F, -2.0F, 7.0F, 7.0F, 1.0F, NONE)
        .texOffs(160, 99).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 1.5F, 1.0F, NONE)
                , PartPose.offset(-3.5F, -0.5F, -3.5F));
        PartDefinition eyeL = mask.addOrReplaceChild("eye_l",
CubeListBuilder.create()
        .texOffs(58, 99).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(2.5F, 3.5F, -2.0F));
        PartDefinition eyeR = mask.addOrReplaceChild("eye_r",
CubeListBuilder.create()
        .texOffs(67, 99).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(-2.5F, 3.5F, -2.0F));
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
        int phase = com.hongmeng.xiuxian.entity.BossPhase.phaseOf(entity);
        if (phase < 1) {
            phase = 1;
        }
        float t = ageInTicks;
        float walk = limbSwingAmount;
        float hurt = entity.hurtTime > 0 ? (float) entity.hurtTime / 10.0F : 0.0F;
        boolean rampage = phase >= 2;      // 二阶段：齿轮暴走
        boolean broken = phase >= 3;       // 三阶段：裂开 + 面具脱落
        float gearMul = broken ? 6.0F : (rampage ? 3.2F : 1.0F);
        float rigAmp = broken ? 0.12F : (rampage ? 0.07F : 0.035F);
        float fold = rampage ? 1.0F : 0.0F;
        float spread = 1.0F + 0.12F * (float) (phase - 1);
        float amp = 0.35F + 0.12F * (float) (phase - 1);
        float stepA = Mth.cos(limbSwing * 0.5F) * walk;
        float stepB = Mth.cos(limbSwing * 0.5F + 1.7F) * walk;
        float stepC = Mth.cos(limbSwing * 0.5F + 3.4F) * walk;
        float stepD = Mth.cos(limbSwing * 0.5F + 4.9F) * walk;

        // ---- 底盘：整具机关被吊着，缓慢摇晃；受击后仰 ----
        this.base.xRot = -0.05F * hurt + 0.012F * Mth.sin(t * 0.031F);
        this.base.zRot = 0.014F * Mth.sin(t * 0.026F + 0.7F)
                + 0.02F * hurt * Mth.sin(t * 0.8F);
        this.baseRing.yRot = t * 0.02F * gearMul;

        // ---- 柜体：随转向微摆；齿轮暴走后开始抖 ----
        this.cabinet.yRot = yaw * 0.10F + Mth.sin(t * 0.021F) * (0.010F + 0.012F * (float) (phase - 1));
        this.cabinet.zRot = broken ? 0.016F * Mth.sin(t * 0.42F) : 0.0F;

        // ---- 柜门：1 阶段半掩；2 阶段全开（核心外露）；3 阶段全开抖动 ----
        float doorA = rampage ? 1.35F : 0.2F;
        float doorJ = broken ? 0.05F * Mth.sin(t * 0.55F) : 0.0F;
        this.doorL.yRot = -doorA - doorJ;
        this.doorR.yRot = doorA + doorJ;

        // ---- 内部 3 层齿轮：逐层反向、随阶段加速（暴走的最直观信号）----
        this.gearDeep.zRot = t * 0.045F * gearMul;
        this.gearMid.zRot = -t * 0.070F * gearMul;
        this.gearFront.zRot = t * 0.105F * gearMul;

        // ---- 发条核心：自转 + 呼吸；2 阶段外露提速，3 阶段爆亮放大 ----
        float coreScale = broken ? 1.55F : (rampage ? 1.15F : 1.0F);
        this.core.zRot = t * 0.04F * gearMul;
        this.core.xScale = coreScale + 0.05F * Mth.sin(t * 0.30F);
        this.core.zScale = coreScale + 0.05F * Mth.sin(t * 0.30F + 0.6F);
        this.core.y = -6.5F + 0.25F * Mth.sin(t * 0.10F);

        // ---- 提线架：随视线滞后倾斜（吊着的木架）----
        this.rigPost.xRot = 0.010F * Mth.sin(t * 0.028F);
        this.rigPost.zRot = 0.010F * Mth.sin(t * 0.033F + 0.9F);
        this.rigBar.xRot = -pitch * 0.10F + 0.012F * Mth.sin(t * 0.030F);
        this.rigBar.yRot = -yaw * 0.12F;
        this.rigBar.zRot = rigAmp * 0.35F * Mth.sin(t * 0.025F + 0.3F);

        // ---- 4 条垂线 + 吊着的机关首（摆幅随阶段变大）----
        float swayX = rigAmp * Mth.sin(t * 0.038F + 0.2F)
                + 0.04F * hurt * Mth.sin(t * 0.7F);
        float swayZ = rigAmp * 0.8F * Mth.sin(t * 0.031F + 1.4F);
        this.stringHL.xRot = swayX;
        this.stringHL.zRot = swayZ;
        this.stringHR.xRot = swayX;
        this.stringHR.zRot = swayZ;
        this.stringSL.xRot = swayX;
        this.stringSL.zRot = 0.2F + swayZ;
        this.stringSR.xRot = swayX;
        this.stringSR.zRot = -0.2F + swayZ;
        this.head.yRot = yaw * 0.85F + 0.05F * Mth.sin(t * 0.043F + 0.4F);
        this.head.xRot = -pitch * 0.55F + 0.05F * Mth.sin(t * 0.052F);
        this.head.zRot = 0.03F * Mth.sin(t * 0.037F) + 0.12F * hurt * Mth.sin(t * 0.6F);
        this.head.y = 7.0F + 0.10F * Mth.sin(t * 0.06F);

        // ---- 面具：1/2 阶段贴在脸上；3 阶段脱落，露出眼阵 ----
        if (broken) {
            this.mask.xRot = -1.25F + 0.10F * Mth.sin(t * 0.35F);
            this.mask.zRot = 0.32F + 0.08F * Mth.sin(t * 0.29F);
            this.mask.y = 2.1F;
            this.mask.z = -4.7F;
        } else {
            this.mask.xRot = 0.0F;
            this.mask.zRot = 0.0F;
            this.mask.y = -0.5F;
            this.mask.z = -3.5F;
        }

        // ---- 3 阶段：柜体裂纹显形 ----
        this.crackA.visible = broken;
        this.crackB.visible = broken;
        this.crackC.visible = broken;

        // ---- 紫绸提线幔 ----
        this.bannerL.xRot = 0.02F + rigAmp * 0.9F * Mth.sin(t * 0.045F + 0.5F);
        this.bannerL.zRot = 0.09F * Mth.sin(t * 0.037F + 0.2F);
        this.bannerR.xRot = 0.02F + rigAmp * 0.9F * Mth.sin(t * 0.045F + 1.9F);
        this.bannerR.zRot = -0.09F * Mth.sin(t * 0.037F + 1.1F);

        // ---- 四对节肢悬臂：错相摆动；阶段越后张得越开（暴走时八腿全张）----
        this.limb1UL.zRot = -1.1F * spread;
        this.limb1UR.zRot = 1.1F * spread;
        this.limb1UL.xRot = 0.1F + amp * stepA;
        this.limb1UR.xRot = 0.1F - amp * stepA;
        this.limb1DL.xRot = 0.2F + 0.16F * Mth.sin(t * 0.05F) + amp * 0.4F * stepA;
        this.limb1DR.xRot = 0.2F + 0.16F * Mth.sin(t * 0.05F + 1.3F) + amp * 0.4F * stepA;
        this.limb1GL.xRot = t * 0.12F * gearMul;
        this.limb1GR.xRot = t * 0.12F * gearMul;

        this.limb2UL.zRot = -0.8F * spread;
        this.limb2UR.zRot = 0.8F * spread;
        this.limb2UL.xRot = 0.3F + amp * stepB;
        this.limb2UR.xRot = 0.3F - amp * stepB;
        this.limb2DL.xRot = 0.2F + 0.16F * Mth.sin(t * 0.05F + 2.1F) + amp * 0.4F * stepB;
        this.limb2DR.xRot = 0.2F + 0.16F * Mth.sin(t * 0.05F + 3.4F) + amp * 0.4F * stepB;
        this.limb2GL.xRot = -t * 0.10F * gearMul;
        this.limb2GR.xRot = -t * 0.10F * gearMul;

        this.limb3UL.zRot = -0.8F * spread;
        this.limb3UR.zRot = 0.8F * spread;
        this.limb3UL.xRot = -0.1F + amp * stepC;
        this.limb3UR.xRot = -0.1F - amp * stepC;
        this.limb3DL.xRot = 0.2F + 0.16F * Mth.sin(t * 0.05F + 4.0F) + amp * 0.4F * stepC;
        this.limb3DR.xRot = 0.2F + 0.16F * Mth.sin(t * 0.05F + 5.2F) + amp * 0.4F * stepC;
        this.limb3GL.xRot = t * 0.14F * gearMul;
        this.limb3GR.xRot = t * 0.14F * gearMul;

        this.limb4UL.zRot = -1.2F * spread;
        this.limb4UR.zRot = 1.2F * spread;
        this.limb4UL.xRot = 0.1F + amp * stepD;
        this.limb4UR.xRot = 0.1F - amp * stepD;
        this.limb4DL.xRot = 0.2F + 0.16F * Mth.sin(t * 0.05F + 0.8F) + amp * 0.4F * stepD;
        this.limb4DR.xRot = 0.2F + 0.16F * Mth.sin(t * 0.05F + 2.7F) + amp * 0.4F * stepD;
        this.limb4GL.xRot = -t * 0.12F * gearMul;
        this.limb4GR.xRot = -t * 0.12F * gearMul;

        // ---- 追加的 2 条提线臂：1 阶段折叠在柜背，2/3 阶段展开 ----
        this.tarm1UL.zRot = 2.9F - 3.5F * fold + 0.04F * Mth.sin(t * 0.05F);
        this.tarm1UR.zRot = -2.9F + 3.5F * fold - 0.04F * Mth.sin(t * 0.05F);
        this.tarm1DL.zRot = -2.8F + 2.6F * fold;
        this.tarm1DR.zRot = 2.8F - 2.6F * fold;
        this.tarm2UL.zRot = 2.9F - 3.45F * fold + 0.04F * Mth.sin(t * 0.05F + 1.2F);
        this.tarm2UR.zRot = -2.9F + 3.45F * fold - 0.04F * Mth.sin(t * 0.05F + 1.2F);
        this.tarm2DL.zRot = -2.8F + 2.6F * fold;
        this.tarm2DR.zRot = 2.8F - 2.6F * fold;

        // ---- 三颗青蓝傀儡核心：绕柜体公转 + 自转 ----
        float o1 = t * 0.030F;
        float o2 = t * 0.026F + 2.1F;
        float o3 = -t * 0.038F + 4.2F;
        this.orbA.x = 12.5F * Mth.cos(o1);
        this.orbA.z = 12.5F * Mth.sin(o1);
        this.orbA.y = 3.0F + 1.0F * Mth.sin(t * 0.055F);
        this.orbA.yRot = -t * 0.9F;
        this.orbB.x = 12.0F * Mth.cos(o2);
        this.orbB.z = 12.0F * Mth.sin(o2);
        this.orbB.y = 8.0F + 1.0F * Mth.sin(t * 0.061F + 1.3F);
        this.orbB.yRot = t * 1.1F;
        this.orbC.x = 11.0F * Mth.cos(o3);
        this.orbC.z = 11.0F * Mth.sin(o3);
        this.orbC.y = 12.0F + 0.8F * Mth.sin(t * 0.048F + 2.6F);
        this.orbC.yRot = t * 0.7F;

    }


    /** 模型最低点像素 y（底盘支撑脚底面）。 */
    public static final int LOWEST_PIXEL_Y = 24;

}
