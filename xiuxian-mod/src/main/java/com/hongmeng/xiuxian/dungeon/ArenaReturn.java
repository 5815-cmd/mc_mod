package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>进出 boss 维度的记录</b> —— 记住玩家是从哪儿来的，好把他送回哪儿去。
 *
 * <p>玩家要求：「打完最后一波自动传送到 boss 维度」——
 * 但**必须能回来**，否则玩家会被永久困在 boss 位面里（这是很容易漏掉的一环）。</p>
 *
 * <p>三个出口：</p>
 * <ol>
 *   <li><b>打完首领</b>：自动回到进入副本前的世界坐标；</li>
 *   <li><b>潜行 + 空手右键祭坛</b>：主动撤离（进度作废，但不会卡住）；</li>
 *   <li><b>首领把你打死了</b>：按原版重生规则处理（重生点在你自己的世界里）。</li>
 * </ol>
 */
public final class ArenaReturn {

    /** 玩家 → (来源维度, 来源坐标)。 */
    private static final Map<UUID, Entry> SAVED = new ConcurrentHashMap<>();

    private ArenaReturn() {
    }

    /** 来源记录。 */
    private record Entry(net.minecraft.resources.ResourceKey<Level> dim, Vec3 pos) {
    }

    /** 记下玩家当前的位置（在传送进 boss 维度**之前**调用）。 */
    public static void remember(ServerPlayer player) {
        if (player == null) {
            return;
        }
        SAVED.put(player.getUUID(), new Entry(player.level().dimension(),
                player.position()));
    }

    /**
     * 把玩家送回来源世界。
     *
     * @param reason 显示给玩家的原因（可为 null）
     * @return 是否成功送回（false = 没有记录，交给调用方兜底）
     */
    public static boolean sendBack(ServerPlayer player, String reason) {
        if (player == null || player.getServer() == null) {
            return false;
        }
        Entry e = SAVED.remove(player.getUUID());
        if (e == null) {
            return false;
        }
        ServerLevel target = player.getServer().getLevel(e.dim());
        if (target == null) {
            HongmengMod.LOGGER.warn("[arena] 来源维度未加载：{}", e.dim().location());
            return false;
        }
        try {
            // 立区块，免得落进未加载区域
            BlockPos p = BlockPos.containing(e.pos());
            int cx = p.getX() >> 4;
            int cz = p.getZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    target.getChunk(cx + dx, cz + dz,
                            net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
                }
            }
            player.teleportTo(target, e.pos().x, e.pos().y, e.pos().z,
                    player.getYRot(), player.getXRot());
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.hurtMarked = true;
            target.sendParticles(ParticleTypes.PORTAL, e.pos().x, e.pos().y + 1.0D,
                    e.pos().z, 80, 0.6D, 1.0D, 0.6D, 0.4D);
            target.playSound(null, p, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS,
                    1.0F, 1.2F);
            if (reason != null) {
                player.displayClientMessage(Component.literal(reason), false);
            }
            HongmengMod.LOGGER.info("[arena] {} 返回 {} {}", player.getName().getString(),
                    e.dim().location(), e.pos());
            return true;
        } catch (Throwable t) {
            HongmengMod.LOGGER.error("[arena] 返回失败", t);
            return false;
        }
    }

    /** 有没有记录（诊断用）。 */
    public static boolean has(UUID id) {
        return SAVED.containsKey(id);
    }

    public static void forget(ServerPlayer player) {
        if (player != null) {
            SAVED.remove(player.getUUID());
        }
    }

    public static void clear() {
        SAVED.clear();
    }
}
