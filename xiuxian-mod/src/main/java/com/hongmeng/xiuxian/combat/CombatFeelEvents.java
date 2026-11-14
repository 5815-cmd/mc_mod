package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 战斗手感的事件入口（服务端一侧）。
 *
 * <h2>为什么用 {@code @Mod.EventBusSubscriber} 自动注册</h2>
 *
 * <p>规格要求的所有服务端逻辑只需要挂三个事件。自动注册（{@code bus = FORGE}）意味着
 * <b>不用去改 {@code HongmengMod} / {@code core/ModEvents}</b>——那几个文件
 * 有别的代理在同时改，少碰一次就少一次互相回退的风险。</p>
 *
 * <h2>只用 {@code LivingDamageEvent} 一个伤害钩子</h2>
 *
 * <p>看字节码确认过它的位置：{@code LivingEntity.actuallyHurt} 里
 * {@code ForgeHooks.onLivingDamage} 拿到的 {@code amount} 已经是
 * <b>扣完护甲、状态效果、境界压制减伤、35% 软上限之后的最终值</b>。</p>
 *
 * <ul>
 *   <li>玩家的近战、原版弓弩、法术（{@code TechniqueCaster}）、领域百分比伤害，
 *       全部经过 {@code actuallyHurt}——一个钩子就全覆盖；</li>
 *   <li>拿到的是最终值，所以"伤害占比"和玩家看到掉的血一致；</li>
 *   <li><b>本类不改伤害</b>：只读 {@code getAmount()}。软上限与减伤照旧生效。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = HongmengMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CombatFeelEvents {

    private CombatFeelEvents() {
    }

    /** 一次伤害结算 → 顿帧 / 震动（事实包）/ 闪白 / 击退 / 音效 / 粒子爆发。 */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event == null || event.getEntity() == null) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        try {
            CombatFeedback.onDamage(event.getEntity(), event.getSource(), event.getAmount());
        } catch (Throwable t) {
            // 手感系统出问题绝不能让伤害结算崩掉：吞掉异常，伤害照常。
            HongmengMod.LOGGER.warn("[鸿蒙·战斗手感] 战斗手感异常（已忽略）", t);
        }
    }

    /** 每 tick：推进冻结实体 + 跑延迟特效队列。 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        try {
            HitstopController.tick(level);
            HitstopController.prune(level.getGameTime());
            CombatFxScheduler.tick(level);
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[鸿蒙·战斗手感] 战斗手感 tick 异常（已忽略）", t);
        }
    }

    /** 强者登场（读过档的怪不算）。 */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        try {
            BossEntrance.onEntityJoin(event.getEntity(), event.getLevel(), event.loadedFromDisk());
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[鸿蒙·战斗手感] 强者登场演出异常（已忽略）", t);
        }
    }

    /** 停服：把静态表清干净（单人存档来回切换时不会有残留）。 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HitstopController.clear();
        CombatFxScheduler.clear();
        ParticleBudget.clear();
        CombatFeedback.clear();
        BossEntrance.clear();
    }
}
