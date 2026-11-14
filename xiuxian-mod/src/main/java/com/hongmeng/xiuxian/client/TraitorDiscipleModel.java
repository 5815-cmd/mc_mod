package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.GuiltySectDiscipleEntity;
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
 * 叛徒执事 · TraitorDisciple / TraitorDiscipleModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>门派执事（叛门追杀者）：人形骨架 + 鬼面罩（额角 + 獠牙）+ 胸前教徽 + 锁链腕环；三派（千机青钢 / 神霄赤铁 / 魔渊紫黑）共用骨架，只换贴图配色。</p>
 */
public class TraitorDiscipleModel extends HumanoidModel<GuiltySectDiscipleEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "traitor_disciple"), "main");

    private final ModelPart root;
    private final ModelPart cuffL;
    private final ModelPart cuffR;
    private final ModelPart emblem;
    private final ModelPart mask;

    public TraitorDiscipleModel(ModelPart root) {
        super(root);
        this.root = root;
        this.cuffL = root.getChild("left_arm").getChild("cuff_l");
        this.cuffR = root.getChild("right_arm").getChild("cuff_r");
        this.emblem = root.getChild("body").getChild("emblem");
        this.mask = root.getChild("head").getChild("mask");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition cuffL = root.getChild("left_arm").addOrReplaceChild("cuff_l",
CubeListBuilder.create()
        .texOffs(21, 32).addBox(-2.5F, 8.0F, -2.5F, 5.0F, 2.0F, 5.0F, NONE)
        .texOffs(16, 40).addBox(-0.5F, 10.0F, -0.5F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition cuffR = root.getChild("right_arm").addOrReplaceChild("cuff_r",
CubeListBuilder.create()
        .texOffs(0, 32).addBox(-2.5F, 8.0F, -2.5F, 5.0F, 2.0F, 5.0F, NONE)
        .texOffs(11, 40).addBox(-0.5F, 10.0F, -0.5F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition emblem = root.getChild("body").addOrReplaceChild("emblem",
CubeListBuilder.create()
        .texOffs(0, 40).addBox(-2.0F, 2.0F, -3.0F, 4.0F, 4.0F, 1.0F, NONE)
        .texOffs(56, 16).addBox(-1.0F, 3.0F, -4.0F, 2.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition mask = root.getChild("head").addOrReplaceChild("mask",
CubeListBuilder.create()
        .texOffs(42, 32).addBox(-4.0F, -8.0F, -5.0F, 8.0F, 6.0F, 1.0F, NONE)
        .texOffs(56, 20).addBox(-0.5F, -10.0F, -5.0F, 1.0F, 2.0F, 1.0F, NONE)
        .texOffs(56, 24).addBox(-3.0F, -2.0F, -6.0F, 1.0F, 2.0F, 1.0F, NONE)
        .texOffs(56, 28).addBox(2.0F, -2.0F, -6.0F, 1.0F, 2.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }


    @Override
    public void setupAnim(GuiltySectDiscipleEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 锁链腕环：走路/挥剑时比手臂慢半拍地晃（绝对赋值，不累加）
        this.cuffR.xRot = Mth.sin(ageInTicks * 0.17F) * 0.14F;
        this.cuffR.zRot = Mth.cos(ageInTicks * 0.13F) * 0.08F;
        this.cuffL.xRot = Mth.sin(ageInTicks * 0.17F + 2.1F) * 0.14F;
        this.cuffL.zRot = Mth.cos(ageInTicks * 0.13F + 2.1F) * 0.08F;

        // 胸前教徽：呼吸般微微外凸（用 z 缩放，不动位置）
        this.emblem.zScale = 1.0F + Mth.sin(ageInTicks * 0.09F) * 0.06F;

    }
}
