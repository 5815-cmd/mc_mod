package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 功法秘籍的注册。
 *
 * <p>刻意**独立成类**而不是塞进 {@code ModItems}：秘籍有 22 门，
 * 集中注册能让 {@code ModItems} 保持清爽，也让"改功法"和"改物品"互不干扰。</p>
 *
 * <p>每门功法对应一件秘籍，注册名是 {@code manual_<枚举名小写>}，
 * 例如 {@code hongmeng:manual_wan_jian_gui_zong}。</p>
 */
public final class TechniqueItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);

    private static final Map<Technique, RegistryObject<TechniqueManualItem>> MANUALS =
            new EnumMap<>(Technique.class);

    static {
        for (Technique t : Technique.values()) {
            // 职业主动（18 门新增）：只在天赋树解锁后可用 → 不生成秘籍，
            // 否则买书就能绕过天赋树。2 门复用条目仍是基础池身份，照旧生成。
            if (CareerTechnique.isUnlockOnly(t)) {
                continue;
            }
            String id = "manual_" + t.name().toLowerCase(java.util.Locale.ROOT);
            MANUALS.put(t, ITEMS.register(id, () -> new TechniqueManualItem(t,
                    new Item.Properties()
                            .stacksTo(1)
                            // 高阶功法用稀有度区分，背包里一眼能看出档次
                            .rarity(rarityFor(t)))));
        }
    }

    private TechniqueItems() {
    }

    /** 功法等级越高，物品稀有度越高（影响名字颜色与提示条）。 */
    private static Rarity rarityFor(Technique technique) {
        int tier = technique.minTier();
        if (tier >= 9) {
            return Rarity.EPIC;
        }
        if (tier >= 7) {
            return Rarity.RARE;
        }
        if (tier >= 5) {
            return Rarity.UNCOMMON;
        }
        return Rarity.COMMON;
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    /** 某门功法的秘籍物品；未注册时返回 null。 */
    public static RegistryObject<TechniqueManualItem> manualOf(Technique technique) {
        return MANUALS.get(technique);
    }

    /** 全部秘籍，按功法声明顺序。 */
    public static Map<Technique, RegistryObject<TechniqueManualItem>> all() {
        return Collections.unmodifiableMap(MANUALS);
    }
}
