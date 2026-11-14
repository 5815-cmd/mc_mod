package com.hongmeng.xiuxian.client;

import com.google.common.collect.ImmutableList;
import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.ThunderEagleEntity;
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
 * 雷鹰的渲染器 + 自建模型。
 *
 * <p>剪影要点：<b>横向的纺锤形身体 + 两对长翼 + 一片长尾羽</b>。
 * 灵狼和烈焰猿都是"竖着"的，雷鹰是唯一横向展开的怪——一眼就能分出天上那只。</p>
 *
 * <p>贴图 {@code 128×128}（14 个盒子，UV 见 {@code tools/gen_beast_textures.py}）。</p>
 *
 * <h3>为什么要泛型</h3>
 *
 * <p>第三批妖兽里的<b>雷纹鹰</b>（{@code StormEagleEntity}）是一只不会飞的鹰，
 * 它要复用同一套骨架、同一张贴图 UV，但<b>不能</b>继承 {@code ThunderEagleEntity}
 * （那个类装了飞行控制与每 tick 蹬地起飞，关掉它比另写一个类更容易出错）。
 * 于是模型做成 {@code EagleModel<T>}、<b>"现在算不算在飞"由构造器注入</b>：
 * 雷鹰传自己的 {@code airborne()}，雷纹鹰传"永远 false"，模型本身一行都不用改。</p>
 *
 * <p>类型上界是 {@code CultivationMonsterEntity} 而不是 {@code Mob}：
 * 品阶发光层 {@link RankGlowLayer} 要的就是这个上界，写 {@code Mob} 反而要到处补
 * {@code instanceof}（第一版就是这么写的，被 javac 拦下了——见文件末尾的踩坑记录）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ThunderEagleRenderer<T extends com.hongmeng.xiuxian.entity.CultivationMonsterEntity>
        extends MobRenderer<T, ThunderEagleRenderer.EagleModel<T>> {
    /** 地面归一化：把模型顶到地面。**必须在 setupRotations 里**（放 scale 里会与动画叠加）。 */
    @Override
    protected void setupRotations(T entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
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
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "thunder_eagle"), "main");

    private final ResourceLocation texture;

    public ThunderEagleRenderer(EntityRendererProvider.Context context) {
        this(context, "thunder_eagle", entity -> entity
                instanceof com.hongmeng.xiuxian.entity.ThunderEagleEntity eagle && eagle.airborne());
    }

    /**
     * @param textureName 贴图名（不含 {@code .png}）：雷鹰自己传 {@code thunder_eagle}，
     *                    雷纹鹰传 {@code storm_eagle}
     */
    public ThunderEagleRenderer(EntityRendererProvider.Context context, String textureName) {
        this(context, textureName, entity -> entity
                instanceof com.hongmeng.xiuxian.entity.ThunderEagleEntity eagle && eagle.airborne());
    }

    /**
     * 完整构造：连"在不在飞"都由调用方决定。
     *
     * <p>刻意是 {@code public} 而不是私有——{@code this(...)} 只能放在构造器第一行，
     * 私有化之后"两个参数的便捷构造"就没法委托给它了。</p>
     */
    public ThunderEagleRenderer(EntityRendererProvider.Context context, String textureName,
                                java.util.function.Predicate<T> airborne) {
        super(context, new EagleModel<>(context.bakeLayer(LAYER), airborne), 0.6F);
        this.texture = com.hongmeng.xiuxian.entity.CultivationMonsterEntity.textureFor(textureName);
        this.addLayer(new RankGlowLayer<>(this));
    }

    /** 给"另一种鹰"用的工厂：骨架一样，贴图与飞行判定换了。 */
    public static <E extends com.hongmeng.xiuxian.entity.CultivationMonsterEntity>
            ThunderEagleRenderer<E> shared(
            EntityRendererProvider.Context context, String textureName,
            java.util.function.Predicate<E> airborne) {
        return new ThunderEagleRenderer<>(context, textureName, airborne);
    }
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }

    @Override
    protected boolean shouldShowName(T entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /** 品阶体型（倍率表在 MobRank，本类不写死数字）。 */
    @Override
    protected void scale(T entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTick) {
        var rank = entity.rank();
        poseStack.scale(rank.scaleXZ(), rank.scaleY(), rank.scaleXZ());
    }

    /**
     * 鹰形模型。
     *
     * <p>骨架（脚底在 y=24，模型 +y 向下）：爪 24→21、腿 21→15、身体 15→5
     * （10 高、16 长的横置纺锤）、颈 5→-1、头 -1→-7、喙再往前伸 6 格；
     * 两对翅膀挂在身体两侧 y=3..5，展开总跨 26 格。</p>
     */
    public static class EagleModel<T extends com.hongmeng.xiuxian.entity.CultivationMonsterEntity>
            extends ColorableAgeableListModel<T> {

        private final ModelPart body;
        private final ModelPart neck;
        private final ModelPart head;
        private final ModelPart beak;
        private final ModelPart crest;
        private final ModelPart tail;
        private final ModelPart rightWingTip;
        private final ModelPart leftWingTip;
        private final ModelPart rightWing;
        private final ModelPart leftWing;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;
        private final ModelPart rightTalon;
        private final ModelPart leftTalon;

        // ---- 身份件（同一套骨架，按实体类型显隐；见 setupAnim 末尾）----
        /** 雷纹鹰：头顶 3 根雷羽冠（整体后倾）。 */
        private final ModelPart stormCrest;
        /** 雷纹鹰：分叉尾羽。 */
        private final ModelPart tailFork;
        /** 雷鹰：左右翅尖的雷光片（飞行时闪）。 */
        private final ModelPart wingTipLightR;
        private final ModelPart wingTipLightL;

        /**
         * "这只鹰现在算不算在飞"——由渲染器注入。
         *
         * <p>不写死查 {@code ThunderEagleEntity.airborne()}：雷纹鹰是另一个类，
         * 它永远返回 false（地上跑）。把这一句做成参数，模型就不必知道有几种鹰。</p>
         */
        private final java.util.function.Predicate<T> airborne;

        public EagleModel(ModelPart root, java.util.function.Predicate<T> airborne) {
            this.airborne = airborne;
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

            this.stormCrest = this.head.getChild("storm_crest");
            this.tailFork = this.tail.getChild("tail_fork");
            this.wingTipLightR = this.rightWingTip.getChild("wing_tip_light_r");
            this.wingTipLightL = this.leftWingTip.getChild("wing_tip_light_l");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation none = CubeDeformation.NONE;

            root.addOrReplaceChild("body", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-5.0F, 0.0F, -8.0F, 10.0F, 10.0F, 16.0F, none),
                    PartPose.offset(0.0F, 5.0F, 0.0F));

            PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create()
                            .texOffs(77, 78).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, none),
                    PartPose.offset(0.0F, -1.0F, -7.0F));
            PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(33, 78).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 6.0F, 6.0F, none),
                    PartPose.offset(0.0F, -6.0F, 0.0F));
            head.addOrReplaceChild("beak", CubeListBuilder.create()
                            .texOffs(58, 78).addBox(-1.5F, 0.0F, -6.0F, 3.0F, 3.0F, 6.0F, none),
                    PartPose.offset(0.0F, 2.0F, -3.0F));
            head.addOrReplaceChild("crest", CubeListBuilder.create()
                            .texOffs(45, 105).addBox(-3.0F, -2.0F, -2.0F, 6.0F, 2.0F, 4.0F, none),
                    PartPose.offset(0.0F, 0.0F, 1.0F));

            root.addOrReplaceChild("tail", CubeListBuilder.create()
                            .texOffs(41, 43).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 2.0F, 14.0F, none),
                    PartPose.offset(0.0F, 9.0F, 8.0F));
            PartDefinition tail = root.getChild("tail");

            // ---- 翅膀：内翼 12 长 + 外翼 16 长，展开跨 26 格 ----
            PartDefinition rightWing = root.addOrReplaceChild("right_wing", CubeListBuilder.create()
                            .texOffs(0, 78).addBox(-4.0F, -1.0F, -6.0F, 4.0F, 2.0F, 12.0F, none),
                    PartPose.offset(-5.0F, 4.0F, -2.0F));
            PartDefinition rightWingTip = rightWing.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
                            .texOffs(0, 43).addBox(-4.0F, -1.0F, -6.0F, 4.0F, 2.0F, 16.0F, none),
                    PartPose.offset(-4.0F, 0.0F, 0.0F));

            PartDefinition leftWing = root.addOrReplaceChild("left_wing", CubeListBuilder.create()
                            .texOffs(86, 43).addBox(0.0F, -1.0F, -6.0F, 4.0F, 2.0F, 12.0F, none),
                    PartPose.offset(5.0F, 4.0F, -2.0F));
            PartDefinition leftWingTip = leftWing.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
                            .texOffs(53, 0).addBox(0.0F, -1.0F, -6.0F, 4.0F, 2.0F, 16.0F, none),
                    PartPose.offset(4.0F, 0.0F, 0.0F));

            // ---- 腿与爪 ----
            PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                            .texOffs(32, 105).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, none),
                    PartPose.offset(-2.5F, 15.0F, 0.0F));
            rightLeg.addOrReplaceChild("right_talon", CubeListBuilder.create()
                            .texOffs(0, 105).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 3.0F, 5.0F, none),
                    PartPose.offset(0.0F, 6.0F, -1.0F));

            PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                            .texOffs(19, 105).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, none),
                    PartPose.offset(2.5F, 15.0F, 0.0F));
            leftLeg.addOrReplaceChild("left_talon", CubeListBuilder.create()
                            .texOffs(94, 78).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 3.0F, 5.0F, none),
                    PartPose.offset(0.0F, 6.0F, -1.0F));

            // ==================================================================
            // 身份件：鹰族 3 个变体共用这套骨架。UV 全排在 128×128 贴图空白区，
            // 显隐按实体类型（setupAnim 末尾，instanceof，不看品阶）。
            // ==================================================================
            // 雷纹鹰：头顶 3 根雷羽冠（同一组绕 X 后倾，看起来像被风往后吹）
            head.addOrReplaceChild("storm_crest", CubeListBuilder.create()
                            .texOffs(93, 0).addBox(-3.0F, -4.0F, -1.0F, 2.0F, 4.0F, 1.0F, none)
                            .texOffs(99, 0).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 4.0F, 1.0F, none)
                            .texOffs(105, 0).addBox(1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 1.0F, none),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.45F, 0.0F, 0.0F));
            // 雷纹鹰：尾羽分叉（尾端两撇）
            tail.addOrReplaceChild("tail_fork", CubeListBuilder.create()
                            .texOffs(93, 5).addBox(1.0F, 0.0F, 14.0F, 2.0F, 1.0F, 3.0F, none)
                            .texOffs(103, 5).addBox(-3.0F, 0.0F, 14.0F, 2.0F, 1.0F, 3.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // 雷鹰：左右翅尖各一片雷光（飞行时闪）
            rightWingTip.addOrReplaceChild("wing_tip_light_r", CubeListBuilder.create()
                            .texOffs(111, 0).addBox(-1.0F, -1.5F, -1.0F, 1.0F, 3.0F, 2.0F, none),
                    PartPose.offset(-4.0F, 0.0F, 0.0F));
            leftWingTip.addOrReplaceChild("wing_tip_light_l", CubeListBuilder.create()
                            .texOffs(117, 0).addBox(0.0F, -1.5F, -1.0F, 1.0F, 3.0F, 2.0F, none),
                    PartPose.offset(4.0F, 0.0F, 0.0F));

            return LayerDefinition.create(mesh, 128, 128);
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
         * 扇翅。
         *
         * <p>飞的时候：翅膀以 z 轴为轴上下扇（左翼取反，免得两边同向翻），
         * 身体顺着视线俯仰——低头就是俯冲的姿态。</p>
         *
         * <p>落地时：翅膀收在身侧（z 轴压到 -1.35 左右），腿做小幅踱步，
         * 头左右张望——不会出现"站在地上还在扇翅膀"的穿帮。</p>
         */
        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            boolean flying = this.airborne.test(entity);

            this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.6F;
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.6F;
            float pitch = entity.getXRot() * ((float) Math.PI / 180.0F) * 0.7F;
            // 颈：抬头/低头 + 飞行时的俯仰。**一次算完、绝对赋值**——
            // 写成 `this.neck.xRot += pitch * 0.5F` 会被误读成跨帧累加
            // （本模型没有 resetPose，真累加的话头会一路飘走）。
            this.neck.xRot = -0.25F + this.head.xRot * 0.4F
                    + (flying ? pitch * 0.5F : 0.0F);

            if (flying) {
                // 扇翅频率随速度走：俯冲时几乎不动，盘旋时扇得欢
                float speed = Mth.clamp(limbSwingAmount, 0.25F, 1.0F);
                float flap = Mth.cos(ageInTicks * (0.55F + speed * 0.35F)) * (0.55F * speed + 0.15F);
                this.rightWing.zRot = flap;
                this.leftWing.zRot = -flap;
                this.rightWingTip.zRot = flap * 0.7F;
                this.leftWingTip.zRot = -flap * 0.7F;
                this.rightWing.xRot = -0.12F;
                this.leftWing.xRot = -0.12F;

                // 身体沿视线俯仰（正的 xRot 是低头）
                this.body.xRot = pitch;
                this.tail.xRot = -pitch * 0.6F + Mth.cos(ageInTicks * 0.1F) * 0.05F;
                this.tail.yRot = Mth.sin(ageInTicks * 0.08F) * 0.12F;

                // 腿收起来
                this.rightLeg.xRot = 0.9F;
                this.leftLeg.xRot = 0.9F;
                this.rightTalon.xRot = 0.5F;
                this.leftTalon.xRot = 0.5F;
            } else {
                float step = Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount;
                this.rightLeg.xRot = step;
                this.leftLeg.xRot = -step;
                // 收翅
                this.rightWing.zRot = -1.35F;
                this.leftWing.zRot = 1.35F;
                this.rightWingTip.zRot = -0.15F;
                this.leftWingTip.zRot = 0.15F;
                this.rightWing.xRot = 0.0F;
                this.leftWing.xRot = 0.0F;
                this.body.xRot = 0.0F;
                this.tail.xRot = 0.15F;
            }

            // 冠羽与喙：一点点小动作，别像标本
            this.crest.xRot = -0.2F + Mth.sin(ageInTicks * 0.13F) * 0.1F;
            this.beak.xRot = Mth.sin(ageInTicks * 0.1F) * 0.04F;

            // ==================================================================
            // 身份件：按实体类型显隐（instanceof，不看品阶）
            // ==================================================================
            boolean storm = entity instanceof com.hongmeng.xiuxian.entity.StormEagleEntity;
            boolean thunder = entity instanceof com.hongmeng.xiuxian.entity.ThunderEagleEntity;
            this.stormCrest.visible = storm;
            this.tailFork.visible = storm;
            this.wingTipLightR.visible = thunder;
            this.wingTipLightL.visible = thunder;

            // 雷羽冠：带电似的轻颤（绝对赋值；基准角 -0.45 是造型的后倾角）
            float spark = Mth.sin(ageInTicks * 0.42F) * 0.10F
                    + Mth.sin(ageInTicks * 0.13F) * 0.04F;
            this.stormCrest.xRot = -0.45F + spark;
            this.stormCrest.zRot = spark * 0.5F;
            // 分叉尾羽：飞行时后掠，落地时下垂微摆
            this.tailFork.xRot = flying ? -0.22F
                    : 0.12F + Mth.sin(ageInTicks * 0.18F) * 0.06F;
            // 翅尖雷光：飞行时一下一下地闪（用缩放做"明灭"，绝对赋值）
            float glimmer = flying ? 0.85F + Mth.sin(ageInTicks * 0.9F) * 0.15F : 0.92F;
            this.wingTipLightR.xScale = glimmer;
            this.wingTipLightR.yScale = glimmer;
            this.wingTipLightR.zScale = glimmer;
            this.wingTipLightL.xScale = glimmer;
            this.wingTipLightL.yScale = glimmer;
            this.wingTipLightL.zScale = glimmer;
        }
    }
}
