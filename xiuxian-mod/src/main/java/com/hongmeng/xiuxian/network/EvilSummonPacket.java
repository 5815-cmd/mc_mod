package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.block.EvilAltarBlockEntity;
import com.hongmeng.xiuxian.realworld.EvilSummon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>「召唤」按钮</b>（客户端 → 服务端）。
 *
 * <p>服务端<b>必须重新校验</b>五格是否就绪 —— 客户端说"我能点"不算数，
 * 否则改个包就能白嫖召唤。</p>
 */
public class EvilSummonPacket {

    private final BlockPos pos;

    public EvilSummonPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(EvilSummonPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }

    public static EvilSummonPacket decode(FriendlyByteBuf buf) {
        return new EvilSummonPacket(buf.readBlockPos());
    }

    public static void handle(EvilSummonPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            var level = player.serverLevel();
            if (player.distanceToSqr(p.pos.getX() + 0.5D, p.pos.getY() + 0.5D,
                    p.pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[evil] 收到召唤请求 pos={} player={}", p.pos, player.getName().getString());
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a77祭坛开始共鸣……"), true);
            EvilAltarBlockEntity be = null;
            if (level.getBlockEntity(p.pos) instanceof EvilAltarBlockEntity direct) {
                be = direct;
            } else {
                // 兜底：坐标轻微错位时在 ±3 内找一台祭坛
                for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(
                        p.pos.offset(-3, -3, -3), p.pos.offset(3, 3, 3))) {
                    if (level.getBlockEntity(q) instanceof EvilAltarBlockEntity found) {
                        be = found;
                        break;
                    }
                }
            }
            if (be == null) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                        "[evil] 召唤失败：坐标 {} 附近没有祭坛", p.pos);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7c没有找到祭坛（请站在祭坛旁再试）"), true);
                return;
            }
            {
                if (!be.ready()) {
                    com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                            "[evil] 校验失败：五格未就绪");
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00a7c祭坛尚未集齐五个维度的凭证。"), true);
                    return;
                }
                be.consumeAll();
                player.closeContainer();
                EvilSummon.begin(player, level, p.pos);
            }
        });
        context.setPacketHandled(true);
    }
}
