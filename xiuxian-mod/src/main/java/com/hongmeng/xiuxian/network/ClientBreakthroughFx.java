package com.hongmeng.xiuxian.network;

/** 客户端突破演出状态。 */
public final class ClientBreakthroughFx {

    private static BreakthroughFxPacket.Kind kind = null;
    private static String realm = "";
    private static int ticks = 0;

    private ClientBreakthroughFx() {
    }

    public static void trigger(BreakthroughFxPacket.Kind k, String realmName) {
        kind = k;
        realm = realmName;
        ticks = 60;
    }

    public static void tick() {
        if (ticks > 0) {
            ticks--;
            if (ticks == 0) {
                kind = null;
            }
        }
    }

    public static boolean active() {
        return ticks > 0;
    }

    public static BreakthroughFxPacket.Kind kind() {
        return kind;
    }

    public static String realm() {
        return realm;
    }

    public static float progress() {
        return 1.0F - (ticks / 60.0F);
    }
}
