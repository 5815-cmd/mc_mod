package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * <b>蓄力母题</b>：每个法术的蓄力各有特色，不是同一套模板。
 *
 * <h2>玩家要求</h2>
 * <p>「要每个法术的蓄力是有特色的不一样的，不是都是一样的模板」。</p>
 *
 * <h2>做法</h2>
 * <p>做八种**完全不同的母题**，按「形态 × 元素」指派：
 * 金→剑阵、雷→雷柱、火→天火、冰水→冰环、木→木藤、土→石阵、风→风旋、
 * 其余（含灭世无属性）→混沌曼荼罗。</p>
 *
 * <p>每个母题都只用公共工具（ring/grandRings/spiral/swirl/shell/burst/trail），
 * 所以自动享受境界分级倍率与蓄力预算放宽，且不越出"纯粒子、不动方块"的铁律。</p>
 */
public final class ChargeStyles {

    /**
     * 蓄力的**粒子预算倍率**（玩家要求：飞升起特效拉满）。
     *
     * <p>飞升(7) 起给最大值，之前的境界按档递减 —— 这样"飞升 = 特效完全解放"
     * 在观感上立刻能感觉到。</p>
     */
    public static double chargeBudget(int tier) {
        if (tier >= 7) {
            return 2.6D;
        }
        return switch (tier) {
            case 6 -> 1.9D;
            case 5 -> 1.5D;
            case 4 -> 1.2D;
            default -> 1.0D;
        };
    }

    /** 母题。 */
    public enum Style {
        SWORD_ARRAY, THUNDER_PILLAR, SKY_FIRE, FROST_RINGS,
        WOOD_VINES, STONE_STELES, WIND_VORTEX, CHAOS_MANDALA
    }

    private ChargeStyles() {
    }

    /**
     * 按「形态 + 元素」选母题。
     *
     * <p>形态优先级最高：斩击一定是剑阵（哪怕元素是金以外的），
     * 灭世级的无属性法术走混沌曼荼罗。</p>
     */
    public static Style pick(TechniqueKind kind, Element element) {
        if (kind == TechniqueKind.SLASH) {
            return Style.SWORD_ARRAY;
        }
        if (element == null) {
            return Style.CHAOS_MANDALA;
        }
        return switch (element) {
            case METAL -> Style.SWORD_ARRAY;
            case THUNDER -> Style.THUNDER_PILLAR;
            case FIRE -> Style.SKY_FIRE;
            case ICE, WATER -> Style.FROST_RINGS;
            case WOOD -> Style.WOOD_VINES;
            case EARTH -> Style.STONE_STELES;
            case WIND -> Style.WIND_VORTEX;
            case NONE -> Style.CHAOS_MANDALA;
        };
    }

    /** 中文名（面板/日志可用）。 */
    public static String cn(Style style) {
        return switch (style) {
            case SWORD_ARRAY -> "剑阵";
            case THUNDER_PILLAR -> "雷柱";
            case SKY_FIRE -> "天火";
            case FROST_RINGS -> "冰环";
            case WOOD_VINES -> "木藤";
            case STONE_STELES -> "石阵";
            case WIND_VORTEX -> "风旋";
            case CHAOS_MANDALA -> "混沌阵";
        };
    }

    /**
     * 按法术名取一个**固定变体**（0~3）。
     *
     * <p>用名字的哈希，所以同一个法术每次蓄力的形态一样，
     * 不同法术之间则明显不同 —— 30 个法术实际会有 8 母题 × 4 变体 = 最多 32 种组合。</p>
     */
    public static int variantOf(String key) {
        if (key == null) {
            return 0;
        }
        int h = key.hashCode();
        return Math.floorMod(h, 4);
    }

