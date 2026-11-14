package com.hongmeng.xiuxian;

import com.hongmeng.xiuxian.block.BeastPortalFrameBlock;
import com.hongmeng.xiuxian.block.DemonPortalFrameBlock;
import com.hongmeng.xiuxian.block.DungeonPortalBlock;
import com.hongmeng.xiuxian.block.EpochPortalFrameBlock;
import com.hongmeng.xiuxian.block.MeditationCushionBlock;
import com.hongmeng.xiuxian.block.RootAltarBlock;
import com.hongmeng.xiuxian.block.SpiritGrassBlock;
import com.hongmeng.xiuxian.block.VoidPortalFrameBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, HongmengMod.MOD_ID);

    /** 打坐蒲团：右键坐下开始吐纳。 */
    public static final RegistryObject<Block> MEDITATION_CUSHION =
            BLOCKS.register("meditation_cushion", () -> new MeditationCushionBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_YELLOW)
                            .strength(0.6F)
                            .sound(SoundType.WOOL)
                            .noOcclusion()
                            .ignitedByLava()));

    /** 灵草：随处可见的灵气节点。 */
    public static final RegistryObject<Block> SPIRIT_GRASS =
            BLOCKS.register("spirit_grass", () -> new SpiritGrassBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .noCollission()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .offsetType(BlockBehaviour.OffsetType.XZ)
                            .pushReaction(PushReaction.DESTROY)));

    /** 测灵台：站着不动重新测一次灵根（保底重抽）。 */
    public static final RegistryObject<Block> ROOT_ALTAR =
            BLOCKS.register("root_altar", () -> new RootAltarBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));

    /** 纪元之门框架：搭成门形，点燃后开启纪元任务副本。 */
    public static final RegistryObject<Block> EPOCH_PORTAL_FRAME =
            BLOCKS.register("epoch_portal_frame", () -> new EpochPortalFrameBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DEEPSLATE)
                            .strength(8.0F, 1200.0F)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()));

    // ------------------------------------------------------------------
    // 三座新副本的门
    //
    // 点火只负责"把门内填成 dungeon_portal"，真正传送发生在玩家走进门里
    // （见 DungeonPortalBlock#entityInside）。三扇门共用一个传送门方块，
    // 目标副本写在方块状态里。
    // ------------------------------------------------------------------

    /** 秘境传送门：走进即传送。半透明、不挡路、拆了不掉落。 */
    public static final RegistryObject<Block> DUNGEON_PORTAL =
            BLOCKS.register("dungeon_portal", () -> new DungeonPortalBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(0.5F)
                            .sound(SoundType.GLASS)
                            .noCollission()
                            .noOcclusion()
                            .lightLevel(state -> 11)
                            .pushReaction(PushReaction.BLOCK)
                            .noLootTable()));

    /** 妖兽之门框架：妖兽秘境（门槛金丹）。 */
    public static final RegistryObject<Block> BEAST_PORTAL_FRAME =
            BLOCKS.register("beast_portal_frame", () -> new BeastPortalFrameBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .strength(8.0F, 1200.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));

    /** 魔道之门框架：魔道巢穴（门槛道印）。 */
    public static final RegistryObject<Block> DEMON_PORTAL_FRAME =
            BLOCKS.register("demon_portal_frame", () -> new DemonPortalFrameBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.NETHER)
                            .strength(9.0F, 1200.0F)
                            .sound(SoundType.NETHER_BRICKS)
                            .requiresCorrectToolForDrops()));

    /** 虚空之门框架：虚空裂隙（门槛仙人）。 */
    public static final RegistryObject<Block> VOID_PORTAL_FRAME =
            BLOCKS.register("void_portal_frame", () -> new VoidPortalFrameBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(10.0F, 1200.0F)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()));

    // ------------------------------------------------------------------
    // 矿石：按稀有度分层分布（生成规则在 data/hongmeng/worldgen/）
    //
    // 注意 DropExperienceBlock 的构造顺序是 (Properties, UniformInt)——
    // 写反了会编译不过。
    // ------------------------------------------------------------------

    /** 灵铁矿：最常见，是新手期的基础材料，也能在坊市换灵叶。 */
    /** 恶之祭坛：四个 boss 掉落物的召唤台。 */
    public static final RegistryObject<Block> EVIL_ALTAR =
            BLOCKS.register("evil_altar", () -> new
                    com.hongmeng.xiuxian.block.EvilAltarBlock(
                    BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)
                            .lightLevel(s -> 7)
                            .strength(12.0F, 1200.0F)
                            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> SPIRIT_IRON_ORE =
            BLOCKS.register("spirit_iron_ore", () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_ORE),
                    net.minecraft.util.valueproviders.UniformInt.of(1, 3)));

    // ---- Boss 召唤祭坛（玩家要求：像末影龙那样"右键特殊方块触发 boss"）----
    /** 通用祭坛：按所在维度挑 boss。 */
    public static final RegistryObject<Block> BOSS_SUMMON_ALTAR =
            BLOCKS.register("boss_summon_altar", () -> new com.hongmeng.xiuxian.block
                    .BossSummonBlock(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS)
                    .strength(4.0F, 1200.0F).lightLevel(st -> st.getValue(
                            com.hongmeng.xiuxian.block.BossSummonBlock.LIT) ? 12 : 3)));
    /** 纪元主题。 */
    public static final RegistryObject<Block> BOSS_SUMMON_ALTAR_EPOCH =
            BLOCKS.register("boss_summon_altar_epoch", () -> new com.hongmeng.xiuxian.block
                    .BossSummonBlock(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS)
                    .strength(4.0F, 1200.0F).lightLevel(st -> st.getValue(
                            com.hongmeng.xiuxian.block.BossSummonBlock.LIT) ? 12 : 3), "epoch"));
    /** 妖兽主题。 */
    public static final RegistryObject<Block> BOSS_SUMMON_ALTAR_BEAST =
            BLOCKS.register("boss_summon_altar_beast", () -> new com.hongmeng.xiuxian.block
                    .BossSummonBlock(BlockBehaviour.Properties.copy(Blocks.BONE_BLOCK)
                    .strength(4.0F, 1200.0F).lightLevel(st -> st.getValue(
                            com.hongmeng.xiuxian.block.BossSummonBlock.LIT) ? 12 : 3), "beast"));
    /** 魔道主题。 */
    public static final RegistryObject<Block> BOSS_SUMMON_ALTAR_DEMON =
            BLOCKS.register("boss_summon_altar_demon", () -> new com.hongmeng.xiuxian.block
                    .BossSummonBlock(BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE)
                    .strength(4.0F, 1200.0F).lightLevel(st -> st.getValue(
                            com.hongmeng.xiuxian.block.BossSummonBlock.LIT) ? 12 : 3), "demon"));
    /** 虚空主题。 */
    public static final RegistryObject<Block> BOSS_SUMMON_ALTAR_VOID =
            BLOCKS.register("boss_summon_altar_void", () -> new com.hongmeng.xiuxian.block
                    .BossSummonBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)
                    .strength(4.0F, 1200.0F).lightLevel(st -> st.getValue(
                            com.hongmeng.xiuxian.block.BossSummonBlock.LIT) ? 12 : 3), "void"));

    /** 深板岩灵铁矿：y 偏低的变种。 */
    public static final RegistryObject<Block> DEEPSLATE_SPIRIT_IRON_ORE =
            BLOCKS.register("deepslate_spirit_iron_ore",
                    () -> new DropExperienceBlock(
                            BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE),
                            net.minecraft.util.valueproviders.UniformInt.of(1, 3)));

    /**
     * <b>真元晶矿</b>：只在<b>真世界开启后</b>出现的新矿种。
     *
     * <p>它不由 worldgen 生成（worldgen 读不到运行时开关），
     * 而是由 {@code HardmodeTerrain} 在玩家脚下<b>回溯播种</b> ——
     * 这才是"肉后新矿"：旧区块也会长出来。</p>
     */
    public static final RegistryObject<Block> ZHENYUAN_ORE =
            BLOCKS.register("zhenyuan_ore",
                    () -> new DropExperienceBlock(
                            BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE),
                            net.minecraft.util.valueproviders.UniformInt.of(2, 5)));

    /** 灵晶矿：少见，炼器与坊市的中档材料。 */
    public static final RegistryObject<Block> SPIRIT_CRYSTAL_ORE =
            BLOCKS.register("spirit_crystal_ore",
                    () -> new DropExperienceBlock(
                            BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE),
                            net.minecraft.util.valueproviders.UniformInt.of(2, 5)));

    /** 深板岩灵晶矿。 */
    public static final RegistryObject<Block> DEEPSLATE_SPIRIT_CRYSTAL_ORE =
            BLOCKS.register("deepslate_spirit_crystal_ore",
                    () -> new DropExperienceBlock(
                            BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE),
                            net.minecraft.util.valueproviders.UniformInt.of(2, 5)));

    /** 空冥矿：稀有，深处才有。 */
    public static final RegistryObject<Block> KONGMING_ORE =
            BLOCKS.register("kongming_ore",
                    () -> new DropExperienceBlock(
                            BlockBehaviour.Properties.copy(Blocks.ANCIENT_DEBRIS),
                            net.minecraft.util.valueproviders.UniformInt.of(3, 7)));

    /** 血罗矿：极稀有，只在特定高度成簇出现。 */
    public static final RegistryObject<Block> XUELUO_ORE =
            BLOCKS.register("xueluo_ore",
                    () -> new DropExperienceBlock(
                            BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE),
                            net.minecraft.util.valueproviders.UniformInt.of(4, 9)));

    // ------------------------------------------------------------------
    // 灵植与炼丹（灵植 / 灵田土 / 丹炉）
    //
    // 六阶灵植都是 SpiritHerbBlock（原版 CropBlock 的灵植版，有 age 0..7）：
    //   tier 1 xuelian_grass 雪莲草   回灵 / 疗伤
    //   tier 2 huoyan_hua    火焰花   攻击 / 火系
    //   tier 3 hanbing_teng  寒冰藤   控制 / 冰系
    //   tier 4 leiyin_cao    雷音草   雷系 / 突破
    //   tier 5 kongming_lan  空冥兰   高阶 / 空间
    //   tier 6 xueluo_zhi    血珞芝   顶级 / 血道
    //
    // **构造期绝对不能 .get()**：这几行是在注册方块时执行的，那时物品还没注册完，
    // 所以种子的 RegistryObject 只能以 Supplier 的形式传进去（SpiritHerbBlock
    // 只在掉落/骨粉等运行时才解引用）。踩过的坑：构造期 .get() 会抛
    // "Registry Object not present"，整个模组加载失败。
    // ------------------------------------------------------------------

    /** 一阶 · 雪莲草：回灵 / 疗伤。最容易种，也最常用。 */
    public static final RegistryObject<Block> XUELIAN_GRASS =
            BLOCKS.register("xuelian_grass", () -> new com.hongmeng.xiuxian.block.SpiritHerbBlock(
                    herbProperties(), com.hongmeng.xiuxian.alchemy.HerbCatalog.XUELIAN_GRASS,
                    com.hongmeng.xiuxian.item.ModItems.XUELIAN_GRASS_SEED,
                    com.hongmeng.xiuxian.item.ModItems.XUELIAN_GRASS_PETAL));

    /** 二阶 · 火焰花：攻击 / 火系丹药的主料。 */
    public static final RegistryObject<Block> HUOYAN_HUA =
            BLOCKS.register("huoyan_hua", () -> new com.hongmeng.xiuxian.block.SpiritHerbBlock(
                    herbProperties(), com.hongmeng.xiuxian.alchemy.HerbCatalog.HUOYAN_HUA,
                    com.hongmeng.xiuxian.item.ModItems.HUOYAN_HUA_SEED,
                    com.hongmeng.xiuxian.item.ModItems.HUOYAN_HUA_PETAL));

    /** 三阶 · 寒冰藤：控制 / 冰系。 */
    public static final RegistryObject<Block> HANBING_TENG =
            BLOCKS.register("hanbing_teng", () -> new com.hongmeng.xiuxian.block.SpiritHerbBlock(
                    herbProperties(), com.hongmeng.xiuxian.alchemy.HerbCatalog.HANBING_TENG,
                    com.hongmeng.xiuxian.item.ModItems.HANBING_TENG_SEED,
                    com.hongmeng.xiuxian.item.ModItems.HANBING_TENG_PETAL));

    /** 四阶 · 雷音草：雷系 / 突破丹。 */
    public static final RegistryObject<Block> LEIYIN_CAO =
            BLOCKS.register("leiyin_cao", () -> new com.hongmeng.xiuxian.block.SpiritHerbBlock(
                    herbProperties(), com.hongmeng.xiuxian.alchemy.HerbCatalog.LEIYIN_CAO,
                    com.hongmeng.xiuxian.item.ModItems.LEIYIN_CAO_SEED,
                    com.hongmeng.xiuxian.item.ModItems.LEIYIN_CAO_PETAL));

    /** 五阶 · 空冥兰：高阶 / 空间。 */
    public static final RegistryObject<Block> KONGMING_LAN =
            BLOCKS.register("kongming_lan", () -> new com.hongmeng.xiuxian.block.SpiritHerbBlock(
                    herbProperties(), com.hongmeng.xiuxian.alchemy.HerbCatalog.KONGMING_LAN,
                    com.hongmeng.xiuxian.item.ModItems.KONGMING_LAN_SEED,
                    com.hongmeng.xiuxian.item.ModItems.KONGMING_LAN_PETAL));

    /** 六阶 · 血珞芝：顶级 / 血道。最难种，也最贵。 */
    public static final RegistryObject<Block> XUELUO_ZHI =
            BLOCKS.register("xueluo_zhi", () -> new com.hongmeng.xiuxian.block.SpiritHerbBlock(
                    herbProperties(), com.hongmeng.xiuxian.alchemy.HerbCatalog.XUELUO_ZHI,
                    com.hongmeng.xiuxian.item.ModItems.XUELUO_ZHI_SEED,
                    com.hongmeng.xiuxian.item.ModItems.XUELUO_ZHI_PETAL));

    /** 灵田土：灵植种在上面生长概率 ×3（见 SpiritHerbBlock#growthChanceOn）。 */
    public static final RegistryObject<Block> LINGTIAN_SOIL =
            BLOCKS.register("lingtian_soil", () -> new com.hongmeng.xiuxian.block.LingtianSoilBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DIRT)
                            .strength(0.6F)
                            .sound(SoundType.GRAVEL)));

    /** 丹炉：右键开界面，主料 + 辅料 → 丹药。 */
    public static final RegistryObject<Block> ALCHEMY_FURNACE =
            BLOCKS.register("alchemy_furnace", () -> new com.hongmeng.xiuxian.alchemy.AlchemyFurnaceBlock(
                    com.hongmeng.xiuxian.alchemy.AlchemyFurnaceBlock.furnaceProperties()));

    // ==================================================================
    // 2026-11-07 · 灵石矿脉（修为经济线，玩家要求）
    //
    // 为什么追加在文件末尾：注册表 ID 按注册顺序分配，中间插入会让后面所有
    // 方块的 ID 前移、老存档错位。新内容一律往后加（见 00-总工程师总纲 §7.3）。
    // ==================================================================

    /**
     * <b>灵石矿</b>：主世界地下成脉生成，挖出「灵石」。
     *
     * <p>掉落走 {@code loot_tables/blocks/spirit_stone_ore.json}（精准采集掉本体、
     * 否则掉 1~3 颗灵石 + 时运加成）；经验由 {@link DropExperienceBlock} 给。
     * 生成规则见 {@code worldgen/configured_feature|placed_feature/spirit_stone_ore.json}
     * 与 {@code forge/biome_modifier/add_spirit_stone_ore*.json}。</p>
     */
    public static final RegistryObject<Block> SPIRIT_STONE_ORE =
            BLOCKS.register("spirit_stone_ore", () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_ORE),
                    net.minecraft.util.valueproviders.UniformInt.of(1, 3)));

    // ==================================================================
    // 2026-11-07 第二批 · 深板岩矿变体（玩家：「完善一下方块，矿要有对应的矿可以采」）
    //
    // 为什么必须有：空冥矿/血罗矿的生成高度是 -60~-8 与 -60~16，**几乎全在深板岩层**，
    // 但它们的 configured feature 只替换 `stone_ore_replaceables` —— 深板岩层里
    // 一格也不生成，玩家自然觉得"没有矿可采"。加上深板岩变体后这两种矿才真正存在；
    // 灵石矿同理：主世界 -24~-8 段也是深板岩，补一个变体把这段覆盖上。
    // ==================================================================

    /** 深板岩空冥矿：空冥石 / 空冥精的采集来源（深层）。 */
    public static final RegistryObject<Block> DEEPSLATE_KONGMING_ORE =
            BLOCKS.register("deepslate_kongming_ore", () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE),
                    net.minecraft.util.valueproviders.UniformInt.of(2, 5)));

    /** 深板岩血罗矿：血珞精的采集来源（深层）。 */
    public static final RegistryObject<Block> DEEPSLATE_XUELUO_ORE =
            BLOCKS.register("deepslate_xueluo_ore", () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE),
                    net.minecraft.util.valueproviders.UniformInt.of(1, 3)));

    /** 深板岩灵石矿：深层的灵石来源。 */
    public static final RegistryObject<Block> DEEPSLATE_SPIRIT_STONE_ORE =
            BLOCKS.register("deepslate_spirit_stone_ore", () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE),
                    net.minecraft.util.valueproviders.UniformInt.of(1, 3)));


    /** 六阶灵植共用的方块属性（注册名各自内联，方便 tools/*.js 按 BLOCKS.register 扫）。 */
    private static BlockBehaviour.Properties herbProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
