package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.server.level.ServerPlayer;

/**
 * 让人省心的同步入口：{@link CultivationSource} 之类的地方直接调它，
 * 不用到处抓 ModNetwork 的静态字段。
 */
public final class ModNetworkBridge {

    private ModNetworkBridge() {
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        SyncCultivationPacket.send(player, data);
    }

    /**
     * <b>把「真实世界已开启」转给客户端</b>（决定维度外观）。
     *
     * <p>专服安全：这里<b>不 import 任何客户端类</b> ——
     * 用 {@code Class.forName} 反射找客户端的入口，找到才调。同一份代码在专服上
     * 不会因为加载客户端类而崩（项目里 {@code HongmengMod} 的客户端特效、
     * {@code CombatFeedbackPacket} 都用的这个办法，是既有约定）。</p>
     */
    public static void setRealWorld(boolean active) {
        try {
            Class<?> cls = Class.forName("com.hongmeng.xiuxian.client.RealWorldAtmosphere");
            cls.getMethod("setActive", boolean.class).invoke(null, active);
        } catch (Throwable ignored) {
            // 专服 / 无客户端：正常情况，外观本来也不需要
        }
    }
}
