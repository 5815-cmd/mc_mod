package com.hongmeng.xiuxian.domain;

import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.network.ClientCultivationCache;
import com.hongmeng.xiuxian.network.DomainActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 道宫界面（G 键）。
 *
 * <p>道印境起可用。展示灵海里的道宫、品质与元素加成，
 * 并提供三个操作：凝聚道宫、破碎重凝、开/关本命领域。</p>
 */
@OnlyIn(Dist.CLIENT)
public class DomainScreen extends Screen {

    private static final int W = 300;
    private static final int H = 196;
    private static final int BG = 0xF0140A1A;
    private static final int PANEL = 0xF01E1030;
    private static final int BORDER = 0xFFB070E8;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int DIM = 0xFF9090A8;
    private static final int GOLD = 0xFFFFD24A;

    // 按钮命中区
    private int condenseX;
    private int condenseY;
    private int condenseW;
    private int condenseH;
    private int toggleX;
    private int toggleY;
    private int toggleW;
    private int toggleH;
    // 友伤开关按钮（画在面板左下角）
    private int ffX;
    private int ffY;
    private int ffW;
    private int ffH;
    private final int[] shatterX = new int[4];
    private final int[] shatterY = new int[4];
    private int shatterW;
    private int shatterH;
    private int shatterCount = 0;

