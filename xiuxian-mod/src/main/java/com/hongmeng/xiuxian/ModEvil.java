package com.hongmeng.xiuxian;

import com.hongmeng.xiuxian.block.EvilAltarBlockEntity;
import com.hongmeng.xiuxian.menu.EvilAltarMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * <b>恶之祭坛</b>的注册入口：方块实体类型 + 界面类型。
 *
 * <p>界面必须用 {@link IForgeMenuType#create} 建 —— 只有它才会把
 * {@code player.openMenu(be, pos)} 里的坐标写进网络缓冲区，
 * 客户端才能查回方块实体。用普通 {@code new MenuType<>} 的话，
 * 客户端打开界面会是四个空槽且永远不同步。</p>
 */
public final class ModEvil {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HongmengMod.MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, HongmengMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<EvilAltarBlockEntity>> EVIL_ALTAR =
            BLOCK_ENTITIES.register("evil_altar", () -> BlockEntityType.Builder
                    .of(EvilAltarBlockEntity::new, ModBlocks.EVIL_ALTAR.get())
                    .build(null));

    public static final RegistryObject<MenuType<EvilAltarMenu>> EVIL_ALTAR_MENU =
            MENUS.register("evil_altar", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new EvilAltarMenu(id, inv, buf)));

    private ModEvil() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }
}
