package com.hongmeng.xiuxian.item.gear;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 装备加成结算。
 *
 * <p>两件事：</p>
 * <ol>
 *   <li><b>法宝</b>：只读**法宝装备栏**（背包界面右侧那五个槽位），
 *       累加护甲 / 韧性（加法）与生命上限 / 攻击（百分比，2026-11-07 重做）；</li>
 *   <li><b>武器增幅</b>：主手武器提供"增幅 %"，作用到总攻击上。</li>
 * </ol>
 *
 * <p>用固定 UUID 的永久修饰符实现，反复调用不会堆叠——每次先移除再加。</p>
 */
public final class GearManager {

    private static final UUID ARMOR_ID =
            UUID.nameUUIDFromBytes("hongmeng_gear_armor".getBytes(StandardCharsets.UTF_8));
    private static final UUID TOUGHNESS_ID =
            UUID.nameUUIDFromBytes("hongmeng_gear_toughness".getBytes(StandardCharsets.UTF_8));
    private static final UUID HEALTH_ID =
            UUID.nameUUIDFromBytes("hongmeng_gear_health".getBytes(StandardCharsets.UTF_8));
    private static final UUID ATTACK_ID =
            UUID.nameUUIDFromBytes("hongmeng_gear_attack".getBytes(StandardCharsets.UTF_8));
    private static final UUID AMPLIFY_ID =
            UUID.nameUUIDFromBytes("hongmeng_weapon_amplify".getBytes(StandardCharsets.UTF_8));

    private static final String ARMOR_NAME = "hongmeng_gear_armor";
    private static final String TOUGHNESS_NAME = "hongmeng_gear_toughness";
    private static final String HEALTH_NAME = "hongmeng_gear_health";
    private static final String ATTACK_NAME = "hongmeng_gear_attack";
    private static final String AMPLIFY_NAME = "hongmeng_weapon_amplify";

    private GearManager() {
    }

    /** 每 20 tick 调一次即可。 */
    public static void refresh(ServerPlayer player) {
        var data = com.hongmeng.xiuxian.core.CultivationCapability.get(player);
        PlayerEquipment equipped = data.getEquipment();

        double armor = 0.0D;
        double toughness = 0.0D;
        double healthPercent = 0.0D;
        double attackPercent = 0.0D;
        for (int i = 0; i < PlayerEquipment.SLOT_COUNT; i++) {
            ItemStack piece = equipped.get(i);
            if (piece.getItem() instanceof ArtifactItem art) {
                // 2026-11-07 重做：护甲/韧性仍是加法，生命/攻击改成**百分比**
                // （旧版固定生命 0.16×标准值 与玩家生命不是同一刻度：道印 +208%、鸿蒙 +4%）。
                // 品质倍率已包含在下面四个带 stack 的重载里。
                armor += art.armor(piece);
                toughness += art.toughness(piece);
                healthPercent += art.healthPercent(piece);
                attackPercent += art.attackPercent(piece);
            }
        }

        // 护甲数值可以很大（五件合计 = 本境标准值 × 0.276），但**属性层**统一封顶到
        // ArmorMitigation.ARMOR_SOFT_CAP（80）：原版公式在 armor≥100 时会出现
        // 「大伤害反而 0 伤」的负数分支。真实减伤由 ArmorMitigation 用原始护甲值结算。
        apply(player, Attributes.ARMOR, ARMOR_ID, ARMOR_NAME, armor);
        apply(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, TOUGHNESS_NAME, toughness);
        apply(player, Attributes.MAX_HEALTH, HEALTH_ID, HEALTH_NAME, healthPercent,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        apply(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, ATTACK_NAME, attackPercent,
                AttributeModifier.Operation.MULTIPLY_TOTAL);

        // 武器增幅
        double amplify = 0.0D;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof RealmWeaponItem w) {
            amplify = w.amplify(main);
        }
        apply(player, Attributes.ATTACK_DAMAGE, AMPLIFY_ID, AMPLIFY_NAME, amplify,
                AttributeModifier.Operation.MULTIPLY_TOTAL);

        // 旧道途护甲（legacy 过渡）：按"本职业对应的旧道途"结算；
        // R-2 起旧装备统一 legacy、不再给加成（bridge 返回 null，这里保留调用以便回滚）。
        com.hongmeng.xiuxian.item.armor.PathArmorBonus.refresh(player,
                com.hongmeng.xiuxian.item.LegacyGearBridge.toLegacyPath(
                        com.hongmeng.xiuxian.talent.Talents.pathOf(player)));

        // R-2 职业甲动态结算（护甲/韧性/生命 + 2/4 件套）
        com.hongmeng.xiuxian.item.armor.CareerArmorHandler.refresh(player);

        // 所有护甲来源都挂完之后，统一做属性层封顶（幂等；换装/突破会在 1 秒内跟上）
        com.hongmeng.xiuxian.compat.ArmorMitigation.capVanillaArmor(player);
    }

    /** 当前装备栏里法宝提供的护甲总值（不含原版装备）。 */
    public static double artifactArmorOf(ServerPlayer player) {
        double armor = 0.0D;
        var equipped = com.hongmeng.xiuxian.core.CultivationCapability.get(player).getEquipment();
        for (int i = 0; i < PlayerEquipment.SLOT_COUNT; i++) {
            ItemStack piece = equipped.get(i);
            if (piece.getItem() instanceof ArtifactItem art) {
                armor += art.armor(piece);
            }
        }
        return armor;
    }

