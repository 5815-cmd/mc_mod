package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.core.RealmStats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 按境界分阶的装备注册表。
 *
 * <p>品阶命名直接用境界（练气法器 / 筑基灵器 / ……），数值锚定
 * {@link RealmStats} 的标准值表。</p>
 *
 * <ul>
 *   <li><b>武器</b>：每境一把剑，附加攻击 = 标准值 × 1.0，增幅每境 +5%</li>
 *   <li><b>法宝</b>：法冠 / 道袍 / 法裤 / 云履 / 玉佩 五个部位，放背包即生效</li>
 * </ul>
 */
public final class ModGear {

    public static final DeferredRegister<Item> GEAR =
            DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);

    /** 十境的品阶名（下标 0 = 练气）。数据在 {@link GearNames}，那里不碰注册表。 */
    public static final String[] GRADE_NAMES = GearNames.GRADE_NAMES;

    /** 十境的剑名。 */
    public static final String[] SWORD_NAMES = GearNames.SWORD_NAMES;

    private static final Rarity[] SWORD_RARITY = {
            Rarity.COMMON, Rarity.COMMON, Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.RARE,
            Rarity.RARE, Rarity.EPIC, Rarity.EPIC, Rarity.EPIC, Rarity.EPIC
    };

    public static final List<RegistryObject<Item>> SWORDS = new ArrayList<>();
    public static final List<RegistryObject<Item>> ARTIFACTS = new ArrayList<>();

    static {
        // ---------- 武器：每境一把 ----------
        for (int tier = 1; tier <= 10; tier++) {
            final int t = tier;
            final double attack = RealmStats.weaponAttack(tier);
            final double amplify = RealmStats.weaponAmplify(tier);
            SWORDS.add(GEAR.register("realm_sword_" + tier, () -> new RealmWeaponItem(
                    t, GRADE_NAMES[t - 1], attack, amplify,
                    new Item.Properties().stacksTo(1).rarity(SWORD_RARITY[t - 1])
                            .durability(200 + t * 220))));
        }

        // ---------- 法宝：五个部位 × 十境（2026-11-07 数值重做）----------
        //
        // 数值表见 ArtifactStats（纯数据，可离线断言）：
        //   五件合计护甲 = 本境标准值 × 0.276；生命上限/攻击各 +6%/+8%/+10% 按外观档。
        // 旧版是「护甲 0.805×std + 固定额外生命 0.16×std」——
        // 护甲 4 境后白给、额外生命道印境 +208%、鸿蒙境 +4%，两头翘，已废弃。
        for (int tier = 1; tier <= 10; tier++) {
            final int t = tier;
            final String grade = GRADE_NAMES[tier - 1];
            final Rarity rar = SWORD_RARITY[tier - 1];
            for (int slot = 0; slot < ArtifactStats.slotCount(); slot++) {
                final int s = slot;
                final String slotName = ArtifactStats.SLOT_NAMES[slot];
                final String id = ArtifactStats.itemName(tier, slot);
                ARTIFACTS.add(GEAR.register(id, () -> new ArtifactItem(
                        t, grade, slotName,
                        ArtifactStats.armor(t, s),
                        ArtifactStats.toughness(t, s),
                        ArtifactStats.healthPercent(t, s),
                        ArtifactStats.attackPercent(t, s),
                        new Item.Properties().stacksTo(1).rarity(rar))));
            }
        }
    }

    /** 中文部位名 → 注册名用的英文 key。 */
    public static String slotKey(String slotName) {
        return GearNames.slotKey(slotName);
    }

    private ModGear() {
    }

    public static void register(IEventBus bus) {
        GEAR.register(bus);
    }
}
