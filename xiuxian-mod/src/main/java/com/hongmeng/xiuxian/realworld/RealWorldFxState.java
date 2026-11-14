package com.hongmeng.xiuxian.realworld;

/**
 * <b>真世界氛围强度</b>（客户端共享状态）。
 *
 * <p>雾色、天空穹顶、远处的环境表现都要读同一个强度值 ——
 * 如果各自维护一份 {@code blend}，它们会<b>不同步</b>：
 * 天已经红了、雾还没变，观感就是"两个东西在打架"。</p>
 *
 * <p>放在 common 包（不 import 任何客户端类）：服务端也能读，不会 NoClassDefFound。
 * 只做一件事 —— 存一个 0~1 的数字。</p>
 */
public final class RealWorldFxState {

    /** 0 = 正常世界，1 = 完全进入真世界。 */
    private static volatile float strength = 0.0F;

    private RealWorldFxState() {
    }

    public static void setStrength(float value) {
        strength = Math.max(0.0F, Math.min(1.0F, value));
    }

    public static float strength() {
        return strength;
    }
}
