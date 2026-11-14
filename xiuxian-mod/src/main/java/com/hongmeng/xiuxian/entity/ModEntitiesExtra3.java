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
 * <b>第四轮 · 第五维度「天外天」新增实体注册表</b>（3 个）。
 *
 * <p>与前两批同构、分开写：新内容整条线都是"新增文件"，不动旧注册表。</p>
 *
 * <ul>
 *   <li><b>仙庭之主</b> {@code heaven_lord}：天外天的巨型首领（OVERLORD），
 *       hitbox 2.0×6.9 —— 模型本身 110px≈6.9 格，再乘品阶/巨化倍率到 ≈11 格高；</li>
 *   <li><b>天兵</b> {@code heaven_soldier}：天外天环境怪主力（精英，人形金甲长戟）；</li>
 *   <li><b>云鹤</b> {@code cloud_crane}：天外天飞行怪（队长，长颈鸟形）。</li>
 * </ul>
 *
 * <p>由 {@code HongmengMod} 构造器调用 {@link #register(IEventBus)}。</p>
 */
public final class ModEntitiesExtra3 {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HongmengMod.MOD_ID);

    /** 仙庭之主：天外天巨型首领。 */
    public static final RegistryObject<EntityType<HeavenLordEntity>> HEAVEN_LORD =
            ENTITIES.register("heaven_lord", () -> EntityType.Builder
                    .of(HeavenLordEntity::new, MobCategory.MONSTER)
                    .sized(4.0F, 7.9F)
                    .clientTrackingRange(16)
                    .build("heaven_lord"));

    /** 天兵：天外天环境怪（精英）。 */
    public static final RegistryObject<EntityType<HeavenSoldierEntity>> HEAVEN_SOLDIER =
            ENTITIES.register("heaven_soldier", () -> EntityType.Builder
                    .of(HeavenSoldierEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(12)
                    .build("heaven_soldier"));

    /** 云鹤：天外天飞行怪（队长）。 */
    public static final RegistryObject<EntityType<CloudCraneEntity>> CLOUD_CRANE =
            ENTITIES.register("cloud_crane", () -> EntityType.Builder
                    .of(CloudCraneEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 1.6F)
                    .clientTrackingRange(12)
                    .build("cloud_crane"));

    private ModEntitiesExtra3() {
    }

    /** 由 {@code HongmengMod} 的构造器调用。 */
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntitiesExtra3::onEntityAttributes);
        bus.addListener(ModEntitiesExtra3::onSpawnPlacements);
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        if (HEAVEN_LORD.isPresent()) {
            event.put(HEAVEN_LORD.get(), HeavenLordEntity.createAttributes().build());
        }
        if (HEAVEN_SOLDIER.isPresent()) {
            event.put(HEAVEN_SOLDIER.get(), HeavenSoldierEntity.createAttributes().build());
        }
        if (CLOUD_CRANE.isPresent()) {
            event.put(CLOUD_CRANE.get(), CloudCraneEntity.createAttributes().build());
        }
    }

    private static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        registerPlacement(event, HEAVEN_LORD);
        registerPlacement(event, HEAVEN_SOLDIER);
        registerPlacement(event, CLOUD_CRANE);
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
