package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * <b>护甲外挂件图层</b>：三层肩甲 + 腰甲 + 腿甲 + 披风。
 *
 * <h2>为什么用 RenderLayer 而不是护甲模型</h2>
 * <p>护甲模型只能用<b>那套甲自己的贴图</b>，所以外挂件永远是纯色。
 * {@code RenderLayer} 是挂在玩家渲染器上的独立图层，可以：
 * <ul>
 *   <li>用<b>自己的贴图</b>（{@code hongmeng_trim.png}，带金边/铆钉/宝石花纹）；</li>
 *   <li>用<b>自己的骨架</b>（跟着玩家模型的手臂/腿）；</li>
 *   <li>只在穿着本模组胸甲时渲染。</li>
 * </ul>
 *
 * <h2>骨骼绑定</h2>
 * <p>图层拿到的是玩家模型本体，直接把外挂件的变换<b>接到对应骨骼的变换上</b>：
 * 先 {@code translate/rotate} 到手臂位置，再画肩甲 —— 这样抬手/走路都会跟着动。</p>
 */
public class ArmorTrimLayer extends RenderLayer<AbstractClientPlayer,
        PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(HongmengMod.MOD_ID, "armor_trim"), "main");

    private static final ResourceLocation TEX = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/models/armor/hongmeng_trim.png");

    private final ModelPart pauldronR;
    private final ModelPart pauldronL;
    private final ModelPart waist;
    private final ModelPart thighR;
    private final ModelPart thighL;
    private final ModelPart cape;

    public ArmorTrimLayer(RenderLayerParent<AbstractClientPlayer,
            PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        // 打包好的部件树（根节点下直接是各外挂件）
        var models = net.minecraft.client.Minecraft.getInstance().getEntityModels();
        ModelPart root = models.bakeLayer(LAYER);
        this.pauldronR = root.getChild("pauldron_r");
        this.pauldronL = root.getChild("pauldron_l");
        this.waist = root.getChild("waist");
        this.thighR = root.getChild("thigh_r");
        this.thighL = root.getChild("thigh_l");
        this.cape = root.getChild("cape");
    }

    /** 骨架：全部以原点为基准，渲染时再平移到骨骼位置。 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();

        // 三层肩甲（每层更小更外更低）
        root.addOrReplaceChild("pauldron_r", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.6F, -2.6F, -2.6F, 5.2F, 2.0F, 5.2F)
                        .texOffs(0, 0).addBox(-5.0F, -1.0F, -2.2F, 5.8F, 1.6F, 4.4F)
                        .texOffs(0, 0).addBox(-5.4F, 0.3F, -1.8F, 6.2F, 1.4F, 3.6F),
                PartPose.ZERO);
        root.addOrReplaceChild("pauldron_l", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.6F, -2.6F, -2.6F, 5.2F, 2.0F, 5.2F)
                        .texOffs(0, 0).addBox(-0.8F, -1.0F, -2.2F, 5.8F, 1.6F, 4.4F)
                        .texOffs(0, 0).addBox(-0.8F, 0.3F, -1.8F, 6.2F, 1.4F, 3.6F),
                PartPose.ZERO);

        // 腰甲：前后裙甲 + 腰带
        root.addOrReplaceChild("waist", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-4.6F, 0.0F, -2.8F, 9.2F, 3.2F, 1.2F)
                        .texOffs(32, 0).addBox(-4.6F, 0.0F, 1.6F, 9.2F, 3.2F, 1.2F)
                        .texOffs(0, 0).addBox(-4.8F, -1.0F, -2.9F, 9.6F, 1.2F, 5.8F),
                PartPose.ZERO);

        // 腿甲
        root.addOrReplaceChild("thigh_r", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-2.8F, 0.0F, -2.4F, 1.2F, 6.5F, 4.8F),
                PartPose.ZERO);
        root.addOrReplaceChild("thigh_l", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(1.6F, 0.0F, -2.4F, 1.2F, 6.5F, 4.8F),
                PartPose.ZERO);

        // 披风（薄盒 + 下摆穗饰）
        root.addOrReplaceChild("cape", CubeListBuilder.create()
                        .texOffs(48, 16).addBox(-4.4F, 0.0F, 0.0F, 8.8F, 13.0F, 0.8F)
                        .texOffs(0, 0).addBox(-4.6F, 13.0F, 0.0F, 9.2F, 1.4F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light,
                       AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!wearingOurChest(player)) {
            return;
        }
        var model = this.getParentModel();
        var vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX));

        // ---- 肩甲：接到手臂骨骼的变换上 ----
        pose.pushPose();
        applyArm(pose, model, true);
        this.pauldronR.render(pose, vc, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();

        pose.pushPose();
        applyArm(pose, model, false);
        this.pauldronL.render(pose, vc, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();

        // ---- 腰甲：接到躯干 ----
        pose.pushPose();
        applyBody(pose, model);
        this.waist.render(pose, vc, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        // ---- 披风：跟在躯干后，随步频摆 ----
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        this.cape.xRot = 0.06F + Mth.cos(limbSwing * 0.6F) * 0.10F * walk
                + Mth.sin(ageInTicks * 0.06F) * 0.03F;
        this.cape.render(pose, vc, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();

        // ---- 腿甲 ----
        pose.pushPose();
        applyLeg(pose, model, true);
        this.thighR.render(pose, vc, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        applyLeg(pose, model, false);
        this.thighL.render(pose, vc, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** 把当前矩阵移到右/左手臂（含摆动）。 */
    private static void applyArm(PoseStack pose, HumanoidModel<?> model, boolean right) {
        ModelPart arm = right ? model.rightArm : model.leftArm;
        pose.translate(right ? -0.36D : 0.36D, 1.40D, 0.0D);
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(arm.zRot));
        pose.mulPose(com.mojang.math.Axis.XP.rotation(arm.xRot));
        pose.mulPose(com.mojang.math.Axis.YP.rotation(arm.yRot));
    }

    private static void applyBody(PoseStack pose, HumanoidModel<?> model) {
        pose.mulPose(com.mojang.math.Axis.YP.rotation(model.body.yRot));
        pose.mulPose(com.mojang.math.Axis.XP.rotation(model.body.xRot));
        pose.translate(0.0D, 0.0D, 0.0D);
    }

    private static void applyLeg(PoseStack pose, HumanoidModel<?> model, boolean right) {
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        pose.translate(right ? -0.12D : 0.12D, 0.72D, 0.0D);
        pose.mulPose(com.mojang.math.Axis.XP.rotation(leg.xRot));
        pose.mulPose(com.mojang.math.Axis.YP.rotation(leg.yRot));
    }

    /** 只对穿本模组胸甲的玩家生效。 */
    private static boolean wearingOurChest(AbstractClientPlayer player) {
        ItemStack chest = player.getInventory().getArmor(2);
        if (chest.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(chest.getItem());
        if (id == null || !HongmengMod.MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return path.startsWith("dao_armor_") || path.startsWith("path_armor_");
    }
}
