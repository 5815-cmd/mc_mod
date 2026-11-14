package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.talent.Path;
import net.minecraft.world.item.ArmorItem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <b>道途护甲的数据表</b>（与注册分离）。
 *
 * <h2>⚠ 为什么必须把数据和注册拆成两个类</h2>
 * <p>我第一版把"注册"和"数据"写在同一个类里，静态块直接建
 * {@code DeferredRegister.create(ForgeRegistries.ITEMS, ...)}。</p>
 *
 * <p>后果：<b>离线验证器一碰这个类就崩</b> ——</p>
 * <pre>
 *   java.lang.ExceptionInInitializerError
 *   Caused by: IllegalArgumentException: Not bootstrapped
 *     at ForgeRegistries.&lt;clinit&gt;
 * </pre>
 * <p>因为验证器不启动游戏，Forge 的注册表没有 bootstrap。于是一条**纯数据**
 * 的断言（护甲值是多少、加成对不对）被注册表拖累到完全无法运行。</p>
 *
 * <p>拆开之后：</p>
 * <ul>
 *   <li>本类<b>只碰纯数据</b>（{@code ArmorItem.Type} 是原版枚举，安全）——
 *       验证器可以随便断言；</li>
 *   <li>{@link ModPathArmor} 才建 {@code DeferredRegister}，
 *       而且改成<b>第一次调用时</b>才建（不是静态块），
 *       所以验证器只要不碰它就不会崩。</li>
 * </ul>
 *
 * <p>这是一条值得记住的规矩：<b>能被离线验证的数据，不要和需要游戏环境的
 * 注册代码放在同一个类的静态初始化里。</b></p>
 */
public final class PathArmorTable {

    private PathArmorTable() {
    }

    /** 四个部位（按穿戴顺序）。 */
    public static final ArmorItem.Type[] TYPES = {
            ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
            ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};

    /** 部位 -> 注册名后缀（与贴图/模型文件名一致）。 */
    public static String slotName(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
        };
    }

    /**
     * 境界上限（与武器一致：练气 1 ~ 鸿蒙 10）。
     *
     * <p>玩家要求「也得分什么阶段穿什么护甲」—— 所以护甲与武器一样按境界分阶。</p>
     */
    public static final int MAX_TIER = 10;

    /**
     * 道途护甲注册名（<b>含境界</b>）：{@code path_armor_<path>_<tier>_<slot>}。
     *
     * <p>⚠ 这个签名是<b>改过的</b>：原来只有 {@code path_armor_<path>_<slot>}（无境界），
     * 结果"练气穿的和鸿蒙穿的是同一件"。旧注册名的物品会消失（开发期代价）。</p>
     */
    public static String itemName(Path path, int tier, ArmorItem.Type type) {
        return "path_armor_" + path.name().toLowerCase(java.util.Locale.ROOT)
                + "_" + tier + "_" + slotName(type);
    }

    /** 旧签名（无境界）—— 仅供需要"找旧物品"的场景，比如存档迁移提示。 */
    public static String legacyItemName(Path path, ArmorItem.Type type) {
        return "path_armor_" + path.name().toLowerCase(java.util.Locale.ROOT)
                + "_" + slotName(type);
    }

    /** 道途 -> 材质名（决定护甲图层贴图路径）。 */
    public static String materialName(Path path) {
        return "path_armor_" + path.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 全部 20 个"道途 + 部位"组合（验证器用来逐条断言）。 */
    public static Map<Path, ArmorItem.Type[]> allCombos() {
        Map<Path, ArmorItem.Type[]> out = new LinkedHashMap<>();
        for (Path p : Path.values()) {
            out.put(p, TYPES);
        }
        return out;
    }

    /** 期望的护甲物品总数（5 道途 × 10 境 × 4 部位 = 200）。 */
    public static int expectedCount() {
        return Path.values().length * MAX_TIER * TYPES.length;
    }

    /**
     * 基础三项之和（全套护甲值）。
     *
     * <p>五套完全相同 —— 这是"战力平衡"的硬线，验证器会断言。</p>
     */
    public static int totalDefense(PathArmorTier tier) {
        int sum = 0;
        for (ArmorItem.Type t : TYPES) {
            sum += tier.defense(t);
        }
        return sum;
    }
}
