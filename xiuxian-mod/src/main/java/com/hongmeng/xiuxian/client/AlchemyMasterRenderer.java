package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 炼丹师的专属渲染器（第二轮 NPC 身份化 A）。
 *
 * <p>不再共用 {@link CultivationNpcRenderer} 的「一套骨架 + Variant」做法，而是走
 * {@link AlchemyMasterModel} 的<b>独立模型</b>——腰间丹葫芦、木药匣、熏黑袖口、
 * 左手托的青铜丹炉盖全都在 {@code createBodyLayer} 里以原版人形骨架为父件长出来，
 * 靠一张 64×64 贴图采样（模型/UV/贴图由
 * {@code 工程/角色设计/round2/npc-a/alchemy_master/spec.py} 同一张表生成）。</p>
 *
 * <p>本类只接管 {@code ModEntities.ALCHEMY_MASTER} 那一条注册。</p>
 */
@OnlyIn(Dist.CLIENT)
public class AlchemyMasterRenderer
        extends HumanoidMobRenderer<CultivationNpcEntity, AlchemyMasterModel> {

    /** 贴图固定一张：炼丹师只有一种外观（换外观就该换模型，不是换贴图）。 */
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/alchemy_master.png");

    public AlchemyMasterRenderer(EntityRendererProvider.Context context) {
        super(context, new AlchemyMasterModel(context.bakeLayer(AlchemyMasterModel.LAYER)),
                0.34F);
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationNpcEntity entity) {
        return TEXTURE;
    }

    /** NPC 的名牌一直挂着：玩家得知道这位能说话。 */
    @Override
    protected boolean shouldShowName(CultivationNpcEntity entity) {
        return true;
    }
}
