package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 门派长老的专属渲染器（第二轮 NPC 身份化 A）。
 *
 * <p>不再共用 {@link CultivationNpcRenderer} 的「一套骨架 + Variant」做法，而是走
 * {@link SectElderModel} 的<b>独立模型</b>——长白须、拂尘、无锋木剑、道髻全都在
 * {@code createBodyLayer} 里以原版人形骨架为父件长出来，靠一张 64×64 贴图采样
 * （模型/UV/贴图由 {@code 工程/角色设计/round2/npc-a/sect_elder/spec.py} 同一张表生成）。</p>
 *
 * <p>本类只接管 {@code ModEntities.SECT_ELDER} 那一条注册。</p>
 */
@OnlyIn(Dist.CLIENT)
public class SectElderRenderer
        extends HumanoidMobRenderer<CultivationNpcEntity, SectElderModel> {

    /** 贴图固定一张：门派长老只有一种外观（换外观就该换模型，不是换贴图）。 */
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/sect_elder.png");

    public SectElderRenderer(EntityRendererProvider.Context context) {
        super(context, new SectElderModel(context.bakeLayer(SectElderModel.LAYER)),
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
