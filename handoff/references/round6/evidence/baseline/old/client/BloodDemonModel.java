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
 * <b>血魔老祖 · 专属骨架</b>（三头六臂的血肉巨人）。
 *
 * <h2>与虚空道主的对比（两套骨架形体完全不同）</h2>
 * <table border="1">
 *   <caption>形体对比</caption>
 *   <tr><th></th><th>虚空道主</th><th>血魔老祖</th></tr>
 *   <tr><td>下身</td><td><b>无腿</b>，尾锥，浮空</td><td><b>三条腿</b>，三脚架站姿</td></tr>
 *   <tr><td>头</td><td>一颗独眼</td><td><b>三颗头</b>（中大一左右小）</td></tr>
 *   <tr><td>臂</td><td>四条</td><td><b>六条</b>（每侧三条）</td></tr>
 *   <tr><td>背部</td><td>六片翼</td><td><b>一条骨刺脊</b>（不是翼）</td></tr>
 *   <tr><td>核心</td><td>虚空核（青）</td><td><b>血色晶核</b>（会脉动）</td></tr>
 * </table>
 *
 * <h2>⚠ 脚底基准</h2>
 * <p>原版渲染前 {@code translate(0, -1.501, 0)}（≈24.016 像素）→
 * <b>模型最低点必须停在 y=24</b>。三条腿的盒底都放在 {@code y=24}
 * （盒高 12，局部 y 从 {@code -12} 到 {@code 0}），所以最低点 = 24 ✔ → {@code GROUND_LIFT = 0}。</p>
 *
 * <h2>UV 版式（128×128，**程序化排布，0 重叠 / 0 越界**）</h2>
 * <p>由 {@code tools/relayout_blood_demon_uv.py} 装箱排出并逐对校验。</p>
 */
