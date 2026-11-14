package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.combat.CombatFeel;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.network.ClientCultivationCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 境界压制：压迫感 vignette + 心跳（{@code docs/combat-feel-spec.md} §3.1）。
 *
 * <h2>规格为什么说"只做数值玩家感受不到"</h2>
 *
 * <p>怪物那边已经有"玩家境界低于怪物境界 → 怪物减伤"的数值压制
 * （{@code MobRank.suppressionMultiplier}，另一个代理在做）。
 * 但玩家看到的只是"伤害数字变小了"——那是<b>挫败</b>，不是<b>压迫</b>。
 * 所以这一半必须把"实力差距"画出来：屏幕边缘变暗、心跳变快、准星发抖。</p>
 *
 * <h2>数据从哪来（一个包都不发）</h2>
 *
 * <p>怪物的境界是 {@code CultivationMonsterEntity.DATA_REALM_TIER}——
 * 一个 {@code SynchedEntityData}，<b>早就同步到客户端了</b>（名牌上那个
 * {@code [化神]} 就是读它画的）。玩家自己的境界在
 * {@code ClientCultivationCache} 里。两个数都在客户端手边，
 * 所以这一整套<b>完全本地推导</b>：不发包、不占带宽、也没有"别人的怪物压制我"的错位。</p>
 *
 * <h2>强度曲线</h2>
 *
 * <p>境界差 {@code 1/2/3/4/≥5} → 压迫强度 {@code 0.25/0.45/0.62/0.80/1.00}
 * （{@link CombatFeel#oppressionIntensity(int, int)}，与减伤表同调）。
 * 再乘一个距离衰减（贴脸 1.0，32 格外 0.25）——远处的高阶怪只是"背景的压力"。</p>
 *
 * <p>上升快（0.25/tick）、回落慢（0.06/tick）：被压制是"一下子压上来"的，
 * 而"松一口气"要慢一点，不然打跑一只怪画面立刻恢复明亮就一点都不压迫了。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class OppressionOverlay {

    /** 扫描半径（格）。 */
    public static final double SCAN_RADIUS = 32.0D;

    /** 半径边缘的强度系数（再远就忽略）。 */
    private static final float EDGE_FACTOR = 0.25F;

    /** 强度上升 / 回落速度（每 tick）。 */
    private static final float RISE_PER_TICK = 0.25F;
    private static final float FALL_PER_TICK = 0.06F;

    /** 当前显示强度 0~1 与目标强度。 */
    private static float shown = 0.0F;
    private static float target = 0.0F;

    /** 压制来源（"谁在压我"——用于 HUD 上写名字）。 */
    private static String sourceName = "";
    private static int sourceGap = 0;

    /** 心跳计时。 */
    private static int heartbeatCooldown = 0;
    private static float beatPhase = 0.0F;

    /** 扫描节流：每 {@value #SCAN_INTERVAL} tick 才真的遍历一次实体。 */
    private static final int SCAN_INTERVAL = 4;
    private static int scanCooldown = 0;

    private OppressionOverlay() {
    }

    /** 特效开关（客户端配置：晕 3D 的玩家可以只关这个）。 */
    public static boolean enabled() {
        try {
            return com.hongmeng.xiuxian.compat.HongmengConfig.CLIENT.oppressionEffects.get();
        } catch (Throwable t) {
            return true;
        }
    }

    /** 当前压迫强度（HUD/诊断）。 */
    public static float intensity() {
        return shown;
    }

    /** 压制来源的名字（没有就是空串）。 */
    public static String sourceName() {
        return sourceName;
    }

    /** 境界差（0 表示没有被压制）。 */
    public static int sourceGap() {
        return sourceGap;
    }

    // ------------------------------------------------------------------
    // 每 tick
    // ------------------------------------------------------------------

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.isPaused()) {
            reset();
            return;
        }
        if (!enabled()) {
            reset();
            return;
        }
        // 扫描本身要遍历实体、还要给每只怪拼一次名字（getName 每次都新建 Component），
        // 所以每 4 tick 才做一次；插值（上升快回落慢）仍然每 tick 走，看起来是连续的。
        if (scanCooldown > 0) {
            scanCooldown--;
        } else {
            scanCooldown = SCAN_INTERVAL;
            target = scan(level, player);
        }
        // 上升快、回落慢
        if (target > shown) {
            shown = Math.min(target, shown + RISE_PER_TICK);
        } else {
            shown = Math.max(target, shown - FALL_PER_TICK);
        }
        beatPhase = (beatPhase + 0.08F + 0.22F * shown) % 1.0F;
        tickHeartbeat(level, player);
    }

    /** 扫出当前最强的那股压力。 */
    private static float scan(ClientLevel level, LocalPlayer player) {
        int myTier = 1;
        try {
            myTier = ClientCultivationCache.get().realm().tier();
        } catch (Throwable ignored) {
            myTier = 1;
        }
        AABB box = player.getBoundingBox().inflate(SCAN_RADIUS);
        float best = 0.0F;
        String bestName = "";
        int bestGap = 0;
        for (CultivationMonsterEntity mob : level.getEntitiesOfClass(
                CultivationMonsterEntity.class, box)) {
            if (!mob.isAlive() || mob.isInvisible()) {
                continue;
            }
            int gap = mob.realmTier() - myTier;
            if (gap <= 0) {
                continue;
            }
            float base = CombatFeel.oppressionIntensity(myTier, mob.realmTier());
            // 品阶加成：头目 / 首领光是站在那儿就有气场
            if (mob.rank().hasBossBar()) {
                base = Math.min(1.0F, base + 0.15F);
            }
            float factor = CombatFeel.distanceFalloff(player.distanceTo(mob),
                    SCAN_RADIUS, EDGE_FACTOR);
            float value = base * factor;
            if (value > best) {
                best = value;
                bestName = mob.getName().getString();
                bestGap = gap;
            }
        }
        if (best <= 0.02F) {
            sourceName = "";
            sourceGap = 0;
            return 0.0F;
        }
        sourceName = bestName;
        sourceGap = bestGap;
        return Math.min(1.0F, best);
    }

    private static void reset() {
        target = 0.0F;
        shown = 0.0F;
        sourceName = "";
        sourceGap = 0;
        heartbeatCooldown = 0;
    }

    /**
     * 心跳：低频、慢速，越被压制跳得越快。
     *
     * <p>用原版 {@code WARDEN_HEARTBEAT}（低沉的一声"咚"），
     * 音量压低、音调也压低——它要像自己胸口的声音，而不是旁边的怪在敲鼓。</p>
     */
    private static void tickHeartbeat(ClientLevel level, LocalPlayer player) {
        if (shown <= 0.05F) {
            heartbeatCooldown = 0;
            return;
        }
        int interval = CombatFeel.heartbeatIntervalTicks(shown);
        if (interval <= 0) {
            return;
        }
        if (heartbeatCooldown > 0) {
            heartbeatCooldown--;
            return;
        }
        heartbeatCooldown = interval;
        float volume = 0.35F + 0.45F * shown;
        // 音调随强度略微下沉：越压迫越"闷"
        float pitch = 0.85F - 0.20F * shown;
        level.playLocalSound(player.getX(), player.getY() + 1.0D, player.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, volume, pitch, false);
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    /**
     * 屏幕边缘暗化 + 准星抖动。
     *
     * <p>暗化用四条 {@code fillGradient} 边带（上下左右），中间留空——
     * 玩家还要看得清敌人在哪。"压迫"是视野被收紧，不是被糊住。</p>
     */
    public static void render(GuiGraphics graphics, int width, int height) {
        if (shown <= 0.01F) {
            return;
        }
        float pulse = 1.0F - 0.25F * (float) Math.abs(Math.sin(beatPhase * Math.PI));
        float alpha = CombatFeel.vignetteAlpha(shown) * pulse;
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        if (a <= 2) {
            return;
        }
        // 暗红到黑：仙侠里的"气场"是压下来的赤黑色，不是纯黑
        int edge = (a << 24) | 0x1A0308;
        int mid = (a / 3 << 24) | 0x1A0308;
        // 边带厚度：原来 /5（1080p 下一条边 216px），四条边把视野糊掉一大半，
        // 玩家反馈「红框太大遮挡视野」。改成 /12（1080p → 90px），
        // 只收紧视野边缘，不糊画面。
        int band = Math.max(12, Math.min(width, height) / 12);

        graphics.fillGradient(0, 0, width, band, edge, mid);                       // 上
        graphics.fillGradient(0, height - band, width, height, mid, edge);         // 下
        graphics.fillGradient(0, 0, band, height, edge, mid);                      // 左
        graphics.fillGradient(width - band, 0, width, height, mid, edge);          // 右

        // 四角再压一层（vignette 的"收紧"感主要来自四角）
        int corner = a / 2 << 24 | 0x1A0308;
        graphics.fillGradient(0, 0, band, band, corner, mid);
        graphics.fillGradient(width - band, 0, width, band, mid, corner);
        graphics.fillGradient(0, height - band, band, height, mid, corner);
        graphics.fillGradient(width - band, height - band, width, height, corner, mid);

        if (shown > 0.3F) {
            drawJitteredCrosshair(graphics, width, height, shown);
        }
    }

    /**
     * 准星轻微抖动。
     *
     * <p>不改原版准星的渲染（那是原版代码），而是在它上面叠画一圈很短的四条线，
     * 位置按强度随机抖。看起来就是"手在抖"。</p>
     */
    private static void drawJitteredCrosshair(GuiGraphics graphics, int width, int height,
                                              float strength) {
        float amp = 1.0F + 2.4F * strength;
        long tick = Minecraft.getInstance().level == null ? 0L
                : Minecraft.getInstance().level.getGameTime();
        // 抖动：两个互质频率的正弦（同一 tick 内稳定，跨 tick 跳变——这就是"抖"）
        float dx = (float) Math.sin(tick * 1.7D) * amp;
        float dy = (float) Math.cos(tick * 2.3D) * amp;
        int cx = width / 2 + Math.round(dx);
        int cy = height / 2 + Math.round(dy);
        int color = 0x60FF6666;
        graphics.fill(cx - 5, cy, cx - 2, cy + 1, color);
        graphics.fill(cx + 2, cy, cx + 5, cy + 1, color);
        graphics.fill(cx, cy - 5, cx + 1, cy - 2, color);
        graphics.fill(cx, cy + 2, cx + 1, cy + 5, color);
    }
}
