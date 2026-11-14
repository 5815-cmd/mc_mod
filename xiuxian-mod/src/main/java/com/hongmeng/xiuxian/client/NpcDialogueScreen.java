package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import com.hongmeng.xiuxian.network.ClientCultivationCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 对话文字面板。
 *
 * <p>所有 NPC 共用这一块面板，{@link CultivationNpcEntity.Panel} 决定内容走向：</p>
 * <ul>
 *   <li>{@code ELDER}：长老的劝诫，末尾<b>按玩家当前境界补一句当下的建议</b>
 *       （境界从 {@link ClientCultivationCache} 读，客户端自己算，服务端不用同步）；</li>
 *   <li>{@code ALCHEMY}：炼丹师的丹药闲谈；</li>
 *   <li>{@code MERCHANT}：<b>不画这块面板</b>，直接换成既有的坊市界面
 *       {@link com.hongmeng.xiuxian.shop.ShopScreen}——交易逻辑一行都不用重写。
 *       云游商人与第四批的<b>丹药商人</b>都走这一条；</li>
 *   <li>{@code SMITH}（炼器师）/ {@code GUIDE}（秘境向导）/ {@code BOUNTY}（悬赏牌看板）：
 *       第四批新增，都是"把话说清楚"的 NPC，因此仍然走这块面板，
 *       只是标题条的颜色与底部的落款不同——玩家一眼能分清自己在跟谁说话。</li>
 * </ul>
 *
 * <p>面板只在客户端开：{@code Entity#interact} 在生存与创造模式下客户端都会跑到
 * （旁观模式被原版拦下），所以不需要额外加一个"打开界面"的自定义网络包。</p>
 */
@OnlyIn(Dist.CLIENT)
public class NpcDialogueScreen extends Screen {

    private static final int BG = 0xF0141210;
    private static final int PANEL = 0xF01E1A16;
    private static final int BORDER = 0xFFD8A85A;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int DIM = 0xFF9A9088;
    private static final int GOLD = 0xFFFFD24A;

    private static final int W = 300;
    private static final int PAD = 12;
    private static final int LINE_H = 12;

    private final String npcTitle;
    private final List<String> rawLines;
    private final List<FormattedCharSequence> wrapped = new ArrayList<>();

    /** 这一张面板属于谁——标题条颜色与落款都跟着它走。 */
    private final CultivationNpcEntity.Panel panel;

    private NpcDialogueScreen(CultivationNpcEntity.Panel panel, String title, List<String> lines) {
        super(Component.literal(title));
        this.panel = panel;
        this.npcTitle = title;
        this.rawLines = new ArrayList<>(lines);
        if (panel == CultivationNpcEntity.Panel.ELDER) {
            this.rawLines.add("");
            this.rawLines.add("\u00a76【长老的提点】\u00a7r" + realmHint());
        }
    }

    /**
     * 每一位 NPC 的强调色。
     *
     * <p>第四批新增了三个只会说话的 NPC（炼器师 / 秘境向导 / 悬赏牌），
     * 如果三张面板长得一模一样，玩家点开之后会分不清自己在跟谁讲话。
     * 这里只改<b>边框与标题条</b>的颜色：结构不动，一眼能认。</p>
     */
    private int accent() {
        return switch (this.panel) {
            case ELDER -> 0xFFD8A85A;       // 金：门派
            case ALCHEMY -> 0xFF6FBF7A;     // 青绿：丹炉
            case SMITH -> 0xFFB8BEC8;       // 铁灰：砧子
            case GUIDE -> 0xFF6FA8DC;       // 天蓝：远行
            case BOUNTY -> 0xFFB08A52;      // 木黄：告示牌
            case MERCHANT -> 0xFFD8A85A;    // 坊市（通常不走这块面板）
        };
    }

    /**
     * 按境界给一句建议。
     *
     * <p>这是"NPC 真的有用"的那一点点：炼气期让你去砍狼攒灵叶，
     * 金丹之后提醒你别碰虚空裂隙。不再多给奖励——奖励归丹药与怪掉落。</p>
     */
    private static String realmHint() {
        var data = ClientCultivationCache.get();
        int tier = data.realm().tier();
        String realm = data.realm().cn();
        if (tier <= 1) {
            return "\u00a7f你尚在" + realm + "\u00a7f。山门外的灵狼最好杀，掉灵叶，"
                    + "灵叶能换丹药——先把回灵丹备足，再谈突破。";
        }
        if (tier <= 3) {
            return "\u00a7f你已至" + realm + "\u00a7f。妖兽秘境的门槛快到了，"
                    + "雷鹰会俯冲，石傀推不动——对付石傀别硬拼，用法宝磨。";
        }
        if (tier <= 6) {
            return "\u00a7f你已至" + realm + "\u00a7f。虚空蛟的毒要带解毒之物，"
                    + "烈焰猿属火，冰系功法对它最有效。";
        }
        return "\u00a7f你已至" + realm + "\u00a7f，老朽没什么可教的了。"
                + "只一句：越往上，越要记得自己为什么开始修。";
    }

    /** 面板一开就换掉坊市那条路：商人不聊天，直接做生意。 */
    public static void open(CultivationNpcEntity.Panel panel, String title, List<String> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (panel == CultivationNpcEntity.Panel.MERCHANT) {
            minecraft.setScreen(new com.hongmeng.xiuxian.shop.ShopScreen());
            return;
        }
        minecraft.setScreen(new NpcDialogueScreen(panel, title, lines));
    }

    @Override
    protected void init() {
        super.init();
        this.wrapped.clear();
        int textWidth = W - PAD * 2;
        for (String line : this.rawLines) {
            if (line.isEmpty()) {
                this.wrapped.add(FormattedCharSequence.EMPTY);
                continue;
            }
            this.wrapped.addAll(this.font.split(Component.literal(line), textWidth));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int height = 52 + this.wrapped.size() * LINE_H;
        int left = (this.width - W) / 2;
        int top = (this.height - height) / 2;

        int accent = this.accent();
        g.fill(left - 1, top - 1, left + W + 1, top + height + 1, accent);
        g.fill(left, top, left + W, top + height, BG);

        // 标题条
        g.fill(left, top, left + W, top + 22, PANEL);
        g.drawString(this.font, "\u00a76\u00a7l" + this.npcTitle, left + PAD, top + 7, TEXT, false);
        g.fill(left, top + 22, left + W, top + 23, accent);

        int y = top + 30;
        for (FormattedCharSequence line : this.wrapped) {
            g.drawString(this.font, line, left + PAD, y, TEXT, false);
            y += LINE_H;
        }

        String hint = "按 ESC 离开";
        g.drawString(this.font, "\u00a78" + hint,
                left + W - PAD - this.font.width(hint), top + height - 12, DIM, false);
        g.drawString(this.font, "\u00a78[\u00a7e" + this.npcTitle + "\u00a78]",
                left + PAD, top + height - 12, GOLD, false);
    }

    /** 面板不吃输入：就是一块"告示牌"。 */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
