package com.hongmeng.xiuxian;

import com.hongmeng.xiuxian.command.HongmengCommand;
import com.hongmeng.xiuxian.command.SectCommand;
import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.core.BlessingTracker;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.MeditationTracker;
import com.hongmeng.xiuxian.core.ModEvents;
import com.hongmeng.xiuxian.entity.ModEntities;
import com.hongmeng.xiuxian.item.ModCreativeTabs;
import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.network.ModNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 鸿蒙修仙 —— 以小说《谁把我仙侠游戏退出键扣了？》为蓝本的 Forge 1.20.1 模组。
 *
 * <p>核心设定：主角即玩家本人。开服即被迫"进入游戏"，没有退出键，
 * 挨打会疼，死了要算业力。修为不只在打坐里攒，杀怪、任务、奇遇、丹药都能涨。</p>
 */
@Mod(HongmengMod.MOD_ID)
public class HongmengMod {

    public static final String MOD_ID = "hongmeng";
    public static final String MOD_NAME = "鸿蒙修仙";

    /**
     * 成就触发器的注册表。
     *
     * <p>必须走 {@code DeferredRegister}（原版所有 criterion trigger 都注册在这里），
     * 才能让成就 JSON 里写的 {@code "trigger": "hongmeng:cultivation"} 解析得到。</p>
     */
    /**
     * 注册成就触发器。
     *
     * <p>⚠ 查过 1.20.1 的字节码：触发器**不走 {@code Registries}**（我在
     * {@code Registries} 里翻遍了都没有 TRIGGER 相关字段），而是走
     * {@code CriteriaTriggers.register(CriterionTrigger)} 这个静态方法
     * —— 它的字节码描述符是
     * {@code (Lnet/minecraft/advancements/CriterionTrigger;)Lnet/minecraft/advancements/CriterionTrigger;}。
     * 所以直接用那个方法注册，别再猜 {@code DeferredRegister} 的键名了。</p>
     */
    private static void registerCriterionTriggers() {
        net.minecraft.advancements.CriteriaTriggers.register(
                com.hongmeng.xiuxian.achievement.CultivationTrigger.INSTANCE);
    }
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public HongmengMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // 注册表
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        // 成就触发器（原版 advancement 框架，自带弹窗与成就界面）
        //
        // 正确做法是往 `Registries.CRITERION_TRIGGER` 这个**注册表**里注册，
        // 而不是挂到事件总线上 —— 挂错了的话 JSON 里写 `hongmeng:cultivation`
        // 会解析不到触发器，成就永远拿不到。
        registerCriterionTriggers();
        com.hongmeng.xiuxian.item.gear.ModGear.register(modBus);
        // 十境「道袍」可穿戴护甲（10 套 × 4 件 + 10 个 ArmorMaterial）
        com.hongmeng.xiuxian.item.armor.ModArmor.register(modBus);
        // 五套「道途护甲」（法修/体修/魔修/血修/阵修 各 4 件 = 20 件）
        com.hongmeng.xiuxian.item.armor.ModPathArmor.register(modBus);
        // 五把「道途武器」按境界分阶（5 道途 × 10 境 = 50 件）
        com.hongmeng.xiuxian.item.gear.ModPathWeapon.register(modBus);
        // R-2 新装备线：职业甲（5 职业 × 入门/进阶 × 4 件 = 40 件）
        com.hongmeng.xiuxian.item.armor.ModCareerArmor.register(modBus);
        // R-2 新装备线：通用双手武器（5 型 × 10 阶 = 50 件）
        com.hongmeng.xiuxian.item.gear.ModCareerWeapon.register(modBus);
        // 功法秘籍（22 门功法各一本）
        com.hongmeng.xiuxian.technique.TechniqueItems.register(modBus);
        // 解除原版属性上限：MAX_HEALTH 默认上限只有 1024，会静默截断境界加成
        com.hongmeng.xiuxian.compat.AttributeCaps.relax();
        ModEntities.register(modBus);
        // 第二轮新增的六只地形怪：实体类型 / 属性 / 刷怪合法性 / 刷怪蛋
        // （单独文件，避免多人改同一个共享注册表；客户端渲染见 ExtraMobRenderers）
        com.hongmeng.xiuxian.entity.ModEntitiesExtra.register(modBus);
        // 第二梯队 7 只（末地/真世界/妖兽·魔道·虚空秘境），同样新增文件、不动旧注册表
        com.hongmeng.xiuxian.entity.ModEntitiesExtra2.register(modBus);
        // 第四轮「天外天」：仙庭之主 + 天兵 + 云鹤（同样新增文件）
        com.hongmeng.xiuxian.entity.ModEntitiesExtra3.register(modBus);
        com.hongmeng.xiuxian.item.ModItemsExtra.register(modBus);
        // 第四轮「天外天」：登天令（进入道具；创造栏监听在这个 register 里一起挂）
        com.hongmeng.xiuxian.item.heaven.HeavenItems.register(modBus);
        ModCreativeTabs.register(modBus);
        ModEffects.register(modBus);
        ModSounds.register(modBus);
        // 炼丹：丹炉的方块实体 + 界面类型（并在注册时自检丹方表，表坏了直接加载失败）
        com.hongmeng.xiuxian.alchemy.ModAlchemy.register(modBus);
        com.hongmeng.xiuxian.ModEvil.register(modBus);

