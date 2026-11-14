package com.hongmeng.xiuxian.sect;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import com.hongmeng.xiuxian.core.QuestChain;
import com.hongmeng.xiuxian.entity.GuiltySectDiscipleEntity;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

/**
 * 门派玩法：入门、贡献、身份升级、叛门与清理门户。
 *
 * <p>原文里主角与千机门、神霄剑宗都有纠缠。这里把那份"入了门就有人管你、
 * 叛了门就有人杀你"的体感做出来。</p>
 */
public final class SectManager {

    /** 入门门槛：练气即可（出生后做完入门任务就能拜师）。 */
    public static final int JOIN_MIN_REALM_TIER = 1;

    /** 清理门户的冷却（tick）：10 分钟。 */
    private static final long HUNT_COOLDOWN = 20L * 60L * 10L;

    private SectManager() {
    }

    // ------------------------------------------------------------------
    // 入门 / 退门
    // ------------------------------------------------------------------

    public static boolean canJoin(ServerPlayer player, CultivationData data) {
        if (!data.isAwakened()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c灵根都没觉醒，报什么名？"));
            return false;
        }
        if (!data.sect().isNone()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c你已经是" + data.sect().colored() + "\u00a7c 的人了。"));
            return false;
        }
        if (data.realm().tier() < JOIN_MIN_REALM_TIER) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c境界太低，先引气入体再说。"));
            return false;
        }
        return true;
    }

    public static void join(ServerPlayer player, CultivationData data, Sect sect, String via) {
        if (sect.isNone()) {
            return;
        }
        if (sect.isHostile()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c" + sect.cn() + "早已叛变，是敌人。拜不进去的。"));
            return;
        }
        if (!canJoin(player, data)) {
            return;
        }
        data.setSect(sect.id());
        data.setContribution(0);
        data.setBetrayed(false);
        ModNetworkBridge.sync(player);

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a76\u00a7l═══ 入 门 ═══"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f你以" + via + "之礼，拜入 " + sect.colored() + "\u00a7f 门下。"));
        player.sendSystemMessage(Component.literal("\u00a77" + sect.desc()));
        player.sendSystemMessage(Component.literal(
                "\u00a7f身份：\u00a7e" + sect.rankName(0)));
        player.sendSystemMessage(Component.literal(
                "\u00a7f门派特性：\u00a7b" + sect.perk()));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a78（杀怪积贡献，贡献够了自动升身份。用 /hongmeng sect status 查看。）"));
        player.sendSystemMessage(Component.literal(""));

        // 入门礼
        net.minecraft.world.item.ItemStack token = switch (sect) {
            case SHENXIAO -> new net.minecraft.world.item.ItemStack(
                    com.hongmeng.xiuxian.item.ModItems.SECT_TOKEN_SHENXIAO.get(), 1);
            default -> new net.minecraft.world.item.ItemStack(
                    com.hongmeng.xiuxian.item.ModItems.KONG_MING_SHI.get(), 2);
        };
        if (!player.getInventory().add(token)) {
            player.drop(token, false);
        }
        player.sendSystemMessage(Component.literal("\u00a7a获得门派信物：" + token.getHoverName().getString()));
        CultivationSource.TASK.grant(player, 120.0D, true);
        QuestChain.onSectJoined(player);
    }

    public static void leave(ServerPlayer player, CultivationData data, boolean betray) {
        Sect old = data.sect();
        if (old.isNone()) {
            player.sendSystemMessage(Component.literal("\u00a77你本来就无门无派。"));
            return;
        }
        data.setSect("");
        if (betray) {
            data.setBetrayed(true);
            data.setContribution(0);
        } else {
            // 正常退门：贡献清零但不背叛徒名号
            data.setContribution(0);
        }
        ModNetworkBridge.sync(player);

        player.sendSystemMessage(Component.literal(""));
        if (betray) {
            player.sendSystemMessage(Component.literal(
                    "\u00a74\u00a7l═══ 叛 门 ═══"));
            player.sendSystemMessage(Component.literal(
                    "\u00a7f你把" + old.colored() + "\u00a7f 的信物掷在地上，转身就走。"));
            player.sendSystemMessage(Component.literal(
                    "\u00a7c身后传来一句：「" + old.shortName() + "的门，不是你想进就进、想出就出的。」"));
            player.sendSystemMessage(Component.literal(
                    "\u00a77从此你背负\u00a7c叛徒\u00a77之名——" + old.colored()
                            + "\u00a77 会派执事来清理门户。"));
        } else {
            player.sendSystemMessage(Component.literal(
                    "\u00a77你向" + old.colored() + "\u00a77 递交了辞帖，从此两不相欠。"));
        }
        player.sendSystemMessage(Component.literal(""));
    }

    // ------------------------------------------------------------------
    // 贡献与身份
    // ------------------------------------------------------------------

    /** 击杀时累计贡献；升身份会播报。 */
    public static void onKill(ServerPlayer player, CultivationData data) {
        Sect sect = data.sect();
        if (sect.isNone()) {
            return;
        }
        int gain = sect.contributionPerKill();
        if (gain <= 0) {
            return;
        }
        int beforeRank = sect.rankIndex(data.getContribution());
        data.addContribution(gain);
        int afterRank = sect.rankIndex(data.getContribution());

        if (afterRank > beforeRank) {
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal(
                    "\u00a7e\u00a7l【身份晋升】" + sect.colored()
                            + "\u00a7e 门下：" + "\u00a7f\u00a7l" + sect.rankName(data.getContribution())));
            player.sendSystemMessage(Component.literal(
                    "\u00a77门派贡献：\u00a7f" + data.getContribution()));
            int next = sect.nextRankThreshold(data.getContribution());
            if (next > 0) {
                player.sendSystemMessage(Component.literal(
                        "\u00a77距下一身份还需贡献：\u00a7f" + (next - data.getContribution())));
            } else {
                player.sendSystemMessage(Component.literal(
                        "\u00a76你已至本门最高身份。"));
            }
            player.sendSystemMessage(Component.literal(""));
            ModNetworkBridge.sync(player);
        }
    }

    // ------------------------------------------------------------------
    // 加成
    // ------------------------------------------------------------------

    /** 身份带来的额外修为倍率：每级身份 +8%。 */
    public static double rankCultivationBonus(CultivationData data) {
        Sect sect = data.sect();
        if (sect.isNone()) {
            return 1.0D;
        }
        return 1.0D + sect.rankIndex(data.getContribution()) * 0.08D;
    }

    /** 综合修为倍率（门派 × 身份）。 */
    public static double cultivationMultiplier(CultivationData data) {
        Sect sect = data.sect();
        if (sect.isNone()) {
            return 1.0D;
        }
        return sect.cultivationMultiplier() * rankCultivationBonus(data);
    }

    /** 攻击加成。 */
    public static double attackBonus(CultivationData data) {
        return data.sect().attackBonus();
    }

    /** 突破成功率加成。 */
    public static double breakthroughBonus(CultivationData data) {
        return data.sect().breakthroughBonus();
    }

    /** 业力上限修正（魔渊/神霄更窄）。 */
    public static int karmaCapModifier(CultivationData data) {
        return data.sect().karmaCapModifier();
    }

    // ------------------------------------------------------------------
    // 清理门户
    // ------------------------------------------------------------------

    /** 叛徒在主世界活动时有概率被门派执事找上门。 */
    public static void tickBetrayal(ServerPlayer player, CultivationData data) {
        if (!data.isBetrayed() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now - data.getLastSectHunt() < HUNT_COOLDOWN) {
            return;
        }
        // 只在主世界、且不在和平模式时追杀
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }
        if (player.getRandom().nextInt(100) >= 4) {
            return;
        }
        data.setLastSectHunt(now);
        spawnHunters(player, level, data);
    }

    private static void spawnHunters(ServerPlayer player, ServerLevel level, CultivationData data) {
        int count = 2 + player.getRandom().nextInt(2);
        String sectName = data.sect().isNone() ? "旧主" : data.sect().shortName();

        for (int i = 0; i < count; i++) {
            GuiltySectDiscipleEntity hunter = com.hongmeng.xiuxian.entity.ModEntities
                    .GUILTY_SECT_DISCIPLE.get().create(level);
            if (hunter == null) {
                continue;
            }
            Vec3 pos = findSpawnPos(player, level, i);
            hunter.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180.0F, 0.0F);
            hunter.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)),
                    MobSpawnType.EVENT, null, null);
            hunter.setHunting(sectName, player.getUUID());
            level.addFreshEntity(hunter);
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a74\u00a7l【清理门户】").withStyle(ChatFormatting.DARK_RED));
        player.sendSystemMessage(Component.literal(
                "\u00a7c" + sectName + "的执事找上门了——" + count + " 人，冲你来的。"));
        player.sendSystemMessage(Component.literal(
                "\u00a78（杀了他们可以洗掉叛徒名号。）"));
        player.sendSystemMessage(Component.literal(""));
    }

    private static Vec3 findSpawnPos(ServerPlayer player, ServerLevel level, int index) {
        double angle = (Math.PI * 2 / 4) * index + player.getRandom().nextDouble();
        for (int radius = 6; radius <= 14; radius += 2) {
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            BlockPos base = BlockPos.containing(x, player.getY(), z);
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos pos = base.offset(0, dy, 0);
                if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
                        && !level.isEmptyBlock(pos.below())) {
                    return new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                }
            }
        }
        return player.position().add(player.getLookAngle().reverse().scale(6.0D));
    }

    /** 击杀执事后洗清叛徒名号。 */
    public static void onHunterKilled(ServerPlayer player, CultivationData data) {
        if (!data.isBetrayed()) {
            return;
        }
        data.setBetrayed(false);
        ModNetworkBridge.sync(player);
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a7a\u00a7l执事倒了。"));
        player.sendSystemMessage(Component.literal(
                "\u00a77你把剑上的血在草上擦掉。「清理门户」这件事，暂时算是了了。"));
        player.sendSystemMessage(Component.literal(
                "\u00a77叛徒名号已洗清。"));
        player.sendSystemMessage(Component.literal(""));
        CultivationSource.SPECIAL.grant(player, 300.0D, true);
    }
}
