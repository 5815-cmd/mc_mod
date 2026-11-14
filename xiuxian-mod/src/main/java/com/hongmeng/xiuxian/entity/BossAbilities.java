package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * <b>Boss 的远程攻击与法术</b>（玩家要求）。
 *
 * <p>玩家原话：「boss 攻击也要有特效，boss 也会释放法术什么的，
 * <b>boss 不仅仅会近战还会远程</b>」。</p>
 *
 * <h2>三种攻击手段</h2>
 * <table border="1">
 *   <caption>按距离自动切换</caption>
 *   <tr><th>手段</th><th>触发距离</th><th>表现</th></tr>
 *   <tr><td><b>近战</b></td><td>&lt; 4 格</td><td>原版近战（已有）</td></tr>
 *   <tr><td><b>远程吐息</b></td><td>4 ~ 32 格</td><td>发射元素投射物（复用蛟龙吐息的实体）+
 *       口部粒子 + 音效</td></tr>
 *   <tr><td><b>法术</b></td><td>8 ~ 48 格</td><td>在目标脚下召出<b>法术法阵</b>并引爆 ——
 *       范围伤害 + 元素负面效果 + 完整贴图特效</td></tr>
 * </table>
 *
 * <h2>为什么要按距离分档</h2>
 * <p>如果只做近战，玩家"风筝"（边跑边打）就永远安全 —— Boss 会变成木桩。
 * 按距离分档之后，无论远近都有威胁，这才是"会打架"的 Boss。</p>
 *
 * <h2>性能</h2>
 * <p>所有判定都**节流**（远程 2 秒一次、法术 6 秒一次），而且只在
 * <b>Boss 品阶</b>上生效。普通怪一个都不受影响。</p>
 */
public final class BossAbilities {

    /** 二阶段专属【魔影突袭】冷却（tick）。 */
    private static final int AMBUSH_CD = 200;
    /** 三阶段专属【天崩地裂】冷却（tick）。 */
    private static final int CATACLYSM_CD = 260;
    /** 突袭预警时长（tick）——给玩家回头的时间。 */
    public static final int AMBUSH_WINDUP = 12;
    /** 冲击波圈数。 */
    public static final int CATACLYSM_WAVES = 3;

    /** 远程吐息间隔（tick）。 */
    private static final int RANGED_CD = 40;
    /** 法术间隔（tick）。 */
    private static final int SPELL_CD = 120;
    /** 吐息射程。 */
    private static final double RANGED_RANGE = 32.0D;
    /** 法术射程。 */
    private static final double SPELL_RANGE = 48.0D;

    private BossAbilities() {
    }

    /**
     * 每 tick 调一次（由 {@code CultivationMonsterEntity.customServerAiStep} 转发）——
     * <b>全模组 Boss 技能的唯一 tick 入口</b>（第四轮保持不变，仍然只有这一个调用点）。
     *
     * <p>第四轮起这里变成<b>路由器</b>：</p>
     * <ul>
     *   <li><b>覆盖名单</b>（四个祭坛 Boss + 焚天炎帝 + 虚空龙帝 + 仙庭之主）→ 交给
     *       {@link BossAttackKit} 的 <b>IDLE→前摇→判定→后摇</b> 状态机：招式有前摇、
     *       有地面预警、玩家能跑出范围；</li>
     *   <li><b>恶</b>（{@code evil_gazer}）→ 最小接入：只加狂暴，技能仍归
     *       {@code EvilAbilities}，下面的旧轮询保留；</li>
     *   <li>其余 BOSS/首领品阶（魔道统领、虚空霸主、……）→ 原样走 {@link #legacyTick}，
     *       <b>行为与第四轮之前完全一致</b>。</li>
     * </ul>
     *
     * @param boss 怪物本体
     */
    public static void tick(CultivationMonsterEntity boss) {
        BossAttackKit.Role role = BossAttackKit.roleOf(boss);
        if (role == BossAttackKit.Role.KIT) {
            BossAttackKit.tick(boss);
            return;
        }
        if (role == BossAttackKit.Role.MINIMAL) {
            // 只接通用狂暴；「恶」的 7 招状态机在 EvilAbilities 里，不能再跑旧轮询（会重复放技能）
            BossAttackKit.tickEnrage(boss);
            return;
        }
        legacyTick(boss);
    }

