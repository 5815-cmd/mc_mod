package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>通用双手武器注册器</b>——R-2 装备线：5 型 × 10 阶 = <b>50 件</b>。
 *
 * <p>注册名严格按 06 §2：{@code career_weapon_<form>_<1..10>}。</p>
 *
 * <h2>外观：21 个基础名各一版（v2）</h2>
 * <p>不再按「低/中/高三档」共用模型 —— 06 表里的 21 个基础名各有一个独立 geo
 * （{@code career_<form>_<variant>}），50 件按阶数映射到对应名字段，例如：</p>
 * <pre>
 *   长剑：1-2 青锋(qingfeng) / 3-4 赤霄(chixiao) / 5-6 玄铁(xuantie)
 *        / 7-8 血槽(xuecao) / 9-10 霜纹古篆(shuangwen)
 * </pre>
 * <p>这样「血槽魔纹剑」不会再显示成霜纹外观。</p>
 */
public final class ModCareerWeapon {

    /** 五型顺序必须与 {@link WeaponForm#values()} 一致。 */
    private static final String[][] VARIANTS = {
            // 长剑
            {"t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
            // 巨剑
            {"t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
            // 镰刀
            {"t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
            // 长枪
            {"t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
            // 棍
            {"t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
            // 战镰
            {"t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
    };

    private static DeferredRegister<Item> items;
    private static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    private ModCareerWeapon() {
    }

    public static void register(IEventBus bus) {
        items = DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);
        WeaponForm[] forms = WeaponForm.values();
        for (int fi = 0; fi < forms.length; fi++) {
            WeaponForm form = forms[fi];
            for (int tier = 1; tier <= 10; tier++) {
                final int t = tier;
                final String family = "career_" + form.key() + "_"
                        + VARIANTS[fi][tier - 1];
                RegistryObject<Item> obj = items.register(
                        "career_weapon_" + form.key() + "_" + tier,
                        () -> new CareerWeaponItem(family, form, t));
                ALL.add(obj);
            }
        }
        items.register(bus);
    }

    /** 某型某阶对应的外观 key（供图鉴/断言查询）。 */
    public static String variantOf(int formIndex, int tier) {
        int f = Math.max(0, Math.min(VARIANTS.length - 1, formIndex));
        int t = Math.max(1, Math.min(10, tier));
        return VARIANTS[f][t - 1];
    }

    /** 某型某阶的完整 family（career_<form>_<variant>）。 */
    public static String familyOf(WeaponForm form, int tier) {
        return "career_" + form.key() + "_" + variantOf(form.ordinal(), tier);
    }

    public static List<RegistryObject<Item>> all() {
        return Collections.unmodifiableList(ALL);
    }
}
