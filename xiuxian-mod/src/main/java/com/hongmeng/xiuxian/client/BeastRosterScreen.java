package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.beast.BeastCommand;
import com.hongmeng.xiuxian.entity.beast.BeastGrowth;
import com.hongmeng.xiuxian.entity.beast.BeastKind;
import com.hongmeng.xiuxian.entity.beast.BeastManager;
import com.hongmeng.xiuxian.entity.beast.BeastRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 灵兽名册 —— 潜行右键灵兽袋打开的界面。
 *
 * <p>规格把"名册"列为灵兽袋的三种交互之一。这里的实现刻意<b>不新增网络包</b>：
 * 袋子里那只 {@link ItemStack} 的 NBT 客户端本来就有（物品栏同步），
 * 所以界面直接读它——名字、成长阶段、指令、血量、护甲、载重全部现成。
 * 协议 id 顺序因此一点没动。</p>
 *
 * <p>布局沿用 {@code QuestScreen} 的"左列表右详情"口径，保持全模组界面一致。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BeastRosterScreen extends Screen {

    private static final int W = 268;
    private static final int H = 168;
    private static final int LIST_W = 106;
    private static final int BG = 0xF0121622;
    private static final int PANEL = 0xF01A1F30;
    private static final int BORDER = 0xFF6FD8C8;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int DIM = 0xFF8A90A8;
    private static final int GOLD = 0xFFFFD24A;

    private final ItemStack pouch;
    private int selected = 0;
    private int count;

    private int listLeft;
    private int listTop;
    private int rowHeight = 16;

    public BeastRosterScreen(ItemStack pouch) {
        super(Component.literal("灵兽名册"));
        this.pouch = pouch == null ? ItemStack.EMPTY : pouch;
        this.count = BeastManager.storedCount(this.pouch);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, BG);

        g.drawString(font, "§b§l灵兽名册", left + 8, top + 6, TEXT, false);
        g.drawString(font, "§8" + count + " / " + BeastManager.POUCH_CAPACITY,
                left + W - 46, top + 6, DIM, false);

        listLeft = left + 6;
        listTop = top + 20;
        int listH = H - 30;
        g.fill(listLeft, listTop, listLeft + LIST_W, listTop + listH, PANEL);

        if (count == 0) {
            g.drawString(font, "§7袋子是空的。", listLeft + 8, listTop + 8, DIM, false);
            g.drawString(font, "§8右键灵兽即可收入。", listLeft + 8, listTop + 22, DIM, false);
            return;
        }

        for (int i = 0; i < count && i * rowHeight < listH; i++) {
            CompoundTag tag = entryTag(i);
            if (tag == null) {
                continue;
            }
            int y = listTop + 2 + i * rowHeight;
            boolean hover = mouseX >= listLeft && mouseX <= listLeft + LIST_W
                    && mouseY >= y && mouseY <= y + rowHeight - 1;
            boolean active = i == selected;
            if (active || hover) {
                g.fill(listLeft + 1, y, listLeft + LIST_W - 1, y + rowHeight - 1,
                        active ? 0xFF2A3A4A : 0xFF202634);
            }
            BeastKind kind = KindOf(tag);
            BeastGrowth growth = BeastGrowth.byName(tag.getString("HmBeastGrowth"));
            g.drawString(font, "§b" + growth.cn() + " §f" + kind.cn(),
                    listLeft + 6, y + 4, TEXT, false);
            g.drawString(font, "§c" + (int) tag.getFloat("Health"), listLeft + LIST_W - 26,
                    y + 4, DIM, false);
        }

        // ---------- 右侧详情 ----------
        CompoundTag tag = entryTag(selected);
        if (tag == null) {
            return;
        }
        int dx = left + LIST_W + 16;
        int dy = top + 22;

        BeastKind kind = KindOf(tag);
        BeastGrowth growth = BeastGrowth.byName(tag.getString("HmBeastGrowth"));
        BeastCommand command = BeastCommand.byName(tag.getString("HmBeastCommand"));
        boolean armored = tag.getBoolean("HmBeastArmor");
        double health = tag.getFloat("Health");
        double maxHealth = tag.contains("Attributes")
                ? maxHealthOf(tag) : kind.baseHealth() * growth.statMultiplier();

        g.drawString(font, "§f§l" + kind.cn(), dx, dy, TEXT, false);
        g.drawString(font, "§8" + kind.id(), dx, dy + 11, DIM, false);

        line(g, dx, dy + 28, "职能", kind.role().cn());
        line(g, dx, dy + 40, "阶段", growth.colored());
        line(g, dx, dy + 52, "指令", command.colored());
        line(g, dx, dy + 64, "生命", "§c" + (int) health + " §8/ §7" + (int) maxHealth);
        line(g, dx, dy + 76, "护甲", armored ? "§b已装备" : "§8无");
        int row = dy + 88;

        // 骑乘系统关闭：载重 / 灵力两栏是骑乘信息，直接隐藏。
        if (com.hongmeng.xiuxian.entity.beast.BeastRiding.ENABLED) {
            line(g, dx, row, "载重", kind.canCarryCargo()
                    ? "§7" + kind.cargoSlots() + " 格" : "§8不能驮货");
            row += 12;

            // 灵力消耗：飞行坐骑才有
            if (kind.flying()) {
                line(g, dx, row, "灵力", "§d" + trim(kind.manaDrain()) + " /tick"
                        + (kind.role() == BeastRole.AIR_SCOUT
                        ? " §8(悬停 " + trim(kind.hoverManaDrain()) + ")" : ""));
            } else {
                line(g, dx, row, "灵力", "§a不消耗");
            }
            row += 12;
        }

        // 成长进度：妖丹
        int fed = tag.getInt("HmBeastFed");
        if (growth.canGrow()) {
            line(g, dx, row, "妖丹", "§e" + fed + " §8/ §7" + growth.feedCost()
                    + " §8→ " + growth.next().cn());
        } else {
            line(g, dx, row, "妖丹", "§6已是化形");
        }

        g.drawString(font, "§8按 ESC 关闭 · 右键空气放出", left + 8, top + H - 12, DIM, false);
    }

    private void line(GuiGraphics g, int x, int y, String label, String value) {
        g.drawString(font, "§8" + label, x, y, DIM, false);
        g.drawString(font, value, x + 34, y, TEXT, false);
    }

    private CompoundTag entryTag(int index) {
        CompoundTag wrapper = BeastManager.storedBeast(pouch, index);
        return wrapper == null ? null : wrapper.getCompound(BeastManager.POUCH_ENTRY);
    }

    /** 从实体 NBT 的 Attributes 里读最大生命。 */
    private static double maxHealthOf(CompoundTag tag) {
        try {
            var list = tag.getList("Attributes", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag attr = list.getCompound(i);
                if ("minecraft:generic.max_health".equals(attr.getString("Name"))) {
                    return attr.getDouble("Base");
                }
            }
        } catch (Throwable ignored) {
            // 老存档 / 手改过的 NBT：退回默认值
        }
        return 20.0D;
    }

    private static BeastKind KindOf(CompoundTag tag) {
        return BeastManager.kindFromTag(tag);
    }

    private static String trim(double v) {
        if (Math.abs(v - Math.round(v)) < 0.01D) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (count > 0 && button == 0
                && mouseX >= listLeft && mouseX <= listLeft + LIST_W
                && mouseY >= listTop) {
            int index = (int) ((mouseY - listTop - 2) / rowHeight);
            if (index >= 0 && index < count) {
                selected = index;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 供绘制使用的金色常量（保留给未来的操作按钮）。 */
    @SuppressWarnings("unused")
    private static final int ACCENT = GOLD;
}
