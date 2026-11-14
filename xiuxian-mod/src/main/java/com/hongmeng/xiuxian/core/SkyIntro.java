package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开局"高天云层之上"。
 *
 * <p><b>为什么这么做：</b>进世界后要弹难度选择界面，如果此时玩家站在地面上，
 * 界面背后就是真实地形——既没有仪式感，也容易让人以为"已经在玩了"。
 * 所以先把他送到**云层之上**（看不见世界），选完难度再落回地面。</p>
 *
 * <p>做法上刻意不用自定义 LoadingOverlay：那要替换 Forge 的加载界面、牵扯
 * 资源包与渲染时序，风险大。这里只用「传送 + 屏蔽伤害」两件事达成同样的观感，
 * 出问题也不会卡住加载。</p>
 *
 * <h3>状态</h3>
 * <ul>
 *   <li>{@link #PENDING} 里记着"这个玩家还悬在天上，以及他原本该落回哪"</li>
 *   <li>悬空期间：无重力、免伤、缓慢下落，所以在云上"飘"着而不是掉下去</li>
 *   <li>选完难度调用 {@link #descend}，落回地面并恢复正常</li>
 * </ul>
 */
public final class SkyIntro {

    /** 悬空的高度：原版云层在 y=192 附近，这里再高一些，确保看不到地面。 */
    public static final double SKY_Y = 320.0D;

    /** 悬空时给的缓降时长（ticks）。够玩家看完难度界面。 */
    private static final int FLOAT_TICKS = 20 * 300;

    /** 玩家 UUID → 他该落回的地面坐标。 */
    private static final Map<UUID, BlockPos> PENDING = new ConcurrentHashMap<>();

    private SkyIntro() {
    }

    /** 这个玩家是不是还在天上等选难度。 */
    public static boolean isWaiting(UUID id) {
        return PENDING.containsKey(id);
    }

    /**
     * 把刚进世界、还没选难度的玩家送上云层。
     *
     * @return true 表示这次真的送上去了
     */
    public static boolean lift(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        CultivationData data = CultivationCapability.get(player);
        if (data == null || data.hasChosenDifficulty()) {
            return false;       // 已经选过难度，不折腾
        }
        if (PENDING.containsKey(player.getUUID())) {
            return false;       // 已经在天上了
        }
        // 记下当前地面位置，选完难度落回这里
        PENDING.put(player.getUUID(), player.blockPosition());

        if (player.level() instanceof ServerLevel level) {
            player.teleportTo(level,
                    player.getX() + 0.5D, SKY_Y, player.getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
        }
        // 悬空：缓降 + 抗性，让他"飘"在云上而不是自由落体
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                FLOAT_TICKS, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                FLOAT_TICKS, 4, false, false, false));
        player.resetFallDistance();
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);

        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a7b\u00a7l云海之上"), false);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a77天地在你脚下铺开——先定下此世的\u00a7f难度\u00a77，再落回人间。"), false);
        HongmengMod.LOGGER.info("[hongmeng] 已将 {} 送到云层之上等待选择难度",
                player.getGameProfile().getName());
        return true;
    }

    /**
     * 选完难度，落回地面。
     *
     * @return true 表示这次真的送下去了
     */
    public static boolean descend(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        BlockPos back = PENDING.remove(player.getUUID());
        if (back == null) {
            return false;
        }
        // 找一个安全落点：从记录的 y 往上找到第一个不卡人的位置
        if (player.level() instanceof ServerLevel level) {
            int y = back.getY();
            for (int i = 0; i < 8; i++) {
                BlockPos candidate = new BlockPos(back.getX(), y + i, back.getZ());
                if (level.isEmptyBlock(candidate) && level.isEmptyBlock(candidate.above())) {
                    y = candidate.getY();
                    break;
                }
            }
            player.teleportTo(level,
                    back.getX() + 0.5D, y, back.getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
        }
        // 撤掉悬空用的效果
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.resetFallDistance();
        // 落点演出：一圈粒子 + 音效
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.3D, player.getZ(),
                    40, 0.6D, 0.3D, 0.6D, 0.05D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.7F);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a7a你落回了大地。\u00a77修行，从这里开始。"), false);
        return true;
    }

    /** 玩家离线时清掉记录，避免内存泄漏。 */
    public static void forget(UUID id) {
        PENDING.remove(id);
    }

    /** 还有多少人在天上（自检用）。 */
    public static int waitingCount() {
        return PENDING.size();
    }
}
