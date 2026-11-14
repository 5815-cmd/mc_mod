package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 悬赏牌看板的专属渲染器（第二轮 NPC 身份化 B）——
 * <b>非人形</b>：一根木架 + 一块大告示牌 + 小瓦檐 + 铜铃。
 *
 * <h2>离地修正</h2>
 * <p>原版画模型前 {@code translate(0, -1.501, 0)}（≈24.016 像素）→
 * <b>模型最低点必须停在 y=24</b>。{@link BountyBoardModel} 的立柱底面正好在
 * {@code y=24}，所以修正量为 <b>0</b>（公式保留，万一以后改模型高度不用再想）。</p>
 *
 * <p>⚠ 修正放在 {@code setupRotations} 里（放 {@code scale} 里会和动画叠加）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BountyBoardRenderer
        extends MobRenderer<CultivationNpcEntity, BountyBoardModel> {

    /** 离地修正量（格）。= (模型最低点像素 y − 24) / 16。 */
    private static final double GROUND_LIFT = (BountyBoardModel.LOWEST_PIXEL_Y - 24) / 16.0D;

    /** 贴图固定一张：悬赏牌只有一种外观。 */
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/bounty_board.png");

    public BountyBoardRenderer(EntityRendererProvider.Context context) {
        super(context, new BountyBoardModel(context.bakeLayer(BountyBoardModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationNpcEntity entity) {
        return TEXTURE;
    }

    /** 一块会说话的牌：名牌一直挂着，玩家才知道能右键接悬赏。 */
    @Override
    protected boolean shouldShowName(CultivationNpcEntity entity) {
        return true;
    }

    @Override
    protected void setupRotations(CultivationNpcEntity entity, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        poseStack.translate(0.0D, GROUND_LIFT, 0.0D);
    }
}
