package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.realworld.EvilSky;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>「恶」的全屏演出层</b>（第五轮升级）。
 *
 * <h2>四件事</h2>
 * <ol>
 *   <li><b>血红渐层 + 暗角</b>：只要 {@link EvilSky#strength()} &gt; 0（恶降临时），屏幕四边持续压暗成暗红，
 *       随强度/阶段加深（阶段 3 最重），像"被血雾包围"；</li>
 *   <li><b>阶段转场全屏冲击</b>：监听 {@link EvilSky#phase()} 0→1/1→2/2→3，触发一次全屏红白冲击
 *       （镜头震动 / FOV 已按玩家要求取消，仅保留画面冲击）；</li>
 *   <li><b>判定红闪</b>：恶的招式 active 帧由 {@code BossPresenceClient} 调
 *       {@link #triggerRedFlash()}，短促一层红闪；</li>
 *   <li><b>坠落速度线</b>：{@link EvilFallFx} 坠落过场期间从屏幕中心向外拉出速度线；</li>
 *   <li><b>阶段色带</b>：屏幕最上沿一条 4px 色带，随恶的阶段由红 → (紫) → 深红加深，
 *       在服务端 BOSS 条颜色的基础上再强化"阶段感"。</li>
 * </ol>
 *
 * <p>本类只读客户端状态（{@link EvilSky} = 服务端同步包写入），不改任何服务端逻辑。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EvilFlashOverlay {

    /** 召唤白闪（原有）。 */
    private static int remaining = 0;
    private static int interval = 6;
    private static int cooldown = 0;
    private static int frame = 0;

    /** 判定红闪（短促）帧数。 */
    private static int redFrame = 0;

    /** 阶段转场冲击剩余 tick 与总长。 */
    private static final int SHOCK_TOTAL = 30;
    private static int shockTicks = 0;

    /** 上一次看到的天空阶段（用于沿触发阶段冲击）。 */
    private static int lastSkyPhase = 0;

    /** 平滑后的强度（避免强度突变闪烁）。 */
    private static float bloodSmooth = 0.0F;

    private EvilFlashOverlay() {
    }

    // ------------------------------------------------------------------
    // 触发
    // ------------------------------------------------------------------

    /** 触发 {@code count} 次白闪（祭坛召唤演出用）。 */
    public static void trigger(int count, int intervalTicks) {
        remaining = Math.max(0, count);
        interval = Math.max(2, intervalTicks);
        cooldown = 0;
        frame = 0;
    }

    /** 判定红闪：恶的 active 帧闪一下（短促，不刺眼）。 */
    public static void triggerRedFlash() {
        redFrame = Math.max(redFrame, 3);
    }

    /** 阶段转场全屏冲击（天空阶段变化时由 tick 自动触发，也可被外部手动调用）。 */
    public static void triggerPhaseShock() {
        shockTicks = SHOCK_TOTAL;
        trigger(2, 3);
        // 2026-11-02 玩家要求：取消 Boss/恶 转阶段的运镜。
        // 保留全屏红白冲击与色带；镜头震动与 FOV 不再触发。
    }

    /** 当前是否有阶段冲击在播（供检查脚本/调试）。 */
    public static int shockTicks() {
        return shockTicks;
    }

    // ------------------------------------------------------------------
    // tick
    // ------------------------------------------------------------------

    /** 每客户端 tick 调一次（由 {@code CombatFeelClient} 转发）。 */
    public static void tick() {
        if (frame > 0) {
            frame--;
        }
        if (redFrame > 0) {
            redFrame--;
        }
        if (shockTicks > 0) {
            shockTicks--;
        }
        // ---- 天空阶段变化 → 全屏冲击 ----
        float s = EvilSky.strength();
        int skyPhase = EvilSky.phase();
        if (s <= 0.01F) {
            if (lastSkyPhase != 0) {
                lastSkyPhase = 0;
            }
            return;
        }
        if (skyPhase > lastSkyPhase && skyPhase > 0) {
            triggerPhaseShock();
        }
        lastSkyPhase = skyPhase;
        // ---- 召唤白闪节奏 ----
        if (remaining <= 0) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        remaining--;
        frame = 4;                  // 亮 4 帧
        cooldown = interval;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        float partial = (float) event.getPartialTick();
        float t = (mc.level.getGameTime() + partial) / 20.0F;
        float strength = EvilSky.strength();

        // ④ 坠落速度线（最底层）
        drawSpeedLines(g, w, h, partial);

        // ① 血红渐层 + 暗角
        bloodSmooth += ((strength > 0.01F ? strength : 0.0F) - bloodSmooth) * 0.10F;
        if (bloodSmooth > 0.01F) {
            drawBloodVignette(g, w, h, bloodSmooth, EvilSky.phase(), t);
            drawPhaseBand(g, w, h, EvilSky.phase());
        }

        // ② 阶段转场冲击
        if (shockTicks > 0) {
            float k = shockTicks / (float) SHOCK_TOTAL;
            float a = k * k * 0.62F;
            int argb = ((int) (a * 255.0F) << 24) | 0x00FF3038;
            g.fill(0, 0, w, h, argb);
            // 中心的白色余晖
            int a2 = (int) (k * 0.35F * 255.0F) << 24;
            int r = Math.min(w, h) / 3;
            g.fill(w / 2 - r, h / 2 - r, w / 2 + r, h / 2 + r, a2 | 0x00FFE8E8);
        }

        // ③ 判定红闪
        if (redFrame > 0) {
            float a = Mth.clamp(redFrame / 3.0F, 0.0F, 1.0F) * 0.26F;
            int argb = ((int) (a * 255.0F) << 24) | 0x00C81820;
            g.fill(0, 0, w, h, argb);
        }

        // 召唤白闪（原有行为，放最后覆盖）
        if (frame > 0) {
            float a = frame >= 3 ? 0.92F : Mth.clamp(frame / 4.0F, 0.0F, 1.0F) * 0.7F;
            int argb = ((int) (a * 255.0F) << 24) | 0x00FFF2F2;
            g.fill(0, 0, w, h, argb);
        }
    }

    /** 血红暗角：四边渐变，随强度/阶段加深 + 呼吸脉动。 */
    private static void drawBloodVignette(GuiGraphics g, int w, int h,
                                          float strength, int phase, float t) {
        float pulse = 0.80F + 0.20F * Mth.sin(t * 1.15F);
        float deep = phase >= 3 ? 1.0F : phase >= 2 ? 0.78F : 0.55F;
        int layers = 4;
        int bandW = Math.max(10, (int) (w * 0.20F * strength * deep));
        int bandH = Math.max(10, (int) (h * 0.22F * strength * deep));
        float baseA = (0.10F + 0.30F * strength) * deep * pulse;
        for (int i = 0; i < layers; i++) {
            float k = (layers - i) / (float) layers;          // 外圈更重
            int a = (int) (baseA * k * 255.0F / layers * 2.0F);
            if (a <= 0) {
                continue;
            }
            int argb = (Math.min(255, a) << 24) | 0x00500810;
            int dx = bandW * i / layers;
            int dy = bandH * i / layers;
            int tw = Math.max(1, bandW / layers);
            int th = Math.max(1, bandH / layers);
            g.fill(dx, dy, w - dx, dy + th, argb);           // 上
            g.fill(dx, h - dy - th, w - dx, h - dy, argb);   // 下
            g.fill(dx, dy, dx + tw, h - dy, argb);           // 左
            g.fill(w - dx - tw, dy, w - dx, h - dy, argb);   // 右
        }
    }

    /** 屏幕最上沿的阶段色带（红 → 更亮红 → 深红，随阶段加深）。 */
    private static void drawPhaseBand(GuiGraphics g, int w, int h, int phase) {
        int thick = Math.max(2, h / 260);
        int rgb;
        int alpha;
        switch (Math.max(1, Math.min(3, phase))) {
            case 3 -> {
                rgb = 0x8A0710;          // 阶段 3：深红
                alpha = 150;
            }
            case 2 -> {
                rgb = 0xC81420;          // 阶段 2：亮红
                alpha = 120;
            }
            default -> {
                rgb = 0xE02028;          // 阶段 1：红
                alpha = 95;
            }
        }
        int argb = (alpha << 24) | rgb;
        g.fill(0, 0, w, thick, argb);
    }

    /** 坠落速度线：从屏幕中心向四周拉出的虚线（读 EvilFallFx 的进度）。 */
    private static void drawSpeedLines(GuiGraphics g, int w, int h, float partial) {
        float k = EvilFallFx.speedLineStrength(partial);
        if (k <= 0.02F) {
            return;
        }
        int cx = w / 2;
        int cy = h / 2;
        float maxR = (float) Math.hypot(w, h) * 0.5F;
        int lines = 30;
        for (int i = 0; i < lines; i++) {
            double a = i * (Math.PI * 2.0D / lines) + (i % 2) * 0.11D;
            float start = maxR * 0.16F * (1.0F - k);
            float end = maxR * (0.42F + 0.62F * k);
            int alpha = (int) (130.0F * k
                    * (0.40F + 0.60F * ((i * 7) % 11) / 11.0F));
            if (alpha < 6) {
                continue;
            }
            int argb = (Math.min(255, alpha) << 24) | 0x00F0F0F5;
            int step = Math.max(4, (int) (maxR * 0.055F));
            int thick = Math.max(1, (int) (1.0F + 2.5F * k));
            for (float d = start; d < end; d += step) {
                int x = (int) (cx + Math.cos(a) * d);
                int y = (int) (cy + Math.sin(a) * d);
                if (x < 0 || y < 0 || x >= w || y >= h) {
                    continue;
                }
                g.fill(x, y, Math.min(w, x + thick), Math.min(h, y + thick), argb);
            }
        }
    }
}
