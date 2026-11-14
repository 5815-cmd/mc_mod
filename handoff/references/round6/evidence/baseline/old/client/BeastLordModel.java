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
 * <b>万兽之主 · 专属骨架</b>（<b>四足兽形</b>，不是人形）。
 *
 * <h2>三只 boss 的形体各不相同</h2>
 * <table border="1">
 *   <caption>骨架对比</caption>
 *   <tr><th></th><th>虚空道主</th><th>血魔老祖</th><th>万兽之主</th></tr>
 *   <tr><td>姿态</td><td>浮空（无腿）</td><td>直立（三腿）</td><td><b>四足趴行</b></td></tr>
 *   <tr><td>躯干</td><td>尾锥 + 腰环 + 胸</td><td>胯 + 胸</td><td><b>长躯干 + 驼峰 + 臀</b></td></tr>
 *   <tr><td>头</td><td>独眼颅骨</td><td>三颗头</td><td><b>兽首 + 可开合的上下颚</b></td></tr>
 *   <tr><td>臂</td><td>四条</td><td>六条</td><td>—（前肢是腿）</td></tr>
 *   <tr><td>背</td><td>六片翼</td><td>骨刺脊</td><td><b>三排骨板</b></td></tr>
 *   <tr><td>尾</td><td>—</td><td>—</td><td><b>长尾 + 尾尖</b></td></tr>
 * </table>
 *
 * <h2>⚠ 脚底基准</h2>
 * <p>原版渲染前 {@code translate(0, -1.501, 0)}（≈24.016 像素）→
 * <b>模型最低点必须停在 y=24</b>。四条腿的盒底（爪子）都放在 {@code y=24}
 * （腿盒高 12–13、局部 y 从 {@code -h} 到 0，基点 y=24），所以最低点 = 24 ✔
 * → {@code GROUND_LIFT = 0}。</p>
 *
 * <h2>UV 版式（贴图 192×128）</h2>
 * <p>四足兽的盒子比前两只多，128 宽装不下，所以加宽到 192。
 * 由 {@code tools/relayout_beast_lord_uv2.py} 程序化装箱排出并逐对校验（0 重叠 / 0 越界）。</p>
 */
