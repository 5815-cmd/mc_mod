package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.talent.Career;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>职业甲注册器</b>——按境界分件（C 线改造）。
 *
 * <p>5 职业 × 10 境界 × 4 部位 = <b>200 件</b>，注册名
 * {@code career_armor_<career>_<realm>_<slot>}（realm = 1~10）。</p>
 *
 * <p>外观仍只有 15 套（5 职业 × 初/进/名器），按境界段复用；
 * 每件数值固定（见 {@link CareerArmorRealm} / {@link CareerArmorMaterial}）。</p>
 */
public final class ModCareerArmor {

    private static DeferredRegister<Item> items;
    private static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    private ModCareerArmor() {
    }

    public static void register(IEventBus bus) {
        items = DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);
        Career[] careers = Career.values();
        for (Career career : careers) {
            for (int realm = 1; realm <= CareerArmorRealm.MAX_REALM; realm++) {
                CareerArmorSet band = CareerArmorRealm.setOf(career, realm);
                Rarity rarity = band.tier() == CareerArmorSet.Tier.MASTER
                        ? Rarity.EPIC
                        : (band.tier() == CareerArmorSet.Tier.ADVANCED
                        ? Rarity.RARE : Rarity.UNCOMMON);
                for (int slot = 0; slot < 4; slot++) {
                    final int s = slot;
                    final int r = realm;
                    final CareerArmorSet b = band;
                    String id = CareerArmorRealm.itemName(career, r,
                            CareerArmorRealm.SLOTS[s]);
                    RegistryObject<Item> obj = items.register(id,
                            () -> new CareerArmorItem(b, r, s, rarity));
                    ALL.add(obj);
                }
            }
        }
        items.register(bus);
    }

    /** 已注册的全部职业甲（创造栏/图鉴用）。 */
    public static List<RegistryObject<Item>> all() {
        return Collections.unmodifiableList(ALL);
    }
}
