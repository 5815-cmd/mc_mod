package com.hongmeng.xiuxian.combat;

/**
 * 战斗手感（{@code docs/combat-feel-spec.md}）的**纯数据表**。
 *
 * <h2>为什么单独开一个类，而且一个 Minecraft 类都不引</h2>
 *
 * <p>规格第一节把顿帧的档位、第二节把震动的档位都写成了表。表如果散落在
 * 事件回调里，就没法离线断言"2% 以下不顿帧、25% 以上 6 tick"这种话。
 * 所以这里做成<b>零依赖的纯函数 + 纯常量</b>：不引 {@code Element}、
 * 不引 {@code ParticleTypes}、不引 {@code SoundEvents}（那三个都会碰注册表，
 * 离线验证器一加载就 {@code Not bootstrapped}）。</p>
 *
 * <p>于是可以直接离线跑（本仓库的探针 {@code _cache/combatprobe/CombatFeelProbe.java}
 * 就是这么做的，也是给 {@code src/verify} 里那几行 {@code check(...)} 用的同一套断言）。</p>
 *
 * <h2>离心率最高的两条约束</h2>
 *
 * <ol>
 *   <li><b>顿帧绝不暂停游戏 tick</b>：这里只算"几 tick"，冻结实体的事在
 *       {@code HitstopController} 里做（清速度 + 短暂免伤）。</li>
 *   <li><b>必须防叠死</b>：同一实体 {@value #HITSTOP_ANTI_STACK_TICKS} tick 内
 *       不得重复顿帧，否则平砍会变成永久僵直。判据也在这里
 *       （{@link #hitstopAllowed(long, long)}），同样是可断言的纯函数。</li>
 * </ol>
 */
public final class CombatFeel {

    private CombatFeel() {
    }

    // ==================================================================
    // 一、顿帧（hitstop）—— 规格 §一，单项收益最大的手法
    // ==================================================================

    /** 低于这个伤害占比不顿帧（否则平砍一直在卡）。 */
    public static final double HITSTOP_THRESHOLD = 0.02D;
    /** 2% ~ 10% → 2 tick 的上界。 */
    public static final double HITSTOP_MEDIUM = 0.10D;
    /** 10% ~ 25% → 4 tick 的上界；超过就是 6 tick。 */
    public static final double HITSTOP_HEAVY = 0.25D;

    /** 2% ~ 10%。 */
    public static final int HITSTOP_TICKS_SMALL = 2;
    /** 10% ~ 25%。 */
    public static final int HITSTOP_TICKS_MEDIUM = 4;
    /** &gt; 25%。 */
    public static final int HITSTOP_TICKS_HEAVY = 6;
    /** 单次顿帧的绝对上限（配置倍率再大也不会超过它）。 */
    public static final int HITSTOP_MAX_TICKS = HITSTOP_TICKS_HEAVY;

    /** 同一实体多少 tick 内不得重复顿帧。 */
    public static final int HITSTOP_ANTI_STACK_TICKS = 10;

    /**
     * 伤害占比 → 顿帧 tick 数。
     *
     * <pre>
     *   &lt; 2%        0   （不顿帧）
     *   2% ~ 10%    2
     *   10% ~ 25%   4
     *   &gt; 25%       6
     * </pre>
     *
     * <p>边界口径：<b>下界含、上界不含</b>——正好 10% 算 2 tick，10.01% 才算 4 tick；
     * 正好 25% 算 4 tick，25.01% 才算 6 tick。这样四档互不重叠、也不留缝隙，
     * 可以直接断言。NaN / 负数一律当 0（不顿帧）。</p>
     */
    public static int hitstopTicks(double damageFraction) {
        if (Double.isNaN(damageFraction) || damageFraction < HITSTOP_THRESHOLD) {
            return 0;
        }
        if (damageFraction <= HITSTOP_MEDIUM) {
            return HITSTOP_TICKS_SMALL;
        }
        if (damageFraction <= HITSTOP_HEAVY) {
            return HITSTOP_TICKS_MEDIUM;
        }
        return HITSTOP_TICKS_HEAVY;
    }

    /**
     * 把配置倍率乘进顿帧时长（然后夹回 0~{@value #HITSTOP_MAX_TICKS}）。
     *
     * <p>倍率是给整合包作者用的：{@code 0} = 完全关掉顿帧，{@code 2} = 全翻倍。</p>
     */
    public static int scaledHitstopTicks(double damageFraction, double scale) {
        int base = hitstopTicks(damageFraction);
        if (base <= 0 || scale <= 0.0D) {
            return 0;
        }
        int scaled = (int) Math.round(base * scale);
        if (scaled <= 0) {
            // 倍率很小但不能把"该有顿帧"变成"没有"：至少 1 tick
            return 1;
        }
        return Math.min(HITSTOP_MAX_TICKS * 2, scaled);
    }

