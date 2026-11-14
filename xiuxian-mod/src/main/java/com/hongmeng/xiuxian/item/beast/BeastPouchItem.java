package com.hongmeng.xiuxian.item.beast;

import com.hongmeng.xiuxian.entity.beast.BeastCommand;
import com.hongmeng.xiuxian.entity.beast.BeastGrowth;
import com.hongmeng.xiuxian.entity.beast.BeastKind;
import com.hongmeng.xiuxian.entity.beast.BeastManager;
import com.hongmeng.xiuxian.entity.beast.SpiritBeastEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 灵兽袋 —— 灵兽的收纳与放出。
 *
 * <p>规格把它写成<b>强制项</b>，理由也很实在："坐骑走丢／卡住"是这类系统
 * 最经典的痛点。只要袋子在，玩家永远有一条退路：收起来，换个地方放出来。</p>
 *
 * <h3>三种右键</h3>
 * <ul>
 *   <li><b>右键灵兽</b> → 收进袋子（完整 NBT，含 UUID、成长阶段、指令、护甲）</li>
 *   <li><b>潜行右键</b> → 打开灵兽名册（客户端界面，直接读手里这只袋子的 NBT，
 *       <b>不需要任何新的网络包</b>）</li>
 *   <li><b>右键空气</b> → 放出袋子里最后收进去的那一只</li>
 * </ul>
 *
 * <p>NBT 结构刻意做得扁平可读：{@code HongmengBeasts = [{Beast: {...实体完整 NBT...}}]}，
 * 每一只独立成条，这样名册界面能逐条读出名字与状态，
 * 也方便以后做"指定放哪一只"。</p>
 */
public class BeastPouchItem extends Item {

    public BeastPouchItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // 右键灵兽：收进袋子
    // ------------------------------------------------------------------

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof SpiritBeastEntity beast)) {
            return InteractionResult.PASS;
        }
        if (!beast.isOwnedBy(player)) {
            sp.displayClientMessage(Component.literal(
                    "\u00a7c\u8fd9\u4e0d\u662f\u4f60\u7684\u7075\u517d\u3002"), true);
            return InteractionResult.CONSUME;
        }
        BeastManager.storeBeast(stack, beast, sp);
        // 无论袋子满没满，这次交互都归灵兽系统处理（满了会给出提示），
        // 不能落回原版——否则原版会把灵兽当成"可繁殖动物"之类的东西处理
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------
    // 右键空气：放出；潜行右键：名册
    // ------------------------------------------------------------------

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            // 名册是纯客户端界面：它直接读手里这只袋子的 NBT，
            // 所以一个网络包都不需要，协议 id 也就不会被扰动。
            if (level.isClientSide()) {
                openRoster(stack);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(
                    stack, level.isClientSide());
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            BeastManager.releaseLast(stack, sp);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(
                stack, level.isClientSide());
    }

    /** 只在客户端调；拆出来是为了让服务端分支不必知道 Screen 的存在。 */
    private static void openRoster(ItemStack stack) {
        com.hongmeng.xiuxian.client.ClientBeastScreens.openRoster(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 0;
    }

    /**
     * 手里拿着灵兽袋时，**必须允许左键挖方块**。
     *
     * <p><b>这里原来写的是 `return false`，是个真 bug（玩家报过两次"挖矿没掉落"）</b>：
     * {@code canAttackBlock} 与"右键放置方块"毫无关系 —— Forge 只在
     * {@code ForgeHooks.onBlockBreakEvent} 里问它一次：
     * <pre>
     *   if (!stack.isEmpty() &amp;&amp; !stack.getItem().canAttackBlock(state, level, pos, player)) {
     *       cancel = true;        // → onBlockBreakEvent 返回 -1
     *   }
     * </pre>
     * 返回 false 的后果是：**手里拿着袋子挖任何方块，服务端都会取消这次破坏**，
     * 于是方块不删、不掉落物；而客户端已经本地预测"方块没了"——
     * 表现就是「方块看不见、踩上去还在、什么都没掉」（幽灵方块）。
     * 玩家精确描述过这个现象，排查了一整轮才定位到这里。</p>
     *
     * <p>袋子本来就不需要"禁止挖方块"这个行为：它真正要挡的是**右键**，
     * 而右键走的是 {@link #use} / {@link #interactLivingEntity}，与这里无关。</p>
     */
    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state,
                                  Level level, net.minecraft.core.BlockPos pos,
                                  Player player) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        int count = BeastManager.storedCount(stack);
        tooltip.add(Component.literal("\u00a77\u5df2\u6536\u7eb3\uff1a\u00a7e" + count
                + " / " + BeastManager.POUCH_CAPACITY).withStyle(ChatFormatting.GRAY));

        // 名册摘要直接进 tooltip：不进界面也能看见袋子里有什么
        for (int i = 0; i < count; i++) {
            CompoundTag wrapper = BeastManager.storedBeast(stack, i);
            if (wrapper == null) {
                continue;
            }
            CompoundTag tag = wrapper.getCompound(BeastManager.POUCH_ENTRY);
            BeastKind kind = BeastKind.byId(pathOf(tag));
            BeastGrowth growth = BeastGrowth.byName(tag.getString("HmBeastGrowth"));
            BeastCommand command = BeastCommand.byName(tag.getString("HmBeastCommand"));
            boolean armored = tag.getBoolean("HmBeastArmor");
            double health = tag.getFloat("Health");
            tooltip.add(Component.literal("  \u00a78\u00b7 \u00a7b" + growth.cn() + " \u00a7f"
                    + kind.cn() + " \u00a78[" + command.cn() + "]"
                    + (armored ? " \u00a77\u26e8" : "")
                    + " \u00a7c\u2764 " + (int) health));
        }

        tooltip.add(Component.literal("\u00a78\u53f3\u952e\u7075\u517d\uff1a\u6536\u5165"));
        tooltip.add(Component.literal("\u00a78\u53f3\u952e\u7a7a\u5730\uff1a\u653e\u51fa"));
        tooltip.add(Component.literal("\u00a78\u6f5c\u884c\u53f3\u952e\uff1a\u7075\u517d\u540d\u518c"));
    }

    /** 从实体 NBT 的 {@code id} 里取路径部分。 */
    private static String pathOf(CompoundTag tag) {
        String id = tag == null ? "" : tag.getString("id");
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }
}
