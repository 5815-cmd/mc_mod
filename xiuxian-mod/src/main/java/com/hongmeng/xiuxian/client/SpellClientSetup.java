package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.network.TechniqueActionPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 法术系统的客户端初始化：法术栏按键、4 个主动槽按键、蓄力条。
 *
 * <p><b>为什么单独开一个类而不是塞进 {@code ClientSetup}：</b>
 * 御剑飞行那边同时在改 {@code ClientSetup}（按键注册 + 客户端 tick），
 * 法术这边自己注册自己的一套，两边不会互相回退。
 * Forge 允许同一个事件有多个监听器，多一份 {@code RegisterKeyMappingsEvent}
 * 与 {@code RegisterGuiOverlaysEvent} 的订阅是完全正常的。</p>
 *
 * <h3>按键选择（按当前仓库里已被占用的键避让）</h3>
 * <pre>
 *   O           法术栏                —— 需求指定
 *   R/B/J/G/K   修行面板/破境/任务/道宫/坊市（既有，未动）
 *   V/C         施展/切换功法（既有，未动）
 *   Z X C V     主动槽 1~4            —— 需求指定的默认值
 * </pre>
 *
 * <p><b>注意 V 与 C：</b>既有的「施展当前功法」就是 V、「切换功法」是 C，
 * 与主动槽 4 / 主动槽 3 撞了。撞键时原版取<b>后注册</b>的那个，
 * 所以实际效果是 V = 主动槽 4、C = 主动槽 3（法术栏的路线），
 * 而"施展当前选中功法"退化为需要到设置里换键。
 * 需求里"主动槽用 Z X C V"是明确指定的默认值，所以保留；
 * 若要彻底避免撞键，把 {@link #ACTIVE_SLOT_KEYS} 改成
 * {@code GLFW_KEY_F / GLFW_KEY_G / GLFW_KEY_H / GLFW_KEY_N} 即可（一个常量的事）。</p>
 *
 * <p>所有按键的 lang key 都在 {@code zh_cn.json / en_us.json} 里补齐了
 * （{@code key.hongmeng.*} 与既有的 {@code key.categories.hongmeng}）——
 * 少一个键在设置里就是隐形的，这个坑踩过一次。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SpellClientSetup {

    /** 法术栏（O）。 */
    public static final KeyMapping SPELL_BOOK_KEY = new KeyMapping(
            "key.hongmeng.spell_book",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.hongmeng");

    /** 4 个主动槽的默认键：Z X C V。 */
    public static final int[] ACTIVE_SLOT_KEYS = {
            GLFW.GLFW_KEY_Z,
            GLFW.GLFW_KEY_X,
            GLFW.GLFW_KEY_C,
            GLFW.GLFW_KEY_V
    };

    /** 主动槽按键（1~4 号）。 */
    public static final KeyMapping[] ACTIVE_SLOT_MAPPINGS = new KeyMapping[ACTIVE_SLOT_KEYS.length];

    static {
        for (int i = 0; i < ACTIVE_SLOT_KEYS.length; i++) {
            ACTIVE_SLOT_MAPPINGS[i] = new KeyMapping(
                    "key.hongmeng.spell_slot_" + (i + 1),
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    ACTIVE_SLOT_KEYS[i],
                    "key.categories.hongmeng");
        }
    }

    private SpellClientSetup() {
    }

    // ------------------------------------------------------------------
    // 注册（MOD 总线）
    // ------------------------------------------------------------------

    @Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(SPELL_BOOK_KEY);
            for (KeyMapping mapping : ACTIVE_SLOT_MAPPINGS) {
                event.register(mapping);
            }
        }

        /**
         * 蓄力条**不走 {@code RegisterGuiOverlaysEvent}**，而是挂到事件总线上。
         *
         * <p>理由和 {@code HealthBarOverlay} 完全一样：{@code CastBarOverlay} 用的是
         * {@code RenderGuiEvent.Post}，它从事件里拿 {@code GuiGraphics}；
         * 而 {@code IGuiOverlay.render} 的签名是
         * {@code (ForgeGui, GuiGraphics, partialTick, w, h)}，两者对不上。
         * 硬套那个接口只会得到一个编译错误。</p>
         *
         * <p>{@code RenderGuiEvent.Post} 在**所有**覆盖层之后必定触发，
         * 不受任何取消影响（原版血条被取消过一次，踩过这个坑），
         * 所以准星下方的进度条不会被原版 UI 盖住。</p>
         */
        @SubscribeEvent
        public static void onClientSetup(
                net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            MinecraftForge.EVENT_BUS.register(new CastBarOverlay());
        }
    }

    // ------------------------------------------------------------------
    // 客户端 tick（FORGE 总线）
    // ------------------------------------------------------------------

    /** 挂到 Forge 总线（由 {@code HongmengMod} 构造或客户端初始化调用一次）。 */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ForgeBus());
    }

    public static final class ForgeBus {

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();

            // 法术栏：屏幕开着的时候也要能按 O 关闭（所以放在 screen 判断之前）
            while (SPELL_BOOK_KEY.consumeClick()) {
                if (mc.screen instanceof SpellBookScreen) {
                    mc.setScreen(null);
                } else if (mc.screen == null) {
                    mc.setScreen(new SpellBookScreen());
                }
            }

            if (mc.player == null || mc.screen != null) {
                return;
            }

            // 主动槽 1~4
            for (int i = 0; i < ACTIVE_SLOT_MAPPINGS.length; i++) {
                while (ACTIVE_SLOT_MAPPINGS[i].consumeClick()) {
                    TechniqueActionPacket.castActiveSlot(i);
                }
            }
        }
    }
}