    /** 当前装备栏里法宝提供的韧性总值。 */
    public static double artifactToughnessOf(ServerPlayer player) {
        double toughness = 0.0D;
        var equipped = com.hongmeng.xiuxian.core.CultivationCapability.get(player).getEquipment();
        for (int i = 0; i < PlayerEquipment.SLOT_COUNT; i++) {
            ItemStack piece = equipped.get(i);
            if (piece.getItem() instanceof ArtifactItem art) {
                toughness += art.toughness(piece);
            }
        }
        return toughness;
    }

    /** 面板用：把装备栏里所有法宝的品质列出来（例如「神品 · 仙品」）。 */
    public static String qualitySummary(ServerPlayer player) {
        var equipped = com.hongmeng.xiuxian.core.CultivationCapability.get(player).getEquipment();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PlayerEquipment.SLOT_COUNT; i++) {
            ItemStack piece = equipped.get(i);
            if (piece.isEmpty() || !(piece.getItem() instanceof ArtifactItem)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\u00a77 ");
            }
            sb.append(com.hongmeng.xiuxian.quality.ItemQuality.of(piece).colored());
        }
        return sb.length() == 0 ? "\u00a78无" : sb.toString();
    }

    /** 一个属性槽位：属性 + 固定 UUID。用 record 取代早先的 Object[][] 强转。 */
    private record ModifierSlot(net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                UUID id) {
    }

    private static final java.util.List<ModifierSlot> SLOTS = java.util.List.of(
            new ModifierSlot(Attributes.ARMOR, ARMOR_ID),
            new ModifierSlot(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID),
            new ModifierSlot(Attributes.MAX_HEALTH, HEALTH_ID),
            new ModifierSlot(Attributes.ATTACK_DAMAGE, ATTACK_ID),
            new ModifierSlot(Attributes.ATTACK_DAMAGE, AMPLIFY_ID));

    /** 把法宝与武器增幅的加成全部清掉（换装备/掉线时调用，避免残留）。 */
    public static void clear(ServerPlayer player) {
        for (ModifierSlot slot : SLOTS) {
            AttributeInstance inst = player.getAttribute(slot.attribute());
            if (inst != null) {
                inst.removeModifier(slot.id());
            }
        }
        // R-2 职业甲的动态属性也一起清（固定 UUID，先摘后挂）
        com.hongmeng.xiuxian.item.armor.CareerArmorHandler.clear(player);
    }

    /** 加法修饰符（护甲/韧性这类"数值本来就很大"的属性）。 */
    private static void apply(ServerPlayer player,
                              net.minecraft.world.entity.ai.attributes.Attribute attr,
                              UUID id, String name, double amount) {
        apply(player, attr, id, name, amount, AttributeModifier.Operation.ADDITION);
    }

    /** 通用版本：显式指定运算方式（生命%/攻击% 用 MULTIPLY_TOTAL）。 */
    private static void apply(ServerPlayer player,
                              net.minecraft.world.entity.ai.attributes.Attribute attr,
                              UUID id, String name, double amount,
                              AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (Math.abs(amount) < 1e-6) {
            return;
        }
        inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    /** 面板用：当前装备栏里的法宝摘要。 */
    public static String summary(ServerPlayer player) {
        var equipped = com.hongmeng.xiuxian.core.CultivationCapability.get(player).getEquipment();
        if (equipped.equippedCount() == 0) {
            return "\u00a78未装备法宝（背包界面右侧有空位）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PlayerEquipment.SLOT_COUNT; i++) {
            ItemStack s = equipped.get(i);
            if (s.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\u00a77 ");
            }
            if (s.getItem() instanceof ArtifactItem art) {
                sb.append("\u00a7b").append(art.slotName()).append("\u00a77(第")
                        .append(art.realmTier()).append("境)");
            } else {
                sb.append("\u00a7b").append(PlayerEquipment.nameOf(i));
            }
        }
        return sb.toString();
    }

    /**
     * 把一件法宝装到它对应的槽位。
     *
     * @return true 表示装上去了（原来的那件会退回背包）
     */
    public static boolean equipFrom(ServerPlayer player, int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) {
            return false;
        }
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        int slot = PlayerEquipment.slotFor(stack);
        if (slot < 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7c这不是法宝，装不进法宝栏。"));
            return false;
        }
        // 境界门槛（2026-11-07）：第 N 境法宝必须达到第 N 境才能装，
        // 否则"每个境界都有法宝可以穿"就变成练气直接穿鸿蒙至宝。
        if (!RealmGear.checkEquip(player, stack)) {
            return false;
        }
        var equipped = com.hongmeng.xiuxian.core.CultivationCapability.get(player).getEquipment();
        ItemStack old = equipped.get(slot);
        ItemStack toEquip = stack.copy();
        toEquip.setCount(1);
        stack.shrink(1);
        equipped.set(slot, toEquip);
        if (!old.isEmpty() && !player.getInventory().add(old)) {
            player.drop(old, false);
        }
        player.getInventory().setChanged();
        com.hongmeng.xiuxian.network.ModNetworkBridge.sync(player);
        refresh(player);
        return true;
    }

    /** 卸下某个槽位的法宝，退回背包。 */
    public static boolean unequip(ServerPlayer player, int slot) {
        var equipped = com.hongmeng.xiuxian.core.CultivationCapability.get(player).getEquipment();
        ItemStack cur = equipped.get(slot);
        if (cur.isEmpty()) {
            return false;
        }
        if (!player.getInventory().add(cur.copy())) {
            player.drop(cur.copy(), false);
        }
        equipped.set(slot, ItemStack.EMPTY);
        player.getInventory().setChanged();
        com.hongmeng.xiuxian.network.ModNetworkBridge.sync(player);
        refresh(player);
        return true;
    }
}
