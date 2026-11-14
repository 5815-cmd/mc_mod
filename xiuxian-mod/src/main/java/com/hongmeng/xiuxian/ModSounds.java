package com.hongmeng.xiuxian;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 音效。占位素材目前是程序生成的音频文件，
 * 具体格式见 tools/gen_assets.py 的说明。
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HongmengMod.MOD_ID);

    public static final RegistryObject<SoundEvent> MEDITATION =
            sound("meditation");
    public static final RegistryObject<SoundEvent> BREAKTHROUGH_SUCCESS =
            sound("breakthrough_success");
    public static final RegistryObject<SoundEvent> BREAKTHROUGH_FAIL =
            sound("breakthrough_fail");
    public static final RegistryObject<SoundEvent> AWAKEN =
            sound("awaken");
    public static final RegistryObject<SoundEvent> SWORD_QI =
            sound("sword_qi");

    private ModSounds() {
    }

    /** 注意：不能叫 register，否则会遮蔽 DeferredRegister.register(IEventBus)。 */
    // ---- 战斗音效（自行合成，非外部素材）----
    /** Boss 转阶段：压迫低频。 */
    public static final RegistryObject<SoundEvent> PHASE = sound("phase");

    /** 法术·Metal（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_METAL = sound("cast_metal");
    /** 法术·Wood（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_WOOD = sound("cast_wood");
    /** 法术·Water（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_WATER = sound("cast_water");
    /** 法术·Fire（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_FIRE = sound("cast_fire");
    /** 法术·Earth（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_EARTH = sound("cast_earth");
    /** 法术·Ice（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_ICE = sound("cast_ice");
    /** 法术·Thunder（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_THUNDER = sound("cast_thunder");
    /** 法术·Wind（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_WIND = sound("cast_wind");
    /** 法术·None（自行合成）。 */
    public static final RegistryObject<SoundEvent> CAST_NONE = sound("cast_none");

    /** 雷劫·落雷（自行合成）。 */
    public static final RegistryObject<SoundEvent> TRIB_BOLT =
            sound("tribulation_bolt");
    /** 雷劫·蓄势（自行合成）。 */
    public static final RegistryObject<SoundEvent> TRIB_CHARGE =
            sound("tribulation_charge");

    /** 背景音乐：bgm_calm（自行合成）。 */
    public static final RegistryObject<SoundEvent> BGM_CALM = sound("bgm_calm");
    /** 背景音乐：bgm_dark（自行合成）。 */
    public static final RegistryObject<SoundEvent> BGM_DARK = sound("bgm_dark");
    /** 背景音乐：bgm_boss（自行合成）。 */
    public static final RegistryObject<SoundEvent> BGM_BOSS = sound("bgm_boss");
    /** 背景音乐：bgm_tribulation（自行合成）。 */
    public static final RegistryObject<SoundEvent> BGM_TRIBULATION = sound("bgm_tribulation");

    // ---- 四段式：每段一个音（自行合成）----
    /** 蓄势：上升低鸣。 */
    public static final RegistryObject<SoundEvent> STAGE_CHARGE = sound("stage_charge");
    /** 爆发：爆裂 + 低频轰鸣。 */
    public static final RegistryObject<SoundEvent> STAGE_BURST = sound("stage_burst");
    /** 余波：高频微光 + 余震。 */
    public static final RegistryObject<SoundEvent> STAGE_AFTERMATH =
            sound("stage_aftermath");

    // ---- Boss 专属大招 ----
    /** 魔影突袭：反向渐强 + 下潜 + 裂帛。 */
    public static final RegistryObject<SoundEvent> BOSS_AMBUSH =
            sound("boss_ambush");
    /** 天崩地裂：巨震 + 余震 + 碎石。 */
    public static final RegistryObject<SoundEvent> BOSS_CATACLYSM =
            sound("boss_cataclysm");

    private static RegistryObject<SoundEvent> sound(String name) {
        ResourceLocation id = new ResourceLocation(HongmengMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        SOUNDS.register(bus);
    }
}
