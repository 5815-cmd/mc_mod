package com.hongmeng.xiuxian;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

/**
 * 状态效果：境界压制、灵力充盈、痛觉、业力缠身。
 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, HongmengMod.MOD_ID);

    private static final UUID REALM_SUPPRESS_ID = UUID.fromString("8f2a1c34-5b6d-4e7f-9a0b-1c2d3e4f5a6b");
    private static final UUID REALM_EMPOWER_ID = UUID.fromString("2c7e9d10-3a4b-4c5d-8e9f-0a1b2c3d4e5f");
    private static final UUID PAIN_ID = UUID.fromString("5d6c7b8a-9012-4a3b-8c7d-6e5f4a3b2c1d");

    /** 境界压制：被高阶存在压制时，攻击与移速下降。 */
    public static final RegistryObject<MobEffect> REALM_SUPPRESS =
            EFFECTS.register("realm_suppress", () -> new RealmSuppressEffect());

    /** 灵力充盈：修炼加成期。 */
    public static final RegistryObject<MobEffect> REALM_EMPOWER =
            EFFECTS.register("realm_empower", () -> new RealmEmpowerEffect());

    /** 痛觉：真实痛觉机制下的减益。 */
    public static final RegistryObject<MobEffect> PAIN =
            EFFECTS.register("pain", () -> new PainEffect());

    /** 业力缠身：纯粹的风味提示（数值惩罚在突破逻辑里算）。 */
    public static final RegistryObject<MobEffect> KARMA_BOUND =
            EFFECTS.register("karma_bound", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x8B1A1A));

    private ModEffects() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        EFFECTS.register(bus);
    }

    /** 简单的无属性效果。 */
    public static class SimpleEffect extends MobEffect {
        public SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            return false;
        }
    }

    /** 被高境界者压制的效果。 */
    public static class RealmSuppressEffect extends MobEffect {
        public RealmSuppressEffect() {
            super(MobEffectCategory.HARMFUL, 0x4B0082);
            addAttributeModifier(Attributes.ATTACK_DAMAGE, REALM_SUPPRESS_ID.toString(),
                    -0.25D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, REALM_SUPPRESS_ID.toString(),
                    -0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            return false;
        }
    }

    /** 灵力充盈：攻击与速度上浮。 */
    public static class RealmEmpowerEffect extends MobEffect {
        public RealmEmpowerEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x66CCFF);
            addAttributeModifier(Attributes.ATTACK_DAMAGE, REALM_EMPOWER_ID.toString(),
                    0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, REALM_EMPOWER_ID.toString(),
                    0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            return false;
        }
    }

    /** 痛觉：移动迟缓、攻击下降。 */
    public static class PainEffect extends MobEffect {
        public PainEffect() {
            super(MobEffectCategory.HARMFUL, 0xAA0000);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, PAIN_ID.toString(),
                    -0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            return false;
        }
    }

    /** 预留：给需要直接改生命上限的效果使用。 */
    static void unused(LivingEntity entity, AttributeMap map) {
        // 占位，避免导入被优化掉
        entity.getAttribute(Attributes.MAX_HEALTH);
    }
}
