package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * <b>每个法术的视觉签名</b>：独立粒子调色板 + 独立蓄力图案组合。
 *
 * <h2>玩家要求</h2>
 * <ul>
 *   <li>「粒子效果可以比这少一点但花样要多，不一定是堆砌粒子，可以是多重花样」；</li>
 *   <li>「大部分蓄力的特效都一样」→ 每个法术要明显不同。</li>
 * </ul>
 *
 * <h2>做法</h2>
 * <p>原来蓄力母题按<b>元素</b>分，所以同元素的 9 个法术长得一样。现在两层去重：</p>
 * <ol>
 *   <li><b>调色板按元素 + 名字错开</b>：四档（主/副/光晕/残迹）在元素基色上
 *       按名字哈希整体偏移一位，同元素的不同法术粒子也不同；</li>
 *   <li><b>图案按名字组合</b>：从八种图案里挑三种叠加，
 *       组合数 C(8,3)×4×2×4 = <b>896</b>，每个法术一套。</li>
 * </ol>
 *
 * <p>粒子总量比上一版<b>降低</b>（倍率 3.0 → 1.6、基数 ×0.6）——
 * 靠花样而不是靠堆量。</p>
 */
public final class SpellSignature {

    /** 蓄力图案。 */
    public enum Motif {
        RING, SPIKE, PILLAR, RAIN, SPIRAL, VINE, FUNNEL, GLYPH
    }

    private SpellSignature() {
    }

    // ------------------------------------------------------------------
    // 一、粒子调色板
    // ------------------------------------------------------------------

    /** 主粒子（最显眼的那一层）。 */
    public static ParticleOptions core(Element e, String key) {
        int v = shift(key);
        return switch (e == null ? Element.NONE : e) {
            case METAL -> pick(v, ParticleTypes.WAX_ON, ParticleTypes.END_ROD,
                    ParticleTypes.CRIT, ParticleTypes.ELECTRIC_SPARK);
            case WOOD -> pick(v, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.COMPOSTER,
                    ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.CHERRY_LEAVES);
            case WATER -> pick(v, ParticleTypes.SPLASH, ParticleTypes.BUBBLE,
                    ParticleTypes.DRIPPING_WATER, ParticleTypes.FALLING_WATER);
            case FIRE -> pick(v, ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME,
                    ParticleTypes.LAVA, ParticleTypes.SOUL_FIRE_FLAME);
            case EARTH -> pick(v, ParticleTypes.CRIT, ParticleTypes.ASH,
                    ParticleTypes.POOF, ParticleTypes.LARGE_SMOKE);
            case ICE -> pick(v, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL,
                    ParticleTypes.WHITE_ASH, ParticleTypes.CLOUD);
            case THUNDER -> pick(v, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD,
                    ParticleTypes.END_ROD, ParticleTypes.GLOW);
            case WIND -> pick(v, ParticleTypes.CLOUD, ParticleTypes.WHITE_ASH,
                    ParticleTypes.POOF, ParticleTypes.SPORE_BLOSSOM_AIR);
            case NONE -> pick(v, ParticleTypes.ENCHANT, ParticleTypes.SCULK_SOUL,
                    ParticleTypes.PORTAL, ParticleTypes.END_ROD);
        };
    }

    /** 副粒子（第二层，和主粒子形成对比）。 */
    public static ParticleOptions accent(Element e, String key) {
        int v = shift(key) + 1;
        return switch (e == null ? Element.NONE : e) {
            case METAL -> pick(v, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.SCRAPE,
                    ParticleTypes.WAX_ON, ParticleTypes.END_ROD);
            case WOOD -> pick(v, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.COMPOSTER,
                    ParticleTypes.CHERRY_LEAVES, ParticleTypes.HAPPY_VILLAGER);
            case WATER -> pick(v, ParticleTypes.BUBBLE_POP, ParticleTypes.SPLASH,
                    ParticleTypes.FALLING_WATER, ParticleTypes.DRIPPING_WATER);
            case FIRE -> pick(v, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.LAVA,
                    ParticleTypes.SMALL_FLAME, ParticleTypes.FLAME);
            case EARTH -> pick(v, ParticleTypes.POOF, ParticleTypes.LARGE_SMOKE,
                    ParticleTypes.ASH, ParticleTypes.CRIT);
            case ICE -> pick(v, ParticleTypes.ITEM_SNOWBALL, ParticleTypes.WHITE_ASH,
                    ParticleTypes.SNOWFLAKE, ParticleTypes.CLOUD);
            case THUNDER -> pick(v, ParticleTypes.END_ROD, ParticleTypes.GLOW,
                    ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD);
            case WIND -> pick(v, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.POOF,
                    ParticleTypes.CLOUD, ParticleTypes.WHITE_ASH);
            case NONE -> pick(v, ParticleTypes.PORTAL, ParticleTypes.REVERSE_PORTAL,
                    ParticleTypes.SCULK_SOUL, ParticleTypes.ENCHANT);
        };
    }

