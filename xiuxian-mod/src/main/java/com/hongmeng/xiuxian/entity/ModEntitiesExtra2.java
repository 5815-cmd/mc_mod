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
 * <b>第二轮 · 第二梯队新增怪物注册表</b>（7 只）。
 *
 * <p>与 {@link ModEntitiesExtra}（第一梯队 6 只）同构、分开写：新怪物整条线都是"新增文件"，
 * 加一批不用回头改上一批的注册表，多人并行也不会互相冲突。</p>
 *
 * <p>分布（详见 {@code 工程/角色设计/round2/新怪物第二梯队设计稿.md} 与
 * {@code 工程/10-地形与怪物分布总表.md}）：</p>
 * <ul>
 *   <li>末地：终界使徒 {@code end_apostle}（精英，自然刷）</li>
 *   <li>真世界：堕仙 {@code fallen_immortal}（队长，Hardmode 精英替换）</li>
 *   <li>妖兽秘境·血食泥沼：血蛭 {@code blood_leech}（普通，自然刷）</li>
 *   <li>妖兽秘境·兽骨荒原：骨鹰 {@code bone_eagle}（精英，自然刷）</li>
 *   <li>魔道巢穴·黑石魔窟：魔蛛 {@code demon_spider}（普通，自然刷）</li>
 *   <li>魔道巢穴·焦土魔原：焦骨骷髅 {@code charred_skeleton}（普通，自然刷）</li>
 *   <li>虚空裂隙·星尘荒原：星尘游魂 {@code star_dust_wraith}（精英，自然刷）</li>
 * </ul>
 *
 * <p>由 {@code HongmengMod} 构造器调用 {@link #register(IEventBus)}。</p>
 */
public final class ModEntitiesExtra2 {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HongmengMod.MOD_ID);

    /** 终界使徒：末地虚无朝圣者，飘浮黑袍 + 四只虚空眼 + 背后断裂门环。 */
    public static final RegistryObject<EntityType<EndApostleEntity>> END_APOSTLE =
            ENTITIES.register("end_apostle", () -> EntityType.Builder
                    .of(EndApostleEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(12)
                    .build("end_apostle"));

    /** 堕仙：真世界的堕落仙人，残破仙袍 + 倒悬断环 + 断剑。 */
    public static final RegistryObject<EntityType<FallenImmortalEntity>> FALLEN_IMMORTAL =
            ENTITIES.register("fallen_immortal", () -> EntityType.Builder
                    .of(FallenImmortalEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 2.4F)
                    .clientTrackingRange(12)
                    .build("fallen_immortal"));

    /** 血蛭：血食泥沼的吸血蠕虫（长条，矮扁命中箱）。 */
    public static final RegistryObject<EntityType<BloodLeechEntity>> BLOOD_LEECH =
            ENTITIES.register("blood_leech", () -> EntityType.Builder
                    .of(BloodLeechEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(10)
                    .build("blood_leech"));

    /** 骨鹰：兽骨荒原的白骨秃鹫（飞行怪，出生在地面再起飞）。 */
    public static final RegistryObject<EntityType<BoneEagleEntity>> BONE_EAGLE =
            ENTITIES.register("bone_eagle", () -> EntityType.Builder
                    .of(BoneEagleEntity::new, MobCategory.MONSTER)
                    .sized(1.5F, 1.6F)
                    .clientTrackingRange(12)
                    .build("bone_eagle"));

    /** 魔蛛：黑石魔窟的黑曜蜘蛛（8 长腿 + 熔岩腹）。 */
    public static final RegistryObject<EntityType<DemonSpiderEntity>> DEMON_SPIDER =
            ENTITIES.register("demon_spider", () -> EntityType.Builder
                    .of(DemonSpiderEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 0.7F)
                    .clientTrackingRange(10)
                    .build("demon_spider"));

    /** 焦骨骷髅：焦土魔原的炭化弓手。 */
    public static final RegistryObject<EntityType<CharredSkeletonEntity>> CHARRED_SKELETON =
            ENTITIES.register("charred_skeleton", () -> EntityType.Builder
                    .of(CharredSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F)
                    .clientTrackingRange(10)
                    .build("charred_skeleton"));

    /** 星尘游魂：星尘荒原的星尘聚合体（飘浮）。 */
    public static final RegistryObject<EntityType<StarDustWraithEntity>> STAR_DUST_WRAITH =
            ENTITIES.register("star_dust_wraith", () -> EntityType.Builder
                    .of(StarDustWraithEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.0F)
                    .clientTrackingRange(10)
                    .build("star_dust_wraith"));

    private ModEntitiesExtra2() {
    }

    /** 由 {@code HongmengMod} 的构造器调用。 */
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntitiesExtra2::onEntityAttributes);
        bus.addListener(ModEntitiesExtra2::onSpawnPlacements);
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        if (END_APOSTLE.isPresent()) {
            event.put(END_APOSTLE.get(), EndApostleEntity.createAttributes().build());
        }
        if (FALLEN_IMMORTAL.isPresent()) {
            event.put(FALLEN_IMMORTAL.get(), FallenImmortalEntity.createAttributes().build());
        }
        if (BLOOD_LEECH.isPresent()) {
            event.put(BLOOD_LEECH.get(), BloodLeechEntity.createAttributes().build());
        }
        if (BONE_EAGLE.isPresent()) {
            event.put(BONE_EAGLE.get(), BoneEagleEntity.createAttributes().build());
        }
        if (DEMON_SPIDER.isPresent()) {
            event.put(DEMON_SPIDER.get(), DemonSpiderEntity.createAttributes().build());
        }
        if (CHARRED_SKELETON.isPresent()) {
            event.put(CHARRED_SKELETON.get(), CharredSkeletonEntity.createAttributes().build());
        }
        if (STAR_DUST_WRAITH.isPresent()) {
            event.put(STAR_DUST_WRAITH.get(), StarDustWraithEntity.createAttributes().build());
        }
    }

    private static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        registerPlacement(event, END_APOSTLE);
        registerPlacement(event, FALLEN_IMMORTAL);
        registerPlacement(event, BLOOD_LEECH);
        registerPlacement(event, BONE_EAGLE);
        registerPlacement(event, DEMON_SPIDER);
        registerPlacement(event, CHARRED_SKELETON);
        registerPlacement(event, STAR_DUST_WRAITH);
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
