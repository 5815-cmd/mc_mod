package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.item.armor.ModArmor;
import com.hongmeng.xiuxian.item.armor.ModPathArmor;
import com.hongmeng.xiuxian.item.gear.ModGear;
import com.hongmeng.xiuxian.item.gear.ModPathWeapon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * <b>创造模式物品栏</b>。
 *
 * <p>⚠ 本文件曾被一次脚本事故<b>截断成空文件</b>（脚本以 {@code "w"} 打开文件后
 * 才抛异常，文件已被清空），最后的备份又太旧。最终是<b>从上次成功构建的
 * {@code .class} 反推出物品顺序</b>重建的 —— 内容与原版等价。</p>
 *
 * <p>教训：后续任何"写文件"的脚本，都应该<b>先读、改内存、最后一次性写</b>，
 * 而不是先 {@code open("w")} 再算内容。</p>
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HongmengMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hongmeng.main"))
                    .icon(() -> new ItemStack(ModItems.PILL_POJING.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.MEDITATION_CUSHION_ITEM.get());
                        output.accept(ModItems.SPIRIT_GRASS_ITEM.get());
                        output.accept(ModItems.ROOT_ALTAR_ITEM.get());
                        output.accept(ModItems.EPOCH_PORTAL_FRAME_ITEM.get());
                        output.accept(ModItems.BEAST_PORTAL_FRAME_ITEM.get());
                        output.accept(ModItems.DEMON_PORTAL_FRAME_ITEM.get());
                        output.accept(ModItems.VOID_PORTAL_FRAME_ITEM.get());
                        output.accept(ModItems.SPIRIT_IRON_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_SPIRIT_IRON_ORE_ITEM.get());
                        output.accept(ModItems.SPIRIT_CRYSTAL_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_SPIRIT_CRYSTAL_ORE_ITEM.get());
                        output.accept(ModItems.KONGMING_ORE_ITEM.get());
                        output.accept(ModItems.XUELUO_ORE_ITEM.get());
                        output.accept(ModItems.SPIRIT_STONE_ORE_ITEM.get());
                        // 深板岩矿变体（2026-11-07 第二批：空冥/血罗/灵石 深层矿）
                        output.accept(ModItems.DEEPSLATE_KONGMING_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_XUELUO_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_SPIRIT_STONE_ORE_ITEM.get());
                        output.accept(ModItems.ZHAOHUA_DAN.get());
                        output.accept(ModItems.PILL_ZHUJI.get());
                        output.accept(ModItems.PILL_POJING.get());
                        output.accept(ModItems.PILL_XILING.get());
                        output.accept(ModItems.PILL_HUILING.get());
                        output.accept(ModItems.PILL_SHUANGDAO.get());
                        output.accept(ModItems.PILL_EXP_COMMON.get());
                        output.accept(ModItems.PILL_EXP_PERFECT.get());
                        output.accept(ModItems.PILL_EXP_IMMORTAL.get());
                        // ---- 灵石 / 灵石系修为丹（2026-11-07 修为经济线）----
                        output.accept(ModItems.SPIRIT_STONE.get());
                        output.accept(ModItems.PILL_JULING.get());
                        output.accept(ModItems.PILL_NINGYUAN.get());
                        // R-2 legacy 处理（06 §4）：
                        //   旧 10 把境界剑（ModGear.SWORDS）、200 件道途甲、
                        //   50 件道途武器——保留注册，但**移出创造栏**、不给加成、删配方。
                        //   这里只留下仍在用的法宝（ARTIFACTS）与十境道袍（ModArmor）。
                        ModGear.ARTIFACTS.forEach(r -> output.accept(r.get()));
                        ModArmor.all().forEach(r -> output.accept(r.get()));
                        com.hongmeng.xiuxian.item.armor.ModCareerArmor.all()
                                .forEach(r -> output.accept(r.get()));
                        com.hongmeng.xiuxian.item.gear.ModCareerWeapon.all()
                                .forEach(r -> output.accept(r.get()));
                        // ---- 旧 Geo 原型武器 10 件（06 §4「旧 Geo 原型 · 仅创造」）----
                        // 不进新成长线、不给配方；**只放创造栏**，否则它们除了 /give
                        // 之外没有任何入口（玩家反馈"图鉴能查到却拿不到"）。
                        output.accept(ModItems.SPIRIT_SWORD_GEO.get());
                        output.accept(ModItems.GEO_LINGXI.get());
                        output.accept(ModItems.GEO_BENGSHAN.get());
                        output.accept(ModItems.GEO_SHIHUN.get());
                        output.accept(ModItems.GEO_XUEYIN.get());
                        output.accept(ModItems.GEO_FEIJIAN.get());
                        output.accept(ModItems.GEO_YUNTIE.get());
                        output.accept(ModItems.GEO_XUEMO.get());
                        output.accept(ModItems.GEO_XUKONG.get());
                        output.accept(ModItems.GEO_LONGGU.get());
                        output.accept(ModItems.SWORD_ART_MANUAL.get());
                        output.accept(ModItems.ZHENYUAN_CRYSTAL.get());
                        output.accept(ModItems.ZHENYUAN_ORE_ITEM.get());
                        output.accept(ModItems.PILL_TALENT_RESET.get());
                        output.accept(ModItems.PILL_PATH_SWITCH.get());
                        output.accept(ModItems.LINGYE.get());
                        output.accept(ModItems.GUIDE_BOOK.get());
                        output.accept(ModItems.BOSS_SUMMON_ALTAR_ITEM.get());
                        output.accept(ModItems.BOSS_SUMMON_ALTAR_EPOCH_ITEM.get());
                        output.accept(ModItems.BOSS_SUMMON_ALTAR_BEAST_ITEM.get());
                        output.accept(ModItems.BOSS_SUMMON_ALTAR_DEMON_ITEM.get());
                        output.accept(ModItems.BOSS_SUMMON_ALTAR_VOID_ITEM.get());
                        output.accept(ModItems.DAOYIN.get());
                        output.accept(ModItems.JIDAN.get());
                        output.accept(ModItems.XUE_LUOGUO.get());
                        output.accept(ModItems.KONG_MING_SHI.get());
                        output.accept(ModItems.SECT_TOKEN_SHENXIAO.get());
                        output.accept(ModItems.SPIRIT_WOLF_SPAWN_EGG.get());
                        output.accept(ModItems.BLOOD_DEMON_CULTIVATOR_SPAWN_EGG.get());
                        output.accept(ModItems.PUPPET_CAPTAIN_SPAWN_EGG.get());
                        output.accept(ModItems.DEMON_GENERAL_SPAWN_EGG.get());
                        output.accept(ModItems.VOID_OVERLORD_SPAWN_EGG.get());
                        output.accept(ModItems.FLAME_APE_SPAWN_EGG.get());
                        output.accept(ModItems.THUNDER_EAGLE_SPAWN_EGG.get());
                        output.accept(ModItems.STONE_GOLEM_SPAWN_EGG.get());
                        output.accept(ModItems.VOID_SERPENT_SPAWN_EGG.get());
                        output.accept(ModItems.BURROW_MARTEN_SPAWN_EGG.get());
                        output.accept(ModItems.VENOM_TOAD_SPAWN_EGG.get());
                        output.accept(ModItems.SECT_ELDER_SPAWN_EGG.get());
                        output.accept(ModItems.ALCHEMY_MASTER_SPAWN_EGG.get());
                        output.accept(ModItems.WANDERING_MERCHANT_SPAWN_EGG.get());
                        output.accept(ModItems.CONTRACT_TALISMAN.get());
                        output.accept(ModItems.BEAST_POUCH.get());
                        output.accept(ModItems.BEAST_ARMOR.get());
                        output.accept(ModItems.CONTRACT_EAGLE_SPAWN_EGG.get());
                        output.accept(ModItems.JIAOLONG_SPAWN_EGG.get());
                        output.accept(ModItems.CONTRACT_WOLF_SPAWN_EGG.get());
                        // ---- 第二轮新怪的 13 颗刷怪蛋（原版刷怪蛋页签也有；这里给主栏一份）----
                        // 之前它们只有注册、没有 item 模型（游戏里是紫黑 missing model），
                        // 2026-11-07 补了 models/item/<egg>.json（template_spawn_egg）。
                        output.accept(ModItemsExtra.MARSH_WRAITH_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.CRYSTAL_SPIDER_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.TERRACOTTA_SOLDIER_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.SCARECROW_PUPPET_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.LAVA_HOUND_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.VOID_JELLY_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.END_APOSTLE_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.FALLEN_IMMORTAL_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.BLOOD_LEECH_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.BONE_EAGLE_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.DEMON_SPIDER_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.CHARRED_SKELETON_SPAWN_EGG.get());
                        output.accept(ModItemsExtra.STAR_DUST_WRAITH_SPAWN_EGG.get());
            output.accept(ModItems.BLAZE_EMPEROR_SPAWN_EGG.get());
            output.accept(ModItems.VOID_DRAGON_EMPEROR_SPAWN_EGG.get());
            output.accept(ModItems.EPOCH_BOSS_SPAWN_EGG.get());
            output.accept(ModItems.BEAST_BOSS_SPAWN_EGG.get());
            output.accept(ModItems.DEMON_BOSS_SPAWN_EGG.get());
            output.accept(ModItems.VOID_BOSS_SPAWN_EGG.get());
            output.accept(ModItems.VOID_WALKER_SPAWN_EGG.get());
            // ---- 恶之祭坛 ----
            output.accept(ModItems.EVIL_ALTAR_ITEM.get());
            // 第五轮：最终 Boss「恶」只允许通过「恶之祭坛（五凭证）」召唤入口出现，
        // 刷怪蛋不进创造栏（保留注册仅为管理员兼容；/summon 仍可用于调试）。
        // output.accept(ModItems.EVIL_GAZER_SPAWN_EGG.get());
            output.accept(ModItems.PUPPET_CORE.get());
            output.accept(ModItems.BEAST_HEART.get());
            output.accept(ModItems.BLOOD_CORE.get());
            output.accept(ModItems.VOID_EYE.get());

                        output.accept(ModItems.ALCHEMY_FURNACE_ITEM.get());
                        output.accept(ModItems.LINGTIAN_SOIL_ITEM.get());
                        output.accept(ModItems.XUELIAN_GRASS_ITEM.get());
                        output.accept(ModItems.HUOYAN_HUA_ITEM.get());
                        output.accept(ModItems.HANBING_TENG_ITEM.get());
                        output.accept(ModItems.LEIYIN_CAO_ITEM.get());
                        output.accept(ModItems.KONGMING_LAN_ITEM.get());
                        output.accept(ModItems.XUELUO_ZHI_ITEM.get());
                        output.accept(ModItems.XUELIAN_GRASS_SEED.get());
                        output.accept(ModItems.HUOYAN_HUA_SEED.get());
                        output.accept(ModItems.HANBING_TENG_SEED.get());
                        output.accept(ModItems.LEIYIN_CAO_SEED.get());
                        output.accept(ModItems.KONGMING_LAN_SEED.get());
                        output.accept(ModItems.XUELUO_ZHI_SEED.get());
                        output.accept(ModItems.XUELIAN_GRASS_PETAL.get());
                        output.accept(ModItems.HUOYAN_HUA_PETAL.get());
                        output.accept(ModItems.HANBING_TENG_PETAL.get());
                        output.accept(ModItems.LEIYIN_CAO_PETAL.get());
                        output.accept(ModItems.KONGMING_LAN_PETAL.get());
                        output.accept(ModItems.XUELUO_ZHI_PETAL.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    /** 往原版物品栏也塞一份（玩家习惯在原版栏找丹药）。 */
    public static void fillVanillaTab(BuildCreativeModeTabContentsEvent event) {
        event.accept(ModItems.ZHAOHUA_DAN);
        event.accept(ModItems.PILL_ZHUJI);
        event.accept(ModItems.PILL_POJING);
        event.accept(ModItems.PILL_XILING);
        event.accept(ModItems.PILL_HUILING);
        event.accept(ModItems.MEDITATION_CUSHION_ITEM);
        event.accept(ModItems.ROOT_ALTAR_ITEM);
        event.accept(ModItems.EPOCH_PORTAL_FRAME_ITEM);
        event.accept(ModItems.BEAST_PORTAL_FRAME_ITEM);
        event.accept(ModItems.DEMON_PORTAL_FRAME_ITEM);
        event.accept(ModItems.VOID_PORTAL_FRAME_ITEM);
        event.accept(ModItems.CONTRACT_TALISMAN);
        event.accept(ModItems.BEAST_POUCH);
        event.accept(ModItems.BEAST_ARMOR);
        event.accept(ModItems.ALCHEMY_FURNACE_ITEM);
        event.accept(ModItems.LINGTIAN_SOIL_ITEM);
        event.accept(ModItems.XUELIAN_GRASS_SEED);
        event.accept(ModItems.HUOYAN_HUA_SEED);
        event.accept(ModItems.HANBING_TENG_SEED);
        event.accept(ModItems.LEIYIN_CAO_SEED);
        event.accept(ModItems.KONGMING_LAN_SEED);
        event.accept(ModItems.XUELUO_ZHI_SEED);
        event.accept(ModItems.XUELIAN_GRASS_PETAL);
        event.accept(ModItems.HUOYAN_HUA_PETAL);
        event.accept(ModItems.HANBING_TENG_PETAL);
        event.accept(ModItems.LEIYIN_CAO_PETAL);
        event.accept(ModItems.KONGMING_LAN_PETAL);
        event.accept(ModItems.XUELUO_ZHI_PETAL);
    }
}
