package com.hongmeng.xiuxian.alchemy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 丹炉界面：主料 / 辅料 / 燃料 / 产物 + 玩家背包。
 *
 * <p>用的是标准 {@link AbstractContainerMenu} 那一套（和原版熔炉完全同款），
 * 所以**不需要额外发包**——槽位内容由菜单同步，进度与燃料由
 * {@link ContainerData} 同步。</p>
 *
 * <p>客户端侧用 {@link SimpleContainer} 占位（原版熔炉也是这么干的：
 * 客户端不需要方块实体，内容物由服务端同步下来），
 * 因此 {@link #stillValid} 直接问容器自己——服务端问的是方块实体，
 * 客户端问的是恒真的占位容器。</p>
 */
public class AlchemyFurnaceMenu extends AbstractContainerMenu {

    public static final int SLOT_MAIN = 0;
    public static final int SLOT_AUX = 1;
    public static final int SLOT_FUEL = 2;
    public static final int SLOT_RESULT = 3;
    public static final int SLOT_COUNT = 4;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_BREW_TOTAL = 1;
    public static final int DATA_FUEL = 2;
    public static final int DATA_FUEL_TOTAL = 3;
    public static final int DATA_COUNT = 4;

    /** 玩家背包（27 格）在 slots 里的起点；后面还有 9 格快捷栏。 */
    public static final int INV_START = SLOT_COUNT;
    public static final int INV_END = INV_START + 36;

    /** 四个功能槽的界面坐标，Screen 画槽底时也用这几个数。 */
    public static final int[][] SLOT_POS = {
            {44, 15},   // 主料
            {44, 39},   // 辅料
            {44, 63},   // 燃料
            {116, 35},  // 产物
    };

    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** 客户端构造（{@code MenuType} 的工厂）：内容物由服务端同步，这里只要一套空壳。 */
    public AlchemyFurnaceMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(DATA_COUNT),
                ContainerLevelAccess.NULL);
    }

    public AlchemyFurnaceMenu(int id, Inventory inventory, Container container, ContainerData data,
                              ContainerLevelAccess access) {
        super(ModAlchemy.ALCHEMY_FURNACE_MENU.get(), id);
        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.access = access;
        container.startOpen(inventory.player);

        // ---- 丹炉四格 ----
        addSlot(new Slot(container, SLOT_MAIN, SLOT_POS[0][0], SLOT_POS[0][1]));
        addSlot(new Slot(container, SLOT_AUX, SLOT_POS[1][0], SLOT_POS[1][1]));
        addSlot(new Slot(container, SLOT_FUEL, SLOT_POS[2][0], SLOT_POS[2][1]));
        addSlot(new AlchemyResultSlot(container, SLOT_RESULT, SLOT_POS[3][0], SLOT_POS[3][1]));

        // ---- 玩家背包 ----
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    // ---- 给 Screen 读的进度 ----

    public int progress() {
        return data.get(DATA_PROGRESS);
    }

    public int brewTotal() {
        return Math.max(1, data.get(DATA_BREW_TOTAL));
    }

    public int fuel() {
        return data.get(DATA_FUEL);
    }

    public int fuelTotal() {
        return Math.max(1, data.get(DATA_FUEL_TOTAL));
    }

    /** 当前主料 + 辅料指向哪颗丹药（客户端只用于提示，不参与逻辑）。 */
    public AlchemyRecipes.Brew previewBrew() {
        return AlchemyRecipes.find(itemId(container.getItem(SLOT_MAIN)),
                itemId(container.getItem(SLOT_AUX)));
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, com.hongmeng.xiuxian.ModBlocks.ALCHEMY_FURNACE.get())
                && container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index == SLOT_RESULT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (AlchemyRecipes.isFuel(itemId(stack))) {
            if (!moveItemStackTo(stack, SLOT_FUEL, SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SLOT_MAIN, SLOT_AUX + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    /** 产物槽：只能拿不能放（放进去的东西会凭空消失）。 */
    private static class AlchemyResultSlot extends Slot {

        AlchemyResultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
