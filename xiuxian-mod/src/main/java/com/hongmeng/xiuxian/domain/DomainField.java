package com.hongmeng.xiuxian.domain;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.entity.MobRank;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 本命领域的<b>服务端结算</b>：八种元素的效果全部在这里落地。
 *
 * <p>数值一个都不在本类里——全在 {@link DomainEffectTable}（纯数据、离线可断言）。
 * 本类只负责"什么时候、对谁、按什么节奏"把那些数字打出去。</p>
 *
 * <h2>节奏</h2>
 *
 * <p>玩家的每 tick 里被调用一次（{@code ModEvents.onPlayerTick}），
 * 但只在 {@link DomainEffectTable#SCAN_INTERVAL_TICKS} 的整数倍 tick 上真正扫描：
 * 5 tick 一次是为了跟得上风（0.25 秒一击，全表最快）。每种元素各自的结算间隔
 * 记在目标自己的 persistentData 里（服务器游戏时间），所以多个元素叠加也不会互相打架。</p>
 *
 * <h2>为什么状态记在怪物身上</h2>
 *
 * <p>木的"5 秒后爆炸"、金的"五层标记"、雷的"每第 3 次"、土的"10 秒拖入"、
 * 冰的"5+3 秒"、风的"10 次"都是<b>按目标</b>计数的。记在目标的 persistentData 里
 * （随实体一起存读档）比在管理器里挂一张 Map 安全：怪死了、卸载了，状态跟着走，
 * 不会漏内存。</p>
 *
 * <h2>水是纯特效</h2>
 *
 * <p>涨潮<b>一个方块都不改</b>（规格 §3.3 明确要求不影响火把与作物）：
 * 水面与雨幕由客户端粒子画（{@code client/DomainClientVfx}），
 * 服务端这里只结算"窒息（原版溺水速率的魔法伤害）+ 减速 + 1%/秒"。</p>
 */
public final class DomainField {

    // ---- persistentData 键 ----
    private static final String K_FIRE = "hm_dom_fire";
    private static final String K_WOOD = "hm_dom_wood";
    private static final String K_WATER = "hm_dom_water";
    private static final String K_METAL = "hm_dom_metal";
    /** 冲击波的**视觉**细刃道数（伤害仍然只结算一次，见 sharpWave）。 */
    private static final int WAVE_VISUAL_BLADES = 3;

    private static final String K_METAL_MARKS = "hm_dom_metal_marks";
    private static final String K_EARTH = "hm_dom_earth";
    private static final String K_ICE = "hm_dom_ice";
    private static final String K_ICE_HIT = "hm_dom_ice_hit";
    private static final String K_THUNDER = "hm_dom_thunder";
    private static final String K_THUNDER_N = "hm_dom_thunder_n";
    private static final String K_WIND = "hm_dom_wind";
    private static final String K_WIND_HITS = "hm_dom_wind_hits";

    private DomainField() {
    }

    // ------------------------------------------------------------------
    // 主循环
    // ------------------------------------------------------------------

    /** 每 tick 调一次；只有到扫描点、且领域开着时才干活。 */
    public static void tick(ServerPlayer player, CultivationData data) {
        if (player == null || data == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!HongmengConfig.SERVER.domainEffectsEnabled.get()
                || !DomainManager.isDomainActive(data)) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        if (level.getGameTime() % DomainEffectTable.SCAN_INTERVAL_TICKS != 0L) {
            return;
        }
        Element[] elements = activeElements(data.getDomainData());
        if (elements.length == 0) {
            return;
        }
        // 环境特效（下雪/火雨/涨潮/金色锐气…）：**纯表现**，按档位与频率让路
        //
        // ⚠ 性能：原来是"每 4 tick × 每个激活元素"各发一批粒子 ——
        // 开三个领域时每 4 tick 就是三批（每批几十个），常驻下来很吃带宽与客户端。
        // 现在：① 频率放宽到每 8 tick（观感几乎无差）；
        //       ② 跟随法术特效档位（0 档 = 不发粒子，最省）。
        //
        // 注意：这里读的是**配置**而不是客户端类 —— 服务端引用客户端类会崩。
        if (level.getGameTime() % 8L == 0L && fxParticlesOn()) {
            for (Element element : elements) {
                ambient(player, level, element);
            }
        }
        // 独立空间（天顶穹顶 + 边界光柱 + 内壁 + 头顶符文环）：每 8 tick 一次，纯表现
        if (level.getGameTime() % EXPANSION_INTERVAL == 0L) {
            domainExpansion(player, level, elements[0]);
        }
        for (LivingEntity target : targets(player, data)) {
            for (Element element : elements) {
                apply(player, level, target, element);
            }
        }
    }

    /** 当前领域激活的元素（每座道宫一个；重复元素只算一次）。 */
    public static Element[] activeElements(DomainData domains) {
        Set<Element> set = new LinkedHashSet<>();
        for (DomainData.Domain domain : domains.domains()) {
            if (DomainEffectTable.hasEffect(domain.element())) {
                set.add(domain.element());
            }
        }
        return set.toArray(new Element[0]);
    }

    /** 领域范围（以玩家为中心的正方体）。 */
    public static AABB area(ServerPlayer player) {
        double half = halfBlocks();
        double vertical = DomainEffectTable.VERTICAL_HALF_BLOCKS;
        return new AABB(
                player.getX() - half, player.getY() - vertical, player.getZ() - half,
                player.getX() + half, player.getY() + vertical, player.getZ() + half);
    }

    /** 领域水平半边长（格）：配置的区块数 × 16 ÷ 2。 */
    public static double halfBlocks() {
        return HongmengConfig.SERVER.domainChunks.get()
                * DomainEffectTable.BLOCKS_PER_CHUNK / 2.0D;
    }

    /** 面板/日志显示的领域范围文本。 */
    public static String rangeText() {
        return DomainEffectTable.rangeText(HongmengConfig.SERVER.domainChunks.get());
    }

    /** 领域内的合法目标（含友伤开关判定与数量上限）。 */
    private static List<LivingEntity> targets(ServerPlayer player, CultivationData data) {
        ServerLevel level = (ServerLevel) player.level();
        boolean friendlyFire = data.isDomainFriendlyFire();
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, area(player),
                e -> DomainDamage.shouldAffect(player, e, friendlyFire));
        int max = Math.max(1, HongmengConfig.SERVER.domainMaxTargets.get());
        if (found.size() > max) {
            return new ArrayList<>(found.subList(0, max));
        }
        return found;
    }

    // ------------------------------------------------------------------
    // 八种元素
    // ------------------------------------------------------------------

    private static void apply(ServerPlayer player, ServerLevel level, LivingEntity target,
                              Element element) {
        switch (element) {
            case FIRE -> fire(player, level, target);
            case WOOD -> wood(player, level, target);
            case WATER -> water(player, level, target);
            case METAL -> metal(player, level, target);
            case EARTH -> earth(player, level, target);
            case ICE -> ice(player, level, target);
            case THUNDER -> thunder(player, level, target);
            case WIND -> wind(player, level, target);
            default -> {
                // NONE：无属性道宫没有领域效果
            }
        }
    }

    /** 火：火雨 · 强制燃烧 + 岩浆级秒伤 4 点 + 每秒 1% 最大生命（首领封顶 2%）。 */
    private static void fire(ServerPlayer player, ServerLevel level, LivingEntity target) {
        // 强制燃烧：每次扫描都把着火时间顶起来（雨里也烧得着）
        if (target.getRemainingFireTicks() < DomainEffectTable.Fire.FORCED_BURN_TICKS) {
            target.setRemainingFireTicks(DomainEffectTable.Fire.FORCED_BURN_TICKS);
        }
        CompoundTag pd = target.getPersistentData();
        long now = level.getGameTime();
        if (!due(pd, K_FIRE, now, DomainEffectTable.Fire.INTERVAL_TICKS)) {
            return;
        }
        MobRank rank = DomainDamage.rankOf(target);
        double percent = DomainEffectTable.sustainedPercent(
                rank, DomainEffectTable.Fire.PERCENT_PER_SECOND);
        float damage = (float) (DomainEffectTable.Fire.LAVA_DAMAGE_PER_SECOND
                + target.getMaxHealth() * percent);
        if (DomainDamage.hit(target, player, damage)) {
            fx(level, target, ParticleTypes.FLAME, 6, 0.35D, 0.02D);
            domainLux(player, level, target, Element.FIRE);
            fx(level, target, ParticleTypes.LAVA, 2, 0.2D, 0.0D);
        }
    }

    /** 木：藤缠绕 → 5 秒后爆炸 5 点 + 八根木刺（各 1%，一次齐射算一次结算）。 */
    private static void wood(ServerPlayer player, ServerLevel level, LivingEntity target) {
        CompoundTag pd = target.getPersistentData();
        long now = level.getGameTime();
        // 玩家要求：**木要有束缚、要有植物缠绕的特效**。
        // 首次命中就把目标缠住：缓慢 200 级（走不动，但能转身/攻击）、
        // 并且画一圈藤蔓（环形 COMPOSTER + 竖直藤条），一眼看得出"被缠住了"。
        if (!pd.contains(K_WOOD)) {
            pd.putLong(K_WOOD, now);
            bindWithVines(player, level, target);
            return;
        }
        long start = pd.getLong(K_WOOD);
        long elapsed = now - start;
        if (elapsed < DomainEffectTable.Wood.DELAY_TICKS) {
            // 缠绕期间藤蔓还在长
            if (elapsed % 20L == 0L) {
                fx(level, target, ParticleTypes.COMPOSTER, 4, 0.35D, 0.0D);
            }
            return;
        }
        pd.remove(K_WOOD);
        MobRank rank = DomainDamage.rankOf(target);
        boolean hit = DomainDamage.hit(target, player,
                (float) DomainEffectTable.Wood.EXPLODE_DAMAGE);
            domainLux(player, level, target, Element.WOOD);
        // 木刺齐射：8 根 × 1% = 8%，对首领按 min(8%, 2%) 结算（一次效果，不是 8 次）
        // 玩家要求：**木刺要有特效展现，而且被木刺打到的也要掉血**。
        // 原来是"把 10 根刺的总比例一次性算给目标"，看不见也说不清；现在改成
        // 真的朝 8 个方向射出可见的刺，并沿路径命中沿途的敌人各掉一次血。
        shootSpikes(player, level, target, rank);
        if (hit) {
            fx(level, target, ParticleTypes.CRIT, 10, 0.5D, 0.15D);
            level.playSound(null, target.blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK,
                    SoundSource.PLAYERS, 0.7F, 0.9F);
        }
    }

    /**
     * 水：涨潮 —— 窒息（原版溺水速率 2 点/秒）+ 减速 + 每秒 1% 最大生命。
     *
     * <p><b>不碰任何方块</b>：水面、雨幕、涨潮全是客户端粒子。
     * 也不去改目标的空气值——原版在陆地上每 tick 回 4 点空气，
     * 我们假装它"在水下"只会得到一个自欺欺人的数字，伤害还是得自己结算。</p>
     */
    private static void water(ServerPlayer player, ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                DomainEffectTable.Water.SLOWNESS_TICKS,
                DomainEffectTable.Water.SLOWNESS_AMPLIFIER, false, false, false));
        CompoundTag pd = target.getPersistentData();
        long now = level.getGameTime();
        if (due(pd, K_WATER, now, DomainEffectTable.Water.INTERVAL_TICKS)) {
            MobRank rank = DomainDamage.rankOf(target);
            double percent = DomainEffectTable.sustainedPercent(
                    rank, DomainEffectTable.Water.PERCENT_PER_SECOND);
            float damage = (float) (DomainEffectTable.Water.SUFFOCATION_DAMAGE
                    + target.getMaxHealth() * percent);
            if (DomainDamage.hit(target, player, damage)) {
                fx(level, target, ParticleTypes.SPLASH, 6, 0.35D, 0.0D);
            domainLux(player, level, target, Element.WATER);
                fx(level, target, ParticleTypes.BUBBLE, 4, 0.3D, 0.02D);
            }
        } else if (now % 10L == 0L) {
            fx(level, target, ParticleTypes.BUBBLE, 2, 0.3D, 0.01D);
        }
    }



    // ------------------------------------------------------------------
    // 领域 = 独立空间（玩家要求：开领域相当于开了一个单独的空间）
    // ------------------------------------------------------------------

    /** 边界光柱的间距（格）。R2：3 → 6（界碑减半，边界仍看得出来）。 */
    private static final int EXPANSION_POST_STEP = 6;

    /** 空间外壁的绘制周期（tick）。R2：8 → 12。 */
    private static final int EXPANSION_INTERVAL = 12;

    /**
     * 把领域画成**一个独立空间**：天顶穹顶 + 边界光柱 + 内壁流动 + 头顶符文环。
     *
     * <p>玩家要求「领域的表现手法更加夸张，这时候开启领域相当于开了一个单独的空间」。
     * 全部是**原版粒子**：不生成方块、不改地形、不碰水与火。</p>
     */
    private static void domainExpansion(ServerPlayer player, ServerLevel level,
                                        Element element) {
        try {
            double cx = player.getX();
            double cy = player.getY();
            double cz = player.getZ();
            double r = halfBlocks();
            double h = DomainEffectTable.VERTICAL_HALF_BLOCKS * 0.35D;   // 空间高度
            var main = com.hongmeng.xiuxian.technique.SpellVfx.particleOf(element);
            var accent = com.hongmeng.xiuxian.technique.SpellVfx.accentOf(element);
            var aura = com.hongmeng.xiuxian.technique.SpellVfx.auraOf(element);

            // ---- 1) 天顶穹顶：在 y = cy + h 的圆盘上铺一层光点（内密外疏）----
            // R2：70 → 24（穹顶轮廓保留，内侧密度大幅下降）
            int dome = 24;
            for (int i = 0; i < dome; i++) {
                double a = Math.PI * 2.0D * i / dome;
                double rr = r * (0.35D + 0.65D * ((i % 5) / 4.0D));
                emit(level, aura,
                        cx + Math.cos(a) * rr, cy + h, cz + Math.sin(a) * rr,
                        1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
            // ---- 2) 边界光柱：每隔 EXPANSION_POST_STEP 格一根，像界碑 ----
            for (double d = -r; d <= r; d += EXPANSION_POST_STEP) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    double px = cx + d;
                    double pz = cz + sign * r;
                    post(level, main, accent, px, cy, pz, h);
                    double px2 = cx + sign * r;
                    double pz2 = cz + d;
                    post(level, main, accent, px2, cy, pz2, h);
                }
            }
            // ---- 3) 内壁流动：四个竖直面上各铺一串流动粒子 ----
            // R2：26 → 9（每面只剩 9 个点，仍是流动的一串）
            int face = 9;
            for (int i = 0; i < face; i++) {
                double t = (double) i / (face - 1);
                double along = -r + t * 2.0D * r;
                double yy = cy + 0.5D + ((level.getGameTime() % 40L) / 40.0D) * h;
                emit(level, accent, cx + along, yy, cz + r, 1, 0.0D, 0.03D, 0.0D, 0.0D);
                emit(level, accent, cx + along, yy, cz - r, 1, 0.0D, 0.03D, 0.0D, 0.0D);
                emit(level, accent, cx + r, yy, cz + along, 1, 0.0D, 0.03D, 0.0D, 0.0D);
                emit(level, accent, cx - r, yy, cz + along, 1, 0.0D, 0.03D, 0.0D, 0.0D);
            }
            // ---- 4) 头顶符文环：两层同心环 + 反向螺旋，缓缓旋转 ----
            // R2：3 层 → 2 层；每层点数 n = 28 + ring*8 → 16 + ring*5
            long t = level.getGameTime();
            for (int ring = 0; ring < 2; ring++) {
                double rr = 1.6D + ring * 0.9D;
                double yy = cy + 3.2D + ring * 0.9D;
                int n = 16 + ring * 5;
                double spin = t * 0.03D * (ring % 2 == 0 ? 1.0D : -1.0D);
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2.0D * i / n + spin;
                    emit(level, main,
                            cx + Math.cos(a) * rr, yy, cz + Math.sin(a) * rr,
                            1, 0.0D, 0.01D, 0.0D, 0.0D);
                }
            }
        } catch (Throwable ignored) {
            // 表现层失败绝不能影响领域结算
        }
    }

    /** 一根边界光柱（从地面到空间顶）。 */
    private static void post(ServerLevel level,
                             net.minecraft.core.particles.ParticleOptions main,
                             net.minecraft.core.particles.ParticleOptions accent,
                             double x, double y, double z, double h) {
        // R2：高度采样减半（max(4, h/0.8) → max(3, h/1.6)），光柱仍是连到顶的一根。
        int steps = (int) Math.max(3.0D, h / 1.6D);
        for (int k = 0; k <= steps; k++) {
            double yy = y + k * (h / steps);
            emit(level, k % 3 == 0 ? main : accent, x, yy, z,
                    1, 0.02D, 0.0D, 0.02D, 0.0D);
        }
    }

    /**
     * 领域的**环境特效**（每 8 tick 一次，纯表现，不参与伤害）。
     *
     * <p>玩家要求「冰领域要下雪地面附上雪，火领域下火雨熔岩裂地，水是下雨涨潮，
     * 这种特效其他的领域也要有」。所以每个元素都给一套**自己能认出来的天候**。</p>
     *
     * <p>R2（玩家 2026-11-07「领域特效也是」）：各元素粒子量整体再降约一半，
     * 只保留每种天候"一眼认得出"的最小规模。</p>
     *
     * <p><b>不动任何方块</b>：全部是粒子。领域是"气势"，不该改地形。</p>
     */
    /**
     * 是否发"纯表现"的粒子。
     *
     * <p>读配置（0 档 = 关），读不到就当作开启。**不引用任何客户端类**，
     * 否则专用服务器会在加载时抛 {@code RuntimeDistCleaner}。</p>
     */
    /**
     * <b>带预算的粒子发送</b>：预算不足就这一帧不发。
     *
     * <p>项目里已经有 {@code ParticleBudget}，但领域这里原来是 16 处直接
     * {@code level.sendParticles(...)}，**完全绕过预算** —— 玩家开几个元素领域常驻时，
     * 这是最大的、不受控的粒子来源。走预算之后，服务端与客户端的负担立刻可控。</p>
     *
     * @return 实际发出的数量（0 = 本帧被预算砍掉）
     */
    private static int emit(ServerLevel level, net.minecraft.core.particles.ParticleOptions type,
                            double x, double y, double z, int count,
                            double dx, double dy, double dz, double speed) {
        if (level == null || type == null || count <= 0) {
            return 0;
        }
        int granted = com.hongmeng.xiuxian.combat.ParticleBudget.spend(level, count);
        if (granted <= 0) {
            return 0;
        }
        level.sendParticles(type, x, y, z, granted, dx, dy, dz, speed);
        return granted;
    }

    private static boolean fxParticlesOn() {
        try {
            return com.hongmeng.xiuxian.compat.HongmengConfig.CLIENT
                    .spellFxQuality.get() > 0;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void ambient(ServerPlayer player, ServerLevel level, Element element) {
        try {
            double cx = player.getX();
            double cy = player.getY();
            double cz = player.getZ();
            double r = halfBlocks();
            switch (element) {
                case ICE -> {
                    // 下雪：从上方 5 格持续飘落（R2：26 → 10）
                    for (int i = 0; i < 10; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                                cx + Math.cos(a) * d, cy + 4.0D + level.random.nextDouble() * 2.0D,
                                cz + Math.sin(a) * d, 1, 0.0D, -0.08D, 0.0D, 0.02D);
                    }
                    // 地面附雪：贴地一圈白点（R2：20 → 8）
                    for (int i = 0; i < 8; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL,
                                cx + Math.cos(a) * d, cy + 0.08D, cz + Math.sin(a) * d,
                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
                case FIRE -> {
                    // 下火雨：高处落下的岩浆滴（R2：18 → 7）
                    for (int i = 0; i < 7; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r * 0.9D;
                        emit(level, net.minecraft.core.particles.ParticleTypes.FALLING_LAVA,
                                cx + Math.cos(a) * d, cy + 6.0D + level.random.nextDouble() * 3.0D,
                                cz + Math.sin(a) * d, 1, 0.0D, -0.25D, 0.0D, 0.55D);
                    }
                    // 地面熔岩裂纹：贴地铺开（R2：24 → 9）
                    for (int i = 0; i < 9; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.LAVA,
                                cx + Math.cos(a) * d, cy + 0.06D, cz + Math.sin(a) * d,
                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
                case WATER -> {
                    // 下雨：头顶雨幕（R2：30 → 12）
                    for (int i = 0; i < 12; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.FALLING_WATER,
                                cx + Math.cos(a) * d, cy + 5.0D + level.random.nextDouble() * 2.0D,
                                cz + Math.sin(a) * d, 1, 0.0D, -0.15D, 0.0D, 0.35D);
                    }
                    // 涨潮：由内向外扩散的几圈（R2：3 圈 → 2 圈，每圈 14 → 6 点）
                    long t = level.getGameTime() / 4L;
                    for (int ring = 0; ring < 2; ring++) {
                        double rr = ((t + ring * 3L) % 9L) / 9.0D * r;
                        com.hongmeng.xiuxian.technique.SpellVfx.ring(
                                level, new net.minecraft.world.phys.Vec3(cx, cy + 0.1D, cz),
                                net.minecraft.core.particles.ParticleTypes.SPLASH, rr, 6, 0.04D, 0.0D);
                    }
                }
                case THUNDER -> {
                    // 天顶乌云（R2：14 → 6）
                    for (int i = 0; i < 6; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                cx + Math.cos(a) * d, cy + 7.0D, cz + Math.sin(a) * d,
                                1, 0.4D, 0.05D, 0.4D, 0.01D);
                    }
                    // 随机落雷点：紫色竖线（R2：3 根 → 1 根）
                    for (int i = 0; i < 1; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r * 0.8D;
                        double x = cx + Math.cos(a) * d;
                        double z = cz + Math.sin(a) * d;
                        // R2：每根雷柱的采样点 10 → 5
                        for (int k = 0; k < 5; k++) {
                            emit(level, 
                                    net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                    x, cy + k * 0.6D, z, 1, 0.05D, 0.0D, 0.05D, 0.0D);
                        }
                    }
                }
                case WIND -> {
                    // 青色气流：双臂螺旋（CLOUD + WHITE_ASH，青白系）
                    // R2：n 26 → 10（两个粒子仍在，只是采样点少了）
                    int n = 10;
                    for (int i = 0; i < n; i++) {
                        double tt = (double) i / (double) (n - 1);
                        double a = tt * Math.PI * 4.0D + level.getGameTime() * 0.08D;
                        double rr = r * 0.85D * (1.0D - tt * 0.3D);
                        emit(level, net.minecraft.core.particles.ParticleTypes.CLOUD,
                                cx + Math.cos(a) * rr, cy + 0.2D + tt * 3.0D, cz + Math.sin(a) * rr,
                                1, 0.0D, 0.02D, 0.0D, 0.01D);
                        emit(level, net.minecraft.core.particles.ParticleTypes.WHITE_ASH,
                                cx + Math.cos(a + 1.6D) * rr, cy + 0.2D + tt * 3.0D,
                                cz + Math.sin(a + 1.6D) * rr,
                                1, 0.0D, 0.01D, 0.0D, 0.01D);
                    }
                }
                case METAL -> {
                    // 金色锐气：自上而下直落（WAX_ON 是原版唯一的蜂蜜金色粒子）
                    // R2：22 → 9
                    for (int i = 0; i < 9; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.WAX_ON,
                                cx + Math.cos(a) * d, cy + 5.0D + level.random.nextDouble() * 2.0D,
                                cz + Math.sin(a) * d, 1, 0.0D, -0.3D, 0.0D, 0.4D);
                    }
                    // 地面金环（R2：24 → 9 点）
                    com.hongmeng.xiuxian.technique.SpellVfx.ring(
                            level, new net.minecraft.world.phys.Vec3(cx, cy + 0.1D, cz),
                            net.minecraft.core.particles.ParticleTypes.SCRAPE, r * 0.9D, 9, 0.04D, 0.0D);
                }
                case WOOD -> {
                    // 地上冒新芽 + 飘落叶片（R2：20 → 8）
                    for (int i = 0; i < 8; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                                cx + Math.cos(a) * d, cy + 0.1D, cz + Math.sin(a) * d,
                                1, 0.0D, 0.02D, 0.0D, 0.0D);
                    }
                    // 飘落叶片（R2：14 → 5）
                    for (int i = 0; i < 5; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                                cx + Math.cos(a) * d, cy + 3.5D, cz + Math.sin(a) * d,
                                1, 0.0D, -0.06D, 0.0D, 0.01D);
                    }
                }
                case EARTH -> {
                    // 扬尘 + 碎石（R2：22 → 9）
                    for (int i = 0; i < 9; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.SMOKE,
                                cx + Math.cos(a) * d, cy + 0.15D, cz + Math.sin(a) * d,
                                1, 0.15D, 0.02D, 0.15D, 0.01D);
                    }
                    // 碎石（R2：10 → 4）
                    for (int i = 0; i < 4; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double d = Math.sqrt(level.random.nextDouble()) * r;
                        emit(level, net.minecraft.core.particles.ParticleTypes.CRIT,
                                cx + Math.cos(a) * d, cy + 0.6D, cz + Math.sin(a) * d,
                                1, 0.1D, 0.05D, 0.1D, 0.02D);
                    }
                }
                default -> {
                    // NONE 等：什么都不做
                }
            }
        } catch (Throwable t) {
            // 特效失败绝不能影响领域结算
        }
    }


    /**
     * 木：**藤蔓缠绕 + 束缚**。
     *
     * <p>玩家要求「木领域没有束缚，没有植物缠绕的特效」。
     * 束缚用缓慢 {@code WOOD_BIND_SLOWNESS_AMPLIFIER}（走不动，但能转身与攻击，
     * 不是硬直定身）；缠绕用环形 COMPOSTER + 一圈竖直藤条表现。</p>
     */
    private static void bindWithVines(ServerPlayer player, ServerLevel level,
                                      LivingEntity target) {
        try {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    DomainEffectTable.Wood.DELAY_TICKS + 10,
                    DomainEffectTable.WOOD_BIND_SLOWNESS_AMPLIFIER,
                    false, false, false));
            var at = target.position();
            // 环形藤蔓（贴地）
            com.hongmeng.xiuxian.technique.SpellVfx.ring(level, at,
                    net.minecraft.core.particles.ParticleTypes.COMPOSTER, 1.0D, 22, 0.05D, 0.0D);
            // 六根竖直"藤条"
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2.0D * i / 6.0D;
                double x = at.x + Math.cos(a) * 0.85D;
                double z = at.z + Math.sin(a) * 0.85D;
                for (int k = 0; k < 7; k++) {
                    emit(level, net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                            x, at.y + 0.15D + k * 0.28D, z, 1, 0.03D, 0.0D, 0.03D, 0.0D);
                }
            }
            level.playSound(null, target.blockPosition(),
                    net.minecraft.sounds.SoundEvents.VINE_PLACE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.9F);
        } catch (Throwable ignored) {
            // 特效/束缚失败不影响伤害结算
        }
    }

    /**
     * 木：爆炸后向 8 个方向射出**可见的木刺**，沿途敌人各吃一次伤害。
     *
     * <p>玩家要求「木的爆炸后发射的木刺也要有特效展现，然后被木刺打到的也要掉血」。
     * 每个方向步进 0.5 格画一串 CRIT（刺身）与绿色尖点（刺头），
     * 每步查一次该位置附近的敌人：命中就掉血并停止推进（刺扎进去就没了）。</p>
     */
    private static void shootSpikes(ServerPlayer player, ServerLevel level,
                                    LivingEntity target, MobRank rank) {
        final int dirs = 8;
        final double step = 0.5D;
        final double range = DomainEffectTable.WOOD_SPIKE_RANGE;
        double basePercent = DomainEffectTable.oneShotPercent(
                rank, DomainEffectTable.Wood.SPIKE_PERCENT);
        for (int d = 0; d < dirs; d++) {
            double a = Math.PI * 2.0D * d / dirs;
            double dx = Math.cos(a);
            double dz = Math.sin(a);
            for (double r = 0.5D; r <= range; r += step) {
                double x = target.getX() + dx * r;
                double y = target.getY() + 0.6D;
                double z = target.getZ() + dz * r;
                emit(level, net.minecraft.core.particles.ParticleTypes.CRIT,
                        x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                emit(level, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                var box = new net.minecraft.world.phys.AABB(
                        x - 0.6D, y - 0.8D, z - 0.6D, x + 0.6D, y + 0.8D, z + 0.6D);
                boolean stop = false;
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box)) {
                    if (victim == player || victim == target || !victim.isAlive()) {
                        continue;
                    }
                    if (!DomainDamage.shouldAffect(player, victim, false)) {
                        continue;
                    }
                    MobRank vr = DomainDamage.rankOf(victim);
                    DomainDamage.hitPercent(victim, player,
                            DomainEffectTable.oneShotPercent(
                                    vr, Math.max(DomainEffectTable.WOOD_SPIKE_HIT_PERCENT,
                                            basePercent)));
                    fx(level, victim, ParticleTypes.CRIT, 6, 0.3D, 0.05D);
                    stop = true;
                }
                if (stop) {
                    break;
                }
            }
        }
        level.playSound(null, target.blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.1F);
    }

    /**
     * 领域特效加浓：在目标脚下铺三层同心阵纹 + 一圈上浮灵气。
     *
     * <p>玩家要求「法术领域特效要足要多、华丽豪华绚丽」。原来每个元素只有
     * 零散几处 {@code fx(...)}，单个怪身上几乎看不出"被领域罩住"；
     * 这里统一补一层**法阵**（多重同心环 + 螺旋 + 漩涡），
     * 用域主所属元素的颜色，一开领域就能看出范围与归属。</p>
     *
     * <p>刻意做成"每个目标一次"而不是每 tick：领域本来就持续很久，
     * 每 tick 画阵纹会把粒子预算吃光。</p>
     */
    private static void domainLux(ServerPlayer player, ServerLevel level,
                                  LivingEntity target, Element element) {
        try {
            net.minecraft.world.phys.Vec3 at = target.position();
            net.minecraft.core.particles.ParticleOptions main =
                    com.hongmeng.xiuxian.technique.SpellVfx.particleOf(element);
            net.minecraft.core.particles.ParticleOptions accent =
                    com.hongmeng.xiuxian.technique.SpellVfx.accentOf(element);
            // **特效强度按元素分化**（玩家要求：雷提高、火与冰降低）：
            //   雷 —— 阵纹 5 层 / 螺旋 44 / 漩涡 3 层（最张扬）
            //   火、冰 —— 阵纹 2 层 / 螺旋 12 / 漩涡 1 层（收敛，别糊屏）
            //   其它 —— 3 / 26 / 2（原值）
            int rings;
            int spiralCount;
            int swirlLayers;
            int swirlCount;
            switch (element) {
                case THUNDER -> {
                    rings = 5;
                    spiralCount = 44;
                    swirlLayers = 3;
                    swirlCount = 48;
                }
                case FIRE, ICE -> {
                    rings = 2;
                    spiralCount = 12;
                    swirlLayers = 1;
                    swirlCount = 16;
                }
                default -> {
                    rings = 3;
                    spiralCount = 26;
                    swirlLayers = 2;
                    swirlCount = 34;
                }
            }
            com.hongmeng.xiuxian.technique.SpellVfx.grandRings(
                    level, at, main, rings, 2.6D, rings * 10);
            com.hongmeng.xiuxian.technique.SpellVfx.spiral(
                    level, at, accent, spiralCount, 1.2D, 2.8D);
            com.hongmeng.xiuxian.technique.SpellVfx.swirl(
                    level, at.add(0.0D, 0.9D, 0.0D), main, swirlLayers, swirlCount, 1.0D);
            // 雷额外加竖直电弧，把"雷狱"的观感顶起来
            if (element == Element.THUNDER) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI * 2.0D * i / 6.0D;
                    net.minecraft.world.phys.Vec3 foot = at.add(
                            Math.cos(a) * 1.1D, 0.0D, Math.sin(a) * 1.1D);
                    com.hongmeng.xiuxian.technique.SpellVfx.trail(level, foot,
                            foot.add(0.0D, 5.0D, 0.0D),
                            net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                            20, 0.08D, 0.02D);
                }
            }
        } catch (Throwable ignored) {
            // 特效失败绝不能影响领域结算
        }
    }

    /** 金：锐气每 0.5 秒 3 点 + 叠一层标记；五层后以玩家为中心放 30% 冲击波（首领封顶 2%）。 */
    private static void metal(ServerPlayer player, ServerLevel level, LivingEntity target) {
        CompoundTag pd = target.getPersistentData();
        long now = level.getGameTime();
        if (!due(pd, K_METAL, now, DomainEffectTable.Metal.INTERVAL_TICKS)) {
            return;
        }
        if (!DomainDamage.hit(target, player, (float) DomainEffectTable.Metal.HIT_DAMAGE)) {
            return;
        }
        fx(level, target, ParticleTypes.ENCHANTED_HIT, 4, 0.3D, 0.05D);
        int marks = pd.getInt(K_METAL_MARKS) + 1;
        if (marks < DomainEffectTable.Metal.MARKS_FOR_WAVE) {
            pd.putInt(K_METAL_MARKS, marks);
            return;
        }
        // 满五层：标记清零，以**角色为半径**放锐利冲击波
        pd.putInt(K_METAL_MARKS, 0);
        sharpWave(player, level);
    }

    /** 锐利冲击波：以玩家为中心，领域范围内的敌人各吃一次 30%（首领 2%）。 */
    private static void sharpWave(ServerPlayer player, ServerLevel level) {
        CultivationData data = com.hongmeng.xiuxian.core.CultivationCapability.get(player);
        boolean friendlyFire = data.isDomainFriendlyFire();
        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class, area(player),
                e -> DomainDamage.shouldAffect(player, e, friendlyFire));
        // **一次冲击波 = 每个敌人只吃一次百分比**（玩家要求："只有一次可以显示，
        // 多道攻击但只生效一次百分比伤害"）。
        //
        // 所以这里是"每个 victim 调一次 hitPercent"，绝不能在循环里按"几道刃"重复调用；
        // 视觉上多画几道 SWEEP_ATTACK 是**故意**的 —— 看起来是万千细刃，结算只有一次。
        for (LivingEntity victim : caught) {
            MobRank rank = DomainDamage.rankOf(victim);
            DomainDamage.hitPercent(victim, player, DomainEffectTable.oneShotPercent(
                    rank, DomainEffectTable.Metal.WAVE_PERCENT));
            for (int blade = 0; blade < WAVE_VISUAL_BLADES; blade++) {
                fx(level, victim, ParticleTypes.SWEEP_ATTACK, 1, 0.6D, 0.0D);
            }
        }
        fx(level, player, ParticleTypes.SWEEP_ATTACK, 12, 2.0D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    /**
     * 土：束缚（不能移动，但仍可远程攻击、仍能被近战打到）→ 10 秒拖入土中 → 秒杀头目以下。
     *
     * <p>刻意<b>不</b>动 AI、不动 goal、不动无敌：束缚只压移速（缓慢 250 级 ≈ 移速 0）
     * 并清掉水平位移，所以怪物照样放法术、玩家照样能近战摸到它——这三条是规格
     * §3.5 的硬要求。</p>
     */
    private static void earth(ServerPlayer player, ServerLevel level, LivingEntity target) {
        CompoundTag pd = target.getPersistentData();
        long now = level.getGameTime();
        if (!pd.contains(K_EARTH)) {
            pd.putLong(K_EARTH, now);
            fx(level, target, ParticleTypes.SMOKE, 8, 0.35D, 0.02D);
        }
        long elapsed = now - pd.getLong(K_EARTH);
        long left = DomainEffectTable.Earth.BIND_TICKS - elapsed;

        // 束缚：剩余时间就是效果时长，领域一停、效果自己到期，不需要额外的清理表
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                (int) Math.max(1L, left) + DomainEffectTable.Earth.SLOWNESS_EXTRA_TICKS,
                DomainEffectTable.Earth.SLOWNESS_AMPLIFIER, false, false, false));

        // 缓慢拖入土中：每秒往下压一点；同时把"想往上跳"的速度掐掉（跳走就不叫束缚了）
        double dy = target.getDeltaMovement().y;
        if (now % 20L == 0L) {
            target.setDeltaMovement(0.0D,
                    Math.min(dy, 0.0D) - DomainEffectTable.Earth.SINK_PER_SECOND, 0.0D);
            target.hurtMarked = true;
            fx(level, target, ParticleTypes.CAMPFIRE_COSY_SMOKE, 3, 0.3D, 0.0D);
        } else {
            target.setDeltaMovement(0.0D, Math.min(dy, 0.0D), 0.0D);
        }

        if (elapsed < DomainEffectTable.Earth.BIND_TICKS) {
            return;
        }
        pd.remove(K_EARTH);
        MobRank rank = DomainDamage.rankOf(target);
        if (DomainEffectTable.canExecute(rank)) {
            // 秒杀：当前生命 × 10。用倍数而不是 setHealth(0)，
            // 这样照样走死亡结算（掉落/修为/道宫能量），也不会显示一个荒唐的伤害数字。
            DomainDamage.hit(target, player,
                    target.getHealth() * DomainEffectTable.EXECUTE_MULTIPLE);
            domainLux(player, level, target, Element.EARTH);
            fx(level, target, ParticleTypes.LARGE_SMOKE, 12, 0.5D, 0.03D);
            level.playSound(null, target.blockPosition(), SoundEvents.GRAVEL_BREAK,
                    SoundSource.PLAYERS, 0.9F, 0.6F);
        } else {
            // 头目 / 首领：规格 §2.2 的 2%
            DomainDamage.hitPercent(target, player,
                    DomainEffectTable.executionFallbackPercent(rank));
            fx(level, target, ParticleTypes.LARGE_SMOKE, 6, 0.4D, 0.02D);
        }
    }

    /** 冰：冰冻 5 秒 → 再 3 秒爆炸 10%（首领 2%）；期间持续减速 + 每次 1 点掉血。 */
    private static void ice(ServerPlayer player, ServerLevel level, LivingEntity target) {
        CompoundTag pd = target.getPersistentData();
        long now = level.getGameTime();
        if (!pd.contains(K_ICE)) {
            pd.putLong(K_ICE, now);
            fx(level, target, ParticleTypes.SNOWFLAKE, 10, 0.4D, 0.0D);
        }
        long elapsed = now - pd.getLong(K_ICE);
        int total = DomainEffectTable.Ice.FREEZE_TICKS + DomainEffectTable.Ice.FUSE_TICKS;

        if (elapsed < total) {
            // 冰冻期 + 引信期都在减速、都在结霜（原版结霜渲染读 ticksFrozen）
            target.setTicksFrozen(DomainEffectTable.Ice.FROZEN_TICKS);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    DomainEffectTable.Ice.SLOWNESS_TICKS,
                    DomainEffectTable.Ice.SLOWNESS_AMPLIFIER, false, false, false));
            fx(level, target, ParticleTypes.ITEM_SNOWBALL, 2, 0.3D, 0.0D);
        }

        // 持续掉血：一次 1 点
        if (due(pd, K_ICE_HIT, now, DomainEffectTable.Ice.TICK_INTERVAL_TICKS)
                && elapsed < total) {
            DomainDamage.hit(target, player, (float) DomainEffectTable.Ice.TICK_DAMAGE);
            domainLux(player, level, target, Element.ICE);
        }

        if (elapsed < total) {
            return;
        }
        pd.remove(K_ICE);
        MobRank rank = DomainDamage.rankOf(target);
        DomainDamage.hitPercent(target, player,
                DomainEffectTable.oneShotPercent(rank, DomainEffectTable.Ice.EXPLODE_PERCENT));
        fx(level, target, ParticleTypes.SNOWFLAKE, 16, 0.6D, 0.1D);
        fx(level, target, ParticleTypes.EXPLOSION, 1, 0.0D, 0.0D);
        level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    /** 雷：每 3 秒 5 点；每第 3 次 → 三连（15 点 + 10%，首领封顶 2%）。 */
    private static void thunder(ServerPlayer player, ServerLevel level, LivingEntity target) {
        CompoundTag pd = target.getPersistentData();
        long now = level.getGameTime();
        if (!due(pd, K_THUNDER, now, DomainEffectTable.Thunder.INTERVAL_TICKS)) {
            return;
        }
        int count = pd.getInt(K_THUNDER_N) + 1;
        boolean triple = count % DomainEffectTable.Thunder.EVERY_N == 0;
        pd.putInt(K_THUNDER_N, triple ? 0 : count);

        if (!triple) {
            if (DomainDamage.hit(target, player,
                    (float) DomainEffectTable.Thunder.HIT_DAMAGE)) {
                bolt(level, target, 1);
            domainLux(player, level, target, Element.THUNDER);
            }
            return;
        }
        MobRank rank = DomainDamage.rankOf(target);
        float damage = (float) (DomainEffectTable.Thunder.TRIPLE_DAMAGE
                + target.getMaxHealth() * DomainEffectTable.oneShotPercent(
                rank, DomainEffectTable.Thunder.TRIPLE_PERCENT));
        if (DomainDamage.hit(target, player, damage)) {
            bolt(level, target, DomainEffectTable.Thunder.TRIPLE_BOLTS);
        }
    }

    /** 风：高频斩击（0.25 秒一击，全表最快）每击 2 点；命中 10 次后 10%（首领 2%）。 */
    private static void wind(ServerPlayer player, ServerLevel level, LivingEntity target) {
        CompoundTag pd = target.getPersistentData();
        long now = level.getGameTime();
        if (!due(pd, K_WIND, now, DomainEffectTable.Wind.INTERVAL_TICKS)) {
            return;
        }
        if (!DomainDamage.hit(target, player, (float) DomainEffectTable.Wind.HIT_DAMAGE)) {
            return;
        }
        fx(level, target, ParticleTypes.SWEEP_ATTACK, 1, 0.2D, 0.0D);
        int hits = pd.getInt(K_WIND_HITS) + 1;
        if (hits < DomainEffectTable.Wind.HITS_FOR_BURST) {
            pd.putInt(K_WIND_HITS, hits);
            return;
        }
        pd.putInt(K_WIND_HITS, 0);
        MobRank rank = DomainDamage.rankOf(target);
        DomainDamage.hitPercent(target, player,
                DomainEffectTable.oneShotPercent(rank, DomainEffectTable.Wind.BURST_PERCENT));
        fx(level, target, ParticleTypes.CLOUD, 8, 0.5D, 0.05D);
        fx(level, target, ParticleTypes.CRIT, 6, 0.4D, 0.1D);
    }

    // ------------------------------------------------------------------
    // 领域关闭时的收尾
    // ------------------------------------------------------------------

    /**
     * 领域被关掉（或玩家下线）时调用：把范围内的束缚类效果收干净。
     *
     * <p>束缚用的是原版缓慢效果（自带到期），本来就会自己消失；
     * 这里主动清一次是为了"关掉领域立刻就能动"，而不是等十几秒。
     * 木/冰/土/雷/风/金的计时标记也一起清掉——不然下次开领域时，
     * 这些怪会拿着上次的计时"续上"，一次结算就爆。</p>
     */
    public static void releaseAll(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double half = halfBlocks() + 8.0D;
        double vertical = DomainEffectTable.VERTICAL_HALF_BLOCKS + 8.0D;
        AABB box = new AABB(player.getX() - half, player.getY() - vertical,
                player.getZ() - half, player.getX() + half, player.getY() + vertical,
                player.getZ() + half);
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : near) {
            CompoundTag pd = entity.getPersistentData();
            pd.remove(K_FIRE);
            pd.remove(K_WOOD);
            pd.remove(K_WATER);
            pd.remove(K_METAL);
            pd.remove(K_METAL_MARKS);
            pd.remove(K_EARTH);
            pd.remove(K_ICE);
            pd.remove(K_ICE_HIT);
            pd.remove(K_THUNDER);
            pd.remove(K_THUNDER_N);
            pd.remove(K_WIND);
            pd.remove(K_WIND_HITS);
            entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    // ------------------------------------------------------------------
    // 小工具
    // ------------------------------------------------------------------

    /** 到点了没：记在目标身上的上次结算时间 + 间隔。 */
    private static boolean due(CompoundTag pd, String key, long now, int interval) {
        long last = pd.getLong(key);
        if (now - last < interval) {
            return false;
        }
        pd.putLong(key, now);
        return true;
    }

    private static void fx(ServerLevel level, net.minecraft.world.entity.Entity at,
                           ParticleOptions type, int count, double spread, double speed) {
        emit(level, type, at.getX(), at.getY() + at.getBbHeight() * 0.5D, at.getZ(),
                count, spread, spread * 0.6D, spread, speed);
    }

    /**
     * 雷霆：真正的雷柱劈下来 + 一声雷。
     *
     * <p>玩家反馈「雷的领域没有真实的落雷劈下来」——原来这里只有
     * {@code ELECTRIC_SPARK} + {@code FLASH} 粒子，看起来像"火花"不像"雷"。
     * 现在召唤**真的 {@link net.minecraft.world.entity.LightningBolt} 实体**，
     * 客户端会画那道贯穿天地的雷柱 + 天空闪白，这才是"落雷"。</p>
     *
     * <p><b>为什么必须 {@code setVisualOnly(true)}</b>：真实闪电自带
     * 点火 + 伤害（而且是绕过护甲的），本项目在雷劫上已经因为这个吃过一次事故
     * （"雷劫必死"）。领域伤害**全部**由 {@link DomainDamage#hit} 按数值表结算，
     * 闪电实体只负责好看——两者绝不能重叠。</p>
     */
    private static void bolt(ServerLevel level, LivingEntity target, int bolts) {
        for (int i = 0; i < bolts; i++) {
            double ox = (i - (bolts - 1) / 2.0D) * 0.6D;
            emit(level, ParticleTypes.ELECTRIC_SPARK,
                    target.getX() + ox, target.getY() + 2.2D, target.getZ(),
                    12, 0.15D, 0.8D, 0.15D, 0.25D);
            // 视觉雷柱降频：结算已经到 10 次/秒，每次都召唤实体闪电会一直闪屏。
            // 每 2 次结算画一根，观感仍然"雷一直在劈"，但不会闪到看不清。
            if (level.getGameTime() % 2L == 0L) {
                spawnVisualBolt(level, target.getX() + ox, target.getY(), target.getZ());
            }
        }
        emit(level, ParticleTypes.FLASH, target.getX(), target.getY() + 1.0D,
                target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        emit(level, ParticleTypes.END_ROD, target.getX(), target.getY() + 0.2D,
                target.getZ(), 4, 0.3D, 0.1D, 0.3D, 0.02D);
        level.playSound(null, target.blockPosition(),
                bolts > 1 ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.TRIDENT_THUNDER,
                SoundSource.PLAYERS, bolts > 1 ? 0.5F : 0.35F, 1.4F);
    }

    /**
     * 只为一帧观感存在的闪电实体：不点火、不伤害。
     *
     * <p>整段包在 try/catch 里：万一某个版本上 {@code LIGHTNING_BOLT} 的创建路径变了，
     * 领域也绝不能因此中断结算（渡劫那边用的就是同一套写法，见
     * {@code core/Breakthrough.spawnVisualBolt}）。</p>
     */
    private static void spawnVisualBolt(ServerLevel level, double x, double y, double z) {
        try {
            net.minecraft.world.entity.LightningBolt bolt =
                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) {
                return;
            }
            bolt.moveTo(x, y, z);
            // 关键：只做视觉。真闪电带点火与无视护甲的伤害，绝不能让它参与结算。
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 领域落雷的视觉闪电创建失败：{}", t.toString());
        }
    }
}
