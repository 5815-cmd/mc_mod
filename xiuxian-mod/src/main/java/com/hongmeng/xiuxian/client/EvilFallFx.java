package com.hongmeng.xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>坠落穿越</b>：传送前的一段"被吸进裂口"的表演（第五轮升级）。
 *
 * <p>客户端独演，四件事：</p>
 * <ol>
 *   <li><b>相机被强拽上仰 + 小幅摇摆</b>（角度由 {@link #cameraAngles(float)} 给
 *       {@link EvilFallCamera}，<b>缓入缓出</b>，不再突然拉满）；</li>
 *   <li><b>速度线</b>：{@link #speedLineStrength(float)} 给 {@link EvilFlashOverlay}
 *       画从屏幕中心向外拉出的速度线（越接近落点越密）；</li>
 *   <li><b>气流粒子</b>：周围持续向上的气流 + 尾部拖尾；</li>
 *   <li><b>风声</b>：开始一声低频风压（{@code STAGE_CHARGE}），70% 处补一声高频呼啸
 *       （{@code ENDER_DRAGON_FLAP} 调低音量/抬高音高），营造"越落越快"。</li>
 * </ol>
 */
@OnlyIn(Dist.CLIENT)
public final class EvilFallFx {

    private static int remaining = 0;
    private static int total = 0;
    private static boolean soundPlayed = false;
    private static boolean whistlePlayed = false;

    private EvilFallFx() {
    }

    /** 开始一段坠落演出（tick）。 */
    public static void begin(int ticks) {
        total = Math.max(1, ticks);
        remaining = total;
        soundPlayed = false;
        whistlePlayed = false;
    }

    public static boolean active() {
        return remaining > 0;
    }

    /** 0~1 的进度。 */
    public static float progress() {
        return total <= 0 ? 0.0F : 1.0F - (float) remaining / total;
    }

    /** 速度线强度 0~1（中后段最强；供 {@link EvilFlashOverlay} 画线）。 */
    public static float speedLineStrength(float partial) {
        if (remaining <= 0) {
            return 0.0F;
        }
        float p = Mth.clamp(progress() + partial / Math.max(1.0F, total), 0.0F, 1.0F);
        // 前 25% 淡入，70% 附近最密，尾段保持
        float rise = Mth.clamp(p / 0.25F, 0.0F, 1.0F);
        float hold = p < 0.85F ? 1.0F : (1.0F - (p - 0.85F) / 0.15F * 0.25F);
        return Mth.clamp(rise * hold, 0.0F, 1.0F);
    }

    /**
     * 相机附加角 {@code [pitch, roll]}（null = 没有演出）。
     *
     * <p>第五轮"更稳"：pitch 用 smoothstep 缓入到 -48°，roll 限制在 ±4.5° 并
     * 用两段正弦错相，避免旧版"越到后面越抖"的晕眩感；结束前 25% 缓慢回正。</p>
     */
    public static float[] cameraAngles(float partial) {
        if (remaining <= 0) {
            return null;
        }
        float p = Mth.clamp(progress() + partial / Math.max(1.0F, total), 0.0F, 1.0F);
        // smoothstep 缓入：前 40% 拉到 -48°，之后保持，最后 20% 回正 15%
        float in = Mth.clamp(p / 0.40F, 0.0F, 1.0F);
        float ease = in * in * (3.0F - 2.0F * in);
        float settle = p > 0.80F ? (p - 0.80F) / 0.20F : 0.0F;
        float pitch = -48.0F * ease * (1.0F - 0.25F * settle);
        // 摇摆：两个不同频率叠加，幅度小、平滑
        float roll = (Mth.sin(p * 9.0F) * 2.4F + Mth.sin(p * 23.0F + 1.3F) * 1.6F)
                * ease * (1.0F - 0.6F * settle);
        return new float[]{Mth.clamp(pitch, -60.0F, 60.0F), Mth.clamp(roll, -6.0F, 6.0F)};
    }

    /** 每客户端 tick（由 {@code CombatFeelClient} 转发）。 */
    public static void tick() {
        if (remaining <= 0) {
            return;
        }
        remaining--;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            remaining = 0;
            return;
        }
        // 风压音（开始那一下）
        if (!soundPlayed) {
            soundPlayed = true;
            try {
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(),
                        mc.player.getZ(),
                        com.hongmeng.xiuxian.ModSounds.STAGE_CHARGE.get(),
                        SoundSource.AMBIENT, 1.0F, 0.55F, false);
            } catch (Throwable ignored) {
                // 音效失败不影响
            }
        }
        // 70% 处第二声"呼啸"（把音调抬高、音量压低，避免刺耳）
        if (!whistlePlayed && progress() >= 0.70F) {
            whistlePlayed = true;
            try {
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(),
                        mc.player.getZ(), SoundEvents.ENDER_DRAGON_FLAP,
                        SoundSource.AMBIENT, 0.7F, 1.55F, false);
            } catch (Throwable ignored) {
                // 音效失败不影响
            }
        }
        // 气流粒子：沿身体向上掠过（客户端本地，不发包）
        float speed = 0.55F + 0.9F * progress();
        for (int i = 0; i < 5; i++) {
            double a = (mc.level.random.nextDouble() * 2.0D - 1.0D) * 1.6D;
            double b = (mc.level.random.nextDouble() * 2.0D - 1.0D) * 1.6D;
            mc.level.addParticle(ParticleTypes.CLOUD,
                    mc.player.getX() + a, mc.player.getY() - 0.8D + i * 0.35D,
                    mc.player.getZ() + b, 0.0D, speed, 0.0D);
        }
        if (remaining % 6 == 0) {
            mc.level.addParticle(ParticleTypes.END_ROD,
                    mc.player.getX(), mc.player.getY() + 1.2D, mc.player.getZ(),
                    0.0D, -1.2D, 0.0D);
        }
        if (remaining % 10 == 0) {
            mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    mc.player.getX(), mc.player.getY() + 0.4D, mc.player.getZ(),
                    0.0D, 1.6D, 0.0D);
        }
    }

    public static void stop() {
        remaining = 0;
        total = 0;
        soundPlayed = false;
        whistlePlayed = false;
    }
}
