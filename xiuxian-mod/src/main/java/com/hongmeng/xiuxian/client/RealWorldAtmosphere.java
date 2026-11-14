package com.hongmeng.xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;

/**
 * <b>「真实世界」开启后的维度外观变化</b>。
 *
 * <h2>为什么需要这个类</h2>
 * <p>玩家原话：「选择开启后这可以做个过场动画，<b>所有维度发生变化</b>」。
 * 我上一轮只做了怪物段位与过场，**维度看起来还是原来的样子** —— 这个类补上。</p>
 *
 * <h2>能改什么、不能改什么（先查过 Forge 的钩子）</h2>
 * <p>可用的只有雾相关的两个事件（已解析 forge jar 确认方法名）：</p>
 * <ul>
 *   <li>{@code ViewportEvent.ComputeFogColor}：{@code getRed/Green/Blue} + {@code set...}；</li>
 *   <li>{@code ViewportEvent.RenderFog}：{@code getMode()} 返回原版的
 *       {@code FogRenderer.FogMode}（用它判断"是不是地形雾"）、
 *       {@code setNearPlaneDistance / setFarPlaneDistance / setFogShape}。</li>
 * </ul>
 * <p><b>天空颜色没有事件钩子</b>（forge 1.20.1 的 {@code ViewportEvent} 只有这四个子类）。
 * 所以我<b>不动天空</b> —— 上一版 {@code BossEntranceOverlay} 的注释里也已经写过
 * "不动天空渲染器，收益不值那个复杂度"。改用雾 + 屏幕叠加层来传达变化，
 * 这两条都是纯官方钩子，兼容性风险最低。</p>
 *
 * <h2>每个维度给什么"真貌"</h2>
 * <table border="1">
 *   <caption>维度 → 真实世界外观</caption>
 *   <tr><th>维度</th><th>雾色</th><th>雾距</th><th>感觉</th></tr>
 *   <tr><td>主世界</td><td>压暗的冷灰蓝</td><td>收近 35%</td><td>天光变了，远处看不清</td></tr>
 *   <tr><td>下界</td><td>暗红</td><td>收近 30%</td><td>血雾</td></tr>
 *   <tr><td>末地</td><td>深紫</td><td>收近 40%</td><td>虚空压得更近</td></tr>
 *   <tr><td>四个首领维度</td><td>各自主题的浓雾</td><td>收近 45%</td><td>魔域显形</td></tr>
 * </table>
 */
@OnlyIn(Dist.CLIENT)
public final class RealWorldAtmosphere {

    private RealWorldAtmosphere() {
    }

    /** 客户端缓存的"真实世界是否已开启"（由 SyncCultivationPacket 带过来）。 */
    private static boolean active;

    /** 开启那一刻的过渡进度（0=刚开、1=完全生效）—— 免得瞬间变色很突兀。 */
    private static float blend;

    public static void setActive(boolean v) {
        active = v;
    }

    public static boolean active() {
        return active;
    }

    /** 每 tick 推进过渡（由 CombatFeelClient 的客户端 tick 调）。 */
    public static void tick() {
        if (active && blend < 1.0F) {
            blend = Math.min(1.0F, blend + 0.012F);     // 约 4 秒过渡完
        } else if (!active && blend > 0.0F) {
            blend = Math.max(0.0F, blend - 0.05F);
        }
    }

    /** 当前生效强度（0~1）。 */
    public static float strength() {
        return blend;
    }

    // ------------------------------------------------------------------
    // 颜色配方
    // ------------------------------------------------------------------

    /** 一个维度的"真貌"配方。 */
    private record Look(float r, float g, float b, float nearScale, float farScale) {
    }

    private static final Look DEFAULT = new Look(0.20F, 0.22F, 0.30F, 0.65F, 0.62F);

    private static Look lookFor(ResourceLocation dim) {
        if (dim == null) {
            return DEFAULT;
        }
        String path = dim.getPath();
        if (path.contains("nether")) {
            return new Look(0.34F, 0.06F, 0.04F, 0.70F, 0.68F);
        }
        if (path.contains("end")) {
            return new Look(0.16F, 0.08F, 0.26F, 0.60F, 0.58F);
        }
        if (path.contains("void")) {
            return new Look(0.10F, 0.06F, 0.22F, 0.55F, 0.52F);
        }
        if (path.contains("demon")) {
            return new Look(0.30F, 0.05F, 0.06F, 0.55F, 0.52F);
        }
        if (path.contains("beast")) {
            return new Look(0.28F, 0.22F, 0.10F, 0.60F, 0.58F);
        }
        if (path.contains("epoch")) {
            return new Look(0.24F, 0.24F, 0.16F, 0.60F, 0.58F);
        }
        return DEFAULT;                                  // 主世界与其它
    }

    private static Look currentLook() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return DEFAULT;
        }
        return lookFor(mc.level.dimension().location());
    }

    // ------------------------------------------------------------------
    // 事件钩子
    // ------------------------------------------------------------------

    /** 雾色：向"真貌"色靠拢（按过渡强度混合）。 */
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!active || blend <= 0.001F) {
            return;
        }
        Look l = currentLook();
        float k = blend;
        event.setRed(Mth.lerp(k, event.getRed(), l.r()));
        event.setGreen(Mth.lerp(k, event.getGreen(), l.g()));
        event.setBlue(Mth.lerp(k, event.getBlue(), l.b()));
    }

    /**
     * 雾距：收近。
     *
     * <p>只改<b>地形雾</b>（{@code FogMode.TERRAIN}）——
     * 水下的雾、失明效果的雾都各有用意，动它们会出怪问题。</p>
     */
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!active || blend <= 0.001F) {
            return;
        }
        try {
            if (event.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) {
                return;
            }
        } catch (Throwable t) {
            return;
        }
        Look l = currentLook();
        float k = blend;
        float near = event.getNearPlaneDistance();
        float far = event.getFarPlaneDistance();
        event.setNearPlaneDistance(Mth.lerp(k, near, near * l.nearScale()));
        event.setFarPlaneDistance(Mth.lerp(k, far, far * l.farScale()));
    }
}
