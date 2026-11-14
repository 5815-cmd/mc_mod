package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * <b>怪物的格挡</b>：让战斗从「单向压制」变对称。
 *
 * <h2>为什么需要</h2>
 * <p>玩家的格挡/弹反做完之后，战斗变成：<b>玩家能弹反怪，怪不会弹反玩家</b>。
 * 于是最优解永远是「贴脸砍」—— 因为怪没有任何"防守"手段。
 * 这和魂系的对手感完全相反。</p>
 *
 * <h2>设计（和玩家那套对称，但更简单）</h2>
 * <table border="1">
 *   <caption>怪物格挡</caption>
 *   <tr><th>时机</th><th>减伤</th><th>附加</th></tr>
 *   <tr><td><b>弹反</b>（起手 {@value #PERFECT_TICKS} tick 内被打）</td>
 *       <td>−85%</td><td>玩家<b>踉跄</b>（减速 + 短击退）</td></tr>
 *   <tr><td>普通格挡</td><td>−60%</td><td>无</td></tr>
 * </table>
 *
 * <h2>为什么只有精英以上会格挡</h2>
 * <p>杂鱼如果也会弹反，玩家在低境界就会被"弹到怀疑人生"。
 * 格挡是<b>精英的标志</b>——看到它会挡，玩家就知道"这个不能无脑砍"。</p>
 *
 * <h2>为什么必须"面朝玩家"</h2>
 * <p>否则会出现"背对着你也能挡"的荒谬感。这条也让<b>绕后</b>继续有收益 ——
 * 和玩家的背刺规则<b>对称</b>。</p>
 */
public final class MobGuard {

    /** 格挡持续（tick）。 */
    public static final int GUARD_TICKS = 16;

    /** 弹反窗口（从起手算起）。 */
    public static final int PERFECT_TICKS = 6;

    /** 冷却（tick）—— 不能一直挡。 */
    public static final int COOLDOWN_TICKS = 90;

    /** 普通格挡减伤。 */
    public static final double NORMAL_CUT = 0.60D;

    /** 弹反减伤。 */
    public static final double PERFECT_CUT = 0.85D;

    /** 玩家被弹反后的减速时长。 */
    public static final int STAGGER_TICKS = 20;

    /** 玩家被弹反后的击退强度。 */
    public static final double STAGGER_KNOCKBACK = 0.45D;

    /** 面朝判定：玩家必须在怪的正前方这个余弦之内。 */
    public static final double FACING_DOT = 0.25D;

    private static final String TAG_UNTIL = "hm_guard_until";
    private static final String TAG_PERFECT = "hm_guard_perfect";
    private static final String TAG_CD = "hm_guard_cd";

    private MobGuard() {
    }

    /** 各品阶的"愿意格挡"概率。 */
    private static double blockChance(MobRank rank) {
        return switch (rank) {
            case ELITE -> 0.22D;
            case CAPTAIN -> 0.30D;
            case BOSS -> 0.38D;
            case OVERLORD -> 0.48D;
            default -> 0.0D;      // 杂鱼不会
        };
    }

    /** 每 tick 决定要不要起手格挡（由实体的 customServerAiStep 调用）。 */
    public static void tick(CultivationMonsterEntity mob) {
        if (mob == null || mob.level().isClientSide()) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(mob.getTarget() instanceof ServerPlayer target) || !target.isAlive()) {
            return;
        }
        var pd = mob.getPersistentData();
        long now = level.getGameTime();
        if (now < pd.getLong(TAG_CD)) {
            return;                     // 冷却中
        }
        if (now <= pd.getLong(TAG_UNTIL)) {
            return;                     // 正在格挡
        }
        // 玩家得贴得够近（远处挥空不该触发格挡）
        if (mob.distanceToSqr(target) > 16.0D) {
            return;
        }
        double chance = blockChance(mob.effectiveRank());
        if (chance <= 0.0D || mob.getRandom().nextDouble() >= chance * 0.25D) {
            // 0.25 是"每 tick 的采样率"—— 否则按 tick 累加会变成必挡
            return;
        }
        if (!faces(mob, target)) {
            return;
        }
        pd.putLong(TAG_UNTIL, now + GUARD_TICKS);
        pd.putLong(TAG_PERFECT, now + PERFECT_TICKS);
        pd.putLong(TAG_CD, now + COOLDOWN_TICKS);
        // 起手表现：身前聚一道气（让玩家看得出"它要挡了"）
        Vec3 look = mob.getLookAngle();
        level.sendParticles(ParticleTypes.END_ROD,
                mob.getX() + look.x * 0.7D,
                mob.getY() + mob.getBbHeight() * 0.6D,
                mob.getZ() + look.z * 0.7D,
                4, 0.25D, 0.25D, 0.25D, 0.01D);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.35F, 1.5F);
        // 气盾表现：只发一次，客户端自己算淡入淡出（比每 tick 同步省几十倍带宽）
        try {
            var pkt = new com.hongmeng.xiuxian.network.MobGuardFxPacket(
                    mob.getId(), GUARD_TICKS);
            for (ServerPlayer other : level.players()) {
                if (other.distanceToSqr(mob) <= 48.0D * 48.0D) {
                    com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(other, pkt);
                }
            }
        } catch (Throwable ignored) {
            // 表现失败不影响格挡判定
        }
    }

    /**
     * 玩家打到这只怪时的结算。
     *
     * @return 修正后的伤害
     */
    public static double onHurt(CultivationMonsterEntity mob, double damage,
                                ServerPlayer attacker) {
        if (mob == null || damage <= 0.0D) {
            return damage;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return damage;
        }
        var pd = mob.getPersistentData();
        long now = level.getGameTime();
        if (now > pd.getLong(TAG_UNTIL)) {
            return damage;              // 没在格挡
        }
        // 背后打过来 → 格挡无效（与玩家的背刺规则对称）
        if (!faces(mob, attacker)) {
            pd.putLong(TAG_UNTIL, now);
            return damage;
        }
        boolean perfect = now <= pd.getLong(TAG_PERFECT);
        double cut = perfect ? PERFECT_CUT : NORMAL_CUT;
        double after = damage * (1.0D - cut);

        if (perfect) {
            pd.putLong(TAG_UNTIL, now);   // 一次弹反只挡一下
            stagger(level, attacker, mob);
        } else {
            pd.putLong(TAG_UNTIL, now + 4);
        }
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8F, perfect ? 1.4F : 1.0F);
        level.sendParticles(ParticleTypes.CRIT,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.6D, mob.getZ(),
                perfect ? 14 : 6, 0.35D, 0.3D, 0.35D, perfect ? 0.15D : 0.06D);
        return after;
    }

    /** 弹反成功：玩家踉跄（减速 + 短击退）。 */
    private static void stagger(ServerLevel level, ServerPlayer player,
                                CultivationMonsterEntity mob) {
        try {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    STAGGER_TICKS, 1, false, false, true));
            // 朝远离怪的方向击退
            Vec3 away = player.position().subtract(mob.position());
            if (away.lengthSqr() < 0.01D) {
                away = new Vec3(0.0D, 0.0D, 1.0D);
            }
            away = away.normalize().scale(STAGGER_KNOCKBACK);
            player.setDeltaMovement(player.getDeltaMovement().add(away.x, 0.18D, away.z));
            player.hurtMarked = true;
            player.displayClientMessage(Component.literal(
                    "\u00a7c\u00a7l被弹开 \u00a77· \u00a78这一下被挡下了"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.7F, 0.9F);
        } catch (Throwable ignored) {
            // 踉跄失败不影响伤害
        }
    }

    /** 目标是否在怪的正前方。 */
    private static boolean faces(CultivationMonsterEntity mob, net.minecraft.world.entity.Entity who) {
        if (who == null) {
            return false;
        }
        try {
            Vec3 to = who.position().subtract(mob.position());
            if (to.lengthSqr() < 0.01D) {
                return true;
            }
            return mob.getLookAngle().normalize().dot(to.normalize()) > FACING_DOT;
        } catch (Throwable t) {
            return false;
        }
    }
}
