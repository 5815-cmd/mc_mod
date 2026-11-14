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
 * <b>虚空道主 · 专属骨架</b>（玩家要求「单独的模型」，且要真正的独立骨架）。
 *
 * <h2>形体（与其它三只完全不同）</h2>
 * <p>不是人形 —— 是一具<b>浮空的虚空体</b>：</p>
 * <ul>
 *   <li><b>没有腿</b>：下身收成一束<b>尾锥</b>（悬在离地一点点的位置），
 *       所以它是"浮着"的，不是站着；</li>
 *   <li><b>四条臂</b>：上臂粗、下臂细，左右各两条，向后张开；</li>
 *   <li><b>背生六片翼</b>：不是一对，是左右各三片、层叠展开；</li>
 *   <li><b>头部是一只"独眼"</b>：八面体颅骨 + 正中一颗大眼（带环状瞳孔），
 *       头顶三根虚空水晶尖；</li>
 *   <li><b>躯体是空的</b>：胸腹之间留一道缝，里面透出虚空核心（渲染成发光层）。</li>
 * </ul>
 *
 * <h2>⚠ 脚底基准（这个项目最容易做错的地方）</h2>
 * <p>原版画模型前会 {@code translate(0, -1.501, 0)}（= 24.016 像素），
 * 所以<b>模型空间的最低点必须停在 y=24</b> 才正好踩在地上。</p>
 *
 * <p>本骨架的最低点是<b>尾锥底面</b>：尾锥放在 {@code y=24} 且盒高 18、
 * 盒子的局部坐标从 {@code -18} 到 {@code 0}，于是最低点正好 = 24 ✔</p>
 *
 * <pre>
 *   尾锥  PartPose.offset(0, 24, 0) + addBox(-3.5, -18, -3.5, 7, 18, 7)
 *         → 世界 y 范围 6..24，最低点 24 ✔
 * </pre>
 *
 * <p>因此这个渲染器的 {@code GROUND_LIFT} 是 <b>0</b>（不需要修正）。
 * {@code tools/audit_model_ground.py} 会实测复核这一点。</p>
 *
 * <h2>UV 版式（贴图 128×128，**程序化排布，0 重叠 / 0 越界**）</h2>
 * <pre>
 *   盒子         texOffs     尺寸
 *   tail          0,  0     7×18×7
 *   skull        29,  0     9×9×9
 *   chest        66,  0    11×10×7
 *   shoulder      0, 26    13×5×8
 *   waist        43, 26     9×4×9
 *   wing         80, 26    12×1×9
 *   arm_upper     0, 40     4×12×4
 *   core         17, 40     5×5×5
 *   arm_lower    38, 40     3×11×3
 *   neck         51, 40     4×5×4
 *   eye          68, 40     5×5×1
 *   spike        81, 40     2×6×2
 *   pupil        90, 40     3×3×1
 * </pre>
 *
 * <p>⚠ 这组 UV 是<b>自动装箱</b>排出来的（{@code tools/relayout_void_lord_uv.py}），
 * 排完做了**逐对重叠检查与越界检查**，都是 0。
 * 上一版我手工挪 UV（披风/翼/尾），结果撞出新重叠 ——
 * <b>手工排 2D 装箱必错</b>，所以改成程序化，改 UV 时也请跑那个脚本复核。</p>
 */
