package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.talent.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>五套「道途护甲」的注册器</b>：5 条道途 × 4 件 = 20 件装备 + 5 个材质。
 *
 * <h2>⚠ 注册逻辑刻意延迟到 {@link #register} 才建</h2>
 * <p>我第一版把这个 {@code DeferredRegister} 放在<b>静态字段</b>里，
 * 结果<b>离线验证器一碰这个类就崩</b>：</p>
 * <pre>
 *   ExceptionInInitializerError
 *   Caused by: IllegalArgumentException: Not bootstrapped
 *     at ForgeRegistries.&lt;clinit&gt;
 * </pre>
 * <p>验证器不启动游戏，Forge 注册表没 bootstrap，而静态字段会强制初始化 ——
 * 于是一条纯数据的断言（护甲值是多少）被注册表拖累到完全跑不了。</p>
 *
 * <p>所以现在：<b>纯数据</b>放在 {@link PathArmorTable}（验证器随便读），
 * <b>注册</b>放这里且只在 {@link #register} 里建。要读数值请走
 * {@link PathArmorTier} 与 {@link PathArmorTable}。</p>
 *
 * <h2>命名（与 {@code ModArmor} 同一套约定）</h2>
 * <pre>
 *   注册名   path_armor_&lt;path&gt;_&lt;slot&gt;   path = fa/ti/mo/xue/zhen
 *   材质名   hongmeng:path_armor_&lt;path&gt;
 *   图层贴图 assets/hongmeng/textures/models/armor/path_armor_&lt;path&gt;_layer_{1,2}.png
 *   物品图标 assets/hongmeng/textures/item/path_armor_&lt;path&gt;_&lt;slot&gt;.png
 *   物品模型 assets/hongmeng/models/item/path_armor_&lt;path&gt;_&lt;slot&gt;.json
 * </pre>
 *
 * <h2>⚠ 1.20.1 的 ArmorMaterial 不是注册表条目</h2>
 * <p>它只是个普通接口，{@code ArmorItem} 在构造时把材质对象存成字段。
 * 所以<b>不要</b>给材质建 {@code DeferredRegister} —— 那会直接编译不过。</p>
 */
public final class ModPathArmor {

    /** 护甲物品注册表 —— 延迟到 {@link #register} 才创建（见类注释）。 */
    private static DeferredRegister<Item> armor;

    /** (道途, 部位) -> 注册对象（注册后才填充）。 */
    /** "PATH_TIER" -> (部位 -> 注册对象)。**key 必须带境界**，否则同部位会互相覆盖。 */
    private static final Map<String, Map<ArmorItem.Type, RegistryObject<Item>>> TABLE =
            new HashMap<>();

    /** 材质："PATH_TIER" -> 材质（一境一材质）。 */
    private static final Map<String, PathArmorMaterial> MATERIALS = new HashMap<>();

    private ModPathArmor() {
    }

    /** 供验证器/别处读取的部位列表（转发到纯数据表）。 */
    public static ArmorItem.Type[] types() {
        return PathArmorTable.TYPES;
    }

        /**
     * 注册全部护甲（由 {@code HongmengMod} 在模组构造时调）。
     *
     * <p>规模：<b>5 道途 × 10 境界 × 4 部位 = 200 件</b>，外加 50 个材质
     * （一道途一境界一个材质 —— 因为 {@code ArmorMaterial} 的数值方法
     * 拿不到 ItemStack，见 {@link PathArmorMaterial} 的说明）。</p>
     */
    public static void register(IEventBus bus) {
        armor = DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);
        for (Path path : Path.values()) {
            PathArmorTier tier = PathArmorTier.of(path);
            if (tier == null) {
                continue;
            }
            Map<ArmorItem.Type, RegistryObject<Item>> row = new HashMap<>();
            for (int t = 1; t <= PathArmorTable.MAX_TIER; t++) {
                final int tt = t;
                // 一境一材质（同一道途、不同境界的护甲值不同）
                PathArmorMaterial material = new PathArmorMaterial(tier, tt,
                        () -> Ingredient.of(Items.IRON_INGOT));
                MATERIALS.put(path.name() + "_" + tt, material);
                for (ArmorItem.Type type : PathArmorTable.TYPES) {
                    String name = PathArmorTable.itemName(path, tt, type);
                    row.put(type, armor.register(name,
                            () -> new PathArmorItem(material, type, path, tt)));
                    // 部位+境界 -> 注册对象（key 带境界，避免同部位互相覆盖）
                    TABLE.put(path.name() + "_" + tt, row);
                }
            }
        }
        registerLegacyAliases();
        armor.register(bus);
    }

    /**
     * <b>注册 20 个旧名别名</b> —— 修复"改注册名导致存档 ID 错位"。
     *
     * <h2>为什么必须做</h2>
     * <p>护甲注册名从 {@code path_armor_<path>_<slot>}（20 件）改成
     * {@code path_armor_<path>_<tier>_<slot>}（200 件）之后，起服报：</p>
     * <pre>
     *   Found a missing id from the world hongmeng:path_armor_fa_helmet   （20 个）
     *   [ERROR] Unidentified mapping from registry minecraft:item
     *   [WARN] Object did not get ID it asked for ...                      （220 个）
     * </pre>
     *
     * <p>后果远不止"20 件旧护甲消失"：Forge 的注册表 ID 按注册顺序分配，
     * 少了 20 个名字之后<b>后面所有物品的 ID 都往前挪了</b> ——
     * 存档里每个物品的 ID 都对不上号。</p>
     *
     * <p>所以把旧名注册回来当别名，数值取<b>第 1 境</b>（旧版无境界概念，
     * 取最低档最保守）。这些别名<b>不给配方</b> —— 只是兼容用的历史遗留。</p>
     */
    private static void registerLegacyAliases() {
        for (Path path : Path.values()) {
            PathArmorTier tier = PathArmorTier.of(path);
            if (tier == null) {
                continue;
            }
            // 别名统一用第 1 境的数值
            PathArmorMaterial legacyMat = new PathArmorMaterial(tier, 1,
                    () -> Ingredient.of(Items.IRON_INGOT));
            for (ArmorItem.Type type : PathArmorTable.TYPES) {
                String legacy = PathArmorTable.legacyItemName(path, type);
                armor.register(legacy,
                        () -> new PathArmorItem(legacyMat, type, path, 1));
            }
        }
        HongmengMod.LOGGER.debug("[path-armor] 已注册 20 个旧名别名（存档兼容）");
    }


    /**
     * 取某道途<b>某境界</b>某部位的护甲物品（未注册时返回 null）。
     *
     * <p>⚠ 签名加了 {@code tier}：玩家要求"分阶段穿护甲"，
     * 所以护甲也按境界注册（5 × 10 × 4 = 200 件）。</p>
     */
    public static Item get(Path path, int tier, ArmorItem.Type type) {
        Map<ArmorItem.Type, RegistryObject<Item>> row =
                TABLE.get(path.name() + "_" + tier);
        if (row == null) {
            return null;
        }
        RegistryObject<Item> obj = row.get(type);
        return obj == null ? null : obj.get();
    }

    /** 全部注册对象。 */
    public static List<RegistryObject<Item>> all() {
        List<RegistryObject<Item>> out = new ArrayList<>();
        for (Map<ArmorItem.Type, RegistryObject<Item>> row : TABLE.values()) {
            out.addAll(row.values());
        }
        return out;
    }

    /**
     * 这件装备属于哪条道途；不是道途护甲返回 {@code null}。
     *
     * <p>只做 {@code instanceof} 判断，不碰注册表 —— 战斗结算会频繁调用它。</p>
     */
    public static Path pathOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof PathArmorItem armorItem)) {
            return null;
        }
        return armorItem.path();
    }

    /** 某条道途的材质数据（取加成数值用）。 */
    public static PathArmorTier tierOf(Path path) {
        return PathArmorTier.of(path);
    }

    /**
     * <b>道途护甲物品</b>。
     *
     * <p>它自己在 tooltip 里写清"这套强化什么"—— 因为玩家<b>看不出</b>
     * 五套的数值差异（基础护甲三项刻意做成完全相同），
     * 差别只在道途加成上。</p>
     */
    public static class PathArmorItem extends ArmorItem {

        private final Path path;
        /** 这件护甲要求的境界（玩家要求"分阶段穿"）。 */
        private final int tier;

        public PathArmorItem(PathArmorMaterial material, Type type, Path path, int tier) {
            super(material, type, new Item.Properties());
            this.path = path;
            this.tier = tier;
        }

        public Path path() {
            return this.path;
        }

        /** 要求的境界。 */
        public int tier() {
            return this.tier;
        }

        @Override
        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                    TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            PathArmorTier tier = PathArmorTier.of(this.path);
            if (tier == null) {
                return;
            }
            tooltip.add(Component.literal("\u00a77道途护甲 \u00a78· "
                    + this.path.colored().getString() + " \u00a77" + tier.cn()));
            tooltip.add(Component.literal("\u00a7d" + tier.bonusText()));
            tooltip.add(Component.literal("\u00a78穿齐 4 件吃满加成"));
            // 套装档位单独一行（tooltip 里必须分开 add，不能用 \n）
            tooltip.add(Component.literal("\u00a78" + tier.setText()));
            tooltip.add(Component.literal(
                    "\u00a78境界需求：\u00a7e第 " + this.tier + " 境"));
        }
    
}
}
