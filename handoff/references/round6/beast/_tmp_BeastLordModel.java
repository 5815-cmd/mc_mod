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
 * 万兽之主 · BeastLord / BeastLordModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>骨甲巨兽：低伏长躯干（前高后低）+ 驼峰 3 排骨板/外翻尖刺 + 巨角兽首（8 獠牙/可开合下颚） + 肩甲前肢 + 外露肋骨 + 胸口发光核心 + 上翘长尾 4 节/尾锤</p>
 */
public class BeastLordModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart core;
    private final ModelPart humpFront;
    private final ModelPart humpMid;
    private final ModelPart humpRear;
    private final ModelPart legBl;
    private final ModelPart legBr;
    private final ModelPart legFl;
    private final ModelPart legFr;
    private final ModelPart neck;
    private final ModelPart ribL;
    private final ModelPart ribR;
    private final ModelPart shoulderL;
    private final ModelPart shoulderR;
    private final ModelPart tail1;
    private final ModelPart head;
    private final ModelPart tail2;
    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart jaw;
    private final ModelPart tail3;
    private final ModelPart tail4;
    private final ModelPart tailBlade;

    public BeastLordModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.core = root.getChild("body").getChild("core");
        this.humpFront = root.getChild("body").getChild("hump_front");
        this.humpMid = root.getChild("body").getChild("hump_mid");
        this.humpRear = root.getChild("body").getChild("hump_rear");
        this.legBl = root.getChild("body").getChild("leg_bl");
        this.legBr = root.getChild("body").getChild("leg_br");
        this.legFl = root.getChild("body").getChild("leg_fl");
        this.legFr = root.getChild("body").getChild("leg_fr");
        this.neck = root.getChild("body").getChild("neck");
        this.ribL = root.getChild("body").getChild("rib_l");
        this.ribR = root.getChild("body").getChild("rib_r");
        this.shoulderL = root.getChild("body").getChild("shoulder_l");
        this.shoulderR = root.getChild("body").getChild("shoulder_r");
        this.tail1 = root.getChild("body").getChild("tail_1");
        this.head = root.getChild("body").getChild("neck").getChild("head");
        this.tail2 = root.getChild("body").getChild("tail_1").getChild("tail_2");
        this.hornL = root.getChild("body").getChild("neck").getChild("head").getChild("horn_l");
        this.hornR = root.getChild("body").getChild("neck").getChild("head").getChild("horn_r");
        this.jaw = root.getChild("body").getChild("neck").getChild("head").getChild("jaw");
        this.tail3 = root.getChild("body").getChild("tail_1").getChild("tail_2").getChild("tail_3");
        this.tail4 = root.getChild("body").getChild("tail_1").getChild("tail_2").getChild("tail_3").getChild("tail_4");
        this.tailBlade = root.getChild("body").getChild("tail_1").getChild("tail_2").getChild("tail_3").getChild("tail_4").getChild("tail_blade");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
