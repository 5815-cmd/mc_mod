package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.combat.CombatFeel;
import com.hongmeng.xiuxian.combat.VfxParticles;
import com.hongmeng.xiuxian.combat.ElementVfx;
import com.hongmeng.xiuxian.domain.DomainData;
import com.hongmeng.xiuxian.network.ClientCultivationCache;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 领域展开的那一刻（{@code docs/combat-feel-spec.md} §3.3）：一圈从脚下向外扩散的冲击环
 * + 一声悠长的"嗡" + 一次强震 + 元素色的画面一闪。
 *
 * <h2>与 {@code DomainClientVfx} 的分工</h2>
 *
 * <p>领域特效被拆成两件事，各写各的文件：</p>
 *
 * <ul>
 *   <li>{@code DomainClientVfx}（领域那一侧负责）：领域开着的时候<b>持续</b>的元素特效
 *       ——火雨、雨幕、藤蔓、细刃……那是"场地"；</li>
 *   <li>本类：<b>激活瞬间</b>的一次性演出——规格原话"领域激活瞬间应该是全场最华丽的一刻"。
 *       它是"事件"，只发生一次，20 tick 内放完。</li>
 * </ul>
 *
 * <p>触发也是纯本地的：领域开没开已经跟着 {@code SyncCultivationPacket} 同步过来了，
 * 客户端只要盯住 {@code false → true} 这个跳变即可，不需要任何新包。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class DomainOpeningVfx {

    /**
     * 冲击环扩散的总时长（tick）。
     *
     * <p>R2（玩家 2026-11-07「领域特效也是」）：20 → 16，演出收短一点，
     * 但仍然是"全场最华丽的一瞬"，音效与全屏闪都保留。</p>
     */
    public static final int RING_TICKS = 16;
    /** 每 tick 撒几颗环上粒子（R2：7 → 3，环还是看得出来是一圈）。 */
    private static final int RING_PER_TICK = 3;

    private static boolean wasActive = false;
    /** 进世界后的第一帧只是"读初值"，不算激活（否则顶着已开领域登录会白放一次演出）。 */
    private static boolean primed = false;
    private static int elapsed = -1;
    private static Element element = Element.NONE;
    private static double centerX;
    private static double centerY;
    private static double centerZ;

    /** 画面一闪的剩余 tick。 */
    private static int flashTicks = 0;
    private static int flashColor = 0xFFFFFF;

    private DomainOpeningVfx() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            wasActive = false;
            primed = false;
            elapsed = -1;
            flashTicks = 0;
            return;
        }
        boolean active = false;
        Element current = Element.NONE;
        try {
            DomainData domains = ClientCultivationCache.get().getDomainData();
            active = domains != null && domains.isDomainActive() && domains.domainCount() > 0;
            if (active && !domains.domains().isEmpty()) {
                current = domains.domains().get(0).element();
            }
        } catch (Throwable ignored) {
            active = false;
        }

        if (active && !wasActive && primed) {
            trigger(level, player, current);
        }
        wasActive = active;
        primed = true;

        if (elapsed >= 0) {
            emitRing(level, player);
            elapsed++;
            if (elapsed > RING_TICKS) {
                elapsed = -1;
            }
        }
        if (flashTicks > 0) {
            flashTicks--;
        }
    }

    /** 激活瞬间。 */
    private static void trigger(ClientLevel level, LocalPlayer player, Element current) {
        element = current;
        centerX = player.getX();
        centerY = player.getY();
        centerZ = player.getZ();
        elapsed = 0;
        flashTicks = 8;
        flashColor = current == null ? 0xFFFFFF : current.color();

        // 玩家 2026-11-02：除 Boss 召唤/登场与自己挨打外，其他镜头反馈全关。
        // 领域展开保留全屏闪与音效，不再推镜头。
        // 一声悠长的"嗡"
        level.playLocalSound(centerX, centerY + 1.0D, centerZ,
                SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 1.6F, 0.55F, false);
        level.playLocalSound(centerX, centerY + 1.0D, centerZ,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 0.6F, false);
    }

    /** 一圈从脚下向外扩散（半径随时间长大、粒子随时间变淡）。 */
    private static void emitRing(ClientLevel level, LocalPlayer player) {
        if (elapsed < 0) {
            return;
        }
        float t = CombatFeel.clamp01((float) elapsed / (float) RING_TICKS);
        double radius = 0.6D + 5.4D * t;
        ElementVfx.Flavor f = ElementVfx.of(element);
        for (int i = 0; i < RING_PER_TICK; i++) {
            double a = Math.PI * 2.0D * ((elapsed * RING_PER_TICK + i) % 48) / 48.0D;
            double x = centerX + Math.cos(a) * radius;
            double z = centerZ + Math.sin(a) * radius;
            level.addParticle(VfxParticles.of(f.afterglowParticle()),
                    x, centerY + 0.15D + 0.05D * (1.0F - t), z,
                    Math.cos(a) * 0.05D, 0.01D, Math.sin(a) * 0.05D);
            // R2：第二层（更亮的 trail 粒子）从 i%3==0 收到每 tick 最多 1 颗
            // （i == 0），环的主体仍然完整。
            if (i == 0) {
                level.addParticle(VfxParticles.of(f.trailParticle()),
                        x, centerY + 0.6D, z, 0.0D, 0.02D, 0.0D);
            }
        }
    }

    /** 元素色的整屏一闪（由 {@code RenderGuiEvent.Post} 画）。 */
    public static void render(net.minecraft.client.gui.GuiGraphics graphics, int width, int height) {
        if (flashTicks <= 0) {
            return;
        }
        float t = (float) flashTicks / 8.0F;
        // R2：全屏闪从 0.22F 降到 0.12F（保留"一闪"，但不再刺眼糊屏）。
        int a = (int) (t * 0.12F * 255.0F);
        if (a <= 2) {
            return;
        }
        graphics.fill(0, 0, width, height, (a << 24) | (flashColor & 0xFFFFFF));
    }

    /** 演出中吗（诊断）。 */
    public static boolean active() {
        return elapsed >= 0 || flashTicks > 0;
    }

    public static void clear() {
        elapsed = -1;
        flashTicks = 0;
        wasActive = false;
        primed = false;
    }
}
