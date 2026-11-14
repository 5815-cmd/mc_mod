package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 十境「道袍」可穿戴护甲的注册表：10 套 × 4 件 = 40 件装备，
 * 外加 10 个 {@link ArmorMaterial}（每个大境一个，保证耐久/护甲/韧性都能按境界给）。
 *
 * <h2>为什么「一个境界一个材质」而不是「一个材质 + 耐久倍率」</h2>
 *
 * <p>{@code ArmorMaterial} 的 {@code getDurabilityForType}、{@code getDefenseForType}、
 * {@code getToughness}、{@code getName()} 全都是**无参（或只吃部位）**的方法，
 * 材质对象自己拿不到 ItemStack。想把「同一材质、不同境界不同数值」做出来，
 * 就得让物品反查自身——那会绕开原版护甲属性系统（{@code ArmorItem}
 * 是在构造时就固定 material 的）。一境一材质最直白，也让
 * {@code getName()} 天然承当护甲图层贴图名。</p>
 *
 * <h2>命名</h2>
 *
 * <pre>
 *   注册名   dao_armor_&lt;tier&gt;_&lt;slot&gt;          slot = helmet/chestplate/leggings/boots
 *   材质名   hongmeng:dao_armor_&lt;tier&gt;
 *   图层贴图 assets/hongmeng/textures/models/armor/dao_armor_&lt;tier&gt;_layer_{1,2}.png
 *   物品图标 assets/hongmeng/textures/item/dao_armor_&lt;tier&gt;_&lt;slot&gt;.png
 *   物品模型 assets/hongmeng/models/item/dao_armor_&lt;tier&gt;_&lt;slot&gt;.json
 *   合成配方 data/hongmeng/recipes/dao_armor_&lt;tier&gt;_&lt;slot&gt;.json
 * </pre>
 *
 * <h2>注意：1.20.1 的 ArmorMaterial 不是注册表条目</h2>
 *
 * <p>{@code ArmorMaterial} 在 1.20.1 就是一个普通接口，原版用枚举
 * {@code ArmorMaterials} 实现；<b>既没有 {@code Registries.ARMOR_MATERIAL}，
 * 也没有 {@code ForgeRegistries.ARMOR_MATERIALS}</b>（那是 1.21 才拆出去的注册表），
 * {@code ArmorItem} 只是在构造时把材质对象存成字段。所以这里直接 new 出十个材质
 * 常量即可，**不要**给材质建 {@code DeferredRegister}——那会直接编译不过。</p>
 *
 * <p>本类只做注册，不含任何数值——数值都在 {@link DaoArmorTier}。</p>
 */
public final class ModArmor {

    /** 护甲物品注册表（护甲材质不是注册表条目，见类注释）。 */
    public static final DeferredRegister<Item> ARMOR =
            DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);

    /** 十境的材质，下标 0 = 练气。 */
    public static final List<DaoArmorMaterial> MATERIALS = new ArrayList<>(10);

    /**
     * 40 件护甲，按 tier 1..10、每境内 helmet → chestplate → leggings → boots 排列。
     * 创造模式物品栏直接遍历这个列表。
     */
    public static final List<RegistryObject<Item>> ARMOR_ITEMS = new ArrayList<>(40);

    /** 修复材料：灵铁锭（全部炼器的起点，十境通用）。延迟解析，注册表就绪后才取。 */
    private static final Supplier<Ingredient> REPAIR = () -> Ingredient.of(ModItems.SPIRIT_IRON_INGOT.get());

    static {
        for (int tier = 1; tier <= 10; tier++) {
            final DaoArmorTier data = DaoArmorTier.of(tier);

            DaoArmorMaterial material = DaoArmorMaterial.of(data, REPAIR);
            MATERIALS.add(material);

            for (int slot = 0; slot < 4; slot++) {
                final int s = slot;
                String id = DaoArmorTier.idOf(tier, slot);
                // ⚠ 必须把注册结果**收进 ARMOR_ITEMS**。
                //
                // 这里原来只调用 `ARMOR.register(...)` 就丢掉了返回值 ——
                // 于是 ARMOR_ITEMS 永远是空的：
                //   * `all()` 返回空 → **创造模式物品栏里一件护甲都看不到**
                //     （玩家反馈"创造物品栏 mod 界面里的物品不全，没有护甲什么的"）；
                //   * `get(t*4+s)` 会抛 IndexOutOfBounds。
                // 顺序必须是 (tier-1)*4 + slot —— 与 get() 的下标算法一致。
                ARMOR_ITEMS.add(ARMOR.register(id, () -> new DaoArmorItem(
                        material, data, s,
                        new Item.Properties().stacksTo(1).rarity(data.rarity()))));
            }
        }
    }

    private ModArmor() {
    }

    /** 第 tier 境（1~10）的护甲材质。 */
    public static DaoArmorMaterial material(int tier) {
        return MATERIALS.get(Math.max(1, Math.min(10, tier)) - 1);
    }

    /**
     * 第 tier 境、第 slot 件（slot: 0 头盔 / 1 胸甲 / 2 护腿 / 3 靴子）。
     *
     * <p>直接按 {@code (tier-1)*4 + slot} 取下标——{@link #ARMOR_ITEMS} 就是按
     * 「tier 1..10、每境内 helmet → chestplate → leggings → boots」的顺序填的。
     * 这样不用再维护一张二维表（也就不会踩泛型数组的 unchecked 警告）。</p>
     */
    public static RegistryObject<Item> piece(int tier, int slot) {
        int t = Math.max(1, Math.min(10, tier)) - 1;
        int s = Math.max(0, Math.min(3, slot));
        return ARMOR_ITEMS.get(t * 4 + s);
    }

    /** 只读的 40 件列表。 */
    public static List<RegistryObject<Item>> all() {
        return Collections.unmodifiableList(ARMOR_ITEMS);
    }

    /** 第 slot 件的部位类型。 */
    public static ArmorItem.Type typeOf(int slot) {
        return DaoArmorTier.TYPES[Math.max(0, Math.min(3, slot))];
    }

    public static void register(IEventBus bus) {
        ARMOR.register(bus);
    }
}