CubeListBuilder.create()
        .texOffs(93, 37).addBox(-16.0F, -16.0F, -32.0F, 32.0F, 20.0F, 12.0F, NONE)
        .texOffs(97, 0).addBox(-12.0F, -12.0F, -20.0F, 24.0F, 18.0F, 18.0F, NONE)
        .texOffs(0, 37).addBox(-14.0F, -8.0F, -2.0F, 28.0F, 14.0F, 18.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition core = body.addOrReplaceChild("core",
CubeListBuilder.create()
        .texOffs(225, 187).addBox(-6.0F, -2.0F, -2.0F, 12.0F, 11.0F, 3.0F, NONE)
        .texOffs(218, 230).addBox(-8.0F, -5.0F, -3.0F, 16.0F, 3.0F, 3.0F, NONE)
        .texOffs(125, 231).addBox(-8.0F, 8.0F, -3.0F, 16.0F, 2.0F, 3.0F, NONE)
        .texOffs(237, 25).addBox(-9.0F, -2.0F, -3.0F, 3.0F, 11.0F, 3.0F, NONE)
        .texOffs(237, 40).addBox(6.0F, -2.0F, -3.0F, 3.0F, 11.0F, 3.0F, NONE)
                , PartPose.offset(0.0F, -6.0F, -33.0F));
        PartDefinition humpFront = body.addOrReplaceChild("hump_front",
CubeListBuilder.create()
        .texOffs(182, 0).addBox(-6.0F, -20.0F, -8.0F, 12.0F, 22.0F, 9.0F, NONE)
        .texOffs(199, 94).addBox(7.0F, -16.0F, -8.0F, 8.0F, 18.0F, 9.0F, NONE)
        .texOffs(74, 99).addBox(-15.0F, -16.0F, -8.0F, 8.0F, 18.0F, 9.0F, NONE)
        .texOffs(47, 101).addBox(17.0F, -14.0F, -12.0F, 5.0F, 7.0F, 8.0F, NONE)
        .texOffs(0, 187).addBox(-22.0F, -14.0F, -12.0F, 5.0F, 7.0F, 8.0F, NONE)
        .texOffs(199, 148).addBox(-4.0F, -24.0F, -16.0F, 8.0F, 7.0F, 12.0F, NONE)
        .texOffs(234, 94).addBox(-3.0F, -29.0F, -6.0F, 5.0F, 10.0F, 5.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -14.0F, -24.0F, -0.1F, 0.0F, 0.0F));
        PartDefinition humpMid = body.addOrReplaceChild("hump_mid",
CubeListBuilder.create()
        .texOffs(0, 70).addBox(-13.0F, -20.0F, -6.0F, 10.0F, 22.0F, 8.0F, NONE)
        .texOffs(37, 70).addBox(2.0F, -20.0F, -6.0F, 10.0F, 22.0F, 8.0F, NONE)
        .texOffs(237, 55).addBox(-9.0F, -26.0F, -4.0F, 4.0F, 7.0F, 4.0F, NONE)
        .texOffs(237, 79).addBox(5.0F, -25.0F, -4.0F, 4.0F, 6.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -12.0F, -10.0F, -0.1F, 0.0F, 0.0F));
        PartDefinition humpRear = body.addOrReplaceChild("hump_rear",
CubeListBuilder.create()
        .texOffs(164, 94).addBox(-6.0F, -18.0F, -4.0F, 9.0F, 20.0F, 8.0F, NONE)
        .texOffs(225, 0).addBox(7.0F, -14.0F, -4.0F, 7.0F, 16.0F, 8.0F, NONE)
        .texOffs(162, 123).addBox(-14.0F, -14.0F, -4.0F, 7.0F, 16.0F, 8.0F, NONE)
        .texOffs(164, 82).addBox(-2.0F, -24.0F, -2.0F, 4.0F, 6.0F, 4.0F, NONE)
                , PartPose.offsetAndRotation(0.0F, -8.0F, 2.0F, 0.1F, 0.0F, 0.0F));
        PartDefinition legBl = body.addOrReplaceChild("leg_bl",
CubeListBuilder.create()
        .texOffs(182, 32).addBox(-7.0F, 0.0F, 0.0F, 15.0F, 18.0F, 12.0F, NONE)
        .texOffs(0, 149).addBox(-6.0F, 18.0F, 2.0F, 11.0F, 10.0F, 8.0F, NONE)
        .texOffs(173, 203).addBox(-7.0F, 28.0F, 0.0F, 15.0F, 4.0F, 9.0F, NONE)
        .texOffs(234, 110).addBox(-6.0F, 28.0F, -6.0F, 4.0F, 4.0F, 6.0F, NONE)
        .texOffs(236, 168).addBox(-1.0F, 28.0F, -6.0F, 4.0F, 4.0F, 6.0F, NONE)
                , PartPose.offset(-15.0F, -8.0F, 16.0F));
        PartDefinition legBr = body.addOrReplaceChild("leg_br",
CubeListBuilder.create()
        .texOffs(182, 63).addBox(-8.0F, 0.0F, 0.0F, 15.0F, 18.0F, 12.0F, NONE)
        .texOffs(0, 168).addBox(-5.0F, 18.0F, 2.0F, 11.0F, 10.0F, 8.0F, NONE)
        .texOffs(25, 204).addBox(-8.0F, 28.0F, 0.0F, 15.0F, 4.0F, 9.0F, NONE)
        .texOffs(178, 169).addBox(-5.0F, 28.0F, -6.0F, 4.0F, 4.0F, 6.0F, NONE)
        .texOffs(127, 171).addBox(0.0F, 28.0F, -6.0F, 4.0F, 4.0F, 6.0F, NONE)
                , PartPose.offset(15.0F, -8.0F, 16.0F));
        PartDefinition legFl = body.addOrReplaceChild("leg_fl",
CubeListBuilder.create()
        .texOffs(0, 101).addBox(-8.0F, 0.0F, -4.0F, 14.0F, 16.0F, 9.0F, NONE)
        .texOffs(43, 148).addBox(-6.0F, 16.0F, -2.0F, 11.0F, 13.0F, 7.0F, NONE)
        .texOffs(127, 186).addBox(-8.0F, 29.0F, -6.0F, 14.0F, 5.0F, 10.0F, NONE)
        .texOffs(84, 142).addBox(-7.0F, 32.0F, -12.0F, 4.0F, 2.0F, 6.0F, NONE)
        .texOffs(77, 189).addBox(-2.0F, 32.0F, -12.0F, 4.0F, 2.0F, 6.0F, NONE)
        .texOffs(83, 218).addBox(3.0F, 32.0F, -12.0F, 4.0F, 2.0F, 6.0F, NONE)
                , PartPose.offset(-15.0F, -10.0F, -26.0F));
        PartDefinition legFr = body.addOrReplaceChild("leg_fr",
CubeListBuilder.create()
        .texOffs(199, 122).addBox(-8.0F, 0.0F, -4.0F, 14.0F, 16.0F, 9.0F, NONE)
        .texOffs(162, 148).addBox(-6.0F, 16.0F, -2.0F, 11.0F, 13.0F, 7.0F, NONE)
        .texOffs(176, 187).addBox(-8.0F, 29.0F, -6.0F, 14.0F, 5.0F, 10.0F, NONE)
        .texOffs(83, 227).addBox(-7.0F, 32.0F, -12.0F, 4.0F, 2.0F, 6.0F, NONE)
        .texOffs(47, 230).addBox(-2.0F, 32.0F, -12.0F, 4.0F, 2.0F, 6.0F, NONE)
        .texOffs(197, 230).addBox(3.0F, 32.0F, -12.0F, 4.0F, 2.0F, 6.0F, NONE)
                , PartPose.offset(15.0F, -10.0F, -26.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
CubeListBuilder.create()
        .texOffs(109, 126).addBox(-7.0F, -8.0F, -14.0F, 14.0F, 12.0F, 12.0F, NONE)
                , PartPose.offset(0.0F, -12.0F, -30.0F));
        PartDefinition ribL = body.addOrReplaceChild("rib_l",
CubeListBuilder.create()
        .texOffs(27, 189).addBox(-4.0F, -11.0F, -4.0F, 4.0F, 6.0F, 8.0F, NONE)
        .texOffs(52, 189).addBox(-4.0F, -5.0F, 1.0F, 4.0F, 6.0F, 8.0F, NONE)
        .texOffs(123, 202).addBox(-4.0F, 1.0F, 6.0F, 4.0F, 6.0F, 8.0F, NONE)
                , PartPose.offset(-15.0F, -6.0F, -20.0F));
        PartDefinition ribR = body.addOrReplaceChild("rib_r",
CubeListBuilder.create()
        .texOffs(148, 202).addBox(0.0F, -11.0F, -4.0F, 4.0F, 6.0F, 8.0F, NONE)
        .texOffs(225, 202).addBox(0.0F, -5.0F, 1.0F, 4.0F, 6.0F, 8.0F, NONE)
        .texOffs(0, 203).addBox(0.0F, 1.0F, 6.0F, 4.0F, 6.0F, 8.0F, NONE)
                , PartPose.offset(15.0F, -6.0F, -20.0F));
        PartDefinition shoulderL = body.addOrReplaceChild("shoulder_l",
CubeListBuilder.create()
        .texOffs(74, 70).addBox(-12.0F, -14.0F, -8.0F, 12.0F, 18.0F, 10.0F, NONE)
        .texOffs(237, 67).addBox(-10.0F, -20.0F, -5.0F, 4.0F, 7.0F, 4.0F, NONE)
                , PartPose.offset(-14.0F, -10.0F, -28.0F));
        PartDefinition shoulderR = body.addOrReplaceChild("shoulder_r",
CubeListBuilder.create()
        .texOffs(119, 70).addBox(0.0F, -14.0F, -8.0F, 12.0F, 18.0F, 10.0F, NONE)
        .texOffs(164, 70).addBox(6.0F, -20.0F, -5.0F, 4.0F, 7.0F, 4.0F, NONE)
                , PartPose.offset(14.0F, -10.0F, -28.0F));
        PartDefinition tail1 = body.addOrReplaceChild("tail_1",
CubeListBuilder.create()
        .texOffs(109, 99).addBox(-6.0F, -4.0F, 0.0F, 12.0F, 12.0F, 14.0F, NONE)
                , PartPose.offset(0.0F, -6.0F, 16.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
CubeListBuilder.create()
        .texOffs(0, 0).addBox(-16.0F, -16.0F, -14.0F, 32.0F, 20.0F, 16.0F, NONE)
        .texOffs(0, 218).addBox(-17.0F, -18.0F, -22.0F, 34.0F, 4.0F, 7.0F, NONE)
        .texOffs(39, 171).addBox(-10.0F, -5.0F, -24.0F, 20.0F, 8.0F, 9.0F, NONE)
        .texOffs(123, 217).addBox(-8.0F, -8.0F, -32.0F, 16.0F, 6.0F, 7.0F, NONE)
        .texOffs(178, 180).addBox(7.0F, -12.0F, -18.0F, 8.0F, 4.0F, 2.0F, NONE)
        .texOffs(104, 231).addBox(-15.0F, -12.0F, -18.0F, 8.0F, 4.0F, 2.0F, NONE)
        .texOffs(246, 131).addBox(-8.0F, 3.0F, -22.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(246, 140).addBox(-4.0F, 3.0F, -22.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(240, 149).addBox(2.0F, 3.0F, -22.0F, 2.0F, 6.0F, 2.0F, NONE)
        .texOffs(149, 151).addBox(6.0F, 3.0F, -22.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -10.0F, -14.0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2",
CubeListBuilder.create()
        .texOffs(43, 127).addBox(-5.0F, -6.0F, 0.0F, 10.0F, 10.0F, 10.0F, NONE)
        .texOffs(240, 158).addBox(-1.0F, -12.0F, 3.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 14.0F));
        PartDefinition hornL = head.addOrReplaceChild("horn_l",
CubeListBuilder.create()
        .texOffs(149, 169).addBox(0.0F, -2.0F, -6.0F, 6.0F, 8.0F, 8.0F, NONE)
        .texOffs(84, 127).addBox(5.0F, -4.0F, 2.0F, 5.0F, 7.0F, 7.0F, NONE)
        .texOffs(77, 203).addBox(9.0F, -6.0F, 10.0F, 4.0F, 7.0F, 7.0F, NONE)
                , PartPose.offset(13.0F, -12.0F, -8.0F));
        PartDefinition hornR = head.addOrReplaceChild("horn_r",
CubeListBuilder.create()
        .texOffs(98, 171).addBox(-6.0F, -2.0F, -6.0F, 6.0F, 8.0F, 8.0F, NONE)
        .texOffs(98, 188).addBox(-10.0F, -4.0F, 2.0F, 5.0F, 7.0F, 7.0F, NONE)
        .texOffs(100, 203).addBox(-13.0F, -6.0F, 10.0F, 4.0F, 7.0F, 7.0F, NONE)
                , PartPose.offset(-13.0F, -12.0F, -8.0F));
        PartDefinition jaw = head.addOrReplaceChild("jaw",
CubeListBuilder.create()
        .texOffs(80, 151).addBox(-11.0F, -5.0F, -24.0F, 22.0F, 7.0F, 12.0F, NONE)
        .texOffs(0, 230).addBox(-9.0F, -2.0F, -30.0F, 18.0F, 4.0F, 5.0F, NONE)
        .texOffs(47, 117).addBox(-9.0F, -6.0F, -20.0F, 2.0F, 7.0F, 2.0F, NONE)
        .texOffs(56, 117).addBox(-4.0F, -6.0F, -20.0F, 2.0F, 7.0F, 2.0F, NONE)
        .texOffs(65, 117).addBox(2.0F, -6.0F, -20.0F, 2.0F, 7.0F, 2.0F, NONE)
        .texOffs(246, 121).addBox(7.0F, -6.0F, -20.0F, 2.0F, 7.0F, 2.0F, NONE)
        .texOffs(197, 217).addBox(-6.0F, -3.0F, -22.0F, 12.0F, 2.0F, 10.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3",
CubeListBuilder.create()
        .texOffs(199, 168).addBox(-4.0F, -8.0F, 0.0F, 8.0F, 8.0F, 10.0F, NONE)
        .texOffs(236, 179).addBox(0.0F, -13.0F, 3.0F, 2.0F, 5.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 10.0F));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail_4",
CubeListBuilder.create()
        .texOffs(170, 217).addBox(-3.0F, -10.0F, 0.0F, 6.0F, 6.0F, 7.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 10.0F));
        PartDefinition tailBlade = tail4.addOrReplaceChild("tail_blade",
CubeListBuilder.create()
        .texOffs(0, 127).addBox(-5.0F, -12.0F, 0.0F, 10.0F, 10.0F, 11.0F, NONE)
        .texOffs(149, 160).addBox(-1.0F, -18.0F, 2.0F, 2.0F, 6.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, -2.0F, 7.0F));
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

        float yaw = netHeadYaw * ((float) Math.PI / 180.0F);
        float pitch = headPitch * ((float) Math.PI / 180.0F);
        float breath = Mth.sin(ageInTicks * 0.08F);
        float walk = limbSwingAmount;

        // ---- 四足步态：前后对角腿同相（兽类慢跑）----
        float stepA = Mth.cos(limbSwing * 0.6F) * walk;
        float stepB = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * walk;
        this.legFl.xRot = stepA * 0.55F;
        this.legBr.xRot = stepA * 0.55F;
        this.legFr.xRot = stepB * 0.55F;
        this.legBl.xRot = stepB * 0.55F;

        // ---- 呼吸：胸腔起伏 + 驼峰骨板随呼吸轻抬（绝对赋值）----
        this.body.xRot = breath * 0.025F;
        this.body.y = breath * 0.6F;
        this.humpFront.xRot = -0.12F + breath * 0.045F;
        this.humpMid.xRot = -0.05F + breath * 0.04F;
        this.humpRear.xRot = 0.06F + breath * 0.035F;
        this.core.xScale = 1.0F + breath * 0.10F;
        this.core.yScale = 1.0F + breath * 0.10F;
        this.core.zScale = 1.0F + breath * 0.10F;

        // ---- 头：低头嗅地 → 再抬起（低频），叠加玩家视角 ----
        float bow = Mth.sin(ageInTicks * 0.045F);
        this.neck.xRot = -0.16F + bow * 0.22F + pitch * 0.30F;
        this.neck.yRot = yaw * 0.45F;
        this.head.xRot = 0.12F - bow * 0.20F + pitch * 0.65F;
        this.head.yRot = yaw * 0.85F;

        // ---- 下颚开合：呼吸 + 偶尔的咆哮（双频叠加）----
        float roar = Mth.sin(ageInTicks * 0.13F) * Mth.sin(ageInTicks * 0.037F);
        this.jaw.xRot = 0.10F + breath * 0.10F + Math.max(0.0F, roar) * 0.55F;
        this.hornL.xRot = this.head.xRot * 0.35F;
        this.hornR.xRot = this.head.xRot * 0.35F;

        // ---- 尾：上翘 + 横扫（4 节相位延迟，末端甩得更狠）----
        float wag = ageInTicks * 0.075F;
        this.tail1.yRot = Mth.sin(wag) * 0.20F;
        this.tail2.yRot = Mth.sin(wag - 0.7F) * 0.26F;
        this.tail3.yRot = Mth.sin(wag - 1.4F) * 0.30F;
        this.tail4.yRot = Mth.sin(wag - 2.1F) * 0.34F;
        this.tailBlade.yRot = Mth.sin(wag - 2.8F) * 0.38F;
        this.tail1.xRot = -0.10F + breath * 0.03F;
        this.tail2.xRot = -0.14F + breath * 0.03F;
        this.tailBlade.xRot = -0.18F + breath * 0.10F;

    }

    /** 最低点像素 y（四只爪子/尾锤都不低于 24）；BeastLordRenderer 用它算 GROUND_LIFT。 */
    public static final double LOWEST_PIXEL_Y = 24.0;

}
