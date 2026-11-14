package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.SpiritWolfEntity;
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
 * 灵狼的渲染器。
 *
 * <p>自己做了一套四足模型（原版 {@code WolfModel} 被写死成只能配 {@code Wolf}，
 * 而灵狼是敌对怪，继承不到那支上去）。模型尺寸与原版狼一致，
 * 贴图沿用原版狼的 UV 布局——同样是重新染色的版本，保证脸和腿不会错位。</p>
 *
 * <h3>为什么要泛型 + 可换贴图</h3>
 *
 * <p>第三批妖兽里的<b>赤炎狼</b>（{@code CrimsonFlameWolfEntity}）是灵狼的火行亚种，
 * <b>特意继承 {@code SpiritWolfEntity}</b>——这样它的扑击 AI、四足骨架、
 * 甚至"能被契灵符收服"的判定全部自动续上，玩家一眼就认出"这是狼"。
 * 于是渲染器只需要多一个"贴图名"参数（狼身 64×32，UV 与原版狼一致）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class SpiritWolfRenderer extends MobRenderer<SpiritWolfEntity, SpiritWolfRenderer.WolfBody<SpiritWolfEntity>> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(SpiritWolfEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
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


    /** 自建模型层：与原版狼同布局，但不受 Wolf 类型约束。 */
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "spirit_wolf"), "main");

    private final ResourceLocation texture;

    public SpiritWolfRenderer(EntityRendererProvider.Context context) {
        this(context, "spirit_wolf");
    }

    /**
     * @param textureName 贴图名（不含 {@code .png}）：灵狼 {@code spirit_wolf}、
     *                    赤炎狼 {@code crimson_flame_wolf}（两者都是原版狼的 64×32 UV）
     */
    public SpiritWolfRenderer(EntityRendererProvider.Context context, String textureName) {
        super(context, new WolfBody<SpiritWolfEntity>(context.bakeLayer(LAYER)), 0.4F);
        this.texture = com.hongmeng.xiuxian.entity.CultivationMonsterEntity.textureFor(textureName);
        // 品阶发光层：灵狼是普通怪，这一层当前不画东西；
        // 但留着它，以后谁把某只狼调成精英就自动有发光眼睛
        // （赤炎狼正是精英——这一层立刻就有用了）
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritWolfEntity entity) {
        return this.texture;
    }

    /** 灵狼是稀有度最低的普通怪，只有被命名／被注视时才显示名牌。 */
    @Override
    protected boolean shouldShowName(SpiritWolfEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /**
     * 品阶体型（普通怪 = ×1，接上表之后"以后调品阶"不用改这里）。
     *
     * <p>注意<b>成长体型不在这里</b>：灵狼 / 赤炎狼走的是 {@code CultivationMonsterEntity} 那条链
     * （有 {@code MobRank} 品阶、没有成长阶段）。"幼年 0.45 / 成年 1.0 / 化形 1.6" 的
     * {@code SpiritBeastEntity#getScale()} 只接到三条灵兽渲染器上：
     * {@link ContractWolfRenderer}（契约灵狼，复用本类的 {@link WolfBody} 骨架）、
     * {@link ContractEagleRenderer}、{@link JiaolongRenderer}。</p>
     */
    @Override
    protected void scale(SpiritWolfEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }

    /**
     * 四足狼形模型：14 个部件全部照原版狼的坐标摆放，
     * 贴图 UV 因此可以直接复用原版 {@code wolf.png} 的布局。
     *
     * <p>泛型参数刻意写成 {@code <T extends Animal>} 而不是写死灵狼：
     * 契约灵狼（{@code ContractWolfEntity}）要<b>复用同一套骨架</b>——
     * 它就是"被你收服的那只灵狼"，长得不一样反而说不通。
     * 写死类型会导致 {@code MobRenderer} 的类型参数对不上。</p>
     */
    public static class WolfBody<T extends net.minecraft.world.entity.Entity>
            extends net.minecraft.client.model.EntityModel<T> {

        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart upperBody;
        private final ModelPart tail;
        private final ModelPart rightHindLeg;
        private final ModelPart leftHindLeg;
        private final ModelPart rightFrontLeg;
        private final ModelPart leftFrontLeg;

        // ---- 身份件（同一套骨架，按实体类型显隐；见 setupAnim 末尾）----
        /** 赤炎狼：颈背 3 片火焰鬃毛。 */
        private final ModelPart flameMane;
        /** 赤炎狼：额前双角。 */
        private final ModelPart crimsonHorns;
        /** 赤炎狼：尾焰 2 片。 */
        private final ModelPart tailFlame;
        /** 契约灵狼：颈环。 */
        private final ModelPart collar;
        /** 契约灵狼：颈环上的铃铛（collar 的子件）。 */
        private final ModelPart collarBell;
        /** 契约灵狼：背鞍布。 */
        private final ModelPart saddle;
        /** 灵狼：尾尖一点灵光。 */
        private final ModelPart spiritTailLight;
        /** 熔岩犬：额前熔岩角。 */
        private final ModelPart lavaHorns;
        /** 熔岩犬：背脊 3 簇晶刺。 */
        private final ModelPart spineCrystals;

        public WolfBody(ModelPart root) {
            this.head = root.getChild("head");
            this.body = root.getChild("body");
            this.upperBody = root.getChild("upper_body");
            this.tail = root.getChild("tail");
            this.rightHindLeg = root.getChild("right_hind_leg");
            this.leftHindLeg = root.getChild("left_hind_leg");
            this.rightFrontLeg = root.getChild("right_front_leg");
            this.leftFrontLeg = root.getChild("left_front_leg");

            this.flameMane = root.getChild("flame_mane");
            this.crimsonHorns = this.head.getChild("crimson_horns");
            this.tailFlame = this.tail.getChild("tail_flame");
            this.collar = root.getChild("collar");
            this.collarBell = this.collar.getChild("bell");
            this.saddle = root.getChild("saddle");
            this.spiritTailLight = this.tail.getChild("spirit_tail_light");
            this.lavaHorns = this.head.getChild("lava_horns");
            this.spineCrystals = root.getChild("spine_crystals");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation none = CubeDeformation.NONE;

            // 头 + 两耳 + 长吻：与原版狼同一套 UV
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-3.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, none)
                            .texOffs(16, 14).addBox(-3.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, none)
                            .texOffs(16, 14).addBox(1.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, none)
                            .texOffs(0, 10).addBox(-2.0F, 0.0F, -5.0F, 3.0F, 3.0F, 4.0F, none),
                    PartPose.offset(-1.0F, 13.5F, -7.0F));

            root.addOrReplaceChild("body", CubeListBuilder.create()
                            .texOffs(18, 14).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, none),
                    PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, Mth.PI / 2.0F, 0.0F, 0.0F));

            root.addOrReplaceChild("upper_body", CubeListBuilder.create()
                            .texOffs(21, 0).addBox(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, none),
                    PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, Mth.PI / 2.0F, 0.0F, 0.0F));

            root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                            .texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, none),
                    PartPose.offset(-2.5F, 16.0F, 7.0F));
            root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                            .texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, none),
                    PartPose.offset(0.5F, 16.0F, 7.0F));
            root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                            .texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, none),
                    PartPose.offset(-2.5F, 16.0F, -4.0F));
            root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                            .texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, none),
                    PartPose.offset(0.5F, 16.0F, -4.0F));

            PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                            .texOffs(9, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, none),
                    PartPose.offsetAndRotation(-1.0F, 12.0F, 9.0F, 0.9F, 0.0F, 0.0F));

            // ==================================================================
            // 身份件：4 个狼族变体共用这套骨架，靠下面这些部件 + 贴图区分。
            // UV 全部排在 64×32 贴图的空白区（原像素不动），由实例类型显隐
            // （setupAnim 末尾，instanceof 判断，不看品阶）。
            // ==================================================================
            // 赤炎狼：颈背 3 片火焰鬃毛（前低后高，整组往后倾）
            root.addOrReplaceChild("flame_mane", CubeListBuilder.create()
                            .texOffs(57, 0).addBox(-1.0F, -4.0F, -2.0F, 2.0F, 4.0F, 1.0F, none)
                            .texOffs(51, 0).addBox(-1.0F, -5.0F, 2.0F, 2.0F, 5.0F, 1.0F, none)
                            .texOffs(57, 5).addBox(-1.0F, -4.0F, 6.0F, 2.0F, 4.0F, 1.0F, none),
                    PartPose.offsetAndRotation(0.0F, 11.0F, -3.0F, -0.18F, 0.0F, 0.0F));
            // 赤炎狼：额前双角（从头顶往上挑）
            head.addOrReplaceChild("crimson_horns", CubeListBuilder.create()
                            .texOffs(59, 10).addBox(-3.0F, -8.0F, -2.5F, 1.0F, 4.0F, 1.0F, none)
                            .texOffs(58, 15).addBox(0.5F, -8.0F, -2.5F, 1.0F, 4.0F, 1.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // 赤炎狼：尾焰 2 片（挂在尾尖上，跟着尾巴摆）
            tail.addOrReplaceChild("tail_flame", CubeListBuilder.create()
                            .texOffs(51, 6).addBox(-1.0F, 6.0F, -1.5F, 2.0F, 3.0F, 1.0F, none)
                            .texOffs(14, 10).addBox(-1.0F, 7.0F, 0.5F, 2.0F, 2.0F, 1.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // 契约灵狼：颈环（含铃铛）+ 背鞍布
            PartDefinition collar = root.addOrReplaceChild("collar", CubeListBuilder.create()
                            .texOffs(42, 20).addBox(-3.0F, 10.0F, -6.0F, 6.0F, 1.0F, 4.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            collar.addOrReplaceChild("bell", CubeListBuilder.create()
                            .texOffs(42, 18).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, none),
                    PartPose.offset(0.5F, 11.5F, -4.5F));
            root.addOrReplaceChild("saddle", CubeListBuilder.create()
                            .texOffs(42, 25).addBox(-2.5F, 10.0F, 0.0F, 5.0F, 1.0F, 5.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // 灵狼：尾尖一点灵光
            tail.addOrReplaceChild("spirit_tail_light", CubeListBuilder.create()
                            .texOffs(0, 28).addBox(-1.0F, 7.0F, 0.5F, 2.0F, 2.0F, 2.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // 熔岩犬：额前熔岩角 + 背脊 3 簇晶刺（黑曜+熔岩发光）
            head.addOrReplaceChild("lava_horns", CubeListBuilder.create()
                            .texOffs(8, 28).addBox(-3.0F, -8.0F, -2.5F, 1.0F, 3.0F, 1.0F, none)
                            .texOffs(12, 28).addBox(0.5F, -8.0F, -2.5F, 1.0F, 3.0F, 1.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("spine_crystals", CubeListBuilder.create()
                            .texOffs(42, 13).addBox(-1.0F, 8.0F, -4.0F, 2.0F, 3.0F, 2.0F, none)
                            .texOffs(50, 15).addBox(-1.0F, 8.0F, 1.0F, 2.0F, 3.0F, 2.0F, none)
                            .texOffs(51, 10).addBox(-1.0F, 8.0F, 6.0F, 2.0F, 3.0F, 2.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            return LayerDefinition.create(mesh, 64, 32);
        }

        /**
         * 绘制。
         *
         * <p>父类 {@code EntityModel} 是抽象类，唯一的抽象方法就是这个；
         * 这里等价于原版 {@code AgeableListModel#renderToBuffer} 的那段
         * "先画头、再画身子"，只是不再带 {@code young} 缩放参数
         * （幼年缩放由 {@code SpiritBeastEntity#getScale()} 统一处理，
         * 灵狼本身不参与成长阶段，也就没有 young 这回事）。</p>
         */
        @Override
        public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                   com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                   int packedLight, int packedOverlay, float red,
                                   float green, float blue, float alpha) {
            this.head.render(poseStack, buffer, packedLight, packedOverlay, red, green,
                    blue, alpha);
            this.body.render(poseStack, buffer, packedLight, packedOverlay, red, green,
                    blue, alpha);
            this.upperBody.render(poseStack, buffer, packedLight, packedOverlay, red, green,
                    blue, alpha);
            this.tail.render(poseStack, buffer, packedLight, packedOverlay, red, green,
                    blue, alpha);
            this.rightHindLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green,
                    blue, alpha);
            this.leftHindLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green,
                    blue, alpha);
            this.rightFrontLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green,
                    blue, alpha);
            this.leftFrontLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green,
                    blue, alpha);
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F);
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
            this.tail.xRot = 0.9F + Mth.cos(ageInTicks * 0.09F) * 0.12F;
            this.tail.yRot = Mth.sin(ageInTicks * 0.09F) * 0.12F;
            this.rightHindLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leftHindLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
            this.rightFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
            this.leftFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;

            // ==================================================================
            // 身份件：按实体类型显隐（instanceof，不看品阶）。
            // 这套骨架被 4 只狼共用：灵狼 / 赤炎狼 / 契约灵狼 / 熔岩犬。
            // ==================================================================
            boolean crimson = entity
                    instanceof com.hongmeng.xiuxian.entity.CrimsonFlameWolfEntity;
            boolean contract = entity
                    instanceof com.hongmeng.xiuxian.entity.beast.ContractWolfEntity;
            boolean spirit = entity
                    instanceof com.hongmeng.xiuxian.entity.SpiritWolfEntity && !crimson;
            boolean lava = entity instanceof com.hongmeng.xiuxian.entity.LavaHoundEntity;

            this.flameMane.visible = crimson;
            this.crimsonHorns.visible = crimson;
            this.tailFlame.visible = crimson;
            this.collar.visible = contract;
            this.saddle.visible = contract;
            this.spiritTailLight.visible = spirit;
            this.lavaHorns.visible = lava;
            this.spineCrystals.visible = lava;

            // ---- 身份件各自动起来（全部绝对赋值，没有 += / *=）----
            // 火焰鬃毛/尾焰：两段不同频率的抖动，看起来像在烧
            float flicker = Mth.sin(ageInTicks * 0.35F) * 0.10F
                    + Mth.sin(ageInTicks * 0.77F) * 0.04F;
            this.flameMane.xRot = -0.18F + flicker;
            this.flameMane.zRot = flicker * 0.5F;
            this.tailFlame.xRot = flicker * 1.6F;
            this.crimsonHorns.xRot = flicker * 0.25F;
            // 契约项圈的铃铛：走路时晃（前后 + 侧摆）
            float bellSwing = Mth.cos(limbSwing * 0.6662F) * 0.45F * limbSwingAmount;
            this.collarBell.xRot = bellSwing;
            this.collarBell.zRot = bellSwing * 0.6F;
            // 灵狼尾尖灵光：呼吸（用缩放，不改位置）
            float glowPulse = 0.85F + Mth.sin(ageInTicks * 0.12F) * 0.15F;
            this.spiritTailLight.xScale = glowPulse;
            this.spiritTailLight.yScale = glowPulse;
            this.spiritTailLight.zScale = glowPulse;
            // 熔岩晶刺：缓慢明灭
            float crystalPulse = 0.92F + Mth.sin(ageInTicks * 0.08F) * 0.08F;
            this.spineCrystals.xScale = crystalPulse;
            this.spineCrystals.yScale = crystalPulse;
            this.spineCrystals.zScale = crystalPulse;
        }
    }
}
