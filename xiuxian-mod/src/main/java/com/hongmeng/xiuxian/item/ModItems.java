package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HongmengMod.MOD_ID);

    // ---------------- 丹药 ----------------
    /** 筑基丹：一口气补一大笔修为。 */
    public static final RegistryObject<Item> PILL_ZHUJI =
            ITEMS.register("pill_zhuji", () -> new CultivationPillItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), 250.0D,
                    "一枚筑基丹下肚，灵气沿着经脉横冲直撞。"));

    /** 破境丹：临时把突破成功率拉满。 */
    public static final RegistryObject<Item> PILL_POJING =
            ITEMS.register("pill_pojing", () -> new BreakthroughPillItem(
                    new Item.Properties().stacksTo(8).rarity(Rarity.RARE)));

    /** 洗灵丹：重抽灵根，保底玄品以上。 */
    public static final RegistryObject<Item> PILL_XILING =
            ITEMS.register("pill_xiling", () -> new RerollRootItem(
                    new Item.Properties().stacksTo(4).rarity(Rarity.EPIC)));

    /** 回灵丹：回灵力（上限的 30%）。见 {@link ManaPillItem}。 */
    /**
     * <b>御剑术秘籍</b>：右键永久解锁剑阵。
     *
     * <p>玩家问「剑阵为什么开局就自带」—— 原来只有"境界 ≥ 第 2 境"一道门槛，
     * 等于白送。现在多一道获取手续。</p>
     */
    /**
     * 护心镜：副手格挡强化。
     *
     * <p>占副手换更强格挡 —— 让格挡变成有代价的选择，
     * 而不是任何武器顺手就能挡。</p>
     */

    // ---- 恶之祭坛：五个维度 boss 的专属掉落物（第 5 件在 HeavenItems） ----
    /** 傀儡核心（puppet_core 掉落）。 */
    public static final RegistryObject<Item> PUPPET_CORE = ITEMS.register("puppet_core",
            () -> new Item(new Item.Properties().stacksTo(16)
                    .rarity(Rarity.EPIC)));
    /** 兽王之心（beast_heart 掉落）。 */
    public static final RegistryObject<Item> BEAST_HEART = ITEMS.register("beast_heart",
            () -> new Item(new Item.Properties().stacksTo(16)
                    .rarity(Rarity.EPIC)));
    /** 血魔之核（blood_core 掉落）。 */
    public static final RegistryObject<Item> BLOOD_CORE = ITEMS.register("blood_core",
            () -> new Item(new Item.Properties().stacksTo(16)
                    .rarity(Rarity.EPIC)));
    /** 虚空之眼（void_eye 掉落）。 */
    public static final RegistryObject<Item> VOID_EYE = ITEMS.register("void_eye",
            () -> new Item(new Item.Properties().stacksTo(16)
                    .rarity(Rarity.EPIC)));

    /** 恶之祭坛的方块物品形式。 */
    public static final RegistryObject<Item> EVIL_ALTAR_ITEM =
            ITEMS.register("evil_altar", () -> new BlockItem(
                    com.hongmeng.xiuxian.ModBlocks.EVIL_ALTAR.get(),
                    new Item.Properties()));

    /** 真元晶矿的方块物品形式。 */
    public static final RegistryObject<Item> ZHENYUAN_ORE_ITEM =
            ITEMS.register("zhenyuan_ore", () -> new BlockItem(
                    ModBlocks.ZHENYUAN_ORE.get(), new Item.Properties()));

    /** 真元晶：真世界专属矿物的产物。 */
    public static final RegistryObject<Item> ZHENYUAN_CRYSTAL =
            ITEMS.register("zhenyuan_crystal", () -> new Item(
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SWORD_ART_MANUAL =
            ITEMS.register("sword_art_manual", () -> new SwordArtManualItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> PILL_HUILING =
            ITEMS.register("pill_huiling", () -> new ManaPillItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    /** 造化丹：原文里新手开局的奖励，给灵叶。 */
    public static final RegistryObject<Item> ZHAOHUA_DAN =
            ITEMS.register("zhaohua_dan", () -> new ZhaohuaPillItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    /** 双道宫丹：解锁第二座道宫。来源是化神试炼。 */
    public static final RegistryObject<Item> PILL_SHUANGDAO =
            ITEMS.register("pill_shuangdao", () -> new DualDomainPillItem(
                    new Item.Properties().stacksTo(4).rarity(Rarity.EPIC)));

    /**
     * 洗忆丹：洗去天赋，点数全部退回（<b>道途不变</b>）。
     *
     * <p>为什么需要它：之前天赋点是一次性的，点错就<b>没有任何补救</b>，
     * 只能重开档 —— 那会让玩家<b>不敢选</b>，"取舍"的意义反而消失。</p>
     *
     * <p>做成物品而不是免费命令：免费洗点会让"点数有限所以要取舍"失效；
     * 物品则让洗点变成"值得做但不该天天做"的决策。</p>
     */
    public static final RegistryObject<Item> PILL_TALENT_RESET =
            ITEMS.register("talent_reset_pill", () -> new TalentResetPillItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    /**
     * 转道丹：洗去旧道，可重新择一条道途（已点天赋全部退回）。
     *
     * <p>道途决定整条成长曲线（乘数/天赋树/专属装备/机制），
     * 永久锁定等于"在信息最少时做不可撤销的决定"。</p>
     */
    public static final RegistryObject<Item> PILL_PATH_SWITCH =
            ITEMS.register("path_switch_pill", () -> new PathSwitchItem(
                    new Item.Properties().stacksTo(4).rarity(Rarity.EPIC)));

    // ---------------- 修为丹（三档品质） ----------------
    // 每颗给"当前小境界所需修为的 25% × 品质倍率"，
    // 所以普通/完美/仙品 = 25% / 75% / 250%。

    /** 普通修为丹：25% 当前小境界。 */
    public static final RegistryObject<Item> PILL_EXP_COMMON =
            ITEMS.register("pill_exp_common", () -> new ExperiencePillItem(
                    PillQuality.COMMON, 0.25D,
                    new Item.Properties().stacksTo(64).rarity(Rarity.COMMON)));

    /** 完美修为丹：75%。 */
    public static final RegistryObject<Item> PILL_EXP_PERFECT =
            ITEMS.register("pill_exp_perfect", () -> new ExperiencePillItem(
                    PillQuality.PERFECT, 0.25D,
                    new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));

    /** 仙品修为丹：250%（一颗能推两个半小境界）。 */
    public static final RegistryObject<Item> PILL_EXP_IMMORTAL =
            ITEMS.register("pill_exp_immortal", () -> new ExperiencePillItem(
                    PillQuality.IMMORTAL, 0.25D,
                    new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));

    // ---------------- 材料 ----------------
    public static final RegistryObject<Item> LINGYE = material("lingye", Rarity.UNCOMMON);

    /**
     * 鸿蒙修行录：mod 自带的图鉴书（玩家要求：类似植物魔法的那本书）。
     *
     * <p>右键翻开，能查：每个物品的获得途径、合成方式、方块/法宝/护甲/秘籍分栏、
     * NPC 介绍、建筑与群系，以及<b>全局搜索</b>。</p>
     */
    public static final RegistryObject<Item> GUIDE_BOOK =
            ITEMS.register("guide_book", com.hongmeng.xiuxian.item.GuideBookItem::new);
    public static final RegistryObject<Item> DAOYIN = material("daoyin", Rarity.RARE);
    public static final RegistryObject<Item> JIDAN = material("jidan", Rarity.RARE);
    /** 千机门是反派，不发令牌。 */
    public static final RegistryObject<Item> SECT_TOKEN_SHENXIAO = material("sect_token_shenxiao", Rarity.UNCOMMON);
    public static final RegistryObject<Item> XUE_LUOGUO = material("xue_luoguo", Rarity.UNCOMMON);
    public static final RegistryObject<Item> KONG_MING_SHI = material("kong_ming_shi", Rarity.UNCOMMON);

    // ---------------- 合成材料 ----------------
    // 矿物掉的是这些材料，而不是原方块（想要方块本体用精准采集）。
    // 加这些是为了让玩家能用矿石走一条**完整的合成链**做出丹药与法宝。

    /** 灵铁锭：灵铁矿的产物，一切炼器的起点。 */
    public static final RegistryObject<Item> SPIRIT_IRON_INGOT =
            material("spirit_iron_ingot", Rarity.COMMON);

    /** 灵晶：灵晶矿的产物，中阶丹药的主料。 */
    public static final RegistryObject<Item> SPIRIT_CRYSTAL =
            material("spirit_crystal", Rarity.UNCOMMON);

    /** 空冥精：空冥矿的产物，高阶炼器材料。 */
    public static final RegistryObject<Item> KONGMING_ESSENCE =
            material("kongming_essence", Rarity.RARE);

    /** 血珞精：血珞矿的产物，与痛觉/业力相关的稀有材料。 */
    public static final RegistryObject<Item> XUELUO_ESSENCE =
            material("xueluo_essence", Rarity.RARE);

    // ---------------- 方块物品 ----------------
    public static final RegistryObject<Item> MEDITATION_CUSHION_ITEM =
            ITEMS.register("meditation_cushion",
                    () -> new BlockItem(ModBlocks.MEDITATION_CUSHION.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_GRASS_ITEM =
            ITEMS.register("spirit_grass",
                    () -> new BlockItem(ModBlocks.SPIRIT_GRASS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ROOT_ALTAR_ITEM =
            ITEMS.register("root_altar",
                    () -> new BlockItem(ModBlocks.ROOT_ALTAR.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> EPOCH_PORTAL_FRAME_ITEM =
            ITEMS.register("epoch_portal_frame",
                    () -> new BlockItem(ModBlocks.EPOCH_PORTAL_FRAME.get(), new Item.Properties().rarity(Rarity.RARE)));

    // ---------------- 三座新副本的门 ----------------
    // 传送门方块本身（hongmeng:dungeon_portal）**刻意不给物品**：
    // 它是点着火之后自己填进门里的技术方块，跟原版下界门一样只该用 /setblock 摆。

    /** 妖兽之门框架（妖兽秘境，门槛金丹）。 */
    public static final RegistryObject<Item> BEAST_PORTAL_FRAME_ITEM =
            ITEMS.register("beast_portal_frame",
                    () -> new BlockItem(ModBlocks.BEAST_PORTAL_FRAME.get(),
                            new Item.Properties().rarity(Rarity.RARE)));

    /** 魔道之门框架（魔道巢穴，门槛道印）。 */
    public static final RegistryObject<Item> DEMON_PORTAL_FRAME_ITEM =
            ITEMS.register("demon_portal_frame",
                    () -> new BlockItem(ModBlocks.DEMON_PORTAL_FRAME.get(),
                            new Item.Properties().rarity(Rarity.EPIC)));

    /** 虚空之门框架（虚空裂隙，门槛仙人）。 */
    public static final RegistryObject<Item> VOID_PORTAL_FRAME_ITEM =
            ITEMS.register("void_portal_frame",
                    () -> new BlockItem(ModBlocks.VOID_PORTAL_FRAME.get(),
                            new Item.Properties().rarity(Rarity.EPIC)));

    // ---------------- Boss 召唤祭坛（方块物品） ----------------
    //
    // ⚠ 这五个**必须**注册 BlockItem：创造栏里 `output.accept(方块)` 会让 Forge
    // 去取它的物品形态；没注册就是空气（数量 0），
    // 直接崩 `IllegalArgumentException: The stack count must be 1`（真实事故）。
    /** 通用召唤祭坛。 */
    public static final RegistryObject<Item> BOSS_SUMMON_ALTAR_ITEM =
            ITEMS.register("boss_summon_altar", () -> new BlockItem(
                    com.hongmeng.xiuxian.ModBlocks.BOSS_SUMMON_ALTAR.get(),
                    new Item.Properties().rarity(Rarity.RARE)));
    /** 纪元召唤祭坛。 */
    public static final RegistryObject<Item> BOSS_SUMMON_ALTAR_EPOCH_ITEM =
            ITEMS.register("boss_summon_altar_epoch", () -> new BlockItem(
                    com.hongmeng.xiuxian.ModBlocks.BOSS_SUMMON_ALTAR_EPOCH.get(),
                    new Item.Properties().rarity(Rarity.RARE)));
    /** 妖兽召唤祭坛。 */
    public static final RegistryObject<Item> BOSS_SUMMON_ALTAR_BEAST_ITEM =
            ITEMS.register("boss_summon_altar_beast", () -> new BlockItem(
                    com.hongmeng.xiuxian.ModBlocks.BOSS_SUMMON_ALTAR_BEAST.get(),
                    new Item.Properties().rarity(Rarity.RARE)));
    /** 血魔教召唤祭坛。 */
    public static final RegistryObject<Item> BOSS_SUMMON_ALTAR_DEMON_ITEM =
            ITEMS.register("boss_summon_altar_demon", () -> new BlockItem(
                    com.hongmeng.xiuxian.ModBlocks.BOSS_SUMMON_ALTAR_DEMON.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));
    /** 虚空召唤祭坛。 */
    public static final RegistryObject<Item> BOSS_SUMMON_ALTAR_VOID_ITEM =
            ITEMS.register("boss_summon_altar_void", () -> new BlockItem(
                    com.hongmeng.xiuxian.ModBlocks.BOSS_SUMMON_ALTAR_VOID.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));

    // ---------------- 矿石 ----------------
    public static final RegistryObject<Item> SPIRIT_IRON_ORE_ITEM =
            ITEMS.register("spirit_iron_ore",
                    () -> new BlockItem(ModBlocks.SPIRIT_IRON_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEEPSLATE_SPIRIT_IRON_ORE_ITEM =
            ITEMS.register("deepslate_spirit_iron_ore",
                    () -> new BlockItem(ModBlocks.DEEPSLATE_SPIRIT_IRON_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_CRYSTAL_ORE_ITEM =
            ITEMS.register("spirit_crystal_ore",
                    () -> new BlockItem(ModBlocks.SPIRIT_CRYSTAL_ORE.get(),
                            new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> DEEPSLATE_SPIRIT_CRYSTAL_ORE_ITEM =
            ITEMS.register("deepslate_spirit_crystal_ore",
                    () -> new BlockItem(ModBlocks.DEEPSLATE_SPIRIT_CRYSTAL_ORE.get(),
                            new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> KONGMING_ORE_ITEM =
            ITEMS.register("kongming_ore",
                    () -> new BlockItem(ModBlocks.KONGMING_ORE.get(),
                            new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> XUELUO_ORE_ITEM =
            ITEMS.register("xueluo_ore",
                    () -> new BlockItem(ModBlocks.XUELUO_ORE.get(),
                            new Item.Properties().rarity(Rarity.EPIC)));

    // ---------------- 五阶野怪的刷怪蛋 ----------------
    // 配色按品阶走：普通偏灰、精英偏绿、队长青铜、头目暗紫、首领金黑。
    //
    // 必须用 ForgeSpawnEggItem 而不是原版 SpawnEggItem：原版构造函数要求
    // 传入已经解析好的 EntityType，而刷怪蛋注册和实体注册在同一个
    // RegisterEvent 里跑——传 .get() 会直接抛
    // "NullPointerException: Registry Object not present"，
    // **整个 mod 加载失败、游戏起不来**（这个坑踩过）。
    // Forge 版本接受 Supplier，只在实际使用刷怪蛋时才去查实体类型。

    /** 灵狼刷怪蛋（普通）。 */
    public static final RegistryObject<Item> SPIRIT_WOLF_SPAWN_EGG =
            ITEMS.register("spirit_wolf_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.SPIRIT_WOLF,
                    0x8E9AA6, 0x3B4650, new Item.Properties()));

    /** 血魔修士刷怪蛋（精英）。 */
    public static final RegistryObject<Item> BLOOD_DEMON_CULTIVATOR_SPAWN_EGG =
            ITEMS.register("blood_demon_cultivator_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.BLOOD_DEMON_CULTIVATOR,
                    0x6B0F14, 0xC0392B, new Item.Properties()));

    /** 傀儡队长刷怪蛋（队长）。 */
    public static final RegistryObject<Item> PUPPET_CAPTAIN_SPAWN_EGG =
            ITEMS.register("puppet_captain_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.PUPPET_CAPTAIN,
                    0x7A6A46, 0x3E8E7E, new Item.Properties()));

    /** 魔道统领刷怪蛋（头目）。 */
    public static final RegistryObject<Item> DEMON_GENERAL_SPAWN_EGG =
            ITEMS.register("demon_general_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.DEMON_GENERAL,
                    0x2E1B4D, 0xFF5C2E, new Item.Properties()));

    /** 虚空道主刷怪蛋（首领）。 */
    public static final RegistryObject<Item> VOID_OVERLORD_SPAWN_EGG =
            ITEMS.register("void_overlord_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.VOID_OVERLORD,
                    0x120C1E, 0xE8C25A, new Item.Properties()));

    // ---------------- 第二批妖兽的刷怪蛋 ----------------
    // 配色沿用"按品阶走"的口径：精英偏火、队长偏铜/石灰、头目暗紫黑。

    /** 烈焰猿刷怪蛋（精英）。 */
    public static final RegistryObject<Item> FLAME_APE_SPAWN_EGG =
            ITEMS.register("flame_ape_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.FLAME_APE,
                    0x3B1A10, 0xFF7A22, new Item.Properties()));

    /** 雷鹰刷怪蛋（队长）。 */
    public static final RegistryObject<Item> THUNDER_EAGLE_SPAWN_EGG =
            ITEMS.register("thunder_eagle_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.THUNDER_EAGLE,
                    0x2B3B52, 0x8FE3FF, new Item.Properties()));

    /** 石傀刷怪蛋（队长）。 */
    public static final RegistryObject<Item> STONE_GOLEM_SPAWN_EGG =
            ITEMS.register("stone_golem_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.STONE_GOLEM,
                    0x5A5F63, 0xC9A44E, new Item.Properties()));

    /** 虚空蛟刷怪蛋（头目）。 */
    public static final RegistryObject<Item> VOID_SERPENT_SPAWN_EGG =
            ITEMS.register("void_serpent_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.VOID_SERPENT,
                    0x160F22, 0x9A4BFF, new Item.Properties()));

    // ---------------- NPC 刷怪蛋 ----------------
    // NPC 不会自然生成，也不该"放出来就乱跑"——刷怪蛋主要是给地图作者与调试用的。

    /** 门派长老刷怪蛋。 */
    public static final RegistryObject<Item> SECT_ELDER_SPAWN_EGG =
            ITEMS.register("sect_elder_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.SECT_ELDER,
                    0xD8D8E0, 0x9A9088, new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 炼丹师刷怪蛋。 */
    public static final RegistryObject<Item> ALCHEMY_MASTER_SPAWN_EGG =
            ITEMS.register("alchemy_master_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.ALCHEMY_MASTER,
                    0x2F6B3A, 0xD8B45C, new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 云游商人刷怪蛋。 */
    public static final RegistryObject<Item> WANDERING_MERCHANT_SPAWN_EGG =
            ITEMS.register("wandering_merchant_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.WANDERING_MERCHANT,
                    0x6B4A2A, 0x4E7FA8, new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---------------- 灵兽 / 坐骑系统 ----------------
    // 见 docs/spirit-beast-spec.md。三件物品 + 三枚刷怪蛋。
    //
    // 刷怪蛋一律用 ForgeSpawnEggItem（吃 Supplier），**不能**用原版
    // SpawnEggItem——原版构造函数要一个已经解析好的 EntityType，而物品注册与
    // 实体注册在同一个 RegisterEvent 里跑，传 .get() 会抛
    // "Registry Object not present"，整个 mod 加载失败（这个坑踩过）。

    /** 契灵符：把打到 25% 生命以下的妖兽收为灵兽。 */
    public static final RegistryObject<Item> CONTRACT_TALISMAN =
            ITEMS.register("contract_talisman",
                    () -> new com.hongmeng.xiuxian.item.beast.ContractTalismanItem(
                            com.hongmeng.xiuxian.item.beast.ContractTalismanItem.defaultProperties()));

    /** 灵兽袋：收纳 / 放出 / 名册。规格里的强制项（解决坐骑走丢）。 */
    public static final RegistryObject<Item> BEAST_POUCH =
            ITEMS.register("beast_pouch",
                    () -> new com.hongmeng.xiuxian.item.beast.BeastPouchItem(
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** 灵兽护甲：给灵兽加护甲。 */
    public static final RegistryObject<Item> BEAST_ARMOR =
            ITEMS.register("beast_armor",
                    () -> new com.hongmeng.xiuxian.item.beast.BeastArmorItem(
                            new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    /** 契约雷鹰刷怪蛋（调试用；正常途径是契约 thunder_eagle）。 */
    public static final RegistryObject<Item> CONTRACT_EAGLE_SPAWN_EGG =
            ITEMS.register("contract_eagle_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.CONTRACT_EAGLE,
                    0x2B3B52, 0x8FE3FF, new Item.Properties()));

    /** 蛟龙刷怪蛋（调试用；正常途径是契约 void_serpent）。 */
    public static final RegistryObject<Item> JIAOLONG_SPAWN_EGG =
            ITEMS.register("jiaolong_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.JIAOLONG,
                    0x160F22, 0x64E8D8, new Item.Properties()));

    /** 契约灵狼刷怪蛋（调试用；正常途径是契约 spirit_wolf）。 */
    public static final RegistryObject<Item> CONTRACT_WOLF_SPAWN_EGG =
            ITEMS.register("contract_wolf_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.CONTRACT_WOLF,
                    0xDCE9F2, 0x4FE3FF, new Item.Properties()));

    // ---------------- 第三批妖兽的刷怪蛋（土黄 / 毒绿，仍按品阶口径配色） ----------------

    /** 土行貂刷怪蛋（精英）。 */
    public static final RegistryObject<Item> BURROW_MARTEN_SPAWN_EGG =
            ITEMS.register("burrow_marten_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.BURROW_MARTEN,
                    0x6B5A3E, 0xC9A44E, new Item.Properties()));

    /** 毒涎蟾刷怪蛋（队长）。 */
    public static final RegistryObject<Item> VENOM_TOAD_SPAWN_EGG =
            ITEMS.register("venom_toad_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.VENOM_TOAD,
                    0x3E5A2E, 0x8FD24A, new Item.Properties()));

    // ---- Boss 刷怪蛋（玩家要求每个 boss 都能单独刷出来）----
    /** 恶（世界终极 boss）刷怪蛋。 */
    public static final RegistryObject<Item> EVIL_GAZER_SPAWN_EGG =
            ITEMS.register("evil_gazer_spawn_egg", () -> new net.minecraftforge.common
                    .ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.EVIL_GAZER,
                    0x2A1030, 0xD02030, new Item.Properties()));
    /** 焚天炎帝（下界终局）。 */
    public static final RegistryObject<Item> BLAZE_EMPEROR_SPAWN_EGG =
            ITEMS.register("blaze_emperor_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.BLAZE_EMPEROR,
                    0xB03A18, 0xFFC64A, new Item.Properties()));
    /** 虚空龙帝（末地终局）。 */
    public static final RegistryObject<Item> VOID_DRAGON_EMPEROR_SPAWN_EGG =
            ITEMS.register("void_dragon_emperor_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.VOID_DRAGON_EMPEROR,
                    0x3A2266, 0xC8A6FF, new Item.Properties()));
    /** 傀儡祖师（祭坛首领）。 */
    public static final RegistryObject<Item> EPOCH_BOSS_SPAWN_EGG =
            ITEMS.register("epoch_boss_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.EPOCH_BOSS,
                    0x6A5A3A, 0x8CE8C0, new Item.Properties()));
    /** 万兽之主（祭坛首领）。 */
    public static final RegistryObject<Item> BEAST_BOSS_SPAWN_EGG =
            ITEMS.register("beast_boss_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.BEAST_BOSS,
                    0x5A4A2A, 0xE0B040, new Item.Properties()));
    /** 血魔老祖（祭坛首领）。 */
    public static final RegistryObject<Item> DEMON_BOSS_SPAWN_EGG =
            ITEMS.register("demon_boss_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.DEMON_BOSS,
                    0x7A1420, 0xFF7038, new Item.Properties()));
    /** 虚空道主（祭坛首领）。 */
    public static final RegistryObject<Item> VOID_BOSS_SPAWN_EGG =
            ITEMS.register("void_boss_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.VOID_BOSS,
                    0x2A1A4A, 0x9A7AFF, new Item.Properties()));
    /** 虚空行者（末地头目）。 */
    public static final RegistryObject<Item> VOID_WALKER_SPAWN_EGG =
            ITEMS.register("void_walker_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.hongmeng.xiuxian.entity.ModEntities.VOID_WALKER,
                    0x33244A, 0x8A7AD0, new Item.Properties()));

    // ---------------- 灵植 / 炼丹（见 docs/herb-alchemy-spec.md） ----------------
    // 六阶灵植各有两件物品：
    //   <herb>_seed   种子。ItemNameBlockItem —— 保证 lang key 走
    //                 item.hongmeng.<herb>_seed，而不是变成方块名。
    //   <herb>_petal  花瓣。炼丹材料，tooltip 里带品阶与用途。
    //
    // 种子一律写成 ItemNameBlockItem：它的 getDescriptionId() 指向**物品自己**
    // （item.hongmeng.<herb>_seed），而不是像 BlockItem 那样借用方块名。
    // 注册一律内联写出名字，不抽 helper —— tools/check_item_assets.py 是按
    // "ITEMS 点 register" 那个正则扫物品清单的，抽成 helper 会让新物品
    // 从审计里消失（贴图缺了也没人报）。

    // 种子的物品名颜色：统一用 UNCOMMON（黄色）。
    // 玩家反馈「种子的字体也应该像灵植一样是黄色」—— MC 的物品名默认颜色由 rarity 决定，
    // COMMON 是白字，所以原来雪莲草/火焰花两颗种子在背包里是白的，其余四颗是黄的。
    /** 一阶 · 雪莲草种子。 */
    public static final RegistryObject<Item> XUELIAN_GRASS_SEED =
            ITEMS.register("xuelian_grass_seed", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.XUELIAN_GRASS.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** 一阶 · 雪莲花瓣（回灵 / 疗伤）。 */
    public static final RegistryObject<Item> XUELIAN_GRASS_PETAL =
            ITEMS.register("xuelian_grass_petal", () -> new AlchemyMaterialItem(
                    new Item.Properties().rarity(Rarity.UNCOMMON), 1, "雪莲草", "回灵 / 疗伤"));

    /** 二阶 · 火焰花种子。 */
    public static final RegistryObject<Item> HUOYAN_HUA_SEED =
            ITEMS.register("huoyan_hua_seed", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.HUOYAN_HUA.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** 二阶 · 火焰花瓣（攻击 / 火系）。 */
    public static final RegistryObject<Item> HUOYAN_HUA_PETAL =
            ITEMS.register("huoyan_hua_petal", () -> new AlchemyMaterialItem(
                    new Item.Properties().rarity(Rarity.UNCOMMON), 2, "火焰花", "攻击 / 火系"));

    /** 三阶 · 寒冰藤种子。 */
    public static final RegistryObject<Item> HANBING_TENG_SEED =
            ITEMS.register("hanbing_teng_seed", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.HANBING_TENG.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** 三阶 · 寒冰藤花瓣（控制 / 冰系）。 */
    public static final RegistryObject<Item> HANBING_TENG_PETAL =
            ITEMS.register("hanbing_teng_petal", () -> new AlchemyMaterialItem(
                    new Item.Properties().rarity(Rarity.RARE), 3, "寒冰藤", "控制 / 冰系"));

    /** 四阶 · 雷音草种子。 */
    public static final RegistryObject<Item> LEIYIN_CAO_SEED =
            ITEMS.register("leiyin_cao_seed", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.LEIYIN_CAO.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** 四阶 · 雷音草花瓣（雷系 / 突破）。 */
    public static final RegistryObject<Item> LEIYIN_CAO_PETAL =
            ITEMS.register("leiyin_cao_petal", () -> new AlchemyMaterialItem(
                    new Item.Properties().rarity(Rarity.RARE), 4, "雷音草", "雷系 / 突破"));

    /** 五阶 · 空冥兰种子。 */
    public static final RegistryObject<Item> KONGMING_LAN_SEED =
            ITEMS.register("kongming_lan_seed", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.KONGMING_LAN.get(), new Item.Properties().rarity(Rarity.RARE)));
    /** 五阶 · 空冥兰花瓣（高阶 / 空间）。 */
    public static final RegistryObject<Item> KONGMING_LAN_PETAL =
            ITEMS.register("kongming_lan_petal", () -> new AlchemyMaterialItem(
                    new Item.Properties().rarity(Rarity.EPIC), 5, "空冥兰", "高阶 / 空间"));

    /** 六阶 · 血珞芝种子。 */
    public static final RegistryObject<Item> XUELUO_ZHI_SEED =
            ITEMS.register("xueluo_zhi_seed", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.XUELUO_ZHI.get(), new Item.Properties().rarity(Rarity.RARE)));
    /** 六阶 · 血珞芝花瓣（顶级 / 血道）。 */
    public static final RegistryObject<Item> XUELUO_ZHI_PETAL =
            ITEMS.register("xueluo_zhi_petal", () -> new AlchemyMaterialItem(
                    new Item.Properties().rarity(Rarity.EPIC), 6, "血珞芝", "顶级 / 血道"));

    // ---------------- 灵田土 / 丹炉（方块物品） ----------------

    /** 灵田土：种灵植用的高级土，灵植种在上面长得快三倍。 */
    public static final RegistryObject<Item> LINGTIAN_SOIL_ITEM =
            ITEMS.register("lingtian_soil",
                    () -> new BlockItem(ModBlocks.LINGTIAN_SOIL.get(),
                            new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 丹炉：炼丹的核心设施。 */
    public static final RegistryObject<Item> ALCHEMY_FURNACE_ITEM =
            ITEMS.register("alchemy_furnace",
                    () -> new BlockItem(ModBlocks.ALCHEMY_FURNACE.get(),
                            new Item.Properties().rarity(Rarity.RARE)));

    // ---------------- 六阶灵植的方块物品 ----------------
    // 野外采到的灵植方块本体也能拿在手里（照原版作物那样不给方块物品也行，
    // 但这里给了：一是创造标签页要展示六株灵植长什么样，
    // 二是让地图作者能直接摆一株"长熟的"灵植）。

    /** 雪莲草（方块物品）。 */
    public static final RegistryObject<Item> XUELIAN_GRASS_ITEM =
            ITEMS.register("xuelian_grass",
                    () -> new BlockItem(ModBlocks.XUELIAN_GRASS.get(),
                            new Item.Properties().rarity(Rarity.COMMON)));
    /** 火焰花（方块物品）。 */
    public static final RegistryObject<Item> HUOYAN_HUA_ITEM =
            ITEMS.register("huoyan_hua",
                    () -> new BlockItem(ModBlocks.HUOYAN_HUA.get(),
                            new Item.Properties().rarity(Rarity.COMMON)));
    /** 寒冰藤（方块物品）。 */
    public static final RegistryObject<Item> HANBING_TENG_ITEM =
            ITEMS.register("hanbing_teng",
                    () -> new BlockItem(ModBlocks.HANBING_TENG.get(),
                            new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** 雷音草（方块物品）。 */
    public static final RegistryObject<Item> LEIYIN_CAO_ITEM =
            ITEMS.register("leiyin_cao",
                    () -> new BlockItem(ModBlocks.LEIYIN_CAO.get(),
                            new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** 空冥兰（方块物品）。 */
    public static final RegistryObject<Item> KONGMING_LAN_ITEM =
            ITEMS.register("kongming_lan",
                    () -> new BlockItem(ModBlocks.KONGMING_LAN.get(),
                            new Item.Properties().rarity(Rarity.RARE)));
    /** 血珞芝（方块物品）。 */
    public static final RegistryObject<Item> XUELUO_ZHI_ITEM =
            ITEMS.register("xueluo_zhi",
                    () -> new BlockItem(ModBlocks.XUELUO_ZHI.get(),
                            new Item.Properties().rarity(Rarity.EPIC)));

    /** 灵犀剑（GeckoLib 版）：12 段弧刃 + 红/青双色自发光。 */
    public static final RegistryObject<Item> SPIRIT_SWORD_GEO =
            ITEMS.register("spirit_sword_v2",
                    () -> new SpiritSwordGeoItem(
                            new Item.Properties().stacksTo(1)));
    /** 灵犀剑 · 三层刃（GeckoLib 立体分层模型）。 */
    public static final RegistryObject<Item> GEO_LINGXI =
            ITEMS.register("g_lingxi", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_lingxi", 9.0, 1.6));
    /** 崩山杵 · 三重阶（GeckoLib 立体分层模型）。 */
    public static final RegistryObject<Item> GEO_BENGSHAN =
            ITEMS.register("g_bengshan", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_bengshan", 14.0, 0.9));
    /** 噬魂刃 · 倒钩（GeckoLib 立体分层模型）。 */
    public static final RegistryObject<Item> GEO_SHIHUN =
            ITEMS.register("g_shihun", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_shihun", 11.0, 1.4));
    /** 血饮双刀 · 獠牙（GeckoLib 立体分层模型）。 */
    public static final RegistryObject<Item> GEO_XUEYIN =
            ITEMS.register("g_xueyin", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_xueyin", 8.0, 2.2));
    // ===== 第二形态库：仙侠飞剑 / 陨铁重剑 / 血魔双爪 / 虚空巨镰 / 八卦阵盘 / 龙骨长枪 =====
    public static final RegistryObject<Item> GEO_FEIJIAN =
            ITEMS.register("g_feijian", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_feijian", 9.0, 1.9));
    public static final RegistryObject<Item> GEO_YUNTIE =
            ITEMS.register("g_yuntie", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_yuntie", 14.0, 0.8));
    public static final RegistryObject<Item> GEO_XUEMO =
            ITEMS.register("g_xuemo", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_xuemo", 7.0, 2.6));
    public static final RegistryObject<Item> GEO_XUKONG =
            ITEMS.register("g_xukong", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_xukong", 12.0, 1.0));

    public static final RegistryObject<Item> GEO_LONGGU =
            ITEMS.register("g_longgu", () -> new com.hongmeng.xiuxian.item.GeoWeaponItem(
                    "g_longgu", 10.0, 1.4));

    // ==================================================================
    // 2026-11-07 · 灵石 / 灵石矿 / 灵石系修为丹（修为经济线，玩家要求）
    //
    // 注册项一律**追加在文件末尾**：注册表 ID 按注册顺序分配，中间插入会让
    // 后面所有物品的 ID 前移、老存档错位（见 00-总工程师总纲 §7.3）。
    // ==================================================================

    /** 灵石矿（方块物品）：主世界地下成脉生成，挖出「灵石」。 */
    public static final RegistryObject<Item> SPIRIT_STONE_ORE_ITEM =
            ITEMS.register("spirit_stone_ore",
                    () -> new BlockItem(ModBlocks.SPIRIT_STONE_ORE.get(),
                            new Item.Properties().rarity(Rarity.UNCOMMON)));

    /**
     * <b>灵石</b>：右键直接服用涨修为（当前小境界需求的 2.5%），
     * 也是「聚灵丹 / 凝元丹」的主料。
     */
    public static final RegistryObject<Item> SPIRIT_STONE =
            ITEMS.register("spirit_stone",
                    () -> new SpiritStoneItem(
                            new Item.Properties().rarity(Rarity.UNCOMMON)));

    /**
     * <b>聚灵丹</b>：灵石 ×4 + 灵叶 ×2 合成；服下给当前小境界需求的 15%。
     */
    public static final RegistryObject<Item> PILL_JULING =
            ITEMS.register("pill_juling",
                    () -> new SpiritPillItem(PillQuality.COMMON, 0.15D, "聚灵丹",
                            new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

    /**
     * <b>凝元丹</b>：灵石 ×6 + 灵晶 ×2 + 灵叶 ×1 合成；服下给 45%。
     */
    public static final RegistryObject<Item> PILL_NINGYUAN =
            ITEMS.register("pill_ningyuan",
                    () -> new SpiritPillItem(PillQuality.COMMON, 0.45D, "凝元丹",
                            new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));

    // ==================================================================
    // 2026-11-07 第二批 · 深板岩矿方块物品（同样追加在末尾，保持老 ID 不变）
    // ==================================================================

    /** 深板岩空冥矿（方块物品）。 */
    public static final RegistryObject<Item> DEEPSLATE_KONGMING_ORE_ITEM =
            ITEMS.register("deepslate_kongming_ore",
                    () -> new BlockItem(ModBlocks.DEEPSLATE_KONGMING_ORE.get(),
                            new Item.Properties().rarity(Rarity.EPIC)));

    /** 深板岩血罗矿（方块物品）。 */
    public static final RegistryObject<Item> DEEPSLATE_XUELUO_ORE_ITEM =
            ITEMS.register("deepslate_xueluo_ore",
                    () -> new BlockItem(ModBlocks.DEEPSLATE_XUELUO_ORE.get(),
                            new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 深板岩灵石矿（方块物品）。 */
    public static final RegistryObject<Item> DEEPSLATE_SPIRIT_STONE_ORE_ITEM =
            ITEMS.register("deepslate_spirit_stone_ore",
                    () -> new BlockItem(ModBlocks.DEEPSLATE_SPIRIT_STONE_ORE.get(),
                            new Item.Properties().rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }

    /**
     * 注册一件**纪念品**（灵根符这类"一人一张、不会攒"的东西）。
     *
     * <p>保持原版 64 堆叠：灵根符是抽到灵根时给的纪念，不是材料，
     * 抬高它的堆叠上限只会让"这到底是材料还是凭证"变得含糊。</p>
     */
    private static RegistryObject<Item> simple(String name, Rarity rarity) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().rarity(rarity)));
    }

    /**
     * 注册一件**材料**物品（会攒一大把的那种）。
     *
     * <p>用 {@link StackableMaterialItem}：矿石产物、灵铁锭、灵晶、灵叶、
     * 道印、妖丹、门派令牌这些"挖／掉／合出来就是几十上百个"的东西，
     * 吃配置里的高堆叠上限（默认 127 —— **安全上限**，理由见那个类的注释：
     * {@code ItemStack.save()} 用 putByte 写 Count，超过 127 存盘会截断）。</p>
     *
     * <p>药品、法宝、秘籍、刷怪蛋、方块物品、灵根符都<b>不</b>走这里，
     * 各自保持原版 64 / 16 / 8 / 4 / 1。</p>
     */
    private static RegistryObject<Item> material(String name, Rarity rarity) {
        return ITEMS.register(name, () -> new StackableMaterialItem(
                new Item.Properties().rarity(rarity)));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
