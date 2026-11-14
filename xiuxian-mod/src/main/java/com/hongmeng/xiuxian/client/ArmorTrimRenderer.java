package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>盔甲外挂部件</b>：肩甲 / 披风 / 护腕。
 *
 * <h2>玩家要求</h2>
 * <blockquote>「装备-幻想盔甲…把它们的『外挂部件』（肩甲、护腕、披风）做成 3D 模型，
 * 视觉提升面比武器更大（穿在身上一直看得见）」</blockquote>
 *
 * <h2>为什么不用自定义盔甲模型</h2>
 * <p>原版盔甲是<b>贴图驱动</b>的（layer_1 / layer_2 按固定 UV 摊开），
 * 加"外挂部件"意味着要改盔甲模型类并接入 {@code IClientItemExtensions#getArmorRenderer}，
 * 一旦某个套装没接上就会<b>整个人隐形或错位</b>。</p>
 *
 * <p>这里改成<b>世界空间现画</b>：读玩家身上的盔甲物品，判断是哪一套，
 * 然后在肩膀/背后/小臂位置画四边形。好处：</p>
 * <ul>
 *   <li>不影响原版盔甲渲染，出错最多是"部件没画出来"，不会毁掉玩家模型；</li>
 *   <li>可以按套装<b>任意上色</b>（顶点色），不用为每套画新贴图；</li>
 *   <li>披风能跟着<b>走路摆动</b>（读 {@code walkAnimation}）。</li>
 * </ul>
 *
 * <h2>配色</h2>
 * <p>十套道袍按<b>阶数渐变</b>（赤铜 → 金 → 青玉 → 紫 → 白金），
 * 五套道途甲按<b>道途色</b>（和武器光效同一套）。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ArmorTrimRenderer {

    /** 可见半径。 */
    private static final double RANGE = 32.0D;

    private static final ResourceLocation TRIM =
            new ResourceLocation("hongmeng", "textures/fx/armor_trim.png");

    /**
     * PERF(C): 渲染类型只建一次（和 MobHealthBarRenderer.PLATE_TYPE 同一个理由）。
     *
     * <p>原来 {@code RenderType.entityTranslucentEmissive(TRIM)} 在<b>每帧</b>
     * 的 getBuffer 和 endBatch 里各 new 一次 —— 会 churn 渲染资源；
     * 项目里已经踩过"每帧新建 RenderType 导致渲染资源损坏"的坑（见 RankGlowLayer 注释）。</p>
     */
    private static final RenderType TRIM_TYPE = RenderType.entityTranslucentEmissive(TRIM);

    /** PERF(C): 物品 id → 部件颜色（原来每帧每个玩家都要 split + new float[]）。 */
    private static final java.util.Map<ResourceLocation, float[]> COLOR_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();
    /** 不是本模组护甲的哨兵值（ConcurrentHashMap 不能存 null）。 */
    private static final float[] NOT_OURS = new float[0];

    private ArmorTrimRenderer() {
    }

    /** 道途甲配色（与武器光效一致）。 */
    private static float[] pathColor(String path) {
        return switch (path) {
            case "fa" -> new float[]{0.42F, 0.76F, 1.00F};
            case "ti" -> new float[]{1.00F, 0.66F, 0.28F};
            case "mo" -> new float[]{0.76F, 0.36F, 1.00F};
            case "xue" -> new float[]{1.00F, 0.30F, 0.34F};
            default -> new float[]{0.34F, 0.96F, 0.80F};
        };
    }

    /** 十境道袍配色：阶数越高越"贵"。 */
    private static float[] robeColor(int tier) {
        float t = Mth.clamp((tier - 1) / 9.0F, 0.0F, 1.0F);
        // 赤铜 -> 金 -> 青玉 -> 紫 -> 白金
        float r = 0.72F + 0.26F * (1.0F - Math.abs(t - 0.35F) / 0.35F);
        float g = 0.42F + 0.50F * t;
        float b = 0.26F + 0.70F * Math.max(0.0F, t - 0.45F) / 0.55F;
        return new float[]{Math.min(1.0F, r), Math.min(1.0F, g),
                Math.min(1.0F, b)};
    }

    /** 判读胸甲属于哪一套；返回 null 表示不是我们的甲。 */
    private static float[] colorOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !"hongmeng".equals(id.getNamespace())) {
            return null;
        }
        // PERF(C): 物品 id 是固定的，颜色按 id 缓存；原来每帧每个玩家都要
        //          String.split("_")（每次编译正则）+ new float[]。
        float[] cached = COLOR_CACHE.computeIfAbsent(id, ArmorTrimRenderer::computeColor);
        return cached == NOT_OURS ? null : cached;
    }

    /** 真正算颜色的那一步（每个物品 id 只会走一次）。 */
    private static float[] computeColor(ResourceLocation id) {
        String path = id.getPath();
        if (path.startsWith("path_armor_")) {
            // path_armor_<fa|ti|mo|xue|zhen>_chestplate
            String[] parts = path.split("_");
            if (parts.length >= 4) {
                return pathColor(parts[2]);
            }
            return pathColor("fa");
        }
        if (path.startsWith("dao_armor_")) {
            // dao_armor_<1..10>_chestplate
            String[] parts = path.split("_");
            if (parts.length >= 4) {
                try {
                    return robeColor(Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) {
                    return robeColor(1);
                }
            }
            return robeColor(1);
        }
        return NOT_OURS;
    }

    /** PERF(C): 扫描结果复用（原来每帧 new 两个 ArrayList）。 */
    private static final List<Player> WEARERS = new ArrayList<>();
    private static final List<float[]> COLORS = new ArrayList<>();

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
        // PERF(C): 复用容器，避免每帧两次 ArrayList 分配
        WEARERS.clear();
        COLORS.clear();
        try {
            for (Player p : mc.level.players()) {
                if (p == null || !p.isAlive()) {
                    continue;
                }
                if (p.distanceToSqr(mc.player) > RANGE * RANGE) {
                    continue;
                }
                float[] c = colorOf(p.getInventory().getArmor(2));   // 胸甲槽
                if (c == null) {
                    continue;
                }
                WEARERS.add(p);
                COLORS.add(c);
            }
        } catch (Throwable ignored) {
            return;
        }
        if (WEARERS.isEmpty()) {
            return;
        }
        try {
            render(event.getPoseStack(), event.getCamera().getPosition(),
                    event.getPartialTick(), WEARERS, COLORS);
        } catch (Throwable ignored) {
            // 渲染失败绝不能崩客户端
        }
    }

    private static void render(PoseStack pose, Vec3 cam, float partial,
                               List<Player> wearers, List<float[]> colors) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        // PERF(C): 复用缓存的渲染类型（原来每帧 new 两次 RenderType）
        VertexConsumer vc = buffers.getBuffer(TRIM_TYPE);

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        var m = pose.last().pose();

        for (int i = 0; i < wearers.size(); i++) {
            Player p = wearers.get(i);
            float[] c = colors.get(i);
            float yawRad = (float) Math.toRadians(
                    Mth.lerp(partial, p.yRotO, p.getYRot()));
            double fx = -Math.sin(yawRad);     // 前方
            double fz = Math.cos(yawRad);
            double rx = -Math.cos(yawRad);     // 右方
            double rz = -Math.sin(yawRad);
            double px = Mth.lerp(partial, p.xOld, p.getX());
            double py = Mth.lerp(partial, p.yOld, p.getY());
            double pz = Mth.lerp(partial, p.zOld, p.getZ());

            // 走路摆动量（0~1）
            float walk = p.walkAnimation.speed(partial);
            float swing = Mth.sin(p.walkAnimation.position(partial)) * 0.10F * walk;

            // ① 肩甲：每侧 2 个**立体小盒**（有厚度，不是贴片）
            for (int side = -1; side <= 1; side += 2) {
                double bx = px + rx * 0.20D * side;
                double by = py + 1.38D;
                double bz = pz + rz * 0.20D * side;
                for (int seg = 0; seg < 2; seg++) {
                    double out = 0.055D + seg * 0.085D;
                    double drop = 0.055D * seg;
                    box(vc, m,
                            bx + rx * out * side, by - drop, bz + rz * out * side,
                            seg == 0 ? 0.075D : 0.055D,
                            seg == 0 ? 0.075D : 0.055D,
                            0.095D,
                            c, 0.55F);
                }
            }

            // ② 披风：一薄片**盒**（有厚度，会随走路摆动）
            {
                double halfW = 0.17D;
                double top = py + 1.44D;
                double bottom = py + 0.78D;
                double back = 0.175D;
                double sway = swing * 0.5D;
                double bx = px - fx * back;
                double bz = pz - fz * back;
                box(vc, m,
                        bx - fx * sway * 0.5D,
                        (top + bottom) * 0.5D,
                        bz - fz * sway * 0.5D,
                        halfW, (top - bottom) * 0.5D, 0.022D,
                        new float[]{c[0] * 0.8F, c[1] * 0.8F, c[2] * 0.8F},
                        0.62F);
            }

            // ③ 护腕（两侧小鼓包）
            for (int side = -1; side <= 1; side += 2) {
                double bx = px + rx * 0.30D * side;
                double bz = pz + rz * 0.30D * side;
                double by = py + 1.02D;
                quad(vc, m,
                        bx - fx * 0.045D, by + 0.06D, bz - fz * 0.07D,
                        bx + fx * 0.045D, by + 0.06D, bz + fz * 0.07D,
                        bx + fx * 0.045D, by - 0.06D, bz + fz * 0.07D,
                        bx - fx * 0.045D, by - 0.06D, bz - fz * 0.07D,
                        c, 0.5F);
            }
        }

        pose.popPose();
        buffers.endBatch(TRIM_TYPE);
    }


    /**
     * <b>画一个六面体</b>（护甲外挂件必须是立体的）。
     *
     * <p>原来每个部件只画<b>一个四边形</b> —— 侧面看是纸片、背面直接消失，
     * 这正是玩家说的"红色的一块贴在身上"。</p>
     *
     * <p>六面各给不同亮度（顶 1.15 / 侧 0.95 / 底 0.55），
     * 才有"有厚度"的观感。</p>
     */
    private static void box(VertexConsumer vc, org.joml.Matrix4f m,
                            double cx, double cy, double cz,
                            double hx, double hy, double hz,
                            float[] c, float alpha) {
        float x0 = (float) (cx - hx), x1 = (float) (cx + hx);
        float y0 = (float) (cy - hy), y1 = (float) (cy + hy);
        float z0 = (float) (cz - hz), z1 = (float) (cz + hz);

        // 顶
        quadShaded(vc, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, c, alpha, 1.15F);
        // 底
        quadShaded(vc, m, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, c, alpha, 0.55F);
        // 北（-z）
        quadShaded(vc, m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, c, alpha, 0.90F);
        // 南（+z）
        quadShaded(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, c, alpha, 1.00F);
        // 西（-x）
        quadShaded(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, c, alpha, 0.82F);
        // 东（+x）
        quadShaded(vc, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, c, alpha, 0.95F);
    }

    /** 带亮度的四边形（内部用，贴图 UV 统一取满）。 */
    private static void quadShaded(VertexConsumer vc, org.joml.Matrix4f m,
                                   float x0, float y0, float z0,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float x3, float y3, float z3,
                                   float[] c, float alpha, float shade) {
        vc.vertex(m, x0, y0, z0)
                .color(c[0] * shade, c[1] * shade, c[2] * shade, alpha)
                .uv(0.0F, 1.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, x1, y1, z1)
                .color(c[0] * shade, c[1] * shade, c[2] * shade, alpha)
                .uv(1.0F, 1.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, x2, y2, z2)
                .color(c[0] * shade, c[1] * shade, c[2] * shade, alpha)
                .uv(1.0F, 0.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, x3, y3, z3)
                .color(c[0] * shade, c[1] * shade, c[2] * shade, alpha)
                .uv(0.0F, 0.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    /** 一个带贴图 UV 的四边形（四个顶点亮度可调）。 */
    private static void quad(VertexConsumer vc, org.joml.Matrix4f m,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             float[] c, float shade) {
        vc.vertex(m, (float) x0, (float) y0, (float) z0)
                .color(c[0] * shade, c[1] * shade, c[2] * shade, 0.92F)
                .uv(0.0F, 1.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x1, (float) y1, (float) z1)
                .color(c[0] * shade, c[1] * shade, c[2] * shade, 0.92F)
                .uv(1.0F, 1.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2)
                .color(c[0] * shade, c[1] * shade, c[2] * shade, 0.92F)
                .uv(1.0F, 0.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3)
                .color(c[0] * shade, c[1] * shade, c[2] * shade, 0.92F)
                .uv(0.0F, 0.0F).overlayCoords(0).uv2(15728880)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
    }
}
