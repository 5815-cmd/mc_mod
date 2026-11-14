package com.hongmeng.xiuxian.talent;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>副业配方</b> —— 让炼丹 / 炼器 / 阵法真的能产出东西。
 *
 * <h2>为什么必须复用已有物品</h2>
 * <p>项目里有 9 种丹药（都带品质系统）、23 种材料、以及
 * {@code ItemQuality.applyAlchemy} 这套"按材料等级滚品质"的逻辑。</p>
 * <p>如果副业的产物是新注册的一堆物品，就得再画几十张贴图、再想一遍数值，
 * 而<b>做出来还是空壳</b>（没有能生效的效果）。所以这里刻意<b>只产出已有物品</b>：
 * 丹药走既有的药效，装备走既有的属性系统。这样玩法一上线就是"真能用"的。</p>
 *
 * <h2>三条副业各做什么</h2>
 * <table border="1">
 *   <caption>副业与产物</caption>
 *   <tr><th>副业</th><th>材料</th><th>产物</th></tr>
 *   <tr><td><b>炼丹</b></td><td>灵草花瓣 + 灵草种子</td>
 *       <td>回灵丹 / 修为丹 / 破境丹（带品质）</td></tr>
 *   <tr><td><b>炼器</b></td><td>灵铁矿 / 灵晶矿 / 空冥矿</td>
 *       <td>同境界的道途武器与护甲</td></tr>
 *   <tr><td><b>阵法</b></td><td>灵晶矿 + 灵草</td>
 *       <td>契约符（既有的 beast 契约）+ 阵域强化材料</td></tr>
 * </table>
 *
 * <h2>成功率的用意</h2>
 * <p>熟练度等级越高，成功率越高（{@link Profession#failCut}）。
 * 这一条同时干两件事：</p>
 * <ul>
 *   <li>让"练熟练度"有回报（不只是产量 +6%）；</li>
 *   <li>给副业一个**早期风险**，这样"专精一门"才有意义。</li>
 * </ul>
 * <p><b>但失败绝不吞材料</b> —— 失败只退一半材料（见 {@link #refundRatio()}）。
 * 玩家要求"不要代价"，把材料全吞掉会让副业变成劝退环节。</p>
 */
public final class ProfessionRecipe {

    /** 一条配方。 */
    public record Recipe(Profession profession, String id, String cn,
                         List<Material> materials, String output, int outputCount,
                         double baseSuccess, int minRank) {
    }

    /** 一种材料。 */
    public record Material(String item, int count) {
    }

    private ProfessionRecipe() {
    }

    /** 失败时退回的材料比例（一半）。 */
    public static double refundRatio() {
        return 0.5D;
    }

    /**
     * 全部配方。
     *
     * <p>配方 id 是<b>存档与命令的键</b>，改动会让老存档的"已会配方"失效，
     * 所以加配方只追加、不重命名已有的。</p>
     */
    public static List<Recipe> all() {
        List<Recipe> out = new ArrayList<>();

        // ---------------- 炼丹 ----------------
        // 低阶：回灵丹（2 种花瓣）
        out.add(new Recipe(Profession.LIANDAN, "dan_huiling", "回灵丹",
                List.of(new Material("hongmeng:huoyan_hua_petal", 2),
                        new Material("hongmeng:xuelian_grass_petal", 1)),
                "hongmeng:pill_huiling", 1, 0.75D, 0));
        // 中阶：修为丹（需要 3 种花瓣 + 种子）
        out.add(new Recipe(Profession.LIANDAN, "dan_exp", "修为丹",
                List.of(new Material("hongmeng:hanbing_teng_petal", 2),
                        new Material("hongmeng:leiyin_cao_petal", 2),
                        new Material("hongmeng:kongming_lan_petal", 1)),
                "hongmeng:pill_exp_common", 1, 0.65D, 2));
        // 高阶：破境丹（需要 5 种材料，熟练度门槛高）
        out.add(new Recipe(Profession.LIANDAN, "dan_pojing", "破境丹",
                List.of(new Material("hongmeng:xueluo_zhi_petal", 3),
                        new Material("hongmeng:kongming_lan_petal", 2),
                        new Material("hongmeng:xuelian_grass_petal", 2),
                        new Material("hongmeng:spirit_crystal_ore", 1)),
                "hongmeng:pill_pojing", 1, 0.50D, 4));

        // ---------------- 炼器 ----------------
        // 低阶：灵铁甲片 → 道途护甲（头盔）
        out.add(new Recipe(Profession.LIANQI, "qi_armor_helm", "道途头盔",
                List.of(new Material("hongmeng:spirit_iron_ore", 4),
                        new Material("hongmeng:spirit_crystal_ore", 1)),
                "", 1, 0.70D, 0));
        // 中阶：灵晶 → 道途武器
        out.add(new Recipe(Profession.LIANQI, "qi_weapon", "道途武器",
                List.of(new Material("hongmeng:spirit_crystal_ore", 3),
                        new Material("hongmeng:kongming_ore", 2)),
                "", 1, 0.60D, 3));
        // 高阶：空冥 → 高阶护甲
        out.add(new Recipe(Profession.LIANQI, "qi_armor_high", "高阶护甲",
                List.of(new Material("hongmeng:kongming_ore", 4),
                        new Material("hongmeng:xueluo_ore", 2)),
                "", 1, 0.45D, 6));

        // ---------------- 阵法 ----------------
        // 低阶：契约符（既有的灵兽契约）
        out.add(new Recipe(Profession.ZHENFA, "zhen_talisman", "契约符",
                List.of(new Material("hongmeng:spirit_crystal_ore", 2),
                        new Material("hongmeng:leiyin_cao_petal", 2)),
                "hongmeng:contract_talisman", 1, 0.70D, 0));
        // 中阶：灵兽袋（既有）
        out.add(new Recipe(Profession.ZHENFA, "zhen_pouch", "灵兽袋",
                List.of(new Material("hongmeng:kongming_ore", 2),
                        new Material("hongmeng:spirit_grass", 4)),
                "hongmeng:beast_pouch", 1, 0.60D, 2));

        return out;
    }

    /** 按副业取配方。 */
    public static List<Recipe> of(Profession p) {
        List<Recipe> out = new ArrayList<>();
        for (Recipe r : all()) {
            if (r.profession() == p) {
                out.add(r);
            }
        }
        return out;
    }

    /** 按 id 找配方。 */
    public static Recipe byId(String id) {
        for (Recipe r : all()) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        return null;
    }

    /**
     * 这条配方的实际成功率。
     *
     * <p>基础成功率 + 熟练度减免（{@link Profession#failCut}）。
     * 上限 98% —— 再高就没有"练熟练度"的意义了。</p>
     */
    public static double successChance(Recipe recipe, int rank) {
        double cut = Profession.failCut(rank);
        double fail = (1.0D - recipe.baseSuccess()) * (1.0D - cut);
        return Math.min(0.98D, 1.0D - fail);
    }

    /** 这条配方当前能不能做（熟练度够不够）。 */
    public static boolean unlocked(Recipe recipe, int rank) {
        return rank >= recipe.minRank();
    }
}
