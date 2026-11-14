package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.item.heaven.HeavenItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>恶之祭坛的方块实体</b>：5 个固定槽位。
 *
 * <p>每个槽只接受<b>一种</b>掉落物（槽位 = 维度）。放错位置放不进去 ——
 * 比"任意五格凑齐"更清楚，也避免玩家把五个东西乱塞后不知道为什么不能召唤。</p>
 */
public class EvilAltarBlockEntity extends BaseContainerBlockEntity
        implements net.minecraft.world.MenuProvider {

    /** 槽位数。 */
    public static final int SLOTS = 5;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public EvilAltarBlockEntity(BlockPos pos, BlockState state) {
        super(com.hongmeng.xiuxian.ModEvil.EVIL_ALTAR.get(), pos, state);
    }

    /** 每个槽接受的物品。 */
    public static Item acceptedFor(int slot) {
        return switch (slot) {
            case 0 -> ModItems.PUPPET_CORE.get();
            case 1 -> ModItems.BEAST_HEART.get();
            case 2 -> ModItems.BLOOD_CORE.get();
            case 3 -> ModItems.VOID_EYE.get();
            case 4 -> HeavenItems.HEAVEN_CROWN.get();
            default -> null;
        };
    }

    /** 五格是否都放满了正确的物品。 */
    public boolean ready() {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = this.items.get(i);
            if (st.isEmpty() || st.getItem() != acceptedFor(i)) {
                return false;
            }
        }
        return true;
    }

    /** 消耗五格（召唤时调用）。 */
    public void consumeAll() {
        for (int i = 0; i < SLOTS; i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        this.setChanged();
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack st : this.items) {
            if (!st.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(this.items, slot, amount);
        if (!out.isEmpty()) {
            this.setChanged();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.setChanged();
    }

    /** 只允许对应槽位的物品。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        Item want = acceptedFor(slot);
        return want != null && stack.getItem() == want;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hongmeng.evil_altar");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new com.hongmeng.xiuxian.menu.EvilAltarMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
    }
}
