package com.hongmeng.xiuxian.client;

import com.google.common.collect.ImmutableList;
import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.beast.ContractEagleEntity;
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
 * 契约雷鹰的渲染器 + 自建模型。
 *
 * <p>它必须是"可以被骑着飞"的鸟：所以模型比野生的 {@code thunder_eagle}
 * 小一圈（野生雷鹰翼展 26 格，这里 20 格），背脊留出一段平的鞍位，
 * 鞍位高度与 {@code ContractEagleEntity.riderYOffset()} 对齐
 * （目标：乘客脚底落在 y≈16，模型空间的背上）。</p>
 *
 * <p>贴图 {@code 64×64}，13 个盒子，UV 见 {@code tools/gen_beast_textures.py}
 * 的 {@code EAGLE_LAYOUT}。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ContractEagleRenderer
        extends MobRenderer<ContractEagleEntity, ContractEagleRenderer.EagleModel> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(ContractEagleEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
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
    private static final double GROUND_LIFT = 0.00D;

    /**
     * 灵兽成长体型：幼年 ×0.45 / 成年 ×1.0 / 化形 ×1.6（{@code SpiritBeastEntity#getScale()}）。
     *
     * <p>灵兽没有品阶，这里只乘成长倍率一次（不叠 {@code MobRank.scale*}）。
     * {@code scale()} 在 {@code translate(0, -1.501, 0)} 之前执行，缩放以脚底为锚。</p>
     */
    @Override
    protected void scale(ContractEagleEntity entity,
                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        super.scale(entity, poseStack, partialTick);
        float s = entity.getScale();
        poseStack.scale(s, s, s);
    }


    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "contract_eagle"),
                    "main");

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/contract_eagle.png");

    public ContractEagleRenderer(EntityRendererProvider.Context context) {
        super(context, new EagleModel(context.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ContractEagleEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(ContractEagleEntity entity) {
        return super.shouldShowName(entity) || entity.isContracted();
    }

    /**
     * 契约雷鹰模型。
     *
     * <pre>
     *   爪 24→21   腿 24→15（9 高）
     *   身体 15→5（10 高、12 长）  颈 3→9   头 -3→3   喙/冠挂在头上
     *   尾羽挂身体后缘（15→3，垂到爪下）
     *   双翼从身体上缘展开：内翼 5→13、外翼再 8 格，总翼展 20 格
     * </pre>
     */
    public static class EagleModel extends ColorableAgeableListModel<ContractEagleEntity> {

        private final ModelPart body;
        private final ModelPart neck;
        private final ModelPart head;
        private final ModelPart beak;
        private final ModelPart crest;
        private final ModelPart tail;
        private final ModelPart rightWing;
        private final ModelPart leftWing;
        private final ModelPart rightWingTip;
        private final ModelPart leftWingTip;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;
        private final ModelPart rightTalon;
        private final ModelPart leftTalon;

        private final ModelPart saddle;
        private final ModelPart contractBell;

        public EagleModel(ModelPart root) {
            this.body = root.getChild("body");
            this.neck = root.getChild("neck");
            this.head = this.neck.getChild("head");
            this.beak = this.head.getChild("beak");
            this.crest = this.head.getChild("crest");
            this.tail = root.getChild("tail");
            this.rightWing = root.getChild("right_wing");
            this.leftWing = root.getChild("left_wing");
            this.rightWingTip = this.rightWing.getChild("right_wing_tip");
            this.leftWingTip = this.leftWing.getChild("left_wing_tip");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
            this.rightTalon = this.rightLeg.getChild("right_talon");
            this.leftTalon = this.leftLeg.getChild("left_talon");

            this.saddle = this.body.getChild("saddle");
            this.contractBell = this.saddle.getChild("bell");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation none = CubeDeformation.NONE;

            // UV 坐标由 tools/gen_beast_mount_textures.py 的排版器算出
            // （`--uv` 可重新打印）。左右翼/腿用**同一组 UV**，
            // 靠 PartPose 的 zRot 反向来实现对称——这是原版的做法。
            //
            // ---- 身体：8 宽 10 高 12 长，背脊就是鞍位 ----
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-4.0F, 0.0F, -6.0F, 8.0F, 10.0F, 12.0F, none),
                    PartPose.offset(0.0F, 5.0F, 0.0F));

            PartDefinition saddle = body.addOrReplaceChild("saddle", CubeListBuilder.create()
                            .texOffs(100, 10).addBox(-3.0F, -1.0F, -2.5F, 6.0F, 1.0F, 5.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            saddle.addOrReplaceChild("bell_rope", CubeListBuilder.create()
                            .texOffs(122, 0).addBox(-0.5F, 0.0F, -3.5F, 1.0F, 2.0F, 1.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            saddle.addOrReplaceChild("bell", CubeListBuilder.create()
                            .texOffs(122, 3).addBox(-0.5F, 2.0F, -4.0F, 1.0F, 1.0F, 1.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));

            // ---- 颈 → 头 → 喙/冠 ----
            PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create()
                            .texOffs(40, 34).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, none),
                    PartPose.offset(0.0F, 3.0F, -5.0F));
            PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(76, 0).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 6.0F, 6.0F, none),
                    PartPose.offset(0.0F, -6.0F, 0.0F));
            head.addOrReplaceChild("beak", CubeListBuilder.create()
                            .texOffs(22, 34).addBox(-1.5F, 0.0F, -6.0F, 3.0F, 3.0F, 6.0F, none),
                    PartPose.offset(0.0F, 2.0F, -3.0F));
            head.addOrReplaceChild("crest", CubeListBuilder.create()
                            .texOffs(74, 34).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 2.0F, 5.0F, none),
                    PartPose.offset(0.0F, 0.0F, 1.0F));

            // ---- 尾羽 ----
            root.addOrReplaceChild("tail", CubeListBuilder.create()
                            .texOffs(40, 0).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 2.0F, 12.0F, none),
                    PartPose.offsetAndRotation(0.0F, 12.0F, 5.0F, 0.35F, 0.0F, 0.0F));

            // ---- 双翼：内翼 8 长 + 外翼 8 长，展开总跨 20 格 ----
            PartDefinition rightWing = root.addOrReplaceChild("right_wing", CubeListBuilder.create()
                            .texOffs(100, 0).addBox(-3.0F, -1.0F, -4.0F, 3.0F, 2.0F, 8.0F, none),
                    PartPose.offset(-4.0F, 6.0F, -1.0F));
            rightWing.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
                            .texOffs(0, 34).addBox(-3.0F, -1.0F, -4.0F, 3.0F, 2.0F, 8.0F, none),
                    PartPose.offset(-3.0F, 0.0F, 0.0F));

            PartDefinition leftWing = root.addOrReplaceChild("left_wing", CubeListBuilder.create()
                            .texOffs(100, 0).addBox(0.0F, -1.0F, -4.0F, 3.0F, 2.0F, 8.0F, none),
                    PartPose.offset(4.0F, 6.0F, -1.0F));
            leftWing.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
                            .texOffs(0, 34).addBox(0.0F, -1.0F, -4.0F, 3.0F, 2.0F, 8.0F, none),
                    PartPose.offset(3.0F, 0.0F, 0.0F));

            // ---- 腿与爪：腿从 y=15（身体底）长到 y=24（地面） ----
            PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                            .texOffs(92, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, none),
                    PartPose.offset(-2.0F, 15.0F, 0.0F));
            rightLeg.addOrReplaceChild("right_talon", CubeListBuilder.create()
                            .texOffs(56, 34).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 3.0F, 5.0F, none),
                    PartPose.offset(0.0F, 6.0F, -1.0F));

            PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                            .texOffs(92, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, none),
                    PartPose.offset(2.0F, 15.0F, 0.0F));
            leftLeg.addOrReplaceChild("left_talon", CubeListBuilder.create()
                            .texOffs(56, 34).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 3.0F, 5.0F, none),
                    PartPose.offset(0.0F, 6.0F, -1.0F));

            // 贴图 128×64（10 个盒子在 64×64 里排不下）
            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        protected Iterable<ModelPart> headParts() {
            return ImmutableList.of(this.neck);
        }

        @Override
        protected Iterable<ModelPart> bodyParts() {
            return ImmutableList.of(this.body, this.tail, this.rightWing, this.leftWing,
                    this.rightLeg, this.leftLeg);
        }

        /**
         * 扇翅 / 悬停 / 落地收翅三态。
         *
         * <p>悬停（{@code airborne 且几乎没在移动}）时翅膀扇得更快更浅，
         * 这是"悬停"这个职能唯一的视觉标志——玩家得能一眼看出它在停着。
         * 这与野生的雷鹰不同：后者只会俯冲与盘旋。</p>
         */
        @Override
        public void setupAnim(ContractEagleEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            boolean flying = entity.airborne();
            float speed = Mth.clamp(limbSwingAmount, 0.15F, 1.0F);
            boolean hovering = flying && limbSwingAmount < 0.25F;

            this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.55F;
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.55F;
            float pitch = entity.getXRot() * ((float) Math.PI / 180.0F) * 0.6F;
            // 颈：抬头/低头 + 飞行时的俯仰。**一次算完、绝对赋值**——
            // 写成 `this.neck.xRot += pitch * 0.5F` 会被误读成跨帧累加
            // （本模型没有 resetPose，真累加的话头会一路飘走）。
            this.neck.xRot = -0.28F + this.head.xRot * 0.4F
                    + (flying ? pitch * 0.5F : 0.0F);

            if (flying) {
                float rate = hovering ? 0.95F : (0.5F + speed * 0.4F);
                float amp = hovering ? 0.42F : (0.5F * speed + 0.18F);
                float flap = Mth.cos(ageInTicks * rate) * amp;
                this.rightWing.zRot = flap;
                this.leftWing.zRot = -flap;
                this.rightWingTip.zRot = flap * 0.65F;
                this.leftWingTip.zRot = -flap * 0.65F;
                // 悬停时翅膀更平，滑翔时略前掠
                this.rightWing.xRot = hovering ? -0.02F : -0.14F;
                this.leftWing.xRot = hovering ? -0.02F : -0.14F;

                this.body.xRot = pitch;
                this.tail.xRot = 0.35F - pitch * 0.5F
                        + Mth.cos(ageInTicks * 0.12F) * 0.06F;

                // 悬停时腿收得更紧
                this.rightLeg.xRot = hovering ? 1.25F : 0.95F;
                this.leftLeg.xRot = hovering ? 1.25F : 0.95F;
                this.rightTalon.xRot = 0.5F;
                this.leftTalon.xRot = 0.5F;
            } else {
                float step = Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount;
                this.rightLeg.xRot = step;
                this.leftLeg.xRot = -step;
                this.rightTalon.xRot = 0.0F;
                this.leftTalon.xRot = 0.0F;
                this.rightWing.zRot = -1.30F;
                this.leftWing.zRot = 1.30F;
                this.rightWingTip.zRot = -0.18F;
                this.leftWingTip.zRot = 0.18F;
                this.rightWing.xRot = 0.0F;
                this.leftWing.xRot = 0.0F;
                this.body.xRot = 0.0F;
                this.tail.xRot = 0.2F;
            }

            this.crest.xRot = -0.22F + Mth.sin(ageInTicks * 0.15F) * 0.1F;
            this.beak.xRot = Mth.sin(ageInTicks * 0.11F) * 0.05F;

            float bellSwing = flying
                    ? 0.35F + Mth.sin(ageInTicks * 0.4F) * 0.18F
                    : Mth.sin(ageInTicks * 0.1F) * 0.08F;
            this.contractBell.xRot = bellSwing;
            this.contractBell.zRot = bellSwing * 0.5F;
        }
    }
}
