package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 服务端粒子发射器：**所有战斗特效的粒子都从这里出去**。
 *
 * <h2>三条纪律</h2>
 *
 * <ol>
 *   <li><b>全部走 {@link ParticleBudget}</b>：先乘密度倍率（配置），再过每 tick 预算
 *       （配置），批不到就少发或不发。没有一条"绕过预算"的后门。</li>
 *   <li><b>爆发放"命中点"，不是全场铺</b>（规格 §零 第 6 条）。
 *       放射爆发用一次 {@code sendParticles} 的随机方向 + 速度实现——
 *       一次调用就是一次爆发（网络开销也最小）。</li>
 *   <li><b>只在服务端调用</b>（要 {@link ServerLevel}）；客户端的纯本地特效
 *       （领域展开的环、压迫感的抖动）自己用 {@code ClientLevel.addParticle}。</li>
 * </ol>
 *
 * <h2>八元素的"起承转合"</h2>
 *
 * <pre>
 *   起 windup()   手上/身前凝聚（已有 castTicks 蓄力系统，这里只加表现）
 *   承 trail()    沿弹道拖尾（粒子带初速，看起来是"飞过去"的）
 *   转 burst()    命中点的放射爆发 + 闪白
 *   合 afterglow() 命中点缓慢消散（排程到未来几十 tick）
 * </pre>
 */
public final class CombatParticles {

    private CombatParticles() {
    }

    // ------------------------------------------------------------------
    // 起（前摇）
    // ------------------------------------------------------------------

