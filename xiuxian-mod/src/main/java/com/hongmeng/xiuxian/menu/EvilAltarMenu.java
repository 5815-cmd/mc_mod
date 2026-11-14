package com.hongmeng.xiuxian.menu;

import com.hongmeng.xiuxian.block.EvilAltarBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * <b>恶之祭坛界面</b>：5 个固定槽 + 玩家背包。
 *
 * <p>召唤按钮不在这里处理 —— 界面上点按钮时客户端直接发
 * {@code EvilSummonPacket} 给服务端，服务端校验五格是否就绪再放行。</p>
 */
public class EvilAltarMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerLevelAccess access;
    /** 祭坛坐标（客户端从这里发召唤包）。 */
    private net.minecraft.core.BlockPos altarPos = net.minecraft.core.BlockPos.ZERO;

    /** 服务端构造。 */
    public EvilAltarMenu(int id, Inventory inv, EvilAltarBlockEntity be) {
        this(id, inv, be, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
    }

    /**
     * 客户端构造：从缓冲区读方块坐标，再查回方块实体。
     *
     * <p>⚠ 缓冲区<b>只能读一次</b>：`resolve()` 内部会 `readBlockPos()`，
     * 每多调一次就多吃 8 字节，坐标与容器都会错位
     * （曾经写成 `resolve(...)` 调三次，导致 altarPos 是垃圾值、
     * 点「召唤」服务端找不到方块，什么都不发生）。</p>
     */
    public EvilAltarMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf));
    }

    private EvilAltarMenu(int id, Inventory inv, ContainerAndAccess ca) {
        this(id, inv, ca.container, ca.access);
        // 坐标在这里一次性落定（ca 是同一个对象，不会再读缓冲区）
        this.altarPos = ca.pos;
    }

    /** 客户端查方块实体；查不到就退化成空容器（界面仍然能开，只是只读）。 */
    private static ContainerAndAccess resolve(Inventory inv, FriendlyByteBuf buf) {
        try {
            net.minecraft.core.BlockPos pos = buf.readBlockPos();
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null
                    && mc.level.getBlockEntity(pos)
                    instanceof EvilAltarBlockEntity be) {
                return new ContainerAndAccess(be,
                        ContainerLevelAccess.create(mc.level, pos), pos);
            }
        } catch (Throwable ignored) {
            // 退化
        }
        return new ContainerAndAccess(new SimpleContainer(EvilAltarBlockEntity.SLOTS),
                ContainerLevelAccess.NULL, net.minecraft.core.BlockPos.ZERO);
    }

    private record ContainerAndAccess(Container container,
                                      ContainerLevelAccess access,
                                      net.minecraft.core.BlockPos pos) {
    }

    private EvilAltarMenu(int id, Inventory inv, Container container,
                          ContainerLevelAccess access) {
        super(com.hongmeng.xiuxian.ModEvil.EVIL_ALTAR_MENU.get(), id);
        this.container = container;
        this.access = access;

        // 5 个祭坛槽（一排 5 格，像五枚凭证）
        for (int i = 0; i < EvilAltarBlockEntity.SLOTS; i++) {
            this.addSlot(new Slot(container, i, 30 + i * 22, 30) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(this.getContainerSlot(), stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18,
                        102 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 160));
        }
    }

    /** 五格是否就绪（界面用来决定召唤按钮是否可点）。 */
    /** 祭坛坐标。 */
    public net.minecraft.core.BlockPos altarPos() {
        return altarPos;
    }

    public boolean ready() {
        for (int i = 0; i < EvilAltarBlockEntity.SLOTS; i++) {
            ItemStack st = this.container.getItem(i);
            if (st.isEmpty() || st.getItem()
                    != EvilAltarBlockEntity.acceptedFor(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack out = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return out;
        }
        ItemStack st = slot.getItem();
        out = st.copy();
        if (index < EvilAltarBlockEntity.SLOTS) {
            if (!this.moveItemStackTo(st, EvilAltarBlockEntity.SLOTS,
                    this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从背包点东西：找到它能放的祭坛槽
            int target = -1;
            for (int i = 0; i < EvilAltarBlockEntity.SLOTS; i++) {
                if (container.canPlaceItem(i, st) && !this.slots.get(i).hasItem()) {
                    target = i;
                    break;
                }
            }
            if (target < 0 || !this.moveItemStackTo(st, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (st.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return out;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock()
                        instanceof com.hongmeng.xiuxian.block.EvilAltarBlock
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D) <= 64.0D,
                true);
    }
}