    /** 光晕粒子（范围层，用体积大的）。 */
    public static ParticleOptions aura(Element e, String key) {
        int v = shift(key) + 2;
        return switch (e == null ? Element.NONE : e) {
            case METAL, THUNDER -> pick(v, ParticleTypes.END_ROD, ParticleTypes.GLOW);
            case WOOD, EARTH -> pick(v, ParticleTypes.COMPOSTER, ParticleTypes.POOF);
            case WATER, ICE -> pick(v, ParticleTypes.CLOUD, ParticleTypes.SNOWFLAKE);
            case FIRE -> pick(v, ParticleTypes.LARGE_SMOKE, ParticleTypes.FLAME);
            case WIND -> pick(v, ParticleTypes.WHITE_ASH, ParticleTypes.CLOUD);
            case NONE -> pick(v, ParticleTypes.ENCHANT, ParticleTypes.PORTAL);
        };
    }

    /** 残迹粒子（拖尾/留痕）。 */
    public static ParticleOptions residue(Element e, String key) {
        int v = shift(key) + 3;
        return switch (e == null ? Element.NONE : e) {
            case METAL, THUNDER -> pick(v, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CRIT);
            case WOOD, EARTH -> pick(v, ParticleTypes.ASH, ParticleTypes.CRIMSON_SPORE);
            case WATER, ICE -> pick(v, ParticleTypes.DRIPPING_WATER, ParticleTypes.WHITE_ASH);
            case FIRE -> pick(v, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.ASH);
            case WIND -> pick(v, ParticleTypes.POOF, ParticleTypes.CLOUD);
            case NONE -> pick(v, ParticleTypes.SCULK_SOUL, ParticleTypes.WITCH);
        };
    }

    /**
     * 调色板偏移：<b>优先读视觉定妆表</b>（{@link SpellLook}），
     * 未登记的法术才退回名字哈希 —— 这样"改一门的配色"只改表里一行。
     */
    private static int shift(String key) {
        if (key == null) {
            return 0;
        }
        SpellLook.Look look = SpellLook.byKey(key);
        return look != null ? look.paletteShift() : Math.floorMod(key.hashCode(), 4);
    }

    private static ParticleOptions pick(int i, ParticleOptions... arr) {
        return arr[Math.floorMod(i, arr.length)];
    }

    // ------------------------------------------------------------------
    // 二、蓄力图案组合
    // ------------------------------------------------------------------

    /**
     * 这个法术用了哪三种图案。
     *
     * <p><b>优先读视觉定妆表</b>（{@link SpellLook}）—— 图案是按法术气质人工挑的
     * （剑法配尖刺/光柱、冰牢配冰环/雨、治疗配藤/环），不是哈希抽签；
     * 未登记的法术才退回旧的名字哈希。</p>
     */
    public static Motif[] motifsOf(String key) {
        SpellLook.Look look = SpellLook.byKey(key);
        if (look != null) {
            return look.motifs().clone();
        }
        Motif[] all = Motif.values();
        int h = key == null ? 0 : key.hashCode();
        int a = Math.floorMod(h, all.length);
        int b = Math.floorMod(h / 7 + 3, all.length - 1);
        int c = Math.floorMod(h / 13 + 5, all.length - 2);
        // 在"去掉已选"的序列里取 b、c，保证三个互不相同
        Motif m1 = all[a];
        Motif[] rest1 = remove(all, a);
        Motif m2 = rest1[b % rest1.length];
        Motif[] rest2 = remove(rest1, indexOf(rest1, m2));
        Motif m3 = rest2[c % rest2.length];
        return new Motif[]{m1, m2, m3};
    }