    /** 伤害占目标最大生命的比例（目标血量为 0 时返回 0）。 */
    public static double damageFraction(float damage, float maxHealth) {
        if (maxHealth <= 0.0F || damage <= 0.0F || Float.isNaN(damage)) {
            return 0.0D;
        }
        return damage / maxHealth;
    }

    /**
     * 防叠死：同一实体要间隔 {@value #HITSTOP_ANTI_STACK_TICKS} tick 才允许再顿帧。
     *
     * @param now      当前游戏刻
     * @param lastTick 这只实体上次被顿帧的刻（没顿过传 {@link Long#MIN_VALUE}）
     */
    public static boolean hitstopAllowed(long now, long lastTick) {
        if (lastTick == Long.MIN_VALUE) {
            return true;
        }
        return now - lastTick >= HITSTOP_ANTI_STACK_TICKS;
    }

    // ==================================================================
    // 二、屏幕震动 —— 规格 §二
    // ==================================================================

    /**
     * 震动档位。
     *
     * <p>伤害占比只能走到 {@link #MEDIUM}（大招），{@link #HEAVY} 与
     * {@link #CATASTROPHIC} 留给"领域爆发 / 雷劫 / 首领登场"这类事件——
     * 规格第二节的表格就是这么分的。</p>
     */
    /**
     * <b>振幅已按玩家反馈压低到约 1/3。</b>
     *
     * <p>玩家先后两次反馈「打怪屏幕震动」——原来的 0.8 / 2.5 / 4.5 / 6.0 度虽然
     * 单次数值不大，但<b>连续攻击会不断叠加</b>，一直晃很晕。
     * 现在打击感交给顿帧 + FOV 冲击 + 音效 + 粒子，镜头尽量别动。</p>
     */
    public enum Shake {
        /** 不震。 */
        NONE(0.0F, 0),
        /** 普通命中：0.5~1 度 / 2 tick。 */
        LIGHT(0.22F, 2),
        /** 大招命中：2~3 度 / 4 tick。 */
        MEDIUM(0.80F, 3),
        /** 领域爆发 / 雷劫：4~6 度 / 8~10 tick。 */
        HEAVY(1.50F, 5),
        /** 首领登场：最强一档。 */
        CATASTROPHIC(2.20F, 7);

        private final float degrees;
        private final int ticks;

        Shake(float degrees, int ticks) {
            this.degrees = degrees;
            this.ticks = ticks;
        }

        /** 最大偏移（度）。 */
        public float degrees() {
            return this.degrees;
        }

        /** 持续 tick 数。 */
        public int ticks() {
            return this.ticks;
        }

        public boolean isNone() {
            return this == NONE;
        }
    }

    /** 伤害占比 → 震动档位（普通命中 / 大招命中两档；再往上由事件决定）。 */
    public static Shake shakeForDamage(double damageFraction) {
        if (Double.isNaN(damageFraction) || damageFraction < HITSTOP_THRESHOLD) {
            return Shake.NONE;
        }
        if (damageFraction <= HITSTOP_MEDIUM) {
            return Shake.LIGHT;
        }
        if (damageFraction <= HITSTOP_HEAVY) {
            return Shake.MEDIUM;
        }
        // 25% 以上仍属"大招命中"档，但振幅吃满（见 shakeStrengthMultiplier）
        return Shake.MEDIUM;
    }

    /**
     * 档位内部的振幅倍率。
     *
     * <p>规格给的是<b>区间</b>（普通命中 0.5~1 度、大招命中 2~3 度），
     * 所以档位只决定"哪一档"，具体落在区间的哪一端由伤害占比决定：</p>
     *
     * <pre>
     *   &lt;2%        0      （不震）
     *   2%~10%     0.90   （LIGHT  0.8° → 0.72°，落在 0.5~1 区间内）
     *   10%~25%    1.00   （MEDIUM 2.5° → 2.5°）
     *   &gt;25%       1.20   （MEDIUM 2.5° → 3.0°，吃到区间上界）
     * </pre>
     */
    public static double shakeStrengthMultiplier(double damageFraction) {
        if (Double.isNaN(damageFraction) || damageFraction < HITSTOP_THRESHOLD) {
            return 0.0D;
        }
        if (damageFraction <= HITSTOP_MEDIUM) {
            return 0.90D;
        }
        return damageFraction <= HITSTOP_HEAVY ? 1.00D : 1.20D;
    }

