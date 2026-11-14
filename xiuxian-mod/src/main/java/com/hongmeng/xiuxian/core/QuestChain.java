package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.spirit.SpiritRoot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 开局任务链。原文里主角一落地就有"帮助叶凝霜解决危机"这种引导任务，
 * 这里把那条线拆成 7 步，顺带把保底"洗灵根"塞在第四步。
 */
public final class QuestChain {

    public static final String NONE = "none";
    public static final String AWAKENED = "awakened";
    public static final String MEDITATE = "meditate";
    public static final String HUNT = "hunt";
    public static final String SWORD_TRIAL = "sword_trial";
    public static final String SPIRIT_GRASS = "spirit_grass";
    public static final String ALTAR = "altar";
    public static final String KARMA_CLEANSED = "karma_cleansed";
    public static final String EPOCH_OPEN = "epoch_open";
    /** 门派支线：已拜入山门。 */
    public static final String SECT_RECRUITED = "sect_recruited";

    /** 各阶段需要击杀的数量。 */
    private static final Map<UUID, Integer> KILL_COUNT = new HashMap<>();

    /**
     * 玩家下线/换维度时清理进度缓存。
     *
     * <p>⚠ 这两个 Map 原来**只 put 不 remove** —— 每多一个 UUID 就永远留着一条，
     * 服务端长跑会持续涨内存、也越来越慢（性能审计发现的真问题）。</p>
     */
    public static void forget(java.util.UUID id) {
        if (id == null) {
            return;
        }
        KILL_COUNT.remove(id);
        MEDITATE_SECONDS.remove(id);
    }
    /** 各阶段已打坐的秒数。 */
    private static final Map<UUID, Integer> MEDITATE_SECONDS = new HashMap<>();

    private QuestChain() {
    }

    /** 觉醒完成后推进到第一步。 */
    public static void onAwakened(ServerPlayer player, CultivationData data) {
        if (NONE.equals(data.getQuestStage())) {
            data.setQuestStage(AWAKENED);
            send(player, "灵根已定",
                    "去弄张蒲团坐下（合成：4 个羊毛 + 1 个线），坐下就能吐纳涨修为。");
        }
    }

    /** 打坐进度。 */
    public static void onMeditateTick(ServerPlayer player, CultivationData data) {
        String stage = data.getQuestStage();
        if (!AWAKENED.equals(stage) && !MEDITATE.equals(stage)) {
            return;
        }
        if (!MEDITATE.equals(stage)) {
            data.setQuestStage(MEDITATE);
        }
        int seconds = MEDITATE_SECONDS.getOrDefault(player.getUUID(), 0) + 1;
        MEDITATE_SECONDS.put(player.getUUID(), seconds);
        if (seconds >= 30) {
            MEDITATE_SECONDS.put(player.getUUID(), 0);
            data.setQuestStage(HUNT);
            send(player, "吐纳有成",
                    "你已经能把灵气引进经脉了。接下来：打点东西试试手——杀 5 只敌对生物。");
            CultivationSource.TASK.grant(player, 80.0D, true);
        } else if (seconds % 10 == 0) {
            player.displayClientMessage(Component.literal(
                    "\u00a77吐纳中… " + seconds + "/30 秒"), true);
        }
    }

    /** 击杀推进。 */
    public static void onKill(ServerPlayer player, LivingEntity victim) {
        CultivationData data = CultivationCapability.get(player);
        String stage = data.getQuestStage();
        if (!HUNT.equals(stage) && !SWORD_TRIAL.equals(stage) && !KARMA_CLEANSED.equals(stage)) {
            return;
        }

        if (HUNT.equals(stage)) {
            int kills = KILL_COUNT.getOrDefault(player.getUUID(), 0) + 1;
            KILL_COUNT.put(player.getUUID(), kills);
            if (kills >= 5) {
                KILL_COUNT.put(player.getUUID(), 0);
                data.setQuestStage(SWORD_TRIAL);
                send(player, "初次杀伐",
                        "五条命换来的手感。现在去找三十只——杀满 30 只，看看你的灵根到底几斤几两。");
                CultivationSource.TASK.grant(player, 150.0D, true);
            } else {
                player.displayClientMessage(Component.literal(
                        "\u00a77斩敌 " + kills + "/5"), true);
            }
            return;
        }

        if (SWORD_TRIAL.equals(stage)) {
            int kills = KILL_COUNT.getOrDefault(player.getUUID(), 0) + 1;
            KILL_COUNT.put(player.getUUID(), kills);
            if (kills >= 30) {
                KILL_COUNT.put(player.getUUID(), 0);
                data.setQuestStage(SPIRIT_GRASS);
                send(player, "剑试已成",
                        "你身上总算有了点修行的样子。去野外找找发光的灵草——顺手薅一株回来。");
                CultivationSource.TASK.grant(player, 300.0D, true);
                if (data.getRoot() != null
                        && data.getRoot().rarity().rank()
                        <= com.hongmeng.xiuxian.spirit.Rarity.MORTAL.rank()) {
                    player.sendSystemMessage(Component.literal(
                            "\u00a78（灵根品质偏低，越往后越吃力。后面有办法重测。）"));
                }
            } else {
                player.displayClientMessage(Component.literal(
                        "\u00a77剑试 " + kills + "/30"), true);
            }
            return;
        }

        if (KARMA_CLEANSED.equals(stage) && (victim instanceof Pillager
                || victim instanceof Vindicator)) {
            player.displayClientMessage(Component.literal(
                    "\u00a7c你沾上了劫掠者的血——但这一战，是为了村子。"), true);
        }
    }

