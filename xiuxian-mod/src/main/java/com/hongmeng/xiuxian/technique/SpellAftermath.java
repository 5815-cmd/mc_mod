package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>持续法术效果</b>：法术放出去之后**留在场上几秒**，逐 tick 结算与播特效。
 *
 * <h2>为什么要有这个类</h2>
 * <p>玩家反馈：「法术释放出来不能一瞬间就消失了我给你举例一个就是黑洞法术的表现一瞬间就没了」。
 * 根因是原来的灭世法术全是"一次性结算完就结束"。这里给它们加一条**时间线**：
 * 落点、半径、元素、时长记下来，每 tick 推进，期间持续牵引/余震/裂痕/火柱与粒子。</p>
 *
 * <h2>为什么不生成实体</h2>
 * <p>生成实体要额外注册、同步、存档，而且会被清除指令误伤。这里只用一张
 * {@code (玩家, 维度)} → 效果 的内存表 —— 玩家下线或换维度即失效，
 * 对单人玩法完全够用，也**不会写进存档**。</p>
 *
 * <h2>铁律</h2>
 * <p>只发原版粒子 + 施加推力，<b>不生成方块、不改地形</b>。</p>
 */
public final class SpellAftermath {

    /** 一个正在持续的法术效果。 */
    private static final class Effect {
        final Technique technique;
        final Vec3 center;
        final double radius;
        final int totalTicks;
        /**
         * 这次施法的基准伤害（职业主动的持续区域用）。
         *
         * <p>旧 52 门走 {@link #add(ServerPlayer, Technique, Vec3, double, int)} 时
         * 一律为 0，它们的持续结算继续用各自的写死数值 —— <b>行为不变</b>。</p>
         */
        final double power;
        int elapsed;

        Effect(Technique technique, Vec3 center, double radius, int totalTicks,
               double power) {
            this.technique = technique;
            this.center = center;
            this.radius = radius;
            this.totalTicks = totalTicks;
            this.power = power;
        }
    }

    private static final Map<UUID, List<Effect>> ACTIVE = new ConcurrentHashMap<>();

    private SpellAftermath() {
    }

    /** 登记一个持续效果。 */
    /** 默认持续时长（tick）：140 = 7 秒，与贴图特效最长寿命对齐，
     * 保证特效还在时伤害也还在（玩家要求）。 */
    public static final int DEFAULT_TICKS = 140;

    public static void add(ServerPlayer player, Technique technique, Vec3 center,
                           double radius, int ticks) {
        add(player, technique, center, radius, ticks, 0.0D);
    }

    /**
     * 登记一个持续效果（带基准伤害）。
     *
     * <p>职业主动的持续区域（血河 / 鬼域展开 / 心魔引）要按本次施法的伤害缩放，
     * 所以多一个 {@code power} 参数；<b>旧 52 门仍走上面的 5 参重载</b>，
     * 结算路径与数值完全不变。</p>
     */
    public static void add(ServerPlayer player, Technique technique, Vec3 center,
                           double radius, int ticks, double power) {
        if (player == null || technique == null || center == null || ticks <= 0) {
            return;
        }
        ACTIVE.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
                .add(new Effect(technique, center, radius, ticks, power));
    }

    /** 玩家离开/死亡时清掉（避免残留继续扣血）。 */
    public static void clear(UUID id) {
        ACTIVE.remove(id);
    }

