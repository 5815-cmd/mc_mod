package com.hongmeng.xiuxian.alchemy;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 丹炉界面的客户端注册。
 *
 * <p><b>这个类只会在客户端被加载</b>：唯一引用它的是
 * {@code HongmengMod} 里 {@code FMLEnvironment.dist == Dist.CLIENT} 分支内的
 * {@code modBus.addListener(...)}。专用服务端不会走到那一行，
 * 也就不会去解析 {@code net.minecraft.client.*}——这是这个模组一贯的做法
 * （见 {@code ClientSetup}）。</p>
 *
 * <p>Forge 1.20.1 <b>没有</b> {@code RegisterMenuScreensEvent}（那是 1.20.2+ 才有的），
 * 界面注册只能在 {@link FMLClientSetupEvent} 里 {@code enqueueWork} 做。</p>
 */
public final class AlchemyClientSetup {

    private AlchemyClientSetup() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(
                ModAlchemy.ALCHEMY_FURNACE_MENU.get(), AlchemyFurnaceScreen::new));
    }
}
