package com.hongmeng.xiuxian.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * 所有 GeckoLib 武器共用的持握渲染器（按武器类型分姿势）。
 *
 * <p>长剑：单手底座（沿用实测定稿）；其余五种武器：第三人称/第一人称都改成
 * "双手持握"的居中式姿势，并按类型单独调位置/角度/大小。</p>
 */
public class GeoWeaponRenderer<T extends Item & GeoItem> extends GeoItemRenderer<T> {

    // ===== 长剑（单手，定稿数值） =====
    private static final float[] POSE_SWORD_TP = {0.50F, 0.20F, 0.60F, 15.0F, -95.0F, 0.55F};
    private static final float[] POSE_SWORD_FP = {0.65F, 0.40F, 0.50F, -15.0F, 80.0F, 0.42F};

    // ===== 其余五种：双手居中式 =====
    private static final float[] POSE_GREATSWORD_TP = {0.30F, 0.08F, 0.72F, 22.0F, -95.0F, 0.50F};
    // 巨剑第一人称：整体抬高（之前"靠下拿"）
    private static final float[] POSE_GREATSWORD_FP = {0.52F, 0.16F, 0.55F, -6.0F, 90.0F, 0.36F};

    private static final float[] POSE_SCYTHE_TP = {0.32F, 0.05F, 0.74F, 10.0F, -90.0F, 0.48F};
    private static final float[] POSE_SCYTHE_FP = {0.50F, 0.24F, 0.58F, -8.0F, 90.0F, 0.34F};

    private static final float[] POSE_SPEAR_TP = {0.24F, 0.10F, 0.70F, 22.0F, -92.0F, 0.50F};
    private static final float[] POSE_SPEAR_FP = {0.50F, 0.22F, 0.60F, -8.0F, 90.0F, 0.36F};

    private static final float[] POSE_STAFF_TP = {0.28F, 0.10F, 0.70F, 16.0F, -92.0F, 0.50F};
    private static final float[] POSE_STAFF_FP = {0.50F, 0.22F, 0.60F, -8.0F, 90.0F, 0.36F};

    private static final float[] POSE_WARSCYTHE_TP = {0.26F, 0.10F, 0.72F, 18.0F, -90.0F, 0.50F};
    private static final float[] POSE_WARSCYTHE_FP = {0.50F, 0.22F, 0.60F, -8.0F, 90.0F, 0.36F};

    /** GeckoLib {@code preRender()} 里那句 translate 的反向补偿。 */
    private static final float CANCEL_X = -0.5F;
    private static final float CANCEL_Y = -0.51F;
    private static final float CANCEL_Z = -0.5F;

    private float modelScale = 1.0F;
    private float guiLengthUnits = 45.0F;
    private String form = "";

    public GeoWeaponRenderer(GeoModel<T> model) {
        super(model);
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** 只为这一把武器单独缩放。 */
    public GeoWeaponRenderer<T> withModelScale(float scale) {
        this.modelScale = scale;
        return this;
    }

    /** 物品栏（GUI）按模型长度自动缩到格子内。 */
    public GeoWeaponRenderer<T> withGuiLength(float lengthUnits) {
        this.guiLengthUnits = Math.max(8.0F, lengthUnits);
        return this;
    }

    /**
     * 武器类型：sword / greatsword / scythe / spear / staff / war_scythe。
     * <p>非长剑类型会使用"双手居中"姿势；镰刀还会在第三人称单独修正朝向。</p>
     */
    public GeoWeaponRenderer<T> withForm(String weaponForm) {
        this.form = weaponForm == null ? "" : weaponForm;
        return this;
    }

    private float[] tpPose() {
        // 双手持握方案已按玩家要求回退：所有武器统一用原来的单手底座
        return POSE_SWORD_TP;
    }

    private float[] fpPose() {
        return POSE_SWORD_FP;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();

        switch (ctx) {
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                float[] p = tpPose();
                if ("scythe".equals(this.form)) {
                    // 镰刀：第三人称反向、第一人称正常——这里单独把第三人称的自转反向
                    p = new float[]{p[0], p[1], p[2], p[3], -p[4], p[5]};
                }
                applyPose(pose, p[0], p[1], p[2], p[3], p[4], p[5] * this.modelScale, false);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                float[] p = fpPose();
                boolean flip = "scythe".equals(this.form);
                applyPose(pose, p[0], p[1], p[2], p[3], p[4], p[5] * this.modelScale, flip);
            }
            case GUI -> {
                float fit = 0.86F * 16.0F / this.guiLengthUnits;
                float halfBlocks = (this.guiLengthUnits / 16.0F) * 0.5F * fit;
                double ang = Math.toRadians(-38.0);
                pose.translate((float) (Math.sin(ang) * halfBlocks),
                        (float) (-Math.cos(ang) * halfBlocks), 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(-38.0F));
                pose.scale(fit, fit, fit);
            }
            case FIXED -> {
                float fit = 0.9F * 16.0F / this.guiLengthUnits;
                pose.scale(fit, fit, fit);
            }
            default -> {
            }
        }

        pose.translate(CANCEL_X, CANCEL_Y, CANCEL_Z);
        super.renderByItem(stack, ctx, pose, buffer, light, overlay);
        pose.popPose();
    }

    private static void applyPose(PoseStack pose, float x, float y, float z,
                                  float rotX, float rotY, float scale,
                                  boolean flipYaw) {
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotationDegrees(rotX));
        pose.mulPose(Axis.YP.rotationDegrees(rotY));
        if (flipYaw) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        pose.scale(scale, scale, scale);
    }
}
