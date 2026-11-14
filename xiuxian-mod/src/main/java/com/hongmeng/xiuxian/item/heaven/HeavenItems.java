package com.hongmeng.xiuxian.item.heaven;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.item.HeavenTokenItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * <b>第四轮 · 第五维度「天外天」的进入道具注册表</b>（13 号设计稿 §1.3）。
 *
 * <p>注册两件东西：{@code heaven_token}（登天令）与 {@code heaven_crown}（仙庭之冕，恶之祭坛第五凭证）。</p>
 *
 * <p>与 {@code ModItemsExtra} 分开写是照本批施工约定：新内容走新增文件，
 * 不去改别人负责的共享注册表。{@link #register(IEventBus)} 由 Lead 在
 * {@code HongmengMod} 构造器里加<b>一行</b>调用（原文见
 * {@code 工程/角色设计/round4/heaven-entry/hunk.md}）：
 * <pre>{@code
 * com.hongmeng.xiuxian.item.heaven.HeavenItems.register(modBus);
 * }</pre>
 * 那一行同时完成两件事：注册道具 + 挂创造栏监听，不需要再动
 * {@code ModItemsExtra}/{@code ModCreativeTabs}。</p>
 */
public final class HeavenItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);

    /** 本模组主创造栏的 key（{@code ModCreativeTabs.MAIN} = {@code hongmeng:main}）。 */
    private static final ResourceKey<CreativeModeTab> MAIN_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    new ResourceLocation(HongmengMod.MOD_ID, "main"));

    /** 登天令：青玉令牌，不消耗，右键登天 / 回归。 */
    public static final RegistryObject<Item> HEAVEN_TOKEN =
            ITEMS.register("heaven_token",
                    () -> new HeavenTokenItem(HeavenTokenItem.defaultProperties()));

    /**
     * 仙庭之冕：天外天 Boss「仙庭之主」的专属凭证。
     *
     * <p>和傀儡核心 / 兽王之心 / 血魔之核 / 虚空之眼并列，作为恶之祭坛的第五个槽位材料。
     * 死亡时由 {@code ModEvents.onAltarBossDeath} 必定掉落，不靠概率。</p>
     */
    public static final RegistryObject<Item> HEAVEN_CROWN =
            ITEMS.register("heaven_crown",
                    () -> new Item(new Item.Properties().stacksTo(16)
                            .rarity(Rarity.EPIC)));

    private HeavenItems() {
    }

    /** 由 {@code HongmengMod} 的构造器调用（hunk.md 里那一行）。 */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(HeavenItems::onBuildCreativeTab);
    }

    /** 把登天令放进本模组主创造栏（{@code hongmeng:main}）的第一屏。 */
    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!MAIN_TAB.equals(event.getTabKey())) {
            return;
        }
        if (HEAVEN_TOKEN.isPresent()) {
            event.accept(HEAVEN_TOKEN.get());
        }
        if (HEAVEN_CROWN.isPresent()) {
            event.accept(HEAVEN_CROWN.get());
        }
    }
}