    /**
     * 衰减曲线：{@code strength * (1 - t/duration)}。
     *
     * <p>规格明确要求"用衰减曲线，不是突然停"。到 {@code elapsed >= duration}
     * 返回 0（也就是自然结束）。</p>
     */
    public static float decay(float amplitude, int elapsed, int duration) {
        if (amplitude <= 0.0F || duration <= 0) {
            return 0.0F;
        }
        if (elapsed <= 0) {
            return amplitude;
        }
        if (elapsed >= duration) {
            return 0.0F;
        }
        return amplitude * (1.0F - (float) elapsed / (float) duration);
    }

    /**
     * 某一帧某根轴上的震动偏移（度）。
     *
     * <p>纯函数：{@code sin(相位) × 衰减}。相位由 {@code elapsed}、
     * {@code seed}（每次命中换一个，避免"每次都一模一样"）与 {@code axis}
     * （0=yaw / 1=pitch / 2=roll）共同决定。</p>
     *
     * <p>写成纯函数的好处：离线就能断言"第一帧振幅最大、最后一帧归零、
     * 且任何一帧都不超过档位上限"。</p>
     */
    public static float shakeOffset(float amplitude, int elapsed, int duration, int seed, int axis) {
        float a = decay(amplitude, elapsed, duration);
        if (a <= 0.0F) {
            return 0.0F;
        }
        double phase = elapsed * 1.7D + seed * 0.37D + axis * 2.1D;
        return (float) (Math.sin(phase) * a);
    }

    /** 震动强度倍率作用到档位上之后的实际振幅（配置可调）。 */
    public static float shakeAmplitude(Shake tier, double multiplier) {
        if (tier == null || tier.isNone() || multiplier <= 0.0D) {
            return 0.0F;
        }
        return (float) (tier.degrees() * multiplier);
    }

    // ==================================================================
    // 三、击退 —— 规格第一节第 4 条"物理上的打飞了"
    // ==================================================================

    /** 最小水平击退（2% 那一档）。 */
    public static final double KNOCKBACK_MIN = 0.30D;
    /** 最大水平击退（25% 及以上）。 */
    public static final double KNOCKBACK_MAX = 1.10D;
    /** 到这个伤害占比，击退就吃满了。 */
    public static final double KNOCKBACK_FULL_FRACTION = 0.25D;

    /** 水平击退冲量（格/tick）。 */
    public static double knockbackHorizontal(double damageFraction) {
        double t = clamp01(damageFraction / KNOCKBACK_FULL_FRACTION);
        return KNOCKBACK_MIN + (KNOCKBACK_MAX - KNOCKBACK_MIN) * t;
    }

    /** 垂直击退冲量（格/tick）：大伤害会把人"掀起来"。 */
    public static double knockbackVertical(double damageFraction) {
        double t = clamp01(damageFraction / KNOCKBACK_FULL_FRACTION);
        return 0.10D + 0.32D * t;
    }

    // ==================================================================
    // 四、粒子预算 —— 规格 §零 的硬约束 1
    // ==================================================================

    /**
     * 每个维度每 tick 的粒子上限（默认值，配置可调）。
     *
     * <p>规格原话："华丽特效如果每 tick 喷 2000 粒子，mod 直接没法玩"。
     * 400 是"满屏大战也不掉帧"的量级——一次普通命中十几颗、
     * 一次大招三四十颗，够 10 场同时打。</p>
     */
    public static final int PARTICLE_BUDGET_PER_TICK = 400;

    /** 单次命中的放射爆发硬上限（再华丽也不许一颗命中吃掉整个预算）。 */
    public static final int BURST_HARD_CAP = 48;

    /** 单次特效调用的硬上限。 */
    public static final int SINGLE_CALL_CAP = 64;

    /**
     * 一次命中的放射爆发粒子数（已经乘过密度倍率，并夹在 [0, {@value #BURST_HARD_CAP}]）。
     *
     * <p>基准：{@code <2% → 6}、{@code 2%~10% → 12}、{@code 10%~25% → 24}、
     * {@code >25% → 36}。</p>
     */
    public static int burstParticles(double damageFraction, double density) {
        int base;
        if (Double.isNaN(damageFraction) || damageFraction < HITSTOP_THRESHOLD) {
            base = 6;
        } else if (damageFraction <= HITSTOP_MEDIUM) {
            base = 12;
        } else if (damageFraction <= HITSTOP_HEAVY) {
            base = 24;
        } else {
            base = 36;
        }
        return clampCount(base, density, BURST_HARD_CAP);
    }

