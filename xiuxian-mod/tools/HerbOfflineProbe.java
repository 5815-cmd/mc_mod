/*
 * 探针：离线（无游戏）加载灵植相关的类，看看会不会 "Not bootstrapped"。
 *
 * 目的：LogicVerifier 那条"绝不能碰注册表"的红线。如果
 * com.hongmeng.xiuxian.block.SpiritHerbBlock（继承 CropBlock → Block）
 * 能在离线 JVM 里完成静态初始化，那么验证器就可以直接断言它的常量；
 * 否则验证器只能断言纯数据类（AlchemyRecipes / HerbCatalog）。
 *
 * 用法：java -cp "_cache/modout;<cpjar>" tools/HerbOfflineProbe.java
 */

public final class HerbOfflineProbe {

    public static void main(String[] args) {
        probe("com.hongmeng.xiuxian.alchemy.AlchemyRecipes");
        probe("com.hongmeng.xiuxian.alchemy.HerbCatalog");
        probe("com.hongmeng.xiuxian.alchemy.AlchemyFurnaceBlockEntity");
        probe("com.hongmeng.xiuxian.alchemy.AlchemyFurnaceMenu");
        probe("com.hongmeng.xiuxian.block.SpiritHerbBlock");
        probe("com.hongmeng.xiuxian.block.LingtianSoilBlock");
        probe("com.hongmeng.xiuxian.ModBlocks");
        probe("com.hongmeng.xiuxian.item.ModItems");
        probe("com.hongmeng.xiuxian.alchemy.ModAlchemy");
    }

    private static void probe(String name) {
        try {
            Class<?> c = Class.forName(name);
            System.out.println("OK   " + name + "  (loaded " + c.getSimpleName() + ")");
        } catch (Throwable t) {
            System.out.println("FAIL " + name + "  -> " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private HerbOfflineProbe() {
    }
}