public class VoidLordModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart root;
    private final ModelPart tail;
    private final ModelPart waist;
    private final ModelPart chest;
    private final ModelPart core;
    private final ModelPart neck;
    private final ModelPart skull;
    private final ModelPart eye;
    private final ModelPart pupil;
    private final ModelPart spikeMid;
    private final ModelPart spikeL;
    private final ModelPart spikeR;
    private final ModelPart armUpperL;
    private final ModelPart armLowerL;
    private final ModelPart armUpperR;
    private final ModelPart armLowerR;
    private final ModelPart armUpperL2;
    private final ModelPart armLowerL2;
    private final ModelPart armUpperR2;
    private final ModelPart armLowerR2;
    private final ModelPart crystalL;
    private final ModelPart crystalR;
    private final ModelPart crystalBack;
    private final ModelPart[] wings = new ModelPart[6];

    public VoidLordModel(ModelPart root) {
        this.root = root;
        this.tail = root.getChild("tail");
        this.waist = root.getChild("waist");
        this.chest = root.getChild("chest");
        this.core = root.getChild("core");
        this.neck = root.getChild("neck");
        this.skull = root.getChild("skull");
        this.eye = root.getChild("eye");
        this.pupil = root.getChild("pupil");
        this.spikeMid = root.getChild("spike_mid");
        this.spikeL = root.getChild("spike_l");
        this.spikeR = root.getChild("spike_r");
        this.armUpperL = root.getChild("arm_upper_l");
        this.armLowerL = root.getChild("arm_lower_l");
        this.armUpperR = root.getChild("arm_upper_r");
        this.armLowerR = root.getChild("arm_lower_r");
        this.armUpperL2 = root.getChild("arm_upper_l2");
        this.armLowerL2 = root.getChild("arm_lower_l2");
        this.armUpperR2 = root.getChild("arm_upper_r2");
        this.armLowerR2 = root.getChild("arm_lower_r2");
        this.crystalL = root.getChild("crystalL");
        this.crystalR = root.getChild("crystalR");
        this.crystalBack = root.getChild("crystalBack");
        for (int i = 0; i < 6; i++) {
            this.wings[i] = root.getChild("wing_" + i);
        }
    }

    /** 建骨架（最低点严格落在 y=24）。 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- 尾锥：最低点 = 24（盒子的局部 y 从 -18 到 0）----
        root.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -18.0F, -3.5F, 7.0F, 18.0F, 7.0F, NONE),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // ---- 腰环：接在尾锥上方 ----
        root.addOrReplaceChild("waist", CubeListBuilder.create()
                        .texOffs(43, 26).addBox(-4.5F, -4.0F, -4.5F, 9.0F, 4.0F, 9.0F, NONE),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        // ---- 胸腔：比腰环收窄一点，做出"束腰"感 ----
        root.addOrReplaceChild("chest", CubeListBuilder.create()
                        .texOffs(66, 0).addBox(-5.5F, -10.0F, -3.5F, 11.0F, 10.0F, 7.0F, NONE),
                PartPose.offset(0.0F, 4.0F, 0.0F));

        // ---- 虚空核心：嵌在胸腹之间（发光层）----
        root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(17, 40).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, NONE),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        // ---- 肩甲：横贯双肩 ----
        root.addOrReplaceChild("shoulder", CubeListBuilder.create()
                        .texOffs(0, 26).addBox(-6.5F, -5.0F, -4.0F, 13.0F, 5.0F, 8.0F, NONE),
                PartPose.offset(0.0F, -6.0F, 0.0F));

        // ---- 颈 + 颅骨 + 独眼 ----
        root.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(51, 40).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F, NONE),
                PartPose.offset(0.0F, -11.0F, 0.0F));
        root.addOrReplaceChild("skull", CubeListBuilder.create()
                        .texOffs(29, 0).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F, NONE),
                PartPose.offset(0.0F, -16.0F, 0.0F));
        // 独眼（贴在颅骨正面）
        root.addOrReplaceChild("eye", CubeListBuilder.create()
                        .texOffs(68, 40).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 1.0F, NONE),
                PartPose.offset(0.0F, -21.5F, -4.6F));
        root.addOrReplaceChild("pupil", CubeListBuilder.create()
                        .texOffs(90, 40).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 1.0F, NONE),
                PartPose.offset(0.0F, -21.5F, -5.2F));

        // ---- 头顶三根虚空水晶尖 ----
        root.addOrReplaceChild("spike_mid", CubeListBuilder.create()
                        .texOffs(81, 40).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE),
                PartPose.offset(0.0F, -25.0F, 0.0F));
        root.addOrReplaceChild("spike_l", CubeListBuilder.create()
                        .texOffs(81, 40).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE),
                PartPose.offsetAndRotation(3.0F, -25.0F, 0.0F, 0.0F, 0.0F, -0.35F));
        root.addOrReplaceChild("spike_r", CubeListBuilder.create()
                        .texOffs(81, 40).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, NONE),
                PartPose.offsetAndRotation(-3.0F, -25.0F, 0.0F, 0.0F, 0.0F, 0.35F));

        // ---- 四条臂（上臂 + 下臂），左右前后各一套 ----
        // 前左 / 前右
        root.addOrReplaceChild("arm_upper_l", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(5.5F, -6.0F, 0.0F, 0.35F, 0.0F, -0.30F));
        root.addOrReplaceChild("arm_lower_l", CubeListBuilder.create()
                        .texOffs(38, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, NONE),
                PartPose.offsetAndRotation(5.5F, 5.5F, 0.0F, 0.60F, 0.0F, -0.15F));
        root.addOrReplaceChild("arm_upper_r", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(-5.5F, -6.0F, 0.0F, 0.35F, 0.0F, 0.30F));
        root.addOrReplaceChild("arm_lower_r", CubeListBuilder.create()
                        .texOffs(38, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, NONE),
                PartPose.offsetAndRotation(-5.5F, 5.5F, 0.0F, 0.60F, 0.0F, 0.15F));
        // 后左 / 后右（从肩胛向后长出）
        root.addOrReplaceChild("arm_upper_l2", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(5.0F, -7.0F, 3.0F, -0.60F, 0.0F, -0.45F));
        root.addOrReplaceChild("arm_lower_l2", CubeListBuilder.create()
                        .texOffs(38, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, NONE),
                PartPose.offsetAndRotation(5.0F, -7.0F, 3.0F, -0.90F, 0.0F, -0.30F));
        root.addOrReplaceChild("arm_upper_r2", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(-5.0F, -7.0F, 3.0F, -0.60F, 0.0F, 0.45F));
        root.addOrReplaceChild("arm_lower_r2", CubeListBuilder.create()
                        .texOffs(38, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, NONE),
                PartPose.offsetAndRotation(-5.0F, -7.0F, 3.0F, -0.90F, 0.0F, 0.30F));

        // ---- 六片翼：左右各三片，层叠展开 ----
        for (int i = 0; i < 3; i++) {
            float spread = 0.30F + i * 0.22F;      // 越靠外张得越开
            float lift = -3.0F - i * 2.0F;
            // 左
            root.addOrReplaceChild("wing_" + (i * 2), CubeListBuilder.create()
                            .texOffs(80, 26).addBox(0.0F, -8.0F, 0.0F, 12.0F, 1.0F, 9.0F, NONE),
                    PartPose.offsetAndRotation(2.5F, lift, 3.5F, 0.45F, -spread, 0.0F));
            // 右
            root.addOrReplaceChild("wing_" + (i * 2 + 1), CubeListBuilder.create()
                            .texOffs(80, 26).addBox(-12.0F, -8.0F, 0.0F, 12.0F, 1.0F, 9.0F, NONE),
                    PartPose.offsetAndRotation(-2.5F, lift, 3.5F, 0.45F, spread, 0.0F));
        }

                root.addOrReplaceChild("crystalL", CubeListBuilder.create()
                        .texOffs(43, 26).addBox(-1.2F, -2.6F, -1.2F, 2.4F, 5.2F, 2.4F, CubeDeformation.NONE),
                PartPose.offset(14.0F, 14.0F, 2.0F));
        root.addOrReplaceChild("crystalR", CubeListBuilder.create()
                        .texOffs(43, 26).addBox(-1.2F, -2.6F, -1.2F, 2.4F, 5.2F, 2.4F, CubeDeformation.NONE),
                PartPose.offset(-14.0F, 14.0F, 2.0F));
        root.addOrReplaceChild("crystalBack", CubeListBuilder.create()
                        .texOffs(72, 19).addBox(-1.6F, -3.0F, -1.6F, 3.2F, 6.0F, 3.2F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 10.0F, 13.0F));
return LayerDefinition.create(mesh, 128, 128);
    }

    /** 这具骨架的最低点像素 y（尾锥底面）。渲染器用它算离地修正。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /**
     * 画出本体。
     *
     * <p>前六个参数是原版 {@code Model.renderToBuffer} 的标准签名；
     * {@code EntityModel} 是抽象类，必须自己实现这一条（人形模型是从
     * {@code HumanoidModel} 继承来的，所以那边不用写）。</p>
     */
    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        // 本骨架是**一个整体**（部件之间靠 offset 相对定位），
        // 所以直接从根节点画，子部件会跟着一起出来。
        this.root.render(poseStack, buffer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    @Override
    public void setupAnim(CultivationMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // ---- 头部跟随玩家（独眼会转过来盯着你）----
        this.neck.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.5F;
        this.neck.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.4F;
        this.skull.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.5F;
        this.skull.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.3F;
        this.eye.yRot = this.skull.yRot;
        this.eye.xRot = this.skull.xRot;
        this.pupil.yRot = this.skull.yRot;
        this.pupil.xRot = this.skull.xRot;

        // ---- 整体悬浮：缓慢上下浮（它是浮空的，没有腿）----
        float bob = Mth.sin(ageInTicks * 0.06F) * 1.2F;
        this.root.y = bob;
        // 悬浮时尾锥轻微摆动
        this.tail.xRot = Mth.sin(ageInTicks * 0.05F) * 0.05F;
        this.waist.xRot = Mth.sin(ageInTicks * 0.05F + 1.0F) * 0.04F;

        // ---- 虚空核缓慢自转 + 呼吸 ----
        this.core.yRot = ageInTicks * 0.12F;
        this.core.xRot = ageInTicks * 0.07F;
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.15F) * 0.08F;
        this.core.xScale = pulse;
        this.core.yScale = pulse;
        this.core.zScale = pulse;

        // ---- 六片翼：staggered 扇动（每片错开相位，看起来像活物）----
        for (int i = 0; i < 6; i++) {
            float phase = i * 0.9F;
            float flap = Mth.sin(ageInTicks * 0.09F + phase) * 0.30F;
            ModelPart w = this.wings[i];
            // 奇偶是左右两侧，扇动方向相反
            w.zRot = (i % 2 == 0) ? flap : -flap;
            w.xRot = 0.45F + Mth.sin(ageInTicks * 0.07F + phase) * 0.10F;
        }

        // ---- 四条臂：各自不同相位的摆动 ----
        float swing = Mth.sin(ageInTicks * 0.08F);
        this.armUpperL.xRot = 0.35F + swing * 0.25F;
        this.armUpperR.xRot = 0.35F - swing * 0.25F;
        this.armUpperL2.xRot = -0.60F + swing * 0.20F;
        this.armUpperR2.xRot = -0.60F - swing * 0.20F;
        this.armLowerL.xRot = 0.60F + swing * 0.18F;
        this.armLowerR.xRot = 0.60F - swing * 0.18F;
        this.armLowerL2.xRot = -0.90F + swing * 0.15F;
        this.armLowerR2.xRot = -0.90F - swing * 0.15F;

        // ---- 水晶尖随头部微动 ----
        // ⚠ 不写成 `new ModelPart[]{...}` 数组遍历 —— 项目的模型解析器
        //   tools/model_parse_lib.py 不支持数组初始化器语法，
        //   而离地审计要靠它算最低点，那样会让整个审计失败。
        this.spikeMid.yRot = this.skull.yRot;
        this.spikeL.yRot = this.skull.yRot;
        this.spikeR.yRot = this.skull.yRot;
    
        float t = ageInTicks;
        this.crystalL.yRot = t * 0.9F; this.crystalL.y = 14.0F + Mth.sin(t * 0.08F) * 0.5F;
        this.crystalR.yRot = -t * 0.9F; this.crystalR.y = 14.0F + Mth.sin(t * 0.08F + 1.0F) * 0.5F;
        this.crystalBack.yRot = t * 0.6F;
    }
}
