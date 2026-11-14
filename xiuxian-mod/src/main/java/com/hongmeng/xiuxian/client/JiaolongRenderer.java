package com.hongmeng.xiuxian.client;

import com.google.common.collect.ImmutableList;
import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.beast.JiaolongEntity;
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
 * 蛟龙的渲染器 + 自建模型（<b>由 {@code VoidSerpentRenderer} 的八节蛇身改来</b>）。
 *
 * <h3>改了什么、为什么</h3>
 *
 * <p>规格明确要求"蛟龙（a segmented serpentine body already exists at
 * {@code client/VoidSerpentRenderer.java} — READ IT and adapt rather than
 * starting over）"。所以骨链的思路、行波公式、共用 UV 的做法全部保留，
 * 但做了四处<b>必要的</b>改动：</p>
 *
 * <ol>
 *   <li><b>节数 8 → {@value #SEGMENTS}</b>，每节 6 格 → <b>7 格</b>，
 *       整条长度从 4.3 格拉到 <b>7.6 格</b>；蛟龙是能骑的，太短会显得骑在一条虫上。</li>
 *   <li><b>贴图 128×128 → 256×256</b>：节数变多 + 每节变大 + 加了须与背鳍，
 *       原来的 UV 已经排不开（{@code gen_beast_textures.py} 里有排版自查）。</li>
 *   <li><b>背鳍的收集方式修好了</b>。{@code VoidSerpentRenderer} 用
 *       {@code i % 2 == 1 && i / 2 < 3} 索引 {@code fins[i / 2]}，
 *       但 {@code i / 2} 只会取到 0/1/2 中的 1 与 2 —— 结果是<b>两片鳍没被收集</b>，
 *       它们不会随身体摆动（浮点判断，视觉上不明显但确实是 bug）。
 *       这里改成顺序计数，几片鳍都拿得到。</li>
 *   <li><b>加了龙须与背鳍</b>：蛟龙是"龙"，不是"蛇"。</li>
 * </ol>
 *
 * <p>贴图 UV 布局见 {@code tools/gen_beast_textures.py} 的
 * {@code JIAOLONG_LAYOUT}（与 {@link #createBodyLayer()} 逐字对应）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class JiaolongRenderer
        extends MobRenderer<JiaolongEntity, JiaolongRenderer.JiaolongModel> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(JiaolongEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
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
     */
    private static void groundAlign(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }

    /** 离地修正量（格）：正数=上移，负数=下移。= (模型最低点像素 y - 24) / 16。 */
    private static final double GROUND_LIFT = -0.44D;

    /**
     * 灵兽成长体型：幼年 ×0.45 / 成年 ×1.0 / 化形 ×1.6（{@code SpiritBeastEntity#getScale()}）。
     *
     * <p>蛟龙没有品阶，这里只乘成长倍率一次（不叠 {@code MobRank.scale*}）。
     * 缩放以模型原点为锚，脚底基准（{@code GROUND_LIFT = -0.44}）在缩放之后才做平移，
     * 所以化形 ×1.6 时蛇身仍然是"贴着地面长"，不会浮起来。</p>
     */
    @Override
    protected void scale(JiaolongEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        super.scale(entity, poseStack, partialTick);
        float s = entity.getScale();
        poseStack.scale(s, s, s);
    }


    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "jiaolong"), "main");

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/jiaolong.png");

    public JiaolongRenderer(EntityRendererProvider.Context context) {
        super(context, new JiaolongModel(context.bakeLayer(LAYER)), 1.1F);
    }

    @Override
    public ResourceLocation getTextureLocation(JiaolongEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(JiaolongEntity entity) {
        return super.shouldShowName(entity) || entity.isContracted();
    }

    /** 九节蛟身：头 + 下颚 + 双角 + 双须 + 九节身体（其中四节带背鳍）+ 尾鳍。 */
    public static class JiaolongModel extends ColorableAgeableListModel<JiaolongEntity> {

        /** 身体节数；改这里必须同步改贴图排版与注释。 */
        public static final int SEGMENTS = 9;

        /** 带背鳍的节序号（0 基）。4 片，均匀分布。 */
        private static final int[] FIN_AT = {1, 3, 5, 7};

        /**
         * 四片背鳍共用同一个 {@code texOffs}（166, 0）。
         *
         * <p>和"九节身体共用一块 UV"是同一个道理：四片鳍是<b>完全相同的盒子</b>，
         * 让它们共用像素是刻意的省法，贴图生成脚本里也只画一份。
         * {@code tools/gen_beast_mount_textures.py} 的排版器把 {@code fin}
         * 当作<b>单一条目</b>排版，正是为了配合这个设计。</p>
         *
         * <p>全部 UV 坐标都由那个脚本的排版器算出（{@code --uv} 可重新打印），
         * 手写坐标会撞车，而 MC 对 UV 重叠不报错、只把颜色糊在一起。</p>
         */
        private static final int FIN_U = 166;
        private static final int FIN_V = 0;

        private final ModelPart[] segments = new ModelPart[SEGMENTS];
        private final ModelPart[] fins = new ModelPart[FIN_AT.length];

        private final ModelPart head;
        private final ModelPart jaw;
        private final ModelPart rightHorn;
        private final ModelPart leftHorn;
        private final ModelPart rightWhisker;
        private final ModelPart leftWhisker;
        private final ModelPart tailFin;

        public JiaolongModel(ModelPart root) {
            this.head = root.getChild("head");
            this.jaw = this.head.getChild("jaw");
            this.rightHorn = this.head.getChild("right_horn");
            this.leftHorn = this.head.getChild("left_horn");
            this.rightWhisker = this.head.getChild("right_whisker");
            this.leftWhisker = this.head.getChild("left_whisker");

            ModelPart parent = root;
            int finIndex = 0;
            for (int i = 0; i < SEGMENTS; i++) {
                ModelPart segment = parent.getChild("segment_" + i);
                this.segments[i] = segment;
                if (finIndex < FIN_AT.length && FIN_AT[finIndex] == i) {
                    // 顺序取，不做 i/2 那种会漏项的算术
                    this.fins[finIndex] = segment.getChild("fin_" + finIndex);
                    finIndex++;
                }
                parent = segment;
            }
            this.tailFin = parent.getChild("tail_fin");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation none = CubeDeformation.NONE;

            // ---- 头：9 宽 8 高 12 长，前端再挂一个吻 ----
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(44, 0).addBox(-4.5F, 0.0F, -12.0F, 9.0F, 8.0F, 12.0F, none)
                            .texOffs(182, 0).addBox(-3.0F, 1.0F, -16.0F, 6.0F, 5.0F, 4.0F, none),
                    PartPose.offset(0.0F, 6.0F, 0.0F));
            head.addOrReplaceChild("jaw", CubeListBuilder.create()
                            .texOffs(86, 0).addBox(-3.5F, 0.0F, -9.0F, 7.0F, 3.0F, 10.0F, none),
                    PartPose.offset(0.0F, 8.0F, -3.0F));
            head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                            .texOffs(202, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, none),
                    PartPose.offsetAndRotation(-4.0F, 1.0F, -2.0F, 0.0F, 0.0F, 0.35F));
            head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                            .texOffs(202, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, none),
                    PartPose.offsetAndRotation(4.0F, 1.0F, -2.0F, 0.0F, 0.0F, -0.35F));
            head.addOrReplaceChild("right_whisker", CubeListBuilder.create()
                            .texOffs(210, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F, none),
                    PartPose.offsetAndRotation(-3.0F, 7.0F, -12.0F, -0.5F, 0.0F, 0.3F));
            head.addOrReplaceChild("left_whisker", CubeListBuilder.create()
                            .texOffs(210, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F, none),
                    PartPose.offsetAndRotation(3.0F, 7.0F, -12.0F, -0.5F, 0.0F, -0.3F));

            // ---- 九节身体：第一节挂在根上（y=9），其余依次往后 7 格 ----
            PartDefinition parent = root;
            int finIndex = 0;
            for (int i = 0; i < SEGMENTS; i++) {
                PartPose pose = (i == 0)
                        ? PartPose.offset(0.0F, 9.0F, 1.0F)
                        : PartPose.offset(0.0F, 0.0F, 7.0F);
                PartDefinition segment = parent.addOrReplaceChild("segment_" + i,
                        CubeListBuilder.create()
                                .texOffs(138, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 7.0F, 7.0F, none),
                        pose);
                if (finIndex < FIN_AT.length && FIN_AT[finIndex] == i) {
                    int fin = finIndex;
                    segment.addOrReplaceChild("fin_" + fin, CubeListBuilder.create()
                                    .texOffs(FIN_U, FIN_V)
                                    .addBox(-0.5F, -6.0F, 0.0F, 1.0F, 6.0F, 7.0F, none),
                            PartPose.offset(0.0F, 0.0F, 2.0F));
                    finIndex++;
                }
                parent = segment;
            }
            parent.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-4.0F, 1.0F, 0.0F, 8.0F, 6.0F, 14.0F, none)
                            .texOffs(120, 0).addBox(-0.5F, -6.0F, 4.0F, 1.0F, 7.0F, 8.0F, none),
                    PartPose.offset(0.0F, 0.0F, 7.0F));

            // 贴图 256×256：节数变多 + 每节变大 + 多了须与鳍，
            // 原来的 128×128 已经排不开（排版器会直接报错，不会静默出界）
            return LayerDefinition.create(mesh, 256, 256);
        }

        @Override
        protected Iterable<ModelPart> headParts() {
            return ImmutableList.of(this.head);
        }

        @Override
        protected Iterable<ModelPart> bodyParts() {
            return ImmutableList.of(this.segments[0]);
        }

        /**
         * 行波扭动（沿用虚空蛟那一套，振幅按"飞行"放大）。
         *
         * <p>空中时整条身体更"飘"、鳍张得更开；落地时幅度收敛，
         * 看起来像盘在地上。骑乘时（有乘客）再加一点俯仰响应，
         * 让"它真的在带着你飞"这件事看得出来。</p>
         */
        @Override
        public void setupAnim(JiaolongEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            float airborne = entity.airborne() ? 1.0F : 0.55F;
            float drive = (0.12F + Mth.clamp(limbSwingAmount, 0.0F, 1.0F) * 0.5F) * airborne;
            boolean ridden = entity.isBeingRidden();

            this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.35F
                    + Mth.sin(ageInTicks * 0.1F) * 0.04F;
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.45F;
            this.jaw.xRot = 0.12F + Mth.sin(ageInTicks * 0.16F) * 0.09F;

            // 龙须永远在飘：这是"蛟"和"蛇"最大的形态区别
            float whisker = 0.3F + drive * 0.7F;
            this.rightWhisker.xRot = -0.5F + Mth.sin(ageInTicks * 0.13F) * whisker;
            this.leftWhisker.xRot = -0.5F + Mth.sin(ageInTicks * 0.13F + 0.6F) * whisker;
            this.rightWhisker.zRot = 0.3F + Mth.cos(ageInTicks * 0.11F) * whisker * 0.4F;
            this.leftWhisker.zRot = -0.3F - Mth.cos(ageInTicks * 0.11F) * whisker * 0.4F;

            for (int i = 0; i < SEGMENTS; i++) {
                float phase = ageInTicks * (ridden ? 0.16F : 0.18F) - i * 0.58F;
                float yaw = Mth.sin(phase) * drive * (ridden ? 0.30F : 0.42F);
                float pitch = Mth.cos(phase * 0.75F) * drive * 0.10F * (1.0F - i * 0.07F);
                if (ridden) {
                    // 骑乘时压一点俯仰，看起来是"顺着主人的视线"在游
                    pitch += entity.getXRot() * ((float) Math.PI / 180.0F) * 0.10F;
                }
                this.segments[i].yRot = yaw;
                this.segments[i].xRot = pitch;
            }

            for (int i = 0; i < this.fins.length; i++) {
                if (this.fins[i] == null) {
                    continue;
                }
                float base = airborne > 0.9F ? -0.25F : -0.05F;
                this.fins[i].xRot = base + Mth.sin(ageInTicks * 0.18F - (FIN_AT[i]) * 0.62F) * 0.22F;
                this.fins[i].yRot = Mth.sin(ageInTicks * 0.18F - (FIN_AT[i]) * 0.62F) * 0.3F;
            }

            this.tailFin.yRot = Mth.sin(ageInTicks * 0.18F - SEGMENTS * 0.58F) * drive * 0.5F;
            this.tailFin.xRot = Mth.cos(ageInTicks * 0.15F) * 0.08F;
        }
    }
}