    /**
     * 画一次蓄力。
     *
     * @param p       蓄力进度 0→1
     * @param g       烈度（1.0 = 灭世级，越小越紧凑）
     * @param variant 固定变体 0~3（改旋转方向 / 层数 / 呼吸节奏）
     */
    public static void render(ServerLevel level, Vec3 c, Element element,
                              Style style, double p, double g, int variant) {
        double q = 0.55D + 0.45D * g;
        long t = level.getGameTime();
        // 三个旋钮：旋转方向 / 层数 / 呼吸节奏
        if (variant % 2 == 1) {
            t = -t;                                   // 反向旋转
        }
        int layers = 3 + Math.floorMod(variant, 4);   // 3~6 层
        // 呼吸节奏：0 慢扩 / 1 快缩 / 2 双拍 / 3 单向流
        double breathe = switch (Math.floorMod(variant, 4)) {
            case 1 -> 1.0D - Math.abs(Math.sin(p * Math.PI));
            case 2 -> Math.abs(Math.sin(p * Math.PI * 2.0D));
            case 3 -> p;
            default -> Math.abs(Math.sin(p * Math.PI * 0.5D));
        };
        p = Math.max(0.0D, Math.min(1.0D, p * (0.75D + 0.5D * breathe)));
        final int lv = 3 + Math.floorMod(variant, 4);   // 3~6 层
        ParticleOptions main = SpellVfx.particleOf(element);
        ParticleOptions accent = SpellVfx.accentOf(element);
        ParticleOptions aura = SpellVfx.auraOf(element);
        double R = (6.0D + p * 10.0D) * g;          // 基准阵盘半径

        switch (style) {
            case SWORD_ARRAY -> swordArray(level, c, main, accent, aura, p, g, q, R, t);
            case THUNDER_PILLAR -> thunderPillar(level, c, main, accent, aura, p, g, q, R, t);
            case SKY_FIRE -> skyFire(level, c, main, accent, aura, p, g, q, R, t);
            case FROST_RINGS -> frostRings(level, c, main, accent, aura, p, g, q, R, t, lv);
            case WOOD_VINES -> woodVines(level, c, main, accent, aura, p, g, q, R, t);
            case STONE_STELES -> stoneSteles(level, c, main, accent, aura, p, g, q, R, t);
            case WIND_VORTEX -> windVortex(level, c, main, accent, aura, p, g, q, R, t, lv);
            case CHAOS_MANDALA -> chaosMandala(level, c, main, accent, aura, p, g, q, R, t, lv);
        }
    }

    // ------------------------------------------------------------------
    // 八种母题
    // ------------------------------------------------------------------

    /** **剑阵**：一圈剑气刺竖直立起，交错转向中心。 */
    private static void swordArray(ServerLevel level, Vec3 c, ParticleOptions main,
                                   ParticleOptions accent, ParticleOptions aura,
                                   double p, double g, double q, double R, long t) {
        int blades = (int) (16 + p * 20);
                com.hongmeng.xiuxian.technique.ParticleBatch pb1 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < blades; i++) {
            double a = Math.PI * 2.0D * i / blades + t * 0.02D * (i % 2 == 0 ? 1 : -1);
            double x = c.x + Math.cos(a) * R;
            double z = c.z + Math.sin(a) * R;
            // 每一道剑气是一根竖直的短刺
            double h = (2.5D + p * 5.0D) * g;
            for (int k = 0; k < (int) (h * 2); k++) {
                pb1.add(k % 2 == 0 ? main : accent, x, c.y + 0.3D + k * 0.5D, z);
            }
        }
        pb1.flush();

