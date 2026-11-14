package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.combat.CombatFeel;
import com.hongmeng.xiuxian.network.CombatFeedbackPacket;
import com.hongmeng.xiuxian.network.ClientCastCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 战斗手感的客户端总入口：**所有客户端事件只挂在这一个类上**。
 *
 * <h2>为什么单独开一个类（而不是塞进 {@code ClientSetup}）</h2>
 *
 * <p>和法术栏那边的理由一样：{@code ClientSetup} / {@code ClientEvents} 有别的代理在改，
 * 自己注册自己的一套就不会互相回退。用
 * {@code @Mod.EventBusSubscriber(bus = FORGE, value = Dist.CLIENT)} 自动注册，
 * <b>连 {@code HongmengMod} 都不用碰</b>（那边只加了一行客户端配置注册）。</p>
 *
 * <h2>挂的钩子与各自的理由</h2>
 *
 * <pre>
 *   ComputeCameraAngles     屏幕震动（规格 §二 指定的钩子）
 *   ComputeFovModifierEvent FOV 冲击（规格第 7 条）
 *   RenderGuiEvent.Post     压迫感 vignette / 天空变暗 / 登场横幅 / 领域一闪
 *                           —— 它一定在所有原版 HUD 之后触发，不会被血条盖住
 *   RenderLevelStageEvent   伤害飘字（世界坐标 billboard 文字，AFTER_PARTICLES）
 *   ClientTickEvent         推进所有计时器 + 本地推导（压迫感扫描、领域激活跳变）
 *   ScreenEvent.*           R 面板上的"屏幕震动"开关按钮
 * </pre>
 *
 * <h2>本地推导优先</h2>
 *
 * <p>能本地算的绝不发包：<b>压迫感</b>（怪物境界早就同步了）、<b>领域展开</b>
 * （领域状态也同步了）、<b>法术释放的 FOV 冲击</b>（蓄力进度客户端自己有）
 * 都是纯本地。<b>震动</b>只收"我打中了/我被打中了"这种<b>事实</b>包，
 * 而且只发给当事人——别人的命中不会震你的屏。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class CombatFeelClient {

    /** 上一 tick 的蓄力进度（用来发现"蓄满了 → 放出去了"这个瞬间）。 */
    private static float lastCastProgress = 0.0F;
    private static boolean wasCasting = false;

    private CombatFeelClient() {
    }

    // ------------------------------------------------------------------
    // 包处理
    // ------------------------------------------------------------------

    /** 由 {@code CombatFeedbackPacket} 调。 */
    public static void onPacket(CombatFeedbackPacket packet) {
        if (packet == null) {
            return;
        }
        switch (packet.kind()) {
            case CombatFeedbackPacket.KIND_IMPACT -> {
                // 玩家 2026-11-02：除 Boss 召唤/登场与自己挨打外，其他镜头反馈全关。
                // 打中敌人仍保留伤害飘字 / 命中白闪 / 顿帧，只是不再推镜头。
                float fraction = (float) packet.fraction();
                DamageNumbers.add(packet.entityId(), packet.x(), packet.y(), packet.z(),
                        packet.amount(), fraction, false);
                extendHitFlash(packet.entityId(), fraction);
            }
            case CombatFeedbackPacket.KIND_TAKEN -> {
                float fraction = (float) packet.fraction();
                CombatFeel.Shake tier = CombatFeel.shakeForDamage(fraction);
                // 自己挨打保留反馈，但镜头强度降到原来的约 36%（1.25 → 0.45），
                // 避免挨一下就被镜头甩得很晕。
                CombatShake.punch(tier, CombatFeel.shakeStrengthMultiplier(fraction) * 0.45D);
                DamageNumbers.add(-1, packet.x(), packet.y(), packet.z(),
                        packet.amount(), fraction, true);
            }
            case CombatFeedbackPacket.KIND_BOSS_ENTRANCE -> {
                CombatShake.punch(CombatFeel.Shake.CATASTROPHIC, 1.0D);
                CombatShake.punchFov(0.12F);
                BossEntranceOverlay.trigger(packet.label(), packet.color());
            }
            default -> {
                // 未知类型：忽略（协议向前兼容）
            }
        }
    }

    // ------------------------------------------------------------------
    // 命中闪白（客户端才是真正生效的那一半）
    // ------------------------------------------------------------------

    /**
     * 把受击者的红闪从原版的 10 tick 拉长到 10~18 tick。
     *
     * <h3>为什么必须在客户端做</h3>
     *
     * <p>1.19.4 起，客户端的受击闪红是<b>由 {@code ClientboundHurtAnimationPacket}
     * 驱动的</b>：客户端收到包后自己调 {@code LivingEntity.animateHurt(...)}，
     * 而那个方法把 {@code hurtDuration/hurtTime} 写死成 10。</p>
     *
     * <p>换句话说：<b>服务端改这两个字段对画面没有任何影响</b>（服务端的 hurtTime
     * 既不参与渲染也不参与 AI）。真正生效的做法是在客户端拿到"我打中了"这条事实之后，
     * 对<b>本地那具实体</b>再拧一次这两个字段——它们本来就是 public 的，
     * 而客户端实体的 tick 会自己把它们递减下去。</p>
     *
     * <p>没有这个的话，"命中闪白加强"就只是一句写在注释里的愿望。</p>
     */
    private static void extendHitFlash(int entityId, double fraction) {
        if (entityId < 0 || fraction < CombatFeel.HITSTOP_THRESHOLD) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        var entity = mc.level.getEntity(entityId);
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) {
            return;
        }
        float t = CombatFeel.clamp01((float) (fraction / CombatFeel.KNOCKBACK_FULL_FRACTION));
        int extend = 10 + Math.round(8.0F * t);
        // 先让它正常"挨打"一次（对方向 / 动画状态与客户端包一致），再拉长时间
        // **玩家不延长受伤闪白**：原版的受伤红屏就是靠 hurtTime 驱动的，
        // 延长它会让红屏持续更久、看起来巨大并挡住视野（玩家反馈过）。
        if (living instanceof net.minecraft.world.entity.player.Player) {
            return;
        }
        living.animateHurt(0.0F);
        living.hurtDuration = Math.max(living.hurtDuration, extend);
        living.hurtTime = Math.max(living.hurtTime, extend);
    }

    // ------------------------------------------------------------------
    // 相机 / FOV
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CombatShake.applyCamera(event);
        // 演出镜头：把玩家朝向拧向「镜头看向的点」（推轨跟随）。
        // 必须**后于** CombatShake —— 演出期间镜头是权威的，震动让位。
        com.hongmeng.xiuxian.client.CinematicCamera.onCameraAngles(event);
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        CombatShake.applyFov(event);
        // 演出期间按关键帧收窄视场角（长焦压缩感）
        com.hongmeng.xiuxian.client.CinematicCamera.onFov(event);
    }

    /**
     * 真实世界：<b>雾色</b>变化（玩家要求「所有维度发生变化」）。
     *
     * <p>Forge 1.20.1 只有雾相关的两个钩子能改环境外观（已解析 forge jar 确认），
     * 天空色没有钩子 —— 所以"维度变了"是靠雾色 + 雾距 + 屏幕叠加层传达的。</p>
     */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        com.hongmeng.xiuxian.client.RealWorldAtmosphere.onFogColor(event);
    }

    /** 真实世界：<b>雾距</b>收近（只动地形雾，水下/失明的雾不碰）。 */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        com.hongmeng.xiuxian.client.RealWorldAtmosphere.onRenderFog(event);
    }

    // ------------------------------------------------------------------
    // 每 tick
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 真世界的环境渐变（雾色转暗红，约 8 秒升满）
        com.hongmeng.xiuxian.client.RealWorldClientFx.tick();
        // 背景音乐：按场景切 4 首自制 BGM
        com.hongmeng.xiuxian.client.MusicDirector.tick();
        // 施法拉远视野的倒计时
        com.hongmeng.xiuxian.client.CastZoom.tick();
        // Boss 战环境压迫的强度平滑
        com.hongmeng.xiuxian.client.BossFightAtmosphere.tick();
        // 全屏闪烁的帧驱动
        com.hongmeng.xiuxian.client.EvilFlashOverlay.tick();
        // 坠落穿越
        com.hongmeng.xiuxian.client.EvilFallFx.tick();
        // 法阵寿命推进（唯一需要每 tick 做的维护）
        if (event.phase == TickEvent.Phase.END) {
            MagicCircles.tick();
        com.hongmeng.xiuxian.client.ClientSpellFx.tick();
        com.hongmeng.xiuxian.client.ClientBossEntrance.tick();
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            // 退回主菜单：把所有状态清干净，免得下次进世界第一帧还在震
            CombatShake.clear();
            DamageNumbers.clear();
            BossEntranceOverlay.clear();
            DomainOpeningVfx.clear();
            lastCastProgress = 0.0F;
            wasCasting = false;
            return;
        }
        CombatShake.tick();
        // 演出镜头：超时兜底（服务端会再发 stop，这里是防丢包的保险）
        com.hongmeng.xiuxian.client.Cinematic.tick();
        DamageNumbers.tick();
        BossEntranceOverlay.tick();
        OppressionOverlay.tick();
        DomainOpeningVfx.tick();
        detectSpellRelease();
    }

    /**
     * 法术"放出去了"的那个瞬间（纯本地推导）。
     *
     * <p>蓄力进度客户端手边就有（{@code ClientCastCache}：起手刻 + 总时长，
     * 服务端只在起手/中断/完成时发包）。所以"蓄满 → 清空"= 释放成功，
     * 这时给一次 FOV 冲击与轻微震动——<b>规格 §四 的"承（释放）"</b>。
     * 中断（进度没到就清空）不给，那是"憋回去了"。</p>
     */
    private static void detectSpellRelease() {
        // 蓄力中：持续生成小法阵（竖直、绽放）
        // 蓄力：法阵**随进度逐层叠加**（玩家要求"生成叠加变化"），
        // 而且每个法术用的图案与配色都由它自己的名字派生 —— 52 个法术互不相同。
        if (ClientCastCache.isCasting()
                && net.minecraft.client.Minecraft.getInstance().player != null
                && net.minecraft.client.Minecraft.getInstance().level != null
                && net.minecraft.client.Minecraft.getInstance().level.getGameTime() % 6L == 0L) {
            com.hongmeng.xiuxian.technique.Technique casting2 = ClientCastCache.technique();
            if (casting2 != null) {
                MagicCircles.spellChargeTick(
                        net.minecraft.client.Minecraft.getInstance().player.position(),
                        casting2.element(), casting2.name(),
                        casting2.kind().chargeGrandeur() >= 1.0F ? 14.0F : 8.0F,
                        ClientCastCache.progress());
            }
        }
        boolean casting = ClientCastCache.isCasting();
        float progress = casting ? ClientCastCache.progress() : 0.0F;
        if (wasCasting && !casting && lastCastProgress >= 0.85F) {
            // 玩家 2026-11-02：法术释放不再推镜头（保留法阵与音效）。
        }
        wasCasting = casting;
        lastCastProgress = progress;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    /** 覆盖层：压迫感 vignette / 天空变暗 + 横幅 / 领域一闪。 */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            // 开着界面（背包 / 面板 / 聊天）时不画：那些是"世界里的气氛"，
            // 糊在菜单上只会碍事
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        var graphics = event.getGuiGraphics();
        OppressionOverlay.render(graphics, width, height);
        DomainOpeningVfx.render(graphics, width, height);
        BossEntranceOverlay.render(graphics, width, height);
    }

    /** 世界坐标的伤害飘字。 */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        // 注意：**不要**把 mc.renderBuffers().bufferSource() 传进去。
        // DamageNumbers 自己开缓冲区——在关卡共享缓冲区上 endBatch()
        // 会让 renderChunkLayer 读到 null shader 而崩客户端（已踩过一次）。
        DamageNumbers.render(pose, camera);
        // 法阵：贴图几何层（自发光）+ 粒子层（在 tick 里布点）叠加
        MagicCircles.render(pose, camera, event.getPartialTick());
        // 法术**贴图**特效层（与粒子层叠加 —— 玩家要的是两者配合，不是纯贴图）：
        // 光柱、冲击环、穹顶、爆闪、巨剑、陨石、虚空裂缝、雪花、花瓣旋涡、符文环。
        com.hongmeng.xiuxian.client.SpellFxRenderer.render(pose, camera,
                event.getPartialTick());
    }

    // ------------------------------------------------------------------
    // R 面板上的震动开关
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        CombatFeelPanel.render(event.getScreen(), event.getGuiGraphics(),
                (int) event.getMouseX(), (int) event.getMouseY());
    }

    @SubscribeEvent
    public static void onScreenMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (CombatFeelPanel.mouseClicked(event.getScreen(),
                event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }
}
