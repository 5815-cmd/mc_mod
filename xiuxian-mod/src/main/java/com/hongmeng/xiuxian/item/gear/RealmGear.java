package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>境界门槛</b> —— 装备"分阶段"的另一半。
 *
 * <h2>玩家要求</h2>
 * <blockquote>
 * 「武器不能做出来就毕业，也得分什么阶段穿什么护甲用什么武器」
 * </blockquote>
 *
 * <h2>光"分阶注册"是不够的</h2>
 * <p>我把护甲改成了 200 件（5 道途 × 10 境 × 4 部位）、武器早已是 50 件
 * （5 × 10）。但如果<b>只要凑够材料就能用最高的那件</b>，
 * 那"分阶段"只是个名字 —— 玩家照样一步到位。</p>
 *
 * <p>所以必须有<b>穿戴门槛</b>：装备要求境界 ≥ 它自己的阶数。
 * 而且要在<b>三个环节</b>都拦：</p>
 * <ol>
 *   <li><b>合成</b>：配方材料本来就按境界递进（高阶要雪落矿/空冥矿）；</li>
 *   <li><b>穿戴</b>：境界不够就穿不上（本类的 {@link #checkEquip}）；</li>
 *   <li><b>持续检查</b>：交易/掉落/队友给的装备也要拦 ——
 *       所以每 20 tick 扫一遍身上的装备，超阶就摘下来放回背包。</li>
 * </ol>
 *
 * <h2>⚠ 为什么第 3 条不能省</h2>
 * <p>只拦"主动穿戴"是不够的：玩家可以从别人手里拿到高阶装备，
 * 或者境界倒退（理论上没有，但洗点/转修会重算天赋）。
 * 不扫的话会出现"练气境穿着鸿蒙护甲在打怪"。</p>
 *
 * <h2>⚠ 为什么"摘下来放回背包"而不是"销毁"</h2>
 * <p>那是玩家的财产。境界不够只是<b>暂时用不了</b>，不该没收。
 * 摘到背包里，等他境界到了再穿。</p>
 */
public final class RealmGear {

    private RealmGear() {
    }

    /** 门槛检查的间隔（tick）—— 每 20 tick 扫一次，不必每 tick。 */
    private static final int CHECK_INTERVAL = 20;

    /**
     * 这件装备要求的最低境界；不是分阶装备返回 0（不限制）。
     *
     * <p>护甲读 {@code PathArmorItem.tier()}，武器读 {@code PathWeaponItem.realmTier()}，
     * 职业甲/通用武器/法宝读自己的 {@code realm()/tier()}。
     * 其它物品（原版装备、丹药、材料）一律不限制。</p>
     */
    public static int requiredTier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.armor.ModPathArmor
                .PathArmorItem armor) {
            return armor.tier();
        }
        // C 线：职业甲按境界分件，每一件都有明确的最低境界
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.armor.CareerArmorItem careerArmor) {
            return careerArmor.realm();
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.ModPathWeapon
                .PathWeaponItem weapon) {
            return weapon.realmTier();
        }
        // R-2：通用双手武器是"每阶一件"的固定阶位武器，同样要拦超阶
        // （例：练气号拿到 career_weapon_sword_10 会被自动收起，而不是一刀 32 万）
        if (stack.getItem() instanceof CareerWeaponItem careerWeapon) {
            return careerWeapon.tier();
        }
        // 2026-11-07：法宝也按境界分件（10 境 × 5 部位），每件有自己的门槛，
        // 否则"每个境界都有法宝可以穿"会变成练气期直接穿鸿蒙至宝。
        if (stack.getItem() instanceof ArtifactItem artifact) {
            return artifact.realmTier();
        }
        return 0;
    }

    /** 玩家当前境界。 */
    public static int realmOf(ServerPlayer player) {
        try {
            CultivationData data = CultivationCapability.get(player);
            return data == null || data.realm() == null ? 1 : data.realm().tier();
        } catch (Throwable t) {
            return 1;
        }
    }

    /** 这个玩家能不能用这件装备。 */
    public static boolean canUse(ServerPlayer player, ItemStack stack) {
        // 创造模式放行：建筑/测试时不想被门槛挡住（生存模式照拦）
        if (player != null && player.isCreative()) {
            return true;
        }
        int need = requiredTier(stack);
        return need <= 0 || realmOf(player) >= need;
    }

    /** 一行说明（tooltip 用）：要求境界。 */
    public static String requirementText(ItemStack stack) {
        int need = requiredTier(stack);
        if (need <= 0) {
            return "";
        }
        return "\u00a78境界需求：\u00a7e第 " + need + " 境";
    }

    /**
     * 主动穿戴前的检查（由 {@code PlayerInteractEvent} 或使用路径调用）。
     *
     * @return 能不能穿/用；不能则已经给玩家发了提示
     */
    public static boolean checkEquip(ServerPlayer player, ItemStack stack) {
        // 创造模式放行（建筑/测试）
        if (player != null && player.isCreative()) {
            return true;
        }
        int need = requiredTier(stack);
        if (need <= 0) {
            return true;
        }
        int have = realmOf(player);
        if (have >= need) {
            return true;
        }
        player.displayClientMessage(Component.literal(
                "\u00a7c境界不足：\u00a77这件装备需要第 \u00a7e" + need
                        + " \u00a77境，你现在是第 \u00a7f" + have + " \u00a77境。"), true);
        return false;
    }

    /**
     * 每 20 tick 扫一遍：身上/法宝栏有超境界的装备就摘下来放回背包。
     *
     * <p>由 {@code ModEvents.onPlayerTick} 调。创造模式跳过（测试/建筑需要）。</p>
     */
    public static void tick(ServerPlayer player) {
        if (player == null || player.isCreative()) {
            return;
        }
        // 用玩家的 tick 计数取模，避免所有人都同一 tick 扫（错开开销）
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        int realm = realmOf(player);
        List<String> removed = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            int need = requiredTier(stack);
            if (need <= 0 || realm >= need) {
                continue;
            }
            // 摘下来放回背包（不销毁 —— 那是玩家的财产）
            ItemStack copy = stack.copy();
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
            removed.add(copy.getHoverName().getString());
        }
        // 法宝栏（capability 五槽）同样要拦：交易/掉落拿到的高阶法宝不能直接生效。
        try {
            var equipped = CultivationCapability.get(player).getEquipment();
            for (int i = 0; i < PlayerEquipment.SLOT_COUNT; i++) {
                ItemStack stack = equipped.get(i);
                int need = requiredTier(stack);
                if (need <= 0 || realm >= need) {
                    continue;
                }
                ItemStack copy = stack.copy();
                if (GearManager.unequip(player, i)) {
                    removed.add(copy.getHoverName().getString());
                }
            }
        } catch (Throwable t) {
            // 法宝栏不可用（例如 capability 未初始化）不影响其它装备检查
            HongmengMod.LOGGER.debug("[realm-gear] 法宝栏境界检查失败：{}", t.toString());
        }
        if (!removed.isEmpty()) {
            player.displayClientMessage(Component.literal(
                    "\u00a7c境界不足，已收起：\u00a77" + String.join("\u00a77、", removed)), false);
            HongmengMod.LOGGER.debug("[realm-gear] {} 境界 {} 收起超阶装备 {}",
                    player.getName().getString(), realm, removed);
        }
    }
}
