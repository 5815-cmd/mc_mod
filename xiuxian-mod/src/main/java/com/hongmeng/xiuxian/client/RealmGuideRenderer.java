package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 秘境向导的专属渲染器（第二轮 NPC 身份化 B）。
 *
 * <p>独立模型 {@link RealmGuideModel}：清瘦风尘 + 窄檐旧草斗笠 + 背后竹卷轴筒
 * + 左手两格高路引竹竿（竿顶挂路引纸）+ 右腰水囊。贴图 64×64。</p>
 */
@OnlyIn(Dist.CLIENT)
public class RealmGuideRenderer
        extends HumanoidMobRenderer<CultivationNpcEntity, RealmGuideModel> {

    /** 贴图固定一张。 */
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/realm_guide.png");

    public RealmGuideRenderer(EntityRendererProvider.Context context) {
        super(context, new RealmGuideModel(context.bakeLayer(RealmGuideModel.LAYER)), 0.32F);
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationNpcEntity entity) {
        return TEXTURE;
    }

    /** NPC 的名牌一直挂着：玩家得知道这位能指路。 */
    @Override
    protected boolean shouldShowName(CultivationNpcEntity entity) {
        return true;
    }
}
