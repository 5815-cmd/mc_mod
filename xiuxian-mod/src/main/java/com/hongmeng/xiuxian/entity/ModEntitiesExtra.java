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
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * <b>第二轮新增怪物 · 注册表</b>（与 {@link ModEntities} 分开，避免多人改同一个共享文件）。
 *
 * <p>六只怪按地形分布（详见 {@code 工程/10-地形与怪物分布总表.md} 与
 * {@code 工程/角色设计/round2/新怪物规范.md}）：</p>
 * <ul>
 *   <li>云梦泽：泽魅 {@code marsh_wraith}</li>
 *   <li>灵脉山谷：晶蛛 {@code crystal_spider}</li>
 *   <li>荒古战场：兵俑 {@code terracotta_soldier}</li>
 *   <li>纪元秘境（草原/稻田）：稻草傀儡 {@code scarecrow_puppet}</li>
 *   <li>下界：熔岩犬 {@code lava_hound}</li>
 *   <li>虚空秘境（浮岛/归墟）：虚空水母 {@code void_jelly}</li>
 * </ul>
 *
 * <p>本类只做三件与注册相关的事：<b>EntityType</b>、<b>属性</b>、<b>刷怪合法性</b>。
 * 贴图/模型/渲染器在各自的 client 类里；刷怪频率在 {@code data/hongmeng/forge/biome_modifier/add_spawn_*.json}。
 * 由 {@link HongmengMod#} 的构造器调用 {@link #register(IEventBus)}。</p>
 */
public final class ModEntitiesExtra {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HongmengMod.MOD_ID);

    // ------------------------------------------------------------------
    // 1) 实体类型（命中箱与模型对齐；模型高由 MobRank 体型倍率再放大）
    // ------------------------------------------------------------------

    /** 泽魅：云梦泽的怨魂，飘浮破袍 + 青磷灯。 */
    public static final RegistryObject<EntityType<MarshWraithEntity>> MARSH_WRAITH =
            ENTITIES.register("marsh_wraith", () -> EntityType.Builder
                    .of(MarshWraithEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.9F)
                    .clientTrackingRange(10)
                    .build("marsh_wraith"));

    /** 晶蛛：灵脉山谷的矿脉猎手，背驮灵晶。 */
    public static final RegistryObject<EntityType<CrystalSpiderEntity>> CRYSTAL_SPIDER =
            ENTITIES.register("crystal_spider", () -> EntityType.Builder
                    .of(CrystalSpiderEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 0.9F)
                    .clientTrackingRange(10)
                    .build("crystal_spider"));

    /** 兵俑：荒古战场的陶土军阵残兵，蓄力突刺、免疫击退。 */
    public static final RegistryObject<EntityType<TerracottaSoldierEntity>> TERRACOTTA_SOLDIER =
            ENTITIES.register("terracotta_soldier", () -> EntityType.Builder
                    .of(TerracottaSoldierEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(10)
                    .build("terracotta_soldier"));

    /** 稻草傀儡：纪元秘境的田间机关，装死大师。 */
    public static final RegistryObject<EntityType<ScarecrowPuppetEntity>> SCARECROW_PUPPET =
            ENTITIES.register("scarecrow_puppet", () -> EntityType.Builder
                    .of(ScarecrowPuppetEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F)
                    .clientTrackingRange(10)
                    .build("scarecrow_puppet"));

    /** 熔岩犬：下界的熔岩猎犬（复用灵狼骨架，贴图换成黑曜+熔岩缝）。 */
    public static final RegistryObject<EntityType<LavaHoundEntity>> LAVA_HOUND =
            ENTITIES.register("lava_hound", () -> EntityType.Builder
                    .of(LavaHoundEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.9F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("lava_hound"));

    /** 虚空水母：虚空秘境的漂游体，飘浮 + 虚空侵蚀。 */
    public static final RegistryObject<EntityType<VoidJellyEntity>> VOID_JELLY =
            ENTITIES.register("void_jelly", () -> EntityType.Builder
                    .of(VoidJellyEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.1F)
                    .clientTrackingRange(10)
                    .build("void_jelly"));

    private ModEntitiesExtra() {
    }

    /** 由 {@code HongmengMod} 的构造器调用。 */
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntitiesExtra::onEntityAttributes);
        bus.addListener(ModEntitiesExtra::onSpawnPlacements);
    }

    // ------------------------------------------------------------------
    // 2) 属性
    // ------------------------------------------------------------------

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        if (MARSH_WRAITH.isPresent()) {
            event.put(MARSH_WRAITH.get(), MarshWraithEntity.createAttributes().build());
        }
        if (CRYSTAL_SPIDER.isPresent()) {
            event.put(CRYSTAL_SPIDER.get(), CrystalSpiderEntity.createAttributes().build());
        }
        if (TERRACOTTA_SOLDIER.isPresent()) {
            event.put(TERRACOTTA_SOLDIER.get(), TerracottaSoldierEntity.createAttributes().build());
        }
        if (SCARECROW_PUPPET.isPresent()) {
            event.put(SCARECROW_PUPPET.get(), ScarecrowPuppetEntity.createAttributes().build());
        }
        if (LAVA_HOUND.isPresent()) {
            event.put(LAVA_HOUND.get(), LavaHoundEntity.createAttributes().build());
        }
        if (VOID_JELLY.isPresent()) {
            event.put(VOID_JELLY.get(), VoidJellyEntity.createAttributes().build());
        }
    }

    // ------------------------------------------------------------------
    // 3) 刷怪合法性（"位置合不合法"；刷不刷、多久刷一次看 biome_modifier 的 weight）
    // ------------------------------------------------------------------

    private static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        registerPlacement(event, MARSH_WRAITH);
        registerPlacement(event, CRYSTAL_SPIDER);
        registerPlacement(event, TERRACOTTA_SOLDIER);
        registerPlacement(event, SCARECROW_PUPPET);
        registerPlacement(event, LAVA_HOUND);
        registerPlacement(event, VOID_JELLY);
    }

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
}
