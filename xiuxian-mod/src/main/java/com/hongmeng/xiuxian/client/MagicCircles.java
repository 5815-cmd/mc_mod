package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.spirit.Element;
import com.hongmeng.xiuxian.technique.SpellVfx;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <b>超位魔法式粒子法阵</b>（玩家给的参考效果：悬空多层圆环 + 底部曼荼罗）。
 *
 * <p><b>这个效果是由粒子组成的</b>（参考视频的弹幕原文如此），所以做法是
 * 把粒子**按几何位置排布**：一圈足够密的点看上去就是一条光环。</p>
 *
 * <h2>参考图的六个特征，逐条对应实现</h2>
 * <ol>
 *   <li><b>悬空多层水平环</b>：每层有独立高度与半径，最高的最大（{@link #burst}）；</li>
 *   <li><b>高度递减</b>：往下每层半径也递减；</li>
 *   <li><b>底部密集曼荼罗</b>：多层细圆 + 符文 + 内嵌星形，密度最高；</li>
 *   <li><b>环上疏密不均</b>：用"虚线步进"（on/off 交替 + 每 6 点加长刻度）而不是均匀撒点；</li>
 *   <li><b>倾斜轨道环</b>：{@code tilt} 参数让环绕 X 轴倾斜，做出斜插着转的那几圈；</li>
 *   <li><b>自发光观感</b>：用元素亮粒子 + 每层叠加"亮芯"层。</li>
 * </ol>
 *
 * <h2>性能</h2>
 * <p>每 {@value #STEP} tick 布一次点；单组法阵最多 {@value #MAX_RINGS} 层；
 * 同时最多 {@value #MAX_GROUPS} 组。超出就丢最旧的，绝不无限增长。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class MagicCircles {

    /** 每多少 tick 布一次点（2026-11-07 R3：2 → 3，粒子环再省 1/3）。 */
    private static final int STEP = 3;

    /** 单组最多几层环（2026-11-07 R3：6 → 3；重"华丽"靠贴层与立体，不靠层数堆叠）。 */
    private static final int MAX_RINGS = 3;

    /** 同时最多几组（2026-11-07 R3：4 → 3）。 */
    private static final int MAX_GROUPS = 3;

    /**
     * 一组法阵的寿命（tick）。
     *
     * <p>玩家要求「释放完法术后的粒子效果留存更华丽」，所以从 90 抬到 150
     * （7.5 秒）；灭世级按半径再放大（见 {@link #lifeFor(float)}）。</p>
     */
    // 原来 150（7.5 秒）—— 玩家反馈「身边一直围着光环挡视野」，降到 50（2.5 秒）。
    /** 法阵视觉半径上限（格）。2026-11-07 R3：8 → **5**（脚下纹路不覆盖战场）。 */
    private static final float VISUAL_RADIUS_CAP = 5.0F;

    /**
     * <b>单环半径硬上限</b>。
     *
     * <p>曾经领域特效传了 44 格（直径 88 格 &gt; 视野），
     * 半透明大面积把画面整个糊住。这里在构造处兜底：
     * 不管调用方传什么，都不会超过这个值。</p>
     *
     * <p>2026-11-07 R3：14 → **7**。玩家截图里"仰头一大片黄色光扇/脚下的巨大法阵"
     * 就是这张 512×512 发光贴图被放大到 20+ 格铺满视野——半径必须再收。</p>
     */
    private static final float MAX_RING_RADIUS = 7.0F;

    private static final int BASE_LIFE = 50;

    /** 一层环。 */
    private static final class Ring {
        final Vec3 pos;            // 组中心
        final Element element;
        final float radius;        // 本层半径（格）
        final float height;        // 相对组中心的高度（格）
        final boolean horizontal;  // true = 水平圆环；false = 竖直面向相机
        final float tiltDeg;       // 倾斜（水平环绕 X 轴）
        final float spinSpeed;     // 转速（度/tick 的比例）
        final int dashOn;          // 虚线：连续几个点
        final int dashOff;         // 虚线：空几个点
        final int baseCount;       // 基准点数
        final boolean bright;      // 是否用"亮芯"粒子（叠加更亮的一层）
        /** PERF(C): 贴图前缀用的元素小写名，构造时算一次（原来每帧 name().toLowerCase()）。 */
        final String texElement;

        Ring(Vec3 pos, Element element, float radius, float height, boolean horizontal,
             float tiltDeg, float spinSpeed, int dashOn, int dashOff, int baseCount,
             boolean bright) {
            this.pos = pos;
            this.element = element;
            this.texElement = element == null ? "none"
                    : element.name().toLowerCase(java.util.Locale.ROOT);
            this.radius = (float) Math.min(MAX_RING_RADIUS, Math.max(0.2F, radius));
            this.height = height;
            this.horizontal = horizontal;
            this.tiltDeg = tiltDeg;
            this.spinSpeed = spinSpeed;
            this.dashOn = dashOn;
            this.dashOff = dashOff;
            this.baseCount = baseCount;
            this.bright = bright;
        }
    }

    /** 一组法阵（一组 = 多层环）。 */
    private static final class Group {
        final List<Ring> rings = new ArrayList<>();
        final int lifeTicks;
        final boolean shrink;
        int age;

        Group(int lifeTicks, boolean shrink) {
            this.lifeTicks = lifeTicks;
            this.shrink = shrink;
        }
    }

    private static final List<Group> ACTIVE = new ArrayList<>();

    private MagicCircles() {
    }

    // ------------------------------------------------------------------
    // 对外：三种组合
    // ------------------------------------------------------------------

    /** 蓄力：竖直小环 + 头顶一圈（不要太大，蓄力要能看清人）。 */
    public static void charge(Vec3 pos, Element element) {
        Group g = new Group(14, false);
        Element el = element == null ? Element.NONE : element;
        g.rings.add(new Ring(pos.add(0.0D, 4.0D, 0.0D), el, 3.0F, 0.0F, false,
                0.0F, 1.0F, 4, 3, 90, false));
        g.rings.add(new Ring(pos.add(0.0D, 6.0D, 0.0D), el, 2.2F, 0.0F, true,
                0.0F, -1.4F, 3, 4, 70, true));
        ACTIVE.add(g);
        trim();
    }

    /**
     * 释放：**参考图的那套**（头顶巨环 + 3 层递减水平环 + 底部曼荼罗 + 斜轨道环）。
     *
     * @param radius 基准半径（格）。灭世级给 24~30，普通给 8~12
     */

    /**
     * 按法阵半径决定寿命：越大的法术，留存越久（灭世要有余韵）。
     *
     * @param radius 法阵半径
     * @return 寿命 tick（基础 7.5 秒；灭世级约 10.5 秒）
     */
    private static int lifeFor(float radius) {
        double mul = radius >= 24.0F ? 1.4D : (radius >= 14.0F ? 1.2D : 1.0D);
        return (int) Math.round(BASE_LIFE * mul);
    }


    /**
     * 给一组法阵补一条**外扩的余烬环**（专管释放后的留存）。
     *
     * <p>半径比主阵大 25%，shrink=false 让它随生命期自然外扩；
     * 转速很慢（0.35），配合残留期的亮芯粒子，形成慢慢散开的光尘。</p>
     */
    private static void emberRing(Group g, Vec3 pos, Element el, float radius) {
        g.rings.add(new Ring(pos.add(0.0D, 0.9D, 0.0D), el, radius * 1.25F, 0.0F,
                true, 0.0F, 0.35F, 1, 3, 160, true));
        g.rings.add(new Ring(pos.add(0.0D, 1.9D, 0.0D), el, radius * 0.95F, 0.0F,
                true, 0.0F, -0.5F, 2, 4, 130, true));
    }

    public static void burst(Vec3 pos, Element element, float radius) {
        Element el = element == null ? Element.NONE : element;
        Group g = new Group(lifeFor(radius), false);

        // ---- 1) 头顶巨环（最高、最大，像张开的光环）----
        g.rings.add(new Ring(pos.add(0.0D, radius * 0.95D, 0.0D), el, radius * 1.00F, 0.0F,
                true, 0.0F, 0.8F, 5, 4, 190, true));
        g.rings.add(new Ring(pos.add(0.0D, radius * 0.80D, 0.0D), el, radius * 0.86F, 0.0F,
                true, 0.0F, -1.1F, 4, 5, 165, false));
        // ---- 2) 中间递减水平环 ----
        g.rings.add(new Ring(pos.add(0.0D, radius * 0.52D, 0.0D), el, radius * 0.68F, 0.0F,
                true, 0.0F, 1.35F, 4, 4, 140, true));
        g.rings.add(new Ring(pos.add(0.0D, radius * 0.28D, 0.0D), el, radius * 0.52F, 0.0F,
                true, 0.0F, -1.6F, 3, 4, 115, false));
        // ---- 3) 底部曼荼罗（密集：细圆 + 符文 + 星形）----
        g.rings.add(new Ring(pos.add(0.0D, 0.15D, 0.0D), el, radius * 0.95F, 0.0F,
                true, 0.0F, 0.55F, 6, 3, 175, true));
        g.rings.add(new Ring(pos.add(0.0D, 0.22D, 0.0D), el, radius * 0.78F, 0.0F,
                true, 0.0F, -0.85F, 5, 5, 145, false));
        g.rings.add(new Ring(pos.add(0.0D, 0.30D, 0.0D), el, radius * 0.60F, 0.0F,
                true, 0.0F, 1.15F, 4, 4, 120, true));
        g.rings.add(new Ring(pos.add(0.0D, 0.38D, 0.0D), el, radius * 0.42F, 0.0F,
                true, 0.0F, -1.5F, 3, 3, 95, false));
        g.rings.add(new Ring(pos.add(0.0D, 0.46D, 0.0D), el, radius * 0.26F, 0.0F,
                true, 0.0F, 2.0F, 2, 3, 70, true));
        // ---- 4) 斜插的轨道环（参考图里那几圈斜着转的）----
        g.rings.add(new Ring(pos.add(0.0D, radius * 0.55D, 0.0D), el, radius * 0.74F, 0.0F,
                true, 62.0F, 1.05F, 4, 6, 130, false));
        g.rings.add(new Ring(pos.add(0.0D, radius * 0.42D, 0.0D), el, radius * 0.58F, 0.0F,
                true, -48.0F, -1.25F, 3, 6, 110, true));
        // ---- 5) 竖直正面环（面向玩家，像"贴脸"的那一层）----
        g.rings.add(new Ring(pos.add(0.0D, radius * 0.5D, 0.0D), el, radius * 0.46F, 0.0F,
                false, 0.0F, -1.9F, 3, 4, 100, false));

                // ---- 外扩的余烬环：专管留存华丽 ----
        //
        // 玩家澄清「华丽指的是释放完法术后的粒子效果留存更华丽」。
        // 这个环**只在残留期亮起来**：半径比主阵更大、转速更慢、
        // 边走边向上飘，观感是法阵炸开之后，一圈发光的余烬慢慢散出去。
        emberRing(g, pos, el, radius);
        ACTIVE.add(g);

        // ---- 脚下光环（玩家明确要求）----
        //
        // 原来蓄力/释放只有悬空的多层环，脚下是空的 —— 玩家反馈
        // 「为什么只有天上有蓄力效果脚下没有光辉光环围绕」。
        // 这里在地面高度再铺两层贴地符文环 + 一层缓慢自转的大光环，
        // 半径随进度同步撑开，与空中环构成"上下呼应"的完整法阵。
        Group ground = new Group(lifeFor(radius), false);
        ground.rings.add(new Ring(pos.add(0.0D, 0.08D, 0.0D), el,
                radius * 1.05F, 0.0F, true, 0.0F, 0.55F, 2, 5, 200, true));
        ground.rings.add(new Ring(pos.add(0.0D, 0.10D, 0.0D), el,
                radius * 0.68F, 0.0F, true, 0.0F, -0.9F, 3, 4, 160, true));
        ground.rings.add(new Ring(pos.add(0.0D, 0.12D, 0.0D), el,
                radius * 1.38F, 0.0F, true, 0.0F, 0.30F, 1, 7, 150, false));
        ACTIVE.add(ground);
        trim();
    }

    /** 领域：放大版 + 更高更多层。 */
    public static void domain(Vec3 pos, Element element) {
        Element el = element == null ? Element.NONE : element;
        // ⚠ 半径必须收敛：原来 40/44 格（直径 88 格 > 视野），
        // 半透明大面积直接把画面糊住（玩家："极其遮挡视野"）。
        // 现在最大 9 格（R2 前是 11）—— 仍然看得出"领域张开"，但只在脚下与身前。
        //
        // R2（玩家 2026-11-07「领域特效也是」）：层数 3+3 → 2+2、半径 11/8.5 → 8.0/6.0、
        // 12/9.25 → 9.0/7.0、斜轨道 9 → 7，Group 存活 120 → 90 tick，每层点数密度再收敛。
        // 半径仍然全部经过构造处的 MAX_RING_RADIUS 夹紧，调用方绕不过硬上限。
        Group g = new Group(90, false);
        // 两层悬空环（贴着身体往上收，高度 2.5 / 5.0 保留）
        for (int i = 0; i < 2; i++) {
            float r = 8.0F - i * 2.0F;
            float h = 2.5F + i * 2.5F;
            g.rings.add(new Ring(pos.add(0.0D, h, 0.0D), el, r, 0.0F, true,
                    0.0F, (i % 2 == 0 ? 1F : -1F) * (0.7F + i * 0.25F), 3, 3, 120 - i * 20,
                    i % 2 == 0));
        }
        // 脚下曼荼罗（两层）
        for (int i = 0; i < 2; i++) {
            g.rings.add(new Ring(pos.add(0.0D, 0.15D + i * 0.1D, 0.0D), el,
                    9.0F - i * 2.0F, 0.0F, true, 0.0F,
                    (i % 2 == 0 ? -1F : 1F) * (0.5F + i * 0.3F), 4, 2, 110 - i * 25,
                    i % 2 == 0));
        }
        // 斜轨道环：倾角 58° → 24°（原来几乎正面横切视线），R2 半径 9 → 7
        g.rings.add(new Ring(pos.add(0.0D, 4.0D, 0.0D), el, 7.0F, 0.0F, true,
                24.0F, 1.0F, 3, 4, 100, false));
        ACTIVE.add(g);
        trim();
    }

    private static void trim() {
        while (ACTIVE.size() > MAX_GROUPS) {
            ACTIVE.remove(0);
        }
    }


    // ------------------------------------------------------------------
    // 每个法术独一无二的法阵（按法术名派生，玩家要求「特效不要重复」）
    // ------------------------------------------------------------------


    // ------------------------------------------------------------------
    // 每个法术独有的视觉签名（玩家：「每个法术的颜色阵法粒子特效都要不一样」）
    // ------------------------------------------------------------------

    /**
     * **色相偏移**（-0.18 ~ +0.18）：同元素内的不同法术给不同色调。
     *
     * <p>光靠元素配色，同元素的几个法术会撞色（玩家指出"万象寂灭和黑洞重合率太高"）。
     * 按名字哈希给一个色相偏移，于是同是"无属性"的两个灭世法术，
     * 一个偏冷紫、一个偏暖红，一眼可分。</p>
     */
    private static float hueShiftOf(int h) {
        return (Math.floorMod(h, 37) - 18) / 100.0F;      // 每 0.01 一档
    }

    /** **疏密基频**：点的间距（2~6）。越小越密。 */
    private static int densityOf(int h) {
        return 2 + Math.floorMod(h / 5, 5);
    }

    /** **转速基准**（0.35 ~ 1.25）。 */
    private static float spinOf(int h) {
        return 0.35F + Math.floorMod(h / 11, 10) * 0.10F;
    }

    /** **层高剖面**：每层的抬升量（0.18 ~ 0.62）。 */
    private static float liftOf(int h) {
        return 0.18F + Math.floorMod(h / 17, 5) * 0.11F;
    }

    /** 一个图案基元对应的环形状。 */
    private enum Motif { RING, SPIKE, PILLAR, RAIN, SPIRAL, VINE, FUNNEL, GLYPH }

    /**
     * 按法术名派生这个法术专属的环组。
     *
     * <p>用与蓄力特效同一套派生函数（{@code SpellSignature}），
     * 所以"蓄力的图案"和"释放的法阵"是**同一套签名的两种表达**，
     * 玩家能感觉到"这个法术从头到尾是一个风格"。</p>
     *
     * @param progress 0→1（蓄力时逐层叠加；释放时给 1.0）
     */
    public static void spellCast(Vec3 pos, Element element, String spellKey,
                                 float radius, double progress) {
        // 视觉半径封顶：法阵是"脚下的纹路"，不该覆盖整个战场。
        // 灭世级原本会铺出直径 75 格的同心环（玩家看到只会觉得像靶子）。
        radius = Math.min(radius, VISUAL_RADIUS_CAP);

        Element el = element == null ? Element.NONE : element;
        int h = spellKey == null ? 0 : spellKey.hashCode();
        int layers = 3 + Math.floorMod(h, 4);                 // 3~6 层
        int spinSign = Math.floorMod(h, 2) == 0 ? 1 : -1;
        Motif[] ms = motifsOf(h);                             // 三种图案
        // ---- 与名字绑定的四个独有维度（见上方注释）----
        final float hue = hueShiftOf(h);
        final int dens = densityOf(h);
        final float spinBase = spinOf(h);
        final float lift = liftOf(h);

        // 蓄力：随进度逐层加入（"法阵一层层叠加上来"）
        int shown = (int) Math.max(1, Math.round(layers * progress));
        double radiusMul = 0.5D + 0.5D * progress;

        Group g = new Group(lifeFor(radius), false);
        for (int i = 0; i < shown; i++) {
            Motif m = ms[i % ms.length];
            // 每层的半径/高度按图案类型给不同剖面
            float r;
            float hgt;
            float tilt = 0.0F;
            int on = 4, off = 4;
            int count = 120;
            switch (m) {
                case RING -> { r = radius * (0.95F - i * 0.11F); hgt = i * 0.35F;
                               on = 5; off = 4; count = 170; }
                case SPIKE -> { r = radius * (0.85F - i * 0.10F); hgt = 0.25F + i * 0.25F;
                                on = 2; off = 6; count = 150; }
                case PILLAR -> { r = radius * (0.60F - i * 0.07F); hgt = radius * (0.5F + i * 0.28F);
                                 on = 3; off = 5; count = 130; }
                case RAIN -> { r = radius * (0.75F - i * 0.09F); hgt = 0.2F + i * 0.5F;
                               on = 2; off = 8; count = 110; }
                case SPIRAL -> { r = radius * (0.90F - i * 0.12F); hgt = i * 0.30F;
                                 on = 6; off = 3; count = 160; }
                case VINE -> { r = radius * (0.88F - i * 0.10F); hgt = 0.3F + i * 0.22F;
                               on = 4; off = 5; count = 150; }
                case FUNNEL -> { r = radius * (1.00F - i * 0.16F); hgt = radius * (0.9F - i * 0.18F);
                                 on = 4; off = 4; count = 140; }
                case GLYPH -> { r = radius * (0.80F - i * 0.09F); hgt = 0.15F + i * 0.18F;
                                on = 1; off = 9; count = 180; }
                default -> { r = radius * 0.8F; hgt = i * 0.3F; }
            }
            // 第 2 层起交替倾斜，做出斜插的轨道环
            if (i >= 2) {
                tilt = (i % 2 == 0 ? 58.0F : -46.0F);
            }
            // 转速与层高用**这个法术自己的签名**，不是全局常数
            float speed = spinSign * (spinBase + i * 0.24F)
                    * (i % 2 == 0 ? 1.0F : -1.0F);
            g.rings.add(new Ring(pos.add(0.0D, hgt * (lift / 0.30F), 0.0D), el,
                    r * (float) radiusMul, hue, true, tilt, speed,
                    Math.max(1, on / Math.max(1, dens / 2)), off + dens, count,
                    i % 2 == 0));
        }
        // 始终补一条竖直正面环：保证"贴脸"也看得见
        g.rings.add(new Ring(pos.add(0.0D, radius * 0.45D, 0.0D), el,
                radius * 0.42F * (float) radiusMul, 0.0F, false, 0.0F,
                -spinSign * 1.8F, 3, 4, 100, false));
        ACTIVE.add(g);
        trim();
    }

    /** 按名字哈希取三种图案（与 SpellSignature 同一套算法，保证两边风格一致）。 */
    private static Motif[] motifsOf(int h) {
        Motif[] all = Motif.values();
        int a = Math.floorMod(h, all.length);
        Motif m1 = all[a];
        Motif[] rest1 = remove(all, a);
        int b = Math.floorMod(h / 7 + 3, rest1.length);
        Motif m2 = rest1[b];
        Motif[] rest2 = remove(rest1, indexOf(rest1, m2));
        int c = Math.floorMod(h / 13 + 5, rest2.length);
        Motif m3 = rest2[c];
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

    /** 上一 tick 施法/蓄力进展（用于"逐层叠加"）。 */
    private static float lastProgress;

    /**
     * 蓄力中每 tick 调一次：法阵**随进度逐层叠加**。
     *
     * @param progress 蓄力进度 0→1
     */
    public static void spellChargeTick(Vec3 pos, Element element, String spellKey,
                                       float radius, double progress) {
        // 视觉半径封顶：法阵是"脚下的纹路"，不该覆盖整个战场。
        // 灭世级原本会铺出直径 75 格的同心环（玩家看到只会觉得像靶子）。
        radius = Math.min(radius, VISUAL_RADIUS_CAP);

        // 每 3 tick 或进度跨过 1/层数 时重铺一次（既顺滑又省粒子）
        if (progress + 0.02D < lastProgress) {
            lastProgress = (float) progress;
            return;
        }
        lastProgress = (float) progress;
        spellCast(pos, element, spellKey, radius, progress);
    }


    // ------------------------------------------------------------------
    // 贴图层：与粒子层叠加（玩家要求「贴图配合粒子」）
    // ------------------------------------------------------------------

    /** 渲染类型缓存（**只建一次**；每帧新建会 churn 渲染资源）。 */
    private static final java.util.Map<net.minecraft.resources.ResourceLocation,
            net.minecraft.client.renderer.RenderType> TYPES =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static net.minecraft.client.renderer.RenderType type(
            net.minecraft.resources.ResourceLocation tex) {
        return TYPES.computeIfAbsent(tex,
                net.minecraft.client.renderer.RenderType::entityTranslucentEmissive);
    }

    /**
     * PERF(C): 每组法阵那三层贴图（ring / rune / star）的 RenderType，按元素名缓存。
     *
     * <p>原来每帧、每组、每层都要现拼 {@code "textures/fx/circle_" + element + "_" + kind}
     * 字符串、new 一个 ResourceLocation 再查表 —— 元素名总共只有 9 种，
     * 直接在第一次见到该元素时把 3 个 RenderType 建好，之后只是数组取值。</p>
     */
    private static final java.util.Map<String, net.minecraft.client.renderer.RenderType[]>
            LAYER_TYPES = new java.util.concurrent.ConcurrentHashMap<>();

    private static net.minecraft.client.renderer.RenderType[] layerTypes(String element) {
        return LAYER_TYPES.computeIfAbsent(element, e ->
                new net.minecraft.client.renderer.RenderType[]{
                        type(new net.minecraft.resources.ResourceLocation("hongmeng",
                                "textures/fx/circle_" + e + "_ring.png")),
                        type(new net.minecraft.resources.ResourceLocation("hongmeng",
                                "textures/fx/circle_" + e + "_rune.png")),
                        type(new net.minecraft.resources.ResourceLocation("hongmeng",
                                "textures/fx/circle_" + e + "_star.png")),
                });
    }

    /**
     * 每个法阵画 2~3 层贴图（自发光），与粒子层叠加。
     *
     * <p>由 {@code RenderLevelStageEvent.AFTER_PARTICLES} 调用。</p>
     */
    public static void render(com.mojang.blaze3d.vertex.PoseStack pose,
                              net.minecraft.world.phys.Vec3 cameraPos,
                              float partialTick) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        // **自己的缓冲区**（理由见类注释：共享缓冲区会崩客户端）
        net.minecraft.client.renderer.MultiBufferSource.BufferSource own =
                net.minecraft.client.renderer.MultiBufferSource.immediate(
                        com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder());

        long time = mc.level.getGameTime();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        for (Group g : ACTIVE) {
            double t = ((double) g.age + partialTick) / g.lifeTicks;
            double decay = t > 0.45D ? (t - 0.45D) / 0.55D : 0.0D;
            // 2026-11-07 R3：缩放收敛 —— 原来最大能涨到 ~1.55 倍再乘 14 格半径（≈21 格），
            // 仰视时那张 512 发光贴图就是"满天黄光扇面"。现在最多 ≈7×0.85×1.15 格。
            double scale = (g.shrink ? (1.2D - 0.6D * t) : (0.50D + 0.30D * t))
                    * (1.0D + 0.25D * decay);
            double fade;
            if (t < 0.10D) {
                fade = t / 0.10D;
            } else if (t < 0.45D) {
                fade = 1.0D;
            } else {
                fade = 1.0D - decay * 0.85D;
            }
            if (fade <= 0.05D) {
                continue;
            }
            // 用第一层环的位置/元素当这组法阵的锚点
            if (g.rings.isEmpty()) {
                continue;
            }
            Ring anchor = g.rings.get(0);
            // PERF(C): 元素名与三层 RenderType 都在组构造/首次渲染时缓存，每帧不再拼字符串
            String el = anchor.texElement;
            net.minecraft.client.renderer.RenderType[] types = layerTypes(el);
            float baseR = anchor.radius * (float) scale;
            // 第一人称近距：贴脚/贴脸的法阵贴图再压暗（玩家反馈"遮眼污染"）
            double camDist = Math.sqrt(cameraPos.distanceToSqr(anchor.pos));
            float fpMul = (firstPerson && camDist < 9.0D) ? 0.45F : 1.0F;

            pose.pushPose();
            pose.translate(anchor.pos.x - cameraPos.x,
                    anchor.pos.y + anchor.height - cameraPos.y,
                    anchor.pos.z - cameraPos.z);
            com.mojang.math.Axis axis = anchor.horizontal
                    ? com.mojang.math.Axis.XP : null;
            if (axis != null) {
                pose.mulPose(axis.rotationDegrees(90.0F));
            } else {
                pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            }
            // 2026-11-07 R3：只画 2 层贴图（外圈 30% / 符文 18%）。
            // ① 透明度 0.95/0.85 → 0.30/0.18：原来是"半透明大面积发光"的主污染源；
            // ② **星形放射层不再绘制** —— 截图里"满天黄色射线"就是它被放大到 20 格；
            // ③ 第一人称 9 格内再乘 0.45，脚下法阵只当氛围。
            drawLayer(pose, own, types[0], baseR, (float) time * 0.35F,
                    (float) fade * 0.30F * fpMul);
            drawLayer(pose, own, types[1], baseR * 0.70F, -(float) time * 0.75F,
                    (float) fade * 0.18F * fpMul);
            pose.popPose();
        }
        own.endBatch();
    }

    /** 画一层贴图法阵（一个中心对称的正方形面片）。 */
    private static void drawLayer(com.mojang.blaze3d.vertex.PoseStack pose,
                                  net.minecraft.client.renderer.MultiBufferSource buffers,
                                  net.minecraft.client.renderer.RenderType type,
                                  float radius, float rotationDeg, float alpha) {
        // PERF(C): RenderType 由 layerTypes() 按元素缓存，这里不再现拼贴图路径/ResourceLocation
        com.mojang.blaze3d.vertex.VertexConsumer vc = buffers.getBuffer(type);
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationDeg));
        org.joml.Matrix4f m = pose.last().pose();
        int a = (int) (alpha * 255.0F) & 0xFF;
        vertex(vc, m, -radius, -radius, a, 0.0F, 1.0F);
        vertex(vc, m, radius, -radius, a, 1.0F, 1.0F);
        vertex(vc, m, radius, radius, a, 1.0F, 0.0F);
        vertex(vc, m, -radius, radius, a, 0.0F, 0.0F);
        pose.popPose();
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer vc,
                               org.joml.Matrix4f m, float x, float y, int a,
                               float u, float v) {
        vc.vertex(m, x, y, 0.0F)
                .color(1.0F, 1.0F, 1.0F, a / 255.0F)
                .uv(u, v)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)          // 全亮：自发光
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /** 每客户端 tick 推进：布点 + 计寿命。 */
    public static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        long time = level.getGameTime();
        ACTIVE.removeIf(g -> {
            g.age++;
            if (g.age >= g.lifeTicks) {
                return true;
            }
            if (time % STEP == 0L) {
                drawGroup(level, g, time);
            }
            return false;
        });
    }

    public static void clear() {
        ACTIVE.clear();
    }

    // ------------------------------------------------------------------
    // 布点
    // ------------------------------------------------------------------

    private static void drawGroup(Level level, Group g, long time) {
        double t = (double) g.age / g.lifeTicks;
        // 半径缩放：绽放 0.45→1.0；收束 1.2→0.6
        double scale = g.shrink ? (1.2D - 0.6D * t) : (0.45D + 0.55D * t);
        // ---- 残留：释放后法阵**更华丽地留存**（玩家澄清："华丽指的是释放完
        //      法术后的粒子效果留存更华丽"）----
        //
        // 三个方向一起做：
        //   ① 扩散更大（最多再涨 90%），有"法阵炸开成一片"的观感；
        //   ② 密度**先维持再缓降**：残留前半段保持满密度（甚至因外环而更密），
        //      后半段才稀下去，最低保留 35%（原来是 15%，太秃）；
        //   ③ 后半段改用**亮芯粒子**（见 drawGroup 里 bright 的判断），
        //      让余韵是"发光的尘埃"而不是暗点。
        double decay = t > 0.40D ? (t - 0.40D) / 0.60D : 0.0D;
        // 2026-11-07 R3：残留期外扩收敛（+90% → +35%），粒子环不再"炸成一圈大靶子"
        scale *= 1.0D + 0.35D * decay;
        double fade;
        if (t < 0.08D) {
            fade = t / 0.08D;                       // 淡入（更快，别拖）
        } else if (t < 0.42D) {
            fade = 1.0D;                            // 全盛
        } else if (t < 0.58D) {
            // 残留前半段：**维持满密度**，甚至略升（外扩的余烬叠上来）
            fade = 1.0D + 0.15D * ((t - 0.42D) / 0.16D);
        } else {
            // 残留后半段：缓慢稀下去，但**保留 35%**
            double k = (t - 0.58D) / 0.42D;
            fade = 1.15D - 0.80D * k;
        }
        fade = Math.max(0.35D, Math.min(1.20D, fade));
        // 残留期整体偏亮（粒子没有 alpha，用"亮芯粒子"表达发光）
        boolean glowing = t > 0.45D;
        // 2026-11-07 R3：第一人称跳过"竖直面向相机"的环（那圈粒子贴在视线正前方）
        boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();

        int used = 0;
        for (Ring ring : g.rings) {
            if (used++ >= MAX_RINGS) {
                break;
            }
            if (firstPerson && !ring.horizontal) {
                continue;
            }
            // 残留期（glowing）**一律用亮芯粒子** —— 玩家要的是"华丽的留存"，
            // 用暗色粒子会显得法阵"熄灭"了。
            ParticleOptions p = (ring.bright || glowing)
                    ? SpellVfx.accentOf(ring.element)
                    : SpellVfx.particleOf(ring.element);
            drawRing(level, ring, p, scale, fade, time);
        }
    }

    /** 画一层环：按虚线步进布点，并可倾斜。 */
    private static void drawRing(Level level, Ring ring, ParticleOptions p,
                                 double scale, double fade, long time) {
        double radius = ring.radius * scale;
        if (radius < 0.4D) {
            return;
        }
        int n = (int) Math.max(6, ring.baseCount * 0.45D * fade);
        double spin = Math.toRadians(time * ring.spinSpeed * 1.7D);
        double tilt = Math.toRadians(ring.tiltDeg);

        double cosT = Math.cos(tilt);
        double sinT = Math.sin(tilt);
        // 竖直面向相机时的右向量
        Vec3 right = null;
        if (!ring.horizontal) {
            org.joml.Vector3f lv = Minecraft.getInstance().gameRenderer
                    .getMainCamera().getLookVector();
            double rx = -lv.z, rz = lv.x;
            double len = Math.max(1.0E-4D, Math.sqrt(rx * rx + rz * rz));
            right = new Vec3(rx / len, 0.0D, rz / len);
        }

        for (int i = 0; i < n; i++) {
            // 虚线：按 on/off 周期跳过一些点 —— 这就是参考图里"疏密不均"的来源
            int cyc = Math.max(1, ring.dashOn + ring.dashOff);
            if (ring.dashOff > 0 && (i % cyc) >= ring.dashOn) {
                continue;
            }
            double a = Math.PI * 2.0D * i / n + spin;
            // 每 6 点加长一次（表盘刻度感）
            double rr = radius * ((i % 6 == 0) ? 1.04D : 1.0D);

            double lx = Math.cos(a) * rr;
            double lz = Math.sin(a) * rr;
            double wx, wy, wz;
            if (ring.horizontal) {
                // 水平环 + 绕 X 轴倾斜
                double ty = -lz * sinT;
                double tz = lz * cosT;
                wx = ring.pos.x + lx;
                wy = ring.pos.y + ring.height + ty;
                wz = ring.pos.z + tz;
            } else {
                wx = ring.pos.x + right.x * lx;
                wy = ring.pos.y + ring.height + lz * 0.0D;
                wz = ring.pos.z + right.z * lx;
            }
            level.addParticle(p, wx, wy, wz, 0.0D, 0.0D, 0.0D);

            // 亮芯：隔点再加一个更亮的粒子，做出"光环有厚度"的观感
            if (ring.bright && i % 3 == 0) {
                level.addParticle(SpellVfx.auraOf(ring.element), wx, wy, wz,
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    /** 随机数（符文用，保留给将来的符文环扩展）。 */
    @SuppressWarnings("unused")
    private static ThreadLocalRandom rnd() {
        return ThreadLocalRandom.current();
    }
}
