package com.hongmeng.xiuxian.talent;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>副业制作</b> —— 把材料变成成品的运行层。
 *
 * <h2>流程</h2>
 * <ol>
 *   <li>检查熟练度门槛（{@link ProfessionRecipe#unlocked}）；</li>
 *   <li>检查材料是否够；</li>
 *   <li>扣材料；</li>
 *   <li>按 {@link ProfessionRecipe#successChance} 判定成败；</li>
 *   <li>成功 → 给成品（并记一次熟练度）；失败 → <b>退回一半材料</b>（不吞）。</li>
 * </ol>
 *
 * <h2>⚠ 为什么失败只退一半、而不是全退或全吞</h2>
 * <ul>
 *   <li><b>全吞</b>：玩家辛苦采的材料一次清空 —— 副业变成劝退环节，
 *       而且与玩家反复强调的"不要代价"直接冲突；</li>
 *   <li><b>全退</b>：那失败没有任何意义，成功率这条数值等于不存在；</li>
 *   <li><b>退一半</b>：有风险但可承受，且"练熟练度降低失败率"变得有价值。</li>
 * </ul>
 *
 * <h2>⚠ 炼器配方的产物是"动态"的</h2>
 * <p>{@code qi_*} 三条配方的 {@code output} 留空 —— 因为产物取决于
 * <b>玩家的道途</b>与<b>当前境界</b>：
 * 法修炼出来是法修的护甲/武器，境界越高越好。
 * 这比"配方里写死一个物品 id"更符合"每种分类要有独立装备"的设计。</p>
 */
public final class ProfessionCrafting {

    private ProfessionCrafting() {
    }

    /**
     * 尝试制作。
     *
     * @param player 玩家
     * @param recipeId 配方 id
     * @return 是否成功产出成品
     */
    public static boolean craft(ServerPlayer player, String recipeId) {
        if (player == null) {
            return false;
        }
        ProfessionRecipe.Recipe recipe = ProfessionRecipe.byId(recipeId);
        if (recipe == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c没有这个配方：\u00a77" + recipeId
                            + "\u00a78（/hongmeng craft 可看可做的）"));
            return false;
        }
        int rank = Talents.professionRank(player, recipe.profession());
        if (!ProfessionRecipe.unlocked(recipe, rank)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c熟练度不足：\u00a77" + recipe.cn() + " 需要 \u00a7e"
                            + recipe.profession().cn() + " " + recipe.minRank()
                            + " \u00a7c级（你现在 " + rank + " 级）"));
            return false;
        }
        if (!hasMaterials(player, recipe)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c材料不足：\u00a77" + materialText(recipe)));
            return false;
        }

        // ---- 扣材料 ----
        consume(player, recipe);

        // ---- 判定成败 ----
        double chance = ProfessionRecipe.successChance(recipe, rank);
        boolean success = player.getRandom().nextDouble() < chance;

        if (!success) {
            // 退一半（向上取整，别让玩家觉得被克扣）
            refund(player, recipe);
            player.sendSystemMessage(Component.literal(
                    "\u00a7c\u00a7l成丹失败 \u00a77· " + recipe.cn()
                            + " \u00a78（成功率 " + Math.round(chance * 100.0D)
                            + "%，退回一半材料）"));
            // 视觉：失败要"看得出来"。只改聊天栏颜色的话，
            // 玩家手快时根本不会注意到，会以为没生效而重复点。
            com.hongmeng.xiuxian.technique.PathMechanicFx.craftFail(player);
            // 失败也算一次练习（但不升级提示，避免刷）
            Talents.recordWork(player, recipe.profession());
            return false;
        }

        // ---- 给成品 ----
        ItemStack out = makeOutput(player, recipe);
        if (out.isEmpty()) {
            // 产物解析不出来：把材料还回去，避免"吃材料不给东西"
            refund(player, recipe);
            player.sendSystemMessage(Component.literal(
                    "\u00a7c产物解析失败，材料已退回（请联系作者）"));
            HongmengMod.LOGGER.warn("[profession] 配方 {} 的产物为空", recipe.id());
            return false;
        }
        if (!player.getInventory().add(out)) {
            // 背包满：掉在脚下，别吞掉玩家的产出
            player.drop(out, false);
        }
        // 成功：记熟练度（可能升级）
        Talents.recordWork(player, recipe.profession());
        player.sendSystemMessage(Component.literal(
                "\u00a7a\u00a7l" + recipe.cn() + " 成了 \u00a77· \u00a7f"
                        + out.getHoverName().getString()
                        + (out.getCount() > 1 ? " ×" + out.getCount() : "")
                        + " \u00a78（成功率 " + Math.round(chance * 100.0D) + "%）"));
        // 视觉：成功给一记向上的光柱（与失败的灰烟形成鲜明对比）
        com.hongmeng.xiuxian.technique.PathMechanicFx.craftSuccess(player,
                recipe.profession());
        return true;
    }

    /** 打印可做的配方。 */
    public static void showRecipes(ServerPlayer player, Profession p) {
        if (player == null || p == null) {
            return;
        }
        int rank = Talents.professionRank(player, p);
        player.sendSystemMessage(Component.literal(
                "\u00a76\u00a7l" + p.cn() + " \u00a77· 熟练度 \u00a7e" + rank
                        + "\u00a77/§f" + Profession.maxRank()
                        + " \u00a78（产出 +" + Math.round(Profession.yieldBonus(rank) * 100)
                        + "%，失败率 −" + Math.round(Profession.failCut(rank) * 100) + "%）"));
        for (ProfessionRecipe.Recipe r : ProfessionRecipe.of(p)) {
            boolean can = ProfessionRecipe.unlocked(r, rank);
            double ch = ProfessionRecipe.successChance(r, rank);
            player.sendSystemMessage(Component.literal(
                    "  " + (can ? "\u00a7a" : "\u00a78") + r.cn()
                            + " \u00a77(" + r.id() + ")"
                            + " \u00a78成功率 " + Math.round(ch * 100.0D) + "%"
                            + " \u00a78· " + materialText(r)
                            + (can ? "" : " \u00a7c需要 " + r.minRank() + " 级")));
        }
        player.sendSystemMessage(Component.literal(
                "\u00a77用法：\u00a7f/hongmeng craft " + p.cn() + " <配方id>"));
    }

    /** 打印全部副业可做的配方。 */
    public static void showAll(ServerPlayer player) {
        if (player == null) {
            return;
        }
        for (Profession p : Profession.values()) {
            showRecipes(player, p);
        }
    }

    // ------------------------------------------------------------------
    // 材料
    // ------------------------------------------------------------------

    private static boolean hasMaterials(ServerPlayer player,
                                        ProfessionRecipe.Recipe recipe) {
        for (ProfessionRecipe.Material m : recipe.materials()) {
            if (countOf(player, m.item()) < m.count()) {
                return false;
            }
        }
        return true;
    }

    /** 数背包里某种物品的总数（含副手，不含装备栏 —— 避免把穿着的护甲当材料）。 */
    private static int countOf(ServerPlayer player, String itemId) {
        Item item = resolve(itemId);
        if (item == null || item == Items.AIR) {
            return 0;
        }
        int n = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && st.getItem() == item) {
                n += st.getCount();
            }
        }
        return n;
    }

    private static void consume(ServerPlayer player, ProfessionRecipe.Recipe recipe) {
        for (ProfessionRecipe.Material m : recipe.materials()) {
            remove(player, m.item(), m.count());
        }
    }

    /** 退回一半（向上取整）。 */
    private static void refund(ServerPlayer player, ProfessionRecipe.Recipe recipe) {
        for (ProfessionRecipe.Material m : recipe.materials()) {
            // ⚠ 原来是 ceil —— count = 1 时 ceil(0.5) = 1，
            //   也就是"失败退回 100%"，净损失 0。
            //   于是带单件材料的配方可以**无限免费重试**，
            //   成功率对它彻底失去意义（等于"早晚会成"）。
            //
            //   改成 floor：count=1 -> 退 0（小料全赔）、2 -> 1、3 -> 1、4 -> 2，
            //   语义仍是"退一半"，但失败真的有代价。
            int back = (int) Math.floor(m.count() * ProfessionRecipe.refundRatio());
            Item item = resolve(m.item());
            if (item == null || item == Items.AIR || back <= 0) {
                continue;
            }
            ItemStack st = new ItemStack(item, back);
            if (!player.getInventory().add(st)) {
                player.drop(st, false);
            }
        }
    }

    private static void remove(ServerPlayer player, String itemId, int count) {
        Item item = resolve(itemId);
        if (item == null || item == Items.AIR) {
            return;
        }
        int left = count;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty() || st.getItem() != item) {
                continue;
            }
            int take = Math.min(left, st.getCount());
            st.shrink(take);
            left -= take;
        }
    }

    private static Item resolve(String id) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) {
                return null;
            }
            return BuiltInRegistries.ITEM.get(rl);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String materialText(ProfessionRecipe.Recipe r) {
        StringBuilder sb = new StringBuilder();
        for (ProfessionRecipe.Material m : r.materials()) {
            if (sb.length() > 0) {
                sb.append("\u00a78 + ");
            }
            String name = m.item().replace("hongmeng:", "").replace("minecraft:", "");
            sb.append("\u00a77").append(name).append("×").append(m.count());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 产物
    // ------------------------------------------------------------------

    /**
     * 造出产物。
     *
     * <p>{@code output} 为空的三条炼器配方是<b>动态产物</b>：
     * 按玩家的道途与境界生成对应的护甲/武器。</p>
     */
    private static ItemStack makeOutput(ServerPlayer player,
                                        ProfessionRecipe.Recipe recipe) {
        // ---- 动态产物（炼器）----
        if (recipe.output() == null || recipe.output().isEmpty()) {
            return makeDynamic(player, recipe);
        }
        Item item = resolve(recipe.output());
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack st = new ItemStack(item, Math.max(1, recipe.outputCount()));
        // 丹药走既有的"按材料等级滚品质"。
        //
        // ⚠ 两个容易踩的点：
        //   1. 丹药类在 `com.hongmeng.xiuxian.item` 包里（**不是** ...item.pill）；
        //   2. ItemQuality 的质量滚动用的是 `java.util.Random`，
        //      而 `player.getRandom()` 是 `RandomSource` —— 类型不通用，
        //      得用新实例（配方本来就带随机性，不需要与服务端随机流同步）。
        try {
            if (st.getItem() instanceof com.hongmeng.xiuxian.item.CultivationPillItem
                    || st.getItem() instanceof com.hongmeng.xiuxian.item.ManaPillItem
                    || st.getItem() instanceof com.hongmeng.xiuxian.item.BreakthroughPillItem) {
                int mainTier = Math.max(1, recipe.minRank() + 1);
                com.hongmeng.xiuxian.quality.ItemQuality.applyAlchemy(st, mainTier,
                        mainTier, new java.util.Random(player.getRandom().nextLong()));
                // 同时标成"制作产物"，这样 tooltip 会显示来源
                com.hongmeng.xiuxian.quality.ItemQuality.applyCrafted(st);
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[profession] 品质滚制失败：{}", t.toString());
        }
        return st;
    }

    /** 炼器的动态产物：按职业对应的旧道途 + 境界生成（新职业武器 R-2 上线）。 */
    private static ItemStack makeDynamic(ServerPlayer player,
                                         ProfessionRecipe.Recipe recipe) {
        com.hongmeng.xiuxian.talent.Career career = Talents.pathOf(player);
        if (career == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c炼器需要先选择职业。"));
            return ItemStack.EMPTY;
        }
        Path path = com.hongmeng.xiuxian.item.LegacyGearBridge
                .toLegacyPath(career);
        if (path == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77你的职业暂时没有对应旧法器（新职业武器 R-2 上线）。"));
            return ItemStack.EMPTY;
        }
        int tier = Math.max(1, Math.min(10, realmTier(player)));
        Item item = null;
        if (recipe.id().equals("qi_weapon")) {
            com.hongmeng.xiuxian.item.gear.PathWeapon kind =
                    com.hongmeng.xiuxian.item.gear.PathWeapon.of(path);
            if (kind != null) {
                item = com.hongmeng.xiuxian.item.gear.ModPathWeapon.get(kind, tier);
            }
        } else if (recipe.id().equals("qi_armor_helm")) {
            // 护甲也按境界分阶了（玩家要求分阶段穿护甲），所以取当前境界那一件
            item = com.hongmeng.xiuxian.item.armor.ModPathArmor.get(path, tier,
                    net.minecraft.world.item.ArmorItem.Type.HELMET);
        } else if (recipe.id().equals("qi_armor_high")) {
            item = com.hongmeng.xiuxian.item.armor.ModPathArmor.get(path, tier,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
        }
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static int realmTier(ServerPlayer player) {
        try {
            var data = com.hongmeng.xiuxian.core.CultivationCapability.get(player);
            return data == null || data.realm() == null ? 1 : data.realm().tier();
        } catch (Throwable t) {
            return 1;
        }
    }

    /** 面板用的一行摘要（服务端算，客户端显示）。 */
    public static String summary(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Profession p : Profession.values()) {
            parts.add(p.cn() + Talents.professionRank(player, p));
        }
        return String.join("/", parts);
    }
}