        // 配置文件
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, HongmengConfig.SPEC, "hongmeng-server.toml");

        // 生命周期
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onBuildCreativeTab);

        // 游戏内事件
        forgeBus.register(new ModEvents());
        // 模组自带的爆炸保护：苦力怕不破坏方块（死亡不掉落已按玩家要求取消）
        forgeBus.register(new com.hongmeng.xiuxian.core.ServerProtections());
        forgeBus.register(new MeditationTracker());
        forgeBus.register(new BlessingTracker());
        forgeBus.register(new com.hongmeng.xiuxian.core.RealmMining());
        // 武器附加攻击真正接入属性系统（之前只写在 tooltip 里，是假的）
        forgeBus.register(new com.hongmeng.xiuxian.item.gear.WeaponAttributeHandler());
        forgeBus.register(new com.hongmeng.xiuxian.compat.ArmorMitigation());
        // 灵兽 / 坐骑：交互入口（契灵符 / 灵兽袋 / 护甲 / 妖丹）
        forgeBus.register(new com.hongmeng.xiuxian.beast.BeastInteractions());
        // 灵兽：飞行时扣主人的灵力（与御剑共用同一口灵力池）
        forgeBus.register(new com.hongmeng.xiuxian.beast.BeastEvents());
        // 品质系统（六档：凡品/良品/上品/极品/仙品/神品）：
        // tooltip 彩行 + 合成盖章 + 妖兽掉落掷骰。纯数据表见 quality/QualityTable。
        forgeBus.register(new com.hongmeng.xiuxian.quality.QualityEvents());
        // 新群系注入：灵脉山谷 / 荒古战场 / 云梦泽。
        // 群系本体是 data/hongmeng/worldgen/biome/*.json，这里只负责把它们
        // 塞进主世界的 multi-noise 群系源（Forge 1.20.1 没有 TerraBlender 那种接口）。
        forgeBus.register(new com.hongmeng.xiuxian.worldgen.OverworldBiomeInjector());
        forgeBus.addListener(this::onRegisterCommands);

        // 客户端专属（GUI / 按键 / HUD / 退出键屏蔽）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(ClientSetup::onClientSetup);
            forgeBus.register(new ClientEvents());
            // 本命领域的客户端特效（元素色灵气 + 八种元素各自的粒子表现）。
            // 纯客户端：领域数据已经同步过来了，不需要额外的包。
            // ⚠ 必须**反射 + 判 dist**：直接 `new DomainClientVfx()` 会让
        // 专用服务器在加载时抛 RuntimeDistCleaner（客户端类不能出现在服务端字节码里）。
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            try {
                forgeBus.register(Class.forName(
                        "com.hongmeng.xiuxian.client.DomainClientVfx")
                        .getDeclaredConstructor().newInstance());
            } catch (Throwable t) {
                LOGGER.warn("[hongmeng] 领域客户端特效注册失败（不影响服务端）", t);
            }
        }
            // 战斗手感的客户端配置（hongmeng-client.toml）：
            // 屏幕震动开关是**无障碍选项**，多人游戏里客户端读不到服务端配置，
            // 所以它必须单独有一份客户端配置（见 HongmengConfig.Client 的类注释）。
            // 只注册在客户端分支：专用服务器既不加载它，也不读它。
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,
                    HongmengConfig.CLIENT_SPEC, "hongmeng-client.toml");
            // 丹炉界面的注册（Forge 1.20.1 没有 RegisterMenuScreensEvent，
            // 只能在 FMLClientSetupEvent 里注册；这个类只在客户端被加载）
            modBus.addListener(com.hongmeng.xiuxian.alchemy.AlchemyClientSetup::onClientSetup);
        }

        LOGGER.info("鸿蒙修仙已载入：鸿蒙修仙传，退出键已扣。");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // 坊市回收价表自检：每个在卖的东西都必须有回收价，且回收价严格低于买入单价
        // （防套利）。放在这里而不是 mod 构造期，是因为 SellPrices 要读
        // ShopCatalog 的秘籍买入价——两个类的静态初始化有先后依赖，
        // 在 common setup 时它们都已经建完了。纯数据，不碰注册表。
        com.hongmeng.xiuxian.shop.SellPrices.validate();

        // 第二次核对属性上限。
        //
        // 属性是注册表对象，别的模组可能在构造阶段之后才注册/替换它——
        // 那样我们先前改的那份就被绕过，上限悄悄回到 1024。
        // 玩家只会看到"血条卡在 1024"，根本查不到原因，所以这里再确认一次。
        com.hongmeng.xiuxian.compat.AttributeCaps.relax("common setup");

        // 品质 / 群系两张纯数据表的自检（不碰注册表，与离线验证器同一套断言）。
        // 放在这里而不是构造期：表写错的表现是"加载失败 + 说得清原因的异常"，
        // 而不是"某件东西的品质悄悄变成凡品"。
        com.hongmeng.xiuxian.quality.QualityEvents.validateAll();
        com.hongmeng.xiuxian.worldgen.BiomePlan.validate();

        // 本命领域的数值表自检 + 打一张表到日志。
        //
        // 纯数据（不碰注册表），八种元素的每个数字都在 domain/DomainEffectTable 里；
        // 打出来是为了"服务端日志本身就能证明跑的是哪套数字"——
        // 玩家给的数字一个都不能改，所以每个版本都留一份在日志里。
        com.hongmeng.xiuxian.domain.DomainEffectTable.validate();
        LOGGER.info("[hongmeng] 本命领域数值表\n{}",
                com.hongmeng.xiuxian.domain.DomainEffectTable.describeAll());

        // 触及距离：BLOCK_REACH / ENTITY_REACH 不在 AttributeCaps.relax() 的名单里，
        // 所以这里读一次它们的真实上限，确认最高 +5 格不会被截断（原版上限 1024）。
        LOGGER.info("[hongmeng] 触及距离属性上限自检：{}",
                com.hongmeng.xiuxian.core.ReachTable.capReport(
                        com.hongmeng.xiuxian.compat.AttributeCaps.maxOf(
                                net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()),
                        com.hongmeng.xiuxian.compat.AttributeCaps.maxOf(
                                net.minecraftforge.common.ForgeMod.ENTITY_REACH.get())));
        event.enqueueWork(() -> {
            CultivationCapability.register();
            ModNetwork.register();
        });
    }

    private void onBuildCreativeTab(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SEARCH
                || event.getTabKey() == CreativeModeTabs.INGREDIENTS
                || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            ModCreativeTabs.fillVanillaTab(event);
        }
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        HongmengCommand.register(event.getDispatcher());
        SectCommand.register(event.getDispatcher());
        // 灵兽的骑乘输入回传（/hmbeast jump）——独立根名，不与 /hongmeng 抢字面量
        com.hongmeng.xiuxian.command.BeastCommand.register(event.getDispatcher());
    }
}
