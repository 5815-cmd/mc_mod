package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.ModEntitiesExtra3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * <b>第四轮 · 天外天新内容 · 客户端渲染接线</b>（独立文件，不往 {@code ClientSetup} 里塞）。
 *
 * <p>仙庭之主（巨型 Boss）/ 天兵 / 云鹤：3 个渲染器 + 3 个模型层，全部在这里注册。</p>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HeavenMobRenderers {

    private HeavenMobRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HeavenLordModel.LAYER, HeavenLordModel::createBodyLayer);
        event.registerLayerDefinition(HeavenSoldierModel.LAYER, HeavenSoldierModel::createBodyLayer);
        event.registerLayerDefinition(CloudCraneModel.LAYER, CloudCraneModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntitiesExtra3.HEAVEN_LORD.get(), HeavenLordRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra3.HEAVEN_SOLDIER.get(),
                HeavenSoldierRenderer::new);
        event.registerEntityRenderer(ModEntitiesExtra3.CLOUD_CRANE.get(), CloudCraneRenderer::new);
    }
}
