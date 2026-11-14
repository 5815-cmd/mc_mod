package com.hongmeng.xiuxian.alchemy;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 炼丹系统的注册入口：方块实体类型 + 界面类型。
 *
 * <p>在 {@code HongmengMod} 的构造里调用。这里做两件事：</p>
 * <ol>
 *   <li>注册 {@code hongmeng:alchemy_furnace} 的方块实体与界面；</li>
 *   <li>调 {@link AlchemyRecipes#validate()} —— <b>配方表坏了要让模组直接加载失败</b>，
 *       而不是等玩家把材料丢进丹炉才发现炼不出来。校验是纯字符串运算，
 *       不碰注册表，所以放在构造期是安全的。</li>
 * </ol>
 *
 * <p>{@code BlockEntityType.Builder.of(...)} 里引用
 * {@code ModBlocks.ALCHEMY_FURNACE.get()}：方块实体注册事件排在方块之后，
 * 所以这个 {@code get()} 在注册期已经是有效对象（和 {@code ModItems} 里
 * {@code new BlockItem(ModBlocks.X.get(), ...)} 是同一个口径）。</p>
 */
public final class ModAlchemy {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HongmengMod.MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, HongmengMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<AlchemyFurnaceBlockEntity>> ALCHEMY_FURNACE_BE =
            BLOCK_ENTITIES.register("alchemy_furnace", () -> BlockEntityType.Builder
                    .of(AlchemyFurnaceBlockEntity::new, ModBlocks.ALCHEMY_FURNACE.get())
                    .build(null));

    public static final RegistryObject<MenuType<AlchemyFurnaceMenu>> ALCHEMY_FURNACE_MENU =
            MENUS.register("alchemy_furnace", () -> new MenuType<>(
                    AlchemyFurnaceMenu::new, FeatureFlags.VANILLA_SET));

    private ModAlchemy() {
    }

    public static void register(IEventBus bus) {
        // 丹方自检：漏一颗丹药、材料拼错、同一对材料两条方子——全部在这里炸出来
        AlchemyRecipes.validate();
        // 灵植表自检：品阶必须 1..6 各一个、生长概率必须在 (0,1]
        HerbCatalog.validate();
        // 雷劫概率表自检：四档之和必须正好 100%、"五个"必须是 20%、
        // 最高品阶那一档必须真的存在丹方（不然雷劫永远不触发）
        AlchemyTribulation.validate();
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }
}