    /**
     * 第四轮之前的「按 tick 轮询放技能」原逻辑。
     *
     * <p>未进 {@link BossAttackKit} 覆盖名单的 BOSS/首领继续用这一套（逐字保留），
     * 保证魔道统领 / 虚空霸主等旧 Boss 的手感不变。</p>
     */
    private static void legacyTick(CultivationMonsterEntity boss) {
        MobRank rank = boss.effectiveRank();
        if (rank != MobRank.BOSS && rank != MobRank.OVERLORD) {
            return;
        }
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double dist = boss.distanceTo(target);
        var pd = boss.getPersistentData();

        // ---- 阶段专属大招（优先于普通技能）----
        int phase = BossPhase.phaseOf(boss);
        if (phase >= 2) {
            int ambushCd = pd.getInt("hm_boss_ambush_cd");
            if (ambushCd > 0) {
                pd.putInt("hm_boss_ambush_cd", ambushCd - 1);
            } else if (dist >= 6.0D && dist <= 24.0D
                    && boss.getRandom().nextFloat() < 0.02F) {
                // 2%/tick 的采样：平均约 2.5 秒一次尝试，冷却 10 秒
                if (ambush(level, boss, target)) {
                    pd.putInt("hm_boss_ambush_cd", AMBUSH_CD);
                }
            }
        }
        if (phase >= 3) {
            int catCd = pd.getInt("hm_boss_cataclysm_cd");
            if (catCd > 0) {
                pd.putInt("hm_boss_cataclysm_cd", catCd - 1);
            } else if (dist <= 10.0D && boss.getRandom().nextFloat() < 0.02F) {
                if (cataclysm(level, boss, target)) {
                    pd.putInt("hm_boss_cataclysm_cd", CATACLYSM_CD);
                }
            }
        }

        // ---- 法术：优先级最高（冷却最长，所以先判）----
        int spellCd = pd.getInt("hm_boss_spell_cd");
        if (spellCd > 0) {
            pd.putInt("hm_boss_spell_cd", spellCd - 1);
        } else if (dist >= 8.0D && dist <= SPELL_RANGE) {
            if (castSpell(level, boss, target)) {
                pd.putInt("hm_boss_spell_cd", SPELL_CD);
            }
        }

        // ---- 远程吐息 ----
        int rangedCd = pd.getInt("hm_boss_ranged_cd");
        if (rangedCd > 0) {
            pd.putInt("hm_boss_ranged_cd", rangedCd - 1);
            return;
        }
        if (dist >= 4.0D && dist <= RANGED_RANGE) {
            if (fireBreath(level, boss, target)) {
                pd.putInt("hm_boss_ranged_cd", RANGED_CD);
            }
        }
    }

    /**
     * <b>二阶段专属：魔影突袭</b>。
     *
     * <p>Boss 先<b>隐去身形</b>（0.6 秒预警，玩家有机会回头），
     * 然后出现在玩家<b>背后</b>并重击。</p>
     *
     * <p>为什么"出现在背后"很重要：本作的<b>背击穿透格挡</b>（{@code Guard.isBackstab}）
     * 会让这一下没法挡 —— 逼玩家必须真的转身，而不是站着按右键。</p>
     */
    private static boolean ambush(ServerLevel level, CultivationMonsterEntity boss,
                                  LivingEntity target) {
        try {
            Vec3 from = boss.position();
            // 预警：原地冒黑烟 + 低吼（"它要消失了"）
            level.sendParticles(ParticleTypes.SQUID_INK,
                    from.x, from.y + boss.getBbHeight() * 0.5D, from.z,
                    30, 0.6D, 0.9D, 0.6D, 0.02D);
            // 专属音：反向渐强 + 下潜 —— 玩家会下意识觉得"它要动了"
            level.playSound(null, boss.blockPosition(),
                    com.hongmeng.xiuxian.ModSounds.BOSS_AMBUSH.get(),
                    SoundSource.HOSTILE, 1.3F, 1.0F);

            // 延迟到预警结束再位移（给玩家反应时间）
            com.hongmeng.xiuxian.combat.StunScheduler.after(level, AMBUSH_WINDUP, () -> {
                try {
                    if (!boss.isAlive() || !target.isAlive()) {
                        return;
                    }
                    // 出现在目标背后 2 格
                    Vec3 look = target.getLookAngle();
                    Vec3 behind = target.position().subtract(look.normalize().scale(2.0D));
                    boss.teleportTo(behind.x, target.getY(), behind.z);
                    boss.getNavigation().stop();
                    // 到位立刻打一下（走原版攻击结算，遵守既有伤害规则）
                    boss.doHurtTarget(target);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            target.getX(), target.getY() + 1.2D, target.getZ(),
                            3, 0.3D, 0.3D, 0.3D, 0.0D);
                    level.playSound(null, target.blockPosition(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE,
                            1.0F, 0.7F);
                } catch (Throwable ignored) {
                    // 突袭失败不影响其它技能
                }
            });
            return true;
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[boss] 突袭失败：{}", t.toString());
            return false;
        }
    }

