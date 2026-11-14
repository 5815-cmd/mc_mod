package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 炼器师的专属渲染器（第二轮 NPC 身份化 B）。
 *
 * <p>独立模型 {@link ArtifactRefinerModel}：赤膊皮围裙 + 5px 宽皮护肩（NPC 里最壮）
 * + 背后两柄交叉剑胚 + 右手铁锤 + 额前皮头带。贴图 64×64，与其余人形 NPC 不共用。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ArtifactRefinerRenderer
        extends HumanoidMobRenderer<CultivationNpcEntity, ArtifactRefinerModel> {

    /** 贴图固定一张：铁匠只有一种外观。 */
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/artifact_refiner.png");

    public ArtifactRefinerRenderer(EntityRendererProvider.Context context) {
        super(context, new ArtifactRefinerModel(context.bakeLayer(ArtifactRefinerModel.LAYER)),
                0.36F);
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationNpcEntity entity) {
        return TEXTURE;
    }

    /** NPC 的名牌一直挂着：玩家得知道这位能修装备。 */
    @Override
    protected boolean shouldShowName(CultivationNpcEntity entity) {
        return true;
    }
}
