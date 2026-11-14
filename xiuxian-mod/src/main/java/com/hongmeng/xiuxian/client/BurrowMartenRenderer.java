package com.hongmeng.xiuxian.client;

import com.google.common.collect.ImmutableList;
import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.BurrowMartenEntity;
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
 * 土行貂的渲染器 + 自建模型。
 *
 * <p>剪影要点：<b>又低又长的一条，四条短腿缩在肚子底下，前爪大得离谱，
 * 尾巴比身体还蓬</b>。跟灵狼比：灵狼是"高腿 + 尖吻 + 立着的尾"，
 * 土行貂是"贴地 + 短吻 + 拖在后面的粗尾"，一眼就能分开。</p>
 *
 * <p>前爪刻意做成 3×3×5 的方块（比后腿还粗一截）：它不是用来走路的，
 * 是用来刨土的——<b>外形直接说明职能</b>。</p>
 *
 * <p>贴图 {@code 128×64}，UV 由 {@code tools/gen_rank_beast_textures.py}
 * 的货架装箱器算出，改这里必须同步改那个脚本。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BurrowMartenRenderer
        extends MobRenderer<BurrowMartenEntity, BurrowMartenRenderer.MartenModel> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(BurrowMartenEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
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
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "burrow_marten"), "main");

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/burrow_marten.png");

    public BurrowMartenRenderer(EntityRendererProvider.Context context) {
        super(context, new MartenModel(context.bakeLayer(LAYER)), 0.42F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(BurrowMartenEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(BurrowMartenEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：精英 ×1.05（倍率表在 MobRank）。 */
    @Override
    protected void scale(BurrowMartenEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }

    /**
     * 貂形模型（<b>脚底 y=24</b>，模型 +y 向下——原版 {@code LivingEntityRenderer}
     * 在渲染前会 {@code translate(0, -1.501, 0)}，正好把 24 像素的对齐到地面）。
     *
     * <p>骨架：身体 12→20、胸口 12.5→19.5、头 11→17（在 z=-13 处往前伸）、
     * 吻 14→17、前爪 21→24（y 方向最底，负责刨土）、后腿 18→24、
     * 尾根挂在 y=14 往上翘。整只高 0.94 格。</p>
     */
    public static class MartenModel extends ColorableAgeableListModel<BurrowMartenEntity> {

        private final ModelPart head;
        private final ModelPart snout;
        private final ModelPart jaw;
        private final ModelPart leftEar;
        private final ModelPart rightEar;
        private final ModelPart body;
        private final ModelPart chest;
        private final ModelPart dorsal;
        private final ModelPart leftClaw;
        private final ModelPart rightClaw;
        private final ModelPart leftHindLeg;
        private final ModelPart rightHindLeg;
        private final ModelPart tailBase;
        private final ModelPart tailTip;

        public MartenModel(ModelPart root) {
            this.head = root.getChild("head");
            this.snout = this.head.getChild("snout");
            this.jaw = this.head.getChild("jaw");
            this.leftEar = this.head.getChild("left_ear");
            this.rightEar = this.head.getChild("right_ear");
            this.body = root.getChild("body");
            this.chest = root.getChild("chest");
            this.dorsal = this.body.getChild("dorsal");
            this.leftClaw = root.getChild("left_claw");
            this.rightClaw = root.getChild("right_claw");
            this.leftHindLeg = root.getChild("left_hind_leg");
            this.rightHindLeg = root.getChild("right_hind_leg");
            this.tailBase = root.getChild("tail_base");
            this.tailTip = this.tailBase.getChild("tail_tip");
        }

        /** 10 个盒子；尺寸必须与 gen_rank_beast_textures.py 的 MARTEN_PARTS 一致。 */
        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation none = CubeDeformation.NONE;

            // ---- 躯干：又低又长（模型脚底一律在 y=24，与原版渲染管线的 -1.501 偏移对齐） ----
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-4.0F, 0.0F, -8.0F, 8.0F, 8.0F, 16.0F, none),
                    PartPose.offset(0.0F, 12.0F, 0.0F));
            // 背脊：一道窄脊，让它趴着的时候也有轮廓
            body.addOrReplaceChild("dorsal", CubeListBuilder.create()
                            .texOffs(48, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 1.0F, 12.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));

            root.addOrReplaceChild("chest", CubeListBuilder.create()
                            .texOffs(0, 40).addBox(-3.5F, 0.0F, -3.0F, 7.0F, 7.0F, 5.0F, none),
                    PartPose.offset(0.0F, 12.5F, -8.0F));

            // ---- 头：小、尖、两只圆耳 ----
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(104, 0).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 6.0F, 6.0F, none),
                    PartPose.offset(0.0F, 11.0F, -13.0F));
            head.addOrReplaceChild("snout", CubeListBuilder.create()
                            .texOffs(72, 40).addBox(-2.0F, 3.0F, -8.0F, 4.0F, 3.0F, 4.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            head.addOrReplaceChild("jaw", CubeListBuilder.create()
                            .texOffs(88, 40).addBox(-2.0F, 6.0F, -7.0F, 4.0F, 1.0F, 3.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                            .texOffs(102, 40).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 1.0F, none),
                    PartPose.offset(-2.0F, 0.0F, -2.0F));
            head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                            .texOffs(102, 40).mirror().addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 1.0F, none),
                    PartPose.offset(2.0F, 0.0F, -2.0F));

            // ---- 前爪：刨土的工具，比后腿粗一圈 ----
            root.addOrReplaceChild("left_claw", CubeListBuilder.create()
                            .texOffs(56, 40).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 3.0F, 5.0F, none),
                    PartPose.offset(-3.0F, 21.0F, -11.0F));
            root.addOrReplaceChild("right_claw", CubeListBuilder.create()
                            .texOffs(56, 40).mirror().addBox(-1.5F, 0.0F, -2.0F, 3.0F, 3.0F, 5.0F, none),
                    PartPose.offset(3.0F, 21.0F, -11.0F));

            // ---- 后腿：短，但蹬地有力 ----
            root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                            .texOffs(42, 40).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F, none),
                    PartPose.offset(-3.0F, 18.0F, 5.0F));
            root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                            .texOffs(42, 40).mirror().addBox(-1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F, none),
                    PartPose.offset(3.0F, 18.0F, 5.0F));

            // ---- 尾巴：往上翘，末端蓬起来 ----
            PartDefinition tail = root.addOrReplaceChild("tail_base", CubeListBuilder.create()
                            .texOffs(24, 40).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F, none),
                    PartPose.offsetAndRotation(0.0F, 14.0F, 8.0F, 0.35F, 0.0F, 0.0F));
            tail.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                            .texOffs(76, 0).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 9.0F, none),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 0.25F, 0.0F, 0.0F));

            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        protected Iterable<ModelPart> headParts() {
            return ImmutableList.of(this.head);
        }

        @Override
        protected Iterable<ModelPart> bodyParts() {
            return ImmutableList.of(this.body, this.chest, this.leftClaw, this.rightClaw,
                    this.leftHindLeg, this.rightHindLeg, this.tailBase);
        }

        /**
         * 貂的走法：贴地小碎步。
         *
         * <p>四条腿的相位照四足的标准写法（对角腿同相），
         * 但<b>前爪的摆幅是后腿的 1.6 倍</b>——它平时也一直在刨，
         * 这是"遁地"这个职能留在动画上的痕迹。</p>
         */
        @Override
        public void setupAnim(BurrowMartenEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.8F;
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.8F;
            this.snout.xRot = 0.05F + Mth.sin(ageInTicks * 0.13F) * 0.05F;
            this.jaw.xRot = 0.10F + Mth.sin(ageInTicks * 0.17F) * 0.12F;
            this.leftEar.zRot = Mth.cos(ageInTicks * 0.11F) * 0.12F;
            this.rightEar.zRot = -Mth.cos(ageInTicks * 0.11F) * 0.12F;

            float swing = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
            this.leftClaw.xRot = swing * 2.1F;
            this.rightClaw.xRot = -swing * 2.1F;
            this.leftHindLeg.xRot = -swing * 1.3F;
            this.rightHindLeg.xRot = swing * 1.3F;

            // 身体随步伐**横向呼吸**（绝对赋值：每帧的基准都是 1.0，不会累乘）。
            // 这里原本是 `this.body.zRot = cos(...)`：躯干自己滚转，
            // 而头与四条腿都是 root 的子节点、不跟着转 —— 接缝处看着就是"散架"。
            float breath = Mth.sin(ageInTicks * 0.08F) * 0.012F;
            this.body.xScale = 1.0F + breath;
            this.body.zScale = 1.0F + breath;
            this.tailBase.xRot = 0.35F + Mth.sin(ageInTicks * 0.09F) * 0.10F
                    + limbSwingAmount * 0.18F;
            this.tailBase.yRot = Mth.sin(ageInTicks * 0.07F) * 0.22F;
            this.tailTip.yRot = Mth.sin(ageInTicks * 0.07F + 0.6F) * 0.28F;
            this.dorsal.xRot = -0.04F + Mth.sin(ageInTicks * 0.1F) * 0.03F;
        }
    }
}
