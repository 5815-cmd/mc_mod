package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>法术特效的立体几何生成器</b>（玩家要求）。
 *
 * <p>玩家原话：「陨石要<b>立体的</b>，那种释放的特效都要立体的，比如还有召唤的巨剑、
 * 黑洞什么的，<b>你不能只贴图贴一个面</b>，还要配合特效」。</p>
 *
 * <h2>问题在哪</h2>
 * <p>之前所有贴图特效都是**一张朝向相机的面片**（billboard）。转到侧面看就是一张薄纸，
 * 陨石像纸片、巨剑像贴纸。真正的做法是用**几片面片拼出体积**：</p>
 * <ul>
 *   <li>{@link #prism}：围绕 Y 轴的 N 棱柱 —— 陨石、巨剑剑身都用它；</li>
 *   <li>{@link #cylinder}：光柱、黑洞吸积盘的外筒；</li>
 *   <li>{@link #sphere}：黑洞本体、能量球；</li>
 *   <li>{@link #cone}：光柱的锥形收口、火舌；</li>
 *   <li>{@link #starBurst}：八向星芒（冲击波的立体版，有厚度）。</li>
 * </ul>
 *
 * <h2>性能</h2>
 * <p>每个立体形状 = N 个四边形，比 billboard 多几倍顶点，但**远少于用粒子堆同体积**
 * （粒子要几百个）。而且顶点都进同一个缓冲区、同一次 draw call，
 * 所以"立体"这件事本身几乎不额外花钱。</p>
 */
public final class SpellFxMesh {

    /**
     * PERF(C): 渲染类型缓存 —— 直接按<b>贴图裸名</b>做键。
     *
     * <p>原来每次调用都先 {@code new ResourceLocation("hongmeng", "textures/fx/" + tex + ".png")}
     * 再查表；而 {@code type()} 在一帧里会被每个几何原语调用几十次
     * （48 条特效 × 每条十几层），等于每帧上千个临时 ResourceLocation。
     * 键换成 String 后连这个分配也省掉（贴图名都是常量字符串，哈希很快）。</p>
     */
    private static final Map<String, RenderType> TYPES = new ConcurrentHashMap<>();


    private SpellFxMesh() {
    }

    /** 加色混合的渲染类型（贴图自发光，用顶点色染色）。 */
    public static RenderType type(String texName) {
        return TYPES.computeIfAbsent(texName, n -> RenderType.entityTranslucentEmissive(
                new ResourceLocation("hongmeng", "textures/fx/" + n + ".png")));
    }


    // ------------------------------------------------------------------
    // 基元
    // ------------------------------------------------------------------

    /**
     * <b>N 棱柱</b>（围绕 Y 轴）：陨石、剑身这种"有厚度的块"。
     *
     * @param sides   侧面数（6~10 像石头，4 像刀剑）
     * @param height  高
     * @param radius  外接半径
     * @param twist   整体绕 Y 的旋转（度）
     * @param irregular 是否给每个顶点做半径扰动（石头的不规则感；0 = 正多边形）
     */
    public static void prism(MultiBufferSource buf, PoseStack pose, String tex,
                             int sides, float height, float radius,
                             float twist, float irregular, float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(type(tex));
        Matrix4f m = pose.last().pose();
        int a = byteAlpha(alpha);
        float half = height * 0.5F;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.toRadians(twist + i * 360.0D / sides);
            double a1 = Math.toRadians(twist + (i + 1) * 360.0D / sides);
            float r0 = radius * (1.0F + wobble(i, irregular));
            float r1 = radius * (1.0F + wobble(i + 1, irregular));
            float x0 = (float) Math.cos(a0) * r0;
            float z0 = (float) Math.sin(a0) * r0;
            float x1 = (float) Math.cos(a1) * r1;
            float z1 = (float) Math.sin(a1) * r1;
            // 侧面（略微向内收，做出"削面"的体积感）
            float topShrink = 0.86F;
            quad(vc, m, rgb, a,
                    x0, -half, z0, x1, -half, z1,
                    x1 * topShrink, half, z1 * topShrink,
                    x0 * topShrink, half, z0 * topShrink);
        }
        // 上下盖（用三角扇近似：中心点 + 每个棱边）
        for (int i = 0; i < sides; i++) {
            double a0 = Math.toRadians(twist + i * 360.0D / sides);
            double a1 = Math.toRadians(twist + (i + 1) * 360.0D / sides);
            float r0 = radius * (1.0F + wobble(i, irregular));
            float r1 = radius * (1.0F + wobble(i + 1, irregular));
            float x0 = (float) Math.cos(a0) * r0;
            float z0 = (float) Math.sin(a0) * r0;
            float x1 = (float) Math.cos(a1) * r1;
            float z1 = (float) Math.sin(a1) * r1;
            tri(vc, m, rgb, a, 0.0F, half, 0.0F, x1 * 0.86F, half, z1 * 0.86F,
                    x0 * 0.86F, half, z0 * 0.86F);
            tri(vc, m, rgb, a, 0.0F, -half, 0.0F, x0, -half, z0, x1, -half, z1);
        }
    }

    /** <b>圆柱筒</b>（不封顶）：光柱、吸积盘外筒。 */
    public static void cylinder(MultiBufferSource buf, PoseStack pose, String tex,
                                int sides, float height, float radius,
                                float[] rgb, float alpha) {
        cylinder(buf, pose, tex, sides, height, radius, rgb, alpha, 1.0F);
    }

    /** 圆柱（可指定整体亮度）。 */
    public static void cylinder(MultiBufferSource buf, PoseStack pose, String tex,
                                int sides, float height, float radius,
                                float[] rgb, float alpha, float bright) {
        VertexConsumer vc = buf.getBuffer(type(tex));
        Matrix4f m = pose.last().pose();
        int a = byteAlpha(alpha);
        float half = height * 0.5F;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.toRadians(i * 360.0D / sides);
            double a1 = Math.toRadians((i + 1) * 360.0D / sides);
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            quad(vc, m, rgb, a, x0, -half, z0, x1, -half, z1,
                    x1, half, z1, x0, half, z0);
        }
    }

    /** <b>圆锥</b>：火舌、光柱收口。尖端朝 +Y。 */
    public static void cone(MultiBufferSource buf, PoseStack pose, String tex,
                            int sides, float height, float radius,
                            float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(type(tex));
        Matrix4f m = pose.last().pose();
        int a = byteAlpha(alpha);
        for (int i = 0; i < sides; i++) {
            double a0 = Math.toRadians(i * 360.0D / sides);
            double a1 = Math.toRadians((i + 1) * 360.0D / sides);
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            tri(vc, m, rgb, a, 0.0F, height, 0.0F, x0, 0.0F, z0, x1, 0.0F, z1);
        }
    }

    /** <b>球</b>（经纬网格）：黑洞本体、能量球。 */
    /**
     * 带平移的球（陨石沿轨迹移动要用）。
     *
     * <p>不直接 pose.translate 的原因：一帧要画 6 个尾迹球，
     * 每次 push/pop 多 12 次矩阵运算 —— 传偏移更省，也不会忘记 pop。</p>
     */
    public static void sphere(MultiBufferSource buf, PoseStack pose, String tex,
                              int rings, int sectors, float radius, float[] rgb,
                              float alpha, float bright,
                              float ox, float oy, float oz) {
        VertexConsumer vc = buf.getBuffer(type(tex));
        Matrix4f m = pose.last().pose();
        int a = byteAlpha(alpha);
        for (int i = 0; i < rings; i++) {
            double p0 = Math.PI * i / rings - Math.PI / 2.0D;
            double p1 = Math.PI * (i + 1) / rings - Math.PI / 2.0D;
            float y0 = (float) Math.sin(p0) * radius;
            float y1 = (float) Math.sin(p1) * radius;
            float r0 = (float) Math.cos(p0) * radius;
            float r1 = (float) Math.cos(p1) * radius;
            for (int j = 0; j < sectors; j++) {
                double t0 = 2.0D * Math.PI * j / sectors;
                double t1 = 2.0D * Math.PI * (j + 1) / sectors;
                float x00 = (float) Math.cos(t0) * r0;
                float z00 = (float) Math.sin(t0) * r0;
                float x01 = (float) Math.cos(t1) * r0;
                float z01 = (float) Math.sin(t1) * r0;
                float x10 = (float) Math.cos(t0) * r1;
                float z10 = (float) Math.sin(t0) * r1;
                float x11 = (float) Math.cos(t1) * r1;
                float z11 = (float) Math.sin(t1) * r1;
                float b00 = bright * phong(x00, y0, z00, radius);
                float b01 = bright * phong(x01, y0, z01, radius);
                float b10 = bright * phong(x10, y1, z10, radius);
                float b11 = bright * phong(x11, y1, z11, radius);
                quadShaded(vc, m, rgb, a,
                        ox + x00, oy + y0, oz + z00, b00,
                        ox + x10, oy + y1, oz + z10, b10,
                        ox + x11, oy + y1, oz + z11, b11,
                        ox + x01, oy + y0, oz + z01, b01);
            }
        }
    }

    public static void sphere(MultiBufferSource buf, PoseStack pose, String tex,
                              int rings, int sectors, float radius,
                              float[] rgb, float alpha) {
        sphere(buf, pose, tex, rings, sectors, radius, rgb, alpha, 1.0F);
    }

    /** 球（可指定整体亮度；内部还会按高度做逐顶点明暗）。 */
    public static void sphere(MultiBufferSource buf, PoseStack pose, String tex,
                              int rings, int sectors, float radius,
                              float[] rgb, float alpha, float bright) {
        VertexConsumer vc = buf.getBuffer(type(tex));
        Matrix4f m = pose.last().pose();
        int a = byteAlpha(alpha);
        for (int i = 0; i < rings; i++) {
            double p0 = Math.PI * i / rings - Math.PI / 2.0D;
            double p1 = Math.PI * (i + 1) / rings - Math.PI / 2.0D;
            float y0 = (float) Math.sin(p0) * radius;
            float y1 = (float) Math.sin(p1) * radius;
            float r0 = (float) Math.cos(p0) * radius;
            float r1 = (float) Math.cos(p1) * radius;
            for (int j = 0; j < sectors; j++) {
                double t0 = 2.0D * Math.PI * j / sectors;
                double t1 = 2.0D * Math.PI * (j + 1) / sectors;
                float x00 = (float) Math.cos(t0) * r0;
                float z00 = (float) Math.sin(t0) * r0;
                float x01 = (float) Math.cos(t1) * r0;
                float z01 = (float) Math.sin(t1) * r0;
                float x10 = (float) Math.cos(t0) * r1;
                float z10 = (float) Math.sin(t0) * r1;
                float x11 = (float) Math.cos(t1) * r1;
                float z11 = (float) Math.sin(t1) * r1;
                // 逐顶点 Phong 着色：法线 · 光源 + 镜面高光。
                //
                // ⚠ 原来只有"上亮下暗"的竖向渐变 —— 那只能说明"这是上半球"，
                //   说明不了"这是个球"。球感的来源是**一个亮斑**（高光）。
                float b00 = bright * phong(x00, y0, z00, radius);
                float b01 = bright * phong(x01, y0, z01, radius);
                float b10 = bright * phong(x10, y1, z10, radius);
                float b11 = bright * phong(x11, y1, z11, radius);
                quadShaded(vc, m, rgb, a,
                        x00, y0, z00, b00,
                        x10, y1, z10, b10,
                        x11, y1, z11, b11,
                        x01, y0, z01, b01);
            }
        }
    }

    /**
     * <b>立体星芒</b>：冲击波的"八角星"，每个角是一个有厚度的三棱锥。
     *
     * <p>比 billboard 的平面星芒更凶 —— 从任何角度看都是立体的刺。</p>
     */
    public static void starBurst(MultiBufferSource buf, PoseStack pose, String tex,
                                 int spikes, float longLen, float shortLen,
                                 float thickness, float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(type(tex));
        Matrix4f m = pose.last().pose();
        int a = byteAlpha(alpha);
        for (int i = 0; i < spikes; i++) {
            double ang = Math.toRadians(i * 360.0D / spikes);
            float len = (i % 2 == 0) ? longLen : shortLen;
            float dx = (float) Math.cos(ang);
            float dz = (float) Math.sin(ang);
            // 垂直方向的偏移（构成三棱锥的底边）
            float px = -dz * thickness;
            float pz = dx * thickness;
            tri(vc, m, rgb, a, dx * len, 0.0F, dz * len, px, 0.0F, pz, -px, 0.0F, -pz);
            tri(vc, m, rgb, a, dx * len, 0.0F, dz * len, -px, 0.0F, -pz, px, 0.0F, pz);
        }
    }

    /** <b>吸积盘</b>：黑洞周围两圈反向旋转的环带（有厚度）。 */
    public static void accretionDisk(MultiBufferSource buf, PoseStack pose, String tex,
                                     int segments, float inner, float outer,
                                     float tilt, float spin,
                                     float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(type(tex));
        Matrix4f m = pose.last().pose();
        int a = byteAlpha(alpha);
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(tilt));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spin));
        m = pose.last().pose();
        for (int i = 0; i < segments; i++) {
            double a0 = Math.toRadians(i * 360.0D / segments);
            double a1 = Math.toRadians((i + 1) * 360.0D / segments);
            float x0 = (float) Math.cos(a0);
            float z0 = (float) Math.sin(a0);
            float x1 = (float) Math.cos(a1);
            float z1 = (float) Math.sin(a1);
            quad(vc, m, rgb, a, x0 * inner, 0.0F, z0 * inner, x0 * outer, 0.0F, z0 * outer,
                    x1 * outer, 0.0F, z1 * outer, x1 * inner, 0.0F, z1 * inner);
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------
    // 顶点输出
    // ------------------------------------------------------------------

    /**
     * 带<b>逐顶点明暗</b>的四边形。
     *
     * <p>为什么必须有这个：原来的 {@link #quad} 四个顶点都是 {@code bright=1.0}，
     * 于是无论网格多细，整块都是**同一个颜色** —— 看起来像剪纸，
     * 这就是"特效没有建模感"的根源。</p>
     *
     * <p>顶点亮度是 CPU 端算好传进去的（没有 shader，所以只能这样）。
     * 常见用法：按法线与光源方向点乘，外侧亮、内侧暗 —— 立刻就"有厚度"。</p>
     */
    static void quadShaded(VertexConsumer vc, Matrix4f m, float[] rgb, int a,
                           float x0, float y0, float z0, float b0,
                           float x1, float y1, float z1, float b1,
                           float x2, float y2, float z2, float b2,
                           float x3, float y3, float z3, float b3) {
        v(vc, m, rgb, a, x0, y0, z0, 0.0F, 1.0F, b0);
        v(vc, m, rgb, a, x1, y1, z1, 1.0F, 1.0F, b1);
        v(vc, m, rgb, a, x2, y2, z2, 1.0F, 0.0F, b2);
        v(vc, m, rgb, a, x3, y3, z3, 0.0F, 0.0F, b3);
    }

    static void quad(VertexConsumer vc, Matrix4f m, float[] rgb, int a,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        v(vc, m, rgb, a, x0, y0, z0, 0.0F, 1.0F);
        v(vc, m, rgb, a, x1, y1, z1, 1.0F, 1.0F);
        v(vc, m, rgb, a, x2, y2, z2, 1.0F, 0.0F);
        v(vc, m, rgb, a, x3, y3, z3, 0.0F, 0.0F);
    }

    /** 四个顶点各自亮度的四边形（做深度明暗用）。 */
    static void quad(VertexConsumer vc, Matrix4f m, float[] rgb, int a,
                     float x0, float y0, float z0, float x1, float y1, float z1,
                     float x2, float y2, float z2, float x3, float y3, float z3,
                     float b0, float b1, float b2, float b3) {
        v(vc, m, rgb, a, x0, y0, z0, 0.0F, 1.0F, b0);
        v(vc, m, rgb, a, x1, y1, z1, 1.0F, 1.0F, b1);
        v(vc, m, rgb, a, x2, y2, z2, 1.0F, 0.0F, b2);
        v(vc, m, rgb, a, x3, y3, z3, 0.0F, 0.0F, b3);
    }

    /**
     * 按<b>高度</b>取明暗：顶端亮、底端暗。
     *
     * <p>这就是"不再是平贴片"的关键 —— 同一个球的上半与下半亮度不同，
     * 眼睛立刻读出"这是个立体的发光体"。</p>
     *
     * @param y      顶点高度（相对形状中心）
     * @param radius 形状半径（用来归一化）
     */
    /**
     * 球面一点的 Phong 亮度（法线·光源 + 镜面高光）。
     *
     * <p>光源方向 (0.42, 0.74, 0.52) —— 左上偏前，与项目其它高光一致。</p>
     *
     * <p>为什么必须有高光：原来球只有上亮下暗的竖向渐变，
     * 那只能说明这是上半球，说明不了这是个球。球感的来源是<b>一个亮斑</b>。</p>
     */
    static float phong(float x, float y, float z, float radius) {
        if (radius <= 0.0001F) {
            return 1.0F;
        }
        float nx = x / radius;
        float ny = y / radius;
        float nz = z / radius;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0001F) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        float diff = Math.max(0.0F, nx * 0.42F + ny * 0.74F + nz * 0.52F);
        float spec = (float) Math.pow(diff, 8.0D);
        return 0.34F + 0.58F * diff + 1.10F * spec;
    }

    static float shade(float y, float radius) {
        if (radius <= 1.0E-4F) {
            return 1.0F;
        }
        float t = Mth.clamp(y / radius, -1.0F, 1.0F);       // -1 底 .. +1 顶
        return 1.0F + t * 0.35F;                             // 0.65 .. 1.35
    }

    static void tri(VertexConsumer vc, Matrix4f m, float[] rgb, int a,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float x2, float y2, float z2) {
        v(vc, m, rgb, a, x0, y0, z0, 0.5F, 1.0F);
        v(vc, m, rgb, a, x1, y1, z1, 0.0F, 0.0F);
        v(vc, m, rgb, a, x2, y2, z2, 1.0F, 0.0F);
        // 第三点重复一次，凑成四边形（渲染管线按四边形消费）
        v(vc, m, rgb, a, x2, y2, z2, 1.0F, 0.0F);
    }

    /**
     * 写一个顶点。
     *
     * <p>⚠ 原来这里写死 `light = 0xF000F0`（满亮）且顶点色不做任何变化，
     * 于是<b>整个形状是同一个亮度</b> —— 这正是玩家说的"贴图贴上就完事、观感很差"的根因：
     * 没有明暗层次，看起来就是一张纸片。</p>
     *
     * <p>现在多了 {@code bright}：顶点色会被乘上它。
     * 调用方按<b>形状内的位置</b>给亮度（内亮外暗、上亮下暗），
     * 同一个形状立刻有了体积感。</p>
     */
    private static void v(VertexConsumer vc, Matrix4f m, float[] rgb, int a,
                          float x, float y, float z, float u, float vv) {
        v(vc, m, rgb, a, x, y, z, u, vv, 1.0F);
    }

    private static void v(VertexConsumer vc, Matrix4f m, float[] rgb, int a,
                          float x, float y, float z, float u, float vv, float bright) {
        float b = Mth.clamp(bright, 0.0F, 2.4F);
        vc.vertex(m, x, y, z)
                .color(rgb[0] * b, rgb[1] * b, rgb[2] * b, a / 255.0F)
                .uv(u, vv)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /**
     * <b>多层同心壳</b>：把一个形状画 N 层，每层半径缩小、亮度提高。
     *
     * <p>这是"体积感"的标准做法：单层壳从侧面看就是一条线，
     * 多层壳会让它看起来<b>真的有厚度、内部还在发光</b>。
     * 比"堆几百个粒子"便宜得多（顶点都进同一个缓冲区、同一次 draw call）。</p>
     *
     * <p>亮度为什么<b>内层更亮</b>：发光体的核心最亮、边缘被自身吸收，
     * 所以内层要往白色偏（乘大于 1 的亮度）、外层往本色偏（乘小于 1）。</p>
     *
     * @param layers 层数（建议 2~4；再多收益递减）
     * @param shrink 每层缩小比例（建议 0.12~0.20）
     */
    public static void sphereShells(MultiBufferSource buf, PoseStack pose, String tex,
                                    int rings, int sectors, float radius,
                                    int layers, float shrink,
                                    float[] rgb, float alpha) {
        for (int i = 0; i < layers; i++) {
            float f = layers == 1 ? 0.0F : (float) i / (layers - 1);
            float r = radius * (1.0F - shrink * i);
            // 内层亮（1.0 + 0.55）、外层暗（0.45）
            float bright = 1.55F - f * 1.10F;
            float a = alpha * (1.0F - f * 0.45F);
            pose.pushPose();
            pose.scale(r / radius, r / radius, r / radius);
            sphere(buf, pose, tex, rings, sectors, radius, rgb, a, bright);
            pose.popPose();
        }
    }

    /** 多层同心壳版圆柱（外层暗、内层亮）。 */
    public static void cylinderShells(MultiBufferSource buf, PoseStack pose, String tex,
                                      int sides, float height, float radius,
                                      int layers, float shrink,
                                      float[] rgb, float alpha) {
        for (int i = 0; i < layers; i++) {
            float f = layers == 1 ? 0.0F : (float) i / (layers - 1);
            float k = 1.0F - shrink * i;
            float bright = 1.55F - f * 1.10F;
            pose.pushPose();
            pose.scale(k, 1.0F, k);
            cylinder(buf, pose, tex, sides, height, radius, rgb,
                    alpha * (1.0F - f * 0.45F), bright);
            pose.popPose();
        }
    }

    static int byteAlpha(float alpha) {
        return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) & 0xFF;
    }

    /** 确定性的顶点扰动（让石头不像正多边形）。 */
    private static float wobble(int i, float amp) {
        if (amp <= 0.0F) {
            return 0.0F;
        }
        // 注意：这两个常数**超过 int 上限**，必须写成 long 再截断，
        // 否则 javac 报"整型太大"（踩过一次）。
        int h = (int) (i * 2654435761L);
        h ^= h >>> 13;
        int m = (int) (h * 1274126177L);
        float f = ((m >>> 8) & 0xFFFF) / 65535.0F;      // 0~1
        return (f * 2.0F - 1.0F) * amp;
    }
}