    /**
     * 每 tick 推进（由 {@code ModEvents.onPlayerTick} 调用）。
     */
    public static void tick(ServerPlayer player) {
        List<Effect> list = ACTIVE.get(player.getUUID());
        if (list == null || list.isEmpty()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        list.removeIf(e -> advance(player, level, e));
        if (list.isEmpty()) {
            ACTIVE.remove(player.getUUID());
        }
    }

    /** @return true 表示这个效果已结束，应当移除 */
    private static boolean advance(ServerPlayer player, ServerLevel level, Effect e) {
        e.elapsed++;
        double t = (double) e.elapsed / e.totalTicks;      // 0 → 1
        boolean last = e.elapsed >= e.totalTicks;
        try {
            switch (e.technique) {
                case TAI_XU_HEI_DONG -> blackHole(player, level, e, t, last);
                case TIAN_YUN_XING_LUO -> meteor(player, level, e, t, last);
                case ZHAN_XING -> starSlash(player, level, e, t, last);
                case FEN_TIAN_ZHU_HAI -> burnSea(player, level, e, t, last);
                case WAN_XIANG_JI_MIE -> annihilate(player, level, e, t, last);
                case GUI_DAO_PAO_JI -> railgun(player, level, e, t, last);
                // ---- 职业主动（只在 technique 匹配时生效；旧 52 门不走这里）----
                case XUE_HE -> bloodRiver(player, level, e, t, last);
                case GUI_YU_ZHAN_KAI -> ghostDomain(player, level, e, t, last);
                case MO_XIN_MO_YIN -> mindDemon(player, level, e, t, last);
                default -> {
                    // 其它法术没有持续表现
                }
            }
        } catch (Throwable ex) {
            HongmengMod.LOGGER.debug("[hongmeng] 持续法术结算异常：{}", ex.toString());
            return true;
        }
        return last;
    }

    // ------------------------------------------------------------------
    // 各法术的持续表现
    // ------------------------------------------------------------------

    /** 太虚黑洞：持续牵引 + 周期性绞碎 + 末段坍塌。 */
    private static void blackHole(ServerPlayer player, ServerLevel level, Effect e,
                                  double t, boolean last) {
        Vec3 c = e.center;
        double r = e.radius * (1.0D - 0.25D * t);          // 慢慢收拢
        // 吸积盘：持续反向旋转
                com.hongmeng.xiuxian.technique.ParticleBatch pb1 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int layer = 0; layer < 2; layer++) {
            double rr = r * (0.35D + 0.25D * layer);
            int n = 34;
            double spin = e.elapsed * 0.12D * (layer % 2 == 0 ? 1.0D : -1.0D);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0D * i / n + spin;
                pb1.add(layer == 1 ? ParticleTypes.SOUL : ParticleTypes.PORTAL, c.x + Math.cos(a) * rr, c.y + 0.4D * layer, c.z + Math.sin(a) * rr);
            }
        }
        pb1.flush();

