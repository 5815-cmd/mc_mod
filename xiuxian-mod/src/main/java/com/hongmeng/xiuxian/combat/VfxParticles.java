package com.hongmeng.xiuxian.combat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * 粒子编号 → 原版粒子。{@link ElementVfx} 的表里存的是编号，
 * 这里做唯一的运行时映射。
 *
 * <p><b>这个类会碰注册表</b>（{@code ParticleTypes} 是注册表对象），
 * 所以离线验证器与探针<b>绝对不能加载它</b>——
 * 纯数据在 {@link ElementVfx}，映射在这里，两边泾渭分明。</p>
 *
 * <p>规格的硬约束：<b>不下任何素材</b>，全部用原版粒子。</p>
 */
public final class VfxParticles {

    private VfxParticles() {
    }

    /** 编号 → 粒子（未知编号退回 {@code ENCHANT}，永远不返回 null）。 */
    public static ParticleOptions of(int id) {
        return switch (id) {
            case ElementVfx.P_FLAME -> ParticleTypes.FLAME;
            case ElementVfx.P_SMALL_FLAME -> ParticleTypes.SMALL_FLAME;
            case ElementVfx.P_LAVA -> ParticleTypes.LAVA;
            case ElementVfx.P_EXPLOSION -> ParticleTypes.EXPLOSION;
            case ElementVfx.P_SNOWFLAKE -> ParticleTypes.SNOWFLAKE;
            case ElementVfx.P_CRIT -> ParticleTypes.CRIT;
            case ElementVfx.P_ENCHANTED_HIT -> ParticleTypes.ENCHANTED_HIT;
            case ElementVfx.P_ELECTRIC_SPARK -> ParticleTypes.ELECTRIC_SPARK;
            case ElementVfx.P_FLASH -> ParticleTypes.FLASH;
            case ElementVfx.P_CLOUD -> ParticleTypes.CLOUD;
            case ElementVfx.P_SWEEP -> ParticleTypes.SWEEP_ATTACK;
            case ElementVfx.P_END_ROD -> ParticleTypes.END_ROD;
            case ElementVfx.P_FIREWORK -> ParticleTypes.FIREWORK;
            case ElementVfx.P_HAPPY_VILLAGER -> ParticleTypes.HAPPY_VILLAGER;
            case ElementVfx.P_SPORE -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case ElementVfx.P_CHERRY -> ParticleTypes.CHERRY_LEAVES;
            case ElementVfx.P_SPLASH -> ParticleTypes.SPLASH;
            case ElementVfx.P_BUBBLE -> ParticleTypes.BUBBLE;
            case ElementVfx.P_FALLING_WATER -> ParticleTypes.FALLING_WATER;
            case ElementVfx.P_LARGE_SMOKE -> ParticleTypes.LARGE_SMOKE;
            case ElementVfx.P_CAMPFIRE_SMOKE -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
            case ElementVfx.P_WHITE_ASH -> ParticleTypes.WHITE_ASH;
            case ElementVfx.P_SMOKE -> ParticleTypes.SMOKE;
            case ElementVfx.P_SOUL_FLAME -> ParticleTypes.SOUL_FIRE_FLAME;
            case ElementVfx.P_TOTEM -> ParticleTypes.TOTEM_OF_UNDYING;
            case ElementVfx.P_DRAGON_BREATH -> ParticleTypes.DRAGON_BREATH;
            case ElementVfx.P_SCULK_SOUL -> ParticleTypes.SCULK_SOUL;
            case ElementVfx.P_WITCH -> ParticleTypes.WITCH;
            case ElementVfx.P_POOF -> ParticleTypes.POOF;
            case ElementVfx.P_SONIC -> ParticleTypes.SONIC_BOOM;
            case ElementVfx.P_GLOW -> ParticleTypes.GLOW;
            case ElementVfx.P_ASH -> ParticleTypes.ASH;
            case ElementVfx.P_DRIPPING_WATER -> ParticleTypes.DRIPPING_WATER;
            default -> ParticleTypes.ENCHANT;
        };
    }
}
