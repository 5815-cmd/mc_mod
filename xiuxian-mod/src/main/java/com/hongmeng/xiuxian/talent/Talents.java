package com.hongmeng.xiuxian.talent;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>职业、天赋与副业的运行层</b>。
 *
 * <h2>三层结构</h2>
 * <ul>
 *   <li><b>职业</b>（{@link Career}）：只能选一条，吃<b>天赋点</b>；</li>
 *   <li><b>副业</b>（{@link Profession}）：可以兼修多门，吃<b>独立熟练度</b>；</li>
 *   <li><b>门派</b>（{@code Sect}）：独立系统，本轮不动。</li>
 * </ul>
 *
 * <h2>点数按"已花费"反推</h2>
 * <p>只存<b>每个天赋点到几级</b>；剩余 = 应得 − 已花，永远自洽，
 * 洗点/转职不会出现负数或白送。</p>
 */
public final class Talents {

    /** 天赋等级表：玩家 -> (节点 id -> 等级)。职业存在同表的 {@link #CAREER_KEY} 键。 */
    private static final Map<UUID, Map<String, Integer>> DATA = new HashMap<>();

    /** 副业熟练度：玩家 -> (副业 -> 干活次数)。 */
    private static final Map<UUID, Map<String, Integer>> WORK = new HashMap<>();

    private static final String CAREER_KEY = "__career";

    private Talents() {
    }

    private static Map<String, Integer> map(ServerPlayer player) {
        return DATA.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
    }

    // ------------------------------------------------------------------
    // 职业
    // ------------------------------------------------------------------

