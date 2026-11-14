package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.combat.CombatFeel;
import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;

/**
 * 屏幕震动 + FOV 冲击（{@code docs/combat-feel-spec.md} §二）。
 *
 * <h2>三条硬约束，逐条落地</h2>
 *
 * <ol>
 *   <li><b>必须能关</b>（无障碍，不是可选项）：{@link #enabled()} 读的是
 *       <b>客户端</b>配置 {@code HongmengConfig.CLIENT.screenShake}，
 *       并且 {@link #setEnabled(boolean)} 会把它写回 {@code hongmeng-client.toml}。
 *       R 面板（修行面板）上的那个按钮调的就是它——见 {@code client.CombatFeelPanel}。
 *       <b>关掉之后</b>，服务端那些"命中事实"包照收（它们本来就不是"震动指令"），
 *       只是这里直接返回，一点都不震。</li>
 *   <li><b>纯客户端</b>：震动强度由本地的 {@link CombatFeel} 表算，
 *       服务端从不发"给我震几下"这种指令。</li>
 *   <li><b>衰减曲线</b>：{@link CombatFeel#decay(float, int, int)}
 *       （{@code strength * (1 - t/duration)}），不是突然停。</li>
 * </ol>
 *
 * <h2>为什么帧间要插值</h2>
 *
 * <p>相机角度是<b>每帧</b>算的，而震动是按 tick 推进的。直接按 tick 取值，
 * 高帧率下会看到"一顿一顿"的跳变。所以这里把相邻两 tick 的偏移按
 * {@code partialTick} 线性插值——震动是噪声，插值不会削弱它，只会让它更"晃"。</p>
 *
 * <p><b>2026-11-07：</b>玩家要求取消所有画面震动，本类已加
 * {@code FORCE_DISABLED = true} 硬开关；{@link #punch}/{@link #punchFov}
 * 以及相机/FOV 钩子全部直接返回，配置和 R 面板也打不开。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CombatShake {

    /** 当前震动的振幅（度）与时长。 */
    private static float amplitude = 0.0F;
    private static int duration = 0;
    private static int elapsed = 0;
    private static int seed = 1;

    /** FOV 冲击。 */
    private static float fovPunch = 0.0F;
    private static int fovElapsed = 0;

    /** 由 R 面板写下的运行时覆盖（也是"配置读不到"时的兜底）。 */
    private static Boolean override = null;

    /**
     * 玩家 2026-11-07 要求：<b>取消玩家画面震动，任何一项都关掉。</b>
     *
     * <p>这是最高优先级硬开关：命中/挨打/Boss常驻/Boss招式/法术特效/领域/
     * Boss登场等所有 {@link #punch} / {@link #punchFov} 全部统一失效；
     * 客户端配置与 R 面板的开关都不再能打开它。改回 {@code false} 即恢复。</p>
     */
    private static final boolean FORCE_DISABLED = true;

    private CombatShake() {
    }

    // ------------------------------------------------------------------
    // 开关（无障碍）
    // ------------------------------------------------------------------

    /** 屏幕震动开着吗？ */
    public static boolean enabled() {
        if (FORCE_DISABLED) {
            return false;
        }
        if (override != null) {
            return override;
        }
        try {
            return HongmengConfig.CLIENT.screenShake.get();
        } catch (Throwable t) {
            return true;
        }
    }

    /** 是否被本版硬开关强制关闭（R 面板用来显示"不可开启"）。 */
    public static boolean forcedDisabled() {
        return FORCE_DISABLED;
    }

    /** 强度倍率（0 = 关，1 = 默认，上限 2）。 */
    public static double strength() {
        try {
            double v = HongmengConfig.CLIENT.screenShakeStrength.get();
            if (Double.isNaN(v) || v < 0.0D) {
                return 1.0D;
            }
            return Math.min(2.0D, v);
        } catch (Throwable t) {
            return 1.0D;
        }
    }

    /**
     * 开/关屏幕震动，并<b>写回配置文件</b>。
     *
     * <p>写文件失败（只读目录、配置还没加载）也不影响本局：运行时覆盖已经生效。</p>
     */
    public static void setEnabled(boolean value) {
        if (FORCE_DISABLED) {
            override = false;
            return;
        }
        override = value;
        try {
            HongmengConfig.CLIENT.screenShake.set(value);
            HongmengConfig.CLIENT_SPEC.save();
        } catch (Throwable ignored) {
            // 配置写不进去也要能用：override 已经记住了
        }
    }

    /** 切换并返回新状态。 */
    public static boolean toggle() {
        if (FORCE_DISABLED) {
            return false;
        }
        boolean next = !enabled();
        setEnabled(next);
        return next;
    }

    // ------------------------------------------------------------------
    // 触发
    // ------------------------------------------------------------------

    /** 按档位震一下。 */
    public static void punch(CombatFeel.Shake tier, double extraMultiplier) {
        if (FORCE_DISABLED || tier == null || tier.isNone()) {
            return;
        }
        float amp = CombatFeel.shakeAmplitude(tier, strength() * Math.max(0.0D, extraMultiplier));
        if (amp <= 0.0F) {
            return;
        }
        // 取"更强的那一个"：连续命中不会互相抵消，也不会叠成地震
        if (amp >= currentAmplitude() || elapsed >= duration) {
            amplitude = amp;
            duration = tier.ticks();
            elapsed = 0;
            seed = (int) (System.nanoTime() & 0x7FFFFFFF);
        }
    }

    /** FOV 冲击（比例：0.06 = 拉近 6%）。 */
    public static void punchFov(float amount) {
        if (FORCE_DISABLED || amount <= 0.0F) {
            return;
        }
        fovPunch = Math.max(fovPunch, Math.min(0.20F, amount));
        fovElapsed = 0;
    }

    /** 每 tick 推进。 */
    public static void tick() {
        if (elapsed < duration) {
            elapsed++;
        } else {
            amplitude = 0.0F;
        }
        if (fovElapsed < CombatFeel.FOV_PUNCH_TICKS) {
            fovElapsed++;
        } else {
            fovPunch = 0.0F;
        }
    }

    public static void clear() {
        amplitude = 0.0F;
        duration = 0;
        elapsed = 0;
        fovPunch = 0.0F;
        fovElapsed = 0;
    }

    /** 当前振幅（诊断/HUD）。 */
    public static float currentAmplitude() {
        return CombatFeel.decay(amplitude, elapsed, duration);
    }

    // ------------------------------------------------------------------
    // 钩子
    // ------------------------------------------------------------------

    /**
     * 相机角度（{@code ViewportEvent.ComputeCameraAngles}）。
     *
     * <p>三根轴一起晃，但 roll 只吃 0.6 倍：roll 最容易让人晕，
     * 而这条链路上"能关"只是底线，默认值本身也不该让人难受。</p>
     */
    public static void applyCamera(ViewportEvent.ComputeCameraAngles event) {
        if (!enabled()) {
            return;
        }
        float amp = amplitude;
        if (amp <= 0.0F || duration <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float yaw = offset(amp, elapsed, duration, seed, 0, partial) * 1.0F;
        float pitch = offset(amp, elapsed, duration, seed, 1, partial) * 0.8F;
        float roll = offset(amp, elapsed, duration, seed, 2, partial) * 0.6F;

        event.setYaw(event.getYaw() + yaw);
        event.setPitch(Mth.clamp(event.getPitch() + pitch, -90.0F, 90.0F));
        event.setRoll(event.getRoll() + roll);
    }

    /** 相邻两 tick 之间插值，避免高帧率下的"跳变"。 */
    private static float offset(float amp, int tick, int total, int seed, int axis, float partial) {
        float now = CombatFeel.shakeOffset(amp, tick, total, seed, axis);
        float next = CombatFeel.shakeOffset(amp, tick + 1, total, seed, axis);
        return Mth.lerp(partial, now, next);
    }

    /** FOV 冲击（{@code ComputeFovModifierEvent}）。 */
    public static void applyFov(ComputeFovModifierEvent event) {
        // 玩家 2026-11-07：所有画面震动/FOV 冲击统一关闭（FORCE_DISABLED）。
        if (FORCE_DISABLED) {
            return;
        }
        // 玩家反馈打怪时屏幕放大抖动就是这里 —— 默认关闭。
        // 想要更重的打击感：hongmeng-client.toml 里把 combat.fovPunch 打开。
        if (!com.hongmeng.xiuxian.compat.HongmengConfig.CLIENT.fovPunch.get()) {
            return;
        }

        if (fovPunch <= 0.0F || fovElapsed >= CombatFeel.FOV_PUNCH_TICKS) {
            return;
        }
        // 冲击也是衰减的（前 1 tick 最强，之后收回）
        float t = (float) fovElapsed / (float) CombatFeel.FOV_PUNCH_TICKS;
        float amount = fovPunch * (1.0F - t);
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F - amount));
    }
}
