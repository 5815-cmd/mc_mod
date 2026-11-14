package com.hongmeng.xiuxian.client;

import com.google.common.collect.ImmutableList;
import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.StoneGolemEntity;
import net.minecraft.client.model.ColorableAgeableListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 石傀的渲染器 + 自建模型。
 *
 * <p>剪影要点：<b>方、宽、没有脖子、肩上顶着两块石板</b>。它比烈焰猿还要宽一圈，
 * 头却更小（8 格），两只拳头各 9 格——远远看过去就是一块长着手脚的石头。</p>
 *
 * <p>贴图 {@code 256×128}（17 个盒子，UV 见 {@code tools/gen_beast_textures.py}）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class StoneGolemRenderer extends MobRenderer<StoneGolemEntity, StoneGolemRenderer.GolemModel> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(StoneGolemEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        groundAlign(poseStack);
    }

    /**
     * 把模型**顶到地面上**（基准：模型最低点应落在模型空间 y=24，原版渲染管线会把 24 像素对齐到地面）。
     *
     * <p>原版在画模型之前会 {@code translate(0, -1.501, 0)}（1.501 格 = 24.016 像素），
     * 所以**模型空间的最低点必须停在 y=24**才正好踩在地上。
     * 本模组的骨架已经照这个基准建好了，正常情况下这个值就是 0；
     * 只有整体最低点不在 24 的模型（蛟龙 / 虚空蛟这类没有腿的蛇身）才非 0。</p>
     *
     * <p>数值 = (模型最低点像素 y - 24) / 16，由
     * {@code tools/audit_model_ground.py} 实测写出，逐个可核。</p>
     */
    private static void groundAlign(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    /** 离地修正量（格）：正数=上移，负数=下移。= (模型最低点像素 y - 24) / 16。 */
    private static final double GROUND_LIFT = 0.00D;


    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "stone_golem"), "main");

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/stone_golem.png");

    public StoneGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(LAYER)), 1.0F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(StoneGolemEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(StoneGolemEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：队长 ×1.12（倍率表在 MobRank，本类不写死数字）。 */
    @Override
    protected void scale(StoneGolemEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }

    /**
     * 石傀模型。
     *
     * <p>骨架（脚底 y=24）：脚 24→20、小腿 20→11、大腿 11→2、躯干 2→-12、
     * 头 -12→-20；护肩挂在躯干顶端两侧，双拳垂到 y=18（离地 6 格）。</p>
     */
    public static class GolemModel extends ColorableAgeableListModel<StoneGolemEntity> {

        private final ModelPart head;
        private final ModelPart torso;
        private final ModelPart core;
        private final ModelPart rightShoulder;
        private final ModelPart leftShoulder;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightForeArm;
        private final ModelPart leftForeArm;
        private final ModelPart rightFist;
        private final ModelPart leftFist;
        private final ModelPart rightThigh;
        private final ModelPart leftThigh;
        private final ModelPart rightShin;
        private final ModelPart leftShin;
        private final ModelPart rightFoot;
        private final ModelPart leftFoot;

        public GolemModel(ModelPart root) {
            this.head = root.getChild("head");
            this.torso = root.getChild("torso");
            this.core = this.torso.getChild("core");
            this.rightShoulder = this.torso.getChild("right_shoulder");
            this.leftShoulder = this.torso.getChild("left_shoulder");
            this.rightArm = root.getChild("right_upper_arm");
            this.leftArm = root.getChild("left_upper_arm");
            this.rightForeArm = this.rightArm.getChild("right_fore_arm");
            this.leftForeArm = this.leftArm.getChild("left_fore_arm");
            this.rightFist = this.rightForeArm.getChild("right_fist");
            this.leftFist = this.leftForeArm.getChild("left_fist");
            this.rightThigh = root.getChild("right_thigh");
            this.leftThigh = root.getChild("left_thigh");
            this.rightShin = this.rightThigh.getChild("right_shin");
            this.leftShin = this.leftThigh.getChild("left_shin");
            this.rightFoot = this.rightShin.getChild("right_foot");
            this.leftFoot = this.leftShin.getChild("left_foot");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation none = CubeDeformation.NONE;

            root.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(66, 35).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F, none),
                    PartPose.offset(0.0F, -20.0F, -1.0F));

            PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-9.0F, 0.0F, -5.0F, 18.0F, 14.0F, 10.0F, none),
                    PartPose.offset(0.0F, -12.0F, 0.0F));
            // 胸口的灵核：一块外凸的发光石头，会慢慢转
            torso.addOrReplaceChild("core", CubeListBuilder.create()
                            .texOffs(74, 61).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 6.0F, 3.0F, none),
                    PartPose.offset(0.0F, 4.0F, -6.0F));
            // 护肩：两块搭在肩上的石板。盒子从躯干外缘往**内**搭（-4..4），
            // 不是往外伸——往外伸会让模型比命中箱宽出半格，看着像"飘在身体外面"。
            torso.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                            .texOffs(208, 35).addBox(-4.0F, 0.0F, -4.5F, 8.0F, 5.0F, 9.0F, none),
                    PartPose.offset(-9.0F, -1.0F, 0.0F));
            torso.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                            .texOffs(173, 35).addBox(-4.0F, 0.0F, -4.5F, 8.0F, 5.0F, 9.0F, none),
                    PartPose.offset(9.0F, -1.0F, 0.0F));

            // 手臂：原点 ±10.5，比躯干外缘（±9）只出去一格半，
            // 拳头顶端落在 ±15 像素 = 0.94 格，正好收在 1.8 格宽的命中箱里。
            PartDefinition rightArm = root.addOrReplaceChild("right_upper_arm", CubeListBuilder.create()
                            .texOffs(152, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 12.0F, 7.0F, none),
                    PartPose.offset(-10.5F, -11.0F, 0.0F));
            rightArm.addOrReplaceChild("right_fore_arm", CubeListBuilder.create()
                            .texOffs(90, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 8.0F, none),
                    PartPose.offset(0.0F, 12.0F, 0.0F));
            rightArm.getChild("right_fore_arm").addOrReplaceChild("right_fist", CubeListBuilder.create()
                            .texOffs(136, 35).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 5.0F, 9.0F, none),
                    PartPose.offset(0.0F, 12.0F, 0.0F));

            PartDefinition leftArm = root.addOrReplaceChild("left_upper_arm", CubeListBuilder.create()
                            .texOffs(123, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 12.0F, 7.0F, none),
                    PartPose.offset(10.5F, -11.0F, 0.0F));
            leftArm.addOrReplaceChild("left_fore_arm", CubeListBuilder.create()
                            .texOffs(57, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 8.0F, none),
                    PartPose.offset(0.0F, 12.0F, 0.0F));
            leftArm.getChild("left_fore_arm").addOrReplaceChild("left_fist", CubeListBuilder.create()
                            .texOffs(99, 35).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 5.0F, 9.0F, none),
                    PartPose.offset(0.0F, 12.0F, 0.0F));

            PartDefinition rightThigh = root.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                            .texOffs(33, 35).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 9.0F, 8.0F, none),
                    PartPose.offset(-4.5F, 2.0F, 0.0F));
            rightThigh.addOrReplaceChild("right_shin", CubeListBuilder.create()
                            .texOffs(214, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 9.0F, 8.0F, none),
                    PartPose.offset(0.0F, 9.0F, 0.0F));
            rightThigh.getChild("right_shin").addOrReplaceChild("right_foot", CubeListBuilder.create()
                            .texOffs(37, 61).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 4.0F, 9.0F, none),
                    PartPose.offset(0.0F, 9.0F, -1.0F));

            PartDefinition leftThigh = root.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                            .texOffs(0, 35).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 9.0F, 8.0F, none),
                    PartPose.offset(4.5F, 2.0F, 0.0F));
            leftThigh.addOrReplaceChild("left_shin", CubeListBuilder.create()
                            .texOffs(181, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 9.0F, 8.0F, none),
                    PartPose.offset(0.0F, 9.0F, 0.0F));
            leftThigh.getChild("left_shin").addOrReplaceChild("left_foot", CubeListBuilder.create()
                            .texOffs(0, 61).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 4.0F, 9.0F, none),
                    PartPose.offset(0.0F, 9.0F, -1.0F));

            return LayerDefinition.create(mesh, 256, 128);
        }

        @Override
        protected Iterable<ModelPart> headParts() {
            return ImmutableList.of(this.head);
        }

        @Override
        protected Iterable<ModelPart> bodyParts() {
            return ImmutableList.of(this.torso, this.rightArm, this.leftArm,
                    this.rightThigh, this.leftThigh);
        }

        /**
         * 石头该怎么动：<b>慢、沉、幅度小</b>。
         *
         * <p>四肢摆幅只有人形怪的一半，躯干几乎不晃（石傀没有腰），
         * 但胸口的灵核一直在慢慢转——那是它身上唯一"活着"的地方，
         * 也是给玩家的提示：打它胸口。</p>
         */
        @Override
        public void setupAnim(StoneGolemEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            float swing = Mth.cos(limbSwing * 0.5F) * 0.55F * limbSwingAmount;

            this.rightThigh.xRot = swing;
            this.leftThigh.xRot = -swing;
            this.rightShin.xRot = Math.max(0.0F, -swing) * 0.8F;
            this.leftShin.xRot = Math.max(0.0F, swing) * 0.8F;
            this.rightFoot.xRot = -this.rightShin.xRot * 0.5F;
            this.leftFoot.xRot = -this.leftShin.xRot * 0.5F;

            this.rightArm.xRot = -swing * 0.6F;
            this.leftArm.xRot = swing * 0.6F;
            this.rightForeArm.xRot = 0.08F;
            this.leftForeArm.xRot = 0.08F;
            this.rightArm.zRot = -0.06F - limbSwingAmount * 0.03F;
            this.leftArm.zRot = 0.06F + limbSwingAmount * 0.03F;

            // 头：转向玩家，但慢半拍
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.45F;
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.45F;

            // 护肩跟着躯干（它们是躯干的子部件，这里只补一点随步伐的震动）
            this.rightShoulder.zRot = -swing * 0.04F;
            this.leftShoulder.zRot = swing * 0.04F;

            // 灵核缓慢自转 + 呼吸般的明暗节奏（明暗靠贴图，转动靠这里）
            this.core.yRot = ageInTicks * 0.06F;
            this.core.xRot = Mth.sin(ageInTicks * 0.05F) * 0.12F;
        }
    }
}