public class BeastLordModel extends EntityModel<CultivationMonsterEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    /** 最低点像素 y（四只爪子）。 */
    public static final int LOWEST_PIXEL_Y = 24;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart hump;
    private final ModelPart hips;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart eyeL;
    private final ModelPart eyeR;
    private final ModelPart tuskL;
    private final ModelPart tuskR;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart legFL;
    private final ModelPart legFR;
    private final ModelPart legBL;
    private final ModelPart legBR;
    // 四只**独立**的爪（原来前后各是一块横跨左右的整板，读起来像连体爪）
    private final ModelPart pawFL;
    private final ModelPart pawFR;
    private final ModelPart pawBL;
    private final ModelPart pawBR;
    private final ModelPart plate1;
    private final ModelPart plate2;
    private final ModelPart plate3;
    private final ModelPart plateA;
    private final ModelPart plateB;
    private final ModelPart plateC;
    private final ModelPart plateD;

    public BeastLordModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.hump = root.getChild("shoulder_hump");
        this.hips = root.getChild("hips");
        this.neck = root.getChild("neck");
        this.head = root.getChild("head");
        this.jaw = root.getChild("jaw");
        this.hornL = root.getChild("horn_l");
        this.hornR = root.getChild("horn_r");
        this.eyeL = root.getChild("eye_l");
        this.eyeR = root.getChild("eye_r");
        this.tuskL = root.getChild("tusk_l");
        this.tuskR = root.getChild("tusk_r");
        this.tail = root.getChild("tail");
        this.tailTip = root.getChild("tail_tip");
        this.legFL = root.getChild("leg_fl");
        this.legFR = root.getChild("leg_fr");
        this.legBL = root.getChild("leg_bl");
        this.legBR = root.getChild("leg_br");
        this.pawFL = root.getChild("paw_fl");
        this.pawFR = root.getChild("paw_fr");
        this.pawBL = root.getChild("paw_bl");
        this.pawBR = root.getChild("paw_br");
        this.plate1 = root.getChild("spine_plate1");
        this.plate2 = root.getChild("spine_plate2");
        this.plate3 = root.getChild("spine_plate3");
        this.plateA = root.getChild("plateA");
        this.plateB = root.getChild("plateB");
        this.plateC = root.getChild("plateC");
        this.plateD = root.getChild("plateD");
    }

    /** 建骨架：四足趴行；最低点（爪底）严格落在 y=24。 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ================= 长躯干（趴行的主体）=================
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-9.0F, -6.0F, -13.0F, 18.0F, 12.0F, 26.0F, NONE),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        // 肩部驼峰（兽类的隆起）
        root.addOrReplaceChild("shoulder_hump", CubeListBuilder.create()
                        .texOffs(104, 39).addBox(-8.0F, -5.0F, -5.0F, 16.0F, 5.0F, 10.0F, NONE),
                PartPose.offset(0.0F, 9.0F, -6.0F));
        // 臀部
        root.addOrReplaceChild("hips", CubeListBuilder.create()
                        .texOffs(0, 39).addBox(-8.0F, -4.0F, -6.0F, 16.0F, 9.0F, 12.0F, NONE),
                PartPose.offset(0.0F, 15.0F, 12.0F));

        // ================= 四条腿 + 爪子 =================
        // 前腿（盒高 12，局部 y 从 -12 到 0，基点 24 → 最低点 24）
        root.addOrReplaceChild("leg_fl", CubeListBuilder.create()
                        .texOffs(159, 62).addBox(-2.5F, -12.0F, -2.5F, 5.0F, 12.0F, 5.0F, NONE),
                PartPose.offsetAndRotation(6.5F, 22.0F, -8.0F, 0.0F, 0.0F, -0.10F));
        root.addOrReplaceChild("leg_fr", CubeListBuilder.create()
                        .texOffs(0, 82).addBox(-2.5F, -12.0F, -2.5F, 5.0F, 12.0F, 5.0F, NONE),
                PartPose.offsetAndRotation(-6.5F, 22.0F, -8.0F, 0.0F, 0.0F, 0.10F));
        // 后腿（更粗、盒高 13）
        root.addOrReplaceChild("leg_bl", CubeListBuilder.create()
                        .texOffs(78, 62).addBox(-3.0F, -13.0F, -3.0F, 6.0F, 13.0F, 6.0F, NONE),
                PartPose.offsetAndRotation(7.0F, 22.0F, 10.0F, 0.0F, 0.0F, -0.12F));
        root.addOrReplaceChild("leg_br", CubeListBuilder.create()
                        .texOffs(103, 62).addBox(-3.0F, -13.0F, -3.0F, 6.0F, 13.0F, 6.0F, NONE),
                PartPose.offsetAndRotation(-7.0F, 22.0F, 10.0F, 0.0F, 0.0F, 0.12F));
        // ---- 四只**独立**的爪（盒高 2、局部 y 从 -2 到 0，基点 24 → 最低点 24）----
        //
        // ⚠ 原来前后各是**一块横跨左右的整板**（宽 19 / 20），读起来像"连体爪"，
        // 而且那块板的真实宽度与我手写的 UV 排布表对不上，导致 220 个纹素被两盒同抢。
        // 现在拆成 4 只，各自贴在对应腿的底端（腿在 x = ±6.5 / ±7）。
        root.addOrReplaceChild("paw_fl", CubeListBuilder.create()
                        .texOffs(142, 82).addBox(-2.5F, -2.0F, -3.5F, 5.0F, 2.0F, 7.0F, NONE),
                PartPose.offset(6.5F, 24.0F, -8.0F));
        root.addOrReplaceChild("paw_fr", CubeListBuilder.create()
                        .texOffs(167, 82).addBox(-2.5F, -2.0F, -3.5F, 5.0F, 2.0F, 7.0F, NONE),
                PartPose.offset(-6.5F, 24.0F, -8.0F));
        root.addOrReplaceChild("paw_bl", CubeListBuilder.create()
                        .texOffs(84, 82).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 2.0F, 8.0F, NONE),
                PartPose.offset(7.0F, 24.0F, 10.0F));
        root.addOrReplaceChild("paw_br", CubeListBuilder.create()
                        .texOffs(113, 82).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 2.0F, 8.0F, NONE),
                PartPose.offset(-7.0F, 24.0F, 10.0F));

        // ================= 颈 + 兽首 + 可开合的下颚 =================
        root.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(0, 62).addBox(-5.0F, -4.0F, -4.0F, 10.0F, 8.0F, 8.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 14.0F, -14.0F, -0.20F, 0.0F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(57, 39).addBox(-5.5F, -5.0F, -6.0F, 11.0F, 10.0F, 12.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 12.0F, -20.0F, 0.10F, 0.0F, 0.0F));
        // 下颚（会张合）
        root.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(37, 62).addBox(-4.5F, 0.0F, -5.5F, 9.0F, 3.0F, 11.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 12.5F, -21.0F, 0.18F, 0.0F, 0.0F));

        // 一对獠牙（从下颚往上翘）
        root.addOrReplaceChild("tusk_l", CubeListBuilder.create()
                        .texOffs(26, 100).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, NONE),
                PartPose.offsetAndRotation(3.5F, 13.0F, -25.0F, -0.45F, 0.0F, -0.20F));
        root.addOrReplaceChild("tusk_r", CubeListBuilder.create()
                        .texOffs(35, 100).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, NONE),
                PartPose.offsetAndRotation(-3.5F, 13.0F, -25.0F, -0.45F, 0.0F, 0.20F));

        // 一对兽角（从头顶向后弯）
        root.addOrReplaceChild("horn_l", CubeListBuilder.create()
                        .texOffs(0, 100).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 9.0F, 3.0F, NONE),
                PartPose.offsetAndRotation(4.0F, 7.5F, -20.0F, -0.50F, 0.0F, -0.35F));
        root.addOrReplaceChild("horn_r", CubeListBuilder.create()
                        .texOffs(13, 100).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 9.0F, 3.0F, NONE),
                PartPose.offsetAndRotation(-4.0F, 7.5F, -20.0F, -0.50F, 0.0F, 0.35F));

        // 一对眼睛（琥珀色兽瞳）
        root.addOrReplaceChild("eye_l", CubeListBuilder.create()
                        .texOffs(44, 100).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 1.0F, NONE),
                PartPose.offset(3.5F, 11.5F, -26.1F));
        root.addOrReplaceChild("eye_r", CubeListBuilder.create()
                        .texOffs(53, 100).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 1.0F, NONE),
                PartPose.offset(-3.5F, 11.5F, -26.1F));

        // ================= 背脊三排骨板 =================
        root.addOrReplaceChild("spine_plate1", CubeListBuilder.create()
                        .texOffs(42, 82).addBox(-1.0F, -6.0F, -4.0F, 2.0F, 6.0F, 8.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 9.0F, -11.0F, -0.30F, 0.0F, 0.0F));
        root.addOrReplaceChild("spine_plate2", CubeListBuilder.create()
                        .texOffs(21, 82).addBox(-1.0F, -7.0F, -4.0F, 2.0F, 7.0F, 8.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 8.0F, -3.0F, -0.10F, 0.0F, 0.0F));
        root.addOrReplaceChild("spine_plate3", CubeListBuilder.create()
                        .texOffs(63, 82).addBox(-1.0F, -6.0F, -4.0F, 2.0F, 6.0F, 8.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 9.0F, 5.0F, 0.15F, 0.0F, 0.0F));

        // ================= 长尾 + 尾尖 =================
        root.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(89, 0).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 22.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 13.0F, 17.0F, -0.12F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                        .texOffs(128, 62).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 10.0F, NONE),
                PartPose.offsetAndRotation(0.0F, 10.5F, 38.0F, -0.25F, 0.0F, 0.0F));

                root.addOrReplaceChild("plateA", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -2.4F, -2.0F, 4.0F, 2.4F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 4.0F, -8.0F));
        root.addOrReplaceChild("plateB", CubeListBuilder.create()
                        .texOffs(0, 10).addBox(-2.6F, -3.2F, -2.6F, 5.2F, 3.2F, 5.2F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 4.0F, -2.0F));
        root.addOrReplaceChild("plateC", CubeListBuilder.create()
                        .texOffs(46, 40).addBox(-2.2F, -2.8F, -2.2F, 4.4F, 2.8F, 4.4F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 4.0F, 4.0F));
        root.addOrReplaceChild("plateD", CubeListBuilder.create()
                        .texOffs(66, 0).addBox(-1.6F, -2.2F, -1.6F, 3.2F, 2.2F, 3.2F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 5.0F, 9.0F));
return LayerDefinition.create(mesh, 192, 128);
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

        // ---- 四足步态：对角腿同相（兽类小跑）----
        float step = Mth.cos(limbSwing * 0.6F) * limbSwingAmount;
        float stepDiag = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * limbSwingAmount;
        this.legFL.xRot = step * 0.7F;
        this.legBR.xRot = step * 0.7F;          // 对角同相
        this.legFR.xRot = stepDiag * 0.7F;
        this.legBL.xRot = stepDiag * 0.7F;

        // ---- 兽首：低头/抬头 + 左右张望（比人形幅度更大）----
        this.head.yRot = yaw * 0.9F;
        this.head.xRot = 0.10F + pitch * 0.8F;
        this.neck.yRot = yaw * 0.5F;
        this.neck.xRot = -0.20F + pitch * 0.35F;

        // ---- 下颚张合（呼吸 + 咆哮感）----
        float breath = Mth.sin(ageInTicks * 0.09F);
        this.jaw.xRot = 0.18F + breath * 0.12F;
        // 獠牙跟着下颚
        this.tuskL.xRot = -0.45F + breath * 0.10F;
        this.tuskR.xRot = -0.45F + breath * 0.10F;
        // 角跟着头
        this.hornL.yRot = this.head.yRot;
        this.hornR.yRot = this.head.yRot;
        this.eyeL.yRot = this.head.yRot;
        this.eyeR.yRot = this.head.yRot;
        this.eyeL.xRot = this.head.xRot;
        this.eyeR.xRot = this.head.xRot;

        // ---- 尾巴：大幅度左右甩（兽类情绪）----
        float swish = Mth.sin(ageInTicks * 0.10F);
        this.tail.yRot = swish * 0.35F;
        this.tail.xRot = -0.12F + Mth.sin(ageInTicks * 0.07F) * 0.08F;
        this.tailTip.yRot = Mth.sin(ageInTicks * 0.10F - 0.6F) * 0.45F;
        this.tailTip.xRot = -0.25F + Mth.sin(ageInTicks * 0.07F - 0.6F) * 0.10F;

        // ---- 背脊骨板随呼吸轻微起伏 ----
        this.plate1.xRot = -0.30F + breath * 0.05F;
        this.plate2.xRot = -0.10F + breath * 0.06F;
        this.plate3.xRot = 0.15F + breath * 0.05F;

        // ---- 躯干随呼吸起伏（巨兽的胸腔）----
        this.body.xRot = breath * 0.02F;
        this.hump.y = 9.0F - breath * 0.3F;
    
        float t = ageInTicks;
        this.plateA.xRot = Mth.sin(t * 0.12F) * 0.04F;
        this.plateB.xRot = Mth.sin(t * 0.12F + 0.6F) * 0.04F;
        this.plateC.xRot = Mth.sin(t * 0.12F + 1.2F) * 0.04F;
        this.plateD.xRot = Mth.sin(t * 0.12F + 1.8F) * 0.04F;
    }
}