        // 事件视界：黑核
        level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 0.6D, c.z, 4, 0.9D, 0.9D, 0.9D, 0.0D);
        // 向内收束
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.6D, c.z, 3, r * 0.7D, r * 0.5D, r * 0.7D, -0.4D);

        if (e.elapsed % 5 != 0) {
            return;
        }
        // 牵引 + 绞碎
        AABB box = new AABB(c.x - r, c.y - 14.0D, c.z - r, c.x + r, c.y + 14.0D, c.z + r);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                v -> v != player && v.isAlive())) {
            Vec3 pull = c.subtract(victim.position());
            double dist = Math.max(0.8D, pull.length());
            if (dist > r) {
                continue;
            }
            Vec3 dir = pull.normalize().scale(Math.min(2.4D, 7.0D / dist));
            victim.push(dir.x, 0.12D, dir.z);
            victim.hurtMarked = true;
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 250,
                    false, false, false));
            // 越靠近中心绞得越狠（每 5 tick 一次）
            // 范围放大之后，留存的持续伤害也同步抬起来（近/中/远三档）——
            // 玩家要求「留存的特效也要带着伤害，后续也要有持续伤害」。
            float dmg = (float) (dist < r * 0.25D ? 42.0D
                    : dist < r * 0.6D ? 24.0D : 12.0D);
            com.hongmeng.xiuxian.domain.DomainDamage.hit(victim,
                    player, dmg);
        }
        // 末段：坍塌大爆 + 一次百分比
        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.6D, c.z, 6, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.6D, c.z, 4, 1.5D, 1.0D, 1.5D, 0.2D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                    v -> v != player && v.isAlive())) {
                com.hongmeng.xiuxian.domain.DomainDamage.hit(victim, player, 22.0F);
                com.hongmeng.xiuxian.domain.DomainDamage.hitPercent(victim, player, 0.04D);
                Vec3 away = victim.position().subtract(c).normalize().scale(2.2D);
                victim.push(away.x, 0.8D, away.z);
                victim.hurtMarked = true;
            }
        }
    }

    /** 天陨星落：余震环逐道外扩 + 地面冒烟。 */
    private static void meteor(ServerPlayer player, ServerLevel level, Effect e,
                               double t, boolean last) {
        Vec3 c = e.center;
        // 每 20 tick 一道外扩环
        if (e.elapsed % 20 == 0) {
            double rr = e.radius * (0.4D + 0.6D * t);
                        com.hongmeng.xiuxian.technique.ParticleBatch pb2 =
                    new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < 45; i++) {
                double a = Math.PI * 2.0D * i / 45.0D;
                pb2.add(ParticleTypes.SWEEP_ATTACK, c.x + Math.cos(a) * rr, c.y + 0.3D, c.z + Math.sin(a) * rr);
                pb2.add(ParticleTypes.CLOUD, c.x + Math.cos(a) * rr, c.y + 0.5D, c.z + Math.sin(a) * rr);
            }
            pb2.flush();

            // 余震伤害
            AABB box = new AABB(c.x - e.radius, c.y - 8.0D, c.z - e.radius,
                    c.x + e.radius, c.y + 10.0D, c.z + e.radius);
            float dmg = (float) (8.0D * (1.0D - t * 0.5D));
            for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, box,
                    x -> x != player && x.isAlive())) {
                com.hongmeng.xiuxian.domain.DomainDamage.hit(v, player, dmg);
            }
        }
        // 落点持续烟柱
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 2.0D + t * 7.0D, c.z,
                3, 1.4D, 0.6D, 1.4D, 0.04D);
        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.0D, c.z, 4, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 斩星：深渊裂痕向上崩裂 + 两侧余波。 */
    private static void starSlash(ServerPlayer player, ServerLevel level, Effect e,
                                  double t, boolean last) {
        Vec3 c = e.center;
        // 裂痕随时间向上长
        double height = 2.0D + t * 24.0D;
                com.hongmeng.xiuxian.technique.ParticleBatch pb3 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int k = 0; k < (int) (height * 0.5D); k++) {
            pb3.add(ParticleTypes.END_ROD, c.x, c.y + k, c.z);
            if (k % 3 == 0) {
                level.sendParticles(ParticleTypes.PORTAL, c.x, c.y + k, c.z,
                        1, 0.5D, 0.0D, 0.5D, 0.03D);
            }
        }
        pb3.flush();

        // 两侧余波斩击
        if (e.elapsed % 12 == 0) {
            for (int side = -1; side <= 1; side += 2) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        c.x + side * e.radius * 0.5D, c.y + 1.5D, c.z,
                        3, e.radius * 0.4D, 1.5D, e.radius * 0.4D, 0.05D);
            }
        }
        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 2.0D, c.z, 5, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 2.0D, c.z,
                    28, e.radius * 0.6D, 3.0D, e.radius * 0.6D, 0.2D);
        }
    }

    /** 焚天煮海：火柱持续喷发 + 焦痕脉动。 */
    private static void burnSea(ServerPlayer player, ServerLevel level, Effect e,
                                double t, boolean last) {
        Vec3 c = e.center;
                com.hongmeng.xiuxian.technique.ParticleBatch pb4 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < 12; i++) {
            pb4.add(ParticleTypes.FALLING_LAVA, c.x + (level.random.nextDouble() - 0.5D) * e.radius * 2.0D, c.y + 10.0D, c.z + (level.random.nextDouble() - 0.5D) * e.radius * 2.0D);
        }
        pb4.flush();

        // 焦痕环脉动
        double rr = e.radius * (0.5D + 0.5D * Math.abs(Math.sin(t * Math.PI)));
                com.hongmeng.xiuxian.technique.ParticleBatch pb5 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2.0D * i / 28.0D;
            pb5.add(ParticleTypes.LAVA, c.x + Math.cos(a) * rr, c.y + 0.15D, c.z + Math.sin(a) * rr);
        }
        pb5.flush();

        // 火柱：随时间移动到别的点
        if (e.elapsed % 10 == 0) {
                        com.hongmeng.xiuxian.technique.ParticleBatch pb6 =
                    new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < 3; i++) {
                double a = level.random.nextDouble() * Math.PI * 2.0D;
                double d = Math.sqrt(level.random.nextDouble()) * e.radius * 0.8D;
                double x = c.x + Math.cos(a) * d;
                double z = c.z + Math.sin(a) * d;
                for (int k = 0; k < 5; k++) {
                    pb6.add(ParticleTypes.FLAME, x, c.y + k * 0.9D, z);
                }
            }
            pb6.flush();

        }
        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.0D, c.z, 4, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 万象寂灭：光柱渐收 + 阵纹退散。 */
    private static void annihilate(ServerPlayer player, ServerLevel level, Effect e,
                                   double t, boolean last) {
        Vec3 c = e.center;
        // 光柱渐收
        double h = 26.0D * (1.0D - t);
                com.hongmeng.xiuxian.technique.ParticleBatch pb7 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int k = 0; k < (int) (h * 0.5D); k++) {
            pb7.add(ParticleTypes.END_ROD, c.x, c.y + k, c.z);
        }
        pb7.flush();

        // 阵纹向外退散
        double rr = e.radius * (0.3D + 0.7D * t);
                com.hongmeng.xiuxian.technique.ParticleBatch pb8 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < 36; i++) {
            double a = Math.PI * 2.0D * i / 36.0D;
            pb8.add(ParticleTypes.ENCHANT, c.x + Math.cos(a) * rr, c.y + 0.2D, c.z + Math.sin(a) * rr);
        }
        pb8.flush();

        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 2.0D, c.z, 6, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 轨道炮击：贯穿光刃残留 + 沿途余爆。 */
    private static void railgun(ServerPlayer player, ServerLevel level, Effect e,
                                double t, boolean last) {
        Vec3 c = e.center;
        double fade = 1.0D - t;
        int n = (int) (28 * fade);
                com.hongmeng.xiuxian.technique.ParticleBatch pb9 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / Math.max(1, n);
            pb9.add(ParticleTypes.ELECTRIC_SPARK, c.x + Math.cos(a) * 3.0D, c.y + 0.5D, c.z + Math.sin(a) * 3.0D);
        }
        pb9.flush();

        if (e.elapsed % 8 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 1.0D, c.z,
                    5, 3.0D, 1.0D, 3.0D, 0.08D);
        }
        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.0D, c.z, 3, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    // ------------------------------------------------------------------
    // 职业主动的持续区域（《07-法术内容交接表》§2）
    // ------------------------------------------------------------------

    /**
     * <b>血河</b>（职业主动·血修）：范围内<b>每 10 tick 小伤害 + 减速</b>。
     *
     * <p>复用本类既有的 Effect/advance 时间线：不生成实体、不写存档、不改地形。
     * {@code e.power} 是施法时的基准伤害（见 6 参 {@link #add}），所以 DoT 随境界缩放，
     * 不写死固定点数。旧 52 门永远不进这个分支。</p>
     */
    private static void bloodRiver(ServerPlayer player, ServerLevel level, Effect e,
                                   double t, boolean last) {
        Vec3 c = e.center;
        double r = e.radius;
        // 表现：地面血色涟漪（每 5 tick 一圈）
        if (e.elapsed % 5 == 0) {
            ParticleBatch pb = new ParticleBatch(level);
            double rr = r * (0.35D + 0.65D * Math.abs(Math.sin(t * Math.PI)));
            for (int i = 0; i < 48; i++) {
                double a = Math.PI * 2.0D * i / 48.0D;
                pb.add(ParticleTypes.CRIMSON_SPORE,
                        c.x + Math.cos(a) * rr, c.y + 0.15D, c.z + Math.sin(a) * rr);
            }
            pb.flush();
            level.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.2D, c.z,
                    4, r * 0.5D, 0.1D, r * 0.5D, 0.0D);
        }
        if (e.elapsed % 10 != 0) {
            return;
        }
        AABB box = new AABB(c.x - r, c.y - 6.0D, c.z - r,
                c.x + r, c.y + 8.0D, c.z + r);
        float dmg = (float) Math.max(4.0D, e.power * 0.05D);
        for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> isHostile(player, x))) {
            double dx = v.getX() - c.x;
            double dz = v.getZ() - c.z;
            if (dx * dx + dz * dz > r * r) {
                continue;
            }
            com.hongmeng.xiuxian.domain.DomainDamage.hit(v, player, dmg);
            v.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1,
                    false, false, true));
        }
        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.0D, c.z,
                    4, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * <b>鬼域展开</b>（职业主动·鬼修）：复用既有鬼域系统 + 本类持续区域。
     *
     * <p>展开时同步调 {@link com.hongmeng.xiuxian.talent.PathRuntime#toggleFormation}
     * （若<b>已经展开</b>则不重复调用），之后的事交给既有鬼域系统；
     * 这里额外补一层"每 10 tick 小伤害 + 减速"，让这次施法的区域有明确的存在感。
     * 同样不生成实体、不改地形。</p>
     */
    private static void ghostDomain(ServerPlayer player, ServerLevel level, Effect e,
                                    double t, boolean last) {
        Vec3 c = e.center;
        double r = e.radius;
        // 首次推进：把"鬼域"真正展开（复用既有 PathRuntime 机制，不另造第二套领域）
        if (e.elapsed == 1
                && !com.hongmeng.xiuxian.talent.PathRuntime.formationOpen(player)) {
            com.hongmeng.xiuxian.talent.PathRuntime.toggleFormation(player);
        }
        // 表现：满地魂火（每 5 tick 一圈）
        if (e.elapsed % 5 == 0) {
            ParticleBatch pb = new ParticleBatch(level);
            double rr = r * (0.55D + 0.45D * Math.abs(Math.sin(t * Math.PI)));
            for (int i = 0; i < 60; i++) {
                double a = Math.PI * 2.0D * i / 60.0D;
                pb.add(ParticleTypes.SOUL,
                        c.x + Math.cos(a) * rr, c.y + 0.2D, c.z + Math.sin(a) * rr);
            }
            pb.flush();
            level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y + 0.6D, c.z,
                    8, r * 0.45D, 0.4D, r * 0.45D, 0.01D);
        }
        if (e.elapsed % 10 != 0) {
            return;
        }
        AABB box = new AABB(c.x - r, c.y - 6.0D, c.z - r,
                c.x + r, c.y + 10.0D, c.z + r);
        float dmg = (float) Math.max(4.0D, e.power * 0.04D);
        for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> isHostile(player, x))) {
            double dx = v.getX() - c.x;
            double dz = v.getZ() - c.z;
            if (dx * dx + dz * dz > r * r) {
                continue;
            }
            com.hongmeng.xiuxian.domain.DomainDamage.hit(v, player, dmg);
            v.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1,
                    false, false, true));
        }
        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.0D, c.z,
                    5, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.SOUL, c.x, c.y + 1.0D, c.z,
                    40, r * 0.6D, 1.5D, r * 0.6D, 0.05D);
        }
    }

    /**
     * <b>心魔引</b>（职业主动·魔修）：持续<b>拉拽 + 恐惧</b>。
     *
     * <p>与太虚黑洞的持续表现刻意分开：半径小（24 格）、不挖坑、不做百分比伤害；
     * 恐惧用 1.20.1 官方映射的 {@code CONFUSION}（= 反胃/NAUSEA）短刷新 +
     * 减速，玩家会看到画面失真、失去方向感。</p>
     */
    private static void mindDemon(ServerPlayer player, ServerLevel level, Effect e,
                                  double t, boolean last) {
        Vec3 c = e.center;
        double r = e.radius * (1.0D - 0.15D * t);      // 慢慢收拢
        // 表现：向内收束的魂火螺旋（每 4 tick 一次，密度低，不抢帧）
        if (e.elapsed % 4 == 0) {
            ParticleBatch pb = new ParticleBatch(level);
            for (int i = 0; i < 36; i++) {
                double a = Math.PI * 2.0D * i / 36.0D + e.elapsed * 0.15D;
                double rr = r * (0.5D + 0.5D * (i % 2));
                pb.add(ParticleTypes.PORTAL,
                        c.x + Math.cos(a) * rr, c.y + 0.5D + 0.25D * (i % 4),
                        c.z + Math.sin(a) * rr);
            }
            pb.flush();
            level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 0.6D, c.z,
                    4, 0.8D, 0.6D, 0.8D, 0.0D);
        }
        if (e.elapsed % 10 != 0) {
            return;
        }
        AABB box = new AABB(c.x - r, c.y - 10.0D, c.z - r,
                c.x + r, c.y + 10.0D, c.z + r);
        float dmg = (float) Math.max(3.0D, e.power * 0.04D);
        for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> isHostile(player, x))) {
            Vec3 pull = c.subtract(v.position());
            double dist = Math.max(0.8D, pull.length());
            if (dist > r) {
                continue;
            }
            Vec3 dir = pull.normalize().scale(Math.min(2.0D, 5.0D / dist));
            v.push(dir.x, 0.12D, dir.z);
            v.hurtMarked = true;
            // 恐惧：反胃（CONFUSION = 1.20.1 的 NAUSEA）+ 减速
            v.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.CONFUSION, 60, 0,
                    false, false, true));
            v.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1,
                    false, false, true));
            com.hongmeng.xiuxian.domain.DomainDamage.hit(v, player, dmg);
        }
        if (last) {
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.0D, c.z,
                    4, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * 只对<b>敌对生物</b>生效 —— 与 {@code TechniqueCaster.isEnemy} 同口径，
     * 不误伤玩家、村民、宠物与动物（持续区域如果打动物会非常扰民）。
     */
    private static boolean isHostile(ServerPlayer player, LivingEntity entity) {
        if (entity == null || entity == player || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }
        return entity instanceof net.minecraft.world.entity.monster.Enemy;
    }
}
