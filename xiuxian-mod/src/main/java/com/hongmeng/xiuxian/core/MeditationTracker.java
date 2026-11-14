package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.ModSounds;
import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 打坐吐纳：坐在蒲团上，修为按秒往上走。
 *
 * <p>原文里主角其实不爱打坐（"不爱加班的懒虫"），所以打坐只是最稳的那条路，
 * 不是唯一的路——杀怪、任务、奇遇、丹药都在别的类里结算。</p>
 */
public final class MeditationTracker {

    private static final Map<UUID, Double> ACCUMULATOR = new HashMap<>();
    private static final Map<UUID, Integer> SOUND_TIMER = new HashMap<>();

    public static void track(ServerPlayer player) {
        double rate = HongmengConfig.SERVER.clampedMeditation();
        if (rate <= 0.0D) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        if (!data.isAwakened() || data.isMaxRealm()) {
            return;
        }

        double perTick = rate / 20.0D;

        // 子时加成
        if (HongmengConfig.SERVER.meditationAtNightBonus.get()) {
            long time = player.level().getDayTime() % 24000L;
            if (time >= 13000L && time <= 23000L) {
                perTick *= HongmengConfig.SERVER.nightBonusMultiplier.get();
            }
        }

        // 灵根加成**不在这里乘** —— 已统一上移到 CultivationSource.grant()，
        // 否则打坐会乘两次（杀怪、采药等来源又完全没乘）。

        double acc = ACCUMULATOR.getOrDefault(player.getUUID(), 0.0D) + perTick;
        if (acc >= 1.0D) {
            long whole = (long) acc;
            acc -= whole;
            CultivationSource.MEDITATION.grant(player, whole, false);
        }
        ACCUMULATOR.put(player.getUUID(), acc);

        // 表现
        if (player.level() instanceof ServerLevel level) {
            if (player.tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.ENCHANT,
                        player.getX(), player.getY() + 0.9D, player.getZ(),
                        2, 0.35D, 0.2D, 0.35D, 0.02D);
            }
            int timer = SOUND_TIMER.getOrDefault(player.getUUID(), 0) + 1;
            if (timer >= 100) {
                timer = 0;
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.MEDITATION.get(), SoundSource.PLAYERS, 0.35F, 1.0F);
            }
            SOUND_TIMER.put(player.getUUID(), timer);
        }
    }

    public static void stop(ServerPlayer player) {
        ACCUMULATOR.remove(player.getUUID());
        SOUND_TIMER.remove(player.getUUID());
    }

    /** 玩家是否坐在蒲团上（脚下是蒲团且处于坐姿）。 */
    public static boolean onCushion(ServerPlayer player) {
        BlockPos below = player.blockPosition().below();
        return player.level().getBlockState(below).is(
                com.hongmeng.xiuxian.ModBlocks.MEDITATION_CUSHION.get())
                || player.level().getBlockState(player.blockPosition()).is(
                com.hongmeng.xiuxian.ModBlocks.MEDITATION_CUSHION.get());
    }

    /** 给个"灵草在旁边加成打坐"的小彩蛋。 */
    public static double nearbySpiritGrassBonus(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 1.0D;
        }
        int count = 0;
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, 2, 3))) {
            if (level.getBlockState(pos).is(com.hongmeng.xiuxian.ModBlocks.SPIRIT_GRASS.get())) {
                count++;
            }
        }
        if (count == 0) {
            return 1.0D;
        }
        return 1.0D + Math.min(0.5D, count * 0.05D);
    }

    /** 附近有没有水（"临水而修"）。 */
    public static boolean nearWater(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -2, -4), origin.offset(4, 1, 4))) {
            if (level.getBlockState(pos).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }
}
