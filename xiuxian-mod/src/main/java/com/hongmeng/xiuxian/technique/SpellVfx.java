package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * <b>法术特效</b>：把"撒一圈粒子"升级成分层、有声、按元素走的特效系统。
 *
 * <p>玩家原话："我需要特效多一点华丽一点，领域的也好法术的也好都要"。</p>
 *
 * <h2>设计：三层</h2>
 *
 * <p>一次释放不是"一种粒子撒 N 个"，而是三层叠加——这是"华丽"和"廉价"
 * 的分界线（单层撒再多也只是更粗的一条线）：</p>
 *
 * <ol>
 *   <li><b>核心 core</b>：最亮、最集中，贴在中轴/中点上，负责"看得见"；</li>
 *   <li><b>轨迹 trail</b>：沿路径拖一条，负责"有方向感"；</li>
 *   <li><b>余韵 aura</b>：外圈扩散或上浮，负责"有体积感"。</li>
 * </ol>
 *
 * <h2>粒子预算（重要）</h2>
 *
 * <p>设计得再好看，一发 2000 个粒子也会把服务器拖死。所以这里有三道闸：</p>
 *
 * <ol>
 *   <li>{@link HongmengConfig} 的 {@code combat.particleDensity}：整合包作者的总开关，
 *       1.0 = 设计值，0.0 = 全关（伤害与音效不受影响）；</li>
 *   <li><b>全局令牌桶</b>：由 {@code combat/ParticleBudget} 统一管理的按维度预算，
 *       多人同时放法术时按比例摊薄，而不是谁后放谁独占；</li>
 *   <li><b>单次上限</b> {@link #MAX_PER_EFFECT}：一发最多这么多，
 *       防止某个特效写错数量直接打穿预算。</li>
 * </ol>
 *
 * <p>预算按"需求"而不是"有人就发"来扣：预算不够时特效会<b>变淡</b>而不是
 * 消失，玩家看到的是"这次没那么亮"，不是"法术没反应"。</p>
 *
 * <p><b>只用原版粒子</b>（{@code ParticleTypes}），不下载任何素材。</p>
 */
public final class SpellVfx {

    private SpellVfx() {
    }

    // ------------------------------------------------------------------
    // 预算
    // ------------------------------------------------------------------

    /** 每 tick 全服可用的粒子总数（设计值，先乘密度再扣）。 */
    public static final int BUDGET_PER_TICK = 900;

    /** 单个特效一次最多发多少粒子（兜底，防止写错数量）。 */
    public static final int MAX_PER_EFFECT = 2200;   // 高境界（帝境/鸿蒙 ×2.2）也要装得下

    /**
     * <b>法术/领域表现层的统一减量系数（2026-11-07 第三轮下调）</b>。
     *
     * <p>玩家前后两轮都要求「特效还是下调」：上一轮把各调用点的数量减了约一半，
     * 但调用点太多、总有漏网；这一轮改成在<b>唯一发粒子的出口</b>再乘一次全局系数，
     * 保证"法术 / 领域 / 职业机制"三路的所有粒子<b>一刀切再降 40%</b>，
     * 不会再出现"改了 A 忘了 B"。</p>
     *
     * <p>注意：战斗命中的粒子走 {@code combat/} 自己的预算，不受这个系数影响 ——
     * 玩家要下调的是"法术/领域特效"，不是打击感。</p>
     */
    public static final double SPELL_FX_SCALE = 0.60D;
    /** 令牌桶对应的 tick，换 tick 就重填。 */
    /** 这一 tick 实际发出去的粒子（测试与诊断用）。 */
    private static int spentThisTick = 0;
    /** 累计发出（测试与诊断用）。 */
    private static long spentTotal = 0L;

    /**
     * 全局粒子预算还剩多少（换 tick 自动重填）。
     *
     * <p>{@code public} 是为了让离线探针/诊断能读，不是给业务逻辑调的。</p>
     */
    public static int remaining(ServerLevel level) {
        // 走唯一的令牌桶（按维度、由 combat.maxParticlesPerTick 配置）
        return com.hongmeng.xiuxian.combat.ParticleBudget.remaining(level);
    }

    /** 这一 tick 已经花掉的粒子数。 */
    public static int spentThisTick() {
        return spentThisTick;
    }

    /** 累计花掉的粒子数。 */
    public static long spentTotal() {
        return spentTotal;
    }



    // ------------------------------------------------------------------
    // 境界分级华丽度（玩家要求：法术分级华丽度）
    // ------------------------------------------------------------------

    /** 当前这次施法所属的境界倍率（主线程内有效，用完即清）。 */
    private static final ThreadLocal<Double> REALM_LUX = new ThreadLocal<>();

    /**
     * 蓄力期间的**额外粒子倍率**。
     *
     * <p>玩家要求「技能蓄力特效要非常非常华丽，使劲堆粒子效果就行了」。
     * 蓄力是站桩的几秒钟，观感优先，所以这里刻意放宽预算（×{@value}）。</p>
     */
    private static final ThreadLocal<Double> CHARGE_BOOST = new ThreadLocal<>();

    /** 蓄力期间把粒子想要量再乘多少（含境界倍率之后再乘）。 */
    private static final double CHARGE_BOOST_FACTOR = 1.6D;

    /** 由调用方按境界设置的蓄力预算（飞升起拉满）。 */
    public static void setChargeBudget(double budget) {
        CHARGE_BUDGET.set(Math.max(0.5D, Math.min(4.0D, budget)));
    }

    private static final ThreadLocal<Double> CHARGE_BUDGET = new ThreadLocal<>();

    /**
     * 按大境给出"华丽度倍率"。
     *
     * <p>练气/筑基朴素，金丹/元婴成型，化神/道印明显变浓，
     * 飞升/仙人华丽，帝境/鸿蒙炫目。</p>
     */
    /** 当前蓄力预算（带默认值与清理）。 */
    private static double currentChargeBudget() {
        Double v = CHARGE_BUDGET.get();
        if (v == null) {
            return CHARGE_BOOST_FACTOR;
        }
        CHARGE_BUDGET.remove();
        return v;
    }

    /** 变化最浓的档位（玩家要求：飞升起拉满）。 */
    public static double realmLux(int tier) {
        // 玩家要求：「飞升境界就可以解锁所有的法术和特效级别」
        // → 飞升(7) 起直接给变化最浓的一档，不再等鸿蒙。
        if (tier >= 7) {
            return 2.20D;      // 飞升 / 仙人 / 帝境 / 鸿蒙：最高档
        }
        return switch (tier) {
            case 6 -> 1.60D;   // 道印
            case 5 -> 1.15D;   // 化神
            case 4 -> 0.80D;   // 元婴
            default -> 0.55D;  // 金丹及以下
        };
    }

    /** 施法前设一次（`TechniqueCaster.resolve` 里调）。 */
    public static void beginRealmLux(int tier) {
        REALM_LUX.set(realmLux(tier));
    }

    /** 施法后立刻清掉，避免影响别人的特效。 */
    public static void endRealmLux() {
        REALM_LUX.remove();
    }

    /** 当前倍率（没设过就是 1.0）。 */
    private static double currentLux() {
        Double v = REALM_LUX.get();
        return v == null ? 1.0D : v;
    }

    /**
     * 把"想要的数量"折算成"这次真的发多少"。
     *
     * @return 实际数量；0 表示这次不发
     */
    static int take(ServerLevel level, int want) {
        if (level == null || want <= 0) {
            return 0;
        }
        // **密度与预算都走 ParticleBudget**——全模组只有这一份口径。
        //
        // 合并前这里是自己一套（全局 320/tick + combat.particleDensity），
        // 而战斗手感那边另有一套（按维度 400/tick + combat.particleDensity）。
        // 结果是**两个旋钮各管一半特效**，最坏合计 720/tick，
        // 而且调低一个只影响一部分——玩家根本没法判断该调哪个。
        double density = com.hongmeng.xiuxian.combat.ParticleBudget.density();
        if (density <= 0.0D) {
            return 0;
        }

        // **境界分级**：同一门法术，境界越高越华丽（练气 ×0.55 → 鸿蒙 ×2.20）。
        // 放在"单次上限"之前乘，这样高境界能真正突破原来的观感上限；
        // 全局令牌桶仍然在后面生效，人多同屏不会被倍率打爆。
        Double boost = CHARGE_BOOST.get();
        double chargeMul = boost == null ? 1.0D : boost;
        int luxed = (int) Math.round(want * currentLux() * chargeMul);

        // 单次上限（这是本类的局部约束，不是全局预算）
        int capped = Math.min(Math.max(luxed, 1), MAX_PER_EFFECT);

        // 2026-11-07 全局减量：法术/领域/职业机制所有粒子在出口处再乘一次系数
        //（玩家二轮反馈「还是下调」；调用点太多难免漏网，统一在唯一出口收口）
        int scaled = (int) Math.round(capped * SPELL_FX_SCALE);
        if (scaled <= 0) {
            return 0;
        }

        // 密度折算
        int wanted = (int) Math.round(scaled * density);
        if (wanted <= 0) {
            // 小数被截掉了：按概率发 1 个，避免密度 0.3 时小特效整个消失
            double frac = scaled * density;
            if (frac <= 0.0D || level.random.nextDouble() >= frac) {
                return 0;
            }
            wanted = 1;
        }

        // 全局（按维度）预算：唯一的令牌桶
        int granted = com.hongmeng.xiuxian.combat.ParticleBudget.spend(level, wanted);
        spentThisTick += granted;
        spentTotal += granted;
        return granted;
    }

    /** 配置里的密度倍率。 */
    private static double density() {
        // 委派给 ParticleBudget：全模组只认 combat.particleDensity 这一个旋钮
        try {
            return com.hongmeng.xiuxian.combat.ParticleBudget.density();
        } catch (Throwable ignored) {
            // 配置没起来：按设计值
        }
        return 1.0D;
    }

    // ------------------------------------------------------------------
    // 元素配色 / 音效
    // ------------------------------------------------------------------

    /**
     * 元素的主粒子（原来的 {@code particleOf}）。
     *
     * <p>保留这个方法名与语义：外部（含别的包）有在用它。</p>
     */
    public static ParticleOptions particleOf(Element element) {
        if (element == null) {
            return ParticleTypes.ENCHANT;
        }
        return switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.SPLASH;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case WIND -> ParticleTypes.CLOUD;
            case EARTH -> ParticleTypes.CRIT;
            case WOOD -> ParticleTypes.HAPPY_VILLAGER;
            // 玩家要求「金的特效是金色的、风是青色」：
            // 金改用 WAX_ON（原版唯一的蜂蜜金色粒子），与风的 CLOUD 彻底分开。
            case METAL -> ParticleTypes.WAX_ON;
            default -> ParticleTypes.ENCHANT;
        };
    }

    /** 第二层：元素的"性格"粒子（更小更密，铺质感）。 */
    public static ParticleOptions accentOf(Element element) {
        if (element == null) {
            return ParticleTypes.ENCHANT;
        }
        return switch (element) {
            case FIRE -> ParticleTypes.SMALL_FLAME;
            case WATER -> ParticleTypes.BUBBLE_POP;
            case ICE -> ParticleTypes.ITEM_SNOWBALL;
            case THUNDER -> ParticleTypes.END_ROD;
            case WIND -> ParticleTypes.WHITE_ASH;
            case EARTH -> ParticleTypes.ASH;
            case WOOD -> ParticleTypes.HAPPY_VILLAGER;
            // 副粒子也从 ELECTRIC_SPARK（会和雷撞）换成 SCRAPE（铜金）。
            case METAL -> ParticleTypes.SCRAPE;
            default -> ParticleTypes.SOUL_FIRE_FLAME;
        };
    }

    /** 第三层：余韵粒子（慢慢散，负责体积感）。 */
    public static ParticleOptions auraOf(Element element) {
        if (element == null) {
            return ParticleTypes.ENCHANT;
        }
        return switch (element) {
            case FIRE -> ParticleTypes.LARGE_SMOKE;
            case WATER -> ParticleTypes.FALLING_WATER;
            case ICE -> ParticleTypes.WHITE_ASH;
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case WIND -> ParticleTypes.CLOUD;
            case EARTH -> ParticleTypes.ASH;         // FALLING_DUST 要 BlockParticleOption，不是 ParticleOptions
            case WOOD -> ParticleTypes.END_ROD;
            case METAL -> ParticleTypes.FIREWORK;
            default -> ParticleTypes.SOUL;
        };
    }

    /** 释放音效（按元素分）。 */
    public static SoundEvent soundOf(Element element) {
        if (element == null) {
            return SoundEvents.ENCHANTMENT_TABLE_USE;
        }
        return switch (element) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case WATER -> SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
            case ICE -> SoundEvents.GLASS_BREAK;
            case THUNDER -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case WIND -> SoundEvents.PHANTOM_FLAP;
            case EARTH -> SoundEvents.STONE_BREAK;
            case WOOD -> SoundEvents.AZALEA_LEAVES_BREAK;
            case METAL -> SoundEvents.ANVIL_LAND;
            default -> SoundEvents.ENCHANTMENT_TABLE_USE;
        };
    }

    /**
     * 起手 / 蓄力音。
     *
     * <p>故意和结算音不同：起手用"吸气"的感觉（低音、轻），结算用"炸开"。</p>
     */
    public static SoundEvent castSoundOf(Element element) {
        if (element == null) {
            return SoundEvents.ENCHANTMENT_TABLE_USE;
        }
        return switch (element) {
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case WATER -> SoundEvents.BUCKET_FILL;
            case ICE -> SoundEvents.GLASS_PLACE;
            case THUNDER -> SoundEvents.TRIDENT_THUNDER;
            case WIND -> SoundEvents.ELYTRA_FLYING;
            case EARTH -> SoundEvents.GRAVEL_PLACE;
            case WOOD -> SoundEvents.GRASS_PLACE;
            case METAL -> SoundEvents.ANVIL_PLACE;
            default -> SoundEvents.ENCHANTMENT_TABLE_USE;
        };
    }

    // ------------------------------------------------------------------
    // 释放音（带一点随机音高，避免连放时像复读机）
    // ------------------------------------------------------------------

    /**
     * 播一次释放音。
     *
     * @param volume 音量倍率（1.0 = 标准）
     */
    public static void playCast(ServerLevel level, Vec3 at, Element element, float volume) {
        if (level == null || at == null) {
            return;
        }
        // 音高在 0.92~1.10 之间抖一下：连放同一门功法不会听成电子音
        float pitch = 0.92F + level.random.nextFloat() * 0.18F;
        level.playSound(null, at.x, at.y, at.z, soundOf(element),
                SoundSource.PLAYERS, Math.max(0.05F, volume), pitch);
    }

    /** 播一次起手/蓄力音。 */
    public static void playBegin(ServerLevel level, Vec3 at, Element element) {
        if (level == null || at == null) {
            return;
        }
        float pitch = 0.75F + level.random.nextFloat() * 0.15F;
        level.playSound(null, at.x, at.y, at.z, castSoundOf(element),
                SoundSource.PLAYERS, 0.45F, pitch);
    }

    // ------------------------------------------------------------------
    // 发射原语
    // ------------------------------------------------------------------

    /**
     * 在一点上撒粒子（三层里任意一层的通用出口）。
     *
     * @param particle 粒子
     * @param count    想要的数量（会被密度与预算折算）
     * @param spread   扩散半径
     * @param speed    初速
     * @return 实际发出多少
     */
    public static int burst(ServerLevel level, Vec3 at, ParticleOptions particle,
                            int count, double spread, double speed) {
        int n = take(level, count);
        if (n <= 0) {
            return 0;
        }
        level.sendParticles(particle, at.x, at.y, at.z, n,
                spread, spread, spread, speed);
        return n;
    }

    /**
     * <b>冲击尘云</b>：贴地横向铺开的灰。
     *
     * <p>让爆炸"有重量"的两半之一。做法是<b>两层叠加</b>：</p>
     * <ul>
     *   <li>{@code POOF}：瞬时爆开的小团烟（扩散快、寿命短）；</li>
     *   <li>{@code CAMPFIRE_COSY_SMOKE}：慢速大团烟，起点略抬高、
     *       带一点向上初速 —— 于是它会<b>横向铺开再缓缓上升</b>，
     *       看起来就是被冲击掀起来的地面浮尘。</li>
     * </ul>
     *
     * @param radius 尘云半径（一般给法术半径的 1.1~1.5 倍）
     */
    public static void impactDust(ServerLevel level, Vec3 at, double radius) {
        if (level == null || at == null) {
            return;
        }
        int want = (int) Math.min(140.0D, 26.0D + radius * 7.0D);
        int poof = take(level, want);
        if (poof > 0) {
            // 贴地薄薄一层，横向扩散
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                    at.x, at.y + 0.35D, at.z, poof,
                    radius * 0.65D, 0.22D, radius * 0.65D, 0.06D);
        }
        int smoke = take(level, Math.max(6, want / 2));
        if (smoke > 0) {
            // 慢速大团烟：起点更高、向上初速更大 —— 才有"掀起来"的感觉
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    at.x, at.y + 0.8D, at.z, smoke,
                    radius * 0.85D, 0.45D, radius * 0.85D, 0.02D);
        }
    }

    /**
     * <b>火花喷溅</b>：被打出来的火星，<b>有重力会往下掉</b>。
     *
     * <p>爆炸"有重量"的另一半。关键是<b>负的向下初速</b>加上原版粒子的重力 ——
     * 于是火星先向外飞、再被拉下去，落在地上熄灭。
     * 纯向上飘的光点永远做不出这个。</p>
     *
     * @param radius 喷溅半径
     * @param heat   0~1，越大越"熔"（用 LAVA 而不是 CRIT）
     */
    public static void sparkSpray(ServerLevel level, Vec3 at, double radius, float heat) {
        if (level == null || at == null) {
            return;
        }
        int want = (int) Math.min(90.0D, 16.0D + radius * 5.0D);
        int n = take(level, want);
        if (n <= 0) {
            return;
        }
        var type = heat > 0.6F
                ? net.minecraft.core.particles.ParticleTypes.LAVA
                : net.minecraft.core.particles.ParticleTypes.CRIT;
        // 向上 + 向外，靠原版重力把它们拉下来
        level.sendParticles(type, at.x, at.y + 0.5D, at.z, n,
                radius * 0.5D, 0.30D, radius * 0.5D, 0.28D);
        // 再补一层电火花（更细、更快熄灭，做出"噼啪"的碎屑感）
        int extra = take(level, n / 2);
        if (extra > 0) {
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    at.x, at.y + 0.6D, at.z, extra,
                    radius * 0.6D, 0.35D, radius * 0.6D, 0.22D);
        }
    }

    /**
     * 沿一条线段撒粒子。
     *
     * @param from      起点
     * @param to        终点
     * @param particle  粒子
     * @param count     想要的总数（会被折算）
     * @param thickness 横向抖动
     * @param speed     初速
     * @return 实际发出多少
     */
    public static int trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle,
                            int count, double thickness, double speed) {
        if (level == null || from == null || to == null) {
            return 0;
        }
        int n = take(level, count);
        if (n <= 0) {
            return 0;
        }
        Vec3 dir = to.subtract(from);
        // 每个点一次 sendParticles：n 个点、每点 1 个，这样才成"线"而不是"云"
                com.hongmeng.xiuxian.technique.ParticleBatch pb1 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.0D : (double) i / (double) (n - 1);
            Vec3 p = from.add(dir.scale(t));
            pb1.add(particle, p.x, p.y, p.z);
        }
        pb1.flush();

        return n;
    }

    /**
     * 一个水平圆环。
     *
     * @param center 圆心（环所在的 y）
     * @param radius 半径
     * @param count  想要的数量（会被折算）
     * @param rise   每个粒子往上抬多少（做出"上升的环"）
     */
    public static int ring(ServerLevel level, Vec3 center, ParticleOptions particle,
                           double radius, int count, double rise, double speed) {
        if (level == null || center == null) {
            return 0;
        }
        int n = take(level, count);
        if (n <= 0) {
            return 0;
        }
                com.hongmeng.xiuxian.technique.ParticleBatch pb2 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n;
            pb2.add(particle, center.x + Math.cos(a) * radius, center.y + rise * (i % 4) / 3.0D, center.z + Math.sin(a) * radius);
        }
        pb2.flush();

        return n;
    }

    /**
     * 一个竖直球壳（做"炸开"的感觉）。
     *
     * <p>用 Fibonacci 球面分布，比随机撒更均匀——随机撒总会有几处特别空。</p>
     */
    public static int shell(ServerLevel level, Vec3 center, ParticleOptions particle,
                            double radius, int count, double speed) {
        if (level == null || center == null) {
            return 0;
        }
        int n = take(level, count);
        if (n <= 0) {
            return 0;
        }
        double golden = Math.PI * (3.0D - Math.sqrt(5.0D));
                com.hongmeng.xiuxian.technique.ParticleBatch pb3 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double y = 1.0D - 2.0D * i / Math.max(1, n - 1);
            double r = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double theta = golden * i;
            pb3.add(particle, center.x + Math.cos(theta) * r * radius, center.y + y * radius, center.z + Math.sin(theta) * r * radius);
        }
        pb3.flush();

        return n;
    }

    // ------------------------------------------------------------------
    // 按形态的成套特效
    // ------------------------------------------------------------------

    /** 直线穿透：一条亮芯 + 两侧飞散。 */
    public static void lineFx(ServerLevel level, Vec3 from, Vec3 to, Element element) {
        trail(level, from, to, ParticleTypes.END_ROD, 26, 0.03D, 0.0D);
        trail(level, from, to, particleOf(element), 32, 0.10D, 0.01D);
        trail(level, from, to, accentOf(element), 18, 0.24D, 0.04D);
        trail(level, from, to, ParticleTypes.ENCHANT, 14, 0.35D, 0.02D);
        burst(level, to, particleOf(element), 36, 0.45D, 0.06D);
        // ---- 华丽层（玩家要求：已有法术也加粒子）----
        // 残余光轨：慢速 END_ROD 让"刚才那一线"留一会儿
        trail(level, from, to, ParticleTypes.END_ROD, 16, 0.12D, 0.005D);
        // 命中处：三重光环 + 球壳 + 上升灵气
        grandRings(level, to, accentOf(element), 2, 2.6D, 34);
        shell(level, to, ParticleTypes.END_ROD, 1.6D, 24, 0.02D);
        spiral(level, to.subtract(0.0D, 0.4D, 0.0D), auraOf(element), 15, 1.2D, 2.6D);
        burst(level, to, ParticleTypes.END_ROD, 2, 0.0D, 0.0D);
    }

    /** 扇形散射：每一道都拖一条细尾。 */
    public static void fanFx(ServerLevel level, Vec3 origin, Vec3[] dirs, Element element) {
        if (dirs == null || dirs.length == 0) {
            return;
        }
        for (Vec3 dir : dirs) {
            Vec3 end = origin.add(dir.scale(10.0D));
            trail(level, origin, end, particleOf(element), 14, 0.05D, 0.01D);
            trail(level, origin, end, ParticleTypes.END_ROD, 8, 0.02D, 0.0D);
            burst(level, end, accentOf(element), 7, 0.20D, 0.03D);
        }
        ring(level, origin, ParticleTypes.SWEEP_ATTACK, 1.1D, 14, 0.05D, 0.0D);
        // 扇形：起手处转起漩涡，让"一次撒出好多道"有来源
        swirl(level, origin.add(0.0D, 1.0D, 0.0D), accentOf(element), 3, 23, 0.9D);
        // ---- 华丽层：每道刺的残影 + 起手十字爆 ----
        for (Vec3 dir : dirs) {
            Vec3 end = origin.add(dir.scale(10.0D));
            Vec3 back = origin.add(dir.scale(6.0D));
            trail(level, back, end, ParticleTypes.END_ROD, 7, 0.10D, 0.0D);
            burst(level, end, ParticleTypes.END_ROD, 1, 0.0D, 0.0D);
        }
        grandRings(level, origin, particleOf(element), 1, 2.2D, 24);
        burst(level, origin.add(0.0D, 1.0D, 0.0D), ParticleTypes.END_ROD, 14, 0.9D, 0.12D);
    }

    /** 投射物：头重尾轻。 */
    public static void projectileFx(ServerLevel level, Vec3 origin, Vec3 look,
                                    double range, Element element) {
        Vec3 end = origin.add(look.scale(range));
        // 尾巴从"离手一点"开始，免得糊在自己脸上
        trail(level, origin.add(look.scale(0.6D)), end, accentOf(element), 20, 0.16D, 0.02D);
        trail(level, origin.add(look.scale(0.6D)), end, particleOf(element), 16, 0.04D, 0.01D);
        trail(level, origin.add(look.scale(0.6D)), end, ParticleTypes.END_ROD, 10, 0.02D, 0.0D);
        burst(level, end, particleOf(element), 30, 0.35D, 0.08D);
        shell(level, end, auraOf(element), 0.9D, 16, 0.02D);
        // 命中：闪光 + 多重光环 + 上浮光点
        burst(level, end, ParticleTypes.END_ROD, 4, 0.0D, 0.0D);
        grandRings(level, end, accentOf(element), 2, 2.8D, 34);
        spiral(level, end.subtract(0.0D, 0.4D, 0.0D), ParticleTypes.ENCHANT, 20, 1.3D, 2.6D);
        // ---- 华丽层：拖尾残影（错开半格）+ 命中球壳 + 尾迹留痕 ----
        Vec3 mid = origin.add(look.scale(range * 0.55D));
        trail(level, mid, end, ParticleTypes.SOUL_FIRE_FLAME, 14, 0.30D, 0.02D);
        shell(level, end, auraOf(element), 1.5D, 24, 0.03D);
        trail(level, origin.add(look.scale(0.6D)), end, ParticleTypes.END_ROD, 16, 0.06D, 0.0D);
    }

    /** 范围爆发：向内收束 + 向外炸开 + 地面环。 */
    public static void aoeFx(ServerLevel level, Vec3 center, double radius,
                             boolean thunder, Element element) {
        Vec3 c = center.add(0.0D, 0.4D, 0.0D);
        // 地面环：范围感的关键（玩家看得见"打到哪里"）
        ring(level, center.add(0.0D, 0.15D, 0.0D), particleOf(element),
                radius * 0.95D, 50, 0.30D, 0.0D);
        ring(level, center.add(0.0D, 0.15D, 0.0D), accentOf(element),
                radius * 0.55D, 26, 0.15D, 0.01D);
        // 炸开
        shell(level, c, particleOf(element), radius * 0.55D, 42, 0.05D);
        shell(level, c, ParticleTypes.END_ROD, radius * 0.85D, 20, 0.02D);
        burst(level, c, ParticleTypes.END_ROD, 3, 0.0D, 0.0D);
        // ---- 华丽层次：脚下阵纹 + 冲天光柱 + 上浮漩涡 ----
        grandRings(level, center, accentOf(element), 3, radius * 1.05D, 40);
        spiral(level, center, particleOf(element), 30, radius * 0.75D, 4.6D);
        swirl(level, center.add(0.0D, 1.2D, 0.0D), auraOf(element), 2, 36, radius * 0.85D);
        // ---- 再加一层（玩家要求：现有法术也要华丽）----
        // 外扩冲击环三波
        for (int wave = 1; wave <= 3; wave++) {
            ring(level, center.add(0.0D, 0.12D * wave, 0.0D), ParticleTypes.SWEEP_ATTACK,
                    radius * (0.35D + 0.22D * wave), 20, 0.05D, 0.05D);
        }
        // 向内收束（先收后放的观感）
        shell(level, c, ParticleTypes.SOUL, radius * 0.8D, 20, -0.35D);
        // 冲天光柱
        trail(level, center, center.add(0.0D, radius * 1.6D, 0.0D),
                ParticleTypes.END_ROD, 30, 0.25D, 0.02D);
        burst(level, center.add(0.0D, 1.0D, 0.0D), ParticleTypes.END_ROD, 3, 0.0D, 0.0D);
        if (thunder) {
            // 九霄雷狱这类雷法：额外打几道竖直电弧
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2.0D * i / 6.0D;
                Vec3 foot = center.add(Math.cos(a) * radius * 0.6D, 0.0D,
                        Math.sin(a) * radius * 0.6D);
                trail(level, foot, foot.add(0.0D, 4.5D, 0.0D),
                        ParticleTypes.ELECTRIC_SPARK, 8, 0.08D, 0.02D);
                burst(level, foot.add(0.0D, 3.2D, 0.0D), ParticleTypes.END_ROD, 4,
                        0.3D, 0.02D);
            }
        }
    }

    /** 位移：残影 + 落地冲击环。 */
    public static void dashFx(ServerLevel level, Vec3 from, Vec3 to, Element element) {
        trail(level, from.add(0.0D, 1.0D, 0.0D), to.add(0.0D, 1.0D, 0.0D),
                particleOf(element), 26, 0.28D, 0.02D);
        trail(level, from.add(0.0D, 1.0D, 0.0D), to.add(0.0D, 1.0D, 0.0D),
                ParticleTypes.END_ROD, 14, 0.10D, 0.0D);
        trail(level, from.add(0.0D, 0.6D, 0.0D), to.add(0.0D, 0.6D, 0.0D),
                ParticleTypes.CLOUD, 7, 0.18D, 0.01D);
        ring(level, to.add(0.0D, 0.1D, 0.0D), accentOf(element), 1.1D, 13, 0.10D, 0.02D);
        burst(level, to.add(0.0D, 1.0D, 0.0D), ParticleTypes.SWEEP_ATTACK, 3, 0.5D, 0.0D);
        // ---- 华丽层：起点爆散 + 途中残影带 + 落点三重环 ----
        burst(level, from.add(0.0D, 1.0D, 0.0D), auraOf(element), 13, 0.7D, 0.10D);
        grandRings(level, to, particleOf(element), 2, 2.4D, 30);
        trail(level, from.add(0.0D, 1.0D, 0.0D), to.add(0.0D, 1.0D, 0.0D),
                ParticleTypes.END_ROD, 18, 0.18D, 0.0D);
    }

    /** 辅助类（治疗/增益）：向上螺旋，和攻击法术的"向外炸"区分开。 */
    public static void supportFx(ServerLevel level, Vec3 center, boolean heal,
                                 Element element) {
        // 玩家要求「攻击出来的特效不能是显示多少心，改成数字」：
// 辅助/治疗类原来用 HEART（心形）粒子，看起来像"回血多少颗心"，与伤害数字混淆。
// 换成绿色灵气（HAPPY_VILLAGER）—— 同样是"增益/回复"的通用语汇，但不是心形，
// 不会再和数字读串。伤害本身由 DamageNumbers 以数字显示。
        ParticleOptions main = heal ? ParticleTypes.HAPPY_VILLAGER : particleOf(element);
        ParticleOptions sub = heal ? ParticleTypes.HAPPY_VILLAGER : accentOf(element);
        int n = take(level, 22);
                com.hongmeng.xiuxian.technique.ParticleBatch pb =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.0D : (double) i / (double) (n - 1);
            double a = t * Math.PI * 4.0D;          // 两圈螺旋
            double r = 0.85D * (1.0D - t * 0.45D);
            pb.add(heal ? main : sub, center.x + Math.cos(a) * r, center.y + 1.9D + t * 2.1D, center.z + Math.sin(a) * r);
        }
        pb.flush();

        // 内层：贴着身体的亮芯
        trail(level, center.add(0.0D, 1.9D, 0.0D), center.add(0.0D, 3.2D, 0.0D),
                heal ? ParticleTypes.END_ROD : main, 9, 0.22D, 0.0D);
        ring(level, center.add(0.0D, 0.1D, 0.0D), sub, 0.95D, 11, 0.06D, 0.01D);
        // ---- 华丽度：三层同心光环 + 上升光柱 + 顶部绽放 ----
        grandRings(level, center, sub, 2, 1.6D, 26);
        burst(level, center.add(0.0D, 2.6D, 0.0D), ParticleTypes.END_ROD, 16, 0.4D, 0.03D);
        shell(level, center.add(0.0D, 1.2D, 0.0D), ParticleTypes.ENCHANT, 1.3D, 18, 0.01D);
        // ---- 再加一层：双向交叉螺旋 + 上浮灵气 ----
        spiral(level, center.add(0.0D, 1.9D, 0.0D), ParticleTypes.END_ROD, 12, 1.0D, 2.6D);
        spiral(level, center.add(0.0D, 1.9D, 0.0D), sub, 12, 0.7D, 2.2D);
        burst(level, center.add(0.0D, 1.6D, 0.0D), ParticleTypes.HAPPY_VILLAGER, 10, 0.6D, 0.02D);
    }

    /** 吸血：从目标身上往自己这边收的红线。 */
    public static void drainFx(ServerLevel level, Vec3 center, Element element) {
        ring(level, center.add(0.0D, 2.4D, 0.0D), ParticleTypes.CRIT,
                1.0D, 14, 0.35D, 0.01D);
        shell(level, center.add(0.0D, 2.4D, 0.0D), ParticleTypes.SOUL_FIRE_FLAME,
                0.8D, 11, 0.01D);
        trail(level, center.add(0.0D, 1.8D, 0.0D), center.add(0.0D, 3.4D, 0.0D),
                ParticleTypes.SOUL, 8, 0.3D, 0.0D);
        // ---- 华丽度：血色漩涡（三层反向螺旋）+ 外环 ----
        swirl(level, center.add(0.0D, 2.5D, 0.0D), ParticleTypes.CRIMSON_SPORE, 2, 18, 0.55D);
        spiral(level, center.add(0.0D, 1.6D, 0.0D), ParticleTypes.SOUL, 14, 1.35D, 2.6D);
        grandRings(level, center.add(0.0D, 1.6D, 0.0D), ParticleTypes.DAMAGE_INDICATOR, 1, 1.25D, 18);
    }

    /** 起手圈：脚下起一圈元素光（带上升），比原来的"平铺一圈"更有抬升感。 */
    public static void beginFx(ServerLevel level, Vec3 center, Element element) {
        ring(level, center.add(0.0D, 0.1D, 0.0D), particleOf(element), 1.15D, 16, 0.45D, 0.0D);
        ring(level, center.add(0.0D, 0.1D, 0.0D), accentOf(element), 0.7D, 9, 0.25D, 0.0D);
        // ---- 华丽度：脚下阵纹（双层反向）+ 元素碎光上浮 ----
        grandRings(level, center, accentOf(element), 1, 1.3D, 22);
        swirl(level, center.add(0.0D, 0.4D, 0.0D), particleOf(element), 2, 14, 0.4D);
    }

    /**
     * 蓄力光效（玩家要求「法术蓄力也要有特效」）。
     *
     * <p>按蓄力进度从 0→1 逐渐变浓：脚下阵纹随进度扩张、身周元素螺旋加速、
     * 快满时开始有"收束"的亮芯 —— 玩家不用看进度条也知道快好了。</p>
     *
     * @param progress 0..1
     */
    /**
     * <b>通用蓄力兜底</b>：完整七层结构（阵盘/符环/三重穹环/高空巨环/流光/
     * 内旋符文/汇聚光柱/满蓄爆闪）。
     *
     * <p>现在正式路径走 {@link ChargeStyles} 的八种母题 —— 每个法术各有特色；
     * 这个方法留作兜底（元素或形态没覆盖到时使用），不再直接调用。</p>
     */
    public static void chargeFx(ServerLevel level, Vec3 center, Element element,
                               double progress, double grandeur) {
        double p = Math.max(0.0D, Math.min(1.0D, progress));
        double g = Math.max(0.25D, Math.min(1.30D, grandeur));
        double q = 0.55D + 0.45D * g;              // 粒子量随烈度缩放

        // 蓄力期间把粒子预算放宽（玩家要求：使劲堆粒子）；烈度越大放宽越多
        CHARGE_BOOST.set(currentChargeBudget() * (0.6D + 0.4D * g));
        try {
            var main = particleOf(element);
            var accent = accentOf(element);
            var aura = auraOf(element);
            long t = level.getGameTime();

            // ---- 1) 脚下巨型阵盘 + 最外圈符环（每 10° 一个亮点）----
            double groundR = (8.0D + p * 14.0D) * g;            // 灭世 8→22，弹道 4→11
            int glyphs = (int) (36 * q);
                        com.hongmeng.xiuxian.technique.ParticleBatch pb1 =
                    new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < glyphs; i++) {
                double a = Math.PI * 2.0D * i / glyphs + t * 0.01D;
                pb1.add(main, center.x + Math.cos(a) * groundR, center.y + 0.15D, center.z + Math.sin(a) * groundR);
            }
            pb1.flush();

            ring(level, center.add(0.0D, 0.10D, 0.0D), main, groundR,
                    (int) ((110 + p * 160) * q), 0.05D, 0.0D);
            ring(level, center.add(0.0D, 0.16D, 0.0D), accent, groundR * 0.88D,
                    (int) ((80 + p * 120) * q), 0.10D, 0.0D);
            ring(level, center.add(0.0D, 0.22D, 0.0D), aura, groundR * 0.72D,
                    (int) ((60 + p * 90) * q), 0.16D, 0.0D);
            grandRings(level, center, aura, 3, groundR * 0.95D, (int) ((46 + p * 60) * q));

            // ---- 2) 天上三重穹环：高度与半径随烈度（灭世 9/13/17，弹道 4/6/8）----
                        com.hongmeng.xiuxian.technique.ParticleBatch pb2 =
                    new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int layer = 0; layer < 3; layer++) {
                double rr = (6.0D + layer * 4.0D) * g * (0.55D + p * 0.65D);
                double yy = center.y + (9.0D + layer * 4.0D) * g;
                int n = (int) ((80 + p * 110) * q);
                double spin = t * 0.05D * (layer % 2 == 0 ? 1.0D : -1.0D);
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2.0D * i / n + spin;
                    pb2.add(layer == 1 ? accent : aura, center.x + Math.cos(a) * rr, yy, center.z + Math.sin(a) * rr);
                }
                ring(level, new Vec3(center.x, yy, center.z), main, rr * 0.55D,
                        (int) ((34 + p * 50) * q), 0.02D, 0.0D);
            }
            pb2.flush();


            // ---- 3) 高空巨环：像"天被打开了"（灭世 26 格 @24 高）----
            double skyR = 26.0D * g * (0.4D + p * 0.6D);
            double skyY = 24.0D * g;
            int skyN = (int) ((110 + p * 90) * q);
                        com.hongmeng.xiuxian.technique.ParticleBatch pb3 =
                    new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < skyN; i++) {
                double a = Math.PI * 2.0D * i / skyN - t * 0.03D;
                pb3.add(ParticleTypes.END_ROD, center.x + Math.cos(a) * skyR, center.y + skyY, center.z + Math.sin(a) * skyR);
            }
            pb3.flush();

            shell(level, center.add(0.0D, skyY, 0.0D), aura, skyR * 0.5D,
                    (int) ((50 + p * 50) * q), -0.20D);

            // ---- 4) 流光围绕：始终有，条数随烈度（灭世 6 道 / 小法术 4 道）----
            int lanes = g >= 0.9D ? 6 : 4;
                        com.hongmeng.xiuxian.technique.ParticleBatch pb4 =
                    new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int lane = 0; lane < lanes; lane++) {
                double base = Math.PI * 2.0D * lane / lanes + t * 0.08D;
                int seg = (int) (22 * Math.max(0.7D, g));
                for (int i = 0; i < seg; i++) {
                    double tt = (double) i / Math.max(1, seg - 1);
                    double a = base + tt * Math.PI * 1.6D;
                    double rr = (2.5D + p * 3.0D) * g * (1.0D - tt * 0.25D);
                    double yy = center.y + 0.4D + tt * (10.0D + p * 8.0D) * g;
                    pb4.add(i % 3 == 0 ? main : accent, center.x + Math.cos(a) * rr, yy, center.z + Math.sin(a) * rr);
                }
            }
            pb4.flush();


            // ---- 5) 内旋符文：两层反向 ----
            spiral(level, center, main,
                    (int) ((60 + p * 110) * q), (3.0D + p * 3.0D) * g, (4.0D + p * 2.5D) * g);
            spiral(level, center, accent,
                    (int) ((50 + p * 90) * q), (2.0D + p * 2.5D) * g, (3.4D + p * 2.0D) * g);
            swirl(level, center.add(0.0D, 1.0D, 0.0D), aura, 4,
                    (int) ((70 + p * 90) * q), 5.0D * g);

            // ---- 6) 汇聚光柱：从阵盘边缘向身体收束 ----
            Vec3 body = center.add(0.0D, 1.2D, 0.0D);
            int beams = (int) ((50 + p * 40) * q);
            for (int i = 0; i < beams; i++) {
                double a = Math.PI * 2.0D * i / beams;
                Vec3 from = new Vec3(center.x + Math.cos(a) * groundR, center.y + 0.25D,
                        center.z + Math.sin(a) * groundR);
                trail(level, from, body, accent, 3, 0.05D, 0.0D);
            }

            // ---- 7) 满蓄：巨型白环 + 上爆 + 顶天光柱 + 天顶炸开 ----
            if (p > 0.90D) {
                grandRings(level, center, ParticleTypes.END_ROD, 5,
                        groundR * 1.6D, (int) (130 * q));
                burst(level, body, ParticleTypes.END_ROD, (int) (260 * q), 2.2D * g, 0.45D);
                trail(level, center, center.add(0.0D, 30.0D * g, 0.0D),
                        ParticleTypes.END_ROD, (int) (200 * q), 0.45D * g, 0.02D);
                shell(level, center.add(0.0D, 1.6D, 0.0D), ParticleTypes.ENCHANT,
                        groundR * 0.9D, (int) (130 * q), 0.02D);
                burst(level, center.add(0.0D, skyY, 0.0D), ParticleTypes.END_ROD,
                        4, 1.0D * g, 0.0D);
            }
        } finally {
            CHARGE_BOOST.remove();
        }
    }


    // ------------------------------------------------------------------
    // 灭世法术特效（玩家要求：粒子效果一定要华丽华丽华丽）
    // ------------------------------------------------------------------

    /**
     * **天陨星落**的光柱：从天顶砸到落点。
     *
     * <p>三层柱体叠出"光柱"的体积感：亮芯（END_ROD）／外焰（元素粒子）／光晕（SOUL）。
     * 落点补白闪与三圈冲击环，天顶补一圈散开的云 —— 这样抬头能看到"天开了个口子"。</p>
     */
    public static void orbitalBeam(ServerLevel level, Vec3 sky, Vec3 impact, Element element) {
        if (level == null || sky == null || impact == null) {
            return;
        }
        Vec3 dir = impact.subtract(sky);
        // 柱体：三层（第三轮减量：颗数约 −50%，柱体在贴图层已有光柱几何，不必堆粒子）
        trail(level, sky, impact, ParticleTypes.END_ROD, 60, 0.06D, 0.0D);
        trail(level, sky, impact, particleOf(element), 78, 0.35D, 0.02D);
        trail(level, sky, impact, auraOf(element), 48, 0.70D, 0.04D);
        trail(level, sky, impact, ParticleTypes.SOUL_FIRE_FLAME, 32, 0.30D, 0.06D);
        // 天顶：散开的云 + 爆散光点
        shell(level, sky, ParticleTypes.LARGE_SMOKE, 3.0D, 30, 0.35D);
        shell(level, sky, ParticleTypes.END_ROD, 1.2D, 12, 0.10D);
        burst(level, sky, ParticleTypes.END_ROD, 36, 1.6D, 0.25D);
        // 落点：白闪 + 多层冲击环 + 蘑菇云
        burst(level, impact, ParticleTypes.END_ROD, 5, 0.0D, 0.0D);
        grandRings(level, impact, particleOf(element), 3, 12.0D, 32);
        grandRings(level, impact, accentOf(element), 1, 7.0D, 20);
        shell(level, impact.add(0.0D, 1.0D, 0.0D), ParticleTypes.EXPLOSION, 2.5D, 34, 0.30D);
        spiral(level, impact, auraOf(element), 38, 5.0D, 7.0D);
        // 蘑菇云：向上翻卷（5 层 → 4 层）
        for (int i = 0; i < 4; i++) {
            double y = impact.y + 1.5D + i * 1.6D;
            double r = 2.0D + i * 1.1D;
            ring(level, new Vec3(impact.x, y, impact.z), ParticleTypes.LARGE_SMOKE,
                    r, 20 + i * 4, 0.25D, 0.06D);
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(impact),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(impact),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.6F, 0.7F);
    }

    /**
     * **轨道炮击**：天顶聚集 → 贯穿光刃。
     *
     * <p>先在天顶画一圈"充能环"（显得能量是从天上压下来的），再沿射线上画
     * 一条很密的能量刃，两侧补空气爆裂（CLOUD + ELECTRIC_SPARK）。</p>
     */
    public static void railgunFx(ServerLevel level, Vec3 playerPos, Vec3 origin, Vec3 end,
                                 Element element) {
        if (level == null || origin == null || end == null) {
            return;
        }
        // 天顶充能环
        Vec3 sky = playerPos.add(0.0D, 55.0D, 0.0D);
        grandRings(level, sky, ParticleTypes.ELECTRIC_SPARK, 3, 9.0D, 34);
        shell(level, sky, ParticleTypes.END_ROD, 2.0D, 14, 0.05D);
        trail(level, sky, origin, ParticleTypes.ELECTRIC_SPARK, 44, 0.25D, 0.04D);
        // 贯穿光刃：一条极密的轴线 + 两侧能量（减量：颗数约 −50%）
        trail(level, origin, end, ParticleTypes.END_ROD, 70, 0.03D, 0.0D);
        trail(level, origin, end, particleOf(element), 78, 0.12D, 0.02D);
        trail(level, origin, end, accentOf(element), 54, 0.40D, 0.05D);
        trail(level, origin, end, ParticleTypes.SOUL_FIRE_FLAME, 36, 0.22D, 0.03D);
        // 沿途空气爆裂（30 段 → 18 段）
        int steps = 18;
        for (int i = 0; i <= steps; i++) {
            Vec3 p = origin.add(end.subtract(origin).scale((double) i / steps));
            burst(level, p, ParticleTypes.CLOUD, 4, 1.6D, 0.20D);
            burst(level, p, ParticleTypes.ELECTRIC_SPARK, 3, 1.2D, 0.15D);
        }
        // 终点大爆
        burst(level, end, ParticleTypes.END_ROD, 3, 0.0D, 0.0D);
        grandRings(level, new Vec3(end.x, origin.y, end.z), auraOf(element), 2, 8.0D, 24);
        level.playSound(null, net.minecraft.core.BlockPos.containing(origin),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.8F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(end),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.8F, 0.9F);
    }

    /**
     * **万象寂灭**：四层阵纹 + 冲天光柱 + 向内收束 + 终末白闪。
     *
     * <p>"寂灭"的观感关键在<b>先收后放</b>：先画向内收束的点（像被吸进去），
     * 再放光柱与外扩环。收束用 shell 的反向速度近似。</p>
     */
    public static void annihilateFx(ServerLevel level, Vec3 center, double radius,
                                    Element element) {
        if (level == null || center == null) {
            return;
        }
        // 收束：一圈被吸向中心的点（第三轮减量：颗数约 −50%）
        shell(level, center.add(0.0D, 1.2D, 0.0D), ParticleTypes.SOUL, radius * 0.9D, 44, -0.6D);
        shell(level, center.add(0.0D, 1.2D, 0.0D), ParticleTypes.END_ROD, radius * 0.7D, 34, -0.4D);
        // 阵纹：四层 → 3 层
        grandRings(level, center, particleOf(element), 3, radius, 44);
        grandRings(level, center, accentOf(element), 2, radius * 0.6D, 30);
        // 冲天光柱
        trail(level, center, center.add(0.0D, 26.0D, 0.0D), ParticleTypes.END_ROD, 72,
                0.20D, 0.02D);
        trail(level, center, center.add(0.0D, 22.0D, 0.0D), particleOf(element), 82,
                0.60D, 0.05D);
        trail(level, center, center.add(0.0D, 18.0D, 0.0D), auraOf(element), 58,
                1.20D, 0.08D);
        // 外扩冲击 + 白闪（4 波 → 3 波）
        for (int wave = 1; wave <= 3; wave++) {
            ring(level, center.add(0.0D, 0.2D + wave * 0.3D, 0.0D),
                    ParticleTypes.SWEEP_ATTACK, radius * (0.3D + 0.2D * wave), 34, 0.05D, 0.05D);
        }
        shell(level, center.add(0.0D, 2.0D, 0.0D), ParticleTypes.END_ROD, 4.0D, 28, 0.20D);
        burst(level, center.add(0.0D, 2.0D, 0.0D), ParticleTypes.END_ROD, 4, 0.0D, 0.0D);
        spiral(level, center, auraOf(element), 44, radius * 0.5D, 12.0D);
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 0.7F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.4F, 1.2F);
    }

    /** 余震：一圈外扩的高速冲击环。 */
    public static void aftershock(ServerLevel level, Vec3 center, double radius) {
        if (level == null || center == null) {
            return;
        }
        ring(level, center.add(0.0D, 0.25D, 0.0D), ParticleTypes.SWEEP_ATTACK,
                radius, 26, 0.05D, 0.06D);
        ring(level, center.add(0.0D, 0.25D, 0.0D), ParticleTypes.CLOUD,
                radius * 0.92D, 20, 0.18D, 0.05D);
        burst(level, center.add(0.0D, 0.4D, 0.0D), ParticleTypes.LARGE_SMOKE, 14, 0.6D, 0.10D);
    }


    // ------------------------------------------------------------------
    // 超位魔法特效（玩家要求：参考超位魔法，粒子要华丽）
    // ------------------------------------------------------------------

    /** **太虚黑洞**：内旋吸积盘 + 事件视界 + 时空畸变环。 */
    public static void blackHoleFx(ServerLevel level, Vec3 center, double radius,
                                   Element element) {
        if (level == null || center == null) {
            return;
        }
        // 事件视界：一圈很密的黑核心（用 SOUL 做"吸光"的观感）（减量：约 −50%）
        shell(level, center, ParticleTypes.SQUID_INK, 1.6D, 28, 0.02D);
        shell(level, center, ParticleTypes.SOUL, 2.2D, 24, -0.30D);
        // 吸积盘：多层反向旋转的盘面（4+3 层 → 3+2 层）
        swirl(level, center, particleOf(element), 3, 56, radius * 0.75D);
        swirl(level, center.add(0.0D, 0.6D, 0.0D), accentOf(element), 2, 42, radius * 0.5D);
        // 向内收束（被吞进去的点）
        shell(level, center, ParticleTypes.END_ROD, radius * 0.85D, 38, -0.55D);
        shell(level, center, auraOf(element), radius * 0.6D, 28, -0.40D);
        // 时空畸变环：三层缓慢外扩
        for (int i = 1; i <= 3; i++) {
            ring(level, center.add(0.0D, 0.1D * i, 0.0D), ParticleTypes.PORTAL,
                    radius * (0.3D + 0.25D * i), 28, 0.06D, 0.02D);
        }
        burst(level, center, ParticleTypes.END_ROD, 2, 0.0D, 0.0D);
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.6F, 0.5F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.4F, 0.8F);
    }

    /** **斩星**：巨刃轮廓 + 两侧斩击波 + 空间裂痕。 */
    public static void starSlashFx(ServerLevel level, Vec3 start, Vec3 end,
                                   double halfWidth, Element element) {
        if (level == null || start == null || end == null) {
            return;
        }
        Vec3 dir = end.subtract(start);
        // 巨刃：一条极亮的窄线 + 两侧渐弱的刃面（减量：约 −50%）
        trail(level, start, end, ParticleTypes.END_ROD, 72, 0.05D, 0.0D);
        trail(level, start, end, particleOf(element), 64, 0.25D, 0.02D);
        trail(level, start, end, accentOf(element), 50, halfWidth * 0.9D, 0.05D);
        trail(level, start, end, ParticleTypes.SOUL_FIRE_FLAME, 32, 0.4D, 0.03D);
        // 两侧斩击波：沿线的竖直薄片（40 段 → 24 段）
        int steps = 24;
        for (int i = 0; i <= steps; i++) {
            Vec3 p = start.add(dir.scale((double) i / steps));
            burst(level, p, ParticleTypes.SWEEP_ATTACK, 1, halfWidth, 0.02D);
            burst(level, p.add(0.0D, 2.0D, 0.0D), ParticleTypes.CLOUD, 3, 1.2D, 0.10D);
        }
        // 空间裂痕：从落点延伸的竖直裂缝（24 → 14 段）
        Vec3 tail = end;
                com.hongmeng.xiuxian.technique.ParticleBatch pb1 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int k = 0; k < 14; k++) {
            pb1.add(ParticleTypes.END_ROD, tail.x, start.y + k * 1.1D, tail.z);
            level.sendParticles(ParticleTypes.PORTAL,
                    tail.x, start.y + k * 1.1D, tail.z, 1, 0.4D, 0.0D, 0.4D, 0.02D);
        }
        pb1.flush();

        grandRings(level, new Vec3(end.x, start.y, end.z), auraOf(element), 3, 9.0D, 30);
        burst(level, end, ParticleTypes.END_ROD, 3, 0.0D, 0.0D);
        level.playSound(null, net.minecraft.core.BlockPos.containing(start),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(end),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.5F);
    }

    /** **焚天煮海**：天火 + 地表焦痕环 + 火柱群。 */
    public static void burnSeaFx(ServerLevel level, Vec3 center, double radius,
                                 Element element) {
        if (level == null || center == null) {
            return;
        }
        // 天火：从高处落下的火雨（60 → 32 颗）
                com.hongmeng.xiuxian.technique.ParticleBatch pb2 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < 32; i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            double d = Math.sqrt(level.random.nextDouble()) * radius;
            pb2.add(ParticleTypes.FALLING_LAVA, center.x + Math.cos(a) * d, center.y + 9.0D + level.random.nextDouble() * 4.0D, center.z + Math.sin(a) * d);
        }
        pb2.flush();

        // 地表焦痕：多层环（4+3 层 → 3+2 层）
        grandRings(level, center, ParticleTypes.LAVA, 3, radius, 34);
        grandRings(level, center, ParticleTypes.SMALL_FLAME, 2, radius * 0.6D, 26);
        // 火柱群：半径内随机 10 根 → 5 根
        for (int i = 0; i < 5; i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            double d = Math.sqrt(level.random.nextDouble()) * radius * 0.85D;
            double x = center.x + Math.cos(a) * d;
            double z = center.z + Math.sin(a) * d;
            trail(level, new Vec3(x, center.y, z), new Vec3(x, center.y + 8.0D, z),
                    ParticleTypes.FLAME, 14, 0.25D, 0.10D);
            trail(level, new Vec3(x, center.y, z), new Vec3(x, center.y + 6.0D, z),
                    ParticleTypes.LAVA, 9, 0.20D, 0.05D);
        }
        burst(level, center.add(0.0D, 1.0D, 0.0D), ParticleTypes.END_ROD, 3, 0.0D, 0.0D);
        shell(level, center.add(0.0D, 2.0D, 0.0D), ParticleTypes.LARGE_SMOKE,
                radius * 0.7D, 32, 0.25D);
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.8F, 0.7F);
    }

    /**
     * 多重同心环（华丽度的主力）：同一高度按不同半径、不同抬升各画一圈。
     *
     * <p>单环只能表达"范围"，多环叠起来才有"阵"的观感 —— 这是参考主流
     * 法术特效素材后最容易见效的一招，且完全用原版粒子实现。</p>
     *
     * @param count 环数
     */
    public static void grandRings(ServerLevel level, Vec3 center, ParticleOptions particle,
                                  int count, double maxRadius, int perRing) {
        if (level == null || center == null || count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double t = count == 1 ? 1.0D : (double) i / (double) (count - 1);
            double r = maxRadius * (0.45D + 0.55D * t);
            // 交替抬升：偶数环静止、奇数环上浮，看起来像"层层推进"
            double rise = (i % 2 == 0) ? 0.0D : 0.35D;
            ring(level, center.add(0.0D, 0.08D + i * 0.06D, 0.0D), particle, r,
                    Math.max(6, perRing - i * 2), rise, 0.0D);
        }
    }

    /**
     * 反向螺旋（配合 {@link #swirl}）：绕竖直轴盘旋上升，做"气流/灵气缠绕"。
     *
     * @param turns   圈数
     */
    public static void spiral(ServerLevel level, Vec3 center, ParticleOptions particle,
                              int count, double radius, double height) {
        if (level == null || center == null) {
            return;
        }
        int n = take(level, count);
        if (n <= 0) {
            return;
        }
                com.hongmeng.xiuxian.technique.ParticleBatch pb3 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.0D : (double) i / (double) (n - 1);
            double a = t * Math.PI * 6.0D;              // 三圈
            double r = radius * (1.0D - t * 0.35D);
            pb3.add(particle, center.x + Math.cos(a) * r, center.y + 0.15D + t * height, center.z + Math.sin(a) * r);
        }
        pb3.flush();

    }

    /**
     * 多层漩涡（横向盘面，正反交替）。
     *
     * <p>和 {@link #spiral} 的分工：spiral 是"竖着往上卷"，swirl 是"横着转盘"，
     * 两个叠在一起才有"灵气在周身翻滚"的厚度。</p>
     */
    public static void swirl(ServerLevel level, Vec3 center, ParticleOptions particle,
                             int layers, int count, double radius) {
        if (level == null || center == null || layers <= 0) {
            return;
        }
                com.hongmeng.xiuxian.technique.ParticleBatch pb4 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int layer = 0; layer < layers; layer++) {
            int n = take(level, Math.max(6, count / layers));
            if (n <= 0) {
                continue;
            }
            double r = radius * (1.0D - layer * 0.22D);
            double y = center.y + layer * 0.22D;
            double dir = (layer % 2 == 0) ? 1.0D : -1.0D;   // 正反交替
            for (int i = 0; i < n; i++) {
                double a = dir * Math.PI * 2.0D * i / n;
                pb4.add(particle, center.x + Math.cos(a) * r, y, center.z + Math.sin(a) * r);
            }
        }
        pb4.flush();

    }

    /** 中断：元素烟散掉。 */
    public static void interruptFx(ServerLevel level, Vec3 center, Element element) {
        ring(level, center.add(0.0D, 0.8D, 0.0D), ParticleTypes.SMOKE, 0.7D, 30, 0.10D, 0.01D);
        ring(level, center.add(0.0D, 1.0D, 0.0D), ParticleTypes.WHITE_ASH, 0.5D, 20, 0.22D, 0.01D);
        burst(level, center.add(0.0D, 0.9D, 0.0D), auraOf(element), 20, 0.4D, 0.01D);
    }

    /**
     * 结算时套在玩家身上的"收束环"。
     *
     * <p>每个法术结算都会走一次：先把注意力吸到施法者身上，再看效果飞出去。</p>
     */
    public static void resolveFx(ServerLevel level, Vec3 center, Element element) {
        ring(level, center.add(0.0D, 0.15D, 0.0D), particleOf(element), 1.35D, 12, 0.5D, 0.0D);
        burst(level, center.add(0.0D, 1.0D, 0.0D), accentOf(element), 6, 0.5D, 0.02D);
    }

    /** 找随机数用（给外部留的口子，避免各处自己 new RandomSource）。 */
    public static RandomSource rng(ServerLevel level) {
        return level == null ? RandomSource.create() : level.random;
    }

    // ------------------------------------------------------------------
    // 元素附带状态的表现（点燃 / 冰霜 / 雷弧 / 定身 / 击退）
    // ------------------------------------------------------------------

    /** 元素附带状态的视觉种类（与 {@code TechniqueCaster.applyElementSideEffect} 一一对应）。 */
    public enum StatusFx {
        /** 点燃。 */
        BURN,
        /** 冰霜（减速 / 冻结）。 */
        FROST,
        /** 雷弧（雷击麻痹）。 */
        SHOCK,
        /** 定身（藤蔓缠绕）。 */
        BIND,
        /** 击退（被震开）。 */
        KNOCK
    }

    /**
     * <b>元素附带状态的视觉反馈</b>：点燃 / 冰霜 / 雷弧 / 定身 / 击退。
     *
     * <p>玩家反馈「法术命中后的元素附带状态只有数值、没有表现」。这里只补<b>表现</b>：
     * <b>不改任何战斗数值、状态时长、等级或概率</b> —— 状态本身仍由
     * {@code TechniqueCaster.applyElementSideEffect} 的原有逻辑结算，
     * 本方法只负责"让玩家看见它发生了"。</p>
     *
     * <h2>层数与预算</h2>
     * <p>每个状态 4 层：主形态 + 副形态 + 余韵 + 元素色光点，单目标一次
     * 约 48~56 颗（设计值，仍会被密度 / 境界倍率 / 全局令牌桶折算）。
     * 全部粒子都经 {@link #take} 预算闸门（这里的每个原语内部都先 take），
     * 并且用 {@link ParticleBatch} 合包，不逐颗 sendParticles。</p>
     *
     * <p>状态是按<b>命中目标</b>结算的：范围法术一次命中多个目标时，
     * 本方法会被调用多次，全局令牌桶会按比例摊薄，不会因为"怪多"而爆预算。</p>
     *
     * @param level   目标所在维度
     * @param at      目标脚底位置
     * @param element 触发状态的元素（只用于最后一层元素色光点，可为 null）
     * @param kind    状态视觉种类
     */
    public static void statusFx(ServerLevel level, Vec3 at, Element element, StatusFx kind) {
        if (level == null || at == null || kind == null) {
            return;
        }
        switch (kind) {
            case BURN -> statusBurnFx(level, at);
            case FROST -> statusFrostFx(level, at);
            case SHOCK -> statusShockFx(level, at);
            case BIND -> statusBindFx(level, at);
            case KNOCK -> statusKnockFx(level, at);
        }
        // 第 4 层：元素色余韵 —— 同一种状态在不同元素功法下也略有辨识度（≤6 颗）
        burst(level, at.add(0.0D, 1.0D, 0.0D), particleOf(element), 3, 0.42D, 0.02D);
    }

    /** 点燃：火星上窜 + 火环 + 余烬下落（约 54 颗/次）。 */
    private static void statusBurnFx(ServerLevel level, Vec3 at) {
        // 1) 火星上窜：沿躯干向上的一线 FLAME
        trail(level, at.add(0.0D, 0.15D, 0.0D), at.add(0.0D, 1.75D, 0.0D),
                ParticleTypes.FLAME, 10, 0.22D, 0.02D);
        // 2) 火环：脚下短促一圈 SMALL_FLAME，交代"整个人烧起来了"
        ring(level, at.add(0.0D, 0.10D, 0.0D), ParticleTypes.SMALL_FLAME,
                0.95D, 8, 0.16D, 0.02D);
        // 3) 余烬下落：LAVA 带原版重力，从胸口高度撒下会自己往下掉（补一点 SMOKE 余烟）
        burst(level, at.add(0.0D, 1.40D, 0.0D), ParticleTypes.LAVA, 5, 0.45D, 0.0D);
        burst(level, at.add(0.0D, 1.10D, 0.0D), ParticleTypes.SMOKE, 3, 0.35D, 0.02D);
    }

    /** 冰霜：冰晶球壳 + 雪花环 + 白雾下沉（约 52 颗/次）。 */
    private static void statusFrostFx(ServerLevel level, Vec3 at) {
        // 1) 冰晶球壳：贴着躯干的 ITEM_SNOWBALL 薄壳
        shell(level, at.add(0.0D, 0.95D, 0.0D), ParticleTypes.ITEM_SNOWBALL,
                0.78D, 7, 0.02D);
        // 2) 雪花环：脚下霜环
        ring(level, at.add(0.0D, 0.12D, 0.0D), ParticleTypes.SNOWFLAKE,
                1.05D, 9, 0.20D, 0.02D);
        // 3) 白雾下沉：脚边 CLOUD + 头顶 WHITE_ASH（后者缓缓落下）
        burst(level, at.add(0.0D, 0.25D, 0.0D), ParticleTypes.CLOUD, 5, 0.65D, 0.01D);
        burst(level, at.add(0.0D, 1.55D, 0.0D), ParticleTypes.WHITE_ASH, 4, 0.45D, 0.01D);
    }

    /** 雷弧：躯干折线电弧 + 一圈电火花 + 亮芯白闪（约 56 颗/次）。 */
    private static void statusShockFx(ServerLevel level, Vec3 at) {
        // 1) 电弧沿躯干折线：竖直方向左右抖动，像电流爬过身体（ParticleBatch 合包）
        int arc = take(level, 10);
        if (arc > 0) {
            ParticleBatch pb = new ParticleBatch(level);
            for (int i = 0; i < arc; i++) {
                double t = arc == 1 ? 0.0D : (double) i / (double) (arc - 1);
                double jitter = (i % 2 == 0) ? 0.22D : -0.22D;
                pb.add(ParticleTypes.ELECTRIC_SPARK, at.x + jitter,
                        at.y + 0.25D + t * 1.55D, at.z + jitter * 0.5D);
            }
            pb.flush();
        }
        // 2) 一圈电火花：脚下炸开
        ring(level, at.add(0.0D, 0.18D, 0.0D), ParticleTypes.ELECTRIC_SPARK,
                0.95D, 8, 0.24D, 0.03D);
        // 3) 亮芯 + 白闪：少量 END_ROD 竖线 + 2 颗 FLASH（不铺满屏）
        trail(level, at.add(0.0D, 0.30D, 0.0D), at.add(0.0D, 1.70D, 0.0D),
                ParticleTypes.END_ROD, 5, 0.10D, 0.0D);
        burst(level, at.add(0.0D, 1.20D, 0.0D), ParticleTypes.END_ROD, 2, 0.0D, 0.0D);
    }

    /** 定身：脚下藤环 + 上升孢子 + 少量绿色灵气（约 48 颗/次）。 */
    private static void statusBindFx(ServerLevel level, Vec3 at) {
        // 1) 脚下藤环：COMPOSTER 像泥土/藤蔓被踩起来，交代"被缠住"
        ring(level, at.add(0.0D, 0.12D, 0.0D), ParticleTypes.COMPOSTER,
                1.00D, 10, 0.08D, 0.02D);
        // 2) 上升孢子：贴着目标往上卷
        spiral(level, at, ParticleTypes.SPORE_BLOSSOM_AIR, 8, 0.55D, 1.8D);
        // 3) 绿色灵气点缀（HAPPY_VILLAGER 少用，免和治疗/增益读串）
        burst(level, at.add(0.0D, 0.85D, 0.0D), ParticleTypes.HAPPY_VILLAGER, 3, 0.45D, 0.01D);
    }

    /** 击退：脚下尘爆 + 向外 SWEEP_ATTACK 环 + 上冲烟柱（约 52 颗/次）。 */
    private static void statusKnockFx(ServerLevel level, Vec3 at) {
        // 1) 脚下尘爆：贴地被掀起来的浮尘
        burst(level, at.add(0.0D, 0.15D, 0.0D), ParticleTypes.POOF, 5, 0.60D, 0.06D);
        burst(level, at.add(0.0D, 0.20D, 0.0D), ParticleTypes.CLOUD, 4, 0.75D, 0.05D);
        // 2) 向外 SWEEP_ATTACK 环：把"被推开"的方向感画出来
        ring(level, at.add(0.0D, 0.30D, 0.0D), ParticleTypes.SWEEP_ATTACK,
                1.15D, 7, 0.06D, 0.06D);
        // 3) 上冲烟柱：撞击的"重量"
        trail(level, at.add(0.0D, 0.10D, 0.0D), at.add(0.0D, 2.10D, 0.0D),
                ParticleTypes.LARGE_SMOKE, 7, 0.26D, 0.04D);
    }
}
