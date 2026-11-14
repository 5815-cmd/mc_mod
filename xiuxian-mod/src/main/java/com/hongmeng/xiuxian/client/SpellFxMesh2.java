package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * <b>法术特效的新几何原语</b>（第 7~9 种）。
 *
 * <h2>为什么要加</h2>
 * <p>玩家一直说「特效要立体、不要只是贴图」。原来的几何库有 6 种原语
 * （棱柱 / 圆柱 / 圆锥 / 球 / 立体星芒 / 吸积盘），已经能做出立体感，
 * 但**同类法术之间形状仍然偏像** —— 因为可选形状就那么几种。</p>
 *
 * <p>所以这里补三种形状差异很大的：</p>
 * <table border="1">
 *   <caption>新增原语</caption>
 *   <tr><th>原语</th><th>形状</th><th>适合的法术感觉</th></tr>
 *   <tr><td>{@link #torus}</td><td><b>圆环体</b>（不是平面环，是有厚度的甜甜圈）</td>
 *       <td>护体金光、领域边界、传送法阵</td></tr>
 *   <tr><td>{@link #helix}</td><td><b>双螺旋</b>（两条相互缠绕的螺旋带）</td>
 *       <td>缠绕类的木/藤法术、雷霆、封印</td></tr>
 *   <tr><td>{@link #lattice}</td><td><b>立体晶格</b>（三轴交叉的棒条构成的笼）</td>
 *       <td>禁锢、阵法、金系的锐利感</td></tr>
 * </table>
 *
 * <p>三者都是**真三维网格**（有厚度、从任何角度看都是立体的），
 * 与原来的 billboard 贴图完全不同。</p>
 *
 * <p>⚠ 顶点写入方式与 {@link SpellFxMesh} 保持一致（同一个 `tri`/`quad` 口径），
 * 所以贴图与光照表现会与既有特效统一。</p>
 */
public final class SpellFxMesh2 {

    /** PERF(C): 逐环明暗用的复用颜色缓冲（写入顶点后立即失效，不跨调用保存）。 */
    private static final float[] SHADE3 = new float[3];

    /** PERF(C): 爬行裂纹前缘的常量颜色（原来每段都 new float[]{...}）。 */
    private static final float[] CRACK_EDGE_COLOR = {1.0F, 0.55F, 0.30F};

    private SpellFxMesh2() {
    }

    /**
     * <b>圆环体</b>（有厚度的环）。
     *
     * @param majorSegments 大圆分段（环的周向），建议 16~24
     * @param minorSegments 小圆分段（管截面），建议 6~10
     * @param major         大半径（环的半径）
     * @param minor         小半径（管的粗细）
     */
    public static void torus(com.mojang.blaze3d.vertex.PoseStack pose,
                             net.minecraft.client.renderer.MultiBufferSource buf,
                             String tex, int majorSegments, int minorSegments,
                             float major, float minor, float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);
        for (int i = 0; i < majorSegments; i++) {
            double u0 = 2.0D * Math.PI * i / majorSegments;
            double u1 = 2.0D * Math.PI * (i + 1) / majorSegments;
            // PERF(C): 大圆的 cos/sin 提到小圆循环外（原来每个格子都重算）
            float cosU0 = (float) Math.cos(u0);
            float sinU0 = (float) Math.sin(u0);
            float cosU1 = (float) Math.cos(u1);
            float sinU1 = (float) Math.sin(u1);
            for (int j = 0; j < minorSegments; j++) {
                double v0 = 2.0D * Math.PI * j / minorSegments;
                double v1 = 2.0D * Math.PI * (j + 1) / minorSegments;
                // 圆环体参数方程
                // PERF(C): 原来每个格子 new 4 个 float[3]（36×12 的环一帧就 1728 个数组），
                //          现在全用局部浮点算，零分配；数学与原来逐项相同。
                float cosV0 = (float) Math.cos(v0);
                float sinV0 = (float) Math.sin(v0);
                float cosV1 = (float) Math.cos(v1);
                float sinV1 = (float) Math.sin(v1);
                float ring0 = major + minor * cosV0;
                float ring1 = major + minor * cosV1;
                float y0 = minor * sinV0;
                float y1 = minor * sinV1;
                float x00 = ring0 * cosU0;
                float z00 = ring0 * sinU0;
                float x10 = ring0 * cosU1;
                float z10 = ring0 * sinU1;
                float x11 = ring1 * cosU1;
                float z11 = ring1 * sinU1;
                float x01 = ring1 * cosU0;
                float z01 = ring1 * sinU0;
                // 管截面的明暗：cos(v) 是法线的"朝外"分量。
                // 外侧亮、内侧暗 —— 这一条就让环看起来是**管子**而不是一圈纸。
                float s00 = shadeTube(v0);
                float s01 = shadeTube(v1);
                SpellFxMesh.quadShaded(vc, m, rgb, a,
                        x00, y0, z00, s00,
                        x10, y0, z10, s00,
                        x11, y1, z11, s01,
                        x01, y1, z01, s01);
            }
        }
    }

    /**
     * 管截面的亮度：以法线朝外分量为基准，加一点固定光方向的镜面高光。
     *
     * <p>光源方向取 (0.35, 0.75, 0.56)（左上偏前），和项目其它高光一致。</p>
     */
    static float shadeTube(double v) {
        double nx = Math.cos(v);
        double ny = Math.sin(v);
        // 环的管子：法线在 (径向, 垂直) 平面内
        double lambert = Math.max(0.0D, nx * 0.55D + ny * 0.83D);
        double spec = Math.pow(lambert, 5.0D) * 0.9D;
        return (float) (0.42D + 0.62D * lambert + spec);
    }

    /**
     * <b>碎片群</b>：多面体碎片向外飞散。
     *
     * <p>三点让它"像碎片"而不是"像火星"：</p>
     * <ol>
     *   <li>每片是<b>两个交叉方片</b>（X 形），从任何角度看都有体积；</li>
     *   <li>随进度<b>翻滚</b>（角度按片号错开）；</li>
     *   <li>受<b>重力</b>下坠（不是匀速直线飞走）。</li>
     * </ol>
     *
     * @param t 0~1 进度
     */
    public static void shards(com.mojang.blaze3d.vertex.PoseStack pose,
                              net.minecraft.client.renderer.MultiBufferSource buf,
                              String tex, int count, float size, float spread,
                              float t, float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);
        float ease = 1.0F - (1.0F - t) * (1.0F - t);   // easeOut
        for (int i = 0; i < count; i++) {
            // 确定性伪随机（同一 i 每帧一致，不会抖）
            float h1 = frac(i * 0.6180339F);
            float h2 = frac(i * 0.7548776F);
            float ang = h1 * 6.2831853F;
            float elev = (h2 - 0.5F) * 1.5F;
            float dx = (float) (Math.cos(ang) * Math.cos(elev));
            float dz = (float) (Math.sin(ang) * Math.cos(elev));
            float dy = (float) Math.sin(elev);
            float dist = spread * ease * (0.55F + 0.45F * h2);
            // 重力下坠
            float gy = -spread * 0.55F * t * t;
            float px = dx * dist;
            float py = dy * dist + gy;
            float pz = dz * dist;
            float hs = size * (1.0F - 0.45F * t) * (0.6F + 0.6F * h1);
            // 翻滚
            float rot = t * 6.0F + h1 * 6.28F;
            float cs = (float) Math.cos(rot);
            float sn = (float) Math.sin(rot);
            // 片 1：水平面
            SpellFxMesh.quad(vc, m, rgb, a,
                    px - hs * cs, py - hs, pz - hs * sn,
                    px + hs * cs, py - hs, pz + hs * sn,
                    px + hs * cs, py + hs, pz + hs * sn,
                    px - hs * cs, py + hs, pz - hs * sn);
            // 片 2：竖直面（交叉）
            SpellFxMesh.quad(vc, m, rgb, a,
                    px - hs * sn, py - hs * cs, pz + hs * cs,
                    px + hs * sn, py - hs * cs, pz - hs * cs,
                    px + hs * sn, py + hs * cs, pz - hs * cs,
                    px - hs * sn, py + hs * cs, pz + hs * cs);
        }
    }

    /** 取小数部分（0~1）。 */
    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    /**
     * <b>能量丝</b>：多股细长管束缠绕上升。
     *
     * <p>每股是一条<b>细带</b>（宽度 {@code width}），沿螺旋路径拐 8 段。
     * 为什么不用圆管：8 段螺旋上圆管的三角面数是细带的 6 倍，
     * 而这个尺寸下（宽 0.04 格）两者看不出区别。</p>
     */
    public static void filaments(com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buf,
                                 String tex, int strands, float height, float radius,
                                 float width, float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);
        int segs = 10;
        for (int s = 0; s < strands; s++) {
            double phase = 2.0D * Math.PI * s / Math.max(1, strands);
            for (int i = 0; i < segs; i++) {
                double t0 = (double) i / segs;
                double t1 = (double) (i + 1) / segs;
                double a0 = phase + t0 * 5.2D;
                double a1 = phase + t1 * 5.2D;
                // 半径随高度收缩（像聚拢成一股）
                double r0 = radius * (1.0D - 0.45D * t0);
                double r1 = radius * (1.0D - 0.45D * t1);
                float y0 = (float) (height * t0);
                float y1 = (float) (height * t1);
                float x00 = (float) (Math.cos(a0) * r0);
                float z00 = (float) (Math.sin(a0) * r0);
                float x01 = (float) (Math.cos(a0) * (r0 + width));
                float z01 = (float) (Math.sin(a0) * (r0 + width));
                float x10 = (float) (Math.cos(a1) * r1);
                float z10 = (float) (Math.sin(a1) * r1);
                float x11 = (float) (Math.cos(a1) * (r1 + width));
                float z11 = (float) (Math.sin(a1) * (r1 + width));
                // 越高越淡（能量上升到顶端散掉）
                float fade = 1.0F - 0.55F * (float) t0;
                SpellFxMesh.quad(vc, m, rgb,
                        SpellFxMesh.byteAlpha(alpha * fade),
                        x00, y0, z00, x10, y1, z10, x11, y1, z11, x01, y0, z01);
            }
        }
    }

    /**
     * <b>地面裂纹贴花</b>：一个平铺在地面的四边形。
     *
     * <p>用 {@code entityTranslucentEmissive}（普通 alpha 混合）——
     * 裂纹要"变暗"，additive 做不到。</p>
     */
    public static void groundCrack(com.mojang.blaze3d.vertex.PoseStack pose,
                                   net.minecraft.client.renderer.MultiBufferSource buf,
                                   String tex, float radius, float[] rgb,
                                   float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);
        SpellFxMesh.quad(vc, m, rgb, a,
                -radius, 0.0F, -radius,
                radius, 0.0F, -radius,
                radius, 0.0F, radius,
                -radius, 0.0F, radius);
    }

    /**
     * <b>陨石拖尾</b>：沿抛物线从 p0 飞到 p1，并在身后拖一串渐小的尾迹。
     *
     * <p>坐标都是<b>相机相对</b>（调用方已经减掉相机位置）。</p>
     */
    public static void meteorArc(com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buf,
                                 String tex, float x0, float y0, float z0,
                                 float x1, float y1, float z1, float radius,
                                 float t, float[] rgb, float alpha) {
        // 抛物弧高度：水平距离的 35%
        float dx = x1 - x0;
        float dz = z1 - z0;
        float horiz = (float) Math.sqrt(dx * dx + dz * dz);
        float arc = Math.max(2.0F, horiz * 0.35F);
        int trail = 6;
        for (int k = trail - 1; k >= 0; k--) {
            float tk = Math.max(0.0F, t - k * 0.045F);
            float e = 1.0F - (1.0F - tk) * (1.0F - tk);
            float px = x0 + dx * e;
            float pz = z0 + dz * e;
            float py = y0 + (y1 - y0) * e + arc * (float) Math.sin(Math.PI * tk);
            float r = radius * (k == 0 ? 1.0F : 0.72F - 0.09F * k);
            float aa = alpha * (k == 0 ? 1.0F : 0.55F - 0.07F * k);
            if (r <= 0.01F || aa <= 0.01F) {
                continue;
            }
            // 用球体当核心（复用带 Phong 的 sphere，有立体感）
            SpellFxMesh.sphere(buf, pose, tex, 6, 10, r, rgb, aa, 1.0F,
                    px, py, pz);
        }
    }

    /**
     * <b>光柱横扫</b>：一根竖直光柱绕原点旋转。
     *
     * @param angleDeg 当前角度（度）
     */
    public static void beamSweep(com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buf,
                                 String tex, float height, float width,
                                 float angleDeg, float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);
        double rad = Math.toRadians(angleDeg);
        float cx = (float) Math.cos(rad);
        float cz = (float) Math.sin(rad);
        // 垂直于扫向的方向 = 光柱的宽度方向
        float wx = -cz * width;
        float wz = cx * width;
        // 从地面到 height 的一条"带子"（两面，避免背面剔除后消失）
        SpellFxMesh.quad(vc, m, rgb, a,
                wx, 0.0F, wz, -wx, 0.0F, -wz,
                -wx, height, -wz, wx, height, wz);
        SpellFxMesh.quad(vc, m, rgb, a,
                -wx, 0.0F, -wz, wx, 0.0F, wz,
                wx, height, wz, -wx, height, -wz);
    }

    /**
     * <b>螺旋上升</b>：{@code count} 个光球沿螺旋一路升高，半径逐渐收缩。
     *
     * <p>"收缩"是关键 —— 半径不变是"圆周上升"（像电梯），
     * 半径收缩才像被"吸"上去。</p>
     */
    public static void spiralAscent(com.mojang.blaze3d.vertex.PoseStack pose,
                                    net.minecraft.client.renderer.MultiBufferSource buf,
                                    String tex, int count, float height,
                                    float radius, float ballR, float t,
                                    float[] rgb, float alpha) {
        int n = Math.max(3, count);
        for (int i = 0; i < n; i++) {
            float ph = (float) i / n;
            float y = height * ((t + ph) % 1.0F);
            double ang = (t * 2.0F + ph) * 6.2831853D;
            float r = radius * (1.0F - 0.62F * y / Math.max(0.001F, height));
            float px = (float) (Math.cos(ang) * r);
            float pz = (float) (Math.sin(ang) * r);
            float a = alpha * (1.0F - 0.35F * y / Math.max(0.001F, height));
            SpellFxMesh.sphere(buf, pose, tex, 5, 8, ballR, rgb, a, 1.0F, px, y, pz);
        }
    }

    /** <b>环绕</b>：几个光球绕施法点旋转，带正弦高度起伏。 */
    public static void orbit(com.mojang.blaze3d.vertex.PoseStack pose,
                             net.minecraft.client.renderer.MultiBufferSource buf,
                             String tex, int count, float radius, float height,
                             float ballR, float t, float[] rgb, float alpha) {
        int n = Math.max(2, count);
        for (int i = 0; i < n; i++) {
            double ang = (t + (float) i / n) * 6.2831853D;
            float px = (float) (Math.cos(ang) * radius);
            float pz = (float) (Math.sin(ang) * radius);
            float py = height + (float) Math.sin(ang * 2.0D) * height * 0.25F;
            SpellFxMesh.sphere(buf, pose, tex, 5, 8, ballR, rgb, alpha, 1.0F,
                    px, py, pz);
        }
    }

    /**
     * <b>爬行裂纹</b>：裂纹由中心向外生长。
     *
     * <p>做法：把贴花按 {@code 0.25 → 1.0} 缩放，同时<b>前缘</b>额外叠一圈亮环
     * （表示"正在裂开的地方"）。只有缩放没有亮环的话，
     * 看起来像"裂纹在放大"，不像"裂纹在爬"。</p>
     */
    public static void creepingCrack(com.mojang.blaze3d.vertex.PoseStack pose,
                                     net.minecraft.client.renderer.MultiBufferSource buf,
                                     String tex, float radius, float t,
                                     float[] rgb, float alpha) {
        float grow = 0.25F + 0.75F * (1.0F - (1.0F - t) * (1.0F - t));
        float r = radius * grow;
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);
        SpellFxMesh.quad(vc, m, rgb, a,
                -r, 0.0F, -r, r, 0.0F, -r, r, 0.0F, r, -r, 0.0F, r);
        // 前缘亮环（只在生长阶段）
        if (t < 0.92F) {
            int seg = 20;
            float lw = radius * 0.06F;
            for (int i = 0; i < seg; i++) {
                double a0 = 2.0D * Math.PI * i / seg;
                double a1 = 2.0D * Math.PI * (i + 1) / seg;
                float x0 = (float) (Math.cos(a0) * r);
                float z0 = (float) (Math.sin(a0) * r);
                float x1 = (float) (Math.cos(a1) * r);
                float z1 = (float) (Math.sin(a1) * r);
                float ix0 = (float) (Math.cos(a0) * (r - lw));
                float iz0 = (float) (Math.sin(a0) * (r - lw));
                float ix1 = (float) (Math.cos(a1) * (r - lw));
                float iz1 = (float) (Math.sin(a1) * (r - lw));
                // 用明亮的雷色描前缘
                // PERF(C): 常量颜色数组，不再每段 new float[]{...}
                SpellFxMesh.quad(vc, m, CRACK_EDGE_COLOR,
                        SpellFxMesh.byteAlpha(0.85F * (1.0F - t)),
                        ix0, 0.01F, iz0, ix1, 0.01F, iz1,
                        x1, 0.01F, z1, x0, 0.01F, z0);
            }
        }
    }

    /**
     * <b>阵法</b>：三重同心环（外/中/内反向旋转）+ 8 根辐条 + 中心符文块。
     *
     * <p>水平铺在地面（y=0 平面），靠 {@code t} 驱动旋转。</p>
     *
     * <p>为什么三重环要<b>反向</b>：同向转看起来像一个唱片在转；
     * 反向才有"机关在运转"的感觉 —— 这是所有游戏里法阵的通用做法。</p>
     */
    public static void mandala(com.mojang.blaze3d.vertex.PoseStack pose,
                               net.minecraft.client.renderer.MultiBufferSource buf,
                               String tex, float radius, float t,
                               float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);

        // ---- 三重同心环 ----
        for (int ring = 0; ring < 3; ring++) {
            float r = radius * (1.0F - ring * 0.26F);
            float width = radius * (0.055F - ring * 0.008F);
            float spin = t * (ring % 2 == 0 ? 1.0F : -1.0F) * (0.8F + ring * 0.4F);
            int seg = 40 - ring * 6;
            float bright = 1.0F - ring * 0.18F;
            // PERF(C): 每一环的颜色只算一次；原来每个分段都 new float[]{...}（约 100 个/帧）
            SHADE3[0] = rgb[0] * bright;
            SHADE3[1] = rgb[1] * bright;
            SHADE3[2] = rgb[2] * bright;
            for (int i = 0; i < seg; i++) {
                // 每 4 段留一个缺口，看起来像刻出来的符文而不是实线
                if (ring == 2 && i % 4 == 3) {
                    continue;
                }
                double a0 = 2.0D * Math.PI * i / seg + spin;
                double a1 = 2.0D * Math.PI * (i + 1) / seg + spin;
                float x0 = (float) (Math.cos(a0) * r);
                float z0 = (float) (Math.sin(a0) * r);
                float x1 = (float) (Math.cos(a1) * r);
                float z1 = (float) (Math.sin(a1) * r);
                float ix0 = (float) (Math.cos(a0) * (r - width));
                float iz0 = (float) (Math.sin(a0) * (r - width));
                float ix1 = (float) (Math.cos(a1) * (r - width));
                float iz1 = (float) (Math.sin(a1) * (r - width));
                SpellFxMesh.quad(vc, m, SHADE3, a,
                        ix0, 0.0F, iz0, ix1, 0.0F, iz1,
                        x1, 0.0F, z1, x0, 0.0F, z0);
            }
        }

        // ---- 8 根辐条（缓慢反向转） ----
        float spokeSpin = -t * 0.45F;
        for (int i = 0; i < 8; i++) {
            double ang = 2.0D * Math.PI * i / 8 + spokeSpin;
            float r0 = radius * 0.30F;
            float r1 = radius * 0.94F;
            float w = radius * 0.030F;
            float ca = (float) Math.cos(ang);
            float sa = (float) Math.sin(ang);
            // 沿径向的细长四边形（法线方向给一点宽度）
            float px = -sa * w;
            float pz = ca * w;
            SpellFxMesh.quad(vc, m, rgb, a,
                    ca * r0 - px, 0.0F, sa * r0 - pz,
                    ca * r0 + px, 0.0F, sa * r0 + pz,
                    ca * r1 + px, 0.0F, sa * r1 + pz,
                    ca * r1 - px, 0.0F, sa * r1 - pz);
        }

        // ---- 中心符文块（四片小方，缓慢自转） ----
        float coreR = radius * 0.16F;
        float cs = (float) Math.cos(t * 1.6F);
        float sn = (float) Math.sin(t * 1.6F);
        SpellFxMesh.quad(vc, m, rgb, a,
                -coreR * cs - coreR * sn, 0.0F, -coreR * sn + coreR * cs,
                coreR * cs - coreR * sn, 0.0F, coreR * sn + coreR * cs,
                coreR * cs + coreR * sn, 0.0F, coreR * sn - coreR * cs,
                -coreR * cs + coreR * sn, 0.0F, -coreR * sn - coreR * cs);
    }

    /**
     * <b>双螺旋</b>：两条相互缠绕的螺旋带。
     *
     * <p>每条带子由一串**有厚度的四边形**连成（不是线），所以从侧面看也是立体的。</p>
     *
     * @param turns  圈数
     * @param height 总高度（沿 +Y）
     * @param radius 螺旋半径
     * @param width  带子的宽度
     * @param strands 螺旋条数（2 = 双螺旋，3 = 三螺旋）
     */
    /**
     * 螺旋带的亮度：越高越亮（能量往上聚集），且朝向镜头的一侧更亮。
     *
     * <p>没有 shader，所以朝向用"带子法线与固定光的点乘"近似 ——
     * 效果是带子有明暗面，转起来能看出是**立体带**而不是一条平涂。</p>
     */
    static float shadeHelix(double angle, float t) {
        double nx = Math.cos(angle);
        double nz = Math.sin(angle);
        double lambert = Math.max(0.0D, nx * 0.42D + 0.52D * nz + 0.35D);
        return (float) (0.40D + 0.50D * lambert + 0.45D * t);
    }

    public static void helix(com.mojang.blaze3d.vertex.PoseStack pose,
                             net.minecraft.client.renderer.MultiBufferSource buf,
                             String tex, int turns, int height, float radius,
                             float width, int strands, float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);
        int steps = Math.max(12, turns * 12);
        for (int s = 0; s < strands; s++) {
            double phase = 2.0D * Math.PI * s / strands;
            for (int i = 0; i < steps; i++) {
                double t0 = (double) i / steps;
                double t1 = (double) (i + 1) / steps;
                double a0 = phase + 2.0D * Math.PI * turns * t0;
                double a1 = phase + 2.0D * Math.PI * turns * t1;
                float y0 = (float) (height * t0);
                float y1 = (float) (height * t1);
                // 带子的两个边（沿径向展开 width）
                float x00 = (float) (Math.cos(a0) * (radius - width * 0.5F));
                float z00 = (float) (Math.sin(a0) * (radius - width * 0.5F));
                float x01 = (float) (Math.cos(a0) * (radius + width * 0.5F));
                float z01 = (float) (Math.sin(a0) * (radius + width * 0.5F));
                float x10 = (float) (Math.cos(a1) * (radius - width * 0.5F));
                float z10 = (float) (Math.sin(a1) * (radius - width * 0.5F));
                float x11 = (float) (Math.cos(a1) * (radius + width * 0.5F));
                float z11 = (float) (Math.sin(a1) * (radius + width * 0.5F));
                // 逐顶点明暗：越高越亮 + 朝向镜头的一侧更亮 ——
                // 转起来能看出是**立体带**，而不是一条平涂的纸带。
                SpellFxMesh.quadShaded(vc, m, rgb, a,
                        x00, y0, z00, shadeHelix(a0, (float) t0),
                        x10, y1, z10, shadeHelix(a1, (float) t1),
                        x11, y1, z11, shadeHelix(a1, (float) t1),
                        x01, y0, z01, shadeHelix(a0, (float) t0));
            }
        }
    }

    /**
     * <b>立体晶格</b>：三轴交叉的棒条构成的笼。
     *
     * <p>金系 / 禁锢 / 阵法的锐利感。每根棒条是四棱柱（有厚度）。</p>
     *
     * @param size  笼的半边长
     * @param bars  每轴棒条数（建议 3~5）
     * @param thick 棒条粗细
     */
    public static void lattice(com.mojang.blaze3d.vertex.PoseStack pose,
                               net.minecraft.client.renderer.MultiBufferSource buf,
                               String tex, float size, int bars, float thick,
                               float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        int a = SpellFxMesh.byteAlpha(alpha);
        for (int i = 0; i < bars; i++) {
            // 每根棒条在另两个轴上的位置（均匀分布）
            float o = bars == 1 ? 0.0F : (2.0F * i / (bars - 1) - 1.0F) * size;
            // X 向棒条
            bar(vc, m, rgb, a, -size, o, o, size, o, o, thick);
            // Y 向棒条
            bar(vc, m, rgb, a, o, -size, o, o, size, o, thick);
            // Z 向棒条
            bar(vc, m, rgb, a, o, o, -size, o, o, size, thick);
        }
    }

    /** 一根四棱柱（沿 A→B，截面为 thick×thick 的正方形，近似用两端点 + 偏移画 4 个面）。 */
    private static void bar(VertexConsumer vc, Matrix4f m, float[] rgb, int a,
                            float ax, float ay, float az,
                            float bx, float by, float bz, float thick) {
        float h = thick * 0.5F;
        // 沿主轴决定截面的两个正交方向
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4F) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        // 任取一个与主轴不平行的向量做叉积，得到截面基
        float ux = -dy, uy = dx, uz = 0.0F;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 1.0E-4F) {
            ux = 1.0F;
            uy = 0.0F;
            uz = 0.0F;
            ul = 1.0F;
        }
        ux = ux / ul * h;
        uy = uy / ul * h;
        uz = uz / ul * h;
        // 第二个方向 = 主轴 × u
        float vx = dy * uz - dz * uy;
        float vy = dz * ux - dx * uz;
        float vz = dx * uy - dy * ux;
        // 四个侧面
        side(vc, m, rgb, a, ax + ux, ay + uy, az + uz, bx + ux, by + uy, bz + uz,
                vx, vy, vz);
        side(vc, m, rgb, a, ax - ux, ay - uy, az - uz, bx - ux, by - uy, bz - uz,
                -vx, -vy, -vz);
        side(vc, m, rgb, a, ax + vx, ay + vy, az + vz, bx + vx, by + vy, bz + vz,
                -ux, -uy, -uz);
        side(vc, m, rgb, a, ax - vx, ay - vy, az - vz, bx - vx, by - vy, bz - vz,
                ux, uy, uz);
    }

    /** 一条侧面（从 A→B，法向偏移 n 决定厚度方向）。 */
    private static void side(VertexConsumer vc, Matrix4f m, float[] rgb, int a,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float nx, float ny, float nz) {
        SpellFxMesh.quad(vc, m, rgb, a,
                ax, ay, az,
                bx, by, bz,
                bx + nx, by + ny, bz + nz,
                ax + nx, ay + ny, az + nz);
    }
}
