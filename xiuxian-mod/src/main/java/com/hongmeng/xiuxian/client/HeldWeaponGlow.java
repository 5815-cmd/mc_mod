package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.item.gear.ModPathWeapon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>手持武器的动态光效</b>：让武器在第三人称里"活"起来。
 *
 * <h2>为什么需要</h2>
 * <p>模型做得再细，站着不动时它也只是<b>一块静态几何</b>。
 * 玩家说的「第三视角很帅」，一半来自模型，另一半来自<b>它不是死的</b>。</p>
 *
 * <h2>画什么</h2>
 * <ul>
 *   <li><b>呼吸微光</b>：护手位置一团随笔呼吸般涨落的能量（1.1 Hz）；</li>
 *   <li><b>三点流光</b>：三颗小火花绕手心缓慢环绕，相位错开 ——
 *       这是"动"的主要来源；</li>
 *   <li>颜色<b>按道途</b>：法修青蓝 / 体修橙金 / 魔修紫 / 血修血红 / 阵修青绿。</li>
 * </ul>
 *
 * <h2>为什么用 {@code RenderType.lightning()}</h2>
 * <p>它自带 additive 与 no-cull —— 能量光效正是要这个。
 * 用普通半透明贴图的话，逆光时会发灰。</p>
 *
 * <h2>性能</h2>
 * <p>只处理 <b>32 格内、主手持道途武器</b>的玩家；每人约 8 个面片。
 * 一个 8 人服务器最多几十个面片 —— 可以忽略不计。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HeldWeaponGlow {

    /** 光效可见半径（格）。 */
    private static final double RANGE = 32.0D;

    /** 手部相对脚底的高度。 */
    private static final double HAND_HEIGHT = 1.05D;

    /** 手部向右的偏移。 */
    private static final double HAND_SIDE = 0.34D;

    private HeldWeaponGlow() {
    }

    /** 道途配色（r,g,b）。 */
    private static float[] colorFor(com.hongmeng.xiuxian.item.gear.PathWeapon wp) {
        // PERF(C): 写进复用缓冲，不再每个玩家每帧 new 一个 float[]
        fillColor(wp, COLOR_SCRATCH);
        return COLOR_SCRATCH;
    }

    /** PERF(C): 道途颜色复用缓冲（渲染线程内同步用完）。 */
    private static final float[] COLOR_SCRATCH = new float[3];

    private static void fillColor(com.hongmeng.xiuxian.item.gear.PathWeapon wp, float[] out) {
        switch (wp) {
            case FA -> {
                out[0] = 0.35F; out[1] = 0.72F; out[2] = 1.00F;    // 法修：青蓝
            }
            case TI -> {
                out[0] = 1.00F; out[1] = 0.62F; out[2] = 0.22F;    // 体修：橙金
            }
            case MO -> {
                out[0] = 0.72F; out[1] = 0.32F; out[2] = 1.00F;    // 魔修：紫
            }
            case XUE -> {
                out[0] = 1.00F; out[1] = 0.26F; out[2] = 0.30F;    // 血修：血红
            }
            case ZHEN -> {
                out[0] = 0.30F; out[1] = 0.95F; out[2] = 0.78F;    // 阵修：青绿
            }
            default -> {
                out[0] = 1.00F; out[1] = 1.00F; out[2] = 1.00F;
            }
        }
    }

    /** PERF(C): 每帧的持有者列表复用（原来每帧 new ArrayList）。 */
    private static final List<Player> HOLDERS = new ArrayList<>();

    /** 挂到 {@code RenderLevelStageEvent(AFTER_PARTICLES)}。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderStage(
            net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event
                .RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        HOLDERS.clear();
        try {
            for (Player p : mc.level.players()) {
                if (p == null || !p.isAlive()) {
                    continue;
                }
                if (p.distanceToSqr(mc.player) > RANGE * RANGE) {
                    continue;
                }
                if (p.getMainHandItem().getItem()
                        instanceof ModPathWeapon.PathWeaponItem) {
                    HOLDERS.add(p);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
        if (HOLDERS.isEmpty()) {
            return;
        }
        try {
            render(event.getPoseStack(), event.getCamera().getPosition(),
                    event.getPartialTick(), HOLDERS);
        } catch (Throwable ignored) {
            // 渲染失败绝不能崩客户端
        }
    }

    private static void render(PoseStack pose, Vec3 cam, float partial,
                               List<Player> holders) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        float time = (mc.level.getGameTime() + partial) / 20.0F;

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        var m = pose.last().pose();
        for (Player p : holders) {
            var stack = p.getMainHandItem();
            if (!(stack.getItem() instanceof ModPathWeapon.PathWeaponItem pw)) {
                continue;
            }
            float[] c = colorFor(pw.weapon());
            // 手部位置：身体朝向决定"右"
            float yawRad = (float) Math.toRadians(
                    Mth.lerp(partial, p.yRotO, p.getYRot()));
            double rightX = -Math.cos(yawRad);
            double rightZ = -Math.sin(yawRad);
            double hx = Mth.lerp(partial, p.xOld, p.getX()) + rightX * HAND_SIDE;
            double hy = Mth.lerp(partial, p.yOld, p.getY()) + HAND_HEIGHT;
            double hz = Mth.lerp(partial, p.zOld, p.getZ()) + rightZ * HAND_SIDE;

            // ① 呼吸微光（1.1 Hz 涨落）
            float breath = 0.62F + 0.38F * Mth.sin(time * 6.9F);
            float r0 = 0.13F * (0.85F + 0.25F * breath);
            crossQuads(vc, m, hx, hy, hz, r0, c, 0.55F * breath);

            // ② 三点流光：绕手心缓慢环绕，相位错开 120°
            for (int i = 0; i < 3; i++) {
                double ang = time * 1.6D + i * 2.0944D;
                double rad = 0.19D;
                double mx = hx + Math.cos(ang) * rad;
                double mz = hz + Math.sin(ang) * rad;
                double my = hy + Math.sin(ang * 1.7D) * 0.07D;
                float mr = 0.045F * (0.8F + 0.4F * Mth.sin(time * 8.0F + i));
                crossQuads(vc, m, mx, my, mz, mr, c, 0.85F);
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.lightning());
    }

    /** 用两片正交方片表示一个发光点（正面/侧面都有体积）。 */
    private static void crossQuads(VertexConsumer vc, org.joml.Matrix4f m,
                                   double x, double y, double z, float r,
                                   float[] c, float a) {
        // 竖直面
        vc.vertex(m, (float) x - r, (float) y - r, (float) z)
                .color(c[0], c[1], c[2], 0.0F).endVertex();
        vc.vertex(m, (float) x + r, (float) y - r, (float) z)
                .color(c[0], c[1], c[2], 0.0F).endVertex();
        vc.vertex(m, (float) x + r, (float) y + r, (float) z)
                .color(c[0], c[1], c[2], a).endVertex();
        vc.vertex(m, (float) x - r, (float) y + r, (float) z)
                .color(c[0], c[1], c[2], a).endVertex();
        // 水平面（交叉）
        vc.vertex(m, (float) x - r, (float) y, (float) z - r)
                .color(c[0], c[1], c[2], 0.0F).endVertex();
        vc.vertex(m, (float) x + r, (float) y, (float) z - r)
                .color(c[0], c[1], c[2], 0.0F).endVertex();
        vc.vertex(m, (float) x + r, (float) y, (float) z + r)
                .color(c[0], c[1], c[2], a).endVertex();
        vc.vertex(m, (float) x - r, (float) y, (float) z + r)
                .color(c[0], c[1], c[2], a).endVertex();
    }
}
