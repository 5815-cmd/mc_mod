package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.technique.Technique;
import com.hongmeng.xiuxian.technique.TechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 左下角的<b>法术槽 HUD</b>：常驻显示 Z X C V 装了什么。
 *
 * <p>玩家反馈「还是不能选法术」——按键链路查过是通的，所以真正的问题是
 * <b>玩家看不到槽里有没有东西</b>（法术栏界面里装了、关掉就忘了，或者
 * {@code /learnall} 之后自动装配失败也无从察觉）。</p>
 *
 * <p>这个 HUD 把四格直接摊在左下角：装了什么一眼可见，空槽明确写「未装配」，
 * 于是"按了没反应"这件事可以当场自查。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SpellSlotHud {

    /** 与 {@code SpellClientSetup.ACTIVE_SLOT_KEYS} 一致：Z X C V。 */
    private static final String[] KEY_LABELS = {"Z", "X", "C", "V"};

    /** PERF(C): 固定标题只建一次 Component（原来每帧 new 一个）。 */
    private static final Component TITLE = Component.literal("§b§l法术槽");

    /**
     * PERF(C): 四个槽位的文字 — 按 Technique 引用缓存。
     *
     * <p>原来每帧都要拼 4 个字符串 + new 4 个 Component；槽位内容只在装配变化时变，
     * 所以只有 Technique 引用变化（或首次渲染）时才重建。</p>
     */
    private final Technique[] cachedTech = new Technique[TechniqueData.ACTIVE_SLOTS];
    private final Component[] cachedText = new Component[TechniqueData.ACTIVE_SLOTS];

    /** 供事件总线实例化（必须是 public，否则 ClientSetup 里 new 不出来）。 */
    public SpellSlotHud() {
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        var data = com.hongmeng.xiuxian.network.ClientCultivationCache.get();
        if (data == null || !data.isAwakened()) {
            return;
        }
        TechniqueData tech = data.getTechniques();
        var g = event.getGuiGraphics();
        var font = mc.font;

        int lines = TechniqueData.ACTIVE_SLOTS + 1;
        int lineH = 10;
        int x = 6;
        int bottom = mc.getWindow().getGuiScaledHeight() - 4;
        int y = bottom - lines * lineH;

        // 半透明底，保证在亮地形上也看得清
        int w = 96;
        g.fill(x - 3, y - 3, x + w, bottom, 0x90000000);

        g.drawString(font, TITLE, x, y, 0xFFFFFF, false);
        y += lineH;
        for (int i = 0; i < TechniqueData.ACTIVE_SLOTS; i++) {
            Technique t = tech.activeAt(i);
            // PERF(C): 只有槽位内容变化时才重建字符串/Component（原来每帧 4 次）
            if (cachedText[i] == null || cachedTech[i] != t) {
                cachedTech[i] = t;
                String label = KEY_LABELS[i];
                String text = t == null
                        ? "§8" + label + " ——（未装配）"
                        : "§e" + label + " §f" + t.cn();
                cachedText[i] = Component.literal(text);
            }
            g.drawString(font, cachedText[i], x, y, 0xFFFFFF, false);
            y += lineH;
        }
    }
}