    /** 前摇（起）粒子数：蓄力越久越多，上限 48。 */
    public static int windupParticles(int castTicks, double density) {
        int base = 8 + Math.max(0, Math.min(40, castTicks)) / 2;
        return clampCount(base, density, 48);
    }

    /** 拖尾（承）沿途每一步的粒子数。 */
    public static int trailParticles(double density) {
        return clampCount(2, density, 8);
    }

    /** 余韵（合）每一缕的粒子数。 */
    public static int afterglowPuff(double density) {
        return clampCount(3, density, 10);
    }

    /** 余韵持续 tick：小命中 20、大招 60。 */
    public static int afterglowTicks(double damageFraction) {
        if (damageFraction <= HITSTOP_MEDIUM) {
            return 20;
        }
        return damageFraction <= HITSTOP_HEAVY ? 40 : 60;
    }

    /** 把"想要的粒子数"夹进配置倍率与硬上限。 */
    public static int clampCount(int base, double density, int hardCap) {
        if (base <= 0 || density <= 0.0D) {
            return 0;
        }
        int n = (int) Math.round(base * density);
        if (n <= 0) {
            return 0;
        }
        return Math.min(n, hardCap);
    }

    /**
     * 按本 tick 剩余预算夹一次（服务端 {@code ParticleBudget} 会再记账）。
     */
    public static int clampToBudget(int requested, int remainingThisTick) {
        if (requested <= 0 || remainingThisTick <= 0) {
            return 0;
        }
        return Math.min(Math.min(requested, SINGLE_CALL_CAP), remainingThisTick);
    }

    /** 一个维度要多少 tick 才把预算攒满（诊断用；预算是"每 tick 重置"的）。 */
    public static int budgetPerSecond() {
        return PARTICLE_BUDGET_PER_TICK * 20;
    }

    // ==================================================================
    // 五、压迫感 —— 规格 §3.1 境界压制
    // ==================================================================

    /**
     * 境界差 → 压迫强度（0~1）。
     *
     * <pre>
     *   差 0 及以下   0.00
     *   差 1          0.25
     *   差 2          0.45
     *   差 3          0.62
     *   差 4          0.80
     *   差 ≥5         1.00
     * </pre>
     *
     * <p>刻意与 {@code MobRank.SUPPRESSION_BY_GAP}（减伤表）<b>同调</b>：
     * 减伤 20% 对应 vignette 0.25、减伤 70% 对应满强度。
     * 规格说这两件事是"同一枚硬币的两面"，那就让两条曲线一起涨。</p>
     */
    public static final float[] OPPRESSION_BY_GAP = {
            0.00F, 0.25F, 0.45F, 0.62F, 0.80F, 1.00F
    };

    /** 玩家境界低于怪物境界时的压迫强度。 */
    public static float oppressionIntensity(int playerTier, int monsterTier) {
        int gap = monsterTier - playerTier;
        if (gap <= 0) {
            return 0.0F;
        }
        return OPPRESSION_BY_GAP[Math.min(gap, OPPRESSION_BY_GAP.length - 1)];
    }

    /** 距离衰减：贴脸 1.0，到 {@code range} 格时剩 {@code minFactor}。 */
    public static float distanceFalloff(double distance, double range, float minFactor) {
        if (range <= 0.0D) {
            return 0.0F;
        }
        float t = (float) clamp01(distance / range);
        return 1.0F + (minFactor - 1.0F) * t;
    }

    /** vignette 最大不透明度（再强也不会把画面糊黑，玩家还要看得见敌人）。 */
    // 原来 0.62：太浓，配合厚边带会把画面糊掉（玩家反馈「红边太大遮挡视野」）。
    // 0.30 足够表达被压制，又不影响看清敌人。
    public static final float VIGNETTE_ALPHA_MAX = 0.30F;

    /** 压迫强度 → vignette 不透明度。 */
    public static float vignetteAlpha(float intensity) {
        return VIGNETTE_ALPHA_MAX * clamp01(intensity);
    }

    /** 心跳最快 / 最慢间隔（tick）。 */
    public static final int HEARTBEAT_MIN_INTERVAL = 16;
    /** 心跳最慢间隔：越被压制跳得越快。 */
    public static final int HEARTBEAT_MAX_INTERVAL = 40;