    private static Motif[] remove(Motif[] src, int idx) {
        Motif[] out = new Motif[src.length - 1];
        int k = 0;
        for (int i = 0; i < src.length; i++) {
            if (i != idx) {
                out[k++] = src[i];
            }
        }
        return out;
    }

    private static int indexOf(Motif[] arr, Motif m) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == m) {
                return i;
            }
        }
        return 0;
    }

    /** 这个法术的蓄力层数（3~6），优先读定妆表。 */
    public static int layersOf(String key) {
        SpellLook.Look look = SpellLook.byKey(key);
        if (look != null) {
            return look.chargeLayers();
        }
        return 3 + Math.floorMod(key == null ? 0 : key.hashCode(), 4);
    }

    /** 旋转方向：+1 顺时针 / -1 逆时针，优先读定妆表。 */
    public static int spinOf(String key) {
        SpellLook.Look look = SpellLook.byKey(key);
        if (look != null) {
            return look.spin();
        }
        return Math.floorMod(key == null ? 0 : key.hashCode(), 2) == 0 ? 1 : -1;
    }

    /**
     * 画一次蓄力：把该法术的三种图案叠起来。
     *
     * @param p 进度 0→1
     * @param g 烈度（越大的法术范围越大）
     */
    public static void charge(ServerLevel level, Vec3 c, Element element, String key,
                              double p, double g) {
        // ---- 2026-11-01 第三轮：蓄力粒子**减量**；2026-11-07 再下调一档 ----
        //
        // 原来每 2 tick 每门法术撒 300+ 颗，屏幕糊成一片、还挡视野。
        // 现在 q 系数 0.60 → **0.19**（连同上轮各图案内基数调整，累计约 −65%），
        // 华丽感交给每 12 tick 补一圈的**立体法阵几何**（SpellFxRuntime.chargeSigil），
        // 粒子只做"有东西在聚"的提示。
        double q = (0.55D + 0.45D * g) * 0.15D;
        long t = level.getGameTime() * spinOf(key);
        ParticleOptions core = core(element, key);
        ParticleOptions accent = accent(element, key);
        ParticleOptions aura = aura(element, key);
        ParticleOptions residue = residue(element, key);
        double R = (3.5D + p * 5.5D) * g;
        int lv = layersOf(key);

        for (Motif m : motifsOf(key)) {
            switch (m) {
                case RING -> rings(level, c, core, accent, aura, p, g, q, R, t, lv);
                case SPIKE -> spikes(level, c, core, accent, p, g, q, R, t, lv);
                case PILLAR -> pillars(level, c, core, accent, p, g, q, R, t, lv);
                case RAIN -> rain(level, c, core, residue, p, g, q, R);
                case SPIRAL -> spirals(level, c, core, accent, p, g, q, R, t, lv);
                case VINE -> vines(level, c, core, accent, p, g, q, R, t, lv);
                case FUNNEL -> funnel(level, c, core, accent, aura, p, g, q, R);
                case GLYPH -> glyphs(level, c, core, aura, p, g, q, R, t);
            }
        }
        // 专属点缀：按定妆表里的"专属演出"再叠一层气质相符的蓄力表现
        SpellLook.Look look = SpellLook.byKey(key);
        if (look != null) {
            chargeAccent(level, c, element, look, p, g, q, R, t);
        }
        // 满蓄：一圈爆闪（所有法术都有，作为"可以放了"的信号）
        // 落点从胸口（1.2）抬到头顶上方（2.9），第一人称不再糊脸。
        if (p > 0.90D) {
            SpellVfx.grandRings(level, c, ParticleTypes.END_ROD, 2, R * 1.2D,
                    (int) (16 * q * 3));
            SpellVfx.burst(level, c.add(0.0D, 2.9D, 0.0D), core, (int) (32 * q * 3),
                    1.5D * g, 0.35D);
        }
    }

    /**
     * <b>蓄力专属点缀</b>：在通用三图案之外，再按法术的"专属演出"补 1~2 层。
     *
     * <h2>为什么还要这一层</h2>
     * <p>三图案的组合已经能保证"不重复"，但同一套图案喂给剑法和冰牢，
     * 气质仍然会糊在一起。这一层让<b>蓄力就能读出这是哪一类法术</b>：</p>
     * <ul>
     *   <li>贯穿类：指尖向上收束的一线光；</li>
     *   <li>剑阵/剑域：一圈朝外的剑芒；</li>
     *   <li>冰牢：贴地雪环；雷牢：向上电弧；火海：脚下火环；</li>
     *   <li>震地：沙尘环；摄魂/血池：向内收的雾；遁术：环绕残影；</li>
     *   <li>天陨：高空预热；斩星：天顶光柱；轨道：头顶充电环。</li>
     * </ul>
     *
     * <p>全部走 {@link SpellVfx} 的既有原语（自带粒子预算闸门），不新增协议、
     * 不改客户端 —— 客户端零改动就能看到变化。</p>
     */
    private static void chargeAccent(ServerLevel level, Vec3 c, Element element,
                                     SpellLook.Look look, double p, double g,
                                     double q, double R, long t) {
        ParticleOptions main = core(element, look.key());
        ParticleOptions ac = accent(element, look.key());
        ParticleOptions au = aura(element, look.key());
        int n = Math.max(3, (int) (6 * q * 2.0D));
        switch (look.setpiece()) {
            case PIERCE_LINE -> {
                // 贯穿：一线向天收束 + 末端亮点（"凝气于指"）
                double h = 2.4D * g + p * 2.2D;
                SpellVfx.trail(level, c, c.add(0.0D, h, 0.0D), main, n, 0.05D, 0.0D);
                SpellVfx.burst(level, c.add(0.0D, h, 0.0D), ac, n / 2, 0.22D, 0.02D);
            }
            case SWORD_FAN, GOLD_DOMAIN -> {
                // 剑阵/剑域：一圈朝外的剑芒（再减量：6 → 4 道）
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI * 2.0D * i / 4.0D + t * 0.02D;
                    Vec3 foot = c.add(Math.cos(a) * R * 0.9D, 0.4D, Math.sin(a) * R * 0.9D);
                    SpellVfx.trail(level, foot,
                            foot.add(0.0D, 1.6D * g + p * 1.6D, 0.0D), ac, 3, 0.10D, 0.0D);
                }
            }
            case FROST_PRISON -> // 冰牢：贴地雪环
                    SpellVfx.grandRings(level, c, ac, 2, R, Math.max(4, (int) (3 * q * 2.0D)));
            case THUNDER_CAGE -> {
                // 雷牢：三道向上电弧（再减量：4 → 3）
                for (int i = 0; i < 3; i++) {
                    double a = Math.PI * 2.0D * i / 3.0D + t * 0.05D;
                    Vec3 foot = c.add(Math.cos(a) * R * 0.8D, 0.3D, Math.sin(a) * R * 0.8D);
                    SpellVfx.trail(level, foot,
                            foot.add(0.0D, 2.2D * g + p * 2.4D, 0.0D), ac, 3, 0.08D, 0.03D);
                }
            }
            case FLAME_STORM -> // 火海：脚下火环 + 火舌
                    SpellVfx.ring(level, c.add(0.0D, 0.12D, 0.0D), ac, R * 0.75D,
                            Math.max(4, (int) (6 * q * 2.0D)), 0.35D, 0.0D);
            case QUAKE -> // 震地：沙尘环
                    SpellVfx.ring(level, c.add(0.0D, 0.1D, 0.0D), au, R * 0.85D,
                            Math.max(4, (int) (5 * q * 2.0D)), 0.12D, 0.0D);
            case SOUL_DRAIN, BLOOD_POOL -> // 摄魂/血池：向内收的雾（抬到胸口以上）
                    SpellVfx.shell(level, c.add(0.0D, 1.8D, 0.0D), ac, R * 0.55D,
                            Math.max(3, (int) (4 * q * 2.0D)), -0.18D);
            case AFTERIMAGE -> // 遁术：环绕残影（抬到头顶上方）
                    SpellVfx.shell(level, c.add(0.0D, 1.9D, 0.0D), au, R * 0.6D,
                            Math.max(3, (int) (4 * q * 2.0D)), 0.10D);
            case GUARD_AURA -> // 护体：贴身球壳（抬到头顶上方）
                    SpellVfx.shell(level, c.add(0.0D, 1.7D, 0.0D), au,
                            1.5D + p * 0.9D, Math.max(3, (int) (4 * q * 2.0D)), 0.03D);
            case HEAL_BLOOM, VINE_BIND -> // 回春/缚藤：花瓣上升
                    SpellVfx.spiral(level, c, ac, Math.max(3, (int) (3 * q * 2.0D)),
                            R * 0.5D, 2.2D * g + p);
            case VOID_SINGULARITY, SPIRIT_BURST -> // 奇点/神识：吸积环（向内）
                    SpellVfx.shell(level, c.add(0.0D, 1.9D, 0.0D), main, R * 0.45D,
                            Math.max(3, (int) (4 * q * 2.0D)), -0.22D);
            case STAR_FALL -> // 天陨：高空预热的一线陨光
                    SpellVfx.trail(level, c.add(0.0D, 12.0D * g, 0.0D),
                            c.add(0.0D, 3.0D, 0.0D), main,
                            Math.max(3, (int) (4 * q * 2.0D)), 0.35D, 0.02D);
            case MOON_SLASH -> // 斩星：天顶垂落的光柱
                    SpellVfx.trail(level, c.add(0.0D, 10.0D * g, 0.0D), c, ac,
                            Math.max(3, (int) (3 * q * 2.0D)), 0.30D, 0.02D);
            case RAILGUN -> // 轨道：头顶充电环
                    SpellVfx.grandRings(level, c.add(0.0D, 5.0D * g, 0.0D), ac, 1,
                            R * 0.7D, Math.max(3, (int) (3 * q * 2.0D)));
            default -> {
                // 被动/无专属演出的法术：保持原样（三图案已经足够）
            }
        }
    }

    // ---- 八种图案 ----

    private static void rings(ServerLevel level, Vec3 c, ParticleOptions core,
                              ParticleOptions accent, ParticleOptions aura,
                              double p, double g, double q, double R, long t, int lv) {
                com.hongmeng.xiuxian.technique.ParticleBatch pb1 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int L = 0; L < lv; L++) {
            double rr = R * (0.4D + 0.6D * ((L + 1.0D) / lv));
            int n = (int) ((14 + p * 18) * q);
            double phase = t * 0.02D * (L % 2 == 0 ? 1 : -1);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0D * i / n + phase;
                pb1.add(L % 2 == 0 ? core : accent, c.x + Math.cos(a) * rr, c.y + 0.15D + L * 0.18D, c.z + Math.sin(a) * rr);
            }
        }
        pb1.flush();

        SpellVfx.grandRings(level, c, aura, 2, R * 0.9D, (int) (20 * q));
    }

    private static void spikes(ServerLevel level, Vec3 c, ParticleOptions core,
                               ParticleOptions accent, double p, double g, double q,
                               double R, long t, int lv) {
        int n = lv * 2;
                com.hongmeng.xiuxian.technique.ParticleBatch pb2 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n;
            double x = c.x + Math.cos(a) * R;
            double z = c.z + Math.sin(a) * R;
            int h = (int) ((1.6D + p * 3.4D) * g);
            for (int k = 0; k < h; k++) {
                pb2.add(k % 3 == 0 ? accent : core, x, c.y + 0.3D + k * 0.35D, z);
            }
        }
        pb2.flush();

    }

    private static void pillars(ServerLevel level, Vec3 c, ParticleOptions core,
                                ParticleOptions accent, double p, double g, double q,
                                double R, long t, int lv) {
                com.hongmeng.xiuxian.technique.ParticleBatch pb3 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < lv; i++) {
            double a = Math.PI * 2.0D * i / lv + t * 0.03D;
            double x = c.x + Math.cos(a) * R;
            double z = c.z + Math.sin(a) * R;
            int h = (int) (5 * g);
            for (int k = 0; k < h; k++) {
                pb3.add(k % 4 == 0 ? accent : core, x, c.y + h - k, z);
            }
        }
        pb3.flush();

    }

    private static void rain(ServerLevel level, Vec3 c, ParticleOptions core,
                             ParticleOptions residue, double p, double g, double q,
                             double R) {
        int n = (int) ((9 + p * 9) * q);
                com.hongmeng.xiuxian.technique.ParticleBatch pb4 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            double rr = Math.sqrt(level.random.nextDouble()) * R;
            pb4.add(i % 3 == 0 ? residue : core, c.x + Math.cos(a) * rr, c.y + (9.0D + p * 5.0D) * g, c.z + Math.sin(a) * rr);
        }
        pb4.flush();

    }

    private static void spirals(ServerLevel level, Vec3 c, ParticleOptions core,
                                ParticleOptions accent, double p, double g, double q,
                                double R, long t, int lv) {
                com.hongmeng.xiuxian.technique.ParticleBatch pb5 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int L = 0; L < 2; L++) {
            double rr = R * (0.5D + 0.3D * L);
            int n = (int) ((12 + p * 12) * q);
            double spin = t * (0.10D + 0.05D * L) * (L % 2 == 0 ? 1 : -1);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0D * i / n + spin;
                double yy = c.y + 0.3D + (1.0D - (double) i / n) * (5.0D + p * 5.0D) * g;
                pb5.add(L % 2 == 0 ? core : accent, c.x + Math.cos(a) * rr, yy, c.z + Math.sin(a) * rr);
            }
        }
        pb5.flush();

    }

    private static void vines(ServerLevel level, Vec3 c, ParticleOptions core,
                              ParticleOptions accent, double p, double g, double q,
                              double R, long t, int lv) {
        int n = Math.max(1, lv);
                com.hongmeng.xiuxian.technique.ParticleBatch pb6 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n + t * 0.01D;
            int seg = (int) ((3 + p * 5) * g);
            for (int k = 0; k < seg; k++) {
                double tt = (double) k / seg;
                double rr = R * (1.0D - tt * (0.3D + p * 0.5D));
                double yy = c.y + 0.2D + Math.sin(tt * Math.PI) * (2.0D + p * 1.8D) * g;
                pb6.add(k % 2 == 0 ? core : accent, c.x + Math.cos(a + tt) * rr, yy, c.z + Math.sin(a + tt) * rr);
            }
        }
        pb6.flush();

    }

    private static void funnel(ServerLevel level, Vec3 c, ParticleOptions core,
                               ParticleOptions accent, ParticleOptions aura,
                               double p, double g, double q, double R) {
        // 从阵盘边缘向身体收束
        // 收束终点抬到头顶上方（2.6）：原 1.2 正好是视线中心 → 第一人称糊脸
        Vec3 body = c.add(0.0D, 2.6D, 0.0D);
        int n = (int) ((9 + p * 9) * q);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n;
            Vec3 from = new Vec3(c.x + Math.cos(a) * R, c.y + 0.25D, c.z + Math.sin(a) * R);
            SpellVfx.trail(level, from, body, i % 2 == 0 ? accent : core, 2, 0.03D, 0.0D);
        }
        SpellVfx.shell(level, body, aura, R * 0.4D, (int) (10 * q), 0.02D);
    }

    private static void glyphs(ServerLevel level, Vec3 c, ParticleOptions core,
                               ParticleOptions aura, double p, double g, double q,
                               double R, long t) {
        // 外圈符环：每 15° 一个亮点，缓慢旋转
        int n = 10;
                com.hongmeng.xiuxian.technique.ParticleBatch pb7 =
                new com.hongmeng.xiuxian.technique.ParticleBatch(level);
for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n + t * 0.012D;
            pb7.add(i % 3 == 0 ? aura : core, c.x + Math.cos(a) * R, c.y + 0.2D, c.z + Math.sin(a) * R);
        }
        pb7.flush();

    }
}