    /**
     * 前摇凝聚：围绕施法者脚下/身前向内收拢的元素粒子。
     *
     * <p>{@code castTicks} 越长（蓄力越久）粒子越多，玩家能从"凝聚的密度"
     * 直接读出"这一发有多大"。</p>
     */
    public static void windup(ServerLevel level, Vec3 center, Element element, int castTicks) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        ElementVfx.Flavor f = ElementVfx.of(element);
        int want = ElementVfx.windupCount(element, ParticleBudget.density());
        int n = ParticleBudget.spend(level, want);
        if (n <= 0) {
            return;
        }
        ParticleOptions p = VfxParticles.of(f.windupParticle());
        // 一圈向内收拢：位置在半径 1.1 的圆上，速度指向圆心
        ring(level, center.add(0.0D, 0.9D, 0.0D), 1.1D, p, n, 0.06D, -0.02D);
    }

    /** 前摇的每一次脉冲（蓄力期间每 4 tick 调一次，越接近完成越亮）。 */
    public static void windupPulse(ServerLevel level, Vec3 center, Element element, float progress) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        int want = CombatFeel.clampCount(4 + (int) (8.0F * CombatFeel.clamp01(progress)),
                ParticleBudget.density(), 16);
        int n = ParticleBudget.spend(level, want);
        if (n <= 0) {
            return;
        }
        ElementVfx.Flavor f = ElementVfx.of(element);
        level.sendParticles(VfxParticles.of(f.windupParticle()),
                center.x, center.y + 0.9D, center.z,
                n, 0.45D, 0.55D, 0.45D, 0.02D);
    }

    // ------------------------------------------------------------------
    // 承（拖尾飞行）
    // ------------------------------------------------------------------

    /**
     * 沿一条路径撒拖尾粒子。
     *
     * <p>粒子带<b>沿路径方向的初速</b>，所以即使是一次性撒完，看起来也是"飞过去"的
     * （而不是"啪一下出现一条线"）。</p>
     *
     * @param from 起点
     * @param to   终点
     */
    public static void trail(ServerLevel level, Vec3 from, Vec3 to, Element element) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        int steps = (int) Math.max(3.0D, Math.min(40.0D, length / 0.8D));
        int per = ElementVfx.trailCount(element, ParticleBudget.density());
        int want = steps * per;
        int allowed = ParticleBudget.spend(level, want);
        if (allowed <= 0) {
            return;
        }
        // 允许的数量可能少于想要：按比例减少每一步的粒子（而不是砍掉后半段路径）
        double ratio = (double) allowed / (double) Math.max(1, want);
        ElementVfx.Flavor f = ElementVfx.of(element);
        ParticleOptions p = VfxParticles.of(f.trailParticle());
        Vec3 dir = length < 1.0E-4D ? new Vec3(0.0D, 0.0D, 0.0D) : delta.scale(1.0D / length);
        for (int i = 0; i < steps; i++) {
            double t = (i + 0.5D) / steps;
            Vec3 at = from.add(delta.scale(t));
            int n = (int) Math.round(per * ratio);
            if (n <= 0) {
                continue;
            }
            level.sendParticles(p, at.x, at.y, at.z, n,
                    dir.x * 0.25D, dir.y * 0.25D, dir.z * 0.25D, 0.35D);
        }
    }

    // ------------------------------------------------------------------
    // 转（命中：爆发 + 闪白）
    // ------------------------------------------------------------------

    /**
     * 命中点的放射爆发——**规格 §零 第 6 条**："只在命中点做放射状爆发，不是全场铺"。
     */
    public static void burst(ServerLevel level, Vec3 at, Element element, double damageFraction) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        ElementVfx.Flavor f = ElementVfx.of(element);
        int want = ElementVfx.burstCount(element, damageFraction, ParticleBudget.density());
        int n = ParticleBudget.spend(level, want);
        if (n <= 0) {
            return;
        }
        // 主粒子：随机方向的放射爆发（一次调用 = 一次爆发）
        level.sendParticles(VfxParticles.of(f.burstParticle()),
                at.x, at.y, at.z, n, 0.28D, 0.28D, 0.28D, 0.42D);
        // 点睛粒子：少量、更亮（雷=白闪、金=刃光、风=落叶…）
        int accent = ParticleBudget.spend(level, Math.max(2, n / 3));
        if (accent > 0) {
            level.sendParticles(VfxParticles.of(f.burstAccent()),
                    at.x, at.y, at.z, accent, 0.4D, 0.4D, 0.4D, 0.28D);
        }
    }

    /**
     * 闪白：命中瞬间在受击者身上放一颗原版 {@code FLASH}（一颗就够——它自带一次白闪）。
     *
     * <p>规格 §零 第 3 条"命中闪白"的可见部分由两件事一起承担：
     * 原版 {@code hurtTime} 拉长（见 {@code CombatFeedback}）+ 这里的白闪粒子。</p>
     */
    public static void flash(ServerLevel level, Vec3 at, double damageFraction) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        // 只有 2% 以上的命中才闪白，平砍一直闪会瞎
        if (damageFraction < CombatFeel.HITSTOP_THRESHOLD) {
            return;
        }
        int n = ParticleBudget.spend(level, 2);
        if (n <= 0) {
            return;
        }
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        if (damageFraction > CombatFeel.HITSTOP_HEAVY) {
            // 大招：再补一层白闪（两层叠起来是"整个屏幕白了一下"）
            level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 0.4D, at.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 小火花：平砍（&lt;2%）也要有一点"打到了"的反馈。 */
    public static void hitSpark(ServerLevel level, Vec3 at, Element element) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        int n = ParticleBudget.spend(level, CombatFeel.clampCount(4, ParticleBudget.density(), 8));
        if (n <= 0) {
            return;
        }
        ElementVfx.Flavor f = ElementVfx.of(element);
        level.sendParticles(VfxParticles.of(f.burstAccent()),
                at.x, at.y, at.z, n, 0.2D, 0.2D, 0.2D, 0.25D);
    }

    // ------------------------------------------------------------------
    // 合（余韵）
    // ------------------------------------------------------------------

    /**
     * 命中点的余韵：排程到未来 {@link CombatFeel#afterglowTicks(double)} tick 里，
     * 每 5 tick 一缕，缓慢消散。
     */
    public static void afterglow(ServerLevel level, Vec3 at, Element element, double damageFraction) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        int total = CombatFeel.afterglowTicks(damageFraction);
        int puffs = Math.max(1, total / 5);
        for (int i = 1; i <= puffs; i++) {
            final int index = i;
            CombatFxScheduler.schedule(level, i * 5, lvl -> {
                int want = ElementVfx.afterglowCount(element, ParticleBudget.density());
                int n = ParticleBudget.spend(lvl, want);
                if (n <= 0) {
                    return;
                }
                ElementVfx.Flavor f = ElementVfx.of(element);
                // 越往后越淡、越往下沉（"余烬飘落"/"沙尘沉降"）
                double rise = 0.35D - 0.05D * index;
                lvl.sendParticles(VfxParticles.of(f.afterglowParticle()),
                        at.x, at.y + rise, at.z, n, 0.55D, 0.25D, 0.55D, 0.015D);
            });
        }
    }

    /**
     * 地面残留（焦痕/冰霜/裂纹的"粒子版"）。
     *
     * <p>规格 §四 的"合"里写了"地面残留焦痕/冰霜/裂纹"。真正改方块材质是 P2
     * 而且要动 {@code domain/**} 之外的世界数据，这里用<b>贴地的一圈粒子</b>表达，
     * 既不破坏地形，也不留下存档垃圾。</p>
     */
    public static void groundResidue(ServerLevel level, Vec3 at, Element element, double damageFraction) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        if (damageFraction < CombatFeel.HITSTOP_MEDIUM) {
            return;     // 只有大招才留痕
        }
        int want = CombatFeel.clampCount(14, ParticleBudget.density(), 24);
        int n = ParticleBudget.spend(level, want);
        if (n <= 0) {
            return;
        }
        ElementVfx.Flavor f = ElementVfx.of(element);
        // 贴地一圈：位置在半径 1.6 的圆上（用 ring 保证"一圈"的形状）
        CombatFxScheduler.schedule(level, 2, lvl ->
                ring(lvl, at, 1.6D, VfxParticles.of(f.afterglowParticle()), n, 0.0D, 0.01D));
    }

    // ------------------------------------------------------------------
    // 大场面（领域 / 首领登场）
    // ------------------------------------------------------------------

    /** 一圈水平扩散的冲击环（从脚下向外）。 */
    public static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle,
                            int count, double outwardSpeed, double upwardSpeed) {
        if (!ParticleBudget.enabled() || count <= 0) {
            return;
        }
        int n = ParticleBudget.spend(level, count);
        if (n <= 0) {
            return;
        }
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n;
            double dx = Math.cos(a) * radius;
            double dz = Math.sin(a) * radius;
            level.sendParticles(particle, center.x + dx, center.y + 0.15D, center.z + dz,
                    1, Math.cos(a) * outwardSpeed, upwardSpeed, Math.sin(a) * outwardSpeed, 0.0D);
        }
    }

    /** 首领登场的径向冲击波：三圈，一圈比一圈大、一圈比一圈慢。 */
    public static void bossShockwave(ServerLevel level, Vec3 center) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        ring(level, center, 1.0D, ParticleTypes.EXPLOSION, 8, 0.05D, 0.02D);
        ring(level, center, 2.2D, ParticleTypes.LARGE_SMOKE, 14, 0.10D, 0.03D);
        ring(level, center, 3.4D, ParticleTypes.SOUL_FIRE_FLAME, 16, 0.14D, 0.05D);
    }

    /** 领域展开：一圈从脚下向外扩散的元素冲击环（规格 §3.3）。 */
    public static void domainRing(ServerLevel level, Vec3 center, Element element) {
        if (!ParticleBudget.enabled()) {
            return;
        }
        ElementVfx.Flavor f = ElementVfx.of(element);
        ring(level, center, 1.2D, VfxParticles.of(f.burstParticle()), 20, 0.35D, 0.04D);
        ring(level, center, 2.6D, VfxParticles.of(f.trailParticle()), 24, 0.30D, 0.02D);
        ring(level, center, 4.2D, VfxParticles.of(f.afterglowParticle()), 24, 0.22D, 0.01D);
    }
}
