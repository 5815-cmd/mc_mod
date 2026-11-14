package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.core.CultivationData;

/**
 * 客户端缓存的自己那份修炼数据。HUD 与面板都读这里，不再跟服务端要。
 */
public final class ClientCultivationCache {

    private static CultivationData data = new CultivationData();
    private static int painTicks = 0;

    private ClientCultivationCache() {
    }

    public static CultivationData get() {
        return data;
    }

    public static void set(CultivationData newData) {
        if (newData != null) {
            data = newData;
        }
    }

    public static boolean hasRoot() {
        return data.getRoot() != null;
    }

    // ---- 道途与机制资源（服务端算好同步过来的，HUD 直接读） ----

    /** 主修序号（-1 = 尚未择道）。 */
    private static int pathOrdinal = -1;
    /** 机制资源快照：激荡 / 魔气 / 血池 / 阵域（0~1）。 */
    private static float[] resources = new float[4];
    /** 状态旗标：bit0 激荡引爆、bit1 锁血、bit2 阵域展开、bit3 缓伤池有货。 */
    private static int flags = 0;

    /** 道途摘要（服务端算好发过来的，格式见 Talents.clientSummary）。 */
    private static String pathSummary = "";

    public static void setPathState(int path, float[] res, int newFlags,
                                    String summary) {
        pathOrdinal = path;
        resources = res == null ? new float[4] : res;
        flags = newFlags;
        pathSummary = summary == null ? "" : summary;
    }

    /** 取摘要的某一段（0=道途名 1=可用点 2=总点 3=专属加成 4=护甲 5=武器 6=副业）。 */
    public static String summaryPart(int index) {
        if (pathSummary.isEmpty()) {
            return "";
        }
        String[] parts = pathSummary.split("\\|", -1);
        return index >= 0 && index < parts.length ? parts[index] : "";
    }

    public static int pathOrdinal() {
        return pathOrdinal;
    }

    /** 取某一项资源（0~1）；没择道时返回 0。 */
    public static float resource(int index) {
        return index >= 0 && index < resources.length ? resources[index] : 0.0F;
    }

    public static boolean flag(int bit) {
        return (flags & (1 << bit)) != 0;
    }

    // ---- 痛觉（纯客户端表现） ----

    public static void flashPain(int ticks) {
        painTicks = Math.max(painTicks, ticks);
    }

    public static void tickPain() {
        if (painTicks > 0) {
            painTicks--;
        }
    }

    public static int painTicks() {
        return painTicks;
    }

    public static float painIntensity() {
        return Math.min(1.0F, painTicks / 20.0F);
    }
}