    public DomainScreen() {
        super(Component.literal("道宫"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, BG);

        CultivationData data = ClientCultivationCache.get();
        DomainData domains = data.getDomainData();

        g.drawString(font, "§5§l灵海 · 道宫", left + 10, top + 8, TEXT, false);
        g.fill(left, top + 20, left + W, top + 21, 0x66B070E8);

        int x = left + 12;
        int y = top + 26;

        // ---- 状态栏 ----
        g.drawString(font, "§7境界：§f" + data.realm().cn()
                + "   §7能量：§a" + domains.energy() + "§7/§a"
                + DomainData.CONDENSE_COST
                + "   §7道宫：§f" + domains.domainCount() + "§7/§f" + domains.maxDomains(),
                x, y, TEXT, false);
        y += 11;
        int pityText = domains.totalCondensed() >= DomainData.PITY_HONGMENG
                ? Integer.MAX_VALUE
                : (domains.totalCondensed() >= DomainData.PITY_IMMORTAL
                ? domains.energyIntoPityHongmeng()
                : domains.energyIntoPityImmortal());
        String pity = domains.totalCondensed() >= DomainData.PITY_HONGMENG
                ? "§6已进入鸿蒙保底"
                : "§8距保底还差 " + pityText + " 次（100 保底仙品 / 500 保底鸿蒙）";
        g.drawString(font, pity, x, y, DIM, false);
        y += 13;

        // ---- 剑/道宫列表 ----
        g.drawString(font, "§b§l已凝道宫", x, y, TEXT, false);
        y += 12;
        shatterCount = 0;

        if (domains.domainCount() == 0) {
            g.drawString(font, "§8灵海里空无一物。", x, y, DIM, false);
            y += 12;
        } else {
            for (int i = 0; i < domains.domains().size(); i++) {
                DomainData.Domain d = domains.domains().get(i);
                g.fill(x - 2, y - 2, left + W - 12, y + 22, PANEL);
                g.drawString(font, "§7第 " + (i + 1) + " 座   "
                        + d.quality().colored() + "   §7元素：§b" + d.element().display(),
                        x, y, TEXT, false);
                y += 10;
                g.drawString(font, "§7功法伤害 §c" + d.quality().damageText()
                                + "   §7释放速度 §e" + d.quality().castText(),
                        x, y, TEXT, false);

                // 破碎按钮
                int bx = left + W - 62;
                int by = y - 10;
                g.fill(bx, by, bx + 48, by + 14, 0xFF5A2020);
                g.renderOutline(bx, by, 48, 14, 0xFFE06060);
                g.drawString(font, "§c破碎重凝", bx + 5, by + 3, TEXT, false);
                if (shatterCount < shatterX.length) {
                    shatterX[shatterCount] = bx;
                    shatterY[shatterCount] = by;
                    shatterCount++;
                }
                shatterW = 48;
                shatterH = 14;
                y += 16;
            }
        }
        y += 4;

        // ---- 加成总览 ----
        g.drawString(font, "§b§l领域加成（开启后生效）", x, y, TEXT, false);
        y += 11;
        double speed = domains.castSpeedBonus();
        double best = 0.0D;
        for (DomainData.Domain d : domains.domains()) {
            best = Math.max(best, d.quality().techniqueDamage());
        }
        g.drawString(font, "§7范围 §f" + DomainEffectTable.rangeText(
                        com.hongmeng.xiuxian.compat.HongmengConfig.SERVER.domainChunks.get())
                        + "   §7同元素功法伤害 §c+"
                        + (int) Math.round(best * 100) + "%   §7释放速度 §e+"
                        + (int) Math.round(speed * 100) + "%", x, y, TEXT, false);
        y += 12;
        // 道宫元素决定领域的表现与效果：一行一个元素（数字全部来自纯数据表）
        if (domains.domainCount() == 0) {
            g.drawString(font, "§8（领域效果按道宫元素走：火雨 / 藤缠 / 涨潮 / 锐气 / 束缚 / 冰冻 / 雷击 / 风刃）",
                    x, y, DIM, false);
            y += 10;
        } else {
            for (DomainData.Domain d : domains.domains()) {
                if (!DomainEffectTable.hasEffect(d.element())) {
                    continue;
                }
                g.drawString(font, "§8· §b" + d.element().cn() + " §8"
                        + DomainEffectTable.shortSummary(d.element()), x, y, TEXT, false);
                y += 10;
            }
        }
        y += 6;

        // ---- 操作按钮 ----
        condenseW = 96;
        condenseH = 16;
        condenseX = x;
        condenseY = y;
        boolean canCondense = data.realm().hasDomain()
                && domains.domainCount() < domains.maxDomains()
                && domains.energy() >= DomainData.CONDENSE_COST;
        int cColor = canCondense ? 0xFF3A2A5A : 0xFF24202E;
        g.fill(condenseX, condenseY, condenseX + condenseW, condenseY + condenseH, cColor);
        g.renderOutline(condenseX, condenseY, condenseW, condenseH,
                canCondense ? BORDER : 0xFF50505A);
        g.drawString(font, "§f凝聚道宫 §8(10 能量)", condenseX + 4, condenseY + 4, TEXT, false);

        toggleW = 96;
        toggleH = 16;
        toggleX = x + condenseW + 8;
        toggleY = y;
        boolean on = domains.isDomainActive() && domains.domainCount() > 0;
        g.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH,
                on ? 0xFF2E5A3A : 0xFF3A2E2E);
        g.renderOutline(toggleX, toggleY, toggleW, toggleH, BORDER);
        g.drawString(font, "§f本命领域：" + (on ? "§a开" : "§c关"),
                toggleX + 6, toggleY + 4, TEXT, false);

        // ---- 友伤开关（规格 §2.1：默认关，这里与 R 面板都能切）----
        boolean friendlyFire = data.isDomainFriendlyFire();
        ffW = 132;
        ffH = 14;
        ffX = x;
        ffY = top + H - ffH - 6;
        g.fill(ffX, ffY, ffX + ffW, ffY + ffH, friendlyFire ? 0xFF4A2430 : 0xFF24382A);
        g.renderOutline(ffX, ffY, ffW, ffH, BORDER);
        g.drawString(font, "§f领域友伤：" + (friendlyFire ? "§c开" : "§a关") + " §8(点击切换)",
                ffX + 4, ffY + 3, TEXT, false);

        g.drawString(font, "§8G 关闭 · 能量靠杀怪获得", left + W - 118, top + H - 11, DIM, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (inside(mouseX, mouseY, condenseX, condenseY, condenseW, condenseH)) {
            DomainActionPacket.send(DomainActionPacket.Action.CONDENSE, 0);
            return true;
        }
        if (inside(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH)) {
            DomainActionPacket.send(DomainActionPacket.Action.TOGGLE, 0);
            return true;
        }
        if (inside(mouseX, mouseY, ffX, ffY, ffW, ffH)) {
            DomainActionPacket.send(DomainActionPacket.Action.FRIENDLY_FIRE, 0);
            return true;
        }
        for (int i = 0; i < shatterCount; i++) {
            if (inside(mouseX, mouseY, shatterX[i], shatterY[i], shatterW, shatterH)) {
                DomainActionPacket.send(DomainActionPacket.Action.SHATTER, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_G
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
