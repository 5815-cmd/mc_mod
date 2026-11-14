package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 悬赏牌看板 · BountyBoard / BountyBoardModel
 *
 * <p>由 `工程/角色设计/tools/studio.py` 从 Spec 生成：
 * 模型与贴图共用同一张 UV 表，`pack_uv` 已逐纹素验证 0 重叠 / 0 越界。</p>
 * <p>两根木柱 + 12×12 大告示牌 + 小瓦檐 + 檐角铜铃；牌面钉 3 张悬赏告示（随风轻飘），脚下 2 块压脚木。唯一的非人形 NPC 剪影。</p>
 */
public class BountyBoardModel extends EntityModel<CultivationNpcEntity> {

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "bounty_board"), "main");

    private final ModelPart root;
    private final ModelPart frame;
    private final ModelPart bell;
    private final ModelPart noticeA;
    private final ModelPart noticeB;
    private final ModelPart noticeC;
    private final ModelPart pinned;

    public BountyBoardModel(ModelPart root) {
        this.root = root;
        this.frame = root.getChild("frame");
        this.bell = root.getChild("frame").getChild("bell");
        this.noticeA = root.getChild("frame").getChild("notice_a");
        this.noticeB = root.getChild("frame").getChild("notice_b");
        this.noticeC = root.getChild("frame").getChild("notice_c");
        this.pinned = root.getChild("frame").getChild("pinned");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition frame = root.addOrReplaceChild("frame",
CubeListBuilder.create()
        .texOffs(29, 0).addBox(3.5F, -12.0F, 1.0F, 2.0F, 12.0F, 2.0F, NONE)
        .texOffs(38, 0).addBox(-5.5F, -12.0F, 1.0F, 2.0F, 12.0F, 2.0F, NONE)
        .texOffs(47, 13).addBox(2.5F, -1.0F, 0.0F, 3.0F, 1.0F, 3.0F, NONE)
        .texOffs(37, 18).addBox(-5.5F, -1.0F, 0.0F, 3.0F, 1.0F, 3.0F, NONE)
        .texOffs(0, 0).addBox(-6.0F, -24.0F, -1.0F, 12.0F, 12.0F, 2.0F, NONE)
        .texOffs(0, 15).addBox(-7.0F, -26.0F, -2.0F, 14.0F, 2.0F, 4.0F, NONE)
        .texOffs(47, 7).addBox(-1.0F, -29.0F, -1.0F, 2.0F, 3.0F, 2.0F, NONE)
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition bell = frame.addOrReplaceChild("bell",
CubeListBuilder.create()
        .texOffs(60, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, NONE)
        .texOffs(50, 18).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 2.0F, 2.0F, NONE)
                , PartPose.offset(6.5F, -24.0F, 0.0F));
        PartDefinition noticeA = frame.addOrReplaceChild("notice_a",
CubeListBuilder.create()
        .texOffs(47, 0).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(-3.5F, -21.0F, -1.5F));
        PartDefinition noticeB = frame.addOrReplaceChild("notice_b",
CubeListBuilder.create()
        .texOffs(56, 0).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 5.0F, 1.0F, NONE)
                , PartPose.offset(0.5F, -22.0F, -1.5F));
        PartDefinition noticeC = frame.addOrReplaceChild("notice_c",
CubeListBuilder.create()
        .texOffs(56, 7).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 4.0F, 1.0F, NONE)
                , PartPose.offset(3.5F, -20.0F, -1.5F));
        PartDefinition pinned = frame.addOrReplaceChild("pinned",
CubeListBuilder.create()
        .texOffs(0, 22).addBox(-4.0F, -15.0F, -2.0F, 8.0F, 2.0F, 1.0F, NONE)
        .texOffs(37, 15).addBox(-4.0F, -21.0F, -2.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(42, 15).addBox(0.0F, -22.0F, -2.5F, 1.0F, 1.0F, 1.0F, NONE)
        .texOffs(60, 17).addBox(3.0F, -20.0F, -2.5F, 1.0F, 1.0F, 1.0F, NONE)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
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
    public void setupAnim(CultivationNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // 动作总览：
        //   idle   —— 三张悬赏告示被风吹得轻轻翻动（不同相位），铜铃小幅摆动
        //   walk   —— 悬赏牌不会走路（实体 canWander=false），这里当"被推着晃"处理
        //   talk   —— 每 200 tick 铜铃响一趟（铃摆加大）
        //   hurt   —— 整块牌子抖一下、告示乱飘
        //   death  —— 牌身倾倒（倒地旋转交给原版渲染器），告示垂下、铃停
        // 全部绝对赋值，没有任何 += / *=。
        float t = ageInTicks;

        // ---- 1. 三张告示：不同相位/幅度地翻动（绕上沿铜钉转）--------------
        float wind = Mth.sin(t * 0.05F);
        this.noticeA.xRot = NOTICE_BASE_XROT + wind * 0.06F;
        this.noticeB.xRot = NOTICE_BASE_XROT + Mth.sin(t * 0.05F + 1.9F) * 0.05F;
        this.noticeC.xRot = NOTICE_BASE_XROT + Mth.sin(t * 0.05F + 3.4F) * 0.07F;
        this.noticeA.zRot = Mth.sin(t * 0.04F) * 0.02F;
        this.noticeB.zRot = Mth.sin(t * 0.04F + 1.2F) * 0.02F;

        // ---- 2. 铜铃：平时小幅摆；每 200 tick 前 30 tick 响铃（摆幅加大）--
        boolean ringing = (t % 200.0F) < 30.0F;
        float ringAmp = ringing ? 0.42F : 0.06F;
        this.bell.zRot = Mth.sin(t * (ringing ? 0.9F : 0.09F)) * ringAmp;
        this.bell.xRot = Mth.cos(t * (ringing ? 0.7F : 0.07F)) * ringAmp * 0.5F;

        // ---- 3. hurt：整块牌子抖一下、告示乱飘 -----------------------------
        if (entity.hurtTime > 0) {
            float hurt = 1.0F - entity.hurtTime / 10.0F;
            this.frame.zRot = hurt * 0.06F;
            this.noticeA.xRot = this.noticeA.xRot + hurt * 0.35F;
            this.noticeC.xRot = this.noticeC.xRot + hurt * 0.30F;
            this.bell.zRot = this.bell.zRot + hurt * 0.5F;
        } else {
            this.frame.zRot = 0.0F;
        }

        // ---- 4. death：告示垂下、铃停（倒地旋转由原版渲染器完成）----------
        if (entity.isDeadOrDying()) {
            this.noticeA.xRot = 0.0F;
            this.noticeB.xRot = 0.0F;
            this.noticeC.xRot = 0.0F;
            this.bell.zRot = 0.0F;
            this.bell.xRot = 0.0F;
        }

    }


    /** 模型最低点（两根立柱 + 压脚木的底面）= 24 → 渲染器 GROUND_LIFT = 0。 */
    public static final int LOWEST_PIXEL_Y = 24;

    /** 最高点：檐顶小方柱 -5 → 总高 29px（hitbox 1.80 = 28.8px）。 */
    public static final int HIGHEST_PIXEL_Y = -5;

    /** 告示纸的基准俯仰（rad，负=往外翘，避免转进牌面）：动画里的绝对基准。 */
    public static final float NOTICE_BASE_XROT = -0.03F;

}
