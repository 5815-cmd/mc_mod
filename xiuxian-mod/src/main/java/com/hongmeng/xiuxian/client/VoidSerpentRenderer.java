package com.hongmeng.xiuxian.client;

import com.google.common.collect.ImmutableList;
import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.VoidSerpentEntity;
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
 * 虚空蛟的渲染器 + 自建模型。
 *
 * <p>这是本模组最"不像怪"的一只：<b>八节串起来的蛇身</b>。
 * 每一节都是前一节的子部件，所以整条身体是一根真正的骨链——
 * 扭动的时候是逐节传递的行波，不是把一整块模型整体旋转。</p>
 *
 * <p>长度约 4.3 格（头 12 格 + 八节 × 7 格 + 尾尖 12 格），
 * 命中判定只留身体中段那一小块（见 {@code ModEntities.VOID_SERPENT} 的 sized），
 * 这是长条怪的常规做法：看得见的部分比打得中的部分长。</p>
 *
 * <p>贴图 {@code 128×128}；八节身体<b>共用同一块 UV</b>
 * （{@code SHARED_UV} 里声明，校验脚本按"有意共用"放行），省下七份贴图空间。</p>
 *
 * <h3>为什么要泛型</h3>
 *
 * <p>第三批妖兽里的<b>玄冰蟒</b>（{@code FrostPythonEntity}）是虚空蛟的寒冰亚种：
 * 骨架、行波扭动、八节身体全部照用，只有贴图、品阶、咬中效果不同。
 * 模型做成 {@code SerpentModel<T>} 之后，两只蛇共用同一套几何，
 * <b>新怪不需要新模型</b>——这正是"优先复用现有模型"的落地方式。</p>
 *
 * <p>类型上界是 {@code CultivationMonsterEntity}：品阶发光层 {@link RankGlowLayer}
 * 要的就是这个上界。</p>
 */
