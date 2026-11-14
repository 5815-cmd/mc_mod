package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.ModEntitiesExtra;
import com.hongmeng.xiuxian.entity.ModEntitiesExtra2;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * <b>第二轮新增怪物 · 刷怪蛋</b>（与 {@link ModItems} 分开，避免多人改同一个共享文件）。
 *
 * <p>六颗蛋的配色按"这只怪第一眼看到的主色"取（详见
 * {@code 工程/角色设计/round2/新怪物规范.md}）。刷怪蛋全部进原版「刷怪蛋」页签
 * （{@link CreativeModeTabs#SPAWN_EGGS}），不进本模组创造栏——和原版口径一致。</p>
 *
 * <p>⚠ 必须用 {@link net.minecraftforge.common.ForgeSpawnEggItem}：原版
 * {@code SpawnEggItem} 的构造器要求已解析的 EntityType，而刷怪蛋和实体在同一个
 * RegisterEvent 里注册，提前 {@code .get()} 会直接让 mod 加载失败（这个坑踩过）。</p>
 */
public final class ModItemsExtra {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);

    /** 泽魅（云梦泽）。 */
    public static final RegistryObject<Item> MARSH_WRAITH_SPAWN_EGG =
            ITEMS.register("marsh_wraith_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra.MARSH_WRAITH, 0x2F4F4F, 0x9AE6D8, new Item.Properties()));

    /** 晶蛛（灵脉山谷）。 */
    public static final RegistryObject<Item> CRYSTAL_SPIDER_SPAWN_EGG =
            ITEMS.register("crystal_spider_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra.CRYSTAL_SPIDER, 0x1B3B52, 0x7FE3F2, new Item.Properties()));

    /** 兵俑（荒古战场）。 */
    public static final RegistryObject<Item> TERRACOTTA_SOLDIER_SPAWN_EGG =
            ITEMS.register("terracotta_soldier_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra.TERRACOTTA_SOLDIER, 0x9C6B45, 0x5A4A1E, new Item.Properties()));

    /** 稻草傀儡（纪元秘境）。 */
    public static final RegistryObject<Item> SCARECROW_PUPPET_SPAWN_EGG =
            ITEMS.register("scarecrow_puppet_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra.SCARECROW_PUPPET, 0xA88A3E, 0x4C5A2C, new Item.Properties()));

    /** 熔岩犬（下界）。 */
    public static final RegistryObject<Item> LAVA_HOUND_SPAWN_EGG =
            ITEMS.register("lava_hound_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra.LAVA_HOUND, 0x2A1108, 0xFF5A00, new Item.Properties()));

    /** 虚空水母（虚空秘境）。 */
    public static final RegistryObject<Item> VOID_JELLY_SPAWN_EGG =
            ITEMS.register("void_jelly_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra.VOID_JELLY, 0x2A1450, 0x7FE3F2, new Item.Properties()));

    // ---------------- 第二梯队 7 只 ----------------

    /** 终界使徒（末地）。 */
    public static final RegistryObject<Item> END_APOSTLE_SPAWN_EGG =
            ITEMS.register("end_apostle_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra2.END_APOSTLE, 0x120B22, 0xB060FF, new Item.Properties()));

    /** 堕仙（真世界）。 */
    public static final RegistryObject<Item> FALLEN_IMMORTAL_SPAWN_EGG =
            ITEMS.register("fallen_immortal_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra2.FALLEN_IMMORTAL, 0x2A1A3A, 0xE8D8A8, new Item.Properties()));

    /** 血蛭（血食泥沼）。 */
    public static final RegistryObject<Item> BLOOD_LEECH_SPAWN_EGG =
            ITEMS.register("blood_leech_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra2.BLOOD_LEECH, 0x3A0A10, 0xE83A4A, new Item.Properties()));

    /** 骨鹰（兽骨荒原）。 */
    public static final RegistryObject<Item> BONE_EAGLE_SPAWN_EGG =
            ITEMS.register("bone_eagle_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra2.BONE_EAGLE, 0xD8CFB8, 0x6B5A40, new Item.Properties()));

    /** 魔蛛（黑石魔窟）。 */
    public static final RegistryObject<Item> DEMON_SPIDER_SPAWN_EGG =
            ITEMS.register("demon_spider_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra2.DEMON_SPIDER, 0x1A1218, 0xFF5A00, new Item.Properties()));

    /** 焦骨骷髅（焦土魔原）。 */
    public static final RegistryObject<Item> CHARRED_SKELETON_SPAWN_EGG =
            ITEMS.register("charred_skeleton_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra2.CHARRED_SKELETON, 0x241C18, 0xFFB347, new Item.Properties()));

    /** 星尘游魂（星尘荒原）。 */
    public static final RegistryObject<Item> STAR_DUST_WRAITH_SPAWN_EGG =
            ITEMS.register("star_dust_wraith_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntitiesExtra2.STAR_DUST_WRAITH, 0x1A1030, 0xC8B8FF, new Item.Properties()));

    private ModItemsExtra() {
    }

    /** 由 {@code HongmengMod} 的构造器调用。 */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItemsExtra::onBuildCreativeTab);
    }

    /** 把六颗蛋放进原版「刷怪蛋」页签。 */
    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.SPAWN_EGGS) {
            return;
        }
        if (MARSH_WRAITH_SPAWN_EGG.isPresent()) {
            event.accept(MARSH_WRAITH_SPAWN_EGG.get());
        }
        if (CRYSTAL_SPIDER_SPAWN_EGG.isPresent()) {
            event.accept(CRYSTAL_SPIDER_SPAWN_EGG.get());
        }
        if (TERRACOTTA_SOLDIER_SPAWN_EGG.isPresent()) {
            event.accept(TERRACOTTA_SOLDIER_SPAWN_EGG.get());
        }
        if (SCARECROW_PUPPET_SPAWN_EGG.isPresent()) {
            event.accept(SCARECROW_PUPPET_SPAWN_EGG.get());
        }
        if (LAVA_HOUND_SPAWN_EGG.isPresent()) {
            event.accept(LAVA_HOUND_SPAWN_EGG.get());
        }
        if (VOID_JELLY_SPAWN_EGG.isPresent()) {
            event.accept(VOID_JELLY_SPAWN_EGG.get());
        }
        // 第二梯队 7 只
        if (END_APOSTLE_SPAWN_EGG.isPresent()) {
            event.accept(END_APOSTLE_SPAWN_EGG.get());
        }
        if (FALLEN_IMMORTAL_SPAWN_EGG.isPresent()) {
            event.accept(FALLEN_IMMORTAL_SPAWN_EGG.get());
        }
        if (BLOOD_LEECH_SPAWN_EGG.isPresent()) {
            event.accept(BLOOD_LEECH_SPAWN_EGG.get());
        }
        if (BONE_EAGLE_SPAWN_EGG.isPresent()) {
            event.accept(BONE_EAGLE_SPAWN_EGG.get());
        }
        if (DEMON_SPIDER_SPAWN_EGG.isPresent()) {
            event.accept(DEMON_SPIDER_SPAWN_EGG.get());
        }
        if (CHARRED_SKELETON_SPAWN_EGG.isPresent()) {
            event.accept(CHARRED_SKELETON_SPAWN_EGG.get());
        }
        if (STAR_DUST_WRAITH_SPAWN_EGG.isPresent()) {
            event.accept(STAR_DUST_WRAITH_SPAWN_EGG.get());
        }
    }
}