    /**
     * <b>三阶段专属：天崩地裂</b>。
     *
     * <p>砸地 → {@value #CATACLYSM_WAVES} 圈冲击波依次向外扩散。
     * 每一圈都是一次独立判定，且<b>只在环带上伤害</b>（不是整片圆），
     * 所以玩家可以"卡在环与环之间"或跳/翻滚躲过。</p>
     *
     * <p>不破坏任何方块 —— 纯伤害与表现。</p>
     */
    private static boolean cataclysm(ServerLevel level, CultivationMonsterEntity boss,
                                     LivingEntity target) {
        try {
            Vec3 at = boss.position();
            // 专属音：巨震 + 余震 + 碎石
            level.playSound(null, boss.blockPosition(),
                    com.hongmeng.xiuxian.ModSounds.BOSS_CATACLYSM.get(),
                    SoundSource.HOSTILE, 1.6F, 1.0F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    at.x, at.y + 0.4D, at.z, 2, 0.4D, 0.2D, 0.4D, 0.0D);

            for (int wave = 0; wave < CATACLYSM_WAVES; wave++) {
                final int w = wave;
                // 每圈间隔 12 tick：给玩家"躲下一圈"的节奏
                com.hongmeng.xiuxian.combat.StunScheduler.after(level, 6 + w * 12, () -> {
                    try {
                        if (!boss.isAlive()) {
                            return;
                        }
                        double radius = 3.0D + w * 2.6D;
                        // 表现：一圈火光
                        //
                        // ⚠ 必须走 ParticleBatch：逐颗 sendParticles 在循环里
                        //   每个粒子一个网络包（项目硬线，检查器会拦）。
                        int seg = 24;
                        var batch = new com.hongmeng.xiuxian.technique.ParticleBatch(level);
                        for (int i = 0; i < seg; i++) {
                            double a = 2.0D * Math.PI * i / seg;
                            batch.add(ParticleTypes.FLAME,
                                    at.x + Math.cos(a) * radius,
                                    at.y + 0.15D,
                                    at.z + Math.sin(a) * radius);
                        }
                        batch.flush();
                        // 每一圈再响一次（音高逐圈升高，像"越来越近"）
                        level.playSound(null, boss.blockPosition(),
                                com.hongmeng.xiuxian.ModSounds.BOSS_CATACLYSM.get(),
                                SoundSource.HOSTILE, 0.75F, 1.25F + w * 0.18F);
                        // 判定：环带上的玩家受伤（内外都有容差）
                        for (ServerPlayer p : level.players()) {
                            double d = Math.hypot(p.getX() - at.x, p.getZ() - at.z);
                            if (Math.abs(d - radius) > 1.6D) {
                                continue;
                            }
                            if (p.getY() - at.y > 3.0D) {
                                continue;   // 站太高不算
                            }
                            // 走魔法伤害，遵守既有的减伤与软上限
                            p.hurt(level.damageSources().magic(),
                                    (float) (boss.getAttributeValue(
                                            net.minecraft.world.entity.ai.attributes
                                                    .Attributes.ATTACK_DAMAGE) * 0.55D));
                        }
                    } catch (Throwable ignored) {
                        // 单圈失败不影响下一圈
                    }
                });
            }
            return true;
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[boss] 天崩地裂失败：{}", t.toString());
            return false;
        }
    }

