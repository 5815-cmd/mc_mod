package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * <b>可堆叠材料物品</b>：把堆叠上限抬到配置值（默认 127）。
 *
 * <p>玩家原话："增加物品堆叠上限到1024"。</p>
 *
 * <h2>为什么不是 1024</h2>
 *
 * <p>1024 <b>做不了</b>，不是没做。1.20.1 的 {@code ItemStack} 里数量字段
 * 在内存里是 {@code int}，但<b>落盘与上线都是 1 字节</b>：</p>
 *
 * <pre>
 *   ItemStack.save(CompoundTag)        ldc_w "Count" ; getfield count:I ; i2b
 *                                      ; putByte(String,B)
 *   ItemStack(CompoundTag)             ldc_w "Count" ; getByte(String)B
 *                                      ; putfield count:I
 *   FriendlyByteBuf.writeItemStack     getCount() ; writeByte(I)
 *   ItemStack.CODEC（另一条路）         getstatic Codec.INT ; fieldOf("Count")
 * </pre>
 *
 * <p>所以 {@code (byte) 1024 == 0}：存盘时写进去 0，读回来
 * {@code count == 0}，而 {@code ItemStack.isEmpty()} 对 {@code count <= 0}
 * 返回 true——<b>一整堆东西在下线再上线之后静默消失</b>，日志里一个字都没有。
 * 256 的倍数（256 / 512 / 1024 / 2048）全部归零，其它 &gt;127 的值会被截成
 * 乱七八糟的小数字（300 → 44）。实测见
 * {@code _cache/stackprobe/StackSizeProbe.java}。</p>
 *
 * <p>所以这里给的是**安全上限 127**（有符号字节的顶），并且：
 * <ul>
 *   <li><b>只作用于本模组自己的物品</b>——覆写 Forge 的
 *       {@code IForgeItem#getMaxStackSize(ItemStack)} 补丁点，
 *       不碰原版、不碰别的模组（改了原版物品会让别人的存档出问题）；</li>
 *   <li>上限走 {@link HongmengConfig} 的 {@code stack.materialStackSize}，
 *       整合包作者想保守可以调回 64；</li>
 *   <li>{@code Inventory.add} / {@code ItemHandlerHelper} 都会读
 *       {@code getMaxStackSize()}，所以合并、装包、漏斗全部自动跟着走。</li>
 * </ul>
 *
 * <p><b>不要</b>在这里调 {@code super.getMaxStackSize(stack)}：原版
 * {@code Item.getMaxStackSize()} 是 {@code final}，读的是构造时的
 * {@code maxStackSize} 字段，跟 {@code Properties.stacksTo()} 同一个值，
 * 没有额外信息。</p>
 */
public class StackableMaterialItem extends Item {

    public StackableMaterialItem(Properties properties) {
        super(properties);
    }

    /**
     * Forge 的堆叠上限钩子。
     *
     * <p>物品注册发生在配置加载之前，所以这里第一次调用时配置可能还没有值——
     * {@link HongmengConfig.Server#clampedMaterialStackSize()} 会兜住，
     * 读不到就返回 {@link HongmengConfig#SAFE_STACK_MAX}。</p>
     */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return safeStackSize();
    }

    /** 配置里的上限，夹在 1..127。配置没起来时返回 127。 */
    public static int safeStackSize() {
        try {
            if (HongmengConfig.SERVER != null) {
                return HongmengConfig.SERVER.clampedMaterialStackSize();
            }
        } catch (Throwable ignored) {
            // 配置还没加载：按安全上限走
        }
        return HongmengConfig.SAFE_STACK_MAX;
    }
}
