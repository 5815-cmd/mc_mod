package com.hongmeng.xiuxian.network;

/** 痛觉强度（纯客户端，供画面晃动与红色遮罩使用）。 */
public final class ClientPainState {

    private static float intensity = 0.0F;
    private static int hold = 0;

    private ClientPainState() {
    }

    public static void setIntensity(float value) {
        intensity = Math.max(0.0F, Math.min(3.0F, value));
        hold = 14;
    }

    public static void tick() {
        if (hold > 0) {
            hold--;
        } else {
            intensity *= 0.86F;
            if (intensity < 0.01F) {
                intensity = 0.0F;
            }
        }
    }

    public static float intensity() {
        return intensity;
    }
}
