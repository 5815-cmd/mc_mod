package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.realworld.RealWorldFxState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>背景音乐导演</b>：按场景切换 4 首自制 BGM。
 *
 * <h2>判定（每 20 tick 一次）</h2>
 * <table border="1">
 *   <caption>优先级</caption>
 *   <tr><th>条件</th><th>曲目</th></tr>
 *   <tr><td>正在渡劫</td><td>{@code bgm_tribulation}</td></tr>
 *   <tr><td>48 格内有 Boss</td><td>{@code bgm_boss}</td></tr>
 *   <tr><td>真世界已开启</td><td>{@code bgm_dark}</td></tr>
 *   <tr><td>其它</td><td>{@code bgm_calm}</td></tr>
 * </table>
 *
 * <h2>为什么用带 looping 的构造器</h2>
 * <p>vanilla 的 {@code forMusic} <b>不循环</b>（循环由 {@code MusicManager} 管）。
 * 我们没接那套管理器，所以直接指定 {@code looping = true}，
 * 由客户端自己循环 —— 不用自己算"放完了没"。</p>
 *
 * <h2>为什么每 20 tick 才判一次</h2>
 * <p>逐 tick 扫描"附近有没有 Boss"是浪费；音乐切换本来也不需要 1 tick 精度。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class MusicDirector {

    /** 判定间隔（tick）。 */
    private static final int INTERVAL = 20;

    /** 播放音量（客户端还会再乘 MUSIC 分类音量）。 */
    private static final float VOLUME = 0.45F;

    /** 判定半径：附近有 Boss 就切战斗曲。 */
    private static final double BOSS_RADIUS = 48.0D;

    private static SoundInstance current = null;
    private static String currentKey = "";

    private MusicDirector() {
    }

    /** 由 ClientSetup 的 ClientTick 调用。 */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stop();
            currentKey = "";
            return;
        }
        if (mc.player.tickCount % INTERVAL != 0) {
            return;
        }
        String want = decide(mc);
        if (want.equals(currentKey)) {
            return;
        }
        play(mc, want);
    }

    /** 决定该放哪首。 */
    private static String decide(Minecraft mc) {
        // ① 渡劫中
        if (TribulationSkyRenderer.active()) {
            return "bgm_tribulation";
        }
        // ② 附近有 Boss
        try {
            var box = mc.player.getBoundingBox().inflate(BOSS_RADIUS);
            for (var e : mc.level.getEntitiesOfClass(
                    com.hongmeng.xiuxian.entity.CultivationMonsterEntity.class, box)) {
                if (e.isAlive() && e.effectiveRank().hasBossBar()) {
                    return "bgm_boss";
                }
            }
        } catch (Throwable ignored) {
            // 判定失败按下一档
        }
        // ③ 真世界
        if (RealWorldFxState.strength() > 0.5F) {
            return "bgm_dark";
        }
        return "bgm_calm";
    }

    /** 换曲：先停旧的，再放新的。 */
    private static void play(Minecraft mc, String key) {
        stop();
        SoundEvent ev = switch (key) {
            case "bgm_tribulation" ->
                    com.hongmeng.xiuxian.ModSounds.BGM_TRIBULATION.get();
            case "bgm_boss" -> com.hongmeng.xiuxian.ModSounds.BGM_BOSS.get();
            case "bgm_dark" -> com.hongmeng.xiuxian.ModSounds.BGM_DARK.get();
            default -> com.hongmeng.xiuxian.ModSounds.BGM_CALM.get();
        };
        try {
            current = new SimpleSoundInstance(
                    ev.getLocation(), SoundSource.MUSIC, VOLUME, 1.0F,
                    RandomSource.create(), true, 0,
                    SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
            mc.getSoundManager().play(current);
            currentKey = key;
        } catch (Throwable ignored) {
            // 播不出来就静音，不影响游戏
        }
    }

    /** 停止当前曲目。 */
    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (current != null && mc.getSoundManager() != null) {
            try {
                mc.getSoundManager().stop(current);
            } catch (Throwable ignored) {
                // 停止失败忽略
            }
        }
        current = null;
    }

    /** 掉线/切世界时清空。 */
    public static void reset() {
        stop();
        currentKey = "";
    }
}