    /**
     * 心跳间隔（tick）；压迫强度为 0 时返回 0 表示"不跳"。
     */
    public static int heartbeatIntervalTicks(float intensity) {
        if (intensity <= 0.05F) {
            return 0;
        }
        float t = clamp01(intensity);
        return (int) Math.round(HEARTBEAT_MAX_INTERVAL
                - (HEARTBEAT_MAX_INTERVAL - HEARTBEAT_MIN_INTERVAL) * t);
    }

    // ==================================================================
    // 六、强者登场 —— 规格 §3.2
    // ==================================================================

    /** 天空变暗持续 tick。 */
    public static final int ENTRANCE_DARKEN_TICKS = 70;
    /** 名牌横幅滑下 + 停留的总时长。 */
    public static final int ENTRANCE_BANNER_TICKS = 100;
    /** 横幅"压下来"用了多少 tick（之后停在那儿）。 */
    public static final int ENTRANCE_SLAM_TICKS = 12;
    /** 触发登场的玩家距离（格）。 */
    public static final double ENTRANCE_RADIUS = 48.0D;
    /** 天空变暗的最大不透明度。 */
    public static final float ENTRANCE_DARKEN_MAX = 0.55F;

    /**
     * 天空变暗的透明度曲线：**快进慢出**。
     *
     * <p>前 1/5 时间冲到最大（"天一下就黑了"），后 4/5 缓慢退去
     * （"黑云还没散"）——这是"压迫感"与"闪一下"的区别。</p>
     */
    public static float skyDarkenAlpha(int elapsed, int total) {
        if (total <= 0 || elapsed < 0 || elapsed >= total) {
            return 0.0F;
        }
        float t = (float) elapsed / (float) total;
        if (t < 0.2F) {
            return ENTRANCE_DARKEN_MAX * (t / 0.2F);
        }
        float out = (t - 0.2F) / 0.8F;
        return ENTRANCE_DARKEN_MAX * (1.0F - out);
    }

    /**
     * 横幅纵坐标（缓出）：从 {@code fromY} 砸到 {@code toY}，之后停在 {@code toY}。
     */
    public static float bannerY(int elapsed, int slamTicks, float fromY, float toY) {
        if (slamTicks <= 0 || elapsed >= slamTicks) {
            return toY;
        }
        if (elapsed <= 0) {
            return fromY;
        }
        float t = (float) elapsed / (float) slamTicks;
        float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);   // easeOutCubic
        return fromY + (toY - fromY) * eased;
    }

    // ==================================================================
    // 七、伤害飘字 / FOV 冲击
    // ==================================================================

    /** 飘字存活 tick。 */
    public static final int DAMAGE_NUMBER_TICKS = 24;

    /** 飘字上浮的高度（格）：前 60% 上浮，之后减速停住。 */
    public static float damageNumberRise(int elapsed) {
        if (elapsed <= 0) {
            return 0.0F;
        }
        float t = clamp01((float) elapsed / DAMAGE_NUMBER_TICKS);
        return 0.85F * (1.0F - (1.0F - t) * (1.0F - t));
    }

    /** 飘字透明度：后 1/4 淡出。 */
    public static float damageNumberAlpha(int elapsed) {
        float t = clamp01((float) elapsed / DAMAGE_NUMBER_TICKS);
        if (t < 0.75F) {
            return 1.0F;
        }
        return 1.0F - (t - 0.75F) / 0.25F;
    }

    /** 伤害占比 → FOV 冲击幅度（比例，0 表示不冲）。 */
    public static float fovPunch(double damageFraction) {
        if (damageFraction < HITSTOP_THRESHOLD) {
            return 0.0F;
        }
        if (damageFraction <= HITSTOP_MEDIUM) {
            return 0.04F;
        }
        return damageFraction <= HITSTOP_HEAVY ? 0.08F : 0.12F;
    }

    /** FOV 冲击的持续 tick。 */
    public static final int FOV_PUNCH_TICKS = 5;

    // ==================================================================
    // 工具
    // ==================================================================

    /** 夹到 0~1。 */
    public static float clamp01(float v) {
        if (Float.isNaN(v)) {
            return 0.0F;
        }
        return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
    }

    /** 夹到 0~1（double）。 */
    public static double clamp01(double v) {
        if (Double.isNaN(v)) {
            return 0.0D;
        }
        return v < 0.0D ? 0.0D : (v > 1.0D ? 1.0D : v);
    }
}
