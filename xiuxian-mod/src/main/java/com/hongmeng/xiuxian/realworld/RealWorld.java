package com.hongmeng.xiuxian.realworld;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>「真实世界」</b>—— 玩家要求的新系统。
 *
 * <h2>玩家原话</h2>
 * <blockquote>
 * 「再设置一个第一次打完虚空裂隙后，开启真实世界选项，如果没有选择开启，
 * 下次打完虚空裂隙也可以选择，选择开启后这可以做个过场动画，所有维度发生变化，
 * 怪物强度升级对标帝境，头目首领小队队长都提升，比如头目就是帝境后期，
 * 首领就是鸿蒙，头目是帝境中期，普通小怪是仙人境界随机，精英小怪是帝境初期」
 * </blockquote>
 *
 * <h2>三个阶段</h2>
 * <ol>
 *   <li><b>未解锁</b>（{@link #LOCKED}）：还没通关过虚空裂隙，玩家根本不知道有这回事。</li>
 *   <li><b>已解锁、未开启</b>（{@link #UNLOCKED}）：通关过虚空裂隙 → 解锁。
 *       从此<b>每次</b>通关都会再问一次（玩家明确说了「如果没有选择开启，
 *       下次打完虚空裂隙也可以选择」）。</li>
 *   <li><b>已开启</b>（{@link #ACTIVE}）：世界已改写 ——
 *       维度外观变化、怪物按帝境段位重排。</li>
 * </ol>
 *
 * <h2>为什么存档标记要存在"全局"而不是玩家身上</h2>
 * <p>「所有维度发生变化」是<b>世界级</b>的效果，不是某个玩家的状态。
 * 所以标记存在世界的 {@code SavedData} 里（跟随存档），
 * 而"是否问过这个玩家"这种小事才存在玩家身上。</p>
 *
 * <h2>开启后的怪物段位（玩家指定）</h2>
 * <table border="1">
 *   <caption>真实世界怪物境界</caption>
 *   <tr><th>品阶</th><th>境界</th></tr>
 *   <tr><td>普通</td><td><b>仙人</b>（随机波动）</td></tr>
 *   <tr><td>精英</td><td><b>帝境初期</b></td></tr>
 *   <tr><td>队长</td><td><b>帝境中期</b></td></tr>
 *   <tr><td>头目</td><td><b>帝境后期</b></td></tr>
 *   <tr><td>首领</td><td><b>鸿蒙</b></td></tr>
 * </table>
 */
public final class RealWorld {

    private RealWorld() {
    }

    /** 状态：未解锁 / 已解锁未开启 / 已开启。 */
    public enum Stage {
        /** 还没通关过虚空裂隙。 */
        LOCKED,
        /** 通关过，可以选择开启 —— 但还没开。 */
        UNLOCKED,
        /** 已开启：维度变了、怪也变了。 */
        ACTIVE
    }

    /** 境界表白名单（与 {@code MobRank.REALM_CN} 同序，1~10）。 */
    public static final int TIER_XIANREN = 8;      // 仙人
    public static final int TIER_DIJING = 9;       // 帝境
    public static final int TIER_HONGMENG = 10;    // 鸿蒙

    private static final Map<UUID, Long> PENDING_PROMPT = new HashMap<>();

    // ------------------------------------------------------------------
    // 状态读写（世界级 SavedData）
    // ------------------------------------------------------------------

    /** 世界级数据。 */
    public static class Data extends net.minecraft.world.level.saveddata.SavedData {
        /** 是否已经解锁（通关过一次虚空裂隙）。 */
        public boolean unlocked;
        /** 是否已开启真实世界。 */
        public boolean active;

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("unlocked", unlocked);
            tag.putBoolean("active", active);
            return tag;
        }

        static Data load(CompoundTag tag) {
            Data d = new Data();
            d.unlocked = tag.getBoolean("unlocked");
            d.active = tag.getBoolean("active");
            return d;
        }
    }

    private static final String KEY = "hongmeng_real_world";

    /**
     * 真世界维度的路径名（用于 {@link #active(net.minecraft.world.level.Level)} 判维度）。
     *
     * <p>必须和维度注册时用的名字一致；写错的表现是"真世界里的怪反而没按帝境排"。</p>
     */
    public static final String DIMENSION_PATH = "real_world";

    public static Data data(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(Data::load, Data::new, KEY);
    }

    public static Stage stage(MinecraftServer server) {
        Data d = data(server);
        if (d.active) {
            return Stage.ACTIVE;
        }
        return d.unlocked ? Stage.UNLOCKED : Stage.LOCKED;
    }

    public static boolean active(MinecraftServer server) {
        return server != null && data(server).active;
    }

    /** 世界级：是否已开启（给怪物缩放等处用，不需要服务器实例）。 */
    public static boolean active(ServerLevel level) {
        return level != null && active(level.getServer());
    }

    /**
     * 世界级：是否已开启（接受任意 {@link net.minecraft.world.level.Level}）。
     *
     * <p>怪物实体拿到的 `level()` 静态类型是 {@code Level} 而不是 {@code ServerLevel}，
     * 所以需要一个宽一点的签名 —— 否则调用处得先强转，多一次可能出错的地方。</p>
     */
    public static boolean active(net.minecraft.world.level.Level level) {
        if (level == null || level.isClientSide()) {
            return false;
        }
        // ⚠ 这是**世界级**开关，**故意**不按维度区分 ——
        //   玩家原话：「选择开启后……**所有维度发生变化**，怪物强度升级对标帝境」。
        //   所以真世界开启后，主世界与四个副本的怪都按帝境段位重排，这是设计。
        return level instanceof ServerLevel sl && active(sl);
    }

    /**
     * 这个维度是不是**真世界**专用的附加维度（本模组没有，恒为 false）。
     *
     * <p>保留这个方法是为了说明一件事：<b>真实世界不是维度</b>，
     * 它是世界级的改写（{@link #active(MinecraftServer)}）。</p>
     */
    public static boolean isRealWorldDimension(net.minecraft.world.level.Level level) {
        return false;
    }

    // ------------------------------------------------------------------
    // 解锁 / 开启
    // ------------------------------------------------------------------

    /**
     * 通关虚空裂隙时调用：**解锁**并**询问玩家**是否开启。
     *
     * <p>玩家要求「第一次打完虚空裂隙后，开启真实世界选项，
     * 如果没有选择开启，下次打完虚空裂隙也可以选择」——
     * 所以这里每次都问（只要还没开启），而不是只问一次。</p>
     */
    public static void onVoidCleared(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        Data d = data(server);
        if (!d.unlocked) {
            d.unlocked = true;
            d.setDirty();
            HongmengMod.LOGGER.info("[realworld] 已解锁（玩家 {} 首次通关虚空裂隙）",
                    player.getName().getString());
        }
        if (d.active) {
            return;                     // 已经开启了就不再问
        }
        prompt(player);
    }

    /** 给玩家发选项提示（用屏幕标题 + 聊天里的可点击命令）。 */
    private static void prompt(ServerPlayer player) {
        PENDING_PROMPT.put(player.getUUID(), player.level().getGameTime());
        player.sendSystemMessage(Component.literal(
                "\u00a75\u00a7l—— 你窥见了世界的「背面」 ——\u00a7r"));
        player.sendSystemMessage(Component.literal(
                "\u00a77你可以选择\u00a7c\u00a7l开启「真实世界」\u00a77："
                        + "所有维度将显露真貌，万物强度重排至\u00a7c帝境\u00a77以上。"));
        player.sendSystemMessage(Component.literal(
                "\u00a77· 开启：\u00a7a\u00a7l点击这里\u00a77（或输入 \u00a7f/hongmeng realworld on\u00a77）")
                .copy().withStyle(s -> s
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                "/hongmeng realworld on"))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                Component.literal("开启真实世界（不可逆）")))));
        player.sendSystemMessage(Component.literal(
                "\u00a77· 暂不开启：\u00a78输入 /hongmeng realworld off\u00a77"
                        + " —— 下次通关虚空裂隙还会再问。"));
    }

    /** 玩家选择了「暂不开启」。 */
    public static void decline(ServerPlayer player) {
        PENDING_PROMPT.remove(player.getUUID());
        player.sendSystemMessage(Component.literal(
                "\u00a78你收回了视线。世界仍是原来的样子 —— "
                        + "下次通关虚空裂隙时，它还会再问你一次。"));
    }

    /**
     * <b>开启真实世界</b>：改维度、改怪物段位，并播放过场。
     *
     * @return 是否真的开启（已经开着就返回 false）
     */
    public static boolean activate(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        Data d = data(server);
        if (d.active) {
            player.sendSystemMessage(Component.literal("\u00a78真实世界早已开启。"));
            return false;
        }
        d.unlocked = true;
        d.active = true;
        d.setDirty();
        PENDING_PROMPT.remove(player.getUUID());

        HongmengMod.LOGGER.info("[realworld] 已开启（由玩家 {} 触发）", player.getName().getString());

        // 过场动画（复用 BOSS 出场那套镜头系统）
        // 玩家限定：运镜只用于「boss 出场 / boss 死亡 / 法术释放」。
            // 进真世界属于场景切换，不再占用镜头（改成提示即可）。
            player.displayClientMessage(net.minecraft.network.chat.Component
                    .literal("\u00a78\u4f60\u8e0f\u5165\u4e86\u771f\u5b9e\u4e16\u754c\u3002"), true);
        // 把所有在线客户端的状态同步过去（雾色/氛围立刻开始渐变）
        try {
            com.hongmeng.xiuxian.network.ModNetwork.sendToAll(
                    new com.hongmeng.xiuxian.network.RealWorldStatePacket(true));
        } catch (Throwable ignored) {
            // 同步失败不影响世界切换
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 怪物段位表（玩家指定的「帝境段位」）
    // ------------------------------------------------------------------

    /**
     * 真实世界下，某个品阶的怪应该是什么境界。
     *
     * <p>玩家原话：「头目就是帝境后期，首领就是鸿蒙，头目是帝境中期，
     * 普通小怪是仙人境界随机，精英小怪是帝境初期」。</p>
     *
     * <p>（玩家在「头目」上说了两次，按后一句"头目是帝境中期"与品阶递增关系，
     * 实际取：队长 = 帝境初期 / 头目 = 帝境中期 / 首领 = 鸿蒙。
     * 见 {@code tools/check_real_world.py} 里逐条对账。）</p>
     *
     * @param rankOrdinal 品阶序号（0 普通 / 1 精英 / 2 队长 / 3 头目 / 4 首领）
     * @param roll         0~1 的随机数（普通怪「随机」用）
     */
    /**
     * <b>真世界开启后，各副本的怪境界阶梯</b>（玩家指定）。
     *
     * <p>玩家原话：「仙人境界对标虚空副本，飞升是魔，道印是妖兽，以此往下」——
     * 即<b>越深的副本，怪境界越高一档</b>：</p>
     * <ul>
     *   <li>虚空裂隙 → 仙人（8）</li>
     *   <li>魔道巢穴 → 飞升（7）</li>
     *   <li>妖兽秘境 → 道印（6）</li>
     *   <li>纪元秘境 → 化神（5）</li>
     * </ul>
     *
     * <p>首领在该维度基础上 +1 境（封顶鸿蒙）—— "最终那只"要比杂兵高一档。</p>
     *
     * @return 0 表示"不是副本维度"（调用方应按品阶规则处理）
     */
    public static int tierForDungeon(String dimensionPath, int rankOrdinal) {
        if (dimensionPath == null) {
            return 0;
        }
        int base = switch (dimensionPath) {
            case "void_realm" -> 8;    // 仙人
            case "demon_realm" -> 7;   // 飞升
            case "beast_realm" -> 6;   // 道印
            case "epoch_realm" -> 5;   // 化神
            default -> 0;
        };
        if (base <= 0) {
            return 0;
        }
        // 首领（BOSS/OVERLORD 序号 >= BOSS）再高一境
        boolean boss = rankOrdinal >= 3;
        return Math.max(1, Math.min(10, base + (boss ? 1 : 0)));
    }

    public static int tierFor(int rankOrdinal, float roll) {
        return switch (rankOrdinal) {
            // 普通：仙人境界随机 —— 在 仙人~帝境初期 之间随机
            case 0 -> roll < 0.72F ? TIER_XIANREN : TIER_DIJING;
            case 1 -> TIER_DIJING;                    // 精英：帝境初期
            case 2 -> TIER_DIJING;                    // 队长：帝境中期
            case 3 -> TIER_DIJING;                    // 头目：帝境后期
            default -> TIER_HONGMENG;                 // 首领：鸿蒙
        };
    }

    /**
     * 段位名（用于日志与提示；{@code MobRank.REALM_CN} 只有大境，
     * 这里补上「初/中/后期」的说法）。
     */
    public static String tierName(int rankOrdinal) {
        return switch (rankOrdinal) {
            case 0 -> "仙人";
            case 1 -> "帝境初期";
            case 2 -> "帝境中期";
            case 3 -> "帝境后期";
            default -> "鸿蒙";
        };
    }
}
