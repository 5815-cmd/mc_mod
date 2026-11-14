package com.hongmeng.xiuxian;

import com.hongmeng.xiuxian.core.MeditationTracker;
import com.hongmeng.xiuxian.network.ClientBreakthroughFx;
import com.hongmeng.xiuxian.network.ClientPainState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端初始化：按键、HUD、痛觉遮罩、突破演出。
 */
public final class ClientSetup {

    public static final KeyMapping OPEN_PANEL = new KeyMapping(
            "key.hongmeng.open_panel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.hongmeng");

    public static final KeyMapping BREAKTHROUGH_KEY = new KeyMapping(
            "key.hongmeng.breakthrough",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.hongmeng");

    /** 任务面板。 */
    public static final KeyMapping QUEST_KEY = new KeyMapping(
            "key.hongmeng.quests",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.hongmeng");

    /** 道宫界面（道印之后才用得上）。 */
    public static final KeyMapping DOMAIN_KEY = new KeyMapping(
            "key.hongmeng.domain",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.hongmeng");
    /**
     * 领域开关：**数字 0**（玩家要求「领域按数字0可以打开或者是关闭」）。
     *
     * <p>与 {@link #DOMAIN_KEY}（G，打开道宫界面）是两件事：
     * 这个键直接切换本命领域的开/关，不用进界面。</p>
     *
     * <p>原版 0 键是切快捷栏第 9 格，两者会同时生效——这是有意的取舍，
     * 玩家明确要求用 0。真要避让可以到设置里改键。</p>
     */
    public static final KeyMapping DOMAIN_TOGGLE_KEY = new KeyMapping(
            "key.hongmeng.domain_toggle",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_0,
            "key.categories.hongmeng");

    /** 坊市（商店）。 */
    public static final KeyMapping SHOP_KEY = new KeyMapping(
            "key.hongmeng.shop",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.hongmeng");

    /** 施展当前功法。 */
    public static final KeyMapping CAST_KEY = new KeyMapping(
            "key.hongmeng.cast",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            "key.categories.hongmeng");

    /** 切换功法。 */
    public static final KeyMapping CYCLE_KEY = new KeyMapping(
            "key.hongmeng.cycle_technique",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "key.categories.hongmeng");

    /**
     * 飞剑剑阵：展开 / 收回 —— <b>N</b>。
     *
     * <p>⚠ 原来用 V，但 V 是**法术槽 4**（Z X C V）—— 玩家明确要求
     * 「能不能不要按键冲突」。N / M / L 三个键当前全空闲。</p>
     */
    public static final KeyMapping SWORD_FORMATION_KEY = new KeyMapping(
            "key.hongmeng.sword_formation",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.hongmeng");

    /** 切换剑阵阵型（Ctrl+R）。 */
    public static final KeyMapping SWORD_PATTERN_KEY = new KeyMapping(
            "key.hongmeng.sword_pattern",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.hongmeng");

    /** 锁定准心目标（Ctrl+H，对应御剑工艺的 H）。 */
    public static final KeyMapping SWORD_LOCK_KEY = new KeyMapping(
            "key.hongmeng.sword_lock",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.hongmeng");

    /**
     * 灵兽名册（潜行右键灵兽袋也能开，这个键是给"手边没袋子但想看"准备的）。
     *
     * <p>默认键 {@code U}：既不与既有的 R/B/J/G/K/O/Z/X/C/V/T/Y/H 撞车，
     * 也不占原版常用键。规格第五节第 6 条踩过两次的坑——
     * {@code key.hongmeng.spirit_beast_roster} 与 {@code key.categories.hongmeng}
     * 两条 lang <b>都已经补齐</b>（zh_cn 与 en_us 各一份）。</p>
     */
    public static final KeyMapping BEAST_ROSTER_KEY = new KeyMapping(
            "key.hongmeng.spirit_beast_roster",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.hongmeng");

private ClientSetup() {
    }

    @Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_PANEL);
        // Boss 阶段形态渲染（体型放大；缩放带脚底补偿，避免沉进地里）
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.BossPhaseRenderer.class);
            event.register(BREAKTHROUGH_KEY);
            event.register(QUEST_KEY);
            event.register(DOMAIN_KEY);
            event.register(DOMAIN_TOGGLE_KEY);
            event.register(SHOP_KEY);
            event.register(CAST_KEY);
            event.register(CYCLE_KEY);
            event.register(BEAST_ROSTER_KEY);
            event.register(SWORD_FORMATION_KEY);
            event.register(SWORD_PATTERN_KEY);
            event.register(SWORD_LOCK_KEY);
        }

        /**
         * 资源重载时加载修行录数据（玩家反馈"打开书是空的"——
         * 根因是在界面里临时读、可能赶在资源就绪之前）。
         */
        @SubscribeEvent
        public static void onAddReloadListeners(
                net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new com.hongmeng.xiuxian.client.GuideReloadListener());
        }

        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // 演出黑边 + 标题（电影感；必须在所有叠加层之上，所以用最上层）
        event.registerAboveAll("cinematic_bars",
                (gui, gfx, partialTick, w, h) -> com.hongmeng.xiuxian.client.CinematicOverlay
                        .render(gfx, partialTick));
            event.registerAboveAll("hongmeng_hud", new CultivationHud());
            event.registerAboveAll("hongmeng_quest_tracker",
                    new com.hongmeng.xiuxian.quest.QuestTrackerHud());
        }

        @SubscribeEvent
        public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            // 叛徒执事：第二轮补完的自建人形骨架（鬼面罩/教徽/锁链腕环，三派换色）
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.GUILTY_SECT_DISCIPLE.get(),
                    com.hongmeng.xiuxian.client.TraitorDiscipleRenderer::new);

            // ---- 五阶野怪 ----
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.EVIL_GAZER.get(),
                    com.hongmeng.xiuxian.client.EvilGazerRenderer::new);
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.BLAZE_EMPEROR.get(),
                    com.hongmeng.xiuxian.client.BlazeEmperorRenderer::new);
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.VOID_DRAGON_EMPEROR.get(),
                    // 独立的龙帝渲染器（复制蛟龙骨架 + 适配 boss API）
                    com.hongmeng.xiuxian.client.VoidDragonEmperorRenderer::new);
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.SPIRIT_WOLF.get(),
                    com.hongmeng.xiuxian.client.SpiritWolfRenderer::new);
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.BLOOD_DEMON_CULTIVATOR.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .BLOOD_DEMON_CULTIVATOR));
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities
                            .TRIBULATION_ASH_CULTIVATOR.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .TRIBULATION_ASH_CULTIVATOR));
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.PUPPET_CAPTAIN.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .PUPPET_CAPTAIN));
            // ---- 祭坛首领：四个副本各一套部件组合 ----
        event.registerEntityRenderer(
                com.hongmeng.xiuxian.entity.ModEntities.EPOCH_BOSS.get(),
                // 傀儡祖师：**独立骨架**（多臂机关体 + 提线人偶，不是人形加部件）
                ctx -> new com.hongmeng.xiuxian.client.PuppetMasterRenderer(ctx, "boss_puppet_master"));
        event.registerEntityRenderer(
                com.hongmeng.xiuxian.entity.ModEntities.BEAST_BOSS.get(),
                // 万兽之主：**独立骨架**（四足兽形，不是人形）
                ctx -> new com.hongmeng.xiuxian.client.BeastLordRenderer(ctx, "boss_beast_lord"));
        event.registerEntityRenderer(
                com.hongmeng.xiuxian.entity.ModEntities.DEMON_BOSS.get(),
                // 血魔老祖：**独立骨架**（三头六臂 + 三腿 + 骨刺脊 + 会脉动的血色晶核）
                ctx -> new com.hongmeng.xiuxian.client.BloodDemonRenderer(ctx, "boss_demon_elder"));
        event.registerEntityRenderer(
                com.hongmeng.xiuxian.entity.ModEntities.VOID_BOSS.get(),
                // 虚空道主：**独立骨架**（不是人形加部件）——
                // 浮空体（无腿）、四条臂、六片翼、独眼颅骨、会自转的虚空核心
                // ⚠ 必须用 **128×128 的新图** boss_void_lord（旧图 boss_void 是 64×64 的，
                //   配这张骨架会严重错位 —— 这个坑真的踩过）
                ctx -> new com.hongmeng.xiuxian.client.VoidLordRenderer(ctx, "boss_void_lord"));
        event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.DEMON_GENERAL.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .DEMON_GENERAL));
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.VOID_OVERLORD.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .VOID_OVERLORD));

            // ---- 第二批妖兽：四种各自带独立模型（剪影互不相同） ----
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.FLAME_APE.get(),
                    com.hongmeng.xiuxian.client.FlameApeRenderer::new);
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.THUNDER_EAGLE.get(),
                    com.hongmeng.xiuxian.client.ThunderEagleRenderer::new);
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.STONE_GOLEM.get(),
                    com.hongmeng.xiuxian.client.StoneGolemRenderer::new);
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.VOID_SERPENT.get(),
                    com.hongmeng.xiuxian.client.VoidSerpentRenderer::new);

            // ---- 第三批妖兽：遁地伏击 + 远程喷毒（各带一套自建模型） ----
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.BURROW_MARTEN.get(),
                    com.hongmeng.xiuxian.client.BurrowMartenRenderer::new);
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.VENOM_TOAD.get(),
                    com.hongmeng.xiuxian.client.VenomToadRenderer::new);
            // 毒涎：自建小模型（暗红蟾涎球：主球 + 4 瘤块 + 亮毒核，自转 + 飞行鼓胀）
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.VENOM_SPIT.get(),
                    com.hongmeng.xiuxian.client.VenomSpitRenderer::new);

            // ---- 第四批：人形 4 只（共用 CultivationHumanoidRenderer，只换 Variant） ----
            //
            // 四只怪**一行模型代码都不写**：CultivationHumanoidRenderer 的每个 Variant
            // 就是"一套部件开关 + 一个体型"，贴图由实体自己按注册名取
            // （见 CultivationMonsterEntity.textureName()）。
            // 加一只新人形怪的标准动作因此是：加 Variant 常量 -> 加实体注册 -> 加一张 64×64 贴图。
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.DEMON_STEWARD.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .DEMON_STEWARD));
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.BLOOD_DEMON_ELDER.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .BLOOD_DEMON_ELDER));
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.VOID_WALKER.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .VOID_WALKER));
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.SWORD_TOMB_GUARDIAN.get(),
                    context -> new com.hongmeng.xiuxian.client.CultivationHumanoidRenderer(
                            context, com.hongmeng.xiuxian.client.CultivationHumanoidRenderer
                                    .SWORD_TOMB_GUARDIAN));

            // ---- 第四批：妖兽 3 只（全部复用既有骨架，只换贴图 / 变体） ----
            //
            // 赤炎狼：灵狼那套四足骨架 + 赤炎狼贴图（64×32，与原版狼同 UV）
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.CRIMSON_FLAME_WOLF.get(),
                    context -> new com.hongmeng.xiuxian.client.SpiritWolfRenderer(
                            context, "crimson_flame_wolf"));
            // 玄冰蟒：虚空蛟那套八节蛇身 + 玄冰蟒贴图（128×128，同一份 UV 布局）
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.FROST_PYTHON.get(),
                    context -> new com.hongmeng.xiuxian.client.VoidSerpentRenderer<>(
                            context, "frost_python"));
            // 雷纹鹰：雷鹰那套鹰形骨架 + **永远返回 false 的 airborne**
            // （雷纹鹰不会飞，模型因此走"收翅站姿"分支——这是模型泛型化的唯一用途）
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.STORM_EAGLE.get(),
                    context -> com.hongmeng.xiuxian.client.ThunderEagleRenderer
                            .shared(context, "storm_eagle", entity -> false));

            // ---- NPC：门派长老 / 炼丹师走独立模型（第二轮 NPC 身份化 A） ----
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.SECT_ELDER.get(),
                    com.hongmeng.xiuxian.client.SectElderRenderer::new);
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.ALCHEMY_MASTER.get(),
                    com.hongmeng.xiuxian.client.AlchemyMasterRenderer::new);
            // 云游商人（试稿③）：独立模型/渲染器（斗笠 + 竹扁担货担 + 褡裢 + 竹杖），
            // 不再走 CultivationNpcRenderer 的共用骨架；其余 6 位 NPC 不变。
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.WANDERING_MERCHANT.get(),
                    com.hongmeng.xiuxian.client.WanderingMerchantRenderer::new);

            // ---- 第四批 NPC：丹药商人走独立模型；炼器师 / 秘境向导 / 悬赏牌看板见下方二轮注册 ----
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.PILL_MERCHANT.get(),
                    com.hongmeng.xiuxian.client.PillMerchantRenderer::new);
            // 第二轮 NPC 身份化 B：炼器师改独立模型（皮护肩 + 背后两柄剑胚 + 铁锤）
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.ARTIFACT_REFINER.get(),
                    com.hongmeng.xiuxian.client.ArtifactRefinerRenderer::new);
            // 第二轮 NPC 身份化 B：秘境向导改独立模型（路引竹竿 + 背后卷轴筒 + 窄檐斗笠）
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.REALM_GUIDE.get(),
                    com.hongmeng.xiuxian.client.RealmGuideRenderer::new);
            // 第二轮 NPC 身份化 B：悬赏牌看板改**非人形**木牌模型（木柱+牌身+瓦檐+告示+铜铃）
            event.registerEntityRenderer(com.hongmeng.xiuxian.entity.ModEntities.BOUNTY_BOARD.get(),
                    com.hongmeng.xiuxian.client.BountyBoardRenderer::new);

            // ---- 灵兽（两套自建模型 + 一枚弹体） ----
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.CONTRACT_EAGLE.get(),
                    com.hongmeng.xiuxian.client.ContractEagleRenderer::new);
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.JIAOLONG.get(),
                    com.hongmeng.xiuxian.client.JiaolongRenderer::new);
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.CONTRACT_WOLF.get(),
                    com.hongmeng.xiuxian.client.ContractWolfRenderer::new);
            // 蛟龙之息：自建小模型（青绿灵叶：叶面 + 中脉 + 前后尖 + 叶柄 + 灵光）
            event.registerEntityRenderer(
                    com.hongmeng.xiuxian.entity.ModEntities.JIAOLONG_SPIT.get(),
                    com.hongmeng.xiuxian.client.JiaolongSpitRenderer::new);
        }



        /**
         * <b>把 GeckoLib 渲染器挂到物品上</b>（1.20.1 的标准做法）。
         *
         * <p>Forge 在 1.20.1 里推荐用 {@code RegisterClientExtensionsEvent}
         * 注册 {@code IClientItemExtensions}，而不是覆盖 {@code Item#initializeClient}
         * ——后者在部分版本上不会触发，物品会退化成"缺失模型"的紫黑方块。</p>
         */


        /** 给玩家渲染器挂上护甲外挂件图层（default / slim 两种皮肤都要）。 */
        @SubscribeEvent
        public static void onAddLayers(
                net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {
            // getSkin 返回的是 EntityRenderer（编译期只看得到基类），
            // 运行时它一定是 PlayerRenderer —— 用原始类型绕过泛型。
            for (String skin : event.getSkins()) {
                var r = event.getSkin(skin);
                if (r instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer) {
                    @SuppressWarnings("unchecked")
                    net.minecraft.client.renderer.entity.player.PlayerRenderer pr =
                            (net.minecraft.client.renderer.entity.player.PlayerRenderer) r;
                    pr.addLayer(new com.hongmeng.xiuxian.client.ArmorTrimLayer(pr));
                }
            }
        }

        /** 自定义模型的自定义模型层：灵狼四足 + 修仙者人形，都要自己注册。 */
        @SubscribeEvent
        public static void onRegisterLayerDefinitions(
                net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.SpiritWolfRenderer.LAYER,
                    com.hongmeng.xiuxian.client.SpiritWolfRenderer.WolfBody::createBodyLayer);
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.CultivationHumanoidModel.LAYER,
                    com.hongmeng.xiuxian.client.CultivationHumanoidModel::createBodyLayer);
            // ---- 祭坛首领：四个副本共用一个模型类，靠**部件组合**区分轮廓 ----
            //
            // 为什么不做四套完全独立的骨架：这个项目最早就强调过「不希望模型出错」，
            // 而新骨架最容易出的问题就是脚底坐标算错导致浮空/埋土。
            // 所以人形骨架原样复用（已被验证），在其上长出各自的兽征部件。
            // 虚空道主的**独立骨架**（浮空体：无腿 + 四臂 + 六翼 + 独眼）
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.VoidLordRenderer.LAYER,
                    com.hongmeng.xiuxian.client.VoidLordModel::createBodyLayer);
            // 血魔老祖的**独立骨架**（三头六臂 + 三腿 + 骨刺脊）
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.BloodDemonRenderer.LAYER,
                    com.hongmeng.xiuxian.client.BloodDemonModel::createBodyLayer);
            // 万兽之主的**独立骨架**（四足兽形：长躯干 + 四腿 + 兽首 + 背脊骨板 + 长尾）
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.BeastLordRenderer.LAYER,
                    com.hongmeng.xiuxian.client.BeastLordModel::createBodyLayer);
            // 傀儡祖师的**独立骨架**（多臂机关体：立柱 + 镂空柜体 + 齿轮环 + 提线杆 + 四悬臂）
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.PuppetMasterRenderer.LAYER,
                    com.hongmeng.xiuxian.client.PuppetMasterModel::createBodyLayer);
            // ---- 第二批妖兽的四套骨架 ----
            // 护甲外挂件图层（三层肩甲/腰甲/腿甲/披风，独立花纹贴图）
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.ArmorTrimLayer.LAYER,
                    com.hongmeng.xiuxian.client.ArmorTrimLayer::createBodyLayer);
            // 恶：三套独立骨架（手臂 / 巨颅 / 上半身），按阶段切换
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.EvilGazerRenderer.LAYER_ARM,
                    com.hongmeng.xiuxian.client.EvilArmModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.EvilGazerRenderer.LAYER_HEAD,
                    com.hongmeng.xiuxian.client.EvilHeadModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.EvilGazerRenderer.LAYER_UPPER,
                    com.hongmeng.xiuxian.client.EvilUpperModel::createBodyLayer);
            // ---- 两只维度 boss 的独立骨架（不复用任何现有模型）----
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.BlazeEmperorRenderer.LAYER,
                    com.hongmeng.xiuxian.client.BlazeEmperorModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.VoidDragonEmperorRenderer.LAYER,
                    com.hongmeng.xiuxian.client.VoidDragonEmperorModel::createBodyLayer);
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.FlameApeRenderer.LAYER,
                    com.hongmeng.xiuxian.client.FlameApeModel::createBodyLayer);
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.ThunderEagleRenderer.LAYER,
                    com.hongmeng.xiuxian.client.ThunderEagleRenderer.EagleModel::createBodyLayer);
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.StoneGolemRenderer.LAYER,
                    com.hongmeng.xiuxian.client.StoneGolemRenderer.GolemModel::createBodyLayer);
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.VoidSerpentRenderer.LAYER,
                    com.hongmeng.xiuxian.client.VoidSerpentRenderer.SerpentModel::createBodyLayer);
            // ---- 第三批妖兽的两套骨架 ----
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.BurrowMartenRenderer.LAYER,
                    com.hongmeng.xiuxian.client.BurrowMartenRenderer.MartenModel::createBodyLayer);
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.VenomToadRenderer.LAYER,
                    com.hongmeng.xiuxian.client.VenomToadRenderer.ToadModel::createBodyLayer);
            // ---- NPC 骨架 ----
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.CultivationNpcModel.LAYER,
                    com.hongmeng.xiuxian.client.CultivationNpcModel::createBodyLayer);
            // 云游商人（试稿③）的独立人形骨架（原版人形 + 斗笠/货担/褡裢/竹杖）
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.WanderingMerchantModel.LAYER,
                    com.hongmeng.xiuxian.client.WanderingMerchantModel::createBodyLayer);
            // 第二轮 NPC 身份化 B：炼器师 / 秘境向导（人形骨架）+ 悬赏牌看板（非人形木牌骨架）
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.ArtifactRefinerModel.LAYER,
                    com.hongmeng.xiuxian.client.ArtifactRefinerModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.RealmGuideModel.LAYER,
                    com.hongmeng.xiuxian.client.RealmGuideModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.BountyBoardModel.LAYER,
                    com.hongmeng.xiuxian.client.BountyBoardModel::createBodyLayer);
            // ---- 第二轮 NPC 身份化 A：门派长老 / 炼丹师 / 丹药商人 的独立人形骨架 ----
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.SectElderModel.LAYER,
                    com.hongmeng.xiuxian.client.SectElderModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.AlchemyMasterModel.LAYER,
                    com.hongmeng.xiuxian.client.AlchemyMasterModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.hongmeng.xiuxian.client.PillMerchantModel.LAYER,
                    com.hongmeng.xiuxian.client.PillMerchantModel::createBodyLayer);
            // ---- 灵兽骨架（雷鹰鸟形 / 蛟龙九节蛇身） ----
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.ContractEagleRenderer.LAYER,
                    com.hongmeng.xiuxian.client.ContractEagleRenderer.EagleModel::createBodyLayer);
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.JiaolongRenderer.LAYER,
                    com.hongmeng.xiuxian.client.JiaolongRenderer.JiaolongModel::createBodyLayer);
            // ---- 第二轮杂项补完：叛徒执事独立人形骨架 + 两枚弹体小模型 ----
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.TraitorDiscipleRenderer.LAYER,
                    com.hongmeng.xiuxian.client.TraitorDiscipleModel::createBodyLayer);
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.VenomSpitRenderer.LAYER,
                    com.hongmeng.xiuxian.client.VenomSpitModel::createBodyLayer);
            event.registerLayerDefinition(com.hongmeng.xiuxian.client.JiaolongSpitRenderer.LAYER,
                    com.hongmeng.xiuxian.client.JiaolongSpitModel::createBodyLayer);
        }
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ForgeBus());
        // 数值血条：挂在原版血条那一层之后
        MinecraftForge.EVENT_BUS.register(new com.hongmeng.xiuxian.client.HealthBarOverlay());
        // 怪物头顶血条 + 品阶外观（光环/护甲片/光晕）：走 RenderNameTagEvent
        MinecraftForge.EVENT_BUS.register(new com.hongmeng.xiuxian.client.MobHealthBarRenderer());
        // 灵兽骑乘时的空格回传（/hmbeast jump）
        MinecraftForge.EVENT_BUS.register(new com.hongmeng.xiuxian.client.ClientBeastInput());
        // 飞剑剑阵的键盘回传（V / Ctrl+R / Ctrl+H）
        MinecraftForge.EVENT_BUS.register(com.hongmeng.xiuxian.client.ClientSwordInput.class);
        // 客户端界面入口（副业界面的掉线清理）
        MinecraftForge.EVENT_BUS.register(com.hongmeng.xiuxian.client.ClientScreens.class);
        // 攻击动作渲染（第三人称整体姿态 + 第一人称手部挥砍）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.CombatAnimRenderer.class);
        // 真世界的环境表现（雾色转暗红 + 视野轻微压缩）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.RealWorldClientFx.class);
        // 怪物格挡的气盾（画在世界空间，挂在 RenderLevelStageEvent 上）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.MobShieldRenderer.class);
        // 真世界的天空穹顶（AFTER_SKY 阶段画渐变穹顶，不用 shader）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.RealWorldSkyRenderer.class);
        // 怪物/Boss 的挥击动作（挂在 RenderLivingEvent 上）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.MobCombatAnim.class);
        // 恶之祭坛界面（MenuScreens 必须在客户端初始化之后注册）
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> net.minecraft.client.gui.screens.MenuScreens.register(
                        com.hongmeng.xiuxian.ModEvil.EVIL_ALTAR_MENU.get(),
                        com.hongmeng.xiuxian.client.EvilAltarScreen::new));
        // 特效看门狗（换维度清空 / 满强度过久强制归零）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.FxWatchdog.class);
        // 施法时拉远视野
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.CastZoom.class);
        // 坠落穿越的相机
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.EvilFallCamera.class);
        // 全屏闪烁（恶降临 / 逃走）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.EvilFlashOverlay.class);
        // 天空裂口 + 巨眼（恶降临）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.EvilSkyRenderer.class);
        // Boss 战环境压迫（暗角 + 色调 + 呼吸脉动）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.BossFightAtmosphere.class);
        // Boss 压迫感光环（双环符文 + 环绕光点 + 脉动核心）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.BossAuraRenderer.class);
        // 旧的世界空间贴片已停用 —— 现在走真正的 3D 护甲模型
        // （保留类文件，方便以后对比/回退）
        // 手持武器的动态光效（呼吸微光 + 三点流光，第三人称可见）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.HeldWeaponGlow.class);
        // Boss 形态（二/三阶段的体型 + 魔角光环 + 翼 + 法阵）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.BossFormRenderer.class);
        // 雷劫天象（劫云实体化 + 落雷闪光 + 地面焦痕）
        MinecraftForge.EVENT_BUS.register(
                com.hongmeng.xiuxian.client.TribulationSkyRenderer.class);
        // 左下角法术槽 HUD：常驻显示 Z X C V 装了什么
        MinecraftForge.EVENT_BUS.register(
                new com.hongmeng.xiuxian.client.SpellSlotHud());
        // **法术按键（O 开法术栏 / Z X C V 放主动法术）**
        //
        // 这段原来漏了：`SpellClientSetup.register()` 全项目零调用点，
        // 所以它那个 `onClientTick` 从来没跑过 —— 玩家报的"ZXCV 不会释放法术"
        // 就是这个。补在这里，和其它客户端事件处理器并列。
        com.hongmeng.xiuxian.client.SpellClientSetup.register();
    }

    /** 客户端 tick：痛觉衰减、突破演出计时、按键处理。 */
    public static final class ForgeBus {

        /** 本次进世界是否已经弹过难度选择。 */
        private boolean difficultyPrompted = false;

        /** 上一 tick 玩家还在不在世界里（用来发现"退出世界"这件事）。 */
        private boolean hadPlayer = false;

        /** 上一次空格按下的时间（毫秒）；0 表示当前不在双击窗口里。 */
        private static long jumpTapAt = 0L;

        /** 这一次「起跳双击」是否已经发过御剑请求（避免三击重复触发）。 */
        private static boolean jumpTapConsumed = false;

        /** 双击窗口：400ms 内连按两次空格（原来 300ms，玩家反馈按不出来，放宽）。 */
        private static final long DOUBLE_TAP_MS = 400L;

        /** 进世界 / 离开世界时清掉双击状态，防止跨存档把上一次的按键算进来。 */
        private static void resetJumpTap() {
            jumpTapAt = 0L;
            jumpTapConsumed = false;
        }

        /**
         * 双击空格 = 御剑起飞（玩家反馈：「御剑飞行为何双击空格飞不起来」）。
         *
         * <p>为什么是「双击」而不是「按一下」：单次空格是原版的跳跃 / 上升，不能抢；
         * 而双击在 MC 里本来就是「起飞」的通用手势（创造模式双击空格飞行），
         * 玩家已经有肌肉记忆。</p>
         *
         * <p>三条硬约束：</p>
         * <ul>
         *   <li><b>不碰创造飞行</b>：这里只发请求包，不改 abilities ——
         *       客户端乱改会和创造模式打架（见 applyFlightPermission）；</li>
         *   <li><b>骑乘时不触发</b>：骑灵兽时空格是「上升 / 二段跳」，有自己的链路
         *       （ClientBeastInput），再叠一个御剑两边都会打架。</li>
         * </ul>
         *
         * <p>只在地面以外计数：站在地上按两下只是「跳两次」，不该起飞。</p>
         */
        private static void tickDoubleTapJump(Minecraft mc) {
            if (mc.player == null || mc.player.isPassenger()) {
                resetJumpTap();
                return;
            }
            // 站在地上也允许双击起飞：很多人习惯"原地按两下空格"，
            // 原来要求必须在空中，tap 全被复位掉了（玩家反馈"按不出来"）。
            // 代价是原地连跳两下也会起飞 —— 那本来就是"想起飞"的意思。
            // 那不是「想起飞」的意思，计数会变成「随手一点就把飞行关了」。
            if (mc.player.getAbilities().flying) {
                resetJumpTap();
                return;
            }
            net.minecraft.client.KeyMapping jump = mc.options.keyJump;
            while (jump.consumeClick()) {
                long now = System.currentTimeMillis();
                if (!jumpTapConsumed && jumpTapAt != 0L && now - jumpTapAt <= DOUBLE_TAP_MS) {
                    jumpTapConsumed = true;
                    // 玩家要求删掉这条提示：它每次双击起跳都糊在动作栏上，
                    // 而真正的原因（脚下没剑 / 境界不够）本来就会由服务端单独说明。
                    // 发包动作保持不变，只是不再弹字。
                } else {
                    jumpTapAt = now;
                    jumpTapConsumed = false;
                }
            }
        }

        /**
         * 用服务端同步过来的飞行许可纠正客户端 abilities。
         *
         * <p>只处理**允许**的方向：服务端说能飞但客户端没开，就补上。
         * 反过来不主动关——关掉飞行是服务端的职责（降境），
         * 客户端乱关会跟创造模式打架。</p>
         */
        private static void applyFlightPermission(Minecraft mc) {
            if (mc.player == null) {
                return;
            }
            if (!com.hongmeng.xiuxian.network.FlightSyncPacket.ClientFlightCache.mayFly()) {
                return;
            }
            if (mc.player.isCreative() || mc.player.isSpectator()) {
                return;
            }
            if (!mc.player.getAbilities().mayfly) {
                mc.player.getAbilities().mayfly = true;
            }
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            ClientPainState.tick();
            ClientBreakthroughFx.tick();

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                // 否则下次进世界的第一帧脚下会凭空多一把剑
                if (mc.player == null && hadPlayer) {
                    hadPlayer = false;
                    difficultyPrompted = false;
                    resetJumpTap();
                }
                return;
            }
            hadPlayer = true;

            // 飞行许可：每 tick 纠正一次客户端 abilities。
            //
            // 原版的能力同步在登录/重生时刻经常会丢，玩家按空格飞不起来，
            // 只有"先开创造飞一次再切回生存"才好——因为切模式会强制重发 abilities。
            // 这里从服务端直接同步过来的标记自行纠正，不再依赖那个时序。
            applyFlightPermission(mc);

            // 进世界后如果还没选难度，弹一次难度选择界面
            if (!difficultyPrompted && mc.player.tickCount > 30) {
                if (!com.hongmeng.xiuxian.network.ClientCultivationCache.get()
                        .hasChosenDifficulty()) {
                    difficultyPrompted = true;
                    mc.setScreen(new com.hongmeng.xiuxian.difficulty.DifficultyScreen());
                    return;
                }
                difficultyPrompted = true;
            }

            while (OPEN_PANEL.consumeClick()) {
                mc.setScreen(new CultivationPanelScreen());
            }

            while (QUEST_KEY.consumeClick()) {
                mc.setScreen(new com.hongmeng.xiuxian.quest.QuestScreen());
            }

            while (DOMAIN_KEY.consumeClick()) {
                mc.setScreen(new com.hongmeng.xiuxian.domain.DomainScreen());
            }
            // 数字 0：直接开关本命领域（玩家要求），不用进道宫界面
            while (DOMAIN_TOGGLE_KEY.consumeClick()) {
                com.hongmeng.xiuxian.network.DomainActionPacket.send(
                        com.hongmeng.xiuxian.network.DomainActionPacket.Action.TOGGLE, 0);
            }

            while (SHOP_KEY.consumeClick()) {
                mc.setScreen(new com.hongmeng.xiuxian.shop.ShopScreen());
            }

            // 施展功法
            while (CAST_KEY.consumeClick()) {
                com.hongmeng.xiuxian.network.TechniqueActionPacket.cast();
            }

            // 切换功法：Shift 反向
            while (CYCLE_KEY.consumeClick()) {
                if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                    com.hongmeng.xiuxian.network.TechniqueActionPacket.prev();
                } else {
                    com.hongmeng.xiuxian.network.TechniqueActionPacket.next();
                }
            }

            // （御剑已删除：飞升起由 Realm.hasFlight() 直接给原版飞行，
            //   不再有 H 键起剑、也不扣灵力。这里只保留双击空格起飞。）

            tickDoubleTapJump(mc);

            // 灵兽名册：手里有灵兽袋就开手里的那只，否则开背包里找到的第一只。
            // 界面读的是物品 NBT，所以这里不需要向服务端要任何数据。
            while (BEAST_ROSTER_KEY.consumeClick()) {
                net.minecraft.world.item.ItemStack pouch =
                        com.hongmeng.xiuxian.client.ClientBeastScreens.findPouch(mc.player);
                if (pouch.isEmpty()) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "\u00a77\u4f60\u8eab\u4e0a\u6ca1\u6709\u7075\u517d\u888b\u3002"), true);
                } else {
                    com.hongmeng.xiuxian.client.ClientBeastScreens.openRoster(pouch);
                }
            }

            while (BREAKTHROUGH_KEY.consumeClick()) {
                // 单人档：客户端不能直接改服务端数据，走命令
                mc.player.connection.sendCommand("hongmeng breakthrough");
            }
        }
    }
}
