package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/**
 * <b>飞剑的立体几何</b> —— 玩家反馈「法剑的模型也不好看」，所以单独做一套。
 *
 * <h2>原来为什么不好看</h2>
 * <p>我之前直接复用了 {@code SpellFxMesh} 里的 {@code BLADE_GOLD} ——
 * 那是给「斩星」做的<b>金色巨剑</b>：又宽又大、从天劈下。
 * 用它当环绕的飞剑，看起来就是"一把大剑在原地慢慢转"，很笨重，
 * 而且六把一起出现时糊成一团。</p>
 *
 * <h2>飞剑要什么</h2>
 * <p>飞剑的观感全在<b>比例</b>上，所以这里按"四件套"分开做：</p>
 * <pre>
 *      ╱▔▔╲        剑尖：收成一点（有锋）
 *     │    │
 *     │ 剑身│       细长、扁（宽:厚 ≈ 4:1）、有脊线
 *     │    │
 *     ├────┤        剑格：横向一小段（比剑身宽，但很薄）
 *     │ 剑柄│       短、方
 *     ╰─┬──╯
 *       ◇          剑首：一个小坠
 * </pre>
 * <p>关键数字：<b>剑身长宽比 ≈ 9:1</b>（巨剑是 3:1）。
 * 只改这一个比例，观感就从"大剑"变成"飞剑"。</p>
 *
 * <h2>四种剑型</h2>
 * <table border="1">
 *   <caption>武器种类（玩家要求「武器种类多一点」）</caption>
 *   <tr><th>类型</th><th>比例</th><th>适合</th></tr>
 *   <tr><td><b>飞剑</b></td><td>细长 9:1</td><td>通用剑修</td></tr>
 *   <tr><td><b>重剑</b></td><td>宽厚 5:1 + 背脊</td><td>体修</td></tr>
 *   <tr><td><b>短刃</b></td><td>短小 5:1</td><td>多把齐飞、密集剑阵</td></tr>
 *   <tr><td><b>骨刃</b></td><td>单刃微弯 + 骨节</td><td>魔修 / 血修</td></tr>
 * </table>
 */
public final class FlyingSwordMesh {

    private FlyingSwordMesh() {
    }

    /** 剑型。 */
    public enum Kind {
        SWORD("飞剑", 1.00F, 1.00F, 1.00F, false),
        HEAVY("重剑", 1.55F, 1.45F, 0.80F, true),
        DAGGER("短刃", 0.80F, 0.95F, 0.55F, false),
        BONEBLADE("骨刃", 0.95F, 0.85F, 1.15F, true);

        private final String cn;
        /** 宽/厚/长 三个方向的倍率。 */
        final float wide;
        final float thick;
        final float len;
        /** 是否带背脊（重剑 / 骨刃）。 */
        final boolean ridged;

        Kind(String cn, float wide, float thick, float len, boolean ridged) {
            this.cn = cn;
            this.wide = wide;
            this.thick = thick;
            this.len = len;
            this.ridged = ridged;
        }

        public String cn() {
            return this.cn;
        }
    }

