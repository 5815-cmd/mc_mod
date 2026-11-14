package com.hongmeng.xiuxian;

import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.Karma;
import com.hongmeng.xiuxian.core.Realm;
import com.hongmeng.xiuxian.domain.DomainData;
import com.hongmeng.xiuxian.network.ClientCultivationCache;
import com.hongmeng.xiuxian.spirit.SpiritRoot;
import com.hongmeng.xiuxian.technique.Technique;
import com.hongmeng.xiuxian.technique.TechniqueData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 修炼面板（R 键）。
 *
 * <h2>分页</h2>
 *
 * <p>玩家原话："我们的 r 界面要做区分不能全做一块，你可以分好几个页面
 * r 打开后可以切换，不能让东西都挤一块"。</p>
 *
 * <p>原来这一整块从"灵根"一路堆到"当前任务"，靠一条纵向流水线画下来，
 * 越加越长、越来越挤。现在拆成 8 页，页签在最上面一排，点页签或按
 * {@code [} / {@code ]} 切换（也可以用 ← →、Tab，以及数字键 1~8 直达）：</p>
 *
 * <pre>
 *   修行    境界 / 修为进度 / 破境按钮 / 空手挖掘 / 门派 / 当前任务
 *   丹药    修为来源一览（灵石 / 聚灵丹 / 凝元丹 / 丹炉修为丹，按当前境界实时算）
 *   灵根    灵根品质 · 属性 · 名称 · 修炼倍率 · 说明
 *   道宫    能量 / 道宫列表 / 保底进度 / 操作指引
 *   功法    已修习列表（点一行选中）+ 主动槽 / 被动槽 + 选中功法详情
 *   灵兽    灵兽图鉴（七列数值表 + 怎么收服）
 *   属性    能力 / 常驻速度 II 开关 / 业力 / 属性上限自检
 *   图鉴    按键索引 + 玩法索引 + 常用命令
 * </pre>
 *
 * <h2>与旧版的关系（功能一个都没少）</h2>
 *
 * <ul>
 *   <li>破境按钮（含"业力封境 / 修为未满 / 渡劫道数 / 成功率"全部状态）
 *       → <b>修行</b>页，判断逻辑一字未改；</li>
 *   <li>常驻速度 II 开关 → <b>属性</b>页，命中区与切换逻辑原样搬过去；</li>
 *   <li>道宫那一段（能量 / 道宫列表 / 保底）→ <b>道宫</b>页；</li>
 *   <li>功法列（点击选中）→ 原来的右侧常驻栏保留，并在 <b>功法</b>页里
 *       另给一份带槽位与详情的完整版；</li>
 *   <li>灵兽图鉴 → 从"右上角一个小页签"升级成 <b>灵兽</b>页，内容不变；</li>
 *   <li>属性上限自检 + 业力 → <b>属性</b>页；</li>
 *   <li>新增 <b>图鉴</b>页，把散落的按键与玩法索引收在一处。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class CultivationPanelScreen extends Screen {

    // ------------------------------------------------------------------
    // 版面
    // ------------------------------------------------------------------

    /** 面板宽度：七列表格的灵兽图鉴要 296，就按这个当统一宽度。 */
    private static final int PANEL_W = 296;
    /** 面板高度：一屏能放下所有页（最长的"修行"页约 270）。 */
    private static final int PANEL_H = 300;

    /** 内容区左右内边距。 */
    private static final int PAD = 12;
    /** 页签条高度。 */
    private static final int TAB_H = 14;
    /** 页签条 y 相对面板顶的偏移（标题下面一行）。 */
    private static final int TAB_TOP = 20;

    private static final int GOLD = 0xFFFFD24A;
    private static final int BG = 0xE8141A2E;
    private static final int BORDER = 0xFF8A6FD8;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int LABEL = 0xFF9AA0C0;
    private static final int ROW_SEL = 0x40FFD24A;
    private static final int ROW_HOVER = 0x20FFFFFF;

    // ------------------------------------------------------------------
    // 页
    // ------------------------------------------------------------------

    private static final String[] PAGES = {
            "修行", "丹药", "灵根", "道宫", "功法", "灵兽", "属性", "图鉴"
    };

    private static final int PAGE_CULTIVATION = 0;
    /** 2026-11-07 新增：修为丹药一览（灵石 / 聚灵丹 / 凝元丹 / 丹炉修为丹）。 */
    private static final int PAGE_PILLS = 1;
    private static final int PAGE_ROOT = 2;
    private static final int PAGE_DOMAIN = 3;
    private static final int PAGE_TECHNIQUE = 4;
    private static final int PAGE_BEAST = 5;
    private static final int PAGE_ATTR = 6;
    private static final int PAGE_CODEX = 7;

    /** 当前页。 */
    private int page = PAGE_CULTIVATION;

    /** 页签命中区（每帧渲染时更新）。 */
    private final int[] tabX = new int[PAGES.length];
    private final int[] tabW = new int[PAGES.length];
    private int tabY;

    // ------------------------------------------------------------------
    // 控件命中区（每帧渲染时更新）
    // ------------------------------------------------------------------

    /** 常驻速度 II 开关按钮。 */
    private int speedButtonX;
    private int speedButtonY;
    private int speedButtonW;
    private int speedButtonH;
    private boolean speedButtonVisible = false;

    /** 择道区：五条道途各自的屏幕 y 与序号（未择道时才有值）。 */
    private final int[] pathHitY = new int[8];
    /**
     * 择道条目对应的道途序号；{@code -1} = 该槽位本帧没有画。
     *
     * <p>⚠ 必须初始化为 -1：用默认值 0 的话，"没画过的槽位"会被当成
     * 道途 0（法修），点到空白处也会给服务端发"选法修"。</p>
     */
    private final int[] pathHitOrd = new int[8];
    private int pathHitX;
    private int pathHitW;
    /**
     * 本帧是否画了择道列表。
     *
     * <p>⚠ 不能复用 {@code talentVisible}：那个是"画了几个天赋节点"，
     * <b>未择道时一个节点都没有</b>，于是点击处理整段被跳过 ——
     * 表现就是"点上去没反应"（界面上却看得见五个选项）。</p>
     */
    private boolean pathChooseVisible = false;

    /**
     * 本帧的鼠标位置。
     *
     * <p>子渲染方法（如 {@code renderPathSection}）拿不到 {@code render} 的
     * 鼠标参数，而"悬停高亮"需要它。所以每帧在 {@code render} 开头存一份。</p>
     */
    private int frameMouseX;
    private int frameMouseY;

    /** 天赋节点命中区（属性页，每帧更新）。 */
    private final int[] talentHitY = new int[8];
    private final String[] talentHitId = new String[8];
    private int talentHitX;
    private int talentHitW;
    private boolean talentVisible = false;

    /** 破境按钮。 */
    private int btButtonX;
    private int btButtonY;
    private int btButtonW;
    private int btButtonH;
    private boolean btButtonVisible = false;

    /**
     * 领域友伤开关（道宫页）。
     *
     * <p>规格 {@code docs/domain-spec.md} §2.1：领域默认不伤害友善生物
     * （玩家 / 驯服的宠物坐骑 / NPC），开关就在这里，状态存在玩家的
     * {@code CultivationData#isDomainFriendlyFire()} 里（跟着存档走）。</p>
     */
    private int ffButtonX;
    private int ffButtonY;
    private int ffButtonW;
    private int ffButtonH;
    private boolean ffButtonVisible = false;

    /** 功法页：点一行就选中它。 */
    private int[] techHitY = new int[0];
    private int techListX;
    private int techListW;
    private boolean techListVisible = false;

    /** 常驻功法栏：点一行就选中它。 */
    private int[] columnHitY = new int[0];
    private int columnX;
    private int columnW;
    private boolean columnVisible = false;

    /** 功法栏宽度（画在主面板右侧的一列）。 */
    private static final int TECH_W = 168;

    public CultivationPanelScreen() {
        super(Component.literal("鸿蒙修仙 · 修炼面板"));
    }

    // ==================================================================
    // 主渲染
    // ==================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.frameMouseX = mouseX;
        this.frameMouseY = mouseY;
        renderBackground(graphics);

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        CultivationData data = ClientCultivationCache.get();

        // 功法页自带宽列，不再重复画右侧那一栏；其余页保留常驻栏
        boolean showColumn = page != PAGE_TECHNIQUE;
        int rightEdge = left + PANEL_W;
        if (showColumn) {
            // 常驻栏画在面板右侧，独立成列，不占主面板的纵向空间
            renderTechniqueColumn(graphics, left + PANEL_W + 8, top, mouseX, mouseY);
            rightEdge = left + PANEL_W + 8 + TECH_W;
        }
        columnVisible = showColumn;

        graphics.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, BORDER);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, BG);

        // ---- 标题 ----
        graphics.drawString(font, Component.literal("§6§l鸿蒙修仙"), left + PAD, top + 7,
                TEXT, false);
        String pageHint = "§8[ / ] ← → 切页 · R 关闭";
        graphics.drawString(font, pageHint,
                left + PANEL_W - PAD - font.width(pageHint), top + 8, TEXT, false);

        renderTabs(graphics, left, top, mouseX, mouseY);

        int x = left + PAD;
        int y = top + TAB_TOP + TAB_H + 6;
        int bottom = top + PANEL_H - 14;

        // 清掉上一帧的命中区，免得切页后还能点到看不见的按钮
        speedButtonVisible = false;
        btButtonVisible = false;
        ffButtonVisible = false;
        techListVisible = false;

        // 未觉醒：图鉴 / 灵兽 / 丹药是纯静态数据或"看怎么涨修为"，照样能看；其余页给同一句提示
        if (!data.isAwakened() && page != PAGE_CODEX && page != PAGE_BEAST
                && page != PAGE_PILLS) {
            graphics.drawString(font, Component.literal("§7灵根尚未觉醒。"), x, y, TEXT, false);
            y += 14;
            graphics.drawString(font, Component.literal("§8稍等片刻，系统会为你分配。"),
                    x, y, TEXT, false);
            y += 18;
            graphics.drawString(font, Component.literal(
                    "§8（灵兽图鉴与图鉴页不依赖灵根，切过去可以看）"), x, y, TEXT, false);
            renderFooter(graphics, left, top);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        switch (page) {
            case PAGE_PILLS -> renderPillsPage(graphics, data, x, y, bottom);
            case PAGE_ROOT -> renderRootPage(graphics, data, x, y, bottom);
            case PAGE_DOMAIN -> renderDomainPage(graphics, data, x, y, bottom);
            case PAGE_TECHNIQUE -> renderTechniquePage(graphics, data, x, y, bottom,
                    mouseX, mouseY);
            case PAGE_BEAST -> renderBeastPage(graphics, data, x, y, bottom);
            case PAGE_ATTR -> renderAttributePage(graphics, data, x, y, bottom);
            case PAGE_CODEX -> renderCodexPage(graphics, data, x, y, bottom);
            default -> renderCultivationPage(graphics, data, x, y, bottom, mouseX, mouseY);
        }

        renderFooter(graphics, left, top);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 底部两行提示（**居中**，避免文字互相重叠）。
     *
     * <p>玩家反馈「这里的字都重叠了」：原来是一行很长的快捷键提示，
     * 在窄窗口／高 GUI 缩放下会与本页自己的提示行压在一起。
     * 现在拆成两行、各自居中，行距固定 10 像素。</p>
     */
    private void renderFooter(GuiGraphics graphics, int left, int top) {
        String[] lines = {
                "§8R 关闭 · B 突破 · O 法术栏 · G 道宫",
                "§8U 灵兽名册 · K 坊市 · T 释放 · Y 切换功法",
        };
        int y = top + PANEL_H - 22;
        for (String line : lines) {
            int w = font.width(line);
            graphics.drawString(font, Component.literal(line),
                    left + (PANEL_W - w) / 2, y, TEXT, false);
            y += 10;
        }
    }

    /** 顶部页签条。 */
    private void renderTabs(GuiGraphics graphics, int left, int top,
                            int mouseX, int mouseY) {
        tabY = top + TAB_TOP;
        int tx = left + PAD;
        int avail = PANEL_W - PAD * 2;
        int each = Math.min(40, Math.max(30,
                (avail - (PAGES.length - 1) * 2) / PAGES.length));
        for (int i = 0; i < PAGES.length; i++) {
            tabX[i] = tx;
            tabW[i] = each;
            boolean active = i == page;
            boolean hover = mouseX >= tx && mouseX <= tx + each
                    && mouseY >= tabY && mouseY <= tabY + TAB_H;
            graphics.fill(tx, tabY, tx + each, tabY + TAB_H,
                    active ? 0xFF3A2A5A : (hover ? 0xFF2A2438 : 0xFF1A1826));
            graphics.renderOutline(tx, tabY, each, TAB_H,
                    active ? BORDER : 0xFF40404A);
            String label = (active ? "§f§l" : "§7") + PAGES[i];
            graphics.drawString(font, label,
                    tx + (each - font.width(label)) / 2, tabY + 3, TEXT, false);
            tx += each + 2;
        }
    }

    // ==================================================================
    // 页 1 · 修行
    // ==================================================================

    /** 境界 / 修为进度 / 破境按钮 / 空手挖掘 / 门派 / 当前任务。 */
    private void renderCultivationPage(GuiGraphics graphics, CultivationData data,
                                       int x, int y, int bottom,
                                       int mouseX, int mouseY) {
        Realm realm = data.realm();
        SpiritRoot root = data.getRoot();

        sectionTitle(graphics, x, y, "境界");
        y += 12;
        graphics.drawString(font, Component.literal("§f当前：§e§l" + realm.cn()), x, y, TEXT, false);
        if (!realm.isMaxRealm()) {
            graphics.drawString(font, Component.literal("§7下一境：§f" + realm.nextCn()),
                    x + 140, y, TEXT, false);
        } else {
            graphics.drawString(font, Component.literal("§6已至绝巅"), x + 140, y, TEXT, false);
        }
        y += 13;

        // 修为进度条
        long need = Math.max(1L, data.needed());
        float ratio = Math.min(1.0F, (float) data.progress() / (float) need);
        int barW = PANEL_W - PAD * 2;
        graphics.fill(x, y, x + barW, y + 9, 0xFF0A0E18);
        graphics.fill(x + 1, y + 1, x + 1 + (int) ((barW - 2) * ratio), y + 8,
                0xFF000000 | (root == null ? 0x66CCFF : (root.rarity().rgb() & 0xFFFFFF)));
        graphics.renderOutline(x, y, barW, 9, BORDER);
        y += 12;
        graphics.drawString(font, Component.literal(
                "§7修为 §f" + data.progress() + " §7/ §f" + data.needed()
                        + "   §7累计 §f" + data.getTotalCultivation()), x, y, TEXT, false);
        y += 14;

        // ---- 破境按钮 ----
        // 修为在本层封顶，必须主动突破才能继续涨。所以这里给一个明确的按钮。
        //
        // 小境界（同大境内）看业力；大境界（跨大境）不看业力，看能不能扛过雷劫。
        btButtonVisible = false;
        if (!realm.isMaxRealm()) {
            boolean full = data.isCultivationFull();
            boolean major = isMajorNext(data);
            // 大境界不看业力，所以只有小境界才可能被业力封住
            boolean sealed = !major && Karma.isCapped(data);
            int btnW = 92;
            int btnH = 16;
            int bx = x;
            int by = y;
            boolean hover = mouseX >= bx && mouseX <= bx + btnW
                    && mouseY >= by && mouseY <= by + btnH;

            int color;
            String label;
            if (sealed) {
                color = 0xFF3A2020;
                label = "§c业力封境";
            } else if (full) {
                color = hover ? 0xFF7A5A1E : 0xFF5A4214;
                label = major ? "§5§l渡 劫" : "§6§l破 境";
            } else {
                color = 0xFF24202E;
                label = "§8修为未满";
            }
            graphics.fill(bx, by, bx + btnW, by + btnH, color);
            graphics.renderOutline(bx, by, btnW, btnH,
                    full && !sealed ? GOLD : 0xFF50505A);
            graphics.drawString(font, label,
                    bx + (btnW - font.width(label)) / 2, by + 4, TEXT, false);

            // 右侧说明
            if (full && !sealed) {
                if (major) {
                    int bolts = com.hongmeng.xiuxian.core.Breakthrough
                            .tribulationBolts(nextTierOf(data));
                    if (bolts > 0) {
                        graphics.drawString(font, Component.literal(
                                        "§7大境界 · 雷劫 §c" + bolts + " §7道"),
                                bx + btnW + 8, by + 4, TEXT, false);
                    } else {
                        graphics.drawString(font, Component.literal(
                                        "§7大境界 · §a无需渡劫"),
                                bx + btnW + 8, by + 4, TEXT, false);
                    }
                } else {
                    graphics.drawString(font, Component.literal(
                                    "§7成功率 §a" + String.format("%.0f%%",
                                            Karma.successRate(data) * 100.0D)),
                            bx + btnW + 8, by + 4, TEXT, false);
                }
            } else if (!full) {
                graphics.drawString(font, Component.literal(
                                "§8还差 §7" + data.cultivationMissing() + " §8修为"),
                        bx + btnW + 8, by + 4, TEXT, false);
            }

            if (!sealed) {
                btButtonVisible = true;
                btButtonX = bx;
                btButtonY = by;
                btButtonW = btnW;
                btButtonH = btnH;
            }
            y += btnH + 6;
        }

        // ---- 境界带来的空手挖掘等级 ----
        graphics.drawString(font, Component.literal(
                "§7空手挖掘：§b" + com.hongmeng.xiuxian.core.RealmMining.describeTier(data)),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal(
                "§7挖掘速度：§b" + realm.harvestSpeedLabel()
                        + "  §7护甲：§b" + (int) realm.armorBonus()
                        + " §8(" + realm.armorRankName() + ")"), x, y, TEXT, false);
        y += 18;

        // ---- 门派 ----
        sectionTitle(graphics, x, y, "门派");
        y += 12;
        com.hongmeng.xiuxian.sect.Sect sect = data.sect();
        if (sect.isNone()) {
            graphics.drawString(font, Component.literal(
                    "§f散修 §8— 可用 §f/sect list §8查看可拜入的门派"), x, y, TEXT, false);
            y += 12;
        } else {
            graphics.drawString(font, Component.literal(
                    "§f" + sect.colored() + " §7· §e" + sect.rankName(data.getContribution())),
                    x, y, TEXT, false);
            graphics.drawString(font, Component.literal(
                    "§7贡献 §f" + data.getContribution()), x + 190, y, TEXT, false);
            y += 11;
            int next = sect.nextRankThreshold(data.getContribution());
            String nextText = next > 0
                    ? "§7距下一身份 §f" + (next - data.getContribution())
                    : "§6已至本门最高身份";
            graphics.drawString(font, Component.literal(nextText), x, y, TEXT, false);
            y += 11;
            if (data.isBetrayed()) {
                graphics.drawString(font, Component.literal(
                        "§4叛徒 §c— 旧主正在派人清理门户"), x, y, TEXT, false);
                y += 11;
            }
        }
        y += 6;

        // ---- 当前任务 ----
        sectionTitle(graphics, x, y, "当前任务");
        y += 12;
        for (FormattedCharSequence line : wrap(
                "§f" + com.hongmeng.xiuxian.core.QuestChain.questText(data),
                PANEL_W - PAD * 2)) {
            if (y > bottom - 10) {
                graphics.drawString(font, Component.literal("§8…"), x, y, TEXT, false);
                break;
            }
            graphics.drawString(font, line, x, y, TEXT, false);
            y += 10;
        }
    }

    // ==================================================================
    // 页 2 · 丹药（修为来源一览）
    // ==================================================================

    /**
     * <b>修为丹药页</b>（2026-11-07 玩家要求「完善 R 键面板」）。
     *
     * <p>把"怎么涨修为"一次说清：灵石直接吃、灵石合成丹、丹炉修为丹三档。
     * 所有数值都按**当前境界**实时算 —— 玩家看到的永远是"现在这一颗能涨多少"，
     * 不是一张固定表格；同时显示背包里各有几颗，省得来回翻包。</p>
     */
    private void renderPillsPage(GuiGraphics graphics, CultivationData data,
                                 int x, int y, int bottom) {
        long need = Math.max(1L, data.realm().need());

        sectionTitle(graphics, x, y, "修为丹药");
        y += 12;
        graphics.drawString(font, Component.literal(
                        "§7当前小境界需求 §f" + data.needed()
                                + " §8（满了按 §eB§8 破境再吃更划算）"),
                x, y, TEXT, false);
        y += 15;

        // ---- 灵石（直接服用） ----
        long stoneGain = com.hongmeng.xiuxian.item.SpiritStoneItem.cultivationFor(data);
        graphics.drawString(font, Component.literal(
                        "§b灵石 §7×" + countItem("hongmeng:spirit_stone")
                                + "   §8右键服用：§e+" + bigNum(stoneGain)
                                + " §8(需求 " + com.hongmeng.xiuxian.item.SpiritStoneItem
                                .DIRECT_RATIO_TEXT + ")"),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal(
                "§8来源：挖「灵石矿脉」（主世界地下 y -24~56，成脉生成）"), x, y, TEXT, false);
        y += 15;

        // ---- 工作台合成丹 ----
        graphics.drawString(font, Component.literal("§e合成丹药§8（工作台）"), x, y, TEXT, false);
        y += 12;
        graphics.drawString(font, Component.literal(
                        "§f聚灵丹 §7×" + countItem("hongmeng:pill_juling")
                                + "   §8服下：§e+" + bigNum(Math.round(need * 0.15D))
                                + " §8(" + Math.round(need * 0.15D * 100.0D / need) + "%)"
                                + "   §8灵石×4 + 灵叶×2"),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal(
                        "§f凝元丹 §7×" + countItem("hongmeng:pill_ningyuan")
                                + "   §8服下：§e+" + bigNum(Math.round(need * 0.45D))
                                + " §8(45%)   §8灵石×6 + 灵晶×2 + 灵叶×1"),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal(
                "§8合成丹按凡品计（×1）；妙处是「灵石→丹药」效率翻倍"), x, y, TEXT, false);
        y += 15;

        // ---- 丹炉修为丹三档 ----
        graphics.drawString(font, Component.literal("§6丹炉丹药§8（丹炉炼制，带品质）"), x, y, TEXT, false);
        y += 12;
        graphics.drawString(font, Component.literal(
                        "§f普通修为丹 §8+" + bigNum(Math.round(need * 0.25D))
                                + "  §f完美修为丹 §8+" + bigNum(Math.round(need * 0.75D))
                                + "  §f仙品修为丹 §8+" + bigNum(Math.round(need * 2.5D))),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal(
                "§8丹炉产出会掷品质（凡/良/上/极/仙/神 ×1~×20），高阶灵植方更划算"), x, y, TEXT, false);
        y += 15;

        // ---- 提醒 ----
        graphics.drawString(font, Component.literal(
                "§7· 修为在当前小境界封顶：吃丹/灵石超出的部分会逸散"), x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal(
                "§7· 打坐 / 杀怪 / 任务 / 奇遇也涨修为（修行页看累计）"), x, y, TEXT, false);
    }

    /** 本地背包里某个物品的数量（只用于展示，不做任何判定）。 */
    private int countItem(String itemId) {
        try {
            if (this.minecraft == null || this.minecraft.player == null) {
                return 0;
            }
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    new net.minecraft.resources.ResourceLocation(itemId));
            int n = 0;
            var inv = this.minecraft.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                var st = inv.getItem(i);
                if (!st.isEmpty() && st.getItem() == item) {
                    n += st.getCount();
                }
            }
            return n;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 大数美化：12345 → 1.2万。只用于面板显示。 */
    private static String bigNum(long v) {
        if (v >= 100_000_000L) {
            return trim1(v / 100_000_000.0D) + "亿";
        }
        if (v >= 10_000L) {
            return trim1(v / 10_000.0D) + "万";
        }
        return Long.toString(v);
    }

    // ==================================================================
    // 页 3 · 灵根
    // ==================================================================

    private void renderRootPage(GuiGraphics graphics, CultivationData data,
                                int x, int y, int bottom) {
        SpiritRoot root = data.getRoot();
        sectionTitle(graphics, x, y, "灵根");
        y += 14;
        if (root == null) {
            graphics.drawString(font, Component.literal("§8灵根数据还没同步到客户端。"),
                    x, y, TEXT, false);
            return;
        }

        graphics.drawString(font, Component.literal("§f§l" + root.cn()), x, y, TEXT, false);
        y += 15;
        graphics.drawString(font, Component.literal("§7品质：§r" + root.rarity().colored()),
                x, y, TEXT, false);
        // ⚠ 这里原来显示「属性：<灵根元素>」——灵根的元素维度已被移除
        //   （玩家要求：把灵根的属性设定删了，保留领域的属性设定）。
        //   所以改成一个指路提示：元素加成现在看【领域】。
        graphics.drawString(font, Component.literal("§7属性：§8见「道宫」"),
                x + 170, y, TEXT, false);
        y += 12;
        graphics.drawString(font, Component.literal(
                "§7修炼倍率：§e×" + String.format("%.2f", root.multiplier())), x, y, TEXT, false);
        y += 12;

        // ---- 领域加成的**实际数值**（玩家："领域的加成感觉没有实现"）----
        //
        // 之前这里只写「由道宫领域提供」——没有任何数字，玩家无法判断
        // 到底有没有在生效。而伤害加成**只在道宫元素 == 功法元素时**才算，
        // 元素不匹配时倍率就是 ×1.00，看起来当然"没效果"。
        //
        // 所以这里把每一门已学功法的实际倍率都摆出来，包括不匹配的
        //（写"未匹配"），玩家一眼就知道要不要换道宫元素。
        y = renderDomainBonusLines(graphics, data, x, y, bottom);

        sectionTitle(graphics, x, y, "这个灵根意味着什么");
        y += 13;
        for (FormattedCharSequence line : wrap("§8" + root.desc(), PANEL_W - PAD * 2)) {
            graphics.drawString(font, line, x, y, TEXT, false);
            y += 10;
        }
        y += 10;

        sectionTitle(graphics, x, y, "灵根与道宫");
        y += 13;
        for (String hint : new String[]{
                "§7· 灵根决定**修炼倍率**：品质越高，修为涨得越快。",
                "§8· 灵根不再有属性维度；**元素加成由道宫领域提供**（同元素时伤害更高）。",
                "§7· 不满意可以用 §e洗灵丹 §7重抽（保底玄品以上）。",
                "§7· 服务器允许时也可用 §f/hongmeng reroll§7（正式玩法建议关掉）。",
        }) {
            for (FormattedCharSequence line : wrap(hint, PANEL_W - PAD * 2)) {
                if (y > bottom - 9) {
                    return;
                }
                graphics.drawString(font, line, x, y, TEXT, false);
                y += 10;
            }
        }
    }

    // ==================================================================
    // 页 3 · 道宫 / 本命领域
    // ==================================================================

    private void renderDomainPage(GuiGraphics graphics, CultivationData data,
                                  int x, int y, int bottom) {
        sectionTitle(graphics, x, y, "道宫 / 本命领域");
        y += 13;

        // ---- 领域的三条限制（时长 / 冷却 / 耗能量）----
        //
        // 玩家要求「把道宫给的领域限制一下」。这里把限制写在最显眼的地方，
        // 否则玩家只会觉得"领域怎么自己关了"。
        graphics.drawString(font, Component.literal(
                "§8限制：单次 §7" + (com.hongmeng.xiuxian.domain.DomainManager
                        .DOMAIN_DURATION / 20) + "§8秒 · 每秒耗能量 §7"
                        + com.hongmeng.xiuxian.domain.DomainManager.DOMAIN_DRAIN_PER_SECOND
                        + "§8 · 收起后冷却 §7"
                        + (com.hongmeng.xiuxian.domain.DomainManager.DOMAIN_COOLDOWN / 20)
                        + "§8秒"), x, y, TEXT, false);
        y += 12;

        if (!data.realm().hasDomain()) {
            graphics.drawString(font, Component.literal(
                    "§8未至道印 §7— 突破到 §e道印 §7境后才能凝聚道宫"), x, y, TEXT, false);
            y += 14;
            graphics.drawString(font, Component.literal(
                    "§8当前：§7" + data.realm().cn()), x, y, TEXT, false);
            y += 18;
            for (FormattedCharSequence line : wrap(
                    "§8道宫是飞升之后的本命之物：凝聚之后可以开启领域，"
                            + "在范围内按元素持续作用，并给同元素功法加成。",
                    PANEL_W - PAD * 2)) {
                graphics.drawString(font, line, x, y, TEXT, false);
                y += 10;
            }
            return;
        }

        DomainData domains = data.getDomainData();
        graphics.drawString(font, Component.literal(
                        "§f能量 §a" + domains.energy() + "§7/§a" + DomainData.CONDENSE_COST
                                + "   §f道宫 §e" + domains.domainCount() + "§7/§e"
                                + domains.maxDomains()
                                + "   §f领域 " + (domains.isDomainActive() ? "§a开" : "§c关")),
                x, y, TEXT, false);
        y += 18;

        if (domains.domainCount() == 0) {
            graphics.drawString(font, Component.literal(
                    "§e尚未凝聚道宫 §7— 按 §fG §7打开界面凝聚"), x, y, TEXT, false);
            y += 18;
        } else {
            sectionTitle(graphics, x, y, "已凝聚的道宫", PANEL_W - PAD * 2);
            y += 13;
            for (var d : domains.domains()) {
                graphics.drawString(font, Component.literal(
                        "§7· " + d.quality().colored() + " §8元素§b"
                                + d.element().display()
                                + " §8伤害§c" + d.quality().damageText()
                                + " §8释放§e" + d.quality().castText()),
                        x, y, TEXT, false);
                y += 11;
            }
            y += 8;
        }

        // ---- 保底进度 ----
        String pity;
        if (domains.totalCondensed() >= DomainData.PITY_HONGMENG) {
            pity = "§6已进入鸿蒙保底";
        } else if (domains.totalCondensed() >= DomainData.PITY_IMMORTAL) {
            pity = "§d仙品保底中，距鸿蒙还差 " + domains.energyIntoPityHongmeng() + " 次";
        } else {
            pity = "§8距仙品保底还差 " + domains.energyIntoPityImmortal() + " 次";
        }
        sectionTitle(graphics, x, y, "保底", PANEL_W - PAD * 2);
        y += 13;
        graphics.drawString(font, Component.literal(pity), x, y, TEXT, false);
        y += 14;

        // ---- 领域效果一览（数字全部来自纯数据表 DomainEffectTable）----
        sectionTitle(graphics, x, y, "领域效果（按道宫元素）", PANEL_W - PAD * 2);
        y += 13;
        graphics.drawString(font, Component.literal(
                        "§7范围 §f" + com.hongmeng.xiuxian.domain.DomainEffectTable.rangeText(
                                com.hongmeng.xiuxian.compat.HongmengConfig.SERVER
                                        .domainChunks.get())
                                + " §8（配置项 domainChunks）"),
                x, y, TEXT, false);
        y += 11;
        boolean anyEffect = false;
        for (var d : domains.domains()) {
            if (!com.hongmeng.xiuxian.domain.DomainEffectTable.hasEffect(d.element())) {
                continue;
            }
            anyEffect = true;
            graphics.drawString(font, Component.literal(
                            "§8· §b" + d.element().cn() + " §8"
                                    + com.hongmeng.xiuxian.domain.DomainEffectTable
                                    .shortSummary(d.element())),
                    x, y, TEXT, false);
            y += 10;
        }
        if (!anyEffect) {
            graphics.drawString(font, Component.literal(
                    "§8（无属性道宫没有领域效果）"), x, y, TEXT, false);
            y += 10;
        }
        y += 4;

        // ---- 友伤开关（规格 §2.1：默认关）----
        boolean friendlyFire = data.isDomainFriendlyFire();
        ffButtonW = 140;
        ffButtonH = 15;
        ffButtonX = x;
        ffButtonY = y;
        ffButtonVisible = true;
        graphics.fill(ffButtonX, ffButtonY, ffButtonX + ffButtonW, ffButtonY + ffButtonH,
                friendlyFire ? 0xFF4A2430 : 0xFF24382A);
        graphics.renderOutline(ffButtonX, ffButtonY, ffButtonW, ffButtonH, BORDER);
        graphics.drawString(font, Component.literal(
                        "§f领域友伤：" + (friendlyFire ? "§c开" : "§a关") + " §8(点击切换)"),
                ffButtonX + 4, ffButtonY + 4, TEXT, false);
        y += ffButtonH + 3;
        graphics.drawString(font, Component.literal(friendlyFire
                        ? "§8友伤开：玩家 / 宠物 / 坐骑 / NPC 也会被打"
                        : "§8友伤关：只打敌对生物"),
                x, y, TEXT, false);
        y += 14;

        sectionTitle(graphics, x, y, "怎么用", PANEL_W - PAD * 2);
        y += 13;
        for (String hint : new String[]{
                "§7· 杀怪攒**能量**，攒满一次可以凝聚一座道宫。",
                "§7· §fG §7打开道宫界面：凝聚 / 破碎 / 领域开关都在那里。",
                "§7· 领域开着时按元素持续作用，并给**同元素功法**加成。",
                "§7· 领域范围与效果强度可在整合包配置里调。",
        }) {
            for (FormattedCharSequence line : wrap(hint, PANEL_W - PAD * 2)) {
                if (y > bottom - 9) {
                    return;
                }
                graphics.drawString(font, line, x, y, TEXT, false);
                y += 10;
            }
        }
    }

    // ==================================================================
    // 页 4 · 功法 / 法术
    // ==================================================================

    /**
     * 功法页：左边一列已修习列表（点击选中），右边一列槽位与详情。
     *
     * <p>"点一行就选中它"与原来右侧功法栏的行为完全一致
     * （发 {@code TechniqueActionPacket.select}）。</p>
     */
    private void renderTechniquePage(GuiGraphics graphics, CultivationData data,
                                     int x, int y, int bottom,
                                     int mouseX, int mouseY) {
        TechniqueData tech = data.getTechniques();
        List<Technique> known = tech.knownList();
        int colW = 152;

        graphics.drawString(font, Component.literal(
                        "§b§l已修习 §8" + known.size() + " / " + Technique.values().length),
                x, y, TEXT, false);
        graphics.fill(x, y + 10, x + colW, y + 11, 0x558A6FD8);
        int listTop = y + 15;

        if (known.isEmpty()) {
            techHitY = new int[0];
            techListVisible = false;
            graphics.drawString(font, Component.literal("§7尚无可施展的功法。"), x, listTop,
                    TEXT, false);
            graphics.drawString(font, Component.literal("§8右键功法秘籍修习，"),
                    x, listTop + 13, TEXT, false);
            graphics.drawString(font, Component.literal("§8或去坊市买（按 K 打开坊市）。"),
                    x, listTop + 24, TEXT, false);
            return;
        }

        techListX = x;
        techListW = colW;
        techListVisible = true;
        techHitY = new int[known.size()];

        long now = 0L;
        if (minecraft != null && minecraft.level != null) {
            now = minecraft.level.getGameTime();
        }
        Technique selected = tech.selectedTechnique();
        int rowH = 22;
        int maxRows = Math.max(1, (bottom - listTop - 10) / rowH);

        for (int i = 0; i < known.size() && i < maxRows; i++) {
            Technique t = known.get(i);
            int rowY = listTop + i * rowH;
            techHitY[i] = rowY;
            boolean isSel = t == selected;
            boolean hover = mouseX >= x && mouseX <= x + colW
                    && mouseY >= rowY && mouseY <= rowY + rowH - 2;
            if (isSel) {
                graphics.fill(x, rowY, x + colW, rowY + rowH - 2, ROW_SEL);
            } else if (hover) {
                graphics.fill(x, rowY, x + colW, rowY + rowH - 2, ROW_HOVER);
            }
            long cd = tech.remainingSeconds(t, now);
            String head = (isSel ? "§e▶ " : "§8  ") + t.colored()
                    + " §8[" + t.element().cn() + t.kind().glyph() + "]";
            graphics.drawString(font, Component.literal(head), x + 3, rowY + 2, TEXT, false);
            String state = cd > 0 ? "§c" + cd + "s" : "§a就绪";
            graphics.drawString(font, Component.literal(state),
                    x + colW - 4 - font.width(state), rowY + 2, TEXT, false);
            String trait = font.plainSubstrByWidth("§7" + t.trait(), colW - 10);
            graphics.drawString(font, Component.literal(trait), x + 8, rowY + 12, TEXT, false);
        }

        if (known.size() > maxRows) {
            graphics.drawString(font, Component.literal(
                            "§8…还有 " + (known.size() - maxRows) + " 门（按 O 看完整法术栏）"),
                    x, listTop + maxRows * rowH, TEXT, false);
        }

        // ---- 右列：槽位 + 选中详情 ----
        int rx = x + colW + 12;
        int rw = PANEL_W - PAD * 2 - colW - 12;
        int ry = y;

        graphics.drawString(font, Component.literal("§b§l法术槽"), rx, ry, TEXT, false);
        graphics.fill(rx, ry + 10, rx + rw, ry + 11, 0x558A6FD8);
        ry += 15;

        graphics.drawString(font, Component.literal("§8主动"), rx, ry, TEXT, false);
        ry += 10;
        String[] keys = {"Z", "X", "C", "V"};
        for (int i = 0; i < TechniqueData.ACTIVE_SLOTS; i++) {
            Technique t = tech.activeAt(i);
            String key = i < keys.length ? keys[i] : "?";
            graphics.drawString(font, Component.literal(
                            "§7[" + key + "] " + (t == null ? "§8空" : "§f" + t.cn())),
                    rx, ry, TEXT, false);
            ry += 10;
        }
        ry += 5;

        graphics.drawString(font, Component.literal("§8被动（装上即生效）"), rx, ry, TEXT, false);
        ry += 10;
        for (int i = 0; i < TechniqueData.PASSIVE_SLOTS; i++) {
            Technique t = tech.passiveAt(i);
            graphics.drawString(font, Component.literal(
                            "§8· " + (t == null ? "§8空" : "§a" + t.cn())),
                    rx, ry, TEXT, false);
            ry += 10;
        }
        ry += 9;

        if (selected != null) {
            graphics.drawString(font, Component.literal("§b§l选中"), rx, ry, TEXT, false);
            graphics.fill(rx, ry + 10, rx + rw, ry + 11, 0x558A6FD8);
            ry += 14;
            graphics.drawString(font, Component.literal("§f§l" + selected.cn()), rx, ry,
                    TEXT, false);
            ry += 12;
            for (FormattedCharSequence line : wrap(
                    "§7" + selected.element().cn() + " / " + selected.kind().archetype()
                            + selected.kind().glyph() + " · 门槛第 " + selected.minTier()
                            + " 境", rw)) {
                graphics.drawString(font, line, rx, ry, TEXT, false);
                ry += 10;
            }
            graphics.drawString(font, Component.literal(
                            "§7灵力 §b" + selected.manaCost() + " §8· §7冷却 §c"
                                    + trim1(selected.cooldownSeconds()) + "s"),
                    rx, ry, TEXT, false);
            ry += 10;
            if (selected.castTicks() > 0) {
                graphics.drawString(font, Component.literal(
                                "§7蓄力 §e" + selected.castLabel()), rx, ry, TEXT, false);
                ry += 10;
            }
            ry += 4;
            for (FormattedCharSequence line : wrap("§8" + selected.trait(), rw)) {
                if (ry > bottom - 10) {
                    break;
                }
                graphics.drawString(font, line, rx, ry, TEXT, false);
                ry += 10;
            }
        } else {
            graphics.drawString(font, Component.literal("§8点左边一行选中一门功法。"),
                    rx, ry, TEXT, false);
        }

        graphics.drawString(font, Component.literal(
                        "§8点行选中 · 施法键 Z X C V · 按 O 装备槽位"),
                x, bottom, TEXT, false);
    }

    // ==================================================================
    // 页 5 · 灵兽
    // ==================================================================

    /**
     * 灵兽图鉴页。
     *
     * <p>这一页<b>全部读静态数据</b>（{@code BeastKind} 枚举），
     * 一个字都不依赖网络包——所以背包里没有灵兽的时候它照样是对的，
     * 也就成了新手"该收哪只"的说明书。当前骑着的那只额外标一个 ▶。</p>
     */
    private void renderBeastPage(GuiGraphics graphics, CultivationData data,
                                 int x, int y, int bottom) {
        var kinds = com.hongmeng.xiuxian.entity.beast.BeastKind.values();
        boolean riding = com.hongmeng.xiuxian.entity.beast.BeastRiding.ENABLED;

        sectionTitle(graphics, x, y, "灵兽图鉴", PANEL_W - PAD * 2);
        y += 14;
        graphics.drawString(font, Component.literal(riding
                ? "§7每只灵兽占一个生态位——§f不是同一只坐骑换个颜色§7。"
                : "§7每只灵兽占一个生态位——§f图鉴与契约仍可查看§7。"), x, y, TEXT, false);
        y += 13;

        // 表头：骑乘关闭时只保留“灵兽 / 职能 / 契约”，不显示飞行、速度、载重、灵力
        String[] head = riding
                ? new String[]{"灵兽", "职能", "飞行", "速度", "载重", "灵力/tick", "契约"}
                : new String[]{"灵兽", "职能", "契约"};
        int[] col = riding
                ? new int[]{0, 40, 88, 108, 134, 160, 220}
                : new int[]{0, 40, 112};
        for (int i = 0; i < head.length; i++) {
            graphics.drawString(font, Component.literal("§8" + head[i]), x + col[i], y,
                    TEXT, false);
        }
        y += 11;
        graphics.fill(x, y, x + PANEL_W - PAD * 2, y + 1, 0x558A6FD8);
        y += 4;

        // 当前骑着的那只（仅骑乘开放时标 ▶）
        com.hongmeng.xiuxian.entity.beast.BeastKind ridingKind = null;
        if (riding && minecraft != null && minecraft.player != null
                && minecraft.player.getVehicle()
                instanceof com.hongmeng.xiuxian.entity.beast.SpiritBeastEntity beast) {
            ridingKind = beast.kind();
        }

        int ownerTier = data.realm().tier();
        for (var kind : kinds) {
            boolean active = kind == ridingKind;
            graphics.drawString(font, Component.literal(
                            (active ? "§a▶ " : "§8· ") + "§f" + kind.cn()),
                    x + col[0], y, TEXT, false);
            graphics.drawString(font, Component.literal("§b" + kind.role().cn()),
                    x + col[1], y, TEXT, false);
            if (riding) {
                graphics.drawString(font, Component.literal(
                                kind.flying() ? "§a能" : "§8不能"),
                        x + col[2], y, TEXT, false);
                graphics.drawString(font, Component.literal("§e×" + trim1(kind.rideSpeed())),
                        x + col[3], y, TEXT, false);
                graphics.drawString(font, Component.literal(kind.canCarryCargo()
                                ? "§7" + kind.cargoSlots() + " 格" : "§8无"),
                        x + col[4], y, TEXT, false);
                graphics.drawString(font, Component.literal(kind.flying()
                                ? "§d" + trim1(kind.manaDrain()) : "§a0"),
                        x + col[5], y, TEXT, false);
            }
            graphics.drawString(font, Component.literal(
                            contractText(kind, ownerTier)),
                    x + col[col.length - 1], y, TEXT, false);
            y += 11;

            // 每只下面附一句"它的长处"，图鉴的价值就在这里
            for (FormattedCharSequence line : wrap("§8  " + roleHint(kind, riding),
                    PANEL_W - PAD * 2 - 16)) {
                graphics.drawString(font, line, x, y, TEXT, false);
                y += 9;
            }
            y += 3;
        }

        y += 4;
        sectionTitle(graphics, x, y, "怎么收服", PANEL_W - PAD * 2);
        y += 13;
        String[] hints = riding
                ? new String[]{
                "§7· 把妖兽打到 §c25% 生命以下§7，手持 §e契灵符§7 右键它。",
                "§7· 成功率看境界差与灵根品质；§c失败会烧掉符纸并激怒它§7。",
                "§7· 契约后进入 §8幼年§7，喂 §e妖丹 §7可成长到 §b成年§7 → §6化形§7。",
                "§7· §f灵兽袋§7：右键灵兽收起、右键空地放出、潜行右键看名册。",
                "§7· 骑乘中 §f潜行右键§7 循环 跟随 → 停留 → 游荡。",
                "§7· 飞行坐骑用 §f空格上升§7 / §fShift 下降§7，并消耗§d你的灵力§7。",
        }
                : new String[]{
                "§7· 把妖兽打到 §c25% 生命以下§7，手持 §e契灵符§7 右键它。",
                "§7· 成功率看境界差与灵根品质；§c失败会烧掉符纸并激怒它§7。",
                "§7· 契约后进入 §8幼年§7，喂 §e妖丹 §7可成长到 §b成年§7 → §6化形§7。",
                "§7· §f灵兽袋§7：右键灵兽收起、右键空地放出、潜行右键看名册。",
        };
        for (String hint : hints) {
            for (FormattedCharSequence wrapped : wrap(hint, PANEL_W - PAD * 2)) {
                if (y > bottom - 9) {
                    return;
                }
                graphics.drawString(font, wrapped, x, y, TEXT, false);
                y += 10;
            }
        }
    }

    /** 契约条件那一列的文字。 */
    private static String contractText(com.hongmeng.xiuxian.entity.beast.BeastKind kind,
                                       int ownerTier) {
        if (kind.contractRealmGap() <= 0) {
            return "§a温顺可驯";
        }
        int need = requiredTierFor(kind);
        if (ownerTier >= need) {
            return "§a已达标";
        }
        return "§c需第 " + need + " 境";
    }

    /**
     * 契约这只灵兽所需的最低大境序号。
     *
     * <p>口径与 {@code BeastManager.targetTier} 完全一致：来源妖兽的大境
     * = {@code 2 + 品阶序号}，再加上灵兽自身的门槛。雷鹰来自队长（第 4 境）
     * + 门槛 1 = 第 5 境；蛟龙来自头目（第 5 境）+ 门槛 3 = 第 8 境。</p>
     */
    private static int requiredTierFor(com.hongmeng.xiuxian.entity.beast.BeastKind kind) {
        int sourceTier = switch (kind) {
            case CONTRACT_EAGLE -> 2 + 2;   // 队长（thunder_eagle）
            case JIAOLONG -> 2 + 3;         // 头目（void_serpent）
            case CONTRACT_WOLF -> 2 + 0;    // 普通（spirit_wolf）
            default -> 2;
        };
        return sourceTier + kind.contractRealmGap();
    }

    /** 每只灵兽的"它凭什么值得带"。骑乘关闭时不提坐骑/骑乘。 */
    private static String roleHint(com.hongmeng.xiuxian.entity.beast.BeastKind kind,
                                   boolean riding) {
        if (!riding) {
            return switch (kind) {
                case CONTRACT_EAGLE -> "空中侦察：能飞、能悬停，但只有 24 血、载重为 0。";
                case JIAOLONG -> "空战：飞得快、血最厚、能喷吐毒息，机能最全面，契约门槛最高。";
                case CONTRACT_WOLF -> "战斗协助：主动替你咬敌对生物，攻击力随你的境界一起涨。";
            };
        }
        return switch (kind) {
            case CONTRACT_EAGLE -> "唯一能悬停的坐骑（悬停灵力减半），但只有 24 血、载重为 0。";
            case JIAOLONG -> "飞得快、血最厚、能喷吐毒息、能驮 6 格货，契约门槛最高。";
            case CONTRACT_WOLF -> "不能骑；主动替你咬敌对生物，攻击力随你的境界一起涨。";
        };
    }

    // ==================================================================
    // 页 6 · 属性 / 能力
    // ==================================================================

        /**
     * 画出<b>领域加成的实际数值</b>（释放速度 + 每门已学功法的伤害倍率）。
     *
     * <p>为什么需要：玩家反馈「领域的加成感觉没有实现啊，只是写出来了」。
     * 核实后确实有两处问题：</p>
     * <ol>
     *   <li><b>释放速度那条从来没生效过</b>（只在界面文字里）——
     *       已接进蓄力时长（{@code TechniqueCaster} 里算 effectiveTicks）；</li>
     *   <li>伤害加成<b>只在道宫元素 == 功法元素</b>时才算，
     *       但界面上只写了一句"由道宫领域提供"，没有任何数字，
     *       玩家无法分辨"没加成"和"加成很小"。</li>
     * </ol>
     *
     * <p>所以这里把每门功法的实际倍率都写出来，不匹配的明确标「未匹配」。</p>
     */
    private int renderDomainBonusLines(GuiGraphics graphics,
                                       com.hongmeng.xiuxian.core.CultivationData data,
                                       int x, int y, int bottom) {
        double speed = 0.0D;
        boolean active = false;
        try {
            speed = data.getDomainData().castSpeedBonus();
            active = data.getDomainData().isDomainActive();
        } catch (Throwable ignored) {
            // 没有道宫：两项都按 0 显示
        }
        graphics.drawString(font, Component.literal(
                        "\u00a77释放速度：\u00a7e+" + Math.round(speed * 100) + "%"
                                + "\u00a77（蓄力 ÷" + String.format("%.2f", 1.0D + speed)
                                + "）\u00a78  领域："
                                + (active ? "\u00a7a开" : "\u00a7c关")),
                x, y, TEXT, false);
        y += 11;
        if (y > bottom - 9) {
            return y;
        }
        if (data.getTechniques() == null || data.getTechniques().isEmpty()) {
            graphics.drawString(font, Component.literal(
                    "\u00a78还没有学会功法，领域加成一览暂空"), x, y, TEXT, false);
            return y + 11;
        }
        for (var t : data.getTechniques().knownList()) {
            if (y > bottom - 9) {
                break;
            }
            if (t.isSupport()) {
                continue;
            }
            double bonus = 0.0D;
            try {
                bonus = data.getDomainData().techniqueBonus(t.element());
            } catch (Throwable ignored) {
                // 没有道宫
            }
            String mark = bonus > 0.0D
                    ? "\u00a7a×" + String.format("%.2f", 1.0D + bonus)
                            + "\u00a78（道宫同" + t.element().cn() + "）"
                    : "\u00a78未匹配（道宫非" + t.element().cn() + "）";
            graphics.drawString(font, Component.literal(
                            "\u00a77· " + t.colored() + " " + mark),
                    x + 6, y, TEXT, false);
            y += 10;
        }
        return y + 4;
    }

    /**
     * <b>道途区块</b>：主修 / 天赋点 / 机制 / 护甲 / 武器 / 副业 / 天赋树。
     *
     * <p>玩家要求「可以做一个天赋点系统」+「每种分类都要有独立的装备」。
     * 这些内容有个共同点：<b>数值差异看不出来</b> ——
     * 五套护甲的基础三项刻意完全相同、五把武器的 DPS 也完全相同，
     * 差别只在"侧重"上。所以这一块必须把"我的道途给了我什么"写清楚。</p>
     *
     * <p>天赋树做成<b>可点击列表</b>（不是画一棵树）：窄面板放不下树形布局，
     * 而 6~7 个节点画成树会比列表更难扫读。点击走 {@code TalentPacket}
     * 交给服务端校验 —— 客户端不自己改数据，避免"显示能点、服务端拒绝"的错位。</p>
     *
     * @return 画完之后的 y
     */
    private int renderPathSection(GuiGraphics graphics, int x, int y) {
        String pathName = com.hongmeng.xiuxian.network.ClientCultivationCache
                .summaryPart(0);
        if (pathName.isEmpty()) {
            // 每帧重置：没画到的槽位必须是 -1（默认 0 会被当成"法修"）
            for (int r = 0; r < pathHitOrd.length; r++) {
                pathHitOrd[r] = -1;
            }
            pathChooseVisible = true;
            sectionTitle(graphics, x, y, "职业 · 择业");
            y += 13;
            graphics.drawString(font, Component.literal(
                    "\u00a77选择一个职业（\u00a7f点一下\u00a77即可，不可随意更改）"),
                    x, y, TEXT, false);
            y += 12;
            // 五个可点条目 —— 之前这里只写"用命令选"，玩家打开面板无从下手。
            int i = 0;
            for (var p : com.hongmeng.xiuxian.talent.Career.values()) {
                boolean hover = frameMouseX >= x
                        && frameMouseX <= x + PANEL_W - PAD * 2
                        && frameMouseY >= y && frameMouseY <= y + 11;
                if (hover) {
                    graphics.fill(x, y - 1, x + PANEL_W - PAD * 2, y + 11, 0x402A1840);
                }
                graphics.drawString(font, Component.literal(
                                (hover ? "\u00a7f> " : "\u00a78· ")
                                        + p.colored().getString() + " \u00a77"
                                        + p.bonus()),
                        x + 2, y, TEXT, false);
                pathHitY[i] = y;
                pathHitOrd[i] = p.ordinal();
                i++;
                y += 12;
                if (i >= pathHitY.length) {
                    break;
                }
            }
            pathHitX = x;
            pathHitW = PANEL_W - PAD * 2;
            y += 6;
            return y;
        }
        pathChooseVisible = false;
        sectionTitle(graphics, x, y, "职业 · " + pathName);
        y += 13;

        graphics.drawString(font, Component.literal("\u00a77天赋点 \u00a7e"
                + com.hongmeng.xiuxian.network.ClientCultivationCache.summaryPart(1)
                + "\u00a77/\u00a7f"
                + com.hongmeng.xiuxian.network.ClientCultivationCache.summaryPart(2)),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal("\u00a7d专属加成 \u00a77"
                + com.hongmeng.xiuxian.network.ClientCultivationCache.summaryPart(3)),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal("\u00a77职业甲 \u00a78"
                + com.hongmeng.xiuxian.network.ClientCultivationCache.summaryPart(4)),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal("\u00a77武器 \u00a78"
                + com.hongmeng.xiuxian.network.ClientCultivationCache.summaryPart(5)),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal("\u00a77副业 \u00a7f"
                + com.hongmeng.xiuxian.network.ClientCultivationCache.summaryPart(6)),
                x, y, TEXT, false);
        y += 13;

        // ---- 天赋树（可点击加点）----
        sectionTitle(graphics, x, y, "天赋（点击加点）");
        y += 12;
        talentVisible = false;
        talentHitX = x;
        talentHitW = PANEL_W - PAD * 2;
        int ti = 0;
        // 有可用点数时行首显示一个 +，给玩家"这里能点"的提示
        String freeText = com.hongmeng.xiuxian.network.ClientCultivationCache
                .summaryPart(1);
        boolean canAdd = !freeText.isEmpty() && !"0".equals(freeText);
        var path = com.hongmeng.xiuxian.talent.Talents.pathOf(minecraft.player);
        if (path != null) {
            for (var node : com.hongmeng.xiuxian.talent.TalentTree.forCareer(path)) {
                if (ti >= talentHitY.length) {
                    break;
                }
                graphics.drawString(font, Component.literal(
                        (canAdd ? "\u00a7e+ " : "\u00a78· ")
                                + "\u00a77" + node.cn() + " \u00a78"
                                + node.desc()), x, y, TEXT, false);
                talentHitY[ti] = y;
                talentHitId[ti] = node.id();
                ti++;
                y += 10;
            }
        }
        talentVisible = ti > 0;
        y += 4;
        return y;
    }


    private void renderAttributePage(GuiGraphics graphics, CultivationData data,
                                     int x, int y, int bottom) {
        Realm realm = data.realm();

        // ---- 道途（主修 / 天赋 / 机制 / 护甲 / 武器）----
        //
        // 放在属性页最上面：这些是"我的角色现在是什么样"的核心。
        // 而且数值差异（五套护甲基础完全相同、五把武器 DPS 完全相同）
        // 玩家是**看不出来**的，所以必须靠文字说清"我的道途给了我什么"。
        y = renderPathSection(graphics, x, y);

        sectionTitle(graphics, x, y, "能力");
        y += 13;
        graphics.drawString(font, Component.literal("§7常驻速度 I："
                        + (realm.hasSpeedOne() ? "§a已获得" : "§8未获得")
                        + "   §7常驻速度 II："
                        + (realm.hasSpeedTwo() ? "§a已获得" : "§8未获得")),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal("§7飞行："
                        + (realm.hasFlight() ? "§a已获得"
                        : (realm.hasAscensionPerks() ? "§6御剑" : "§8未获得"))
                        + "   §7本命领域："
                        + (realm.hasDomain() ? "§a已获得" : "§8未获得")),
                x, y, TEXT, false);
        y += 16;

        // 帝境：速度 II 开关按钮（原来在总览页，按主题挪到属性页）
        if (realm.hasSpeedTwo()) {
            boolean on = data.isSpeedBoostEnabled();
            int btnW = 130;
            int btnH = 16;
            graphics.fill(x, y, x + btnW, y + btnH, on ? 0xFF2E5A3A : 0xFF3A2E2E);
            graphics.renderOutline(x, y, btnW, btnH, BORDER);
            graphics.drawString(font, Component.literal(
                            "§f常驻速度 II：" + (on ? "§a开" : "§c关") + " §8(点击切换)"),
                    x + 4, y + 4, TEXT, false);
            speedButtonX = x;
            speedButtonY = y;
            speedButtonW = btnW;
            speedButtonH = btnH;
            speedButtonVisible = true;
            y += btnH + 9;
        }
        y += 2;

        // ---- 业力 ----
        sectionTitle(graphics, x, y, "业力");
        y += 13;
        int karma = data.getKarma();
        graphics.drawString(font, Component.literal(
                "§f层数：§c" + karma + " §7/ 10"), x, y, TEXT, false);
        graphics.drawString(font, Component.literal(
                "§f突破失败率：§c" + Karma.failurePercentText(data)), x + 140, y, TEXT, false);
        y += 12;
        if (karma > 0) {
            graphics.drawString(font, Component.literal(
                    "§8附魔金苹果全清 · 劫掠事件 -1 层"), x, y, TEXT, false);
        } else {
            graphics.drawString(font, Component.literal("§8道基无垢。"), x, y, TEXT, false);
        }
        y += 20;

        // ---- 属性上限自检 ----
        // 玩家反馈过"血条卡在 1024"，而那是原版 RangedAttribute 的硬上限。
        // 这里把**客户端自己那份**的上限显示出来，出问题时一眼能定位。
        //
        // 注意用 AttributeCapDiagnostics 的**反射字段读数**而不是 getMaxValue()：
        // 两者不一致就说明有 JIT 内联之类的问题，只信 getter 会得出错误结论。
        sectionTitle(graphics, x, y, "属性上限自检");
        y += 13;
        var maxHealthAttr = net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH;
        double viaGetter = com.hongmeng.xiuxian.compat.AttributeCaps.maxOf(maxHealthAttr);
        double viaField = com.hongmeng.xiuxian.compat.AttributeCapDiagnostics
                .fieldMaxValue(maxHealthAttr);
        double myMaxHp = minecraft != null && minecraft.player != null
                ? minecraft.player.getMaxHealth() : 0.0D;
        double myBaseHp = minecraft != null && minecraft.player != null
                ? minecraft.player.getAttribute(
                net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .getBaseValue() : 0.0D;

        boolean capOk = viaField >= com.hongmeng.xiuxian.compat.AttributeCaps.NEW_MAX;
        graphics.drawString(font, Component.literal(
                        "§7客户端属性上限：§" + (capOk ? "a" : "c")
                                + (Double.isNaN(viaField) ? "读不到"
                                : String.valueOf((long) viaField))
                                + " §8(getter " + (long) viaGetter + ")"),
                x, y, TEXT, false);
        y += 11;
        graphics.drawString(font, Component.literal(
                        "§7本机生命：§b基础 " + (long) myBaseHp
                                + " §7→ §b最终 " + (long) myMaxHp),
                x, y, TEXT, false);
        y += 13;
        if (!capOk) {
            graphics.drawString(font, Component.literal(
                            "§c**客户端上限未解除！这就是血条卡 1024 的原因**"),
                    x, y, TEXT, false);
            y += 13;
        }
        y += 4;
        for (FormattedCharSequence line : wrap(
                "§8属性上限已经全部解除（见 /hongmeng attrcap）。"
                        + "数值不对请把这一页截图发给作者。", PANEL_W - PAD * 2)) {
            if (y > bottom - 9) {
                return;
            }
            graphics.drawString(font, line, x, y, TEXT, false);
            y += 10;
        }
    }

    // ==================================================================
    // 页 7 · 图鉴（按键与玩法索引）
    // ==================================================================

    private void renderCodexPage(GuiGraphics graphics, CultivationData data,
                                 int x, int y, int bottom) {
        sectionTitle(graphics, x, y, "按键", PANEL_W - PAD * 2);
        y += 13;
        for (String[] row : new String[][]{
                {"R", "修炼面板（就是这一页）"},
                {"B", "尝试突破境界"},
                {"J", "修行录（任务 + 坊市）"},
                {"K", "坊市"},
                {"G", "道宫界面（凝聚 / 破碎 / 领域）"},
                {"O", "法术栏（装备 Z X C V 四个槽）"},
                {"U", "灵兽名册"},
                {"H", "御剑飞行 / 切换飞剑显示"},
                {"Z X C V", "释放主动法术 1~4"},
                {"N", "剑阵展开 / 收回"},
                {"M", "切换剑阵阵型（环 / 扇 / 天）"},
                {"L", "锁定准心方向的目标"},
                {"T", "施展当前功法（旧流程）"},
                {"Y", "切换当前功法"},
        }) {
            graphics.drawString(font, Component.literal("§e" + row[0]), x, y, TEXT, false);
            graphics.drawString(font, Component.literal("§7" + row[1]),
                    x + 64, y, TEXT, false);
            y += 11;
        }
        y += 8;

        sectionTitle(graphics, x, y, "玩法索引", PANEL_W - PAD * 2);
        y += 13;
        for (String hint : new String[]{
                "§7· §f修炼§7：打坐（潜行右键蒲团）/ 杀怪 / 任务都涨修为，满了按 §eB§7 破境。",
                "§7· §f坊市§7（K）：挖到的矿物、灵植、丹药都能卖；回收页可「卖 N / 全部 / 保留 N」。",
                "§7· §f炼丹§7：右键丹炉，主料 + 辅料 + 燃料，10 秒出锅（六阶丹方会挨雷）。",
                "§7· §f副本§7：/hongmeng dungeon 看门槛与当前怪强度。",
                "§7· §f道宫§7：道印境之后杀怪攒能量，按 G 凝聚。",
                "§7· §f灵兽§7：妖兽打到 25% 血以下，手持契灵符右键。",
        }) {
            for (FormattedCharSequence line : wrap(hint, PANEL_W - PAD * 2)) {
                if (y > bottom - 9) {
                    return;
                }
                graphics.drawString(font, line, x, y, TEXT, false);
                y += 10;
            }
        }
        y += 8;

        sectionTitle(graphics, x, y, "常用命令", PANEL_W - PAD * 2);
        y += 13;
        for (String cmd : new String[]{
                "§7· §f/hongmeng debug §8— 属性 + 准星方块挖掘判定",
                "§7· §f/hongmeng attrcap §8— 属性上限诊断（5 行全 OK 才算正常）",
                "§7· §f/hongmeng status §8— 当前境界与修为",
                "§7· §f/hongmeng learnall §8— 学会全部功法（测试用）",
                "§7· §f/hongmeng dungeon §8— 副本一览",
        }) {
            if (y > bottom - 9) {
                return;
            }
            graphics.drawString(font, Component.literal(cmd), x, y, TEXT, false);
            y += 11;
        }
    }

    // ==================================================================
    // 通用
    // ==================================================================

    private void sectionTitle(GuiGraphics graphics, int x, int y, String title) {
        sectionTitle(graphics, x, y, title, PANEL_W - PAD * 2);
    }

    private void sectionTitle(GuiGraphics graphics, int x, int y, String title, int width) {
        graphics.drawString(font, Component.literal("§b§l" + title), x, y, TEXT, false);
        graphics.fill(x, y + 9, x + width, y + 10, 0x558A6FD8);
    }

    private static String trim1(double v) {
        if (Math.abs(v - Math.round(v)) < 0.001D) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private List<FormattedCharSequence> wrap(String text, int maxWidth) {
        return font.split(Component.literal(text), maxWidth);
    }

    /** 下一层会不会跨大境（跨大境要渡劫、且不看业力）。 */
    private static boolean isMajorNext(com.hongmeng.xiuxian.core.CultivationData data) {
        try {
            return com.hongmeng.xiuxian.core.Breakthrough.isMajorBreakthrough(data);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 下一层所在的大境序号。 */
    private static int nextTierOf(com.hongmeng.xiuxian.core.CultivationData data) {
        int step = Math.min(com.hongmeng.xiuxian.core.Realm.TOTAL_STEPS - 1,
                data.unlockedStep() + 1);
        return com.hongmeng.xiuxian.core.Realm.ofStep(step).tier();
    }

    // ==================================================================
    // 常驻功法栏（画在主面板右侧的一列）
    // ==================================================================

    /**
     * 画右侧的功法栏。
     *
     * <p>客户端缓存的 {@code TechniqueData} 里带冷却结束刻，所以这里能用
     * {@code ClientCultivationCache} 的本地时刻算出剩余秒数——不需要额外的网络包。</p>
     *
     * <p>"功法"页自带宽列，所以那一页不画这一栏（避免同一份列表出现两次）。</p>
     */
    private void renderTechniqueColumn(GuiGraphics graphics, int x, int top,
                                       int mouseX, int mouseY) {
        var tech = ClientCultivationCache.get().getTechniques();
        var known = tech.knownList();

        // 高度按条目数算，但至少跟主面板一样高
        int h = Math.max(PANEL_H, 60 + known.size() * 20);
        graphics.fill(x - 1, top - 1, x + TECH_W + 1, top + h + 1, BORDER);
        graphics.fill(x, top, x + TECH_W, top + h, BG);

        int tx = x + 8;
        int ty = top + 10;
        graphics.drawString(font, Component.literal("§6§l功法"), tx, ty, TEXT, false);
        graphics.drawString(font, Component.literal("§8" + known.size() + " / "
                + com.hongmeng.xiuxian.technique.Technique.values().length),
                tx + TECH_W - 46, ty + 2, TEXT, false);
        ty += 14;

        if (known.isEmpty()) {
            columnHitY = new int[0];
            columnVisible = false;
            graphics.drawString(font, Component.literal("§7尚无可施展的功法。"), tx, ty, TEXT, false);
            ty += 11;
            graphics.drawString(font, Component.literal("§8右键功法秘籍修习，"), tx, ty, TEXT, false);
            ty += 10;
            graphics.drawString(font, Component.literal("§8或去坊市买（按 K 打开坊市）。"),
                    tx, ty, TEXT, false);
            return;
        }

        columnX = x;
        columnW = TECH_W;
        columnVisible = true;
        columnHitY = new int[known.size()];

        long now = 0L;
        if (minecraft != null && minecraft.level != null) {
            now = minecraft.level.getGameTime();
        }
        var selected = tech.selectedTechnique();

        // "功法"页里已经在左边列过一遍了，这一栏就不重复
        if (page == PAGE_TECHNIQUE) {
            return;
        }

        for (int i = 0; i < known.size(); i++) {
            var t = known.get(i);
            int rowY = ty + i * 20;
            columnHitY[i] = rowY;

            boolean isSel = t == selected;
            boolean hover = mouseX >= x && mouseX <= x + TECH_W
                    && mouseY >= rowY && mouseY <= rowY + 18;
            if (isSel) {
                graphics.fill(x + 2, rowY, x + TECH_W - 2, rowY + 18, 0x40FFD24A);
            } else if (hover) {
                graphics.fill(x + 2, rowY, x + TECH_W - 2, rowY + 18, 0x20FFFFFF);
            }

            // 第一行：选中标记 + 名字 + 元素/形态
            long cd = tech.remainingSeconds(t, now);
            String head = (isSel ? "§e▶ " : "§8  ") + t.colored()
                    + " §8[" + t.element().cn() + t.kind().glyph() + "]";
            graphics.drawString(font, Component.literal(head), tx, rowY + 2, TEXT, false);
            // 右上角：冷却或就绪
            String state = cd > 0 ? "§c" + cd + "s" : "§a就绪";
            graphics.drawString(font, Component.literal(state),
                    x + TECH_W - 8 - font.width(state), rowY + 2, TEXT, false);
            // 第二行：特性（截断到宽度内）
            String trait = font.plainSubstrByWidth("§7" + t.trait(), TECH_W - 20);
            graphics.drawString(font, Component.literal(trait), tx + 6, rowY + 11, TEXT, false);
        }

        // 底部操作提示
        int hintY = ty + known.size() * 20 + 4;
        graphics.drawString(font, Component.literal("§8Z X C V 施展 · 点击选中"),
                tx, hintY, TEXT, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================================================================
    // 输入
    // ==================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // 页签
        if (mouseY >= tabY && mouseY <= tabY + TAB_H) {
            for (int i = 0; i < PAGES.length; i++) {
                if (mouseX >= tabX[i] && mouseX <= tabX[i] + tabW[i]) {
                    switchPage(i);
                    return true;
                }
            }
        }
        // 功法页：点击某一门就选中它
        if (techListVisible) {
            for (int i = 0; i < techHitY.length; i++) {
                if (mouseX >= techListX && mouseX <= techListX + techListW
                        && mouseY >= techHitY[i] && mouseY <= techHitY[i] + 20) {
                    com.hongmeng.xiuxian.network.TechniqueActionPacket.select(i);
                    return true;
                }
            }
        }
        // 常驻功法栏：点击某一门就选中它
        if (columnVisible) {
            for (int i = 0; i < columnHitY.length; i++) {
                if (mouseX >= columnX && mouseX <= columnX + columnW
                        && mouseY >= columnHitY[i] && mouseY <= columnHitY[i] + 18) {
                    com.hongmeng.xiuxian.network.TechniqueActionPacket.select(i);
                    return true;
                }
            }
        }
        // ---- 择道区：必须用 pathChooseVisible，**不能**用 talentVisible ----
        //
        //   talentVisible = (画了几个天赋节点 > 0)，而**未择道时是 0**
        //   —— 用它当门，择道点击永远不执行。这就是"点了没反应"的根因：
        //   界面上五个选项画着，鼠标点上去什么也不发生。
        if (pathChooseVisible) {
            for (int i = 0; i < pathHitY.length; i++) {
                if (pathHitOrd[i] < 0) {
                    continue;
                }
                if (mouseX >= pathHitX && mouseX <= pathHitX + pathHitW
                        && mouseY >= pathHitY[i] && mouseY <= pathHitY[i] + 11) {
                    com.hongmeng.xiuxian.network.PathChoosePacket.send(pathHitOrd[i]);
                    return true;
                }
            }
        }
        // 天赋节点：点一下加一点（发包给服务端校验，客户端不自己改数据）
        if (talentVisible) {
            for (int i = 0; i < talentHitY.length; i++) {
                if (talentHitId[i] == null) {
                    continue;
                }
                if (mouseX >= talentHitX && mouseX <= talentHitX + talentHitW
                        && mouseY >= talentHitY[i] && mouseY <= talentHitY[i] + 9) {
                    com.hongmeng.xiuxian.network.TalentPacket.send(talentHitId[i]);
                    return true;
                }
            }
        }
        // 破境按钮
        if (btButtonVisible
                && mouseX >= btButtonX && mouseX <= btButtonX + btButtonW
                && mouseY >= btButtonY && mouseY <= btButtonY + btButtonH) {
            com.hongmeng.xiuxian.network.BreakthroughPacket.send();
            return true;
        }
        // 常驻速度 II 开关
        if (speedButtonVisible
                && mouseX >= speedButtonX && mouseX <= speedButtonX + speedButtonW
                && mouseY >= speedButtonY && mouseY <= speedButtonY + speedButtonH) {
            boolean next = !com.hongmeng.xiuxian.network.ClientCultivationCache
                    .get().isSpeedBoostEnabled();
            // 本地先翻转，服务端确认后会回同步覆盖
            com.hongmeng.xiuxian.network.ClientCultivationCache
                    .get().setSpeedBoostEnabled(next);
            com.hongmeng.xiuxian.network.ToggleSpeedPacket.send(next);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal(next
                                ? "§a常驻速度 II：已开启"
                                : "§c常驻速度 II：已关闭"), true);
            }
            return true;
        }
        // 领域友伤开关（规格 §2.1）：本地先翻转，服务端确认后回同步覆盖
        if (ffButtonVisible
                && mouseX >= ffButtonX && mouseX <= ffButtonX + ffButtonW
                && mouseY >= ffButtonY && mouseY <= ffButtonY + ffButtonH) {
            boolean next = !com.hongmeng.xiuxian.network.ClientCultivationCache
                    .get().isDomainFriendlyFire();
            com.hongmeng.xiuxian.network.ClientCultivationCache
                    .get().setDomainFriendlyFire(next);
            com.hongmeng.xiuxian.network.DomainActionPacket.send(
                    com.hongmeng.xiuxian.network.DomainActionPacket.Action.FRIENDLY_FIRE, 0);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal(next
                                ? "§c领域友伤：开 §8（宠物 / NPC 也会被领域打）"
                                : "§a领域友伤：关 §8（只打敌对生物）"), true);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 换页：顺手关掉上一页遗留的按钮命中区。 */
    private void switchPage(int target) {
        if (target < 0 || target >= PAGES.length || target == page) {
            return;
        }
        page = target;
        btButtonVisible = false;
        speedButtonVisible = false;
        ffButtonVisible = false;
        techListVisible = false;
        techHitY = new int[0];
    }

    /** 面板打开时按 R 或 ESC 关闭；[ ] ← → Tab 切页，1~8 直达。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_R
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_BRACKET
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            switchPage((page + 1) % PAGES.length);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_BRACKET
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
            switchPage((page - 1 + PAGES.length) % PAGES.length);
            return true;
        }
        // 数字键 1..8 直达对应页。
        //
        // **但"功法"页要用 1~9 装法术**（玩家要求：法术页按 123456789 装备法术），
        // 两件事撞在同一个键上。裁决规则：**在功法页时数字键归功法页**，
        // 想切页用 [ ] ← → Tab 或鼠标点页签（那些都没被占用）。
        if (page == PAGE_TECHNIQUE) {
            switch (keyCode) {
                case org.lwjgl.glfw.GLFW.GLFW_KEY_1, org.lwjgl.glfw.GLFW.GLFW_KEY_2,
                     org.lwjgl.glfw.GLFW.GLFW_KEY_3, org.lwjgl.glfw.GLFW.GLFW_KEY_4,
                     org.lwjgl.glfw.GLFW.GLFW_KEY_5,
                     org.lwjgl.glfw.GLFW.GLFW_KEY_Z, org.lwjgl.glfw.GLFW.GLFW_KEY_X,
                     org.lwjgl.glfw.GLFW.GLFW_KEY_C, org.lwjgl.glfw.GLFW.GLFW_KEY_V,
                     org.lwjgl.glfw.GLFW.GLFW_KEY_0,
                     org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE -> {
                    // 与法术界面同一套规则：1~5 装被动、Z X C V 装主动、0 清空
                    hotkeyEquipTechnique(keyCode);
                    return true;
                }
                default -> {
                    // 落到下面的通用切页逻辑
                }
            }
        }
        for (int i = 0; i < PAGES.length; i++) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_1 + i) {
                switchPage(i);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 功法页的数字键装法术（与 {@code SpellBookScreen} 里的按键逻辑同一套规则）。
     *
     * <p>1~5 = 第 N 个被动槽、6~9 = 第 N-5 个主动槽、0/Backspace = 清空。
     * 编号取的是**当前功法页列表里第几个**，所以"挨个按过去"就是顺着列表走。</p>
     */
    private void hotkeyEquipTechnique(int keyCode) {
        var data = com.hongmeng.xiuxian.core.CultivationCapability.get(minecraft.player);
        if (data == null) {
            return;
        }
        var tech = data.getTechniques();
        var list = tech.knownList();
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_0
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            for (int i = 0; i < com.hongmeng.xiuxian.technique.TechniqueData.PASSIVE_SLOTS; i++) {
                com.hongmeng.xiuxian.network.TechniqueActionPacket.equipPassiveSlot(i);
            }
            for (int i = 0; i < com.hongmeng.xiuxian.technique.TechniqueData.ACTIVE_SLOTS; i++) {
                com.hongmeng.xiuxian.network.TechniqueActionPacket.equipActiveSlot(i);
            }
            techHotkeyNote = "\u00a77已清空全部法术槽";
            techHotkeyAt = System.currentTimeMillis();
            return;
        }
        // Z X C V → 主动槽 1~4（与法术界面一致）
        int active = -1;
        switch (keyCode) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_Z -> active = 0;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_X -> active = 1;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_C -> active = 2;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_V -> active = 3;
            default -> {
                // 不是 ZXCV
            }
        }
        if (active >= 0) {
            com.hongmeng.xiuxian.technique.Technique chosen = null;
            for (var t : list) {
                if (!t.isPassive()) {
                    chosen = t;
                    break;
                }
            }
            if (chosen == null) {
                techHotkeyNote = "\u00a7c没有可装的主动功法";
                techHotkeyAt = System.currentTimeMillis();
                return;
            }
            com.hongmeng.xiuxian.network.TechniqueActionPacket.equipActiveSlot(active);
            techHotkeyNote = "\u00a7a已装备 " + chosen.cn() + " \u00a78→ 主动槽 "
                    + (active + 1) + " \u00a77（按 " + (char) ('Z' + active) + " 施放）";
            techHotkeyAt = System.currentTimeMillis();
            return;
        }

        int digit = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1 + 1;
        if (digit < 1 || digit > 5) {
            return;
        }
        if (digit > list.size()) {
            techHotkeyNote = "\u00a7c列表里没有第 " + digit + " 门功法（当前 " + list.size() + " 门）";
            techHotkeyAt = System.currentTimeMillis();
            return;
        }
        var t = list.get(digit - 1);
        // 玩家要求：数字键只管被动（主动用 Z X C V 施放）。
        if (!t.isPassive()) {
            techHotkeyNote = "\u00a7c" + t.cn() + " 是主动功法 —— 装好后用 Z X C V 施放";
            techHotkeyAt = System.currentTimeMillis();
            return;
        }
        com.hongmeng.xiuxian.network.TechniqueActionPacket.equipPassiveSlot(digit - 1);
        techHotkeyNote = "\u00a7a已装备 " + t.cn() + " \u00a78→ 被动槽 " + digit;
        techHotkeyAt = System.currentTimeMillis();
    }

    /** 功法页数字键装法术的反馈（5 秒内有效）。 */
    private String techHotkeyNote = "";
    private long techHotkeyAt = 0L;

    /** 鼠标滚轮也切页（方向直觉：往下滚看下一页）。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            switchPage((page - 1 + PAGES.length) % PAGES.length);
        } else if (delta < 0) {
            switchPage((page + 1) % PAGES.length);
        }
        return true;
    }
}
