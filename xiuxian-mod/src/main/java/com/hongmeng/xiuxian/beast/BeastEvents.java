package com.hongmeng.xiuxian.beast;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.entity.beast.BeastManager;
import com.hongmeng.xiuxian.entity.beast.BeastRiding;
import com.hongmeng.xiuxian.entity.beast.SpiritBeastEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 灵兽的每 tick 结算：<b>飞行扣主人的灵力</b>。
 *
 * <h3>为什么必须挂在这里，而不是放在实体自己的 tick 里</h3>
 *
 * <p>规格第五节第 4 条要求"客户端也要拿到数值（灵力条、骑乘状态）"。
 * 灵力是<b>玩家</b>的属性、灵力条画在玩家的 HUD 上，所以扣灵力这件事
 * 只有一个正确的落点：玩家的 tick。放在坐骑的 tick 里会让"谁在扣谁"
 * 变得含糊，而且坐骑卸载后灵力条不会立刻刷新。</p>
 *
 * <p>本类<b>只做扣费与中断</b>，不做回复——回复仍然在
 * {@code ModEvents.onPlayerTick} 里，由它读
 * {@link BeastManager#ridingFlyingBeast} 决定这一 tick 该不该回蓝。
 * 这两件事分开写是刻意的：飞行的灵力池必须与御剑共用同一套规则，
 * 而"骑飞行灵兽时不回蓝"正是御剑那条规则的直接推广
 * （御剑中也不回蓝，见 {@code SwordFlight.regenTick}）。</p>
 */
public final class BeastEvents {

    /** 公开构造：Forge 事件总线要求实例注册（见 HongmengMod 的 forgeBus.register）。 */
    public BeastEvents() {
    }

    /** 每 tick：飞行的灵力消耗。顺序必须在灵力回复之前（见 ModEvents）。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        // 骑乘系统已关闭：老存档里还骑在灵兽上的玩家，登录后自动下马。
        if (!BeastRiding.ENABLED) {
            if (player.getVehicle() instanceof SpiritBeastEntity) {
                player.stopRiding();
            }
            return;
        }
        SpiritBeastEntity mount = BeastManager.flyingMountOf(player);
        if (mount == null) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        BeastManager.tickFlightDrain(player, data);
    }

    /** 掉线时清运行期状态（灵兽本身留在世界里，由实体自己保存）。 */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BeastManager.forget(event.getEntity());
    }
}
