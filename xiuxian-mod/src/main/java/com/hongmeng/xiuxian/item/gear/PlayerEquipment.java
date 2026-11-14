package com.hongmeng.xiuxian.item.gear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家的法宝装备栏。
 *
 * <p>五个槽位，对应五种法宝部位（定义在 {@link EquipmentSlots}）。
 * **只有装备在槽位里的法宝才生效**——之前那套"丢背包里自动生效"的做法已经废弃，
 * 因为有了真正的装备栏之后，两种语义并存只会让玩家困惑。</p>
 *
 * <p>数据存在玩家的 capability 里（跟着存档走），
 * 客户端只读缓存，所有变更都由 {@code EquipmentActionPacket} 走服务端。</p>
 */
public class PlayerEquipment {

    /** 槽位元数据（纯数据类里，避免离线验证器被注册表卡住）。 */
    public static final String[] SLOT_KEYS = EquipmentSlots.SLOT_KEYS;
    public static final String[] SLOT_NAMES = EquipmentSlots.SLOT_NAMES;
    public static final int SLOT_COUNT = EquipmentSlots.COUNT;

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];

    public PlayerEquipment() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }

    public static int indexOf(String slotKey) {
        return EquipmentSlots.indexOf(slotKey);
    }

    public static String nameOf(int index) {
        return EquipmentSlots.nameOf(index);
    }

    public ItemStack get(int index) {
        if (!EquipmentSlots.isValid(index)) {
            return ItemStack.EMPTY;
        }
        return slots[index];
    }

    public void set(int index, ItemStack stack) {
        if (!EquipmentSlots.isValid(index)) {
            return;
        }
        slots[index] = stack == null ? ItemStack.EMPTY : stack;
    }

    public boolean isEmpty(int index) {
        return get(index).isEmpty();
    }

    /** 已装备的法宝数量。 */
    public int equippedCount() {
        int n = 0;
        for (ItemStack s : slots) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** 这件法宝应该装到哪个槽位；不是法宝返回 -1。 */
    public static int slotFor(ItemStack stack) {
        if (stack.getItem() instanceof ArtifactItem art) {
            return EquipmentSlots.indexOf(art.slotKey());
        }
        return -1;
    }

    /** 五个槽位是否全部装满。 */
    public boolean isFull() {
        return equippedCount() >= SLOT_COUNT;
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    public void save(CompoundTag tag) {
        CompoundTag root = new CompoundTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!slots[i].isEmpty()) {
                CompoundTag s = new CompoundTag();
                slots[i].save(s);
                root.put(SLOT_KEYS[i], s);
            }
        }
        tag.put("HongmengEquipment", root);
    }

    public void load(CompoundTag tag) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = ItemStack.EMPTY;
        }
        if (!tag.contains("HongmengEquipment")) {
            return;
        }
        CompoundTag root = tag.getCompound("HongmengEquipment");
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (root.contains(SLOT_KEYS[i])) {
                slots[i] = ItemStack.of(root.getCompound(SLOT_KEYS[i]));
            }
        }
    }

    public void copyFrom(PlayerEquipment other) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = other.slots[i].copy();
        }
    }

    // ------------------------------------------------------------------
    // 网络
    // ------------------------------------------------------------------

    public void toNetwork(FriendlyByteBuf buf) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            buf.writeItem(slots[i]);
        }
    }

    public void fromNetwork(FriendlyByteBuf buf) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = buf.readItem();
        }
    }
}
