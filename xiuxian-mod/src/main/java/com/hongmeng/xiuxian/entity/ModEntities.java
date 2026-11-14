package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HongmengMod.MOD_ID);

    /** 门派执事：叛门之后被派来清理门户的追杀者。 */
    public static final RegistryObject<EntityType<GuiltySectDiscipleEntity>> GUILTY_SECT_DISCIPLE =
            ENTITIES.register("guilty_sect_disciple", () -> EntityType.Builder
                    .of(GuiltySectDiscipleEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("guilty_sect_disciple"));

    // ------------------------------------------------------------------
    // 修仙界五阶野怪（品阶见 MobRank）
    // ------------------------------------------------------------------

    /** 灵狼：最快的杂鱼，会扑击。 */
    public static final RegistryObject<EntityType<SpiritWolfEntity>> SPIRIT_WOLF =
            ENTITIES.register("spirit_wolf", () -> EntityType.Builder
                    .of(SpiritWolfEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.85F)
                    .clientTrackingRange(10)
                    .build("spirit_wolf"));

    /** 血魔修士：精英，剑气能递出五格。 */
    public static final RegistryObject<EntityType<BloodDemonCultivatorEntity>> BLOOD_DEMON_CULTIVATOR =
            ENTITIES.register("blood_demon_cultivator", () -> EntityType.Builder
                    .of(BloodDemonCultivatorEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("blood_demon_cultivator"));

    /** 恶：天空裂口里的巨大存在（OVERLORD 品阶，三阶段）。 */
    public static final RegistryObject<EntityType<EvilGazerEntity>> EVIL_GAZER =
            ENTITIES.register("evil_gazer", () -> EntityType.Builder
                    .of(EvilGazerEntity::new, MobCategory.MONSTER)
                    .sized(8.0F, 21.0F)
                    .clientTrackingRange(16)
                    .build("evil_gazer"));

    /** 焚天炎帝：下界终局 boss（BOSS 品阶）。 */
    public static final RegistryObject<EntityType<BlazeEmperorEntity>>
            BLAZE_EMPEROR =
            ENTITIES.register("blaze_emperor", () -> EntityType.Builder
                    .of(BlazeEmperorEntity::new, MobCategory.MONSTER)
                    // 模型高 3.69 格 / 宽 3.12 格，命中箱按模型给
                    .sized(5.4F, 6.7F)
                    .clientTrackingRange(12)
                    .build("blaze_emperor"));

    /** 虚空龙帝：末地终局 boss（OVERLORD 品阶）。 */
    public static final RegistryObject<EntityType<VoidDragonEmperorEntity>>
            VOID_DRAGON_EMPEROR =
            ENTITIES.register("void_dragon_emperor", () -> EntityType.Builder
                    .of(VoidDragonEmperorEntity::new, MobCategory.MONSTER)
                    // 模型高 3.06 格 / 翼展 7.5 格，命中箱取躯干
                    .sized(3.5F, 9.1F)
                    .clientTrackingRange(12)
                    .build("void_dragon_emperor"));

    /** 劫灰修士：真世界专属精英（渡劫失败者的残躯）。 */
    public static final RegistryObject<EntityType<TribulationAshCultivatorEntity>>
            TRIBULATION_ASH_CULTIVATOR =
            ENTITIES.register("tribulation_ash_cultivator", () -> EntityType.Builder
                    .of(TribulationAshCultivatorEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("tribulation_ash_cultivator"));

    /** 傀儡队长：队长，重甲抗击退。 */
    public static final RegistryObject<EntityType<PuppetCaptainEntity>> PUPPET_CAPTAIN =
            ENTITIES.register("puppet_captain", () -> EntityType.Builder
                    .of(PuppetCaptainEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(10)
                    .build("puppet_captain"));

    /** 魔道统领：头目，火免。 */
    public static final RegistryObject<EntityType<DemonGeneralEntity>> DEMON_GENERAL =
            ENTITIES.register("demon_general", () -> EntityType.Builder
                    .of(DemonGeneralEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 2.4F)
                    .clientTrackingRange(12)
                    .build("demon_general"));

    /** 虚空道主：首领，火免且完全免疫击退。 */
    public static final RegistryObject<EntityType<VoidOverlordEntity>> VOID_OVERLORD =
            ENTITIES.register("void_overlord", () -> EntityType.Builder
                    .of(VoidOverlordEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.8F)
                    .clientTrackingRange(14)
                    .build("void_overlord"));

    // ------------------------------------------------------------------
    // 祭坛首领：四个副本各一只**专属实体**
    //
    // 玩家要求「单独的模型呢？」—— 光有模型不够，实体本身也得是新的，
    // 否则召唤出来的还是原来那只小怪（属性、掉落、名字完全一样）。
    //
    // 它们各自配一套**独立骨架** + 专属贴图：
    //   EPOCH_BOSS  纪元·傀儡祖师 —— 冠冕 + 披风         （高护甲、慢）
    //   BEAST_BOSS  妖兽·万兽之主 —— 魔角 + 兽尾         （高血高攻、怕火）
    //   DEMON_BOSS  魔道·血魔老祖 —— 魔角 + 四臂 + 兽尾 + 披风（吸血、火免）
    //   VOID_BOSS   虚空·虚空道主 —— 冠冕 + 魔角 + 四臂 + 巨翼 + 披风（首领、火免）
    // ------------------------------------------------------------------

    /** 纪元 · 傀儡祖师（头目）。 */
    public static final RegistryObject<EntityType<EpochBossEntity>> EPOCH_BOSS =
            ENTITIES.register("epoch_boss", () -> EntityType.Builder
                    .of(EpochBossEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.6F)
                    .clientTrackingRange(14)
                    .build("epoch_boss"));

    /** 妖兽 · 万兽之主（头目）。 */
    public static final RegistryObject<EntityType<BeastBossEntity>> BEAST_BOSS =
            ENTITIES.register("beast_boss", () -> EntityType.Builder
                    .of(BeastBossEntity::new, MobCategory.MONSTER)
                    .sized(4.6F, 4.95F)
                    .clientTrackingRange(14)
                    .build("beast_boss"));

    /** 魔道 · 血魔老祖（头目）。 */
    public static final RegistryObject<EntityType<DemonBossEntity>> DEMON_BOSS =
            ENTITIES.register("demon_boss", () -> EntityType.Builder
                    .of(DemonBossEntity::new, MobCategory.MONSTER)
                    .sized(5.2F, 7.4F)
                    .clientTrackingRange(14)
                    .build("demon_boss"));

    /** 虚空 · 虚空道主（首领）。 */
    public static final RegistryObject<EntityType<VoidBossEntity>> VOID_BOSS =
            ENTITIES.register("void_boss", () -> EntityType.Builder
                    .of(VoidBossEntity::new, MobCategory.MONSTER)
                    .sized(4.5F, 8.9F)
                    .clientTrackingRange(16)
                    .build("void_boss"));

    // ------------------------------------------------------------------
    // 第二批妖兽：四种各自带独立模型的新怪（精英 / 队长 / 队长 / 头目）
    //
    // 尺寸口径：模型盒子的排版定死之后，实体高度就跟着模型走——
    // 烈焰猿模型 47 像素 = 2.9 格、石傀 44 像素 = 2.8 格、雷鹰 33 像素 = 1.9 格、
    // 虚空蛟身体只占 0.9 格（长条怪的长度不进命中箱，否则贴着脸反而打不中）。
    // ------------------------------------------------------------------

    /** 烈焰猿：精英，火免，会跳起来捶地。 */
    public static final RegistryObject<EntityType<FlameApeEntity>> FLAME_APE =
            ENTITIES.register("flame_ape", () -> EntityType.Builder
                    .of(FlameApeEntity::new, MobCategory.MONSTER)
                    .sized(1.5F, 2.9F)
                    // 火免也写在实体类型上：这样连岩浆的"着火"判定都直接跳过，
                    // 不必等实体类里的 fireImmune() 生效
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("flame_ape"));

    /** 雷鹰：队长，会飞，会俯冲。 */
    public static final RegistryObject<EntityType<ThunderEagleEntity>> THUNDER_EAGLE =
            ENTITIES.register("thunder_eagle", () -> EntityType.Builder
                    .of(ThunderEagleEntity::new, MobCategory.MONSTER)
                    .sized(1.5F, 1.9F)
                    .clientTrackingRange(12)
                    .build("thunder_eagle"));

    /** 石傀：队长，极高生命 + 近乎免疫击退，但慢。 */
    public static final RegistryObject<EntityType<StoneGolemEntity>> STONE_GOLEM =
            ENTITIES.register("stone_golem", () -> EntityType.Builder
                    .of(StoneGolemEntity::new, MobCategory.MONSTER)
                    // 1.8 格宽：模型两只拳头外缘在 ±0.94 格，
                    // 命中箱贴到 1.8 才不至于"看着打中了其实没打中"
                    .sized(1.8F, 2.8F)
                    .clientTrackingRange(14)
                    .build("stone_golem"));

    /** 虚空蛟：头目，八节蛇身，咬中带毒。 */
    public static final RegistryObject<EntityType<VoidSerpentEntity>> VOID_SERPENT =
            ENTITIES.register("void_serpent", () -> EntityType.Builder
                    .of(VoidSerpentEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 1.4F)
                    .clientTrackingRange(16)
                    .build("void_serpent"));

    // ------------------------------------------------------------------
    // 第三批妖兽：两只按"职能分化"补上空白的怪（见 docs/spirit-beast-spec.md）
    //
    // 尺寸口径：模型脚底一律落在 y=24（24 像素 = 1.5 格），再乘品阶体型倍率：
    //   土行貂 模型高 0.94 格 × 精英 1.05 ≈ 0.99 → sized 0.8 × 1.0
    //   毒涎蟾 模型高 1.00 格 × 队长 1.12 ≈ 1.12 → sized 1.2 × 1.15
    // 命中箱刻意贴着"放大后的模型"，否则玩家会觉得"明明打中了却没中"。
    // ------------------------------------------------------------------

    /** 土行貂：精英，遁地伏击——地下不可见但可被打到，破土一击带减速。 */
    public static final RegistryObject<EntityType<BurrowMartenEntity>> BURROW_MARTEN =
            ENTITIES.register("burrow_marten", () -> EntityType.Builder
                    .of(BurrowMartenEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.0F)
                    .clientTrackingRange(12)
                    .build("burrow_marten"));

    /** 毒涎蟾：队长，远程喷毒——主动保持 4~7 格，近战很弱。 */
    public static final RegistryObject<EntityType<VenomToadEntity>> VENOM_TOAD =
            ENTITIES.register("venom_toad", () -> EntityType.Builder
                    .of(VenomToadEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 1.15F)
                    .clientTrackingRange(12)
                    .build("venom_toad"));

    /**
     * 毒涎（弹体）。
     *
     * <p>与 {@code jiaolong_spit} 同样的口径：{@code noSave()}（弹体是纯运行期的东西）、
     * 8 格的追踪距离足够，且<b>不注册属性</b>——它不是 {@code LivingEntity}。</p>
     */
    public static final RegistryObject<EntityType<VenomSpitEntity>> VENOM_SPIT =
            ENTITIES.register("venom_spit", () -> EntityType.Builder
                    .<VenomSpitEntity>of(VenomSpitEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(4)
                    .noSave()
                    .build("venom_spit"));

    // ------------------------------------------------------------------
    // 第四批：更细分的怪（人形 4 只 + 妖兽 3 只）
    //
    // 尺寸口径与前面几批一致：
    //   * 人形怪走 CultivationHumanoidModel，原版人形六件套 + 发髻等自建件，
    //     模型脚底在 y=24（1.5 格），所以"模型高度 = 1.5 格 + 头高"——
    //     与既有的血魔修士/傀儡队长完全同尺，命中箱照旧取 0.6~0.7 × 1.95~2.4；
    //   * 妖兽复用既有骨架（灵狼 64×32 / 虚空蛟 128×128 / 雷鹰 128×128），
    //     所以命中箱也直接沿用被复用的那一只的口径（赤炎狼 = 灵狼、
    //     雷纹鹰 = 雷鹰），只有玄冰蟒按"队长比头目小一圈"手动收窄。
    //
    // 全部走 MobCategory.MONSTER：它们要接 MobScaling 的境界缩放，
    // 而那个只看 Monster（见 MobScaling.onEntityJoin）。
    // ------------------------------------------------------------------

    /** 魔道执事（精英）：人形系最瘦最小，残血会喊同伙。 */
    public static final RegistryObject<EntityType<DemonStewardEntity>> DEMON_STEWARD =
            ENTITIES.register("demon_steward", () -> EntityType.Builder
                    .of(DemonStewardEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("demon_steward"));

    /** 血魔长老（队长）：咬中吸血，体型比血魔修士壮一圈。 */
    public static final RegistryObject<EntityType<BloodDemonElderEntity>> BLOOD_DEMON_ELDER =
            ENTITIES.register("blood_demon_elder", () -> EntityType.Builder
                    .of(BloodDemonElderEntity::new, MobCategory.MONSTER)
                    .sized(0.65F, 2.0F)
                    .clientTrackingRange(10)
                    .build("blood_demon_elder"));

    /** 虚空行者（头目）：被打中时有概率瞬移到攻击者背后。 */
    public static final RegistryObject<EntityType<VoidWalkerEntity>> VOID_WALKER =
            ENTITIES.register("void_walker", () -> EntityType.Builder
                    .of(VoidWalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.68F, 2.35F)
                    .clientTrackingRange(12)
                    .build("void_walker"));

    /** 剑冢守灵（队长）：慢、硬、半血剑意爆发后提速。 */
    public static final RegistryObject<EntityType<SwordTombGuardianEntity>> SWORD_TOMB_GUARDIAN =
            ENTITIES.register("sword_tomb_guardian", () -> EntityType.Builder
                    .of(SwordTombGuardianEntity::new, MobCategory.MONSTER)
                    .sized(0.62F, 2.1F)
                    .clientTrackingRange(10)
                    .build("sword_tomb_guardian"));

    /**
     * 赤炎狼（精英）：灵狼的火行亚种。
     *
     * <p>尺寸与 {@code spirit_wolf} 完全一致——它用的就是灵狼那套四足骨架
     * （{@code SpiritWolfRenderer.WolfBody}），命中箱另取一个数只会让"看着打中了却没中"。</p>
     */
    public static final RegistryObject<EntityType<CrimsonFlameWolfEntity>> CRIMSON_FLAME_WOLF =
            ENTITIES.register("crimson_flame_wolf", () -> EntityType.Builder
                    .of(CrimsonFlameWolfEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.85F)
                    // 火行亚种：连岩浆的"着火"判定都跳过（与烈焰猿同一个口径）
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("crimson_flame_wolf"));

    /**
     * 玄冰蟒（队长）：虚空蛟的寒冰亚种，八节蛇身复用同一套骨架。
     *
     * <p><b>命中箱与虚空蛟逐字相同（1.6 × 1.4），这是刻意的。</b>
     * 第一版把队长的小体型也写进了命中箱（1.3 × 1.2），被
     * {@code tools/check_beast_hitboxes.py} 拦下了：命中箱必须跟着<b>模型</b>走，
     * 而模型是同一份——"看得见的身体比打得中的判定长"正是那条脚本要防的毛病。
     * 「队长比头目小一圈」由 {@code MobRank.scaleXZ()} 在<b>渲染层</b>表达，
     * 不重复写进命中箱。</p>
     */
    public static final RegistryObject<EntityType<FrostPythonEntity>> FROST_PYTHON =
            ENTITIES.register("frost_python", () -> EntityType.Builder
                    .of(FrostPythonEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 1.4F)
                    .clientTrackingRange(14)
                    .build("frost_python"));

    /** 雷纹鹰（精英）：雷鹰骨架的地面变体，不会飞，靠贴地短扑。 */
    public static final RegistryObject<EntityType<StormEagleEntity>> STORM_EAGLE =
            ENTITIES.register("storm_eagle", () -> EntityType.Builder
                    .of(StormEagleEntity::new, MobCategory.MONSTER)
                    // 与雷鹰同尺：骨架一模一样，站立姿态的包围盒也是同一份
                    .sized(1.5F, 1.9F)
                    .clientTrackingRange(12)
                    .build("storm_eagle"));

    // ------------------------------------------------------------------
    // NPC：门派长老 / 炼丹师 / 云游商人
    //
    // 用 MobCategory.CREATURE：他们算"动物"那一档，不占怪物的刷怪上限，
    // 也不会被 MobScaling 的境界缩放碰（那个只看 Monster）。
    // 他们没有自然生成规则（不进 biome_modifier，也不注册 SpawnPlacements）——
    // 只由结构 NBT 里的实体列表放置。
    // ------------------------------------------------------------------

    /** 门派长老：给提示的长者。 */
    public static final RegistryObject<EntityType<SectElderEntity>> SECT_ELDER =
            ENTITIES.register("sect_elder", () -> EntityType.Builder
                    .of(SectElderEntity::new, MobCategory.CREATURE)
                    .sized(0.62F, 1.94F)
                    .clientTrackingRange(10)
                    .build("sect_elder"));

    /** 炼丹师：丹药闲谈。 */
    public static final RegistryObject<EntityType<AlchemyMasterEntity>> ALCHEMY_MASTER =
            ENTITIES.register("alchemy_master", () -> EntityType.Builder
                    .of(AlchemyMasterEntity::new, MobCategory.CREATURE)
                    .sized(0.66F, 1.86F)
                    .clientTrackingRange(10)
                    .build("alchemy_master"));

    /** 云游商人：右键直接开坊市。 */
    public static final RegistryObject<EntityType<WanderingMerchantEntity>> WANDERING_MERCHANT =
            ENTITIES.register("wandering_merchant", () -> EntityType.Builder
                    .of(WanderingMerchantEntity::new, MobCategory.CREATURE)
                    .sized(0.64F, 1.98F)
                    .clientTrackingRange(10)
                    .build("wandering_merchant"));

    // ------------------------------------------------------------------
    // 第四批 NPC：丹药商人 / 炼器师 / 秘境向导 / 悬赏牌看板
    //
    // 与上面三位同一套口径：MobCategory.CREATURE（不占怪物刷怪上限、
    // 也不会被 MobScaling 的境界缩放碰）、不注册刷怪规则、只由结构放置。
    // ------------------------------------------------------------------

    /** 丹药商人：右键直接开坊市（与云游商人同一条逻辑）。 */
    public static final RegistryObject<EntityType<PillMerchantEntity>> PILL_MERCHANT =
            ENTITIES.register("pill_merchant", () -> EntityType.Builder
                    .of(PillMerchantEntity::new, MobCategory.CREATURE)
                    .sized(0.68F, 1.86F)
                    .clientTrackingRange(10)
                    .build("pill_merchant"));

    /** 炼器师：说法宝升阶与修理的门道，见面送一块灵铁锭。 */
    public static final RegistryObject<EntityType<ArtifactRefinerEntity>> ARTIFACT_REFINER =
            ENTITIES.register("artifact_refiner", () -> EntityType.Builder
                    .of(ArtifactRefinerEntity::new, MobCategory.CREATURE)
                    .sized(0.72F, 1.96F)
                    .clientTrackingRange(10)
                    .build("artifact_refiner"));

    /** 秘境向导：报四个维度的门槛、波次与开门材料。 */
    public static final RegistryObject<EntityType<RealmGuideEntity>> REALM_GUIDE =
            ENTITIES.register("realm_guide", () -> EntityType.Builder
                    .of(RealmGuideEntity::new, MobCategory.CREATURE)
                    .sized(0.62F, 1.96F)
                    .clientTrackingRange(10)
                    .build("realm_guide"));

    /** 悬赏牌看板：一块不会走路的木牌，右键看今天的赏格。 */
    public static final RegistryObject<EntityType<BountyBoardEntity>> BOUNTY_BOARD =
            ENTITIES.register("bounty_board", () -> EntityType.Builder
                    .of(BountyBoardEntity::new, MobCategory.CREATURE)
                    // 比人矮半头、宽一圈：它是一块钉在地上的牌子
                    .sized(0.72F, 1.80F)
                    .clientTrackingRange(10)
                    .build("bounty_board"));

    // ------------------------------------------------------------------
    // 灵兽 / 坐骑（见 docs/spirit-beast-spec.md）
    //
    // 尺寸口径：模型脚底一律落在 y=24（24 像素 = 1.5 格），所以实体高度按
    // "模型像素 / 16" 取整——雷鹰 24 像素 = 1.5 格。
    //
    // 全部走 MobCategory.CREATURE：
    //   * 它们不占怪物的刷怪上限，也就不会把秘境里的妖兽挤掉；
    //   * MobScaling 的境界缩放只看 Monster，灵兽因此不会被境界放大
    //     （玩家的坐骑不该因为主人升境而变成怪物）。
    // ------------------------------------------------------------------


    /** 契约雷鹰：唯一的悬停型飞行坐骑，脆且不能驮货。 */
    public static final RegistryObject<EntityType<com.hongmeng.xiuxian.entity.beast.ContractEagleEntity>>
            CONTRACT_EAGLE =
            ENTITIES.register("contract_eagle", () -> EntityType.Builder
                    .of(com.hongmeng.xiuxian.entity.beast.ContractEagleEntity::new,
                            MobCategory.CREATURE)
                    .sized(1.2F, 1.5F)
                    .clientTrackingRange(12)
                    .build("contract_eagle"));

    /** 蛟龙：飞行 + 空战，契约门槛最高。 */
    public static final RegistryObject<EntityType<com.hongmeng.xiuxian.entity.beast.JiaolongEntity>>
            JIAOLONG =
            ENTITIES.register("jiaolong", () -> EntityType.Builder
                    .of(com.hongmeng.xiuxian.entity.beast.JiaolongEntity::new,
                            MobCategory.CREATURE)
                    .sized(1.7F, 2.2F)
                    .clientTrackingRange(16)
                    .build("jiaolong"));

    /** 契约灵狼：战斗协助，唯一不能被骑的灵兽（来源 spirit_wolf）。 */
    public static final RegistryObject<EntityType<com.hongmeng.xiuxian.entity.beast.ContractWolfEntity>>
            CONTRACT_WOLF =
            ENTITIES.register("contract_wolf", () -> EntityType.Builder
                    .of(com.hongmeng.xiuxian.entity.beast.ContractWolfEntity::new,
                            MobCategory.CREATURE)
                    .sized(0.7F, 0.85F)
                    .clientTrackingRange(10)
                    .build("contract_wolf"));

    /**
     * 蛟龙之息（弹体）。
     *
     * <p>{@code noSave()}：弹体是纯运行期的东西，落盘只会在读档时留下一堆
     * 半空中的毒球。{@code clientTrackingRange(8)} 足够——它飞得快、活得短。</p>
     */
    public static final RegistryObject<EntityType<com.hongmeng.xiuxian.entity.beast.JiaolongSpitEntity>>
            JIAOLONG_SPIT =
            ENTITIES.register("jiaolong_spit", () -> EntityType.Builder
                    .<com.hongmeng.xiuxian.entity.beast.JiaolongSpitEntity>of(
                            com.hongmeng.xiuxian.entity.beast.JiaolongSpitEntity::new,
                            MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(4)
                    .noSave()
                    .build("jiaolong_spit"));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        // 生成规则**只能**走 SpawnPlacementRegisterEvent。
        //
        // 曾经在这里直接调了一次 registerPlacements()（想"双保险"），
        // 结果是**游戏直接起不来**：mod 构造阶段 DeferredRegister 还没被
        // 填充，RegistryObject.get() 会抛
        // "NullPointerException: Registry Object not present: hongmeng:spirit_wolf"，
        // 整个 mod 实例化失败。
        //
        // SpawnPlacementRegisterEvent 在注册表填充之后触发，时机本来就够早——
        // 官方推荐路径，不需要"保险"。
        bus.addListener(ModEntities::onSpawnPlacements);
    }

    /**
     * 刷怪规则：一律按"黑暗处生成的地面怪"处理。
     *
     * <p>真正决定"多久刷一只"的是 {@code data/hongmeng/forge/biome_modifier/add_spawn_*.json}
     * 里的 weight——普通 20、精英 6、队长 2、头目 1、首领 1，越强越罕见。
     * 生成规则只负责"位置合不合法"，不负责频率。</p>
     */
    private static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        registerPlacement(event, SPIRIT_WOLF);
        registerPlacement(event, BLOOD_DEMON_CULTIVATOR);
        registerPlacement(event, TRIBULATION_ASH_CULTIVATOR);
        registerPlacement(event, PUPPET_CAPTAIN);
        registerPlacement(event, DEMON_GENERAL);
        // 祭坛首领（四个副本各一只专属实体）
        registerPlacement(event, EPOCH_BOSS);
        registerPlacement(event, BEAST_BOSS);
        registerPlacement(event, DEMON_BOSS);
        registerPlacement(event, VOID_BOSS);
        registerPlacement(event, VOID_OVERLORD);
        // 第二批妖兽
        registerPlacement(event, FLAME_APE);
        // 雷鹰是飞行怪，但**出生点仍然按地面算**：原版的蝙蝠也是 ON_GROUND，
        // 先站在地上再起飞，比"凭空刷在天上"自然得多
        registerPlacement(event, THUNDER_EAGLE);
        registerPlacement(event, STONE_GOLEM);
        registerPlacement(event, VOID_SERPENT);
        // 第三批妖兽（同样只负责"位置合不合法"，频率看 biome_modifier 的 weight）
        registerPlacement(event, BURROW_MARTEN);
        registerPlacement(event, VENOM_TOAD);
        // 第四批：人形 4 只 + 妖兽 3 只
        //
        // 七只全部自然生成，但**频率与刷怪频率是两件事**：这里只管"位置合不合法"
        // （黑暗中、地面、非液体），真正决定"多久刷一只"的是 biome_modifier 里的 weight
        // （普通 20 / 精英 6 / 队长 2 / 头目 1）。
        registerPlacement(event, DEMON_STEWARD);
        registerPlacement(event, BLOOD_DEMON_ELDER);
        registerPlacement(event, VOID_WALKER);
        registerPlacement(event, SWORD_TOMB_GUARDIAN);
        registerPlacement(event, CRIMSON_FLAME_WOLF);
        registerPlacement(event, FROST_PYTHON);
        // 雷纹鹰是**地面**怪（它不会飞，见 StormEagleEntity 的说明）：
        // 用 Monster::checkMonsterSpawnRules 正好，不需要任何飞行特例
        registerPlacement(event, STORM_EAGLE);
        // ---- 灵兽（见 docs/spirit-beast-spec.md）----
        //
        // 温顺动物走**动物**那条规则（需要亮度、草方块），
        // 所以用 Animal::checkAnimalSpawnRules，而不是 Monster 那一套。
        //
        // 雷鹰（契约后）与蛟龙**不注册**刷怪规则：它们只能由契灵符收服
        // （雷鹰的来源妖兽 thunder_eagle 已经在上面注册过了，蛟龙同理），
        // 野外直接刷一只驯好的坐骑会让"契约"这个玩法整个失去意义。
        // NPC 不注册刷怪规则：他们只由结构放置，不走自然生成
    }

    /**
     * 温顺动物的刷怪规则。
     *
     * <p>与 {@link #registerPlacement} 分开是必须的：{@code Monster}
     * 那一条会做"光照 &lt; 7 才刷"的判断，而温顺动物不是——
     * 用怪物规则会让它只在漆黑的洞里出现。</p>
     */
    private static void registerAnimalPlacement(SpawnPlacementRegisterEvent event,
                                                RegistryObject<? extends EntityType<? extends net.minecraft.world.entity.animal.Animal>> holder) {
        if (!holder.isPresent()) {
            return;
        }
        EntityType<? extends net.minecraft.world.entity.animal.Animal> type = holder.get();
        event.register(type, SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    /** 走事件的注册：拿 RegistryObject 本身，不提前解引用。 */
    private static void registerPlacement(SpawnPlacementRegisterEvent event,
                                          RegistryObject<? extends EntityType<? extends Monster>> holder) {
        if (!holder.isPresent()) {
            return;
        }
        EntityType<? extends Monster> type = holder.get();
        event.register(type, SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private static void registerPlacement(EntityType<? extends Monster> type) {
        SpawnPlacements.register(type, SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);
    }

    /** 属性注册必须挂在 mod 事件总线上。 */
    @Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        private Attributes() {
        }

        @SubscribeEvent
        public static void onEntityAttributes(EntityAttributeCreationEvent event) {
            event.put(GUILTY_SECT_DISCIPLE.get(), GuiltySectDiscipleEntity.createAttributes().build());
            event.put(SPIRIT_WOLF.get(), SpiritWolfEntity.createAttributes().build());
            event.put(BLOOD_DEMON_CULTIVATOR.get(), BloodDemonCultivatorEntity.createAttributes().build());
            event.put(PUPPET_CAPTAIN.get(), PuppetCaptainEntity.createAttributes().build());
            event.put(DEMON_GENERAL.get(), DemonGeneralEntity.createAttributes().build());
        // 祭坛首领：属性见各自的 createAttributes（虚空道主是首领品阶）
        event.put(EPOCH_BOSS.get(), EpochBossEntity.createAttributes().build());
        event.put(BEAST_BOSS.get(), BeastBossEntity.createAttributes().build());
        event.put(DEMON_BOSS.get(), DemonBossEntity.createAttributes().build());
        event.put(VOID_BOSS.get(), VoidBossEntity.createAttributes().build());
            event.put(VOID_OVERLORD.get(), VoidOverlordEntity.createAttributes().build());
            // 劫灰修士（真世界专属新怪）
            //
            // ⚠ 漏了这一行，起服日志会报
            //   「Entity hongmeng:tribulation_ash_cultivator has no attributes」
            //   而且**刷出来的怪血量为 0、瞬间消失** —— 这是实测踩到的坑。
            event.put(EVIL_GAZER.get(),
                    EvilGazerEntity.createAttributes().build());
            event.put(BLAZE_EMPEROR.get(),
                    BlazeEmperorEntity.createAttributes().build());
            event.put(VOID_DRAGON_EMPEROR.get(),
                    VoidDragonEmperorEntity.createAttributes().build());
            event.put(TRIBULATION_ASH_CULTIVATOR.get(),
                    TribulationAshCultivatorEntity.createAttributes().build());
            // ---- 第二批妖兽 ----
            event.put(FLAME_APE.get(), FlameApeEntity.createAttributes().build());
            event.put(THUNDER_EAGLE.get(), ThunderEagleEntity.createAttributes().build());
            event.put(STONE_GOLEM.get(), StoneGolemEntity.createAttributes().build());
            event.put(VOID_SERPENT.get(), VoidSerpentEntity.createAttributes().build());
            // ---- 第三批妖兽（都走 scaledAttributes，里面已经注册了 FLYING_SPEED）----
            event.put(BURROW_MARTEN.get(), BurrowMartenEntity.createAttributes().build());
            event.put(VENOM_TOAD.get(), VenomToadEntity.createAttributes().build());
            // ---- 第四批：人形 4 只 ----
            //
            // 四只的属性表**全部**来自 CultivationMonsterEntity.scaledAttributes：
            // 基础值 × MobRank 倍率在注册时就算进去了，境界那一层由 onAddedToWorld 补，
            // 所以这里不需要（也不该）再乘任何东西。
            event.put(DEMON_STEWARD.get(), DemonStewardEntity.createAttributes().build());
            event.put(BLOOD_DEMON_ELDER.get(), BloodDemonElderEntity.createAttributes().build());
            event.put(VOID_WALKER.get(), VoidWalkerEntity.createAttributes().build());
            event.put(SWORD_TOMB_GUARDIAN.get(),
                    SwordTombGuardianEntity.createAttributes().build());
            // ---- 第四批：妖兽 3 只（赤炎狼复用灵狼骨架，玄冰蟒复用虚空蛟，雷纹鹰复用雷鹰）----
            event.put(CRIMSON_FLAME_WOLF.get(),
                    CrimsonFlameWolfEntity.createAttributes().build());
            event.put(FROST_PYTHON.get(), FrostPythonEntity.createAttributes().build());
            event.put(STORM_EAGLE.get(), StormEagleEntity.createAttributes().build());
            // ---- NPC ----
            event.put(SECT_ELDER.get(), SectElderEntity.createAttributes().build());
            event.put(ALCHEMY_MASTER.get(), AlchemyMasterEntity.createAttributes().build());
            event.put(WANDERING_MERCHANT.get(), WanderingMerchantEntity.createAttributes().build());
            // ---- 第四批 NPC ----
            event.put(PILL_MERCHANT.get(), PillMerchantEntity.createAttributes().build());
            event.put(ARTIFACT_REFINER.get(), ArtifactRefinerEntity.createAttributes().build());
            event.put(REALM_GUIDE.get(), RealmGuideEntity.createAttributes().build());
            event.put(BOUNTY_BOARD.get(), BountyBoardEntity.createAttributes().build());
            // ---- 灵兽 ----
            event.put(CONTRACT_EAGLE.get(),
                    com.hongmeng.xiuxian.entity.beast.ContractEagleEntity.createAttributes().build());
            event.put(JIAOLONG.get(),
                    com.hongmeng.xiuxian.entity.beast.JiaolongEntity.createAttributes().build());
            event.put(CONTRACT_WOLF.get(),
                    com.hongmeng.xiuxian.entity.beast.ContractWolfEntity.createAttributes().build());
            // 弹体是 Projectile（不是 LivingEntity），**不需要**属性表——
            // 给非生物实体注册属性会在启动时抛 ClassCastException
        }
    }
}
