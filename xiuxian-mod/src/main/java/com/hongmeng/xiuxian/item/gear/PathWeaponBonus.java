package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.talent.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * <b>道途武器的"攻速差异"结算</b>。
 *
 * <h2>为什么要单独一个类</h2>
 * <p>{@link PathWeapon} 的伤害倍率可以写进物品本身（{@code SwordItem} 的
 * attack damage 修饰符），但<b>攻速倍率不行</b> —— 原版攻击速度是
 * <b>玩家属性</b>（{@code Attributes.ATTACK_SPEED}），物品只能给它加修饰符，
 * 而"拿到主手才生效"这件事需要每 tick 检查。</p>
 *
 * <p>所以拆成两半：</p>
 * <ul>
 *   <li><b>物品</b>负责单次伤害（{@code damageFactor}）；</li>
 *   <li><b>本类</b>负责攻速（{@code speedFactor}），主手持有才施加。</li>
 * </ul>
 * <p>两半合起来才是 {@code damageFactor × speedFactor = 1.00} 的
 * "总 DPS 对齐"。</p>
 *
 * <h2>⚠ 幂等</h2>
 * <p>用固定 UUID + <b>先摘后挂</b>。直接 {@code addPermanentModifier} 在重复调用时
 * 会抛 {@code IllegalArgumentException: Modifier is already applied}，
 * 而这类异常常被 try/catch 吃掉 —— 结果是 buff 静默失效（项目里踩过一次）。</p>
 */
public final class PathWeaponBonus {

    /** 攻速修饰符 id（固定 → 幂等）。 */
    private static final UUID SPEED_ID = UUID.nameUUIDFromBytes(
            "hongmeng_path_weapon_speed".getBytes(StandardCharsets.UTF_8));

    private PathWeaponBonus() {
    }

    /**
     * 每 tick（或低频）调一次：把主手道途武器的攻速差异施加到玩家身上。
     *
     * <p>主手不是道途武器（或不是本道途的）→ 撤销修饰符，攻速回到原版。</p>
     */
    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            AttributeInstance inst = player.getAttribute(Attributes.ATTACK_SPEED);
            if (inst == null) {
                return;
            }
            PathWeapon kind = com.hongmeng.xiuxian.item.gear.ModPathWeapon
                    .kindOf(player.getMainHandItem());
            // 旧武器按新职业→旧道途的过渡桥匹配（R-2 新武器上线后删桥）
            com.hongmeng.xiuxian.talent.Career career =
                    com.hongmeng.xiuxian.talent.Talents.pathOf(player);
            Path path = com.hongmeng.xiuxian.item.LegacyGearBridge
                    .toLegacyPath(career);
            if (kind == null || path == null || !kind.name().equals(path.name())) {
                remove(inst);
                return;
            }
            double delta = kind.speedFactor() - 1.0D;   // 相对基准剑的攻速差
            apply(inst, delta);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[path-weapon] 攻速施加失败：{}", t.toString());
        }
    }

    /** 固定 UUID + 先摘后挂（幂等）。 */
    private static void apply(AttributeInstance inst, double delta) {
        if (inst.getModifier(SPEED_ID) != null) {
            inst.removeModifier(SPEED_ID);
        }
        if (Math.abs(delta) < 1.0E-6D) {
            return;
        }
        inst.addPermanentModifier(new AttributeModifier(SPEED_ID, "道途武器攻速", delta,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void remove(AttributeInstance inst) {
        if (inst.getModifier(SPEED_ID) != null) {
            inst.removeModifier(SPEED_ID);
        }
    }

    /** 面板用的一行摘要。 */
    public static String summary(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return "\u00a78未持道途武器";
        }
        PathWeapon kind = ModPathWeapon.kindOf(player.getMainHandItem());
        if (kind == null) {
            return "\u00a78主手非道途武器";
        }
        Path path = com.hongmeng.xiuxian.item.LegacyGearBridge.toLegacyPath(
                com.hongmeng.xiuxian.talent.Talents.pathOf(player));
        boolean match = path != null && kind.name().equals(path.name());
        return (match ? "\u00a7a" : "\u00a78") + kind.cn()
                + "\u00a78 伤害×" + String.format("%.2f", kind.damageFactor())
                + " 攻速×" + String.format("%.2f", kind.speedFactor())
                + (match ? "" : "\u00a78（非本道途，攻速不生效）");
    }
}
