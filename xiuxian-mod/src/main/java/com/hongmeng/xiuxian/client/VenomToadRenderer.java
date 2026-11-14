package com.hongmeng.xiuxian.client;

import com.google.common.collect.ImmutableList;
import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.VenomToadEntity;
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
 * 毒涎蟾的渲染器 + 自建模型。
 *
 * <p>剪影要点：<b>一团蹲着的方块，宽比高还长，头顶两只鼓眼，下巴挂着鸣囊，
 * 后腿折成两团肉</b>。跟石傀比：石傀"高、方、有肩甲"，毒涎蟾"矮、宽、圆"，
 * 两个都是宽体，但一个站着、一个趴着。</p>
 *
 * <p>下巴的鸣囊（{@code throat}）会一直鼓动——那是它在"咕咕"地蓄毒；
 * 喷吐之后嘴会张开（{@code jaw} 跟着 {code throat} 的节拍开合），
 * 玩家因此能从动作预判下一发。</p>
 *
 * <p>贴图 {@code 128×64}，UV 由 {@code tools/gen_rank_beast_textures.py} 算出。</p>
 */
@OnlyIn(Dist.CLIENT)
public class VenomToadRenderer extends MobRenderer<VenomToadEntity, VenomToadRenderer.ToadModel> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(VenomToadEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
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
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "venom_toad"), "main");

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/venom_toad.png");

    public VenomToadRenderer(EntityRendererProvider.Context context) {
        super(context, new ToadModel(context.bakeLayer(LAYER)), 0.55F);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(VenomToadEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(VenomToadEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型：队长 ×1.12（倍率表在 MobRank）。 */
    @Override
    protected void scale(VenomToadEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }

    /**
     * 蟾形模型（<b>脚底 y=24</b>，原版渲染管线会把 24 像素对齐到地面）。
     *
     * <p>骨架：身体 10→17（12 宽 13 长，最宽的一块）、头 10.5→16.5、
     * 下颚往前探到 z=-13、鼓眼顶在头上（y 8→11，比头还高）、
     * 鸣囊挂在 15→19、前腿 14→24、折起来的后腿 13→24 + 脚掌。整只高 1.0 格。</p>
     */
    public static class ToadModel extends ColorableAgeableListModel<VenomToadEntity> {

        private final ModelPart head;
        private final ModelPart jaw;
        private final ModelPart leftEye;
        private final ModelPart rightEye;
        private final ModelPart body;
        /** body 的基准 y（构造时定下）；每帧的呼吸起伏要用它做基准，
         *  不能写 `this.body.y = this.body.y + ...`（那就是 +=，会跨帧累加）。 */
        private final float bodyBaseY;
        private final ModelPart throat;
        private final ModelPart wartA;
        private final ModelPart wartB;
        private final ModelPart wartC;
        private final ModelPart leftFrontLeg;
        private final ModelPart rightFrontLeg;
        private final ModelPart leftHindLeg;
        private final ModelPart rightHindLeg;
        private final ModelPart leftFoot;
        private final ModelPart rightFoot;

        public ToadModel(ModelPart root) {
            this.head = root.getChild("head");
            this.jaw = this.head.getChild("jaw");
            this.leftEye = this.head.getChild("left_eye");
            this.rightEye = this.head.getChild("right_eye");
            this.body = root.getChild("body");
            this.bodyBaseY = this.body.y;
            this.throat = root.getChild("throat");
            this.wartA = this.body.getChild("wart_a");
            this.wartB = this.body.getChild("wart_b");
            this.wartC = this.body.getChild("wart_c");
            this.leftFrontLeg = root.getChild("left_front_leg");
            this.rightFrontLeg = root.getChild("right_front_leg");
            this.leftHindLeg = root.getChild("left_hind_leg");
            this.rightHindLeg = root.getChild("right_hind_leg");
            this.leftFoot = this.leftHindLeg.getChild("left_foot");
            this.rightFoot = this.rightHindLeg.getChild("right_foot");
        }

        /** 9 个盒子（疣是同一个盒子的三份）；尺寸与 gen_rank_beast_textures.py 一致。 */
        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation none = CubeDeformation.NONE;

            // ---- 身体：又宽又扁（脚底一律在 y=24，与原版渲染管线的 -1.501 偏移对齐） ----
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 7.0F, 13.0F, none),
                    PartPose.offset(0.0F, 10.0F, 1.0F));
            // 背上的疣：三颗，位置错开（同一个盒子，靠位置区分）
            body.addOrReplaceChild("wart_a", CubeListBuilder.create()
                            .texOffs(88, 33).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, none),
                    PartPose.offset(-3.0F, 0.0F, -2.0F));
            body.addOrReplaceChild("wart_b", CubeListBuilder.create()
                            .texOffs(88, 33).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, none),
                    PartPose.offset(2.0F, -1.0F, 3.0F));
            body.addOrReplaceChild("wart_c", CubeListBuilder.create()
                            .texOffs(88, 33).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, none),
                    PartPose.offset(0.0F, -1.5F, 6.0F));

            // ---- 头：与身体连在一起（蟾蜍没有脖子） ----
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(98, 0).addBox(-5.0F, 0.0F, -4.0F, 10.0F, 6.0F, 5.0F, none),
                    PartPose.offset(0.0F, 10.5F, -5.0F));
            // 下颚：一张又宽又低的嘴，往前探出一截
            head.addOrReplaceChild("jaw", CubeListBuilder.create()
                            .texOffs(0, 33).addBox(-5.0F, 5.0F, -8.0F, 10.0F, 3.0F, 8.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // 鼓眼：顶在头上，比头还高
            head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                            .texOffs(76, 33).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F, none),
                    PartPose.offset(-3.0F, 0.5F, -3.0F));
            head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                            .texOffs(76, 33).mirror().addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F, none),
                    PartPose.offset(3.0F, 0.5F, -3.0F));

            // ---- 鸣囊：下巴底下那个会鼓的袋子 ----
            root.addOrReplaceChild("throat", CubeListBuilder.create()
                            .texOffs(36, 33).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 4.0F, 6.0F, none),
                    PartPose.offset(0.0F, 15.0F, -6.0F));

            // ---- 前腿：短细，撑住前半身 ----
            root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                            .texOffs(64, 33).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, none),
                    PartPose.offset(-5.0F, 14.0F, -4.0F));
            root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                            .texOffs(64, 33).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, none),
                    PartPose.offset(5.0F, 14.0F, -4.0F));

            // ---- 后腿：折起来的粗腿 + 大脚掌（青蛙腿） ----
            PartDefinition leftHind = root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                            .texOffs(50, 0).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 11.0F, 7.0F, none),
                    PartPose.offset(-6.0F, 13.0F, 3.0F));
            leftHind.addOrReplaceChild("left_foot", CubeListBuilder.create()
                            .texOffs(72, 0).addBox(-2.0F, 8.0F, -3.0F, 4.0F, 3.0F, 9.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            PartDefinition rightHind = root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                            .texOffs(50, 0).mirror().addBox(-2.0F, 0.0F, -3.0F, 4.0F, 11.0F, 7.0F, none),
                    PartPose.offset(6.0F, 13.0F, 3.0F));
            rightHind.addOrReplaceChild("right_foot", CubeListBuilder.create()
                            .texOffs(72, 0).mirror().addBox(-2.0F, 8.0F, -3.0F, 4.0F, 3.0F, 9.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));

            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        protected Iterable<ModelPart> headParts() {
            return ImmutableList.of(this.head);
        }

        @Override
        protected Iterable<ModelPart> bodyParts() {
            return ImmutableList.of(this.body, this.throat, this.leftFrontLeg, this.rightFrontLeg,
                    this.leftHindLeg, this.rightHindLeg);
        }

        /**
         * 蟾的动作：<b>鸣囊一直在鼓，嘴跟着开合，后腿一蹬一蹬</b>。
         *
         * <p>豚跳的写法：四条腿都按同一个相位摆动，但后腿的幅度是前腿的两倍，
         * 于是看起来是"蹲—蹬—落"，而不是四足动物那种对角步。</p>
         */
        @Override
        public void setupAnim(VenomToadEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.7F;
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.7F;

            // 鸣囊：一直鼓（静止时最明显，走动时被步伐盖掉一点）
            float croak = Mth.sin(ageInTicks * 0.14F);
            float puff = 1.0F + 0.16F * croak;
            this.throat.xScale = puff;
            this.throat.yScale = puff;
            this.throat.zScale = 1.0F + 0.08F * croak;
            this.throat.xRot = 0.05F * croak;

            // 嘴：鸣囊鼓到最大的时候张开，读者能预判"要吐了"
            this.jaw.xRot = 0.06F + Math.max(0.0F, croak) * 0.30F;
            this.leftEye.zRot = Mth.cos(ageInTicks * 0.09F) * 0.08F;
            this.rightEye.zRot = -Mth.cos(ageInTicks * 0.09F) * 0.08F;

            float hop = Mth.cos(limbSwing * 0.8F) * limbSwingAmount;
            this.leftHindLeg.xRot = -hop * 1.1F;
            this.rightHindLeg.xRot = -hop * 1.1F;
            this.leftFoot.xRot = Math.max(0.0F, hop) * 0.9F;
            this.rightFoot.xRot = Math.max(0.0F, hop) * 0.9F;
            this.leftFrontLeg.xRot = hop * 0.55F;
            this.rightFrontLeg.xRot = hop * 0.55F;

            // 身体随呼吸轻微起伏，疣跟着抖。
            // **绝对赋值**：`this.body.y = this.body.y + v` 与 `+=` 等价，
            // setupAnim 每帧都跑，本模型没有 resetPose —— 只能拿构造时的
            // 基准 bodyBaseY 加上本帧的起伏量。
            this.body.y = this.bodyBaseY + Mth.sin(ageInTicks * 0.14F) * 0.02F;
            this.wartA.yRot = Mth.sin(ageInTicks * 0.05F) * 0.4F;
            this.wartB.yRot = -Mth.sin(ageInTicks * 0.06F) * 0.4F;
            this.wartC.yRot = Mth.sin(ageInTicks * 0.07F) * 0.4F;
        }
    }
}