    /** 破坏灵草推进。 */
    public static void onSpiritGrass(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        if (!SPIRIT_GRASS.equals(data.getQuestStage())) {
            return;
        }
        data.setQuestStage(ALTAR);
        send(player, "灵草入手",
                "灵草含着没散尽的灵气。传闻有人在南方废祠里立了座『测灵台』——"
                        + "站上去，可以重新测一次灵根，保底玄品以上。去找找看。");
        CultivationSource.TASK.grant(player, 400.0D, true);
        ItemStack reward = new ItemStack(ModItems.LINGYE.get(), 3);
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.sendSystemMessage(Component.literal("\u00a7a获得：灵叶 ×3"));
    }

    /** 使用测灵台。 */
    public static void onAltar(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        if (ALTAR.equals(data.getQuestStage())) {
            data.setQuestStage(KARMA_CLEANSED);
            send(player, "重测灵根",
                    "灵根既定，前路自明。最后一步：你身上已经开始积业力了——"
                            + "去替一个村庄解决劫掠袭击，或者啃一颗附魔金苹果，把业力清掉。");
            CultivationSource.TASK.grant(player, 500.0D, true);
            return;
        }
        if (KARMA_CLEANSED.equals(data.getQuestStage()) && data.getKarma() == 0) {
            data.setQuestStage(EPOCH_OPEN);
            send(player, "业力清净",
                    "道基无垢。现在，去搭一座『纪元之门』——用纪元之门框架围成门形，"
                            + "在门内点火，就能进入纪元任务。那里才是真正的主线。");
            CultivationSource.TASK.grant(player, 600.0D, true);
        }
    }

    /** 业力清零时的通用检查。 */
    public static void onKarmaCleared(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        if (KARMA_CLEANSED.equals(data.getQuestStage())) {
            data.setQuestStage(EPOCH_OPEN);
            send(player, "业力清净",
                    "道基无垢。去搭一座『纪元之门』（用纪元之门框架围成门形，门内点火），"
                            + "进入纪元任务。");
            CultivationSource.TASK.grant(player, 600.0D, true);
        }
    }

    /** 拜入门派。 */
    public static void onSectJoined(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        if (KARMA_CLEANSED.equals(data.getQuestStage())
                || ALTAR.equals(data.getQuestStage())
                || data.getQuestStage().equals(SECT_RECRUITED)) {
            // 还没走完主线就不抢戏，只提示一句
            player.sendSystemMessage(Component.literal(
                    "\u00a78（门派身份可以在修炼面板里查看；贡献够了会自动升身份。）"));
            return;
        }
        if (!data.getQuestStage().equals(EPOCH_OPEN) && data.isAwakened()) {
            data.setQuestStage(SECT_RECRUITED);
            send(player, "山门已入",
                    "往后你有靠山了，也有管你的人了。用 /sect status 看身份与贡献，"
                            + "杀怪会自动积累门派贡献。");
        }
    }

    /** 当前任务提示文本（修炼面板里显示用）。 */
    public static String questText(CultivationData data) {
        if (!data.isAwakened()) {
            return "等待灵根觉醒……";
        }
        return switch (data.getQuestStage()) {
            case AWAKENED, MEDITATE -> "吐纳：坐上蒲团打坐 30 秒";
            case HUNT -> "试手：击杀 5 只敌对生物";
            case SWORD_TRIAL -> "剑试：累计击杀 30 只";
            case SPIRIT_GRASS -> "采集：找到并破坏一株灵草";
            case ALTAR -> "重测：找到测灵台并使用";
            case KARMA_CLEANSED -> "清业：解决一次劫掠袭击，或吃附魔金苹果";
            case EPOCH_OPEN -> "开门：搭建纪元之门框架并点燃";
            case SECT_RECRUITED -> "门派修行：杀怪积累贡献，提升身份（/sect status）";
            default -> "自由修行。灵根既定，路自己走。";
        };
    }

    private static void send(ServerPlayer player, String title, String body) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00a7e\u00a7l【任务】" + title));
        player.sendSystemMessage(Component.literal("\u00a7f" + body));
        player.sendSystemMessage(Component.literal(""));
    }

    /** 找一片能放蒲团的地面（给命令用）。 */
    public static BlockPos findFlatGround(ServerLevel level, BlockPos near) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = near.offset(0, dy, 0);
            if (level.getBlockState(pos.below()).isSolid()
                    && level.getBlockState(pos).isAir()
                    && !level.getBlockState(pos.below()).is(Blocks.WATER)) {
                return pos;
            }
        }
        return near;
    }

    public static boolean isQuestHolder(Player player) {
        return player != null;
    }
}
