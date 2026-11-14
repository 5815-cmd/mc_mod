package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * <b>剑气拖尾</b>：挥砍时沿武器轨迹扫出的一条发光弧带。
 *
 * <h2>参考了什么</h2>
 * <p>玩家要求参考「棱花剑魂」这类动作 mod。它们的招牌效果就是
 * <b>刀光 / 剑气拖尾</b>（同类实现见 Fancy Trail、After Swing）——
 * 一条跟随挥砍扫过、头亮尾淡、几帧后消失的发光带。</p>
 *
 * <h2>怎么画（零外部素材）</h2>
 * <p>用原版的 {@link RenderType#lightning()}：它的顶点格式是
 * {@code POSITION_COLOR}、<b>加色混合</b>、无纹理 —— 正好是"自发光的东西"，
 * 闪电就是这么画的。所以这里只喂几何 + 颜色，不需要任何贴图文件。</p>
 *
 * <p>几何是一条四边形带：沿扫过角度切 N 段，每段内外两条边拼成 4 个顶点。
 * 弧长为 0 时整条带退化掉，不会出现"贴片"。</p>
 *
 * <h2>为什么按武器分型画不同形状</h2>
 * <p>横扫要横着的弧、重砸要竖着的弧、双刀要左右交替、轮刃要整圈 ——
 * 同一条弧套在所有武器上会很假。形状表直接复用动作系统里的分型常量。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SlashTrailRenderer {

    /** 拖尾条数上限（防止刷屏时堆一堆）。 */
    private static final int MAX_TRAILS = 24;

    /** 沿弧切几段（16 段在 2 格半径下已经看不出折线）。 */
    private static final int SEGMENTS = 16;

    /** 拖尾持续 tick，与动作时长一致。 */
    private static final int TICKS = CombatAnimClient.TICKS;

    /** 一次挥砍的拖尾。 */
    private static final class Trail {
        final int kind;
        final int combo;
        final long startTick;
        final Vec3 origin;
        final float yaw;
        /** 颜色（按武器分型上色）。 */
        final float r;
        final float g;
        final float b;

        Trail(int kind, int combo, long startTick, Vec3 origin, float yaw,
              float r, float g, float b) {
            this.kind = kind;
            this.combo = combo;
            this.startTick = startTick;
            this.origin = origin;
            this.yaw = yaw;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static final Map<UUID, Trail> TRAILS = new HashMap<>();

    private SlashTrailRenderer() {
    }

    /** 开始一条拖尾（自己与别人都走这里）。只在主手拿着武器、且非第一人称时生成。 */
    public static void start(Player player, int kind, int combo) {
        if (player == null || !holdingWeapon(player)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (TRAILS.size() >= MAX_TRAILS) {
            // 砍掉最早的一条，保证不会无限堆
            Iterator<UUID> it = TRAILS.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        long now = player.level() == null ? 0L : player.level().getGameTime();
        Vec3 at = player.position().add(0.0D, player.getEyeHeight() * 0.85D, 0.0D);
        float[] col = colorOf(kind, combo);
        TRAILS.put(player.getUUID(),
                new Trail(kind, combo, now, at, player.getYRot(), col[0], col[1], col[2]));
    }

    /**
     * 主手是不是武器。
     *
     * <p>玩家 2026-11-08 要求：<b>只有拿武器的时候才会有剑气拖尾</b>。
     * 空手 / 拿方块 / 拿食物 / 拿工具时不再生成拖尾；动作本身不变。</p>
     */
    private static boolean holdingWeapon(Player player) {
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.SwordItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.CareerWeaponItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.ModPathWeapon.PathWeaponItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.RealmWeaponItem) {
            return true;
        }
        // 兜底：任何被本模组武器属性处理器认作武器的物品
        return com.hongmeng.xiuxian.item.gear.WeaponAttributeHandler.attackOf(stack) > 0.0D;
    }

    /** 五条道途各一个色（与武器图标的主色一致，看颜色就知道是谁的招式）。 */
    private static float[] colorOf(int kind, int combo) {
        return switch (kind) {
            case CombatAnimClient.KIND_HEAVY -> new float[]{1.00F, 0.82F, 0.30F};  // 体修 金
            case CombatAnimClient.KIND_STAB -> new float[]{0.68F, 0.36F, 1.00F};   // 魔修 紫
            case CombatAnimClient.KIND_TWIN -> new float[]{1.00F, 0.32F, 0.32F};   // 血修 红
            case CombatAnimClient.KIND_RING -> new float[]{0.35F, 1.00F, 0.62F};   // 阵修 青玉
            default -> new float[]{0.52F, 0.86F, 1.00F};                           // 法修 青蓝
        };
    }

    /** 清账（换世界 / 掉线）。 */
    public static void clear() {
        TRAILS.clear();
    }

    /**
     * 每帧渲染（由 {@code RenderLevelStageEvent} 在粒子之后调用）。
     *
     * <p>坐标必须是<b>世界坐标</b>：所以先把 poseStack 平移到相机原点。</p>
     */
    public static void render(PoseStack pose, Vec3 camPos, float partial) {
        if (TRAILS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            TRAILS.clear();
            return;
        }
        // 玩家 2026-11-08：第一人称不再画剑气拖尾（只保留第三人称）。
        // 第一人称下拖尾会正对镜头扫过，挡视野。
        if (mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        long now = mc.level.getGameTime();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        pose.pushPose();
        // 转到相机坐标系（世界坐标 - 相机位置）
        pose.translate(-camPos.x, -camPos.y, -camPos.z);

        boolean any = false;
        Iterator<Map.Entry<UUID, Trail>> it = TRAILS.entrySet().iterator();
        while (it.hasNext()) {
            Trail t = it.next().getValue();
            float done = (now - t.startTick) + partial;
            if (done < 0.0F || done > TICKS) {
                it.remove();
                continue;
            }
            float p = Mth.clamp(done / (float) TICKS, 0.0F, 1.0F);
            any |= drawTrail(pose, buffers, t, p);
        }
        pose.popPose();

        if (any) {
            buffers.endBatch(RenderType.lightning());
        }
    }

    /** 画一条拖尾；返回是否真的画了东西。 */
    private static boolean drawTrail(PoseStack pose, MultiBufferSource buffers,
                                     Trail t, float p) {
        // 扫过的角度：前 70% 时间扫完，后 30% 用来淡出
        float sweepAll = sweepOf(t.kind);
        float grow = Mth.clamp(p / 0.70F, 0.0F, 1.0F);
        float head = easeOut(grow) * sweepAll;
        if (Math.abs(head) < 1.0F) {
            return false;
        }
        float fade = p <= 0.70F ? 1.0F : 1.0F - (p - 0.70F) / 0.30F;
        if (fade <= 0.01F) {
            return false;
        }

        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        pose.pushPose();
        pose.translate(t.origin.x, t.origin.y, t.origin.z);
        // 朝向：以施法瞬间的身体朝向为基准
        pose.mulPose(Axis.YP.rotationDegrees(-t.yaw));

        // 段位变体：让刀光的**扫向**与动作一致
        //
        // 原来所有段位都扫在同一个平面里 —— 动作换了方向、刀光却还是老样子，
        // 观感上对不上。这里按段位给整个拖尾加一个平面旋转：
        //   0 横扫（水平）· 1 竖劈（垂直）· 2 斜斩 · 3 突刺（窄且平）· 4 回旋
        int variant = Math.floorMod(t.combo, 5);
        switch (variant) {
            case 0 -> pose.mulPose(Axis.ZP.rotationDegrees(-70.0F));  // 水平面
            case 1 -> pose.mulPose(Axis.ZP.rotationDegrees(10.0F));   // 竖直面
            case 2 -> pose.mulPose(Axis.ZP.rotationDegrees(-40.0F));  // 斜
            case 3 -> pose.mulPose(Axis.ZP.rotationDegrees(-82.0F));  // 贴平 + 窄
            default -> pose.mulPose(Axis.ZP.rotationDegrees(-55.0F)); // 回旋
        }
        // 双刀第二段换个方向扫
        float sign = (t.kind == CombatAnimClient.KIND_TWIN && t.combo % 2 == 1)
                ? -1.0F : 1.0F;

        float rIn = innerRadiusOf(t.kind);
        float rOut = outerRadiusOf(t.kind);
        // PERF(C): 4 个点的坐标写进复用数组，不再每段 new 4 个 Vec3
        // （16 段 × 4 = 64 个 Vec3/条/帧；原来最多 24 条同时存在）
        float[] pts = POINT_SCRATCH;
        for (int i = 0; i < SEGMENTS; i++) {
            float f0 = (float) i / SEGMENTS;
            float f1 = (float) (i + 1) / SEGMENTS;
            float a0 = head * f0 * sign;
            float a1 = head * f1 * sign;
            // 尾端淡、前端亮（头亮尾淡才有"扫过去"的速度感）
            float al0 = fade * f0 * f0;
            float al1 = fade * f1 * f1;
            if (al1 <= 0.004F) {
                continue;
            }
            pointOn(t.kind, a0, rIn, pts, 0);     // in0
            pointOn(t.kind, a0, rOut, pts, 3);    // out0
            pointOn(t.kind, a1, rOut, pts, 6);    // out1
            pointOn(t.kind, a1, rIn, pts, 9);     // in1
            quad(pose, vc, pts, t.r, t.g, t.b, al0, al1);
        }
        pose.popPose();
        return true;
    }

    /** PERF(C): 拖尾 4 个点的复用缓冲（渲染线程内同步用完，不跨帧保存）。 */
    private static final float[] POINT_SCRATCH = new float[12];

    /** 往缓冲区塞一个四边形（左下 -> 右下 -> 右上 -> 左上）。 */
    private static void quad(PoseStack pose, VertexConsumer vc, float[] p,
                             float r, float g, float b, float a0, float a1) {
        var m = pose.last().pose();
        // 内 0
        vc.vertex(m, p[0], p[1], p[2])
                .color(r, g, b, a0).endVertex();
        // 外 0
        vc.vertex(m, p[3], p[4], p[5])
                .color(r, g, b, a1).endVertex();
        // 外 1
        vc.vertex(m, p[6], p[7], p[8])
                .color(r, g, b, a1).endVertex();
        // 内 1
        vc.vertex(m, p[9], p[10], p[11])
                .color(r, g, b, a0).endVertex();
    }

    /** 总扫角（度）。 */
    private static float sweepOf(int kind) {
        return switch (kind) {
            case CombatAnimClient.KIND_HEAVY -> 150.0F;   // 重砸：从头顶砸到身前
            case CombatAnimClient.KIND_STAB -> 26.0F;     // 直刺：窄
            case CombatAnimClient.KIND_TWIN -> 170.0F;    // 双刀：大摆
            case CombatAnimClient.KIND_RING -> 340.0F;    // 轮刃：几乎一整圈
            default -> 150.0F;                            // 剑：横扫
        };
    }

    private static float innerRadiusOf(int kind) {
        return kind == CombatAnimClient.KIND_RING ? 1.1F : 0.55F;
    }

    private static float outerRadiusOf(int kind) {
        return switch (kind) {
            case CombatAnimClient.KIND_HEAVY -> 2.6F;
            case CombatAnimClient.KIND_RING -> 2.4F;
            case CombatAnimClient.KIND_STAB -> 3.0F;
            default -> 2.3F;
        };
    }

    /**
     * 弧上一点（局部坐标），写进 {@code out[off..off+2]}。
     *
     * <p>横扫在<b>水平面</b>扫（X-Z），重砸在<b>竖直面</b>扫（Y-Z）——
     * 这两者的观感完全不同，不能用同一套。</p>
     *
     * <p>PERF(C): 原来返回 {@code new Vec3(...)}，每段 4 个；改成写复用数组，
     * 数值计算与原来一致。</p>
     */
    private static void pointOn(int kind, float angleDeg, float radius,
                                float[] out, int off) {
        float a = (float) Math.toRadians(angleDeg);
        // 从正前方（+Z）往右偏 -90° 起扫，看起来像"从右肩挥到左腰"
        float base = -1.9F;
        float ang = base + a;
        if (kind == CombatAnimClient.KIND_HEAVY) {
            // 竖直面：角度从上方扫到前下方
            float va = (float) Math.toRadians(-70.0F + angleDeg);
            out[off] = 0.0F;
            out[off + 1] = radius * Mth.sin(va);
            out[off + 2] = radius * Mth.cos(va);
            return;
        }
        out[off] = radius * Mth.sin(ang);
        out[off + 1] = 0.0F;
        out[off + 2] = radius * Mth.cos(ang);
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }
}
