package com.hongmeng.xiuxian.item.armor;

import com.hongmeng.xiuxian.talent.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>道途护甲套装的加成结算</b>。
 *
 * <h2>规则</h2>
 * <ul>
 *   <li>只有<b>本道途</b>的护甲才计数 —— 法修穿体修甲不给法修加成
 *       （这是"独立装备"的意义所在）；</li>
 *   <li>加成<b>按件数线性</b>（穿 2 件吃 50%，穿齐 4 件吃满）；</li>
 *   <li>基础护甲/韧性由原版 {@code ArmorItem} 自己给（材质已设定），
 *       这里只管<b>道途专属加成</b>；</li>
 *   <li>额外生命也用固定 UUID 的永久修饰符加，<b>先摘后挂</b>保证幂等
 *       （直接 add 会在重复调用时抛异常，而异常常被 try/catch 吃掉，
 *        结果是 buff 静默失效 —— 这个坑项目里踩过一次）。</li>
 * </ul>
 *
 * <h2>为什么缓存到内存而不是每次重新数</h2>
 * <p>道途加成会被战斗结算频繁读取（每次伤害都要问一次吸血/破防）。
 * 每 tick 数一遍背包太浪费，所以 {@link #refresh} 时算好存起来，
 * 战斗侧只读缓存。</p>
 */
public final class PathArmorBonus {

    /** 玩家 -> 当前套装加成。 */
    private static final Map<UUID, double[]> CACHE = new HashMap<>();

    /** 加成数组的下标含义。 */
    public static final int SPELL = 0;
    public static final int MANA = 1;
    public static final int MELEE = 2;
    public static final int ARMOR = 3;
    public static final int LIFESTEAL = 4;
    public static final int PENETRATION = 5;
    public static final int FORMATION = 6;
    public static final int HEALTH = 7;
    /** 2 件套可能的加成：冷却缩减。 */
    public static final int COOLDOWN = 8;
    /** 2 件套可能的加成：抗击退。 */
    public static final int KNOCKBACK = 9;
    /** 2 件套可能的加成：领域范围。 */
    public static final int AREA = 10;
    /** 4 件套的"机制强化系数"（含义由道途决定，见 PathArmorTier.setFourKind）。 */
    public static final int SET_BONUS = 11;
    public static final int COUNT = 12;

    /** 额外生命的修饰符 id（固定 → 幂等）。 */
    private static final UUID HEALTH_ID =
            UUID.nameUUIDFromBytes("hongmeng_path_armor_health"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private PathArmorBonus() {
    }

    /**
     * 重新结算（由 {@code GearManager.refresh} 顺带调用，约每 20 tick 一次）。
     *
     * @param path 玩家当前道途；{@code null} 表示尚未择道
     */
    public static void refresh(ServerPlayer player, Path path) {
        if (player == null) {
            return;
        }
        double[] out = new double[COUNT];
        if (path == null) {
            CACHE.put(player.getUUID(), out);
            applyHealth(player, 0.0D);
            return;
        }
        PathArmorTier tier = PathArmorTier.of(path);
        if (tier == null) {
            CACHE.put(player.getUUID(), out);
            applyHealth(player, 0.0D);
            return;
        }
        int worn = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (PathArmorBonus.pathOf(stack) == path) {
                worn++;
            }
        }
        if (worn == 0) {
            CACHE.put(player.getUUID(), out);
            applyHealth(player, 0.0D);
            return;
        }
        // 按件数线性（穿齐 = 100%）
        double frac = worn / 4.0D;
        // 套装档位（2 件 / 4 件）——见 PathArmorTier 的套装注释
        boolean two = worn >= 2;
        boolean four = worn >= 4;
        out[SPELL] = tier.spellBonus() * frac;
        out[MANA] = tier.manaBonus() * frac;
        out[MELEE] = tier.meleeBonus() * frac;
        out[ARMOR] = tier.armorBonus() * frac;
        out[LIFESTEAL] = tier.lifestealBonus() * frac;
        out[PENETRATION] = tier.penetrationBonus() * frac;
        out[FORMATION] = tier.formationBonus() * frac;
        out[HEALTH] = tier.healthPerPiece() * worn;

        // ---- 2 件套：一个"早点凑两件也值得"的小加成 ----
        if (two) {
            double v = tier.setTwoPiece();
            switch (tier.setTwoKind()) {
                case "法术冷却缩减" -> out[COOLDOWN] += v;
                case "抗击退" -> out[KNOCKBACK] += v;
                case "破防" -> out[PENETRATION] += v;
                case "吸血" -> out[LIFESTEAL] += v;
                case "领域范围" -> out[AREA] += v;
                default -> { }
            }
        }

        // ---- 4 件套：强化本道途的**核心机制**（不是再加数值）----
        //
        // 这些系数不在"玩家属性"里，而是给机制计算用的，
        // 所以只记在 SET_BONUS 这一格，由 PathRuntime 自己去读
        // （见下面的 setFourBonus）。
        if (four) {
            out[SET_BONUS] = tier.setFourPiece();
        }
        CACHE.put(player.getUUID(), out);
        applyHealth(player, out[HEALTH]);
    }

    /** 这件装备属于哪条道途（转发到 {@link ModPathArmor}）。 */
    public static Path pathOf(ItemStack stack) {
        return ModPathArmor.pathOf(stack);
    }

    /** 穿够 2 件 → true（2 件套生效）。 */
    public static boolean setTwoActive(net.minecraft.world.entity.player.Player player) {
        Path path = com.hongmeng.xiuxian.item.LegacyGearBridge.toLegacyPath(
                com.hongmeng.xiuxian.talent.Talents.pathOf(player));
        return path != null && wornCount(player, path) >= 2;
    }

    /** 穿够 4 件 → true（4 件套生效）。 */
    public static boolean setFourActive(net.minecraft.world.entity.player.Player player) {
        Path path = com.hongmeng.xiuxian.item.LegacyGearBridge.toLegacyPath(
                com.hongmeng.xiuxian.talent.Talents.pathOf(player));
        return path != null && wornCount(player, path) >= 4;
    }

    /**
     * 4 件套的<b>机制强化系数</b>（0 = 没生效）。
     *
     * <p>含义由道途决定（{@code PathArmorTier.setFourKind()}）：
     * 法修=真元盾转化率、体修=激荡获取、魔修=魔气上限、
     * 血修=缓伤比例、阵修=阵域上限。</p>
     */
    public static double setFourBonus(net.minecraft.world.entity.player.Player player) {
        return get(player, SET_BONUS);
    }

    /** 读某一项加成（没穿 = 0）。 */
    public static double get(net.minecraft.world.entity.player.Player player, int index) {
        if (player == null) {
            return 0.0D;
        }
        double[] v = CACHE.get(player.getUUID());
        return v == null || index < 0 || index >= v.length ? 0.0D : v[index];
    }

    /** 穿了几件本道途护甲（面板/tooltip 用）。 */
    public static int wornCount(net.minecraft.world.entity.player.Player player, Path path) {
        if (player == null || path == null) {
            return 0;
        }
        int worn = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (pathOf(player.getItemBySlot(slot)) == path) {
                worn++;
            }
        }
        return worn;
    }

    /** 额外生命：固定 UUID + 先摘后挂（幂等）。 */
    private static void applyHealth(ServerPlayer player, double amount) {
        try {
            var inst = player.getAttribute(
                    net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (inst == null) {
                return;
            }
            if (inst.getModifier(HEALTH_ID) != null) {
                inst.removeModifier(HEALTH_ID);
            }
            if (amount > 0.0D) {
                inst.addPermanentModifier(
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                HEALTH_ID, "道途护甲生命", amount,
                                net.minecraft.world.entity.ai.attributes.AttributeModifier
                                        .Operation.ADDITION));
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[path-armor] 生命加成施加失败：{}", t.toString());
        }
    }

    /** 面板用的一行摘要。 */
    public static String summary(net.minecraft.world.entity.player.Player player, Path path) {
        if (player == null || path == null) {
            return "\u00a78未择道";
        }
        PathArmorTier tier = PathArmorTier.of(path);
        if (tier == null) {
            return "\u00a78无";
        }
        int worn = wornCount(player, path);
        if (worn == 0) {
            return "\u00a78未穿戴（0/4）\u00a77 " + tier.cn();
        }
        // ⚠ 这里要**简短**：它会出现在面板的一行里（detail 在 tooltip 看）。
        //   原来把整串 bonusText 拼进来，长到溢出面板边框。
        String mark = worn >= 4 ? " \u00a76[4件套]"
                : (worn >= 2 ? " \u00a7e[2件套]" : "");
        return "\u00a7a" + tier.cn() + " \u00a77" + worn + "/4" + mark;
    }

    public static void forget(UUID uuid) {
        CACHE.remove(uuid);
    }
}
