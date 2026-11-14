package com.hongmeng.xiuxian.client;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 屏蔽原版"伤害指示器"粒子（就是那颗<b>黑心</b>）。
 *
 * <h2>玩家的原话</h2>
 * <p>「高版本打怪之后怪掉血会爆出红心（颜色是黑心）…我说的是这个心」。</p>
 *
 * <p>那正是 {@code ParticleTypes.DAMAGE_INDICATOR}：它的贴图
 * {@code damage_indicator.png} 就是一颗深红偏黑的心，每次受击由原版的
 * {@code LivingEntity.hurt()} 喷一圈。本模组自己的代码早就不喷它了
 * （已换成 {@code CRIT} 星形火花），所以现在这一批是<b>原版</b>喷的 ——
 * 服务端照旧发包，只能在客户端拦。</p>
 *
 * <h2>怎么拦</h2>
 * <p>把 {@code minecraft:damage_indicator} 的 provider 换成一个返回 {@code null}
 * 的空实现（{@code ParticleProvider.Sprite}）：原版流程一点不改，
 * 只是屏幕上不再出现那颗心。伤害数字（{@code DamageNumbers}，画的是真数值）
 * 与 CRIT 火花都不受影响。</p>
 *
 * <p><b>注意</b>：{@code RegisterParticleProvidersEvent} 是 {@code IModBusEvent}，
 * 所以这里只能挂在 <b>MOD 事件总线</b>（{@code @Mod.EventBusSubscriber} 的默认总线），
 * 挂到 forge 总线上不会触发。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "hongmeng", bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class NoDamageIndicatorParticles {

    private NoDamageIndicatorParticles() {
    }

    @SubscribeEvent
    public static void onRegisterProviders(RegisterParticleProvidersEvent event) {
        // 返回 null 的 Sprite provider = "这个位置不生成粒子"。
        // 直接覆盖原版为 damage_indicator 注册的那一个。
        //
        // 写成**显式匿名类**而不是 lambda：registerSprite 要的是
        // ParticleProvider.Sprite<T>（方法有三个以上参数），
        // 用 `spriteSet -> null` 会被 javac 判成"参数类型不兼容"（踩过一次）。
        event.registerSprite(ParticleTypes.DAMAGE_INDICATOR,
                new net.minecraft.client.particle.ParticleProvider.Sprite<
                        net.minecraft.core.particles.SimpleParticleType>() {
                    @Override
                    public net.minecraft.client.particle.TextureSheetParticle createParticle(
                            net.minecraft.core.particles.SimpleParticleType type,
                            net.minecraft.client.multiplayer.ClientLevel level,
                            double x, double y, double z,
                            double dx, double dy, double dz) {
                        // 返回 null = 这一帧不生成粒子
                        return null;
                    }
                });
    }
}
