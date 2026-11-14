package com.hongmeng.xiuxian.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>鸿蒙修行录</b>的界面（玩家要求：类似植物魔法的那本书）。
 *
 * <p>左栏分类、顶部搜索、右侧详情。搜索是<b>全局</b>的：名称、id、以及
 * 每一条来源描述都会被匹配，所以"搜一个词就能把相关的物品/结构/群系/NPC 全捞出来"。</p>
 *
 * <p>数据来自 {@code data/hongmeng/guide/guide.json}，由
 * {@code tools/build_guide_data.py} 扫描生成 —— 改了配方书里就跟着变。</p>
 */
@OnlyIn(Dist.CLIENT)
public class GuideBookScreen extends Screen {

    // ---- 配色（与法术栏/修炼面板同一套） ----
    private static final int BG = 0xF0141A2E;
    private static final int BORDER = 0xFF8A6FD8;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int DIM = 0xFF9AA0B8;
    private static final int TAB_ON = 0x40FFD24A;
    private static final int TAB_OFF = 0xFF1C2438;
    private static final int ROW_SEL = 0x40FFD24A;
    private static final int LINE = 0xFF2A3350;
    /** 配方格子的底与边（与法术栏同一套）。 */
    private static final int SLOT_BG = 0xFF1C2438;
    private static final int SLOT_BORDER = 0xFF3A4460;

    /** 分类。 */
    private static final String[][] TABS = {
            {"item", "物品"},
            // 2026-11-07 分类细化：丹药 / 材料 从「物品」里拆出来单独成页，
            // 玩家找"涨修为吃什么""某某材料哪来的"不用在 100+ 条物品里翻。
            {"pill", "丹药"},
            {"material", "材料"},
            {"block", "方块"},
            {"gear", "法宝"},
            {"armor", "护甲"},
            {"manual", "秘籍"},
            {"structure", "建筑"},
            {"biome", "群系"},
            {"npc", "NPC"},
            // 玩法：道途 / 天赋 / 副业 / 剑阵 / 机制 / 洗点。
            // 之前这本手册只有物品图鉴，玩家"有书可看却学不到怎么玩"。
            {"guide", "玩法"},
    };

    /** 来源类型 → 颜色（一眼分得清"合成的"还是"打怪掉的"）。 */
    private static int colorOf(String type) {
        return switch (type == null ? "" : type) {
            case "craft" -> 0xFF6FD86F;
            case "smelt" -> 0xFFE0A040;
            case "mob" -> 0xFFE05A5A;
            case "chest" -> 0xFFFFD24A;
            case "ore" -> 0xFF9AA0B8;
            case "plant" -> 0xFF5AD8A0;
            case "shop" -> 0xFF5AC8E0;
            case "quest" -> 0xFFD86FD8;
            case "trade" -> 0xFFA0D860;
            case "dimension" -> 0xFF8A6FD8;
            default -> TEXT;
        };
    }

    /** 来源类型的中文标签。 */
    private static String cnOf(String type) {
        return switch (type == null ? "" : type) {
            case "craft" -> "合成";
            case "smelt" -> "冶炼";
            case "mob" -> "杀怪";
            case "chest" -> "宝箱";
            case "ore" -> "挖矿";
            case "plant" -> "采集";
            case "shop" -> "坊市";
            case "quest" -> "任务";
            case "trade" -> "交易";
            case "dimension" -> "维度";
            // 玩法说明（道途/天赋/副业…）—— 不是"获得途径"，但复用同一套渲染
            case "guide" -> "玩法";
            default -> "其它";
        };
    }

    // ---- 数据 ----
    private JsonObject root;
    private boolean loaded;
    private String tab = "item";
    private String query = "";
    private int scroll;
    private int selected = -1;

    /** 详情页开关（双击进入，Esc / 再双击退出）。 */
    private boolean detailOpen;
    /** 上一次点击的时间与位置 —— 用来识别"双击"。 */
    private long lastClickAt;
    private double lastClickX;
    private double lastClickY;

