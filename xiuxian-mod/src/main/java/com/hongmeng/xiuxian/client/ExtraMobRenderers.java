package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.ModEntitiesExtra;
import com.hongmeng.xiuxian.entity.ModEntitiesExtra2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * <b>第二轮新增怪物 · 客户端渲染接线</b>（独立文件，不往 {@code ClientSetup} 里塞）。
 *
 * <p>六只怪的渲染器在 {@link EntityRenderersEvent.RegisterRenderers} 里注册；
 * 五只新模型的模型层在 {@link EntityRenderersEvent.RegisterLayerDefinitions} 里注册
 * （熔岩犬复用灵狼的 {@code SpiritWolfRenderer.LAYER}，不需要新层）。</p>
 *
 * <p>把接线集中在这一个文件的好处：新怪物整条线（实体/刷怪蛋/渲染）都是**新增文件**，
 * 不动任何共享文件，六只一起加也不会和别人冲突。</p>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExtraMobRenderers {

    private ExtraMobRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MarshWraithModel.LAYER, MarshWraithModel::createBodyLayer);
        event.registerLayerDefinition(CrystalSpiderModel.LAYER, CrystalSpiderModel::createBodyLayer);
        event.registerLayerDefinition(TerracottaSoldierModel.LAYER, TerracottaSoldierModel::createBodyLayer);
        event.registerLayerDefinition(ScarecrowPuppetModel.LAYER, ScarecrowPuppetModel::createBodyLayer);
        event.registerLayerDefinition(VoidJellyModel.LAYER, VoidJellyModel::createBodyLayer);
        // ---- 第二梯队 7 只 ----
        event.registerLayerDefinition(EndApostleModel.LAYER, EndApostleModel::createBodyLayer);
        event.registerLayerDefinition(FallenImmortalModel.LAYER,
                FallenImmortalModel::createBodyLayer);
        event.registerLayerDefinition(BloodLeechModel.LAYER, BloodLeechModel::createBodyLayer);
        event.registerLayerDefinition(BoneEagleModel.LAYER, BoneEagleModel::createBodyLayer);
        event.registerLayerDefinition(DemonSpiderModel.LAYER, DemonSpiderModel::createBodyLayer);
        event.registerLayerDefinition(CharredSkeletonModel.LAYER,
                CharredSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(StarDustWraithModel.LAYER,
                StarDustWraithModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntitiesExtra.MARSH_WRAITH.get(), MarshWraithRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra.CRYSTAL_SPIDER.get(), CrystalSpiderRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra.TERRACOTTA_SOLDIER.get(),
                TerracottaSoldierRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra.SCARECROW_PUPPET.get(),
                ScarecrowPuppetRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra.LAVA_HOUND.get(), LavaHoundRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra.VOID_JELLY.get(), VoidJellyRenderer::new);
        // ---- 第二梯队 7 只 ----
        event.registerEntityRenderer(ModEntitiesExtra2.END_APOSTLE.get(), EndApostleRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra2.FALLEN_IMMORTAL.get(),
                FallenImmortalRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra2.BLOOD_LEECH.get(), BloodLeechRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra2.BONE_EAGLE.get(), BoneEagleRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra2.DEMON_SPIDER.get(), DemonSpiderRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra2.CHARRED_SKELETON.get(),
                CharredSkeletonRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra2.STAR_DUST_WRAITH.get(),
                StarDustWraithRenderer::new);
    }
}