    /**
     * 发射元素吐息（复用已有的蛟龙吐息投射物）。
     *
     * <p>包级私有：第四轮起 {@link BossAttackKit} 的 {@code BREATH} 招式直接调它
     * （只在"前摇走完"的判定帧调用，所以玩家有 10t 可以走位）。</p>
     */
    static boolean fireBreath(ServerLevel level, CultivationMonsterEntity boss,
                              LivingEntity target) {
        try {
            var spit = new com.hongmeng.xiuxian.entity.beast.JiaolongSpitEntity(
                    com.hongmeng.xiuxian.entity.ModEntities.VENOM_SPIT.get(), level, boss);
            // 从口部发出
            Vec3 from = boss.position().add(0.0D, boss.getBbHeight() * 0.8D, 0.0D);
            Vec3 dir = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                    .subtract(from).normalize();
            spit.setPos(from.x, from.y, from.z);
            spit.shoot(dir.x, dir.y, dir.z, 1.4F, 2.0F);
            level.addFreshEntity(spit);

            // 表现：口部粒子 + 音效（"boss 攻击也要有特效"）
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, from.x, from.y, from.z,
                    14, 0.35D, 0.35D, 0.35D, 0.04D);
            level.playSound(null, boss.blockPosition(), SoundEvents.BLAZE_SHOOT,
                    SoundSource.HOSTILE, 1.6F, 0.7F);
            return true;
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[boss] 远程吐息失败：{}", t.toString());
            return false;
        }
    }

    /**
     * 释放法术：在目标脚下召出法阵并引爆。
     *
     * <p>用<b>贴图特效 + 范围伤害 + 元素负面效果</b>三件套，
     * 与玩家自己放法术的观感对齐（玩家要求"boss 召唤特效要对标法术特效"）。</p>
     *
     * <p>包级私有：第四轮起 {@link BossAttackKit} 的 {@code SPELL} 招式直接调它
     * （前摇走完才引爆，玩家有 20t 可以离开法阵）。</p>
     */
    static boolean castSpell(ServerLevel level, CultivationMonsterEntity boss,
                             LivingEntity target) {
        try {
            Vec3 at = target.position();
            // ---- 特效尺度（玩家要求逐条确认）----
            //
            // 玩家原话：「BOSS 的法术特效小一点，像普通法术就行，
            //           像那种灭世法术就祭坛召唤的 boss 第三阶段变身的时候放，
            //           普通怪潮里的头目 boss 不需要会放灭世法术」。
            //
            // 所以：普通头目 = 0.6F（普通法术尺度，只有小法阵 + 冲击环）；
            //       祭坛 Boss 第三阶段 = 1.3F（灭世级五层）。
            int phase = com.hongmeng.xiuxian.entity.BossPhase.phaseOf(boss);
            boolean altarBoss = boss.getPersistentData()
                    .getBoolean("hm_arena_boss");
            float fxScale = (altarBoss && phase >= 3) ? 1.3F : 0.6F;
            com.hongmeng.xiuxian.technique.SpellFxRuntime.playPhase(level, at,
                    boss.elementForPhaseFx(), fxScale);
            // 2) 粒子点缀
            level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 0.4D, at.z,
                    3, 0.0D, 0.0D, 0.0D, 0.0D);
            // 粒子数量也跟着尺度走（普通头目不铺满半个屏幕）
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.6D, at.z,
                    (altarBoss && phase >= 3) ? 60 : 22,
                    (altarBoss && phase >= 3) ? 2.6D : 1.4D, 0.7D,
                    (altarBoss && phase >= 3) ? 2.6D : 1.4D, 0.06D);
            // 3) 音效
            level.playSound(null, boss.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.HOSTILE, 2.0F, 0.6F);

            // 4) 范围伤害 + 负面效果（半径 5，与特效尺寸匹配）
            double r = 5.0D;
            AABB box = new AABB(at.x - r, at.y - 2.0D, at.z - r,
                    at.x + r, at.y + 3.0D, at.z + r);
            List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> !(e instanceof CultivationMonsterEntity) && e.isAlive());
            float dmg = (float) Math.max(6.0D, boss.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            for (LivingEntity victim : caught) {
                if (victim == boss) {
                    continue;
                }
                victim.hurt(level.damageSources().mobAttack(boss), dmg * 0.9F);
                // 元素负面效果（与玩家的法术同口径）
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        20 * 4, 1, false, true));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        20 * 6, 0, false, true));
                if (victim instanceof ServerPlayer sp) {
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "\u00a7c" + boss.getName().getString()
                                    + " \u00a77的法术轰在你脚下！"), true);
                }
            }
            // 5) 地震：把附近的人掀一下
            for (LivingEntity victim : caught) {
                Vec3 away = victim.position().subtract(at);
                if (away.lengthSqr() > 0.01D) {
                    Vec3 d = away.normalize().scale(0.9D);
                    victim.push(d.x, 0.55D, d.z);
                    victim.hurtMarked = true;
                }
            }
            return true;
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[boss] 法术释放失败：{}", t.toString());
            return false;
        }
    }
}
