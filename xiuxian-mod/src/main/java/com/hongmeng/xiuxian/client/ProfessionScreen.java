package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.talent.Profession;
import com.hongmeng.xiuxian.talent.ProfessionRecipe;
import com.hongmeng.xiuxian.talent.ProfessionCrafting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>副业界面</b> —— 炼丹 / 炼器 / 阵法。
 *
 * <h2>为什么需要它</h2>
 * <p>原来副业只有 {@code /hongmeng craft} 命令：玩家<b>看不到自己有什么材料</b>、
 * <b>看不到能做什么</b>、也<b>不知道成功率是多少</b>。这在操作上是最大的摩擦 ——
 * 一个玩法如果必须靠记命令和手动数背包才能玩，等于没做。</p>
 *
 * <h2>数据从哪来（三类来源，刻意分开）</h2>
 * <ul>
 *   <li><b>配方表</b>（静态）→ 直接读 {@link ProfessionRecipe}。它<b>不碰注册表</b>，
 *       客户端可以算（这也是我当初把数据与注册拆开的原因之一）；</li>
 *   <li><b>熟练度等级</b>（服务端状态）→ 读
 *       {@code ClientCultivationCache.summaryPart(6)} 里同步过来的"副业 阵法2/炼丹1/炼器0"；</li>
 *   <li><b>材料够不够</b>（本地背包）→ 客户端自己数 —— 背包本来就在本地，
 *       不需要服务端告诉。这样"点了没反应"的情况会少很多：界面上直接标红。</li>
 * </ul>
 *
 * <h2>⚠ 界面只负责"显示与点击"</h2>
 * <p>真正的判定与产出在服务端（{@link ProfessionCrafting}）。客户端点击只发
 * {@code CraftPacket}。所以即使客户端的"材料够"判断有偏差，服务端也会再校验一次 ——
 * <b>不会出现"客户端说能做、实际白扣材料"</b>。</p>
 */
public class ProfessionScreen extends Screen {

    private static final int W = 320;
    private static final int H = 210;
    private static final int BG = 0xF0140A1A;
    private static final int PANEL = 0xF01E1030;
    private static final int BORDER = 0xFFB070E8;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int DIM = 0xFF9090A8;
    private static final int GOLD = 0xFFFFD24A;
    private static final int GREEN = 0xFF7CE89B;
    private static final int RED = 0xFFE86B6B;

    /** 当前副业页签。 */
    private int tab = 0;

    /** 每行配方对应的屏幕 y（用于点击命中）。 */
    private final List<Integer> rowY = new ArrayList<>();
    private final List<String> rowId = new ArrayList<>();