@OnlyIn(Dist.CLIENT)
public class VoidSerpentRenderer<T extends com.hongmeng.xiuxian.entity.CultivationMonsterEntity>
        extends MobRenderer<T, VoidSerpentRenderer.SerpentModel<T>> {
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
    private static final double GROUND_LIFT = -0.31D;


    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(HongmengMod.MOD_ID, "void_serpent"), "main");

    private final ResourceLocation texture;

    public VoidSerpentRenderer(EntityRendererProvider.Context context) {
        this(context, "void_serpent");
    }

    /**
     * @param textureName 贴图名（不含 {@code .png}）：虚空蛟 {@code void_serpent}、
     *                    玄冰蟒 {@code frost_python}
     */
    public VoidSerpentRenderer(EntityRendererProvider.Context context, String textureName) {
        super(context, new SerpentModel<>(context.bakeLayer(LAYER)), 0.95F);
        this.texture = com.hongmeng.xiuxian.entity.CultivationMonsterEntity.textureFor(textureName);
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }

    /** 头目级：名牌默认就挂着，玩家隔着老远也该认出它。 */
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

    /** 蛇形模型：头 + 下颚 + 双角 + 八节身体 + 三片背鳍 + 尾尖。 */
    public static class SerpentModel<T extends com.hongmeng.xiuxian.entity.CultivationMonsterEntity>
            extends ColorableAgeableListModel<T> {

        /** 身体节数；改这里要同时改 {@code gen_beast_textures.py} 的说明与这里的注释。 */
        public static final int SEGMENTS = 8;

        /** 八节共用同一块 UV（{@code segment}），所以贴图上只有一份蛇身花纹。 */
        private final ModelPart[] segments = new ModelPart[SEGMENTS];
        private final ModelPart[] fins = new ModelPart[3];

        private final ModelPart head;
        private final ModelPart jaw;
        private final ModelPart rightHorn;
        private final ModelPart leftHorn;
        private final ModelPart tailTip;

        // ---- 身份件（同一套骨架，按实体类型显隐；见 setupAnim 末尾）----
        /** 虚空蛟：头侧两枚虚空结晶刺。 */
        private final ModelPart voidCrystals;
        /** 玄冰蟒：头侧两枚冰晶刺。 */
        private final ModelPart frostCrystals;
        /** 玄冰蟒：背部 3 道冰棱（挂在第 1/3/5 节背上，跟着行波扭）。 */
        private final ModelPart[] iceRidges = new ModelPart[3];

        public SerpentModel(ModelPart root) {
            this.head = root.getChild("head");
            this.jaw = this.head.getChild("jaw");
            this.rightHorn = this.head.getChild("right_horn");
            this.leftHorn = this.head.getChild("left_horn");

            ModelPart parent = root;
            for (int i = 0; i < SEGMENTS; i++) {
                ModelPart segment = parent.getChild("segment_" + i);
                this.segments[i] = segment;
                // 第 2/4/6 节背上插一片鳍
                int finIndex = i / 2;
                if (i % 2 == 1 && finIndex < this.fins.length) {
                    this.fins[finIndex] = segment.getChild("fin_" + finIndex);
                }
                parent = segment;
            }
            this.tailTip = parent.getChild("tail_tip");

            this.voidCrystals = this.head.getChild("void_crystals");
            this.frostCrystals = this.head.getChild("frost_crystals");
            for (int i = 0; i < this.iceRidges.length; i++) {
                this.iceRidges[i] = this.segments[i * 2 + 1].getChild("ice_ridge_" + i);
            }
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation none = CubeDeformation.NONE;

            // ---- 头：9 宽 8 高 12 长，向前伸 ----
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-4.5F, 0.0F, -12.0F, 9.0F, 8.0F, 12.0F, none),
                    PartPose.offset(0.0F, 8.0F, 0.0F));
            head.addOrReplaceChild("jaw", CubeListBuilder.create()
                            .texOffs(80, 0).addBox(-3.5F, 0.0F, -8.0F, 7.0F, 3.0F, 10.0F, none),
                    PartPose.offset(0.0F, 8.0F, -3.0F));
            head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                            .texOffs(83, 33).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, none),
                    PartPose.offset(-4.0F, 1.0F, -2.0F));
            head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                            .texOffs(95, 33).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, none),
                    PartPose.offset(4.0F, 1.0F, -2.0F));

            // ==================================================================
            // 身份件：蛇族 3 个变体共用这套骨架。UV 全排在 128×128 贴图空白区，
            // 显隐按实体类型（setupAnim 末尾，instanceof，不看品阶）。
            // ==================================================================
            // 虚空蛟：头侧两枚虚空结晶刺（朝外张开）
            head.addOrReplaceChild("void_crystals", CubeListBuilder.create()
                            .texOffs(97, 13).addBox(4.5F, 2.0F, -6.0F, 4.0F, 3.0F, 2.0F, none)
                            .texOffs(109, 16).addBox(-8.5F, 2.0F, -6.0F, 4.0F, 3.0F, 2.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // 玄冰蟒：头侧两枚冰晶刺（比虚空蛟的窄，蓝白）
            head.addOrReplaceChild("frost_crystals", CubeListBuilder.create()
                            .texOffs(42, 17).addBox(4.5F, 2.0F, -6.0F, 4.0F, 3.0F, 2.0F, none)
                            .texOffs(54, 17).addBox(-8.5F, 2.0F, -6.0F, 4.0F, 3.0F, 2.0F, none),
                    PartPose.offset(0.0F, 0.0F, 0.0F));

            // ---- 八节身体：第一节挂在根上，其余依次往后再挂 7 格 ----
            PartDefinition parent = root;
            for (int i = 0; i < SEGMENTS; i++) {
                PartPose pose = (i == 0)
                        ? PartPose.offset(0.0F, 11.0F, 1.0F)
                        : PartPose.offset(0.0F, 0.0F, 7.0F);
                PartDefinition segment = parent.addOrReplaceChild("segment_" + i, CubeListBuilder.create()
                                .texOffs(0, 33).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 7.0F, 7.0F, none),
                        pose);
                if (i % 2 == 1 && i / 2 < 3) {
                    int fin = i / 2;
                    int finU = 29 + fin * 15;
                    segment.addOrReplaceChild("fin_" + fin, CubeListBuilder.create()
                                    .texOffs(finU, 33).addBox(-0.5F, -5.0F, 0.0F, 1.0F, 5.0F, 6.0F, none),
                            PartPose.offset(0.0F, 0.0F, 2.0F));
                }
                // 玄冰蟒：第 1/3/5 节背上一道冰棱（在鳍前面一格，跟着这一节一起扭）
                if (i == 1 || i == 3 || i == 5) {
                    int ridge = (i - 1) / 2;
                    int ridgeU = 66 + ridge * 8;
                    int ridgeV = ridge == 0 ? 17 : 18;
                    segment.addOrReplaceChild("ice_ridge_" + ridge, CubeListBuilder.create()
                                    .texOffs(ridgeU, ridgeV).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, none),
                            PartPose.offset(0.0F, 0.0F, 0.0F));
                }
                parent = segment;
            }
            parent.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                            .texOffs(43, 0).addBox(-3.0F, 1.0F, 0.0F, 6.0F, 5.0F, 12.0F, none),
                    PartPose.offset(0.0F, 0.0F, 7.0F));

            return LayerDefinition.create(mesh, 128, 128);
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
         * 行波扭动。
         *
         * <p>第 i 节的偏航角 = {@code sin(时间 × 角速度 - i × 相位差) × 幅度}。
         * 因为每节都是上一节的子部件，这些角度会<b>沿着骨链累加</b>，
         * 于是整条蛇自然摆成一条 S 形——这正是"长虫"和"一串方块"的区别。</p>
         *
         * <p>另外每节再叠一点点俯仰上的起伏（幅度沿身体递减），
         * 看起来像贴着地面游动。<b>它趴得比地面低半格</b>（模型底边在 y=18，
         * 地面在 y=24）：虚空蛟是浮着的，不该有脚。</p>
         */
        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.3F
                    + Mth.sin(ageInTicks * 0.1F) * 0.04F;
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.4F;
            this.jaw.xRot = 0.12F + Mth.sin(ageInTicks * 0.16F) * 0.09F;
            this.rightHorn.zRot = -0.25F;
            this.leftHorn.zRot = 0.25F;

            // 游动速度：静止时也慢慢蠕动（它是有生命的东西），移动时幅度加倍
            float drive = 0.12F + Mth.clamp(limbSwingAmount, 0.0F, 1.0F) * 0.5F;

            for (int i = 0; i < SEGMENTS; i++) {
                float phase = ageInTicks * 0.18F - i * 0.62F;
                float yaw = Mth.sin(phase) * drive * 0.42F;
                float pitch = Mth.cos(phase * 0.75F) * drive * 0.10F * (1.0F - i * 0.08F);
                this.segments[i].yRot = yaw;
                this.segments[i].xRot = pitch;
            }

            for (int i = 0; i < this.fins.length; i++) {
                if (this.fins[i] != null) {
                    this.fins[i].yRot = Mth.sin(ageInTicks * 0.18F - (i * 2 + 1) * 0.62F) * 0.3F;
                }
            }

            this.tailTip.yRot = Mth.sin(ageInTicks * 0.18F - SEGMENTS * 0.62F) * drive * 0.5F;

            // ==================================================================
            // 身份件：按实体类型显隐（instanceof，不看品阶）
            // 虚空蛟 = VoidSerpentEntity 且不是玄冰蟒（玄冰蟒是它的子类）。
            // ==================================================================
            boolean frost = entity instanceof com.hongmeng.xiuxian.entity.FrostPythonEntity;
            boolean voidSnake = entity
                    instanceof com.hongmeng.xiuxian.entity.VoidSerpentEntity && !frost;
            this.voidCrystals.visible = voidSnake;
            this.frostCrystals.visible = frost;
            for (int i = 0; i < this.iceRidges.length; i++) {
                this.iceRidges[i].visible = frost;
            }

            // 头侧晶刺：随头部动作轻摆（绝对赋值）
            float cyst = Mth.sin(ageInTicks * 0.21F) * 0.10F;
            this.voidCrystals.zRot = cyst;
            this.frostCrystals.zRot = -cyst;
            // 背棱：跟着各自那一节的相位轻轻张合
            for (int i = 0; i < this.iceRidges.length; i++) {
                this.iceRidges[i].xRot =
                        Mth.sin(ageInTicks * 0.18F - (i * 2 + 1) * 0.62F) * 0.14F;
            }
        }
    }
}
