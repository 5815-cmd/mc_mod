package com.hongmeng.xiuxian.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * <b>怪物的翻滚闪避</b>：让高难战斗真正对称。
 *
 * <h2>为什么需要</h2>
 * <p>玩家现在有四种防御手段：格挡、翻滚、处决、背刺。
 * 而怪只有<b>格挡</b>一种 —— 面对玩家的连招，怪始终是"站着挨打或挡一下"。
 * 加上闪避之后，玩家必须面对一个新问题：<b>这一下会不会被闪掉？</b></p>
 *
 * <h2>只有精英以上会闪</h2>
 * <p>杂鱼会闪的话，玩家在低境界会陷入"打不中"的挫败。
 * 和格挡一样 —— 闪避是<b>精英的标志</b>。</p>
 *
 * <h2>为什么闪避要有冷却，而且比格挡长</h2>
 * <p>闪避是<b>完全免伤</b>（不像格挡只减伤）。如果冷却短，
 * 玩家会陷入"砍十下被闪八下"的地狱。所以：</p>
 * <ul>
 *   <li>概率按品阶 20%~26%，<b>首领反而更低</b>（大块头不该灵活）；</li>
 *   <li>冷却 {@value #COOLDOWN_TICKS} tick（3.5 秒）—— 远长于格挡的 90 tick 里的"有效格挡次数"；</li>
 *   <li>僵直中<b>不能</b>闪（它 AI 都停了，闪避会显得荒谬）。</li>
 * </ul>
 *
 * <h2>闪避是"位移"不是"瞬移"</h2>
 * <p>给一个横向初速，让它真的<b>滚出去</b>。瞬移会让玩家以为"卡了"。</p>
 */
public final class MobDodge {

    /** 冷却（tick）—— 比格挡长得多，因为闪避是完全免伤。 */
    public static final int COOLDOWN_TICKS = 70;

    /** 闪避的横向初速（格/tick）。 */
    public static final double SPEED = 0.72D;

    /** 闪避距离（用于选落点）。 */
    public static final double DISTANCE = 2.6D;

    private static final String TAG_CD = "hm_dodge_cd";

    private MobDodge() {
    }

    /** 各品阶的闪避概率（首领反而低：大块头不灵活）。 */
    private static double chanceOf(MobRank rank) {
        return switch (rank) {
            case ELITE -> 0.20D;
            case CAPTAIN -> 0.26D;
            case BOSS -> 0.12D;
            case OVERLORD -> 0.12D;
            default -> 0.0D;
        };
    }

    /**
     * 玩家这一下打过来时，怪是否有机会闪掉。
     *
     * @return true 表示<b>闪掉了</b>（调用方应把伤害设为 0）
     */
    public static boolean tryDodge(CultivationMonsterEntity mob, ServerPlayer attacker,
                                   double damage) {
        if (mob == null || damage <= 0.0D) {
            return false;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        var pd = mob.getPersistentData();
        long now = level.getGameTime();
        if (now < pd.getLong(TAG_CD)) {
            return false;               // 冷却中
        }
        // 僵直中不能闪（AI 都停了，闪避会显得荒谬）
        if (com.hongmeng.xiuxian.combat.StunScheduler.isStunned(mob.getUUID(), now)) {
            return false;
        }
        if (mob.isNoAi()) {
            return false;
        }
        double chance = chanceOf(mob.effectiveRank());
        if (chance <= 0.0D || mob.getRandom().nextDouble() >= chance) {
            return false;
        }

        // ---- 真的滚出去：沿垂直于"玩家→怪"的方向横移 ----
        Vec3 rel = mob.position().subtract(attacker.position());
        if (rel.lengthSqr() < 0.01D) {
            rel = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 away = rel.normalize();
        // 垂直方向（左右任选）
        double side = mob.getRandom().nextBoolean() ? 1.0D : -1.0D;
        Vec3 dir = new Vec3(-away.z * side, 0.0D, away.x * side).normalize()
                .scale(SPEED);
        mob.setDeltaMovement(dir.x, 0.20D, dir.z);
        mob.hurtMarked = true;

        pd.putLong(TAG_CD, now + COOLDOWN_TICKS);

        // 表现：尘土 + 风声（让"闪掉了"这件事看得见）
        level.sendParticles(ParticleTypes.CLOUD,
                mob.getX(), mob.getY() + 0.15D, mob.getZ(),
                7, 0.3D, 0.1D, 0.3D, 0.04D);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.5F, 1.9F);
        attacker.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a77它闪开了。"), true);
        return true;
    }

    /** 清除冷却（重置世界时用）。 */
    public static void reset(CultivationMonsterEntity mob) {
        if (mob != null) {
            mob.getPersistentData().remove(TAG_CD);
        }
    }

}
