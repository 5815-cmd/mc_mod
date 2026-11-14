package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.core.RealmStats;
import com.hongmeng.xiuxian.talent.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
 * <b>道途武器注册器</b> —— 5 条道途各一把，每把按境界分阶。
 *
 * <h2>为什么每把要做 10 阶</h2>
 * <p>既有的 {@code realm_sword_<tier>} 是"每境一把剑"。如果道途武器只有一把，
 * 那么练气期的玩家拿到它就一直用到大乘 —— 既没有成长感，也会让"每境换装备"
 * 这条既有的成长线断掉。所以道途武器同样按境界分阶（10 把 × 5 道途 = 50 件）。</p>
 *
 * <h2>数值怎么来的</h2>
 * <p>用既有的 {@link RealmStats#weaponAttack(int)} / {@link RealmStats#weaponAmplify(int)}
 * 作基准，再乘上 {@link PathWeapon} 的伤害倍率。
 * <b>攻速倍率</b>通过"额外攻速修饰符"施加 —— 但那个不能挂在物品上
 * （原版攻击速度是玩家属性），所以这里：
 * </p>
 * <ul>
 *   <li>物品只负责<b>单次伤害</b>（用 damageFactor 调整）；</li>
 *   <li>攻速差异由 {@link PathWeaponBonus} 在装备主手时施加到玩家属性上。</li>
 * </ul>
 * <p>两边合起来才是 {@link PathWeapon#dpsFactor()} 恒为 1.00 的"总 DPS 对齐"。</p>
 *
 * <h2>⚠ 注册逻辑延迟到 {@link #register}（与护甲同样的原因）</h2>
 * <p>静态字段建 {@code DeferredRegister} 会让离线验证器一碰这个类就抛
 * {@code Not bootstrapped}。纯数据在 {@link PathWeapon}，注册在这里。</p>
 */
public final class ModPathWeapon {

    private static DeferredRegister<Item> weapons;

    /** (道途, 境界) -> 注册对象。 */
    private static final Map<String, RegistryObject<Item>> TABLE = new HashMap<>();

    /** 全部注册对象（按注册顺序）。 */
    private static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    /** 境界上限（与既有武器一致：练气 1 ~ 鸿蒙 10）。 */
    public static final int MAX_TIER = 10;

    private ModPathWeapon() {
    }

    /**
     * 注册全部 50 件道途武器（由 {@code HongmengMod} 在模组构造时调）。
     */
    public static void register(IEventBus bus) {
        weapons = DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);
        for (PathWeapon weapon : PathWeapon.values()) {
            for (int tier = 1; tier <= MAX_TIER; tier++) {
                final int t = tier;
                String name = itemName(weapon, tier);
                RegistryObject<Item> obj = weapons.register(name,
                        () -> new PathWeaponItem(weapon, t,
                                new Item.Properties().stacksTo(1)
                                        .durability(200 + t * 220)));
                TABLE.put(weapon.name() + "_" + tier, obj);
                ALL.add(obj);
            }
        }
        weapons.register(bus);
    }

    /** 注册名：`path_weapon_<key>_<tier>`。 */
    public static String itemName(PathWeapon weapon, int tier) {
        return "path_weapon_" + weapon.key() + "_" + tier;
    }

    /** 取某个道途某境界的武器（未注册时返回 null）。 */
    public static Item get(PathWeapon weapon, int tier) {
        RegistryObject<Item> obj = TABLE.get(weapon.name() + "_" + tier);
        return obj == null ? null : obj.get();
    }

    /** 全部注册对象（验证器用；注意读它会初始化注册表，所以验证器改读纯数据）。 */
    public static List<RegistryObject<Item>> all() {
        return ALL;
    }

    /** 这把武器属于哪条道途；不是道途武器返回 null。 */
    public static PathWeapon kindOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof PathWeaponItem w)) {
            return null;
        }
        return w.weapon();
    }

    /** 物品的伤害值（供 {@code PathWeaponBonus} 与 tooltip 读）。 */
    public static double attackOf(PathWeapon weapon, int tier) {
        return RealmStats.weaponAttack(tier) * weapon.damageFactor();
    }

    /**
     * <b>道途武器物品</b>。
     *
     * <p>它不继承 {@code RealmWeaponItem}，因为那会把"增幅"也一起带进来，
     * 而道途武器的差异在<b>伤害 / 攻速的分配</b>上，不需要再叠一层增幅。
     * 直接继承 {@code SwordItem} 更清楚。</p>
     */
    public static class PathWeaponItem extends net.minecraft.world.item.SwordItem {

        private final PathWeapon weapon;
        private final int tier;

        /**
         * 构造。
         *
         * <p>⚠ <b>伤害修饰符刻意传 0</b>（原来是 {@code 2 + damageFactor*2}，
         * 即 3~5 的整数值）。</p>
         *
         * <p>原因：道途武器的真实伤害是<b>境界量级</b>
         * （{@code RealmStats.weaponAttack(tier)}：第 1 境 8 → 第 10 境 320000），
         * 由 {@code WeaponAttributeHandler} 接成属性修饰符。</p>
         *
         * <p>如果这里再传一个 3~5 的整数，那么"原版那点伤害 + 境界伤害"会在
         * 低境界虚高（第 1 境 5+8=13，而同境界 {@code realm_sword} 是 9）。
         * 传 0 之后，两种武器的口径一致：
         * 原版只贡献 {@code Tiers} 的附加伤害（石 +1 ~ 下界 +4），
         * 主要伤害全部来自境界那一份。</p>
         */
        public PathWeaponItem(PathWeapon weapon, int tier, Item.Properties props) {
            super(weaponTier(tier), 0,
                    (float) (-2.4D + (weapon.speedFactor() - 1.0D) * 1.6D), props);
            this.weapon = weapon;
            this.tier = tier;
        }

        public PathWeapon weapon() {
            return this.weapon;
        }

        public int realmTier() {
            return this.tier;
        }

        /** 单次伤害（含道途倍率）。 */
        public double attack() {
            return attackOf(this.weapon, this.tier);
        }

        @Override
        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                    TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            Path p = Path.byName(this.weapon.name());
            tooltip.add(Component.literal("\u00a77道途武器 \u00a78· "
                    + (p == null ? "" : p.colored().getString() + " \u00a77")
                    + this.weapon.cn()));
            tooltip.add(Component.literal("\u00a78" + this.weapon.desc()));
            // 显示**实际伤害值**而不是只给倍率 —— 玩家关心的是"能打多少"。
            // （这个数值之前只存在于字符串里，从没接到属性上，见 WeaponAttributeHandler）
            tooltip.add(Component.literal(String.format(
                    "\u00a77单次伤害 \u00a7c+%.0f \u00a78(×%.2f) \u00a77· 攻速 \u00a7e×%.2f",
                    this.attack(), this.weapon.damageFactor(),
                    this.weapon.speedFactor())));
            tooltip.add(Component.literal(
                    "\u00a78每秒伤害与其他道途武器相同（差异在单次 vs 频率）"));
            // 装备主手时才吃攻速差异
            tooltip.add(Component.literal("\u00a78拿在主手时攻速差异才生效"));
            // 境界门槛（玩家要求"分阶段用武器"）
            tooltip.add(Component.literal(
                    "\u00a78境界需求：\u00a7e第 " + this.tier + " 境"));
        }

        /**
         * 境界 -> 原版工具材质（挖掘等级与耐久"看起来"合理）。
         *
         * <p>与 {@code RealmWeaponItem} 用同一套映射 —— 这样同一境界的
         * 道途武器与通用剑在挖掘等级上一致，不会出现"拿道途武器挖不动"的怪事。</p>
         */
        private static net.minecraft.world.item.Tier weaponTier(int realmTier) {
            if (realmTier <= 2) {
                return net.minecraft.world.item.Tiers.STONE;
            }
            if (realmTier <= 3) {
                return net.minecraft.world.item.Tiers.IRON;
            }
            if (realmTier <= 5) {
                return net.minecraft.world.item.Tiers.DIAMOND;
            }
            return net.minecraft.world.item.Tiers.NETHERITE;
        }
    }
}