        // 剑气底部阵盘 + 交错光环
        SpellVfx.ring(level, c.add(0.0D, 0.1D, 0.0D), main, R, (int) (90 * q), 0.04D, 0.0D);
        SpellVfx.grandRings(level, c, aura, 3, R * 0.85D, (int) (40 * q));
        // 向心的横斩（像是被牵引的剑意）
        for (int i = 0; i < (int) (26 * q); i++) {
            double a = Math.PI * 2.0D * i / Math.max(1, (int) (26 * q)) + t * 0.05D;
            Vec3 from = new Vec3(c.x + Math.cos(a) * R, c.y + 2.0D * g + p * 2.0D,
                    c.z + Math.sin(a) * R);
            SpellVfx.trail(level, from, c.add(0.0D, 1.2D, 0.0D), accent, 3, 0.03D, 0.0D);
        }
    }

    /** **雷柱**：四面雷柱交错劈落 + 头顶电弧乱窜。 */
    private static void thunderPillar(ServerLevel level, Vec3 c, ParticleOptions main,
                                      ParticleOptions accent, ParticleOptions aura,
                                      double p, double g, double q, double R, long t) {
        // 四面雷柱
        int posts = 4 + (int) (p * 4);
                com.hongmeng.xiuxian.technique.ParticleBatch pb2 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < posts; i++) {
            double a = Math.PI * 2.0D * i / posts + t * 0.04D;
            double x = c.x + Math.cos(a) * R;
            double z = c.z + Math.sin(a) * R;
            for (int k = 0; k < (int) (18 * g); k++) {
                pb2.add(k % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK : main, x, c.y + 18.0D * g - k * 1.0D, z);
            }
        }
        pb2.flush();

        // 头顶电弧乱窜
        int arcs = (int) ((10 + p * 14) * q);
        for (int i = 0; i < arcs; i++) {
            double a0 = level.random.nextDouble() * Math.PI * 2.0D;
            double a1 = a0 + (level.random.nextDouble() - 0.5D) * 1.6D;
            double hh = (5.0D + p * 8.0D) * g;
            Vec3 from = new Vec3(c.x + Math.cos(a0) * R * 0.9D, c.y + hh, c.z + Math.sin(a0) * R * 0.9D);
            Vec3 to = new Vec3(c.x + Math.cos(a1) * R * 0.9D, c.y + hh * 0.6D,
                    c.z + Math.sin(a1) * R * 0.9D);
            SpellVfx.trail(level, from, to, ParticleTypes.ELECTRIC_SPARK, 6, 0.10D, 0.02D);
        }
        SpellVfx.ring(level, c.add(0.0D, 0.12D, 0.0D), ParticleTypes.ELECTRIC_SPARK, R,
                (int) (100 * q), 0.06D, 0.0D);
        SpellVfx.grandRings(level, c, accent, 3, R, (int) (44 * q));
    }

    /** **天火**：火云在头顶翻卷、火点自天而降打在阵盘上。 */
    private static void skyFire(ServerLevel level, Vec3 c, ParticleOptions main,
                                ParticleOptions accent, ParticleOptions aura,
                                double p, double g, double q, double R, long t) {
        // 头顶火云盘
        int cloud = (int) ((90 + p * 90) * q);
                com.hongmeng.xiuxian.technique.ParticleBatch pb3 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < cloud; i++) {
            double a = Math.PI * 2.0D * i / cloud + t * 0.015D;
            double rr = R * (0.5D + 0.6D * ((i % 7) / 6.0D));
            pb3.add(i % 4 == 0 ? ParticleTypes.LARGE_SMOKE : main, c.x + Math.cos(a) * rr, c.y + (10.0D + p * 6.0D) * g, c.z + Math.sin(a) * rr);
        }
        pb3.flush();

        // 火点自天而降
                com.hongmeng.xiuxian.technique.ParticleBatch pb4 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < (int) (34 * q); i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            double rr = Math.sqrt(level.random.nextDouble()) * R;
            pb4.add(ParticleTypes.FALLING_LAVA, c.x + Math.cos(a) * rr, c.y + (11.0D + p * 6.0D) * g, c.z + Math.sin(a) * rr);
        }
        pb4.flush();

        // 阵盘被火点打出来的焦痕环
        SpellVfx.ring(level, c.add(0.0D, 0.12D, 0.0D), ParticleTypes.LAVA, R,
                (int) (90 * q), 0.05D, 0.0D);
        SpellVfx.grandRings(level, c, accent, 3, R * 0.9D, (int) (40 * q));
        SpellVfx.swirl(level, c.add(0.0D, 1.0D, 0.0D), aura, 3, (int) (50 * q), R * 0.5D);
    }

    /** **冰环**：多层冰环由外向内逐层收拢，霜花从地面长起。 */
    private static void frostRings(ServerLevel level, Vec3 c, ParticleOptions main,
                                   ParticleOptions accent, ParticleOptions aura,
                                   double p, double g, double q, double R, long t,
                                   int lv) {
        // 逐层收拢：每一层的半径随进度减小，相位错开
                com.hongmeng.xiuxian.technique.ParticleBatch pb5 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int L = 0; L < lv; L++) {
            double phase = (t * 0.02D + L * 0.2D) % 1.0D;
            double rr = R * (1.0D - phase * 0.75D);
            int n = (int) ((70 + p * 50) * q / lv * 2);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0D * i / n;
                pb5.add(L % 2 == 0 ? main : accent, c.x + Math.cos(a) * rr, c.y + 0.15D + L * 0.25D, c.z + Math.sin(a) * rr);
            }
        }
        pb5.flush();

        // 霜花：从地面向上长的短晶簇
        int spikes = (int) (14 + p * 16);
                com.hongmeng.xiuxian.technique.ParticleBatch pb6 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < spikes; i++) {
            double a = Math.PI * 2.0D * i / spikes;
            double rr = R * (0.4D + 0.5D * ((i % 3) / 2.0D));
            double x = c.x + Math.cos(a) * rr;
            double z = c.z + Math.sin(a) * rr;
            double h = (1.5D + p * 3.0D) * g;
            for (int k = 0; k < (int) (h * 2); k++) {
                pb6.add(ParticleTypes.SNOWFLAKE, x, c.y + 0.2D + k * 0.5D, z);
            }
        }
        pb6.flush();

        SpellVfx.grandRings(level, c, aura, 3, R * 0.8D, (int) (38 * q));
    }

    /** **木藤**：藤蔓从阵盘边缘向中心生长，叶片向上翻卷。 */
    private static void woodVines(ServerLevel level, Vec3 c, ParticleOptions main,
                                  ParticleOptions accent, ParticleOptions aura,
                                  double p, double g, double q, double R, long t) {
        // 藤蔓：从边缘向中心爬
        int vines = 12;
                com.hongmeng.xiuxian.technique.ParticleBatch pb7 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < vines; i++) {
            double a = Math.PI * 2.0D * i / vines + t * 0.01D;
            int seg = (int) ((8 + p * 16) * g);
            for (int k = 0; k < seg; k++) {
                double tt = (double) k / seg;
                double rr = R * (1.0D - tt * (0.35D + p * 0.55D));
                double yy = c.y + 0.2D + Math.sin(tt * Math.PI) * (2.2D + p * 2.0D) * g;
                pb7.add(k % 2 == 0 ? main : accent, c.x + Math.cos(a + tt * 0.9D) * rr, yy, c.z + Math.sin(a + tt * 0.9D) * rr);
            }
        }
        pb7.flush();

        // 叶片向上翻卷
                com.hongmeng.xiuxian.technique.ParticleBatch pb8 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < (int) (40 * q); i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            double rr = Math.sqrt(level.random.nextDouble()) * R * 0.8D;
            pb8.add(ParticleTypes.HAPPY_VILLAGER, c.x + Math.cos(a) * rr, c.y + 0.4D + p * 3.5D * g, c.z + Math.sin(a) * rr);
        }
        pb8.flush();

        SpellVfx.ring(level, c.add(0.0D, 0.1D, 0.0D), accent, R, (int) (80 * q), 0.05D, 0.0D);
        SpellVfx.grandRings(level, c, aura, 3, R * 0.85D, (int) (36 * q));
    }

    /** **石阵**：石笋从地面逐根升起，土块绕行。 */
    private static void stoneSteles(ServerLevel level, Vec3 c, ParticleOptions main,
                                    ParticleOptions accent, ParticleOptions aura,
                                    double p, double g, double q, double R, long t) {
        // 石笋：按进度逐根"升起"
        int total = 18;
        int up = (int) (total * (0.3D + p * 0.7D));
                com.hongmeng.xiuxian.technique.ParticleBatch pb9 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < total; i++) {
            double a = Math.PI * 2.0D * i / total;
            double rr = R * (0.45D + 0.5D * ((i % 4) / 3.0D));
            double x = c.x + Math.cos(a) * rr;
            double z = c.z + Math.sin(a) * rr;
            double h = i < up ? (1.5D + p * 3.5D) * g : 0.3D;
            for (int k = 0; k < (int) (h * 2); k++) {
                pb9.add(k < 2 ? ParticleTypes.LARGE_SMOKE : main, x, c.y + 0.2D + k * 0.5D, z);
            }
        }
        pb9.flush();

        // 土块绕行
                com.hongmeng.xiuxian.technique.ParticleBatch pb10 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < (int) (30 * q); i++) {
            double a = Math.PI * 2.0D * i / Math.max(1, (int) (30 * q)) + t * 0.03D;
            pb10.add(ParticleTypes.CRIT, c.x + Math.cos(a) * R * 1.05D, c.y + 0.6D + p * 1.5D, c.z + Math.sin(a) * R * 1.05D);
        }
        pb10.flush();

        SpellVfx.grandRings(level, c, accent, 3, R, (int) (38 * q));
        SpellVfx.shell(level, c.add(0.0D, 0.6D, 0.0D), aura, R * 0.6D, (int) (30 * q), 0.05D);
    }

    /** **风旋**：巨大旋风把地面颗粒卷上天，风眼在脚下。 */
    private static void windVortex(ServerLevel level, Vec3 c, ParticleOptions main,
                                   ParticleOptions accent, ParticleOptions aura,
                                   double p, double g, double q, double R, long t,
                                   int lv) {
        // 多层螺旋把颗粒卷上天
                com.hongmeng.xiuxian.technique.ParticleBatch pb11 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int L = 0; L < lv; L++) {
            double rr = R * (0.3D + 0.25D * L) * (0.8D + p * 0.5D);
            int n = (int) ((70 + p * 60) * q / lv * 2);
            double spin = t * (0.12D + 0.04D * L) * (L % 2 == 0 ? 1.0D : -1.0D);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0D * i / n + spin;
                double yy = c.y + 0.3D + L * (2.2D + p * 1.6D) * g;
                pb11.add(L % 2 == 0 ? main : ParticleTypes.CLOUD, c.x + Math.cos(a) * rr, yy, c.z + Math.sin(a) * rr);
            }
        }
        pb11.flush();

        // 风眼：脚下向上的气流
                com.hongmeng.xiuxian.technique.ParticleBatch pb12 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < (int) (36 * q); i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            double rr = level.random.nextDouble() * R * 0.35D;
            pb12.add(ParticleTypes.CLOUD, c.x + Math.cos(a) * rr, c.y + 0.4D + p * 5.0D * g, c.z + Math.sin(a) * rr);
        }
        pb12.flush();

        SpellVfx.ring(level, c.add(0.0D, 0.1D, 0.0D), accent, R, (int) (95 * q), 0.10D, 0.02D);
        SpellVfx.grandRings(level, c, aura, 3, R * 0.9D, (int) (40 * q));
    }

    /** **混沌阵**：多层曼荼罗反向旋转 + 符文浮升（灭世无属性默认）。 */
    private static void chaosMandala(ServerLevel level, Vec3 c, ParticleOptions main,
                                     ParticleOptions accent, ParticleOptions aura,
                                     double p, double g, double q, double R, long t,
                                   int lv) {
        // 曼荼罗：四层反向
                com.hongmeng.xiuxian.technique.ParticleBatch pb13 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int L = 0; L < lv; L++) {
            double rr = R * (0.35D + 0.22D * L);
            int n = (int) ((80 + p * 70) * q / lv * 2);
            double spin = t * 0.05D * (L % 2 == 0 ? 1.0D : -1.0D);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0D * i / n + spin;
                pb13.add(L == 1 ? accent : (L == 3 ? ParticleTypes.ENCHANT : aura), c.x + Math.cos(a) * rr, c.y + 0.15D + L * 0.22D, c.z + Math.sin(a) * rr);
            }
        }
        pb13.flush();

        // 符文浮升
                com.hongmeng.xiuxian.technique.ParticleBatch pb14 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < (int) (44 * q); i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            double rr = Math.sqrt(level.random.nextDouble()) * R * 0.9D;
            pb14.add(ParticleTypes.ENCHANT, c.x + Math.cos(a) * rr, c.y + 0.3D + p * 6.0D * g, c.z + Math.sin(a) * rr);
        }
        pb14.flush();

        SpellVfx.spiral(level, c, main, (int) ((60 + p * 100) * q),
                2.5D * g, (4.0D + p * 3.0D) * g);
        SpellVfx.grandRings(level, c, aura, 3, R, (int) (42 * q));
    }
}