    public static Career pathOf(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return null;
        }
        Integer ord = DATA.getOrDefault(player.getUUID(), Map.of()).get(CAREER_KEY);
        if (ord == null) {
            return null;
        }
        Career[] all = Career.values();
        return ord >= 0 && ord < all.length ? all[ord] : null;
    }

    /**
     * 选定职业（免费，送入门点数）。
     *
     * <p>已选过则拒绝 —— 但<b>不是永久</b>：服下转道丹会清掉职业，
     * 之后可以重新选。</p>
     */
    public static boolean choose(ServerPlayer player, Career career) {
        if (player == null || career == null) {
            return false;
        }
        Map<String, Integer> m = map(player);
        Career current = pathOf(player);
        if (m.containsKey(CAREER_KEY) && current != null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77你已入 \u00a7f" + current.cn()
                            + "\u00a77。想转职需服下 \u00a7d转道丹"
                            + "\u00a77\u00a78（旧职业天赋会尽数退回）"));
            return false;
        }
        m.put(CAREER_KEY, career.ordinal());
        player.sendSystemMessage(Component.literal(
                "\u00a7a\u00a7l你踏上了「" + career.cn() + "」之路。"));
        player.sendSystemMessage(Component.literal("\u00a77" + career.desc()));
        player.sendSystemMessage(Component.literal(
                "\u00a7d专属加成 \u00a77· \u00a7f" + career.bonus()));
        player.sendSystemMessage(Component.literal(
                "\u00a77获赠 \u00a7e" + TalentTree.POINTS_ON_CHOOSE
                        + " \u00a77点天赋点。输入 \u00a7f/hongmeng talent"
                        + "\u00a77 查看天赋树。"));
        return true;
    }

    /**
     * 应该得到的总点数 = 入门赠送 + 境界推进 × 每境点数。
     */
    public static int totalPoints(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return 0;
        }
        Map<String, Integer> m = DATA.get(player.getUUID());
        if (m == null || !m.containsKey(CAREER_KEY)) {
            return 0;
        }
        int tier = realmTier(player);
        return TalentTree.POINTS_ON_CHOOSE
                + Math.max(0, tier - 1) * TalentTree.POINTS_PER_REALM;
    }

    public static int spentPoints(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return 0;
        }
        Map<String, Integer> m = DATA.get(player.getUUID());
        if (m == null) {
            return 0;
        }
        int sum = 0;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (!CAREER_KEY.equals(e.getKey())) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    /** 剩余点数（反推）。 */
    public static int freePoints(net.minecraft.world.entity.player.Player player) {
        return Math.max(0, totalPoints(player) - spentPoints(player));
    }

    public static int rankOf(ServerPlayer player, String id) {
        Map<String, Integer> m = DATA.get(player.getUUID());
        return m == null ? 0 : m.getOrDefault(id, 0);
    }

    /** 某分支已投入的点数（第 2 节点与技能解锁的门槛）。 */
    public static int branchInvested(ServerPlayer player, CareerBranch branch) {
        if (player == null || branch == null) {
            return 0;
        }
        int sum = 0;
        for (CareerTalentTree.Node n : TalentTree.ofBranch(branch)) {
            sum += rankOf(player, n.id());
        }
        return sum;
    }

    /** 该分支是否已解锁主动（5 点）。 */
    public static boolean activeUnlocked(ServerPlayer player, CareerActive active) {
        return active != null && branchInvested(player, active.branch()) >= 5;
    }

    /** 某条职业被动是否已解锁（核心自带 or 分支门槛）。 */
    public static boolean skillUnlocked(ServerPlayer player, CareerSkill skill) {
        if (player == null || skill == null || pathOf(player) != skill.career()) {
            return false;
        }
        return skill.isCore()
                || branchInvested(player, skill.branch()) >= (int) skill.unlockPoints();
    }

    /** 加一点天赋。 */
    public static boolean rankUp(ServerPlayer player, String id) {
        if (player == null) {
            return false;
        }
        Career career = pathOf(player);
        if (career == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c先选职业：\u00a7f/hongmeng path <剑修|血修|魔修|鬼修|体修>"));
            return false;
        }
        List<CareerTalentTree.Node> tree = TalentTree.forCareer(career);
        CareerTalentTree.Node node = TalentTree.find(tree, id);
        if (node == null) {
            player.sendSystemMessage(Component.literal("\u00a7c点不了：\u00a77没有这个天赋"));
            return false;
        }
        String why = TalentTree.canRankUp(tree, id, freePoints(player),
                branchInvested(player, node.branch()), n -> rankOf(player, n));
        if (why != null) {
            player.sendSystemMessage(Component.literal("\u00a7c点不了：\u00a77" + why));
            return false;
        }
        map(player).merge(id, 1, Integer::sum);
        player.sendSystemMessage(Component.literal(
                "\u00a7a" + node.cn() + " \u00a77提升到 \u00a7e" + rankOf(player, id)
                        + "\u00a77 级。剩余天赋点 \u00a7e" + freePoints(player)));
        return true;
    }

    /** 某类效果的合计加成（只统计当前职业的节点）。 */
    public static double bonus(ServerPlayer player, CareerTalentTree.Kind kind) {
        Career career = pathOf(player);
        if (career == null) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (CareerTalentTree.Node n : TalentTree.forCareer(career)) {
            if (n.kind() == kind) {
                sum += n.perRank() * rankOf(player, n.id());
            }
        }
        return sum;
    }

    /**
     * <b>职业专属加成</b>（"多给你一样"那部分，选了就永久拥有）。
     */
    public static double careerBonus(ServerPlayer player, Career.BonusKind kind) {
        Career career = pathOf(player);
        if (career == null || career.bonusKind() != kind) {
            return 0.0D;
        }
        return career.bonusValue();
    }

    /**
     * 突破大境时结算（幂等：点数按境界反推，不额外发）。
     */
    public static void onBreakthrough(ServerPlayer player) {
        if (player == null || pathOf(player) == null) {
            return;
        }
        int free = freePoints(player);
        if (free > 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7d\u00a7l境界精进 \u00a77· 你获得 \u00a7e" + free
                            + " \u00a77点天赋点（共 \u00a7f" + totalPoints(player)
                            + "\u00a77 点）"));
        }
    }

    // ------------------------------------------------------------------
    // 副业（可兼修）
    // ------------------------------------------------------------------

    public static int workOf(net.minecraft.world.entity.player.Player player, Profession p) {
        if (player == null || p == null) {
            return 0;
        }
        Map<String, Integer> w = WORK.get(player.getUUID());
        return w == null ? 0 : w.getOrDefault(p.name(), 0);
    }

    public static int professionRank(net.minecraft.world.entity.player.Player player,
                                     Profession p) {
        int work = workOf(player, p);
        int rank = 0;
        while (rank < Profession.maxRank()
                && work >= Profession.workNeeded(rank + 1)) {
            rank++;
        }
        return rank;
    }

    /** 记一次干活（炼丹成一炉、炼器成一器、布阵成一阵）。 */
    public static boolean recordWork(ServerPlayer player, Profession p) {
        if (player == null || p == null) {
            return false;
        }
        int before = professionRank(player, p);
        int step = Math.max(1, (int) Math.round(professionSpeed(player, p)));
        WORK.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .merge(p.name(), step, Integer::sum);
        int after = professionRank(player, p);
        if (after > before) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7a\u00a7l" + p.cn() + " \u00a77精进至 \u00a7e" + after
                            + " \u00a77级 \u00a78（产出 +"
                            + Math.round(Profession.yieldBonus(after) * 100.0D)
                            + "%，失败率 −"
                            + Math.round(Profession.failCut(after) * 100.0D) + "%）"));
        }
        return after > before;
    }

    /** 副业对某类产出的加成。 */
    public static double professionYield(ServerPlayer player, Profession p) {
        return Profession.yieldBonus(professionRank(player, p));
    }

    /** 鬼修练阵法副业会更快（鬼域继承阵域，保留"主修与副业互相加成"）。 */
    public static double professionSpeed(ServerPlayer player, Profession p) {
        Career career = pathOf(player);
        if (career == Career.GUI_XIU && p == Profession.ZHENFA) {
            return 2.0D;
        }
        return 1.0D;
    }

    // ------------------------------------------------------------------
    // 展示
    // ------------------------------------------------------------------

    public static void show(ServerPlayer player) {
        Career career = pathOf(player);
        if (career == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77你尚未择业。可选职业：\u00a7f" + Career.allNames()));
            player.sendSystemMessage(Component.literal(
                    "\u00a77用法：\u00a7f/hongmeng path 剑修"));
            return;
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b\u00a7l" + career.cn() + " \u00a77· 天赋点 \u00a7e"
                        + freePoints(player) + "\u00a77/\u00a7f" + totalPoints(player)
                        + "\u00a77（已投 " + spentPoints(player) + "）"));
        player.sendSystemMessage(Component.literal(
                "\u00a7d专属加成 \u00a77· \u00a7f" + career.bonus()));
        for (CareerTalentTree.Node n : TalentTree.forCareer(career)) {
            int r = rankOf(player, n.id());
            String bar = r > 0 ? "\u00a7a" + r + "/" + n.maxRank() : "\u00a78未点";
            player.sendSystemMessage(Component.literal(
                    "  \u00a77" + n.branch().cn() + "·" + n.order() + " \u00a7f"
                            + n.cn() + " \u00a77(" + n.id() + ") " + bar
                            + " \u00a78— " + n.desc()));
        }
        // 分支解锁进度（技能）
        for (CareerBranch b : CareerBranch.ofCareer(career)) {
            int inv = branchInvested(player, b);
            CareerActive active = CareerActive.ofBranch(b);
            player.sendSystemMessage(Component.literal(
                    "  \u00a78" + b.cn() + " 投入 " + inv + " 点"
                            + (active == null ? "" : (inv >= 5
                            ? " \u00a7a【主动已解锁：" + active.cn() + "】"
                            : " \u00a78（主动还需 " + (5 - inv) + " 点）"))));
        }
    }

    /** 副业一览。 */
    public static void showProfessions(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                "\u00a76\u00a7l副业 \u00a77（可兼修，用哪门哪门涨）"));
        for (Profession p : Profession.values()) {
            int r = professionRank(player, p);
            int work = workOf(player, p);
            String next = r >= Profession.maxRank() ? "\u00a7a已满"
                    : "\u00a78下一级还需 " + Math.max(0,
                            Profession.workNeeded(r + 1) - work) + " 次";
            player.sendSystemMessage(Component.literal(
                    "  " + p.colored().getString() + " \u00a77等级 \u00a7e" + r
                            + "\u00a77/" + Profession.maxRank()
                            + " \u00a78（已做 " + work + " 次） " + next
                            + " \u00a78— " + p.desc()));
        }
    }

    /**
     * <b>职业摘要</b>：一行文字，给客户端面板显示（7 段）。
     */
    public static String clientSummary(net.minecraft.world.entity.player.Player player) {
        Career career = pathOf(player);
        StringBuilder sb = new StringBuilder();
        if (career == null) {
            sb.append("")                       // 0: 职业名（空 = 尚未择业）
                    .append("|").append(0)      // 1: 剩余点
                    .append("|").append(0)      // 2: 总点
                    .append("|").append("")     // 3: 职业专属加成
                    .append("|").append("\u00a78未择业")
                    .append("|").append("\u00a78无")
                    .append("|");
        } else {
            sb.append(career.cn())
                    .append("|").append(freePoints(player))
                    .append("|").append(totalPoints(player))
                    .append("|").append(career.bonus())
                    .append("|").append("\u00a78职业甲 R-2 上线")
                    .append("|").append("\u00a78通用武器 R-2 上线")
                    .append("|");
        }
        StringBuilder prof = new StringBuilder();
        for (Profession p : Profession.values()) {
            if (prof.length() > 0) {
                prof.append("/");
            }
            prof.append(p.cn()).append(professionRank(player, p));
        }
        sb.append(prof);
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 低层入口（存档层用：只有 UUID，没有 ServerPlayer；不校验）
    // ------------------------------------------------------------------

    /** 按 UUID 取职业（存档层用）。 */
    public static Career careerAt(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Integer ord = DATA.getOrDefault(uuid, Map.of()).get(CAREER_KEY);
        if (ord == null) {
            return null;
        }
        Career[] all = Career.values();
        return ord >= 0 && ord < all.length ? all[ord] : null;
    }

    /** 按 UUID 直接设置职业（存档回读用，不校验）。 */
    public static void setCareerAt(UUID uuid, Career career) {
        if (uuid == null) {
            return;
        }
        Map<String, Integer> m = DATA.computeIfAbsent(uuid, k -> new HashMap<>());
        if (career == null) {
            m.remove(CAREER_KEY);
        } else {
            m.put(CAREER_KEY, career.ordinal());
        }
    }

    /** 按 UUID 取天赋等级表（不含职业那个键）。 */
    public static Map<String, Integer> ranksAt(UUID uuid) {
        Map<String, Integer> out = new HashMap<>();
        Map<String, Integer> m = uuid == null ? null : DATA.get(uuid);
        if (m == null) {
            return out;
        }
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (!CAREER_KEY.equals(e.getKey())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    /** 按 UUID 直接设置某个天赋等级（存档回读用，不校验）。 */
    public static void setRankAt(UUID uuid, String id, int rank) {
        if (uuid == null || id == null || id.isEmpty() || CAREER_KEY.equals(id)) {
            return;
        }
        Map<String, Integer> m = DATA.computeIfAbsent(uuid, k -> new HashMap<>());
        if (rank <= 0) {
            m.remove(id);
        } else {
            m.put(id, rank);
        }
    }

    /** 按 UUID 取副业干活次数（存档层用）。 */
    public static Map<String, Integer> workAt(UUID uuid) {
        Map<String, Integer> w = uuid == null ? null : WORK.get(uuid);
        return w == null ? new HashMap<>() : new HashMap<>(w);
    }

    /** 按 UUID 直接设置副业干活次数（存档回读用，不校验）。 */
    public static void setWorkAt(UUID uuid, String professionName, int count) {
        if (uuid == null || professionName == null || professionName.isEmpty() || count <= 0) {
            return;
        }
        WORK.computeIfAbsent(uuid, k -> new HashMap<>()).put(professionName, count);
    }

    /** 洗点：把所有天赋节点等级清零（点数自动全额退回）。 */
    public static int clearRanks(ServerPlayer player) {
        return player == null ? 0 : clearRanks(player.getUUID());
    }

    /** 同 {@link #clearRanks(ServerPlayer)}，但按 UUID。 */
    public static int clearRanks(UUID uuid) {
        if (uuid == null) {
            return 0;
        }
        Map<String, Integer> m = DATA.get(uuid);
        if (m == null) {
            return 0;
        }
        int cleared = 0;
        java.util.Iterator<Map.Entry<String, Integer>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (CAREER_KEY.equals(e.getKey())) {
                continue;
            }
            cleared += e.getValue();
            it.remove();
        }
        return cleared;
    }

    public static void forget(UUID uuid) {
        DATA.remove(uuid);
        WORK.remove(uuid);
    }

    private static int realmTier(net.minecraft.world.entity.player.Player player) {
        CultivationData data = CultivationCapability.get(player);
        return data == null || data.realm() == null ? 1 : data.realm().tier();
    }
}
