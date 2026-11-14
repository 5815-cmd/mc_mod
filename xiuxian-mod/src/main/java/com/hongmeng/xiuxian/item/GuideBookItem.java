package com.hongmeng.xiuxian.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * <b>鸿蒙修行录</b>：mod 自带的图鉴书（玩家要求：类似植物魔法的那本书）。
 *
 * <p>右键翻开，里面能查：</p>
 * <ul>
 *   <li><b>每个物品的获得途径</b>（合成 / 冶炼 / 杀怪 / 宝箱 / 挖矿 / 采集 / 坊市 / 任务 / 交易 / 维度）；</li>
 *   <li><b>方块、法宝、护甲、秘籍</b>分栏；</li>
 *   <li><b>NPC 介绍</b>与在哪遇到；</li>
 *   <li><b>建筑与群系</b>（哪座结构、哪个群系、里面有什么）；</li>
 *   <li><b>全局搜索</b>：输字即筛，搜"宫殿"能找到结构、搜"灵草"能找到物品与生成它的群系。</li>
 * </ul>
 *
 * <h2>⚠ 这个类的写法有讲究（踩过一次服务端崩溃）</h2>
 * <p>打开界面**不能**写成
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::openScreen)} ——
 * 那样 JVM 在服务端链接这个 lambda 时就会去解析 {@code Screen} 相关类型，
 * FML 的 {@code RuntimeDistCleaner} 会直接抛
 * <i>"Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist
 * DEDICATED_SERVER"</i>，整个模组加载失败（真实事故）。</p>
 *
 * <p>正确做法是这里的**反射式持有者**：服务端编译到的只是一句
 * {@code Class.forName("...GuideBookHolder")} 字符串，不会触发客户端类链接；
 * 真正要开界面时先判断 {@link net.minecraftforge.fml.loading.FMLEnvironment#dist}，
 * 只有客户端才会去加载那个持有者类。</p>
 */
public class GuideBookItem extends Item {

    /**
     * 客户端唯一的类名（**字符串**，所以服务端不会链接它）。
     *
     * <p>嵌套在 {@link GuideBookItem} 里是刻意的：只有真正要开界面时才会被加载。</p>
     */
    private static final String CLIENT_HOLDER =
            "com.hongmeng.xiuxian.item.GuideBookItem$ClientHolder";

    public GuideBookItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                  InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openGuide();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 打开界面。
     *
     * <p>先判断物理侧再反射调用 —— <b>服务端永远走不到 {@code Class.forName} 那一步</b>，
     * 所以不会有任何客户端类被链接。</p>
     */
    private static void openGuide() {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                != net.minecraftforge.api.distmarker.Dist.CLIENT) {
            return;
        }
        try {
            Class<?> holder = Class.forName(CLIENT_HOLDER);
            holder.getMethod("open").invoke(null);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[hongmeng] 修行录界面打不开：{}", t.toString());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip,
                                TooltipFlag flag) {
        tip.add(Component.literal("\u00a77右键翻开 · 查合成 / 获得途径 / 结构 / 群系 / NPC"));
        tip.add(Component.literal("\u00a78书里可搜索：物品 / 结构 / 群系 / NPC"));
    }

    /**
     * <b>客户端专用持有者</b>：真正碰 {@code Screen} 的那一段全在这里。
     *
     * <p>它只会被 {@link GuideBookItem#openGuide()} 通过反射加载，
     * 而那个方法在服务端会被 {@code FMLEnvironment.dist} 提前挡掉 ——
     * 所以专用服务器上这个类永远不会被加载。</p>
     */
    public static final class ClientHolder {

        private ClientHolder() {
        }

        /** 客户端才存在的方法：开修行录。 */
        public static void open() {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.hongmeng.xiuxian.client.GuideBookScreen());
        }
    }
}