public class BloodDemonModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    /** 最低点像素 y（三条腿的盒底）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    private final ModelPart root;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart legB;
    private final ModelPart hip;
    private final ModelPart chest;
    private final ModelPart core;
    private final ModelPart spine;
    private final ModelPart neck;
    private final ModelPart headMid;
    private final ModelPart headL;
    private final ModelPart headR;
    private final ModelPart eyeMid;
    private final ModelPart eyeL;
    private final ModelPart eyeR;
    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart spikeA;
    private final ModelPart spikeB;
    // 六条臂各自的部件（不写成数组 —— 模型解析器不支持数组初始化器语法）
    private final ModelPart armU1;
    private final ModelPart armL1;
    private final ModelPart armU2;
    private final ModelPart armL2;
    private final ModelPart armU3;
    private final ModelPart armL3;
    private final ModelPart armU1r;
    private final ModelPart armL1r;
    private final ModelPart armU2r;
    private final ModelPart armL2r;
    private final ModelPart armU3r;
    private final ModelPart armL3r;
    private final ModelPart spineTop;

    public BloodDemonModel(ModelPart root) {
        this.root = root;
        this.legL = root.getChild("leg_l");
        this.legR = root.getChild("leg_r");
        this.legB = root.getChild("leg_b");
        this.hip = root.getChild("hip");
        this.chest = root.getChild("chest");
        this.core = root.getChild("core");
        this.spine = root.getChild("spine");
        this.neck = root.getChild("neck");
        this.headMid = root.getChild("head_mid");
        this.headL = root.getChild("head_l");
        this.headR = root.getChild("head_r");
        this.eyeMid = root.getChild("eye_mid");
        this.eyeL = root.getChild("eye_l");
        this.eyeR = root.getChild("eye_r");
        this.hornL = root.getChild("horn_l");
        this.hornR = root.getChild("horn_r");
        this.spikeA = root.getChild("spike_a");
        this.spikeB = root.getChild("spike_b");
        this.armU1 = root.getChild("arm_u1");
        this.armL1 = root.getChild("arm_l1");
        this.armU2 = root.getChild("arm_u2");
        this.armL2 = root.getChild("arm_l2");
        this.armU3 = root.getChild("arm_u3");
        this.armL3 = root.getChild("arm_l3");
        this.armU1r = root.getChild("arm_u1_r");
        this.armL1r = root.getChild("arm_l1_r");
        this.armU2r = root.getChild("arm_u2_r");
        this.armL2r = root.getChild("arm_l2_r");
        this.armU3r = root.getChild("arm_u3_r");
        this.armL3r = root.getChild("arm_l3_r");
        this.spineTop = root.getChild("spineTop");
    }

    /** 建骨架：三腿 + 三头 + 六臂 + 骨刺脊；最低点严格落在 y=24。 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ================= 三条腿（三脚架站姿）=================
        // 盒高 12、局部 y 从 -12 到 0，基点 y=24 → 最低点 24 ✔
        root.addOrReplaceChild("leg_l", CubeListBuilder.create()
                        .texOffs(83, 40).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(6.0F, 24.0F, 0.0F, 0.0F, 0.0F, -0.16F));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create()
                        .texOffs(100, 40).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(-6.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.16F));
        root.addOrReplaceChild("leg_b", CubeListBuilder.create()
                        .texOffs(0, 57).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 24.0F, 6.0F, -0.16F, 0.0F, 0.0F));

        // ================= 胯 + 胸 =================
        root.addOrReplaceChild("hip", CubeListBuilder.create()
                        .texOffs(72, 21).addBox(-5.5F, -5.0F, -4.0F, 11.0F, 5.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("chest", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.5F, -12.0F, -4.0F, 13.0F, 12.0F, 8.0F, NONE),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        // 血色晶核（发光层）
        root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(58, 40).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, NONE),
                PartPose.offset(0.0F, -1.0F, -1.0F));

        // ================= 背后骨刺脊 =================
        root.addOrReplaceChild("spine", CubeListBuilder.create()
                        .texOffs(37, 21).addBox(-1.5F, -1.0F, -14.0F, 3.0F, 2.0F, 14.0F, NONE),
                PartPose.offsetAndRotation(0.0F, -3.0F, 4.0F, 0.25F, 0.0F, 0.0F));
        root.addOrReplaceChild("spike_a", CubeListBuilder.create()
                        .texOffs(18, 74).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, NONE),
                PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, -0.15F, 0.0F, 0.0F));
        root.addOrReplaceChild("spike_b", CubeListBuilder.create()
                        .texOffs(27, 74).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, NONE),
                PartPose.offsetAndRotation(0.0F, -4.0F, -4.0F, -0.25F, 0.0F, 0.0F));

        // ================= 肩甲（横贯六条臂的根）=================
        root.addOrReplaceChild("shoulder", CubeListBuilder.create()
                        .texOffs(43, 0).addBox(-9.5F, -5.0F, -4.0F, 19.0F, 4.0F, 8.0F, NONE),
                PartPose.offset(0.0F, -5.0F, 0.0F));

        // ================= 六条臂（每侧三条，前后错开）=================
        // 上臂：盒高 12、局部 y 从 0 到 12（从肩往下挂）
        //
        // ⚠ 这里**不用 Java 数组初始化器**（`float[][] x = {{...},{...}}`）——
        // 项目的模型解析器（tools/model_parse_lib.py）不支持那种写法，
        // 而离地审计要靠它算最低点。所以写成三条显式调用。
        armPair(root, 0, 8.5F, -5.0F, 0.0F, 0.20F, -0.28F);
        armPair(root, 1, 8.0F, -5.0F, 3.0F, -0.35F, -0.42F);
        armPair(root, 2, 8.0F, -5.0F, -3.0F, 0.55F, -0.36F);

        // ================= 颈 + 三颗头 =================
        root.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(107, 57).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F, NONE),
                PartPose.offset(0.0F, -9.0F, 0.0F));
        // 正中的头（大）
        root.addOrReplaceChild("head_mid", CubeListBuilder.create()
                        .texOffs(0, 21).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F, NONE),
                PartPose.offset(0.0F, -14.0F, 0.0F));
        // 左右两颗小头（各自独立转动 —— 三颗头一起盯着你）
        root.addOrReplaceChild("head_l", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F, NONE),
                PartPose.offsetAndRotation(6.0F, -14.5F, 0.0F, 0.0F, 0.0F, -0.30F));
        root.addOrReplaceChild("head_r", CubeListBuilder.create()
                        .texOffs(29, 40).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F, NONE),
                PartPose.offsetAndRotation(-6.0F, -14.5F, 0.0F, 0.0F, 0.0F, 0.30F));

        // 三对眼睛（贴在各自脸前）
        root.addOrReplaceChild("eye_mid", CubeListBuilder.create()
                        .texOffs(36, 74).addBox(-2.5F, -1.5F, 0.0F, 5.0F, 3.0F, 1.0F, NONE),
                PartPose.offset(0.0F, -19.0F, -4.6F));
        root.addOrReplaceChild("eye_l", CubeListBuilder.create()
                        .texOffs(49, 74).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 1.0F, NONE),
                PartPose.offsetAndRotation(6.0F, -18.5F, -3.6F, 0.0F, 0.0F, -0.30F));
        root.addOrReplaceChild("eye_r", CubeListBuilder.create()
                        .texOffs(58, 74).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 1.0F, NONE),
                PartPose.offsetAndRotation(-6.0F, -18.5F, -3.6F, 0.0F, 0.0F, 0.30F));

        // 一对魔角（长在正中的头上）
        root.addOrReplaceChild("horn_l", CubeListBuilder.create()
                        .texOffs(0, 74).addBox(0.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F, NONE),
                PartPose.offsetAndRotation(3.5F, -22.0F, 0.0F, -0.30F, 0.0F, -0.40F));
        root.addOrReplaceChild("horn_r", CubeListBuilder.create()
                        .texOffs(9, 74).addBox(-2.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F, NONE),
                PartPose.offsetAndRotation(-3.5F, -22.0F, 0.0F, -0.30F, 0.0F, 0.40F));

        root.addOrReplaceChild("spineTop", CubeListBuilder.create()
                        .texOffs(66, 14).addBox(-1.4F, -7.0F, -1.0F, 2.8F, 7.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 2.0F, 5.0F));
return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * 挂一对臂（左 + 右镜像）：上臂从肩往下挂，下臂接在上臂末端。
     *
     * <p>抽成方法而不是用数组循环，是为了让离地审计的模型解析器能读懂
     * （它不支持 Java 的数组初始化器语法）。</p>
     *
     * @param idx 第几对（0/1/2），决定 UV 偏移与名字
     */
    private static void armPair(PartDefinition root, int idx, float ax, float ay, float az,
                                float xRot, float zRot) {
        int upperU = 17 + idx * 17;
        int lowerU = 68 + idx * 13;
        // 左
        root.addOrReplaceChild("arm_u" + (idx + 1), CubeListBuilder.create()
                        .texOffs(upperU, 57).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(ax, ay, az, xRot, 0.0F, zRot));
        root.addOrReplaceChild("arm_l" + (idx + 1), CubeListBuilder.create()
                        .texOffs(lowerU, 57).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, NONE),
                PartPose.offsetAndRotation(ax + 0.5F, ay + 11.0F, az,
                        xRot + 0.45F, 0.0F, zRot * 0.5F));
        // 右（镜像）
        root.addOrReplaceChild("arm_u" + (idx + 1) + "_r", CubeListBuilder.create()
                        .texOffs(upperU, 57).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, NONE),
                PartPose.offsetAndRotation(-ax, ay, az, xRot, 0.0F, -zRot));
        root.addOrReplaceChild("arm_l" + (idx + 1) + "_r", CubeListBuilder.create()
                        .texOffs(lowerU, 57).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, NONE),
                PartPose.offsetAndRotation(-ax - 0.5F, ay + 11.0F, az,
                        xRot + 0.45F, 0.0F, -zRot * 0.5F));
    }

    /** 摆一对臂（左 + 右）。抽成方法是为了避开数组初始化器语法。 */
    private static void armSwing(ModelPart ul, ModelPart ll, ModelPart ur, ModelPart lr,
                                 float ageInTicks, float walk, float phase, float base) {
        float swing = Mth.sin(ageInTicks * 0.08F + phase) * 0.30F + walk * 0.35F;
        ul.xRot = base + swing;
        ur.xRot = base - swing;
        ll.xRot = base + 0.45F + swing * 0.8F;
        lr.xRot = base + 0.45F - swing * 0.8F;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    @Override
    public void setupAnim(CultivationMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float yaw = netHeadYaw * ((float) Math.PI / 180.0F);
        float pitch = headPitch * ((float) Math.PI / 180.0F);

        // ---- 三颗头各自转动（中间跟得最准，两侧慢半拍 + 外偏）----
        this.headMid.yRot = yaw * 0.7F;
        this.headMid.xRot = pitch * 0.6F;
        this.headL.yRot = yaw * 0.5F + 0.35F;
        this.headL.xRot = pitch * 0.4F;
        this.headR.yRot = yaw * 0.5F - 0.35F;
        this.headR.xRot = pitch * 0.4F;
        this.eyeMid.yRot = this.headMid.yRot;
        this.eyeMid.xRot = this.headMid.xRot;
        this.eyeL.yRot = this.headL.yRot;
        this.eyeR.yRot = this.headR.yRot;
        this.hornL.yRot = this.headMid.yRot;
        this.hornR.yRot = this.headMid.yRot;
        this.neck.yRot = yaw * 0.35F;

        // ---- 六条臂：每对错开相位，做出"六臂乱舞"的感觉 ----
        float walk = Mth.cos(limbSwing * 0.5F) * limbSwingAmount;
        armSwing(this.armU1, this.armL1, this.armU1r, this.armL1r, ageInTicks, walk, 0.0F, 0.20F);
        armSwing(this.armU2, this.armL2, this.armU2r, this.armL2r, ageInTicks, walk, 1.1F, -0.35F);
        armSwing(this.armU3, this.armL3, this.armU3r, this.armL3r, ageInTicks, walk, 2.2F, 0.55F);

        // ---- 三条腿：交替摆动（三脚架步态）----
        float step = Mth.cos(limbSwing * 0.6F) * limbSwingAmount;
        this.legL.xRot = step * 0.5F;
        this.legR.xRot = -step * 0.5F;
        this.legB.xRot = Mth.cos(limbSwing * 0.6F + 1.5F) * limbSwingAmount * 0.4F;

        // ---- 血色晶核：脉动（比虚空核更快、更像心跳）----
        float beat = 1.0F + Mth.sin(ageInTicks * 0.35F) * 0.12F;
        this.core.xScale = beat;
        this.core.yScale = beat;
        this.core.zScale = beat;
        this.core.yRot = ageInTicks * 0.05F;

        // ---- 骨刺脊微微起伏 ----
        this.spine.xRot = 0.25F + Mth.sin(ageInTicks * 0.06F) * 0.05F;
        this.spikeA.xRot = -0.15F + Mth.sin(ageInTicks * 0.07F) * 0.06F;
        this.spikeB.xRot = -0.25F + Mth.sin(ageInTicks * 0.07F + 1.0F) * 0.06F;

        // ---- 胸腔随呼吸起伏 ----
        this.chest.xRot = Mth.sin(ageInTicks * 0.05F) * 0.03F;
    
        float t = ageInTicks;
        this.spineTop.xRot = Mth.sin(t * 0.10F) * 0.05F;
    }
}