    // ---- 当前分类下过滤后的条目 ----
    private final List<String> keys = new ArrayList<>();
    private final List<String> names = new ArrayList<>();

    private EditBox search;

    public GuideBookScreen() {
        super(Component.literal("鸿蒙修行录"));
    }

    // ------------------------------------------------------------------
    // 加载
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        // 每次打开都重新读一次：不再依赖"重载事件是否及时触发"。
        // 打开书的那一刻资源包必然已就绪，所以这一步是可靠的。
        load();
        loaded = true;
        // 搜索框
        search = new EditBox(this.font, this.width - 216, 24, 200, 16,
                Component.literal("搜索"));
        search.setHint(Component.literal("\u00a78搜索物品 / 结构 / 群系 / NPC"));
        search.setResponder(s -> {
            query = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
            scroll = 0;
            selected = -1;
            rebuild();
        });
        addRenderableWidget(search);
        rebuild();
    }

    private void load() {
        // 强制重新读一次（GuideData 内部有 try/catch，失败会把原因留在 error 里）。
        // 第一版是在这里临时读、读不到就当空数据 → 玩家看到的是空书且无从判断；
        // 现在打开必重读 + 失败必显示原因。
        GuideData.reload();
        root = GuideData.root();
    }

    private JsonObject section(String name) {
        if (root == null || !root.has(name) || !root.get(name).isJsonObject()) {
            return new JsonObject();
        }
        return root.getAsJsonObject(name);
    }

    /** 归类：优先用数据里的 kind，没有就按 id 猜。 */
    private String kindOf(String id, JsonObject entry) {
        String kind = entry.has("kind") ? GsonHelper.getAsString(entry, "kind", "") : "";
        if (!kind.isEmpty()) {
            return switch (kind) {
                case "block_item" -> "block";
                case "gear", "weapon" -> "gear";
                case "armor" -> "armor";
                case "manual", "technique" -> "manual";
                case "pill" -> "pill";
                case "material" -> "material";
                case "guide" -> "guide";
                default -> "item";
            };
        }
        String p = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        if (p.startsWith("pill_") || p.endsWith("_dan")) {
            return "pill";
        }
        if (p.contains("helmet") || p.contains("chestplate") || p.contains("leggings")
                || p.contains("boots")) {
            return "armor";
        }
        if (p.contains("sword") || p.contains("blade") || p.contains("artifact")
                || p.contains("talisman")) {
            return "gear";
        }
        if (p.contains("manual") || p.contains("technique") || p.contains("_book")) {
            return "manual";
        }
        return "item";
    }

    /** 按当前分类 + 搜索词重建列表。 */
    private void rebuild() {
        keys.clear();
        names.clear();
        if ("structure".equals(tab)) {
            addAll(section("structures"), "structure");
        } else if ("biome".equals(tab)) {
            addAll(section("biomes"), "biome");
        } else if ("npc".equals(tab)) {
            addAll(section("npcs"), "npc");
        } else {
            JsonObject items = section("items");
            for (Map.Entry<String, JsonElement> e : items.entrySet()) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject o = e.getValue().getAsJsonObject();
                if (!tab.equals(kindOf(e.getKey(), o))) {
                    continue;
                }
                if (matches(e.getKey(), o)) {
                    keys.add(e.getKey());
                    names.add(display(e.getKey(), o));
                }
            }
        }
    }

    private void addAll(JsonObject sec, String _k) {
        for (Map.Entry<String, JsonElement> e : sec.entrySet()) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            JsonObject o = e.getValue().getAsJsonObject();
            if (matches(e.getKey(), o)) {
                keys.add(e.getKey());
                names.add(display(e.getKey(), o));
            }
        }
    }

    private String display(String id, JsonObject o) {
        String cn = o.has("cn") ? GsonHelper.getAsString(o, "cn", "") : "";
        return cn.isEmpty() ? shortId(id) : cn;
    }

    private static String shortId(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }

    /**
     * 按像素宽度**折行**（不是截断）。
     *
     * <p>为什么必须折行：原来的 `plainSubstrByWidth` 是截断 ——
     * 长句在一半被砍掉、还没有省略号，读起来像坏掉了。</p>
     *
     * @param text     原文（可含 § 颜色码）
     * @param width    可用宽度（像素）
     * @param maxLines 最多几行；超出时最后一行加「…」
     */
    private java.util.List<String> wrapText(String text, int width, int maxLines) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (text == null || text.isEmpty() || width <= 0) {
            out.add(text == null ? "" : text);
            return out;
        }
        String rest = text;
        while (!rest.isEmpty() && out.size() < maxLines) {
            String cut = this.font.plainSubstrByWidth(rest, width);
            if (cut.isEmpty()) {
                cut = rest.substring(0, 1);          // 防死循环
            }
            // 如果不是最后一行、且还有剩余，尽量在标点/空格处断开
            if (cut.length() < rest.length() && out.size() < maxLines - 1) {
                int brk = Math.max(
                        Math.max(cut.lastIndexOf(' '), cut.lastIndexOf('、')),
                        Math.max(cut.lastIndexOf('，'), cut.lastIndexOf('；')));
                if (brk > cut.length() / 2) {
                    cut = cut.substring(0, brk + 1);
                }
            }
            out.add(cut);
            rest = rest.substring(cut.length());
        }
        if (!rest.isEmpty() && !out.isEmpty()) {
            int last = out.size() - 1;
            String tail = out.get(last);
            out.set(last, this.font.plainSubstrByWidth(tail + "…",
                    Math.max(8, width)));
        }
        return out;
    }

    /** 搜索匹配：名称 / id / **全部来源描述** 都算。 */
    private boolean matches(String id, JsonObject o) {
        if (query.isEmpty()) {
            return true;
        }
        StringBuilder sb = new StringBuilder(id).append(' ')
                .append(o.has("cn") ? GsonHelper.getAsString(o, "cn", "") : "");
        for (String field : new String[]{"sources", "chest_items", "worldgen_blocks",
                "biomes", "dimension", "spawn_egg"}) {
            if (o.has(field)) {
                sb.append(' ').append(o.get(field).toString());
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT).contains(query);
    }

    // ------------------------------------------------------------------
    // 布局
    // ------------------------------------------------------------------

    private int panelX() {
        return Math.max(8, this.width / 2 - 260);
    }

    private int panelW() {
        return Math.min(520, this.width - 24);
    }

    private int listTop() {
        return 64;
    }

    private int listBottom() {
        return this.height - 28;
    }

    private int rowH() {
        return 14;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / rowH());
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int x = panelX(), w = panelW(), y = 16, h = this.height - 32;
        g.fill(x, y, x + w, y + h, BG);
        g.renderOutline(x, y, w, h, BORDER);

        // 标题 + 统计（统计用**数据总量**而不是"当前分类条数"，
        // 这样空分类与"数据没加载"一眼能分辨）
        g.drawString(this.font, "\u00a7d鸿蒙修行录", x + 10, y + 7, TEXT, false);
        String stat = "物品 " + GuideData.itemCount() + " · 来源 " + GuideData.sourceCount()
                + " · 当前 " + keys.size();
        g.drawString(this.font, "\u00a78" + stat, x + 10 + this.font.width("鸿蒙修行录") + 14,
                y + 7, DIM, false);
        // 加载失败时把原因直接画出来（不再静默变空）
        if (!GuideData.error().isEmpty()) {
            g.drawString(this.font, "\u00a7c数据加载失败：" + GuideData.error()
                    + "  \u00a78(" + GuideData.location() + ")",
                    x + 10, y + 20, 0xFFFF8080, false);
        }

        // 左侧分类
        int ty = y + 24;
        for (String[] t : TABS) {
            boolean on = t[0].equals(tab);
            int tw = this.font.width(t[1]) + 12;
            g.fill(x + 8, ty, x + 8 + tw, ty + 14, on ? TAB_ON : TAB_OFF);
            if (on) {
                g.renderOutline(x + 8, ty, tw, 14, BORDER);
            }
            g.drawString(this.font, t[1], x + 14, ty + 3, on ? 0xFFFFD24A : DIM, false);
            ty += 16;
        }

        // 列表区
        int lx = x + 8 + 78;
        int lw = w - 78 - 16;
        g.fill(lx - 2, listTop() - 2, lx + lw + 2, listBottom() + 2, 0x60000000);
        g.renderOutline(lx - 2, listTop() - 2, lw + 4, listBottom() - listTop() + 4, LINE);

        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, keys.size() - rows)));
        if (keys.isEmpty()) {
            String why = GuideData.itemCount() <= 0
                    ? "\u00a7c数据没加载出来（见上方红字）"
                    : (query.isEmpty() ? "\u00a78这个分类下暂时没有条目"
                            : "\u00a78没有匹配 \"" + query + "\" 的条目");
            g.drawString(this.font, why, lx + 6, listTop() + 6, DIM, false);
        }
        // 详情页打开时**整个列表都不画** —— 从根上杜绝两页文字互相压字
        //（玩家两次反馈「下面的图表上来了把上面的字顶掉了」）。
        for (int i = 0; !detailOpen && i < rows && i + scroll < keys.size(); i++) {
            int idx = i + scroll;
            int ry = listTop() + i * rowH();
            boolean sel = idx == selected;
            boolean hover = mouseX >= lx && mouseX <= lx + lw && mouseY >= ry && mouseY < ry + rowH();
            if (sel || hover) {
                g.fill(lx, ry, lx + lw, ry + rowH(), sel ? ROW_SEL : 0x20FFFFFF);
            }
            // 物品图标（能取到就画真物品）
            String id = keys.get(idx);
            ItemStack icon = iconOf(id);
            if (!icon.isEmpty()) {
                g.renderItem(icon, lx + 1, ry - 3);
            }
            g.drawString(this.font, names.get(idx), lx + 20, ry + 3,
                    sel ? 0xFFFFD24A : TEXT, false);
        }

        // 详情：双击 → 全屏详情页（带合成配方）；单击 → 右侧简要
        if (selected >= 0 && selected < keys.size()) {
            if (detailOpen) {
                renderFullDetail(g, keys.get(selected), x + 8, listTop() - 2, w - 16);
            } else if (lw > 220) {
                renderDetail(g, keys.get(selected), lx + lw + 6, listTop(),
                        x + w - (lx + lw + 6) - 6);
            }
        }

        // 底部提示
        g.drawString(this.font,
                "\u00a78滚轮翻页 · 输字即筛 · \u00a7eR\u00a78 重载数据 · Esc 关闭",
                x + 10, y + h - 14, DIM, false);
        super.render(g, mouseX, mouseY, partial);
    }

    /** 按 id 取物品（取不到就返回空）。 */
    private ItemStack iconOf(String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);
            var item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Throwable ignored) {
        }
        return ItemStack.EMPTY;
    }

    /** 右侧详情。 */
    private void renderDetail(GuiGraphics g, String id, int dx, int dy, int dw) {
        if (dw < 90) {
            return;
        }
        JsonObject o = entryOf(id);
        if (o == null) {
            return;
        }
        int yy = dy;
        // 名称
        ItemStack icon = iconOf(id);
        if (!icon.isEmpty()) {
            g.renderItem(icon, dx, yy - 4);
            g.drawString(this.font, display(id, o), dx + 20, yy, 0xFFFFD24A, false);
        } else {
            g.drawString(this.font, display(id, o), dx, yy, 0xFFFFD24A, false);
        }
        yy += 14;
        g.drawString(this.font, "\u00a78" + id, dx, yy, DIM, false);
        yy += 12;
        g.fill(dx, yy, dx + dw - 4, yy + 1, LINE);
        yy += 5;

        // 结构/群系/NPC 的附加信息
        if (o.has("dimension")) {
            g.drawString(this.font, "\u00a77维度：\u00a7b" + shortId(GsonHelper.getAsString(o, "dimension", "")),
                    dx, yy, TEXT, false);
            yy += 12;
        }
        if (o.has("biomes") && o.get("biomes").isJsonArray()) {
            g.drawString(this.font, "\u00a77群系：\u00a7a" + join(o.getAsJsonArray("biomes"), 4),
                    dx, yy, TEXT, false);
            yy += 12;
        }
        if (o.has("worldgen_blocks") && o.get("worldgen_blocks").isJsonArray()) {
            g.drawString(this.font, "\u00a77生成：\u00a7a" + join(o.getAsJsonArray("worldgen_blocks"), 5),
                    dx, yy, TEXT, false);
            yy += 12;
        }
        if (o.has("spawn_egg")) {
            g.drawString(this.font, "\u00a77刷怪蛋：\u00a7e" + shortId(GsonHelper.getAsString(o, "spawn_egg", "")),
                    dx, yy, TEXT, false);
            yy += 12;
        }

        // 获得途径
        JsonArray src = o.has("sources") && o.get("sources").isJsonArray()
                ? o.getAsJsonArray("sources") : new JsonArray();
        // 「玩法」页签下这些不是"获得途径"，而是说明
        String srcTitle = "guide".equals(tab) ? "\u00a76说明" : "\u00a76获得途径";
        g.drawString(this.font, srcTitle + "\u00a78（" + src.size() + "）", dx, yy,
                TEXT, false);
        yy += 12;
        int shown = 0;
        for (JsonElement e : src) {
            // 换行后一条最多占 4 行（44 像素），所以留足空间再画，
            // 否则最后一个条目会被面板底边裁掉。
            if (yy > listBottom() - 48 || shown > 12) {
                g.drawString(this.font, "\u00a78…", dx + 4, yy, DIM, false);
                break;
            }
            JsonObject s = e.getAsJsonObject();
            String type = s.has("type") ? GsonHelper.getAsString(s, "type", "") : "";
            String detail = s.has("detail") ? GsonHelper.getAsString(s, "detail", "") : "";
            // 类型标签
            String tag = "[" + cnOf(type) + "]";
            g.drawString(this.font, tag, dx, yy, colorOf(type), false);
            int tw = this.font.width(tag) + 4;
            // ⚠ 详情必须**折行**，不能截断。
            //
            //   原来是 `plainSubstrByWidth(detail, ...)` —— 那是截断，
            //   长句会在一半被砍掉且没有省略号，读起来像坏掉了。
            //   「玩法」条目每句都长，所以这里换成 wrapText。
            int avail = Math.max(40, dw - tw - 8);
            java.util.List<String> lines = wrapText(detail, avail, 4);
            for (int li = 0; li < lines.size(); li++) {
                g.drawString(this.font, lines.get(li),
                        dx + tw, yy + li * 10, TEXT, false);
            }
            yy += 11 * Math.max(1, lines.size());
            shown++;
        }

        // 宝箱产出
        if (o.has("chest_items") && o.get("chest_items").isJsonObject()) {
            yy += 3;
            g.drawString(this.font, "\u00a76箱内产出", dx, yy, TEXT, false);
            yy += 11;
            for (Map.Entry<String, JsonElement> ce : o.getAsJsonObject("chest_items").entrySet()) {
                if (yy > listBottom() - 10) {
                    break;
                }
                String cn = ce.getValue().isJsonObject()
                        ? GsonHelper.getAsString(ce.getValue().getAsJsonObject(), "cn", shortId(ce.getKey()))
                        : shortId(ce.getKey());
                g.drawString(this.font, "\u00a78· \u00a7f" + cn, dx + 4, yy, TEXT, false);
                yy += 10;
            }
        }
    }

    private String join(JsonArray arr, int max) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (JsonElement e : arr) {
            if (n >= max) {
                sb.append("…");
                break;
            }
            if (n > 0) {
                sb.append("、");
            }
            String t = e.getAsString();
            sb.append(t.startsWith("#") ? "(标签)" : t);
            n++;
        }
        return sb.toString();
    }


    // ------------------------------------------------------------------
    // 全屏详情页（双击打开）：介绍 + 获得途径 + **合成配方网格**
    // ------------------------------------------------------------------

    /**
     * 画全屏详情页。
     *
     * <p>玩家要求：「双击物品可以查看物品介绍和合成配方」。</p>
     *
     * <p>配方不用客户端解析原版配方 —— {@code guide.json} 里已经带了
     * 归一化的 {@code {type, pattern, key, ingredients, result}}，
     * 这里只按 pattern 画格子。</p>
     */
    private void renderFullDetail(GuiGraphics g, String id, int dx, int dy, int dw) {
        JsonObject o = entryOf(id);
        if (o == null) {
            return;
        }
        // 半透明底：盖住列表，避免两页信息叠在一起
        // **不透明**底：详情页打开时把列表整块遮住，
        // 否则两页文字会互相压字（玩家截图里"土行貂刷怪蛋"盖住"炼丹师刷怪蛋"）。
        // 把详情页要占的整块区域**先用不透明色清一遍** ——
        // 玩家两次反馈「下面的图表上来了把上面的字顶掉了」。
        // 关键是：清底要覆盖到**列表的每一行**（含高亮行），而不只是面板本身。
        g.fill(dx - 8, dy - 6, dx + dw + 8, listBottom() + 16, BG);
        g.fill(dx - 8, dy - 6, dx + dw + 8, listTop() + 2, BG);
        g.renderOutline(dx - 6, dy - 4, dw + 12, listBottom() - dy + 14, BORDER);

        int yy = dy + 2;
        ItemStack icon = iconOf(id);
        if (!icon.isEmpty()) {
            g.renderItem(icon, dx, yy);
            g.pose().pushPose();
            g.pose().translate(dx, yy, 0);
            g.pose().scale(2.0F, 2.0F, 1.0F);
            g.renderItem(icon, 0, 0);
            g.pose().popPose();
            g.drawString(this.font, this.font.plainSubstrByWidth(display(id, o), dw - 46),
                    dx + 40, yy + 2, 0xFFFFD24A, false);
            g.drawString(this.font, this.font.plainSubstrByWidth(id, dw - 46),
                    dx + 40, yy + 14, DIM, false);
            yy += 36;
        } else {
            g.drawString(this.font, display(id, o), dx, yy, 0xFFFFD24A, false);
            g.drawString(this.font, "\u00a78" + id, dx, yy + 12, DIM, false);
            yy += 26;
        }
        g.fill(dx, yy, dx + dw - 8, yy + 1, LINE);
        yy += 6;

        // ---- 获得途径 ----
        JsonArray src = o.has("sources") && o.get("sources").isJsonArray()
                ? o.getAsJsonArray("sources") : new JsonArray();
        g.drawString(this.font, "\u00a76获得途径\u00a78（" + src.size() + "）", dx, yy, TEXT, false);
        yy += 12;
        int shown = 0;
        for (JsonElement e : src) {
            if (yy > listBottom() - 60 || shown > 8) {
                g.drawString(this.font, "\u00a78…", dx + 4, yy, DIM, false);
                yy += 10;
                break;
            }
            JsonObject s = e.getAsJsonObject();
            String type = s.has("type") ? GsonHelper.getAsString(s, "type", "") : "";
            String detail = s.has("detail") ? GsonHelper.getAsString(s, "detail", "") : "";
            String tag = "[" + cnOf(type) + "]";
            int avail = Math.max(40, dw - this.font.width(tag) - 20);
            g.drawString(this.font, tag, dx, yy, colorOf(type), false);
            g.drawString(this.font, this.font.plainSubstrByWidth(detail, avail),
                    dx + this.font.width(tag) + 4, yy, TEXT, false);
            yy += 11;
            shown++;
        }

        // ---- 合成配方 ----
        yy += 6;
        g.fill(dx, yy, dx + dw - 8, yy + 1, LINE);
        yy += 6;
        JsonArray rids = o.has("recipes") && o.get("recipes").isJsonArray()
                ? o.getAsJsonArray("recipes") : new JsonArray();
        if (rids.size() == 0) {
            g.drawString(this.font, "\u00a76合成配方", dx, yy, TEXT, false);
            yy += 12;
            g.drawString(this.font,
                    this.font.plainSubstrByWidth(
                            "此物品没有合成配方 —— 按上面的途径获得（挖矿 / 采集 / 掉落 / 坊市 / NPC 等）",
                            dw - 12),
                    dx + 4, yy, DIM, false);
        } else {
            g.drawString(this.font, "\u00a76合成配方\u00a78（" + rids.size() + " 条）",
                    dx, yy, TEXT, false);
            yy += 14;
            int drawn = 0;
            for (JsonElement re : rids) {
                if (yy > listBottom() - 46 || drawn >= 2) {
                    break;
                }
                drawRecipe(g, re.getAsString(), dx, yy, dw);
                yy += 46;
                drawn++;
            }
        }
    }

    /** 画一条配方（3×3 网格或一排材料）。 */
    private void drawRecipe(GuiGraphics g, String recipeId, int dx, int dy, int dw) {
        JsonObject all = GuideData.root() == null ? null : GuideData.root()
                .getAsJsonObject("recipes");
        if (all == null || !all.has(recipeId)) {
            g.drawString(this.font, "\u00a78" + recipeId, dx, dy, DIM, false);
            return;
        }
        JsonObject r = all.getAsJsonObject(recipeId);
        JsonObject result = r.has("result") && r.get("result").isJsonObject()
                ? r.getAsJsonObject("result") : new JsonObject();
        JsonArray pattern = r.has("pattern") && r.get("pattern").isJsonArray()
                ? r.getAsJsonArray("pattern") : new JsonArray();
        JsonObject key = r.has("key") && r.get("key").isJsonObject()
                ? r.getAsJsonObject("key") : new JsonObject();

        int cell = 18;
        int gx = dx + 2;
        int gy = dy + 2;
        if (pattern.size() > 0) {
            // 有序配方：3×3
            for (int row = 0; row < pattern.size() && row < 3; row++) {
                String line = pattern.get(row).getAsString();
                for (int col = 0; col < line.length() && col < 3; col++) {
                    int cx = gx + col * cell;
                    int cy = gy + row * cell;
                    g.fill(cx, cy, cx + cell - 2, cy + cell - 2, SLOT_BG);
                    g.renderOutline(cx, cy, cell - 2, cell - 2, SLOT_BORDER);
                    char ch = line.charAt(col);
                    if (ch == ' ') {
                        continue;
                    }
                    String iid = key.has(String.valueOf(ch))
                            ? GsonHelper.getAsString(key, String.valueOf(ch), "") : "";
                    ItemStack st = iconOf(iid);
                    if (!st.isEmpty()) {
                        g.renderItem(st, cx + 1, cy + 1);
                    }
                }
            }
        } else {
            // 无序配方：一排材料
            JsonArray ings = r.has("ingredients") && r.get("ingredients").isJsonArray()
                    ? r.getAsJsonArray("ingredients") : new JsonArray();
            int i = 0;
            for (JsonElement ie : ings) {
                if (i >= 9) {
                    break;
                }
                int cx = gx + i * cell;
                g.fill(cx, gy, cx + cell - 2, gy + cell - 2, SLOT_BG);
                g.renderOutline(cx, gy, cell - 2, cell - 2, SLOT_BORDER);
                String iid = ie.isJsonObject()
                        ? GsonHelper.getAsString(ie.getAsJsonObject(), "item", "") : "";
                ItemStack st = iconOf(iid);
                if (!st.isEmpty()) {
                    g.renderItem(st, cx + 1, gy + 1);
                }
                i++;
            }
        }
        // 箭头 + 产物
        int ax = gx + 3 * cell + 6;
        g.drawString(this.font, "\u00a77→", ax, gy + 22, TEXT, false);
        int px = ax + 16;
        g.fill(px, gy + 12, px + 20, gy + 34, SLOT_BG);
        g.renderOutline(px, gy + 12, 20, 22, BORDER);
        String outId = result.has("item") ? GsonHelper.getAsString(result, "item", "") : "";
        ItemStack out = iconOf(outId);
        if (!out.isEmpty()) {
            g.renderItem(out, px + 2, gy + 15);
        }
        int cnt = result.has("count") ? GsonHelper.getAsInt(result, "count", 1) : 1;
        if (cnt > 1) {
            g.drawString(this.font, "×" + cnt, px + 22, gy + 20, 0xFFFFD24A, false);
        }
        g.drawString(this.font, "\u00a78" + GsonHelper.getAsString(r, "type", ""),
                px + 34, gy + 20, DIM, false);
    }

    private JsonObject entryOf(String id) {
        for (String sec : new String[]{"items", "structures", "biomes", "npcs"}) {
            JsonObject s = section(sec);
            if (s.has(id) && s.get(id).isJsonObject()) {
                return s.getAsJsonObject(id);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 交互
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = panelX(), w = panelW();
        // 点标题栏 = 重新加载（数据没读到时唯一的自救办法）
        if (my >= 16 && my <= 16 + 30 && mx >= x + 6 && mx <= x + w - 6) {
            retry();
            return true;
        }
        int ty = 16 + 24;
        for (String[] t : TABS) {
            int tw = this.font.width(t[1]) + 12;
            if (mx >= x + 8 && mx <= x + 8 + tw && my >= ty && my <= ty + 14) {
                tab = t[0];
                scroll = 0;
                selected = -1;
                rebuild();
                return true;
            }
            ty += 16;
        }
        // 列表点击：单击选中，**双击进详情页**（玩家要求）
        int lx = x + 8 + 78;
        int lw = w - 78 - 16;
        if (mx >= lx && mx <= lx + lw && my >= listTop() && my <= listBottom()) {
            int idx = scroll + (int) ((my - listTop()) / rowH());
            if (idx >= 0 && idx < keys.size()) {
                long now = System.currentTimeMillis();
                boolean dbl = (now - lastClickAt) < 400L
                        && Math.abs(mx - lastClickX) < 6.0D
                        && Math.abs(my - lastClickY) < 6.0D;
                lastClickAt = now;
                lastClickX = mx;
                lastClickY = my;
                if (dbl && selected == idx) {
                    detailOpen = !detailOpen;      // 再双击一次返回
                } else {
                    selected = idx;
                    detailOpen = false;
                }
                return true;
            }
        }
        // 详情页：点空白处返回
        if (detailOpen) {
            detailOpen = false;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, keys.size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta) * 2));
        return true;
    }

    /** 手动重试：重新读数据 + 清搜索 + 重建列表。 */
    private void retry() {
        load();
        query = "";
        if (search != null) {
            search.setValue("");
        }
        scroll = 0;
        selected = -1;
        rebuild();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // 详情页里 Esc 先返回列表（再按一次才关书）
        if (key == 256 && detailOpen) {
            detailOpen = false;
            return true;
        }
        if (key == 82) {    // R = 重新加载
            retry();
            return true;
        }
        if (key == 264) {   // 下
            scroll = Math.min(Math.max(0, keys.size() - visibleRows()), scroll + 1);
            return true;
        }
        if (key == 265) {   // 上
            scroll = Math.max(0, scroll - 1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
