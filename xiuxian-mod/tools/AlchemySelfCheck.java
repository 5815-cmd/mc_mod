/*
 * 炼丹配方表的离线自检入口（default package，放在 tools/ 下，不进 mod jar）。
 *
 * 为什么要有这么一个文件：
 *   LogicVerifier（src/verify/**）没有反射、没有扫描，只调用写死的断言，
 *   想让丹方表进验证器就必须改那个文件——而它是别人的地盘。
 *   所以这里用 java 的单文件源码启动方式把这个表**真的跑一遍**：
 *
 *     java -cp _cache/modout tools/AlchemySelfCheck.java
 *
 *   AlchemyRecipes.validate() 是纯字符串运算，不碰任何注册表
 *   （碰了就会 "Not bootstrapped"），所以离线跑得起来。
 *   输出用 "|" 分隔的机器可读格式，交给 tools/check_herb_alchemy.py 继续
 *   和 ModItems.java / data/**.json 对撞。
 *
 * 退出码：0 = 表合法；1 = 表坏了（异常信息会打到 stderr）。
 */

import com.hongmeng.xiuxian.alchemy.AlchemyRecipes;
import com.hongmeng.xiuxian.alchemy.HerbCatalog;

public final class AlchemySelfCheck {

    public static void main(String[] args) {
        // 表坏了这里会抛 IllegalStateException，进程非 0 退出 —— 这就是"响亮地失败"
        AlchemyRecipes.validate();
        HerbCatalog.validate();

        System.out.println("ALCHEMY-TABLE-OK|brews=" + AlchemyRecipes.BREWS.size()
                + "|covered=" + AlchemyRecipes.coveredPills().size()
                + "|pills=" + AlchemyRecipes.PILL_IDS.size()
                + "|ingredients=" + AlchemyRecipes.allIngredients().size()
                + "|brewTicks=" + AlchemyRecipes.BREW_TICKS);
        System.out.println("HERB-TABLE-OK|herbs=" + HerbCatalog.ALL.size()
                + "|maxTier=" + HerbCatalog.maxTier());

        for (HerbCatalog.Herb herb : HerbCatalog.ALL) {
            System.out.println("HERB|" + herb.id() + "|" + herb.cn() + "|" + herb.tier()
                    + "|" + herb.use() + "|" + herb.growthChance() + "|" + herb.petalMax()
                    + "|" + herb.seedId() + "|" + herb.petalId());
        }

        for (AlchemyRecipes.Brew brew : AlchemyRecipes.BREWS) {
            System.out.println("BREW|" + brew.main() + "|" + brew.aux() + "|" + brew.result()
                    + "|" + brew.count() + "|" + brew.tier() + "|" + brew.brewTicks()
                    + "|" + brew.note());
        }
        for (String pill : AlchemyRecipes.PILL_IDS) {
            boolean covered = AlchemyRecipes.coveredPills().contains(pill);
            System.out.println("PILL|" + pill + "|" + (covered ? "covered" : "UNREACHABLE"));
        }
        System.out.println("FUEL|" + AlchemyRecipes.FUEL_LEAF + "|"
                + AlchemyRecipes.FUEL_LEAF_TICKS + "|" + AlchemyRecipes.FUEL_BLAZE + "|"
                + AlchemyRecipes.FUEL_BLAZE_TICKS);
    }

    private AlchemySelfCheck() {
    }
}