    /**
     * 画一把飞剑（<b>剑身沿 +Y</b>，剑柄在原点附近，剑尖朝上）。
     *
     * <p>调用方用 {@code pose.mulPose(...)} 把它转向要去的地方。
     * 朝上是有意的：{@code prism} 这些原语本来就沿 Y 长，
     * 这样不用在每个部件上再做一次旋转。</p>
     *
     * @param scale 整体尺寸（一把环绕的飞剑用 0.55~0.8 就够，别用巨剑的尺寸）
     */
    public static void sword(PoseStack pose, MultiBufferSource buf, String tex,
                             Kind kind, float scale, float[] rgb, float alpha) {
        float s = scale;
        float wide = kind.wide;
        float thick = kind.thick;
        float len = kind.len;

        // ---------------- 剑身 ----------------
        // 4 棱柱 + 极度压扁（x 宽 / z 薄）—— 这就是"有厚度但很薄"的剑身。
        // 长宽比 ≈ 9:1 是"飞剑"而不是"大剑"的关键。
        pose.pushPose();
        pose.translate(0.0D, s * 1.15D, 0.0D);
        pose.scale(0.13F * wide, 1.0F, 0.035F * thick);
        SpellFxMesh.prism(buf, pose, tex, 4, s * 2.30F * len, s * 3.10F, 45.0F, 0.0F,
                rgb, alpha);
        pose.popPose();

        // 剑尖：一个小锥，收成一点（没有它剑身顶部是平的，很钝）
        pose.pushPose();
        pose.translate(0.0D, s * 3.45D * len, 0.0D);
        pose.scale(0.13F * wide, 1.0F, 0.035F * thick);
        SpellFxMesh.cone(buf, pose, tex, 4, s * 0.55F, s * 1.15F, rgb, alpha);
        pose.popPose();

        // ---------------- 剑格 ----------------
        // 横向一小段：比剑身宽、但很薄 —— "十字"感是剑的辨识特征。
        pose.pushPose();
        pose.translate(0.0D, s * 1.08D, 0.0D);
        pose.scale(1.0F, 0.10F, 0.28F);
        SpellFxMesh.prism(buf, pose, tex, 4, s * 0.16F, s * (0.62F * wide), 45.0F, 0.0F,
                rgb, alpha);
        pose.popPose();

        // ---------------- 剑柄 ----------------
        pose.pushPose();
        pose.translate(0.0D, s * 0.72D, 0.0D);
        pose.scale(0.42F, 1.0F, 0.42F);
        SpellFxMesh.prism(buf, pose, tex, 4, s * 0.72F, s * 0.30F, 45.0F, 0.0F,
                rgb, alpha);
        pose.popPose();

        // ---------------- 剑首 ----------------
        pose.pushPose();
        pose.translate(0.0D, s * 0.38D, 0.0D);
        SpellFxMesh.sphere(buf, pose, tex, 3, 6, s * 0.16F, rgb, alpha);
        pose.popPose();

        // ---------------- 背脊（重剑 / 骨刃）----------------
        if (kind.ridged) {
            pose.pushPose();
            pose.translate(0.0D, s * 1.15D, -s * 0.055D * thick);
            pose.scale(0.09F, 1.0F, 0.06F);
            SpellFxMesh.prism(buf, pose, tex, 4, s * 2.25F * len, s * 1.15F, 45.0F, 0.0F,
                    rgb, alpha * 0.85F);
            pose.popPose();
        }

        // ---------------- 骨节（骨刃）----------------
        if (kind == Kind.BONEBLADE) {
            for (int i = 0; i < 4; i++) {
                double y = s * (1.35D + i * 0.52D);
                pose.pushPose();
                pose.translate(0.0D, y, 0.0D);
                SpellFxMesh2.torus(pose, buf, tex, 8, 4, s * 0.17F, s * 0.045F,
                        rgb, alpha * 0.9F);
                pose.popPose();
            }
        }
    }

    /**
     * 画一把"被投掷出去"的飞剑 —— 额外带一道细长的剑气拖尾。
     *
     * <p>穿刺时用它，让"剑飞过去"这件事在视觉上有速度感。
     * 拖尾用一根很细的 4 棱柱沿 -Y（剑的后方）拉出去，
     * 比加一堆粒子更便宜、形状也更利落。</p>
     */
    public static void swordWithTrail(PoseStack pose, MultiBufferSource buf, String tex,
                                      Kind kind, float scale, float[] rgb, float alpha,
                                      float trailLen) {
        sword(pose, buf, tex, kind, scale, rgb, alpha);
        if (trailLen <= 0.01F) {
            return;
        }
        pose.pushPose();
        pose.translate(0.0D, -scale * trailLen * 0.5D, 0.0D);
        pose.scale(0.10F, 1.0F, 0.030F);
        SpellFxMesh.prism(buf, pose, tex, 4, scale * trailLen, scale * 1.6F, 45.0F, 0.0F,
                rgb, alpha * 0.45F);
        pose.popPose();
    }

    /**
     * 画剑阵的"基座环"（六把剑环绕时脚下的一圈）。
     *
     * <p>只在剑阵展开时画一次，让"阵"这件事有立足点，
     * 而不是六把剑凭空浮着。</p>
     */
    public static void formationBase(PoseStack pose, MultiBufferSource buf, String tex,
                                     float radius, float[] rgb, float alpha) {
        SpellFxMesh2.torus(pose, buf, tex, 20, 6, radius, radius * 0.06F, rgb, alpha * 0.55F);
        pose.pushPose();
        pose.scale(0.72F, 1.0F, 0.72F);
        SpellFxMesh2.torus(pose, buf, tex, 16, 5, radius, radius * 0.04F, rgb, alpha * 0.35F);
        pose.popPose();
    }
}
