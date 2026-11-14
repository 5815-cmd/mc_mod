package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 云游商人的专属渲染器（试稿③）。
 *
 * <p>和其余 6 位 NPC 不同：他不再共用 {@link CultivationNpcRenderer} 的
 * "一套骨架 + Variant" 那套做法，而是走 {@link WanderingMerchantModel} 的
 * <b>独立模型</b>——斗笠、竹扁担、前后两只货筐、褡裢、竹杖全都在
 * {@code createBodyLayer} 里以原版人形骨架为父件长出来，靠一张 64×64 贴图
 * 采样（模型/UV/贴图由 {@code 工程/角色设计/03-云游商人/spec.py} 同一张表生成）。</p>
 *
 * <p>其余 6 位 NPC 仍由 {@link CultivationNpcRenderer} 负责，本类只接管
 * {@code ModEntities.WANDERING_MERCHANT} 那一条注册。</p>
 */
@OnlyIn(Dist.CLIENT)
public class WanderingMerchantRenderer
        extends HumanoidMobRenderer<CultivationNpcEntity, WanderingMerchantModel> {

    /** 贴图固定一张：云游商人只有一种外观（换外观就该换模型，不是换贴图）。 */
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/wandering_merchant.png");

    public WanderingMerchantRenderer(EntityRendererProvider.Context context) {
        super(context, new WanderingMerchantModel(context.bakeLayer(WanderingMerchantModel.LAYER)),
                0.34F);
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationNpcEntity entity) {
        return TEXTURE;
    }

    /** NPC 的名牌一直挂着：玩家得知道这位能说话、能开坊市。 */
    @Override
    protected boolean shouldShowName(CultivationNpcEntity entity) {
        return true;
    }
}
