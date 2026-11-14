package com.hongmeng.xiuxian.item.gear;

import com.hongmeng.xiuxian.network.ClientCultivationCache;
import com.hongmeng.xiuxian.network.EquipmentActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 法宝装备栏：画在原版背包界面里，玩家模型右上方的空位上。
 *
 * <p>五个竖排槽位（法冠 / 道袍 / 法裤 / 云履 / 玉佩）。</p>
 *
 * <p><b>操作：</b></p>
 * <ul>
 *   <li>左键点<b>空格位</b>：自动把背包里第一件该部位的法宝装上去</li>
 *   <li>左键点<b>已装备的槽位</b>：卸下来退回背包</li>
 *   <li>左键点<b>背包里的法宝</b>：直接装到它对应的槽位（原来的退回背包）</li>
 * </ul>
 *
 * <p>所有变更都发 {@link EquipmentActionPacket} 给服务端执行，
 * 客户端只负责画与收集意图。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EquipmentPanel {

    private static final int SLOT = 18;
    private static final int GAP = 2;
    /** 底板上方的标题条高度（画「法宝」两个字用）。 */
    private static final int HEADER = 13;
    /** 底板内边距。 */
    private static final int PAD = 3;
    /** 背包界面的原始尺寸（原版 InventoryScreen 的常量，这里是 1.20.1 的值）。 */
    private static final int GUI_W = 176;
    private static final int GUI_H = 166;

    private EquipmentPanel() {
    }

    /** 背包界面的左上角 X。 */
    private static int guiLeft(Screen screen) {
        return (screen.width - GUI_W) / 2;
    }

    private static int guiTop(Screen screen) {
        return (screen.height - GUI_H) / 2;
    }

    /** 一次布局算出来的结果：面板左上角 + 每个槽位的位置。 */
    private record Layout(int x, int y, int[] sx, int[] sy, int panelW, int panelH) {
    }

    /**
     * 算布局：**优先放在背包正上方**（玩家要求「法宝放上方」）。
     *
     * <p>原来的写法是"背包右上角 + 竖排"，槽位正好压在合成输出格和物品 tooltip 上，
     * 玩家投诉过一次。现在：</p>
     *
     * <ul>
     *   <li>槽位横排，5 个一共 5×18 + 4×2 = 98 宽，正好和背包一样宽；</li>
     *   <li>上方（guiTop 之后的 y ≥ 4）放得下就放上方；</li>
     *   <li>放不下（小窗口 / GUI 缩放很大）就退到背包**右侧外面**——
     *       宁可贴边也不能再盖住背包内容。</li>
     * </ul>
     */
    private static Layout layout(Screen screen) {
        int n = PlayerEquipment.SLOT_COUNT;
        int panelW = n * SLOT + (n - 1) * GAP + PAD * 2;
        int panelH = HEADER + SLOT + PAD;

        int gx = guiLeft(screen);
        int gy = guiTop(screen);

        int px;
        int py;
        int aboveY = gy - panelH - 2;
        if (aboveY >= 4) {
            // 放在背包正上方，与背包左对齐
            px = gx;
            py = aboveY;
        } else {
            // 退路：背包右侧外面（不覆盖背包）
            px = Math.min(screen.width - panelW - 2, gx + GUI_W + 4);
            py = Math.max(4, gy);
        }

        int[] sx = new int[n];
        int[] sy = new int[n];
        for (int i = 0; i < n; i++) {
            sx[i] = px + PAD + i * (SLOT + GAP);
            sy[i] = py + HEADER;
        }
        return new Layout(px, py, sx, sy, panelW, panelH);
    }

    /** 该不该显示：只有灵根已觉醒（也就是真正开始修仙了）才显示。 */
    public static boolean shouldShow() {
        return ClientCultivationCache.get().isAwakened();
    }

    // ------------------------------------------------------------------
    // 绘制
    // ------------------------------------------------------------------

    /** 在背包界面渲染完之后调用。 */
    public static void render(InventoryScreen screen, GuiGraphics g,
                              int mouseX, int mouseY) {
        if (!shouldShow()) {
            return;
        }
        var equipped = ClientCultivationCache.get().getEquipment();
        Layout L = layout(screen);

        // 面板底 + 标题
        g.fill(L.x(), L.y(), L.x() + L.panelW(), L.y() + L.panelH(), 0xE0101420);
        g.renderOutline(L.x(), L.y(), L.panelW(), L.panelH(), 0xFF8A6FD8);
        g.drawString(Minecraft.getInstance().font, "§d法宝", L.x() + PAD, L.y() + 3,
                0xFFFFFF, false);

        int hoveredSlot = -1;
        for (int i = 0; i < PlayerEquipment.SLOT_COUNT; i++) {
            int sx = L.sx()[i];
            int sy = L.sy()[i];
            boolean hover = mouseX >= sx && mouseX < sx + SLOT
                    && mouseY >= sy && mouseY < sy + SLOT;
            if (hover) {
                hoveredSlot = i;
            }

            // 槽位底
            g.fill(sx, sy, sx + SLOT, sy + SLOT, 0xFF1A1F30);
            g.renderOutline(sx, sy, SLOT, SLOT, hover ? 0xFFFFD24A
                    : (equipped.isEmpty(i) ? 0xFF40404A : 0xFF8A6FD8));

            ItemStack stack = equipped.get(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, sx + 1, sy + 1);
                g.renderItemDecorations(Minecraft.getInstance().font, stack, sx + 1, sy + 1);
            } else {
                // 空格位用部位名的首字提示
                String hint = PlayerEquipment.SLOT_NAMES[i].substring(0, 1);
                g.drawString(Minecraft.getInstance().font, "§8" + hint,
                        sx + 6, sy + 5, 0xFFFFFF, false);
            }
        }

        // 悬停提示
        if (hoveredSlot >= 0) {
            ItemStack stack = equipped.get(hoveredSlot);
            if (stack.isEmpty()) {
                g.renderTooltip(Minecraft.getInstance().font,
                        Component.literal("§7" + PlayerEquipment.SLOT_NAMES[hoveredSlot]
                                + " §8（空）\n§8左键：自动装上背包里对应部位的法宝"),
                        mouseX, mouseY);
            } else {
                g.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
            }
        }
    }

    // ------------------------------------------------------------------
    // 点击
    // ------------------------------------------------------------------

    /** @return true 表示这次点击被装备栏吃掉了 */
    public static boolean mouseClicked(InventoryScreen screen, double mouseX, double mouseY,
                                      int button) {
        if (!shouldShow() || button != 0) {
            return false;
        }
        var equipped = ClientCultivationCache.get().getEquipment();
        Layout L = layout(screen);

        // 1) 点装备槽位
        for (int i = 0; i < PlayerEquipment.SLOT_COUNT; i++) {
            int sx = L.sx()[i];
            int sy = L.sy()[i];
            if (mouseX < sx || mouseX >= sx + SLOT || mouseY < sy || mouseY >= sy + SLOT) {
                continue;
            }
            if (equipped.isEmpty(i)) {
                // 空格位：从背包里找第一件该部位的法宝装上
                int inv = findInInventory(screen, PlayerEquipment.SLOT_KEYS[i]);
                if (inv < 0) {
                    message("§7背包里没有「" + PlayerEquipment.SLOT_NAMES[i] + "」类的法宝。");
                } else {
                    EquipmentActionPacket.send(EquipmentActionPacket.Action.EQUIP, inv);
                }
            } else {
                EquipmentActionPacket.send(EquipmentActionPacket.Action.UNEQUIP, i);
            }
            return true;
        }

        // 2) 点背包里的法宝 → 直接装到对应槽位
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Slot hit = findSlotAt(screen, mouseX, mouseY);
            if (hit != null && hit.container instanceof Inventory
                    && hit.hasItem() && PlayerEquipment.slotFor(hit.getItem()) >= 0) {
                EquipmentActionPacket.send(EquipmentActionPacket.Action.EQUIP, hit.getContainerSlot());
                return true;
            }
        }
        return false;
    }

    /** 在玩家背包里找该部位的第一件法宝，返回背包索引；没有返回 -1。 */
    private static int findInInventory(InventoryScreen screen, String slotKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return -1;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot.container instanceof Inventory) || !slot.hasItem()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (PlayerEquipment.slotFor(stack) == PlayerEquipment.indexOf(slotKey)) {
                return slot.getContainerSlot();
            }
        }
        return -1;
    }

    /** 找出鼠标位置下的原版槽位。 */
    private static Slot findSlotAt(InventoryScreen screen, double mouseX, double mouseY) {
        for (Slot slot : screen.getMenu().slots) {
            // 槽位坐标是相对界面左上角的
            int sx = guiLeft(screen) + slot.x;
            int sy = guiTop(screen) + slot.y;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                return slot;
            }
        }
        return null;
    }

    private static void message(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), true);
        }
    }
}