    public ProfessionScreen() {
        super(Component.literal("副业"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;
        g.fill(left, top, left + W, top + H, BG);
        g.renderOutline(left, top, W, H, BORDER);
        g.fill(left + 2, top + 2, left + W - 2, top + 24, PANEL);
        g.drawString(this.font, "§d§l副业 · 炼丹 / 炼器 / 阵法",
                left + 8, top + 8, TEXT, false);
        g.drawString(this.font, "§8材料从本地背包数，点击即制作",
                left + 170, top + 9, DIM, false);

        // ---- 页签 ----
        int tx = left + 8;
        int ty = top + 28;
        Profession[] all = Profession.values();
        for (int i = 0; i < all.length; i++) {
            Profession p = all[i];
            int w = 62;
            boolean sel = i == this.tab;
            g.fill(tx, ty, tx + w, ty + 14, sel ? 0xFF3A2050 : 0x80201028);
            g.renderOutline(tx, ty, w, 14, sel ? BORDER : 0x60B070E8);
            g.drawString(this.font, (sel ? "§f" : "§7") + p.cn(), tx + 6, ty + 3,
                    TEXT, false);
            tx += w + 6;
        }
        ty += 20;

        Profession cur = all[this.tab];
        int rank = rankOf(cur);
        g.drawString(this.font, String.format(
                        "§7熟练度 §e%d§7/§f%d   §7成功率加成 §a+%d%%   §8产出 +%d%%",
                        rank, Profession.maxRank(),
                        Math.round(Profession.failCut(rank) * 100),
                        Math.round(Profession.yieldBonus(rank) * 100)),
                left + 8, ty, TEXT, false);
        ty += 14;
        g.drawString(this.font, "§8" + cur.desc(), left + 8, ty, DIM, false);
        ty += 12;

        // ---- 配方列表 ----
        rowY.clear();
        rowId.clear();
        for (ProfessionRecipe.Recipe r : ProfessionRecipe.of(cur)) {
            boolean unlocked = ProfessionRecipe.unlocked(r, rank);
            double ch = ProfessionRecipe.successChance(r, rank);
            boolean enough = hasMaterials(r);
            int x = left + 8;
            int w = W - 16;
            boolean hover = mouseX >= x && mouseX <= x + w
                    && mouseY >= ty && mouseY <= ty + 26;
            g.fill(x, ty, x + w, ty + 26, hover ? 0x502A1840 : 0x30140820);
            g.renderOutline(x, ty, w, 26, unlocked ? 0x60B070E8 : 0x40FFFFFF);

            String head = (unlocked ? (enough ? "§a" : "§e") : "§8") + r.cn()
                    + " §8(" + r.id() + ")";
            g.drawString(this.font, head, x + 5, ty + 3, TEXT, false);
            g.drawString(this.font, String.format("%s成功率 %d%%", unlocked ? "§7" : "§8",
                    Math.round(ch * 100)), x + w - 78, ty + 3, TEXT, false);

            // 材料：缺的标红
            StringBuilder mats = new StringBuilder();
            for (ProfessionRecipe.Material m : r.materials()) {
                if (mats.length() > 0) {
                    mats.append(" §8+ ");
                }
                int have = countInInventory(m.item());
                boolean ok = have >= m.count();
                mats.append(ok ? "§a" : "§c")
                        .append(shortName(m.item()))
                        .append("§7 ").append(have).append("/").append(m.count());
            }
            if (!unlocked) {
                mats.append(" §8需要 ").append(r.minRank()).append(" 级");
            }
            g.drawString(this.font, mats.toString(), x + 5, ty + 14, TEXT, false);

            rowY.add(ty);
            rowId.add(r.id());
            ty += 29;
        }

        g.drawString(this.font, "§8左键制作 · ESC 关闭", left + 8, top + H - 14, DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;
        // 页签
        int tx = left + 8;
        int ty = top + 28;
        for (int i = 0; i < Profession.values().length; i++) {
            if (mouseX >= tx && mouseX <= tx + 62 && mouseY >= ty && mouseY <= ty + 14) {
                this.tab = i;
                return true;
            }
            tx += 68;
        }
        // 配方行
        int x = left + 8;
        int w = W - 16;
        for (int i = 0; i < rowY.size(); i++) {
            int y = rowY.get(i);
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 26) {
                // 只发包，不做任何本地判定 —— 判定与扣材料一律在服务端
                com.hongmeng.xiuxian.network.CraftPacket.send(rowId.get(i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------
    // 本地数据
    // ------------------------------------------------------------------

    /**
     * 从同步过来的摘要里解析某门副业的熟练度。
     *
     * <p>摘要格式见 {@code Talents.clientSummary}：
     * 第 6 段是「阵法2/炼丹1/炼器0」。</p>
     */
    private int rankOf(Profession p) {
        String blob = com.hongmeng.xiuxian.network.ClientCultivationCache.summaryPart(6);
        if (blob.isEmpty()) {
            return 0;
        }
        int idx = blob.indexOf(p.cn());
        if (idx < 0) {
            return 0;
        }
        int i = idx + p.cn().length();
        StringBuilder num = new StringBuilder();
        while (i < blob.length() && Character.isDigit(blob.charAt(i))) {
            num.append(blob.charAt(i));
            i++;
        }
        try {
            return num.length() == 0 ? 0 : Integer.parseInt(num.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /** 数本地背包里某种物品的数量（与 {@code ProfessionCrafting} 同一口径）。 */
    private int countInInventory(String itemId) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return 0;
        }
        Item item = resolve(itemId);
        if (item == null) {
            return 0;
        }
        int n = 0;
        var inv = this.minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && st.getItem() == item) {
                n += st.getCount();
            }
        }
        return n;
    }

    private boolean hasMaterials(ProfessionRecipe.Recipe r) {
        for (ProfessionRecipe.Material m : r.materials()) {
            if (countInInventory(m.item()) < m.count()) {
                return false;
            }
        }
        return true;
    }

    private static Item resolve(String id) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? null : BuiltInRegistries.ITEM.get(rl);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 把 `hongmeng:spirit_iron_ore` 这种长 id 缩短成便于显示的短名。 */
    private static String shortName(String itemId) {
        String s = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        return s.length() <= 18 ? s : s.substring(0, 17) + "…";
    }
}
